package dev.strataindustria.datagen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.ceramics.MoldType;
import dev.strataindustria.geology.OreGrade;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.journal.JournalTrigger;
import dev.strataindustria.material.Metal;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.ModTags;
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
 * as a tree that branches where the work can happen in any order. Titles and hints are in the lang file.
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
        AdvancementHolder copperPick = goal(copperIngot, "t2/copper_pickaxe", Items.COPPER_PICKAXE, has(Items.COPPER_PICKAXE));
        List<ItemLike> alloyOres = new ArrayList<>();
        List<ItemLike> crushed = new ArrayList<>();
        for (OreMineral mineral : OreMineral.values()) {
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

        goal(smith, "t2/bronze_tools", ModItems.tool(Metal.BRONZE, MoldType.PICKAXE_HEAD), AdvancementType.GOAL,
                InventoryChangeTrigger.TriggerInstance.hasItems(bronzeTools()));
        goal(smith, "t2/bronze_armour", ModItems.ARMOUR.get(Metal.BRONZE).get(ModItems.armourTypes()[1]).get(), AdvancementType.GOAL,
                JournalTrigger.TriggerInstance.of(Journal.BRONZE_ARMOUR));
        goal(bronze, "t2/prospectors_pick", ModItems.PROSPECTORS_PICKS.get(Metal.BRONZE).get(),
                JournalTrigger.TriggerInstance.of(Journal.PROSPECT));
        AdvancementHolder bronzeAnvil = goal(smith, "t2/bronze_anvil", ModItems.BRONZE_ANVIL.get(), AdvancementType.GOAL,
                ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(blocks, ModBlocks.BRONZE_ANVIL.get()));

        // Tier 3 is the next milestone: a signpost with no way to complete it yet.
        goal(bronzeAnvil, "t3/iron", Items.RAW_IRON, AdvancementType.GOAL,
                CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance()));
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
                .display(icon.asItem(), title(key), hint(key), type, true, false, false)
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
