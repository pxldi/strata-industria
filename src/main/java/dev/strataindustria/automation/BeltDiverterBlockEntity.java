package dev.strataindustria.automation;

import dev.strataindustria.registry.Tier4BlockEntities;
import dev.strataindustria.registry.Tier4Sounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A belt diverter's cargo and filter (tier 4 spec 13.2). It is a belt: items ride and queue as on any
 * other, and the front item is handed on in the usual way unless the filter matches it, in which case it is
 * pushed off the side. A side with a belt or a container takes the item or holds it (and the line) until it
 * can; a side with nothing there lets it pass, so a diverter that leads nowhere does not jam the line.
 */
public class BeltDiverterBlockEntity extends ConveyorBlockEntity {
    /** How long the paddle takes to flick out and back. */
    public static final int FLICK_TICKS = 10;

    private ItemStack filter = ItemStack.EMPTY;
    private long lastPush = -1000L;

    public BeltDiverterBlockEntity(BlockPos pos, BlockState state) {
        super(Tier4BlockEntities.BELT_DIVERTER.get(), pos, state);
    }

    public boolean hasFilter() {
        return !filter.isEmpty();
    }

    public ItemStack filter() {
        return filter;
    }

    /** Fits {@code stack} as the filter (or clears it) and returns the one that was there. */
    public ItemStack setFilter(ItemStack stack) {
        ItemStack old = filter;
        filter = stack;
        setChanged();
        if (level != null) {
            level.setBlock(worldPosition, getBlockState().setValue(BeltDiverterBlock.FILTERED, !stack.isEmpty()), Block.UPDATE_ALL);
        }
        return old;
    }

    /** Whether the filter sends {@code stack} off the side; with no filter nothing is diverted. */
    public boolean diverts(ItemStack stack) {
        return !filter.isEmpty() && FilterContents.of(filter).test(stack);
    }

    /** Game time of the last push, for the paddle's flick. */
    public long lastPush() {
        return lastPush;
    }

    @Override
    protected boolean deliver(ServerLevel level, BlockPos pos, BlockState state, ItemStack stack) {
        if (diverts(stack)) {
            Boolean pushed = push(level, pos, BeltDiverterBlock.pushSide(state), stack);
            if (pushed != null) {
                if (pushed) {
                    lastPush = level.getGameTime();
                    level.playSound(null, pos, Tier4Sounds.BELT_DIVERTER_PUSH.get(), SoundSource.BLOCKS, 0.6f, 0.95f + level.getRandom().nextFloat() * 0.1f);
                    changed();
                }
                return pushed;
            }
        }
        return super.deliver(level, pos, state, stack);
    }

    /** Puts {@code stack} on the belt or in the container at {@code side}; null when there is neither, false when it is full. */
    private static Boolean push(ServerLevel level, BlockPos pos, Direction side, ItemStack stack) {
        BlockPos target = pos.relative(side);
        if (!level.hasChunkAt(target)) return false;
        BlockState there = level.getBlockState(target);
        if (there.getBlock() instanceof ConveyorBlock) {
            // A belt facing back at the diverter would take the item head-on; it takes it on its back or side only.
            if (there.getValue(ConveyorBlock.FACING) == side.getOpposite()) return null;
            return level.getBlockEntity(target) instanceof ConveyorBlockEntity belt && belt.accept(stack);
        }
        Container container = HopperBlockEntity.getContainerAt(level, target);
        if (container == null) return null;
        if (!HopperBlockEntity.addItem(null, container, stack.copy(), side.getOpposite()).isEmpty()) return false;
        container.setChanged();
        return true;
    }

    /** The filter falls out with the block. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), filter);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        filter = in.read("filter", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        lastPush = in.getLongOr("last_push", -1000L);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (!filter.isEmpty()) out.store("filter", ItemStack.OPTIONAL_CODEC, filter);
        out.putLong("last_push", lastPush);
    }
}
