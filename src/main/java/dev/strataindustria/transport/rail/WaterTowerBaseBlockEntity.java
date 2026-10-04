package dev.strataindustria.transport.rail;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.fluid.FluidPort;
import dev.strataindustria.fluid.FluidTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/** The water tower's foot as a pipe port: water pushed in at any side but the top goes up into the tank above. Nothing else is taken. */
public class WaterTowerBaseBlockEntity extends BlockEntity implements FluidPort {
    public WaterTowerBaseBlockEntity(BlockPos pos, BlockState state) {
        super(RailwayRegistry.WATER_TOWER_BASE_ENTITY.get(), pos, state);
    }

    private FluidTankBlockEntity tank() {
        return level != null && level.getBlockEntity(worldPosition.above()) instanceof FluidTankBlockEntity tank ? tank : null;
    }

    @Override
    public boolean connectsFluid(Direction side) {
        return side != Direction.UP;
    }

    @Override
    public int fill(Direction side, Fluid fluid, int amount, float pressure, boolean simulate) {
        FluidTankBlockEntity tank = tank();
        if (tank == null || !fluid.isSame(Fluids.WATER) || side == Direction.UP) return 0;
        return tank.fill(Fluids.WATER, amount, 0, simulate);
    }

    /** Water in the tanks above, out of what they hold. */
    public Component readout() {
        FluidTankBlockEntity tank = tank();
        if (tank == null) return WaterTowerBaseBlock.none();
        java.util.List<FluidTankBlockEntity> group = tank.group();
        int total = FluidTankBlockEntity.fluidOf(group).isSame(Fluids.WATER) ? FluidTankBlockEntity.totalOf(group) : 0;
        return Component.translatable(StrataIndustria.MOD_ID + ".water_tower.holds", total, group.size() * FluidTankBlockEntity.CAPACITY);
    }
}
