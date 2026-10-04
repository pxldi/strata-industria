package dev.strataindustria.geology.worldgen;

import dev.strataindustria.block.BoulderBlock;
import dev.strataindustria.geology.GeologyContext;
import dev.strataindustria.geology.Rock;
import dev.strataindustria.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Boulders of the local top rock (redesign R1): small groups of one big rock with a few smaller ones round it,
 * readable from a distance. Hills get an outcrop of several groups. A limestone boulder often shows flint
 * nodules. Writes only inside its own chunk, and only on bare ground.
 */
final class BoulderPlacement {
    /** Chance per chunk of each group attempt. */
    static final float GROUP_CHANCE = 0.55f;
    /** Group attempts per chunk, and extra attempts on high ground. */
    static final int ATTEMPTS = 2;
    static final int HILL_ATTEMPTS = 3;
    /** Ground this far above the sea counts as a hill. */
    static final int HILL_HEIGHT = 85;
    static final float LIMESTONE_FLINT_CHANCE = 0.7f;

    private BoulderPlacement() {}

    static void place(WorldGenLevel level, GeologyContext ctx, RandomSource random, int minX, int minZ) {
        int centreSurface = level.getHeight(Heightmap.Types.OCEAN_FLOOR, minX + 8, minZ + 8);
        int attempts = ATTEMPTS + (centreSurface >= HILL_HEIGHT ? HILL_ATTEMPTS : 0);
        for (int i = 0; i < attempts; i++) {
            if (random.nextFloat() >= GROUP_CHANCE) continue;
            int x = minX + 1 + random.nextInt(14), z = minZ + 1 + random.nextInt(14);
            int surface = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z);
            Rock rock = ctx.sampler().column(x, z, surface).province().top();
            // One big rock, then up to three smaller ones within two blocks.
            int main = random.nextInt(5) == 0 ? 3 : random.nextInt(2) == 0 ? 2 : 1;
            put(level, random, rock, x, z, main);
            int others = 1 + random.nextInt(3);
            for (int k = 0; k < others; k++) {
                int ox = x + random.nextInt(5) - 2, oz = z + random.nextInt(5) - 2;
                if (ox < minX || ox > minX + 15 || oz < minZ || oz > minZ + 15 || (ox == x && oz == z)) continue;
                put(level, random, rock, ox, oz, Math.max(1, Math.min(2, main - random.nextInt(2))));
            }
        }
    }

    private static void put(WorldGenLevel level, RandomSource random, Rock rock, int x, int z, int size) {
        boolean flinty = rock == Rock.LIMESTONE && random.nextFloat() < LIMESTONE_FLINT_CHANCE;
        Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        BoulderBlock block = ModBlocks.BOULDER.get(rock).get();
        GroundCoverFeature.placeOnSurface(level, x, z, block.with(size, flinty, facing));
    }
}
