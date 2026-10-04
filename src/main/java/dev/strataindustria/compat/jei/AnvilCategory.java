package dev.strataindustria.compat.jei;

import dev.strataindustria.compat.recipeview.Processes;
import dev.strataindustria.compat.recipeview.RecipeText;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.smithing.AnvilBlockEntity;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

/**
 * Work at the anvil: a piece struck into a shape, or two pieces welded into one. Shows the blows it takes
 * (more for iron and steel) and the heat the metal must be worked or welded at, which follows the input as it cycles.
 */
public class AnvilCategory extends StrataCategory<Processes.AnvilWork> {
    public AnvilCategory(IGuiHelper gui) {
        super(JeiTypes.ANVIL, "anvil", gui.createDrawableItemLike(ModItems.BRONZE_ANVIL.get()), 162, 44);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Processes.AnvilWork work, IFocusGroup focuses) {
        builder.addInputSlot(6, 6).setStandardSlotBackground().setSlotName("piece").addItemStacks(work.piece());
        if (work.weld()) builder.addInputSlot(38, 6).setStandardSlotBackground().setSlotName("second").addItemStacks(work.second());
        builder.addOutputSlot(66, 6).setOutputSlotBackground().add(work.result());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, Processes.AnvilWork work, IFocusGroup focuses) {
        if (work.weld()) {
            builder.addRecipePlusSignWidget().setPosition(24, 8);
            builder.addRecipeArrowWidget().setPosition(42, 6);
        } else {
            builder.addRecipeArrowWidget().setPosition(30, 6);
        }
    }

    @Override
    public void draw(Processes.AnvilWork work, IRecipeSlotsView slots, GuiGraphicsExtractor g, double mouseX, double mouseY) {
        text(g, RecipeText.key("anvil.blows", work.blows()), 92, 11, TEXT);
        ItemStack piece = slots.findSlotByName("piece").flatMap(s -> s.getDisplayedItemStack()).orElse(ItemStack.EMPTY);
        if (work.weld()) {
            ItemStack second = slots.findSlotByName("second").flatMap(s -> s.getDisplayedItemStack()).orElse(ItemStack.EMPTY);
            int needed = AnvilBlockEntity.weldingTemperature(piece, second);
            if (needed > 0) heat(g, "weld", needed, 6, 30);
        } else {
            int working = piece.isEmpty() ? 0 : AnvilBlockEntity.workingTemperature(piece);
            if (working > 0) heat(g, "work", working, 6, 30);
        }
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, Processes.AnvilWork work, IRecipeSlotsView slots, double mouseX, double mouseY) {
        if (over(mouseX, mouseY, 92, 8, 66, 14)) tooltip.add(RecipeText.key("anvil.blows.tip"));
        if (over(mouseX, mouseY, 6, 30, 150, 12)) tooltip.add(RecipeText.key("heat.tooltip"));
    }
}
