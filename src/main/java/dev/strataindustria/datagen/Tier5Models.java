package dev.strataindustria.datagen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.electric.BatteryBoxBlock;
import dev.strataindustria.electric.CableBlock;
import dev.strataindustria.electric.GeneratorBlock;
import dev.strataindustria.electric.EnergyAdapterBlock;
import dev.strataindustria.electric.ElectricPumpBlock;
import dev.strataindustria.electric.KineticDynamoBlock;
import dev.strataindustria.electric.KineticMotorBlock;
import dev.strataindustria.electric.LiquidFuelBurnerBlock;
import dev.strataindustria.electric.TransformerBlock;
import dev.strataindustria.electric.machine.ElectricMachineBlock;
import dev.strataindustria.electric.machine.LatheBlock;
import dev.strataindustria.power.ElectricTier;
import dev.strataindustria.power.StatusLight;
import dev.strataindustria.registry.Tier5Blocks;
import dev.strataindustria.registry.Tier5DataComponents;
import dev.strataindustria.rubber.TreeTapBlock;
import dev.strataindustria.registry.Tier5Items;
import dev.strataindustria.electric.PoleInsulatorBlock;
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
                Tier5Items.ALUMINA, Tier5Items.LEAD_ACID_CELL, Tier5Items.MV_UPGRADE_KIT, Tier5Items.UNFIRED_INSULATOR, Tier5Items.CERAMIC_INSULATOR,
                Tier5Items.ACSR_CONDUCTOR, Tier5Items.WRENCH, Tier5Items.ORE_SCANNER)) {
            itemModels.generateFlatItem(item.get(), ModelTemplates.FLAT_ITEM);
        }
        treeTap(blockModels);
        overheadLines(blockModels, itemModels);
        cables(blockModels, itemModels);
        dynamo(blockModels, itemModels);
        batteryBox(blockModels, itemModels);
        machine(blockModels, itemModels, Tier5Blocks.ELECTRIC_FURNACE.get(), "electric_furnace");
        machine(blockModels, itemModels, Tier5Blocks.MACERATOR.get(), "macerator");
        machine(blockModels, itemModels, Tier5Blocks.WIREMILL.get(), "wiremill");
        machine(blockModels, itemModels, Tier5Blocks.BENDER.get(), "bender");
        machine(blockModels, itemModels, Tier5Blocks.MIXER.get(), "mixer");
        machine(blockModels, itemModels, Tier5Blocks.ASSEMBLER.get(), "assembler");
        machine(blockModels, itemModels, Tier5Blocks.ELECTROLYSER.get(), "electrolyser");
        lathe(blockModels, itemModels);
        extruder(blockModels, itemModels);
        powerHammer(blockModels, itemModels);
        transformer(blockModels, itemModels);
        energyAdapter(blockModels, itemModels);
        generator(blockModels, itemModels, Tier5Blocks.STEAM_TURBINE.get(), "steam_turbine", false);
        generator(blockModels, itemModels, Tier5Blocks.COMBUSTION_GENERATOR.get(), "combustion_generator", true);
        generator(blockModels, itemModels, Tier5Blocks.ELECTRIC_HEATER.get(), "electric_heater", true);
        liquidFuelBurner(blockModels, itemModels);
        electricPump(blockModels, itemModels);
        kineticMotor(blockModels, itemModels);

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

    // Spec 5.4 and 8.3: the treated log is a pillar; the pole and the insulator are hand-built models (the insulator points up).
    private static void overheadLines(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        Block log = Tier5Blocks.TREATED_LOG.get();
        Identifier logModel = ModelTemplates.CUBE_COLUMN.create(log, TextureMapping.column(texture("treated_log_side"), texture("treated_log_top")),
                blockModels.modelOutput);
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(log, BlockModelGenerators.plainVariant(logModel))
                .with(BlockModelGenerators.createRotatedPillar()));
        plainItem(itemModels, log.asItem(), logModel);

        Identifier pole = StrataIndustria.id("block/utility_pole");
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(Tier5Blocks.UTILITY_POLE.get(), BlockModelGenerators.plainVariant(pole)));
        plainItem(itemModels, Tier5Items.UTILITY_POLE.get(), pole);

        MultiVariant up = BlockModelGenerators.plainVariant(StrataIndustria.id("block/pole_insulator"));
        PropertyDispatch.C1<MultiVariant, Direction> dispatch = PropertyDispatch.initial(PoleInsulatorBlock.FACING);
        dispatch.select(Direction.UP, up);
        dispatch.select(Direction.DOWN, up.with(BlockModelGenerators.X_ROT_180));
        dispatch.select(Direction.NORTH, up.with(BlockModelGenerators.X_ROT_90));
        dispatch.select(Direction.SOUTH, up.with(BlockModelGenerators.X_ROT_270));
        dispatch.select(Direction.EAST, up.with(BlockModelGenerators.X_ROT_90).with(BlockModelGenerators.Y_ROT_90));
        dispatch.select(Direction.WEST, up.with(BlockModelGenerators.X_ROT_90).with(BlockModelGenerators.Y_ROT_270));
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(Tier5Blocks.POLE_INSULATOR.get()).with(dispatch));
        plainItem(itemModels, Tier5Items.POLE_INSULATOR.get(), StrataIndustria.id("block/pole_insulator"));
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
        tieredItem(itemModels, Tier5Items.BATTERY_BOX.get(), "block/battery_box_lv_0", "block/battery_box_mv_0");
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
        tieredItem(itemModels, block.asItem(), "block/" + name + "_lv_off", "block/" + name + "_mv_off");
    }

    // Spec 10.9: the extruder only exists as MV, so the LV states reuse the MV models.
    private static void extruder(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        Block block = Tier5Blocks.EXTRUDER.get();
        PropertyDispatch.C4<MultiVariant, Direction, ElectricTier, StatusLight, Boolean> dispatch = PropertyDispatch.initial(
                ElectricMachineBlock.FACING, ElectricMachineBlock.TIER, ElectricMachineBlock.STATUS, ElectricMachineBlock.ACTIVE);
        for (boolean active : new boolean[] {false, true}) {
            for (StatusLight light : StatusLight.values()) {
                TextureMapping faces = new TextureMapping()
                        .put(TextureSlot.FRONT, texture("extruder_front_mv" + (active ? "_active" : "")))
                        .put(TextureSlot.SIDE, texture("casing/mv_side")).put(TextureSlot.TOP, texture("casing/mv_top"))
                        .put(TextureSlot.BOTTOM, texture("casing/mv_bottom"));
                Identifier model = StrataIndustria.id("block/extruder_mv" + (active ? "_active" : "") + "_" + light.getSerializedName());
                Identifier id = light == StatusLight.OFF
                        ? ModelTemplates.CUBE_ORIENTABLE_TOP_BOTTOM.create(model, faces, blockModels.modelOutput)
                        : MACHINE.create(model, faces.put(STATUS, texture("overlay/status_" + light.getSerializedName())), blockModels.modelOutput);
                MultiVariant variant = BlockModelGenerators.plainVariant(id);
                for (ElectricTier tier : ElectricTier.values()) {
                    dispatch.select(Direction.NORTH, tier, light, active, variant);
                    dispatch.select(Direction.EAST, tier, light, active, variant.with(BlockModelGenerators.Y_ROT_90));
                    dispatch.select(Direction.SOUTH, tier, light, active, variant.with(BlockModelGenerators.Y_ROT_180));
                    dispatch.select(Direction.WEST, tier, light, active, variant.with(BlockModelGenerators.Y_ROT_270));
                }
            }
        }
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(block).with(dispatch));
        plainItem(itemModels, block.asItem(), StrataIndustria.id("block/extruder_mv_off"));
    }

    private static final TextureSlot FRAME = TextureSlot.create("frame"), HAMMER_MOTOR = TextureSlot.create("motor"),
            HAMMER_MOTOR_FRONT = TextureSlot.create("motor_front"), COIL = TextureSlot.create("coil"), BASE = TextureSlot.create("base"),
            BASE_TOP = TextureSlot.create("base_top");
    private static final ModelTemplate POWER_HAMMER = new ModelTemplate(Optional.of(StrataIndustria.id("block/power_hammer")), Optional.empty(),
            FRAME, HAMMER_MOTOR, HAMMER_MOTOR_FRONT, COIL, BASE, BASE_TOP, STATUS);

    // Spec 10.8 and 23.2: the hand-built frame per tier, the motor front per tier and active, the lamp lit by status.
    // The ram is drawn by the renderer and shown in the item by a second model.
    private static void powerHammer(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        Block block = Tier5Blocks.POWER_HAMMER.get();
        PropertyDispatch.C4<MultiVariant, Direction, ElectricTier, StatusLight, Boolean> dispatch = PropertyDispatch.initial(
                ElectricMachineBlock.FACING, ElectricMachineBlock.TIER, ElectricMachineBlock.STATUS, ElectricMachineBlock.ACTIVE);
        for (ElectricTier tier : ElectricTier.values()) {
            String t = tier.getSerializedName();
            for (boolean active : new boolean[] {false, true}) {
                for (StatusLight light : StatusLight.values()) {
                    TextureMapping textures = new TextureMapping().put(FRAME, texture("power_hammer_frame_" + t)).put(HAMMER_MOTOR, texture("power_hammer_motor"))
                            .put(HAMMER_MOTOR_FRONT, texture("power_hammer_motor_front_" + t + (active ? "_active" : ""))).put(COIL, texture("power_hammer_coil"))
                            .put(BASE, texture("steam_hammer_base")).put(BASE_TOP, texture("steam_hammer_base_top"))
                            .put(STATUS, texture("overlay/status_" + (light == StatusLight.OFF ? "off" : light.getSerializedName())));
                    Identifier model = POWER_HAMMER.create(StrataIndustria.id("block/power_hammer_" + t + (active ? "_active" : "") + "_"
                            + light.getSerializedName()), textures, blockModels.modelOutput);
                    MultiVariant variant = BlockModelGenerators.plainVariant(model);
                    dispatch.select(Direction.NORTH, tier, light, active, variant);
                    dispatch.select(Direction.EAST, tier, light, active, variant.with(BlockModelGenerators.Y_ROT_90));
                    dispatch.select(Direction.SOUTH, tier, light, active, variant.with(BlockModelGenerators.Y_ROT_180));
                    dispatch.select(Direction.WEST, tier, light, active, variant.with(BlockModelGenerators.Y_ROT_270));
                }
            }
        }
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(block).with(dispatch));
        net.minecraft.client.renderer.item.ItemModel.Unbaked lv = ItemModelUtils.composite(ItemModelUtils.plainModel(StrataIndustria.id("block/power_hammer_lv_off")),
                ItemModelUtils.plainModel(StrataIndustria.id("block/power_hammer_ram")));
        net.minecraft.client.renderer.item.ItemModel.Unbaked mv = ItemModelUtils.composite(ItemModelUtils.plainModel(StrataIndustria.id("block/power_hammer_mv_off")),
                ItemModelUtils.plainModel(StrataIndustria.id("block/power_hammer_ram")));
        itemModels.itemModelOutput.accept(block.asItem(), ItemModelUtils.conditional(new net.minecraft.client.renderer.item.properties.conditional.HasComponent(
                Tier5DataComponents.MACHINE_TIER.get(), false), mv, lv));
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
        tieredItem(itemModels, block.asItem(), "block/lathe_lv_off", "block/lathe_mv_off");
    }

    // Spec 23.3: the MV front with its fins, the coil window on the other sides, an arrow on top for the mode.
    private static void transformer(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        PropertyDispatch.C3<MultiVariant, Direction, Boolean, Boolean> dispatch = PropertyDispatch.initial(TransformerBlock.FACING,
                TransformerBlock.STEP_UP, TransformerBlock.ACTIVE);
        Identifier first = null;
        for (boolean up : new boolean[] {false, true}) {
            String mode = up ? "step_up" : "step_down";
            TextureMapping faces = new TextureMapping().put(TextureSlot.FRONT, texture("transformer_front"))
                    .put(TextureSlot.SIDE, texture("transformer_side")).put(TextureSlot.TOP, texture("transformer_top_" + mode))
                    .put(TextureSlot.BOTTOM, texture("casing/mv_bottom"));
            Identifier id = ModelTemplates.CUBE_ORIENTABLE_TOP_BOTTOM.create(StrataIndustria.id("block/transformer_" + mode), faces, blockModels.modelOutput);
            if (first == null) first = id;
            MultiVariant model = BlockModelGenerators.plainVariant(id);
            for (boolean active : new boolean[] {false, true}) {
                dispatch.select(Direction.NORTH, up, active, model);
                dispatch.select(Direction.EAST, up, active, model.with(BlockModelGenerators.Y_ROT_90));
                dispatch.select(Direction.SOUTH, up, active, model.with(BlockModelGenerators.Y_ROT_180));
                dispatch.select(Direction.WEST, up, active, model.with(BlockModelGenerators.Y_ROT_270));
            }
        }
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(Tier5Blocks.TRANSFORMER.get()).with(dispatch));
        plainItem(itemModels, Tier5Items.TRANSFORMER.get(), first);
    }

    // Spec 23.3: a brass socket on the front, six-way like the dynamo.
    private static void energyAdapter(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        PropertyDispatch.C2<MultiVariant, Direction, ElectricTier> dispatch = PropertyDispatch.initial(EnergyAdapterBlock.FACING, EnergyAdapterBlock.TIER);
        for (ElectricTier tier : ElectricTier.values()) {
            String t = tier.getSerializedName();
            TextureMapping faces = new TextureMapping().put(TextureSlot.FRONT, texture("energy_adapter_front_" + t))
                    .put(TextureSlot.SIDE, texture("casing/" + t + "_side")).put(TextureSlot.TOP, texture("casing/" + t + "_side"))
                    .put(TextureSlot.BOTTOM, texture("casing/" + t + "_side"));
            Identifier id = ModelTemplates.CUBE_ORIENTABLE_TOP_BOTTOM.create(StrataIndustria.id("block/energy_adapter_" + t), faces, blockModels.modelOutput);
            MultiVariant model = BlockModelGenerators.plainVariant(id);
            dispatch.select(Direction.NORTH, tier, model);
            dispatch.select(Direction.EAST, tier, model.with(BlockModelGenerators.Y_ROT_90));
            dispatch.select(Direction.SOUTH, tier, model.with(BlockModelGenerators.Y_ROT_180));
            dispatch.select(Direction.WEST, tier, model.with(BlockModelGenerators.Y_ROT_270));
            dispatch.select(Direction.UP, tier, model.with(BlockModelGenerators.X_ROT_270));
            dispatch.select(Direction.DOWN, tier, model.with(BlockModelGenerators.X_ROT_90));
        }
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(Tier5Blocks.ENERGY_ADAPTER.get()).with(dispatch));
        tieredItem(itemModels, Tier5Items.ENERGY_ADAPTER.get(), "block/energy_adapter_lv", "block/energy_adapter_mv");
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
        tieredItem(itemModels, block.asItem(), "block/" + name + "_lv_off", "block/" + name + "_mv_off");
    }

    // Spec 7.5 and 23.2: fire bricks with a brass valve on the front; the lit front shows a nozzle flame.
    private static void liquidFuelBurner(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        Block block = Tier5Blocks.LIQUID_FUEL_BURNER.get();
        TextureMapping cold = new TextureMapping().put(TextureSlot.FRONT, texture("liquid_fuel_burner_front"))
                .put(TextureSlot.SIDE, texture("liquid_fuel_burner_side")).put(TextureSlot.TOP, texture("liquid_fuel_burner_top"));
        MultiVariant unlit = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_ORIENTABLE.create(block, cold, blockModels.modelOutput));
        MultiVariant lit = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_ORIENTABLE.createWithSuffix(block, "_lit",
                cold.copyAndUpdate(TextureSlot.FRONT, texture("liquid_fuel_burner_front_lit")), blockModels.modelOutput));
        PropertyDispatch.C2<MultiVariant, Direction, Boolean> dispatch = PropertyDispatch.initial(LiquidFuelBurnerBlock.FACING, LiquidFuelBurnerBlock.LIT);
        for (boolean on : new boolean[] {false, true}) horizontal(dispatch, on, on ? lit : unlit);
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(block).with(dispatch));
        plainItem(itemModels, Tier5Items.LIQUID_FUEL_BURNER.get(), StrataIndustria.id("block/liquid_fuel_burner"));
    }

    // Spec 23.2: the mechanical pump's casing cut low with a motor housing on top (hand-made models, one per ACTIVE).
    private static void electricPump(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        PropertyDispatch.C2<MultiVariant, Direction, Boolean> dispatch = PropertyDispatch.initial(ElectricPumpBlock.FACING, ElectricPumpBlock.ACTIVE);
        for (boolean active : new boolean[] {false, true}) {
            horizontal(dispatch, active, BlockModelGenerators.plainVariant(StrataIndustria.id("block/electric_pump" + (active ? "_active" : ""))));
        }
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(Tier5Blocks.ELECTRIC_PUMP.get()).with(dispatch));
        plainItem(itemModels, Tier5Items.ELECTRIC_PUMP.get(), StrataIndustria.id("block/electric_pump"));
    }

    private static void horizontal(PropertyDispatch.C2<MultiVariant, Direction, Boolean> dispatch, boolean flag, MultiVariant model) {
        dispatch.select(Direction.NORTH, flag, model);
        dispatch.select(Direction.EAST, flag, model.with(BlockModelGenerators.Y_ROT_90));
        dispatch.select(Direction.SOUTH, flag, model.with(BlockModelGenerators.Y_ROT_180));
        dispatch.select(Direction.WEST, flag, model.with(BlockModelGenerators.Y_ROT_270));
    }

    private static final TextureSlot INNER = TextureSlot.create("inner");
    /** The dynamo's window geometry with the motor's own faces; the shaft is drawn by its renderer. */
    private static final ModelTemplate MOTOR = new ModelTemplate(Optional.of(StrataIndustria.id("block/kinetic_dynamo")), Optional.empty(),
            TextureSlot.PARTICLE, TextureSlot.FRONT, BACK, TextureSlot.SIDE, INNER);

    // Spec 10.11 and 23.2: shaft out of the front face (a window the rotor shows through), fan grille on the back.
    private static void kineticMotor(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        PropertyDispatch.C3<MultiVariant, Direction, ElectricTier, StatusLight> dispatch = PropertyDispatch.initial(KineticMotorBlock.FACING,
                KineticMotorBlock.TIER, KineticMotorBlock.STATUS);
        for (ElectricTier tier : ElectricTier.values()) {
            String t = tier.getSerializedName();
            for (StatusLight light : StatusLight.values()) {
                TextureMapping faces = new TextureMapping().put(TextureSlot.PARTICLE, texture("casing/" + t + "_side"))
                        .put(TextureSlot.FRONT, texture("kinetic_motor_front_" + t + "_" + light.getSerializedName()))
                        .put(BACK, texture("kinetic_motor_back_" + t)).put(TextureSlot.SIDE, texture("casing/" + t + "_side"))
                        .put(INNER, texture("kinetic_motor_inner"));
                MultiVariant model = BlockModelGenerators.plainVariant(MOTOR.create(StrataIndustria.id("block/kinetic_motor_" + t + "_" + light.getSerializedName()),
                        faces, blockModels.modelOutput));
                dispatch.select(Direction.NORTH, tier, light, model);
                dispatch.select(Direction.EAST, tier, light, model.with(BlockModelGenerators.Y_ROT_90));
                dispatch.select(Direction.SOUTH, tier, light, model.with(BlockModelGenerators.Y_ROT_180));
                dispatch.select(Direction.WEST, tier, light, model.with(BlockModelGenerators.Y_ROT_270));
                dispatch.select(Direction.UP, tier, light, model.with(BlockModelGenerators.X_ROT_270));
                dispatch.select(Direction.DOWN, tier, light, model.with(BlockModelGenerators.X_ROT_90));
            }
        }
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(Tier5Blocks.KINETIC_MOTOR.get()).with(dispatch));
        tieredItem(itemModels, Tier5Items.KINETIC_MOTOR.get(), "block/kinetic_motor_lv_off", "block/kinetic_motor_mv_off");
    }

    /** Spec 23.7: a machine item shows the MV casing once it carries the {@code machine_tier} component. */
    private static void tieredItem(ItemModelGenerators itemModels, Item item, String lv, String mv) {
        itemModels.itemModelOutput.accept(item, ItemModelUtils.conditional(new net.minecraft.client.renderer.item.properties.conditional.HasComponent(
                Tier5DataComponents.MACHINE_TIER.get(), false), ItemModelUtils.plainModel(StrataIndustria.id(mv)), ItemModelUtils.plainModel(StrataIndustria.id(lv))));
    }

    private static void plainItem(ItemModelGenerators itemModels, Item item, Identifier model) {
        itemModels.itemModelOutput.accept(item, ItemModelUtils.plainModel(model));
    }

    private static Material texture(String path) {
        return new Material(StrataIndustria.id("block/" + path));
    }
}
