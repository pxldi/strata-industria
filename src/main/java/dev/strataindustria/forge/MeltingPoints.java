package dev.strataindustria.forge;

import dev.strataindustria.geology.OreGrade;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.material.Metal;
import dev.strataindustria.registry.ModItems;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Where an item starts to melt: its metal's melting point, or the lowest metal in an ore. */
public final class MeltingPoints {
    private static Map<Item, Float> points;

    private MeltingPoints() {}

    private static Map<Item, Float> points() {
        if (points == null) {
            Map<Item, Float> map = new IdentityHashMap<>();
            map.put(Items.COPPER_INGOT, (float) Metal.COPPER.meltingPoint());
            map.put(Items.COPPER_NUGGET, (float) Metal.COPPER.meltingPoint());
            for (OreMineral mineral : OreMineral.withPieces()) {
                if (mineral.isSulfide()) continue;
                float lowest = (float) mineral.composition().keySet().stream().mapToInt(Metal::meltingPoint).min().orElseThrow();
                map.put(ModItems.SMALL_ORES.get(mineral).get(), lowest);
                for (OreGrade grade : OreGrade.values()) {
                    map.put(ModItems.orePiece(mineral, grade), lowest);
                    map.put(ModItems.crushedOre(mineral, grade), lowest);
                    if (mineral.washable()) map.put(ModItems.washedOre(mineral, grade), lowest);
                }
            }
            points = map;
        }
        return points;
    }

    public static Optional<Float> of(ItemStack stack) {
        return Optional.ofNullable(points().get(stack.getItem()));
    }
}
