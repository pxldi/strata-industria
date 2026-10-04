package dev.strataindustria.automation;

import dev.strataindustria.registry.Tier4DataComponents;
import dev.strataindustria.registry.Tier4Menus;
import dev.strataindustria.registry.Tier4Sounds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The filter's screen (tier 4 spec 13.5). Its nine entries are ghosts: clicking one with an item sets it,
 * with an empty hand clears it, and right-clicking steps through the item's tags. The entries live in the
 * filter's {@code filter_contents} component, so the held filter is locked while the screen is open.
 */
public class FilterMenu extends AbstractContainerMenu {
    public static final int GRID_X = 62, GRID_Y = 17, INVENTORY_Y = 84;
    public static final int BUTTON_TAG = FilterContents.SIZE, BUTTON_WHITELIST = 2 * FilterContents.SIZE, BUTTON_GRADE = BUTTON_WHITELIST + 1;

    private final InteractionHand hand;
    private final ItemStack filter;
    private final int lockedHotbar;

    public FilterMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readBoolean() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
    }

    public FilterMenu(int id, Inventory inventory, InteractionHand hand) {
        super(Tier4Menus.FILTER.get(), id);
        this.hand = hand;
        this.filter = inventory.player.getItemInHand(hand);
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

    public FilterContents contents() {
        return FilterContents.of(filter);
    }

    /** The hand holding the filter; the screen reads the live stack from it. */
    public InteractionHand hand() {
        return hand;
    }

    private void set(Player player, FilterContents contents) {
        filter.set(Tier4DataComponents.FILTER_CONTENTS.get(), contents);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), Tier4Sounds.FILTER_CONFIGURE.get(), SoundSource.PLAYERS,
                0.5f, 0.9f + player.getRandom().nextFloat() * 0.2f);
        broadcastChanges();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        FilterContents contents = contents();
        if (id >= 0 && id < FilterContents.SIZE) {
            ItemStack carried = getCarried();
            set(player, contents.withEntry(id, carried.isEmpty() ? Items.AIR : carried.getItem()));
        } else if (id >= BUTTON_TAG && id < BUTTON_WHITELIST) {
            set(player, contents.cycleTag(id - BUTTON_TAG));
        } else if (id == BUTTON_WHITELIST) {
            set(player, contents.toggleWhitelist());
        } else if (id == BUTTON_GRADE) {
            set(player, contents.toggleGrade());
        } else {
            return false;
        }
        return true;
    }

    /** Shift-clicking an item in the inventory sets it in the first empty entry; the item stays put. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem() || slot instanceof LockedSlot) return ItemStack.EMPTY;
        FilterContents contents = contents();
        for (int i = 0; i < FilterContents.SIZE; i++) {
            if (contents.item(i) == slot.getItem().getItem()) return ItemStack.EMPTY;
        }
        for (int i = 0; i < FilterContents.SIZE; i++) {
            if (contents.item(i) == Items.AIR) {
                set(player, contents.withEntry(i, slot.getItem().getItem()));
                break;
            }
        }
        return ItemStack.EMPTY;
    }

    /** Number-key swaps would move the open filter out from under the menu. */
    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player player) {
        if (input == ContainerInput.SWAP && (button == lockedHotbar || (lockedHotbar < 0 && button == 40))) return;
        super.clicked(slotId, button, input, player);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getItemInHand(hand) == filter && !filter.isEmpty();
    }

    /** The slot holding the open filter: it cannot be picked up or swapped out. */
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
