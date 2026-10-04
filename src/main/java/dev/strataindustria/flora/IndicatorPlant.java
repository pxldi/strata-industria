package dev.strataindustria.flora;

import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.geology.VeinType;
import java.util.List;
import java.util.Locale;

/**
 * The plants that only grow above one family of ore. Each names the minerals it marks and the journal lead that
 * studying it brings to mind ({@code null}: none yet).
 */
public enum IndicatorPlant {
    COPPER_FLOWER(List.of(OreMineral.NATIVE_COPPER, OreMineral.MALACHITE, OreMineral.TENNANTITE), "t1/nugget"),
    HORSETAIL(List.of(OreMineral.NATIVE_GOLD), null),
    STUNTED_BIRCH(List.of(OreMineral.SPHALERITE, OreMineral.GALENA, OreMineral.SULFUR, OreMineral.CINNABAR), "t4/sphalerite"),
    PINK_THRIFT(List.of(OreMineral.CASSITERITE, OreMineral.BISMUTHINITE), "t2/alloy_metal"),
    /** Grows in colonies on bare granite. Uranium arrives in tier 8; the plant is the early sign. */
    LOCOWEED(List.of(), null);

    private final List<OreMineral> minerals;
    private final String lead;

    IndicatorPlant(List<OreMineral> minerals, String lead) {
        this.minerals = minerals;
        this.lead = lead;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public List<OreMineral> minerals() {
        return minerals;
    }

    /** The lead path that studying this plant hints, or null. */
    public String lead() {
        return lead;
    }

    /** The plant that grows over a vein of this type, or null if it has none. */
    public static IndicatorPlant forVein(VeinType type) {
        if (!type.placesOre()) return null;
        for (IndicatorPlant plant : values()) {
            for (VeinType.MineralWeight weight : type.minerals()) {
                if (plant.minerals.contains(weight.mineral())) return plant;
            }
        }
        return null;
    }
}
