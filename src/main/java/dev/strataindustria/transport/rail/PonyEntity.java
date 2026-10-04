package dev.strataindustria.transport.rail;

import dev.strataindustria.Config;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.transport.outpost.Charter;
import dev.strataindustria.transport.outpost.RouteIndex;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityProcessor;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Donkey;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.equine.Mule;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The pony (outposts spec 5.3): a horse, donkey or mule in harness, standing between the wooden rails. It is the
 * lead of a tub consist and walks it from stop to stop by itself; at a stop it eats from a hay rack within three
 * blocks. Unharnessing gives the animal back as it was. It never takes damage from work and stops, hungry, when
 * the hay is gone.
 */
public class PonyEntity extends MineTubEntity {
    /** Blocks per tick on the level and downhill, and uphill (spec 5.3). */
    public static final double WALK = 0.2;
    public static final double UPHILL = 0.1;
    /** Slope rails in a row a pony will climb before it balks. */
    public static final int MAX_SLOPE_RAILS = 8;
    /** Bales of hay a pony holds at most, and what a freshly harnessed one has in it. */
    public static final float HAY_CAP = 2.0f;
    public static final float HAY_START = 1.0f;
    /** A hay rack counts if its block is this close to the stop. */
    public static final int RACK_REACH = 3;
    public static final int KIND_HORSE = 0, KIND_DONKEY = 1, KIND_MULE = 2;

    private static final int CHEW_TICKS = 30;
    private static final int TURN_TICKS = 30;
    private static final int BALK_TICKS = 100;
    private static final int NAG_TICKS = 200;
    private static final int TICKET_LINGER = 600;
    private static final double EDGE_OFFSET = 0.2;

    private static final EntityDataAccessor<Integer> DATA_KIND = SynchedEntityData.defineId(PonyEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LOOK = SynchedEntityData.defineId(PonyEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_EATING = SynchedEntityData.defineId(PonyEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_HEADING = SynchedEntityData.defineId(PonyEntity.class, EntityDataSerializers.FLOAT);

    private @Nullable CompoundTag animal;
    private float hay = HAY_START;
    private Vec3 heading = Vec3.ZERO;
    private @Nullable BlockPos lastRail;
    private int slopeRun;
    private int balkLeft;
    private int turnLeft;
    private int eatLeft;
    private int nagLeft;
    private int lingering;
    private boolean rideWalk;
    private boolean ticketed;
    // client
    private float walkSpeed, walkSpeedO, walkPos;

    public PonyEntity(EntityType<? extends PonyEntity> type, net.minecraft.world.level.Level level) {
        super(type, level);
    }

    // ---------------------------------------------------------------- the animal

    /** Whether {@code animal} can pull a tramway: an adult tamed horse, donkey or mule. */
    public static boolean canHarness(Entity animal) {
        return animal instanceof AbstractHorse horse && (horse instanceof Horse || horse instanceof Donkey || horse instanceof Mule)
                && horse.isTamed() && !horse.isBaby() && horse.isAlive() && !horse.isVehicle() && !horse.isPassenger();
    }

    /**
     * Puts {@code animal} in harness on the track block at {@code rail}: the animal is saved whole and replaced by a
     * pony. Returns null when it cannot be done.
     */
    public static @Nullable PonyEntity harness(ServerLevel level, AbstractHorse animal, BlockPos rail) {
        BlockState state = level.getBlockState(rail);
        if (!(state.getBlock() instanceof BaseRailBlock block) || !state.is(RailRegistry.TRACK) || !canHarness(animal)) return null;
        PonyEntity pony = RailRegistry.PONY_ENTITY.get().create(level, EntitySpawnReason.TRIGGERED);
        if (pony == null) return null;
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        if (!animal.saveAsPassenger(out)) return null;
        pony.animal = out.buildResult();
        int kind = animal instanceof Donkey ? KIND_DONKEY : animal instanceof Mule ? KIND_MULE : KIND_HORSE;
        pony.entityData.set(DATA_KIND, kind);
        if (animal instanceof Horse horse) pony.entityData.set(DATA_LOOK, horse.getVariant().getId() | horse.getMarkings().ordinal() << 8);
        RailShape shape = state.getValue(block.getShapeProperty());
        double y = rail.getY() + 0.0625 + (shape.isSlope() ? 0.5 : 0.0);
        pony.setPos(rail.getX() + 0.5, y, rail.getZ() + 0.5);
        Vec3 look = Vec3.directionFromRotation(0, animal.getYRot());
        Vec3 axis = pony.railAxis(rail);
        pony.heading = axis.dot(look) >= 0 ? axis : axis.scale(-1);
        pony.syncHeading();
        if (animal.hasCustomName()) pony.setCustomName(animal.getCustomName());
        animal.discard();
        level.addFreshEntity(pony);
        return pony;
    }

    /** Takes the harness off: the animal stands where the pony did, with the harness dropped beside it. */
    public @Nullable Entity unharness(ServerLevel level) {
        Entity restored = null;
        if (animal != null) {
            ValueInput in = TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), animal);
            restored = EntityType.loadEntityRecursive(in, level, EntitySpawnReason.LOAD, EntityProcessor.NOP);
            if (restored != null) {
                restored.snapTo(getX(), getY(), getZ(), (float) Math.toDegrees(Math.atan2(-heading.x, heading.z)), 0);
                restored.setDeltaMovement(Vec3.ZERO);
                level.addFreshEntity(restored);
            }
        }
        ejectPassengers();
        decoupleAll();
        spawnAtLocation(level, new ItemStack(RailRegistry.HARNESS.get()));
        animal = null;
        discard();
        return restored;
    }

    private void decoupleAll() {
        MineTubEntity back = follower();
        if (back != null) back.decouple();
    }

    @Override
    protected Item getDropItem() {
        return RailRegistry.HARNESS.get();
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(RailRegistry.HARNESS.get());
    }

    /** Hitting a pony down gives the animal back too; it takes no harm. */
    @Override
    public void destroy(ServerLevel level, DamageSource source) {
        unharness(level);
    }

    @Override
    public int getContainerSize() {
        return 0;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new DispenserMenu(containerId, inventory, new net.minecraft.world.SimpleContainer(9));
    }

    @Override
    public boolean isRideable() {
        return true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_KIND, KIND_HORSE);
        entityData.define(DATA_LOOK, 0);
        entityData.define(DATA_EATING, false);
        entityData.define(DATA_HEADING, 0f);
    }

