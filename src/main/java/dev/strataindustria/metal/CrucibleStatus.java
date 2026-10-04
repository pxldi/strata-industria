package dev.strataindustria.metal;

/** The crucible screen's status line (spec 7.1). */
public enum CrucibleStatus {
    COLD, HEATING, MELTING, MOLTEN, SOLID, NO_FORGE,
    // Tier 4 spec 3 and 4.2.
    AT_LIMIT, CARBON_WAITING, CARBON_BURNED;

    public String key() {
        return "strataindustria.crucible.status." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
