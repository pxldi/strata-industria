package dev.strataindustria.bronze;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.strataindustria.material.Metal;
import dev.strataindustria.metal.Melt;
import dev.strataindustria.metal.Quality;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

/**
 * How a cast bell sounds (uniqueness 4.2). The pitch comes from what the melt held: more tin rings higher,
 * arsenic, bismuth and zinc do less for it, plain copper stays dull and low. A crude casting (poor ore in the melt) is
 * cracked, and the crack makes it ring with a beat.
 */
public record BellTone(float pitch, boolean cracked) {
    public static final float LOWEST = 0.6f;
    public static final float HIGHEST = 1.5f;
    /** Pitch of a bell with no record of its casting. */
    public static final BellTone DEFAULT = new BellTone(1.0f, false);

    public static final Codec<BellTone> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.fieldOf("pitch").forGetter(BellTone::pitch),
            Codec.BOOL.optionalFieldOf("cracked", false).forGetter(BellTone::cracked)
    ).apply(i, BellTone::new));

    public static final StreamCodec<ByteBuf, BellTone> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, BellTone::pitch,
            ByteBufCodecs.BOOL, BellTone::cracked,
            BellTone::new);

    /** The tone of a bell cast from {@code melt} with the given casting quality. */
    public static BellTone of(Melt melt, Quality quality) {
        float share = 0;
        int total = Math.max(1, melt.total());
        for (var entry : melt.units().entrySet()) share += weight(entry.getKey()) * entry.getValue() / total;
        float pitch = Mth.clamp(LOWEST + 5.0f * share, LOWEST, HIGHEST);
        boolean cracked = quality != null && quality.grade().equals("crude");
        return new BellTone(Math.round(pitch * 100) / 100f, cracked);
    }

    /** How much of each metal's share goes into the bell's pitch. */
    private static float weight(Metal metal) {
        return switch (metal) {
            case TIN -> 1.0f;
            case ARSENIC -> 0.8f;
            case BISMUTH -> 0.7f;
            case ZINC -> 0.6f;
            default -> 0f;
        };
    }

    /** The note colour (0 to 1) for the particle that rises when the bell rings. */
    public float noteColour() {
        return Mth.clamp((pitch - LOWEST) / (HIGHEST - LOWEST), 0f, 1f);
    }
}
