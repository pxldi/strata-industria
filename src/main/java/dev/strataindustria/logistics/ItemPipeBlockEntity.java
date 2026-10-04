package dev.strataindustria.logistics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.strataindustria.automation.FilterContents;
import dev.strataindustria.journal.Journal;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * One item pipe's contents (tier 5 spec 12.1): the stacks travelling through it, each with the way it will
 * leave, and the filters on its inventory faces. A pipe is also a container with one slot that never holds
 * anything: whatever a hopper, inserter, chute, belt or machine puts in that slot goes into the network
 * instead, as long as some inventory on the network takes it.
 */
public class ItemPipeBlockEntity extends BlockEntity implements WorldlyContainer {
    /** Ticks an item takes to cross one pipe block: 4 blocks a second. */
    public static final int SPEED = 5;
    /** Most stacks travelling in one pipe block. */
    public static final int CAPACITY = 16;
    /** Ticks between tries at a new route for an item with nowhere to go. */
    private static final int RETRY = 10;

    /** A stack on its way: it came in by {@code from}, leaves by {@code route[0]}, and entered at {@code stamp}. */
    public static final class Travelling {
        public static final Codec<Travelling> CODEC = RecordCodecBuilder.create(i -> i.group(
                ItemStack.CODEC.fieldOf("stack").forGetter(t -> t.stack),
                Direction.CODEC.fieldOf("from").forGetter(t -> t.from),
                Direction.CODEC.listOf().fieldOf("route").forGetter(t -> t.route),
                Codec.LONG.fieldOf("stamp").forGetter(t -> t.stamp),
                Codec.BOOL.optionalFieldOf("waiting", false).forGetter(t -> t.waiting)
        ).apply(i, Travelling::new));

        ItemStack stack;
        Direction from;
        List<Direction> route;
        long stamp;
        boolean waiting;

        Travelling(ItemStack stack, Direction from, List<Direction> route, long stamp, boolean waiting) {
            this.stack = stack;
            this.from = from;
            this.route = List.copyOf(route);
            this.stamp = stamp;
            this.waiting = waiting;
        }

        public ItemStack stack() {
            return stack;
        }

        public Direction from() {
            return from;
        }

        /** The face it leaves by, or null while it has nowhere to go. */
        public @Nullable Direction to() {
            return waiting || route.isEmpty() ? null : route.get(0);
        }

        public long stamp() {
            return stamp;
        }
    }

    private final List<Travelling> items = new ArrayList<>();
    private final ItemStack[] filters = new ItemStack[6];
    /** Which inventory at the same distance gets the next item. */
    private int turn;
    /** The face the last hopper-style push came through, noted when the container asks about faces. */
    private @Nullable Direction pushedFrom;

    public ItemPipeBlockEntity(BlockPos pos, BlockState state) {
        super(Tier5Logistics.ITEM_PIPE_BE.get(), pos, state);
        java.util.Arrays.fill(filters, ItemStack.EMPTY);
    }

    public List<Travelling> items() {
        return items;
    }

    // ------------------------------------------------------------------ filters

    public ItemStack filter(Direction side) {
        return filters[side.ordinal()];
    }

    public boolean hasFilter(Direction side) {
        return !filters[side.ordinal()].isEmpty();
    }

    /** Fits {@code stack} on {@code side} (or clears it) and returns the one that was there. */
    public ItemStack setFilter(Direction side, ItemStack stack) {
        ItemStack old = filters[side.ordinal()];
        filters[side.ordinal()] = stack;
        setChanged();
        return old;
    }

    /** Whether the filter on {@code side} lets {@code stack} through; with no filter everything passes. */
    public boolean allows(Direction side, ItemStack stack) {
        ItemStack filter = filters[side.ordinal()];
        return filter.isEmpty() || FilterContents.of(filter).test(stack);
    }

    // ------------------------------------------------------------------ travel

    /** Whether {@link #insert} would take {@code stack}: there is room in the pipe and an inventory takes it. */
    public boolean accepts(ItemStack stack, @Nullable Direction from) {
        if (!(level instanceof ServerLevel server) || items.size() >= CAPACITY) return false;
        Direction noReturn = from != null && ItemPipeBlock.face(getBlockState(), from) == ItemPipeBlock.Face.PORT ? from : null;
        return ItemRoutes.find(server, worldPosition, stack, noReturn, turn) != null;
    }

