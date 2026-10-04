package dev.strataindustria.grid;

import dev.strataindustria.power.ElectricConsumer;
import dev.strataindustria.power.ElectricNetwork;
import dev.strataindustria.power.ElectricNetworks;
import dev.strataindustria.power.ElectricStatus;
import dev.strataindustria.power.ElectricTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** An electric lamp's place on its network: a 1 J/t LV consumer whose light follows what the network gives it. */
public class ElectricLampBlockEntity extends BlockEntity implements ElectricConsumer {
    /** J/t a lamp draws. */
    public static final double DRAW = 1.0;
    /** Levels a lamp loses while the network dips. */
    public static final int DIP = 6;
    /** Dimmest a powered lamp burns. */
    public static final int FLOOR = 3;

    public ElectricLampBlockEntity(BlockPos pos, BlockState state) {
        super(GridBlocks.ELECTRIC_LAMP_ENTITY.get(), pos, state);
    }

    /** The light level a lamp should burn at: full on a fully supplied network, scaled down when short, dipped for a moment. */
    public static int targetLevel(ElectricStatus status, double fraction, boolean dipping) {
        if (status != ElectricStatus.RUNNING && status != ElectricStatus.LOW_POWER || fraction <= 0) return 0;
        int level = Math.max(FLOOR, (int) Math.round(15 * Math.min(1.0, fraction)));
        return dipping ? Math.max(2, level - DIP) : level;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ElectricLampBlockEntity lamp) {
        if (level.getGameTime() % 2 != 0) return;
        ElectricNetwork network = ElectricNetworks.networkAt(level, pos);
        ElectricNetwork.Report report = network == null ? ElectricNetwork.Report.NONE : network.report(pos);
        int target = targetLevel(report.status(), report.fraction(), network != null && network.dipping());
        int now = state.getValue(ElectricLampBlock.LEVEL);
        // The filament takes a moment to catch up, so a dip reads as a flicker rather than a jump.
        int next = now < target ? Math.min(target, now + 3) : Math.max(target, now - 3);
        if (next != now) level.setBlock(pos, state.setValue(ElectricLampBlock.LEVEL, next), Block.UPDATE_CLIENTS);
    }

    @Override
    public ElectricTier tier() {
        return ElectricTier.LV;
    }

    @Override
    public boolean connectsElectric(Direction side) {
        return true;
    }

    @Override
    public double request() {
        return DRAW;
    }

    @Override
    public void receive(double amount) {}

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
