package dev.strataindustria.compat.jei;

import dev.strataindustria.compat.recipeview.RecipeText;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.smithing.AnvilBlockEntity;
import dev.strataindustria.smithing.WeldingRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/** Welding on an anvil: two hot pieces and a pinch of flux, at the hotter piece's welding heat. */
public class WeldingCategory extends StrataCategory<RecipeHolder<WeldingRecipe>> {
    public WeldingCategory(IGuiHelper gui) {
        super(JeiTypes.WELDING, "welding", gui.createDrawableItemLike(ModItems.FLUX.get()), 150, 44);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<WeldingRecipe> holder, IFocusGroup focuses) {
        WeldingRecipe recipe = holder.value();
        builder.addInputSlot(6, 6).setStandardSlotBackground().setSlotName("first").add(recipe.first());
        builder.addInputSlot(38, 6).setStandardSlotBackground().setSlotName("second").add(recipe.second());
        builder.addOutputSlot(66, 6).setOutputSlotBackground().add(recipe.result());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, RecipeHolder<WeldingRecipe> holder, IFocusGroup focuses) {
        builder.addRecipePlusSignWidget().setPosition(24, 8);
        builder.addRecipeArrowWidget().setPosition(42, 6);
    }

    @Override
    public void draw(RecipeHolder<WeldingRecipe> holder, IRecipeSlotsView slots, GuiGraphicsExtractor g, double mouseX, double mouseY) {
        ItemStack a = slots.findSlotByName("first").flatMap(s -> s.getDisplayedItemStack()).orElse(ItemStack.EMPTY);
        ItemStack b = slots.findSlotByName("second").flatMap(s -> s.getDisplayedItemStack()).orElse(ItemStack.EMPTY);
        text(g, RecipeText.key("anvil.blows", dev.strataindustria.smithing.Smithing.WELD_BLOWS), 92, 11, TEXT);
        int needed = AnvilBlockEntity.weldingTemperature(a, b);
        if (needed > 0) heat(g, "weld", needed, 6, 30);
    }
}
