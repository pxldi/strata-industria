package dev.strataindustria.client.screen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.roasting.RoasterBlockEntity;
import dev.strataindustria.roasting.RoasterMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Roaster: ore over calcine with an arrow filling down between each pair, and the gas tank on the right. */
public class RoasterScreen extends AbstractContainerScreen<RoasterMenu> {
    private static final Identifier BACKGROUND = StrataIndustria.id("textures/gui/roaster.png");
    public static final int ARROW_Y = 36, ARROW_SIZE = 8, ARROW_U = 176, ARROW_V = 0;
    public static final int TANK_X = 140, TANK_Y = 17, TANK_W = 16, TANK_H = 46, TANK_U = 184, TANK_V = 0;
    public static final int STATUS_Y = 68;

    public RoasterScreen(RoasterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, RoasterMenu.INVENTORY_Y + 82);
        this.inventoryLabelY = RoasterMenu.INVENTORY_Y - 11;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        for (int i = 0; i < RoasterBlockEntity.INPUTS; i++) {
            // Each arrow fills downward as its slot roasts.
            int h = Math.round(menu.progress(i) * ARROW_SIZE);
            if (h <= 0) continue;
            int x = RoasterMenu.SLOT_X + i * 18 + 4;
            g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos + x, topPos + ARROW_Y, ARROW_U, ARROW_V, ARROW_SIZE, h, 256, 256);
        }
        int gas = Math.round((float) menu.gas() * TANK_H / RoasterBlockEntity.CAPACITY);
        if (gas > 0) {
            g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos + TANK_X, topPos + TANK_Y + TANK_H - gas,
                    TANK_U, TANK_V + TANK_H - gas, TANK_W, gas, 256, 256);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        RoasterBlockEntity.Status status = menu.status();
        Component line = Component.translatable(status.key());
        boolean fine = status == RoasterBlockEntity.Status.ROASTING || status == RoasterBlockEntity.Status.EMPTY;
        g.text(font, line, imageWidth / 2 - font.width(line) / 2, STATUS_Y, fine ? HeatLine.FINE : HeatLine.SHORT, false);
        int need = RoasterBlockEntity.MIN_TEMPERATURE;
        if (status != RoasterBlockEntity.Status.EMPTY && status != RoasterBlockEntity.Status.NO_RECIPE) {
            Component heat = HeatLine.line("roaster", need, menu.temperature(), menu.heat(), menu.limit());
            g.text(font, heat, imageWidth / 2 - font.width(heat) / 2, STATUS_Y + 10, HeatLine.colour(need, menu.temperature()), false);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);
        int x = mouseX - leftPos, y = mouseY - topPos;
        if (x >= TANK_X && x < TANK_X + TANK_W && y >= TANK_Y && y < TANK_Y + TANK_H) {
            g.setTooltipForNextFrame(Component.translatable(StrataIndustria.MOD_ID + ".roaster.tank", menu.gas(), RoasterBlockEntity.CAPACITY),
                    mouseX, mouseY);
        } else if (y >= STATUS_Y + 9 && y < STATUS_Y + 19 && x >= 8 && x < imageWidth - 8) {
            g.setTooltipForNextFrame(HeatLine.tooltip(RoasterBlockEntity.MIN_TEMPERATURE, menu.temperature(), menu.limit()), mouseX, mouseY);
        }
    }
}
