package dev.strataindustria.tanning;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.material.Fluid;

/** What sits in a soaking barrel: the input stack and the tank. */
public record BarrelInput(ItemStack item, Fluid fluid, int amount) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return index == 0 ? item : ItemStack.EMPTY;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return item.isEmpty() && amount <= 0;
    }
}
