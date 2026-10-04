import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Random;
import javax.imageio.ImageIO;

/**
 * Textures for the leads notebook (journal leads spec): the open notebook spread, the corkboard, and the widget
 * sheet with index cards, pins, the crossing-off tick, the sketch frame, page arrows, ribbon tabs and the toast
 * slip. Follows the style guide: ramps only, light from the top-left, 1 px bevels like vanilla GUIs.
 *
 * <p>Run from the repository root: {@code java tools/texturegen/JournalTextures.java}. Output is deterministic and
 * goes to {@code src/main/resources/assets/strataindustria/textures/gui/journal}; a 2x preview is written to
 * {@code build/texturegen/journal.png}.
 */
public final class JournalTextures {
    static final Path OUT = Path.of("src/main/resources/assets/strataindustria/textures/gui/journal");

    // Ramps, dark to light (style guide 3). Paper, ink, leather and wood match the survey notes and TextureGen.
    static final int[] PAPER = {0x4e4838, 0x76705a, 0xa29a80, 0xc8c0a2, 0xe2dabe};
    static final int[] INK = {0x1e1c1a, 0x2e2a26, 0x46403a, 0x5e5646, 0x7a705e};
    static final int[] LEATHER = {0x3a2216, 0x553322, 0x744834, 0x92614a, 0xb07e62};
    static final int[] WOOD = {0x3a2618, 0x5a3c24, 0x7a5634, 0x9c7448, 0xbc9462};
    static final int[] CORK = {0x4a301c, 0x6a4628, 0x8a5e36, 0xa67848, 0xc0945e};
    static final int[] PIN = {0x4a1414, 0x7a1e1a, 0xa82c22, 0xcc4a34, 0xe07a5a};
    static final int[] MALACHITE = {0x163828, 0x1e4a36, 0x2e6b4a, 0x4a9466, 0x78bf8a};
    static final int[] STRAW = {0x5a4a1e, 0x7c662a, 0xa08a3c, 0xc4ad56, 0xdcca78};
    static final int[] TWINE = {0x5a4630, 0x7a6040, 0x9c7c52, 0xb89a6a, 0xd0b888};

    static final int W = 292, H = 180;

    public static void main(String[] args) throws IOException {
        BufferedImage notebook = notebook();
        BufferedImage corkboard = corkboard();
        BufferedImage widgets = widgets();
        save("notebook", notebook);
        save("corkboard", corkboard);
        save("widgets", widgets);
        BufferedImage preview = img(W * 2 + 8 + 256, Math.max(H * 2, 256));
        blit(preview, notebook, 0, 0, W, H);
        blit(preview, corkboard, 0, H, W, H);
        blit(preview, widgets, W + 8, 0, 256, 128);
        File out = new File("build/texturegen/journal.png");
        out.getParentFile().mkdirs();
        ImageIO.write(scale(preview, 2), "png", out);
    }

    // ---------------------------------------------------------------- helpers

    static BufferedImage img(int w, int h) {
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    }

    static void px(BufferedImage im, int x, int y, int rgb) {
        if (x < 0 || y < 0 || x >= im.getWidth() || y >= im.getHeight()) return;
        im.setRGB(x, y, 0xFF000000 | rgb);
    }

    static void rect(BufferedImage im, int x0, int y0, int w, int h, int rgb) {
        for (int y = y0; y < y0 + h; y++) {
            for (int x = x0; x < x0 + w; x++) px(im, x, y, rgb);
        }
    }

    /** Vanilla-style bevel: dark outline, lit top-left inner edge, shaded bottom-right inner edge. */
    static void bevel(BufferedImage im, int x0, int y0, int w, int h, int[] ramp) {
        for (int x = x0; x < x0 + w; x++) {
            px(im, x, y0, ramp[0]);
            px(im, x, y0 + h - 1, ramp[0]);
        }
        for (int y = y0; y < y0 + h; y++) {
            px(im, x0, y, ramp[0]);
            px(im, x0 + w - 1, y, ramp[0]);
        }
        for (int x = x0 + 1; x < x0 + w - 1; x++) {
            px(im, x, y0 + 1, ramp[4]);
            px(im, x, y0 + h - 2, ramp[1]);
        }
        for (int y = y0 + 1; y < y0 + h - 1; y++) {
            px(im, x0 + 1, y, ramp[4]);
            px(im, x0 + w - 2, y, ramp[1]);
        }
    }

