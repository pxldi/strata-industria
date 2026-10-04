package dev.strataindustria.compat.recipeview;

import dev.strataindustria.Config;
import dev.strataindustria.bloomery.BloomeryBlockEntity;
import dev.strataindustria.charcoal.CharcoalPileBlock;
import dev.strataindustria.charcoal.LogPileBlockEntity;
import dev.strataindustria.ceramics.MoldType;
import dev.strataindustria.coking.CokeOvenBlockEntity;
import dev.strataindustria.material.Metal;
import dev.strataindustria.metal.Alloy;
import dev.strataindustria.metal.CastMoldItem;
import dev.strataindustria.metal.CrucibleBlockEntity;
import dev.strataindustria.metal.MetalContent;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.ModTags;
import dev.strataindustria.registry.Tier4Fluids;
import dev.strataindustria.steam.FireboxBlockEntity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.IntSupplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;

/**
 * The in-world processes that are code rather than recipe data, as plain display records. Recipe viewer
 * plugins (JEI now, EMI later) lay these out; nothing here depends on a viewer.
 */
public final class Processes {
    private Processes() {}

    /** Work at the anvil: one piece struck into a shape, or two pieces welded ({@code second} is empty for a strike). */
    public record AnvilWork(List<ItemStack> piece, List<ItemStack> second, ItemStack result, int blows) {
        public boolean weld() {
            return !second.isEmpty();
        }
    }

    /** An unfired piece fired in a pit kiln. */
    public record Firing(ItemStack input, ItemStack result, int ticks) {}

    /** Logs burnt under cover in a charcoal pit; per-log output is a config value, so it is shown per stack. */
    public record CharcoalPit(List<ItemStack> logs, ItemStack charcoal, ItemStack ash, int ticks) {}

    /** Two metals melted together in a crucible within share ranges (in percent). */
    public record Alloying(Alloy alloy, List<ItemStack> base, List<ItemStack> added, ItemStack result,
                           float baseMin, float baseMax, float addedMin, float addedMax, int meltingPoint, boolean refractory) {}

    /** Metal poured into a mold; {@code units} is how much metal the mold takes (100 per ingot). */
    public record Casting(ItemStack mold, Metal metal, List<ItemStack> metalItems, int units, ItemStack result,
                          int meltingPoint, boolean refractory) {}

    /** Iron-bearing items and charcoal fired in a bloomery. */
    public record Bloomery(List<ItemStack> ore, List<ItemStack> fuel, ItemStack bloom, ItemStack slag, int minTemperature) {}

    /** One coke oven bake. */
    public record Coking(List<ItemStack> input, ItemStack result, Fluid creosote, int creosoteAmount, int ticks) {}

    /** Items that burn in a steam firebox, grouped by the fuel row they share (tier 4 spec 8.1). */
    public record FireboxFuel(List<ItemStack> fuel, int burnTicks, int maxTemperature, int heat) {}

    public static List<FireboxFuel> fireboxFuel() {
        java.util.Map<FireboxBlockEntity.Fuel, List<ItemStack>> byFuel = new java.util.LinkedHashMap<>();
        BuiltInRegistries.ITEM.stream().sorted(Comparator.comparing(Processes::id)).map(ItemStack::new).forEach(stack -> {
            FireboxBlockEntity.Fuel fuel = FireboxBlockEntity.fuelFor(stack);
            if (fuel != null) byFuel.computeIfAbsent(fuel, f -> new ArrayList<>()).add(stack);
        });
        return byFuel.entrySet().stream()
                .sorted(Comparator.comparingInt(e -> e.getKey().maxTemperature()))
                .map(e -> new FireboxFuel(e.getValue(), e.getKey().burnTicks(), e.getKey().maxTemperature(), e.getKey().heat()))
                .toList();
    }

    public static List<Firing> kilnFiring() {
        int ticks = safe(Config.FIRING_TICKS::getAsInt, 900);
        List<Firing> out = new ArrayList<>();
        dev.strataindustria.ceramics.KilnFiring.all().forEach((in, fired) -> out.add(new Firing(new ItemStack(in), new ItemStack(fired), ticks)));
        out.sort(Comparator.comparing(f -> id(f.input().getItem())));
        return out;
    }

    public static List<CharcoalPit> charcoalPit() {
        int ticks = safe(Config.CHARCOAL_BURN_TICKS::getAsInt, 12000);
        double perLog = safeDouble(Config.CHARCOAL_PER_LOG::getAsDouble, 0.5);
        int logs = LogPileBlockEntity.MAX_LOGS;
        List<ItemStack> logStacks = tagged(BuiltInRegistries.ITEM.stream().filter(i -> new ItemStack(i).is(ItemTags.LOGS_THAT_BURN)).toList(), logs);
        int charcoal = Math.min(CharcoalPileBlock.MAX_CHARCOAL, Math.max(1, (int) Math.floor(logs * perLog)));
        return List.of(new CharcoalPit(logStacks, new ItemStack(Items.CHARCOAL, charcoal), new ItemStack(ModItems.ASH.get()), ticks));
    }

