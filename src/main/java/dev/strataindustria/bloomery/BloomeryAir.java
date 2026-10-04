package dev.strataindustria.bloomery;

import net.minecraft.core.BlockPos;

/** Something that blows air through a wall block into a bloomery chamber: a powered bellows (spec 8.3). */
public interface BloomeryAir {
    /** Whether this blows into {@code wall} right now, turned by a mechanism. */
    boolean blowsInto(BlockPos wall);
}