    public int kind() {
        return entityData.get(DATA_KIND);
    }

    /** The horse's coat (low byte) and markings (next byte) as the renderer draws them. */
    public int look() {
        return entityData.get(DATA_LOOK);
    }

    public boolean eating() {
        return entityData.get(DATA_EATING);
    }

    /** The way the pony faces, as a yaw in degrees. */
    public float headingYaw() {
        return entityData.get(DATA_HEADING);
    }

    public float hay() {
        return hay;
    }

    public void setHay(float bales) {
        hay = Mth.clamp(bales, 0, HAY_CAP);
    }

    /** Walk animation state for the renderer. */
    public float walkSpeed(float partial) {
        return Mth.lerp(partial, walkSpeedO, walkSpeed);
    }

    public float walkPos(float partial) {
        return walkPos - walkSpeed * (1.0f - partial);
    }

    // ---------------------------------------------------------------- what it wants

    @Override
    public Vec3 intent() {
        return heading;
    }

    private void syncHeading() {
        if (heading.lengthSqr() > 1.0E-6) entityData.set(DATA_HEADING, (float) Math.toDegrees(Math.atan2(-heading.x, heading.z)));
    }

    private void turnAround() {
        if (level() instanceof ServerLevel && !reverseConsist(heading)) {
            // No rail to walk round on: it stands a while longer.
            turnLeft = TURN_TICKS;
            return;
        }
        heading = heading.scale(-1);
        syncHeading();
        slopeRun = 0;
        turnLeft = 0;
        balkLeft = 0;
    }

