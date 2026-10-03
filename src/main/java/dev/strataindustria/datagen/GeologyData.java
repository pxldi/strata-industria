package dev.strataindustria.datagen;

import static dev.strataindustria.geology.Rock.BASALT;
import static dev.strataindustria.geology.Rock.GABBRO;
import static dev.strataindustria.geology.Rock.GRANITE;
import static dev.strataindustria.geology.Rock.LIMESTONE;
import static dev.strataindustria.geology.Rock.MARBLE;
import static dev.strataindustria.geology.Rock.RHYOLITE;
import static dev.strataindustria.geology.Rock.SHALE;
import static dev.strataindustria.geology.Rock.SLATE;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.geology.Province;
import dev.strataindustria.geology.Rock;
import dev.strataindustria.geology.VeinType;
import dev.strataindustria.geology.worldgen.ClayPatchFeature;
import dev.strataindustria.geology.worldgen.GroundCoverFeature;
import dev.strataindustria.geology.worldgen.StrataFeature;
import dev.strataindustria.geology.worldgen.VeinFeature;
import dev.strataindustria.registry.ModWorldgen;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.OrePlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;

/** Provinces, vein types, features and biome modifiers (worldgen spec 4.1, 5.3, 7.4, 7.6). */
final class GeologyData {
    private GeologyData() {}

    static void provinces(BootstrapContext<Province> ctx) {
        HolderGetter<Biome> biomes = ctx.lookup(Registries.BIOME);
        province(ctx, "carbonate_platform", LIMESTONE, MARBLE, GRANITE, 3,
                bias(biomes, Tags.Biomes.IS_PLAINS, 2), bias(biomes, BiomeTags.IS_FOREST, 2), bias(biomes, BiomeTags.IS_BEACH, 2));
        province(ctx, "shale_basin", SHALE, SLATE, GABBRO, 3,
                bias(biomes, Tags.Biomes.IS_SWAMP, 2), bias(biomes, BiomeTags.IS_RIVER, 2), bias(biomes, BiomeTags.IS_SAVANNA, 2));
        province(ctx, "volcanic_field", BASALT, RHYOLITE, GABBRO, 2,
                bias(biomes, BiomeTags.IS_OCEAN, 2), bias(biomes, BiomeTags.IS_BADLANDS, 2), bias(biomes, BiomeTags.IS_JUNGLE, 2));
        province(ctx, "granite_highlands", GRANITE, SLATE, GABBRO, 2,
                bias(biomes, Tags.Biomes.IS_WINDSWEPT, 3), bias(biomes, BiomeTags.IS_TAIGA, 3), bias(biomes, Tags.Biomes.IS_SNOWY_PLAINS, 3));
        province(ctx, "folded_mountains", SLATE, MARBLE, GRANITE, 2,
                bias(biomes, BiomeTags.IS_MOUNTAIN, 3));
    }

    private static Province.BiomeBias bias(HolderGetter<Biome> biomes, TagKey<Biome> tag, float multiplier) {
        return new Province.BiomeBias(biomes.getOrThrow(tag), multiplier);
    }

    private static void province(BootstrapContext<Province> ctx, String name, Rock top, Rock middle, Rock bottom, int weight,
            Province.BiomeBias... bias) {
        ctx.register(ResourceKey.create(Province.REGISTRY, StrataIndustria.id(name)), new Province(top, middle, bottom, weight, List.of(bias)));
    }

    static void veins(BootstrapContext<VeinType> ctx) {
        vein(ctx, "native_copper", VeinType.ClusterShape.of(5, 8, 3, 5, 0.30f),
                List.of(m(OreMineral.NATIVE_COPPER, 100)), List.of(BASALT, GABBRO, RHYOLITE), List.of(BASALT), -64, 160, 30);
        vein(ctx, "malachite", VeinType.ClusterShape.of(8, 14, 4, 7, 0.25f),
                List.of(m(OreMineral.MALACHITE, 85), m(OreMineral.NATIVE_COPPER, 15)), List.of(LIMESTONE, MARBLE), List.of(LIMESTONE), 0, 160, 30);
        vein(ctx, "tennantite", VeinType.ClusterShape.of(8, 12, 4, 6, 0.25f),
                List.of(m(OreMineral.TENNANTITE, 80), m(OreMineral.BISMUTHINITE, 20)), List.of(SHALE, SLATE, GRANITE), List.of(SHALE), -24, 140, 30);
        vein(ctx, "cassiterite", VeinType.ClusterShape.of(10, 16, 5, 8, 0.22f),
                List.of(m(OreMineral.CASSITERITE, 100)), List.of(GRANITE, RHYOLITE), List.of(GRANITE), -64, 160, 25);
        vein(ctx, "bismuthinite", VeinType.ClusterShape.of(4, 7, 3, 5, 0.30f),
                List.of(m(OreMineral.BISMUTHINITE, 100)), List.of(SLATE, SHALE, LIMESTONE, MARBLE), List.of(SLATE), -24, 120, 20);
    }

    private static VeinType.MineralWeight m(OreMineral mineral, int weight) {
        return new VeinType.MineralWeight(mineral, weight);
    }

