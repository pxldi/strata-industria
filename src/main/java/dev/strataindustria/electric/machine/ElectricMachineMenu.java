package dev.strataindustria.electric.machine;

import dev.strataindustria.power.ElectricTier;
import dev.strataindustria.registry.Tier5Menus;
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

/**
 * An electric item machine's screen (spec 10.1): one lane per parallel operation with its input, arrow
 * and outputs, the power bar, the status line and the auto-eject toggle (button 0).
 */
public class ElectricMachineMenu extends AbstractContainerMenu {
    public static final int BUTTON_EJECT = 0;

    private final ElectricMachineLayout layout;
    private final ElectricTier tier;
    private final int lanes;
    private final Container container;
    private final ContainerData data;
    private final BlockPos pos;

    public ElectricMachineMenu(ElectricMachineLayout layout, int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(layout, buf.readBlockPos(), buf.readEnum(ElectricTier.class), id, inventory);
    }

    private ElectricMachineMenu(ElectricMachineLayout layout, BlockPos pos, ElectricTier tier, int id, Inventory inventory) {
        this(layout, tier, id, inventory, pos, new SimpleContainer(layout.slots()), new SimpleContainerData(ElectricMachineLayout.DATA_COUNT));
    }

    public ElectricMachineMenu(ElectricMachineLayout layout, ElectricTier tier, int id, Inventory inventory, BlockPos pos, Container container,
            ContainerData data) {
        super(type(layout), id);
        checkContainerSize(container, layout.slots());
        this.layout = layout;
        this.tier = tier;
        this.lanes = tier == ElectricTier.MV ? ElectricMachineLayout.MAX_LANES : 1;
        this.container = container;
        this.data = data;
        this.pos = pos;
        for (int lane = 0; lane < lanes; lane++) {
            int slot = layout.inputSlot(lane);
            int y = ElectricMachineLayout.laneY(lanes, lane);
            addSlot(new Slot(container, slot, ElectricMachineLayout.INPUT_X, y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return container.canPlaceItem(slot, stack);
                }
            });
            for (int i = 0; i < layout.outputs(); i++) {
                addSlot(new Slot(container, layout.outputSlot(lane, i), ElectricMachineLayout.outputX(i), y) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                });
            }
        }
        addStandardInventorySlots(inventory, 8, ElectricMachineLayout.INVENTORY_Y);
        addDataSlots(data);
    }

    private static MenuType<ElectricMachineMenu> type(ElectricMachineLayout layout) {
        return switch (layout) {
            case ELECTRIC_FURNACE -> Tier5Menus.ELECTRIC_FURNACE.get();
            case MACERATOR -> Tier5Menus.MACERATOR.get();
        };
    }

    public ElectricMachineLayout layout() {
        return layout;
    }

    public ElectricTier tier() {
        return tier;
    }

    public int lanes() {
        return lanes;
    }

    public BlockPos pos() {
        return pos;
    }

    /** How far {@code lane} has got, 0 to 1. */
    public float progress(int lane) {
        return Math.min(1.0f, data.get(lane) / 1000.0f);
    }

    public ElectricMachineBlockEntity.Status status() {
        int s = data.get(ElectricMachineLayout.STATUS);
        var values = ElectricMachineBlockEntity.Status.values();
        return s >= 0 && s < values.length ? values[s] : ElectricMachineBlockEntity.Status.EMPTY;
    }

    /** Percent of full speed the machine last ran at. */
    public int powerPercent() {
        return data.get(ElectricMachineLayout.POWER);
    }

    /** How full the internal buffer is, 0 to 1. */
    public float buffer() {
        return Math.min(1.0f, data.get(ElectricMachineLayout.BUFFER) / 100.0f);
    }

    public boolean autoEject() {
        return data.get(ElectricMachineLayout.EJECT) != 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_EJECT && container instanceof ElectricMachineBlockEntity machine) {
            machine.toggleAutoEject();
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int machine = lanes * (1 + layout.outputs());
        if (index < machine) {
            if (!moveItemStackTo(stack, machine, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            for (int lane = 0; lane < lanes && !stack.isEmpty(); lane++) {
                int at = lane * (1 + layout.outputs());
                if (container.canPlaceItem(layout.inputSlot(lane), stack)) moved |= moveItemStackTo(stack, at, at + 1, false);
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
