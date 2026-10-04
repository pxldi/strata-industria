package dev.strataindustria.compat.jei;

import dev.strataindustria.compat.recipeview.IngredientStacks;
import dev.strataindustria.compat.recipeview.RecipeText;
import dev.strataindustria.knapping.GridPattern;
import dev.strataindustria.knapping.Knapping;
import dev.strataindustria.knapping.KnappingRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * Knapping and clay forming: the 5x5 shape to leave, drawn in the material being worked (rock, flint or
 * clay, following the input slot as it cycles), the material and its cost, and the result.
 */
public class KnappingCategory extends StrataCategory<RecipeHolder<KnappingRecipe>> {
    private static final int CELL = 10, GRID = GridPattern.SIZE * CELL + 2;
    private static final int INPUT_X = 64, INPUT_Y = 8, ARROW_X = 88, ARROW_Y = 19, OUTPUT_X = 124, OUTPUT_Y = 19;

    public KnappingCategory(IGuiHelper gui, boolean clay) {
        super(clay ? JeiTypes.CLAY_FORMING : JeiTypes.KNAPPING, clay ? "clay_forming" : "knapping",
                gui.createDrawableItemLike(clay ? Items.CLAY_BALL : Items.FLINT), 150, GRID);
    }

    /** Whether a knapping recipe works clay, so it belongs on the clay forming page. */
    public static boolean isClay(KnappingRecipe recipe) {
        return recipe.ingredient().test(new ItemStack(Items.CLAY_BALL))
                || recipe.ingredient().test(new ItemStack(dev.strataindustria.registry.ModItems.FIRE_CLAY_BALL.get()));
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<KnappingRecipe> holder, IFocusGroup focuses) {
        KnappingRecipe recipe = holder.value();
        builder.addInputSlot(INPUT_X, INPUT_Y).setStandardSlotBackground().setSlotName("material")
                .addItemStacks(IngredientStacks.of(recipe.ingredient(), recipe.consume()));
        builder.addOutputSlot(OUTPUT_X, OUTPUT_Y).setOutputSlotBackground().add(recipe.result());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, RecipeHolder<KnappingRecipe> holder, IFocusGroup focuses) {
        builder.addRecipeArrowWidget().setPosition(ARROW_X, ARROW_Y);
    }

    @Override
    public void draw(RecipeHolder<KnappingRecipe> holder, IRecipeSlotsView slots, GuiGraphicsExtractor g, double mouseX, double mouseY) {
        KnappingRecipe recipe = holder.value();
        ItemStack material = slots.findSlotByName("material").flatMap(s -> s.getDisplayedItemStack()).orElse(new ItemStack(Items.FLINT));
        Identifier texture = Knapping.gridTexture(material);
        frame(g, 0, 0, GRID, GRID);
        int mask = recipe.pattern();
        for (int cell = 0; cell < GridPattern.CELLS; cell++) {
            int cx = cell % GridPattern.SIZE, cy = cell / GridPattern.SIZE;
            int x = 1 + cx * CELL, y = 1 + cy * CELL;
            if ((mask & (1 << cell)) == 0) {
                // A struck-out cell: a dark hollow, lit along its lower edge.
                g.fill(x, y, x + CELL, y + CELL, 0xFF2E2B29);
                g.fill(x, y + CELL - 1, x + CELL, y + CELL, 0xFF4A4642);
                continue;
            }
            g.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, (cx * 3) % 6, (cy * 3) % 6, CELL, CELL, 16, 16);
            // The same edge shading as the knapping screen, so the kept shape reads as raised.
            if (cy < GridPattern.SIZE - 1 && !kept(mask, cell + GridPattern.SIZE)) g.fill(x, y + CELL - 2, x + CELL, y + CELL, 0x70000000);
            if (cx < GridPattern.SIZE - 1 && !kept(mask, cell + 1)) g.fill(x + CELL - 2, y, x + CELL, y + CELL, 0x50000000);
            if (cy > 0 && !kept(mask, cell - GridPattern.SIZE)) g.fill(x, y, x + CELL, y + 1, 0x40FFFFFF);
            if (cx > 0 && !kept(mask, cell - 1)) g.fill(x, y, x + 1, y + CELL, 0x30FFFFFF);
        }
        if (recipe.mirror() && GridPattern.mirror(mask) != mask) {
            centred(g, RecipeText.key("knapping.mirror"), INPUT_X + 8, INPUT_Y + 24, TEXT_FAINT);
        }
    }

    private static boolean kept(int mask, int cell) {
        return (mask & (1 << cell)) != 0;
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, RecipeHolder<KnappingRecipe> holder, IRecipeSlotsView slots, double mouseX, double mouseY) {
        if (over(mouseX, mouseY, 0, 0, GRID, GRID)) {
            tooltip.add(RecipeText.key("knapping.grid"));
            if (holder.value().mirror()) tooltip.add(RecipeText.key("knapping.grid_mirror"));
        }
    }

}
