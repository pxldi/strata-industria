package dev.strataindustria.transport.rail;

import dev.strataindustria.Config;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.fluid.FluidBuckets;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.journal.Observations;
import dev.strataindustria.listening.ListeningSounds;
import dev.strataindustria.registry.Tier4Sounds;
import dev.strataindustria.steam.FireboxBlockEntity;
import dev.strataindustria.transport.outpost.Charter;
import dev.strataindustria.transport.outpost.RouteIndex;
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
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The steam locomotive (outposts spec 7.3): a small 0-4-0 tank engine. One fuel slot, 8 000 mB of water. A lit
 * firebox raises steam over 200 ticks; the throttle (W and S, in five steps) and the brake (space) are the rider's,
 * and a locomotive nobody rides runs its line by itself, stop to stop, as the pony does. It uses water with the
 * throttle, stops at the next stop when it is low on water or fuel, and when it runs dry it only stands and says so.
 */
public class SteamLocomotiveEntity extends MineTubEntity {
    public static final int FUEL_SLOT = 0;
    public static final int WATER_CAPACITY = 8_000;
    public static final int BUCKET = 1000;
    /** Ticks a cold boiler takes to raise full steam. */
    public static final int BUILD_TICKS = 200;
    /** Water and fuel left below which a driverless engine waits at its stop for more. */
    public static final int LOW_WATER = 1000;
    public static final int LOW_BURN = 200;
    /** Steam needed before it will pull away from a stop. */
    public static final float PULL_AWAY = 0.9f;
    /** Throttle steps: 0, a quarter, a half, three quarters, full. */
    public static final int FULL = 4;
    /** Top speed with up to four wagons and with up to eight, on steel (spec 7.3); the rail's own limit still applies. */
    public static final double LIGHT_CAP = 0.5;
    public static final double HEAVY_CAP = 0.4;
    public static final int LIGHT_WAGONS = 4;
    public static final double ACCEL = 0.006;
    public static final double COAST = 0.004;
    public static final double BRAKE = 0.03;
    /** Blocks a wheel set turns for one chuff. */
    private static final double CHUFF_EVERY = 2.0;
    private static final int WHISTLE_COOLDOWN = 24;
    private static final int NAG_TICKS = 200;
    private static final int HUD_EVERY = 5;
    /** How far ahead along the line a driverless engine looks for its stop to ease off for it. */
    private static final int APPROACH = 8;

