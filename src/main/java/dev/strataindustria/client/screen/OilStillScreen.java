package dev.strataindustria.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.oil.OilStillBlockEntity;
import dev.strataindustria.oil.OilStillMenu;
import dev.strataindustria.registry.Tier6Fluids;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Oil still: the crude tank on the left, an arrow that fills as the batch cooks, and the naphtha, diesel and
 * heavy oil tanks on the right. Clicking a product tank fills a bucket from your pack.
 */
public class OilStillScreen extends AbstractContainerScreen<OilStillMenu> {
    private static final Identifier BACKGROUND = StrataIndustria.id("textures/gui/oil_still.png");
    public static final int TANK_Y = 17, TANK_W = 16, TANK_H = 46, CRUDE_X = 26;
    /** Left edge of each product tank. */
    public static final int[] PRODUCT_X = {88, 112, 136};
    public static final int ARROW_X = 52, ARROW_Y = 32, ARROW_W = 24, ARROW_H = 16, ARROW_U = 176, ARROW_V = 0;
    /** Where the gauge fills sit on the texture: crude, naphtha, diesel, heavy oil, anything else. */
    private static final int FILL_V = 16, FILL_U = 176, FILL_STEP = 16;
    public static final int STATUS_Y = 68;

    public OilStillScreen(OilStillMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, OilStillMenu.INVENTORY_Y + 82);
        this.inventoryLabelY = OilStillMenu.INVENTORY_Y - 11;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        int w = Math.round(menu.progress() * ARROW_W);
        if (w > 0) g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos + ARROW_X, topPos + ARROW_Y, ARROW_U, ARROW_V, w, ARROW_H, 256, 256);
        gauge(g, 0, CRUDE_X);
        for (int i = 0; i < OilStillBlockEntity.PRODUCTS; i++) gauge(g, 1 + i, PRODUCT_X[i]);
    }

    private void gauge(GuiGraphicsExtractor g, int tank, int x) {
        int h = Math.round((float) menu.amount(tank) * TANK_H / OilStillBlockEntity.CAPACITY);
        if (h <= 0) return;
        int u = FILL_U + FILL_STEP * fillIndex(menu.fluid(tank));
        g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos + x, topPos + TANK_Y + TANK_H - h, u, FILL_V + TANK_H - h, TANK_W, h, 256, 256);
    }

    private static int fillIndex(Fluid fluid) {
        if (fluid.isSame(Tier6Fluids.CRUDE_OIL.source().get())) return 0;
        if (fluid.isSame(Tier6Fluids.NAPHTHA.source().get())) return 1;
        if (fluid.isSame(Tier6Fluids.DIESEL.source().get())) return 2;
        if (fluid.isSame(Tier6Fluids.HEAVY_OIL.source().get())) return 3;
        return 4;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        OilStillBlockEntity.Status status = menu.status();
        Component line = Component.translatable(status.key());
        boolean fine = status == OilStillBlockEntity.Status.DISTILLING || status == OilStillBlockEntity.Status.EMPTY;
        g.text(font, line, imageWidth / 2 - font.width(line) / 2, STATUS_Y, fine ? HeatLine.FINE : HeatLine.SHORT, false);
        if (status != OilStillBlockEntity.Status.EMPTY && status != OilStillBlockEntity.Status.NO_RECIPE) {
            int need = OilStillBlockEntity.MIN_TEMPERATURE;
            Component heat = HeatLine.line("oil_still", need, menu.temperature(), menu.heat(), menu.limit());
            g.text(font, heat, imageWidth / 2 - font.width(heat) / 2, STATUS_Y + 10, HeatLine.colour(need, menu.temperature()), false);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int product = productAt(event.x(), event.y());
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && product >= 0 && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, product);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    /** The product tank (0 based) under the pointer, or -1. */
    private int productAt(double mouseX, double mouseY) {
        double x = mouseX - leftPos, y = mouseY - topPos;
        if (y < TANK_Y || y >= TANK_Y + TANK_H) return -1;
        for (int i = 0; i < PRODUCT_X.length; i++) if (x >= PRODUCT_X[i] && x < PRODUCT_X[i] + TANK_W) return i;
        return -1;
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);
        int x = mouseX - leftPos, y = mouseY - topPos;
        String key = StrataIndustria.MOD_ID + ".oil_still.";
        if (y >= TANK_Y && y < TANK_Y + TANK_H && x >= CRUDE_X && x < CRUDE_X + TANK_W) {
            g.setTooltipForNextFrame(Component.translatable(key + "crude", menu.amount(0), OilStillBlockEntity.CAPACITY), mouseX, mouseY);
            return;
        }
        int product = productAt(mouseX, mouseY);
        if (product >= 0) {
            int tank = 1 + product;
            Fluid fluid = menu.fluid(tank);
            Component name = fluid.isSame(Fluids.EMPTY) ? Component.translatable(key + "empty_tank") : fluid.getFluidType().getDescription();
            g.setTooltipForNextFrame(Component.translatable(key + "product", name, menu.amount(tank), OilStillBlockEntity.CAPACITY)
                    .append("\n").append(Component.translatable(key + "take").withStyle(net.minecraft.ChatFormatting.GRAY)), mouseX, mouseY);
        } else if (y >= STATUS_Y + 9 && y < STATUS_Y + 19 && x >= 8 && x < imageWidth - 8) {
            g.setTooltipForNextFrame(HeatLine.tooltip(OilStillBlockEntity.MIN_TEMPERATURE, menu.temperature(), menu.limit()), mouseX, mouseY);
        }
    }
}
