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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
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

    /** A rider asking for a bucket at node {@code node}; {@code sent} when a bucket has been sent out to fetch them. */
    private static final class Waiting {
        final ServerPlayer player;
        final int node;
        final long expires;
        boolean sent;

        Waiting(ServerPlayer player, int node, long expires) {
            this.player = player;
            this.node = node;
            this.expires = expires;
        }
    }

    /** Blocks within which a waiting rider has to stay of their station. */
    private static final double STAY = 7.0;
    private final Map<UUID, Waiting> waiting = new HashMap<>();
    private double stepFrom;
    private boolean holding;

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
            if (bucket.rider != null) dropRider(level, bucket, "snapped");
            spare++;
        }
        buckets.clear();
        waiting.clear();
        updateHold(level, false);
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
        terminal.stepFrom = terminal.advance;
        terminal.run(server, target);
        terminal.rides(server, time);
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
                boolean out = RopewayPath.crosses(before, after, at, loop), home = RopewayPath.crosses(before, after, loop - at, loop);
                if (!out && !home) continue;
                if (isAngle(level, node)) {
                    level.playSound(null, nodes.get(node), RopewayRegistry.ANGLE_TURN.get(), SoundSource.BLOCKS, 0.8f, 0.9f + level.getRandom().nextFloat() * 0.2f);
                    if (out && !bucket.stack.isEmpty()) topUp(level, bucket, node);
                } else {
                    level.playSound(null, nodes.get(node), RopewayRegistry.SHEAVE.get(), SoundSource.BLOCKS, 0.7f, 0.85f + level.getRandom().nextFloat() * 0.3f);
                }
            }
            if (!RopewayPath.crosses(before, after, 0, loop) || bucket.rider != null) continue;
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
        if (spare <= 0 || speed <= 0 || !roomAtTerminal(loop)) return;
        ItemStack cargo = take(level);
        if (cargo.isEmpty()) return;
        spare--;
        buckets.add(new RopewayBucket(((-advance % loop) + loop) % loop, cargo, false));
        hung(level);
        netDirty = true;
        setChanged();
    }

    /** Whether the rope behind the terminal is clear for a bucket to be hung. */
    private boolean roomAtTerminal(double loop) {
        for (RopewayBucket bucket : buckets) {
            double s = bucket.at(advance, loop);
            if (Math.min(s, loop - s) < SPACING) return false;
        }
        return true;
    }

    private void hung(ServerLevel level) {
        level.playSound(null, worldPosition, RopewayRegistry.BUCKET_HANG.get(), SoundSource.BLOCKS, 0.8f, 0.9f + level.getRandom().nextFloat() * 0.2f);
    }

    // ---------------------------------------------------------------- loading and unloading

    /** One stack from the container behind the terminal, else the one under it. */
    private ItemStack take(ServerLevel level) {
        return pull(level, worldPosition, facing().getOpposite(), stack -> true, 0);
    }

    /**
     * A stack of what {@code wanted} accepts from the container behind {@code station} (on its {@code back} side), else
     * the one beneath it; at most {@code limit} items, or a full stack when that is 0.
     */
    private static ItemStack pull(ServerLevel level, BlockPos station, Direction back, Predicate<ItemStack> wanted, int limit) {
        for (Direction side : new Direction[] {back, Direction.DOWN}) {
            Container source = HopperBlockEntity.getContainerAt(level, station.relative(side));
            if (source == null) continue;
            Direction face = side.getOpposite();
            int[] slots = source instanceof WorldlyContainer worldly ? worldly.getSlotsForFace(face) : null;
            int count = slots == null ? source.getContainerSize() : slots.length;
            for (int n = 0; n < count; n++) {
                int slot = slots == null ? n : slots[n];
                ItemStack stack = source.getItem(slot);
                if (stack.isEmpty() || !wanted.test(stack)) continue;
                if (source instanceof WorldlyContainer worldly && !worldly.canTakeItemThroughFace(slot, stack, face)) continue;
                ItemStack taken = source.removeItem(slot, limit > 0 ? Math.min(limit, stack.getCount()) : stack.getMaxStackSize());
                source.setChanged();
                if (!taken.isEmpty()) return taken;
            }
        }
        return ItemStack.EMPTY;
    }

    /** Whether node {@code node} (a tower position on the line) is an angle station. */
    private boolean isAngle(ServerLevel level, int node) {
        BlockPos pos = nodes.get(node);
        return level.hasChunkAt(pos) && level.getBlockState(pos).is(RopewayRegistry.ANGLE_STATION.get());
    }

    /** A bucket passing an angle station takes what it can of its own kind from the chest behind or beneath the station. */
    private void topUp(ServerLevel level, RopewayBucket bucket, int node) {
        BlockPos station = nodes.get(node);
        BlockState state = level.getBlockState(station);
        int room = bucket.stack.getMaxStackSize() - bucket.stack.getCount();
        if (room <= 0) return;
        ItemStack more = pull(level, station, state.getValue(RopewayAngleBlock.FACING).getOpposite(),
                stack -> ItemStack.isSameItemSameComponents(stack, bucket.stack), room);
        if (more.isEmpty()) return;
        bucket.stack.grow(more.getCount());
        netDirty = true;
        level.playSound(null, station, RopewayRegistry.TOP_UP.get(), SoundSource.BLOCKS, 0.7f, 0.9f + 0.2f * bucket.stack.getCount() / bucket.stack.getMaxStackSize());
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, bucket.stack.getItem()), station.getX() + 0.5, station.getY() + 1.1, station.getZ() + 0.5,
                2 + more.getCount() / 8, 0.15, 0.1, 0.15, 0.05);
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

    // ---------------------------------------------------------------- riding

    /** How long a rider waits at a station for a bucket, in ticks: long enough for one to come round the whole loop. */
    private int patience() {
        double speedNow = Math.max(0.05, kinetic().rpm() * SPEED_PER_RPM);
        return 400 + (int) Math.ceil(2.0 * path.length() / speedNow);
    }

    /** Riders asking for a bucket at a station. */
    public int waiting() {
        return waiting.size();
    }

    /** Whether a bucket is out that nobody sits in: every one of them comes home past the return, empty. */
    private boolean hasBucketOut() {
        for (RopewayBucket bucket : buckets) if (bucket.rider == null) return true;
        return false;
    }

    private void say(Player player, String key) {
        player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".ropeway.ride." + key));
    }

    /**
     * An empty hand on a station asks for a seat. At the terminal one of the spare buckets is hung for the rider and
     * goes out empty. Anywhere else the next empty bucket coming home takes them up as it swings round the wheel, and if
     * none is out, a spare one is sent to fetch them.
     */
    public InteractionResult ride(ServerPlayer player, BlockPos station) {
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        int node = nodes.indexOf(station);
        if (path == null || node < 0) say(player, "no_line");
        else if (player.isPassenger()) return InteractionResult.SUCCESS_SERVER;
        else if (waiting.containsKey(player.getUUID())) say(player, "waiting");
        else if (!powered()) say(player, "unpowered");
        else if (!server.hasChunkAt(nodes.getLast())) say(player, "far_station");
        else if (spare == 0 && (node == 0 || !hasBucketOut())) say(player, node == 0 ? "no_spare" : "no_bucket");
        else {
            waiting.put(player.getUUID(), new Waiting(player, node, server.getGameTime() + patience()));
            server.playSound(null, station, RopewayRegistry.BUCKET_HANG.get(), SoundSource.BLOCKS, 0.5f, 1.3f);
            say(player, node == 0 ? "waiting_out" : "waiting_back");
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    /** Boards, seats, rides and lets off; called every tick once the rope has moved. */
    private void rides(ServerLevel level, long time) {
        RopewayPath line = path;
        if (line == null) return;
        double loop = 2.0 * line.length();
        int riders = 0;
        Iterator<RopewayBucket> each = buckets.iterator();
        while (each.hasNext()) {
            RopewayBucket bucket = each.next();
            if (bucket.rider == null) continue;
            ServerPlayer rider = bucket.rider;
            Entity seat = bucket.seat == null ? null : level.getEntity(bucket.seat);
            if (rider.isRemoved() || !(seat instanceof RopewaySeatEntity chair) || chair.isRemoved() || rider.getVehicle() != chair) {
                if (seat != null) seat.discard();
                bucket.rider = null;
                bucket.seat = null;
                netDirty = true;
                continue;
            }
            double before = bucket.at(stepFrom, loop), after = bucket.at(advance, loop);
            int station = time > bucket.boarded ? stationPassed(level, line, before, after, loop) : -1;
            if (station >= 0) {
                letOff(level, bucket, rider, chair, station);
                // Home at the terminal, an empty bucket is simply hung up again.
                if (station == 0 && bucket.stack.isEmpty()) {
                    each.remove();
                    spare++;
                }
                continue;
            }
            RopewayPath.Point point = line.point(after);
            chair.drive(point.position(), point.heading(), speed, time);
            if ((time + riders) % 16 == 0) {
                level.playSound(null, chair.getX(), chair.getY() + 1.0, chair.getZ(), RopewayRegistry.RIDE_WIND.get(), SoundSource.BLOCKS,
                        (float) (0.15 + speed * 1.5), 0.8f + (float) speed * 1.5f);
            }
            riders++;
        }
        serveWaiting(level, line, loop, time);
        updateHold(level, riders > 0 || !waiting.isEmpty());
    }

    private void updateHold(ServerLevel level, boolean hold) {
        if (hold == holding) return;
        holding = hold;
        RopewayRides.hold(level, worldPosition, hold);
    }

    /** The first station (the terminal, an angle station or the return) the bucket passed between {@code before} and {@code after}, else -1. */
    private int stationPassed(ServerLevel level, RopewayPath line, double before, double after, double loop) {
        for (int node = 0; node < nodes.size(); node++) {
            boolean station = node == 0 || node == nodes.size() - 1 || isAngle(level, node);
            if (!station) continue;
            double at = line.at(node);
            if (RopewayPath.crosses(before, after, at, loop) || RopewayPath.crosses(before, after, loop - at, loop)) return node;
        }
        return -1;
    }

    private void serveWaiting(ServerLevel level, RopewayPath line, double loop, long time) {
        Iterator<Map.Entry<UUID, Waiting>> each = waiting.entrySet().iterator();
        while (each.hasNext()) {
            Map.Entry<UUID, Waiting> entry = each.next();
            Waiting wait = entry.getValue();
            ServerPlayer player = wait.player;
            if (player.isRemoved() || player.isPassenger() || wait.node >= nodes.size()) {
                each.remove();
                continue;
            }
            BlockPos station = nodes.get(wait.node);
            if (player.level() != level || player.distanceToSqr(Vec3.atCenterOf(station)) > STAY * STAY || time > wait.expires) {
                say(player, "gave_up");
                each.remove();
                continue;
            }
            if (wait.node == 0) {
                if (spare > 0 && speed > 0 && roomAtTerminal(loop)) {
                    spare--;
                    RopewayBucket bucket = new RopewayBucket(((-advance % loop) + loop) % loop, ItemStack.EMPTY, false);
                    buckets.add(bucket);
                    board(level, player, bucket, time);
                    each.remove();
                }
                continue;
            }
            double mark = loop - line.at(wait.node);
            RopewayBucket pick = null;
            for (RopewayBucket bucket : buckets) {
                if (!bucket.stack.isEmpty() || bucket.rider != null) continue;
                if (RopewayPath.crosses(bucket.at(stepFrom, loop), bucket.at(advance, loop), mark, loop)) pick = bucket;
            }
            if (pick != null) {
                board(level, player, pick, time);
                each.remove();
            } else if (!wait.sent && !hasBucketOut() && spare > 0 && speed > 0 && roomAtTerminal(loop)) {
                spare--;
                buckets.add(new RopewayBucket(((-advance % loop) + loop) % loop, ItemStack.EMPTY, false));
                wait.sent = true;
                netDirty = true;
                hung(level);
                say(player, "sent");
            }
        }
    }

    private void board(ServerLevel level, ServerPlayer player, RopewayBucket bucket, long time) {
        RopewayPath line = path;
        if (line == null) return;
        double loop = 2.0 * line.length();
        RopewayPath.Point point = line.point(bucket.at(advance, loop));
        RopewaySeatEntity seat = RopewayRegistry.SEAT.get().create(level, EntitySpawnReason.TRIGGERED);
        if (seat == null) return;
        seat.drive(point.position(), point.heading(), speed, time);
        level.addFreshEntity(seat);
        player.startRiding(seat, true, false);
        bucket.rider = player;
        bucket.seat = seat.getUUID();
        bucket.boarded = time;
        netDirty = true;
        level.playSound(null, seat.getX(), seat.getY() + 1.2, seat.getZ(), RopewayRegistry.SEAT_CLIP.get(), SoundSource.BLOCKS, 0.9f, 1.0f);
        level.sendParticles(ParticleTypes.CLOUD, seat.getX(), seat.getY() + 1.4, seat.getZ(), 6, 0.25, 0.1, 0.25, 0.02);
        say(player, "aboard");
    }

    /** The rider steps off at {@code node}: onto the top of the station, with a clack and a puff. */
    private void letOff(ServerLevel level, RopewayBucket bucket, ServerPlayer rider, RopewaySeatEntity seat, int node) {
        BlockPos at = nodes.get(node);
        seat.release();
        rider.stopRiding();
        seat.discard();
        rider.teleportTo(level, at.getX() + 0.5, at.getY() + 1.0, at.getZ() + 0.5, Set.of(), rider.getYRot(), rider.getXRot(), true);
        bucket.rider = null;
        bucket.seat = null;
        netDirty = true;
        level.playSound(null, at, RopewayRegistry.SEAT_RELEASE.get(), SoundSource.BLOCKS, 0.9f, 1.0f);
        level.sendParticles(ParticleTypes.CLOUD, at.getX() + 0.5, at.getY() + 1.1, at.getZ() + 0.5, 8, 0.3, 0.1, 0.3, 0.02);
        say(rider, node == 0 ? "off_terminal" : node == nodes.size() - 1 ? "off_return" : "off_angle");
    }

    /** The line is down under a rider: the seat goes and they come down slowly, with a word. */
    private void dropRider(ServerLevel level, RopewayBucket bucket, String key) {
        ServerPlayer rider = bucket.rider;
        Entity seat = bucket.seat == null ? null : level.getEntity(bucket.seat);
        if (seat != null) seat.discard();
        if (rider != null) say(rider, key);
        bucket.rider = null;
        bucket.seat = null;
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
        for (RopewayBucket bucket : buckets) views.add(new RopewayPayloads.BucketView((float) bucket.offset, bucket.stack, bucket.rider != null));
        tell(level, nodes, new RopewayPayloads.LineState(worldPosition, nodes, advance, (float) speed, views));
    }

    /** Sends {@code state} to every player near a piece of the line given by {@code line}. */
    private void tell(ServerLevel level, List<BlockPos> line, RopewayPayloads.LineState state) {
        if (line.isEmpty()) return;
        RopewayPath shape = RopewayPath.of(line);
        for (ServerPlayer player : level.players()) {
            if (shape.distanceTo(player.position()) <= TELL_RANGE && player.connection.hasChannel(RopewayPayloads.LineState.TYPE)) PacketDistributor.sendToPlayer(player, state);
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
