package dev.strataindustria.transport.ropeway;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.power.KineticBlockEntity;
import dev.strataindustria.power.KineticConsumer;
import dev.strataindustria.power.KineticNetworks;
import dev.strataindustria.transport.outpost.Charter;
import dev.strataindustria.transport.outpost.LinkKind;
import dev.strataindustria.transport.outpost.RouteIndex;
import dev.strataindustria.transport.rail.TramwayRoutes;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

/**
 * The drive terminal (outposts spec 8): turned by a shaft, it drives the whole loop of rope and holds every bucket on
 * it. The buckets are not entities. The terminal keeps them as places on the loop, moves them each tick by the
 * rope's speed, loads the ones that come home from the container behind it and, one at a time, sends them out; the
 * return station takes their loads. Both stations have to be loaded and nothing in between has to be.
 */
public class RopewayTerminalBlockEntity extends KineticBlockEntity implements KineticConsumer {
    /** SU per RPM for each 64 blocks of line. */
    public static final int IMPACT_PER_64 = 4;
    public static final int MIN_SPEED = 8;
    /** Blocks a tick of rope speed per RPM: 0.1 at 16, 0.2 at 32. */
    public static final double SPEED_PER_RPM = 1.0 / 160.0;
    /** Fewest blocks of line between two buckets. */
    public static final double SPACING = 4.0;
    private static final double RAMP_UP = 0.004, RAMP_DOWN = 0.008;
    private static final int HEARTBEAT = 40, SEND_EVERY = 4, VALIDATE = 40, DRIVE_EVERY = 24;
    /** Blocks from any part of the line a player is told what it carries. */
    private static final double TELL_RANGE = 144;

    private List<BlockPos> nodes = List.of();
    private @Nullable RopewayPath path;
    private final List<RopewayBucket> buckets = new ArrayList<>();
    private int spare;
    private double advance;
    private int delivered;
    private boolean stalled;
    private boolean goalGiven;

    private double speed;
    private boolean netDirty = true;
    private double sentSpeed = -1;
    private long lastSend;
    private int driveTick;
    private boolean lightSave;
    /** The first node past the terminal, which clients draw the first span to before the line's picture arrives. */
    private @Nullable BlockPos first;

