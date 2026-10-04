package dev.strataindustria.smithing;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/** The two pieces on the anvil for a weld (tier 3 spec 9.4). */
public record WeldingInput(ItemStack first, ItemStack second) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return index == 0 ? first : second;
    }

    @Override
    public int size() {
        return 2;
    }
}
