package dev.strataindustria.fluid;

import net.minecraft.core.Direction;
import net.minecraft.world.level.material.Fluid;

/**
 * A block entity that fluid pipes connect to (tier 4 spec 9.2): boilers, engines, pumps. Pipes are
 * passive; sources push through them into ports that accept.
 */
public interface FluidPort {
    /** Whether a pipe on {@code side} of this block joins it. */
    boolean connectsFluid(Direction side);

    /**
     * Takes up to {@code amount} mB of {@code fluid} in through {@code side}, at {@code pressure} bar
     * (0 for anything but steam), and returns how much it took, or would take when simulating.
     */
    int fill(Direction side, Fluid fluid, int amount, float pressure, boolean simulate);
}
