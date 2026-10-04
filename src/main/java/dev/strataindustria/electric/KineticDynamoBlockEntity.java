package dev.strataindustria.electric;

import dev.strataindustria.Config;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.power.ElectricNetwork;
import dev.strataindustria.power.ElectricNetworks;
import dev.strataindustria.power.ElectricSource;
import dev.strataindustria.power.ElectricStatus;
import dev.strataindustria.power.ElectricTier;
import dev.strataindustria.power.KineticBlockEntity;
import dev.strataindustria.power.KineticConsumer;
import dev.strataindustria.power.KineticState;
import dev.strataindustria.power.StatusLight;
import dev.strataindustria.registry.Tier5BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Spec 7.1: loads its shaft by 16 SU per RPM and makes 0.25 J/t per RPM, up to 32 J/t at 128 RPM.
 * Unlike other generators its load never throttles: what the network does not take is lost.
 */
public class KineticDynamoBlockEntity extends KineticBlockEntity implements KineticConsumer, ElectricSource {
    public static final int IMPACT = 16;
    public static final int MIN_SPEED = 8;

    private double extracted;

    public KineticDynamoBlockEntity(BlockPos pos, BlockState state) {
        super(Tier5BlockEntities.KINETIC_DYNAMO.get(), pos, state);
    }

    /** J/t the dynamo makes at {@code rpm}: none below 8 RPM, capped at the LV limit. */
    public static double outputAt(float rpm) {
        if (rpm < MIN_SPEED) return 0.0;
        return Math.min(ElectricTier.LV.maxPower(), rpm * Config.ELECTRIC_DYNAMO_JOULES_PER_RPM.get());
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, KineticDynamoBlockEntity dynamo) {
        StatusLight light = dynamo.light(ElectricNetworks.report(level, pos));
        if (dynamo.extracted > 0 && level.getGameTime() % 100 == 0) Journal.awardNear(level, pos, Journal.DYNAMO);
        dynamo.extracted = 0;
        if (state.getValue(KineticDynamoBlock.STATUS) != light) {
            level.setBlock(pos, state.setValue(KineticDynamoBlock.STATUS, light), Block.UPDATE_CLIENTS);
        }
    }

    private StatusLight light(ElectricNetwork.Report report) {
        if (report.status().fault()) return StatusLight.ERROR;
        if (kinetic().status() != KineticState.Status.RUNNING || kinetic().rpm() < MIN_SPEED) return StatusLight.OFF;
        // "Spinning, no demand" shows amber.
        return extracted > 0 ? StatusLight.RUN : StatusLight.WAIT;
    }

    @Override
    public int impact() {
        return IMPACT;
    }

    @Override
    public int minSpeed() {
        return MIN_SPEED;
    }

    @Override
    public int speedLimit() {
        return dev.strataindustria.power.IronTransmission.SPEED_LIMIT;
    }

    @Override
    public ElectricTier tier() {
        return ElectricTier.LV;
    }

    @Override
    public boolean connectsElectric(Direction side) {
        return side != getBlockState().getValue(KineticDynamoBlock.FACING).getOpposite();
    }

    @Override
    public double maxOutput() {
        return kinetic().status() == KineticState.Status.RUNNING ? outputAt(kinetic().rpm()) : 0.0;
    }

    @Override
    public void extract(double amount) {
        extracted = amount;
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
