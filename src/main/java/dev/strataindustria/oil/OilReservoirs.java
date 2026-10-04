package dev.strataindustria.oil;

import dev.strataindustria.geology.GeologyContext;
import dev.strataindustria.geology.Province;
import dev.strataindustria.geology.RockCategory;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * Finds the oil reservoirs of a level (tier 6 spec 19.1). Reservoirs are rolled lazily and
 * deterministically per field cell; only the overworld has them.
 */
public final class OilReservoirs {
    /** Spec 19.1.2: chance of a reservoir per field cell, by the province at the cell centre. */
    private static final Map<String, Double> CHANCE = Map.of(
            "shale_basin", 0.50,
            "carbonate_platform", 0.35,
            "folded_mountains", 0.20,
            "granite_highlands", 0.10,
            "volcanic_field", 0.05);
    /** Provinces added by datapacks get the granite highlands' chance. */
    private static final double DEFAULT_CHANCE = 0.10;
    private static final int CACHE_LIMIT = 4096;
    private static final Map<ServerLevel, Map<Long, Optional<OilReservoir>>> CACHE = new WeakHashMap<>();

    private OilReservoirs() {}

    /** The reservoir under a chunk, if any. */
    public static Optional<OilReservoir> at(ServerLevel level, ChunkPos pos) {
        return inCell(level, Math.floorDiv(pos.x(), OilReservoir.CELL_CHUNKS), Math.floorDiv(pos.z(), OilReservoir.CELL_CHUNKS))
                .filter(r -> r.contains(pos));
    }

    /** The reservoir of a field cell, if it has one. */
    public static Optional<OilReservoir> inCell(ServerLevel level, int cellX, int cellZ) {
        if (level.dimension() != Level.OVERWORLD) return Optional.empty();
        GeologyContext geology = GeologyContext.get(level);
        if (geology == null) return Optional.empty();
        Map<Long, Optional<OilReservoir>> cache;
        synchronized (CACHE) {
            cache = CACHE.computeIfAbsent(level, l -> new ConcurrentHashMap<>());
        }
        long key = OilReservoir.cellKey(cellX, cellZ);
        Optional<OilReservoir> cached = cache.get(key);
        if (cached != null) return cached;
        Optional<OilReservoir> rolled = OilReservoir.roll(level.getSeed(), cellX, cellZ, site(level, geology));
        if (cache.size() > CACHE_LIMIT) cache.clear();
        cache.put(key, rolled);
        return rolled;
    }

    private static OilReservoir.Site site(ServerLevel level, GeologyContext geology) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        RandomState randomState = level.getChunkSource().randomState();
        BiomeResolver biomes = generator.getBiomeSource().createUncachedResolver(randomState);
        Registry<Province> provinces = level.registryAccess().lookupOrThrow(Province.REGISTRY);
        return new OilReservoir.Site() {
            @Override
            public double chance(int x, int z) {
                Identifier id = provinces.getKey(geology.sampler().provinceAt(x, z));
                return id == null ? DEFAULT_CHANCE : CHANCE.getOrDefault(id.getPath(), DEFAULT_CHANCE);
            }

            @Override
            public boolean seepsPossible(int x, int z) {
                if (geology.sampler().provinceAt(x, z).top().category() != RockCategory.SEDIMENTARY) return false;
                Holder<Biome> biome;
                synchronized (biomes) {
                    biome = biomes.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(surface(x, z)), QuartPos.fromBlock(z));
                }
                return isLand(biome);
            }

            @Override
            public int surface(int x, int z) {
                return generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, randomState);
            }
        };
    }

    /** Seeps never form in oceans, rivers or on beaches (spec 19.1.5). */
    public static boolean isLand(Holder<Biome> biome) {
        return !biome.is(BiomeTags.IS_OCEAN) && !biome.is(BiomeTags.IS_DEEP_OCEAN) && !biome.is(BiomeTags.IS_RIVER)
                && !biome.is(BiomeTags.IS_BEACH);
    }
}
