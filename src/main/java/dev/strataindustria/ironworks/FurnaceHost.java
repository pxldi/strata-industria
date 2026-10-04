package dev.strataindustria.ironworks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.WorldlyContainer;

/**
 * A furnace controller whose hatches stand in for it (tier 4 spec 12): the charging hatch takes burden
 * into its charge slots and the tap hatch gives up what it has made.
 */
public interface FurnaceHost extends WorldlyContainer {
    int[] chargeSlots();

    int[] tapSlots();

    /** Whether the furnace is built and counts the block at {@code part} as one of its hatches. */
    boolean usesPart(BlockPos part);
}
