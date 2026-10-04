package dev.strataindustria.metal;

import dev.strataindustria.ceramics.MoldType;
import dev.strataindustria.geology.OreGrade;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.material.Metal;
import dev.strataindustria.registry.ModDataComponents;
import dev.strataindustria.registry.ModItems;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * What an item gives when melted (spec 6.2, 7.1): ore pieces at 80% of their crushed value, crushed
 * ore in full, surface nuggets at 10 units, ingots 100, nuggets 10, plates and heads 100 (sword
 * blades 200), and slag metal 90% of what went into it.
 */
public final class MetalContent {
    public static final int INGOT_UNITS = 100;
    public static final int NUGGET_UNITS = 10;
    public static final float SLAG_RETURN = 0.9f;
    public static final int ROD_UNITS = 50;
    /** A vanilla raw iron, copper or gold from loot: as much as a raw normal hematite. */
    public static final int RAW_VANILLA_UNITS = 28;
    public static final int BLOOMERY_SLAG_UNITS = 10;
    public static final int CARBON_DUST_UNITS = 5;
    /** Tier 4 spec 4.4: zinc calcine gives up its zinc to carbon at this heat. */
    public static final int CALCINE_REDUCTION = 950;

    private static Map<Item, Melt> fixed;

    private MetalContent() {}

    private static Map<Item, Melt> fixed() {
        if (fixed == null) {
            Map<Item, Melt> map = new IdentityHashMap<>();
            for (OreMineral mineral : OreMineral.withPieces()) {
                // Tier 4 spec 4.4: a sulfide never melts as ore; it is roasted to calcine first.
                if (mineral.isSulfide()) continue;
                map.put(ModItems.SMALL_ORES.get(mineral).get(), ore(mineral, OreMineral.SMALL_ORE_UNITS));
                for (OreGrade grade : OreGrade.values()) {
                    int crushed = mineral.crushedUnits(grade);
                    map.put(ModItems.orePiece(mineral, grade), ore(mineral, crushed * OreMineral.RAW_MELT_EFFICIENCY));
                    map.put(ModItems.crushedOre(mineral, grade), ore(mineral, crushed));
                    // Tier 3 spec 11.2: washing is worth a tenth more.
                    if (mineral.washable()) map.put(ModItems.washedOre(mineral, grade), ore(mineral, (float) Math.floor(crushed * OreMineral.WASHED_BONUS)));
                }
            }
            for (Metal metal : Metal.values()) {
                if (metal == Metal.SLAG_METAL) continue;
                if (ModItems.INGOTS.containsKey(metal)) map.put(ModItems.ingot(metal), Alloy.parts(metal, INGOT_UNITS));
                if (ModItems.NUGGETS.containsKey(metal)) map.put(ModItems.NUGGETS.get(metal).get(), Alloy.parts(metal, NUGGET_UNITS));
                if (ModItems.PLATES.containsKey(metal)) map.put(ModItems.PLATES.get(metal).get(), Alloy.parts(metal, INGOT_UNITS));
                if (ModItems.RODS.containsKey(metal)) map.put(ModItems.RODS.get(metal).get(), Alloy.parts(metal, ROD_UNITS));
                if (ModItems.GEARS.containsKey(metal)) map.put(ModItems.GEARS.get(metal).get(), Alloy.parts(metal, INGOT_UNITS));
                if (ModItems.HEADS.containsKey(metal)) {
                    for (MoldType type : metal.toolTypes()) map.put(ModItems.head(metal, type), Alloy.parts(metal, type.units()));
                }
                if (ModItems.PROSPECTOR_HEADS.containsKey(metal)) map.put(ModItems.PROSPECTOR_HEADS.get(metal).get(), Alloy.parts(metal, INGOT_UNITS));
            }
            // Tier 3 spec 4.1: rods are half an ingot, a double ingot two.
            map.put(ModItems.WROUGHT_IRON_ROD.get(), Melt.of(Metal.WROUGHT_IRON, ROD_UNITS));
            map.put(ModItems.WROUGHT_IRON_DOUBLE_INGOT.get(), Melt.of(Metal.WROUGHT_IRON, 2 * INGOT_UNITS));
            map.put(ModItems.STEEL_DOUBLE_INGOT.get(), Alloy.parts(Metal.STEEL, 2 * INGOT_UNITS));
            // Tier 4 spec 4.2: charcoal and coke dust carry carbon into an iron melt.
            map.put(dev.strataindustria.registry.Tier4Items.CHARCOAL_DUST.get(), Melt.of(Metal.CARBON, CARBON_DUST_UNITS));
            map.put(dev.strataindustria.registry.Tier4Items.COKE_DUST.get(), Melt.of(Metal.CARBON, CARBON_DUST_UNITS));
            // Tier 4 spec 4.4: roasted sphalerite carries its full crushed value of zinc.
            for (OreGrade grade : OreGrade.values()) {
                int units = OreMineral.SPHALERITE.crushedUnits(grade);
                map.put(dev.strataindustria.registry.Tier4Items.zincCalcine(grade), new Melt(Map.of(Metal.ZINC, units)));
            }
            map.put(dev.strataindustria.registry.Tier4Items.SMALL_ZINC_CALCINE.get(),
                    new Melt(Map.of(Metal.ZINC, OreMineral.SMALL_ORE_UNITS)));
            dev.strataindustria.registry.Tier5Items.metalContent(map);
            // Tier 3 spec 5.4: bloomery slag still holds some iron.
            map.put(ModItems.BLOOMERY_SLAG.get(), Melt.of(Metal.WROUGHT_IRON, BLOOMERY_SLAG_UNITS));
            // Spec 4.2: vanilla raw ores from loot count as raw normal ore.
            map.put(Items.RAW_IRON, Melt.of(Metal.WROUGHT_IRON, RAW_VANILLA_UNITS));
            map.put(Items.RAW_COPPER, Melt.of(Metal.COPPER, RAW_VANILLA_UNITS));
            map.put(Items.RAW_GOLD, Melt.of(Metal.GOLD, RAW_VANILLA_UNITS));
            fixed = map;
        }
        return fixed;
    }