    private static final EntityDataAccessor<Float> DATA_HEADING = SynchedEntityData.defineId(SteamLocomotiveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_THROTTLE = SynchedEntityData.defineId(SteamLocomotiveEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_PRESSURE = SynchedEntityData.defineId(SteamLocomotiveEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_WATER = SynchedEntityData.defineId(SteamLocomotiveEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_FIRE = SynchedEntityData.defineId(SteamLocomotiveEntity.class, EntityDataSerializers.BOOLEAN);
    /** Counters the client watches: each step is one chuff, one vent of steam, one pour of water into the tank. */
    private static final EntityDataAccessor<Integer> DATA_CHUFF = SynchedEntityData.defineId(SteamLocomotiveEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_VENT = SynchedEntityData.defineId(SteamLocomotiveEntity.class, EntityDataSerializers.INT);

    private Vec3 heading = Vec3.ZERO;
    private float water;
    private float pressure;
    private int burnLeft;
    private int throttle;
    private boolean parked = true;
    private boolean brake;
    private boolean dry;
    private double chuffClock;
    private int whistleLeft;
    private int nagLeft;
    private int brakeSqueal;
    private int turnLeft;
    private boolean lastForward, lastBackward;
    private int holdForward;
    private int legs;
    private boolean legRidden;
    private @Nullable BlockPos departedFrom;
    private final LineTicket ticket = new LineTicket();
    // client
    private int chuffSeen, ventSeen;
    private double wheelDistance, wheelSpeed;

    public SteamLocomotiveEntity(EntityType<? extends SteamLocomotiveEntity> type, Level level) {
        super(type, level);
    }

    // ---------------------------------------------------------------- item, menu, data

    @Override
    protected Item getDropItem() {
        return RailwayRegistry.STEAM_LOCOMOTIVE.get();
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(RailwayRegistry.STEAM_LOCOMOTIVE.get());
    }

    /** One slot, for fuel. */
    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new DispenserMenu(containerId, inventory, new net.minecraft.world.SimpleContainer(9));
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return FireboxBlockEntity.fuelFor(stack) != null;
    }

    @Override
    public boolean isRideable() {
        return true;
    }

    @Override
    protected boolean tips() {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_HEADING, 0f);
        entityData.define(DATA_THROTTLE, 0);
        entityData.define(DATA_PRESSURE, 0);
        entityData.define(DATA_WATER, 0);
        entityData.define(DATA_FIRE, false);
        entityData.define(DATA_CHUFF, 0);
        entityData.define(DATA_VENT, 0);
    }

    /** The way the engine faces, as a yaw in degrees (chimney end first). */
    public float headingYaw() {
        return entityData.get(DATA_HEADING);
    }

    /** Throttle step 0 to 4, for the renderer and the readout. */
    public int throttle() {
        return entityData.get(DATA_THROTTLE);
    }

    /** Steam, 0 to 100. */
    public int pressurePercent() {
        return entityData.get(DATA_PRESSURE);
    }

    /** The water tank, 0 to 16 pixels, for the sight glass. */
    public int waterGauge() {
        return entityData.get(DATA_WATER);
    }

    public boolean fireLit() {
        return entityData.get(DATA_FIRE);
    }

    // ---------------------------------------------------------------- the boiler

    public float water() {
        return water;
    }

    public void setWater(float mb) {
        water = Mth.clamp(mb, 0, WATER_CAPACITY);
        syncBoiler();
    }

    /** Takes up to {@code mb} of water; returns how much went in. */
    public int takeWater(int mb) {
        int room = (int) Math.floor(WATER_CAPACITY - water);
        int taken = Math.max(0, Math.min(mb, room));
        if (taken > 0) {
            water += taken;
            dry = false;
            syncBoiler();
        }
        return taken;
    }

    public float pressure() {
        return pressure;
    }

    public void setPressure(float level) {
        pressure = Mth.clamp(level, 0, 1);
        syncBoiler();
    }

    public int burnLeft() {
        return burnLeft;
    }

    public boolean burning() {
        return burnLeft > 0;
    }

    public ItemStack fuel() {
        return getItem(FUEL_SLOT);
    }

    /** Puts what it can of {@code stack} into the fuel slot; returns how many went in. */
    public int takeFuel(ItemStack stack) {
        if (FireboxBlockEntity.fuelFor(stack) == null) return 0;
        ItemStack slot = getItem(FUEL_SLOT);
        if (slot.isEmpty()) {
            int count = Math.min(stack.getCount(), stack.getMaxStackSize());
            setItem(FUEL_SLOT, stack.copyWithCount(count));
            return count;
        }
        if (!ItemStack.isSameItemSameComponents(slot, stack)) return 0;
        int count = Math.min(stack.getCount(), slot.getMaxStackSize() - slot.getCount());
        if (count > 0) slot.grow(count);
        return Math.max(count, 0);
    }

    /** Whether a lit firebox or fuel in the slot can keep the boiler going for a while. */
    public boolean hasFire() {
        return burnLeft >= LOW_BURN || !fuel().isEmpty();
    }

    public boolean parked() {
        return parked;
    }

    public void setParked(boolean parked) {
        this.parked = parked;
    }

    public int legs() {
        return legs;
    }

    private void syncBoiler() {
        entityData.set(DATA_PRESSURE, Math.round(pressure * 100));
        entityData.set(DATA_WATER, (int) Math.ceil(16.0 * water / WATER_CAPACITY));
    }

    private void setThrottle(int step) {
        throttle = Mth.clamp(step, 0, FULL);
        entityData.set(DATA_THROTTLE, throttle);
    }

    /** What the steam allows: nothing below a fifth, all of it from four fifths up. */
    private double steamFactor() {
        return Mth.clamp((pressure - 0.2) / 0.6, 0, 1);
    }

    // ---------------------------------------------------------------- speed

    /** The rail's limit, and a locomotive's own limit for its load. */
    @Override
    protected double getMaxSpeed(ServerLevel level) {
        return Math.min(super.getMaxSpeed(level), consistCap());
    }

    private double consistCap() {
        return consist().size() - 1 > LIGHT_WAGONS ? HEAVY_CAP : LIGHT_CAP;
    }

    /** How fast it wants to go this tick, before the steam and the rail have their say. */
    private double wantedSpeed(BlockPos rail) {
        if (isHeld() || turnLeft > 0) return 0;
        boolean ridden = riderPlayer() != null;
        int step = ridden ? throttle : parked || !driverlessReady() ? 0 : FULL;
        if (step <= 0) return 0;
        double cap = Math.min(grade() == null ? 0.4 : grade().cap(), consistCap());
        double wanted = cap * step / FULL * steamFactor();
        if (!ridden) wanted = Math.min(wanted, approach(rail, wanted));
        return wanted;
    }

    /** A driverless engine runs only with water and a fire. */
    private boolean driverlessReady() {
        return water > 0 && (burnLeft > 0 || !fuel().isEmpty() || pressure > 0.2);
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
                if (level().getBlockState(probe).getBlock() instanceof TubStopBlock && !probe.equals(ignoredStop())) {
                    return Math.min(wanted, 0.05 + 0.07 * (k - 1));
                }
            }
        }
        return wanted;
    }

    /** The engine moves itself: it takes the speed it wants a step at a time and never loses it to friction. */
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
        if (wanted > 0 && nearEdge(rail, heading) && (deadEnd(rail, heading) || atBuffer())) {
            turnLeft = 30;
            wanted = 0;
        }
        double next;
        if (brake) {
            next = Math.max(0, speed - BRAKE);
        } else if (speed < wanted) {
            next = Math.min(wanted, Math.max(speed, 0.02) + ACCEL);
        } else {
            next = Math.max(wanted, speed - (wanted <= 0 ? BRAKE * 0.6 : COAST));
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
        if (whistleLeft > 0) whistleLeft--;
        if (nagLeft > 0) nagLeft--;
        if (brakeSqueal > 0) brakeSqueal--;
        if (turnLeft > 0 && --turnLeft == 0) turnAround();
        ServerPlayer rider = riderPlayer();
        if (rider != null) {
            driving(server, rider);
            legRidden = true;
        } else {
            lastForward = lastBackward = false;
            brake = false;
            if (throttle != 0) setThrottle(0);
        }
        double speed = getDeltaMovement().horizontalDistance();
        boiler(server, speed);
        wheels(server, speed);
        ticket.tick(server, this);
        if (rider != null && tickCount % HUD_EVERY == 0) rider.sendOverlayMessage(hud());
    }

    /** The fire, the steam and the water. */
    private void boiler(ServerLevel server, double speed) {
        boolean moving = speed > 0.02;
        boolean demand = pressure < 1.0f || moving || throttle > 0;
        if (burnLeft <= 0 && demand && water > 0 && !fuel().isEmpty()) {
            FireboxBlockEntity.Fuel fuel = FireboxBlockEntity.fuelFor(fuel());
            if (fuel != null) {
                burnLeft = fuel.burnTicks();
                fuel().shrink(1);
                if (fuel().isEmpty()) setItem(FUEL_SLOT, ItemStack.EMPTY);
                server.playSound(null, getX(), getY(), getZ(), Tier4Sounds.FIREBOX_LIGHT.get(), SoundSource.NEUTRAL, 0.7f, 1.0f);
                server.sendParticles(ParticleTypes.FLAME, getX() - heading.x * 0.5, getY() + 0.5, getZ() - heading.z * 0.5, 4, 0.1, 0.1, 0.1, 0.02);
            }
        }
        boolean lit = burnLeft > 0;
        if (lit) burnLeft--;
        if (entityData.get(DATA_FIRE) != lit) entityData.set(DATA_FIRE, lit);
        float before = pressure;
        if (lit && water > 0) pressure = Math.min(1.0f, pressure + 1.0f / BUILD_TICKS);
        else pressure = Math.max(0, pressure - 1.0f / (BUILD_TICKS * 2));
        // Water boils away with the throttle, and a little while the boiler is coming up.
        double perTick = Config.TRANSPORT_LOCOMOTIVE_WATER_PER_TICK.getAsDouble();
        double use = moving ? perTick * throttleShare() : lit && pressure < 1.0f ? perTick * 0.1 : 0;
        if (use > 0 && water > 0) water = Math.max(0, water - (float) use);
        if (water <= 0 && !dry && (lit || pressure > 0)) runDry(server);
        if (before < 1.0f && pressure >= 1.0f) steamUp(server);
        if (tickCount % 4 == 0) syncBoiler();
    }

    private double throttleShare() {
        return riderPlayer() != null ? throttle / (double) FULL : parked ? 0 : 1.0;
    }

    /** The boiler is dry: the fire dies down and the engine only stands. */
    private void runDry(ServerLevel server) {
        dry = true;
        burnLeft = 0;
        pressure = Math.min(pressure, 0.2f);
        server.playSound(null, getX(), getY(), getZ(), Tier4Sounds.BOILER_DRY_FIRE.get(), SoundSource.NEUTRAL, 0.8f, 0.9f);
        server.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.9, getZ(), 8, 0.2, 0.1, 0.2, 0.02);
        tell(server, "locomotive.dry");
        Observations.boilerDry(server, blockPosition());
    }

