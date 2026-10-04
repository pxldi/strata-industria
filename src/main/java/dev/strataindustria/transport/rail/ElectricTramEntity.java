package dev.strataindustria.transport.rail;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.power.ElectricNetworks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The electric tram (outposts spec 9.3): a small single-ended car with a trolley pole. The pole finds the wire above
 * the track, springs up against it and the tram draws joules through the nearest bracket: 48 J/t while it accelerates
 * or climbs, 16 J/t at a steady pace, nothing standing. A small reserve carries it over a gap of a few ticks; with no
 * wire it coasts and stops. It is quicker than the steam engine, 0.6 b/t on steel track, carries nine slots of
 * freight and pulls four wagons. The rider has the locomotive's controls (W and S for the throttle, space for the
 * brake, H for the bell) and a tram nobody rides runs its line stop to stop on its own.
 */
public class ElectricTramEntity extends MineTubEntity {
    /** J/t while accelerating or climbing, at a steady pace, and standing (spec 9.3). */
    public static final double ACCEL_DRAW = 48.0, CRUISE_DRAW = 16.0;
    /** Top speed on steel track under wire. */
    public static final double TOP_SPEED = 0.6;
    /** Ticks of full draw the reserve holds. */
    public static final int RESERVE_TICKS = 10;
    public static final double RESERVE = ACCEL_DRAW * RESERVE_TICKS;
    /** Throttle steps: 0, a quarter, a half, three quarters, full. */
    public static final int FULL = 4;
    /** Blocks of running on wire power for the journal goal. */
    public static final double GOAL_BLOCKS = 256.0;
    public static final double ACCEL = 0.009;
    public static final double COAST = 0.005;
    public static final double BRAKE = 0.035;
    /** How far above its feet the roof is, and how long the trolley pole is, in blocks (the model's 72 px). */
    public static final double ROOF = 1.2, POLE_LENGTH = 4.5;
    private static final int BELL_COOLDOWN = 16;
    private static final int NAG_TICKS = 200;
    private static final int HUD_EVERY = 5;
    private static final int APPROACH = 8;
    private static final int HUM_EVERY = 8;
    /** Reserve below which a driverless tram will not pull away from a stop without wire. */
    private static final double SET_OFF = ACCEL_DRAW * 2;
    private static final double LYING = -1.45;

