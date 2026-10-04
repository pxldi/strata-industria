package dev.strataindustria.ceramics;

/** Tool molds (tier 0-2 spec 4.1): each casts one metal head or blade. */
public enum MoldType {
    PICKAXE_HEAD("pickaxe_head"),
    AXE_HEAD("axe_head"),
    SHOVEL_HEAD("shovel_head"),
    HOE_HEAD("hoe_head"),
    KNIFE_BLADE("knife_blade"),
    HAMMER_HEAD("hammer_head"),
    SAW_BLADE("saw_blade"),
    SWORD_BLADE("sword_blade");

    private final String id;

    MoldType(String id) {
        this.id = id;
    }

    /** "axe_head": the part it casts. The mold item is {@code <id>_mold}. */
    public String id() {
        return id;
    }
}
