package dev.strataindustria.datagen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.registry.Tier6Blocks;
import dev.strataindustria.registry.Tier6Fluids;
import dev.strataindustria.registry.Tier6Items;
import dev.strataindustria.registry.Tier6Tags;
import dev.strataindustria.registry.Tier6Worldgen;
import dev.strataindustria.oil.SeepFeature;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.data.tags.FluidTagsProvider;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;

/** Tier 6 data: worldgen entries, translations and fluid tags, kept out of the shared providers. */
final class Tier6Data {
    private Tier6Data() {}

    static void features(BootstrapContext<Feature> ctx) {
        ctx.register(Tier6Worldgen.OIL_SEEPS, SeepFeature.INSTANCE);
    }

    static void placedFeatures(BootstrapContext<PlacedFeature> ctx) {
        HolderGetter<Feature> features = ctx.lookup(Registries.FEATURE);
        ctx.register(Tier6Worldgen.OIL_SEEPS_PLACED, new PlacedFeature(features.getOrThrow(Tier6Worldgen.OIL_SEEPS), List.of()));
    }

    static void biomeModifiers(BootstrapContext<BiomeModifier> ctx) {
        HolderGetter<Biome> biomes = ctx.lookup(Registries.BIOME);
        HolderGetter<PlacedFeature> placed = ctx.lookup(Registries.PLACED_FEATURE);
        ctx.register(Tier6Worldgen.ADD_OIL_SEEPS, new BiomeModifiers.AddFeaturesBiomeModifier(biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
                HolderSet.direct(placed.getOrThrow(Tier6Worldgen.OIL_SEEPS_PLACED)), GenerationStep.Decoration.LAKES));
    }

