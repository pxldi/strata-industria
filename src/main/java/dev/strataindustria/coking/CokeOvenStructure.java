package dev.strataindustria.coking;

import dev.strataindustria.registry.Tier4Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The coke oven's shape (tier 4 spec 5.1): a 3 x 3 x 3 cube of coke oven bricks around a one-block
 * chamber, with the door in the middle of the front face.
 */
public final class CokeOvenStructure {
    public enum Problem { NONE, NEEDS_BRICK, NEEDS_AIR }

    public record Result(Problem problem, BlockPos at) {
        public boolean complete() {
            return problem == Problem.NONE;
        }
    }

    public static final Result INCOMPLETE = new Result(Problem.NEEDS_BRICK, BlockPos.ZERO);

    private CokeOvenStructure() {}

    public static BlockPos chamber(BlockPos door, Direction facing) {
        return door.relative(facing.getOpposite());
    }

    public static Result check(Level level, BlockPos door, Direction facing) {
        BlockPos chamber = chamber(door, facing);
        // Bottom layer first, so the message points at what a builder places next.
        for (int dy = -1; dy <= 1; dy++)
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos pos = chamber.offset(dx, dy, dz);
                    if (pos.equals(door)) continue;
                    if (pos.equals(chamber)) {
                        if (!open(level, pos)) return new Result(Problem.NEEDS_AIR, pos);
                    } else if (!level.getBlockState(pos).is(Tier4Blocks.COKE_OVEN_BRICKS.get())) {
                        return new Result(Problem.NEEDS_BRICK, pos);
                    }
                }
        return new Result(Problem.NONE, BlockPos.ZERO);
    }

    private static boolean open(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.getCollisionShape(level, pos).isEmpty() && state.getFluidState().isEmpty();
    }
}
