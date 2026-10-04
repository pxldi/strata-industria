import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Random;

/**
 * Textures for the aerial ropeway (outposts and transport spec 8 and 14): the drive terminal and the return, the two
 * tower heads, the bull wheel, the cable, the bucket, and the wire rope and bucket items. Run from the repository root:
 * <pre>
 * mkdir -p build/tg &amp;&amp; javac -d build/tg tools/texturegen/TextureGen.java tools/texturegen/RopewayTextures.java &amp;&amp; java -cp build/tg RopewayTextures
 * </pre>
 */
public final class RopewayTextures {
    static final TextureGen.Ramp STEEL = TextureGen.V2.STEEL_V2;
    static final TextureGen.Ramp IRON = TextureGen.WROUGHT_IRON;
    static final TextureGen.Ramp BRASS = TextureGen.BRASS;
    static final TextureGen.Ramp TIMBER = TextureGen.TREATED_WOOD;
    static final int DARK = 0x16171b, DARKER = 0x24262c;

    // ---------------------------------------------------------------- shared pieces

    /** The tile edge: light top and left, dark bottom and right. */
    private static void bevel(BufferedImage im, TextureGen.Ramp ramp) {
        for (int i = 0; i < 16; i++) {
            TextureGen.px(im, i, 0, ramp.get(5));
            TextureGen.px(im, 0, i, ramp.get(4));
            TextureGen.px(im, i, 15, ramp.get(1));
            TextureGen.px(im, 15, i, ramp.get(2));
        }
    }

    /** A rivet: a light pixel with a dark one down and to the right. */
    private static void rivet(BufferedImage im, int x, int y, TextureGen.Ramp ramp) {
        TextureGen.px(im, x, y, ramp.get(5));
        TextureGen.px(im, x + 1, y + 1, ramp.get(1));
    }

    /** Four boards of 4 px across the tile with dark joints. */
    private static void boards(BufferedImage im, TextureGen.Ramp ramp, Random r, boolean vertical) {
        for (int b = 0; b < 4; b++) {
            if (vertical) TextureGen.grain(im, ramp, r, b * 4, 0, 4, 16, 3, true);
            else TextureGen.grain(im, ramp, r, 0, b * 4, 16, 4, 3, false);
            for (int i = 0; i < 16; i++) {
                if (vertical) {
                    TextureGen.px(im, b * 4, i, ramp.get(4));
                    TextureGen.px(im, b * 4 + 3, i, ramp.get(1));
                } else {
                    TextureGen.px(im, i, b * 4, ramp.get(4));
                    TextureGen.px(im, i, b * 4 + 3, ramp.get(1));
                }
            }
        }
    }

    // ---------------------------------------------------------------- blocks

    /** Timber boards with two iron straps: the frame the stations sit in. */
    static BufferedImage timber() {
        BufferedImage im = TextureGen.img();
        boards(im, TIMBER, new Random(2101), false);
        for (int x : new int[] {2, 13}) {
            for (int y = 0; y < 16; y++) {
                TextureGen.px(im, x, y, IRON.get(3));
                TextureGen.px(im, x + 1, y, IRON.get(2));
            }
            TextureGen.px(im, x, 3, IRON.get(5));
            TextureGen.px(im, x, 11, IRON.get(5));
        }
        return im;
    }

    /** Brushed steel casing with a brass band round the drive: the terminal. */
    static BufferedImage terminal() {
        BufferedImage im = TextureGen.img();
        Random r = new Random(2102);
        TextureGen.grain(im, STEEL, r, 0, 0, 16, 16, 3, false);
        bevel(im, STEEL);
        for (int y = 6; y <= 9; y++) {
            for (int x = 1; x < 15; x++) {
                int step = y == 6 ? 5 : y == 9 ? 1 : (x + y) % 3 == 0 ? 4 : 3;
                TextureGen.px(im, x, y, BRASS.get(step));
            }
        }
        for (int x : new int[] {3, 7, 11}) TextureGen.px(im, x, 7, BRASS.spec());
        rivet(im, 2, 2, STEEL);
        rivet(im, 12, 2, STEEL);
        rivet(im, 2, 12, STEEL);
        rivet(im, 12, 12, STEEL);
        return im;
    }

    /** Brushed steel casing with a seam through the middle: the return. */
    static BufferedImage station() {
        BufferedImage im = TextureGen.img();
        Random r = new Random(2103);
        TextureGen.grain(im, STEEL, r, 0, 0, 16, 16, 3, false);
        bevel(im, STEEL);
        for (int x = 1; x < 15; x++) {
            TextureGen.px(im, x, 7, STEEL.get(1));
            TextureGen.px(im, x, 8, STEEL.get(5));
        }
        for (int x : new int[] {3, 7, 11}) rivet(im, x, 3, STEEL);
        for (int x : new int[] {3, 7, 11}) rivet(im, x, 11, STEEL);
        return im;
    }

