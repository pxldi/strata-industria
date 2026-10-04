package dev.strataindustria.survey;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What a sheet of survey notes says (structures spec 5.1). Notes come out of a barrel with only their
 * {@code targets}, the minerals the writer was looking for, in order of interest. The first time a player
 * holds them they are read against the vein cells around that spot and gain an {@link Entry}.
 */
public record SurveyNotes(List<String> targets, Optional<Entry> entry, boolean found) {
    public static final Codec<SurveyNotes> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.listOf().optionalFieldOf("targets", List.of()).forGetter(SurveyNotes::targets),
            Entry.CODEC.optionalFieldOf("entry").forGetter(SurveyNotes::entry),
            Codec.BOOL.optionalFieldOf("found", false).forGetter(SurveyNotes::found)
    ).apply(i, SurveyNotes::new));
    public static final StreamCodec<ByteBuf, SurveyNotes> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), SurveyNotes::targets,
            ByteBufCodecs.optional(Entry.STREAM_CODEC), SurveyNotes::entry,
            ByteBufCodecs.BOOL, SurveyNotes::found,
            SurveyNotes::new);

    public static SurveyNotes looking(String... minerals) {
        return new SurveyNotes(List.of(minerals), Optional.empty(), false);
    }

    public SurveyNotes resolve(Entry entry) {
        return new SurveyNotes(targets, Optional.of(entry), false);
    }

    public SurveyNotes markFound() {
        return new SurveyNotes(targets, entry, true);
    }

    /**
     * One deposit.
     *
     * @param mineral mineral id, such as {@code cassiterite}
     * @param pos     centre of the vein
     * @param radius  horizontal radius of the vein in blocks
     * @param depth   blocks from the surface down to the top of the vein
     * @param host    rock id at the vein centre
     * @param size    0 small, 1 medium, 2 large
     * @param hand    which of the handwritten lines to show
     */
    public record Entry(String mineral, BlockPos pos, int radius, int depth, String host, int size, int hand) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("mineral").forGetter(Entry::mineral),
                BlockPos.CODEC.fieldOf("pos").forGetter(Entry::pos),
                Codec.INT.fieldOf("radius").forGetter(Entry::radius),
                Codec.INT.fieldOf("depth").forGetter(Entry::depth),
                Codec.STRING.fieldOf("host").forGetter(Entry::host),
                Codec.INT.fieldOf("size").forGetter(Entry::size),
                Codec.INT.fieldOf("hand").forGetter(Entry::hand)
        ).apply(i, Entry::new));
        public static final StreamCodec<ByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Entry::mineral,
                BlockPos.STREAM_CODEC, Entry::pos,
                ByteBufCodecs.VAR_INT, Entry::radius,
                ByteBufCodecs.VAR_INT, Entry::depth,
                ByteBufCodecs.STRING_UTF8, Entry::host,
                ByteBufCodecs.VAR_INT.map(v -> new int[] {v >> 4, v & 15}, a -> a[0] << 4 | a[1]), e -> new int[] {e.size, e.hand},
                (mineral, pos, radius, depth, host, packed) -> new Entry(mineral, pos, radius, depth, host, packed[0], packed[1]));
    }
}
