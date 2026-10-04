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
import dev.strataindustria.structure.StructureContent;
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

        // Near-origin guarantee (structures spec 3.3): one ring of three tries around the world origin.
        context.register(set("prospector_camp_near_origin"), new StructureSet(
                structures.getOrThrow(CampStructure.Layout.PROSPECTOR_CAMP.key()),
                new ConcentricRingsStructurePlacement(4, 3, 3, biomes.getOrThrow(biomes(CampStructure.Layout.PROSPECTOR_CAMP)))));
        context.register(set("mining_camp_near_origin"), new StructureSet(
                structures.getOrThrow(CampStructure.Layout.MINING_CAMP.key()),
                new ConcentricRingsStructurePlacement(10, 3, 3, biomes.getOrThrow(biomes(CampStructure.Layout.MINING_CAMP)))));
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
        var entry = LootItem.lootTableItem(item);
        if (min != 1 || max != 1) entry.apply(SetItemCountFunction.setCount(min == max ? ContextIntProviders.exactly(min)
                : ContextIntProviders.between(min, max)));
        return new LootPoolEntry(entry);
    }

    /** A tool left behind: 10 to 30% of its durability left (structures spec 4.1, L3). */
    private static LootPoolEntry worn(ItemLike item) {
        return new LootPoolEntry(LootItem.lootTableItem(item).apply(SetItemDamageFunction.setDamage(ContextFloatProviders.between(0.1f, 0.3f))));
    }

    private static LootPoolEntry notes(SurveyNotes notes) {
        return new LootPoolEntry(LootItem.lootTableItem(StructureContent.SURVEY_NOTES.get())
                .apply(SetComponentsFunction.setComponent(StructureContent.SURVEY.get(), notes)));
    }

    /** Minerals of tier 3 and above are never given as items before their tier (structures spec 4.1, L6 and L8). */
    private static boolean early(OreMineral mineral) {
        return !mineral.needsBronzeTool();
    }

    private static Item small(OreMineral mineral) {
        return early(mineral) ? ModItems.SMALL_ORES.get(mineral).get() : Items.FLINT;
    }

    static final class ChestLoot implements LootTableSubProvider {
        private final LootTableSubProvider.Context context;

        ChestLoot(LootTableSubProvider.Context context) {
            this.context = context;
        }

        private void add(String path, LootTable.Builder table) {
            context.accept(CampLoot.key(path), table);
        }

        @Override
        public void run() {
            // Charcoal burners' clearing (H = 0).
            add(CampLoot.CLEARING_HUT, LootTable.lootTable()
                    .withPool(pool(1, item(Items.STICK, 4, 10)))
                    .withPool(pool(0.9f, item(ModItems.STRAW.get(), 3, 8)))
                    .withPool(pool(0.7f, item(ModItems.TWINE.get(), 1, 4)))
                    .withPool(pool(0.6f, item(Items.APPLE, 1, 3)))
                    .withPool(pool(0.4f, item(Items.BREAD, 1, 2)))
                    .withPool(pool(0.8f, worn(ModItems.FIRESTARTER.get())))
                    .withPool(pool(0.5f, worn(ModItems.STONE_AXE.get())))
                    .withPool(pool(0.5f, worn(ModItems.STONE_SHOVEL.get())))
                    .withPool(pool(0.6f, item(ModItems.ASH.get(), 1, 4))));

            // Mining camp tents and smithy (H = 2).
            add(CampLoot.MINING_TENT, LootTable.lootTable()
                    .withPool(LootPool.lootPool().setRolls(ContextIntProviders.exactly(1))
                            .when(LootItemRandomChanceCondition.randomChance(0.8f))
                            .add(item(Items.BREAD, 1, 4).builder())
                            .add(item(Items.BAKED_POTATO, 1, 4).builder())
                            .add(item(Items.COOKED_MUTTON, 1, 4).builder()))
                    .withPool(pool(0.7f, item(Items.TORCH, 3, 8)))
                    .withPool(pool(0.6f, item(ModItems.TWINE.get(), 1, 3)))
                    .withPool(pool(0.6f, item(ModItems.FIBRE_CLOTH.get(), 1, 3)))
                    .withPool(pool(0.4f, item(Items.FLINT, 1, 3)))
                    .withPool(pool(0.3f, item(Items.PAPER, 1, 2))));
            add(CampLoot.MINING_SMITHY, LootTable.lootTable()
                    .withPool(pool(1, item(Items.CHARCOAL, 4, 10)))
                    .withPool(pool(0.8f, item(ModItems.NUGGETS.get(Metal.COPPER).get(), 2, 4)))
                    .withPool(pool(0.6f, item(ModItems.NUGGETS.get(Metal.TIN).get(), 1, 3)))
                    .withPool(pool(0.5f, item(ModItems.INGOT_MOLD.get(), 1, 1)))
                    .withPool(pool(0.2f, item(ModItems.MOLDS.get(MoldType.PICKAXE_HEAD).get(), 1, 1)))
                    .withPool(pool(0.5f, worn(ModItems.STONE_HAMMER.get())))
                    .withPool(pool(0.6f, item(ModItems.STRAW.get(), 2, 6)))
                    .withPool(pool(0.6f, item(Items.STICK, 2, 6))));

            // Ruined bloomery (H = 2): a taste of iron, too little to work, and where the smiths found theirs.
            add(CampLoot.BLOOMERY_CACHE, LootTable.lootTable()
                    .withPool(pool(1, notes(BLOOMERY_NOTES)))
                    .withPool(pool(0.8f, item(Items.CHARCOAL, 2, 6)))
                    .withPool(pool(0.6f, item(ModItems.ASH.get(), 1, 4)))
                    .withPool(pool(0.5f, item(Items.IRON_NUGGET, 1, 3)))
                    .withPool(pool(0.4f, item(ModItems.NUGGETS.get(Metal.COPPER).get(), 2, 6))));

            // Placer workings (H = 2): a few grains of gold, never the washed ore itself.
            add(CampLoot.PLACER_CACHE, LootTable.lootTable()
                    .withPool(pool(0.7f, item(Items.GOLD_NUGGET, 1, 3)))
                    .withPool(pool(0.5f, notes(PLACER_NOTES)))
                    .withPool(LootPool.lootPool().setRolls(ContextIntProviders.exactly(1))
                            .when(LootItemRandomChanceCondition.randomChance(0.7f))
                            .add(item(Items.BREAD, 1, 3).builder())
                            .add(item(Items.COOKED_SALMON, 1, 3).builder()))
                    .withPool(pool(0.5f, item(Items.BOWL, 1, 1)))
                    .withPool(pool(0.5f, item(ModItems.TWINE.get(), 1, 4))));

            for (OreMineral mineral : OreMineral.values()) {
                // Abandoned prospector's camp (H = 1).
                context.accept(CampLoot.key(CampLoot.PROSPECTOR_PACK, mineral), LootTable.lootTable()
                        .withPool(pool(1, notes(PROSPECTOR_NOTES)))
                        .withPool(pool(1, item(small(mineral), 2, 4)))
                        .withPool(pool(0.6f, item(Items.CLAY_BALL, 3, 8)))
                        .withPool(pool(0.6f, worn(ModItems.STONE_PICKAXE.get())))
                        .withPool(pool(0.4f, worn(ModItems.STONE_KNIFE.get())))
                        .withPool(pool(0.25f, item(ModItems.UNFIRED_CRUCIBLE.get(), 1, 1)))
                        .withPool(pool(0.7f, item(ModItems.TWINE.get(), 2, 6)))
                        .withPool(pool(0.7f, item(ModItems.PLANT_FIBRE.get(), 2, 6)))
                        .withPool(pool(0.6f, item(Items.BREAD, 1, 3)))
                        .withPool(pool(0.6f, item(Items.COOKED_COD, 1, 3)))
                        .withPool(pool(0.5f, item(Items.TORCH, 2, 5))));

                // Mining camp ore cart (H = 2): the ore the crew sorted, and notes on the iron they could not cut.
                LootTable.Builder cart = LootTable.lootTable().withPool(pool(1, notes(MINING_NOTES)));
                if (early(mineral)) {
                    cart.withPool(pool(1, item(ModItems.orePiece(mineral, OreGrade.POOR), 2, 5)))
                            .withPool(pool(0.7f, item(ModItems.orePiece(mineral, OreGrade.NORMAL), 1, 3)))
                            .withPool(pool(0.3f, item(ModItems.crushedOre(mineral, OreGrade.NORMAL), 1, 2)));
                }
                if (mineral != OreMineral.CASSITERITE) {
                    cart.withPool(pool(0.3f, item(ModItems.orePiece(OreMineral.CASSITERITE, OreGrade.NORMAL), 1, 2)));
                }
                context.accept(CampLoot.key(CampLoot.MINING_ORE_CART, mineral), cart);

                // Collapsed adit cache (H = 2).
                LootTable.Builder cache = LootTable.lootTable().withPool(pool(0.8f, item(Items.TORCH, 2, 6)));
                if (early(mineral)) cache.withPool(pool(0.7f, item(ModItems.orePiece(mineral, OreGrade.POOR), 2, 5)));
                cache.withPool(pool(0.5f, item(ModItems.NUGGETS.get(Metal.COPPER).get(), 1, 4)))
                        .withPool(pool(0.3f, worn(ModItems.STONE_PICKAXE.get())))
                        .withPool(pool(0.25f, notes(ADIT_NOTES)))
                        .withPool(pool(0.5f, item(Items.BREAD, 1, 2)));
                context.accept(CampLoot.key(CampLoot.ADIT_CACHE, mineral), cache);
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
                UniformContainerBase.Builder<?> entry = entries[i] instanceof LootPoolEntry e ? e.builder()
                        : LootItem.lootTableItem((ItemLike) entries[i]);
                pool.add(entry.setWeight((Integer) entries[i + 1]));
            }
            return LootTable.lootTable().withPool(pool);
        }

        @Override
        public void run() {
            for (OreMineral mineral : OreMineral.values()) {
                Item small = small(mineral);
                Item poor = early(mineral) ? ModItems.orePiece(mineral, OreGrade.POOR) : Items.FLINT;
                Item copper = ModItems.NUGGETS.get(Metal.COPPER).get();
                context.accept(CampLoot.key(CampLoot.DIG_PROSPECTOR, mineral), weighted(small, 50, Items.FLINT, 30, Items.STICK, 20));
                context.accept(CampLoot.key(CampLoot.DIG_SPOIL, mineral), weighted(poor, 30, small, 20, Items.FLINT, 15, copper, 15,
                        Items.BONE, 10, Items.MINER_POTTERY_SHERD, 5, notes(MINING_NOTES), 5));
                context.accept(CampLoot.key(CampLoot.DIG_ADIT, mineral), weighted(Items.FLINT, 30, small, 30, Items.BONE, 20,
                        Items.MINER_POTTERY_SHERD, 5, copper, 15));
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
            dropSelf(StructureContent.PIT_PROP.get());
            // A cracked brick mostly crumbles to nothing; sometimes a piece is worth grinding into grog.
            add(StructureContent.CRACKED_FIRE_BRICKS.get(), block -> createSilkTouchDispatchTable(block, applyExplosionCondition(block,
                    LootItem.lootTableItem(ModItems.GROG.get()).when(LootItemRandomChanceCondition.randomChance(0.3f)))));
            dropOther(StructureContent.SLAG_HEAP.get(), ModItems.BLOOMERY_SLAG.get());
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return StructureContent.BLOCKS.getEntries().stream().map(DeferredHolder::value).map(Block.class::cast)::iterator;
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
    }

    // ---------------------------------------------------------------- assets

    static void models(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        blockModels.createFullAndCarpetBlocks(StructureContent.FIBRE_CANVAS.get(), StructureContent.FIBRE_CANVAS_CARPET.get());

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
    }

    static void lang(BiConsumer<String, String> add) {
        String id = StrataIndustria.MOD_ID;
        add.accept("block." + id + ".fibre_canvas", "Fibre Canvas");
        add.accept("block." + id + ".fibre_canvas_carpet", "Fibre Canvas Carpet");
        add.accept("block." + id + ".pit_prop", "Pit Prop");
        add.accept("block." + id + ".cracked_fire_bricks", "Cracked Fire Bricks");
        add.accept("block." + id + ".slag_heap", "Slag Heap");

        String notes = "item." + id + ".survey_notes";
        add.accept(notes, "Survey Notes");
        add.accept(notes + ".on", "Notes on %s");
        add.accept(notes + ".found", "Deposit found");
        add.accept(notes + ".found_message", "Survey notes: deposit found");
        add.accept(notes + ".blank", "Nothing worth keeping is written here.");
        add.accept(notes + ".unread", "Folded notes. Carry them a moment to read them.");
        add.accept(notes + ".bearing", "%s, %s");
        add.accept(notes + ".distance", "about %s blocks");
        add.accept(notes + ".distance.here", "right around here");
        String[][] compass = {{"north", "North"}, {"north_east", "North-east"}, {"east", "East"}, {"south_east", "South-east"},
                {"south", "South"}, {"south_west", "South-west"}, {"west", "West"}, {"north_west", "North-west"}};
        for (String[] dir : compass) add.accept(notes + ".dir." + dir[0], dir[1]);
        add.accept(notes + ".depth.surface", "At the surface");
        add.accept(notes + ".depth.just_under", "Just under the surface");
        add.accept(notes + ".depth.little_down", "A little way down");
        add.accept(notes + ".depth.deep", "Deep");
        add.accept(notes + ".depth.very_deep", "Very deep");
        add.accept(notes + ".host", "In %s");
        add.accept(notes + ".size.small", "A small deposit");
        add.accept(notes + ".size.medium", "A fair deposit");
        add.accept(notes + ".size.large", "A large deposit");
        add.accept(notes + ".tool.copper", "Needs a copper pick");
        add.accept(notes + ".tool.bronze", "Needs a bronze pick");
        for (OreMineral mineral : OreMineral.values()) {
            String[] hands = hands(mineral);
            for (int i = 0; i < hands.length; i++) add.accept(notes + ".hand." + mineral.id() + "." + i, hands[i]);
        }

        String subtitles = "subtitles." + id + ".";
        add.accept(subtitles + "pit_prop.creak", "Timber creaks");
        add.accept(subtitles + "survey_notes.open", "Notes unfold");
        add.accept(subtitles + "survey_notes.found", "Deposit found");
        add.accept(subtitles + "journal.place", "Journal page written");
        add.accept(subtitles + "cracked_fire_bricks.break", "Brick crumbles");
        add.accept(subtitles + "slag_heap.break", "Slag crunches");

        String place = "journal." + id + ".place";
        add.accept(place + ".noted", "Field journal: %s");
        add.accept(place + ".charcoal_burners_clearing", "Charcoal burners' clearing");
        add.accept(place + ".charcoal_burners_clearing.hint", "Someone stacked logs here, buried them under earth and never lit "
                + "them. A pile sealed on every side burns slowly into charcoal instead of ash. Cover the open side with earth, "
                + "then light it.");
        add.accept(place + ".prospector_camp", "Abandoned prospector's camp");
        add.accept(place + ".prospector_camp.hint", "A prospector laid three stones on a log: the rock at the surface, the rock "
                + "below it, and the hard rock at the bottom. Every region stacks its own three rocks, and every rock keeps its "
                + "own ores. The pebbles of ore on the ground here sit above a vein.");
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
                + "were made from a special pale clay. The slag still holds a little iron.");
        add.accept(place + ".placer_workings", "Placer workings");
        add.accept(place + ".placer_workings.hint", "Panners washed river gravel here and left a heap they never finished. "
                + "Heavy grains settle when gravel is swirled in water; the light stuff washes away. Look for glints in river "
                + "gravel.");
    }

    /** The prospector's own words, six per mineral family (structures spec 5.2 and 14). */
    private static String[] hands(OreMineral mineral) {
        return switch (mineral) {
            case NATIVE_COPPER -> new String[] {
                    "Bright copper in the rock, bent and twisted like roots. Soft enough to hammer cold.",
                    "Green stain on the stones and a lump of red metal in the gravel. Copper, sure as rain.",
                    "Picked a nugget out with a knife. The vein is plain to see once the turf is off.",
                    "Copper you can beat flat on a rock. Worth the walk.",
                    "Found metal where the rock is cracked. It shines when you scratch it.",
                    "Copper close to the grass. Stone picks will do."};
            case MALACHITE -> new String[] {
                    "Green rock, banded like a cut onion. It melts down to copper.",
                    "Bright green crust on the stones. Where it shows, the copper is near.",
                    "Green stone that leaves green on the fingers. A good sign.",
                    "Malachite under the soil. Easy digging with stone.",
                    "Green and soft. Crush it, heat it, and copper runs out.",
                    "The green ore again. Whole slope is stained with it."};
            case TENNANTITE -> new String[] {
                    "Grey ore, dull as lead. It smokes with a garlic stink in the fire. Copper in it all the same.",
                    "Dark grey grains in the rock. Gives copper, and something that hardens it.",
                    "Steel-grey ore. Keep upwind when it roasts.",
                    "Grey copper ore. Makes a harder metal than the green kind.",
                    "Found the grey ore. The old smiths liked it for blades.",
                    "Dull grey and heavy. Copper, with a bite to it."};
            case CASSITERITE -> new String[] {
                    "Heavy black grains in the granite. Our copper picks barely scratch it.",
                    "Black tin stone. Heavier than it looks. This is what makes bronze.",
                    "Dark glassy crystals, very heavy. Tin. Bring a copper pick at least.",
                    "Tin ore, black as soot. Mix its metal with copper and you have bronze.",
                    "Heavy dark pebbles in the stream bed led us up to this.",
                    "The black stone is tin. Stone picks just skid off it."};
            case BISMUTHINITE -> new String[] {
                    "Grey needles in the rock, shining like lead. Bismuth. Melts very easy.",
                    "Soft grey ore with a rainbow sheen. Good for a bronze of its own.",
                    "Bright grey streaks. Melts in a small fire.",
                    "Bismuth ore. Mix it with copper when there is no tin to be had.",
                    "Silver-grey crystals in fans. Easy to dig.",
                    "Grey ore that melts low. Worth a look."};
            case HEMATITE -> new String[] {
                    "Red earth in the shale. Too hard for our picks; it wants a better tool.",
                    "Blood-red ore, heavy. Iron, they say, if the fire is hot enough.",
                    "Red streak on the stone. Bronze barely marks it.",
                    "Iron ore, red as rust. Needs a hotter fire than ours.",
                    "Heavy red rock. The old tales say iron hides in it.",
                    "Red ore everywhere here. We could not cut it."};
            case MAGNETITE -> new String[] {
                    "Black ore that pulls at a needle. Iron, and hard to cut.",
                    "Heavy black stone, it drags a knife blade toward it. Iron.",
                    "Black iron ore. Our picks bounce off.",
                    "The black stone that grabs at metal. Iron, deep in it.",
                    "Very heavy, very black. Wants a better pick than bronze.",
                    "Lodestone ore. Iron for whoever can dig it."};
            case LIMONITE -> new String[] {
                    "Rusty lumps in the bog. Iron, the soft kind.",
                    "Brown iron ore in the wet ground. Dig it with a shovel.",
                    "Yellow-brown crust in the marsh. It smelts to iron.",
                    "Bog ore. Poor stuff, but it is iron.",
                    "Rust-coloured lumps under the reeds.",
                    "The bog is full of iron ore. Cut peat and you find it."};
            case NATIVE_GOLD -> new String[] {
                    "Yellow flecks in the quartz. Gold. Soft and no use for tools.",
                    "Gold in the white rock. Pretty, but it will not hold an edge.",
                    "A gleam of gold in the vein. Too soft for anything but show.",
                    "Gold. Heavy and yellow. The river sand is full of it below here.",
                    "Found gold, little threads of it in the quartz.",
                    "Gold ore. Worth something to someone."};
            default -> new String[] {
                    "Ore in the rock here. Worth a closer look.",
                    "Strange stone. Marked it down to come back to.",
                    "Heavy rock with a shine to it.",
                    "Ore showing where the hill is cut.",
                    "Something worth digging, if the tools allow.",
                    "Noted this one for later."};
        };
    }
}
