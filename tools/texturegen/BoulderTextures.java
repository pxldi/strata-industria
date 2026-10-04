import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.Random;

/**
 * Boulders (redesign R1): the rock shard item of every rock, the two crack overlays and the flint nodules
 * overlay that sit on the boulder models. Reuses the helpers of {@link TextureGen}; it does not call its
 * {@code main}.
 *
 * <p>Run from the repository root:
 * <pre>
 * mkdir -p build/tg &amp;&amp; javac -d build/tg tools/texturegen/TextureGen.java tools/texturegen/BoulderTextures.java &amp;&amp; java -cp build/tg BoulderTextures
 * </pre>
 */
public final class BoulderTextures {
    // Digits 1-5 index the rock's ramp. A wedge of rock with a bright broken edge, pointing up and to the right.
    static final String[] SHARD = {
            "........55..",
            ".......4544.",
            "......44433.",
            "....5444333.",
            "...54443332.",
            "..544433322.",
            ".4444333222.",
            "4443332221..",
            "3332222211..",
            ".21111111...",
    };

    static final int CRACK_DARK = 0x1a1c22;
    static final int CRACK_EDGE = 0x2a2c32;

    /** Pixel paths of the cracks, read as (x, y) pairs from the top of the face down. */
    static final int[][] MAIN = {{7, 0}, {7, 1}, {6, 2}, {6, 3}, {7, 4}, {7, 5}, {8, 6}, {8, 7}, {7, 8}, {7, 9}, {6, 10}, {6, 11}, {5, 12}, {5, 13}, {4, 14}, {4, 15}};
    static final int[][] SPUR = {{8, 6}, {9, 6}, {10, 7}, {11, 7}, {12, 8}};
    static final int[][] LEFT = {{6, 3}, {5, 3}, {4, 4}, {3, 4}, {2, 5}, {1, 5}};
    static final int[][] FORK = {{7, 9}, {8, 10}, {9, 10}, {10, 11}, {10, 12}, {11, 13}, {12, 14}};
    static final int[][] HAIR = {{8, 7}, {9, 8}, {9, 9}, {10, 9}};

    static void trace(BufferedImage im, int[][] path, boolean shadow) {
        for (int[] p : path) {
            TextureGen.px(im, p[0], p[1], CRACK_DARK);
            if (shadow && p[0] + 1 < 16 && (im.getRGB(p[0] + 1, p[1]) >>> 24) == 0) TextureGen.px(im, p[0] + 1, p[1], CRACK_EDGE);
        }
    }

    /** One crack from top to bottom with a short spur. */
    static BufferedImage crackOne() {
        BufferedImage im = TextureGen.img();
        trace(im, MAIN, true);
        trace(im, SPUR, true);
        return im;
    }

    /** The crack has opened: wider in places, with branches both ways. */
    static BufferedImage crackTwo() {
        BufferedImage im = TextureGen.img();
        trace(im, MAIN, false);
        for (int i = 4; i < 10; i += 2) TextureGen.px(im, MAIN[i][0] + 1, MAIN[i][1], CRACK_DARK);
        trace(im, SPUR, false);
        trace(im, LEFT, false);
        trace(im, FORK, false);
        trace(im, HAIR, false);
        return im;
    }

    /** Dark flint nodules with a pale chalky rim, scattered. */
    static BufferedImage nodules() {
        BufferedImage im = TextureGen.img();
        int rim = 0xd6d2c0, rimShade = 0xb4b09c;
        int[][] at = {{2, 3}, {9, 1}, {12, 6}, {5, 8}, {10, 11}, {1, 13}, {7, 14}, {13, 13}};
        Random r = new Random(4242);
        for (int[] a : at) {
            int w = 3 + r.nextInt(2), h = 2 + r.nextInt(2);
            for (int dy = -1; dy <= h; dy++) {
                for (int dx = -1; dx <= w; dx++) {
                    int x = a[0] + dx, y = a[1] + dy;
                    if (x < 0 || y < 0 || x > 15 || y > 15) continue;
                    boolean edge = dx == -1 || dy == -1 || dx == w || dy == h;
                    boolean corner = (dx == -1 || dx == w) && (dy == -1 || dy == h);
                    if (corner) continue;
                    if (edge) TextureGen.px(im, x, y, dy == h || dx == w ? rimShade : rim);
                    else TextureGen.px(im, x, y, TextureGen.FLINT.get(dy == 0 ? 4 : dy == h - 1 ? 1 : 2));
                }
            }
        }
        return im;
    }

    public static void main(String[] args) throws IOException {
        for (TextureGen.Rock rock : TextureGen.ROCKS) TextureGen.save("item/" + rock.name() + "_shard", TextureGen.map(rock.ramp(), SHARD));
        TextureGen.save("block/boulder_crack_1", crackOne());
        TextureGen.save("block/boulder_crack_2", crackTwo());
        TextureGen.save("block/boulder_nodules", nodules());
    }

    private BoulderTextures() {}
}
