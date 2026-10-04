package dev.strataindustria.ironworks;

import dev.strataindustria.registry.Tier4BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * A charging or tap hatch (tier 4 spec 12.1). It holds nothing itself: hoppers and chutes reach through
 * it to the controller that last claimed it, the charging hatch into the charge slots and the tap hatch
 * out of the output slots. Until a built furnace claims it, it has no slots at all.
 */
public class FurnaceHatchBlockEntity extends BlockEntity implements WorldlyContainer {
    private static final int[] NONE = new int[0];

    private @Nullable BlockPos host;

    public FurnaceHatchBlockEntity(BlockPos pos, BlockState state) {
        super(Tier4BlockEntities.FURNACE_HATCH.get(), pos, state);
    }

    /** Called by a controller each time it finds this hatch in its built structure. */
    public void claim(BlockPos controller) {
        host = controller.immutable();
    }

    private boolean tap() {
        return getBlockState().getBlock() instanceof TapHatchBlock;
    }

    public @Nullable FurnaceHost host() {
        if (host == null || level == null || !level.isLoaded(host)) return null;
        return level.getBlockEntity(host) instanceof FurnaceHost furnace && furnace.usesPart(worldPosition) ? furnace : null;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        FurnaceHost furnace = host();
        if (furnace == null) return NONE;
        return tap() ? furnace.tapSlots() : furnace.chargeSlots();
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        FurnaceHost furnace = host();
        return furnace != null && !tap() && contains(furnace.chargeSlots(), slot) && furnace.canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        FurnaceHost furnace = host();
        return furnace != null && tap() && contains(furnace.tapSlots(), slot);
    }

    private static boolean contains(int[] slots, int slot) {
        for (int s : slots) if (s == slot) return true;
        return false;
    }

    @Override
    public int getContainerSize() {
        FurnaceHost furnace = host();
        return furnace == null ? 0 : furnace.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        FurnaceHost furnace = host();
        return furnace == null || furnace.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        FurnaceHost furnace = host();
        return furnace == null ? ItemStack.EMPTY : furnace.getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        FurnaceHost furnace = host();
        return furnace == null ? ItemStack.EMPTY : furnace.removeItem(slot, count);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        FurnaceHost furnace = host();
        return furnace == null ? ItemStack.EMPTY : furnace.removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        FurnaceHost furnace = host();
        if (furnace != null) furnace.setItem(slot, stack);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        FurnaceHost furnace = host();
        return furnace != null && !tap() && contains(furnace.chargeSlots(), slot) && furnace.canPlaceItem(slot, stack);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        FurnaceHost furnace = host();
        if (furnace != null) furnace.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return false;
    }

    /** The furnace's contents belong to the controller; emptying a hatch empties nothing. */
    @Override
    public void clearContent() {}

    /** Breaking a hatch must not spill the controller's contents, which stay in the controller. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {}
}
