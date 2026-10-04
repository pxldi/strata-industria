package dev.strataindustria.datagen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.ceramics.MoldType;
import dev.strataindustria.geology.OreGrade;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.geology.Rock;
import dev.strataindustria.geology.RockCategory;
import dev.strataindustria.heat.HeatBand;
import dev.strataindustria.material.Metal;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModCreativeTabs;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.Tier4Blocks;
import dev.strataindustria.registry.Tier4Items;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

final class ModLanguageProvider extends LanguageProvider {
    ModLanguageProvider(PackOutput output) {
        super(output, StrataIndustria.MOD_ID, "en_us");
    }

    static String title(String id) {
        return Arrays.stream(id.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1))
                .collect(Collectors.joining(" "));
    }

    @Override
    protected void addTranslations() {
        add(ModCreativeTabs.MAIN_TAB_TITLE, "Strata Industria");
        StructureData.lang(this::add);
        Tier5Language.add(this::add);
        JournalLanguage.add(this::add);
        Tier6Data.lang(this::add);
        recipeViewer();

        addItem(ModItems.PLANT_FIBRE, "Plant Fibre");
        addItem(ModItems.STRAW, "Straw");
        addItem(ModItems.TWINE, "Twine");
        addItem(ModItems.FIBRE_CLOTH, "Fibre Cloth");
        addItem(ModItems.FIELD_JOURNAL, "Field Journal");
        addItem(ModItems.STONE_AXE_HEAD, "Stone Axe Head");
        addItem(ModItems.STONE_KNIFE_BLADE, "Stone Knife Blade");
        addItem(ModItems.STONE_SHOVEL_HEAD, "Stone Shovel Head");
        addItem(ModItems.STONE_HOE_HEAD, "Stone Hoe Head");
        addItem(ModItems.STONE_HAMMER_HEAD, "Stone Hammer Head");
        addItem(ModItems.STONE_SPEAR_HEAD, "Stone Spear Head");
        addItem(ModItems.STONE_PICKAXE_HEAD, "Stone Pickaxe Head");
        addItem(ModItems.STONE_AXE, "Stone Axe");
        addItem(ModItems.STONE_KNIFE, "Stone Knife");
        addItem(ModItems.STONE_SHOVEL, "Stone Shovel");
        addItem(ModItems.STONE_HOE, "Stone Hoe");
        addItem(ModItems.STONE_HAMMER, "Stone Hammer");
        addItem(ModItems.STONE_PICKAXE, "Stone Pickaxe");

        String knapped = StrataIndustria.MOD_ID + ".knapped_from.";
        add(knapped + "rock", "Knapped from %s (%s)");
        add(knapped + "flint", "Knapped from flint");
        for (Rock rock : Rock.values()) {
            add(knapped + "material." + rock.id(), rock.id().replace('_', ' '));
        }
        for (RockCategory category : RockCategory.values()) {
            add(knapped + "category." + category.getSerializedName(), category.getSerializedName().replace('_', ' '));
        }
        add("container." + StrataIndustria.MOD_ID + ".knapping", "Knapping");
        addItem(ModItems.FIRESTARTER, "Firestarter");
        addBlock(ModBlocks.FIRE_PIT, "Fire Pit");
        add("container." + StrataIndustria.MOD_ID + ".fire_pit", "Fire Pit");
        for (HeatBand band : HeatBand.values()) {
            if (band == HeatBand.NONE) continue;
            String name = band.id().replace('_', ' ');
            add(StrataIndustria.MOD_ID + ".heat." + band.id(), name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1));
        }
        add(StrataIndustria.MOD_ID + ".knapping.need_more", "You need %s %s to start");
        add("container." + StrataIndustria.MOD_ID + ".clay_forming", "Clay Forming");
        addItem(ModItems.UNFIRED_SMALL_VESSEL, "Unfired Small Vessel");
        addItem(ModItems.UNFIRED_LARGE_VESSEL, "Unfired Large Vessel");
        addItem(ModItems.UNFIRED_CRUCIBLE, "Unfired Crucible");
        addItem(ModItems.UNFIRED_INGOT_MOLD, "Unfired Ingot Mold");
        addItem(ModItems.UNFIRED_BRICK, "Unfired Brick");
        addItem(ModItems.SMALL_VESSEL, "Small Vessel");
        addBlock(ModBlocks.LARGE_VESSEL, "Large Vessel");
        addBlock(ModBlocks.CRUCIBLE, "Crucible");
        addItem(ModItems.INGOT_MOLD, "Ingot Mold");
        for (MoldType type : MoldType.values()) {
            String name = title(type.id());
            addItem(ModItems.UNFIRED_MOLDS.get(type), "Unfired " + name + " Mold");
            addItem(ModItems.MOLDS.get(type), name + " Mold");
        }
        add("container." + StrataIndustria.MOD_ID + ".small_vessel", "Small Vessel");
        add("container." + StrataIndustria.MOD_ID + ".large_vessel", "Large Vessel");
        addBlock(ModBlocks.PIT_KILN, "Pit Kiln");
        addBlock(ModBlocks.LOG_PILE, "Log Pile");
        addBlock(ModBlocks.CHARCOAL_PILE, "Charcoal Pile");
        addItem(ModItems.ASH, "Ash");
        addBlock(ModBlocks.FORGE, "Forge");
        addBlock(ModBlocks.QUERN, "Quern");
        smithing();
        journal();
        addItem(ModItems.QUERNSTONE, "Quernstone");
        add("container." + StrataIndustria.MOD_ID + ".forge", "Forge");
        metals();
        String kiln = StrataIndustria.MOD_ID + ".pit_kiln.";
        add(kiln + "burning", "The kiln is already burning");
        add(kiln + "needs_straw", "The kiln needs 8 straw on top first");
        add(kiln + "needs_logs", "The kiln needs 8 logs on the straw first");
        add(kiln + "needs_walls", "The kiln needs solid blocks on all four sides");
        add(kiln + "needs_air", "The kiln needs open air above it");
        add(kiln + "rain", "Rain would put the fire straight out");

        String subtitles = "subtitles." + StrataIndustria.MOD_ID + ".";
        add(subtitles + "clay.shape", "Clay squelches");
        add(subtitles + "clay.finish", "Clay piece formed");
        add(subtitles + "pit_kiln.straw", "Straw laid");
        add(subtitles + "pit_kiln.log", "Log stacked");
        add(subtitles + "pit_kiln.fired", "Kiln burns out");
        add(subtitles + "heat.quench", "Hot metal hisses");
        add(subtitles + "heat.sear", "Skin sears");
        add(subtitles + "forge.ignite", "Forge catches");
        add(subtitles + "crucible.melt", "Metal melts");
        add(subtitles + "quern.grind", "Quern grinds");
        add(subtitles + "anvil.hit", "Hammer rings");
        add(subtitles + "prospect", "Pick taps rock");
        add(subtitles + "journal.open", "Pages turn");
        add(subtitles + "bloomery.light", "Bloomery catches");
        add(subtitles + "bloomery.roar", "Bloomery roars");
        add(subtitles + "bloomery.charge", "Bloomery charged");
        add(subtitles + "bloomery.done", "Bloom settles");
        add(subtitles + "bloomery.extract", "Bloom pulled out");
        add(subtitles + "raw_bloom.hit", "Bloom thuds");
        add(subtitles + "wrought_iron.hit", "Iron rings");
        add(subtitles + "anvil.done", "Piece finished");
        add(subtitles + "anvil.dress", "Stone dressed");
        add(subtitles + "anvil.weld", "Metal welds");
        add(subtitles + "hand_crank.turn", "Crank creaks");
        add(subtitles + "water_wheel.turn", "Water wheel splashes");
        add(subtitles + "kinetic.overstress", "Machinery groans to a halt");
        add(subtitles + "millstone.grind", "Millstone grinds");
        add(subtitles + "bellows.pump", "Bellows wheeze");
        add(subtitles + "saw_mill.saw", "Saw rasps");
        add(subtitles + "saw_mill.blade_break", "Saw blade snaps");
        add(subtitles + "core_sampler.drill", "Core drill grinds");
        add(subtitles + "core_sampler.done", "Core sample ready");
        add(subtitles + "core_sample.open", "Core sample examined");
        add(subtitles + "sluice.wash", "Sluice water runs");
        add(subtitles + "windmill.turn", "Windmill creaks");
        add(subtitles + "belt.attach", "Belt fastened");
        add(subtitles + "soaking_barrel.seal", "Barrel lid thuds shut");
        add(subtitles + "soaking_barrel.open", "Barrel lid scrapes open");
        add(subtitles + "soaking_barrel.fill", "Water splashes into barrel");
        add(subtitles + "soaking_barrel.done", "Barrel soak finishes");
        add(subtitles + "hide.scrape", "Knife scrapes hide");
        add(subtitles + "washing_pan.swirl", "Pan swirls");
        add(subtitles + "washing_pan.find", "Something glints");
        add(subtitles + "anvil.weld_fail", "Weld refused");
        add(subtitles + "quern.load", "Quern loaded");
        add(subtitles + "quern.done", "Quern spills ground");
        add(subtitles + "crucible.pour", "Molten metal pours");
        add(subtitles + "mold.knock", "Cast knocked out");
        add(subtitles + "mold.break", "Mold cracks");
        add(subtitles + "knapping.rock", "Stone chips");
        add(subtitles + "knapping.flint", "Flint chips");
        add(subtitles + "knapping.finish", "Stone tool knapped");
        add(subtitles + "firestarter.drill", "Bow drill whirs");
        add(subtitles + "fire_pit.ignite", "Fire catches");
        add(subtitles + "fire_pit.extinguish", "Fire goes out");
        add(subtitles + "fire_pit.torch", "Torch lit");

        for (Rock rock : Rock.values()) {
            String name = title(rock.id());
            addBlock(ModBlocks.RAW_ROCK.get(rock), "Raw " + name);
            addBlock(ModBlocks.COBBLED_ROCK.get(rock), "Cobbled " + name);
            addBlock(ModBlocks.LOOSE_ROCK.get(rock), "Loose " + name);
            for (OreMineral mineral : OreMineral.inRockValues()) {
                addBlock(ModBlocks.ORES.get(rock).get(mineral), name + " " + title(mineral.id()) + " Ore");
            }
        }
        for (OreMineral mineral : OreMineral.values()) {
            String name = title(mineral.id());
            addBlock(ModBlocks.SMALL_ORES.get(mineral), "Small " + name);
            if (!mineral.hasPieces()) continue;
            for (OreGrade grade : OreGrade.values()) {
                String graded = grade == OreGrade.NORMAL ? name : title(grade.getSerializedName()) + " " + name;
                addItem(ModItems.ORE_PIECES.get(mineral).get(grade), graded);
                addItem(ModItems.CRUSHED_ORES.get(mineral).get(grade), "Crushed " + graded);
                if (mineral.washable()) addItem(ModItems.WASHED_ORES.get(mineral).get(grade), "Washed " + graded);
            }
        }
        ironAge();
        tier4();
        addBlock(ModBlocks.LOOSE_STICK, "Loose Stick");
        addBlock(ModBlocks.LOOSE_FLINT, "Loose Flint");

        String config = StrataIndustria.MOD_ID + ".configuration.";
        add(config + "title", "Strata Industria Configs");
        add(config + "section." + StrataIndustria.MOD_ID + ".common.toml", "Strata Industria Configs");
        add(config + "section." + StrataIndustria.MOD_ID + ".common.toml.title", "Strata Industria Configs");
        add(config + "showDebugInfo", "Show debug info");
        add(config + "worldgen", "World Generation");
        add(config + "provinceScale", "Province size");
        add(config + "veinAttemptsPerCell", "Vein attempts per cell");
        add(config + "shallowBias", "Shallow vein bias");
        add(config + "emptyWeight", "Empty vein weight");
        add(config + "veinSizeMultiplier", "Vein size multiplier");
        add(config + "indicatorDepth", "Indicator depth");
        add(config + "indicatorDensity", "Indicator density");
        add(config + "spawnGuarantee", "Copper near spawn");
        add(config + "vanilla", "Vanilla Changes");
        add(config + "logsNeedAxe", "Logs need an axe");
        add(config + "removeWoodTools", "Remove wooden tools");
        add(config + "removeStoneTools", "Remove stone tools");
        add(config + "planksNeedTools", "Planks need tools");
        add(config + "removeCampfire", "Remove campfire recipe");
        add(config + "gateFurnace", "Gate the furnace");
        add(config + "replaceCopperGear", "Replace copper gear recipes");
        add(config + "leavesDropSticks", "Leaves drop sticks");
        add(config + "gravelFlintChance", "More flint from gravel");
        add(config + "kiln", "Pit Kiln");
        add(config + "burnTicks", "Burn time (ticks)");
        add(config + "charcoal", "Charcoal Pit");
        add(config + "perLog", "Charcoal per log");
        add(config + "crucible", "Crucible");
        add(config + "capacity", "Capacity (units)");
        add(config + "casting", "Casting");
        add(config + "ingotMoldBreak", "Ingot mold break chance");
        add(config + "toolMoldBreak", "Tool mold break chance");
        add(config + "smithing", "Smithing");
        add(config + "randomTargets", "Random targets per world");
        add(config + "removeFurnaceCharcoal", "Remove furnace charcoal");
        add(config + "removeOreSmelting", "Remove ore smelting");
        add(config + "removeBlastFurnace", "Remove blast furnace recipe");
        add(config + "replaceIronGear", "Replace iron gear recipes");
        add(config + "replaceGoldGear", "Replace gold gear recipes");
        add(config + "golemDropsNuggets", "Iron golems drop nuggets");
        add(config + "hitCooling", "Cooling per hit on iron");
        add(config + "heat", "Item Heat");
        add(config + "burnPlayer", "Hot items burn bare hands");
        add(config + "bloomery", "Bloomery");
        add(config + "runTicks", "Bloomery run time");
        add(config + "journal", "Field Journal");
        add(config + "steam", "Steam");
        add(config + "blastFurnace", "Blast Furnace");
        add(config + "giveOnJoin", "Give a journal on first join");
    }

    /** Tier 3 spec 3 and 4: fire clay, the new deposits, and the wrought iron name for vanilla iron. */
    // Tier 4 spec 4.6: materials.
    private void tier4() {
        addItem(ModItems.STEEL_DOUBLE_INGOT, "Steel Double Ingot");
        addItem(dev.strataindustria.registry.Tier4Items.SULFUR, "Sulfur");
        addItem(dev.strataindustria.registry.Tier4Items.SULFUR_DUST, "Sulfur Dust");
        addItem(dev.strataindustria.registry.Tier4Items.CHARCOAL_DUST, "Charcoal Dust");

        // Tier 4 spec 5: coke, the coke oven and creosote.
        String id = StrataIndustria.MOD_ID;
        addItem(Tier4Items.COKE, "Coke");
        addItem(Tier4Items.COKE_DUST, "Coke Dust");
        addBlock(Tier4Blocks.COKE_BLOCK, "Block of Coke");
        addItem(Tier4Items.UNFIRED_COKE_OVEN_BRICK, "Unfired Coke Oven Brick");
        addItem(Tier4Items.COKE_OVEN_BRICK, "Coke Oven Brick");
        addBlock(Tier4Blocks.COKE_OVEN_BRICKS, "Coke Oven Bricks");
        addBlock(Tier4Blocks.COKE_OVEN_DOOR, "Coke Oven Door");
        addItem(Tier4Items.CREOSOTE_BUCKET, "Creosote Bucket");
        add("fluid_type." + id + ".creosote", "Creosote");
        addBlock(Tier4Blocks.TREATED_PLANKS, "Treated Planks");
        addBlock(Tier4Blocks.TREATED_SLAB, "Treated Slab");
        addBlock(Tier4Blocks.TREATED_STAIRS, "Treated Stairs");
        addBlock(Tier4Blocks.TREATED_FENCE, "Treated Fence");
        addItem(Tier4Items.TREATED_STICK, "Treated Stick");
        String oven = id + ".coke_oven.";
        add("container." + id + ".coke_oven", "Coke Oven");
        add(oven + "status.incomplete", "Oven incomplete");
        add(oven + "status.empty", "Empty");
        add(oven + "status.no_recipe", "This will not coke");
        add(oven + "status.working", "Baking... %s%%");
        add(oven + "status.output_full", "Output full");
        add(oven + "status.tank_full", "Creosote tank full");
        add(oven + "problem.brick", "Needs bricks at %s, %s, %s");
        add(oven + "problem.air", "Needs air at %s, %s, %s");
        add(oven + "tank", "Creosote %s / %s mB");
        add(oven + "tank_empty", "No creosote yet");
        String subtitles = "subtitles." + id + ".";
        add(subtitles + "coke_oven.working", "Coke oven smoulders");
        add(subtitles + "coke_oven.done", "Coke settles");
        add(subtitles + "creosote.fill", "Creosote glugs");
        add(subtitles + "crucible.carbon_burn", "Carbon burns off");
        add(subtitles + "crucible.calcine_reduce", "Calcine reduces");
        add(subtitles + "roasting.sizzle", "Ore roasts");
        add(subtitles + "roasting.done", "Calcine crumbles");

        // Spec 6: refractory ceramics and steel in the crucible.
        addItem(Tier4Items.UNFIRED_REFRACTORY_CRUCIBLE, "Unfired Refractory Crucible");
        addBlock(Tier4Blocks.REFRACTORY_CRUCIBLE, "Refractory Crucible");
        addItem(Tier4Items.UNFIRED_REFRACTORY_INGOT_MOLD, "Unfired Refractory Ingot Mold");
        addItem(Tier4Items.REFRACTORY_INGOT_MOLD, "Refractory Ingot Mold");
        addItem(Tier4Items.UNFIRED_REFRACTORY_GEAR_MOLD, "Unfired Refractory Gear Mold");
        addItem(Tier4Items.REFRACTORY_GEAR_MOLD, "Refractory Gear Mold");
        addItem(Tier4Items.UNFIRED_GEAR_MOLD, "Unfired Gear Mold");
        addItem(Tier4Items.GEAR_MOLD, "Gear Mold");
        add("container." + id + ".refractory_crucible", "Refractory Crucible");
        String status = id + ".crucible.status.";
        add(status + "at_limit", "Crucible at its limit (%s °C)");
        add(status + "carbon_waiting", "Carbon needs molten iron or calcine");
        add(status + "calcine_short", "Calcine needs more carbon");
        add(status + "carbon_burned", "Excess carbon burned off");
        String problem = id + ".crucible.problem.";
        add(problem + "mold_too_weak", "This mold cannot take metal this hot; use a refractory mold");
        add(problem + "no_gear", "That metal does not make gears");
        String config = id + ".configuration.";
        add(config + "clayMaxTemperature", "Clay crucible limit (°C)");
        add(config + "refractoryMoldBreak", "Refractory mold break chance");
        // Spec 4.4, 5.3 and 14.4: zinc calcine, its gas, and the steel anvil.
        addItem(Tier4Items.ZINC_CALCINES.get(dev.strataindustria.geology.OreGrade.POOR), "Poor Zinc Calcine");
        addItem(Tier4Items.ZINC_CALCINES.get(dev.strataindustria.geology.OreGrade.NORMAL), "Zinc Calcine");
        addItem(Tier4Items.ZINC_CALCINES.get(dev.strataindustria.geology.OreGrade.RICH), "Rich Zinc Calcine");
        addItem(Tier4Items.SMALL_ZINC_CALCINE, "Small Zinc Calcine");
        add("fluid_type." + id + ".sulfur_dioxide", "Sulfur Dioxide");
        addBlock(Tier4Blocks.STEEL_ANVIL, "Steel Anvil");
        addBlock(Tier4Blocks.IRON_AXLE, "Iron Axle");
        addBlock(Tier4Blocks.IRON_GEARBOX, "Iron Gearbox");
        addBlock(Tier4Blocks.IRON_STEP_UP_GEARBOX, "Iron Step-up Gearbox");
        add(id + ".ore.sulfide", "Sulfide ore: roast it in a forge before melting");

        // Spec 8.1, 9 and 10: the firebox, pipes, the gauge and the bronze boiler.
        addBlock(Tier4Blocks.FIREBOX, "Firebox");
        addBlock(Tier4Blocks.BRONZE_BOILER, "Bronze Boiler");
        addBlock(Tier4Blocks.CRACKED_BRONZE_BOILER, "Cracked Bronze Boiler");
        addBlock(Tier4Blocks.COPPER_FLUID_PIPE, "Copper Fluid Pipe");
        addBlock(Tier4Blocks.BRONZE_FLUID_PIPE, "Bronze Fluid Pipe");
        addBlock(Tier4Blocks.STEEL_FLUID_PIPE, "Steel Fluid Pipe");
        addBlock(Tier4Blocks.PRESSURE_GAUGE, "Pressure Gauge");
        addBlock(Tier4Blocks.COPPER_HEAT_PIPE, "Copper Heat Pipe");
        addBlock(Tier4Blocks.REFRACTORY_HEAT_DUCT, "Refractory Heat Duct");
        addBlock(Tier4Blocks.HEAT_INLET, "Heat Inlet");
        addBlock(Tier4Blocks.INSULATED_COPPER_HEAT_PIPE, "Insulated Copper Heat Pipe");
        addBlock(Tier4Blocks.INSULATED_REFRACTORY_HEAT_DUCT, "Insulated Refractory Heat Duct");
        addBlock(Tier4Blocks.KILN, "Kiln");
        addItem(Tier4Items.SLAG_WOOL, "Slag Wool");
        add("container." + id + ".kiln", "Kiln");
        String kilnStatus = id + ".kiln.status.";
        add(kilnStatus + "empty", "Load clay to fire");
        add(kilnStatus + "no_recipe", "That does not fire");
        add(kilnStatus + "output_full", "Output full");
        add(kilnStatus + "needs_heat", "Needs heat");
        add(kilnStatus + "firing", "Firing (%s%%)");
        addBlock(Tier4Blocks.ROASTER, "Roaster");
        add("container." + id + ".roaster", "Roaster");
        String roaster = id + ".roaster.";
        add(roaster + "status.empty", "Load sulfide ore to roast");
        add(roaster + "status.no_recipe", "That does not roast");
        add(roaster + "status.output_full", "Output full");
        add(roaster + "status.needs_heat", "Needs heat");
        add(roaster + "status.roasting", "Roasting");
        add(roaster + "status.venting", "Venting sulfur dioxide");
        add(roaster + "tank", "Sulfur Dioxide %s / %s mB");
        addBlock(Tier4Blocks.SMELTER, "Smelter");
        add("container." + id + ".smelter", "Smelter");
        String smelter = id + ".smelter.";
        add(smelter + "auto_on", "Auto-pour: On");
        add(smelter + "auto_off", "Auto-pour: Off");
        add(smelter + "cooling", "%s °C");
        add(smelter + "slot.mold", "Mold being filled");
        add(smelter + "slot.stock", "Spare empty molds (up to 16)");
        add(smelter + "slot.cooling", "Filled mold cooling");
        add(smelter + "slot.output", "Castings");
        add(id + ".crucible.status.no_heat", "Needs heat from a firebox or heat pipes");
        add("fluid_type." + id + ".steam", "Steam");
        add("container." + id + ".firebox", "Firebox");
        add("container." + id + ".bronze_boiler", "Bronze Boiler");
        String firebox = id + ".firebox.";
        add(firebox + "status.empty", "Add fuel");
        add(firebox + "status.idle", "Burning, %s HU/t wasted");
        add(firebox + "status.heating", "Heating: %s / %s HU/t");
        add(firebox + "status.cooling", "Out of fuel, cooling");
        add(firebox + "temperature", "%s °C");
        add(firebox + "blower", "Blower: +%s °C, 1.5× heat");
        // Spec 8.3: the line a multiblock shows about the heat coming in through its inlets.
        String heatLine = id + ".heat_line.";
        add(heatLine + "hot_blast", "Hot blast");
        add(heatLine + "preheat", "Preheat");
        add(heatLine + "kiln", "Heat");
        add(heatLine + "roaster", "Heat");
        add(heatLine + "smelter", "Heat");
        add(heatLine + "steam_hammer", "Heat");
        add(heatLine + "none", "%s: no heat");
        add(heatLine + "cold", "%s: %3$s of %2$s °C");
        add(heatLine + "limited", "%s: pipes cap %s °C");
        add(heatLine + "fine", "%s: %s °C, %s HU/t");
        add(heatLine + "limited_by", "Limited by %s (%s °C)");
        add(heatLine + "needs", "Needs %s °C");
        add(heatLine + "pipes", "the pipes");
        String boiler = id + ".boiler.";
        add(boiler + "status.no_heat", "No heat");
        add(boiler + "status.too_cool", "Fire too cool: %2$s °C");
        add(boiler + "status.no_water", "No water");
        add(boiler + "status.heating", "Heating water (%s%%)");
        add(boiler + "status.running", "Making steam");
        add(boiler + "status.venting", "Venting: no demand");
        add(boiler + "status.pipe_too_hot", "Pipe too weak");
        add(boiler + "status.low_water", "Low water");
        add(boiler + "status.dry_firing", "Dry firing! %s%%");
        add(boiler + "pressure", "%s bar");
        add(boiler + "heat", "%s HU/t");
        add(boiler + "fire", "%s °C");
        add(boiler + "water", "Water %s / %s mB");
        add(boiler + "steam", "Steam %s / %s mB");
        add(boiler + "integrity", "Integrity %s%%");
        addBlock(Tier4Blocks.VALVE, "Valve");
        add(id + ".valve.held_shut", "Opened, but a redstone signal holds it shut");
        addBlock(Tier4Blocks.FLUID_TANK, "Fluid Tank");
        add(id + ".fluid_tank.empty", "Empty, holds %s mB (%s tanks)");
        add(id + ".fluid_tank.holds", "%s / %s mB of %s (%s tanks)");
        add(subtitles + "valve.open", "Valve opens");
        add(subtitles + "valve.close", "Valve shuts");
        add(subtitles + "fluid_tank.fill", "Tank fills");
        String gauge = id + ".pressure_gauge.";
        add(gauge + "empty", "Nothing flowing");
        add(gauge + "pressure", "Steam at %s bar");
        add(gauge + "flow", "%s flowing, %s mB/t");
        add(subtitles + "firebox.light", "Firebox catches");
        add(subtitles + "firebox.burn", "Firebox roars");
        add(subtitles + "heat_pipe.tick", "Heat pipe ticks");
        add(subtitles + "kiln.work", "Kiln roars");
        add(subtitles + "kiln.done", "Kiln batch fired");
        add(subtitles + "smelter.pour", "Smelter pours");
        add(subtitles + "smelter.bubble", "Molten metal bubbles");
        add(subtitles + "fluid_pipe.refuse", "Pipe clanks");
        add(subtitles + "boiler.heat", "Boiler creaks");
        add(subtitles + "boiler.run", "Boiler rumbles");
        add(subtitles + "boiler.vent", "Safety valve whistles");
        add(subtitles + "boiler.low_water", "Boiler warns of low water");
        add(subtitles + "boiler.dry_fire", "Boiler groans");
        add(subtitles + "boiler.steam_burst", "Steam bursts");
        add(subtitles + "boiler.crack", "Boiler cracks");
        add(subtitles + "boiler.repair", "Boiler patched");
        add(config + "boilerExplosions", "Cracking boilers explode");
        add(config + "dryFiringDamagePerSecond", "Dry firing damage per second (%)");
        add(config + "engineCapacity", "Steam engine capacity (SU)");
        addBlock(Tier4Blocks.MECHANICAL_PUMP, "Mechanical Pump");
        addBlock(Tier4Blocks.STEAM_ENGINE, "Steam Engine");
        addBlock(Tier4Blocks.STEAM_HAMMER, "Steam Hammer");
        add("container." + id + ".steam_hammer", "Steam Hammer");
        String steamHammer = id + ".steam_hammer.";
        add(steamHammer + "status.no_pattern", "Needs a recorded pattern");
        add(steamHammer + "status.outdated_pattern", "Outdated pattern: record it again");
        add(steamHammer + "status.waiting", "Waiting for a workpiece");
        add(steamHammer + "status.wrong_piece", "The pattern does not fit this piece");
        add(steamHammer + "status.output_full", "Output full");
        add(steamHammer + "status.no_steam", "Needs steam at 1 bar or more");
        add(steamHammer + "status.too_cold", "Workpiece too cold");
        add(steamHammer + "status.heating", "Heating the piece: %s of %s °C");
        add(steamHammer + "status.working", "Working (%s / %s hits)");
        add(steamHammer + "steam.none", "Steam: none");
        add(steamHammer + "steam.slow", "Steam: %s bar, half speed");
        add(steamHammer + "steam.full", "Steam: %s bar");
        add(steamHammer + "buffer", "Steam: %s / %s mB");
        add(steamHammer + "slot.pattern", "Recorded smithing pattern");
        add(steamHammer + "slot.input", "Workpieces, cold or hot");
        add(steamHammer + "slot.piece", "On the anvil");
        add(steamHammer + "slot.result", "Finished pieces");
        add(subtitles + "steam_hammer.strike", "Steam hammer strikes");
        String engine = id + ".steam_engine.";
        add(engine + "no_steam", "No steam");
        add(engine + "low_pressure", "Steam pressure under 1 bar");
        add(engine + "running", "%s RPM on %s bar");
        String pump = id + ".mechanical_pump.status.";
        add(pump + "not_turning", "Not turning");
        add(pump + "too_slow", "Too slow: needs 8 RPM");
        add(pump + "no_water", "No water source at the intake");
        add(pump + "no_outlet", "No pipe at the outlet");
        add(pump + "outlet_full", "Nothing takes the water");
        add(pump + "pumping", "Pumping %s mB/t");
        add(subtitles + "steam_engine.chuff", "Steam engine chuffs");
        add(subtitles + "steam_engine.start", "Steam engine starts");
        add(subtitles + "steam_engine.stop", "Steam engine stops");
        add(subtitles + "mechanical_pump.run", "Pump thumps");
        addBlock(Tier4Blocks.CRUSHER, "Crusher");
        add("container." + id + ".crusher", "Crusher");
        add(subtitles + "crusher.crush", "Crusher grinds");
        addBlock(Tier4Blocks.WASHER, "Washer");
        add("container." + id + ".washer", "Washer");
        add(subtitles + "washer.wash", "Washer sloshes");
        add(id + ".machine.no_water", "Needs water: pipe it in");
        add(id + ".washer.water", "Water: %s / %s mB");

        // Spec 12.1 and 11.6: the blast furnace and the blower.
        addBlock(Tier4Blocks.REFRACTORY_CASING, "Refractory Casing");
        addBlock(Tier4Blocks.BLAST_FURNACE_CONTROLLER, "Blast Furnace Controller");
        addBlock(Tier4Blocks.TUYERE, "Tuyere");
        addBlock(Tier4Blocks.CHARGING_HATCH, "Charging Hatch");
        addBlock(Tier4Blocks.TAP_HATCH, "Tap Hatch");
        addBlock(Tier4Blocks.BLOWER, "Blower");
        addBlock(Tier4Blocks.BLOWING_ENGINE, "Blowing Engine");
        add(id + ".blowing_engine.full", "Blowing hard, worth two blowers (%s bar)");
        add(id + ".blowing_engine.half", "Half stroke, worth one blower (%s bar)");
        add(id + ".blowing_engine.no_steam", "Needs steam at 1 bar or more at the back");
        add("subtitles." + id + ".blowing_engine.stroke", "Blowing engine chuffs");
        addItem(Tier4Items.SLAG, "Slag");
        addItem(Tier4Items.SLAG_DUST, "Slag Dust");
        add("container." + id + ".blast_furnace", "Blast Furnace");
        String furnace = id + ".blast_furnace.";
        add(furnace + "status.incomplete", "Structure incomplete");
        add(furnace + "status.no_air", "No air blast");
        add(furnace + "status.needs_fuel", "Needs coke");
        add(furnace + "status.heating", "Heating hearth (%s%%)");
        add(furnace + "status.needs_iron", "Needs iron");
        add(furnace + "status.needs_flux", "Needs flux");
        add(furnace + "status.output_full", "Output full");
        add(furnace + "status.running", "Running, air from %s blower(s)");
        add(furnace + "problem.needs_casing", "Refractory casing: layer %s, %s");
        add(furnace + "problem.needs_refractory", "Refractory block: layer %s, %s");
        add(furnace + "problem.needs_air", "Clear the shaft: layer %s, %s");
        add(furnace + "problem.needs_charging_hatch", "Charging hatch: layer %s, %s");
        add(furnace + "problem.needs_tap_hatch", "Tap hatch: layer %s, an edge");
        add(furnace + "problem.needs_tuyere", "Tuyere: layer %s, an edge");
        String[] spots = {"front left", "front", "front right", "middle left", "middle", "middle right", "back left", "back", "back right"};
        for (int i = 0; i < spots.length; i++) add(furnace + "spot." + i, spots[i]);
        add(furnace + "iron", "Iron: %s / %s units");
        add(furnace + "fuel", "Coke: %s / %s");
        add(furnace + "flux", "Flux: %s / %s");
        add(furnace + "hearth", "Hearth heat: %s%%");
        add(subtitles + "blower.run", "Blower whooshes");
        add(subtitles + "blast_furnace.roar", "Blast furnace roars");
        add(subtitles + "blast_furnace.tap", "Pig iron pours");
        add(subtitles + "multiblock.form", "Structure complete");
        addBlock(Tier4Blocks.CONVERTER_CONTROLLER, "Converter Controller");
        add("container." + id + ".converter", "Converter");
        String converter = id + ".converter.";
        add(converter + "status.incomplete", "Structure incomplete");
        add(converter + "status.empty", "Needs pig iron");
        add(converter + "status.charging", "Charging: waiting for more pig iron");
        add(converter + "status.no_air", "No air blast");
        add(converter + "status.needs_preheat", "Needs preheat: add a coke");
        add(converter + "status.output_full", "Output full");
        add(converter + "status.blowing", "Blowing (%s%%)");
        add(converter + "charge_pig", "Pig iron in the blow: %s / %s");
        add(converter + "charge_scrap", "Scrap in the blow: %s / %s");
        add(converter + "preheated", "Preheated with coke");
        add(converter + "not_preheated", "One coke preheats each blow");
        add(converter + "flame", "The flame climbs to white, then drops when the steel is done");
        add(subtitles + "converter.blow", "Converter roars");
        add(subtitles + "converter.done", "Converter blow ends");
        add(config + "ticksPerIngot", "Ticks per pig iron ingot");
        add(config + "warmupTicks", "Hearth warm-up (ticks)");
    }

    private void ironAge() {
        String id = StrataIndustria.MOD_ID;
        addItem(ModItems.FIRE_CLAY_BALL, "Fire Clay Ball");
        addItem(ModItems.GROG, "Grog");
        addItem(ModItems.UNFIRED_FIRE_BRICK, "Unfired Fire Brick");
        addItem(ModItems.FIRE_BRICK, "Fire Brick");
        addBlock(ModBlocks.FIRE_CLAY, "Fire Clay");
        addBlock(ModBlocks.FIRE_BRICKS, "Fire Bricks");
        addBlock(ModBlocks.FIRE_BRICK_SLAB, "Fire Brick Slab");
        addBlock(ModBlocks.FIRE_BRICK_STAIRS, "Fire Brick Stairs");
        addBlock(ModBlocks.FIRE_BRICK_WALL, "Fire Brick Wall");
        addItem(ModItems.LIGNITE, "Lignite");
        addBlock(ModBlocks.LIGNITE_SEAM, "Lignite Seam");
        addBlock(ModBlocks.BOG_IRON, "Bog Iron");
        addBlock(ModBlocks.PLACER_GRAVEL, "Placer Gravel");
        addBlock(ModBlocks.PLACER_SAND, "Placer Sand");
        addItem(ModItems.WROUGHT_IRON_ROD, "Wrought Iron Rod");
        addItem(ModItems.WROUGHT_IRON_DOUBLE_INGOT, "Wrought Iron Double Ingot");
        // Spec 2: the vanilla iron ingot and nugget are wrought iron.
        add("item.minecraft.iron_ingot", "Wrought Iron Ingot");
        add("item.minecraft.iron_nugget", "Wrought Iron Nugget");
        add(id + ".ore.lignite_seam", "lignite");
        add(id + ".ore.fire_clay", "fire clay");
        add(id + ".prospect.rich", ", rich ore nearby");
        add(id + ".crucible.no_iron", "Iron does not melt this hot");

        // Spec 5: the bloomery.
        addBlock(ModBlocks.BLOOMERY, "Bloomery");
        addItem(ModItems.RAW_BLOOM, "Raw Bloom");
        addItem(ModItems.BLOOMERY_SLAG, "Bloomery Slag");
        String bloomery = id + ".bloomery.";
        add("container." + id + ".bloomery", "Bloomery");
        add(bloomery + "status.incomplete", "Structure incomplete");
        add(bloomery + "status.empty", "Empty");
        add(bloomery + "status.charged", "Charged, ready to light");
        add(bloomery + "status.needs_charcoal", "Needs %s more charcoal");
        add(bloomery + "status.heating", "Heating (%s °C)");
        add(bloomery + "status.too_cool", "Too cool: needs 1200 °C");
        add(bloomery + "status.burning", "Burning (%s%%)");
        add(bloomery + "status.ready", "Bloom ready: use a pickaxe");
        add(bloomery + "problem.brick", "Needs fire bricks");
        add(bloomery + "problem.air", "Needs to be open");
        add(bloomery + "at", "at %s, %s, %s");
        add(bloomery + "hint", "Bricks around and under the chamber.");
        add(bloomery + "charge", "Ore %s/%s, charcoal %s/%s");
        add(bloomery + "draught", "Flue %s, bellows %s: %s °C");
        add(bloomery + "yield", "Yield %s%%, %s blooms");
        add(bloomery + "no_yield", "Too cool for a bloom");
        add(bloomery + "not_iron", "Only iron-bearing items go in a bloomery");
        add(bloomery + "lignite", "Lignite burns too cool for a bloomery");
        add(bloomery + "busy", "Not while it burns or holds a bloom");
        add(bloomery + "full", "The bloomery is full");
        add(bloomery + "bloom_units", "%s units of iron");
    }

    /** Spec 11: the field journal's goals and hints. */
    private void journal() {
        String journal = "journal." + StrataIndustria.MOD_ID + ".";
        add(journal + "root", "Field Journal");
        add(journal + "root.hint", "Every goal of the field journal at a glance. Switch this tab on in the config to open it from the journal.");
        add(journal + "t0.loose_rock", "A Rock to Start With");
        add(journal + "t0.loose_rock.hint", "Pick up a loose rock lying on the ground.");
        add(journal + "t0.knap", "First Edge");
        add(journal + "t0.knap.hint", "Hold two loose rocks or one flint, use it, and strike away the waste until a tool head is left.");
        add(journal + "t0.stone_axe", "Stone Axe");
        add(journal + "t0.stone_axe.hint", "Bind a knapped axe head to a stick with twine.");
        add(journal + "t0.log", "Timber");
        add(journal + "t0.log.hint", "Fell a tree with your axe. Bare hands will not do.");
        add(journal + "t0.crafting_table", "A Proper Workbench");
        add(journal + "t0.crafting_table.hint", "Build a crafting table.");
        add(journal + "t0.twine", "Twisted Fibre");
        add(journal + "t0.twine.hint", "Cut grass with a knapped knife blade for plant fibre, then twist two fibres into twine.");
        add(journal + "t0.fire", "Firelight");
        add(journal + "t0.fire.hint", "Build a fire pit from sticks and straw, put fuel in, and hold a firestarter on it.");
        add(journal + "t0.clay", "Riverbank Clay");
        add(journal + "t0.clay.hint", "Gather five clay balls. Look along riverbanks and in swamps.");
        add(journal + "t1.clay_forming", "Shaped by Hand");
        add(journal + "t1.clay_forming.hint", "Form something from clay: use five clay balls to open the forming grid.");
        add(journal + "t1.pit_kiln", "Fired Pottery");
        add(journal + "t1.pit_kiln.hint", "Sneak and place unfired clay on the floor of a one-block pit, add 8 straw and 8 logs, then light it and let it burn out.");
        add(journal + "t1.charcoal", "Charcoal Burner");
        add(journal + "t1.charcoal.hint", "Sneak and place logs into a pile, light it, cover every face with soil or stone, then dig out the charcoal.");
        add(journal + "t1.forge", "The Forge");
        add(journal + "t1.forge.hint", "Build a forge. It burns charcoal far hotter than a fire pit.");
        add(journal + "t1.nugget", "Signs in the Soil");
        add(journal + "t1.nugget.hint", "Find a small ore on the surface. A vein lies somewhere below it.");
        add(journal + "t1.crucible", "Vessel and Mold");
        add(journal + "t1.crucible.hint", "Fire a crucible and an ingot mold in the pit kiln.");
        add(journal + "t2.melt", "Molten");
        add(journal + "t2.melt.hint", "Set a crucible on top of a lit forge and melt ore in it.");
        add(journal + "t2.copper_ingot", "First Metal");
        add(journal + "t2.copper_ingot.hint", "Pour molten copper into an ingot mold and knock the ingot out once it cools.");
        add(journal + "t2.copper_pickaxe", "Copper Pick");
        add(journal + "t2.copper_pickaxe.hint", "Make a copper pickaxe. It breaks ore a stone pick cannot.");
        add(journal + "t2.alloy_metal", "Tin, Bismuth or Arsenic");
        add(journal + "t2.alloy_metal.hint", "Mine cassiterite, bismuthinite or tennantite ore.");
        add(journal + "t2.quern", "Ground Fine");
        add(journal + "t2.quern.hint", "Knap a quernstone from four loose rocks and stack two under a stick for a quern. Put ore in and keep turning it; crushed ore melts down to more metal.");
        add(journal + "t2.bronze", "Bronze Age");
        add(journal + "t2.bronze.hint", "Melt copper with tin, bismuth or arsenic in the right shares and cast a bronze ingot.");
        add(journal + "t2.stone_anvil", "Dressed Stone");
        add(journal + "t2.stone_anvil.hint", "Sneak and strike the top of raw igneous rock with a hammer to dress it into an anvil.");
        add(journal + "t2.smith", "Hammer Work");
        add(journal + "t2.smith.hint", "Heat an ingot in the forge and smith it into a plate on an anvil.");
        add(journal + "t2.bronze_tools", "Full Kit");
        add(journal + "t2.bronze_tools.hint", "Own a bronze pickaxe, axe, shovel, knife, hammer, saw and sword.");
        add(journal + "t2.bronze_armour", "Clad in Bronze");
        add(journal + "t2.bronze_armour.hint", "Wear a full set of bronze armour.");
        add(journal + "t2.prospectors_pick", "Listening to the Rock");
        add(journal + "t2.prospectors_pick.hint", "Strike rock with a prospector's pick to learn what ore lies nearby.");
        add(journal + "t2.bronze_anvil", "Bronze Anvil");
        add(journal + "t2.bronze_anvil.hint", "Build a bronze anvil, the last tool of the bronze age.");
        add(journal + "t3.fire_clay", "Fire Clay");
        add(journal + "t3.fire_clay.hint", "Bronze cannot melt iron. You will need a bloomery, and it is built of fire bricks. "
                + "Dig fire clay from the pale beds near the surface in shale and slate.");
        add(journal + "t3.fire_brick", "Fire Brick");
        add(journal + "t3.fire_brick.hint", "Form fire clay into bricks and fire them in a pit kiln.");
        add(journal + "t3.iron_ore", "Iron Ore");
        add(journal + "t3.iron_ore.hint", "Iron hides in shale and slate, under swamps, and deep in dark igneous rock. "
                + "Your prospector's pick can find it.");
        add(journal + "t3.bloomery", "Bloomery");
        add(journal + "t3.bloomery.hint", "A bloomery is a chimney of fire bricks with a door in front. Two levels are enough to start.");
        add(journal + "t3.bloom", "Iron Bloom");
        add(journal + "t3.bloom.hint", "Charge the bloomery with iron ore and charcoal, light it, and wait for the bloom.");
        add(journal + "t3.refine", "Wrought Iron");
        add(journal + "t3.refine.hint", "Hammer a hot bloom on a bronze anvil to squeeze out the slag.");
        add(journal + "t3.iron_pickaxe", "Iron Pickaxe");
        add(journal + "t3.iron_pickaxe.hint", "Smith a wrought iron pickaxe head and fit it to a handle.");
        add(journal + "t3.bucket", "Bucket");
        add(journal + "t3.bucket.hint", "Wrought iron is finally enough for a bucket.");
        add(journal + "t3.furnace", "Furnace");
        add(journal + "t3.furnace.hint", "Eight fire bricks make a furnace.");
        add(journal + "t3.rotation", "Turning");
        add(journal + "t3.rotation.hint", "Put a hand crank on the end of a wooden axle and hold right-click on it.");
        add(journal + "t3.water_power", "Water Power");
        add(journal + "t3.water_power.hint", "Set a water wheel on an axle where flowing water pushes one side of it.");
        add(journal + "t3.millstone", "Millstone");
        add(journal + "t3.millstone.hint", "Drive a millstone from an axle and let it grind for you.");
        add(journal + "t3.bellows", "Bellows");
        add(journal + "t3.bellows.hint", "Turn a bellows with a shaft and point its nozzle into a forge or a bloomery wall.");
        add(journal + "t3.saw_mill", "Saw Mill");
        add(journal + "t3.saw_mill.hint", "Fit a saw blade to a saw mill and saw a log: six planks and the bark.");
        add(journal + "t3.trip_hammer", "Trip Hammer");
        add(journal + "t3.wash", "Panning");
        add(journal + "t3.wash.hint", "Scoop placer gravel from a river bed with a washing pan and swirl it standing in water.");
        add(journal + "t3.core_sample", "Core Sample");
        add(journal + "t3.core_sample.hint", "Turn a core sampler and press Drill to read the ground 64 blocks down.");
        add(journal + "t3.trip_hammer.hint", "Give a trip hammer a recorded pattern and a forge beside its anvil, and let it smith.");
        add(journal + "t3.weld", "Forge Weld");
        add(journal + "t3.weld.hint", "Heat two iron ingots, add flux, and weld them into a double ingot on the anvil.");
        add(journal + "t3.pattern", "Smithing Pattern");
        add(journal + "t3.pattern.hint", "Put a blank pattern in the anvil while you smith, and it records every hit.");
        add(journal + "t3.hide", "Raw Hide");
        add(journal + "t3.hide.hint", "Animals give raw hides now. Leather has to be tanned.");
        add(journal + "t3.leather", "Tanned Leather");
        add(journal + "t3.leather.hint", "Soak hides in lye, scrape them with a knife, then soak them in tannin in a sealed barrel.");
        add(journal + "t3.iron_anvil", "Wrought Iron Anvil");
        add(journal + "t3.iron_anvil.hint", "Build a wrought iron anvil from double ingots. It can work steel later on.");
        add(journal + "t4.coal", "Black Rock");
        add(journal + "t4.coal.hint", "Coal lies in deep shale and arkose seams. Bronze will not cut it.");
        add(journal + "t4.coke_oven", "Coke Oven");
        add(journal + "t4.coke_oven.hint", "Fire coke oven bricks and build a hollow 3x3x3 oven with a door in the middle of one side.");
        add(journal + "t4.coke", "Coke");
        add(journal + "t4.coke.hint", "Bake coal in the coke oven. Coke burns hot enough to melt steel.");
        add(journal + "t4.creosote", "Creosote");
        add(journal + "t4.creosote.hint", "Draw the creosote from the oven into a bucket and treat planks with it.");
        add(journal + "t4.refractory_crucible", "Refractory Crucible");
        add(journal + "t4.refractory_crucible.hint", "Form a crucible from fire clay and fire it. It stands the heat of melting iron.");
        add(journal + "t4.molten_iron", "Molten Iron");
        add(journal + "t4.molten_iron.hint", "Melt iron in a refractory crucible on a forge burning coke, with bellows.");
        add(journal + "t4.steel", "Crucible Steel");
        add(journal + "t4.steel.hint", "Stir coke or charcoal dust into molten iron, one dust to five ingots, and cast it in a refractory mold.");
        add(journal + "t4.sphalerite", "Zinc Blende");
        add(journal + "t4.sphalerite.hint", "Find sphalerite. Zinc is the key to brass.");
        add(journal + "t4.roast", "Roasted Ore");
        add(journal + "t4.roast.hint", "Zinc ore must be roasted before it will melt. A hot forge will do.");
        add(journal + "t4.brass", "Brass");
        add(journal + "t4.brass.hint", "Melt calcine with copper and a little carbon dust. Two parts copper to one of zinc.");
        add(journal + "t4.solder", "Solder");
        add(journal + "t4.solder.hint", "Galena melts like tin ore. Alloy its lead with tin.");
        add(journal + "t4.pipe", "Pipework");
        add(journal + "t4.pipe.hint", "Join copper plates with solder into pipe. Copper carries a bronze boiler's steam.");
        add(journal + "t4.boiler", "Raising Steam");
        add(journal + "t4.boiler.hint", "Set a bronze boiler on a burning firebox, fill it with water and wait for the needle to rise.");
        add(journal + "t4.heat_network", "Heat on the Move");
        add(journal + "t4.heat_network.hint", "Lay four or more heat pipes from a burning firebox to a boiler or a heat inlet. Copper pipe holds the heat down to 1000 °C.");
        add(journal + "t4.steam_engine", "Steam Power");
        add(journal + "t4.steam_hammer", "Hammer with Steam");
        add(journal + "t4.steam_hammer.hint", "Give a steam hammer a recorded pattern, steam at the back and heat from below, and let it smith steel.");
        add(journal + "t4.steam_engine.hint", "Pipe a boiler's steam into the back of a steam engine. At 2 bar it turns a shaft at 32 RPM.");
        add(journal + "t4.crusher", "Crushing Power");
        add(journal + "t4.crusher.hint", "Drive a crusher at 16 RPM or more. Ore comes out crushed, sometimes with a second piece and a bit of another mineral.");
        add(journal + "t4.blast_furnace", "Blast Furnace");
        add(journal + "t4.blast_furnace.hint", "Build a five-high refractory furnace, blow air into its tuyere and charge it with iron ore, coke and flux. Tap the pig iron.");
        add(journal + "t4.converter", "Bessemer Steel");
        add(journal + "t4.converter.hint", "Blow air through pig iron in a converter, with a coke to preheat it. Watch the flame: when it drops, the steel is done.");
        add(journal + "t4.steel_anvil", "Steel Anvil");
        add(journal + "t4.steel_anvil.hint", "Weld steel into double ingots and build an anvil that can work anything.");
    }

    /** Spec 9: anvils, the smithing screen, and tongs. */
    private void smithing() {
        String id = StrataIndustria.MOD_ID;
        for (var entry : ModBlocks.STONE_ANVILS.entrySet()) addBlock(entry.getValue(), title(entry.getKey().id()) + " Anvil");
        addBlock(ModBlocks.BRONZE_ANVIL, "Bronze Anvil");
        addItem(ModItems.TONGS_JAW, "Tongs Jaw");
        addItem(ModItems.TONGS, "Tongs");
        add("item." + id + ".tongs.tooltip", "In the off hand: hold hot metal without burns");
        add("heat." + id + ".too_hot", "Too hot to hold bare-handed. Tongs in your off hand would help.");
        add("death.attack." + id + ".hot_item", "%1$s held on to hot metal for too long");
        add("death.attack." + id + ".hot_item.player", "%1$s held on to hot metal for too long while fighting %2$s");
        add("container." + id + ".anvil", "Anvil");
        String status = id + ".anvil.status.";
        add(status + "empty", "Put a heated workpiece in");
        add(status + "choose", "Pick what to make");
        add(status + "ready", "Hits so far: %s");
        add(status + "too_cold", "Too cold to work");
        add(status + "no_hammer", "Needs a hammer in your hotbar");
        add(status + "too_weak", "This anvil cannot work that metal");
        add(status + "output_full", "Take the finished piece out first");
        add(status + "not_enough", "Needs more of that metal");
        add(status + "no_plan", "Nothing can be smithed from that");
        add(id + ".anvil.out_of_range", "That would overwork it");
        add(id + ".anvil.rules", "Rules");
        add(id + ".anvil.recent", "Last hits");
        add(id + ".anvil.rule", "%s %s");
        for (var hit : dev.strataindustria.smithing.HitType.values()) {
            add(id + ".anvil.hit." + hit.id(), title(hit.id()) + " (%s)");
        }
        for (var kind : dev.strataindustria.smithing.Rule.Kind.values()) {
            add(id + ".anvil.kind." + kind.getSerializedName(), title(kind.getSerializedName()));
        }
        add(id + ".anvil.where.last", "last");
        add(id + ".anvil.where.second_last", "second last");
        add(id + ".anvil.where.third_last", "third last");
        add(id + ".anvil.where.not_last", "not last");
        add(id + ".anvil.where.any", "any of the last three");

        // Spec 7 and 8: mechanical power and machines.
        addBlock(ModBlocks.WOODEN_AXLE, "Wooden Axle");
        addBlock(ModBlocks.WOODEN_GEARBOX, "Wooden Gearbox");
        addBlock(ModBlocks.HAND_CRANK, "Hand Crank");
        addBlock(ModBlocks.WATER_WHEEL, "Water Wheel");
        addBlock(ModBlocks.MILLSTONE, "Millstone");
        addBlock(ModBlocks.BELLOWS, "Bellows");
        addItem(ModItems.WOODEN_GEAR, "Wooden Gear");
        add("container." + id + ".millstone", "Millstone");
        String kinetic = id + ".kinetic.";
        add(kinetic + "running", "%s RPM, %s / %s SU");
        add(kinetic + "idle", "Not turning");
        add(kinetic + "overstressed", "Overstressed (%s / %s SU)");
        add(kinetic + "incomplete", "Network incomplete");
        add(kinetic + "too_large", "Network too large");
        add(kinetic + "overspeed", "Overspeed: wooden parts cannot pass 64 RPM");
        String windmill = id + ".windmill.";
        add(windmill + "blocked", "Blocked: the sails need a clear 15 by 15 area");
        add(windmill + "few_sails", "Needs at least %s sails (has %s)");
        String belt = id + ".belt.";
        add(belt + "started", "Now use the belt on a second pulley");
        add(belt + "joined", "Pulleys belted together");
        add(belt + "taken", "That pulley already has a belt");
        add(belt + "not_parallel", "The pulleys must turn on parallel axles");
        add(belt + "not_in_line", "The pulleys must sit side by side, not on the same axle line");
        add(belt + "too_far", "Too far apart for one belt (8 blocks at most)");
        addBlock(ModBlocks.STEP_UP_GEARBOX, "Step-up Gearbox");
        addBlock(ModBlocks.PULLEY, "Pulley");
        addBlock(ModBlocks.WINDMILL_BEARING, "Windmill Bearing");
        addBlock(ModBlocks.WINDMILL_SAIL, "Windmill Sail");
        addItem(ModItems.LEATHER_BELT, "Leather Belt");
        addBlock(ModBlocks.SOAKING_BARREL, "Soaking Barrel");
        addItem(ModItems.RAW_HIDE, "Raw Hide");
        addItem(ModItems.LIMED_HIDE, "Limed Hide");
        addItem(ModItems.SCRAPED_HIDE, "Scraped Hide");
        add("fluid_type." + id + ".lye", "Lye");
        add("fluid_type." + id + ".tannin", "Tannin");
        add("container." + id + ".soaking_barrel", "Soaking Barrel");
        String barrel = id + ".soaking_barrel.";
        add(barrel + "empty", "Add water, then something to soak");
        add(barrel + "open", "Sneak and use to seal the lid");
        add(barrel + "no_recipe", "Nothing here soaks into anything");
        add(barrel + "needs_more", "Needs %s more to use up the tank");
        add(barrel + "needs_fluid", "Not enough fluid; an empty bucket tips it out");
        add(barrel + "output_full", "Take out what is done");
        add(barrel + "working", "Soaking: %s%%");
        add(barrel + "lid_on", "Take the lid off first (sneak and use)");
        add(barrel + "poured_out", "%s tipped out of the barrel");
        add(barrel + "tank", "%s: %s / %s mB");
        add(barrel + "tank_empty", "Empty");
        add(barrel + "fluid.none", "Empty");
        add(barrel + "fluid.water", "Water");
        add(barrel + "fluid.lye", "Lye");
        add(barrel + "fluid.tannin", "Tannin");
        add(barrel + "fluid.other", "Fluid");
        add(kinetic + "limited_by", "speed limited by the source at %s %s %s");
        add(id + ".water_wheel.no_flow", "No water flow");
        add(id + ".water_wheel.blocked", "Blocked");
        String machine = id + ".machine.";
        add(machine + "empty", "Waiting for input");
        add(machine + "working", "Working");
        add(machine + "not_turning", "Needs a turning shaft");
        add(machine + "too_slow", "Too slow: needs %s RPM");
        add(machine + "no_recipe", "Cannot work that");
        add(machine + "output_full", "Output full");
        add(machine + "no_blade", "Needs a saw blade");
        addBlock(ModBlocks.SAW_MILL, "Saw Mill");
        addBlock(ModBlocks.TRIP_HAMMER, "Trip Hammer");
        addItem(ModItems.BARK, "Bark");
        add("container." + id + ".saw_mill", "Saw Mill");
        add("container." + id + ".trip_hammer", "Trip Hammer");
        add("container." + id + ".core_sampler", "Core Sampler");
        add("container." + id + ".sluice", "Sluice");
        addBlock(ModBlocks.SLUICE, "Sluice");
        addItem(ModItems.WASHING_PAN, "Washing Pan");
        String sluice = id + ".sluice.";
        add(sluice + "empty", "Throw in crushed ore or placer gravel");
        add(sluice + "working", "Washing: %s%%");
        add(sluice + "no_water", "No water flow at the back");
        add(sluice + "no_recipe", "Washing out what it cannot use");
        String pan = id + ".washing_pan.";
        add(pan + "how", "Scoop up placer gravel or sand, or hold crushed ore in your other hand");
        add(pan + "cannot", "%s cannot be washed");
        add(pan + "no_water", "Stand in water to wash");
        add(pan + "holds", "Holds %s");
        addBlock(ModBlocks.CORE_SAMPLER, "Core Sampler");
        addItem(ModItems.CORE_SAMPLE, "Core Sample");
        String sampler = id + ".core_sampler.";
        add(sampler + "drill", "Drill");
        add(sampler + "idle", "Press Drill to take a core");
        add(sampler + "drilling", "Drilling");
        add(sampler + "not_turning", "Not turning");
        add(sampler + "too_slow", "Too slow: needs %s RPM");
        add(sampler + "output_full", "Take the last core out first");
        add(sampler + "done", "Core ready");
        String core = id + ".core_sample.";
        add(core + "cave", "Open cave");
        add(core + "at", "y %s");
        add(core + "range", "y %s\u2013%s");
        add(core + "layer", "%s: %s");
        add(core + "find", "%s: %s \u00d7%s");
        add(core + "graded", "%2$s %1$s");
        add(core + "layers", "Layers");
        add(core + "finds", "Ore and deposits");
        add(core + "barren", "No ore in this core");
        add(core + "where", "x %s, z %s");
        add(core + "origin", "Drilled at %s, %s, %s");
        add(core + "contains", "Shows %s");
        add(core + "scroll", "%s/%s, scroll");
        for (OreGrade grade : OreGrade.values()) add(id + ".grade." + grade.getSerializedName(), grade.getSerializedName());
        String hammer = id + ".trip_hammer.";
        add(hammer + "no_anvil", "No anvil in front");
        add(hammer + "no_pattern", "Needs a recorded pattern");
        add(hammer + "not_turning", "Needs a turning shaft");
        add(hammer + "too_slow", "Too slow: needs %s RPM");
        add(hammer + "waiting", "Waiting for a hot workpiece");
        add(hammer + "working", "Working (%s / %s hits)");
        add(hammer + "output_full", "Output full");
        add(hammer + "anvil_busy", "Something else is on the anvil");
        add(hammer + "outdated_pattern", "Outdated pattern: record it again");

        // Spec 9.4 and 9.5: welding, flux and patterns.
        addBlock(ModBlocks.WROUGHT_IRON_ANVIL, "Wrought Iron Anvil");
        addItem(ModItems.FLUX, "Flux");
        addItem(ModItems.SMITHING_PATTERN, "Smithing Pattern");
        add(id + ".anvil.weld", "Weld");
        add(id + ".anvil.pattern", "Pattern");
        String weld = id + ".anvil.weld.";
        add(weld + "ready", "Ready to weld");
        add(weld + "no_recipe", "Those two pieces do not weld");
        add(weld + "too_weak", "This anvil cannot weld that metal");
        add(weld + "output_full", "Take the finished piece out first");
        add(weld + "too_cold", "Both pieces need %s °C to weld");
        add(weld + "no_flux", "Sprinkle flux in the flux slot");
        add(weld + "no_hammer", "Needs a hammer in your hotbar");
        add(id + ".pattern.recorded", "%s, %s hits, %s");
        add(id + ".pattern.blank", "Put it in the anvil's pattern slot to record your next piece");
    }

    /** Spec 6 to 8: metal names, the crucible screen, molds, cast parts and tools. */
    private void metals() {
        String id = StrataIndustria.MOD_ID;
        for (Metal metal : Metal.values()) {
            String name = metal == Metal.SLAG_METAL ? "Slag" : title(metal.id());
            add(id + ".metal." + metal.id(), name);
            if (metal.isVanilla() || !metal.hasIngot()) {
                // Copper's ingot and nugget are the vanilla items.
            } else {
                addItem(ModItems.INGOTS.get(metal), name + " Ingot");
                if (metal.hasNugget()) addItem(ModItems.NUGGETS.get(metal), name + " Nugget");
            }
            if (metal.hasPlate()) addItem(ModItems.PLATES.get(metal), name + " Plate");
            if (ModItems.RODS.containsKey(metal)) addItem(ModItems.RODS.get(metal), name + " Rod");
            if (ModItems.GEARS.containsKey(metal)) addItem(ModItems.GEARS.get(metal), name + " Gear");
            if (!metal.isToolMetal()) continue;
            if (!metal.isVanilla()) {
                for (var piece : ModItems.ARMOUR.get(metal).entrySet()) {
                    add(piece.getValue().get(), name + " " + title(piece.getKey().getName()));
                }
            }
            if (ModItems.PROSPECTOR_HEADS.containsKey(metal)) {
                addItem(ModItems.PROSPECTOR_HEADS.get(metal), name + " Prospector's Pick Head");
                addItem(ModItems.PROSPECTORS_PICKS.get(metal), name + " Prospector's Pick");
            }
            for (MoldType type : metal.toolTypes()) {
                addItem(ModItems.HEADS.get(metal).get(type), name + " " + title(type.id()));
                var tool = ModItems.TOOLS.get(metal).get(type);
                if (tool instanceof net.neoforged.neoforge.registries.DeferredItem<?> item) {
                    add(item.get(), name + " " + title(type.tool()));
                }
            }
        }
        add("container." + id + ".crucible", "Crucible");
        String status = id + ".crucible.status.";
        add(status + "cold", "Cold");
        add(status + "heating", "Heating up");
        add(status + "melting", "Melting... %s%%");
        add(status + "molten", "%s, molten");
        add(status + "molten_unknown", "Molten, but no known alloy");
        add(status + "solid", "Solid, needs more heat");
        add(status + "no_forge", "Needs a lit forge below");
        add(id + ".crucible.pour", "Pour");
        // The alloy name (%1$s) is left out to fit the line; the status line names it once it forms.
        add(id + ".crucible.hint", "Needs %2$s-%3$s%% %4$s, has %5$s%%");
        String problem = id + ".crucible.problem.";
        add(problem + "no_mold", "Put an empty mold in the mold slot");
        add(problem + "pouring", "Already pouring");
        add(problem + "not_molten", "The metal is not molten");
        add(problem + "not_enough", "Not enough metal to fill that mold");
        add(problem + "no_alloy", "That mix will not make a usable metal");
        add(id + ".mold.contents", "Holds %s (%s units)");
        add(id + ".mold.still_molten", "The metal is still molten. Let it cool first");
        add(id + ".mold.broke", "The mold cracked apart");
        String prospect = id + ".prospect.";
        add(prospect + "nothing", "No ore nearby");
        String[][] sizes = {{"traces", "Traces of %s", "traces of %s"}, {"small", "Small %s", "small %s"},
                {"medium", "Medium %s", "medium %s"}, {"large", "Large %s", "large %s"}, {"very_large", "Very large %s", "very large %s"}};
        for (String[] size : sizes) {
            add(prospect + size[0], size[1]);
            add(prospect + size[0] + ".more", size[2]);
        }
        for (OreMineral mineral : OreMineral.values()) add(id + ".ore." + mineral.id(), mineral.id().replace('_', ' '));
        add(id + ".metal.slag_note", "Remelts to most of the metal that went in");
        for (String grade : new String[] {"crude", "rough", "standard", "fine", "masterwork"}) {
            add(id + ".quality." + grade, title(grade) + " quality");
        }
    }

    /** Recipe viewer pages (compat/jei); keys under {@code recipe_view.strataindustria.}. */
    private void recipeViewer() {
        String k = dev.strataindustria.compat.recipeview.RecipeText.KEY;
        add(k + "category.knapping", "Knapping");
        add(k + "category.clay_forming", "Clay Forming");
        add(k + "category.pit_kiln", "Pit Kiln");
        add(k + "category.charcoal_pit", "Charcoal Pit");
        add(k + "category.quern", "Grinding");
        add(k + "category.alloying", "Crucible Alloying");
        add(k + "category.casting", "Casting");
        add(k + "category.anvil", "Anvil Smithing");
        add(k + "category.welding", "Welding");
        add(k + "category.bloomery", "Bloomery");
        add(k + "category.sawing", "Saw Mill");
        add(k + "category.washing", "Ore Washing");
        add(k + "category.barrel", "Soaking Barrel");
        add(k + "category.roasting", "Roasting");
        add(k + "category.coking", "Coke Oven");
        add(k + "category.firebox_fuel", "Firebox Fuel");
        add(k + "heat.work", "Work at %s heat");
        add(k + "heat.weld", "Weld at %s heat");
        add(k + "heat.melt", "Melts at %s heat");
        add(k + "heat.roast", "Roast at %s heat");
        add(k + "heat.bloom", "Fire to %s heat");
        add(k + "heat.fuel", "Up to %s heat");
        add(k + "heat.tooltip", "Heat it until it glows this colour");
        add(k + "time.seconds", "%s s");
        add(k + "time.minutes", "%s min");
        add(k + "time.minutes_seconds", "%s min %s s");
        add(k + "kinetic.rated", "at %s RPM");
        add(k + "chance", "%s%% chance");
        add(k + "washing.sluice_only", "in a sluice");
        add(k + "knapping.mirror", "or mirrored");
        add(k + "knapping.grid", "Strike out the dark cells; what is left is the shape");
        add(k + "knapping.grid_mirror", "Its mirror image works too");
        add(k + "pit_kiln.fuel", "Under straw and logs");
        add(k + "charcoal_pit.cover", "Covered on every side");
        add(k + "alloying.share", "%s %s\u2013%s%%");
        add(k + "crucible.refractory", "Refractory crucible only");
        add(k + "casting.units", "%s units");
        add(k + "bloomery.per_level", "Per chimney level");
        add(k + "firebox.heat", "%s HU per tick");
    }
}
