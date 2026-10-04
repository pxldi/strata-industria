import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.imageio.ImageIO;

/**
 * Procedural texture generator for Strata Industria, following docs/art/STYLE_GUIDE.md.
 *
 * <p>Run from the repository root: {@code java tools/texturegen/TextureGen.java}. It writes every
 * generated texture into {@code src/main/resources/assets/strataindustria/textures} and a scaled
 * preview sheet into {@code build/texturegen/preview.png}. Output is deterministic.
 *
 * <p>Hand-made textures live next to generated ones; a file listed in {@link #HAND_MADE} is never
 * overwritten.
 */
public final class TextureGen {
    static final Path OUT = Path.of("src/main/resources/assets/strataindustria/textures");
    static final Map<String, BufferedImage> PREVIEW = new LinkedHashMap<>();
    static final java.util.Set<String> HAND_MADE = java.util.Set.of();

    // ---------------------------------------------------------------- palette (5-step ramps)

    record Ramp(int[] c, int spec) {
        int get(int step) { return c[Math.max(0, Math.min(4, step - 1))]; }
    }

    static Ramp ramp(int spec, int... c) { return new Ramp(c, spec); }

    // Rocks. The *_V2 ramps (style guide v2, vanilla value band) colour the terrain blocks and ores; the
    // old ramps below stay for machines and items that have not been reworked yet (lane A, chunks A2+).
    static final Ramp GRANITE = ramp(0, 0x55494c, 0x6c5e60, 0x83726f, 0x978480, 0xad9a92);
    static final Ramp BASALT = ramp(0, 0x2c2e34, 0x383b42, 0x45484f, 0x53565d, 0x64676c);
    static final Ramp LIMESTONE = ramp(0, 0x8c8676, 0xa19a88, 0xb2ab97, 0xc1baa5, 0xd0cab6);
    static final Ramp SHALE = ramp(0, 0x40444c, 0x50555e, 0x5f656e, 0x6f757e, 0x80868e);
    static final Ramp SLATE = ramp(0, 0x3e4642, 0x4e5752, 0x5e6862, 0x6f7a73, 0x818c85);
    static final Ramp MARBLE = ramp(0, 0xa09e9a, 0xb8b6b0, 0xcac8c2, 0xdad8d2, 0xe8e6e0);
    static final Ramp GABBRO = ramp(0, 0x262830, 0x33363b, 0x41444a, 0x50535a, 0x61646b);
    static final Ramp RHYOLITE = ramp(0, 0x7a625c, 0x917872, 0xa38a80, 0xb59d92, 0xc6b0a4);
    static final Ramp GRANITE_V2 = ramp(0, 0x55494c, 0x6c5e60, 0x83726f, 0x978480, 0xad9a92);
    static final Ramp BASALT_V2 = ramp(0, 0x2c2e34, 0x383b42, 0x45484f, 0x53565d, 0x64676c);
    static final Ramp LIMESTONE_V2 = ramp(0, 0x8c8676, 0xa19a88, 0xb2ab97, 0xc1baa5, 0xd0cab6);
    static final Ramp SHALE_V2 = ramp(0, 0x40444c, 0x50555e, 0x5f656e, 0x6f757e, 0x80868e);
    static final Ramp SLATE_V2 = ramp(0, 0x3e4642, 0x4e5752, 0x5e6862, 0x6f7a73, 0x818c85);
    static final Ramp MARBLE_V2 = ramp(0, 0xa09e9a, 0xb8b6b0, 0xcac8c2, 0xdad8d2, 0xe8e6e0);
    static final Ramp GABBRO_V2 = ramp(0, 0x262830, 0x33363b, 0x41444a, 0x50535a, 0x61646b);
    static final Ramp RHYOLITE_V2 = ramp(0, 0x7a625c, 0x917872, 0xa38a80, 0xb59d92, 0xc6b0a4);
    static final Ramp FLINT = ramp(0xe4e8ee, 0x2c2e36, 0x484c58, 0x6a707e, 0x9096a4, 0xbcc2cc);
    // Metals
    static final Ramp COPPER = ramp(0xffe2bc, 0x4a2214, 0x82401f, 0xb85f2f, 0xe08040, 0xf4a868);
    static final Ramp TIN = ramp(0xffffff, 0x383c48, 0x5e6474, 0x8890a0, 0xb4bcc8, 0xdce2ea);
    static final Ramp BRONZE = ramp(0xfff0b8, 0x3a2510, 0x6b4a1c, 0x9c7030, 0xc8983e, 0xe8c060);
    static final Ramp BISMUTH = ramp(0xfaf4fc, 0x403648, 0x665870, 0x8e7e98, 0xb4a4bc, 0xd4c8da);
    static final Ramp ARSENICAL_BRONZE = ramp(0xf6d2b4, 0x3e2216, 0x6a3e24, 0x93583a, 0xb87852, 0xd89c70);
    static final Ramp BISMUTH_BRONZE = ramp(0xf2dcc4, 0x3e2a1e, 0x694834, 0x91694c, 0xb48a68, 0xd2ac88);
    static final Ramp SLAG = ramp(0xc8bcb0, 0x38302c, 0x58504a, 0x7a6e66, 0x988c82, 0xb4a89c);
    // Minerals
    static final Ramp MALACHITE = ramp(0, 0x163828, 0x1e4a36, 0x2e6b4a, 0x4a9466, 0x78bf8a);
    static final Ramp CASSITERITE = ramp(0xc8b8a8, 0x1e1614, 0x2a1e1a, 0x4a342a, 0x6e5444, 0x9a8070);
    static final Ramp TENNANTITE = ramp(0xc6d0d6, 0x1a1e24, 0x2a3038, 0x404852, 0x5c6672, 0x84909a);
    static final Ramp BISMUTHINITE = ramp(0xe2e6ee, 0x26282e, 0x3a3e48, 0x565c6a, 0x7a8292, 0xa4acba);
    // Organics and ceramics
    static final Ramp WOOD = ramp(0, 0x3f2f1c, 0x594328, 0x735a35, 0x8c7043, 0xa48854);
    static final Ramp FIBRE = ramp(0, 0x38441c, 0x4e6028, 0x6a7e34, 0x8aa044, 0xaec25e);
    static final Ramp STRAW = ramp(0, 0x6a5624, 0x8a7430, 0xb09a42, 0xd0b858, 0xe8d476);
    static final Ramp CLAY = ramp(0, 0x5a5254, 0x7a7072, 0x9c9090, 0xbcb0ae, 0xd6cac6);
    static final Ramp CERAMIC = ramp(0xeab08a, 0x4e2a22, 0x7c4234, 0xa65c46, 0xc47c5c, 0xdc9e78);
    static final Ramp CHARCOAL = ramp(0, 0x141416, 0x232327, 0x34343a, 0x4a4a52, 0x626270);
    static final Ramp ASH = ramp(0, 0x5a5856, 0x787572, 0x98948f, 0xb8b4ae, 0xd4d0ca);
    static final Ramp LEATHER = ramp(0, 0x3a2216, 0x553322, 0x744834, 0x92614a, 0xb07e62);
    static final Ramp PAPER = ramp(0, 0x6e6250, 0x948670, 0xb8aa8e, 0xd6caae, 0xece2c8);

    record Rock(String name, Ramp ramp, String category) {}

    static final List<Rock> ROCKS = List.of(
            new Rock("limestone", LIMESTONE_V2, "sedimentary"),
            new Rock("shale", SHALE_V2, "sedimentary"),
            new Rock("slate", SLATE_V2, "metamorphic"),
            new Rock("marble", MARBLE_V2, "metamorphic"),
            new Rock("granite", GRANITE_V2, "intrusive"),
            new Rock("gabbro", GABBRO_V2, "intrusive"),
            new Rock("basalt", BASALT_V2, "extrusive"),
            new Rock("rhyolite", RHYOLITE_V2, "extrusive"));

    record Mineral(String name, Ramp ramp, boolean metallic) {}

    // Mineral ramps for ore blocks (style guide v2: brighter and more saturated than the host rock).
    // They also colour the ore items built from {@code Mineral}; the other ramps above are untouched.
    static final Ramp ORE_COPPER = ramp(0xfad2a6, 0x5a2a18, 0x8e4426, 0xc0652f, 0xe08848, 0xf2b07a);
    static final Ramp ORE_MALACHITE = ramp(0, 0x123a2a, 0x1d5a3e, 0x2e8058, 0x4caa78, 0x86d4a0);
    static final Ramp ORE_TENNANTITE = ramp(0xe0eaf0, 0x2a3038, 0x4a5560, 0x6c7a88, 0x92a2b0, 0xbccad4);
    static final Ramp ORE_CASSITERITE = ramp(0xe4cca8, 0x2a1c16, 0x4a3226, 0x76523a, 0xa07a58, 0xc8a47c);
    static final Ramp ORE_BISMUTHINITE = ramp(0xeef0fa, 0x3a3e4c, 0x5a6074, 0x8088a0, 0xa8b0c6, 0xcdd4e4);
    static final Ramp ORE_HEMATITE = ramp(0, 0x4a1e18, 0x7a3226, 0xa04a34, 0xc06a4a, 0xd89070);
    static final Ramp ORE_MAGNETITE = ramp(0xc8d0e0, 0x1c1e28, 0x343846, 0x505668, 0x707890, 0x98a0b8);
    static final Ramp ORE_GOLD = ramp(0xfff6c0, 0x5a3a10, 0x94641a, 0xc8921e, 0xe8bc34, 0xf8e070);
    static final Ramp ORE_LIMONITE = ramp(0, 0x4a2c0e, 0x7a4a18, 0xa8701e, 0xc89030, 0xe0b050);
    static final Ramp ORE_SPHALERITE = ramp(0, 0x2a1810, 0x4e2c14, 0x7a4a1a, 0xa8701e, 0xd09a36);
    static final Ramp ORE_GALENA = ramp(0xeef2f8, 0x30343e, 0x525a68, 0x7a8494, 0xa0aab8, 0xc8d0dc);
    static final Ramp ORE_COAL = ramp(0, 0x1c1c22, 0x2e2e36, 0x464650, 0x666674, 0x8e8e9e);
    static final Ramp ORE_SULFUR = ramp(0, 0x5a5214, 0x867a1c, 0xb4a428, 0xd8c83c, 0xf0e46a);


    static final List<Mineral> MINERALS = List.of(
            new Mineral("native_copper", ORE_COPPER, true),
            new Mineral("malachite", ORE_MALACHITE, false),
            new Mineral("tennantite", ORE_TENNANTITE, true),
            new Mineral("cassiterite", ORE_CASSITERITE, true),
            new Mineral("bismuthinite", ORE_BISMUTHINITE, true));

    record Metal(String name, Ramp ramp, boolean vanillaIngot) {}

    static final List<Metal> METALS = List.of(
            new Metal("copper", COPPER, true),
            new Metal("tin", TIN, false),
            new Metal("bismuth", BISMUTH, false),
            new Metal("bronze", BRONZE, false),
            new Metal("arsenical_bronze", ARSENICAL_BRONZE, false),
            new Metal("bismuth_bronze", BISMUTH_BRONZE, false));

    // ---------------------------------------------------------------- image helpers

    static final int CLEAR = 0;

    static BufferedImage img() { return new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB); }

    static void px(BufferedImage im, int x, int y, int rgb) {
        if (x < 0 || y < 0 || x >= im.getWidth() || y >= im.getHeight()) return;
        im.setRGB(x, y, 0xFF000000 | rgb);
    }

    static void pxWrap(BufferedImage im, int x, int y, int rgb) {
        im.setRGB(Math.floorMod(x, 16), Math.floorMod(y, 16), 0xFF000000 | rgb);
    }

    static boolean opaque(BufferedImage im, int x, int y) {
        if (x < 0 || y < 0 || x >= im.getWidth() || y >= im.getHeight()) return false;
        return (im.getRGB(x, y) >>> 24) != 0;
    }

    static int rgb(BufferedImage im, int x, int y) { return im.getRGB(x, y) & 0xFFFFFF; }

    static int scale(int c, double f) {
        int r = (int) Math.min(255, ((c >> 16) & 255) * f);
        int g = (int) Math.min(255, ((c >> 8) & 255) * f);
        int b = (int) Math.min(255, (c & 255) * f);
        return (r << 16) | (g << 8) | b;
    }

    /** Darker and slightly cooler, for outlines. */
    static int outlineOf(int c, boolean lit) {
        double f = lit ? 0.62 : 0.45;
        int r = (int) (((c >> 16) & 255) * f);
        int g = (int) (((c >> 8) & 255) * f);
        int b = (int) Math.min(255, (c & 255) * f + 6);
        r = Math.max(r, 12);
        g = Math.max(g, 12);
        b = Math.max(b, 16);
        return (r << 16) | (g << 8) | b;
    }

    /** True while generating items that already follow style guide v2 (near-black outline). */
    static boolean itemsV2 = false;

    /** Adds the 1 px coloured outline around an item silhouette (style guide 5). */
    static BufferedImage outline(BufferedImage im) {
        if (itemsV2) return V2.outline(im);
        BufferedImage out = img();
        out.getGraphics().drawImage(im, 0, 0, null);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                if (opaque(im, x, y)) continue;
                // Prefer the neighbour to the right/below (outline on the lit side) or left/above.
                int[][] dirs = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
                for (int i = 0; i < 4; i++) {
                    int nx = x + dirs[i][0], ny = y + dirs[i][1];
                    if (opaque(im, nx, ny)) {
                        boolean lit = i < 2; // outline pixel sits top/left of the shape
                        px(out, x, y, outlineOf(rgb(im, nx, ny), lit));
                        break;
                    }
                }
            }
        }
        return out;
    }

    /** Tileable value noise in 0..1 with the given cell period (must divide 16). */
    static double[][] noise(long seed, int period) {
        Random r = new Random(seed);
        int n = 16 / period;
        double[][] grid = new double[n][n];
        for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) grid[i][j] = r.nextDouble();
        double[][] out = new double[16][16];
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                double fx = (double) x / period, fy = (double) y / period;
                int x0 = (int) fx, y0 = (int) fy;
                double tx = smooth(fx - x0), ty = smooth(fy - y0);
                double a = grid[y0 % n][x0 % n], b = grid[y0 % n][(x0 + 1) % n];
                double c = grid[(y0 + 1) % n][x0 % n], d = grid[(y0 + 1) % n][(x0 + 1) % n];
                out[y][x] = lerp(lerp(a, b, tx), lerp(c, d, tx), ty);
            }
        }
        return out;
    }

    static double smooth(double t) { return t * t * (3 - 2 * t); }

    static double lerp(double a, double b, double t) { return a + (b - a) * t; }

    static double[][] fractal(long seed) {
        double[][] a = noise(seed, 8), b = noise(seed + 1, 4), c = noise(seed + 2, 2);
        double[][] out = new double[16][16];
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) out[y][x] = a[y][x] * 0.5 + b[y][x] * 0.33 + c[y][x] * 0.17;
        return out;
    }

    /** Draws a 2-4 px cluster lit from the top-left. */
    static void speck(BufferedImage im, Random r, int x, int y, int lightRgb, int darkRgb, int size) {
        pxWrap(im, x, y, lightRgb);
        if (size >= 2) pxWrap(im, x + 1, y, r.nextBoolean() ? lightRgb : darkRgb);
        if (size >= 3) pxWrap(im, x, y + 1, darkRgb);
        if (size >= 4) pxWrap(im, x + 1, y + 1, darkRgb);
    }

    // ---------------------------------------------------------------- rocks

    /**
     * Terrain block in the v2 style: per-pixel grain smeared along the rock's bedding, quantised by share so
     * every rock has the density of vanilla stone (style guide 5).
     */
    static BufferedImage rock(Rock rock) {
        long seed = rock.name().hashCode() * 31L;
        Ramp p = rock.ramp();
        Random r = new Random(seed ^ 0x5eedL);
        // Intrusive rocks have almost no grain direction, so the period-4 noise would show as a grid: lean on per-pixel noise.
        double[][] f = V2.terrainField(seed, rock.category().equals("intrusive") ? 0.8 : 0.68);
        switch (rock.category()) {
            case "intrusive" -> f = V2.smear(f, 1, 0, 0.25);      // coarse crystals, little grain direction
            case "extrusive" -> f = V2.smear(f, 0, 1, 0.35);      // columnar: vertical grain
            case "sedimentary" -> {                              // horizontal bedding grain plus faint beds
                f = V2.smear(f, 1, 0, 0.5);
                for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) f[y][x] += (y / 5) % 2 == 1 ? 0.08 : 0;
            }
            case "metamorphic" -> f = V2.smear(V2.smear(f, 1, 1, 0.4), 1, 1, 0.3);  // diagonal foliation
            default -> throw new IllegalArgumentException(rock.category());
        }
        BufferedImage im = V2.paint(V2.quantize(f, V2.TERRAIN_SHARES), p);
        switch (rock.category()) {
            case "intrusive" -> {  // feldspar flecks and mafic specks, single pixels
                for (int i = 0; i < 5; i++) pxWrap(im, r.nextInt(16), r.nextInt(16), r.nextBoolean() ? p.get(5) : p.get(1));
            }
            case "metamorphic" -> {
                if (rock.name().equals("marble")) {  // two thin diagonal veins a step darker
                    for (int v = 0; v < 2; v++) {
                        int x = r.nextInt(16), y0 = r.nextInt(16);
                        for (int k = 0; k < 6; k++) pxWrap(im, x + k, y0 + k, p.get(k % 3 == 2 ? 3 : 2));
                    }
                } else {              // slate: a few dark cleavage dashes
                    for (int v = 0; v < 3; v++) {
                        int x = r.nextInt(16), y0 = r.nextInt(16);
                        for (int k = 0; k < 3; k++) pxWrap(im, x + k, y0 + k, p.get(1));
                    }
                }
            }
            default -> { }
        }
        return im;
    }

    /** Cobble: rounded stones with dark gaps, vanilla's layout, per-pixel grain on each stone (style guide 5). */
    static BufferedImage cobbled(Rock rock) {
        Ramp p = rock.ramp();
        BufferedImage im = img();
        int c3 = p.get(3);
        boolean dark = (0.299 * ((c3 >> 16) & 255) + 0.587 * ((c3 >> 8) & 255) + 0.114 * (c3 & 255)) / 255 < 0.3;
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) px(im, x, y, p.get(1));
        Random r = new Random(rock.name().hashCode() * 17L);
        int[][] centres = new int[9][2];
        for (int i = 0; i < 9; i++) {
            centres[i][0] = (i % 3) * 5 + 2 + r.nextInt(3);
            centres[i][1] = (i / 3) * 5 + 2 + r.nextInt(3);
        }
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int best = 0;
                double bd = 1e9, sd = 1e9;
                for (int i = 0; i < 9; i++) {
                    for (int ox = -16; ox <= 16; ox += 16)
                        for (int oy = -16; oy <= 16; oy += 16) {
                            double dx = x - centres[i][0] - ox, dy = y - centres[i][1] - oy;
                            double d = Math.sqrt(dx * dx + dy * dy);
                            if (d < bd) { sd = bd; best = i; bd = d; }
                            else if (d < sd) sd = d;
                        }
                }
                if (sd - bd < 1.0) continue; // gap
                // Lit from the top-left; every stone gets a speckle of its own tone.
                int cx = centres[best][0], cy = centres[best][1];
                double rel = ((x - cx + 24) % 16 - 8 + (y - cy + 24) % 16 - 8);
                int step = rel < -2 ? 4 : rel > 3 ? 2 : 3;
                double g = r.nextDouble();
                step += g < 0.18 ? 1 : g > 0.84 ? -1 : 0;
                if (dark) step++; // dark rocks sit in the lower part of vanilla's value band, not under it
                px(im, x, y, p.get(Math.max(2, Math.min(5, step))));
            }
        return im;
    }

    // ---------------------------------------------------------------- v2 helpers (style guide v2)

    /**
     * Ports of the helpers in {@code art/tools/gen_samples_v2.py}: grain fields, share quantising,
     * stamped ore clusters and the hard dark item outline. Later art chunks (items, machines) reuse them.
     */
    static final class V2 {
        /** Share of pixels per ramp step, dark to light: 4, 22, 44, 24, 6 percent. */
        static final int[] TERRAIN_SHARES = {4, 22, 44, 24, 6};

        // Ramps from the guide's section 4 that items and machines will need (steps 1-5, glint as spec).
        static final Ramp BRONZE_V2 = ramp(0xfff0b8, 0x3a2510, 0x6b4a1c, 0x9c7030, 0xc8983e, 0xe8c060);
        static final Ramp WOOD_V2 = ramp(0, 0x28190a, 0x493615, 0x684e1e, 0x896727, 0xa88238);
        static final Ramp FIRE_BRICK_V2 = ramp(0, 0x5e4a36, 0x7a6248, 0x927a58, 0xa68e68, 0xbaa27a);
        static final Ramp BRICK_V2 = ramp(0, 0x7a443a, 0x8c5042, 0x9c5c4c, 0xac6a58, 0xba7a66);
        static final Ramp MORTAR_V2 = ramp(0, 0x6a645a, 0x6a645a, 0xaaa498, 0xc0baae, 0xc0baae);
        static final Ramp STEEL_V2 = ramp(0xd4d8da, 0x3a3c42, 0x5a5d64, 0x7a7d84, 0x959aa0, 0xb4b8bc);
        // A2: tier 0-2 item ramps (6 steps with a glint, vanilla item value band).
        static final Ramp COPPER_V2 = ramp(0xffe2bc, 0x4a2214, 0x82401f, 0xb85f2f, 0xe08040, 0xf4a868);
        static final Ramp TIN_V2 = ramp(0xffffff, 0x383c48, 0x5e6474, 0x8890a0, 0xb4bcc8, 0xdce2ea);
        static final Ramp BISMUTH_V2 = ramp(0xfaf4fc, 0x403648, 0x665870, 0x8e7e98, 0xb4a4bc, 0xd4c8da);
        static final Ramp ARSENICAL_BRONZE_V2 = ramp(0xf6d2b4, 0x3e2216, 0x6a3e24, 0x93583a, 0xb87852, 0xd89c70);
        static final Ramp BISMUTH_BRONZE_V2 = ramp(0xf2dcc4, 0x3e2a1e, 0x694834, 0x91694c, 0xb48a68, 0xd2ac88);
        static final Ramp SLAG_V2 = ramp(0xc8bcb0, 0x38302c, 0x58504a, 0x7a6e66, 0x988c82, 0xb4a89c);
        static final Ramp FLINT_V2 = ramp(0xe4e8ee, 0x2c2e36, 0x484c58, 0x6a707e, 0x9096a4, 0xbcc2cc);
        static final Ramp CLAY_V2 = ramp(0, 0x5a5254, 0x7a7072, 0x9c9090, 0xbcb0ae, 0xd6cac6);
        static final Ramp CERAMIC_V2 = ramp(0xeab08a, 0x4e2a22, 0x7c4234, 0xa65c46, 0xc47c5c, 0xdc9e78);
        static final Ramp ASH_V2 = ramp(0, 0x5a5856, 0x787572, 0x98948f, 0xb8b4ae, 0xd4d0ca);
        static final Ramp FIBRE_V2 = ramp(0, 0x38441c, 0x4e6028, 0x6a7e34, 0x8aa044, 0xaec25e);
        static final Ramp STRAW_V2 = ramp(0, 0x6a5624, 0x8a7430, 0xb09a42, 0xd0b858, 0xe8d476);

        /** 50% per-pixel noise + 30% tileable noise at period 4 + 20% at period 2. */
        static double[][] terrainField(long seed) { return terrainField(seed, 0.5); }

        /** Same field with a chosen share of per-pixel noise; the rest is split 3:2 between period 4 and 2. */
        static double[][] terrainField(long seed, double white) {
            double lo = (1 - white) * 0.6, mid = (1 - white) * 0.4;
            Random r = new Random(seed);
            double[][] a = noise(seed + 11, 4), b = noise(seed + 12, 2);
            double[][] out = new double[16][16];
            for (int y = 0; y < 16; y++)
                for (int x = 0; x < 16; x++) out[y][x] = white * r.nextDouble() + lo * a[y][x] + mid * b[y][x];
            return out;
        }

        /** Per-pixel grain field rank-normalised to 0-1, a drop-in for the old blob noise where thresholds are shares. */
        static double[][] grain(long seed) {
            double[][] f = terrainField(seed, 0.6);
            Integer[] order = new Integer[256];
            for (int i = 0; i < 256; i++) order[i] = i;
            java.util.Arrays.sort(order, java.util.Comparator.comparingDouble(i -> f[i / 16][i % 16]));
            double[][] out = new double[16][16];
            for (int k = 0; k < 256; k++) out[order[k] / 16][order[k] % 16] = (k + 0.5) / 256;
            return out;
        }

        /** Directional wrapped smear: gives stone its streaky grain. */
        static double[][] smear(double[][] f, int dx, int dy, double w) {
            double[][] out = new double[16][16];
            for (int y = 0; y < 16; y++)
                for (int x = 0; x < 16; x++) {
                    double a = f[Math.floorMod(y - dy, 16)][Math.floorMod(x - dx, 16)];
                    double b = f[Math.floorMod(y + dy, 16)][Math.floorMod(x + dx, 16)];
                    out[y][x] = f[y][x] * (1 - w) + (a + b) * w / 2;
                }
            return out;
        }

        /** Ramp step index (0-based) per pixel by share of pixels, dark to light; exact and deterministic. */
        static int[][] quantize(double[][] f, int[] shares) {
            Integer[] order = new Integer[256];
            for (int i = 0; i < 256; i++) order[i] = i;
            java.util.Arrays.sort(order, java.util.Comparator.comparingDouble(i -> f[i / 16][i % 16]));
            int total = 0;
            for (int s : shares) total += s;
            int[][] idx = new int[16][16];
            int pos = 0;
            double acc = 0;
            for (int step = 0; step < shares.length; step++) {
                acc += 256.0 * shares[step] / total;
                int end = step == shares.length - 1 ? 256 : (int) Math.round(acc);
                for (; pos < end; pos++) idx[order[pos] / 16][order[pos] % 16] = step;
            }
            return idx;
        }

        static BufferedImage paint(int[][] idx, Ramp p) {
            BufferedImage im = img();
            for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) px(im, x, y, p.get(idx[y][x] + 1));
            return im;
        }

        /** Vanilla brick layout: 4 courses of 7 px bricks in half bond, light mortar, 2-3 tones per brick. */
        static BufferedImage bricks(Ramp brick, long seed) {
            Random r = new Random(seed);
            Ramp m = MORTAR_V2;
            BufferedImage im = img();
            for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) px(im, x, y, m.get(3));
            for (int course = 0; course < 4; course++) {
                int y0 = course * 4, off = course % 2 == 0 ? 0 : 4;
                for (int b = 0; b < 2; b++) {
                    int x0 = (off + b * 8) % 16, tone = 2 + r.nextInt(3);
                    for (int y = y0; y < y0 + 3; y++)
                        for (int x = x0; x < x0 + 7; x++) {
                            int s = tone + (y == y0 && r.nextDouble() < 0.6 ? 1 : 0) - (y == y0 + 2 && r.nextDouble() < 0.4 ? 1 : 0);
                            s += (r.nextDouble() < 0.15 ? 1 : 0) - (r.nextDouble() < 0.12 ? 1 : 0);
                            px(im, x % 16, y, brick.get(Math.max(1, Math.min(5, s))));
                        }
                }
                for (int x = 0; x < 16; x++) px(im, x, y0 + 3, m.get(1));
                for (int x = off - 1; x < off + 16; x += 8)
                    for (int y = y0; y < y0 + 3; y++) px(im, Math.floorMod(x, 16), y, m.get(y == y0 + 2 ? 1 : 3));
            }
            return im;
        }

        /** Item outline colour: the neighbour darkened to 32%, hue kept. */
        static int outlineCol(int c) {
            int d = scale(c, 0.32);
            // Snap to a coarse grid so neighbouring steps share one outline colour, like vanilla's near-black rims.
            int r = ((d >> 16) & 255) / 12 * 12, g = ((d >> 8) & 255) / 12 * 12, b = (d & 255) / 12 * 12;
            return (r << 16) | (g << 8) | b;
        }

        /** 1 px outline round the silhouette on all four sides, near black, colour from the bordering pixel. */
        static BufferedImage outline(BufferedImage in) {
            BufferedImage out = img();
            out.getGraphics().drawImage(in, 0, 0, null);
            int[][] dirs = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};
            for (int y = 0; y < 16; y++)
                for (int x = 0; x < 16; x++) {
                    if (opaque(in, x, y)) continue;
                    for (int[] d : dirs)
                        if (opaque(in, x + d[0], y + d[1])) { px(out, x, y, outlineCol(rgb(in, x + d[0], y + d[1]))); break; }
                }
            return out;
        }

        /** Merges the closest, least used colours until at most {@code max} remain (vanilla items stay under 12). */
        static BufferedImage limitColours(BufferedImage in, int max) {
            java.util.Map<Integer, Integer> count = new java.util.LinkedHashMap<>();
            for (int y = 0; y < 16; y++)
                for (int x = 0; x < 16; x++) if (opaque(in, x, y)) count.merge(rgb(in, x, y), 1, Integer::sum);
            java.util.Map<Integer, Integer> remap = new java.util.HashMap<>();
            for (int c : count.keySet()) remap.put(c, c);
            while (count.size() > max) {
                long best = Long.MAX_VALUE;
                int from = 0, to = 0;
                for (int a : count.keySet())
                    for (int b : count.keySet()) {
                        if (a == b || count.get(a) > count.get(b)) continue;
                        int dr = ((a >> 16) & 255) - ((b >> 16) & 255), dg = ((a >> 8) & 255) - ((b >> 8) & 255), db = (a & 255) - (b & 255);
                        long d = (long) (dr * dr + dg * dg + db * db) * count.get(a);
                        if (d < best) { best = d; from = a; to = b; }
                    }
                count.merge(to, count.remove(from), Integer::sum);
                for (var e : remap.entrySet()) if (e.getValue() == from) e.setValue(to);
            }
            BufferedImage out = img();
            for (int y = 0; y < 16; y++)
                for (int x = 0; x < 16; x++) if (opaque(in, x, y)) px(out, x, y, remap.get(rgb(in, x, y)));
            return out;
        }

        // Hand-shaped mineral clusters (ramp steps, '.' = host rock): highlight top-left, shadow and a dark
        // rim bottom-right, like vanilla ore specks.
        static final String[][] SMALL = {{"45", "32"}, {"54.", "431"}, {".5", "43", "21"}, {"45.", ".321"}};
        static final String[][] MEDIUM = {{".45.", "4432", ".21."}, {"45.", "4432", ".321"}, {".54", "4431", "321."}, {"54..", "4432", ".321"}};
        static final String[][] LARGE = {{".455.", "44432", "43321", ".21.."}, {"..45.", ".4443", "44332", ".321."}, {".54..", "44543", "33321", "..21."}};

        /** Mineral overlay: poor = 3 small, normal = 3 medium + 2 small, rich = 2 large + 3 medium + 2 small. */
        static BufferedImage ore(Mineral m, String grade) {
            Random r = new Random((m.name() + "/" + grade).hashCode() * 131L);
            String[][][] plan = switch (grade) {
                case "poor" -> new String[][][] {SMALL, SMALL, SMALL};
                case "normal" -> new String[][][] {MEDIUM, MEDIUM, MEDIUM, SMALL, SMALL};
                default -> new String[][][] {LARGE, LARGE, MEDIUM, MEDIUM, MEDIUM, SMALL, SMALL};
            };
            Ramp p = m.ramp();
            BufferedImage im = img();
            boolean[][] taken = new boolean[16][16];
            for (String[][] pool : plan) {
                String[] shape = pool[r.nextInt(pool.length)];
                int h = shape.length, w = 0;
                for (String row : shape) w = Math.max(w, row.length());
                int ox = 0, oy = 0;
                boolean fits = false;
                for (int t = 0; t < 300 && !fits; t++) {
                    ox = 1 + r.nextInt(15 - w);
                    oy = 1 + r.nextInt(15 - h);
                    fits = true;
                    for (int y = Math.max(0, oy - 1); y < Math.min(16, oy + h + 1); y++)
                        for (int x = Math.max(0, ox - 1); x < Math.min(16, ox + w + 1); x++) if (taken[y][x]) fits = false;
                }
                if (!fits) continue;
                boolean glint = pool == LARGE && m.metallic() && p.spec() != 0;
                for (int y = 0; y < h; y++)
                    for (int x = 0; x < shape[y].length(); x++) {
                        char ch = shape[y].charAt(x);
                        if (ch == '.') continue;
                        int c = p.get(ch - '0');
                        if (glint && ch == '5') { c = p.spec(); glint = false; }
                        px(im, ox + x, oy + y, c);
                    }
                for (int y = oy; y < oy + h; y++) for (int x = ox; x < ox + w; x++) taken[y][x] = true;
            }
            return im;
        }
    }

    /** Mineral overlay per grade: stamped clusters, grade shown by cluster count and size (style guide 6). */
    static BufferedImage oreOverlay(Mineral m, String grade) { return V2.ore(m, grade); }

    static BufferedImage composite(BufferedImage base, BufferedImage overlay) {
        BufferedImage out = img();
        out.getGraphics().drawImage(base, 0, 0, null);
        out.getGraphics().drawImage(overlay, 0, 0, null);
        return out;
    }

    // ---------------------------------------------------------------- items from pixel maps

    /**
     * Draws a pixel map: digits 1-5 are ramp steps of {@code a}, letters a-e are steps 1-5 of
     * {@code b}, 's' is a's specular colour, '.' is transparent. The outline is added afterwards.
     */
    static BufferedImage map(Ramp a, Ramp b, String... rows) {
        BufferedImage im = img();
        int top = (16 - rows.length) / 2;
        for (int y = 0; y < rows.length; y++) {
            String row = rows[y];
            int left = (16 - row.length()) / 2;
            for (int x = 0; x < row.length(); x++) {
                char ch = row.charAt(x);
                int c;
                if (ch >= '1' && ch <= '5') c = a.get(ch - '0');
                else if (ch >= 'a' && ch <= 'e' && b != null) c = b.get(ch - 'a' + 1);
                else if (ch == 's') c = a.spec() != 0 ? a.spec() : a.get(5);
                else continue;
                px(im, left + x, top + y, c);
            }
        }
        return outline(im);
    }

    static BufferedImage map(Ramp a, String... rows) { return map(a, null, rows); }

    static final String[] INGOT = {
            "..........",
            "...45555s.",
            "..4444445.",
            ".344444443",
            "3333333332",
            "2333333321",
            ".2222222..",
    };
    static final String[] INGOT_V2 = {
            "......5555s52...",
            ".....444444421..",
            "....4444444421..",
            "...333333333321.",
            "...22222222221..",
    };

    /** Tier 0-2 metal ramps in the v2 value band. */
    static Ramp v2Metal(String name) {
        return switch (name) {
            case "copper" -> V2.COPPER_V2;
            case "tin" -> V2.TIN_V2;
            case "bismuth" -> V2.BISMUTH_V2;
            case "bronze" -> V2.BRONZE_V2;
            case "arsenical_bronze" -> V2.ARSENICAL_BRONZE_V2;
            case "bismuth_bronze" -> V2.BISMUTH_BRONZE_V2;
            default -> throw new IllegalArgumentException(name);
        };
    }

    static final String[] NUGGET = {
            "..45.",
            ".4543",
            "44332",
            ".322.",
    };
    static final String[] PLATE = {
            "...55555s...",
            "..4444444445",
            ".44444444443",
            ".33333333332",
            "332222222221",
            ".1111111111.",
    };
    static final String[] ORE_SMALL = {
            "..45..",
            ".4433.",
            "443322",
            ".3221.",
    };
    static final String[] ORE_NORMAL = {
            "...45...",
            "..44543.",
            ".4443332",
            "44433322",
            ".3332221",
            "..2211..",
    };
    static final String[] ORE_RICH = {
            "....45....",
            "..445s43..",
            ".44454333.",
            "4444433322",
            "4443333221",
            ".33332221.",
            "..222211..",
    };
    static final String[] CRUSHED_SMALL = {
            "..4.......",
            ".43..45...",
            "....433.4.",
            ".45..2..32",
            "4332......",
    };
    static final String[] CRUSHED_NORMAL = {
            "....45....",
            "..4.433.4.",
            ".453.2..32",
            "443324532.",
            ".43322332.",
            "4433222221",
    };
    static final String[] CRUSHED_RICH = {
            "...45.45...",
            ".4.4334s3..",
            "45324532.4.",
            "433244332.3",
            "44333332221",
            "43332222221",
            ".222211111.",
    };
    static final String[] LOOSE_ROCK = {
            "....4455..",
            "..4444443.",
            ".444433332",
            "4443333322",
            "3333332221",
            ".22222211.",
    };
    static final String[] FLINT_SHARD = {
            ".......45",
            "......453",
            ".....4432",
            "....4432.",
            "...4432..",
            "..4332...",
            ".3322....",
            "322......",
            "21.......",
    };

    // Tool heads and blades (drawn pointing up-right like the finished tools).
    static final String[] PICKAXE_HEAD = {
            "...44455s..",
            ".4433333345",
            "43.......43",
            "3.........3",
    };
    static final String[] AXE_HEAD = {
            "..455.",
            ".44435",
            "443334",
            "433333",
            ".33322",
            "..222.",
    };
    static final String[] SHOVEL_HEAD = {
            ".455.",
            "44445",
            "43334",
            "43333",
            "33332",
            ".322.",
            "..2..",
    };
    static final String[] HOE_HEAD = {
            "4444455s",
            "4333....",
            "32......",
    };
    static final String[] KNIFE_BLADE = {
            ".........5",
            "........54",
            ".......543",
            "......443.",
            ".....443..",
            "....443...",
            "...332....",
            "..32......",
    };
    static final String[] HAMMER_HEAD = {
            "4445555s",
            "43333334",
            "33333332",
            "...22...",
    };
    static final String[] SAW_BLADE = {
            "445555555s",
            "4333333334",
            "3232323232",
    };
    static final String[] SWORD_BLADE = {
            "..........45",
            ".........453",
            "........453.",
            ".......443..",
            "......443...",
            ".....443....",
            "....443.....",
            "...332......",
            "..332.......",
            ".32.........",
    };
    static final String[] SPEAR_HEAD = {
            "..45..",
            ".4453.",
            ".4433.",
            "44332.",
            ".332..",
            "..2...",
    };
    static final String[] PROSPECTOR_HEAD = {
            "....4455s",
            "44333334.",
            "3......3.",
    };
    static final String[] TONGS_JAW = {
            "45....45",
            "43....43",
            ".43..43.",
            "..4334..",
            "..3..3..",
            "..3..3..",
    };

    // Knapped stone heads: faceted bifaces with flake scars (alternating steps) and chipped edges.
    static final String[] KNAPPED_AXE_HEAD = {
            "......455...",
            "....44544...",
            "...4543443..",
            "..44434343..",
            ".4434443432.",
            ".4343434332.",
            "44434343432.",
            "4343434332..",
            ".33434332...",
            "..333322....",
            "...2222.....",
    };
    static final String[] KNAPPED_KNIFE_BLADE = {
            "..........45",
            ".........454",
            "........4543",
            ".......4443.",
            "......4543..",
            ".....4443...",
            "....4543....",
            "...4433.....",
            "..4332......",
            ".3322.......",
            ".22.........",
    };
    static final String[] KNAPPED_SHOVEL_HEAD = {
            "..44554..",
            ".4545444.",
            "444434343",
            "434343433",
            "443434332",
            "343433332",
            ".3333322.",
            "..33222..",
            "...222...",
    };
    static final String[] KNAPPED_HOE_HEAD = {
            "...4455554..",
            ".4445454443.",
            "444343434332",
            "433333333322",
            ".2222222221.",
    };
    static final String[] KNAPPED_HAMMER_HEAD = {
            "..445554..",
            ".44545444.",
            "4445444343",
            "4343434333",
            "3434343332",
            ".33333322.",
            "..22222...",
    };
    static final String[] KNAPPED_SPEAR_HEAD = {
            "...45...",
            "..4554..",
            "..4543..",
            ".445443.",
            ".454343.",
            "44434332",
            ".434332.",
            ".43332..",
            "..3322..",
            "...22...",
    };
    static final String[] KNAPPED_PICKAXE_HEAD = {
            "...445555443..",
            ".44543434344..",
            "443.......344.",
            "43.........43.",
            "3...........3.",
    };

    /** Diagonal tool: handle bottom-left to head top-right. */
    static BufferedImage tool(Ramp head, Ramp handle, Ramp lashing, String kind) {
        BufferedImage im = img();
        // Handle: a diagonal stick from (2,13) towards (10,5).
        int end = switch (kind) {
            case "knife", "sword" -> 6;
            case "shovel", "spear" -> 9;
            default -> 10;
        };
        if (itemsV2) {
            // Vanilla pose: 1 px handle from the bottom-left corner, two tones, a lighter pixel near the grip.
            for (int i = 0; i <= end; i++) {
                px(im, 1 + i, 14 - i, handle.get(i % 3 == 0 ? 2 : 3));
            }
        } else {
            for (int i = 0; i < end; i++) {
                int x = 2 + i, y = 13 - i;
                px(im, x, y, handle.get(3));
                px(im, x + 1, y, handle.get(2));
                if (i % 3 == 0) px(im, x, y, handle.get(4));
            }
        }
        if (lashing != null) {
            int x = 2 + end - 2, y = 13 - (end - 2);
            px(im, x, y, lashing.get(4));
            px(im, x + 1, y, lashing.get(3));
            px(im, x + 1, y - 1, lashing.get(3));
        }
        Ramp h = head;
        switch (kind) {
            case "pickaxe" -> {
                if (itemsV2) { crescent(im, h, true); break; }
                String[] rows = {
                        "....44455..",
                        "..443333345",
                        ".43......44",
                        "43........4",
                        "3.........3",
                        "3..........",
                };
                drawAt(im, h, rows, 4, 1);
            }
            case "axe" -> {
                if (itemsV2) drawAt(im, h, new String[] {"..5", "345", "2345", "..345", "..234", "...23"}, 10, 1);
                else drawAt(im, h, new String[] {"..455", ".4443", "44333", "43332", ".332.", "..2.."}, 9, 1);
            }
            case "shovel" -> drawAt(im, h, new String[] {"..45.", ".4445", "44433", "43332", "4332.", ".2..."}, 9, 1);
            case "hoe" -> drawAt(im, h, new String[] {"4444455", "43333..", "32....."}, 7, 2);
            case "knife" -> drawAt(im, h, new String[] {"......45", ".....453", "....443.", "...443..", "..443...", ".332....", ".2......"}, 6, 1);
            case "hammer" -> drawAt(im, h, new String[] {"..4455.", ".44433s", "4433332", ".33322.", "..32..."}, 8, 1);
            // A wide blade with teeth along its lower edge.
            case "saw" -> drawAt(im, h, new String[] {"........455", ".......44432", "......4443.", ".....44432.", "....4443...", "...44432...", "..332......", ".2........."}, 5, 0);
            case "sword" -> {
                drawAt(im, h, SWORD_BLADE, 5, 0);
                if (itemsV2) { // crossguard across the handle
                    px(im, 3, 8, h.get(3)); px(im, 4, 9, h.get(4)); px(im, 6, 11, h.get(3)); px(im, 7, 12, h.get(2));
                }
            }
            case "spear" -> drawAt(im, h, new String[] {"..455", ".4453", "44332", "4332.", ".2..."}, 10, 1);
            case "prospectors_pick" -> drawAt(im, h, new String[] {"...4455", ".443334", "43....3", "3......"}, 6, 2);
            default -> throw new IllegalArgumentException(kind);
        }
        return outline(im);
    }

    /** Vanilla-style pickaxe head: a crescent arc across the top-right, tapered at both tips, glint on the outer curve. */
    static void crescent(BufferedImage im, Ramp c, boolean withTip) {
        double cx = 1.5, cy = 14.5;
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double dx = x + 0.5 - cx, dy = cy - (y + 0.5);
                double r = Math.hypot(dx, dy), ang = Math.toDegrees(Math.atan2(dy, dx));
                double taper = (ang < 26 || ang > 64) ? 0.9 : 0;
                if (r >= 11.6 + taper && r <= 13.9 - taper * 0.3 && ang >= 14 && ang <= 76) {
                    boolean outer = r > 12.9;
                    int step = outer ? 4 : 2;
                    if (!outer && ang > 40 && ang < 50) step = 3;
                    px(im, x, y, c.get(step));
                }
            }
        px(im, 10, 3, c.spec() != 0 ? c.spec() : c.get(5));
    }

    /** The crescent head on its own, centred. */
    static BufferedImage pickaxeHeadV2(Ramp c) {
        BufferedImage im = img();
        crescent(im, c, false);
        int x0 = 16, x1 = -1, y0 = 16, y1 = -1;
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) if (opaque(im, x, y)) { x0 = Math.min(x0, x); x1 = Math.max(x1, x); y0 = Math.min(y0, y); y1 = Math.max(y1, y); }
        BufferedImage out = img();
        int ox = (16 - (x1 - x0 + 1)) / 2 - x0, oy = (16 - (y1 - y0 + 1)) / 2 - y0;
        for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) if (opaque(im, x, y)) px(out, x + ox, y + oy, rgb(im, x, y));
        return outline(out);
    }

    static void drawAt(BufferedImage im, Ramp a, String[] rows, int left, int top) {
        for (int y = 0; y < rows.length; y++)
            for (int x = 0; x < rows[y].length(); x++) {
                char ch = rows[y].charAt(x);
                if (ch >= '1' && ch <= '5') px(im, left + x, top + y, a.get(ch - '0'));
                else if (ch == 's') px(im, left + x, top + y, a.spec() != 0 ? a.spec() : a.get(5));
            }
    }

    // ---------------------------------------------------------------- ground cover

    /** Two crossed sticks seen from above, for the flat loose stick model. */
    static final String[] LOOSE_STICKS = {
            "................",
            "..45............",
            "..443...........",
            "...343......5...",
            "....342....443..",
            ".....342..443...",
            "......3424432...",
            ".......34432....",
            ".......4432.....",
            "......443232....",
            ".....4432.322...",
            "....4432...322..",
            "...4432.....32..",
            "...332.......2..",
            "....2...........",
            "................",
    };

    /** A few knapped flint flakes on the ground. */
    static final String[] LOOSE_FLINTS = {
            "................",
            "................",
            "................",
            "........5.......",
            ".......453......",
            "......4442......",
            ".....44432......",
            "....44332...4...",
            "....3322...453..",
            ".....2....44432.",
            "..........33322.",
            "...45.......2...",
            "..4443..........",
            ".44332..........",
            "..222...........",
            "................",
    };

    /** A small book: woven straw covers, a twine-stitched spine and a twine tie, page edges at the side. */
    static final String[] FIELD_JOURNAL = {
            "................",
            "..2355555554....",
            "..2353443344r...",
            "..2354433443s...",
            "..dc54334433r...",
            "..2353344334s...",
            "..2353443344r...",
            "..dcccccccccd...",
            "..23443344cdec..",
            "..2343344334sb..",
            "..dc33443344r...",
            "..2324422442s...",
            "..2344224422r...",
            "..dc22222222q...",
            "....qrrrrrrrq...",
            "................",
    };

    /** Like {@link #art}, but digits draw from the first ramp, a-e from the second and p-t from the third. */
    static BufferedImage art(String[] rows, Ramp digits, Ramp letters, Ramp paper) {
        BufferedImage im = img();
        for (int y = 0; y < rows.length; y++)
            for (int x = 0; x < rows[y].length(); x++) {
                char ch = rows[y].charAt(x);
                if (ch >= '1' && ch <= '5') px(im, x, y, digits.get(ch - '0'));
                else if (ch >= 'a' && ch <= 'e') px(im, x, y, letters.get(ch - 'a' + 1));
                else if (ch >= 'p' && ch <= 't') px(im, x, y, paper.get(ch - 'p' + 1));
            }
        return outline(im);
    }

    /** Full 16x16 art without centring, then outlined. */
    static BufferedImage art(Ramp a, String[] rows) {
        BufferedImage im = img();
        for (int y = 0; y < rows.length; y++)
            for (int x = 0; x < rows[y].length(); x++) {
                char ch = rows[y].charAt(x);
                if (ch >= '1' && ch <= '5') px(im, x, y, a.get(ch - '0'));
            }
        return outline(im);
    }

    /** Solid pebble surface for small ore indicators: mottled mineral with lit nubs and glints. */
    static BufferedImage pebbles(Mineral m) {
        Ramp a = m.ramp();
        long seed = m.name().hashCode() * 31L + 7;
        double[][] n = fractal(seed);
        BufferedImage im = img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int i = 2 + (int) Math.round(n[y][x] * 2.4);
                px(im, x, y, a.get(Math.max(1, Math.min(4, i))));
            }
        Random r = new Random(seed);
        for (int k = 0; k < 7; k++) speck(im, r, r.nextInt(16), r.nextInt(16), a.get(5), a.get(2), 2 + r.nextInt(2));
        if (m.metallic() && a.spec() != 0)
            for (int k = 0; k < 3; k++) pxWrap(im, r.nextInt(16), r.nextInt(16), a.spec());
        return im;
    }

    // ---------------------------------------------------------------- stone age

    /** A sheaf of straw, tied with a band of darker stalks across the middle. */
    static final String[] STRAW_SHEAF = {
            "................",
            "..........5.....",
            "......5..45.5...",
            ".....45.454.4...",
            "......4.4434....",
            ".......44343....",
            "........4433....",
            ".......4433.....",
            "......2121......",
            ".....2121.......",
            "....3443........",
            "...443.43.......",
            "..4.3..3.3......",
            ".43.3...3.......",
            "..3.............",
            "................",
    };

    /** A small hank of twisted twine: two loops and a loose end. */
    static final String[] TWINE_HANK = {
            "................",
            "................",
            ".....455543.....",
            "...4432..2344...",
            "..443......343..",
            "..43..4554..43..",
            ".443.432234.432.",
            ".43..43..34..32.",
            ".43..432234..32.",
            ".343..4332..332.",
            "..343......332..",
            "...3432..23322..",
            ".....433322.....",
            "..........32....",
            "...........32...",
            "................",
    };

    /** A loose bundle of long grass fibres laid on the diagonal, tied once at the middle, ends splayed. */
    static BufferedImage plantFibre() {
        Ramp a = V2.FIBRE_V2;
        BufferedImage im = img();
        // Two strands side by side, a lit one and a shaded one, bowed slightly in the middle.
        for (int i = 0; i < 13; i++) {
            int bow = i >= 5 && i <= 8 ? 1 : 0;
            px(im, 1 + i + bow, 14 - i, a.get(i % 5 == 4 ? 5 : 4));
            px(im, 2 + i + bow, 14 - i, a.get(i % 4 == 1 ? 2 : 3));
        }
        // The tie, across both strands.
        px(im, 6, 8, a.get(1));
        px(im, 7, 8, a.get(1));
        px(im, 7, 7, a.get(2));
        px(im, 8, 7, a.get(1));
        // Frayed ends: single strands splayed away from the bundle.
        px(im, 0, 14, a.get(3));
        px(im, 1, 15, a.get(2));
        px(im, 15, 1, a.get(4));
        px(im, 14, 0, a.get(5));
        px(im, 15, 3, a.get(3));
        return outline(im);
    }

    /** A square of woven fibre cloth with a frayed lower edge. */
    static BufferedImage fibreCloth() {
        BufferedImage im = img();
        for (int y = 3; y < 13; y++)
            for (int x = 2; x < 14; x++) {
                boolean warp = ((x + y) & 1) == 0;
                int step = warp ? 4 : 3;
                if (y == 3) step++;
                if (x == 13 || y == 12) step--;
                if (((x * 7 + y * 3) % 11) == 0) step--;
                px(im, x, y, FIBRE.get(step));
            }
        // Fraying threads.
        px(im, 3, 13, FIBRE.get(2));
        px(im, 6, 13, FIBRE.get(3));
        px(im, 10, 13, FIBRE.get(2));
        px(im, 12, 13, FIBRE.get(3));
        return outline(im);
    }

    /** Knapping grid fill for flint: dark glassy surface with conchoidal ripples. */
    static BufferedImage flintSurface() {
        BufferedImage im = img();
        double[][] n = fractal(97);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double ripple = Math.sin(Math.hypot(x - 4, y - 5) * 1.3) * 0.5 + 0.5;
                int i = 1 + (int) Math.round(n[y][x] * 1.6 + ripple * 1.4);
                px(im, x, y, FLINT.get(Math.max(1, Math.min(4, i))));
            }
        Random r = new Random(11);
        for (int k = 0; k < 3; k++) px(im, r.nextInt(16), r.nextInt(16), FLINT.get(5));
        return im;
    }

    // ---------------------------------------------------------------- fire

    static final Ramp FIELD_STONE = ramp(0, 0x34363a, 0x4a4c50, 0x63656a, 0x7d7f83, 0x98999c);
    /** Embers: charcoal shot through with the heat-glow colours (style guide 6), only for the lit bed. */
    static final int[] EMBER_GLOW = {0x6e1e14, 0xa0281a, 0xd23a1e, 0xf07a22};

    /** Rounded fieldstones with dark gaps, for the ring around the fire pit. */
    static BufferedImage fieldStones() {
        double[][] n = fractal(4242);
        BufferedImage im = img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int i = 2 + (int) Math.round(n[y][x] * 2.2);
                px(im, x, y, FIELD_STONE.get(Math.max(1, Math.min(5, i))));
            }
        Random r = new Random(4242);
        for (int k = 0; k < 6; k++) speck(im, r, r.nextInt(16), r.nextInt(16), FIELD_STONE.get(5), FIELD_STONE.get(1), 2 + r.nextInt(2));
        return im;
    }

    /** Stick bark: lengthwise grain with a few knots. */
    static BufferedImage stickWood() {
        BufferedImage im = img();
        Random r = new Random(77);
        for (int y = 0; y < 16; y++) {
            int base = (y % 4 == 0) ? 2 : (y % 4 == 3 ? 4 : 3);
            for (int x = 0; x < 16; x++) {
                int step = base + (r.nextInt(7) == 0 ? (r.nextBoolean() ? 1 : -1) : 0);
                px(im, x, y, WOOD.get(Math.max(1, Math.min(5, step))));
            }
        }
        for (int k = 0; k < 3; k++) {
            int x = r.nextInt(14), y = r.nextInt(14);
            px(im, x, y, WOOD.get(1));
            px(im, x + 1, y, WOOD.get(2));
        }
        return im;
    }

    /** Cold ash bed with a few charcoal lumps. */
    static BufferedImage ashBed() {
        double[][] n = fractal(313);
        BufferedImage im = img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int i = 2 + (int) Math.round(n[y][x] * 2.0);
                px(im, x, y, ASH.get(Math.max(1, Math.min(4, i))));
            }
        Random r = new Random(313);
        for (int k = 0; k < 7; k++) speck(im, r, r.nextInt(16), r.nextInt(16), CHARCOAL.get(4), CHARCOAL.get(1), 2);
        return im;
    }

    /** The lit bed: charcoal with glowing cracks, brighter towards the middle. */
    static BufferedImage emberBed() {
        return emberBed(919);
    }

    static BufferedImage emberBed(long seed) {
        return emberBed(seed, 0.0);
    }

    /** {@code glow} raises the whole bed towards the glowing steps, for a hearth under a bellows draught. */
    static BufferedImage emberBed(long seed, double glow) {
        double[][] n = fractal(seed);
        BufferedImage im = img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double centre = 1.0 - Math.hypot(x - 7.5, y - 7.5) / 10.6;
                double v = n[y][x] * 0.6 + centre * 0.6 + glow;
                int c;
                if (v > 0.98) c = EMBER_GLOW[3];
                else if (v > 0.88) c = EMBER_GLOW[2];
                else if (v > 0.78) c = EMBER_GLOW[1];
                else if (v > 0.70) c = EMBER_GLOW[0];
                else c = CHARCOAL.get(1 + (int) Math.round(v * 4));
                px(im, x, y, c);
            }
        return im;
    }

    /** A hand drill: spindle stood on a hearth board, a twine wrap, a charred notch. */
    static final String[] FIRESTARTER = {
            "................",
            "................",
            ".......45.......",
            ".......43.......",
            ".......43.......",
            "......ab3a......",
            "......bad3......",
            ".......ab.......",
            ".......43.......",
            ".......43.......",
            ".......32.......",
            "..444553x5554...",
            "..33333223333...",
            "..22222222222...",
            "................",
            "................",
    };

    static BufferedImage firestarter() {
        String[] rows = FIRESTARTER.clone();
        BufferedImage im = img();
        for (int y = 0; y < rows.length; y++)
            for (int x = 0; x < rows[y].length(); x++) {
                char ch = rows[y].charAt(x);
                if (ch >= '1' && ch <= '5') px(im, x, y, WOOD.get(ch - '0'));
                else if (ch >= 'a' && ch <= 'e') px(im, x, y, STRAW.get(ch - 'a' + 2));
                else if (ch == 'x') px(im, x, y, CHARCOAL.get(1));
            }
        return outline(im);
    }

    // ---------------------------------------------------------------- clay and ceramics (spec 4)
    // Unfired and fired pieces share each silhouette and swap only the ramp (style guide 5).

    /** Style guide sample: a tapered crucible with a lit rim. */
    static final String[] CRUCIBLE_ITEM = {
            "................",
            "................",
            "................",
            "...1222222221...",
            "...4555555553...",
            "....34444432....",
            "....44333322....",
            "....44333322....",
            "....43333221....",
            "....43332221....",
            ".....332221.....",
            ".....332211.....",
            "......2211......",
            "................",
            "................",
            "................",
    };

    /** Style guide sample: an ingot tray with the cavity in shadow. */
    static final String[] INGOT_MOLD_ITEM = {
            "................",
            "................",
            "................",
            "................",
            "................",
            "....44444444....",
            "...4555555553...",
            "..455111111432..",
            "..4511222212432.",
            "..441111111232..",
            "..3333333333322.",
            "..22222222222...",
            "................",
            "................",
            "................",
            "................",
    };

    /** A small round-bellied pot with a short neck. */
    static final String[] SMALL_VESSEL_ITEM = {
            "................",
            "................",
            "................",
            "......4553......",
            "......1221......",
            ".......43.......",
            ".....444332.....",
            "....45444332....",
            "...4544443332...",
            "...4444433322...",
            "...4444333222...",
            "...4433332221...",
            "....33332221....",
            ".....322221.....",
            "................",
            "................",
    };

    /** A tall storage jar: wide shoulders, a cord band, a narrow mouth. */
    static final String[] LARGE_VESSEL_ITEM = {
            "................",
            ".....4555553....",
            ".....1222221....",
            "......44332.....",
            "....444443332...",
            "...45444443332..",
            "...32323232322..",
            "...44444433322..",
            "...44444333222..",
            "...44443333222..",
            "...44433332221..",
            "....433332221...",
            "....333322221...",
            ".....3322211....",
            "......22111.....",
            "................",
    };

    /** A rounded brick, lit from the top-left. */
    static final String[] BRICK_ITEM = {
            "................",
            "................",
            "................",
            "................",
            "................",
            "....455555553...",
            "...45555555543..",
            "..4444444444332.",
            "..4444444443322.",
            "..4433333333221.",
            "..3333333332211.",
            "...222222222111.",
            "................",
            "................",
            "................",
            "................",
    };

    /** Each tool mold's cavity, 8x6, carved into the slab: the shape of the head it casts. */
    static final java.util.Map<String, String[]> MOLD_CAVITIES = new java.util.LinkedHashMap<>();

    static {
        MOLD_CAVITIES.put("pickaxe_head", new String[] {"..####..", ".######.", "##....##", "#......#", "........", "........"});
        MOLD_CAVITIES.put("axe_head", new String[] {"....##..", "..#####.", "########", "..#####.", "....##..", "........"});
        MOLD_CAVITIES.put("shovel_head", new String[] {"..####..", ".######.", ".######.", ".######.", "..####..", "...##..."});
        MOLD_CAVITIES.put("hoe_head", new String[] {"........", "########", "######..", "##......", "........", "........"});
        MOLD_CAVITIES.put("knife_blade", new String[] {"......##", ".....###", "...####.", ".####...", "###.....", "........"});
        MOLD_CAVITIES.put("hammer_head", new String[] {"........", "########", "########", "...##...", "...##...", "........"});
        MOLD_CAVITIES.put("saw_blade", new String[] {"........", "########", "########", "#.#.#.#.", "........", "........"});
        MOLD_CAVITIES.put("sword_blade", new String[] {"......##", ".....##.", "....##..", "...##...", "..##....", ".#......"});
    }

    /**
     * A tool mold: a square slab seen from above with a front edge, the cavity sunk into it. The cavity's
     * top-left walls are in shadow and its bottom-right walls catch the light.
     */
    /** Cast metal sitting in the cavity, in a neutral cooled-metal ramp, so one texture serves every metal. */
    static final Ramp CAST_METAL = ramp(0xe6dccb, 0x3a3430, 0x5a524a, 0x81766a, 0xa69a8a, 0xc8bca8);

    static BufferedImage filledMold(String[] cavity) {
        return filledMold(CERAMIC, cavity);
    }

    static BufferedImage filledMold(Ramp body, String[] cavity) {
        BufferedImage im = mold(body, cavity);
        int cx = 4, cy = 5;
        java.util.function.BiPredicate<Integer, Integer> in = (x, y) ->
                y >= 0 && y < cavity.length && x >= 0 && x < cavity[y].length() && cavity[y].charAt(x) == '#';
        boolean spec = false;
        for (int y = 0; y < cavity.length; y++)
            for (int x = 0; x < cavity[y].length(); x++) {
                if (!in.test(x, y)) continue;
                int step = 4;
                if (!in.test(x, y - 1) || !in.test(x - 1, y)) step = 3;
                else if (!in.test(x, y + 1) || !in.test(x + 1, y)) step = 5;
                int c = CAST_METAL.get(step);
                if (step == 5 && !spec) { c = CAST_METAL.spec(); spec = true; }
                px(im, cx + x, cy + y, c);
            }
        return im;
    }

    /** The ingot mold with cast metal in it: the cavity's floor pixels turn to metal, lit at the bottom. */
    static BufferedImage filledIngotMold() {
        return filledIngotMold(CERAMIC);
    }

    static BufferedImage filledIngotMold(Ramp body) {
        BufferedImage im = art(body, INGOT_MOLD_ITEM);
        for (int y = 7; y <= 9; y++)
            for (int x = 4; x <= 11; x++) {
                char ch = INGOT_MOLD_ITEM[y].charAt(x);
                if (ch != '1' && ch != '2') continue;
                int c = CAST_METAL.get(y == 7 ? 3 : y == 8 ? 4 : 5);
                if (y == 9 && x == 9) c = CAST_METAL.spec();
                px(im, x, y, c);
            }
        return im;
    }

    static BufferedImage mold(Ramp a, String[] cavity) {
        BufferedImage im = img();
        int left = 2, top = 3, w = 12, h = 9, cx = 4, cy = 5;
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                int step = 4;
                if (y == 0 || x == 0) step = 5;
                else if (x == w - 1) step = 3;
                px(im, left + x, top + y, a.get(step));
            }
        for (int x = 0; x < w; x++) {
            px(im, left + x, top + h, a.get(x == w - 1 ? 2 : 3));
            px(im, left + x, top + h + 1, a.get(2));
        }
        java.util.function.BiPredicate<Integer, Integer> in = (x, y) ->
                y >= 0 && y < cavity.length && x >= 0 && x < cavity[y].length() && cavity[y].charAt(x) == '#';
        for (int y = 0; y < cavity.length; y++)
            for (int x = 0; x < cavity[y].length(); x++) {
                if (!in.test(x, y)) continue;
                int step = 2;
                if (!in.test(x, y - 1) || !in.test(x - 1, y)) step = 1;
                else if (!in.test(x, y + 1) || !in.test(x + 1, y)) step = 3;
                px(im, cx + x, cy + y, a.get(step));
            }
        return outline(im);
    }

    /** Smooth worked clay for the forming grid, with a few broad finger smears. */
    static BufferedImage claySurface() {
        double[][] n = V2.grain(5151);
        BufferedImage im = img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int i = 3 + (int) Math.round((n[y][x] - 0.5) * 2.0);
                px(im, x, y, CLAY.get(Math.max(2, Math.min(4, i))));
            }
        Random r = new Random(5151);
        for (int k = 0; k < 3; k++) {
            int x = r.nextInt(12), y = 2 + r.nextInt(12);
            for (int i = 0; i < 4; i++) {
                pxWrap(im, x + i, y, CLAY.get(4));
                pxWrap(im, x + i, y + 1, CLAY.get(2));
            }
        }
        return im;
    }

    /**
     * Pit-fired ceramic for vessel walls: coil lines every three rows and darker fire clouds where
     * the logs lay against the pot.
     */
    static BufferedImage ceramicWall(long seed, boolean band) {
        double[][] n = V2.grain(seed);
        BufferedImage im = img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int step = n[y][x] > 0.84 ? 4 : 3;
                if (y % 3 == 2 && (x + y) % 7 != 0) step = Math.min(step, 3) - (n[y][x] < 0.4 ? 1 : 0);
                // Fire clouds towards the base.
                if (y > 9 && n[(y + 5) % 16][(x + 3) % 16] > 0.6) step = 2;
                px(im, x, y, CERAMIC.get(Math.max(1, step)));
            }
        if (band) {
            // A cord-impressed band under the shoulder: slanted ticks.
            for (int x = 0; x < 16; x++) {
                px(im, x, 5, CERAMIC.get(x % 3 == 0 ? 2 : 4));
                px(im, x, 6, CERAMIC.get((x + 1) % 3 == 0 ? 2 : 3));
            }
        }
        return im;
    }

    /** The vessel's top: shoulder ring, lit rim, dark mouth. */
    static BufferedImage vesselTop() {
        BufferedImage im = img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double d = Math.max(Math.abs(x - 7.5), Math.abs(y - 7.5));
                int c;
                if (d < 2) c = CHARCOAL.get(1);
                else if (d < 2.6) c = CERAMIC.get(2);
                else if (d < 4) c = (x < 8 || y < 8) ? CERAMIC.get(5) : CERAMIC.get(4);
                else c = CERAMIC.get(n2(x, y) ? 4 : 3);
                px(im, x, y, c);
            }
        return im;
    }

    static boolean n2(int x, int y) { return ((x * 7 + y * 13) % 11) < 4; }

    /** Plain ceramic in one or two steps: a base, a crucible floor. */
    static BufferedImage ceramicPlain(long seed, int low, int high) {
        double[][] n = V2.grain(seed);
        BufferedImage im = img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) px(im, x, y, CERAMIC.get(n[y][x] > 0.8 ? high : low));
        return im;
    }

    /** Stone anvil face: the rock dressed flat, a smoother worked centre with a chiselled border. */
    static BufferedImage stoneAnvilTop(Rock rock) {
        BufferedImage im = rock(rock);
        Ramp p = rock.ramp();
        double[][] n = fractal(rock.name().hashCode() * 7L + 3);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                boolean edge = x == 0 || y == 0 || x == 15 || y == 15;
                boolean border = x == 1 || y == 1 || x == 14 || y == 14;
                if (edge) px(im, x, y, p.get(y == 0 || x == 0 ? 4 : 2));
                else if (border) px(im, x, y, p.get((x + y) % 3 == 0 ? 2 : 3));
                else if (x >= 3 && x <= 12 && y >= 3 && y <= 12) px(im, x, y, p.get(n[y][x] > 0.62 ? 5 : 4));
            }
        // A few bright hammer marks on the working face.
        px(im, 5, 6, p.get(5));
        px(im, 9, 4, p.get(5));
        px(im, 10, 10, p.get(5));
        return im;
    }

    /** Bronze anvil body: cast bronze with a soft vertical sheen. */
    static BufferedImage bronzeAnvilBody() {
        double[][] n = V2.grain(4141);
        BufferedImage im = img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int step = n[y][x] > 0.82 ? 4 : n[y][x] < 0.14 ? 2 : 3;
                if (x == 3 || x == 4) step = Math.min(5, step + 1);
                px(im, x, y, BRONZE.get(step));
            }
        return im;
    }

    /** Bronze anvil face: polished from use, lighter in the middle. */
    static BufferedImage bronzeAnvilTop() {
        double[][] n = V2.grain(4242);
        BufferedImage im = img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double d = Math.abs(x - 7.5) / 8 + Math.abs(y - 7.5) / 16;
                int step = d < 0.45 ? 5 : d < 0.75 ? 4 : 3;
                if (n[y][x] < 0.12) step--;
                px(im, x, y, BRONZE.get(Math.max(2, step)));
            }
        im.setRGB(7, 6, 0xff000000 | BRONZE.spec());
        return im;
    }

    static final String[] TONGS_JAW_ITEM = {
            "................",
            "................",
            "................",
            "...45......45...",
            "...443....443...",
            "....43....43....",
            ".....43..43.....",
            "......4334......",
            "......3s23......",
            ".....32..32.....",
            "....32....32....",
            "...32......32...",
            "................",
            "................",
            "................",
            "................",
    };

    /** Tongs: bronze jaws and pivot at the top right, two wooden handles running down to the bottom left. */
    static final String[] TONGS_ITEM = {
            "................",
            "............45..",
            "..........4453..",
            "..........43....",
            ".........s3..45.",
            "........432.443.",
            ".......d3.443...",
            "......dc.432....",
            ".....dc.d3......",
            "....dc.dc.......",
            "...dc.dc........",
            "..dc.dc.........",
            ".dc.dc..........",
            ".c..c...........",
            "................",
            "................",
    };

    static BufferedImage tongs() {
        return map(V2.BRONZE_V2, V2.WOOD_V2, TONGS_ITEM);
    }

    // ---------------------------------------------------------------- armour (spec 8.4)

    /**
     * Fills a face of an armour layer with overlapping plates: bands three pixels tall, lit along
     * their top edge, with seams that step over band to band and a rivet now and then.
     */
    static void plates(BufferedImage im, Ramp a, int x0, int y0, int w, int h, int fromRow, int toRow, long seed) {
        Random r = new Random(seed);
        for (int y = Math.max(0, fromRow); y < Math.min(h, toRow); y++) {
            int band = (y - fromRow) / 3, inBand = (y - fromRow) % 3;
            for (int x = 0; x < w; x++) {
                int step = inBand == 0 ? 4 : inBand == 1 ? 3 : 2;
                if ((x + band * 2) % 4 == 3) step = Math.max(1, step - 1);
                if (inBand == 0 && x == w - 1) step = 3;
                int c = a.get(step);
                if (inBand == 1 && (x + band * 2) % 4 == 1 && r.nextInt(3) == 0) c = a.spec() != 0 ? a.spec() : a.get(5);
                im.setRGB(x0 + x, y0 + y, 0xff000000 | c);
            }
        }
    }

    /** Fibre cloth padding peeking out under the plates. */
    static void padding(BufferedImage im, int x0, int y0, int w, int fromRow, int toRow) {
        for (int y = fromRow; y < toRow; y++)
            for (int x = 0; x < w; x++) im.setRGB(x0 + x, y0 + y, 0xff000000 | FIBRE.get(((x + y) & 1) == 0 ? 3 : 2));
    }

    /** The helmet, chestplate and boots layer (64 x 32, vanilla humanoid layout). */
    static BufferedImage armourLayer(Ramp a) {
        BufferedImage im = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        // Helmet: top, then right, front, left and back of the head.
        plates(im, a, 8, 0, 8, 8, 0, 8, 1);
        for (int f = 0; f < 4; f++) plates(im, a, f * 8, 8, 8, 8, 0, 6, 2 + f);
        // Cheek guards at the front corners, and a nose guard over an open face.
        for (int f = 0; f < 4; f++) {
            int x0 = f * 8;
            for (int y = 14; y < 16; y++)
                for (int x = 0; x < 8; x++) {
                    boolean guard = f == 1 ? (x < 2 || x > 5) : f == 3 ? false : (f == 0 ? x > 4 : x < 3);
                    if (guard) im.setRGB(x0 + x, y, 0xff000000 | a.get(y == 14 ? 3 : 2));
                }
        }
        for (int y = 12; y < 15; y++) im.setRGB(11, y, 0xff000000 | a.get(y == 12 ? 4 : 3));
        for (int y = 12; y < 15; y++) im.setRGB(12, y, 0xff000000 | a.get(2));
        for (int y = 12; y < 14; y++)
            for (int x = 8; x < 16; x++) if (x < 10 || x > 13) im.setRGB(x, y, 0xff000000 | a.get(3));
        // Chestplate: body top, then right, front, left and back; the bottom rows are padding.
        plates(im, a, 20, 16, 8, 4, 0, 4, 7);
        int[][] body = {{16, 4}, {20, 8}, {28, 4}, {32, 8}};
        for (int[] face : body) {
            plates(im, a, face[0], 20, face[1], 12, 0, 10, face[0]);
            padding(im, face[0], 20, face[1], 10, 12);
        }
        // Pauldrons on the arms.
        plates(im, a, 44, 16, 4, 4, 0, 4, 9);
        for (int f = 0; f < 4; f++) {
            plates(im, a, 40 + f * 4, 20, 4, 12, 0, 5, 10 + f);
            padding(im, 40 + f * 4, 20, 4, 5, 6);
        }
        // Boots: the lower legs, with a cloth cuff.
        for (int f = 0; f < 4; f++) {
            padding(im, f * 4, 20, 4, 6, 7);
            plates(im, a, f * 4, 20, 4, 12, 7, 12, 20 + f);
        }
        plates(im, a, 8, 16, 4, 4, 0, 4, 24);
        return im;
    }

    /** The leggings layer: a belt over the hips and plated thighs. */
    static BufferedImage leggingsLayer(Ramp a) {
        BufferedImage im = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        int[][] body = {{16, 4}, {20, 8}, {28, 4}, {32, 8}};
        for (int[] face : body) {
            padding(im, face[0], 20, face[1], 8, 10);
            for (int x = 0; x < face[1]; x++) {
                im.setRGB(face[0] + x, 30, 0xff000000 | a.get(4));
                im.setRGB(face[0] + x, 31, 0xff000000 | a.get(2));
            }
        }
        im.setRGB(23, 30, 0xff000000 | (a.spec() != 0 ? a.spec() : a.get(5)));
        for (int f = 0; f < 4; f++) {
            plates(im, a, f * 4, 20, 4, 12, 0, 9, 30 + f);
            padding(im, f * 4, 20, 4, 9, 10);
        }
        plates(im, a, 4, 16, 4, 4, 0, 4, 34);
        return im;
    }

    static final String[] HELMET_ITEM = {
            "................",
            "................",
            "................",
            "....44455544....",
            "...4444444443...",
            "..443333333332..",
            "..43........32..",
            "..43........32..",
            "..432......332..",
            "...3........2...",
            "................",
    };
    static final String[] CHESTPLATE_ITEM = {
            "..445....544...",
            ".4444455444433.",
            ".4444444444433.",
            "..34444444433..",
            "...444444443...",
            "...333333332...",
            "...444444443...",
            "...333333332...",
            "...ccccccccb...",
            "...cbcbcbcbb...",
    };
    static final String[] LEGGINGS_ITEM = {
            "..cccccccccb..",
            "..4444554443..",
            "..4433..3332..",
            "..443....332..",
            "..333....222..",
            "..443....332..",
            "..333....222..",
            "..cbc....cbb..",
    };
    static final String[] BOOTS_ITEM = {
            "..cbc....cbc...",
            "..443....443...",
            "..443....443...",
            "..4433...44433.",
            ".44333..443332.",
            ".33222..332221.",
    };

    /** Quern stone side: dressed granite with horizontal tooling marks and a worn top edge. */
    static BufferedImage quernSide() {
        double[][] n = V2.grain(5151);
        BufferedImage im = img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int step = n[y][x] > 0.97 ? 5 : n[y][x] > 0.74 ? 4 : n[y][x] < 0.03 ? 1 : n[y][x] < 0.22 ? 2 : 3;
                // Chisel marks run round the stone.
                if ((y == 2 || y == 8 || y == 13) && (x * 7 + y) % 5 != 0) step = Math.max(2, step - 1);
                if (y == 0 || y == 7 || y == 11) step = 4;
                if (y == 6 || y == 10 || y == 15) step = 2;
                px(im, x, y, GRANITE.get(step));
            }
        return im;
    }

    /** Quern stone face: the grinding surface dressed with radial furrows, lighter where it is worn. */
    static BufferedImage quernFace(long seed, boolean hole) {
        double[][] n = V2.grain(seed);
        BufferedImage im = img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double dx = x - 7.5, dy = y - 7.5;
                double r = Math.sqrt(dx * dx + dy * dy);
                double a = Math.atan2(dy, dx);
                int step = n[y][x] > 0.97 ? 5 : n[y][x] > 0.7 ? 4 : 3;
                // Eight furrows, each a little skewed like real millstone dressing.
                double sector = (a + r * 0.12) / (Math.PI / 4);
                if (Math.abs(sector - Math.round(sector)) < 0.12 && r > 2.5) step = 2;
                if (r > 6.8) step = Math.min(step, 3);
                if (r < 4.2 && !hole) step = Math.max(step, 4);
                if (hole && r < 1.9) step = 1;
                else if (hole && r < 2.9) step = 2;
                px(im, x, y, GRANITE.get(step));
            }
        return im;
    }

    /** A plain quernstone item: a round slab with its centre hole, seen at an angle. */
    static final String[] QUERNSTONE_ITEM = {
            "................",
            "................",
            "................",
            "................",
            ".....444445.....",
            "...4455555554...",
            "..445554455543..",
            "..445542245543..",
            "..344554455433..",
            "..33444444443...",
            "..2333333333322.",
            "...22333333322..",
            ".....2222222....",
            "................",
            "................",
            "................",
    };

    /** Crucible wall: refractory clay, sooty towards the base where it sits in the coals. */
    static BufferedImage crucibleSide() {
        double[][] n = V2.grain(8282);
        BufferedImage im = img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int step = n[y][x] > 0.82 ? 4 : 3;
                if (x % 5 == 1 && n[y][x] < 0.5) step = 3;
                // Thrown from coils: a thick lip at the rim, a shadowed seam under it and a faint coil line.
                if (y < 2) step = 5;
                else if (y == 2) step = 2;
                else if (y == 7 && (x + 2) % 7 != 0) step = 2;
                else if (y == 6 && x % 2 == 0) step = Math.max(step, 4);
                else if (x == 0) step = 5;
                else if (x >= 14) step = Math.max(2, step - 1);
                int c = CERAMIC.get(step);
                if (y > 11 + (int) Math.round(n[y][x] * 2)) c = CHARCOAL.get(n[y][x] > 0.5 ? 4 : 3);
                px(im, x, y, c);
            }
        return im;
    }

    /** Crucible rim seen from above: lit on the near edges. */
    static BufferedImage crucibleTop() {
        double[][] g = V2.grain(8283);
        BufferedImage im = img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) px(im, x, y, CERAMIC.get(((x < 3 || y < 3) ? 4 : 3) + (g[y][x] > 0.75 ? 1 : 0)));
        return im;
    }

    /** Crucible floor and inner walls: darker, with a few glassy flecks of slag. */
    static BufferedImage crucibleInside() {
        BufferedImage im = ceramicPlain(9393, 1, 2);
        Random r = new Random(9393);
        for (int k = 0; k < 4; k++) speck(im, r, r.nextInt(16), r.nextInt(16), SLAG.get(4), SLAG.get(2), 2);
        return im;
    }

    /** Thatch: overlapping straw strands laid at a slight slant, tiling in both directions. */
    static BufferedImage thatch() {
        BufferedImage im = img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) px(im, x, y, STRAW.get(y % 4 == 3 ? 2 : 3));
        Random r = new Random(2468);
        for (int k = 0; k < 26; k++) {
            int x = r.nextInt(16), y = r.nextInt(16), len = 3 + r.nextInt(4);
            int light = r.nextInt(3) == 0 ? 5 : 4;
            for (int i = 0; i < len; i++) pxWrap(im, x + i, y + i / 3, STRAW.get(i == 0 ? light : 4));
            pxWrap(im, x + len, y + len / 3 + 1, STRAW.get(2));
        }
        return im;
    }

    /** Small vessel screen, 176x133: four slots in a row, inventory from y 51. */
    static BufferedImage smallVesselGui() {
        BufferedImage im = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        panel(im, 176, 133);
        for (int i = 0; i < 4; i++) slot(im, 53 + i * 18, 20);
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++) slot(im, 8 + col * 18, 51 + row * 18);
        for (int col = 0; col < 9; col++) slot(im, 8 + col * 18, 109);
        return im;
    }

    // ---------------------------------------------------------------- charcoal pit (spec 4.4)

    /** A log pile end-on: four by four log ends, bark rim, pale wood with a ring and a dark heart. */
    static BufferedImage logPileSide() {
        BufferedImage im = img();
        Random r = new Random(4040);
        for (int ly = 0; ly < 4; ly++)
            for (int lx = 0; lx < 4; lx++) {
                int ox = lx * 4 + ((ly % 2 == 1) ? 2 : 0), oy = ly * 4;
                for (int y = 0; y < 4; y++)
                    for (int x = 0; x < 4; x++) {
                        boolean corner = (x == 0 || x == 3) && (y == 0 || y == 3);
                        int c;
                        if (corner) c = WOOD.get(1);
                        else if (x == 0 || y == 0) c = WOOD.get(3);
                        else if (x == 3 || y == 3) c = WOOD.get(2);
                        else c = (x == 2 && y == 2) ? WOOD.get(3) : WOOD.get(r.nextInt(4) == 0 ? 4 : 5);
                        pxWrap(im, ox + x, oy + y, c);
                    }
            }
        return im;
    }

    /** The pile's top: logs lying side by side, bark with lengthwise grain. */
    static BufferedImage logPileTop() {
        BufferedImage im = img();
        Random r = new Random(4141);
        for (int y = 0; y < 16; y++) {
            int row = y % 4;
            for (int x = 0; x < 16; x++) {
                int step = row == 0 ? 4 : row == 3 ? 1 : 3;
                if (row != 0 && row != 3 && r.nextInt(6) == 0) step = 2;
                px(im, x, y, WOOD.get(step));
            }
        }
        return im;
    }

    /** Burnt-out pile: charcoal lumps with lit facets, grey ash in the gaps. */
    static BufferedImage charcoalPile() {
        double[][] n = fractal(5050);
        BufferedImage im = img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double v = n[y][x];
                int c = v < 0.24 ? ASH.get(2) : CHARCOAL.get(v > 0.58 ? 3 : 2);
                px(im, x, y, c);
            }
        Random r = new Random(5050);
        for (int k = 0; k < 9; k++) speck(im, r, r.nextInt(16), r.nextInt(16), CHARCOAL.get(5), CHARCOAL.get(1), 2 + r.nextInt(2));
        return im;
    }

    static final String[] ASH_HEAP = {
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            ".......45.......",
            ".....44443......",
            "....4544433.....",
            "...454443332....",
            "..44443433322...",
            "..4443333322....",
            "...333322221....",
            ".....2222.......",
            "................",
            "................",
    };

    // ---------------------------------------------------------------- forge (spec 4.5)

    /** Fired bricks in courses of four, mortar in ramp step 1, each brick lit along its top edge. */
    static BufferedImage forgeBricks(long seed) { return V2.bricks(V2.BRICK_V2, seed); }

    /** Unlit hearth: cold charcoal lumps. */
    static BufferedImage forgeCoals() {
        double[][] n = fractal(6060);
        BufferedImage im = img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) px(im, x, y, CHARCOAL.get(n[y][x] > 0.6 ? 3 : n[y][x] < 0.3 ? 1 : 2));
        Random r = new Random(6060);
        for (int k = 0; k < 10; k++) speck(im, r, r.nextInt(16), r.nextInt(16), CHARCOAL.get(5), CHARCOAL.get(1), 2);
        return im;
    }

    /** Forge screen, 176x170: four heating slots, the fuel slot under the flames, a tall heat gauge. */
    static BufferedImage forgeGui() {
        BufferedImage im = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        panel(im, 176, 170);
        for (int i = 0; i < 4; i++) {
            slot(im, 44 + i * 18, 18);
            // Heat strip under each slot.
            well(im, 44 + i * 18 - 1, 35, 18, 4, 0x2a2a2a);
        }
        slot(im, 80, 58);
        for (int y = 0; y < FLAME_SILHOUETTE.length; y++)
            for (int x = 0; x < 14; x++)
                if (FLAME_SILHOUETTE[y].charAt(x) == '#') im.setRGB(81 + x, 40 + y, 0xff000000 | SLOT_FILL);
        // Gauge: 0 to 1750 degrees over 58 px, a notch every 250.
        well(im, 150, 16, 12, 60, 0x2a2a2a);
        for (int t = 250; t < 1750; t += 250) {
            int y = 17 + 58 - Math.round(t / 1750f * 58);
            fill(im, 147, y, 3, 1, t % 500 == 0 ? GUI_SHADOW : SLOT_FILL);
        }
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++) slot(im, 8 + col * 18, 88 + row * 18);
        for (int col = 0; col < 9; col++) slot(im, 8 + col * 18, 146);
        return im;
    }

    /** Crucible (spec 7.1): 3x3 inputs, mold slot under them, melt bar, pour progress and gauge. */
    static BufferedImage crucibleGui() {
        BufferedImage im = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        panel(im, 176, 210);
        for (int i = 0; i < 9; i++) slot(im, 8 + (i % 3) * 18, 18 + (i / 3) * 18);
        slot(im, 8, 78);
        // A faint ingot shape in the empty mold slot.
        for (int y = 0; y < INGOT.length; y++)
            for (int x = 0; x < INGOT[y].length(); x++)
                if (INGOT[y].charAt(x) != '.') im.setRGB(11 + x, 82 + y, 0xff000000 | SLOT_FILL - 0x080808);
        // Melt bar and pour progress.
        well(im, 65, 17, 10, 54, 0x2a2a2a);
        for (int u = 1; u < 4; u++) fill(im, 75, 17 + 1 + Math.round(52 - u * 13f), 2, 1, SLOT_FILL);
        well(im, 71, 83, 32, 6, 0x2a2a2a);
        // Gauge: 0 to 1750 degrees over 52 px, a notch every 250.
        well(im, 159, 17, 10, 54, 0x2a2a2a);
        for (int t = 250; t < 1750; t += 250) {
            int y = 18 + 52 - Math.round(t / 1750f * 52);
            fill(im, 156, y, 3, 1, t % 500 == 0 ? GUI_SHADOW : SLOT_FILL);
        }
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++) slot(im, 8 + col * 18, 128 + row * 18);
        for (int col = 0; col < 9; col++) slot(im, 8 + col * 18, 186);
        return im;
    }

    /** Anvil (spec 9.2): workpiece, plans, work bar, eight hit buttons, rule and recent-hit boxes. */
    static BufferedImage anvilGui() {
        BufferedImage im = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        panel(im, 176, 252);
        slot(im, 8, 27);
        slot(im, 152, 27);
        for (int i = 0; i < 12; i++) slot(im, 30 + (i % 6) * 18, 18 + (i / 6) * 18);
        well(im, 25, 26, 4, 18, 0x2a2a2a);
        // A small arrow from the plans to the finished piece.
        for (int j = 0; j < 7; j++) {
            int d = Math.abs(j - 3);
            for (int i = 0; i < 9 - d; i++) if (i < 5 ? d <= 1 : true) im.setRGB(139 + i, 32 + j, 0xff000000 | SLOT_FILL);
        }
        // Work bar: 0 to 150 with a notch every 25.
        well(im, 12, 60, 152, 8, 0x2a2a2a);
        for (int t = 25; t < 150; t += 25) fill(im, 13 + t, 68, 1, 2, t % 50 == 0 ? GUI_SHADOW : SLOT_FILL);
        for (int i = 0; i < 3; i++) {
            well(im, 8 + i * 20, 106, 18, 18, SLOT_FILL);
            well(im, 116 + i * 18, 106, 18, 18, SLOT_FILL);
        }
        // Weld row: second piece, flux, the weld button, and the pattern slot on the right.
        slot(im, 8, 130);
        slot(im, 26, 130);
        slot(im, 152, 130);
        ghost(im, 8, 130, GHOST_INGOT);
        ghost(im, 26, 130, GHOST_FLUX);
        ghost(im, 152, 130, GHOST_PATTERN);
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++) slot(im, 8 + col * 18, 173 + row * 18);
        for (int col = 0; col < 9; col++) slot(im, 8 + col * 18, 231);

        // Sprites: button faces (normal, hovered, disabled), then the hit icons.
        int[] faces = {0xa8a8a8, 0xc8c8d8, 0x6e6e6e};
        for (int b = 0; b < 3; b++) {
            int x0 = 176 + b * 18;
            fill(im, x0, 0, 18, 18, faces[b]);
            fill(im, x0, 0, 18, 1, 0x000000);
            fill(im, x0, 17, 18, 1, 0x000000);
            fill(im, x0, 0, 1, 18, 0x000000);
            fill(im, x0 + 17, 0, 1, 18, 0x000000);
            fill(im, x0 + 1, 1, 16, 1, b == 2 ? 0x8a8a8a : 0xffffff);
            fill(im, x0 + 1, 1, 1, 16, b == 2 ? 0x8a8a8a : 0xffffff);
            fill(im, x0 + 1, 16, 16, 1, b == 2 ? 0x4a4a4a : 0x555555);
            fill(im, x0 + 16, 1, 1, 16, b == 2 ? 0x4a4a4a : 0x555555);
        }
        int[] red = {0x5a1a10, 0xa0281a, 0xe0603a};
        int[] green = {0x1e4a1e, 0x3a8a3a, 0x7ac07a};
        for (int i = 0; i < 8; i++) {
            BufferedImage icon = hitIcon(i < 4 ? red : green, i < 4, i % 4);
            im.getGraphics().drawImage(icon, 176 + (i % 4) * 16, 18 + (i / 4) * 16, null);
        }
        im.getGraphics().drawImage(anyHitIcon(), 240, 18, null);
        // Weld button faces (normal, hovered, disabled) with two bars meeting in a spark.
        for (int b = 0; b < 3; b++) {
            int x0 = 176 + b * 18;
            im.getGraphics().drawImage(im.getSubimage(176 + b * 18, 0, 18, 18), x0, 52, null);
            drawRows(im, x0 + 1, 53, WELD_ICON, b == 2
                    ? new int[] {0x5a5a5a, 0x6a6a6a, 0x7a7a7a, 0x8a8a8a, 0x8a8a8a}
                    : new int[] {0x3a3d48, 0x838998, 0xd6dbe2, 0xf0a030, 0xfff0a0});
        }
        // Quick-smith button faces (normal, hovered, disabled) at v=72: two forward chevrons, like a fast-forward.
        for (int b = 0; b < 3; b++) {
            int x0 = 176 + b * 18;
            im.getGraphics().drawImage(im.getSubimage(176 + b * 18, 0, 18, 18), x0, 72, null);
            int[] ink = b == 2 ? new int[] {0x5a5a5a, 0x7a7a7a} : new int[] {0x3a3d48, 0xf0a030};
            for (int c = 0; c < 2; c++) {
                for (int i = 0; i < 9; i++) {
                    int dx = i < 5 ? i : 8 - i;
                    int x = x0 + 4 + c * 5 + dx;
                    im.setRGB(x, 77 + i, 0xff000000 | ink[0]);
                    im.setRGB(x + 1, 77 + i, 0xff000000 | ink[1]);
                    im.setRGB(x + 2, 77 + i, 0xff000000 | ink[1]);
                    im.setRGB(x + 3, 77 + i, 0xff000000 | ink[0]);
                }
            }
        }
        return im;
    }

    static final String[] WELD_ICON = {
            "................",
            ".......5........",
            "....5..4..5.....",
            ".....4.4.4......",
            "......444.......",
            "..1111.4.1111...",
            "..2223...3222...",
            "..3332...2333...",
            "..1111...1111...",
            "................",
            "................",
            "..111111111111..",
            "..222223322222..",
            "..333332233333..",
            "..111111111111..",
            "................",
    };

    static final String[] GHOST_INGOT = {
            "................",
            "................",
            "................",
            "................",
            "................",
            "....########....",
            "...#........#...",
            "..#..........#..",
            ".##############.",
            ".#............#.",
            ".#............#.",
            ".##############.",
            "................",
            "................",
            "................",
            "................",
    };

    static final String[] GHOST_FLUX = {
            "................",
            "................",
            "................",
            "................",
            "................",
            ".......#........",
            "....#......#....",
            "..........#.....",
            "......###.......",
            "....#######.....",
            "...#########....",
            "..###########...",
            ".#############..",
            "................",
            "................",
            "................",
    };

    static final String[] GHOST_PATTERN = {
            "................",
            "...#########....",
            "...#.......##...",
            "...#.......#.#..",
            "...#.......####.",
            "...#..........#.",
            "...#.###.##...#.",
            "...#..........#.",
            "...#.##.####..#.",
            "...#..........#.",
            "...#.####.##..#.",
            "...#..........#.",
            "...#.###......#.",
            "...#..........#.",
            "...############.",
            "................",
    };

    /** A faint outline in an empty slot showing what goes there. */
    static void ghost(BufferedImage im, int itemX, int itemY, String[] rows) {
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++)
                if (rows[y].charAt(x) == '#') im.setRGB(itemX + x, itemY + y, 0xff7a7a7a);
    }

    /** Pixel rows where '1' to '5' pick from a five-colour palette. */
    static void drawRows(BufferedImage im, int x0, int y0, String[] rows, int[] palette) {
        for (int y = 0; y < rows.length; y++)
            for (int x = 0; x < rows[y].length(); x++) {
                char ch = rows[y].charAt(x);
                if (ch >= '1' && ch <= '5') im.setRGB(x0 + x, y0 + y, 0xff000000 | palette[ch - '1']);
            }
    }

    /**
     * A hit icon: chevrons pointing the way the hit moves the workpiece, one to three by strength;
     * the fourth of each group (draw, shrink) is a big chevron against a stop bar.
     */
    static BufferedImage hitIcon(int[] c, boolean left, int strength) {
        // Drawn pointing left, then mirrored for the right-hand group.
        BufferedImage im = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        java.util.function.BiConsumer<Integer, Integer> chevron = (tip, size) -> {
            for (int k = -size; k <= size; k++) {
                int x = tip + Math.abs(k), y = 8 + k;
                im.setRGB(x, y, 0xff000000 | c[1]);
                im.setRGB(x + 1, y, 0xff000000 | (k < 0 ? c[2] : c[1]));
                if (x + 2 < 16) im.setRGB(x + 2, y, 0xff000000 | c[0]);
            }
        };
        if (strength < 3) {
            int n = strength + 1;
            int start = (16 - ((n - 1) * 4 + 5)) / 2;
            for (int j = 0; j < n; j++) chevron.accept(start + j * 4, 4);
        } else {
            chevron.accept(6, 5);
            for (int y = 2; y <= 13; y++) {
                im.setRGB(3, y, 0xff000000 | c[1]);
                im.setRGB(4, y, 0xff000000 | c[0]);
            }
        }
        if (left) return im;
        BufferedImage mirrored = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) mirrored.setRGB(15 - x, y, im.getRGB(x, y));
        return mirrored;
    }

    /** "Any hit" for rules: a small grey hammer. */
    static BufferedImage anyHitIcon() {
        String[] rows = {
                "................",
                "................",
                "......5554......",
                ".....455443.....",
                ".....444433.....",
                "......2332......",
                ".......a........",
                ".......b........",
                ".......a........",
                ".......b........",
                ".......a........",
                ".......b........",
                "................",
                "................",
                "................",
                "................",
        };
        BufferedImage im = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Ramp metal = ramp(0, 0x3a3d48, 0x5c6170, 0x838998, 0xaeb4c0, 0xd6dbe2);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                char ch = rows[y].charAt(x);
                if (ch >= '1' && ch <= '5') im.setRGB(x, y, 0xff000000 | metal.get(ch - '0'));
                else if (ch == 'a') im.setRGB(x, y, 0xff000000 | WOOD.get(4));
                else if (ch == 'b') im.setRGB(x, y, 0xff000000 | WOOD.get(3));
            }
        return im;
    }

    // ---------------------------------------------------------------- GUI

    static final int GUI_FACE = 0xc6c6c6, GUI_LIGHT = 0xffffff, GUI_SHADOW = 0x555555, GUI_EDGE = 0x000000;
    static final int SLOT_FILL = 0x8b8b8b, SLOT_DARK = 0x373737;

    static void fill(BufferedImage im, int x, int y, int w, int h, int rgb) {
        for (int j = y; j < y + h; j++)
            for (int i = x; i < x + w; i++) im.setRGB(i, j, 0xff000000 | rgb);
    }

    /** Vanilla container panel: black rim with cut corners, white top-left bevel, grey bottom-right. */
    static void panel(BufferedImage im, int w, int h) {
        fill(im, 0, 0, w, h, GUI_FACE);
        for (int i = 0; i < w; i++) { im.setRGB(i, 0, 0); im.setRGB(i, h - 1, 0); }
        for (int j = 0; j < h; j++) { im.setRGB(0, j, 0); im.setRGB(w - 1, j, 0); }
        fill(im, 1, 1, w - 3, 2, GUI_LIGHT);
        fill(im, 1, 1, 2, h - 3, GUI_LIGHT);
        fill(im, 3, h - 3, w - 4, 2, GUI_SHADOW);
        fill(im, w - 3, 3, 2, h - 4, GUI_SHADOW);
        // Rounded corners: clear the outer corner pixels and step the rim inwards.
        int[][] corners = {{0, 0}, {w - 1, 0}, {0, h - 1}, {w - 1, h - 1}};
        for (int[] c : corners) im.setRGB(c[0], c[1], 0);
        clear(im, 0, 0); clear(im, 1, 0); clear(im, 0, 1);
        clear(im, w - 1, 0); clear(im, w - 2, 0); clear(im, w - 1, 1);
        clear(im, 0, h - 1); clear(im, 1, h - 1); clear(im, 0, h - 2);
        clear(im, w - 1, h - 1); clear(im, w - 2, h - 1); clear(im, w - 1, h - 2);
        im.setRGB(1, 1, 0xff000000); im.setRGB(w - 2, 1, 0xff000000);
        im.setRGB(1, h - 2, 0xff000000); im.setRGB(w - 2, h - 2, 0xff000000);
        im.setRGB(2, 2, 0xff000000 | GUI_LIGHT);
        im.setRGB(w - 3, h - 3, 0xff000000 | GUI_SHADOW);
    }

    static void clear(BufferedImage im, int x, int y) { im.setRGB(x, y, 0); }

    /** A recessed well: dark top-left, white bottom-right, like a vanilla slot. */
    static void well(BufferedImage im, int x, int y, int w, int h, int fillRgb) {
        fill(im, x, y, w, h, fillRgb);
        fill(im, x, y, w - 1, 1, SLOT_DARK);
        fill(im, x, y, 1, h - 1, SLOT_DARK);
        fill(im, x + 1, y + h - 1, w - 1, 1, GUI_LIGHT);
        fill(im, x + w - 1, y + 1, 1, h - 1, GUI_LIGHT);
        im.setRGB(x + w - 1, y, 0xff000000 | SLOT_FILL);
        im.setRGB(x, y + h - 1, 0xff000000 | SLOT_FILL);
    }

    static void slot(BufferedImage im, int itemX, int itemY) {
        well(im, itemX - 1, itemY - 1, 18, 18, SLOT_FILL);
    }

    /** The vanilla crafting arrow, pointing right. */
    static void arrow(BufferedImage im, int x, int y) {
        int c = 0xff000000 | SLOT_FILL;
        for (int j = 0; j < 15; j++) {
            int d = Math.abs(j - 7);
            if (d <= 2) for (int i = 0; i < 15; i++) im.setRGB(x + i, y + j, c);
            for (int i = 15; i < 22 - d; i++) im.setRGB(x + i, y + j, c);
        }
    }

    static final String[] FLAME_SILHOUETTE = {
            "......#.......",
            ".....##.......",
            ".....###......",
            "....####.#....",
            "....#######...",
            "...########...",
            "..#########...",
            "..##########..",
            ".###########..",
            ".############.",
            ".############.",
            "##############",
            ".############.",
            "..##########..",
    };

    /** Fire pit screen, 176x166: cooking slot over the flames, fuel below, heat gauge on the right. */
    static BufferedImage firePitGui() {
        BufferedImage im = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        panel(im, 176, 166);
        slot(im, 80, 18);
        slot(im, 80, 54);
        for (int y = 0; y < FLAME_SILHOUETTE.length; y++)
            for (int x = 0; x < 14; x++)
                if (FLAME_SILHOUETTE[y].charAt(x) == '#') im.setRGB(81 + x, 37 + y, 0xff000000 | SLOT_FILL);
        // Cooking progress bar.
        well(im, 99, 23, 24, 6, 0x2a2a2a);
        // Heat gauge: 0 to 700 degrees over 54 px, with a notch where cooking starts (200).
        well(im, 150, 16, 12, 56, 0x2a2a2a);
        int cookY = 17 + 54 - Math.round(200 / 700f * 54);
        fill(im, 147, cookY, 3, 1, GUI_SHADOW);
        for (int t = 100; t < 700; t += 100) {
            int y = 17 + 54 - Math.round(t / 700f * 54);
            if (t != 200) fill(im, 148, y, 2, 1, SLOT_FILL);
        }
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++) slot(im, 8 + col * 18, 84 + row * 18);
        for (int col = 0; col < 9; col++) slot(im, 8 + col * 18, 142);
        return im;
    }

    /**
     * Knapping screen, 176x196 on a 256x256 sheet: a 5x5 stone well at (17,18), arrow, large result
     * slot at (134,50) and the player inventory from y 114. Positions match KnappingMenu.
     */
    static BufferedImage knappingGui() {
        BufferedImage im = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        panel(im, 176, 196);
        // The grid well sits one pixel outside the 80x80 cell area; the floor is darker, like a work surface.
        well(im, 16, 17, 82, 82, 0x6f6f6f);
        arrow(im, 104, 51);
        // Large result slot like the crafting table's.
        well(im, 129, 45, 26, 26, SLOT_FILL);
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++) slot(im, 8 + col * 18, 114 + row * 18);
        for (int col = 0; col < 9; col++) slot(im, 8 + col * 18, 172);
        repeatButton(im);
        return im;
    }

    /** Repeat-last button faces (normal, hovered, disabled) at (176,0): a circular arrow, inked like the anvil's quick button. */
    static void repeatButton(BufferedImage im) {
        int[] faces = {0xa8a8a8, 0xc8c8d8, 0x6e6e6e};
        for (int b = 0; b < 3; b++) {
            int x0 = 176 + b * 18;
            fill(im, x0, 0, 18, 18, faces[b]);
            fill(im, x0, 0, 18, 1, 0x000000);
            fill(im, x0, 17, 18, 1, 0x000000);
            fill(im, x0, 0, 1, 18, 0x000000);
            fill(im, x0 + 17, 0, 1, 18, 0x000000);
            fill(im, x0 + 1, 1, 16, 1, b == 2 ? 0x8a8a8a : 0xffffff);
            fill(im, x0 + 1, 1, 1, 16, b == 2 ? 0x8a8a8a : 0xffffff);
            fill(im, x0 + 1, 16, 16, 1, b == 2 ? 0x4a4a4a : 0x555555);
            fill(im, x0 + 16, 1, 1, 16, b == 2 ? 0x4a4a4a : 0x555555);
            int[] ink = b == 2 ? new int[] {0x5a5a5a, 0x7a7a7a} : new int[] {0x3a3d48, 0xf0a030};
            boolean[][] mark = new boolean[18][18];
            double cx = 8.5, cy = 9.5;
            // Ring open at the top right, with an arrowhead where it ends.
            for (int y = 3; y < 16; y++) {
                for (int x = 3; x < 15; x++) {
                    double d = Math.hypot(x + 0.5 - cx, y + 0.5 - cy);
                    double a = Math.toDegrees(Math.atan2(-(y + 0.5 - cy), x + 0.5 - cx));
                    if (d >= 3.0 && d <= 4.7 && !(a > 5 && a < 65)) mark[y][x] = true;
                }
            }
            int[][] head = {{11, 3}, {12, 3}, {13, 3}, {14, 3}, {12, 4}, {13, 4}, {14, 4}, {13, 5}, {14, 5}, {14, 6}};
            for (int[] h : head) mark[h[1]][h[0]] = true;
            for (int y = 0; y < 18; y++) {
                for (int x = 0; x < 18; x++) {
                    if (mark[y][x]) continue;
                    boolean near = false;
                    for (int dy = -1; dy <= 1; dy++)
                        for (int dx = -1; dx <= 1; dx++) {
                            int yy = y + dy, xx = x + dx;
                            if (yy >= 0 && yy < 18 && xx >= 0 && xx < 18 && mark[yy][xx]) near = true;
                        }
                    if (near) im.setRGB(x0 + x, y, 0xff000000 | ink[0]);
                }
            }
            for (int y = 0; y < 18; y++)
                for (int x = 0; x < 18; x++)
                    if (mark[y][x]) im.setRGB(x0 + x, y, 0xff000000 | ink[1]);
        }
    }

    // ---------------------------------------------------------------- heat glow

    /** Metal items that go in the forge: ingots, nuggets, plates, cast heads and blades, the tongs jaw. */
    static final java.util.regex.Pattern HEATABLE =
            java.util.regex.Pattern.compile("(?!stone_|unfired_|wooden_).*(_ingot|_nugget|(?<!chest)_plate|_head|_blade|_rod|_gear)|tongs_jaw|raw_bloom");

    /**
     * A pale copy of each heatable item: its shading kept as light greys so the heat tint reads as glowing
     * metal with the same detail, and the outline a little darker to hold the shape.
     */
    static void glowLayers() throws IOException {
        File[] items = OUT.resolve("item").toFile().listFiles((dir, name) -> name.endsWith(".png"));
        if (items == null) return;
        for (File file : items) {
            String name = file.getName().substring(0, file.getName().length() - 4);
            if (!HEATABLE.matcher(name).matches()) continue;
            BufferedImage src = ImageIO.read(file);
            BufferedImage glow = img();
            for (int y = 0; y < 16; y++)
                for (int x = 0; x < 16; x++) {
                    int argb = src.getRGB(x, y);
                    int a = argb >>> 24;
                    if (a == 0) continue;
                    double l = (0.299 * (argb >> 16 & 0xFF) + 0.587 * (argb >> 8 & 0xFF) + 0.114 * (argb & 0xFF)) / 255.0;
                    boolean edge = isEdge(src, x, y);
                    int v = (int) Math.round(255 * (edge ? 0.45 + 0.35 * l : 0.62 + 0.38 * l));
                    glow.setRGB(x, y, a << 24 | v << 16 | v << 8 | v);
                }
            saveRaw("item/glow/" + name, glow);
        }
    }

    static boolean isEdge(BufferedImage im, int x, int y) {
        int[][] around = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] d : around) {
            int nx = x + d[0], ny = y + d[1];
            if (nx < 0 || ny < 0 || nx > 15 || ny > 15 || im.getRGB(nx, ny) >>> 24 == 0) return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- tier 3: iron (spec 20)

    // New ramps (spec 20.1).
    static final Ramp WROUGHT_IRON = ramp(0xdcd8cf, 0x2a2b30, 0x45464c, 0x6a6a6c, 0x8e8c88, 0xb4b0a8);
    static final Ramp GOLD = ramp(0xfcf0bc, 0x4a3010, 0x8a5e18, 0xc08a26, 0xe2b440, 0xf4d878);
    static final Ramp HEMATITE = ramp(0, 0x2a1416, 0x4a2020, 0x6e3226, 0x8e4a34, 0xae6a4c);
    static final Ramp MAGNETITE = ramp(0x9aa0aa, 0x141620, 0x20232c, 0x32353e, 0x4a4e58, 0x686c76);
    static final Ramp LIMONITE = ramp(0, 0x3a2410, 0x5a3a16, 0x80561e, 0xa6762c, 0xc69a48);
    static final Ramp FIRE_CLAY = ramp(0, 0x5e5a50, 0x7a7466, 0x9c9480, 0xbcb49c, 0xd6ceb4);
    static final Ramp FIRE_BRICK = ramp(0, 0x5e4a36, 0x7a6248, 0x927a58, 0xa68e68, 0xbaa27a);
    static final Ramp LIGNITE = ramp(0, 0x1a1410, 0x2a2018, 0x3c2e22, 0x52402e, 0x6a543c);
    // Earths for the tier 3 ground blocks, built per SG 3 (low contrast, cool darks, warm lights).
    static final Ramp MUD = ramp(0, 0x1a1818, 0x29241f, 0x383028, 0x483d32, 0x5c4e40);
    static final Ramp SAND = ramp(0, 0x8a7656, 0xa69068, 0xc0aa7c, 0xd4c092, 0xe6d6ac);

    static final List<Mineral> T3_MINERALS = List.of(
            new Mineral("hematite", ORE_HEMATITE, false),
            new Mineral("magnetite", ORE_MAGNETITE, true),
            new Mineral("native_gold", ORE_GOLD, true),
            new Mineral("limonite", ORE_LIMONITE, false));

    /** Heat band (SG 6), dark red to white, for the lit bloomery door. */
    static final int[] HEAT_BAND = {0x6e1e14, 0xa0281a, 0xd23a1e, 0xf07a22, 0xf8c23a, 0xfff4d0};

    /** Fire clay: smooth like vanilla clay, fine per-pixel grain in three tones, no clods. */
    static BufferedImage fireClayBlock() {
        return V2.paint(V2.quantize(V2.terrainField(2020, 0.8), new int[] {0, 26, 52, 22, 0}), FIRE_CLAY);
    }

    /** The value below which the given fraction of the tile's samples fall. */
    static double quantile(double[][] n, double q) {
        double[] all = new double[256];
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) all[y * 16 + x] = n[y][x];
        java.util.Arrays.sort(all);
        return all[(int) Math.min(255, Math.round(q * 255))];
    }

    /** Vertical joint positions per course for the fire brick bond (bricks 4-5 px, staggered). */
    static final int[][] FIRE_BRICK_JOINTS = {{2, 7, 12}, {4, 10, 15}, {1, 7, 12}, {4, 9, 15}};

    /** Pale fire bricks, four courses of small bricks, thin dark mortar (spec 20.2). */
    static BufferedImage fireBricks() { return V2.bricks(FIRE_BRICK, 3131); }

    /** Lignite: sedimentary beds in the lignite ramp, broken partings, woody fibre streaks, no specular. */
    static BufferedImage ligniteSeam() {
        Random r = new Random(6464);
        int[] beds = new int[16];
        int tone = 3, y = 0;
        while (y < 16) {
            int h = 3 + r.nextInt(3);
            for (int i = 0; i < h && y < 16; i++, y++) beds[y] = tone;
            tone = tone == 3 ? (r.nextBoolean() ? 4 : 2) : 3;
        }
        double[][] n = fractal(6464);
        double lo = quantile(n, 0.15), hi = quantile(n, 0.85);
        BufferedImage im = img();
        for (y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int s = beds[y] + (n[y][x] > hi ? 1 : n[y][x] < lo ? -1 : 0);
                px(im, x, y, LIGNITE.get(Math.max(2, Math.min(4, s))));
            }
        for (y = 0; y < 16; y++) {
            if (beds[y] == beds[Math.floorMod(y - 1, 16)]) continue;
            int x = r.nextInt(16), len = 4 + r.nextInt(5);
            for (int i = 0; i < len; i++) pxWrap(im, x + i, y, LIGNITE.get(1));
            int x2 = x + len + 3 + r.nextInt(3);
            for (int i = 0; i < 2; i++) pxWrap(im, x2 + i, y, LIGNITE.get(2));
        }
        // Woody streaks: thin step 4 fibres with a darker tail, only on the darker beds.
        int placed = 0;
        for (int attempts = 0; placed < 4 && attempts < 200; attempts++) {
            int x = r.nextInt(16), yy = r.nextInt(16), len = 3 + r.nextInt(4);
            if (beds[yy] > 3 || rgb(im, x, yy) == LIGNITE.get(1)) continue;
            for (int i = 0; i < len; i++) pxWrap(im, x + i, yy, LIGNITE.get(i == len - 1 ? 3 : 4));
            placed++;
        }
        return im;
    }

    /** Dark wet mud: soft patches, puddled dark spots and a few wet sheen highlights. */
    static BufferedImage mud() {
        double[][] n = fractal(7272);
        BufferedImage im = img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) px(im, x, y, MUD.get(n[y][x] < 0.32 ? 1 : n[y][x] < 0.6 ? 2 : 3));
        Random r = new Random(7272);
        for (int k = 0; k < 5; k++) speck(im, r, r.nextInt(16), r.nextInt(16), MUD.get(4), MUD.get(2), 3);
        for (int k = 0; k < 3; k++) {
            int x = r.nextInt(16), y = r.nextInt(16);
            pxWrap(im, x, y, MUD.get(5));
            pxWrap(im, x + 1, y, MUD.get(4));
            pxWrap(im, x, y + 1, MUD.get(1));
        }
        return im;
    }

    /** A pebble lit from the top-left with a shadow pixel row under it. */
    static void pebble(BufferedImage im, Ramp p, int x, int y, int w, int h, boolean shine) {
        for (int j = 0; j < h; j++)
            for (int i = 0; i < w; i++) {
                int step = (i == 0 || j == 0) ? 4 : 3;
                if (i == w - 1 || j == h - 1) step = (i == 0 || j == 0) ? 3 : 2;
                if (i == 0 && j == 0 && shine) step = 5;
                pxWrap(im, x + i, y + j, p.get(step));
            }
        for (int i = 1; i <= w; i++) pxWrap(im, x + i, y + h, p.get(1));
    }

    /** Gold glints: single step 5 pixels of the gold ramp, each on a shaded pixel so it reads. */
    static void glints(BufferedImage im, Ramp ground, long seed, int count) {
        Random r = new Random(seed);
        List<int[]> placed = new ArrayList<>();
        while (placed.size() < count) {
            int x = 1 + r.nextInt(14), y = 1 + r.nextInt(14);
            boolean clash = false;
            for (int[] q : placed) if (Math.abs(q[0] - x) < 5 && Math.abs(q[1] - y) < 5) clash = true;
            if (clash) continue;
            placed.add(new int[] {x, y});
            px(im, x, y, GOLD.get(5));
            px(im, x + 1, y, ground.get(2));
            px(im, x, y + 1, ground.get(2));
            px(im, x - 1, y, ground.get(3));
        }
    }

    /** Gravel: small rounded stones of mixed rock on a jittered grid, dark gaps, lit top-left. */
    static BufferedImage placerGravel() {
        Random r = new Random(8181);
        int cells = 4;
        double pitch = 16.0 / cells;
        double[][] centre = new double[cells * cells][2];
        Ramp[] ramps = new Ramp[cells * cells];
        int[] base = new int[cells * cells];
        Ramp[] pool = {FIELD_STONE, FIELD_STONE, FIELD_STONE, GRANITE, FLINT, FIELD_STONE};
        for (int i = 0; i < centre.length; i++) {
            centre[i][0] = (i % cells + 0.25 + r.nextDouble() * 0.5) * pitch;
            centre[i][1] = (i / cells + 0.25 + r.nextDouble() * 0.5) * pitch;
            ramps[i] = pool[r.nextInt(pool.length)];
            base[i] = 3 + (r.nextInt(3) == 0 ? 1 : 0);
        }
        BufferedImage im = img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int best = 0;
                double bd = 1e9, sd = 1e9, bdx = 0, bdy = 0;
                for (int i = 0; i < centre.length; i++)
                    for (int ox = -16; ox <= 16; ox += 16)
                        for (int oy = -16; oy <= 16; oy += 16) {
                            double dx = x + 0.5 - centre[i][0] - ox, dy = y + 0.5 - centre[i][1] - oy;
                            double d = Math.sqrt(dx * dx + dy * dy);
                            if (d < bd) { sd = bd; bd = d; best = i; bdx = dx; bdy = dy; }
                            else if (d < sd) sd = d;
                        }
                if (sd - bd < 0.6) { px(im, x, y, FIELD_STONE.get(bdx + bdy > 0 ? 1 : 2)); continue; }
                double rel = bdx + bdy;
                int step = base[best] + (rel < -1.2 ? 1 : rel > 1.0 ? -1 : 0);
                if (rel < -2.4 && base[best] == 4) step = 5;
                px(im, x, y, ramps[best].get(Math.max(1, Math.min(5, step))));
            }
        glints(im, FIELD_STONE, 8182, 3);
        return im;
    }

    static BufferedImage placerSand() {
        double[][] n = V2.grain(9191);
        BufferedImage im = img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) px(im, x, y, SAND.get(n[y][x] < 0.42 ? 3 : 4));
        Random r = new Random(9191);
        for (int k = 0; k < 8; k++) speck(im, r, r.nextInt(16), r.nextInt(16), SAND.get(5), SAND.get(4), 2);
        for (int k = 0; k < 8; k++) speck(im, r, r.nextInt(16), r.nextInt(16), SAND.get(3), SAND.get(2), 2);
        glints(im, SAND, 9192, 3);
        return im;
    }

    /**
     * Bloomery door: a riveted copper plate set into the fire brick face, a dark slot across it.
     * 'm' mortar, 'b' brick, digits copper, 'r' rivet, 'k'/'K' slot (glows when lit), 'l' lower lip.
     */
    static final String[] BLOOMERY_DOOR = {
            "mmmmmmmmmm",
            "m54444443m",
            "m4r3343r2m",
            "m43333332m",
            "m43343332m",
            "m4kkkkkk2m",
            "m4KKKKKK2m",
            "m4llllll2m",
            "m43334332m",
            "m4r3333r2m",
            "m32222221m",
            "mmmmmmmmmm",
    };

    /** {@code frame} -1 is the unlit face; 0-3 are the flickering lit frames. */
    static BufferedImage bloomeryFront(int frame) {
        BufferedImage im = fireBricks();
        int x0 = 3, y0 = 4;
        int[] bright = {0, 1, 2, 1};
        Random r = new Random(5000 + frame);
        for (int y = 0; y < BLOOMERY_DOOR.length; y++)
            for (int x = 0; x < BLOOMERY_DOOR[y].length(); x++) {
                char ch = BLOOMERY_DOOR[y].charAt(x);
                int c;
                if (ch == 'm') c = FIRE_BRICK.get(1);
                else if (ch >= '1' && ch <= '5') c = COPPER.get(ch - '0');
                else if (ch == 'r') c = COPPER.get(5);
                else if (ch == 'l') c = frame < 0 ? COPPER.get(4) : (x % 3 == (frame & 1) ? COPPER.spec() : COPPER.get(5));
                else if (ch == 'k' || ch == 'K') {
                    if (frame < 0) c = CHARCOAL.get(ch == 'k' ? 1 : 2);
                    else {
                        int i = (ch == 'k' ? 1 : 2) + bright[frame] + r.nextInt(2);
                        if (x == 2 || x == 7) i--;
                        c = HEAT_BAND[Math.max(0, Math.min(5, i))];
                    }
                } else continue;
                px(im, x0 + x, y0 + y, c);
            }
        // When lit, heat leaks round the bottom of the door.
        if (frame >= 0)
            for (int x = 4; x <= 11; x++)
                px(im, x, 15, HEAT_BAND[(x + frame) % 4 == 0 ? Math.min(2, 1 + bright[frame] / 2) : bright[frame] > 0 ? 1 : 0]);
        return im;
    }

    static BufferedImage bloomeryFrontLit() {
        BufferedImage strip = new BufferedImage(16, 64, BufferedImage.TYPE_INT_ARGB);
        for (int f = 0; f < 4; f++) strip.getGraphics().drawImage(bloomeryFront(f), 0, f * 16, null);
        return strip;
    }

    /** Hammer marks: clusters of small dents, shadowed top-left and lit bottom-right. */
    static void hammerMarks(BufferedImage im, Random r, Ramp a, int dark, int light, int clusters, int x0, int y0, int x1, int y1) {
        int[][] offs = {{0, 0}, {3, 1}, {1, 3}, {4, -1}};
        for (int k = 0; k < clusters; k++) {
            // Spread the clusters over the area: one per band, alternating sides.
            int bw = Math.max(1, (x1 - x0 - 4) / 2), bh = Math.max(1, (y1 - y0 - 4) / clusters);
            int cx = x0 + (k % 2) * bw + r.nextInt(bw), cy = y0 + k * bh + r.nextInt(bh);
            int n = 2 + r.nextInt(2);
            for (int i = 0; i < n; i++) {
                int x = cx + offs[i][0], y = cy + offs[i][1];
                px(im, x + 1, y, a.get(dark));
                px(im, x, y + 1, a.get(dark));
                px(im, x + 1, y + 1, a.get(light));
            }
        }
    }

    /** Wrought iron anvil body: forged iron, hammer-mark clusters, a dark scale edge. */
    static BufferedImage wroughtAnvilBody() {
        double[][] n = V2.grain(5454), e = noise(5455, 4);
        BufferedImage im = img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int step = n[y][x] > 0.78 ? 4 : n[y][x] < 0.2 ? 2 : 3;
                int d = Math.min(Math.min(x, y), Math.min(15 - x, 15 - y));
                if (d == 0) step = e[y][x] > 0.5 ? 1 : 2;
                else if (d == 1 && e[y][x] > 0.6) step = 2;
                px(im, x, y, WROUGHT_IRON.get(step));
            }
        hammerMarks(im, new Random(5454), WROUGHT_IRON, 2, 4, 4, 2, 2, 14, 14);
        return im;
    }

    /** Wrought iron anvil face (visible columns 3-12): worked bright centre, hammer marks, scaled rim. */
    static BufferedImage wroughtAnvilTop() {
        double[][] n = V2.grain(5656), e = noise(5657, 4);
        BufferedImage im = img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int step;
                boolean rim = x <= 3 || x >= 12 || y == 0 || y == 15;
                boolean inner = x == 4 || x == 11 || y == 1 || y == 14;
                if (rim) step = e[y][x] > 0.55 ? 1 : 2;
                else if (inner) step = e[y][x] > 0.5 ? 2 : 3;
                else {
                    double d = Math.abs(x - 7.5) / 4 + Math.abs(y - 7.5) / 14;
                    step = d < 0.6 ? 4 : 3;
                    if (n[y][x] > 0.84 && d < 0.9) step = 5;
                    else if (n[y][x] < 0.2) step--;
                }
                px(im, x, y, WROUGHT_IRON.get(step));
            }
        // Single dents spread along the working face.
        int[][] dents = {{5, 2}, {8, 4}, {6, 7}, {9, 10}, {5, 11}, {8, 13}};
        for (int[] d : dents) {
            px(im, d[0] + 1, d[1], WROUGHT_IRON.get(3));
            px(im, d[0], d[1] + 1, WROUGHT_IRON.get(3));
            px(im, d[0] + 1, d[1] + 1, WROUGHT_IRON.get(5));
        }
        px(im, 6, 6, WROUGHT_IRON.spec());
        return im;
    }

    // Items.

    static final String[] FIRE_CLAY_BALL = {
            "....4554....",
            "..44555443..",
            ".4455444433.",
            ".4454444333.",
            "444444443332",
            "444443333322",
            ".4433333322.",
            ".3333332221.",
            "..22222211..",
    };
    static final String[] GROG_CHIPS = {
            "..5.........",
            ".453....5...",
            "4432...453..",
            "3221..44332.",
            "......32221.",
            ".45.........",
            "4443...45...",
            "33321.4433..",
            ".221.443332.",
            "....3322221.",
    };
    /** A slimmer brick than the clay brick: same top-left lighting, five rows. */
    static final String[] FIRE_BRICK_ITEM = {
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "....4555555554..",
            "...455555555443.",
            "..4444444444332.",
            "..3333333333221.",
            "...222222222111.",
            "................",
            "................",
            "................",
            "................",
            "................",
    };
    static final String[] LIGNITE_LUMP = {
            "...44554....",
            "..4455443...",
            ".445444433..",
            ".4222222332.",
            "44544444332.",
            "433333333321",
            ".4222223221.",
            ".333333221..",
            "..222211....",
    };
    /** Raw bloom: spongy iron, digits wrought iron, a/b slag pockets in the lignite ramp. */
    static final String[] RAW_BLOOM = {
            "...4554.4...",
            "..44a45s4a3.",
            ".4544bb4433.",
            ".44a4443a332",
            "4544443b3332",
            "44ab43333b32",
            "4443a333a321",
            ".33333ab3321",
            ".3a3b333221.",
            "..2223322a..",
            "...2211.....",
    };
    /** Bloomery slag: glassy dark lump (slag ramp) with a rust streak (c/d hematite). */
    static final String[] BLOOMERY_SLAG = {
            "....4553....",
            "..44554332..",
            ".4454433d32.",
            ".443433dc221",
            "44333dc32221",
            "4335cd232211",
            ".332c3222211",
            "..22222211..",
            "...1111.....",
    };
    static final String[] FLUX_HEAP = {
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            ".......45.......",
            ".....44543......",
            "....45444353....",
            "...4544533432...",
            "..445434353322..",
            "..44533333522...",
            "...333322221....",
            ".....2222.......",
            "................",
            "................",
    };
    static final String[] SMITHING_PATTERN = {
            "................",
            "................",
            "...34444444443..",
            "..2455555555542.",
            "..1322222222231.",
            "....45555554....",
            "....54444443....",
            "....54444443....",
            "....54444443....",
            "....54444443....",
            "....44444442....",
            "...34444444443..",
            "..2455555555542.",
            "..1322222222231.",
            "................",
            "................",
    };
    static final String[] SMITHING_PATTERN_RECORDED = {
            "................",
            "................",
            "...34444444443..",
            "..2455555555542.",
            "..1322222222231.",
            "....45555554....",
            "....5ccc4cc3....",
            "....54444443....",
            "....5cc4ccc3....",
            "....54444443....",
            "....4bbbb442....",
            "...34444444443..",
            "..2455555555542.",
            "..1322222222231.",
            "................",
            "................",
    };

    // New shared shapes (spec 20.4).
    static final String[] ROD = {
            "..........45",
            ".........453",
            "........4s2.",
            ".......442..",
            "......452...",
            ".....442....",
            "....452.....",
            "...442......",
            "..432.......",
            ".332........",
            "321.........",
            "21..........",
    };
    static final String[] DOUBLE_INGOT = {
            "...45555s5..",
            "..4444444445",
            ".44444444443",
            "333333333332",
            "212221222121",
            "344444444432",
            "333333333332",
            "233333333321",
            ".2222222221.",
    };

    static final String[] DOUBLE_INGOT_V2 = {
            "......5555s52...",
            ".....444444421..",
            "....4444444421..",
            "...333333333321.",
            "...121212121211.",
            "...344444444321.",
            "...333333333321.",
            "...22222222221..",
    };

    static void tier3() throws IOException {
        // Blocks (spec 20.2).
        save("block/fire_clay", fireClayBlock());
        save("block/fire_bricks", fireBricks());
        save("block/lignite_seam", ligniteSeam());
        Mineral bogLimonite = new Mineral("bog_limonite", ORE_LIMONITE, false);
        for (String grade : List.of("poor", "normal", "rich"))
            save("block/bog_iron_" + grade, composite(mud(), oreOverlay(bogLimonite, grade)));
        save("block/placer_gravel", placerGravel());
        save("block/placer_sand", placerSand());
        for (String grade : List.of("poor", "normal", "rich")) {
            save("block/ore/hematite_" + grade, oreOverlay(T3_MINERALS.get(0), grade));
            save("block/ore/magnetite_" + grade, oreOverlay(T3_MINERALS.get(1), grade));
            save("block/ore/native_gold_" + grade, oreOverlay(T3_MINERALS.get(2), grade));
        }
        save("block/bloomery_front", bloomeryFront(-1));
        saveRaw("block/bloomery_front_lit", bloomeryFrontLit());
        Files.writeString(OUT.resolve("block/bloomery_front_lit.png.mcmeta"), "{\"animation\":{\"frametime\":3}}\n");
        save("block/wrought_iron_anvil", wroughtAnvilBody());
        save("block/wrought_iron_anvil_top", wroughtAnvilTop());

        // Ore items and small ores for the four new minerals.
        for (Mineral m : T3_MINERALS) {
            String n = m.name();
            save("item/poor_" + n, map(m.ramp(), ORE_SMALL));
            save("item/" + n, map(m.ramp(), ORE_NORMAL));
            save("item/rich_" + n, map(m.ramp(), ORE_RICH));
            save("item/crushed_poor_" + n, map(m.ramp(), CRUSHED_SMALL));
            save("item/crushed_" + n, map(m.ramp(), CRUSHED_NORMAL));
            save("item/crushed_rich_" + n, map(m.ramp(), CRUSHED_RICH));
            save("item/small_" + n, map(m.ramp(), NUGGET));
            save("block/small_" + n, pebbles(m));
        }

        // Materials (spec 20.4).
        save("item/fire_clay_ball", map(FIRE_CLAY, FIRE_CLAY_BALL));
        save("item/grog", map(CERAMIC, GROG_CHIPS));
        save("item/unfired_fire_brick", art(FIRE_CLAY, FIRE_BRICK_ITEM));
        save("item/fire_brick", art(FIRE_BRICK, FIRE_BRICK_ITEM));
        save("item/lignite", map(LIGNITE, LIGNITE_LUMP));
        save("item/raw_bloom", map(WROUGHT_IRON, LIGNITE, RAW_BLOOM));
        save("item/bloomery_slag", map(SLAG, HEMATITE, BLOOMERY_SLAG));
        save("item/flux", art(MARBLE, FLUX_HEAP));
        save("item/smithing_pattern", art(SMITHING_PATTERN, PAPER, CHARCOAL, FIBRE));
        save("item/smithing_pattern_recorded", art(SMITHING_PATTERN_RECORDED, PAPER, CHARCOAL, FIBRE));

        // Wrought iron and gold forms (both keep vanilla ingots and nuggets).
        java.util.Map<String, String[]> heads = new java.util.LinkedHashMap<>();
        heads.put("pickaxe_head", PICKAXE_HEAD);
        heads.put("axe_head", AXE_HEAD);
        heads.put("shovel_head", SHOVEL_HEAD);
        heads.put("hoe_head", HOE_HEAD);
        heads.put("knife_blade", KNIFE_BLADE);
        heads.put("hammer_head", HAMMER_HEAD);
        heads.put("saw_blade", SAW_BLADE);
        heads.put("sword_blade", SWORD_BLADE);
        heads.put("prospectors_pick_head", PROSPECTOR_HEAD);
        save("item/wrought_iron_plate", map(WROUGHT_IRON, PLATE));
        save("item/wrought_iron_rod", map(WROUGHT_IRON, ROD));
        save("item/wrought_iron_double_ingot", map(WROUGHT_IRON, DOUBLE_INGOT_V2));
        for (var head : heads.entrySet())
            save("item/wrought_iron_" + head.getKey(), head.getKey().equals("pickaxe_head") ? pickaxeHeadV2(WROUGHT_IRON) : map(WROUGHT_IRON, head.getValue()));
        for (String kind : List.of("knife", "hammer", "saw", "prospectors_pick"))
            save("item/wrought_iron_" + kind, tool(WROUGHT_IRON, V2.WOOD_V2, null, kind));
        save("item/gold_plate", map(GOLD, PLATE));
        for (String head : List.of("pickaxe_head", "axe_head", "shovel_head", "hoe_head", "sword_blade"))
            save("item/gold_" + head, head.equals("pickaxe_head") ? pickaxeHeadV2(GOLD) : map(GOLD, heads.get(head)));

        saveRaw("gui/bloomery", bloomeryGui());
    }

    /** Bloomery status screen, 176x90: temperature gauge on the left, progress bar along the bottom, no slots. */
    static BufferedImage bloomeryGui() {
        BufferedImage im = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        panel(im, 176, 90);
        well(im, 10, 18, 12, 62, 0x2a2a2a);
        well(im, 34, 70, 132, 10, 0x2a2a2a);
        return im;
    }

    // ---------------------------------------------------------------- tier 3: kinetics (spec 21)

    // Worn, hand-darkened wood for crank handles; weathered wet wood for water wheels (built per SG 3).
    static final Ramp DARK_WOOD = ramp(0, 0x281a16, 0x3c281e, 0x553b29, 0x705136, 0x8c6a46);
    static final Ramp WET_WOOD = ramp(0, 0x221d20, 0x332b28, 0x4a3e33, 0x625340, 0x7c6a50);

    static int clampStep(int s) { return Math.max(1, Math.min(5, s)); }

    /**
     * Fills a rectangle with wood grain: every line (a column if {@code vertical}, else a row) wanders
     * one step above or below {@code base} in runs of 2 to 5 px, and every third line or so is a dark latewood streak.
     */
    static void grain(BufferedImage im, Ramp a, Random r, int x0, int y0, int w, int h, int base, boolean vertical) {
        int lines = vertical ? w : h, len = vertical ? h : w;
        for (int l = 0; l < lines; l++) {
            int bias = r.nextInt(3) == 0 ? -1 : 0;
            int i = 0;
            while (i < len) {
                int run = 2 + r.nextInt(4);
                int roll = r.nextInt(6);
                int step = clampStep(base + bias + (roll == 0 ? 1 : roll == 1 ? -1 : 0));
                for (int k = 0; k < run && i < len; k++, i++) {
                    if (vertical) px(im, x0 + l, y0 + i, a.get(step));
                    else px(im, x0 + i, y0 + l, a.get(step));
                }
            }
        }
    }

    /** A treenail: a 2x2 dowel end, lit top-left, sitting in a dark bore. */
    static void peg(BufferedImage im, Ramp a, int x, int y) {
        px(im, x, y, a.get(5));
        px(im, x + 1, y, a.get(4));
        px(im, x, y + 1, a.get(4));
        px(im, x + 1, y + 1, a.get(2));
    }

    /** End grain: growth rings round the centre with a little wobble, a dark heart and one radial check. */
    static BufferedImage endGrain(Ramp a, long seed, double cx, double cy, double spacing) {
        double[][] n = fractal(seed);
        BufferedImage im = img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double d = Math.hypot(x - cx, y - cy) + (n[y][x] - 0.5) * 0.9;
                int ring = (int) Math.floor(d / spacing);
                double f = d / spacing - ring;
                int step = f < 0.3 ? 2 : (ring % 2 == 0 ? 4 : 3);
                if (d < 1.0) step = 2;
                px(im, x, y, a.get(step));
            }
        // A drying check running out from the heart towards the bottom-right.
        for (int i = 1; i < 5; i++) px(im, (int) Math.round(cx + 0.5 + i * 0.8), (int) Math.round(cy + 0.5 + i * 0.45), a.get(i < 3 ? 1 : 2));
        return im;
    }

    /** Hewn axle side: lengthwise grain, two fibre twine lashings wound diagonally round the shaft. */
    static BufferedImage axleSide() {
        BufferedImage im = img();
        grain(im, WOOD, new Random(8101), 0, 0, 16, 16, 3, true);
        for (int b0 : new int[] {2, 10}) {
            for (int x = 0; x < 16; x++) {
                for (int j = 0; j < 3; j++) {
                    int k = Math.floorMod(x - j, 3);
                    px(im, x, b0 + j, FIBRE.get(clampStep((k == 0 ? 3 : k == 1 ? 2 : 1) + (j == 0 && k == 0 ? 1 : 0))));
                }
                px(im, x, b0 + 3, WOOD.get(1)); // the rope's shadow on the shaft
            }
        }
        // A tight knot in the grain.
        px(im, 7, 6, WOOD.get(1)); px(im, 8, 6, WOOD.get(2)); px(im, 7, 7, WOOD.get(2)); px(im, 6, 6, WOOD.get(4));
        return im;
    }

    static BufferedImage axleEnd() {
        BufferedImage im = endGrain(WOOD, 8102, 7.5, 7.5, 1.6);
        return im;
    }

    /** Gearbox face: plank panel in a pegged frame, a round opening with an eight-tooth cog turning behind it. */
    static BufferedImage gearboxFace() {
        BufferedImage im = img();
        Random r = new Random(8201);
        // Panel boards, vertical, three wide.
        grain(im, WOOD, r, 2, 2, 12, 12, 2, true);
        for (int y = 2; y < 14; y++) {
            px(im, 5, y, WOOD.get(1));
            px(im, 10, y, WOOD.get(1));
            px(im, 6, y, WOOD.get(3));
            px(im, 11, y, WOOD.get(3));
        }
        // Frame: horizontal rails over vertical stiles, lit top-left.
        grain(im, WOOD, r, 0, 0, 16, 2, 3, false);
        grain(im, WOOD, r, 0, 14, 16, 2, 3, false);
        grain(im, WOOD, r, 0, 2, 2, 12, 3, true);
        grain(im, WOOD, r, 14, 2, 2, 12, 3, true);
        for (int i = 0; i < 16; i++) {
            px(im, i, 0, WOOD.get(4));
            px(im, i, 15, WOOD.get(2));
            if (i > 1 && i < 14) {
                px(im, 0, i, WOOD.get(4));
                px(im, 15, i, WOOD.get(2));
                px(im, i, 2, WOOD.get(2)); // frame shadow on the panel
                px(im, 2, i, WOOD.get(2));
            }
        }
        // Round opening with the cog behind it; deep shadow in the gaps, the far (bottom-right) wall lit.
        boolean[][] hole = new boolean[16][16], cog = gearMask();
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) hole[y][x] = Math.hypot(x - 7.5, y - 7.5) <= 6.1;
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                if (!hole[y][x] || cog[y][x]) continue;
                boolean farWall = !hole[y + 1][x] || !hole[y][x + 1];
                px(im, x, y, farWall ? WOOD.get(2) : DARK_WOOD.get(1));
            }
        drawGear(im, 4);
        // Lit rim of the panel round the opening's top-left (light catches the cut edge).
        for (int y = 2; y < 14; y++)
            for (int x = 2; x < 14; x++)
                if (!hole[y][x] && ((y + 1 < 16 && hole[y + 1][x]) || (x + 1 < 16 && hole[y][x + 1]))) px(im, x, y, WOOD.get(4));
        // Corner pegs.
        peg(im, WOOD, 0, 0); peg(im, WOOD, 14, 0); peg(im, WOOD, 0, 14); peg(im, WOOD, 14, 14);
        return im;
    }

    /** Hand-worn dark wood: grain with a few polished streaks and scuffs. */
    static BufferedImage crankWood() {
        BufferedImage im = img();
        Random r = new Random(8301);
        grain(im, DARK_WOOD, r, 0, 0, 16, 16, 3, true);
        int[][] polish = {{2, 3}, {9, 1}, {5, 9}, {12, 7}, {14, 12}, {7, 13}};
        for (int[] p : polish) { px(im, p[0], p[1], DARK_WOOD.get(5)); px(im, p[0], p[1] + 1, DARK_WOOD.get(4)); }
        int[][] scuff = {{4, 6}, {11, 3}, {10, 11}, {1, 13}};
        for (int[] p : scuff) { px(im, p[0], p[1], DARK_WOOD.get(1)); px(im, p[0] + 1, p[1], DARK_WOOD.get(2)); }
        return im;
    }

    /** Water wheel boards: weathered wet planks, lit upper edges, damp dark lower edges with a little algae, pegged butt joints. */
    static BufferedImage wheelPlanks() {
        BufferedImage im = img();
        Random r = new Random(8401);
        grain(im, WET_WOOD, r, 0, 0, 16, 16, 3, false);
        int[] joints = {11, 4, 13, 6};
        for (int p = 0; p < 4; p++) {
            int y0 = p * 4;
            for (int x = 0; x < 16; x++) {
                px(im, x, y0, WET_WOOD.get(4));
                px(im, x, y0 + 3, WET_WOOD.get(r.nextInt(5) == 0 ? 2 : 1));
            }
            int j = joints[p];
            px(im, j, y0 + 1, WET_WOOD.get(1)); px(im, j, y0 + 2, WET_WOOD.get(1));
            px(im, j + 1, y0 + 1, WET_WOOD.get(4)); px(im, j + 1, y0 + 2, WET_WOOD.get(3));
            // Treenails either side of the joint.
            px(im, Math.floorMod(j - 2, 16), y0 + 1, WET_WOOD.get(5)); px(im, Math.floorMod(j - 2, 16), y0 + 2, WET_WOOD.get(2));
            px(im, Math.floorMod(j + 3, 16), y0 + 1, WET_WOOD.get(5)); px(im, Math.floorMod(j + 3, 16), y0 + 2, WET_WOOD.get(2));
        }
        // Algae where the water sits along the seams, and a few wet glints.
        int[][] algae = {{2, 3}, {3, 3}, {7, 7}, {8, 7}, {14, 11}, {15, 11}, {5, 15}, {6, 15}, {9, 3}};
        for (int[] a : algae) px(im, a[0], a[1], FIBRE.get(1));
        int[][] glint = {{4, 1}, {13, 5}, {1, 9}, {9, 13}};
        for (int[] g : glint) { px(im, g[0], g[1], WET_WOOD.get(5)); px(im, g[0] + 1, g[1], WET_WOOD.get(5)); }
        return im;
    }

    /** Water wheel hub: wet end grain with a square axle socket and four wedges. */
    static BufferedImage wheelHub() {
        BufferedImage im = endGrain(WET_WOOD, 8402, 7.5, 7.5, 1.5);
        for (int y = 6; y <= 9; y++) for (int x = 6; x <= 9; x++) px(im, x, y, (x == 9 || y == 9) ? WET_WOOD.get(2) : WOOD.get(3));
        for (int y = 7; y <= 8; y++) for (int x = 7; x <= 8; x++) px(im, x, y, WOOD.get(x == 7 && y == 7 ? 4 : 2));
        int[][] wedges = {{7, 5}, {5, 7}, {10, 8}, {8, 10}};
        for (int[] w : wedges) px(im, w[0], w[1], WET_WOOD.get(1));
        return im;
    }

    /** Millstone housing side: post grain above, a pegged wooden curb band, two courses of dressed limestone below. */
    static BufferedImage millstoneSide() {
        BufferedImage im = img();
        Random r = new Random(8501);
        grain(im, WOOD, r, 0, 0, 16, 7, 3, true);
        grain(im, WOOD, r, 0, 7, 16, 2, 3, false);
        for (int x = 0; x < 16; x++) { px(im, x, 6, WOOD.get(2)); px(im, x, 7, WOOD.get(4)); }
        peg(im, WOOD, 2, 7); peg(im, WOOD, 12, 7);
        double[][] n = fractal(8502);
        int[][] joints = {{5, 13}, {1, 9}};
        for (int y = 9; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int course = y < 12 ? 0 : 1, top = course == 0 ? 9 : 12;
                int step = n[y][x] > 0.6 ? 4 : 3;
                if (y == top) step = 4;
                if (y == 15) step = 2;
                boolean joint = false, afterJoint = false;
                for (int j : joints[course]) { if (x == j) joint = true; if (x == j + 1) afterJoint = true; }
                if (afterJoint) step = 4;
                if (joint) step = 1;
                if (y == 12 && !joint) step = 1;
                if (y == 13 && !joint) step = Math.max(step, 4);
                px(im, x, y, LIMESTONE.get(step));
            }
        // Shadow under the curb, and a few diagonal chisel strokes.
        for (int x = 0; x < 16; x++) if (rgb(im, x, 9) != LIMESTONE.get(1)) px(im, x, 9, LIMESTONE.get(2));
        int[][] tool = {{3, 10}, {8, 11}, {10, 10}, {4, 14}, {11, 14}, {14, 13}};
        for (int[] t : tool) { px(im, t[0], t[1], LIMESTONE.get(2)); px(im, t[0] + 1, t[1] - 1, LIMESTONE.get(2)); }
        return im;
    }

    /** Millstone housing top: a mitred wooden curb round a limestone bed, its meal channel and the granite bedstone. */
    static BufferedImage millstoneTop() {
        BufferedImage im = img();
        double[][] n = fractal(8503);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double dx = x - 7.5, dy = y - 7.5, d = Math.hypot(dx, dy);
                if (d <= 4.8) {
                    // The granite bedstone the runner turns on, furrowed like the runner.
                    int step = n[y][x] > 0.6 ? 4 : 3;
                    if (d > 1.5 && (Math.abs(dx) < 0.6 || Math.abs(dy) < 0.6 || Math.abs(Math.abs(dx) - Math.abs(dy)) < 0.6)) step = 2;
                    if (d < 1.5) step = 1;
                    px(im, x, y, GRANITE.get(step));
                    continue;
                }
                int step = n[y][x] > 0.58 ? 4 : n[y][x] < 0.4 ? 2 : 3;
                if (d < 6.0) step = (dx + dy > 0) ? 4 : 1; // meal channel: shadowed upper-left wall, lit far wall
                px(im, x, y, LIMESTONE.get(step));
            }
        Random r = new Random(8504);
        grain(im, WOOD, r, 0, 0, 16, 2, 3, false);
        grain(im, WOOD, r, 0, 14, 16, 2, 3, false);
        grain(im, WOOD, r, 0, 2, 2, 12, 3, true);
        grain(im, WOOD, r, 14, 2, 2, 12, 3, true);
        for (int i = 0; i < 16; i++) {
            px(im, i, 0, WOOD.get(4));
            px(im, 0, i, WOOD.get(4));
            px(im, i, 15, WOOD.get(2));
            px(im, 15, i, WOOD.get(2));
        }
        for (int i = 2; i < 14; i++) { px(im, i, 2, LIMESTONE.get(2)); px(im, 2, i, LIMESTONE.get(2)); }
        // Mitre joints.
        px(im, 1, 1, WOOD.get(1)); px(im, 14, 1, WOOD.get(1)); px(im, 1, 14, WOOD.get(1)); px(im, 14, 14, WOOD.get(1));
        peg(im, WOOD, 7, 0); peg(im, WOOD, 0, 7); peg(im, WOOD, 7, 14); peg(im, WOOD, 14, 7);
        return im;
    }

    /**
     * Runner stone: dressed granite. Eight straight furrows run out from the eye, each with a lit lip,
     * lands speckled with crystals, a plain skirt round the edge so the sides read as tooled stone.
     */
    static BufferedImage millstoneRunner() {
        double[][] n = fractal(8601);
        BufferedImage im = img();
        Random r = new Random(8602);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double px0 = x - 7.5, py0 = y - 7.5, d = Math.hypot(px0, py0);
                int step = n[y][x] > 0.62 ? 4 : n[y][x] < 0.36 ? 2 : 3;
                boolean skirt = x < 2 || y < 2 || x > 13 || y > 13;
                if (!skirt && d > 2.2) {
                    for (int k = 0; k < 8; k++) {
                        double a = k * Math.PI / 4, dx = Math.cos(a), dy = Math.sin(a);
                        double along = px0 * dx + py0 * dy, across = px0 * -dy + py0 * dx;
                        if (along > 0.5 && Math.abs(across) < 0.5) step = 1;
                        else if (along > 0.5 && across > 0.5 && across < 1.2 && step != 1) step = 4;
                    }
                }
                if (skirt) step = n[y][x] > 0.55 ? 4 : 3;
                px(im, x, y, GRANITE.get(step));
            }
        // Skirt tooling: short vertical pick marks.
        for (int x = 1; x < 16; x += 3) { px(im, x, 0, GRANITE.get(2)); px(im, x, 15, GRANITE.get(2)); px(im, 0, x, GRANITE.get(2)); px(im, 15, x, GRANITE.get(2)); }
        // The eye.
        for (int y = 6; y <= 9; y++) for (int x = 6; x <= 9; x++) px(im, x, y, GRANITE.get((x == 9 || y == 9) ? 4 : 2));
        for (int y = 7; y <= 8; y++) for (int x = 7; x <= 8; x++) px(im, x, y, GRANITE.get(1));
        for (int k = 0; k < 6; k++) speck(im, r, 2 + r.nextInt(12), 2 + r.nextInt(12), GRANITE.get(5), GRANITE.get(2), 2);
        return im;
    }

    /** Leather pleats seen from the side: ridge lit from above, face, dark crease, with some sheen and wrinkles. */
    static void pleats(BufferedImage im, int y0, int y1, long seed) {
        Random r = new Random(seed);
        for (int y = y0; y < y1; y++)
            for (int x = 0; x < 16; x++) {
                int k = Math.floorMod(y - y0 - 1, 3);
                // The bag bulges: folds are fullest mid-face and tuck in towards the ends.
                boolean end = x < 2 || x > 13, mid = x > 3 && x < 12;
                int step = k == 0 ? (mid ? 4 : end ? 2 : 3) : k == 1 ? (end ? 2 : 3) : 1;
                if (k == 2 && (x == 0 || x == 15)) step = 1;
                if (y == y0) step = 1; // shadow under the board
                px(im, x, y, LEATHER.get(step));
            }
        for (int i = 0; i < 5; i++) {
            int x = 1 + r.nextInt(13), y = y0 + 1 + 3 * r.nextInt((y1 - y0 - 1) / 3);
            px(im, x, y, LEATHER.get(5)); px(im, x + 1, y, LEATHER.get(5));
        }
        for (int i = 0; i < 4; i++) {
            int x = 1 + r.nextInt(14), y = y0 + 2 + 3 * r.nextInt((y1 - y0 - 1) / 3);
            if (y < y1) px(im, x, y, LEATHER.get(2));
        }
    }

    static void boardEdge(BufferedImage im, Random r, int y) {
        grain(im, WOOD, r, 0, y, 16, 2, 3, false);
        for (int x = 0; x < 16; x++) { px(im, x, y, WOOD.get(4)); }
        px(im, 0, y, WOOD.get(3)); px(im, 0, y + 1, WOOD.get(2)); px(im, 15, y, WOOD.get(3)); px(im, 15, y + 1, WOOD.get(2));
    }

    static BufferedImage bellowsSide() {
        BufferedImage im = img();
        Random r = new Random(8701);
        pleats(im, 2, 14, 8702);
        boardEdge(im, r, 0);
        boardEdge(im, r, 14);
        // Brass-free, iron-free: the bag is nailed with wooden pegs along the boards.
        for (int x = 2; x < 16; x += 4) { px(im, x, 1, WOOD.get(5)); px(im, x, 14, WOOD.get(5)); }
        return im;
    }

    /** Front: the nose block between the boards, its nozzle bore ringed with an iron band. */
    static BufferedImage bellowsFront() {
        BufferedImage im = img();
        Random r = new Random(8711);
        grain(im, WOOD, r, 0, 2, 16, 12, 2, true);
        for (int x = 0; x < 16; x++) px(im, x, 2, WOOD.get(1));
        boardEdge(im, r, 0);
        boardEdge(im, r, 14);
        for (int y = 5; y <= 10; y++)
            for (int x = 5; x <= 10; x++) {
                boolean ring = x == 5 || x == 10 || y == 5 || y == 10;
                if (ring) px(im, x, y, WROUGHT_IRON.get(x == 10 || y == 10 ? 2 : (x == 5 && y == 5) ? 5 : 4));
                else px(im, x, y, WOOD.get(x == 6 || y == 6 ? 4 : 3));
            }
        for (int y = 7; y <= 8; y++) for (int x = 7; x <= 8; x++) px(im, x, y, CHARCOAL.get(1));
        px(im, 7, 7, CHARCOAL.get(1)); px(im, 8, 8, CHARCOAL.get(3));
        // Iron rivets on the band, and the band's straps running up and down to the boards.
        for (int y = 3; y <= 12; y++) if (y < 5 || y > 10) { px(im, 7, y, WROUGHT_IRON.get(3)); px(im, 8, y, WROUGHT_IRON.get(2)); }
        px(im, 7, 3, WROUGHT_IRON.get(4)); px(im, 7, 12, WROUGHT_IRON.get(4));
        return im;
    }

    /** Back: pleats between the boards, with a worn wooden pull handle hanging from the top board. */
    static BufferedImage bellowsBack() {
        BufferedImage im = img();
        Random r = new Random(8721);
        pleats(im, 2, 14, 8722);
        boardEdge(im, r, 0);
        boardEdge(im, r, 14);
        for (int y = 2; y <= 4; y++) { px(im, 3, y, DARK_WOOD.get(4)); px(im, 4, y, DARK_WOOD.get(2)); px(im, 11, y, DARK_WOOD.get(4)); px(im, 12, y, DARK_WOOD.get(2)); }
        for (int x = 2; x <= 13; x++) { px(im, x, 5, DARK_WOOD.get(x < 4 ? 5 : 4)); px(im, x, 6, DARK_WOOD.get(2)); }
        px(im, 2, 6, DARK_WOOD.get(3));
        for (int x = 2; x <= 13; x++) px(im, x, 7, LEATHER.get(1));
        return im;
    }

    /** Top board: three planks running front to back, pegged to cross battens underneath. */
    static BufferedImage bellowsTop() {
        BufferedImage im = img();
        Random r = new Random(8731);
        grain(im, WOOD, r, 0, 0, 16, 16, 3, true);
        for (int y = 0; y < 16; y++) {
            px(im, 5, y, WOOD.get(1)); px(im, 6, y, WOOD.get(4));
            px(im, 10, y, WOOD.get(1)); px(im, 11, y, WOOD.get(4));
            px(im, 0, y, WOOD.get(4)); px(im, 15, y, WOOD.get(2));
        }
        for (int bx : new int[] {2, 7, 12}) { peg(im, WOOD, bx, 2); peg(im, WOOD, bx, 12); }
        return im;
    }

    // Items.

    /** Shades a mask lit top-left: pixels whose top or left neighbour is outside the part are one step lighter, bottom/right one darker. */
    static void shadeMask(BufferedImage im, boolean[][] m, Ramp a, int base) {
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                if (!m[y][x]) continue;
                boolean lit = y == 0 || x == 0 || !m[y - 1][x] || !m[y][x - 1];
                boolean dark = y == 15 || x == 15 || !m[y + 1][x] || !m[y][x + 1];
                px(im, x, y, a.get(clampStep(base + (lit ? 1 : 0) - (dark && !lit ? 1 : 0))));
            }
    }

    /** Water wheel front-on: octagonal rim on a cross of spokes, eight paddle boards, open between the spokes. */
    static BufferedImage waterWheelItem() {
        BufferedImage im = img();
        boolean[][] rim = new boolean[16][16], paddle = new boolean[16][16], spoke = new boolean[16][16], open = new boolean[16][16];
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double dx = Math.abs(x - 7.5), dy = Math.abs(y - 7.5);
                boolean outer = Math.max(dx, dy) <= 4.5 && dx + dy <= 6.5;
                boolean inner = Math.max(dx, dy) <= 3.5 && dx + dy <= 5.5;
                if (outer && !inner) rim[y][x] = true;
                else if (inner && (dx < 1 || dy < 1)) spoke[y][x] = true;
                else if (inner) open[y][x] = true;
            }
        int[][] paddles = {{7, 2}, {8, 2}, {7, 13}, {8, 13}, {2, 7}, {2, 8}, {13, 7}, {13, 8},
                {4, 4}, {3, 4}, {4, 3}, {11, 4}, {12, 4}, {11, 3}, {4, 11}, {3, 11}, {4, 12}, {11, 11}, {12, 11}, {11, 12}};
        for (int[] p : paddles) paddle[p[1]][p[0]] = true;
        shadeMask(im, spoke, WET_WOOD, 3);
        shadeMask(im, rim, WET_WOOD, 4);
        shadeMask(im, paddle, WOOD, 3);
        // Hub with its square axle.
        for (int y = 6; y <= 9; y++) for (int x = 6; x <= 9; x++) px(im, x, y, WOOD.get(x == 6 || y == 6 ? 5 : x == 9 || y == 9 ? 2 : 4));
        px(im, 7, 7, DARK_WOOD.get(1)); px(im, 8, 7, DARK_WOOD.get(2)); px(im, 7, 8, DARK_WOOD.get(2)); px(im, 8, 8, DARK_WOOD.get(1));
        BufferedImage out = outline(im);
        // Keep the gaps between the spokes see-through rather than filled with outline.
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) if (open[y][x]) clear(out, x, y);
        return out;
    }

    static BufferedImage handCrankItem() {
        BufferedImage im = img();
        boolean[][] arm = new boolean[16][16], grip = new boolean[16][16];
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                // Arm from the hub (bottom-left) up to the grip (top-right).
                double t = ((x - 4.5) + -(y - 10.5)) / 2.0, across = ((x - 4.5) + (y - 10.5)) / Math.sqrt(2);
                if (t >= 0 && t <= 6.5 && Math.abs(across) <= 0.8) arm[y][x] = true;
                if (x >= 10 && x <= 12 && y >= 2 && y <= 6 && !(y == 2 && x == 12)) grip[y][x] = true;
            }
        shadeMask(im, arm, DARK_WOOD, 3);
        shadeMask(im, grip, DARK_WOOD, 4);
        // Polished band where the hand turns it.
        px(im, 10, 4, DARK_WOOD.get(5)); px(im, 10, 5, DARK_WOOD.get(5));
        // Square shaft end at the hub, end grain with a dark heart.
        for (int y = 9; y <= 13; y++)
            for (int x = 2; x <= 6; x++) {
                int step = (x == 2 || y == 9) ? 5 : (x == 6 || y == 13) ? 2 : 4;
                px(im, x, y, WOOD.get(step));
            }
        px(im, 4, 11, WOOD.get(2)); px(im, 3, 11, WOOD.get(3)); px(im, 4, 10, WOOD.get(3));
        return outline(im);
    }

    /** An eight-tooth cog, 12 px across: a round body with square teeth on the axes and diagonals. */
    static boolean[][] gearMask() {
        boolean[][] m = new boolean[16][16];
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) if (Math.hypot(x - 7.5, y - 7.5) <= 4.5) m[y][x] = true;
        int[][] teeth = {{7, 2}, {8, 2}, {7, 3}, {8, 3}, {7, 12}, {8, 12}, {7, 13}, {8, 13}, {2, 7}, {2, 8}, {3, 7}, {3, 8}, {12, 7}, {12, 8}, {13, 7}, {13, 8},
                {4, 3}, {3, 4}, {4, 4}, {11, 3}, {12, 4}, {11, 4}, {3, 11}, {4, 12}, {4, 11}, {12, 11}, {11, 12}, {11, 11}};
        for (int[] t : teeth) m[t[1]][t[0]] = true;
        return m;
    }

    /** Shades the cog lit top-left, then adds the hub boss, square axle hole and four treenails. */
    static void drawGear(BufferedImage im, int base) {
        shadeMask(im, gearMask(), WOOD, base);
        for (int y = 5; y <= 10; y++)
            for (int x = 5; x <= 10; x++) {
                double d = Math.hypot(x - 7.5, y - 7.5);
                if (d > 2.0 && d < 3.0) px(im, x, y, WOOD.get(x + y < 15 ? base - 1 : base + 1));
            }
        px(im, 7, 7, DARK_WOOD.get(1)); px(im, 8, 7, DARK_WOOD.get(1)); px(im, 7, 8, DARK_WOOD.get(1)); px(im, 8, 8, DARK_WOOD.get(2));
        int[][] pegs = {{5, 5}, {10, 5}, {5, 10}, {10, 10}};
        for (int[] p : pegs) px(im, p[0], p[1], WOOD.get(5));
    }

    static BufferedImage woodenGearItem() {
        BufferedImage im = img();
        drawGear(im, 3);
        return outline(im);
    }

    /** Millstone (spec 21.4): input, vanilla-style progress arrow, large output slot; strip y 60-72 left for the status line. */
    static BufferedImage millstoneGui() {
        BufferedImage im = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        panel(im, 176, 166);
        slot(im, 56, 35);
        well(im, 111, 30, 26, 26, SLOT_FILL);
        arrow(im, 79, 34);
        // Filled arrow sprite at u=176, v=14, drawn with the same shape as the empty arrow.
        for (int j = 0; j < 15; j++) {
            int d = Math.abs(j - 7);
            if (d <= 2) for (int i = 0; i < 15; i++) im.setRGB(176 + i, 14 + j, 0xff000000 | GUI_LIGHT);
            for (int i = 15; i < 22 - d; i++) im.setRGB(176 + i, 14 + j, 0xff000000 | GUI_LIGHT);
        }
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++) slot(im, 8 + col * 18, 84 + row * 18);
        for (int col = 0; col < 9; col++) slot(im, 8 + col * 18, 142);
        return im;
    }

    static void kinetics() throws IOException {
        save("block/wooden_axle", axleSide());
        save("block/wooden_axle_end", axleEnd());
        save("block/wooden_gearbox", gearboxFace());
        save("block/hand_crank", crankWood());
        save("block/water_wheel", wheelPlanks());
        save("block/water_wheel_hub", wheelHub());
        save("block/millstone_side", millstoneSide());
        save("block/millstone_top", millstoneTop());
        save("block/millstone_runner", millstoneRunner());
        save("block/bellows_side", bellowsSide());
        save("block/bellows_front", bellowsFront());
        save("block/bellows_back", bellowsBack());
        save("block/bellows_top", bellowsTop());
        save("item/water_wheel", waterWheelItem());
        save("item/hand_crank", handCrankItem());
        save("item/wooden_gear", woodenGearItem());
        saveRaw("gui/millstone", millstoneGui());
    }

    // ---------------------------------------------------------------- tier 3: machines (spec 22)

    // Rough outer bark: grey-brown, cooler in the fissures, warmer on the ridges (built per SG 3).
    static final Ramp BARK = ramp(0, 0x29211f, 0x3b302a, 0x524337, 0x6b5946, 0x857257);

    /** Writes a vertical animation strip and its .mcmeta. */
    static void saveAnimated(String path, BufferedImage strip, int frametime) throws IOException {
        saveRaw(path, strip);
        Files.writeString(OUT.resolve(path + ".png.mcmeta"), "{\"animation\":{\"frametime\":" + frametime + "}}\n");
    }

    /** A dome rivet: lit head, shadow below-right. */
    static void rivet(BufferedImage im, int x, int y) {
        px(im, x, y, WROUGHT_IRON.get(5));
        px(im, x + 1, y, WROUGHT_IRON.get(2));
        px(im, x, y + 1, WROUGHT_IRON.get(1));
    }

    /** A horizontal wrought iron strap, 2 rows, with its shadow on the wood below. */
    static void strapH(BufferedImage im, int x0, int x1, int y, Ramp wood) {
        for (int x = x0; x <= x1; x++) {
            px(im, x, y, WROUGHT_IRON.get((x * 5 + y) % 7 == 0 ? 5 : 4));
            px(im, x, y + 1, WROUGHT_IRON.get((x * 3) % 5 == 0 ? 2 : 3));
            px(im, x, y + 2, wood.get(1));
        }
    }

    /**
     * Saw mill table body (the model shows rows 2-15): plank top edge, apron rail, two legs with iron bands,
     * and recessed boards between the legs. {@code stretcher} adds the low rail of the plain sides.
     */
    static BufferedImage sawMillFrame(long seed, boolean stretcher) {
        BufferedImage im = img();
        Random r = new Random(seed);
        grain(im, WOOD, r, 0, 0, 16, 4, 3, false);
        for (int x = 0; x < 16; x++) { px(im, x, 2, WOOD.get(x % 7 == 3 ? 5 : 4)); px(im, x, 4, WOOD.get(1)); }
        grain(im, WOOD, r, 3, 5, 10, 11, 2, true);
        for (int y = 8; y < 16; y++) { px(im, 6, y, WOOD.get(1)); px(im, 7, y, WOOD.get(3)); px(im, 9, y, WOOD.get(1)); px(im, 10, y, WOOD.get(3)); }
        grain(im, WOOD, r, 3, 5, 10, 3, 3, false);
        for (int x = 3; x < 13; x++) { px(im, x, 5, WOOD.get(4)); px(im, x, 7, WOOD.get(2)); px(im, x, 8, WOOD.get(1)); }
        if (stretcher) {
            grain(im, WOOD, r, 3, 12, 10, 2, 3, false);
            for (int x = 3; x < 13; x++) { px(im, x, 12, WOOD.get(4)); px(im, x, 13, WOOD.get(2)); px(im, x, 14, WOOD.get(1)); }
        }
        for (int lx : new int[] {0, 13}) {
            grain(im, WOOD, r, lx, 5, 3, 11, 3, true);
            for (int y = 5; y < 16; y++) { px(im, lx, y, WOOD.get(4)); px(im, lx + 2, y, WOOD.get(2)); }
            px(im, lx, 5, WOOD.get(3)); px(im, lx + 1, 5, WOOD.get(2)); px(im, lx + 2, 5, WOOD.get(1));
            peg(im, WOOD, lx, 6);
            if (stretcher) peg(im, WOOD, lx, 12);
            // Iron band round the foot of the leg.
            for (int x = lx; x < lx + 3; x++) { px(im, x, 9, WROUGHT_IRON.get(x == lx ? 5 : 4)); px(im, x, 10, WROUGHT_IRON.get(x == lx + 2 ? 2 : 3)); px(im, x, 11, WOOD.get(2)); }
            px(im, lx + 1, 9, WROUGHT_IRON.spec()); px(im, lx + 1, 10, WROUGHT_IRON.get(1));
        }
        return im;
    }

    static BufferedImage sawMillSide() { return sawMillFrame(9101, true); }

    /**
     * Saw mill front: the frame with an opening under the apron. Inside, the blade is seen edge-on dropping from the
     * table slot to its toothed bottom, the arbor crossing it; a heap of sawdust on the sill.
     * {@code frame} -1 is idle; 0-3 are running frames (teeth blurred, sawdust thrown out).
     */
    static BufferedImage sawMillFront(int frame) {
        BufferedImage im = sawMillFrame(9102, false);
        boolean run = frame >= 0;
        // Opening x 4-11, rows 9-14.
        for (int y = 9; y <= 14; y++)
            for (int x = 4; x <= 11; x++) {
                boolean far = x == 11 || y == 14;
                px(im, x, y, far ? WOOD.get(2) : DARK_WOOD.get(1));
            }
        for (int x = 4; x <= 11; x++) px(im, x, 9, CHARCOAL.get(1));
        for (int y = 9; y <= 14; y++) px(im, 4, y, CHARCOAL.get(1));
        for (int x = 3; x <= 12; x++) px(im, x, 15, WOOD.get(x == 3 ? 3 : 4)); // lit sill
        for (int y = 9; y <= 15; y++) { px(im, 3, y, WOOD.get(2)); px(im, 12, y, WOOD.get(4)); }
        // The blade under the table, seen nearly edge-on as a narrow disc hanging from the slot: lit left rim,
        // shaded right rim, a hub on the arbor and set teeth zig-zagging along both edges.
        int phase = run ? frame : 0;
        for (int y = 9; y <= 14; y++)
            for (int x = 5; x <= 10; x++) {
                double dx = (x - 7.5) / 2.4, dy = (y - 10.0) / 4.5;
                double d = dx * dx + dy * dy;
                if (d > 1.0) {
                    // Set teeth just outside the rim, alternately left and right.
                    double d2 = ((x - 7.5) / 3.4) * ((x - 7.5) / 3.4) + dy * dy;
                    boolean left = x < 8;
                    if (d2 <= 1.0 && Math.floorMod(y + phase + (left ? 0 : 1), 2) == 0)
                        px(im, x, y, WROUGHT_IRON.get(run ? (left ? 4 : 3) : (left ? 3 : 2)));
                    continue;
                }
                int step = x <= 6 ? 4 : x >= 9 ? 2 : 3;
                if (run) step = Math.min(5, step + (Math.floorMod(x + y + frame, 3) == 0 ? 1 : 0));
                px(im, x, y, WROUGHT_IRON.get(step));
            }
        px(im, 7, 10, WROUGHT_IRON.get(5)); px(im, 8, 10, WROUGHT_IRON.get(3));
        px(im, 7, 11, WROUGHT_IRON.get(2)); px(im, 8, 11, WROUGHT_IRON.get(1));
        if (!run) px(im, 6, 12, WROUGHT_IRON.spec());
        // Sawdust heap on the sill.
        int[][] heap = {{9, 14}, {10, 14}, {11, 14}, {10, 13}, {4, 14}, {5, 14}};
        for (int[] h : heap) px(im, h[0], h[1], h[1] == 13 || h[0] == 4 ? STRAW.get(4) : STRAW.get(3));
        if (run) {
            // Sawdust thrown forward and down out of the opening, a different spray each frame.
            int[][][] spray = {
                    {{10, 10}, {11, 12}, {9, 15}, {13, 13}, {2, 14}},
                    {{11, 11}, {10, 9}, {12, 15}, {13, 11}, {5, 15}},
                    {{10, 12}, {11, 10}, {13, 14}, {2, 12}, {8, 15}},
                    {{11, 13}, {9, 10}, {14, 15}, {12, 12}, {1, 15}},
            };
            int k = 0;
            for (int[] s : spray[frame]) px(im, s[0], s[1], STRAW.get(k++ % 2 == 0 ? 5 : 4));
        }
        return im;
    }

    static BufferedImage sawMillFrontActive() {
        BufferedImage strip = new BufferedImage(16, 64, BufferedImage.TYPE_INT_ARGB);
        for (int f = 0; f < 4; f++) strip.getGraphics().drawImage(sawMillFront(f), 0, f * 16, null);
        return strip;
    }

    /** Table top: planks running front to back, the blade slot down the middle between two riveted iron wear strips. */
    static BufferedImage sawMillTop() {
        BufferedImage im = img();
        Random r = new Random(9103);
        grain(im, WOOD, r, 0, 0, 16, 16, 3, true);
        for (int y = 0; y < 16; y++) {
            px(im, 4, y, WOOD.get(1)); px(im, 5, y, WOOD.get(4));
            px(im, 11, y, WOOD.get(1)); px(im, 12, y, WOOD.get(4));
            px(im, 0, y, WOOD.get(4)); px(im, 15, y, WOOD.get(2));
        }
        for (int x = 0; x < 16; x++) { px(im, x, 0, WOOD.get(4)); px(im, x, 15, WOOD.get(2)); }
        // Iron wear strips either side of the slot.
        for (int y = 1; y <= 14; y++) {
            px(im, 6, y, WROUGHT_IRON.get(y == 1 ? 5 : 4)); px(im, 9, y, WROUGHT_IRON.get(y == 1 ? 4 : 3));
        }
        px(im, 6, 14, WROUGHT_IRON.get(3)); px(im, 9, 14, WROUGHT_IRON.get(2));
        // The slot: dark, its far (right) wall catching a little light.
        for (int y = 2; y <= 13; y++) { px(im, 7, y, CHARCOAL.get(1)); px(im, 8, y, y == 13 ? WOOD.get(2) : DARK_WOOD.get(2)); }
        for (int y : new int[] {3, 8, 12}) { px(im, 6, y, WROUGHT_IRON.spec()); px(im, 9, y, WROUGHT_IRON.get(5)); }
        // Sawdust gathered at the slot's ends and along the strips.
        int[][] dust = {{7, 1}, {8, 1}, {7, 14}, {8, 14}, {10, 13}, {10, 2}, {5, 9}};
        for (int[] d : dust) px(im, d[0], d[1], STRAW.get(d[1] == 1 || d[0] == 5 ? 5 : 4));
        peg(im, WOOD, 1, 1); peg(im, WOOD, 13, 1); peg(im, WOOD, 1, 13); peg(im, WOOD, 13, 13);
        return im;
    }

    /** Saw blade plate: set teeth along the top (rows 0-1), then the ground steel disc with arc scratches. */
    static BufferedImage sawMillBlade() {
        BufferedImage im = img();
        double[][] n = fractal(9104);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int step = n[y][x] > 0.6 ? 4 : 3;
                double d = Math.hypot(x - 7.5, y - 20);
                if (Math.abs(d - 9.5) < 0.5 || Math.abs(d - 14.5) < 0.4) step = 4;
                if (Math.abs(d - 12) < 0.45) step = 2;
                px(im, x, y, WROUGHT_IRON.get(step));
            }
        int[] tooth = {5, 4, 2, 1};
        int[] gullet = {4, 3, 3, 2};
        for (int x = 0; x < 16; x++) {
            px(im, x, 0, WROUGHT_IRON.get(tooth[x % 4]));
            px(im, x, 1, WROUGHT_IRON.get(gullet[x % 4]));
        }
        px(im, 4, 0, WROUGHT_IRON.spec());
        return im;
    }

    /** Heavy oak frame timber: lengthwise grain, two riveted wrought iron straps, a knot and a drying check. */
    static BufferedImage tripHammerFrame() {
        BufferedImage im = img();
        Random r = new Random(9201);
        grain(im, WOOD, r, 0, 0, 16, 16, 3, true);
        // Knot and check.
        px(im, 9, 7, WOOD.get(1)); px(im, 10, 7, WOOD.get(2)); px(im, 9, 8, WOOD.get(2)); px(im, 8, 7, WOOD.get(4));
        for (int y = 6; y <= 9; y++) px(im, 4 + (y > 7 ? 1 : 0), y, WOOD.get(1));
        for (int sy : new int[] {1, 11}) {
            strapH(im, 0, 15, sy, WOOD);
            for (int x : new int[] {2, 7, 13}) rivet(im, x, sy);
        }
        return im;
    }

    /** Wrought iron hammer head: forged dark body (rows 0-11) with hammer marks, bright worn striking face (rows 12-15). */
    static BufferedImage tripHammerHead() {
        double[][] n = fractal(9301), e = noise(9302, 4);
        BufferedImage im = img();
        for (int y = 0; y < 12; y++)
            for (int x = 0; x < 16; x++) {
                int step = n[y][x] > 0.6 ? 3 : 2;
                if (y == 0 || x == 0) step = 3;
                if ((y == 0 && x < 6) || (x == 0 && y < 4)) step = 4;
                if (x == 15 || (y == 11 && e[y][x] > 0.45)) step = 1;
                px(im, x, y, WROUGHT_IRON.get(step));
            }
        int[][] dents = {{3, 3}, {9, 2}, {12, 6}, {5, 8}, {10, 9}};
        for (int[] d : dents) {
            px(im, d[0], d[1], WROUGHT_IRON.get(1));
            px(im, d[0] + 1, d[1], WROUGHT_IRON.get(1));
            px(im, d[0], d[1] + 1, WROUGHT_IRON.get(2));
            px(im, d[0] + 1, d[1] + 1, WROUGHT_IRON.get(4));
        }
        // Striking face: polished by the work, brightest in the middle, a few pits.
        for (int y = 12; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double d = Math.hypot((x - 7.5) / 2.0, y - 13.5);
                int step = d < 1.6 ? 5 : d < 3.2 ? 4 : 3;
                if (y == 15 || x == 15) step = Math.min(step, 3);
                if (y == 12 || x == 0) step = Math.max(step, 4);
                px(im, x, y, WROUGHT_IRON.get(step));
            }
        px(im, 7, 13, WROUGHT_IRON.spec()); px(im, 8, 13, WROUGHT_IRON.spec());
        int[][] pits = {{3, 14}, {11, 13}, {13, 14}};
        for (int[] p : pits) px(im, p[0], p[1], WROUGHT_IRON.get(2));
        return im;
    }

    /** Oak beam side grain, along the texture, with an iron band (columns 7-8) round the beam. */
    static BufferedImage tripHammerArm() {
        BufferedImage im = img();
        Random r = new Random(9401);
        grain(im, WOOD, r, 0, 0, 16, 16, 3, false);
        for (int y = 0; y < 16; y += 4) for (int x = 0; x < 16; x++) { px(im, x, y, WOOD.get(4)); }
        px(im, 3, 9, WOOD.get(1)); px(im, 4, 9, WOOD.get(2)); px(im, 3, 10, WOOD.get(2)); px(im, 12, 5, WOOD.get(1)); px(im, 13, 5, WOOD.get(2));
        for (int y = 0; y < 16; y++) {
            px(im, 7, y, WROUGHT_IRON.get(4)); px(im, 8, y, WROUGHT_IRON.get(2)); px(im, 9, y, WOOD.get(1));
        }
        for (int y = 1; y < 16; y += 4) px(im, 7, y, WROUGHT_IRON.get(5));
        return im;
    }

    /** Core sampler base side: four pegged horizontal boards with iron corner straps. */
    static BufferedImage coreSamplerSide() {
        BufferedImage im = img();
        Random r = new Random(9501);
        grain(im, WOOD, r, 0, 0, 16, 16, 3, false);
        for (int b = 0; b < 4; b++) {
            int y0 = b * 4;
            for (int x = 0; x < 16; x++) { px(im, x, y0, WOOD.get(4)); px(im, x, y0 + 3, WOOD.get(r.nextInt(4) == 0 ? 2 : 1)); }
            peg(im, WOOD, 3 + (b % 2) * 8, y0 + 1);
        }
        for (int y = 0; y < 16; y++) {
            px(im, 0, y, WROUGHT_IRON.get(y % 4 == 0 ? 5 : 4)); px(im, 1, y, WROUGHT_IRON.get(3));
            px(im, 14, y, WROUGHT_IRON.get(3)); px(im, 15, y, WROUGHT_IRON.get(2));
        }
        for (int y = 1; y < 16; y += 4) { px(im, 0, y, WROUGHT_IRON.get(5)); px(im, 1, y, WROUGHT_IRON.get(2)); px(im, 14, y, WROUGHT_IRON.get(5)); px(im, 15, y, WROUGHT_IRON.get(1)); }
        return im;
    }

    /** Core sampler top: boards round a riveted wrought iron drill collar; the bore in the middle. */
    static BufferedImage coreSamplerTop() {
        BufferedImage im = img();
        Random r = new Random(9502);
        grain(im, WOOD, r, 0, 0, 16, 16, 3, true);
        for (int y = 0; y < 16; y++) { px(im, 3, y, WOOD.get(1)); px(im, 4, y, WOOD.get(4)); px(im, 11, y, WOOD.get(1)); px(im, 12, y, WOOD.get(4)); }
        for (int i = 0; i < 16; i++) { px(im, i, 0, WOOD.get(4)); px(im, 0, i, WOOD.get(4)); px(im, i, 15, WOOD.get(2)); px(im, 15, i, WOOD.get(2)); }
        for (int y = 2; y <= 13; y++)
            for (int x = 2; x <= 13; x++) {
                double dx = x - 7.5, dy = y - 7.5, d = Math.hypot(dx, dy);
                if (d > 4.9 && d < 5.6) px(im, x, y, WOOD.get(1)); // collar shadow cut into the boards
                if (d <= 4.9 && d > 1.5) {
                    int step = d > 4.0 ? (dx + dy < 0 ? 5 : 2) : d < 2.5 ? (dx + dy < 0 ? 2 : 4) : 3;
                    px(im, x, y, WROUGHT_IRON.get(step));
                }
                if (d <= 1.5) px(im, x, y, CHARCOAL.get(1));
            }
        int[][] rv = {{7, 3}, {3, 7}, {11, 7}, {7, 11}};
        for (int[] p : rv) rivet(im, p[0], p[1]);
        px(im, 5, 5, WROUGHT_IRON.spec());
        peg(im, WOOD, 1, 1); peg(im, WOOD, 13, 1); peg(im, WOOD, 1, 13); peg(im, WOOD, 13, 13);
        return im;
    }

    /** Drill rod: wrought iron with a spiral flute winding down it (one dark groove with a lit lip every 6 rows). */
    static BufferedImage coreSamplerDrill() {
        BufferedImage im = img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int k = Math.floorMod(y - x / 2, 8);
                int step = k == 0 ? 1 : k == 1 ? 2 : k == 7 ? 4 : 3;
                px(im, x, y, WROUGHT_IRON.get(step));
            }
        return im;
    }

    /**
     * Bark: a broad strip lying diagonally, bowed like a trough. The upper-left half shows the pale, streaky inner
     * bark; along the lower-right the rough outer bark with its fissures rolls into view; the top-right end curls
     * over on itself.
     */
    static BufferedImage barkItem() {
        BufferedImage im = img();
        Random r = new Random(9601);
        for (int y = 2; y <= 13; y++)
            for (int x = 2; x <= 13; x++) {
                int t = x - y;                 // along the strip: bottom-left negative, top-right positive
                double c = 15.5 + 0.03 * t * t; // bowed centre line (x + y)
                int k = (int) Math.floor(x + y - c + 0.5);
                if (k < -3 || k > 3 || t < -9 || t > 4) continue;
                int col;
                if (k == -3) col = BARK.get(3);                                 // far rim: the bark's thickness
                else if (k <= 0) {                                               // inner face, streaked lengthwise
                    int step = k == -2 ? 4 : k == -1 ? 4 : 3;
                    if (k == -1 && Math.floorMod(t, 5) == 1) step = 5;
                    if (k == 0 && Math.floorMod(t, 4) == 2) step = 2;
                    col = LEATHER.get(step);
                } else {                                                         // outer bark rolling away
                    int step = k == 1 ? 4 : k == 2 ? 2 : 1;
                    if (k == 2 && Math.floorMod(t * 3, 7) < 2) step = 3;
                    if (k == 1 && r.nextInt(4) == 0) step = 3;
                    col = BARK.get(step);
                }
                px(im, x, y, col);
            }
        // The curl: the top-right end is rolled up into a tube lying across the strip, outer bark outside.
        for (int y = 2; y <= 13; y++)
            for (int x = 2; x <= 13; x++) {
                int t = x - y, sum = x + y;
                if (t < 4 || t > 6 || sum < 12 || sum > 19) continue;
                int step = t == 4 ? 3 : t == 5 ? 4 : 2;
                if (t == 5 && sum % 3 == 0) step = 5;
                if (t == 4 && sum % 4 == 1) step = 2;
                px(im, x, y, BARK.get(step));
            }
        // The roll's open end at the upper left: a spiral of bark round the pale inner layer.
        px(im, 8, 3, BARK.get(4)); px(im, 9, 3, LEATHER.get(5)); px(im, 9, 4, LEATHER.get(3)); px(im, 10, 4, BARK.get(2));
        px(im, 8, 4, BARK.get(3)); px(im, 9, 2, BARK.get(4)); px(im, 10, 3, BARK.get(1));
        return outline(im);
    }

    /** Core sample: an upright stone cylinder, bedded bands of five rocks, a copper fleck in the granite. */
    static BufferedImage coreSampleItem() {
        BufferedImage im = img();
        Ramp[] bands = {LIMESTONE, SHALE, RHYOLITE, BASALT, GRANITE};
        int[] tops = {3, 5, 7, 9, 11};
        int[][] wobble = {{0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 1, 1, 0, 0, 0}, {0, 0, 0, 0, -1, -1, 0, 0}, {1, 0, 0, 0, 0, 0, 1, 1}, {0, 0, 0, 0, 0, 1, 1, 0}};
        int[] shade = {0, 1, 1, 0, 0, 0, -1, -1};
        for (int y = 3; y <= 13; y++)
            for (int x = 4; x <= 11; x++) {
                if (y == 13 && (x == 4 || x == 11)) continue;
                int b = 0;
                for (int i = 1; i < 5; i++) if (y >= tops[i] + wobble[i][x - 4]) b = i;
                int step = 3 + shade[x - 4];
                if (y == 13) step--;
                px(im, x, y, bands[b].get(clampStep(step)));
            }
        // Top: the cut face of the limestone, lit from above, a darker front lip.
        for (int x = 5; x <= 10; x++) px(im, x, 2, LIMESTONE.get(x < 8 ? 5 : 4));
        px(im, 4, 3, LIMESTONE.get(4)); px(im, 11, 3, LIMESTONE.get(2));
        for (int x = 5; x <= 10; x++) px(im, x, 3, LIMESTONE.get(x < 8 ? 4 : 3));
        // A parting in the shale, granite crystals, a vesicle in the basalt, and the ore fleck.
        px(im, 6, 6, SHALE.get(5)); px(im, 7, 6, SHALE.get(4));
        px(im, 9, 12, GRANITE.get(5)); px(im, 5, 13, GRANITE.get(1)); px(im, 10, 11, GRANITE.get(1));
        px(im, 6, 11, COPPER.get(4)); px(im, 7, 11, COPPER.spec()); px(im, 7, 12, COPPER.get(2)); px(im, 6, 12, COPPER.get(3));
        px(im, 8, 9, BASALT.get(1)); px(im, 9, 9, BASALT.get(4));
        return outline(im);
    }

    static String[] sawBladeGhost() {
        String[] rows = new String[16];
        for (int y = 0; y < 16; y++) {
            StringBuilder sb = new StringBuilder();
            for (int x = 0; x < 16; x++) {
                double dx = x - 7.5, dy = y - 7.5, d = Math.hypot(dx, dy);
                double a = (Math.atan2(dy, dx) / (2 * Math.PI) + 1) * 12 % 1.0;
                boolean rim = d >= 5.2 && d < 6.2 && a >= 0.4;
                boolean tooth = d >= 5.2 && d < 7.4 && a < 0.4 && a > 0.05;
                boolean hole = d >= 1.2 && d < 2.2;
                sb.append(rim || tooth || hole ? '#' : '.');
            }
            rows[y] = sb.toString();
        }
        return rows;
    }

    static void inventory(BufferedImage im) {
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++) slot(im, 8 + col * 18, 84 + row * 18);
        for (int col = 0; col < 9; col++) slot(im, 8 + col * 18, 142);
    }

    /** Trip hammer: pattern slot and workpiece slot; y 60-72 left for the status line. */
    static BufferedImage tripHammerGui() {
        BufferedImage im = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        panel(im, 176, 166);
        slot(im, 44, 35);
        slot(im, 80, 35);
        ghost(im, 44, 35, GHOST_PATTERN);
        ghost(im, 80, 35, GHOST_INGOT);
        inventory(im);
        return im;
    }

    /** Saw mill: input, blade, progress arrow, large plank output and a bark output. */
    static BufferedImage sawMillGui() {
        BufferedImage im = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        panel(im, 176, 166);
        slot(im, 56, 35);
        slot(im, 32, 35);
        ghost(im, 32, 35, sawBladeGhost());
        arrow(im, 79, 34);
        well(im, 111, 30, 26, 26, SLOT_FILL);
        slot(im, 142, 35);
        for (int j = 0; j < 15; j++) {
            int d = Math.abs(j - 7);
            if (d <= 2) for (int i = 0; i < 15; i++) im.setRGB(176 + i, 14 + j, 0xff000000 | GUI_LIGHT);
            for (int i = 15; i < 22 - d; i++) im.setRGB(176 + i, 14 + j, 0xff000000 | GUI_LIGHT);
        }
        inventory(im);
        return im;
    }

    /** Core sampler: large output, drill button sprites (u 176, v 0/18/36), progress well and its brass fill (u 176, v 54). */
    static BufferedImage coreSamplerGui() {
        BufferedImage im = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        panel(im, 176, 166);
        well(im, 119, 30, 26, 26, SLOT_FILL);
        well(im, 34, 58, 80, 6, 0x2a2a2a);
        int[] faces = {0x8b8b8b, 0x9a9ec8, 0x5a5a5a};
        int[] lights = {0xc6c6c6, 0xc8ccf0, 0x6e6e6e};
        int[] shadows = {0x555555, 0x5a5e88, 0x404040};
        for (int b = 0; b < 3; b++) {
            int y0 = b * 18;
            fill(im, 176, y0, 54, 18, faces[b]);
            fill(im, 176, y0, 54, 1, GUI_EDGE);
            fill(im, 176, y0 + 17, 54, 1, GUI_EDGE);
            fill(im, 176, y0, 1, 18, GUI_EDGE);
            fill(im, 229, y0, 1, 18, GUI_EDGE);
            fill(im, 177, y0 + 1, 52, 1, lights[b]);
            fill(im, 177, y0 + 1, 1, 16, lights[b]);
            fill(im, 177, y0 + 15, 52, 2, shadows[b]);
            fill(im, 228, y0 + 1, 1, 16, shadows[b]);
            if (b == 1) { // hovered: white rim like vanilla
                fill(im, 176, y0, 54, 1, GUI_LIGHT); fill(im, 176, y0 + 17, 54, 1, GUI_LIGHT);
                fill(im, 176, y0, 1, 18, GUI_LIGHT); fill(im, 229, y0, 1, 18, GUI_LIGHT);
            }
        }
        for (int x = 0; x < 80; x++)
            for (int y = 0; y < 6; y++) {
                int step = y == 0 ? 5 : y == 5 ? 2 : y == 4 ? 3 : 4;
                if (y > 0 && y < 4 && x % 8 == 7) step = 3;
                px(im, 176 + x, 54 + y, BRONZE.get(step));
            }
        inventory(im);
        return im;
    }

    static void machines() throws IOException {
        save("block/saw_mill_side", sawMillSide());
        save("block/saw_mill_top", sawMillTop());
        save("block/saw_mill_front", sawMillFront(-1));
        saveAnimated("block/saw_mill_front_active", sawMillFrontActive(), 2);
        save("block/saw_mill_blade", sawMillBlade());
        save("block/trip_hammer_frame", tripHammerFrame());
        save("block/trip_hammer_head", tripHammerHead());
        save("block/trip_hammer_arm", tripHammerArm());
        save("block/core_sampler_side", coreSamplerSide());
        save("block/core_sampler_top", coreSamplerTop());
        save("block/core_sampler_drill", coreSamplerDrill());
        save("item/bark", barkItem());
        save("item/core_sample", coreSampleItem());
        saveRaw("gui/trip_hammer", tripHammerGui());
        saveRaw("gui/saw_mill", sawMillGui());
        saveRaw("gui/core_sampler", coreSamplerGui());
    }

    // ---------------------------------------------------------------- tier 3: washing (spec 20.2, 20.4, 20.5)

    // Clear stream water, built per SG 3: inky blue darks, pale cyan-grey lights; the specular is the sheen.
    static final Ramp WATER = ramp(0xe2eef0, 0x1c2434, 0x27384c, 0x37526a, 0x557890, 0x8cb0c0);

    /**
     * The washed form of a crushed pile (spec 20.4): same silhouette, and every lit grain (a 4-connected
     * cluster of steps 4, 5 and specular) gains one more lit pixel next to it. The water sheen is added by the caller.
     */
    static String[] washedPile(String[] crushed) {
        int h = crushed.length, w = crushed[0].length();
        char[][] g = new char[h][];
        for (int y = 0; y < h; y++) g[y] = crushed[y].toCharArray();
        boolean[][] seen = new boolean[h][w];
        java.util.function.BiPredicate<Integer, Integer> lit = (x, y) ->
                x >= 0 && y >= 0 && y < h && x < w && (g[y][x] == '4' || g[y][x] == '5' || g[y][x] == 's');
        List<int[]> raise = new ArrayList<>();
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                if (seen[y][x] || !lit.test(x, y)) continue;
                List<int[]> cluster = new ArrayList<>();
                java.util.ArrayDeque<int[]> q = new java.util.ArrayDeque<>();
                q.add(new int[] {x, y});
                seen[y][x] = true;
                while (!q.isEmpty()) {
                    int[] p = q.poll();
                    cluster.add(p);
                    int[][] d = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
                    for (int[] e : d) {
                        int nx = p[0] + e[0], ny = p[1] + e[1];
                        if (lit.test(nx, ny) && !seen[ny][nx]) { seen[ny][nx] = true; q.add(new int[] {nx, ny}); }
                    }
                }
                cluster.sort((a, b) -> a[1] != b[1] ? a[1] - b[1] : a[0] - b[0]);
                // The grain's top surface grows: right of a lit pixel first, then below it.
                search:
                for (int[] p : cluster)
                    for (int[] e : new int[][] {{1, 0}, {0, 1}}) {
                        int nx = p[0] + e[0], ny = p[1] + e[1];
                        if (nx < w && ny < h && (g[ny][nx] == '3' || g[ny][nx] == '2')) { raise.add(new int[] {nx, ny}); break search; }
                    }
            }
        for (int[] p : raise) g[p[1]][p[0]] = '4';
        String[] out = new String[h];
        for (int y = 0; y < h; y++) out[y] = new String(g[y]);
        return out;
    }

    /**
     * Draws a washed pile and puts the single water-sheen highlight on it (map coordinates): a near-white
     * glint with a pale trailing pixel, like light on the film of water left on the grains.
     */
    static BufferedImage washedItem(Ramp a, String[] rows, int sx, int sy) {
        BufferedImage im = map(a, rows);
        int left = (16 - rows[0].length()) / 2, top = (16 - rows.length) / 2;
        px(im, left + sx, top + sy, WATER.spec());
        if (rows[sy].charAt(sx + 1) != '.') px(im, left + sx + 1, top + sy, WATER.get(5));
        return im;
    }

    /**
     * Washing pan, seen from above at an angle: a shallow copper dish (lit rim top-left, the concave
     * floor lit on its lower-right wall), its outer wall showing below the rim, and a stick handle
     * riveted into a tang at the lower left. {@code loaded} fills the bowl with wet gravel.
     */
    static BufferedImage washingPan(boolean loaded) {
        BufferedImage im = img();
        double cx = 8.6, cy = 6.5, rx = 5.5, ry = 3.7;
        boolean[][] dish = new boolean[16][16], bowl = new boolean[16][16];
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double nx = (x + 0.5 - cx) / rx, ny = (y + 0.5 - cy) / ry;
                dish[y][x] = nx * nx + ny * ny <= 1.0;
                double ix = (x + 0.5 - cx) / (rx - 1.25), iy = (y + 0.5 - cy - 0.15) / (ry - 1.1);
                bowl[y][x] = ix * ix + iy * iy <= 1.0;
            }
        // Handle first, so the pan's outer wall overlaps its end: a 2 px stick running down-left.
        int[][] stick = {{5, 10}, {4, 11}, {3, 12}, {2, 13}};
        for (int[] p : stick) {
            px(im, p[0], p[1], WOOD.get(4));
            px(im, p[0] + 1, p[1], WOOD.get(3));
            px(im, p[0] + 1, p[1] + 1, WOOD.get(2));
        }
        px(im, 2, 14, WOOD.get(2)); px(im, 3, 14, CLEAR);
        im.setRGB(3, 14, 0);
        px(im, 3, 12, WOOD.get(5));
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                // Outer wall: the band of the dish shifted down one pixel.
                if (!dish[y][x] && y > 0 && dish[y - 1][x]) px(im, x, y, COPPER.get(x < cx - 1 ? 2 : 1));
                if (!dish[y][x]) continue;
                int step;
                if (!bowl[y][x]) step = y + 0.5 < cy ? (x + 0.5 < cx + 1 ? 5 : 4) : (x + 0.5 < cx - 2 ? 4 : x + 0.5 < cx + 2 ? 3 : 2); // rim
                else if (!bowl[y - 1][x]) step = 1;        // far wall in the shadow of the rim
                else if (!bowl[y + 1][x]) step = 4;        // near wall faces the light
                else if (!bowl[y][x - 1]) step = 2;        // left wall, shaded
                else if (!bowl[y][x + 1]) step = 4;        // right wall, lit
                else step = 3;
                px(im, x, y, COPPER.get(step));
            }
        px(im, 6, 3, COPPER.spec());
        if (!loaded) {
            // A polished scour on the floor where the gravel swirls.
            px(im, 9, 7, COPPER.get(4)); px(im, 10, 7, COPPER.get(4));
        } else {
            // Wet gravel heaped to the rim: dark grey stones, lit tops, a film of water round the near edge.
            for (int y = 0; y < 16; y++)
                for (int x = 0; x < 16; x++) {
                    if (!bowl[y][x]) continue;
                    int step = !bowl[y - 1][x] ? 1 : !bowl[y + 1][x] ? 0 : 2;
                    px(im, x, y, step == 0 ? WATER.get(3) : FIELD_STONE.get(step));
                }
            int[][] stones = {{5, 6, 2}, {7, 5, 2}, {10, 5, 2}, {12, 6, 1}, {8, 7, 2}, {11, 7, 1}, {6, 8, 1}};
            for (int[] st : stones) {
                int x0 = st[0], y0 = st[1], w = st[2];
                for (int i = 0; i < w; i++) {
                    if (bowl[y0][x0 + i]) px(im, x0 + i, y0, FIELD_STONE.get(i == 0 ? 4 : 3));
                    if (bowl[y0 + 1][x0 + i] && bowl[y0 + 2][x0 + i]) px(im, x0 + i, y0 + 1, FIELD_STONE.get(i == 0 ? 3 : 1));
                }
            }
            px(im, 9, 6, FIELD_STONE.get(1));
            px(im, 7, 5, FIELD_STONE.get(5));
            px(im, 10, 7, GOLD.get(5)); px(im, 10, 8, GOLD.get(2));
            px(im, 7, 8, WATER.spec()); px(im, 8, 8, WATER.get(5));
        }
        return outline(im);
    }

    /**
     * Sluice boards: four horizontal planks, lit top edges, dark seams, a butt joint with treenails per board,
     * and a damp lower edge where the trough sweats water (the model shows rows 8-15 on its outer walls).
     */
    static BufferedImage sluicePlanks() {
        BufferedImage im = img();
        Random r = new Random(9701);
        grain(im, WOOD, r, 0, 0, 16, 16, 3, false);
        int[] joints = {10, 3, 12, 6};
        for (int b = 0; b < 4; b++) {
            int y0 = b * 4;
            for (int x = 0; x < 16; x++) {
                px(im, x, y0, WOOD.get(x % 6 == 2 ? 5 : 4));
                px(im, x, y0 + 3, WOOD.get(r.nextInt(5) == 0 ? 2 : 1));
            }
            int j = joints[b];
            px(im, j, y0 + 1, WOOD.get(1)); px(im, j, y0 + 2, WOOD.get(1));
            px(im, j + 1, y0 + 1, WOOD.get(4)); px(im, j + 1, y0 + 2, WOOD.get(3));
            px(im, Math.floorMod(j - 2, 16), y0 + 1, WOOD.get(5)); px(im, Math.floorMod(j - 2, 16), y0 + 2, WOOD.get(2));
            px(im, Math.floorMod(j + 3, 16), y0 + 1, WOOD.get(5)); px(im, Math.floorMod(j + 3, 16), y0 + 2, WOOD.get(2));
        }
        // Damp bottom board: the wood darkens towards the water line.
        for (int x = 0; x < 16; x++) {
            if ((x * 7) % 5 < 2) px(im, x, 14, WET_WOOD.get(3));
            px(im, x, 15, WET_WOOD.get((x * 3) % 7 == 0 ? 2 : 1));
        }
        return im;
    }

    /**
     * Sluice bed, seen from above with north (the outlet) at the top: wet lengthwise boards, and riffle bars
     * across at rows 3, 7 and 11 (the model's bars sample these rows). Behind each bar (upstream, the row
     * below it) heavy dark sand is trapped with a gold fleck; in front of each bar the water scours a shadow.
     */
    static BufferedImage sluiceBed() {
        BufferedImage im = img();
        Random r = new Random(9702);
        grain(im, WET_WOOD, r, 0, 0, 16, 16, 4, true);
        for (int y = 0; y < 16; y++)
            for (int sx : new int[] {2, 6, 10, 14}) px(im, sx, y, WET_WOOD.get((y * 5 + sx) % 7 == 0 ? 2 : 1));
        int[] gold = {9, 4, 12};
        int[] bars = {3, 7, 11};
        for (int i = 0; i < 3; i++) {
            int z = bars[i];
            for (int x = 0; x < 16; x++) {
                px(im, x, z, WOOD.get(x % 5 == 1 ? 5 : 4));
                px(im, x, z - 1, WET_WOOD.get(x % 6 == 4 ? 2 : 1));
            }
            // Trapped heavy sand: a dark bank against the bar with lit grain tops, thinning out upstream.
            Random sr = new Random(9710 + i);
            int x = 0;
            while (x < 16) {
                int run = 2 + sr.nextInt(3);
                boolean black = sr.nextInt(4) == 0;
                for (int k = 0; k < run && x < 16; k++, x++) {
                    Ramp a = black ? MAGNETITE : FIELD_STONE;
                    px(im, x, z + 1, a.get(k == 0 ? 3 : 2));
                    if (k == 0 && sr.nextInt(2) == 0) px(im, x, z + 2, FIELD_STONE.get(2));
                }
            }
            px(im, gold[i], z + 1, GOLD.get(5));
            px(im, gold[i] + 1, z + 1, GOLD.get(3));
        }
        return im;
    }

    /**
     * Running water for the sluice's water plane: 16 frames, coloured like vanilla default water (#3F76E4)
     * so it needs no tint, partial alpha (SG 2 allows it for fluids). Streaks drift one pixel per frame
     * towards row 0 (north, the outlet), so the strip loops seamlessly; foam flecks flicker just downstream
     * of the riffle bars (rows 3, 7, 11 of the bed).
     */
    static BufferedImage sluiceWater() {
        int frames = 16;
        int base = 0x3f76e4, dark = 0x3466cc, light = 0x6a96ec, streak = 0x9ebff4, foam = 0xdce8f8;
        BufferedImage im = new BufferedImage(16, 16 * frames, BufferedImage.TYPE_INT_ARGB);
        double[][] n = fractal(9720);
        // Streaks: column, start row, length, bright.
        int[][] streaks = {{3, 1, 4, 1}, {6, 9, 3, 0}, {8, 4, 5, 1}, {11, 12, 4, 0}, {12, 2, 3, 1}, {4, 13, 3, 0}, {9, 14, 2, 0}};
        int[][] flecks = {{4, 2}, {9, 2}, {12, 2}, {3, 6}, {7, 6}, {11, 6}, {5, 10}, {10, 10}, {13, 10}};
        for (int f = 0; f < frames; f++) {
            int oy = f * 16;
            for (int y = 0; y < 16; y++)
                for (int x = 0; x < 16; x++) {
                    double v = n[Math.floorMod(y + f, 16)][x];
                    int c = v < 0.34 ? dark : v > 0.70 ? light : base;
                    im.setRGB(x, oy + y, (c == dark ? 0xbc : c == light ? 0xbe : 0xb4) << 24 | c);
                }
            for (int[] st : streaks)
                for (int k = 0; k < st[2]; k++) {
                    int y = Math.floorMod(st[1] + k - f, 16);
                    int c = st[3] == 1 && k == 0 ? streak : light;
                    im.setRGB(st[0], oy + y, (c == streak ? 0xc8 : 0xbe) << 24 | c);
                }
            for (int i = 0; i < flecks.length; i++) {
                int[] fl = flecks[i];
                int phase = (f + i * 5) % 8;
                if (phase < 3) im.setRGB(fl[0], oy + fl[1], 0xd8 << 24 | foam);
                if (phase == 1) im.setRGB(fl[0] + 1, oy + fl[1], 0xc8 << 24 | streak);
                if (phase == 2) im.setRGB(fl[0], oy + fl[1] - 1, 0xc8 << 24 | streak);
            }
        }
        return im;
    }

    /** Sluice: a row of four buffer slots (frames at x 53/71/89/107, y 35; items at +1), status line at y 62, player inventory at y 84. */
    static BufferedImage sluiceGui() {
        BufferedImage im = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        panel(im, 176, 166);
        for (int i = 0; i < 4; i++) slot(im, 54 + i * 18, 36); // frame top-left at 53 + 18i, 35
        inventory(im);
        return im;
    }

    /**
     * Core sample viewer, 220x172: a dark recessed well for the core strip (outer x 12, y 18, 20x132; the screen
     * paints 64 rows of 2 px from x 13, y 20 in the 18 px wide interior) with engraved depth ticks every 16 px
     * at x 8-11, and an inset paper sheet for the readout at x 40, y 18, 168x144.
     */
    static BufferedImage coreSampleGui() {
        BufferedImage im = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        panel(im, 220, 172);
        well(im, 12, 18, 20, 132, 0x2a2a2a);
        for (int k = 0; k <= 8; k++) {
            int y = 20 + k * 16;
            int x0 = k % 2 == 0 ? 8 : 9; // every other tick a pixel shorter, so the 32-block marks read
            fill(im, x0, y, 12 - x0, 1, GUI_SHADOW);
            fill(im, x0, y + 1, 12 - x0, 1, GUI_LIGHT);
        }
        // Paper: a 1 px recess, then fibrous sheet in the top paper steps, low contrast for dark grey text.
        well(im, 40, 18, 168, 144, PAPER.get(5));
        Random r = new Random(9730);
        double[][] n = fractal(9731);
        for (int y = 19; y < 161; y++)
            for (int x = 41; x < 207; x++)
                if (n[y % 16][x % 16] < 0.3 && (x * 3 + y * 7) % 5 == 0) px(im, x, y, PAPER.get(4));
        for (int i = 0; i < 90; i++) {
            int x = 42 + r.nextInt(160), y = 20 + r.nextInt(138), len = 2 + r.nextInt(3);
            for (int k = 0; k < len; k++) px(im, x + k, y, PAPER.get(4));
        }
        // Inner shadow under the top and left edges of the recess.
        fill(im, 41, 19, 166, 1, PAPER.get(4));
        fill(im, 41, 19, 1, 142, PAPER.get(4));
        return im;
    }

    static void washing() throws IOException {
        List<Mineral> all = new ArrayList<>(MINERALS);
        all.addAll(T3_MINERALS);
        String[] poor = washedPile(CRUSHED_SMALL), normal = washedPile(CRUSHED_NORMAL), rich = washedPile(CRUSHED_RICH);
        for (Mineral m : all) {
            String n = m.name();
            save("item/washed_poor_" + n, washedItem(m.ramp(), poor, 6, 2));
            save("item/washed_" + n, washedItem(m.ramp(), normal, 7, 3));
            save("item/washed_rich_" + n, washedItem(m.ramp(), rich, 6, 3));
        }
        save("item/washing_pan", washingPan(false));
        save("item/washing_pan_loaded", washingPan(true));
        save("block/sluice_planks", sluicePlanks());
        save("block/sluice_bed", sluiceBed());
        saveAnimated("block/sluice_water", sluiceWater(), 2);
        saveRaw("gui/sluice", sluiceGui());
        saveRaw("gui/core_sample", coreSampleGui());
    }

    // ---------------------------------------------------------------- tier 3: wind, belts and tanning (spec 7.2, 7.3, 12.1)

    // Sail cloth: fibre cloth bleached toward cream (spec 20.2). Hides: the spec 20.1 hide ramp, plus a chalky
    // grey-cream for limed hide and a clean pale tan for scraped hide. Barrel fluids: milky lye, tea-dark tannin.
    // All built per SG 3 (cool darks, warm lights).
    static final Ramp SAILCLOTH = ramp(0, 0x4a4832, 0x6a6646, 0x8e885e, 0xb2aa7c, 0xcec69a);
    static final Ramp HIDE = ramp(0, 0x4a3a30, 0x6a5444, 0x8c725a, 0xac9274, 0xc8b092);
    static final Ramp LIMED = ramp(0, 0x6a665e, 0x8c877a, 0xaca696, 0xc8c2b0, 0xdedac8);
    static final Ramp SCRAPED = ramp(0, 0x5c4838, 0x80664e, 0xa48866, 0xc2a682, 0xd8c29e);
    static final Ramp LYE = ramp(0, 0x7c8590, 0x99a1ab, 0xb5bcc4, 0xcdd3d8, 0xe2e6e9);
    static final Ramp TANNIN = ramp(0, 0x220c0c, 0x361410, 0x4c1d14, 0x64281a, 0x7e3824);

    static BufferedImage transpose(BufferedImage src) {
        BufferedImage im = img();
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) im.setRGB(x, y, src.getRGB(y, x));
        return im;
    }

    /** A vertical wrought iron strap, 2 columns, with its shadow on the wood to the right and a rivet per board. */
    static void strapV(BufferedImage im, int x, int y0, int y1, Ramp wood) {
        for (int y = y0; y <= y1; y++) {
            px(im, x, y, WROUGHT_IRON.get((y * 5 + x) % 7 == 0 ? 5 : 4));
            px(im, x + 1, y, WROUGHT_IRON.get((y * 3) % 5 == 0 ? 2 : 3));
            px(im, x + 2, y, wood.get(1));
        }
    }

    /**
     * Windmill bearing housing side: four heavy boards running front to back, lit upper edges, dark seams, and two
     * iron straps wrapping round the housing at columns 4-5 and 10-11. The strap columns are mirror-symmetric, so
     * the same texture serves the east face (u = 16 - z) and the west face (u = z) of the housing.
     */
    static BufferedImage bearingSide() {
        BufferedImage im = img();
        Random r = new Random(9801);
        grain(im, WOOD, r, 0, 0, 16, 16, 3, false);
        for (int b = 0; b < 4; b++) {
            int y0 = b * 4;
            for (int x = 0; x < 16; x++) {
                px(im, x, y0, WOOD.get(x % 6 == 2 ? 5 : 4));
                px(im, x, y0 + 3, WOOD.get(1));
            }
        }
        // Knots and a check in the boards.
        px(im, 2, 6, WOOD.get(1)); px(im, 3, 6, WOOD.get(2)); px(im, 2, 5, WOOD.get(5));
        px(im, 13, 9, WOOD.get(2)); px(im, 14, 9, WOOD.get(1)); px(im, 13, 10, WOOD.get(2));
        px(im, 7, 13, WOOD.get(2)); px(im, 8, 13, WOOD.get(2));
        for (int sx : new int[] {4, 10}) {
            strapV(im, sx, 0, 15, WOOD);
            for (int b = 0; b < 4; b++) rivet(im, sx, b * 4 + 1);
        }
        return im;
    }

    /** Windmill bearing front: a pegged timber frame round a riveted iron bearing plate with a polished ring and dark bore. */
    static BufferedImage bearingFront() {
        BufferedImage im = img();
        Random r = new Random(9811);
        grain(im, WOOD, r, 0, 0, 16, 2, 3, false);
        grain(im, WOOD, r, 0, 14, 16, 2, 3, false);
        grain(im, WOOD, r, 0, 2, 2, 12, 3, true);
        grain(im, WOOD, r, 14, 2, 2, 12, 3, true);
        for (int i = 0; i < 16; i++) {
            px(im, i, 0, WOOD.get(4)); px(im, 0, i, WOOD.get(4));
            px(im, i, 15, WOOD.get(2)); px(im, 15, i, WOOD.get(2));
        }
        px(im, 1, 1, WOOD.get(1)); px(im, 14, 1, WOOD.get(1)); px(im, 1, 14, WOOD.get(1)); px(im, 14, 14, WOOD.get(1));
        double[][] n = fractal(9812);
        for (int y = 2; y < 14; y++)
            for (int x = 2; x < 14; x++) {
                double d = Math.hypot(x - 7.5, y - 7.5);
                int c;
                if (d < 3.4) c = CHARCOAL.get(d < 2.4 ? 1 : 2);                         // the bore the hub turns in
                else if (d < 4.3) c = WROUGHT_IRON.get(x + y < 15 ? 1 : 3);              // inner lip: shadowed top-left, lit far side
                else if (d < 5.5) c = WROUGHT_IRON.get(x + y < 13 ? 5 : x + y < 17 ? 4 : 2); // polished race
                else {
                    int step = n[y][x] > 0.62 ? 2 : 3;
                    if (y == 2 || x == 2) step = 4;
                    if (y == 13 || x == 13) step = 2;
                    c = WROUGHT_IRON.get(step);
                }
                px(im, x, y, c);
            }
        px(im, 5, 4, WROUGHT_IRON.spec()); px(im, 4, 5, WROUGHT_IRON.spec());
        rivet(im, 3, 3); rivet(im, 11, 3); rivet(im, 3, 11); rivet(im, 11, 11);
        return im;
    }

    /** Windmill bearing back: upright boards and an iron-lined square socket where the axle enters. */
    static BufferedImage bearingBack() {
        BufferedImage im = img();
        Random r = new Random(9821);
        grain(im, WOOD, r, 0, 0, 16, 16, 3, true);
        for (int y = 0; y < 16; y++) {
            for (int sx : new int[] {3, 7, 11}) { px(im, sx, y, WOOD.get(1)); px(im, sx + 1, y, WOOD.get(4)); }
            px(im, 0, y, WOOD.get(4)); px(im, 15, y, WOOD.get(2));
        }
        for (int x = 0; x < 16; x++) { px(im, x, 0, WOOD.get(4)); px(im, x, 15, WOOD.get(2)); }
        for (int y = 4; y <= 11; y++)
            for (int x = 4; x <= 11; x++) {
                boolean edge = x == 4 || y == 4 || x == 11 || y == 11;
                if (edge) px(im, x, y, WROUGHT_IRON.get(x == 11 || y == 11 ? 2 : (x == 4 && y == 4) ? 5 : 4));
                else if (x >= 6 && x <= 9 && y >= 6 && y <= 9) px(im, x, y, (x == 9 || y == 9) ? WOOD.get(2) : DARK_WOOD.get(1));
                else px(im, x, y, WROUGHT_IRON.get(x == 5 || y == 5 ? 3 : (x == 10 || y == 10) ? 4 : 3));
            }
        px(im, 6, 6, CHARCOAL.get(1));
        rivet(im, 5, 5); rivet(im, 9, 5); rivet(im, 5, 9); rivet(im, 9, 9);
        peg(im, WOOD, 1, 1); peg(im, WOOD, 13, 1); peg(im, WOOD, 1, 13); peg(im, WOOD, 13, 13);
        return im;
    }

    /**
     * Windmill hub, laid out as a small atlas for the hub rotor: rows 0-1 lengthwise spoke grain, columns 0-1 and
     * 14-15 (rows 2-11) the same grain turned upright, the centre (4-11, 4-11) the hub's front cap (end grain in an
     * iron band, a square iron boss), rows 12-15 (columns 4-11) the hub's rim: an iron band at the front, wood behind.
     */
    static BufferedImage windmillHub() {
        BufferedImage im = img();
        Random r = new Random(9831);
        grain(im, DARK_WOOD, r, 0, 0, 16, 16, 3, false);
        grain(im, WOOD, r, 0, 0, 16, 2, 3, false);
        for (int x = 0; x < 16; x++) { px(im, x, 0, WOOD.get(4)); px(im, x, 1, WOOD.get(x % 5 == 3 ? 1 : 2)); }
        for (int sx : new int[] {0, 14}) {
            grain(im, WOOD, r, sx, 2, 2, 10, 3, true);
            for (int y = 2; y < 12; y++) { px(im, sx, y, WOOD.get(4)); px(im, sx + 1, y, WOOD.get(y % 5 == 1 ? 1 : 2)); }
        }
        BufferedImage cap = endGrain(WOOD, 9832, 7.5, 7.5, 1.4);
        for (int y = 4; y < 12; y++)
            for (int x = 4; x < 12; x++) {
                boolean band = x == 4 || y == 4 || x == 11 || y == 11;
                int c = band ? WROUGHT_IRON.get(x == 11 || y == 11 ? 2 : 4) : rgb(cap, x, y);
                px(im, x, y, c);
            }
        px(im, 4, 4, WROUGHT_IRON.get(5));
        for (int y = 6; y <= 9; y++)
            for (int x = 6; x <= 9; x++) px(im, x, y, WROUGHT_IRON.get(x == 9 || y == 9 ? 2 : (x == 6 || y == 6) ? 4 : 3));
        px(im, 6, 6, WROUGHT_IRON.spec()); px(im, 7, 7, WROUGHT_IRON.get(1)); px(im, 8, 8, WROUGHT_IRON.get(5));
        grain(im, WOOD, r, 4, 14, 8, 2, 3, false);
        for (int x = 4; x < 12; x++) {
            px(im, x, 12, WROUGHT_IRON.get(x == 6 ? 5 : 4));
            px(im, x, 13, WROUGHT_IRON.get(x % 3 == 0 ? 2 : 3));
            px(im, x, 14, WOOD.get(1));
        }
        rivet(im, 7, 12);
        return im;
    }

    /**
     * Windmill sail: fibre cloth stretched on a plank frame. A 1 px frame (2 px where sails meet) and a 2 px batten
     * across the middle; each cloth pane is shaded under the bar above it, bellies lighter in the middle, and is
     * lashed to the frame with twine. A loose weave runs over the cloth in short 2 px threads.
     */
    static BufferedImage windmillSail() {
        BufferedImage im = img();
        Random r = new Random(9841);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int pane = y < 8 ? 0 : 1, top = pane == 0 ? 1 : 9, bottom = pane == 0 ? 6 : 14;
                // The cloth bellies out: lit across its upper half, shadowed under the bar and where it meets the frame.
                int step = y == top ? 2 : y <= top + 2 ? 4 : 3;
                if (y == bottom || x == 14) step = 2;
                if (x == 1 && step == 4) step = 3;
                // Loose weave: 2 px threads, offset every other row.
                int phase = Math.floorMod(x + (y % 2) * 3, 6);
                if (step == 3 && phase == 0 && x < 14) step = 4;
                if (step == 3 && phase == 4 && y == bottom - 1) step = 2;
                px(im, x, y, SAILCLOTH.get(step));
            }
        // Frame and the middle batten.
        for (int i = 0; i < 16; i++) {
            px(im, i, 0, WOOD.get(4)); px(im, 0, i, WOOD.get(4));
            px(im, i, 15, WOOD.get(2)); px(im, 15, i, WOOD.get(2));
            px(im, i, 7, WOOD.get(i % 5 == 2 ? 5 : 4)); px(im, i, 8, WOOD.get(2));
        }
        px(im, 0, 0, WOOD.get(5)); px(im, 15, 0, WOOD.get(3)); px(im, 0, 15, WOOD.get(3)); px(im, 15, 15, WOOD.get(1));
        px(im, 0, 7, WOOD.get(3)); px(im, 15, 8, WOOD.get(1)); px(im, 0, 8, WOOD.get(3)); px(im, 15, 7, WOOD.get(3));
        // Twine lashings binding the cloth to the frame.
        int[][] ties = {{4, 0}, {11, 0}, {4, 15}, {11, 15}, {0, 3}, {15, 3}, {0, 12}, {15, 12}};
        for (int[] t : ties) {
            px(im, t[0], t[1], FIBRE.get(4));
            if (t[1] == 0 || t[1] == 15) px(im, t[0] + 1, t[1], FIBRE.get(2));
            else px(im, t[0], t[1] + 1, FIBRE.get(2));
        }
        for (int x : new int[] {4, 11}) { px(im, x, 7, FIBRE.get(4)); px(im, x + 1, 7, FIBRE.get(3)); px(im, x, 8, FIBRE.get(2)); }
        // A darned patch on the lower pane.
        px(im, 9, 11, SAILCLOTH.get(2)); px(im, 10, 11, SAILCLOTH.get(5)); px(im, 9, 12, SAILCLOTH.get(5)); px(im, 10, 12, SAILCLOTH.get(2));
        return im;
    }

    /** Windmill sail item: a framed sail panel, two cloth panes billowing, lashed at the corners. */
    static BufferedImage windmillSailItem() {
        BufferedImage im = img();
        // Cloth.
        for (int y = 2; y <= 13; y++)
            for (int x = 3; x <= 12; x++) {
                int top = y < 8 ? 2 : 8, bottom = y < 8 ? 7 : 13;
                int step = y == top + 1 ? 2 : y <= top + 2 ? 4 : 3;
                if (x == 12 || y == bottom - 1) step = Math.min(step, 2);
                if (step == 3 && Math.floorMod(x + (y % 2) * 3, 5) == 0) step = 4;
                px(im, x, y, SAILCLOTH.get(step));
            }
        // Frame: stiles, rails and the middle batten.
        for (int y = 1; y <= 14; y++) { px(im, 2, y, WOOD.get(4)); px(im, 13, y, WOOD.get(2)); }
        for (int x = 2; x <= 13; x++) {
            px(im, x, 1, WOOD.get(x == 2 ? 5 : 4)); px(im, x, 2, WOOD.get(2));
            px(im, x, 7, WOOD.get(4)); px(im, x, 8, WOOD.get(2));
            px(im, x, 13, WOOD.get(4)); px(im, x, 14, WOOD.get(2));
        }
        px(im, 2, 2, WOOD.get(3)); px(im, 13, 1, WOOD.get(3));
        // Twine at the joints.
        int[][] ties = {{2, 4}, {13, 4}, {2, 11}, {13, 11}};
        for (int[] t : ties) { px(im, t[0], t[1], FIBRE.get(t[0] == 2 ? 4 : 3)); px(im, t[0], t[1] + 1, FIBRE.get(2)); }
        return outline(im);
    }

    // Step-up gearbox (spec 20.2): front shows a small fast gear with hooked, arrow-like teeth; back a large gear.

    /** The output face's small gear (10 px, eight teeth), placed at columns and rows 3-12. */
    static final String[] SMALL_GEAR = {
            "....##....",
            ".#..##..#.",
            "..######..",
            "..######..",
            "##########",
            "##########",
            "..######..",
            "..######..",
            ".#..##..#.",
            "....##....",
    };

    static boolean[][] smallGearMask() {
        boolean[][] m = new boolean[16][16];
        for (int y = 0; y < 10; y++) for (int x = 0; x < 10; x++) m[y + 3][x + 3] = SMALL_GEAR[y].charAt(x) == '#';
        return m;
    }

    /** The input face's large gear: a 12 px body with eight teeth reaching the frame, like {@link #gearMask()} scaled up. */
    static boolean[][] largeGearMask() {
        boolean[][] m = new boolean[16][16];
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) if (Math.hypot(x - 7.5, y - 7.5) <= 5.6) m[y][x] = true;
        for (int i = 0; i < 2; i++)
            for (int j : new int[] {7, 8}) { m[1 + i][j] = true; m[13 + i][j] = true; m[j][1 + i] = true; m[j][13 + i] = true; }
        int[][] diag = {{3, 3}, {3, 4}, {4, 3}};
        for (int[] d : diag) { m[d[1]][d[0]] = true; m[d[1]][15 - d[0]] = true; m[15 - d[1]][d[0]] = true; m[15 - d[1]][15 - d[0]] = true; }
        return m;
    }

    /** Casing panel shared by all six step-up gearbox faces: boards in a pegged frame, iron L-brackets on every corner. */
    static BufferedImage stepUpPanel(long seed) {
        BufferedImage im = img();
        Random r = new Random(seed);
        grain(im, WOOD, r, 2, 2, 12, 12, 2, true);
        for (int y = 2; y < 14; y++) {
            px(im, 5, y, WOOD.get(1)); px(im, 6, y, WOOD.get(3));
            px(im, 10, y, WOOD.get(1)); px(im, 11, y, WOOD.get(3));
        }
        grain(im, WOOD, r, 0, 0, 16, 2, 3, false);
        grain(im, WOOD, r, 0, 14, 16, 2, 3, false);
        grain(im, WOOD, r, 0, 2, 2, 12, 3, true);
        grain(im, WOOD, r, 14, 2, 2, 12, 3, true);
        for (int i = 0; i < 16; i++) {
            px(im, i, 0, WOOD.get(4)); px(im, i, 15, WOOD.get(2));
            if (i > 1 && i < 14) { px(im, 0, i, WOOD.get(4)); px(im, 15, i, WOOD.get(2)); px(im, i, 2, WOOD.get(2)); px(im, 2, i, WOOD.get(2)); }
        }
        // Iron corner brackets: 5 px legs along both edges, 2 px wide, a rivet in each leg.
        for (int c = 0; c < 4; c++) {
            boolean right = c % 2 == 1, bottom = c >= 2;
            for (int i = 0; i < 5; i++)
                for (int j = 0; j < 2; j++) {
                    int hx = right ? 15 - i : i, hy = bottom ? 15 - j : j;   // leg along the top/bottom edge
                    int vx = right ? 15 - j : j, vy = bottom ? 15 - i : i;   // leg along the side edge
                    px(im, hx, hy, WROUGHT_IRON.get(hy == 0 || (hx == 0 && !bottom) ? 5 : hy == 15 ? 2 : j == 0 ? 4 : 3));
                    px(im, vx, vy, WROUGHT_IRON.get(vx == 0 ? 4 : vx == 15 ? 2 : j == 0 ? 4 : 3));
                }
            // Shadow at the bracket ends.
            px(im, right ? 10 : 5, bottom ? 14 : 1, WOOD.get(1));
            px(im, right ? 14 : 1, bottom ? 10 : 5, WOOD.get(1));
        }
        px(im, 0, 0, WROUGHT_IRON.get(5)); px(im, 15, 15, WROUGHT_IRON.get(1));
        for (int[] rv : new int[][] {{2, 0}, {0, 2}, {12, 0}, {14, 2}, {2, 14}, {0, 12}, {12, 14}, {14, 12}})
            { px(im, rv[0] + (rv[0] >= 12 ? 1 : 0), rv[1] + (rv[1] >= 12 ? 1 : 0), WROUGHT_IRON.get(5)); }
        return im;
    }

    /** Cuts a round opening into a panel and draws a gear in it, deep shadow in the gaps and the far wall lit. */
    static void gearWindow(BufferedImage im, double rHole, boolean[][] gear, int base, double rBoss) {
        boolean[][] hole = new boolean[16][16];
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) hole[y][x] = Math.hypot(x - 7.5, y - 7.5) <= rHole;
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                if (!hole[y][x] || gear[y][x]) continue;
                boolean farWall = !hole[y + 1][x] || !hole[y][x + 1];
                px(im, x, y, farWall ? WOOD.get(2) : DARK_WOOD.get(1));
            }
        boolean[][] g = new boolean[16][16];
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) g[y][x] = gear[y][x] && hole[y][x];
        shadeMask(im, g, WOOD, base);
        // Hub boss ring, square axle hole.
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double d = Math.hypot(x - 7.5, y - 7.5);
                if (g[y][x] && d > rBoss - 0.5 && d < rBoss + 0.5) px(im, x, y, WOOD.get(x + y < 15 ? base - 1 : base + 1));
            }
        px(im, 7, 7, DARK_WOOD.get(1)); px(im, 8, 7, DARK_WOOD.get(1)); px(im, 7, 8, DARK_WOOD.get(1)); px(im, 8, 8, DARK_WOOD.get(2));
        for (int y = 2; y < 14; y++)
            for (int x = 2; x < 14; x++)
                if (!hole[y][x] && ((y + 1 < 16 && hole[y + 1][x]) || (x + 1 < 16 && hole[y][x + 1]))) px(im, x, y, WOOD.get(4));
    }

    /** Output face: small opening, a small gear with six hooked teeth that lean the way it turns, so it reads "fast". */
    static BufferedImage stepUpFront() {
        BufferedImage im = stepUpPanel(9851);
        gearWindow(im, 4.6, smallGearMask(), 4, 0);
        // An iron collar round the small window, riveted on the diagonals.
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double d = Math.hypot(x - 7.5, y - 7.5);
                if (d > 4.6 && d <= 5.7) px(im, x, y, WROUGHT_IRON.get(x + y < 12 ? 5 : x + y < 19 ? 4 : 2));
                else if (d > 5.7 && d <= 6.3 && x + y >= 15) px(im, x, y, WOOD.get(1));   // the collar's shadow on the boards
                else if (d <= 4.6 && !smallGearMask()[y][x]) px(im, x, y, DARK_WOOD.get(1));
            }
        rivet(im, 4, 4); rivet(im, 10, 4); rivet(im, 4, 10); rivet(im, 10, 10);
        return im;
    }

    /** Input face: a big opening filled by a large twelve-tooth gear. */
    static BufferedImage stepUpBack() {
        BufferedImage im = stepUpPanel(9861);
        gearWindow(im, 6.6, largeGearMask(), 4, 3.4);
        // Lightening holes in the big gear's web.
        int[][] holes = {{5, 5}, {9, 5}, {5, 9}, {9, 9}};
        for (int[] h : holes) { px(im, h[0], h[1], DARK_WOOD.get(1)); px(im, h[0] + 1, h[1], DARK_WOOD.get(1)); px(im, h[0], h[1] + 1, DARK_WOOD.get(1)); px(im, h[0] + 1, h[1] + 1, WOOD.get(2)); }
        return im;
    }

    static BufferedImage stepUpSide() {
        BufferedImage im = stepUpPanel(9871);
        peg(im, WOOD, 7, 4); peg(im, WOOD, 7, 10);
        return im;
    }

    // Pulley and belt (spec 7.3).

    /** The pulley wheel's outline in plan: a 13 px square with 2 px notched corners (the model builds it from three boxes). */
    static boolean pulleyPlan(double x, double y) {
        boolean a = x >= 1.5 && x < 14.5 && y >= 3.5 && y < 12.5, b = x >= 3.5 && x < 12.5 && y >= 1.5 && y < 14.5;
        return a || b;
    }

    /**
     * Pulley face (the flat sides of the wheel): two boards glued edge to edge, an iron tyre round the notched rim,
     * an iron hub plate round the axle with four bolts.
     */
    static BufferedImage pulleyFace() {
        BufferedImage im = img();
        Random r = new Random(9881);
        grain(im, WOOD, r, 0, 0, 16, 16, 3, false);
        for (int x = 0; x < 16; x++) { px(im, x, 7, WOOD.get(1)); px(im, x, 8, WOOD.get(4)); }
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                if (!pulleyPlan(x + 0.5, y + 0.5)) { px(im, x, y, WOOD.get(2)); continue; }
                boolean rim = !pulleyPlan(x - 0.5, y + 0.5) || !pulleyPlan(x + 1.5, y + 0.5) || !pulleyPlan(x + 0.5, y - 0.5) || !pulleyPlan(x + 0.5, y + 1.5);
                if (rim) {
                    boolean lit = !pulleyPlan(x - 0.5, y + 0.5) || !pulleyPlan(x + 0.5, y - 0.5);
                    px(im, x, y, WROUGHT_IRON.get(lit ? 4 : 2));
                }
            }
        // Hub plate.
        for (int y = 4; y <= 11; y++)
            for (int x = 4; x <= 11; x++) {
                boolean edge = x == 4 || y == 4 || x == 11 || y == 11;
                if (!edge) continue;
                px(im, x, y, WROUGHT_IRON.get(x == 11 || y == 11 ? 2 : 4));
            }
        rivet(im, 5, 5); rivet(im, 9, 5); rivet(im, 5, 9); rivet(im, 9, 9);
        for (int y = 6; y <= 9; y++) for (int x = 6; x <= 9; x++) px(im, x, y, WOOD.get(x == 9 || y == 9 ? 2 : 3));
        px(im, 3, 1, WROUGHT_IRON.spec());
        return im;
    }

    /**
     * Pulley rim, seen side-on. Rows 5-10 are what the model shows (wheel y 5 to 11): the upper flange's iron tyre,
     * the groove (iron-lined, polished in the middle where the belt runs), the lower flange's tyre. The other rows
     * repeat the profile so the texture still reads as an iron-shod rim.
     */
    static BufferedImage pulleySide() {
        BufferedImage im = img();
        int[] profile = {5, 3, 2, 3, 4, 2}; // tyre lit edge, tyre, groove shadow, polished track, groove far wall, tyre underside
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int k = Math.floorMod(y - 5, 6);
                int step = profile[k];
                if (k == 1 && x % 5 == 2) step = 4;         // nail heads on the tyre
                if (k == 3 && x % 7 == 4) step = 5;          // belt-polished glints
                if (k == 2 && x % 6 == 1) step = 1;
                px(im, x, y, WROUGHT_IRON.get(step));
            }
        for (int x = 0; x < 16; x++) if (x % 5 == 2) px(im, x + 1, 6, WROUGHT_IRON.get(2));
        return im;
    }

    /**
     * Leather belt strip: four 4 px straps side by side so the model's x 6-10 shows exactly one strap (columns 6-9),
     * and the whole texture still reads as belting if drawn full width. Each strap: lit edge, a row of saddle
     * stitches, a worn body and a dark edge. Tiles along v.
     */
    static BufferedImage leatherBelt() {
        BufferedImage im = img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int k = Math.floorMod(x - 6, 4);
                int step = k == 0 ? 4 : k == 3 ? 2 : 3;
                if (k == 2 && Math.floorMod(y * 3 + x, 7) == 0) step = 4;   // grain scuffs
                if (k == 1 && Math.floorMod(y, 4) == 2) step = 2;           // stitch hole
                if (k == 1 && (Math.floorMod(y, 4) == 0 || Math.floorMod(y, 4) == 1)) step = 5; // thread
                px(im, x, y, LEATHER.get(step));
            }
        // A riveted splice where two belt lengths are joined.
        for (int x = 6; x <= 9; x++) px(im, x, 12, LEATHER.get(1));
        px(im, 7, 13, WROUGHT_IRON.get(4)); px(im, 8, 13, WROUGHT_IRON.get(2));
        return im;
    }

    /** Pulley item, three-quarter view: grooved rim stepping back to the lower right, plank face with iron tyre and hub. */
    static BufferedImage pulleyItem() {
        BufferedImage im = img();
        double cx = 6.5, cy = 6.5, rad = 5.4;
        int[][] rimSteps = {{3, 2}, {2, 1}, {1, 3}};   // offset, iron step: far tyre, groove, near tyre
        for (int[] s : rimSteps)
            for (int y = 0; y < 16; y++)
                for (int x = 0; x < 16; x++) {
                    double d = Math.hypot(x - cx - s[0], y - cy - s[0]);
                    if (d <= rad) px(im, x, y, WROUGHT_IRON.get(s[1] == 1 ? 1 : (x + y > cx + cy + 2 * s[0] + 3 ? 2 : s[1] + 1)));
                }
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double d = Math.hypot(x - cx, y - cy);
                if (d > rad) continue;
                int c;
                if (d > rad - 1.0) c = WROUGHT_IRON.get(x + y < cx + cy - 2 ? 5 : x + y < cx + cy + 3 ? 4 : 3);
                else {
                    int step = (y == 6) ? 1 : (y == 7) ? 4 : 3;
                    if ((x * 5 + y * 3) % 7 == 0 && step == 3) step = 4;
                    c = WOOD.get(step);
                }
                px(im, x, y, c);
            }
        // Hub and axle hole.
        for (int y = 5; y <= 8; y++)
            for (int x = 5; x <= 8; x++) px(im, x, y, WROUGHT_IRON.get(x == 8 || y == 8 ? 2 : (x == 5 || y == 5) ? 4 : 3));
        px(im, 6, 6, DARK_WOOD.get(1)); px(im, 7, 6, DARK_WOOD.get(2)); px(im, 6, 7, DARK_WOOD.get(2)); px(im, 7, 7, DARK_WOOD.get(1));
        px(im, 3, 3, WROUGHT_IRON.spec());
        return outline(im);
    }

    /** Leather belt item: a coiled strap with a stitched edge, a twine tie and the iron buckle on the loose end. */
    static BufferedImage leatherBeltItem() {
        BufferedImage im = img();
        double cx = 6.5, cy = 6.5;
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double dx = x - cx, dy = y - cy, d = Math.hypot(dx, dy);
                if (d > 5.6 || d < 1.4) continue;
                double turn = (Math.atan2(dy, dx) / (2 * Math.PI) + 1) % 1.0;
                // Three wraps of strap, each a 1 px gap then 1 px of strap face; the outer wrap ends in the tail.
                double f = ((d - 1.4 + turn * 0.6) / 2.0) % 1.0;
                int step = f < 0.42 ? 1 : f < 0.7 ? 4 : 3;
                if (step == 3 && dx + dy < -1) step = 4;
                if (step == 4 && dx + dy > 2) step = 3;             // the lower right of the coil turns away from the light
                if (step == 3 && dx + dy < -4) step = 4;
                px(im, x, y, LEATHER.get(step));
            }
        // Hole in the middle.
        for (int y = 5; y <= 8; y++) for (int x = 5; x <= 8; x++) if (Math.hypot(x - cx, y - cy) < 1.4) clear(im, x, y);
        // Loose end running out to the lower right, with the buckle.
        int[][] tail = {{10, 10}, {11, 10}, {11, 11}, {12, 11}, {12, 12}, {13, 12}};
        for (int[] t : tail) { px(im, t[0], t[1], LEATHER.get(3)); px(im, t[0], t[1] + 1, LEATHER.get(2)); }
        px(im, 11, 10, LEATHER.get(4)); px(im, 12, 11, LEATHER.get(4));
        int[][] buckle = {{12, 12}, {13, 12}, {14, 12}, {12, 13}, {14, 13}, {12, 14}, {13, 14}, {14, 14}};
        for (int[] b : buckle) px(im, b[0], b[1], WROUGHT_IRON.get(b[1] == 12 || b[0] == 12 ? 4 : 2));
        px(im, 13, 13, LEATHER.get(2)); px(im, 12, 12, WROUGHT_IRON.get(5));
        // Twine tie across the coil.
        for (int y = 1; y <= 12; y++) {
            int x = 3 + (y + 1) / 4;
            if (opaque(im, x, y)) px(im, x, y, FIBRE.get(y % 2 == 0 ? 4 : 3));
        }
        return outline(im);
    }

    // Tanning (spec 12.1).

    /** One outstretched hide (four legs, neck and tail), its lower-right corner folded back over along x + y = 19. */
    static final String[] HIDE_SHAPE = {
            "................",
            "..##........##..",
            "..###..##..###..",
            "...##########...",
            "...##########...",
            "..############..",
            ".##############.",
            ".##############.",
            "..############..",
            "..############..",
            "...##########...",
            "...##########...",
            "..###..##..###..",
            "..##....#...##..",
            "................",
            "................",
    };

    /** Region map of the folded hide: 0 outside, 1 outer (hair) side, 2 the folded flap showing the flesh side, 3 the fold crease. */
    static int[][] hideRegions() {
        int[][] m = new int[16][16];
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++)
                if (HIDE_SHAPE[y].charAt(x) == '#' && x + y <= 19) m[y][x] = 1;
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++)
                if (HIDE_SHAPE[y].charAt(x) == '#' && x + y > 19) {
                    int fx = 19 - y, fy = 19 - x; // mirror across the fold line
                    if (fx >= 0 && fy >= 0 && fx < 16 && fy < 16) m[fy][fx] = 2;
                }
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) if (m[y][x] != 0 && x + y == 19) m[y][x] = 3;
        return m;
    }

    /**
     * The three hide stages share {@link #HIDE_SHAPE}: raw has short hair strokes and a few tufts sticking out of the
     * outline; limed is swollen and chalky with lime blotches; scraped is smooth with long scraper strokes.
     */
    static BufferedImage hideItem(int stage) {
        BufferedImage im = img();
        int[][] m = hideRegions();
        Ramp outer = stage == 0 ? HIDE : stage == 1 ? LIMED : SCRAPED;
        double[][] n = fractal(9900 + stage);
        Random r = new Random(9910 + stage);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                if (m[y][x] == 0) continue;
                boolean litEdge = (y > 0 && m[y - 1][x] == 0) || (x > 0 && m[y][x - 1] == 0);
                boolean darkEdge = (y < 15 && m[y + 1][x] == 0) || (x < 15 && m[y][x + 1] == 0);
                int step;
                if (m[y][x] == 1) {
                    step = n[y][x] > 0.6 ? 4 : n[y][x] < 0.38 ? 2 : 3;
                    if (stage == 1 && n[y][x] > 0.52) step = 4;
                    if (litEdge) step = Math.min(5, step + 1);
                    if (darkEdge) step = Math.max(1, step - 1);
                    px(im, x, y, outer.get(step));
                } else if (m[y][x] == 2) {
                    // Flesh side: paler and smoother than the outside, darker toward the free edge.
                    step = darkEdge ? 3 : 4;
                    if (stage == 2 && !darkEdge && (x + y) % 5 == 0) step = 5;
                    Ramp flesh = stage == 1 ? LIMED : SCRAPED;
                    px(im, x, y, flesh.get(stage == 2 ? step : Math.min(5, step + 1)));
                } else {
                    // The crease: a lit roll on the fold, shadow on the flap side of it.
                    px(im, x, y, outer.get(5));
                }
            }
        // The flap's free edge casts a shadow on the hide beside it.
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++)
                if (m[y][x] == 1 && ((y > 0 && m[y - 1][x] == 2) || (x > 0 && m[y][x - 1] == 2))) px(im, x, y, outer.get(1));
        if (stage == 0) {
            // Hair: short dark strokes lying toward the lower right, a few light ones, and tufts on the outline.
            for (int i = 0; i < 26; i++) {
                int x = 1 + r.nextInt(14), y = 1 + r.nextInt(13);
                if (m[y][x] != 1 || y + 1 > 15 || m[y + 1][x] != 1) continue;
                boolean dark = i % 3 != 0;
                px(im, x, y, HIDE.get(dark ? 2 : 4));
                px(im, x, y + 1, HIDE.get(dark ? 1 : 3));
            }
            int[][] tufts = {{1, 5}, {4, 2}, {7, 1}, {14, 5}, {1, 9}, {3, 13}, {10, 1}};
            for (int[] t : tufts) if (!opaque(im, t[0], t[1])) px(im, t[0], t[1], HIDE.get(2));
        } else if (stage == 1) {
            // Lime: chalky white blotches in 2-3 px clusters, and swollen pores.
            int[][] blots = {{4, 4}, {9, 3}, {6, 8}, {3, 10}, {11, 6}, {8, 11}, {5, 6}};
            for (int[] b : blots) {
                if (m[b[1]][b[0]] != 1) continue;
                px(im, b[0], b[1], LIMED.get(5));
                if (m[b[1]][b[0] + 1] == 1) px(im, b[0] + 1, b[1], LIMED.get(5));
                if (m[b[1] + 1][b[0]] == 1) px(im, b[0], b[1] + 1, LIMED.get(4));
            }
            int[][] pores = {{7, 5}, {4, 8}, {10, 9}, {6, 12}};
            for (int[] p : pores) if (m[p[1]][p[0]] == 1) px(im, p[0], p[1], LIMED.get(2));
        } else {
            // Scraper strokes: long diagonal light streaks, clean surface.
            for (int s = 0; s < 4; s++) {
                int x0 = 2 + s * 3, y0 = 3 + (s % 2) * 2;
                for (int k = 0; k < 4; k++) {
                    int x = x0 + k, y = y0 + k;
                    if (x < 16 && y < 16 && m[y][x] == 1) px(im, x, y, SCRAPED.get(k == 0 ? 5 : 4));
                }
            }
        }
        return outline(im);
    }

    // Soaking barrel (spec 12.1, 20.2).

    /** Barrel side: four staves with dark joints, lit left edges, two iron hoops (rows 3-4, 11-12) riveted at each stave. */
    static BufferedImage barrelSide() {
        BufferedImage im = img();
        Random r = new Random(9921);
        grain(im, WOOD, r, 0, 0, 16, 16, 3, true);
        for (int y = 0; y < 16; y++)
            for (int s : new int[] {1, 5, 9, 13}) {
                px(im, s, y, WOOD.get(4));
                px(im, Math.floorMod(s - 1, 16), y, WOOD.get(1));
            }
        for (int x = 0; x < 16; x++) { px(im, x, 0, WOOD.get(4)); px(im, x, 15, WOOD.get(2)); }
        for (int hy : new int[] {3, 11}) {
            for (int x = 0; x < 16; x++) {
                px(im, x, hy, WROUGHT_IRON.get((x * 5 + hy) % 7 == 0 ? 5 : 4));
                px(im, x, hy + 1, WROUGHT_IRON.get(x % 4 == 0 ? 2 : 3));
                px(im, x, hy + 2, WOOD.get(1));
            }
            for (int s : new int[] {2, 10}) { px(im, s, hy, WROUGHT_IRON.get(5)); px(im, s + 1, hy, WROUGHT_IRON.get(2)); px(im, s, hy + 1, WROUGHT_IRON.get(1)); }
        }
        // Damp streak running down from the rim and a knot.
        px(im, 7, 1, WOOD.get(2)); px(im, 7, 2, WOOD.get(2)); px(im, 7, 6, WOOD.get(2)); px(im, 7, 7, WOOD.get(2));
        px(im, 11, 8, WOOD.get(1)); px(im, 12, 8, WOOD.get(2)); px(im, 11, 9, WOOD.get(2));
        return im;
    }

    /** Open top: the stave ends round the 1 px rim (x, z 1 to 15) with joints every 4 px; inside, the shadowed well. */
    static BufferedImage barrelTopOpen() {
        BufferedImage im = img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                boolean rim = x >= 1 && x <= 14 && y >= 1 && y <= 14 && (x == 1 || x == 14 || y == 1 || y == 14);
                if (!rim) { px(im, x, y, DARK_WOOD.get(x <= 2 || y <= 2 ? 1 : 2)); continue; }
                int step = (x == 1 || y == 1) ? 5 : 3;                     // lit near rim, shaded far rim
                if ((x == 1 || x == 14) && y % 4 == 0) step = 2;            // joints between staves
                if ((y == 1 || y == 14) && x % 4 == 0) step = 2;
                px(im, x, y, WOOD.get(step));
            }
        return im;
    }

    /** Inside of the staves and the barrel floor: dark, damp wood, a tide line near the top. */
    static BufferedImage barrelInner() {
        BufferedImage im = img();
        Random r = new Random(9931);
        grain(im, DARK_WOOD, r, 0, 0, 16, 16, 2, true);
        for (int y = 0; y < 16; y++)
            for (int s : new int[] {3, 7, 11, 15}) px(im, s, y, DARK_WOOD.get(1));
        for (int x = 0; x < 16; x++) { px(im, x, 0, DARK_WOOD.get(3)); if (x % 4 != 3) px(im, x, 1, DARK_WOOD.get(x % 3 == 0 ? 4 : 3)); }
        return im;
    }

    /** A barrel head: three boards across, a dark chime groove round them, two nails per board. {@code lid} is paler and has a cleat strip in rows 0-1. */
    static BufferedImage barrelHead(long seed, boolean lid) {
        BufferedImage im = img();
        Random r = new Random(seed);
        grain(im, WOOD, r, 0, 0, 16, 16, lid ? 4 : 3, false);
        for (int b : new int[] {3, 6, 9, 12}) for (int x = 0; x < 16; x++) { px(im, x, b, WOOD.get(1)); px(im, x, b + 1, WOOD.get(lid ? 5 : 4)); }
        for (int i = 2; i < 14; i++) {
            px(im, i, 2, WOOD.get(2)); px(im, 2, i, WOOD.get(2));
            px(im, i, 13, WOOD.get(4)); px(im, 13, i, WOOD.get(4));
        }
        for (int bx : new int[] {4, 11}) for (int by : new int[] {5, 8, 11}) px(im, bx, by, WROUGHT_IRON.get(lid ? 4 : 2));
        if (lid) {
            // The cleat used as a handle bar (model uses rows 0-1 for its faces): darker, polished by hands.
            for (int x = 0; x < 16; x++) {
                px(im, x, 0, DARK_WOOD.get(x % 5 == 1 ? 5 : 4));
                px(im, x, 1, DARK_WOOD.get(x % 4 == 2 ? 2 : 3));
            }
        }
        return im;
    }

    /** Samples a 16x16 periodic field with bilinear interpolation. */
    static double sampleWrap(double[][] n, double x, double y) {
        int x0 = (int) Math.floor(x), y0 = (int) Math.floor(y);
        double tx = x - x0, ty = y - y0;
        double a = n[Math.floorMod(y0, 16)][Math.floorMod(x0, 16)], b = n[Math.floorMod(y0, 16)][Math.floorMod(x0 + 1, 16)];
        double c = n[Math.floorMod(y0 + 1, 16)][Math.floorMod(x0, 16)], d = n[Math.floorMod(y0 + 1, 16)][Math.floorMod(x0 + 1, 16)];
        return lerp(lerp(a, b, tx), lerp(c, d, tx), ty);
    }

    /**
     * Barrel fluid, an animated strip. Still: 32 frames, two noise layers drifting slowly at right angles (each moves
     * exactly 16 px per loop, so it loops seamlessly). Flow: 16 frames, streaks stretched along v moving 1 px a frame.
     * Lye is milky and cloudy (alpha 0xd8 to 0xe8); tannin nearly opaque (alpha 0xf0) with darker flecks drifting.
     */
    static BufferedImage barrelFluid(Ramp a, boolean flow, boolean milky, long seed) {
        int frames = flow ? 16 : 32;
        BufferedImage im = new BufferedImage(16, 16 * frames, BufferedImage.TYPE_INT_ARGB);
        Random r = new Random(seed);
        double p1 = r.nextDouble(), p2 = r.nextDouble();
        int[][] specks = new int[7][2];
        for (int[] sp : specks) { sp[0] = r.nextInt(16); sp[1] = r.nextInt(16); }
        double tau = 2 * Math.PI;
        for (int f = 0; f < frames; f++) {
            double t = (double) f / frames;
            for (int y = 0; y < 16; y++)
                for (int x = 0; x < 16; x++) {
                    double u = x / 16.0, w = y / 16.0, v;
                    // Smooth, domain-warped swirls; integer frequencies keep the tile and the loop seamless.
                    if (flow) {
                        double wf = w - t;
                        v = 0.5 + 0.26 * Math.sin(tau * (2 * u + p1) + 1.4 * Math.sin(tau * (wf + p2))) + 0.16 * Math.sin(tau * (3 * u + wf));
                    } else {
                        v = 0.5 + 0.24 * Math.sin(tau * (u + w + p1) + 1.2 * Math.sin(tau * (w - t)))
                                + 0.18 * Math.sin(tau * (2 * u - w + t + p2) + Math.sin(tau * (u + t)));
                    }
                    int step;
                    if (milky) step = v < 0.36 ? 3 : v < 0.64 ? 4 : 5;            // cloudy: soft swirls, little contrast
                    else step = v < 0.34 ? 2 : v < 0.68 ? 3 : 4;                // tea: dark body, a dull sheen on the swirls
                    if (!milky && v < 0.16) step = 1;
                    if (milky && v < 0.14) step = 2;
                    int alpha = milky ? (step >= 4 ? 0xe8 : 0xd8) : 0xf0;
                    im.setRGB(x, f * 16 + y, alpha << 24 | a.get(step));
                }
            // Drifting specks (dark flecks of bark in tannin, undissolved lime in lye); each moves a whole tile per loop.
            for (int i = 0; i < specks.length; i++) {
                int sx = Math.floorMod(specks[i][0] + (flow ? 0 : (int) Math.round(t * 16 * (i % 2 == 0 ? 1 : -1))), 16);
                int sy = Math.floorMod(specks[i][1] + (int) Math.round(t * 16 * (flow ? 1 : (i % 3 == 0 ? 1 : 0))), 16);
                int c = milky ? a.get(i % 2 == 0 ? 5 : 2) : a.get(i % 2 == 0 ? 1 : 4);
                im.setRGB(sx, f * 16 + sy, (milky ? 0xe8 : 0xf0) << 24 | c);
            }
        }
        return im;
    }

    /** The outline of a '#' shape (every '#' with a 4-neighbour outside it), for ghost icons. */
    static String[] edgeRows(String[] rows) {
        String[] out = new String[rows.length];
        for (int y = 0; y < rows.length; y++) {
            StringBuilder sb = new StringBuilder();
            for (int x = 0; x < rows[y].length(); x++) {
                boolean in = rows[y].charAt(x) == '#';
                boolean edge = in && (y == 0 || x == 0 || y == rows.length - 1 || x == rows[y].length() - 1
                        || rows[y - 1].charAt(x) != '#' || rows[y + 1].charAt(x) != '#' || rows[y].charAt(x - 1) != '#' || rows[y].charAt(x + 1) != '#');
                sb.append(edge ? '#' : '.');
            }
            out[y] = sb.toString();
        }
        return out;
    }

    /** The soaking barrel's progress arrow, 24x17: a 5 px shaft and a head tapering to a point at the right. */
    static boolean barrelArrow(int i, int j) {
        int d = Math.abs(j - 8);
        return (i < 15 && d <= 2) || (i >= 15 && i < 24 - d);
    }

    /** A 16x52 fill strip: vertical streaks plus a short repeating ripple, so any bottom part of it reads as liquid. */
    static void fillStrip(BufferedImage im, int u, int[] c, int alpha) {
        for (int y = 0; y < 52; y++)
            for (int x = 0; x < 16; x++) {
                int k = 2;
                if (x % 5 == 1 || x % 7 == 4) k = 3;                   // streaks
                if (x % 6 == 3) k = 1;
                if ((x + y * 2) % 11 == 0) k = Math.min(4, k + 1);      // ripples
                if (x == 0) k = 1;
                if (x == 15) k = 1;
                im.setRGB(u + x, y, alpha << 24 | c[k]);
            }
    }

    /**
     * Soaking barrel screen, 176x166. Tank well: inner (12,17) 16x52, frame (11,16) 18x54, engraved 1000 mB ticks
     * left of it at x 7-10, y 30/43/56 (2000 mB tick longer). Input slot frame (43,26), output (115,26), empty
     * progress arrow (76,26) 24x17. Kept clear: status line y 56 right of the tank, labels at y 6 (x 34) and y 73.
     * Sprites: fill strips 16x52 at v 0 (water u 176, lye u 192, tannin u 208, generic grey u 224), filled
     * arrow at u 176, v 56 (24x17).
     */
    static BufferedImage soakingBarrelGui() {
        BufferedImage im = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        panel(im, 176, 166);
        well(im, 11, 16, 18, 54, 0x2a2a2a);
        int[] ticks = {56, 43, 30};
        for (int i = 0; i < 3; i++) {
            int x0 = i == 1 ? 6 : 7;
            fill(im, x0, ticks[i], 11 - x0, 1, GUI_SHADOW);
            fill(im, x0, ticks[i] + 1, 11 - x0, 1, GUI_LIGHT);
        }
        slot(im, 44, 27);
        slot(im, 116, 27);
        ghost(im, 44, 27, edgeRows(HIDE_SHAPE));
        for (int j = 0; j < 17; j++)
            for (int i = 0; i < 24; i++)
                if (barrelArrow(i, j)) {
                    im.setRGB(76 + i, 26 + j, 0xff000000 | SLOT_FILL);
                    im.setRGB(176 + i, 56 + j, 0xff000000 | GUI_LIGHT);
                }
        inventory(im);
        int[] water = {0x2c5cb8, 0x3466cc, 0x3f76e4, 0x5a8aec, 0x7aa4f0};
        int[] lye = {LYE.get(2), LYE.get(3), LYE.get(4), LYE.get(5), LYE.get(5)};
        int[] tannin = {TANNIN.get(1), TANNIN.get(2), TANNIN.get(3), TANNIN.get(4), TANNIN.get(5)};
        int[] grey = {0xa8a8a8, 0xc0c0c0, 0xd8d8d8, 0xececec, 0xffffff};
        fillStrip(im, 176, water, 0xff);
        fillStrip(im, 192, lye, 0xff);
        fillStrip(im, 208, tannin, 0xff);
        fillStrip(im, 224, grey, 0xff);
        return im;
    }

    static void windAndTanning() throws IOException {
        save("block/windmill_bearing_side", bearingSide());
        save("block/windmill_bearing_top", transpose(bearingSide()));
        save("block/windmill_bearing_front", bearingFront());
        save("block/windmill_bearing_back", bearingBack());
        save("block/windmill_hub", windmillHub());
        save("block/windmill_sail", windmillSail());
        save("item/windmill_sail", windmillSailItem());
        save("block/step_up_gearbox_front", stepUpFront());
        save("block/step_up_gearbox_back", stepUpBack());
        save("block/step_up_gearbox_side", stepUpSide());
        save("block/pulley_face", pulleyFace());
        save("block/pulley_side", pulleySide());
        save("block/leather_belt", leatherBelt());
        save("item/pulley", pulleyItem());
        save("item/leather_belt", leatherBeltItem());
        save("item/raw_hide", hideItem(0));
        save("item/limed_hide", hideItem(1));
        save("item/scraped_hide", hideItem(2));
        save("block/soaking_barrel_side", barrelSide());
        save("block/soaking_barrel_top_open", barrelTopOpen());
        save("block/soaking_barrel_inner", barrelInner());
        save("block/soaking_barrel_bottom", barrelHead(9941, false));
        save("block/soaking_barrel_top_sealed", barrelHead(9951, true));
        saveAnimated("block/lye_still", barrelFluid(LYE, false, true, 9961), 3);
        saveAnimated("block/lye_flow", barrelFluid(LYE, true, true, 9961), 2);
        saveAnimated("block/tannin_still", barrelFluid(TANNIN, false, false, 9971), 3);
        saveAnimated("block/tannin_flow", barrelFluid(TANNIN, true, false, 9971), 2);
        saveRaw("gui/soaking_barrel", soakingBarrelGui());
    }

    // ---------------------------------------------------------------- output

    // ---------------------------------------------------------------- tier 4: steel and steam (spec 21)

    // Metal ramps (tier 4 spec 21.1).
    static final Ramp STEEL = ramp(0xd4d8da, 0x30343c, 0x4e535c, 0x6e737c, 0x8e939c, 0xaeb3ba);
    static final Ramp PIG_IRON = ramp(0xb0a8a0, 0x302c2a, 0x48423e, 0x625b54, 0x7c746c, 0x988f86);
    static final Ramp BRASS = ramp(0xfff0b8, 0x4a3414, 0x7c5a22, 0xa88232, 0xcca64a, 0xe8c868);
    static final Ramp ZINC = ramp(0xe8eef0, 0x383c44, 0x585e66, 0x7c848c, 0xa0a8ae, 0xc6cdd1);
    static final Ramp LEAD = ramp(0xb4bccc, 0x2c303c, 0x424858, 0x5c647a, 0x7a8498, 0x9aa4b6);
    static final Ramp SOLDER = ramp(0xe6eae6, 0x3c4042, 0x5a6064, 0x7e8588, 0xa4aaa8, 0xc8cec8);
    // Ore ramps (worldgen spec 14.1): sulfur stays a muted green-yellow, well off the heat band.
    static final Ramp BITUMINOUS_COAL = ramp(0, 0x121318, 0x1e2027, 0x2c2e37, 0x3e414c, 0x565a66);
    static final Ramp SPHALERITE = ramp(0, 0x1c1210, 0x2e1c14, 0x4a2e1a, 0x6e4622, 0x946632);
    static final Ramp GALENA = ramp(0xd0d6e0, 0x1e2028, 0x343844, 0x525866, 0x767d8c, 0x9ea6b4);
    static final Ramp SULFUR = ramp(0, 0x3c3820, 0x5c5630, 0x827a40, 0xa69c54, 0xc6bc72);

    // Tier 5 ores (tier 5 spec 18): cinnabar is a cool vermilion, lazurite a deep ultramarine with pale flecks.
    static final Ramp ORE_CINNABAR = ramp(0xf0b8a8, 0x3c1214, 0x6a1c1e, 0x9c2a24, 0xc83c2e, 0xe8644a);
    static final Ramp ORE_LAZURITE = ramp(0xd8e0f4, 0x141c4a, 0x1f2f78, 0x2e46a4, 0x4468cc, 0x7a98e6);
    static final List<Mineral> T5_MINERALS = List.of(
            new Mineral("cinnabar", ORE_CINNABAR, true),
            new Mineral("lazurite", ORE_LAZURITE, false));

    /** Ore overlays and small ore pebbles for cinnabar and lazurite. */
    static void tier5Ores() throws IOException {
        for (Mineral m : T5_MINERALS) {
            for (String grade : List.of("poor", "normal", "rich")) save("block/ore/" + m.name() + "_" + grade, oreOverlay(m, grade));
            save("block/small_" + m.name(), pebbles(m));
        }
    }

    static final List<Mineral> T4_MINERALS = List.of(
            new Mineral("sphalerite", ORE_SPHALERITE, false),
            new Mineral("galena", ORE_GALENA, true),
            new Mineral("bituminous_coal", ORE_COAL, false),
            new Mineral("sulfur", ORE_SULFUR, false));

    /** A soft conical heap of powder (coke, charcoal, slag and sulfur dust). */
    static final String[] DUST_HEAP = {
            "....45.....",
            "...4544....",
            "..444443...",
            ".44443332..",
            "4444333322.",
            "43333322221",
            ".222221111.",
    };
    /** A muted yellow crystal lump: angular faces, lit top-left. */
    static final String[] SULFUR_LUMP = {
            ".....5......",
            "....454..5..",
            "...44543454.",
            "..4445434433",
            ".44443343342",
            "444433333322",
            "433333322221",
            ".3322222211.",
            "..1111111...",
    };

    /** An eight-tooth gear with a bored centre, lit from the top-left, one specular pixel on the upper-left teeth. */
    static BufferedImage gear(Ramp a) {
        BufferedImage im = img();
        double cx = 7.5, cy = 7.5;
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double dx = x - cx, dy = y - cy, r = Math.sqrt(dx * dx + dy * dy);
                double ang = Math.atan2(dy, dx);
                // Teeth: eight lobes reaching from the rim (r 4.6) out to r 6.4.
                double tooth = Math.cos(ang * 8);
                boolean body = r <= 4.7 || (r <= 6.5 && tooth > 0.35);
                if (!body || r < 1.6) continue;
                double light = -(dx + dy) / Math.max(r, 0.01);
                int step = light > 0.45 ? 4 : light < -0.45 ? 2 : 3;
                if (r < 2.6) step = light > 0 ? 2 : 4; // the bore's inner wall is lit from the other side
                if (r > 5.3 && light > 0.6) step = 5;
                px(im, x, y, a.get(step));
            }
        if (a.spec() != 0) px(im, 4, 3, a.spec());
        return outline(im);
    }

    /** The shared ingot with a rough, sand-cast top: some lit pixels sink a step. */
    static String[] sandCast(String[] rows) {
        Random r = new Random(4040);
        String[] out = new String[rows.length];
        for (int y = 0; y < rows.length; y++) {
            char[] row = rows[y].toCharArray();
            for (int x = 0; x < row.length; x++)
                if ((row[x] == '4' || row[x] == '5' || row[x] == 's') && r.nextInt(3) == 0) row[x] = '3';
            out[y] = new String(row);
        }
        return out;
    }

    // Coke oven ramps (spec 21.1).
    static final Ramp COKE = ramp(0, 0x18181c, 0x2a2a30, 0x3e3e46, 0x56565e, 0x727078);
    static final Ramp COKE_OVEN_BRICK = ramp(0, 0x4c342c, 0x674238, 0x84564a, 0xa06c58, 0xb8846c);
    static final Ramp TREATED_WOOD = ramp(0, 0x2c1e10, 0x45301a, 0x5e4424, 0x7a5a30, 0x967642);
    static final Ramp CREOSOTE = ramp(0, 0x140c08, 0x24160c, 0x382212, 0x4c301a, 0x624024);

    /** Fired fire clay: a pale buff, warmer than the grey unfired clay so the two read apart (spec 21.1, adjusted). */
    static final Ramp REFRACTORY = ramp(0, 0x5c4c3c, 0x7c6a54, 0x9c886c, 0xb8a486, 0xd0bea0);

    /** A gear cavity: eight teeth round a raised hub. */
    static final String[] GEAR_CAVITY = {
            "..#..#..",
            ".######.",
            "###..###",
            "###..###",
            ".######.",
            "..#..#..",
    };

    /** Refractory crucible wall: smooth fire clay with faint coil lines, sooted where it sits in the coals. */
    static BufferedImage refractoryCrucibleSide() {
        double[][] n = V2.grain(8383);
        BufferedImage im = img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int step = n[y][x] > 0.62 ? 4 : 3;
                if (y % 4 == 3 && (x + y) % 6 != 0) step = 3 - (n[y][x] < 0.42 ? 1 : 0);
                int c = REFRACTORY.get(step);
                if (y > 12 + (int) Math.round(n[y][x] * 2)) c = COKE.get(n[y][x] > 0.5 ? 4 : 3);
                px(im, x, y, c);
            }
        return im;
    }

    static BufferedImage refractoryCrucibleTop() {
        BufferedImage im = img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) px(im, x, y, REFRACTORY.get((x < 3 || y < 3) ? 5 : 4));
        return im;
    }

    /** Inside: glazed dark by heat, with glassy drips of iron slag. */
    static BufferedImage refractoryCrucibleInside() {
        double[][] n = V2.grain(9494);
        BufferedImage im = img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) px(im, x, y, REFRACTORY.get(n[y][x] > 0.55 ? 2 : 1));
        Random r = new Random(9494);
        for (int k = 0; k < 4; k++) speck(im, r, r.nextInt(16), r.nextInt(16), SLAG.get(4), PIG_IRON.get(1), 2);
        return im;
    }

    static BufferedImage refractoryPlain(long seed) {
        double[][] n = V2.grain(seed);
        BufferedImage im = img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) px(im, x, y, REFRACTORY.get(n[y][x] > 0.55 ? 3 : 2));
        return im;
    }

    /** Vertical joints per course of the large coke oven bricks: three courses, bricks 7-9 px long. */
    static final int[][] COKE_OVEN_JOINTS = {{3, 11}, {7, 15}, {0, 8}};
    static final int[] COKE_OVEN_COURSE_TOP = {0, 5, 10}, COKE_OVEN_COURSE_H = {5, 5, 6};

    /**
     * Large dark-red refractory bricks in three courses, thick dark mortar, the top edge of every brick
     * sooted (steps 1-2) where smoke has crept out of the joints (spec 21.2).
     */
    static BufferedImage cokeOvenBricks() {
        BufferedImage im = img();
        Random r = new Random(5151);
        for (int c = 0; c < 3; c++) {
            int top = COKE_OVEN_COURSE_TOP[c], h = COKE_OVEN_COURSE_H[c] - 1;
            int[] j = COKE_OVEN_JOINTS[c];
            for (int b = 0; b < j.length; b++) {
                int start = j[b] + 1, w = Math.floorMod(j[(b + 1) % j.length] - start, 16);
                int tone = r.nextInt(3) == 0 ? -1 : 0;
                for (int row = 0; row < h; row++)
                    for (int i = 0; i < w; i++) {
                        int step = 4 + tone;
                        if (row == h - 1 || i == w - 1) step = 3 + tone;          // shaded bottom and right edges
                        if (row == 1 && i == 0) step = 5 + tone;                  // lit corner below the soot
                        if (r.nextInt(7) == 0) step--;                             // pitting
                        // Soot: the top row is always dark, the second row patchily so.
                        if (row == 0) step = r.nextInt(4) == 0 ? 1 : 2;
                        else if (row == 1 && r.nextInt(3) == 0) step = 2;
                        pxWrap(im, start + i, top + row, COKE_OVEN_BRICK.get(clampStep(step)));
                    }
                for (int y = 0; y <= h; y++) pxWrap(im, j[b], top + y, COKE.get(1));
            }
            for (int x = 0; x < 16; x++) px(im, x, top + h, COKE.get(1));
        }
        return im;
    }

    /**
     * The coke oven door, 10x14 on the brick face: a riveted wrought iron leaf with a strap across it,
     * a latch and a peephole. 'g' is the seam round the leaf, 'p' the peephole's rim, 'P' the hole.
     */
    static final String[] COKE_OVEN_DOOR = {
            "gggggggggg",
            "g54444443g",
            "g4r3333r2g",
            "g433pp332g",
            "g43pPPp32g",
            "g43pPPp32g",
            "g433pp332g",
            "g22222221g",
            "g45555543g",
            "g4r3333r2g",
            "g4333hh32g",
            "g4r3333r2g",
            "g32222221g",
            "gggggggggg",
    };

    /** {@code frame} -1 is the cold door; 0-3 are the lit frames with the charge glowing through. */
    static BufferedImage cokeOvenDoor(int frame) {
        BufferedImage im = cokeOvenBricks();
        int x0 = 3, y0 = 1;
        int[] bright = {1, 2, 1, 0};
        Random r = new Random(6100 + frame);
        for (int y = 0; y < COKE_OVEN_DOOR.length; y++)
            for (int x = 0; x < COKE_OVEN_DOOR[y].length(); x++) {
                char ch = COKE_OVEN_DOOR[y].charAt(x);
                int c;
                if (ch >= '1' && ch <= '5') c = WROUGHT_IRON.get(ch - '0');
                else if (ch == 'r' || ch == 'h') c = WROUGHT_IRON.get(5);
                else if (ch == 'p') c = WROUGHT_IRON.get(1);
                else if (ch == 'P') c = frame < 0 ? 0x0e0c0c : HEAT_BAND[Math.min(4, 2 + bright[frame] / 2 + r.nextInt(2))];
                else if (ch == 'g') {
                    // A thin glow leaks round the leaf, brightest along the bottom where the heat sits.
                    boolean bottom = y == COKE_OVEN_DOOR.length - 1;
                    if (frame < 0 || (!bottom && r.nextInt(3) != 0)) c = COKE.get(1);
                    else c = HEAT_BAND[Math.min(2, (bottom ? 1 : 0) + (r.nextInt(3) < bright[frame] ? 1 : 0))];
                } else continue;
                px(im, x0 + x, y0 + y, c);
            }
        // Rivet shadows and the latch's shadow.
        for (int y = 0; y < COKE_OVEN_DOOR.length; y++)
            for (int x = 0; x < COKE_OVEN_DOOR[y].length(); x++)
                if (COKE_OVEN_DOOR[y].charAt(x) == 'r' || COKE_OVEN_DOOR[y].charAt(x) == 'h') {
                    char below = COKE_OVEN_DOOR[y + 1].charAt(x);
                    if (below >= '1' && below <= '5') px(im, x0 + x, y0 + y + 1, WROUGHT_IRON.get(1));
                }
        return im;
    }

    static BufferedImage cokeOvenDoorLit() {
        BufferedImage strip = new BufferedImage(16, 64, BufferedImage.TYPE_INT_ARGB);
        for (int f = 0; f < 4; f++) strip.getGraphics().drawImage(cokeOvenDoor(f), 0, f * 16, null);
        return strip;
    }

    /**
     * Coke block: fused grey-black chunks (a tiling Voronoi of 9 cells), dark cracks between them, each chunk
     * lit on its upper-left, and pale pores (steps 4-5) that make it read as coke rather than coal.
     */
    static BufferedImage cokeBlock() {
        Random r = new Random(7272);
        int n = 9;
        double[][] pts = new double[n][2];
        for (double[] p : pts) { p[0] = r.nextDouble() * 16; p[1] = r.nextDouble() * 16; }
        BufferedImage im = img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double best = 1e9, second = 1e9;
                double bdx = 0, bdy = 0;
                for (double[] p : pts)
                    for (int ox = -16; ox <= 16; ox += 16)
                        for (int oy = -16; oy <= 16; oy += 16) {
                            double dx = x + 0.5 - (p[0] + ox), dy = y + 0.5 - (p[1] + oy);
                            double d = Math.sqrt(dx * dx + dy * dy);
                            if (d < best) { second = best; best = d; bdx = dx; bdy = dy; }
                            else if (d < second) second = d;
                        }
                int step;
                if (second - best < 0.9) step = 1;
                else {
                    double light = -(bdx + bdy) / Math.max(best, 0.01);
                    step = light > 0.5 ? 4 : light < -0.5 ? 2 : 3;
                }
                px(im, x, y, COKE.get(step));
            }
        // Pores: a pale lip with a dark hole tucked under it.
        for (int i = 0; i < 18; i++) {
            int x = r.nextInt(16), y = r.nextInt(16);
            if ((im.getRGB(x, y) & 0xffffff) == COKE.get(1)) continue;
            pxWrap(im, x, y, COKE.get(r.nextInt(3) == 0 ? 5 : 4));
            pxWrap(im, x + 1, y + 1, COKE.get(1));
        }
        return im;
    }

    /** A rounded, porous lump of coke: duller and lighter than coal, pocked with pores. */
    static BufferedImage cokeLump() {
        double[][] n = fractal(7373);
        BufferedImage im = img();
        double cx = 7.5, cy = 8.5, rx = 6.2, ry = 5.2;
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double dx = (x - cx) / rx, dy = (y - cy) / ry, d = Math.sqrt(dx * dx + dy * dy);
                if (d > 0.82 + n[y][x] * 0.3) continue;
                double light = -(dx + dy) / Math.max(d, 0.2);
                int step = light > 0.6 && d > 0.35 ? 4 : light < -0.6 && d > 0.55 ? 2 : 3;
                if (d < 0.35) step = 3;
                px(im, x, y, COKE.get(step));
            }
        // Pores: a dark pit with its lit far wall below-right.
        int[][] pores = {{5, 6}, {9, 5}, {7, 9}, {11, 8}, {4, 10}, {9, 11}};
        for (int[] p : pores) {
            if (!opaque(im, p[0], p[1]) || !opaque(im, p[0] + 1, p[1] + 1)) continue;
            px(im, p[0], p[1], COKE.get(1));
            px(im, p[0] + 1, p[1] + 1, COKE.get(5));
        }
        return outline(im);
    }

    /** The dust heap with pale grit through it, so coke dust reads lighter than charcoal dust. */
    static final String[] COKE_DUST_HEAP = {
            "....45.....",
            "...4554....",
            "..454453...",
            ".45443532..",
            "4444353422.",
            "43533322521",
            ".222521111.",
    };

    /** A large brick, heavier than the fire brick: six rows, the same top-left light. */
    static final String[] COKE_OVEN_BRICK_ITEM = {
            "................",
            "................",
            "................",
            "................",
            "................",
            "...45555555554..",
            "..4555555555443.",
            ".44444444444332.",
            ".44444444444332.",
            ".33333333333221.",
            "..2222222222111.",
            "................",
    };

    /** The unfired brick: pale fire clay with grains of the sand mixed into it. */
    static BufferedImage unfiredCokeOvenBrick() {
        BufferedImage im = art(FIRE_CLAY, COKE_OVEN_BRICK_ITEM);
        int[][] grains = {{4, 6}, {9, 7}, {6, 8}, {12, 8}, {3, 9}, {10, 9}, {7, 10}};
        for (int[] g : grains) px(im, g[0], g[1], SAND.get(4));
        return im;
    }

    /** A fired brick, sooted along its top like the bricks in the wall. */
    static BufferedImage cokeOvenBrickItem() {
        String[] rows = COKE_OVEN_BRICK_ITEM.clone();
        rows[5] = "...44344443444..";
        return art(COKE_OVEN_BRICK, rows);
    }

    /** An iron bucket with dark creosote inside: digits are wrought iron, letters creosote. */
    static final String[] CREOSOTE_BUCKET = {
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

    static final String[] TREATED_STICK = {
            "................",
            "............45..",
            "...........443..",
            "..........442...",
            ".........342....",
            "........342.....",
            ".......342......",
            "......342.......",
            ".....342........",
            "....342.........",
            "...342..........",
            "..332...........",
            "..21............",
            "................",
    };

    /**
     * Treated planks: four boards of dark, creosote-soaked wood, a lit top row and dark seam per board, a butt
     * joint in each, and one oily step-4 sheen per board (spec 21.2).
     */
    static BufferedImage treatedPlanks() {
        BufferedImage im = img();
        Random r = new Random(8484);
        int[] joints = {11, 4, 13, 7};
        for (int b = 0; b < 4; b++) {
            int y0 = b * 4;
            for (int row = 0; row < 3; row++) {
                int x = 0;
                while (x < 16) {
                    int run = 2 + r.nextInt(5);
                    int step = row == 0 ? 3 : (r.nextInt(3) == 0 ? 3 : 2);
                    if (row == 2 && r.nextInt(3) == 0) step = 1;      // latewood streaks low on the board
                    for (int k = 0; k < run && x < 16; k++, x++) px(im, x, y0 + row, TREATED_WOOD.get(step));
                }
            }
            for (int x = 0; x < 16; x++) px(im, x, y0 + 3, TREATED_WOOD.get(1));
            int j = joints[b];
            for (int row = 0; row < 3; row++) {
                px(im, j, y0 + row, TREATED_WOOD.get(1));
                px(im, (j + 1) % 16, y0 + row, TREATED_WOOD.get(row == 0 ? 4 : 3));
            }
            // The oily sheen: a short bright run on the upper half of the board, clear of the joint.
            int sx = Math.floorMod(j + 3 + r.nextInt(4), 16);
            for (int k = 0; k < 3; k++) if (Math.floorMod(sx + k, 16) != j) px(im, Math.floorMod(sx + k, 16), y0 + 1, TREATED_WOOD.get(4));
        }
        return im;
    }

    /**
     * Coke oven screen, 176x166. Input (34,26) and output (94,26) slot frames with the flame between them at
     * (63,27) 14x14, creosote tank well (129,16) 18x54 with ticks at every 4000 mB, bucket in (152,17) and
     * out (152,53) with an arrow between. Status lines at y 50 and 60 over x 8-130. Sprites: creosote fill
     * strip 16x52 at u 176, v 0; lit flame 14x14 at u 192, v 0.
     */
    static BufferedImage cokeOvenGui() {
        BufferedImage im = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        panel(im, 176, 166);
        slot(im, 34, 26);
        slot(im, 94, 26);
        slot(im, 152, 17);
        slot(im, 152, 53);
        ghost(im, 152, 17, edgeRows(BUCKET_SHAPE));
        well(im, 129, 16, 18, 54, 0x2a2a2a);
        for (int i = 1; i <= 3; i++) {
            int y = 16 + 1 + 52 - i * 13;
            fill(im, 125, y, 4, 1, GUI_SHADOW);
            fill(im, 125, y + 1, 4, 1, GUI_LIGHT);
        }
        // Down arrow from the bucket in to the bucket out.
        for (int j = 0; j < 9; j++) {
            int half = j < 5 ? 1 : 8 - j;
            for (int i = -half; i <= half; i++) im.setRGB(159 + i, 36 + j, 0xff000000 | SLOT_FILL);
        }
        // Unlit flame, and its lit sprite.
        for (int y = 0; y < 14; y++)
            for (int x = 0; x < 14; x++)
                if (FLAME_SILHOUETTE[y].charAt(x) == '#') {
                    im.setRGB(63 + x, 27 + y, 0xff000000 | SLOT_FILL);
                    int band = y < 4 ? 4 : y < 8 ? 3 : y < 11 ? 2 : 1;
                    im.setRGB(192 + x, y, 0xff000000 | HEAT_BAND[band]);
                }
        inventory(im);
        int[] creosote = {CREOSOTE.get(1), CREOSOTE.get(2), CREOSOTE.get(3), CREOSOTE.get(4), CREOSOTE.get(5)};
        fillStrip(im, 176, creosote, 0xff);
        return im;
    }

    /** A bucket silhouette for the coke oven's empty bucket slot. */
    static final String[] BUCKET_SHAPE = {
            "................",
            "................",
            "................",
            "...##########...",
            "..############..",
            "..############..",
            "..############..",
            "...##########...",
            "...##########...",
            "....########....",
            "....########....",
            ".....######.....",
            "................",
            "................",
            "................",
            "................",
    };

    // Tier 4 spec 21: zinc calcine, chalky roasted lumps with scorched spots, sized by grade.
    static final Ramp CALCINE = ramp(0, 0x6a6660, 0x8a867e, 0xaaa69c, 0xc6c2b8, 0xdcd8d0);
    static final Ramp SCORCH = ramp(0, 0x3e3226, 0x56463a, 0x6e5c4a, 0x86735c, 0x9e8a70);
    static final String[] CALCINE_SMALL = {
            "..45..",
            ".4554.",
            "45b443",
            "34432a",
            ".2322.",
    };
    static final String[] CALCINE_POOR = {
            "...45.....",
            "..4554.45.",
            ".4b4434543",
            ".34432b432",
            "..2a22.22.",
    };
    static final String[] CALCINE_NORMAL = {
            "....455....",
            "...45544...",
            "..4b5443.45",
            ".45443a2453",
            "454332343b2",
            "3433b223322",
            ".222.2a21..",
    };
    static final String[] CALCINE_RICH = {
            ".....455.....",
            "...45554445..",
            "..4b5443b554.",
            ".4554432443b3",
            "45443a2345432",
            "4b43322433a22",
            "3433223b33222",
            ".2222.2a222..",
    };

    /** Steel anvil body: rolled steel, fine-grained and even, with a crisp dark edge and a cool vertical sheen. */
    static BufferedImage steelAnvilBody() {
        double[][] n = V2.grain(6161), e = noise(6162, 2);
        BufferedImage im = img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int step = n[y][x] > 0.6 ? 4 : n[y][x] < 0.38 ? 2 : 3;
                if (e[y][x] > 0.82) step = Math.min(5, step + 1);
                else if (e[y][x] < 0.12) step = Math.max(2, step - 1);
                if (x == 3 || x == 4) step = Math.min(5, step + 1);
                int d = Math.min(Math.min(x, y), Math.min(15 - x, 15 - y));
                if (d == 0) step = 1;
                else if (d == 1 && (x == 1 || y == 1)) step = Math.min(5, step + 1);
                px(im, x, y, STEEL.get(step));
            }
        px(im, 3, 4, STEEL.spec());
        return im;
    }

    /** Steel anvil face (visible columns 3-12): a ground, bright working face with crisp edges and a polished horn. */
    static BufferedImage steelAnvilTop() {
        double[][] n = V2.grain(6262), e = noise(6263, 2);
        BufferedImage im = img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int step;
                boolean rim = x <= 3 || x >= 12 || y == 0 || y == 15;
                boolean edge = x == 4 || x == 11 || y == 1 || y == 14;
                if (rim) step = e[y][x] > 0.6 ? 3 : 2;
                else if (edge) step = x == 4 || y == 1 ? 5 : 3;
                else {
                    step = x >= 6 && x <= 9 ? 5 : 4;
                    if (n[y][x] < 0.3) step--;
                    else if (step == 4 && n[y][x] > 0.7) step = 5;
                }
                px(im, x, y, STEEL.get(step));
            }
        // The horn end (top) is polished to a highlight.
        px(im, 7, 2, STEEL.spec());
        px(im, 8, 3, STEEL.spec());
        return im;
    }

    // Tier 4 spec 11.7: iron transmission, drawn over the wooden parts' layouts in wrought iron and brass.

    /** Forged iron axle: a round bar shaded across its width, with two brass collars. */
    static BufferedImage ironAxleSide() {
        double[][] n = fractal(8301);
        BufferedImage im = img();
        int[] shade = {4, 4, 4, 4, 4, 4, 5, 4, 4, 3, 3, 2, 2, 2, 2, 2};
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int step = shade[x];
                if (n[y][x] < 0.25 && step > 2) step--;
                px(im, x, y, WROUGHT_IRON.get(step));
            }
        for (int b0 : new int[] {2, 12}) {
            for (int x = 0; x < 16; x++) {
                int step = Math.min(5, shade[x] + (x == 6 ? 0 : 0));
                px(im, x, b0, BRASS.get(Math.min(5, step + 1)));
                px(im, x, b0 + 1, BRASS.get(step));
                px(im, x, b0 + 2, WROUGHT_IRON.get(1));
            }
        }
        px(im, 6, 7, WROUGHT_IRON.spec());
        return im;
    }

    /** Iron axle end: a dark bar end with a bright chamfer and a centre punch. */
    static BufferedImage ironAxleEnd() {
        BufferedImage im = img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double d = Math.hypot(x - 7.5, y - 7.5);
                int step = d > 2.4 ? (x + y < 15 ? 4 : 2) : 3;
                px(im, x, y, WROUGHT_IRON.get(step));
            }
        px(im, 7, 7, WROUGHT_IRON.get(1));
        px(im, 8, 8, WROUGHT_IRON.get(4));
        return im;
    }

    /**
     * Recolours a wooden gearbox face into iron: boards and frame become wrought iron plate, and the cog
     * showing through the window within {@code gearRadius} of the centre becomes brass.
     */
    static BufferedImage ironise(BufferedImage wood, double gearRadius) {
        BufferedImage im = img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int c = rgb(wood, x, y) & 0xffffff;
                boolean inGear = Math.hypot(x - 7.5, y - 7.5) <= gearRadius;
                int out = c;
                for (int i = 1; i <= 5; i++) {
                    if (c == (WOOD.get(i) & 0xffffff)) out = inGear ? BRASS.get(i) : WROUGHT_IRON.get(i);
                    else if (c == (DARK_WOOD.get(i) & 0xffffff)) out = inGear ? PIG_IRON.get(i) : WROUGHT_IRON.get(Math.max(1, i - 1));
                    else if (c == (WROUGHT_IRON.get(i) & 0xffffff) && inGear) out = BRASS.get(i);
                }
                px(im, x, y, out);
            }
        return im;
    }

    // Tier 4 spec 21.2 and 21.5: steam. The firebox is fire brick with a cast iron grate; the boiler is
    // riveted bronze plate; pipes and the gauge are drawn into the parts of one sheet their models use.

    /** Firebox side: fire brick held by a riveted wrought iron band along the top and bottom. */
    static BufferedImage fireboxSide() {
        BufferedImage im = fireBricks();
        for (int x = 0; x < 16; x++) {
            px(im, x, 0, WROUGHT_IRON.get(x % 5 == 0 ? 5 : 4));
            px(im, x, 1, WROUGHT_IRON.get(2));
            px(im, x, 14, WROUGHT_IRON.get(x % 7 == 3 ? 4 : 3));
            px(im, x, 15, WROUGHT_IRON.get(1));
        }
        for (int x : new int[] {2, 13}) {
            px(im, x, 0, WROUGHT_IRON.spec());
            px(im, x, 14, WROUGHT_IRON.get(5));
        }
        return im;
    }

    /** Firebox top: a cast iron hot plate in a fire brick rim, where the boiler sits. */
    static BufferedImage fireboxTop() {
        BufferedImage im = fireBricks();
        double[][] n = V2.grain(8411);
        for (int y = 2; y <= 13; y++)
            for (int x = 2; x <= 13; x++) {
                int step = 3;
                if (x == 2 || y == 2) step = 4;
                else if (x == 13 || y == 13) step = 2;
                else if (n[y][x] < 0.3) step = 2;
                else if (n[y][x] > 0.7) step = 4;
                px(im, x, y, PIG_IRON.get(step));
            }
        // A cross of casting ribs, and the four corner bolts.
        for (int i = 4; i <= 11; i++) {
            px(im, i, 7, PIG_IRON.get(4));
            px(im, i, 8, PIG_IRON.get(1));
            px(im, 7, i, PIG_IRON.get(4));
            px(im, 8, i, PIG_IRON.get(1));
        }
        for (int[] b : new int[][] {{3, 3}, {11, 3}, {3, 11}, {11, 11}}) rivet(im, b[0], b[1]);
        return im;
    }

    /**
     * Firebox front: fire brick round a cast iron grate door with an ash slot under it. Frames 0-3 are the
     * lit animation; {@code hot} pushes the coals to yellow and white (coke with a blower), -1 is cold.
     */
    static BufferedImage fireboxFront(int frame, boolean hot) {
        BufferedImage im = fireBricks();
        int[] bright = {0, 1, 2, 1};
        Random r = new Random(8500 + frame * 7 + (hot ? 100 : 0));
        int base = hot ? 3 : 1, cap = hot ? 5 : 3;
        // Coals behind the bars, brightest low down where the bed is.
        for (int y = 3; y <= 11; y++)
            for (int x = 3; x <= 12; x++) {
                int c;
                if (frame < 0) c = r.nextInt(4) == 0 ? CHARCOAL.get(2) : CHARCOAL.get(1);
                else {
                    int i = base + bright[frame] / 2 + (y >= 8 ? 1 : 0) - (y <= 4 ? 1 : 0) + (r.nextInt(3) == 0 ? 1 : 0) - (r.nextInt(4) == 0 ? 1 : 0);
                    c = HEAT_BAND[Math.max(0, Math.min(cap, i))];
                }
                px(im, x, y, c);
            }
        // Frame: lit top and left, shadowed bottom and right.
        for (int x = 2; x <= 13; x++) {
            px(im, x, 2, PIG_IRON.get(x == 2 ? 5 : 4));
            px(im, x, 12, PIG_IRON.get(2));
        }
        for (int y = 3; y <= 11; y++) {
            px(im, 2, y, PIG_IRON.get(4));
            px(im, 13, y, PIG_IRON.get(2));
        }
        // Vertical bars with a cross bar, each lit along its top.
        for (int x = 4; x <= 12; x += 2)
            for (int y = 3; y <= 11; y++) px(im, x, y, PIG_IRON.get(y == 3 ? 4 : 3));
        for (int x = 3; x <= 12; x++) {
            px(im, x, 7, PIG_IRON.get(x % 2 == 0 ? 4 : 3));
        }
        // Hinges on the left, latch on the right.
        px(im, 1, 4, WROUGHT_IRON.get(4));
        px(im, 1, 10, WROUGHT_IRON.get(4));
        px(im, 13, 6, WROUGHT_IRON.get(5));
        px(im, 13, 7, WROUGHT_IRON.get(4));
        px(im, 14, 7, WROUGHT_IRON.get(3));
        px(im, 13, 8, WROUGHT_IRON.get(2));
        // Ash slot: dark, with a faint glow when lit.
        for (int x = 4; x <= 11; x++) {
            px(im, x, 13, PIG_IRON.get(4));
            int glow = frame < 0 ? CHARCOAL.get(1) : HEAT_BAND[(x + frame) % 3 == 0 ? Math.min(cap, base) : 0];
            px(im, x, 14, glow);
        }
        px(im, 3, 13, PIG_IRON.get(4));
        px(im, 3, 14, PIG_IRON.get(2));
        px(im, 12, 13, PIG_IRON.get(3));
        px(im, 12, 14, PIG_IRON.get(2));
        return im;
    }

    static BufferedImage fireboxFrontLit(boolean hot) {
        BufferedImage strip = new BufferedImage(16, 64, BufferedImage.TYPE_INT_ARGB);
        for (int f = 0; f < 4; f++) strip.getGraphics().drawImage(fireboxFront(f, hot), 0, f * 16, null);
        return strip;
    }

    /**
     * Riveted bronze plate in two horizontal courses (style guide 7): each course lapped over the one below
     * with a dark seam and a row of rivets, the vertical seams staggered.
     */
    static BufferedImage bronzePlates(long seed) {
        BufferedImage im = img();
        double[][] n = V2.grain(seed);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int row = y % 8;
                int step = row == 0 ? 4 : row == 7 ? 2 : 3;
                if (n[y][x] > 0.68 && step == 3) step = 4;
                if (n[y][x] < 0.3 && step == 3) step = 2;
                px(im, x, y, BRONZE.get(step));
            }
        for (int course = 0; course < 2; course++) {
            int y0 = course * 8;
            int seam = course == 0 ? 0 : 8;
            for (int y = y0; y < y0 + 8; y++) {
                pxWrap(im, seam - 1, y, BRONZE.get(1));
                pxWrap(im, seam, y, BRONZE.get(4));
            }
            for (int x = 0; x < 16; x++) pxWrap(im, x, y0 + 7, BRONZE.get(1));
            for (int x = (course == 0 ? 2 : 6); x < 16 + (course == 0 ? 2 : 6); x += 4) {
                pxWrap(im, x, y0 + 1, BRONZE.spec());
                pxWrap(im, x + 1, y0 + 2, BRONZE.get(1));
                pxWrap(im, x, y0 + 2, BRONZE.get(2));
            }
        }
        return im;
    }

    /** Boiler top: plates round the riveted steam outlet flange. */
    static BufferedImage boilerTop() {
        BufferedImage im = bronzePlates(8611);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double d = Math.hypot(x - 7.5, y - 7.5);
                if (d <= 5.2 && d > 3.6) px(im, x, y, BRONZE.get(x + y < 15 ? 5 : 3));
                else if (d <= 3.6 && d > 2.2) px(im, x, y, BRASS.get(x + y < 15 ? 4 : 2));
                else if (d <= 2.2) px(im, x, y, BRONZE.get(1));
                if (d > 5.2 && d <= 5.9) px(im, x, y, BRONZE.get(1));
            }
        for (int[] p : new int[][] {{7, 3}, {12, 7}, {8, 12}, {3, 8}}) {
            px(im, p[0], p[1], BRONZE.spec());
            px(im, p[0] + 1, p[1] + 1, BRONZE.get(2));
        }
        px(im, 6, 6, 0x0e0c0c);
        return im;
    }

    /** Boiler front: plates with the brass-rimmed gauge high up and a water sight glass beside it. */
    static BufferedImage boilerFront() {
        BufferedImage im = bronzePlates(8612);
        for (int y = 1; y <= 8; y++)
            for (int x = 2; x <= 9; x++) {
                double d = Math.hypot(x - 5.5, y - 4.5);
                if (d > 4.0) continue;
                int c;
                if (d > 3.0) c = BRASS.get(x + y < 10 ? 5 : 2);
                else c = PAPER.get(5);
                px(im, x, y, c);
            }
        // Arcs: green, amber, red round the top of the face.
        int[][] arc = {{3, 5, 0x4c9a3a}, {3, 4, 0x4c9a3a}, {4, 3, 0x4c9a3a}, {5, 2, 0xd8a030}, {6, 2, 0xd8a030}, {7, 3, 0xc8342a}, {7, 4, 0xc8342a}};
        for (int[] a : arc) px(im, a[0], a[1], a[2]);
        px(im, 5, 5, 0x2a2626);
        px(im, 5, 4, 0x2a2626);
        px(im, 6, 3, 0xb02a1e);
        px(im, 4, 6, BRASS.get(1));
        // Sight glass: a brass-framed tube, water to half way.
        for (int y = 2; y <= 13; y++) {
            px(im, 12, y, BRASS.get(y == 2 || y == 13 ? 5 : 4));
            px(im, 14, y, BRASS.get(y == 2 || y == 13 ? 3 : 2));
            px(im, 13, y, y == 2 || y == 13 ? BRASS.get(4) : y >= 8 ? (y == 8 ? 0x7aa4f0 : 0x3f76e4) : 0x9aa4a8);
        }
        px(im, 13, 4, 0xe8eef0);
        return im;
    }

    /** The cracked boiler: a dark split across the plates, stained pale where steam escaped, a bent rivet line. */
    static BufferedImage crackedBoiler(BufferedImage im, long seed) {
        BufferedImage out = img();
        out.getGraphics().drawImage(im, 0, 0, null);
        Random r = new Random(seed);
        int y = 3 + r.nextInt(3);
        for (int x = 1; x < 15; x++) {
            if (r.nextInt(3) == 0) y += r.nextBoolean() ? 1 : -1;
            y = Math.max(2, Math.min(13, y));
            px(out, x, y, 0x120c08);
            px(out, x, y + 1, BRONZE.get(1));
            // Steam stains above the split.
            if (r.nextInt(2) == 0) {
                int s = rgb(out, x, y - 1);
                px(out, x, y - 1, blend(s, 0xd8d4c8, 0.45));
                if (r.nextInt(2) == 0) px(out, x, y - 2, blend(rgb(out, x, y - 2), 0xd8d4c8, 0.25));
            }
        }
        // Scorch toward the bottom edge.
        for (int x = 0; x < 16; x++)
            for (int yy = 13; yy < 16; yy++)
                if (r.nextInt(3) == 0) px(out, x, yy, scale(rgb(out, x, yy), 0.7));
        return out;
    }

    static int blend(int a, int b, double t) {
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }

    /**
     * One pipe sheet: the tube's side at (0,0) 5x5, run lengthwise; the joint face at (5,5) 6x6, solder
     * bands for copper and bronze or a bolted flange for steel; the open end at (0,11) 5x5. The rest is
     * plain tube, for break particles.
     */
    static BufferedImage pipeSheet(Ramp metal, boolean flanged, long seed) {
        BufferedImage im = img();
        double[][] n = V2.grain(seed);
        int[] across = {4, 5, 4, 3, 2};
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int step = across[y % 5];
                if (n[y][x] < 0.28 && step > 2) step--;
                px(im, x, y, metal.get(step));
            }
        px(im, 2, 1, metal.spec());
        // Joint face.
        for (int y = 5; y <= 10; y++)
            for (int x = 5; x <= 10; x++) {
                boolean edge = x == 5 || y == 5 || x == 10 || y == 10;
                int step = edge ? (x == 5 || y == 5 ? 4 : 2) : 3;
                if (!edge && (x + y) % 5 == 0) step = 4;
                px(im, x, y, metal.get(step));
            }
        if (flanged) {
            for (int[] b : new int[][] {{6, 6}, {9, 6}, {6, 9}, {9, 9}}) px(im, b[0], b[1], metal.spec());
            for (int[] b : new int[][] {{7, 7}, {8, 8}}) px(im, b[0], b[1], metal.get(2));
        } else {
            for (int i = 6; i <= 9; i++) {
                px(im, i, 6, SOLDER.get(i == 6 ? 5 : 4));
                px(im, 6, i, SOLDER.get(4));
                px(im, i, 9, SOLDER.get(2));
                px(im, 9, i, SOLDER.get(3));
            }
        }
        // Open end: a lit rim round a dark bore.
        for (int y = 11; y <= 15; y++)
            for (int x = 0; x <= 4; x++) {
                boolean rim = x == 0 || x == 4 || y == 11 || y == 15;
                int c = rim ? metal.get(x == 0 || y == 11 ? 5 : 3) : (x == 2 && y == 13 ? 0x0e0c0c : metal.get(1));
                px(im, x, y, c);
            }
        // A band of the joint metal near the end of the tube side, so arms read as soldered on.
        if (!flanged) for (int y = 0; y < 5; y++) px(im, 4, y, SOLDER.get(across[y] - 1 < 1 ? 1 : across[y] - 1));
        else for (int y = 0; y < 5; y++) px(im, 4, y, metal.get(Math.min(5, across[y] + 1)));
        return im;
    }

    /** Gauge housing: turned brass, its cap a dome with a centre screw. */
    static BufferedImage gaugeHousing() {
        BufferedImage im = img();
        double[][] n = V2.grain(8701);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double d = Math.hypot(x - 7.5, y - 7.5);
                int step = d < 1.6 ? 5 : d < 3.2 ? 4 : 3;
                if (x == 4 || y == 4) step = 4;
                if (x == 11 || y == 11) step = 2;
                if (n[y][x] < 0.25 && step == 3) step = 2;
                px(im, x, y, BRASS.get(step));
            }
        px(im, 7, 7, BRASS.spec());
        px(im, 8, 8, BRASS.get(2));
        return im;
    }

    /**
     * Gauge dial in the centre 8x8: brass bezel, cream face with green, amber and red arcs, and the needle
     * at {@code reading} of 4 (0 on the left, 4 on the right).
     */
    static BufferedImage gaugeDial(int reading) {
        BufferedImage im = gaugeHousing();
        // Bezel.
        for (int y = 4; y <= 11; y++)
            for (int x = 4; x <= 11; x++) {
                boolean edge = x == 4 || y == 4 || x == 11 || y == 11;
                if (edge) px(im, x, y, BRASS.get(x == 4 || y == 4 ? 5 : 2));
                else px(im, x, y, PAPER.get(5));
            }
        for (int[] c : new int[][] {{4, 4}, {11, 4}, {4, 11}, {11, 11}}) px(im, c[0], c[1], BRASS.get(3));
        double hx = 7.5, hy = 9.0;
        for (int y = 5; y <= 10; y++)
            for (int x = 5; x <= 10; x++) {
                double dx = x + 0.5 - hx, dy = hy - (y + 0.5);
                double d = Math.hypot(dx, dy);
                if (dy < 0 || d < 2.3 || d > 3.6) continue;
                double share = 1.0 - Math.atan2(dy, dx) / Math.PI;
                px(im, x, y, share < 0.6 ? 0x4c9a3a : share < 0.85 ? 0xd8a030 : 0xc8342a);
            }
        double angle = Math.PI * (1.0 - reading / 4.0);
        for (double t = 0.6; t <= 2.6; t += 0.5) {
            int x = (int) Math.floor(hx + Math.cos(angle) * t), y = (int) Math.floor(hy - Math.sin(angle) * t);
            px(im, x, y, t > 2.0 ? 0xb02a1e : 0x2a2626);
        }
        px(im, 7, 9, 0x3a3230);
        px(im, 5, 5, 0xffffff);
        return im;
    }

    /** Firebox screen (spec 21.5): fuel slots under the flame, status line on top, gauge on the right. */
    static BufferedImage fireboxGui() {
        BufferedImage im = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        panel(im, 176, 166);
        for (int i = 0; i < 4; i++) slot(im, 35 + i * 18, 53);
        for (int y = 0; y < 14; y++)
            for (int x = 0; x < 14; x++)
                if (FLAME_SILHOUETTE[y].charAt(x) == '#') {
                    im.setRGB(63 + x, 35 + y, 0xff000000 | SLOT_FILL);
                    int band = y < 4 ? 4 : y < 8 ? 3 : y < 11 ? 2 : 1;
                    im.setRGB(192 + x, y, 0xff000000 | HEAT_BAND[band]);
                }
        // Gauge: 0 to 1750 degrees over 58 px, a notch every 250, as on the forge.
        well(im, 150, 16, 12, 60, 0x2a2a2a);
        for (int t = 250; t < 1750; t += 250) {
            int y = 17 + 58 - Math.round(t / 1750f * 58);
            fill(im, 147, y, 3, 1, t % 500 == 0 ? GUI_SHADOW : SLOT_FILL);
        }
        inventory(im);
        return im;
    }

    /**
     * Bronze boiler screen (spec 21.5): water and steam tanks, the pressure dial (0 to 4 bar, green to 60%,
     * amber to 85%, then red), the integrity bar. Fill strips: water at u 176, steam at u 192.
     */
    static BufferedImage boilerGui() {
        BufferedImage im = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        panel(im, 176, 166);
        for (int x : new int[] {7, 29}) {
            well(im, x, 16, 18, 54, 0x2a2a2a);
            for (int i = 1; i <= 3; i++) fill(im, x + 1, 16 + 1 + 52 - i * 13, 3, 1, 0x4a4a4a);
        }
        // Dial: brass bezel, cream face, arcs, a tick per bar.
        int cx = 88, cy = 44;
        for (int y = cy - 26; y <= cy + 2; y++)
            for (int x = cx - 26; x <= cx + 26; x++) {
                double dx = x + 0.5 - (cx + 0.5), dy = (cy + 0.5) - (y + 0.5);
                double d = Math.hypot(dx, dy);
                if (dy < -2.5 || d > 25.5) continue;
                int c;
                if (d > 23.5) c = BRASS.get(dx < 0 && dy > 8 ? 5 : dy > 8 ? 4 : 2);
                else if (d > 22.5) c = BRASS.get(1);
                else c = PAPER.get(5);
                if (d <= 22.5 && d > 19.5 && dy >= 0) {
                    double share = 1.0 - Math.atan2(dy, dx) / Math.PI;
                    c = share < 0.6 ? 0x4c9a3a : share < 0.85 ? 0xd8a030 : 0xc8342a;
                }
                im.setRGB(x, y, 0xff000000 | c);
            }
        for (int bar = 0; bar <= 4; bar++) {
            double a = Math.PI * (1.0 - bar / 4.0);
            for (double t = 16.5; t <= 19.0; t += 0.5)
                im.setRGB(cx + (int) Math.round(Math.cos(a) * t), cy - (int) Math.round(Math.sin(a) * t), 0xff3a3230);
        }
        // Integrity bar.
        well(im, 119, 18, 50, 6, 0x2a2a2a);
        inventory(im);
        int[] water = {0x2c5cb8, 0x3466cc, 0x3f76e4, 0x5a8aec, 0x7aa4f0};
        int[] steam = {0xb8bcc0, 0xcdd0d2, 0xdfe1e2, 0xeceeef, 0xffffff};
        fillStrip(im, 176, water, 0xff);
        fillStrip(im, 192, steam, 0xd0);
        return im;
    }

    /** Riveted wrought iron plate with a rivet row round the edge: the engine's bed. */
    static BufferedImage engineBase() {
        BufferedImage im = img();
        double[][] n = V2.grain(8801);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int step = n[y][x] > 0.66 ? 4 : n[y][x] < 0.3 ? 2 : 3;
                if (x == 0 || y == 0) step = 4;
                if (x == 15 || y == 15) step = 2;
                px(im, x, y, WROUGHT_IRON.get(step));
            }
        for (int i = 2; i <= 12; i += 5) {
            rivet(im, i, 2);
            rivet(im, i, 12);
            rivet(im, 2, i);
            rivet(im, 12, i);
        }
        return im;
    }

    /** The cylinder: brass lagging in vertical staves, held by two steel bands. */
    static BufferedImage engineCylinder() {
        BufferedImage im = img();
        double[][] n = V2.grain(8802);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int step = x % 3 == 0 ? 2 : x % 3 == 1 ? 4 : 3;
                if (n[y][x] < 0.25 && step > 2) step--;
                px(im, x, y, BRASS.get(step));
            }
        for (int band : new int[] {3, 12}) {
            for (int x = 0; x < 16; x++) {
                px(im, x, band, STEEL.get(x % 6 == 1 ? 5 : 4));
                px(im, x, band + 1, STEEL.get(2));
            }
            px(im, 7, band, STEEL.spec());
        }
        return im;
    }

    /** Cylinder end, in the centre 8x8: a bolted brass cover round the steam inlet. */
    static BufferedImage engineCylinderEnd() {
        BufferedImage im = engineCylinder();
        for (int y = 4; y <= 11; y++)
            for (int x = 4; x <= 11; x++) {
                double d = Math.hypot(x - 7.5, y - 7.5);
                int c = d > 3.6 ? BRASS.get(x + y < 15 ? 4 : 2) : d > 1.6 ? BRASS.get(x + y < 15 ? 5 : 3) : 0x0e0c0c;
                px(im, x, y, c);
            }
        for (int[] b : new int[][] {{4, 4}, {11, 4}, {4, 11}, {11, 11}, {7, 4}, {4, 7}, {11, 8}, {8, 11}}) px(im, b[0], b[1], STEEL.get(5));
        px(im, 6, 6, BRASS.spec());
        return im;
    }

    /** Polished steel bar for the shaft and piston rod. */
    static BufferedImage engineRod() {
        BufferedImage im = img();
        int[] shade = {3, 4, 5, 5, 4, 4, 3, 3, 3, 4, 5, 5, 4, 4, 3, 2};
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) px(im, x, y, STEEL.get(shade[(x + y) % 16] - (y % 8 == 7 ? 1 : 0)));
        px(im, 2, 2, STEEL.spec());
        px(im, 10, 10, STEEL.spec());
        return im;
    }

    /** Cast flywheel iron: dark and grainy, with lit casting edges. */
    static BufferedImage engineFlywheel() {
        BufferedImage im = img();
        double[][] n = V2.grain(8803);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int step = n[y][x] > 0.7 ? 4 : n[y][x] < 0.32 ? 2 : 3;
                if (x % 8 == 0 || y % 8 == 0) step = 4;
                if (x % 8 == 7 || y % 8 == 7) step = 2;
                px(im, x, y, PIG_IRON.get(step));
            }
        px(im, 3, 3, PIG_IRON.spec());
        px(im, 11, 11, PIG_IRON.spec());
        return im;
    }

    /** Pump intake: a bronze housing round a grille of dark slots. */
    static BufferedImage pumpFront() {
        BufferedImage im = bronzePlates(8811);
        for (int y = 3; y <= 12; y++)
            for (int x = 3; x <= 12; x++) {
                boolean frame = x == 3 || y == 3 || x == 12 || y == 12;
                int c;
                if (frame) c = BRONZE.get(x == 3 || y == 3 ? 5 : 2);
                else c = y % 2 == 0 ? BRONZE.get(4) : 0x141010;
                px(im, x, y, c);
            }
        for (int[] b : new int[][] {{2, 2}, {13, 2}, {2, 13}, {13, 13}}) rivet(im, b[0], b[1]);
        return im;
    }

    /** Pump side: the brass gear on the shaft, behind a bronze cover. */
    static BufferedImage pumpSide() {
        BufferedImage im = bronzePlates(8812);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double d = Math.hypot(x - 7.5, y - 7.5);
                double a = Math.atan2(y - 7.5, x - 7.5);
                boolean tooth = Math.cos(a * 8) > 0.2;
                if (d <= 4.6 || (d <= 6.0 && tooth)) {
                    int step = (x + y < 15) ? 4 : 3;
                    if (d > 4.6) step = (x + y < 15) ? 5 : 2;
                    px(im, x, y, BRASS.get(step));
                }
                if (d <= 6.0 && d > 5.4 && !tooth) px(im, x, y, BRONZE.get(1));
            }
        // Shaft end through the middle.
        for (int y = 6; y <= 9; y++)
            for (int x = 6; x <= 9; x++) px(im, x, y, WROUGHT_IRON.get(x + y < 15 ? 4 : 2));
        px(im, 7, 7, WROUGHT_IRON.get(1));
        return im;
    }

    /** Pump outlet: a flange with a dark bore, where the pipe joins. */
    static BufferedImage pumpBack() {
        BufferedImage im = bronzePlates(8813);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double d = Math.hypot(x - 7.5, y - 7.5);
                if (d <= 4.8 && d > 2.6) px(im, x, y, BRONZE.get(x + y < 15 ? 5 : 3));
                else if (d <= 2.6) px(im, x, y, d > 1.6 ? BRONZE.get(1) : 0x0e0c0c);
            }
        for (int[] b : new int[][] {{7, 3}, {12, 7}, {8, 12}, {3, 8}}) px(im, b[0], b[1], SOLDER.get(5));
        return im;
    }

    // ---------------------------------------------------------------- tier 4: crusher (spec 21.2)

    /** Crusher side: engineBase() style riveted wrought iron plate with an iron shaft boss in the centre. */
    static BufferedImage crusherSide() {
        BufferedImage im = engineBase();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double d = Math.hypot(x - 7.5, y - 7.5);
                if (d > 4.6 && d <= 5.4) px(im, x, y, WROUGHT_IRON.get(1));
                else if (d <= 4.6 && d > 2.8) px(im, x, y, WROUGHT_IRON.get(x + y < 15 ? 5 : 3));
                else if (d <= 2.8 && d > 1.6) px(im, x, y, WROUGHT_IRON.get(x + y < 15 ? 2 : 4));
                else if (d <= 1.6) px(im, x, y, WROUGHT_IRON.get(1));
            }
        px(im, 4, 4, WROUGHT_IRON.get(5));
        px(im, 6, 6, WROUGHT_IRON.get(2));
        return im;
    }

    /** Crusher top: wrought iron rim, dark hopper, two toothed steel jaw edges seen from above. */
    static BufferedImage crusherTop() {
        BufferedImage im = engineBase();
        for (int y = 2; y <= 13; y++)
            for (int x = 2; x <= 13; x++) {
                boolean edge = x == 2 || y == 2 || x == 13 || y == 13;
                if (edge) px(im, x, y, WROUGHT_IRON.get(x == 2 || y == 2 ? 5 : 2));
                else px(im, x, y, PIG_IRON.get(1));
            }
        // inner shadow below/right of the top-left rim
        for (int i = 3; i <= 12; i++) { px(im, i, 3, 0x0e0c0c); px(im, 3, i, 0x0e0c0c); }
        for (int y = 4; y <= 11; y++) {
            for (int x = 4; x <= 5; x++) px(im, x, y, STEEL.get(x == 4 ? 4 : 3));
            px(im, 6, y, y % 2 == 0 ? STEEL.get(5) : STEEL.get(2));
            for (int x = 10; x <= 11; x++) px(im, x, y, STEEL.get(x == 10 ? 4 : 2));
            px(im, 9, y, y % 2 == 1 ? STEEL.get(4) : STEEL.get(1));
        }
        px(im, 6, 6, STEEL.spec());
        for (int[] r : new int[][] {{1, 1}, {13, 1}, {1, 13}, {13, 13}}) px(im, r[0], r[1], WROUGHT_IRON.get(5));
        return im;
    }

    /** One crusher front frame: shift d pulls the jaws together; f < 0 is the idle still frame. */
    static BufferedImage crusherFront(int d, boolean grit, int f) {
        BufferedImage im = engineBase();
        for (int[] r : new int[][] {{2, 2}, {12, 2}, {2, 12}, {12, 12}}) rivet(im, r[0], r[1]);
        // window frame
        for (int y = 3; y <= 12; y++)
            for (int x = 3; x <= 12; x++) {
                boolean edge = x == 3 || y == 3 || x == 12 || y == 12;
                if (edge) px(im, x, y, STEEL.get(x == 3 || y == 3 ? 2 : 4));
                else px(im, x, y, PIG_IRON.get(1));
            }
        // interior x4..11, y4..11 : lit sill on the first row for depth
        for (int x = 4; x <= 11; x++) px(im, x, 4, 0x0e0c0c);
        for (int y = 5; y <= 11; y++) {
            for (int x = 4 + d; x <= 5 + d; x++) px(im, x, y, STEEL.get(x == 4 + d ? 4 : 3));
            int tl = 6 + d;
            if (y % 2 == 1 && tl <= 11 - d - 2) px(im, tl, y, STEEL.get(5));
            for (int x = 10 - d; x <= 11 - d; x++) px(im, x, y, STEEL.get(x == 10 - d ? 4 : 2));
            int tr = 9 - d;
            if (y % 2 == 0 && tr >= 6 + d + 2) px(im, tr, y, STEEL.get(4));
        }
        if (d == 0) px(im, 4, 6, STEEL.spec());
        if (grit) {
            int[][] g = {{7, 0}, {8, 3}, {7, 5}, {8, 2}};
            int[] cols = {PIG_IRON.get(4), BARK.get(4), PIG_IRON.get(5), BARK.get(5)};
            for (int k = 0; k < g.length; k++) {
                int y = 5 + (g[k][1] + f * 2 + k) % 7;
                int x = g[k][0] + ((f + k) % 2 == 0 ? 0 : (k % 2 == 0 ? 1 : -1));
                px(im, Math.max(6 + d, Math.min(9 - d, x)), y, cols[k]);
            }
        }
        return im;
    }

    static BufferedImage crusherFrontActive() {
        BufferedImage strip = new BufferedImage(16, 64, BufferedImage.TYPE_INT_ARGB);
        int[] shift = {0, 1, 2, 1};
        for (int f = 0; f < 4; f++) strip.getGraphics().drawImage(crusherFront(shift[f], true, f), 0, f * 16, null);
        return strip;
    }

    /** Crusher screen, 176x176: three lanes (input, arrow, two outputs each) over a taller inventory. */
    static BufferedImage crusherGui() {
        BufferedImage im = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        panel(im, 176, 176);
        for (int y : new int[] {18, 36, 54}) {
            slot(im, 30, y);
            arrow(im, 56, y);
        }
        for (int x : new int[] {98, 116, 134})
            for (int y : new int[] {27, 45}) slot(im, x, y);
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++) slot(im, 8 + col * 18, 94 + row * 18);
        for (int col = 0; col < 9; col++) slot(im, 8 + col * 18, 152);
        return im;
    }

    static void crusher() throws IOException {
        save("block/crusher_side", crusherSide());
        save("block/crusher_top", crusherTop());
        save("block/crusher_front", crusherFront(0, false, 0));
        saveAnimated("block/crusher_front_active", crusherFrontActive(), 2);
        saveRaw("gui/crusher", crusherGui());
    }

    // ---------------------------------------------------------------- tier 4: washer (spec 21.2)

    /** Blend two rgb colours: t is the weight of b. */
    static int mix(int a, int b, double t) {
        int r = (int) Math.round(((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) Math.round(((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) Math.round((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }

    /** Vertical treated-wood staves, 4 px wide, lit left edge and dark seam, with seeded grain streaks. */
    static void washerStaves(BufferedImage im, long seed) {
        Random r = new Random(seed);
        for (int s = 0; s < 4; s++) {
            int x0 = s * 4;
            for (int y = 0; y < 16; y++) {
                px(im, x0, y, TREATED_WOOD.get(4));
                px(im, x0 + 1, y, TREATED_WOOD.get(3));
                px(im, x0 + 2, y, TREATED_WOOD.get(3));
                px(im, x0 + 3, y, TREATED_WOOD.get(1));
            }
            int gx = x0 + 1 + r.nextInt(2);
            int gy = r.nextInt(6);
            for (int k = 0; k < 4 + r.nextInt(3); k++) px(im, gx, (gy + k) % 16, TREATED_WOOD.get(2));
            int hx = x0 + 1 + (gx - x0 == 1 ? 1 : 0);
            int hy = 8 + r.nextInt(6);
            for (int k = 0; k < 3; k++) px(im, hx, (hy + k) % 16, TREATED_WOOD.get(4));
        }
    }

    /** A 2-row bronze band across the full width at row y, a shadow row below, rivets on the lit row. */
    static void washerBand(BufferedImage im, int y, int[] rivets) {
        for (int x = 0; x < 16; x++) {
            px(im, x, y, BRONZE.get((x * 5 + y) % 9 == 0 ? 5 : 4));
            px(im, x, y + 1, BRONZE.get((x * 3 + y) % 7 == 0 ? 2 : 3));
            px(im, x, y + 2, TREATED_WOOD.get(1));
            px(im, x, y - 1, TREATED_WOOD.get(1));
        }
        for (int x : rivets) {
            px(im, x, y, BRONZE.spec());
            px(im, x + 1, y + 1, BRONZE.get(1));
        }
    }

    static BufferedImage washerSide() {
        BufferedImage im = img();
        washerStaves(im, 4242);
        washerBand(im, 1, new int[] {1, 6, 11});
        washerBand(im, 12, new int[] {2, 7, 13});
        // shaft boss: bronze collar round an iron hub
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double d = Math.hypot(x - 7.5, y - 7.5);
                if (d > 4.4 && d <= 5.0) px(im, x, y, BRONZE.get(1));
                else if (d <= 4.4 && d > 2.8) px(im, x, y, BRONZE.get(x + y < 15 ? 5 : 3));
                else if (d <= 2.8 && d > 1.6) px(im, x, y, WROUGHT_IRON.get(x + y < 15 ? 2 : 4));
                else if (d <= 1.6) px(im, x, y, WROUGHT_IRON.get(1));
            }
        px(im, 5, 5, BRONZE.spec());
        px(im, 6, 6, WROUGHT_IRON.get(5));
        return im;
    }

    static BufferedImage washerTop() {
        BufferedImage im = img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                boolean in = x >= 2 && x <= 13 && y >= 2 && y <= 13;
                if (in) continue;
                boolean edge = x == 0 || y == 0;
                boolean lip = x == 1 || y == 1 || x == 14 || y == 14;
                px(im, x, y, edge ? TREATED_WOOD.get(4) : (x == 15 || y == 15) ? TREATED_WOOD.get(1) : TREATED_WOOD.get(lip ? 3 : 2));
            }
        for (int i = 4; i < 15; i += 5) { px(im, i, 15, TREATED_WOOD.get(2)); px(im, 15, i, TREATED_WOOD.get(2)); }
        for (int y = 2; y <= 13; y++)
            for (int x = 2; x <= 13; x++) {
                int c = WATER.get(((x * 3 + y * 5) % 11 == 0) ? 3 : 2);
                if (y == 2 || x == 2) c = WATER.get(1);
                px(im, x, y, c);
            }
        for (int[] f : new int[][] {{3, 12}, {4, 12}, {3, 11}, {12, 3}, {11, 3}, {12, 12}, {11, 13}, {13, 7}})
            px(im, f[0], f[1], WATER.get(5));
        px(im, 5, 4, WATER.get(4)); px(im, 10, 11, WATER.get(4)); px(im, 4, 10, WATER.get(4));
        // paddle drum: wooden body with bronze hoops
        for (int x = 4; x <= 11; x++) {
            boolean hoop = x == 4 || x == 11 || x == 7 || x == 8;
            px(im, x, 5, hoop ? BRONZE.get(4) : WOOD.get(4));
            px(im, x, 6, hoop ? BRONZE.get(3) : WOOD.get(3));
            px(im, x, 9, hoop ? BRONZE.get(3) : WOOD.get(2));
            px(im, x, 10, hoop ? BRONZE.get(2) : WOOD.get(1));
        }
        // axle across the tub, resting in the rim
        for (int x = 1; x <= 14; x++) {
            px(im, x, 7, x < 3 || x > 12 ? WROUGHT_IRON.get(5) : BRONZE.get(5));
            px(im, x, 8, x < 3 || x > 12 ? WROUGHT_IRON.get(2) : BRONZE.get(2));
        }
        px(im, 5, 7, BRONZE.spec());
        return im;
    }

    /**
     * One washer front frame: treated wood face, bronze bands, an open window onto a paddle drum with four
     * blades starting at angle a (degrees). Active adds sloshing water, foam and splash.
     */
    static BufferedImage washerFront(double a, int f, boolean active) {
        BufferedImage im = img();
        washerStaves(im, 5151);
        washerBand(im, 0, new int[] {2, 10});
        washerBand(im, 13, new int[] {3, 12});
        for (int y = 3; y <= 12; y++)
            for (int x = 3; x <= 12; x++) {
                boolean edge = x == 3 || y == 3 || x == 12 || y == 12;
                if (edge) px(im, x, y, BRONZE.get(x == 3 || y == 3 ? 4 : 2));
                else px(im, x, y, WATER.get(1));
            }
        px(im, 3, 3, BRONZE.spec());
        for (int x = 4; x <= 11; x++) px(im, x, 4, 0x10141c);
        double cx = 7.5, cy = 7.5;
        for (int y = 5; y <= 11; y++)
            for (int x = 4; x <= 11; x++) {
                double dx = x - cx, dy = y - cy;
                double r = Math.hypot(dx, dy);
                int col = -1;
                for (int k = 0; k < 4; k++) {
                    double th = Math.toRadians(a + k * 90);
                    double along = dx * Math.cos(th) + dy * Math.sin(th);
                    double perp = -dx * Math.sin(th) + dy * Math.cos(th);
                    if (along >= 1.0 && along <= 4.6 && Math.abs(perp) <= 1.05) {
                        boolean tip = along > 3.3;
                        if (perp < 0) col = tip ? WOOD.get(5) : BRONZE.get(5);
                        else col = tip ? WOOD.get(3) : BRONZE.get(4);
                        if (perp > 0.5) col = tip ? WOOD.get(2) : BRONZE.get(2);
                    }
                }
                if (r <= 1.6) col = r <= 0.9 ? WROUGHT_IRON.get(2) : BRONZE.get(x + y < 15 ? 5 : 3);
                if (col >= 0) px(im, x, y, col);
            }
        px(im, 7, 7, WROUGHT_IRON.get(5));
        double ph = f * Math.PI / 4.0;
        for (int x = 4; x <= 11; x++) {
            int surf = active ? 9 + (int) Math.round(Math.sin(ph + x * 0.9)) : 9;
            surf = Math.max(8, Math.min(10, surf));
            for (int y = surf; y <= 11; y++) {
                int under = rgb(im, x, y);
                boolean top = y == surf;
                int w = top ? WATER.get(4) : WATER.get(y == surf + 1 ? 3 : 2);
                px(im, x, y, top ? mix(under, w, 0.7) : mix(under, w, 0.45));
            }
            if (active && (x + f) % 3 == 0) px(im, x, surf, WATER.get(5));
            if (active && (x + f) % 4 == 1 && surf > 8) px(im, x, surf - 1, WATER.get(4));
        }
        if (!active) { px(im, 4, 9, WATER.get(5)); px(im, 5, 9, WATER.get(4)); px(im, 10, 9, WATER.get(5)); px(im, 11, 9, WATER.get(4)); }
        else {
            int[][] sp = {{5, 6}, {10, 6}, {6, 5}, {9, 5}};
            for (int k = 0; k < 2; k++) {
                int[] s = sp[(f + k * 2) % 4];
                int c = rgb(im, s[0], s[1]);
                if (c == WATER.get(1) || c == 0x10141c) px(im, s[0], s[1], WATER.get(5));
            }
        }
        for (int x = 4; x <= 11; x++) px(im, x, 12, BRONZE.get(2));
        return im;
    }

    static BufferedImage washerFrontActive() {
        BufferedImage strip = new BufferedImage(16, 128, BufferedImage.TYPE_INT_ARGB);
        for (int f = 0; f < 8; f++) strip.getGraphics().drawImage(washerFront(10 + f * 11.25, f, true), 0, f * 16, null);
        return strip;
    }

    /** Washer screen, 176x176: tank well with ticks, two input lanes, six outputs. */
    static BufferedImage washerGui() {
        BufferedImage im = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        panel(im, 176, 176);
        well(im, 8, 18, 12, 52, 0x2a2a2a);
        for (int y = 25; y <= 61; y += 12) {
            int len = y == 37 ? 4 : 2;
            fill(im, 21, y, len, 1, SLOT_DARK);
            fill(im, 21, y + 1, len, 1, GUI_LIGHT);
        }
        for (int y : new int[] {27, 45}) {
            slot(im, 30, y);
            arrow(im, 56, y);
        }
        for (int x : new int[] {98, 116, 134})
            for (int y : new int[] {27, 45}) slot(im, x, y);
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++) slot(im, 8 + col * 18, 94 + row * 18);
        for (int col = 0; col < 9; col++) slot(im, 8 + col * 18, 152);
        return im;
    }

    static void washer() throws IOException {
        save("block/washer_side", washerSide());
        save("block/washer_top", washerTop());
        save("block/washer_front", washerFront(22.5, 0, false));
        saveAnimated("block/washer_front_active", washerFrontActive(), 2);
        saveRaw("gui/washer", washerGui());
    }

    // ---------------------------------------------------------------- tier 4: blast furnace parts (spec 21.2)

    /** Blast furnace art: casing, controller, tuyere, hatches, blower, slag and the GUI sheet. */
    static final class Bf {
        static final Ramp SLAG_SPEC = ramp(0, 0x1e2024, 0x2e3236, 0x44484a, 0x5c6260, 0x787e78);
        /** Grey-green glass tint that sits between the slag steps. */
        static final Ramp SLAG_TINT = ramp(0, 0x34423c, 0x4a5e52, 0x647c6e, 0x839c8c, 0xa6bca8);
        static final int BORE = 0x0e0c0c;

        static final String[] SLAG_BLOB = {
                "....bcccc.....",
                "..bcdeeccccb..",
                ".bcde2ccccccc3",
                "bccc22cccce2c3",
                "bccccccccc22c3",
                "bcccccccccccc3",
                "bccce2cccccc23",
                "bccc22ccccccc3",
                ".bccccccccc332",
                "..22ccccc3322.",
                "....1222221...",
        };

        static final String[] GHOST_ORE = {
                "................",
                "................",
                "................",
                ".....#####......",
                "....#######.....",
                "...#########....",
                "...##########...",
                "..############..",
                "..############..",
                "...##########...",
                "....########....",
                "................",
                "................",
                "................",
                "................",
                "................",
        };
        static final String[] GHOST_COKE = {
                "................",
                "................",
                "................",
                "................",
                "....######......",
                "...########.....",
                "..##########....",
                "..###########...",
                "..############..",
                "...###########..",
                "....##########..",
                ".....########...",
                "......#####.....",
                "................",
                "................",
                "................",
        };
        static final String[] GHOST_HEAP = {
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "........##......",
                ".......####.....",
                "......######....",
                ".....########...",
                "...###########..",
                "..#############.",
                "................",
                "................",
                "................",
                "................",
        };

        static int iron(int step) { return WROUGHT_IRON.get(step); }

        /** Fire bricks (the plain block's own pattern) inside a 2 px wrought iron strap frame with corner rivets. */
        static BufferedImage casing() {
            BufferedImage im = fireBricks();
            for (int y = 0; y < 16; y++)
                for (int x = 0; x < 16; x++) {
                    if (x > 1 && x < 14 && y > 1 && y < 14) continue;
                    int step;
                    boolean outer = x == 0 || y == 0 || x == 15 || y == 15;
                    boolean lit = x <= 1 && y <= 14 || y <= 1 && x <= 14;
                    if (outer) step = (x == 15 || y == 15) ? 2 : 4;
                    else step = lit ? 3 : 2;
                    if (!outer && (x == 14 || y == 14) && !(x == 1 || y == 1)) step = 2;
                    px(im, x, y, iron(step));
                }
            // Inner edge of the strap throws a one-pixel shadow onto the bricks (top and left).
            for (int i = 2; i < 14; i++) {
                px(im, i, 2, FIRE_BRICK.get(1));
                px(im, 2, i, FIRE_BRICK.get(1));
            }
            for (int[] r : new int[][] {{0, 0}, {14, 0}, {0, 14}, {14, 14}}) {
                px(im, r[0], r[1], iron(5));
                px(im, r[0] + 1, r[1], iron(3));
                px(im, r[0], r[1] + 1, iron(3));
                px(im, r[0] + 1, r[1] + 1, iron(1));
            }
            return im;
        }

        static BufferedImage controller(int frame) {
            BufferedImage im = casing();
            int[] bright = {1, 2, 1, 0};
            Random r = new Random(9300 + frame);
            // Peephole: iron ring round a bore, centre (7.5, 6.5).
            for (int y = 2; y < 12; y++)
                for (int x = 3; x < 13; x++) {
                    double dx = x - 7.5 + 0.0, dy = y - 6.0;
                    double d = Math.hypot(dx, dy);
                    if (d > 4.3) continue;
                    if (d > 3.2) px(im, x, y, iron(dx + dy < 0 ? 5 : 2));
                    else if (d > 2.7) px(im, x, y, iron(dx + dy < 0 ? 2 : 4));
                    else {
                        int c = BORE;
                        if (frame >= 0) {
                            double g = d / 2.7;
                            int b = g < 0.55 ? 5 : g < 0.9 ? 4 : 3;
                            if (bright[frame] == 2 && g < 0.9) b = Math.min(5, b + 1);
                            if (bright[frame] == 0 && g > 0.5) b = Math.max(2, b - 1);
                            if (r.nextInt(4) == 0 && b > 3) b--;
                            c = HEAT_BAND[Math.min(5, b)];
                        } else if (dx + dy < -1.5) c = PIG_IRON.get(2);
                        px(im, x, y, c);
                    }
                }
            // Brass gauge, lower left.
            int gx = 3, gy = 10;
            px(im, gx + 1, gy, BRASS.get(5)); px(im, gx + 2, gy, BRASS.get(4));
            px(im, gx, gy + 1, BRASS.get(4)); px(im, gx + 1, gy + 1, 0xdcd8cf); px(im, gx + 2, gy + 1, 0xdcd8cf); px(im, gx + 3, gy + 1, BRASS.get(2));
            px(im, gx, gy + 2, BRASS.get(3)); px(im, gx + 1, gy + 2, 0xdcd8cf); px(im, gx + 2, gy + 2, BRASS.get(1)); px(im, gx + 3, gy + 2, BRASS.get(2));
            px(im, gx + 1, gy + 3, BRASS.get(2)); px(im, gx + 2, gy + 3, BRASS.get(2));
            // Status light, lower right: dull when cold, warm amber when lit.
            int lc = frame < 0 ? STRAW.get(2) : GOLD.get(5 - (frame == 1 ? 0 : 1));
            int lh = frame < 0 ? STRAW.get(3) : GOLD.spec();
            px(im, 10, 11, lh); px(im, 11, 11, lc);
            px(im, 10, 12, lc); px(im, 11, 12, frame < 0 ? STRAW.get(1) : GOLD.get(3));
            return im;
        }

        static BufferedImage controllerLit() {
            BufferedImage strip = new BufferedImage(16, 64, BufferedImage.TYPE_INT_ARGB);
            for (int f = 0; f < 4; f++) strip.getGraphics().drawImage(controller(f), 0, f * 16, null);
            return strip;
        }

        static int shade(double dx, double dy, double d) {
            double t = (dx + dy) / Math.max(d, 0.01);
            return t < -0.45 ? 5 : t < -0.1 ? 4 : t < 0.5 ? 3 : t < 0.8 ? 2 : 1;
        }

        static BufferedImage tuyereFront() {
            BufferedImage im = casing();
            for (int y = 2; y < 14; y++)
                for (int x = 2; x < 14; x++) {
                    double dx = x - 7.5, dy = y - 7.5, d = Math.hypot(dx, dy);
                    if (d <= 1.3) px(im, x, y, BORE);
                    else if (d <= 1.9) px(im, x, y, BRONZE.get(dx + dy < 0 ? 2 : 4));
                    else if (d <= 3.1) px(im, x, y, BRONZE.get(Math.min(5, shade(dx, dy, d))));
                    else if (d <= 4.0) px(im, x, y, COPPER.get(Math.min(5, shade(dx, dy, d))));
                    else if (d <= 5.0) px(im, x, y, COPPER.get(Math.max(1, Math.min(5, shade(dx, dy, d) - 1))));
                    else if (d <= 5.6 && dx + dy > 0) px(im, x, y, FIRE_BRICK.get(1));
                }
            px(im, 6, 5, BRONZE.spec());
            return im;
        }

        static BufferedImage tuyereSide() {
            BufferedImage im = casing();
            int[] rows = {5, 4, 4, 3, 2, 1};
            for (int i = 0; i < 6; i++)
                for (int x = 0; x < 16; x++) px(im, x, 5 + i, BRONZE.get(rows[i]));
            for (int x : new int[] {3, 12}) {
                for (int i = 0; i < 6; i++) px(im, x, 5 + i, COPPER.get(rows[i]));
                for (int i = 1; i < 5; i++) px(im, x + 1, 5 + i, COPPER.get(Math.min(rows[i], 3)));
                px(im, x, 4, COPPER.get(4)); px(im, x + 1, 4, COPPER.get(3));
                px(im, x, 11, COPPER.get(1)); px(im, x + 1, 11, COPPER.get(1));
            }
            px(im, 7, 5, BRONZE.spec()); px(im, 8, 5, BRONZE.spec());
            for (int x = 2; x < 14; x++) if (x != 3 && x != 4 && x != 12 && x != 13) px(im, x, 11, FIRE_BRICK.get(1));
            return im;
        }

        static BufferedImage hatchTop() {
            BufferedImage im = img();
            Random r = new Random(9410);
            for (int y = 0; y < 16; y++)
                for (int x = 0; x < 16; x++) {
                    int step = r.nextInt(5) == 0 ? 4 : 3;
                    if (x == 0 || y == 0) step = 5 - (r.nextInt(3) == 0 ? 1 : 0);
                    if (x == 15 || y == 15) step = 2;
                    px(im, x, y, iron(step));
                }
            // Opening x 3..12, y 3..12 over a dark shaft; the frame casts its shadow top and left.
            for (int y = 3; y <= 12; y++)
                for (int x = 3; x <= 12; x++) {
                    int c = PIG_IRON.get(1);
                    if (x == 3 || y == 3) c = BORE;
                    else if (x >= 9 && y >= 9) c = PIG_IRON.get(2);
                    px(im, x, y, c);
                }
            for (int i = 2; i <= 13; i++) { px(im, i, 2, iron(2)); px(im, 2, i, iron(2)); px(im, i, 13, iron(4)); px(im, 13, i, iron(4)); }
            px(im, 2, 2, iron(1));
            // Grate: three vertical bars and one cross bar, lit on top-left.
            for (int x : new int[] {5, 8, 11}) for (int y = 3; y <= 12; y++) px(im, x, y, iron(y == 3 ? 3 : 4));
            for (int x = 3; x <= 12; x++) if (x != 5 && x != 8 && x != 11) { px(im, x, 7, iron(4)); px(im, x, 8, iron(2)); }
            for (int x : new int[] {5, 8, 11}) { px(im, x, 7, iron(5)); px(im, x + 1 > 12 ? x : x + 1, 8, iron(2)); }
            for (int[] rv : new int[][] {{0, 0}, {14, 0}, {0, 14}, {14, 14}}) {
                px(im, rv[0] + 0, rv[1] + 0, iron(5));
                px(im, rv[0] + 1, rv[1] + 1, iron(1));
            }
            return im;
        }

        static BufferedImage hatchSide() {
            BufferedImage im = fireBricks();
            Random r = new Random(9420);
            for (int y = 0; y < 8; y++)
                for (int x = 0; x < 16; x++) {
                    int step = r.nextInt(5) == 0 ? 4 : 3;
                    if (y == 0) step = 5;
                    if (y == 6) step = 4;
                    if (y == 7) step = 1;
                    px(im, x, y, iron(step));
                }
            for (int x = 0; x < 16; x++) if (x % 5 != 2) { /* plate edge stays lit */ }
            for (int x : new int[] {2, 7, 12}) { px(im, x, 2, iron(5)); px(im, x + 1, 2, iron(2)); px(im, x, 3, iron(1)); }
            for (int x : new int[] {2, 7, 12}) { px(im, x, 5, iron(5)); px(im, x + 1, 5, iron(2)); }
            for (int x = 0; x < 16; x++) px(im, x, 8, FIRE_BRICK.get(1));
            return im;
        }

        static BufferedImage tapHatch(int frame) {
            BufferedImage im = casing();
            boolean hot = frame >= 0;
            int[] bright = {1, 2, 1, 0};
            Random r = new Random(9500 + frame);
            for (int y = 2; y < 13; y++)
                for (int x = 3; x < 13; x++) {
                    double dx = x - 7.5, dy = y - 6.5, d = Math.hypot(dx, dy);
                    if (d > 3.9) continue;
                    if (d > 3.0) { px(im, x, y, PIG_IRON.get(dx + dy < 0 ? 1 : 2)); continue; }
                    int c;
                    if (hot) {
                        int b = d < 1.6 ? 4 : 3;
                        b += bright[frame] == 2 ? 1 : 0;
                        if (bright[frame] == 0 && d > 1.6) b--;
                        if (r.nextInt(3) == 0) b = Math.max(2, b - 1);
                        c = HEAT_BAND[Math.min(5, b)];
                    } else {
                        c = CLAY.get(dx + dy < -1.2 ? 5 : dx + dy < 1.0 ? 4 : dx + dy < 2.4 ? 3 : 2);
                    }
                    px(im, x, y, c);
                }
            if (hot) {
                // Molten trickle from the bottom of the hole, down through the gap between the bars.
                int[] cols = {HEAT_BAND[4], HEAT_BAND[5], HEAT_BAND[4], HEAT_BAND[3]};
                for (int y = 10; y <= 14; y++) {
                    int k = (y + frame) % 4;
                    px(im, 7, y, y == 10 ? HEAT_BAND[4] : cols[k]);
                    px(im, 8, y, y == 10 ? HEAT_BAND[5] : cols[(k + 2) % 4]);
                    if (y >= 11 && (y + frame) % 3 == 0) px(im, 6 + (frame % 2) * 3, y, HEAT_BAND[2]);
                }
                px(im, 7, 15, HEAT_BAND[3]); px(im, 8, 15, HEAT_BAND[2]);
                px(im, 7, 10, HEAT_BAND[4]); px(im, 8, 10, HEAT_BAND[4]);
            }
            // Bars: two 2 px vertical iron bars with end rivets.
            for (int x0 : new int[] {3, 11})
                for (int y = 1; y <= 14; y++) {
                    px(im, x0, y, iron(y == 1 ? 5 : 4));
                    px(im, x0 + 1, y, iron(2));
                }
            for (int x0 : new int[] {3, 11}) { px(im, x0, 2, iron(5)); px(im, x0, 13, iron(5)); }
            return im;
        }

        static BufferedImage tapHatchHot() {
            BufferedImage strip = new BufferedImage(16, 64, BufferedImage.TYPE_INT_ARGB);
            for (int f = 0; f < 4; f++) strip.getGraphics().drawImage(tapHatch(f), 0, f * 16, null);
            return strip;
        }

        static BufferedImage blowerFront() {
            BufferedImage im = img();
            Random r = new Random(9600);
            for (int y = 0; y < 16; y++)
                for (int x = 0; x < 16; x++) {
                    boolean inner = x >= 3 && x <= 12 && y >= 3 && y <= 12;
                    if (inner) {
                        int c = PIG_IRON.get(r.nextInt(7) == 0 ? 2 : 1);
                        if (x <= 4 || y <= 4) c = (x <= 3 || y <= 3) ? BORE : PIG_IRON.get(1);
                        px(im, x, y, c);
                    } else if (x == 2 || y == 2 || x == 13 || y == 13) {
                        boolean lit = (x == 2 && y <= 13) || (y == 2 && x <= 13);
                        px(im, x, y, LEATHER.get(lit ? 4 : 2));
                    } else {
                        int step = 3;
                        if (x == 0 || y == 0) step = 4;
                        else if (x == 15 || y == 15) step = 2;
                        else if (x == 14 || y == 14) step = 2;
                        else if (r.nextInt(6) == 0) step = 4;
                        px(im, x, y, iron(step));
                    }
                }
            px(im, 2, 2, LEATHER.get(3)); px(im, 13, 13, LEATHER.get(1));
            // Rivets in the border: corners and the middle of each side.
            for (int[] rv : new int[][] {{1, 1}, {14, 1}, {1, 14}, {14, 14}, {7, 1}, {1, 7}, {14, 8}, {8, 14}}) {
                px(im, rv[0], rv[1], iron(5));
            }
            // Fan shaft hub in the middle of the back wall.
            int[][] hub = {{7, 6, 4}, {8, 6, 3}, {6, 7, 4}, {7, 7, 5}, {8, 7, 3}, {9, 7, 2}, {6, 8, 3}, {7, 8, 3}, {8, 8, 2}, {9, 8, 1}, {7, 9, 2}, {8, 9, 1}};
            for (int[] h : hub) px(im, h[0], h[1], STEEL.get(h[2]));
            return im;
        }

        static void plate(BufferedImage im, long seed) {
            Random r = new Random(seed);
            for (int y = 0; y < 16; y++)
                for (int x = 0; x < 16; x++) {
                    int step = r.nextInt(6) == 0 ? 4 : r.nextInt(8) == 0 ? 2 : 3;
                    if (x == 0 || y == 0) step = 4;
                    if (x == 15 || y == 15) step = 2;
                    px(im, x, y, iron(step));
                }
        }

        static void rivetAt(BufferedImage im, int x, int y) {
            px(im, x, y, iron(5));
            px(im, x + 1, y, iron(2));
            px(im, x, y + 1, iron(1));
        }

        static BufferedImage blowerSide() {
            BufferedImage im = img();
            plate(im, 9610);
            for (int y = 0; y < 16; y++) { px(im, 7, y, iron(1)); px(im, 8, y, iron(5)); px(im, 9, y, iron(3)); }
            for (int y : new int[] {2, 7, 12}) { rivetAt(im, 5, y); rivetAt(im, 11, y); }
            for (int x = 1; x < 15; x++) if (x < 7 || x > 9) { px(im, x, 0, iron(5)); px(im, x, 15, iron(1)); }
            return im;
        }

        static BufferedImage blowerBack() {
            BufferedImage im = img();
            plate(im, 9620);
            for (int[] rv : new int[][] {{1, 1}, {12, 1}, {1, 12}, {12, 12}}) rivetAt(im, rv[0], rv[1]);
            // Bearing ring x 4..11, square axle socket x 6..9.
            for (int y = 4; y <= 11; y++)
                for (int x = 4; x <= 11; x++) {
                    boolean edge = x == 4 || y == 4 || x == 11 || y == 11;
                    if (edge) px(im, x, y, BRONZE.get(x == 4 || y == 4 ? 4 : 2));
                    else px(im, x, y, BRONZE.get(x == 5 || y == 5 ? 3 : 1));
                }
            for (int y = 6; y <= 9; y++)
                for (int x = 6; x <= 9; x++) px(im, x, y, (x == 6 || y == 6) ? BORE : PIG_IRON.get(1));
            px(im, 9, 9, PIG_IRON.get(2)); px(im, 4, 4, BRONZE.get(5));
            return im;
        }

        static BufferedImage blowerFan() {
            BufferedImage im = img();
            Random r = new Random(9630);
            int[] colStep = {2, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 3, 3, 3, 3, 2};
            for (int y = 0; y < 16; y++)
                for (int x = 0; x < 16; x++) {
                    int step = colStep[x];
                    if (r.nextInt(9) == 0 && step == 4) step = 3;
                    px(im, x, y, iron(step));
                }
            for (int y = 5; y <= 10; y++)
                for (int x = 5; x <= 10; x++) {
                    double dx = x - 7.5, dy = y - 7.5, d = Math.hypot(dx, dy);
                    if (d > 3.3) continue;
                    int step = d > 2.6 ? (dx + dy < 0 ? 4 : 2) : shade(dx, dy, d);
                    if (d > 2.6 && dx + dy > 0) step = 1;
                    px(im, x, y, BRASS.get(Math.max(1, Math.min(5, step))));
                }
            px(im, 7, 7, BRASS.get(2)); px(im, 8, 8, BRASS.get(2));
            px(im, 7, 6, BRASS.spec());
            return im;
        }

        /** The shared dust heap in the slag ramp, with the grey-green tint on its lit side. */
        static BufferedImage slagDust() {
            String[] rows = DUST_HEAP.clone();
            for (int i = 0; i < rows.length; i++) rows[i] = rows[i].replace('5', 'd').replace('4', 'c');
            return map(SLAG_SPEC, SLAG_TINT, rows);
        }

        static BufferedImage slagItem() {
            BufferedImage im = img();
            int top = 3, left = 1;
            for (int y = 0; y < SLAG_BLOB.length; y++)
                for (int x = 0; x < SLAG_BLOB[y].length(); x++) {
                    char ch = SLAG_BLOB[y].charAt(x);
                    int c;
                    if (ch >= '1' && ch <= '5') c = SLAG_SPEC.get(ch - '0');
                    else if (ch >= 'b' && ch <= 'e') c = SLAG_TINT.get(ch - 'a' + 1);
                    else continue;
                    px(im, left + x, top + y, c);
                }
            return outline(im);
        }

        // ---- GUI

        static void gaugeFill(BufferedImage im, int u, int v, int[] c, long seed, int sparkle) {
            Random r = new Random(seed);
            for (int y = 0; y < 6; y++)
                for (int x = 0; x < 50; x++) {
                    int k = y == 0 ? c[4] : y >= 5 ? c[1] : y == 4 ? c[2] : c[3];
                    im.setRGB(u + x, v + y, 0xff000000 | k);
                }
            for (int i = 0; i < 14; i++) {
                int x = 1 + r.nextInt(46), y = 1 + r.nextInt(3), w = 2 + r.nextInt(2);
                for (int k = 0; k < w; k++) im.setRGB(u + x + k, v + y, 0xff000000 | c[2]);
                im.setRGB(u + x, v + y - 1, 0xff000000 | c[4]);
            }
            for (int i = 0; i < sparkle; i++) {
                int x = 2 + r.nextInt(46), y = 1 + r.nextInt(3);
                im.setRGB(u + x, v + y, 0xff000000 | c[0]);
            }
        }

        static void gaugeFill30(BufferedImage im, int u, int v, int[] c, long seed) {
            Random r = new Random(seed);
            for (int y = 0; y < 6; y++)
                for (int x = 0; x < 30; x++) {
                    int k = y == 0 ? c[4] : y >= 5 ? c[1] : y == 4 ? c[2] : c[3];
                    im.setRGB(u + x, v + y, 0xff000000 | k);
                }
            for (int i = 0; i < 8; i++) {
                int x = 1 + r.nextInt(26), y = 1 + r.nextInt(3), w = 2 + r.nextInt(2);
                for (int k = 0; k < w; k++) im.setRGB(u + x + k, v + y, 0xff000000 | c[2]);
                im.setRGB(u + x, v + y - 1, 0xff000000 | c[4]);
            }
        }

        static BufferedImage gui() {
            BufferedImage im = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
            panel(im, 176, 190);
            int[] sy = {18, 38, 58};
            for (int y : sy) slot(im, 8, y);
            ghost(im, 8, 18, edgeRows(GHOST_ORE));
            ghost(im, 8, 38, edgeRows(GHOST_COKE));
            ghost(im, 8, 58, edgeRows(GHOST_HEAP));
            for (int y : new int[] {23, 43, 63}) well(im, 29, y - 1, 52, 8, 0x2a2a2a);
            well(im, 87, 17, 12, 58, 0x2a2a2a);
            arrow(im, 104, 36);
            slot(im, 132, 36);
            slot(im, 152, 36);
            for (int row = 0; row < 3; row++)
                for (int col = 0; col < 9; col++) slot(im, 8 + col * 18, 108 + row * 18);
            for (int col = 0; col < 9; col++) slot(im, 8 + col * 18, 166);
            // Gauge fills.
            gaugeFill(im, 176, 0, new int[] {MAGNETITE.get(5), HEMATITE.get(2), MAGNETITE.get(4), HEMATITE.get(4), HEMATITE.get(5)}, 9701, 3);
            gaugeFill(im, 176, 6, new int[] {COKE.get(5), COKE.get(1), COKE.get(2), COKE.get(3), COKE.get(4)}, 9702, 4);
            for (int i = 0; i < 4; i++) im.setRGB(176 + 5 + i * 12, 6 + 2, 0xff000000 | 0x9a9aa8);
            gaugeFill(im, 176, 12, new int[] {LIMESTONE.get(3), LIMESTONE.get(2), LIMESTONE.get(4), LIMESTONE.get(5), 0xe8e0c4}, 9703, 0);
            for (int x = 0; x < 50; x++) im.setRGB(176 + x, 12, 0xff000000 | LIMESTONE.get(5));
            // Hearth heat gradient, 10x56, hot at the top.
            int[] bandH = {7, 9, 11, 11, 10, 8};
            int[] bandC = {5, 4, 3, 2, 1, 0};
            int y = 0;
            for (int b = 0; b < bandH.length; b++)
                for (int k = 0; k < bandH[b]; k++, y++)
                    for (int x = 0; x < 10; x++) {
                        int c = HEAT_BAND[bandC[b]];
                        if (x == 0) c = mix(c, 0xfff4d0, 0.18);
                        if (x == 9) c = mix(c, 0x3a1410, 0.28);
                        im.setRGB(176 + x, 18 + y, 0xff000000 | c);
                    }
            // Progress arrow fill, same shape and light tone as the other guis' filled arrows.
            for (int j = 0; j < 15; j++) {
                int d = Math.abs(j - 7);
                if (d <= 2) for (int i = 0; i < 15; i++) im.setRGB(188 + i, 18 + j, 0xff000000 | GUI_LIGHT);
                for (int i = 15; i < 22 - d; i++) im.setRGB(188 + i, 18 + j, 0xff000000 | GUI_LIGHT);
            }
            return im;
        }
    }

    static void blastFurnace() throws IOException {
        save("block/refractory_casing", Bf.casing());
        save("block/blast_furnace_controller_front", Bf.controller(-1));
        saveAnimated("block/blast_furnace_controller_front_lit", Bf.controllerLit(), 3);
        save("block/tuyere_front", Bf.tuyereFront());
        save("block/tuyere_side", Bf.tuyereSide());
        save("block/charging_hatch_top", Bf.hatchTop());
        save("block/charging_hatch_side", Bf.hatchSide());
        save("block/tap_hatch_front", Bf.tapHatch(-1));
        saveAnimated("block/tap_hatch_front_hot", Bf.tapHatchHot(), 3);
        save("block/blower_front", Bf.blowerFront());
        save("block/blower_side", Bf.blowerSide());
        save("block/blower_back", Bf.blowerBack());
        save("block/blower_fan", Bf.blowerFan());
        save("item/slag", Bf.slagItem());
        save("item/slag_dust", Bf.slagDust());
        saveRaw("gui/blast_furnace", Bf.gui());
    }

    static final class Cv {
        static int st(int step) { return STEEL.get(step); }

        static BufferedImage front(int frame) {
            BufferedImage im = Bf.casing();
            boolean hot = frame >= 0;
            // Steel bands: one under the rim slot, one through the trunnion axle.
            for (int[] b : new int[][] {{5, 6}, {9, 10}}) {
                for (int x = 2; x < 14; x++) {
                    px(im, x, b[0], st(x % 5 == 2 ? 5 : 4));
                    px(im, x, b[1], st(2));
                }
                px(im, 2, b[0], st(5)); px(im, 13, b[0], st(3));
                for (int x = 2; x < 14; x++) px(im, x, b[1] + 1, FIRE_BRICK.get(1));
                for (int x : new int[] {3, 12}) px(im, x, b[0], st(5));
            }
            // Rim slot near the top: dark mouth, steel lip below it.
            int[][] fl = {{2, 3, 2, 3, 2, 2, 3, 2}, {3, 2, 3, 2, 3, 3, 2, 2}, {2, 3, 3, 2, 3, 2, 3, 3}, {3, 2, 2, 3, 2, 3, 2, 3}};
            for (int y = 3; y <= 4; y++)
                for (int x = 4; x <= 11; x++) {
                    int c = (y == 3 || x == 4) ? 0x050404 : Bf.BORE;
                    if (hot) {
                        int h = fl[frame][x - 4];
                        int depth = 4 - y; // 0 at the lip, 1 above
                        if (depth < h - 1) {
                            c = depth == 0 ? ((x + frame) % 3 == 0 ? 0xfffbe6 : 0xffe488) : HEAT_BAND[4];
                        } else if (depth == 0) c = HEAT_BAND[3];
                        else c = mix(Bf.BORE, HEAT_BAND[1], 0.5);
                        if (y == 3 && h == 3) c = HEAT_BAND[(x + frame) % 2 == 0 ? 4 : 3];
                    }
                    px(im, x, y, c);
                }
            if (hot) for (int x = 3; x <= 12; x++) px(im, x, 2, mix(FIRE_BRICK.get(3), HEAT_BAND[2], ((x + frame) % 3 == 0) ? 0.6 : 0.4));
            // Trunnion hub: steel ring round a dark bearing, lit top left.
            double cx = 7.5, cy = 9.5;
            for (int y = 6; y < 14; y++)
                for (int x = 3; x < 13; x++) {
                    double dx = x - cx, dy = y - cy, d = Math.hypot(dx, dy);
                    if (d > 3.5) continue;
                    int c;
                    double t = dx + dy;
                    if (d > 2.7) c = t < 0 ? st(5) : st(1);
                    else if (d > 1.5) c = t < -1 ? st(4) : t < 1.5 ? st(3) : st(2);
                    else c = t < 0 ? PIG_IRON.get(2) : Bf.BORE;
                    if (hot && d <= 2.7) c = mix(c, HEAT_BAND[d < 1.6 ? 4 : 3], d < 1.6 ? 0.6 : 0.4);
                    else if (hot) c = mix(c, HEAT_BAND[3], 0.2);
                    px(im, x, y, c);
                }
            // Brass pressure gauge, lower right.
            int gx = 11, gy = 11;
            px(im, gx + 1, gy, BRASS.get(5));
            px(im, gx, gy + 1, BRASS.get(4)); px(im, gx + 1, gy + 1, 0xdcd8cf); px(im, gx + 2, gy + 1, BRASS.get(2));
            px(im, gx, gy + 2, BRASS.get(3)); px(im, gx + 1, gy + 2, BRASS.get(1)); px(im, gx + 2, gy + 2, BRASS.get(2));
            return im;
        }

        static BufferedImage blowing() {
            BufferedImage strip = new BufferedImage(16, 64, BufferedImage.TYPE_INT_ARGB);
            for (int f = 0; f < 4; f++) strip.getGraphics().drawImage(front(f), 0, f * 16, null);
            return strip;
        }

        static final String[] GHOST_INGOT = {
                "................", "................", "................", "................", "................",
                "....########....", "...##########...", "..############..", "..############..",
                ".##############.", ".##############.", "................", "................", "................", "................", "................",
        };
        static final String[] GHOST_SCRAP = {
                "................", "................", "...........##...", "..........###...", ".........###....",
                "........###.....", ".......###......", "......###.......", ".....###........",
                "....###.........", "...###..........", "..###...........", "................", "................", "................", "................",
        };

        static BufferedImage gui() {
            BufferedImage im = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
            panel(im, 176, 190);
            for (int y : new int[] {18, 38, 58}) slot(im, 8, y);
            ghost(im, 8, 18, edgeRows(GHOST_INGOT));
            ghost(im, 8, 38, edgeRows(GHOST_SCRAP));
            ghost(im, 8, 58, edgeRows(Bf.GHOST_COKE));
            for (int y : new int[] {23, 43, 63}) well(im, 29, y - 1, 32, 8, 0x2a2a2a);
            well(im, 79, 17, 16, 58, 0x2a2a2a);
            arrow(im, 104, 36);
            slot(im, 132, 36);
            slot(im, 152, 36);
            for (int row = 0; row < 3; row++)
                for (int col = 0; col < 9; col++) slot(im, 8 + col * 18, 108 + row * 18);
            for (int col = 0; col < 9; col++) slot(im, 8 + col * 18, 166);
            // Flame column 14x56, hot at the top: bottom deep orange, through yellow, to near white.
            Random r = new Random(9801);
            for (int y = 0; y < 56; y++) {
                double t = 1.0 - y / 55.0; // 1 at top
                for (int x = 0; x < 14; x++) {
                    double e = Math.abs(x - 6.5) / 6.5;
                    double v = t + (r.nextInt(5) - 2) * 0.012 - e * 0.10 * (1 - t) ;
                    v = Math.max(0, Math.min(1, v));
                    double f = v * 5;
                    int i0 = (int) Math.min(4, Math.floor(f));
                    int c = mix(HEAT_BAND[i0], HEAT_BAND[i0 + 1], f - i0);
                    if (v < 0.04) c = HEAT_BAND[1];
                    if (x == 0) c = mix(c, 0xfff4d0, 0.15);
                    if (x == 13) c = mix(c, 0x3a1410, 0.25);
                    im.setRGB(176 + x, y, 0xff000000 | c);
                }
            }
            for (int j = 0; j < 15; j++) {
                int d = Math.abs(j - 7);
                if (d <= 2) for (int i = 0; i < 15; i++) im.setRGB(192 + i, j, 0xff000000 | GUI_LIGHT);
                for (int i = 15; i < 22 - d; i++) im.setRGB(192 + i, j, 0xff000000 | GUI_LIGHT);
            }
            Bf.gaugeFill30(im, 192, 16, new int[] {PIG_IRON.get(5), PIG_IRON.get(1), PIG_IRON.get(2), PIG_IRON.get(3), PIG_IRON.get(4)}, 9811);
            Bf.gaugeFill30(im, 192, 22, new int[] {STEEL.get(5), STEEL.get(1), STEEL.get(2), STEEL.get(3), STEEL.get(4)}, 9812);
            Bf.gaugeFill30(im, 192, 28, new int[] {COKE.get(5), COKE.get(1), COKE.get(2), COKE.get(3), COKE.get(4)}, 9813);
            return im;
        }
    }

    /** Heat pipes and ducts: copper tube with soldered joints, fire brick sleeve with steel bands, and the inlet flange. */
    static final class Hp {
        static final int BORE = 0x0e0c0c;
        static int st(int s) { return STEEL.get(s); }
        static int cu(int s) { return COPPER.get(s); }
        static int so(int s) { return SOLDER.get(s); }
        static int fb(int s) { return FIRE_BRICK.get(s); }

        /** Heat band colour for glow g in 0..1: dull red up to yellow. */
        static int band(double g) {
            double f = Math.max(0, Math.min(1, g)) * 4;
            int i = (int) Math.min(3, Math.floor(f));
            return mix(HEAT_BAND[i], HEAT_BAND[i + 1], f - i);
        }
        static void glow(BufferedImage im, int x, int y, double g, double strength) {
            if (g <= 0) return;
            px(im, x, y, mix(rgb(im, x, y), band(g), Math.min(1, g * strength)));
        }

        // ---- copper pipe
        static BufferedImage copper(boolean hot) {
            BufferedImage im = img();
            int[] across = {5, 4, 4, 3, 3, 2, 2};
            double[] mid = {0.30, 0.52, 0.72, 0.85, 0.72, 0.52, 0.30};
            // Tube side everywhere first (plain tube for break particles).
            for (int y = 0; y < 16; y++)
                for (int x = 0; x < 16; x++) {
                    int a = y % 7;
                    int step = across[a];
                    if (x == 3) step = 0; // solder band marker
                    if (step == 0) px(im, x, y, so(a == 0 ? 5 : a < 3 ? 4 : a < 5 ? 3 : 2));
                    else px(im, x, y, cu(step));
                    if (y < 7 && x == 1 && a == 1) px(im, x, y, COPPER.spec());
                }
            // Hub: collar block with a solder ring round a copper boss.
            for (int y = 4; y <= 11; y++)
                for (int x = 4; x <= 11; x++) {
                    boolean edge = x == 4 || y == 4 || x == 11 || y == 11;
                    boolean ring = !edge && (x == 5 || y == 5 || x == 10 || y == 10);
                    int c;
                    if (edge) c = cu(x == 4 || y == 4 ? 5 : 2);
                    else if (ring) c = so(x == 5 || y == 5 ? 5 : x == 10 || y == 10 ? 2 : 4);
                    else c = cu((x + y) < 15 ? 4 : 3);
                    px(im, x, y, c);
                }
            px(im, 6, 6, COPPER.spec());
            px(im, 9, 9, cu(2)); px(im, 9, 8, cu(2)); px(im, 8, 9, cu(2));
            // End: copper ring round a dark bore.
            for (int y = 9; y <= 15; y++)
                for (int x = 0; x <= 6; x++) {
                    int dx = Math.min(x, 6 - x), dy = Math.min(y - 9, 15 - y), d = Math.min(dx, dy);
                    boolean lit = x + (y - 9) < 6;
                    int c;
                    if (d == 0) c = cu(lit ? 5 : 2);
                    else if (d == 1) c = cu(lit ? 4 : 3);
                    else c = (y == 11 || x == 2) ? cu(1) : BORE;
                    px(im, x, y, c);
                }
            if (hot) {
                for (int y = 0; y < 16; y++)
                    for (int x = 0; x < 16; x++) {
                        boolean hub = x >= 4 && x <= 11 && y >= 4 && y <= 11;
                        boolean end = x <= 6 && y >= 9;
                        if (hub) {
                            double d = Math.hypot(x - 7.5, y - 7.5);
                            glow(im, x, y, Math.max(0.2, 0.9 - d * 0.14), 0.85);
                        } else if (end) {
                            int dx = Math.min(x, 6 - x), dy = Math.min(y - 9, 15 - y), d = Math.min(dx, dy);
                            if (d >= 2) px(im, x, y, band(d == 2 ? 0.85 : 1.0));
                            else glow(im, x, y, d == 1 ? 0.6 : 0.4, 0.85);
                        } else glow(im, x, y, mid[y % 7], 0.85);
                    }
                // Hot spots on the tube: lit crest stays brighter.
                for (int y = 0; y < 16; y++) if (y % 7 == 3) for (int x = 0; x < 4; x++) if (y < 7) px(im, x, y, band(x == 3 ? 0.9 : 1.0));
            }
            return im;
        }

        // ---- refractory duct
        static BufferedImage duct(boolean hot) {
            BufferedImage im = img();
            // Side: brick courses (7 rows: 3 brick, mortar, 3 brick), staggered joints, steel band at x=3.
            int[] rowStep = {5, 4, 3, 1, 5, 4, 3};
            for (int y = 0; y < 16; y++)
                for (int x = 0; x < 16; x++) {
                    int a = y % 7;
                    int step = rowStep[a];
                    int joint = a < 3 ? 1 : 2;
                    if (step != 1 && x == joint) step = 1;
                    if (step != 1 && x == joint + 1 && a != 0) step = Math.max(2, step - 1);
                    if (step != 1) step = Math.max(2, step);
                    px(im, x, y, fb(step));
                }
            for (int y = 0; y < 16; y++) {
                int a = y % 7;
                px(im, 3, y, st(a == 0 ? 5 : a < 3 ? 4 : a < 5 ? 3 : 2));
            }
            // Hub: brick block, one mortar course and a staggered joint, steel brackets in each corner.
            for (int y = 4; y <= 11; y++)
                for (int x = 4; x <= 11; x++) {
                    int step = (y - 4) % 4 == 0 ? 5 : 4;
                    if (y == 7 || y == 11) step = 1;
                    if (y < 7 && x == 8 && y != 7) step = 1;
                    if (y > 7 && y < 11 && x == 6) step = 1;
                    if (step != 1 && (x == 11)) step = 3;
                    px(im, x, y, fb(step));
                }
            // Corner brackets: 3x3 L shapes of steel, lit top-left.
            for (int[] c : new int[][] {{4, 4}, {9, 4}, {4, 9}, {9, 9}}) {
                for (int j = 0; j < 3; j++)
                    for (int i = 0; i < 3; i++) {
                        boolean left = c[0] == 4, top = c[1] == 4;
                        boolean onX = left ? i == 0 : i == 2, onY = top ? j == 0 : j == 2;
                        if (!(onX || onY)) continue;
                        int s = 3;
                        if (j == 0 && top || i == 0 && left) s = 5;
                        if (j == 2 && !top || i == 2 && !left) s = 2;
                        if (i == (left ? 0 : 2) && j == (top ? 0 : 2)) s = left && top ? 5 : left || top ? 4 : 2;
                        px(im, c[0] + i, c[1] + j, st(s));
                    }
            }
            // End: square brick sleeve round a dark square bore, steel corner pins.
            for (int y = 9; y <= 15; y++)
                for (int x = 0; x <= 6; x++) {
                    int dx = Math.min(x, 6 - x), dy = Math.min(y - 9, 15 - y), d = Math.min(dx, dy);
                    boolean lit = x + (y - 9) < 6;
                    int c;
                    if (d == 0) c = fb(lit ? 5 : 3);
                    else if (d == 1) c = fb(lit ? 4 : 2);
                    else c = (y == 11 || x == 2) ? fb(1) : BORE;
                    px(im, x, y, c);
                }
            px(im, 0, 9, st(5)); px(im, 6, 9, st(4)); px(im, 0, 15, st(3)); px(im, 6, 15, st(2));
            if (hot) {
                for (int y = 0; y < 16; y++)
                    for (int x = 0; x < 16; x++) {
                        int c = rgb(im, x, y);
                        boolean hub = x >= 4 && x <= 11 && y >= 4 && y <= 11;
                        boolean end = x <= 6 && y >= 9;
                        boolean mortar = c == fb(1);
                        boolean steel = false;
                        for (int s = 1; s <= 5; s++) if (c == st(s)) steel = true;
                        if (end) {
                            int dx = Math.min(x, 6 - x), dy = Math.min(y - 9, 15 - y), d = Math.min(dx, dy);
                            if (d >= 2 && !mortar) px(im, x, y, band(1.0));
                            else if (mortar) px(im, x, y, band(0.7));
                            else if (!steel) glow(im, x, y, d == 1 ? 0.45 : 0.3, 0.8);
                            continue;
                        }
                        if (mortar) { px(im, x, y, band(0.78)); continue; }
                        if (steel) { glow(im, x, y, 0.2, 0.5); continue; }
                        if (hub) {
                            double d = Math.hypot(x - 7.5, y - 7.5);
                            glow(im, x, y, Math.max(0.15, 0.7 - d * 0.1), 0.8);
                        } else glow(im, x, y, 0.32, 0.8);
                    }
                // Heat seeps along the middle mortar course of the side.
                for (int y = 0; y < 16; y++) if (y % 7 == 3 && !(y >= 4 && y <= 11)) for (int x = 0; x < 3; x++) px(im, x, y, band(0.95));
            }
            return im;
        }

        // ---- heat inlet: refractory casing with a round steel duct flange
        static BufferedImage inlet() {
            BufferedImage im = Bf.casing();
            double cx = 7.5, cy = 7.5;
            for (int y = 2; y < 14; y++)
                for (int x = 2; x < 14; x++) {
                    double dx = x - cx, dy = y - cy, d = Math.hypot(dx, dy);
                    if (d > 5.3) continue;
                    double t = dx + dy;
                    int c;
                    if (d > 4.6) c = t < 0 ? st(5) : st(2);
                    else if (d > 2.9) c = t < 1 ? st(3) : st(2);
                    else if (d > 2.2) c = t < 0 ? st(1) : st(3);
                    else c = BORE;
                    px(im, x, y, c);
                }
            // Bolt heads on the flange face (pinwheel), lit top-left with a shadow pixel.
            for (int[] b : new int[][] {{4, 6}, {9, 4}, {11, 9}, {6, 11}}) {
                px(im, b[0], b[1], st(5));
                px(im, b[0] + 1, b[1] + 1, st(1));
            }
            return im;
        }
    }

    static void heatPipes() throws IOException {
        save("block/copper_heat_pipe", Hp.copper(false));
        save("block/copper_heat_pipe_hot", Hp.copper(true));
        save("block/refractory_heat_duct", Hp.duct(false));
        save("block/refractory_heat_duct_hot", Hp.duct(true));
        save("block/heat_inlet", Hp.inlet());
    }

    // ---------------------------------------------------------------- tier 4: kiln and insulated heat lines

    static final class Kn {
        static int fb(int s) { return FIRE_BRICK.get(s); }
        static int st(int s) { return STEEL.get(s); }
        static int pi(int s) { return PIG_IRON.get(s); }
        static int cu(int s) { return COPPER.get(s); }

        // ---- fire brick faces
        static boolean isMortar(BufferedImage im, int x, int y) { return rgb(im, x, y) == fb(1); }

        /** Fire brick ring-and-radial joints around a centre, between radii r0 and r1 (dome voussoirs). */
        static BufferedImage front(boolean active) {
            BufferedImage im = fireBricks();
            double cx = 7.5, cy = 8.0;
            // Voussoir ring round the arch: radial joints every ~30 degrees, keystone lit.
            for (int y = 0; y < 8; y++)
                for (int x = 0; x < 16; x++) {
                    double dx = x - cx, dy = y - cy, d = Math.hypot(dx, dy);
                    if (d < 5.2 || d > 7.8) continue;
                    double ang = Math.toDegrees(Math.atan2(-dy, dx)); // 0 right, 90 up
                    int sector = (int) Math.floor((ang + 15) / 30.0);
                    double edge = ((ang + 15) % 30 + 30) % 30;
                    boolean joint = edge < 7.0 / Math.max(d, 1) * 8 && edge < 6.0 && d > 5.2;
                    boolean lit = dx + dy < -1;
                    int step = lit ? 5 : (dx > 1.5 ? 3 : 4);
                    if (Math.abs(sector - 3) == 0 && d < 7.0) step = 5;
                    if (joint) step = 1;
                    else if (d > 7.0 && step > 3) step--;
                    px(im, x, y, fb(step));
                }
            // Door opening, 8 wide, round-headed: cells by row range.
            int[][] span = {{6, 9}, {5, 10}, {4, 11}, {4, 11}};
            int top = 3, bottom = 14;
            boolean[][] open = new boolean[16][16];
            for (int y = top; y <= bottom; y++) {
                int a = y - top < 3 ? span[y - top][0] : 4, b = y - top < 3 ? span[y - top][1] : 11;
                for (int x = a; x <= b; x++) open[y][x] = true;
            }
            // Frame ring (dark iron) around the opening, lit top-left.
            for (int y = 0; y < 16; y++)
                for (int x = 0; x < 16; x++) {
                    if (open[y][x]) continue;
                    boolean near = false;
                    for (int j = -1; j <= 1; j++)
                        for (int i = -1; i <= 1; i++) {
                            int nx = x + i, ny = y + j;
                            if (nx >= 0 && ny >= 0 && nx < 16 && ny < 16 && open[ny][nx]) near = true;
                        }
                    if (!near) continue;
                    boolean up = y < 9 && (x + y < 12 || x - y > 3 ? true : false);
                    int s = (x <= 3 || y <= 3 && x < 8) ? 4 : 2;
                    if (y == bottom + 1) s = 2;
                    if ((x == 2 && y == 6) || (x == 13 && y == 6) || (x == 2 && y == 13) || (x == 13 && y == 13)) s = 5;
                    px(im, x, y, pi(s));
                }
            // Door leaf inside a 1 px gap.
            for (int y = top; y <= bottom; y++)
                for (int x = 3; x <= 12; x++) {
                    if (!open[y][x]) continue;
                    boolean leaf = isLeaf(open, x, y);
                    int c;
                    if (!leaf) c = active ? Hp.band(y > 8 ? 0.85 : 0.65) : 0x16110f;
                    else {
                        int s = 3;
                        if (x == 5 || y == top + 1 && x < 8 || y == top + 1 && x == 5) s = 4;
                        if (x == 10 || y == bottom - 1) s = 2;
                        c = pi(s);
                    }
                    px(im, x, y, c);
                }
            // Vent slits and handle.
            for (int x = 6; x <= 9; x++) {
                px(im, x, 7, active ? Hp.band(1.0) : pi(1));
                px(im, x, 11, active ? Hp.band(0.9) : pi(1));
            }
            for (int x = 6; x <= 9; x++) { px(im, x, 6, pi(4)); px(im, x, 10, pi(4)); }
            px(im, 9, 9, st(5)); px(im, 9, 10, st(3));
            px(im, 6, 5, st(4)); px(im, 6, 13, st(4));
            if (active) {
                // Warm bleed on the frame pixels touching the gaps.
                for (int y = top - 1; y <= bottom + 1; y++)
                    for (int x = 2; x <= 13; x++) {
                        if (y < 0 || y > 15 || open[y][x] || (y >= 0 && y <= 15 && !isFrame(im, x, y))) continue;
                        Hp.glow(im, x, y, 0.35, 0.7);
                    }
            }
            return im;
        }

        static boolean isLeaf(boolean[][] open, int x, int y) {
            // Leaf = open cell whose 4 neighbours are all open (gap is the 1 px ring inside the frame).
            return open[y][x] && open[y - 1][x] && open[y + 1 < 16 ? y + 1 : y][x] && open[y][x - 1] && open[y][x + 1]
                    && y + 1 <= 14 + 0 && true;
        }

        static boolean isFrame(BufferedImage im, int x, int y) {
            int c = rgb(im, x, y);
            for (int s = 1; s <= 5; s++) if (c == pi(s)) return true;
            return false;
        }

        static BufferedImage side() {
            BufferedImage im = img();
            Random r = new Random(9101);
            // Bricks in three bands separated by a riveted iron hoop; edge columns step down to read as curved.
            int[][] joints = {{3, 9, 14}, {6, 12, 1}, {2, 8, 13}};
            for (int y = 0; y < 16; y++)
                for (int x = 0; x < 16; x++) {
                    int row = y < 7 ? y / 3 : y > 8 ? (y - 9) / 3 : -1; // 0,1 above ; courses
                    int step;
                    int a = y < 7 ? y % 3 : (y - 9) % 3;
                    if (y < 7 || y > 8) {
                        int c = y < 7 ? y / 3 : 2 + (y - 9) / 3;
                        int[] j = joints[c % 3];
                        boolean joint = a == 2 || x == j[0] || x == j[1] || x == j[2];
                        if (y < 7 && y % 3 == 2 && y == 6) joint = false;
                        step = joint ? 1 : (a == 0 ? 5 : 4);
                        if (step != 1) {
                            if (x == j[0] - 1 || x == j[1] - 1 || x == j[2] - 1) step = Math.max(3, step - 1);
                            if (x <= 1) step = Math.min(5, step + 1);
                            if (x >= 14) step = Math.max(2, step - 1);
                            if (r.nextInt(7) == 0) step = Math.max(2, step - 1);
                        } else if (y == 6 || y == 15 || y == 0) step = 1;
                    } else {
                        // Iron hoop, 2 px tall.
                        int s = y == 7 ? 4 : 2;
                        if (x == 0) s = y == 7 ? 5 : 3;
                        if (x == 15) s = 1;
                        px(im, x, y, pi(s));
                        continue;
                    }
                    px(im, x, y, fb(step));
                }
            for (int x : new int[] {3, 11}) { px(im, x, 7, st(5)); px(im, x + 1, 8, pi(1)); }
            return im;
        }

        static BufferedImage top() {
            BufferedImage im = img();
            double c = 7.5;
            for (int y = 0; y < 16; y++)
                for (int x = 0; x < 16; x++) {
                    double dx = x - c, dy = y - c, d = Math.hypot(dx, dy);
                    double ang = Math.toDegrees(Math.atan2(dy, dx));
                    int step;
                    if (d <= 2.0) step = -1;
                    else if (d <= 3.4) step = -2;
                    else {
                        boolean ring = (d > 5.5 && d <= 6.4) || (d > 8.2 && d <= 9.0);
                        int seg = d < 5.5 ? 6 : d < 8.2 ? 10 : 14;
                        double w = 360.0 / seg;
                        double off = d > 5.5 && d < 8.2 ? w / 2 : 0;
                        double e = (((ang + off) % w) + w) % w;
                        boolean joint = ring || e * Math.PI / 180 * d < 1.0 && d < 8.2;
                        if (d > 8.2 && !ring) joint = (((ang) % 22.5) + 22.5) % 22.5 * Math.PI / 180 * d < 1.0;
                        step = joint ? 1 : (dx + dy < -2 ? 5 : dx + dy > 5 ? 3 : 4);
                    }
                    int col;
                    if (step == -1) col = x + y < 14 ? 0x0e0c0c : 0x1a1514;
                    else if (step == -2) col = dx + dy < 0 ? pi(5) : dx + dy < 2.5 ? pi(4) : pi(2);
                    else col = fb(step);
                    if (step == -1 && (x == 6 && y == 6 || x == 7 && y == 6 || x == 6 && y == 7)) col = pi(1);
                    px(im, x, y, col);
                }
            return im;
        }

        // ---- insulated lines: slag wool wrap
        static int wc(int s) { return mix(Bf.SLAG_TINT.get(s), Bf.SLAG_SPEC.get(s), 0.4); }

        /** Tile of wool steps (2..5): clustered fibres, a lit tuft or two, wrapped so edges tile. */
        static int[][] wool(long seed) {
            Random r = new Random(seed);
            int[][] f = new int[16][16];
            for (int[] row : f) java.util.Arrays.fill(row, 3);
            for (int n = 0; n < 16; n++) {
                int x = r.nextInt(16), y = r.nextInt(16), w = 2 + r.nextInt(2), dir = r.nextInt(3) - 1;
                int tone = r.nextInt(5) < 3 ? 1 : -1;
                for (int i = 0; i < w; i++) {
                    f[Math.floorMod(y + (dir * i) / 2, 16)][Math.floorMod(x + i, 16)] += tone;
                    if (i == 0 || i == w - 1) f[Math.floorMod(y + 1, 16)][Math.floorMod(x + i, 16)] += tone > 0 ? 0 : 0;
                }
            }
            for (int[] row : f) for (int i = 0; i < 16; i++) row[i] = Math.max(2, Math.min(4, row[i]));
            return f;
        }

        static int ws(int[][] f, int x, int y, int shade) {
            return Math.max(2, Math.min(5, f[Math.floorMod(y, 16)][Math.floorMod(x, 16)] + shade));
        }

        static BufferedImage insulated(boolean duct) {
            BufferedImage im = img();
            int[][] f = wool(duct ? 7702 : 7701);
            int[] sh = {1, 1, 0, 0, 0, -1, -1};
            int[] wire = {5, 4, 3, 3, 2, 2, 1};
            // Wrapped tube side everywhere (also fills the unused pixels): shaded across 7 rows, wire ties x=1 and x=3... one tie at x=3.
            for (int y = 0; y < 16; y++)
                for (int x = 0; x < 16; x++) {
                    int a = y % 7;
                    if (x % 4 == 3 && !(x >= 4 && x <= 11 && y >= 4 && y <= 11)) px(im, x, y, st(wire[a] == 1 ? 1 : Math.min(5, wire[a])));
                    else px(im, x, y, wc(ws(f, x, y, sh[a])));
                }
            // Hub: wrapped joint, two ties, an exposed band of the pipe material.
            for (int y = 4; y <= 11; y++)
                for (int x = 4; x <= 11; x++) {
                    boolean top = y == 4, left = x == 4, bot = y == 11, right = x == 11;
                    int sh2 = (top || left) ? 1 : (bot || right) ? -1 : (x + y < 14 ? 0 : -0);
                    int s = ws(f, x + 3, y + 5, sh2);
                    if (top || left) s = Math.max(s, 4);
                    if (bot || right) s = Math.min(s, 3);
                    px(im, x, y, wc(s));
                }
            for (int y = 5; y <= 10; y++) { // wire ties
                px(im, 5, y, st(y == 5 ? 5 : 3)); px(im, 10, y, st(y == 5 ? 4 : 2));
            }
            // Exposed band between the ties.
            for (int y = 7; y <= 8; y++)
                for (int x = 6; x <= 9; x++) {
                    if (duct) px(im, x, y, fb(y == 7 ? (x == 6 ? 5 : 4) : (x == 9 ? 2 : 3)));
                    else px(im, x, y, cu(y == 7 ? (x == 6 ? 5 : 4) : (x == 9 ? 2 : 3)));
                }
            if (duct) px(im, 8, 7, fb(1));
            else px(im, 6, 7, COPPER.spec());
            px(im, 7, 5, wc(5)); px(im, 6, 10, wc(2));
            // End: wool ring, then pipe material ring, then bore.
            for (int y = 9; y <= 15; y++)
                for (int x = 0; x <= 6; x++) {
                    int dx = Math.min(x, 6 - x), dy = Math.min(y - 9, 15 - y), d = Math.min(dx, dy);
                    boolean lit = x + (y - 9) < 6;
                    int c;
                    if (d == 0) c = wc(ws(f, x + 9, y, lit ? 1 : -1));
                    else if (d == 1) c = duct ? (lit ? fb(4) : fb(2)) : cu(lit ? 4 : 3);
                    else c = (y == 11 || x == 2) ? (duct ? fb(1) : cu(1)) : Hp.BORE;
                    px(im, x, y, c);
                }
            if (duct) { px(im, 1, 10, st(5)); px(im, 5, 14, st(2)); }
            else px(im, 1, 10, COPPER.spec());
            px(im, 3, 9, wc(4)); // wire tie crosses the ring
            px(im, 3, 15, st(2));
            return im;
        }

        static BufferedImage wool() {
            BufferedImage im = img();
            Random r = new Random(7703);
            // Overlapping puffs: centre x, y, radius.
            double[][] puffs = {{8, 9.5, 4.4}, {5, 8, 3.2}, {11, 8.5, 3.4}, {7.5, 6, 3.3}, {10, 11.5, 2.8}, {5, 11, 2.6}};
            for (int y = 1; y < 15; y++)
                for (int x = 1; x < 15; x++) {
                    double best = -1;
                    for (double[] p : puffs) {
                        double d = Math.hypot(x - p[0], (y - p[1]) * 1.1) / p[2];
                        if (d < 1 && (1 - d) > best) best = 1 - d;
                    }
                    if (best < 0) continue;
                    int s = 3;
                    double lx = x - 6.5, ly = y - 6.5;
                    if (lx + ly < 0) s++;
                    if (lx + ly < -5) s++;
                    if (lx + ly > 6) s--;
                    if (best < 0.18 && lx + ly > 0) s--;
                    if (r.nextInt(5) == 0) s += r.nextBoolean() ? 1 : -1;
                    px(im, x, y, wc(Math.max(2, Math.min(5, s))));
                }
            // Fibre wisps: short bright strands on the lit side, dark creases between puffs.
            int[][] wisps = {{6, 3}, {7, 2}, {10, 4}, {3, 7}, {12, 10}, {13, 9}};
            for (int[] w : wisps) if (!opaque(im, w[0], w[1])) px(im, w[0], w[1], wc(w[0] + w[1] < 11 ? 4 : 3));
            int[][] creases = {{8, 7}, {9, 7}, {6, 10}, {7, 11}, {10, 9}, {11, 9}};
            for (int[] c : creases) if (opaque(im, c[0], c[1])) px(im, c[0], c[1], wc(2));
            int[][] bright = {{5, 6}, {6, 5}, {7, 4}};
            for (int[] c : bright) if (opaque(im, c[0], c[1])) px(im, c[0], c[1], wc(5));
            return outline(im);
        }

        static BufferedImage gui() {
            BufferedImage im = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
            panel(im, 176, 176);
            for (int x : new int[] {8, 26, 44, 62}) for (int y : new int[] {20, 38}) slot(im, x, y);
            for (int x : new int[] {98, 116, 134, 152}) for (int y : new int[] {20, 38}) slot(im, x, y);
            for (int y = 0; y < 14; y++)
                for (int x = 0; x < 14; x++)
                    if (FLAME_SILHOUETTE[y].charAt(x) == '#') {
                        im.setRGB(81 + x, 28 + y, 0xff000000 | SLOT_FILL);
                        int band = y < 4 ? 4 : y < 8 ? 3 : y < 11 ? 2 : 1;
                        im.setRGB(176 + x, y, 0xff000000 | HEAT_BAND[band]);
                    }
            for (int row = 0; row < 3; row++)
                for (int col = 0; col < 9; col++) slot(im, 8 + col * 18, 94 + row * 18);
            for (int col = 0; col < 9; col++) slot(im, 8 + col * 18, 152);
            return im;
        }

        static void kiln() throws IOException {
            save("block/kiln_front", front(false));
            save("block/kiln_front_active", front(true));
            save("block/kiln_side", side());
            save("block/kiln_top", top());
            save("block/insulated_copper_heat_pipe", insulated(false));
            save("block/insulated_refractory_heat_duct", insulated(true));
            save("item/slag_wool", wool());
            saveRaw("gui/kiln", gui());
        }
    }

    // ---------------------------------------------------------------- tier 4: roaster

    static final class Ro {
        static int fb(int s) { return FIRE_BRICK.get(s); }
        static int cu(int s) { return COPPER.get(s); }
        static final int CAVITY = 0x16110f, DEEP = 0x0e0c0c;
        static final int[] FUME = {0xe8e2a6, 0xcfc67e, 0xa89f5c};

        static int hash(int x, int y) { return Math.floorMod(x * 73 + y * 151 + x * y * 17 + 11, 97); }

        /** Copper hood plates: lit top edge, seams, rivets, lip shadow on the face below. */
        static void hood(BufferedImage im, int rows) {
            for (int y = 0; y < rows; y++)
                for (int x = 0; x < 16; x++) {
                    int s = y == 0 ? 5 : y == 1 ? 4 : y >= rows - 2 ? 2 : 3;
                    if (y == rows - 2) s = 2;
                    if (y == rows - 1) s = 1;
                    if ((x == 5 || x == 10) && y > 0 && y < rows - 2) s = 2;
                    if ((x == 6 || x == 11) && y > 0 && y < rows - 2) s = 4;
                    if (x == 0 && y < rows - 1) s = Math.min(5, s + 1);
                    if (x == 15 && y < rows - 1) s = Math.max(2, s - 1);
                    px(im, x, y, cu(s));
                }
            for (int x : new int[] {2, 8, 13}) { px(im, x, 2, cu(5)); px(im, x + 1, 3, cu(1)); }
            px(im, 3, 1, COPPER.spec());
        }

        /** Ore bed height per column x=3..12. */
        static final int[] HEIGHT = {2, 3, 3, 4, 4, 4, 3, 3, 3, 2};

        static int oreStep(int x, int y, int top) {
            int h = hash(x, y);
            int s = h % 5 == 0 ? 3 : h % 3 == 0 ? 2 : 1;
            if (y == top && x < 8) s = Math.min(4, s + 1);
            if (y == top && x >= 8) s = Math.max(1, s);
            return s;
        }

        static BufferedImage front(int frame) {
            boolean active = frame >= 0;
            BufferedImage im = fireBricks();
            hood(im, 8);
            // Hearth opening: x3..12, y8..13, brick surround with a dark rim.
            for (int y = 8; y <= 14; y++)
                for (int x = 2; x <= 13; x++) {
                    boolean open = x >= 3 && x <= 12 && y <= 13;
                    if (open) continue;
                    int s = (x == 2 || y == 14) ? (y == 14 ? 4 : 1) : (x == 13 ? 3 : 1);
                    if (y == 14 && x == 2) s = 1;
                    if (x == 13 && y == 14) s = 2;
                    px(im, x, y, fb(s));
                }
            for (int x = 3; x <= 12; x++) px(im, x, 14, fb(x < 8 ? 5 : 4));
            for (int y = 8; y <= 13; y++)
                for (int x = 3; x <= 12; x++) {
                    int top = 14 - HEIGHT[x - 3];
                    int c;
                    if (y < top) c = y == 8 ? DEEP : CAVITY;
                    else c = mix(CHARCOAL.get(oreStep(x, y, top)), CASSITERITE.get(oreStep(x, y, top) + 1), 0.5);
                    px(im, x, y, c);
                }
            if (!active) return im;
            // Glowing parts of the bed: surface and crevices, flickering by frame.
            for (int x = 3; x <= 12; x++)
                for (int y = 14 - HEIGHT[x - 3]; y <= 13; y++) {
                    int h = hash(x, y);
                    if (h % 5 >= 3 && y != 14 - HEIGHT[x - 3]) continue;
                    double g = 0.30 + 0.62 * (((h + frame) % 4) / 3.0);
                    if (y == 13) g *= 0.8;
                    px(im, x, y, Hp.band(g));
                }
            // Warm bleed on the surround and the cavity ceiling.
            for (int x = 3; x <= 12; x++) {
                Hp.glow(im, x, 14, 0.5, 0.45);
            }
            for (int y = 9; y <= 13; y++) { Hp.glow(im, 2, y, 0.45, 0.4); Hp.glow(im, 13, y, 0.45, 0.4); }
            // Fume wisps rising through the cavity.
            int[][] wisps = {{5, 0}, {8, 2}, {10, 1}};
            for (int[] w : wisps) {
                int top = 14 - HEIGHT[w[0] - 3];
                for (int k = 0; k < 2; k++) {
                    int y = top - 1 - Math.floorMod(frame + w[1] + k * 2, 4) % 3 + 0;
                    y = Math.max(8, Math.min(top - 1, y));
                    int xx = w[0] + ((frame + w[1] + k) % 2);
                    int top2 = 14 - HEIGHT[Math.min(12, Math.max(3, xx)) - 3];
                    if (y >= 8 && y < top2) px(im, xx, y, FUME[k == 0 ? 0 : 1 + (frame + w[1]) % 2]);
                }
            }
            return im;
        }

        static BufferedImage side() {
            BufferedImage im = fireBricks();
            for (int x = 0; x < 16; x++) {
                px(im, x, 0, cu(5));
                px(im, x, 1, cu(4));
                px(im, x, 2, cu(3));
                px(im, x, 3, cu(2));
                px(im, x, 4, fb(1));
                if (x == 0) { px(im, x, 1, cu(5)); px(im, x, 2, cu(4)); }
                if (x == 15) { px(im, x, 0, cu(4)); px(im, x, 1, cu(3)); px(im, x, 2, cu(2)); }
            }
            for (int x : new int[] {3, 11}) { px(im, x, 1, COPPER.spec()); px(im, x + 1, 2, cu(1)); }
            for (int x = 5; x <= 9; x++) px(im, x, 3, cu(1));
            return im;
        }

        static BufferedImage back() {
            BufferedImage im = fireBricks();
            double c = 7.5;
            for (int y = 0; y < 16; y++)
                for (int x = 0; x < 16; x++) {
                    double dx = x - c, dy = y - c, d = Math.hypot(dx, dy);
                    if (d > 6.0 && d <= 7.1 && dx + dy > 1) px(im, x, y, fb(1)); // cast shadow
                    if (d > 6.0) continue;
                    boolean lit = dx + dy < -1.5;
                    int s;
                    if (d <= 2.2) { px(im, x, y, dx + dy < -1 ? DEEP : 0x1b1615); continue; }
                    else if (d <= 3.6) s = lit ? 5 : dx + dy > 2 ? 2 : 3; // pipe collar
                    else s = lit ? 4 : dx + dy > 2.5 ? 2 : 3; // flange face
                    if (d > 5.3) s = lit ? 5 : dx + dy > 2.5 ? 1 : 2; // flange rim
                    if (Math.abs(d - 3.65) < 0.5) s = 1; // collar seam
                    px(im, x, y, cu(s));
                }
            // Eight bolts on the flange.
            int[][] bolts = {{7, 2}, {8, 2}, {7, 13}, {8, 13}, {2, 7}, {2, 8}, {13, 7}, {13, 8}};
            int[][] diag = {{3, 3}, {12, 3}, {3, 12}, {12, 12}};
            for (int[] b : diag) { px(im, b[0], b[1], cu(5)); px(im, b[0] + 1, b[1] + 1, cu(1)); }
            px(im, 7, 2, cu(5)); px(im, 8, 13, cu(1)); px(im, 2, 8, cu(5)); px(im, 13, 7, cu(1));
            px(im, 8, 2, cu(1)); px(im, 7, 13, cu(5)); px(im, 2, 7, cu(1)); px(im, 13, 8, cu(5));
            px(im, 3, 3, COPPER.spec());
            return im;
        }

        static BufferedImage top() {
            BufferedImage im = img();
            for (int y = 0; y < 16; y++)
                for (int x = 0; x < 16; x++) {
                    int s = 3;
                    if (y == 0 || x == 0) s = 5;
                    else if (y == 15 || x == 15) s = 1;
                    else if (y == 1 || x == 1) s = 4;
                    else if (y == 14 || x == 14) s = 2;
                    else if (x == 5 || x == 10) s = (x == 5) ? 2 : 2;
                    else if (x == 6 || x == 11) s = 4;
                    px(im, x, y, cu(s));
                }
            for (int[] r : new int[][] {{2, 2}, {13, 2}, {2, 13}, {13, 13}}) {
                px(im, r[0], r[1], cu(5)); px(im, r[0] + 1, r[1] + 1, cu(1));
            }
            px(im, 2, 2, COPPER.spec());
            // Flue: 6x6 collar with a 4x4 dark opening.
            for (int y = 5; y <= 10; y++)
                for (int x = 5; x <= 10; x++) {
                    boolean in = x >= 6 && x <= 9 && y >= 6 && y <= 9;
                    if (in) { px(im, x, y, (x == 6 || y == 6) ? DEEP : 0x1b1615); continue; }
                    int s = (y == 5 || x == 5) ? 5 : (y == 10 || x == 10) ? 2 : 4;
                    if (x == 10 && y == 10) s = 1;
                    px(im, x, y, cu(s));
                }
            px(im, 7, 7, 0x000000 | CAVITY);
            return im;
        }

        static void arrowShape(BufferedImage im, int ox, int oy, java.util.function.IntBinaryOperator colour) {
            String[] a = {"..####..", "..####..", "..####..", "..####..", "########", ".######.", "..####..", "...##..."};
            for (int y = 0; y < 8; y++)
                for (int x = 0; x < 8; x++)
                    if (a[y].charAt(x) == '#') im.setRGB(ox + x, oy + y, 0xff000000 | colour.applyAsInt(x, y));
        }

        static BufferedImage gui() {
            BufferedImage im = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
            panel(im, 176, 182);
            for (int i = 0; i < 4; i++) {
                int x = 35 + 18 * i;
                slot(im, x, 17);
                slot(im, x, 46);
                arrowShape(im, x + 4, 36, (px, py) -> SLOT_FILL);
            }
            // Filled arrow sprite.
            arrowShape(im, 176, 0, (px, py) -> {
                int c = mix(HEAT_BAND[3], HEAT_BAND[4], py / 7.0);
                if (px >= 5 && py < 4) c = mix(c, HEAT_BAND[2], 0.35);
                if (py == 7) c = HEAT_BAND[4];
                return c;
            });
            // Gas gauge: recessed well, 16x46 dark inner.
            well(im, 139, 16, 18, 48, 0x2b2b2b);
            fill(im, 140, 17, 16, 46, 0x2b2b2b);
            for (int t = 0; t < 4; t++) {
                int y = 17 + 9 + t * 9;
                int len = t == 1 ? 5 : 3;
                fill(im, 156 - len, y, len, 1, SLOT_FILL);
            }
            // Filled gas sprite: sulfur yellow gradient, lighter top, a soft light edge on the left.
            for (int y = 0; y < 46; y++)
                for (int x = 0; x < 16; x++) {
                    double t = y / 45.0;
                    int c = mix(0xeee89c, 0xb8ae48, t);
                    if (x == 0) c = mix(c, 0xfff8c8, 0.4);
                    if (x == 15) c = mix(c, 0x80781e, 0.3);
                    if (y == 0) c = 0xfff4b8;
                    if (((x * 5 + y * 3) % 11) == 0 && x > 1 && x < 14) c = mix(c, 0xfff8c8, 0.35);
                    im.setRGB(184 + x, y, 0xff000000 | c);
                }
            for (int t = 0; t < 4; t++) {
                int y = 9 + t * 9, len = t == 1 ? 5 : 3;
                for (int i = 0; i < len; i++) im.setRGB(184 + 15 - i, y, 0xff000000 | 0x8a8030);
            }
            for (int row = 0; row < 3; row++)
                for (int col = 0; col < 9; col++) slot(im, 8 + col * 18, 100 + row * 18);
            for (int col = 0; col < 9; col++) slot(im, 8 + col * 18, 158);
            return im;
        }

        static void roaster() throws IOException {
            save("block/roaster_front", front(-1));
            BufferedImage strip = new BufferedImage(16, 64, BufferedImage.TYPE_INT_ARGB);
            for (int f = 0; f < 4; f++) {
                BufferedImage fr = front(f);
                for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) strip.setRGB(x, f * 16 + y, fr.getRGB(x, y));
            }
            saveAnimated("block/roaster_front_active", strip, 3);
            save("block/roaster_side", side());
            save("block/roaster_back", back());
            save("block/roaster_top", top());
            saveRaw("gui/roaster", gui());
        }
    }

    static final class Sm {
        static int fb(int s) { return FIRE_BRICK.get(s); }
        static int st(int s) { return STEEL.get(s); }
        static int rf(int s) { return REFRACTORY.get(s); }
        static final int CAVITY = 0x16110f, DEEP = 0x0e0c0c;
        static int hash(int x, int y) { return Math.floorMod(x * 73 + y * 151 + x * y * 17 + 11, 97); }

        /** Steel band across rows 2..4: lit top edge, shaded lower edge, rivets, seam shadow below. */
        static void band(BufferedImage im) {
            for (int x = 0; x < 16; x++) {
                px(im, x, 2, st(x == 15 ? 4 : 5));
                px(im, x, 3, st(x == 15 ? 2 : 4));
                px(im, x, 4, st(x == 15 ? 1 : 2));
                if (x == 0) { px(im, x, 3, st(5)); px(im, x, 4, st(3)); }
                px(im, x, 5, fb(1));
            }
            px(im, 3, 3, STEEL.spec()); px(im, 4, 4, st(1));
            px(im, 11, 3, st(5)); px(im, 12, 4, st(1));
        }

        static BufferedImage side() {
            BufferedImage im = fireBricks();
            band(im);
            return im;
        }

        static BufferedImage front(int frame) {
            boolean active = frame >= 0;
            BufferedImage im = fireBricks();
            band(im);
            // Spout housing: steel block x5..10, y6..9 with a dark mouth, lit top-left.
            for (int y = 6; y <= 10; y++)
                for (int x = 5; x <= 10; x++) {
                    int s = y == 6 ? 5 : y == 10 ? 1 : 3;
                    if (x == 5) s = Math.min(5, s + 1);
                    if (x == 10) s = Math.max(1, s - 1);
                    if (y == 10 && x == 5) s = 2;
                    px(im, x, y, st(s));
                }
            // Mouth, 2 wide, running down the lip.
            for (int y = 8; y <= 10; y++) { px(im, 7, y, DEEP); px(im, 8, y, DEEP); }
            px(im, 6, 8, st(1)); px(im, 9, 8, st(2));
            // Shadow cast under the spout.
            px(im, 6, 11, fb(1)); px(im, 7, 11, fb(2)); px(im, 8, 11, fb(2)); px(im, 9, 11, fb(1)); px(im, 10, 11, fb(1));
            // Catch trough below.
            for (int x = 4; x <= 11; x++) { px(im, x, 14, x == 4 ? st(4) : x == 11 ? st(1) : st(2)); px(im, x, 13, x < 8 ? st(5) : st(4)); }
            px(im, 4, 13, st(5)); px(im, 11, 13, st(2));
            for (int x = 5; x <= 10; x++) px(im, x, 15, fb(1));
            // Status light: small lens on the band's right end.
            px(im, 13, 3, 0x2a3a30);
            if (active) {
                px(im, 13, 3, 0x4ac060);
                px(im, 12, 3, mix(st(4), 0x4ac060, 0.45)); px(im, 14, 3, mix(st(4), 0x4ac060, 0.3));
                px(im, 13, 2, mix(st(5), 0x4ac060, 0.5));
                // Glowing mouth and a thin trickle with a drip running down.
                px(im, 7, 8, Hp.band(0.8)); px(im, 8, 8, Hp.band(0.95));
                px(im, 7, 9, Hp.band(0.85)); px(im, 8, 9, Hp.band(1.0));
                px(im, 7, 10, Hp.band(0.9)); px(im, 8, 10, Hp.band(0.75));
                px(im, 6, 9, mix(st(3), Hp.band(0.5), 0.5)); px(im, 9, 9, mix(st(2), Hp.band(0.5), 0.5));
                px(im, 8, 11, Hp.band(0.9));
                px(im, 8, 12, Hp.band(0.8));
                int dy = 12 + frame % 4;
                // Stream into the trough, a drip falling with the frame.
                px(im, 8, 13, Hp.band(1.0));
                if (frame % 2 == 0) px(im, 7, 11, Hp.band(0.55));
                int y1 = 11 + (frame % 4);
                px(im, 7, y1, Hp.band(1.0));
                if (y1 + 1 <= 12) px(im, 7, y1 + 1, Hp.band(0.6));
                // Metal pooled in the trough.
                for (int x = 5; x <= 10; x++) px(im, x, 13, Hp.band(x == 8 ? 1.0 : 0.55 + 0.3 * (((x + frame) % 3) / 2.0)));
                // Warm bleed on the steel round the mouth and the bricks under the trough.
                for (int x = 5; x <= 10; x++) { Hp.glow(im, x, 10, 0.5, 0.45); Hp.glow(im, x, 14, 0.4, 0.35); }
                Hp.glow(im, 5, 8, 0.4, 0.4); Hp.glow(im, 10, 8, 0.4, 0.4);
            }
            return im;
        }

        /** Rim from above: fire brick, steel collar, refractory pot rim, 6x6 opening. */
        static BufferedImage top(int frame) {
            boolean active = frame >= 0;
            BufferedImage im = fireBricks();
            for (int y = 1; y <= 14; y++)
                for (int x = 1; x <= 14; x++) {
                    boolean corner = (x == 1 || x == 14) && (y == 1 || y == 14);
                    if (corner) continue;
                    boolean outer = x == 1 || x == 14 || y == 1 || y == 14;
                    int s = 3;
                    if (outer) s = (x == 14 || y == 14) ? 1 : 3;
                    else if (y == 2 || x == 2) s = 5;
                    else if (y == 13 || x == 13) s = 2;
                    else if (y == 3 || x == 3) s = 4;
                    px(im, x, y, st(s));
                }
            // Collar rivets.
            for (int[] r : new int[][] {{3, 3}, {12, 3}, {3, 12}, {12, 12}}) { px(im, r[0], r[1], st(5)); px(im, r[0] + 1, r[1] + 1, st(1)); }
            px(im, 3, 3, STEEL.spec());
            // Pot rim: refractory ring x4..11.
            for (int y = 4; y <= 11; y++)
                for (int x = 4; x <= 11; x++) {
                    boolean corner = (x == 4 || x == 11) && (y == 4 || y == 11);
                    if (corner) { px(im, x, y, st(2)); continue; }
                    int s = (y == 4 || x == 4) ? 5 : (y == 11 || x == 11) ? 2 : 4;
                    px(im, x, y, rf(s));
                }
            // Opening x5..10, y5..10 with the corners cut.
            for (int y = 5; y <= 10; y++)
                for (int x = 5; x <= 10; x++) {
                    boolean corner = (x == 5 || x == 10) && (y == 5 || y == 10);
                    if (corner) { px(im, x, y, rf(2)); continue; }
                    int c;
                    if (!active) c = (x == 5 || y == 5) ? DEEP : (x == 10 || y == 10) ? 0x1f1a17 : CAVITY;
                    else {
                        double g = 0.62 + 0.38 * (((hash(x, y) + frame * 3) % 8) / 7.0);
                        // slow drifting swirl: lighter band sweeping diagonally
                        double sweep = Math.sin((x + y) * 0.9 - frame * 0.785) * 0.5 + 0.5;
                        g = Math.min(1.0, 0.55 + 0.25 * sweep + 0.2 * g);
                        c = Hp.band(g);
                        if (hash(x + 5, y) % 9 == 0 && (x + frame) % 4 != 0) c = HEAT_BAND[2];
                        if (x == 5 || y == 5) c = mix(c, HEAT_BAND[2], 0.5);
                        if (hash(x, y + frame) % 17 == 0) c = HEAT_BAND[5];
                    }
                    px(im, x, y, c);
                }
            if (active) {
                for (int i = 4; i <= 11; i++) { Hp.glow(im, i, 4, 0.5, 0.3); Hp.glow(im, 4, i, 0.5, 0.3); Hp.glow(im, i, 11, 0.5, 0.35); Hp.glow(im, 11, i, 0.5, 0.35); }
            }
            return im;
        }

        static BufferedImage strip(java.util.function.IntFunction<BufferedImage> f) {
            BufferedImage s = new BufferedImage(16, 128, BufferedImage.TYPE_INT_ARGB);
            for (int fr = 0; fr < 8; fr++) {
                BufferedImage a = f.apply(fr);
                for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) s.setRGB(x, fr * 16 + y, a.getRGB(x, y));
            }
            return s;
        }

        static void rightArrow(BufferedImage im, int ox, int oy, java.util.function.IntBinaryOperator colour) {
            String[] a = {".......#....", ".......##...", "##########..", "############", "############", "##########..", ".......##...", ".......#...."};
            for (int y = 0; y < 8; y++)
                for (int x = 0; x < 12; x++)
                    if (a[y].charAt(x) == '#') im.setRGB(ox + x, oy + y, 0xff000000 | colour.applyAsInt(x, y));
        }

        static BufferedImage gui() {
            BufferedImage im = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
            panel(im, 176, 240);
            for (int i = 0; i < 9; i++) slot(im, 8 + (i % 3) * 18, 18 + (i / 3) * 18);
            well(im, 65, 17, 10, 54, 0x2a2a2a);
            for (int u = 1; u < 4; u++) fill(im, 75, 17 + 1 + Math.round(52 - u * 13f), 2, 1, SLOT_FILL);
            well(im, 159, 17, 10, 54, 0x2a2a2a);
            for (int t = 250; t < 1750; t += 250) {
                int y = 18 + 52 - Math.round(t / 1750f * 52);
                fill(im, 156, y, 3, 1, t % 500 == 0 ? GUI_SHADOW : SLOT_FILL);
            }
            slot(im, 8, 78); slot(im, 26, 78); slot(im, 62, 78); slot(im, 98, 78);
            rightArrow(im, 45, 82, (px, py) -> SLOT_FILL);
            rightArrow(im, 81, 82, (px, py) -> SLOT_FILL);
            rightArrow(im, 176, 0, (px, py) -> {
                int c = mix(HEAT_BAND[3], HEAT_BAND[4], py / 7.0);
                if (px < 7 && py < 3) c = mix(c, HEAT_BAND[2], 0.35);
                if (px == 11 || py == 7) c = HEAT_BAND[4];
                return c;
            });
            for (int row = 0; row < 3; row++)
                for (int col = 0; col < 9; col++) slot(im, 8 + col * 18, 158 + row * 18);
            for (int col = 0; col < 9; col++) slot(im, 8 + col * 18, 216);
            return im;
        }

        static void smelter() throws IOException {
            save("block/smelter_front", front(-1));
            saveAnimated("block/smelter_front_active", strip(Sm::front), 3);
            save("block/smelter_side", side());
            save("block/smelter_top", top(-1));
            saveAnimated("block/smelter_top_active", strip(Sm::top), 3);
            saveRaw("gui/smelter", gui());
        }
    }

    static void converter() throws IOException {
        save("block/converter_controller_front", Cv.front(-1));
        saveAnimated("block/converter_controller_front_blowing", Cv.blowing(), 2);
        saveRaw("gui/converter", Cv.gui());
    }

    static void engines() throws IOException {
        save("block/steam_engine_base", engineBase());
        save("block/steam_engine_cylinder", engineCylinder());
        save("block/steam_engine_cylinder_end", engineCylinderEnd());
        save("block/steam_engine_rod", engineRod());
        save("block/steam_engine_flywheel", engineFlywheel());
        save("block/mechanical_pump_front", pumpFront());
        save("block/mechanical_pump_side", pumpSide());
        save("block/mechanical_pump_back", pumpBack());
        save("block/mechanical_pump_top", bronzePlates(8814));
        crusher();
        washer();
        blastFurnace();
        converter();
        heatPipes();
        Kn.kiln();
        Ro.roaster();
        Sm.smelter();
    }

    static void steam() throws IOException {
        save("block/firebox_side", fireboxSide());
        save("block/firebox_top", fireboxTop());
        save("block/firebox_front", fireboxFront(-1, false));
        saveAnimated("block/firebox_front_lit", fireboxFrontLit(false), 2);
        saveAnimated("block/firebox_front_lit_hot", fireboxFrontLit(true), 2);
        save("block/bronze_boiler_side", bronzePlates(8610));
        save("block/bronze_boiler_top", boilerTop());
        save("block/bronze_boiler_front", boilerFront());
        save("block/cracked_bronze_boiler_side", crackedBoiler(bronzePlates(8610), 8620));
        save("block/cracked_bronze_boiler_top", crackedBoiler(boilerTop(), 8621));
        save("block/cracked_bronze_boiler_front", crackedBoiler(boilerFront(), 8622));
        save("block/copper_fluid_pipe", pipeSheet(COPPER, false, 8630));
        save("block/bronze_fluid_pipe", pipeSheet(BRONZE, false, 8631));
        save("block/steel_fluid_pipe", pipeSheet(STEEL, true, 8632));
        save("block/pressure_gauge", gaugeHousing());
        for (int r = 0; r <= 4; r++) save("block/pressure_gauge_dial_" + r, gaugeDial(r));
        saveRaw("gui/firebox", fireboxGui());
        saveRaw("gui/bronze_boiler", boilerGui());
        engines();
    }

    static void tier4() throws IOException {
        // Spec 11.7: iron transmission.
        save("block/iron_axle", ironAxleSide());
        save("block/iron_axle_end", ironAxleEnd());
        save("block/iron_gearbox", ironise(gearboxFace(), 6.1));
        save("block/iron_step_up_gearbox_front", ironise(stepUpFront(), 5.7));
        save("block/iron_step_up_gearbox_back", ironise(stepUpBack(), 6.6));
        save("block/iron_step_up_gearbox_side", ironise(stepUpSide(), 0));
        // Spec 4.4 and 14.4: zinc calcines and the steel anvil.
        save("item/poor_zinc_calcine", map(CALCINE, SCORCH, CALCINE_POOR));
        save("item/zinc_calcine", map(CALCINE, SCORCH, CALCINE_NORMAL));
        save("item/rich_zinc_calcine", map(CALCINE, SCORCH, CALCINE_RICH));
        save("item/small_zinc_calcine", map(CALCINE, SCORCH, CALCINE_SMALL));
        save("block/steel_anvil", steelAnvilBody());
        save("block/steel_anvil_top", steelAnvilTop());
        // Ores (worldgen spec 14.2 and 14.3).
        for (String grade : List.of("poor", "normal", "rich")) {
            save("block/ore/sphalerite_" + grade, oreOverlay(T4_MINERALS.get(0), grade));
            save("block/ore/galena_" + grade, oreOverlay(T4_MINERALS.get(1), grade));
            save("block/ore/bituminous_coal_" + grade, oreOverlay(T4_MINERALS.get(2), grade));
            save("block/ore/sulfur_" + grade, oreOverlay(T4_MINERALS.get(3), grade));
        }
        for (Mineral m : T4_MINERALS) save("block/small_" + m.name(), pebbles(m));
        String[] washedPoor = washedPile(CRUSHED_SMALL), washedNormal = washedPile(CRUSHED_NORMAL), washedRich = washedPile(CRUSHED_RICH);
        for (Mineral m : T4_MINERALS.subList(0, 2)) {
            String n = m.name();
            save("item/poor_" + n, map(m.ramp(), ORE_SMALL));
            save("item/" + n, map(m.ramp(), ORE_NORMAL));
            save("item/rich_" + n, map(m.ramp(), ORE_RICH));
            save("item/crushed_poor_" + n, map(m.ramp(), CRUSHED_SMALL));
            save("item/crushed_" + n, map(m.ramp(), CRUSHED_NORMAL));
            save("item/crushed_rich_" + n, map(m.ramp(), CRUSHED_RICH));
            save("item/small_" + n, map(m.ramp(), NUGGET));
        }
        // Sphalerite is never washed (spec 11.3).
        save("item/washed_poor_galena", washedItem(GALENA, washedPoor, 6, 2));
        save("item/washed_galena", washedItem(GALENA, washedNormal, 7, 3));
        save("item/washed_rich_galena", washedItem(GALENA, washedRich, 6, 3));

        // Metals (spec 4.1 and 21.4).
        save("item/pig_iron_ingot", map(PIG_IRON, sandCast(INGOT_V2)));
        for (var e : List.of(java.util.Map.entry("steel", STEEL), java.util.Map.entry("zinc", ZINC), java.util.Map.entry("lead", LEAD),
                java.util.Map.entry("brass", BRASS))) {
            save("item/" + e.getKey() + "_ingot", map(e.getValue(), INGOT_V2));
            save("item/" + e.getKey() + "_nugget", map(e.getValue(), NUGGET));
        }
        save("item/solder_ingot", map(SOLDER, INGOT_V2));
        for (var e : List.of(java.util.Map.entry("steel", STEEL), java.util.Map.entry("brass", BRASS))) {
            save("item/" + e.getKey() + "_plate", map(e.getValue(), PLATE));
            save("item/" + e.getKey() + "_rod", map(e.getValue(), ROD));
            save("item/" + e.getKey() + "_gear", gear(e.getValue()));
        }
        for (Metal metal : METALS)
            if (metal.name().contains("bronze")) save("item/" + metal.name() + "_gear", gear(metal.ramp()));
        save("item/steel_double_ingot", map(STEEL, DOUBLE_INGOT_V2));

        // Steel tools: smithed heads, smooth handles with a brass ferrule (SG 5, tier 4+).
        java.util.Map<String, String[]> heads = new java.util.LinkedHashMap<>();
        heads.put("pickaxe_head", PICKAXE_HEAD);
        heads.put("axe_head", AXE_HEAD);
        heads.put("shovel_head", SHOVEL_HEAD);
        heads.put("hoe_head", HOE_HEAD);
        heads.put("knife_blade", KNIFE_BLADE);
        heads.put("hammer_head", HAMMER_HEAD);
        heads.put("saw_blade", SAW_BLADE);
        heads.put("sword_blade", SWORD_BLADE);
        for (var head : heads.entrySet()) save("item/steel_" + head.getKey(), head.getKey().equals("pickaxe_head") ? pickaxeHeadV2(STEEL) : map(STEEL, head.getValue()));
        for (String kind : List.of("pickaxe", "axe", "shovel", "hoe", "knife", "hammer", "saw", "sword"))
            save("item/steel_" + kind, tool(STEEL, V2.WOOD_V2, BRASS, kind));

        // Steel armour: plates laced onto leather (spec 14.3).
        saveRaw("entity/equipment/humanoid/steel", armourLayer(STEEL));
        saveRaw("entity/equipment/humanoid_leggings/steel", leggingsLayer(STEEL));
        save("item/steel_helmet", map(STEEL, LEATHER, HELMET_ITEM));
        save("item/steel_chestplate", map(STEEL, LEATHER, CHESTPLATE_ITEM));
        save("item/steel_leggings", map(STEEL, LEATHER, LEGGINGS_ITEM));
        save("item/steel_boots", map(STEEL, LEATHER, BOOTS_ITEM));

        // Materials (spec 4.6).
        save("item/sulfur", map(SULFUR, SULFUR_LUMP));
        save("item/sulfur_dust", map(SULFUR, DUST_HEAP));
        save("item/charcoal_dust", map(CHARCOAL, DUST_HEAP));

        // Coke oven, coke and creosote (spec 21.2 and 21.4).
        save("block/coke_oven_bricks", cokeOvenBricks());
        save("block/coke_oven_door", cokeOvenDoor(-1));
        saveAnimated("block/coke_oven_door_lit", cokeOvenDoorLit(), 3);
        save("block/coke_block", cokeBlock());
        save("block/treated_planks", treatedPlanks());
        save("item/coke", cokeLump());
        save("item/coke_dust", map(COKE, COKE_DUST_HEAP));
        save("item/unfired_coke_oven_brick", unfiredCokeOvenBrick());
        save("item/coke_oven_brick", cokeOvenBrickItem());
        save("item/creosote_bucket", V2.limitColours(map(WROUGHT_IRON, CREOSOTE, CREOSOTE_BUCKET), 12));
        save("item/treated_stick", art(TREATED_WOOD, TREATED_STICK));
        saveRaw("gui/coke_oven", cokeOvenGui());

        // Refractory ceramics (spec 21.2 and 21.4): the clay silhouettes in fire clay, then the refractory ramp.
        save("block/refractory_crucible_side", refractoryCrucibleSide());
        save("block/refractory_crucible_top", refractoryCrucibleTop());
        save("block/refractory_crucible_inside", refractoryCrucibleInside());
        save("block/refractory_crucible_bottom", refractoryPlain(7474));
        save("item/unfired_refractory_crucible", art(FIRE_CLAY, CRUCIBLE_ITEM));
        save("item/refractory_crucible", art(REFRACTORY, CRUCIBLE_ITEM));
        save("item/unfired_refractory_ingot_mold", art(FIRE_CLAY, INGOT_MOLD_ITEM));
        save("item/refractory_ingot_mold", art(REFRACTORY, INGOT_MOLD_ITEM));
        save("item/refractory_ingot_mold_filled", filledIngotMold(REFRACTORY));
        save("item/unfired_refractory_gear_mold", mold(FIRE_CLAY, GEAR_CAVITY));
        save("item/refractory_gear_mold", mold(REFRACTORY, GEAR_CAVITY));
        save("item/refractory_gear_mold_filled", filledMold(REFRACTORY, GEAR_CAVITY));
        save("item/unfired_gear_mold", mold(CLAY, GEAR_CAVITY));
        save("item/gear_mold", mold(CERAMIC, GEAR_CAVITY));
        save("item/gear_mold_filled", filledMold(CERAMIC, GEAR_CAVITY));
        steam();
    }

    static void save(String path, BufferedImage im) throws IOException {
        PREVIEW.put(path, im);
        if (HAND_MADE.contains(path) && Files.exists(OUT.resolve(path + ".png"))) return;
        File f = OUT.resolve(path + ".png").toFile();
        f.getParentFile().mkdirs();
        ImageIO.write(im, "png", f);
    }

    /** Writes a non-16x16 texture (GUI sheets); kept out of the item preview. */
    static void saveRaw(String path, BufferedImage im) throws IOException {
        File f = OUT.resolve(path + ".png").toFile();
        f.getParentFile().mkdirs();
        ImageIO.write(im, "png", f);
    }

    static void preview() throws IOException {
        int cols = 16, scale = 4, cell = 16 * scale + 8;
        int rows = (PREVIEW.size() + cols - 1) / cols;
        BufferedImage sheet = new BufferedImage(cols * cell, rows * cell, BufferedImage.TYPE_INT_ARGB);
        var g = sheet.getGraphics();
        g.setColor(new java.awt.Color(0x24, 0x24, 0x28));
        g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        int i = 0;
        for (var e : PREVIEW.entrySet()) {
            BufferedImage im = e.getValue();
            if (Files.exists(OUT.resolve(e.getKey() + ".png"))) im = ImageIO.read(OUT.resolve(e.getKey() + ".png").toFile());
            int x = (i % cols) * cell + 4, y = (i / cols) * cell + 4;
            g.drawImage(im.getScaledInstance(16 * scale, 16 * scale, java.awt.Image.SCALE_FAST), x, y, null);
            i++;
        }
        File f = new File("build/texturegen/preview.png");
        f.getParentFile().mkdirs();
        ImageIO.write(sheet, "png", f);
    }

    public static void main(String[] args) throws IOException {
        itemsV2 = true;
        for (Rock rock : ROCKS) {
            save("block/" + rock.name(), rock(rock));
            save("block/cobbled_" + rock.name(), cobbled(rock));
            save("item/loose_" + rock.name(), map(rock.ramp(), LOOSE_ROCK));
        }
        for (Mineral m : MINERALS) {
            for (String grade : List.of("poor", "normal", "rich"))
                save("block/ore/" + m.name() + "_" + grade, oreOverlay(m, grade));
            String n = m.name();
            save("item/poor_" + n, map(m.ramp(), ORE_SMALL));
            save("item/" + n, map(m.ramp(), ORE_NORMAL));
            save("item/rich_" + n, map(m.ramp(), ORE_RICH));
            save("item/crushed_poor_" + n, map(m.ramp(), CRUSHED_SMALL));
            save("item/crushed_" + n, map(m.ramp(), CRUSHED_NORMAL));
            save("item/crushed_rich_" + n, map(m.ramp(), CRUSHED_RICH));
            save("item/small_" + n, map(m.ramp(), NUGGET));
            save("block/small_" + n, pebbles(m));
        }
        save("item/flint_shard", map(V2.FLINT_V2, FLINT_SHARD));
        itemsV2 = false;
        save("block/loose_stick", art(WOOD, LOOSE_STICKS));
        save("block/loose_flint", art(FLINT, LOOSE_FLINTS));
        itemsV2 = true;

        // Stone age (tier 0-2 spec 3).
        save("item/straw", art(V2.STRAW_V2, STRAW_SHEAF));
        save("item/twine", art(V2.FIBRE_V2, TWINE_HANK));
        save("item/plant_fibre", plantFibre());
        save("item/field_journal", art(FIELD_JOURNAL, V2.STRAW_V2, V2.FIBRE_V2, PAPER));
        save("item/fibre_cloth", fibreCloth());
        save("item/stone_axe_head", map(V2.FLINT_V2, KNAPPED_AXE_HEAD));
        save("item/stone_knife_blade", map(V2.FLINT_V2, KNAPPED_KNIFE_BLADE));
        save("item/stone_shovel_head", map(V2.FLINT_V2, KNAPPED_SHOVEL_HEAD));
        save("item/stone_hoe_head", map(V2.FLINT_V2, KNAPPED_HOE_HEAD));
        save("item/stone_hammer_head", map(V2.FLINT_V2, KNAPPED_HAMMER_HEAD));
        save("item/stone_spear_head", map(V2.FLINT_V2, KNAPPED_SPEAR_HEAD));
        save("item/stone_pickaxe_head", pickaxeHeadV2(V2.FLINT_V2));
        for (String kind : List.of("axe", "knife", "shovel", "hoe", "hammer", "pickaxe"))
            save("item/stone_" + kind, tool(V2.FLINT_V2, V2.WOOD_V2, V2.FIBRE_V2, kind));
        save("gui/knapping/flint", flintSurface());
        saveRaw("gui/knapping", knappingGui());

        // Fire (spec 3.5).
        itemsV2 = false;
        save("block/fire_pit_stones", fieldStones());
        save("block/fire_pit_wood", stickWood());
        save("block/fire_pit_ash", ashBed());
        save("block/fire_pit_embers", emberBed());
        itemsV2 = true;
        save("item/firestarter", firestarter());
        itemsV2 = false;
        saveRaw("gui/fire_pit", firePitGui());
        itemsV2 = true;

        // Clay (spec 4.1 to 4.3).
        itemsV2 = false;
        save("gui/knapping/clay", claySurface());
        itemsV2 = true;
        for (var piece : List.of(java.util.Map.entry("small_vessel", SMALL_VESSEL_ITEM), java.util.Map.entry("large_vessel", LARGE_VESSEL_ITEM),
                java.util.Map.entry("crucible", CRUCIBLE_ITEM), java.util.Map.entry("ingot_mold", INGOT_MOLD_ITEM))) {
            save("item/unfired_" + piece.getKey(), art(V2.CLAY_V2, piece.getValue()));
            save("item/" + piece.getKey(), art(V2.CERAMIC_V2, piece.getValue()));
        }
        save("item/unfired_brick", art(V2.CLAY_V2, BRICK_ITEM));
        save("item/ingot_mold_filled", filledIngotMold(V2.CERAMIC_V2));
        for (var cavity : MOLD_CAVITIES.entrySet()) {
            save("item/unfired_" + cavity.getKey() + "_mold", mold(V2.CLAY_V2, cavity.getValue()));
            save("item/" + cavity.getKey() + "_mold", mold(V2.CERAMIC_V2, cavity.getValue()));
            save("item/" + cavity.getKey() + "_mold_filled", filledMold(V2.CERAMIC_V2, cavity.getValue()));
        }
        itemsV2 = false;
        save("block/large_vessel_side", ceramicWall(6161, true));
        save("block/large_vessel_top", vesselTop());
        save("block/large_vessel_bottom", ceramicPlain(6262, 2, 3));
        save("block/crucible_side", crucibleSide());
        save("block/crucible_top", crucibleTop());
        save("block/crucible_inside", crucibleInside());
        save("block/crucible_bottom", ceramicPlain(7373, 2, 3));
        save("block/pit_kiln_thatch", thatch());
        save("block/pit_kiln_embers", emberBed(1717));
        saveRaw("gui/small_vessel", smallVesselGui());

        // Charcoal (spec 4.4).
        save("block/log_pile_side", logPileSide());
        save("block/log_pile_top", logPileTop());
        save("block/charcoal_pile", charcoalPile());
        itemsV2 = true;
        save("item/ash", art(V2.ASH_V2, ASH_HEAP));
        itemsV2 = false;

        // Forge (spec 4.5).
        save("block/forge_side", forgeBricks(7070));
        save("block/forge_top", forgeBricks(7171));
        save("block/forge_coals", forgeCoals());
        save("block/forge_embers", emberBed(919, 0.12));
        saveRaw("gui/forge", forgeGui());

        // Metals (spec 6 to 8): one shared shape per form, recoloured per metal ramp.
        java.util.Map<String, String[]> heads = new java.util.LinkedHashMap<>();
        heads.put("pickaxe_head", PICKAXE_HEAD);
        heads.put("axe_head", AXE_HEAD);
        heads.put("shovel_head", SHOVEL_HEAD);
        heads.put("hoe_head", HOE_HEAD);
        heads.put("knife_blade", KNIFE_BLADE);
        heads.put("hammer_head", HAMMER_HEAD);
        heads.put("saw_blade", SAW_BLADE);
        heads.put("sword_blade", SWORD_BLADE);
        itemsV2 = true;
        for (Metal metal : METALS) {
            String n = metal.name();
            Ramp mr = v2Metal(n);
            if (!metal.vanillaIngot()) {
                save("item/" + n + "_ingot", map(mr, INGOT_V2));
                save("item/" + n + "_nugget", map(mr, NUGGET));
            }
            if (n.equals("tin") || n.equals("bismuth")) continue;
            save("item/" + n + "_plate", map(mr, PLATE));
            for (var head : heads.entrySet())
                save("item/" + n + "_" + head.getKey(), head.getKey().equals("pickaxe_head") ? pickaxeHeadV2(mr) : map(mr, head.getValue()));
            for (String kind : List.of("pickaxe", "axe", "shovel", "hoe", "knife", "hammer", "saw", "sword")) {
                // Copper's pickaxe, axe, shovel, hoe and sword are the vanilla items and keep vanilla art.
                if (metal.vanillaIngot() && !List.of("knife", "hammer", "saw").contains(kind)) continue;
                save("item/" + n + "_" + kind, tool(mr, V2.WOOD_V2, null, kind));
            }
        }
        save("item/slag_metal_ingot", map(V2.SLAG_V2, INGOT_V2));
        for (Metal metal : METALS) {
            String n = metal.name();
            if (!n.contains("bronze")) continue;
            Ramp mr = v2Metal(n);
            saveRaw("entity/equipment/humanoid/" + n, armourLayer(mr));
            saveRaw("entity/equipment/humanoid_leggings/" + n, leggingsLayer(mr));
            save("item/" + n + "_helmet", map(mr, V2.FIBRE_V2, HELMET_ITEM));
            save("item/" + n + "_chestplate", map(mr, V2.FIBRE_V2, CHESTPLATE_ITEM));
            save("item/" + n + "_leggings", map(mr, V2.FIBRE_V2, LEGGINGS_ITEM));
            save("item/" + n + "_boots", map(mr, V2.FIBRE_V2, BOOTS_ITEM));
            save("item/" + n + "_prospectors_pick_head", map(mr, PROSPECTOR_HEAD));
            save("item/" + n + "_prospectors_pick", tool(mr, V2.WOOD_V2, null, "prospectors_pick"));
        }
        itemsV2 = false;
        saveRaw("gui/crucible", crucibleGui());

        saveRaw("gui/anvil", anvilGui());
        for (Rock rock : ROCKS) {
            if (rock.category().equals("intrusive") || rock.category().equals("extrusive"))
                save("block/" + rock.name() + "_anvil_top", stoneAnvilTop(rock));
        }
        save("block/bronze_anvil", bronzeAnvilBody());
        save("block/bronze_anvil_top", bronzeAnvilTop());
        itemsV2 = true;
        save("item/tongs_jaw", map(V2.BRONZE_V2, TONGS_JAW_ITEM));
        save("item/tongs", tongs());
        itemsV2 = false;

        // Quern (spec 10.1).
        save("block/quern_side", quernSide());
        save("block/quern_top", quernFace(5252, false));
        save("block/quern_runner", quernFace(5353, true));
        itemsV2 = true;
        save("item/quernstone", art(GRANITE_V2, QUERNSTONE_ITEM));
        itemsV2 = false;

        itemsV2 = true;
        tier3();
        kinetics();
        machines();
        washing();
        windAndTanning();
        tier4();
        tier5Ores();

        // Glow layers for hot metal: written last, from the finished item textures.
        glowLayers();
        if (args.length > 0 && args[0].equals("--preview-only")) { preview(); return; }
        preview();
        System.out.println("Wrote " + PREVIEW.size() + " textures");
    }
}
