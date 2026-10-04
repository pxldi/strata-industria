package dev.strataindustria.registry;

import dev.strataindustria.oil.OilStillBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Tier 6 block entity types. */
public final class Tier6BlockEntities {
    /** Spec 5.5: the oil still. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OilStillBlockEntity>> OIL_STILL =
            ModBlockEntities.BLOCK_ENTITIES.register("oil_still", () -> new BlockEntityType<>(OilStillBlockEntity::new, Tier6Blocks.OIL_STILL.get()));

    public static void init() {}

    private Tier6BlockEntities() {}
}
