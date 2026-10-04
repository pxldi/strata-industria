package dev.strataindustria.ironworks;

import dev.strataindustria.multiblock.Multiblocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * The converter's shape (tier 4 spec 12.2): a 3 x 3 x 3 vessel. The bottom is refractory casing with the
 * controller and one or two tuyeres on its edges; the middle layer is casing around the open vessel with
 * the tap hatch on one edge; the top is any refractory block with the charging hatch in the middle. Heat
 * inlets may stand in for casing in the bottom layer (spec 8.4). The pattern is data, {@code multiblock/converter.json}.
 */
public final class ConverterStructure {
    public static final int HEIGHT = 3;

    private ConverterStructure() {}

    public static BlockPos vessel(BlockPos controller, Direction facing) {
        return controller.relative(facing.getOpposite());
    }

    public static BlastFurnaceStructure.Result check(Level level, BlockPos controller, Direction facing) {
        return BlastFurnaceStructure.result(Multiblocks.check(level, Multiblocks.CONVERTER, controller, facing), vessel(controller, facing));
    }
}
