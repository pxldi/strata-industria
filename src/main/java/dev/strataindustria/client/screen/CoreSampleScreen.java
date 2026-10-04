package dev.strataindustria.client.screen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.prospecting.CoreSample;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Reads a core sample (tier 3 spec 8.5): the core as a strip, one row per block of depth coloured by
 * its rock, beside the rock layers and the ore found, with depths. Hover the strip for a single row.
 */
public class CoreSampleScreen extends Screen {
    private static final Identifier BACKGROUND = StrataIndustria.id("textures/gui/core_sample.png");
    public static final int WIDTH = 220, HEIGHT = 172;
    public static final int STRIP_X = 13, STRIP_Y = 20, STRIP_W = 18, ROW_H = 2;
    public static final int TEXT_X = 44, TEXT_Y = 22, TEXT_LINES = 13, LINE_H = 10;
    private static final int TEXT = 0xFF3A3530, HEADING = 0xFF6A4A28, FAINT = 0xFF8A8078;

    private final CoreSample sample;
    private final List<Component> lines = new ArrayList<>();
    private final List<Integer> colours = new ArrayList<>();
    private int left, top, scroll;

    public CoreSampleScreen(CoreSample sample) {
        super(Component.translatable("item." + StrataIndustria.MOD_ID + ".core_sample"));
        this.sample = sample;
        add(Component.translatable(StrataIndustria.MOD_ID + ".core_sample.layers"), HEADING);
        for (CoreSample.Segment segment : sample.segments()) add(segment.line(), TEXT);
        add(Component.empty(), TEXT);
        add(Component.translatable(StrataIndustria.MOD_ID + ".core_sample.finds"), HEADING);
        if (sample.finds().isEmpty()) add(Component.translatable(StrataIndustria.MOD_ID + ".core_sample.barren"), FAINT);
        for (CoreSample.Find find : sample.finds()) add(find.line(), TEXT);
    }

    private void add(Component line, int colour) {
        lines.add(line);
        colours.add(colour);
    }

    @Override
    protected void init() {
        super.init();
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, left, top, 0, 0, WIDTH, HEIGHT, 256, 256);
        g.text(font, title, left + STRIP_X - 1, top + 6, 0xFF404040, false);
        Component where = Component.translatable(StrataIndustria.MOD_ID + ".core_sample.where",
                sample.origin().getX(), sample.origin().getZ());
        g.text(font, where, left + WIDTH - 12 - font.width(where), top + 6, 0xFF606060, false);

        List<CoreSample.Row> rows = sample.rows();
        for (int i = 0; i < rows.size(); i++) {
            int y = top + STRIP_Y + i * ROW_H;
            g.fill(left + STRIP_X, y, left + STRIP_X + STRIP_W, y + ROW_H, 0xFF000000 | rows.get(i).colour());
        }
        int row = hoveredRow(mouseX, mouseY);
        if (row >= 0) {
            int y = top + STRIP_Y + row * ROW_H;
            g.fill(left + STRIP_X, y, left + STRIP_X + STRIP_W, y + ROW_H, 0x60FFFFFF);
            g.setTooltipForNextFrame(Component.translatable(StrataIndustria.MOD_ID + ".core_sample.layer",
                    Component.translatable(StrataIndustria.MOD_ID + ".core_sample.at", sample.top() - row),
                    Component.translatable(rows.get(row).name())), mouseX, mouseY);
        }

        for (int i = 0; i < TEXT_LINES && scroll + i < lines.size(); i++) {
            g.text(font, lines.get(scroll + i), left + TEXT_X, top + TEXT_Y + i * LINE_H, colours.get(scroll + i), false);
        }
        if (lines.size() > TEXT_LINES) {
            Component more = Component.translatable(StrataIndustria.MOD_ID + ".core_sample.scroll", scroll + 1,
                    lines.size() - TEXT_LINES + 1);
            g.text(font, more, left + WIDTH - 16 - font.width(more), top + TEXT_Y + TEXT_LINES * LINE_H + 1, FAINT, false);
        }
    }

    private int hoveredRow(double mouseX, double mouseY) {
        double x = mouseX - left - STRIP_X, y = mouseY - top - STRIP_Y;
        if (x < 0 || x >= STRIP_W || y < 0) return -1;
        int row = (int) (y / ROW_H);
        return row < sample.rows().size() ? row : -1;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int max = Math.max(0, lines.size() - TEXT_LINES);
        scroll = Math.clamp(scroll - (int) Math.signum(scrollY), 0, max);
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
