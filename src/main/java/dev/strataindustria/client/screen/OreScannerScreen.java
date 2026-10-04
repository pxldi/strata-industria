package dev.strataindustria.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.geology.OreGrade;
import dev.strataindustria.oil.SeismicSurvey;
import dev.strataindustria.prospecting.OreScan;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * The ore scanner's map (spec 13.2): the 3 x 3 chunks as tiles in the vanilla map colours, and beside them every ore
 * found with its count. Click an ore to outline the chunks that hold it; hover a chunk or an ore for the details.
 * Once a seismic charge has gone off near the scanner a second tab shows that survey (tier 6 spec 5.2): the surveyed
 * chunks with each reservoir's footprint outlined, and beside them what is known of each.
 */
public class OreScannerScreen extends Screen {
    private static final int PIXEL = 3, TILE = 16 * PIXEL, MAP = TILE * OreScan.SIDE;
    private static final int WIDTH = 270, HEIGHT = 176;
    private static final int MAP_X = 12, MAP_Y = 24, LIST_X = MAP_X + MAP + 14, LIST_Y = 24, ROW_H = 11, ROWS = 12;
    private static final int PANEL = 0xFF14201a, EDGE = 0xFF3a4a40, TEXT = 0xFFb8e6a8, HEADING = 0xFF8ad66a, FAINT = 0xFF6a8a62, MARK = 0xFFf2c45a;

    private static final int[] SIZE_COLOUR = {0xFF6a8a62, 0xFFc8a84a, 0xFFd8663a};
    private static final int SEISMIC_MAP = 140, SEISMIC_ROW_H = 11, SEISMIC_ROWS = 12, SEISMIC_LINES = 3;

    private final @Nullable OreScan scan;
    private final @Nullable SeismicSurvey survey;
    private final List<Map.Entry<String, Integer>> ores;
    private int left, top, scroll, seismicScroll, selectedHit = -1;
    private String selected;
    private boolean seismicTab;

