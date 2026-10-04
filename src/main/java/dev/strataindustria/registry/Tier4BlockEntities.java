package dev.strataindustria.registry;

import dev.strataindustria.coking.CokeOvenBlockEntity;
import dev.strataindustria.fluid.PressureGaugeBlockEntity;
import dev.strataindustria.metal.CrucibleBlockEntity;
import dev.strataindustria.power.IronTransmission;
import dev.strataindustria.steam.BoilerBlockEntity;
import dev.strataindustria.steam.FireboxBlockEntity;
import dev.strataindustria.steam.MechanicalPumpBlockEntity;
import dev.strataindustria.steam.SteamEngineBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Tier 4 block entity types. */
public final class Tier4BlockEntities {
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CokeOvenBlockEntity>> COKE_OVEN =
            ModBlockEntities.BLOCK_ENTITIES.register("coke_oven", () -> new BlockEntityType<>(CokeOvenBlockEntity::new, Tier4Blocks.COKE_OVEN_DOOR.get()));
    /** Spec 6.1: the refractory crucible is a crucible with a bigger pot and a hotter limit. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrucibleBlockEntity>> REFRACTORY_CRUCIBLE =
            ModBlockEntities.BLOCK_ENTITIES.register("refractory_crucible", () -> new BlockEntityType<>(
                    (pos, state) -> new CrucibleBlockEntity(Tier4BlockEntities.REFRACTORY_CRUCIBLE.get(), pos, state),
                    Tier4Blocks.REFRACTORY_CRUCIBLE.get()));

    /** Spec 11.7: iron axles and gearboxes, which take up to 256 RPM. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IronTransmission.Entity>> IRON_TRANSMISSION =
            ModBlockEntities.BLOCK_ENTITIES.register("iron_transmission", () -> new BlockEntityType<>(IronTransmission.Entity::new,
                    Tier4Blocks.IRON_AXLE.get(), Tier4Blocks.IRON_GEARBOX.get(), Tier4Blocks.IRON_STEP_UP_GEARBOX.get()));

    // Spec 8.1, 9.3 and 10.2: steam.
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FireboxBlockEntity>> FIREBOX =
            ModBlockEntities.BLOCK_ENTITIES.register("firebox", () -> new BlockEntityType<>(FireboxBlockEntity::new, Tier4Blocks.FIREBOX.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BoilerBlockEntity>> BRONZE_BOILER =
            ModBlockEntities.BLOCK_ENTITIES.register("bronze_boiler", () -> new BlockEntityType<>(BoilerBlockEntity::new, Tier4Blocks.BRONZE_BOILER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PressureGaugeBlockEntity>> PRESSURE_GAUGE =
            ModBlockEntities.BLOCK_ENTITIES.register("pressure_gauge", () -> new BlockEntityType<>(PressureGaugeBlockEntity::new,
                    Tier4Blocks.PRESSURE_GAUGE.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MechanicalPumpBlockEntity>> MECHANICAL_PUMP =
            ModBlockEntities.BLOCK_ENTITIES.register("mechanical_pump", () -> new BlockEntityType<>(MechanicalPumpBlockEntity::new,
                    Tier4Blocks.MECHANICAL_PUMP.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SteamEngineBlockEntity>> STEAM_ENGINE =
            ModBlockEntities.BLOCK_ENTITIES.register("steam_engine", () -> new BlockEntityType<>(SteamEngineBlockEntity::new,
                    Tier4Blocks.STEAM_ENGINE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.strataindustria.processing.CrusherBlockEntity>> CRUSHER =
            ModBlockEntities.BLOCK_ENTITIES.register("crusher", () -> new BlockEntityType<>(dev.strataindustria.processing.CrusherBlockEntity::new,
                    Tier4Blocks.CRUSHER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.strataindustria.processing.WasherBlockEntity>> WASHER =
            ModBlockEntities.BLOCK_ENTITIES.register("washer", () -> new BlockEntityType<>(dev.strataindustria.processing.WasherBlockEntity::new,
                    Tier4Blocks.WASHER.get()));

    // Spec 12.1 and 11.6: the blast furnace, its hatches, and the blower.
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.strataindustria.ironworks.BlastFurnaceBlockEntity>> BLAST_FURNACE =
            ModBlockEntities.BLOCK_ENTITIES.register("blast_furnace", () -> new BlockEntityType<>(dev.strataindustria.ironworks.BlastFurnaceBlockEntity::new,
                    Tier4Blocks.BLAST_FURNACE_CONTROLLER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.strataindustria.ironworks.ConverterBlockEntity>> CONVERTER =
            ModBlockEntities.BLOCK_ENTITIES.register("converter", () -> new BlockEntityType<>(dev.strataindustria.ironworks.ConverterBlockEntity::new,
                    Tier4Blocks.CONVERTER_CONTROLLER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.strataindustria.ironworks.FurnaceHatchBlockEntity>> FURNACE_HATCH =
            ModBlockEntities.BLOCK_ENTITIES.register("furnace_hatch", () -> new BlockEntityType<>(dev.strataindustria.ironworks.FurnaceHatchBlockEntity::new,
                    Tier4Blocks.CHARGING_HATCH.get(), Tier4Blocks.TAP_HATCH.get()));
    // Spec 8.6: the kiln.
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.strataindustria.ceramics.KilnBlockEntity>> KILN =
            ModBlockEntities.BLOCK_ENTITIES.register("kiln", () -> new BlockEntityType<>(dev.strataindustria.ceramics.KilnBlockEntity::new,
                    Tier4Blocks.KILN.get()));
    // Spec 8.5: the roaster.
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.strataindustria.roasting.RoasterBlockEntity>> ROASTER =
            ModBlockEntities.BLOCK_ENTITIES.register("roaster", () -> new BlockEntityType<>(dev.strataindustria.roasting.RoasterBlockEntity::new,
                    Tier4Blocks.ROASTER.get()));
    // Spec 8.7: the smelter.
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.strataindustria.metal.SmelterBlockEntity>> SMELTER =
            ModBlockEntities.BLOCK_ENTITIES.register("smelter", () -> new BlockEntityType<>(dev.strataindustria.metal.SmelterBlockEntity::new,
                    Tier4Blocks.SMELTER.get()));
    // Spec 8.4: the heat inlet's link to its multiblock.
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.strataindustria.heat.HeatInletBlockEntity>> HEAT_INLET =
            ModBlockEntities.BLOCK_ENTITIES.register("heat_inlet", () -> new BlockEntityType<>(dev.strataindustria.heat.HeatInletBlockEntity::new,
                    Tier4Blocks.HEAT_INLET.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.strataindustria.ironworks.BlowerBlockEntity>> BLOWER =
            ModBlockEntities.BLOCK_ENTITIES.register("blower", () -> new BlockEntityType<>(dev.strataindustria.ironworks.BlowerBlockEntity::new,
                    Tier4Blocks.BLOWER.get()));

    public static void init() {}

    private Tier4BlockEntities() {}
}
