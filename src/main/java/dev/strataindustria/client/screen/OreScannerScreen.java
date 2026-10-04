package dev.strataindustria.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.geology.OreGrade;
import dev.strataindustria.prospecting.OreScan;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * The ore scanner's map (spec 13.2): the 3 x 3 chunks as tiles in the vanilla map colours, and beside them every ore
 * found with its count. Click an ore to outline the chunks that hold it; hover a chunk or an ore for the details.
 */
public class OreScannerScreen extends Screen {
    private static final int PIXEL = 3, TILE = 16 * PIXEL, MAP = TILE * OreScan.SIDE;
    private static final int WIDTH = 270, HEIGHT = 176;
    private static final int MAP_X = 12, MAP_Y = 24, LIST_X = MAP_X + MAP + 14, LIST_Y = 24, ROW_H = 11, ROWS = 12;
    private static final int PANEL = 0xFF14201a, EDGE = 0xFF3a4a40, TEXT = 0xFFb8e6a8, HEADING = 0xFF8ad66a, FAINT = 0xFF6a8a62, MARK = 0xFFf2c45a;

    private final OreScan scan;
    private final List<Map.Entry<String, Integer>> ores;
    private int left, top, scroll;
    private String selected;

    public OreScannerScreen(OreScan scan) {
        super(Component.translatable("item." + StrataIndustria.MOD_ID + ".ore_scanner"));
        this.scan = scan;
        this.ores = new ArrayList<>(scan.totals().entrySet());
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
        g.text(font, title, left + MAP_X, top + 8, HEADING, false);
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

    private void detail(GuiGraphicsExtractor g, int mouseX, int mouseY) {
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
        scroll = Math.clamp(scroll - (int) Math.signum(scrollY), 0, Math.max(0, ores.size() - ROWS));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
