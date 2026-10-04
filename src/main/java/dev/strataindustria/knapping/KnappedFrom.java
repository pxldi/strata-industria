package dev.strataindustria.knapping;

import com.mojang.serialization.Codec;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.geology.Rock;
import java.util.Arrays;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What a stone tool was struck from: a rock id or {@code flint}. The rock sets how long the tool lasts
 * ({@link Rock#toolDurability()}).
 */
public record KnappedFrom(String source) {
    public static final String FLINT = "flint";
    public static final float FLINT_MULTIPLIER = 1.5f;

    public static final Codec<KnappedFrom> CODEC = Codec.STRING.xmap(KnappedFrom::new, KnappedFrom::source);
    public static final StreamCodec<RegistryFriendlyByteBuf, KnappedFrom> STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.map(KnappedFrom::new, KnappedFrom::source).cast();

    public static KnappedFrom of(Rock rock) {
        return new KnappedFrom(rock.id());
    }

    public Optional<Rock> rock() {
        return Arrays.stream(Rock.values()).filter(r -> r.id().equals(source)).findFirst();
    }

    public float durabilityMultiplier() {
        return rock().map(Rock::toolDurability).orElse(FLINT.equals(source) ? FLINT_MULTIPLIER : 1.0f);
    }

    /** "Struck from basalt". */
    public Component tooltip() {
        String prefix = StrataIndustria.MOD_ID + ".knapped_from.";
        return rock()
                .map(r -> Component.translatable(prefix + "rock", Component.translatable(prefix + "material." + r.id())))
                .orElseGet(() -> Component.translatable(prefix + "flint"))
                .withStyle(ChatFormatting.GRAY);
    }
}
