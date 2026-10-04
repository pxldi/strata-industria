package dev.strataindustria.automation;

import dev.strataindustria.power.KineticBlockEntity;
import dev.strataindustria.power.KineticConsumer;
import dev.strataindustria.power.KineticNetworks;
import dev.strataindustria.registry.Tier4BlockEntities;
import dev.strataindustria.registry.Tier4Sounds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A conveyor belt's cargo (tier 4 spec 13.1). Up to four single items ride at {@code progress} 0 (back
 * of the belt) to 1 (front), a quarter block apart, moving 1 block per 16 ticks at 16 RPM. The front
 * item is handed on to the next belt of the line, into the container the belt faces, or dropped when
 * nothing is there; a full target stops the line and items queue up behind it. The belt is a
 * container so hoppers, chutes and inserters can load and unload it. The client moves the items with
 * the same rule between updates.
 */
public class ConveyorBlockEntity extends KineticBlockEntity implements KineticConsumer, WorldlyContainer {
    public static final int IMPACT = 1, MIN_SPEED = 4;
    public static final int SLOTS = 4;
    /** Closest two items may ride, as a share of a belt. */
    public static final float SPACING = 0.25f;
    private static final int[] ALL_SLOTS = {0, 1, 2, 3};

    private final ItemStack[] items = {ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
    private final float[] progress = new float[SLOTS];
    private boolean dirty;

    public ConveyorBlockEntity(BlockPos pos, BlockState state) {
        super(Tier4BlockEntities.CONVEYOR_BELT.get(), pos, state);
    }

    @Override
    public int impact() {
        return IMPACT;
    }

    @Override
    public int minSpeed() {
        return MIN_SPEED;
    }

    private BlockState state() {
        return level == null ? getBlockState() : level.getBlockState(worldPosition);
    }

    // ------------------------------------------------------------------ drive

    /** The belts this one shares a drive with: the next of its line and the ones feeding it. */
    @Override
    public List<BlockPos> links() {
        if (level == null) return List.of();
        return links(state());
    }

    private List<BlockPos> links(BlockState state) {
        if (!(state.getBlock() instanceof ConveyorBlock)) return List.of();
        List<BlockPos> links = new ArrayList<>(ConveyorBlock.previous(level, worldPosition, state));
        BlockPos next = ConveyorBlock.next(level, worldPosition, state);
        if (next != null) links.add(next);
        return links;
    }

    /** Runs {@code change}, which alters how the line is joined, and rebuilds the drive on both sides of it. */
    public void relink(Runnable change) {
        List<BlockPos> before = links();
        change.run();
        for (BlockPos pos : before) KineticNetworks.markDirty(level, pos);
        for (BlockPos pos : links()) KineticNetworks.markDirty(level, pos);
        KineticNetworks.markDirty(level, worldPosition);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        for (BlockPos pos : links()) KineticNetworks.markDirty(level, pos);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null) for (BlockPos pos : links(getBlockState())) KineticNetworks.markDirty(level, pos);
    }

    /** Blocks a second per belt, positive while the drive turns. */
    public float speed() {
        float rpm = kinetic().rpm();
        return rpm >= MIN_SPEED ? rpm / 256.0f : 0.0f;
    }

    // ------------------------------------------------------------------ cargo

    public ItemStack item(int slot) {
        return items[slot];
    }

    /** Where in the belt a slot's item is, from 0 at the back to 1 at the front, {@code partial} ticks on from the last tick. */
    public float[] positions(float partial) {
        float[] shown = progress.clone();
        float step = speed() * partial;
        if (step > 0.0f) advance(items, shown, step);
        return shown;
    }

    /**
     * Moves every item {@code step} forward, front item first, so none passes the one ahead of it
     * (leaving {@link #SPACING}) or the end of the belt.
     */
    private static void advance(ItemStack[] items, float[] at, float step) {
        int[] order = new int[SLOTS];
        int count = 0;
        for (int i = 0; i < SLOTS; i++) if (!items[i].isEmpty()) order[count++] = i;
        for (int a = 1; a < count; a++) {
            int moving = order[a], b = a - 1;
            while (b >= 0 && at[order[b]] < at[moving]) {
                order[b + 1] = order[b];
                b--;
            }
            order[b + 1] = moving;
        }
        float limit = 1.0f;
        for (int k = 0; k < count; k++) {
            int i = order[k];
            at[i] = Math.max(at[i], Math.min(at[i] + step, limit));
            limit = at[i] - SPACING;
        }
    }

    /** Whether the back of the belt is clear for another item. */
    public boolean entryFree() {
        boolean room = false;
        for (int i = 0; i < SLOTS; i++) {
            if (items[i].isEmpty()) room = true;
            else if (progress[i] < SPACING) return false;
        }
        return room;
    }

    /** Puts one item on the back of the belt if there is room. */
    public boolean accept(ItemStack stack) {
        if (stack.isEmpty() || !entryFree()) return false;
        for (int i = 0; i < SLOTS; i++) {
            if (!items[i].isEmpty()) continue;
            items[i] = stack.copyWithCount(1);
            progress[i] = 0.0f;
            changed();
            return true;
        }
        return false;
    }

    public boolean hasCargo() {
        for (ItemStack stack : items) if (!stack.isEmpty()) return true;
        return false;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ConveyorBlockEntity belt) {
        belt.tick(level, pos, state);
    }