    /** Each metal of the ore gets its share of the units, rounded down per metal. */
    private static Melt ore(OreMineral mineral, float units) {
        Map<Metal, Integer> out = new EnumMap<>(Metal.class);
        for (var e : mineral.composition().entrySet()) {
            int u = (int) Math.floor(units * e.getValue());
            if (u > 0) out.put(e.getKey(), u);
        }
        return new Melt(out);
    }

    /** The metal in one of this item, if any. */
    public static Optional<Melt> of(ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        Melt bloom = stack.get(ModDataComponents.BLOOM_CONTENTS.get());
        if (bloom != null) return Optional.of(bloom);
        Melt slag = stack.get(ModDataComponents.SLAG.get());
        if (slag != null) return Optional.of(slag.scaled(SLAG_RETURN));
        return Optional.ofNullable(fixed().get(stack.getItem()));
    }

    /** Roasted zinc ore, which needs carbon in the melt before it will give up its zinc (tier 4 spec 4.2). */
    public static boolean isCalcine(ItemStack stack) {
        return stack.is(dev.strataindustria.registry.Tier4Items.SMALL_ZINC_CALCINE.get())
                || dev.strataindustria.registry.Tier4Items.ZINC_CALCINES.values().stream().anyMatch(c -> stack.is(c.get()));
    }

    /** Carbon a calcine piece takes from the melt: a tenth of its zinc, rounded up. */
    public static int carbonFor(Melt calcine) {
        return (calcine.units().getOrDefault(Metal.ZINC, 0) + 9) / 10;
    }

    /** Every item with a fixed metal content that holds some of {@code metal}, for recipe viewers. */
    public static java.util.List<Item> itemsHolding(Metal metal) {
        return fixed().entrySet().stream().filter(e -> e.getValue().units().getOrDefault(metal, 0) > 0)
                .map(Map.Entry::getKey).toList();
    }

    public static boolean hasMetal(ItemStack stack) {
        return of(stack).isPresent();
    }
}
