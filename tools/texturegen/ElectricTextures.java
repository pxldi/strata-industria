import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import javax.imageio.ImageIO;

/**
 * Tier 5 electric textures: LV/MV casings, battery boxes, cables, kinetic dynamo, copper/lead/rubber items,
 * electric furnace and macerator fronts, status overlays and electric machine GUIs and sprites.
 * Reuses the helpers of {@link TextureGen}; it does not call its {@code main}.
 *
 * <p>Run from the repository root:
 * <pre>
 * mkdir -p build/tg &amp;&amp; javac -d build/tg tools/texturegen/TextureGen.java tools/texturegen/ElectricTextures.java &amp;&amp; java -cp build/tg ElectricTextures
 * </pre>
 * A 6x preview sheet is written to build/texturegen/electric.png.
 */
public final class ElectricTextures {
    static final TextureGen.Ramp ALUMINIUM = TextureGen.ramp(0xf2f6f8, 0x3a4048, 0x5c646e, 0x8a929c, 0xb4bcc4, 0xd8dee4);
    static final TextureGen.Ramp RUBBER = TextureGen.ramp(0, 0x141212, 0x211d1c, 0x302a28, 0x423a36, 0x564c46);
    static final TextureGen.Ramp RAW_RUBBER = TextureGen.ramp(0, 0x3e2a14, 0x5c4020, 0x7c5a2e, 0x9c7840, 0xba9858);
    static final TextureGen.Ramp STEEL = TextureGen.STEEL;
    static final TextureGen.Ramp COPPER = TextureGen.COPPER;
    static final TextureGen.Ramp LEAD = TextureGen.LEAD;
    static final TextureGen.Ramp IRON = TextureGen.WROUGHT_IRON;
    static final TextureGen.Ramp BRASS = TextureGen.BRASS;
    static final TextureGen.Ramp SULFUR = TextureGen.SULFUR;
    static final TextureGen.Ramp MAGNETITE = TextureGen.MAGNETITE;

    // Status lamp ramps, 4 steps each (reserved for lamp pixels on machine faces).
    static final int[] RUN = {0x1e4a26, 0x2e7a36, 0x4fae4a, 0x8ad66a};
    static final int[] WAIT = {0x5a3a10, 0x9a6a1a, 0xd89a2a, 0xf2c45a};
    static final int[] ERROR = {0x4a1414, 0x8a2020, 0xc83a2a, 0xee6a4a};

    static final Map<String, BufferedImage> OUTS = new LinkedHashMap<>();

    static void save(String path, BufferedImage im) throws IOException {
        TextureGen.save(path, im);
        OUTS.put(path, im);
    }

    static int c(TextureGen.Ramp r, int step) { return r.get(Math.max(1, Math.min(5, step))); }

