package dev.strataindustria.registry;

import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Tier 4 spec 5.2: creosote, the oil a coke oven drives out of coal and wood. Like lye and tannin it lives
 * in tanks; the bucket carries it from the oven to the crafting grid.
 */
public final class Tier4Fluids {
    public static final DeferredHolder<FluidType, FluidType> CREOSOTE_TYPE = ModFluids.TYPES.register("creosote",
            () -> new FluidType(FluidType.Properties.create().density(1100).viscosity(3000)));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> CREOSOTE = ModFluids.FLUIDS.register("creosote",
            () -> new BaseFlowingFluid.Source(Tier4Fluids.creosoteProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_CREOSOTE = ModFluids.FLUIDS.register("flowing_creosote",
            () -> new BaseFlowingFluid.Flowing(Tier4Fluids.creosoteProperties()));

    /**
     * Spec 5.3: the gas roasting drives out of sphalerite. It has no bucket and no world block; it lives
     * in tanks until tier 5 turns it into acid.
     */
    public static final DeferredHolder<FluidType, FluidType> SULFUR_DIOXIDE_TYPE = ModFluids.TYPES.register("sulfur_dioxide",
            () -> new FluidType(FluidType.Properties.create().density(-500).viscosity(200)));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> SULFUR_DIOXIDE = ModFluids.FLUIDS.register("sulfur_dioxide",
            () -> new BaseFlowingFluid.Source(Tier4Fluids.sulfurDioxideProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_SULFUR_DIOXIDE = ModFluids.FLUIDS.register("flowing_sulfur_dioxide",
            () -> new BaseFlowingFluid.Flowing(Tier4Fluids.sulfurDioxideProperties()));

    private static BaseFlowingFluid.Properties sulfurDioxideProperties() {
        return new BaseFlowingFluid.Properties(SULFUR_DIOXIDE_TYPE, SULFUR_DIOXIDE, FLOWING_SULFUR_DIOXIDE);
    }

    private static BaseFlowingFluid.Properties creosoteProperties() {
        return new BaseFlowingFluid.Properties(CREOSOTE_TYPE, CREOSOTE, FLOWING_CREOSOTE);
    }

    public static void init() {}

    private Tier4Fluids() {}
}
