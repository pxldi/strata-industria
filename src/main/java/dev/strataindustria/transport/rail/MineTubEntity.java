package dev.strataindustria.transport.rail;

import com.mojang.datafixers.util.Pair;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.Config;
import dev.strataindustria.transport.foot.FootRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecartContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The mine tub (outposts spec 5.2): nine slots on four wheels. On the wooden rail it is slow, loses speed fast and
 * more so when loaded. A tub reaching a tub stop is held there; tubs couple into a consist whose followers are
 * drawn after the lead; a tub standing on a tipple tips into the container below. A trip from one stop to
 * another proves the line between two charters.
 */
public class MineTubEntity extends AbstractMinecartContainer {
    public static final int SLOTS = 9;
    /** Blocks between coupled tubs, centre to centre (spec 5.2). */
    public static final double SPACING = 1.25;
    /** Speed lost per tick on the flat, and the same when more than half full. */
    public static final double FRICTION = 0.004;
    public static final double LOADED_FRICTION = 0.006;
    /** Top speed on the wooden rail (spec 5.1). */
    public static final double RAIL_CAP = 0.2;
    /** What a steel rail keeps of a vehicle's speed each tick: a little under the vanilla rails. */
    private static final double STEEL_DRAG = 0.992;
    /** Fastest a follower is drawn after its lead: a little over the steel track limit. */
    private static final double FOLLOW_CAP = 0.55;
    /** How far a coupling reaches. */
    public static final double COUPLE_REACH = 3.0;
    /** Past this a follower has lost its lead and the coupling lets go. */
    private static final double SNAP = 4.0;
    /** Ticks a tub takes to tip over or right itself, and between one stack and the next. */
    private static final float TIP_STEP = 0.05f;
    private static final int DUMP_EVERY = 10;
    /** Where on its block a tub comes to rest against a buffer, measured from the block centre toward the timber. */
    private static final double BUFFER_LINE = -0.05;

