package dev.strataindustria.datagen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.material.Metal;
import dev.strataindustria.transport.rail.HayRackBlock;
import dev.strataindustria.transport.rail.InclineWinchBlock;
import dev.strataindustria.transport.rail.RailBufferBlock;
import dev.strataindustria.transport.rail.RailRegistry;
import dev.strataindustria.transport.rail.TubStopBlock;
import java.util.function.BiConsumer;
import net.minecraft.advancements.Advancement;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.RailShape;

/** Models, text and recipes for the wooden tramway (outposts and transport spec 5): {@link RailRegistry}. */
final class RailData {
    private RailData() {}

    // ---------------------------------------------------------------- models

    static void models(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        // Wooden rail: the vanilla rail's flat, curved and raised models with our textures.
        Block rail = RailRegistry.WOODEN_RAIL.get();
        TextureMapping texture = TextureMapping.rail(rail);
        TextureMapping corner = TextureMapping.rail(TextureMapping.getBlockTexture(rail, "_corner"));
        MultiVariant flat = BlockModelGenerators.plainVariant(ModelTemplates.RAIL_FLAT.create(rail, texture, blockModels.modelOutput));
        MultiVariant curved = BlockModelGenerators.plainVariant(ModelTemplates.RAIL_CURVED.create(rail, corner, blockModels.modelOutput));
        MultiVariant risingNE = BlockModelGenerators.plainVariant(ModelTemplates.RAIL_RAISED_NE.create(rail, texture, blockModels.modelOutput));
        MultiVariant risingSW = BlockModelGenerators.plainVariant(ModelTemplates.RAIL_RAISED_SW.create(rail, texture, blockModels.modelOutput));
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(rail).with(PropertyDispatch.initial(BlockStateProperties.RAIL_SHAPE)
                .select(RailShape.NORTH_SOUTH, flat)
                .select(RailShape.EAST_WEST, flat.with(BlockModelGenerators.Y_ROT_90))
                .select(RailShape.ASCENDING_EAST, risingNE.with(BlockModelGenerators.Y_ROT_90))
                .select(RailShape.ASCENDING_WEST, risingSW.with(BlockModelGenerators.Y_ROT_90))
                .select(RailShape.ASCENDING_NORTH, risingNE)
                .select(RailShape.ASCENDING_SOUTH, risingSW)
                .select(RailShape.SOUTH_EAST, curved)
                .select(RailShape.SOUTH_WEST, curved.with(BlockModelGenerators.Y_ROT_90))
                .select(RailShape.NORTH_WEST, curved.with(BlockModelGenerators.Y_ROT_180))
                .select(RailShape.NORTH_EAST, curved.with(BlockModelGenerators.Y_ROT_270))));
        flatItem(blockModels, RailRegistry.WOODEN_RAIL_ITEM.get(), "wooden_rail");

        // Stop and tipple: straight rails, flat or on a slope.
        straight(blockModels, RailRegistry.TUB_STOP.get(), RailRegistry.TUB_STOP_ITEM.get(), "tub_stop", true);
        straight(blockModels, RailRegistry.TIPPLE_RAIL.get(), RailRegistry.TIPPLE_RAIL_ITEM.get(), "tipple_rail", false);