    /** Vertical treated boards, iron straps and a diagonal brace: a wooden tower head. */
    static BufferedImage woodenTower() {
        BufferedImage im = TextureGen.img();
        boards(im, TIMBER, new Random(2104), true);
        for (int y : new int[] {2, 13}) {
            for (int x = 0; x < 16; x++) {
                TextureGen.px(im, x, y, IRON.get(4));
                TextureGen.px(im, x, y + 1, IRON.get(2));
            }
            for (int x : new int[] {1, 5, 9, 13}) TextureGen.px(im, x, y, IRON.get(5));
        }
        for (int i = 0; i < 8; i++) {
            TextureGen.px(im, 4 + i, 4 + i, TIMBER.get(1));
            TextureGen.px(im, 5 + i, 4 + i, TIMBER.get(4));
        }
        return im;
    }

    /** A lattice of steel flats on black: the steel tower. */
    static BufferedImage steelTower() {
        BufferedImage im = TextureGen.img();
        Random r = new Random(2105);
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) TextureGen.px(im, x, y, (x + y) % 5 == 0 ? DARKER : DARK);
        for (int i = 0; i < 16; i++) {
            for (int w = 0; w < 2; w++) {
                TextureGen.px(im, i, Math.min(15, i + w), STEEL.get(w == 0 ? 4 : 2));
                TextureGen.px(im, 15 - i, Math.min(15, i + w), STEEL.get(w == 0 ? 3 : 2));
            }
        }
        TextureGen.grain(im, STEEL, r, 0, 0, 16, 2, 3, false);
        TextureGen.grain(im, STEEL, r, 0, 14, 16, 2, 3, false);
        TextureGen.grain(im, STEEL, r, 0, 0, 2, 16, 3, true);
        TextureGen.grain(im, STEEL, r, 14, 0, 2, 16, 3, true);
        for (int i = 0; i < 16; i++) {
            TextureGen.px(im, i, 0, STEEL.get(5));
            TextureGen.px(im, 0, i, STEEL.get(5));
            TextureGen.px(im, i, 15, STEEL.get(1));
            TextureGen.px(im, 15, i, STEEL.get(1));
        }
        rivet(im, 2, 2, STEEL);
        rivet(im, 12, 12, STEEL);
        return im;
    }

    /**
     * The bull wheel, seen from above: a grooved iron rim, four spokes and a brass hub. The model maps its top faces
     * straight onto the tile, so the circle sits in the middle; the bottom two rows are the rim seen from the side.
     */
    static BufferedImage wheel() {
        BufferedImage im = TextureGen.img();
        Random r = new Random(2106);
        TextureGen.grain(im, IRON, r, 0, 0, 16, 14, 3, false);
        for (int y = 0; y < 14; y++) {
            for (int x = 0; x < 16; x++) {
                double dx = x - 7.5, dy = y - 7.5, d = Math.sqrt(dx * dx + dy * dy);
                if (d > 5.6) TextureGen.px(im, x, y, IRON.get((x * 7 + y * 3) % 11 == 0 ? 3 : 2));
                else if (d > 4.6) TextureGen.px(im, x, y, STEEL.get(dx + dy < 0 ? 5 : 4));
                else if (d > 3.6) TextureGen.px(im, x, y, DARK);
                else if (Math.abs(dx) < 0.9 || Math.abs(dy) < 0.9) TextureGen.px(im, x, y, STEEL.get(dx + dy < 0 ? 4 : 3));
                else TextureGen.px(im, x, y, DARKER);
                if (d < 1.7) TextureGen.px(im, x, y, BRASS.get(dx + dy < 0 ? 5 : 3));
            }
        }
        for (int x = 0; x < 16; x++) {
            TextureGen.px(im, x, 14, STEEL.get(4));
            TextureGen.px(im, x, 15, STEEL.get(2));
        }
        return im;
    }

    /** Two strands twisted together; 2 px wide, the model uses the left two columns. */
    static BufferedImage cable() {
        BufferedImage im = TextureGen.img();
        for (int y = 0; y < 16; y++) {
            boolean twist = (y / 2) % 2 == 0;
            TextureGen.px(im, 0, y, STEEL.get(twist ? 4 : 2));
            TextureGen.px(im, 1, y, STEEL.get(twist ? 2 : 4));
            for (int x = 2; x < 16; x++) TextureGen.px(im, x, y, STEEL.get(3));
        }
        return im;
    }

    /** The bucket's plate: riveted iron, a band near the lip, and a dark inside on the right half for the top face. */
    static BufferedImage bucket() {
        BufferedImage im = TextureGen.img();
        Random r = new Random(2107);
        TextureGen.grain(im, IRON, r, 0, 0, 8, 16, 3, true);
        for (int y = 0; y < 16; y++) {
            TextureGen.px(im, 0, y, IRON.get(4));
            TextureGen.px(im, 7, y, IRON.get(1));
        }
        for (int x = 0; x < 8; x++) {
            TextureGen.px(im, x, 1, STEEL.get(5));
            TextureGen.px(im, x, 2, STEEL.get(2));
            TextureGen.px(im, x, 5, IRON.get(1));
        }
        for (int x : new int[] {2, 5}) rivet(im, x, 3, IRON);
        for (int y = 0; y < 16; y++) {
            for (int x = 8; x < 16; x++) {
                boolean rim = x < 9 || x > 14 || y < 1 || y > 6;
                TextureGen.px(im, x, y, y > 7 ? IRON.get(3) : rim ? IRON.get(5) : (x + y) % 3 == 0 ? DARKER : DARK);
            }
        }
        return im;
    }

    // ---------------------------------------------------------------- items

    private static void ring(BufferedImage im, double cx, double cy, double rx, double ry, TextureGen.Ramp ramp, int turns) {
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                double dx = (x - cx) / rx, dy = (y - cy) / ry, d = Math.sqrt(dx * dx + dy * dy);
                if (d > 1.0 || d < 0.45) continue;
                // Strands: alternate light and dark with the angle, so the coil reads as twisted wire.
                double angle = Math.atan2(dy, dx);
                boolean strand = (int) Math.floor((angle + Math.PI) / (2 * Math.PI) * turns) % 2 == 0;
                int step = d > 0.85 ? 2 : d < 0.55 ? 2 : strand ? 4 : 3;
                if (dy < 0 && d > 0.7 && strand) step = 5;
                TextureGen.px(im, x, y, ramp.get(step));
            }
        }
    }

    /** A coil of wire rope with its end hanging: grey strands, a glint on the lit side. */
    static BufferedImage wireRope() {
        BufferedImage im = TextureGen.img();
        ring(im, 7.5, 7.0, 6.2, 5.0, STEEL, 14);
        // The inside of the coil is open; the free end drops from the lower right.
        for (int i = 0; i < 4; i++) {
            TextureGen.px(im, 11 + i / 2, 11 + i, STEEL.get(i % 2 == 0 ? 4 : 2));
            TextureGen.px(im, 12 + i / 2, 11 + i, STEEL.get(3));
        }
        TextureGen.px(im, 12, 15, BRASS.get(4));
        TextureGen.px(im, 13, 15, BRASS.get(2));
        TextureGen.px(im, 4, 3, STEEL.spec());
        TextureGen.px(im, 5, 2, STEEL.spec());
        return im;
    }

    /** The bucket as an item: a squared iron pail with a bail, hung from a small carriage. */
    static BufferedImage bucketItem() {
        BufferedImage im = TextureGen.img();
        // Carriage: two small sheaves over the bail.
        for (int x : new int[] {5, 9}) {
            TextureGen.px(im, x, 1, IRON.get(4));
            TextureGen.px(im, x + 1, 1, IRON.get(2));
            TextureGen.px(im, x, 2, IRON.get(3));
            TextureGen.px(im, x + 1, 2, IRON.get(1));
        }
        for (int x = 5; x <= 10; x++) TextureGen.px(im, x, 0, STEEL.get(x == 5 ? 5 : 4));
        // Bail.
        for (int y = 3; y <= 5; y++) {
            TextureGen.px(im, 4 + (y == 3 ? 1 : 0), y, IRON.get(4));
            TextureGen.px(im, 11 - (y == 3 ? 1 : 0), y, IRON.get(2));
        }
        // Body: wider at the lip than the base.
        for (int y = 6; y <= 13; y++) {
            int inset = (y - 6) / 4;
            for (int x = 3 + inset; x <= 12 - inset; x++) {
                int step = x == 3 + inset ? 4 : x == 12 - inset ? 1 : 3;
                if (y == 6) step = 5;
                if (y == 13) step = 1;
                TextureGen.px(im, x, y, IRON.get(step));
            }
        }
        for (int x = 4; x <= 11; x++) TextureGen.px(im, x, 7, DARKER);
        for (int x : new int[] {5, 7, 9, 10}) TextureGen.px(im, x, 9, STEEL.get(5));
        for (int x = 4; x <= 11; x++) TextureGen.px(im, x, 10, IRON.get(2));
        TextureGen.px(im, 4, 7, STEEL.spec());
        return im;
    }

    public static void main(String[] args) throws IOException {
        TextureGen.save("block/ropeway_timber", timber());
        TextureGen.save("block/ropeway_terminal", terminal());
        TextureGen.save("block/ropeway_return", station());
        TextureGen.save("block/wooden_ropeway_tower", woodenTower());
        TextureGen.save("block/steel_ropeway_tower", steelTower());
        TextureGen.save("block/ropeway_wheel", wheel());
        TextureGen.save("block/ropeway_cable", cable());
        TextureGen.save("block/ropeway_bucket", bucket());
        TextureGen.itemsV2 = true;
        TextureGen.save("item/wire_rope", wireRope());
        TextureGen.save("item/ropeway_bucket", bucketItem());
        TextureGen.itemsV2 = false;
    }

    private RopewayTextures() {}
}
