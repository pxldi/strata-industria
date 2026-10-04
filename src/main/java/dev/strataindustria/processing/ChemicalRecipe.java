package dev.strataindustria.processing;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/** A recipe of a fluid machine: what it takes and makes. */
public interface ChemicalRecipe {
    ChemicalIo io();

    /** The recipe input the vanilla recipe API wants; a fluid machine never uses it. */
    record Input() implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public int size() {
            return 0;
        }
    }
}