    private static void vein(BootstrapContext<VeinType> ctx, String name, VeinType.ClusterShape shape, List<VeinType.MineralWeight> minerals,
            List<Rock> hosts, List<Rock> preferred, int minY, int maxY, int weight) {
        ctx.register(ResourceKey.create(VeinType.REGISTRY, StrataIndustria.id(name)),
                new VeinType(shape, minerals, hosts, preferred, minY, maxY, weight, true));
    }

    static void features(BootstrapContext<Feature> ctx) {
        ctx.register(ModWorldgen.STRATA, StrataFeature.INSTANCE);
        ctx.register(ModWorldgen.VEINS, VeinFeature.INSTANCE);
        ctx.register(ModWorldgen.GROUND_COVER, GroundCoverFeature.INSTANCE);
        ctx.register(ModWorldgen.CLAY_PATCHES, ClayPatchFeature.INSTANCE);
    }

    static void placedFeatures(BootstrapContext<PlacedFeature> ctx) {
        HolderGetter<Feature> features = ctx.lookup(Registries.FEATURE);
        ctx.register(ModWorldgen.STRATA_PLACED, new PlacedFeature(features.getOrThrow(ModWorldgen.STRATA), List.of()));
        ctx.register(ModWorldgen.VEINS_PLACED, new PlacedFeature(features.getOrThrow(ModWorldgen.VEINS), List.of()));
        ctx.register(ModWorldgen.GROUND_COVER_PLACED, new PlacedFeature(features.getOrThrow(ModWorldgen.GROUND_COVER), List.of()));
        ctx.register(ModWorldgen.CLAY_PATCHES_PLACED, new PlacedFeature(features.getOrThrow(ModWorldgen.CLAY_PATCHES), List.of()));
    }

    static void biomeModifiers(BootstrapContext<BiomeModifier> ctx) {
        HolderGetter<Biome> biomes = ctx.lookup(Registries.BIOME);
        HolderGetter<PlacedFeature> placed = ctx.lookup(Registries.PLACED_FEATURE);
        HolderSet<Biome> overworld = biomes.getOrThrow(BiomeTags.IS_OVERWORLD);

        add(ctx, ModWorldgen.ADD_STRATA, overworld, placed, ModWorldgen.STRATA_PLACED, GenerationStep.Decoration.RAW_GENERATION);
        add(ctx, ModWorldgen.ADD_CLAY_PATCHES, overworld, placed, ModWorldgen.CLAY_PATCHES_PLACED, GenerationStep.Decoration.LAKES);
        add(ctx, ModWorldgen.ADD_VEINS, overworld, placed, ModWorldgen.VEINS_PLACED, GenerationStep.Decoration.UNDERGROUND_ORES);
        add(ctx, ModWorldgen.ADD_GROUND_COVER, overworld, placed, ModWorldgen.GROUND_COVER_PLACED, GenerationStep.Decoration.TOP_LAYER_MODIFICATION);

        List<ResourceKey<PlacedFeature>> vanilla = List.of(
                OrePlacements.ORE_GRANITE_UPPER, OrePlacements.ORE_GRANITE_LOWER, OrePlacements.ORE_DIORITE_UPPER,
                OrePlacements.ORE_DIORITE_LOWER, OrePlacements.ORE_ANDESITE_UPPER, OrePlacements.ORE_ANDESITE_LOWER,
                OrePlacements.ORE_TUFF, OrePlacements.ORE_COAL_UPPER, OrePlacements.ORE_COAL_LOWER, OrePlacements.ORE_IRON_UPPER,
                OrePlacements.ORE_IRON_MIDDLE, OrePlacements.ORE_IRON_SMALL, OrePlacements.ORE_GOLD_EXTRA, OrePlacements.ORE_GOLD,
                OrePlacements.ORE_GOLD_LOWER, OrePlacements.ORE_REDSTONE, OrePlacements.ORE_REDSTONE_LOWER, OrePlacements.ORE_DIAMOND,
                OrePlacements.ORE_DIAMOND_MEDIUM, OrePlacements.ORE_DIAMOND_LARGE, OrePlacements.ORE_DIAMOND_BURIED,
                OrePlacements.ORE_LAPIS, OrePlacements.ORE_LAPIS_BURIED, OrePlacements.ORE_INFESTED, OrePlacements.ORE_EMERALD,
                OrePlacements.ORE_COPPER, OrePlacements.ORE_COPPER_LARGE);
        ctx.register(ModWorldgen.REMOVE_VANILLA_ORES, new BiomeModifiers.RemoveFeaturesBiomeModifier(overworld,
                HolderSet.direct(vanilla.stream().map(placed::getOrThrow).toList()),
                EnumSet.of(GenerationStep.Decoration.UNDERGROUND_ORES, GenerationStep.Decoration.UNDERGROUND_DECORATION)));
    }

    private static void add(BootstrapContext<BiomeModifier> ctx, ResourceKey<BiomeModifier> key, HolderSet<Biome> biomes,
            HolderGetter<PlacedFeature> placed, ResourceKey<PlacedFeature> feature, GenerationStep.Decoration step) {
        ctx.register(key, new BiomeModifiers.AddFeaturesBiomeModifier(biomes, HolderSet.direct(placed.getOrThrow(feature)), step));
    }
}
