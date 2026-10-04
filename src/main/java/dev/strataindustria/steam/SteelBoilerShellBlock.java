package dev.strataindustria.steam;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A steel boiler shell block (tier 4 spec 10.3). Once its boiler is built it passes the heat of the
 * firebox under it on to the controller.
 */
public class SteelBoilerShellBlock extends BaseEntityBlock {
    public SteelBoilerShellBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BoilerPartBlockEntity(pos, state);
    }
}
