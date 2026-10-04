package dev.strataindustria.client.screen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.ceramics.BrickKilnBlockEntity;
import dev.strataindustria.ceramics.BrickKilnMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Brick kiln: pieces to fire on the left, the flame rising as the batch fires, fired pieces on the right. */
public class BrickKilnScreen extends AbstractContainerScreen<BrickKilnMenu> {
    private static final Identifier BACKGROUND = StrataIndustria.id("textures/gui/brick_kiln.png");
    public static final int FLAME_X = 81, FLAME_Y = 29, FLAME_SIZE = 14, FLAME_U = 176, FLAME_V = 0;
    public static final int STATUS_Y = 56;

    public BrickKilnScreen(BrickKilnMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        this.inventoryLabelY = BrickKilnMenu.INVENTORY_Y - 11;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        if (menu.status() == BrickKilnBlockEntity.Status.FIRING) {
            int h = Math.max(1, Math.round(menu.progress() * FLAME_SIZE));
            g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos + FLAME_X, topPos + FLAME_Y + FLAME_SIZE - h,
                    FLAME_U, FLAME_V + FLAME_SIZE - h, FLAME_SIZE, h, 256, 256);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        BrickKilnBlockEntity.Status status = menu.status();
        Component line = status == BrickKilnBlockEntity.Status.FIRING
                ? Component.translatable(status.key(), Math.round(menu.progress() * 100))
                : Component.translatable(status.key());
        boolean fine = status == BrickKilnBlockEntity.Status.FIRING || status == BrickKilnBlockEntity.Status.EMPTY;
        g.text(font, line, imageWidth / 2 - font.width(line) / 2, STATUS_Y, fine ? HeatLine.FINE : HeatLine.SHORT, false);
    }
}
