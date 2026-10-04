package dev.strataindustria.registry;

import dev.strataindustria.metal.CastMoldItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredItem;

/** Tier 4 (steel and steam) items, kept apart from the earlier tiers' items. */
public final class Tier4Items {
    /** Tier 4 spec 4.6: native sulfur ore drops this; with charcoal dust it makes gunpowder. */
    public static final DeferredItem<Item> SULFUR = ModItems.ITEMS.registerSimpleItem("sulfur");
    public static final DeferredItem<Item> SULFUR_DUST = ModItems.ITEMS.registerSimpleItem("sulfur_dust");
    /** Ground charcoal: carbon for a melt (spec 4.2). */
    public static final DeferredItem<Item> CHARCOAL_DUST = ModItems.ITEMS.registerSimpleItem("charcoal_dust");

    // Spec 5.1: coke and its oven.
    public static final DeferredItem<Item> COKE = ModItems.ITEMS.registerSimpleItem("coke");
    public static final DeferredItem<Item> COKE_DUST = ModItems.ITEMS.registerSimpleItem("coke_dust");
    public static final DeferredItem<BlockItem> COKE_BLOCK = ModItems.ITEMS.registerSimpleBlockItem(Tier4Blocks.COKE_BLOCK);
    public static final DeferredItem<Item> UNFIRED_COKE_OVEN_BRICK = ModItems.ITEMS.registerSimpleItem("unfired_coke_oven_brick");
    public static final DeferredItem<Item> COKE_OVEN_BRICK = ModItems.ITEMS.registerSimpleItem("coke_oven_brick");
    public static final DeferredItem<BlockItem> COKE_OVEN_BRICKS = ModItems.ITEMS.registerSimpleBlockItem(Tier4Blocks.COKE_OVEN_BRICKS);
    public static final DeferredItem<BlockItem> COKE_OVEN_DOOR = ModItems.ITEMS.registerSimpleBlockItem(Tier4Blocks.COKE_OVEN_DOOR);
    /** Spec 5.2: a bucket of creosote from the oven's tank; the bucket comes back when it is used in a recipe. */
    public static final DeferredItem<Item> CREOSOTE_BUCKET = ModItems.ITEMS.registerSimpleItem("creosote_bucket",
            p -> p.stacksTo(1).craftRemainder(Items.BUCKET));

    // Spec 4.6: treated wood.
    public static final DeferredItem<BlockItem> TREATED_PLANKS = ModItems.ITEMS.registerSimpleBlockItem(Tier4Blocks.TREATED_PLANKS);
    public static final DeferredItem<BlockItem> TREATED_SLAB = ModItems.ITEMS.registerSimpleBlockItem(Tier4Blocks.TREATED_SLAB);
    public static final DeferredItem<BlockItem> TREATED_STAIRS = ModItems.ITEMS.registerSimpleBlockItem(Tier4Blocks.TREATED_STAIRS);
    public static final DeferredItem<BlockItem> TREATED_FENCE = ModItems.ITEMS.registerSimpleBlockItem(Tier4Blocks.TREATED_FENCE);
    public static final DeferredItem<Item> TREATED_STICK = ModItems.ITEMS.registerSimpleItem("treated_stick");

    /** Loads the class so its items join the register before it fires. */
    // Spec 6.1: refractory ceramics, and the clay gear mold.
    public static final DeferredItem<Item> UNFIRED_REFRACTORY_CRUCIBLE = ModItems.ITEMS.registerSimpleItem("unfired_refractory_crucible",
            p -> p.stacksTo(1));
    public static final DeferredItem<BlockItem> REFRACTORY_CRUCIBLE = ModItems.ITEMS.registerSimpleBlockItem(Tier4Blocks.REFRACTORY_CRUCIBLE,
            p -> p.stacksTo(1));
    public static final DeferredItem<Item> UNFIRED_REFRACTORY_INGOT_MOLD = ModItems.ITEMS.registerSimpleItem("unfired_refractory_ingot_mold",
            p -> p.stacksTo(16));
    public static final DeferredItem<CastMoldItem> REFRACTORY_INGOT_MOLD = ModItems.ITEMS.registerItem("refractory_ingot_mold",
            p -> new CastMoldItem(null, false, true, p), p -> p.stacksTo(16));
    public static final DeferredItem<Item> UNFIRED_REFRACTORY_GEAR_MOLD = ModItems.ITEMS.registerSimpleItem("unfired_refractory_gear_mold",
            p -> p.stacksTo(16));
    public static final DeferredItem<CastMoldItem> REFRACTORY_GEAR_MOLD = ModItems.ITEMS.registerItem("refractory_gear_mold",
            p -> new CastMoldItem(null, true, true, p), p -> p.stacksTo(16));
    public static final DeferredItem<Item> UNFIRED_GEAR_MOLD = ModItems.ITEMS.registerSimpleItem("unfired_gear_mold", p -> p.stacksTo(16));
    public static final DeferredItem<CastMoldItem> GEAR_MOLD = ModItems.ITEMS.registerItem("gear_mold",
            p -> new CastMoldItem(null, true, false, p), p -> p.stacksTo(16));

    public static void init() {}

    private Tier4Items() {}
}
