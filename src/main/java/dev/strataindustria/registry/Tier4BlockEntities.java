package dev.strataindustria.registry;

import dev.strataindustria.coking.CokeOvenBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Tier 4 block entity types. */
public final class Tier4BlockEntities {
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CokeOvenBlockEntity>> COKE_OVEN =
            ModBlockEntities.BLOCK_ENTITIES.register("coke_oven", () -> new BlockEntityType<>(CokeOvenBlockEntity::new, Tier4Blocks.COKE_OVEN_DOOR.get()));

    public static void init() {}

    private Tier4BlockEntities() {}
}
