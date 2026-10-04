package dev.strataindustria.electric;

import dev.strataindustria.power.ElectricConductor;
import dev.strataindustria.power.ElectricNetworks;
import dev.strataindustria.power.ElectricTier;
import dev.strataindustria.registry.Tier5BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A cable's place in its network (spec 16.1). It holds nothing and never ticks; it only lets the
 * network find the cable and its tier, and marks the network for a rebuild when it comes or goes.
 */
public class CableBlockEntity extends BlockEntity implements ElectricConductor {
    public CableBlockEntity(BlockPos pos, BlockState state) {
        super(Tier5BlockEntities.CABLE.get(), pos, state);
    }

    @Override
    public boolean connectsElectric(Direction side) {
        return true;
    }

    @Override
    public ElectricTier cableTier() {
        return getBlockState().getBlock() instanceof CableBlock cable ? cable.tier() : ElectricTier.LV;
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
