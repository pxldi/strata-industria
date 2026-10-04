package dev.strataindustria.compat.jei;

import dev.strataindustria.compat.recipeview.IngredientStacks;
import dev.strataindustria.compat.recipeview.RecipeText;
import dev.strataindustria.power.ElectricTier;
import dev.strataindustria.processing.ChemicalIo;
import dev.strataindustria.processing.ChemicalRecipe;
import dev.strataindustria.processing.ExtrudingRecipe;
import dev.strataindustria.tanning.FluidAmount;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;

/**
 * The mixer, electrolyser, assembler and extruder share one layout: item inputs in a grid of three, fluid
 * inputs under them, an arrow, then the item and fluid results. Below are the LV time, the lowest tier that
 * runs the recipe, and how hot the items come out. Times and powers are the LV machine's; an MV machine is
 * faster, as its own page says.
 */
public class ChemicalCategory<R extends Recipe<?> & ChemicalRecipe> extends StrataCategory<RecipeHolder<R>> {
    private static final int COLUMNS = 3, X_IN = 6, X_OUT = 98, Y = 6;

    public ChemicalCategory(IRecipeHolderType<R> type, String name, ItemLike icon, IGuiHelper gui, int height) {
        super(type, name, gui.createDrawableItemLike(icon), 138, height);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<R> holder, IFocusGroup focuses) {
        ChemicalIo io = holder.value().io();
        int i = 0;
        for (ChemicalIo.ItemInput input : io.items()) {
            builder.addInputSlot(X_IN + i % COLUMNS * SLOT, Y + i / COLUMNS * SLOT).setStandardSlotBackground()
                    .addItemStacks(IngredientStacks.of(input.ingredient(), input.count()));
            i++;
        }
        int fluidY = Y + (rows(io.items().size()) * SLOT);
        for (int f = 0; f < io.fluids().size(); f++) fluid(builder, RecipeIngredientRole.INPUT, io.fluids().get(f), X_IN + f * SLOT, fluidY);
        for (int r = 0; r < io.itemResults().size(); r++) {
            builder.addOutputSlot(X_OUT + r * SLOT, Y).setOutputSlotBackground().add(io.itemResults().get(r));
        }
        int outFluidY = Y + (io.itemResults().isEmpty() ? 0 : SLOT);
        for (int f = 0; f < io.fluidResults().size(); f++) fluid(builder, RecipeIngredientRole.OUTPUT, io.fluidResults().get(f), X_OUT + f * SLOT, outFluidY);
    }

    private static int rows(int items) {
        return (items + COLUMNS - 1) / COLUMNS;
    }

    private static void fluid(IRecipeLayoutBuilder builder, RecipeIngredientRole role, FluidAmount fluid, int x, int y) {
        builder.addSlot(role, x, y).setStandardSlotBackground().add(fluid.fluid(), fluid.amount()).setFluidRenderer(fluid.amount(), false, 16, 16);
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, RecipeHolder<R> holder, IFocusGroup focuses) {
        builder.addAnimatedRecipeArrowWidget(Math.min(holder.value().io().ticks(), 400)).setPosition(68, Y);
    }

    @Override
    public void draw(RecipeHolder<R> holder, IRecipeSlotsView slots, GuiGraphicsExtractor g, double mouseX, double mouseY) {
        ChemicalIo io = holder.value().io();
        int y = getHeight() - 2 * LINE - 4;
        time(g, io.ticks(), 6, y);
        if (holder.value() instanceof ExtrudingRecipe extruding) {
            text(g, RecipeText.key("extruder.mode." + extruding.mode()), 6 + 11 + font().width(RecipeText.time(io.ticks())) + 6, y + 1, TEXT_FAINT);
        }
        y += LINE;
        if (io.minTier() != ElectricTier.LV) {
            text(g, RecipeText.key("chemical.needs_tier", io.minTier().label()), 6, y + 1, TEXT_WARN);
        } else if (io.resultTemperature() > 0) {
            text(g, RecipeText.key("chemical.hot", RecipeText.bandName(io.resultTemperature())), 6, y + 1, TEXT);
        }
    }
}
