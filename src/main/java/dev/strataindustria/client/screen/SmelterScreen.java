package dev.strataindustria.client.screen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.heat.HeatBand;
import dev.strataindustria.metal.CrucibleStatus;
import dev.strataindustria.metal.Melt;
import dev.strataindustria.metal.SmelterMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Smelter: the crucible's grid, melt bar and gauge on top; below them the mold line from stock to output
 * with the pour flowing along the first arrow; the Pour and Auto-pour buttons; then the status, the heat
 * coming in, and the alloy hint.
 */
public class SmelterScreen extends AbstractContainerScreen<SmelterMenu> {
    private static final Identifier BACKGROUND = StrataIndustria.id("textures/gui/smelter.png");
    public static final int ARROW_X = 45, ARROW_Y = SmelterMenu.ROW_Y + 4, ARROW_W = 12, ARROW_H = 8, ARROW_U = 176, ARROW_V = 0;
    public static final int COOLING_TEXT_X = 120;
    public static final int STATUS_Y = 116;
    private static final String KEY = StrataIndustria.MOD_ID + ".smelter.";

    private Button pour;
    private Button auto;

    public SmelterScreen(SmelterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, SmelterMenu.INVENTORY_Y + 82);
        this.inventoryLabelY = SmelterMenu.INVENTORY_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        pour = addRenderableWidget(Button.builder(Component.translatable(StrataIndustria.MOD_ID + ".crucible.pour"),
                b -> click(SmelterMenu.BUTTON_POUR)).bounds(leftPos + 8, topPos + SmelterMenu.BUTTON_Y, 40, 16).build());
        auto = addRenderableWidget(Button.builder(autoLabel(), b -> click(SmelterMenu.BUTTON_AUTO))
                .bounds(leftPos + 52, topPos + SmelterMenu.BUTTON_Y, 80, 16).build());
    }

    private void click(int button) {
        if (minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
    }

    private Component autoLabel() {
        return Component.translatable(KEY + (menu.autoPour() ? "auto_on" : "auto_off"));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        CrucibleStatus status = menu.status();
        // With auto-pour on there is nothing to press: it pours by itself.
        if (pour != null) {
            pour.active = !menu.autoPour() && (status == CrucibleStatus.MOLTEN || status == CrucibleStatus.CARBON_BURNED) && menu.pourPercent() == 0;
        }
        if (auto != null) auto.setMessage(autoLabel());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        CrucibleScreen.drawSlotProgress(g, leftPos + SmelterMenu.GRID_X, topPos + SmelterMenu.GRID_Y, menu::slotProgress);
        CrucibleScreen.drawMeltBar(g, leftPos + CrucibleScreen.BAR_X, topPos + CrucibleScreen.BAR_Y, menu.melt(), menu.capacity());
        CrucibleScreen.drawGauge(g, leftPos + CrucibleScreen.GAUGE_X, topPos + CrucibleScreen.GAUGE_Y, menu.temperature());
        // The pour runs along the arrow from the mold towards the cooling slot.
        int poured = menu.pourPercent();
        if (poured > 0) {
            int w = Math.max(1, Math.round(ARROW_W * poured / 100f));
            g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos + ARROW_X, topPos + ARROW_Y, ARROW_U, ARROW_V, w, ARROW_H, 256, 256);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        Melt melt = menu.melt();
        CrucibleScreen.drawMeltText(g, font, melt, menu.capacity(), CrucibleScreen.TEXT_X, CrucibleScreen.TEXT_Y);
        int cooling = menu.coolingTemperature();
        if (cooling > 0) {
            Component t = Component.translatable(KEY + "cooling", cooling);
            g.text(font, t, COOLING_TEXT_X, SmelterMenu.ROW_Y + 4, 0xFF000000 | darker(HeatBand.of(cooling).colour()), false);
        }
        g.text(font, CrucibleScreen.statusLine(menu.status(), melt, menu.meltingPercent(), menu.maxTemperature()), 8, STATUS_Y, HeatLine.FINE, false);
        int need = menu.need();
        if (need > 0) {
            Component heat = HeatLine.line("smelter", need, menu.heatTemperature(), menu.heat(), menu.limit());
            g.text(font, heat, 8, STATUS_Y + 10, HeatLine.colour(need, menu.heatTemperature()), false);
        }
        Component hint = CrucibleScreen.hintLine(melt);
        if (hint != null) g.text(font, hint, 8, STATUS_Y + 20, 0xFF7a4a20, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);
        int x = mouseX - leftPos, y = mouseY - topPos;
        int need = menu.need();
        if (need > 0 && y >= STATUS_Y + 9 && y < STATUS_Y + 19 && x >= 8 && x < imageWidth - 8) {
            g.setTooltipForNextFrame(HeatLine.tooltip(need, menu.heatTemperature(), menu.limit()), mouseX, mouseY);
        } else if (hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot.container != minecraft.player.getInventory()) {
            // Empty slots on the mold line say what they are for.
            String what = switch (hoveredSlot.getContainerSlot()) {
                case dev.strataindustria.metal.CrucibleBlockEntity.MOLD_SLOT -> "slot.mold";
                case dev.strataindustria.metal.SmelterBlockEntity.STOCK_SLOT -> "slot.stock";
                case dev.strataindustria.metal.SmelterBlockEntity.COOLING_SLOT -> "slot.cooling";
                case dev.strataindustria.metal.SmelterBlockEntity.OUTPUT_SLOT -> "slot.output";
                default -> null;
            };
            if (what != null) g.setTooltipForNextFrame(Component.translatable(KEY + what), mouseX, mouseY);
        }
    }

    private static int darker(int rgb) {
        int r = (rgb >> 16 & 255) * 3 / 5, gr = (rgb >> 8 & 255) * 3 / 5, b = (rgb & 255) * 3 / 5;
        return r << 16 | gr << 8 | b;
    }
}
