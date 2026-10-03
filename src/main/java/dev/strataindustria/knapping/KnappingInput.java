package dev.strataindustria.knapping;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/**
 * The state of a 5x5 shaping grid: the material being worked and a bit mask of the cells still in
 * place (bit {@code row * 5 + column}, top row first).
 */
public record KnappingInput(ItemStack material, int kept) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return index == 0 ? material : ItemStack.EMPTY;
    }

    @Override
    public int size() {
        return 1;
    }
}
