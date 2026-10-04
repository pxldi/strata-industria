package dev.strataindustria.ironworks;

import dev.strataindustria.registry.ModTags;
import dev.strataindustria.registry.Tier4Blocks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The converter's shape (tier 4 spec 12.2): a 3 x 3 x 3 vessel. The bottom is refractory casing with the
 * controller and one or two tuyeres on its edges; the middle layer is casing around the open vessel with
 * the tap hatch on one edge; the top is any refractory block with the charging hatch in the middle.
 */
public final class ConverterStructure {
    public static final int HEIGHT = 3;

    private ConverterStructure() {}

    public static BlockPos vessel(BlockPos controller, Direction facing) {
        return controller.relative(facing.getOpposite());
    }

    public static BlastFurnaceStructure.Result check(Level level, BlockPos controller, Direction facing) {
        BlockPos centre = vessel(controller, facing);
        // Layer 1: casing, with tuyeres on the edges.
        List<BlastFurnaceStructure.Opening> tuyeres = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos pos = centre.offset(dx, 0, dz);
                if (pos.equals(controller)) continue;
                BlockState state = level.getBlockState(pos);
                boolean edge = (dx == 0) != (dz == 0);
                if (edge && state.is(Tier4Blocks.TUYERE.get())) {
                    tuyeres.add(new BlastFurnaceStructure.Opening(pos, side(dx, dz)));
                } else if (!state.is(Tier4Blocks.REFRACTORY_CASING.get())) {
                    return BlastFurnaceStructure.Result.fail(BlastFurnaceStructure.Problem.NEEDS_CASING, pos);
                }
            }
        if (tuyeres.isEmpty()) {
            return BlastFurnaceStructure.Result.fail(BlastFurnaceStructure.Problem.NEEDS_TUYERE, BlastFurnaceStructure.freeEdge(level, centre, controller));
        }
        // Layer 2: casing around the vessel, with the tap hatch on one edge.
        BlockPos middle = centre.above();
        BlockPos tap = null;
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                BlockPos pos = middle.offset(dx, 0, dz);
                BlockState state = level.getBlockState(pos);
                boolean edge = (dx == 0) != (dz == 0);
                if (edge && tap == null && state.is(Tier4Blocks.TAP_HATCH.get())) tap = pos;
                else if (!state.is(Tier4Blocks.REFRACTORY_CASING.get())) return BlastFurnaceStructure.Result.fail(BlastFurnaceStructure.Problem.NEEDS_CASING, pos);
            }
        if (tap == null) return BlastFurnaceStructure.Result.fail(BlastFurnaceStructure.Problem.NEEDS_TAP_HATCH, middle.relative(facing));
        if (!BlastFurnaceStructure.open(level, middle)) return BlastFurnaceStructure.Result.fail(BlastFurnaceStructure.Problem.NEEDS_AIR, middle);
        // Layer 3: any refractory block, with the charging hatch over the vessel.
        BlockPos top = centre.above(2);
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                BlockPos pos = top.offset(dx, 0, dz);
                if (!level.getBlockState(pos).is(ModTags.Blocks.REFRACTORY)) {
                    return BlastFurnaceStructure.Result.fail(BlastFurnaceStructure.Problem.NEEDS_REFRACTORY, pos);
                }
            }
        if (!level.getBlockState(top).is(Tier4Blocks.CHARGING_HATCH.get())) {
            return BlastFurnaceStructure.Result.fail(BlastFurnaceStructure.Problem.NEEDS_CHARGING_HATCH, top);
        }
        return new BlastFurnaceStructure.Result(BlastFurnaceStructure.Problem.NONE, BlockPos.ZERO, List.copyOf(tuyeres), tap, top);
    }

    private static Direction side(int dx, int dz) {
        return dx > 0 ? Direction.EAST : dx < 0 ? Direction.WEST : dz > 0 ? Direction.SOUTH : Direction.NORTH;
    }
}
