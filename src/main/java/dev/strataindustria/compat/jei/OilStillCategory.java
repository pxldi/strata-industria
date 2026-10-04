package dev.strataindustria.compat.jei;

import dev.strataindustria.processing.OilStillRecipe;
import dev.strataindustria.registry.Tier6Items;
import dev.strataindustria.tanning.FluidAmount;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.crafting.RecipeHolder;

/** The oil still: one fluid in, its fractions out, held at a heat for a time. */
public class OilStillCategory extends StrataCategory<RecipeHolder<OilStillRecipe>> {
    private static final int X_IN = 6, X_OUT = 72, Y = 6;

    public OilStillCategory(IGuiHelper gui) {
        super(JeiTypes.OIL_STILL, "oil_still", gui.createDrawableItemLike(Tier6Items.OIL_STILL.get()), 140, 54);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<OilStillRecipe> holder, IFocusGroup focuses) {
        OilStillRecipe recipe = holder.value();
        fluid(builder, RecipeIngredientRole.INPUT, recipe.input(), X_IN);
        for (int i = 0; i < recipe.results().size(); i++) fluid(builder, RecipeIngredientRole.OUTPUT, recipe.results().get(i), X_OUT + i * SLOT);
    }

    private static void fluid(IRecipeLayoutBuilder builder, RecipeIngredientRole role, FluidAmount fluid, int x) {
        builder.addSlot(role, x, Y).setStandardSlotBackground().add(fluid.fluid(), fluid.amount()).setFluidRenderer(fluid.amount(), false, 16, 16);
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, RecipeHolder<OilStillRecipe> holder, IFocusGroup focuses) {
        builder.addAnimatedRecipeArrowWidget(Math.min(holder.value().ticks(), 400)).setPosition(32, Y);
    }

    @Override
    public void draw(RecipeHolder<OilStillRecipe> holder, IRecipeSlotsView slots, GuiGraphicsExtractor g, double mouseX, double mouseY) {
        OilStillRecipe recipe = holder.value();
        heat(g, "work", recipe.minTemperature(), 6, 28);
        time(g, recipe.ticks(), 6, 28 + 14);
        if (recipe.vented() > 0) text(g, dev.strataindustria.compat.recipeview.RecipeText.key("oil_still.vented", recipe.vented()), 72, 28 + 15, TEXT_FAINT);
    }
}
