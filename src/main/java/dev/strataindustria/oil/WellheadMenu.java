package dev.strataindustria.oil;

import dev.strataindustria.registry.Tier6Menus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Wellhead screen: the casing slot, how deep the bore is and what is known of the reservoir. */
public class WellheadMenu extends AbstractContainerMenu {
    public static final int CASING_X = 80, CASING_Y = 20, INVENTORY_Y = 84;

    private final Container container;
    private final ContainerData data;
    private final BlockPos pos;

    public WellheadMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos(), new SimpleContainer(1), new SimpleContainerData(WellheadBlockEntity.DATA_COUNT));
    }

    public WellheadMenu(int id, Inventory inventory, BlockPos pos, Container container, ContainerData data) {
        super(Tier6Menus.WELLHEAD.get(), id);
        checkContainerSize(container, 1);
        this.container = container;
        this.data = data;
        this.pos = pos;
        addSlot(new Slot(container, WellheadBlockEntity.CASING, CASING_X + 1, CASING_Y + 1) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return container.canPlaceItem(WellheadBlockEntity.CASING, stack);
            }
        });
        addStandardInventorySlots(inventory, 8, INVENTORY_Y);
        addDataSlots(data);
    }

    public BlockPos pos() {
        return pos;
    }

    public WellheadBlockEntity.Status status() {
        int s = data.get(WellheadBlockEntity.DATA_STATUS);
        WellheadBlockEntity.Status[] values = WellheadBlockEntity.Status.values();
        return s >= 0 && s < values.length ? values[s] : WellheadBlockEntity.Status.NO_RESERVOIR;
    }

    public int bored() {
        return data.get(WellheadBlockEntity.DATA_BORED);
    }

    public int depth() {
        return data.get(WellheadBlockEntity.DATA_DEPTH);
    }

    /** Percent of the reservoir left, or -1 with no reservoir below. */
    public int remaining() {
        return data.get(WellheadBlockEntity.DATA_REMAINING);
    }

    public int nearX() {
        return data.get(WellheadBlockEntity.DATA_NEAR_X);
    }

    public int nearZ() {
        return data.get(WellheadBlockEntity.DATA_NEAR_Z);
    }

    /** mB per tick the well gave last tick. */
    public int rate() {
        return data.get(WellheadBlockEntity.DATA_RATE);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index == 0) {
            if (!moveItemStackTo(stack, 1, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!container.canPlaceItem(0, stack) || !moveItemStackTo(stack, 0, 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }
}
