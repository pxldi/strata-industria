package dev.strataindustria.compat.jei;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.compat.recipeview.RecipeText;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Shared look for the mod's recipe pages: slots and arrows from JEI, and an info row of small icons
 * (thermometer for heat, clock for time, gear for mechanical power) drawn from {@code gui/jei/icons.png}.
 *
 * <p>Layout conventions: inputs on the left, the arrow in the middle, outputs on the right, and the info
 * lines underneath, {@link #LINE} pixels apart.
 */
public abstract class StrataCategory<T> extends AbstractRecipeCategory<T> {
    public static final Identifier ICONS = StrataIndustria.id("textures/gui/jei/icons.png");
    public static final int ICONS_SIZE = 64;
    public static final int TEXT = 0xFF404040, TEXT_WARN = 0xFF8A3A2A, TEXT_FAINT = 0xFF707070;
    public static final int LINE = 11;
    /** Item slot size and the 24x17 JEI arrow. */
    public static final int SLOT = 18, ARROW_W = 24, ARROW_H = 17;

    protected StrataCategory(IRecipeType<T> type, String name, IDrawable icon, int width, int height) {
        super(type, RecipeText.key("category." + name), icon, width, height);
    }

    protected static Font font() {
        return Minecraft.getInstance().font;
    }

    protected static void text(GuiGraphicsExtractor g, Component line, int x, int y, int colour) {
        g.text(font(), line, x, y, colour, false);
    }

    protected static void centred(GuiGraphicsExtractor g, Component line, int centreX, int y, int colour) {
        g.text(font(), line, centreX - font().width(line) / 2, y, colour, false);
    }

    /** A thermometer filled in the band's glow colour, then "Works at Orange heat". */
    protected static void heat(GuiGraphicsExtractor g, String what, int temperature, int x, int y) {
        int colour = 0xFF000000 | RecipeText.band(temperature).colour();
        g.fill(x + 3, y + 2, x + 4, y + 9, colour);
        g.fill(x + 2, y + 8, x + 5, y + 11, colour);
        g.blit(RenderPipelines.GUI_TEXTURED, ICONS, x, y, 0, 0, 7, 12, ICONS_SIZE, ICONS_SIZE);
        text(g, RecipeText.heat(what, temperature), x + 10, y + 2, TEXT);
    }

    /** A clock and the time. */
    protected static void time(GuiGraphicsExtractor g, int ticks, int x, int y) {
        g.blit(RenderPipelines.GUI_TEXTURED, ICONS, x, y, 16, 0, 9, 9, ICONS_SIZE, ICONS_SIZE);
        text(g, RecipeText.time(ticks), x + 11, y + 1, TEXT);
    }

    /** A gear and a line about mechanical power. */
    protected static void kinetic(GuiGraphicsExtractor g, Component line, int x, int y) {
        g.blit(RenderPipelines.GUI_TEXTURED, ICONS, x, y, 32, 0, 9, 9, ICONS_SIZE, ICONS_SIZE);
        text(g, line, x + 11, y + 1, TEXT);
    }

    /** A sunken frame like an inventory slot, for tanks and grids. */
    protected static void frame(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xFF8B8B8B);
        g.fill(x, y, x + w - 1, y + 1, 0xFF373737);
        g.fill(x, y, x + 1, y + h - 1, 0xFF373737);
        g.fill(x + 1, y + h - 1, x + w, y + h, 0xFFFFFFFF);
        g.fill(x + w - 1, y + 1, x + w, y + h, 0xFFFFFFFF);
    }

    protected static boolean over(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }
}
