package dev.strataindustria.knapping;

/**
 * How a stone breaks under the hammerstone (uniqueness 3.2). Clean-grained stone flakes where it is
 * struck and holds a slightly better edge, coarse stone takes the blow dully, and crumbly stone sheds
 * extra chips with every blow.
 */
public enum Grain {
    CLEAN("clean", 1.2f, 1, 1.1f),
    COARSE("coarse", 0.8f, 1, 1.0f),
    CRUMBLY("crumbly", 0.95f, 2, 1.0f);

    private final String id;
    private final float strikePitch;
    private final int chipFactor;
    private final float durabilityMultiplier;

    Grain(String id, float strikePitch, int chipFactor, float durabilityMultiplier) {
        this.id = id;
        this.strikePitch = strikePitch;
        this.chipFactor = chipFactor;
        this.durabilityMultiplier = durabilityMultiplier;
    }

    public String id() {
        return id;
    }

    /** Base pitch of the strike sound: clean stone rings higher, coarse stone lower. */
    public float strikePitch() {
        return strikePitch;
    }

    /** How many times the usual number of chips a blow throws off. */
    public int chipFactor() {
        return chipFactor;
    }

    /** Applied on top of the rock category's durability multiplier. */
    public float durabilityMultiplier() {
        return durabilityMultiplier;
    }
}
