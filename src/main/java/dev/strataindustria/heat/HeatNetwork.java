package dev.strataindustria.heat;

import dev.strataindustria.Config;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.registry.Tier4Sounds;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Carrying a firebox's heat over pipes (tier 4 spec 8.2). A source finds every consumer its pipes reach
 * within {@code heat.maxPipeLength} blocks, by the shortest path; it looks again only when a pipe joins
 * or leaves. Each tick it shares its heat among what the consumers ask for: all of it when there is
 * enough, otherwise the same fraction of each request. A consumer gets the source's temperature, held
 * down to the weakest pipe's rating and less the drop of each block on the way, and its share less the
 * loss of each block.
 */
public final class HeatNetwork {
    /** Bumped whenever a pipe joins or leaves anything, so sources know to look again. */
    private static final AtomicInteger VERSION = new AtomicInteger();
    /** Heat that crosses this many pipe blocks or more counts for the journal (spec 15, goal 68). */
    public static final int JOURNAL_PIPES = 4;

    private HeatNetwork() {}

    public static void changed() {
        VERSION.incrementAndGet();
    }

    public static int version() {
        return VERSION.get();
    }

    /**
     * A consumer that heat reaches: how many pipe blocks it crosses, the °C and share it loses on the
     * way, and the rating of the network's weakest pipe.
     */
    public record Route(BlockPos pos, int pipes, float drop, float loss, int cap, @Nullable HeatPipeBlock weakest) {
        /** A consumer the source touches. */
        public static Route direct(BlockPos pos) {
            return new Route(pos.immutable(), 0, 0, 0, Integer.MAX_VALUE, null);
        }

        /** The temperature it gets from a source at {@code temperature}. */
        public float temperature(float temperature) {
            return Math.min(temperature, cap) - drop;
        }
    }

    /** Every consumer a source's pipes reach, and the pipes themselves. */
    public record Routes(List<Route> routes, List<BlockPos> pipes) {
        public static final Routes NONE = new Routes(List.of(), List.of());
    }

    private record Step(BlockPos pos, int length, float drop, float loss) {}

    public static int maxLength() {
        return Config.HEAT_MAX_PIPE_LENGTH.getAsInt();
    }

    /** The consumers the pipes on any face of {@code source} reach. */
    public static Routes find(Level level, BlockPos source) {
        Map<BlockPos, Route> best = new LinkedHashMap<>();
        List<BlockPos> pipes = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        int max = maxLength();
        for (Direction face : Direction.values()) {
            BlockPos start = source.relative(face);
            if (seen.contains(start) || !(level.getBlockState(start).getBlock() instanceof HeatPipeBlock first)) continue;
            // One network: walk it breadth first, so each consumer is reached by its shortest path.
            List<Step> reached = new ArrayList<>();
            Map<BlockPos, Step> consumers = new HashMap<>();
            ArrayDeque<Step> queue = new ArrayDeque<>();
            queue.add(new Step(start, 1, first.drop(), first.loss()));
            seen.add(start);
            int cap = Integer.MAX_VALUE;
            HeatPipeBlock weakest = null;
            while (!queue.isEmpty()) {
                Step step = queue.poll();
                if (!(level.getBlockState(step.pos()).getBlock() instanceof HeatPipeBlock pipe)) continue;
                pipes.add(step.pos());
                reached.add(step);
                if (pipe.maxTemperature() < cap) {
                    cap = pipe.maxTemperature();
                    weakest = pipe;
                }
                for (Direction side : Direction.values()) {
                    BlockPos next = step.pos().relative(side);
                    if (next.equals(source)) continue;
                    if (level.getBlockState(next).getBlock() instanceof HeatPipeBlock nextPipe) {
                        if (step.length() < max && seen.add(next)) {
                            queue.add(new Step(next, step.length() + 1, step.drop() + nextPipe.drop(), step.loss() + nextPipe.loss()));
                        }
                    } else if (level.getBlockEntity(next) instanceof HeatConsumer && level.getBlockEntity(next) instanceof HeatPort port
                            && port.connectsHeat(side.getOpposite()) && !consumers.containsKey(next)) {
                        consumers.put(next.immutable(), step);
                    }
                }
            }
            for (Map.Entry<BlockPos, Step> entry : consumers.entrySet()) {
                Step step = entry.getValue();
                Route route = new Route(entry.getKey(), step.length(), step.drop(), Math.min(1.0f, step.loss()), cap, weakest);
                Route known = best.get(entry.getKey());
                if (known == null || route.temperature(Float.MAX_VALUE) - known.temperature(Float.MAX_VALUE) > 0
                        || route.temperature(Float.MAX_VALUE) == known.temperature(Float.MAX_VALUE) && route.pipes() < known.pipes()) {
                    best.put(entry.getKey(), route);
                }
            }
        }
        return new Routes(List.copyOf(best.values()), List.copyOf(pipes));
    }

    /**
     * Shares {@code heat} HU at {@code temperature} °C among the consumers on {@code routes} and returns
     * how much of it they took, before losses on the way.
     */
    public static int deliver(Level level, List<Route> routes, float temperature, int heat) {
        int n = routes.size();
        HeatConsumer[] consumers = new HeatConsumer[n];
        float[] temperatures = new float[n];
        int[] demand = new int[n];
        long total = 0;
        for (int i = 0; i < n; i++) {
            Route route = routes.get(i);
            if (!(level.getBlockEntity(route.pos()) instanceof HeatConsumer consumer)) continue;
            consumers[i] = consumer;
            temperatures[i] = route.temperature(temperature);
            demand[i] = heat <= 0 ? 0 : Math.max(0, consumer.heatDemand(temperatures[i]));
            total += demand[i];
        }
        int taken = 0;
        boolean journal = level.getGameTime() % 20 == 0;
        for (int i = 0; i < n; i++) {
            if (consumers[i] == null) continue;
            Route route = routes.get(i);
            // When the consumers ask for more than there is, each gets the same fraction of its request.
            int give = total <= heat ? demand[i] : (int) (demand[i] * (long) heat / total);
            int arrives = Math.max(0, Math.round(give * (1.0f - route.loss())));
            if (route.pipes() > 0) consumers[i].heatRoute(route.pipes(), temperature > route.cap() ? route.weakest() : null);
            int took = consumers[i].offerHeat(temperatures[i], arrives);
            if (took <= 0) continue;
            taken += give;
            if (journal && route.pipes() >= JOURNAL_PIPES) Journal.awardNear(level, route.pos(), Journal.HEAT_NETWORK);
        }
        return taken;
    }

    /** Lights or darkens the pipes, with a tick of metal as they heat or cool (spec 21.5). */
    public static void glow(Level level, List<BlockPos> pipes, boolean hot) {
        boolean changed = false;
        for (BlockPos pos : pipes) {
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof HeatPipeBlock) || state.getValue(HeatPipeBlock.HOT) == hot) continue;
            level.setBlock(pos, state.setValue(HeatPipeBlock.HOT, hot), Block.UPDATE_CLIENTS);
            if (!changed) {
                level.playSound(null, pos, Tier4Sounds.HEAT_PIPE_TICK.get(), SoundSource.BLOCKS, 0.4f, 0.9f + level.getRandom().nextFloat() * 0.2f);
                changed = true;
            }
        }
    }
}
