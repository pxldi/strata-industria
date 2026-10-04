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

/** Saw mill screen: the blade beside the input, the progress arrow, then planks and bark. */
public class SawMillMenu extends AbstractContainerMenu {
    public static final int BLADE_X = 32, INPUT_X = 56, INPUT_Y = 35, OUTPUT_X = 116, EXTRA_X = 142;
    public static final int INVENTORY_Y = 84;
    private static final int SLOTS = 4;

    private final Container container;
    private final ContainerData data;
    private final BlockPos pos;

    public SawMillMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos(), new SimpleContainer(SLOTS), new SimpleContainerData(SawMillBlockEntity.DATA_COUNT));
    }

    public SawMillMenu(int id, Inventory inventory, BlockPos pos, Container container, ContainerData data) {
        super(ModMenus.SAW_MILL.get(), id);
        checkContainerSize(container, SLOTS);
        this.container = container;
        this.data = data;
        this.pos = pos;
        addSlot(new Slot(container, SawMillBlockEntity.INPUT, INPUT_X, INPUT_Y));
        addSlot(new Slot(container, SawMillBlockEntity.BLADE, BLADE_X, INPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return SawMillBlockEntity.bladeLife(stack) > 0;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addSlot(output(container, SawMillBlockEntity.OUTPUT, OUTPUT_X));
        addSlot(output(container, SawMillBlockEntity.EXTRA, EXTRA_X));
        addStandardInventorySlots(inventory, 8, INVENTORY_Y);
        addDataSlots(data);
    }

    private static Slot output(Container container, int index, int x) {
        return new Slot(container, index, x, INPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        };
    }

    public BlockPos pos() {
        return pos;
    }

    public float progress() {
        return Math.min(1.0f, data.get(SawMillBlockEntity.DATA_PROGRESS) / 1000.0f);
    }

    public SawMillBlockEntity.Status status() {
        int s = data.get(SawMillBlockEntity.DATA_STATUS);
        SawMillBlockEntity.Status[] values = SawMillBlockEntity.Status.values();
        return s >= 0 && s < values.length ? values[s] : SawMillBlockEntity.Status.EMPTY;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < SLOTS) {
            if (!moveItemStackTo(stack, SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (SawMillBlockEntity.bladeLife(stack) > 0 && !slots.get(1).hasItem()) {
            if (!moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
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
