package dev.strataindustria.transport.rail;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/** Three slots of hay bales, open to hoppers and chutes from every side (outposts spec 5.3). */
public class HayRackBlockEntity extends BlockEntity implements WorldlyContainer {
    public static final int SLOTS = 3;
    private static final int[] ALL = {0, 1, 2};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

    public HayRackBlockEntity(BlockPos pos, BlockState state) {
        super(RailRegistry.HAY_RACK_ENTITY.get(), pos, state);
    }

    /** Bales in the rack, counting every slot. */
    public int bales() {
        int count = 0;
        for (ItemStack stack : items) count += stack.getCount();
        return count;
    }

    /** Puts one bale in, if there is room. */
    public boolean addBale() {
        for (int slot = 0; slot < SLOTS; slot++) {
            ItemStack stack = items.get(slot);
            if (stack.isEmpty()) {
                setItem(slot, new ItemStack(Items.HAY_BLOCK));
                return true;
            }
            if (stack.getCount() < stack.getMaxStackSize()) {
                stack.grow(1);
                setChanged();
                return true;
            }
        }
        return false;
    }

    /** Takes one bale out, if there is one. */
    public boolean takeBale() {
        for (int slot = SLOTS - 1; slot >= 0; slot--) {
            ItemStack stack = items.get(slot);
            if (stack.isEmpty()) continue;
            stack.shrink(1);
            if (stack.isEmpty()) items.set(slot, ItemStack.EMPTY);
            setChanged();
            return true;
        }
        return false;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level == null) return;
        BlockState state = getBlockState();
        boolean filled = !isEmpty();
        if (state.hasProperty(HayRackBlock.FILLED) && state.getValue(HayRackBlock.FILLED) != filled) {
            level.setBlock(worldPosition, state.setValue(HayRackBlock.FILLED, filled), Block.UPDATE_ALL);
        }
        level.updateNeighbourForOutputSignal(worldPosition, state.getBlock());
    }

    // ---------------------------------------------------------------- container

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) if (!stack.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack taken = ContainerHelper.removeItem(items, slot, count);
        if (!taken.isEmpty()) setChanged();
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        stack.limitSize(getMaxStackSize(stack));
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.is(Items.HAY_BLOCK);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ALL;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    /** Hoppers do not pull the hay out again; the ponies and a sneaking hand do. */
    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    // ---------------------------------------------------------------- saving

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items.clear();
        ContainerHelper.loadAllItems(input, items);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
