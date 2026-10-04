import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import javax.imageio.ImageIO;

/**
 * Textures for the collectibles of the world structures: pottery sherds and decorated pot faces for the six
 * ruin types, the miner and smith armour trim templates and trims, the pick and hammer banner pattern, the
 * mineral specimen item with its tinted overlay and the specimen shelf. Follows style guide v2 and the vanilla
 * conventions for each kind: sherds and pot faces share vanilla's brick-clay ramp, templates keep the slate
 * silhouette with a cyan motif, trims use only the five grey values the trim palette recolours, banner and
 * shield masks use vanilla's alpha convention (255 body, ~191 bevel, ~60 recess).
 *
 * <p>Run from the repository root: {@code java tools/texturegen/CollectibleTextures.java}. Output is
 * deterministic and goes to {@code src/main/resources/assets/strataindustria/textures}; a 6x preview is
 * written to {@code build/texturegen/collectibles.png}.
 */
public final class CollectibleTextures {
    static final Path OUT = Path.of("src/main/resources/assets/strataindustria/textures");

    /** Brick clay of vanilla sherds and pot faces, dark to light. */
    static final int[] CLAY = {0x47251d, 0x522d25, 0x5e372f, 0x693b31, 0x754236, 0x83493b, 0x905041, 0x9f5949};
    /** Lit rim of a pot face (vanilla uses a tan highlight along the top). */
    static final int[] CLAY_RIM = {0x9f5949, 0x945344, 0x9f6547, 0xab714b, 0xb98053};
    static final int[] WOOD = {0x3a2618, 0x5a3c24, 0x7a5634, 0x9c7448, 0xbc9462};
    static final int[] STONE = {0x3a3a3c, 0x55555a, 0x6f6f72, 0x8a8a8c, 0xa6a6a8};
    static final String[] PLACES = {"charcoal_burners", "prospector", "mining_camp", "collapsed_adit", "ruined_bloomery", "placer_workings"};

    // ---------------------------------------------------------------- motifs (12x12, digits = CLAY index)

    static final String[][] MOTIFS = {
            // charcoal burners: a turf-covered mound with a draught hole, smoke curling off the top
            {
                    ".....111....",
                    "....1...1...",
                    "....1.1.1...",
                    ".....1.1....",
                    "......1.....",
                    "......1.....",
                    "....1111....",
                    "..11333311..",
                    ".1334334331.",
                    ".1333333331.",
                    "133300003331",
                    "111111111111"},
            // prospector: a cairn of three stones, each lit top-left
            {
                    "............",
                    "...111111...",
                    "...143331...",
                    "...133231...",
                    "..11111111..",
                    "..14333331..",
                    "..13333321..",
                    "..13333221..",
                    ".1111111111.",
                    ".1433333331.",
                    ".1333333221.",
                    ".1111111111."},
            // mining camp: a pick standing upright, a lamp hanging on a chain from one tip of its head
            {
                    "....1111....",
                    "..11....11..",
                    ".11......11.",
                    "11........11",
                    "1....21...2.",
                    ".....21...2.",
                    ".....21...2.",
                    ".....21..11.",
                    ".....21.1771",
                    ".....21.1761",
                    ".....21..11.",
                    ".....21....."},
            // collapsed adit: two timbers and a cap beam that has split, rubble in the opening
            {
                    "............",
                    "1111111.11..",
                    "1433333.31..",
                    "111111111111",
                    ".14130000131",
                    ".14103000131",
                    ".14100300131",
                    ".14100030131",
                    ".14100000131",
                    ".14100340131",
                    ".14103443131",
                    ".14134343131"},
            bloomery(),
            pan()};

    /** A tall brick stack: broken crown, mortar courses in half bond, a flame in the stoke mouth. */
    static String[] bloomery() {
        char[][] g = new char[12][12];
        for (char[] row : g) java.util.Arrays.fill(row, '.');
        int[] left = {4, 4, 4, 4, 3, 3, 3, 2, 2, 2, 1, 0}, right = {7, 7, 7, 7, 8, 8, 8, 9, 9, 9, 10, 11};
        for (int y = 1; y < 12; y++) {
            for (int x = left[y]; x <= right[y]; x++) {
                boolean edge = x == left[y] || x == right[y];
                boolean mortar = y % 3 == 0;
                int off = (y / 3) % 2 == 0 ? 0 : 2;
                boolean joint = !mortar && (x + off) % 4 == 0;
                g[y][x] = edge || mortar || y == 11 ? '1' : joint ? '1' : x == left[y] + 1 || y % 3 == 1 ? '4' : '3';
            }
        }
        // Broken crown: ragged top edge.
        g[0][4] = '1'; g[0][7] = '1'; g[1][5] = '.'; g[1][6] = '.'; g[1][4] = '1'; g[1][7] = '1';
        // The stoke mouth: an arch with a flame.
        String[] mouth = {".00.", "0760", "0770", "0670"};
        for (int y = 0; y < 4; y++) for (int x = 0; x < 4; x++) if (mouth[y].charAt(x) != '.') g[8 + y][4 + x] = mouth[y].charAt(x);
        String[] out = new String[12];
        for (int y = 0; y < 12; y++) out[y] = new String(g[y]);
        return out;
    }

