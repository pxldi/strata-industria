import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Random;

/**
 * Textures for the telegraph and the dispatch board (outposts and transport spec 9.1 and 14): the key and sounder
 * parts (wood, brass, iron, coil), the board's slate, frame and chalk, the wire piece, the wire item and the board
 * screen. Run from the repository root:
 * <pre>
 * mkdir -p build/tg &amp;&amp; javac -d build/tg tools/texturegen/TextureGen.java tools/texturegen/TelegraphTextures.java &amp;&amp; java -cp build/tg TelegraphTextures
 * </pre>
 */
public final class TelegraphTextures {
    static final TextureGen.Ramp WOOD = TextureGen.V2.WOOD_V2;
    static final TextureGen.Ramp BRASS = TextureGen.BRASS;
    static final TextureGen.Ramp IRON = TextureGen.WROUGHT_IRON;
    static final TextureGen.Ramp COPPER = TextureGen.COPPER;
    static final TextureGen.Ramp SLATE = TextureGen.SLATE_V2;
    static final TextureGen.Ramp CHALK = TextureGen.ramp(0xffffff, 0x9c9a90, 0xb8b6ac, 0xd2d0c6, 0xe6e4da, 0xf6f4ea);
    static final int WIRE_DARK = 0x24262c, WIRE_MID = 0x34373e, WIRE_LIGHT = 0x4a4e57;

    // ---------------------------------------------------------------- blocks

    /** Horizontal boards with two brass nails: the base of the key and the sounder. */
    static BufferedImage wood() {
        BufferedImage im = TextureGen.img();
        Random r = new Random(2501);
        for (int b = 0; b < 4; b++) {
            TextureGen.grain(im, WOOD, r, 0, b * 4, 16, 4, 3, false);
            for (int x = 0; x < 16; x++) {
                TextureGen.px(im, x, b * 4, WOOD.get(4));
                TextureGen.px(im, x, b * 4 + 3, WOOD.get(1));
            }
        }
        for (int[] nail : new int[][] {{2, 1}, {13, 1}, {2, 9}, {13, 9}}) {
            TextureGen.px(im, nail[0], nail[1], BRASS.get(5));
            TextureGen.px(im, nail[0] + 1, nail[1], BRASS.get(2));
        }
        return im;
    }

    /** Brushed brass with a screw head and a bright edge on the lit side. */
    static BufferedImage brass() {
        BufferedImage im = TextureGen.img();
        Random r = new Random(2502);
        TextureGen.grain(im, BRASS, r, 0, 0, 16, 16, 3, false);
        for (int i = 0; i < 16; i++) {
            TextureGen.px(im, i, 0, BRASS.get(5));
            TextureGen.px(im, 0, i, BRASS.get(4));
            TextureGen.px(im, i, 15, BRASS.get(1));
            TextureGen.px(im, 15, i, BRASS.get(2));
        }
        TextureGen.px(im, 7, 7, BRASS.get(1));
        TextureGen.px(im, 8, 7, BRASS.get(5));
        TextureGen.px(im, 7, 8, BRASS.get(5));
        TextureGen.px(im, 8, 8, BRASS.get(1));
        TextureGen.px(im, 3, 11, BRASS.spec());
        return im;
    }

    /** Dark worked iron: the pivot post, the yoke and the contact. */
    static BufferedImage iron() {
        BufferedImage im = TextureGen.img();
        Random r = new Random(2503);
        TextureGen.grain(im, IRON, r, 0, 0, 16, 16, 2, true);
        for (int i = 0; i < 16; i++) {
            TextureGen.px(im, 0, i, IRON.get(3));
            TextureGen.px(im, 15, i, IRON.get(1));
        }
        TextureGen.px(im, 5, 3, IRON.get(4));
        TextureGen.px(im, 10, 11, IRON.get(4));
        return im;
    }

