package dev.strataindustria.registry;

import java.util.List;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredItem;

/** Tier 6 (industrial) items, kept apart from the earlier tiers' items. */
public final class Tier6Items {
    // Spec 4.6: oil and its fractions. Only crude oil can be poured out (spec 12.3); the others give the
    // bucket back when a recipe uses them, like creosote.
    public static final DeferredItem<BucketItem> CRUDE_OIL_BUCKET = ModItems.ITEMS.registerItem("crude_oil_bucket",
            p -> new BucketItem(Tier6Fluids.CRUDE_OIL.source().get(), p), p -> p.stacksTo(1).craftRemainder(Items.BUCKET));
    public static final DeferredItem<Item> NAPHTHA_BUCKET = fractionBucket("naphtha_bucket");
    public static final DeferredItem<Item> DIESEL_BUCKET = fractionBucket("diesel_bucket");
    public static final DeferredItem<Item> HEAVY_OIL_BUCKET = fractionBucket("heavy_oil_bucket");
    /** Column bottoms (spec 7.1), the binder of asphalt. */
    public static final DeferredItem<Item> BITUMEN = ModItems.ITEMS.registerSimpleItem("bitumen");

    // Spec 8.4 to 8.7: plastics and synthetic rubber.
    public static final DeferredItem<Item> POLYETHYLENE_PELLET = ModItems.ITEMS.registerSimpleItem("polyethylene_pellet");
    public static final DeferredItem<Item> POLYETHYLENE_SHEET = ModItems.ITEMS.registerSimpleItem("polyethylene_sheet");
    public static final DeferredItem<Item> PVC_PELLET = ModItems.ITEMS.registerSimpleItem("pvc_pellet");
    public static final DeferredItem<Item> PVC_SHEET = ModItems.ITEMS.registerSimpleItem("pvc_sheet");
    public static final DeferredItem<Item> SYNTHETIC_RUBBER = ModItems.ITEMS.registerSimpleItem("synthetic_rubber");

    /** Spec 5.5: the oil still. */
    public static final DeferredItem<BlockItem> OIL_STILL = ModItems.ITEMS.registerSimpleBlockItem(Tier6Blocks.OIL_STILL);

    /** Plain items with a flat item model and a texture of the same name. */
    public static List<DeferredItem<? extends Item>> flatItems() {
        return List.of(CRUDE_OIL_BUCKET, NAPHTHA_BUCKET, DIESEL_BUCKET, HEAVY_OIL_BUCKET, BITUMEN, POLYETHYLENE_PELLET,
                POLYETHYLENE_SHEET, PVC_PELLET, PVC_SHEET, SYNTHETIC_RUBBER);
    }

    private static DeferredItem<Item> fractionBucket(String name) {
        return ModItems.ITEMS.registerSimpleItem(name, p -> p.stacksTo(1).craftRemainder(Items.BUCKET));
    }

    public static void init() {}

    private Tier6Items() {}
}
