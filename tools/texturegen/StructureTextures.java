import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Random;
import javax.imageio.ImageIO;

/**
 * Textures for the world structures (structures spec 10): fibre canvas, pit props, the survey notes item and
 * the survey notes page, and the cracked fire bricks and slag heaps of a ruined bloomery. Follows the style guide like {@code TextureGen}: 16x16, ramps only, light from the
 * top-left, 1 px coloured outline on items.
 *
 * <p>Run from the repository root: {@code java tools/texturegen/StructureTextures.java}. Output is
 * deterministic and goes to {@code src/main/resources/assets/strataindustria/textures}; a 6x preview is
 * written to {@code build/texturegen/structures.png}.
 */
public final class StructureTextures {
    static final Path OUT = Path.of("src/main/resources/assets/strataindustria/textures");

    // Ramps, dark to light (style guide 3).
    static final int[] CANVAS = {0x3e3a2a, 0x5e583e, 0x827a58, 0xa69e78, 0xc4bc98};
    static final int[] WOOD = {0x3a2618, 0x5a3c24, 0x7a5634, 0x9c7448, 0xbc9462};
    static final int[] PAPER = {0x4e4838, 0x76705a, 0xa29a80, 0xc8c0a2, 0xe2dabe};
    static final int[] INK = {0x1e1c1a, 0x2e2a26, 0x46403a, 0x5e5646, 0x7a705e};
    static final int[] MALACHITE = {0x163828, 0x1e4a36, 0x2e6b4a, 0x4a9466, 0x78bf8a};
    static final int[] RUST = {0x3a1a14, 0x5a2a1e, 0x7a3c2a, 0x9a5638, 0xb8744c};
    // Shared with TextureGen, so the ruin matches the fire bricks and slag the player makes later.
    static final int[] FIRE_BRICK = {0x6a5434, 0x8c7044, 0xae9058, 0xc8ac72, 0xe0c890};
    static final int[] CHARCOAL = {0x141416, 0x232327, 0x34343a, 0x4a4a52, 0x626270};
    static final int[] SLAG = {0x2a2420, 0x3e3632, 0x544a44, 0x6e625a, 0x887c72};
    static final int[] HEMATITE = {0x2a1416, 0x4a2020, 0x6e3226, 0x8e4a34, 0xae6a4c};

    public static void main(String[] args) throws IOException {
        BufferedImage[] preview = {
                save("block/fibre_canvas", canvas()),
                save("block/pit_prop_side", propSide()),
                save("block/pit_prop_end", propEnd()),
                save("item/survey_notes", outline(notes())),
                save("item/survey_notes_overlay", swatch()),
                save("block/cracked_fire_bricks", crackedBricks(false)),
                save("block/cracked_fire_bricks_sooted", crackedBricks(true)),
                save("block/slag_heap", slagHeap()),
        };
        save("gui/survey_notes", page());
        save("gui/survey_arrows", arrows());
        writePreview(preview);
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
        for (int y = 0; y < 16; y++) {
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
        }
        return out;
    }

    static BufferedImage save(String path, BufferedImage im) throws IOException {
        File f = OUT.resolve(path + ".png").toFile();
        f.getParentFile().mkdirs();
        ImageIO.write(im, "png", f);
        return im;
    }

    // ---------------------------------------------------------------- fibre canvas

