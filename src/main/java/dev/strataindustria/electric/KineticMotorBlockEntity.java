package dev.strataindustria.electric;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.power.ElectricConsumer;
import dev.strataindustria.power.ElectricNetwork;
import dev.strataindustria.power.ElectricNetworks;
import dev.strataindustria.power.ElectricStats;
import dev.strataindustria.power.ElectricTier;
import dev.strataindustria.power.IronTransmission;
import dev.strataindustria.power.KineticBlockEntity;
import dev.strataindustria.power.KineticNetworks;
import dev.strataindustria.power.KineticSource;
import dev.strataindustria.power.StatusLight;
import dev.strataindustria.registry.Tier5BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Spec 10.11: electricity back into rotation, for the tier 3 and 4 machines that have no river or steam
 * to turn them. It draws 16 J/t (LV, 32 RPM, 512 SU) or 64 J/t (MV, 64 RPM, 2048 SU) all the time. With
 * the full draw it runs at full speed; with 50 to 99% it runs at half speed and half capacity; below 50% it
 * stops. The shaft comes out of the front face.
 */
public class KineticMotorBlockEntity extends KineticBlockEntity implements KineticSource, ElectricConsumer {
    public static final float LV_SPEED = 32.0f, MV_SPEED = 64.0f;
    public static final int LV_CAPACITY = 512, MV_CAPACITY = 2048;
    private static final ElectricStats FALLBACK = ElectricStats.standard(16);

    /** Share of its draw the network delivered last tick. */
    private double power;
    private double received;
    private float speed;
    private int capacity;

    public KineticMotorBlockEntity(BlockPos pos, BlockState state) {
        super(Tier5BlockEntities.KINETIC_MOTOR.get(), pos, state);
    }

    @Override
    public ElectricTier tier() {
        return getBlockState().getValue(KineticMotorBlock.TIER);
    }

    public Direction facing() {
        return getBlockState().getValue(KineticMotorBlock.FACING);
    }

    /** J/t it wants. */
    public int draw() {
        return ElectricStats.of(getBlockState().getBlock(), FALLBACK).draw().get(tier());
    }

    /** What the motor runs at in {@code power} of its draw (spec 10.11): full, half, or stopped. */
    public static float speedAt(ElectricTier tier, double power) {
        float full = tier == ElectricTier.MV ? MV_SPEED : LV_SPEED;
        return power >= 0.999 ? full : power >= 0.5 ? full / 2 : 0.0f;
    }

    public static int capacityAt(ElectricTier tier, double power) {
        int full = tier == ElectricTier.MV ? MV_CAPACITY : LV_CAPACITY;
        return power >= 0.999 ? full : power >= 0.5 ? full / 2 : 0;
    }

    /** Share of its draw it got last tick, 0 to 1. */
    public double power() {
        return power;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, KineticMotorBlockEntity motor) {
        motor.tick(level, pos, state);
    }

    private void tick(Level level, BlockPos pos, BlockState state) {
        ElectricNetwork.Report report = ElectricNetworks.report(level, pos);
        boolean fault = report.status().fault();
        power = fault ? 0 : Math.min(1.0, received / Math.max(1, draw()));
        received = 0;
        float target = speedAt(tier(), power);
        int cap = capacityAt(tier(), power);
        if (target != speed || cap != capacity) {
            speed = target;
            capacity = cap;
            KineticNetworks.markDirty(level, pos);
            setChanged();
        }
        StatusLight light;
        if (fault) light = StatusLight.ERROR;
        else if (speed >= (tier() == ElectricTier.MV ? MV_SPEED : LV_SPEED)) light = StatusLight.RUN;
        else if (power > 0) light = StatusLight.WAIT;
        else light = StatusLight.OFF;
        if (state.getValue(KineticMotorBlock.STATUS) != light) level.setBlock(pos, state.setValue(KineticMotorBlock.STATUS, light), Block.UPDATE_CLIENTS);
    }

    // ------------------------------------------------------------------ kinetic

    @Override
    public float sourceSpeed() {
        return speed;
    }

    @Override
    public int capacity() {
        return capacity;
    }

    /** Steel and iron throughout: it takes the iron transmission's speeds. */
    @Override
    public int speedLimit() {
        return IronTransmission.SPEED_LIMIT;
    }

    @Override
    public @Nullable Component idleReason() {
        if (speed > 0) return null;
        return Component.translatable(StrataIndustria.MOD_ID + ".kinetic_motor." + (power > 0 ? "low_power" : "no_power"));
    }

    // ------------------------------------------------------------------ power

    @Override
    public boolean connectsElectric(Direction side) {
        return side != facing();
    }

    @Override
    public double request() {
        return draw();
    }

    @Override
    public void receive(double amount) {
        received += amount;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        ElectricNetworks.markDirty(level, worldPosition);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        ElectricNetworks.markDirty(level, worldPosition);
    }
}
