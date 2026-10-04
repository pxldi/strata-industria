import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Random;

/**
 * Textures for the pony and the incline winch (outposts and transport spec 5.3, 5.4 and 14): the harness item, the
 * harness overlay drawn over the vanilla horse sheet, the hay rack and the winch frame and drum.
 *
 * <p>Run from the repository root:
 * <pre>
 * mkdir -p build/tg &amp;&amp; javac -d build/tg tools/texturegen/TextureGen.java tools/texturegen/TransportTextures.java tools/texturegen/PonyTextures.java &amp;&amp; java -cp build/tg PonyTextures
 * </pre>
 */
public final class PonyTextures {
    static final TextureGen.Ramp WOOD = TextureGen.V2.WOOD_V2;
    static final TextureGen.Ramp DARK = TextureGen.DARK_WOOD;
    static final TextureGen.Ramp IRON = TextureGen.WROUGHT_IRON;
    static final TextureGen.Ramp LEATHER = TextureGen.LEATHER;
    static final TextureGen.Ramp BRASS = TextureGen.BRASS;
    static final TextureGen.Ramp ROPE = TextureGen.ramp(0, 0x4e3818, 0x785526, 0xa27a38, 0xc99f4c, 0xe6c46e);

    // ---------------------------------------------------------------- blocks

    /** The hay rack's boards: horizontal planks, a joint every five rows, treenails near the ends. */
    static BufferedImage hayRack() {
        BufferedImage im = TextureGen.img();
        Random r = new Random(601);
        TextureGen.grain(im, WOOD, r, 0, 0, 16, 16, 3, false);
        for (int y : new int[] {5, 10, 15}) {
            for (int x = 0; x < 16; x++) {
                TextureGen.px(im, x, y, WOOD.get(1));
                if (y + 1 < 16) TextureGen.px(im, x, y + 1, WOOD.get(4));
            }
        }
        // Top edge lit.
        for (int x = 0; x < 16; x++) TextureGen.px(im, x, 0, WOOD.get(5));
        for (int y : new int[] {2, 7, 12}) {
            TextureGen.px(im, 2, y, WOOD.get(1));
            TextureGen.px(im, 13, y, WOOD.get(1));
        }
        // A few knots.
        TextureGen.px(im, 7, 3, WOOD.get(2));
        TextureGen.px(im, 8, 3, WOOD.get(1));
        TextureGen.px(im, 8, 8, WOOD.get(2));
        return im;
    }

    /** The winch frame: dark seasoned beams with iron straps across them and a bolt at each strap. */
    static BufferedImage winchFrame() {
        BufferedImage im = TextureGen.img();
        Random r = new Random(602);
        TextureGen.grain(im, DARK, r, 0, 0, 16, 16, 3, true);
        for (int x = 0; x < 16; x += 8) {
            for (int y = 0; y < 16; y++) {
                TextureGen.px(im, x, y, DARK.get(1));
                TextureGen.px(im, x + 1, y, DARK.get(5));
            }
        }
        for (int y : new int[] {3, 11}) {
            for (int x = 0; x < 16; x++) {
                TextureGen.px(im, x, y, IRON.get(4));
                TextureGen.px(im, x, y + 1, IRON.get(2));
            }
            for (int x : new int[] {2, 10}) {
                TextureGen.px(im, x, y, IRON.get(5));
                TextureGen.px(im, x + 1, y, IRON.get(4));
                TextureGen.px(im, x, y + 1, IRON.get(3));
            }
        }
        return im;
    }

    /** Rope wound round the drum: coils run across the tile, each twisted on a slant, one lay over the next. */
    static BufferedImage winchDrum() {
        BufferedImage im = TextureGen.img();
        for (int y = 0; y < 16; y++) {
            int coil = y % 4;
            for (int x = 0; x < 16; x++) {
                int twist = (x + y / 4 * 3) % 4;
                int step = switch (coil) {
                    case 0 -> 5;
                    case 1 -> twist < 2 ? 4 : 3;
                    case 2 -> twist < 2 ? 3 : 2;
                    default -> 1;
                };
                TextureGen.px(im, x, y, ROPE.get(step));
            }
        }
        return im;
    }

    // ---------------------------------------------------------------- item