    private static final EntityDataAccessor<Float> DATA_HEADING = SynchedEntityData.defineId(ElectricTramEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_THROTTLE = SynchedEntityData.defineId(ElectricTramEntity.class, EntityDataSerializers.INT);
    /** Height of the wire the pole is on, in sixteenths of a block above the tram's feet; 0 for none. */
    private static final EntityDataAccessor<Integer> DATA_WIRE = SynchedEntityData.defineId(ElectricTramEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_CHARGE = SynchedEntityData.defineId(ElectricTramEntity.class, EntityDataSerializers.INT);
    /** Counters the client watches: each step is one burst of sparks, one ring of the bell. */
    private static final EntityDataAccessor<Integer> DATA_SPARK = SynchedEntityData.defineId(ElectricTramEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_BELL = SynchedEntityData.defineId(ElectricTramEntity.class, EntityDataSerializers.INT);

    /** How the wire looked to the tram this tick, for the readout. */
    public enum Wire { NONE, LIVE, DEAD, TOO_STRONG, TOO_FAR }

    private Vec3 heading = Vec3.ZERO;
    private double charge;
    private int throttle;
    private boolean parked = true;
    private boolean brake;
    private int turnLeft;
    private boolean lastForward, lastBackward;
    private int holdForward;
    private int bellLeft;
    private int nagLeft;
    private int brakeSqueal;
    private int sparkLeft;
    /** The pace the throttle asks for this tick, before the wire has its say, and whether the line climbs. */
    private double intent;
    private boolean climbing;
    private boolean driveOk;
    /** The share of the asked-for draw the motor got this tick; a weak line still pulls, but picks up speed slower. */
    private double drive = 1;
    private TrolleyWires.@Nullable Contact contact;
    private Wire wire = Wire.NONE;
    private double distance;
    private boolean goalDone;
    private final LineTicket ticket = new LineTicket();
    // client
    private float poleAngle = (float) LYING, poleAngleO = (float) LYING, poleVelocity;
    private int bellSeen, sparkSeen, bellTime, bellTimeO;

    public ElectricTramEntity(EntityType<? extends ElectricTramEntity> type, Level level) {
        super(type, level);
    }

    // ---------------------------------------------------------------- item, menu, data

    @Override
    protected Item getDropItem() {
        return TramRegistry.ELECTRIC_TRAM.get();
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(TramRegistry.ELECTRIC_TRAM.get());
    }

    @Override
    public boolean isRideable() {
        return true;
    }

    /** A tram stands on its wheels; a tipple does nothing to it. */
    @Override
    protected boolean tips() {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_HEADING, 0f);
        entityData.define(DATA_THROTTLE, 0);
        entityData.define(DATA_WIRE, 0);
        entityData.define(DATA_CHARGE, 0);
        entityData.define(DATA_SPARK, 0);
        entityData.define(DATA_BELL, 0);
    }

    /** The way the tram faces, as a yaw in degrees (cab end first). */
    public float headingYaw() {
        return entityData.get(DATA_HEADING);
    }

    public int throttle() {
        return entityData.get(DATA_THROTTLE);
    }

    /** Reserve in the tram, 0 to 100. */
    public int chargePercent() {
        return entityData.get(DATA_CHARGE);
    }

    /** True while the pole is up against a wire. */
    public boolean onWire() {
        return entityData.get(DATA_WIRE) > 0;
    }

    private Vec3 front() {
        double yaw = Math.toRadians(headingYaw());
        return new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
    }

    // ---------------------------------------------------------------- the reserve

    public double charge() {
        return charge;
    }

    public void setCharge(double joules) {
        charge = Mth.clamp(joules, 0, RESERVE);
        syncCharge();
    }

    public Wire wire() {
        return wire;
    }

    public boolean parked() {
        return parked;
    }

    public void setParked(boolean parked) {
        this.parked = parked;
    }

    public double distance() {
        return distance;
    }

    public Vec3 heading() {
        return heading;
    }

    public boolean ticketed() {
        return ticket.open();
    }

    private void syncCharge() {
        int percent = (int) Math.round(100.0 * charge / RESERVE);
        if (entityData.get(DATA_CHARGE) != percent) entityData.set(DATA_CHARGE, percent);
    }

    private void setThrottle(int step) {
        throttle = Mth.clamp(step, 0, FULL);
        entityData.set(DATA_THROTTLE, throttle);
    }

    // ---------------------------------------------------------------- speed

    /** On steel track under wire it runs to its own top speed, a little over what the track alone allows. */
    @Override
    protected double getMaxSpeed(ServerLevel level) {
        return grade() == RailGrade.STEEL ? TOP_SPEED : super.getMaxSpeed(level);
    }

    /** How fast the rider or the line wants it to go this tick, before the wire and the rail have their say. */
    private double wantedSpeed(BlockPos rail) {
        intent = 0;
        if (isHeld() || atSignal() || turnLeft > 0) return 0;
        boolean ridden = riderPlayer() != null;
        int step = ridden ? throttle : parked || !driverlessReady() ? 0 : FULL;
        if (step <= 0) return 0;
        double cap = level() instanceof ServerLevel server ? getMaxSpeed(server) : 0.4;
        double wanted = cap * step / FULL;
        if (!ridden) wanted = Math.min(wanted, approach(rail, wanted));
        intent = wanted;
        return driveOk ? wanted : 0;
    }

    /** A driverless tram runs only with wire over it or a reserve to go on. */
    private boolean driverlessReady() {
        return wire == Wire.LIVE || charge >= SET_OFF;
    }

    /** Eases the pace down as the next stop on a straight run comes up, so it glides in. */
    private double approach(BlockPos rail, double wanted) {
        if (heading.lengthSqr() < 1.0E-6) return wanted;
        int dx = (int) Math.signum(Math.round(heading.x * 10) / 10.0), dz = (int) Math.signum(Math.round(heading.z * 10) / 10.0);
        if (dx != 0 && dz != 0) return wanted;
        for (int k = 1; k <= APPROACH; k++) {
            BlockPos ahead = rail.offset(dx * k, 0, dz * k);
            if (!level().hasChunkAt(ahead)) return wanted;
            for (BlockPos probe : new BlockPos[] {ahead, ahead.above(), ahead.below()}) {
                if (level().getBlockState(probe).getBlock() instanceof TubStopBlock && !probe.equals(ignoredStop()) && wantsStop(probe)) {
                    return Math.min(wanted, 0.06 + 0.09 * (k - 1));
                }
            }
        }
        return wanted;
    }

    /** The motor moves it: it takes the speed it wants a step at a time, and runs down gently when the power goes. */
    @Override
    protected Vec3 applyNaturalSlowdown(Vec3 movement) {
        if (winched() || level().isClientSide()) return super.applyNaturalSlowdown(movement);
        if (isHeld()) return movement;
        BlockPos rail = railBlock();
        if (rail == null) return super.applyNaturalSlowdown(movement);
        Vec3 flat = new Vec3(movement.x, 0, movement.z);
        double speed = flat.length();
        if (speed > 0.02) {
            heading = flat.normalize();
            syncHeading();
        } else if (heading.lengthSqr() < 1.0E-6) {
            heading = railAxis(rail);
            syncHeading();
        }
        if (!level().hasChunkAt(rail.relative(Direction.getApproximateNearest(heading.x, 0, heading.z)))) return Vec3.ZERO;
        double wanted = wantedSpeed(rail);
        climbing = uphill(rail, heading);
        if (wanted > 0 && nearEdge(rail, heading) && (deadEnd(rail, heading) || atBuffer())) {
            turnLeft = 30;
            wanted = 0;
            intent = 0;
        }
        double next;
        if (brake) {
            next = Math.max(0, speed - BRAKE);
        } else if (speed < wanted) {
            next = Math.min(wanted, Math.max(speed, 0.02) + ACCEL * drive);
        } else if (intent > 0 || riderPlayer() != null && throttle > 0) {
            // Power gone with the throttle open: coast.
            next = Math.max(wanted, speed - COAST);
        } else {
            next = Math.max(0, speed - (wire == Wire.NONE && charge <= 0 ? COAST : BRAKE * 0.5));
        }
        return heading.scale(next);
    }

    private void syncHeading() {
        if (heading.lengthSqr() > 1.0E-6) entityData.set(DATA_HEADING, (float) Math.toDegrees(Math.atan2(-heading.x, heading.z)));
    }

    // ---------------------------------------------------------------- ticking

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel server) serverTick(server);
        else clientTick();
    }