    private void tick(Level level, BlockPos pos, BlockState state) {
        float speed = speed();
        if (speed > 0.0f && hasCargo()) {
            advance(items, progress, speed);
            if (level instanceof ServerLevel server) {
                if (soundDue(server)) server.playSound(null, pos, Tier4Sounds.CONVEYOR_RUN.get(), SoundSource.BLOCKS, 0.3f, 0.9f + server.getRandom().nextFloat() * 0.2f);
                handOn(server, pos, state);
            }
        }
        if (dirty && level instanceof ServerLevel server) {
            dirty = false;
            server.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    /** One soft rumble a second per belt that is carrying something, spread out so a line does not pulse. */
    private boolean soundDue(ServerLevel level) {
        return (level.getGameTime() + Math.floorMod(worldPosition.asLong(), 20L)) % 20 == 0;
    }

    /** Passes the front item on once it has reached the end. */
    private void handOn(ServerLevel level, BlockPos pos, BlockState state) {
        int front = -1;
        for (int i = 0; i < SLOTS; i++) if (!items[i].isEmpty() && progress[i] >= 1.0f && (front < 0 || progress[i] > progress[front])) front = i;
        if (front < 0 || !deliver(level, pos, state, items[front])) return;
        items[front] = ItemStack.EMPTY;
        progress[front] = 0.0f;
        changed();
    }

    /** Hands {@code stack} to what the belt faces; false if it cannot go yet. */
    private boolean deliver(ServerLevel level, BlockPos pos, BlockState state, ItemStack stack) {
        Direction facing = state.getValue(ConveyorBlock.FACING);
        BlockPos nextBelt = ConveyorBlock.next(level, pos, state);
        if (nextBelt != null) {
            return level.getBlockEntity(nextBelt) instanceof ConveyorBlockEntity belt && belt.accept(stack);
        }
        BlockPos front = pos.relative(facing).above(state.getValue(ConveyorBlock.SLOPE).highEnd() ? 1 : 0);
        BlockState there = level.getBlockState(front);
        if (there.getBlock() instanceof ConveyorBlock) {
            // A belt crossing the end takes items on its back or side, never head-on.
            return there.getValue(ConveyorBlock.FACING) != facing.getOpposite()
                    && level.getBlockEntity(front) instanceof ConveyorBlockEntity belt && belt.accept(stack);
        }
        Container target = HopperBlockEntity.getContainerAt(level, front);
        if (target != null) {
            if (!HopperBlockEntity.addItem(null, target, stack.copy(), facing.getOpposite()).isEmpty()) return false;
            target.setChanged();
            return true;
        }
        if (!there.getCollisionShape(level, front).isEmpty()) return false;
        ItemEntity entity = new ItemEntity(level, front.getX() + 0.5 - facing.getStepX() * 0.3, front.getY() + 0.3,
                front.getZ() + 0.5 - facing.getStepZ() * 0.3, stack.copy(), facing.getStepX() * 0.08, 0.02, facing.getStepZ() * 0.08);
        entity.setDefaultPickUpDelay();
        level.addFreshEntity(entity);
        return true;
    }

    /** Something stands in this belt's block: dropped items are picked up, anything else is carried along. */
    public void carry(Entity entity) {
        if (level == null) return;
        if (entity instanceof ItemEntity dropped) {
            if (level.isClientSide() || !dropped.isAlive() || dropped.hasPickUpDelay()) return;
            ItemStack stack = dropped.getItem();
            if (!entryFree()) return;
            accept(stack);
            stack.shrink(1);
            if (stack.isEmpty()) dropped.discard();
            else dropped.setItem(stack);
            return;
        }
        float speed = speed();
        if (speed <= 0.0f || entity.isShiftKeyDown() || entity.isNoGravity() || !entity.onGround() || entity instanceof Player player && player.getAbilities().flying) return;
        Direction facing = state().getValue(ConveyorBlock.FACING);
        // Ground friction takes most of each push away again, so a belt pushes by about half its own speed to carry at its speed.
        double push = speed * 0.45;
        Vec3 motion = entity.getDeltaMovement();
        entity.setDeltaMovement(motion.x + facing.getStepX() * push, motion.y, motion.z + facing.getStepZ() * push);
    }

    private void changed() {
        dirty = true;
        setChanged();
    }

    // ------------------------------------------------------------------ container

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ALL_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return true;
    }

    /** An empty slot takes an item only while the back of the belt is clear. */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot >= 0 && slot < SLOTS && items[slot].isEmpty() && entryFree();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    public boolean isEmpty() {
        return !hasCargo();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < SLOTS ? items[slot] : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        if (slot < 0 || slot >= SLOTS || items[slot].isEmpty()) return ItemStack.EMPTY;
        ItemStack taken = items[slot].split(count);
        if (items[slot].isEmpty()) items[slot] = ItemStack.EMPTY;
        changed();
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot < 0 || slot >= SLOTS) return ItemStack.EMPTY;
        ItemStack taken = items[slot];
        items[slot] = ItemStack.EMPTY;
        return taken;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= SLOTS) return;
        boolean was = items[slot].isEmpty();
        items[slot] = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        if (was && !stack.isEmpty()) progress[slot] = 0.0f;
        changed();
    }

    @Override
    public boolean stillValid(Player player) {
        return false;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < SLOTS; i++) items[i] = ItemStack.EMPTY;
        changed();
    }

    /** What rides the belt falls out with the block. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level == null) return;
        for (ItemStack stack : items) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
    }

    // ------------------------------------------------------------------ saving

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        for (int i = 0; i < SLOTS; i++) {
            items[i] = in.read("item" + i, ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
            progress[i] = in.getFloatOr("at" + i, 0.0f);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        for (int i = 0; i < SLOTS; i++) {
            if (items[i].isEmpty()) continue;
            out.store("item" + i, ItemStack.OPTIONAL_CODEC, items[i]);
            out.putFloat("at" + i, progress[i]);
        }
    }
}
