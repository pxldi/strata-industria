package dev.strataindustria.compat.jei;

import dev.strataindustria.compat.recipeview.Processes;
import dev.strataindustria.compat.recipeview.RecipeText;
import dev.strataindustria.registry.ModItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Pouring a crucible's metal into a fired mold. The mold is kept unless it cracks. */
public class CastingCategory extends StrataCategory<Processes.Casting> {
    public CastingCategory(IGuiHelper gui) {
        super(JeiTypes.CASTING, "casting", gui.createDrawableItemLike(ModItems.INGOT_MOLD.get()), 150, 56);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Processes.Casting cast, IFocusGroup focuses) {
        builder.addInputSlot(6, 6).setStandardSlotBackground().addItemStacks(cast.metalItems());
        builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, 42, 6).setStandardSlotBackground().add(cast.mold());
        builder.addOutputSlot(118, 6).setOutputSlotBackground().add(cast.result());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, Processes.Casting cast, IFocusGroup focuses) {
        builder.addRecipePlusSignWidget().setPosition(25, 8);
        builder.addRecipeArrowWidget().setPosition(76, 6);
    }

    @Override
    public void draw(Processes.Casting cast, IRecipeSlotsView slots, GuiGraphicsExtractor g, double mouseX, double mouseY) {
        text(g, RecipeText.key("casting.units", cast.units(), RecipeText.metal(cast.metal())), 6, 29, TEXT);
        heat(g, "melt", cast.meltingPoint(), 6, 29 + LINE);
        if (cast.refractory()) text(g, RecipeText.key("crucible.refractory"), 6, 29 + 2 * LINE + 3, TEXT_WARN);
    }
}
