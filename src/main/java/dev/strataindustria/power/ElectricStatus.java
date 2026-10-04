package dev.strataindustria.power;

import dev.strataindustria.StrataIndustria;
import java.util.Locale;

/** What one device on an electric network is doing, set by the network each tick (spec 6.5). */
public enum ElectricStatus {
    /** Not part of a network yet, or the network has no source. */
    NO_SOURCE,
    /** A consumer fully supplied, or a source giving power. */
    RUNNING,
    /** A consumer given less than it asked for. */
    LOW_POWER,
    /** A consumer that wants nothing, or a source nothing draws on. */
    IDLE,
    /** A device below the network's tier. */
    OVERVOLTAGE,
    /** A cable below the network's tier stops the whole network. */
    CABLE_OVERVOLTAGE,
    /** A consumer whose path loses half its power or more. */
    TOO_FAR,
    /** The network is larger than {@code electric.maxNetworkSize}. */
    TOO_LARGE;

    public String key() {
        return StrataIndustria.MOD_ID + ".electric.status." + name().toLowerCase(Locale.ROOT);
    }

    /** A fault the player has to fix, shown red on status lights. */
    public boolean fault() {
        return this == OVERVOLTAGE || this == CABLE_OVERVOLTAGE || this == TOO_FAR || this == TOO_LARGE;
    }
}
