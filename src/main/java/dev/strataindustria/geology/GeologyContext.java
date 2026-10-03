package dev.strataindustria.geology;

import dev.strataindustria.Config;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

/** Per-level sampler and vein cells, built lazily from the level's registries and seed. */
public final class GeologyContext {
    private static final Map<ServerLevel, GeologyContext> CONTEXTS = new WeakHashMap<>();

    private final StrataSampler sampler;
    private final VeinCells veins;

    private GeologyContext(ServerLevel level) {
        long seed = level.getSeed();
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        RandomState randomState = level.getChunkSource().randomState();
        BiomeResolver biomes = generator.getBiomeSource().createUncachedResolver(randomState);

        List<Province> provinces = sorted(level.registryAccess().lookupOrThrow(Province.REGISTRY));
        List<VeinType> types = sorted(level.registryAccess().lookupOrThrow(VeinType.REGISTRY));

        StrataSampler.Weigher weigher = (province, x, z) -> {
            Holder<Biome> biome;
            synchronized (biomes) {
                biome = biomes.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(64), QuartPos.fromBlock(z));
            }
            return province.weightIn(biome);
        };
        this.sampler = new StrataSampler(seed, provinces, Config.PROVINCE_SCALE.get(), weigher);
        VeinCells.Settings settings = new VeinCells.Settings(Config.VEIN_ATTEMPTS.get(), Config.SHALLOW_BIAS.get(),
                Config.EMPTY_WEIGHT.get(), Config.VEIN_SIZE.get(), Config.SPAWN_GUARANTEE.get());
        this.veins = new VeinCells(seed, sampler, types, settings,
                (x, z) -> generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, randomState));
    }

    private static <T> List<T> sorted(Registry<T> registry) {
        return registry.listElements()
                .sorted(Comparator.comparing(h -> h.key().identifier().toString()))
                .map(Holder::value)
                .toList();
    }

    /** Null if the level has no provinces (for example when a datapack removed them). */
    public static GeologyContext get(ServerLevel level) {
        synchronized (CONTEXTS) {
            GeologyContext ctx = CONTEXTS.get(level);
            if (ctx == null) {
                if (level.registryAccess().lookupOrThrow(Province.REGISTRY).keySet().isEmpty()) return null;
                ctx = new GeologyContext(level);
                CONTEXTS.put(level, ctx);
            }
            return ctx;
        }
    }

    public StrataSampler sampler() {
        return sampler;
    }

    public VeinCells veins() {
        return veins;
    }
}
