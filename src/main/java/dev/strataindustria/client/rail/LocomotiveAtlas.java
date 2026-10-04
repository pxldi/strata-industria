package dev.strataindustria.client.rail;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Where each part of the steam locomotive sits on its 128 x 64 sheet. The model and the texture generator both read
 * this table, so a part's net is painted exactly where the model looks for it. A part's net is {@code 2 (dx + dz)}
 * wide and {@code dz + dy} tall, laid out as the model system lays out a box. No Minecraft classes in here.
 */
public final class LocomotiveAtlas {
    public static final int WIDTH = 128, HEIGHT = 64;

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
            new Size("back_wall", 1, 16, 10), new Size("band", 1, 9, 10), new Size("boiler_b", 7, 8, 9), new Size("smokebox", 3, 8, 9),
            new Size("boiler_a", 7, 10, 6), new Size("beam", 1, 3, 12), new Size("roof", 11, 1, 12), new Size("frame", 18, 2, 10),
            new Size("side_panel", 7, 8, 1), new Size("tank", 7, 6, 3), new Size("dome", 3, 3, 3), new Size("chimney", 2, 5, 2),
            new Size("chimney_cap", 3, 1, 3), new Size("post", 1, 8, 1), new Size("wheel", 4, 4, 1), new Size("rod", 8, 1, 1),
            new Size("hitch", 4, 1, 1), new Size("buffer", 1, 2, 2), new Size("lamp", 1, 2, 2), new Size("glass", 1, 5, 1),
            new Size("needle", 3, 1, 1), new Size("dome_cap", 1, 1, 1),
    };

    private static final Map<String, Cell> CELLS = pack();

    private LocomotiveAtlas() {}

    public static Cell cell(String name) {
        Cell cell = CELLS.get(name);
        if (cell == null) throw new IllegalArgumentException("No locomotive part " + name);
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
            if (y + h > HEIGHT) throw new IllegalStateException("The locomotive sheet is full at " + size.name());
            cells.put(size.name(), new Cell(size.name(), size.dx(), size.dy(), size.dz(), x, y));
            x += w;
            shelf = Math.max(shelf, h);
        }
        return cells;
    }
}
