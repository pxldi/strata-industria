package dev.strataindustria.ironworks;

import dev.strataindustria.registry.ModTags;
import dev.strataindustria.registry.Tier4Blocks;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The blast furnace's shape (tier 4 spec 12.1): a 3 x 3 hearth of refractory casing with the controller,
 * a tap hatch and one or two tuyeres on its edges; a casing bosh; two layers of any refractory block
 * around the open shaft; and a refractory throat with the charging hatch over the shaft. Heat inlets may
 * stand in for casing in the hearth and the bosh (spec 8.4).
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
        BlockPos centre = hearth(controller, facing);
        // Layer 1, the hearth: casing at the corners and the middle; tuyeres and the tap on the edges.
        List<Opening> tuyeres = new ArrayList<>();
        List<BlockPos> inlets = new ArrayList<>();
        BlockPos tap = null;
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos pos = centre.offset(dx, 0, dz);
                if (pos.equals(controller)) continue;
                BlockState state = level.getBlockState(pos);
                boolean edge = (dx == 0) != (dz == 0);
                if (edge && state.is(Tier4Blocks.TUYERE.get())) {
                    tuyeres.add(new Opening(pos, dx > 0 ? Direction.EAST : dx < 0 ? Direction.WEST : dz > 0 ? Direction.SOUTH : Direction.NORTH));
                } else if (edge && state.is(Tier4Blocks.TAP_HATCH.get()) && tap == null) {
                    tap = pos;
                } else if (!casing(state, pos, inlets)) {
                    return Result.fail(Problem.NEEDS_CASING, pos);
                }
            }
        if (tap == null) return Result.fail(Problem.NEEDS_TAP_HATCH, freeEdge(level, centre, controller));
        if (tuyeres.isEmpty()) return Result.fail(Problem.NEEDS_TUYERE, freeEdge(level, centre, controller));
        // Layer 2, the bosh: casing around the shaft.
        Result ring = ring(level, centre.above(), Problem.NEEDS_CASING, inlets);
        if (ring != null) return ring;
        // Layers 3 and 4, the stack, and layer 5, the throat: any refractory block.
        for (int y = 2; y <= 4; y++) {
            ring = ring(level, centre.above(y), Problem.NEEDS_REFRACTORY, inlets);
            if (ring != null) return ring;
        }
        for (int y = 1; y <= 3; y++) {
            BlockPos shaft = centre.above(y);
            if (!open(level, shaft)) return Result.fail(Problem.NEEDS_AIR, shaft);
        }
        BlockPos hatch = centre.above(4);
        if (!level.getBlockState(hatch).is(Tier4Blocks.CHARGING_HATCH.get())) return Result.fail(Problem.NEEDS_CHARGING_HATCH, hatch);
        return new Result(Problem.NONE, BlockPos.ZERO, List.copyOf(tuyeres), tap, hatch, List.copyOf(inlets));
    }

    /** The eight blocks around the shaft at one height, or null if they are all right. */
    private static Result ring(Level level, BlockPos middle, Problem missing, List<BlockPos> inlets) {
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                BlockPos pos = middle.offset(dx, 0, dz);
                BlockState state = level.getBlockState(pos);
                boolean ok = missing == Problem.NEEDS_CASING ? casing(state, pos, inlets) : state.is(ModTags.Blocks.REFRACTORY);
                if (!ok) return Result.fail(missing, pos);
            }
        return null;
    }

    /** Refractory casing, or a heat inlet in its place, which is noted in {@code inlets}. */
    static boolean casing(BlockState state, BlockPos pos, List<BlockPos> inlets) {
        if (state.is(Tier4Blocks.HEAT_INLET.get())) {
            inlets.add(pos.immutable());
            return true;
        }
        return state.is(Tier4Blocks.REFRACTORY_CASING.get());
    }

    /** An edge of the hearth that holds plain casing, where a missing tap hatch or tuyere could go. */
    static BlockPos freeEdge(Level level, BlockPos centre, BlockPos controller) {
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos pos = centre.relative(side);
            if (!pos.equals(controller) && level.getBlockState(pos).is(Tier4Blocks.REFRACTORY_CASING.get())) return pos;
        }
        return centre;
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

    static boolean open(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.getCollisionShape(level, pos).isEmpty() && state.getFluidState().isEmpty();
    }
}
