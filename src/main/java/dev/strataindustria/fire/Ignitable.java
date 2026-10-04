package dev.strataindustria.fire;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block that a firestarter, torch or flint and steel can light: the fire pit now, later the pit
 * kiln, log pile and forge.
 */
public interface Ignitable {
    /** Whether the block could be lit right now: unlit, fuelled and not rained on. */
    boolean canIgnite(Level level, BlockPos pos, BlockState state);

    /** Lights the block. Returns whether it caught. Only called on the server. */
    boolean ignite(Level level, BlockPos pos, BlockState state);

    /** True when rain falls on the block, which keeps it from catching. */
    static boolean rainedOn(Level level, BlockPos pos) {
        return level.isRainingAt(pos.above());
    }
}
