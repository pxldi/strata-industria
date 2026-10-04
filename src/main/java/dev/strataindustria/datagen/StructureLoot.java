package dev.strataindustria.datagen;

import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.material.Metal;
import dev.strataindustria.registry.ModItems;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

/**
 * The loot audit (structures v2 section 3). While a table is built, every item it can hand out is recorded
 * at its largest count; when the table is done the totals are checked against the rules and a table that
 * breaks one fails the data run, so CI fails with it. Limits are per table, so one container can never hold
 * a skipped tier; a structure's own containers are sized with that in mind.
 *
 * <ul>
 *   <li>L3 tools are worn stone tools (tier 0) and never above tier H - 1 (the clearing, H 0, may give tier 0 ones).
 *   <li>L5 metal, in units (100 to an ingot): tier at or below H - 1 up to 600, tier H up to 300, tier H + 1 only as
 *       nuggets and up to 30, nothing above that.
 *   <li>L6 at most 32 ore pieces (small, raw, crushed) and none of a mineral that needs a bronze tool or better (L8).
 * </ul>
 */
final class StructureLoot {
    static final int ORE_PIECES = 32;
    static final int LOW_TIER_UNITS = 600;
    static final int SAME_TIER_UNITS = 300;
    static final int NEXT_TIER_UNITS = 30;
    private static final int NUGGET_UNITS = 10;
    private static final int INGOT_UNITS = 100;

    private static final Map<Item, Metal> NUGGETS = new HashMap<>();
    private static final Map<Item, Metal> INGOTS = new HashMap<>();
    private static final Map<Item, OreMineral> ORES = new HashMap<>();

    private static Audit current;

    private StructureLoot() {}

    /** Structure tier H by the table's path, such as {@code chests/mining_camp/cache}. */
    static int tierOf(String path) {
        String[] parts = path.split("/");
        String place = parts.length > 1 ? parts[1] : "";
        return switch (place) {
            case "charcoal_burners_clearing" -> 0;
            case "prospector_camp" -> 1;
            default -> 2;
        };
    }

    static void begin(String path) {
        index();
        current = new Audit(path, tierOf(path));
    }

    static void end() {
        Audit audit = current;
        current = null;
        audit.check();
    }

    /** Called for every item a table can give, at the largest count it can give it. */
    static void record(ItemLike item, int max) {
        if (current == null) return;
        Item key = item.asItem();
        Metal nugget = NUGGETS.get(key), ingot = INGOTS.get(key);
        if (nugget != null) {
            current.metal(nugget, max * NUGGET_UNITS, true);
            return;
        }
        if (ingot != null) {
            current.metal(ingot, max * INGOT_UNITS, false);
            return;
        }
        OreMineral ore = ORES.get(key);
        if (ore != null) {
            current.ore(ore, max);
            return;
        }
        // Vanilla metal items that stand for the mod's own (iron) or that the mod does not replace (gold).
        if (key == Items.IRON_NUGGET) current.metal(Metal.WROUGHT_IRON, max * NUGGET_UNITS, true);
        if (key == Items.IRON_INGOT) current.metal(Metal.WROUGHT_IRON, max * INGOT_UNITS, false);
        if (key == Items.GOLD_NUGGET) current.metal(Metal.GOLD, max * NUGGET_UNITS, true);
        if (key == Items.GOLD_INGOT) current.metal(Metal.GOLD, max * INGOT_UNITS, false);
    }

    /** A tool left behind. Only worn stone tools (tier 0) are ever given. */
    static void tool(ItemLike item, float minLeft, float maxLeft) {
        if (current == null) return;
        Item key = item.asItem();
        boolean stone = key == ModItems.STONE_AXE.get() || key == ModItems.STONE_SHOVEL.get() || key == ModItems.STONE_PICKAXE.get()
                || key == ModItems.STONE_KNIFE.get() || key == ModItems.STONE_HAMMER.get() || key == ModItems.FIRESTARTER.get();
        if (!stone) current.fail("tool " + key + " is not a stone tool");
        if (key != ModItems.FIRESTARTER.get() && (minLeft < 0.25f || maxLeft > 0.8f)) {
            current.fail("tool " + key + " keeps " + minLeft + " to " + maxLeft + " of its durability, rule is 25 to 80 percent");
        }
    }

    private static void index() {
        if (!NUGGETS.isEmpty()) return;
        ModItems.NUGGETS.forEach((metal, item) -> NUGGETS.put(item.get(), metal));
        ModItems.INGOTS.forEach((metal, item) -> INGOTS.put(item.get(), metal));
        ModItems.SMALL_ORES.forEach((mineral, item) -> ORES.put(item.get(), mineral));
        for (var map : java.util.List.of(ModItems.ORE_PIECES, ModItems.CRUSHED_ORES, ModItems.WASHED_ORES)) {
            map.forEach((mineral, grades) -> grades.values().forEach(item -> ORES.put(item.get(), mineral)));
        }
    }

    private static final class Audit {
        private final String path;
        private final int h;
        private final Map<Metal, Integer> units = new EnumMap<>(Metal.class);
        private final Map<Metal, Boolean> onlyNuggets = new EnumMap<>(Metal.class);
        private int ores;

        Audit(String path, int h) {
            this.path = path;
            this.h = h;
        }

        void metal(Metal metal, int amount, boolean nugget) {
            units.merge(metal, amount, Integer::sum);
            onlyNuggets.merge(metal, nugget, Boolean::logicalAnd);
        }

        void ore(OreMineral mineral, int count) {
            if (mineral.needsBronzeTool()) fail(mineral.id() + " ore is bronze age or later and is never given as an item");
            ores += count;
        }

        void fail(String why) {
            throw new IllegalStateException("Loot audit: " + path + " (H " + h + "): " + why);
        }

        void check() {
            if (ores > ORES_LIMIT) fail(ores + " ore pieces, limit " + ORES_LIMIT);
            // Metals of one tier share the limit of that tier.
            Map<Integer, Integer> byTier = new HashMap<>();
            units.forEach((metal, amount) -> {
                int tier = metal.tier();
                if (tier > h + 1) fail(metal + " is tier " + tier + ", too far above H " + h);
                if (tier == h + 1 && !onlyNuggets.get(metal)) fail(metal + " of the next tier is only given as nuggets");
                byTier.merge(tier, amount, Integer::sum);
            });
            byTier.forEach((tier, amount) -> {
                int limit = tier <= h - 1 ? LOW_TIER_UNITS : tier == h ? SAME_TIER_UNITS : NEXT_TIER_UNITS;
                if (amount > limit) fail(amount + " units of tier " + tier + " metal, limit " + limit);
            });
        }

        private static final int ORES_LIMIT = ORE_PIECES;
    }
}
