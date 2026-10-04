package dev.strataindustria.compat.jei;

import dev.strataindustria.quern.QuernRecipe;
import dev.strataindustria.registry.ModItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.crafting.RecipeHolder;

/** Grinding in the hand quern or the millstone. */
public class QuernCategory extends StrataCategory<RecipeHolder<QuernRecipe>> {
    public QuernCategory(IGuiHelper gui) {
        super(JeiTypes.QUERN, "quern", gui.createDrawableItemLike(ModItems.QUERN.get()), 120, 42);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<QuernRecipe> holder, IFocusGroup focuses) {
        builder.addInputSlot(10, 6).setStandardSlotBackground().add(holder.value().ingredient());
        builder.addOutputSlot(80, 6).setOutputSlotBackground().add(holder.value().result());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, RecipeHolder<QuernRecipe> holder, IFocusGroup focuses) {
        builder.addAnimatedRecipeArrowWidget(holder.value().ticks()).setPosition(38, 6);
    }

    @Override
    public void draw(RecipeHolder<QuernRecipe> holder, IRecipeSlotsView slots, GuiGraphicsExtractor g, double mouseX, double mouseY) {
        time(g, holder.value().ticks(), 10, 30);
    }
}
