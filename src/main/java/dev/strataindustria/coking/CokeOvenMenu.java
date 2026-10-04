package dev.strataindustria.coking;

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
import net.minecraft.world.item.Items;

/** Coke oven screen: what bakes, what came out, the creosote tank and its bucket slots. */
public class CokeOvenMenu extends AbstractContainerMenu {
    public static final int INPUT_X = 34, OUTPUT_X = 94, SLOT_Y = 26, BUCKET_X = 152, BUCKET_IN_Y = 17, BUCKET_OUT_Y = 53,
            INVENTORY_Y = 84;

    private final Container container;
    private final ContainerData data;
    private final BlockPos pos;

    public CokeOvenMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos(), new SimpleContainer(CokeOvenBlockEntity.SLOTS), new SimpleContainerData(CokeOvenBlockEntity.DATA_COUNT));
    }

    public CokeOvenMenu(int id, Inventory inventory, BlockPos pos, Container container, ContainerData data) {
        super(Tier4Menus.COKE_OVEN.get(), id);
        checkContainerSize(container, CokeOvenBlockEntity.SLOTS);
        this.container = container;
        this.data = data;
        this.pos = pos;
        addSlot(new Slot(container, CokeOvenBlockEntity.INPUT, INPUT_X, SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return CokeOvenBlockEntity.recipeFor(stack).isPresent();
            }
        });
        addSlot(new Slot(container, CokeOvenBlockEntity.OUTPUT, OUTPUT_X, SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addSlot(new Slot(container, CokeOvenBlockEntity.BUCKET_IN, BUCKET_X, BUCKET_IN_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.BUCKET);
            }
        });
        addSlot(new Slot(container, CokeOvenBlockEntity.BUCKET_OUT, BUCKET_X, BUCKET_OUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addStandardInventorySlots(inventory, 8, INVENTORY_Y);
        addDataSlots(data);
    }

    public BlockPos pos() {
        return pos;
    }

    public float progress() {
        return Math.min(1.0f, data.get(CokeOvenBlockEntity.DATA_PROGRESS) / 1000.0f);
    }

    public CokeOvenBlockEntity.Status status() {
        int s = data.get(CokeOvenBlockEntity.DATA_STATUS);
        var values = CokeOvenBlockEntity.Status.values();
        return s >= 0 && s < values.length ? values[s] : CokeOvenBlockEntity.Status.INCOMPLETE;
    }

    public int creosote() {
        return data.get(CokeOvenBlockEntity.DATA_CREOSOTE);
    }

    public CokeOvenStructure.Problem problem() {
        int p = data.get(CokeOvenBlockEntity.DATA_PROBLEM);
        var values = CokeOvenStructure.Problem.values();
        return p >= 0 && p < values.length ? values[p] : CokeOvenStructure.Problem.NONE;
    }

    /** Where the first missing block is, relative to the door. */
    public BlockPos problemOffset() {
        return new BlockPos(data.get(CokeOvenBlockEntity.DATA_DX), data.get(CokeOvenBlockEntity.DATA_DY), data.get(CokeOvenBlockEntity.DATA_DZ));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int n = CokeOvenBlockEntity.SLOTS;
        if (index < n) {
            if (!moveItemStackTo(stack, n, slots.size(), true)) return ItemStack.EMPTY;
        } else if (stack.is(Items.BUCKET)) {
            if (!moveItemStackTo(stack, CokeOvenBlockEntity.BUCKET_IN, CokeOvenBlockEntity.BUCKET_IN + 1, false)) return ItemStack.EMPTY;
        } else if (!CokeOvenBlockEntity.recipeFor(stack).isPresent()
                || !moveItemStackTo(stack, CokeOvenBlockEntity.INPUT, CokeOvenBlockEntity.INPUT + 1, false)) {
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