    private void serverTick(ServerLevel server) {
        if (bellLeft > 0) bellLeft--;
        if (nagLeft > 0) nagLeft--;
        if (brakeSqueal > 0) brakeSqueal--;
        if (sparkLeft > 0) sparkLeft--;
        if (turnLeft > 0 && --turnLeft == 0) turnAround();
        ServerPlayer rider = riderPlayer();
        if (rider != null) {
            driving(server, rider);
        } else {
            lastForward = lastBackward = false;
            brake = false;
            if (throttle != 0) setThrottle(0);
        }
        double speed = getDeltaMovement().horizontalDistance();
        pickup(server, speed);
        motor(server, speed);
        ticket.tick(server, this);
        if (rider != null && tickCount % HUD_EVERY == 0) rider.sendOverlayMessage(hud());
    }

    /** The pole finds its wire, the bracket hands over what the network gave, and the motor takes its draw. */
    private void pickup(ServerLevel server, double speed) {
        TrolleyWires.Contact found = TrolleyWires.find(server, position());
        boolean had = contact != null;
        contact = found;
        if (found != null && !had) {
            server.playSound(null, getX(), getY() + found.height(), getZ(), TramRegistry.CONTACT.get(), SoundSource.NEUTRAL, 0.8f, 1.0f);
            spark(server, false);
        } else if (found == null && had) {
            server.playSound(null, getX(), getY() + 2.5, getZ(), TramRegistry.LOSE_WIRE.get(), SoundSource.NEUTRAL, 0.7f, 1.0f);
            if (charge < ACCEL_DRAW) tell(server, "tram.lost_wire");
        }
        int height = found == null ? 0 : Math.max(1, (int) Math.round(found.height() * 16));
        if (entityData.get(DATA_WIRE) != height) entityData.set(DATA_WIRE, height);

        wire = Wire.NONE;
        if (found != null && server.getBlockEntity(found.bracket()) instanceof TrolleyBracketBlockEntity bracket && !bracket.isRemoved()) {
            charge = Math.min(RESERVE, charge + bracket.takeDelivered());
            var report = ElectricNetworks.report(server, found.bracket());
            wire = switch (report.status()) {
                case OVERVOLTAGE -> Wire.TOO_STRONG;
                case TOO_FAR -> Wire.TOO_FAR;
                case NO_SOURCE, CABLE_OVERVOLTAGE, TOO_LARGE -> Wire.DEAD;
                default -> charge > 0 || report.drawn() > 0 || report.fraction() > 0 ? Wire.LIVE : Wire.DEAD;
            };
            double want = Math.min(TrolleyBracketBlockEntity.PULL_LIMIT, RESERVE - charge);
            if (want > 0.5 && wire != Wire.TOO_STRONG) bracket.pull(want);
            nightSparks(server, found, speed);
        }

        // The draw: full while it speeds up or climbs, a third of it at a steady pace, none standing or coasting.
        double use = 0;
        if (intent > 0) use = speed < intent - 0.015 || climbing && speed > 0.02 ? ACCEL_DRAW : CRUISE_DRAW;
        if (use <= 0) {
            driveOk = charge > 0 || wire == Wire.LIVE;
            drive = 1;
        } else if (charge >= use) {
            charge -= use;
            driveOk = true;
            drive = 1;
        } else {
            drive = charge / use;
            charge = 0;
            driveOk = drive > 0.01;
        }
        if (driveOk && found != null && speed > 0.02 && wire == Wire.LIVE) {
            distance += speed;
            if (!goalDone && distance >= GOAL_BLOCKS) {
                goalDone = true;
                Journal.awardNear(server, blockPosition(), Journal.TRAM_DISTANCE);
                if (riderPlayer() != null) Journal.award(riderPlayer(), Journal.TRAM_DISTANCE);
            }
        }
        if (tickCount % 4 == 0) syncCharge();
    }

