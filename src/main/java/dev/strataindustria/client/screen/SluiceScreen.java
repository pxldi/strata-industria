package dev.strataindustria.client.screen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.washing.SluiceBlockEntity;
import dev.strataindustria.washing.SluiceMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Sluice: the items waiting to be washed and how the washing goes. */
public class SluiceScreen extends AbstractContainerScreen<SluiceMenu> {
    private static final Identifier BACKGROUND = StrataIndustria.id("textures/gui/sluice.png");
    public static final int STATUS_Y = 62;

    public SluiceScreen(SluiceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        this.inventoryLabelY = SluiceMenu.INVENTORY_Y - 11;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        SluiceBlockEntity.Status status = menu.status();
        Component line = status == SluiceBlockEntity.Status.WORKING
                ? Component.translatable(status.key(), Math.round(menu.progress() * 100))
                : Component.translatable(status.key());
        int colour = status == SluiceBlockEntity.Status.NO_WATER ? 0xFF7A4A20 : 0xFF404040;
        g.text(font, line, (imageWidth - font.width(line)) / 2, STATUS_Y, colour, false);
    }
}
