package dev.strataindustria.transport.rail;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** The ore wagon (outposts spec 7.2): a steel-bodied hopper wagon with 27 slots that tips on a tipple like the tub. */
public class OreWagonEntity extends MineTubEntity {
    public static final int WAGON_SLOTS = 27;

    public OreWagonEntity(EntityType<? extends OreWagonEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected Item getDropItem() {
        return RailwayRegistry.ORE_WAGON.get();
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(RailwayRegistry.ORE_WAGON.get());
    }

    @Override
    public int getContainerSize() {
        return WAGON_SLOTS;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return ChestMenu.threeRows(containerId, inventory, this);
    }
}
