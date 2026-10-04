package dev.strataindustria.heat;

import dev.strataindustria.registry.ModDataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Item heat (spec 5.2): every change is exponential towards a target, dT/dt = k (T_target - T).
 * Out of a heat source the target is ambient and k is {@link #AMBIENT_RATE}.
 */
public final class Heat {
    public static final float AMBIENT = 20.0f;
    /** Cooling in the inventory, a chest or on the ground, per second. */
    public static final float AMBIENT_RATE = 0.010f;
    /** Quenching in water, per second. */
    public static final float QUENCH_RATE = 0.200f;
    /** Forge heating slots, per second. */
    public static final float FORGE_RATE = 0.040f;
    /** Below this the component is dropped, so cooled stacks stack with cold ones again. */
    static final float FORGET_BELOW = AMBIENT + 5.0f;

    private Heat() {}

    /** The stack's temperature now. */
    public static float get(ItemStack stack, Level level) {
        return get(stack, level.getGameTime());
    }

    public static float get(ItemStack stack, long now) {
        Temperature t = stack.get(ModDataComponents.TEMPERATURE.get());
        if (t == null) return AMBIENT;
        return approach(t.value(), AMBIENT, AMBIENT_RATE, Math.max(0, now - t.updated()));
    }

    public static boolean isHot(ItemStack stack, Level level) {
        return get(stack, level) >= HeatBand.WARMING.from();
    }

    public static void set(ItemStack stack, float value, long now) {
        if (value < FORGET_BELOW) stack.remove(ModDataComponents.TEMPERATURE.get());
        else stack.set(ModDataComponents.TEMPERATURE.get(), new Temperature(value, now));
    }

    /** Moves the stack towards {@code target} at {@code ratePerSecond} for {@code ticks}, capped at {@code max}. */
    public static float heatToward(ItemStack stack, float target, float ratePerSecond, int ticks, float max, long now) {
        float current = get(stack, now);
        float next = Math.min(max, approach(current, target, ratePerSecond, ticks));
        set(stack, next, now);
        return next;
    }

    static float approach(float from, float target, float ratePerSecond, long ticks) {
        return (float) (target + (from - target) * Math.exp(-ratePerSecond * ticks / 20.0));
    }
}
