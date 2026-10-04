package dev.strataindustria.compat.jei;

import dev.strataindustria.compat.recipeview.Processes;
import dev.strataindustria.compat.recipeview.RecipeText;
import dev.strataindustria.registry.Tier4Items;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Firebox fuels: how long one item burns, how hot it lets the firebox get, and the heat it gives. */
public class FireboxFuelCategory extends StrataCategory<Processes.FireboxFuel> {
    public FireboxFuelCategory(IGuiHelper gui) {
        super(JeiTypes.FIREBOX_FUEL, "firebox_fuel", gui.createDrawableItemLike(Tier4Items.FIREBOX.get()), 140, 50);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Processes.FireboxFuel fuel, IFocusGroup focuses) {
        builder.addInputSlot(6, 15).setStandardSlotBackground().addItemStacks(fuel.fuel());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, Processes.FireboxFuel fuel, IFocusGroup focuses) {
        builder.addAnimatedRecipeFlameWidget(fuel.burnTicks()).setPosition(8, 0);
    }

    @Override
    public void draw(Processes.FireboxFuel fuel, IRecipeSlotsView slots, GuiGraphicsExtractor g, double mouseX, double mouseY) {
        heat(g, "fuel", fuel.maxTemperature(), 30, 4);
        time(g, fuel.burnTicks(), 30, 4 + 14);
        text(g, RecipeText.key("firebox.heat", fuel.heat()), 30, 4 + 14 + LINE + 1, TEXT_FAINT);
    }
}
