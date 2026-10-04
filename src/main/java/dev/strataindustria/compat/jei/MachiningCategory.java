package dev.strataindustria.compat.jei;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.compat.recipeview.RecipeText;
import dev.strataindustria.processing.MachiningRecipe;
import dev.strataindustria.registry.Tier5Items;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;

/** The wiremill, bender and lathe: one item in, one stack out. The line underneath names the machine and the lathe's mode. */
public class MachiningCategory extends StrataCategory<RecipeHolder<MachiningRecipe>> {
    public MachiningCategory(IGuiHelper gui) {
        super(JeiTypes.MACHINING, "machining", gui.createDrawableItemLike(Tier5Items.WIREMILL.get()), 110, 44);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<MachiningRecipe> holder, IFocusGroup focuses) {
        MachiningRecipe recipe = holder.value();
        builder.addInputSlot(6, 6).setStandardSlotBackground().add(recipe.ingredient());
        builder.addOutputSlot(72, 6).setOutputSlotBackground().add(recipe.result());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, RecipeHolder<MachiningRecipe> holder, IFocusGroup focuses) {
        builder.addAnimatedRecipeArrowWidget(100).setPosition(32, 6);
    }

    @Override
    public void draw(RecipeHolder<MachiningRecipe> holder, IRecipeSlotsView slots, GuiGraphicsExtractor g, double mouseX, double mouseY) {
        MachiningRecipe recipe = holder.value();
        Component machine = Component.translatable("block." + StrataIndustria.MOD_ID + "." + recipe.machine());
        Component line = recipe.mode().isPresent()
                ? RecipeText.key("machining.mode", machine, RecipeText.key("machining.mode." + recipe.mode().get())) : machine;
        text(g, line, 6, 30, TEXT);
    }
}
