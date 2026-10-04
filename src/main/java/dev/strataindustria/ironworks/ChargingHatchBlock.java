package dev.strataindustria.ironworks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The charging hatch (tier 4 spec 12.1): the furnace throat. Hoppers and chutes above it feed the
 * controller's charge slots.
 */
public class ChargingHatchBlock extends BaseEntityBlock {

    public ChargingHatchBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FurnaceHatchBlockEntity(pos, state);
    }
}
