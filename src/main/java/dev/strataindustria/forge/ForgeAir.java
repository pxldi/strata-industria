package dev.strataindustria.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/** Something that blows air into a forge: a bellows, pumped by hand or turned by a shaft (spec 8.3). */
public interface ForgeAir {
    /** Whether this blows into the forge at {@code forge} right now. */
    boolean blowsInto(BlockPos forge);

    /** Whether any bellows beside the forge blows into it; more than one adds nothing. */
    static boolean blown(Level level, BlockPos forge) {
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (level.getBlockEntity(forge.relative(side)) instanceof ForgeAir air && air.blowsInto(forge)) return true;
        }
        return false;
    }
}
