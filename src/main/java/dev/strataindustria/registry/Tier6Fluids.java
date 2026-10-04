package dev.strataindustria.registry;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.SoundAction;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jspecify.annotations.Nullable;

/**
 * Tier 6 spec 12.3: crude oil and what the refinery makes of it. Crude oil is the only one with a world
 * block (spec 5.1); the refined liquids have buckets that cannot be poured out, and the gases live only
 * in pipes and tanks.
 */
public final class Tier6Fluids {
    /** One registered fluid: its type, source and flowing forms. */
    public record Entry(String name, DeferredHolder<FluidType, FluidType> type, DeferredHolder<Fluid, BaseFlowingFluid.Source> source,
            DeferredHolder<Fluid, BaseFlowingFluid.Flowing> flowing, boolean gas) {}

    /** Every tier 6 fluid in spec order, for datagen and the client. */
    public static final Map<String, Entry> ALL = new LinkedHashMap<>();

    /** Spec 5.1: slow, thick and black; flows 3 blocks, ticks every 30. */
    public static final Entry CRUDE_OIL = register("crude_oil", false,
            FluidType.Properties.create().density(900).viscosity(6000).canSwim(false).canExtinguish(true)
                    .fallDistanceModifier(0.2f).motionScale(0.007),
            p -> p.tickRate(30).slopeFindDistance(2).levelDecreasePerBlock(2)
                    .block(() -> Tier6Blocks.CRUDE_OIL.get()).bucket(() -> Tier6Items.CRUDE_OIL_BUCKET.get()),
            () -> Tier6Sounds.CRUDE_OIL_BUCKET_FILL.get(), () -> Tier6Sounds.CRUDE_OIL_BUCKET_EMPTY.get());
    public static final Entry NAPHTHA = liquid("naphtha", 700, 600);
    public static final Entry DIESEL = liquid("diesel", 840, 1500);
    public static final Entry HEAVY_OIL = liquid("heavy_oil", 980, 8000);
    public static final Entry REFINERY_GAS = gas("refinery_gas");
    public static final Entry ETHYLENE = gas("ethylene");
    public static final Entry BUTADIENE = gas("butadiene");
    public static final Entry VINYL_CHLORIDE = gas("vinyl_chloride");
    /** Corrosive (spec 12.2); only stainless and PVC carry it. */
    public static final Entry HYDROGEN_CHLORIDE = gas("hydrogen_chloride");

    private static Entry liquid(String name, int density, int viscosity) {
        return register(name, false, FluidType.Properties.create().density(density).viscosity(viscosity), p -> p, null, null);
    }

    private static Entry gas(String name) {
        return register(name, true, FluidType.Properties.create().density(-500).viscosity(200), p -> p, null, null);
    }

    @SuppressWarnings("unchecked")
    private static Entry register(String name, boolean gas, FluidType.Properties typeProperties,
            java.util.function.UnaryOperator<BaseFlowingFluid.Properties> tweak, @Nullable Supplier<SoundEvent> fill,
            @Nullable Supplier<SoundEvent> empty) {
        DeferredHolder<FluidType, FluidType> type = ModFluids.TYPES.register(name, () -> new FluidType(typeProperties) {
            @Override
            public @Nullable SoundEvent getSound(SoundAction action) {
                if (action == SoundActions.BUCKET_FILL && fill != null) return fill.get();
                if (action == SoundActions.BUCKET_EMPTY && empty != null) return empty.get();
                return super.getSound(action);
            }
        });
        DeferredHolder<Fluid, BaseFlowingFluid.Source>[] source = new DeferredHolder[1];
        DeferredHolder<Fluid, BaseFlowingFluid.Flowing>[] flowing = new DeferredHolder[1];
        Supplier<BaseFlowingFluid.Properties> properties = () -> tweak.apply(new BaseFlowingFluid.Properties(type, source[0], flowing[0]));
        // Only crude oil can sit in the world, and it drips black through the block under it (spec 24.7).
        boolean drips = fill != null;
        source[0] = ModFluids.FLUIDS.register(name, () -> new BaseFlowingFluid.Source(properties.get()) {
            @Override
            public @Nullable ParticleOptions getDripParticle() {
                return drips ? Tier6Particles.DRIPPING_OIL.get() : null;
            }
        });
        flowing[0] = ModFluids.FLUIDS.register("flowing_" + name, () -> new BaseFlowingFluid.Flowing(properties.get()) {
            @Override
            public @Nullable ParticleOptions getDripParticle() {
                return drips ? Tier6Particles.DRIPPING_OIL.get() : null;
            }
        });
        Entry entry = new Entry(name, type, source[0], flowing[0], gas);
        ALL.put(name, entry);
        return entry;
    }

    public static void init() {}

    private Tier6Fluids() {}
}
