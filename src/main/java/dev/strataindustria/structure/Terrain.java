package dev.strataindustria.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** Reading and reshaping the ground under a structure, one column at a time. */
final class Terrain {
    /** How far up and down from the expected height to look for the ground. */
    static final int SEARCH_UP = 16, SEARCH_DOWN = 24;
    /** Deepest a foundation or a fill reaches before giving up. */
    static final int MAX_FILL = 12;

    private Terrain() {}

    /** Solid natural ground: not air, water, plants, logs or leaves. */
    static boolean isGround(BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty() && state.isSolid()
                && !state.is(BlockTags.LEAVES) && !state.is(BlockTags.LOGS);
    }

    /** Trees, plants and snow that a builder would clear away. */
    static boolean isGrowth(BlockState state) {
        if (state.isAir() || !state.getFluidState().isEmpty()) return false;
        return state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS) || !state.isSolid() || state.is(Blocks.SNOW);
    }

    /** Height of the top solid ground block of a column, searched around {@code near}. */
    static int surface(WorldGenLevel level, int x, int z, int near) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, near + SEARCH_UP, z);
        for (int y = near + SEARCH_UP; y >= near - SEARCH_DOWN; y--) {
            if (isGround(level.getBlockState(pos.setY(y)))) return y;
        }
        return near;
    }

    /**
     * Moves the ground of a column from {@code natural} to {@code target}: clears ground and growth above the
     * target up to {@code clearTop}, keeps the column's own top block (grass, sand, stone) on top, and fills
     * underneath with the matching soil.
     */
    static void setSurface(WorldGenLevel level, BoundingBox chunkBB, int x, int z, int natural, int target, int clearTop) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, natural, z);
        BlockState skin = level.getBlockState(pos);
        if (!isGround(skin)) skin = Blocks.GRASS_BLOCK.defaultBlockState();
        BlockState fill = skin.is(BlockTags.SAND) || skin.is(BlockTags.BASE_STONE_OVERWORLD) ? skin : Blocks.DIRT.defaultBlockState();
        for (int y = target + 1; y <= clearTop; y++) {
            pos.setY(y);
            if (!chunkBB.isInside(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (isGround(state) || isGrowth(state)) level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        pos.setY(target);
        if (chunkBB.isInside(pos)) level.setBlock(pos, skin, Block.UPDATE_CLIENTS);
        for (int y = target - 1; y >= target - MAX_FILL; y--) {
            pos.setY(y);
            if (!chunkBB.isInside(pos)) break;
            if (y < natural && isGround(level.getBlockState(pos))) break;
            level.setBlock(pos, fill, Block.UPDATE_CLIENTS);
        }
    }

    /** Fills air and water under a block down to the ground, so nothing hangs in the air. */
    static void foundation(WorldGenLevel level, BlockPos start, BoundingBox chunkBB, BlockState fill) {
        BlockPos.MutableBlockPos pos = start.mutable();
        for (int i = 0; i < MAX_FILL; i++) {
            if (!chunkBB.isInside(pos) || isGround(level.getBlockState(pos))) return;
            level.setBlock(pos, fill, Block.UPDATE_CLIENTS);
            pos.move(0, -1, 0);
        }
    }
}
