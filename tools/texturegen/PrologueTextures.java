import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Random;

/**
 * Textures for the brick kiln and the casting table. Reuses the helpers and ramps of {@link TextureGen}; it
 * does not call its {@code main}.
 *
 * <p>Run from the repository root:
 * <pre>
 * mkdir -p build/tg &amp;&amp; javac -d build/tg tools/texturegen/TextureGen.java tools/texturegen/PrologueTextures.java &amp;&amp; java -cp build/tg PrologueTextures
 * </pre>
 */
public final class PrologueTextures {
    static int ce(int step) { return TextureGen.CERAMIC.get(step); }
    static int ls(int step) { return TextureGen.LIMESTONE_V2.get(step); }
    static void px(BufferedImage im, int x, int y, int c) { TextureGen.px(im, x, y, c); }

    static final int CAVITY = 0x16110f, DEEP = 0x0e0c0c;

    /** Course lines of the kiln's brick face: which pixels are mortar, for details that must follow the courses. */
    static BufferedImage bricks(long seed) { return TextureGen.forgeBricks(seed); }

    /** Door: a flat-arched opening, 8 wide and 8 tall, set into bricks with a lighter lintel course above it. */
    static BufferedImage front(int frame) {
        boolean active = frame >= 0;
        BufferedImage im = bricks(9101);
        // Lintel: a row of header bricks over the opening.
        for (int x = 3; x <= 12; x++) {
            px(im, x, 4, ce(x % 3 == 0 ? 1 : 5));
            px(im, x, 5, ce(x % 3 == 0 ? 1 : 4));
        }
        px(im, 2, 5, ce(1)); px(im, 13, 5, ce(1));
        // Opening x4..11, y6..13, with a one pixel reveal in the dark brick tone on the left and top.
        for (int y = 6; y <= 13; y++)
            for (int x = 4; x <= 11; x++) {
                int c;
                if (!active) c = y == 6 || x == 4 ? DEEP : CAVITY;
                else c = DEEP;
                px(im, x, y, c);
            }
        // Reveal: shadowed brick edge around the opening.
        for (int y = 6; y <= 14; y++) { px(im, 3, y, ce(1)); px(im, 12, y, ce(2)); }
        for (int x = 3; x <= 12; x++) px(im, x, 14, ce(x < 8 ? 5 : 4));
        if (active) {
            // Fire bed on the floor of the opening, flickering by frame.
            int[] height = {2, 3, 4, 4, 3, 3, 2, 2};
            for (int i = 0; i < 8; i++) {
                int x = 4 + i;
                int h = height[i] + ((frame + i) % 3 == 0 ? 1 : 0);
                for (int k = 0; k < h; k++) {
                    int y = 13 - k;
                    double g = 0.28 + 0.2 * k + 0.12 * ((frame + i * 2 + k) % 3);
                    px(im, x, y, TextureGen.Hp.band(Math.min(1.0, g)));
                }
            }
            // Warm bleed on the reveal and the lintel underside.
            for (int y = 8; y <= 14; y++) { TextureGen.Hp.glow(im, 3, y, 0.45, 0.45); TextureGen.Hp.glow(im, 12, y, 0.45, 0.45); }
            for (int x = 3; x <= 12; x++) TextureGen.Hp.glow(im, x, 14, 0.5, 0.45);
        }
        return im;
    }

    static BufferedImage side() {
        return bricks(9202);
    }

    /** Roof: bricks laid flat with a round flue in the middle. */
    static BufferedImage top() {
        BufferedImage im = bricks(9303);
        for (int y = 5; y <= 10; y++)
            for (int x = 5; x <= 10; x++) {
                double d = Math.hypot(x - 7.5, y - 7.5);
                if (d > 3.3) continue;
                if (d > 2.4) px(im, x, y, ce(x + y < 15 ? 4 : 2));   // the flue's lip
                else px(im, x, y, x + y < 14 ? DEEP : CAVITY);
            }
        return im;
    }

    /** Table top: dressed limestone with four shallow beds for molds. */
    static BufferedImage tableTop() {
        BufferedImage im = TextureGen.img();
        double[][] n = TextureGen.fractal(9404);
        Random r = new Random(9404);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int s = n[y][x] > 0.62 ? 4 : n[y][x] < 0.34 ? 2 : 3;
                if (r.nextInt(14) == 0) s += r.nextBoolean() ? 1 : -1;
                if (x == 0 || y == 0) s = 5;
                else if (x == 15 || y == 15) s = 1;
                px(im, x, y, ls(Math.max(1, Math.min(5, s))));
            }
        // Four beds, 6x6, sunk a little: dark top-left, lit bottom-right.
        for (int[] bed : new int[][] {{1, 1}, {9, 1}, {1, 9}, {9, 9}}) {
            for (int j = 0; j < 6; j++)
                for (int i = 0; i < 6; i++) {
                    int s = 2;
                    if (i == 0 || j == 0) s = 1;
                    else if (i == 5 || j == 5) s = 4;
                    else if (i == 1 || j == 1) s = 2;
                    else s = 3;
                    if ((i == 0 && j == 5) || (i == 5 && j == 0)) s = 2;
                    px(im, bed[0] + i, bed[1] + j, ls(s));
                }
        }
        return im;
    }

    /** Table edge: layered limestone. */
    static BufferedImage tableSide() {
        BufferedImage im = TextureGen.img();
        double[][] n = TextureGen.fractal(9505);
        Random r = new Random(9505);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int s = n[y][x] > 0.6 ? 4 : n[y][x] < 0.36 ? 2 : 3;
                if (y % 4 == 3) s = Math.max(1, s - 1);   // faint bedding lines
                if (r.nextInt(12) == 0) s += r.nextBoolean() ? 1 : -1;
                px(im, x, y, ls(Math.max(1, Math.min(5, s))));
            }
        for (int x = 0; x < 16; x++) { px(im, x, 2, ls(5)); px(im, x, 5, ls(1)); }
        return im;
    }

    static BufferedImage tableLeg() {
        return bricks(9606);
    }

    static BufferedImage gui() {
        BufferedImage im = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        TextureGen.panel(im, 176, 166);
        for (int i = 0; i < 4; i++) {
            TextureGen.slot(im, 8 + i * 18, 28);
            TextureGen.slot(im, 98 + i * 18, 28);
        }
        for (int y = 0; y < 14; y++)
            for (int x = 0; x < 14; x++)
                if (TextureGen.FLAME_SILHOUETTE[y].charAt(x) == '#') {
                    im.setRGB(81 + x, 29 + y, 0xff000000 | TextureGen.SLOT_FILL);
                    int band = y < 4 ? 4 : y < 8 ? 3 : y < 11 ? 2 : 1;
                    im.setRGB(176 + x, y, 0xff000000 | TextureGen.HEAT_BAND[band]);
                }
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++) TextureGen.slot(im, 8 + col * 18, 84 + row * 18);
        for (int col = 0; col < 9; col++) TextureGen.slot(im, 8 + col * 18, 142);
        return im;
    }

    public static void main(String[] args) throws IOException {
        TextureGen.save("block/brick_kiln_front", front(-1));
        TextureGen.save("block/brick_kiln_front_active", front(1));
        TextureGen.save("block/brick_kiln_side", side());
        TextureGen.save("block/brick_kiln_top", top());
        TextureGen.save("block/casting_table_top", tableTop());
        TextureGen.save("block/casting_table_side", tableSide());
        TextureGen.save("block/casting_table_leg", tableLeg());
        TextureGen.saveRaw("gui/brick_kiln", gui());
    }

    private PrologueTextures() {}
}