    /** A gold pan seen at a slant: ring rim, shadowed back wall, a few bright grains, ripples beneath. */
    static String[] pan() {
        char[][] g = new char[12][12];
        for (char[] row : g) java.util.Arrays.fill(row, '.');
        for (int y = 0; y < 12; y++)
            for (int x = 0; x < 12; x++) {
                double dx = (x - 5.5) / 5.7, dy = (y - 5.0) / 3.2;
                double d = dx * dx + dy * dy;
                double di = ((x - 5.5) / 4.3) * ((x - 5.5) / 4.3) + ((y - 5.0) / 2.0) * ((y - 5.0) / 2.0);
                if (d > 1) continue;
                g[y][x] = di > 1 ? '1' : y < 5 ? '2' : '4';
            }
        g[4][3] = '3'; g[4][4] = '3'; g[4][8] = '3';
        g[5][4] = '7'; g[6][6] = '7'; g[5][7] = '6'; g[6][3] = '6';
        String[] out = new String[12];
        for (int y = 0; y < 12; y++) out[y] = new String(g[y]);
        out[10] = "..3.33.3.3..";
        return out;
    }

    /** Vanilla sherd silhouette (rows 0..15, x ranges inclusive). */
    static final int[][] SHERD_ROWS = {
            {}, {7, 13}, {6, 13}, {2, 13}, {2, 13}, {2, 13}, {2, 13}, {2, 13}, {2, 13}, {2, 14}, {4, 14}, {3, 14},
            {2, 14}, {1, 12}, {1, 9}, {}};

    public static void main(String[] args) throws IOException {
        BufferedImage[] sherds = new BufferedImage[6], pots = new BufferedImage[6];
        for (int i = 0; i < 6; i++) {
            sherds[i] = save("item/" + PLACES[i] + "_pottery_sherd", sherd(i));
            pots[i] = save("entity/decorated_pot/" + PLACES[i] + "_pottery_pattern", potFace(i));
        }
        BufferedImage minerT = save("item/miner_armor_trim_smithing_template", template(MINER_MOTIF));
        BufferedImage smithT = save("item/smith_armor_trim_smithing_template", template(SMITH_MOTIF));
        BufferedImage bannerItem = save("item/pick_and_hammer_banner_pattern", bannerPattern());
        int[][][][] trims = {minerHumanoid(), minerLeggings(), smithHumanoid(), smithLeggings()};
        String[] trimNames = {"humanoid/miner", "humanoid_leggings/miner", "humanoid/smith", "humanoid_leggings/smith"};
        String mcmeta = "{\n  \"palette\" : {\n    \"base_palette\" : \"trim_base\"\n  }\n}\n";
        for (int i = 0; i < 4; i++) {
            saveLA("trims/entity/" + trimNames[i], trims[i][0], trims[i][1]);
            File f = OUT.resolve("trims/entity/" + trimNames[i] + ".png.mcmeta").toFile();
            Files.writeString(f.toPath(), mcmeta);
        }
        int[][][] banner = bannerFlag(), shield = shieldFace();
        saveLA("entity/banner/pick_and_hammer", banner[0], banner[1]);
        saveLA("entity/shield/pick_and_hammer", shield[0], shield[1]);
        BufferedImage specimen = save("item/mineral_specimen", specimenBase());
        BufferedImage overlay = save("item/mineral_specimen_overlay", specimenOverlay());
        BufferedImage shelfFront = save("block/specimen_shelf_front", shelfFront());
        BufferedImage shelfSide = save("block/specimen_shelf_side", shelfSide());
        preview(sherds, pots, new BufferedImage[]{minerT, smithT, bannerItem, specimen, overlay, shelfFront, shelfSide},
                trims, banner, shield);
    }

    // ---------------------------------------------------------------- helpers

    static BufferedImage img(int w, int h) {
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    }

