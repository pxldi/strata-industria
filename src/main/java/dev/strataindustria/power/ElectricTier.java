package dev.strataindustria.power;

import java.util.Locale;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Voltage tiers (tier 5 spec 0 and 6.2). A tier is only a label for the most power one device of
 * that tier handles; screens show the label and J/t, never volts or amps. HV and up come with tier 6.
 */
public enum ElectricTier implements StringRepresentable {
    LV(32, 128, 0.0025),
    MV(128, 512, 0.001);

    /** The blockstate {@code tier} on every device that comes in LV and MV. */
    public static final EnumProperty<ElectricTier> PROPERTY = EnumProperty.create("tier", ElectricTier.class);

    private final int maxPower;
    private final int cableCapacity;
    private final double cableLoss;

    ElectricTier(int maxPower, int cableCapacity, double cableLoss) {
        this.maxPower = maxPower;
        this.cableCapacity = cableCapacity;
        this.cableLoss = cableLoss;
    }

    /** Most J/t a generator, storage block or machine of this tier handles. */
    public int maxPower() {
        return maxPower;
    }

    /** J/t a cable of this tier carries (spec 6.2). */
    public int cableCapacity() {
        return cableCapacity;
    }

    /** Fraction of the power lost per block of cable of this tier (spec 6.3). */
    public double cableLoss() {
        return cableLoss;
    }

    /** "LV" or "MV". */
    public String label() {
        return name();
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
