package dev.strataindustria.datagen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModItems;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;

final class ModRecipeProvider extends RecipeProvider {
    ModRecipeProvider(BootstrapContext<Recipe<?>> recipeOutput, BootstrapContext<Advancement> advancementOutput) {
        super(recipeOutput, advancementOutput);
    }

    @Override
    protected void buildRecipes() {
        shapeless(RecipeCategory.MISC, Items.STRING)
                .requires(ModItems.PLANT_FIBRE.get(), 3)
                .unlockedBy("has_plant_fibre", has(ModItems.PLANT_FIBRE.get()))
                .save(output, key("string_from_plant_fibre"));

        shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.FIRE_BRICKS.get(), 2)
                .pattern("BB")
                .pattern("BB")
                .define('B', Items.BRICK)
                .unlockedBy("has_brick", has(Items.BRICK))
                .save(output, key("fire_bricks"));
    }

    private static ResourceKey<Recipe<?>> key(String path) {
        return ResourceKey.create(Registries.RECIPE, StrataIndustria.id(path));
    }
}
