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
 * Tier 6 (oil and plastics) textures: fluid buckets, bitumen, polyethylene and PVC pellets and sheets,
 * synthetic rubber, animated oil fluids and the gas tints drawn in tank gauges. Follows the style guide like
 * {@code TextureGen}: ramps only, light from the top-left, 1 px coloured outline on items.
 *
 * <p>Run from the repository root: {@code java tools/texturegen/Tier6Textures.java}. Output is deterministic
 * and goes to {@code src/main/resources/assets/strataindustria/textures}; a 6x preview is written to
 * {@code build/texturegen/tier6.png}.
 */
public final class Tier6Textures {
    static final Path OUT = Path.of("src/main/resources/assets/strataindustria/textures");

    // Ramps, dark to light, plus specular (0 = none).
    record Ramp(int spec, int... c) {
        int get(int step) { return c[Math.max(0, Math.min(4, step - 1))]; }
    }

    static final Ramp CRUDE = new Ramp(0x6e6478, 0x0c0a0c, 0x161214, 0x221c1e, 0x302828, 0x403638);
    static final Ramp BITUMEN = new Ramp(0, 0x141416, 0x1e1e22, 0x2a2a2e, 0x38383c, 0x48484c);
    static final Ramp NAPHTHA = new Ramp(0, 0x6e6440, 0x8e8456, 0xb0a670, 0xccc48c, 0xe4dcaa);
    static final Ramp DIESEL = new Ramp(0, 0x4e3010, 0x6e4618, 0x925e22, 0xb47a30, 0xd09a48);
    static final Ramp HEAVY = new Ramp(0, 0x0e0e0a, 0x1a1a12, 0x28281c, 0x383626, 0x4a4632);
    static final Ramp POLY = new Ramp(0, 0x7e8486, 0xa0a6a6, 0xc2c6c4, 0xdcdeda, 0xe8e8e4);
    static final Ramp PVC = new Ramp(0, 0x6e6a5e, 0x8e897a, 0xada896, 0xc8c4b2, 0xe0dccc);
    // No rubber ramp exists in TextureGen yet; dark warm grey-brown, low contrast.
    static final Ramp RUBBER = new Ramp(0, 0x1e1a18, 0x2c2724, 0x3c3531, 0x4e4540, 0x625852);
    // TextureGen.WROUGHT_IRON
    static final Ramp IRON = new Ramp(0xdcd8cf, 0x2a2b30, 0x45464c, 0x6a6a6c, 0x8e8c88, 0xb4b0a8);

    public static void main(String[] args) throws IOException {
        List<BufferedImage> items = new ArrayList<>();
        items.add(save("item/crude_oil_bucket", bucket(CRUDE, true)));
        items.add(save("item/naphtha_bucket", bucket(NAPHTHA, false)));
        items.add(save("item/diesel_bucket", bucket(DIESEL, false)));
        items.add(save("item/heavy_oil_bucket", bucket(HEAVY, false)));
        items.add(save("item/bitumen", bitumen()));
        items.add(save("item/polyethylene_pellet", pellets(POLY)));
        items.add(save("item/pvc_pellet", pellets(PVC)));
        items.add(save("item/polyethylene_sheet", sheet(true)));
        items.add(save("item/pvc_sheet", sheet(false)));
        items.add(save("item/synthetic_rubber", rubber()));

        List<BufferedImage> still = new ArrayList<>(), flow = new ArrayList<>(), gases = new ArrayList<>();
        // crude oil and heavy oil: thick, slow, opaque.
        still.add(oil("block/fluid/crude_oil_still", CRUDE, 16, 32, 3, 1, true));
        flow.add(oil("block/fluid/crude_oil_flow", CRUDE, 32, 32, 3, 2, true));
        still.add(oil("block/fluid/heavy_oil_still", HEAVY, 16, 32, 4, 3, false));
        flow.add(oil("block/fluid/heavy_oil_flow", HEAVY, 32, 32, 4, 4, false));
        still.add(thin("block/fluid/naphtha_still", NAPHTHA, 16, 16, 2, 0xA8, true));
        flow.add(thin("block/fluid/naphtha_flow", NAPHTHA, 32, 16, 2, 0xA8, true));
        still.add(thin("block/fluid/diesel_still", DIESEL, 16, 16, 2, 0xB4, false));
        flow.add(thin("block/fluid/diesel_flow", DIESEL, 32, 16, 2, 0xB4, false));

        int[] tints = {0xb8b0a0, 0xd8dcd0, 0xc8c0d4, 0xc4d0c8, 0xe0e4e4};
        String[] names = {"refinery_gas", "ethylene", "butadiene", "vinyl_chloride", "hydrogen_chloride"};
        for (int i = 0; i < names.length; i++) gases.add(save("block/fluid/" + names[i], gas(tints[i], 100 + i * 17)));

        writePreview(items, still, flow, gases);
    }

