package dev.strataindustria.fire;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * A block that flint struck on a rock, a torch or flint and steel can light: the fire pit now, later the pit
 * kiln, log pile and forge.
 */
public interface Ignitable {
    /** Whether the block could be lit right now: unlit, fuelled and not rained on. */
    boolean canIgnite(Level level, BlockPos pos, BlockState state);

    /** Lights the block. Returns whether it caught. Only called on the server. */
    boolean ignite(Level level, BlockPos pos, BlockState state);

    /**
     * {@link #canIgnite} on the server. The client never sees the fuel inside a fire pit or forge, so there
     * it only asks whether the block is unlit and leaves the real check to the server.
     */
    static boolean mayIgnite(Level level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof Ignitable target)) return false;
        if (!level.isClientSide()) return target.canIgnite(level, pos, state);
        return !state.hasProperty(BlockStateProperties.LIT) || !state.getValue(BlockStateProperties.LIT);
    }

    /** True when rain falls on the block, which keeps it from catching. */
    static boolean rainedOn(Level level, BlockPos pos) {
        return level.isRainingAt(pos.above());
    }
}
