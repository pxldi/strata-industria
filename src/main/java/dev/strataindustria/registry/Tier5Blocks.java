package dev.strataindustria.registry;

import dev.strataindustria.electric.BatteryBoxBlock;
import dev.strataindustria.electric.CombustionGeneratorBlockEntity;
import dev.strataindustria.electric.GeneratorBlock;
import dev.strataindustria.electric.SteamTurbineBlockEntity;
import dev.strataindustria.electric.CableBlock;
import dev.strataindustria.electric.ElectricHeaterBlockEntity;
import dev.strataindustria.electric.ElectricPumpBlock;
import dev.strataindustria.electric.EnergyAdapterBlock;
import dev.strataindustria.electric.KineticMotorBlock;
import dev.strataindustria.electric.LiquidFuelBurnerBlock;
import dev.strataindustria.electric.TransformerBlock;
import dev.strataindustria.electric.KineticDynamoBlock;
import dev.strataindustria.electric.machine.ElectricFurnaceBlockEntity;
import dev.strataindustria.electric.machine.ChemicalMachineBlock;
import dev.strataindustria.electric.machine.ElectricMachineBlock;
import dev.strataindustria.electric.machine.AssemblerBlockEntity;
import dev.strataindustria.electric.machine.ElectrolyserBlockEntity;
import dev.strataindustria.electric.machine.MixerBlockEntity;
import dev.strataindustria.electric.machine.BenderBlockEntity;
import dev.strataindustria.electric.machine.LatheBlock;
import dev.strataindustria.electric.machine.LatheBlockEntity;
import dev.strataindustria.electric.machine.MaceratorBlockEntity;
import dev.strataindustria.electric.machine.WiremillBlockEntity;
import dev.strataindustria.power.ElectricTier;
import dev.strataindustria.rubber.TreeTapBlock;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.common.util.DeferredSoundType;
import net.neoforged.neoforge.registries.DeferredBlock;

/** Tier 5 (electric) blocks, kept apart from the earlier tiers' blocks. */
public final class Tier5Blocks {
    /** Spec 23.6: cables thud softly, like wool a little higher. */
    public static final SoundType CABLE_SOUND = new SoundType(1.0f, 1.2f, SoundType.WOOL.getBreakSound(), SoundType.WOOL.getStepSound(),
            SoundType.WOOL.getPlaceSound(), SoundType.WOOL.getHitSound(), SoundType.WOOL.getFallSound());
    /** Spec 23.6: electric machines ring like metal, with the heavier netherite knock when hit. */
    public static final SoundType MACHINE_SOUND = new SoundType(1.0f, 1.0f, SoundType.METAL.getBreakSound(), SoundType.METAL.getStepSound(),
            SoundType.METAL.getPlaceSound(), SoundType.NETHERITE_BLOCK.getHitSound(), SoundType.METAL.getFallSound());

    /** Spec 23.6: the tap knocks in like wood with a metal tick; otherwise it sounds like the copper it is. */
    public static final SoundType TREE_TAP_SOUND = new DeferredSoundType(1.0f, 1.0f, () -> SoundEvents.COPPER_BREAK,
            () -> SoundEvents.COPPER_STEP, Tier5Sounds.TREE_TAP_PLACE, () -> SoundEvents.COPPER_HIT, () -> SoundEvents.COPPER_FALL);

    // Spec 5.1: the tree tap.
    public static final DeferredBlock<TreeTapBlock> TREE_TAP = ModBlocks.BLOCKS.registerBlock("tree_tap", TreeTapBlock::new,
            p -> p.mapColor(MapColor.COLOR_ORANGE)
                    .strength(0.8f)
                    .sound(TREE_TAP_SOUND)
                    .noOcclusion()
                    .pushReaction(PushReaction.POPPED));

    /** Spec 23.6: treated wood sounds like wood, a little lower. */
    public static final SoundType TREATED_WOOD_SOUND = new SoundType(1.0f, 0.9f, SoundType.WOOD.getBreakSound(), SoundType.WOOD.getStepSound(),
            SoundType.WOOD.getPlaceSound(), SoundType.WOOD.getHitSound(), SoundType.WOOD.getFallSound());

    // Spec 5.4 and 8.3: creosote-soaked logs, the poles cut from them and the insulators on the poles.
    public static final DeferredBlock<net.minecraft.world.level.block.RotatedPillarBlock> TREATED_LOG = ModBlocks.BLOCKS.registerBlock("treated_log",
            net.minecraft.world.level.block.RotatedPillarBlock::new,
            p -> p.mapColor(MapColor.COLOR_BROWN).strength(2.0f).sound(TREATED_WOOD_SOUND));
    public static final DeferredBlock<dev.strataindustria.electric.UtilityPoleBlock> UTILITY_POLE = ModBlocks.BLOCKS.registerBlock("utility_pole",
            dev.strataindustria.electric.UtilityPoleBlock::new,
            p -> p.mapColor(MapColor.COLOR_BROWN).strength(2.0f).sound(TREATED_WOOD_SOUND).noOcclusion());
    public static final DeferredBlock<dev.strataindustria.electric.PoleInsulatorBlock> POLE_INSULATOR = ModBlocks.BLOCKS.registerBlock("pole_insulator",
            dev.strataindustria.electric.PoleInsulatorBlock::new,
            p -> p.mapColor(MapColor.COLOR_ORANGE).strength(0.8f).sound(SoundType.DECORATED_POT).noOcclusion().pushReaction(PushReaction.POPPED));

