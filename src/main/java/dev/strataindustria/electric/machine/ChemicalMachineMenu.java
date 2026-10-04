package dev.strataindustria.electric.machine;

import dev.strataindustria.power.ElectricTier;
import dev.strataindustria.registry.Tier5Menus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.level.material.Fluid;

/**
 * A fluid machine's screen (spec 10.1, 11.1 and 11.2): two item inputs, the item outputs, the tanks, the
 * power bar, the status line and the auto-eject toggle (button 0).
 */
public class ChemicalMachineMenu extends AbstractContainerMenu {
    public static final int BUTTON_EJECT = 0;

    private final ChemicalMachineLayout layout;
    private final ElectricTier tier;
    private final Container container;
    private final ContainerData data;
    private final BlockPos pos;

    public ChemicalMachineMenu(ChemicalMachineLayout layout, int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(layout, buf.readBlockPos(), buf.readEnum(ElectricTier.class), id, inventory);
    }

    private ChemicalMachineMenu(ChemicalMachineLayout layout, BlockPos pos, ElectricTier tier, int id, Inventory inventory) {
        this(layout, tier, id, inventory, pos, new SimpleContainer(layout.slots()), new SimpleContainerData(ChemicalMachineLayout.DATA_COUNT));
    }

    public ChemicalMachineMenu(ChemicalMachineLayout layout, ElectricTier tier, int id, Inventory inventory, BlockPos pos, Container container,
            ContainerData data) {
        super(type(layout), id);
        checkContainerSize(container, layout.slots());
        this.layout = layout;
        this.tier = tier;
        this.container = container;
        this.data = data;
        this.pos = pos;
        for (int i = 0; i < ChemicalMachineLayout.ITEM_INPUTS; i++) {
            int slot = i;
            addSlot(new Slot(container, slot, layout.inputSlotX(i), ChemicalMachineLayout.SLOT_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return container.canPlaceItem(slot, stack);
                }
            });
        }
        for (int i = 0; i < layout.itemOutputs(); i++) {
            addSlot(new Slot(container, ChemicalMachineLayout.ITEM_INPUTS + i, layout.outputSlotX(i), ChemicalMachineLayout.SLOT_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }
        addStandardInventorySlots(inventory, 8, ChemicalMachineLayout.INVENTORY_Y);
        addDataSlots(data);
    }

    private static MenuType<ChemicalMachineMenu> type(ChemicalMachineLayout layout) {
        return switch (layout) {
            case MIXER -> Tier5Menus.MIXER.get();
            case ELECTROLYSER -> Tier5Menus.ELECTROLYSER.get();
        };
    }

    public ChemicalMachineLayout layout() {
        return layout;
    }

    public ElectricTier tier() {
        return tier;
    }

    public BlockPos pos() {
        return pos;
    }

    public float progress() {
        return Math.min(1.0f, data.get(ChemicalMachineLayout.PROGRESS) / 1000.0f);
    }

    public ElectricMachineBlockEntity.Status status() {
        int s = data.get(ChemicalMachineLayout.STATUS);
        var values = ElectricMachineBlockEntity.Status.values();
        return s >= 0 && s < values.length ? values[s] : ElectricMachineBlockEntity.Status.EMPTY;
    }

    public int powerPercent() {
        return data.get(ChemicalMachineLayout.POWER);
    }

    public float buffer() {
        return Math.min(1.0f, data.get(ChemicalMachineLayout.BUFFER) / 100.0f);
    }

    public boolean autoEject() {
        return data.get(ChemicalMachineLayout.EJECT) != 0;
    }

    /** The fluid that has no room, for "Hydrogen tank full". */
    public Fluid fullFluid() {
        return BuiltInRegistries.FLUID.byId(data.get(ChemicalMachineLayout.FULL_FLUID));
    }

    public Fluid tankFluid(int tank) {
        return amount(tank) > 0 ? BuiltInRegistries.FLUID.byId(data.get(ChemicalMachineLayout.TANKS + 2 * tank)) : net.minecraft.world.level.material.Fluids.EMPTY;
    }

    public int amount(int tank) {
        return data.get(ChemicalMachineLayout.TANKS + 2 * tank + 1);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_EJECT && container instanceof ChemicalMachineBlockEntity machine) {
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
        int machine = layout.slots();
        if (index < machine) {
            if (!moveItemStackTo(stack, machine, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, ChemicalMachineLayout.ITEM_INPUTS, false)) {
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
