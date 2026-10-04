package dev.strataindustria.ledger;

import dev.strataindustria.multiblock.Multiblock;
import dev.strataindustria.multiblock.Multiblocks;
import dev.strataindustria.registry.Tier4Blocks;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The multiblocks the builder's ledger knows: for each, its controller, how to tell it is whole and the blocks
 * of one standard build, bottom layer first, all read from the multiblock pattern data. The ledger places the
 * standard build, the one the specs describe; a hand-built variant (a second tuyere, a taller boiler) still counts as the same entry.
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

    /** The pattern this entry reads: the same data the machines check and the ghost preview shows. */
    public ResourceKey<Multiblock> key() {
        return switch (this) {
            case COKE_OVEN -> Multiblocks.COKE_OVEN;
            case BLAST_FURNACE -> Multiblocks.BLAST_FURNACE;
            case CONVERTER -> Multiblocks.CONVERTER;
            case STEEL_BOILER -> Multiblocks.STEEL_BOILER;
        };
    }

    /** Whether the multiblock around this controller is whole right now. */
    public boolean complete(Level level, BlockPos controller, Direction facing) {
        return Multiblocks.check(level, key(), controller, facing).complete();
    }

    /** The blocks of the standard build around a controller, lowest layer first. The controller itself is not in it. */
    public List<Part> parts(Level level, BlockPos controller, Direction facing) {
        List<Part> parts = new ArrayList<>();
        for (Multiblock.Part part : Multiblocks.get(level, key()).parts(controller, facing)) parts.add(new Part(part.pos(), part.state()));
        return parts;
    }
}