    static void clear(BufferedImage im, int x, int y) {
        if (x >= 0 && y >= 0 && x < im.getWidth() && y < im.getHeight()) im.setRGB(x, y, 0);
    }

    /** Knocks the four corner pixels off a rectangle so it reads as rounded. */
    static void round(BufferedImage im, int x0, int y0, int w, int h) {
        clear(im, x0, y0);
        clear(im, x0 + w - 1, y0);
        clear(im, x0, y0 + h - 1);
        clear(im, x0 + w - 1, y0 + h - 1);
    }

    /** Short fibre flecks a step lighter or darker than the base, as on the survey notes. */
    static void flecks(BufferedImage im, Random r, int x0, int y0, int w, int h, int count, int[] ramp) {
        for (int i = 0; i < count; i++) {
            int x = x0 + r.nextInt(Math.max(1, w - 3)), y = y0 + r.nextInt(Math.max(1, h));
            boolean light = r.nextInt(3) > 0;
            int len = 2 + r.nextInt(2);
            for (int k = 0; k < len; k++) px(im, x + k, y, light ? ramp[4] : ramp[2]);
        }
    }

    static void save(String name, BufferedImage im) throws IOException {
        File file = OUT.resolve(name + ".png").toFile();
        file.getParentFile().mkdirs();
        ImageIO.write(im, "png", file);
    }

