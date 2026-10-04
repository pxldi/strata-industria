package dev.strataindustria.registry;

import dev.strataindustria.smithing.SmithingPattern;
import dev.strataindustria.smithing.SmithingProgress;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.heat.Temperature;
import dev.strataindustria.knapping.KnappedFrom;
import dev.strataindustria.metal.Melt;
import dev.strataindustria.metal.Quality;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, StrataIndustria.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<KnappedFrom>> KNAPPED_FROM =
            COMPONENTS.registerComponentType("knapped_from", b -> b
                    .persistent(KnappedFrom.CODEC)
                    .networkSynchronized(KnappedFrom.STREAM_CODEC));

    /** Spec 5.1: an item's temperature, worked out lazily from when it was last set. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Temperature>> TEMPERATURE =
            COMPONENTS.registerComponentType("temperature", b -> b
                    .persistent(Temperature.CODEC)
                    .networkSynchronized(Temperature.STREAM_CODEC));

    /** Spec 6.4: material and craft quality of cast and smithed items. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Quality>> QUALITY =
            COMPONENTS.registerComponentType("quality", b -> b
                    .persistent(Quality.CODEC)
                    .networkSynchronized(Quality.STREAM_CODEC));

    /** Spec 9.2: an unfinished smithing workpiece. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SmithingProgress>> SMITHING_PROGRESS =
            COMPONENTS.registerComponentType("smithing_progress", b -> b
                    .persistent(SmithingProgress.CODEC)
                    .networkSynchronized(SmithingProgress.STREAM_CODEC));

    /** Spec 7.4: what went into a slag metal ingot. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Melt>> SLAG =
            COMPONENTS.registerComponentType("slag", b -> b
                    .persistent(Melt.CODEC)
                    .networkSynchronized(Melt.STREAM_CODEC));

    /** Spec 7.3: the metal poured into a mold. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Melt>> CAST_CONTENTS =
            COMPONENTS.registerComponentType("cast_contents", b -> b
                    .persistent(Melt.CODEC)
                    .networkSynchronized(Melt.STREAM_CODEC));

    /** Spec 7.1: a crucible's melt, kept on the item when it is broken. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Melt>> CRUCIBLE_MELT =
            COMPONENTS.registerComponentType("crucible_melt", b -> b
                    .persistent(Melt.CODEC)
                    .networkSynchronized(Melt.STREAM_CODEC));

    /** Tier 3 spec 5.4: the iron in a raw bloom. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Melt>> BLOOM_CONTENTS =
            COMPONENTS.registerComponentType("bloom_contents", b -> b
                    .persistent(Melt.CODEC)
                    .networkSynchronized(Melt.STREAM_CODEC));

    /** Tier 3 spec 9.5: a recorded smithing sequence. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SmithingPattern>> SMITHING_PATTERN =
            COMPONENTS.registerComponentType("smithing_pattern", b -> b
                    .persistent(SmithingPattern.CODEC)
                    .networkSynchronized(SmithingPattern.STREAM_CODEC));

    /** Tier 3 spec 8.5: the column a core sampler drilled. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<dev.strataindustria.prospecting.CoreSample>> CORE_SAMPLE =
            COMPONENTS.registerComponentType("core_sample", b -> b
                    .persistent(dev.strataindustria.prospecting.CoreSample.CODEC)
                    .networkSynchronized(dev.strataindustria.prospecting.CoreSample.STREAM_CODEC));

    private ModDataComponents() {}
}
