package dev.strataindustria.datagen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.material.Metal;
import dev.strataindustria.processing.OilStillRecipe;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.Tier4Items;
import dev.strataindustria.registry.Tier6Fluids;
import dev.strataindustria.registry.Tier6Items;
import dev.strataindustria.tanning.FluidAmount;
import java.util.List;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;

/** Tier 6 (industrial) recipes. {@link ModRecipeProvider} runs it, so tier 6 stays out of the shared provider. */
final class Tier6RecipeProvider extends RecipeProvider {
    Tier6RecipeProvider(BootstrapContext<Recipe<?>> recipeOutput, BootstrapContext<Advancement> advancementOutput) {
        super(recipeOutput, advancementOutput);
    }

    @Override
    protected void buildRecipes() {
        oilStill();
    }

    // Spec 5.5: 1000 mB crude gives 200 naphtha, 300 diesel and 400 heavy oil; the other 100 mB vents.
    private void oilStill() {
        shaped(RecipeCategory.DECORATIONS, Tier6Items.OIL_STILL.get())
                .pattern("PFP")
                .pattern("PTP")
                .pattern("BBB")
                .define('P', ModItems.PLATES.get(Metal.COPPER).get())
                .define('F', Tier4Items.COPPER_FLUID_PIPE.get())
                .define('T', Tier4Items.FLUID_TANK.get())
                .define('B', ModItems.FIRE_BRICK.get())
                .unlockedBy("has_fluid_tank", has(Tier4Items.FLUID_TANK.get()))
                .save(output, key("oil_still"));
        output.accept(key("oil_still/crude_oil"), new OilStillRecipe(new FluidAmount(Tier6Fluids.CRUDE_OIL.source().get(), 1000),
                List.of(new FluidAmount(Tier6Fluids.NAPHTHA.source().get(), 200), new FluidAmount(Tier6Fluids.DIESEL.source().get(), 300),
                        new FluidAmount(Tier6Fluids.HEAVY_OIL.source().get(), 400)), 100, 1200, 400), null);
    }

    private static ResourceKey<Recipe<?>> key(String path) {
        return ResourceKey.create(Registries.RECIPE, StrataIndustria.id(path));
    }
}
