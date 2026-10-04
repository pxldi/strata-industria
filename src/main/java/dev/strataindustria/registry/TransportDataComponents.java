package dev.strataindustria.registry;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Outposts and transport data components (outposts spec 12). */
public final class TransportDataComponents {
    /** A charter taken down keeps its name on the item, so a post moved far still has it. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> CHARTER_NAME =
            ModDataComponents.COMPONENTS.registerComponentType("charter_name", b -> b
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8));

    public static void init() {}

    private TransportDataComponents() {}
}
