package dev.strataindustria.heat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The heat inlet (tier 4 spec 8.4): stands in for a casing block in a multiblock and passes the heat of
 * the pipes or the firebox it touches to the controller.
 */
public class HeatInletBlock extends BaseEntityBlock {

    public HeatInletBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HeatInletBlockEntity(pos, state);
    }
}
