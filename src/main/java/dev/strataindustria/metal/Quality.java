package dev.strataindustria.metal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.strataindustria.StrataIndustria;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

/**
 * Item quality: how well a piece was made. Struck bright on the anvil it gains up to +4, a casting starts
 * at {@link #CAST}. Tools get a percent of durability per point. Ore grade has no part in it.
 */
public record Quality(int craft) {
    public static final int LIMIT = 20;
    /** Craft part of anything cast in a mold. */
    public static final int CAST = -4;

    public static final Codec<Quality> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("craft").forGetter(Quality::craft)
    ).apply(i, Quality::new));

    public static final StreamCodec<ByteBuf, Quality> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Quality::craft,
            Quality::new);

    public int total() {
        return Mth.clamp(craft, -LIMIT, LIMIT);
    }

    public float durabilityMultiplier() {
        return 1.0f + total() / 100.0f;
    }

    public String grade() {
        int t = total();
        if (t <= CAST) return "rough";
        if (t < 3) return "standard";
        return "fine";
    }

    public Component tooltip() {
        ChatFormatting colour = switch (grade()) {
            case "rough" -> ChatFormatting.GOLD;
            case "fine" -> ChatFormatting.GREEN;
            default -> ChatFormatting.GRAY;
        };
        return Component.translatable(StrataIndustria.MOD_ID + ".quality." + grade()).withStyle(colour);
    }
}
