package dev.strataindustria.registry;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.knapping.KnappedFrom;
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

    private ModDataComponents() {}
}
