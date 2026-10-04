package dev.strataindustria.registry;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jspecify.annotations.Nullable;

/**
 * Tier 5 fluids (spec 5.2 and 11.5): latex, sulfuric acid, brine and the three gases. None has a world
 * block; they live in tap cups, barrels, tanks, machines and pipes. Latex and sulfuric acid have buckets.
 */
public final class Tier5Fluids {
    /** One registered fluid: its still and flowing forms. */
    public record Entry(String name, DeferredHolder<Fluid, BaseFlowingFluid.Source> source,
            DeferredHolder<Fluid, BaseFlowingFluid.Flowing> flowing, boolean gas) {}

    /** The fluids added after latex, in spec order, for the client and datagen. */
    public static final Map<String, Entry> ALL = new LinkedHashMap<>();

    public static final DeferredHolder<FluidType, FluidType> LATEX_TYPE = ModFluids.TYPES.register("latex",
            () -> new FluidType(FluidType.Properties.create().density(980).viscosity(4000).temperature(293)));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> LATEX = ModFluids.FLUIDS.register("latex",
            () -> new BaseFlowingFluid.Source(Tier5Fluids.latexProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_LATEX = ModFluids.FLUIDS.register("flowing_latex",
            () -> new BaseFlowingFluid.Flowing(Tier5Fluids.latexProperties()));

    /** Oily and pale yellow; the contact process's product. */
    public static final Entry SULFURIC_ACID = register("sulfuric_acid", false, 1840, 2400, Tier5Items.SULFURIC_ACID_BUCKET);
    /** Salt water pumped from oceans, the electrolyser's chlorine source. */
    public static final Entry BRINE = register("brine", false, 1100, 1100, null);
    public static final Entry HYDROGEN = register("hydrogen", true, -50, 100, null);
    public static final Entry OXYGEN = register("oxygen", true, -500, 200, null);
    public static final Entry CHLORINE = register("chlorine", true, -300, 200, null);

    private static BaseFlowingFluid.Properties latexProperties() {
        return new BaseFlowingFluid.Properties(LATEX_TYPE, LATEX, FLOWING_LATEX).bucket(Tier5Items.LATEX_BUCKET);
    }

    @SuppressWarnings("unchecked")
    private static Entry register(String name, boolean gas, int density, int viscosity, @Nullable DeferredHolder<Item, Item> bucket) {
        DeferredHolder<FluidType, FluidType> type = ModFluids.TYPES.register(name,
                () -> new FluidType(FluidType.Properties.create().density(density).viscosity(viscosity).temperature(293)));
        DeferredHolder<Fluid, BaseFlowingFluid.Source>[] source = new DeferredHolder[1];
        DeferredHolder<Fluid, BaseFlowingFluid.Flowing>[] flowing = new DeferredHolder[1];
        java.util.function.Supplier<BaseFlowingFluid.Properties> properties = () -> {
            BaseFlowingFluid.Properties p = new BaseFlowingFluid.Properties(type, source[0], flowing[0]);
            return bucket == null ? p : p.bucket(bucket);
        };
        source[0] = ModFluids.FLUIDS.register(name, () -> new BaseFlowingFluid.Source(properties.get()));
        flowing[0] = ModFluids.FLUIDS.register("flowing_" + name, () -> new BaseFlowingFluid.Flowing(properties.get()));
        Entry entry = new Entry(name, source[0], flowing[0], gas);
        ALL.put(name, entry);
        return entry;
    }

    /** The fluid a bucket in the hand pours into a machine, or null: water and the two bucketed tier 5 liquids. */
    public static @Nullable Fluid fluidOfBucket(ItemStack stack) {
        if (stack.is(Items.WATER_BUCKET)) return Fluids.WATER;
        if (stack.is(Tier5Items.SULFURIC_ACID_BUCKET.get())) return SULFURIC_ACID.source().get();
        if (stack.is(Tier5Items.LATEX_BUCKET.get())) return LATEX.get();
        return null;
    }

    public static void init() {}

    private Tier5Fluids() {}
}
