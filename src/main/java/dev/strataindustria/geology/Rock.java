package dev.strataindustria.geology;

import java.util.Locale;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.material.MapColor;

/** The M1 rock list (worldgen spec 3.1). Every rock registers raw, cobbled and loose blocks. */
public enum Rock implements StringRepresentable {
    LIMESTONE(RockCategory.SEDIMENTARY, 1.2f, MapColor.SAND),
    SHALE(RockCategory.SEDIMENTARY, 1.0f, MapColor.COLOR_GRAY),
    SLATE(RockCategory.METAMORPHIC, 1.6f, MapColor.COLOR_CYAN),
    MARBLE(RockCategory.METAMORPHIC, 1.6f, MapColor.QUARTZ),
    GRANITE(RockCategory.IGNEOUS_INTRUSIVE, 2.2f, MapColor.DIRT),
    GABBRO(RockCategory.IGNEOUS_INTRUSIVE, 2.4f, MapColor.COLOR_BLACK),
    BASALT(RockCategory.IGNEOUS_EXTRUSIVE, 2.0f, MapColor.DEEPSLATE),
    RHYOLITE(RockCategory.IGNEOUS_EXTRUSIVE, 1.8f, MapColor.TERRACOTTA_WHITE);

    private final RockCategory category;
    private final float hardness;
    private final MapColor mapColor;

    Rock(RockCategory category, float hardness, MapColor mapColor) {
        this.category = category;
        this.hardness = hardness;
        this.mapColor = mapColor;
    }

    public RockCategory category() {
        return category;
    }

    public float hardness() {
        return hardness;
    }

    public MapColor mapColor() {
        return mapColor;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String getSerializedName() {
        return id();
    }
}
