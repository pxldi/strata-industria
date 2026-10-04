package dev.strataindustria.ceramics;

import dev.strataindustria.registry.PrologueRegistry;
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

/** Brick kiln screen: four pieces to fire, the fire between, four fired pieces. */
public class BrickKilnMenu extends AbstractContainerMenu {
    public static final int INPUT_X = 8, OUTPUT_X = 98, ROW_Y = 28, INVENTORY_Y = 84;

    private final Container container;
    private final ContainerData data;
    private final BlockPos pos;

    public BrickKilnMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos(), new SimpleContainer(BrickKilnBlockEntity.SLOTS), new SimpleContainerData(BrickKilnBlockEntity.DATA_COUNT));
    }

    public BrickKilnMenu(int id, Inventory inventory, BlockPos pos, Container container, ContainerData data) {
        super(PrologueRegistry.BRICK_KILN_MENU.get(), id);
        checkContainerSize(container, BrickKilnBlockEntity.SLOTS);
        this.container = container;
        this.data = data;
        this.pos = pos;
        for (int i = 0; i < BrickKilnBlockEntity.INPUTS; i++) {
            addSlot(new Slot(container, i, INPUT_X + i * 18, ROW_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return BrickKilnBlockEntity.fires(stack);
                }
            });
        }
        for (int i = 0; i < BrickKilnBlockEntity.OUTPUTS; i++) {
            addSlot(new Slot(container, BrickKilnBlockEntity.INPUTS + i, OUTPUT_X + i * 18, ROW_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }
        addStandardInventorySlots(inventory, 8, INVENTORY_Y);
        addDataSlots(data);
    }

    public BlockPos pos() {
        return pos;
    }

    /** How far the batch has fired, from 0 to 1. */
    public float progress() {
        return Math.min(1.0f, data.get(BrickKilnBlockEntity.DATA_PROGRESS) / 1000.0f);
    }

    public BrickKilnBlockEntity.Status status() {
        int s = data.get(BrickKilnBlockEntity.DATA_STATUS);
        var values = BrickKilnBlockEntity.Status.values();
        return s >= 0 && s < values.length ? values[s] : BrickKilnBlockEntity.Status.EMPTY;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int n = BrickKilnBlockEntity.SLOTS;
        if (index < n) {
            if (!moveItemStackTo(stack, n, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!BrickKilnBlockEntity.fires(stack) || !moveItemStackTo(stack, 0, BrickKilnBlockEntity.INPUTS, false)) {
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
