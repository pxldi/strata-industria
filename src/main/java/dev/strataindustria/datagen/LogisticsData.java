package dev.strataindustria.datagen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.fluid.FluidFilterBlock;
import dev.strataindustria.logistics.ItemPipeBlock;
import dev.strataindustria.logistics.PipeExtractorBlock;
import dev.strataindustria.logistics.StorageControllerBlock;
import dev.strataindustria.logistics.Tier5Logistics;
import java.util.function.BiConsumer;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiPartGenerator;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

/** Names, messages and models for the tier 5 item pipes, extractors, storage controller and fluid filter (spec 12). */
final class LogisticsData {
    private LogisticsData() {}

    static void language(BiConsumer<String, String> lang) {
        String id = StrataIndustria.MOD_ID;
        lang.accept("block." + id + ".item_pipe", "Item Pipe");
        lang.accept("block." + id + ".pipe_extractor", "Pipe Extractor");
        lang.accept("block." + id + ".fast_pipe_extractor", "Fast Pipe Extractor");
        lang.accept("block." + id + ".storage_controller", "Storage Controller");
        lang.accept("block." + id + ".fluid_filter", "Fluid Filter");
        lang.accept("container." + id + ".storage_controller", "Storage Controller");
        String sc = id + ".storage_controller.";
        lang.accept(sc + "search", "Search");
        lang.accept(sc + "search_hint", "Search, #tag or @mod");
        lang.accept(sc + "no_power", "No power");
        lang.accept(sc + "empty", "Nothing stored");
        lang.accept(sc + "no_match", "No match");
        lang.accept(sc + "inventories", "%s chests");
        lang.accept(sc + "stored", "Stored: %s");
        lang.accept(sc + "sort_name", "Sort by name");
        lang.accept(sc + "sort_count", "Sort by count");
        String face = id + ".item_pipe.";
        lang.accept(face + "face.pipe", "%s: pipe");
        lang.accept(face + "face.port", "%s: port");
        lang.accept(face + "face.storage", "%s: storage");
        lang.accept(face + "face.off", "%s: off");
        lang.accept(face + "face.none", "%s: nothing");
        lang.accept(face + "filtered", " (filtered)");
        String ex = id + ".pipe_extractor.status.";
        lang.accept(ex + "no_power", "No power");
        lang.accept(ex + "paused", "Paused by redstone");
        lang.accept(ex + "no_inventory", "Nothing to pull from");
        lang.accept(ex + "no_pipe", "Nothing to push into");
        lang.accept(ex + "nothing", "Nothing to move");
        lang.accept(ex + "pulling", "Pulling");
        lang.accept(id + ".fluid_filter.set", "Lets through: %s");
        lang.accept(id + ".fluid_filter.unset", "Takes the first fluid that comes");
        for (Direction dir : Direction.values()) {
            lang.accept("direction." + id + "." + dir.getName(), capital(dir.getName()));
        }
        String sub = "subtitles." + id + ".";
        lang.accept(sub + "block.item_pipe.extract", "Extractor pulls");
        lang.accept(sub + "block.storage_controller.open", "Controller opens");
        lang.accept(sub + "block.storage_controller.close", "Controller closes");
        lang.accept(sub + "block.storage_controller.store", "Controller stores");
    }