    static void blit(BufferedImage dst, BufferedImage src, int dx, int dy, int w, int h) {
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int c = src.getRGB(x, y);
                if ((c >>> 24) != 0) dst.setRGB(dx + x, dy + y, c);
            }
        }
    }

    static BufferedImage scale(BufferedImage src, int k) {
        BufferedImage out = img(src.getWidth() * k, src.getHeight() * k);
        for (int y = 0; y < out.getHeight(); y++) {
            for (int x = 0; x < out.getWidth(); x++) out.setRGB(x, y, src.getRGB(x / k, y / k));
        }
        return out;
    }

    // ---------------------------------------------------------------- notebook

    /** Page rectangles inside the spread: left and right page, inset from the leather cover. */
    static final int PAGE_TOP = 5, PAGE_BOTTOM = H - 6, LEFT_PAGE = 7, RIGHT_PAGE_END = W - 8, SPINE = W / 2;

    /**
     * The open notebook in a 512x256 sheet: a leather cover with stitched edges, two paper pages with fibre flecks
     * and foxing, page edges stacked under the outer sides, and a shaded gutter at the spine.
     */
    static BufferedImage notebook() {
        BufferedImage im = img(512, 256);
        Random r = new Random(41);
        // Cover: leather, lit from the top-left, with grain and a stitched border.
        rect(im, 0, 0, W, H, LEATHER[2]);
        for (int i = 0; i < 700; i++) px(im, r.nextInt(W), r.nextInt(H), r.nextBoolean() ? LEATHER[1] : LEATHER[3]);
        bevel(im, 0, 0, W, H, LEATHER);
        for (int x = 4; x < W - 4; x += 3) {
            px(im, x, 2, LEATHER[4]);
            px(im, x, H - 3, LEATHER[3]);
        }
        round(im, 0, 0, W, H);

        // Page edges under each page, offset down and outward: a sheaf of pages, lit on top.
        for (int k = 3; k >= 1; k--) {
            int[] edge = {PAPER[1], PAPER[2], PAPER[3]};
            for (int x = LEFT_PAGE - k; x < SPINE; x++) px(im, x, PAGE_BOTTOM + k - 1, edge[k - 1]);
            for (int y = PAGE_TOP + k; y < PAGE_BOTTOM + k; y++) px(im, LEFT_PAGE - k, y, edge[k - 1]);
            for (int x = SPINE; x < RIGHT_PAGE_END + k; x++) px(im, x, PAGE_BOTTOM + k - 1, edge[k - 1]);
            for (int y = PAGE_TOP + k; y < PAGE_BOTTOM + k; y++) px(im, RIGHT_PAGE_END + k - 1, y, edge[k - 1]);
        }

        // Pages.
        rect(im, LEFT_PAGE, PAGE_TOP, RIGHT_PAGE_END - LEFT_PAGE, PAGE_BOTTOM - PAGE_TOP, PAPER[3]);
        flecks(im, r, LEFT_PAGE + 2, PAGE_TOP + 2, RIGHT_PAGE_END - LEFT_PAGE - 4, PAGE_BOTTOM - PAGE_TOP - 4, 520, PAPER);
        for (int i = 0; i < 16; i++) {
            int x = LEFT_PAGE + 6 + r.nextInt(RIGHT_PAGE_END - LEFT_PAGE - 12), y = PAGE_TOP + 6 + r.nextInt(PAGE_BOTTOM - PAGE_TOP - 12);
            if (Math.abs(x - SPINE) < 8) continue;
            px(im, x, y, PAPER[2]);
            px(im, x + 1, y, PAPER[2]);
            px(im, x, y + 1, PAPER[2]);
        }
        // Lit top edge, shaded bottom edge of the paper.
        for (int x = LEFT_PAGE; x < RIGHT_PAGE_END; x++) {
            px(im, x, PAGE_TOP, PAPER[4]);
            px(im, x, PAGE_BOTTOM - 1, PAPER[2]);
        }
        for (int y = PAGE_TOP; y < PAGE_BOTTOM; y++) {
            px(im, LEFT_PAGE, y, PAPER[4]);
            px(im, RIGHT_PAGE_END - 1, y, PAPER[2]);
        }
        // Gutter: the pages curve down into the spine, darker the closer they get.
        int[] gutter = {PAPER[3], PAPER[2], PAPER[2], PAPER[1], PAPER[0]};
        for (int d = 0; d < gutter.length; d++) {
            for (int y = PAGE_TOP; y < PAGE_BOTTOM; y++) {
                px(im, SPINE - 1 - d, y, gutter[gutter.length - 1 - d]);
                px(im, SPINE + d, y, gutter[gutter.length - 1 - d]);
            }
        }
        // A few page curls: the outer corners are a step darker where they lift.
        for (int i = 0; i < 4; i++) {
            px(im, LEFT_PAGE + i, PAGE_BOTTOM - 1 - (3 - i), PAPER[2]);
            px(im, RIGHT_PAGE_END - 1 - i, PAGE_BOTTOM - 1 - (3 - i), PAPER[1]);
        }
        // Title rules under each page heading, drawn in faded pencil with small gaps.
        for (int x = 18; x < 132; x++) {
            if (r.nextInt(14) != 0) px(im, x, 25, PAPER[2]);
        }
        for (int x = 160; x < 274; x++) {
            if (r.nextInt(14) != 0) px(im, x, 25, PAPER[2]);
        }
        // Ribbon marker hanging from the top of the spine over the right page.
        for (int y = PAGE_TOP; y < PAGE_TOP + 28; y++) {
            px(im, SPINE + 6, y, PIN[3]);
            px(im, SPINE + 7, y, PIN[2]);
            px(im, SPINE + 8, y, PIN[1]);
        }
        px(im, SPINE + 6, PAGE_TOP + 28, PIN[2]);
        px(im, SPINE + 8, PAGE_TOP + 28, PIN[1]);
        return im;
    }

    // ---------------------------------------------------------------- corkboard

    /** A cork board in a wooden frame, in a 512x256 sheet. Cork granules in 1 to 2 px clusters, no dithering. */
    static BufferedImage corkboard() {
        BufferedImage im = img(512, 256);
        Random r = new Random(77);
        rect(im, 0, 0, W, H, CORK[2]);
        for (int i = 0; i < 2600; i++) {
            int x = r.nextInt(W), y = r.nextInt(H);
            int step = r.nextInt(10) < 6 ? 1 : 3;
            px(im, x, y, CORK[step]);
            if (r.nextInt(3) == 0) px(im, x + 1, y, CORK[step]);
        }
        for (int i = 0; i < 260; i++) px(im, r.nextInt(W), r.nextInt(H), r.nextInt(4) == 0 ? CORK[4] : CORK[0]);
        // Old pin holes.
        for (int i = 0; i < 40; i++) px(im, 12 + r.nextInt(W - 24), 12 + r.nextInt(H - 24), CORK[0]);
        // Frame: 8 px of wood, grain along each side, lit top-left, mitred corners.
        int frame = 8;
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                int d = Math.min(Math.min(x, y), Math.min(W - 1 - x, H - 1 - y));
                if (d >= frame) continue;
                boolean horizontal = Math.min(y, H - 1 - y) <= Math.min(x, W - 1 - x);
                int grain = horizontal ? (x * 7 + y * 31) % 11 : (y * 7 + x * 31) % 11;
                int step = grain < 2 ? 1 : grain < 8 ? 2 : 3;
                boolean lit = horizontal ? y < H / 2 : x < W / 2;
                if (d == 0) step = 0;
                else if (d == 1) step = lit ? 4 : 1;
                else if (d == frame - 1) step = lit ? 1 : 3;
                else if (!lit) step = Math.max(1, step - 1);
                px(im, x, y, WOOD[step]);
            }
        }
        round(im, 0, 0, W, H);
        // Shadow the frame throws onto the cork, top and left.
        for (int x = frame; x < W - frame; x++) px(im, x, frame, CORK[0]);
        for (int y = frame; y < H - frame; y++) px(im, frame, y, CORK[0]);
        return im;
    }

    // ---------------------------------------------------------------- widgets

    /**
     * The widget sheet (256x256), matching the offsets in {@code JournalScreen} and {@code JournalToast}:
     * cards at (0,0) and (26,0); pin (0,26); tick (8,26); sketch frame (0,40); arrows (22,40) and (34,40), hovered
     * one row down at y 49; tabs from (0,64), selected at y 82; toast slip (0,96).
     */
    static BufferedImage widgets() {
        BufferedImage im = img(256, 256);
        card(im, 0, 0, false);
        card(im, 26, 0, true);
        pin(im, 0, 26);
        tick(im, 8, 26);
        frame(im, 0, 40);
        arrow(im, 22, 40, false, INK);
        arrow(im, 34, 40, true, INK);
        arrow(im, 22, 49, false, PIN);
        arrow(im, 34, 49, true, PIN);
        tab(im, 0, PIN, Icon.QUESTION);
        tab(im, 22, STRAW, Icon.PIN);
        tab(im, 44, MALACHITE, Icon.LIST);
        slip(im, 0, 96);
        return im;
    }

    /** An index card, 26x26, with two faint ruled lines; a closed card is older paper. */
    static void card(BufferedImage im, int x0, int y0, boolean closed) {
        int base = closed ? PAPER[2] : PAPER[3];
        rect(im, x0 + 1, y0 + 1, 24, 24, base);
        Random r = new Random(closed ? 3 : 2);
        for (int i = 0; i < 14; i++) px(im, x0 + 3 + r.nextInt(19), y0 + 3 + r.nextInt(20), closed ? PAPER[1] : PAPER[4]);
        for (int x = x0 + 3; x < x0 + 23; x++) {
            px(im, x, y0 + 23, closed ? PAPER[1] : PAPER[2]);
        }
        // Bevel, then a shadow on the cork below and to the right.
        for (int x = x0; x < x0 + 25; x++) {
            px(im, x, y0, PAPER[0]);
            px(im, x, y0 + 24, PAPER[0]);
        }
        for (int y = y0; y < y0 + 25; y++) {
            px(im, x0, y, PAPER[0]);
            px(im, x0 + 24, y, PAPER[0]);
        }
        for (int x = x0 + 1; x < x0 + 24; x++) px(im, x, y0 + 1, closed ? PAPER[3] : PAPER[4]);
        for (int y = y0 + 1; y < y0 + 24; y++) px(im, x0 + 1, y, closed ? PAPER[3] : PAPER[4]);
        for (int x = x0 + 1; x < x0 + 25; x++) px(im, x, y0 + 25, CORK[0]);
        for (int y = y0 + 1; y < y0 + 26; y++) px(im, x0 + 25, y, CORK[0]);
    }

    /** A red pushpin seen from above, 8x8: round head lit top-left with a single highlight pixel. */
    static void pin(BufferedImage im, int x0, int y0) {
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                double dx = x - 3.5, dy = y - 3.5, d = Math.sqrt(dx * dx + dy * dy);
                if (d > 3.6) continue;
                int step = d > 3.0 ? 0 : (dx + dy < -2.5 ? 4 : dx + dy < 0 ? 3 : dx + dy < 2.5 ? 2 : 1);
                px(im, x0 + x, y0 + y, PIN[step]);
            }
        }
        px(im, x0 + 2, y0 + 2, 0xf0b0a0);
    }

    /** The crossing-off tick in green ink, 10x10, two pixels thick and lit on top. */
    static void tick(BufferedImage im, int x0, int y0) {
        int[][] tick = {{1, 5}, {2, 6}, {3, 7}, {4, 6}, {5, 5}, {6, 4}, {7, 3}, {8, 2}};
        for (int[] t : tick) {
            px(im, x0 + t[0], y0 + t[1] - 1, MALACHITE[4]);
            px(im, x0 + t[0], y0 + t[1], MALACHITE[2]);
            px(im, x0 + t[0], y0 + t[1] + 1, MALACHITE[1]);
        }
    }

    /** A sketch frame, 22x22: a paper inset with ink photo corners, for the item a closed lead made. */
    static void frame(BufferedImage im, int x0, int y0) {
        rect(im, x0 + 1, y0 + 1, 20, 20, PAPER[4]);
        for (int x = x0 + 1; x < x0 + 21; x++) {
            px(im, x, y0 + 1, PAPER[2]);
            px(im, x, y0 + 20, PAPER[3]);
        }
        for (int y = y0 + 1; y < y0 + 21; y++) {
            px(im, x0 + 1, y, PAPER[2]);
            px(im, x0 + 20, y, PAPER[3]);
        }
        int[][] corners = {{0, 0, 1, 1}, {21, 0, -1, 1}, {0, 21, 1, -1}, {21, 21, -1, -1}};
        for (int[] c : corners) {
            for (int i = 0; i < 5; i++) {
                px(im, x0 + c[0] + c[2] * i, y0 + c[1], INK[1]);
                px(im, x0 + c[0], y0 + c[1] + c[3] * i, INK[1]);
            }
            px(im, x0 + c[0] + c[2], y0 + c[1] + c[3], INK[2]);
        }
    }

    /** A hand-drawn arrow, 12x9, pointing left or right. */
    static void arrow(BufferedImage im, int x0, int y0, boolean right, int[] ramp) {
        for (int y = 0; y < 9; y++) {
            for (int x = 0; x < 12; x++) {
                int ax = right ? 11 - x : x;
                double dy = Math.abs(y - 4);
                boolean head = ax < 5 && dy <= ax;
                boolean shaft = ax >= 4 && ax <= 10 && dy <= 1;
                if (!head && !shaft) continue;
                int step = y < 4 ? 3 : y == 4 ? 2 : 1;
                px(im, x0 + x, y0 + y, ramp[step]);
            }
        }
    }

    enum Icon { QUESTION, PIN, LIST }

    /**
     * A cloth ribbon tab, 22x18, sticking out from under the right edge of the book, with a swallowtail notch and a
     * small paper-coloured icon. The selected tab is one ramp step lighter.
     */
    static void tab(BufferedImage im, int x0, int[] ramp, Icon icon) {
        for (int selected = 0; selected < 2; selected++) {
            int y0 = selected == 0 ? 64 : 82;
            int[] c = selected == 0 ? ramp : new int[] {ramp[1], ramp[2], ramp[3], ramp[4], ramp[4]};
            for (int y = 0; y < 18; y++) {
                for (int x = 0; x < 22; x++) {
                    int notch = 21 - Math.abs(y - 8) / 2;
                    if (x > notch - (y >= 6 && y <= 11 ? 3 : 0)) continue;
                    int step = y == 0 || y == 17 ? 0 : y == 1 ? 3 : y == 16 ? 1 : 2;
                    if (x == 0) step = 0;
                    px(im, x0 + x, y0 + y, c[step]);
                }
            }
            int ix = x0 + 8, iy = y0 + 5;
            int light = PAPER[4], dark = PAPER[2];
            switch (icon) {
                case QUESTION -> {
                    int[][] q = {{1, 0}, {2, 0}, {3, 0}, {0, 1}, {4, 1}, {4, 2}, {3, 3}, {2, 4}, {2, 5}, {2, 7}};
                    for (int[] p : q) px(im, ix + p[0], iy + p[1], p[1] < 4 ? light : dark);
                }
                case PIN -> {
                    for (int y = 0; y < 4; y++) {
                        for (int x = 0; x < 4; x++) {
                            if ((x == 0 || x == 3) && (y == 0 || y == 3)) continue;
                            px(im, ix + 1 + x, iy + y, x + y < 3 ? light : dark);
                        }
                    }
                    px(im, ix + 2, iy + 4, dark);
                    px(im, ix + 2, iy + 5, dark);
                    px(im, ix + 2, iy + 6, dark);
                }
                case LIST -> {
                    for (int row = 0; row < 3; row++) {
                        px(im, ix, iy + row * 3, light);
                        for (int x = 2; x < 6; x++) px(im, ix + x, iy + row * 3, row == 0 ? light : dark);
                    }
                }
            }
        }
    }

    /** A torn notebook slip for toasts, 160x32: a bevelled paper strip whose right edge is torn ragged. */
    static void slip(BufferedImage im, int x0, int y0) {
        Random r = new Random(9);
        int[] tear = new int[32];
        for (int y = 0; y < 32; y++) tear[y] = 155 + r.nextInt(4);
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x <= tear[y]; x++) px(im, x0 + x, y0 + y, PAPER[3]);
        }
        flecks(im, r, x0 + 3, y0 + 3, 148, 26, 70, PAPER);
        for (int x = 0; x < 160; x++) {
            if (x <= tear[0]) px(im, x0 + x, y0, PAPER[0]);
            if (x <= tear[31]) px(im, x0 + x, y0 + 31, PAPER[0]);
            if (x <= tear[1] - 1) px(im, x0 + x, y0 + 1, PAPER[4]);
            if (x <= tear[30] - 1) px(im, x0 + x, y0 + 30, PAPER[1]);
        }
        for (int y = 0; y < 32; y++) {
            px(im, x0, y0 + y, PAPER[0]);
            if (y > 0 && y < 31) px(im, x0 + 1, y0 + y, PAPER[4]);
            px(im, x0 + tear[y], y0 + y, PAPER[1]);
        }
        // The icon sits on a sketch-frame inset like the notes page.
        for (int y = 5; y < 27; y++) {
            for (int x = 5; x < 27; x++) {
                boolean edge = x == 5 || y == 5 || x == 26 || y == 26;
                if (edge) px(im, x0 + x, y0 + y, (x == 5 || y == 5) ? PAPER[2] : PAPER[4]);
            }
        }
        // A pencil rule under the heading line.
        for (int x = 30; x < 150; x++) {
            if (r.nextInt(12) != 0) px(im, x0 + x, y0 + 16, PAPER[2]);
        }
    }
}