    // Spec 8.1: rubber-insulated copper cables.
    public static final DeferredBlock<CableBlock> LV_CABLE = ModBlocks.BLOCKS.registerBlock("lv_cable", p -> new CableBlock(ElectricTier.LV, p),
            Tier5Blocks::cable);
    public static final DeferredBlock<CableBlock> MV_CABLE = ModBlocks.BLOCKS.registerBlock("mv_cable", p -> new CableBlock(ElectricTier.MV, p),
            Tier5Blocks::cable);

    // Spec 7.1 and 7.4: the first generator and the network's storage.
    public static final DeferredBlock<KineticDynamoBlock> KINETIC_DYNAMO = ModBlocks.BLOCKS.registerBlock("kinetic_dynamo", KineticDynamoBlock::new,
            p -> p.mapColor(MapColor.METAL)
                    .strength(3.5f, 6.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion());
    public static final DeferredBlock<BatteryBoxBlock> BATTERY_BOX = ModBlocks.BLOCKS.registerBlock("battery_box", BatteryBoxBlock::new,
            Tier5Blocks::machine);

    // Spec 7.2 and 7.3: the fuel-driven generators.
    public static final DeferredBlock<GeneratorBlock<SteamTurbineBlockEntity>> STEAM_TURBINE = ModBlocks.BLOCKS.registerBlock("steam_turbine",
            p -> new GeneratorBlock<>(Tier5BlockEntities.STEAM_TURBINE, SteamTurbineBlockEntity::new, p), Tier5Blocks::machine);
    public static final DeferredBlock<GeneratorBlock<CombustionGeneratorBlockEntity>> COMBUSTION_GENERATOR = ModBlocks.BLOCKS.registerBlock(
            "combustion_generator", p -> new GeneratorBlock<>(Tier5BlockEntities.COMBUSTION_GENERATOR, CombustionGeneratorBlockEntity::new, p),
            Tier5Blocks::machine);

    // Spec 7.5, 7.6, 10.10 and 10.11: heat from fuel and from the grid, water by motor, rotation by motor.
    public static final DeferredBlock<LiquidFuelBurnerBlock> LIQUID_FUEL_BURNER = ModBlocks.BLOCKS.registerBlock("liquid_fuel_burner",
            LiquidFuelBurnerBlock::new, p -> p.mapColor(MapColor.SAND)
                    .strength(2.5f, 8.0f)
                    .requiresCorrectToolForDrops()
                    .sound(Tier4Blocks.FIRE_BRICK_SOUND)
                    .lightLevel(state -> state.getValue(LiquidFuelBurnerBlock.LIT) ? 11 : 0));
    public static final DeferredBlock<GeneratorBlock<ElectricHeaterBlockEntity>> ELECTRIC_HEATER = ModBlocks.BLOCKS.registerBlock("electric_heater",
            p -> new GeneratorBlock<>(Tier5BlockEntities.ELECTRIC_HEATER, ElectricHeaterBlockEntity::new, p), Tier5Blocks::machine);
    public static final DeferredBlock<ElectricPumpBlock> ELECTRIC_PUMP = ModBlocks.BLOCKS.registerBlock("electric_pump", ElectricPumpBlock::new,
            p -> p.mapColor(MapColor.COLOR_BROWN).strength(3.0f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.COPPER).noOcclusion());
    public static final DeferredBlock<KineticMotorBlock> KINETIC_MOTOR = ModBlocks.BLOCKS.registerBlock("kinetic_motor", KineticMotorBlock::new,
            p -> p.mapColor(MapColor.METAL).strength(3.5f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion());

    /** Spec 9.5: the shell every electric machine is built around; also a decorative block. */
    public static final DeferredBlock<Block> LV_MACHINE_HULL = ModBlocks.BLOCKS.registerSimpleBlock("lv_machine_hull", Tier5Blocks::machine);

    // Spec 10.2 and 10.3: the first electric machines.
    public static final DeferredBlock<ElectricMachineBlock<ElectricFurnaceBlockEntity>> ELECTRIC_FURNACE = ModBlocks.BLOCKS.registerBlock(
            "electric_furnace", p -> new ElectricMachineBlock<>(Tier5BlockEntities.ELECTRIC_FURNACE, ElectricFurnaceBlockEntity::new, p),
            Tier5Blocks::machine);
    public static final DeferredBlock<ElectricMachineBlock<MaceratorBlockEntity>> MACERATOR = ModBlocks.BLOCKS.registerBlock(
            "macerator", p -> new ElectricMachineBlock<>(Tier5BlockEntities.MACERATOR, MaceratorBlockEntity::new, p),
            Tier5Blocks::machine);
    // Spec 10.4 to 10.6: the shaping machines.
    public static final DeferredBlock<ElectricMachineBlock<WiremillBlockEntity>> WIREMILL = ModBlocks.BLOCKS.registerBlock(
            "wiremill", p -> new ElectricMachineBlock<>(Tier5BlockEntities.WIREMILL, WiremillBlockEntity::new, p), Tier5Blocks::machine);
    public static final DeferredBlock<ElectricMachineBlock<BenderBlockEntity>> BENDER = ModBlocks.BLOCKS.registerBlock(
            "bender", p -> new ElectricMachineBlock<>(Tier5BlockEntities.BENDER, BenderBlockEntity::new, p), Tier5Blocks::machine);
    public static final DeferredBlock<LatheBlock> LATHE = ModBlocks.BLOCKS.registerBlock(
            "lathe", p -> new LatheBlock(Tier5BlockEntities.LATHE, LatheBlockEntity::new, p), Tier5Blocks::machine);

    // Spec 11.1 and 11.2: the chemistry machines.
    public static final DeferredBlock<ChemicalMachineBlock<MixerBlockEntity>> MIXER = ModBlocks.BLOCKS.registerBlock(
            "mixer", p -> new ChemicalMachineBlock<>(Tier5BlockEntities.MIXER, MixerBlockEntity::new, p), Tier5Blocks::machine);
    public static final DeferredBlock<ChemicalMachineBlock<ElectrolyserBlockEntity>> ELECTROLYSER = ModBlocks.BLOCKS.registerBlock(
            "electrolyser", p -> new ChemicalMachineBlock<>(Tier5BlockEntities.ELECTROLYSER, ElectrolyserBlockEntity::new, p), Tier5Blocks::machine);

    public static final DeferredBlock<ChemicalMachineBlock<AssemblerBlockEntity>> ASSEMBLER = ModBlocks.BLOCKS.registerBlock(
            "assembler", p -> new ChemicalMachineBlock<>(Tier5BlockEntities.ASSEMBLER, AssemblerBlockEntity::new, p), Tier5Blocks::machine);

    // Spec 10.8 and 10.9: the power hammer and the (MV only) extruder.
    public static final DeferredBlock<dev.strataindustria.electric.PowerHammerBlock> POWER_HAMMER = ModBlocks.BLOCKS.registerBlock("power_hammer",
            dev.strataindustria.electric.PowerHammerBlock::new, p -> machine(p).noOcclusion());
    public static final DeferredBlock<dev.strataindustria.electric.machine.ExtruderBlock> EXTRUDER = ModBlocks.BLOCKS.registerBlock(
            "extruder", p -> new dev.strataindustria.electric.machine.ExtruderBlock(Tier5BlockEntities.EXTRUDER,
                    dev.strataindustria.electric.machine.ExtruderBlockEntity::new, p), Tier5Blocks::machine);

    // Spec 8.2 and 8.5: the transformer and the energy adapter.
    public static final DeferredBlock<TransformerBlock> TRANSFORMER = ModBlocks.BLOCKS.registerBlock("transformer", TransformerBlock::new,
            Tier5Blocks::machine);
    public static final DeferredBlock<EnergyAdapterBlock> ENERGY_ADAPTER = ModBlocks.BLOCKS.registerBlock("energy_adapter", EnergyAdapterBlock::new,
            Tier5Blocks::machine);

    /** Spec 9.5: every machine that comes in LV and MV; the kit upgrades these and they drop with their tier. */
    public static java.util.List<DeferredBlock<? extends Block>> upgradable() {
        return java.util.List.of(BATTERY_BOX, ELECTRIC_FURNACE, MACERATOR, WIREMILL, BENDER, LATHE, MIXER, ELECTROLYSER, ASSEMBLER, POWER_HAMMER, STEAM_TURBINE,
                COMBUSTION_GENERATOR, ENERGY_ADAPTER, ELECTRIC_HEATER, KINETIC_MOTOR);
    }

    private static Block.Properties cable(Block.Properties p) {
        return p.mapColor(MapColor.COLOR_BLACK)
                .strength(0.4f)
                .sound(CABLE_SOUND)
                .noOcclusion()
                .pushReaction(PushReaction.POPPED);
    }

    /** Riveted steel casings: mined with a pickaxe. */
    private static Block.Properties machine(Block.Properties p) {
        return p.mapColor(MapColor.METAL)
                .strength(4.0f, 6.0f)
                .requiresCorrectToolForDrops()
                .sound(MACHINE_SOUND);
    }

    public static void init() {}

    private Tier5Blocks() {}
}
