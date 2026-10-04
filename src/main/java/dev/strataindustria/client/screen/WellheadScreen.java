package dev.strataindustria.client.screen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.oil.WellheadBlockEntity;
import dev.strataindustria.oil.WellheadMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Wellhead: the casing slot, a depth bar that fills as the bore goes down, and a line on what it is doing. */
public class WellheadScreen extends AbstractContainerScreen<WellheadMenu> {
    private static final Identifier BACKGROUND = StrataIndustria.id("textures/gui/wellhead.png");
    private static final int BAR_X = 130, BAR_Y = 17, BAR_W = 12, BAR_H = 46, FILL_U = 176, FILL_V = 0;
    private static final int STATUS_Y = 68;
    private static final int FINE = 0xFF404040, SHORT = 0xFFa03020;

    public WellheadScreen(WellheadMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        this.inventoryLabelY = WellheadMenu.INVENTORY_Y - 11;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        int depth = menu.depth();
        int h = depth <= 0 ? (menu.status() == WellheadBlockEntity.Status.NO_RESERVOIR ? 0 : BAR_H) : Math.round((float) Math.min(menu.bored(), depth) * BAR_H / depth);
        if (h > 0) g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos + BAR_X, topPos + BAR_Y + BAR_H - h, FILL_U, FILL_V + BAR_H - h, BAR_W, h, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        WellheadBlockEntity.Status status = menu.status();
        String key = StrataIndustria.MOD_ID + ".wellhead.";
        Component line = switch (status) {
            case DRILLING -> Component.translatable(key + "drilling", menu.bored(), menu.depth());
            case TOO_CLOSE -> Component.translatable(status.key(), menu.nearX(), menu.nearZ());
            case FLOWING, PUMPING -> Component.translatable(status.key(), menu.rate());
            default -> Component.translatable(status.key());
        };
        boolean fine = switch (status) {
            case DRILLING, GUSHING, FLOWING, PUMPING -> true;
            default -> false;
        };
        g.text(font, line, imageWidth / 2 - font.width(line) / 2, STATUS_Y, fine ? FINE : SHORT, false);
        if (menu.remaining() >= 0 && menu.depth() >= 0 && menu.bored() >= menu.depth()) {
            Component left = Component.translatable(key + "remaining", menu.remaining());
            g.text(font, left, imageWidth / 2 - font.width(left) / 2, STATUS_Y + 10, FINE, false);
        }
    }
}
