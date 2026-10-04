package dev.strataindustria.electric;

import dev.strataindustria.power.ElectricConductor;
import dev.strataindustria.power.ElectricNetwork;
import dev.strataindustria.power.ElectricNetworks;
import dev.strataindustria.power.ElectricTier;
import dev.strataindustria.registry.Tier5BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A pole block's place in its network (spec 8.3): MV rated, lossless. A pole column conducts between its
 * insulators and its bottom block, so only the bottom block joins what stands beside or under it.
 */
public class UtilityPoleBlockEntity extends BlockEntity implements ElectricConductor {
    public UtilityPoleBlockEntity(BlockPos pos, BlockState state) {
        super(Tier5BlockEntities.UTILITY_POLE.get(), pos, state);
    }

    @Override
    public boolean connectsElectric(Direction side) {
        if (level == null) return false;
        var neighbour = level.getBlockState(worldPosition.relative(side)).getBlock();
        if (neighbour instanceof PoleInsulatorBlock || neighbour instanceof dev.strataindustria.transport.rail.TrolleyBracketBlock) return true;
        if (side.getAxis() == Direction.Axis.Y && neighbour instanceof UtilityPoleBlock) return true;
        // Only the bottom block of a column reaches sideways and downwards.
        return !(level.getBlockState(worldPosition.below()).getBlock() instanceof UtilityPoleBlock) && side != Direction.UP;
    }

    @Override
    public ElectricTier cableTier() {
        return ElectricTier.MV;
    }

    @Override
    public double blockLoss() {
        return 0;
    }

    @Override
    public int capacity() {
        return ElectricNetwork.SPAN_CAPACITY;
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
