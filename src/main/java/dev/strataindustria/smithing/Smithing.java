package dev.strataindustria.smithing;

import dev.strataindustria.Config;
import dev.strataindustria.material.Metal;
import net.minecraft.util.Mth;

/**
 * Anvil striking numbers (redesign L2): how many blows a shape takes, when metal is bright, the craft part
 * a piece earns from its bright blows, and the five-note scale each blow plays.
 */
public final class Smithing {
    /** Ticks from a blow to the glint at the top of the hammer's rebound. */
    public static final int BEAT_TICKS = 10;
    /** Clicks closer together than this (and not a held series) are ignored. */
    public static final int MIN_GAP_TICKS = 6;
    /** A held button repeats every few ticks; a blow lands at most this often while holding. */
    public static final int HOLD_GAP_TICKS = 10;
    /** Raw clicks this close together are a held button, never a timed strike. */
    public static final int HOLD_REPEAT_TICKS = 5;
    /** Past this many ticks since the last blow the rhythm is gone. */
    public static final int RHYTHM_TIMEOUT_TICKS = 30;
    /** Best craft part a bright run earns. */
    public static final int MAX_CRAFT = 4;
    /** Blows the weld shape takes. */
    public static final int WELD_BLOWS = 3;

    /** The scale a piece climbs, one note per stretch of the work (a major pentatonic). */
    private static final float[] NOTES = {0.76f, 0.85f, 0.96f, 1.14f, 1.28f};
    /** The home note the last blow lands on, an octave above the first. */
    private static final float HOME = 1.52f;

    private Smithing() {}

    /** Blows to finish a shape in this metal: the recipe's own, one more for iron, two more for steel and past it. */
    public static int totalBlows(int recipeBlows, Metal metal) {
        int extra = metal == null ? 0 : metal.tier() >= 4 ? 2 : metal.tier() == 3 ? 1 : 0;
        return Math.max(1, recipeBlows + extra);
    }

    /** The temperature from which a blow counts as bright. */
    public static float brightFrom(int workingTemperature) {
        return workingTemperature * (1.0f + Config.SMITHING_BRIGHT_MARGIN.get() / 100.0f);
    }

    /** Craft part from the share of bright blows: 0 to {@link #MAX_CRAFT}. */
    public static int craftQuality(int bright, int blows) {
        if (blows <= 0) return 0;
        return Mth.clamp(Math.round(MAX_CRAFT * bright / (float) blows), 0, MAX_CRAFT);
    }

    /** The note at {@code index} on the scale, wrapping round: what the shape button plays as it steps through shapes. */
    public static float scaleNote(int index) {
        return NOTES[Math.floorMod(index, NOTES.length)];
    }

    /** Pitch of the note for a blow when {@code done} of {@code total} blows are already in; the finish takes the home note. */
    public static float notePitch(int done, int total, boolean finish, boolean bright) {
        float pitch = finish ? HOME : NOTES[Mth.clamp(done * NOTES.length / Math.max(1, total), 0, NOTES.length - 1)];
        return bright ? pitch : pitch * 0.89f;
    }

    /** Whether a metal's heat is in the glowing top band. */
    public static boolean isBright(float temperature, int workingTemperature) {
        return temperature >= brightFrom(workingTemperature);
    }
}
