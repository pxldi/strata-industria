package dev.strataindustria.automation;

import dev.strataindustria.power.KineticBlockEntity;
import dev.strataindustria.power.KineticConsumer;
import dev.strataindustria.registry.Tier4BlockEntities;
import dev.strataindustria.registry.Tier4Sounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The inserter's arm (tier 4 spec 13.4). One transfer is one swing: it takes one item from the block
 * behind, carries it round and puts it in the block in front, then returns. A swing takes 40 ticks at
 * 16 RPM, 20 at 32. Containers decide what they accept and give up through their faces, so a machine
 * only ever receives items for its input and fuel slots and only gives up its outputs (smart insertion).
 * A redstone signal holds the arm where it is.
 */
public class InserterBlockEntity extends KineticBlockEntity implements KineticConsumer {
    public static final int IMPACT = 4, MIN_SPEED = 8;
    /** Ticks for a full swing at 16 RPM. */
    public static final int SWING_TICKS = 40;
    /** Ticks between tries while there is nothing to take or nowhere to put it. */
    private static final int RETRY = 4;

    /** Where in a swing the arm is: waiting to pick up, carrying forward, or coming back empty. */
    public enum Phase { IDLE, FORWARD, BACK }

    private ItemStack held = ItemStack.EMPTY;
    private ItemStack filter = ItemStack.EMPTY;
    private Phase phase = Phase.IDLE;
    /** 0 with the arm behind, 0.5 over the front, 1 back again. */
    private float progress;
    /** The game time the saved {@link #progress} was true at, for the client to carry on from. */
    private long stamp;
    /** Ticks until the next try at picking up or setting down, so a blocked arm does not search every tick. */
    private int wait;

    public InserterBlockEntity(BlockPos pos, BlockState state) {
        super(Tier4BlockEntities.INSERTER.get(), pos, state);
    }

    @Override
    public int impact() {
        return IMPACT;
    }

    @Override
    public int minSpeed() {
        return MIN_SPEED;
    }

    public ItemStack held() {
        return held;
    }