    /** Whether the track climbs on the way the pony is going. */
    private boolean uphill(BlockPos rail) {
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

    /** True when the track ends ahead: no rail where the pony is headed, in a loaded chunk. */
    private boolean deadEnd(BlockPos rail) {
        BlockState state = level().getBlockState(rail);
        if (!(state.getBlock() instanceof BaseRailBlock block)) return false;
        Vec3 ahead = null;
        double best = 0.1;
        for (Vec3i exit : new Vec3i[] {
                net.minecraft.world.entity.vehicle.minecart.AbstractMinecart.exits(state.getValue(block.getShapeProperty())).getFirst(),
                net.minecraft.world.entity.vehicle.minecart.AbstractMinecart.exits(state.getValue(block.getShapeProperty())).getSecond()}) {
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

    /** Whether the pony is far enough into its block, going the way it heads, to be stopped before the edge. */
    private boolean nearEdge(BlockPos rail) {
        double offset = (getX() - (rail.getX() + 0.5)) * heading.x + (getZ() - (rail.getZ() + 0.5)) * heading.z;
        return offset > EDGE_OFFSET;
    }

    /** The speed it wants this tick; zero while held, balking, ridden and told to stop, or at an end of the line. */
    private double walkSpeedFor(BlockPos rail) {
        if (balkLeft > 0 || turnLeft > 0) return 0;
        if (isHeld()) return 0;
        if (hasPassenger(e -> e instanceof Player) && !rideWalk) return 0;
        return uphill(rail) ? UPHILL : WALK;
    }

    /** The pony walks by itself: a steady pace along the track in the way it heads, and it never loses speed to friction. */
    @Override
    protected Vec3 applyNaturalSlowdown(Vec3 movement) {
        if (winched() || level().isClientSide() || !onOurTrack()) return super.applyNaturalSlowdown(movement);
        if (isHeld()) return movement;
        BlockPos rail = railBlock();
        if (rail == null) return super.applyNaturalSlowdown(movement);
        Vec3 flat = new Vec3(movement.x, 0, movement.z);
        if (flat.lengthSqr() > 1.0E-4) {
            heading = flat.normalize();
            syncHeading();
        } else if (heading.lengthSqr() < 1.0E-6) {
            heading = railAxis(rail);
            syncHeading();
        }
        if (!level().hasChunkAt(rail.relative(Direction.getApproximateNearest(heading.x, 0, heading.z)))) return Vec3.ZERO;
        double speed = walkSpeedFor(rail);
        if (speed > 0 && nearEdge(rail) && (deadEnd(rail) || atBuffer())) {
            turnLeft = TURN_TICKS;
            speed = 0;
        }
        return heading.scale(speed);
    }

    // ---------------------------------------------------------------- ticking

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            walkSpeedO = walkSpeed;
            double moved = Math.hypot(getX() - xo, getZ() - zo);
            walkSpeed += (Math.min((float) moved * 4.0f, 1.0f) - walkSpeed) * 0.4f;
            walkPos += walkSpeed;
        } else if (level() instanceof ServerLevel server) {
            serverTick(server);
        }
    }

    private void serverTick(ServerLevel server) {
        BlockPos rail = railBlock();
        if (rail != null && !winched() && isLead() && !rail.equals(lastRail)) {
            lastRail = rail;
            if (isHeld() || !onOurTrack()) slopeRun = 0;
            else if (uphill(rail)) {
                if (++slopeRun > MAX_SLOPE_RAILS && balkLeft <= 0) {
                    balkLeft = BALK_TICKS;
                    tell(server, "pony.balks");
                    server.playSound(null, getX(), getY(), getZ(), RailRegistry.PONY_SNORT.get(), SoundSource.NEUTRAL, 0.8f, 1.0f);
                }
            } else {
                slopeRun = 0;
            }
        }
        if (winched()) slopeRun = 0;
        if (balkLeft > 0 && --balkLeft == 0) turnAround();
        if (turnLeft > 0 && --turnLeft == 0) turnAround();
        if (nagLeft > 0) nagLeft--;
        ridden();
        if (isHeld()) feed(server);
        else if (eatLeft > 0) eatLeft = 0;
        entityData.set(DATA_EATING, eatLeft > 0);
        ticket(server);
    }

    /** A rider sets walk or stop with W and S, the way it does on a horse. */
    private void ridden() {
        if (!(getFirstPassenger() instanceof ServerPlayer rider)) {
            rideWalk = false;
            return;
        }
        var input = rider.getLastClientInput();
        if (input.forward() && !rideWalk) {
            rideWalk = true;
            Vec3 look = Vec3.directionFromRotation(0, rider.getYRot());
            BlockPos rail = railBlock();
            Vec3 axis = rail == null ? heading : railAxis(rail);
            if (axis.lengthSqr() > 1.0E-6) {
                Vec3 flat = new Vec3(look.x, 0, look.z);
                heading = axis.dot(flat) >= 0 ? axis : axis.scale(-1);
                syncHeading();
            }
            if (isHeld()) setDeltaMovement(heading.scale(TubStopBlockEntity.START_PUSH));
        }
        if (input.backward()) rideWalk = false;
    }

    // ---------------------------------------------------------------- hay

    /** Hay used by one leg of a trip, from one stop to the next. */
    private static float legCost() {
        return (float) (Config.TRANSPORT_PONY_HAY_PER_TRIP.getAsDouble() / 2.0);
    }

    /** At a stop, eats from a hay rack within reach once there is room for a bale. */
    private void feed(ServerLevel server) {
        BlockPos stop = heldAt();
        if (stop == null) return;
        if (eatLeft > 0) {
            eatLeft--;
            return;
        }
        if (hay > HAY_CAP - 1.0f || server.getGameTime() % 10 != 0) return;
        HayRackBlockEntity rack = rackNear(server, stop);
        if (rack == null || !rack.takeBale()) return;
        hay = Math.min(HAY_CAP, hay + 1.0f);
        eatLeft = CHEW_TICKS;
        server.playSound(null, getX(), getY(), getZ(), RailRegistry.PONY_EAT.get(), SoundSource.NEUTRAL, 0.8f, 1.0f);
    }

    private @Nullable HayRackBlockEntity rackNear(ServerLevel server, BlockPos stop) {
        for (BlockPos pos : BlockPos.betweenClosed(stop.offset(-RACK_REACH, -RACK_REACH, -RACK_REACH), stop.offset(RACK_REACH, RACK_REACH, RACK_REACH))) {
            if (server.getBlockState(pos).is(RailRegistry.HAY_RACK.get()) && server.getBlockEntity(pos) instanceof HayRackBlockEntity rack
                    && !rack.isEmpty()) return rack;
        }
        return null;
    }

    private void tell(ServerLevel server, String key) {
        if (nagLeft > 0) return;
        nagLeft = NAG_TICKS;
        Component message = Component.translatable(StrataIndustria.MOD_ID + "." + key);
        for (ServerPlayer player : server.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(24))) player.sendOverlayMessage(message);
    }

