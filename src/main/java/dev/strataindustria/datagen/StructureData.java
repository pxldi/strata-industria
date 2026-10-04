package dev.strataindustria.datagen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.ceramics.MoldType;
import dev.strataindustria.client.SurveyClient;
import dev.strataindustria.geology.OreGrade;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.journal.JournalTrigger;
import dev.strataindustria.material.Metal;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.structure.CampLoot;
import dev.strataindustria.structure.CampStructure;
import dev.strataindustria.structure.SpecimenShelfBlockEntity;
import dev.strataindustria.structure.StructureContent;
import dev.strataindustria.structure.Ledgers;
import dev.strataindustria.structure.StructureEvents;
import dev.strataindustria.survey.SurveyNotes;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.data.tags.BiomeTagsProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.DecoratedPotPattern;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.BuiltinStructureSets;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.placement.AbstractSpreadingStructurePlacement;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.UniformContainerBase;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.SetEnchantmentsFunction;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemDamageFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.floats.ContextFloatProviders;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Data for the world structures (structures spec 3, 6, 9 and 11): the structures and their sets, the biomes
 * they appear in, their loot, the "Places" pages of the journal, and the models and words of the new blocks.
 */
final class StructureData {
    private StructureData() {}

    // ---------------------------------------------------------------- structures and sets

    static TagKey<Biome> biomes(CampStructure.Layout layout) {
        return TagKey.create(Registries.BIOME, StrataIndustria.id("has_structure/" + layout.id()));
    }

    static void structures(BootstrapContext<Structure> context) {
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        for (CampStructure.Layout layout : CampStructure.Layout.values()) {
            // The pieces level and clear their own ground, after trees and ore veins are in place.
            context.register(layout.key(), new CampStructure(new Structure.StructureSettings.Builder(biomes.getOrThrow(biomes(layout)))
                    .generationStep(GenerationStep.Decoration.TOP_LAYER_MODIFICATION)
                    .terrainAdapation(TerrainAdjustment.NONE)
                    .build(), layout));
        }
    }

    static void structureSets(BootstrapContext<StructureSet> context) {
        HolderGetter<Structure> structures = context.lookup(Registries.STRUCTURE);
        HolderGetter<StructureSet> sets = context.lookup(Registries.STRUCTURE_SET);
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        var villages = Optional.of(new AbstractSpreadingStructurePlacement.ExclusionZone(sets.getOrThrow(BuiltinStructureSets.VILLAGES), 5));
        // Spacing and separation from structures spec section 7.
        Map<CampStructure.Layout, int[]> spread = Map.of(
                CampStructure.Layout.CHARCOAL_BURNERS_CLEARING, new int[] {26, 9, 503118271},
                CampStructure.Layout.PROSPECTOR_CAMP, new int[] {24, 8, 611407329},
                CampStructure.Layout.MINING_CAMP, new int[] {34, 12, 718204551},
                CampStructure.Layout.COLLAPSED_ADIT, new int[] {18, 6, 829366117},
                CampStructure.Layout.RUINED_BLOOMERY, new int[] {36, 12, 934521187},
                CampStructure.Layout.PLACER_WORKINGS, new int[] {10, 3, 145287613});
        spread.forEach((layout, s) -> context.register(set(layout.id()), new StructureSet(structures.getOrThrow(layout.key()),
                new RandomSpreadStructurePlacement(net.minecraft.core.Vec3i.ZERO, AbstractSpreadingStructurePlacement.FrequencyReductionMethod.DEFAULT,
                        1.0f, s[2], villages, s[0], s[1], RandomSpreadType.LINEAR))));

        // Near-origin guarantee (structures spec 3.3): one ring of tries around the world origin. A try fails
        // when its site is unsuitable and nothing retries it, so six tries make a near miss on every one unlikely.
        context.register(set("prospector_camp_near_origin"), new StructureSet(
                structures.getOrThrow(CampStructure.Layout.PROSPECTOR_CAMP.key()),
                new ConcentricRingsStructurePlacement(4, 6, 6, biomes.getOrThrow(biomes(CampStructure.Layout.PROSPECTOR_CAMP)))));
        context.register(set("mining_camp_near_origin"), new StructureSet(
                structures.getOrThrow(CampStructure.Layout.MINING_CAMP.key()),
                new ConcentricRingsStructurePlacement(10, 6, 6, biomes.getOrThrow(biomes(CampStructure.Layout.MINING_CAMP)))));
    }

    private static ResourceKey<StructureSet> set(String id) {
        return ResourceKey.create(Registries.STRUCTURE_SET, StrataIndustria.id(id));
    }

    /** {@code #strataindustria:has_structure/<id>}. */
    static final class BiomeTagProvider extends BiomeTagsProvider {
        BiomeTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
            super(output, lookupProvider, StrataIndustria.MOD_ID);
        }

