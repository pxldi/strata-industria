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

/**
 * Crucible alloying: the two metals in whole parts (or the share range each must fall in), the heat the mix has to reach, and
 * whether a clay crucible can get that hot.
 */
public class AlloyingCategory extends StrataCategory<Processes.Alloying> {
    public AlloyingCategory(IGuiHelper gui) {
        super(JeiTypes.ALLOYING, "alloying", gui.createDrawableItemLike(ModItems.CRUCIBLE.get()), 150, 66);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Processes.Alloying alloy, IFocusGroup focuses) {
        builder.addInputSlot(6, 6).setStandardSlotBackground().addItemStacks(alloy.base());
        builder.addInputSlot(42, 6).setStandardSlotBackground().addItemStacks(alloy.added());
        builder.addOutputSlot(118, 6).setOutputSlotBackground().add(alloy.result());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, Processes.Alloying alloy, IFocusGroup focuses) {
        builder.addRecipePlusSignWidget().setPosition(25, 8);
        builder.addRecipeArrowWidget().setPosition(76, 6);
    }

    @Override
    public void draw(Processes.Alloying alloy, IRecipeSlotsView slots, GuiGraphicsExtractor g, double mouseX, double mouseY) {
        if (alloy.alloy().byParts()) {
            text(g, RecipeText.key("alloying.parts", alloy.alloy().baseParts(), RecipeText.metal(alloy.alloy().base()),
                    alloy.alloy().addedParts(), RecipeText.metal(alloy.alloy().added())), 6, 29, TEXT);
            text(g, RecipeText.key("alloying.parts_any"), 6, 29 + LINE - 1, TEXT);
        } else {
            text(g, RecipeText.key("alloying.share", RecipeText.metal(alloy.alloy().base()),
                    RecipeText.percent(alloy.baseMin()), RecipeText.percent(alloy.baseMax())), 6, 29, TEXT);
            text(g, RecipeText.key("alloying.share", RecipeText.metal(alloy.alloy().added()),
                    RecipeText.percent(alloy.addedMin()), RecipeText.percent(alloy.addedMax())), 6, 29 + LINE - 1, TEXT);
        }
        heat(g, "melt", alloy.meltingPoint(), 6, 29 + 2 * LINE);
        if (alloy.refractory()) text(g, RecipeText.key("crucible.refractory"), 6, 29 + 3 * LINE + 3, TEXT_WARN);
    }
}
