package dev.strataindustria.registry;

import dev.strataindustria.machine.BellowsBlockEntity;
import dev.strataindustria.machine.MillstoneBlockEntity;
import dev.strataindustria.machine.SawMillBlockEntity;
import dev.strataindustria.machine.TripHammerBlockEntity;
import dev.strataindustria.power.HandCrankBlockEntity;
import dev.strataindustria.power.KineticBlockEntity;
import dev.strataindustria.power.WaterWheelBlockEntity;
import dev.strataindustria.bloomery.BloomeryBlockEntity;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.charcoal.LogPileBlockEntity;
import dev.strataindustria.fire.FirePitBlockEntity;
import dev.strataindustria.forge.ForgeBlockEntity;
import dev.strataindustria.metal.CrucibleBlockEntity;
import dev.strataindustria.quern.QuernBlockEntity;
import dev.strataindustria.smithing.AnvilBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, StrataIndustria.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FirePitBlockEntity>> FIRE_PIT =
            BLOCK_ENTITIES.register("fire_pit", () -> new BlockEntityType<>(FirePitBlockEntity::new, ModBlocks.FIRE_PIT.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LogPileBlockEntity>> LOG_PILE =
            BLOCK_ENTITIES.register("log_pile", () -> new BlockEntityType<>(LogPileBlockEntity::new, ModBlocks.LOG_PILE.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ForgeBlockEntity>> FORGE =
            BLOCK_ENTITIES.register("forge", () -> new BlockEntityType<>(ForgeBlockEntity::new, ModBlocks.FORGE.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<QuernBlockEntity>> QUERN =
            BLOCK_ENTITIES.register("quern", () -> new BlockEntityType<>(QuernBlockEntity::new, ModBlocks.QUERN.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AnvilBlockEntity>> ANVIL =
            BLOCK_ENTITIES.register("anvil", () -> {
                java.util.List<net.minecraft.world.level.block.Block> anvils = new java.util.ArrayList<>();
                ModBlocks.STONE_ANVILS.values().forEach(b -> anvils.add(b.get()));
                anvils.add(ModBlocks.IRON_ANVIL.get());
                return new BlockEntityType<>(AnvilBlockEntity::new, anvils.toArray(new net.minecraft.world.level.block.Block[0]));
            });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrucibleBlockEntity>> CRUCIBLE =
            BLOCK_ENTITIES.register("crucible", () -> new BlockEntityType<>(CrucibleBlockEntity::new, ModBlocks.CRUCIBLE.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BloomeryBlockEntity>> BLOOMERY =
            BLOCK_ENTITIES.register("bloomery", () -> new BlockEntityType<>(BloomeryBlockEntity::new, ModBlocks.BLOOMERY.get()));

    // Tier 3 spec 7 and 8: axles and gearboxes share one block entity type.
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<KineticBlockEntity>> KINETIC_TRANSMISSION =
            BLOCK_ENTITIES.register("kinetic_transmission", () -> new BlockEntityType<>(
                    (pos, state) -> new KineticBlockEntity(ModBlockEntities.KINETIC_TRANSMISSION.get(), pos, state),
                    ModBlocks.WOODEN_AXLE.get(), ModBlocks.WOODEN_GEARBOX.get(), ModBlocks.STEP_UP_GEARBOX.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.strataindustria.power.PulleyBlockEntity>> PULLEY =
            BLOCK_ENTITIES.register("pulley", () -> new BlockEntityType<>(dev.strataindustria.power.PulleyBlockEntity::new, ModBlocks.PULLEY.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.strataindustria.power.WindmillBearingBlockEntity>> WINDMILL_BEARING =
            BLOCK_ENTITIES.register("windmill_bearing", () -> new BlockEntityType<>(dev.strataindustria.power.WindmillBearingBlockEntity::new,
                    ModBlocks.WINDMILL_BEARING.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.strataindustria.tanning.SoakingBarrelBlockEntity>> SOAKING_BARREL =
            BLOCK_ENTITIES.register("soaking_barrel", () -> new BlockEntityType<>(dev.strataindustria.tanning.SoakingBarrelBlockEntity::new,
                    ModBlocks.SOAKING_BARREL.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HandCrankBlockEntity>> HAND_CRANK =
            BLOCK_ENTITIES.register("hand_crank", () -> new BlockEntityType<>(HandCrankBlockEntity::new, ModBlocks.HAND_CRANK.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WaterWheelBlockEntity>> WATER_WHEEL =
            BLOCK_ENTITIES.register("water_wheel", () -> new BlockEntityType<>(WaterWheelBlockEntity::new, ModBlocks.WATER_WHEEL.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MillstoneBlockEntity>> MILLSTONE =
            BLOCK_ENTITIES.register("millstone", () -> new BlockEntityType<>(MillstoneBlockEntity::new, ModBlocks.MILLSTONE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BellowsBlockEntity>> BELLOWS =
            BLOCK_ENTITIES.register("bellows", () -> new BlockEntityType<>(BellowsBlockEntity::new, ModBlocks.BELLOWS.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SawMillBlockEntity>> SAW_MILL =
            BLOCK_ENTITIES.register("saw_mill", () -> new BlockEntityType<>(SawMillBlockEntity::new, ModBlocks.SAW_MILL.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.strataindustria.machine.CoreSamplerBlockEntity>> CORE_SAMPLER =
            BLOCK_ENTITIES.register("core_sampler", () -> new BlockEntityType<>(dev.strataindustria.machine.CoreSamplerBlockEntity::new,
                    ModBlocks.CORE_SAMPLER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.strataindustria.washing.SluiceBlockEntity>> SLUICE =
            BLOCK_ENTITIES.register("sluice", () -> new BlockEntityType<>(dev.strataindustria.washing.SluiceBlockEntity::new,
                    ModBlocks.SLUICE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TripHammerBlockEntity>> TRIP_HAMMER =
            BLOCK_ENTITIES.register("trip_hammer", () -> new BlockEntityType<>(TripHammerBlockEntity::new, ModBlocks.TRIP_HAMMER.get()));

    private ModBlockEntities() {}
}
