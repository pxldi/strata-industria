package dev.strataindustria.geology;

import dev.strataindustria.material.Metal;
import java.util.Locale;
import java.util.Map;
import net.minecraft.util.StringRepresentable;

/** Ore minerals of M1 (worldgen spec 5.1, content spec 6.2). */
public enum OreMineral implements StringRepresentable {
    NATIVE_COPPER(Map.of(Metal.COPPER, 1.0f), new int[] {20, 35, 50}, false),
    MALACHITE(Map.of(Metal.COPPER, 1.0f), new int[] {15, 30, 45}, false),
    TENNANTITE(Map.of(Metal.COPPER, 0.8f, Metal.ARSENIC, 0.2f), new int[] {20, 35, 50}, false),
    CASSITERITE(Map.of(Metal.TIN, 1.0f), new int[] {20, 35, 50}, true),
    BISMUTHINITE(Map.of(Metal.BISMUTH, 1.0f), new int[] {20, 35, 50}, false);

    /** Raw (uncrushed) ore melts at this share of the crushed value. */
    public static final float RAW_MELT_EFFICIENCY = 0.8f;
    /** Units of a surface nugget. */
    public static final int SMALL_ORE_UNITS = 10;

    private final Map<Metal, Float> composition;
    private final int[] crushedUnits;
    private final boolean needsCopperTool;

    OreMineral(Map<Metal, Float> composition, int[] crushedUnits, boolean needsCopperTool) {
        this.composition = composition;
        this.crushedUnits = crushedUnits;
        this.needsCopperTool = needsCopperTool;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Metal share by weight, summing to 1. */
    public Map<Metal, Float> composition() {
        return composition;
    }

    /** Metal units of one crushed ore piece of this grade. */
    public int crushedUnits(OreGrade grade) {
        return crushedUnits[grade.ordinal()];
    }

    public boolean needsCopperTool() {
        return needsCopperTool;
    }

    /** The main metal, used for tags such as {@code c:ores/copper}. */
    public Metal primaryMetal() {
        return composition.entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow().getKey();
    }

    @Override
    public String getSerializedName() {
        return id();
    }
}
