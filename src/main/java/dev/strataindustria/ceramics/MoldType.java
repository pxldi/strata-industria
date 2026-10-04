package dev.strataindustria.ceramics;

/** Tool molds (tier 0-2 spec 4.1): each casts one metal head or blade. */
public enum MoldType {
    PICKAXE_HEAD("pickaxe_head", "pickaxe", 100),
    AXE_HEAD("axe_head", "axe", 100),
    SHOVEL_HEAD("shovel_head", "shovel", 100),
    HOE_HEAD("hoe_head", "hoe", 100),
    KNIFE_BLADE("knife_blade", "knife", 100),
    HAMMER_HEAD("hammer_head", "hammer", 100),
    SAW_BLADE("saw_blade", "saw", 100),
    SWORD_BLADE("sword_blade", "sword", 200);

    private final String id;
    private final String tool;
    private final int units;

    MoldType(String id, String tool, int units) {
        this.id = id;
        this.tool = tool;
        this.units = units;
    }

    /** "pickaxe": the tool the cast head goes on. */
    public String tool() {
        return tool;
    }

    /** Metal units the mold takes (spec 7.3). */
    public int units() {
        return units;
    }

    /** "axe_head": the part it casts. The mold item is {@code <id>_mold}. */
    public String id() {
        return id;
    }
}