    public static List<Alloying> alloying() {
        int clayMax = safe(Config.CLAY_CRUCIBLE_MAX::getAsInt, 1400);
        List<Alloying> out = new ArrayList<>();
        for (Alloy alloy : Alloy.values()) {
            // Wrought iron with a trace of carbon is still wrought iron, not a mix anyone makes.
            if (alloy.result() == alloy.base()) continue;
            int melt = alloy.result().meltingPoint();
            float addedMin = alloy.addedMin(), addedMax = alloy.addedMax();
            out.add(new Alloying(alloy, metalItems(alloy.base()), metalItems(alloy.added()), new ItemStack(ModItems.ingot(alloy.result())),
                    100 * (1 - addedMax), 100 * (1 - addedMin), 100 * addedMin, 100 * addedMax, melt, melt > clayMax));
        }
        return out;
    }

    public static List<Casting> casting() {
        int clayMax = safe(Config.CLAY_CRUCIBLE_MAX::getAsInt, 1400);
        List<CastMoldItem> molds = BuiltInRegistries.ITEM.stream().filter(i -> i instanceof CastMoldItem)
                .map(i -> (CastMoldItem) i).sorted(Comparator.comparing(Processes::id)).toList();
        List<Casting> out = new ArrayList<>();
        for (Metal metal : Metal.values()) {
            if (!metal.hasIngot() || metal.dissolvedOnly() || metal == Metal.SLAG_METAL) continue;
            int melt = metal.meltingPoint();
            if (melt > CrucibleBlockEntity.REFRACTORY_MAX_TEMPERATURE) continue;
            for (CastMoldItem mold : molds) {
                if (!mold.takes(melt)) continue;
                Item result = castResult(mold, metal);
                if (result == null) continue;
                int ingots = Math.max(1, mold.units() / MetalContent.INGOT_UNITS);
                out.add(new Casting(new ItemStack(mold), metal, List.of(new ItemStack(ModItems.ingot(metal), ingots)), mold.units(),
                        new ItemStack(result), melt, melt > clayMax));
            }
        }
        return out;
    }

    private static Item castResult(CastMoldItem mold, Metal metal) {
        if (mold.isBell()) return CastMoldItem.ringsAsBell(metal) ? dev.strataindustria.bronze.BronzeRegistry.BELL_ITEM.get() : null;
        if (mold.isGear()) return ModItems.GEARS.containsKey(metal) ? ModItems.GEARS.get(metal).get() : null;
        MoldType type = mold.type();
        if (type == null) return ModItems.ingot(metal);
        if (!metal.toolTypes().contains(type) || !ModItems.HEADS.containsKey(metal) || !ModItems.HEADS.get(metal).containsKey(type)) return null;
        return ModItems.head(metal, type);
    }

    public static List<Bloomery> bloomery() {
        List<ItemStack> ore = BuiltInRegistries.ITEM.stream().map(ItemStack::new)
                .filter(BloomeryBlockEntity::isOre).sorted(Comparator.comparing(s -> id(s.getItem()))).toList();
        List<ItemStack> fuel = BuiltInRegistries.ITEM.stream().map(ItemStack::new)
                .filter(s -> s.is(ModTags.Items.BLOOMERY_FUEL)).toList();
        return List.of(new Bloomery(
                ore.stream().map(s -> s.copyWithCount(BloomeryBlockEntity.ORE_PER_LEVEL)).toList(),
                fuel.stream().map(s -> s.copyWithCount(BloomeryBlockEntity.CHARCOAL_PER_LEVEL)).toList(),
                new ItemStack(ModItems.RAW_BLOOM.get()), new ItemStack(ModItems.BLOOMERY_SLAG.get()),
                (int) BloomeryBlockEntity.MIN_TEMPERATURE));
    }

    public static List<Coking> coking() {
        List<Item> items = BuiltInRegistries.ITEM.stream().sorted(Comparator.comparing(Processes::id)).toList();
        List<Coking> out = new ArrayList<>();
        for (CokeOvenBlockEntity.Coking coking : CokeOvenBlockEntity.RECIPES) {
            List<ItemStack> input = items.stream().map(ItemStack::new).filter(coking.input())
                    .map(s -> s.copyWithCount(coking.count())).toList();
            if (input.isEmpty()) continue;
            out.add(new Coking(input, coking.result().get(), Tier4Fluids.CREOSOTE.get(), coking.creosote(), coking.ticks()));
        }
        return out;
    }

    /** The ingot of a metal, or for a metal that never stands alone (arsenic, carbon) whatever carries it. */
    public static List<ItemStack> metalItems(Metal metal) {
        if (metal.hasIngot() && ModItems.INGOTS.containsKey(metal)) return List.of(new ItemStack(ModItems.ingot(metal)));
        return MetalContent.itemsHolding(metal).stream().sorted(Comparator.comparing(Processes::id)).map(ItemStack::new).toList();
    }

    private static List<ItemStack> tagged(List<Item> items, int count) {
        return items.stream().sorted(Comparator.comparing(Processes::id)).map(i -> new ItemStack(i, count)).toList();
    }

    private static String id(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    /** Config values are read on the client too; fall back to the default before the config has loaded. */
    private static int safe(IntSupplier value, int fallback) {
        try {
            return value.getAsInt();
        } catch (IllegalStateException e) {
            return fallback;
        }
    }

    private static double safeDouble(java.util.function.DoubleSupplier value, double fallback) {
        try {
            return value.getAsDouble();
        } catch (IllegalStateException e) {
            return fallback;
        }
    }
}
