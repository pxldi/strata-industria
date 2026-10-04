package dev.strataindustria.ceramics;

import dev.strataindustria.metal.CrucibleBlockEntity;
import dev.strataindustria.registry.Tier4BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Tier 4 spec 6.1: a fire clay crucible. It holds 600 units and stands 1700 degrees, enough to melt iron. */
public class RefractoryCrucibleBlock extends CrucibleBlock {
    public RefractoryCrucibleBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrucibleBlockEntity(Tier4BlockEntities.REFRACTORY_CRUCIBLE.get(), pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, Tier4BlockEntities.REFRACTORY_CRUCIBLE.get(), CrucibleBlockEntity::serverTick);
    }
}
