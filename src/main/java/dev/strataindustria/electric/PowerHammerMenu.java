package dev.strataindustria.electric;

import dev.strataindustria.registry.Tier5Menus;
import dev.strataindustria.smithing.AnvilBlockEntity;
import dev.strataindustria.smithing.ShapeMachine;
import dev.strataindustria.smithing.ShapeSelector;
import net.minecraft.server.level.ServerPlayer;
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
 * Power hammer screen: the shape button on the left, the input and the second (weld)
 * piece, then the piece on the anvil and the result, with the hits filling the arrow, the power bar on the
 * right, the status and temperature lines, and the auto-eject toggle (button 0).
 */
public class PowerHammerMenu extends AbstractContainerMenu {
    public static final int BUTTON_EJECT = 0;
    public static final int SHAPE_X = 8, SHAPE_Y = 36, QUEUE_X = 35, QUEUE_Y = 26, SECOND_X = 35, SECOND_Y = 46,
            PIECE_X = 71, PIECE_Y = 35, RESULT_X = 125, RESULT_Y = 35;
    public static final int INVENTORY_Y = 100;
    private static final int SLOTS = PowerHammerBlockEntity.HAMMER_SLOTS;
    /** Menu slots in order: input, second, piece, result. */
    private static final int MENU_SLOTS = 4;

    private final Container container;
    private final ContainerData data;

    public PowerHammerMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, new SimpleContainer(SLOTS), new SimpleContainerData(PowerHammerBlockEntity.DATA_COUNT));
        buf.readBlockPos();
    }

    public PowerHammerMenu(int id, Inventory inventory, Container container, ContainerData data) {
        super(Tier5Menus.POWER_HAMMER.get(), id);
        checkContainerSize(container, SLOTS);
        this.container = container;
        this.data = data;
        addSlot(new Slot(container, PowerHammerBlockEntity.QUEUE, QUEUE_X, QUEUE_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return container.canPlaceItem(PowerHammerBlockEntity.QUEUE, stack);
            }
        });
        addSlot(new Slot(container, AnvilBlockEntity.SECOND, SECOND_X, SECOND_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return container.canPlaceItem(AnvilBlockEntity.SECOND, stack);
            }
        });
        // The piece on the anvil can be taken off, but only the hammer puts one there.
        addSlot(new Slot(container, AnvilBlockEntity.INPUT, PIECE_X, PIECE_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addSlot(new Slot(container, PowerHammerBlockEntity.RESULT, RESULT_X, RESULT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addStandardInventorySlots(inventory, 8, INVENTORY_Y);
        addDataSlots(data);
    }

    public PowerHammerBlockEntity.Status status() {
        int s = data.get(PowerHammerBlockEntity.DATA_STATUS);
        PowerHammerBlockEntity.Status[] values = PowerHammerBlockEntity.Status.values();
        return s >= 0 && s < values.length ? values[s] : PowerHammerBlockEntity.Status.WAITING;
    }

    public int hitsDone() {
        return data.get(PowerHammerBlockEntity.DATA_HITS);
    }

    public int hitsTotal() {
        return data.get(PowerHammerBlockEntity.DATA_TOTAL);
    }

    public int pieceTemperature() {
        return data.get(PowerHammerBlockEntity.DATA_PIECE_TEMPERATURE);
    }

    /** The temperature the piece has to reach: its working temperature, or the weld's. */
    public int needed() {
        return data.get(PowerHammerBlockEntity.DATA_NEEDED);
    }

    public int powerPercent() {
        return data.get(PowerHammerBlockEntity.DATA_POWER);
    }

    public float buffer() {
        return Math.min(1.0f, data.get(PowerHammerBlockEntity.DATA_BUFFER) / 100.0f);
    }

    public boolean autoEject() {
        return data.get(PowerHammerBlockEntity.DATA_EJECT) != 0;
    }

    /** What the shape button shows: the item the working shape makes. */
    public ItemStack shape() {
        return ShapeSelector.displayStack(data.get(PowerHammerBlockEntity.DATA_SHAPE));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (player instanceof ServerPlayer server && ShapeMachine.press(container, server, id)) return true;
        if (id == BUTTON_EJECT && container instanceof PowerHammerBlockEntity hammer) {
            hammer.toggleAutoEject();
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
        if (index < MENU_SLOTS) {
            if (!moveItemStackTo(stack, MENU_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, 1, false)) {
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
