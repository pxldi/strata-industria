package dev.strataindustria.transport.rail;

import dev.strataindustria.transport.outpost.RouteIndex;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The wooden rail (outposts spec 5.1): vanilla rail behaviour (curves, slopes, T-junctions that switch with
 * redstone), plank rails on sleepers. Taking one out cuts every line that ran over it.
 */
public class WoodenRailBlock extends RailBlock {
    public WoodenRailBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        RouteIndex.get(level).cut(level, pos);
    }
}
