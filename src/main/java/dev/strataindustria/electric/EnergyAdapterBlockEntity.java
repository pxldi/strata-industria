package dev.strataindustria.electric;

import dev.strataindustria.Config;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.power.ElectricConsumer;
import dev.strataindustria.power.ElectricNetworks;
import dev.strataindustria.power.ElectricSource;
import dev.strataindustria.power.ElectricTier;
import dev.strataindustria.registry.Tier5BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

/**
 * The energy adapter (tier 5 spec 8.5): its front face is the only place the mod exposes NeoForge's energy
 * capability, 1 J = 4 FE. Out, it is a consumer of its own tier that pushes what it receives into the FE
 * block in front. In, it takes FE from the front into a one-tick buffer and is a source of its own tier of
 * at most {@code electric.feInputMax} J/t, whatever its tier.
 */
public class EnergyAdapterBlockEntity extends BlockEntity implements ElectricSource, ElectricConsumer {
    /** The FE a neighbour may push in: one tick's worth. */
    private final class Buffer extends SimpleEnergyHandler {
        Buffer() {
            super(0);
        }

        void refresh() {
            capacity = maxInFe();
            maxInsert = capacity;
            if (energy > capacity) energy = capacity;
        }

        void drain(int fe) {
            energy = Math.max(0, energy - fe);
        }

        @Override
        public int insert(int amount, TransactionContext transaction) {
            refresh();
            return super.insert(amount, transaction);
        }

        @Override
        protected void onEnergyChanged(int previousAmount) {
            setChanged();
        }
    }

    private final Buffer buffer = new Buffer();
    /** J/t the FE block in front can take, worked out once per tick. */
    private double outRequest;
    /** Fraction of an FE carried between ticks. */
    private double carry;
    private double fedOut, tookIn, lastOut, lastIn;

    public EnergyAdapterBlockEntity(BlockPos pos, BlockState state) {
        super(Tier5BlockEntities.ENERGY_ADAPTER.get(), pos, state);
    }

    private static int ratio() {
        return Config.ELECTRIC_FE_RATIO.get();
    }

    private static int maxInFe() {
        return Config.ELECTRIC_FE_INPUT_MAX.get() * ratio();
    }

    public Direction front() {
        return getBlockState().getValue(EnergyAdapterBlock.FACING);
    }

    /** The handler other mods see on the front face. */
    public EnergyHandler feHandler() {
        return buffer;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EnergyAdapterBlockEntity adapter) {
        adapter.lastOut = adapter.fedOut;
        adapter.lastIn = adapter.tookIn;
        adapter.fedOut = adapter.tookIn = 0;
        adapter.buffer.refresh();
        adapter.outRequest = adapter.measureOut(level);
    }

    /** Asks the FE block in front what it would take, without taking it. */
    private double measureOut(Level level) {
        Direction front = front();
        EnergyHandler target = level.getCapability(Capabilities.Energy.BLOCK, worldPosition.relative(front), front.getOpposite());
        if (target == null || target == buffer) return 0;
        int offer = (int) (tier().maxPower() * ratio());
        try (Transaction simulation = Transaction.openRoot()) {
            return target.insert(offer, simulation) / (double) ratio();
        }
    }

    /** "Energy adapter LV: 32 J/t out (128 FE/t), 0 J/t in". */
    public Component readout() {
        return Component.translatable(StrataIndustria.MOD_ID + ".energy_adapter.readout", tier().label(), ElectricNetworks.power(lastOut),
                ElectricNetworks.power(lastOut * ratio()), ElectricNetworks.power(lastIn), ElectricNetworks.power(lastIn * ratio()));
    }

    // ---------------------------------------------------------------- electric

    @Override
    public ElectricTier tier() {
        return getBlockState().getValue(EnergyAdapterBlock.TIER);
    }

    @Override
    public boolean connectsElectric(Direction side) {
        return side != front();
    }

    /** J/t offered to the network from FE taken in. */
    @Override
    public double maxOutput() {
        return Math.min(Config.ELECTRIC_FE_INPUT_MAX.get(), buffer.getAmountAsLong() / (double) ratio());
    }

    @Override
    public void extract(double amount) {
        buffer.drain((int) Math.ceil(amount * ratio() - 1e-9));
        tookIn += amount;
    }

    @Override
    public double request() {
        return outRequest;
    }

    @Override
    public void receive(double amount) {
        if (amount <= 0) return;
        Direction front = front();
        EnergyHandler target = level.getCapability(Capabilities.Energy.BLOCK, worldPosition.relative(front), front.getOpposite());
        if (target == null) return;
        carry += amount * ratio();
        int fe = (int) carry;
        if (fe <= 0) return;
        try (Transaction transaction = Transaction.openRoot()) {
            int accepted = target.insert(fe, transaction);
            transaction.commit();
            carry -= fe;
            fedOut += accepted / (double) ratio();
        }
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

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        buffer.deserialize(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        buffer.serialize(output);
    }
}
