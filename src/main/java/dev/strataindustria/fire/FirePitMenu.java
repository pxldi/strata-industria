package dev.strataindustria.fire;

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

/** Fire pit screen: cooking slot above the flames, fuel slot below, temperature gauge to the side. */
public class FirePitMenu extends AbstractContainerMenu {
    public static final int COOK_X = 80;
    public static final int COOK_Y = 18;
    public static final int FUEL_X = 80;
    public static final int FUEL_Y = 54;
    public static final int INVENTORY_Y = 84;
    private static final int CONTAINER_SLOTS = 2;

    private final Container container;
    private final ContainerData data;

    public FirePitMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(CONTAINER_SLOTS), new SimpleContainerData(FirePitBlockEntity.DATA_COUNT));
    }

    public FirePitMenu(int id, Inventory inventory, Container container, ContainerData data) {
        super(ModMenus.FIRE_PIT.get(), id);
        checkContainerSize(container, CONTAINER_SLOTS);
        this.container = container;
        this.data = data;
        addSlot(new Slot(container, FirePitBlockEntity.FUEL_SLOT, FUEL_X, FUEL_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return FirePitFuel.isFuel(stack);
            }
        });
        addSlot(new Slot(container, FirePitBlockEntity.COOK_SLOT, COOK_X, COOK_Y) {
            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addStandardInventorySlots(inventory, 8, INVENTORY_Y);
        addDataSlots(data);
    }

    public int temperature() {
        return data.get(FirePitBlockEntity.DATA_TEMPERATURE);
    }

    /** Fraction of the current fuel item left, 0 when nothing burns. */
    public float burnFraction() {
        int total = data.get(FirePitBlockEntity.DATA_BURN_TOTAL);
        return total <= 0 ? 0 : Math.min(1, data.get(FirePitBlockEntity.DATA_BURN_LEFT) / (float) total);
    }

    public float cookFraction() {
        return Math.min(1, data.get(FirePitBlockEntity.DATA_COOK) / (float) FirePitBlockEntity.COOK_TICKS);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < CONTAINER_SLOTS) {
            if (!moveItemStackTo(stack, CONTAINER_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (FirePitFuel.isFuel(stack)) {
            if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 1, 2, false)) {
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
