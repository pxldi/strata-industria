package dev.strataindustria.registry;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.oil.SeepFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Tier 6 worldgen: the crude oil seeps over reservoirs (spec 19.1.5). Reservoirs themselves are not blocks. */
public final class Tier6Worldgen {
    static {
        ModWorldgen.FEATURE_TYPES.register("oil_seeps", () -> SeepFeature.CODEC);
    }

    public static final ResourceKey<Feature> OIL_SEEPS = ResourceKey.create(Registries.FEATURE, StrataIndustria.id("oil_seeps"));
    public static final ResourceKey<PlacedFeature> OIL_SEEPS_PLACED = ResourceKey.create(Registries.PLACED_FEATURE,
            StrataIndustria.id("oil_seeps"));
    public static final ResourceKey<BiomeModifier> ADD_OIL_SEEPS = ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS,
            StrataIndustria.id("add_oil_seeps"));

    public static void init() {}

    private Tier6Worldgen() {}
}
