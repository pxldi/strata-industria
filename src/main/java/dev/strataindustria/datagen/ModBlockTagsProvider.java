package dev.strataindustria.datagen;

import dev.strataindustria.structure.SharedBlocks;
import dev.strataindustria.structure.StructureContent;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.geology.Rock;
import dev.strataindustria.geology.RockCategory;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.Tier4Blocks;
import dev.strataindustria.registry.ModTags;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;

final class ModBlockTagsProvider extends BlockTagsProvider {
    ModBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, StrataIndustria.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        // Tier 5: casings need a pickaxe; cables come off by hand.
        tag(dev.strataindustria.rubber.Tappable.TAG).add(net.minecraft.world.level.block.Blocks.JUNGLE_LOG.builtInRegistryHolder().key());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(dev.strataindustria.registry.Tier5Blocks.KINETIC_DYNAMO.getKey())
                .add(dev.strataindustria.registry.Tier6Blocks.OIL_STILL.getKey())
                .add(dev.strataindustria.registry.Tier5Blocks.BATTERY_BOX.getKey()).add(dev.strataindustria.registry.Tier5Blocks.LV_MACHINE_HULL.getKey())
                .add(dev.strataindustria.registry.Tier5Blocks.ELECTRIC_FURNACE.getKey()).add(dev.strataindustria.registry.Tier5Blocks.MACERATOR.getKey())
                .add(dev.strataindustria.registry.Tier5Blocks.WIREMILL.getKey()).add(dev.strataindustria.registry.Tier5Blocks.BENDER.getKey())
                .add(dev.strataindustria.registry.Tier5Blocks.LATHE.getKey()).add(dev.strataindustria.registry.Tier5Blocks.POWER_HAMMER.getKey())
                .add(dev.strataindustria.registry.Tier5Blocks.EXTRUDER.getKey())
                .add(dev.strataindustria.registry.Tier5Blocks.MIXER.getKey()).add(dev.strataindustria.registry.Tier5Blocks.ASSEMBLER.getKey()).add(dev.strataindustria.registry.Tier5Blocks.ELECTROLYSER.getKey())
                .add(dev.strataindustria.registry.Tier5Blocks.STEAM_TURBINE.getKey()).add(dev.strataindustria.registry.Tier5Blocks.COMBUSTION_GENERATOR.getKey())
                .add(dev.strataindustria.registry.Tier5Blocks.TRANSFORMER.getKey()).add(dev.strataindustria.registry.Tier5Blocks.ENERGY_ADAPTER.getKey())
                .add(dev.strataindustria.registry.Tier5Blocks.LIQUID_FUEL_BURNER.getKey()).add(dev.strataindustria.registry.Tier5Blocks.ELECTRIC_HEATER.getKey())
                .add(dev.strataindustria.registry.Tier5Blocks.ELECTRIC_PUMP.getKey()).add(dev.strataindustria.registry.Tier5Blocks.KINETIC_MOTOR.getKey())
                .add(dev.strataindustria.logistics.Tier5Logistics.ITEM_PIPE.getKey()).add(dev.strataindustria.logistics.Tier5Logistics.PIPE_EXTRACTOR.getKey())
                .add(dev.strataindustria.logistics.Tier5Logistics.FAST_PIPE_EXTRACTOR.getKey()).add(dev.strataindustria.logistics.Tier5Logistics.STORAGE_CONTROLLER.getKey())
                .add(dev.strataindustria.logistics.Tier5Logistics.FLUID_FILTER.getKey());
        tag(ModTags.Blocks.WRENCH_BREAKABLE).add(dev.strataindustria.registry.Tier5Blocks.LV_CABLE.getKey())
                .add(dev.strataindustria.registry.Tier5Blocks.MV_CABLE.getKey()).add(dev.strataindustria.logistics.Tier5Logistics.ITEM_PIPE.getKey())
                .addTag(ModTags.Blocks.FLUID_PIPES);
        // Spec 5.4 and 8.3: treated wood is chopped, never burnt; insulators are pottery.
        tag(BlockTags.MINEABLE_WITH_AXE).add(dev.strataindustria.registry.Tier5Blocks.TREATED_LOG.getKey())
                .add(dev.strataindustria.registry.Tier5Blocks.UTILITY_POLE.getKey());
        tag(dev.strataindustria.registry.Tier5Tags.TREATED_WOOD_BLOCKS)
                .add(dev.strataindustria.registry.Tier5Blocks.TREATED_LOG.getKey());
        for (RockCategory category : RockCategory.values()) {
            var categoryTag = tag(ModTags.Blocks.rocks(category));
            for (Rock rock : Rock.values()) {
                if (rock.category() == category) categoryTag.add(ModBlocks.RAW_ROCK.get(rock).getKey());
            }
            tag(ModTags.Blocks.ROCKS).addTag(ModTags.Blocks.rocks(category));
            if (category.isIgneous()) tag(ModTags.Blocks.IGNEOUS_ROCKS).addTag(ModTags.Blocks.rocks(category));
        }

