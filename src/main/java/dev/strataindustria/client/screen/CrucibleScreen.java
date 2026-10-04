package dev.strataindustria.client.screen;

import dev.strataindustria.client.HeatWords;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.heat.HeatBand;
import dev.strataindustria.material.Metal;
import dev.strataindustria.metal.Alloy;
import dev.strataindustria.metal.CrucibleBlockEntity;
import dev.strataindustria.metal.CrucibleMenu;
import dev.strataindustria.metal.CrucibleStatus;
import dev.strataindustria.metal.Melt;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Crucible: inputs on the left with melt progress over each, the melt bar and its make-up in the
 * middle, the gauge, mold slot and Pour button on the right, and a status line under it all.
 */
public class CrucibleScreen extends AbstractContainerScreen<CrucibleMenu> {
    private static final Identifier BACKGROUND = StrataIndustria.id("textures/gui/crucible.png");
    public static final int BAR_X = 66, BAR_Y = 18, BAR_W = 8, BAR_H = 52;
    public static final int GAUGE_X = 160, GAUGE_Y = 18, GAUGE_W = 8, GAUGE_H = 52;
    public static final float GAUGE_MAX = 1750.0f;
    public static final int TEXT_X = 80, TEXT_Y = 18;
    public static final int STATUS_Y = 98;
    public static final int POUR_X = 28, POUR_Y = 78;

    private Button pour;

    public CrucibleScreen(CrucibleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 210);
        this.inventoryLabelY = CrucibleMenu.INVENTORY_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        pour = addRenderableWidget(Button.builder(Component.translatable(StrataIndustria.MOD_ID + ".crucible.pour"), b -> {
            if (minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, CrucibleMenu.BUTTON_POUR);
        }).bounds(leftPos + POUR_X, topPos + POUR_Y, 40, 16).build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        CrucibleStatus status = menu.status();
        if (pour != null) pour.active = (status == CrucibleStatus.MOLTEN || status == CrucibleStatus.CARBON_BURNED) && menu.pourPercent() == 0;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);

        drawSlotProgress(g, leftPos + CrucibleMenu.GRID_X, topPos + CrucibleMenu.GRID_Y, menu::slotProgress);
        drawMeltBar(g, leftPos + BAR_X, topPos + BAR_Y, menu.melt(), menu.capacity());

        int poured = menu.pourPercent();
        if (poured > 0) {
            int x = leftPos + POUR_X + 44, y = topPos + POUR_Y + 6;
            g.fill(x, y, x + Math.round(30 * poured / 100f), y + 4, 0xFFF07A22);
        }

