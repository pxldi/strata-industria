package dev.strataindustria.transport.outpost;

import net.minecraft.util.StringRepresentable;

/** What carried the proof of a link (outposts spec 3.2). The tier decides how large an area the link loads. */
public enum LinkKind implements StringRepresentable {
    TRAMWAY("tramway", 3),
    RAILWAY("railway", 4),
    /** A locomotive ran it, but wooden rail is still in the route: the area stays at the tramway's size. */
    RAILWAY_MIXED("railway_mixed", 3),
    ROPEWAY("ropeway", 4),
    TRAM("tram", 5),
    POWER("power", 5),
    TELEGRAPH("telegraph", 5),
    PIPELINE("pipeline", 6);

    public static final com.mojang.serialization.Codec<LinkKind> CODEC = StringRepresentable.fromEnum(LinkKind::values);

    private final String id;
    private final int tier;

    LinkKind(String id, int tier) {
        this.id = id;
        this.tier = tier;
    }

    /** 3 to 6: the tier of the equipment, which picks the area size. */
    public int tier() {
        return tier;
    }

    public String langKey() {
        return "strataindustria.outposts.kind." + id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
