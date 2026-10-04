package dev.strataindustria.survey;

import dev.strataindustria.StrataIndustria;
import net.minecraft.network.chat.Component;

/** The words on a sheet of survey notes (structures spec 5.2). Shared by the tooltip and the screen. */
public final class SurveyText {
    static final String PREFIX = "item." + StrataIndustria.MOD_ID + ".survey_notes.";
    static final String[] COMPASS = {"north", "north_east", "east", "south_east", "south", "south_west", "west", "north_west"};

    private SurveyText() {}

    public static Component mineralName(String mineral) {
        return Component.translatable("item." + StrataIndustria.MOD_ID + "." + mineral);
    }

    /** One of eight compass points, 0 = north, clockwise. */
    public static int compassIndex(double dx, double dz) {
        double angle = Math.toDegrees(Math.atan2(dx, -dz));
        return Math.floorMod((int) Math.round(angle / 45.0), 8);
    }

    public static Component direction(double dx, double dz) {
        return Component.translatable(PREFIX + "dir." + COMPASS[compassIndex(dx, dz)]);
    }

    /** "~250", rounded to 50 so the notes stay a sketch, not a map. */
    public static Component distance(double dx, double dz) {
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d < 32) return Component.translatable(PREFIX + "distance.here");
        int rounded = Math.max(50, (int) Math.round(d / 50.0) * 50);
        return Component.translatable(PREFIX + "distance", rounded);
    }

    /** Where to look and how far: "NE, ~250". */
    public static Component bearing(double dx, double dz) {
        return Component.translatable(PREFIX + "bearing", direction(dx, dz), distance(dx, dz));
    }

    public static Component depth(int depth) {
        String key = depth <= 0 ? "surface" : depth <= 6 ? "just_under" : depth <= 20 ? "little_down" : depth <= 48 ? "deep" : "very_deep";
        return Component.translatable(PREFIX + "depth." + key);
    }

    /** "In granite, shallow": the host rock and how far down, on one line. */
    public static Component where(String rock, int depth) {
        return Component.translatable(PREFIX + "where",
                Component.translatable(StrataIndustria.MOD_ID + ".knapped_from.material." + rock), depth(depth));
    }

    /** The writer's own line about this mineral; one of {@link Surveyor#HANDS}. */
    public static Component hand(String mineral, int hand) {
        return Component.translatable(PREFIX + "hand." + mineral + "." + Math.floorMod(hand, Surveyor.HANDS));
    }
}
