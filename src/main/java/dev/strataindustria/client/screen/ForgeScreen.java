package dev.strataindustria.client.screen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.forge.ForgeBlockEntity;
import dev.strataindustria.forge.ForgeLimits;
import dev.strataindustria.forge.ForgeMenu;
import dev.strataindustria.heat.Heat;
import dev.strataindustria.heat.HeatBand;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Forge: four heating slots, each with a strip under it in its item's heat colour; flames over the
 * fuel; the forge's own gauge on the right, 0 to 1750 °C.
 */
public class ForgeScreen extends AbstractContainerScreen<ForgeMenu> {
    private static final Identifier BACKGROUND = StrataIndustria.id("textures/gui/forge.png");
    private static final Identifier FLAME = Identifier.withDefaultNamespace("container/furnace/lit_progress");

    public static final int GAUGE_X = 151, GAUGE_Y = 17, GAUGE_W = 10, GAUGE_H = 58;
    public static final float GAUGE_MAX = 1750.0f;
    public static final int FLAME_X = 81, FLAME_Y = 40;
    /** Strip under each heating slot. */
    public static final int STRIP_Y = 36, STRIP_H = 2;

    public ForgeScreen(ForgeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 170);
        this.inventoryLabelY = ForgeMenu.INVENTORY_Y - 11;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);

        if (menu.burnFraction() > 0) {
            int h = Math.round(menu.burnFraction() * 13) + 1;
            g.blitSprite(RenderPipelines.GUI_TEXTURED, FLAME, 14, 14, 0, 14 - h, leftPos + FLAME_X, topPos + FLAME_Y + 14 - h, 14, h);
        }

        float temperature = menu.temperature();
        int filled = Math.round(Math.min(1.0f, Math.max(0.0f, temperature / GAUGE_MAX)) * GAUGE_H);
        if (filled > 0) {
            int x = leftPos + GAUGE_X, bottom = topPos + GAUGE_Y + GAUGE_H;
            g.fill(x, bottom - filled, x + GAUGE_W, bottom, 0xFF000000 | HeatBand.of(temperature).colour());
            g.fill(x, bottom - filled, x + 2, bottom, 0x30FFFFFF);
        }

        long now = minecraft.level == null ? 0 : minecraft.level.getGameTime();
        for (int i = 0; i < ForgeBlockEntity.HEAT_SLOTS; i++) {
            ItemStack stack = menu.getSlot(1 + i).getItem();
            if (stack.isEmpty()) continue;
            float heat = Heat.get(stack, now);
            HeatBand band = HeatBand.of(heat);
            int x = leftPos + ForgeMenu.HEAT_X + i * 18, y = topPos + STRIP_Y;
            if (band != HeatBand.NONE) g.fill(x, y, x + 16, y + STRIP_H, 0xFF000000 | band.colour());
            // At its limit an item sits just under its melting point; a pulsing edge warns of it.
            if (ForgeLimits.atLimit(stack, heat) && (now / 10) % 2 == 0) {
                g.fill(x - 1, topPos + ForgeMenu.HEAT_Y - 1, x + 17, topPos + ForgeMenu.HEAT_Y, 0xFFF8C23A);
            }
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
