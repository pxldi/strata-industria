package dev.strataindustria.registry;

import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Tier 5 spec 5.2: latex, the milky sap a tree tap draws from a living jungle tree. It has a bucket but
 * no world block; it lives in tap cups, barrels and pipes.
 */
public final class Tier5Fluids {
    public static final DeferredHolder<FluidType, FluidType> LATEX_TYPE = ModFluids.TYPES.register("latex",
            () -> new FluidType(FluidType.Properties.create().density(980).viscosity(4000).temperature(293)));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> LATEX = ModFluids.FLUIDS.register("latex",
            () -> new BaseFlowingFluid.Source(Tier5Fluids.latexProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_LATEX = ModFluids.FLUIDS.register("flowing_latex",
            () -> new BaseFlowingFluid.Flowing(Tier5Fluids.latexProperties()));

    private static BaseFlowingFluid.Properties latexProperties() {
        return new BaseFlowingFluid.Properties(LATEX_TYPE, LATEX, FLOWING_LATEX).bucket(Tier5Items.LATEX_BUCKET);
    }

    public static void init() {}

    private Tier5Fluids() {}
}
