package dev.strataindustria.geology.worldgen;

import com.mojang.serialization.MapCodec;
import dev.strataindustria.geology.OreGrade;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.neoforge.common.Tags;

/**
 * Bog iron (worldgen spec 6.4): discs of rusty limonite soil in swamps, and more rarely beside rivers
 * and lakes elsewhere, with a few limonite nodules lying on top as the tell.
 */
public record BogIronFeature() implements Feature {
    public static final BogIronFeature INSTANCE = new BogIronFeature();
    public static final MapCodec<BogIronFeature> CODEC = MapCodec.unit(INSTANCE);
    /** About one patch per 2 x 2 chunks in swamps. */
    private static final float SWAMP_CHANCE = 0.25f;
    /** About one patch per 6 x 6 chunks by water elsewhere. */
    private static final float WATERSIDE_CHANCE = 1 / 36f;

    @Override
    public MapCodec<BogIronFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        int minX = (origin.getX() >> 4) << 4, minZ = (origin.getZ() >> 4) << 4;
        int x = minX + 4 + random.nextInt(8), z = minZ + 4 + random.nextInt(8);
        int surface = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z) - 1;
        BlockPos centre = new BlockPos(x, surface, z);
        Holder<Biome> biome = level.getBiome(centre);
        if (biome.value().getBaseTemperature() < 0.15f) return false;
        boolean swamp = biome.is(Tags.Biomes.IS_SWAMP);
        if (random.nextFloat() > (swamp ? SWAMP_CHANCE : WATERSIDE_CHANCE)) return false;
        if (!swamp && !ClayPatchFeature.nearWater(level, centre)) return false;

        int radius = 2 + random.nextInt(3);
        boolean placed = false;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius + random.nextInt(2)) continue;
                int px = x + dx, pz = z + dz;
                if (px < minX || px > minX + 15 || pz < minZ || pz > minZ + 15) continue;
                int top = level.getHeight(Heightmap.Types.OCEAN_FLOOR, px, pz) - 1;
                if (Math.abs(top - surface) > 2) continue;
                int depth = 1 + random.nextInt(3);
                for (int d = 0; d < depth; d++) {
                    BlockPos pos = new BlockPos(px, top - d, pz);
                    if (!isBogSoil(level.getBlockState(pos))) continue;
                    level.setBlock(pos, ModBlocks.BOG_IRON.get().withGrade(grade(random)), Block.UPDATE_CLIENTS);
                    placed = true;
                    BlockPos above = pos.above();
                    BlockState over = level.getBlockState(above);
                    if (d == 0 && !over.isAir() && over.getFluidState().isEmpty() && !over.is(BlockTags.LOGS)) {
                        level.setBlock(above, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
        if (placed) {
            int nodules = 1 + random.nextInt(3);
            for (int i = 0; i < nodules; i++) {
                int px = Math.clamp(x - radius + random.nextInt(2 * radius + 1), minX, minX + 15);
                int pz = Math.clamp(z - radius + random.nextInt(2 * radius + 1), minZ, minZ + 15);
                GroundCoverFeature.placeOnSurface(level, px, pz,
                        ModBlocks.SMALL_ORES.get(OreMineral.LIMONITE).get().defaultBlockState());
            }
        }
        return placed;
    }

    private static boolean isBogSoil(BlockState state) {
        return state.is(BlockTags.DIRT) || state.is(Blocks.MUD) || state.is(Blocks.CLAY);
    }

    /** 30% poor, 60% normal, 10% rich: bog ore is patchy, not graded by distance. */
    private static OreGrade grade(RandomSource random) {
        float roll = random.nextFloat();
        return roll < 0.3f ? OreGrade.POOR : roll < 0.9f ? OreGrade.NORMAL : OreGrade.RICH;
    }
}
