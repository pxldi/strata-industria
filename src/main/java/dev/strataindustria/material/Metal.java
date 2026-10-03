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
    SLAG_METAL(1000, 0, 0x6E625A, Forms.INGOT_ONLY, false);

    public static final Codec<Metal> CODEC = StringRepresentable.fromEnum(Metal::values);
    public static final StreamCodec<ByteBuf, Metal> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(i -> values()[i], Metal::ordinal);

    public enum Forms { NONE, INGOT_ONLY, STORAGE, TOOL_METAL }

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

    /** True for copper, whose ingot and nugget are the vanilla items. */
    public boolean isVanilla() {
        return vanilla;
    }

    public boolean hasIngot() {
        return forms != Forms.NONE;
    }

    public boolean hasNugget() {
        return forms == Forms.STORAGE || forms == Forms.TOOL_METAL;
    }

    /** Plates, tool heads and tools. */
    public boolean isToolMetal() {
        return forms == Forms.TOOL_METAL;
    }

    public boolean isBronze() {
        return this == BRONZE || this == ARSENICAL_BRONZE || this == BISMUTH_BRONZE;
    }

    @Override
    public String getSerializedName() {
        return id();
    }
}
