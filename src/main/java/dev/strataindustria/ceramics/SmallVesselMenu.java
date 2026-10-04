package dev.strataindustria.ceramics;

import dev.strataindustria.registry.ModMenus;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * The small vessel's four slots. Contents live in the vessel's {@code container} component and are
 * written back on every change; the vessel's own slot is locked while it is open.
 */
public class SmallVesselMenu extends AbstractContainerMenu {
    public static final int SLOTS = 4;
    public static final int SLOT_X = 53, SLOT_Y = 20, INVENTORY_Y = 51;

    private final InteractionHand hand;
    private final ItemStack vessel;
    private final int lockedHotbar;

    public SmallVesselMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readBoolean() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
    }

    public SmallVesselMenu(int id, Inventory inventory, InteractionHand hand) {
        super(ModMenus.SMALL_VESSEL.get(), id);
        this.hand = hand;
        this.vessel = inventory.player.getItemInHand(hand);
        SimpleContainer contents = new SimpleContainer(SLOTS) {
            @Override
            public void setChanged() {
                super.setChanged();
                vessel.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(getItems()));
            }
        };
        NonNullList<ItemStack> stored = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        vessel.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(stored);
        for (int i = 0; i < SLOTS; i++) contents.setItem(i, stored.get(i), false);

        for (int i = 0; i < SLOTS; i++) {
            addSlot(new Slot(contents, i, SLOT_X + i * 18, SLOT_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.getItem().canFitInsideContainerItems();
                }
            });
        }
        int locked = hand == InteractionHand.MAIN_HAND ? inventory.getSelectedSlot() : -1;
        this.lockedHotbar = locked;
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inventory, 9 + row * 9 + col, 8 + col * 18, INVENTORY_Y + row * 18));
        for (int col = 0; col < 9; col++) {
            addSlot(col == locked ? new LockedSlot(inventory, col, 8 + col * 18, INVENTORY_Y + 58)
                    : new Slot(inventory, col, 8 + col * 18, INVENTORY_Y + 58));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem() || slot instanceof LockedSlot) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < SLOTS) {
            if (!moveItemStackTo(stack, SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!stack.getItem().canFitInsideContainerItems() || !moveItemStackTo(stack, 0, SLOTS, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }

    /** Number-key swaps would move the open vessel out from under the menu. */
    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player player) {
        if (input == ContainerInput.SWAP && (button == lockedHotbar || (lockedHotbar < 0 && button == 40))) return;
        super.clicked(slotId, button, input, player);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getItemInHand(hand) == vessel && !vessel.isEmpty();
    }

    /** The slot holding the open vessel: it cannot be picked up or swapped out. */
    private static final class LockedSlot extends Slot {
        LockedSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
