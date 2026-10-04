package dev.strataindustria.geology.worldgen;

import com.mojang.serialization.MapCodec;
import dev.strataindustria.Config;
import dev.strataindustria.geology.GeologyContext;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.geology.VeinCells;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.util.Noise;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Surface pass: ore indicators above shallow veins (worldgen spec 6.1) and boulders of the local top rock
 * (redesign R1, {@link BoulderPlacement}). Only writes inside the current chunk.
 */
public record GroundCoverFeature() implements Feature {
    public static final GroundCoverFeature INSTANCE = new GroundCoverFeature();
    public static final MapCodec<GroundCoverFeature> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public MapCodec<GroundCoverFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        GeologyContext ctx = GeologyContext.get(level.getLevel());
        if (ctx == null) return false;
        int minX = (origin.getX() >> 4) << 4, minZ = (origin.getZ() >> 4) << 4;
        placeIndicators(level, ctx, minX, minZ);
        dev.strataindustria.flora.IndicatorPlants.generate(level, ctx, minX, minZ);
        BoulderPlacement.place(level, ctx, random, minX, minZ);
        return true;
    }

    private static void placeIndicators(WorldGenLevel level, GeologyContext ctx, int minX, int minZ) {
        int depth = Config.INDICATOR_DEPTH.get();
        double density = Config.INDICATOR_DENSITY.get();
        for (VeinCells.Vein vein : ctx.veins().around(minX + 8, minZ + 8)) {
            if (!vein.type().indicators() || !vein.type().placesOre()) continue;
            if (vein.y() + vein.verticalReach() < vein.surfaceY() - depth) continue;
            int reach = vein.radiusH() + 4;
            if (!vein.intersects(minX - 4, minZ - 4, minX + 19, minZ + 19)) continue;
            int count = (int) Math.round(Math.clamp(vein.estimatedOreBlocks() / 40.0, 3, 12) * density);
            for (int k = 0; k < count; k++) {
                long h = Noise.hash(vein.seed(), 500 + k);
                int x = vein.x() - reach + (int) (Noise.unit(Noise.hash(h, 1)) * (2 * reach + 1));
                int z = vein.z() - reach + (int) (Noise.unit(Noise.hash(h, 2)) * (2 * reach + 1));
                if (x < minX || x > minX + 15 || z < minZ || z > minZ + 15) continue;
                OreMineral mineral = vein.type().pickMineral(Noise.unit(Noise.hash(h, 3)));
                placeOnSurface(level, x, z, ModBlocks.SMALL_ORES.get(mineral).get().defaultBlockState());
            }
        }
    }

    /** Places a ground cover block on natural ground at the top of a column, if there is room. */
    static boolean placeOnSurface(WorldGenLevel level, int x, int z, BlockState state) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos pos = new BlockPos(x, y, z);
        BlockState here = level.getBlockState(pos);
        if (!(here.isAir() || here.is(Blocks.SHORT_GRASS) || here.is(Blocks.SNOW))) return false;
        if (!here.getFluidState().isEmpty()) return false;
        BlockPos below = pos.below();
        BlockState ground = level.getBlockState(below);
        if (ground.is(BlockTags.LEAVES) || !ground.isFaceSturdy(level, below, Direction.UP)) return false;
        if (!ground.getFluidState().isEmpty()) return false;
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        return true;
    }
}
