package dev.strataindustria.geology;

import dev.strataindustria.knapping.Grain;
import java.util.Locale;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.material.MapColor;

/** The M1 rock list (worldgen spec 3.1). Every rock registers raw, cobbled and loose blocks. */
public enum Rock implements StringRepresentable {
    LIMESTONE(RockCategory.SEDIMENTARY, 1.2f, MapColor.SAND, Grain.CRUMBLY, 0.8f),
    SHALE(RockCategory.SEDIMENTARY, 1.0f, MapColor.COLOR_GRAY, Grain.CRUMBLY, 0.7f),
    SLATE(RockCategory.METAMORPHIC, 1.6f, MapColor.COLOR_CYAN, Grain.CRUMBLY, 0.9f),
    MARBLE(RockCategory.METAMORPHIC, 1.6f, MapColor.QUARTZ, Grain.COARSE, 0.95f),
    GRANITE(RockCategory.IGNEOUS_INTRUSIVE, 2.2f, MapColor.DIRT, Grain.COARSE, 1.2f),
    GABBRO(RockCategory.IGNEOUS_INTRUSIVE, 2.4f, MapColor.COLOR_BLACK, Grain.COARSE, 1.3f),
    BASALT(RockCategory.IGNEOUS_EXTRUSIVE, 2.0f, MapColor.DEEPSLATE, Grain.COARSE, 1.25f),
    RHYOLITE(RockCategory.IGNEOUS_EXTRUSIVE, 1.8f, MapColor.TERRACOTTA_WHITE, Grain.CLEAN, 1.4f);

    private final RockCategory category;
    private final float hardness;
    private final MapColor mapColor;
    private final Grain grain;
    private final float toolDurability;

    Rock(RockCategory category, float hardness, MapColor mapColor, Grain grain, float toolDurability) {
        this.grain = grain;
        this.toolDurability = toolDurability;
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

    /** How it breaks when knapped. */
    public Grain grain() {
        return grain;
    }

    /** What a tool struck from this rock lasts, as a multiple of the base stone tool durability. */
    public float toolDurability() {
        return toolDurability;
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
