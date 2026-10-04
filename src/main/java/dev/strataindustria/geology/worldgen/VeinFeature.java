package dev.strataindustria.geology.worldgen;

import com.mojang.serialization.MapCodec;
import dev.strataindustria.geology.GeologyContext;
import dev.strataindustria.geology.OreGrade;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.geology.Rock;
import dev.strataindustria.geology.RockLookup;
import dev.strataindustria.geology.VeinCells;
import dev.strataindustria.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Writes the parts of all nearby veins that fall inside the current chunk (worldgen spec 5.5). Veins
 * come from deterministic per-cell lists, so a vein crossing chunks is assembled from independent
 * writes, and nothing is written outside the chunk.
 */
public record VeinFeature() implements Feature {
    public static final VeinFeature INSTANCE = new VeinFeature();
    public static final MapCodec<VeinFeature> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public MapCodec<VeinFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        GeologyContext ctx = GeologyContext.get(level.getLevel());
        if (ctx == null) return false;
        ChunkAccess chunk = level.getChunk(origin);
        int minX = chunk.getPos().getMinBlockX(), minZ = chunk.getPos().getMinBlockZ();
        int maxX = minX + 15, maxZ = minZ + 15;
        int minY = chunk.getMinY(), maxY = chunk.getMaxY();
        boolean placed = false;

        for (VeinCells.Vein vein : ctx.veins().around(minX + 8, minZ + 8)) {
            if (!vein.intersects(minX, minZ, maxX, maxZ)) continue;
            int x0 = Math.max(minX, vein.x() - vein.radiusH()), x1 = Math.min(maxX, vein.x() + vein.radiusH());
            int z0 = Math.max(minZ, vein.z() - vein.radiusH()), z1 = Math.min(maxZ, vein.z() + vein.radiusH());
            int y0 = Math.max(minY, vein.y() - vein.verticalReach()), y1 = Math.min(maxY, vein.y() + vein.verticalReach());
            BlockState plain = vein.type().block().orElse(null);
            if (plain == null && !vein.type().placesOre()) continue;
            for (int y = y0; y <= y1; y++) {
                LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(y));
                if (section.hasOnlyAir()) continue;
                for (int x = x0; x <= x1; x++) {
                    for (int z = z0; z <= z1; z++) {
                        double d = VeinCells.distance(vein, x, y, z);
                        if (d > 1) continue;
                        Rock host = RockLookup.rawRock(section.getBlockState(x & 15, y & 15, z & 15));
                        if (host == null || !vein.type().hosts().contains(host)) continue;
                        if (!VeinCells.isOre(vein, x, y, z, d)) continue;
                        if (plain != null) {
                            section.setBlockState(x & 15, y & 15, z & 15, plain, false);
                            placed = true;
                            continue;
                        }
                        OreMineral mineral = VeinCells.mineral(vein, x, y, z);
                        OreGrade grade = VeinCells.grade(vein, host, x, y, z, d);
                        section.setBlockState(x & 15, y & 15, z & 15, ModBlocks.ore(host, mineral, grade), false);
                        placed = true;
                    }
                }
            }
        }
        return placed;
    }
}
