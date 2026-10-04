package dev.strataindustria.datagen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.material.Metal;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.Tier4Items;
import dev.strataindustria.transport.rail.RailBufferBlock;
import dev.strataindustria.transport.rail.RailwayRegistry;
import dev.strataindustria.transport.rail.WagonFluidPortBlock;
import java.util.function.BiConsumer;
import net.minecraft.advancements.Advancement;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.RailShape;

/** Models, text and recipes for the steel track and wagons (outposts and transport spec 7.1 and 7.2): {@link RailwayRegistry}. */
final class RailwayData {
    private RailwayData() {}

    // ---------------------------------------------------------------- models

    static void models(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        // Steel track: the vanilla rail's models with our textures, as the wooden rail has.
        Block rail = RailwayRegistry.STEEL_TRACK.get();
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
        RailData.flatItem(blockModels, RailwayRegistry.STEEL_TRACK_ITEM.get(), "steel_track");

        RailData.straight(blockModels, RailwayRegistry.STATION_TRACK.get(), RailwayRegistry.STATION_TRACK_ITEM.get(), "station_track", true);

        Identifier buffer = StrataIndustria.id("block/steel_buffer");
        MultiVariant north = BlockModelGenerators.plainVariant(buffer);
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(RailwayRegistry.STEEL_BUFFER.get()).with(
                PropertyDispatch.initial(RailBufferBlock.FACING)
                        .select(Direction.NORTH, north)
                        .select(Direction.EAST, north.with(BlockModelGenerators.Y_ROT_90))
                        .select(Direction.SOUTH, north.with(BlockModelGenerators.Y_ROT_180))
                        .select(Direction.WEST, north.with(BlockModelGenerators.Y_ROT_270))));
        RailData.flatItem(blockModels, RailwayRegistry.STEEL_BUFFER_ITEM.get(), "steel_buffer");

