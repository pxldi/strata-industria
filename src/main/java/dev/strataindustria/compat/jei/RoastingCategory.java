package dev.strataindustria.compat.jei;

import dev.strataindustria.registry.ModItems;
import dev.strataindustria.roasting.RoastingRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.crafting.RecipeHolder;

/** Roasting in a forge heating slot: held at a heat for a time, giving off gas. */
public class RoastingCategory extends StrataCategory<RecipeHolder<RoastingRecipe>> {
    public RoastingCategory(IGuiHelper gui) {
        super(JeiTypes.ROASTING, "roasting", gui.createDrawableItemLike(ModItems.FORGE.get()), 140, 54);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<RoastingRecipe> holder, IFocusGroup focuses) {
        RoastingRecipe recipe = holder.value();
        builder.addInputSlot(6, 6).setStandardSlotBackground().add(recipe.ingredient());
        builder.addOutputSlot(72, 6).setOutputSlotBackground().add(recipe.result());
        recipe.gas().ifPresent(gas -> builder.addSlot(RecipeIngredientRole.OUTPUT, 102, 6).setStandardSlotBackground()
                .add(gas.fluid(), gas.amount()).setFluidRenderer(gas.amount(), false, 16, 16));
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, RecipeHolder<RoastingRecipe> holder, IFocusGroup focuses) {
        builder.addAnimatedRecipeArrowWidget(Math.min(holder.value().ticks(), 400)).setPosition(32, 6);
    }

    @Override
    public void draw(RecipeHolder<RoastingRecipe> holder, IRecipeSlotsView slots, GuiGraphicsExtractor g, double mouseX, double mouseY) {
        heat(g, "roast", holder.value().minTemperature(), 6, 28);
        time(g, holder.value().ticks(), 6, 28 + 14);
    }
}
