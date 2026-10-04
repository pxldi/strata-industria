package dev.strataindustria.compat.jei;

import dev.strataindustria.compat.recipeview.RecipeText;
import dev.strataindustria.processing.CrushingRecipe;
import dev.strataindustria.registry.Tier4Items;
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

/** The steam crusher and the macerator: a sure product and up to three chance extras. Some ores need the macerator. */
public class CrushingCategory extends StrataCategory<RecipeHolder<CrushingRecipe>> {
    private static final int CHANCE_X = 98;

    public CrushingCategory(IGuiHelper gui) {
        super(JeiTypes.CRUSHING, "crushing", gui.createDrawableItemLike(Tier4Items.CRUSHER.get()), 156, 52);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<CrushingRecipe> holder, IFocusGroup focuses) {
        CrushingRecipe recipe = holder.value();
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
    public void createRecipeExtras(IRecipeExtrasBuilder builder, RecipeHolder<CrushingRecipe> holder, IFocusGroup focuses) {
        builder.addAnimatedRecipeArrowWidget(100).setPosition(30, 8);
    }

    @Override
    public void draw(RecipeHolder<CrushingRecipe> holder, IRecipeSlotsView slots, GuiGraphicsExtractor g, double mouseX, double mouseY) {
        List<WashingRecipe.Chance> chances = holder.value().chances();
        for (int i = 0; i < chances.size(); i++) {
            Component odds = Component.literal(RecipeText.percent(chances.get(i).chance() * 100) + "%");
            centred(g, odds, CHANCE_X + i * SLOT + 8, 27, TEXT_FAINT);
        }
        boolean macerator = holder.value().minTier().isPresent();
        text(g, RecipeText.key(macerator ? "crushing.macerator_only" : "crushing.both"), 6, 40, macerator ? TEXT_WARN : TEXT_FAINT);
    }
}
