package dev.strataindustria.geology.worldgen;

import dev.strataindustria.Config;
import dev.strataindustria.block.BoulderBlock;
import dev.strataindustria.geology.GeologyContext;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.geology.Rock;
import dev.strataindustria.geology.VeinCells;
import dev.strataindustria.geology.VeinType;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.signs.SignBlocks;
import dev.strataindustria.signs.Stain;
import dev.strataindustria.util.Noise;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The colours the surface takes over shallow veins (redesign R7): rust-red gossan over iron, green malachite
 * bloom over copper and a yellow crust over native sulfur, as ground stains and as coats on a few boulders, and
 * black sand in the stream beds near tin. Writes only inside its own chunk.
 */
public final class SignPlacement {
    /** Black sand reaches this far past the edge of a tin vein, along the water. */
    static final int SAND_REACH = 28;
    private static final int SAND_TRIES = 36;

    private SignPlacement() {}

    static void place(WorldGenLevel level, GeologyContext ctx, RandomSource random, int minX, int minZ) {
        int depth = Config.INDICATOR_DEPTH.get();
        double density = Config.INDICATOR_DENSITY.get();
        for (VeinCells.Vein vein : ctx.veins().around(minX + 8, minZ + 8)) {
            if (!vein.type().indicators() || !vein.type().placesOre()) continue;
            if (vein.y() + vein.verticalReach() < vein.surfaceY() - depth) continue;
            if (hasTin(vein.type()) && vein.intersects(minX - SAND_REACH, minZ - SAND_REACH, minX + 15 + SAND_REACH, minZ + 15 + SAND_REACH)) {
                blackSand(level, vein, minX, minZ);
            }
            int reach = vein.radiusH() + 4;
            if (!vein.intersects(minX - 4, minZ - 4, minX + 19, minZ + 19)) continue;
            int patches = (int) Math.round(Math.clamp(vein.estimatedOreBlocks() / 50.0, 3, 10) * density);
            for (int k = 0; k < patches; k++) {
                long h = Noise.hash(vein.seed(), 500 + k);
                int x = vein.x() - reach + (int) (Noise.unit(Noise.hash(h, 1)) * (2 * reach + 1));
                int z = vein.z() - reach + (int) (Noise.unit(Noise.hash(h, 2)) * (2 * reach + 1));
                if (x < minX - 2 || x > minX + 17 || z < minZ - 2 || z > minZ + 17) continue;
                Stain stain = Stain.forMineral(vein.type().pickMineral(Noise.unit(Noise.hash(h, 3))));
                if (stain != null) patch(level, minX, minZ, x, z, stain, h);
            }
            // Iron and copper also coat a boulder or two: the sign you can break open for the first ore.
            Stain coat = null;
            for (VeinType.MineralWeight weight : vein.type().minerals()) {
                Stain stain = Stain.forMineral(weight.mineral());
                if (stain != null && stain != Stain.SULFUR_CRUST) coat = stain;
            }
            if (coat == null) continue;
            int groups = 1 + random.nextInt(2);
            for (int g = 0; g < groups; g++) {
                int x = vein.x() - reach + random.nextInt(2 * reach + 1), z = vein.z() - reach + random.nextInt(2 * reach + 1);
                if (x < minX || x > minX + 15 || z < minZ || z > minZ + 15) continue;
                int dx = x - vein.x(), dz = z - vein.z();
                if (dx * dx + dz * dz > reach * reach) continue;
                coatedGroup(level, ctx, random, x, z, BoulderBlock.Coat.of(coat));
            }
        }
    }

    private static boolean hasTin(VeinType type) {
        return type.minerals().stream().anyMatch(m -> m.mineral() == OreMineral.CASSITERITE);
    }

    /** A stain of a few blocks, thinning out at its edge. */
    public static int patch(WorldGenLevel level, int minX, int minZ, int cx, int cz, Stain stain, long seed) {
        int placed = 0;
        var block = SignBlocks.STAINS.get(stain).get();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int x = cx + dx, z = cz + dz;
                if (x < minX || x > minX + 15 || z < minZ || z > minZ + 15) continue;
                double edge = Math.sqrt(dx * dx + dz * dz);
                long h = Noise.hash(seed, 900 + dx * 7, dz);
                if (Noise.unit(h) > 0.95 - 0.28 * edge) continue;
                if (GroundCoverFeature.placeOnSurface(level, x, z, block.with((int) (Noise.unit(Noise.hash(h, 1)) * 4)))) placed++;
            }
        }
        return placed;
    }

    /** One stained boulder with a couple of smaller ones beside it. */
    private static void coatedGroup(WorldGenLevel level, GeologyContext ctx, RandomSource random, int x, int z, BoulderBlock.Coat coat) {
        int surface = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z);
        coatedGroup(level, random, ctx.sampler().column(x, z, surface).province().top(), x, z, coat);
    }

    public static void coatedGroup(WorldGenLevel level, RandomSource random, Rock rock, int x, int z, BoulderBlock.Coat coat) {
        BoulderPlacement.put(level, random, rock, x, z, 2 + random.nextInt(2), coat);
        int others = 1 + random.nextInt(2);
        for (int i = 0; i < others; i++) {
            BoulderPlacement.put(level, random, rock, x + random.nextInt(5) - 2, z + random.nextInt(5) - 2, 1 + random.nextInt(2), coat);
        }
    }

    /** Stream beds and shores of sand and gravel near a tin vein turn to black sand, thinning with distance. */
    private static void blackSand(WorldGenLevel level, VeinCells.Vein vein, int minX, int minZ) {
        int reach = vein.radiusH() + SAND_REACH;
        for (int k = 0; k < SAND_TRIES; k++) {
            long h = Noise.hash(vein.seed(), 1300 + k);
            int x = minX + (int) (Noise.unit(Noise.hash(h, 1)) * 16), z = minZ + (int) (Noise.unit(Noise.hash(h, 2)) * 16);
            double d = Math.hypot(x - vein.x(), z - vein.z());
            if (d > reach || Noise.unit(Noise.hash(h, 3)) > 1.0 - d / reach) continue;
            blackSandPatch(level, minX, minZ, x, z, 1 + (int) (Noise.unit(Noise.hash(h, 4)) * 3));
        }
    }

    /** A round patch of black sand on the sand and gravel of a wet bed or shore, one or two blocks deep. Returns the blocks changed. */
    public static int blackSandPatch(WorldGenLevel level, int minX, int minZ, int cx, int cz, int radius) {
        int changed = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int px = cx + dx, pz = cz + dz;
                if (dx * dx + dz * dz > radius * radius || px < minX || px > minX + 15 || pz < minZ || pz > minZ + 15) continue;
                BlockPos bed = new BlockPos(px, level.getHeight(Heightmap.Types.OCEAN_FLOOR, px, pz) - 1, pz);
                BlockState state = level.getBlockState(bed);
                if (!(state.is(BlockTags.SAND) || state.is(Blocks.GRAVEL)) || !PlacerFeature.wet(level, bed)) continue;
                level.setBlock(bed, ModBlocks.BLACK_SAND.get().defaultBlockState(), Block.UPDATE_CLIENTS);
                changed++;
                BlockPos under = bed.below();
                BlockState below = level.getBlockState(under);
                if (below.is(BlockTags.SAND) || below.is(Blocks.GRAVEL)) {
                    level.setBlock(under, ModBlocks.BLACK_SAND.get().defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
        return changed;
    }
}
