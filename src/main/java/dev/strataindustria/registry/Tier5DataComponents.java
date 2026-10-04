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

    /** Spec 16.3: the first insulator clicked with an ACSR conductor, waiting for the second. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<net.minecraft.core.BlockPos>> SPAN_LINK =
            ModDataComponents.COMPONENTS.registerComponentType("span_link", b -> b
                    .persistent(net.minecraft.core.BlockPos.CODEC)
                    .networkSynchronized(net.minecraft.core.BlockPos.STREAM_CODEC));

    /** Spec 13.2: what the ore scanner found last; kept on the item until the next scan. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<dev.strataindustria.prospecting.OreScan>> ORE_SCAN =
            ModDataComponents.COMPONENTS.registerComponentType("ore_scan", b -> b
                    .persistent(dev.strataindustria.prospecting.OreScan.CODEC)
                    .networkSynchronized(dev.strataindustria.prospecting.OreScan.STREAM_CODEC));

    public static void init() {}

    private Tier5DataComponents() {}
}
