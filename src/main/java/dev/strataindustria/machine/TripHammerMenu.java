package dev.strataindustria.machine;

import dev.strataindustria.registry.ModMenus;
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

/** Trip hammer screen: the recorded pattern, a slot for hot workpieces, and the status and network lines. */
public class TripHammerMenu extends AbstractContainerMenu {
    public static final int PATTERN_X = 44, INPUT_X = 80, SLOT_Y = 35;
    public static final int INVENTORY_Y = 84;

    private final Container container;
    private final ContainerData data;
    private final BlockPos pos;

    public TripHammerMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos(), new SimpleContainer(2), new SimpleContainerData(TripHammerBlockEntity.DATA_COUNT));
    }

    public TripHammerMenu(int id, Inventory inventory, BlockPos pos, Container container, ContainerData data) {
        super(ModMenus.TRIP_HAMMER.get(), id);
        checkContainerSize(container, 2);
        this.container = container;
        this.data = data;
        this.pos = pos;
        addSlot(new Slot(container, TripHammerBlockEntity.PATTERN, PATTERN_X, SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return TripHammerBlockEntity.isRecordedPattern(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addSlot(new Slot(container, TripHammerBlockEntity.INPUT, INPUT_X, SLOT_Y));
        addStandardInventorySlots(inventory, 8, INVENTORY_Y);
        addDataSlots(data);
    }

    public BlockPos pos() {
        return pos;
    }

    public TripHammerBlockEntity.Status status() {
        int s = data.get(TripHammerBlockEntity.DATA_STATUS);
        TripHammerBlockEntity.Status[] values = TripHammerBlockEntity.Status.values();
        return s >= 0 && s < values.length ? values[s] : TripHammerBlockEntity.Status.NO_PATTERN;
    }

    public int hitsDone() {
        return data.get(TripHammerBlockEntity.DATA_HITS);
    }

    public int hitsTotal() {
        return data.get(TripHammerBlockEntity.DATA_TOTAL);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < 2) {
            if (!moveItemStackTo(stack, 2, slots.size(), true)) return ItemStack.EMPTY;
        } else if (TripHammerBlockEntity.isRecordedPattern(stack)) {
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
