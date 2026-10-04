package dev.strataindustria.registry;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Tier 5 data components (spec 16.3). */
public final class Tier5DataComponents {
    /** Joules held by a battery box picked up with its charge. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ENERGY =
            ModDataComponents.COMPONENTS.registerComponentType("energy", b -> b
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT));

    public static void init() {}

    private Tier5DataComponents() {}
}
