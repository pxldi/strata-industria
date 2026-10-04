package dev.strataindustria.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import java.util.List;

/** Three pegs, one tool each. */
public class ToolRackBlockEntity extends BlockEntity {
    public static final int SLOTS = 3;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

    public ToolRackBlockEntity(BlockPos pos, BlockState state) {
        super(SharedBlocks.TOOL_RACK_ENTITY.get(), pos, state);
    }

    public NonNullList<ItemStack> items() {
        return items;
    }

    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    /** Hangs a tool on the first free peg. */
    public boolean hang(ItemStack tool) {
        if (!ToolRackBlock.hangs(tool)) return false;
        for (int i = 0; i < SLOTS; i++) {
            if (items.get(i).isEmpty()) {
                items.set(i, tool.copyWithCount(1));
                changed();
                return true;
            }
        }
        return false;
    }

    public ItemStack takeLast() {
        for (int i = SLOTS - 1; i >= 0; i--) {
            if (!items.get(i).isEmpty()) {
                ItemStack taken = items.get(i);
                items.set(i, ItemStack.EMPTY);
                changed();
                return taken;
            }
        }
        return ItemStack.EMPTY;
    }

    /** Hangs ready-made tools on a rack a structure has just placed, such as worn tools rolled from a loot table. */
    public static void stock(ServerLevel level, BlockPos pos, List<ItemStack> tools) {
        if (!(level.getBlockEntity(pos) instanceof ToolRackBlockEntity rack)) return;
        for (ItemStack tool : tools) rack.hang(tool);
    }

    void dropContents(net.minecraft.world.level.Level level, BlockPos pos) {
        net.minecraft.world.Containers.dropContents(level, pos, items);
    }

    private void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null) dropContents(level, pos);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (int i = 0; i < SLOTS; i++) items.set(i, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items, true);
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