    /**
     * Coarse plain weave: two-pixel threads crossing over and under, each thread lit on its top-left
     * pixel. A few darker weathering stains break up the repeat. Tiles seamlessly.
     */
    static BufferedImage canvas() {
        BufferedImage im = img(16, 16);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                // 4x4 cells of two threads each, alternating between running across and running down.
                boolean across = (((x >> 2) + (y >> 2)) & 1) == 0;
                int side = across ? y & 1 : x & 1;      // 0 = the thread's lit edge
                int end = across ? x & 3 : y & 3;       // 3 = where the threads dip under the next cell
                int step = side == 0 ? 3 : 2;
                if (end == 3) step--;
                if (end == 0 && side == 0) step = 4;
                im.setRGB(x, y, 0xFF000000 | CANVAS[step]);
            }
        }
        // Stains: soft clusters one step darker, so the repeat does not show.
        int[][] stains = {{2, 9}, {3, 9}, {2, 10}, {12, 3}, {13, 3}, {9, 13}, {10, 14}};
        for (int[] st : stains) {
            int step = indexOf(CANVAS, rgb(im, st[0], st[1]));
            im.setRGB(st[0], st[1], 0xFF000000 | CANVAS[Math.max(1, step - 1)]);
        }
        return im;
    }

    static int indexOf(int[] ramp, int c) {
        for (int i = 0; i < ramp.length; i++) if (ramp[i] == c) return i;
        return 2;
    }

    // ---------------------------------------------------------------- ruined bloomery

    /** Hairline cracks across the fire brick bond: stepped diagonals, never straight runs. */
    static final int[][][] CRACKS = {
            {{1, 1}, {2, 1}, {3, 2}, {4, 2}, {5, 3}},
            {{9, 4}, {10, 5}, {10, 6}, {11, 6}, {12, 7}},
            {{3, 9}, {3, 10}, {4, 10}, {5, 11}},
            {{12, 12}, {13, 13}, {13, 14}, {14, 14}, {15, 15}, {0, 15}},
    };

    /**
     * The fire bricks of the bloomery, broken: the same bond and ramp read from {@code fire_bricks.png}, with
     * hairline cracks, two chipped corners filled with mortar, and in the sooted variant a cloud of soot over
     * the upper third.
     */
    static BufferedImage crackedBricks(boolean sooted) throws IOException {
        BufferedImage base = ImageIO.read(OUT.resolve("block/fire_bricks.png").toFile());
        BufferedImage im = img(16, 16);
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) px(im, x, y, rgb(base, x, y));
        boolean[][] cracked = new boolean[16][16];
        for (int[][] crack : CRACKS) {
            for (int i = 0; i < crack.length; i++) {
                int x = crack[i][0], y = crack[i][1];
                // The crack is darkest in the middle and fades into the brick at its ends.
                boolean end = i == 0 || i == crack.length - 1;
                px(im, x, y, end ? FIRE_BRICK[0] : CHARCOAL[3]);
                cracked[y][x] = true;
            }
        }
        // Each crack catches the light on its lower edge, which is what makes it read as a crack.
        for (int[][] crack : CRACKS) {
            for (int[] c : crack) {
                int x = c[0], y = Math.min(15, c[1] + 1);
                if (!cracked[y][x] && rgb(base, x, y) != FIRE_BRICK[0]) px(im, x, y, FIRE_BRICK[4]);
            }
        }
        // Chipped corners: a brick's corner pixels broken off, showing the mortar behind.
        int[][] chips = {{6, 4}, {7, 4}, {6, 5}, {13, 8}, {13, 9}};
        for (int[] c : chips) px(im, c[0], c[1], FIRE_BRICK[c[1] == 5 || c[1] == 9 ? 1 : 0]);
        if (sooted) {
            // A cloud of soot over the upper third, thickest at the top, with a ragged lower edge.
            Random r = new Random(5150);
            int[] reach = new int[16];
            // One billow of soot that wraps around the tile edge, so neighbouring blocks do not form a band.
            for (int x = 0; x < 16; x++) {
                reach[x] = (int) Math.round(1.5 + 3.5 * Math.sin(Math.PI * 2 * x / 16 + 0.6)) + r.nextInt(2);
            }
            for (int y = 0; y < 6; y++) {
                for (int x = 0; x < 16; x++) {
                    if (y > reach[x]) continue;
                    boolean mortar = rgb(base, x, y) == FIRE_BRICK[0];
                    int depth = reach[x] - y;
                    int c = mortar ? CHARCOAL[0] : depth >= 3 ? CHARCOAL[1] : CHARCOAL[2];
                    // Highlights of the brick still show faintly through thin soot.
                    if (!mortar && depth == 0 && rgb(base, x, y) == FIRE_BRICK[4]) c = CHARCOAL[3];
                    px(im, x, y, c);
                }
            }
        }
        return im;
    }

    /** Up-face rectangles of the slag heap model (x0, y0, x1, y1), each one lump seen from above. */
    static final int[][] SLAG_LUMPS = {{1, 1, 6, 6}, {9, 3, 13, 7}, {3, 7, 6, 10}, {10, 9, 12, 11}};

    /**
     * Glassy dark lumps for the slag heap model: each lump's top lit along its top-left edge (step 5), a
     * rust streak on one lump, a couple of pores, and darker side bands along the bottom rows.
     */
    static BufferedImage slagHeap() {
        BufferedImage im = img(16, 16);
        Random r = new Random(4242);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int step = y >= 11 ? 1 : 2;
                if (r.nextInt(5) == 0) step += r.nextBoolean() ? 1 : -1;
                px(im, x, y, SLAG[Math.max(0, step)]);
            }
        }
        for (int[] l : SLAG_LUMPS) {
            for (int y = l[1]; y < l[3]; y++) {
                for (int x = l[0]; x < l[2]; x++) {
                    int step = 3;
                    if (y == l[1] && x < l[2] - 1) step = 4;
                    if (x == l[0] && y < l[3] - 1) step = 4;
                    if (x == l[2] - 1 || y == l[3] - 1) step = 2;
                    px(im, x, y, SLAG[step]);
                }
            }
            // The sharp glassy glint where the light catches the corner.
            px(im, l[0] + 1, l[1], SLAG[4]);
            px(im, l[0], l[1], SLAG[4]);
            px(im, l[0] + 1, l[1] + 1, 0xa49a8e);
        }
        // Side bands: lit at the top edge, dark at the foot.
        for (int x = 0; x < 16; x++) {
            px(im, x, 11, SLAG[2]);
            px(im, x, 12, SLAG[3]);
            px(im, x, 15, SLAG[0]);
        }
        // Rust weeping out of the second lump, and pores.
        int[][] rust = {{10, 5}, {11, 5}, {11, 6}, {12, 13}, {13, 14}};
        for (int[] p : rust) px(im, p[0], p[1], HEMATITE[p[1] == 5 ? 3 : 2]);
        px(im, 4, 3, SLAG[0]);
        px(im, 4, 9, SLAG[0]);
        px(im, 11, 4, SLAG[1]);
        return im;
    }

    // ---------------------------------------------------------------- pit prop

    /**
     * A stripped round timber standing upright. The model shows columns 2 to 13: lit on the left, dark
     * on the right as the round turns away from the light, with long grain lines, adze facets and a knot.
     */
    static BufferedImage propSide() {
        BufferedImage im = img(16, 16);
        int[] column = {1, 1, 4, 4, 3, 3, 3, 3, 2, 3, 2, 2, 2, 1, 1, 1};
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) im.setRGB(x, y, 0xFF000000 | WOOD[column[x]]);
        }
        // Grain: broken dark lines running the length of the timber.
        int[][] grain = {{5, 0, 6}, {5, 9, 16}, {7, 3, 12}, {10, 0, 4}, {10, 7, 16}, {12, 2, 10}};
        for (int[] g : grain) {
            for (int y = g[1]; y < g[2]; y++) px(im, g[0], y, WOOD[Math.max(1, column[g[0]] - 1)]);
        }
        // Adze facets: short lit strokes where the bark was cut away.
        int[][] facets = {{3, 2}, {4, 7}, {3, 12}, {6, 5}, {8, 13}};
        for (int[] f : facets) {
            px(im, f[0], f[1], WOOD[4]);
            px(im, f[0], f[1] + 1, WOOD[Math.min(4, column[f[0]] + 1)]);
        }
        // A knot: dark core, lit top-left rim.
        px(im, 8, 9, WOOD[0]);
        px(im, 9, 9, WOOD[1]);
        px(im, 8, 10, WOOD[1]);
        px(im, 7, 8, WOOD[4]);
        px(im, 8, 8, WOOD[3]);
        // A shrinkage crack near the dark side.
        for (int y = 4; y < 9; y++) px(im, 11, y, WOOD[0]);
        return im;
    }

    /** The sawn end: growth rings around the pith, a radial check, and the weathered rim. */
    static BufferedImage propEnd() {
        BufferedImage im = img(16, 16);
        double cx = 7.5, cy = 7.5;
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                double d = Math.hypot(x - cx, y - cy);
                int step;
                if (d > 5.7) step = x + y < 13 ? 2 : 1;      // weathered rim, lifted where it faces the light
                else if (d < 1.3) step = 1;                  // pith
                else if (d < 2.3 || (d >= 3.4 && d < 4.4)) step = 3;
                else if (d >= 5.0) step = 4;                 // sapwood just under the rim
                else step = 2;                               // dark latewood rings
                im.setRGB(x, y, 0xFF000000 | WOOD[step]);
            }
        }
        // Radial check from the pith toward the bottom right.
        int[][] check = {{9, 9}, {10, 10}, {11, 11}, {11, 12}};
        for (int[] c : check) px(im, c[0], c[1], WOOD[0]);
        // A few lit pores on the upper left of the face.
        px(im, 5, 4, WOOD[4]);
        px(im, 4, 6, WOOD[4]);
        px(im, 7, 3, WOOD[4]);
        return im;
    }

    // ---------------------------------------------------------------- survey notes

    /** A folded sheet tied with twine, tilted, with lines of ink and a bare spot for the mineral swatch. */
    static BufferedImage notes() {
        BufferedImage im = img(16, 16);
        // Sheet: rows of the folded paper, slightly skewed so it lies at an angle.
        int[][] rows = { // y, x0, x1
                {2, 4, 12}, {3, 3, 12}, {4, 3, 13}, {5, 3, 13}, {6, 2, 13}, {7, 2, 13}, {8, 2, 13}, {9, 2, 14},
                {10, 2, 14}, {11, 2, 14}, {12, 3, 14}, {13, 4, 13}};
        for (int[] row : rows) {
            for (int x = row[1]; x <= row[2]; x++) {
                int step = 3;
                if (x == row[1] || row[0] == 2) step = 4;          // lit edges
                if (x == row[2] || row[0] == 13) step = 2;         // edges facing away
                px(im, x, row[0], PAPER[step]);
            }
        }
        // The fold: a crease across the sheet, lit above and shaded below.
        for (int x = 3; x <= 13; x++) {
            px(im, x, 7, PAPER[2]);
            if (x > 3 && x < 13) px(im, x, 6, PAPER[4]);
        }
        // Ink lines of handwriting.
        int[][] ink = {{4, 4, 9}, {4, 10, 11}, {9, 4, 6}, {9, 8, 12}, {11, 4, 10}};
        for (int[] l : ink) {
            for (int x = l[1]; x <= l[2]; x++) px(im, x, l[0], INK[3]);
        }
        // Twine tied round the bundle.
        for (int y = 2; y <= 13; y++) {
            int x = 8 - (y - 2) / 6;
            if (opaque(im, x, y)) px(im, x, y, y < 7 ? 0xA08A3C : 0x7C662A);
        }
        // The bare corner where the swatch sits (top right of the sheet).
        for (int y = 3; y <= 4; y++) for (int x = 11; x <= 12; x++) px(im, x, y, PAPER[3]);
        return im;
    }

    /** The swatch layer: light greys that the mineral tint colours, lit top-left. */
    static BufferedImage swatch() {
        BufferedImage im = img(16, 16);
        px(im, 11, 3, 0xFFFFFF);
        px(im, 12, 3, 0xE4E4E4);
        px(im, 11, 4, 0xE4E4E4);
        px(im, 12, 4, 0xB8B8B8);
        return im;
    }

    // ---------------------------------------------------------------- the page

    static final int PAGE_W = 176, PAGE_H = 166;
    static final int ROSE_X = 10, ROSE_Y = 40, ROSE_SIZE = 64;

    /**
     * One parchment page in a 256x256 sheet: vanilla-style 1 px bevel in the paper ramp, soft fibre
     * flecks, a vertical fold, worn corners, and a compass rose drawn in faded ink where the arrow goes.
     */
    static BufferedImage page() {
        BufferedImage im = img(256, 256);
        Random r = new Random(5);
        for (int y = 0; y < PAGE_H; y++) {
            for (int x = 0; x < PAGE_W; x++) im.setRGB(x, y, 0xFF000000 | PAPER[3]);
        }
        // Fibre flecks: short 2-3 px clusters, a step lighter or darker.
        for (int i = 0; i < 260; i++) {
            int x = 3 + r.nextInt(PAGE_W - 6), y = 3 + r.nextInt(PAGE_H - 6);
            boolean light = r.nextInt(3) > 0;
            int len = 2 + r.nextInt(2);
            for (int k = 0; k < len; k++) px(im, x + k, y, light ? PAPER[4] : PAPER[2]);
        }
        // Foxing: a few faint brown spots, as on old paper.
        for (int i = 0; i < 9; i++) {
            int x = 6 + r.nextInt(PAGE_W - 12), y = 6 + r.nextInt(PAGE_H - 12);
            px(im, x, y, PAPER[2]);
            px(im, x + 1, y, PAPER[2]);
            px(im, x, y + 1, PAPER[2]);
        }
        // The fold under the heading: lit above the crease, shaded below it.
        int fold = 35;
        for (int x = 2; x < PAGE_W - 2; x++) {
            px(im, x, fold - 1, PAPER[4]);
            px(im, x, fold, PAPER[2]);
        }
        // Bevel: outer line, then lit top-left and shaded bottom-right edges.
        for (int x = 0; x < PAGE_W; x++) {
            px(im, x, 0, PAPER[0]);
            px(im, x, PAGE_H - 1, PAPER[0]);
            px(im, x, 1, PAPER[4]);
            px(im, x, PAGE_H - 2, PAPER[1]);
        }
        for (int y = 0; y < PAGE_H; y++) {
            px(im, 0, y, PAPER[0]);
            px(im, PAGE_W - 1, y, PAPER[0]);
            if (y > 0 && y < PAGE_H - 1) {
                px(im, 1, y, PAPER[4]);
                px(im, PAGE_W - 2, y, PAPER[1]);
            }
        }
        // Worn corners: cut by two pixels, with the outline following the cut.
        int[][] corners = {{0, 0, 1, 1}, {PAGE_W - 1, 0, -1, 1}, {0, PAGE_H - 1, 1, -1}, {PAGE_W - 1, PAGE_H - 1, -1, -1}};
        for (int[] c : corners) {
            im.setRGB(c[0], c[1], 0);
            im.setRGB(c[0] + c[2], c[1], 0);
            im.setRGB(c[0], c[1] + c[3], 0);
            px(im, c[0] + c[2], c[1] + c[3], PAPER[0]);
            px(im, c[0] + 2 * c[2], c[1], PAPER[0]);
            px(im, c[0], c[1] + 2 * c[3], PAPER[0]);
        }
        rose(im);
        return im;
    }

    /** A ring with eight ticks and a north mark, in faded ink, centred where the screen draws the arrow. */
    static void rose(BufferedImage im) {
        double cx = ROSE_X + ROSE_SIZE / 2.0 - 0.5, cy = ROSE_Y + ROSE_SIZE / 2.0 - 0.5;
        // Two rings: the outer drawn firmly, the inner faint.
        for (int y = ROSE_Y; y < ROSE_Y + ROSE_SIZE; y++) {
            for (int x = ROSE_X; x < ROSE_X + ROSE_SIZE; x++) {
                double d = Math.hypot(x - cx, y - cy);
                if (Math.abs(d - 27) < 0.55) px(im, x, y, INK[3]);
                else if (Math.abs(d - 20) < 0.5 && ((x + y) % 3 != 0)) px(im, x, y, INK[4]);
            }
        }
        // Ticks at the eight compass points; the cardinal ones longer.
        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(i * 45);
            double inner = i % 2 == 0 ? 21 : 23.5, outer = i % 2 == 0 ? 31 : 27;
            for (double t = inner; t <= outer; t += 0.35) {
                int x = (int) Math.round(cx + Math.sin(a) * t), y = (int) Math.round(cy - Math.cos(a) * t);
                px(im, x, y, i % 2 == 0 ? INK[2] : INK[3]);
            }
        }
        // "N" above the north tick, drawn in pixels like a quick pen stroke.
        int nx = (int) Math.round(cx) - 2, ny = ROSE_Y - 6;
        String[] n = {"X..X", "XX.X", "X.XX", "X..X", "X..X"};
        for (int y = 0; y < n.length; y++) {
            for (int x = 0; x < 4; x++) if (n[y].charAt(x) == 'X') px(im, nx + x, ny + y, INK[1]);
        }
        // A small dot at the centre where the arrow pivots.
        px(im, (int) Math.round(cx), (int) Math.round(cy), INK[2]);
    }

    // ---------------------------------------------------------------- arrows

    /**
     * Ten 16 px cells: an inked arrow pointing ahead, then turned 45 degrees clockwise each cell; a
     * tick mark in malachite green for a deposit that has been found; and an empty cell.
     */
    static BufferedImage arrows() {
        BufferedImage im = img(160, 16);
        // Arrow pointing up, in cell coordinates centred on (0, 0): a head and a feathered shaft.
        double[][] head = {{0, -7}, {4.2, -1.2}, {1.2, -2.2}, {1.2, 6.5}, {-1.2, 6.5}, {-1.2, -2.2}, {-4.2, -1.2}};
        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(i * 45);
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    // Rotate the pixel centre back into the arrow's frame.
                    double px = x - 7.5, py = y - 7.5;
                    double ax = px * Math.cos(-a) - py * Math.sin(-a), ay = px * Math.sin(-a) + py * Math.cos(-a);
                    if (!inside(head, ax, ay)) continue;
                    // Head darker and redder (a quick wash of rust ink), shaft in plain ink; lit on the left.
                    int c = ay < -1.6 ? (ax < 0 ? RUST[3] : RUST[2]) : (ax < 0 ? INK[2] : INK[1]);
                    px(im, i * 16 + x, y, c);
                }
            }
        }
        // The tick: a short stroke down-right and a long one up-right, two pixels thick, lit on top.
        int ox = 8 * 16;
        int[][] tick = {{3, 8}, {4, 9}, {5, 10}, {6, 11}, {7, 10}, {8, 9}, {9, 8}, {10, 7}, {11, 6}, {12, 5}, {13, 4}};
        for (int[] t : tick) {
            px(im, ox + t[0], t[1], MALACHITE[3]);
            px(im, ox + t[0], t[1] + 1, MALACHITE[2]);
            px(im, ox + t[0], t[1] + 2, MALACHITE[1]);
        }
        return im;
    }

    static boolean inside(double[][] poly, double x, double y) {
        boolean in = false;
        for (int i = 0, j = poly.length - 1; i < poly.length; j = i++) {
            if ((poly[i][1] > y) != (poly[j][1] > y)
                    && x < (poly[j][0] - poly[i][0]) * (y - poly[i][1]) / (poly[j][1] - poly[i][1]) + poly[i][0]) {
                in = !in;
            }
        }
        return in;
    }

    // ---------------------------------------------------------------- preview

    static void writePreview(BufferedImage[] tiles) throws IOException {
        int scale = 6, cell = 16 * scale + 8;
        BufferedImage sheet = img(cell * tiles.length + cell * 3, cell * 3);
        var g = sheet.getGraphics();
        g.setColor(new java.awt.Color(0x24, 0x24, 0x28));
        g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        for (int i = 0; i < tiles.length; i++) {
            g.drawImage(tiles[i].getScaledInstance(16 * scale, 16 * scale, java.awt.Image.SCALE_FAST), i * cell + 4, 4, null);
        }
        // 3x3 tiling check of the canvas, and the tinted notes icon.
        for (int ty = 0; ty < 3; ty++) {
            for (int tx = 0; tx < 3; tx++) {
                g.drawImage(tiles[0].getScaledInstance(32, 32, java.awt.Image.SCALE_FAST), 4 + tx * 32, cell + 4 + ty * 32, null);
                g.drawImage(tiles[1].getScaledInstance(32, 32, java.awt.Image.SCALE_FAST), 120 + tx * 32, cell + 4 + ty * 32, null);
            }
        }
        File f = new File("build/texturegen/structures.png");
        f.getParentFile().mkdirs();
        ImageIO.write(sheet, "png", f);
    }
}
