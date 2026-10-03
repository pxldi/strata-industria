package dev.strataindustria.datagen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.geology.Rock;
import dev.strataindustria.registry.ModItems;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;

final class ModRecipeProvider extends RecipeProvider {
    ModRecipeProvider(BootstrapContext<Recipe<?>> recipeOutput, BootstrapContext<Advancement> advancementOutput) {
        super(recipeOutput, advancementOutput);
    }

    @Override
    protected void buildRecipes() {
        for (Rock rock : Rock.values()) {
            var loose = ModItems.LOOSE_ROCK.get(rock).get();
            var cobbled = ModItems.COBBLED_ROCK.get(rock).get();
            shaped(RecipeCategory.BUILDING_BLOCKS, cobbled)
                    .pattern("RR")
                    .pattern("RR")
                    .define('R', loose)
                    .unlockedBy("has_loose_rock", has(loose))
                    .save(output, key("cobbled_" + rock.id()));
            shapeless(RecipeCategory.MISC, loose, 4)
                    .requires(cobbled)
                    .unlockedBy("has_cobbled_rock", has(cobbled))
                    .save(output, key("loose_" + rock.id() + "_from_cobbled"));
        }
    }

    private static ResourceKey<Recipe<?>> key(String path) {
        return ResourceKey.create(Registries.RECIPE, StrataIndustria.id(path));
    }
}
