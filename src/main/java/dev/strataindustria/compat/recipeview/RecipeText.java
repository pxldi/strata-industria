package dev.strataindustria.compat.recipeview;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.heat.HeatBand;
import java.util.Locale;
import net.minecraft.network.chat.Component;

/**
 * Text shared by recipe viewer pages. Heat is always shown by band name, never in degrees (tier 0-2
 * spec 5.3), and times read in seconds or minutes.
 */
public final class RecipeText {
    public static final String KEY = "recipe_view." + StrataIndustria.MOD_ID + ".";

    private RecipeText() {}

    public static Component key(String path, Object... args) {
        return Component.translatable(KEY + path, args);
    }

    /** The band a temperature falls in, uncoloured so it reads on any page background. */
    public static HeatBand band(int temperature) {
        return HeatBand.of(temperature);
    }

    public static Component bandName(int temperature) {
        HeatBand band = band(temperature);
        return Component.translatable(StrataIndustria.MOD_ID + ".heat." + (band == HeatBand.NONE ? HeatBand.WARMING : band).id());
    }

    /** "Works at Bright red heat", with {@code what} one of the {@code heat.*} lines. */
    public static Component heat(String what, int temperature) {
        return key("heat." + what, bandName(temperature));
    }

    /** "12 s", "5 min" or "2 min 30 s". */
    public static Component time(int ticks) {
        int seconds = Math.max(1, Math.round(ticks / 20.0f));
        if (seconds < 60) return key("time.seconds", seconds);
        int minutes = seconds / 60, rest = seconds % 60;
        return rest == 0 ? key("time.minutes", minutes) : key("time.minutes_seconds", minutes, rest);
    }

    /** "88" or "0.5": a share in percent with at most one decimal. */
    public static String percent(float value) {
        float rounded = Math.round(value * 10) / 10.0f;
        return rounded == Math.floor(rounded) ? Integer.toString((int) rounded) : String.format(Locale.ROOT, "%.1f", rounded);
    }

    public static Component metal(dev.strataindustria.material.Metal metal) {
        return Component.translatable(StrataIndustria.MOD_ID + ".metal." + metal.id());
    }
}
