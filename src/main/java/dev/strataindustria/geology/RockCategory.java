package dev.strataindustria.geology;

import net.minecraft.util.StringRepresentable;

/** The four rock categories (shelves, tags, oil basins). Tool durability is per rock, see {@link Rock#toolDurability()}. */
public enum RockCategory implements StringRepresentable {
    IGNEOUS_EXTRUSIVE("igneous_extrusive"),
    IGNEOUS_INTRUSIVE("igneous_intrusive"),
    METAMORPHIC("metamorphic"),
    SEDIMENTARY("sedimentary");

    private final String name;

    RockCategory(String name) {
        this.name = name;
    }

    public boolean isIgneous() {
        return this == IGNEOUS_EXTRUSIVE || this == IGNEOUS_INTRUSIVE;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
