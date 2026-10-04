package dev.strataindustria.bloomery;

import dev.strataindustria.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The bloomery's shape (spec 5.1): the controller in front of a one-block chamber, fire bricks around
 * the chamber and under it, and one to three chimney levels of bricks around the chamber column.
 */
public final class BloomeryStructure {
    public static final int MAX_CHIMNEY = 3;

    /** What is wrong at the first bad position. */
    public enum Problem { NONE, NEEDS_BRICK, NEEDS_AIR }

    /**
     * @param chimney complete chimney levels, 0 when the structure is incomplete
     * @param problem what the first bad block should be, with its position in {@code at}
     */
    public record Result(int chimney, Problem problem, BlockPos at) {
        public boolean complete() {
            return problem == Problem.NONE && chimney > 0;
        }

        /** Base level plus chimney levels: what the charge capacity counts (spec 5.2). */
        public int levels() {
            return complete() ? chimney + 1 : 0;
        }
    }

    private BloomeryStructure() {}

    public static BlockPos chamber(BlockPos controller, Direction facing) {
        return controller.relative(facing.getOpposite());
    }

    public static Result check(Level level, BlockPos controller, Direction facing) {
        BlockPos chamber = chamber(controller, facing);
        // Floor and base level.
        if (!brick(level, chamber.below())) return new Result(0, Problem.NEEDS_BRICK, chamber.below());
        if (!open(level, chamber)) return new Result(0, Problem.NEEDS_AIR, chamber);
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (side == facing) continue;
            BlockPos pos = chamber.relative(side);
            if (!brick(level, pos)) return new Result(0, Problem.NEEDS_BRICK, pos);
        }
        // Chimney: level 1 must be whole; further levels count while they are whole.
        int chimney = 0;
        for (int k = 1; k <= MAX_CHIMNEY; k++) {
            BlockPos core = chamber.above(k);
            BlockPos missing = null;
            if (!open(level, core)) missing = core;
            for (Direction side : Direction.Plane.HORIZONTAL) {
                if (missing != null) break;
                if (!brick(level, core.relative(side))) missing = core.relative(side);
            }
            if (missing != null) {
                if (k == 1) return new Result(0, open(level, core) ? Problem.NEEDS_BRICK : Problem.NEEDS_AIR, missing);
                break;
            }
            chimney = k;
        }
        BlockPos top = chamber.above(chimney + 1);
        if (blocksDraught(level, top)) return new Result(0, Problem.NEEDS_AIR, top);
        return new Result(chimney, Problem.NONE, BlockPos.ZERO);
    }

    private static boolean brick(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(ModTags.Blocks.REFRACTORY);
    }

    /** The chamber column is open space; item entities dropped in fall down it. */
    private static boolean open(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.getCollisionShape(level, pos).isEmpty() && state.getFluidState().isEmpty();
    }

    private static boolean blocksDraught(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isCollisionShapeFullBlock(level, pos) || !state.getFluidState().isEmpty();
    }
}
