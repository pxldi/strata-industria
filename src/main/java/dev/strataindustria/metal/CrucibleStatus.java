package dev.strataindustria.metal;

/** The crucible screen's status line (spec 7.1). */
public enum CrucibleStatus {
    COLD, HEATING, MELTING, MOLTEN, SOLID, NO_FORGE;

    public String key() {
        return "strataindustria.crucible.status." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
