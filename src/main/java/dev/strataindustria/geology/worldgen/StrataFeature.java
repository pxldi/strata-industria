package dev.strataindustria.geology.worldgen;

import com.mojang.serialization.MapCodec;
import dev.strataindustria.geology.GeologyContext;
import dev.strataindustria.geology.Rock;
import dev.strataindustria.geology.RockLookup;
import dev.strataindustria.geology.StrataSampler;
import dev.strataindustria.registry.ModBlocks;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Replaces vanilla stone with the rock of each province and layer (worldgen spec 4 and 7.3). Runs once
 * per chunk as the first feature, so caves, ores and every later feature already see Strata Industria
 * rock. It works on the chunk sections directly and skips sections without stone.
 */
public record StrataFeature() implements Feature {
    public static final StrataFeature INSTANCE = new StrataFeature();
    public static final MapCodec<StrataFeature> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public MapCodec<StrataFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        GeologyContext ctx = GeologyContext.get(level.getLevel());
        if (ctx == null) return false;
        StrataSampler sampler = ctx.sampler();
        ChunkAccess chunk = level.getChunk(origin);
        int minX = chunk.getPos().getMinBlockX(), minZ = chunk.getPos().getMinBlockZ();

        StrataSampler.Column[] columns = new StrataSampler.Column[256];
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int surface = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, lx, lz);
                columns[lx * 16 + lz] = sampler.column(minX + lx, minZ + lz, surface);
            }
        }

        Map<Rock, BlockState> rockStates = new EnumMap<>(Rock.class);
        for (Rock rock : Rock.values()) rockStates.put(rock, ModBlocks.RAW_ROCK.get(rock).get().defaultBlockState());

        LevelChunkSection[] sections = chunk.getSections();
        boolean changed = false;
        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection section = sections[i];
            if (section.hasOnlyAir() || !section.maybeHas(RockLookup::isReplaced)) continue;
            int baseY = chunk.getSectionYFromSectionIndex(i) << 4;
            for (int ly = 0; ly < 16; ly++) {
                int y = baseY + ly;
                for (int lx = 0; lx < 16; lx++) {
                    for (int lz = 0; lz < 16; lz++) {
                        if (!RockLookup.isReplaced(section.getBlockState(lx, ly, lz))) continue;
                        section.setBlockState(lx, ly, lz, rockStates.get(columns[lx * 16 + lz].rock(y)), false);
                        changed = true;
                    }
                }
            }
        }
        return changed;
    }
}
