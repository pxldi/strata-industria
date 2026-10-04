package dev.strataindustria.client.rail;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Where each part of the electric tram sits on its 256 x 128 sheet. The model and the texture generator both read
 * this table, so a part's net is painted exactly where the model looks for it. A part's net is {@code 2 (dx + dz)}
 * wide and {@code dz + dy} tall, laid out as the model system lays out a box. No Minecraft classes in here.
 */
public final class TramAtlas {
    public static final int WIDTH = 256, HEIGHT = 128;

    /** One part: its size in pixels and where its net starts. */
    public record Cell(String name, int dx, int dy, int dz, int u, int v) {
        public int width() {
            return 2 * (dx + dz);
        }

        public int height() {
            return dz + dy;
        }
    }

    private record Size(String name, int dx, int dy, int dz) {}

    private static final Size[] SIZES = {
            new Size("body_low", 32, 6, 18), new Size("body_up", 32, 6, 18), new Size("roof", 34, 2, 20), new Size("frame", 32, 2, 16),
            new Size("bumper", 1, 2, 16), new Size("vent", 6, 2, 6), new Size("lamp", 1, 3, 4), new Size("wheel", 4, 4, 1),
            new Size("pole_base", 3, 2, 3), new Size("pole", 2, 72, 2), new Size("shoe", 4, 1, 3), new Size("bell", 3, 3, 3),
            new Size("hitch", 4, 1, 1),
    };

    private static final Map<String, Cell> CELLS = pack();

    private TramAtlas() {}

    public static Cell cell(String name) {
        Cell cell = CELLS.get(name);
        if (cell == null) throw new IllegalArgumentException("No tram part " + name);
        return cell;
    }

    public static List<Cell> cells() {
        return new ArrayList<>(CELLS.values());
    }

    /** Shelf packing: tallest first, left to right, a new shelf when the row is full. */
    private static Map<String, Cell> pack() {
        List<Size> sorted = new ArrayList<>(List.of(SIZES));
        sorted.sort((a, b) -> Integer.compare(b.dz() + b.dy(), a.dz() + a.dy()));
        Map<String, Cell> cells = new LinkedHashMap<>();
        int x = 0, y = 0, shelf = 0;
        for (Size size : sorted) {
            int w = 2 * (size.dx() + size.dz()), h = size.dz() + size.dy();
            if (x + w > WIDTH) {
                x = 0;
                y += shelf;
                shelf = 0;
            }
            if (y + h > HEIGHT) throw new IllegalStateException("The tram sheet is full at " + size.name());
            cells.put(size.name(), new Cell(size.name(), size.dx(), size.dy(), size.dz(), x, y));
            x += w;
            shelf = Math.max(shelf, h);
        }
        return cells;
    }
}
