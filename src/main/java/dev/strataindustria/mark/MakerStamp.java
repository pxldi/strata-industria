package dev.strataindustria.mark;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** The mark a piece was stamped with and the name of whoever made it. Rides on cast heads, gears, smithed parts and tools. */
public record MakerStamp(MakersMark mark, String maker) {
    public static final Codec<MakerStamp> CODEC = RecordCodecBuilder.create(i -> i.group(
            MakersMark.CODEC.fieldOf("mark").forGetter(MakerStamp::mark),
            Codec.STRING.fieldOf("maker").forGetter(MakerStamp::maker)
    ).apply(i, MakerStamp::new));

    public static final StreamCodec<ByteBuf, MakerStamp> STREAM_CODEC = StreamCodec.composite(
            MakersMark.STREAM_CODEC, MakerStamp::mark,
            ByteBufCodecs.STRING_UTF8, MakerStamp::maker,
            MakerStamp::new);
}
