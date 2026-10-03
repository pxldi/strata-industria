package dev.strataindustria.geology.worldgen;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
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
 * Visible clay near surface water (worldgen spec 6.3): discs of radius 2 to 5 that replace soil and
 * sand to a depth of 1 to 3, within 6 blocks of water, in temperate and warm biomes.
 */
public record ClayPatchFeature() implements Feature {
    public static final ClayPatchFeature INSTANCE = new ClayPatchFeature();
    public static final MapCodec<ClayPatchFeature> CODEC = MapCodec.unit(INSTANCE);
    private static final float CHANCE = 0.4f;

    @Override
    public MapCodec<ClayPatchFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        if (random.nextFloat() > CHANCE) return false;
        int minX = (origin.getX() >> 4) << 4, minZ = (origin.getZ() >> 4) << 4;
        int x = minX + 3 + random.nextInt(10), z = minZ + 3 + random.nextInt(10);
        int surface = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z) - 1;
        BlockPos centre = new BlockPos(x, surface, z);
        if (level.getBiome(centre).value().getBaseTemperature() < 0.3f) return false;
        if (!nearWater(level, centre)) return false;

        int radius = 2 + random.nextInt(4);
        boolean placed = false;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue;
                // Keep inside the current chunk.
                int px = x + dx, pz = z + dz;
                if (px < minX || px > minX + 15 || pz < minZ || pz > minZ + 15) continue;
                int top = level.getHeight(Heightmap.Types.OCEAN_FLOOR, px, pz) - 1;
                if (Math.abs(top - surface) > 3) continue;
                int depth = 1 + random.nextInt(3);
                for (int d = 0; d < depth; d++) {
                    BlockPos pos = new BlockPos(px, top - d, pz);
                    BlockState state = level.getBlockState(pos);
                    if (state.is(BlockTags.DIRT) || state.is(Blocks.SAND) || state.is(Blocks.GRAVEL)) {
                        level.setBlock(pos, Blocks.CLAY.defaultBlockState(), Block.UPDATE_CLIENTS);
                        placed = true;
                        // Plants on top of the old soil cannot stay on clay.
                        BlockPos above = pos.above();
                        if (d == 0 && !level.getBlockState(above).isAir() && level.getBlockState(above).getFluidState().isEmpty()) {
                            level.setBlock(above, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                        }
                    }
                }
            }
        }
        return placed;
    }

    private static boolean nearWater(WorldGenLevel level, BlockPos centre) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -6; dx <= 6; dx += 2) {
            for (int dz = -6; dz <= 6; dz += 2) {
                for (int dy = -1; dy <= 3; dy++) {
                    pos.set(centre.getX() + dx, centre.getY() + dy, centre.getZ() + dz);
                    if (level.getFluidState(pos).is(FluidTags.WATER)) return true;
                }
            }
        }
        return false;
    }
}
