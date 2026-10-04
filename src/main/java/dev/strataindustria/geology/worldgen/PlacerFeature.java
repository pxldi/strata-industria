package dev.strataindustria.geology.worldgen;

import com.mojang.serialization.MapCodec;
import dev.strataindustria.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * River placers (worldgen spec 6.5): patches of gold- and tin-bearing gravel and sand on river beds,
 * found by their glints through shallow water and worked with a washing pan.
 */
public record PlacerFeature() implements Feature {
    public static final PlacerFeature INSTANCE = new PlacerFeature();
    public static final MapCodec<PlacerFeature> CODEC = MapCodec.unit(INSTANCE);
    /** About one patch per 4 x 4 chunks of river. */
    private static final float CHANCE = 1 / 16f;

    @Override
    public MapCodec<PlacerFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        if (random.nextFloat() > CHANCE) return false;
        int minX = (origin.getX() >> 4) << 4, minZ = (origin.getZ() >> 4) << 4;
        int x = minX + 4 + random.nextInt(8), z = minZ + 4 + random.nextInt(8);
        int bed = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z) - 1;
        BlockPos centre = new BlockPos(x, bed, z);
        if (!level.getBiome(centre).is(BiomeTags.IS_RIVER)) return false;

        int radius = 2 + random.nextInt(3);
        boolean placed = false;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue;
                int px = x + dx, pz = z + dz;
                if (px < minX || px > minX + 15 || pz < minZ || pz > minZ + 15) continue;
                int top = level.getHeight(Heightmap.Types.OCEAN_FLOOR, px, pz) - 1;
                BlockPos surfacePos = new BlockPos(px, top, pz);
                if (!wet(level, surfacePos)) continue;
                int depth = 1 + random.nextInt(2);
                for (int d = 0; d < depth; d++) {
                    BlockPos pos = surfacePos.below(d);
                    BlockState state = level.getBlockState(pos);
                    BlockState placer = state.is(Blocks.GRAVEL) ? ModBlocks.PLACER_GRAVEL.get().defaultBlockState()
                            : state.is(Blocks.SAND) ? ModBlocks.PLACER_SAND.get().defaultBlockState() : null;
                    if (placer == null) continue;
                    level.setBlock(pos, placer, Block.UPDATE_CLIENTS);
                    placed = true;
                }
            }
        }
        return placed;
    }

    /** Under water, or at the water's edge. */
    private static boolean wet(WorldGenLevel level, BlockPos pos) {
        if (level.getFluidState(pos.above()).is(FluidTags.WATER)) return true;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (level.getFluidState(pos.relative(side).above()).is(FluidTags.WATER)) return true;
        }
        return false;
    }
}
