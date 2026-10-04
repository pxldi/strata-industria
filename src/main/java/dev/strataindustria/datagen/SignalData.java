package dev.strataindustria.datagen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.material.Metal;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.Tier5Items;
import dev.strataindustria.transport.signal.RouteSwitchBlock;
import dev.strataindustria.transport.signal.SignalBlock;
import dev.strataindustria.transport.signal.SignalRegistry;
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
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.RailShape;

/** Models, text and recipes for the block signal, timetable and route switch (outposts and transport spec 9.4): {@link SignalRegistry}. */
final class SignalData {
    private SignalData() {}

    static void models(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        // The signal's two hand-modelled states face north; the blockstate turns them.
        MultiVariant stop = BlockModelGenerators.plainVariant(StrataIndustria.id("block/block_signal_stop"));
        MultiVariant clear = BlockModelGenerators.plainVariant(StrataIndustria.id("block/block_signal_clear"));
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(SignalRegistry.BLOCK_SIGNAL.get()).with(
                PropertyDispatch.initial(SignalBlock.CLEAR, SignalBlock.FACING)
                        .select(false, Direction.NORTH, stop)
                        .select(false, Direction.EAST, stop.with(BlockModelGenerators.Y_ROT_90))
                        .select(false, Direction.SOUTH, stop.with(BlockModelGenerators.Y_ROT_180))
                        .select(false, Direction.WEST, stop.with(BlockModelGenerators.Y_ROT_270))
                        .select(true, Direction.NORTH, clear)
                        .select(true, Direction.EAST, clear.with(BlockModelGenerators.Y_ROT_90))
                        .select(true, Direction.SOUTH, clear.with(BlockModelGenerators.Y_ROT_180))
                        .select(true, Direction.WEST, clear.with(BlockModelGenerators.Y_ROT_270))));
        blockModels.itemModelOutput.accept(SignalRegistry.BLOCK_SIGNAL_ITEM.get(),
                ItemModelUtils.plainModel(StrataIndustria.id("block/block_signal_stop")));

