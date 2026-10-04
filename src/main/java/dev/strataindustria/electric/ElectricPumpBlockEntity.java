package dev.strataindustria.electric;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.fluid.FluidPipes;
import dev.strataindustria.fluid.FluidPort;
import dev.strataindustria.fluid.PumpIntake;
import dev.strataindustria.power.ElectricConsumer;
import dev.strataindustria.power.ElectricNetwork;
import dev.strataindustria.power.ElectricNetworks;
import dev.strataindustria.power.ElectricStats;
import dev.strataindustria.power.ElectricTier;
import dev.strataindustria.registry.Tier5BlockEntities;
import dev.strataindustria.registry.Tier5Sounds;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Spec 10.10: the mechanical pump's rules (intake on the front, outlet on the back, infinite water from a
 * source with two water neighbours, brine in oceans) at 100 mB a tick for 4 J/t, through a 4000 mB buffer.
 * LV only. Power below 100% slows it in proportion.
 */
public class ElectricPumpBlockEntity extends BlockEntity implements ElectricConsumer, FluidPort {
    public static final int RATE = 100, CAPACITY = 4000;
    /** What a lone source block gives before it is gone. */
    public static final int SOURCE_AMOUNT = 1000;
    private static final int SOUND_INTERVAL = 40;
    private static final ElectricStats FALLBACK = ElectricStats.standard(4);

    public enum Status {
        NO_POWER, NO_WATER, NO_OUTLET, OUTLET_FULL, PUMPING;

        public String key() {
            return StrataIndustria.MOD_ID + ".electric_pump.status." + name().toLowerCase(Locale.ROOT);
        }
    }

    private Fluid fluid = Fluids.EMPTY;
    private int buffer;
    private double carry;
    private int drained;
    private Status status = Status.NO_POWER;
    private int lastMoved;
    private double power;
    private double received;
    private boolean working;
    private int age;
    private FluidPipes.Network network = FluidPipes.Network.NONE;
    private int networkAge = 20;

    public ElectricPumpBlockEntity(BlockPos pos, BlockState state) {
        super(Tier5BlockEntities.ELECTRIC_PUMP.get(), pos, state);
    }

    private Direction facing() {
        return getBlockState().getValue(ElectricPumpBlock.FACING);
    }

    public Status status() {
        return status;
    }

    public int lastMoved() {
        return lastMoved;
    }

    public int buffer() {
        return buffer;
    }

    public Fluid fluid() {
        return buffer > 0 ? fluid : Fluids.EMPTY;
    }

    public int draw() {
        return ElectricStats.of(getBlockState().getBlock(), FALLBACK).draw().lv();
    }

    @Override
    public ElectricTier tier() {
        return ElectricTier.LV;
    }

    // ------------------------------------------------------------------ fluid

    @Override
    public boolean connectsFluid(Direction side) {
        return side == facing().getOpposite();
    }

    /** It only pushes; nothing is pumped back into it. */
    @Override
    public int fill(Direction side, Fluid fluid, int amount, float pressure, boolean simulate) {
        return 0;
    }

    // ------------------------------------------------------------------ power

    @Override
    public boolean connectsElectric(Direction side) {
        return side != facing() && side != facing().getOpposite();
    }

    @Override
    public double request() {
        return working ? draw() : 0;
    }

    @Override
    public void receive(double amount) {
        received += amount;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ElectricPumpBlockEntity pump) {
        pump.tick((ServerLevel) level, pos, state);
    }

    private void tick(ServerLevel level, BlockPos pos, BlockState state) {
        age++;
        lastMoved = 0;
        ElectricNetwork.Report report = ElectricNetworks.report(level, pos);
        power = report.status().fault() ? 0 : Math.min(1.0, received / Math.max(1, draw()));
        received = 0;
        Status before = status;
        status = pump(level, pos);
        boolean active = status == Status.PUMPING;
        if (active && age % SOUND_INTERVAL == 0) level.playSound(null, pos, Tier5Sounds.ELECTRIC_PUMP_RUN.get(), SoundSource.BLOCKS, 0.4f, 1.0f);
        if (state.getValue(ElectricPumpBlock.ACTIVE) != active) level.setBlock(pos, state.setValue(ElectricPumpBlock.ACTIVE, active), Block.UPDATE_CLIENTS);
        if (status != before) setChanged();
    }

    private Status pump(ServerLevel level, BlockPos pos) {
        BlockPos intake = pos.relative(facing());
        Fluid source = PumpIntake.fluidAt(level, intake);
        // It asks for power only while it could pump more.
        boolean room = buffer < CAPACITY && (buffer == 0 || source != null && fluid.isSame(source));
        working = source != null && room;
        if (++networkAge >= 20) {
            network = FluidPipes.find(level, pos, facing().getOpposite());
            networkAge = 0;
        }
        int moved = 0;
        if (buffer > 0) {
            moved = FluidPipes.push(level, network, fluid, Math.min(buffer, RATE), 20.0f, 0.0f).moved();
            buffer -= moved;
            lastMoved = moved;
        }
        int pumped = 0;
        if (source == null) {
            drained = 0;
            carry = 0;
        } else if (power > 0 && working) {
            carry += RATE * power;
            int whole = (int) carry;
            pumped = Math.min(whole, CAPACITY - buffer);
            carry = Math.min(carry - pumped, 1.0);
        } else {
            carry = 0;
        }
        if (pumped > 0) {
            fluid = source;
            buffer += pumped;
            if (!PumpIntake.infinite(level, intake)) {
                drained += pumped;
                if (drained >= SOURCE_AMOUNT) {
                    drained = 0;
                    level.setBlock(intake, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
        if (moved > 0 || pumped > 0 && !network.isEmpty()) return Status.PUMPING;
        if (buffer > 0 || pumped > 0) return network.isEmpty() ? Status.NO_OUTLET : Status.OUTLET_FULL;
        if (source == null) return Status.NO_WATER;
        return power <= 0 ? Status.NO_POWER : Status.PUMPING;
    }

    public Component report() {
        if (status == Status.PUMPING) return Component.translatable(status.key(), lastMoved);
        return Component.translatable(status.key());
    }

    // ------------------------------------------------------------------ lifecycle

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

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        drained = in.getIntOr("drained", 0);
        buffer = in.getIntOr("buffer", 0);
        fluid = in.read("fluid", BuiltInRegistries.FLUID.byNameCodec()).orElse(Fluids.EMPTY);
        if (fluid.isSame(Fluids.EMPTY)) buffer = 0;
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("drained", drained);
        out.putInt("buffer", buffer);
        if (buffer > 0) out.store("fluid", BuiltInRegistries.FLUID.byNameCodec(), fluid);
    }
}
