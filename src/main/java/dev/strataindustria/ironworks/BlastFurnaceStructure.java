package dev.strataindustria.ironworks;

import dev.strataindustria.multiblock.Multiblock;
import dev.strataindustria.multiblock.Multiblocks;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * The blast furnace's shape (tier 4 spec 12.1): a 3 x 3 hearth of refractory casing with the controller,
 * a tap hatch and one or two tuyeres on its edges; a casing bosh; two layers of any refractory block
 * around the open shaft; and a refractory throat with the charging hatch over the shaft. Heat inlets may
 * stand in for casing in the hearth and the bosh (spec 8.4). The pattern is data, {@code multiblock/blast_furnace.json}.
 */
public final class BlastFurnaceStructure {
    public static final int HEIGHT = 5;

    public enum Problem {
        NONE, NEEDS_CASING, NEEDS_REFRACTORY, NEEDS_AIR, NEEDS_CHARGING_HATCH, NEEDS_TAP_HATCH, NEEDS_TUYERE;

        public String key() {
            return dev.strataindustria.StrataIndustria.MOD_ID + ".blast_furnace.problem." + name().toLowerCase(Locale.ROOT);
        }
    }

    /**
     * What a check found: the first problem and where, and on success the hatches, the tuyeres with the
     * side each faces out of the furnace, and the heat inlets.
     */
    public record Result(Problem problem, BlockPos at, List<Opening> tuyeres, BlockPos tap, BlockPos hatch, List<BlockPos> inlets) {
        public boolean complete() {
            return problem == Problem.NONE;
        }

        static Result fail(Problem problem, BlockPos at) {
            return new Result(problem, at, List.of(), BlockPos.ZERO, BlockPos.ZERO, List.of());
        }
    }

    /** A block in the hearth wall and the side of it that faces out. */
    public record Opening(BlockPos pos, Direction out) {}

    public static final Result INCOMPLETE = Result.fail(Problem.NEEDS_CASING, BlockPos.ZERO);

    private BlastFurnaceStructure() {}

    /** The middle of the hearth, behind the controller. */
    public static BlockPos hearth(BlockPos controller, Direction facing) {
        return controller.relative(facing.getOpposite());
    }

    public static Result check(Level level, BlockPos controller, Direction facing) {
        return result(Multiblocks.check(level, Multiblocks.BLAST_FURNACE, controller, facing), hearth(controller, facing));
    }

    /** Reads a hearth-style match (blast furnace or converter) into the result the machines use. */
    static Result result(Multiblock.Match match, BlockPos centre) {
        if (!match.complete()) return Result.fail(Problem.valueOf(match.problem().toUpperCase(Locale.ROOT)), match.at());
        List<Opening> tuyeres = match.role("tuyere").stream()
                .sorted(Comparator.<BlockPos>comparingInt(pos -> pos.getX()).thenComparingInt(pos -> pos.getZ()))
                .map(pos -> new Opening(pos, outward(pos, centre)))
                .toList();
        return new Result(Problem.NONE, BlockPos.ZERO, tuyeres, match.first("tap"), match.first("hatch"), List.copyOf(match.role("inlet")));
    }

    private static Direction outward(BlockPos pos, BlockPos centre) {
        int dx = pos.getX() - centre.getX(), dz = pos.getZ() - centre.getZ();
        return dx > 0 ? Direction.EAST : dx < 0 ? Direction.WEST : dz > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    /**
     * Where a missing block is, for a screen: layer (from 0) times 9, plus row (front, middle, back) times
     * 3, plus column (left, centre, right) as seen standing at the controller.
     */
    public static int where(BlockPos controller, Direction facing, BlockPos at, int height) {
        Direction back = facing.getOpposite();
        Direction left = back.getCounterClockWise();
        BlockPos rel = at.subtract(controller);
        int depth = rel.getX() * back.getStepX() + rel.getZ() * back.getStepZ();
        int side = rel.getX() * left.getStepX() + rel.getZ() * left.getStepZ();
        int layer = Math.clamp(rel.getY(), 0, height - 1);
        return layer * 9 + Math.clamp(depth, 0, 2) * 3 + (1 - Math.clamp(side, -1, 1));
    }
}
