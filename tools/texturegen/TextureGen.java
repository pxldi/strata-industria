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
        panel(im, 176, 234);
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
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++) slot(im, 8 + col * 18, 152 + row * 18);
        for (int col = 0; col < 9; col++) slot(im, 8 + col * 18, 210);

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
        return im;
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
            java.util.regex.Pattern.compile("(?!stone_|unfired_).*(_ingot|_nugget|(?<!chest)_plate|_head|_blade|_rod)|tongs_jaw|raw_bloom");

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

    // ---------------------------------------------------------------- output

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

        // Glow layers for hot metal: written last, from the finished item textures.
        glowLayers();
        if (args.length > 0 && args[0].equals("--preview-only")) { preview(); return; }
        preview();
        System.out.println("Wrote " + PREVIEW.size() + " textures");
    }
}
