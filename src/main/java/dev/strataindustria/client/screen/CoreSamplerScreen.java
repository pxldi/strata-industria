package dev.strataindustria.client.screen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.machine.CoreSamplerBlockEntity;
import dev.strataindustria.machine.CoreSamplerMenu;
import dev.strataindustria.power.Kinetic;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

/** Core sampler: a Drill button, the drilling progress, the core's slot, then the status and network lines. */
public class CoreSamplerScreen extends AbstractContainerScreen<CoreSamplerMenu> {
    private static final Identifier BACKGROUND = StrataIndustria.id("textures/gui/core_sampler.png");
    public static final int BUTTON_X = 34, BUTTON_Y = 28, BUTTON_W = 54, BUTTON_H = 18;
    public static final int BAR_X = 34, BAR_Y = 58, BAR_W = 80, BAR_H = 6;
    public static final int STATUS_Y = 48;
    // Sprite sheet, to the right of the panel: button faces (normal, hovered, disabled), then the bar fill.
    private static final int BUTTON_U = 176, FILL_U = 176, FILL_V = 54;

    public CoreSamplerScreen(CoreSamplerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        this.inventoryLabelY = CoreSamplerMenu.INVENTORY_Y - 11;
    }

    private boolean overButton(double mouseX, double mouseY) {
        double x = mouseX - leftPos - BUTTON_X, y = mouseY - topPos - BUTTON_Y;
        return x >= 0 && x < BUTTON_W && y >= 0 && y < BUTTON_H;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && minecraft.gameMode != null && overButton(event.x(), event.y())) {
            if (menu.canDrill()) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, CoreSamplerBlockEntity.DRILL_BUTTON);
                if (minecraft.player != null) minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.4f, 1.0f);
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        boolean live = menu.canDrill();
        int v = !live ? 36 : overButton(mouseX, mouseY) ? 18 : 0;
        g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos + BUTTON_X, topPos + BUTTON_Y, BUTTON_U, v, BUTTON_W, BUTTON_H, 256, 256);
        Component label = Component.translatable(StrataIndustria.MOD_ID + ".core_sampler.drill");
        g.text(font, label, leftPos + BUTTON_X + (BUTTON_W - font.width(label)) / 2, topPos + BUTTON_Y + 5,
                live ? 0xFFFFFFFF : 0xFFA0A0A0, live);
        int w = Math.round(menu.progress() * BAR_W);
        if (w > 0) g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos + BAR_X, topPos + BAR_Y, FILL_U, FILL_V, w, BAR_H, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        CoreSamplerBlockEntity.Status status = menu.status();
        Component line = status == CoreSamplerBlockEntity.Status.TOO_SLOW
                ? Component.translatable(status.key(), CoreSamplerBlockEntity.MIN_SPEED)
                : Component.translatable(status.key());
        boolean fine = status == CoreSamplerBlockEntity.Status.IDLE || status == CoreSamplerBlockEntity.Status.DRILLING
                || status == CoreSamplerBlockEntity.Status.DONE;
        g.text(font, line, BAR_X, STATUS_Y, fine ? 0xFF404040 : 0xFF7A4A20, false);
        if (minecraft.level != null && minecraft.level.getBlockEntity(menu.pos()) instanceof Kinetic kinetic) {
            Component network = kinetic.kinetic().line();
            g.text(font, network, imageWidth - 8 - font.width(network), titleLabelY, 0xFF404040, false);
        }
    }
}
