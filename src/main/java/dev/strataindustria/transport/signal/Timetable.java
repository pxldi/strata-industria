package dev.strataindustria.transport.signal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.strataindustria.transport.signal.TimetableStops.Entry;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A timetable a lead vehicle is running (outposts spec 9.4): its stops in order and the one it is bound for. The
 * vehicle goes by every other stop on the line and stands at the next one on its list; after the last it starts again.
 */
public final class Timetable {
    public static final Codec<Timetable> CODEC = RecordCodecBuilder.create(in -> in.group(
            TimetableStops.CODEC.fieldOf("stops").forGetter(t -> new TimetableStops(t.entries)),
            Codec.INT.optionalFieldOf("next", 0).forGetter(t -> t.next),
            Codec.INT.optionalFieldOf("visited", 0).forGetter(t -> t.visited)).apply(in, Timetable::new));

    private final List<Entry> entries;
    private int next;
    private int visited;
    private boolean looped;
    private int rounds;

    private Timetable(TimetableStops stops, int next, int visited) {
        this.entries = stops.entries();
        this.next = entries.isEmpty() ? 0 : Math.floorMod(next, entries.size());
        this.visited = Math.max(0, visited);
    }

    /** A fresh run of {@code stops}, bound for the first, or null when there is nothing to run. */
    public static @Nullable Timetable of(TimetableStops stops) {
        TimetableStops clean = stops.cleaned();
        return clean.isEmpty() ? null : new Timetable(clean, 0, 0);
    }

    public List<Entry> entries() {
        return entries;
    }

    public int size() {
        return entries.size();
    }

    /** The line the vehicle is bound for. */
    public Entry target() {
        return entries.get(next);
    }

    /** The line after the target, which is the target again on a one-stop timetable. */
    public Entry after() {
        return entries.get((next + 1) % entries.size());
    }

    public int nextIndex() {
        return next;
    }

    /** Whether the vehicle should stand at a stop called {@code name}: it is the one it is bound for. */
    public boolean wants(String name) {
        return target().names(name);
    }

    /** Whether stops carry on through, ignoring "reverse here": there is a stop to go on to. */
    public boolean carriesOn() {
        return entries.size() >= 2;
    }

    /** The vehicle stands at its target: that line is returned and the next becomes the target. */
    public Entry arrive() {
        Entry entry = target();
        next = (next + 1) % entries.size();
        looped = ++visited >= entries.size();
        if (looped) {
            visited = 0;
            rounds++;
        }
        return entry;
    }

    /** Rounds of every stop on the list this run has finished. */
    public int rounds() {
        return rounds;
    }

    /** True when the last arrival finished a round of every stop on the list. */
    public boolean completedRound() {
        return looped;
    }
}
