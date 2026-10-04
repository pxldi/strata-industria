package dev.strataindustria.datagen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.ceramics.MoldType;
import dev.strataindustria.geology.OreGrade;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.journal.JournalTrigger;
import dev.strataindustria.logistics.Tier5Logistics;
import dev.strataindustria.material.Metal;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.ModTags;
import dev.strataindustria.registry.Tier4Blocks;
import dev.strataindustria.registry.Tier4Items;
import dev.strataindustria.registry.Tier5Blocks;
import dev.strataindustria.registry.Tier5Items;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.ImpossibleTrigger;
import net.minecraft.advancements.triggers.InventoryChangeTrigger;
import net.minecraft.advancements.triggers.ItemUsedOnLocationTrigger;
import net.minecraft.advancements.triggers.PlayerTrigger;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

/**
 * The field journal tab (spec 11): one goal per step from the first loose rock to the bronze anvil, laid out
 * as a tree that branches where the work can happen in any order. Titles and hints are in the lang file. Goals show
 * no vanilla toast: the leads notebook announces them in its own words (journal leads spec).
 */
final class ModAdvancements extends AdvancementSubProvider {
    private final HolderGetter<Item> items;
    private final HolderGetter<Block> blocks;

    ModAdvancements(BootstrapContext<Advancement> output) {
        super(output);
        this.items = output.lookup(Registries.ITEM);
        this.blocks = output.lookup(Registries.BLOCK);
    }

