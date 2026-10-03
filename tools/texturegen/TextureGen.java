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
    static final java.util.Set<String> HAND_MADE = java.util.Set.of(
            "block/granite", "block/basalt", "block/limestone", "item/plant_fibre");

    // ---------------------------------------------------------------- palette (5-step ramps)

    record Ramp(int[] c, int spec) {
        int get(int step) { return c[Math.max(0, Math.min(4, step - 1))]; }
    }

    static Ramp ramp(int spec, int... c) { return new Ramp(c, spec); }

    // Rocks
    static final Ramp GRANITE = ramp(0, 0x3e3236, 0x5a464a, 0x7a6062, 0x9a7c78, 0xb89a90);
    static final Ramp BASALT = ramp(0, 0x1f2226, 0x2c3036, 0x3b4048, 0x4c525a, 0x5f6670);
    static final Ramp LIMESTONE = ramp(0, 0x6e6655, 0x8a8169, 0xa69c80, 0xc2b898, 0xd8cfaf);
    static final Ramp SHALE = ramp(0, 0x262a32, 0x343944, 0x454b55, 0x575d66, 0x6a6f76);
    static final Ramp SLATE = ramp(0, 0x2c322f, 0x3c4440, 0x4e5751, 0x626b64, 0x767f77);
    static final Ramp MARBLE = ramp(0, 0x8c8a88, 0xaeaca8, 0xc8c6c0, 0xdcdad4, 0xecebe6);
    static final Ramp GABBRO = ramp(0, 0x16181c, 0x22252a, 0x303338, 0x41444a, 0x55585c);
    static final Ramp RHYOLITE = ramp(0, 0x6a5450, 0x84695f, 0x9c8072, 0xb59c8e, 0xc8b2a2);
    static final Ramp FLINT = ramp(0, 0x23252b, 0x383b44, 0x525764, 0x727986, 0x9aa0ac);
    // Metals
    static final Ramp COPPER = ramp(0xf6cf9a, 0x4a2218, 0x7a3a22, 0xa8562e, 0xcf7a3e, 0xeba66a);
    static final Ramp TIN = ramp(0xeef1f5, 0x3a3d48, 0x5c6170, 0x838998, 0xaeb4c0, 0xd6dbe2);
    static final Ramp BRONZE = ramp(0xf2e0a6, 0x4a3216, 0x75501f, 0xa0752e, 0xc79a45, 0xe3c273);
    static final Ramp BISMUTH = ramp(0xf0e8f2, 0x3e3444, 0x5e5066, 0x85758c, 0xab9cb0, 0xcbc0cf);
    static final Ramp ARSENICAL_BRONZE = ramp(0xf0d2b2, 0x46281a, 0x6e4128, 0x965c3c, 0xb87a52, 0xd8a072);
    static final Ramp BISMUTH_BRONZE = ramp(0xf0dcc6, 0x48301e, 0x704c34, 0x96684c, 0xb68a6a, 0xd4ac8c);
    static final Ramp SLAG = ramp(0, 0x2a2420, 0x3e3632, 0x544a44, 0x6e625a, 0x887c72);
    // Minerals
    static final Ramp MALACHITE = ramp(0, 0x163828, 0x1e4a36, 0x2e6b4a, 0x4a9466, 0x78bf8a);
    static final Ramp CASSITERITE = ramp(0xc8b8a8, 0x1e1614, 0x2a1e1a, 0x4a342a, 0x6e5444, 0x9a8070);
    static final Ramp TENNANTITE = ramp(0xc6d0d6, 0x1a1e24, 0x2a3038, 0x404852, 0x5c6672, 0x84909a);
    static final Ramp BISMUTHINITE = ramp(0xe2e6ee, 0x26282e, 0x3a3e48, 0x565c6a, 0x7a8292, 0xa4acba);
    // Organics and ceramics
    static final Ramp WOOD = ramp(0, 0x3a2618, 0x5a3c24, 0x7a5634, 0x9c7448, 0xbc9462);
    static final Ramp FIBRE = ramp(0, 0x2e3818, 0x3d4a22, 0x5a6b2e, 0x7a8c3c, 0x9dae52);
    static final Ramp STRAW = ramp(0, 0x5a4a1e, 0x7c662a, 0xa08a3c, 0xc4ad56, 0xdcca78);
    static final Ramp CLAY = ramp(0, 0x5a4c4c, 0x6a5a58, 0x8e7a74, 0xae9890, 0xc8b4aa);
    static final Ramp CERAMIC = ramp(0, 0x5a2e22, 0x83432e, 0xa85e3e, 0xc77e56, 0xdda27a);
    static final Ramp CHARCOAL = ramp(0, 0x141416, 0x232327, 0x34343a, 0x4a4a52, 0x626270);
    static final Ramp ASH = ramp(0, 0x4a4846, 0x64615e, 0x807c78, 0x9c9894, 0xb8b4ae);
    static final Ramp LEATHER = ramp(0, 0x3a2216, 0x553322, 0x744834, 0x92614a, 0xb07e62);

    record Rock(String name, Ramp ramp, String category) {}

    static final List<Rock> ROCKS = List.of(
            new Rock("limestone", LIMESTONE, "sedimentary"),
            new Rock("shale", SHALE, "sedimentary"),
            new Rock("slate", SLATE, "metamorphic"),
            new Rock("marble", MARBLE, "metamorphic"),
            new Rock("granite", GRANITE, "intrusive"),
            new Rock("gabbro", GABBRO, "intrusive"),
            new Rock("basalt", BASALT, "extrusive"),
            new Rock("rhyolite", RHYOLITE, "extrusive"));

    record Mineral(String name, Ramp ramp, boolean metallic) {}

    static final List<Mineral> MINERALS = List.of(
            new Mineral("native_copper", COPPER, true),
            new Mineral("malachite", MALACHITE, false),
            new Mineral("tennantite", TENNANTITE, true),
            new Mineral("cassiterite", CASSITERITE, true),
            new Mineral("bismuthinite", BISMUTHINITE, true));

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

    /** Adds the 1 px coloured outline around an item silhouette (style guide 5). */
    static BufferedImage outline(BufferedImage im) {
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

    static BufferedImage rock(Rock rock) {
        long seed = rock.name().hashCode() * 31L;
        Ramp p = rock.ramp();
        BufferedImage im = img();
        Random r = new Random(seed);
        switch (rock.category()) {
            case "intrusive" -> {
                double[][] n = fractal(seed);
                for (int y = 0; y < 16; y++)
                    for (int x = 0; x < 16; x++) px(im, x, y, p.get(n[y][x] < 0.42 ? 2 : n[y][x] < 0.6 ? 3 : 4));
                for (int i = 0; i < 9; i++) speck(im, r, r.nextInt(16), r.nextInt(16), p.get(5), p.get(4), 1 + r.nextInt(2));
                for (int i = 0; i < 9; i++) speck(im, r, r.nextInt(16), r.nextInt(16), p.get(1), p.get(2), 1 + r.nextInt(2));
            }
            case "extrusive" -> {
                double[][] n = noise(seed, 8);
                for (int y = 0; y < 16; y++)
                    for (int x = 0; x < 16; x++) px(im, x, y, p.get(n[y][x] < 0.35 ? 2 : n[y][x] < 0.75 ? 3 : 4));
                for (int i = 0; i < 4; i++) {
                    int x = r.nextInt(16), y = r.nextInt(16);
                    pxWrap(im, x, y, p.get(1));
                    pxWrap(im, x + 1, y, p.get(1));
                    pxWrap(im, x, y + 1, p.get(4));
                    pxWrap(im, x + 1, y + 1, p.get(4));
                }
            }
            case "sedimentary" -> {
                int[] beds = new int[16];
                int tone = 3, y = 0;
                while (y < 16) {
                    int h = 3 + r.nextInt(3);
                    for (int i = 0; i < h && y < 16; i++, y++) beds[y] = tone;
                    tone = tone == 3 ? (r.nextBoolean() ? 4 : 2) : 3;
                }
                double[][] n = noise(seed, 4);
                for (y = 0; y < 16; y++)
                    for (int x = 0; x < 16; x++) {
                        int s = beds[y] + (n[y][x] > 0.8 ? 1 : n[y][x] < 0.15 ? -1 : 0);
                        px(im, x, y, p.get(Math.max(2, Math.min(4, s))));
                    }
                // Broken parting lines between beds.
                for (y = 1; y < 16; y++) {
                    if (beds[y] == beds[y - 1]) continue;
                    int x = r.nextInt(16);
                    int len = 5 + r.nextInt(6);
                    for (int i = 0; i < len; i++) pxWrap(im, x + i, y, p.get(1));
                    int x2 = x + len + 2 + r.nextInt(3);
                    for (int i = 0; i < 3; i++) pxWrap(im, x2 + i, y, p.get(2));
                }
            }
            case "metamorphic" -> {
                boolean marble = rock.name().equals("marble");
                double[][] n = noise(seed, 8);
                for (int yy = 0; yy < 16; yy++)
                    for (int x = 0; x < 16; x++) {
                        // Diagonal foliation: bands along x + y.
                        int band = Math.floorMod(yy * 4 - x, 16) / 4;
                        double v = n[yy][x] * (marble ? 0.6 : 0.35) + (marble ? 0.4 : (band % 2 == 0 ? 0.55 : 0.3));
                        px(im, x, yy, p.get(marble ? (v < 0.5 ? 3 : 4) : (v < 0.42 ? 2 : v < 0.62 ? 3 : 4)));
                    }
                if (marble) {
                    // Two thin grey veins running diagonally.
                    for (int v = 0; v < 2; v++) {
                        int x = r.nextInt(16), y0 = 0;
                        for (int yy = 0; yy < 16; yy++) {
                            pxWrap(im, x, y0 + yy, p.get(yy % 5 == 0 ? 1 : 2));
                            if (r.nextInt(3) == 0) x += r.nextBoolean() ? 1 : -1;
                            else x += 1;
                        }
                    }
                } else {
                    // Slate: thin parallel sheets with dark cleavage lines.
                    for (int i = 0; i < 5; i++) {
                        int y0 = r.nextInt(16), x0 = r.nextInt(16), len = 4 + r.nextInt(5);
                        for (int k = 0; k < len; k++) pxWrap(im, x0 + 2 * k, y0 - k, p.get(1));
                    }
                }
            }
            default -> throw new IllegalArgumentException(rock.category());
        }
        return im;
    }

    /** Cobble: rounded clusters with dark gaps (style guide 7). */
    static BufferedImage cobbled(Rock rock) {
        Ramp p = rock.ramp();
        BufferedImage im = img();
        // Fill with gap colour, then paint stones on a jittered grid.
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) px(im, x, y, p.get(1));
        Random r = new Random(rock.name().hashCode() * 17L);
        int[][] owner = new int[16][16];
        int[][] centres = new int[9][2];
        for (int i = 0; i < 9; i++) {
            centres[i][0] = (i % 3) * 5 + 2 + r.nextInt(3);
            centres[i][1] = (i / 3) * 5 + 2 + r.nextInt(3);
        }
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int best = 0, second = 0;
                double bd = 1e9, sd = 1e9;
                for (int i = 0; i < 9; i++) {
                    for (int ox = -16; ox <= 16; ox += 16)
                        for (int oy = -16; oy <= 16; oy += 16) {
                            double dx = x - centres[i][0] - ox, dy = y - centres[i][1] - oy;
                            double d = Math.sqrt(dx * dx + dy * dy);
                            if (d < bd) { sd = bd; second = best; bd = d; best = i; }
                            else if (d < sd) { sd = d; second = i; }
                        }
                }
                owner[y][x] = best;
                if (sd - bd < 1.0) continue; // gap
                // Lit from the top-left: pixels up-left of the centre are lighter.
                int cx = centres[best][0], cy = centres[best][1];
                double rel = ((x - cx + 24) % 16 - 8 + (y - cy + 24) % 16 - 8);
                int step = rel < -2 ? 4 : rel > 2 ? 2 : 3;
                if (bd < 1.2 && r.nextInt(3) == 0) step = 5;
                px(im, x, y, p.get(step));
            }
        return im;
    }

    // ---------------------------------------------------------------- ores

    /** Mineral overlay per grade: grain count and size show the grade (style guide 7). */
    static BufferedImage oreOverlay(Mineral m, String grade) {
        BufferedImage im = img();
        Random r = new Random((m.name() + grade).hashCode());
        int small, large;
        switch (grade) {
            case "poor" -> { small = 3; large = 0; }
            case "normal" -> { small = 4; large = 1; }
            default -> { small = 5; large = 3; }
        }
        List<int[]> placed = new ArrayList<>();
        int attempts = 0;
        for (int i = 0; i < small + large && attempts < 400; attempts++) {
            boolean big = i < large;
            int size = big ? 3 : (grade.equals("poor") && i == 0 ? 1 : 2);
            int x = 1 + r.nextInt(14 - size), y = 1 + r.nextInt(14 - size);
            boolean clash = false;
            for (int[] q : placed) if (Math.abs(q[0] - x) < size + 2 && Math.abs(q[1] - y) < size + 2) clash = true;
            if (clash) continue;
            placed.add(new int[] {x, y, size});
            drawGrain(im, m, x, y, size, grade.equals("rich") && big);
            i++;
        }
        return im;
    }

    static void drawGrain(BufferedImage im, Mineral m, int x, int y, int size, boolean bright) {
        Ramp p = m.ramp();
        int lift = bright ? 1 : 0;
        if (size == 1) {
            px(im, x, y, p.get(4 + lift));
            px(im, x + 1, y, p.get(3));
            return;
        }
        if (size == 2) {
            px(im, x, y, p.get(4 + lift));
            px(im, x + 1, y, p.get(3 + lift));
            px(im, x, y + 1, p.get(3));
            px(im, x + 1, y + 1, p.get(2));
            return;
        }
        // 3x3 rounded grain
        px(im, x + 1, y, p.get(4 + lift));
        px(im, x, y + 1, p.get(4 + lift));
        px(im, x + 1, y + 1, m.metallic() && p.spec() != 0 ? p.spec() : p.get(5));
        px(im, x + 2, y + 1, p.get(3));
        px(im, x + 1, y + 2, p.get(2));
        px(im, x + 2, y + 2, p.get(1 + lift));
        px(im, x, y + 2, p.get(2));
        px(im, x + 2, y, p.get(3));
    }

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

    /** Diagonal tool: handle bottom-left to head top-right. */
    static BufferedImage tool(Ramp head, Ramp handle, Ramp lashing, String kind) {
        BufferedImage im = img();
        // Handle: a diagonal stick from (2,13) towards (10,5).
        int end = switch (kind) {
            case "knife", "sword" -> 6;
            case "shovel", "spear" -> 9;
            default -> 10;
        };
        for (int i = 0; i < end; i++) {
            int x = 2 + i, y = 13 - i;
            px(im, x, y, handle.get(3));
            px(im, x + 1, y, handle.get(2));
            if (i % 3 == 0) px(im, x, y, handle.get(4));
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
            case "axe" -> drawAt(im, h, new String[] {"..455", ".4443", "44333", "43332", ".332.", "..2.."}, 9, 1);
            case "shovel" -> drawAt(im, h, new String[] {"..45.", ".4445", "44433", "43332", "4332.", ".2..."}, 9, 1);
            case "hoe" -> drawAt(im, h, new String[] {"4444455", "43333..", "32....."}, 7, 2);
            case "knife" -> drawAt(im, h, new String[] {"......45", ".....453", "....443.", "...443..", "..443...", ".332....", ".2......"}, 6, 1);
            case "hammer" -> drawAt(im, h, new String[] {"..4455.", ".44433s", "4433332", ".33322.", "..32..."}, 8, 1);
            case "saw" -> drawAt(im, h, new String[] {"........45", ".......443", "......443.", ".....443..", "....443...", "...332....", "..32.....", ".2........"}, 6, 0);
            case "sword" -> drawAt(im, h, SWORD_BLADE, 5, 0);
            case "spear" -> drawAt(im, h, new String[] {"..455", ".4453", "44332", "4332.", ".2..."}, 10, 1);
            case "prospectors_pick" -> drawAt(im, h, new String[] {"...4455", ".443334", "43....3", "3......"}, 6, 2);
            default -> throw new IllegalArgumentException(kind);
        }
        return outline(im);
    }

    static void drawAt(BufferedImage im, Ramp a, String[] rows, int left, int top) {
        for (int y = 0; y < rows.length; y++)
            for (int x = 0; x < rows[y].length(); x++) {
                char ch = rows[y].charAt(x);
                if (ch >= '1' && ch <= '5') px(im, left + x, top + y, a.get(ch - '0'));
                else if (ch == 's') px(im, left + x, top + y, a.spec() != 0 ? a.spec() : a.get(5));
            }
    }

    // ---------------------------------------------------------------- output

    static void save(String path, BufferedImage im) throws IOException {
        PREVIEW.put(path, im);
        if (HAND_MADE.contains(path) && Files.exists(OUT.resolve(path + ".png"))) return;
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
            save("block/small_" + n, oreOverlay(m, "rich"));
        }
        save("item/flint_shard", map(FLINT, FLINT_SHARD));
        if (args.length > 0 && args[0].equals("--preview-only")) { preview(); return; }
        preview();
        System.out.println("Wrote " + PREVIEW.size() + " textures");
    }
}