    public RopewayTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(RopewayRegistry.TERMINAL_ENTITY.get(), pos, state);
    }

    // ---------------------------------------------------------------- kinetics

    @Override
    public int impact() {
        double length = path == null ? 0 : path.length();
        return IMPACT_PER_64 * Math.max(1, (int) Math.ceil(length / 64.0));
    }

    @Override
    public int minSpeed() {
        return MIN_SPEED;
    }

    public boolean powered() {
        return kinetic().rpm() >= MIN_SPEED;
    }

    public Direction facing() {
        return getBlockState().getValue(RopewayTerminalBlock.FACING);
    }

    // ---------------------------------------------------------------- the line

    public boolean hasLine() {
        return path != null;
    }

    /** The first node past the terminal, if there is a line; known to clients. */
    public @Nullable BlockPos first() {
        return first;
    }

    private void synced() {
        first = nodes.size() >= 2 ? nodes.get(1) : null;
        setChanged();
        if (level instanceof ServerLevel server) server.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    public @Nullable RopewayPath path() {
        return path;
    }

    public List<BlockPos> nodes() {
        return nodes;
    }

    /** Buckets hanging on the line, loaded or not. */
    public int onLine() {
        return buckets.size();
    }

    public int spare() {
        return spare;
    }

    public int deliveredItems() {
        return delivered;
    }

    public double speed() {
        return speed;
    }

    public boolean stalled() {
        return stalled;
    }

    public double advance() {
        return advance;
    }

    public List<RopewayBucket> buckets() {
        return buckets;
    }

    /** Buckets the line can hold, on it and spare. */
    public int capacity() {
        return path == null ? 0 : (int) Math.floor(path.length() / SPACING);
    }

    /** Makes {@code nodes} (terminal, towers, return) the line. */
    public void setLine(ServerLevel level, List<BlockPos> line) {
        if (path != null) cutLine(level, worldPosition, false);
        nodes = List.copyOf(line);
        path = RopewayPath.of(nodes);
        advance = 0;
        speed = 0;
        stalled = false;
        goalGiven = delivered >= Journal.ROPEWAY_ITEMS;
        for (int i = 1; i < nodes.size() - 1; i++) {
            if (level.getBlockEntity(nodes.get(i)) instanceof RopewayTowerBlockEntity tower) tower.attach(worldPosition, i, nodes.get(i + 1));
        }
        if (level.getBlockEntity(nodes.getLast()) instanceof RopewayReturnBlockEntity far) far.attach(worldPosition);
        KineticNetworks.markDirty(level, worldPosition);
        netDirty = true;
        synced();
    }

    /**
     * The line is down: everything on it comes off at the terminal, buckets back to the spares, loads dropped, and the
     * towers forget it. {@code at} is the block that broke, or null when it was taken down by hand.
     */
    public void cutLine(ServerLevel level, @Nullable BlockPos at, boolean snap) {
        if (path == null) return;
        BlockPos from = worldPosition;
        for (RopewayBucket bucket : buckets) {
            if (!bucket.stack.isEmpty()) Containers.dropItemStack(level, from.getX() + 0.5, from.getY() + 1.0, from.getZ() + 0.5, bucket.stack);
            spare++;
        }
        buckets.clear();
        for (int i = 1; i < nodes.size(); i++) {
            BlockPos node = nodes.get(i);
            if (!level.hasChunkAt(node)) continue;
            if (level.getBlockEntity(node) instanceof RopewayTowerBlockEntity tower) tower.detach(worldPosition);
            else if (level.getBlockEntity(node) instanceof RopewayReturnBlockEntity far) far.detach(worldPosition);
        }
        List<BlockPos> old = nodes;
        nodes = List.of();
        path = null;
        speed = 0;
        stalled = false;
        RouteIndex.get(level).cut(level, at == null ? worldPosition : at);
        if (snap) {
            level.playSound(null, worldPosition, RopewayRegistry.LINE_SNAP.get(), SoundSource.BLOCKS, 1.0f, 0.9f);
            if (at != null && !at.equals(worldPosition)) level.playSound(null, at, RopewayRegistry.LINE_SNAP.get(), SoundSource.BLOCKS, 1.0f, 1.1f);
        }
        tell(level, old, new RopewayPayloads.LineState(worldPosition, List.of(), 0, 0, List.of()));
        KineticNetworks.markDirty(level, worldPosition);
        netDirty = true;
        synced();
    }

    /** Whether the line still stands: a node that is loaded and no longer there cuts it. Returns true if it cut. */
    private boolean validate(ServerLevel level) {
        for (int i = 1; i < nodes.size(); i++) {
            BlockPos node = nodes.get(i);
            if (!level.hasChunkAt(node)) continue;
            boolean last = i == nodes.size() - 1;
            BlockState state = level.getBlockState(node);
            boolean ok = last ? state.is(RopewayRegistry.RETURN.get()) : state.getBlock() instanceof RopewayTowerBlock;
            if (!ok) {
                cutLine(level, node, true);
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- buckets by hand

    /** Hangs {@code count} more buckets in the terminal; returns how many it took. */
    public int addSpare(int count) {
        int room = Math.max(0, capacity() - spare - buckets.size());
        int take = Math.min(count, room);
        spare += take;
        if (take > 0) setChanged();
        return take;
    }

    /** Every spare bucket, handed out. */
    public int takeSpare() {
        int taken = spare;
        spare = 0;
        if (taken > 0) setChanged();
        return taken;
    }

    /** A bucket used on the terminal: it goes on the hook, or with a sneaking hand one comes off. */
    public InteractionResult bucketBy(Player player, ItemStack held) {
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        String id = StrataIndustria.MOD_ID + ".ropeway.bucket.";
        if (path == null) {
            player.sendOverlayMessage(Component.translatable(id + "no_line"));
        } else if (player.isShiftKeyDown()) {
            if (spare == 0) {
                player.sendOverlayMessage(Component.translatable(id + "none_spare"));
            } else {
                int out = takeSpare();
                ItemStack back = new ItemStack(RopewayRegistry.BUCKET.get(), out);
                player.getInventory().add(back);
                if (!back.isEmpty()) Containers.dropItemStack(server, player.getX(), player.getY(), player.getZ(), back);
                server.playSound(null, worldPosition, RopewayRegistry.BUCKET_HANG.get(), SoundSource.BLOCKS, 0.6f, 0.7f);
                player.sendOverlayMessage(Component.translatable(id + "out", out));
            }
        } else if (addSpare(1) == 0) {
            player.sendOverlayMessage(Component.translatable(id + "full", capacity()));
        } else {
            if (!player.hasInfiniteMaterials()) held.shrink(1);
            server.playSound(null, worldPosition, RopewayRegistry.BUCKET_HANG.get(), SoundSource.BLOCKS, 0.7f, 1.0f + 0.04f * spare);
            player.sendOverlayMessage(Component.translatable(id + "hung", spare, capacity()));
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    public Component status() {
        String id = StrataIndustria.MOD_ID + ".ropeway.status.";
        if (path == null) return Component.translatable(id + "no_line");
        if (stalled) return Component.translatable(id + "backed_up");
        if (!powered()) return Component.translatable(id + "unpowered", buckets.size(), spare);
        if (!level.hasChunkAt(nodes.getLast())) return Component.translatable(id + "far_station");
        return Component.translatable(id + (speed > 0 ? "running" : "idle"), buckets.size(), spare);
    }

    // ---------------------------------------------------------------- the tick

    public static void serverTick(Level level, BlockPos pos, BlockState state, RopewayTerminalBlockEntity terminal) {
        if (!(level instanceof ServerLevel server) || terminal.path == null) return;
        long time = server.getGameTime();
        if ((time + pos.asLong()) % VALIDATE == 0 && terminal.validate(server)) return;
        BlockPos far = terminal.nodes.getLast();
        double target = terminal.powered() && server.hasChunkAt(far) ? terminal.kinetic().rpm() * SPEED_PER_RPM : 0.0;
        terminal.run(server, target);
        terminal.share(server, time);
    }

    private void run(ServerLevel level, double target) {
        RopewayPath line = path;
        if (line == null) return;
        double length = line.length(), loop = 2.0 * length;
        if (stalled) {
            if (retryUnload(level, length)) stalled = false;
            else {
                speed = 0;
                return;
            }
        }
        speed += Mth.clamp(target - speed, -RAMP_DOWN, RAMP_UP);
        if (speed < 1.0E-4) speed = target <= 0 ? 0 : speed;
        if (speed <= 0) {
            hangNext(level, loop);
            return;
        }
        if (++driveTick >= DRIVE_EVERY) {
            driveTick = 0;
            level.playSound(null, worldPosition, RopewayRegistry.DRIVE.get(), SoundSource.BLOCKS, 0.45f + (float) speed, 0.7f + (float) speed * 2.0f);
        }
        double from = advance, to = from + speed;
        // A loaded bucket reaching the return is tipped out there; if the load will not all go, the line stops with it at the lip.
        for (RopewayBucket bucket : buckets) {
            if (bucket.stack.isEmpty()) continue;
            double now = bucket.at(from, loop), reach = length - now;
            if (reach <= 0 || reach > speed) continue;
            if (!unload(level, bucket)) {
                to = from + reach;
                stalled = true;
                speed = 0;
                netDirty = true;
            }
            break;
        }
        advance = ((to % loop) + loop) % loop;
        Iterator<RopewayBucket> each = buckets.iterator();
        while (each.hasNext()) {
            RopewayBucket bucket = each.next();
            double before = bucket.at(from, loop), after = bucket.at(to, loop);
            for (int node = 1; node < nodes.size() - 1; node++) {
                double at = line.at(node);
                if (RopewayPath.crosses(before, after, at, loop) || RopewayPath.crosses(before, after, loop - at, loop)) {
                    level.playSound(null, nodes.get(node), RopewayRegistry.SHEAVE.get(), SoundSource.BLOCKS, 0.7f, 0.85f + level.getRandom().nextFloat() * 0.3f);
                }
            }
            if (!RopewayPath.crosses(before, after, 0, loop)) continue;
            if (bucket.delivered) {
                bucket.delivered = false;
                tripDone(level);
            }
            ItemStack cargo = take(level);
            if (cargo.isEmpty()) {
                each.remove();
                spare++;
                netDirty = true;
            } else {
                bucket.stack = cargo;
                hung(level);
                netDirty = true;
            }
        }
        hangNext(level, loop);
        setChanged();
    }

    /** With cargo waiting and a bucket to spare, sends one out as soon as there is room on the rope behind the last. */
    private void hangNext(ServerLevel level, double loop) {
        if (spare <= 0 || speed <= 0) return;
        for (RopewayBucket bucket : buckets) {
            double s = bucket.at(advance, loop);
            if (Math.min(s, loop - s) < SPACING) return;
        }
        ItemStack cargo = take(level);
        if (cargo.isEmpty()) return;
        spare--;
        buckets.add(new RopewayBucket(((-advance % loop) + loop) % loop, cargo, false));
        hung(level);
        netDirty = true;
        setChanged();
    }

    private void hung(ServerLevel level) {
        level.playSound(null, worldPosition, RopewayRegistry.BUCKET_HANG.get(), SoundSource.BLOCKS, 0.8f, 0.9f + level.getRandom().nextFloat() * 0.2f);
    }

    // ---------------------------------------------------------------- loading and unloading

    /** One stack from the container behind the terminal, else the one under it. */
    private ItemStack take(ServerLevel level) {
        Direction back = facing().getOpposite();
        for (Direction side : new Direction[] {back, Direction.DOWN}) {
            Container source = HopperBlockEntity.getContainerAt(level, worldPosition.relative(side));
            if (source == null) continue;
            Direction face = side.getOpposite();
            int[] slots = source instanceof WorldlyContainer worldly ? worldly.getSlotsForFace(face) : null;
            int count = slots == null ? source.getContainerSize() : slots.length;
            for (int n = 0; n < count; n++) {
                int slot = slots == null ? n : slots[n];
                ItemStack stack = source.getItem(slot);
                if (stack.isEmpty()) continue;
                if (source instanceof WorldlyContainer worldly && !worldly.canTakeItemThroughFace(slot, stack, face)) continue;
                ItemStack taken = source.removeItem(slot, stack.getMaxStackSize());
                source.setChanged();
                if (!taken.isEmpty()) return taken;
            }
        }
        return ItemStack.EMPTY;
    }

    /** Tips what a bucket carries into the return station's container. Returns whether the bucket is empty now. */
    private boolean unload(ServerLevel level, RopewayBucket bucket) {
        BlockPos far = nodes.getLast();
        if (!(level.getBlockEntity(far) instanceof RopewayReturnBlockEntity station)) return false;
        int before = bucket.stack.getCount();
        ItemStack carried = bucket.stack.copy();
        ItemStack rest = station.accept(level, bucket.stack.copy());
        int moved = before - rest.getCount();
        if (moved > 0) {
            delivered += moved;
            netDirty = true;
            level.playSound(null, far, RopewayRegistry.BUCKET_TIP.get(), SoundSource.BLOCKS, 0.9f, 0.9f + level.getRandom().nextFloat() * 0.2f);
            Vec3 lip = station.lip();
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, carried.getItem()), lip.x, lip.y, lip.z, 4 + moved / 4, 0.1, 0.05, 0.1, 0.06);
            if (!goalGiven && delivered >= Journal.ROPEWAY_ITEMS) {
                goalGiven = true;
                Journal.awardNear(level, worldPosition, Journal.ROPEWAY_DELIVERED);
                Journal.awardNear(level, far, Journal.ROPEWAY_DELIVERED);
            }
        }
        bucket.stack = rest;
        if (!rest.isEmpty()) return false;
        bucket.delivered = true;
        return true;
    }

    /** A line that stopped at the return tries again for the bucket waiting there. */
    private boolean retryUnload(ServerLevel level, double length) {
        double loop = 2.0 * length;
        for (RopewayBucket bucket : buckets) {
            if (bucket.stack.isEmpty() || Math.abs(bucket.at(advance, loop) - length) > 1.0E-3) continue;
            if (unload(level, bucket)) {
                netDirty = true;
                return true;
            }
            return false;
        }
        return true;
    }

    /** A bucket that carried something out has come home: the ropeway proves the link between its two stations. */
    private void tripDone(ServerLevel level) {
        RouteIndex index = RouteIndex.get(level);
        Charter near = TramwayRoutes.stationCharter(index, worldPosition), far = TramwayRoutes.stationCharter(index, nodes.getLast());
        if (near == null || far == null || near.id().equals(far.id())) return;
        RouteIndex.Proof proof = index.prove(level, near.id(), far.id(), LinkKind.ROPEWAY, new ArrayList<>(nodes));
        if (proof.refusal() != null) {
            for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(worldPosition).inflate(24))) player.sendOverlayMessage(proof.refusal());
        } else if (proof.link() != null) {
            index.traffic(level, proof.link().id());
        }
    }

    // ---------------------------------------------------------------- telling the players

    private void share(ServerLevel level, long time) {
        RopewayPath line = path;
        if (line == null) return;
        if (Math.abs(speed - sentSpeed) > 1.0E-6) netDirty = true;
        boolean beat = (time + worldPosition.asLong()) % HEARTBEAT == 0;
        if (!beat && !(netDirty && time - lastSend >= SEND_EVERY)) return;
        netDirty = false;
        lastSend = time;
        sentSpeed = speed;
        List<RopewayPayloads.BucketView> views = new ArrayList<>(buckets.size());
        for (RopewayBucket bucket : buckets) views.add(new RopewayPayloads.BucketView((float) bucket.offset, bucket.stack));
        tell(level, nodes, new RopewayPayloads.LineState(worldPosition, nodes, advance, (float) speed, views));
    }

    /** Sends {@code state} to every player near a piece of the line given by {@code line}. */
    private void tell(ServerLevel level, List<BlockPos> line, RopewayPayloads.LineState state) {
        if (line.isEmpty()) return;
        RopewayPath shape = RopewayPath.of(line);
        for (ServerPlayer player : level.players()) {
            if (shape.distanceTo(player.position()) <= TELL_RANGE) PacketDistributor.sendToPlayer(player, state);
        }
    }

    // ---------------------------------------------------------------- removal

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (!(level instanceof ServerLevel server)) return;
        if (path != null) cutLine(server, pos, true);
        if (spare > 0) Containers.dropItemStack(server, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                new ItemStack(RopewayRegistry.BUCKET.get(), spare));
        spare = 0;
    }

    // ---------------------------------------------------------------- saving

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        first = in.read("first", BlockPos.CODEC).orElse(null);
        nodes = in.listOrEmpty("nodes", BlockPos.CODEC).stream().toList();
        path = nodes.size() >= 2 ? RopewayPath.of(nodes) : null;
        if (path == null) nodes = List.of();
        else first = nodes.get(1);
        spare = in.getIntOr("spare", 0);
        advance = in.getDoubleOr("advance", 0);
        delivered = in.getIntOr("delivered", 0);
        stalled = in.getBooleanOr("stalled", false);
        goalGiven = in.getBooleanOr("goal_given", false);
        buckets.clear();
        in.listOrEmpty("buckets", RopewayBucket.CODEC).forEach(buckets::add);
        netDirty = true;
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (first != null) out.store("first", BlockPos.CODEC, first);
        if (lightSave) return;
        if (path != null) {
            var list = out.list("nodes", BlockPos.CODEC);
            for (BlockPos node : nodes) list.add(node);
        }
        out.putInt("spare", spare);
        out.putDouble("advance", advance);
        out.putInt("delivered", delivered);
        out.putBoolean("stalled", stalled);
        out.putBoolean("goal_given", goalGiven);
        var saved = out.list("buckets", RopewayBucket.CODEC);
        for (RopewayBucket bucket : buckets) saved.add(bucket);
    }

    /** Clients get only the kinetic state from the block; the line and its buckets come by their own packet. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        lightSave = true;
        try {
            return super.getUpdateTag(registries);
        } finally {
            lightSave = false;
        }
    }
}