        // Raw rock behaves like vanilla stone for every later worldgen feature and for mob spawning.
        for (TagKey<Block> stoneLike : java.util.List.of(
                BlockTags.BASE_STONE_OVERWORLD,
                BlockTags.STONE_ORE_REPLACEABLES,
                BlockTags.DRIPSTONE_REPLACEABLE,
                BlockTags.MOSS_REPLACEABLE,
                BlockTags.LUSH_GROUND_REPLACEABLE,
                BlockTags.AZALEA_ROOT_REPLACEABLE,
                BlockTags.SCULK_REPLACEABLE,
                BlockTags.SCULK_REPLACEABLE_WORLD_GEN,
                BlockTags.MINEABLE_WITH_PICKAXE,
                Tags.Blocks.STONES)) {
            tag(stoneLike).addTag(ModTags.Blocks.ROCKS);
        }

        var looseRocks = tag(ModTags.Blocks.LOOSE_ROCKS);
        var cobbled = tag(Tags.Blocks.COBBLESTONES);
        var pickaxe = tag(BlockTags.MINEABLE_WITH_PICKAXE);
        var ores = tag(Tags.Blocks.ORES);
        var needsCopper = tag(ModTags.Blocks.NEEDS_COPPER_TOOL);
        for (Rock rock : Rock.values()) {
            looseRocks.add(ModBlocks.LOOSE_ROCK.get(rock).getKey());
            cobbled.add(ModBlocks.COBBLED_ROCK.get(rock).getKey());
            pickaxe.add(ModBlocks.COBBLED_ROCK.get(rock).getKey());
            for (OreMineral mineral : OreMineral.inRockValues()) {
                var ore = ModBlocks.ORES.get(rock).get(mineral).getKey();
                ores.add(ore);
                pickaxe.add(ore);
                if (mineral.needsSteelTool()) tag(ModTags.Blocks.NEEDS_STEEL_TOOL).add(ore);
                else if (mineral.needsWroughtIronTool()) tag(ModTags.Blocks.NEEDS_WROUGHT_IRON_TOOL).add(ore);
                else if (mineral.needsBronzeTool()) tag(ModTags.Blocks.NEEDS_BRONZE_TOOL).add(ore);
                else if (mineral.needsCopperTool()) needsCopper.add(ore);
                if (mineral.isIron()) tag(Tags.Blocks.ORES_IRON).add(ore);
                if (mineral == OreMineral.NATIVE_GOLD) tag(Tags.Blocks.ORES_GOLD).add(ore);
                if (mineral == OreMineral.BITUMINOUS_COAL) tag(Tags.Blocks.ORES_COAL).add(ore);
                if (mineral == OreMineral.CINNABAR) tag(Tags.Blocks.ORES_REDSTONE).add(ore);
                if (mineral == OreMineral.LAZURITE) tag(Tags.Blocks.ORES_LAPIS).add(ore);
            }
        }

        var smallOres = tag(ModTags.Blocks.SMALL_ORES);
        for (OreMineral mineral : OreMineral.values()) {
            smallOres.add(ModBlocks.SMALL_ORES.get(mineral).getKey());
        }

