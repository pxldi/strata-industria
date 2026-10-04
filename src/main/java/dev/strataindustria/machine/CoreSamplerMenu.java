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
import org.jspecify.annotations.Nullable;

/** Core sampler screen: the Drill button, a progress bar, the output slot and the status and network lines. */
public class CoreSamplerMenu extends AbstractContainerMenu {
    public static final int OUTPUT_X = 124, OUTPUT_Y = 35;
    public static final int INVENTORY_Y = 84;

    private final Container container;
    private final ContainerData data;
    private final BlockPos pos;
    private final @Nullable CoreSamplerBlockEntity sampler;

    public CoreSamplerMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos(), new SimpleContainer(1), new SimpleContainerData(CoreSamplerBlockEntity.DATA_COUNT));
    }

    public CoreSamplerMenu(int id, Inventory inventory, BlockPos pos, Container container, ContainerData data) {
        super(ModMenus.CORE_SAMPLER.get(), id);
        checkContainerSize(container, 1);
        this.container = container;
        this.data = data;
        this.pos = pos;
        this.sampler = container instanceof CoreSamplerBlockEntity be ? be : null;
        addSlot(new Slot(container, CoreSamplerBlockEntity.OUTPUT, OUTPUT_X, OUTPUT_Y) {
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
        return Math.min(1.0f, data.get(CoreSamplerBlockEntity.DATA_PROGRESS) / 1000.0f);
    }

    public CoreSamplerBlockEntity.Status status() {
        int s = data.get(CoreSamplerBlockEntity.DATA_STATUS);
        return s >= 0 && s < CoreSamplerBlockEntity.Status.values().length ? CoreSamplerBlockEntity.Status.values()[s] : CoreSamplerBlockEntity.Status.IDLE;
    }

    /** Whether the Drill button is live: nothing running and the slot is free. */
    public boolean canDrill() {
        return status().canStart() && !slots.getFirst().hasItem();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != CoreSamplerBlockEntity.DRILL_BUTTON || sampler == null) return false;
        sampler.startDrilling();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (index != CoreSamplerBlockEntity.OUTPUT || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (!moveItemStackTo(stack, 1, slots.size(), true)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }
}
