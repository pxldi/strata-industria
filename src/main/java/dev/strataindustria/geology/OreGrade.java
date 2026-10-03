package dev.strataindustria.geology;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/** Ore grade. Lives on ore blocks as a blockstate and decides which ore piece drops. */
public enum OreGrade implements StringRepresentable {
    POOR("poor", -10),
    NORMAL("normal", 0),
    RICH("rich", 10);

    public static final EnumProperty<OreGrade> PROPERTY = EnumProperty.create("grade", OreGrade.class);

    private final String name;
    private final int quality;

    OreGrade(String name, int quality) {
        this.name = name;
        this.quality = quality;
    }

    /** Material quality contribution (content spec 6.4). */
    public int quality() {
        return quality;
    }

    /** Item id prefix: {@code poor_}, empty, or {@code rich_}. */
    public String prefix() {
        return this == NORMAL ? "" : name + "_";
    }

    public OreGrade up() {
        return this == POOR ? NORMAL : RICH;
    }

    public OreGrade down() {
        return this == RICH ? NORMAL : POOR;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
