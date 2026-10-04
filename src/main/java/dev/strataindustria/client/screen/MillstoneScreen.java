package dev.strataindustria.client.screen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.machine.MillstoneBlockEntity;
import dev.strataindustria.machine.MillstoneMenu;
import dev.strataindustria.power.Kinetic;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Millstone: input, a progress arrow, output, then the machine's status and its network line. */
public class MillstoneScreen extends AbstractContainerScreen<MillstoneMenu> {
    private static final Identifier BACKGROUND = StrataIndustria.id("textures/gui/millstone.png");
    private static final Identifier ARROW = Identifier.withDefaultNamespace("container/furnace/burn_progress");
    public static final int ARROW_X = 79, ARROW_Y = 34, STATUS_Y = 62;

    public MillstoneScreen(MillstoneMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        this.inventoryLabelY = MillstoneMenu.INVENTORY_Y - 11;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        int w = Math.round(menu.progress() * 24);
        if (w > 0) g.blitSprite(RenderPipelines.GUI_TEXTURED, ARROW, 24, 16, 0, 0, leftPos + ARROW_X, topPos + ARROW_Y, w, 16);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        MillstoneBlockEntity.Status status = menu.status();
        Component line = status == MillstoneBlockEntity.Status.TOO_SLOW
                ? Component.translatable(status.key(), MillstoneBlockEntity.MIN_SPEED)
                : Component.translatable(status.key());
        int colour = status == MillstoneBlockEntity.Status.WORKING || status == MillstoneBlockEntity.Status.EMPTY ? 0xFF404040 : 0xFF7A4A20;
        g.text(font, line, 8, STATUS_Y, colour, false);
        if (minecraft.level != null && minecraft.level.getBlockEntity(menu.pos()) instanceof Kinetic kinetic) {
            Component network = kinetic.kinetic().line();
            g.text(font, network, imageWidth - 8 - font.width(network), STATUS_Y, 0xFF404040, false);
        }
    }
}