    static void fill(BufferedImage im, int x0, int y0, int x1, int y1, int col) {
        for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) TextureGen.px(im, x, y, col);
    }

    // ---------------------------------------------------------------- casings

    /** Plate with brushed streaks, edge bevel, a seam at 2..13 and four corner rivets. */
    static BufferedImage casing(TextureGen.Ramp r, int base, long seed) {
        BufferedImage im = TextureGen.img();
        fill(im, 0, 0, 15, 15, c(r, base));
        Random rnd = new Random(seed);
        for (int i = 0; i < 6; i++) {
            int len = 3 + rnd.nextInt(3), x = 2 + rnd.nextInt(9), y = 2 + rnd.nextInt(12);
            for (int k = 0; k < len; k++) TextureGen.px(im, x + k, y, c(r, base + 1));
        }
        for (int i = 0; i < 3; i++) {
            int len = 3 + rnd.nextInt(2), x = 2 + rnd.nextInt(9), y = 2 + rnd.nextInt(12);
            for (int k = 0; k < len; k++) TextureGen.px(im, x + k, y, c(r, base - 1));
        }
        for (int i = 0; i < 16; i++) {
            TextureGen.px(im, i, 0, c(r, base + 1));
            TextureGen.px(im, 0, i, c(r, base + 1));
            TextureGen.px(im, i, 15, c(r, base - 1));
            TextureGen.px(im, 15, i, c(r, base - 1));
        }
        seam(im, r, base);
        rivets(im, r, base, new int[][]{{3, 3}, {11, 3}, {3, 11}, {11, 11}});
        return im;
    }

    static void seam(BufferedImage im, TextureGen.Ramp r, int base) {
        for (int i = 2; i <= 13; i++) {
            TextureGen.px(im, i, 2, c(r, base - 2));
            TextureGen.px(im, 2, i, c(r, base - 2));
            TextureGen.px(im, i, 13, c(r, base + 1));
            TextureGen.px(im, 13, i, c(r, base + 1));
        }
        TextureGen.px(im, 13, 2, c(r, base - 1));
        TextureGen.px(im, 2, 13, c(r, base - 1));
    }

    static void rivets(BufferedImage im, TextureGen.Ramp r, int base, int[][] at) {
        for (int i = 0; i < at.length; i++) {
            int lit = (i == 0 && r.spec() != 0) ? r.spec() : c(r, base + 2);
            TextureGen.px(im, at[i][0], at[i][1], lit);
            TextureGen.px(im, at[i][0] + 1, at[i][1] + 1, c(r, base - 2));
        }
    }

    static void stripe(BufferedImage im, int y, int step) {
        for (int x = 0; x < 16; x++) TextureGen.px(im, x, y, c(COPPER, step));
    }

    static void casings() throws IOException {
        BufferedImage s = casing(STEEL, 3, 51);
        stripe(s, 14, 3);
        save("block/casing/lv_side", s);
        save("block/casing/lv_top", casing(STEEL, 3, 52));
        save("block/casing/lv_bottom", casing(STEEL, 2, 53));
        save("block/casing/mv_side", mvSide());
        save("block/casing/mv_top", casing(ALUMINIUM, 4, 62));
        save("block/casing/mv_bottom", casing(ALUMINIUM, 3, 63));

        BufferedImage h = casing(STEEL, 3, 51);
        fill(h, 5, 5, 10, 10, c(STEEL, 2));
        for (int i = 5; i <= 10; i++) {
            TextureGen.px(h, i, 5, c(STEEL, 1));
            TextureGen.px(h, 5, i, c(STEEL, 1));
            TextureGen.px(h, i, 10, c(STEEL, 4));
            TextureGen.px(h, 10, i, c(STEEL, 4));
        }
        TextureGen.px(h, 5, 10, c(STEEL, 2));
        TextureGen.px(h, 10, 5, c(STEEL, 2));
        fill(h, 6, 6, 9, 9, c(STEEL, 2));
        // screw heads at the panel corners
        for (int[] p : new int[][]{{6, 6}, {9, 6}, {6, 9}, {9, 9}}) TextureGen.px(h, p[0], p[1], c(STEEL, 4));
        save("block/lv_machine_hull_front", h);
    }

    static BufferedImage mvSide() {
        BufferedImage m = casing(ALUMINIUM, 4, 61);
        for (int x = 0; x < 16; x++) {
            TextureGen.px(m, x, 13, c(COPPER, 4));
            TextureGen.px(m, x, 15, c(COPPER, 3));
        }
        for (int x = 0; x < 16; x++) TextureGen.px(m, x, 14, c(ALUMINIUM, 5));
        return m;
    }

    // ---------------------------------------------------------------- battery boxes

    static BufferedImage battery(TextureGen.Ramp r, int base, int lit) {
        BufferedImage im = TextureGen.img();
        fill(im, 0, 0, 15, 15, c(r, base));
        Random rnd = new Random(r == STEEL ? 71 : 72);
        for (int i = 0; i < 6; i++) {
            int len = 3 + rnd.nextInt(2), x = 1 + rnd.nextInt(10), y = 15 - 1 - rnd.nextInt(14);
            for (int k = 0; k < len; k++) TextureGen.px(im, x + k, y, c(r, base + 1));
        }
        for (int i = 0; i < 16; i++) {
            TextureGen.px(im, i, 0, c(r, base + 1));
            TextureGen.px(im, 0, i, c(r, base + 1));
            TextureGen.px(im, i, 15, c(r, base - 1));
            TextureGen.px(im, 15, i, c(r, base - 1));
        }
        // grille recess with lead cells
        fill(im, 3, 3, 9, 13, c(LEAD, 1));
        for (int i = 2; i <= 10; i++) {
            TextureGen.px(im, i, 2, c(r, base - 2));
            TextureGen.px(im, i, 14, c(r, base + 1));
        }
        for (int i = 2; i <= 14; i++) {
            TextureGen.px(im, 2, i, c(r, base - 2));
            TextureGen.px(im, 10, i, c(r, base + 1));
        }
        for (int cy : new int[]{5, 10}) {
            fill(im, 4, cy, 8, cy, c(LEAD, 4));
            fill(im, 4, cy + 1, 8, cy + 1, c(LEAD, 3));
            fill(im, 4, cy + 2, 8, cy + 2, c(LEAD, 2));
            TextureGen.px(im, 5, cy - 1, c(STEEL, 5));
            TextureGen.px(im, 7, cy - 1, c(STEEL, 4));
        }
        for (int y : new int[]{3, 8, 13}) fill(im, 3, y, 9, y, c(r, base + 1));
        for (int y : new int[]{3, 8, 13}) TextureGen.px(im, 3, y, c(r, base + 2));
        // charge meter bezel and segments
        fill(im, 11, 3, 14, 13, c(STEEL, 2));
        for (int i = 0; i < 5; i++) {
            int y = 12 - 2 * i;
            boolean on = i < lit;
            if (on) {
                TextureGen.px(im, 12, y, RUN[3]);
                TextureGen.px(im, 13, y, RUN[2]);
            } else {
                TextureGen.px(im, 12, y, c(STEEL, 1));
                TextureGen.px(im, 13, y, c(STEEL, 1));
            }
        }
        rivets(im, r, base, new int[][]{{1, 1}, {14, 1}, {1, 14}, {14, 14}});
        return im;
    }

    static void batteries() throws IOException {
        for (int n = 0; n <= 5; n++) {
            save("block/battery_box_front_lv_" + n, battery(STEEL, 3, n));
            save("block/battery_box_front_mv_" + n, battery(ALUMINIUM, 4, n));
        }
    }

    // ---------------------------------------------------------------- cables

    static BufferedImage cable(boolean mv) {
        BufferedImage im = TextureGen.img();
        fill(im, 0, 0, 15, 15, c(RUBBER, 3));
        for (int y = 0; y < 16; y++) {
            TextureGen.px(im, 3, y, c(RUBBER, 4));
            TextureGen.px(im, 4, y, c(RUBBER, 4));
            TextureGen.px(im, 5, y, c(RUBBER, 4));
            TextureGen.px(im, 6, y, c(RUBBER, 5));
            for (int x = 10; x <= 15; x++) TextureGen.px(im, x, y, c(RUBBER, 2));
            TextureGen.px(im, 12, y, c(RUBBER, 1));
        }
        for (int o = 0; o < 16; o += 8) {
            if (!mv) {
                stripeCable(im, o + 3, 4);
                stripeCable(im, o + 4, 3);
            } else {
                stripeCable(im, o + 2, 4);
                stripeCable(im, o + 3, 3);
                stripeCable(im, o + 4, 4);
                stripeCable(im, o + 5, 3);
            }
        }
        return im;
    }

    static void stripeCable(BufferedImage im, int y, int step) {
        for (int x = 0; x < 16; x++) {
            int step2 = x <= 6 ? step : (x >= 10 ? step - 1 : step);
            TextureGen.px(im, x, y, c(COPPER, step2));
        }
    }

    static BufferedImage cableEnd(boolean mv) {
        BufferedImage im = TextureGen.img();
        fill(im, 0, 0, 15, 15, c(RUBBER, 2));
        double ring = mv ? 4.6 : 3.6, core = mv ? 3.0 : 2.0;
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                double dx = x - 7.5, dy = y - 7.5, d = Math.hypot(dx, dy);
                if (d <= ring) TextureGen.px(im, x, y, c(RUBBER, (dx + dy < 0) ? 4 : 3));
                if (d <= core + 0.9) TextureGen.px(im, x, y, c(RUBBER, 1));
                if (d <= core) {
                    int step = (dx + dy < -1) ? 4 : (dx + dy > 1 ? 2 : 3);
                    TextureGen.px(im, x, y, c(COPPER, step));
                }
            }
        }
        // strand gaps
        if (mv) {
            int[][] g = {{7, 6}, {7, 7}, {8, 8}, {8, 9}, {6, 8}, {9, 7}};
            for (int[] p : g) TextureGen.px(im, p[0], p[1], c(COPPER, 2));
            TextureGen.px(im, 6, 6, c(COPPER, 5));
        } else {
            TextureGen.px(im, 7, 7, c(COPPER, 2));
            TextureGen.px(im, 8, 8, c(COPPER, 2));
            TextureGen.px(im, 6, 7, c(COPPER, 5));
        }
        return im;
    }

    static void cables() throws IOException {
        save("block/lv_cable", cable(false));
        save("block/mv_cable", cable(true));
        save("block/lv_cable_end", cableEnd(false));
        save("block/mv_cable_end", cableEnd(true));
    }

    // ---------------------------------------------------------------- kinetic dynamo

    static BufferedImage ironCasing(long seed) { return casing(IRON, 3, seed); }

    static BufferedImage dynamoInner() {
        BufferedImage im = TextureGen.img();
        fill(im, 0, 0, 15, 15, c(IRON, 1));
        Random rnd = new Random(91);
        for (int i = 0; i < 5; i++) {
            int len = 3 + rnd.nextInt(3), x = rnd.nextInt(11), y = rnd.nextInt(16);
            for (int k = 0; k < len; k++) TextureGen.px(im, x + k, y, c(IRON, 2));
        }
        for (int y = 4; y <= 11; y++) {
            int[] steps = {4, 3, 2, 1};
            int s = steps[(y - 4) % 4];
            for (int x : new int[]{4, 5, 10, 11}) {
                int st = (x == 4 || x == 10) ? s : Math.max(1, s - 1);
                TextureGen.px(im, x, y, c(COPPER, st));
            }
        }
        for (int y = 5; y <= 10; y++) {
            TextureGen.px(im, 6, y, c(STEEL, (y == 5 || y == 10) ? 3 : 4));
            TextureGen.px(im, 9, y, c(STEEL, (y == 5 || y == 10) ? 2 : 3));
        }
        TextureGen.px(im, 6, 5, c(STEEL, 3));
        for (int y = 4; y <= 11; y++) {
            TextureGen.px(im, 7, y, c(IRON, 1));
            TextureGen.px(im, 8, y, c(IRON, 1));
        }
        return im;
    }

    static BufferedImage dynamoFront(int[] lamp) {
        BufferedImage im = TextureGen.img();
        fill(im, 0, 0, 15, 15, c(IRON, 3));
        Random rnd = new Random(92);
        for (int i = 0; i < 6; i++) {
            int len = 3 + rnd.nextInt(3), x = rnd.nextInt(11), y = rnd.nextInt(16);
            for (int k = 0; k < len; k++) TextureGen.px(im, x + k, y, c(IRON, 4));
        }
        for (int i = 0; i < 16; i++) {
            TextureGen.px(im, i, 0, c(IRON, 4));
            TextureGen.px(im, 0, i, c(IRON, 4));
            TextureGen.px(im, i, 15, c(IRON, 2));
            TextureGen.px(im, 15, i, c(IRON, 2));
        }
        // bevelled lip around the window (window is 4..11)
        for (int i = 3; i <= 12; i++) {
            TextureGen.px(im, i, 3, c(IRON, 2));
            TextureGen.px(im, 3, i, c(IRON, 2));
            TextureGen.px(im, i, 12, c(IRON, 5));
            TextureGen.px(im, 12, i, c(IRON, 5));
        }
        TextureGen.px(im, 3, 12, c(IRON, 3));
        TextureGen.px(im, 12, 3, c(IRON, 3));
        // nameplate
        fill(im, 5, 13, 10, 13, c(BRASS, 4));
        fill(im, 5, 14, 10, 14, c(BRASS, 3));
        TextureGen.px(im, 5, 13, c(BRASS, 5));
        for (int x = 6; x <= 9; x++) TextureGen.px(im, x, 14, c(BRASS, 2));
        rivets(im, IRON, 3, new int[][]{{1, 1}, {1, 13}, {13, 13}});
        // lamp socket and lamp
        fill(im, 11, 0, 14, 3, c(IRON, 1));
        TextureGen.px(im, 12, 1, lamp[3]);
        TextureGen.px(im, 13, 1, lamp[2]);
        TextureGen.px(im, 12, 2, lamp[2]);
        TextureGen.px(im, 13, 2, lamp[1]);
        // window hole filled with the inner colour
        BufferedImage inner = dynamoInner();
        for (int y = 4; y <= 11; y++) for (int x = 4; x <= 11; x++) im.setRGB(x, y, inner.getRGB(x, y));
        return im;
    }

    static BufferedImage dynamoBack() {
        BufferedImage im = ironCasing(95);
        for (int i = 5; i <= 10; i++) {
            TextureGen.px(im, i, 5, c(IRON, 5));
            TextureGen.px(im, 5, i, c(IRON, 5));
            TextureGen.px(im, i, 10, c(IRON, 2));
            TextureGen.px(im, 10, i, c(IRON, 2));
        }
        for (int[] p : new int[][]{{5, 5}, {10, 5}, {5, 10}, {10, 10}}) TextureGen.px(im, p[0], p[1], c(IRON, 3));
        fill(im, 6, 6, 9, 9, c(IRON, 1));
        TextureGen.px(im, 9, 8, c(IRON, 2));
        TextureGen.px(im, 8, 9, c(IRON, 2));
        TextureGen.px(im, 9, 9, c(IRON, 3));
        return im;
    }

    static BufferedImage dynamoSide() {
        BufferedImage im = ironCasing(94);
        for (int x = 5; x <= 10; x++) {
            TextureGen.px(im, x, 6, c(IRON, 1));
            TextureGen.px(im, x, 7, c(IRON, 1));
            TextureGen.px(im, x, 8, c(IRON, 4));
        }
        TextureGen.px(im, 5, 6, c(IRON, 2));
        TextureGen.px(im, 5, 7, c(IRON, 2));
        TextureGen.px(im, 5, 8, c(IRON, 3));
        return im;
    }

    static BufferedImage armature() {
        BufferedImage im = TextureGen.img();
        int[] steps = {4, 3, 2, 1};
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int s = steps[y % 4];
                if (s > 1 && (x == 0 || x == 1)) s = Math.min(5, s + 1) == 5 ? 4 : s + 1;
                if (s > 1 && x >= 14) s = s - 1;
                TextureGen.px(im, x, y, c(COPPER, s));
            }
        }
        // winding crossings: slanted dark ticks on the lit rows
        for (int y = 0; y < 16; y += 4) for (int x = 3 + (y / 4) % 2 * 2; x < 15; x += 4) TextureGen.px(im, x, y + 1, c(COPPER, 2));
        TextureGen.px(im, 4, 4, c(COPPER, 5));
        return im;
    }

    static BufferedImage armatureEnd() {
        BufferedImage im = TextureGen.img();
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int dx = 2 * x - 15, dy = 2 * y - 15;
                int k = (Math.max(Math.abs(dx), Math.abs(dy)) + 1) / 2; // 1..8
                int col;
                if (k <= 2) {
                    if (k == 1) col = c(STEEL, 1);
                    else col = c(STEEL, (dx < 0 || dy < 0) ? (dx < 0 && dy < 0 ? 5 : 4) : 3);
                    if (k == 2 && dx > 0 && dy > 0) col = c(STEEL, 2);
                } else if (k == 3) {
                    col = c(COPPER, 1);
                } else {
                    int s = (k % 2 == 0) ? 3 : 2;
                    boolean litSide = (dy < 0 && Math.abs(dy) >= Math.abs(dx)) || (dx < 0 && Math.abs(dx) >= Math.abs(dy));
                    if (litSide) s++;
                    if (k == 8) s = 1;
                    col = c(COPPER, s);
                }
                TextureGen.px(im, x, y, col);
            }
        }
        return im;
    }

    static void dynamo() throws IOException {
        save("block/kinetic_dynamo_side", dynamoSide());
        save("block/kinetic_dynamo_back", dynamoBack());
        save("block/kinetic_dynamo_inner", dynamoInner());
        int[] off = {c(STEEL, 1), c(STEEL, 1), c(STEEL, 1), c(STEEL, 2)};
        save("block/kinetic_dynamo_front_off", dynamoFront(off));
        save("block/kinetic_dynamo_front_run", dynamoFront(RUN));
        save("block/kinetic_dynamo_front_wait", dynamoFront(WAIT));
        save("block/kinetic_dynamo_front_error", dynamoFront(ERROR));
        save("block/kinetic_dynamo_armature", armature());
        save("block/kinetic_dynamo_armature_end", armatureEnd());
    }

    // ---------------------------------------------------------------- items

    /** Coiled loop of wire with a free end at the top right; shared by every wire form. */
    static final String[] WIRE = buildWire();

    static String[] buildWire() {
        int n = 12;
        char[][] g = new char[n][n];
        for (char[] row : g) java.util.Arrays.fill(row, '.');
        double cx = 4.5, cy = 7.0;
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                double dx = x - cx, dy = y - cy, d = Math.hypot(dx, dy);
                if (d >= 1.7 && d <= 4.9) {
                    double lit = -(dx + dy) / (d * 1.414); // 1 at top-left
                    char ch = lit > 0.5 ? '5' : lit > 0.0 ? '4' : lit > -0.5 ? '3' : '2';
                    if (d > 3.1 && d < 3.7) ch = '1'; // groove between the two windings
                    g[y][x] = ch;
                }
            }
        }
        // free end rising to the top right, 2 px thick
        String[] tail = {"..........45", ".........453", "........453.", ".......43..."};
        for (int t = 0; t < tail.length; t++)
            for (int x = 0; x < n; x++) if (tail[t].charAt(x) != '.') g[t][x] = tail[t].charAt(x);
        g[1][9] = 's';
        String[] out = new String[n];
        for (int i = 0; i < n; i++) out[i] = new String(g[i]);
        return out;
    }

    static final String[] MAGNET = {
            "cdd....cdd",
            "bcc....bcc",
            "bcc....bcc",
            "453....453",
            "4s3....453",
            "443....332",
            "4433..3322",
            ".44333322.",
            "..222211..",
    };

    static final String[] SHEET = {
            "...2233333322.",
            "..234444443332",
            ".2344455544432",
            "23444555554432",
            "23444555555432",
            ".2344455554432",
            "..234444444322",
            "...2233443221.",
            ".....11222....",
    };

    static final String[] ROLL = {
            "...44444444444...",
            "..4333333333332..",
            ".43333333333332..",
            "4432..",
    };

    static final String[] ROLLED = {
            "..44444444444.",
            ".4555555555543",
            "45333333333332",
            "43322222222221",
            "4322.2222222221",
    };

    static String[] withSpecks(String[] rows, int[][] at) {
        String[] out = rows.clone();
        for (int i = 0; i < at.length; i++) {
            char[] row = out[at[i][1]].toCharArray();
            row[at[i][0]] = (i % 2 == 0) ? 'e' : 'd';
            out[at[i][1]] = new String(row);
        }
        return out;
    }

    static void items() throws IOException {
        save("item/copper_rod", TextureGen.map(COPPER, TextureGen.ROD));
        save("item/copper_wire", TextureGen.map(COPPER, WIRE));
        save("item/lead_plate", TextureGen.map(LEAD, TextureGen.PLATE));
        save("item/magnet", TextureGen.map(STEEL, MAGNETITE, MAGNET));
        save("item/raw_rubber", TextureGen.map(RAW_RUBBER, SHEET));
        TextureGen.Ramp mixed = TextureGen.ramp(0, RAW_RUBBER.get(1), RAW_RUBBER.get(2), RAW_RUBBER.get(3), RUBBER.get(4), RUBBER.get(5));
        save("item/compounded_rubber", TextureGen.map(mixed, SULFUR,
                withSpecks(SHEET, new int[][]{{4, 2}, {9, 3}, {6, 4}, {3, 5}, {10, 5}, {7, 6}})));
        save("item/rubber", TextureGen.map(RUBBER, rolled()));
    }

    /** Horizontal roll of rubber sheet: body, one matte highlight line, spiral end on the left. */
    static String[] rolled() {
        return new String[]{
                "..555555555555..",
                ".45444444444443.",
                "4543333333333332",
                "4533233333333322",
                "4533233333333322",
                "4432222222222211",
                ".32222222222211.",
                "..111111111111..",
        };
    }

    // ---------------------------------------------------------------- rubber (tree tap, latex)

    static final TextureGen.Ramp LATEX = TextureGen.ramp(0, 0x6e6656, 0x8e8670, 0xb0a88c, 0xcec6a8, 0xe6e0c4);

    static void rubber() throws IOException {
        save("block/tree_tap", treeTapAtlas());
        save("block/tree_tap_latex", latexSurface());
        save("item/tree_tap", treeTapItem());
        save("item/latex_bucket", TextureGen.map(IRON, LATEX, TextureGen.CREOSOTE_BUCKET));
        TextureGen.saveAnimated("block/latex_still", latexFluid(false, 7711), 3);
        TextureGen.saveAnimated("block/latex_flow", latexFluid(true, 7711), 2);
        latexGuiStrip();
    }

    /** UV atlas of the tree tap model: copper spout, iron hanger, wooden bowl. */
    static BufferedImage treeTapAtlas() {
        TextureGen.Ramp cu = COPPER, wd = TextureGen.WOOD;
        BufferedImage im = TextureGen.img();
        Random rnd = new Random(8801);
        // copper block x 0..15, y 0..7: light top-left, dark bottom-right, hammer dents in 2 px clusters
        for (int y = 0; y < 8; y++)
            for (int x = 0; x < 16; x++) {
                int s = 3;
                if (y == 0 || x == 0) s = 4;
                if (y == 7 || x == 7 || x == 15) s = 2;
                TextureGen.px(im, x, y, c(cu, s));
            }
        for (int i = 0; i < 9; i++) {
            int x = 2 + rnd.nextInt(12), y = 1 + rnd.nextInt(6);
            TextureGen.px(im, x, y, c(cu, 2));
            TextureGen.px(im, x + 1, y, c(cu, 2));
            TextureGen.px(im, x, y + 1, c(cu, 4));
        }
        // spout mouth 2x2 at 0,0: hollow, dark with a faint lit lower-right
        TextureGen.px(im, 0, 0, c(cu, 1)); TextureGen.px(im, 1, 0, c(cu, 1));
        TextureGen.px(im, 0, 1, c(cu, 1)); TextureGen.px(im, 1, 1, c(cu, 2));
        // spout sides/top continuation x 2..4, y 0..4
        for (int y = 0; y < 5; y++) for (int x = 2; x < 5; x++) TextureGen.px(im, x, y, c(cu, y == 0 || y == 2 && x < 3 ? 4 : 3));
        for (int x = 2; x < 5; x++) TextureGen.px(im, x, 4, c(cu, 2));
        // drip lip x 0..1, y 2..4: lit rolled edge, shadow underneath
        for (int x = 0; x < 2; x++) {
            TextureGen.px(im, x, 2, c(cu, 5)); TextureGen.px(im, x, 3, c(cu, 3)); TextureGen.px(im, x, 4, c(cu, 2));
        }
        // dark hanger strap x 8..10, y 0..3 (overwrites the copper there)
        for (int y = 0; y < 4; y++)
            for (int x = 8; x < 11; x++) TextureGen.px(im, x, y, c(IRON, y == 0 ? 3 : x == 8 ? 3 : 2));
        TextureGen.px(im, 9, 1, c(IRON, 1)); TextureGen.px(im, 9, 3, c(IRON, 1));
        TextureGen.px(im, 10, 0, c(IRON, 2));
        // wood x 0..15, y 8..15
        for (int y = 8; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int s = y == 8 ? 5 : y <= 11 ? 3 : 2;
                if (y == 15) s = 1 + (x % 3 == 0 ? 1 : 0);
                if (y == 9 && s == 3) s = 4;
                TextureGen.px(im, x, y, c(wd, s));
            }
        // inner floor y 10..15 x 1..7: darker inner wood
        for (int y = 10; y < 16; y++) for (int x = 1; x < 8; x++) TextureGen.px(im, x, y, c(wd, y == 10 ? 3 : y == 15 ? 1 : 2));
        // grain streaks, horizontal runs
        for (int i = 0; i < 14; i++) {
            int x = rnd.nextInt(12), y = 9 + rnd.nextInt(6), len = 2 + rnd.nextInt(3);
            for (int k = 0; k < len; k++) {
                int px = x + k, cur = TextureGen.rgb(im, px, y);
                if (y == 15 || y == 8) continue;
                for (int s = 2; s <= 4; s++) if (c(wd, s) == cur) { TextureGen.px(im, px, y, c(wd, s - 1)); break; }
            }
        }
        // side walls x 8..12, y 8..15: lit left edge, shaded right edge
        for (int y = 9; y < 16; y++) {
            TextureGen.px(im, 8, y, c(wd, y < 12 ? 4 : 3));
            TextureGen.px(im, 12, y, c(wd, y < 12 ? 2 : 1));
        }
        return im;
    }

    /** Latex in the bowl seen from above: matte, a soft lighter centre, very low contrast. */
    static BufferedImage latexSurface() {
        BufferedImage im = TextureGen.img();
        double tau = 2 * Math.PI;
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double dx = x + 0.5 - 8, dy = y + 0.5 - 8, d = Math.sqrt(dx * dx + dy * dy);
                double fold = 0.5 + 0.5 * Math.sin(tau * (x + 2 * y) / 16.0 + 1.1 * Math.sin(tau * y / 16.0));
                double v = 1.0 - d / 9.0 + 0.22 * fold;
                int s = v > 0.88 ? 5 : v > 0.42 ? 4 : 3;
                TextureGen.px(im, x, y, c(LATEX, s));
            }
        return im;
    }

    /** Inventory icon: copper spout angled out of a bark sliver, wooden bowl below with latex. */
    static BufferedImage treeTapItem() {
        String[] rows = {
                "................",
                ".cdd............",
                ".bcd............",
                ".bcd4555........",
                ".bbc33444.......",
                ".bcc..2334......",
                ".bcc...3321.....",
                ".bcc......m.....",
                ".bbcp.....n.....",
                ".bbcp.jlmnnmlih.",
                ".bbcppihhhhhhgg.",
                ".bb....ihhhggf..",
                "........hhhggf..",
                "........gfffe...",
                "................",
                "................",
        };
        BufferedImage im = TextureGen.img();
        for (int y = 0; y < rows.length; y++)
            for (int x = 0; x < rows[y].length() && x < 16; x++) {
                char ch = rows[y].charAt(x);
                int col;
                if (ch >= '1' && ch <= '5') col = COPPER.get(ch - '0');
                else if (ch >= 'a' && ch <= 'e') col = TextureGen.BARK.get(ch - 'a' + 1);
                else if (ch >= 'f' && ch <= 'j') col = TextureGen.WOOD.get(ch - 'f' + 1);
                else if (ch >= 'k' && ch <= 'o') col = LATEX.get(ch - 'k' + 1);
                else if (ch == 'p') col = IRON.get(2);
                else continue;
                TextureGen.px(im, x, y, col);
            }
        return TextureGen.outline(im);
    }

    /** Opaque milky cream latex fluid: slow thick folds, low contrast. Same frame layout as the lye strips. */
    static BufferedImage latexFluid(boolean flow, long seed) {
        int frames = flow ? 16 : 32;
        BufferedImage im = new BufferedImage(16, 16 * frames, BufferedImage.TYPE_INT_ARGB);
        Random r = new Random(seed);
        double p1 = r.nextDouble(), p2 = r.nextDouble(), tau = 2 * Math.PI;
        for (int f = 0; f < frames; f++) {
            double t = (double) f / frames;
            for (int y = 0; y < 16; y++)
                for (int x = 0; x < 16; x++) {
                    double u = x / 16.0, w = y / 16.0, v;
                    if (flow) {
                        double wf = w - t;
                        v = 0.5 + 0.24 * Math.sin(tau * (u + p1) + 1.1 * Math.sin(tau * (wf + p2))) + 0.14 * Math.sin(tau * (2 * u + wf));
                    } else {
                        v = 0.5 + 0.24 * Math.sin(tau * (u + w + p1) + 0.9 * Math.sin(tau * (w - t)))
                                + 0.12 * Math.sin(tau * (u - w + t + p2));
                    }
                    int step = v < 0.3 ? 3 : v < 0.7 ? 4 : 5;
                    if (v < 0.1) step = 2;
                    im.setRGB(x, f * 16 + y, 0xFF000000 | LATEX.get(step));
                }
        }
        return im;
    }

    /** Latex fill strip of the soaking barrel GUI (u 240, v 0, 16x52), same shading as the other strips. */
    static void latexGuiStrip() throws IOException {
        File f = TextureGen.OUT.resolve("gui/soaking_barrel.png").toFile();
        BufferedImage im = ImageIO.read(f);
        int[] c = {LATEX.get(2), LATEX.get(3), LATEX.get(4), LATEX.get(5), LATEX.get(5)};
        TextureGen.fillStrip(im, 240, c, 0xff);
        ImageIO.write(im, "png", f);
    }

    // ---------------------------------------------------------------- red alloy and components

    /** Cold crimson metal; darker than the SG 6 heat bands (cherry red #d23a1e) so it never reads as hot. */
    static final TextureGen.Ramp RED_ALLOY = TextureGen.ramp(0xe8a08a, 0x3a1420, 0x5e1c26, 0x8a2a2e, 0xae4038, 0xcc6450);
    static final TextureGen.Ramp CIRCUIT_BOARD = TextureGen.ramp(0, 0x2a2410, 0x40381a, 0x5a4e26, 0x766634, 0x928046);
    /** Relay casing: near-black with a cool tint. */
    static final TextureGen.Ramp RELAY = TextureGen.ramp(0, 0x101216, 0x1a1d22, 0x272b32, 0x383d46, 0x4c525c);

    /**
     * Pixel map over several ramps: digits 1-5 = ramps[0], a-e = ramps[1], f-j = ramps[2], k-o = ramps[3];
     * 's' = specular of ramps[0]; '.' transparent. The 1 px outline is added afterwards.
     */
    static BufferedImage grid(String[] rows, TextureGen.Ramp... ramps) {
        BufferedImage im = TextureGen.img();
        int top = (16 - rows.length) / 2;
        for (int y = 0; y < rows.length; y++) {
            String row = rows[y];
            int left = (16 - row.length()) / 2;
            for (int x = 0; x < row.length(); x++) {
                char ch = row.charAt(x);
                int col;
                if (ch >= '1' && ch <= '5') col = ramps[0].get(ch - '0');
                else if (ch >= 'a' && ch <= 'o') col = ramps[1 + (ch - 'a') / 5].get((ch - 'a') % 5 + 1);
                else if (ch == 's') col = ramps[0].spec() != 0 ? ramps[0].spec() : ramps[0].get(5);
                else continue;
                TextureGen.px(im, left + x, top + y, col);
            }
        }
        return TextureGen.outline(im);
    }

    /** Steel plate 12x9 with three drilled holes growing from 1 px to 3 px. */
    static String[] drawPlateRows() {
        String[] base = {
                ".4555555s55.",
                "444444444443",
                "444444444443",
                "433333333332",
                "433333333332",
                "433333333332",
                "433333333332",
                "322222222221",
                ".1111111111.",
        };
        char[][] g = new char[base.length][];
        for (int i = 0; i < base.length; i++) g[i] = base[i].toCharArray();
        int[][] holes = {{2, 4, 1}, {4, 3, 2}, {7, 3, 3}}; // {x, y, size}, centred on row 4
        for (int[] h : holes) {
            for (int dy = 0; dy < h[2]; dy++)
                for (int dx = 0; dx < h[2]; dx++) g[h[1] + dy][h[0] + dx] = '1';
            if (h[2] >= 2) // lit lower-right rim (light from top-left)
                for (int k = 0; k <= h[2]; k++) {
                    g[h[1] + h[2]][h[0] + k] = '4';
                    if (k < h[2]) g[h[1] + k][h[0] + h[2]] = '4';
                }
        }
        String[] out = new String[g.length];
        for (int i = 0; i < g.length; i++) out[i] = new String(g[i]);
        return out;
    }

    static final String[] BOARD = {
            "544444444443",
            "431333333132",
            "433333333332",
            "433143314332",
            "433333333332",
            "433314331332",
            "433333333332",
            "433143314332",
            "433333333332",
            "433314331332",
            "431333333132",
            "322222222221",
    };

    // Board 1-5, red alloy a-e, copper f-j, relay k-o: red traces joining a copper coil and a black relay.
    static final String[] CIRCUIT = {
            "544444444443",
            "431333333132",
            "433dddddd332",
            "43ijih33d332",
            "43hgfg3mnnm2",
            "43ihih3mllk2",
            "43ggff3mllk2",
            "433d333lkkk2",
            "43edddddde32",
            "43333d333332",
            "43133dddd132",
            "322222222221",
    };

    static String[] motorRows() {
        // 13 wide: body x0..7, copper winding end x8..10, shaft x11..12 (upper right).
        int[] bodyStep = {5, 4, 4, 3, 3, 3, 2, 2, 1};
        int n = bodyStep.length + 1;
        char[][] g = new char[n][13];
        for (char[] r : g) java.util.Arrays.fill(r, '.');
        for (int y = 0; y < bodyStep.length; y++) {
            for (int x = 0; x < 8; x++) {
                int step = bodyStep[y];
                if (x == 0) step = Math.min(5, step + 1);
                if (x == 7) step = Math.max(1, step - 1);
                if (x == 2 || x == 5) step = Math.max(1, step - 1);                      // rib groove
                if ((x == 3 || x == 6) && y > 0 && y < 8) step = Math.min(5, step + 1);  // rib lit edge
                g[y + 1][x] = (char) ('0' + step);
            }
        }
        for (int x = 1; x <= 6; x++) g[0][x] = (x == 1 ? '4' : '3');
        g[0][3] = 's';
        // copper windings, vertical turns alternating bright and dark
        char[] wind = {'d', 'b', 'c'};
        for (int y = 2; y <= 9; y++)
            for (int x = 8; x <= 10; x++) {
                char ch = wind[x - 8];
                if (y == 2 || y == 3) ch = (char) Math.min('e', ch + 1);
                if (y >= 8) ch = (char) Math.max('a', ch - 1);
                if (y % 2 == 0 && x == 9) ch = (char) Math.min('e', ch + 1);
                g[y][x] = ch;
            }
        // steel shaft sticking out of the upper right of the end
        g[3][11] = '5'; g[3][12] = 's';
        g[4][11] = '3'; g[4][12] = '4';
        String[] out = new String[n];
        for (int i = 0; i < n; i++) out[i] = new String(g[i]);
        return out;
    }

    static void redAlloy() throws IOException {
        save("item/red_alloy_ingot", TextureGen.map(RED_ALLOY, TextureGen.INGOT));
        save("item/red_alloy_nugget", TextureGen.map(RED_ALLOY, TextureGen.NUGGET));
        save("item/red_alloy_rod", TextureGen.map(RED_ALLOY, TextureGen.ROD));
        save("item/red_alloy_wire", TextureGen.map(RED_ALLOY, WIRE));
        save("item/draw_plate", grid(drawPlateRows(), STEEL));
        save("item/circuit_board", grid(BOARD, CIRCUIT_BOARD));
        save("item/basic_circuit", grid(CIRCUIT, CIRCUIT_BOARD, RED_ALLOY, COPPER, RELAY));
        save("item/electric_motor", grid(motorRows(), STEEL, COPPER));
    }

    // ---------------------------------------------------------------- machines (fronts, GUIs, sprites)

    static final TextureGen.Ramp BRICK = TextureGen.FIRE_BRICK;
    static final int[] HEAT = TextureGen.HEAT_BAND;
    static final int[][] LAMP_PX = {{12, 2}, {13, 2}, {12, 3}, {13, 3}};

    static TextureGen.Ramp tierRamp(boolean mv) { return mv ? ALUMINIUM : STEEL; }

    /** Tier casing plate with the identity area (1..14, 1..12) cleared, ready for a machine front. */
    static BufferedImage frontBase(boolean mv, long seed) {
        TextureGen.Ramp r = tierRamp(mv);
        int base = mv ? 4 : 3;
        BufferedImage im = mv ? mvSide() : casing(STEEL, 3, 51);
        if (!mv) stripe(im, 14, 3);
        fill(im, 1, 1, 14, mv ? 12 : 13, c(r, base));
        Random rnd = new Random(seed);
        for (int i = 0; i < 2; i++) {
            int len = 3 + rnd.nextInt(2), x = 1 + rnd.nextInt(6), y = 2 + 2 * i;
            for (int k = 0; k < len; k++) TextureGen.px(im, x + k, y, c(r, base + 1));
        }
        return im;
    }

    /** Recessed port lip (1..14, 5..12) in the tier ramp: dark top-left, lit bottom-right. */
    static void lip(BufferedImage im, boolean mv) {
        TextureGen.Ramp r = tierRamp(mv);
        int base = mv ? 4 : 3;
        for (int x = 1; x <= 14; x++) {
            TextureGen.px(im, x, 5, c(r, base - 1));
            TextureGen.px(im, x, 12, c(r, base + 2));
        }
        for (int y = 5; y <= 12; y++) {
            TextureGen.px(im, 1, y, c(r, base - 1));
            TextureGen.px(im, 14, y, c(r, base + 2));
        }
        TextureGen.px(im, 14, 5, c(r, base));
        TextureGen.px(im, 1, 12, c(r, base));
    }

    /** Status lamp socket at 11..14, 1..4 with the OFF lamp (steel step 1) at 12..13, 2..3. */
    static void lampOff(BufferedImage im, boolean mv) {
        TextureGen.Ramp r = tierRamp(mv);
        int base = mv ? 4 : 3;
        for (int i = 11; i <= 14; i++) {
            TextureGen.px(im, i, 1, c(r, base - 1));
            TextureGen.px(im, i, 4, c(r, base + 1));
        }
        for (int j = 1; j <= 4; j++) {
            TextureGen.px(im, 11, j, c(r, base - 1));
            TextureGen.px(im, 14, j, c(r, base + 1));
        }
        TextureGen.px(im, 14, 1, c(r, base));
        TextureGen.px(im, 11, 4, c(r, base));
        for (int[] p : LAMP_PX) TextureGen.px(im, p[0], p[1], c(STEEL, 1));
    }

    static BufferedImage lampOverlay(int[] ramp) {
        BufferedImage im = TextureGen.img();
        TextureGen.px(im, 12, 2, ramp[3]);
        TextureGen.px(im, 13, 2, ramp[2]);
        TextureGen.px(im, 12, 3, ramp[2]);
        TextureGen.px(im, 13, 3, ramp[1]);
        return im;
    }

    /** Firebrick chamber 2..13, 6..11: two courses, mortar rows at 8 and 11; hot = even heat band glow. */
    static void brickChamber(BufferedImage im, boolean hot) {
        int[][] joints = {{5, 10}, {4, 9}};
        int[] top = {6, 9};
        for (int course = 0; course < 2; course++) {
            int y0 = top[course];
            for (int x = 2; x <= 13; x++) {
                boolean joint = x == joints[course][0] || x == joints[course][1];
                boolean first = x == 2 || x == joints[course][0] + 1 || x == joints[course][1] + 1;
                int mortar = hot ? HEAT[2] : c(BRICK, 1);
                if (joint) {
                    TextureGen.px(im, x, y0, mortar);
                    TextureGen.px(im, x, y0 + 1, mortar);
                } else {
                    TextureGen.px(im, x, y0, hot ? (first ? HEAT[4] : HEAT[3]) : c(BRICK, first ? 4 : 3));
                    TextureGen.px(im, x, y0 + 1, hot ? HEAT[3] : c(BRICK, first ? 3 : 2));
                }
                TextureGen.px(im, x, y0 + 2, hot ? HEAT[2] : c(BRICK, 1));
            }
        }
        // glass reflection on the port, top-left
        TextureGen.px(im, 3, 6, c(STEEL, 5));
        TextureGen.px(im, 4, 6, c(STEEL, 5));
        TextureGen.px(im, 2, 7, c(STEEL, 5));
    }

    static BufferedImage furnaceFront(boolean mv, boolean active) {
        TextureGen.Ramp r = tierRamp(mv);
        int base = mv ? 4 : 3;
        BufferedImage im = frontBase(mv, mv ? 711 : 710);
        // two vent slits above the port, lit lower lip
        for (int y : new int[]{2, 4})
            for (int x = 4; x <= 9; x++) {
                TextureGen.px(im, x, y, c(STEEL, 1));
                if (y == 2) TextureGen.px(im, x, y + 1, c(r, base + 1));
            }
        for (int x = 4; x <= 9; x++) TextureGen.px(im, x, 4, c(STEEL, 1));
        rivets(im, r, base, new int[][]{{2, 2}});
        lip(im, mv);
        brickChamber(im, active);
        lampOff(im, mv);
        return im;
    }

    /** One grinding wheel: bright teeth, dark grooves, flat face, raised hub boss. */
    static void wheel(BufferedImage im, double cx, double cy, double phase) {
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double ox = x + 0.5 - cx, oy = y + 0.5 - cy, d2 = ox * ox + oy * oy;
                if (d2 > 8.0) continue;
                boolean lit = ox + oy < 0;
                double d = Math.sqrt(d2);
                int col;
                if (d <= 0.8) {
                    col = c(STEEL, ox < 0 ? (oy < 0 ? 5 : 2) : (oy < 0 ? 2 : 1));
                } else if (d <= 1.9) {
                    col = c(STEEL, 3);
                } else {
                    int sector = Math.floorMod((int) Math.floor((Math.atan2(oy, ox) + phase) / (Math.PI / 4)), 2);
                    col = c(STEEL, sector == 0 ? (lit ? 5 : 4) : (lit ? 3 : 2));
                }
                TextureGen.px(im, x, y, col);
            }
    }

    static final int[][][] GRIT_FLOOR = {{{7, 10}, {9, 11}}, {{8, 10}, {7, 11}}, {{8, 11}, {8, 10}}, {{6, 11}, {7, 11}}};

    static BufferedImage maceratorFront(boolean mv, int frame, boolean active) {
        BufferedImage im = frontBase(mv, mv ? 721 : 720);
        lip(im, mv);
        fill(im, 2, 6, 13, 11, c(STEEL, 1));
        double ph = active ? frame * Math.PI / 8 : 0;
        wheel(im, 5, 9, ph);
        wheel(im, 11, 9, -ph + Math.PI / 4);
        // intake hopper over the nip between the wheels (columns 7 and 8)
        for (int x = 5; x <= 10; x++) TextureGen.px(im, x, 1, c(STEEL, x == 5 ? 5 : (x == 10 ? 2 : 4)));
        TextureGen.px(im, 5, 2, c(STEEL, 4));
        TextureGen.px(im, 10, 2, c(STEEL, 2));
        for (int x = 6; x <= 9; x++) TextureGen.px(im, x, 2, c(STEEL, 1));
        TextureGen.px(im, 6, 3, c(STEEL, 4));
        TextureGen.px(im, 9, 3, c(STEEL, 2));
        for (int x = 7; x <= 8; x++)
            for (int y = 3; y <= 5; y++) TextureGen.px(im, x, y, c(STEEL, 1));
        if (active) {
            TextureGen.px(im, 7, 2 + frame, c(TextureGen.LIMESTONE, 5));
            TextureGen.px(im, 8, 2 + (frame + 2) % 4, c(TextureGen.LIMESTONE, 4));
            TextureGen.px(im, GRIT_FLOOR[frame][0][0], GRIT_FLOOR[frame][0][1], c(TextureGen.LIMESTONE, 5));
            TextureGen.px(im, GRIT_FLOOR[frame][1][0], GRIT_FLOOR[frame][1][1], c(TextureGen.GRANITE, 4));
        }
        lampOff(im, mv);
        return im;
    }

    static BufferedImage maceratorStrip(boolean mv) {
        BufferedImage strip = new BufferedImage(16, 64, BufferedImage.TYPE_INT_ARGB);
        for (int f = 0; f < 4; f++) strip.getGraphics().drawImage(maceratorFront(mv, f, true), 0, f * 16, null);
        return strip;
    }

    // ---- GUI

    static void lane(BufferedImage im, int y, int outputs) {
        TextureGen.slot(im, 44, y);
        TextureGen.arrow(im, 68, y);
        for (int i = 0; i < outputs; i++) TextureGen.slot(im, 98 + 18 * i, y);
    }

    /** 176x176 electric machine screen: one or two lanes, power bar well, player inventory at y 94. */
    static BufferedImage machineGui(int[] laneY, int outputs) {
        BufferedImage im = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        TextureGen.panel(im, 176, 176);
        TextureGen.well(im, 16, 17, 10, 54, 0x2b2b30);
        for (int y : laneY) lane(im, y, outputs);
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++) TextureGen.slot(im, 8 + col * 18, 94 + row * 18);
        for (int col = 0; col < 9; col++) TextureGen.slot(im, 8 + col * 18, 152);
        return im;
    }

    // ---- sprites

    static int desat(int col) { return TextureGen.mix(col, 0x808080, 0.28); }

    static BufferedImage powerBar(int[] ramp) {
        BufferedImage im = new BufferedImage(8, 52, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 52; y++)
            for (int x = 0; x < 8; x++) {
                int col = x == 0 ? desat(ramp[3]) : desat(ramp[2]);
                if (x > 0 && y % 13 == 12) col = desat(ramp[1]);
                TextureGen.px(im, x, y, col);
            }
        return im;
    }

    static final int[][] EJECT_BOX = {
            {3, 3}, {4, 3}, {5, 3}, {6, 3}, {7, 3}, {3, 4}, {3, 5}, {3, 6}, {3, 7}, {3, 8}, {3, 9},
            {3, 10}, {4, 10}, {5, 10}, {6, 10}, {7, 10}};
    static final int[][] EJECT_ARROW = {
            {5, 6}, {6, 6}, {7, 6}, {8, 6}, {5, 7}, {6, 7}, {7, 7}, {8, 7},
            {9, 4}, {9, 5}, {9, 6}, {9, 7}, {9, 8}, {9, 9}, {10, 5}, {10, 6}, {10, 7}, {10, 8}, {11, 6}, {11, 7}};

    static BufferedImage ejectButton(boolean on) {
        BufferedImage im = new BufferedImage(14, 14, BufferedImage.TYPE_INT_ARGB);
        int face = on ? 0xaeaeae : TextureGen.GUI_FACE;
        int tl = on ? TextureGen.SLOT_DARK : TextureGen.GUI_LIGHT;
        int br = on ? TextureGen.GUI_LIGHT : TextureGen.GUI_SHADOW;
        fill(im, 0, 0, 13, 13, face);
        for (int i = 0; i < 14; i++) {
            TextureGen.px(im, i, 0, tl);
            TextureGen.px(im, 0, i, tl);
            TextureGen.px(im, i, 13, br);
            TextureGen.px(im, 13, i, br);
        }
        TextureGen.px(im, 13, 0, face);
        TextureGen.px(im, 0, 13, face);
        int o = on ? 1 : 0;
        for (int[] p : EJECT_BOX) TextureGen.px(im, p[0] + o - (on ? 0 : 0), p[1] + o, TextureGen.SLOT_DARK);
        for (int[] p : EJECT_ARROW) TextureGen.px(im, p[0] + o, p[1] + o, on ? RUN[1] : TextureGen.SLOT_DARK);
        if (on) for (int[] p : new int[][]{{9, 4}, {9, 5}, {10, 5}, {5, 6}, {6, 6}, {7, 6}, {8, 6}})
            TextureGen.px(im, p[0] + 1, p[1] + 1, RUN[2]);
        return im;
    }

    static BufferedImage ejectHighlight() {
        BufferedImage im = new BufferedImage(14, 14, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < 14; i++) {
            TextureGen.px(im, i, 0, 0xf4f4f0);
            TextureGen.px(im, i, 13, 0xf4f4f0);
            TextureGen.px(im, 0, i, 0xf4f4f0);
            TextureGen.px(im, 13, i, 0xf4f4f0);
        }
        return im;
    }

    static final Map<Character, String[]> LETTERS = Map.of(
            'L', new String[]{"#...", "#...", "#...", "#...", "####"},
            'V', new String[]{"#...#", "#...#", ".#.#.", ".#.#.", "..#.."},
            'M', new String[]{"#...#", "##.##", "#.#.#", "#...#", "#...#"});

    static BufferedImage tierBadge(boolean mv) {
        BufferedImage im = new BufferedImage(15, 9, BufferedImage.TYPE_INT_ARGB);
        TextureGen.Ramp r = tierRamp(mv);
        int base = mv ? 4 : 3;
        fill(im, 0, 0, 14, 8, c(r, base));
        for (int x = 0; x < 15; x++) {
            TextureGen.px(im, x, 0, c(r, base + 1));
            TextureGen.px(im, x, 8, c(r, base - 1));
        }
        for (int y = 0; y < 9; y++) {
            TextureGen.px(im, 0, y, c(r, base + 1));
            TextureGen.px(im, 14, y, c(r, base - 1));
        }
        TextureGen.px(im, 14, 0, c(r, base));
        TextureGen.px(im, 0, 8, c(r, base));
        for (int[] p : new int[][]{{0, 0}, {14, 0}, {0, 8}, {14, 8}}) im.setRGB(p[0], p[1], 0);
        String word = mv ? "MV" : "LV";
        int w = (mv ? 5 : 4) + 1 + 5, x0 = 1 + (13 - w) / 2, y0 = 2;
        for (char ch : word.toCharArray()) {
            String[] g = LETTERS.get(ch);
            for (int j = 0; j < g.length; j++)
                for (int i = 0; i < g[j].length(); i++)
                    if (g[j].charAt(i) == '#') TextureGen.px(im, x0 + i, y0 + j, c(STEEL, 1));
            x0 += g[0].length() + 1;
        }
        return im;
    }

    static void machines() throws IOException {
        for (boolean mv : new boolean[]{false, true}) {
            String t = mv ? "mv" : "lv";
            save("block/electric_furnace_front_" + t, furnaceFront(mv, false));
            save("block/electric_furnace_front_" + t + "_active", furnaceFront(mv, true));
            save("block/macerator_front_" + t, maceratorFront(mv, 0, false));
            TextureGen.saveAnimated("block/macerator_front_" + t + "_active", maceratorStrip(mv), 2);
            TextureGen.saveRaw("gui/sprites/container/electric_machine/tier_" + t, tierBadge(mv));
        }
        save("block/overlay/status_run", lampOverlay(RUN));
        save("block/overlay/status_wait", lampOverlay(WAIT));
        save("block/overlay/status_error", lampOverlay(ERROR));

        int[] one = {35}, two = {24, 46};
        TextureGen.saveRaw("gui/electric_furnace", machineGui(one, 1));
        TextureGen.saveRaw("gui/electric_furnace_mv", machineGui(two, 1));
        TextureGen.saveRaw("gui/macerator", machineGui(one, 3));
        TextureGen.saveRaw("gui/macerator_mv", machineGui(two, 3));

        String sp = "gui/sprites/container/electric_machine/";
        TextureGen.saveRaw(sp + "power_bar_run", powerBar(RUN));
        TextureGen.saveRaw(sp + "power_bar_low", powerBar(WAIT));
        TextureGen.saveRaw(sp + "power_bar_stopped", powerBar(ERROR));
        TextureGen.saveRaw(sp + "eject_off", ejectButton(false));
        TextureGen.saveRaw(sp + "eject_on", ejectButton(true));
        TextureGen.saveRaw(sp + "eject_highlighted", ejectHighlight());
    }

    // ---------------------------------------------------------------- main

    public static void main(String[] args) throws IOException {
        casings();
        batteries();
        cables();
        dynamo();
        items();
        rubber();
        redAlloy();
        machines();
        preview();
        System.out.println("wrote " + OUTS.size() + " textures");
    }

    static void preview() throws IOException {
        int cols = 8, s = 6, cell = 16 * s + 8;
        int rows = (OUTS.size() + cols - 1) / cols;
        BufferedImage sheet = new BufferedImage(cols * cell, rows * cell, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < sheet.getHeight(); y++) for (int x = 0; x < sheet.getWidth(); x++) sheet.setRGB(x, y, 0xFF3a3a40);
        int i = 0;
        for (BufferedImage im : OUTS.values()) {
            int ox = (i % cols) * cell + 4, oy = (i / cols) * cell + 4;
            for (int y = 0; y < 16; y++)
                for (int x = 0; x < 16; x++) {
                    int p = im.getRGB(x, y);
                    if ((p >>> 24) == 0) continue;
                    for (int a = 0; a < s; a++) for (int b = 0; b < s; b++) sheet.setRGB(ox + x * s + a, oy + y * s + b, p);
                }
            i++;
        }
        File f = new File("build/texturegen/electric.png");
        f.getParentFile().mkdirs();
        ImageIO.write(sheet, "png", f);
    }
}
