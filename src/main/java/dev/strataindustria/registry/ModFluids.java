package dev.strataindustria.registry;

import dev.strataindustria.StrataIndustria;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Lye and tannin (tier 3 spec 12.1): real fluids with no world block and no bucket. They live in soaking
 * barrels for now; tier 4 pipes can move them.
 */
public final class ModFluids {
    public static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, StrataIndustria.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, StrataIndustria.MOD_ID);

    public static final DeferredHolder<FluidType, FluidType> LYE_TYPE = TYPES.register("lye",
            () -> new FluidType(FluidType.Properties.create().density(1100).viscosity(1200)));
    public static final DeferredHolder<FluidType, FluidType> TANNIN_TYPE = TYPES.register("tannin",
            () -> new FluidType(FluidType.Properties.create().density(1050).viscosity(1500)));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> LYE = FLUIDS.register("lye",
            () -> new BaseFlowingFluid.Source(ModFluids.lyeProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_LYE = FLUIDS.register("flowing_lye",
            () -> new BaseFlowingFluid.Flowing(ModFluids.lyeProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> TANNIN = FLUIDS.register("tannin",
            () -> new BaseFlowingFluid.Source(ModFluids.tanninProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_TANNIN = FLUIDS.register("flowing_tannin",
            () -> new BaseFlowingFluid.Flowing(ModFluids.tanninProperties()));

    private static BaseFlowingFluid.Properties lyeProperties() {
        return new BaseFlowingFluid.Properties(LYE_TYPE, LYE, FLOWING_LYE);
    }

    private static BaseFlowingFluid.Properties tanninProperties() {
        return new BaseFlowingFluid.Properties(TANNIN_TYPE, TANNIN, FLOWING_TANNIN);
    }

    private ModFluids() {}
}
