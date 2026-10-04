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

/** Converter screen: pig iron, scrap and coke in; the blow's flame; steel and slag out. */
public class ConverterMenu extends AbstractContainerMenu {
    public static final int CHARGE_X = 8, PIG_IRON_Y = 18, SCRAP_Y = 38, COKE_Y = 58, OUTPUT_Y = 36, STEEL_X = 132, SLAG_X = 152,
            INVENTORY_Y = 108;

    private final Container container;
    private final ContainerData data;
    private final BlockPos pos;

    public ConverterMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos(), new SimpleContainer(ConverterBlockEntity.SLOTS), new SimpleContainerData(ConverterBlockEntity.DATA_COUNT));
    }

    public ConverterMenu(int id, Inventory inventory, BlockPos pos, Container container, ContainerData data) {
        super(Tier4Menus.CONVERTER.get(), id);
        checkContainerSize(container, ConverterBlockEntity.SLOTS);
        this.container = container;
        this.data = data;
        this.pos = pos;
        charge(ConverterBlockEntity.PIG_IRON, PIG_IRON_Y);
        charge(ConverterBlockEntity.SCRAP, SCRAP_Y);
        charge(ConverterBlockEntity.COKE, COKE_Y);
        output(ConverterBlockEntity.STEEL, STEEL_X);
        output(ConverterBlockEntity.SLAG, SLAG_X);
        addStandardInventorySlots(inventory, 8, INVENTORY_Y);
        addDataSlots(data);
    }

    private void charge(int index, int y) {
        addSlot(new Slot(container, index, CHARGE_X, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return ConverterBlockEntity.fitsSlot(index, stack);
            }

            @Override
            public int getMaxStackSize() {
                return ConverterBlockEntity.slotLimit(index);
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

    public ConverterBlockEntity.Status status() {
        int s = data.get(ConverterBlockEntity.DATA_STATUS);
        var values = ConverterBlockEntity.Status.values();
        return s >= 0 && s < values.length ? values[s] : ConverterBlockEntity.Status.INCOMPLETE;
    }

    public float progress() {
        return Math.min(1.0f, data.get(ConverterBlockEntity.DATA_PROGRESS) / 1000.0f);
    }

    public float flame() {
        return Math.min(1.0f, data.get(ConverterBlockEntity.DATA_FLAME) / 1000.0f);
    }

    public int air() {
        return data.get(ConverterBlockEntity.DATA_AIR);
    }

    /** Pig iron and scrap in the blow under way. */
    public int chargePig() {
        return data.get(ConverterBlockEntity.DATA_CHARGE_PIG);
    }

    public int chargeScrap() {
        return data.get(ConverterBlockEntity.DATA_CHARGE_SCRAP);
    }

    public BlastFurnaceStructure.Problem problem() {
        int p = data.get(ConverterBlockEntity.DATA_PROBLEM);
        var values = BlastFurnaceStructure.Problem.values();
        return p >= 0 && p < values.length ? values[p] : BlastFurnaceStructure.Problem.NONE;
    }

    public int problemLayer() {
        return data.get(ConverterBlockEntity.DATA_WHERE) / 9 + 1;
    }

    public int problemSpot() {
        return data.get(ConverterBlockEntity.DATA_WHERE) % 9;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int n = ConverterBlockEntity.SLOTS;
        if (index < n) {
            if (!moveItemStackTo(stack, n, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            for (int charge = ConverterBlockEntity.PIG_IRON; charge <= ConverterBlockEntity.COKE && !moved; charge++) {
                if (ConverterBlockEntity.fitsSlot(charge, stack)) moved = moveItemStackTo(stack, charge, charge + 1, false);
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
