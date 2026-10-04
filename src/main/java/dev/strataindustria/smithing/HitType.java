package dev.strataindustria.smithing;

import java.util.Locale;

/** The eight anvil hits (spec 9.2) and how far each moves the workpiece. */
public enum HitType {
    LIGHT(-3),
    MEDIUM(-6),
    HARD(-9),
    DRAW(-15),
    PUNCH(2),
    BEND(7),
    UPSET(13),
    SHRINK(16);

    public static final HitType[] VALUES = values();

    private final int delta;

    HitType(int delta) {
        this.delta = delta;
    }

    public int delta() {
        return delta;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static HitType byId(int ordinal) {
        return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : null;
    }
}
