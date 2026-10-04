import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;

/**
 * Textures for pattern casting: pattern blank, carved patterns, sand flask and sand molds, plus the carving
 * grid surface. Reuses the helpers and ramps of {@link TextureGen}; it does not call its {@code main}.
 *
 * <p>Run from the repository root:
 * <pre>
 * mkdir -p build/tg &amp;&amp; javac -d build/tg tools/texturegen/TextureGen.java tools/texturegen/PatternTextures.java &amp;&amp; java -cp build/tg PatternTextures
 * </pre>
 */
public final class PatternTextures {
    static final TextureGen.Ramp WOOD = TextureGen.V2.WOOD_V2;
    static final TextureGen.Ramp SAND = TextureGen.SAND;
    static final int LEFT = 2, TOP = 3, W = 12, H = 9, CX = 4, CY = 5;

    static final String[] INGOT = {"........", "........", "########", "########", "########", "........"};

    static boolean in(String[] cavity, int x, int y) {
        return y >= 0 && y < cavity.length && x >= 0 && x < cavity[y].length() && cavity[y].charAt(x) == '#';
    }

    static void board(BufferedImage im, TextureGen.Ramp a, int base) {
        for (int y = 0; y < H; y++)
            for (int x = 0; x < W; x++) {
                int step = base;
                if (y == 0 || x == 0) step = base + 1;
                else if (x == W - 1) step = base - 1;
                TextureGen.px(im, LEFT + x, TOP + y, a.get(step));
            }
        for (int x = 0; x < W; x++) {
            TextureGen.px(im, LEFT + x, TOP + H, a.get(x == W - 1 ? 2 : 3));
            TextureGen.px(im, LEFT + x, TOP + H + 1, a.get(2));
        }
    }

    /** Plank grain: a darker run every few pixels along the board. */
    static void grain(BufferedImage im, long seed) {
        java.util.Random r = new java.util.Random(seed);
        for (int k = 0; k < 6; k++) {
            int y = 1 + r.nextInt(H - 2), x = 1 + r.nextInt(W - 5), len = 3 + r.nextInt(4);
            for (int i = 0; i < len && x + i < W - 1; i++) TextureGen.px(im, LEFT + x + i, TOP + y, WOOD.get(1));
        }
    }

    static BufferedImage blank() {
        BufferedImage im = TextureGen.img();
        board(im, WOOD, 3);
        grain(im, 4401);
        return TextureGen.outline(im);
    }

    /** The shape stands proud of the board: lit top-left, shaded bottom-right. */
    static BufferedImage pattern(String[] cavity, long seed) {
        BufferedImage im = TextureGen.img();
        board(im, WOOD, 2);
        grain(im, seed);
        for (int y = 0; y < cavity.length; y++)
            for (int x = 0; x < cavity[y].length(); x++) {
                if (!in(cavity, x, y)) continue;
                int step = 4;
                if (!in(cavity, x, y - 1) || !in(cavity, x - 1, y)) step = 5;
                else if (!in(cavity, x, y + 1) || !in(cavity, x + 1, y)) step = 3;
                TextureGen.px(im, CX + x, CY + y, WOOD.get(step));
            }
        return TextureGen.outline(im);
    }

    /** A wooden rim round sand; the pressed shape is sunk into the sand. */
    static BufferedImage sandMold(String[] cavity, boolean filled) {
        BufferedImage im = TextureGen.img();
        board(im, WOOD, 3);
        for (int y = 1; y < H - 1; y++)
            for (int x = 1; x < W - 1; x++) {
                int step = ((x * 7 + y * 13) % 5 == 0) ? 3 : 4;
                if (y == 1 || x == 1) step = 3;
                TextureGen.px(im, LEFT + x, TOP + y, SAND.get(step));
            }
        if (cavity != null) {
            for (int y = 0; y < cavity.length; y++)
                for (int x = 0; x < cavity[y].length(); x++) {
                    if (!in(cavity, x, y)) continue;
                    int step = 2;
                    if (!in(cavity, x, y - 1) || !in(cavity, x - 1, y)) step = 1;
                    else if (!in(cavity, x, y + 1) || !in(cavity, x + 1, y)) step = 3;
                    int c = SAND.get(step);
                    if (filled) {
                        step = 4;
                        if (!in(cavity, x, y - 1) || !in(cavity, x - 1, y)) step = 3;
                        else if (!in(cavity, x, y + 1) || !in(cavity, x + 1, y)) step = 5;
                        c = TextureGen.CAST_METAL.get(step);
                    }
                    TextureGen.px(im, CX + x, CY + y, c);
                }
        }
        return TextureGen.outline(im);
    }

    static BufferedImage flask() {
        return sandMold(null, false);
    }

    /** Smooth planed wood for the carving grid, with a few long grain lines. */
    static BufferedImage gridSurface() {
        BufferedImage im = TextureGen.img();
        java.util.Random r = new java.util.Random(4411);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) TextureGen.px(im, x, y, WOOD.get(y % 5 == 2 ? 3 : 4));
        for (int k = 0; k < 4; k++) {
            int y = r.nextInt(16), x = r.nextInt(16), len = 4 + r.nextInt(6);
            for (int i = 0; i < len; i++) TextureGen.px(im, (x + i) % 16, y, WOOD.get(3));
        }
        return im;
    }

    public static void main(String[] args) throws IOException {
        TextureGen.itemsV2 = true;
        TextureGen.save("item/pattern_blank", blank());
        TextureGen.save("item/sand_flask", flask());
        Map<String, String[]> shapes = new java.util.LinkedHashMap<>();
        shapes.put("ingot", INGOT);
        shapes.put("gear", TextureGen.GEAR_CAVITY);
        shapes.putAll(TextureGen.MOLD_CAVITIES);
        long seed = 4420;
        for (var shape : shapes.entrySet()) {
            String id = shape.getKey().equals("ingot") || shape.getKey().equals("gear") ? shape.getKey() : shape.getKey();
            TextureGen.save("item/" + id + "_pattern", pattern(shape.getValue(), seed++));
            TextureGen.save("item/" + id + "_sand_mold", sandMold(shape.getValue(), false));
            TextureGen.save("item/" + id + "_sand_mold_filled", sandMold(shape.getValue(), true));
        }
        TextureGen.itemsV2 = false;
        TextureGen.saveRaw("gui/knapping/wood", gridSurface());
    }

    private PatternTextures() {}
}