    /** Puts {@code stack} into the pipe, entering by the face {@code from} (null when it appeared inside). Returns whether it went in. */
    public boolean insert(ItemStack stack, @Nullable Direction from) {
        if (!(level instanceof ServerLevel server) || stack.isEmpty() || items.size() >= CAPACITY) return false;
        Direction noReturn = from != null && ItemPipeBlock.face(getBlockState(), from) == ItemPipeBlock.Face.PORT ? from : null;
        ItemRoutes.Route route = ItemRoutes.find(server, worldPosition, stack, noReturn, turn++);
        if (route == null) return false;
        items.add(new Travelling(stack.copy(), from != null ? from : route.path().get(0).getOpposite(), route.path(), server.getGameTime(), false));
        sync();
        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ItemPipeBlockEntity pipe) {
        if (!pipe.items.isEmpty()) pipe.tick((ServerLevel) level, state);
    }

    private void tick(ServerLevel level, BlockState state) {
        long now = level.getGameTime();
        boolean changed = false;
        for (int i = 0; i < items.size(); i++) {
            Travelling t = items.get(i);
            if (t.waiting) {
                if ((now + worldPosition.asLong()) % RETRY == 0 && reroute(level, t, now)) changed = true;
                continue;
            }
            if (now - t.stamp < SPEED) continue;
            Direction out = t.route.get(0);
            ItemPipeBlock.Face face = ItemPipeBlock.face(state, out);
            BlockPos next = worldPosition.relative(out);
            if (face == ItemPipeBlock.Face.PIPE && t.route.size() > 1 && level.getBlockEntity(next) instanceof ItemPipeBlockEntity far) {
                if (far.items.size() >= CAPACITY) continue;
                t.from = out.getOpposite();
                t.route = List.copyOf(t.route.subList(1, t.route.size()));
                t.stamp = now;
                far.items.add(t);
                far.sync();
                items.remove(i--);
                changed = true;
            } else if (face == ItemPipeBlock.Face.PORT && t.route.size() == 1 && allows(out, t.stack)) {
                var container = HopperBlockEntity.getContainerAt(level, next);
                ItemStack left = container == null ? t.stack : HopperBlockEntity.addItem(null, container, t.stack.copy(), out.getOpposite());
                if (left.isEmpty()) {
                    items.remove(i--);
                    Journal.awardNear(level, worldPosition, Journal.ITEM_PIPE);
                } else {
                    t.stack = left;
                    reroute(level, t, now);
                }
                changed = true;
            } else {
                reroute(level, t, now);
                changed = true;
            }
        }
        if (changed) sync();
    }

    /** Looks for a new way for {@code t}; with none it waits in the middle of the pipe. */
    private boolean reroute(ServerLevel level, Travelling t, long now) {
        ItemRoutes.Route route = ItemRoutes.find(level, worldPosition, t.stack, null, turn++);
        if (route == null) {
            boolean was = t.waiting;
            t.waiting = true;
            t.route = List.of();
            return !was;
        }
        t.waiting = false;
        t.route = route.path();
        t.stamp = now - SPEED / 2;
        return true;
    }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    // ------------------------------------------------------------------ container (pushes from outside)

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ItemStack.EMPTY;
    }

    /** What is put in the slot enters the network; if it cannot, it is dropped rather than lost. */
    @Override
    public void setItem(int slot, ItemStack stack) {
        if (stack.isEmpty() || level == null || level.isClientSide()) return;
        if (!insert(stack, pushedFrom)) Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return false;
    }

    @Override
    public void clearContent() {}

    @Override
    public int[] getSlotsForFace(Direction side) {
        pushedFrom = side;
        return new int[] {0};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        pushedFrom = side;
        if (side != null && ItemPipeBlock.face(getBlockState(), side) == ItemPipeBlock.Face.OFF) return false;
        return accepts(stack, side);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    // ------------------------------------------------------------------ lifecycle and saving

    /** Everything in the pipe falls out with it. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level == null) return;
        for (Travelling t : items) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, t.stack);
        for (ItemStack filter : filters) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, filter);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        items.clear();
        in.read("items", Travelling.CODEC.listOf()).ifPresent(items::addAll);
        for (Direction side : Direction.values()) {
            filters[side.ordinal()] = in.read("filter_" + side.getName(), ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (!items.isEmpty()) out.store("items", Travelling.CODEC.listOf(), items);
        for (Direction side : Direction.values()) {
            if (!filters[side.ordinal()].isEmpty()) out.store("filter_" + side.getName(), ItemStack.OPTIONAL_CODEC, filters[side.ordinal()]);
        }
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
