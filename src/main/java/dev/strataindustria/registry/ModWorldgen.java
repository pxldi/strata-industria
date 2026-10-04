package dev.strataindustria.registry;

import com.mojang.serialization.MapCodec;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.geology.Province;
import dev.strataindustria.geology.VeinType;
import dev.strataindustria.geology.worldgen.BogIronFeature;
import dev.strataindustria.geology.worldgen.ClayPatchFeature;
import dev.strataindustria.geology.worldgen.PlacerFeature;
import dev.strataindustria.geology.worldgen.GroundCoverFeature;
import dev.strataindustria.geology.worldgen.StrataFeature;
import dev.strataindustria.geology.worldgen.VeinFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.NewDatapackRegistryEvent;

public final class ModWorldgen {
    public static final DeferredRegister<MapCodec<? extends Feature>> FEATURE_TYPES =
            DeferredRegister.create(Registries.FEATURE_TYPE, StrataIndustria.MOD_ID);

    static {
        FEATURE_TYPES.register("strata", () -> StrataFeature.CODEC);
        FEATURE_TYPES.register("veins", () -> VeinFeature.CODEC);
        FEATURE_TYPES.register("ground_cover", () -> GroundCoverFeature.CODEC);
        FEATURE_TYPES.register("clay_patches", () -> ClayPatchFeature.CODEC);
        FEATURE_TYPES.register("bog_iron", () -> BogIronFeature.CODEC);
        FEATURE_TYPES.register("placers", () -> PlacerFeature.CODEC);
    }

    public static final ResourceKey<Feature> STRATA = feature("strata");
    public static final ResourceKey<Feature> VEINS = feature("veins");
    public static final ResourceKey<Feature> GROUND_COVER = feature("ground_cover");
    public static final ResourceKey<Feature> CLAY_PATCHES = feature("clay_patches");
    public static final ResourceKey<Feature> BOG_IRON = feature("bog_iron");
    public static final ResourceKey<Feature> PLACERS = feature("placers");

    public static final ResourceKey<PlacedFeature> STRATA_PLACED = placed("strata");
    public static final ResourceKey<PlacedFeature> VEINS_PLACED = placed("veins");
    public static final ResourceKey<PlacedFeature> GROUND_COVER_PLACED = placed("ground_cover");
    public static final ResourceKey<PlacedFeature> CLAY_PATCHES_PLACED = placed("clay_patches");
    public static final ResourceKey<PlacedFeature> BOG_IRON_PLACED = placed("bog_iron");
    public static final ResourceKey<PlacedFeature> PLACERS_PLACED = placed("placers");

    public static final ResourceKey<BiomeModifier> ADD_STRATA = modifier("add_strata");
    public static final ResourceKey<BiomeModifier> ADD_VEINS = modifier("add_veins");
    public static final ResourceKey<BiomeModifier> ADD_GROUND_COVER = modifier("add_ground_cover");
    public static final ResourceKey<BiomeModifier> ADD_CLAY_PATCHES = modifier("add_clay_patches");
    public static final ResourceKey<BiomeModifier> ADD_BOG_IRON = modifier("add_bog_iron");
    public static final ResourceKey<BiomeModifier> ADD_PLACERS = modifier("add_placers");
    public static final ResourceKey<BiomeModifier> REMOVE_VANILLA_ORES = modifier("remove_vanilla_ores");

    public static void register(IEventBus modBus) {
        FEATURE_TYPES.register(modBus);
        modBus.addListener(ModWorldgen::registerDatapackRegistries);
    }

    private static void registerDatapackRegistries(NewDatapackRegistryEvent event) {
        event.worldRegistry(Province.REGISTRY, Province.CODEC);
        event.worldRegistry(VeinType.REGISTRY, VeinType.CODEC);
    }

    private static ResourceKey<Feature> feature(String name) {
        return ResourceKey.create(Registries.FEATURE, StrataIndustria.id(name));
    }

    private static ResourceKey<PlacedFeature> placed(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, StrataIndustria.id(name));
    }

    private static ResourceKey<BiomeModifier> modifier(String name) {
        return ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, StrataIndustria.id(name));
    }

    private ModWorldgen() {}
}