    /** Full steam for the first time since it was cold: a toot and a burst from the dome, and the goal for those about. */
    private void steamUp(ServerLevel server) {
        whistleLeft = WHISTLE_COOLDOWN;
        server.playSound(null, getX(), getY(), getZ(), RailwayRegistry.LOCOMOTIVE_WHISTLE.get(), SoundSource.NEUTRAL, 0.8f, 1.2f);
        vent();
        tell(server, "locomotive.steam_up");
        Journal.awardNear(server, blockPosition(), Journal.LOCOMOTIVE_PRESSURE);
    }

    private void vent() {
        entityData.set(DATA_VENT, entityData.get(DATA_VENT) + 1);
    }

    /** Chuffs and the rattle of the wheels, in step with how fast it runs. */
    private void wheels(ServerLevel server, double speed) {
        if (speed < 0.03 || isHeld()) {
            chuffClock = 0;
            return;
        }
        chuffClock += speed;
        if (chuffClock < CHUFF_EVERY) return;
        chuffClock -= CHUFF_EVERY;
        entityData.set(DATA_CHUFF, entityData.get(DATA_CHUFF) + 1);
        server.playSound(null, getX(), getY(), getZ(), RailwayRegistry.LOCOMOTIVE_CHUFF.get(), SoundSource.NEUTRAL,
                0.6f + (float) speed * 0.6f, 0.75f + (float) speed * 0.9f);
    }

