package dev.strataindustria.client.screen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.electric.machine.ElectricMachineBlockEntity;
import dev.strataindustria.electric.machine.ElectricMachineLayout;
import dev.strataindustria.electric.machine.ElectricMachineMenu;
import dev.strataindustria.power.ElectricTier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * An electric item machine (spec 10.1 and 6.5): a lane per operation, the power bar (green when fully
 * supplied, amber on low power, red when stopped) showing the buffer, the tier badge, the status line
 * and the auto-eject toggle.
 */
public class ElectricMachineScreen extends AbstractContainerScreen<ElectricMachineMenu> {
    private static final Identifier ARROW = Identifier.withDefaultNamespace("container/furnace/burn_progress");
    private static final Identifier BAR_RUN = sprite("power_bar_run"), BAR_LOW = sprite("power_bar_low"), BAR_STOPPED = sprite("power_bar_stopped");
    private static final Identifier EJECT_OFF = sprite("eject_off"), EJECT_ON = sprite("eject_on"), EJECT_HIGHLIGHTED = sprite("eject_highlighted");
    private static final Identifier MODE_ROD = sprite("lathe_rod"), MODE_GEAR = sprite("lathe_gear");
    public static final int BADGE_W = 15, BADGE_H = 9;
    private final Identifier background;

    public ElectricMachineScreen(ElectricMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, ElectricMachineLayout.WIDTH, ElectricMachineLayout.HEIGHT);
        this.inventoryLabelY = ElectricMachineLayout.INVENTORY_Y - 11;
        this.background = StrataIndustria.id("textures/gui/" + menu.layout().texture(menu.tier()) + ".png");
    }

    private static Identifier sprite(String name) {
        return StrataIndustria.id("container/electric_machine/" + name);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.blit(RenderPipelines.GUI_TEXTURED, background, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        for (int lane = 0; lane < menu.lanes(); lane++) {
            int w = Math.round(menu.progress(lane) * 24);
            if (w > 0) {
                g.blitSprite(RenderPipelines.GUI_TEXTURED, ARROW, 24, 16, 0, 0, leftPos + ElectricMachineLayout.ARROW_X,
                        topPos + ElectricMachineLayout.laneY(menu.lanes(), lane), w, 16);
            }
        }
        powerBar(g);
        int ex = leftPos + ElectricMachineLayout.EJECT_X, ey = topPos + ElectricMachineLayout.EJECT_Y;
        int size = ElectricMachineLayout.EJECT_SIZE;
        g.blitSprite(RenderPipelines.GUI_TEXTURED, menu.autoEject() ? EJECT_ON : EJECT_OFF, ex, ey, size, size);
        if (overEject(mouseX, mouseY)) g.blitSprite(RenderPipelines.GUI_TEXTURED, EJECT_HIGHLIGHTED, ex, ey, size, size);
        if (menu.layout().hasMode()) {
            int mx = leftPos + ElectricMachineLayout.MODE_X, my = topPos + ElectricMachineLayout.MODE_Y;
            g.blitSprite(RenderPipelines.GUI_TEXTURED, menu.mode() == 0 ? MODE_ROD : MODE_GEAR, mx, my, size, size);
            if (overMode(mouseX, mouseY)) g.blitSprite(RenderPipelines.GUI_TEXTURED, EJECT_HIGHLIGHTED, mx, my, size, size);
        }
    }

    /** Spec 6.5: the bar's height is the buffer, its colour the supply. */
    private void powerBar(GuiGraphicsExtractor g) {
        ElectricMachineBlockEntity.Status status = menu.status();
        boolean stopped = status == ElectricMachineBlockEntity.Status.NO_POWER || status.light() == dev.strataindustria.power.StatusLight.ERROR
                && status != ElectricMachineBlockEntity.Status.OUTPUT_FULL;
        int h = stopped ? ElectricMachineLayout.BAR_H : Math.round(menu.buffer() * ElectricMachineLayout.BAR_H);
        if (h <= 0) return;
        Identifier bar = stopped ? BAR_STOPPED : status == ElectricMachineBlockEntity.Status.LOW_POWER ? BAR_LOW : BAR_RUN;
        int x = leftPos + ElectricMachineLayout.BAR_X, bottom = topPos + ElectricMachineLayout.BAR_Y + ElectricMachineLayout.BAR_H;
        g.blitSprite(RenderPipelines.GUI_TEXTURED, bar, ElectricMachineLayout.BAR_W, ElectricMachineLayout.BAR_H, 0, ElectricMachineLayout.BAR_H - h,
                x, bottom - h, ElectricMachineLayout.BAR_W, h);
    }

    private boolean overEject(double mouseX, double mouseY) {
        double x = mouseX - leftPos - ElectricMachineLayout.EJECT_X, y = mouseY - topPos - ElectricMachineLayout.EJECT_Y;
        return x >= 0 && x < ElectricMachineLayout.EJECT_SIZE && y >= 0 && y < ElectricMachineLayout.EJECT_SIZE;
    }

    private boolean overMode(double mouseX, double mouseY) {
        if (!menu.layout().hasMode()) return false;
        double x = mouseX - leftPos - ElectricMachineLayout.MODE_X, y = mouseY - topPos - ElectricMachineLayout.MODE_Y;
        return x >= 0 && x < ElectricMachineLayout.EJECT_SIZE && y >= 0 && y < ElectricMachineLayout.EJECT_SIZE;
    }

    private boolean overBar(int mouseX, int mouseY) {
        int x = mouseX - leftPos - ElectricMachineLayout.BAR_X, y = mouseY - topPos - ElectricMachineLayout.BAR_Y;
        return x >= 0 && x < ElectricMachineLayout.BAR_W && y >= 0 && y < ElectricMachineLayout.BAR_H;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && overMode(event.x(), event.y()) && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ElectricMachineMenu.BUTTON_MODE);
            minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0f));
            return true;
        }
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && overEject(event.x(), event.y()) && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ElectricMachineMenu.BUTTON_EJECT);
            minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0f));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);
        String prefix = StrataIndustria.MOD_ID + ".electric_machine.";
        if (overBar(mouseX, mouseY)) {
            g.setTooltipForNextFrame(Component.translatable(prefix + "buffer", Math.round(menu.buffer() * 100)), mouseX, mouseY);
        } else if (overMode(mouseX, mouseY)) {
            g.setTooltipForNextFrame(Component.translatable(prefix + (menu.mode() == 0 ? "mode_rod" : "mode_gear")), mouseX, mouseY);
        } else if (overEject(mouseX, mouseY)) {
            g.setTooltipForNextFrame(Component.translatable(prefix + (menu.autoEject() ? "eject_on" : "eject_off")), mouseX, mouseY);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        ElectricMachineBlockEntity.Status status = menu.status();
        Component line = ElectricMachineBlockEntity.statusLine(status, menu.powerPercent());
        int colour = status.light() == dev.strataindustria.power.StatusLight.ERROR ? 0xFF8A2A20
                : status.light() == dev.strataindustria.power.StatusLight.WAIT ? 0xFF7A4A20 : 0xFF404040;
        g.text(font, line, 8, ElectricMachineLayout.STATUS_Y, colour, false);
        Identifier badge = sprite(menu.tier() == ElectricTier.MV ? "tier_mv" : "tier_lv");
        g.blitSprite(RenderPipelines.GUI_TEXTURED, badge, imageWidth - 8 - BADGE_W, titleLabelY - 1, BADGE_W, BADGE_H);
    }
}
