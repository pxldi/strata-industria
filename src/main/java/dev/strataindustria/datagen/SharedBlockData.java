package dev.strataindustria.datagen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.geology.Rock;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.structure.CrateBlock;
import dev.strataindustria.structure.MinersLampBlock;
import dev.strataindustria.structure.OreCartBlock;
import dev.strataindustria.structure.RubbleBlock;
import dev.strataindustria.structure.SharedBlocks;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import net.minecraft.advancements.Advancement;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * Models, lang, recipes and loot of the shared structure blocks (structures v2 section 5). The models of the
 * odd shapes are hand-built in resources; this class wires the block states to them.
 */
final class SharedBlockData {
    private SharedBlockData() {}

    /** Every block {@link SharedBlocks} registers, for the loot audit. */
    static List<Block> blocks() {
        java.util.ArrayList<Block> all = new java.util.ArrayList<>(List.of(SharedBlocks.CRATE.get(), SharedBlocks.MINERS_LAMP.get(),
                SharedBlocks.ORE_CART.get(), SharedBlocks.TOOL_RACK.get(), SharedBlocks.SMOULDERING_LOG_PILE.get(),
                SharedBlocks.WINDLASS.get(), SharedBlocks.SLUICE_BOX.get()));
        for (Rock rock : Rock.values()) {
            all.add(SharedBlocks.RUBBLE.get(rock).get());
            all.add(SharedBlocks.CRACKED.get(rock).get());
            all.add(SharedBlocks.MOSSY_COBBLED.get(rock).get());
        }
        return all;
    }

    // ---------------------------------------------------------------- models

    private static net.minecraft.resources.Identifier model(String name) {
        return StrataIndustria.id("block/" + name);
    }

    static void models(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        // Crate: a shut one is nailed and roped.
        PropertyDispatch.C1<MultiVariant, Boolean> crate = PropertyDispatch.initial(CrateBlock.LOCKED);
        crate.select(false, BlockModelGenerators.plainVariant(model("crate")));
        crate.select(true, BlockModelGenerators.plainVariant(model("crate_locked")));
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(SharedBlocks.CRATE.get()).with(crate));
        itemModels.itemModelOutput.accept(SharedBlocks.CRATE_ITEM.get(), ItemModelUtils.plainModel(model("crate")));