    static void px(BufferedImage im, int x, int y, int rgb) {
        if (x < 0 || y < 0 || x >= im.getWidth() || y >= im.getHeight()) return;
        im.setRGB(x, y, 0xFF000000 | rgb);
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

    /** Item outline: the bordering pixel darkened to 32%, hue kept, on all four sides (style guide 7). */
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

    /** Draws an art block: a digit is a ramp index, anything else leaves the pixel alone. */
    static void art(BufferedImage im, String[] rows, int x0, int y0, int[] ramp) {
        for (int y = 0; y < rows.length; y++)
            for (int x = 0; x < rows[y].length(); x++) {
                char c = rows[y].charAt(x);
                if (c >= '0' && c <= '9') px(im, x0 + x, y0 + y, ramp[c - '0']);
            }
    }

    static BufferedImage save(String path, BufferedImage im) throws IOException {
        File f = OUT.resolve(path + ".png").toFile();
        f.getParentFile().mkdirs();
        ImageIO.write(im, "png", f);
        return im;
    }

    /** Writes an 8-bit grey + alpha PNG from per-pixel grey and alpha arrays [y][x]. */
    static void saveLA(String path, int[][] grey, int[][] alpha) throws IOException {
        int h = grey.length, w = grey[0].length;
        ComponentColorModel cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), true, false,
                java.awt.Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);
        WritableRaster r = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, w, h, 2, null);
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                r.setSample(x, y, 0, grey[y][x]);
                r.setSample(x, y, 1, alpha[y][x]);
            }
        BufferedImage im = new BufferedImage(cm, r, false, null);
        File f = OUT.resolve(path + ".png").toFile();
        f.getParentFile().mkdirs();
        ImageIO.write(im, "png", f);
    }

    // ---------------------------------------------------------------- sherds and pot faces

    /**
     * A vanilla-shaped shard of brick clay: grain between ramp steps 3 and 6, a bright bevel under the top edge,
     * a darker rim on the right and foot, and the broken-off motif carved in dark clay.
     */
    static BufferedImage sherd(int place) {
        BufferedImage im = img(16, 16);
        Random r = new Random(7100 + place);
        boolean[][] in = new boolean[16][16];
        for (int y = 0; y < 16; y++) {
            if (SHERD_ROWS[y].length == 0) continue;
            for (int x = SHERD_ROWS[y][0]; x <= SHERD_ROWS[y][1]; x++) in[y][x] = true;
        }
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                if (!in[y][x]) continue;
                int[] grain = {4, 5, 5, 5, 5, 5, 6, 6};
                int idx = grain[r.nextInt(grain.length)];
                boolean top = y == 0 || !in[y - 1][x], left = x == 0 || !in[y][x - 1];
                boolean right = x == 15 || !in[y][x + 1], bottom = y == 15 || !in[y + 1][x];
                boolean belowTop = y > 0 && in[y - 1][x] && (y == 1 || !in[y - 2][x]);
                if (belowTop) idx = r.nextInt(3) == 0 ? 6 : 7;
                if (top) idx = 4;
                if (left && !top) idx = r.nextInt(3) == 0 ? 4 : 6;
                if (right) idx = r.nextInt(2) == 0 ? 2 : 3;
                if (bottom) idx = r.nextInt(3) == 0 ? 1 : 0;
                px(im, x, y, CLAY[idx]);
            }
        // The motif, clipped to the shard; strokes on the lit side catch a lighter edge.
        BufferedImage m = img(16, 16);
        art(m, MOTIFS[place], 2, 3, CLAY);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) if (opaque(m, x, y) && in[y][x]) px(im, x, y, rgb(m, x, y));
        return im;
    }

    /** The same motif drawn full on a pot face: tan lit top rim, clay body, darker foot (vanilla layout). */
    static BufferedImage potFace(int place) {
        BufferedImage im = img(16, 16);
        Random r = new Random(7200 + place);
        for (int y = 0; y < 16; y++)
            for (int x = 1; x <= 14; x++) {
                int c;
                if (y == 0) c = CLAY_RIM[r.nextInt(2)];
                else if (y <= 2) c = CLAY_RIM[1 + r.nextInt(4)];
                else if (y >= 13) c = CLAY[2 + r.nextInt(3)];
                else {
                    int[] grain = {4, 5, 5, 5, 5, 5, 6, 6};
                    c = CLAY[grain[r.nextInt(grain.length)]];
                    if (y >= 11) c = CLAY[3 + r.nextInt(3)];
                }
                if (x == 1 && y > 2) c = CLAY[3 + r.nextInt(3)];
                if (x == 14 && y > 2) c = CLAY[1 + r.nextInt(3)];
                px(im, x, y, c);
            }
        art(im, MOTIFS[place], 2, 2, new int[]{CLAY[0], CLAY[0], CLAY[1], CLAY[2], CLAY[3], CLAY[5], CLAY[6], CLAY[7]});
        return im;
    }

    // ---------------------------------------------------------------- smithing templates

    /** The slate silhouette of vanilla's armour trim templates, class per pixel. 'c' is where the motif sits. */
    static final String[] TEMPLATE = {
            "......001.......",
            ".....002001.....",
            ".....02022013...",
            "....12200000133.",
            "....100000011445",
            "...401cc00000445",
            "...411cc10004435",
            "..41cc01cc00335.",
            "..40cc11cc04335.",
            ".41400cc101335..",
            ".44111c1114335..",
            ".411111111335...",
            ".541111414335...",
            "..5541144335....",
            "....55414335....",
            "......55555....."};
    static final int[] CYAN = {0x60e8e8, 0x4bc9c9, 0x209ab8};

    /** Motifs 6x6: 0 = bright cyan, 1 = body, 2 = deep. */
    static final String[] MINER_MOTIF = {
            "..11..",
            ".1001.",
            ".1001.",
            "..11..",
            "111111",
            "202202"};
    static final String[] SMITH_MOTIF = {
            "...011",
            "..0111",
            ".012..",
            "02....",
            "111111",
            "1.22.1"};

    static BufferedImage template(String[] motif) {
        BufferedImage im = img(16, 16);
        Random r = new Random(7300 + motif[0].hashCode());
        int[] base = {0x76746a, 0x565c55, 0x8e8c7e, 0x353239, 0x484449, 0x221e26};
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                char c = TEMPLATE[y].charAt(x);
                if (c == '.') continue;
                int k = c == 'c' ? 1 : c - '0';
                int col = base[k];
                if (k == 0 && r.nextInt(6) == 0) col = 0x6a6c62;
                if (k == 1 && r.nextInt(6) == 0) col = 0x4c524b;
                px(im, x, y, col);
            }
        art(im, motif, 4, 5, CYAN);
        return im;
    }

    // ---------------------------------------------------------------- armour trims

    static final int[] GREY = {0, 96, 128, 160, 192, 224};

    /** 64x32 trim sheet builder: grey values only from {96,128,160,192,224}. */
    static final class Sheet {
        final int[][] g = new int[32][64], a = new int[32][64];

        void set(int x, int y, int step) {
            if (x < 0 || y < 0 || x >= 64 || y >= 32) return;
            g[y][x] = GREY[step];
            a[y][x] = 255;
        }

        /** A beveled plate: lit top-left (5/4), body 3, shaded bottom-right (2). */
        void plate(int x0, int y0, int w, int h) {
            for (int y = 0; y < h; y++)
                for (int x = 0; x < w; x++) {
                    int s = 3;
                    if (y == 0 || x == 0) s = y == 0 && x == 0 ? 5 : 4;
                    if (y == h - 1 || x == w - 1) s = 2;
                    if (y == 0 && x == w - 1 || x == 0 && y == h - 1) s = 3;
                    set(x0 + x, y0 + y, s);
                }
        }

        /** A strap band over columns [x0, x1] and rows y0 (lit) and y0+1 (shade), with rivets every few pixels. */
        void strap(int x0, int x1, int y0) {
            for (int x = x0; x <= x1; x++) {
                set(x, y0, 4);
                set(x, y0 + 1, 2);
            }
        }

        void rivet(int x, int y) {
            set(x, y, 5);
        }

        void line(int x0, int x1, int y, int step) {
            for (int x = x0; x <= x1; x++) set(x, y, step);
        }
    }

    static int[][][] sheet(Sheet s) {
        return new int[][][]{s.g, s.a};
    }

    /** Miner: a lamp-shaped plate on the helmet brow, riveted straps on chest, arms and boots. */
    static int[][][] minerHumanoid() {
        Sheet s = new Sheet();
        // Helmet brow, front face x8..15, y8..15: lamp plate with a lit lens, flanked by strap stubs.
        s.strap(8, 9, 11);
        s.strap(14, 15, 11);
        s.set(10, 9, 4); s.set(11, 9, 5); s.set(12, 9, 5); s.set(13, 9, 4);
        s.set(10, 10, 4); s.set(11, 10, 5); s.set(12, 10, 4); s.set(13, 10, 3);
        s.set(10, 11, 3); s.set(11, 11, 4); s.set(12, 11, 3); s.set(13, 11, 2);
        s.set(11, 12, 3); s.set(12, 12, 2);
        // The strap runs round both sides and the back of the head.
        s.strap(0, 7, 11); s.strap(16, 23, 11); s.strap(24, 31, 11);
        s.rivet(3, 11); s.rivet(19, 11); s.rivet(27, 11); s.rivet(28, 11);
        // Chest straps over the whole body ring (right 16..19, front 20..27, left 28..31, back 32..39).
        s.strap(16, 39, 24);
        s.rivet(21, 24); s.rivet(26, 24); s.rivet(17, 24); s.rivet(30, 24); s.rivet(34, 24); s.rivet(37, 24);
        s.plate(23, 23, 2, 4);
        // Arm straps (arm ring x40..55).
        s.strap(40, 55, 24);
        s.rivet(42, 24); s.rivet(46, 24); s.rivet(50, 24); s.rivet(54, 24);
        // Boots on the lower leg rows.
        s.strap(0, 15, 29);
        s.rivet(1, 29); s.rivet(5, 29); s.rivet(6, 29); s.rivet(10, 29); s.rivet(13, 29);
        s.line(4, 7, 28, 3);
        return sheet(s);
    }

    static int[][][] minerLeggings() {
        Sheet s = new Sheet();
        // Waist belt round the body ring with a square buckle plate in front.
        s.strap(16, 39, 28);
        s.plate(22, 27, 4, 4);
        s.rivet(23, 28);
        s.rivet(17, 28); s.rivet(30, 28); s.rivet(34, 28); s.rivet(38, 28);
        // Thigh and knee straps on the leg ring x0..15.
        s.strap(0, 15, 23);
        s.rivet(1, 23); s.rivet(5, 23); s.rivet(6, 23); s.rivet(10, 23); s.rivet(14, 23);
        s.strap(0, 15, 28);
        s.rivet(2, 28); s.rivet(5, 28); s.rivet(6, 28); s.rivet(9, 28); s.rivet(13, 28);
        return sheet(s);
    }

    /** Smith: hammered square plates, and an apron line across the chest and the legs. */
    static int[][][] smithHumanoid() {
        Sheet s = new Sheet();
        // Helmet: two square plates side by side on the brow, a plate over each ear.
        s.plate(9, 9, 3, 3);
        s.plate(12, 9, 3, 3);
        s.set(10, 10, 2); s.set(13, 10, 2);
        s.plate(2, 9, 4, 3); s.plate(18, 9, 4, 3);
        s.line(24, 31, 10, 3);
        // Chest: the apron line across, and a hammered square plate hanging below it.
        s.line(16, 39, 23, 4);
        s.line(16, 39, 24, 2);
        s.plate(21, 25, 6, 5);
        s.set(23, 27, 2); s.set(24, 27, 2);
        s.rivet(21, 25); s.rivet(26, 25);
        s.plate(33, 25, 4, 4);
        // Shoulders: a square plate on each arm front and side.
        s.plate(44, 20, 4, 4);
        s.plate(40, 20, 4, 3);
        s.plate(52, 20, 4, 3);
        s.set(45, 21, 2);
        // Boots: a plate over the instep.
        s.plate(4, 28, 4, 3);
        s.line(0, 3, 29, 3); s.line(8, 15, 29, 3);
        return sheet(s);
    }

    static int[][][] smithLeggings() {
        Sheet s = new Sheet();
        // Apron line across the waist and a plate on the front.
        s.line(16, 39, 27, 4);
        s.line(16, 39, 28, 2);
        s.plate(21, 29, 6, 3);
        s.rivet(21, 29); s.rivet(26, 29);
        // The apron line again across both legs, a hammered plate on each thigh front.
        s.line(0, 15, 23, 4);
        s.line(0, 15, 24, 2);
        s.plate(4, 25, 4, 4);
        s.set(5, 26, 2);
        s.plate(0, 25, 3, 3);
        s.plate(8, 25, 3, 3);
        s.plate(12, 25, 3, 3);
        return sheet(s);
    }

    // ---------------------------------------------------------------- banner pattern and flags

    /** The banner pattern scroll (vanilla item layout: cream sheet, rolled ends) with the pick and hammer. */
    static final String[] SCROLL = {
            "................",
            "...aaaaaaaaaa...",
            "..bwwwwwwwwccb..",
            ".dbwwwwwwwwwcbd.",
            ".edaccccccccade.",
            "..ebbbaaaaabbe..",
            "...fcwwwwwccf...",
            "...fwwwwwwwcf...",
            "..fawwwwwwwwcf..",
            "..fcwwwwwwwwcf..",
            "..fcwwwwwwwwcf..",
            "..fcwwwwwwwwcf..",
            "..fcwwwwwwwwcf..",
            "..fwwwwwwwwwcf..",
            "..ffffffffffff..",
            "................"};
    static final String[] ITEM_PICK_HAMMER = {
            "..7777",
            ".7..77",
            "7.77..",
            "..77..",
            ".7..7.",
            "7....7"};

    static BufferedImage bannerPattern() {
        BufferedImage im = img(16, 16);
        int[] col = new int[128];
        col['a'] = 0xb2a684; col['b'] = 0x968b6c; col['w'] = 0xebebeb; col['c'] = 0xdad2bc;
        col['d'] = 0x704431; col['e'] = 0x5b3523; col['f'] = 0x847651;
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                char c = SCROLL[y].charAt(x);
                if (c != '.') px(im, x, y, col[c]);
            }
        // Motif in the sheet's brown ink (colour 7) with a pale grey lit edge (8).
        for (int y = 0; y < 6; y++)
            for (int x = 0; x < 6; x++) {
                char c = ITEM_PICK_HAMMER[y].charAt(x);
                if (c == '7') px(im, 5 + x, 7 + y, 0x634e42);
                            }
        return im;
    }

    /**
     * Crossed pick and hammer on an n x n grid: the pick is an arc head round the crossing with a diagonal
     * handle, the hammer a solid block head on the opposite diagonal.
     */
    static String[] flagMotif(int n) {
        char[][] g = new char[n][n];
        for (char[] row : g) java.util.Arrays.fill(row, '.');
        double c = (n - 1) / 2.0, rad = n * 0.34, th = n >= 16 ? 1.7 : 1.35;
        for (int y = 0; y < n; y++)
            for (int x = 0; x < n; x++) {
                double d = Math.hypot(x - c, y - c);
                double ang = Math.toDegrees(Math.atan2(y - c, x - c));
                if (ang < 0) ang += 360;
                if (Math.abs(d - rad - th / 2) <= th / 2 + 0.2 && Math.abs(ang - 225) <= 50) g[y][x] = '#';
            }
        int w = 2;
        // Pick handle, top-left to bottom-right, from the arc's middle out to the corner.
        int lo = (int) Math.round(c - rad * 0.95), hi = n - 1 - (n >= 16 ? 1 : 0);
        for (int k = lo; k <= hi; k++) {
            for (int t = 0; t < w; t++) if (k + t < n) g[k][k + t] = '#';
            if (n >= 16 && k + 1 < n) g[k + 1][k] = '#';
        }
        // Hammer handle, top-right to bottom-left, and its square head.
        int hy0 = 0, hh = n >= 16 ? 5 : 3, hx0 = n - (n >= 16 ? 5 : 4), hw = n >= 16 ? 5 : 4;
        for (int k = hh - 1; k < n - (n >= 16 ? 1 : 0); k++) {
            int x = n - 1 - (n >= 16 ? 4 : 3) - (k - (hh - 1));
            for (int t = 0; t < w; t++) if (x + t >= 0 && x + t < n && k < n) g[k][x + t] = '#';
            if (n >= 16 && x >= 0 && k + 1 < n) g[k + 1][x] = '#';
        }
        for (int y = hy0; y < hy0 + hh; y++) for (int x = hx0; x < hx0 + hw; x++) g[y][x] = '#';
        String[] out = new String[n];
        for (int y = 0; y < n; y++) out[y] = new String(g[y]);
        return out;
    }

    static final String[] FLAG_MOTIF = flagMotif(16);
    static final String[] SHIELD_MOTIF = flagMotif(10);

    /** Alpha convention: 255 body, 191 on the shaded (lower/right) edge, 60 in recesses. */
    static int[][] maskAlpha(String[] m, int size) {
        int h = m.length, w = m[0].length();
        int[][] a = new int[h][w];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                if (m[y].charAt(x) != '#') continue;
                boolean right = x + 1 >= w || m[y].charAt(x + 1) != '#';
                boolean below = y + 1 >= h || m[y + 1].charAt(x) != '#';
                a[y][x] = right || below ? 191 : 255;
            }
        return a;
    }

    static int[][][] bannerFlag() {
        int[][] g = new int[64][64], a = new int[64][64];
        int[][] ma = maskAlpha(FLAG_MOTIF, 16);
        Random r = new Random(7400);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                if (ma[y][x] == 0) continue;
                int n = r.nextInt(4);
                g[13 + y][3 + x] = 242 + n;
                a[13 + y][3 + x] = ma[y][x];
                g[13 + y][24 + x] = 225 + n;
                a[13 + y][24 + x] = ma[y][x];
            }
        return new int[][][]{g, a};
    }

    static int[][][] shieldFace() {
        int[][] g = new int[64][64], a = new int[64][64];
        int[][] ma = maskAlpha(SHIELD_MOTIF, 10);
        for (int y = 0; y < 10; y++)
            for (int x = 0; x < 10; x++) {
                if (ma[y][x] == 0) continue;
                g[7 + y][2 + x] = 242;
                a[7 + y][2 + x] = ma[y][x];
            }
        return new int[][][]{g, a};
    }

    // ---------------------------------------------------------------- mineral specimen

    /** Overlay greys (tinted at runtime): pure white facets down to a mid grey so the tint keeps its colour. */
    static final int[] FACET = {0x8e8e8e, 0xb4b4b4, 0xd2d2d2, 0xeaeaea, 0xffffff};
    static final String[] CHIP = {
            "..444433..",
            ".44333322.",
            "4433222211",
            "3322222110",
            ".32221110.",
            "..221110..",
            "....10...."};

    /** A grey stone matrix on a tiny wooden label plate; the cut chip itself is the overlay's job. */
    static BufferedImage specimenBase() {
        BufferedImage im = img(16, 16);
        Random r = new Random(7500);
        // Stone matrix round the chip: a rounded lump, lit top-left, darker toward the foot.
        for (int y = 1; y <= 10; y++)
            for (int x = 1; x <= 14; x++) {
                double dx = (x - 7.5) / 6.8, dy = (y - 6.0) / 5.0;
                if (dx * dx + dy * dy > 1) continue;
                int sh = 2;
                if (x + y < 10) sh = 3;
                if (x + y > 17) sh = 1;
                if (r.nextInt(4) == 0) sh += r.nextBoolean() ? 1 : -1;
                px(im, x, y, STONE[Math.max(0, Math.min(4, sh))]);
            }
        // Wooden label plate with two nail dots and an ink line.
        for (int y = 11; y <= 13; y++)
            for (int x = 1; x <= 14; x++) {
                int s = 2 + r.nextInt(2);
                if (y == 11) s = 4;
                if (y == 13) s = 1;
                px(im, x, y, WOOD[s]);
            }
        px(im, 2, 12, WOOD[0]);
        px(im, 13, 12, WOOD[0]);
        for (int x = 5; x <= 10; x++) px(im, x, 12, WOOD[1]);
        return outline(im);
    }

    static BufferedImage specimenOverlay() {
        BufferedImage im = img(16, 16);
        art(im, CHIP, 3, 3, FACET);
        return im;
    }

    // ---------------------------------------------------------------- specimen shelf

    /** Spruce slat frame round four open cubbies (6x6 each, divider 2 px so tiles join at 2 px). */
    static BufferedImage shelfFront() {
        BufferedImage im = img(16, 16);
        Random r = new Random(7600);
        boolean[][] hole = new boolean[16][16];
        int[][] cub = {{1, 1}, {9, 1}, {1, 9}, {9, 9}};
        for (int[] c : cub) for (int y = 0; y < 6; y++) for (int x = 0; x < 6; x++) hole[c[1] + y][c[0] + x] = true;
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                if (hole[y][x]) continue;
                boolean horizontal = y == 0 || y == 7 || y == 8 || y == 15;
                int s = 2 + (r.nextInt(5) < 2 ? 1 : 0) + (r.nextInt(7) == 0 ? -1 : 0);
                // Grain: horizontal runs on the rails, vertical on the stiles.
                if (horizontal && ((x * 3 + y) % 5 == 0)) s--;
                if (!horizontal && ((y * 3 + x) % 5 == 0)) s--;
                // Lit top edge of each rail, shaded lower edge.
                if (y == 0 || y == 7) s = 4 - (r.nextInt(4) == 0 ? 1 : 0);
                if (y == 15 || y == 8) s = 1 + (r.nextInt(3) == 0 ? 1 : 0);
                if (x == 7 && !horizontal) s = 3 + (r.nextInt(3) == 0 ? 1 : 0);
                if (x == 8 && !horizontal) s = 2 - (r.nextInt(3) == 0 ? 1 : 0);
                px(im, x, y, WOOD[Math.max(0, Math.min(4, s))]);
            }
        // Nail heads in the crossing of the slats.
        px(im, 7, 7, WOOD[4]);
        px(im, 8, 8, WOOD[0]);
        px(im, 0, 0, WOOD[4]);
        px(im, 15, 15, WOOD[0]);
        int[] inside = {0x120b06, 0x1d1209, 0x2a190c, 0x3a2414, 0x4a2f1a};
        for (int[] c : cub)
            for (int y = 0; y < 6; y++)
                for (int x = 0; x < 6; x++) {
                    int col = inside[2];
                    if (y == 0 || x == 0) col = inside[0];           // shadow under the top rail and beside the left stile
                    else if (y == 1 || x == 1) col = inside[1];
                    if (y == 5 || x == 5) col = inside[4];            // lit floor and right wall
                    else if ((y == 4 || x == 4) && col == inside[2]) col = inside[3];
                    if ((x + y * 2) % 7 == 0 && col == inside[2]) col = inside[1]; // grain in the back panel
                    px(im, c[0] + x, c[1] + y, col);
                }
        return im;
    }

    /** Four vertical spruce slats with dark joints, a few nails; tiles on all sides. */
    static BufferedImage shelfSide() {
        BufferedImage im = img(16, 16);
        Random r = new Random(7700);
        int[] base = {2, 3, 2, 3};
        for (int slat = 0; slat < 4; slat++) {
            int x0 = slat * 4;
            int[] run = new int[16];
            int cur = base[slat];
            for (int y = 0; y < 16; y++) {
                if (r.nextInt(3) == 0) cur = Math.max(1, Math.min(4, base[slat] + r.nextInt(3) - 1));
                run[y] = cur;
            }
            for (int x = 0; x < 4; x++)
                for (int y = 0; y < 16; y++) {
                    int s = run[(y + x * 5) % 16];
                    if (x == 0) s = Math.min(4, s + 1);         // lit left edge of the slat
                    if (x == 3) s = 0;                          // joint
                    px(im, x0 + x, y, WOOD[s]);
                }
        }
        // Nails: a lit head with a dark pixel below-right, top and bottom of every other slat.
        int[][] nails = {{1, 1}, {9, 1}, {1, 13}, {9, 13}};
        for (int[] n : nails) {
            px(im, n[0], n[1], WOOD[4]);
            px(im, n[0] + 1, n[1] + 1, WOOD[0]);
        }
        return im;
    }

    // ---------------------------------------------------------------- preview

    static void preview(BufferedImage[] sherds, BufferedImage[] pots, BufferedImage[] misc, int[][][][] trims,
            int[][][] banner, int[][][] shield) throws IOException {
        int s = 6, cell = 16 * s + 8;
        BufferedImage sheet = img(cell * 7, cell * 5 + 4 * 64 * 2 / 2 + 20);
        Graphics2D g = sheet.createGraphics();
        g.setColor(new Color(0x24, 0x24, 0x28));
        g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        for (int i = 0; i < 6; i++) {
            g.drawImage(sherds[i].getScaledInstance(16 * s, 16 * s, java.awt.Image.SCALE_FAST), i * cell + 4, 4, null);
            g.drawImage(pots[i].getScaledInstance(16 * s, 16 * s, java.awt.Image.SCALE_FAST), i * cell + 4, cell + 4, null);
        }
        for (int i = 0; i < misc.length; i++) {
            if (i == 4) continue;
            g.drawImage(misc[i].getScaledInstance(16 * s, 16 * s, java.awt.Image.SCALE_FAST), i * cell + 4, 2 * cell + 4, null);
        }
        // Specimen with a tint (emerald) composited, overlay alone on the right.
        BufferedImage tinted = img(16, 16);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int base = misc[3].getRGB(x, y);
                int ov = misc[4].getRGB(x, y);
                int c = base;
                if ((ov >>> 24) != 0) {
                    int t = 0x2fb36a;
                    int gr = ov & 255;
                    int rr = ((t >> 16) & 255) * gr / 255, gg = ((t >> 8) & 255) * gr / 255, bb = (t & 255) * gr / 255;
                    c = 0xFF000000 | (rr << 16) | (gg << 8) | bb;
                }
                tinted.setRGB(x, y, c);
            }
        g.drawImage(tinted.getScaledInstance(16 * s, 16 * s, java.awt.Image.SCALE_FAST), 4 * cell + 4, 2 * cell + 4, null);
        // Flags: banner front over a rust field, shield face over grey.
        BufferedImage flag = img(21, 41);
        for (int y = 0; y < 41; y++) for (int x = 0; x < 21; x++) flag.setRGB(x, y, 0xFF8a3a2a);
        composite(flag, banner, 1, 1, 0, 0, 20, 40, 0xe8e0d0);
        g.drawImage(flag.getScaledInstance(21 * 4, 41 * 4, java.awt.Image.SCALE_FAST), 4, 3 * cell + 4, null);
        BufferedImage sh = img(14, 24);
        for (int y = 0; y < 24; y++) for (int x = 0; x < 14; x++) sh.setRGB(x, y, 0xFF8a3a2a);
        composite(sh, shield, 1, 1, 0, 0, 12, 22, 0xe8e0d0);
        g.drawImage(sh.getScaledInstance(14 * 4, 24 * 4, java.awt.Image.SCALE_FAST), 120, 3 * cell + 4, null);
        // Trim sheets: raw greys on a dark field, 4x.
        for (int i = 0; i < 4; i++) {
            BufferedImage t = img(64, 32);
            for (int y = 0; y < 32; y++)
                for (int x = 0; x < 64; x++) {
                    int v = trims[i][0][y][x];
                    t.setRGB(x, y, trims[i][1][y][x] == 0 ? 0xFF303036 : 0xFF000000 | (v << 16) | (v << 8) | v);
                }
            g.drawImage(t.getScaledInstance(64 * 4, 32 * 4, java.awt.Image.SCALE_FAST), 200 + (i % 2) * 264, 3 * cell + 4 + (i / 2) * 140, null);
        }
        g.dispose();
        File f = new File("build/texturegen/collectibles.png");
        f.getParentFile().mkdirs();
        ImageIO.write(sheet, "png", f);
    }

    /** Shows a mask on a base colour: pattern colour weighted by its alpha, sampled from the sheet at (sx, sy). */
    static void composite(BufferedImage dst, int[][][] mask, int dx, int dy, int sx, int sy, int w, int h, int color) {
        for (int y = 0; y < h && dy + y < dst.getHeight(); y++)
            for (int x = 0; x < w && dx + x < dst.getWidth(); x++) {
                int a = mask[1][sy + dy + y][sx + dx + x];
                if (a == 0) continue;
                int base = dst.getRGB(dx + x, dy + y);
                int rr = (((base >> 16) & 255) * (255 - a) + ((color >> 16) & 255) * a) / 255;
                int gg = (((base >> 8) & 255) * (255 - a) + ((color >> 8) & 255) * a) / 255;
                int bb = ((base & 255) * (255 - a) + (color & 255) * a) / 255;
                dst.setRGB(dx + x, dy + y, 0xFF000000 | (rr << 16) | (gg << 8) | bb);
            }
    }
}
