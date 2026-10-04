package dev.strataindustria.heat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * An item stack's temperature (spec 5.1): the value it had at game time {@code updated}. The current
 * value is worked out on read from the time since, so stacks lying in chests need no ticking.
 */
public record Temperature(float value, long updated) {
    public static final Codec<Temperature> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.fieldOf("value").forGetter(Temperature::value),
            Codec.LONG.fieldOf("updated").forGetter(Temperature::updated)
    ).apply(i, Temperature::new));

    public static final StreamCodec<ByteBuf, Temperature> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, Temperature::value,
            ByteBufCodecs.VAR_LONG, Temperature::updated,
            Temperature::new);
}
