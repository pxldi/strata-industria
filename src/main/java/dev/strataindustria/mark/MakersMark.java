package dev.strataindustria.mark;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** A maker's mark (uniqueness 2.2): an 8x8 stamp, one bit per cell, row by row from the top left. */
public record MakersMark(long bits) {
    public static final int SIZE = 8;
    public static final int CELLS = SIZE * SIZE;
    public static final MakersMark BLANK = new MakersMark(0L);

    public static final Codec<MakersMark> CODEC = Codec.LONG.xmap(MakersMark::new, MakersMark::bits);
    public static final StreamCodec<ByteBuf, MakersMark> STREAM_CODEC = ByteBufCodecs.VAR_LONG.map(MakersMark::new, MakersMark::bits);

    public boolean isBlank() {
        return bits == 0L;
    }

    public boolean get(int cell) {
        return (bits >>> cell & 1L) != 0L;
    }

    public boolean get(int x, int y) {
        return get(y * SIZE + x);
    }

    public MakersMark toggled(int cell) {
        return new MakersMark(bits ^ 1L << cell);
    }

    public int count() {
        return Long.bitCount(bits);
    }
}
