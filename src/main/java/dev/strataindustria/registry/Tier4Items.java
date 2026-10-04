package dev.strataindustria.registry;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

/** Tier 4 (steel and steam) materials, kept apart from the earlier tiers' items. */
public final class Tier4Items {
    /** Tier 4 spec 4.6: native sulfur ore drops this; with charcoal dust it makes gunpowder. */
    public static final DeferredItem<Item> SULFUR = ModItems.ITEMS.registerSimpleItem("sulfur");
    public static final DeferredItem<Item> SULFUR_DUST = ModItems.ITEMS.registerSimpleItem("sulfur_dust");
    /** Ground charcoal: carbon for a melt (spec 4.2). */
    public static final DeferredItem<Item> CHARCOAL_DUST = ModItems.ITEMS.registerSimpleItem("charcoal_dust");

    /** Loads the class so its items join the register before it fires. */
    public static void init() {}

    private Tier4Items() {}
}
