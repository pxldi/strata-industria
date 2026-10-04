package dev.strataindustria.power;

import dev.strataindustria.Config;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.registry.ModSounds;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jspecify.annotations.Nullable;

/**
 * Kinetic networks (spec 7.1 and 7.4). A network is recomputed only when one of its blocks is
 * placed, broken, loaded or unloaded, or a source changes speed: those mark a position dirty, and
 * at the end of the level tick each dirty position is flood-filled once.
 */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class KineticNetworks {
    private static final Map<ResourceKey<Level>, Set<BlockPos>> DIRTY = new HashMap<>();

    private KineticNetworks() {}

    /** Recompute the network through {@code pos} (or its neighbours, if {@code pos} is gone) at the end of this tick. */
    public static void markDirty(@Nullable Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) return;
        Set<BlockPos> dirty = DIRTY.computeIfAbsent(server.dimension(), k -> new HashSet<>());
        dirty.add(pos.immutable());
        for (Direction side : Direction.values()) dirty.add(pos.relative(side));
    }

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Set<BlockPos> dirty = DIRTY.remove(level.dimension());
        if (dirty == null) return;
        Set<BlockPos> done = new HashSet<>();
        for (BlockPos pos : dirty) {
            if (!done.contains(pos)) rebuild(level, pos, done);
        }
    }

    private static void rebuild(ServerLevel level, BlockPos start, Set<BlockPos> done) {
        if (!level.isLoaded(start) || !(level.getBlockEntity(start) instanceof Kinetic first)) return;
        int max = Config.KINETIC_MAX_NETWORK.get();
        Map<BlockPos, Kinetic> members = new LinkedHashMap<>();
        // Speed of each block relative to the start block, from the gear ratios crossed on the way.
        Map<BlockPos, Float> ratios = new HashMap<>();
        members.put(start, first);
        ratios.put(start, 1.0f);
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        boolean incomplete = false, tooLarge = false;
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            Kinetic block = members.get(pos);
            float ratio = ratios.get(pos);
            for (Direction side : Direction.values()) {
                if (!block.connects(side)) continue;
                BlockPos next = pos.relative(side);
                if (members.containsKey(next)) continue;
                if (!level.isLoaded(next)) {
                    incomplete = true;
                    continue;
                }
                if (!(level.getBlockEntity(next) instanceof Kinetic neighbour) || !neighbour.connects(side.getOpposite())) continue;
                if (members.size() >= max) {
                    tooLarge = true;
                    continue;
                }
                members.put(next, neighbour);
                ratios.put(next, ratio * throughRatio(block, pos, side, ratios, members));
                queue.add(next);
            }
        }
        done.addAll(members.keySet());

        // Network speed is the slowest turning source, measured at the start block (spec 7.1).
        float base = Float.MAX_VALUE;
        BlockPos limiter = null;
        int capacity = 0, turningSources = 0;
        for (var entry : members.entrySet()) {
            if (!(entry.getValue() instanceof KineticSource source)) continue;
            float speed = source.sourceSpeed();
            if (speed <= 0) continue;
            turningSources++;
            capacity += source.capacity();
            float atStart = speed / ratios.get(entry.getKey());
            if (atStart < base) {
                base = atStart;
                limiter = entry.getKey();
            }
        }
        if (turningSources < 2) limiter = null;
        int load = 0;
        if (turningSources > 0) {
            for (var entry : members.entrySet()) {
                if (entry.getValue() instanceof KineticConsumer consumer) {
                    load += Math.round(consumer.impact() * base * ratios.get(entry.getKey()));
                }
            }
        }

        KineticState.Status status;
        if (tooLarge) status = KineticState.Status.TOO_LARGE;
        else if (incomplete) status = KineticState.Status.INCOMPLETE;
        else if (turningSources == 0) status = KineticState.Status.IDLE;
        else if (load > capacity) status = KineticState.Status.OVERSTRESSED;
        else status = KineticState.Status.RUNNING;
        boolean running = status == KineticState.Status.RUNNING;

        boolean wasRunning = false, wasOverstressed = false;
        for (var entry : members.entrySet()) {
            KineticState state = entry.getValue().kinetic();
            wasRunning |= state.status() == KineticState.Status.RUNNING;
            wasOverstressed |= state.status() == KineticState.Status.OVERSTRESSED;
            float rpm = running ? base * ratios.get(entry.getKey()) : 0.0f;
            if (state.set(rpm, status, load, capacity, limiter) && entry.getValue() instanceof BlockEntity be) {
                be.setChanged();
                level.sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        if (status == KineticState.Status.OVERSTRESSED && !wasOverstressed) {
            level.playSound(null, start, ModSounds.KINETIC_OVERSTRESS.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
        }
        if (running && !wasRunning) Journal.awardNear(level, start, Journal.ROTATION);
    }

    /** The ratio between {@code pos} and its neighbour on {@code side}, through the block at {@code pos}. */
    private static float throughRatio(Kinetic block, BlockPos pos, Direction side, Map<BlockPos, Float> ratios,
            Map<BlockPos, Kinetic> members) {
        // Rotation enters a block from the neighbour it was reached through. For the start block,
        // or a block reached from several sides, the first side found decides; plain blocks are 1:1 anyway.
        for (Direction from : Direction.values()) {
            if (from == side || !block.connects(from)) continue;
            BlockPos prev = pos.relative(from);
            if (ratios.containsKey(prev) && members.get(prev) != null && !prev.equals(pos.relative(side))) {
                return block.ratio(from, side);
            }
        }
        return 1.0f;
    }
}
