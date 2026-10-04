package dev.strataindustria.heat;

import net.minecraft.core.BlockPos;

/** A multiblock that takes heat through its heat inlets (tier 4 spec 8.4). */
public interface InletHost extends HeatConsumer {
    /** Whether {@code inlet} is part of the structure as last checked. */
    boolean usesInlet(BlockPos inlet);
}
