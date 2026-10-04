package dev.strataindustria.ledger;

import dev.strataindustria.coking.CokeOvenStructure;
import dev.strataindustria.ironworks.BlastFurnaceStructure;
import dev.strataindustria.ironworks.ConverterStructure;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.Tier4Blocks;
import dev.strataindustria.steam.BoilerFluidPortBlock;
import dev.strataindustria.steam.SteelBoilerStructure;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The multiblocks the builder's ledger knows: for each, its controller, how to tell it is whole and the blocks
 * of one standard build, bottom layer first. The ledger places the standard build, the one the specs
 * describe; a hand-built variant (a second tuyere, a taller boiler) still counts as the same entry.
 */
public enum Plan {
    COKE_OVEN, BLAST_FURNACE, CONVERTER, STEEL_BOILER;

    /** A block of a build and where it goes. */
    public record Part(BlockPos pos, BlockState state) {}

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Block controller() {
        return switch (this) {
            case COKE_OVEN -> Tier4Blocks.COKE_OVEN_DOOR.get();
            case BLAST_FURNACE -> Tier4Blocks.BLAST_FURNACE_CONTROLLER.get();
            case CONVERTER -> Tier4Blocks.CONVERTER_CONTROLLER.get();
            case STEEL_BOILER -> Tier4Blocks.BOILER_CONTROLLER.get();
        };
    }

    public static @Nullable Plan of(BlockState state) {
        for (Plan plan : values()) if (state.is(plan.controller())) return plan;
        return null;
    }

    public static @Nullable Plan byId(String id) {
        for (Plan plan : values()) if (plan.id().equals(id)) return plan;
        return null;
    }

    public static Direction facing(BlockState state) {
        return state.getValue(HorizontalDirectionalBlock.FACING);
    }

    /** Whether the multiblock around this controller is whole right now. */
    public boolean complete(Level level, BlockPos controller, Direction facing) {
        return switch (this) {
            case COKE_OVEN -> CokeOvenStructure.check(level, controller, facing).complete();
            case BLAST_FURNACE -> BlastFurnaceStructure.check(level, controller, facing).complete();
            case CONVERTER -> ConverterStructure.check(level, controller, facing).complete();
            case STEEL_BOILER -> SteelBoilerStructure.check(level, controller, facing).complete();
        };
    }

    /** The blocks of the standard build around a controller, lowest layer first. The controller itself is not in it. */
    public List<Part> parts(BlockPos controller, Direction facing) {
        List<Part> parts = new ArrayList<>();
        BlockPos centre = controller.relative(facing.getOpposite());
        switch (this) {
            case COKE_OVEN -> {
                BlockState bricks = Tier4Blocks.COKE_OVEN_BRICKS.get().defaultBlockState();
                for (int dy = -1; dy <= 1; dy++) cube(parts, centre.above(dy), controller, bricks, dy == 0);
            }
            case BLAST_FURNACE -> {
                hearth(parts, centre, controller, facing, true);
                ring(parts, centre.above(), Tier4Blocks.REFRACTORY_CASING.get().defaultBlockState());
                for (int y = 2; y <= 4; y++) ring(parts, centre.above(y), ModBlocks.FIRE_BRICKS.get().defaultBlockState());
                parts.add(new Part(centre.above(4), Tier4Blocks.CHARGING_HATCH.get().defaultBlockState()));
            }
            case CONVERTER -> {
                hearth(parts, centre, controller, facing, false);
                BlockPos middle = centre.above();
                ring(parts, middle, Tier4Blocks.REFRACTORY_CASING.get().defaultBlockState());
                BlockPos tap = middle.relative(facing.getOpposite());
                parts.removeIf(p -> p.pos().equals(tap));
                parts.add(new Part(tap, tapHatch(facing.getOpposite())));
                BlockPos top = centre.above(2);
                ring(parts, top, ModBlocks.FIRE_BRICKS.get().defaultBlockState());
                parts.add(new Part(top, Tier4Blocks.CHARGING_HATCH.get().defaultBlockState()));
            }
            case STEEL_BOILER -> {
                BlockState firebox = Tier4Blocks.FIREBOX.get().defaultBlockState();
                for (int dx = -1; dx <= 1; dx++)
                    for (int dz = -1; dz <= 1; dz++) parts.add(new Part(centre.offset(dx, -1, dz), firebox));
                BlockState shell = Tier4Blocks.STEEL_BOILER_SHELL.get().defaultBlockState();
                BlockPos water = centre.above().relative(facing.getOpposite());
                BlockPos steam = centre.above().relative(facing.getClockWise());
                for (int layer = 0; layer < SteelBoilerStructure.MIN_LAYERS; layer++)
                    for (int dx = -1; dx <= 1; dx++)
                        for (int dz = -1; dz <= 1; dz++) {
                            BlockPos pos = centre.offset(dx, layer, dz);
                            if (pos.equals(controller)) continue;
                            BlockState state = shell;
                            if (pos.equals(water)) state = port(BoilerFluidPortBlock.Mode.WATER);
                            else if (pos.equals(steam)) state = port(BoilerFluidPortBlock.Mode.STEAM);
                            parts.add(new Part(pos, state));
                        }
            }
        }
        return parts;
    }

    private static BlockState port(BoilerFluidPortBlock.Mode mode) {
        return Tier4Blocks.BOILER_FLUID_PORT.get().defaultBlockState().setValue(BoilerFluidPortBlock.MODE, mode);
    }

    private static BlockState tapHatch(Direction out) {
        return Tier4Blocks.TAP_HATCH.get().defaultBlockState().setValue(dev.strataindustria.ironworks.TapHatchBlock.FACING, out);
    }

    /** A 3 x 3 layer of {@code state} around {@code middle}, the middle itself left out when {@code hollow}, the controller never placed. */
    private static void cube(List<Part> parts, BlockPos middle, @Nullable BlockPos controller, BlockState state, boolean hollow) {
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) {
                if (hollow && dx == 0 && dz == 0) continue;
                BlockPos pos = middle.offset(dx, 0, dz);
                if (!pos.equals(controller)) parts.add(new Part(pos, state));
            }
    }

    /** The eight blocks around the shaft at one height. */
    private static void ring(List<Part> parts, BlockPos middle, BlockState state) {
        cube(parts, middle, null, state, true);
    }

    /** A blast furnace or converter hearth: casing everywhere, a tuyere on the left edge, and for the furnace a tap hatch behind (the converter's is higher up). */
    private static void hearth(List<Part> parts, BlockPos centre, BlockPos controller, Direction facing, boolean tap) {
        BlockState casing = Tier4Blocks.REFRACTORY_CASING.get().defaultBlockState();
        BlockPos tuyere = centre.relative(facing.getClockWise());
        BlockPos tapAt = centre.relative(facing.getOpposite());
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos pos = centre.offset(dx, 0, dz);
                if (pos.equals(controller)) continue;
                BlockState state = casing;
                if (pos.equals(tuyere)) {
                    state = Tier4Blocks.TUYERE.get().defaultBlockState().setValue(dev.strataindustria.ironworks.FurnacePartBlock.FACING, facing.getClockWise());
                } else if (tap && pos.equals(tapAt)) {
                    state = tapHatch(facing.getOpposite());
                }
                parts.add(new Part(pos, state));
            }
    }
}
