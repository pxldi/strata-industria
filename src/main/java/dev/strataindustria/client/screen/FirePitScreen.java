package dev.strataindustria.client.screen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.fire.FirePitMenu;
import dev.strataindustria.heat.HeatBand;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Fire pit: flames burn down like a furnace's, the gauge fills in heat-band colours. */
public class FirePitScreen extends AbstractContainerScreen<FirePitMenu> {
    private static final Identifier BACKGROUND = StrataIndustria.id("textures/gui/fire_pit.png");
    private static final Identifier FLAME = Identifier.withDefaultNamespace("container/furnace/lit_progress");

    /** Inner area of the temperature gauge (spec 3.5 range, 0 to 700 °C). */
    public static final int GAUGE_X = 151, GAUGE_Y = 17, GAUGE_W = 10, GAUGE_H = 54;
    public static final float GAUGE_MAX = 700.0f;
    /** Inner area of the cooking progress bar, right of the cooking slot. */
    public static final int COOK_BAR_X = 100, COOK_BAR_Y = 24, COOK_BAR_W = 22, COOK_BAR_H = 4;
    public static final int FLAME_X = 81, FLAME_Y = 37;

    public FirePitScreen(FirePitMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);

        int flame = Math.round(menu.burnFraction() * 13);
        if (menu.burnFraction() > 0) {
            int h = flame + 1;
            g.blitSprite(RenderPipelines.GUI_TEXTURED, FLAME, 14, 14, 0, 14 - h, leftPos + FLAME_X, topPos + FLAME_Y + 14 - h, 14, h);
        }

        float temperature = menu.temperature();
        int filled = Math.round(Math.min(1.0f, Math.max(0.0f, temperature / GAUGE_MAX)) * GAUGE_H);
        if (filled > 0) {
            int colour = 0xFF000000 | HeatBand.of(temperature).colour();
            int x = leftPos + GAUGE_X, bottom = topPos + GAUGE_Y + GAUGE_H;
            g.fill(x, bottom - filled, x + GAUGE_W, bottom, colour);
            // A lit strip down the left edge keeps the column reading as a glowing bar.
            g.fill(x, bottom - filled, x + 2, bottom, 0x30FFFFFF);
        }

        int cooked = Math.round(menu.cookFraction() * COOK_BAR_W);
        if (cooked > 0) {
            g.fill(leftPos + COOK_BAR_X, topPos + COOK_BAR_Y, leftPos + COOK_BAR_X + cooked, topPos + COOK_BAR_Y + COOK_BAR_H, 0xFFE0A040);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        HeatBand band = HeatBand.of(menu.temperature());
        if (band != HeatBand.NONE) {
            Component name = band.displayName();
            g.text(font, name, GAUGE_X - 5 - font.width(name), FLAME_Y + 3, 0xFF404040, false);
        }
    }
}
