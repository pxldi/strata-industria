package dev.strataindustria.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.mark.MakersMark;
import dev.strataindustria.mark.MarkMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** The mark editor: click cells to cut or fill them, and a life-size preview beside the grid. */
public class MarkScreen extends AbstractContainerScreen<MarkMenu> {
    private static final Identifier BACKGROUND = StrataIndustria.id("textures/gui/makers_mark.png");
    private static final int GRID_X = 17, GRID_Y = 22, CELL = 10;
    private static final int PREVIEW_X = 112, PREVIEW_Y = 28, PREVIEW_CELL = 4;
    private static final int INK = 0xFF2A2630, INK_LIGHT = 0xFF4A4452;

    public MarkScreen(MarkMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 112);
        this.inventoryLabelY = 1000;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable(StrataIndustria.MOD_ID + ".mark.stamp"), b -> click(MarkMenu.CONFIRM))
                .bounds(leftPos + 106, topPos + 68, 58, 16).build());
        addRenderableWidget(Button.builder(Component.translatable(StrataIndustria.MOD_ID + ".mark.clear"), b -> click(MarkMenu.CLEAR))
                .bounds(leftPos + 106, topPos + 88, 58, 16).build());
    }

    private void click(int id) {
        if (minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        int hovered = hoveredCell(mouseX, mouseY);
        for (int cell = 0; cell < MakersMark.CELLS; cell++) {
            int x = leftPos + GRID_X + cell % MakersMark.SIZE * CELL, y = topPos + GRID_Y + cell / MakersMark.SIZE * CELL;
            if (menu.isOn(cell)) {
                g.fill(x, y, x + CELL - 1, y + CELL - 1, INK);
                g.fill(x, y, x + CELL - 1, y + 1, INK_LIGHT);
                g.fill(x, y, x + 1, y + CELL - 1, INK_LIGHT);
            } else if (cell == hovered) {
                g.fill(x, y, x + CELL - 1, y + CELL - 1, 0x40FFFFFF);
            }
            int px = leftPos + PREVIEW_X + cell % MakersMark.SIZE * PREVIEW_CELL, py = topPos + PREVIEW_Y + cell / MakersMark.SIZE * PREVIEW_CELL;
            if (menu.isOn(cell)) g.fill(px, py, px + PREVIEW_CELL, py + PREVIEW_CELL, INK);
        }
    }

    private int hoveredCell(double mouseX, double mouseY) {
        int gx = (int) Math.floor((mouseX - leftPos - GRID_X) / CELL);
        int gy = (int) Math.floor((mouseY - topPos - GRID_Y) / CELL);
        if (gx < 0 || gy < 0 || gx >= MakersMark.SIZE || gy >= MakersMark.SIZE) return -1;
        return gy * MakersMark.SIZE + gx;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int cell = hoveredCell(event.x(), event.y());
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && cell >= 0) {
            click(cell);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
}
