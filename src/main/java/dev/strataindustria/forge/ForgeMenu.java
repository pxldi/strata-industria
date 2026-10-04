package dev.strataindustria.forge;

import dev.strataindustria.registry.ModMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Forge screen: four heating slots in a row, the fuel slot under the flames, the gauge to the right. */
public class ForgeMenu extends AbstractContainerMenu {
    public static final int HEAT_X = 44, HEAT_Y = 18;
    public static final int FUEL_X = 80, FUEL_Y = 58;
    public static final int INVENTORY_Y = 88;
    private static final int CONTAINER_SLOTS = 1 + ForgeBlockEntity.HEAT_SLOTS;

    private final Container container;
    private final ContainerData data;

    public ForgeMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(CONTAINER_SLOTS), new SimpleContainerData(ForgeBlockEntity.DATA_COUNT));
    }

    public ForgeMenu(int id, Inventory inventory, Container container, ContainerData data) {
        super(ModMenus.FORGE.get(), id);
        checkContainerSize(container, CONTAINER_SLOTS);
        this.container = container;
        this.data = data;
        addSlot(new Slot(container, ForgeBlockEntity.FUEL_SLOT, FUEL_X, FUEL_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return ForgeBlockEntity.isFuel(stack);
            }

            @Override
            public int getMaxStackSize() {
                return ForgeBlockEntity.FUEL_STACK;
            }
        });
        for (int i = 0; i < ForgeBlockEntity.HEAT_SLOTS; i++) {
            addSlot(new Slot(container, ForgeBlockEntity.FIRST_HEAT_SLOT + i, HEAT_X + i * 18, HEAT_Y) {
                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        addStandardInventorySlots(inventory, 8, INVENTORY_Y);
        addDataSlots(data);
    }

    public int temperature() {
        return data.get(ForgeBlockEntity.DATA_TEMPERATURE);
    }

    public float burnFraction() {
        int total = data.get(ForgeBlockEntity.DATA_BURN_TOTAL);
        return total <= 0 ? 0 : Math.min(1, data.get(ForgeBlockEntity.DATA_BURN_LEFT) / (float) total);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < CONTAINER_SLOTS) {
            if (!moveItemStackTo(stack, CONTAINER_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (ForgeBlockEntity.isFuel(stack)) {
            if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 1, CONTAINER_SLOTS, false)) {
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
