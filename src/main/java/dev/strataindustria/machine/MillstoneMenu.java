package dev.strataindustria.machine;

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

/** Millstone screen: input, progress arrow, output, and the status and network lines. */
public class MillstoneMenu extends AbstractContainerMenu {
    public static final int INPUT_X = 56, OUTPUT_X = 116, SLOT_Y = 35;
    public static final int INVENTORY_Y = 84;

    private final Container container;
    private final ContainerData data;
    private final BlockPos pos;

    public MillstoneMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos(), new SimpleContainer(2), new SimpleContainerData(MillstoneBlockEntity.DATA_COUNT));
    }

    public MillstoneMenu(int id, Inventory inventory, BlockPos pos, Container container, ContainerData data) {
        super(ModMenus.MILLSTONE.get(), id);
        checkContainerSize(container, 2);
        this.container = container;
        this.data = data;
        this.pos = pos;
        addSlot(new Slot(container, MillstoneBlockEntity.INPUT, INPUT_X, SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return container.canPlaceItem(MillstoneBlockEntity.INPUT, stack);
            }
        });
        addSlot(new Slot(container, MillstoneBlockEntity.OUTPUT, OUTPUT_X, SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addStandardInventorySlots(inventory, 8, INVENTORY_Y);
        addDataSlots(data);
    }

    public BlockPos pos() {
        return pos;
    }

    public float progress() {
        return Math.min(1.0f, data.get(MillstoneBlockEntity.DATA_PROGRESS) / 1000.0f);
    }

    public MillstoneBlockEntity.Status status() {
        int s = data.get(MillstoneBlockEntity.DATA_STATUS);
        return s >= 0 && s < MillstoneBlockEntity.Status.values().length ? MillstoneBlockEntity.Status.values()[s] : MillstoneBlockEntity.Status.EMPTY;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < 2) {
            if (!moveItemStackTo(stack, 2, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, 1, false)) {
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
