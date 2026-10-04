import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Random;

/**
 * Textures for outposts and transport (outposts spec 14): so far the outpost charter, its item and its board
 * screen. Charter and item come from the {@code TextureGen} ramps; the board screen is a plank frame round a
 * sheet of paper, like the deed pinned on the post.
 *
 * <p>Run from the repository root:
 * <pre>
 * mkdir -p build/tg &amp;&amp; javac -d build/tg tools/texturegen/TextureGen.java tools/texturegen/TransportTextures.java &amp;&amp; java -cp build/tg TransportTextures
 * </pre>
 */
public final class TransportTextures {
    static final TextureGen.Ramp WOOD = TextureGen.V2.WOOD_V2;
    static final TextureGen.Ramp PAPER = TextureGen.PAPER;
    static final TextureGen.Ramp BRONZE = TextureGen.BRONZE;
    static final int INK = 0x3a3028, INK_FADED = 0x6e6250, SEAL = 0xa8362a, SEAL_DARK = 0x6e231b, GLASS = 0xf4f0e0;

    /** Vertical planks: the post side. */
    static BufferedImage post() {
        BufferedImage im = TextureGen.img();
        TextureGen.grain(im, WOOD, new Random(41), 0, 0, 16, 16, 3, true);
        for (int y = 0; y < 16; y++) {
            TextureGen.px(im, 6, y, WOOD.get(4));
            TextureGen.px(im, 9, y, WOOD.get(1));
        }
        // Two nail heads and a split knot.
        TextureGen.px(im, 7, 3, WOOD.get(0));
        TextureGen.px(im, 8, 12, WOOD.get(0));
        TextureGen.px(im, 7, 8, WOOD.get(1));
        TextureGen.px(im, 8, 9, WOOD.get(1));
        return im;
    }

    /** Horizontal boards, for the back of the board. */
    static BufferedImage back() {
        BufferedImage im = TextureGen.img();
        TextureGen.grain(im, WOOD, new Random(42), 0, 0, 16, 16, 2, false);
        for (int x = 0; x < 16; x++) {
            TextureGen.px(im, x, 7, WOOD.get(1));
            TextureGen.px(im, x, 8, WOOD.get(3));
        }
        return im;
    }

    /** The board front in the region x 1..14, y 2..13: a plank frame round a deed under glass. */
    static BufferedImage front() {
        BufferedImage im = TextureGen.img();
        for (int y = 2; y <= 13; y++)
            for (int x = 1; x <= 14; x++) {
                boolean edge = x == 1 || x == 14 || y == 2 || y == 13;
                boolean ring = x == 2 || x == 13 || y == 3 || y == 12;
                int step = edge ? 1 : ring ? ((x == 2 || y == 3) ? 4 : 2) : 0;
                if (step > 0) TextureGen.px(im, x, y, WOOD.get(step));
            }
        for (int y = 4; y <= 11; y++)
            for (int x = 3; x <= 12; x++) TextureGen.px(im, x, y, PAPER.get(y == 4 || x == 3 ? 5 : y == 11 || x == 12 ? 3 : 4));
        // Ink lines, a seal and two bronze pins.
        for (int x = 4; x <= 10; x++) TextureGen.px(im, x, 5, INK);
        for (int x = 4; x <= 9; x++) TextureGen.px(im, x, 7, INK_FADED);
        for (int x = 4; x <= 7; x++) TextureGen.px(im, x, 9, INK_FADED);
        TextureGen.px(im, 10, 9, SEAL);
        TextureGen.px(im, 11, 9, SEAL);
        TextureGen.px(im, 10, 10, SEAL);
        TextureGen.px(im, 11, 10, SEAL_DARK);
        TextureGen.px(im, 3, 4, BRONZE.get(5));
        TextureGen.px(im, 12, 4, BRONZE.get(4));
        // Glass glint across the top left.
        TextureGen.px(im, 4, 6, GLASS);
        TextureGen.px(im, 5, 6, GLASS);
        TextureGen.px(im, 3, 7, GLASS);
        return im;
    }