    // ---------------------------------------------------------------- stops and tickets

    /** The stop lets the pony go when it has hay for the leg and, in a charter's area, a moving ticket. */
    @Override
    public boolean canDepart(ServerLevel server) {
        if (hay + 1.0E-4f < legCost()) {
            tell(server, "pony.hungry");
            dev.strataindustria.journal.Observations.hungryPony(server, blockPosition());
            return false;
        }
        BlockPos stop = heldAt();
        if (stop != null && !ticketed && !hasPassenger(e -> e instanceof Player)) {
            Charter charter = TramwayRoutes.stationCharter(RouteIndex.get(server), stop);
            if (charter != null && VehicleTickets.ownerAround(server, charter)) {
                if (!VehicleTickets.available(server, charter.owner())) {
                    tell(server, "pony.no_line");
                    return false;
                }
                VehicleTickets.open(getUUID(), charter.owner());
                ticketed = true;
            }
        }
        return true;
    }

    @Override
    public void release(BlockPos stop, Vec3 push) {
        // Leaving back the way it came, the pony first walks round to the other end of its tubs.
        Vec3 turn = new Vec3(push.x, 0, push.z);
        if (heading.lengthSqr() > 1.0E-6 && turn.dot(heading) < 0 && !reverseConsist(heading)) push = push.scale(-1);
        super.release(stop, push);
        hay -= legCost();
        Vec3 flat = new Vec3(push.x, 0, push.z);
        if (flat.lengthSqr() > 1.0E-8) {
            heading = flat.normalize();
            syncHeading();
        }
        lingering = 0;
        slopeRun = 0;
    }

    /** Keeps the nine chunks around the pony loaded while it runs a line, and lets them go 30 seconds after it rests in a station. */
    private void ticket(ServerLevel server) {
        if (!ticketed) return;
        if (isHeld() && heldAt() != null && TramwayRoutes.stationCharter(RouteIndex.get(server), heldAt()) != null) {
            if (++lingering >= TICKET_LINGER) {
                VehicleTickets.release(server, getUUID());
                ticketed = false;
                lingering = 0;
                return;
            }
        } else {
            lingering = 0;
        }
        if (tickCount % 4 == 0) VehicleTickets.follow(server, getUUID(), chunkPosition());
    }

