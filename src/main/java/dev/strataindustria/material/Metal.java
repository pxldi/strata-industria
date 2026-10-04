package dev.strataindustria.material;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Locale;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/**
 * Metals of tiers 0 to 2 (content spec 6.1). Item forms are generated from this list, so it lives in
 * code; alloy rules are data ({@code strataindustria:alloy} recipes).
 */
public enum Metal implements StringRepresentable {
    COPPER(1085, 1, 0xC8743A, Forms.TOOL_METAL, true),
    TIN(232, 1, 0xD6D6CC, Forms.STORAGE, false),
    BISMUTH(271, 1, 0xB4A6B8, Forms.STORAGE, false),
    ARSENIC(615, 1, 0x8A8F86, Forms.NONE, false),
    BRONZE(950, 2, 0xC5933C, Forms.TOOL_METAL, false),
    ARSENICAL_BRONZE(1010, 2, 0xB07A50, Forms.TOOL_METAL, false),
    BISMUTH_BRONZE(985, 2, 0xB48A6A, Forms.TOOL_METAL, false),
    SLAG_METAL(1000, 0, 0x6E625A, Forms.INGOT_ONLY, false),
    /** Tier 3. Its ingot and nugget are the vanilla iron items; it never melts in tier 3. */
    WROUGHT_IRON(1538, 3, 0x6A6A6C, Forms.TOOL_METAL, true),
    /** Gold works like copper: it melts, casts and smiths on a stone anvil. */
    GOLD(1064, 1, 0xC08A26, Forms.TOOL_METAL, true),
    // Tier 4 spec 4.1. Colours are each ramp's base step (spec 21.1).
    /** Cast from the blast furnace; too brittle to smith. */
    PIG_IRON(1200, 4, 0x45403C, Forms.INGOT_ONLY, false),
    STEEL(1450, 4, 0x5C636E, Forms.TOOL_METAL, false),
    ZINC(420, 3, 0x6E767E, Forms.STORAGE, false),
    LEAD(327, 3, 0x444A5C, Forms.STORAGE, false),
    /** The fittings metal: plates, rods and gears, but no tools. */
    BRASS(930, 3, 0xA8862E, Forms.PARTS, false),
    SOLDER(190, 3, 0x737A7C, Forms.INGOT_ONLY, false),
    /** Exists only dissolved in a melt, like arsenic. */
    CARBON(0, 0, 0x2A2A2A, Forms.NONE, false);

    public static final Codec<Metal> CODEC = StringRepresentable.fromEnum(Metal::values);
    public static final StreamCodec<ByteBuf, Metal> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(i -> values()[i], Metal::ordinal);

    /** PARTS: ingot, nugget, plate, rod and gear, but no tools (brass). */
    public enum Forms { NONE, INGOT_ONLY, STORAGE, PARTS, TOOL_METAL }

    private final int meltingPoint;
    private final int tier;
    private final int colour;
    private final Forms forms;
    private final boolean vanilla;

    Metal(int meltingPoint, int tier, int colour, Forms forms, boolean vanilla) {
        this.meltingPoint = meltingPoint;
        this.tier = tier;
        this.colour = colour;
        this.forms = forms;
        this.vanilla = vanilla;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public int meltingPoint() {
        return meltingPoint;
    }

    /** Working temperature for smithing: 60% of the melting point. */
    public int workingTemperature() {
        return Math.round(meltingPoint * 0.6f);
    }

    /** Welding temperature: 80% of the melting point (unused until tier 3). */
    public int weldingTemperature() {
        return Math.round(meltingPoint * 0.8f);
    }

    public int tier() {
        return tier;
    }

    public int colour() {
        return colour;
    }

    /** True for copper, wrought iron and gold, whose ingot and nugget are the vanilla items. */
    public boolean isVanilla() {
        return vanilla;
    }

    public boolean hasIngot() {
        return forms != Forms.NONE;
    }

    public boolean hasNugget() {
        return forms == Forms.STORAGE || forms == Forms.PARTS || forms == Forms.TOOL_METAL;
    }

    /** Plates: every tool metal and brass. */
    public boolean hasPlate() {
        return forms == Forms.PARTS || forms == Forms.TOOL_METAL;
    }

    /** Generic rods (tier 4 spec 4.1): steel and brass. Wrought iron has its own tier 3 rod item. */
    public boolean hasRod() {
        return this == STEEL || this == BRASS;
    }

    /** Gears (tier 4 spec 6.1): brass, steel and the bronzes. */
    public boolean hasGear() {
        return this == STEEL || this == BRASS || isBronze();
    }

    /** Plates, tool heads and tools. */
    public boolean isToolMetal() {
        return forms == Forms.TOOL_METAL;
    }

    public boolean isBronze() {
        return this == BRONZE || this == ARSENICAL_BRONZE || this == BISMUTH_BRONZE;
    }

    /** The bronzes and wrought iron get a prospector's pick. */
    public boolean hasProspectorsPick() {
        return isBronze() || this == WROUGHT_IRON;
    }

    /** Which heads and tools this metal makes. Gold makes only the five vanilla golden tools. */
    public java.util.List<dev.strataindustria.ceramics.MoldType> toolTypes() {
        if (!isToolMetal()) return java.util.List.of();
        if (this == GOLD) {
            return java.util.List.of(dev.strataindustria.ceramics.MoldType.PICKAXE_HEAD, dev.strataindustria.ceramics.MoldType.AXE_HEAD,
                    dev.strataindustria.ceramics.MoldType.SHOVEL_HEAD, dev.strataindustria.ceramics.MoldType.HOE_HEAD,
                    dev.strataindustria.ceramics.MoldType.SWORD_BLADE);
        }
        return java.util.List.of(dev.strataindustria.ceramics.MoldType.values());
    }

    /** Whether a clay crucible gets hot enough to melt it (tier 3 spec 4.1: iron does not). */
    public boolean meltsInCrucible() {
        return meltingPoint <= 1400;
    }

    /** Metals that only exist dissolved in a melt and never pour on their own: arsenic and carbon. */
    public boolean dissolvedOnly() {
        return this == ARSENIC || this == CARBON;
    }

    @Override
    public String getSerializedName() {
        return id();
    }
}
