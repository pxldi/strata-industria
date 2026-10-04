package dev.strataindustria.structure;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Takes whole trees down where a structure is built, so that no trunk is left cut in half, no crown hangs
 * in the air and no leaf is left to decay. A tree is any connected run of logs and leaves that touches the
 * cleared area; it goes in one piece, including the parts that reach out of the area.
 */
final class TreeClearing {
    /** How far from the cleared area a tree's crown is followed, in blocks. */
    static final int REACH = 9;
    /** Blocks a single sweep gives up at, so a jungle cannot hold up world generation. */
    private static final int LIMIT = 40_000;
    /** Leaves lose their log at this many steps (vanilla's decay distance). */
    private static final int LEAF_RANGE = 6;
    /** A run with fewer logs than this and no leaves is a post or a chopping block, not a tree. */
    private static final int MIN_TRUNK = 3;

    private TreeClearing() {}

    /**
     * Removes every tree that touches the box {@code x0..x1, y0..y1, z0..z1}, then every leaf in {@code chunkBB}
     * that has lost its log. Blocks for which {@code keep} is true (the structure's own) are never removed.
     */
    static void clear(WorldGenLevel level, BoundingBox chunkBB, int x0, int z0, int x1, int z1, int y0, int y1,
            Predicate<BlockPos> keep) {
        Set<BlockPos> visited = new HashSet<>();
        List<BlockPos> doomed = new ArrayList<>();
        BlockPos.MutableBlockPos scan = new BlockPos.MutableBlockPos();
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                if (!level.hasChunk(x >> 4, z >> 4)) continue;
                for (int y = y0; y <= y1; y++) {
                    scan.set(x, y, z);
                    if (!isTree(level.getBlockState(scan)) || keep.test(scan) || visited.contains(scan)) continue;
                    List<BlockPos> run = new ArrayList<>();
                    int logs = 0;
                    boolean leaves = false;
                    ArrayDeque<BlockPos> queue = new ArrayDeque<>();
                    BlockPos start = scan.immutable();
                    queue.add(start);
                    visited.add(start);
                    while (!queue.isEmpty() && visited.size() < LIMIT) {
                        BlockPos at = queue.poll();
                        run.add(at);
                        if (level.getBlockState(at).is(BlockTags.LOGS)) logs++;
                        else leaves = true;
                        for (Direction d : Direction.values()) {
                            BlockPos next = at.relative(d);
                            if (next.getX() < x0 - REACH || next.getX() > x1 + REACH || next.getZ() < z0 - REACH || next.getZ() > z1 + REACH
                                    || next.getY() < y0 - 2 || next.getY() > y1 + 48 || visited.contains(next)) continue;
                            if (!level.hasChunk(next.getX() >> 4, next.getZ() >> 4)) continue;
                            if (!isTree(level.getBlockState(next)) || keep.test(next)) continue;
                            visited.add(next);
                            queue.add(next);
                        }
                    }
                    if (leaves || logs >= MIN_TRUNK) doomed.addAll(run);
                }
            }
        }
        for (BlockPos pos : doomed) remove(level, pos);
        int sweep = REACH + LEAF_RANGE + 1;
        sweepLeaves(level, chunkBB, x0 - sweep, z0 - sweep, x1 + sweep, z1 + sweep, y0 - 2, y1 + 48);
    }

    private static boolean isTree(BlockState state) {
        return state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES);
    }

    private static void remove(WorldGenLevel level, BlockPos pos) {
        if (!level.ensureCanWrite(pos)) return;
        level.setBlock(pos, level.getFluidState(pos).createLegacyBlock(), Block.UPDATE_CLIENTS);
    }

    /** Removes leaves of this chunk that no log reaches any more: they would only rot away in front of the player. */
    private static void sweepLeaves(WorldGenLevel level, BoundingBox chunkBB, int x0, int z0, int x1, int z1, int y0, int y1) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        List<BlockPos> orphans = new ArrayList<>();
        for (int x = Math.max(x0, chunkBB.minX()); x <= Math.min(x1, chunkBB.maxX()); x++) {
            for (int z = Math.max(z0, chunkBB.minZ()); z <= Math.min(z1, chunkBB.maxZ()); z++) {
                for (int y = Math.max(y0, chunkBB.minY()); y <= Math.min(y1, chunkBB.maxY()); y++) {
                    pos.set(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (!state.is(BlockTags.LEAVES)) continue;
                    if (state.hasProperty(LeavesBlock.PERSISTENT) && state.getValue(LeavesBlock.PERSISTENT)) continue;
                    if (!supported(level, pos)) orphans.add(pos.immutable());
                }
            }
        }
        for (BlockPos orphan : orphans) remove(level, orphan);
    }

    /** Whether a log lies within decay range of a leaf, counting steps through other leaves. */
    static boolean supported(WorldGenLevel level, BlockPos leaf) {
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> frontier = new ArrayDeque<>();
        frontier.add(leaf);
        seen.add(leaf);
        for (int step = 1; step <= LEAF_RANGE && !frontier.isEmpty(); step++) {
            ArrayDeque<BlockPos> next = new ArrayDeque<>();
            for (BlockPos at : frontier) {
                for (Direction d : Direction.values()) {
                    BlockPos n = at.relative(d);
                    if (!seen.add(n) || !level.hasChunk(n.getX() >> 4, n.getZ() >> 4)) continue;
                    BlockState state = level.getBlockState(n);
                    if (state.is(BlockTags.LOGS)) return true;
                    if (state.is(BlockTags.LEAVES)) next.add(n);
                }
            }
            frontier = next;
        }
        return false;
    }
}