    private @Nullable ServerPlayer riderPlayer() {
        return getFirstPassenger() instanceof ServerPlayer player ? player : null;
    }

    // ---------------------------------------------------------------- driving

    /** W a step up, S a step down, and at a standstill S turns the engine round; space is the brake. */
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
                // Cylinder cocks open as it sets off.
                vent();
                server.playSound(null, getX(), getY(), getZ(), ListeningSounds.BOILER_HISS.get(), SoundSource.NEUTRAL, 0.5f, 1.2f);
            }
        }
        if (stepDown) {
            if (throttle > 0) {
                setThrottle(throttle - 1);
                server.playSound(null, getX(), getY(), getZ(), SoundEvents.LEVER_CLICK, SoundSource.NEUTRAL, 0.6f, 0.7f);
            } else if (speed < 0.03 && !reversing()) {
                turnAround();
                whistle(server, 0.9f);
            }
        }
        if (brake) {
            if (speed > 0.15 && brakeSqueal <= 0) {
                brakeSqueal = 25;
                server.playSound(null, getX(), getY(), getZ(), RailwayRegistry.LOCOMOTIVE_BRAKE.get(), SoundSource.NEUTRAL, 0.7f, 0.9f + (float) speed * 0.4f);
                server.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 0.15, getZ(), 6, 0.3, 0.02, 0.3, 0.1);
            }
            if (throttle > 0) setThrottle(0);
        }
    }

    private boolean reversing() {
        return turnLeft > 0;
    }

    /** Blows the whistle: a toot as long as the steam allows, with a puff from the dome. */
    public boolean whistle(ServerLevel server, float pitch) {
        if (whistleLeft > 0 || pressure < 0.15f) return false;
        whistleLeft = WHISTLE_COOLDOWN;
        server.playSound(null, getX(), getY(), getZ(), RailwayRegistry.LOCOMOTIVE_WHISTLE.get(), SoundSource.NEUTRAL, 1.0f, pitch);
        vent();
        return true;
    }

    /** The whistle key, from a rider. */
    public void whistleBy(ServerLevel server, ServerPlayer player) {
        if (getFirstPassenger() == player) whistle(server, 0.95f + 0.1f * pressure);
    }

    /** The engine runs round its train and leads the other way. */
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
        return Component.translatable(StrataIndustria.MOD_ID + ".locomotive.hud", steps[throttle], Math.round(pressure * 100),
                Math.round(water), fuel().getCount());
    }

    private void tell(ServerLevel server, String key) {
        if (nagLeft > 0) return;
        nagLeft = NAG_TICKS;
        Component message = Component.translatable(StrataIndustria.MOD_ID + "." + key);
        for (ServerPlayer player : server.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(24))) player.sendOverlayMessage(message);
    }

    // ---------------------------------------------------------------- stops, tickets and trips

    /** The stop lets it go once it has steam, water and fuel for the run and, in a charter's area, a moving ticket. */
    @Override
    public boolean canDepart(ServerLevel server) {
        if (riderPlayer() == null && parked) return false;
        if (water < LOW_WATER) {
            tell(server, "locomotive.low_water");
            return false;
        }
        if (!hasFire()) {
            tell(server, "locomotive.low_fuel");
            return false;
        }
        if (pressure < PULL_AWAY) return false;
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
                server.playSound(null, getX(), getY(), getZ(), RailwayRegistry.LOCOMOTIVE_BRAKE.get(), SoundSource.NEUTRAL, 0.7f, 1.0f);
                server.playSound(null, getX(), getY(), getZ(), ListeningSounds.BOILER_HISS.get(), SoundSource.NEUTRAL, 0.6f, 1.0f);
                vent();
            }
            arrived(server, stop);
        }
        setThrottle(0);
    }

    /** Counts a leg run without a rider; three round trips make the goal. */
    private void arrived(ServerLevel server, BlockPos stop) {
        if (departedFrom == null || departedFrom.equals(stop)) return;
        departedFrom = null;
        if (legRidden) {
            legs = 0;
            legRidden = false;
            return;
        }
        legs++;
        if (legs >= 6) {
            Journal.awardNear(server, stop, Journal.DRIVERLESS_TRIPS);
            Charter charter = TramwayRoutes.stationCharter(RouteIndex.get(server), stop);
            if (charter != null) Journal.awardOwners(server, charter, Journal.DRIVERLESS_TRIPS);
        }
    }

    @Override
    public void release(BlockPos stop, Vec3 push) {
        // Leaving back the way it came, the engine first runs round its wagons.
        Vec3 turn = new Vec3(push.x, 0, push.z);
        if (heading.lengthSqr() > 1.0E-6 && turn.dot(heading) < 0 && !reverseConsist(heading)) push = push.scale(-1);
        super.release(stop, push);
        Vec3 flat = new Vec3(push.x, 0, push.z);
        if (flat.lengthSqr() > 1.0E-8) {
            heading = flat.normalize();
            syncHeading();
        }
        ticket.leaving();
        departedFrom = stop.immutable();
        if (riderPlayer() != null) legRidden = true;
        else legRidden = false;
        if (level() instanceof ServerLevel server && getDeltaMovement().horizontalDistance() > 0) {
            // Steam from the cylinders as it pulls away.
            vent();
            server.playSound(null, getX(), getY(), getZ(), ListeningSounds.BOILER_HISS.get(), SoundSource.NEUTRAL, 0.5f, 1.3f);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (level() instanceof ServerLevel server) ticket.release(server, this);
        super.remove(reason);
    }

    /** Fuel and water do not count against a stop's rules: they are the engine's own. */
    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public boolean cannotTakeMore(ServerLevel server) {
        return true;
    }

    @Override
    public int contentSignature() {
        return 0;
    }

    @Override
    public double fillAmount() {
        return 0;
    }

    @Override
    public int fillCapacity() {
        return 0;
    }

    @Override
    public boolean loaded() {
        return false;
    }

    /** A locomotive leads; it is never coupled behind anything. */
    @Override
    public Coupling couple() {
        return Coupling.LEADS;
    }

    // ---------------------------------------------------------------- using

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(Items.IRON_CHAIN) || held.is(dev.strataindustria.transport.foot.FootRegistry.ROPE.get())) {
            if (player instanceof ServerPlayer serverPlayer) tryCouple(serverPlayer);
            return InteractionResult.SUCCESS;
        }
        if (FireboxBlockEntity.fuelFor(held) != null) {
            if (level() instanceof ServerLevel server) stoke(server, player, held);
            return InteractionResult.SUCCESS;
        }
        Fluid bucketFluid = FluidBuckets.fluidOf(held);
        if (bucketFluid != null && bucketFluid.isSame(Fluids.WATER)) {
            if (water > WATER_CAPACITY - BUCKET) {
                if (level() instanceof ServerLevel && player instanceof ServerPlayer serverPlayer) serverPlayer.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".locomotive.full_tank"));
                return InteractionResult.CONSUME;
            }
            if (level() instanceof ServerLevel server) {
                takeWater(BUCKET);
                if (!player.getAbilities().instabuild) player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                server.playSound(null, getX(), getY(), getZ(), SoundEvents.BUCKET_EMPTY, SoundSource.NEUTRAL, 1.0f, 1.0f);
                vent();
            }
            return InteractionResult.SUCCESS;
        }
        if (player.isSecondaryUseActive() && held.isEmpty()) {
            if (!level().isClientSide()) {
                if (isCoupled()) {
                    if (follower() != null) follower().decouple();
                    level().playSound(null, getX(), getY(), getZ(), RailRegistry.TUB_COUPLE.get(), SoundSource.NEUTRAL, 0.6f, 0.7f);
                } else {
                    parked = !parked;
                    level().playSound(null, getX(), getY(), getZ(), parked ? RailRegistry.TUB_STOP_BRAKE.get() : SoundEvents.LEVER_CLICK, SoundSource.NEUTRAL, 0.8f, parked ? 0.8f : 1.0f);
                    if (player instanceof ServerPlayer serverPlayer) {
                        serverPlayer.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + (parked ? ".locomotive.parked" : ".locomotive.released")));
                    }
                }
            }
            return InteractionResult.SUCCESS;
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

    /** A hand puts fuel in the firebox door: a rattle, a flare of flame, and the fuel count rising. */
    private void stoke(ServerLevel server, Player player, ItemStack held) {
        int moved = takeFuel(player.isSecondaryUseActive() ? held : held.copyWithCount(1));
        if (moved <= 0) {
            if (player instanceof ServerPlayer serverPlayer) serverPlayer.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".locomotive.full_fuel"));
            return;
        }
        if (!player.getAbilities().instabuild) held.shrink(moved);
        server.playSound(null, getX(), getY(), getZ(), RailwayRegistry.COAL_LOAD.get(), SoundSource.NEUTRAL, 0.8f, 1.0f + 0.05f * Math.min(8, fuel().getCount()));
        server.sendParticles(ParticleTypes.FLAME, getX() - heading.x * 0.5, getY() + 0.45, getZ() - heading.z * 0.5, 6, 0.12, 0.08, 0.12, 0.02);
        if (player instanceof ServerPlayer serverPlayer) serverPlayer.sendOverlayMessage(hud());
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        // The cab is at the back: the rider sits behind the boiler, whichever way the engine runs.
        return new Vec3(-heading.x * 0.38, 0.3, -heading.z * 0.38);
    }

    // ---------------------------------------------------------------- the client's show

    /** Smoke from the chimney with each chuff, steam from the cylinders as it pulls away, a burst from the dome on a vent. */
    private void clientTick() {
        Vec3 front = new Vec3(Math.sin(Math.toRadians(-headingYaw())), 0, Math.cos(Math.toRadians(-headingYaw())));
        double speed = Math.hypot(getX() - xo, getZ() - zo);
        wheelDistance += speed;
        wheelSpeed = speed;
        int chuff = entityData.get(DATA_CHUFF), vent = entityData.get(DATA_VENT);
        double chimneyX = getX() + front.x * 0.42, chimneyY = getY() + 1.0, chimneyZ = getZ() + front.z * 0.42;
        if (chuff != chuffSeen) {
            chuffSeen = chuff;
            level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, chimneyX, chimneyY, chimneyZ, 0, 0.05 + speed * 0.3, 0);
            level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, chimneyX, chimneyY + 0.1, chimneyZ, front.x * speed * 0.2, 0.04, front.z * speed * 0.2);
            if (speed < 0.3) {
                // Cylinder steam on both sides while it is getting away.
                for (int side = -1; side <= 1; side += 2) {
                    level().addParticle(ParticleTypes.CLOUD, getX() + front.x * 0.3 - front.z * 0.5 * side, getY() + 0.3, getZ() + front.z * 0.3 + front.x * 0.5 * side,
                            -front.z * 0.04 * side, 0.01, front.x * 0.04 * side);
                }
            }
        } else if (fireLit() && speed < 0.03 && tickCount % 9 == 0) {
            level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, chimneyX, chimneyY, chimneyZ, 0, 0.03, 0);
        }
        if (vent != ventSeen) {
            ventSeen = vent;
            for (int i = 0; i < 8; i++) {
                level().addParticle(ParticleTypes.CLOUD, getX() + random.nextGaussian() * 0.06, getY() + 1.0, getZ() + random.nextGaussian() * 0.06,
                        random.nextGaussian() * 0.04, 0.12 + random.nextDouble() * 0.06, random.nextGaussian() * 0.04);
            }
        }
    }

    /** How far the wheels have turned, in radians: half a turn a block, so one turn goes with each chuff. */
    public float wheelPhase(float partialTicks) {
        return (float) ((wheelDistance + wheelSpeed * partialTicks) * Math.PI);
    }

    // ---------------------------------------------------------------- saving

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putFloat("Water", water);
        output.putFloat("Pressure", pressure);
        output.putInt("Burn", burnLeft);
        output.putBoolean("Parked", parked);
        output.putInt("Legs", legs);
        output.putDouble("HeadX", heading.x);
        output.putDouble("HeadZ", heading.z);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        water = input.getFloatOr("Water", 0);
        pressure = input.getFloatOr("Pressure", 0);
        burnLeft = input.getIntOr("Burn", 0);
        parked = input.getBooleanOr("Parked", true);
        legs = input.getIntOr("Legs", 0);
        heading = new Vec3(input.getDoubleOr("HeadX", 0), 0, input.getDoubleOr("HeadZ", 0));
        syncHeading();
        syncBoiler();
    }

    /** Whether a driverless leg is under way, for tests. */
    public boolean ticketed() {
        return ticket.open();
    }

    public Vec3 heading() {
        return heading;
    }

    public boolean dry() {
        return dry;
    }
}
