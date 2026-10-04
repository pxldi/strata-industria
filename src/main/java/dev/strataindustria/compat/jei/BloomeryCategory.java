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

/** The bloomery: iron-bearing charge and charcoal, per chimney level, fired into blooms and slag. */
public class BloomeryCategory extends StrataCategory<Processes.Bloomery> {
    public BloomeryCategory(IGuiHelper gui) {
        super(JeiTypes.BLOOMERY, "bloomery", gui.createDrawableItemLike(ModItems.BLOOMERY.get()), 150, 56);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Processes.Bloomery bloomery, IFocusGroup focuses) {
        builder.addInputSlot(6, 6).setStandardSlotBackground().addItemStacks(bloomery.ore());
        builder.addInputSlot(42, 6).setStandardSlotBackground().addItemStacks(bloomery.fuel());
        builder.addOutputSlot(102, 6).setOutputSlotBackground().add(bloomery.bloom());
        builder.addOutputSlot(128, 6).setStandardSlotBackground().add(bloomery.slag());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, Processes.Bloomery bloomery, IFocusGroup focuses) {
        builder.addRecipePlusSignWidget().setPosition(25, 8);
        builder.addAnimatedRecipeArrowWidget(200).setPosition(66, 6);
    }

    @Override
    public void draw(Processes.Bloomery bloomery, IRecipeSlotsView slots, GuiGraphicsExtractor g, double mouseX, double mouseY) {
        text(g, RecipeText.key("bloomery.per_level"), 6, 29, TEXT);
        heat(g, "bloom", bloomery.minTemperature(), 6, 29 + LINE);
    }
}
