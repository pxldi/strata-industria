package dev.strataindustria.ceramics;

import dev.strataindustria.registry.ModItems;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** What each unfired clay piece becomes in a pit kiln (spec 4.2). */
public final class KilnFiring {
    private static Map<Item, Item> results;

    private KilnFiring() {}

    private static Map<Item, Item> results() {
        if (results == null) {
            Map<Item, Item> map = new IdentityHashMap<>();
            map.put(ModItems.UNFIRED_SMALL_VESSEL.get(), ModItems.SMALL_VESSEL.get());
            map.put(ModItems.UNFIRED_LARGE_VESSEL.get(), ModItems.LARGE_VESSEL.get());
            map.put(ModItems.UNFIRED_CRUCIBLE.get(), ModItems.CRUCIBLE.get());
            map.put(ModItems.UNFIRED_INGOT_MOLD.get(), ModItems.INGOT_MOLD.get());
            map.put(ModItems.UNFIRED_BRICK.get(), Items.BRICK);
            map.put(ModItems.UNFIRED_FIRE_BRICK.get(), ModItems.FIRE_BRICK.get());
            map.put(dev.strataindustria.registry.Tier4Items.UNFIRED_COKE_OVEN_BRICK.get(), dev.strataindustria.registry.Tier4Items.COKE_OVEN_BRICK.get());
            // Tier 4 spec 6.1: refractory ceramics and the gear molds.
            map.put(dev.strataindustria.registry.Tier4Items.UNFIRED_REFRACTORY_CRUCIBLE.get(), dev.strataindustria.registry.Tier4Items.REFRACTORY_CRUCIBLE.get());
            map.put(dev.strataindustria.registry.Tier4Items.UNFIRED_REFRACTORY_INGOT_MOLD.get(), dev.strataindustria.registry.Tier4Items.REFRACTORY_INGOT_MOLD.get());
            map.put(dev.strataindustria.registry.Tier4Items.UNFIRED_REFRACTORY_GEAR_MOLD.get(), dev.strataindustria.registry.Tier4Items.REFRACTORY_GEAR_MOLD.get());
            map.put(dev.strataindustria.registry.Tier4Items.UNFIRED_GEAR_MOLD.get(), dev.strataindustria.registry.Tier4Items.GEAR_MOLD.get());
            for (MoldType type : MoldType.values()) {
                map.put(ModItems.UNFIRED_MOLDS.get(type).get(), ModItems.MOLDS.get(type).get());
            }
            results = map;
        }
        return results;
    }

    /** Every unfired piece and what it fires into, for recipe viewers. */
    public static Map<Item, Item> all() {
        return java.util.Collections.unmodifiableMap(results());
    }

    public static boolean isFireable(ItemStack stack) {
        return results().containsKey(stack.getItem());
    }

    /** The fired stack, or the stack unchanged when it does not fire. */
    public static ItemStack fire(ItemStack stack) {
        Item fired = results().get(stack.getItem());
        return fired == null ? stack : new ItemStack(fired, stack.getCount());
    }
}
