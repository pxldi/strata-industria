package dev.strataindustria.registry;

import com.mojang.serialization.Codec;
import dev.strataindustria.power.ElectricTier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Tier 5 data components (spec 16.3). */
public final class Tier5DataComponents {
    /** Joules held by a battery box picked up with its charge. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ENERGY =
            ModDataComponents.COMPONENTS.registerComponentType("energy", b -> b
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Spec 9.5: the tier an upgraded machine keeps when it is picked up; absent means LV. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ElectricTier>> MACHINE_TIER =
            ModDataComponents.COMPONENTS.registerComponentType("machine_tier", b -> b
                    .persistent(StringRepresentable.fromEnum(ElectricTier::values))
                    .networkSynchronized(ByteBufCodecs.idMapper(i -> ElectricTier.values()[i], ElectricTier::ordinal)));

    public static void init() {}

    private Tier5DataComponents() {}
}
