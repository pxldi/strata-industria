import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import javax.imageio.ImageIO;

/**
 * Tier 5 electric textures: LV/MV casings, battery boxes, cables, kinetic dynamo, copper/lead/rubber items.
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

    // ---------------------------------------------------------------- main

    public static void main(String[] args) throws IOException {
        casings();
        batteries();
        cables();
        dynamo();
        items();
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
