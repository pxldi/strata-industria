package dev.strataindustria.compat.jei;

import dev.strataindustria.compat.recipeview.RecipeText;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.washing.WashingRecipe;
import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;

/** Washing in a pan or a sluice: a sure product and up to three chance finds, each with its odds. */
public class WashingCategory extends StrataCategory<RecipeHolder<WashingRecipe>> {
    private static final int CHANCE_X = 98;

    public WashingCategory(IGuiHelper gui) {
        super(JeiTypes.WASHING, "washing", gui.createDrawableItemLike(ModItems.WASHING_PAN.get()), 156, 52);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<WashingRecipe> holder, IFocusGroup focuses) {
        WashingRecipe recipe = holder.value();
        builder.addInputSlot(6, 8).setStandardSlotBackground().add(recipe.ingredient());
        builder.addOutputSlot(66, 8).setOutputSlotBackground().add(recipe.result());
        List<WashingRecipe.Chance> chances = recipe.chances();
        for (int i = 0; i < chances.size(); i++) {
            WashingRecipe.Chance chance = chances.get(i);
            Component odds = RecipeText.key("chance", RecipeText.percent(chance.chance() * 100));
            builder.addOutputSlot(CHANCE_X + i * SLOT, 8).setStandardSlotBackground().add(chance.item())
                    .addRichTooltipCallback((view, tooltip) -> tooltip.add(odds));
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, RecipeHolder<WashingRecipe> holder, IFocusGroup focuses) {
        builder.addAnimatedRecipeArrowWidget(holder.value().ticks()).setPosition(30, 8);
    }

    @Override
    public void draw(RecipeHolder<WashingRecipe> holder, IRecipeSlotsView slots, GuiGraphicsExtractor g, double mouseX, double mouseY) {
        List<WashingRecipe.Chance> chances = holder.value().chances();
        for (int i = 0; i < chances.size(); i++) {
            Component odds = Component.literal(RecipeText.percent(chances.get(i).chance() * 100) + "%");
            centred(g, odds, CHANCE_X + i * SLOT + 8, 27, TEXT_FAINT);
        }
        time(g, holder.value().ticks(), 6, 40);
        text(g, RecipeText.key("washing.sluice_only"), 6 + 11 + font().width(RecipeText.time(holder.value().ticks())) + 4, 41, TEXT_FAINT);
    }
}
