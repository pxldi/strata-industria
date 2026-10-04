package dev.strataindustria.processing;

import dev.strataindustria.washing.WashingRecipe;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/** What a machine makes from one input item: outputs it always gives, and outputs that come up by chance. */
public record Processing(List<ItemStack> outputs, List<WashingRecipe.Chance> chances) {
    /** Everything this could give at most, to check the output slots have room before starting. */
    public List<ItemStack> mostPossible() {
        List<ItemStack> all = new ArrayList<>();
        for (ItemStack stack : outputs) all.add(stack.copy());
        for (WashingRecipe.Chance chance : chances) all.add(chance.item().create());
        return all;
    }

    /** The outputs, and whichever chance outputs came up this time. */
    public List<ItemStack> roll(RandomSource random) {
        List<ItemStack> all = new ArrayList<>();
        for (ItemStack stack : outputs) all.add(stack.copy());
        for (WashingRecipe.Chance chance : chances) {
            if (random.nextFloat() < chance.chance()) all.add(chance.item().create());
        }
        return all;
    }
}
