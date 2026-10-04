package dev.strataindustria.geology;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/** Ore grade. Lives on ore blocks as a blockstate and decides which ore piece drops; it sets yield, not quality. */
public enum OreGrade implements StringRepresentable {
    POOR("poor"),
    NORMAL("normal"),
    RICH("rich");

    public static final EnumProperty<OreGrade> PROPERTY = EnumProperty.create("grade", OreGrade.class);

    private final String name;

    OreGrade(String name) {
        this.name = name;
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
