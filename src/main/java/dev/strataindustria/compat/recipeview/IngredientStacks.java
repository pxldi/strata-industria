package dev.strataindustria.compat.recipeview;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/** The items an ingredient matches, as stacks of a given size, for slots that show a count. */
public final class IngredientStacks {
    private static final Map<Ingredient, List<ItemStack>> CACHE = new IdentityHashMap<>();

    private IngredientStacks() {}

    public static synchronized List<ItemStack> of(Ingredient ingredient, int count) {
        List<ItemStack> matching = CACHE.computeIfAbsent(ingredient, i -> BuiltInRegistries.ITEM.stream()
                .map(ItemStack::new).filter(s -> !s.isEmpty() && i.test(s)).toList());
        return matching.stream().map(s -> s.copyWithCount(count)).toList();
    }

    /** Recipes are rebuilt on every datapack reload, so the cache is dropped with them. */
    public static synchronized void clear() {
        CACHE.clear();
    }
}