    /** At night the shoe crackles where it crosses a bracket: a blue-white flash and a snap (spec 9.3). */
    private void nightSparks(ServerLevel server, TrolleyWires.Contact found, double speed) {
        if (speed < 0.08 || sparkLeft > 0 || !server.getBlockState(found.bracket()).hasProperty(TrolleyBracketBlock.FACING)) return;
        long time = server.getOverworldClockTime() % 24000L;
        if (time < 13000L || time > 23000L) return;
        Vec3 tip = TrolleyBracketBlockEntity.tip(found.bracket(), server.getBlockState(found.bracket()));
        if (Math.hypot(getX() - tip.x, getZ() - tip.z) > 1.1) return;
        sparkLeft = 12;
        spark(server, true);
    }

    private void spark(ServerLevel server, boolean snap) {
        entityData.set(DATA_SPARK, entityData.get(DATA_SPARK) + 1);
        if (snap) server.playSound(null, getX(), getY() + 3.0, getZ(), TramRegistry.SPARK.get(), SoundSource.NEUTRAL, 0.6f, 0.9f + random.nextFloat() * 0.3f);
    }

    /** The motor's hum, rising with speed, and a thrum when it pulls hard. */
    private void motor(ServerLevel server, double speed) {
        if (isHeld() || tickCount % HUM_EVERY != 0) return;
        boolean pulling = intent > 0 && driveOk;
        if (speed < 0.03 && !pulling) return;
        float pitch = 0.65f + (float) speed * 1.6f + (speed < intent - 0.015 ? 0.15f : 0f);
        server.playSound(null, getX(), getY(), getZ(), TramRegistry.MOTOR.get(), SoundSource.NEUTRAL, 0.3f + (float) speed * 0.5f, pitch);
    }

