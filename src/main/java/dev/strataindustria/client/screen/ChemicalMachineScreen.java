package dev.strataindustria.client.screen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.electric.machine.ChemicalMachineLayout;
import dev.strataindustria.electric.machine.ChemicalMachineMenu;
import dev.strataindustria.electric.machine.ElectricMachineBlockEntity;
import dev.strataindustria.power.ElectricTier;
import dev.strataindustria.power.StatusLight;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;

/**
 * The mixer and electrolyser screen (spec 10.1): tanks drawn as fluid-coloured columns with a lighter rim
 * on top, the arrow, the power bar, the status line (naming the full tank) and the auto-eject toggle.
 */
public class ChemicalMachineScreen extends AbstractContainerScreen<ChemicalMachineMenu> {
    private static final Identifier ARROW = Identifier.withDefaultNamespace("container/furnace/burn_progress");
    private static final Identifier BAR_RUN = sprite("power_bar_run"), BAR_LOW = sprite("power_bar_low"), BAR_STOPPED = sprite("power_bar_stopped");
    private static final Identifier EJECT_OFF = sprite("eject_off"), EJECT_ON = sprite("eject_on"), EJECT_HIGHLIGHTED = sprite("eject_highlighted");
    /** Fluid colours, matching the sprites; anything else is a neutral grey. Gases are drawn a little lighter. */
    private static final Map<String, Integer> COLOURS = Map.ofEntries(
            Map.entry("water", 0x3F76E4), Map.entry("sulfuric_acid", 0xD8C85A), Map.entry("sulfur_dioxide", 0xC9C49A),
            Map.entry("oxygen", 0x9EDCE6), Map.entry("hydrogen", 0xC4DAF4), Map.entry("chlorine", 0xB4B45E), Map.entry("brine", 0x6E8C84),
            Map.entry("lye", 0xC8C0A8), Map.entry("latex", 0xEFE6D0), Map.entry("steam", 0xDCE4EA));
    private final Identifier background;

