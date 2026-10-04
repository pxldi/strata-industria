package dev.strataindustria.metal;

import dev.strataindustria.material.Metal;
import java.util.Map;
import java.util.Optional;

/**
 * Alloy rules: a base metal and one alloying metal. Most alloys are fixed parts like GregTech's: 3 copper and
 * 1 tin make 4 bronze, whether the parts are ingots, nuggets or crushed ore; any multiple works. The
 * iron and carbon alloys stay share ranges, because carbon comes in dust. A melt of a single metal is that metal.
 */
public enum Alloy {
    BRONZE(Metal.BRONZE, Metal.COPPER, 3, Metal.TIN, 1),
    ARSENICAL_BRONZE(Metal.ARSENICAL_BRONZE, Metal.COPPER, 7, Metal.ARSENIC, 1),
    BISMUTH_BRONZE(Metal.BISMUTH_BRONZE, Metal.COPPER, 5, Metal.BISMUTH, 1),
    // Tier 4 spec 5: iron and carbon. Iron items carry their carbon only as a trace when remelted.
    WROUGHT_IRON(Metal.WROUGHT_IRON, Metal.WROUGHT_IRON, 0.995f, 1.0f, Metal.CARBON, 0.0f, 0.005f, 0f),
    STEEL(Metal.STEEL, Metal.WROUGHT_IRON, 0.98f, 0.995f, Metal.CARBON, 0.005f, 0.02f, 0.01f),
    PIG_IRON(Metal.PIG_IRON, Metal.WROUGHT_IRON, 0.94f, 0.97f, Metal.CARBON, 0.03f, 0.06f, 0.04f),
    BRASS(Metal.BRASS, Metal.COPPER, 2, Metal.ZINC, 1),
    SOLDER(Metal.SOLDER, Metal.TIN, 3, Metal.LEAD, 2),
    // Tier 5 spec 4.3: 1 copper ingot and 4 redstone (100 units each) make 2 red alloy.
    RED_ALLOY(Metal.RED_ALLOY, Metal.COPPER, 1, Metal.REDSTONE, 1);

    private final Metal result;
    private final Metal base;
    private final float baseMin, baseMax;
    private final Metal added;
    private final float addedMin, addedMax;
    private final float itemShare;
    /** Whole parts of base and added metal; 0 for the share-range alloys. */
    private final int baseParts, addedParts;
    /** How far off the exact ratio a mix may be (in share of the whole), for rounding in ore yields. */
    private static final float SLACK = 0.03f;

    Alloy(Metal result, Metal base, int baseParts, Metal added, int addedParts) {
        float share = addedParts / (float) (baseParts + addedParts);
        this.result = result;
        this.base = base;
        this.added = added;
        this.baseParts = baseParts;
        this.addedParts = addedParts;
        this.addedMin = share - SLACK;
        this.addedMax = share + SLACK;
        this.baseMin = 1 - addedMax;
        this.baseMax = 1 - addedMin;
        this.itemShare = share;
    }

    Alloy(Metal result, Metal base, float baseMin, float baseMax, Metal added, float addedMin, float addedMax) {
        this(result, base, baseMin, baseMax, added, addedMin, addedMax, (addedMin + addedMax) / 2);
    }

    Alloy(Metal result, Metal base, float baseMin, float baseMax, Metal added, float addedMin, float addedMax, float itemShare) {
        this.baseParts = 0;
        this.addedParts = 0;
        this.itemShare = itemShare;
        this.result = result;
        this.base = base;
        this.baseMin = baseMin;
        this.baseMax = baseMax;
        this.added = added;
        this.addedMin = addedMin;
        this.addedMax = addedMax;
    }

    public Metal result() {
        return result;
    }

    public Metal base() {
        return base;
    }

    public Metal added() {
        return added;
    }

    /** Whole parts of the base metal, or 0 when this alloy is a share range. */
    public int baseParts() {
        return baseParts;
    }

    public int addedParts() {
        return addedParts;
    }

    public boolean byParts() {
        return baseParts > 0;
    }

    /**
     * What a part mix needs to be this alloy: the metal to add and how many units of it. Empty for share-range
     * alloys and for a mix that already is the alloy.
     */
    public Optional<Missing> missing(Melt melt) {
        if (!byParts() || matches(melt)) return Optional.empty();
        int base = melt.units().getOrDefault(this.base, 0), add = melt.units().getOrDefault(added, 0);
        // Adding more of the short metal is the smaller step.
        int needAdded = Math.round(base * addedParts / (float) baseParts) - add;
        if (needAdded > 0) return Optional.of(new Missing(added, needAdded));
        int needBase = Math.round(add * baseParts / (float) addedParts) - base;
        return needBase > 0 ? Optional.of(new Missing(this.base, needBase)) : Optional.empty();
    }

    /** Units of a metal to add. */
    public record Missing(Metal metal, int units) {}

    public float addedMin() {
        return addedMin;
    }

    public float addedMax() {
        return addedMax;
    }

    /**
     * What an alloy item melts back into: its metals at the middle of the alloy's range, so a remelted
     * bronze ingot is bronze again and mixes with fresh copper and tin. Plain metals melt as themselves.
     */
    public static Melt parts(Metal metal, int units) {
        for (Alloy alloy : values()) {
            if (alloy.result != metal || alloy.itemShare <= 0) continue;
            int added = Math.round(units * alloy.itemShare);
            if (added <= 0) return Melt.of(alloy.base, units);
            return new Melt(Map.of(alloy.base, units - added, alloy.added, added));
        }
        return Melt.of(metal, units);
    }

    boolean matches(Melt melt) {
        if (melt.units().size() != 2) return false;
        float b = melt.share(base), a = melt.share(added);
        // A small tolerance so a mix that reads as the edge percentage in the screen counts.
        return b >= baseMin - 0.0005f && b <= baseMax + 0.0005f && a >= addedMin - 0.0005f && a <= addedMax + 0.0005f;
    }

    /** What the melt is: a single metal, an alloy, or nothing known. */
    public static Optional<Metal> resultOf(Melt melt) {
        if (melt.isEmpty()) return Optional.empty();
        Map<Metal, Integer> units = melt.units();
        if (units.size() == 1) {
            Metal only = units.keySet().iterator().next();
            // Arsenic never exists on its own outside a melt (spec 6.1).
            return only.dissolvedOnly() ? Optional.empty() : Optional.of(only);
        }
        for (Alloy alloy : values()) if (alloy.matches(melt)) return Optional.of(alloy.result);
        return Optional.empty();
    }

    /**
     * The alloy this mix is nearest to, for the hint line: of the alloys whose metals are both present,
     * the one whose range for the alloying metal is closest. Pig iron is left out: it comes from the
     * blast furnace, and iron with too much carbon is aiming for steel.
     */
    public static Optional<Alloy> closest(Melt melt) {
        Alloy best = null;
        float bestDistance = Float.MAX_VALUE;
        for (Alloy alloy : values()) {
            if (alloy.result == Metal.PIG_IRON || alloy.result == alloy.base) continue;
            float share = melt.share(alloy.added);
            if (melt.share(alloy.base) <= 0 || share <= 0) continue;
            float distance = share < alloy.addedMin ? alloy.addedMin - share : share > alloy.addedMax ? share - alloy.addedMax : 0;
            if (distance < bestDistance) {
                best = alloy;
                bestDistance = distance;
            }
        }
        return Optional.ofNullable(best);
    }
}
