package dev.strataindustria.datagen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.material.Metal;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.Tier4Items;
import dev.strataindustria.transport.foot.FootRegistry;
import dev.strataindustria.transport.ropeway.RopewayAngleBlock;
import dev.strataindustria.transport.ropeway.RopewayRegistry;
import dev.strataindustria.transport.ropeway.RopewayReturnBlock;
import dev.strataindustria.transport.ropeway.RopewayTerminalBlock;
import java.util.function.BiConsumer;
import net.minecraft.advancements.Advancement;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Recipe;

/** Models, text and recipes for the aerial ropeway (outposts and transport spec 8): {@link RopewayRegistry}. */
final class RopewayData {
    private RopewayData() {}

    // ---------------------------------------------------------------- models

    static void models(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        // The stations and towers are hand-made models; the bull wheel, cable and bucket are render-only (items/rotor).
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(RopewayRegistry.TERMINAL.get()).with(
                PropertyDispatch.initial(RopewayTerminalBlock.FACING).generate(facing ->
                        RailData.turn(BlockModelGenerators.plainVariant(StrataIndustria.id("block/ropeway_terminal")), facing))));
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(RopewayRegistry.RETURN.get()).with(
                PropertyDispatch.initial(RopewayReturnBlock.FACING).generate(facing ->
                        RailData.turn(BlockModelGenerators.plainVariant(StrataIndustria.id("block/ropeway_return")), facing))));
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(RopewayRegistry.ANGLE_STATION.get()).with(
                PropertyDispatch.initial(RopewayAngleBlock.FACING).generate(facing ->
                        RailData.turn(BlockModelGenerators.plainVariant(StrataIndustria.id("block/ropeway_angle_station")), facing))));
        blockModels.itemModelOutput.accept(RopewayRegistry.ANGLE_STATION_ITEM.get(), ItemModelUtils.plainModel(StrataIndustria.id("block/ropeway_angle_station")));
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(RopewayRegistry.WOODEN_TOWER.get(),
                BlockModelGenerators.plainVariant(StrataIndustria.id("block/wooden_ropeway_tower"))));
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(RopewayRegistry.STEEL_TOWER.get(),
                BlockModelGenerators.plainVariant(StrataIndustria.id("block/steel_ropeway_tower"))));
        blockModels.itemModelOutput.accept(RopewayRegistry.TERMINAL_ITEM.get(), ItemModelUtils.plainModel(StrataIndustria.id("block/ropeway_terminal")));
        blockModels.itemModelOutput.accept(RopewayRegistry.RETURN_ITEM.get(), ItemModelUtils.plainModel(StrataIndustria.id("block/ropeway_return")));
        blockModels.itemModelOutput.accept(RopewayRegistry.WOODEN_TOWER_ITEM.get(), ItemModelUtils.plainModel(StrataIndustria.id("block/wooden_ropeway_tower")));
        blockModels.itemModelOutput.accept(RopewayRegistry.STEEL_TOWER_ITEM.get(), ItemModelUtils.plainModel(StrataIndustria.id("block/steel_ropeway_tower")));
        itemModels.generateFlatItem(RopewayRegistry.WIRE_ROPE.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(RopewayRegistry.BUCKET.get(), ModelTemplates.FLAT_ITEM);
    }

    // ---------------------------------------------------------------- text

    static void lang(BiConsumer<String, String> add) {
        String id = StrataIndustria.MOD_ID;
        add.accept("block." + id + ".ropeway_terminal", "Ropeway Terminal");
        add.accept("block." + id + ".ropeway_return", "Ropeway Return");
        add.accept("block." + id + ".ropeway_angle_station", "Ropeway Angle Station");
        add.accept("block." + id + ".wooden_ropeway_tower", "Wooden Ropeway Tower");
        add.accept("block." + id + ".steel_ropeway_tower", "Steel Ropeway Tower");
        add.accept("item." + id + ".wire_rope", "Wire Rope");
        add.accept("item." + id + ".ropeway_bucket", "Ropeway Bucket");

        String line = id + ".ropeway.line.";
        add.accept(line + "started", "Rope made fast at the terminal. Now each tower, then the return.");
        add.accept(line + "exists", "Already strung. Sneak with the rope to take the line down and start again.");
        add.accept(line + "need_terminal", "Start at the terminal.");
        add.accept(line + "span", "%s blocks. Used %s wire rope.");
        add.accept(line + "too_long", "Too far for that. %s blocks at most.");
        add.accept(line + "too_steep", "Too steep. One block up or down for every two across.");
        add.accept(line + "blocked", "Something solid in the way at %s.");
        add.accept(line + "need_rope", "Need %s wire rope for that span.");
        add.accept(line + "too_far", "That makes the line longer than %s blocks.");
        add.accept(line + "too_many", "No more than %s spans.");
        add.accept(line + "too_sharp", "Too sharp a bend for a tower. %s degrees at most; an angle station takes more.");
        add.accept(line + "too_sharp_angle", "Too sharp even for an angle station. %s degrees at most.");
        add.accept(line + "twice", "Already on this line.");
        add.accept(line + "taken", "That one carries another line.");
        add.accept(line + "lost", "The terminal is gone.");
        add.accept(line + "done", "Strung. %s blocks, room for %s buckets. Hang them in the terminal and load from behind it.");
        String status = id + ".ropeway.status.";
        add.accept(status + "no_line", "No line. Wire rope on the terminal, then each tower, then the return.");
        add.accept(status + "running", "Running. %s buckets out, %s spare.");
        add.accept(status + "idle", "Waiting for a load. %s buckets out, %s spare.");
        add.accept(status + "unpowered", "Drive isn't turning. %s buckets out, %s spare.");
        add.accept(status + "backed_up", "Backed up. Nowhere to put the load at the return.");
        add.accept(status + "far_station", "The return station isn't loaded.");
        String bucket = id + ".ropeway.bucket.";
        add.accept(bucket + "no_line", "String the line first.");
        add.accept(bucket + "hung", "%s buckets in the terminal, room for %s.");
        add.accept(bucket + "full", "The line holds %s buckets at most.");
        add.accept(bucket + "none_spare", "No spare buckets.");
        add.accept(bucket + "out", "%s buckets back in hand.");
        add.accept(id + ".ropeway.return.line", "Chest or chute behind it takes the loads.");
        add.accept(id + ".ropeway.return.bare", "Not on a line.");
        add.accept(id + ".ropeway.return.backed_up", "Full. The line is waiting.");
        String ride = id + ".ropeway.ride.";
        add.accept(ride + "no_line", "No line to ride.");
        add.accept(ride + "unpowered", "Drive isn't turning.");
        add.accept(ride + "far_station", "The return station isn't loaded.");
        add.accept(ride + "no_spare", "Hang a spare bucket in the terminal first.");
        add.accept(ride + "no_bucket", "No empty bucket out, and none spare at the terminal to send.");
        add.accept(ride + "waiting", "Already waiting.");
        add.accept(ride + "waiting_out", "Stand by. A bucket goes out for you.");
        add.accept(ride + "waiting_back", "Stand by. The next empty bucket coming home takes you.");
        add.accept(ride + "sent", "Sent a bucket out for you.");
        add.accept(ride + "gave_up", "Nobody came. Try again.");
        add.accept(ride + "aboard", "Hold on. Sneak to climb out if the line stops.");
        add.accept(ride + "off_terminal", "Off at the terminal.");
        add.accept(ride + "off_return", "Off at the return.");
        add.accept(ride + "off_angle", "Off at the angle station.");
        add.accept(ride + "snapped", "The line parted.");
        add.accept(id + ".ropeway.tower.carrying", "Carrying a line.");
        add.accept(id + ".ropeway.tower.bare", "No line yet.");

        String subtitles = "subtitles." + id + ".";
        add.accept(subtitles + "ropeway.drive", "Bull wheel turns");
        add.accept(subtitles + "ropeway.sheave", "Bucket clacks over tower");
        add.accept(subtitles + "ropeway.bucket_hang", "Bucket hangs on");
        add.accept(subtitles + "ropeway.bucket_tip", "Bucket tips");
        add.accept(subtitles + "ropeway.rope_tie", "Rope made fast");
        add.accept(subtitles + "ropeway.line_strung", "Line strung");
        add.accept(subtitles + "ropeway.snap", "Line snaps");
        add.accept(subtitles + "ropeway.angle_turn", "Bucket swings round the wheel");
        add.accept(subtitles + "ropeway.seat_clip", "Seat clips on");
        add.accept(subtitles + "ropeway.seat_release", "Seat lets go");
        add.accept(subtitles + "ropeway.ride_wind", "Wind rushes");
        add.accept(subtitles + "ropeway.top_up", "Bucket takes on more");

        String journal = "journal." + id + ".";
        add.accept(journal + "t4.ropeway", "Send Ore by Ropeway");
        add.accept(journal + "t4.ropeway.hint", "Towers and wire rope, a drive wheel at one end, a return at the other. Hang buckets in the drive, put a chest behind each station. Sixty-four items across. An empty hand on a station rides the line.");
        add.accept(journal + "t4.ropeway.lead", "Track won't go over that valley.");
        add.accept(journal + "t4.ropeway.note", "Buckets going over the valley all day now.");
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
            Item ironPlate = ModItems.PLATES.get(Metal.WROUGHT_IRON).get();
            Item ironRod = ModItems.WROUGHT_IRON_ROD.get();
            shapeless(RecipeCategory.TRANSPORTATION, RopewayRegistry.WIRE_ROPE.get(), 4)
                    .requires(rod, 3)
                    .requires(FootRegistry.ROPE.get())
                    .unlockedBy("has_steel_rod", has(rod))
                    .save(output, key("wire_rope"));
            shaped(RecipeCategory.TRANSPORTATION, RopewayRegistry.TERMINAL_ITEM.get())
                    .pattern("EgE")
                    .pattern("AXA")
                    .pattern("EXE")
                    .define('E', plate)
                    .define('g', ModItems.GEARS.get(Metal.BRASS).get())
                    .define('A', Tier4Items.IRON_AXLE.get())
                    .define('X', planks)
                    .unlockedBy("has_steel_rod", has(rod))
                    .save(output, key("ropeway_terminal"));
            shaped(RecipeCategory.TRANSPORTATION, RopewayRegistry.RETURN_ITEM.get())
                    .pattern("E E")
                    .pattern("AXA")
                    .pattern("EXE")
                    .define('E', plate)
                    .define('A', Tier4Items.IRON_AXLE.get())
                    .define('X', planks)
                    .unlockedBy("has_steel_rod", has(rod))
                    .save(output, key("ropeway_return"));
            shapeless(RecipeCategory.TRANSPORTATION, RopewayRegistry.ANGLE_STATION_ITEM.get())
                    .requires(RopewayRegistry.RETURN_ITEM.get())
                    .requires(RopewayRegistry.TERMINAL_ITEM.get())
                    .unlockedBy("has_steel_rod", has(rod))
                    .save(output, key("ropeway_angle_station"));
            shaped(RecipeCategory.TRANSPORTATION, RopewayRegistry.WOODEN_TOWER_ITEM.get(), 2)
                    .pattern("X X")
                    .pattern("XIX")
                    .pattern("X X")
                    .define('X', planks)
                    .define('I', ironRod)
                    .unlockedBy("has_steel_rod", has(rod))
                    .save(output, key("wooden_ropeway_tower"));
            shaped(RecipeCategory.TRANSPORTATION, RopewayRegistry.STEEL_TOWER_ITEM.get(), 2)
                    .pattern("E E")
                    .pattern("EDE")
                    .pattern("E E")
                    .define('E', plate)
                    .define('D', rod)
                    .unlockedBy("has_steel_rod", has(rod))
                    .save(output, key("steel_ropeway_tower"));
            shaped(RecipeCategory.TRANSPORTATION, RopewayRegistry.BUCKET.get())
                    .pattern("W W")
                    .pattern("W W")
                    .pattern(" I ")
                    .define('W', ironPlate)
                    .define('I', ironRod)
                    .unlockedBy("has_steel_rod", has(rod))
                    .save(output, key("ropeway_bucket"));
        }
    }
}
