package dev.strataindustria.oil;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.strataindustria.StrataIndustria;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * How much crude each tapped reservoir has left (spec 19.1.1). A reservoir nobody has drawn from is not
 * stored: it is full.
 */
public final class OilReservoirData extends SavedData {
    private record Entry(int cellX, int cellZ, long remaining) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("cell_x").forGetter(Entry::cellX),
                Codec.INT.fieldOf("cell_z").forGetter(Entry::cellZ),
                Codec.LONG.fieldOf("remaining").forGetter(Entry::remaining)
        ).apply(i, Entry::new));
    }

    /**
     * A well bored into a reservoir (spec 5.3): how deep the drill got and whether it struck oil. The bore
     * belongs to the reservoir and the column, not to the wellhead, so moving a wellhead never costs the
     * casing twice.
     */
    public record Bore(int cellX, int cellZ, int x, int z, int depth, boolean struck) {
        static final Codec<Bore> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("cell_x").forGetter(Bore::cellX),
                Codec.INT.fieldOf("cell_z").forGetter(Bore::cellZ),
                Codec.INT.fieldOf("x").forGetter(Bore::x),
                Codec.INT.fieldOf("z").forGetter(Bore::z),
                Codec.INT.fieldOf("depth").forGetter(Bore::depth),
                Codec.BOOL.fieldOf("struck").forGetter(Bore::struck)
        ).apply(i, Bore::new));

        public double distanceSqr(int otherX, int otherZ) {
            double dx = x - otherX, dz = z - otherZ;
            return dx * dx + dz * dz;
        }
    }

    private static final Codec<OilReservoirData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Entry.CODEC.listOf().fieldOf("reservoirs").forGetter(OilReservoirData::entries),
            Bore.CODEC.listOf().optionalFieldOf("bores", List.of()).forGetter(OilReservoirData::bores)
    ).apply(i, OilReservoirData::new));

    public static final SavedDataType<OilReservoirData> TYPE = new SavedDataType<>(StrataIndustria.id("oil_reservoirs"),
            OilReservoirData::new, CODEC);

    private final Map<Long, Entry> remaining = new HashMap<>();
    /** Per reservoir key, the bores by block column. */
    private final Map<Long, Map<Long, Bore>> bores = new HashMap<>();

    public OilReservoirData() {}

    private OilReservoirData(List<Entry> entries, List<Bore> bores) {
        for (Entry e : entries) remaining.put(OilReservoir.cellKey(e.cellX(), e.cellZ()), e);
        for (Bore b : bores) this.bores.computeIfAbsent(OilReservoir.cellKey(b.cellX(), b.cellZ()), k -> new HashMap<>())
                .put(column(b.x(), b.z()), b);
    }

    private List<Bore> bores() {
        List<Bore> all = new java.util.ArrayList<>();
        for (Map<Long, Bore> perReservoir : bores.values()) all.addAll(perReservoir.values());
        return all;
    }

    private static long column(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    /** The bore at a block column of a reservoir, if a wellhead ever started one there. */
    public java.util.Optional<Bore> bore(OilReservoir reservoir, int x, int z) {
        Map<Long, Bore> perReservoir = bores.get(reservoir.key());
        return perReservoir == null ? java.util.Optional.empty() : java.util.Optional.ofNullable(perReservoir.get(column(x, z)));
    }

    /** Records how far a column has been bored. */
    public void setBore(OilReservoir reservoir, int x, int z, int depth, boolean struck) {
        bores.computeIfAbsent(reservoir.key(), k -> new HashMap<>())
                .put(column(x, z), new Bore(reservoir.cellX(), reservoir.cellZ(), x, z, depth, struck));
        setDirty();
    }

    /** The nearest other bore of a reservoir closer than {@code spacing} blocks to a column, if any. */
    public java.util.Optional<Bore> tooClose(OilReservoir reservoir, int x, int z, double spacing) {
        Map<Long, Bore> perReservoir = bores.get(reservoir.key());
        if (perReservoir == null) return java.util.Optional.empty();
        Bore nearest = null;
        for (Bore b : perReservoir.values()) {
            if (b.x() == x && b.z() == z || b.distanceSqr(x, z) >= spacing * spacing) continue;
            if (nearest == null || b.distanceSqr(x, z) < nearest.distanceSqr(x, z)) nearest = b;
        }
        return java.util.Optional.ofNullable(nearest);
    }

    /** Whether any well of this reservoir has struck oil: only then does a survey show what is left. */
    public boolean tapped(OilReservoir reservoir) {
        Map<Long, Bore> perReservoir = bores.get(reservoir.key());
        return perReservoir != null && perReservoir.values().stream().anyMatch(Bore::struck);
    }

    private List<Entry> entries() {
        return List.copyOf(remaining.values());
    }

    public static OilReservoirData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    /** Crude left in a reservoir, in mB. */
    public long remaining(OilReservoir reservoir) {
        Entry e = remaining.get(reservoir.key());
        return e == null ? reservoir.capacity() : e.remaining();
    }

    /** Share of the capacity left, 0 to 1. */
    public double fraction(OilReservoir reservoir) {
        return reservoir.capacity() <= 0 ? 0 : (double) remaining(reservoir) / reservoir.capacity();
    }

    /**
     * Takes up to {@code amount} mB out of a reservoir and returns how much came out. A reservoir never goes
     * below zero; at zero wells keep producing at the stripper rate (spec 5.4), which the caller handles.
     */
    public long drain(OilReservoir reservoir, long amount) {
        long left = remaining(reservoir);
        long taken = Math.max(0, Math.min(amount, left));
        remaining.put(reservoir.key(), new Entry(reservoir.cellX(), reservoir.cellZ(), left - taken));
        setDirty();
        return taken;
    }
}