        // Wagon fluid port: a flanged box, blue tab to fill the wagon and white tab to empty it.
        Block port = RailwayRegistry.WAGON_FLUID_PORT.get();
        MultiVariant load = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_ALL.createWithSuffix(port, "_load",
                TextureMapping.singleSlot(TextureSlot.ALL, TextureMapping.getBlockTexture(port, "_load")), blockModels.modelOutput));
        MultiVariant unload = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_ALL.createWithSuffix(port, "_unload",
                TextureMapping.singleSlot(TextureSlot.ALL, TextureMapping.getBlockTexture(port, "_unload")), blockModels.modelOutput));
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(port).with(PropertyDispatch.initial(WagonFluidPortBlock.MODE)
                .select(WagonFluidPortBlock.Mode.LOAD, load)
                .select(WagonFluidPortBlock.Mode.UNLOAD, unload)));
        blockModels.itemModelOutput.accept(RailwayRegistry.WAGON_FLUID_PORT_ITEM.get(), ItemModelUtils.plainModel(StrataIndustria.id("block/wagon_fluid_port_load")));

        itemModels.generateFlatItem(RailwayRegistry.ORE_WAGON.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(RailwayRegistry.TANK_WAGON.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(RailwayRegistry.FLAT_WAGON.get(), ModelTemplates.FLAT_ITEM);
    }

    // ---------------------------------------------------------------- text

    static void lang(BiConsumer<String, String> add) {
        String id = StrataIndustria.MOD_ID;
        add.accept("block." + id + ".steel_track", "Steel Track");
        add.accept("block." + id + ".station_track", "Station Track");
        add.accept("block." + id + ".steel_buffer", "Steel Buffer");
        add.accept("block." + id + ".wagon_fluid_port", "Wagon Fluid Port");
        add.accept("item." + id + ".ore_wagon", "Ore Wagon");
        add.accept("item." + id + ".tank_wagon", "Tank Wagon");
        add.accept("item." + id + ".flat_wagon", "Flat Wagon");
        add.accept("entity." + id + ".ore_wagon", "Ore Wagon");
        add.accept("entity." + id + ".tank_wagon", "Tank Wagon");
        add.accept("entity." + id + ".flat_wagon", "Flat Wagon");

        add.accept(id + ".tank_wagon.empty", "Empty. Holds %s mB.");
        add.accept(id + ".tank_wagon.holds", "%s of %s mB, %s.");
        add.accept(id + ".wagon_port.load", "Fills the wagon.");
        add.accept(id + ".wagon_port.unload", "Empties the wagon.");
        add.accept(id + ".flat_wagon.lifted", "On the wagon.");
        add.accept(id + ".flat_wagon.set_down", "Set down.");
        add.accept(id + ".flat_wagon.no_room", "No room beside it.");
        add.accept(id + ".flat_wagon.too_fixed", "That won't come away.");

        String subtitles = "subtitles." + id + ".";
        add.accept(subtitles + "rail.clatter_steel", "Wheels clatter");
        add.accept(subtitles + "steel_buffer.clang", "Buffer clangs");
        add.accept(subtitles + "wagon_port.flow", "Fluid flows");
        add.accept(subtitles + "flat_wagon.load", "Load thumps");
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
            Item plate = ModItems.PLATES.get(Metal.STEEL).get();
            Item rod = ModItems.RODS.get(Metal.STEEL).get();
            Item planks = Tier4Items.TREATED_PLANKS.get();
            shaped(RecipeCategory.TRANSPORTATION, RailwayRegistry.STEEL_TRACK_ITEM.get(), 16)
                    .pattern("D D")
                    .pattern("DXD")
                    .pattern("D D")
                    .define('D', rod)
                    .define('X', planks)
                    .unlockedBy("has_steel_rod", has(rod))
                    .save(output, key("steel_track"));
            shapeless(RecipeCategory.TRANSPORTATION, RailwayRegistry.STATION_TRACK_ITEM.get())
                    .requires(RailwayRegistry.STEEL_TRACK_ITEM.get())
                    .requires(ModItems.GEARS.get(Metal.BRASS).get())
                    .requires(planks)
                    .unlockedBy("has_steel_track", has(RailwayRegistry.STEEL_TRACK_ITEM.get()))
                    .save(output, key("station_track"));
            shaped(RecipeCategory.TRANSPORTATION, RailwayRegistry.STEEL_BUFFER_ITEM.get(), 2)
                    .pattern("E E")
                    .pattern("XXX")
                    .define('E', plate)
                    .define('X', planks)
                    .unlockedBy("has_steel_track", has(RailwayRegistry.STEEL_TRACK_ITEM.get()))
                    .save(output, key("steel_buffer"));
            shaped(RecipeCategory.TRANSPORTATION, RailwayRegistry.ORE_WAGON.get())
                    .pattern("E E")
                    .pattern("EXE")
                    .pattern(" D ")
                    .define('E', plate)
                    .define('X', planks)
                    .define('D', rod)
                    .unlockedBy("has_steel_track", has(RailwayRegistry.STEEL_TRACK_ITEM.get()))
                    .save(output, key("ore_wagon"));
            shaped(RecipeCategory.TRANSPORTATION, RailwayRegistry.TANK_WAGON.get())
                    .pattern("E E")
                    .pattern("ETE")
                    .pattern(" D ")
                    .define('E', plate)
                    .define('T', Tier4Items.FLUID_TANK.get())
                    .define('D', rod)
                    .unlockedBy("has_steel_track", has(RailwayRegistry.STEEL_TRACK_ITEM.get()))
                    .save(output, key("tank_wagon"));
            shaped(RecipeCategory.TRANSPORTATION, RailwayRegistry.FLAT_WAGON.get())
                    .pattern("XXX")
                    .pattern(" D ")
                    .define('X', planks)
                    .define('D', rod)
                    .unlockedBy("has_steel_track", has(RailwayRegistry.STEEL_TRACK_ITEM.get()))
                    .save(output, key("flat_wagon"));
            shaped(RecipeCategory.TRANSPORTATION, RailwayRegistry.WAGON_FLUID_PORT_ITEM.get())
                    .pattern("E E")
                    .pattern(" p ")
                    .pattern("E E")
                    .define('E', plate)
                    .define('p', Tier4Items.STEEL_FLUID_PIPE.get())
                    .unlockedBy("has_steel_track", has(RailwayRegistry.STEEL_TRACK_ITEM.get()))
                    .save(output, key("wagon_fluid_port"));
        }
    }
}