        // The route switch: the vanilla rail's flat and curved models with its own textures, the lamp white straight and amber across.
        Block rail = SignalRegistry.ROUTE_SWITCH.get();
        TextureMapping texture = TextureMapping.rail(rail);
        TextureMapping corner = TextureMapping.rail(TextureMapping.getBlockTexture(rail, "_corner"));
        MultiVariant flat = BlockModelGenerators.plainVariant(ModelTemplates.RAIL_FLAT.create(rail, texture, blockModels.modelOutput));
        MultiVariant curved = BlockModelGenerators.plainVariant(ModelTemplates.RAIL_CURVED.create(rail, corner, blockModels.modelOutput));
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(rail).with(PropertyDispatch.initial(RouteSwitchBlock.SHAPE)
                .select(RailShape.NORTH_SOUTH, flat)
                .select(RailShape.EAST_WEST, flat.with(BlockModelGenerators.Y_ROT_90))
                .select(RailShape.ASCENDING_EAST, flat.with(BlockModelGenerators.Y_ROT_90))
                .select(RailShape.ASCENDING_WEST, flat.with(BlockModelGenerators.Y_ROT_90))
                .select(RailShape.ASCENDING_NORTH, flat)
                .select(RailShape.ASCENDING_SOUTH, flat)
                .select(RailShape.SOUTH_EAST, curved)
                .select(RailShape.SOUTH_WEST, curved.with(BlockModelGenerators.Y_ROT_90))
                .select(RailShape.NORTH_WEST, curved.with(BlockModelGenerators.Y_ROT_180))
                .select(RailShape.NORTH_EAST, curved.with(BlockModelGenerators.Y_ROT_270))));
        RailData.flatItem(blockModels, SignalRegistry.ROUTE_SWITCH_ITEM.get(), "route_switch");

        itemModels.generateFlatItem(SignalRegistry.TIMETABLE.get(), ModelTemplates.FLAT_ITEM);
    }

    static void lang(BiConsumer<String, String> add) {
        String id = StrataIndustria.MOD_ID;
        add.accept("block." + id + ".block_signal", "Block Signal");
        add.accept("block." + id + ".route_switch", "Route Switch");
        add.accept("item." + id + ".timetable", "Timetable");

        add.accept(id + ".signal.no_track", "No track on its left. It watches the rail beside it, looking the way it faces.");
        add.accept(id + ".signal.clear", "Block clear.");
        add.accept(id + ".signal.occupied", "Block occupied.");
        add.accept(id + ".signal.waiting", "Waiting at signal.");

        String key = id + ".timetable.";
        add.accept(key + "blank", "Blank. Write stops on it, then use it on an engine, pony or tram.");
        add.accept(key + "stop_rule", "Stop's rule");
        add.accept(key + "stops_in_order", "Stops, in order");
        add.accept(key + "branch_for", "Branch for");
        add.accept(key + "name_hint", "stop name");
        add.accept(key + "line", "Line %s");
        add.accept(key + "done", "Done");
        add.accept(key + "loaded", "Timetable: %s.");
        add.accept(key + "cleared", "Timetable cleared.");
        add.accept(key + "lead_only", "Use it on the lead.");
        add.accept(key + "no_mind", "Nothing here to follow a timetable. A pony, engine or tram will.");
        add.accept(key + "arrived", "%s. Next: %s.");

        String subtitles = "subtitles." + id + ".";
        add.accept(subtitles + "signal.arm", "Signal clunks");
        add.accept(subtitles + "route_switch.throw", "Switch thrown");
        add.accept(subtitles + "timetable.load", "Timetable clipped on");
        add.accept(subtitles + "timetable.arrive", "Stop chimes");

        String journal = "journal." + id + ".";
        add.accept(journal + "t5.timetable", "Keep a Timetable");
        add.accept(journal + "t5.timetable.hint", "Write three stops on a timetable and clip it onto a tram. It stands at each in turn and starts over.");
        add.accept(journal + "t5.timetable.lead", "A tram goes where the track goes. Nobody tells it which stop.");
        add.accept(journal + "t5.timetable.note", "Tram ran the three stops in order and came round again. No one on the footplate.");
    }

    static final class Recipes extends net.minecraft.data.recipes.RecipeProvider {
        Recipes(BootstrapContext<Recipe<?>> recipes, BootstrapContext<Advancement> advancements) {
            super(recipes, advancements);
        }

        private static ResourceKey<Recipe<?>> key(String path) {
            return ResourceKey.create(Registries.RECIPE, StrataIndustria.id(path));
        }

        @Override
        protected void buildRecipes() {
            Item rod = ModItems.RODS.get(Metal.STEEL).get();
            shaped(RecipeCategory.TRANSPORTATION, SignalRegistry.BLOCK_SIGNAL_ITEM.get(), 2)
                    .pattern(" r ")
                    .pattern(" D ")
                    .pattern(" D ")
                    .define('r', Items.REDSTONE_LAMP)
                    .define('D', rod)
                    .unlockedBy("has_basic_circuit", has(Tier5Items.BASIC_CIRCUIT.get()))
                    .save(output, key("block_signal"));
            shapeless(RecipeCategory.TRANSPORTATION, SignalRegistry.TIMETABLE.get())
                    .requires(Items.PAPER)
                    .requires(Tier5Items.BASIC_CIRCUIT.get())
                    .unlockedBy("has_basic_circuit", has(Tier5Items.BASIC_CIRCUIT.get()))
                    .save(output, key("timetable"));
            shapeless(RecipeCategory.TRANSPORTATION, SignalRegistry.ROUTE_SWITCH_ITEM.get())
                    .requires(dev.strataindustria.transport.rail.RailwayRegistry.STEEL_TRACK_ITEM.get())
                    .requires(Tier5Items.BASIC_CIRCUIT.get())
                    .requires(Items.COMPARATOR)
                    .unlockedBy("has_basic_circuit", has(Tier5Items.BASIC_CIRCUIT.get()))
                    .save(output, key("route_switch"));
        }
    }
}