        // Miner's lamp: standing or hanging, off, burning or guttering. The item is a flat icon.
        PropertyDispatch.C2<MultiVariant, Boolean, MinersLampBlock.Mode> lamp =
                PropertyDispatch.initial(MinersLampBlock.HANGING, MinersLampBlock.MODE);
        for (boolean hanging : new boolean[] {false, true}) {
            for (MinersLampBlock.Mode mode : MinersLampBlock.Mode.values()) {
                lamp.select(hanging, mode, BlockModelGenerators.plainVariant(
                        model("miners_lamp_" + (hanging ? "hanging_" : "standing_") + mode.getSerializedName())));
            }
        }
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(SharedBlocks.MINERS_LAMP.get()).with(lamp));
        itemModels.generateFlatItem(SharedBlocks.MINERS_LAMP_ITEM.get(), ModelTemplates.FLAT_ITEM);

        // Ore cart: the model runs north to south; the four fill levels show more ore.
        PropertyDispatch.C2<MultiVariant, Direction, Integer> cart = PropertyDispatch.initial(OreCartBlock.FACING, OreCartBlock.FILL);
        for (int fill = 0; fill <= 3; fill++) {
            var base = BlockModelGenerators.plainVariant(model("ore_cart_" + fill));
            cart.select(Direction.NORTH, fill, base);
            cart.select(Direction.SOUTH, fill, base.with(BlockModelGenerators.Y_ROT_180));
            cart.select(Direction.EAST, fill, base.with(BlockModelGenerators.Y_ROT_90));
            cart.select(Direction.WEST, fill, base.with(BlockModelGenerators.Y_ROT_270));
        }
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(SharedBlocks.ORE_CART.get()).with(cart));
        itemModels.itemModelOutput.accept(SharedBlocks.ORE_CART_ITEM.get(), ItemModelUtils.plainModel(model("ore_cart_2")));

        facing(blockModels, itemModels, SharedBlocks.TOOL_RACK, SharedBlocks.TOOL_RACK_ITEM.get(), "tool_rack");
        facing(blockModels, itemModels, SharedBlocks.WINDLASS, SharedBlocks.WINDLASS_ITEM.get(), "windlass");
        facing(blockModels, itemModels, SharedBlocks.SLUICE_BOX, SharedBlocks.SLUICE_BOX_ITEM.get(), "sluice_box");

        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(SharedBlocks.SMOULDERING_LOG_PILE.get(),
                BlockModelGenerators.plainVariant(model("smouldering_log_pile"))));
        itemModels.itemModelOutput.accept(SharedBlocks.SMOULDERING_LOG_PILE_ITEM.get(), ItemModelUtils.plainModel(model("smouldering_log_pile")));

        for (Rock rock : Rock.values()) {
            // Rubble: broken pieces of the rock's cobbled texture, one to three layers, turned at random.
            Block rubble = SharedBlocks.RUBBLE.get(rock).get();
            PropertyDispatch.C1<MultiVariant, Integer> layers = PropertyDispatch.initial(RubbleBlock.LAYERS);
            net.minecraft.resources.Identifier two = null;
            for (int layer = 1; layer <= 3; layer++) {
                ModelTemplate template = new ModelTemplate(Optional.of(model("template_rubble_" + layer)), Optional.empty(), ModModelProvider.ROCK);
                var id = template.createWithSuffix(rubble, "_" + layer,
                        TextureMapping.singleSlot(ModModelProvider.ROCK, ModModelProvider.blockTexture("cobbled_" + rock.id())), blockModels.modelOutput);
                if (layer == 2) two = id;
                layers.select(layer, BlockModelGenerators.createRotatedVariants(BlockModelGenerators.plainModel(id)));
            }
            blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(rubble).with(layers));
            itemModels.itemModelOutput.accept(SharedBlocks.RUBBLE_ITEMS.get(rock).get(), ItemModelUtils.plainModel(two));

            blockModels.createTrivialCube(SharedBlocks.CRACKED.get(rock).get());
            blockModels.createTrivialCube(SharedBlocks.MOSSY_COBBLED.get(rock).get());
        }
    }

    private static void facing(BlockModelGenerators blockModels, ItemModelGenerators itemModels, DeferredBlock<?> block,
            net.minecraft.world.item.Item item, String model) {
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(block.get(), BlockModelGenerators.plainVariant(model(model)))
                .with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING));
        itemModels.itemModelOutput.accept(item, ItemModelUtils.plainModel(model(model)));
    }

    // ---------------------------------------------------------------- recipes

    /** Crate, lamp, cart and rack are made at a bench; mossy rock is cobble with a vine over it. Cracked rock is only weathered. */
    static final class Recipes extends net.minecraft.data.recipes.RecipeProvider {
        Recipes(BootstrapContext<Recipe<?>> recipes, BootstrapContext<Advancement> advancements) {
            super(recipes, advancements);
        }

        private static ResourceKey<Recipe<?>> key(String path) {
            return ResourceKey.create(Registries.RECIPE, StrataIndustria.id(path));
        }

        @Override
        protected void buildRecipes() {
            shaped(RecipeCategory.DECORATIONS, SharedBlocks.CRATE_ITEM.get())
                    .pattern("PPP")
                    .pattern("T T")
                    .pattern("PPP")
                    .define('P', ItemTags.PLANKS)
                    .define('T', ModItems.TWINE.get())
                    .unlockedBy("has_twine", has(ModItems.TWINE.get()))
                    .save(output, key("crate"));
            shaped(RecipeCategory.DECORATIONS, SharedBlocks.MINERS_LAMP_ITEM.get())
                    .pattern(" T ")
                    .pattern("CGC")
                    .pattern(" C ")
                    .define('T', ModItems.TWINE.get())
                    .define('G', Items.GLASS_PANE)
                    .define('C', Items.COPPER_INGOT)
                    .unlockedBy("has_copper_ingot", has(Items.COPPER_INGOT))
                    .save(output, key("miners_lamp"));
            shaped(RecipeCategory.TRANSPORTATION, SharedBlocks.ORE_CART_ITEM.get())
                    .pattern("P P")
                    .pattern("PPP")
                    .pattern("C C")
                    .define('P', ItemTags.PLANKS)
                    .define('C', Items.COPPER_INGOT)
                    .unlockedBy("has_copper_ingot", has(Items.COPPER_INGOT))
                    .save(output, key("ore_cart"));
            shaped(RecipeCategory.DECORATIONS, SharedBlocks.TOOL_RACK_ITEM.get())
                    .pattern("SSS")
                    .pattern("P P")
                    .define('S', Items.STICK)
                    .define('P', ItemTags.PLANKS)
                    .unlockedBy("has_stick", has(Items.STICK))
                    .save(output, key("tool_rack"));
            shaped(RecipeCategory.DECORATIONS, SharedBlocks.WINDLASS_ITEM.get())
                    .pattern("S S")
                    .pattern("SLS")
                    .pattern("T T")
                    .define('S', Items.STICK)
                    .define('L', ItemTags.LOGS)
                    .define('T', ModItems.TWINE.get())
                    .unlockedBy("has_twine", has(ModItems.TWINE.get()))
                    .save(output, key("windlass"));
            shaped(RecipeCategory.DECORATIONS, SharedBlocks.SLUICE_BOX_ITEM.get())
                    .pattern("P P")
                    .pattern("PSP")
                    .define('P', ItemTags.PLANKS)
                    .define('S', Items.STICK)
                    .unlockedBy("has_stick", has(Items.STICK))
                    .save(output, key("sluice_box"));
            for (Rock rock : Rock.values()) {
                shapeless(RecipeCategory.BUILDING_BLOCKS, SharedBlocks.MOSSY_COBBLED_ITEMS.get(rock).get())
                        .requires(ModBlocks.COBBLED_ROCK.get(rock).get())
                        .requires(Items.VINE)
                        .unlockedBy("has_cobbled", has(ModBlocks.COBBLED_ROCK.get(rock).get()))
                        .save(output, key("mossy_cobbled_" + rock.id()));
            }
        }
    }

    // ---------------------------------------------------------------- loot

    /** Everything drops itself, except what the ruin made of it: rubble gives a loose rock now and then and the pile gives ash. */
    static final class Loot extends BlockLootSubProvider {
        Loot(LootTableSubProvider.Context context) {
            super(java.util.Set.of(), FeatureFlags.REGISTRY.allFlags(), context);
        }

        @Override
        protected void generate() {
            // Crates and carts keep what they hold in the block entity; the container drops it.
            for (Block block : List.of(SharedBlocks.CRATE.get(), SharedBlocks.MINERS_LAMP.get(), SharedBlocks.ORE_CART.get(),
                    SharedBlocks.TOOL_RACK.get(), SharedBlocks.WINDLASS.get(), SharedBlocks.SLUICE_BOX.get())) {
                dropSelf(block);
            }
            add(SharedBlocks.SMOULDERING_LOG_PILE.get(), block -> LootTable.lootTable().withPool(LootPool.lootPool()
                    .setRolls(ContextIntProviders.exactly(1))
                    .add(LootItem.lootTableItem(ModItems.ASH.get())
                            .apply(SetItemCountFunction.setCount(ContextIntProviders.between(1, 3))))));
            for (Rock rock : Rock.values()) {
                dropSelf(SharedBlocks.CRACKED.get(rock).get());
                dropSelf(SharedBlocks.MOSSY_COBBLED.get(rock).get());
                add(SharedBlocks.RUBBLE.get(rock).get(), block -> LootTable.lootTable().withPool(LootPool.lootPool()
                        .setRolls(ContextIntProviders.exactly(1))
                        .add(LootItem.lootTableItem(ModItems.LOOSE_ROCK.get(rock).get())
                                .when(LootItemRandomChanceCondition.randomChance(0.2f)))));
            }
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return blocks();
        }
    }

    // ---------------------------------------------------------------- lang

    private static String titled(String id) {
        return java.util.Arrays.stream(id.split("_")).map(w -> Character.toUpperCase(w.charAt(0)) + w.substring(1))
                .collect(java.util.stream.Collectors.joining(" "));
    }

    static void lang(BiConsumer<String, String> add) {
        String id = StrataIndustria.MOD_ID;
        add.accept("block." + id + ".crate", "Crate");
        add.accept("block." + id + ".crate.locked", "Nailed shut.");
        add.accept("block." + id + ".miners_lamp", "Miner's Lamp");
        add.accept("block." + id + ".ore_cart", "Ore Cart");
        add.accept("block." + id + ".tool_rack", "Tool Rack");
        add.accept("block." + id + ".smouldering_log_pile", "Smouldering Log Pile");
        add.accept("block." + id + ".windlass", "Windlass");
        add.accept("block." + id + ".sluice_box", "Sluice Box");
        for (Rock rock : Rock.values()) {
            String name = titled(rock.id());
            add.accept("block." + id + ".rubble_" + rock.id(), name + " Rubble");
            add.accept("block." + id + ".cracked_" + rock.id(), "Cracked " + name);
            add.accept("block." + id + ".mossy_cobbled_" + rock.id(), "Mossy Cobbled " + name);
        }
        String subtitles = "subtitles." + id + ".";
        add.accept(subtitles + "crate.open", "Crate opens");
        add.accept(subtitles + "crate.close", "Crate closes");
        add.accept(subtitles + "crate.locked", "Crate rattles");
        add.accept(subtitles + "crate.unlock", "Latch gives");
        add.accept(subtitles + "miners_lamp.light", "Lamp lights");
        add.accept(subtitles + "miners_lamp.snuff", "Lamp goes out");
        add.accept(subtitles + "miners_lamp.flutter", "Flame flutters");
        add.accept(subtitles + "ore_cart.rattle", "Cart rattles");
        add.accept(subtitles + "tool_rack.clink", "Tool clinks");
        add.accept(subtitles + "rubble.break", "Rubble scatters");
        add.accept(subtitles + "cracked_rock.break", "Stone crumbles");
        add.accept(subtitles + "smouldering_log_pile.crackle", "Embers crackle");
        add.accept(subtitles + "windlass.creak", "Rope creaks");
    }
}
