package dev.strataindustria.registry;

import dev.strataindustria.automation.FilterContents;
import net.minecraft.core.component.DataComponentType;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Tier 4 data components (spec 16). */
public final class Tier4DataComponents {
    /** Spec 13.5: a filter's entries and toggles. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FilterContents>> FILTER_CONTENTS =
            ModDataComponents.COMPONENTS.registerComponentType("filter_contents", b -> b
                    .persistent(FilterContents.CODEC)
                    .networkSynchronized(FilterContents.STREAM_CODEC));

    public static void init() {}

    private Tier4DataComponents() {}
}
