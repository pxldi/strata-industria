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

    private static final Codec<OilReservoirData> CODEC = Entry.CODEC.listOf().fieldOf("reservoirs").codec()
            .xmap(OilReservoirData::new, OilReservoirData::entries);

    public static final SavedDataType<OilReservoirData> TYPE = new SavedDataType<>(StrataIndustria.id("oil_reservoirs"),
            OilReservoirData::new, CODEC);

    private final Map<Long, Entry> remaining = new HashMap<>();

    public OilReservoirData() {}

    private OilReservoirData(List<Entry> entries) {
        for (Entry e : entries) remaining.put(OilReservoir.cellKey(e.cellX(), e.cellZ()), e);
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
