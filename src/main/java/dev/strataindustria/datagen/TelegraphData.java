package dev.strataindustria.datagen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.material.Metal;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.Tier4Items;
import dev.strataindustria.registry.Tier5Items;
import dev.strataindustria.transport.telegraph.DispatchBoardBlock;
import dev.strataindustria.transport.telegraph.TelegraphKeyBlock;
import dev.strataindustria.transport.telegraph.TelegraphRegistry;
import dev.strataindustria.transport.telegraph.TelegraphSounderBlock;
import java.util.function.BiConsumer;
import net.minecraft.advancements.Advancement;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;

/** Models, text and recipes for the telegraph and the dispatch board (outposts and transport spec 9.1): {@link TelegraphRegistry}. */
final class TelegraphData {
    private TelegraphData() {}

    // ---------------------------------------------------------------- models

    static void models(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        // The key, the sounder and the board are hand-made models (resources/models/block); the key and the sounder have a
        // resting and a worked pose, the board hangs on whichever wall it was put on.
        PropertyDispatch.C2<net.minecraft.client.data.models.MultiVariant, Direction, Boolean> key = PropertyDispatch.initial(TelegraphKeyBlock.FACING, TelegraphKeyBlock.PRESSED);
        PropertyDispatch.C2<net.minecraft.client.data.models.MultiVariant, Direction, Boolean> sounder = PropertyDispatch.initial(TelegraphSounderBlock.FACING, TelegraphSounderBlock.POWERED);
        for (boolean down : new boolean[] {false, true}) {
            for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
                key.select(facing, down, RailData.turn(BlockModelGenerators.plainVariant(StrataIndustria.id("block/telegraph_key" + (down ? "_pressed" : ""))), facing));
                sounder.select(facing, down, RailData.turn(BlockModelGenerators.plainVariant(StrataIndustria.id("block/telegraph_sounder" + (down ? "_down" : ""))), facing));
            }
        }
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(TelegraphRegistry.KEY.get()).with(key));
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(TelegraphRegistry.SOUNDER.get()).with(sounder));
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(TelegraphRegistry.BOARD.get()).with(
                PropertyDispatch.initial(DispatchBoardBlock.FACING).generate(facing ->
                        RailData.turn(BlockModelGenerators.plainVariant(StrataIndustria.id("block/dispatch_board")), facing))));
        blockModels.itemModelOutput.accept(TelegraphRegistry.KEY_ITEM.get(), ItemModelUtils.plainModel(StrataIndustria.id("block/telegraph_key")));
        blockModels.itemModelOutput.accept(TelegraphRegistry.SOUNDER_ITEM.get(), ItemModelUtils.plainModel(StrataIndustria.id("block/telegraph_sounder")));
        blockModels.itemModelOutput.accept(TelegraphRegistry.BOARD_ITEM.get(), ItemModelUtils.plainModel(StrataIndustria.id("block/dispatch_board")));
        itemModels.generateFlatItem(TelegraphRegistry.WIRE.get(), ModelTemplates.FLAT_ITEM);
    }

    // ---------------------------------------------------------------- text

    static void lang(BiConsumer<String, String> add) {
        String id = StrataIndustria.MOD_ID;
        add.accept("block." + id + ".telegraph_key", "Telegraph Key");
        add.accept("block." + id + ".telegraph_sounder", "Telegraph Sounder");
        add.accept("block." + id + ".dispatch_board", "Dispatch Board");
        add.accept("item." + id + ".telegraph_wire", "Telegraph Wire");

        String wire = id + ".telegraph.wire.";
        add.accept(wire + "started", "Wire made fast. Now the next insulator.");
        add.accept(wire + "joined", "Strung. Used %s wire.");
        add.accept(wire + "needs", "Need %s wire for that span.");
        add.accept(wire + "not_insulator", "Insulators only.");
        add.accept(wire + "already", "Already wired.");
        add.accept(wire + "too_far", "Too far. %s blocks at most.");
        add.accept(wire + "full", "No room for another wire there.");
        add.accept(wire + "blocked", "Something solid in the way.");
        String key = id + ".telegraph.key.";
        add.accept(key + "sent", "Sent. %s sounders on the line.");
        add.accept(key + "silent", "Nobody on the line.");
        add.accept(key + "no_pole", "No pole within %2$s blocks. Put an insulator closer.");

        String dispatch = id + ".dispatch.";
        add.accept(dispatch + "done", "Done");
        add.accept(dispatch + "no_pole", "No pole within reach. Not on the line.");
        add.accept(dispatch + "empty", "Nobody has reported.");
        add.accept(dispatch + "empty_hint", "A key at each outpost, on the same wire.");
        add.accept(dispatch + "more", "and %s more");
        add.accept(dispatch + "state.0", "loaded");
        add.accept(dispatch + "state.1", "idle");
        add.accept(dispatch + "state.2", "no line");
        add.accept(dispatch + "state.3", "asleep");
        add.accept(dispatch + "state.4", "squeezed");
        add.accept(dispatch + "state.short.0", "loaded");
        add.accept(dispatch + "state.short.1", "idle");
        add.accept(dispatch + "state.short.2", "no line");
        add.accept(dispatch + "state.short.3", "asleep");
        add.accept(dispatch + "state.short.4", "squeezed");
        add.accept(dispatch + "line.open", "%s open");
        add.accept(dispatch + "line.cut", "%s cut");
        add.accept(dispatch + "line.telegraph_only", "telegraph only");
        add.accept(dispatch + "line.none", "no line");
        add.accept(dispatch + "join", "%s, %s");
        add.accept(dispatch + "crate", "crate %s%%");
        add.accept(dispatch + "silent", "no word for %s min");
        add.accept(dispatch + "just_now", "%s just now");
        add.accept(dispatch + "minutes_ago", "%s %s min ago");
        add.accept(dispatch + "hours_ago", "%s %s h ago");
        add.accept(dispatch + "last.tramway", "last tub");
        add.accept(dispatch + "last.railway", "last train");
        add.accept(dispatch + "last.railway_mixed", "last train");
        add.accept(dispatch + "last.ropeway", "last bucket");
        add.accept(dispatch + "last.tram", "last tram");
        add.accept(dispatch + "last.power", "last load");
        add.accept(dispatch + "last.telegraph", "last call");
        add.accept(dispatch + "last.pipeline", "last pig");

        String subtitles = "subtitles." + id + ".";
        add.accept(subtitles + "telegraph.key", "Key clicks");
        add.accept(subtitles + "telegraph.key_up", "Key springs back");
        add.accept(subtitles + "telegraph.sounder", "Sounder clacks");
        add.accept(subtitles + "telegraph.sounder_lift", "Sounder ticks");
        add.accept(subtitles + "telegraph.wire_strung", "Wire made fast");
        add.accept(subtitles + "telegraph.wire_snap", "Wire parts");
        add.accept(subtitles + "telegraph.drop_hung", "Drop wire clips on");
        add.accept(subtitles + "telegraph.line_open", "Line comes alive");
        add.accept(subtitles + "dispatch_board.update", "Chalk scratches");

        String journal = "journal." + id + ".";
        add.accept(journal + "t5.telegraph", "Send a Telegram");
        add.accept(journal + "t5.telegraph.hint", "Telegraph wire goes on the same poles. A key at one charter, a sounder at the other. Press the key.");
        add.accept(journal + "t5.telegraph.lead", "No way of knowing how the outposts are doing without riding out.");
        add.accept(journal + "t5.telegraph.note", "Key down at home and the sounder clacked at the outpost. Quicker than a pony.");
        add.accept(journal + "t5.dispatch", "Watch the Board");
        add.accept(journal + "t5.dispatch.hint", "A key at each outpost on the same wire as a dispatch board at home. The board lists every outpost that reports in.");
        add.accept(journal + "t5.dispatch.lead", "A key tells me they're alive. It doesn't tell me how they're doing.");
        add.accept(journal + "t5.dispatch.note", "Board shows every outpost. Saves a lot of riding.");
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
            Item brass = ModItems.PLATES.get(Metal.BRASS).get();
            Item copper = Tier5Items.COPPER_WIRE.get();
            Item ironPlate = ModItems.PLATES.get(Metal.WROUGHT_IRON).get();
            Item treated = Tier4Items.TREATED_PLANKS.get();
            shapeless(RecipeCategory.REDSTONE, TelegraphRegistry.WIRE.get(), 4)
                    .requires(Tier5Items.STEEL_WIRE.get(), 2)
                    .requires(Tier5Items.CERAMIC_INSULATOR.get())
                    .unlockedBy("has_pole_insulator", has(Tier5Items.POLE_INSULATOR.get()))
                    .save(output, key("telegraph_wire"));
            shaped(RecipeCategory.REDSTONE, TelegraphRegistry.KEY_ITEM.get())
                    .pattern(" b ")
                    .pattern("CWC")
                    .pattern("PPP")
                    .define('b', brass)
                    .define('C', copper)
                    .define('W', ironPlate)
                    .define('P', net.minecraft.tags.ItemTags.PLANKS)
                    .unlockedBy("has_pole_insulator", has(Tier5Items.POLE_INSULATOR.get()))
                    .save(output, key("telegraph_key"));
            shaped(RecipeCategory.REDSTONE, TelegraphRegistry.SOUNDER_ITEM.get())
                    .pattern(" b ")
                    .pattern("CMC")
                    .pattern("PPP")
                    .define('b', brass)
                    .define('C', copper)
                    .define('M', Tier5Items.MAGNET.get())
                    .define('P', net.minecraft.tags.ItemTags.PLANKS)
                    .unlockedBy("has_pole_insulator", has(Tier5Items.POLE_INSULATOR.get()))
                    .save(output, key("telegraph_sounder"));
            shaped(RecipeCategory.REDSTONE, TelegraphRegistry.BOARD_ITEM.get())
                    .pattern("XbX")
                    .pattern("CcC")
                    .pattern("XXX")
                    .define('X', treated)
                    .define('b', brass)
                    .define('C', copper)
                    .define('c', Tier5Items.BASIC_CIRCUIT.get())
                    .unlockedBy("has_pole_insulator", has(Tier5Items.POLE_INSULATOR.get()))
                    .save(output, key("dispatch_board"));
        }
    }
}
