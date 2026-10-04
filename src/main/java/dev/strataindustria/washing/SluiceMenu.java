package dev.strataindustria.washing;

import dev.strataindustria.registry.ModMenus;
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

/** Sluice screen: the four waiting items and the status line. */
public class SluiceMenu extends AbstractContainerMenu {
    public static final int SLOT_X = 53, SLOT_Y = 35, INVENTORY_Y = 84;

    private final Container container;
    private final ContainerData data;
    private final BlockPos pos;

    public SluiceMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos(), new SimpleContainer(SluiceBlockEntity.SLOTS), new SimpleContainerData(SluiceBlockEntity.DATA_COUNT));
    }

    public SluiceMenu(int id, Inventory inventory, BlockPos pos, Container container, ContainerData data) {
        super(ModMenus.SLUICE.get(), id);
        checkContainerSize(container, SluiceBlockEntity.SLOTS);
        this.container = container;
        this.data = data;
        this.pos = pos;
        for (int i = 0; i < SluiceBlockEntity.SLOTS; i++) {
            int slot = i;
            addSlot(new Slot(container, slot, SLOT_X + 1 + i * 18, SLOT_Y + 1) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return container.canPlaceItem(slot, stack);
                }
            });
        }
        addStandardInventorySlots(inventory, 8, INVENTORY_Y);
        addDataSlots(data);
    }

    public BlockPos pos() {
        return pos;
    }

    public float progress() {
        return Math.min(1.0f, data.get(SluiceBlockEntity.DATA_PROGRESS) / 1000.0f);
    }

    public SluiceBlockEntity.Status status() {
        int s = data.get(SluiceBlockEntity.DATA_STATUS);
        return s >= 0 && s < SluiceBlockEntity.Status.values().length ? SluiceBlockEntity.Status.values()[s] : SluiceBlockEntity.Status.EMPTY;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int n = SluiceBlockEntity.SLOTS;
        if (index < n) {
            if (!moveItemStackTo(stack, n, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, n, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }
}
