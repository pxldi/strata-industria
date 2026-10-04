import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;

/**
 * Textures for the shared structure blocks: storage crates, miner's lamps, the ore cart, the tool rack,
 * cracked and mossy rock variants, the smouldering log pile, the windlass and the sluice box. Style guide v2:
 * per-pixel grain, vanilla spruce plank palette for wood, near-black outlines on item icons.
 *
 * <p>Run from the repository root: {@code java tools/texturegen/SharedBlockTextures.java}. Output is
 * deterministic and goes to {@code src/main/resources/assets/strataindustria/textures}; a 6x preview sheet is
 * written to {@code build/texturegen/shared_blocks.png}. The cracked and mossy rock textures are derived from
 * the existing {@code <rock>.png} and {@code cobbled_<rock>.png}, so run {@code TextureGen} first.
 *
 * <h2>Miner's lamp atlas layout</h2>
 * {@code miners_lamp_off}, {@code miners_lamp_lit} (4 frames) and {@code miners_lamp_guttering} (6 frames,
 * irregular order) share one 16x16 frame layout, so the six models can swap the texture and nothing else.
 * Coordinates are texel (x, y) of the frame, y down:
 * <ul>
 * <li>(0,0)-(6,3): copper base side, 6 wide x 3 tall, rivets on the middle row;</li>
 * <li>(6,0)-(12,2): copper cap side, 6 wide x 2 tall;</li>
 * <li>(0,3)-(6,9): copper top and bottom plate, 6x6, dark vent in the middle (cap top, cap and base underside,
 *     base shoulder);</li>
 * <li>(6,3)-(10,7): glass chimney side, 4x4: translucent tint, bright edge streak; soot streak when guttering;</li>
 * <li>(10,3)-(14,7): flame, 4x4, transparent around it (draw it on crossed planes with light emission);</li>
 * <li>(14,3)-(16,5): wick, 2x2: cold and charred when off, glowing tip when lit;</li>
 * <li>(8,9)-(16,13): hanging ring and bail, 8x4 dark copper strap;</li>
 * <li>(0,9)-(8,16) and (8,13)-(16,16): plain copper filler, so a stray uv still shows metal.</li>
 * </ul>
 * Only the glass tint, the flame and the wick differ between the three textures.
 *
 * <p>Other layouts: {@code ore_cart} holds the outer wall strip in rows 0-7 (side faces sample x 2-14, end faces
 * x 1-15) and the dark interior planks in rows 8-15; {@code ore_cart_wheels} is a separate file (wheel disc at
 * (0,0)-(5,5), tread at (5,0)-(10,5), axle strip in row 5 from x 10). {@code windlass} has wood posts in
 * x 0-11 rows 0-13, a crossbeam strip in rows 14-15 and the iron bracket plate in x 12-15.
 * {@code windlass_drum} has rope in x 0-8 rows 0-11 and end-grain wood in (8,0)-(16,8). {@code sluice_box}
 * has the wet bed in x 0-9 (rows 14-15 are a lighter riffle strip) and outer planks in x 10-15, drawn vertical
 * so the side and end faces use them with rotation 90.
 */
public final class SharedBlockTextures {
    static final Path OUT = Path.of("src/main/resources/assets/strataindustria/textures");
    static final String[] ROCKS = {"limestone", "shale", "slate", "marble", "granite", "gabbro", "basalt", "rhyolite"};

    /** Vanilla spruce plank values, plus a darker and a lighter step for slats, gaps and highlights. */
    static final int[] SPRUCE = {0x3f2a15, 0x553a1f, 0x614b2e, 0x70522e, 0x7a5a34, 0x886539};
    static final int[] WEATHERED = {0x37302a, 0x463d33, 0x57493a, 0x6a5b47, 0x7d6d56};
    static final int[] WET = {0x1e140b, 0x2c1e11, 0x3a2916, 0x4a3520, 0x5a4428};
    static final int[] IRON = {0x1e2024, 0x34373d, 0x4d5159, 0x6a6f78, 0x8a9099, 0xb8bec6};
    static final int[] COPPER = {0x4a2214, 0x82401f, 0xb85f2f, 0xe08040, 0xf4a868};
    static final int[] ROPE = {0x4a3b22, 0x6e5a36, 0x93794a, 0xb59a62, 0xd2ba84};
    static final int[] FLAME = {0, 0xa0281a, 0xd23a1e, 0xf07a22, 0xf8c23a, 0xfff4d0};
    static final int[] EMBER = {0x3a140e, 0x6e1e14, 0xa0281a, 0xd23a1e, 0xf07a22, 0xf8c23a};
    static final int[] CHAR = {0x0e0d0d, 0x1a1818, 0x28262a, 0x3a3836, 0x504c48};
    static final int[] ASH = {0x2c2a2a, 0x3e3b3a, 0x524e4c, 0x6a6561, 0x857f78};
    static final int[] EARTH = {0x1e1812, 0x2c231a, 0x3a2e22, 0x4a3c2c};
    static final int[] ORE = {0x24201e, 0x3a3330, 0x504640, 0x6a5e56, 0x857870};
    static final int[] MOSS = {0x525d39, 0x5a6d41, 0x627941, 0x738552, 0x7a8f55};

