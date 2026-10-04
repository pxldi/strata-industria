package dev.strataindustria.client.screen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.ceramics.KilnBlockEntity;
import dev.strataindustria.ceramics.KilnMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Kiln: what goes in on the left, the fire burning down as the batch fires, what comes out on the right. */
public class KilnScreen extends AbstractContainerScreen<KilnMenu> {
    private static final Identifier BACKGROUND = StrataIndustria.id("textures/gui/kiln.png");
    public static final int FLAME_X = 81, FLAME_Y = 28, FLAME_SIZE = 14, FLAME_U = 176, FLAME_V = 0;
    public static final int STATUS_Y = 60;

    public KilnScreen(KilnMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 176);
        this.inventoryLabelY = KilnMenu.INVENTORY_Y - 11;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        if (menu.status() == KilnBlockEntity.Status.FIRING) {
            // The flame rises as the batch fires.
            int h = Math.max(1, Math.round(menu.progress() * FLAME_SIZE));
            g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos + FLAME_X, topPos + FLAME_Y + FLAME_SIZE - h,
                    FLAME_U, FLAME_V + FLAME_SIZE - h, FLAME_SIZE, h, 256, 256);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        KilnBlockEntity.Status status = menu.status();
        Component line = status == KilnBlockEntity.Status.FIRING
                ? Component.translatable(status.key(), Math.round(menu.progress() * 100))
                : Component.translatable(status.key());
        boolean fine = status == KilnBlockEntity.Status.FIRING || status == KilnBlockEntity.Status.EMPTY;
        g.text(font, line, imageWidth / 2 - font.width(line) / 2, STATUS_Y, fine ? HeatLine.FINE : HeatLine.SHORT, false);
        int need = KilnBlockEntity.MIN_TEMPERATURE;
        if (status != KilnBlockEntity.Status.EMPTY && status != KilnBlockEntity.Status.NO_RECIPE) {
            Component heat = HeatLine.line("kiln", need, menu.temperature(), menu.heat(), menu.limit());
            g.text(font, heat, imageWidth / 2 - font.width(heat) / 2, STATUS_Y + 10, HeatLine.colour(need, menu.temperature()), false);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);
        int x = mouseX - leftPos, y = mouseY - topPos;
        if (y >= STATUS_Y + 9 && y < STATUS_Y + 19 && x >= 8 && x < imageWidth - 8) {
            g.setTooltipForNextFrame(HeatLine.tooltip(KilnBlockEntity.MIN_TEMPERATURE, menu.temperature(), menu.limit()), mouseX, mouseY);
        }
    }
}