        @Override
        protected void addTags(HolderLookup.Provider registries) {
            // Hot climates where laterite forms (tier 6 spec 19.4).
            tag(TagKey.create(Registries.BIOME, StrataIndustria.id("bauxite_hosts")))
                    .addTag(BiomeTags.IS_SAVANNA).addTag(BiomeTags.IS_JUNGLE).addTag(BiomeTags.IS_BADLANDS);
            tag(biomes(CampStructure.Layout.CHARCOAL_BURNERS_CLEARING))
                    .add(Biomes.FOREST, Biomes.FLOWER_FOREST, Biomes.BIRCH_FOREST, Biomes.OLD_GROWTH_BIRCH_FOREST,
                            Biomes.DARK_FOREST, Biomes.TAIGA, Biomes.OLD_GROWTH_PINE_TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA);

            List<ResourceKey<Biome>> land = List.of(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.SNOWY_PLAINS, Biomes.FOREST,
                    Biomes.FLOWER_FOREST, Biomes.BIRCH_FOREST, Biomes.OLD_GROWTH_BIRCH_FOREST, Biomes.DARK_FOREST, Biomes.TAIGA,
                    Biomes.SNOWY_TAIGA, Biomes.OLD_GROWTH_PINE_TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA, Biomes.SAVANNA,
                    Biomes.SAVANNA_PLATEAU, Biomes.DESERT, Biomes.SWAMP, Biomes.MEADOW, Biomes.CHERRY_GROVE, Biomes.GROVE,
                    Biomes.JUNGLE, Biomes.SPARSE_JUNGLE);
            var prospector = tag(biomes(CampStructure.Layout.PROSPECTOR_CAMP));
            land.forEach(prospector::add);

            var mining = tag(biomes(CampStructure.Layout.MINING_CAMP));
            land.forEach(mining::add);
            mining.add(Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_GRAVELLY_HILLS, Biomes.WINDSWEPT_FOREST, Biomes.WINDSWEPT_SAVANNA,
                    Biomes.BADLANDS, Biomes.ERODED_BADLANDS, Biomes.WOODED_BADLANDS);

            tag(biomes(CampStructure.Layout.COLLAPSED_ADIT))
                    .addTag(BiomeTags.IS_MOUNTAIN).addTag(BiomeTags.IS_HILL).addTag(BiomeTags.IS_BADLANDS).addTag(BiomeTags.IS_TAIGA)
                    .add(Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_GRAVELLY_HILLS, Biomes.WINDSWEPT_FOREST, Biomes.WINDSWEPT_SAVANNA,
                            Biomes.STONY_SHORE);

            tag(biomes(CampStructure.Layout.RUINED_BLOOMERY))
                    .addTag(BiomeTags.IS_FOREST).addTag(BiomeTags.IS_TAIGA).addTag(BiomeTags.IS_SAVANNA)
                    .add(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.SNOWY_PLAINS, Biomes.MEADOW, Biomes.SWAMP);

            // Rivers and the land along them; the structure itself checks for river water within reach.
            tag(biomes(CampStructure.Layout.PLACER_WORKINGS))
                    .addTag(BiomeTags.IS_RIVER).addTag(BiomeTags.IS_FOREST).addTag(BiomeTags.IS_TAIGA).addTag(BiomeTags.IS_SAVANNA)
                    .add(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.SNOWY_PLAINS, Biomes.MEADOW, Biomes.SWAMP);
        }
    }

    // ---------------------------------------------------------------- loot

    /** Survey notes targets (structures spec 6): the next metal along from the camp they were found in. */
    private static final SurveyNotes PROSPECTOR_NOTES = SurveyNotes.looking("cassiterite", "bismuthinite", "tennantite");
    private static final SurveyNotes MINING_NOTES = SurveyNotes.looking("hematite", "magnetite", "fire_clay", "cassiterite");
    private static final SurveyNotes ADIT_NOTES = SurveyNotes.looking("cassiterite", "hematite", "magnetite");
    private static final SurveyNotes BLOOMERY_NOTES = SurveyNotes.looking("fire_clay", "hematite", "magnetite");
    private static final SurveyNotes PLACER_NOTES = SurveyNotes.looking("native_gold");

    /** One pool with a chance to give {@code min} to {@code max} of an item. */
    private static LootPool.Builder pool(float chance, LootPoolEntry entry) {
        LootPool.Builder pool = LootPool.lootPool().setRolls(ContextIntProviders.exactly(1)).add(entry.builder());
        if (chance < 1) pool.when(LootItemRandomChanceCondition.randomChance(chance));
        return pool;
    }

    private record LootPoolEntry(UniformContainerBase.Builder<?> builder) {}

    private static LootPoolEntry item(ItemLike item, int min, int max) {
        StructureLoot.record(item, max);
        var entry = LootItem.lootTableItem(item);
        if (min != 1 || max != 1) entry.apply(SetItemCountFunction.setCount(min == max ? ContextIntProviders.exactly(min)
                : ContextIntProviders.between(min, max)));
        return new LootPoolEntry(entry);
    }

    /** A tool left behind with 25 to 80% of its durability (structures v2, L3). */
    private static LootPoolEntry worn(ItemLike item) {
        return worn(item, 0.25f, 0.8f);
    }

    /** A worn tool with its own share of durability left. */
    private static LootPoolEntry worn(ItemLike item, float min, float max) {
        StructureLoot.tool(item, min, max);
        return new LootPoolEntry(LootItem.lootTableItem(item).apply(SetItemDamageFunction.setDamage(ContextFloatProviders.between(min, max))));
    }

    private static LootPoolEntry notes(SurveyNotes notes) {
        return new LootPoolEntry(LootItem.lootTableItem(StructureContent.SURVEY_NOTES.get())
                .apply(SetComponentsFunction.setComponent(StructureContent.SURVEY.get(), notes)));
    }

    /** A handwritten page of a place's ledger (structures v2 4.2). */
    private static LootPoolEntry ledger(String place, int page) {
        return new LootPoolEntry(LootItem.lootTableItem(StructureContent.SURVEY_NOTES.get())
                .apply(SetComponentsFunction.setComponent(StructureContent.LEDGER.get(), Ledgers.key(place, page))));
    }

    /** A cut sample of {@code mineral}: a collectible that unlocks nothing. */
    private static LootPoolEntry specimen(OreMineral mineral) {
        return new LootPoolEntry(LootItem.lootTableItem(StructureContent.MINERAL_SPECIMEN.get())
                .apply(SetComponentsFunction.setComponent(StructureContent.MINERAL.get(), mineral.id())));
    }

    private static LootPoolEntry sherd(String place) {
        return new LootPoolEntry(LootItem.lootTableItem(StructureContent.SHERDS.get(place).get()));
    }

    /** Minerals of tier 3 and above are never given as items before their tier (structures spec 4.1, L6 and L8). */
    private static boolean early(OreMineral mineral) {
        return !mineral.needsBronzeTool();
    }

    private static Item small(OreMineral mineral) {
        return early(mineral) ? ModItems.SMALL_ORES.get(mineral).get() : Items.FLINT;
    }

    /** Vanilla treasure at vanilla rarity (L13): level I and II books without Mending, Silk Touch or Fortune. */
    private static LootPool.Builder book(LootTableSubProvider.Context context, float chance) {
        var enchantments = context.lookup(Registries.ENCHANTMENT);
        LootPool.Builder pool = LootPool.lootPool().setRolls(ContextIntProviders.exactly(1))
                .when(LootItemRandomChanceCondition.randomChance(chance));
        for (var key : List.of(Enchantments.UNBREAKING, Enchantments.PROTECTION, Enchantments.FEATHER_FALLING,
                Enchantments.SHARPNESS, Enchantments.EFFICIENCY)) {
            pool.add(LootItem.lootTableItem(Items.ENCHANTED_BOOK).setWeight(1).apply(new SetEnchantmentsFunction.Builder()
                    .withEnchantment(enchantments.getOrThrow(key), ContextIntProviders.between(1, 2))));
        }
        return pool;
    }

    /** Lure I or Luck of the Sea I, for the panners' cache. */
    private static LootPool.Builder fishingBook(LootTableSubProvider.Context context, float chance) {
        var enchantments = context.lookup(Registries.ENCHANTMENT);
        LootPool.Builder pool = LootPool.lootPool().setRolls(ContextIntProviders.exactly(1))
                .when(LootItemRandomChanceCondition.randomChance(chance));
        for (var key : List.of(Enchantments.LURE, Enchantments.LUCK_OF_THE_SEA)) {
            pool.add(LootItem.lootTableItem(Items.ENCHANTED_BOOK).setWeight(1).apply(new SetEnchantmentsFunction.Builder()
                    .withEnchantment(enchantments.getOrThrow(key), ContextIntProviders.exactly(1))));
        }
        return pool;
    }

    static final class ChestLoot implements LootTableSubProvider {
        private final LootTableSubProvider.Context context;

        ChestLoot(LootTableSubProvider.Context context) {
            this.context = context;
        }

        /** Builds a table under the audit; {@code build} must create every pool it hands out. */
        private void add(String path, java.util.function.Supplier<LootTable.Builder> build) {
            add(CampLoot.key(path), path, build);
        }

        private void add(ResourceKey<LootTable> key, String path, java.util.function.Supplier<LootTable.Builder> build) {
            StructureLoot.begin(path);
            LootTable.Builder table = build.get();
            StructureLoot.end();
            context.accept(key, table);
        }

        @Override
        public void run() {
            // Charcoal burners' clearing (H = 0): the hut barrel, and the cache a hidden spot holds.
            add(CampLoot.CLEARING_HUT, () -> LootTable.lootTable()
                    .withPool(pool(1, item(Items.STICK, 8, 24)))
                    .withPool(pool(0.9f, item(ModItems.STRAW.get(), 8, 20)))
                    .withPool(pool(0.7f, item(ModItems.TWINE.get(), 2, 6)))
                    .withPool(pool(0.4f, item(Items.APPLE, 2, 5)))
                    .withPool(pool(0.4f, item(Items.BREAD, 2, 5)))
                    .withPool(pool(0.5f, item(Items.SPRUCE_LOG, 16, 32)))
                    .withPool(pool(0.8f, item(Items.FLINT, 1, 3)))
                    .withPool(pool(0.5f, worn(ModItems.STONE_AXE.get())))
                    .withPool(pool(0.5f, worn(ModItems.STONE_SHOVEL.get())))
                    .withPool(pool(0.6f, item(ModItems.ASH.get(), 2, 8)))
                    .withPool(pool(0.6f, item(Items.CLAY_BALL, 1, 2)))
                    .withPool(pool(0.25f, ledger("charcoal_burners_clearing", 1))));
            add(CampLoot.CLEARING_CACHE, () -> LootTable.lootTable()
                    .withPool(pool(1, ledger("charcoal_burners_clearing", 2)))
                    .withPool(pool(1, sherd("charcoal_burners")))
                    .withPool(pool(1, item(Items.FLINT, 4, 8)))
                    .withPool(pool(0.8f, item(Items.TORCH, 4, 10)))
                    .withPool(pool(0.4f, item(Items.BREAD, 2, 5)))
                    .withPool(pool(0.6f, item(Items.EMERALD, 1, 3)))
                    .withPool(pool(0.15f, item(Items.NAME_TAG, 1, 1)))
                    .withPool(book(context, 0.2f)));

            // Mining camp tents and smithy (H = 2).
            add(CampLoot.MINING_TENT, () -> LootTable.lootTable()
                    .withPool(LootPool.lootPool().setRolls(ContextIntProviders.exactly(1))
                            .when(LootItemRandomChanceCondition.randomChance(0.8f))
                            .add(item(Items.BREAD, 2, 5).builder())
                            .add(item(Items.BAKED_POTATO, 2, 5).builder())
                            .add(item(Items.COOKED_MUTTON, 2, 5).builder()))
                    .withPool(pool(0.7f, item(Items.TORCH, 4, 10)))
                    .withPool(pool(0.6f, item(ModItems.TWINE.get(), 2, 6)))
                    .withPool(pool(0.6f, item(ModItems.FIBRE_CLOTH.get(), 2, 6)))
                    .withPool(pool(0.4f, item(Items.FLINT, 2, 6)))
                    .withPool(pool(0.3f, item(Items.PAPER, 1, 4))));
            add(CampLoot.MINING_SMITHY, () -> LootTable.lootTable()
                    .withPool(pool(1, item(Items.CHARCOAL, 16, 32)))
                    .withPool(pool(0.8f, item(ModItems.NUGGETS.get(Metal.COPPER).get(), 4, 10)))
                    .withPool(pool(0.6f, item(ModItems.NUGGETS.get(Metal.TIN).get(), 2, 5)))
                    .withPool(pool(0.5f, item(ModItems.INGOT_MOLD.get(), 1, 1)))
                    .withPool(pool(0.2f, item(ModItems.MOLDS.get(MoldType.PICKAXE_HEAD).get(), 1, 1)))
                    .withPool(pool(0.5f, worn(ModItems.STONE_HAMMER.get())))
                    .withPool(pool(0.6f, item(ModItems.STRAW.get(), 4, 12)))
                    .withPool(pool(0.6f, item(Items.STICK, 4, 12))));
            // The foreman's cache: the camp's best find.
            add(CampLoot.MINING_CACHE, () -> LootTable.lootTable()
                    .withPool(pool(1, ledger("mining_camp", 2)))
                    .withPool(pool(1, item(ModItems.INGOTS.get(Metal.COPPER).get(), 2, 3)))
                    .withPool(pool(0.7f, item(ModItems.INGOTS.get(Metal.TIN).get(), 1, 1)))
                    .withPool(pool(0.5f, item(Items.IRON_NUGGET, 1, 3)))
                    .withPool(pool(0.5f, new LootPoolEntry(LootItem.lootTableItem(StructureContent.PICK_AND_HAMMER_BANNER_PATTERN.get()))))
                    .withPool(pool(0.35f, new LootPoolEntry(LootItem.lootTableItem(StructureContent.MINER_TRIM_TEMPLATE.get()))))
                    .withPool(pool(0.6f, sherd("mining_camp")))
                    .withPool(pool(0.5f, specimen(OreMineral.CASSITERITE)))
                    .withPool(pool(0.6f, item(Items.EMERALD, 2, 5)))
                    .withPool(book(context, 0.25f)));

            // Ruined bloomery (H = 2): the workshop crate holds the three bricks of the stack and what the smith left;
            // the cache under the beams holds the best of it.
            add(CampLoot.BLOOMERY_WORKSHOP, () -> LootTable.lootTable()
                    .withPool(pool(1, item(StructureContent.CRACKED_FIRE_BRICKS_ITEM.get(), 3, 3)))
                    .withPool(pool(1, notes(BLOOMERY_NOTES)))
                    .withPool(pool(1, ledger("ruined_bloomery", 1)))
                    .withPool(pool(0.9f, item(Items.CHARCOAL, 8, 24)))
                    .withPool(pool(0.7f, item(ModItems.ASH.get(), 2, 8)))
                    .withPool(pool(0.7f, item(ModItems.INGOTS.get(Metal.COPPER).get(), 1, 2)))
                    .withPool(pool(0.6f, item(Items.IRON_NUGGET, 1, 3)))
                    .withPool(pool(0.6f, worn(ModItems.STONE_HAMMER.get()))));
            add(CampLoot.BLOOMERY_CACHE, () -> LootTable.lootTable()
                    .withPool(pool(1, ledger("ruined_bloomery", 3)))
                    .withPool(pool(1, ledger("ruined_bloomery", 2)))
                    .withPool(pool(0.35f, new LootPoolEntry(LootItem.lootTableItem(StructureContent.SMITH_TRIM_TEMPLATE.get()))))
                    .withPool(pool(0.6f, sherd("ruined_bloomery")))
                    .withPool(pool(1, specimen(OreMineral.HEMATITE)))
                    .withPool(pool(0.5f, item(Items.EMERALD, 1, 3)))
                    .withPool(book(context, 0.2f)));

            // Placer workings (H = 2): the hut barrel holds the family's food, the tin a few grains of gold (30 units
            // at most), the cache sunk in the bed the washer's pan and the way to native gold.
            add(CampLoot.PLACER_HUT, () -> LootTable.lootTable()
                    .withPool(LootPool.lootPool().setRolls(ContextIntProviders.exactly(1))
                            .add(item(Items.COOKED_SALMON, 2, 5).builder())
                            .add(item(Items.COOKED_COD, 2, 5).builder()))
                    .withPool(pool(0.4f, item(Items.BREAD, 2, 5)))
                    .withPool(pool(0.6f, item(Items.BOWL, 1, 3)))
                    .withPool(pool(0.6f, item(ModItems.TWINE.get(), 2, 8)))
                    .withPool(pool(0.6f, item(Items.FISHING_ROD, 1, 1)))
                    .withPool(pool(0.6f, item(Items.TORCH, 4, 10))));
            add(CampLoot.PLACER_TIN, () -> LootTable.lootTable()
                    .withPool(pool(1, item(Items.GOLD_NUGGET, 2, 3))));
            add(CampLoot.PLACER_CACHE, () -> LootTable.lootTable()
                    .withPool(pool(0.6f, worn(ModItems.WASHING_PAN.get())))
                    .withPool(pool(1, notes(PLACER_NOTES)))
                    .withPool(pool(1, ledger("placer_workings", 2)))
                    .withPool(pool(0.6f, sherd("placer_workings")))
                    .withPool(pool(0.5f, specimen(OreMineral.NATIVE_GOLD)))
                    .withPool(pool(0.4f, item(Items.EMERALD, 1, 3)))
                    .withPool(fishingBook(context, 0.3f)));

            for (OreMineral mineral : OreMineral.values()) {
                // Abandoned prospector's camp (H = 1): the pack and the cache.
                add(CampLoot.key(CampLoot.PROSPECTOR_PACK, mineral), CampLoot.PROSPECTOR_PACK + "/" + mineral.id(), () -> LootTable.lootTable()
                        .withPool(pool(1, notes(PROSPECTOR_NOTES)))
                        .withPool(pool(1, item(small(mineral), 6, 12)))
                        .withPool(pool(0.6f, item(Items.CLAY_BALL, 8, 24)))
                        .withPool(pool(0.6f, worn(ModItems.STONE_PICKAXE.get())))
                        .withPool(pool(0.4f, worn(ModItems.STONE_KNIFE.get())))
                        .withPool(pool(0.25f, item(ModItems.UNFIRED_CRUCIBLE.get(), 1, 1)))
                        .withPool(pool(0.7f, item(ModItems.TWINE.get(), 4, 10)))
                        .withPool(pool(0.7f, item(ModItems.PLANT_FIBRE.get(), 4, 10)))
                        .withPool(pool(0.4f, item(Items.BREAD, 2, 5)))
                        .withPool(pool(0.4f, item(Items.COOKED_COD, 2, 5)))
                        .withPool(pool(0.5f, item(Items.TORCH, 4, 10)))
                        .withPool(pool(0.25f, ledger("prospector_camp", 1))));
                add(CampLoot.key(CampLoot.PROSPECTOR_CACHE, mineral), CampLoot.PROSPECTOR_CACHE + "/" + mineral.id(), () -> {
                    LootTable.Builder cache = LootTable.lootTable()
                            .withPool(pool(1, ledger("prospector_camp", 2)))
                            .withPool(pool(0.8f, item(ModItems.NUGGETS.get(Metal.COPPER).get(), 5, 12)))
                            .withPool(pool(0.5f, item(ModItems.NUGGETS.get(Metal.TIN).get(), 2, 6)))
                            .withPool(pool(0.15f, item(ModItems.NUGGETS.get(Metal.BRONZE).get(), 1, 3)))
                            .withPool(pool(0.6f, sherd("prospector")))
                            .withPool(pool(0.6f, specimen(mineral)))
                            .withPool(pool(0.5f, item(Items.EMERALD, 1, 3)))
                            .withPool(book(context, 0.2f));
                    if (early(mineral)) cache.withPool(pool(0.8f, item(ModItems.orePiece(mineral, OreGrade.POOR), 6, 12)));
                    return cache;
                });

                // Mining camp ore cart (H = 2): the ore the crew sorted, and notes on the iron they could not cut.
                add(CampLoot.key(CampLoot.MINING_ORE_CART, mineral), CampLoot.MINING_ORE_CART + "/" + mineral.id(), () -> {
                    LootTable.Builder cart = LootTable.lootTable().withPool(pool(1, notes(MINING_NOTES)));
                    if (early(mineral)) {
                        cart.withPool(pool(1, item(ModItems.orePiece(mineral, OreGrade.POOR), 6, 12)))
                                .withPool(pool(0.7f, item(ModItems.orePiece(mineral, OreGrade.NORMAL), 3, 6)))
                                .withPool(pool(0.3f, item(ModItems.crushedOre(mineral, OreGrade.NORMAL), 2, 4)));
                    }
                    if (mineral != OreMineral.CASSITERITE) {
                        cart.withPool(pool(0.3f, item(ModItems.orePiece(OreMineral.CASSITERITE, OreGrade.NORMAL), 1, 2)));
                    }
                    return cart.withPool(pool(0.25f, ledger("mining_camp", 1)));
                });

                // Collapsed adit cache (H = 2).
                add(CampLoot.key(CampLoot.ADIT_CACHE, mineral), CampLoot.ADIT_CACHE + "/" + mineral.id(), () -> {
                    LootTable.Builder cache = LootTable.lootTable().withPool(pool(0.8f, item(Items.TORCH, 4, 10)));
                    if (early(mineral)) cache.withPool(pool(0.7f, item(ModItems.orePiece(mineral, OreGrade.POOR), 6, 12)));
                    return cache.withPool(pool(0.5f, item(ModItems.NUGGETS.get(Metal.COPPER).get(), 4, 12)))
                            .withPool(pool(0.5f, worn(ModItems.STONE_PICKAXE.get())))
                            .withPool(pool(0.25f, notes(ADIT_NOTES)))
                            .withPool(pool(1, ledger("collapsed_adit", 2)))
                            .withPool(pool(0.4f, item(Items.BREAD, 2, 5)));
                });

                // Hidden behind two cracked blocks in the miner's chamber.
                add(CampLoot.key(CampLoot.ADIT_HIDDEN, mineral), CampLoot.ADIT_HIDDEN + "/" + mineral.id(), () -> LootTable.lootTable()
                        .withPool(pool(1, sherd("collapsed_adit")))
                        .withPool(pool(1, specimen(mineral)))
                        .withPool(pool(0.7f, item(Items.EMERALD, 1, 3)))
                        .withPool(book(context, 0.25f)));
            }
        }
    }

    static final class ArchaeologyLoot implements LootTableSubProvider {
        private final LootTableSubProvider.Context context;

        ArchaeologyLoot(LootTableSubProvider.Context context) {
            this.context = context;
        }

        private static LootTable.Builder weighted(Object... entries) {
            LootPool.Builder pool = LootPool.lootPool().setRolls(ContextIntProviders.exactly(1));
            for (int i = 0; i < entries.length; i += 2) {
                UniformContainerBase.Builder<?> entry;
                if (entries[i] instanceof LootPoolEntry e) {
                    entry = e.builder();
                } else {
                    StructureLoot.record((ItemLike) entries[i], 1);
                    entry = LootItem.lootTableItem((ItemLike) entries[i]);
                }
                pool.add(entry.setWeight((Integer) entries[i + 1]));
            }
            return LootTable.lootTable().withPool(pool);
        }

        private void add(ResourceKey<LootTable> key, String path, java.util.function.Supplier<LootTable.Builder> build) {
            StructureLoot.begin(path);
            LootTable.Builder table = build.get();
            StructureLoot.end();
            context.accept(key, table);
        }

        @Override
        public void run() {
            for (OreMineral mineral : OreMineral.values()) {
                Item small = small(mineral);
                Item poor = early(mineral) ? ModItems.orePiece(mineral, OreGrade.POOR) : Items.FLINT;
                Item copper = ModItems.NUGGETS.get(Metal.COPPER).get();
                add(CampLoot.key(CampLoot.DIG_PROSPECTOR, mineral), CampLoot.DIG_PROSPECTOR + "/" + mineral.id(),
                        () -> weighted(small, 50, Items.FLINT, 30, Items.STICK, 20, sherd("prospector"), 4));
                add(CampLoot.key(CampLoot.DIG_SPOIL, mineral), CampLoot.DIG_SPOIL + "/" + mineral.id(),
                        () -> weighted(poor, 30, small, 20, Items.FLINT, 15, copper, 15, Items.BONE, 10, sherd("mining_camp"), 5,
                                ledger("mining_camp", 1), 3, notes(MINING_NOTES), 5));
                add(CampLoot.key(CampLoot.DIG_ADIT, mineral), CampLoot.DIG_ADIT + "/" + mineral.id(),
                        () -> weighted(Items.FLINT, 30, small, 30, Items.BONE, 20, sherd("collapsed_adit"), 5, copper, 15));
            }
        }
    }

    static final class BlockLoot extends BlockLootSubProvider {
        BlockLoot(LootTableSubProvider.Context context) {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags(), context);
        }

        @Override
        protected void generate() {
            dropSelf(StructureContent.FIBRE_CANVAS.get());
            dropSelf(StructureContent.FIBRE_CANVAS_CARPET.get());
            dropSelf(StructureContent.FIBRE_CANVAS_STAIRS.get());
            dropSelf(StructureContent.PIT_PROP.get());
            dropSelf(StructureContent.SPECIMEN_SHELF.get());
            // A cracked brick mostly crumbles to nothing; sometimes a piece is worth grinding into grog.
            add(StructureContent.CRACKED_FIRE_BRICKS.get(), block -> createSilkTouchDispatchTable(block, applyExplosionCondition(block,
                    LootItem.lootTableItem(ModItems.GROG.get()).when(LootItemRandomChanceCondition.randomChance(0.3f)))));
            dropOther(StructureContent.SLAG_HEAP.get(), ModItems.BLOOMERY_SLAG.get());
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            var shared = new java.util.HashSet<>(SharedBlockData.blocks());
            return StructureContent.BLOCKS.getEntries().stream().map(DeferredHolder::value).map(Block.class::cast)
                    .filter(block -> !shared.contains(block))::iterator;
        }
    }

    // ---------------------------------------------------------------- journal

    /** "Places" pages: hidden until visited, with no toast; {@link StructureEvents} plays a page turn instead. */
    static void places(BootstrapContext<Advancement> output, AdvancementHolder root) {
        Map<CampStructure.Layout, ItemLike> icons = Map.of(
                CampStructure.Layout.CHARCOAL_BURNERS_CLEARING, Items.OAK_LOG,
                CampStructure.Layout.PROSPECTOR_CAMP, ModItems.STONE_PICKAXE.get(),
                CampStructure.Layout.MINING_CAMP, StructureContent.PIT_PROP_ITEM.get(),
                CampStructure.Layout.COLLAPSED_ADIT, Items.GRAVEL,
                CampStructure.Layout.RUINED_BLOOMERY, ModItems.BLOOMERY_SLAG.get(),
                CampStructure.Layout.PLACER_WORKINGS, ModItems.PLACER_GRAVEL.get());
        for (CampStructure.Layout layout : CampStructure.Layout.values()) {
            String key = "journal." + StrataIndustria.MOD_ID + ".place." + layout.id();
            Advancement.Builder.advancement()
                    .parent(root)
                    .display(icons.get(layout).asItem(), Component.translatable(key), Component.translatable(key + ".hint"),
                            AdvancementType.TASK, false, false, true)
                    .addCriterion("visited", JournalTrigger.TriggerInstance.of(StructureEvents.PLACE + layout.id()))
                    .save(output, Journal.goal(StructureEvents.PLACE + layout.id()).toString());
        }
        // A shelf of four different mineral specimens is a page too: a collection, no progression.
        String collection = "journal." + StrataIndustria.MOD_ID + ".place.specimens";
        Advancement.Builder.advancement()
                .parent(root)
                .display(StructureContent.MINERAL_SPECIMEN.get(), Component.translatable(collection),
                        Component.translatable(collection + ".hint"), AdvancementType.TASK, false, false, true)
                .addCriterion("collected", JournalTrigger.TriggerInstance.of(SpecimenShelfBlockEntity.COLLECTION))
                .save(output, Journal.goal(SpecimenShelfBlockEntity.COLLECTION).toString());
        // A full shelf in the specimen cabinet is a page as well.
        String cabinet = "journal." + StrataIndustria.MOD_ID + ".place.cabinet";
        Advancement.Builder.advancement()
                .parent(root)
                .display(dev.strataindustria.cabinet.CabinetRegistry.SPECIMEN_CABINET_ITEM.get(), Component.translatable(cabinet),
                        Component.translatable(cabinet + ".hint"), AdvancementType.TASK, false, false, true)
                .addCriterion("filled", JournalTrigger.TriggerInstance.of(dev.strataindustria.cabinet.SpecimenCabinetBlock.JOURNAL))
                .save(output, Journal.goal(dev.strataindustria.cabinet.SpecimenCabinetBlock.JOURNAL).toString());
    }

    // ---------------------------------------------------------------- assets

    static void models(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        net.minecraft.data.BlockFamily canvasFamily = new net.minecraft.data.BlockFamily.Builder(StructureContent.FIBRE_CANVAS.get())
                .stairs(StructureContent.FIBRE_CANVAS_STAIRS.get()).getFamily();
        blockModels.family(StructureContent.FIBRE_CANVAS.get()).generateFor(canvasFamily)
                .carpet(StructureContent.FIBRE_CANVAS_CARPET.get());

        // A round post along its axis; the model is hand-built in resources.
        var prop = StrataIndustria.id("block/pit_prop");
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(StructureContent.PIT_PROP.get(),
                BlockModelGenerators.plainVariant(prop)).with(BlockModelGenerators.createRotatedPillar()));
        itemModels.itemModelOutput.accept(StructureContent.PIT_PROP_ITEM.get(), ItemModelUtils.plainModel(prop));

        // Cracked bricks, one in four sooted, picked per position.
        Block cracked = StructureContent.CRACKED_FIRE_BRICKS.get();
        var plain = ModelTemplates.CUBE_ALL.create(cracked, TextureMapping.cube(cracked), blockModels.modelOutput);
        var sooted = ModelTemplates.CUBE_ALL.createWithSuffix(cracked, "_sooted", TextureMapping.cube(TextureMapping.getBlockTexture(cracked,
                "_sooted")), blockModels.modelOutput);
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(cracked, new MultiVariant(WeightedList.of(List.of(
                new Weighted<>(BlockModelGenerators.plainModel(plain), 3),
                new Weighted<>(BlockModelGenerators.plainModel(sooted), 1))))));
        itemModels.itemModelOutput.accept(StructureContent.CRACKED_FIRE_BRICKS_ITEM.get(), ItemModelUtils.plainModel(plain));

        // Lumps of slag on the ground; the model is hand-built in resources.
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(StructureContent.SLAG_HEAP.get(),
                BlockModelGenerators.createRotatedVariants(BlockModelGenerators.plainModel(StrataIndustria.id("block/slag_heap")))));

        // A folded sheet with a smudge of the mineral's colour.
        Item notes = StructureContent.SURVEY_NOTES.get();
        var model = ModelTemplates.TWO_LAYERED_ITEM.create(notes, TextureMapping.layered(TextureMapping.getItemTexture(notes),
                TextureMapping.getItemTexture(notes, "_overlay")), itemModels.modelOutput);
        itemModels.itemModelOutput.accept(notes, ItemModelUtils.tintedModel(model, ItemModelUtils.constantTint(-1),
                new SurveyClient.MineralTint()));

        // Collectibles (structures v2 4.1). A specimen is a grey chip with a facet layer tinted per mineral.
        Item specimen = StructureContent.MINERAL_SPECIMEN.get();
        var specimenModel = ModelTemplates.TWO_LAYERED_ITEM.create(specimen, TextureMapping.layered(
                TextureMapping.getItemTexture(specimen), TextureMapping.getItemTexture(specimen, "_overlay")), itemModels.modelOutput);
        itemModels.itemModelOutput.accept(specimen, ItemModelUtils.tintedModel(specimenModel, ItemModelUtils.constantTint(-1),
                new SurveyClient.SpecimenTint()));
        StructureContent.SHERDS.values().forEach(sherd -> itemModels.generateFlatItem(sherd.get(), ModelTemplates.FLAT_ITEM));
        itemModels.generateFlatItem(StructureContent.MINER_TRIM_TEMPLATE.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(StructureContent.SMITH_TRIM_TEMPLATE.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(StructureContent.PICK_AND_HAMMER_BANNER_PATTERN.get(), ModelTemplates.FLAT_ITEM);

        // A wall shelf: the model is hand-built in resources and turned to face the player.
        var shelf = StrataIndustria.id("block/specimen_shelf");
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(StructureContent.SPECIMEN_SHELF.get(),
                BlockModelGenerators.plainVariant(shelf)).with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING));
        itemModels.itemModelOutput.accept(StructureContent.SPECIMEN_SHELF_ITEM.get(), ItemModelUtils.plainModel(shelf));
        SharedBlockData.models(blockModels, itemModels);
    }

    // ---------------------------------------------------------------- collectible registries

    static void potPatterns(BootstrapContext<DecoratedPotPattern> context) {
        for (String place : StructureContent.SHERD_PLACES) {
            context.register(StructureContent.potPattern(place), new DecoratedPotPattern(StrataIndustria.id(place + "_pottery_pattern")));
        }
    }

    static void trimPatterns(BootstrapContext<TrimPattern> context) {
        for (var key : List.of(StructureContent.MINER_TRIM, StructureContent.SMITH_TRIM)) {
            context.register(key, new TrimPattern(key.identifier(), Component.translatable("trim_pattern." + StrataIndustria.MOD_ID + "."
                    + key.identifier().getPath()), false));
        }
    }

    static void bannerPatterns(BootstrapContext<BannerPattern> context) {
        context.register(StructureContent.PICK_AND_HAMMER, new BannerPattern(StrataIndustria.id("pick_and_hammer"),
                "block." + StrataIndustria.MOD_ID + ".banner.pick_and_hammer"));
    }

    /** The banner pattern the pattern item grants. */
    static final class BannerPatternTagProvider extends net.minecraft.data.tags.TagsProvider<BannerPattern> {
        BannerPatternTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
            super(output, Registries.BANNER_PATTERN, lookupProvider, StrataIndustria.MOD_ID);
        }

        @Override
        protected void addTags(HolderLookup.Provider registries) {
            tag(StructureContent.PICK_AND_HAMMER_TAG).add(StructureContent.PICK_AND_HAMMER);
        }
    }

    /** Duplicating the two armour trim templates, and the trims themselves (vanilla smithing rules). */
    static final class CollectibleRecipes extends net.minecraft.data.recipes.RecipeProvider {
        CollectibleRecipes(BootstrapContext<net.minecraft.world.item.crafting.Recipe<?>> recipes, BootstrapContext<Advancement> advancements) {
            super(recipes, advancements);
        }

        @Override
        protected void buildRecipes() {
            copySmithingTemplate(StructureContent.MINER_TRIM_TEMPLATE.get(), Items.COBBLESTONE);
            copySmithingTemplate(StructureContent.SMITH_TRIM_TEMPLATE.get(), ModItems.BLOOMERY_SLAG.get());
            trimSmithing(StructureContent.MINER_TRIM_TEMPLATE.get(), StructureContent.MINER_TRIM,
                    ResourceKey.create(Registries.RECIPE, StrataIndustria.id("miner_armor_trim_smithing_template_smithing_trim")));
            trimSmithing(StructureContent.SMITH_TRIM_TEMPLATE.get(), StructureContent.SMITH_TRIM,
                    ResourceKey.create(Registries.RECIPE, StrataIndustria.id("smith_armor_trim_smithing_template_smithing_trim")));
        }
    }

    static void lang(BiConsumer<String, String> add) {
        String id = StrataIndustria.MOD_ID;
        SharedBlockData.lang(add);
        add.accept("block." + id + ".fibre_canvas", "Fibre Canvas");
        add.accept("block." + id + ".fibre_canvas_carpet", "Fibre Canvas Carpet");
        add.accept("block." + id + ".fibre_canvas_stairs", "Fibre Canvas Stairs");
        add.accept("block." + id + ".pit_prop", "Pit Prop");
        add.accept("block." + id + ".cracked_fire_bricks", "Cracked Fire Bricks");
        add.accept("block." + id + ".slag_heap", "Slag Heap");

        String notes = "item." + id + ".survey_notes";
        add.accept(notes, "Survey Notes");
        add.accept(notes + ".ledger", "Ledger Page");
        add.accept(notes + ".found", "Found");
        add.accept(notes + ".found_message", "Survey notes: deposit found");
        add.accept(notes + ".blank", "Blank. The ink has run.");
        add.accept(notes + ".unread", "Not read yet. Hold them a moment.");
        add.accept(notes + ".bearing", "%s, %s");
        add.accept(notes + ".distance", "~%s");
        add.accept(notes + ".distance.here", "here");
        String[][] compass = {{"north", "N"}, {"north_east", "NE"}, {"east", "E"}, {"south_east", "SE"},
                {"south", "S"}, {"south_west", "SW"}, {"west", "W"}, {"north_west", "NW"}};
        for (String[] dir : compass) add.accept(notes + ".dir." + dir[0], dir[1]);
        add.accept(notes + ".depth.surface", "at surface");
        add.accept(notes + ".depth.just_under", "shallow");
        add.accept(notes + ".depth.little_down", "medium depth");
        add.accept(notes + ".depth.deep", "deep");
        add.accept(notes + ".depth.very_deep", "very deep");
        add.accept(notes + ".where", "In %s, %s");
        for (OreMineral mineral : OreMineral.values()) {
            String[] hands = hands(mineral);
            for (int i = 0; i < hands.length; i++) add.accept(notes + ".hand." + mineral.id() + "." + i, hands[i]);
        }
        for (var page : Ledgers.PAGES.entrySet()) {
            String[] texts = ledgerTexts(page.getKey());
            for (int i = 0; i < texts.length; i++) add.accept("item." + id + ".ledger." + Ledgers.key(page.getKey(), i + 1), texts[i]);
        }

        // Collectibles (structures v2 4.1).
        add.accept("block." + id + ".specimen_shelf", "Specimen Shelf");
        String specimen = "item." + id + ".mineral_specimen";
        add.accept(specimen, "Mineral Specimen");
        add.accept(specimen + ".of", "%s Specimen");
        add.accept(specimen + ".forms_in", "Forms in %s");
        add.accept(specimen + ".depth", "Between y %s and y %s");
        String[] sherdNames = {"Charcoal Burners'", "Prospector's", "Mining Camp", "Collapsed Adit", "Ruined Bloomery", "Placer Workings"};
        for (int i = 0; i < sherdNames.length; i++) {
            add.accept("item." + id + "." + StructureContent.SHERD_PLACES.get(i) + "_pottery_sherd", sherdNames[i] + " Pottery Sherd");
        }
        add.accept("item." + id + ".miner_armor_trim_smithing_template", "Smithing Template");
        add.accept("item." + id + ".smith_armor_trim_smithing_template", "Smithing Template");
        add.accept("trim_pattern." + id + ".miner", "Miner Armor Trim");
        add.accept("trim_pattern." + id + ".smith", "Smith Armor Trim");
        add.accept("item." + id + ".pick_and_hammer_banner_pattern", "Banner Pattern");
        add.accept("item." + id + ".pick_and_hammer_banner_pattern.desc", "Pick and Hammer");
        String[] colours = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan",
                "purple", "blue", "brown", "green", "red", "black"};
        for (String colour : colours) {
            String name = java.util.Arrays.stream(colour.split("_")).map(w -> Character.toUpperCase(w.charAt(0)) + w.substring(1))
                    .collect(java.util.stream.Collectors.joining(" "));
            add.accept("block." + id + ".banner.pick_and_hammer." + colour, name + " Pick and Hammer");
        }
        add.accept("journal." + id + ".place.cabinet", "Full shelf");
        add.accept("journal." + id + ".place.cabinet.hint", "Every rock of one kind, or every mineral, in a specimen cabinet. "
                + "My pick reaches further into rock I know.");
        add.accept("journal." + id + ".place.specimens", "Specimen collection");
        add.accept("journal." + id + ".place.specimens.hint", "Four different minerals on one specimen shelf. Cut samples turn up "
                + "in old camps, adits and caches.");

        String subtitles = "subtitles." + id + ".";
        add.accept(subtitles + "pit_prop.creak", "Timber creaks");
        add.accept(subtitles + "survey_notes.open", "Notes unfold");
        add.accept(subtitles + "survey_notes.found", "Deposit found");
        add.accept(subtitles + "journal.place", "Journal page written");
        add.accept(subtitles + "cracked_fire_bricks.break", "Brick crumbles");
        add.accept(subtitles + "cracked_fire_bricks.settle", "Bricks tick");
        add.accept(subtitles + "slag_heap.break", "Slag crunches");
        add.accept(subtitles + "sluice_box.water", "Water trickles");

        String place = "journal." + id + ".place";
        add.accept(place + ".noted", "Field journal: %s");
        add.accept(place + ".charcoal_burners_clearing", "Charcoal burners' clearing");
        add.accept(place + ".charcoal_burners_clearing.hint", "Someone stacked logs here, buried them under earth and never lit "
                + "them. A pile sealed on every side burns slowly into charcoal instead of ash. Cover the open side with earth, "
                + "then light it. They slept warm. Look where they slept.");
        add.accept(place + ".prospector_camp", "Abandoned prospector's camp");
        add.accept(place + ".prospector_camp.hint", "A prospector laid three stones on a counter: the rock at the surface, the rock "
                + "below it, and the hard rock at the bottom. Every region stacks its own three rocks, and every rock keeps its "
                + "own ores. The pebbles of ore on the ground here sit above a vein. The trench ends where they stopped digging.");
        add.accept(place + ".mining_camp", "Mining camp");
        add.accept(place + ".mining_camp.hint", "Miners followed this vein into the hill, propping the tunnel as they went. They "
                + "stopped at the poor ore on the vein's edge; the rich ore at the heart of a vein is still down there. Their "
                + "notes mention ore their picks could not cut.");
        add.accept(place + ".collapsed_adit", "Collapsed adit");
        add.accept(place + ".collapsed_adit.hint", "The mouth of an old mine has fallen in. Mine timbers still hold up the "
                + "tunnel beyond. Old tunnels were dug toward ore.");
        add.accept(place + ".ruined_bloomery", "Ruined bloomery");
        add.accept(place + ".ruined_bloomery.hint", "A chimney of pale bricks, cracked by heat, and heaps of glassy slag. Smiths "
                + "once made iron here: not by melting it, but by baking ore with charcoal in a tall brick stack. These bricks "
                + "were made from a special pale clay. The slag still holds a little iron. Three bricks are missing from the "
                + "stack. The roof fell on something. Move the beams.");
        add.accept(place + ".placer_workings", "Placer workings");
        add.accept(place + ".placer_workings.hint", "Panners washed river gravel here and left a heap they never finished. "
                + "Heavy grains settle when gravel is swirled in water; the light stuff washes away. Look for glints in river "
                + "gravel. They hid what they found where the river could watch it.");
    }

    /** The prospector's remark, six per mineral. A few words each: the note says the rest (structures v2 2a). */
    private static String[] hands(OreMineral mineral) {
        return switch (mineral) {
            case NATIVE_COPPER -> new String[] {"Soft metal, bends.", "Red metal in the gravel.", "Green stain on the rock.",
                    "Beat it flat cold.", "Metal in the cracks.", "Near the grass. Stone picks do."};
            case MALACHITE -> new String[] {"Green, banded.", "Green crust on the stones.", "Green rubs off on the fingers.",
                    "Under the soil. Stone picks do.", "Crushes soft. Melts to copper.", "Whole slope stained green."};
            case TENNANTITE -> new String[] {"Grey. Stinks when heated.", "Dark grey grains.", "Steel-grey. Roast it upwind.",
                    "Harder metal than the green ore.", "Grey, heavy.", "Dull grey, copper inside."};
            case CASSITERITE -> new String[] {"Black, heavy grains.", "Black. Heavier than it looks.", "Dark glassy crystals. Copper pick.",
                    "Tin. Stone picks skid off.", "Heavy dark pebbles in the stream.", "Black stone. Needs a better pick."};
            case BISMUTHINITE -> new String[] {"Grey needles. Melts easy.", "Grey, rainbow sheen.", "Silver-grey fans of crystal.",
                    "Soft grey ore.", "Bright streaks, melts low.", "Grey. Easy to dig."};
            case HEMATITE -> new String[] {"Red, heavy. Too hard for our picks.", "Blood-red ore.", "Red streak. Bronze barely marks it.",
                    "Rust-red rock, heavy.", "Red all over. Needs a harder pick.", "Red ore. Iron."};
            case MAGNETITE -> new String[] {"Black. Pulls a needle.", "Heavy black, drags a knife blade.", "Black ore. Picks bounce off.",
                    "Black, very heavy.", "Lodestone. Iron.", "Black ore, magnetic."};
            case LIMONITE -> new String[] {"Rusty lumps in the bog.", "Brown ore in wet ground. Use a shovel.", "Yellow-brown crust in the marsh.",
                    "Bog ore. Poor, but iron.", "Rust-coloured lumps under the reeds.", "Cut peat, find iron."};
            case NATIVE_GOLD -> new String[] {"Yellow flecks in quartz.", "Gold in white rock. Too soft for tools.", "Gold threads in the vein.",
                    "Yellow, heavy.", "Gold in the river sand below.", "Little gold. Worth something."};
            default -> new String[] {"Ore in the rock.", "Marked for later.", "Heavy rock, some shine.", "Showing where the hill is cut.",
                    "Worth a closer look.", "Noted."};
        };
    }

    /** What the people of each place wrote down (structures v2 2a and 4.2): plain, short, practical. */
    private static String[] ledgerTexts(String place) {
        return switch (place) {
            case "charcoal_burners_clearing" -> new String[] {
                    "Third burn this month. Sealed it with turf this time and it came out black all the way through. Last one was half ash.",
                    "Out of straw. Walked to the next clearing for more, two days there and back. Leaving the axe here, the handle is split anyway."};
            case "prospector_camp" -> new String[] {
                    "Three stones on the counter, top rock, middle rock, bottom rock, in that order. Every hill here stacks them the same way. Look at the stones before you dig. Spare things are in the crate at the end of the trench.",
                    "Tin stone half a day out. Needs a copper pick and mine is stone. Marked the spot, will come back."};
            case "mining_camp" -> new String[] {
                    "Lower level flooded again. Pumps can't keep up. Moving the crew to the east face.",
                    "Poor ore at the edges. Nothing in the middle but hard grey stone we can't cut. Foreman says leave it. Pay day Friday."};
            case "collapsed_adit" -> new String[] {
                    "Roof came down Tuesday. Nobody hurt. The props at the far end still hold, we think. Not going back in.",
                    "Followed the vein from the portal and it dips. Props every three steps. Bring plenty of timber."};
            case "ruined_bloomery" -> new String[] {
                    "Fire bricks crack after a few burns. The pale clay holds longest. Stack lost three, spares are in the crate. Slag is mostly iron still, don't throw it out.",
                    "Bloom came out small again. Too little charcoal, too much hurry. Don't open it before the glow is gone.",
                    "Stack plan. Pale bricks only, three to a side, the door on the front and the chimney on top. Slag collects at the bottom."};
            case "placer_workings" -> new String[] {
                    "River gravel, three pans a day. Gold is heavy and sits at the bottom. Tip slow and the light stuff goes over the edge.",
                    "Water rose and took half the trough. Will rebuild when it drops."};
            default -> new String[0];
        };
    }
}
