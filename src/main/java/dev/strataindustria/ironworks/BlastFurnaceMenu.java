package dev.strataindustria.ironworks;

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

/** Blast furnace screen: the three charge slots, the buffers and hearth, and the tapped pig iron and slag. */
public class BlastFurnaceMenu extends AbstractContainerMenu {
    public static final int CHARGE_X = 8, ORE_Y = 18, FUEL_Y = 38, FLUX_Y = 58, OUTPUT_Y = 36, PIG_IRON_X = 132, SLAG_X = 152,
            INVENTORY_Y = 108;

    private final Container container;
    private final ContainerData data;
    private final BlockPos pos;

    public BlastFurnaceMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos(), new SimpleContainer(BlastFurnaceBlockEntity.SLOTS),
                new SimpleContainerData(BlastFurnaceBlockEntity.DATA_COUNT));
    }

    public BlastFurnaceMenu(int id, Inventory inventory, BlockPos pos, Container container, ContainerData data) {
        super(Tier4Menus.BLAST_FURNACE.get(), id);
        checkContainerSize(container, BlastFurnaceBlockEntity.SLOTS);
        this.container = container;
        this.data = data;
        this.pos = pos;
        charge(BlastFurnaceBlockEntity.ORE, ORE_Y);
        charge(BlastFurnaceBlockEntity.FUEL, FUEL_Y);
        charge(BlastFurnaceBlockEntity.FLUX, FLUX_Y);
        output(BlastFurnaceBlockEntity.PIG_IRON, PIG_IRON_X);
        output(BlastFurnaceBlockEntity.SLAG, SLAG_X);
        addStandardInventorySlots(inventory, 8, INVENTORY_Y);
        addDataSlots(data);
    }

    private void charge(int index, int y) {
        addSlot(new Slot(container, index, CHARGE_X, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return BlastFurnaceBlockEntity.fitsSlot(index, stack);
            }
        });
    }

    private void output(int index, int x) {
        addSlot(new Slot(container, index, x, OUTPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
    }

    public BlockPos pos() {
        return pos;
    }

    public int iron() {
        return data.get(BlastFurnaceBlockEntity.DATA_IRON);
    }

    public int fuel() {
        return data.get(BlastFurnaceBlockEntity.DATA_FUEL);
    }

    public int flux() {
        return data.get(BlastFurnaceBlockEntity.DATA_FLUX);
    }

    public int air() {
        return data.get(BlastFurnaceBlockEntity.DATA_AIR);
    }

    /** Hearth heat, 0 to 1. */
    /** How many heat inlets the furnace has; the hot blast line shows only with one. */
    public int inlets() {
        return data.get(BlastFurnaceBlockEntity.DATA_INLETS);
    }

    public int hotTemperature() {
        return data.get(BlastFurnaceBlockEntity.DATA_HOT_TEMPERATURE);
    }

    public int hotHeat() {
        return data.get(BlastFurnaceBlockEntity.DATA_HOT_HEAT);
    }

    public int hotLimit() {
        return data.get(BlastFurnaceBlockEntity.DATA_HOT_LIMIT);
    }

    public float warmth() {
        return Math.min(1.0f, data.get(BlastFurnaceBlockEntity.DATA_WARMTH) / 1000.0f);
    }

    public float progress() {
        return Math.min(1.0f, data.get(BlastFurnaceBlockEntity.DATA_PROGRESS) / 1000.0f);
    }

    public BlastFurnaceBlockEntity.Status status() {
        int s = data.get(BlastFurnaceBlockEntity.DATA_STATUS);
        var values = BlastFurnaceBlockEntity.Status.values();
        return s >= 0 && s < values.length ? values[s] : BlastFurnaceBlockEntity.Status.INCOMPLETE;
    }

    public BlastFurnaceStructure.Problem problem() {
        int p = data.get(BlastFurnaceBlockEntity.DATA_PROBLEM);
        var values = BlastFurnaceStructure.Problem.values();
        return p >= 0 && p < values.length ? values[p] : BlastFurnaceStructure.Problem.NONE;
    }

    /** Layer 1 to 5 of the missing block. */
    public int problemLayer() {
        return data.get(BlastFurnaceBlockEntity.DATA_WHERE) / 9 + 1;
    }

    /** Where in its layer, from 0 (front left) to 8 (back right), as seen standing at the controller. */
    public int problemSpot() {
        return data.get(BlastFurnaceBlockEntity.DATA_WHERE) % 9;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int n = BlastFurnaceBlockEntity.SLOTS;
        if (index < n) {
            if (!moveItemStackTo(stack, n, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            for (int charge = BlastFurnaceBlockEntity.ORE; charge <= BlastFurnaceBlockEntity.FLUX && !moved; charge++) {
                if (BlastFurnaceBlockEntity.fitsSlot(charge, stack)) moved = moveItemStackTo(stack, charge, charge + 1, false);
            }
            if (!moved) return ItemStack.EMPTY;
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
