package dev.strataindustria.geology;

import net.minecraft.util.StringRepresentable;

/** The four rock categories. Knapped tools take their durability multiplier from these. */
public enum RockCategory implements StringRepresentable {
    IGNEOUS_EXTRUSIVE("igneous_extrusive", 1.2f),
    IGNEOUS_INTRUSIVE("igneous_intrusive", 1.0f),
    METAMORPHIC("metamorphic", 1.0f),
    SEDIMENTARY("sedimentary", 0.8f);

    private final String name;
    private final float durabilityMultiplier;

    RockCategory(String name, float durabilityMultiplier) {
        this.name = name;
        this.durabilityMultiplier = durabilityMultiplier;
    }

    public float durabilityMultiplier() {
        return durabilityMultiplier;
    }

    public boolean isIgneous() {
        return this == IGNEOUS_EXTRUSIVE || this == IGNEOUS_INTRUSIVE;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
