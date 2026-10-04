package dev.strataindustria.client.screen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.machine.TripHammerBlockEntity;
import dev.strataindustria.machine.TripHammerMenu;
import dev.strataindustria.power.Kinetic;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Trip hammer: pattern and workpiece slots, then what it is doing and its network line. */
public class TripHammerScreen extends AbstractContainerScreen<TripHammerMenu> {
    private static final Identifier BACKGROUND = StrataIndustria.id("textures/gui/trip_hammer.png");
    public static final int STATUS_Y = 62;

    public TripHammerScreen(TripHammerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        this.inventoryLabelY = TripHammerMenu.INVENTORY_Y - 11;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        TripHammerBlockEntity.Status status = menu.status();
        Component line = status == TripHammerBlockEntity.Status.WORKING
                ? Component.translatable(status.key(), menu.hitsDone(), menu.hitsTotal())
                : status == TripHammerBlockEntity.Status.TOO_SLOW
                ? Component.translatable(status.key(), TripHammerBlockEntity.MIN_SPEED)
                : Component.translatable(status.key());
        int colour = status == TripHammerBlockEntity.Status.WORKING || status == TripHammerBlockEntity.Status.WAITING ? 0xFF404040 : 0xFF7A4A20;
        g.text(font, line, 8, STATUS_Y, colour, false);
        if (minecraft.level != null && minecraft.level.getBlockEntity(menu.pos()) instanceof Kinetic kinetic) {
            Component network = kinetic.kinetic().line();
            g.text(font, network, imageWidth - 8 - font.width(network), titleLabelY, 0xFF404040, false);
        }
    }
}