    static void lang(BiConsumer<String, String> add) {
        String id = StrataIndustria.MOD_ID;
        add.accept(Tier6Blocks.CRUDE_OIL.get().getDescriptionId(), "Crude Oil");
        add.accept(Tier6Items.CRUDE_OIL_BUCKET.get().getDescriptionId(), "Crude Oil Bucket");
        add.accept(Tier6Items.NAPHTHA_BUCKET.get().getDescriptionId(), "Naphtha Bucket");
        add.accept(Tier6Items.DIESEL_BUCKET.get().getDescriptionId(), "Diesel Bucket");
        add.accept(Tier6Items.HEAVY_OIL_BUCKET.get().getDescriptionId(), "Heavy Oil Bucket");
        add.accept(Tier6Blocks.OIL_STILL.get().getDescriptionId(), "Oil Still");
        add.accept(Tier6Items.BITUMEN.get().getDescriptionId(), "Bitumen");
        add.accept(Tier6Items.POLYETHYLENE_PELLET.get().getDescriptionId(), "Polyethylene Pellet");
        add.accept(Tier6Items.POLYETHYLENE_SHEET.get().getDescriptionId(), "Polyethylene Sheet");
        add.accept(Tier6Items.PVC_PELLET.get().getDescriptionId(), "PVC Pellet");
        add.accept(Tier6Items.PVC_SHEET.get().getDescriptionId(), "PVC Sheet");
        add.accept(Tier6Items.SYNTHETIC_RUBBER.get().getDescriptionId(), "Synthetic Rubber");

        add.accept("fluid_type." + id + ".crude_oil", "Crude Oil");
        add.accept("fluid_type." + id + ".naphtha", "Naphtha");
        add.accept("fluid_type." + id + ".diesel", "Diesel");
        add.accept("fluid_type." + id + ".heavy_oil", "Heavy Oil");
        add.accept("fluid_type." + id + ".refinery_gas", "Refinery Gas");
        add.accept("fluid_type." + id + ".ethylene", "Ethylene");
        add.accept("fluid_type." + id + ".butadiene", "Butadiene");
        add.accept("fluid_type." + id + ".vinyl_chloride", "Vinyl Chloride");
        add.accept("fluid_type." + id + ".hydrogen_chloride", "Hydrogen Chloride");

        add.accept(Tier6Blocks.SEISMIC_CHARGE.get().getDescriptionId(), "Seismic Charge");
        add.accept(Tier6Blocks.WELLHEAD.get().getDescriptionId(), "Wellhead");
        add.accept(Tier6Blocks.PUMP_JACK.get().getDescriptionId(), "Pump Jack");
        add.accept("container." + id + ".wellhead", "Wellhead");
        String charge = id + ".seismic_charge.";
        add.accept(charge + "fizzle", "The charge needs solid ground");
        String well = id + ".wellhead.";
        add.accept(well + "status.no_reservoir", "No reservoir below. Survey first.");
        add.accept(well + "status.too_close", "Too close to the well at %s, %s");
        add.accept(well + "status.not_turning", "Needs rotation");
        add.accept(well + "status.too_slow", "Turning too slowly");
        add.accept(well + "status.needs_casing", "Needs well casing (steel fluid pipe)");
        add.accept(well + "status.drilling", "Drilling");
        add.accept(well + "drilling", "Drilling: %s / %s blocks");
        add.accept(well + "status.gushing", "Struck oil!");
        add.accept(well + "status.flowing", "Flowing: %s mB/t");
        add.accept(well + "status.low_pressure", "Reservoir pressure too low. Add a pump jack.");
        add.accept(well + "status.pumping", "Pumped: %s mB/t");
        add.accept(well + "status.no_outlet", "Nothing takes the oil");
        add.accept(well + "remaining", "Reservoir %s%% left");
        add.accept(well + "struck", "Struck oil!");
        String jack = id + ".pump_jack.status.";
        add.accept(jack + "no_wellhead", "Place on a drilled wellhead");
        add.accept(jack + "no_room", "Needs room for the beam");
        add.accept(jack + "not_turning", "Needs rotation");
        add.accept(jack + "too_slow", "Turning too slowly");
        add.accept(jack + "no_outlet", "Nothing takes the oil");
        add.accept(jack + "pumping", "Pumping %s mB/t");
        add.accept(jack + "stripper", "Stripper well: 25%");
        String scanner = id + ".ore_scanner.";
        add.accept(scanner + "tab.ore", "Ore scan");
        add.accept(scanner + "tab.seismic", "Seismic");
        add.accept(scanner + "seismic.where", "Charge at %s, %s, Y %s");
        add.accept(scanner + "seismic.none", "No reservoirs found");
        add.accept(scanner + "seismic.empty", "Nothing under this chunk");
        add.accept(scanner + "seismic.size.small", "Small reservoir");
        add.accept(scanner + "seismic.size.medium", "Medium reservoir");
        add.accept(scanner + "seismic.size.large", "Large reservoir");
        add.accept(scanner + "seismic.below", "Top at Y %s, %s blocks below you");
        add.accept(scanner + "seismic.above", "Top at Y %s, %s blocks above you");
        add.accept(scanner + "seismic.left", "%s%% left");
        String config = id + ".configuration.";
        add.accept(config + "oil", "Oil");
        add.accept(config + "minWellSpacing", "Fewest blocks between wells");
        add.accept(config + "gusherPlacesOil", "Gushers spill oil");
        add.accept(config + "seismic", "Seismic surveys");
        add.accept(config + "listenRange", "Listening range");
        add.accept(config + "chunkRadius", "Chunks surveyed each way");
        add.accept("container." + id + ".oil_still", "Oil Still");
        String still = id + ".oil_still.";
        add.accept(still + "status.empty", "Pour in crude oil");
        add.accept(still + "status.no_recipe", "That does not distil");
        add.accept(still + "status.tank_full", "Product tank full");
        add.accept(still + "status.needs_heat", "Needs heat");
        add.accept(still + "status.distilling", "Distilling");
        add.accept(still + "crude", "Crude %s / %s mB");
        add.accept(still + "product", "%s %s / %s mB");
        add.accept(still + "empty_tank", "Empty");
        add.accept(still + "take", "Click with an empty bucket in your pack to fill it");
        add.accept(still + "port.1", "naphtha only");
        add.accept(still + "port.2", "diesel only");
        add.accept(still + "port.3", "heavy oil only");
        add.accept(id + ".heat_line.oil_still", "Heat");
        String jei = dev.strataindustria.compat.recipeview.RecipeText.KEY;
        add.accept(jei + "category.oil_still", "Oil Still");
        add.accept(jei + "oil_still.vented", "Vents %s mB of gas");

        String subtitles = "subtitles." + id + ".";
        add.accept(subtitles + "block.oil_still.boil", "Oil still bubbles");
        add.accept(subtitles + "block.oil_still.vent", "Oil still vents gas");
        add.accept(subtitles + "item.bucket.fill_crude_oil", "Bucket fills with crude oil");
        add.accept(subtitles + "item.bucket.empty_crude_oil", "Bucket empties crude oil");
        add.accept(subtitles + "block.crude_oil.bubble", "Crude oil bubbles");
        add.accept(subtitles + "block.seismic_charge.fuse", "Fuse hisses");
        add.accept(subtitles + "block.seismic_charge.thump", "Ground thumps");
        add.accept(subtitles + "item.ore_scanner.echo", "Ore scanner records a survey");
        add.accept(subtitles + "block.wellhead.drill", "Drill grinds");
        add.accept(subtitles + "block.wellhead.casing", "Casing clanks down the bore");
        add.accept(subtitles + "block.wellhead.gusher", "Oil gushes");
        add.accept(subtitles + "block.wellhead.flow", "Oil flows up the well");
        add.accept(subtitles + "block.pump_jack.stroke", "Pump jack strokes");

        String oil = "commands." + id + ".oil.";
        add.accept(oil + "none", "No oil reservoir under this chunk");
        add.accept(oil + "reservoir", "%s reservoir centred at %s, %s: top at Y %s, %s%% left, %s seeps");
        add.accept(oil + "details", "%s chunks, top at Y %s (%s blocks below you), capacity %s mB, %s mB left (%s%%)");
        add.accept(oil + "seep", "Seep at %s, %s: %s blocks of crude oil");
        add.accept(oil + "map", "%s reservoirs within %s chunks");
        add.accept(oil + "size.small", "Small");
        add.accept(oil + "size.medium", "Medium");
        add.accept(oil + "size.large", "Large");
    }

    /** Fluid tags (spec 17.6). */
    static final class FluidTags extends FluidTagsProvider {
        FluidTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
            super(output, lookupProvider, StrataIndustria.MOD_ID);
        }

        @Override
        protected void addTags(HolderLookup.Provider registries) {
            for (Tier6Fluids.Entry fluid : List.of(Tier6Fluids.CRUDE_OIL, Tier6Fluids.DIESEL, Tier6Fluids.NAPHTHA, Tier6Fluids.HEAVY_OIL,
                    Tier6Fluids.ETHYLENE, Tier6Fluids.HYDROGEN_CHLORIDE)) {
                tag(Tier6Tags.Fluids.common(fluid.name())).add(fluid.source().getKey()).add(fluid.flowing().getKey());
            }
            tag(Tier6Tags.Fluids.CORROSIVE).add(Tier6Fluids.HYDROGEN_CHLORIDE.source().getKey())
                    .add(Tier6Fluids.HYDROGEN_CHLORIDE.flowing().getKey());
        }
    }
}