        drawGauge(g, leftPos + GAUGE_X, topPos + GAUGE_Y, menu.temperature());
    }

    /** Melt progress rises over each input of the 3x3 grid at {@code x, y} like a liquid line. */
    static void drawSlotProgress(GuiGraphicsExtractor g, int x0, int y0, java.util.function.IntUnaryOperator progress) {
        for (int i = 0; i < CrucibleBlockEntity.INPUT_SLOTS; i++) {
            int p = progress.applyAsInt(i);
            if (p <= 0) continue;
            int h = Math.max(1, Math.round(16 * Math.min(100, p) / 100f));
            int x = x0 + (i % 3) * 18, y = y0 + (i / 3) * 18 + 16 - h;
            g.fill(x, y, x + 16, y + h, 0x60F07A22);
        }
    }

    /** Contents bar: each metal stacked in its own colour, scaled to capacity. */
    static void drawMeltBar(GuiGraphicsExtractor g, int x, int y, Melt melt, int capacity) {
        capacity = Math.max(1, capacity);
        int bottom = y + BAR_H;
        for (Metal metal : Metal.values()) {
            int u = melt.units().getOrDefault(metal, 0);
            if (u <= 0) continue;
            int h = Math.max(1, Math.round(BAR_H * Math.min(1f, u / (float) capacity)));
            g.fill(x, bottom - h, x + BAR_W, bottom, 0xFF000000 | metal.colour());
            bottom -= h;
        }
    }

    /** The temperature gauge, filled in its heat band's colour. */
    static void drawGauge(GuiGraphicsExtractor g, int x, int y, float t) {
        int filled = Math.round(Math.min(1.0f, Math.max(0.0f, t / GAUGE_MAX)) * GAUGE_H);
        if (filled > 0) {
            int b = y + GAUGE_H;
            g.fill(x, b - filled, x + GAUGE_W, b, 0xFF000000 | HeatBand.of(t).colour());
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        Melt melt = menu.melt();
        drawMeltText(g, font, melt, menu.capacity(), TEXT_X, TEXT_Y);
        g.text(font, statusLine(menu.status(), melt, menu.meltingPercent(), menu.maxTemperature()), 8, STATUS_Y, 0xFF404040, false);
        Component hint = hintLine(melt);
        if (hint != null) g.text(font, hint, 8, STATUS_Y + 10, 0xFF7a4a20, false);
    }

    /** Units against capacity, then each metal's share in a darker shade of its colour. */
    static void drawMeltText(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font, Melt melt, int capacity, int x, int y0) {
        int y = y0;
        g.text(font, Component.literal(melt.total() + " / " + capacity), x, y, 0xFF404040, false);
        for (Metal metal : Metal.values()) {
            int u = melt.units().getOrDefault(metal, 0);
            if (u <= 0) continue;
            y += 10;
            Component line = Component.translatable(StrataIndustria.MOD_ID + ".metal." + metal.id())
                    .append(String.format(Locale.ROOT, " %d%%", Math.round(melt.share(metal) * 100)));
            g.text(font, line, x, y, 0xFF000000 | darker(metal.colour()), false);
            if (y > y0 + 40) break;
        }
    }

    static Component statusLine(CrucibleStatus status, Melt melt, int meltingPercent, int maxTemperature) {
        return switch (status) {
            case MELTING -> Component.translatable(status.key(), meltingPercent);
            case AT_LIMIT -> Component.translatable(status.key(), HeatWords.of(maxTemperature));
            case MOLTEN -> Alloy.resultOf(melt)
                    .map(m -> Component.translatable(status.key(), Component.translatable(StrataIndustria.MOD_ID + ".metal." + m.id())))
                    .orElse(Component.translatable(StrataIndustria.MOD_ID + ".crucible.status.molten_unknown"));
            default -> Component.translatable(status.key());
        };
    }

    /** "Bronze 3:1, add 1 tin" for a mix that is near a part alloy; "Needs 0.5-2% carbon, has 3%" for the iron ones. */
    static Component hintLine(Melt melt) {
        if (melt.units().size() < 2 || Alloy.resultOf(melt).isPresent()) return null;
        Optional<Alloy> closest = Alloy.closest(melt);
        if (closest.isEmpty()) return null;
        Alloy alloy = closest.get();
        if (alloy.byParts()) {
            Optional<Alloy.Missing> missing = alloy.missing(melt);
            if (missing.isEmpty()) return null;
            return Component.translatable(StrataIndustria.MOD_ID + ".crucible.hint_parts",
                    Component.translatable(StrataIndustria.MOD_ID + ".metal." + alloy.result().id()),
                    alloy.baseParts() + ":" + alloy.addedParts(),
                    ingots(missing.get().units(), missing.get().metal() == Metal.REDSTONE ? 25 : 100),
                    Component.translatable(StrataIndustria.MOD_ID + ".metal." + missing.get().metal().id()));
        }
        return Component.translatable(StrataIndustria.MOD_ID + ".crucible.hint",
                Component.translatable(StrataIndustria.MOD_ID + ".metal." + alloy.result().id()),
                percent(alloy.addedMin()), percent(alloy.addedMax()),
                Component.translatable(StrataIndustria.MOD_ID + ".metal." + alloy.added().id()),
                percent(melt.share(alloy.added())));
    }

    /** Units as items of {@code perItem} units, rounded up to a tenth: "1", "0.5". */
    static String ingots(int units, int perItem) {
        int tenths = Math.max(1, (int) Math.ceil(units * 10.0 / perItem));
        return tenths % 10 == 0 ? Integer.toString(tenths / 10) : String.format(Locale.ROOT, "%.1f", tenths / 10f);
    }

    /** Whole percent, or one decimal below 10% so carbon in steel (0.5 to 2%) reads properly. */
    static String percent(float share) {
        float p = share * 100;
        if (p >= 10 || Math.abs(p - Math.round(p)) < 0.05f) return Integer.toString(Math.round(p));
        return String.format(Locale.ROOT, "%.1f", p);
    }

    private static int darker(int rgb) {
        int r = (rgb >> 16 & 255) * 3 / 5, gr = (rgb >> 8 & 255) * 3 / 5, b = (rgb & 255) * 3 / 5;
        return r << 16 | gr << 8 | b;
    }
}
