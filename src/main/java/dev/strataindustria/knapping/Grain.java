package dev.strataindustria.knapping;

/**
 * How a stone breaks under the hammerstone (uniqueness 3.2). Clean-grained stone flakes where it is
 * struck and holds a slightly better edge, coarse stone takes the blow dully, and crumbly stone sometimes
 * loses a second cell beside the one struck.
 */
public enum Grain {
    CLEAN("clean", 1.2f, 0f, 1.1f),
    COARSE("coarse", 0.8f, 0f, 1.0f),
    CRUMBLY("crumbly", 0.95f, 0.15f, 1.0f);

    private final String id;
    private final float strikePitch;
    private final float crumbleChance;
    private final float durabilityMultiplier;

    Grain(String id, float strikePitch, float crumbleChance, float durabilityMultiplier) {
        this.id = id;
        this.strikePitch = strikePitch;
        this.crumbleChance = crumbleChance;
        this.durabilityMultiplier = durabilityMultiplier;
    }

    public String id() {
        return id;
    }

    /** Base pitch of the strike sound: clean stone rings higher, coarse stone lower. */
    public float strikePitch() {
        return strikePitch;
    }

    /** Chance that a strike also takes off a kept cell next to it. */
    public float crumbleChance() {
        return crumbleChance;
    }

    /** Applied on top of the rock category's durability multiplier. */
    public float durabilityMultiplier() {
        return durabilityMultiplier;
    }
}
