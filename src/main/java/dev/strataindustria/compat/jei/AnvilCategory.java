package dev.strataindustria.compat.jei;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.compat.recipeview.IngredientStacks;
import dev.strataindustria.compat.recipeview.RecipeText;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.smithing.AnvilBlockEntity;
import dev.strataindustria.smithing.AnvilRecipe;
import dev.strataindustria.smithing.HitType;
import dev.strataindustria.smithing.Rule;
import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * Anvil smithing: the workpiece, the result, the finishing rules drawn with the anvil screen's own hit
 * icons and pips, and the heat the workpiece must be worked at (it follows the input as it cycles).
 */
public class AnvilCategory extends StrataCategory<RecipeHolder<AnvilRecipe>> {
    /** The anvil screen sheet: hit icons start at (176, 18), the "any hit" icon is at (240, 18). */
    private static final Identifier ANVIL = StrataIndustria.id("textures/gui/anvil.png");
    private static final int ICON_U = 176, ICON_V = 18, ANY_HIT_U = 240, ANY_HIT_V = 18;
    private static final int RULES_X = 100, RULES_Y = 2, RULE_W = 20;

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
        List<Rule> rules = holder.value().rules();
        for (int i = 0; i < rules.size(); i++) {
            Rule rule = rules.get(i);
            int x = RULES_X + i * RULE_W, y = RULES_Y;
            frame(g, x, y, 18, 18);
            if (rule.hit() == Rule.Kind.HIT) {
                g.blit(RenderPipelines.GUI_TEXTURED, ANVIL, x + 1, y + 1, ANY_HIT_U, ANY_HIT_V, 16, 16, 256, 256);
            } else {
                int icon = kindIcon(rule.hit()).ordinal();
                g.blit(RenderPipelines.GUI_TEXTURED, ANVIL, x + 1, y + 1, ICON_U + (icon % 4) * 16, ICON_V + (icon / 4) * 16, 16, 16, 256, 256);
            }
            // Three pips, third last to last, lit where the hit has to land: the anvil screen's notation.
            for (int p = 0; p < 3; p++) {
                boolean wanted = switch (rule.where()) {
                    case LAST -> p == 2;
                    case SECOND_LAST -> p == 1;
                    case THIRD_LAST -> p == 0;
                    case NOT_LAST -> p < 2;
                    case ANY -> true;
                };
                int px = x + 2 + p * 5, py = y + 20;
                g.fill(px, py, px + 4, py + 3, wanted ? 0xFFF0C040 : 0xFF555555);
            }
        }
        ItemStack piece = slots.findSlotByName("piece").flatMap(s -> s.getDisplayedItemStack()).orElse(ItemStack.EMPTY);
        int working = piece.isEmpty() ? 0 : AnvilBlockEntity.workingTemperature(piece);
        if (working > 0) heat(g, "work", working, 6, 30);
    }

    private static HitType kindIcon(Rule.Kind kind) {
        return switch (kind) {
            case HIT -> HitType.MEDIUM;
            case DRAW -> HitType.DRAW;
            case PUNCH -> HitType.PUNCH;
            case BEND -> HitType.BEND;
            case UPSET -> HitType.UPSET;
            case SHRINK -> HitType.SHRINK;
        };
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, RecipeHolder<AnvilRecipe> holder, IRecipeSlotsView slots, double mouseX, double mouseY) {
        List<Rule> rules = holder.value().rules();
        for (int i = 0; i < rules.size(); i++) {
            if (!over(mouseX, mouseY, RULES_X + i * RULE_W, RULES_Y, 18, 24)) continue;
            Rule rule = rules.get(i);
            tooltip.add(Component.translatable(StrataIndustria.MOD_ID + ".anvil.rule",
                    Component.translatable(StrataIndustria.MOD_ID + ".anvil.kind." + rule.hit().getSerializedName()),
                    Component.translatable(StrataIndustria.MOD_ID + ".anvil.where." + rule.where().getSerializedName())));
        }
        if (over(mouseX, mouseY, 6, 30, 150, 12)) tooltip.add(RecipeText.key("heat.tooltip"));
    }
}
