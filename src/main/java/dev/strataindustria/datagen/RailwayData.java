package dev.strataindustria.datagen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.material.Metal;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.Tier4Items;
import dev.strataindustria.transport.rail.CoalStageBlock;
import dev.strataindustria.transport.rail.RailBufferBlock;
import dev.strataindustria.transport.rail.RailwayRegistry;
import dev.strataindustria.transport.rail.WagonFluidPortBlock;
import dev.strataindustria.transport.rail.WaterTowerSpoutBlock;
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

        // Water tower and coal stage: hand-made models; the spout turns to face its engine and swings out while it pours.
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(RailwayRegistry.WATER_TOWER_BASE.get(),
                BlockModelGenerators.plainVariant(StrataIndustria.id("block/water_tower_base"))));
        blockModels.itemModelOutput.accept(RailwayRegistry.WATER_TOWER_BASE_ITEM.get(), ItemModelUtils.plainModel(StrataIndustria.id("block/water_tower_base")));
        MultiVariant stowed = BlockModelGenerators.plainVariant(StrataIndustria.id("block/water_tower_spout"));
        MultiVariant out = BlockModelGenerators.plainVariant(StrataIndustria.id("block/water_tower_spout_out"));
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(RailwayRegistry.WATER_TOWER_SPOUT.get()).with(
                PropertyDispatch.initial(WaterTowerSpoutBlock.FACING, WaterTowerSpoutBlock.POURING)
                        .generate((facing, pouring) -> RailData.turn(pouring ? out : stowed, facing))));
        blockModels.itemModelOutput.accept(RailwayRegistry.WATER_TOWER_SPOUT_ITEM.get(), ItemModelUtils.plainModel(StrataIndustria.id("block/water_tower_spout")));
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(RailwayRegistry.COAL_STAGE.get()).with(
                PropertyDispatch.initial(CoalStageBlock.LOADING)
                        .select(false, BlockModelGenerators.plainVariant(StrataIndustria.id("block/coal_stage")))
                        .select(true, BlockModelGenerators.plainVariant(StrataIndustria.id("block/coal_stage_open")))));
        blockModels.itemModelOutput.accept(RailwayRegistry.COAL_STAGE_ITEM.get(), ItemModelUtils.plainModel(StrataIndustria.id("block/coal_stage")));

        itemModels.generateFlatItem(RailwayRegistry.STEAM_LOCOMOTIVE.get(), ModelTemplates.FLAT_ITEM);
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
        add.accept("block." + id + ".water_tower_base", "Water Tower Base");
        add.accept("block." + id + ".water_tower_spout", "Water Tower Spout");
        add.accept("block." + id + ".coal_stage", "Coal Stage");
        add.accept("item." + id + ".steam_locomotive", "Steam Locomotive");
        add.accept("entity." + id + ".steam_locomotive", "Steam Locomotive");
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

        add.accept(id + ".locomotive.hud", "Throttle %s  Steam %s%%  Water %s mB  Fuel %s");
        add.accept(id + ".locomotive.dry", "Boiler dry.");
        add.accept(id + ".locomotive.low_water", "Low water.");
        add.accept(id + ".locomotive.low_fuel", "Low fuel.");
        add.accept(id + ".locomotive.steam_up", "Steam up.");
        add.accept(id + ".locomotive.parked", "Brakes on.");
        add.accept(id + ".locomotive.released", "Brakes off.");
        add.accept(id + ".locomotive.leads", "The engine leads. Couple the wagons behind it.");
        add.accept(id + ".locomotive.full_tank", "Tank is full.");
        add.accept(id + ".locomotive.full_fuel", "Firebox is full.");
        add.accept(id + ".water_tower.holds", "%s of %s mB.");
        add.accept(id + ".water_tower.no_tank", "No tank on the trestle.");
        add.accept("key." + id + ".whistle", "Blow Whistle");

        String subtitles = "subtitles." + id + ".";
        add.accept(subtitles + "locomotive.chuff", "Engine chuffs");
        add.accept(subtitles + "locomotive.whistle", "Whistle blows");
        add.accept(subtitles + "locomotive.brake", "Brakes squeal");
        add.accept(subtitles + "water_tower.pour", "Water pours");
        add.accept(subtitles + "coal_stage.load", "Coal rattles");
        add.accept(subtitles + "rail.clatter_steel", "Wheels clatter");
        add.accept(subtitles + "steel_buffer.clang", "Buffer clangs");
        add.accept(subtitles + "wagon_port.flow", "Fluid flows");
        add.accept(subtitles + "flat_wagon.load", "Load thumps");

        String journal = "journal." + id + ".";
        add.accept(journal + "t4.locomotive", "Raise Steam on the Line");
        add.accept(journal + "t4.locomotive.hint", "Coal in the firebox door, water in the tank. Give it a couple of minutes and it comes up to pressure.");
        add.accept(journal + "t4.locomotive.lead", "A pony and wooden rail won't do for the long haul.");
        add.accept(journal + "t4.locomotive.note", "Engine's up to full pressure. Whistle works.");
        add.accept(journal + "t4.railway", "Run a Driverless Train");
        add.accept(journal + "t4.railway.hint", "Station track at each end, a water tower and a coal stage at home. Take the brake off with an empty hand and sneak, and leave it. Three round trips.");
        add.accept(journal + "t4.railway.lead", "Can't sit on this engine all day.");
        add.accept(journal + "t4.railway.note", "Runs by itself. Water and coal at home, that's all it wants.");
        add.accept(journal + "t4.outpost_5", "Grow an Outpost");
        add.accept(journal + "t4.outpost_5.hint", "A locomotive run over steel track the whole way, between two charters, gives the outpost more ground.");
        add.accept(journal + "t4.outpost_5.lead", "Wooden rail keeps the place small.");
        add.accept(journal + "t4.outpost_5.note", "Steel track all the way. The outpost loads wider now.");
        add.accept(journal + "observe.boiler_dry", "Engine ran dry halfway. Need a water stop.");
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
            shaped(RecipeCategory.TRANSPORTATION, RailwayRegistry.STEAM_LOCOMOTIVE.get())
                    .pattern("EpE")
                    .pattern("BFB")
                    .pattern("DgD")
                    .define('E', plate)
                    .define('p', Tier4Items.PRESSURE_GAUGE.get())
                    .define('B', Tier4Items.BRONZE_BOILER.get())
                    .define('F', Tier4Items.FIREBOX.get())
                    .define('D', rod)
                    .define('g', ModItems.GEARS.get(Metal.STEEL).get())
                    .unlockedBy("has_steel_track", has(RailwayRegistry.STEEL_TRACK_ITEM.get()))
                    .save(output, key("steam_locomotive"));
            shaped(RecipeCategory.TRANSPORTATION, RailwayRegistry.WATER_TOWER_BASE_ITEM.get())
                    .pattern("XXX")
                    .pattern("XpX")
                    .pattern("X X")
                    .define('X', planks)
                    .define('p', Tier4Items.STEEL_FLUID_PIPE.get())
                    .unlockedBy("has_steel_track", has(RailwayRegistry.STEEL_TRACK_ITEM.get()))
                    .save(output, key("water_tower_base"));
            shaped(RecipeCategory.TRANSPORTATION, RailwayRegistry.WATER_TOWER_SPOUT_ITEM.get())
                    .pattern("E E")
                    .pattern(" p ")
                    .define('E', plate)
                    .define('p', Tier4Items.STEEL_FLUID_PIPE.get())
                    .unlockedBy("has_steel_track", has(RailwayRegistry.STEEL_TRACK_ITEM.get()))
                    .save(output, key("water_tower_spout"));
            shaped(RecipeCategory.TRANSPORTATION, RailwayRegistry.COAL_STAGE_ITEM.get())
                    .pattern("X X")
                    .pattern("XcX")
                    .pattern("XXX")
                    .define('X', planks)
                    .define('c', Tier4Items.CHUTE.get())
                    .unlockedBy("has_steel_track", has(RailwayRegistry.STEEL_TRACK_ITEM.get()))
                    .save(output, key("coal_stage"));
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
