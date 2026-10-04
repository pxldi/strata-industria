package dev.strataindustria.electric;

import dev.strataindustria.Config;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.power.ElectricConsumer;
import dev.strataindustria.power.ElectricNetworks;
import dev.strataindustria.power.ElectricNode;
import dev.strataindustria.power.ElectricSource;
import dev.strataindustria.power.ElectricTier;
import dev.strataindustria.registry.Tier5BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The transformer (tier 5 spec 8.2). Its front is the MV side and the other five faces are the LV side; each
 * side is a port in a network of its own. In step-down mode the MV port is an MV consumer and the LV port an
 * LV source; step up swaps them. Power passes through a one-tick buffer, up to 128 J/t, minus the loss.
 */
public class TransformerBlockEntity extends BlockEntity implements ElectricNode {
    /** J/t either side passes (spec 8.2); more than an LV device's own limit, which is why it needs its own cap. */
    public static final double LIMIT = 128.0;
    private static final int MV_PORT = 1;

    private double buffer;
    /** J taken in and sent out on the last tick, for the readout and the hum. */
    private double in, out;
    private double takenIn, sentOut;

    private final Input lvIn = new Input(ElectricTier.LV);
    private final Input mvIn = new Input(ElectricTier.MV);
    private final Output lvOut = new Output(ElectricTier.LV);
    private final Output mvOut = new Output(ElectricTier.MV);

    public TransformerBlockEntity(BlockPos pos, BlockState state) {
        super(Tier5BlockEntities.TRANSFORMER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TransformerBlockEntity transformer) {
        transformer.in = transformer.takenIn;
        transformer.out = transformer.sentOut;
        transformer.takenIn = transformer.sentOut = 0;
        boolean active = transformer.out > 0.01 || transformer.in > 0.01;
        if (active && transformer.out > 0.01 && level.getGameTime() % 100 == 0) Journal.awardNear(level, pos, Journal.TRANSFORMER);
        if (state.getValue(TransformerBlock.ACTIVE) != active) {
            level.setBlock(pos, state.setValue(TransformerBlock.ACTIVE, active), Block.UPDATE_CLIENTS);
        }
    }

    private boolean stepUp() {
        return getBlockState().getValue(TransformerBlock.STEP_UP);
    }

    private Direction front() {
        return getBlockState().getValue(TransformerBlock.FACING);
    }

    /** The "50 Hz" readout: mode, what comes in on which side and what goes out. */
    public Component readout() {
        String key = StrataIndustria.MOD_ID + ".transformer.readout." + (stepUp() ? "up" : "down");
        return Component.translatable(key, ElectricNetworks.power(in), ElectricNetworks.power(out));
    }

    public double buffer() {
        return buffer;
    }

    /** Clears the buffer when the mode changes, so nothing from one direction leaks into the other. */
    public void clearBuffer() {
        buffer = 0;
        setChanged();
    }

    // ---------------------------------------------------------------- ports

    /** Only the two port objects join networks; the block entity itself is never a node. */
    @Override
    public boolean connectsElectric(Direction side) {
        return true;
    }

    @Override
    public int ports() {
        return 2;
    }

    @Override
    public int portAt(Direction side) {
        return side == front() ? MV_PORT : 0;
    }

    @Override
    public ElectricNode port(int index) {
        if (index == MV_PORT) return stepUp() ? mvOut : mvIn;
        return stepUp() ? lvIn : lvOut;
    }

    @Override
    public boolean removed() {
        return isRemoved();
    }

    private abstract class Port implements ElectricNode {
        final ElectricTier tier;

        Port(ElectricTier tier) {
            this.tier = tier;
        }

        int index() {
            return tier == ElectricTier.MV ? MV_PORT : 0;
        }

        public ElectricTier tier() {
            return tier;
        }

        public double powerLimit() {
            return LIMIT;
        }

        @Override
        public boolean connectsElectric(Direction side) {
            return TransformerBlockEntity.this.portAt(side) == index();
        }

        @Override
        public boolean removed() {
            return isRemoved();
        }
    }

    /** The side power comes in on: a consumer of its own tier that fills the buffer. */
    private final class Input extends Port implements ElectricConsumer {
        Input(ElectricTier tier) {
            super(tier);
        }

        @Override
        public double request() {
            return Math.max(0, LIMIT - buffer);
        }

        @Override
        public void receive(double amount) {
            buffer += amount;
            takenIn += amount;
            if (amount > 0) setChanged();
        }
    }

    /** The side power goes out on: a source of its own tier that drains the buffer, less the loss. */
    private final class Output extends Port implements ElectricSource {
        Output(ElectricTier tier) {
            super(tier);
        }

        private double efficiency() {
            return 1.0 - Config.ELECTRIC_TRANSFORMER_LOSS.get();
        }

        @Override
        public double maxOutput() {
            return Math.min(LIMIT, buffer * efficiency());
        }

        @Override
        public void extract(double amount) {
            buffer = Math.max(0, buffer - amount / efficiency());
            sentOut += amount;
            if (amount > 0) setChanged();
        }
    }

    // ---------------------------------------------------------------- lifecycle

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
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        buffer = input.getDoubleOr("buffer", 0.0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("buffer", buffer);
    }
}
