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
 * Item quality (spec 6.4): a material part from the ore grades in the melt and a craft part from how
 * it was made. Tools get up to 20% more or less durability from it.
 */
public record Quality(int material, int craft) {
    public static final int LIMIT = 20;
    /** Craft part of anything cast in a mold. */
    public static final int CAST = -4;

    public static final Codec<Quality> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("material").forGetter(Quality::material),
            Codec.INT.fieldOf("craft").forGetter(Quality::craft)
    ).apply(i, Quality::new));

    public static final StreamCodec<ByteBuf, Quality> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Quality::material,
            ByteBufCodecs.VAR_INT, Quality::craft,
            Quality::new);

    public int total() {
        return Mth.clamp(material + craft, -LIMIT, LIMIT);
    }

    public float durabilityMultiplier() {
        return 1.0f + total() / 100.0f;
    }

    public String grade() {
        int t = total();
        if (t <= -11) return "crude";
        if (t <= -4) return "rough";
        if (t <= 3) return "standard";
        if (t <= 10) return "fine";
        return "masterwork";
    }

    public Component tooltip() {
        ChatFormatting colour = switch (grade()) {
            case "crude" -> ChatFormatting.DARK_RED;
            case "rough" -> ChatFormatting.GOLD;
            case "fine" -> ChatFormatting.GREEN;
            case "masterwork" -> ChatFormatting.AQUA;
            default -> ChatFormatting.GRAY;
        };
        return Component.translatable(StrataIndustria.MOD_ID + ".quality." + grade()).withStyle(colour);
    }
}
