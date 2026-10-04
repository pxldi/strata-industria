package dev.strataindustria.steam;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.ironworks.BlastFurnaceStructure;
import dev.strataindustria.registry.Tier4Blocks;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The steel boiler's shape (tier 4 spec 10.3): a 3 x 3 fire layer of fireboxes and heat inlets, then two to
 * four full 3 x 3 layers of steel boiler shell. The controller stands at the front centre of the lowest
 * shell layer, and somewhere in the shell there is at least one water port and one steam port.
 */
public final class SteelBoilerStructure {
    public static final int MIN_LAYERS = 2, MAX_LAYERS = 4;

    public enum Problem {
        NONE, NEEDS_FIRE, NEEDS_SHELL, NEEDS_WATER_PORT, NEEDS_STEAM_PORT;

        public String key() {
            return StrataIndustria.MOD_ID + ".boiler.problem." + name().toLowerCase(Locale.ROOT);
        }
    }

    /**
     * What a check found: the first problem and where, and on success the number of shell layers, the shell
     * blocks and ports (everything that forwards to the controller), the steam ports and the heat inlets.
     */
    public record Result(Problem problem, BlockPos at, int layers, List<BlockPos> parts, List<BlockPos> steamPorts, List<BlockPos> inlets) {
        public boolean complete() {
            return problem == Problem.NONE;
        }

        static Result fail(Problem problem, BlockPos at) {
            return new Result(problem, at, 0, List.of(), List.of(), List.of());
        }

        /** Whether a block is inside the boiler, fire layer included. */
        public boolean contains(BlockPos pos, BlockPos controller, Direction facing) {
            BlockPos centre = centre(controller, facing);
            return Math.abs(pos.getX() - centre.getX()) <= 1 && Math.abs(pos.getZ() - centre.getZ()) <= 1
                    && pos.getY() >= controller.getY() - 1 && pos.getY() < controller.getY() + layers;
        }
    }

    public static final Result INCOMPLETE = Result.fail(Problem.NEEDS_FIRE, BlockPos.ZERO);

    private SteelBoilerStructure() {}

    /** The middle of the lowest shell layer, behind the controller. */
    public static BlockPos centre(BlockPos controller, Direction facing) {
        return controller.relative(facing.getOpposite());
    }

    public static Result check(Level level, BlockPos controller, Direction facing) {
        BlockPos centre = centre(controller, facing);
        List<BlockPos> inlets = new ArrayList<>();
        // Layer 1: the fire.
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos pos = centre.offset(dx, -1, dz);
                BlockState state = level.getBlockState(pos);
                if (state.is(Tier4Blocks.HEAT_INLET.get())) inlets.add(pos);
                else if (!state.is(Tier4Blocks.FIREBOX.get())) return Result.fail(Problem.NEEDS_FIRE, pos);
            }
        // Shell layers: the first two must be whole; up to two more count if they are started at all.
        List<BlockPos> parts = new ArrayList<>();
        List<BlockPos> steam = new ArrayList<>();
        boolean water = false;
        int layers = 0;
        for (int layer = 0; layer < MAX_LAYERS; layer++) {
            List<BlockPos> found = new ArrayList<>();
            BlockPos missing = null;
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos pos = centre.offset(dx, layer, dz);
                    if (pos.equals(controller)) continue;
                    BlockState state = level.getBlockState(pos);
                    if (state.is(Tier4Blocks.STEEL_BOILER_SHELL.get()) || state.is(Tier4Blocks.BOILER_FLUID_PORT.get())) found.add(pos);
                    else if (missing == null) missing = pos;
                }
            if (layer >= MIN_LAYERS && found.isEmpty()) break;
            if (missing != null) return Result.fail(Problem.NEEDS_SHELL, missing);
            for (BlockPos pos : found) {
                BlockState state = level.getBlockState(pos);
                if (state.is(Tier4Blocks.BOILER_FLUID_PORT.get())) {
                    if (state.getValue(BoilerFluidPortBlock.MODE) == BoilerFluidPortBlock.Mode.STEAM) steam.add(pos);
                    else water = true;
                }
            }
            parts.addAll(found);
            layers++;
        }
        if (!water) return Result.fail(Problem.NEEDS_WATER_PORT, centre);
        if (steam.isEmpty()) return Result.fail(Problem.NEEDS_STEAM_PORT, centre);
        return new Result(Problem.NONE, BlockPos.ZERO, layers, List.copyOf(parts), List.copyOf(steam), List.copyOf(inlets));
    }

    /**
     * Where a missing block is, for the screen: layer (0 for the fire) times 9, plus the spot in the layer
     * as {@link BlastFurnaceStructure#where} counts it.
     */
    public static int where(BlockPos controller, Direction facing, BlockPos at) {
        int layer = Math.clamp(at.getY() - controller.getY() + 1, 0, MAX_LAYERS);
        int spot = BlastFurnaceStructure.where(controller, facing, at.atY(controller.getY()), 1) % 9;
        return layer * 9 + spot;
    }
}