    /** Enamelled copper winding, turn on turn, with a sheen down the left side. */
    static BufferedImage coil() {
        BufferedImage im = TextureGen.img();
        for (int y = 0; y < 16; y++) {
            boolean gap = y % 3 == 2;
            for (int x = 0; x < 16; x++) {
                int step = gap ? 1 : y % 3 == 0 ? 4 : 3;
                if (!gap && x < 2) step = 5;
                if (!gap && x > 13) step = 2;
                TextureGen.px(im, x, y, COPPER.get(step));
            }
        }
        TextureGen.px(im, 5, 6, COPPER.spec());
        return im;
    }

    /** A school slate: dark, flat grain, faint ruled lines and the ghost of old chalk. */
    static BufferedImage slate() {
        BufferedImage im = TextureGen.img();
        Random r = new Random(2504);
        TextureGen.grain(im, SLATE, r, 0, 0, 16, 16, 2, false);
        for (int y : new int[] {4, 9, 14}) for (int x = 1; x < 15; x++) if ((x + y) % 4 != 0) TextureGen.px(im, x, y, SLATE.get(3));
        for (int i = 0; i < 14; i++) {
            int x = r.nextInt(16), y = r.nextInt(16);
            TextureGen.px(im, x, y, SLATE.get(4));
            TextureGen.px(im, (x + 1) % 16, y, SLATE.get(3));
        }
        return im;
    }

    /** Vertical planks with a lit top and left edge: the board's frame and ledge. */
    static BufferedImage frame() {
        BufferedImage im = TextureGen.img();
        Random r = new Random(2505);
        TextureGen.grain(im, WOOD, r, 0, 0, 16, 16, 3, true);
        for (int i = 0; i < 16; i++) {
            TextureGen.px(im, i, 0, WOOD.get(5));
            TextureGen.px(im, 0, i, WOOD.get(4));
            TextureGen.px(im, i, 15, WOOD.get(1));
            TextureGen.px(im, 15, i, WOOD.get(1));
        }
        for (int x : new int[] {5, 10}) for (int y = 0; y < 16; y++) TextureGen.px(im, x, y, y % 5 == 0 ? WOOD.get(4) : WOOD.get(2));
        TextureGen.px(im, 3, 3, BRASS.get(5));
        TextureGen.px(im, 12, 12, BRASS.get(4));
        return im;
    }