    /** The item: the post with its board, outlined like the other items. */
    static BufferedImage item() {
        BufferedImage im = TextureGen.img();
        for (int y = 5; y <= 14; y++) for (int x = 7; x <= 8; x++) TextureGen.px(im, x, y, WOOD.get(x == 7 ? 4 : 2));
        for (int y = 2; y <= 8; y++)
            for (int x = 3; x <= 12; x++) {
                boolean frame = x == 3 || x == 12 || y == 2 || y == 8;
                TextureGen.px(im, x, y, frame ? WOOD.get(x == 3 || y == 2 ? 4 : 2) : PAPER.get(y == 3 || x == 4 ? 5 : 4));
            }
        for (int x = 5; x <= 9; x++) TextureGen.px(im, x, 4, INK);
        for (int x = 5; x <= 8; x++) TextureGen.px(im, x, 6, INK_FADED);
        TextureGen.px(im, 10, 6, SEAL);
        TextureGen.px(im, 10, 7, SEAL_DARK);
        for (int x = 6; x <= 9; x++) TextureGen.px(im, x, 14, WOOD.get(1));
        return TextureGen.outline(im);
    }

    /** The board screen: 176 x 166 in a 256 x 256 sheet. Plank frame, paper panel, a rule under the heading. */
    static BufferedImage gui() {
        BufferedImage im = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        int w = 176, h = 166;
        Random r = new Random(43);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) im.setRGB(x, y, 0xFF000000 | WOOD.get(3));
        }
        TextureGen.grain(im, WOOD, r, 0, 0, w, h, 3, false);
        // Frame bevel: dark outline, lit top left, shaded bottom right.
        for (int x = 0; x < w; x++) {
            TextureGen.px(im, x, 0, WOOD.get(0));
            TextureGen.px(im, x, h - 1, WOOD.get(0));
            TextureGen.px(im, x, 1, WOOD.get(4));
            TextureGen.px(im, x, h - 2, WOOD.get(1));
        }
        for (int y = 0; y < h; y++) {
            TextureGen.px(im, 0, y, WOOD.get(0));
            TextureGen.px(im, w - 1, y, WOOD.get(0));
            if (y > 0 && y < h - 1) {
                TextureGen.px(im, 1, y, WOOD.get(4));
                TextureGen.px(im, w - 2, y, WOOD.get(1));
            }
        }
        // Paper panel with a dark inset edge.
        int x0 = 7, y0 = 7, x1 = w - 8, y1 = 133;
        for (int y = y0; y <= y1; y++)
            for (int x = x0; x <= x1; x++) {
                boolean edge = x == x0 || y == y0;
                boolean shade = x == x1 || y == y1;
                TextureGen.px(im, x, y, edge ? WOOD.get(0) : shade ? PAPER.get(5) : PAPER.get(3));
            }
        for (int i = 0; i < 160; i++) {
            int x = x0 + 2 + r.nextInt(x1 - x0 - 4), y = y0 + 2 + r.nextInt(y1 - y0 - 4);
            boolean light = r.nextInt(3) > 0;
            for (int k = 0; k < 2 + r.nextInt(2); k++) TextureGen.px(im, x + k, y, light ? PAPER.get(4) : PAPER.get(2));
        }
        // Rule under the name and owner, and one above the traffic line.
        for (int x = 14; x < w - 14; x++) {
            TextureGen.px(im, x, 39, PAPER.get(2));
            TextureGen.px(im, x, y1 - 14, PAPER.get(2));
        }
        // Four bronze pins in the paper corners.
        for (int[] c : new int[][] {{x0 + 3, y0 + 3}, {x1 - 3, y0 + 3}, {x0 + 3, y1 - 3}, {x1 - 3, y1 - 3}}) {
            TextureGen.px(im, c[0], c[1], BRONZE.get(5));
            TextureGen.px(im, c[0] + 1, c[1], BRONZE.get(3));
            TextureGen.px(im, c[0], c[1] + 1, BRONZE.get(3));
            TextureGen.px(im, c[0] + 1, c[1] + 1, BRONZE.get(1));
        }
        return im;
    }

    public static void main(String[] args) throws IOException {
        TextureGen.save("block/outpost_charter_post", post());
        TextureGen.save("block/outpost_charter_back", back());
        TextureGen.save("block/outpost_charter_front", front());
        TextureGen.itemsV2 = true;
        TextureGen.save("item/outpost_charter", item());
        TextureGen.itemsV2 = false;
        TextureGen.saveRaw("gui/charter_board", gui());
    }

    private TransportTextures() {}
}
