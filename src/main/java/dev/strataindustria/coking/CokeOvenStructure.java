package dev.strataindustria.coking;

import dev.strataindustria.multiblock.Multiblock;
import dev.strataindustria.multiblock.Multiblocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * The coke oven's shape (tier 4 spec 5.1): a 3 x 3 x 3 cube of coke oven bricks around a one-block
 * chamber, with the door in the middle of the front face. The pattern is data, {@code multiblock/coke_oven.json}.
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
        Multiblock.Match match = Multiblocks.check(level, Multiblocks.COKE_OVEN, door, facing);
        if (match.complete()) return new Result(Problem.NONE, BlockPos.ZERO);
        return new Result("needs_air".equals(match.problem()) ? Problem.NEEDS_AIR : Problem.NEEDS_BRICK, match.at());
    }
}
