package dev.strataindustria.datagen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.geology.Rock;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.transport.foot.CairnBlock;
import dev.strataindustria.transport.foot.FootRegistry;
import java.util.Optional;
import java.util.function.BiConsumer;
import net.minecraft.advancements.Advancement;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import java.util.function.Function;
import net.minecraft.core.HolderGetter;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.advancements.predicates.StatePropertiesPredicate;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.MatchBlock;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

/** Models, text, loot and recipes for tier 2 on foot (outposts and transport spec 4): {@link FootRegistry}. */
final class FootData {
    private FootData() {}

    // ---------------------------------------------------------------- models

    static void models(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        for (var item : java.util.List.of(FootRegistry.ROPE, FootRegistry.PACK_FRAME, FootRegistry.HANDCART)) {
            itemModels.generateFlatItem(item.get(), ModelTemplates.FLAT_ITEM);
        }

        // Rope ladder and blaze: hand-made models, turned to face their side.
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(FootRegistry.ROPE_LADDER.get())
                .with(facing(StrataIndustria.id("block/rope_ladder"))));
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(FootRegistry.BLAZE_MARK.get())
                .with(facing(StrataIndustria.id("block/blaze_mark"))));

        // Cairn: one model per rock and height, textured with the rock the stones came from.
        Block cairn = FootRegistry.CAIRN.get();
        PropertyDispatch.C2<MultiVariant, Rock, Integer> dispatch = PropertyDispatch.initial(CairnBlock.ROCK, CairnBlock.HEIGHT);
        for (int height = 1; height <= 3; height++) {
            ModelTemplate template = new ModelTemplate(Optional.of(StrataIndustria.id("block/template_cairn_" + height)),
                    Optional.empty(), ModModelProvider.ROCK);
            for (Rock rock : Rock.values()) {
                Identifier model = template.createWithSuffix(cairn, "_" + rock.id() + "_" + height,
                        TextureMapping.singleSlot(ModModelProvider.ROCK, ModModelProvider.blockTexture(rock.id())), blockModels.modelOutput);
                dispatch.select(rock, height, BlockModelGenerators.plainVariant(model));
            }
        }
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(cairn).with(dispatch));
    }

    private static PropertyDispatch.C1<MultiVariant, Direction> facing(Identifier model) {
        MultiVariant north = BlockModelGenerators.plainVariant(model);
        PropertyDispatch.C1<MultiVariant, Direction> facing = PropertyDispatch.initial(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING);
        facing.select(Direction.NORTH, north);
        facing.select(Direction.EAST, north.with(BlockModelGenerators.Y_ROT_90));
        facing.select(Direction.SOUTH, north.with(BlockModelGenerators.Y_ROT_180));
        facing.select(Direction.WEST, north.with(BlockModelGenerators.Y_ROT_270));
        return facing;
    }

    // ---------------------------------------------------------------- text

    static void lang(BiConsumer<String, String> add) {
        String id = StrataIndustria.MOD_ID;
        add.accept("item." + id + ".rope", "Rope");
        add.accept("item." + id + ".pack_frame", "Pack Frame");
        add.accept("item." + id + ".handcart", "Handcart");
        add.accept("block." + id + ".rope_ladder", "Rope Ladder");
        add.accept("block." + id + ".cairn", "Cairn");
        add.accept("block." + id + ".blaze_mark", "Blaze");
        add.accept("entity." + id + ".handcart", "Handcart");
        add.accept("container." + id + ".pack_frame", "Pack Frame");
        add.accept("key." + id + ".pack", "Open Pack Frame");
        add.accept("key.category." + id + ".transport", "Strata Industria: Transport");

        add.accept(id + ".rope.too_long", "That is as long as a rope ladder goes.");
        add.accept(id + ".handcart.blocked", "The cart won't go up there.");
        add.accept(id + ".trail.next", "Next mark: %s blocks %s.");
        add.accept(id + ".trail.back", "Back: %s blocks %s.");
        add.accept(id + ".trail.end", "Last mark on this trail.");
        add.accept(id + ".trail.alone", "No other mark near this one.");
        for (String direction : new String[] {"north", "north_east", "east", "south_east", "south", "south_west", "west", "north_west"}) {
            add.accept(id + ".trail.dir." + direction, direction.replace('_', '-'));
        }

        String subtitles = "subtitles." + id + ".";
        add.accept(subtitles + "rope.hang", "Rope hangs down");
        add.accept(subtitles + "pack.open", "Pack straps creak");
        add.accept(subtitles + "cairn.stack", "Stones stacked");
        add.accept(subtitles + "blaze.cut", "Bark cut");
        add.accept(subtitles + "handcart.roll", "Handcart rattles");
        add.accept(subtitles + "handcart.shafts", "Shafts shift");

        String journal = "journal." + id + ".";
        add.accept(journal + "t2.pack_frame", "Pack on the Back");
        add.accept(journal + "t2.pack_frame.hint", "Wear a pack frame in the chest slot.");
        add.accept(journal + "t2.pack_frame.lead", "My pockets are full again. What carries more than a back can hold in its hands?");
        add.accept(journal + "t2.pack_frame.note", "Frame on my back, nine more slots. When it is more than half full I can't run.");
        add.accept(journal + "t2.handcart", "Handcart");
        add.accept(journal + "t2.handcart.hint", "Pull a handcart 200 blocks without letting go of the shafts.");
        add.accept(journal + "t2.handcart.lead", "A pack won't carry ore for long. What if the load had wheels?");
        add.accept(journal + "t2.handcart.note", "Two wheels and two shafts. Holds a lot, but a full block stops it.");
        add.accept(journal + "t2.trail", "Marked Trail");
        add.accept(journal + "t2.trail.hint", "Set four cairns or blazes in a row, each within 64 blocks of the last.");
        add.accept(journal + "t2.trail.lead", "I will never find that tin hill again. How did the old prospectors mark a way?");
        add.accept(journal + "t2.trail.note", "Cairns on open ground, blazes on trees. Touch one and it points to the next.");
    }

    // ---------------------------------------------------------------- recipes

    static final class Recipes extends net.minecraft.data.recipes.RecipeProvider {
        Recipes(BootstrapContext<Recipe<?>> recipes, BootstrapContext<Advancement> advancements) {
            super(recipes, advancements);
        }

        private static ResourceKey<Recipe<?>> key(String path) {
            return ResourceKey.create(Registries.RECIPE, StrataIndustria.id(path));
        }

        @Override
        protected void buildRecipes() {
            shapeless(RecipeCategory.MISC, FootRegistry.ROPE.get())
                    .requires(ModItems.TWINE.get(), 3)
                    .unlockedBy("has_twine", has(ModItems.TWINE.get()))
                    .save(output, key("rope"));
            shaped(RecipeCategory.TOOLS, FootRegistry.PACK_FRAME.get())
                    .pattern("S S")
                    .pattern("TLT")
                    .pattern("S S")
                    .define('S', Items.STICK)
                    .define('T', ModItems.TWINE.get())
                    .define('L', Items.LEATHER)
                    .unlockedBy("has_leather", has(Items.LEATHER))
                    .save(output, key("pack_frame"));
            shaped(RecipeCategory.TRANSPORTATION, FootRegistry.HANDCART.get())
                    .pattern("PBP")
                    .pattern("PPP")
                    .pattern("S S")
                    .define('P', net.minecraft.tags.ItemTags.PLANKS)
                    .define('B', ModItems.ingot(dev.strataindustria.material.Metal.BRONZE))
                    .define('S', Items.STICK)
                    .unlockedBy("has_bronze_ingot", has(ModItems.ingot(dev.strataindustria.material.Metal.BRONZE)))
                    .save(output, key("handcart"));
        }
    }

    // ---------------------------------------------------------------- loot

    /** The ladder gives its rope back; a cairn gives back the rocks it was built from, four to a course. */
    static void loot(BiConsumer<Block, Function<Block, LootTable.Builder>> add, HolderGetter<Block> blocks) {
        add.accept(FootRegistry.ROPE_LADDER.get(), block -> LootTable.lootTable().withPool(LootPool.lootPool()
                .setRolls(ContextIntProviders.exactly(1))
                .add(LootItem.lootTableItem(FootRegistry.ROPE.get()))));
        add.accept(FootRegistry.CAIRN.get(), block -> {
            LootTable.Builder table = LootTable.lootTable();
            for (Rock rock : Rock.values()) {
                for (int height = 1; height <= 3; height++) {
                    table.withPool(LootPool.lootPool().setRolls(ContextIntProviders.exactly(1))
                            .when(MatchBlock.blockMatches(blocks, block,
                                    StatePropertiesPredicate.Builder.properties()
                                            .hasProperty(CairnBlock.ROCK, rock)
                                            .hasProperty(CairnBlock.HEIGHT, height)))
                            .add(LootItem.lootTableItem(ModItems.ROCK_SHARD.get(rock).get())
                                    .apply(SetItemCountFunction.setCount(ContextIntProviders.exactly(CairnBlock.ROCKS_PER_COURSE * height)))));
                }
            }
            return table;
        });
    }
}