    private static String capital(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    static void models(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        // Item pipe: a core, an arm per connected face, and a collar for storage and off faces.
        MultiPartGenerator pipe = MultiPartGenerator.multiPart(Tier5Logistics.ITEM_PIPE.get())
                .with(BlockModelGenerators.plainVariant(StrataIndustria.id("block/item_pipe_core")));
        MultiVariant arm = BlockModelGenerators.plainVariant(StrataIndustria.id("block/item_pipe_arm"));
        MultiVariant storage = BlockModelGenerators.plainVariant(StrataIndustria.id("block/item_pipe_collar_storage"));
        MultiVariant off = BlockModelGenerators.plainVariant(StrataIndustria.id("block/item_pipe_collar_off"));
        for (Direction side : Direction.values()) {
            var property = ItemPipeBlock.FACES.get(side);
            pipe.with(BlockModelGenerators.condition().term(property, ItemPipeBlock.Face.PIPE, ItemPipeBlock.Face.PORT,
                    ItemPipeBlock.Face.STORAGE, ItemPipeBlock.Face.OFF), rotate(arm, side));
            pipe.with(BlockModelGenerators.condition().term(property, ItemPipeBlock.Face.STORAGE), rotate(storage, side));
            pipe.with(BlockModelGenerators.condition().term(property, ItemPipeBlock.Face.OFF), rotate(off, side));
        }
        blockModels.blockStateOutput.accept(pipe);
        plainItem(itemModels, Tier5Logistics.ITEM_PIPE_ITEM.get(), "block/item_pipe");

        // Extractors: the mouth faces FACING.
        for (var entry : java.util.List.of(
                java.util.Map.entry(Tier5Logistics.PIPE_EXTRACTOR, "pipe_extractor"),
                java.util.Map.entry(Tier5Logistics.FAST_PIPE_EXTRACTOR, "fast_pipe_extractor"))) {
            PropertyDispatch.C2<MultiVariant, Direction, Boolean> dispatch = PropertyDispatch.initial(PipeExtractorBlock.FACING, PipeExtractorBlock.ACTIVE);
            for (boolean active : new boolean[]{false, true}) {
                MultiVariant model = BlockModelGenerators.plainVariant(StrataIndustria.id("block/" + entry.getValue() + (active ? "_active" : "")));
                for (Direction dir : Direction.values()) dispatch.select(dir, active, rotate(model, dir));
            }
            blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(entry.getKey().get()).with(dispatch));
            plainItem(itemModels, entry.getKey().get().asItem(), "block/" + entry.getValue());
        }

        // Storage controller: an orientable cube with a lit front.
        PropertyDispatch.C2<MultiVariant, Direction, Boolean> controller = PropertyDispatch.initial(StorageControllerBlock.FACING, StorageControllerBlock.ACTIVE);
        for (boolean active : new boolean[]{false, true}) {
            MultiVariant model = BlockModelGenerators.plainVariant(StrataIndustria.id("block/storage_controller" + (active ? "_active" : "")));
            controller.select(Direction.NORTH, active, model);
            controller.select(Direction.EAST, active, model.with(BlockModelGenerators.Y_ROT_90));
            controller.select(Direction.SOUTH, active, model.with(BlockModelGenerators.Y_ROT_180));
            controller.select(Direction.WEST, active, model.with(BlockModelGenerators.Y_ROT_270));
        }
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(Tier5Logistics.STORAGE_CONTROLLER.get()).with(controller));
        plainItem(itemModels, Tier5Logistics.STORAGE_CONTROLLER_ITEM.get(), "block/storage_controller");

        // Fluid filter: modelled along Z, rotated to its axis.
        PropertyDispatch.C1<MultiVariant, Direction> filter = PropertyDispatch.initial(FluidFilterBlock.FACING);
        MultiVariant filterModel = BlockModelGenerators.plainVariant(StrataIndustria.id("block/fluid_filter"));
        filter.select(Direction.NORTH, filterModel);
        filter.select(Direction.SOUTH, filterModel);
        filter.select(Direction.EAST, filterModel.with(BlockModelGenerators.Y_ROT_90));
        filter.select(Direction.WEST, filterModel.with(BlockModelGenerators.Y_ROT_90));
        filter.select(Direction.UP, filterModel.with(BlockModelGenerators.X_ROT_90));
        filter.select(Direction.DOWN, filterModel.with(BlockModelGenerators.X_ROT_90));
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(Tier5Logistics.FLUID_FILTER.get()).with(filter));
        plainItem(itemModels, Tier5Logistics.FLUID_FILTER_ITEM.get(), "block/fluid_filter");
    }

    /** A model built facing north, turned to point at {@code side}. */
    private static MultiVariant rotate(MultiVariant north, Direction side) {
        return switch (side) {
            case NORTH -> north;
            case EAST -> north.with(BlockModelGenerators.Y_ROT_90);
            case SOUTH -> north.with(BlockModelGenerators.Y_ROT_180);
            case WEST -> north.with(BlockModelGenerators.Y_ROT_270);
            case UP -> north.with(BlockModelGenerators.X_ROT_270);
            case DOWN -> north.with(BlockModelGenerators.X_ROT_90);
        };
    }

    private static void plainItem(ItemModelGenerators itemModels, Item item, String model) {
        itemModels.itemModelOutput.accept(item, ItemModelUtils.plainModel(StrataIndustria.id(model)));
    }
}
