package dev.strataindustria.ceramics;

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

/** Kiln screen: eight pieces to fire, the fire between, eight fired pieces, and the heat it gets. */
public class KilnMenu extends AbstractContainerMenu {
    public static final int INPUT_X = 8, OUTPUT_X = 98, ROW_Y = 20, INVENTORY_Y = 94;

    private final Container container;
    private final ContainerData data;
    private final BlockPos pos;

    public KilnMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos(), new SimpleContainer(KilnBlockEntity.SLOTS), new SimpleContainerData(KilnBlockEntity.DATA_COUNT));
    }

    public KilnMenu(int id, Inventory inventory, BlockPos pos, Container container, ContainerData data) {
        super(Tier4Menus.KILN.get(), id);
        checkContainerSize(container, KilnBlockEntity.SLOTS);
        this.container = container;
        this.data = data;
        this.pos = pos;
        for (int i = 0; i < KilnBlockEntity.INPUTS; i++) {
            addSlot(new Slot(container, i, INPUT_X + (i % 4) * 18, ROW_Y + (i / 4) * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return KilnBlockEntity.fires(stack);
                }
            });
        }
        for (int i = 0; i < KilnBlockEntity.OUTPUTS; i++) {
            addSlot(new Slot(container, KilnBlockEntity.INPUTS + i, OUTPUT_X + (i % 4) * 18, ROW_Y + (i / 4) * 18) {
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
        return Math.min(1.0f, data.get(KilnBlockEntity.DATA_PROGRESS) / 1000.0f);
    }

    public KilnBlockEntity.Status status() {
        int s = data.get(KilnBlockEntity.DATA_STATUS);
        var values = KilnBlockEntity.Status.values();
        return s >= 0 && s < values.length ? values[s] : KilnBlockEntity.Status.EMPTY;
    }

    public int temperature() {
        return data.get(KilnBlockEntity.DATA_TEMPERATURE);
    }

    public int heat() {
        return data.get(KilnBlockEntity.DATA_HEAT);
    }

    public int limit() {
        return data.get(KilnBlockEntity.DATA_LIMIT);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int n = KilnBlockEntity.SLOTS;
        if (index < n) {
            if (!moveItemStackTo(stack, n, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!KilnBlockEntity.fires(stack) || !moveItemStackTo(stack, 0, KilnBlockEntity.INPUTS, false)) {
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
