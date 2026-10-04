package dev.strataindustria.felling;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Finds the tree a log belongs to: the logs above the struck base log, and the leaves the world grew on them.
 * Leaves a player placed are persistent, so a log cabin or a hedge is never mistaken for a tree.
 */
public final class TreeScan {
    static final int MAX_LOGS = 400;
    static final int MAX_LEAVES = 700;
    static final int REACH = 9;
    static final int HEIGHT = 48;
    static final int LEAF_REACH = 7;
    /** Fewer natural leaves than this and the logs are somebody's building. */
    static final int MIN_LEAVES = 2;

    /** {@code base} are the logs standing on the ground at the foot of the trunk, {@code anchor} names the tree. */
    public record Tree(BlockPos start, BlockPos anchor, List<BlockPos> base, List<BlockPos> logs, List<BlockPos> leaves) {}

    private TreeScan() {}

    /** The tree whose foot is {@code start}, or null when that log is not the bottom log of a natural tree. */
    public static Tree scan(Level level, BlockPos start) {
        if (!level.getBlockState(start).is(BlockTags.LOGS)) return null;
        BlockPos below = start.below();
        BlockState ground = level.getBlockState(below);
        if (ground.is(BlockTags.LOGS) || !ground.isFaceSturdy(level, below, Direction.UP)) return null;

        List<BlockPos> logs = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        seen.add(start);
        queue.add(start);
        while (!queue.isEmpty() && logs.size() < MAX_LOGS) {
            BlockPos at = queue.poll();
            logs.add(at);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos next = at.offset(dx, dy, dz);
                        if (next.getY() < start.getY() || next.getY() - start.getY() > HEIGHT) continue;
                        if (Math.abs(next.getX() - start.getX()) > REACH || Math.abs(next.getZ() - start.getZ()) > REACH) continue;
                        if (!seen.add(next)) continue;
                        if (level.getBlockState(next).is(BlockTags.LOGS)) queue.add(next);
                    }
                }
            }
        }

        List<BlockPos> leaves = new ArrayList<>();
        Set<BlockPos> leafSeen = new HashSet<>();
        ArrayDeque<BlockPos> front = new ArrayDeque<>();
        ArrayDeque<Integer> depth = new ArrayDeque<>();
        for (BlockPos log : logs) {
            front.add(log);
            depth.add(0);
            leafSeen.add(log);
        }
        while (!front.isEmpty() && leaves.size() < MAX_LEAVES) {
            BlockPos at = front.poll();
            int d = depth.poll();
            if (d >= LEAF_REACH) continue;
            for (Direction dir : Direction.values()) {
                BlockPos next = at.relative(dir);
                if (!leafSeen.add(next)) continue;
                BlockState state = level.getBlockState(next);
                if (!state.is(BlockTags.LEAVES) || isPlaced(state)) continue;
                leaves.add(next);
                front.add(next);
                depth.add(d + 1);
            }
        }
        if (leaves.size() < MIN_LEAVES) return null;

        List<BlockPos> base = new ArrayList<>();
        BlockPos anchor = start;
        for (BlockPos log : logs) {
            if (log.getY() != start.getY()) continue;
            BlockPos under = log.below();
            if (!level.getBlockState(under).isFaceSturdy(level, under, Direction.UP)) continue;
            base.add(log);
            if (log.asLong() < anchor.asLong()) anchor = log;
        }
        return new Tree(start, anchor, base, logs, leaves);
    }

    private static boolean isPlaced(BlockState leaf) {
        return leaf.hasProperty(LeavesBlock.PERSISTENT) && leaf.getValue(LeavesBlock.PERSISTENT);
    }
}
