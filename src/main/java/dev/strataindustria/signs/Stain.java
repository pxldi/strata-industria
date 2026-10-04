package dev.strataindustria.signs;

import dev.strataindustria.geology.OreMineral;
import java.util.Locale;
import net.minecraft.util.StringRepresentable;

/**
 * Colour the ground takes over shallow veins (redesign R7): rust-red gossan over iron, green malachite bloom
 * over copper, a yellow crust over native sulfur. They are overlays on the normal ground, and the same colours
 * coat some boulders ({@link dev.strataindustria.block.BoulderBlock#COAT}).
 */
public enum Stain implements StringRepresentable {
    GOSSAN(0xA8532F),
    MALACHITE_BLOOM(0x3F9A6A),
    SULFUR_CRUST(0xD8C83C);

    private final int color;

    Stain(int color) {
        this.color = color;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The colour of the dust a rub or a blow throws up. */
    public int color() {
        return color;
    }

    /** The stain a vein of this mineral leaves on the surface, or null (tin shows as black sand in streams instead). */
    public static Stain forMineral(OreMineral mineral) {
        if (mineral.isIron()) return GOSSAN;
        if (mineral.composition().containsKey(dev.strataindustria.material.Metal.COPPER)) return MALACHITE_BLOOM;
        if (mineral == OreMineral.SULFUR) return SULFUR_CRUST;
        return null;
    }

    @Override
    public String getSerializedName() {
        return id();
    }
}
