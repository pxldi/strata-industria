package dev.strataindustria.fluid;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

/**
 * Moving fluid through pipes (tier 4 spec 9.2). A source finds the pipe network on one of its faces and
 * pushes into it; the network splits the flow evenly among the ports that accept, limited by the
 * slowest pipe, and refuses fluid hotter than its weakest pipe is rated for.
 */
public final class FluidPipes {
    /** Most pipe blocks one network searches. */
    public static final int MAX_PIPES = 256;
    /** Red warning dust at a pipe that refuses hot fluid. */
    private static final int WARNING = 0xD83A2A;

    private FluidPipes() {}

    /** A port a network reaches, and the side of it the pipe touches. */
    public record Endpoint(BlockPos pos, Direction side) {}

    /**
     * What a source's face is joined to.
     *
     * @param firstPipe the pipe touching the source, null when a port touches it directly
     */
    public record Network(List<BlockPos> pipes, List<Endpoint> ports, List<BlockPos> gauges, int maxTemperature, int throughput,
            @Nullable BlockPos firstPipe) {
        public static final Network NONE = new Network(List.of(), List.of(), List.of(), Integer.MAX_VALUE, 0, null);

        public boolean isEmpty() {
            return ports.isEmpty();
        }
    }

    /** What a push did: how much moved, and whether the pipes refused the fluid as too hot. */
    public record Push(int moved, boolean refused) {
        public static final Push NOTHING = new Push(0, false);
    }

    /** The network on {@code face} of {@code source}. A port right against the face is a network of its own. */
    public static Network find(Level level, BlockPos source, Direction face) {
        BlockPos start = source.relative(face);
        if (!(level.getBlockState(start).getBlock() instanceof FluidPipeBlock)) {
            if (level.getBlockEntity(start) instanceof FluidPort port && port.connectsFluid(face.getOpposite())) {
                return new Network(List.of(), List.of(new Endpoint(start, face.getOpposite())), List.of(), Integer.MAX_VALUE, Integer.MAX_VALUE, null);
            }
            return Network.NONE;
        }
        List<BlockPos> pipes = new ArrayList<>();
        List<Endpoint> ports = new ArrayList<>();
        List<BlockPos> gauges = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start);
        int maxTemperature = Integer.MAX_VALUE, throughput = Integer.MAX_VALUE;
        while (!queue.isEmpty() && pipes.size() < MAX_PIPES) {
            BlockPos pos = queue.poll();
            BlockState state = level.getBlockState(pos);
            // A shut valve is a wall: nothing passes it.
            if (!(state.getBlock() instanceof FluidPipeBlock pipe) || !ValveBlock.passes(state)) continue;
            pipes.add(pos);
            if (pipe instanceof PressureGaugeBlock) gauges.add(pos);
            maxTemperature = Math.min(maxTemperature, pipe.maxTemperature());
            throughput = Math.min(throughput, pipe.throughput());
            for (Direction side : Direction.values()) {
                BlockPos next = pos.relative(side);
                if (next.equals(source) || !seen.add(next)) continue;
                if (level.getBlockState(next).getBlock() instanceof FluidPipeBlock) queue.add(next);
                else if (level.getBlockEntity(next) instanceof FluidPort port && port.connectsFluid(side.getOpposite())) {
                    ports.add(new Endpoint(next, side.getOpposite()));
                    seen.remove(next); // another pipe may reach the same port on another face
                }
            }
        }
        return new Network(pipes, ports, gauges, maxTemperature, throughput, start);
    }

    /**
     * Pushes up to {@code amount} mB into the network, split evenly among the ports that take it. Fluid
     * hotter than the network's weakest pipe is refused, with red dust at the pipe by the source.
     */
    public static Push push(Level level, Network network, Fluid fluid, int amount, float temperature, float pressure) {
        if (network.isEmpty() || amount <= 0) return Push.NOTHING;
        if (temperature > network.maxTemperature()) {
            if (network.firstPipe() != null && level instanceof ServerLevel server && level.getGameTime() % 10 == 0) {
                BlockPos p = network.firstPipe();
                server.sendParticles(new DustParticleOptions(WARNING, 1.0f), p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5,
                        3, 0.2, 0.2, 0.2, 0.0);
            }
            return new Push(0, true);
        }
        amount = Math.min(amount, network.throughput());
        List<Endpoint> ports = network.ports();
        int n = ports.size();
        FluidPort[] handlers = new FluidPort[n];
        int[] room = new int[n];
        for (int i = 0; i < n; i++) {
            Endpoint e = ports.get(i);
            if (level.getBlockEntity(e.pos()) instanceof FluidPort port) {
                handlers[i] = port;
                room[i] = Math.max(0, port.fill(e.side(), fluid, amount, pressure, true));
            }
        }
        // Even split: ports that want less than their share give the rest to the others.
        int[] give = new int[n];
        int left = amount;
        boolean progress = true;
        while (left > 0 && progress) {
            progress = false;
            int wanting = 0;
            for (int i = 0; i < n; i++) if (room[i] > give[i]) wanting++;
            if (wanting == 0) break;
            int share = Math.max(1, left / wanting);
            for (int i = 0; i < n && left > 0; i++) {
                int more = Math.min(share, Math.min(room[i] - give[i], left));
                if (more <= 0) continue;
                give[i] += more;
                left -= more;
                progress = true;
            }
        }
        int moved = 0;
        for (int i = 0; i < n; i++) {
            if (give[i] > 0 && handlers[i] != null) moved += handlers[i].fill(ports.get(i).side(), fluid, give[i], pressure, false);
        }
        if (moved > 0) {
            for (BlockPos pipe : network.gauges()) {
                if (level.getBlockEntity(pipe) instanceof PressureGaugeBlockEntity gauge) gauge.read(fluid, pressure, moved, network.throughput());
            }
        }
        return new Push(moved, false);
    }
}