    public Phase phase() {
        return phase;
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
            BlockState state = getBlockState().setValue(InserterBlock.FILTERED, !stack.isEmpty());
            level.setBlock(worldPosition, state, Block.UPDATE_ALL);
        }
        return old;
    }

    /** Whether the filter lets {@code stack} through; no filter lets everything through. */
    public boolean allows(ItemStack stack) {
        return filter.isEmpty() || FilterContents.of(filter).test(stack);
    }

    /** Where the arm is along its swing on the client, 0 behind and 1 over the front, carried on from the last stamp. */
    public float reach(long now, float partial) {
        float speed = kinetic().rpm() / (16.0f * SWING_TICKS);
        float p = progress + ((now - stamp) + partial) * speed;
        return switch (phase) {
            case IDLE -> 0.0f;
            case FORWARD -> Math.min(p, 0.5f) * 2.0f;
            case BACK -> (1.0f - Math.min(p, 1.0f)) * 2.0f;
        };
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, InserterBlockEntity inserter) {
        inserter.tick((ServerLevel) level, pos, state.getValue(InserterBlock.FACING));
    }

    private void tick(ServerLevel level, BlockPos pos, Direction facing) {
        float rpm = kinetic().rpm();
        if (rpm < MIN_SPEED || level.hasNeighborSignal(pos)) return;
        float speed = rpm / (16.0f * SWING_TICKS);
        switch (phase) {
            case IDLE -> {
                if (--wait > 0) return;
                if (grab(level, pos, facing)) {
                    phase = Phase.FORWARD;
                    progress = 0.0f;
                    swing(level, pos);
                    sync(level);
                } else {
                    wait = RETRY;
                }
            }
            case FORWARD -> {
                progress = Math.min(progress + speed, 0.5f);
                if (progress < 0.5f || --wait > 0) return;
                if (place(level, pos, facing)) {
                    held = ItemStack.EMPTY;
                    phase = Phase.BACK;
                    swing(level, pos);
                    sync(level);
                } else {
                    wait = RETRY;
                }
            }
            case BACK -> {
                progress += speed;
                if (progress >= 1.0f) {
                    progress = 0.0f;
                    phase = Phase.IDLE;
                    sync(level);
                }
            }
        }
    }

    /** Takes one allowed item from the block behind, from the faces that give items up, that the block in front would accept. */
    private boolean grab(ServerLevel level, BlockPos pos, Direction facing) {
        Container source = HopperBlockEntity.getContainerAt(level, pos.relative(facing.getOpposite()));
        if (source == null) return false;
        Container target = HopperBlockEntity.getContainerAt(level, pos.relative(facing));
        Direction face = facing;
        int[] slots = source instanceof WorldlyContainer worldly ? worldly.getSlotsForFace(face) : null;
        int count = slots == null ? source.getContainerSize() : slots.length;
        for (int n = 0; n < count; n++) {
            int slot = slots == null ? n : slots[n];
            ItemStack stack = source.getItem(slot);
            if (stack.isEmpty() || !allows(stack)) continue;
            if (source instanceof WorldlyContainer worldly && !worldly.canTakeItemThroughFace(slot, stack, face)) continue;
            if (target != null && !fits(target, stack, facing.getOpposite())) continue;
            held = source.removeItem(slot, 1);
            source.setChanged();
            return !held.isEmpty();
        }
        return false;
    }

    /** Whether some of {@code stack} would go into {@code target} through {@code face}: smart insertion takes only what fits. */
    public static boolean fits(Container target, ItemStack stack, Direction face) {
        int[] slots = target instanceof WorldlyContainer worldly ? worldly.getSlotsForFace(face) : null;
        int count = slots == null ? target.getContainerSize() : slots.length;
        for (int n = 0; n < count; n++) {
            int slot = slots == null ? n : slots[n];
            if (!target.canPlaceItem(slot, stack)) continue;
            if (target instanceof WorldlyContainer worldly && !worldly.canPlaceItemThroughFace(slot, stack, face)) continue;
            ItemStack there = target.getItem(slot);
            if (there.isEmpty() || ItemStack.isSameItemSameComponents(there, stack) && there.getCount() < Math.min(target.getMaxStackSize(), there.getMaxStackSize())) {
                return true;
            }
        }
        return false;
    }

    /** Puts the held item in the block in front, or on the ground when there is nothing there. */
    private boolean place(ServerLevel level, BlockPos pos, Direction facing) {
        BlockPos front = pos.relative(facing);
        Container target = HopperBlockEntity.getContainerAt(level, front);
        if (target != null) {
            ItemStack left = HopperBlockEntity.addItem(null, target, held.copy(), facing.getOpposite());
            if (!left.isEmpty()) return false;
            target.setChanged();
            return true;
        }
        if (!level.getBlockState(front).getCollisionShape(level, front).isEmpty()) return false;
        ItemEntity entity = new ItemEntity(level, front.getX() + 0.5, front.getY() + 0.3, front.getZ() + 0.5, held.copy(), 0.0, 0.0, 0.0);
        entity.setDefaultPickUpDelay();
        level.addFreshEntity(entity);
        return true;
    }

    private void swing(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, Tier4Sounds.INSERTER_SWING.get(), SoundSource.BLOCKS, 0.35f, 0.95f + level.getRandom().nextFloat() * 0.1f);
    }

    /** Sends the swing's new phase to whoever is watching; the client carries on from the stamp. */
    private void sync(ServerLevel level) {
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    /** What the arm carries and the filter fall out with the block. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level == null) return;
        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), held);
        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), filter);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        held = in.read("held", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        filter = in.read("filter", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        phase = Phase.values()[Math.floorMod(in.getIntOr("phase", 0), Phase.values().length)];
        progress = in.getFloatOr("progress", 0.0f);
        stamp = in.getLongOr("stamp", 0L);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (!held.isEmpty()) out.store("held", ItemStack.OPTIONAL_CODEC, held);
        if (!filter.isEmpty()) out.store("filter", ItemStack.OPTIONAL_CODEC, filter);
        out.putInt("phase", phase.ordinal());
        out.putFloat("progress", progress);
        out.putLong("stamp", level == null ? 0L : level.getGameTime());
    }
}
