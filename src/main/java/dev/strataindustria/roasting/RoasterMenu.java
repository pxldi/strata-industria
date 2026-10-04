package dev.strataindustria.roasting;

import dev.strataindustria.registry.Tier4Menus;
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

/** Roaster screen: four slots of ore over four of calcine, the gas tank beside them, and the heat it gets. */
public class RoasterMenu extends AbstractContainerMenu {
    public static final int SLOT_X = 35, INPUT_Y = 17, OUTPUT_Y = 46, INVENTORY_Y = 100;

    private final Container container;
    private final ContainerData data;
    private final BlockPos pos;

    public RoasterMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos(), new SimpleContainer(RoasterBlockEntity.SLOTS), new SimpleContainerData(RoasterBlockEntity.DATA_COUNT));
    }

    public RoasterMenu(int id, Inventory inventory, BlockPos pos, Container container, ContainerData data) {
        super(Tier4Menus.ROASTER.get(), id);
        checkContainerSize(container, RoasterBlockEntity.SLOTS);
        this.container = container;
        this.data = data;
        this.pos = pos;
        for (int i = 0; i < RoasterBlockEntity.INPUTS; i++) {
            // The block entity knows the recipes; the client lets anything in and the server has the last word.
            addSlot(new Slot(container, i, SLOT_X + i * 18, INPUT_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return container.canPlaceItem(getContainerSlot(), stack);
                }
            });
        }
        for (int i = 0; i < RoasterBlockEntity.OUTPUTS; i++) {
            addSlot(new Slot(container, RoasterBlockEntity.INPUTS + i, SLOT_X + i * 18, OUTPUT_Y) {
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

    /** How far input slot {@code slot} has roasted, from 0 to 1. */
    public float progress(int slot) {
        return Math.min(1.0f, data.get(RoasterBlockEntity.DATA_PROGRESS + slot) / 1000.0f);
    }

    /** Sulfur dioxide in the tank, in mB. */
    public int gas() {
        return data.get(RoasterBlockEntity.DATA_GAS);
    }

    public RoasterBlockEntity.Status status() {
        int s = data.get(RoasterBlockEntity.DATA_STATUS);
        var values = RoasterBlockEntity.Status.values();
        return s >= 0 && s < values.length ? values[s] : RoasterBlockEntity.Status.EMPTY;
    }

    public int temperature() {
        return data.get(RoasterBlockEntity.DATA_TEMPERATURE);
    }

    public int heat() {
        return data.get(RoasterBlockEntity.DATA_HEAT);
    }

    public int limit() {
        return data.get(RoasterBlockEntity.DATA_LIMIT);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int n = RoasterBlockEntity.SLOTS;
        if (index < n) {
            if (!moveItemStackTo(stack, n, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, RoasterBlockEntity.INPUTS, false)) {
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
