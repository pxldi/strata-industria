package dev.strataindustria.client;

import com.mojang.serialization.MapCodec;
import dev.strataindustria.heat.Heat;
import dev.strataindustria.heat.HeatBand;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Hot metal glows (style guide heat scale). Heatable items carry a pale copy of their texture as a second
 * layer, shown once the item is hot enough to glow and tinted with the heat colour, more opaque as it heats.
 */
public final class HeatGlow {
    /** Metal starts to glow visibly here: the faint red band. */
    public static final float GLOW_FROM = HeatBand.FAINT_RED.from();
    /** At and above this the glow layer covers the item fully. */
    static final float FULL_FROM = HeatBand.YELLOW.from();
    static final float MIN_ALPHA = 0.3f;

    private HeatGlow() {}

    static float temperature(ItemStack stack, @Nullable ClientLevel level) {
        ClientLevel at = level != null ? level : Minecraft.getInstance().level;
        return at == null ? Heat.AMBIENT : Heat.get(stack, at.getGameTime());
    }

    /** ARGB glow for a temperature: band colours blended, alpha rising with heat. */
    public static int colour(float temperature) {
        if (temperature < GLOW_FROM) return 0;
        HeatBand[] bands = HeatBand.values();
        int rgb = HeatBand.WHITE.colour();
        for (int i = HeatBand.FAINT_RED.ordinal(); i < bands.length - 1; i++) {
            HeatBand low = bands[i], high = bands[i + 1];
            if (temperature < high.from()) {
                float t = (temperature - low.from()) / (high.from() - low.from());
                rgb = lerp(low.colour(), high.colour(), t);
                break;
            }
        }
        float alpha = Mth.clampedMap(temperature, GLOW_FROM, FULL_FROM, MIN_ALPHA, 1.0f);
        return Math.round(alpha * 255) << 24 | rgb;
    }

    private static int lerp(int a, int b, float t) {
        int r = Math.round(Mth.lerp(t, a >> 16 & 0xFF, b >> 16 & 0xFF));
        int g = Math.round(Mth.lerp(t, a >> 8 & 0xFF, b >> 8 & 0xFF));
        int bl = Math.round(Mth.lerp(t, a & 0xFF, b & 0xFF));
        return r << 16 | g << 8 | bl;
    }

    /** Tint for the glow layer. */
    public record Tint() implements ItemTintSource {
        public static final Tint INSTANCE = new Tint();
        public static final MapCodec<Tint> MAP_CODEC = MapCodec.unit(INSTANCE);

        @Override
        public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
            return colour(temperature(stack, level));
        }

        @Override
        public MapCodec<Tint> type() {
            return MAP_CODEC;
        }
    }

    /** True while the item is hot enough to glow; the model only draws the glow layer then. */
    public record Glowing() implements ConditionalItemModelProperty {
        public static final Glowing INSTANCE = new Glowing();
        public static final MapCodec<Glowing> MAP_CODEC = MapCodec.unit(INSTANCE);

        @Override
        public boolean get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner, int seed,
                ItemDisplayContext context) {
            return temperature(stack, level) >= GLOW_FROM;
        }

        @Override
        public MapCodec<Glowing> type() {
            return MAP_CODEC;
        }
    }
}