        // Tier 3 spec 3, 4.2 and 4.3: fire clay, fire bricks, bog iron, lignite and river placers.
        ores.add(ModBlocks.BOG_IRON.getKey());
        tag(Tags.Blocks.ORES_IRON).add(ModBlocks.BOG_IRON.getKey());
        tag(ModTags.Blocks.NEEDS_BRONZE_TOOL).add(ModBlocks.BOG_IRON.getKey());
        tag(BlockTags.MINEABLE_WITH_SHOVEL).add(ModBlocks.BOG_IRON.getKey()).add(ModBlocks.BAUXITE_BED.getKey()).add(ModBlocks.FIRE_CLAY.getKey())
                .add(ModBlocks.PLACER_GRAVEL.getKey()).add(ModBlocks.PLACER_SAND.getKey());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.LIGNITE_SEAM.getKey()).add(ModBlocks.FIRE_BRICKS.getKey())
                .add(ModBlocks.BLOOMERY.getKey())
                .add(ModBlocks.FIRE_BRICK_SLAB.getKey()).add(ModBlocks.FIRE_BRICK_STAIRS.getKey()).add(ModBlocks.FIRE_BRICK_WALL.getKey());
        // Tier 3 spec 7 and 8: wooden power parts take an axe, the millstone a pickaxe.
        tag(BlockTags.MINEABLE_WITH_AXE).add(ModBlocks.WOODEN_AXLE.getKey()).add(ModBlocks.WOODEN_GEARBOX.getKey())
                .add(ModBlocks.HAND_CRANK.getKey()).add(ModBlocks.WATER_WHEEL.getKey()).add(ModBlocks.BELLOWS.getKey())
                .add(ModBlocks.SAW_MILL.getKey()).add(ModBlocks.TRIP_HAMMER.getKey())
                .add(ModBlocks.CORE_SAMPLER.getKey()).add(ModBlocks.SLUICE.getKey()).add(ModBlocks.STEP_UP_GEARBOX.getKey())
                .add(ModBlocks.PULLEY.getKey()).add(ModBlocks.WINDMILL_BEARING.getKey()).add(ModBlocks.WINDMILL_SAIL.getKey())
                .add(ModBlocks.SOAKING_BARREL.getKey());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.MILLSTONE.getKey());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(StructureContent.CRACKED_FIRE_BRICKS.getKey());
        tag(BlockTags.SLABS).add(ModBlocks.FIRE_BRICK_SLAB.getKey());
        tag(BlockTags.STAIRS).add(ModBlocks.FIRE_BRICK_STAIRS.getKey());
        tag(BlockTags.WALLS).add(ModBlocks.FIRE_BRICK_WALL.getKey());
        tag(ModTags.Blocks.REFRACTORY).add(ModBlocks.FIRE_BRICKS.getKey()).add(Tier4Blocks.REFRACTORY_CASING.getKey());
        // Tier 4 spec 5.
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(Tier4Blocks.COKE_OVEN_BRICKS.getKey()).add(Tier4Blocks.COKE_OVEN_DOOR.getKey())
                .add(Tier4Blocks.COKE_BLOCK.getKey()).add(Tier4Blocks.REFRACTORY_CRUCIBLE.getKey())
                .add(Tier4Blocks.IRON_AXLE.getKey()).add(Tier4Blocks.IRON_GEARBOX.getKey())
                .add(Tier4Blocks.IRON_STEP_UP_GEARBOX.getKey()).add(Tier4Blocks.FIREBOX.getKey()).add(Tier4Blocks.BRONZE_BOILER.getKey())
                .add(Tier4Blocks.CRACKED_BRONZE_BOILER.getKey()).add(Tier4Blocks.COPPER_FLUID_PIPE.getKey())
                .add(Tier4Blocks.BRONZE_FLUID_PIPE.getKey()).add(Tier4Blocks.STEEL_FLUID_PIPE.getKey()).add(Tier4Blocks.PRESSURE_GAUGE.getKey())
                .add(Tier4Blocks.MECHANICAL_PUMP.getKey()).add(Tier4Blocks.STEAM_ENGINE.getKey())
                .add(dev.strataindustria.listening.ListeningBlocks.STEAM_WHISTLE.getKey())
                .add(dev.strataindustria.bronze.BronzeRegistry.FUME_HOOD.getKey())
                .add(dev.strataindustria.bronze.BronzeRegistry.BELL.getKey())
                .add(Tier4Blocks.CRUSHER.getKey()).add(Tier4Blocks.REFRACTORY_CASING.getKey())
                .add(Tier4Blocks.BLAST_FURNACE_CONTROLLER.getKey()).add(Tier4Blocks.TUYERE.getKey()).add(Tier4Blocks.CHARGING_HATCH.getKey())
                .add(Tier4Blocks.TAP_HATCH.getKey()).add(Tier4Blocks.BLOWER.getKey()).add(Tier4Blocks.CONVERTER_CONTROLLER.getKey())
                .add(Tier4Blocks.COPPER_HEAT_PIPE.getKey()).add(Tier4Blocks.REFRACTORY_HEAT_DUCT.getKey()).add(Tier4Blocks.HEAT_INLET.getKey())
                .add(Tier4Blocks.INSULATED_COPPER_HEAT_PIPE.getKey()).add(Tier4Blocks.INSULATED_REFRACTORY_HEAT_DUCT.getKey())
                .add(Tier4Blocks.KILN.getKey()).add(Tier4Blocks.ROASTER.getKey())
                .add(Tier4Blocks.SMELTER.getKey()).add(Tier4Blocks.STEAM_HAMMER.getKey())
                .add(Tier4Blocks.VALVE.getKey()).add(Tier4Blocks.FLUID_TANK.getKey()).add(Tier4Blocks.BLOWING_ENGINE.getKey())
                .add(Tier4Blocks.STEEL_BOILER_SHELL.getKey()).add(Tier4Blocks.BOILER_FLUID_PORT.getKey())
                .add(Tier4Blocks.BOILER_CONTROLLER.getKey()).add(Tier4Blocks.CRACKED_BOILER_CONTROLLER.getKey()).add(Tier4Blocks.CHUTE.getKey()).add(Tier4Blocks.INSERTER.getKey());
        tag(ModTags.Blocks.HEAT_PIPES).add(Tier4Blocks.COPPER_HEAT_PIPE.getKey()).add(Tier4Blocks.REFRACTORY_HEAT_DUCT.getKey())
                .add(Tier4Blocks.INSULATED_COPPER_HEAT_PIPE.getKey()).add(Tier4Blocks.INSULATED_REFRACTORY_HEAT_DUCT.getKey());
        tag(dev.strataindustria.registry.ModTags.Blocks.FLUID_PIPES).add(Tier4Blocks.COPPER_FLUID_PIPE.getKey())
                .add(Tier4Blocks.BRONZE_FLUID_PIPE.getKey()).add(Tier4Blocks.STEEL_FLUID_PIPE.getKey()).add(Tier4Blocks.PRESSURE_GAUGE.getKey())
                .add(Tier4Blocks.VALVE.getKey());
        tag(BlockTags.MINEABLE_WITH_AXE).add(Tier4Blocks.WASHER.getKey()).add(Tier4Blocks.TREATED_PLANKS.getKey()).add(Tier4Blocks.TREATED_SLAB.getKey())
                .add(Tier4Blocks.TREATED_STAIRS.getKey()).add(Tier4Blocks.TREATED_FENCE.getKey()).add(Tier4Blocks.CONVEYOR_BELT.getKey())
                .add(Tier4Blocks.BELT_DIVERTER.getKey());
        tag(BlockTags.WOODEN_SLABS).add(Tier4Blocks.TREATED_SLAB.getKey());
        tag(BlockTags.WOODEN_STAIRS).add(Tier4Blocks.TREATED_STAIRS.getKey());
        tag(BlockTags.WOODEN_FENCES).add(Tier4Blocks.TREATED_FENCE.getKey());
        tag(ModTags.Blocks.PROSPECTABLE).add(ModBlocks.BAUXITE_BED.getKey()).add(ModBlocks.LIGNITE_SEAM.getKey()).add(ModBlocks.FIRE_CLAY.getKey())
                .add(ModBlocks.BOG_IRON.getKey());

        // Mining tiers: stone < copper < bronze < wrought iron < steel. Vanilla diamond and above still apply.
        // Tier 4 spec 4.7: steel mines what a vanilla diamond pickaxe mines.
        tag(ModTags.Blocks.INCORRECT_FOR_STEEL_TOOL).addTag(BlockTags.INCORRECT_FOR_DIAMOND_TOOL);
        tag(ModTags.Blocks.INCORRECT_FOR_WROUGHT_IRON_TOOL)
                .addTag(ModTags.Blocks.NEEDS_STEEL_TOOL)
                .addTag(BlockTags.NEEDS_DIAMOND_TOOL);
        tag(BlockTags.INCORRECT_FOR_IRON_TOOL).addTag(ModTags.Blocks.NEEDS_STEEL_TOOL);
        tag(ModTags.Blocks.INCORRECT_FOR_BRONZE_TOOL)
                .addTag(ModTags.Blocks.NEEDS_WROUGHT_IRON_TOOL)
                .addTag(BlockTags.NEEDS_DIAMOND_TOOL);
        tag(ModTags.Blocks.INCORRECT_FOR_COPPER_TOOL)
                .addTag(ModTags.Blocks.NEEDS_BRONZE_TOOL)
                .addTag(ModTags.Blocks.INCORRECT_FOR_BRONZE_TOOL)
                .addTag(BlockTags.NEEDS_IRON_TOOL);
        tag(ModTags.Blocks.INCORRECT_FOR_STONE_TOOL)
                .addTag(ModTags.Blocks.NEEDS_COPPER_TOOL)
                .addTag(ModTags.Blocks.INCORRECT_FOR_COPPER_TOOL);
        // The vanilla copper pickaxe is the copper tier; vanilla wood and stone tools sit below it.
        tag(BlockTags.INCORRECT_FOR_COPPER_TOOL).addTag(ModTags.Blocks.NEEDS_BRONZE_TOOL).addTag(ModTags.Blocks.NEEDS_WROUGHT_IRON_TOOL);
        tag(BlockTags.INCORRECT_FOR_STONE_TOOL).addTag(ModTags.Blocks.NEEDS_COPPER_TOOL)
                .addTag(ModTags.Blocks.NEEDS_BRONZE_TOOL).addTag(ModTags.Blocks.NEEDS_WROUGHT_IRON_TOOL);
        tag(BlockTags.INCORRECT_FOR_WOODEN_TOOL).addTag(ModTags.Blocks.NEEDS_COPPER_TOOL)
                .addTag(ModTags.Blocks.NEEDS_BRONZE_TOOL).addTag(ModTags.Blocks.NEEDS_WROUGHT_IRON_TOOL);
        tag(BlockTags.INCORRECT_FOR_GOLD_TOOL).addTag(ModTags.Blocks.NEEDS_COPPER_TOOL)
                .addTag(ModTags.Blocks.NEEDS_BRONZE_TOOL).addTag(ModTags.Blocks.NEEDS_WROUGHT_IRON_TOOL);

        // Knives cut grass for fibre and straw, and go through leaves quickly.
        var fibrePlants = tag(ModTags.Blocks.FIBRE_PLANTS);
        for (Block plant : java.util.List.of(Blocks.SHORT_GRASS, Blocks.TALL_GRASS, Blocks.FERN, Blocks.LARGE_FERN,
                Blocks.SHORT_DRY_GRASS, Blocks.TALL_DRY_GRASS, Blocks.BUSH)) {
            fibrePlants.add(plant.builtInRegistryHolder().key());
        }
        tag(ModTags.Blocks.MINEABLE_WITH_KNIFE).addTag(ModTags.Blocks.FIBRE_PLANTS).addTag(BlockTags.LEAVES);
        for (var plant : dev.strataindustria.flora.FloraBlocks.PLANTS.values()) {
            tag(BlockTags.SWORD_INSTANTLY_MINES).add(plant.getKey());
            tag(BlockTags.REPLACEABLE_BY_TREES).add(plant.getKey());
        }
        tag(ModTags.Blocks.MINEABLE_WITH_HAMMER);
        tag(BlockTags.MINEABLE_WITH_AXE).add(ModBlocks.FIRE_PIT.getKey());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.LARGE_VESSEL.getKey()).add(ModBlocks.CRUCIBLE.getKey())
                .add(ModBlocks.FORGE.getKey()).add(ModBlocks.QUERN.getKey())
                .add(ModBlocks.IRON_ANVIL.getKey())
                .add(dev.strataindustria.registry.PrologueRegistry.BRICK_KILN.getKey())
                .add(dev.strataindustria.registry.PrologueRegistry.CASTING_TABLE.getKey());
        for (var anvil : ModBlocks.STONE_ANVILS.values()) tag(BlockTags.MINEABLE_WITH_PICKAXE).add(anvil.getKey());
        tag(BlockTags.MINEABLE_WITH_SHOVEL).add(ModBlocks.PIT_KILN.getKey()).add(ModBlocks.CHARCOAL_PILE.getKey());
        tag(BlockTags.MINEABLE_WITH_AXE).add(ModBlocks.LOG_PILE.getKey());
        tag(BlockTags.MINEABLE_WITH_AXE).add(StructureContent.PIT_PROP.getKey());
        tag(BlockTags.MINEABLE_WITH_AXE).add(dev.strataindustria.ledger.LedgerRegistry.BUILDERS_CRATE.getKey());
        // Outposts and transport spec 4: tier 2 on foot.
        tag(BlockTags.CLIMBABLE).add(dev.strataindustria.transport.foot.FootRegistry.ROPE_LADDER.getKey());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(dev.strataindustria.transport.foot.FootRegistry.CAIRN.getKey());
        // Tier 3: the wooden tramway.
        for (var rail : java.util.List.of(dev.strataindustria.transport.rail.RailRegistry.WOODEN_RAIL, dev.strataindustria.transport.rail.RailRegistry.TUB_STOP,
                dev.strataindustria.transport.rail.RailRegistry.TIPPLE_RAIL, dev.strataindustria.transport.rail.RailRegistry.RAIL_BUFFER)) {
            tag(BlockTags.RAILS).add(rail.getKey());
            tag(dev.strataindustria.transport.rail.RailRegistry.TRACK).add(rail.getKey());
            tag(BlockTags.MINEABLE_WITH_AXE).add(rail.getKey());
        }
        // Tier 4: steel track and the wagon fluid port.
        for (var rail : java.util.List.of(dev.strataindustria.transport.rail.RailwayRegistry.STEEL_TRACK, dev.strataindustria.transport.rail.RailwayRegistry.STATION_TRACK,
                dev.strataindustria.transport.rail.RailwayRegistry.STEEL_BUFFER)) {
            tag(BlockTags.RAILS).add(rail.getKey());
            tag(dev.strataindustria.transport.rail.RailRegistry.TRACK).add(rail.getKey());
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(rail.getKey());
        }
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(dev.strataindustria.transport.rail.RailwayRegistry.WAGON_FLUID_PORT.getKey())
                .add(dev.strataindustria.transport.rail.RailwayRegistry.WATER_TOWER_SPOUT.getKey());
        tag(BlockTags.MINEABLE_WITH_AXE).add(dev.strataindustria.transport.rail.RailwayRegistry.WATER_TOWER_BASE.getKey())
                .add(dev.strataindustria.transport.rail.RailwayRegistry.COAL_STAGE.getKey());
        tag(BlockTags.MINEABLE_WITH_AXE).add(dev.strataindustria.transport.rail.RailRegistry.HAY_RACK.getKey())
                .add(dev.strataindustria.transport.rail.RailRegistry.INCLINE_WINCH.getKey());
        tag(BlockTags.MINEABLE_WITH_AXE).add(dev.strataindustria.registry.TransportBlocks.OUTPOST_CHARTER.getKey());
        tag(BlockTags.MINEABLE_WITH_AXE).add(dev.strataindustria.ledger.LedgerRegistry.BUILDERS_CRATE.getKey())
                .add(dev.strataindustria.cabinet.CabinetRegistry.SPECIMEN_CABINET.getKey());
        // Structures v2 shared blocks.
        tag(BlockTags.MINEABLE_WITH_AXE).add(SharedBlocks.CRATE.getKey()).add(SharedBlocks.ORE_CART.getKey())
                .add(SharedBlocks.TOOL_RACK.getKey()).add(SharedBlocks.WINDLASS.getKey()).add(SharedBlocks.SLUICE_BOX.getKey())
                .add(SharedBlocks.SMOULDERING_LOG_PILE.getKey());
        for (Rock rock : Rock.values()) {
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(SharedBlocks.CRACKED.get(rock).getKey()).add(SharedBlocks.MOSSY_COBBLED.get(rock).getKey());
        }
    }
}
