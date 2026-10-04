package dev.strataindustria.compat.jei;

import dev.strataindustria.compat.recipeview.Processes;
import dev.strataindustria.compat.recipeview.RecipeText;
import dev.strataindustria.registry.ModItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Firing unfired clay on the hearth stones of a fire pit. */
public class FiringCategory extends StrataCategory<Processes.Firing> {
    public FiringCategory(IGuiHelper gui) {
        super(JeiTypes.FIRING, "firing", gui.createDrawableItemLike(ModItems.UNFIRED_CRUCIBLE.get()), 130, 62);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Processes.Firing firing, IFocusGroup focuses) {
        builder.addInputSlot(10, 16).setStandardSlotBackground().add(firing.input());
        builder.addOutputSlot(80, 16).setOutputSlotBackground().add(firing.result());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, Processes.Firing firing, IFocusGroup focuses) {
        builder.addAnimatedRecipeFlameWidget(200).setPosition(43, 1);
        builder.addAnimatedRecipeArrowWidget(200).setPosition(38, 16);
    }

    @Override
    public void draw(Processes.Firing firing, IRecipeSlotsView slots, GuiGraphicsExtractor g, double mouseX, double mouseY) {
        time(g, firing.ticks(), 10, 40);
        text(g, RecipeText.key("firing.fuel"), 10, 40 + LINE + 1, TEXT_FAINT);
    }
}
