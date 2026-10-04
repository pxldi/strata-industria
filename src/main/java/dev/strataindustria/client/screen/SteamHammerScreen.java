package dev.strataindustria.client.screen;

import dev.strataindustria.client.HeatWords;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.steam.SteamHammerBlockEntity;
import dev.strataindustria.steam.SteamHammerMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Steam hammer: the shape button, then input, piece and result with the hits filling the arrow, the steam
 * buffer on the right, and lines for what it is doing, its heat and its steam.
 */
public class SteamHammerScreen extends AbstractContainerScreen<SteamHammerMenu> {
    private static final Identifier BACKGROUND = StrataIndustria.id("textures/gui/steam_hammer.png");
    public static final int ARROW_X = 93, ARROW_Y = 35, ARROW_W = 22, ARROW_H = 16, ARROW_U = 176, ARROW_V = 0;
    public static final int STEAM_X = 154, STEAM_Y = 18, STEAM_W = 12, STEAM_H = 44, STEAM_U = 176, STEAM_V = 17;
    public static final int STATUS_Y = 58;
    private static final String KEY = StrataIndustria.MOD_ID + ".steam_hammer.";
    private final ShapeButton shapeButton = new ShapeButton(SteamHammerMenu.SHAPE_X, SteamHammerMenu.SLOT_Y);

    public SteamHammerScreen(SteamHammerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, SteamHammerMenu.INVENTORY_Y + 82);
        this.inventoryLabelY = SteamHammerMenu.INVENTORY_Y - 11;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        shapeButton.extract(g, leftPos, topPos, menu.shape(), mouseX, mouseY);
        int total = menu.hitsTotal();
        if (total > 0 && menu.hitsDone() > 0) {
            int w = Math.max(1, Math.round((float) ARROW_W * Math.min(menu.hitsDone(), total) / total));
            g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos + ARROW_X, topPos + ARROW_Y, ARROW_U, ARROW_V, w, ARROW_H, 256, 256);
        }
        int steam = Math.round((float) menu.steam() * STEAM_H / SteamHammerBlockEntity.BUFFER);
        if (steam > 0) {
            g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos + STEAM_X, topPos + STEAM_Y + STEAM_H - steam,
                    STEAM_U, STEAM_V + STEAM_H - steam, STEAM_W, steam, 256, 256);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (shapeButton.click(event, menu.containerId, leftPos, topPos)) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        SteamHammerBlockEntity.Status status = menu.status();
        Component line = switch (status) {
            case WORKING -> Component.translatable(status.key(), menu.hitsDone(), menu.hitsTotal());
            case HEATING -> Component.translatable(status.key(), HeatWords.of(menu.pieceTemperature()), HeatWords.of(menu.working()));
            default -> Component.translatable(status.key());
        };
        g.text(font, line, 8, STATUS_Y, status.fine() ? HeatLine.FINE : HeatLine.SHORT, false);
        int need = menu.working();
        if (need > 0) {
            Component heat = HeatLine.line("steam_hammer", need, menu.heatTemperature(), menu.heat(), menu.limit());
            g.text(font, heat, 8, STATUS_Y + 10, HeatLine.colour(need, menu.heatTemperature()), false);
        }
        g.text(font, steamLine(), 8, STATUS_Y + 20,
                menu.pressure() >= SteamHammerBlockEntity.MIN_PRESSURE ? HeatLine.FINE : HeatLine.SHORT, false);
    }

    private Component steamLine() {
        float pressure = menu.pressure();
        String bar = String.format(java.util.Locale.ROOT, "%.1f", pressure);
        if (pressure < SteamHammerBlockEntity.MIN_PRESSURE) return Component.translatable(KEY + "steam.none");
        if (pressure < SteamHammerBlockEntity.FULL_PRESSURE) return Component.translatable(KEY + "steam.slow", bar);
        return Component.translatable(KEY + "steam.full", bar);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);
        int x = mouseX - leftPos, y = mouseY - topPos;
        int need = menu.working();
        if (shapeButton.over(mouseX, mouseY, leftPos, topPos)) {
            shapeButton.tooltip(g, font, menu.shape(), mouseX, mouseY);
        } else if (x >= STEAM_X && x < STEAM_X + STEAM_W && y >= STEAM_Y && y < STEAM_Y + STEAM_H) {
            g.setTooltipForNextFrame(Component.translatable(KEY + "buffer", menu.steam(), SteamHammerBlockEntity.BUFFER), mouseX, mouseY);
        } else if (need > 0 && y >= STATUS_Y + 9 && y < STATUS_Y + 19 && x >= 8 && x < imageWidth - 8) {
            g.setTooltipForNextFrame(HeatLine.tooltip(need, menu.heatTemperature(), menu.limit()), mouseX, mouseY);
        } else if (hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot.container != minecraft.player.getInventory()) {
            // Empty slots say what they are for.
            int slot = hoveredSlot.getContainerSlot();
            String what = slot == SteamHammerBlockEntity.QUEUE ? "slot.input"
                    : slot == dev.strataindustria.smithing.AnvilBlockEntity.INPUT ? "slot.piece"
                    : slot == SteamHammerBlockEntity.RESULT ? "slot.result" : null;
            if (what != null) g.setTooltipForNextFrame(Component.translatable(KEY + what), mouseX, mouseY);
        }
    }
}
