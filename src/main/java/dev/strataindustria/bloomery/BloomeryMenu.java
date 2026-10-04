package dev.strataindustria.bloomery;

import dev.strataindustria.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

/** The bloomery status screen (spec 5.3): no slots, only what the bloomery is doing. */
public class BloomeryMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private final ContainerData data;

    public BloomeryMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos(), new SimpleContainerData(BloomeryBlockEntity.DATA_COUNT));
    }

    public BloomeryMenu(int id, Inventory inventory, BlockPos pos, ContainerData data) {
        super(ModMenus.BLOOMERY.get(), id);
        this.pos = pos;
        this.data = data;
        addDataSlots(data);
    }

    public BlockPos pos() {
        return pos;
    }

    public int get(int index) {
        return data.get(index);
    }

    public BloomeryBlockEntity.Status status() {
        BloomeryBlockEntity.Status[] values = BloomeryBlockEntity.Status.values();
        int i = data.get(BloomeryBlockEntity.DATA_STATUS);
        return i >= 0 && i < values.length ? values[i] : BloomeryBlockEntity.Status.INCOMPLETE;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64
                && player.level().getBlockEntity(pos) instanceof BloomeryBlockEntity;
    }
}
