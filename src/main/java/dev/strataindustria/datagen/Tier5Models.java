package dev.strataindustria.datagen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.electric.BatteryBoxBlock;
import dev.strataindustria.electric.CableBlock;
import dev.strataindustria.electric.GeneratorBlock;
import dev.strataindustria.electric.KineticDynamoBlock;
import dev.strataindustria.electric.machine.ElectricMachineBlock;
import dev.strataindustria.electric.machine.LatheBlock;
import dev.strataindustria.power.ElectricTier;
import dev.strataindustria.power.StatusLight;
import dev.strataindustria.registry.Tier5Blocks;
import dev.strataindustria.rubber.TreeTapBlock;
import dev.strataindustria.registry.Tier5Items;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiPartGenerator;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import java.util.Optional;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** Block states and models for tier 5 (electric). {@link ModModelProvider} calls it. */
final class Tier5Models {
    private Tier5Models() {}

    static void register(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        for (var item : java.util.List.of(Tier5Items.MAGNET, Tier5Items.COPPER_ROD, Tier5Items.COPPER_WIRE, Tier5Items.LEAD_PLATE,
                Tier5Items.RAW_RUBBER, Tier5Items.COMPOUNDED_RUBBER, Tier5Items.RUBBER, Tier5Items.LATEX_BUCKET, Tier5Items.TREE_TAP,
                Tier5Items.RED_ALLOY_ROD, Tier5Items.RED_ALLOY_WIRE, Tier5Items.DRAW_PLATE, Tier5Items.CIRCUIT_BOARD, Tier5Items.BASIC_CIRCUIT,
                Tier5Items.ELECTRIC_MOTOR, Tier5Items.ALUMINIUM_WIRE, Tier5Items.STEEL_WIRE, Tier5Items.SULFURIC_ACID_BUCKET, Tier5Items.ALUM,
                Tier5Items.ALUMINA)) {
            itemModels.generateFlatItem(item.get(), ModelTemplates.FLAT_ITEM);
        }
        treeTap(blockModels);
        cables(blockModels, itemModels);
        dynamo(blockModels, itemModels);
        batteryBox(blockModels, itemModels);
        machine(blockModels, itemModels, Tier5Blocks.ELECTRIC_FURNACE.get(), "electric_furnace");
        machine(blockModels, itemModels, Tier5Blocks.MACERATOR.get(), "macerator");
        machine(blockModels, itemModels, Tier5Blocks.WIREMILL.get(), "wiremill");
        machine(blockModels, itemModels, Tier5Blocks.BENDER.get(), "bender");
        machine(blockModels, itemModels, Tier5Blocks.MIXER.get(), "mixer");
        machine(blockModels, itemModels, Tier5Blocks.ELECTROLYSER.get(), "electrolyser");
        lathe(blockModels, itemModels);
        generator(blockModels, itemModels, Tier5Blocks.STEAM_TURBINE.get(), "steam_turbine", false);
        generator(blockModels, itemModels, Tier5Blocks.COMBUSTION_GENERATOR.get(), "combustion_generator", true);

        // Spec 9.5: casing all round, with a blank access panel on the sides so it reads as unfinished.
        TextureMapping hull = new TextureMapping().put(TextureSlot.SIDE, texture("lv_machine_hull_front"))
                .put(TextureSlot.TOP, texture("casing/lv_top")).put(TextureSlot.BOTTOM, texture("casing/lv_bottom"));
        Identifier hullModel = ModelTemplates.CUBE_BOTTOM_TOP.create(Tier5Blocks.LV_MACHINE_HULL.get(), hull, blockModels.modelOutput);
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(Tier5Blocks.LV_MACHINE_HULL.get(), BlockModelGenerators.plainVariant(hullModel)));
        plainItem(itemModels, Tier5Items.LV_MACHINE_HULL.get(), hullModel);
    }

    // Spec 5.1 and 23.2: a hand-built spout and bowl facing north (log to the south), one model per cup level.
    private static void treeTap(BlockModelGenerators blockModels) {
        PropertyDispatch.C2<MultiVariant, Direction, Integer> dispatch = PropertyDispatch.initial(TreeTapBlock.FACING, TreeTapBlock.FILL);
        for (int fill = 0; fill <= 3; fill++) {
            MultiVariant model = BlockModelGenerators.plainVariant(StrataIndustria.id("block/tree_tap" + (fill == 0 ? "" : "_" + fill)));
            dispatch.select(Direction.NORTH, fill, model);
            dispatch.select(Direction.EAST, fill, model.with(BlockModelGenerators.Y_ROT_90));
            dispatch.select(Direction.SOUTH, fill, model.with(BlockModelGenerators.Y_ROT_180));
            dispatch.select(Direction.WEST, fill, model.with(BlockModelGenerators.Y_ROT_270));
        }
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(Tier5Blocks.TREE_TAP.get()).with(dispatch));
    }

    // Spec 8.1: hand-built core and arm models (core, arm pointing north) joined on each connected face.
    private static void cables(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        for (var cable : java.util.List.of(Tier5Blocks.LV_CABLE, Tier5Blocks.MV_CABLE)) {
            String name = cable.getId().getPath();
            MultiPartGenerator parts = MultiPartGenerator.multiPart(cable.get())
                    .with(BlockModelGenerators.plainVariant(StrataIndustria.id("block/" + name + "_core")));
            MultiVariant north = BlockModelGenerators.plainVariant(StrataIndustria.id("block/" + name + "_arm"));
            var props = CableBlock.PROPERTIES;
            parts.with(BlockModelGenerators.condition(props.get(Direction.NORTH), true), north);
            parts.with(BlockModelGenerators.condition(props.get(Direction.EAST), true), north.with(BlockModelGenerators.Y_ROT_90));
            parts.with(BlockModelGenerators.condition(props.get(Direction.SOUTH), true), north.with(BlockModelGenerators.Y_ROT_180));
            parts.with(BlockModelGenerators.condition(props.get(Direction.WEST), true), north.with(BlockModelGenerators.Y_ROT_270));
            parts.with(BlockModelGenerators.condition(props.get(Direction.UP), true), north.with(BlockModelGenerators.X_ROT_270));
            parts.with(BlockModelGenerators.condition(props.get(Direction.DOWN), true), north.with(BlockModelGenerators.X_ROT_90));
            blockModels.blockStateOutput.accept(parts);
            plainItem(itemModels, cable.get().asItem(), StrataIndustria.id("block/" + name));
        }
    }

    // Spec 7.1 and 23.2: a hand-built body facing north with the armature window in front; one model per status lamp.
    private static void dynamo(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        PropertyDispatch.C2<MultiVariant, Direction, StatusLight> dispatch = PropertyDispatch.initial(KineticDynamoBlock.FACING, KineticDynamoBlock.STATUS);
        for (StatusLight light : StatusLight.values()) {
            MultiVariant model = BlockModelGenerators.plainVariant(StrataIndustria.id("block/kinetic_dynamo_" + light.getSerializedName()));
            dispatch.select(Direction.NORTH, light, model);
            dispatch.select(Direction.EAST, light, model.with(BlockModelGenerators.Y_ROT_90));
            dispatch.select(Direction.SOUTH, light, model.with(BlockModelGenerators.Y_ROT_180));
            dispatch.select(Direction.WEST, light, model.with(BlockModelGenerators.Y_ROT_270));
            dispatch.select(Direction.UP, light, model.with(BlockModelGenerators.X_ROT_270));
            dispatch.select(Direction.DOWN, light, model.with(BlockModelGenerators.X_ROT_90));
        }
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(Tier5Blocks.KINETIC_DYNAMO.get()).with(dispatch));
        plainItem(itemModels, Tier5Items.KINETIC_DYNAMO.get(), StrataIndustria.id("block/kinetic_dynamo_off"));
    }

    // Spec 7.4 and 23.3: casing on the sides and top, cells behind a grille on the front, the meter lit by charge.
    private static void batteryBox(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        PropertyDispatch.C4<MultiVariant, Direction, ElectricTier, Integer, Boolean> dispatch = PropertyDispatch.initial(
                BatteryBoxBlock.FACING, BatteryBoxBlock.TIER, BatteryBoxBlock.CHARGE, BatteryBoxBlock.CHARGING);
        for (ElectricTier tier : ElectricTier.values()) {
            String t = tier.getSerializedName();
            for (int charge = 0; charge <= BatteryBoxBlock.SEGMENTS; charge++) {
                TextureMapping faces = new TextureMapping().put(TextureSlot.FRONT, texture("battery_box_front_" + t + "_" + charge))
                        .put(TextureSlot.SIDE, texture("casing/" + t + "_side")).put(TextureSlot.TOP, texture("casing/" + t + "_top"))
                        .put(TextureSlot.BOTTOM, texture("casing/" + t + "_bottom"));
                Identifier id = ModelTemplates.CUBE_ORIENTABLE_TOP_BOTTOM.create(StrataIndustria.id("block/battery_box_" + t + "_" + charge), faces,
                        blockModels.modelOutput);
                MultiVariant model = BlockModelGenerators.plainVariant(id);
                for (boolean charging : new boolean[] {false, true}) {
                    dispatch.select(Direction.NORTH, tier, charge, charging, model);
                    dispatch.select(Direction.EAST, tier, charge, charging, model.with(BlockModelGenerators.Y_ROT_90));
                    dispatch.select(Direction.SOUTH, tier, charge, charging, model.with(BlockModelGenerators.Y_ROT_180));
                    dispatch.select(Direction.WEST, tier, charge, charging, model.with(BlockModelGenerators.Y_ROT_270));
                }
            }
        }
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(Tier5Blocks.BATTERY_BOX.get()).with(dispatch));
        plainItem(itemModels, Tier5Items.BATTERY_BOX.get(), StrataIndustria.id("block/battery_box_lv_0"));
    }

    private static final TextureSlot STATUS = TextureSlot.create("status");
    /** A cube with the front's status lamp as an emissive overlay (spec 23.2). */
    private static final ModelTemplate MACHINE = new ModelTemplate(Optional.of(StrataIndustria.id("block/electric_machine")), Optional.empty(),
            TextureSlot.FRONT, TextureSlot.SIDE, TextureSlot.TOP, TextureSlot.BOTTOM, STATUS);

    // Spec 23.2: the front per tier and active, the casing by tier, the lamp lit by status (none when off).
    private static void machine(BlockModelGenerators blockModels, ItemModelGenerators itemModels, Block block, String name) {
        PropertyDispatch.C4<MultiVariant, Direction, ElectricTier, StatusLight, Boolean> dispatch = PropertyDispatch.initial(
                ElectricMachineBlock.FACING, ElectricMachineBlock.TIER, ElectricMachineBlock.STATUS, ElectricMachineBlock.ACTIVE);
        for (ElectricTier tier : ElectricTier.values()) {
            String t = tier.getSerializedName();
            for (boolean active : new boolean[] {false, true}) {
                for (StatusLight light : StatusLight.values()) {
                    TextureMapping faces = new TextureMapping()
                            .put(TextureSlot.FRONT, texture(name + "_front_" + t + (active ? "_active" : "")))
                            .put(TextureSlot.SIDE, texture("casing/" + t + "_side")).put(TextureSlot.TOP, texture("casing/" + t + "_top"))
                            .put(TextureSlot.BOTTOM, texture("casing/" + t + "_bottom"));
                    Identifier model = StrataIndustria.id("block/" + name + "_" + t + (active ? "_active" : "") + "_" + light.getSerializedName());
                    Identifier id = light == StatusLight.OFF
                            ? ModelTemplates.CUBE_ORIENTABLE_TOP_BOTTOM.create(model, faces, blockModels.modelOutput)
                            : MACHINE.create(model, faces.put(STATUS, texture("overlay/status_" + light.getSerializedName())), blockModels.modelOutput);
                    MultiVariant variant = BlockModelGenerators.plainVariant(id);
                    dispatch.select(Direction.NORTH, tier, light, active, variant);
                    dispatch.select(Direction.EAST, tier, light, active, variant.with(BlockModelGenerators.Y_ROT_90));
                    dispatch.select(Direction.SOUTH, tier, light, active, variant.with(BlockModelGenerators.Y_ROT_180));
                    dispatch.select(Direction.WEST, tier, light, active, variant.with(BlockModelGenerators.Y_ROT_270));
                }
            }
        }
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(block).with(dispatch));
        plainItem(itemModels, block.asItem(), StrataIndustria.id("block/" + name + "_lv_off"));
    }

    // Spec 23.2: like machine(), with the rod or gear icon of the mode on the front.
    private static void lathe(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        Block block = Tier5Blocks.LATHE.get();
        PropertyDispatch.C5<MultiVariant, Direction, ElectricTier, StatusLight, Boolean, Boolean> dispatch = PropertyDispatch.initial(
                ElectricMachineBlock.FACING, ElectricMachineBlock.TIER, ElectricMachineBlock.STATUS, ElectricMachineBlock.ACTIVE, LatheBlock.GEAR);
        for (ElectricTier tier : ElectricTier.values()) {
            String t = tier.getSerializedName();
            for (boolean gear : new boolean[] {false, true}) {
                for (boolean active : new boolean[] {false, true}) {
                    for (StatusLight light : StatusLight.values()) {
                        TextureMapping faces = new TextureMapping()
                                .put(TextureSlot.FRONT, texture("lathe_front_" + t + (gear ? "_gear" : "") + (active ? "_active" : "")))
                                .put(TextureSlot.SIDE, texture("casing/" + t + "_side")).put(TextureSlot.TOP, texture("casing/" + t + "_top"))
                                .put(TextureSlot.BOTTOM, texture("casing/" + t + "_bottom"));
                        Identifier model = StrataIndustria.id("block/lathe_" + t + (gear ? "_gear" : "") + (active ? "_active" : "") + "_"
                                + light.getSerializedName());
                        Identifier id = light == StatusLight.OFF
                                ? ModelTemplates.CUBE_ORIENTABLE_TOP_BOTTOM.create(model, faces, blockModels.modelOutput)
                                : MACHINE.create(model, faces.put(STATUS, texture("overlay/status_" + light.getSerializedName())), blockModels.modelOutput);
                        MultiVariant variant = BlockModelGenerators.plainVariant(id);
                        dispatch.select(Direction.NORTH, tier, light, active, gear, variant);
                        dispatch.select(Direction.EAST, tier, light, active, gear, variant.with(BlockModelGenerators.Y_ROT_90));
                        dispatch.select(Direction.SOUTH, tier, light, active, gear, variant.with(BlockModelGenerators.Y_ROT_180));
                        dispatch.select(Direction.WEST, tier, light, active, gear, variant.with(BlockModelGenerators.Y_ROT_270));
                    }
                }
            }
        }
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(block).with(dispatch));
        plainItem(itemModels, block.asItem(), StrataIndustria.id("block/lathe_lv_off"));
    }

    private static final TextureSlot BACK = TextureSlot.create("back");
    /** Like {@link #MACHINE} with its own back face. */
    private static final ModelTemplate GENERATOR = new ModelTemplate(Optional.of(StrataIndustria.id("block/electric_generator")), Optional.empty(),
            TextureSlot.FRONT, BACK, TextureSlot.SIDE, TextureSlot.TOP, TextureSlot.BOTTOM, STATUS);

    // Spec 23.2: a generator has a front per tier and active, a back (the turbine's steam flange), and for the
    // combustion generator an exhaust stack on top.
    private static void generator(BlockModelGenerators blockModels, ItemModelGenerators itemModels, Block block, String name, boolean stack) {
        PropertyDispatch.C4<MultiVariant, Direction, ElectricTier, StatusLight, Boolean> dispatch = PropertyDispatch.initial(
                GeneratorBlock.FACING, GeneratorBlock.TIER, GeneratorBlock.STATUS, GeneratorBlock.ACTIVE);
        for (ElectricTier tier : ElectricTier.values()) {
            String t = tier.getSerializedName();
            for (boolean active : new boolean[] {false, true}) {
                for (StatusLight light : StatusLight.values()) {
                    TextureMapping faces = new TextureMapping()
                            .put(TextureSlot.FRONT, texture(name + "_front_" + t + (active ? "_active" : "")))
                            .put(BACK, texture(stack ? "casing/" + t + "_side" : name + "_back_" + t))
                            .put(TextureSlot.SIDE, texture("casing/" + t + "_side"))
                            .put(TextureSlot.TOP, texture(stack ? name + "_top_" + t : "casing/" + t + "_top"))
                            .put(TextureSlot.BOTTOM, texture("casing/" + t + "_bottom"))
                            .put(STATUS, texture("overlay/status_" + light.getSerializedName()));
                    Identifier model = GENERATOR.create(StrataIndustria.id("block/" + name + "_" + t + (active ? "_active" : "") + "_" + light.getSerializedName()),
                            faces, blockModels.modelOutput);
                    MultiVariant variant = BlockModelGenerators.plainVariant(model);
                    dispatch.select(Direction.NORTH, tier, light, active, variant);
                    dispatch.select(Direction.EAST, tier, light, active, variant.with(BlockModelGenerators.Y_ROT_90));
                    dispatch.select(Direction.SOUTH, tier, light, active, variant.with(BlockModelGenerators.Y_ROT_180));
                    dispatch.select(Direction.WEST, tier, light, active, variant.with(BlockModelGenerators.Y_ROT_270));
                }
            }
        }
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(block).with(dispatch));
        plainItem(itemModels, block.asItem(), StrataIndustria.id("block/" + name + "_lv_off"));
    }

    private static void plainItem(ItemModelGenerators itemModels, Item item, Identifier model) {
        itemModels.itemModelOutput.accept(item, ItemModelUtils.plainModel(model));
    }

    private static Material texture(String path) {
        return new Material(StrataIndustria.id("block/" + path));
    }
}
