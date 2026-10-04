package dev.strataindustria.transport.signal;

import dev.strataindustria.transport.rail.MineTubEntity;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * The rules of block signalling (outposts spec 9.4). A signal stands on the right of the track, looking the way the
 * traffic it guards travels. The track from the signal's rail to the next signal that guards the same way is a block;
 * while a vehicle is in it the signal stands at stop and no lead passes it.
 */
public final class Signals {
    /** Most rail blocks a block may run before it simply ends. */
    public static final int MAX_BLOCK = 128;
    /** How far in front of the signal's rail centre a held lead comes to rest. */
    public static final double STOP_LINE = 0.3;
    /** Blocks ahead a lead looks for a stop signal, to ease down in time. */
    public static final int LOOKAHEAD = 7;

    private Signals() {}

    // ---------------------------------------------------------------- where things are

    /** The rail block a signal at {@code signal} watches: the one on its left, looking along {@code facing}. */
    public static @Nullable BlockPos railOf(BlockGetter level, BlockPos signal, Direction facing) {
        BlockPos side = signal.relative(facing.getCounterClockWise());
        for (BlockPos probe : new BlockPos[] {side, side.below(), side.above()}) {
            if (BaseRailBlock.isRail(level.getBlockState(probe))) return probe;
        }
        return null;
    }

    /** The signal beside {@code rail} that guards traffic travelling {@code travel}, or null. */
    public static @Nullable BlockPos signalFor(BlockGetter level, BlockPos rail, Direction travel) {
        BlockPos side = rail.relative(travel.getClockWise());
        for (BlockPos probe : new BlockPos[] {side, side.above(), side.below()}) {
            BlockState state = level.getBlockState(probe);
            if (state.getBlock() instanceof SignalBlock && state.getValue(SignalBlock.FACING) == travel && rail.equals(railOf(level, probe, travel))) {
                return probe;
            }
        }
        return null;
    }

    /** True when a signal beside {@code rail} stands at stop against traffic going {@code travel}. */
    public static boolean stopAt(BlockGetter level, BlockPos rail, Direction travel) {
        BlockPos signal = signalFor(level, rail, travel);
        return signal != null && !level.getBlockState(signal).getValue(SignalBlock.CLEAR);
    }

    // ---------------------------------------------------------------- blocks

    /**
     * The rail blocks of the block that starts after the signal's own rail, in order, ending at the rail of the next
     * signal guarding the same way. It also ends where the track ends, at a chunk that is not loaded, or after
     * {@link #MAX_BLOCK} blocks.
     */
    public static java.util.List<BlockPos> blockAhead(Level level, BlockPos start, Direction travel) {
        java.util.List<BlockPos> path = new java.util.ArrayList<>();
        LongOpenHashSet seen = new LongOpenHashSet();
        BlockPos pos = start;
        BlockPos from = null;
        Direction going = travel;
        seen.add(pos.asLong());
        for (int step = 0; step < MAX_BLOCK; step++) {
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof BaseRailBlock rail)) break;
            var exits = AbstractMinecart.exits(state.getValue(rail.getShapeProperty()));
            Vec3i best = null;
            double bestScore = -Double.MAX_VALUE;
            for (Vec3i exit : new Vec3i[] {exits.getFirst(), exits.getSecond()}) {
                BlockPos target = pos.offset(exit);
                if (from != null && (target.equals(from) || target.below().equals(from) || target.above().equals(from))) continue;
                double score = exit.getX() * going.getStepX() + exit.getZ() * going.getStepZ();
                if (score > bestScore) {
                    bestScore = score;
                    best = exit;
                }
            }
            if (best == null || from == null && bestScore <= 0) break;
            BlockPos next = null;
            for (BlockPos probe : new BlockPos[] {pos.offset(best), pos.offset(best).below(), pos.offset(best).above()}) {
                if (level.hasChunkAt(probe) && BaseRailBlock.isRail(level.getBlockState(probe))) {
                    next = probe;
                    break;
                }
            }
            if (next == null || !seen.add(next.asLong())) break;
            if (best.getX() != 0 || best.getZ() != 0) going = Direction.getApproximateNearest(best.getX(), 0, best.getZ());
            path.add(next);
            if (signalFor(level, next, going) != null) break;
            from = pos;
            pos = next;
        }
        return path;
    }

    /** Whether any vehicle stands on the rail blocks of {@code path}. */
    public static boolean occupied(Level level, java.util.List<BlockPos> path) {
        if (path.isEmpty()) return false;
        LongOpenHashSet blocks = new LongOpenHashSet();
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos pos : path) {
            blocks.add(pos.asLong());
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }
        AABB box = new AABB(minX - 1, minY - 1, minZ - 1, maxX + 2, maxY + 3, maxZ + 2);
        for (MineTubEntity vehicle : level.getEntitiesOfClass(MineTubEntity.class, box)) {
            BlockPos at = vehicle.blockPosition();
            if (blocks.contains(at.asLong()) || blocks.contains(at.below().asLong())) return true;
        }
        return false;
    }

    /** Whether the block a signal guards, from its rail ahead, has a vehicle in it. */
    public static boolean blockOccupied(ServerLevel level, BlockPos rail, Direction travel) {
        return occupied(level, blockAhead(level, rail, travel));
    }

    /** Whether the signal at {@code signal} can see track, for the placing message. */
    public static boolean watchesTrack(LevelReader level, BlockPos signal, Direction facing) {
        return railOf(level, signal, facing) != null;
    }
}
