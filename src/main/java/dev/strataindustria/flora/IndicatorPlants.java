package dev.strataindustria.flora;

import dev.strataindustria.Config;
import dev.strataindustria.geology.GeologyContext;
import dev.strataindustria.geology.Rock;
import dev.strataindustria.geology.VeinCells;
import dev.strataindustria.util.Noise;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Worldgen for the indicator plants: patches that grow only over shallow veins of their ore, horsetail along
 * placer banks, and locoweed colonies on bare granite. Only writes inside the current chunk.
 */
public final class IndicatorPlants {
    /** Veins whose top lies deeper below the surface than this have no plants over them. */
    static final int DEPTH = 40;
    /** Locoweed colonies sit on a grid of this many blocks; most cells have none. */
    public static final int COLONY_CELL = 96;
    private static final double COLONY_CHANCE = 0.12;
    private static final int COLONY_SALT = 0x10C0;

    private IndicatorPlants() {}

    /** The centre of a locoweed colony in this cell, or null. Tier 8 uranium veins are keyed to these. */
    public static BlockPos colony(long seed, int cellX, int cellZ) {
        long h = Noise.hash(seed, COLONY_SALT, cellX, cellZ);
        if (Noise.unit(h) > COLONY_CHANCE) return null;
        int x = cellX * COLONY_CELL + 8 + (int) (Noise.unit(Noise.hash(h, 1)) * (COLONY_CELL - 16));
        int z = cellZ * COLONY_CELL + 8 + (int) (Noise.unit(Noise.hash(h, 2)) * (COLONY_CELL - 16));
        return new BlockPos(x, 0, z);
    }

    /** Places every plant that belongs in this chunk. */
    public static void generate(WorldGenLevel level, GeologyContext ctx, int minX, int minZ) {
        double density = Config.INDICATOR_DENSITY.get();
        for (VeinCells.Vein vein : ctx.veins().around(minX + 8, minZ + 8)) {
            IndicatorPlant plant = IndicatorPlant.forVein(vein.type());
            if (plant == null) continue;
            if (vein.y() + vein.verticalReach() < vein.surfaceY() - DEPTH) continue;
            int reach = vein.radiusH() + 5;
            if (!vein.intersects(minX - 5, minZ - 5, minX + 20, minZ + 20)) continue;
            int count = (int) Math.round(Math.clamp(vein.estimatedOreBlocks() / 25.0, 6, 22) * density);
            for (int k = 0; k < count; k++) {
                long h = Noise.hash(vein.seed(), 700 + k);
                int dx = (int) ((Noise.unit(Noise.hash(h, 1)) * 2 - 1) * reach);
                int dz = (int) ((Noise.unit(Noise.hash(h, 2)) * 2 - 1) * reach);
                if (dx * dx + dz * dz > reach * reach) continue;
                place(level, minX, minZ, vein.x() + dx, vein.z() + dz, plant);
            }
        }

        long seed = level.getSeed();
        int cellX = Math.floorDiv(minX + 8, COLONY_CELL), cellZ = Math.floorDiv(minZ + 8, COLONY_CELL);
        for (int cx = cellX - 1; cx <= cellX + 1; cx++) {
            for (int cz = cellZ - 1; cz <= cellZ + 1; cz++) {
                BlockPos centre = colony(seed, cx, cz);
                if (centre == null || Math.abs(centre.getX() - minX - 8) > 20 || Math.abs(centre.getZ() - minZ - 8) > 20) continue;
                for (int k = 0; k < 10; k++) {
                    long h = Noise.hash(seed, COLONY_SALT + 1, centre.getX() * 31L + centre.getZ(), k);
                    int x = centre.getX() + (int) ((Noise.unit(Noise.hash(h, 1)) * 2 - 1) * 5);
                    int z = centre.getZ() + (int) ((Noise.unit(Noise.hash(h, 2)) * 2 - 1) * 5);
                    int surface = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z);
                    if (ctx.sampler().column(x, z, surface).province().top() != Rock.GRANITE) continue;
                    place(level, minX, minZ, x, z, IndicatorPlant.LOCOWEED);
                }
            }
        }
    }

    /** A few horsetails on the bank beside a river placer. */
    public static void horsetailBank(WorldGenLevel level, RandomSource random, int minX, int minZ, int x, int z, int radius) {
        int reach = radius + 6;
        for (int k = 0; k < 7; k++) {
            int px = x + random.nextInt(2 * reach + 1) - reach, pz = z + random.nextInt(2 * reach + 1) - reach;
            place(level, minX, minZ, px, pz, IndicatorPlant.HORSETAIL);
        }
    }

    /** Puts a plant on the ground at the top of a column, inside the chunk, if the ground suits it and there is room. */
    static boolean place(WorldGenLevel level, int minX, int minZ, int x, int z, IndicatorPlant plant) {
        if (x < minX || x > minX + 15 || z < minZ || z > minZ + 15) return false;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return placeAt(level, new BlockPos(x, y, z), plant);
    }

    /** Puts a plant at {@code pos} if it is free, the ground suits the plant and the ground is solid. */
    public static boolean placeAt(WorldGenLevel level, BlockPos pos, IndicatorPlant plant) {
        BlockState here = level.getBlockState(pos);
        if (!(here.isAir() || here.is(Blocks.SHORT_GRASS))) return false;
        BlockPos below = pos.below();
        BlockState state = FloraBlocks.PLANTS.get(plant).get().defaultBlockState();
        if (!state.canSurvive(level, pos)) return false;
        if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) return false;
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        return true;
    }
}