    public ChemicalMachineScreen(ChemicalMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, ChemicalMachineLayout.WIDTH, ChemicalMachineLayout.HEIGHT);
        this.inventoryLabelY = ChemicalMachineLayout.INVENTORY_Y - 11;
        this.background = StrataIndustria.id("textures/gui/" + menu.layout().id() + ".png");
    }

    private static Identifier sprite(String name) {
        return StrataIndustria.id("container/electric_machine/" + name);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.blit(RenderPipelines.GUI_TEXTURED, background, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        int w = Math.round(menu.progress() * 24);
        if (w > 0) g.blitSprite(RenderPipelines.GUI_TEXTURED, ARROW, 24, 16, 0, 0, leftPos + menu.layout().arrowX(), topPos + ChemicalMachineLayout.ARROW_Y, w, 16);
        for (int tank = 0; tank < menu.layout().tanks(); tank++) drawTank(g, tank);
        powerBar(g);
        int ex = leftPos + ChemicalMachineLayout.EJECT_X, ey = topPos + ChemicalMachineLayout.EJECT_Y, size = ChemicalMachineLayout.EJECT_SIZE;
        g.blitSprite(RenderPipelines.GUI_TEXTURED, menu.autoEject() ? EJECT_ON : EJECT_OFF, ex, ey, size, size);
        if (overEject(mouseX, mouseY)) g.blitSprite(RenderPipelines.GUI_TEXTURED, EJECT_HIGHLIGHTED, ex, ey, size, size);
    }

    private static int colour(Fluid fluid) {
        return 0xFF000000 | COLOURS.getOrDefault(BuiltInRegistries.FLUID.getKey(fluid).getPath(), 0x909090);
    }

    private void drawTank(GuiGraphicsExtractor g, int tank) {
        int amount = menu.amount(tank);
        if (amount <= 0) return;
        int h = Math.max(1, Math.round(ChemicalMachineLayout.TANK_H * Math.min(1.0f, amount / (float) ChemicalMachineLayout.TANK_CAPACITY)));
        int x = leftPos + menu.layout().tankX(tank), bottom = topPos + ChemicalMachineLayout.TANK_Y + ChemicalMachineLayout.TANK_H;
        int base = colour(menu.tankFluid(tank));
        g.fill(x, bottom - h, x + ChemicalMachineLayout.TANK_W, bottom, base);
        // A lighter rim on the surface and a darker strip down the right side give the column some depth.
        g.fill(x, bottom - h, x + ChemicalMachineLayout.TANK_W, bottom - h + 1, lighter(base));
        g.fill(x + ChemicalMachineLayout.TANK_W - 3, bottom - h + 1, x + ChemicalMachineLayout.TANK_W, bottom, darker(base));
        g.fill(x + 2, bottom - h + 1, x + 4, bottom, lighter(base) & 0x80FFFFFF | 0x40000000);
    }

    private static int lighter(int argb) {
        return 0xFF000000 | mix(argb, 0xFFFFFF, 0.35f);
    }

    private static int darker(int argb) {
        return 0xFF000000 | mix(argb, 0x000000, 0.25f);
    }

    private static int mix(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int gr = Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return r << 16 | gr << 8 | bl;
    }

    private void powerBar(GuiGraphicsExtractor g) {
        ElectricMachineBlockEntity.Status status = menu.status();
        boolean stopped = status == ElectricMachineBlockEntity.Status.NO_POWER
                || status.light() == StatusLight.ERROR && status != ElectricMachineBlockEntity.Status.OUTPUT_FULL
                && status != ElectricMachineBlockEntity.Status.TANK_FULL;
        int h = stopped ? ChemicalMachineLayout.BAR_H : Math.round(menu.buffer() * ChemicalMachineLayout.BAR_H);
        if (h <= 0) return;
        Identifier bar = stopped ? BAR_STOPPED : status == ElectricMachineBlockEntity.Status.LOW_POWER ? BAR_LOW : BAR_RUN;
        int x = leftPos + ChemicalMachineLayout.BAR_X, bottom = topPos + ChemicalMachineLayout.BAR_Y + ChemicalMachineLayout.BAR_H;
        g.blitSprite(RenderPipelines.GUI_TEXTURED, bar, ChemicalMachineLayout.BAR_W, ChemicalMachineLayout.BAR_H, 0, ChemicalMachineLayout.BAR_H - h,
                x, bottom - h, ChemicalMachineLayout.BAR_W, h);
    }

    private boolean overEject(double mouseX, double mouseY) {
        double x = mouseX - leftPos - ChemicalMachineLayout.EJECT_X, y = mouseY - topPos - ChemicalMachineLayout.EJECT_Y;
        return x >= 0 && x < ChemicalMachineLayout.EJECT_SIZE && y >= 0 && y < ChemicalMachineLayout.EJECT_SIZE;
    }

    private boolean overBar(int mouseX, int mouseY) {
        int x = mouseX - leftPos - ChemicalMachineLayout.BAR_X, y = mouseY - topPos - ChemicalMachineLayout.BAR_Y;
        return x >= 0 && x < ChemicalMachineLayout.BAR_W && y >= 0 && y < ChemicalMachineLayout.BAR_H;
    }

    private int overTank(int mouseX, int mouseY) {
        for (int tank = 0; tank < menu.layout().tanks(); tank++) {
            int x = mouseX - leftPos - menu.layout().tankX(tank), y = mouseY - topPos - ChemicalMachineLayout.TANK_Y;
            if (x >= 0 && x < ChemicalMachineLayout.TANK_W && y >= 0 && y < ChemicalMachineLayout.TANK_H) return tank;
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && overEject(event.x(), event.y()) && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ChemicalMachineMenu.BUTTON_EJECT);
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
        int tank = overTank(mouseX, mouseY);
        if (tank >= 0) {
            Component line = menu.amount(tank) <= 0 ? Component.translatable(prefix + "tank_empty")
                    : Component.translatable(prefix + "tank", menu.tankFluid(tank).getFluidType().getDescription(), menu.amount(tank),
                            ChemicalMachineLayout.TANK_CAPACITY);
            g.setTooltipForNextFrame(line, mouseX, mouseY);
        } else if (overBar(mouseX, mouseY)) {
            g.setTooltipForNextFrame(Component.translatable(prefix + "buffer", Math.round(menu.buffer() * 100)), mouseX, mouseY);
        } else if (overEject(mouseX, mouseY)) {
            g.setTooltipForNextFrame(Component.translatable(prefix + (menu.autoEject() ? "eject_on" : "eject_off")), mouseX, mouseY);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        ElectricMachineBlockEntity.Status status = menu.status();
        Component line = status == ElectricMachineBlockEntity.Status.TANK_FULL
                ? Component.translatable(status.key(), menu.fullFluid().getFluidType().getDescription())
                : ElectricMachineBlockEntity.statusLine(status, menu.powerPercent());
        int colour = status.light() == StatusLight.ERROR ? 0xFF8A2A20 : status.light() == StatusLight.WAIT ? 0xFF7A4A20 : 0xFF404040;
        g.text(font, line, 8, ChemicalMachineLayout.STATUS_Y, colour, false);
        Identifier badge = sprite(menu.tier() == ElectricTier.MV ? "tier_mv" : "tier_lv");
        g.blitSprite(RenderPipelines.GUI_TEXTURED, badge, imageWidth - 8 - ElectricMachineScreen.BADGE_W, titleLabelY - 1,
                ElectricMachineScreen.BADGE_W, ElectricMachineScreen.BADGE_H);
    }
}
