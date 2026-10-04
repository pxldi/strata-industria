package dev.strataindustria.knapping;

/**
 * How a stone breaks under the hammerstone (uniqueness 3.2). Clean-grained stone flakes where it is
 * struck and holds a slightly better edge, coarse stone takes the blow dully, and crumbly stone sheds
 * extra chips with every blow.
 */
public enum Grain {
    CLEAN("clean", 1.2f, 1),
    COARSE("coarse", 0.8f, 1),
    CRUMBLY("crumbly", 0.95f, 2);

    private final String id;
    private final float strikePitch;
    private final int chipFactor;

    Grain(String id, float strikePitch, int chipFactor) {
        this.id = id;
        this.strikePitch = strikePitch;
        this.chipFactor = chipFactor;
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
}
