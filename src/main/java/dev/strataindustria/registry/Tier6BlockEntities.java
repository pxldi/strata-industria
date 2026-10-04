package dev.strataindustria.registry;

import dev.strataindustria.oil.OilStillBlockEntity;
import dev.strataindustria.oil.PumpJackBlockEntity;
import dev.strataindustria.oil.WellheadBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Tier 6 block entity types. */
public final class Tier6BlockEntities {
    /** Spec 5.5: the oil still. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OilStillBlockEntity>> OIL_STILL =
            ModBlockEntities.BLOCK_ENTITIES.register("oil_still", () -> new BlockEntityType<>(OilStillBlockEntity::new, Tier6Blocks.OIL_STILL.get()));

    /** Spec 5.3: the wellhead. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WellheadBlockEntity>> WELLHEAD =
            ModBlockEntities.BLOCK_ENTITIES.register("wellhead", () -> new BlockEntityType<>(WellheadBlockEntity::new, Tier6Blocks.WELLHEAD.get()));
    /** Spec 5.4: the pump jack. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PumpJackBlockEntity>> PUMP_JACK =
            ModBlockEntities.BLOCK_ENTITIES.register("pump_jack", () -> new BlockEntityType<>(PumpJackBlockEntity::new, Tier6Blocks.PUMP_JACK.get()));

    public static void init() {}

    private Tier6BlockEntities() {}
}
