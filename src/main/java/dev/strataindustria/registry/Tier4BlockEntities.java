package dev.strataindustria.registry;

import dev.strataindustria.coking.CokeOvenBlockEntity;
import dev.strataindustria.metal.CrucibleBlockEntity;
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

    public static void init() {}

    private Tier4BlockEntities() {}
}
