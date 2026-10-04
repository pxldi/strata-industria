package dev.strataindustria.client.screen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.ceramics.SmallVesselMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** The small vessel: four slots in a row above the inventory. */
public class SmallVesselScreen extends AbstractContainerScreen<SmallVesselMenu> {
    private static final Identifier BACKGROUND = StrataIndustria.id("textures/gui/small_vessel.png");

    public SmallVesselScreen(SmallVesselMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 133);
        this.inventoryLabelY = SmallVesselMenu.INVENTORY_Y - 11;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
    }
}
