package dev.strataindustria.registry;

import dev.strataindustria.electric.BatteryBoxBlockEntity;
import dev.strataindustria.electric.CableBlockEntity;
import dev.strataindustria.electric.EnergyAdapterBlockEntity;
import dev.strataindustria.electric.TransformerBlockEntity;
import dev.strataindustria.electric.CombustionGeneratorBlockEntity;
import dev.strataindustria.electric.ElectricHeaterBlockEntity;
import dev.strataindustria.electric.ElectricPumpBlockEntity;
import dev.strataindustria.electric.KineticMotorBlockEntity;
import dev.strataindustria.electric.LiquidFuelBurnerBlockEntity;
import dev.strataindustria.electric.SteamTurbineBlockEntity;
import dev.strataindustria.electric.KineticDynamoBlockEntity;
import dev.strataindustria.electric.machine.ElectricFurnaceBlockEntity;
import dev.strataindustria.electric.machine.AssemblerBlockEntity;
import dev.strataindustria.electric.machine.ElectrolyserBlockEntity;
import dev.strataindustria.electric.machine.MixerBlockEntity;
import dev.strataindustria.electric.machine.BenderBlockEntity;
import dev.strataindustria.electric.machine.LatheBlockEntity;
import dev.strataindustria.electric.machine.MaceratorBlockEntity;
import dev.strataindustria.electric.machine.WiremillBlockEntity;
import dev.strataindustria.rubber.TreeTapBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Tier 5 block entity types. */
public final class Tier5BlockEntities {
    /** Spec 16.1: one type shared by both cables; it never ticks. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CableBlockEntity>> CABLE =
            ModBlockEntities.BLOCK_ENTITIES.register("cable", () -> new BlockEntityType<>(CableBlockEntity::new,
                    Tier5Blocks.LV_CABLE.get(), Tier5Blocks.MV_CABLE.get()));
    /** Spec 16.1: poles only carry the network; insulators hold the span list. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.strataindustria.electric.UtilityPoleBlockEntity>> UTILITY_POLE =
            ModBlockEntities.BLOCK_ENTITIES.register("utility_pole", () -> new BlockEntityType<>(dev.strataindustria.electric.UtilityPoleBlockEntity::new,
                    Tier5Blocks.UTILITY_POLE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.strataindustria.electric.PoleInsulatorBlockEntity>> POLE_INSULATOR =
            ModBlockEntities.BLOCK_ENTITIES.register("pole_insulator", () -> new BlockEntityType<>(dev.strataindustria.electric.PoleInsulatorBlockEntity::new,
                    Tier5Blocks.POLE_INSULATOR.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<KineticDynamoBlockEntity>> KINETIC_DYNAMO =
            ModBlockEntities.BLOCK_ENTITIES.register("kinetic_dynamo", () -> new BlockEntityType<>(KineticDynamoBlockEntity::new,
                    Tier5Blocks.KINETIC_DYNAMO.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BatteryBoxBlockEntity>> BATTERY_BOX =
            ModBlockEntities.BLOCK_ENTITIES.register("battery_box", () -> new BlockEntityType<>(BatteryBoxBlockEntity::new,
                    Tier5Blocks.BATTERY_BOX.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TransformerBlockEntity>> TRANSFORMER =
            ModBlockEntities.BLOCK_ENTITIES.register("transformer", () -> new BlockEntityType<>(TransformerBlockEntity::new,
                    Tier5Blocks.TRANSFORMER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyAdapterBlockEntity>> ENERGY_ADAPTER =
            ModBlockEntities.BLOCK_ENTITIES.register("energy_adapter", () -> new BlockEntityType<>(EnergyAdapterBlockEntity::new,
                    Tier5Blocks.ENERGY_ADAPTER.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TreeTapBlockEntity>> TREE_TAP =
            ModBlockEntities.BLOCK_ENTITIES.register("tree_tap", () -> new BlockEntityType<>(TreeTapBlockEntity::new,
                    Tier5Blocks.TREE_TAP.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectricFurnaceBlockEntity>> ELECTRIC_FURNACE =
            ModBlockEntities.BLOCK_ENTITIES.register("electric_furnace", () -> new BlockEntityType<>(ElectricFurnaceBlockEntity::new,
                    Tier5Blocks.ELECTRIC_FURNACE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MaceratorBlockEntity>> MACERATOR =
            ModBlockEntities.BLOCK_ENTITIES.register("macerator", () -> new BlockEntityType<>(MaceratorBlockEntity::new,
                    Tier5Blocks.MACERATOR.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WiremillBlockEntity>> WIREMILL =
            ModBlockEntities.BLOCK_ENTITIES.register("wiremill", () -> new BlockEntityType<>(WiremillBlockEntity::new, Tier5Blocks.WIREMILL.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BenderBlockEntity>> BENDER =
            ModBlockEntities.BLOCK_ENTITIES.register("bender", () -> new BlockEntityType<>(BenderBlockEntity::new, Tier5Blocks.BENDER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LatheBlockEntity>> LATHE =
            ModBlockEntities.BLOCK_ENTITIES.register("lathe", () -> new BlockEntityType<>(LatheBlockEntity::new, Tier5Blocks.LATHE.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MixerBlockEntity>> MIXER =
            ModBlockEntities.BLOCK_ENTITIES.register("mixer", () -> new BlockEntityType<>(MixerBlockEntity::new, Tier5Blocks.MIXER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.strataindustria.electric.PowerHammerBlockEntity>> POWER_HAMMER =
            ModBlockEntities.BLOCK_ENTITIES.register("power_hammer", () -> new BlockEntityType<>(dev.strataindustria.electric.PowerHammerBlockEntity::new,
                    Tier5Blocks.POWER_HAMMER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.strataindustria.electric.machine.ExtruderBlockEntity>> EXTRUDER =
            ModBlockEntities.BLOCK_ENTITIES.register("extruder", () -> new BlockEntityType<>(dev.strataindustria.electric.machine.ExtruderBlockEntity::new,
                    Tier5Blocks.EXTRUDER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AssemblerBlockEntity>> ASSEMBLER =
            ModBlockEntities.BLOCK_ENTITIES.register("assembler", () -> new BlockEntityType<>(AssemblerBlockEntity::new, Tier5Blocks.ASSEMBLER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectrolyserBlockEntity>> ELECTROLYSER =
            ModBlockEntities.BLOCK_ENTITIES.register("electrolyser", () -> new BlockEntityType<>(ElectrolyserBlockEntity::new,
                    Tier5Blocks.ELECTROLYSER.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SteamTurbineBlockEntity>> STEAM_TURBINE =
            ModBlockEntities.BLOCK_ENTITIES.register("steam_turbine", () -> new BlockEntityType<>(SteamTurbineBlockEntity::new,
                    Tier5Blocks.STEAM_TURBINE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CombustionGeneratorBlockEntity>> COMBUSTION_GENERATOR =
            ModBlockEntities.BLOCK_ENTITIES.register("combustion_generator", () -> new BlockEntityType<>(CombustionGeneratorBlockEntity::new,
                    Tier5Blocks.COMBUSTION_GENERATOR.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LiquidFuelBurnerBlockEntity>> LIQUID_FUEL_BURNER =
            ModBlockEntities.BLOCK_ENTITIES.register("liquid_fuel_burner", () -> new BlockEntityType<>(LiquidFuelBurnerBlockEntity::new,
                    Tier5Blocks.LIQUID_FUEL_BURNER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectricHeaterBlockEntity>> ELECTRIC_HEATER =
            ModBlockEntities.BLOCK_ENTITIES.register("electric_heater", () -> new BlockEntityType<>(ElectricHeaterBlockEntity::new,
                    Tier5Blocks.ELECTRIC_HEATER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectricPumpBlockEntity>> ELECTRIC_PUMP =
            ModBlockEntities.BLOCK_ENTITIES.register("electric_pump", () -> new BlockEntityType<>(ElectricPumpBlockEntity::new,
                    Tier5Blocks.ELECTRIC_PUMP.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<KineticMotorBlockEntity>> KINETIC_MOTOR =
            ModBlockEntities.BLOCK_ENTITIES.register("kinetic_motor", () -> new BlockEntityType<>(KineticMotorBlockEntity::new,
                    Tier5Blocks.KINETIC_MOTOR.get()));

    public static void init() {}

    private Tier5BlockEntities() {}
}