        // Buffer: a hand-made model (rail bed and timber) turned to face the end it closes.
        Identifier buffer = StrataIndustria.id("block/rail_buffer");
        MultiVariant north = BlockModelGenerators.plainVariant(buffer);
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(RailRegistry.RAIL_BUFFER.get()).with(
                PropertyDispatch.initial(RailBufferBlock.FACING)
                        .select(Direction.NORTH, north)
                        .select(Direction.EAST, north.with(BlockModelGenerators.Y_ROT_90))
                        .select(Direction.SOUTH, north.with(BlockModelGenerators.Y_ROT_180))
                        .select(Direction.WEST, north.with(BlockModelGenerators.Y_ROT_270))));
        flatItem(blockModels, RailRegistry.RAIL_BUFFER_ITEM.get(), "rail_buffer");

        itemModels.generateFlatItem(RailRegistry.MINE_TUB.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(RailRegistry.HARNESS.get(), ModelTemplates.FLAT_ITEM);

        // Hay rack and incline winch: hand-made models turned to face.
        MultiVariant empty = BlockModelGenerators.plainVariant(StrataIndustria.id("block/hay_rack"));
        MultiVariant filled = BlockModelGenerators.plainVariant(StrataIndustria.id("block/hay_rack_filled"));
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(RailRegistry.HAY_RACK.get()).with(
                PropertyDispatch.initial(HayRackBlock.FACING, HayRackBlock.FILLED).generate((facing, hay) -> turn(hay ? filled : empty, facing))));
        blockModels.itemModelOutput.accept(RailRegistry.HAY_RACK_ITEM.get(), ItemModelUtils.plainModel(StrataIndustria.id("block/hay_rack")));
        MultiVariant winch = BlockModelGenerators.plainVariant(StrataIndustria.id("block/incline_winch"));
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(RailRegistry.INCLINE_WINCH.get()).with(
                PropertyDispatch.initial(InclineWinchBlock.FACING).generate(facing -> turn(winch, facing))));
        blockModels.itemModelOutput.accept(RailRegistry.INCLINE_WINCH_ITEM.get(), ItemModelUtils.plainModel(StrataIndustria.id("block/incline_winch")));
    }

    /** A model drawn facing north, turned to face {@code facing}. */
    private static MultiVariant turn(MultiVariant north, Direction facing) {
        return switch (facing) {
            case EAST -> north.with(BlockModelGenerators.Y_ROT_90);
            case SOUTH -> north.with(BlockModelGenerators.Y_ROT_180);
            case WEST -> north.with(BlockModelGenerators.Y_ROT_270);
            default -> north;
        };
    }

    private static void straight(BlockModelGenerators blockModels, Block block, Item item, String texture, boolean powered) {
        TextureMapping mapping = TextureMapping.rail(block);
        MultiVariant flat = BlockModelGenerators.plainVariant(ModelTemplates.RAIL_FLAT.create(block, mapping, blockModels.modelOutput));
        MultiVariant risingNE = BlockModelGenerators.plainVariant(ModelTemplates.RAIL_RAISED_NE.create(block, mapping, blockModels.modelOutput));
        MultiVariant risingSW = BlockModelGenerators.plainVariant(ModelTemplates.RAIL_RAISED_SW.create(block, mapping, blockModels.modelOutput));
        PropertyDispatch.C1<MultiVariant, RailShape> shapes = PropertyDispatch.initial(BlockStateProperties.RAIL_SHAPE_STRAIGHT)
                .select(RailShape.NORTH_SOUTH, flat)
                .select(RailShape.EAST_WEST, flat.with(BlockModelGenerators.Y_ROT_90))
                .select(RailShape.ASCENDING_EAST, risingNE.with(BlockModelGenerators.Y_ROT_90))
                .select(RailShape.ASCENDING_WEST, risingSW.with(BlockModelGenerators.Y_ROT_90))
                .select(RailShape.ASCENDING_NORTH, risingNE)
                .select(RailShape.ASCENDING_SOUTH, risingSW);
        if (powered) {
            // The stop has a POWERED state that looks the same.
            blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(block).with(
                    PropertyDispatch.initial(TubStopBlock.POWERED, BlockStateProperties.RAIL_SHAPE_STRAIGHT).generate((on, shape) -> switch (shape) {
                        case NORTH_SOUTH -> flat;
                        case EAST_WEST -> flat.with(BlockModelGenerators.Y_ROT_90);
                        case ASCENDING_EAST -> risingNE.with(BlockModelGenerators.Y_ROT_90);
                        case ASCENDING_WEST -> risingSW.with(BlockModelGenerators.Y_ROT_90);
                        case ASCENDING_NORTH -> risingNE;
                        case ASCENDING_SOUTH -> risingSW;
                        default -> throw new IllegalStateException("not a straight rail shape: " + shape);
                    })));
        } else {
            blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(block).with(shapes));
        }
        flatItem(blockModels, item, texture);
    }

    /** A flat item model that shows the block's own texture, as the vanilla rails do. */
    private static void flatItem(BlockModelGenerators blockModels, Item item, String blockTexture) {
        Identifier model = ModelTemplates.FLAT_ITEM.create(ModelLocationUtils.getModelLocation(item),
                TextureMapping.layer0(new Material(StrataIndustria.id("block/" + blockTexture))), blockModels.modelOutput);
        blockModels.itemModelOutput.accept(item, ItemModelUtils.plainModel(model));
    }

    // ---------------------------------------------------------------- text

    static void lang(BiConsumer<String, String> add) {
        String id = StrataIndustria.MOD_ID;
        add.accept("block." + id + ".wooden_rail", "Wooden Rail");
        add.accept("block." + id + ".tub_stop", "Tub Stop");
        add.accept("block." + id + ".tipple_rail", "Tipple");
        add.accept("block." + id + ".rail_buffer", "Rail Buffer");
        add.accept("item." + id + ".mine_tub", "Mine Tub");
        add.accept("entity." + id + ".mine_tub", "Mine Tub");
        add.accept("block." + id + ".hay_rack", "Hay Rack");
        add.accept("block." + id + ".incline_winch", "Incline Winch");
        add.accept("item." + id + ".harness", "Harness");
        add.accept("entity." + id + ".pony", "Pony");

        add.accept(id + ".pony.leads", "The pony leads. Couple the tubs behind it.");
        add.accept(id + ".pony.untamed", "Only a tame horse will wear it.");
        add.accept(id + ".pony.cannot", "Too young, or busy.");
        add.accept(id + ".pony.no_track", "Stand it beside the wooden rail.");
        add.accept(id + ".pony.hungry", "Pony hungry.");
        add.accept(id + ".pony.no_line", "No line free.");
        add.accept(id + ".pony.balks", "The pony won't take that hill.");
        add.accept(id + ".pony.hay", "Hay in it: %s bales.");
        add.accept(id + ".hay_rack.stock", "Hay: %s bales.");
        add.accept(id + ".winch.hauls", "Winch hauls.");
        add.accept(id + ".winch.lowers", "Winch lets down.");

        add.accept(id + ".tub.coupled", "Coupled.");
        add.accept(id + ".tub.none", "No free tub within reach.");
        add.accept(id + ".tub.following", "That tub is already coupled to one ahead.");
        add.accept(id + ".tub.too_long", "Only %s tubs follow the first.");
        add.accept(id + ".outposts.vanilla_rails", "Line not proven: vanilla rails at %s.");

        add.accept(id + ".stop.name", "Stop name");
        add.accept(id + ".stop.name_hint", "Unnamed");
        add.accept(id + ".stop.station", "Station of %s");
        add.accept(id + ".stop.leaves", "Leaves:");
        add.accept(id + ".stop.rule.wait", "After a wait");
        add.accept(id + ".stop.rule.full", "When full");
        add.accept(id + ".stop.rule.empty", "When empty");
        add.accept(id + ".stop.rule.redstone", "On redstone");
        add.accept(id + ".stop.rule.idle", "When nothing moves");
        add.accept(id + ".stop.seconds", "%s seconds");
        add.accept(id + ".stop.reverse_on", "Turns back here");
        add.accept(id + ".stop.reverse_off", "Runs on through");
        add.accept(id + ".stop.redstone", "A pulse lets it go at once.");
        add.accept(id + ".stop.done", "Done");

        String subtitles = "subtitles." + id + ".";
        add.accept(subtitles + "tub.roll_wood", "Tub rumbles");
        add.accept(subtitles + "tub.couple", "Tubs coupled");
        add.accept(subtitles + "tub.thud", "Tub thuds");
        add.accept(subtitles + "tub_stop.brake", "Brake catches");
        add.accept(subtitles + "tipple.dump", "Tub tips");
        add.accept(subtitles + "pony.harness", "Harness jingles");
        add.accept(subtitles + "pony.step", "Pony clops");
        add.accept(subtitles + "pony.eat", "Pony eats");
        add.accept(subtitles + "pony.snort", "Pony snorts");
        add.accept(subtitles + "winch.haul", "Winch creaks");

        String journal = "journal." + id + ".";
        add.accept(journal + "t3.tramway", "Run a Tub");
        add.accept(journal + "t3.tramway.hint", "Set a tub on wooden rail between two tub stops and let it roll in. Stops hold it until their rule lets it go.");
        add.accept(journal + "t3.tramway.lead", "A handcart is no good on a long haul. What if the load rode on rails?");
        add.accept(journal + "t3.tramway.note", "Plank rails and a tub on wheels. Slow, and it stops short if I don't push, but it keeps to the line.");
        add.accept(journal + "t3.pony", "Harness a Pony");
        add.accept(journal + "t3.pony.hint", "A harness puts a horse on the rails. It wants hay at the stops.");
        add.accept(journal + "t3.pony.lead", "Pushing tubs this far is no good.");
        add.accept(journal + "t3.pony.note", "Put the old horse on the tramway. Two trips a bale.");
        add.accept(journal + "t3.incline", "Haul up an Incline");
        add.accept(journal + "t3.incline.hint", "A winch at the top of a slope hauls tubs up on its rope. Turn it with a shaft.");
        add.accept(journal + "t3.incline.lead", "The pony won't take that hill.");
        add.accept(journal + "t3.incline.note", "Winch at the top, rope to the tub. Shaft turning, it climbs. Shaft stopped, it stays put.");
        add.accept(journal + "observe.hungry_pony", "Pony stopped at the stop. No hay left.");
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
            Item plate = ModItems.PLATES.get(Metal.WROUGHT_IRON).get();
            shaped(RecipeCategory.TRANSPORTATION, RailRegistry.WOODEN_RAIL_ITEM.get(), 12)
                    .pattern("P P")
                    .pattern("PSP")
                    .pattern("P P")
                    .define('P', ItemTags.PLANKS)
                    .define('S', Items.STICK)
                    .unlockedBy("has_planks", has(ItemTags.PLANKS))
                    .save(output, key("wooden_rail"));
            shapeless(RecipeCategory.TRANSPORTATION, RailRegistry.TUB_STOP_ITEM.get())
                    .requires(RailRegistry.WOODEN_RAIL_ITEM.get())
                    .requires(ModItems.WOODEN_GEAR.get())
                    .requires(ItemTags.PLANKS)
                    .unlockedBy("has_wooden_rail", has(RailRegistry.WOODEN_RAIL_ITEM.get()))
                    .save(output, key("tub_stop"));
            shaped(RecipeCategory.TRANSPORTATION, RailRegistry.TIPPLE_RAIL_ITEM.get())
                    .pattern("W W")
                    .pattern(" R ")
                    .pattern("PPP")
                    .define('W', plate)
                    .define('R', RailRegistry.WOODEN_RAIL_ITEM.get())
                    .define('P', ItemTags.PLANKS)
                    .unlockedBy("has_wooden_rail", has(RailRegistry.WOODEN_RAIL_ITEM.get()))
                    .save(output, key("tipple_rail"));
            shaped(RecipeCategory.TRANSPORTATION, RailRegistry.RAIL_BUFFER_ITEM.get(), 2)
                    .pattern("P P")
                    .pattern("PPP")
                    .define('P', ItemTags.PLANKS)
                    .unlockedBy("has_wooden_rail", has(RailRegistry.WOODEN_RAIL_ITEM.get()))
                    .save(output, key("rail_buffer"));
            shaped(RecipeCategory.TRANSPORTATION, RailRegistry.HARNESS.get())
                    .pattern("L L")
                    .pattern("LRL")
                    .pattern(" I ")
                    .define('L', Items.LEATHER)
                    .define('R', dev.strataindustria.transport.foot.FootRegistry.ROPE.get())
                    .define('I', ModItems.WROUGHT_IRON_ROD.get())
                    .unlockedBy("has_leather", has(Items.LEATHER))
                    .save(output, key("harness"));
            shaped(RecipeCategory.TRANSPORTATION, RailRegistry.HAY_RACK_ITEM.get())
                    .pattern("S S")
                    .pattern("PSP")
                    .pattern("PPP")
                    .define('S', Items.STICK)
                    .define('P', ItemTags.PLANKS)
                    .unlockedBy("has_hay_block", has(Items.HAY_BLOCK))
                    .save(output, key("hay_rack"));
            shaped(RecipeCategory.TRANSPORTATION, RailRegistry.INCLINE_WINCH_ITEM.get())
                    .pattern("R R")
                    .pattern("GAG")
                    .pattern("PWP")
                    .define('R', dev.strataindustria.transport.foot.FootRegistry.ROPE.get())
                    .define('G', ModItems.WOODEN_GEAR.get())
                    .define('A', ModItems.WOODEN_AXLE.get())
                    .define('P', ItemTags.PLANKS)
                    .define('W', plate)
                    .unlockedBy("has_wooden_rail", has(RailRegistry.WOODEN_RAIL_ITEM.get()))
                    .save(output, key("incline_winch"));
            shaped(RecipeCategory.TRANSPORTATION, RailRegistry.MINE_TUB.get())
                    .pattern("W W")
                    .pattern("WPW")
                    .pattern(" I ")
                    .define('W', plate)
                    .define('P', ItemTags.PLANKS)
                    .define('I', ModItems.WROUGHT_IRON_ROD.get())
                    .unlockedBy("has_wrought_iron_plate", has(plate))
                    .save(output, key("mine_tub"));
        }
    }
}