    @Override
    public void generate() {
        AdvancementHolder root = Advancement.Builder.advancement()
                .rootDisplay(ModItems.FIELD_JOURNAL.get(), title("root"), hint("root"),
                        StrataIndustria.id("block/granite"), AdvancementType.TASK, false, false, false)
                .addCriterion("tick", PlayerTrigger.TriggerInstance.tick())
                .save(output, Journal.ROOT.toString());

        StructureData.places(output, root);

        // Tier 0: stone
        AdvancementHolder looseRock = goal(root, "t0/loose_rock", ModItems.LOOSE_ROCK.values().iterator().next().get(),
                InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().of(items, ModTags.Items.LOOSE_ROCKS)));
        AdvancementHolder knap = goal(looseRock, "t0/knap", ModItems.STONE_AXE_HEAD.get(), JournalTrigger.TriggerInstance.of(Journal.KNAP));
        AdvancementHolder stoneAxe = goal(knap, "t0/stone_axe", ModItems.STONE_AXE.get(), has(ModItems.STONE_AXE.get()));
        AdvancementHolder log = goal(stoneAxe, "t0/log", Items.OAK_LOG,
                InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().of(items, ItemTags.LOGS)));
        goal(log, "t0/crafting_table", Items.CRAFTING_TABLE, has(Items.CRAFTING_TABLE));
        AdvancementHolder twine = goal(knap, "t0/twine", ModItems.TWINE.get(), has(ModItems.TWINE.get()));
        AdvancementHolder fire = goal(twine, "t0/fire", ModItems.FIRE_PIT.get(), JournalTrigger.TriggerInstance.of(Journal.FIRE_PIT_LIT));
        AdvancementHolder clay = goal(fire, "t0/clay", Items.CLAY_BALL, InventoryChangeTrigger.TriggerInstance.hasItems(
                ItemPredicate.Builder.item().of(items, Items.CLAY_BALL).withCount(MinMaxBounds.Ints.atLeast(5))));

        // Tier 1: fire and clay
        AdvancementHolder forming = goal(clay, "t1/clay_forming", ModItems.UNFIRED_SMALL_VESSEL.get(),
                JournalTrigger.TriggerInstance.of(Journal.CLAY_FORMING));
        AdvancementHolder kiln = goal(forming, "t1/pit_kiln", ModItems.SMALL_VESSEL.get(),
                JournalTrigger.TriggerInstance.of(Journal.PIT_KILN_FIRED));
        AdvancementHolder charcoal = goal(fire, "t1/charcoal", Items.CHARCOAL, has(Items.CHARCOAL));
        AdvancementHolder forge = goal(charcoal, "t1/forge", ModItems.FORGE.get(),
                ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(blocks, ModBlocks.FORGE.get()));
        goal(looseRock, "t1/nugget", ModItems.SMALL_ORES.get(OreMineral.NATIVE_COPPER).get(),
                InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().of(items, ModTags.Items.SMALL_ORES)));
        AdvancementHolder crucible = goal(kiln, "t1/crucible", ModItems.CRUCIBLE.get(),
                InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.CRUCIBLE.get(), ModItems.INGOT_MOLD.get()));

        // Tier 2: copper and bronze
        AdvancementHolder melt = goal(crucible, "t2/melt", ModItems.INGOT_MOLD.get(),
                JournalTrigger.TriggerInstance.of(Journal.CRUCIBLE_MOLTEN));
        AdvancementHolder copperIngot = goal(melt, "t2/copper_ingot", Items.COPPER_INGOT, has(Items.COPPER_INGOT));
        goal(kiln, "t2/brick_kiln", dev.strataindustria.registry.PrologueRegistry.BRICK_KILN_ITEM.get(),
                JournalTrigger.TriggerInstance.of(Journal.BRICK_KILN_FIRED));
        goal(copperIngot, "t2/pattern_casting", dev.strataindustria.registry.PatternRegistry.PATTERNS.get("ingot").get(),
                JournalTrigger.TriggerInstance.of(Journal.PATTERN_PRESSED));
        goal(copperIngot, "t2/casting_table", dev.strataindustria.registry.PrologueRegistry.CASTING_TABLE_ITEM.get(),
                JournalTrigger.TriggerInstance.of(Journal.CASTING_TABLE_POURED));
        AdvancementHolder copperPick = goal(copperIngot, "t2/copper_pickaxe", Items.COPPER_PICKAXE, has(Items.COPPER_PICKAXE));
        List<ItemLike> alloyOres = new ArrayList<>();
        List<ItemLike> crushed = new ArrayList<>();
        for (OreMineral mineral : OreMineral.withPieces()) {
            for (OreGrade grade : OreGrade.values()) {
                if (mineral == OreMineral.CASSITERITE || mineral == OreMineral.BISMUTHINITE || mineral == OreMineral.TENNANTITE) {
                    alloyOres.add(ModItems.orePiece(mineral, grade));
                }
                crushed.add(ModItems.crushedOre(mineral, grade));
            }
        }
        AdvancementHolder alloyMetal = goal(copperPick, "t2/alloy_metal", ModItems.orePiece(OreMineral.CASSITERITE, OreGrade.values()[1]),
                anyOf(alloyOres));
        goal(alloyMetal, "t2/quern", ModItems.QUERN.get(), anyOf(crushed));
        AdvancementHolder bronze = goal(alloyMetal, "t2/bronze", ModItems.ingot(Metal.BRONZE),
                InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().of(items, ModTags.Items.ANY_BRONZE_INGOTS)));
        AdvancementHolder stoneAnvil = goal(bronze, "t2/stone_anvil", ModItems.STONE_HAMMER.get(),
                JournalTrigger.TriggerInstance.of(Journal.STONE_ANVIL));
        List<ItemLike> plates = new ArrayList<>();
        ModItems.PLATES.values().forEach(plate -> plates.add(plate.get()));
        AdvancementHolder smith = goal(stoneAnvil, "t2/smith", ModItems.PLATES.get(Metal.BRONZE).get(), anyOf(plates));

        goal(smith, "t2/bright_strike", ModItems.tool(Metal.BRONZE, MoldType.HAMMER_HEAD),
                JournalTrigger.TriggerInstance.of(Journal.BRIGHT_STRIKE));
        goal(smith, "t2/bronze_tools", ModItems.tool(Metal.BRONZE, MoldType.PICKAXE_HEAD), AdvancementType.GOAL,
                InventoryChangeTrigger.TriggerInstance.hasItems(bronzeTools()));
        goal(smith, "t2/bronze_armour", ModItems.ARMOUR.get(Metal.BRONZE).get(ModItems.armourTypes()[1]).get(), AdvancementType.GOAL,
                JournalTrigger.TriggerInstance.of(Journal.BRONZE_ARMOUR));
        // Outposts and transport spec 11: the T2 goals, all optional.
        goal(bronze, "t2/pack_frame", dev.strataindustria.transport.foot.FootRegistry.PACK_FRAME.get(),
                JournalTrigger.TriggerInstance.of(Journal.PACK_FRAME_WORN));
        goal(bronze, "t2/handcart", dev.strataindustria.transport.foot.FootRegistry.HANDCART.get(),
                JournalTrigger.TriggerInstance.of(Journal.HANDCART_HAUL));
        goal(bronze, "t2/trail", dev.strataindustria.transport.foot.FootRegistry.ROPE.get(),
                JournalTrigger.TriggerInstance.of(Journal.TRAIL_MARKED));
        goal(bronze, "t2/prospectors_pick", ModItems.PROSPECTORS_PICKS.get(Metal.BRONZE).get(),
                JournalTrigger.TriggerInstance.of(Journal.PROSPECT));
        AdvancementHolder bronzeAnvil = goal(smith, "t2/bronze_anvil", ModItems.BRONZE_ANVIL.get(), AdvancementType.GOAL,
                ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(blocks, ModBlocks.BRONZE_ANVIL.get()));

        // Tier 3 (spec 13): fire clay to bloomery to wrought iron.
        AdvancementHolder fireClay = goal(bronzeAnvil, "t3/fire_clay", ModItems.FIRE_CLAY_BALL.get(), has(ModItems.FIRE_CLAY_BALL.get()));
        AdvancementHolder fireBrick = goal(fireClay, "t3/fire_brick", ModItems.FIRE_BRICK.get(), has(ModItems.FIRE_BRICK.get()));
        AdvancementHolder ironOre = goal(fireBrick, "t3/iron_ore", ModItems.orePiece(OreMineral.HEMATITE, OreGrade.NORMAL),
                InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().of(items, ModTags.Items.IRON_ORES)));
        AdvancementHolder bloomery = goal(ironOre, "t3/bloomery", ModItems.BLOOMERY.get(), AdvancementType.GOAL,
                JournalTrigger.TriggerInstance.of(Journal.BLOOMERY_BUILT));
        AdvancementHolder bloom = goal(bloomery, "t3/bloom", ModItems.RAW_BLOOM.get(), has(ModItems.RAW_BLOOM.get()));
        AdvancementHolder refine = goal(bloom, "t3/refine", Items.IRON_INGOT, JournalTrigger.TriggerInstance.of(Journal.BLOOM_REFINED));
        goal(refine, "t3/iron_pickaxe", Items.IRON_PICKAXE, has(Items.IRON_PICKAXE));
        goal(refine, "t3/bucket", Items.BUCKET, has(Items.BUCKET));
        goal(fireBrick, "t3/furnace", Items.FURNACE, has(Items.FURNACE));
        AdvancementHolder rotation = goal(refine, "t3/rotation", ModItems.WOODEN_AXLE.get(), JournalTrigger.TriggerInstance.of(Journal.ROTATION));
        AdvancementHolder waterPower = goal(rotation, "t3/water_power", ModItems.WATER_WHEEL.get(), AdvancementType.GOAL,
                JournalTrigger.TriggerInstance.of(Journal.WATER_POWER));
        goal(waterPower, "t3/millstone", ModItems.MILLSTONE.get(), JournalTrigger.TriggerInstance.of(Journal.MILLSTONE));
        goal(waterPower, "t3/bellows", ModItems.BELLOWS.get(), JournalTrigger.TriggerInstance.of(Journal.BELLOWS));
        goal(waterPower, "t3/saw_mill", ModItems.SAW_MILL.get(), JournalTrigger.TriggerInstance.of(Journal.SAW_MILL));
        AdvancementHolder weld = goal(refine, "t3/weld", ModItems.WROUGHT_IRON_DOUBLE_INGOT.get(), has(ModItems.WROUGHT_IRON_DOUBLE_INGOT.get()));
        AdvancementHolder pattern = goal(weld, "t3/pattern", ModItems.SMITHING_PATTERN.get(),
                JournalTrigger.TriggerInstance.of(Journal.PATTERN_RECORDED));
        goal(pattern, "t3/trip_hammer", ModItems.TRIP_HAMMER.get(), AdvancementType.GOAL,
                JournalTrigger.TriggerInstance.of(Journal.TRIP_HAMMER));
        goal(refine, "t3/tramway", dev.strataindustria.transport.rail.RailRegistry.MINE_TUB.get(),
                JournalTrigger.TriggerInstance.of(Journal.TUB_ARRIVED));
        goal(refine, "t3/charter", dev.strataindustria.registry.TransportBlocks.OUTPOST_CHARTER_ITEM.get(), AdvancementType.GOAL,
                JournalTrigger.TriggerInstance.of(Journal.CHARTER_LINKED));
        goal(rotation, "t3/core_sample", ModItems.CORE_SAMPLE.get(), has(ModItems.CORE_SAMPLE.get()));
        goal(refine, "t3/wash", ModItems.WASHING_PAN.get(), JournalTrigger.TriggerInstance.of(Journal.WASH));
        AdvancementHolder hide = goal(waterPower, "t3/hide", ModItems.RAW_HIDE.get(), has(ModItems.RAW_HIDE.get()));
        goal(hide, "t3/leather", Items.LEATHER, AdvancementType.GOAL, JournalTrigger.TriggerInstance.of(Journal.LEATHER));
        AdvancementHolder ironAnvil = goal(weld, "t3/iron_anvil", ModItems.WROUGHT_IRON_ANVIL.get(), AdvancementType.GOAL,
                ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(blocks, ModBlocks.WROUGHT_IRON_ANVIL.get()));

        // Tier 4 spec 15.
        AdvancementHolder coal = goal(ironAnvil, "t4/coal", Items.COAL, has(Items.COAL));
        AdvancementHolder cokeOven = goal(coal, "t4/coke_oven", Tier4Items.COKE_OVEN_DOOR.get(),
                JournalTrigger.TriggerInstance.of(Journal.COKE_OVEN_BUILT));
        AdvancementHolder coke = goal(cokeOven, "t4/coke", Tier4Items.COKE.get(), has(Tier4Items.COKE.get()));
        AdvancementHolder refractory = goal(coke, "t4/refractory_crucible", Tier4Items.REFRACTORY_CRUCIBLE.get(),
                has(Tier4Items.REFRACTORY_CRUCIBLE.get()));
        AdvancementHolder moltenIron = goal(refractory, "t4/molten_iron", Items.IRON_INGOT,
                JournalTrigger.TriggerInstance.of(Journal.MOLTEN_IRON));
        AdvancementHolder steel = goal(moltenIron, "t4/steel", ModItems.ingot(Metal.STEEL), AdvancementType.GOAL, has(ModItems.ingot(Metal.STEEL)));
        goal(steel, "t4/steel_anvil", Tier4Items.STEEL_ANVIL.get(), AdvancementType.GOAL,
                ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(blocks, Tier4Blocks.STEEL_ANVIL.get()));
        goal(cokeOven, "t4/creosote", Tier4Items.TREATED_PLANKS.get(), has(Tier4Items.TREATED_PLANKS.get()));
        List<ItemLike> sphalerite = new ArrayList<>();
        sphalerite.add(ModItems.SMALL_ORES.get(OreMineral.SPHALERITE).get());
        for (OreGrade grade : OreGrade.values()) sphalerite.add(ModItems.orePiece(OreMineral.SPHALERITE, grade));
        AdvancementHolder zincOre = goal(ironAnvil, "t4/sphalerite", ModItems.orePiece(OreMineral.SPHALERITE, OreGrade.NORMAL), anyOf(sphalerite));
        List<ItemLike> calcines = new ArrayList<>();
        calcines.add(Tier4Items.SMALL_ZINC_CALCINE.get());
        for (OreGrade grade : OreGrade.values()) calcines.add(Tier4Items.zincCalcine(grade));
        AdvancementHolder roast = goal(zincOre, "t4/roast", Tier4Items.zincCalcine(OreGrade.NORMAL), anyOf(calcines));
        goal(roast, "t4/brass", ModItems.ingot(Metal.BRASS), has(ModItems.ingot(Metal.BRASS)));
        AdvancementHolder solder = goal(zincOre, "t4/solder", ModItems.ingot(Metal.SOLDER), has(ModItems.ingot(Metal.SOLDER)));
        // Spec 15, goals 62 and 63: lay any fluid pipe, then raise a boiler to 1 bar.
        AdvancementHolder pipe = Advancement.Builder.advancement()
                .parent(solder)
                .display(Tier4Items.COPPER_FLUID_PIPE.get(), title("t4.pipe"), hint("t4.pipe"), AdvancementType.TASK, false, false, false)
                .addCriterion("copper", ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(blocks, Tier4Blocks.COPPER_FLUID_PIPE.get()))
                .addCriterion("bronze", ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(blocks, Tier4Blocks.BRONZE_FLUID_PIPE.get()))
                .addCriterion("steel", ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(blocks, Tier4Blocks.STEEL_FLUID_PIPE.get()))
                .addCriterion("gauge", ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(blocks, Tier4Blocks.PRESSURE_GAUGE.get()))
                .requirements(net.minecraft.advancements.AdvancementRequirements.Strategy.OR)
                .save(output, Journal.goal("t4/pipe").toString());
        AdvancementHolder boiler = goal(pipe, "t4/boiler", Tier4Items.BRONZE_BOILER.get(), JournalTrigger.TriggerInstance.of(Journal.BOILER));
        // Spec 15, goal 68: heat reaches a consumer over 4 or more pipe blocks.
        goal(boiler, "t4/heat_network", Tier4Items.COPPER_HEAT_PIPE.get(), JournalTrigger.TriggerInstance.of(Journal.HEAT_NETWORK));
        AdvancementHolder engine = goal(boiler, "t4/steam_engine", Tier4Items.STEAM_ENGINE.get(), AdvancementType.GOAL,
                JournalTrigger.TriggerInstance.of(Journal.STEAM_ENGINE));
        // Spec 15, goal 65: a steam hammer finishes a recipe.
        goal(engine, "t4/steam_hammer", Tier4Items.STEAM_HAMMER.get(), JournalTrigger.TriggerInstance.of(Journal.STEAM_HAMMER));
        // Spec 15, goal 69: a powered crusher finishes something.
        goal(engine, "t4/crusher", Tier4Items.CRUSHER.get(), JournalTrigger.TriggerInstance.of(Journal.CRUSHER));
        // Spec 15, goal 66: a blast furnace taps pig iron.
        AdvancementHolder blastFurnace = goal(engine, "t4/blast_furnace", Tier4Items.BLAST_FURNACE_CONTROLLER.get(),
                JournalTrigger.TriggerInstance.of(Journal.BLAST_FURNACE));
        // Spec 15, goal 67: a converter finishes a blow.
        goal(blastFurnace, "t4/converter", Tier4Items.CONVERTER_CONTROLLER.get(), AdvancementType.GOAL,
                JournalTrigger.TriggerInstance.of(Journal.CONVERTER));
        // Spec 15, goal 74 and spec 13.6: a machine finishes a long run fed and emptied by automation. Closes tier 4.
        AdvancementHolder automated = goal(engine, "t4/automated_chain", Tier4Items.INSERTER.get(), AdvancementType.GOAL,
                JournalTrigger.TriggerInstance.of(Journal.AUTOMATED_CHAIN));
        tier5(automated);
    }

    /** Tier 5 spec 15: goals 75 to 100. Opens when tier 4 closes; exit goals are 93, 94, 96, 98 and 100. */
    private void tier5(AdvancementHolder tier4Exit) {
        // Goal 75: a bucket of latex, or a tap that pushed latex into a pipe.
        AdvancementHolder latex = Advancement.Builder.advancement()
                .parent(tier4Exit)
                .display(Tier5Items.LATEX_BUCKET.get(), title("t5.latex"), hint("t5.latex"), AdvancementType.TASK, false, false, false)
                .addCriterion("bucket", has(Tier5Items.LATEX_BUCKET.get()))
                .addCriterion("piped", JournalTrigger.TriggerInstance.of(Journal.LATEX_PIPED))
                .requirements(net.minecraft.advancements.AdvancementRequirements.Strategy.OR)
                .save(output, Journal.goal("t5/latex").toString());
        goal(latex, "t5/rubber", Tier5Items.RUBBER.get(), has(Tier5Items.RUBBER.get()));
        AdvancementHolder cinnabar = goal(tier4Exit, "t5/cinnabar", Items.REDSTONE,
                JournalTrigger.TriggerInstance.of(Journal.CINNABAR_MINED));
        AdvancementHolder redAlloy = goal(cinnabar, "t5/red_alloy", ModItems.ingot(Metal.RED_ALLOY), has(ModItems.ingot(Metal.RED_ALLOY)));
        AdvancementHolder wire = goal(tier4Exit, "t5/wire", Tier5Items.COPPER_WIRE.get(), has(Tier5Items.COPPER_WIRE.get()));
        goal(redAlloy, "t5/circuit", Tier5Items.BASIC_CIRCUIT.get(), has(Tier5Items.BASIC_CIRCUIT.get()));
        AdvancementHolder dynamo = goal(wire, "t5/dynamo", Tier5Items.KINETIC_DYNAMO.get(), JournalTrigger.TriggerInstance.of(Journal.DYNAMO));
        AdvancementHolder cable = goal(dynamo, "t5/cable", Tier5Items.LV_CABLE.get(),
                ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(blocks, Tier5Blocks.LV_CABLE.get()));
        AdvancementHolder machine = goal(cable, "t5/first_machine", Tier5Items.ELECTRIC_FURNACE.get(),
                JournalTrigger.TriggerInstance.of(Journal.FIRST_MACHINE));
        goal(dynamo, "t5/turbine", Tier5Items.STEAM_TURBINE.get(), JournalTrigger.TriggerInstance.of(Journal.TURBINE));
        goal(machine, "t5/macerator", Tier5Items.MACERATOR.get(), JournalTrigger.TriggerInstance.of(Journal.MACERATOR));
        AdvancementHolder assembler = goal(machine, "t5/assembler", Tier5Items.ASSEMBLER.get(), JournalTrigger.TriggerInstance.of(Journal.ASSEMBLER));
        goal(machine, "t5/power_hammer", Tier5Items.POWER_HAMMER.get(), JournalTrigger.TriggerInstance.of(Journal.POWER_HAMMER));
        AdvancementHolder battery = goal(cable, "t5/battery", Tier5Items.BATTERY_BOX.get(), JournalTrigger.TriggerInstance.of(Journal.BATTERY));
        goal(machine, "t5/electric_heat", Tier5Items.ELECTRIC_HEATER.get(), JournalTrigger.TriggerInstance.of(Journal.ELECTRIC_HEAT));
        AdvancementHolder acid = goal(assembler, "t5/sulfuric_acid", Tier5Items.SULFURIC_ACID_BUCKET.get(),
                JournalTrigger.TriggerInstance.of(Journal.SULFURIC_ACID));
        goal(acid, "t5/electrolysis", Tier5Items.ELECTROLYSER.get(), JournalTrigger.TriggerInstance.of(Journal.ELECTROLYSIS));
        AdvancementHolder alumina = goal(acid, "t5/alumina", Tier5Items.ALUMINA.get(), has(Tier5Items.ALUMINA.get()));
        goal(alumina, "t5/aluminium", ModItems.ingot(Metal.ALUMINIUM), AdvancementType.GOAL, has(ModItems.ingot(Metal.ALUMINIUM)));
        AdvancementHolder mv = goal(assembler, "t5/mv", Tier5Items.MV_UPGRADE_KIT.get(), AdvancementType.GOAL,
                JournalTrigger.TriggerInstance.of(Journal.MV_UPGRADE));
        AdvancementHolder transformer = goal(mv, "t5/transformer", Tier5Items.TRANSFORMER.get(), JournalTrigger.TriggerInstance.of(Journal.TRANSFORMER));
        goal(transformer, "t5/power_line", Tier5Items.POLE_INSULATOR.get(), AdvancementType.GOAL, JournalTrigger.TriggerInstance.of(Journal.POWER_LINE));
        AdvancementHolder pipe = goal(machine, "t5/item_pipe", Tier5Logistics.ITEM_PIPE_ITEM.get(), JournalTrigger.TriggerInstance.of(Journal.ITEM_PIPE));
        AdvancementHolder storage = goal(pipe, "t5/storage", Tier5Logistics.STORAGE_CONTROLLER_ITEM.get(), AdvancementType.GOAL,
                JournalTrigger.TriggerInstance.of(Journal.STORAGE));
        goal(machine, "t5/stethoscope", dev.strataindustria.grid.GridBlocks.STETHOSCOPE.get(), JournalTrigger.TriggerInstance.of(Journal.STETHOSCOPE));
        goal(battery, "t5/lightning", dev.strataindustria.grid.GridBlocks.LEYDEN_JAR_ITEM.get(), AdvancementType.GOAL,
                JournalTrigger.TriggerInstance.of(Journal.LIGHTNING_BANK));
        goal(battery, "t5/ore_scanner", Tier5Items.ORE_SCANNER.get(), JournalTrigger.TriggerInstance.of(Journal.ORE_SCAN));
        goal(storage, "t5/electric_chain", Tier5Items.ELECTROLYSER.get(), AdvancementType.GOAL, JournalTrigger.TriggerInstance.of(Journal.ELECTRIC_CHAIN));
    }

    /** Pickaxe, axe, shovel, knife, hammer, saw and sword, each from any bronze (spec 11, goal 23). */
    private ItemPredicate[] bronzeTools() {
        List<MoldType> kit = List.of(MoldType.PICKAXE_HEAD, MoldType.AXE_HEAD, MoldType.SHOVEL_HEAD, MoldType.KNIFE_BLADE,
                MoldType.HAMMER_HEAD, MoldType.SAW_BLADE, MoldType.SWORD_BLADE);
        ItemPredicate[] predicates = new ItemPredicate[kit.size()];
        for (int i = 0; i < kit.size(); i++) {
            List<ItemLike> tools = new ArrayList<>();
            for (Metal metal : Metal.values()) {
                if (metal.isBronze()) tools.add(ModItems.tool(metal, kit.get(i)));
            }
            predicates[i] = ItemPredicate.Builder.item().of(items, tools.toArray(ItemLike[]::new)).build();
        }
        return predicates;
    }

    private Criterion<InventoryChangeTrigger.TriggerInstance> has(ItemLike item) {
        return InventoryChangeTrigger.TriggerInstance.hasItems(item);
    }

    private Criterion<InventoryChangeTrigger.TriggerInstance> anyOf(List<ItemLike> options) {
        return InventoryChangeTrigger.TriggerInstance.hasItems(
                ItemPredicate.Builder.item().of(items, options.toArray(ItemLike[]::new)));
    }

    private AdvancementHolder goal(AdvancementHolder parent, String path, ItemLike icon, Criterion<?> criterion) {
        return goal(parent, path, icon, AdvancementType.TASK, criterion);
    }

    private AdvancementHolder goal(AdvancementHolder parent, String path, ItemLike icon, AdvancementType type, Criterion<?> criterion) {
        String key = path.replace('/', '.');
        return Advancement.Builder.advancement()
                .parent(parent)
                .display(icon.asItem(), title(key), hint(key), type, false, false, false)
                .addCriterion("done", criterion)
                .save(output, Journal.goal(path).toString());
    }

    private static Component title(String key) {
        return Component.translatable("journal." + StrataIndustria.MOD_ID + "." + key);
    }

    private static Component hint(String key) {
        return Component.translatable("journal." + StrataIndustria.MOD_ID + "." + key + ".hint");
    }
}