    // ---------------------------------------------------------------- helpers

    static BufferedImage img(int w, int h) { return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB); }

    static void px(BufferedImage im, int x, int y, int rgb) {
        if (x < 0 || y < 0 || x >= im.getWidth() || y >= im.getHeight()) return;
        im.setRGB(x, y, 0xFF000000 | rgb);
    }

    static boolean opaque(BufferedImage im, int x, int y) {
        if (x < 0 || y < 0 || x >= im.getWidth() || y >= im.getHeight()) return false;
        return (im.getRGB(x, y) >>> 24) != 0;
    }

    static int rgb(BufferedImage im, int x, int y) { return im.getRGB(x, y) & 0xFFFFFF; }

    /** Darker and slightly cooler, for outlines (same rule as TextureGen). */
    static int outlineOf(int c, boolean lit) {
        double f = lit ? 0.62 : 0.45;
        int r = Math.max((int) (((c >> 16) & 255) * f), 12);
        int g = Math.max((int) (((c >> 8) & 255) * f), 12);
        int b = Math.max((int) Math.min(255, (c & 255) * f + 6), 16);
        return (r << 16) | (g << 8) | b;
    }

    static BufferedImage outline(BufferedImage im) {
        BufferedImage out = img(16, 16);
        out.getGraphics().drawImage(im, 0, 0, null);
        int[][] dirs = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                if (opaque(im, x, y)) continue;
                for (int i = 0; i < 4; i++) {
                    int nx = x + dirs[i][0], ny = y + dirs[i][1];
                    if (opaque(im, nx, ny)) {
                        px(out, x, y, outlineOf(rgb(im, nx, ny), i < 2));
                        break;
                    }
                }
            }
        return out;
    }

    static BufferedImage save(String path, BufferedImage im) throws IOException {
        File f = OUT.resolve(path + ".png").toFile();
        f.getParentFile().mkdirs();
        ImageIO.write(im, "png", f);
        return im;
    }

    static void mcmeta(String path, int frametime) throws IOException {
        Files.writeString(OUT.resolve(path + ".png.mcmeta"),
                "{\"animation\":{\"frametime\":" + frametime + ",\"interpolate\":false}}\n");
    }

    /** Pixel map: digits 1-5 ramp a, letters a-e ramp b, 'S' b's sheen, '.' transparent. Outline added. */
    static BufferedImage map(Ramp a, Ramp b, String[] rows) {
        BufferedImage im = img(16, 16);
        int top = (16 - rows.length) / 2;
        for (int y = 0; y < rows.length; y++) {
            if (rows[y].length() != 16) throw new IllegalStateException("row width " + rows[y]);
            for (int x = 0; x < 16; x++) {
                char ch = rows[y].charAt(x);
                if (ch >= '1' && ch <= '5') px(im, x, top + y, a.get(ch - '0'));
                else if (ch >= 'a' && ch <= 'e') px(im, x, top + y, b.get(ch - 'a' + 1));
                else if (ch == 'S') px(im, x, top + y, b.spec());
            }
        }
        return outline(im);
    }

    // ---------------------------------------------------------------- buckets

    /** Same silhouette as TextureGen.CREOSOTE_BUCKET: digits iron, letters fluid. */
    static final String[] BUCKET = {
            "................",
            ".....222222.....",
            "....2......2....",
            "...4555555554...",
            "..45ccddccbc54..",
            "..4bbbcbbbbb43..",
            "..355444444432..",
            "...4554444332...",
            "...4544443322...",
            "....44443322....",
            "....43333221....",
            ".....322211.....",
    };

    static BufferedImage bucket(Ramp fluid, boolean sheen) {
        String[] rows = BUCKET.clone();
        if (sheen) rows[4] = "..45ccdSccbc54..";
        return map(IRON, fluid, rows);
    }

    // ---------------------------------------------------------------- bitumen

    static final String[] BITUMEN_ART = {
            "................",
            "................",
            "......344443....",
            "....34555544332.",
            "...3455222443322",
            "..34552222244322",
            "..34522222234432",
            "..34452222334432",
            ".3444433333332..",
            ".334443333322...",
            "..33333333222...",
            "...333322221....",
            ".....2222211....",
            "................",
            "................",
            "................",
    };

    /** Glossy black lump; the broken face in the upper left is a smooth concave scoop with a lit rim. */
    static BufferedImage bitumen() {
        BufferedImage im = img(16, 16);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                char ch = BITUMEN_ART[y].charAt(x);
                if (ch >= '1' && ch <= '5') px(im, x, y, BITUMEN.get(ch - '0'));
            }
        // conchoidal ridges inside the face: one curved step-3 arc, one step-1 shadow tucked under the rim
        px(im, 6, 6, BITUMEN.get(3)); px(im, 7, 7, BITUMEN.get(3)); px(im, 8, 7, BITUMEN.get(3));
        px(im, 6, 5, BITUMEN.get(1)); px(im, 7, 5, BITUMEN.get(1));
        // gloss: a short lit streak on the lower right shoulder, one step up
        px(im, 11, 8, BITUMEN.get(4)); px(im, 11, 9, BITUMEN.get(4));
        return outline(im);
    }

    // ---------------------------------------------------------------- pellets

    /** Heap of 3x3 pellets, each lit top-left. Back rows first so front pellets overlap them. */
    static BufferedImage pellets(Ramp r) {
        BufferedImage im = img(16, 16);
        int[][] heap = {
                {7, 3, 0}, {5, 5, 1}, {8, 5, 0},
                {3, 7, 0}, {6, 7, 1}, {9, 7, 0}, {11, 6, 1},
                {2, 9, 1}, {5, 9, 0}, {8, 9, 1}, {11, 9, 0},
                {3, 11, 0}, {6, 11, 1}, {9, 11, 0}, {12, 11, 1},
        };
        for (int[] p : heap) pellet(im, p[0], p[1], r, p[2]);
        return outline(im);
    }

    /** One round pellet: lit top-left pixel, darker bottom-right, cut corners. {@code dim} drops a step. */
    static void pellet(BufferedImage im, int x, int y, Ramp r, int dim) {
        int[][] steps = {
                {0, 4, 3},
                {4, 3, 2},
                {3, 2, 0},
        };
        for (int j = 0; j < 3; j++)
            for (int i = 0; i < 3; i++) {
                int s = steps[j][i];
                if (s == 0) continue;
                px(im, x + i, y + j, r.get(Math.max(1, s - dim)));
            }
        px(im, x, y, r.get(Math.max(2, 5 - dim)));
        px(im, x + 2, y + 2, r.get(Math.max(1, 3 - dim)));
        px(im, x + 1, y + 2, r.get(Math.max(1, 2 - dim)));
    }

    // ---------------------------------------------------------------- sheets

    /**
     * A folded sheet seen at an angle: a flap folded back along the top and the main sheet under it.
     * Polyethylene is milky (light centre, darker translucent rim, ghost of the fold showing through);
     * PVC is opaque cream with a hard shaded fold.
     */
    static BufferedImage sheet(boolean poly) {
        Ramp r = poly ? POLY : PVC;
        BufferedImage im = img(16, 16);
        int len = 11;
        for (int y = 2; y <= 12; y++) {
            int s = 4 - (y - 2) / 3;
            boolean flap = y <= 4, crease = y == 5, bottom = y == 12;
            int x0 = flap ? s + 1 : s;
            for (int i = 0; i < len; i++) {
                int x = x0 + i;
                boolean first = i == 0, last = i == len - 1;
                int step;
                if (poly) {
                    if (flap) step = y == 2 ? 3 : 4;
                    else if (crease) step = 2;
                    else step = 4;
                    // milky centre: lighter in the middle, rim a step darker
                    if (!flap && !crease && y >= 7 && y <= 10 && i >= 3 && i <= len - 4) step = 5;
                    if (!flap && !crease && (first || y == 6)) step = 3;
                    if (last || bottom) step = 2;
                    if (first && !flap) step = 3;
                    if (!flap && !crease && !bottom && i == len - 2) step = 3;
                } else {
                    if (flap) step = y == 2 ? 5 : 4;
                    else if (crease) step = 2;
                    else step = 3;
                    if (!flap && !crease && (first || y == 6)) step = 4;
                    if (last || bottom) step = 2;
                    if (!flap && !crease && !bottom && i == len - 2) step = 2;
                    if (bottom && first) step = 3;
                }
                px(im, x, y, r.get(step));
            }
        }
        if (poly) {
            // fold ghosting through the milky film: a faint step-3 diagonal
            px(im, 5, 8, r.get(3)); px(im, 6, 8, r.get(3)); px(im, 7, 9, r.get(3)); px(im, 8, 9, r.get(3));
        } else {
            // a wrinkle in the opaque sheet
            px(im, 8, 8, r.get(2)); px(im, 9, 8, r.get(2)); px(im, 6, 9, r.get(4)); px(im, 7, 9, r.get(4));
            px(im, 9, 9, r.get(2));
        }
        return outline(im);
    }

    // ---------------------------------------------------------------- synthetic rubber

    /** Square bale seen from above-front: top face with a pressed 3x3 grid, front face with a pressed groove. */
    static BufferedImage rubber() {
        BufferedImage im = img(16, 16);
        // top face x 2..13, y 3..8; front face y 9..13
        for (int y = 3; y <= 13; y++)
            for (int x = 2; x <= 13; x++) {
                boolean top = y <= 8;
                int step = top ? 4 : 3;
                if (top) {
                    if (y == 3 || x == 2) step = 5;
                    if (x == 13 || y == 8) step = 3;
                } else {
                    if (x == 2) step = 4;
                    if (x == 13 || y == 13) step = 2;
                }
                px(im, x, y, RUBBER.get(step));
            }
        // pressed grid on the top: grooves at x=6 and x=10, y=5 and y=7 are rows; lit edge below/right of groove
        for (int x = 3; x <= 12; x++) {
            px(im, x, 5, RUBBER.get(2));
            if (x < 12) px(im, x, 6, RUBBER.get(4));
        }
        for (int y = 4; y <= 7; y++) {
            for (int gx : new int[]{6, 10}) {
                px(im, gx, y, RUBBER.get(2));
                if (y != 5) px(im, gx + 1, y, RUBBER.get(5 - 0));
            }
        }
        // front face: one pressed groove and a dark lower band
        for (int x = 3; x <= 12; x++) {
            px(im, x, 11, RUBBER.get(2));
            px(im, x, 12, RUBBER.get(x == 3 ? 3 : 3));
        }
        px(im, 3, 10, RUBBER.get(4)); px(im, 4, 10, RUBBER.get(4));
        return outline(im);
    }

    // ---------------------------------------------------------------- fluid helpers

    /** Periodic value noise on a lattice of gx x gy cells, smooth-interpolated. */
    static final class Lattice {
        final double[][] v;
        final int gx, gy;
        Lattice(long seed, int gx, int gy) {
            this.gx = gx; this.gy = gy;
            Random r = new Random(seed);
            v = new double[gy][gx];
            for (double[] row : v) for (int i = 0; i < gx; i++) row[i] = r.nextDouble();
        }
        double at(double u, double w) {
            u = ((u % gx) + gx) % gx; w = ((w % gy) + gy) % gy;
            int x0 = (int) Math.floor(u), y0 = (int) Math.floor(w);
            double fx = u - x0, fy = w - y0;
            fx = fx * fx * (3 - 2 * fx); fy = fy * fy * (3 - 2 * fy);
            int x1 = (x0 + 1) % gx, y1 = (y0 + 1) % gy;
            double a = v[y0][x0] + (v[y0][x1] - v[y0][x0]) * fx;
            double b = v[y1][x0] + (v[y1][x1] - v[y1][x0]) * fx;
            return a + (b - a) * fy;
        }
    }

    /** Value at which a fraction {@code q} of the samples lie below. */
    static double quantile(double[] all, double q) {
        double[] s = all.clone();
        Arrays.sort(s);
        return s[Math.min(s.length - 1, (int) (q * s.length))];
    }

    static int[] despeckle(int[] g, int p) {
        int[] out = g.clone();
        for (int y = 0; y < p; y++)
            for (int x = 0; x < p; x++) {
                int c = g[y * p + x];
                int l = g[y * p + (x + p - 1) % p], r = g[y * p + (x + 1) % p];
                int u = g[((y + p - 1) % p) * p + x], d = g[((y + 1) % p) * p + x];
                if (c != l && c != r && c != u && c != d) out[y * p + x] = (l == r || l == u || l == d) ? l : (r == u || r == d) ? r : u;
            }
        return out;
    }

    static int tint(int c, double f) {
        int r = (int) Math.max(0, Math.min(255, ((c >> 16) & 255) * f));
        int g = (int) Math.max(0, Math.min(255, ((c >> 8) & 255) * f));
        int b = (int) Math.max(0, Math.min(255, (c & 255) * f));
        return (r << 16) | (g << 8) | b;
    }

    static BufferedImage strip(int[][][] frames, int p, int[] palette, int alpha) {
        BufferedImage im = img(p, p * frames.length);
        for (int t = 0; t < frames.length; t++)
            for (int y = 0; y < p; y++)
                for (int x = 0; x < p; x++) {
                    int idx = frames[t][0][y * p + x];
                    im.setRGB(x, t * p + y, (alpha << 24) | palette[idx]);
                }
        return im;
    }

    // ---------------------------------------------------------------- thick oils (crude, heavy)

    /**
     * Thick, slow oil: two counter-drifting folds layers shaded top-left, quantised on global quantiles so
     * frames do not flicker. Crude gets short sheen streaks drifting faster. Every layer moves a whole
     * number of periods over the loop, so the last frame flows into the first.
     */
    static BufferedImage oil(String path, Ramp ramp, int p, int n, int frametime, int seed, boolean sheen) throws IOException {
        double[][] h = new double[n][p * p], v = new double[n][p * p], sh = new double[n][p * p];
        Lattice a = new Lattice(seed * 31L + 1, p / 8, p / 4);   // broad horizontal folds
        Lattice b = new Lattice(seed * 31L + 2, p / 4, p / 4);
        Lattice s = new Lattice(seed * 31L + 3, p / 4, p / 2);
        double slow = p / (double) n, fast = 2 * p / (double) n;
        boolean flow = p == 32;
        for (int t = 0; t < n; t++) {
            for (int y = 0; y < p; y++)
                for (int x = 0; x < p; x++) {
                    double dy = flow ? slow * 2 : slow;   // flow texture runs downward, the still one just creeps
                    double ha = a.at((x + 0.5 - slow * t * 0.5) / 8.0, (y + 0.5 - dy * t * 0.5) / 4.0);
                    double hb = b.at((x + 0.5 + slow * t) / 4.0, (y + 0.5 - (flow ? slow * t : -slow * t * 0.5)) / 4.0);
                    h[t][y * p + x] = 0.65 * ha + 0.35 * hb;
                    sh[t][y * p + x] = s.at((x + 0.5 - fast * t) / 4.0, (y + 0.5 - (flow ? fast * t : 0)) / 2.0);
                }
        }
        // light from the top-left: pixels whose surface falls away towards the lower right catch light
        for (int t = 0; t < n; t++)
            for (int y = 0; y < p; y++)
                for (int x = 0; x < p; x++) {
                    double self = h[t][y * p + x];
                    double lowerRight = h[t][((y + 1) % p) * p + (x + 1) % p];
                    v[t][y * p + x] = self + 0.9 * (self - lowerRight);
                }
        double[] all = new double[n * p * p], sall = new double[n * p * p];
        for (int t = 0; t < n; t++) {
            System.arraycopy(v[t], 0, all, t * p * p, p * p);
            System.arraycopy(sh[t], 0, sall, t * p * p, p * p);
        }
        double q1 = quantile(all, 0.22), q2 = quantile(all, 0.55), q3 = quantile(all, 0.86);
        double qs = quantile(sall, 0.975);
        int[][][] frames = new int[n][1][];
        int[] palette = {ramp.get(1), ramp.get(2), ramp.get(3), ramp.get(4), sheen ? ramp.spec() : ramp.get(5)};
        for (int t = 0; t < n; t++) {
            int[] g = new int[p * p];
            for (int i = 0; i < g.length; i++) g[i] = v[t][i] < q1 ? 0 : v[t][i] < q2 ? 1 : v[t][i] < q3 ? 2 : 3;
            g = despeckle(g, p);
            // sheen: streaks on the lit folds only, always at least two pixels wide
            boolean[] on = new boolean[p * p];
            for (int i = 0; i < g.length; i++) on[i] = sh[t][i] > qs && g[i] >= 2;
            boolean[] add = new boolean[p * p];
            for (int y = 0; y < p; y++)
                for (int x = 0; x < p; x++) {
                    if (!on[y * p + x]) continue;
                    boolean nb = on[y * p + (x + 1) % p] || on[y * p + (x + p - 1) % p]
                            || on[((y + 1) % p) * p + x] || on[((y + p - 1) % p) * p + x];
                    if (!nb) add[y * p + (x + 1) % p] = true;
                }
            // heavy oil has no specular: its step 5 highlights are rarer and only 2 px streaks
            for (int i = 0; i < g.length; i++) if (on[i] || add[i]) g[i] = 4;
            frames[t][0] = g;
        }
        BufferedImage im = strip(frames, p, palette, 0xFF);
        save(path, im);
        mcmeta(path, frametime);
        return im.getSubimage(0, 0, p, p);
    }

    // ---------------------------------------------------------------- thin fluids (naphtha, diesel)

    /**
     * Sum of travelling waves (integer wave numbers, so the tile and the loop are both seamless) plus a slowly
     * drifting noise to break the regularity. Partial alpha, as fluids may.
     */
    static BufferedImage thin(String path, Ramp ramp, int p, int n, int frametime, int alpha, boolean quick) throws IOException {
        int sc = p / 16;
        int[][] waves = quick
                ? new int[][]{{2, 1, 1}, {-1, 2, 2}, {3, -2, 3}}
                : new int[][]{{1, 1, 1}, {-1, 2, 1}, {2, -1, 2}};
        double[] amp = quick ? new double[]{1.0, 0.8, 0.6} : new double[]{1.0, 0.7, 0.4};
        Lattice nz = new Lattice(quick ? 77 : 91, p / 4, p / 4);
        double[][] v = new double[n][p * p];
        double[] all = new double[n * p * p];
        for (int t = 0; t < n; t++)
            for (int y = 0; y < p; y++)
                for (int x = 0; x < p; x++) {
                    double sum = 0;
                    for (int k = 0; k < waves.length; k++) {
                        // wave numbers scale with the tile so flow and still ripple at the same pixel size
                        double ph = 2 * Math.PI * sc * (waves[k][0] * x + waves[k][1] * y) / p
                                - 2 * Math.PI * waves[k][2] * t / n + k * 1.7;
                        sum += amp[k] * Math.sin(ph);
                    }
                    double drift = nz.at((x + 0.5 - (p / (double) n) * t * (quick ? 1 : 0.5)) / 4.0, (y + 0.5 - (p / (double) n) * t * (quick ? 1 : 0.5)) / 4.0);
                    double val = sum * 0.35 + (drift - 0.5) * (quick ? 0.9 : 1.1);
                    v[t][y * p + x] = val;
                    all[t * p * p + y * p + x] = val;
                }
        double[] qs = quick ? new double[]{quantile(all, 0.18), quantile(all, 0.55), quantile(all, 0.88)}
                : new double[]{quantile(all, 0.12), quantile(all, 0.55), quantile(all, 0.9)};
        int[][][] frames = new int[n][1][];
        int[] palette = {ramp.get(2), ramp.get(3), ramp.get(4), ramp.get(5)};
        for (int t = 0; t < n; t++) {
            int[] g = new int[p * p];
            for (int i = 0; i < g.length; i++) g[i] = v[t][i] < qs[0] ? 0 : v[t][i] < qs[1] ? 1 : v[t][i] < qs[2] ? 2 : 3;
            frames[t][0] = despeckle(g, p);
        }
        BufferedImage im = strip(frames, p, palette, alpha);
        save(path, im);
        mcmeta(path, frametime);
        return im.getSubimage(0, 0, p, p);
    }

    // ---------------------------------------------------------------- gases

    /** Soft swirls in three close tints of the gas colour; domain-warped periodic noise, tiles seamlessly. */
    static BufferedImage gas(int tint, long seed) {
        Lattice warpX = new Lattice(seed, 4, 4), warpY = new Lattice(seed + 1, 4, 4), base = new Lattice(seed + 2, 4, 4);
        double[] val = new double[256];
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double u = x / 4.0 + 2.0 * (warpX.at(x / 4.0, y / 4.0) - 0.5);
                double w = y / 4.0 + 2.0 * (warpY.at(x / 4.0, y / 4.0) - 0.5);
                val[y * 16 + x] = base.at(u, w);
            }
        double q1 = quantile(val, 0.30), q2 = quantile(val, 0.72);
        int[] g = new int[256];
        for (int i = 0; i < 256; i++) g[i] = val[i] < q1 ? 0 : val[i] < q2 ? 1 : 2;
        g = despeckle(g, 16);
        int[] pal = {tint(tint, 0.88), tint, tint(tint, 1.07)};
        BufferedImage im = img(16, 16);
        for (int i = 0; i < 256; i++) im.setRGB(i % 16, i / 16, (0xE6 << 24) | pal[g[i]]);
        return im;
    }

    // ---------------------------------------------------------------- preview

    static void writePreview(List<BufferedImage> items, List<BufferedImage> still, List<BufferedImage> flow,
                             List<BufferedImage> gases) throws IOException {
        int s = 6, cell = 16 * s + 8;
        BufferedImage sheet = img(cell * 10, cell * 4);
        var g = sheet.createGraphics();
        g.setColor(new java.awt.Color(0x6a, 0x6a, 0x72));
        g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        for (int i = 0; i < items.size(); i++) g.drawImage(items.get(i).getScaledInstance(16 * s, 16 * s, java.awt.Image.SCALE_FAST), i * cell + 4, 4, null);
        // fluids: 2x2 tiled stills at 4x, then flows at 3x
        int x = 4, y = cell + 4;
        for (BufferedImage im : still) {
            for (int ty = 0; ty < 2; ty++) for (int tx = 0; tx < 2; tx++)
                g.drawImage(im.getScaledInstance(64, 64, java.awt.Image.SCALE_FAST), x + tx * 64, y + ty * 64, null);
            x += 136;
        }
        x = 4; y = cell + 4 + 136;
        for (BufferedImage im : flow) {
            g.drawImage(im.getScaledInstance(96, 96, java.awt.Image.SCALE_FAST), x, y, null);
            x += 104;
        }
        x = 4; y = cell * 3 + 4;
        for (BufferedImage im : gases) {
            for (int ty = 0; ty < 2; ty++) for (int tx = 0; tx < 2; tx++)
                g.drawImage(im.getScaledInstance(64, 64, java.awt.Image.SCALE_FAST), x + tx * 64, y + ty * 64, null);
            x += 136;
        }
        g.dispose();
        // items at 12x for hand checking
        BufferedImage zoom = img(items.size() * 200 + 8, 208);
        var zg = zoom.createGraphics();
        zg.setColor(new java.awt.Color(0x6a, 0x6a, 0x72));
        zg.fillRect(0, 0, zoom.getWidth(), zoom.getHeight());
        for (int i = 0; i < items.size(); i++) zg.drawImage(items.get(i).getScaledInstance(192, 192, java.awt.Image.SCALE_FAST), 8 + i * 200, 8, null);
        zg.dispose();
        ImageIO.write(zoom, "png", new File("build/texturegen/tier6_items.png"));
        File f = new File("build/texturegen/tier6.png");
        f.getParentFile().mkdirs();
        ImageIO.write(sheet, "png", f);
    }
}
