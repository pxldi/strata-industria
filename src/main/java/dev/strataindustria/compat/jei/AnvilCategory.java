package dev.strataindustria.compat.jei;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.compat.recipeview.IngredientStacks;
import dev.strataindustria.compat.recipeview.RecipeText;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.smithing.AnvilBlockEntity;
import dev.strataindustria.smithing.AnvilRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * Anvil striking: the workpiece, the result, how many blows the shape takes (more for iron and steel), and
 * the heat the workpiece must be worked at (it follows the input as it cycles).
 */
public class AnvilCategory extends StrataCategory<RecipeHolder<AnvilRecipe>> {
    public AnvilCategory(IGuiHelper gui) {
        super(JeiTypes.ANVIL, "anvil", gui.createDrawableItemLike(ModItems.BRONZE_ANVIL.get()), 162, 44);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<AnvilRecipe> holder, IFocusGroup focuses) {
        AnvilRecipe recipe = holder.value();
        builder.addInputSlot(6, 6).setStandardSlotBackground().setSlotName("piece")
                .addItemStacks(IngredientStacks.of(recipe.ingredient(), recipe.count()));
        builder.addOutputSlot(66, 6).setOutputSlotBackground().add(recipe.result());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, RecipeHolder<AnvilRecipe> holder, IFocusGroup focuses) {
        builder.addRecipeArrowWidget().setPosition(30, 6);
    }

    @Override
    public void draw(RecipeHolder<AnvilRecipe> holder, IRecipeSlotsView slots, GuiGraphicsExtractor g, double mouseX, double mouseY) {
        text(g, RecipeText.key("anvil.blows", holder.value().blows()), 92, 11, TEXT);
        ItemStack piece = slots.findSlotByName("piece").flatMap(s -> s.getDisplayedItemStack()).orElse(ItemStack.EMPTY);
        int working = piece.isEmpty() ? 0 : AnvilBlockEntity.workingTemperature(piece);
        if (working > 0) heat(g, "work", working, 6, 30);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, RecipeHolder<AnvilRecipe> holder, IRecipeSlotsView slots, double mouseX, double mouseY) {
        if (over(mouseX, mouseY, 92, 8, 66, 14)) tooltip.add(RecipeText.key("anvil.blows.tip"));
        if (over(mouseX, mouseY, 6, 30, 150, 12)) tooltip.add(RecipeText.key("heat.tooltip"));
    }
}
