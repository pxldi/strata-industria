package dev.strataindustria.heat;

import dev.strataindustria.StrataIndustria;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;

/**
 * Heat bands (tier 0-2 spec 5.3). The player reads heat by band name and colour, never by number; the
 * colours are the style guide's heat glow scale, used for tooltips, gauges and hot items alike.
 */
public enum HeatBand {
    NONE("none", Float.NEGATIVE_INFINITY, 0x707070),
    WARMING("warming", 80, 0x8a6a5a),
    HOT("hot", 210, 0x9a5038),
    VERY_HOT("very_hot", 480, 0x8a3420),
    FAINT_RED("faint_red", 580, 0x6e1e14),
    DARK_RED("dark_red", 730, 0xa0281a),
    BRIGHT_RED("bright_red", 930, 0xd23a1e),
    ORANGE("orange", 1100, 0xf07a22),
    YELLOW("yellow", 1300, 0xf8c23a),
    WHITE("white", 1400, 0xfff4d0);

    private final String id;
    private final float from;
    private final int colour;

    HeatBand(String id, float from, int colour) {
        this.id = id;
        this.from = from;
        this.colour = colour;
    }

    public static HeatBand of(float temperature) {
        HeatBand band = NONE;
        for (HeatBand b : values()) if (temperature >= b.from) band = b;
        return band;
    }

    public float from() {
        return from;
    }

    public int colour() {
        return colour;
    }

    /** "Very hot", in the band's colour. */
    public Component displayName() {
        return Component.translatable(StrataIndustria.MOD_ID + ".heat." + id).withStyle(s -> s.withColor(TextColor.fromRgb(colour)));
    }

    public String id() {
        return id;
    }
}
