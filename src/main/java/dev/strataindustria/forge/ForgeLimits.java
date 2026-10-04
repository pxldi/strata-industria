package dev.strataindustria.forge;

import net.minecraft.world.item.ItemStack;

/**
 * How hot a forge lets an item get: 10 °C under its melting point (spec 4.5), so a mistake in the
 * forge is never lost metal. Items with no melting point have no limit.
 */
public final class ForgeLimits {
    public static final float MARGIN = 10.0f;

    private ForgeLimits() {}

    public static float maxFor(ItemStack stack) {
        return MeltingPoints.of(stack).map(melt -> melt - MARGIN).orElse(Float.MAX_VALUE);
    }

    public static boolean atLimit(ItemStack stack, float temperature) {
        return MeltingPoints.of(stack).map(melt -> temperature >= melt - MARGIN - 0.5f).orElse(false);
    }
}