    /** A stick of chalk: pale, a little crumbly. */
    static BufferedImage chalk() {
        BufferedImage im = TextureGen.img();
        Random r = new Random(2506);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int step = x < 4 ? 5 : x > 11 ? 2 : 4;
                if (r.nextInt(7) == 0) step = Math.max(1, step - 1);
                TextureGen.px(im, x, y, CHALK.get(step));
            }
        }
        return im;
    }

    /** The wire as the renderer sees it: 1 px wide, so the first column does the work, with insulating bands. */
    static BufferedImage line() {
        BufferedImage im = TextureGen.img();
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) TextureGen.px(im, x, y, y % 4 < 2 ? WIRE_MID : x == 0 ? WIRE_LIGHT : WIRE_DARK);
        }
        return im;
    }

    // ---------------------------------------------------------------- item

    /** A bobbin of thin dark wire: wooden flanges, a wound barrel and the free end looped out to the lower right. */
    static BufferedImage wireItem() {
        BufferedImage im = TextureGen.img();
        for (int y = 3; y <= 12; y++) {
            for (int x = 3; x <= 11; x++) {
                boolean flange = x <= 4 || x >= 10;
                int step = flange ? (x == 3 || y == 3 ? 4 : 2) : y % 2 == 0 ? 3 : 2;
                if (!flange) {
                    TextureGen.px(im, x, y, y % 2 == 0 ? WIRE_LIGHT : WIRE_DARK);
                } else {
                    TextureGen.px(im, x, y, WOOD.get(step));
                }
            }
        }
        for (int y = 4; y <= 11; y += 7) for (int x = 3; x <= 11; x++) if (x <= 4 || x >= 10) TextureGen.px(im, x, y, WOOD.get(1));
        TextureGen.px(im, 7, 3, WIRE_LIGHT);
        TextureGen.px(im, 12, 11, WIRE_MID);
        TextureGen.px(im, 13, 12, WIRE_MID);
        TextureGen.px(im, 13, 13, WIRE_DARK);
        TextureGen.px(im, 12, 14, WIRE_MID);
        TextureGen.px(im, 11, 14, BRASS.get(4));
        TextureGen.px(im, 10, 14, BRASS.get(3));
        return im;
    }

    // ---------------------------------------------------------------- gui

    /** The board screen: 224 x 196 in a 256 x 256 sheet. A plank frame round a slate with chalk rules. */
    static BufferedImage gui() {
        BufferedImage im = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        int w = 224, h = 196;
        Random r = new Random(2507);
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) im.setRGB(x, y, 0xFF000000 | WOOD.get(3));
        TextureGen.grain(im, WOOD, r, 0, 0, w, h, 3, false);
        for (int x = 0; x < w; x++) {
            TextureGen.px(im, x, 0, WOOD.get(1));
            TextureGen.px(im, x, h - 1, WOOD.get(1));
            TextureGen.px(im, x, 1, WOOD.get(5));
            TextureGen.px(im, x, h - 2, WOOD.get(1));
        }
        for (int y = 0; y < h; y++) {
            TextureGen.px(im, 0, y, WOOD.get(1));
            TextureGen.px(im, w - 1, y, WOOD.get(1));
            if (y > 0 && y < h - 1) {
                TextureGen.px(im, 1, y, WOOD.get(5));
                TextureGen.px(im, w - 2, y, WOOD.get(1));
            }
        }
        int x0 = 7, y0 = 7, x1 = w - 8, y1 = 163;
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                boolean edge = x == x0 || y == y0;
                boolean shade = x == x1 || y == y1;
                TextureGen.px(im, x, y, edge ? 0x14161a : shade ? SLATE.get(3) : SLATE.get(2));
            }
        }
        TextureGen.grain(im, SLATE, r, x0 + 1, y0 + 1, x1 - x0 - 2, y1 - y0 - 2, 2, false);
        // Faint chalk dust where hands have wiped it.
        for (int i = 0; i < 220; i++) {
            int x = x0 + 2 + r.nextInt(x1 - x0 - 4), y = y0 + 2 + r.nextInt(y1 - y0 - 4);
            TextureGen.px(im, x, y, SLATE.get(r.nextInt(3) == 0 ? 5 : 4));
        }
        // A chalk rule under the heading and a faint one between rows.
        for (int x = 12; x < w - 12; x++) TextureGen.px(im, x, 24, CHALK.get(3));
        for (int row = 0; row < 8; row++) {
            int y = 30 + 19 * row + 17;
            if (y >= y1 - 2) break;
            for (int x = 14; x < w - 14; x += 2) TextureGen.px(im, x, y, SLATE.get(4));
        }
        // Four brass screws in the slate corners.
        for (int[] c : new int[][] {{x0 + 3, y0 + 3}, {x1 - 4, y0 + 3}, {x0 + 3, y1 - 4}, {x1 - 4, y1 - 4}}) {
            TextureGen.px(im, c[0], c[1], BRASS.get(5));
            TextureGen.px(im, c[0] + 1, c[1], BRASS.get(3));
            TextureGen.px(im, c[0], c[1] + 1, BRASS.get(3));
            TextureGen.px(im, c[0] + 1, c[1] + 1, BRASS.get(1));
        }
        return im;
    }

    public static void main(String[] args) throws IOException {
        TextureGen.save("block/telegraph_wood", wood());
        TextureGen.save("block/telegraph_brass", brass());
        TextureGen.save("block/telegraph_iron", iron());
        TextureGen.save("block/telegraph_coil", coil());
        TextureGen.save("block/dispatch_slate", slate());
        TextureGen.save("block/dispatch_frame", frame());
        TextureGen.save("block/dispatch_chalk", chalk());
        TextureGen.save("block/telegraph_line", line());
        TextureGen.itemsV2 = true;
        TextureGen.save("item/telegraph_wire", wireItem());
        TextureGen.itemsV2 = false;
        TextureGen.saveRaw("gui/dispatch_board", gui());
    }

    private TelegraphTextures() {}
}
