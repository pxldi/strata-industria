package dev.strataindustria.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.strataindustria.StrataIndustria;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
import org.jspecify.annotations.Nullable;

/**
 * Tier 5 spec 10.1: an electric machine's numbers, as the block data map {@code strataindustria:electric_stats}
 * so packs can rebalance them. Recipes hold LV base ticks; the tick multiplier and parallel count turn
 * them into each tier's speed (the MV rule: 4x draw, half the ticks, two operations at once).
 *
 * @param draw        J/t while running
 * @param ticks       multiplier on a recipe's base ticks
 * @param parallel    operations at once
 * @param bufferTicks the internal buffer, in ticks of full draw (spec 6.4)
 */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public record ElectricStats(Tiered<Integer> draw, Tiered<Float> ticks, Tiered<Integer> parallel, int bufferTicks) {
    /** One value for each tier. */
    public record Tiered<T>(T lv, T mv) {
        static <T> Codec<Tiered<T>> codec(Codec<T> value) {
            return RecordCodecBuilder.create(i -> i.group(
                    value.fieldOf("lv").forGetter(Tiered::lv),
                    value.fieldOf("mv").forGetter(Tiered::mv)
            ).apply(i, Tiered::new));
        }

        public T get(ElectricTier tier) {
            return tier == ElectricTier.MV ? mv : lv;
        }
    }

    public static final Codec<ElectricStats> CODEC = RecordCodecBuilder.create(i -> i.group(
            Tiered.codec(Codec.intRange(1, 100_000)).fieldOf("draw").forGetter(ElectricStats::draw),
            Tiered.codec(Codec.floatRange(0.01f, 100.0f)).fieldOf("ticks").forGetter(ElectricStats::ticks),
            Tiered.codec(Codec.intRange(1, 2)).fieldOf("parallel").forGetter(ElectricStats::parallel),
            Codec.intRange(1, 1200).optionalFieldOf("buffer_ticks", 20).forGetter(ElectricStats::bufferTicks)
    ).apply(i, ElectricStats::new));

    public static final DataMapType<Block, ElectricStats> DATA_MAP = DataMapType.builder(StrataIndustria.id("electric_stats"), Registries.BLOCK, CODEC)
            .synced(CODEC, false)
            .build();

    /** The standard MV rule on an LV draw: 4x the power, half the ticks, two at once, a 20 tick buffer. */
    public static ElectricStats standard(int lvDraw) {
        return new ElectricStats(new Tiered<>(lvDraw, lvDraw * 4), new Tiered<>(1.0f, 0.5f), new Tiered<>(1, 2), 20);
    }

    /** The data map entry for {@code block}, or {@code fallback} when a pack removed it. */
    public static ElectricStats of(Block block, ElectricStats fallback) {
        @Nullable ElectricStats stats = block.builtInRegistryHolder().getData(DATA_MAP);
        return stats != null ? stats : fallback;
    }

    @SubscribeEvent
    static void register(RegisterDataMapTypesEvent event) {
        event.register(DATA_MAP);
    }
}
