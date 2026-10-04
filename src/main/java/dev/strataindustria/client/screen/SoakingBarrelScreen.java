package dev.strataindustria.client.screen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.tanning.SoakingBarrelBlockEntity;
import dev.strataindustria.tanning.SoakingBarrelMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Soaking barrel: what soaks, the tank, and how long it has to go. */
public class SoakingBarrelScreen extends AbstractContainerScreen<SoakingBarrelMenu> {
    private static final Identifier BACKGROUND = StrataIndustria.id("textures/gui/soaking_barrel.png");
    /** The tank's inside, and where each fluid's fill strip sits in the texture (water, lye, tannin, other). */
    public static final int TANK_X = 12, TANK_Y = 17, TANK_W = 16, TANK_H = 52, FLUID_U = 176, FLUID_V = 0;
    public static final int ARROW_X = 76, ARROW_Y = 26, ARROW_W = 24, ARROW_H = 17, ARROW_U = 176, ARROW_V = 56;
    public static final int STATUS_Y = 56;

    public SoakingBarrelScreen(SoakingBarrelMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        this.inventoryLabelY = SoakingBarrelMenu.INVENTORY_Y - 11;
        this.titleLabelX = 34;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        SoakingBarrelBlockEntity.TankFluid fluid = menu.fluid();
        int amount = menu.amount();
        if (fluid != SoakingBarrelBlockEntity.TankFluid.NONE && amount > 0) {
            int h = Math.max(1, Math.round(TANK_H * Math.min(1.0f, amount / (float) SoakingBarrelBlockEntity.CAPACITY)));
            // Five strips fit along the top of the sheet; later fluids continue on a second row.
            int strip = fluid.ordinal() - 1;
            int u = FLUID_U + (strip % 5) * TANK_W;
            int v = FLUID_V + (strip / 5) * 76;
            g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos + TANK_X, topPos + TANK_Y + TANK_H - h,
                    u, v + TANK_H - h, TANK_W, h, 256, 256);
        }
        if (menu.status() == SoakingBarrelBlockEntity.Status.WORKING) {
            int w = Math.round(ARROW_W * menu.progress());
            if (w > 0) g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos + ARROW_X, topPos + ARROW_Y, ARROW_U, ARROW_V, w, ARROW_H, 256, 256);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        SoakingBarrelBlockEntity.Status status = menu.status();
        Component line = switch (status) {
            case WORKING -> Component.translatable(status.key(), Math.round(menu.progress() * 100));
            case NEEDS_MORE -> Component.translatable(status.key(), menu.itemsShort());
            default -> Component.translatable(status.key());
        };
        int colour = status == SoakingBarrelBlockEntity.Status.WORKING ? 0xFF404040 : 0xFF7A4A20;
        int centre = (TANK_X + TANK_W + imageWidth) / 2;
        g.text(font, line, centre - font.width(line) / 2, STATUS_Y, colour, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);
        int x = mouseX - leftPos, y = mouseY - topPos;
        if (x >= TANK_X && x < TANK_X + TANK_W && y >= TANK_Y && y < TANK_Y + TANK_H) {
            SoakingBarrelBlockEntity.TankFluid fluid = menu.fluid();
            Component text = fluid == SoakingBarrelBlockEntity.TankFluid.NONE || menu.amount() <= 0
                    ? Component.translatable(StrataIndustria.MOD_ID + ".soaking_barrel.tank_empty")
                    : Component.translatable(StrataIndustria.MOD_ID + ".soaking_barrel.tank",
                            Component.translatable(fluid.key()), menu.amount(), SoakingBarrelBlockEntity.CAPACITY);
            g.setTooltipForNextFrame(text, mouseX, mouseY);
        }
    }
}
