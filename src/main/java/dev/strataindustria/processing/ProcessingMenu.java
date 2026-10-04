package dev.strataindustria.processing;

import dev.strataindustria.registry.Tier4Menus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** An ore processing machine's screen: its inputs, one progress arrow each, the outputs and the status. */
public class ProcessingMenu extends AbstractContainerMenu {
    private final MachineLayout layout;
    private final Container container;
    private final ContainerData data;
    private final BlockPos pos;

    public ProcessingMenu(MachineLayout layout, int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(layout, id, inventory, buf.readBlockPos(), new SimpleContainer(layout.slots()), new SimpleContainerData(layout.dataCount()));
    }

    public ProcessingMenu(MachineLayout layout, int id, Inventory inventory, BlockPos pos, Container container, ContainerData data) {
        super(type(layout), id);
        checkContainerSize(container, layout.slots());
        this.layout = layout;
        this.container = container;
        this.data = data;
        this.pos = pos;
        for (int i = 0; i < layout.inputs(); i++) {
            int slot = i;
            addSlot(new Slot(container, slot, layout.inputX(), layout.inputY(i)) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return container.canPlaceItem(slot, stack);
                }
            });
        }
        for (int i = 0; i < layout.outputs(); i++) {
            addSlot(new Slot(container, layout.inputs() + i, layout.outputX(i), layout.outputY(i)) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }
        addStandardInventorySlots(inventory, 8, MachineLayout.INVENTORY_Y);
        addDataSlots(data);
    }

    private static MenuType<ProcessingMenu> type(MachineLayout layout) {
        return switch (layout) {
            case CRUSHER -> Tier4Menus.CRUSHER.get();
        };
    }

    public MachineLayout layout() {
        return layout;
    }

    public BlockPos pos() {
        return pos;
    }

    /** How far input {@code input} has got, 0 to 1. */
    public float progress(int input) {
        return Math.min(1.0f, data.get(input) / 1000.0f);
    }

    public ProcessingBlockEntity.Status status() {
        int s = data.get(layout.statusIndex());
        var values = ProcessingBlockEntity.Status.values();
        return s >= 0 && s < values.length ? values[s] : ProcessingBlockEntity.Status.EMPTY;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int machine = layout.slots();
        if (index < machine) {
            if (!moveItemStackTo(stack, machine, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!container.canPlaceItem(0, stack) || !moveItemStackTo(stack, 0, layout.inputs(), false)) {
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
