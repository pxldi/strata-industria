package dev.strataindustria.steam;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.ironworks.BlastFurnaceStructure;
import dev.strataindustria.multiblock.Multiblock;
import dev.strataindustria.multiblock.Multiblocks;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * The steel boiler's shape (tier 4 spec 10.3): a 3 x 3 fire layer of fireboxes and heat inlets, then two to
 * four full 3 x 3 layers of steel boiler shell. The controller stands at the front centre of the lowest
 * shell layer, and somewhere in the shell there is at least one water port and one steam port. The pattern is
 * data, {@code multiblock/steel_boiler.json}.
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

    /** Index of the optional shell layers in the pattern; the fire, the controller's layer and the second shell layer come first. */
    private static final int OPTIONAL_LAYER = 3;

    public static Result check(Level level, BlockPos controller, Direction facing) {
        Multiblock.Match match = Multiblocks.check(level, Multiblocks.STEEL_BOILER, controller, facing);
        if (!match.complete()) return Result.fail(Problem.valueOf(match.problem().toUpperCase(Locale.ROOT)), match.at());
        return new Result(Problem.NONE, BlockPos.ZERO, MIN_LAYERS + match.repeats()[OPTIONAL_LAYER], List.copyOf(match.role("part")),
                List.copyOf(match.role("steam_port")), List.copyOf(match.role("inlet")));
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
