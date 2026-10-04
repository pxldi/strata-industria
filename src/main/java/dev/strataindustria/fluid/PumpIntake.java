package dev.strataindustria.fluid;

import dev.strataindustria.Config;
import dev.strataindustria.registry.Tier5Fluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

/**
 * What the mechanical and electric pumps draw from the block in front of them (tier 4 spec 9.3, tier 5 spec
 * 3 and 10.10): a water source, which is brine in an ocean or beach biome, and which refills itself when two
 * horizontal neighbours are sources too.
 */
public final class PumpIntake {
    private PumpIntake() {}

    /** The fluid a pump at {@code intake} would draw, or null when it is not a water source. */
    public static @Nullable Fluid fluidAt(Level level, BlockPos intake) {
        FluidState water = level.getFluidState(intake);
        if (!water.is(FluidTags.WATER) || !water.isSource()) return null;
        if (Config.PUMPS_OCEAN_BRINE.getAsBoolean()) {
            var biome = level.getBiome(intake);
            if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_BEACH)) return Tier5Fluids.BRINE.source().get();
        }
        return Fluids.WATER;
    }

    /** Vanilla's infinite water: at least two horizontal neighbours are water sources too. */
    public static boolean infinite(Level level, BlockPos intake) {
        int sources = 0;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            FluidState next = level.getFluidState(intake.relative(side));
            if (next.is(FluidTags.WATER) && next.isSource()) sources++;
        }
        return sources >= 2;
    }
}
