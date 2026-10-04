package dev.strataindustria.fluid;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

/** Which fluid a bucket in the hand holds. */
public final class FluidBuckets {
    private FluidBuckets() {}

    /** The source fluid whose bucket {@code stack} is, or null for anything that is not a full bucket. */
    public static @Nullable Fluid fluidOf(ItemStack stack) {
        if (stack.isEmpty()) return null;
        for (Fluid fluid : BuiltInRegistries.FLUID) {
            if (fluid.isSource(fluid.defaultFluidState()) && fluid.getBucket() == stack.getItem() && !fluid.getBucket().equals(net.minecraft.world.item.Items.AIR)) {
                return fluid;
            }
        }
        return null;
    }
}
