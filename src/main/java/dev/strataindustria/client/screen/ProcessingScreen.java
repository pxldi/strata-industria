package dev.strataindustria.client.screen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.power.Kinetic;
import dev.strataindustria.processing.MachineLayout;
import dev.strataindustria.processing.ProcessingBlockEntity;
import dev.strataindustria.processing.ProcessingMenu;
import dev.strataindustria.processing.WasherBlockEntity;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** An ore processing machine: inputs, an arrow for each, the output grid, the status and the network line. */
public class ProcessingScreen extends AbstractContainerScreen<ProcessingMenu> {
    private static final Identifier ARROW = Identifier.withDefaultNamespace("container/furnace/burn_progress");
    /** The washer's water gauge inside its well. */
    public static final int TANK_X = 9, TANK_Y = 19, TANK_W = 10, TANK_H = 50;
    private final Identifier background;

    public ProcessingScreen(ProcessingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, MachineLayout.WIDTH, MachineLayout.HEIGHT);
        this.inventoryLabelY = MachineLayout.INVENTORY_Y - 11;
        this.background = StrataIndustria.id("textures/gui/" + menu.layout().id() + ".png");
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.blit(RenderPipelines.GUI_TEXTURED, background, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        MachineLayout layout = menu.layout();
        for (int i = 0; i < layout.inputs(); i++) {
            int w = Math.round(menu.progress(i) * 24);
            if (w > 0) {
                g.blitSprite(RenderPipelines.GUI_TEXTURED, ARROW, 24, 16, 0, 0, leftPos + MachineLayout.ARROW_X,
                        topPos + layout.inputY(i), w, 16);
            }
        }
        if (layout == MachineLayout.WASHER) {
            int h = Math.round(TANK_H * Math.min(1.0f, menu.extra(0) / (float) WasherBlockEntity.TANK));
            if (h > 0) {
                int x = leftPos + TANK_X, bottom = topPos + TANK_Y + TANK_H;
                g.fill(x, bottom - h, x + TANK_W, bottom, 0xFF3466CC);
                g.fill(x, bottom - h, x + 2, bottom, 0xFF5A8AEC);
                g.fill(x, bottom - h, x + TANK_W, bottom - h + 1, 0xFF7AA4F0);
            }
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);
        int x = mouseX - leftPos, y = mouseY - topPos;
        if (menu.layout() == MachineLayout.WASHER && x >= TANK_X && x < TANK_X + TANK_W && y >= TANK_Y && y < TANK_Y + TANK_H) {
            g.setTooltipForNextFrame(Component.translatable(StrataIndustria.MOD_ID + ".washer.water", menu.extra(0), WasherBlockEntity.TANK),
                    mouseX, mouseY);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        ProcessingBlockEntity.Status status = menu.status();
        Component line = status == ProcessingBlockEntity.Status.TOO_SLOW && minecraft.level != null
                && minecraft.level.getBlockEntity(menu.pos()) instanceof ProcessingBlockEntity machine
                ? Component.translatable(status.key(), machine.minSpeed())
                : Component.translatable(status.key());
        int colour = status == ProcessingBlockEntity.Status.WORKING || status == ProcessingBlockEntity.Status.EMPTY ? 0xFF404040 : 0xFF7A4A20;
        g.text(font, line, 8, MachineLayout.STATUS_Y, colour, false);
        if (minecraft.level != null && minecraft.level.getBlockEntity(menu.pos()) instanceof Kinetic kinetic) {
            Component network = kinetic.kinetic().line();
            g.text(font, network, imageWidth - 8 - font.width(network), titleLabelY, 0xFF404040, false);
        }
    }
}
