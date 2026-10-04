package dev.strataindustria.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.electric.PowerHammerBlockEntity;
import dev.strataindustria.electric.PowerHammerMenu;
import dev.strataindustria.power.StatusLight;
import dev.strataindustria.smithing.AnvilBlockEntity;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Power hammer: the pattern and flux, the input and the second piece, the piece on the anvil and the
 * result with the hits filling the arrow, the power bar on the right, and lines for what it is doing and
 * how hot the piece is.
 */
public class PowerHammerScreen extends AbstractContainerScreen<PowerHammerMenu> {
    private static final Identifier BACKGROUND = StrataIndustria.id("textures/gui/power_hammer.png");
    private static final Identifier BAR_RUN = sprite("power_bar_run"), BAR_LOW = sprite("power_bar_low"), BAR_STOPPED = sprite("power_bar_stopped");
    private static final Identifier EJECT_OFF = sprite("eject_off"), EJECT_ON = sprite("eject_on"), EJECT_HIGHLIGHTED = sprite("eject_highlighted");
    public static final int ARROW_X = 93, ARROW_Y = 35, ARROW_W = 22, ARROW_H = 16, ARROW_U = 176, ARROW_V = 0;
    public static final int BAR_X = 154, BAR_Y = 18, BAR_W = 8, BAR_H = 44, BAR_SPRITE_H = 52;
    public static final int EJECT_X = 154, EJECT_Y = 68, EJECT_SIZE = 14;
    public static final int STATUS_Y = 66;
    private static final String KEY = StrataIndustria.MOD_ID + ".power_hammer.";

    public PowerHammerScreen(PowerHammerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, PowerHammerMenu.INVENTORY_Y + 82);
        this.inventoryLabelY = PowerHammerMenu.INVENTORY_Y - 11;
    }

    private static Identifier sprite(String name) {
        return StrataIndustria.id("container/electric_machine/" + name);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        int total = menu.hitsTotal();
        if (total > 0 && menu.hitsDone() > 0) {
            int w = Math.max(1, Math.round((float) ARROW_W * Math.min(menu.hitsDone(), total) / total));
            g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos + ARROW_X, topPos + ARROW_Y, ARROW_U, ARROW_V, w, ARROW_H, 256, 256);
        }
        powerBar(g);
        g.blitSprite(RenderPipelines.GUI_TEXTURED, menu.autoEject() ? EJECT_ON : EJECT_OFF, leftPos + EJECT_X, topPos + EJECT_Y, EJECT_SIZE, EJECT_SIZE);
        if (overEject(mouseX, mouseY)) {
            g.blitSprite(RenderPipelines.GUI_TEXTURED, EJECT_HIGHLIGHTED, leftPos + EJECT_X, topPos + EJECT_Y, EJECT_SIZE, EJECT_SIZE);
        }
    }

    private void powerBar(GuiGraphicsExtractor g) {
        PowerHammerBlockEntity.Status status = menu.status();
        boolean stopped = status == PowerHammerBlockEntity.Status.NO_POWER
                || status.light() == StatusLight.ERROR && status != PowerHammerBlockEntity.Status.OUTPUT_FULL;
        int h = stopped ? BAR_H : Math.round(menu.buffer() * BAR_H);
        if (h <= 0) return;
        Identifier bar = stopped ? BAR_STOPPED : status == PowerHammerBlockEntity.Status.LOW_POWER ? BAR_LOW : BAR_RUN;
        g.blitSprite(RenderPipelines.GUI_TEXTURED, bar, BAR_W, BAR_SPRITE_H, 0, BAR_SPRITE_H - h, leftPos + BAR_X, topPos + BAR_Y + BAR_H - h, BAR_W, h);
    }

    private boolean overEject(double mouseX, double mouseY) {
        double x = mouseX - leftPos - EJECT_X, y = mouseY - topPos - EJECT_Y;
        return x >= 0 && x < EJECT_SIZE && y >= 0 && y < EJECT_SIZE;
    }

    private boolean overBar(int mouseX, int mouseY) {
        int x = mouseX - leftPos - BAR_X, y = mouseY - topPos - BAR_Y;
        return x >= 0 && x < BAR_W && y >= 0 && y < BAR_H;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && overEject(event.x(), event.y()) && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, PowerHammerMenu.BUTTON_EJECT);
            minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0f));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        PowerHammerBlockEntity.Status status = menu.status();
        Component line = switch (status) {
            case WORKING -> Component.translatable(status.key(), menu.hitsDone(), menu.hitsTotal());
            case HEATING -> Component.translatable(status.key(), menu.pieceTemperature(), menu.needed());
            case LOW_POWER -> Component.translatable(status.key(), menu.powerPercent());
            default -> Component.translatable(status.key());
        };
        int colour = status.light() == StatusLight.ERROR ? HeatLine.SHORT : status.light() == StatusLight.WAIT ? 0xFF7A4A20 : HeatLine.FINE;
        g.text(font, line, 8, STATUS_Y, colour, false);
        if (menu.pieceTemperature() > 0 || menu.needed() > 0) {
            g.text(font, Component.translatable(KEY + "temperature", menu.pieceTemperature(), menu.needed()), 8, STATUS_Y + 10,
                    menu.pieceTemperature() >= menu.needed() ? HeatLine.FINE : HeatLine.SHORT, false);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);
        if (overBar(mouseX, mouseY)) {
            g.setTooltipForNextFrame(Component.translatable(StrataIndustria.MOD_ID + ".electric_machine.buffer", Math.round(menu.buffer() * 100)),
                    mouseX, mouseY);
        } else if (overEject(mouseX, mouseY)) {
            g.setTooltipForNextFrame(Component.translatable(StrataIndustria.MOD_ID + ".electric_machine." + (menu.autoEject() ? "eject_on" : "eject_off")),
                    mouseX, mouseY);
        } else if (hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot.container != minecraft.player.getInventory()) {
            // Empty slots say what they are for.
            int slot = hoveredSlot.getContainerSlot();
            String what = slot == AnvilBlockEntity.PATTERN ? "slot.pattern"
                    : slot == AnvilBlockEntity.FLUX ? "slot.flux"
                    : slot == PowerHammerBlockEntity.QUEUE ? "slot.input"
                    : slot == AnvilBlockEntity.SECOND ? "slot.second"
                    : slot == AnvilBlockEntity.INPUT ? "slot.piece"
                    : slot == PowerHammerBlockEntity.RESULT ? "slot.result" : null;
            if (what != null) g.setTooltipForNextFrame(Component.translatable(KEY + what), mouseX, mouseY);
        }
    }
}
