package dev.strataindustria.client.screen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.heat.HeatBand;
import dev.strataindustria.steam.FireboxBlockEntity;
import dev.strataindustria.steam.FireboxMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Firebox: four fuel slots under the fire, the gauge on the right (0 to 1750 °C, as on the forge),
 * and two lines saying what the fire makes, where it goes, and whether a blower fans it.
 */
public class FireboxScreen extends AbstractContainerScreen<FireboxMenu> {
    private static final Identifier BACKGROUND = StrataIndustria.id("textures/gui/firebox.png");
    private static final String KEY = StrataIndustria.MOD_ID + ".firebox.";
    public static final int GAUGE_X = 151, GAUGE_Y = 17, GAUGE_W = 10, GAUGE_H = 58;
    public static final float GAUGE_MAX = 1750.0f;
    /** The flame over the fuel; its lit sprite is at (FLAME_U, 0) in the texture. */
    public static final int FLAME_X = 63, FLAME_Y = 35, FLAME_SIZE = 14, FLAME_U = 192;
    public static final int TEXT_X = 8, TEXT_Y = 18;

    public FireboxScreen(FireboxMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        this.inventoryLabelY = FireboxMenu.INVENTORY_Y - 11;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        if (menu.output() > 0) {
            // The flame burns down with the current fuel item.
            int h = Math.max(1, Math.round(menu.burnFraction() * FLAME_SIZE));
            g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos + FLAME_X, topPos + FLAME_Y + FLAME_SIZE - h,
                    FLAME_U, FLAME_SIZE - h, FLAME_SIZE, h, 256, 256);
        }
        float temperature = menu.temperature();
        int filled = Math.round(Math.min(1.0f, Math.max(0.0f, temperature / GAUGE_MAX)) * GAUGE_H);
        if (filled > 0 && HeatBand.of(temperature) != HeatBand.NONE) {
            int x = leftPos + GAUGE_X, bottom = topPos + GAUGE_Y + GAUGE_H;
            g.fill(x, bottom - filled, x + GAUGE_W, bottom, 0xFF000000 | HeatBand.of(temperature).colour());
            g.fill(x, bottom - filled, x + 2, bottom, 0x30FFFFFF);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        FireboxBlockEntity.Status status = menu.status();
        Component line = switch (status) {
            case HEATING -> Component.translatable(status.key(), menu.taken(), menu.output());
            case IDLE -> Component.translatable(status.key(), menu.output());
            default -> Component.translatable(status.key());
        };
        int colour = status == FireboxBlockEntity.Status.IDLE ? 0xFF8A6A2A : 0xFF404040;
        g.text(font, line, TEXT_X, TEXT_Y, colour, false);
        if (menu.blown() && menu.output() > 0) {
            g.text(font, Component.translatable(KEY + "blower", FireboxBlockEntity.BLOWER_TEMPERATURE), TEXT_X, TEXT_Y + 9, 0xFF404040, false);
        }
        HeatBand band = HeatBand.of(menu.temperature());
        if (band != HeatBand.NONE) {
            Component name = band.displayName();
            g.text(font, name, GAUGE_X - 5 - font.width(name), FLAME_Y + 3, 0xFF404040, false);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);
        int x = mouseX - leftPos, y = mouseY - topPos;
        if (x >= GAUGE_X && x < GAUGE_X + GAUGE_W && y >= GAUGE_Y && y < GAUGE_Y + GAUGE_H) {
            g.setTooltipForNextFrame(Component.translatable(KEY + "temperature", Math.round(menu.temperature())), mouseX, mouseY);
        }
    }
}
