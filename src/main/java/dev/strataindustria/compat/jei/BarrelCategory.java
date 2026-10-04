package dev.strataindustria.compat.jei;

import dev.strataindustria.compat.recipeview.IngredientStacks;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.tanning.BarrelRecipe;
import dev.strataindustria.tanning.FluidAmount;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.crafting.RecipeHolder;

/** The sealed soaking barrel: items and a fluid in, items and a fluid out, for one batch. */
public class BarrelCategory extends StrataCategory<RecipeHolder<BarrelRecipe>> {
    private static final int TANK_W = 18, TANK_H = 42;

    public BarrelCategory(IGuiHelper gui) {
        super(JeiTypes.BARREL, "barrel", gui.createDrawableItemLike(ModItems.SOAKING_BARREL.get()), 140, 58);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<BarrelRecipe> holder, IFocusGroup focuses) {
        BarrelRecipe recipe = holder.value();
        recipe.ingredient().ifPresent(i -> builder.addInputSlot(4, 14).setStandardSlotBackground()
                .addItemStacks(IngredientStacks.of(i, recipe.count())));
        recipe.fluid().ifPresent(f -> tank(builder.addSlot(RecipeIngredientRole.INPUT, 27, 1), f));
        recipe.result().ifPresent(r -> builder.addOutputSlot(90, 14).setStandardSlotBackground().add(r));
        recipe.fluidResult().ifPresent(f -> tank(builder.addSlot(RecipeIngredientRole.OUTPUT, 113, 1), f));
    }

    private static void tank(IRecipeSlotBuilder slot, FluidAmount fluid) {
        slot.add(fluid.fluid(), fluid.amount()).setFluidRenderer(fluid.amount(), false, TANK_W - 2, TANK_H - 2);
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, RecipeHolder<BarrelRecipe> holder, IFocusGroup focuses) {
        builder.addRecipeArrowWidget().setPosition(56, 14);
    }

    @Override
    public void draw(RecipeHolder<BarrelRecipe> holder, IRecipeSlotsView slots, GuiGraphicsExtractor g, double mouseX, double mouseY) {
        BarrelRecipe recipe = holder.value();
        if (recipe.fluid().isPresent()) frame(g, 26, 0, TANK_W, TANK_H);
        if (recipe.fluidResult().isPresent()) frame(g, 112, 0, TANK_W, TANK_H);
        time(g, recipe.ticks(), 4, 46);
    }
}
