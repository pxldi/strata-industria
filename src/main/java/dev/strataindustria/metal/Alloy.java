package dev.strataindustria.metal;

import dev.strataindustria.material.Metal;
import java.util.Map;
import java.util.Optional;

/**
 * Alloy rules (spec 7.2): a base metal and one alloying metal, each within a share range. A melt
 * of a single metal is that metal.
 */
public enum Alloy {
    BRONZE(Metal.BRONZE, Metal.COPPER, 0.88f, 0.92f, Metal.TIN, 0.08f, 0.12f),
    ARSENICAL_BRONZE(Metal.ARSENICAL_BRONZE, Metal.COPPER, 0.88f, 0.94f, Metal.ARSENIC, 0.06f, 0.12f),
    BISMUTH_BRONZE(Metal.BISMUTH_BRONZE, Metal.COPPER, 0.85f, 0.90f, Metal.BISMUTH, 0.10f, 0.15f);

    private final Metal result;
    private final Metal base;
    private final float baseMin, baseMax;
    private final Metal added;
    private final float addedMin, addedMax;

    Alloy(Metal result, Metal base, float baseMin, float baseMax, Metal added, float addedMin, float addedMax) {
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

    public Metal added() {
        return added;
    }

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
            if (alloy.result != metal) continue;
            int added = Math.round(units * (alloy.addedMin + alloy.addedMax) / 2);
            return new Melt(Map.of(alloy.base, units - added, alloy.added, added), 0);
        }
        return Melt.of(metal, units, 0);
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
            return only == Metal.ARSENIC ? Optional.empty() : Optional.of(only);
        }
        for (Alloy alloy : values()) if (alloy.matches(melt)) return Optional.of(alloy.result);
        return Optional.empty();
    }

    /** The alloy this mix is nearest to, for the hint line: the one whose alloying metal is present. */
    public static Optional<Alloy> closest(Melt melt) {
        Alloy best = null;
        float bestShare = 0;
        for (Alloy alloy : values()) {
            float share = melt.share(alloy.added);
            if (melt.share(alloy.base) > 0 && share > bestShare) {
                best = alloy;
                bestShare = share;
            }
        }
        return Optional.ofNullable(best);
    }
}
