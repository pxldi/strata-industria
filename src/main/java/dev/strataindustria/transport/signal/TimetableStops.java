package dev.strataindustria.transport.signal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.strataindustria.transport.rail.StopData;
import dev.strataindustria.transport.rail.StopRule;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What a timetable item holds (outposts spec 9.4): up to eight stop names in the order they are visited, each with
 * an optional departure rule that stands in for the stop's own.
 */
public record TimetableStops(List<Entry> entries) {
    public static final int MAX_STOPS = 8;
    public static final TimetableStops EMPTY = new TimetableStops(List.of());

    /** One line of a timetable: a stop's name and, if the line sets one, how long to stand there. */
    public record Entry(String name, Optional<StopRule> rule, int seconds) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(in -> in.group(
                Codec.STRING.fieldOf("name").forGetter(Entry::name),
                StopRule.CODEC.optionalFieldOf("rule").forGetter(Entry::rule),
                Codec.INT.optionalFieldOf("seconds", StopData.DEFAULT_SECONDS).forGetter(Entry::seconds)).apply(in, Entry::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(StopData.MAX_NAME * 4), Entry::name,
                ByteBufCodecs.optional(ByteBufCodecs.idMapper(i -> StopRule.values()[Math.floorMod(i, StopRule.values().length)], StopRule::ordinal)), Entry::rule,
                ByteBufCodecs.VAR_INT, Entry::seconds,
                Entry::new);

        public Entry cleaned() {
            return new Entry(StopData.cleanName(name), rule, StopData.cleanSeconds(seconds));
        }

        /** True when this line names {@code stop}: names compare without regard to case. */
        public boolean names(String stop) {
            return !name.isBlank() && name.equalsIgnoreCase(StopData.cleanName(stop));
        }
    }

    public static final Codec<TimetableStops> CODEC = Entry.CODEC.sizeLimitedListOf(MAX_STOPS).xmap(TimetableStops::new, TimetableStops::entries);
    public static final StreamCodec<RegistryFriendlyByteBuf, TimetableStops> STREAM_CODEC =
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_STOPS)).map(TimetableStops::new, TimetableStops::entries);

    /** The same lines with blank ones dropped, names trimmed and the list cut to eight. */
    public TimetableStops cleaned() {
        List<Entry> out = new ArrayList<>();
        for (Entry entry : entries) {
            Entry clean = entry.cleaned();
            if (!clean.name().isBlank() && out.size() < MAX_STOPS) out.add(clean);
        }
        return new TimetableStops(List.copyOf(out));
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }
}