    /** @param seismicTab which tab to open on; the tab of a missing scan or survey is never shown */
    public OreScannerScreen(@Nullable OreScan scan, @Nullable SeismicSurvey survey, boolean seismicTab) {
        super(Component.translatable("item." + StrataIndustria.MOD_ID + ".ore_scanner"));
        this.scan = scan;
        this.survey = survey;
        this.ores = scan == null ? new ArrayList<>() : new ArrayList<>(scan.totals().entrySet());
        this.seismicTab = survey != null && (seismicTab || scan == null);
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
        g.fill(left - 2, top - 2, left + WIDTH + 2, top + HEIGHT + 2, EDGE);
        g.fill(left, top, left + WIDTH, top + HEIGHT, PANEL);
        int headX = left + MAP_X;
        if (scan != null && survey != null) {
            headX = tab(g, headX, "ore", !seismicTab, mouseX, mouseY);
            tab(g, headX, "seismic", seismicTab, mouseX, mouseY);
        } else {
            g.text(font, title, headX, top + 8, HEADING, false);
        }
        if (seismicTab && survey != null) {
            extractSeismic(g, mouseX, mouseY);
            return;
        }
        if (scan == null) return;
        Component where = Component.translatable(StrataIndustria.MOD_ID + ".ore_scanner.where", scan.chunkX() * 16, scan.chunkZ() * 16);
        g.text(font, where, left + WIDTH - 10 - font.width(where), top + 8, FAINT, false);

        for (int index = 0; index < scan.tiles().size(); index++) {
            OreScan.Tile tile = scan.tiles().get(index);
            int tx = left + MAP_X + index % OreScan.SIDE * TILE, ty = top + MAP_Y + index / OreScan.SIDE * TILE;
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    g.fill(tx + x * PIXEL, ty + z * PIXEL, tx + x * PIXEL + PIXEL, ty + z * PIXEL + PIXEL, tile.argb(x, z));
                }
            }
            boolean marked = selected != null && tile.finds().stream().anyMatch(find -> find.ore().equals(selected));
            outline(g, tx, ty, TILE, marked ? MARK : 0x40000000, marked ? 2 : 1);
        }

        for (int i = 0; i < ROWS && scroll + i < ores.size(); i++) {
            Map.Entry<String, Integer> ore = ores.get(scroll + i);
            int y = top + LIST_Y + i * ROW_H;
            boolean chosen = ore.getKey().equals(selected);
            if (chosen) g.fill(left + LIST_X - 2, y - 1, left + WIDTH - 8, y + ROW_H - 2, 0xFF24382c);
            Component name = Component.translatable(StrataIndustria.MOD_ID + ".ore." + ore.getKey());
            g.text(font, name, left + LIST_X, y, chosen ? MARK : TEXT, false);
            String count = String.valueOf(ore.getValue());
            g.text(font, count, left + WIDTH - 12 - font.width(count), y, chosen ? MARK : FAINT, false);
        }
        if (ores.isEmpty()) g.text(font, Component.translatable(StrataIndustria.MOD_ID + ".ore_scanner.nothing"), left + LIST_X, top + LIST_Y, FAINT, false);
        if (ores.size() > ROWS) {
            Component more = Component.translatable(StrataIndustria.MOD_ID + ".ore_scanner.scroll", scroll + 1, ores.size() - ROWS + 1);
            g.text(font, more, left + WIDTH - 12 - font.width(more), top + HEIGHT - 12, FAINT, false);
        }
        detail(g, mouseX, mouseY);
    }

    /** Draws one tab label and returns where the next starts. */
    private int tab(GuiGraphicsExtractor g, int x, String name, boolean active, int mouseX, int mouseY) {
        Component label = Component.translatable(StrataIndustria.MOD_ID + ".ore_scanner.tab." + name);
        boolean over = tabAt(mouseX, mouseY) == (name.equals("ore") ? 0 : 1);
        g.text(font, label, x, top + 8, active ? HEADING : over ? TEXT : FAINT, false);
        if (active) g.fill(x, top + 18, x + font.width(label), top + 19, HEADING);
        return x + font.width(label) + 14;
    }

    /** The tab under the pointer, 0 for ore, 1 for seismic, or -1. */
    private int tabAt(double mouseX, double mouseY) {
        if (scan == null || survey == null || mouseY < top + 4 || mouseY >= top + 20) return -1;
        int oreWidth = font.width(Component.translatable(StrataIndustria.MOD_ID + ".ore_scanner.tab.ore"));
        int seismicWidth = font.width(Component.translatable(StrataIndustria.MOD_ID + ".ore_scanner.tab.seismic"));
        double x = mouseX - left - MAP_X;
        if (x >= 0 && x < oreWidth) return 0;
        return x >= oreWidth + 14 && x < oreWidth + 14 + seismicWidth ? 1 : -1;
    }

    // ---------------------------------------------------------------- seismic tab

    private int seismicTile() {
        return survey == null ? 0 : Math.min(28, SEISMIC_MAP / survey.side());
    }

    private void extractSeismic(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        SeismicSurvey s = survey;
        if (s == null) return;
        Component where = Component.translatable(StrataIndustria.MOD_ID + ".ore_scanner.seismic.where", s.chunkX() * 16, s.chunkZ() * 16, s.originY());
        g.text(font, where, left + WIDTH - 10 - font.width(where), top + 8, FAINT, false);
        int tile = seismicTile(), side = s.side(), size = tile * side;
        int mapX = left + MAP_X, mapY = top + MAP_Y;
        g.fill(mapX, mapY, mapX + size, mapY + size, 0xFF0c1410);
        for (int row = 0; row < side; row++) {
            for (int column = 0; column < side; column++) {
                int x = mapX + column * tile, y = mapY + row * tile;
                outline(g, x, y, tile, 0xFF1c2c22, 1);
                for (int h = 0; h < s.hits().size(); h++) {
                    SeismicSurvey.Hit hit = s.hits().get(h);
                    if (!hit.covers(side, column, row)) continue;
                    int colour = SIZE_COLOUR[hit.sizeClass().ordinal()];
                    g.fill(x + 1, y + 1, x + tile - 1, y + tile - 1, colour & 0x00FFFFFF | (h == selectedHit ? 0xA0000000 : 0x60000000));
                    int line = h == selectedHit ? MARK : colour;
                    if (row == 0 || !hit.covers(side, column, row - 1)) g.fill(x, y, x + tile, y + 1, line);
                    if (row == side - 1 || !hit.covers(side, column, row + 1)) g.fill(x, y + tile - 1, x + tile, y + tile, line);
                    if (column == 0 || !hit.covers(side, column - 1, row)) g.fill(x, y, x + 1, y + tile, line);
                    if (column == side - 1 || !hit.covers(side, column + 1, row)) g.fill(x + tile - 1, y, x + tile, y + tile, line);
                }
            }
        }
        // An arrow on the edge where a reservoir goes on: that is the way to walk.
        for (SeismicSurvey.Hit hit : s.hits()) {
            int mid = size / 2;
            if ((hit.edges() & SeismicSurvey.NORTH) != 0) arrow(g, mapX + mid, mapY - 6, 0, -1);
            if ((hit.edges() & SeismicSurvey.SOUTH) != 0) arrow(g, mapX + mid, mapY + size + 6, 0, 1);
            if ((hit.edges() & SeismicSurvey.WEST) != 0) arrow(g, mapX - 6, mapY + mid, -1, 0);
            if ((hit.edges() & SeismicSurvey.EAST) != 0) arrow(g, mapX + size + 6, mapY + mid, 1, 0);
        }

        if (s.hits().isEmpty()) {
            g.text(font, Component.translatable(StrataIndustria.MOD_ID + ".ore_scanner.seismic.none"), left + LIST_X, top + LIST_Y, FAINT, false);
        }
        int first = seismicScroll;
        for (int h = first; h < s.hits().size() && (h - first + 1) * SEISMIC_LINES <= SEISMIC_ROWS; h++) {
            SeismicSurvey.Hit hit = s.hits().get(h);
            int y = top + LIST_Y + (h - first) * SEISMIC_LINES * SEISMIC_ROW_H;
            boolean chosen = h == selectedHit;
            if (chosen) g.fill(left + LIST_X - 2, y - 1, left + WIDTH - 8, y + SEISMIC_LINES * SEISMIC_ROW_H - 2, 0xFF24382c);
            g.fill(left + LIST_X, y + 1, left + LIST_X + 4, y + 7, SIZE_COLOUR[hit.sizeClass().ordinal()]);
            g.text(font, Component.translatable(StrataIndustria.MOD_ID + ".ore_scanner.seismic.size." + hit.sizeClass().id()), left + LIST_X + 8, y,
                    chosen ? MARK : TEXT, false);
            int depth = s.originY() - hit.topY();
            Component depthLine = Component.translatable(StrataIndustria.MOD_ID + ".ore_scanner.seismic." + (depth >= 0 ? "below" : "above"), hit.topY(),
                    Math.abs(depth));
            g.text(font, depthLine, left + LIST_X + 8, y + SEISMIC_ROW_H, FAINT, false);
            if (hit.remaining() >= 0) {
                g.text(font, Component.translatable(StrataIndustria.MOD_ID + ".ore_scanner.seismic.left", hit.remaining()), left + LIST_X + 8,
                        y + 2 * SEISMIC_ROW_H, FAINT, false);
            }
        }
        int tileHit = seismicTileAt(mouseX, mouseY);
        if (tileHit >= 0) {
            List<Component> lines = new ArrayList<>();
            int column = tileHit % side, row = tileHit / side;
            lines.add(Component.translatable(StrataIndustria.MOD_ID + ".ore_scanner.chunk", s.chunkX() + column, s.chunkZ() + row));
            for (SeismicSurvey.Hit hit : s.hits()) {
                if (!hit.covers(side, column, row)) continue;
                lines.add(Component.translatable(StrataIndustria.MOD_ID + ".ore_scanner.seismic.size." + hit.sizeClass().id()));
                lines.add(Component.translatable(StrataIndustria.MOD_ID + ".ore_scanner.seismic." + (s.originY() >= hit.topY() ? "below" : "above"),
                        hit.topY(), Math.abs(s.originY() - hit.topY())).withStyle(net.minecraft.ChatFormatting.GRAY));
            }
            if (lines.size() == 1) lines.add(Component.translatable(StrataIndustria.MOD_ID + ".ore_scanner.seismic.empty").withStyle(net.minecraft.ChatFormatting.GRAY));
            g.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
        }
    }

    /** A small filled triangle pointing along ({@code dx}, {@code dy}) with its tip two blocks past the point. */
    private static void arrow(GuiGraphicsExtractor g, int cx, int cy, int dx, int dy) {
        for (int i = 0; i < 4; i++) {
            int x = cx + dx * (2 - i), y = cy + dy * (2 - i);
            if (dy != 0) g.fill(x - i, y, x + i + 1, y + 1, MARK);
            else g.fill(x, y - i, x + 1, y + i + 1, MARK);
        }
    }

    private int seismicTileAt(double mouseX, double mouseY) {
        if (survey == null) return -1;
        int tile = seismicTile(), side = survey.side();
        double x = mouseX - left - MAP_X, y = mouseY - top - MAP_Y;
        if (x < 0 || y < 0 || x >= tile * side || y >= tile * side) return -1;
        return (int) (y / tile) * side + (int) (x / tile);
    }

    private int seismicRowAt(double mouseX, double mouseY) {
        if (survey == null) return -1;
        double x = mouseX - left - LIST_X, y = mouseY - top - LIST_Y;
        if (x < -2 || x >= WIDTH - LIST_X - 8 || y < 0 || y >= SEISMIC_ROWS * SEISMIC_ROW_H) return -1;
        int hit = seismicScroll + (int) (y / (SEISMIC_ROW_H * SEISMIC_LINES));
        return hit < survey.hits().size() ? hit : -1;
    }

    private void detail(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (scan == null) return;
        int tile = tileAt(mouseX, mouseY);
        if (tile >= 0) {
            OreScan.Tile hovered = scan.tiles().get(tile);
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable(StrataIndustria.MOD_ID + ".ore_scanner.chunk", scan.chunkX() + tile % OreScan.SIDE,
                    scan.chunkZ() + tile / OreScan.SIDE));
            if (hovered.finds().isEmpty()) lines.add(Component.translatable(StrataIndustria.MOD_ID + ".ore_scanner.barren"));
            for (OreScan.Find find : hovered.finds()) lines.add(findLine(find));
            g.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
            return;
        }
        int row = rowAt(mouseX, mouseY);
        if (row >= 0) {
            String ore = ores.get(row).getKey();
            int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE, chunks = 0;
            for (OreScan.Tile t : scan.tiles()) {
                boolean here = false;
                for (OreScan.Find find : t.finds()) {
                    if (!find.ore().equals(ore)) continue;
                    here = true;
                    min = Math.min(min, find.minY());
                    max = Math.max(max, find.maxY());
                }
                if (here) chunks++;
            }
            g.setTooltipForNextFrame(Component.translatable(StrataIndustria.MOD_ID + ".ore_scanner.range", min, max, chunks), mouseX, mouseY);
        }
    }

    /** "Native copper 34, mostly rich, Y 12 to 40". */
    private static Component findLine(OreScan.Find find) {
        Component ore = Component.translatable(StrataIndustria.MOD_ID + ".ore." + find.ore());
        if (find.grade() < 0) return Component.translatable(StrataIndustria.MOD_ID + ".ore_scanner.find_ungraded", ore, find.count(), find.minY(), find.maxY());
        Component grade = Component.translatable(StrataIndustria.MOD_ID + ".ore_scanner.grade." + OreGrade.values()[find.grade()].getSerializedName());
        return Component.translatable(StrataIndustria.MOD_ID + ".ore_scanner.find", ore, find.count(), grade, find.minY(), find.maxY());
    }

    private static void outline(GuiGraphicsExtractor g, int x, int y, int size, int colour, int thick) {
        g.fill(x, y, x + size, y + thick, colour);
        g.fill(x, y + size - thick, x + size, y + size, colour);
        g.fill(x, y, x + thick, y + size, colour);
        g.fill(x + size - thick, y, x + size, y + size, colour);
    }

    private int tileAt(double mouseX, double mouseY) {
        double x = mouseX - left - MAP_X, y = mouseY - top - MAP_Y;
        if (x < 0 || y < 0 || x >= MAP || y >= MAP) return -1;
        return (int) (y / TILE) * OreScan.SIDE + (int) (x / TILE);
    }

    private int rowAt(double mouseX, double mouseY) {
        double x = mouseX - left - LIST_X, y = mouseY - top - LIST_Y;
        if (x < -2 || x >= WIDTH - LIST_X - 8 || y < 0) return -1;
        int row = scroll + (int) (y / ROW_H);
        return (int) (y / ROW_H) < ROWS && row < ores.size() ? row : -1;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            int tab = tabAt(event.x(), event.y());
            if (tab >= 0) {
                seismicTab = tab == 1;
                return true;
            }
            if (seismicTab) {
                int hit = seismicRowAt(event.x(), event.y());
                if (hit >= 0) {
                    selectedHit = hit == selectedHit ? -1 : hit;
                    return true;
                }
                return super.mouseClicked(event, doubleClick);
            }
            int row = rowAt(event.x(), event.y());
            if (row >= 0) {
                String ore = ores.get(row).getKey();
                selected = ore.equals(selected) ? null : ore;
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (seismicTab) {
            int most = survey == null ? 0 : Math.max(0, survey.hits().size() - SEISMIC_ROWS / SEISMIC_LINES);
            seismicScroll = Math.clamp(seismicScroll - (int) Math.signum(scrollY), 0, most);
            return true;
        }
        scroll = Math.clamp(scroll - (int) Math.signum(scrollY), 0, Math.max(0, ores.size() - ROWS));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
