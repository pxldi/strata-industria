package dev.strataindustria.transport.ropeway;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * The place a rider sits on a ropeway bucket's hanger (outposts spec 8.3). It has no life of its own: the terminal
 * that owns the line puts it on the rope every tick, and it lets the rider go if the terminal stops saying where it is.
 * It is not saved, so a rider is never left hanging on a line that has not been loaded.
 */
public class RopewaySeatEntity extends Entity {
    /** Ticks without news from the terminal after which the seat lets go. */
    private static final int STALE = 20;
    /** Blocks the seat hangs below the rope. */
    public static final double DROP = 1.35;

    private long drivenAt = -1;
    private boolean moving;
    private boolean releasing;

    public RopewaySeatEntity(EntityType<? extends RopewaySeatEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    /** The terminal puts the seat where the rope has taken it. {@code rope} is the point on the line the hanger hangs from. */
    public void drive(Vec3 rope, Vec3 heading, double speed, long time) {
        double flat = Math.sqrt(heading.x * heading.x + heading.z * heading.z);
        if (flat > 1.0E-4) setYRot((float) (Math.toDegrees(Math.atan2(-heading.x, heading.z))));
        setPos(rope.x, rope.y - DROP, rope.z);
        setDeltaMovement(heading.scale(speed));
        drivenAt = time;
        moving = speed > 1.0E-4;
    }

    /** The rider is being let off; no sneak is needed and none is refused. */
    public void release() {
        releasing = true;
    }

    /** Whether the rider may climb out: at a station, when the line stands still, or when the seat is adrift. */
    public boolean mayLeave() {
        return releasing || isRemoved() || !moving || stale();
    }

    private boolean stale() {
        return drivenAt >= 0 && level().getGameTime() - drivenAt > STALE;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel)) return;
        if (drivenAt < 0) drivenAt = level().getGameTime();
        // Adrift (nobody is saying where it is) or empty: it goes, and a rider still on it comes down slowly.
        if (stale() || (tickCount > 5 && getPassengers().isEmpty())) discard();
    }

    /** Someone who climbs out in mid-air falls slowly: the line is not a fall to be afraid of. */
    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (!level().isClientSide() && passenger instanceof LivingEntity living && !releasing) {
            living.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 160, 0, false, false, true));
        }
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty();
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        return position().add(0, 0.6, 0);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    protected void readAdditionalSaveData(ValueInput input) {}

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {}
}