    private @Nullable ServerPlayer riderPlayer() {
        return getFirstPassenger() instanceof ServerPlayer player ? player : null;
    }

    // ---------------------------------------------------------------- driving

    /** W a step up, S a step down, and at a standstill S turns the tram; space is the brake. */
    private void driving(ServerLevel server, ServerPlayer rider) {
        var input = rider.getLastClientInput();
        boolean forward = input.forward(), backward = input.backward();
        boolean stepUp = forward && (!lastForward || ++holdForward % 8 == 0);
        if (!forward) holdForward = 0;
        boolean stepDown = backward && !lastBackward;
        lastForward = forward;
        lastBackward = backward;
        brake = input.jump();
        double speed = getDeltaMovement().horizontalDistance();
        if (stepUp) {
            if (throttle < FULL) {
                setThrottle(throttle + 1);
                server.playSound(null, getX(), getY(), getZ(), SoundEvents.LEVER_CLICK, SoundSource.NEUTRAL, 0.6f, 0.8f + 0.1f * throttle);
            }
            if (isHeld() && throttle > 0) {
                BlockPos stop = heldAt();
                if (stop != null && server.getBlockEntity(stop) instanceof TubStopBlockEntity block) block.pulse();
            }
            if (throttle == 1 && speed < 0.03) {
                // The contactor closes as it sets off: a clack and a thrum from the motor.
                server.playSound(null, getX(), getY(), getZ(), TramRegistry.MOTOR.get(), SoundSource.NEUTRAL, 0.6f, 0.6f);
                if (onWire()) spark(server, false);
            }
        }
        if (stepDown) {
            if (throttle > 0) {
                setThrottle(throttle - 1);
                server.playSound(null, getX(), getY(), getZ(), SoundEvents.LEVER_CLICK, SoundSource.NEUTRAL, 0.6f, 0.7f);
            } else if (speed < 0.03 && turnLeft <= 0) {
                turnAround();
                ring(server, 0.9f);
            }
        }
        if (brake) {
            if (speed > 0.15 && brakeSqueal <= 0) {
                brakeSqueal = 25;
                server.playSound(null, getX(), getY(), getZ(), RailwayRegistry.LOCOMOTIVE_BRAKE.get(), SoundSource.NEUTRAL, 0.7f, 1.0f + (float) speed * 0.4f);
                server.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 0.15, getZ(), 6, 0.3, 0.02, 0.3, 0.1);
            }
            if (throttle > 0) setThrottle(0);
        }
    }

    /** Rings the bell, a double stroke, unless it has only just rung. */
    public boolean ring(ServerLevel server, float pitch) {
        if (bellLeft > 0) return false;
        bellLeft = BELL_COOLDOWN;
        entityData.set(DATA_BELL, entityData.get(DATA_BELL) + 1);
        server.playSound(null, getX(), getY(), getZ(), TramRegistry.BELL.get(), SoundSource.NEUTRAL, 1.0f, pitch);
        return true;
    }

    /** The bell key, from a rider. */
    public void bellBy(ServerLevel server, ServerPlayer player) {
        if (getFirstPassenger() == player) ring(server, 1.0f);
    }

    /** The tram reverses: the cab end swaps with the other and the consist is brought round behind it. */
    private void turnAround() {
        if (level() instanceof ServerLevel && !reverseConsist(heading)) {
            turnLeft = 30;
            return;
        }
        heading = heading.scale(-1);
        syncHeading();
        turnLeft = 0;
        setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public Vec3 intent() {
        return heading;
    }

    private Component hud() {
        String[] steps = {"0", "1/4", "1/2", "3/4", "full"};
        return Component.translatable(StrataIndustria.MOD_ID + ".tram.hud", steps[throttle], Math.round(100.0 * charge / RESERVE),
                Component.translatable(StrataIndustria.MOD_ID + ".tram.wire." + wire.name().toLowerCase(java.util.Locale.ROOT)));
    }

    private void tell(ServerLevel server, String key) {
        if (nagLeft > 0) return;
        nagLeft = NAG_TICKS;
        Component message = Component.translatable(StrataIndustria.MOD_ID + "." + key);
        for (ServerPlayer player : server.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(24))) player.sendOverlayMessage(message);
    }

    // ---------------------------------------------------------------- stops, tickets and trips

    /** The stop lets it go once it has wire or a reserve for the run and, in a charter's area, a moving ticket. */
    @Override
    public boolean canDepart(ServerLevel server) {
        if (riderPlayer() == null && parked) return false;
        if (!driverlessReady()) {
            tell(server, wire == Wire.TOO_STRONG ? "tram.too_strong" : "tram.no_power");
            return false;
        }
        if (!ticket.admit(server, this, heldAt())) {
            tell(server, "pony.no_line");
            return false;
        }
        return true;
    }

    @Override
    public void holdAt(BlockPos stop) {
        double speed = getDeltaMovement().horizontalDistance();
        super.holdAt(stop);
        if (level() instanceof ServerLevel server) {
            if (speed > 0.15) {
                server.playSound(null, getX(), getY(), getZ(), RailwayRegistry.LOCOMOTIVE_BRAKE.get(), SoundSource.NEUTRAL, 0.6f, 1.1f);
                server.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 0.15, getZ(), 6, 0.3, 0.02, 0.3, 0.1);
            }
            // The driver's ding as it comes to rest at the stop.
            ring(server, 1.1f);
        }
        setThrottle(0);
    }

    @Override
    public void release(BlockPos stop, Vec3 push) {
        // Leaving back the way it came, the tram first brings its wagons round.
        Vec3 turn = new Vec3(push.x, 0, push.z);
        if (heading.lengthSqr() > 1.0E-6 && turn.dot(heading) < 0 && !reverseConsist(heading)) push = push.scale(-1);
        super.release(stop, push);
        Vec3 flat = new Vec3(push.x, 0, push.z);
        if (flat.lengthSqr() > 1.0E-8) {
            heading = flat.normalize();
            syncHeading();
        }
        ticket.leaving();
        // Two strokes of the bell as it pulls away.
        if (level() instanceof ServerLevel server && getDeltaMovement().horizontalDistance() > 0) ring(server, 0.95f);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (level() instanceof ServerLevel server) ticket.release(server, this);
        super.remove(reason);
    }

    /** A tram leads; it is never coupled behind anything. */
    @Override
    public Coupling couple() {
        return Coupling.LEADS;
    }

    // ---------------------------------------------------------------- using

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(dev.strataindustria.transport.signal.SignalRegistry.TIMETABLE.get())) return loadTimetable(player, held);
        if (held.is(Items.IRON_CHAIN) || held.is(dev.strataindustria.transport.foot.FootRegistry.ROPE.get())) {
            if (player instanceof ServerPlayer serverPlayer) tryCouple(serverPlayer);
            return InteractionResult.SUCCESS;
        }
        if (player.isSecondaryUseActive()) {
            if (held.isEmpty()) {
                if (!level().isClientSide()) {
                    if (isCoupled()) {
                        if (follower() != null) follower().decouple();
                        level().playSound(null, getX(), getY(), getZ(), RailRegistry.TUB_COUPLE.get(), SoundSource.NEUTRAL, 0.6f, 0.7f);
                    } else {
                        parked = !parked;
                        level().playSound(null, getX(), getY(), getZ(), parked ? RailRegistry.TUB_STOP_BRAKE.get() : SoundEvents.LEVER_CLICK, SoundSource.NEUTRAL, 0.8f, parked ? 0.8f : 1.0f);
                        if (player instanceof ServerPlayer serverPlayer) {
                            serverPlayer.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + (parked ? ".tram.parked" : ".tram.released")));
                        }
                    }
                }
                return InteractionResult.SUCCESS;
            }
            // Sneaking with something in hand opens the freight.
            return openLoad(player, hand, location);
        }
        if (!isVehicle()) {
            if (level() instanceof ServerLevel && player.startRiding(this)) {
                if (player instanceof ServerPlayer serverPlayer) serverPlayer.sendOverlayMessage(hud());
                return InteractionResult.SUCCESS_SERVER;
            }
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        // The driver stands at the cab end, whichever way the tram runs.
        Vec3 front = front();
        return new Vec3(front.x * 0.3, 0.1, front.z * 0.3);
    }

    // ---------------------------------------------------------------- the client's show

    private void clientTick() {
        poleAngleO = poleAngle;
        bellTimeO = bellTime;
        if (bellTime > 0) bellTime--;
        // The pole is a spring: it flies up to the wire, overshoots a touch and settles.
        double target = targetAngle();
        poleVelocity += (float) ((target - poleAngle) * 0.2);
        poleVelocity *= 0.68f;
        poleAngle += poleVelocity;
        Vec3 front = front();
        double speed = Math.hypot(getX() - xo, getZ() - zo);
        int bell = entityData.get(DATA_BELL), sparks = entityData.get(DATA_SPARK);
        double shoeY = getY() + Math.max(entityData.get(DATA_WIRE) / 16.0, ROOF + 0.4);
        if (sparks != sparkSeen) {
            sparkSeen = sparks;
            for (int i = 0; i < 8; i++) {
                level().addParticle(ParticleTypes.ELECTRIC_SPARK, getX() - front.x * 0.9, shoeY, getZ() - front.z * 0.9,
                        random.nextGaussian() * 0.12, 0.05 + random.nextDouble() * 0.1, random.nextGaussian() * 0.12);
            }
        } else if (onWire() && throttle() > 0 && speed < 0.35 && random.nextInt(7) == 0) {
            // Pulling hard under the wire: the odd spit of a spark from the shoe.
            level().addParticle(ParticleTypes.ELECTRIC_SPARK, getX() - front.x * 0.9, shoeY, getZ() - front.z * 0.9,
                    random.nextGaussian() * 0.04, 0.03, random.nextGaussian() * 0.04);
        }
        if (bell != bellSeen) {
            bellSeen = bell;
            bellTime = 30;
            level().addParticle(ParticleTypes.NOTE, getX() + front.x * 0.5, getY() + 1.5, getZ() + front.z * 0.5, (random.nextInt(24)) / 24.0, 0, 0);
        }
    }

    private float targetAngle() {
        int wire = entityData.get(DATA_WIRE);
        if (wire <= 0) return (float) LYING;
        double above = wire / 16.0 - ROOF;
        return (float) -Math.acos(Mth.clamp(above / POLE_LENGTH, 0.12, 1.0));
    }

    /** Where the trolley pole leans, in radians about the model's z axis: back and up, more upright the lower the wire. */
    public float poleAngle(float partialTicks) {
        return Mth.lerp(partialTicks, poleAngleO, poleAngle);
    }

    /** How far the bell is swinging, in radians. */
    public float bellSwing(float partialTicks) {
        float time = Mth.lerp(partialTicks, bellTimeO, bellTime);
        return (float) (Math.sin(time * 1.1) * time / 30.0 * 0.8);
    }

    // ---------------------------------------------------------------- saving

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putDouble("Charge", charge);
        output.putBoolean("Parked", parked);
        output.putDouble("Distance", distance);
        output.putBoolean("GoalDone", goalDone);
        output.putDouble("HeadX", heading.x);
        output.putDouble("HeadZ", heading.z);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        charge = input.getDoubleOr("Charge", 0);
        parked = input.getBooleanOr("Parked", true);
        distance = input.getDoubleOr("Distance", 0);
        goalDone = input.getBooleanOr("GoalDone", false);
        heading = new Vec3(input.getDoubleOr("HeadX", 0), 0, input.getDoubleOr("HeadZ", 0));
        syncHeading();
        syncCharge();
    }
}
