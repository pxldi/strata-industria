package dev.strataindustria.power;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/** A block whose block entity is kinetic; it says which faces pass rotation. */
public interface KineticBlock {
    boolean connects(BlockState state, Direction side);
}
