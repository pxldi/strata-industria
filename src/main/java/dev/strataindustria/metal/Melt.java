package dev.strataindustria.metal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.strataindustria.material.Metal;
import io.netty.buffer.ByteBuf;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Metal by units. Used for a crucible's contents, a filled mold and a slag ingot's composition. */
public record Melt(Map<Metal, Integer> units) {
    public static final Melt EMPTY = new Melt(Map.of());

    /** Old saves carry a {@code quality_units} field from the ore-grade quality; it is ignored. */
    public static final Codec<Melt> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(Metal.CODEC, Codec.INT).fieldOf("units").forGetter(Melt::units)
    ).apply(i, Melt::new));

    public static final StreamCodec<ByteBuf, Melt> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.<ByteBuf, Metal, Integer, Map<Metal, Integer>>map(i -> new EnumMap<>(Metal.class), Metal.STREAM_CODEC,
                    ByteBufCodecs.VAR_INT), Melt::units,
            Melt::new);

    public Melt {
        units = Map.copyOf(units);
    }

    public static Melt of(Metal metal, int units) {
        return new Melt(Map.of(metal, units));
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

    public Melt plus(Melt other) {
        Map<Metal, Integer> sum = new EnumMap<>(Metal.class);
        sum.putAll(units);
        other.units.forEach((metal, u) -> sum.merge(metal, u, Integer::sum));
        return new Melt(sum);
    }

    /**
     * Takes {@code amount} units out evenly across the metals. Leftover units from rounding go to the
     * metals with the largest fractions, so a 1% share of carbon still leaves with a 100-unit pour.
     */
    public Melt minus(int amount) {
        int total = total();
        if (amount >= total) return EMPTY;
        Map<Metal, Integer> take = new EnumMap<>(Metal.class);
        Map<Metal, Double> fraction = new EnumMap<>(Metal.class);
        int taken = 0;
        for (var e : units.entrySet()) {
            double exact = e.getValue() * (double) amount / total;
            int floor = (int) Math.floor(exact);
            take.put(e.getKey(), floor);
            fraction.put(e.getKey(), exact - floor);
            taken += floor;
        }
        int spare = amount - taken;
        var order = new java.util.ArrayList<>(units.keySet());
        order.sort((a, b) -> Double.compare(fraction.get(b), fraction.get(a)));
        for (int i = 0; i < spare && i < order.size(); i++) take.merge(order.get(i), 1, Integer::sum);
        Map<Metal, Integer> left = new EnumMap<>(Metal.class);
        units.forEach((metal, u) -> {
            int rest = u - take.get(metal);
            if (rest > 0) left.put(metal, rest);
        });
        return new Melt(left);
    }

    /** The same mix at a share of its units: slag gives back 90% when remelted. */
    public Melt scaled(float factor) {
        Map<Metal, Integer> out = new EnumMap<>(Metal.class);
        units.forEach((metal, u) -> {
            int scaled = (int) Math.floor(u * factor);
            if (scaled > 0) out.put(metal, scaled);
        });
        // Flooring each metal on its own loses units; the largest share takes up what the whole should give.
        int shortfall = Math.round(total() * factor) - out.values().stream().mapToInt(Integer::intValue).sum();
        if (shortfall > 0) {
            units.entrySet().stream().max(Map.Entry.comparingByValue())
                    .ifPresent(largest -> out.merge(largest.getKey(), shortfall, Integer::sum));
        }
        return new Melt(out);
    }

    /** Where everything in the mix is liquid: the highest melting point among its metals. */
    public int meltingPoint() {
        return units.keySet().stream().mapToInt(Metal::meltingPoint).max().orElse(0);
    }
}
