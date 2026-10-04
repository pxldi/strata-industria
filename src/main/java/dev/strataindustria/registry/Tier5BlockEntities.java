package dev.strataindustria.registry;

import dev.strataindustria.electric.BatteryBoxBlockEntity;
import dev.strataindustria.electric.CableBlockEntity;
import dev.strataindustria.electric.KineticDynamoBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Tier 5 block entity types. */
public final class Tier5BlockEntities {
    /** Spec 16.1: one type shared by both cables; it never ticks. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CableBlockEntity>> CABLE =
            ModBlockEntities.BLOCK_ENTITIES.register("cable", () -> new BlockEntityType<>(CableBlockEntity::new,
                    Tier5Blocks.LV_CABLE.get(), Tier5Blocks.MV_CABLE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<KineticDynamoBlockEntity>> KINETIC_DYNAMO =
            ModBlockEntities.BLOCK_ENTITIES.register("kinetic_dynamo", () -> new BlockEntityType<>(KineticDynamoBlockEntity::new,
                    Tier5Blocks.KINETIC_DYNAMO.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BatteryBoxBlockEntity>> BATTERY_BOX =
            ModBlockEntities.BLOCK_ENTITIES.register("battery_box", () -> new BlockEntityType<>(BatteryBoxBlockEntity::new,
                    Tier5Blocks.BATTERY_BOX.get()));

    public static void init() {}

    private Tier5BlockEntities() {}
}