    static final List<BufferedImage> PREVIEW = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        for (boolean locked : new boolean[]{false, true}) {
            String s = locked ? "_locked" : "";
            save("block/crate_side" + s, crate(true, locked));
            save("block/crate_top" + s, crate(false, locked));
        }
        // Lamp: three block textures (shared layout) and the item icon.
        save("block/miners_lamp_off", lampStrip(0, 1));
        save("block/miners_lamp_lit", lampStrip(1, 4));
        save("block/miners_lamp_guttering", lampStrip(2, 6));
        mcmeta("block/miners_lamp_lit", "{\"animation\":{\"frametime\":3}}");
        mcmeta("block/miners_lamp_guttering", "{\"animation\":{\"frametime\":3,\"frames\":["
                + "{\"index\":0,\"time\":4},{\"index\":1,\"time\":2},{\"index\":2,\"time\":5},{\"index\":3,\"time\":9},"
                + "{\"index\":4,\"time\":3},{\"index\":1,\"time\":2},{\"index\":5,\"time\":4},{\"index\":2,\"time\":3}]}}");
        save("item/miners_lamp", lampItem());
        save("block/ore_cart", oreCart());
        save("block/ore_cart_wheels", oreCartWheels());
        save("block/ore_cart_ore", oreHeap());
        save("block/tool_rack", toolRack());
        for (String rock : ROCKS) {
            save("block/cracked_" + rock, cracked(rock));
            save("block/mossy_cobbled_" + rock, mossy(rock));
        }
        save("block/smouldering_log_pile_side", logPileSide());
        save("block/smouldering_log_pile_top", logPileTop());
        mcmeta("block/smouldering_log_pile_top", "{\"animation\":{\"frametime\":6}}");
        save("block/windlass", windlass());
        save("block/windlass_drum", windlassDrum());
        save("block/sluice_box", sluiceBox());
        preview();
    }

    // ---------------------------------------------------------------- helpers

    static BufferedImage img(int w, int h) {
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    }

    static void px(BufferedImage im, int x, int y, int rgb) {
        if (x < 0 || y < 0 || x >= im.getWidth() || y >= im.getHeight()) return;
        im.setRGB(x, y, 0xFF000000 | rgb);
    }

    static void pxa(BufferedImage im, int x, int y, int rgb, int alpha) {
        if (x < 0 || y < 0 || x >= im.getWidth() || y >= im.getHeight()) return;
        im.setRGB(x, y, (alpha << 24) | rgb);
    }

    static boolean opaque(BufferedImage im, int x, int y) {
        if (x < 0 || y < 0 || x >= im.getWidth() || y >= im.getHeight()) return false;
        return (im.getRGB(x, y) >>> 24) != 0;
    }

    static int rgb(BufferedImage im, int x, int y) {
        return im.getRGB(x, y) & 0xFFFFFF;
    }

    static int scale(int c, double f) {
        int r = (int) Math.round(((c >> 16) & 255) * f), g = (int) Math.round(((c >> 8) & 255) * f),
                b = (int) Math.round((c & 255) * f);
        return (Math.min(r, 255) << 16) | (Math.min(g, 255) << 8) | Math.min(b, 255);
    }

    static double lum(int c) {
        return (0.2126 * ((c >> 16) & 255) + 0.7152 * ((c >> 8) & 255) + 0.0722 * (c & 255)) / 255.0;
    }

    static BufferedImage outline(BufferedImage in) {
        BufferedImage out = img(in.getWidth(), in.getHeight());
        out.getGraphics().drawImage(in, 0, 0, null);
        int[][] dirs = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};
        for (int y = 0; y < in.getHeight(); y++)
            for (int x = 0; x < in.getWidth(); x++) {
                if (opaque(in, x, y)) continue;
                for (int[] d : dirs)
                    if (opaque(in, x + d[0], y + d[1])) {
                        px(out, x, y, scale(rgb(in, x + d[0], y + d[1]), 0.32));
                        break;
                    }
            }
        return out;
    }

    static void save(String path, BufferedImage im) throws IOException {
        File f = OUT.resolve(path + ".png").toFile();
        f.getParentFile().mkdirs();
        ImageIO.write(im, "png", f);
        for (int i = 0; i < im.getHeight() / 16; i++) PREVIEW.add(im.getSubimage(0, i * 16, 16, 16));
    }

    static void mcmeta(String path, String json) throws IOException {
        Files.writeString(OUT.resolve(path + ".png.mcmeta"), json + "\n");
    }

    /**
     * Fills a rectangle with per-pixel wood grain smeared along one axis. Steps lo..hi of the ramp, centre-biased.
     */
    static void wood(BufferedImage im, int x0, int y0, int w, int h, boolean alongX, long seed, int[] ramp, int lo, int hi) {
        Random r = new Random(seed);
        double[][] n = new double[h + 2][w + 2];
        for (double[] row : n) for (int i = 0; i < row.length; i++) row[i] = r.nextDouble();
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                double b = alongX ? n[y + 1][x] + n[y + 1][x + 2] : n[y][x + 1] + n[y + 2][x + 1];
                double v = 0.5 * n[y + 1][x + 1] + 0.25 * b;
                int idx = lo + (int) Math.min(hi - lo, Math.max(0, Math.floor((v - 0.2) / 0.6 * (hi - lo + 1))));
                px(im, x0 + x, y0 + y, ramp[idx]);
            }
    }

    static void rect(BufferedImage im, int x0, int y0, int w, int h, int c) {
        for (int y = y0; y < y0 + h; y++) for (int x = x0; x < x0 + w; x++) px(im, x, y, c);
    }

    /** A board edge: dark joint on one side. */
    static void vline(BufferedImage im, int x, int y0, int y1, int c) {
        for (int y = y0; y <= y1; y++) px(im, x, y, c);
    }

    static void hline(BufferedImage im, int x0, int x1, int y, int c) {
        for (int x = x0; x <= x1; x++) px(im, x, y, c);
    }

    /** Metal: noisy steps lo..hi of IRON. */
    static void metal(BufferedImage im, int x0, int y0, int w, int h, long seed, int[] ramp, int lo, int hi) {
        wood(im, x0, y0, w, h, true, seed, ramp, lo, hi);
    }

    /** A rivet: light head with a dark pixel below right. */
    static void rivet(BufferedImage im, int x, int y, int[] ramp, int head, int shade) {
        px(im, x, y, ramp[head]);
        if (opaqueAt(im, x + 1, y + 1)) px(im, x + 1, y + 1, ramp[shade]);
    }

    static boolean opaqueAt(BufferedImage im, int x, int y) {
        return x >= 0 && y >= 0 && x < im.getWidth() && y < im.getHeight();
    }

    /** A twisted rope row: alternating light and mid steps, phase shifts the twist. */
    static void ropeRow(BufferedImage im, int x0, int x1, int y, int a, int b, int phase) {
        for (int x = x0; x <= x1; x++) px(im, x, y, ROPE[((x + phase) & 1) == 0 ? a : b]);
    }

    // ---------------------------------------------------------------- crates

    /**
     * A slatted storage crate in the vanilla spruce plank palette: corner battens, horizontal slats with dark
     * gaps. The side carries a rope grip cut into the slats; the locked variants add a rope wrap (side) and a
     * nailed cross batten lid (top).
     */
    static BufferedImage crate(boolean side, boolean locked) {
        BufferedImage im = img(16, 16);
        long seed = (side ? 11 : 12) + (locked ? 100 : 0);
        if (side) {
            wood(im, 0, 0, 16, 16, true, seed, SPRUCE, 2, 4);
            // slats between the battens, rows 2-4, 6-9, 11-13, with dark gaps in rows 5 and 10
            int[][] slats = {{2, 4}, {6, 9}, {11, 13}};
            for (int[] s : slats) {
                for (int x = 3; x <= 12; x++) {
                    px(im, x, s[0], SPRUCE[Math.min(5, ((rgbIdx(im, x, s[0])) + 1))]);
                    px(im, x, s[1], SPRUCE[Math.max(0, ((rgbIdx(im, x, s[1])) - 1))]);
                }
            }
            hline(im, 3, 12, 5, SPRUCE[0]);
            hline(im, 3, 12, 10, SPRUCE[0]);
            // battens: lighter, vertical grain; horizontal rails top and bottom
            wood(im, 0, 0, 3, 16, false, seed + 1, SPRUCE, 3, 5);
            wood(im, 13, 0, 3, 16, false, seed + 2, SPRUCE, 3, 5);
            wood(im, 3, 0, 10, 2, true, seed + 3, SPRUCE, 3, 5);
            wood(im, 3, 14, 10, 2, true, seed + 4, SPRUCE, 3, 5);
            vline(im, 3, 2, 13, SPRUCE[1]);
            vline(im, 12, 2, 13, SPRUCE[1]);
            hline(im, 3, 12, 1, SPRUCE[2]);
            hline(im, 3, 12, 14, SPRUCE[1]);
            vline(im, 0, 0, 15, SPRUCE[2]);
            vline(im, 15, 0, 15, SPRUCE[1]);
            for (int[] n : new int[][]{{1, 3}, {1, 12}, {14, 3}, {14, 12}}) px(im, n[0], n[1], IRON[3]);
            if (!locked) {
                // rope grip: opening cut into the slats, a rope bar across it
                rect(im, 5, 5, 6, 4, WET[0]);
                hline(im, 5, 10, 8, SPRUCE[1]);
                ropeRow(im, 4, 11, 6, 4, 3, 0);
                ropeRow(im, 4, 11, 7, 2, 1, 1);
                px(im, 4, 6, ROPE[2]);
                px(im, 11, 7, ROPE[1]);
            } else {
                // rope wrapped round the crate with a knot and two loose ends
                ropeRow(im, 0, 15, 6, 4, 3, 0);
                ropeRow(im, 0, 15, 7, 3, 2, 1);
                ropeRow(im, 0, 15, 8, 1, 0, 0);
                rect(im, 7, 5, 3, 4, ROPE[3]);
                px(im, 8, 6, ROPE[4]);
                px(im, 9, 8, ROPE[1]);
                for (int y = 9; y <= 12; y++) px(im, 7, y, ROPE[y % 2 == 0 ? 2 : 3]);
                for (int y = 9; y <= 11; y++) px(im, 9, y, ROPE[y % 2 == 0 ? 3 : 1]);
                px(im, 7, 13, ROPE[1]);
                px(im, 9, 12, ROPE[0]);
            }
        } else {
            wood(im, 0, 0, 16, 16, false, seed, SPRUCE, 2, 4);
            // three boards, 4-wide frame
            wood(im, 0, 0, 16, 2, true, seed + 1, SPRUCE, 3, 5);
            wood(im, 0, 14, 16, 2, true, seed + 2, SPRUCE, 3, 5);
            wood(im, 0, 2, 2, 12, false, seed + 3, SPRUCE, 3, 5);
            wood(im, 14, 2, 2, 12, false, seed + 4, SPRUCE, 3, 5);
            vline(im, 5, 2, 13, SPRUCE[0]);
            vline(im, 9, 2, 13, SPRUCE[0]);
            vline(im, 2, 2, 13, SPRUCE[1]);
            vline(im, 13, 2, 13, SPRUCE[1]);
            hline(im, 0, 15, 2, SPRUCE[1]);
            hline(im, 0, 15, 13, SPRUCE[1]);
            hline(im, 0, 15, 15, SPRUCE[1]);
            hline(im, 0, 15, 0, SPRUCE[5]);
            if (locked) {
                // diagonal cross battens, nailed
                for (int i = 2; i <= 13; i++) {
                    for (int d = -1; d <= 1; d++) {
                        int[] a = {i, i + d}, b = {i, 15 - i + d};
                        px(im, a[0], a[1], SPRUCE[d == 0 ? 4 : 3]);
                        px(im, b[0], b[1], SPRUCE[d == 0 ? 4 : 3]);
                    }
                }
                for (int i = 2; i <= 13; i++) {
                    if (i - 2 >= 2) px(im, i, i - 2, SPRUCE[1]);
                    if (i + 2 <= 13) px(im, i, i + 2, SPRUCE[1]);
                    if (15 - i - 2 >= 2) px(im, i, 15 - i - 2, SPRUCE[1]);
                    if (15 - i + 2 <= 13) px(im, i, 15 - i + 2, SPRUCE[1]);
                }
                for (int[] n : new int[][]{{3, 3}, {12, 3}, {3, 12}, {12, 12}, {7, 7}, {8, 8}})
                    px(im, n[0], n[1], IRON[4]);
                for (int[] n : new int[][]{{4, 4}, {13, 4}, {4, 13}, {13, 13}})
                    px(im, n[0], n[1], SPRUCE[0]);
            }
            for (int[] n : new int[][]{{1, 1}, {14, 1}, {1, 14}, {14, 14}}) px(im, n[0], n[1], IRON[3]);
        }
        return im;
    }

    static int rgbIdx(BufferedImage im, int x, int y) {
        int c = rgb(im, x, y);
        for (int i = 0; i < SPRUCE.length; i++) if (SPRUCE[i] == c) return i;
        return 3;
    }

    // ---------------------------------------------------------------- miner's lamp

    static final String[][] FLAME_LIT = {
            {"0100", "0210", "1331", "0450"},
            {"0010", "0120", "0331", "0540"},
            {"0000", "0100", "0231", "0450"},
            {"0100", "1210", "0331", "0540"}};
    static final String[][] FLAME_GUTTER = {
            {"0000", "0000", "0100", "0230"},
            {"0000", "0100", "0210", "0330"},
            {"0000", "0000", "0000", "0120"},
            {"0000", "0000", "0000", "0010"},
            {"0000", "0000", "0000", "0200"},
            {"0000", "0000", "0100", "0220"}};

    /** mode 0 off, 1 lit, 2 guttering; frames stacked vertically. */
    static BufferedImage lampStrip(int mode, int frames) {
        BufferedImage strip = img(16, 16 * frames);
        for (int f = 0; f < frames; f++) {
            BufferedImage fr = lampFrame(mode, f);
            strip.getGraphics().drawImage(fr, 0, 16 * f, null);
        }
        return strip;
    }

    static BufferedImage lampFrame(int mode, int frame) {
        BufferedImage im = img(16, 16);
        Random r = new Random(77);
        // copper filler everywhere first, so unused texels still read as metal
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) px(im, x, y, COPPER[2 - r.nextInt(2) * 1]);
        // A: base side, B: cap side
        copperBlock(im, 0, 0, 6, 3, 31);
        copperBlock(im, 6, 0, 6, 2, 32);
        px(im, 1, 1, COPPER[4]); px(im, 4, 1, COPPER[4]); px(im, 2, 2, COPPER[0]); px(im, 5, 2, COPPER[0]);
        px(im, 7, 0, COPPER[4]); px(im, 10, 0, COPPER[4]);
        // C: plate with vent
        copperBlock(im, 0, 3, 6, 6, 33);
        rect(im, 2, 5, 2, 2, COPPER[0]);
        for (int[] n : new int[][]{{1, 4}, {4, 4}, {1, 7}, {4, 7}}) px(im, n[0], n[1], COPPER[4]);
        // G: ring strap, darker copper
        for (int y = 9; y < 13; y++)
            for (int x = 8; x < 16; x++) px(im, x, y, COPPER[(y == 9 ? 2 : y == 12 ? 0 : 1) + (r.nextInt(4) == 0 ? 1 : 0)]);
        // D: glass
        int tint = mode == 0 ? 0x1c2428 : mode == 1 ? 0xd89a38 : 0x2a2a28;
        int tintA = mode == 0 ? 96 : mode == 1 ? 46 : 70;
        for (int y = 3; y < 7; y++) for (int x = 6; x < 10; x++) pxa(im, x, y, tint, tintA);
        for (int y = 3; y < 7; y++) pxa(im, 6, y, 0xb4d2da, y == 5 ? 110 : 190);
        pxa(im, 9, 6, 0x6a8c98, 150); pxa(im, 9, 3, 0x6a8c98, 150);
        pxa(im, 7, 3, 0xb4d2da, 200);
        if (mode == 2) {
            // soot streak climbing from the top of the chimney
            for (int y = 3; y <= 6; y++) pxa(im, 8, y, 0x14110e, y <= 4 ? 245 : 200);
            pxa(im, 7, 3, 0x1a1612, 235); pxa(im, 7, 4, 0x1a1612, 210); pxa(im, 9, 3, 0x14110e, 235);
            pxa(im, 8, 6, 0x14110e, 130);
        }
        // E: flame
        for (int y = 3; y < 7; y++) for (int x = 10; x < 14; x++) im.setRGB(x, y, 0);
        if (mode == 1) drawFlame(im, FLAME_LIT[frame % FLAME_LIT.length], FLAME);
        if (mode == 2) drawFlame(im, FLAME_GUTTER[frame % FLAME_GUTTER.length], FLAME);
        // F: wick
        int[] wick = mode == 0 ? new int[]{0x2a241c, 0x3e362a, 0x544836, 0x6a5a40}
                : mode == 1 ? new int[]{0xf8c23a, 0xf07a22, 0x6a5a40, 0x544836}
                : new int[]{0xa0281a, 0x4a2a1c, 0x6a5a40, 0x544836};
        px(im, 14, 3, wick[0]); px(im, 15, 3, wick[1]); px(im, 14, 4, wick[2]); px(im, 15, 4, wick[3]);
        return im;
    }

    static void drawFlame(BufferedImage im, String[] rows, int[] ramp) {
        for (int y = 0; y < 4; y++)
            for (int x = 0; x < 4; x++) {
                int d = rows[y].charAt(x) - '0';
                if (d > 0) px(im, 10 + x, 3 + y, ramp[d]);
            }
    }

    static void copperBlock(BufferedImage im, int x0, int y0, int w, int h, long seed) {
        wood(im, x0, y0, w, h, true, seed, COPPER, 1, 2);
        hline(im, x0, x0 + w - 1, y0, COPPER[3]);
        hline(im, x0, x0 + w - 1, y0 + h - 1, COPPER[0]);
        for (int y = y0; y < y0 + h; y++) px(im, x0 + 1, y, COPPER[3]);
    }

    /** Flat inventory icon: ring, cap, glass with flame, base, near-black outline. */
    static BufferedImage lampItem() {
        BufferedImage im = img(16, 16);
        // hanging ring
        for (int[] p : new int[][]{{7, 1}, {8, 1}, {6, 2}, {9, 2}, {6, 3}, {9, 3}}) px(im, p[0], p[1], COPPER[2]);
        px(im, 7, 1, COPPER[3]);
        // cap rows 4-5, x 4..11
        rect(im, 4, 4, 8, 1, COPPER[3]);
        rect(im, 4, 5, 8, 1, COPPER[2]);
        px(im, 11, 5, COPPER[1]);
        // glass x 5..10 rows 6-10
        for (int y = 6; y <= 10; y++)
            for (int x = 5; x <= 10; x++) px(im, x, y, x == 5 ? 0x6a8c98 : 0x34444c);
        // flame
        px(im, 7, 7, FLAME[3]); px(im, 7, 8, FLAME[4]); px(im, 8, 8, FLAME[3]);
        px(im, 7, 9, FLAME[4]); px(im, 8, 9, FLAME[4]); px(im, 7, 10, FLAME[4]); px(im, 8, 10, FLAME[3]);
        // base rows 11-13, x 3..12
        rect(im, 3, 11, 10, 1, COPPER[3]);
        rect(im, 3, 12, 10, 1, COPPER[2]);
        rect(im, 3, 13, 10, 1, COPPER[1]);
        BufferedImage out = outline(im);
        // snap outline colours to a coarse grid so they read as one near-black
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++)
                if (opaque(out, x, y) && !opaque(im, x, y)) {
                    int c = rgb(out, x, y);
                    px(out, x, y, ((((c >> 16) & 255) / 40 * 40) << 16) | ((((c >> 8) & 255) / 40 * 40) << 8) | ((c & 255) / 40 * 40));
                }
        return out;
    }

    // ---------------------------------------------------------------- ore cart

    static BufferedImage oreCart() {
        BufferedImage im = img(16, 16);
        // outer wall strip rows 0-7: dark spruce boards, iron rims and straps
        wood(im, 0, 0, 16, 8, false, 41, SPRUCE, 1, 3);
        vline(im, 5, 2, 5, SPRUCE[0]);
        vline(im, 10, 2, 5, SPRUCE[0]);
        metal(im, 0, 0, 16, 2, 42, IRON, 2, 4);
        hline(im, 0, 15, 0, IRON[4]);
        hline(im, 0, 15, 1, IRON[2]);
        metal(im, 0, 6, 16, 2, 43, IRON, 1, 3);
        hline(im, 0, 15, 6, IRON[3]);
        hline(im, 0, 15, 7, IRON[0]);
        for (int sx : new int[]{3, 11}) {
            rect(im, sx, 2, 2, 4, IRON[2]);
            vline(im, sx, 2, 5, IRON[3]);
            vline(im, sx + 1, 2, 5, IRON[1]);
            px(im, sx, 3, IRON[5]);
            px(im, sx + 1, 4, IRON[0]);
        }
        for (int[] rv : new int[][]{{1, 1}, {7, 1}, {14, 1}, {1, 6}, {7, 6}, {14, 6}}) px(im, rv[0], rv[1], IRON[5]);
        // interior rows 8-15: dark wet planks
        wood(im, 0, 8, 16, 8, true, 44, WET, 0, 2);
        hline(im, 0, 15, 11, WET[0]);
        hline(im, 0, 15, 14, WET[0]);
        hline(im, 0, 15, 8, WET[0]);
        hline(im, 0, 15, 12, WET[3]);
        hline(im, 0, 15, 9, WET[3]);
        hline(im, 0, 15, 15, WET[3]);
        for (int[] n : new int[][]{{2, 10}, {13, 13}, {7, 10}}) px(im, n[0], n[1], IRON[2]);
        return im;
    }

    static BufferedImage oreCartWheels() {
        BufferedImage im = img(16, 16);
        // wheel disc 5x5 at (0,0): rim, dark gaps, spokes, hub
        for (int y = 0; y < 5; y++)
            for (int x = 0; x < 5; x++) {
                double d = Math.hypot(x - 2, y - 2);
                if (d > 2.5) continue;
                int c;
                if (d < 0.5) c = IRON[0];
                else if (d > 1.9) c = (x + y < 4) ? IRON[4] : IRON[3];
                else if (x == 2 || y == 2) c = IRON[3];
                else c = IRON[1];
                px(im, x, y, c);
            }
        px(im, 0, 2, IRON[5]); px(im, 2, 0, IRON[5]);
        // tread 5x5 at (5,0)
        metal(im, 5, 0, 5, 5, 51, IRON, 2, 4);
        vline(im, 5, 0, 4, IRON[1]);
        // axle strip
        metal(im, 10, 5, 6, 1, 52, IRON, 1, 3);
        metal(im, 0, 5, 10, 1, 53, IRON, 1, 3);
        return im;
    }

    /** A heap of mixed raw ore lumps, tileable: neutral grey-brown, a few mineral flecks. */
    static BufferedImage oreHeap() {
        BufferedImage im = img(16, 16);
        Random r = new Random(61);
        int[][] idx = new int[16][16];
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) idx[y][x] = r.nextInt(3) == 0 ? 0 : 1;
        int[][] centres = {{2, 2}, {7, 1}, {12, 3}, {4, 6}, {10, 7}, {14, 10}, {1, 10}, {7, 12}, {12, 14}, {3, 14}, {8, 8}};
        for (int[] c : centres) {
            double rx = 1.7 + r.nextDouble() * 1.1, ry = 1.5 + r.nextDouble() * 1.0;
            for (int dy = -3; dy <= 3; dy++)
                for (int dx = -3; dx <= 3; dx++) {
                    double d = (dx * dx) / (rx * rx) + (dy * dy) / (ry * ry);
                    if (d > 1.0) continue;
                    int x = Math.floorMod(c[0] + dx, 16), y = Math.floorMod(c[1] + dy, 16);
                    int step = 2;
                    if (dx + dy <= -1 && d < 0.75) step = 3;
                    if (dx + dy <= -2 && d < 0.5 && r.nextBoolean()) step = 4;
                    if (dx + dy >= 2 || d > 0.8 && dx > 0 && dy > 0) step = 1;
                    idx[y][x] = step;
                }
        }
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) px(im, x, y, ORE[idx[y][x]]);
        // mineral flecks: malachite, hematite, native copper, cassiterite-pale
        int[][] mins = {{0x2e8058, 0x4caa78}, {0xa04a34, 0xc06a4a}, {0xc0652f, 0xe08848}, {0x8a8a6a, 0xb8b890}, {0x2e8058, 0x4caa78}, {0xc0652f, 0xe08848}};
        int[][] spots = {{5, 3}, {13, 5}, {2, 8}, {9, 10}, {14, 13}, {6, 14}};
        for (int i = 0; i < spots.length; i++) {
            int x = spots[i][0], y = spots[i][1];
            px(im, x, y, mins[i][1]);
            px(im, x + 1, y, mins[i][0]);
            if (i % 2 == 0) px(im, x, y + 1, mins[i][0]);
        }
        return im;
    }

    // ---------------------------------------------------------------- tool rack

    static BufferedImage toolRack() {
        BufferedImage im = img(16, 16);
        wood(im, 0, 0, 16, 16, false, 71, WEATHERED, 1, 3);
        // three boards 5, 5 and 6 wide with dark joints and a lit left edge
        for (int jx : new int[]{5, 10}) {
            vline(im, jx, 0, 15, WEATHERED[0]);
            vline(im, jx + 1, 0, 15, WEATHERED[4]);
        }
        vline(im, 0, 0, 15, WEATHERED[4]);
        vline(im, 15, 0, 15, WEATHERED[0]);
        hline(im, 0, 15, 0, WEATHERED[0]);
        hline(im, 0, 15, 15, WEATHERED[0]);
        // grey weathering streaks
        for (int[] g : new int[][]{{2, 3}, {3, 4}, {8, 9}, {13, 6}, {12, 11}, {7, 12}, {1, 13}})
            px(im, g[0], g[1], WEATHERED[4]);
        // nails near the top and bottom of each board
        for (int bx : new int[]{2, 8, 13})
            for (int ny : new int[]{2, 13}) {
                px(im, bx, ny, IRON[3]);
                px(im, bx + 1, ny + 1, WEATHERED[0]);
            }
        // shadow cast under each peg position (pegs at x 2-3, 7-8, 12-13; y 9-10 above the floor = rows 5-6)
        for (int px : new int[]{2, 7, 12}) {
            hline(im, px, px + 1, 7, WEATHERED[0]);
            px(im, px + 2, 7, WEATHERED[1]);
        }
        // knot
        px(im, 4, 9, WEATHERED[0]); px(im, 3, 9, WEATHERED[1]); px(im, 4, 10, WEATHERED[1]);
        return im;
    }

    // ---------------------------------------------------------------- cracked and mossy rock

    static BufferedImage load(String name) throws IOException {
        return ImageIO.read(OUT.resolve("block/" + name + ".png").toFile());
    }

    static BufferedImage cracked(String rock) throws IOException {
        BufferedImage src = load(rock);
        BufferedImage im = img(16, 16);
        im.getGraphics().drawImage(src, 0, 0, null);
        int darkest = -1;
        double dl = 9;
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int c = rgb(src, x, y);
                if (lum(c) < dl) { dl = lum(c); darkest = c; }
            }
        Random r = new Random(rock.hashCode() * 31L + 7);
        boolean[][] mark = new boolean[16][16];
        int cracks = 2;
        for (int k = 0; k < cracks; k++) {
            int x = 2 + r.nextInt(12), y = k == 0 ? r.nextInt(3) : 8 + r.nextInt(3);
            int dirx = r.nextBoolean() ? 1 : -1;
            int len = 9 + r.nextInt(4);
            for (int i = 0; i < len && y < 16 && x >= 0 && x < 16; i++) {
                mark[y][x] = true;
                int m = r.nextInt(4);
                if (m == 0) x += dirx; else if (m == 1) { y++; x += dirx; } else y++;
                if (r.nextInt(7) == 0) dirx = -dirx;
                if (r.nextInt(5) == 0 && x + dirx >= 0 && x + dirx < 16) mark[y < 16 ? y : 15][x + dirx] = true;
            }
        }
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) if (mark[y][x]) px(im, x, y, darkest);
        return im;
    }

    static BufferedImage mossy(String rock) throws IOException {
        BufferedImage src = load("cobbled_" + rock);
        BufferedImage im = img(16, 16);
        double min = 9, max = -1;
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double l = lum(rgb(src, x, y));
                min = Math.min(min, l);
                max = Math.max(max, l);
            }
        Random r = new Random(rock.hashCode() * 17L + 3);
        double[][] a = new double[4][4], b = new double[8][8];
        for (double[] row : a) for (int i = 0; i < 4; i++) row[i] = r.nextDouble();
        for (double[] row : b) for (int i = 0; i < 8; i++) row[i] = r.nextDouble();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                // top-left bias plus patches on the mid-edges (top middle, left middle, right middle)
                double bias = Math.max(0, 1.0 - (x + y) / 17.0);
                bias += 0.55 * patch(x, y, 8, 0, 4) + 0.5 * patch(x, y, 0, 8, 4) + 0.4 * patch(x, y, 15, 9, 3)
                        + 0.35 * patch(x, y, 10, 15, 3);
                double n = 0.5 * value(a, x / 4.0, y / 4.0) + 0.3 * value(b, x / 2.0, y / 2.0) + 0.2 * r.nextDouble();
                int c = rgb(src, x, y);
                if (bias * 1.25 + n * 0.75 > 1.02) {
                    double t = (lum(c) - min) / Math.max(1e-6, max - min);
                    int step = (int) Math.round(t * 4 + (r.nextInt(3) - 1) * 0.5);
                    c = MOSS[Math.max(0, Math.min(4, step))];
                }
                px(im, x, y, c);
            }
        return im;
    }

    /** Soft round patch weight at wrapped distance from (cx, cy). */
    static double patch(int x, int y, int cx, int cy, double radius) {
        double dx = Math.min(Math.abs(x - cx), 16 - Math.abs(x - cx)), dy = Math.min(Math.abs(y - cy), 16 - Math.abs(y - cy));
        return Math.max(0, 1 - Math.hypot(dx, dy) / radius);
    }

    /** Bilinear lookup in a wrapped grid. */
    static double value(double[][] g, double fx, double fy) {
        int n = g.length;
        int x0 = (int) Math.floor(fx), y0 = (int) Math.floor(fy);
        double tx = fx - x0, ty = fy - y0;
        double v00 = g[Math.floorMod(y0, n)][Math.floorMod(x0, n)], v10 = g[Math.floorMod(y0, n)][Math.floorMod(x0 + 1, n)];
        double v01 = g[Math.floorMod(y0 + 1, n)][Math.floorMod(x0, n)], v11 = g[Math.floorMod(y0 + 1, n)][Math.floorMod(x0 + 1, n)];
        return (v00 * (1 - tx) + v10 * tx) * (1 - ty) + (v01 * (1 - tx) + v11 * tx) * ty;
    }

    // ---------------------------------------------------------------- smouldering log pile

    static BufferedImage logPileSide() {
        BufferedImage im = img(16, 16);
        Random r = new Random(81);
        wood(im, 0, 0, 16, 13, true, 82, CHAR, 0, 3);
        int[][] courses = {{0, 3}, {5, 8}, {10, 12}};
        for (int[] c : courses) {
            hline(im, 0, 15, c[0], CHAR[4]);
            for (int x = 0; x < 16; x++) if (r.nextInt(3) == 0) px(im, x, c[0], CHAR[3]);
            hline(im, 0, 15, c[1], CHAR[0]);
        }
        // seams between courses glow
        for (int sy : new int[]{4, 9}) {
            hline(im, 0, 15, sy, 0x0a0908);
            for (int x = 0; x < 16; x++)
                if (r.nextInt(100) < 46) px(im, x, sy, EMBER[1 + r.nextInt(3)]);
        }
        // log ends, staggered, with embers in the joints
        int[][] ends = {{7, 0, 3}, {3, 5, 8}, {11, 5, 8}, {5, 10, 12}, {12, 10, 12}};
        for (int[] e : ends) {
            for (int y = e[1]; y <= e[2]; y++) {
                px(im, e[0], y, 0x0a0908);
                if (r.nextInt(100) < 45) px(im, e[0], y, EMBER[1 + r.nextInt(3)]);
                px(im, e[0] + 1, y, CHAR[1]);
            }
        }
        // bark cracks with a glint of fire in a few
        for (int i = 0; i < 9; i++) {
            int x = r.nextInt(16), y = 1 + r.nextInt(11);
            if (y == 4 || y == 9) continue;
            px(im, x, y, CHAR[0]);
            if (r.nextInt(100) < 25) px(im, x, y, EMBER[1]);
        }
        // sealed under dark earth and ash: rows 13-15 with a ragged top
        for (int y = 13; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int s = r.nextInt(4);
                px(im, x, y, EARTH[Math.min(3, s + (y == 13 ? 1 : 0))]);
                if (r.nextInt(100) < 9) px(im, x, y, ASH[2 + r.nextInt(2)]);
            }
        for (int x = 0; x < 16; x++) if (r.nextInt(4) == 0) px(im, x, 12, EARTH[2]);
        return im;
    }

    static BufferedImage logPileTop() {
        Random r = new Random(91);
        // static ash crust
        int[][] crust = new int[16][16];
        double[][] n = new double[16][16];
        for (double[] row : n) for (int i = 0; i < 16; i++) row[i] = r.nextDouble();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double v = 0.5 * n[y][x] + 0.25 * (n[y][Math.floorMod(x - 1, 16)] + n[y][(x + 1) % 16]);
                crust[y][x] = Math.max(0, Math.min(4, (int) ((v - 0.15) / 0.7 * 4.6) - (r.nextInt(5) == 0 ? 1 : 0)));
            }
        // ember cracks: three random walks and a few dots, each with its own pulse phase
        List<int[]> crack = new ArrayList<>();
        int[][] starts = {{3, 0, 1}, {13, 2, -1}, {7, 9, 1}};
        for (int w = 0; w < starts.length; w++) {
            int x = starts[w][0], y = starts[w][1], dir = starts[w][2];
            for (int i = 0; i < 12 && y < 16 && x >= 0 && x < 16; i++) {
                crack.add(new int[]{x, y, w});
                int m = r.nextInt(3);
                if (m == 0) x += dir; else if (m == 1) { y++; x += dir; } else y++;
                if (r.nextInt(4) == 0) dir = -dir;
            }
        }
        for (int i = 0; i < 6; i++) crack.add(new int[]{1 + r.nextInt(14), 1 + r.nextInt(14), 3});
        BufferedImage strip = img(16, 64);
        for (int f = 0; f < 4; f++) {
            BufferedImage fr = img(16, 16);
            for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) px(fr, x, y, ASH[crust[y][x]]);
            for (int[] c : crack) {
                double ph = c[2] * 1.3 + (c[0] + c[1]) * 0.12;
                double b = 0.5 + 0.5 * Math.sin(2 * Math.PI * f / 4.0 + ph);
                int idx = Math.max(1, (int) Math.round(b * 3.6));
                px(fr, c[0], c[1], EMBER[idx]);
                if (idx >= 4)
                    for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                        int nx = c[0] + d[0], ny = c[1] + d[1];
                        if (nx < 0 || ny < 0 || nx > 15 || ny > 15) continue;
                        if (crack.stream().anyMatch(o -> o[0] == nx && o[1] == ny)) continue;
                        px(fr, nx, ny, CHAR[1 + (nx + ny) % 2]);
                    }
            }
            strip.getGraphics().drawImage(fr, 0, 16 * f, null);
        }
        return strip;
    }

    // ---------------------------------------------------------------- windlass, drum, sluice

    static BufferedImage windlass() {
        BufferedImage im = img(16, 16);
        wood(im, 0, 0, 12, 14, false, 101, SPRUCE, 1, 4);
        for (int jx : new int[]{3, 7}) {
            vline(im, jx, 0, 13, SPRUCE[0]);
            vline(im, jx + 1, 0, 13, SPRUCE[5]);
        }
        vline(im, 11, 0, 13, SPRUCE[0]);
        vline(im, 0, 0, 13, SPRUCE[5]);
        // crossbeam strip rows 14-15, horizontal grain
        wood(im, 0, 14, 12, 2, true, 102, SPRUCE, 2, 5);
        hline(im, 0, 11, 15, SPRUCE[0]);
        hline(im, 0, 11, 14, SPRUCE[5]);
        px(im, 4, 5, SPRUCE[0]); px(im, 5, 6, SPRUCE[1]); px(im, 1, 10, SPRUCE[0]);
        // iron bracket plate x 12-15
        metal(im, 12, 0, 4, 16, 103, IRON, 1, 3);
        vline(im, 12, 0, 15, IRON[4]);
        vline(im, 15, 0, 15, IRON[0]);
        for (int y : new int[]{1, 6, 11, 14}) {
            px(im, 13, y, IRON[5]);
            px(im, 14, y + 1 > 15 ? 15 : y + 1, IRON[0]);
        }
        hline(im, 13, 14, 8, IRON[0]);
        hline(im, 13, 14, 3, IRON[4]);
        return im;
    }

    static BufferedImage windlassDrum() {
        BufferedImage im = img(16, 16);
        // rope coils in x 0-7, rows 0-11: four coils of three rows, each row twisted diagonally
        for (int c = 0; c < 4; c++) {
            for (int dy = 0; dy < 3; dy++) {
                int y = c * 3 + dy;
                for (int x = 0; x < 8; x++) {
                    int t = (x + y * 2) % 4;
                    int step = dy == 0 ? (t < 2 ? 4 : 3) : dy == 1 ? (t < 2 ? 3 : 2) : (t < 2 ? 1 : 0);
                    px(im, x, y, ROPE[step]);
                }
            }
        }
        // end grain wood (8,0)-(16,8): dark rings
        for (int y = 0; y < 8; y++)
            for (int x = 8; x < 16; x++) {
                double d = Math.hypot(x - 11.5, y - 3.5);
                int ring = ((int) Math.round(d)) % 2;
                int step = d > 3.4 ? 0 : (ring == 0 ? 2 : 1);
                if ((x * 7 + y * 13) % 11 == 0) step = Math.min(3, step + 1);
                px(im, x, y, SPRUCE[step + 1]);
            }
        // the rest: dark wood filler
        wood(im, 0, 12, 16, 4, true, 111, SPRUCE, 0, 2);
        wood(im, 8, 8, 8, 4, true, 112, SPRUCE, 0, 2);
        return im;
    }

    static BufferedImage sluiceBox() {
        BufferedImage im = img(16, 16);
        // wet bed x 0-9: planks running along v (3, 3 and 4 wide), rows 0-13; dark and damp
        wood(im, 0, 0, 10, 16, false, 121, WET, 0, 3);
        vline(im, 3, 0, 13, WET[0]);
        vline(im, 6, 0, 13, WET[0]);
        vline(im, 9, 0, 13, WET[1]);
        vline(im, 0, 0, 13, WET[1]);
        for (int[] s : new int[][]{{1, 3}, {4, 8}, {7, 5}, {2, 11}, {8, 12}}) px(im, s[0], s[1], WET[4]);
        // lighter riffle strip rows 14-15
        wood(im, 0, 14, 10, 2, true, 122, SPRUCE, 1, 3);
        hline(im, 0, 9, 14, SPRUCE[4]);
        hline(im, 0, 9, 15, SPRUCE[0]);
        // outer planks x 10-15 (vertical in the file, rotated 90 on the faces): two boards of three
        wood(im, 10, 0, 6, 16, false, 123, SPRUCE, 1, 3);
        vline(im, 13, 0, 15, SPRUCE[0]);
        vline(im, 10, 0, 15, SPRUCE[4]);
        vline(im, 14, 0, 15, SPRUCE[4]);
        vline(im, 12, 0, 15, SPRUCE[1]);
        vline(im, 15, 0, 15, SPRUCE[1]);
        for (int y : new int[]{2, 13}) {
            px(im, 11, y, IRON[3]);
            px(im, 14, y, IRON[3]);
        }
        return im;
    }

    // ---------------------------------------------------------------- preview

    static void preview() throws IOException {
        int s = 6, cell = 16 * s + 6, perRow = 10;
        int rows = (PREVIEW.size() + perRow - 1) / perRow;
        BufferedImage sheet = img(cell * perRow, cell * rows);
        Graphics2D g = sheet.createGraphics();
        g.setColor(new Color(0x34, 0x34, 0x3a));
        g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        for (int i = 0; i < PREVIEW.size(); i++) {
            int cx = (i % perRow) * cell + 3, cy = (i / perRow) * cell + 3;
            g.drawImage(PREVIEW.get(i).getScaledInstance(16 * s, 16 * s, java.awt.Image.SCALE_FAST), cx, cy, null);
        }
        g.dispose();
        File f = new File("build/texturegen/shared_blocks.png");
        f.getParentFile().mkdirs();
        ImageIO.write(sheet, "png", f);
    }
}
