package dev.strataindustria.compat.jei;

import dev.strataindustria.compat.recipeview.Processes;
import dev.strataindustria.registry.Tier4Items;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** One bake in the coke oven: the charge, the coke, and the creosote it sweats out. */
public class CokingCategory extends StrataCategory<Processes.Coking> {
    public CokingCategory(IGuiHelper gui) {
        super(JeiTypes.COKING, "coking", gui.createDrawableItemLike(Tier4Items.COKE.get()), 130, 50);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Processes.Coking coking, IFocusGroup focuses) {
        builder.addInputSlot(10, 16).setStandardSlotBackground().addItemStacks(coking.input());
        builder.addOutputSlot(80, 16).setOutputSlotBackground().add(coking.result());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 106, 16).setStandardSlotBackground()
                .add(coking.creosote(), coking.creosoteAmount()).setFluidRenderer(coking.creosoteAmount(), false, 16, 16);
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, Processes.Coking coking, IFocusGroup focuses) {
        builder.addAnimatedRecipeFlameWidget(Math.min(coking.ticks(), 400)).setPosition(43, 1);
        builder.addAnimatedRecipeArrowWidget(Math.min(coking.ticks(), 400)).setPosition(38, 16);
    }

    @Override
    public void draw(Processes.Coking coking, IRecipeSlotsView slots, GuiGraphicsExtractor g, double mouseX, double mouseY) {
        time(g, coking.ticks(), 10, 40);
    }
}
