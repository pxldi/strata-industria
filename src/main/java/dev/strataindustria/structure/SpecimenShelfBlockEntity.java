package dev.strataindustria.structure;

import dev.strataindustria.journal.Journal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Four cubbies, each holding one mineral specimen (structures v2 section 5). */
public class SpecimenShelfBlockEntity extends BlockEntity {
    public static final int SLOTS = 4;
    /** Journal page written when a shelf holds four different minerals (a "Places" page, like the camps). */
    public static final String COLLECTION = StructureEvents.PLACE + "specimens";

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

    public SpecimenShelfBlockEntity(BlockPos pos, BlockState state) {
        super(StructureContent.SPECIMEN_SHELF_ENTITY.get(), pos, state);
    }

    public NonNullList<ItemStack> items() {
        return items;
    }

    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    /** Puts one specimen into the first free cubby. */
    public boolean place(ItemStack held) {
        if (!held.is(StructureContent.MINERAL_SPECIMEN.get())) return false;
        for (int i = 0; i < SLOTS; i++) {
            if (items.get(i).isEmpty()) {
                items.set(i, held.split(1));
                changed();
                return true;
            }
        }
        return false;
    }

    /** Takes the specimen from the last filled cubby. */
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

    /** Four cubbies, four different minerals. */
    public boolean isFullCollection() {
        java.util.Set<String> minerals = new java.util.HashSet<>();
        for (ItemStack stack : items) {
            String mineral = MineralSpecimenItem.mineral(stack);
            if (mineral != null) minerals.add(mineral);
        }
        return minerals.size() == SLOTS;
    }

    void dropContents(Level level, BlockPos pos) {
        Containers.dropContents(level, pos, items);
    }

    private void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
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
