package dev.strataindustria.journal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/**
 * One player's leads notebook (journal leads spec): the leads they have opened, hinted and closed, the notes
 * they have written, and which observations already fired. Kept on the player, copied on death, and synced to
 * its owner so the notebook screen and toasts need no packets of their own.
 *
 * <p>Every change takes the next {@link #seq} number, which orders the notes page and tells the client which
 * changes it has not yet shown as a toast.
 */
public final class JournalState {
    /** A goal of the journal tree as the notebook sees it. {@code -1} means "not yet" for each seq field. */
    public record Lead(String path, String parent, Identifier icon, int x, int y, long openedAt, int openedSeq,
            int hintedSeq, int closedSeq, long closedDay) {
        static final Codec<Lead> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("path").forGetter(Lead::path),
                Codec.STRING.optionalFieldOf("parent", "").forGetter(Lead::parent),
                Identifier.CODEC.fieldOf("icon").forGetter(Lead::icon),
                Codec.INT.optionalFieldOf("x", 0).forGetter(Lead::x),
                Codec.INT.optionalFieldOf("y", 0).forGetter(Lead::y),
                Codec.LONG.optionalFieldOf("opened_at", 0L).forGetter(Lead::openedAt),
                Codec.INT.optionalFieldOf("opened", -1).forGetter(Lead::openedSeq),
                Codec.INT.optionalFieldOf("hinted", -1).forGetter(Lead::hintedSeq),
                Codec.INT.optionalFieldOf("closed", -1).forGetter(Lead::closedSeq),
                Codec.LONG.optionalFieldOf("closed_day", 0L).forGetter(Lead::closedDay)
        ).apply(i, Lead::new));
        static final StreamCodec<RegistryFriendlyByteBuf, Lead> STREAM_CODEC = StreamCodec.of(
                (buf, lead) -> {
                    buf.writeUtf(lead.path);
                    buf.writeUtf(lead.parent);
                    Identifier.STREAM_CODEC.encode(buf, lead.icon);
                    buf.writeVarInt(lead.x);
                    buf.writeVarInt(lead.y);
                    buf.writeVarLong(lead.openedAt);
                    buf.writeVarInt(lead.openedSeq);
                    buf.writeVarInt(lead.hintedSeq);
                    buf.writeVarInt(lead.closedSeq);
                    buf.writeVarLong(lead.closedDay);
                },
                buf -> new Lead(buf.readUtf(), buf.readUtf(), Identifier.STREAM_CODEC.decode(buf), buf.readVarInt(),
                        buf.readVarInt(), buf.readVarLong(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                        buf.readVarLong()));

        public boolean open() {
            return openedSeq >= 0 && closedSeq < 0;
        }

        public boolean hinted() {
            return hintedSeq >= 0;
        }

        public boolean closed() {
            return closedSeq >= 0;
        }

        /** "t3/iron_ore" becomes the lang key base "journal.strataindustria.t3.iron_ore". */
        public String key() {
            return Journal.key(path);
        }

        Lead opened(long at, int seq) {
            return new Lead(path, parent, icon, x, y, at, seq, hintedSeq, closedSeq, closedDay);
        }

        Lead hinted(int seq) {
            return new Lead(path, parent, icon, x, y, openedAt, openedSeq, seq, closedSeq, closedDay);
        }

        Lead closed(int seq, long day) {
            return new Lead(path, parent, icon, x, y, openedAt, openedSeq < 0 ? seq : openedSeq, hintedSeq, seq, day);
        }
    }

    /**
     * A line written into the notebook: an observation, a study, or a place. {@code args} are lang keys, each
     * shown translated in place of a {@code %s}. Quiet notes are written without a toast.
     */
    public record Note(String key, List<String> args, Optional<Identifier> icon, long day, int seq, boolean quiet) {
        static final Codec<Note> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("key").forGetter(Note::key),
                Codec.STRING.listOf().optionalFieldOf("args", List.of()).forGetter(Note::args),
                Identifier.CODEC.optionalFieldOf("icon").forGetter(Note::icon),
                Codec.LONG.optionalFieldOf("day", 0L).forGetter(Note::day),
                Codec.INT.fieldOf("seq").forGetter(Note::seq),
                Codec.BOOL.optionalFieldOf("quiet", false).forGetter(Note::quiet)
        ).apply(i, Note::new));
        static final StreamCodec<RegistryFriendlyByteBuf, Note> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Note::key,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), Note::args,
                ByteBufCodecs.optional(Identifier.STREAM_CODEC), Note::icon,
                ByteBufCodecs.VAR_LONG, Note::day,
                ByteBufCodecs.VAR_INT, Note::seq,
                ByteBufCodecs.BOOL, Note::quiet,
                Note::new);
    }

    /** The newest notes kept; older ones fall off the back of the notebook. */
    static final int MAX_NOTES = 200;

    private final Map<String, Lead> leads;
    private final List<Note> notes;
    private final Set<String> seen;
    private int seq;

    public JournalState() {
        this(new LinkedHashMap<>(), new ArrayList<>(), new HashSet<>(), 0);
    }

    private JournalState(Map<String, Lead> leads, List<Note> notes, Set<String> seen, int seq) {
        this.leads = leads;
        this.notes = notes;
        this.seen = seen;
        this.seq = seq;
    }

    public static final MapCodec<JournalState> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Lead.CODEC.listOf().optionalFieldOf("leads", List.of()).forGetter(s -> List.copyOf(s.leads.values())),
            Note.CODEC.listOf().optionalFieldOf("notes", List.of()).forGetter(s -> s.notes),
            Codec.STRING.listOf().optionalFieldOf("seen", List.of()).forGetter(s -> List.copyOf(s.seen)),
            Codec.INT.optionalFieldOf("seq", 0).forGetter(s -> s.seq)
    ).apply(i, JournalState::of));

    public static final StreamCodec<RegistryFriendlyByteBuf, JournalState> STREAM_CODEC = StreamCodec.composite(
            Lead.STREAM_CODEC.apply(ByteBufCodecs.list()), s -> List.copyOf(s.leads.values()),
            Note.STREAM_CODEC.apply(ByteBufCodecs.list()), s -> s.notes,
            ByteBufCodecs.VAR_INT, s -> s.seq,
            (leads, notes, seq) -> of(leads, notes, List.of(), seq));

    private static JournalState of(List<Lead> leads, List<Note> notes, List<String> seen, int seq) {
        Map<String, Lead> map = new LinkedHashMap<>();
        for (Lead lead : leads) map.put(lead.path(), lead);
        return new JournalState(map, new ArrayList<>(notes), new HashSet<>(seen), seq);
    }

    public Map<String, Lead> leads() {
        return leads;
    }

    public List<Note> notes() {
        return notes;
    }

    public int seq() {
        return seq;
    }

    public Lead lead(String path) {
        return leads.get(path);
    }

    public boolean seen(String observation) {
        return seen.contains(observation);
    }

    /** Marks an observation as fired; false if it already had. */
    boolean see(String observation) {
        return seen.add(observation);
    }

    int next() {
        return seq++;
    }

    void put(Lead lead) {
        leads.put(lead.path(), lead);
    }

    void add(Note note) {
        notes.add(note);
        if (notes.size() > MAX_NOTES) notes.removeFirst();
    }

    public boolean hasOpenLead() {
        for (Lead lead : leads.values()) {
            if (lead.open()) return true;
        }
        return false;
    }
}
