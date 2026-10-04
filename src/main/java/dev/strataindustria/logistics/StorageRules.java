package dev.strataindustria.logistics;

import dev.strataindustria.ceramics.LargeVesselBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;

/** Which inventories the storage controller takes in (tier 5 spec 12.3): plain containers, never machines. */
public final class StorageRules {
    private StorageRules() {}

    /** Chests, barrels, shulker boxes, large vessels and the like: anything that only holds items. */
    public static boolean plain(BlockEntity entity) {
        if (entity instanceof LargeVesselBlockEntity) return true;
        return entity instanceof RandomizableContainerBlockEntity && !(entity instanceof HopperBlockEntity) && !(entity instanceof DispenserBlockEntity);
    }
}
