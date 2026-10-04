package dev.strataindustria.geology;

import dev.strataindustria.material.Metal;
import java.util.Locale;
import java.util.Map;
import net.minecraft.util.StringRepresentable;

/** Ore minerals (worldgen spec 5.1, content spec 6.2; tier 3 spec 4.2). */
public enum OreMineral implements StringRepresentable {
    NATIVE_COPPER(Map.of(Metal.COPPER, 1.0f), new int[] {20, 35, 50}, 0, true),
    MALACHITE(Map.of(Metal.COPPER, 1.0f), new int[] {15, 30, 45}, 0, true),
    TENNANTITE(Map.of(Metal.COPPER, 0.8f, Metal.ARSENIC, 0.2f), new int[] {20, 35, 50}, 0, true),
    CASSITERITE(Map.of(Metal.TIN, 1.0f), new int[] {20, 35, 50}, 1, true),
    BISMUTHINITE(Map.of(Metal.BISMUTH, 1.0f), new int[] {20, 35, 50}, 0, true),
    /** Bog iron: a soil block, not an ore in rock, so it has no per-rock blocks. */
    LIMONITE(Map.of(Metal.WROUGHT_IRON, 1.0f), new int[] {15, 25, 35}, 2, false),
    HEMATITE(Map.of(Metal.WROUGHT_IRON, 1.0f), new int[] {20, 35, 50}, 2, true),
    MAGNETITE(Map.of(Metal.WROUGHT_IRON, 1.0f), new int[] {25, 40, 55}, 2, true),
    NATIVE_GOLD(Map.of(Metal.GOLD, 1.0f), new int[] {20, 35, 50}, 2, true),
    // Tier 4 spec 4.4: zinc and lead, then two deposits that drop an item instead of ore pieces.
    /** A sulfide: never melts as ore, it is roasted to zinc calcine first. */
    SPHALERITE(Map.of(Metal.ZINC, 1.0f), new int[] {20, 35, 50}, 3, true),
    GALENA(Map.of(Metal.LEAD, 1.0f), new int[] {20, 35, 50}, 3, true),
    BITUMINOUS_COAL(Map.of(), new int[] {0, 0, 0}, 3, true),
    SULFUR(Map.of(), new int[] {0, 0, 0}, 3, true);

    /** Raw (uncrushed) ore melts at this share of the crushed value. */
    public static final float RAW_MELT_EFFICIENCY = 0.8f;
    /** Washed ore melts to this share of the crushed value (tier 3 spec 11.2). */
    public static final float WASHED_BONUS = 1.1f;
    /** Units of a surface nugget. */
    public static final int SMALL_ORE_UNITS = 10;

    private final Map<Metal, Float> composition;
    private final int[] crushedUnits;
    /** 0 = stone tools, 1 = copper, 2 = bronze, 3 = wrought iron. */
    private final int toolTier;
    private final boolean inRock;

    OreMineral(Map<Metal, Float> composition, int[] crushedUnits, int toolTier, boolean inRock) {
        this.composition = composition;
        this.crushedUnits = crushedUnits;
        this.toolTier = toolTier;
        this.inRock = inRock;
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

    /** Needs a copper tool or better. */
    public boolean needsCopperTool() {
        return toolTier >= 1;
    }

    /** Needs a bronze tool or better. */
    public boolean needsBronzeTool() {
        return toolTier >= 2;
    }

    /** Needs a wrought iron tool or better (tier 4 spec 4.7). */
    public boolean needsWroughtIronTool() {
        return toolTier >= 3;
    }

    /**
     * Metal ores break into graded ore pieces that crush, wash and melt. Bituminous coal and native
     * sulfur drop a plain item instead (tier 4 spec 4.4).
     */
    public boolean hasPieces() {
        return !composition.isEmpty();
    }

    /** Minerals with ore pieces. */
    public static java.util.List<OreMineral> withPieces() {
        return java.util.Arrays.stream(values()).filter(OreMineral::hasPieces).toList();
    }

    /** Whether crushed pieces can be washed; sphalerite is refused (tier 4 spec 11.3). */
    public boolean washable() {
        return hasPieces() && this != SPHALERITE;
    }

    /** Minerals with washed forms. */
    public static java.util.List<OreMineral> washableValues() {
        return java.util.Arrays.stream(values()).filter(OreMineral::washable).toList();
    }

    /** A sulfide ore that has to be roasted before it melts (tier 4 spec 5.3). */
    public boolean isSulfide() {
        return this == SPHALERITE;
    }

    /** Whether the mineral has an ore block in every rock; limonite only forms in soil. */
    public boolean inRock() {
        return inRock;
    }

    /** Minerals with ore blocks in rock. */
    public static java.util.List<OreMineral> inRockValues() {
        return java.util.Arrays.stream(values()).filter(OreMineral::inRock).toList();
    }

    /** Iron ores: limonite, hematite and magnetite. */
    public boolean isIron() {
        return composition.containsKey(Metal.WROUGHT_IRON);
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
