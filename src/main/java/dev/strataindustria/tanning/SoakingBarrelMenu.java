package dev.strataindustria.tanning;

import dev.strataindustria.journal.Journal;
import dev.strataindustria.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
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

/** Soaking barrel screen: input, output, the tank and how the soak goes. */
public class SoakingBarrelMenu extends AbstractContainerMenu {
    public static final int INPUT_X = 43, OUTPUT_X = 115, SLOT_Y = 26, INVENTORY_Y = 84;

    private final Container container;
    private final ContainerData data;
    private final BlockPos pos;

    public SoakingBarrelMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos(), new SimpleContainer(SoakingBarrelBlockEntity.SLOTS),
                new SimpleContainerData(SoakingBarrelBlockEntity.DATA_COUNT));
    }

    public SoakingBarrelMenu(int id, Inventory inventory, BlockPos pos, Container container, ContainerData data) {
        super(ModMenus.SOAKING_BARREL.get(), id);
        checkContainerSize(container, SoakingBarrelBlockEntity.SLOTS);
        this.container = container;
        this.data = data;
        this.pos = pos;
        addSlot(new Slot(container, SoakingBarrelBlockEntity.INPUT, INPUT_X + 1, SLOT_Y + 1));
        addSlot(new Slot(container, SoakingBarrelBlockEntity.OUTPUT, OUTPUT_X + 1, SLOT_Y + 1) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                if (stack.is(Items.LEATHER) && player instanceof ServerPlayer serverPlayer) Journal.award(serverPlayer, Journal.LEATHER);
                super.onTake(player, stack);
            }
        });
        addStandardInventorySlots(inventory, 8, INVENTORY_Y);
        addDataSlots(data);
    }

    public BlockPos pos() {
        return pos;
    }

    public float progress() {
        return Math.min(1.0f, data.get(SoakingBarrelBlockEntity.DATA_PROGRESS) / 1000.0f);
    }

    public SoakingBarrelBlockEntity.Status status() {
        int s = data.get(SoakingBarrelBlockEntity.DATA_STATUS);
        var values = SoakingBarrelBlockEntity.Status.values();
        return s >= 0 && s < values.length ? values[s] : SoakingBarrelBlockEntity.Status.EMPTY;
    }

    public SoakingBarrelBlockEntity.TankFluid fluid() {
        int f = data.get(SoakingBarrelBlockEntity.DATA_FLUID);
        var values = SoakingBarrelBlockEntity.TankFluid.values();
        return f >= 0 && f < values.length ? values[f] : SoakingBarrelBlockEntity.TankFluid.NONE;
    }

    public int amount() {
        return data.get(SoakingBarrelBlockEntity.DATA_AMOUNT);
    }

    public int itemsShort() {
        return data.get(SoakingBarrelBlockEntity.DATA_SHORT);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int n = SoakingBarrelBlockEntity.SLOTS;
        if (index < n) {
            if (!moveItemStackTo(stack, n, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, SoakingBarrelBlockEntity.INPUT, SoakingBarrelBlockEntity.INPUT + 1, false)) {
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
