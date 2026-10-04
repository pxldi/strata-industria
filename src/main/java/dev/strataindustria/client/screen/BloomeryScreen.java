package dev.strataindustria.client.screen;

import dev.strataindustria.client.HeatWords;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.bloomery.BloomeryBlockEntity;
import dev.strataindustria.bloomery.BloomeryBlockEntity.Status;
import dev.strataindustria.bloomery.BloomeryMenu;
import dev.strataindustria.bloomery.BloomeryStructure;
import dev.strataindustria.heat.HeatBand;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Bloomery status (spec 5.3): the hearth's temperature on the left, what it is doing and what it will
 * give on the right, and the burn's progress along the bottom.
 */
public class BloomeryScreen extends AbstractContainerScreen<BloomeryMenu> {
    private static final Identifier BACKGROUND = StrataIndustria.id("textures/gui/bloomery.png");
    public static final int GAUGE_X = 11, GAUGE_Y = 19, GAUGE_W = 10, GAUGE_H = 60;
    public static final float GAUGE_MAX = 1500.0f;
    public static final int BAR_X = 35, BAR_Y = 71, BAR_W = 130, BAR_H = 8;
    public static final int TEXT_X = 34, TEXT_Y = 19, LINE = 10;
    private static final String KEY = StrataIndustria.MOD_ID + ".bloomery.";

    public BloomeryScreen(BloomeryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 90);
        this.inventoryLabelY = 1000;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);

        float temperature = menu.get(BloomeryBlockEntity.DATA_TEMPERATURE);
        int filled = Math.round(Math.min(1.0f, Math.max(0.0f, temperature / GAUGE_MAX)) * GAUGE_H);
        int gx = leftPos + GAUGE_X, bottom = topPos + GAUGE_Y + GAUGE_H;
        if (filled > 0) {
            g.fill(gx, bottom - filled, gx + GAUGE_W, bottom, 0xFF000000 | HeatBand.of(temperature).colour());
            g.fill(gx, bottom - filled, gx + 2, bottom, 0x30FFFFFF);
        }
        // A notch at 1200 °C, the least a bloom needs, and at 1300 °C, full yield.
        for (float mark : new float[] {BloomeryBlockEntity.minTemperature(), BloomeryBlockEntity.fullYieldTemperature()}) {
            int y = bottom - Math.round(mark / GAUGE_MAX * GAUGE_H);
            g.fill(gx - 1, y, gx + GAUGE_W + 1, y + 1, 0xFF2A2420);
        }

        int progress = menu.get(BloomeryBlockEntity.DATA_PROGRESS);
        if (progress > 0) {
            int w = Math.round(progress / 1000.0f * BAR_W);
            g.fill(leftPos + BAR_X, topPos + BAR_Y, leftPos + BAR_X + w, topPos + BAR_Y + BAR_H, 0xFF000000 | HeatBand.of(temperature).colour());
            g.fill(leftPos + BAR_X, topPos + BAR_Y, leftPos + BAR_X + w, topPos + BAR_Y + 2, 0x30FFFFFF);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, title, titleLabelX, titleLabelY, 0xFF404040, false);
        Status status = menu.status();
        int y = TEXT_Y;
        g.text(font, statusLine(status), TEXT_X, y, statusColour(status), false);
        y += LINE + 2;
        int chimney = menu.get(BloomeryBlockEntity.DATA_CHIMNEY);
        if (status == Status.INCOMPLETE) {
            BloomeryStructure.Problem[] problems = BloomeryStructure.Problem.values();
            int p = menu.get(BloomeryBlockEntity.DATA_PROBLEM);
            BloomeryStructure.Problem problem = p >= 0 && p < problems.length ? problems[p] : BloomeryStructure.Problem.NEEDS_BRICK;
            BlockPos at = menu.pos().offset(menu.get(BloomeryBlockEntity.DATA_DX), menu.get(BloomeryBlockEntity.DATA_DY),
                    menu.get(BloomeryBlockEntity.DATA_DZ));
            g.text(font, Component.translatable(KEY + "problem." + (problem == BloomeryStructure.Problem.NEEDS_AIR ? "air" : "brick")),
                    TEXT_X, y, 0xFF404040, false);
            y += LINE;
            g.text(font, Component.translatable(KEY + "at", at.getX(), at.getY(), at.getZ()), TEXT_X, y, 0xFF404040, false);
            y += LINE;
            g.text(font, Component.translatable(KEY + "hint"), TEXT_X, y, 0xFF707070, false);
            return;
        }
        int levels = chimney + 1;
        g.text(font, Component.translatable(KEY + "charge", menu.get(BloomeryBlockEntity.DATA_ORE),
                BloomeryBlockEntity.ORE_PER_LEVEL * levels, menu.get(BloomeryBlockEntity.DATA_CHARCOAL),
                BloomeryBlockEntity.CHARCOAL_PER_LEVEL * levels), TEXT_X, y, 0xFF404040, false);
        y += LINE;
        g.text(font, Component.translatable(KEY + "draught", chimney, menu.get(BloomeryBlockEntity.DATA_BELLOWS),
                HeatWords.of(menu.get(BloomeryBlockEntity.DATA_TARGET))), TEXT_X, y, 0xFF404040, false);
        y += LINE;
        int yield = menu.get(BloomeryBlockEntity.DATA_YIELD);
        Component expect = yield <= 0
                ? Component.translatable(KEY + "no_yield")
                : Component.translatable(KEY + "yield", yield, menu.get(BloomeryBlockEntity.DATA_BLOOMS));
        g.text(font, expect, TEXT_X, y, yield <= 0 ? 0xFF8A3A2A : 0xFF404040, false);
    }

    private Component statusLine(Status status) {
        return switch (status) {
            case NEEDS_CHARCOAL -> {
                int ore = menu.get(BloomeryBlockEntity.DATA_ORE), charcoal = menu.get(BloomeryBlockEntity.DATA_CHARCOAL);
                yield Component.translatable(status.key(), Math.max(0, (ore + 1) / 2 - charcoal));
            }
            case BURNING -> Component.translatable(status.key(), menu.get(BloomeryBlockEntity.DATA_PROGRESS) / 10);
            case HEATING -> Component.translatable(status.key(), HeatWords.of(menu.get(BloomeryBlockEntity.DATA_TEMPERATURE)));
            default -> Component.translatable(status.key());
        };
    }

    private static int statusColour(Status status) {
        return switch (status) {
            case INCOMPLETE, TOO_COOL, NEEDS_CHARCOAL -> 0xFF8A3A2A;
            case READY -> 0xFF2F6B2A;
            case BURNING, HEATING -> 0xFF9A5A10;
            default -> 0xFF404040;
        };
    }
}
