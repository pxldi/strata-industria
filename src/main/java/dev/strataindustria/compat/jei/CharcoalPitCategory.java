package dev.strataindustria.compat.jei;

import dev.strataindustria.compat.recipeview.Processes;
import dev.strataindustria.compat.recipeview.RecipeText;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.Items;

/** A full log pile, sealed on every side and lit, burnt down to charcoal and ash. */
public class CharcoalPitCategory extends StrataCategory<Processes.CharcoalPit> {
    public CharcoalPitCategory(IGuiHelper gui) {
        super(JeiTypes.CHARCOAL_PIT, "charcoal_pit", gui.createDrawableItemLike(Items.CHARCOAL), 130, 62);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Processes.CharcoalPit pit, IFocusGroup focuses) {
        builder.addInputSlot(10, 16).setStandardSlotBackground().addItemStacks(pit.logs());
        builder.addOutputSlot(80, 16).setOutputSlotBackground().add(pit.charcoal());
        builder.addOutputSlot(106, 16).setStandardSlotBackground().add(pit.ash());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, Processes.CharcoalPit pit, IFocusGroup focuses) {
        builder.addAnimatedRecipeFlameWidget(200).setPosition(43, 1);
        builder.addAnimatedRecipeArrowWidget(200).setPosition(38, 16);
    }

    @Override
    public void draw(Processes.CharcoalPit pit, IRecipeSlotsView slots, GuiGraphicsExtractor g, double mouseX, double mouseY) {
        time(g, pit.ticks(), 10, 40);
        text(g, RecipeText.key("charcoal_pit.cover"), 10, 40 + LINE + 1, TEXT_FAINT);
    }
}
