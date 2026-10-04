package dev.strataindustria.registry;

import dev.strataindustria.oil.SeismicSurvey;
import net.minecraft.core.component.DataComponentType;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Tier 6 data components (spec 17.3). */
public final class Tier6DataComponents {
    /** Spec 5.2: the last seismic survey an ore scanner recorded, kept next to its last ore scan. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SeismicSurvey>> SEISMIC_RESULT =
            ModDataComponents.COMPONENTS.registerComponentType("seismic_result", b -> b
                    .persistent(SeismicSurvey.CODEC)
                    .networkSynchronized(SeismicSurvey.STREAM_CODEC));

    public static void init() {}

    private Tier6DataComponents() {}
}
