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

/** Firing unfired clay in a pit kiln under straw and logs. */
public class PitKilnCategory extends StrataCategory<Processes.Firing> {
    public PitKilnCategory(IGuiHelper gui) {
        super(JeiTypes.PIT_KILN, "pit_kiln", gui.createDrawableItemLike(ModItems.UNFIRED_SMALL_VESSEL.get()), 130, 62);
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
        text(g, RecipeText.key("pit_kiln.fuel"), 10, 40 + LINE + 1, TEXT_FAINT);
    }
}