    private static final EntityDataAccessor<Float> DATA_TIP = SynchedEntityData.defineId(MineTubEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_COUPLED = SynchedEntityData.defineId(MineTubEntity.class, EntityDataSerializers.BOOLEAN);

    private @Nullable UUID leaderId;
    private @Nullable UUID followerId;
    private @Nullable BlockPos holdPos;
    private @Nullable BlockPos ignoreStop;
    private @Nullable UUID pusher;
    private boolean driven;
    private final TrackTrace trace = new TrackTrace();
    private float tip;
    private int dumpCooldown;
    private int noRoomCooldown;
    private double clackDistance;
    private int lastClack;
    private boolean thudded;
    private int winchStamp = -100;
    private Vec3 winchVelocity = Vec3.ZERO;
    private @Nullable Vec3 winchAnchor;
    // client
    private float tipVisual, tipVisualO;

    public MineTubEntity(EntityType<? extends MineTubEntity> type, Level level) {
        super(type, level);
    }

    // ---------------------------------------------------------------- item, menu, data

    @Override
    protected Item getDropItem() {
        return RailRegistry.MINE_TUB.get();
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(RailRegistry.MINE_TUB.get());
    }

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new DispenserMenu(containerId, inventory, this);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_TIP, 0f);
        entityData.define(DATA_COUPLED, false);
    }

    /** A tub is not a seat. */
    @Override
    public boolean isRideable() {
        return false;
    }

    /** How far it has tipped, 0 to 1, for the renderer. */
    public float tip(float partialTicks) {
        return Mth.lerp(partialTicks, tipVisualO, tipVisual);
    }

    /** Whether it is coupled to another tub, either side. */
    public boolean isCoupled() {
        return entityData.get(DATA_COUPLED);
    }

    public TrackTrace trace() {
        return trace;
    }

    public @Nullable UUID pusher() {
        return pusher;
    }

    // ---------------------------------------------------------------- the track under us

    /** The rail block under the tub, if it is on any rail. */
    protected @Nullable BlockPos railBlock() {
        BlockPos pos = getCurrentBlockPosOrRailBelow();
        return BaseRailBlock.isRail(level().getBlockState(pos)) ? pos : null;
    }

    /** True while the tub is on a rail block of this mod. */
    public boolean onOurTrack() {
        BlockPos pos = getCurrentBlockPosOrRailBelow();
        return level().getBlockState(pos).is(RailRegistry.TRACK);
    }

    /** The horizontal direction the rail at {@code pos} runs, as a unit vector (the second exit minus the first). */
    public Vec3 railAxis(BlockPos pos) {
        BlockState state = level().getBlockState(pos);
        if (state.getBlock() instanceof BaseRailBlock rail) {
            RailShape shape = rail.getRailDirection(state, level(), pos, this);
            Pair<Vec3i, Vec3i> exits = AbstractMinecart.exits(shape);
            double x = exits.getSecond().getX() - exits.getFirst().getX();
            double z = exits.getSecond().getZ() - exits.getFirst().getZ();
            double length = Math.sqrt(x * x + z * z);
            if (length > 0) return new Vec3(x / length, 0, z / length);
        }
        return new Vec3(1, 0, 0);
    }

    /** The grade of the track under the vehicle, or null on a vanilla rail or off the rails. */
    public @Nullable RailGrade grade() {
        return RailGrade.of(level().getBlockState(getCurrentBlockPosOrRailBelow()));
    }

    /** Wooden rail is slow and steel track quick; other rails keep the vanilla limit. */
    @Override
    protected double getMaxSpeed(ServerLevel level) {
        RailGrade grade = grade();
        return grade == null ? super.getMaxSpeed(level) : grade.cap();
    }

    /** The wooden rail holds a tub back by a fixed amount a tick, more when it is heavy; followers keep what the lead gave. */
    @Override
    protected Vec3 applyNaturalSlowdown(Vec3 movement) {
        if (leaderId != null) return movement;
        if (winched()) return winchVelocity;
        RailGrade grade = grade();
        if (grade == RailGrade.STEEL) return movement.multiply(STEEL_DRAG, 0.0, STEEL_DRAG);
        if (grade == null) {
            // Off our track the container's drag counts slots, which a tank or a flat wagon has none of.
            return getContainerSize() == 0 ? movement.multiply(0.98, 0.0, 0.98) : super.applyNaturalSlowdown(movement);
        }
        double speed = movement.horizontalDistance();
        if (speed < 1.0E-6) return movement;
        double next = Math.max(0, speed - (loaded() ? LOADED_FRICTION : FRICTION));
        double factor = next / speed;
        return movement.multiply(factor, 0.0, factor);
    }

    /** More than half the slots are in use. */
    public boolean loaded() {
        int size = getContainerSize();
        int used = 0;
        for (int slot = 0; slot < size; slot++) if (!getItem(slot).isEmpty()) used++;
        return size > 0 && used * 2 > size;
    }

    // ---------------------------------------------------------------- ticking

    @Override
    public void tick() {
        if (!level().isClientSide() && leaderId != null) {
            MineTubEntity lead = leader();
            if (lead == null || lead.isRemoved()) {
                decouple();
            } else if (!driven) {
                // The lead moves us, after it has moved itself.
                return;
            }
        }
        driven = false;
        step();
    }

    private void step() {
        if (level() instanceof ServerLevel server) {
            if (holdPos != null) settleOnStop();
            super.tick();
            if (holdPos != null && leaderId == null) setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
            if (winchAnchor != null) {
                if (winched()) {
                    setPos(winchAnchor.x, getY(), winchAnchor.z);
                    setDeltaMovement(Vec3.ZERO);
                } else {
                    winchAnchor = null;
                }
            }
            buffer(server);
            tipple(server);
            roll(server);
            track(server);
            MineTubEntity follower = follower();
            if (follower != null) follower.driveFrom(this);
        } else {
            super.tick();
            tipVisualO = tipVisual;
            tipVisual += (entityData.get(DATA_TIP) - tipVisual) * 0.4f;
        }
    }

    /** A held lead glides to the middle of its stop. */
    private void settleOnStop() {
        BlockPos stop = holdPos;
        if (stop == null) return;
        Vec3 axis = railAxis(stop);
        double offset = (getX() - (stop.getX() + 0.5)) * axis.x + (getZ() - (stop.getZ() + 0.5)) * axis.z;
        double move = Mth.clamp(-offset * 0.5, -0.05, 0.05);
        if (Math.abs(offset) < 0.02) move = 0;
        setDeltaMovement(axis.x * move, getDeltaMovement().y, axis.z * move);
    }

    /** Drives this follower after its lead: speed along the track follows the lead, corrected toward the coupling distance. */
    private void driveFrom(MineTubEntity lead) {
        double dx = lead.getX() - getX(), dz = lead.getZ() - getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance > SNAP) {
            decouple();
            return;
        }
        double ux = distance > 1.0E-4 ? dx / distance : 0, uz = distance > 1.0E-4 ? dz / distance : 0;
        Vec3 leadMotion = lead.getDeltaMovement();
        double along = leadMotion.x * ux + leadMotion.z * uz;
        double speed = Mth.clamp(along + 0.6 * (distance - SPACING), -FOLLOW_CAP, FOLLOW_CAP);
        setDeltaMovement(ux * speed, getDeltaMovement().y, uz * speed);
        driven = true;
        tick();
    }

    /** A lead on a tub stop is held by it; a lead on the track notes the blocks it rolls over. */
    private void track(ServerLevel server) {
        BlockPos pos = getCurrentBlockPosOrRailBelow();
        BlockState state = server.getBlockState(pos);
        if (ignoreStop != null && !ignoreStop.equals(pos)) ignoreStop = null;
        if (leaderId != null) {
            trace.lose();
            return;
        }
        if (BaseRailBlock.isRail(state)) trace.ride(pos, state.is(RailRegistry.TRACK));
        else if (!isOnRails()) trace.lose();
        if (holdPos == null && state.getBlock() instanceof TubStopBlock && !pos.equals(ignoreStop)
                && server.getBlockEntity(pos) instanceof TubStopBlockEntity stop && !stop.isHolding()) {
            stop.capture(server, this);
        }
    }

    // ---------------------------------------------------------------- stops

    /** The stop holds this tub where it stands. */
    public void holdAt(BlockPos stop) {
        holdPos = stop.immutable();
        setDeltaMovement(Vec3.ZERO);
    }

    public boolean isHeldAt(BlockPos stop) {
        return stop.equals(holdPos);
    }

    /** True while a stop has this vehicle. */
    protected final boolean isHeld() {
        return holdPos != null;
    }

    /** Whether a stop has the vehicle held right now. */
    public boolean holding() {
        return holdPos != null;
    }

    /** The stop holding this vehicle, if any. */
    protected final @Nullable BlockPos heldAt() {
        return holdPos;
    }

    /** The stop this vehicle has just left and will not be taken by again until it is clear of it. */
    protected final @Nullable BlockPos ignoredStop() {
        return ignoreStop;
    }

    /** True while this vehicle is pressed against a rail buffer. */
    protected final boolean atBuffer() {
        return thudded;
    }

    /** Whether the stop may let this consist go now. The pony says no while it is hungry. */
    public boolean canDepart(ServerLevel server) {
        return true;
    }

    /** True for the first vehicle of a consist: nothing is coupled ahead of it. */
    public boolean isLead() {
        return leaderId == null;
    }

    /** The way this vehicle wants to go along the track, a unit vector, or zero when it has no mind of its own. */
    public Vec3 intent() {
        return Vec3.ZERO;
    }

    /** A winch (outposts spec 5.4) takes the lead: it moves at {@code velocity} this tick, or holds still where it is. */
    public void winchControl(Vec3 velocity, boolean hold) {
        winchStamp = tickCount;
        winchVelocity = velocity;
        if (hold && winchAnchor == null) winchAnchor = position();
        if (!hold) winchAnchor = null;
    }

    /** True while a winch has had hold of this vehicle in the last couple of ticks. */
    public boolean winched() {
        return tickCount - winchStamp <= 2;
    }

    /** The stop lets go, with a shove along the track. */
    public void release(BlockPos stop, Vec3 push) {
        holdPos = null;
        ignoreStop = stop.immutable();
        setDeltaMovement(push.x, 0, push.z);
    }

    // ---------------------------------------------------------------- walking the line

    /** How far into its block a self-propelled vehicle goes before it is stopped at the end of the line. */
    private static final double NEAR_EDGE = 0.2;

    /** Whether the track climbs on the way the vehicle is going. */
    protected boolean uphill(BlockPos rail, Vec3 heading) {
        BlockState state = level().getBlockState(rail);
        if (!(state.getBlock() instanceof BaseRailBlock block)) return false;
        Direction up = switch (state.getValue(block.getShapeProperty())) {
            case ASCENDING_EAST -> Direction.EAST;
            case ASCENDING_WEST -> Direction.WEST;
            case ASCENDING_NORTH -> Direction.NORTH;
            case ASCENDING_SOUTH -> Direction.SOUTH;
            default -> null;
        };
        return up != null && heading.x * up.getStepX() + heading.z * up.getStepZ() > 0.1;
    }

    /** True when the track ends ahead: no rail where the vehicle is headed, in a loaded chunk. */
    protected boolean deadEnd(BlockPos rail, Vec3 heading) {
        BlockState state = level().getBlockState(rail);
        if (!(state.getBlock() instanceof BaseRailBlock block)) return false;
        Vec3 ahead = null;
        double best = 0.1;
        for (Vec3i exit : new Vec3i[] {
                AbstractMinecart.exits(state.getValue(block.getShapeProperty())).getFirst(),
                AbstractMinecart.exits(state.getValue(block.getShapeProperty())).getSecond()}) {
            double dot = exit.getX() * heading.x + exit.getZ() * heading.z;
            if (dot > best) {
                best = dot;
                ahead = new Vec3(exit.getX(), exit.getY(), exit.getZ());
            }
        }
        if (ahead == null) return false;
        BlockPos next = rail.offset((int) ahead.x, (int) ahead.y, (int) ahead.z);
        if (!level().hasChunkAt(next)) return false;
        return !(BaseRailBlock.isRail(level(), next) || BaseRailBlock.isRail(level(), next.below()) || BaseRailBlock.isRail(level(), next.above()));
    }

    /** Whether the vehicle is far enough into its block, going the way it heads, to be stopped before the edge. */
    protected boolean nearEdge(BlockPos rail, Vec3 heading) {
        double offset = (getX() - (rail.getX() + 0.5)) * heading.x + (getZ() - (rail.getZ() + 0.5)) * heading.z;
        return offset > NEAR_EDGE;
    }

    // ---------------------------------------------------------------- buffer

    private void buffer(ServerLevel server) {
        BlockPos pos = getCurrentBlockPosOrRailBelow();
        BlockState state = server.getBlockState(pos);
        if (!(state.getBlock() instanceof RailBufferBlock)) {
            thudded = false;
            return;
        }
        Direction face = state.getValue(RailBufferBlock.FACING);
        Vec3 motion = getDeltaMovement();
        double along = motion.x * face.getStepX() + motion.z * face.getStepZ();
        double offset = (getX() - (pos.getX() + 0.5)) * face.getStepX() + (getZ() - (pos.getZ() + 0.5)) * face.getStepZ();
        // Rolling up to the timber is free; it only bites once the tub's centre is at it.
        if (offset <= BUFFER_LINE) return;
        if (along > 0.03 && !thudded) {
            boolean steel = state.getBlock() instanceof SteelBufferBlock;
            server.playSound(null, getX(), getY(), getZ(), (steel ? RailwayRegistry.BUFFER_CLANG : RailRegistry.TUB_THUD).get(),
                    SoundSource.NEUTRAL, 0.5f + (float) along * 2f, steel ? 0.8f : 0.9f);
        }
        thudded = true;
        if (offset > BUFFER_LINE) {
            double back = offset - BUFFER_LINE;
            setPos(getX() - face.getStepX() * back, getY(), getZ() - face.getStepZ() * back);
        }
        double keep = along > 0 ? along : 0;
        setDeltaMovement(motion.x - face.getStepX() * keep, motion.y, motion.z - face.getStepZ() * keep);
    }

    // ---------------------------------------------------------------- tipple

    private void tipple(ServerLevel server) {
        BlockPos pos = getCurrentBlockPosOrRailBelow();
        boolean standing = tips() && server.getBlockState(pos).is(RailRegistry.TIPPLE_RAIL.get()) && getDeltaMovement().horizontalDistanceSqr() < 1.0E-4
                && !isEmpty() && noRoomCooldown <= 0;
        if (noRoomCooldown > 0) noRoomCooldown--;
        float target = standing ? 1f : 0f;
        float before = tip;
        tip += Mth.clamp(target - tip, -TIP_STEP, TIP_STEP);
        if (Math.abs(tip - before) > 1.0E-4f) entityData.set(DATA_TIP, tip);
        if (before < 0.5f && tip >= 0.5f) {
            server.playSound(null, getX(), getY(), getZ(), RailRegistry.TIPPLE_DUMP.get(), SoundSource.BLOCKS, 0.7f, 0.9f);
        }
        if (!standing || tip < 0.95f) return;
        if (--dumpCooldown > 0) return;
        dumpCooldown = DUMP_EVERY;
        Container bin = HopperBlockEntity.getContainerAt(server, pos.below());
        boolean moved = false;
        if (bin != null) {
            for (int slot = 0; slot < getContainerSize() && !moved; slot++) {
                ItemStack stack = getItem(slot);
                if (stack.isEmpty()) continue;
                ItemStack rest = HopperBlockEntity.addItem(null, bin, stack.copy(), Direction.UP);
                if (rest.getCount() != stack.getCount()) {
                    setItem(slot, rest);
                    moved = true;
                }
            }
        }
        if (moved) {
            server.playSound(null, getX(), getY(), getZ(), RailRegistry.TIPPLE_DUMP.get(), SoundSource.BLOCKS, 0.5f, 1.2f);
        } else {
            noRoomCooldown = 60;
        }
    }

    /** Whether the vehicle tips its slots out on a tipple; tanks and flat wagons do not. */
    protected boolean tips() {
        return true;
    }

    /** How far the load has tipped, for tests. */
    public float tipAmount() {
        return tip;
    }

    // ---------------------------------------------------------------- sound

    protected void roll(ServerLevel server) {
        if (!onOurTrack()) return;
        double speed = getDeltaMovement().horizontalDistance();
        if (speed < 0.02) return;
        clackDistance += speed;
        if (clackDistance < 0.5 || tickCount - lastClack < 3) return;
        clackDistance = 0;
        lastClack = tickCount;
        boolean steel = grade() == RailGrade.STEEL;
        server.playSound(null, getX(), getY(), getZ(), (steel ? RailwayRegistry.CLATTER_STEEL : RailRegistry.TUB_ROLL).get(), SoundSource.NEUTRAL,
                (leaderId == null ? 0.4f : 0.25f) + (float) speed, (steel ? 0.9f : 0.8f) + (float) speed * (steel ? 0.8f : 1.5f));
    }

    // ---------------------------------------------------------------- pushing and using

    @Override
    public void push(Entity entity) {
        if (entity instanceof Player player) pusher = player.getUUID();
        super.push(entity);
    }

    /** A push on a follower goes to the head of its consist; hand-pushed consists have no other lead. */
    @Override
    public void push(double x, double y, double z) {
        if (leaderId != null && !level().isClientSide()) {
            MineTubEntity head = head();
            if (head != this) {
                head.push(x, y, z);
                return;
            }
        }
        super.push(x, y, z);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        pusher = player.getUUID();
        ItemStack held = player.getItemInHand(hand);
        if (held.is(Items.IRON_CHAIN) || held.is(FootRegistry.ROPE.get())) {
            if (player instanceof ServerPlayer serverPlayer) tryCouple(serverPlayer);
            return InteractionResult.SUCCESS;
        }
        if (player.isSecondaryUseActive() && held.isEmpty() && (leaderId != null || followerId != null)) {
            if (!level().isClientSide()) {
                if (leaderId != null) decouple();
                else if (follower() != null) follower().decouple();
                level().playSound(null, getX(), getY(), getZ(), RailRegistry.TUB_COUPLE.get(), SoundSource.NEUTRAL, 0.6f, 0.7f);
            }
            return InteractionResult.SUCCESS;
        }
        return openLoad(player, hand, location);
    }

    /** What a use on the vehicle does when it is not about coupling: a tub opens its slots; a wagon may do more. */
    protected InteractionResult openLoad(Player player, InteractionHand hand, Vec3 location) {
        return super.interact(player, hand, location);
    }

    // ---------------------------------------------------------------- coupling

    public enum Coupling { OK, NONE, FOLLOWING, TOO_LONG, LEADS }

    /** The tub ahead of this one in its consist, if any. */
    public @Nullable MineTubEntity leader() {
        return leaderId != null && level() instanceof ServerLevel server && server.getEntity(leaderId) instanceof MineTubEntity tub ? tub : null;
    }

    /** The tub behind this one, if any. */
    public @Nullable MineTubEntity follower() {
        return followerId != null && level() instanceof ServerLevel server && server.getEntity(followerId) instanceof MineTubEntity tub ? tub : null;
    }

    /** The first tub of the consist this one is in. */
    public MineTubEntity head() {
        MineTubEntity tub = this;
        for (int guard = 0; guard < 64; guard++) {
            MineTubEntity ahead = tub.leader();
            if (ahead == null) return tub;
            tub = ahead;
        }
        return tub;
    }

    /** This tub and every tub behind it, in order. */
    public List<MineTubEntity> consist() {
        List<MineTubEntity> chain = new ArrayList<>();
        MineTubEntity tub = head();
        for (int guard = 0; tub != null && guard < 64; guard++) {
            chain.add(tub);
            tub = tub.follower();
        }
        return chain;
    }

    /**
     * The head walks round to the other end of its consist: the chain is turned about and the head put behind what was
     * the last tub, so it leads the other way. False when there is no rail there to stand on.
     */
    protected boolean reverseConsist(Vec3 heading) {
        List<MineTubEntity> chain = consist();
        if (chain.size() < 2 || chain.get(0) != this || heading.lengthSqr() < 1.0E-6) return true;
        MineTubEntity tail = chain.get(chain.size() - 1);
        Vec3 spot = tail.position().subtract(heading.normalize().scale(SPACING));
        BlockPos at = BlockPos.containing(spot);
        if (!(BaseRailBlock.isRail(level(), at) || BaseRailBlock.isRail(level(), at.below()) || BaseRailBlock.isRail(level(), at.above()))) return false;
        for (int i = chain.size() - 1; i >= 1; i--) {
            MineTubEntity tub = chain.get(i);
            tub.leaderId = i == chain.size() - 1 ? getUUID() : chain.get(i + 1).getUUID();
            tub.followerId = i == 1 ? null : chain.get(i - 1).getUUID();
            tub.refreshCoupled();
        }
        followerId = tail.getUUID();
        setPos(spot.x, tail.getY(), spot.z);
        setDeltaMovement(Vec3.ZERO);
        refreshCoupled();
        return true;
    }

    /** The tub nearest this one that it can be coupled behind: free at the back, in reach, not in its own consist. */
    private @Nullable MineTubEntity findFront() {
        List<MineTubEntity> mine = consist();
        MineTubEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (MineTubEntity other : level().getEntitiesOfClass(MineTubEntity.class, getBoundingBox().inflate(COUPLE_REACH))) {
            if (other == this || mine.contains(other) || other.followerId != null) continue;
            double distance = other.distanceTo(this);
            if (distance > COUPLE_REACH || Math.abs(other.getY() - getY()) > 1.5) continue;
            if (distance < bestDistance) {
                best = other;
                bestDistance = distance;
            }
        }
        return best;
    }

    /** Couples this tub behind the nearest free tub, or says why not. */
    public Coupling couple() {
        if (leaderId != null) return Coupling.FOLLOWING;
        MineTubEntity front = findFront();
        if (front == null) return Coupling.NONE;
        int length = front.consist().size() + consist().size();
        if (length - 1 > maxConsist(front.head())) return Coupling.TOO_LONG;
        front.followerId = getUUID();
        leaderId = front.getUUID();
        front.entityData.set(DATA_COUPLED, true);
        entityData.set(DATA_COUPLED, true);
        trace.lose();
        return Coupling.OK;
    }

    /** Vehicles that may follow {@code lead}: a locomotive pulls more than a pony. */
    private static int maxConsist(MineTubEntity lead) {
        return lead instanceof SteamLocomotiveEntity ? Config.TRANSPORT_MAX_CONSIST_T4.getAsInt() : Config.TRANSPORT_MAX_CONSIST_T3.getAsInt();
    }

    protected void tryCouple(ServerPlayer player) {
        Coupling result = couple();
        if (result == Coupling.OK) {
            level().playSound(null, getX(), getY(), getZ(), RailRegistry.TUB_COUPLE.get(), SoundSource.NEUTRAL, 0.8f, 1.0f);
        }
        player.sendOverlayMessage(switch (result) {
            case OK -> Component.translatable(StrataIndustria.MOD_ID + ".tub.coupled");
            case NONE -> Component.translatable(StrataIndustria.MOD_ID + ".tub.none");
            case FOLLOWING -> Component.translatable(StrataIndustria.MOD_ID + ".tub.following");
            case TOO_LONG -> Component.translatable(StrataIndustria.MOD_ID + ".tub.too_long", maxConsist(findFront() == null ? this : findFront().head()));
            case LEADS -> Component.translatable(StrataIndustria.MOD_ID + (this instanceof PonyEntity ? ".pony.leads" : ".locomotive.leads"));
        });
    }

    /** Lets go of the tub ahead. */
    public void decouple() {
        MineTubEntity front = leader();
        if (front != null && getUUID().equals(front.followerId)) {
            front.followerId = null;
            front.refreshCoupled();
        }
        leaderId = null;
        refreshCoupled();
    }

    private void refreshCoupled() {
        entityData.set(DATA_COUPLED, leaderId != null || followerId != null);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide() && reason.shouldDestroy()) {
            MineTubEntity front = leader();
            MineTubEntity back = follower();
            if (front != null) {
                front.followerId = back == null ? null : back.getUUID();
                front.refreshCoupled();
            }
            if (back != null) {
                back.leaderId = front == null ? null : front.getUUID();
                back.refreshCoupled();
            }
        }
        super.remove(reason);
    }

    // ---------------------------------------------------------------- loads

    /** A number that changes whenever what the vehicle carries does, for the stop's "until idle" rule. */
    public int contentSignature() {
        int hash = 1;
        for (int slot = 0; slot < getContainerSize(); slot++) {
            ItemStack stack = getItem(slot);
            hash = hash * 31 + (stack.isEmpty() ? 0 : net.minecraft.core.registries.BuiltInRegistries.ITEM.getId(stack.getItem()) * 64 + stack.getCount());
        }
        return hash;
    }

    /** How full the vehicle is, in units out of {@link #fillCapacity()} (a slot is one unit), for the stop's comparator. */
    public double fillAmount() {
        double total = 0;
        for (int slot = 0; slot < getContainerSize(); slot++) {
            ItemStack stack = getItem(slot);
            if (!stack.isEmpty()) total += (double) stack.getCount() / Math.min(getMaxStackSize(), stack.getMaxStackSize());
        }
        return total;
    }

    public int fillCapacity() {
        return getContainerSize();
    }

    /** True when nothing more fits: every slot is a full stack, or the hopper above offers only what will not go in. */
    public boolean cannotTakeMore(ServerLevel server) {
        boolean full = true;
        for (int slot = 0; slot < getContainerSize(); slot++) {
            ItemStack stack = getItem(slot);
            if (stack.isEmpty() || stack.getCount() < Math.min(getMaxStackSize(), stack.getMaxStackSize())) full = false;
        }
        if (getContainerSize() == 0 || full) return true;
        BlockPos rail = getCurrentBlockPosOrRailBelow();
        if (!(server.getBlockEntity(rail.above()) instanceof HopperBlockEntity hopper)) return false;
        boolean offered = false;
        for (int i = 0; i < hopper.getContainerSize(); i++) {
            ItemStack offer = hopper.getItem(i);
            if (offer.isEmpty()) continue;
            offered = true;
            if (accepts(offer)) return false;
        }
        return offered;
    }

    private boolean accepts(ItemStack offer) {
        for (int slot = 0; slot < getContainerSize(); slot++) {
            ItemStack stack = getItem(slot);
            if (stack.isEmpty()) return true;
            if (ItemStack.isSameItemSameComponents(stack, offer) && stack.getCount() < Math.min(getMaxStackSize(), stack.getMaxStackSize())) return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- saving

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.storeNullable("Leader", UUIDUtil.CODEC, leaderId);
        output.storeNullable("Follower", UUIDUtil.CODEC, followerId);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        leaderId = input.read("Leader", UUIDUtil.CODEC).orElse(null);
        followerId = input.read("Follower", UUIDUtil.CODEC).orElse(null);
        entityData.set(DATA_COUPLED, leaderId != null || followerId != null);
    }

    /** The box a coupling search covers around this tub, for tests. */
    public AABB couplingBox() {
        return getBoundingBox().inflate(COUPLE_REACH);
    }
}
