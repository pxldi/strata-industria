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

    // ---------------------------------------------------------------- transformer and energy adapter

    /** MV front: aluminium plate with five cooling fins; the MV double stripe ring comes from {@link #mvSide()}. */
    static BufferedImage transformerFront() {
        BufferedImage im = frontBase(true, 2301);
        for (int y = 2; y <= 10; y += 2) {
            for (int x = 2; x <= 13; x++) {
                pxs(im, x, y, c(ALUMINIUM, 2));
                pxs(im, x, y + 1, c(ALUMINIUM, 5));
            }
            pxs(im, 2, y, c(ALUMINIUM, 1));
            pxs(im, 13, y + 1, c(ALUMINIUM, 4));
        }
        return im;
    }

    /** Steel side with a recessed copper coil window (3..12, 3..10), vertical windings. */
    static BufferedImage transformerSide() {
        BufferedImage im = casing(STEEL, 3, 2302);
        stripe(im, 14, 3);
        fill(im, 3, 3, 12, 10, c(STEEL, 1));
        for (int x = 4; x <= 11; x++) {
            int col = (x % 2 == 0) ? 4 : 2;
            for (int y = 4; y <= 9; y++) pxs(im, x, y, c(COPPER, col));
            pxs(im, x, 4, c(COPPER, col + 1));
            pxs(im, x, 9, c(COPPER, 1));
        }
        for (int y = 3; y <= 10; y++) { pxs(im, 3, y, c(STEEL, 1)); pxs(im, 12, y, c(STEEL, 4)); }
        for (int x = 3; x <= 12; x++) pxs(im, x, 10, c(STEEL, 4));
        pxs(im, 12, 3, c(STEEL, 3));
        pxs(im, 3, 10, c(STEEL, 3));
        return im;
    }

    /** Aluminium top with an amber arrow; texture top edge is the MV front. Down = step down (MV in, back out). */
    static BufferedImage transformerTop(boolean down) {
        BufferedImage im = casing(ALUMINIUM, 4, 62);
        for (int pass = 0; pass < 2; pass++) {
            int o = pass == 0 ? 1 : 0;
            int col = pass == 0 ? c(ALUMINIUM, 2) : WAIT[3];
            for (int y = 3; y <= 8; y++) fill(im, 7 + o, ty(y, down) + o, 8 + o, ty(y, down) + o, col);
            fill(im, 4 + o, ty(9, down) + o, 11 + o, ty(9, down) + o, col);
            fill(im, 5 + o, ty(10, down) + o, 10 + o, ty(10, down) + o, col);
            fill(im, 6 + o, ty(11, down) + o, 9 + o, ty(11, down) + o, col);
            fill(im, 7 + o, ty(12, down) + o, 8 + o, ty(12, down) + o, col);
        }
        return im;
    }

    static int ty(int y, boolean down) { return down ? y : 15 - y; }

    /** Neutral square socket in the brass ramp: beveled frame, dark opening, four contact pins. */
    static BufferedImage adapterFront(boolean mv) {
        TextureGen.Ramp r = tierRamp(mv);
        int base = mv ? 4 : 3;
        BufferedImage im = frontBase(mv, mv ? 2312 : 2311);
        rivets(im, r, base, new int[][]{{2, 2}});
        lampOff(im, mv);
        int x0 = 4, y0 = 4, x1 = 11, y1 = 11;
        fill(im, x0, y0, x1, y1, c(BRASS, 3));
        for (int i = x0; i <= x1; i++) { pxs(im, i, y0, c(BRASS, 5)); pxs(im, i, y1, c(BRASS, 1)); }
        for (int j = y0; j <= y1; j++) { pxs(im, x0, j, c(BRASS, 5)); pxs(im, x1, j, c(BRASS, 1)); }
        pxs(im, x1, y0, c(BRASS, 3));
        pxs(im, x0, y1, c(BRASS, 3));
        fill(im, 6, 6, 9, 9, c(STEEL, 1));
        for (int i = 6; i <= 9; i++) pxs(im, i, 9, c(BRASS, 2));
        for (int[] p : new int[][]{{6, 7}, {9, 7}}) pxs(im, p[0], p[1], c(BRASS, 4));
        pxs(im, 7, 7, c(STEEL, 2));
        pxs(im, 8, 7, c(STEEL, 2));
        return im;
    }

    static void transformerAndAdapter() throws IOException {
        save("block/transformer_front", transformerFront());
        save("block/transformer_side", transformerSide());
        save("block/transformer_top_step_down", transformerTop(true));
        save("block/transformer_top_step_up", transformerTop(false));
        save("block/energy_adapter_front_lv", adapterFront(false));
        save("block/energy_adapter_front_mv", adapterFront(true));
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

    /** Spec 9.4: a squat dark lead cell with two copper terminals, 12x11. */
    static String[] cellRows() {
        return new String[]{
                "..ed....ed..",
                "..cb....cb..",
                "455555555552",
                "433333333332",
                "432222222221",
                "433333333332",
                "433333333332",
                "433333333332",
                "432222222221",
                "322222222221",
                ".1111111111.",
        };
    }

    /** Spec 23.7: an aluminium panel with two copper stripes and a circuit clipped to it, 12x12. */
    static String[] upgradeKitRows() {
        return new String[]{
                "455555555552",
                "433333333332",
                "4333iiiiii32",
                "4333ihhmhj32",
                "4333ihmhhj32",
                "4333ihhhmj32",
                "4333jjjjjj32",
                "433333333332",
                "4dddddddddd2",
                "433333333332",
                "4cccccccccc2",
                "322222222221",
        };
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
        save("item/lead_acid_cell", grid(cellRows(), LEAD, COPPER));
        save("item/mv_upgrade_kit", grid(upgradeKitRows(), ALUMINIUM, COPPER, CIRCUIT_BOARD, RED_ALLOY));
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

    // ---- wiremill, bender, lathe

    static void pxs(BufferedImage im, int x, int y, int col) { TextureGen.px(im, x, y, col); }

    /** Engraved dark glyph with a lit shadow below-right; rows of '#'. */
    static void engrave(BufferedImage im, boolean mv, int x0, int y0, String[] rows) {
        TextureGen.Ramp r = tierRamp(mv);
        int base = mv ? 4 : 3;
        for (int j = 0; j < rows.length; j++)
            for (int i = 0; i < rows[j].length(); i++)
                if (rows[j].charAt(i) == '#') pxs(im, x0 + i + 1, y0 + j + 1, c(r, base + 2));
        for (int j = 0; j < rows.length; j++)
            for (int i = 0; i < rows[j].length(); i++)
                if (rows[j].charAt(i) == '#') pxs(im, x0 + i, y0 + j, c(STEEL, 1));
    }

    /** Beveled steel block: lit top-left, shaded bottom-right. */
    static void block(BufferedImage im, int x0, int y0, int x1, int y1, int body) {
        fill(im, x0, y0, x1, y1, c(STEEL, body));
        for (int x = x0; x <= x1; x++) { pxs(im, x, y0, c(STEEL, body + 2)); pxs(im, x, y1, c(STEEL, body - 1)); }
        for (int y = y0; y <= y1; y++) { pxs(im, x0, y, c(STEEL, body + 2)); pxs(im, x1, y, c(STEEL, body - 1)); }
        pxs(im, x1, y0, c(STEEL, body)); pxs(im, x0, y1, c(STEEL, body));
    }

    static BufferedImage wiremillFront(boolean mv, int frame, boolean active) {
        TextureGen.Ramp r = tierRamp(mv);
        int base = mv ? 4 : 3;
        BufferedImage im = frontBase(mv, mv ? 731 : 730);
        // copper tag with a rivet at each end
        fill(im, 3, 2, 8, 3, c(COPPER, 3));
        for (int x = 3; x <= 8; x++) { pxs(im, x, 2, c(COPPER, 4)); pxs(im, x, 3, c(COPPER, 2)); }
        pxs(im, 3, 2, c(COPPER, 5));
        lip(im, mv);
        fill(im, 2, 6, 13, 11, c(STEEL, 1));
        // draw plate on the right with a small round die
        block(im, 9, 6, 13, 11, 3);
        for (int x = 11; x <= 12; x++) pxs(im, x, 7, c(STEEL, 4));
        pxs(im, 11, 8, c(STEEL, 1)); pxs(im, 12, 8, c(STEEL, 1));
        pxs(im, 11, 9, c(STEEL, 1)); pxs(im, 12, 9, c(STEEL, 1));
        pxs(im, 10, 8, c(STEEL, 2)); pxs(im, 10, 9, c(STEEL, 2));
        pxs(im, 11, 10, c(STEEL, 4)); pxs(im, 12, 10, c(STEEL, 4)); pxs(im, 13, 9, c(STEEL, 4));
        // spool: steel rim, copper windings in rotating sectors, steel hub
        double ph = active ? frame * Math.PI / 6 : 0;
        for (int y = 6; y <= 11; y++)
            for (int x = 2; x <= 8; x++) {
                double ox = x + 0.5 - 5.5, oy = y + 0.5 - 8.5, d = Math.sqrt(ox * ox + oy * oy);
                if (d > 3.05) continue;
                boolean lit = ox + oy < 0;
                int col;
                if (d > 2.5) col = c(STEEL, lit ? 5 : 2);
                else if (d > 1.15) {
                    int sec = Math.floorMod((int) Math.floor((Math.atan2(oy, ox) + ph) / (Math.PI / 3)), 2);
                    col = c(COPPER, sec == 0 ? (lit ? 5 : 4) : (lit ? 3 : 2));
                } else col = c(STEEL, ox < 0 && oy < 0 ? 5 : (ox > 0 && oy > 0 ? 2 : 3));
                pxs(im, x, y, col);
            }
        // wire from the spool through the die and out to the right edge
        pxs(im, 8, 8, c(COPPER, 4));
        pxs(im, 9, 8, c(COPPER, 4));
        pxs(im, 10, 8, c(COPPER, 3));
        pxs(im, 11, 8, c(COPPER, 4));
        pxs(im, 12, 8, c(COPPER, 4));
        pxs(im, 13, 8, c(COPPER, 3));
        pxs(im, 9, 9, c(COPPER, 2));
        if (active) {
            int[] hx = {8, 10, 12, 13};
            pxs(im, hx[frame], 8, c(COPPER, 5));
            pxs(im, 13 - (hx[frame] - 8) * 0 - (frame == 3 ? 5 : 0), 8, c(COPPER, 5));
        }
        lampOff(im, mv);
        return im;
    }

    /** One bender roller: lit/shaded rim, flat face, dark axle and two rotating marks (bright, dark). */
    static void roller(BufferedImage im, double cx, double cy, double rad, double ph, int topClip) {
        for (int y = topClip; y < 12; y++)
            for (int x = 2; x <= 13; x++) {
                double ox = x + 0.5 - cx, oy = y + 0.5 - cy, d = Math.sqrt(ox * ox + oy * oy);
                if (d > rad) continue;
                boolean lit = ox + oy < 0;
                int col = d > rad - 0.9 ? c(STEEL, lit ? 5 : 2) : c(STEEL, lit ? 4 : 3);
                pxs(im, x, y, col);
            }
        pxs(im, (int) Math.floor(cx), (int) Math.floor(cy), c(STEEL, 1));
        double mr = rad - 1.5;
        pxs(im, (int) Math.floor(cx + mr * Math.cos(ph)), (int) Math.floor(cy + mr * Math.sin(ph)), c(STEEL, 5));
        pxs(im, (int) Math.floor(cx - mr * Math.cos(ph)), (int) Math.floor(cy - mr * Math.sin(ph)), c(STEEL, 1));
    }

    static BufferedImage benderFront(boolean mv, int frame, boolean active) {
        TextureGen.Ramp r = tierRamp(mv);
        int base = mv ? 4 : 3;
        BufferedImage im = frontBase(mv, mv ? 741 : 740);
        // tall recess 1..14, 1..12 (the lamp socket is redrawn on top): dark top-left, lit bottom-right bevel
        fill(im, 2, 2, 13, 11, c(STEEL, 1));
        for (int x = 1; x <= 14; x++) { pxs(im, x, 1, c(r, base - 1)); pxs(im, x, 12, c(r, base + 2)); }
        for (int y = 1; y <= 12; y++) { pxs(im, 1, y, c(r, base - 1)); pxs(im, 14, y, c(r, base + 2)); }
        pxs(im, 14, 1, c(r, base)); pxs(im, 1, 12, c(r, base));
        double ph = active ? frame * Math.PI / 4 : 0.6;
        roller(im, 4.5, 10.2, 2.6, -ph, 2);
        roller(im, 11.5, 10.2, 2.6, -ph + 1.0, 2);
        // plate edge through the nip; bright dashes slide when active
        for (int x = 2; x <= 13; x++) {
            boolean bright = active ? ((x + frame) % 4) < 2 : (x % 4) < 2;
            pxs(im, x, 7, c(ALUMINIUM, bright ? 5 : 4));
            pxs(im, x, 8, c(ALUMINIUM, 2));
        }
        roller(im, 8.0, 4.8, 2.6, ph, 2);
        lampOff(im, mv);
        return im;
    }

    static final String[] ROD_ICON = {"...#", "..#.", ".#..", "#..."};
    static final String[] GEAR_ICON = {"#.#.#", ".###.", "##.##", ".###.", "#.#.#"};

    /** Lathe: chuck left, tool post and cutter right, spinning rod or gear blank between. */
    static BufferedImage latheFront(boolean mv, int frame, boolean active, boolean gear) {
        BufferedImage im = frontBase(mv, mv ? 751 : 750);
        if (gear) engrave(im, mv, 6, 0 + 1, new String[]{"#.#.#", ".###.", "##.##", ".###."});
        else engrave(im, mv, 6, 1, ROD_ICON);
        lip(im, mv);
        fill(im, 2, 6, 13, 11, c(STEEL, 1));
        // bed rail along the bottom
        for (int x = 2; x <= 13; x++) pxs(im, x, 11, c(STEEL, 2));
        // chuck with jaws
        block(im, 2, 6, 4, 10, 4);
        for (int y : new int[]{7, 9}) for (int x = 2; x <= 4; x++) pxs(im, x, y, c(STEEL, 2));
        // tool post right with cutter tip pointing at the work
        block(im, 11, 6, 13, 9, 3);
        pxs(im, 12, 7, c(STEEL, 1));
        pxs(im, 10, 7, c(STEEL, 5));
        pxs(im, 11, 8, c(STEEL, 5));
        pxs(im, 10, 8, c(STEEL, 4));
        // workpiece, banded lighter and darker in alternating columns when spinning
        int ph = active ? frame : 0;
        if (!gear) {
            // thin round rod on rows 8..9 between chuck and cutter
            for (int x = 5; x <= 10; x++) {
                boolean band = active && ((x + ph) % 4 < 2);
                pxs(im, x, 8, c(STEEL, band ? 4 : 5));
                pxs(im, x, 9, c(STEEL, band ? 2 : 3));
            }
        } else {
            // gear blank: short drum rows 7..10 with a toothed rim top and bottom
            for (int x = 5; x <= 9; x++) {
                boolean tooth = ((x + ph) % 2) == 0;
                boolean band = active && ((x + ph) % 4 < 2);
                pxs(im, x, 7, tooth ? c(STEEL, 4) : c(STEEL, 1));
                pxs(im, x, 8, c(STEEL, band ? 4 : 5));
                pxs(im, x, 9, c(STEEL, band ? 3 : 4));
                pxs(im, x, 10, tooth ? c(STEEL, 2) : c(STEEL, 1));
            }
        }
        lampOff(im, mv);
        return im;
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

    /** Lathe mode button: same bevelled face as the eject button, rod or gear glyph in SLOT_DARK. */
    static BufferedImage latheButton(boolean gear) {
        BufferedImage im = new BufferedImage(14, 14, BufferedImage.TYPE_INT_ARGB);
        fill(im, 0, 0, 13, 13, TextureGen.GUI_FACE);
        for (int i = 0; i < 14; i++) {
            TextureGen.px(im, i, 0, TextureGen.GUI_LIGHT);
            TextureGen.px(im, 0, i, TextureGen.GUI_LIGHT);
            TextureGen.px(im, i, 13, TextureGen.GUI_SHADOW);
            TextureGen.px(im, 13, i, TextureGen.GUI_SHADOW);
        }
        TextureGen.px(im, 13, 0, TextureGen.GUI_FACE);
        TextureGen.px(im, 0, 13, TextureGen.GUI_FACE);
        String[] g = gear
                ? new String[]{"..#....#..", ".##.##.##.", "..######..", "###....###", "###....###", "..######..", ".##.##.##.", "..#....#.."}
                : new String[]{"........##", ".......###", "......###.", ".....###..", "....###...", "...###....", "..###.....", ".###......", "###......."};
        int x0 = (14 - g[0].length()) / 2, y0 = (14 - g.length) / 2;
        for (int j = 0; j < g.length; j++)
            for (int i = 0; i < g[j].length(); i++)
                if (g[j].charAt(i) == '#') TextureGen.px(im, x0 + i, y0 + j, TextureGen.SLOT_DARK);
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
            save("block/wiremill_front_" + t, wiremillFront(mv, 0, false));
            TextureGen.saveAnimated("block/wiremill_front_" + t + "_active", strip(f -> wiremillFront(mv, f, true)), 2);
            save("block/bender_front_" + t, benderFront(mv, 0, false));
            TextureGen.saveAnimated("block/bender_front_" + t + "_active", strip(f -> benderFront(mv, f, true)), 2);
            save("block/lathe_front_" + t, latheFront(mv, 0, false, false));
            TextureGen.saveAnimated("block/lathe_front_" + t + "_active", strip(f -> latheFront(mv, f, true, false)), 2);
            save("block/lathe_front_" + t + "_gear", latheFront(mv, 0, false, true));
            TextureGen.saveAnimated("block/lathe_front_" + t + "_gear_active", strip(f -> latheFront(mv, f, true, true)), 2);
            OUTS.put("preview/wiremill_" + t, wiremillFront(mv, 1, true));
            OUTS.put("preview/bender_" + t, benderFront(mv, 1, true));
            OUTS.put("preview/lathe_" + t, latheFront(mv, 1, true, false));
            OUTS.put("preview/lathe_gear_" + t, latheFront(mv, 1, true, true));
        }
        save("block/overlay/status_run", lampOverlay(RUN));
        save("block/overlay/status_wait", lampOverlay(WAIT));
        save("block/overlay/status_error", lampOverlay(ERROR));

        int[] one = {35}, two = {24, 46};
        TextureGen.saveRaw("gui/electric_furnace", machineGui(one, 1));
        TextureGen.saveRaw("gui/electric_furnace_mv", machineGui(two, 1));
        TextureGen.saveRaw("gui/macerator", machineGui(one, 3));
        TextureGen.saveRaw("gui/macerator_mv", machineGui(two, 3));
        for (String n : new String[]{"wiremill", "bender", "lathe"}) {
            TextureGen.saveRaw("gui/" + n, machineGui(one, 1));
            TextureGen.saveRaw("gui/" + n + "_mv", machineGui(two, 1));
        }

        String sp = "gui/sprites/container/electric_machine/";
        TextureGen.saveRaw(sp + "power_bar_run", powerBar(RUN));
        TextureGen.saveRaw(sp + "power_bar_low", powerBar(WAIT));
        TextureGen.saveRaw(sp + "power_bar_stopped", powerBar(ERROR));
        TextureGen.saveRaw(sp + "eject_off", ejectButton(false));
        TextureGen.saveRaw(sp + "eject_on", ejectButton(true));
        TextureGen.saveRaw(sp + "eject_highlighted", ejectHighlight());
        TextureGen.saveRaw(sp + "lathe_rod", latheButton(false));
        TextureGen.saveRaw(sp + "lathe_gear", latheButton(true));
    }

    // ---------------------------------------------------------------- generators (steam turbine, combustion generator)

    static final TextureGen.Ramp BRONZE = TextureGen.BRONZE;
    static final TextureGen.Ramp BLUE = TextureGen.WATER;

    /** Round porthole: dark steel bezel (d 4..5), brass rotor with 6 blades and a hub inside; phase rotates the blades. */
    static BufferedImage steamTurbineFront(boolean mv, int frame, boolean active) {
        TextureGen.Ramp r = tierRamp(mv);
        int base = mv ? 4 : 3;
        BufferedImage im = frontBase(mv, mv ? 731 : 730);
        rivets(im, r, base, new int[][]{{2, 2}});
        double cx = 7.5, cy = 8.5;
        double phase = active ? frame * (Math.PI / 3) / 4 : 0.2;
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double ox = x + 0.5 - cx, oy = y + 0.5 - cy, d = Math.sqrt(ox * ox + oy * oy);
                if (d > 5.0) continue;
                boolean lit = ox + oy < 0;
                int col;
                if (d > 4.0) {
                    col = c(r, lit ? base - 1 : base + 2);
                } else {
                    col = c(STEEL, 1);
                    double ang = Math.atan2(oy, ox) - phase;
                    double sector = Math.PI / 3;
                    double delta = Math.abs(Math.IEEEremainder(ang, sector));
                    boolean blade = d > 1.2 && Math.sin(delta) * d <= 0.62 && Math.cos(delta) > 0 && d <= 3.9;
                    if (d <= 1.2) {
                        col = c(BRASS, ox < 0 && oy < 0 ? 5 : (ox > 0 && oy > 0 ? 2 : 4));
                    } else if (blade) {
                        col = c(BRASS, d > 3.1 ? 3 : (lit ? 5 : 4));
                        if (d > 3.1 && lit) col = c(BRASS, 4);
                    } else if (active && d > 3.0) {
                        col = ((x + y) & 1) == 0 ? c(STEEL, 3) : c(BRASS, 2);
                    } else if (d > 3.0) {
                        col = c(STEEL, 2);
                    }
                }
                TextureGen.px(im, x, y, col);
            }
        // glass glint on the bezel edge of the window, top-left
        TextureGen.px(im, 5, 5, c(STEEL, 5));
        TextureGen.px(im, 4, 6, c(STEEL, 5));
        lampOff(im, mv);
        return im;
    }

    static BufferedImage strip(java.util.function.IntFunction<BufferedImage> frames) {
        BufferedImage strip = new BufferedImage(16, 64, BufferedImage.TYPE_INT_ARGB);
        for (int f = 0; f < 4; f++) strip.getGraphics().drawImage(frames.apply(f), 0, f * 16, null);
        return strip;
    }

    /** Casing side with a large round bronze steam inlet flange, 4 bolts and a dark bore. */
    static BufferedImage steamTurbineBack(boolean mv) {
        BufferedImage im;
        if (mv) {
            im = mvSide();
        } else {
            im = casing(STEEL, 3, 51);
            stripe(im, 14, 3);
        }
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double ox = x + 0.5 - 8, oy = y + 0.5 - 8, d = Math.sqrt(ox * ox + oy * oy);
                if (d > 6.0) continue;
                boolean lit = ox + oy < 0;
                int col;
                if (d > 5.2) col = c(BRONZE, lit ? 5 : 2);
                else if (d > 2.9) col = c(BRONZE, 3);
                else if (d > 2.1) col = c(BRONZE, lit ? 1 : 4);
                else col = c(STEEL, 1);
                TextureGen.px(im, x, y, col);
            }
        for (int[] p : new int[][]{{4, 4}, {11, 4}, {4, 11}, {11, 11}}) {
            TextureGen.px(im, p[0], p[1], c(BRONZE, 5));
            TextureGen.px(im, p[0] + 1, p[1] + 1, c(BRONZE, 1));
        }
        TextureGen.px(im, 9, 9, c(STEEL, 2));
        return im;
    }

    static final String[][] FLAME = {
            {".....", ".roo.", "bbyor", "bbyo.", ".r..."},
            {"..r..", ".ooy.", "bbywo", "bbyor", "....."},
            {".....", "..r..", "bbyo.", "bbyor", ".ro.."},
            {".r...", ".oy..", "bbyoo", "bbyo.", ".r.r."}};

    static int flameColor(char ch) {
        return switch (ch) {
            case 'b' -> c(BLUE, 5);
            case 'r' -> HEAT[2];
            case 'o' -> HEAT[3];
            case 'y' -> HEAT[4];
            default -> 0;
        };
    }

    /** Small square window 4..11, 6..11 onto a dark burner chamber with a nozzle; fuel gauge ticks above. */
    static BufferedImage combustionFront(boolean mv, int frame, boolean active) {
        TextureGen.Ramp r = tierRamp(mv);
        int base = mv ? 4 : 3;
        BufferedImage im = frontBase(mv, mv ? 741 : 740);
        // fuel gauge: baseline with long and short ticks
        for (int x = 2; x <= 9; x++) TextureGen.px(im, x, 4, c(r, base - 1));
        for (int x = 2; x <= 8; x += 2) {
            TextureGen.px(im, x, 3, c(r, base + 2));
            if (x == 2 || x == 8) TextureGen.px(im, x, 2, c(r, base + 2));
        }
        lip(im, mv);
        // window frame and dark chamber
        for (int y = 6; y <= 11; y++) {
            TextureGen.px(im, 3, y, c(r, base - 1));
            TextureGen.px(im, 12, y, c(r, base + 2));
        }
        fill(im, 4, 6, 11, 11, c(STEEL, 1));
        for (int x = 4; x <= 11; x++) TextureGen.px(im, x, 11, c(STEEL, 2));
        // nozzle on the left wall
        TextureGen.px(im, 4, 7, c(STEEL, 3));
        TextureGen.px(im, 4, 8, c(STEEL, 4));
        TextureGen.px(im, 5, 8, c(STEEL, 4));
        TextureGen.px(im, 6, 8, c(STEEL, 5));
        TextureGen.px(im, 4, 9, c(STEEL, 2));
        TextureGen.px(im, 5, 9, c(STEEL, 3));
        TextureGen.px(im, 6, 9, c(STEEL, 2));
        if (active) {
            String[] f = FLAME[frame];
            for (int j = 0; j < 5; j++)
                for (int i = 0; i < 5; i++) {
                    int col = flameColor(f[j].charAt(i));
                    if (col != 0) TextureGen.px(im, 7 + i, 6 + j, col);
                }
        }
        // glass reflection, top-left
        TextureGen.px(im, 4, 6, c(STEEL, 5));
        TextureGen.px(im, 5, 6, c(STEEL, 5));
        lampOff(im, mv);
        return im;
    }

    /** Casing top with a round exhaust stack: dark bore, dark ring, soot-stained rim. */
    static BufferedImage combustionTop(boolean mv) {
        BufferedImage im = mv ? casing(ALUMINIUM, 4, 62) : casing(STEEL, 3, 52);
        TextureGen.Ramp r = tierRamp(mv);
        int base = mv ? 4 : 3;
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double ox = x + 0.5 - 8, oy = y + 0.5 - 8, d = Math.sqrt(ox * ox + oy * oy);
                if (d > 4.8) continue;
                boolean lit = ox + oy < 0;
                int col;
                if (d > 3.6) col = (x * 3 + y * 5) % 4 == 0 ? c(STEEL, 3) : c(STEEL, lit ? 3 : 2);
                else if (d > 2.5) col = c(STEEL, 2);
                else col = c(STEEL, 1);
                if (d > 4.2 && !lit) col = c(STEEL, 1 + (x + y) % 2);
                TextureGen.px(im, x, y, col);
            }
        TextureGen.px(im, 7, 7, c(STEEL, 1));
        TextureGen.px(im, 9, 6, c(r, base - 1));
        return im;
    }

    static void generators() throws IOException {
        for (boolean mv : new boolean[]{false, true}) {
            String t = mv ? "mv" : "lv";
            save("block/steam_turbine_front_" + t, steamTurbineFront(mv, 0, false));
            TextureGen.saveAnimated("block/steam_turbine_front_" + t + "_active",
                    strip(f -> steamTurbineFront(mv, f, true)), 2);
            save("block/steam_turbine_back_" + t, steamTurbineBack(mv));
            save("block/combustion_generator_front_" + t, combustionFront(mv, 0, false));
            TextureGen.saveAnimated("block/combustion_generator_front_" + t + "_active",
                    strip(f -> combustionFront(mv, f, true)), 2);
            save("block/combustion_generator_top_" + t, combustionTop(mv));
        }
    }

    // ---------------------------------------------------------------- chemistry (aluminium, acids, mixer, electrolyser)

    static final TextureGen.Ramp ALUMINA = TextureGen.ramp(0xffffff, 0xa4aab4, 0xc2c7ce, 0xdce0e4, 0xeef0f3, 0xfafbfc);
    static final TextureGen.Ramp ALUM = TextureGen.ramp(0xffffff, 0x6c667c, 0x8e88a0, 0xb4b0c4, 0xd6d4e0, 0xeeedf5);
    static final TextureGen.Ramp ACID = TextureGen.ramp(0, 0x7a7430, 0x9a9040, 0xbcb25a, 0xd8cf7c, 0xeee8a2);
    static final TextureGen.Ramp BRINE = TextureGen.ramp(0, 0x2e4a50, 0x3e6068, 0x507a80, 0x6a9498, 0x8cb2b2);

    static final String[] ALUMINA_HEAP = {
            "....5s......",
            "...455554...",
            "..44555554..",
            ".3445545544.",
            "334454444433",
            "233344343332",
            ".2233333322.",
    };
    static final String[] ALUM_LUMPS = {
            "....s5...s5.",
            "...4554.4554",
            "..4455433443",
            ".44554344332",
            "344433444322",
            "233333333221",
            ".2222222221.",
    };

    static void chemistry() throws IOException {
        for (String m : new String[]{"ingot", "nugget", "plate", "rod"}) {
            String[] shape = m.equals("ingot") ? TextureGen.INGOT : m.equals("nugget") ? TextureGen.NUGGET
                    : m.equals("plate") ? TextureGen.PLATE : TextureGen.ROD;
            save("item/aluminium_" + m, TextureGen.map(ALUMINIUM, shape));
        }
        save("item/aluminium_wire", TextureGen.map(ALUMINIUM, WIRE));
        save("item/steel_wire", TextureGen.map(STEEL, WIRE));
        save("item/alumina", TextureGen.map(ALUMINA, ALUMINA_HEAP));
        save("item/alum", TextureGen.map(ALUM, ALUM_LUMPS));
        save("item/sulfuric_acid_bucket", TextureGen.map(IRON, ACID, TextureGen.CREOSOTE_BUCKET));

        fluidAnimated("block/fluid/sulfuric_acid_still", acidFluid(false), 4);
        fluidAnimated("block/fluid/sulfuric_acid_flow", acidFluid(true), 4);
        fluidAnimated("block/fluid/brine_still", brineFluid(false), 2);
        fluidAnimated("block/fluid/brine_flow", brineFluid(true), 2);
        TextureGen.save("block/fluid/hydrogen", gasTexture(0xcfe4ff, 0x4c, 301));
        TextureGen.save("block/fluid/oxygen", gasTexture(0xb4e6ee, 0xb0, 302));
        TextureGen.save("block/fluid/chlorine", gasTexture(0xbcc068, 0xe0, 303));

        for (boolean mv : new boolean[]{false, true}) {
            String t = mv ? "mv" : "lv";
            for (String n : new String[]{"mixer", "electrolyser"}) {
                boolean mix = n.equals("mixer");
                save("block/" + n + "_front_" + t, chemFront(mix, mv, 0, false));
                BufferedImage s = new BufferedImage(16, 128, BufferedImage.TYPE_INT_ARGB);
                for (int f = 0; f < 8; f++) s.getGraphics().drawImage(chemFront(mix, mv, f, true), 0, f * 16, null);
                TextureGen.saveAnimated("block/" + n + "_front_" + t + "_active", s, 2);
                OUTS.put("preview/" + n + "_" + t, chemFront(mix, mv, 2, true));
            }
        }
        for (boolean mv : new boolean[]{false, true}) {
            String t = mv ? "mv" : "lv";
            save("block/assembler_front_" + t, assemblerFront(mv, 0, false));
            BufferedImage s = new BufferedImage(16, 128, BufferedImage.TYPE_INT_ARGB);
            for (int f = 0; f < 8; f++) s.getGraphics().drawImage(assemblerFront(mv, f, true), 0, f * 16, null);
            TextureGen.saveAnimated("block/assembler_front_" + t + "_active", s, 2);
            OUTS.put("preview/assembler_" + t, assemblerFront(mv, 2, true));
        }
        TextureGen.saveRaw("gui/assembler", assemblerGui());
        TextureGen.saveRaw("gui/mixer", mixerGui());
        TextureGen.saveRaw("gui/electrolyser", electrolyserGui());
        for (String n : new String[]{"aluminium_ingot", "aluminium_plate", "aluminium_rod", "aluminium_wire", "steel_wire",
                "alumina", "alum", "sulfuric_acid_bucket"})
            OUTS.put("preview/item_" + n, ImageIO.read(TextureGen.OUT.resolve("item/" + n + ".png").toFile()));
    }

    static void fluidAnimated(String path, BufferedImage strip, int frametime) throws IOException {
        TextureGen.save(path, strip);
        java.nio.file.Files.writeString(TextureGen.OUT.resolve(path + ".png.mcmeta"),
                "{\"animation\":{\"frametime\":" + frametime + ",\"interpolate\":false}}\n");
    }

    /** Oily pale yellow, partial alpha: slow folds with thin lighter sheen streaks. Looping, same layout as creosote. */
    static BufferedImage acidFluid(boolean flow) {
        int p = flow ? 32 : 16, frames = 32, k = p / 16;
        BufferedImage im = new BufferedImage(p, p * frames, BufferedImage.TYPE_INT_ARGB);
        double tau = 2 * Math.PI, p1 = 0.37, p2 = 0.71;
        for (int f = 0; f < frames; f++) {
            double t = (double) f / frames;
            for (int y = 0; y < p; y++)
                for (int x = 0; x < p; x++) {
                    double u = (double) x / p, w = (double) y / p;
                    double wf = flow ? w - t : w;
                    double v = 0.5 + 0.24 * Math.sin(tau * (k * u + k * w + p1) + 0.9 * Math.sin(tau * (wf - (flow ? 0 : t))))
                            + 0.12 * Math.sin(tau * (2 * k * u - k * w + p2 + (flow ? 0 : t)));
                    int step = v < 0.28 ? 2 : v < 0.62 ? 3 : 4;
                    int alpha = 0xC4;
                    double sy = flow ? wf : w - t;
                    double streak = Math.sin(tau * (2 * k * u + 3 * sy + 0.2));
                    if (streak > 0.93 && v > 0.45) { step = 5; alpha = 0xDC; }
                    im.setRGB(x, f * p + y, alpha << 24 | ACID.get(step));
                }
        }
        return im;
    }

    /** Vanilla water structure (lattice of waves quantised to four shades) in a grey green-blue. */
    static BufferedImage brineFluid(boolean flow) {
        int p = flow ? 32 : 16, frames = 16, k = p / 16;
        BufferedImage im = new BufferedImage(p, p * frames, BufferedImage.TYPE_INT_ARGB);
        double tau = 2 * Math.PI;
        int[][] waves = {{1, 1, 1}, {-1, 2, 1}, {2, -1, 2}};
        double[] amp = {1.0, 0.7, 0.45};
        for (int f = 0; f < frames; f++) {
            double t = (double) f / frames;
            for (int y = 0; y < p; y++)
                for (int x = 0; x < p; x++) {
                    double sum = 0;
                    for (int i = 0; i < 3; i++)
                        sum += amp[i] * Math.sin(tau * (k * (waves[i][0] * x + waves[i][1] * y) / (double) p
                                - waves[i][2] * (flow ? t : -t) + i * 1.7));
                    double v = sum / 2.15 + 0.5;
                    int step = v < 0.3 ? 2 : v < 0.55 ? 3 : v < 0.8 ? 4 : 5;
                    int alpha = step == 5 ? 0xCC : step == 2 ? 0xB0 : 0xBC;
                    im.setRGB(x, f * p + y, alpha << 24 | BRINE.get(step));
                }
        }
        return im;
    }

    /** Single-frame gas: soft warped three-shade clouds of one tint, like steam and sulfur dioxide. */
    static BufferedImage gasTexture(int tint, int alpha, long seed) {
        Random r = new Random(seed);
        double[][] g = new double[4][4];
        for (double[] row : g) for (int i = 0; i < 4; i++) row[i] = r.nextDouble();
        double[] val = new double[256];
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double u = x / 4.0, w = y / 4.0;
                int x0 = (int) Math.floor(u), y0 = (int) Math.floor(w);
                double fx = u - x0, fy = w - y0;
                fx = fx * fx * (3 - 2 * fx); fy = fy * fy * (3 - 2 * fy);
                double a = g[y0 & 3][x0 & 3], b = g[y0 & 3][(x0 + 1) & 3], c2 = g[(y0 + 1) & 3][x0 & 3], d = g[(y0 + 1) & 3][(x0 + 1) & 3];
                val[y * 16 + x] = (a * (1 - fx) + b * fx) * (1 - fy) + (c2 * (1 - fx) + d * fx) * fy;
            }
        double[] sorted = val.clone();
        java.util.Arrays.sort(sorted);
        double q1 = sorted[(int) (256 * 0.30)], q2 = sorted[(int) (256 * 0.72)];
        BufferedImage im = TextureGen.img();
        for (int i = 0; i < 256; i++) {
            double m = val[i] < q1 ? 0.88 : val[i] < q2 ? 1.0 : 1.07;
            int col = 0;
            for (int sh = 16; sh >= 0; sh -= 8) col |= Math.min(255, (int) Math.round((tint >> sh & 0xFF) * m)) << sh;
            int a = val[i] < q1 ? alpha * 9 / 10 : alpha;
            im.setRGB(i % 16, i / 16, a << 24 | col);
        }
        return im;
    }

    /** Pipe stub: dark mouth over a shaded brass rim, 2x2. */
    static void port(BufferedImage im, int x, int y) {
        pxs(im, x, y, c(STEEL, 1)); pxs(im, x + 1, y, c(STEEL, 1));
        pxs(im, x, y + 1, c(BRASS, 5)); pxs(im, x + 1, y + 1, c(BRASS, 3));
    }

    static void chemLip(BufferedImage im, int x0, int y0, int x1, int y1, boolean mv) {
        TextureGen.Ramp r = tierRamp(mv);
        int base = mv ? 4 : 3;
        for (int x = x0; x <= x1; x++) { pxs(im, x, y0, c(r, base + 2)); pxs(im, x, y1, c(r, base - 1)); }
        for (int y = y0; y <= y1; y++) { pxs(im, x0, y, c(r, base + 2)); pxs(im, x1, y, c(r, base - 1)); }
        pxs(im, x1, y0, c(r, base)); pxs(im, x0, y1, c(r, base));
    }

    static final int GLASS_DARK = 0x1a2630, GLASS_LIT = 0xa8c8d0, LIQUID = 0x4a6a74, LIQUID_LIVE = 0x6a98a4;

    static BufferedImage chemFront(boolean mixer, boolean mv, int frame, boolean active) {
        TextureGen.Ramp r = tierRamp(mv);
        int base = mv ? 4 : 3;
        BufferedImage im = frontBase(mv, (mixer ? 761 : 771) + (mv ? 1 : 0));
        if (mixer) {
            for (int x : new int[]{2, 5, 8}) port(im, x, 2);
            double cx = 8.0, cy = 9.0;
            double ph = active ? frame * Math.PI / 8 : 0.5;
            for (int y = 5; y <= 12; y++)
                for (int x = 4; x <= 11; x++) {
                    double ox = x + 0.5 - cx, oy = y + 0.5 - cy, d = Math.sqrt(ox * ox + oy * oy);
                    if (d > 4.0) continue;
                    int col;
                    if (d > 3.0) col = c(r, ox + oy < 0 ? base + 2 : base - 1);
                    else {
                        col = GLASS_DARK;
                        if (y >= 9) col = active ? LIQUID_LIVE : LIQUID;
                        if (y == 9 && active && (x + frame) % 4 < 2) col = TextureGen.mix(LIQUID_LIVE, 0xffffff, 0.3);
                        else if (y == 9) col = TextureGen.mix(col, 0xffffff, 0.15);
                        if (ox < -1.5 && oy < -0.5 && ox + oy < -3.2) col = GLASS_LIT;
                    }
                    pxs(im, x, y, col);
                }
            // agitator: two blades from a hub, drawn over the glass and the liquid
            for (int s = -1; s <= 1; s += 2)
                for (double t = 1.0; t <= 2.6; t += 0.5) {
                    int px = (int) Math.floor(cx + s * t * Math.cos(ph)), py = (int) Math.floor(cy + s * t * Math.sin(ph));
                    pxs(im, px, py, c(STEEL, t > 2.0 ? 4 : 5));
                }
            for (int j = 8; j <= 9; j++) for (int i = 7; i <= 8; i++) pxs(im, i, j, c(STEEL, i == 7 && j == 8 ? 5 : 3));
        } else {
            // tall glass cell with two lead electrodes
            chemLip(im, 2, 2, 9, 10, mv);
            fill(im, 3, 3, 8, 9, GLASS_DARK);
            fill(im, 3, 5, 8, 9, active ? LIQUID_LIVE : LIQUID);
            for (int x = 3; x <= 8; x++) pxs(im, x, 5, TextureGen.mix(active ? LIQUID_LIVE : LIQUID, 0xffffff, 0.2));
            pxs(im, 3, 3, GLASS_LIT); pxs(im, 3, 4, TextureGen.mix(GLASS_LIT, GLASS_DARK, 0.5));
            for (int[] e : new int[][]{{3, 4}, {7, 8}}) {
                for (int y = 3; y <= 9; y++) { pxs(im, e[0], y, c(LEAD, 5)); pxs(im, e[1], y, c(LEAD, 3)); }
                pxs(im, e[0], 3, c(LEAD, 5)); pxs(im, e[1], 3, c(LEAD, 4));
            }
            if (active) {
                int[][] bubbles = {{5, 0}, {6, 4}, {5, 5}, {6, 1}};
                for (int[] b : bubbles) {
                    int yy = 9 - ((frame + b[1]) % 8) * 5 / 8;
                    pxs(im, b[0], Math.max(5, yy), 0xe8f4ff);
                }
            }
            for (int y : new int[]{7, 9, 11}) { pxs(im, 12, y, c(STEEL, 1)); pxs(im, 13, y, c(STEEL, 1)); pxs(im, 12, y + 1, c(r, base + 2)); pxs(im, 13, y + 1, c(r, base + 2)); }
            for (int x : new int[]{2, 5, 8}) port(im, x, 11);
        }
        lampOff(im, mv);
        return im;
    }

    /** Spec 23.2: a small window with two inserter-style arms over a work surface; the arms swing in turn when active. */
    static BufferedImage assemblerFront(boolean mv, int frame, boolean active) {
        TextureGen.Ramp r = tierRamp(mv);
        BufferedImage im = frontBase(mv, 781 + (mv ? 1 : 0));
        chemLip(im, 2, 5, 13, 12, mv);
        fill(im, 3, 6, 12, 11, GLASS_DARK);
        // work surface with a steel top edge and a copper blank on it
        fill(im, 3, 10, 12, 11, c(STEEL, 3));
        for (int x = 3; x <= 12; x++) { pxs(im, x, 10, c(STEEL, 5)); pxs(im, x, 11, c(STEEL, 2)); }
        pxs(im, 7, 9, c(COPPER, 4)); pxs(im, 8, 9, c(COPPER, 5));
        if (active && frame % 4 >= 2) pxs(im, 8, 8, c(COPPER, 3));
        pxs(im, 3, 6, GLASS_LIT); pxs(im, 3, 7, TextureGen.mix(GLASS_LIT, GLASS_DARK, 0.5));
        for (int side = 0; side < 2; side++) {
            int px = side == 0 ? 4 : 11, dir = side == 0 ? 1 : -1;
            double ph = active ? frame * Math.PI / 4 + side * Math.PI : 0.0;
            // upper arm drops from the ceiling pivot, forearm reaches toward the centre and swings down to the surface
            double reach = active ? 1.5 + 1.5 * Math.sin(ph) : 1.0;
            int ex = px + dir, ey = 7;
            int tx = px + dir * (int) Math.round(1 + reach), ty = 8 + (active && Math.cos(ph) < 0 ? 1 : 0);
            pxs(im, px, 6, c(STEEL, 1));
            pxs(im, px, 7, c(STEEL, 5));
            for (int k = 0; k <= 2; k++) {
                int lx = ex + (int) Math.round((tx - ex) * k / 2.0), ly = ey + (int) Math.round((ty - ey) * k / 2.0);
                pxs(im, lx, ly, c(STEEL, k == 0 ? 4 : 5));
            }
            pxs(im, tx, ty, c(BRASS, 5));
            pxs(im, tx, Math.min(ty + 1, 9), c(BRASS, 3));
        }
        for (int x : new int[]{2, 5, 8}) port(im, x, 2);
        lampOff(im, mv);
        return im;
    }

    // ---- GUIs

    /** 18x36 recessed glass tank frame around a 16x34 well at (x, y), quarter tick marks on the right edge. */
    static void tank(BufferedImage im, int x, int y) {
        TextureGen.well(im, x - 1, y - 1, 18, 36, 0x20262e);
        for (int j = 0; j < 34; j++) {
            TextureGen.px(im, x, y + j, 0x28303a);
            TextureGen.px(im, x + 1, y + j, 0x232a33);
        }
        for (int j = 0; j < 4; j++) TextureGen.px(im, x + 1, y + 1 + j, 0x3c4a58);
        for (int q = 1; q <= 3; q++) {
            int ty = y + 34 - q * 34 / 4;
            int len = q == 2 ? 4 : 2;
            for (int i = 0; i < len; i++) TextureGen.px(im, x + 15 - i, ty, 0x7a8ea0);
            TextureGen.px(im, x + 16, ty, TextureGen.SLOT_DARK);
        }
    }

    static BufferedImage chemGui(int[][] items, int[][] tanks, int arrowX) {
        BufferedImage im = machineGui(new int[]{}, 0);
        for (int[] t : tanks) tank(im, t[0], t[1]);
        for (int[] s : items) TextureGen.slot(im, s[0], s[1]);
        TextureGen.arrow(im, arrowX, 36);
        return im;
    }

    static BufferedImage assemblerGui() {
        BufferedImage im = chemGui(new int[][]{{62, 26}, {80, 26}, {98, 26}, {62, 44}, {80, 44}, {98, 44}, {144, 36}},
                new int[][]{{36, 18}}, 116);
        // a thin work rail under the grid, with two small clamps
        TextureGen.fill(im, 62, 63, 54, 1, 0x373737);
        TextureGen.fill(im, 62, 64, 54, 1, 0xffffff);
        for (int x : new int[]{62, 113}) TextureGen.fill(im, x, 61, 3, 2, 0x8b8b8b);
        return im;
    }

    static BufferedImage mixerGui() {
        return chemGui(new int[][]{{36, 54}, {54, 54}, {118, 54}},
                new int[][]{{36, 18}, {54, 18}, {72, 18}, {118, 18}}, 92);
    }

    static BufferedImage electrolyserGui() {
        BufferedImage im = chemGui(new int[][]{{36, 54}, {54, 54}, {100, 54}, {118, 54}},
                new int[][]{{36, 18}, {100, 18}, {118, 18}, {136, 18}}, 66);
        // small lead electrode pair between the input tank and the arrow, with two rising bubbles
        TextureGen.fill(im, 56, 20, 8, 1, 0x373737);
        for (int[] e : new int[][]{{57, 5}, {61, 3}}) {
            for (int y = 21; y <= 34; y++) { TextureGen.px(im, e[0], y, c(LEAD, e[1])); TextureGen.px(im, e[0] + 1, y, c(LEAD, e[1] - 1)); }
            TextureGen.px(im, e[0], 21, c(LEAD, 5));
        }
        TextureGen.px(im, 60, 30, 0x8b8b8b); TextureGen.px(im, 60, 26, 0x8b8b8b); TextureGen.px(im, 60, 23, 0x8b8b8b);
        return im;
    }

    // ---------------------------------------------------------------- overhead lines (poles, insulators, ACSR)

    static final TextureGen.Ramp GLAZED_INSULATOR = TextureGen.ramp(0xe0b48a, 0x2e1a12, 0x4a2a1a, 0x6e3e24, 0x925632, 0xb27448);
    static final TextureGen.Ramp TREATED = TextureGen.TREATED_WOOD;
    static final TextureGen.Ramp CREOSOTE = TextureGen.CREOSOTE;

    /** Deterministic hash in 0..15 for grain noise; callers pass wrapped coordinates so tiles stay seamless. */
    static int hh(int x, int y, int seed) {
        int h = x * 374761393 + y * 668265263 + seed * 1274126177;
        h = (h ^ (h >>> 13)) * 1103515245;
        return (h ^ (h >>> 16)) & 15;
    }

    /** Glossy treated wood with vertical grain over w x 16 px at x0; left lit, tiles in y (and in x when w = 16). */
    static void treatedGrain(BufferedImage im, int x0, int w, int[] tone, int seed) {
        for (int x = 0; x < w; x++)
            for (int y = 0; y < 16; y++) {
                int t = tone[x];
                int run = hh(x, ((y + x * 5) & 15) >> 2, seed);
                if (run < 3) t--; else if (run > 12) t++;
                if (hh(x, y, seed + 7) == 0) t--;
                int col = t <= 0 ? c(CREOSOTE, 3 + t) : c(TREATED, t);
                TextureGen.px(im, x0 + x, y, col);
            }
    }

    static BufferedImage treatedLogSide() {
        BufferedImage im = TextureGen.img();
        int[] tone = {3, 3, 4, 3, 2, 0, 3, 4, 4, 3, 2, 0, 3, 3, 4, 3};
        treatedGrain(im, 0, 16, tone, 11);
        // glossy streaks: lit runs on the highlight columns
        int[][] gloss = {{2, 1}, {2, 2}, {2, 3}, {2, 8}, {2, 9}, {8, 12}, {8, 13}, {8, 14}, {9, 5}, {9, 6}, {14, 10}, {14, 11}};
        for (int[] g : gloss) TextureGen.px(im, g[0], g[1], c(TREATED, 5));
        // creosote weeping down the grooves
        int[][] drip = {{5, 2}, {5, 3}, {5, 4}, {5, 5}, {5, 11}, {5, 12}, {11, 6}, {11, 7}, {11, 8}, {11, 14}, {11, 15}, {11, 0}};
        for (int[] d : drip) TextureGen.px(im, d[0], d[1], c(CREOSOTE, 1));
        return im;
    }

    /** Rings soaked dark to the core; creosote ramp for the darkest rings, glossy treated edge. */
    static BufferedImage treatedLogTop() {
        BufferedImage im = TextureGen.img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double dx = x - 7.5, dy = y - 7.5;
                double d = Math.sqrt(dx * dx + dy * dy);
                int col;
                if (Math.max(Math.abs(dx), Math.abs(dy)) > 6.9) {
                    col = (x == 0 || y == 0) ? c(TREATED, 4) : (x == 15 || y == 15) ? c(TREATED, 2) : c(TREATED, 3);
                } else if (d < 1.6) col = c(CREOSOTE, 1);
                else if (d < 2.8) col = c(CREOSOTE, 3);
                else if (d < 3.6) col = c(CREOSOTE, 2);
                else if (d < 4.6) col = c(TREATED, 3);
                else if (d < 5.4) col = c(CREOSOTE, 3);
                else if (d < 6.4) col = c(TREATED, 3);
                else col = c(TREATED, 2);
                // a touch of light on the upper-left rings
                if (d > 3.6 && d < 6.4 && dx + dy < -4 && (x + y) % 2 == 0 && col == c(TREATED, 3)) col = c(TREATED, 4);
                TextureGen.px(im, x, y, col);
            }
        // radial check from the core
        int[][] crack = {{9, 6}, {10, 5}, {11, 5}, {12, 4}};
        for (int[] k : crack) TextureGen.px(im, k[0], k[1], c(CREOSOTE, 1));
        return im;
    }

    static void ringsEnd(BufferedImage im, int x0, int y0) {
        for (int y = 0; y < 6; y++)
            for (int x = 0; x < 6; x++) {
                double dx = x - 2.5, dy = y - 2.5, d = Math.sqrt(dx * dx + dy * dy);
                int col;
                if (Math.max(Math.abs(dx), Math.abs(dy)) > 2.4) col = (x == 0 || y == 0) ? c(TREATED, 4) : c(TREATED, 2);
                else if (d < 1.0) col = c(CREOSOTE, 1);
                else if (d < 1.9) col = c(CREOSOTE, 3);
                else col = c(TREATED, 3);
                TextureGen.px(im, x0 + x, y0 + y, col);
            }
    }

    static BufferedImage utilityPole() {
        BufferedImage im = TextureGen.img();
        int[] tone = {4, 3, 3, 2, 2, 2};
        treatedGrain(im, 0, 6, tone, 23);
        for (int y : new int[]{3, 11}) {
            TextureGen.px(im, 1, y, c(STEEL, 5));
            TextureGen.px(im, 2, y, c(STEEL, 4));
            TextureGen.px(im, 1, y + 1, c(TREATED, 1));
            TextureGen.px(im, 2, y + 1, c(TREATED, 1));
        }
        ringsEnd(im, 6, 0);
        return im;
    }

    static BufferedImage poleInsulator() {
        BufferedImage im = TextureGen.img();
        // glaze 8x8: stacked bell bands, lit top-left, one specular pixel on the lit rim
        int[] band = {5, 4, 3, 2, 4, 3, 2, 1};
        for (int y = 0; y < 8; y++)
            for (int x = 0; x < 8; x++) {
                int t = band[y] + (x <= 1 ? 1 : x >= 6 ? -1 : 0);
                if (x == 0 && y > 0 && y % 4 != 0) t = band[y];
                TextureGen.px(im, x, y, c(GLAZED_INSULATOR, t));
            }
        TextureGen.px(im, 1, 4, GLAZED_INSULATOR.spec());
        // steel 8x8 at x 8: brushed pin and plate
        int[] cols = {4, 5, 4, 3, 3, 3, 2, 1};
        for (int y = 0; y < 8; y++)
            for (int x = 0; x < 8; x++) {
                int t = cols[x] + (hh(x, y, 5) < 4 ? 1 : hh(x, y, 5) > 12 ? -1 : 0);
                if (y == 0) t = Math.max(t, 4);
                if (y == 7) t = Math.min(t, 2);
                TextureGen.px(im, 8 + x, y, c(STEEL, t));
            }
        TextureGen.px(im, 9, 3, c(STEEL, 5));
        TextureGen.px(im, 10, 4, c(STEEL, 1));
        TextureGen.px(im, 13, 3, c(STEEL, 5));
        TextureGen.px(im, 14, 4, c(STEEL, 1));
        return im;
    }

    /** Stacked bell: stub on top, three discs growing downward. Same rows for unfired (clay) and fired (glaze). */
    static String[] insulatorRows() {
        return new String[]{
                ".....54.....",
                ".....43.....",
                "...554432...",
                "...332221...",
                "..55443322..",
                "..33222111..",
                "s54444333322",
                "443333222211",
                "332222221111",
        };
    }

    /** Coil of twisted aluminium wire (diagonal turns) wound on a dark steel core whose ends show. */
    static String[] acsrRows() {
        String[] rows = new String[10];
        for (int y = 0; y < 10; y++) {
            StringBuilder sb = new StringBuilder();
            for (int x = 0; x < 12; x++) {
                if ((y == 0 || y == 9) && (x < 2 || x > 9)) { sb.append('.'); continue; }
                if ((y == 1 || y == 8) && (x == 0 || x == 11)) { sb.append('.'); continue; }
                if (x == 0) { sb.append('d'); continue; }
                if (x == 11) { sb.append('b'); continue; }
                int t = (x + y) % 4;
                int k = t == 0 ? 5 : t == 1 ? 4 : t == 2 ? 3 : 0;
                if (y >= 7) k = k == 0 ? 0 : k - 2;
                else if (y >= 5) k = k == 0 ? 0 : k - 1;
                if (k <= 0) sb.append(y >= 7 ? 'a' : 'b'); else sb.append((char) ('0' + k));
            }
            rows[y] = sb.toString();
        }
        return rows;
    }

    static BufferedImage acsrLine() {
        BufferedImage im = TextureGen.img();
        int[] step = {4, 3, 2, 3};
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) TextureGen.px(im, x, y, c(ALUMINIUM, step[(x + y) & 3]));
        return im;
    }

    /** Creosote fill strip of the soaking barrel GUI (u 176, v 76, 16x52); same pattern as TextureGen.fillStrip. */
    static void creosoteGuiStrip() throws IOException {
        File f = TextureGen.OUT.resolve("gui/soaking_barrel.png").toFile();
        BufferedImage im = ImageIO.read(f);
        int[] c = {c(CREOSOTE, 1), c(CREOSOTE, 2), c(CREOSOTE, 3), c(CREOSOTE, 4), c(CREOSOTE, 5)};
        for (int y = 0; y < 52; y++)
            for (int x = 0; x < 16; x++) {
                int k = 2;
                if (x % 5 == 1 || x % 7 == 4) k = 3;
                if (x % 6 == 3) k = 1;
                if ((x + y * 2) % 11 == 0) k = Math.min(4, k + 1);
                if (x == 0 || x == 15) k = 1;
                im.setRGB(176 + x, 76 + y, 0xff000000 | c[k]);
            }
        ImageIO.write(im, "png", f);
    }

    static void overheadLines() throws IOException {
        save("block/treated_log_side", treatedLogSide());
        save("block/treated_log_top", treatedLogTop());
        save("block/utility_pole", utilityPole());
        save("block/pole_insulator", poleInsulator());
        save("item/unfired_insulator", grid(insulatorRows(), TextureGen.CLAY));
        save("item/ceramic_insulator", grid(insulatorRows(), GLAZED_INSULATOR));
        save("item/acsr_conductor", grid(acsrRows(), ALUMINIUM, STEEL));
        save("block/acsr_line", acsrLine());
        creosoteGuiStrip();
    }

    // ---------------------------------------------------------------- heat and motion (heater, fuel burner, kinetic motor, electric pump)

    static final int DARK1 = 0x141418, DARK2 = 0x24262c, PUMP_SLOT = 0x141010;

    static void px(BufferedImage im, int x, int y, int col) { TextureGen.px(im, x, y, col); }

    /** Round-ish distance from the tile centre (7.5, 7.5) shifted to (cx, cy). */
    static double dist(int x, int y, double cx, double cy) { return Math.hypot(x - cx, y - cy); }

    // ---- electric heater

    /** Coil pixel: dull steel when idle, dark red (LV) or orange (MV) heat band colours when active. */
    static int coilColor(boolean mv, boolean active, int frame, int x, int y) {
        if (!active) return x % 2 == 0 ? c(STEEL, 3) : c(STEEL, 2);
        boolean hi = (x + y * 2 + frame) % 3 == 0;
        if (!mv) return hi ? HEAT[1] : HEAT[0];
        if ((x * 2 + y + frame * 3) % 9 == 0) return HEAT[4];
        return hi ? HEAT[3] : HEAT[2];
    }

    static BufferedImage heaterFront(boolean mv, int frame, boolean active) {
        TextureGen.Ramp r = tierRamp(mv);
        int base = mv ? 4 : 3;
        BufferedImage im = frontBase(mv, mv ? 811 : 810);
        // recessed opening 2..13 x 2..11: dark top-left lip, lit bottom-right lip
        for (int x = 2; x <= 13; x++) { px(im, x, 2, c(r, base - 1)); px(im, x, 11, c(r, base + 2)); }
        for (int y = 2; y <= 11; y++) { px(im, 2, y, c(r, base - 1)); px(im, 13, y, c(r, base + 2)); }
        px(im, 13, 2, c(r, base));
        px(im, 2, 11, c(r, base));
        fill(im, 3, 3, 12, 10, DARK1);
        fill(im, 3, 10, 12, 10, DARK2);
        // coil: three horizontal runs joined by turns at alternating ends
        for (int y : new int[]{4, 6, 8}) for (int x = 3; x <= 12; x++) px(im, x, y, coilColor(mv, active, frame, x, y));
        px(im, 12, 5, coilColor(mv, active, frame, 12, 5));
        px(im, 3, 7, coilColor(mv, active, frame, 3, 7));
        // grille bars over the coil
        for (int x : new int[]{5, 8, 11}) {
            for (int y = 3; y <= 10; y++) px(im, x, y, c(r, y == 3 ? base + 1 : y == 10 ? base - 1 : base));
        }
        // vent slits under the opening
        for (int x = 4; x <= 11; x++) px(im, x, 12, x % 2 == 0 ? c(STEEL, 1) : c(r, base + 1));
        rivets(im, r, base, new int[][]{{1, 1}, {13, 1}});
        return im;
    }

    /** Fire brick heat face with a faint heated centre: glowing mortar joints and scorched brick faces. */
    static BufferedImage heaterTop(boolean mv) {
        BufferedImage im = TextureGen.fireBricks();
        int mortar = BRICK.get(1) & 0xFFFFFF;
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double d = dist(x, y, 7.5, 7.5);
                if (d > 4.6) continue;
                int col = im.getRGB(x, y) & 0xFFFFFF;
                if (col == mortar) px(im, x, y, HEAT[mv ? 1 : 0]);
                else if (d <= 3.2) {
                    // scorched: one step darker, never below step 2
                    for (int s = 5; s >= 3; s--) if (col == (BRICK.get(s) & 0xFFFFFF)) px(im, x, y, BRICK.get(s - 1));
                }
            }
        if (mv) for (int[] p : new int[][]{{1, 1}, {13, 1}, {1, 13}, {13, 13}}) {
            px(im, p[0], p[1], c(ALUMINIUM, 5));
            px(im, p[0] + 1, p[1], c(ALUMINIUM, 3));
            px(im, p[0], p[1] + 1, c(ALUMINIUM, 2));
        }
        return im;
    }

    // ---- liquid fuel burner

    /** Nozzle opening in fire bricks, brass nozzle, brass fuel valve below with an iron hand wheel. */
    static BufferedImage burnerFront(int frame, boolean lit) {
        BufferedImage im = TextureGen.fireBricks();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double d = dist(x, y, 7.5, 4.5);
                if (d > 4.4) continue;
                if (d > 3.6) px(im, x, y, TextureGen.PIG_IRON.get(x + y < 12 ? 4 : 2));
                else px(im, x, y, DARK1);
            }
        // brass nozzle 6..9 x 3..6 with a 2x2 bore
        for (int y = 3; y <= 6; y++)
            for (int x = 6; x <= 9; x++) {
                int s = (x == 6 || y == 3) ? 5 : (x == 9 || y == 6) ? 2 : 4;
                px(im, x, y, c(BRASS, s));
            }
        for (int y = 4; y <= 5; y++) for (int x = 7; x <= 8; x++) px(im, x, y, DARK1);
        if (lit) {
            for (int y = 0; y < 16; y++)
                for (int x = 0; x < 16; x++) {
                    if (dist(x, y, 7.5, 4.5) > 3.6) continue;
                    boolean nozzle = x >= 6 && x <= 9 && y >= 3 && y <= 6;
                    if (nozzle) continue;
                    boolean halo = x >= 5 && x <= 10 && y >= 2 && y <= 7;
                    if (halo) px(im, x, y, ((x * 2 + y * 3 + frame * 5) % 4 == 0) ? HEAT[4] : c(BLUE, 5));
                    else px(im, x, y, ((x + y + frame) % 4 == 0) ? DARK1 : c(BLUE, 3));
                }
            px(im, 7, 4, HEAT[4]);
            px(im, 8, 4, frame % 2 == 0 ? HEAT[5] : HEAT[4]);
            px(im, 7, 5, frame % 2 == 0 ? HEAT[4] : HEAT[5]);
            px(im, 8, 5, HEAT[5]);
        }
        // hand wheel and stem
        for (int x = 5; x <= 10; x++) px(im, x, 10, TextureGen.PIG_IRON.get(x == 5 || x == 6 ? 5 : 4));
        px(im, 4, 10, TextureGen.PIG_IRON.get(3));
        px(im, 11, 10, TextureGen.PIG_IRON.get(2));
        px(im, 7, 11, c(BRASS, 3));
        px(im, 8, 11, c(BRASS, 2));
        // pipe running through the valve body
        for (int x = 0; x <= 15; x++) {
            px(im, x, 12, c(BRASS, 5));
            px(im, x, 13, c(BRASS, 3));
            px(im, x, 14, c(BRASS, 1));
        }
        for (int y = 11; y <= 15; y++)
            for (int x = 5; x <= 10; x++) {
                int s = x == 5 ? 5 : x == 10 ? 2 : (y == 11 ? 5 : y >= 14 ? 2 : 4);
                px(im, x, y, c(BRASS, s));
            }
        px(im, 7, 11, c(BRASS, 3));
        px(im, 8, 11, c(BRASS, 2));
        px(im, 7, 13, c(BRASS, 1));
        px(im, 8, 13, c(BRASS, 1));
        return im;
    }

    static BufferedImage burnerSide() {
        BufferedImage im = TextureGen.fireBricks();
        for (int x = 0; x < 16; x++) {
            px(im, x, 0, TextureGen.WROUGHT_IRON.get(x % 5 == 0 ? 5 : 4));
            px(im, x, 1, TextureGen.WROUGHT_IRON.get(2));
            px(im, x, 14, TextureGen.WROUGHT_IRON.get(x % 7 == 3 ? 4 : 3));
            px(im, x, 15, TextureGen.WROUGHT_IRON.get(1));
        }
        for (int y = 2; y <= 13; y++)
            for (int x = 0; x < 16; x++) {
                double d = dist(x, y, 7.5, 7.5);
                if (d > 5.4) continue;
                boolean lit = x + y < 15;
                int col;
                if (d > 4.4) col = c(BRASS, lit ? 5 : 2);
                else if (d > 2.9) col = c(BRASS, 3);
                else if (d > 2.1) col = c(BRASS, lit ? 1 : 4);
                else col = DARK1;
                px(im, x, y, col);
            }
        for (int[] p : new int[][]{{4, 4}, {11, 4}, {4, 11}, {11, 11}}) {
            px(im, p[0], p[1], c(BRASS, 5));
            px(im, p[0] + 1, p[1] + 1, c(BRASS, 1));
        }
        return im;
    }

    /** Fire bricks with an iron plate, a round flue and four bolts (the firebox has a hot plate instead). */
    static BufferedImage burnerTop() {
        BufferedImage im = TextureGen.fireBricks();
        Random rnd = new Random(821);
        for (int y = 2; y <= 13; y++)
            for (int x = 2; x <= 13; x++) {
                int step = 3;
                if (x == 2 || y == 2) step = 4;
                else if (x == 13 || y == 13) step = 2;
                else if (rnd.nextInt(6) == 0) step = rnd.nextBoolean() ? 2 : 4;
                px(im, x, y, TextureGen.PIG_IRON.get(step));
            }
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double d = dist(x, y, 7.5, 7.5);
                if (d > 4.3) continue;
                if (d > 3.2) px(im, x, y, TextureGen.PIG_IRON.get(x + y < 15 ? 5 : 2));
                else px(im, x, y, DARK1);
            }
        px(im, 6, 7, DARK2);
        px(im, 6, 6, DARK2);
        for (int[] b : new int[][]{{3, 3}, {11, 3}, {3, 11}, {11, 11}}) TextureGen.rivet(im, b[0], b[1]);
        return im;
    }

    // ---- kinetic motor

    /** Cavity seen behind the shaft: dark bore, steel laminations and a few copper field coil turns. */
    static BufferedImage motorInner() {
        BufferedImage im = TextureGen.img();
        fill(im, 0, 0, 15, 15, DARK2);
        Random rnd = new Random(891);
        for (int i = 0; i < 5; i++) {
            int len = 3 + rnd.nextInt(3), x = rnd.nextInt(11), y = rnd.nextInt(16);
            for (int k = 0; k < len; k++) px(im, x + k, y, DARK1);
        }
        for (int y = 4; y <= 11; y++) {
            boolean even = y % 2 == 0;
            px(im, 4, y, c(STEEL, even ? 3 : 2));
            px(im, 5, y, c(STEEL, even ? 2 : 1));
            px(im, 10, y, c(STEEL, even ? 2 : 1));
            px(im, 11, y, c(STEEL, even ? 3 : 2));
        }
        for (int y = 5; y <= 10; y++) {
            if (y % 3 != 1) {
                px(im, 5, y, c(COPPER, y % 3 == 2 ? 3 : 2));
                px(im, 10, y, c(COPPER, y % 3 == 2 ? 2 : 1));
            }
        }
        for (int y = 4; y <= 11; y++) for (int x = 6; x <= 9; x++) px(im, x, y, DARK1);
        for (int y = 4; y <= 11; y++) px(im, 6, y, c(STEEL, 1));
        return im;
    }

    static BufferedImage motorFront(boolean mv, int[] lamp) {
        TextureGen.Ramp r = tierRamp(mv);
        int base = mv ? 4 : 3;
        BufferedImage im = frontBase(mv, mv ? 911 : 910);
        // bearing flange round the window (window is 4..11): dark top-left, lit bottom-right, rounded corners
        for (int i = 4; i <= 11; i++) {
            px(im, i, 3, c(r, base - 1));
            px(im, 3, i, c(r, base - 1));
            px(im, i, 12, c(r, base + 2));
            px(im, 12, i, c(r, base + 2));
        }
        for (int[] p : new int[][]{{3, 3}, {12, 3}, {3, 12}, {12, 12}}) px(im, p[0], p[1], c(r, base));
        px(im, 4, 4, c(r, base - 2));
        // shaft key marks on the flange instead of a nameplate: four bolts at the diagonals
        for (int[] p : new int[][]{{2, 2}, {13, 13}}) px(im, p[0], p[1], c(r, base + 2));
        px(im, 3, 2, c(r, base - 2));
        px(im, 14, 14, c(r, base - 2));
        if (!mv) for (int x = 5; x <= 10; x++) px(im, x, 13, x == 5 || x == 10 ? c(STEEL, 2) : DARK2);
        rivets(im, r, base, new int[][]{{1, 8}, {13, 6}});
        // status lamp socket and lamp, as on the dynamo
        fill(im, 11, 0, 14, 3, c(STEEL, 1));
        px(im, 12, 1, lamp[3]);
        px(im, 13, 1, lamp[2]);
        px(im, 12, 2, lamp[2]);
        px(im, 13, 2, lamp[1]);
        BufferedImage inner = motorInner();
        for (int y = 4; y <= 11; y++) for (int x = 4; x <= 11; x++) im.setRGB(x, y, inner.getRGB(x, y));
        return im;
    }

    /** Casing back with a round fan guard: frame ring, two guard rings, diagonal spokes, hub. */
    static BufferedImage motorBack(boolean mv) {
        TextureGen.Ramp r = tierRamp(mv);
        int base = mv ? 4 : 3;
        BufferedImage im = mv ? casing(ALUMINIUM, 4, 65) : casing(STEEL, 3, 55);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double ox = x - 7.5, oy = y - 7.5, d = Math.hypot(ox, oy);
                if (d > 6.0) continue;
                boolean lit = ox + oy < 0;
                int col;
                if (d > 5.1) col = c(r, lit ? base + 1 : base - 1);
                else if (d <= 1.7) col = c(r, lit ? base + 1 : base);
                else {
                    col = DARK1;
                    boolean ring = d >= 3.0 && d <= 3.9;
                    boolean spoke = Math.abs(ox) < 0.6 || Math.abs(oy) < 0.6;
                    if (ring || spoke) col = c(r, lit ? base : base - 1);
                }
                px(im, x, y, col);
            }
        px(im, 7, 7, DARK2);
        px(im, 8, 8, DARK2);
        return im;
    }

    /** Vertical steel shaft: cylinder shading across x, brushed streaks along y, a keyway and two grooves. */
    static BufferedImage motorShaft() {
        BufferedImage im = TextureGen.img();
        int[] prof = {3, 4, 5, 5, 4, 4, 4, 3, 3, 3, 3, 2, 2, 2, 1, 1};
        Random rnd = new Random(931);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int s = prof[x];
                if (rnd.nextInt(7) == 0) s += rnd.nextBoolean() ? 1 : -1;
                px(im, x, y, c(STEEL, s));
            }
        for (int y = 2; y <= 13; y++) {
            px(im, 9, y, c(STEEL, 1));
            px(im, 10, y, c(STEEL, 4));
        }
        for (int x = 0; x < 16; x++) {
            px(im, x, 0, c(STEEL, 1));
            px(im, x, 1, c(STEEL, prof[x] + 1));
            px(im, x, 8, c(STEEL, 1));
            px(im, x, 9, c(STEEL, prof[x] + 1));
        }
        px(im, 2, 3, c(STEEL, 5));
        px(im, 2, 11, c(STEEL, 5));
        return im;
    }

    static BufferedImage motorShaftEnd() {
        BufferedImage im = TextureGen.img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int dx = 2 * x - 15, dy = 2 * y - 15;
                int k = (Math.max(Math.abs(dx), Math.abs(dy)) + 1) / 2; // 1..8
                boolean litSide = (dy < 0 && Math.abs(dy) >= Math.abs(dx)) || (dx < 0 && Math.abs(dx) >= Math.abs(dy));
                int col;
                if (k == 1) col = c(STEEL, 1);
                else if (k == 2) col = c(STEEL, litSide ? 5 : 3);
                else if (k == 3) col = c(STEEL, 1);
                else if (k == 8) col = c(STEEL, 1);
                else col = c(STEEL, (k % 2 == 0 ? 3 : 2) + (litSide ? 1 : 0));
                px(im, x, y, col);
            }
        // keyway notch in the hub
        px(im, 8, 6, c(STEEL, 1));
        px(im, 8, 7, c(STEEL, 1));
        return im;
    }

    // ---- electric pump

    static BufferedImage pumpElFront(int frame, boolean active) {
        BufferedImage im = TextureGen.bronzePlates(8811);
        Random rnd = new Random(951);
        // rows 0..3 are cropped away by the model: plain casing there
        for (int y = 0; y <= 3; y++) for (int x = 0; x < 16; x++) px(im, x, y, c(BRONZE, y == 3 ? 2 : rnd.nextInt(5) == 0 ? 4 : 3));
        for (int x = 0; x < 16; x++) px(im, x, 4, c(BRONZE, x % 5 == 0 ? 5 : 4));
        for (int y = 5; y <= 15; y++) for (int x : new int[]{0, 15}) px(im, x, y, c(BRONZE, x == 0 ? 4 : 2));
        // intake grille 3..12 x 5..11 in a bronze frame
        for (int y = 5; y <= 11; y++)
            for (int x = 3; x <= 12; x++) {
                boolean frame2 = x == 3 || y == 5 || x == 12 || y == 11;
                int col;
                if (frame2) col = c(BRONZE, x == 3 || y == 5 ? 5 : 2);
                else if (y % 2 == 0) col = c(BRONZE, 4);
                else col = active ? c(BLUE, 1) : PUMP_SLOT;
                px(im, x, y, col);
            }
        if (active) {
            for (int y : new int[]{7, 9}) {
                int gx = 4 + ((frame * 2 + (y == 9 ? 3 : 0)) % 8);
                px(im, gx, y, c(BLUE, 4));
                if (gx + 1 <= 11) px(im, gx + 1, y, c(BLUE, 3));
                if (gx - 1 >= 4 && frame % 2 == 0) px(im, gx - 1, y, c(BLUE, 5));
            }
        }
        // power lamp in a dark socket, bottom right
        fill(im, 11, 12, 14, 14, c(BRONZE, 1));
        px(im, 12, 13, active ? RUN[3] : c(STEEL, 2));
        px(im, 13, 13, active ? RUN[2] : c(STEEL, 1));
        for (int[] b : new int[][]{{1, 13}}) { px(im, b[0], b[1], c(BRONZE, 5)); px(im, b[0] + 1, b[1] + 1, c(BRONZE, 1)); }
        return im;
    }

    /** Housing top: the used 8x8 centre carries four cooling fins; the rest is plain steel plate. */
    static BufferedImage pumpMotorTop() {
        BufferedImage im = TextureGen.img();
        fill(im, 0, 0, 15, 15, c(STEEL, 3));
        Random rnd = new Random(961);
        for (int i = 0; i < 8; i++) {
            int len = 3 + rnd.nextInt(3), x = rnd.nextInt(11), y = rnd.nextInt(16);
            for (int k = 0; k < len; k++) px(im, x + k, y, c(STEEL, 4));
        }
        for (int y = 4; y <= 11; y++)
            for (int x = 4; x <= 11; x++) {
                int col;
                if (y % 2 == 1) col = c(STEEL, 1);
                else col = c(STEEL, x == 4 ? 5 : x == 11 ? 2 : 4);
                px(im, x, y, col);
            }
        for (int x = 4; x <= 11; x++) px(im, x, 4, c(STEEL, x == 4 ? 5 : 5));
        px(im, 11, 4, c(STEEL, 3));
        return im;
    }

    /** Steel motor can: rows 0..3 (and every 4th row) are lit rim, body, body, shadow; ribs at the used edges. */
    static BufferedImage pumpMotorSide() {
        BufferedImage im = TextureGen.img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int row = y % 4;
                int s = row == 0 ? 4 : row == 3 ? 2 : 3;
                if (x == 4 || x == 11) s = row == 3 ? 1 : s + 1;
                px(im, x, y, c(STEEL, Math.min(5, s)));
            }
        for (int y = 0; y < 16; y += 4) {
            // nameplate-like vent slot and two bolts
            for (int x = 6; x <= 9; x++) {
                px(im, x, y + 1, c(STEEL, 1));
                px(im, x, y + 2, x == 6 ? c(STEEL, 2) : c(STEEL, 4));
            }
            px(im, 5, y + 1, c(STEEL, 5));
            px(im, 10, y + 1, c(STEEL, 5));
        }
        return im;
    }

    static void heatAndMotion() throws IOException {
        for (boolean mv : new boolean[]{false, true}) {
            String t = mv ? "mv" : "lv";
            save("block/electric_heater_front_" + t, heaterFront(mv, 0, false));
            TextureGen.saveAnimated("block/electric_heater_front_" + t + "_active",
                    strip(f -> heaterFront(mv, f, true)), 4);
            save("block/electric_heater_top_" + t, heaterTop(mv));

            int[] off = {c(STEEL, 1), c(STEEL, 1), c(STEEL, 1), c(STEEL, 2)};
            save("block/kinetic_motor_front_" + t + "_off", motorFront(mv, off));
            save("block/kinetic_motor_front_" + t + "_run", motorFront(mv, RUN));
            save("block/kinetic_motor_front_" + t + "_wait", motorFront(mv, WAIT));
            save("block/kinetic_motor_front_" + t + "_error", motorFront(mv, ERROR));
            save("block/kinetic_motor_back_" + t, motorBack(mv));
        }
        save("block/liquid_fuel_burner_front", burnerFront(0, false));
        TextureGen.saveAnimated("block/liquid_fuel_burner_front_lit", strip(f -> burnerFront(f, true)), 3);
        save("block/liquid_fuel_burner_side", burnerSide());
        save("block/liquid_fuel_burner_top", burnerTop());
        save("block/kinetic_motor_inner", motorInner());
        save("block/kinetic_motor_shaft", motorShaft());
        save("block/kinetic_motor_shaft_end", motorShaftEnd());
        save("block/electric_pump_front", pumpElFront(0, false));
        TextureGen.saveAnimated("block/electric_pump_front_active", strip(f -> pumpElFront(f, true)), 2);
        save("block/electric_pump_motor_top", pumpMotorTop());
        save("block/electric_pump_motor_side", pumpMotorSide());
    }

    // ---------------------------------------------------------------- power hammer and extruder

    /** Riveted steel frame column: flanged edges with rivets, recessed web with a weld seam, tier accent across the foot. */
    static BufferedImage hammerFrame(boolean mv) {
        BufferedImage im = TextureGen.img();
        fill(im, 0, 0, 15, 15, c(STEEL, 3));
        Random rnd = new Random(mv ? 1302 : 1301);
        // web: vertical brushed streaks
        for (int i = 0; i < 7; i++) {
            int len = 2 + rnd.nextInt(3), x = 4 + rnd.nextInt(8), y = 1 + rnd.nextInt(11);
            for (int k = 0; k < len; k++) px(im, x, y + k, c(STEEL, i % 3 == 0 ? 2 : 4));
        }
        // flanges: lit left edge, shaded right edge, inner shadows where the web sits back
        for (int y = 0; y < 16; y++) {
            px(im, 0, y, c(STEEL, 5));
            px(im, 1, y, c(STEEL, 4));
            px(im, 2, y, c(STEEL, 2));
            px(im, 3, y, c(STEEL, 2));
            px(im, 12, y, c(STEEL, 4));
            px(im, 13, y, c(STEEL, 4));
            px(im, 14, y, c(STEEL, 3));
            px(im, 15, y, c(STEEL, 1));
        }
        for (int x = 0; x < 16; x++) { px(im, x, 0, c(STEEL, x < 15 ? 5 : 3)); px(im, x, 15, c(STEEL, 1)); }
        // weld seam across the web
        for (int x = 4; x <= 11; x++) {
            px(im, x, 7, c(STEEL, 1));
            px(im, x, 8, c(STEEL, x % 2 == 0 ? 5 : 4));
        }
        // rivets down both flanges
        for (int y : new int[]{2, 7, 12}) {
            px(im, 1, y, c(STEEL, 5)); px(im, 2, y + 1, c(STEEL, 1));
            px(im, 13, y, c(STEEL, 5)); px(im, 14, y + 1, c(STEEL, 1));
        }
        // tier accent across the foot, as on the casings
        if (mv) {
            for (int x = 0; x < 16; x++) {
                px(im, x, 13, c(COPPER, 4));
                px(im, x, 14, c(ALUMINIUM, 5));
                px(im, x, 15, c(COPPER, 3));
            }
        } else {
            for (int x = 0; x < 16; x++) px(im, x, 14, c(COPPER, 3));
        }
        return im;
    }

    /** Motor housing side, top and back: bevelled steel plate, cooling fin banks above and below a vent grille. */
    static BufferedImage hammerMotor() {
        BufferedImage im = casing(STEEL, 3, 1311);
        fill(im, 3, 3, 12, 12, c(STEEL, 3));
        for (int y : new int[]{3, 10}) {
            for (int k = 0; k < 3; k++)
                for (int x = 3; x <= 12; x++) px(im, x, y + k, c(STEEL, k == 0 ? 5 : k == 1 ? 3 : 1));
        }
        // vent grille: dark slats with a lit lower lip
        for (int y : new int[]{6, 8}) {
            for (int x = 3; x <= 12; x++) {
                px(im, x, y, c(STEEL, 1));
                px(im, x, y + 1, c(STEEL, 4));
            }
        }
        // fins end in rounded caps
        for (int y : new int[]{3, 10}) {
            px(im, 3, y, c(STEEL, 3)); px(im, 12, y, c(STEEL, 3));
            px(im, 3, y + 2, c(STEEL, 2)); px(im, 12, y + 2, c(STEEL, 2));
        }
        return im;
    }

    static final String[] TIER_LV = {"#...", "#...", "#...", "#...", "####"};

    /** Motor housing front: dark winding window with three copper poles, lamp socket and an engraved tier badge. */
    static BufferedImage hammerMotorFront(boolean mv, int frame, boolean active) {
        TextureGen.Ramp r = tierRamp(mv);
        int base = mv ? 4 : 3;
        BufferedImage im = frontBase(mv, mv ? 1322 : 1321);
        // winding window (lip x 1..10, y 1..6), interior 2..9 x 2..5
        chemLip(im, 1, 1, 10, 5, mv);
        fill(im, 2, 2, 9, 4, c(STEEL, 1));
        for (int pole = 0; pole < 3; pole++) {
            int x0 = 2 + pole * 3;
            for (int y = 2; y <= 4; y++) {
                for (int dx = 0; dx < 2; dx++) {
                    boolean wrap = ((y + (active ? frame : 0) + pole) % 2) == 0;
                    int col;
                    if (active) col = wrap ? c(COPPER, 5) : c(COPPER, 4);
                    else col = wrap ? c(COPPER, 3) : c(COPPER, 2);
                    if (dx == 1) col = active ? (wrap ? c(COPPER, 4) : c(COPPER, 3)) : (wrap ? c(COPPER, 2) : c(COPPER, 1));
                    px(im, x0 + dx, y, col);
                }
            }
            if (active) px(im, x0, 2 + (frame + pole) % 3, HEAT[4]);
        }
        if (active) {
            // warm bleed into the gaps between the poles
            for (int x : new int[]{4, 7}) for (int y = 2; y <= 4; y++) px(im, x, y, y % 2 == frame % 2 ? HEAT[1] : HEAT[0]);
        }
        // engraved tier badge under the window
        String[] l = {"#...", "#...", "#...", "#...", "####"};
        String[] v = {"#...#", "#...#", ".#.#.", ".#.#.", "..#.."};
        String[] m = {"#...#", "##.##", "#.#.#", "#...#", "#...#"};
        String[] a = mv ? m : l;
        int w = a[0].length() + 1 + v[0].length();
        String[] both = new String[5];
        for (int j = 0; j < 5; j++) both[j] = a[j] + "." + v[j];
        engrave(im, mv, 1 + (14 - w) / 2, 7, both);
        rivets(im, r, base, new int[][]{{12, 5}});
        lampOff(im, mv);
        return im;
    }

    /** Copper induction coil winding: horizontal wraps with a lit crest, a dark gap and a crossing mark per wrap. */
    static BufferedImage hammerCoil() {
        BufferedImage im = TextureGen.img();
        for (int y = 0; y < 16; y++) {
            int k = y % 4, wrap = y / 4;
            for (int x = 0; x < 16; x++) {
                int step = k == 0 ? 5 : k == 1 ? 4 : k == 2 ? 3 : 1;
                // the wire is thicker toward the middle of each run: shade by a gentle sine
                if (k == 2 && (x + wrap * 5) % 8 < 2) step = 2;
                if (k == 1 && (x + wrap * 5) % 8 == 0) step = 5;
                px(im, x, y, c(COPPER, step));
            }
            px(im, (wrap * 5 + 3) % 16, y - k + 1 >= 0 ? y - k + 1 : y, c(COPPER, 2));
        }
        // insulating tie straps at the two ends of the run
        for (int y = 0; y < 16; y++) {
            px(im, 0, y, c(RUBBER, y % 4 == 0 ? 5 : 3));
            px(im, 15, y, c(RUBBER, y % 4 == 0 ? 4 : 2));
        }
        return im;
    }

    /** Ram head atlas, laid out like block/steam_hammer_ram: cap, rod, side strip, ram body, face strip. */
    static BufferedImage hammerRam() {
        BufferedImage im = TextureGen.img();
        // end cap (0..4 x 0..5)
        block(im, 0, 0, 4, 5, 3);
        px(im, 2, 2, c(STEEL, 1)); px(im, 2, 3, c(STEEL, 4));
        // polished rod (6..12 x 0..5): lit left, shaded right, copper collar
        for (int y = 0; y <= 5; y++)
            for (int x = 6; x <= 12; x++) px(im, x, y, c(STEEL, x <= 7 ? 5 : x <= 9 ? 4 : x <= 11 ? 3 : 2));
        for (int x = 6; x <= 12; x++) {
            px(im, x, 1, c(COPPER, x <= 8 ? 5 : x <= 10 ? 4 : 2));
            px(im, x, 2, c(COPPER, x <= 8 ? 4 : x <= 10 ? 3 : 1));
        }
        // side strip (13..15 x 0..5)
        for (int y = 0; y <= 5; y++) { px(im, 13, y, c(STEEL, 4)); px(im, 14, y, c(STEEL, 2)); px(im, 15, y, c(STEEL, 1)); }
        // ram body (0..12 x 6..12): bevelled plate, copper band, rivets
        block(im, 0, 6, 12, 12, 3);
        for (int x = 1; x <= 11; x++) { px(im, x, 9, c(COPPER, 4)); px(im, x, 10, c(COPPER, 2)); }
        for (int[] p : new int[][]{{2, 7}, {10, 7}, {2, 11}, {10, 11}}) px(im, p[0], p[1], c(STEEL, 5));
        for (int y = 6; y <= 12; y++) { px(im, 13, y, c(STEEL, 3)); px(im, 14, y, c(STEEL, 2)); px(im, 15, y, c(STEEL, 1)); }
        // striking face strip (rows 13..15)
        for (int x = 0; x < 16; x++) {
            px(im, x, 13, c(STEEL, 1));
            px(im, x, 14, c(STEEL, x % 5 == 2 ? 5 : 4));
            px(im, x, 15, c(STEEL, x % 5 == 2 ? 3 : 2));
        }
        return im;
    }

    // ---- extruder

    /** Extruder: ram cylinder left, billet chamber, heavy die block right with a bore; the ram pushes the billet through. */
    static BufferedImage extruderFront(int frame, boolean active) {
        boolean mv = true;
        TextureGen.Ramp r = tierRamp(mv);
        int base = 4;
        BufferedImage im = frontBase(mv, 1331);
        // vent slits and a rivet above the port
        for (int y : new int[]{2, 4})
            for (int x = 3; x <= 8; x++) {
                px(im, x, y, c(STEEL, 1));
                if (y == 2) px(im, x, y + 1, c(r, base + 1));
            }
        rivets(im, r, base, new int[][]{{1, 2}});
        lip(im, mv);
        fill(im, 2, 6, 13, 11, c(STEEL, 1));
        int head = active ? 5 + frame : 5;
        // hydraulic cylinder with its rod
        block(im, 2, 6, 3, 11, 4);
        px(im, 2, 8, c(STEEL, 2)); px(im, 3, 8, c(STEEL, 2));
        for (int x = 4; x <= head; x++) { px(im, x, 8, c(STEEL, 5)); px(im, x, 9, c(STEEL, 3)); }
        // ram head
        for (int y = 7; y <= 10; y++) { px(im, head + 1, y, c(STEEL, y == 7 ? 5 : 4)); px(im, head + 2, y, c(STEEL, y == 10 ? 1 : 2)); }
        // billet (warm copper) between the head and the die
        for (int x = head + 3; x <= 8; x++) for (int y = 7; y <= 10; y++)
            px(im, x, y, c(COPPER, y == 7 ? 5 : y == 10 ? 2 : (active ? 4 : 3)));
        // heavy die block with a narrow bore through the middle
        block(im, 9, 6, 13, 11, 3);
        for (int x = 9; x <= 13; x++) { px(im, x, 8, c(STEEL, 1)); px(im, x, 9, c(STEEL, 1)); }
        px(im, 9, 7, c(STEEL, 1)); px(im, 9, 10, c(STEEL, 1));
        if (active) {
            px(im, 9, 8, HEAT[3]); px(im, 9, 9, HEAT[2]);
            int len = frame == 0 ? 2 : 4;
            for (int x = 13 - len + 1; x <= 13; x++) { px(im, x, 8, c(COPPER, x == 13 ? 5 : 4)); px(im, x, 9, c(COPPER, 2)); }
        }
        // bolts on the die block corners
        px(im, 10, 6, c(STEEL, 5)); px(im, 12, 11, c(STEEL, 1));
        lampOff(im, mv);
        return im;
    }

    static BufferedImage hammerGui() {
        BufferedImage im = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        TextureGen.panel(im, 176, 182);
        TextureGen.slot(im, 8, 26);
        TextureGen.slot(im, 8, 46);
        TextureGen.slot(im, 35, 26);
        TextureGen.slot(im, 35, 46);
        TextureGen.slot(im, 71, 35);
        TextureGen.well(im, 120, 30, 26, 26, TextureGen.SLOT_FILL);
        // progress arrow: empty outline in the sheet, filled version at u=176, v=0
        for (int j = 0; j < ARROW_EMPTY.length; j++)
            for (int i = 0; i < ARROW_EMPTY[j].length(); i++) {
                char ch = ARROW_EMPTY[j].charAt(i);
                if (ch == 'o') im.setRGB(93 + i, 35 + j, 0xff555555);
                else if (ch == 'f') im.setRGB(93 + i, 35 + j, 0xff000000 | TextureGen.SLOT_FILL);
                char fc = ARROW_FULL[j].charAt(i);
                if (fc == 'h') im.setRGB(176 + i, j, 0xffffffff);
                else if (fc == 'b') im.setRGB(176 + i, j, 0xffdfe4ea);
                else if (fc == 's') im.setRGB(176 + i, j, 0xffadb4be);
            }
        // power bar well: 10x46 frame, 8x44 inside at (154, 18)
        TextureGen.well(im, 153, 17, 10, 46, 0x2b2b30);
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++) TextureGen.slot(im, 8 + col * 18, 100 + row * 18);
        for (int col = 0; col < 9; col++) TextureGen.slot(im, 8 + col * 18, 158);
        return im;
    }

    static final String[] ARROW_EMPTY = {
            "..............o.......",
            "..............oo......",
            "..............ofo.....",
            "..............offo....",
            "ooooooooooooooffffo...",
            "offffffffffffffffffo..",
            "offfffffffffffffffffo.",
            "offffffffffffffffffffo",
            "offfffffffffffffffffff",
            "offffffffffffffffffff.",
            "offfffffffffffffffff..",
            "offffffffffffffffff...",
            "..............offf....",
            "..............off.....",
            "..............of......",
            "..............o.......",
    };
    static final String[] ARROW_FULL = {
            "..............h.......",
            "..............hh......",
            "..............hbh.....",
            "..............hbbh....",
            "hhhhhhhhhhhhhhbbbbh...",
            "hbbbbbbbbbbbbbbbbbbh..",
            "hbbbbbbbbbbbbbbbbbbbh.",
            "hbbbbbbbbbbbbbbbbbbbbh",
            "hbbbbbbbbbbbbbbbbbbbbs",
            "hbbbbbbbbbbbbbbbbbbbs.",
            "hbbbbbbbbbbbbbbbbbbs..",
            "hsssssssssssssbbbbs...",
            "..............hbbs....",
            "..............hbs.....",
            "..............hs......",
            "..............h.......",
    };

    static BufferedImage extruderGui() {
        BufferedImage im = machineGui(new int[]{}, 0);
        TextureGen.slot(im, 44, 36);
        TextureGen.slot(im, 62, 36);
        TextureGen.arrow(im, 86, 36);
        TextureGen.slot(im, 116, 36);
        return im;
    }

    /** Mode button: the lathe button's bevelled 14x14 face with a coloured glyph map ('#' dark, letters in {@code pal}). */
    static BufferedImage modeButton(String[] g, Map<Character, Integer> pal) {
        BufferedImage im = new BufferedImage(14, 14, BufferedImage.TYPE_INT_ARGB);
        fill(im, 0, 0, 13, 13, TextureGen.GUI_FACE);
        for (int i = 0; i < 14; i++) {
            TextureGen.px(im, i, 0, TextureGen.GUI_LIGHT);
            TextureGen.px(im, 0, i, TextureGen.GUI_LIGHT);
            TextureGen.px(im, i, 13, TextureGen.GUI_SHADOW);
            TextureGen.px(im, 13, i, TextureGen.GUI_SHADOW);
        }
        TextureGen.px(im, 13, 0, TextureGen.GUI_FACE);
        TextureGen.px(im, 0, 13, TextureGen.GUI_FACE);
        int x0 = (14 - g[0].length()) / 2, y0 = (14 - g.length) / 2;
        for (int j = 0; j < g.length; j++)
            for (int i = 0; i < g[j].length(); i++) {
                char ch = g[j].charAt(i);
                if (ch == '.') continue;
                TextureGen.px(im, x0 + i, y0 + j, ch == '#' ? TextureGen.SLOT_DARK : pal.get(ch));
            }
        return im;
    }

    static final String[] CABLE_LV = {
            "..####..",
            ".#ssss#.",
            "#ssccss#",
            "#sccccs#",
            "#sccccs#",
            "#ssccss#",
            ".#ssss#.",
            "..####..",
    };
    static final String[] CABLE_MV = {
            "...####...",
            ".##tttt##.",
            ".#tiiiit#.",
            "#tiicciit#",
            "#ticcccit#",
            "#ticcccit#",
            "#tiicciit#",
            ".#tiiiit#.",
            ".##tttt##.",
            "...####...",
    };
    static final String[] PIPE = {
            "........####",
            "........#ff#",
            "#########ff#",
            "#hhhhhhh#ff#",
            "#mmmmmmm#ff#",
            "#mmmmmmm#ff#",
            "#ddddddd#ff#",
            "#########ff#",
            "........#ff#",
            "........####",
    };

    static void hammerAndExtruder() throws IOException {
        for (boolean mv : new boolean[]{false, true}) {
            String t = mv ? "mv" : "lv";
            save("block/power_hammer_frame_" + t, hammerFrame(mv));
            save("block/power_hammer_motor_front_" + t, hammerMotorFront(mv, 0, false));
            TextureGen.saveAnimated("block/power_hammer_motor_front_" + t + "_active", strip2(f -> hammerMotorFront(mv, f, true)), 2);
        }
        save("block/power_hammer_motor", hammerMotor());
        save("block/power_hammer_coil", hammerCoil());
        save("block/power_hammer_ram", hammerRam());
        TextureGen.saveRaw("gui/power_hammer", hammerGui());

        save("block/extruder_front_mv", extruderFront(0, false));
        TextureGen.saveAnimated("block/extruder_front_mv_active", strip2(f -> extruderFront(f, true)), 2);
        TextureGen.saveRaw("gui/extruder", extruderGui());

        String sp = "gui/sprites/container/electric_machine/";
        TextureGen.saveRaw(sp + "extruder_cable_lv", modeButton(CABLE_LV, Map.of('s', 0x3c3836, 'c', 0xcf7a3e)));
        TextureGen.saveRaw(sp + "extruder_cable_mv", modeButton(CABLE_MV,
                Map.of('t', 0x4a5a70, 'i', 0x8a98aa, 'c', 0xcf7a3e)));
        TextureGen.saveRaw(sp + "extruder_pipe", modeButton(PIPE, Map.of('h', 0xffffff, 'm', 0x8b8b8b, 'd', 0x555555, 'f', 0x7a7a7a)));

        OUTS.put("preview/power_hammer_motor_lv", hammerMotorFront(false, 1, true));
        OUTS.put("preview/power_hammer_motor_mv", hammerMotorFront(true, 1, true));
        OUTS.put("preview/extruder_mv", extruderFront(1, true));
        OUTS.put("preview/power_hammer_coil", hammerCoil());
    }

    static BufferedImage strip2(java.util.function.IntFunction<BufferedImage> frames) {
        BufferedImage strip = new BufferedImage(16, 32, BufferedImage.TYPE_INT_ARGB);
        for (int f = 0; f < 2; f++) strip.getGraphics().drawImage(frames.apply(f), 0, f * 16, null);
        return strip;
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
        transformerAndAdapter();
        generators();
        heatAndMotion();
        chemistry();
        hammerAndExtruder();
        overheadLines();
        handTools();
        preview();
        System.out.println("wrote " + OUTS.size() + " textures");
    }

    // ---------------------------------------------------------------- hand tools (spec 13)

    static final TextureGen.Ramp SCREEN = TextureGen.ramp(0xcff5c0, 0x0e2a14, 0x1e4a26, 0x2e7a36, 0x4fae4a, 0x8ad66a);

    /** Spec 13.1: a steel spanner on the diagonal with an open jaw at the top right and a rubber grip, 16x16. */
    static BufferedImage wrenchImage() {
        BufferedImage im = TextureGen.img();
        double ax = 3.5, ay = 12.5, bx = 10.0, by = 6.0; // handle axis
        double hx = 11.0, hy = 4.5, hr = 3.4;            // head centre and radius
        double dx = bx - ax, dy = by - ay, len2 = dx * dx + dy * dy, len = Math.sqrt(len2);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                double px = x + 0.5, py = y + 0.5;
                double t = Math.clamp(((px - ax) * dx + (py - ay) * dy) / len2, 0, 1);
                double dist = Math.hypot(px - (ax + t * dx), py - (ay + t * dy));
                // signed offset across the handle: negative on the lit top-left side
                double across = ((px - ax) * dy - (py - ay) * dx) / len;
                boolean handle = dist <= 1.15;
                double hd = Math.hypot(px - hx, py - hy);
                boolean head = hd <= hr;
                // the jaw: a slot opening up and to the right of the head centre
                double along = ((px - hx) * 1 + (py - hy) * -1) / Math.sqrt(2);
                double side = ((px - hx) * 1 + (py - hy) * 1) / Math.sqrt(2);
                if (head && along > 0.2 && Math.abs(side) < 1.1) head = false;
                if (!handle && !head) continue;
                int level;
                if (head) {
                    double lit = (px - hx) + (py - hy);
                    level = lit < -2.0 ? 5 : lit < -0.4 ? 4 : lit < 1.6 ? 3 : 2;
                } else {
                    level = across < -0.6 ? 5 : across < 0.4 ? 4 : 2;
                }
                boolean grip = handle && !head && t < 0.42;
                int colour = grip ? RUBBER.get(Math.max(2, level - 1)) : STEEL.get(level);
                TextureGen.px(im, x, y, colour);
            }
        }
        return TextureGen.outline(im);
    }

    /** Spec 13.2: a handheld scanner, steel casing, green screen with ore blips, copper aerial and buttons, 10x14. */
    static String[] scannerRows() {
        return new String[]{
                "....c.....",
                "....c.....",
                ".55555555.",
                "4444444443",
                "43ffffff32",
                "43fhhhhf32",
                "43fhihjf32",
                "43fhhhhf32",
                "43ffffff32",
                "4333333332",
                "43c3dd3332",
                "4333333332",
                "3222222221",
                ".11111111.",
        };
    }

    static void handTools() throws IOException {
        save("item/wrench", wrenchImage());
        save("item/ore_scanner", grid(scannerRows(), STEEL, COPPER, SCREEN));
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
