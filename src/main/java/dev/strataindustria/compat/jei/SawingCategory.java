package dev.strataindustria.compat.jei;

import dev.strataindustria.compat.recipeview.RecipeText;
import dev.strataindustria.machine.SawingRecipe;
import dev.strataindustria.registry.ModItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.crafting.RecipeHolder;

/** The saw mill: a main product and a by-product such as bark, timed at 16 RPM. */
public class SawingCategory extends StrataCategory<RecipeHolder<SawingRecipe>> {
    public SawingCategory(IGuiHelper gui) {
        super(JeiTypes.SAWING, "sawing", gui.createDrawableItemLike(ModItems.SAW_MILL.get()), 130, 54);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<SawingRecipe> holder, IFocusGroup focuses) {
        SawingRecipe recipe = holder.value();
        builder.addInputSlot(10, 6).setStandardSlotBackground().add(recipe.ingredient());
        builder.addOutputSlot(76, 6).setOutputSlotBackground().add(recipe.result());
        recipe.extra().ifPresent(extra -> builder.addOutputSlot(104, 6).setStandardSlotBackground().add(extra));
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, RecipeHolder<SawingRecipe> holder, IFocusGroup focuses) {
        builder.addAnimatedRecipeArrowWidget(holder.value().ticks()).setPosition(38, 6);
    }

    @Override
    public void draw(RecipeHolder<SawingRecipe> holder, IRecipeSlotsView slots, GuiGraphicsExtractor g, double mouseX, double mouseY) {
        time(g, holder.value().ticks(), 10, 30);
        kinetic(g, RecipeText.key("kinetic.rated", 16), 10, 30 + LINE);
    }
}
