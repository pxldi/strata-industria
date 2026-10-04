package dev.strataindustria.steam;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.fluid.FluidPipes;
import dev.strataindustria.fluid.FluidPort;
import dev.strataindustria.fluid.PumpIntake;
import dev.strataindustria.power.KineticBlockEntity;
import dev.strataindustria.power.KineticConsumer;
import dev.strataindustria.registry.Tier4BlockEntities;
import dev.strataindustria.registry.Tier4Sounds;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The mechanical pump (tier 4 spec 9.3 and 11.1): 25 mB a tick at 16 RPM, more or less with speed,
 * from the water source block at its intake into the pipes at its back. A source with two water source
 * neighbours refills itself, as vanilla water does; any other source runs dry after a bucket.
 */
public class MechanicalPumpBlockEntity extends KineticBlockEntity implements KineticConsumer, FluidPort {
    public static final int IMPACT = 2, MIN_SPEED = 8;
    /** mB per tick at 16 RPM. */
    public static final int RATE = 25;
    /** What a lone source block gives before it is gone. */
    public static final int SOURCE_AMOUNT = 1000;

    public enum Status {
        NOT_TURNING, TOO_SLOW, NO_WATER, NO_OUTLET, OUTLET_FULL, PUMPING;

        public String key() {
            return StrataIndustria.MOD_ID + ".mechanical_pump.status." + name().toLowerCase(Locale.ROOT);
        }
    }

    private Status status = Status.NOT_TURNING;
    private float carry;
    private int drained;
    private int lastMoved;
    private int age;
    private FluidPipes.Network network = FluidPipes.Network.NONE;
    private int networkAge = 20;

    public MechanicalPumpBlockEntity(BlockPos pos, BlockState state) {
        super(Tier4BlockEntities.MECHANICAL_PUMP.get(), pos, state);
    }

    private Direction facing() {
        return getBlockState().getValue(MechanicalPumpBlock.FACING);
    }

    public Status status() {
        return status;
    }

    /** mB it moved last tick. */
    public int lastMoved() {
        return lastMoved;
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
    public boolean connectsFluid(Direction side) {
        return side == facing().getOpposite();
    }

    /** It only pushes; nothing is pumped back into it. */
    @Override
    public int fill(Direction side, Fluid fluid, int amount, float pressure, boolean simulate) {
        return 0;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MechanicalPumpBlockEntity pump) {
        pump.tick((ServerLevel) level, pos);
    }

    private void tick(ServerLevel level, BlockPos pos) {
        age++;
        lastMoved = 0;
        Status before = status;
        status = pump(level, pos);
        if (status == Status.PUMPING && age % 30 == 0) {
            float rpm = kinetic().rpm();
            float pitch = rpm >= 32 ? 1.2f : rpm >= 16 ? 1.0f : 0.8f;
            level.playSound(null, pos, Tier4Sounds.MECHANICAL_PUMP_RUN.get(), SoundSource.BLOCKS, 0.5f, pitch);
        }
        if (status != before) setChanged();
    }

    private Status pump(ServerLevel level, BlockPos pos) {
        float rpm = kinetic().rpm();
        if (rpm <= 0) return Status.NOT_TURNING;
        if (rpm < MIN_SPEED) return Status.TOO_SLOW;
        BlockPos intake = pos.relative(facing());
        Fluid drawn = PumpIntake.fluidAt(level, intake);
        if (drawn == null) {
            drained = 0;
            carry = 0;
            return Status.NO_WATER;
        }
        if (++networkAge >= 20) {
            network = FluidPipes.find(level, pos, facing().getOpposite());
            networkAge = 0;
        }
        if (network.isEmpty()) {
            carry = 0;
            return Status.NO_OUTLET;
        }
        carry += RATE * rpm / 16.0f;
        int amount = (int) carry;
        carry -= amount;
        FluidPipes.Push push = FluidPipes.push(level, network, drawn, amount, 20.0f, 0.0f);
        lastMoved = push.moved();
        if (lastMoved <= 0) return Status.OUTLET_FULL;
        if (!PumpIntake.infinite(level, intake)) {
            drained += lastMoved;
            if (drained >= SOURCE_AMOUNT) {
                drained = 0;
                level.setBlock(intake, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        return Status.PUMPING;
    }

    public Component report() {
        if (status == Status.PUMPING) return Component.translatable(status.key(), lastMoved);
        return Component.translatable(status.key());
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        drained = in.getIntOr("drained", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("drained", drained);
    }
}