    @Override
    public void remove(RemovalReason reason) {
        if (level() instanceof ServerLevel server) VehicleTickets.release(server, getUUID());
        super.remove(reason);
    }

    // ---------------------------------------------------------------- using

    /** A pony leads; it is never coupled behind anything. */
    @Override
    public Coupling couple() {
        return Coupling.LEADS;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        ItemStack held = player.getItemInHand(hand);
        if (player.isSecondaryUseActive() && held.isEmpty()) {
            if (level() instanceof ServerLevel server) {
                server.playSound(null, getX(), getY(), getZ(), RailRegistry.PONY_HARNESS.get(), SoundSource.NEUTRAL, 0.8f, 0.8f);
                unharness(server);
            }
            return InteractionResult.SUCCESS;
        }
        if (held.is(net.minecraft.world.item.Items.HAY_BLOCK) && hay <= HAY_CAP - 1.0f) {
            if (level() instanceof ServerLevel server) {
                if (!player.isCreative()) held.shrink(1);
                hay = Math.min(HAY_CAP, hay + 1.0f);
                eatLeft = CHEW_TICKS;
                server.playSound(null, getX(), getY(), getZ(), RailRegistry.PONY_EAT.get(), SoundSource.NEUTRAL, 0.8f, 1.0f);
            }
            return InteractionResult.SUCCESS;
        }
        if (held.is(net.minecraft.world.item.Items.IRON_CHAIN) || held.is(dev.strataindustria.transport.foot.FootRegistry.ROPE.get())) {
            if (player instanceof ServerPlayer serverPlayer) tryCouple(serverPlayer);
            return InteractionResult.SUCCESS;
        }
        if (!isVehicle()) {
            if (level() instanceof ServerLevel && player.startRiding(this)) return InteractionResult.SUCCESS_SERVER;
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    /** What a hand on the pony tells: how full it is. */
    public Component hayStatus() {
        return Component.translatable(StrataIndustria.MOD_ID + ".pony.hay", Math.round(hay * 100) / 100.0);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        // Work and blows do not hurt a pony; only the void and commands take it, and then the animal comes back.
        if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            unharness(level);
            return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- sound

    /** Hooves on the sleepers, not wheels. */
    @Override
    protected void roll(ServerLevel server) {
        if (!onOurTrack()) return;
        double speed = getDeltaMovement().horizontalDistance();
        if (speed < 0.02 || tickCount % 5 != 0) return;
        server.playSound(null, getX(), getY(), getZ(), RailRegistry.PONY_STEP.get(), SoundSource.NEUTRAL, 0.5f, 0.9f + server.getRandom().nextFloat() * 0.2f);
    }

    // ---------------------------------------------------------------- saving

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (animal != null) output.store("Animal", CompoundTag.CODEC, animal);
        output.putFloat("Hay", hay);
        output.putDouble("HeadX", heading.x);
        output.putDouble("HeadZ", heading.z);
        output.putInt("Kind", kind());
        output.putInt("Look", look());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        animal = input.read("Animal", CompoundTag.CODEC).orElse(null);
        hay = input.getFloatOr("Hay", HAY_START);
        heading = new Vec3(input.getDoubleOr("HeadX", 0), 0, input.getDoubleOr("HeadZ", 0));
        entityData.set(DATA_KIND, input.getIntOr("Kind", KIND_HORSE));
        entityData.set(DATA_LOOK, input.getIntOr("Look", 0));
        syncHeading();
    }

    /** The animal this pony was, for tests. */
    public @Nullable CompoundTag animalTag() {
        return animal;
    }

    /** True while the pony is standing off a hill it will not climb. */
    public boolean balking() {
        return balkLeft > 0;
    }

    /** Slope rails climbed in a row, for tests. */
    public int slopeRun() {
        return slopeRun;
    }

    public void setSlopeRun(int rails) {
        slopeRun = rails;
    }

    /** Whether the pony holds a moving ticket, for tests. */
    public boolean ticketed() {
        return ticketed;
    }

    public UUID id() {
        return getUUID();
    }

    /** Awards the harness goal to whoever put it on. */
    public static void award(ServerPlayer player) {
        Journal.award(player, Journal.PONY_HARNESSED);
    }
}
