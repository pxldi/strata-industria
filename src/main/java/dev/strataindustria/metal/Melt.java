package dev.strataindustria.metal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.strataindustria.material.Metal;
import io.netty.buffer.ByteBuf;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Metal by units, with the unit-weighted material quality carried along (spec 6.4). Used for a
 * crucible's contents, a filled mold and a slag ingot's composition.
 */
public record Melt(Map<Metal, Integer> units, int qualityUnits) {
    public static final Melt EMPTY = new Melt(Map.of(), 0);

    public static final Codec<Melt> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(Metal.CODEC, Codec.INT).fieldOf("units").forGetter(Melt::units),
            Codec.INT.optionalFieldOf("quality_units", 0).forGetter(Melt::qualityUnits)
    ).apply(i, Melt::new));

    public static final StreamCodec<ByteBuf, Melt> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.<ByteBuf, Metal, Integer, Map<Metal, Integer>>map(i -> new EnumMap<>(Metal.class), Metal.STREAM_CODEC,
                    ByteBufCodecs.VAR_INT), Melt::units,
            ByteBufCodecs.INT, Melt::qualityUnits,
            Melt::new);

    public Melt {
        units = Map.copyOf(units);
    }

    public static Melt of(Metal metal, int units, int quality) {
        return new Melt(Map.of(metal, units), units * quality);
    }

    public int total() {
        int total = 0;
        for (int u : units.values()) total += u;
        return total;
    }

    public boolean isEmpty() {
        return total() <= 0;
    }

    /** Share of a metal, 0 to 1. */
    public float share(Metal metal) {
        int total = total();
        return total == 0 ? 0 : units.getOrDefault(metal, 0) / (float) total;
    }

    public int quality() {
        int total = total();
        return total == 0 ? 0 : Math.round(qualityUnits / (float) total);
    }

    public Melt plus(Melt other) {
        Map<Metal, Integer> sum = new EnumMap<>(Metal.class);
        sum.putAll(units);
        other.units.forEach((metal, u) -> sum.merge(metal, u, Integer::sum));
        return new Melt(sum, qualityUnits + other.qualityUnits);
    }

    /** Takes {@code amount} units out evenly across the metals. */
    public Melt minus(int amount) {
        int total = total();
        if (amount >= total) return EMPTY;
        Map<Metal, Integer> left = new EnumMap<>(Metal.class);
        int removed = 0;
        Metal largest = null;
        for (var e : units.entrySet()) {
            int take = (int) Math.floor(e.getValue() * (double) amount / total);
            left.put(e.getKey(), e.getValue() - take);
            removed += take;
            if (largest == null || e.getValue() > units.get(largest)) largest = e.getKey();
        }
        // Rounding leftovers come out of the largest share.
        if (largest != null) left.merge(largest, -(amount - removed), Integer::sum);
        left.values().removeIf(u -> u <= 0);
        return new Melt(left, Math.round(qualityUnits * (float) (total - amount) / total));
    }

    /** The same mix at a share of its units: slag gives back 90% when remelted. */
    public Melt scaled(float factor) {
        Map<Metal, Integer> out = new EnumMap<>(Metal.class);
        units.forEach((metal, u) -> {
            int scaled = (int) Math.floor(u * factor);
            if (scaled > 0) out.put(metal, scaled);
        });
        return new Melt(out, Math.round(qualityUnits * factor));
    }

    /** Where everything in the mix is liquid: the highest melting point among its metals. */
    public int meltingPoint() {
        return units.keySet().stream().mapToInt(Metal::meltingPoint).max().orElse(0);
    }
}
