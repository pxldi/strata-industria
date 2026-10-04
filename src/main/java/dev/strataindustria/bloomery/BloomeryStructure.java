package dev.strataindustria.bloomery;

import dev.strataindustria.multiblock.Multiblock;
import dev.strataindustria.multiblock.Multiblocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * The bloomery's shape (spec 5.1): the controller in front of a one-block chamber, fire bricks around
 * the chamber and under it, and one to three chimney levels of bricks around the chamber column. The pattern
 * is data, {@code multiblock/bloomery.json}.
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

    /** Index of the repeating chimney layer in the pattern. */
    private static final int CHIMNEY_LAYER = 2;

    public static Result check(Level level, BlockPos controller, Direction facing) {
        Multiblock.Match match = Multiblocks.check(level, Multiblocks.BLOOMERY, controller, facing);
        if (!match.complete()) {
            return new Result(0, "needs_air".equals(match.problem()) ? Problem.NEEDS_AIR : Problem.NEEDS_BRICK, match.at());
        }
        return new Result(match.repeats()[CHIMNEY_LAYER], Problem.NONE, BlockPos.ZERO);
    }
}
