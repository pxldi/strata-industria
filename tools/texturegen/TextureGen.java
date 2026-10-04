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
            "block/granite", "block/basalt", "block/limestone", "item/plant_fibre", "item/stone_axe");

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
    static final Ramp PAPER = ramp(0, 0x6e6250, 0x948670, 0xb8aa8e, 0xd6caae, 0xece2c8);

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
            // A wide blade with teeth along its lower edge.
            case "saw" -> drawAt(im, h, new String[] {"........455", ".......44432", "......4443.", ".....44432.", "....4443...", "...44432...", "..332......", ".2........."}, 5, 0);
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
        BufferedImage im = mold(CERAMIC, cavity);
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
        BufferedImage im = art(CERAMIC, INGOT_MOLD_ITEM);
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
        double[][] n = fractal(5151);
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
        double[][] n = fractal(seed);
        BufferedImage im = img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int step = n[y][x] > 0.62 ? 4 : 3;
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
        double[][] n = fractal(seed);
        BufferedImage im = img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) px(im, x, y, CERAMIC.get(n[y][x] > 0.55 ? high : low));
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
        double[][] n = fractal(4141);
        BufferedImage im = img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int step = n[y][x] > 0.6 ? 4 : n[y][x] < 0.35 ? 2 : 3;
                if (x == 3 || x == 4) step = Math.min(5, step + 1);
                px(im, x, y, BRONZE.get(step));
            }
        return im;
    }

    /** Bronze anvil face: polished from use, lighter in the middle. */
    static BufferedImage bronzeAnvilTop() {
        double[][] n = fractal(4242);
        BufferedImage im = img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double d = Math.abs(x - 7.5) / 8 + Math.abs(y - 7.5) / 16;
                int step = d < 0.45 ? 5 : d < 0.75 ? 4 : 3;
                if (n[y][x] < 0.3) step--;
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
        return map(BRONZE, WOOD, TONGS_ITEM);
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
        double[][] n = fractal(5151);
        BufferedImage im = img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int step = n[y][x] > 0.62 ? 4 : n[y][x] < 0.38 ? 2 : 3;
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
        double[][] n = fractal(seed);
        BufferedImage im = img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double dx = x - 7.5, dy = y - 7.5;
                double r = Math.sqrt(dx * dx + dy * dy);
                double a = Math.atan2(dy, dx);
                int step = n[y][x] > 0.6 ? 4 : 3;
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
        double[][] n = fractal(8282);
        BufferedImage im = img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int step = n[y][x] > 0.6 ? 4 : 3;
                if (x % 5 == 1 && n[y][x] < 0.5) step = 3;
                int c = CERAMIC.get(step);
                if (y > 11 + (int) Math.round(n[y][x] * 2)) c = CHARCOAL.get(n[y][x] > 0.5 ? 4 : 3);
                px(im, x, y, c);
            }
        return im;
    }

    /** Crucible rim seen from above: lit on the near edges. */
    static BufferedImage crucibleTop() {
        BufferedImage im = img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) px(im, x, y, CERAMIC.get((x < 3 || y < 3) ? 5 : 4));
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
    static BufferedImage forgeBricks(long seed) {
        BufferedImage im = img();
        Random r = new Random(seed);
        for (int y = 0; y < 16; y++) {
            int course = y / 4, row = y % 4;
            for (int x = 0; x < 16; x++) {
                int joint = (x + (course % 2 == 1 ? 4 : 0)) % 8;
                int c;
                if (row == 3 || joint == 7) c = CERAMIC.get(1);
                else if (row == 0) c = CERAMIC.get(4);
                else if (joint == 6 || row == 2) c = CERAMIC.get(r.nextInt(5) == 0 ? 2 : 3);
                else c = CERAMIC.get(r.nextInt(6) == 0 ? 4 : 3);
                px(im, x, y, c);
            }
        }
        return im;
    }

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
        // Gauge: 0 to 1500 degrees over 58 px, a notch every 250.
        well(im, 150, 16, 12, 60, 0x2a2a2a);
        for (int t = 250; t < 1500; t += 250) {
            int y = 17 + 58 - Math.round(t / 1500f * 58);
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
        // Gauge: 0 to 1500 degrees over 52 px, a notch every 250.
        well(im, 159, 17, 10, 54, 0x2a2a2a);
        for (int t = 250; t < 1500; t += 250) {
            int y = 18 + 52 - Math.round(t / 1500f * 52);
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
        return im;
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
    static final Ramp FIRE_BRICK = ramp(0, 0x6a5434, 0x8c7044, 0xae9058, 0xc8ac72, 0xe0c890);
    static final Ramp LIGNITE = ramp(0, 0x1a1410, 0x2a2018, 0x3c2e22, 0x52402e, 0x6a543c);
    // Earths for the tier 3 ground blocks, built per SG 3 (low contrast, cool darks, warm lights).
    static final Ramp MUD = ramp(0, 0x1a1818, 0x29241f, 0x383028, 0x483d32, 0x5c4e40);
    static final Ramp SAND = ramp(0, 0x8a7656, 0xa69068, 0xc0aa7c, 0xd4c092, 0xe6d6ac);

    static final List<Mineral> T3_MINERALS = List.of(
            new Mineral("hematite", HEMATITE, false),
            new Mineral("magnetite", MAGNETITE, true),
            new Mineral("native_gold", GOLD, true),
            new Mineral("limonite", LIMONITE, false));

    /** Heat band (SG 6), dark red to white, for the lit bloomery door. */
    static final int[] HEAT_BAND = {0x6e1e14, 0xa0281a, 0xd23a1e, 0xf07a22, 0xf8c23a, 0xfff4d0};

    /**
     * Stamps a small pattern: digits are ramp steps, 's' is the specular colour when {@code spec}
     * is set and step {@code fallback} otherwise, '.' is left alone.
     */
    static void stamp(BufferedImage im, Ramp a, String[] rows, int x0, int y0, boolean spec, int fallback) {
        for (int y = 0; y < rows.length; y++)
            for (int x = 0; x < rows[y].length(); x++) {
                char ch = rows[y].charAt(x);
                if (ch >= '1' && ch <= '5') px(im, x0 + x, y0 + y, a.get(ch - '0'));
                else if (ch == 's') px(im, x0 + x, y0 + y, spec && a.spec() != 0 ? a.spec() : a.get(fallback));
            }
    }

    /**
     * An ore overlay built from per-mineral piece shapes (tiny, small, large). Grade by count and
     * size (SG 7): poor 3 mostly tiny, normal 5 with one large, rich 8 with three large. The first
     * piece gets the single specular pixel of a metallic mineral.
     */
    static BufferedImage pieceOverlay(Mineral m, String grade, String[][] shapes, int fallback) {
        Random r = new Random((m.name() + "/" + grade).hashCode() * 131L);
        int[] kinds = switch (grade) {
            case "poor" -> new int[] {1, 0, 0};
            case "normal" -> new int[] {2, 1, 1, 1, 0};
            default -> new int[] {2, 2, 2, 1, 1, 1, 0, 0};
        };
        // Random placement with spacing; start over until every piece fits.
        for (int trial = 0; trial < 500; trial++) {
            BufferedImage im = placePieces(m, r, kinds, shapes, fallback, trial < 250 ? 2 : 1);
            if (im != null) return im;
        }
        throw new IllegalStateException("could not place " + m.name() + " " + grade);
    }

    static BufferedImage placePieces(Mineral m, Random r, int[] kinds, String[][] shapes, int fallback, int gap) {
        BufferedImage im = img();
        List<int[]> placed = new ArrayList<>();
        int i = 0;
        for (int attempts = 0; i < kinds.length && attempts < 400; attempts++) {
            String[] s = shapes[kinds[i]];
            int w = s[0].length(), h = s.length;
            int x = 1 + r.nextInt(15 - w), y = 1 + r.nextInt(15 - h);
            boolean clash = false;
            for (int[] q : placed)
                if (x < q[0] + q[2] + gap && q[0] < x + w + gap && y < q[1] + q[3] + gap && q[1] < y + h + gap) clash = true;
            if (clash) continue;
            placed.add(new int[] {x, y, w, h});
            stamp(im, m.ramp(), s, x, y, m.metallic() && i == 0, fallback);
            i++;
        }
        return i < kinds.length ? null : im;
    }

    /** Hematite: flat red-brown lenses lying along the bedding, matte. */
    static final String[][] HEMATITE_LENSES = {
            {"443", ".32"},
            {"4443.", ".3322"},
            {".4543..", "4443332", "..2221."},
    };
    /** Magnetite: small blocky black crystals lit on their top-left faces. */
    static final String[][] MAGNETITE_CRYSTALS = {
            {"s3", "31"},
            {"s43", "431", "311"},
            {"5443", "4s32", "4321", "3211"},
    };
    /** Native gold: thin bright wires and flakes, a few pixels each. */
    static final String[][] GOLD_WIRES = {
            {"54", ".3"},
            {"..s", ".43", "43."},
            {"...45", "..s3.", ".434.", "43.3."},
    };

    /** Soft lumpy soil in the fire clay ramp, with a couple of dark rootlets. */
    static BufferedImage fireClayBlock() {
        double[][] a = noise(2020, 4), b = noise(2021, 2);
        double[][] n = new double[16][16];
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) n[y][x] = a[y][x] * 0.6 + b[y][x] * 0.4;
        double lo = quantile(n, 0.22), hi = quantile(n, 0.8);
        BufferedImage im = img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) px(im, x, y, FIRE_CLAY.get(n[y][x] < lo ? 2 : n[y][x] < hi ? 3 : 4));
        Random r = new Random(2020);
        // Rounded clods, lit cap and shaded underside, spaced so they stay separate lumps.
        String[][] clods = {{".44.", "4433", ".322"}, {"45.", "432", ".2."}, {".445.", "44333", ".3322"}};
        List<int[]> placed = new ArrayList<>();
        for (int attempts = 0; placed.size() < 7 && attempts < 500; attempts++) {
            int x = r.nextInt(16), y = r.nextInt(16);
            boolean clash = false;
            for (int[] q : placed) {
                int dx = Math.abs(Math.floorMod(q[0] - x + 8, 16) - 8), dy = Math.abs(Math.floorMod(q[1] - y + 8, 16) - 8);
                if (dx < 6 && dy < 5) clash = true;
            }
            if (clash) continue;
            placed.add(new int[] {x, y});
            String[] c = clods[placed.size() % clods.length];
            for (int j = 0; j < c.length; j++)
                for (int i = 0; i < c[j].length(); i++)
                    if (c[j].charAt(i) != '.') pxWrap(im, x + i, y + j, FIRE_CLAY.get(c[j].charAt(i) - '0'));
        }
        // Rootlets: two thin curling lines of step 1 fading to step 2.
        int[][][] roots = {{{0, 0}, {1, 0}, {2, 1}, {3, 1}, {4, 1}, {5, 2}}, {{0, 0}, {0, 1}, {1, 2}, {1, 3}, {2, 3}}};
        for (int k = 0; k < roots.length; k++) {
            int x = r.nextInt(16), y = r.nextInt(16);
            for (int i = 0; i < roots[k].length; i++)
                pxWrap(im, x + roots[k][i][0], y + roots[k][i][1], FIRE_CLAY.get(i >= roots[k].length - 2 ? 2 : 1));
        }
        return im;
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
    static BufferedImage fireBricks() {
        BufferedImage im = img();
        Random r = new Random(3131);
        for (int c = 0; c < 4; c++) {
            int[] j = FIRE_BRICK_JOINTS[c];
            for (int b = 0; b < j.length; b++) {
                int start = j[b] + 1, w = Math.floorMod(j[(b + 1) % j.length] - start, 16);
                int tone = r.nextInt(3) == 0 ? -1 : 0;
                int fleck = r.nextInt(4) == 0 ? 1 + r.nextInt(Math.max(1, w - 2)) : -1;
                for (int row = 0; row < 3; row++)
                    for (int i = 0; i < w; i++) {
                        int step = row == 0 ? 5 : 4;
                        if (i == w - 1 || (row == 2 && i == w - 2)) step = row == 0 ? 4 : 3;
                        step += tone;
                        if (row == 1 && i == fleck) step = 3;
                        pxWrap(im, start + i, c * 4 + row, FIRE_BRICK.get(Math.max(2, step)));
                    }
                for (int y = 0; y < 4; y++) pxWrap(im, j[b], c * 4 + y, FIRE_BRICK.get(1));
            }
            for (int x = 0; x < 16; x++) px(im, x, c * 4 + 3, FIRE_BRICK.get(1));
        }
        return im;
    }

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
        double[][] n = fractal(9191);
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
        double[][] n = fractal(5454), e = noise(5455, 4);
        BufferedImage im = img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int step = n[y][x] > 0.58 ? 4 : n[y][x] < 0.3 ? 2 : 3;
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
        double[][] n = fractal(5656), e = noise(5657, 4);
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
                    if (n[y][x] > 0.64 && d < 0.9) step = 5;
                    else if (n[y][x] < 0.3) step--;
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

    static void tier3() throws IOException {
        // Blocks (spec 20.2).
        save("block/fire_clay", fireClayBlock());
        save("block/fire_bricks", fireBricks());
        save("block/lignite_seam", ligniteSeam());
        Mineral bogLimonite = new Mineral("bog_limonite", LIMONITE, false);
        for (String grade : List.of("poor", "normal", "rich"))
            save("block/bog_iron_" + grade, composite(mud(), oreOverlay(bogLimonite, grade)));
        save("block/placer_gravel", placerGravel());
        save("block/placer_sand", placerSand());
        for (String grade : List.of("poor", "normal", "rich")) {
            save("block/ore/hematite_" + grade, pieceOverlay(T3_MINERALS.get(0), grade, HEMATITE_LENSES, 5));
            save("block/ore/magnetite_" + grade, pieceOverlay(T3_MINERALS.get(1), grade, MAGNETITE_CRYSTALS, 5));
            save("block/ore/native_gold_" + grade, pieceOverlay(T3_MINERALS.get(2), grade, GOLD_WIRES, 5));
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
        save("item/wrought_iron_double_ingot", map(WROUGHT_IRON, DOUBLE_INGOT));
        for (var head : heads.entrySet()) save("item/wrought_iron_" + head.getKey(), map(WROUGHT_IRON, head.getValue()));
        for (String kind : List.of("knife", "hammer", "saw", "prospectors_pick"))
            save("item/wrought_iron_" + kind, tool(WROUGHT_IRON, WOOD, null, kind));
        save("item/gold_plate", map(GOLD, PLATE));
        for (String head : List.of("pickaxe_head", "axe_head", "shovel_head", "hoe_head", "sword_blade"))
            save("item/gold_" + head, map(GOLD, heads.get(head)));

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
    static final Ramp STEEL = ramp(0xdfe4ea, 0x22262e, 0x3a404a, 0x5c636e, 0x848b96, 0xadb4be);
    static final Ramp PIG_IRON = ramp(0xa8a098, 0x1c1a1a, 0x2e2a28, 0x45403c, 0x5e5852, 0x7a736a);
    static final Ramp BRASS = ramp(0xf8ecb8, 0x4a3814, 0x7a5e1e, 0xa8862e, 0xd0ae48, 0xecd27a);
    static final Ramp ZINC = ramp(0xe4ecee, 0x2e3238, 0x4a5058, 0x6e767e, 0x97a0a6, 0xbfc7cb);
    static final Ramp LEAD = ramp(0xa8b0be, 0x1e2028, 0x2e3240, 0x444a5c, 0x5e6678, 0x7c8494);
    static final Ramp SOLDER = ramp(0xe2e6e2, 0x34383a, 0x50565a, 0x737a7c, 0x9aa09e, 0xbec4c0);
    // Ore ramps (worldgen spec 14.1): sulfur stays a muted green-yellow, well off the heat band.
    static final Ramp BITUMINOUS_COAL = ramp(0, 0x121318, 0x1e2027, 0x2c2e37, 0x3e414c, 0x565a66);
    static final Ramp SPHALERITE = ramp(0, 0x1c1210, 0x2e1c14, 0x4a2e1a, 0x6e4622, 0x946632);
    static final Ramp GALENA = ramp(0xd0d6e0, 0x1e2028, 0x343844, 0x525866, 0x767d8c, 0x9ea6b4);
    static final Ramp SULFUR = ramp(0, 0x3c3820, 0x5c5630, 0x827a40, 0xa69c54, 0xc6bc72);

    static final List<Mineral> T4_MINERALS = List.of(
            new Mineral("sphalerite", SPHALERITE, false),
            new Mineral("galena", GALENA, true),
            new Mineral("bituminous_coal", BITUMINOUS_COAL, false),
            new Mineral("sulfur", SULFUR, false));

    /** Sphalerite, "black jack": blocky dark grains with a resinous amber core. */
    static final String[][] SPHALERITE_GRAINS = {
            {"24", "12"},
            {"221", "254", "121"},
            {".221", "2254", "2441", "1211"},
    };
    /** Galena: crisp cubes, lit top-left faces, shadowed bottom-right. */
    static final String[][] GALENA_CUBES = {
            {"s4", "41"},
            {"554", "531", "411"},
            {"5554", "5331", "5331", "4111"},
    };
    /** Bituminous coal: thin horizontal seams, a lit top edge and one glassy glint each. */
    static final String[][] COAL_STREAKS = {
            {"54", "11"},
            {"4544", "1121"},
            {"445444", "111211"},
    };
    /** Native sulfur: crusty, rounded-angular crystal clusters, a little larger than other grains. */
    static final String[][] SULFUR_CRUSTS = {
            {"45", "32"},
            {".54.", "4543", "3322"},
            {".545.", "45543", "34433", ".222."},
    };

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
    static final Ramp COKE_OVEN_BRICK = ramp(0, 0x3e2a24, 0x583a30, 0x74503e, 0x906850, 0xa88466);
    static final Ramp TREATED_WOOD = ramp(0, 0x24180e, 0x3a2616, 0x52361e, 0x6a4a2a, 0x82603a);
    static final Ramp CREOSOTE = ramp(0, 0x140c08, 0x24160c, 0x382212, 0x4c301a, 0x624024);

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

    static void tier4() throws IOException {
        // Ores (worldgen spec 14.2 and 14.3).
        for (String grade : List.of("poor", "normal", "rich")) {
            save("block/ore/sphalerite_" + grade, pieceOverlay(T4_MINERALS.get(0), grade, SPHALERITE_GRAINS, 4));
            save("block/ore/galena_" + grade, pieceOverlay(T4_MINERALS.get(1), grade, GALENA_CUBES, 5));
            save("block/ore/bituminous_coal_" + grade, pieceOverlay(T4_MINERALS.get(2), grade, COAL_STREAKS, 5));
            save("block/ore/sulfur_" + grade, pieceOverlay(T4_MINERALS.get(3), grade, SULFUR_CRUSTS, 5));
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
        save("item/pig_iron_ingot", map(PIG_IRON, sandCast(INGOT)));
        for (var e : List.of(java.util.Map.entry("steel", STEEL), java.util.Map.entry("zinc", ZINC), java.util.Map.entry("lead", LEAD),
                java.util.Map.entry("brass", BRASS))) {
            save("item/" + e.getKey() + "_ingot", map(e.getValue(), INGOT));
            save("item/" + e.getKey() + "_nugget", map(e.getValue(), NUGGET));
        }
        save("item/solder_ingot", map(SOLDER, INGOT));
        for (var e : List.of(java.util.Map.entry("steel", STEEL), java.util.Map.entry("brass", BRASS))) {
            save("item/" + e.getKey() + "_plate", map(e.getValue(), PLATE));
            save("item/" + e.getKey() + "_rod", map(e.getValue(), ROD));
            save("item/" + e.getKey() + "_gear", gear(e.getValue()));
        }
        for (Metal metal : METALS)
            if (metal.name().contains("bronze")) save("item/" + metal.name() + "_gear", gear(metal.ramp()));
        save("item/steel_double_ingot", map(STEEL, DOUBLE_INGOT));

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
        for (var head : heads.entrySet()) save("item/steel_" + head.getKey(), map(STEEL, head.getValue()));
        for (String kind : List.of("pickaxe", "axe", "shovel", "hoe", "knife", "hammer", "saw", "sword"))
            save("item/steel_" + kind, tool(STEEL, WOOD, BRASS, kind));

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
        save("item/creosote_bucket", map(WROUGHT_IRON, CREOSOTE, CREOSOTE_BUCKET));
        save("item/treated_stick", art(TREATED_WOOD, TREATED_STICK));
        saveRaw("gui/coke_oven", cokeOvenGui());
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
        save("item/flint_shard", map(FLINT, FLINT_SHARD));
        save("block/loose_stick", art(WOOD, LOOSE_STICKS));
        save("block/loose_flint", art(FLINT, LOOSE_FLINTS));

        // Stone age (tier 0-2 spec 3).
        save("item/straw", art(STRAW, STRAW_SHEAF));
        save("item/twine", art(FIBRE, TWINE_HANK));
        save("item/field_journal", art(FIELD_JOURNAL, STRAW, FIBRE, PAPER));
        save("item/fibre_cloth", fibreCloth());
        save("item/stone_axe_head", map(FLINT, KNAPPED_AXE_HEAD));
        save("item/stone_knife_blade", map(FLINT, KNAPPED_KNIFE_BLADE));
        save("item/stone_shovel_head", map(FLINT, KNAPPED_SHOVEL_HEAD));
        save("item/stone_hoe_head", map(FLINT, KNAPPED_HOE_HEAD));
        save("item/stone_hammer_head", map(FLINT, KNAPPED_HAMMER_HEAD));
        save("item/stone_spear_head", map(FLINT, KNAPPED_SPEAR_HEAD));
        save("item/stone_pickaxe_head", map(FLINT, KNAPPED_PICKAXE_HEAD));
        for (String kind : List.of("axe", "knife", "shovel", "hoe", "hammer", "pickaxe"))
            save("item/stone_" + kind, tool(FLINT, WOOD, FIBRE, kind));
        save("gui/knapping/flint", flintSurface());
        saveRaw("gui/knapping", knappingGui());

        // Fire (spec 3.5).
        save("block/fire_pit_stones", fieldStones());
        save("block/fire_pit_wood", stickWood());
        save("block/fire_pit_ash", ashBed());
        save("block/fire_pit_embers", emberBed());
        save("item/firestarter", firestarter());
        saveRaw("gui/fire_pit", firePitGui());

        // Clay (spec 4.1 to 4.3).
        save("gui/knapping/clay", claySurface());
        for (var piece : List.of(java.util.Map.entry("small_vessel", SMALL_VESSEL_ITEM), java.util.Map.entry("large_vessel", LARGE_VESSEL_ITEM),
                java.util.Map.entry("crucible", CRUCIBLE_ITEM), java.util.Map.entry("ingot_mold", INGOT_MOLD_ITEM))) {
            save("item/unfired_" + piece.getKey(), art(CLAY, piece.getValue()));
            save("item/" + piece.getKey(), art(CERAMIC, piece.getValue()));
        }
        save("item/unfired_brick", art(CLAY, BRICK_ITEM));
        save("item/ingot_mold_filled", filledIngotMold());
        for (var cavity : MOLD_CAVITIES.entrySet()) {
            save("item/unfired_" + cavity.getKey() + "_mold", mold(CLAY, cavity.getValue()));
            save("item/" + cavity.getKey() + "_mold", mold(CERAMIC, cavity.getValue()));
            save("item/" + cavity.getKey() + "_mold_filled", filledMold(cavity.getValue()));
        }
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
        save("item/ash", art(ASH, ASH_HEAP));

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
        for (Metal metal : METALS) {
            String n = metal.name();
            if (!metal.vanillaIngot()) {
                save("item/" + n + "_ingot", map(metal.ramp(), INGOT));
                save("item/" + n + "_nugget", map(metal.ramp(), NUGGET));
            }
            if (n.equals("tin") || n.equals("bismuth")) continue;
            save("item/" + n + "_plate", map(metal.ramp(), PLATE));
            for (var head : heads.entrySet()) save("item/" + n + "_" + head.getKey(), map(metal.ramp(), head.getValue()));
            for (String kind : List.of("pickaxe", "axe", "shovel", "hoe", "knife", "hammer", "saw", "sword")) {
                // Copper's pickaxe, axe, shovel, hoe and sword are the vanilla items and keep vanilla art.
                if (metal.vanillaIngot() && !List.of("knife", "hammer", "saw").contains(kind)) continue;
                save("item/" + n + "_" + kind, tool(metal.ramp(), WOOD, null, kind));
            }
        }
        save("item/slag_metal_ingot", map(SLAG, INGOT));
        for (Metal metal : METALS) {
            String n = metal.name();
            if (!n.contains("bronze")) continue;
            saveRaw("entity/equipment/humanoid/" + n, armourLayer(metal.ramp()));
            saveRaw("entity/equipment/humanoid_leggings/" + n, leggingsLayer(metal.ramp()));
            save("item/" + n + "_helmet", map(metal.ramp(), FIBRE, HELMET_ITEM));
            save("item/" + n + "_chestplate", map(metal.ramp(), FIBRE, CHESTPLATE_ITEM));
            save("item/" + n + "_leggings", map(metal.ramp(), FIBRE, LEGGINGS_ITEM));
            save("item/" + n + "_boots", map(metal.ramp(), FIBRE, BOOTS_ITEM));
            save("item/" + n + "_prospectors_pick_head", map(metal.ramp(), PROSPECTOR_HEAD));
            save("item/" + n + "_prospectors_pick", tool(metal.ramp(), WOOD, null, "prospectors_pick"));
        }
        saveRaw("gui/crucible", crucibleGui());

        saveRaw("gui/anvil", anvilGui());
        for (Rock rock : ROCKS) {
            if (rock.category().equals("intrusive") || rock.category().equals("extrusive"))
                save("block/" + rock.name() + "_anvil_top", stoneAnvilTop(rock));
        }
        save("block/bronze_anvil", bronzeAnvilBody());
        save("block/bronze_anvil_top", bronzeAnvilTop());
        save("item/tongs_jaw", map(BRONZE, TONGS_JAW_ITEM));
        save("item/tongs", tongs());

        // Quern (spec 10.1).
        save("block/quern_side", quernSide());
        save("block/quern_top", quernFace(5252, false));
        save("block/quern_runner", quernFace(5353, true));
        save("item/quernstone", art(GRANITE, QUERNSTONE_ITEM));

        tier3();
        kinetics();
        machines();
        washing();
        windAndTanning();
        tier4();

        // Glow layers for hot metal: written last, from the finished item textures.
        glowLayers();
        if (args.length > 0 && args[0].equals("--preview-only")) { preview(); return; }
        preview();
        System.out.println("Wrote " + PREVIEW.size() + " textures");
    }
}
