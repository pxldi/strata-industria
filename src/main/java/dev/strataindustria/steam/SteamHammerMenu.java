package dev.strataindustria.steam;

import dev.strataindustria.registry.Tier4Menus;
import dev.strataindustria.smithing.AnvilBlockEntity;
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

/**
 * Steam hammer screen: the recorded pattern, then the line from the input through the piece on the anvil
 * to the result, the steam buffer on the right, and the status, heat and steam lines.
 */
public class SteamHammerMenu extends AbstractContainerMenu {
    public static final int SLOT_Y = 35, PATTERN_X = 8, QUEUE_X = 35, PIECE_X = 71, RESULT_X = 125;
    public static final int INVENTORY_Y = 100;
    private static final int SLOTS = SteamHammerBlockEntity.HAMMER_SLOTS;
    /** Menu slots in order: pattern, input, piece, result. */
    private static final int MENU_SLOTS = 4;

    private final Container container;
    private final ContainerData data;

    public SteamHammerMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, new SimpleContainer(SLOTS), new SimpleContainerData(SteamHammerBlockEntity.DATA_COUNT));
        buf.readBlockPos();
    }

    public SteamHammerMenu(int id, Inventory inventory, Container container, ContainerData data) {
        super(Tier4Menus.STEAM_HAMMER.get(), id);
        checkContainerSize(container, SLOTS);
        this.container = container;
        this.data = data;
        addSlot(new Slot(container, AnvilBlockEntity.PATTERN, PATTERN_X, SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return SteamHammerBlockEntity.isRecordedPattern(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addSlot(new Slot(container, SteamHammerBlockEntity.QUEUE, QUEUE_X, SLOT_Y));
        // The piece on the anvil can be taken off, but only the hammer puts one there.
        addSlot(new Slot(container, AnvilBlockEntity.INPUT, PIECE_X, SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addSlot(new Slot(container, SteamHammerBlockEntity.RESULT, RESULT_X, SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addStandardInventorySlots(inventory, 8, INVENTORY_Y);
        addDataSlots(data);
    }

    public SteamHammerBlockEntity.Status status() {
        int s = data.get(SteamHammerBlockEntity.DATA_STATUS);
        SteamHammerBlockEntity.Status[] values = SteamHammerBlockEntity.Status.values();
        return s >= 0 && s < values.length ? values[s] : SteamHammerBlockEntity.Status.NO_PATTERN;
    }

    public int hitsDone() {
        return data.get(SteamHammerBlockEntity.DATA_HITS);
    }

    public int hitsTotal() {
        return data.get(SteamHammerBlockEntity.DATA_TOTAL);
    }

    public int pieceTemperature() {
        return data.get(SteamHammerBlockEntity.DATA_PIECE_TEMPERATURE);
    }

    /** The working temperature of the piece on the anvil, or of the next one waiting. */
    public int working() {
        return data.get(SteamHammerBlockEntity.DATA_WORKING);
    }

    public int heat() {
        return data.get(SteamHammerBlockEntity.DATA_HEAT);
    }

    public int heatTemperature() {
        return data.get(SteamHammerBlockEntity.DATA_HEAT_TEMPERATURE);
    }

    public int limit() {
        return data.get(SteamHammerBlockEntity.DATA_LIMIT);
    }

    public int steam() {
        return data.get(SteamHammerBlockEntity.DATA_STEAM);
    }

    public float pressure() {
        return data.get(SteamHammerBlockEntity.DATA_PRESSURE) / 10.0f;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < MENU_SLOTS) {
            if (!moveItemStackTo(stack, MENU_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (SteamHammerBlockEntity.isRecordedPattern(stack)) {
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
