package dev.strataindustria.registry;

import dev.strataindustria.coking.CokeOvenBlockEntity;
import dev.strataindustria.metal.CrucibleBlockEntity;
import dev.strataindustria.power.IronTransmission;
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

    public static void init() {}

    private Tier4BlockEntities() {}
}
