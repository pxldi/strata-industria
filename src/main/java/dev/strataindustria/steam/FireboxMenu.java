package dev.strataindustria.steam;

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

/** Firebox screen: four fuel slots, the fire, its temperature and how much heat goes where. */
public class FireboxMenu extends AbstractContainerMenu {
    public static final int FUEL_X = 35, FUEL_Y = 53, INVENTORY_Y = 84;

    private final Container container;
    private final ContainerData data;
    private final BlockPos pos;

    public FireboxMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos(), new SimpleContainer(FireboxBlockEntity.SLOTS), new SimpleContainerData(FireboxBlockEntity.DATA_COUNT));
    }

    public FireboxMenu(int id, Inventory inventory, BlockPos pos, Container container, ContainerData data) {
        super(Tier4Menus.FIREBOX.get(), id);
        checkContainerSize(container, FireboxBlockEntity.SLOTS);
        this.container = container;
        this.data = data;
        this.pos = pos;
        for (int i = 0; i < FireboxBlockEntity.SLOTS; i++) {
            addSlot(new Slot(container, i, FUEL_X + i * 18, FUEL_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return FireboxBlockEntity.fuelFor(stack) != null;
                }
            });
        }
        addStandardInventorySlots(inventory, 8, INVENTORY_Y);
        addDataSlots(data);
    }

    public BlockPos pos() {
        return pos;
    }

    public float temperature() {
        return data.get(FireboxBlockEntity.DATA_TEMPERATURE);
    }

    /** Share of the current fuel item left, 0 to 1. */
    public float burnFraction() {
        int total = data.get(FireboxBlockEntity.DATA_BURN_TOTAL);
        return total <= 0 ? 0 : Math.min(1.0f, data.get(FireboxBlockEntity.DATA_BURN) / (float) total);
    }

    /** HU per tick the fire makes. */
    public int output() {
        return data.get(FireboxBlockEntity.DATA_OUTPUT);
    }

    /** HU per tick the block above takes. */
    public int taken() {
        return data.get(FireboxBlockEntity.DATA_TAKEN);
    }

    /** Whether a blower blows into it. */
    public boolean blown() {
        return data.get(FireboxBlockEntity.DATA_BLOWN) != 0;
    }

    public FireboxBlockEntity.Status status() {
        int s = data.get(FireboxBlockEntity.DATA_STATUS);
        var values = FireboxBlockEntity.Status.values();
        return s >= 0 && s < values.length ? values[s] : FireboxBlockEntity.Status.EMPTY;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int n = FireboxBlockEntity.SLOTS;
        if (index < n) {
            if (!moveItemStackTo(stack, n, slots.size(), true)) return ItemStack.EMPTY;
        } else if (FireboxBlockEntity.fuelFor(stack) == null || !moveItemStackTo(stack, 0, n, false)) {
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