    /** The harness: a leather collar with two brass hames, and the traces trailing away from it. */
    static BufferedImage harnessItem() {
        BufferedImage im = TextureGen.img();
        // Collar: a ring, two pixels thick, seen from the front.
        double cx = 7.0, cy = 6.5;
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double dx = (x + 0.5 - cx) / 5.2, dy = (y + 0.5 - cy) / 5.6;
                double d = Math.sqrt(dx * dx + dy * dy);
                if (d > 1.0 || d < 0.58) continue;
                int step = dx + dy < -0.5 ? 5 : dx + dy < 0.2 ? 4 : dx + dy < 0.8 ? 3 : 2;
                TextureGen.px(im, x, y, LEATHER.get(step));
            }
        // Hames: brass stripes down each side of the collar.
        for (int y = 3; y <= 9; y++) {
            TextureGen.px(im, 3, y, BRASS.get(y < 6 ? 5 : 4));
            TextureGen.px(im, 11, y, BRASS.get(y < 6 ? 4 : 3));
        }
        TextureGen.px(im, 7, 0, BRASS.get(5));
        TextureGen.px(im, 8, 0, BRASS.get(4));
        // Traces: two straps from the collar to a brass ring at the lower right.
        int[][] strapA = {{10, 10}, {11, 11}, {12, 12}, {13, 13}};
        int[][] strapB = {{9, 11}, {10, 12}, {11, 13}, {12, 14}};
        for (int[] p : strapA) TextureGen.px(im, p[0], p[1], LEATHER.get(4));
        for (int[] p : strapB) TextureGen.px(im, p[0], p[1], LEATHER.get(3));
        TextureGen.px(im, 13, 14, BRASS.get(5));
        TextureGen.px(im, 14, 14, BRASS.get(4));
        TextureGen.px(im, 13, 15, BRASS.get(3));
        TextureGen.px(im, 14, 15, BRASS.get(2));
        // Buckle on the left of the collar.
        TextureGen.px(im, 2, 11, IRON.get(4));
        TextureGen.px(im, 3, 11, IRON.get(3));
        TextureGen.px(im, 2, 12, IRON.get(3));
        TextureGen.px(im, 3, 12, IRON.get(1));
        return TextureGen.outline(im);
    }

    // ---------------------------------------------------------------- entity

    private static void strap(BufferedImage im, int x0, int y0, int w, int h, int litStep) {
        for (int y = y0; y < y0 + h; y++)
            for (int x = x0; x < x0 + w; x++) {
                int step = litStep;
                if (w > h ? y == y0 + h - 1 : x == x0 + w - 1) step = Math.max(1, litStep - 2);
                else if (w > h ? y == y0 : x == x0) step = Math.min(5, litStep + 1);
                TextureGen.px(im, x, y, LEATHER.get(step));
            }
    }

    private static void ring(BufferedImage im, int x, int y) {
        TextureGen.px(im, x, y, BRASS.get(5));
        TextureGen.px(im, x + 1, y, BRASS.get(4));
        TextureGen.px(im, x, y + 1, BRASS.get(3));
        TextureGen.px(im, x + 1, y + 1, BRASS.get(2));
    }

    /**
     * The harness overlay, 64 x 64 in the vanilla horse layout: a girth strap round the belly, a breast strap and two
     * traces along the flanks, a collar at the base of the neck and a bridle on the head. The rest stays clear so the
     * coat shows through.
     */
    static BufferedImage harnessSheet() {
        BufferedImage im = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        // Body box: u 0, v 32, 10 wide, 10 high, 22 long. West (0,54) and east (32,54) are 22 x 10 with the front at
        // u 22 and 32; the top (22,32) has the front at the bottom edge, the underside (32,32) at the top.
        // Girth strap, two wide, 7 behind the front.
        strap(im, 13, 54, 2, 10, 3);
        strap(im, 39, 54, 2, 10, 4);
        strap(im, 22, 45, 10, 2, 5);
        strap(im, 32, 39, 10, 2, 2);
        // Backband just behind it, on the back and the sides.
        strap(im, 22, 40, 10, 1, 4);
        strap(im, 9, 54, 1, 5, 3);
        strap(im, 44, 54, 1, 5, 4);
        // Breast strap across the chest and round the shoulders.
        strap(im, 22, 58, 10, 2, 4);
        strap(im, 20, 54, 2, 10, 3);
        strap(im, 32, 54, 2, 10, 4);
        // Traces: one line down each flank from the breast strap back to the girth, with a ring at each end.
        for (int u = 15; u <= 21; u++) TextureGen.px(im, u, 57, LEATHER.get(3));
        for (int u = 34; u <= 40; u++) TextureGen.px(im, u, 57, LEATHER.get(4));
        ring(im, 17, 56);
        ring(im, 36, 56);
        // Neck box: u 0, v 35, 4 wide, 12 high, 7 deep. West (0,42) 7 x 12, north (7,42) 4 x 12, east (11,42) 7 x 12,
        // south (18,42) 4 x 12. Collar at the foot of the neck: three rows.
        strap(im, 0, 49, 22, 3, 4);
        strap(im, 0, 49, 22, 1, 5);
        for (int x : new int[] {2, 12}) {
            for (int y = 44; y <= 48; y++) TextureGen.px(im, x, y, BRASS.get(y < 47 ? 5 : 4));
        }
        ring(im, 8, 50);
        // Head box: u 0, v 13, 6 wide, 5 high, 7 deep. West (0,20) 7 x 5, north (7,20) 6 x 5, east (13,20) 7 x 5, south (20,20).
        // Browband across the forehead and cheek straps down the sides.
        strap(im, 7, 21, 6, 1, 3);
        strap(im, 0, 21, 7, 1, 3);
        strap(im, 13, 21, 7, 1, 3);
        strap(im, 3, 20, 1, 5, 3);
        strap(im, 16, 20, 1, 5, 4);
        // Muzzle box: u 0, v 25, 4 wide, 5 high, 5 deep. West (0,30) 5 x 5, north (5,30) 4 x 5, east (9,30) 5 x 5.
        strap(im, 0, 32, 18, 2, 3);
        ring(im, 3, 33);
        ring(im, 11, 33);
        return im;
    }

    public static void main(String[] args) throws IOException {
        TextureGen.save("block/hay_rack", hayRack());
        TextureGen.save("block/incline_winch", winchFrame());
        TextureGen.save("block/incline_winch_drum", winchDrum());
        TextureGen.itemsV2 = true;
        TextureGen.save("item/harness", harnessItem());
        TextureGen.itemsV2 = false;
        TextureGen.saveRaw("entity/pony_harness", harnessSheet());
    }

    private PonyTextures() {}
}
