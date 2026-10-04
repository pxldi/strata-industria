import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Random;

/**
 * Textures for the wooden tramway (outposts and transport spec 5 and 14): the wooden rail with its corner, the tub
 * stop, the tipple, the rail buffer, the mine tub item and its entity sheet, and the stop screen.
 *
 * <p>Run from the repository root:
 * <pre>
 * mkdir -p build/tg &amp;&amp; javac -d build/tg tools/texturegen/TextureGen.java tools/texturegen/TransportTextures.java tools/texturegen/RailTextures.java &amp;&amp; java -cp build/tg RailTextures
 * </pre>
 */
public final class RailTextures {
    static final TextureGen.Ramp WOOD = TextureGen.V2.WOOD_V2;
    static final TextureGen.Ramp SLEEPER = TextureGen.DARK_WOOD;
    static final TextureGen.Ramp IRON = TextureGen.WROUGHT_IRON;
    static final TextureGen.Ramp PAPER = TextureGen.PAPER;
    static final TextureGen.Ramp BRONZE = TextureGen.BRONZE;
    static final int RAIL_X0 = 3, RAIL_X1 = 11;

    // ---------------------------------------------------------------- track

    /** Four sleepers across, two plank rails along, as vanilla rails lie: rails run north to south. */
    static BufferedImage straight() {
        BufferedImage im = TextureGen.img();
        Random r = new Random(501);
        for (int row = 0; row < 4; row++) {
            int y = 1 + row * 4;
            for (int x = 1; x <= 14; x++) {
                boolean end = x == 1 || x == 14;
                TextureGen.px(im, x, y, SLEEPER.get(end ? 2 : 4 - (r.nextInt(5) == 0 ? 1 : 0)));
                TextureGen.px(im, x, y + 1, SLEEPER.get(end ? 1 : 2 + (r.nextInt(6) == 0 ? 1 : 0)));
            }
        }
        for (int rail : new int[] {RAIL_X0, RAIL_X1}) rail(im, rail, r);
        return im;
    }

    /** One plank rail two pixels wide: lit on the left, a dark joint halfway and a treenail on each sleeper. */
    private static void rail(BufferedImage im, int x, Random r) {
        for (int y = 0; y < 16; y++) {
            int lit = y % 8 == 7 ? 1 : 4 + (r.nextInt(7) == 0 ? 1 : 0);
            int body = y % 8 == 7 ? 1 : 3 - (r.nextInt(7) == 0 ? 1 : 0);
            TextureGen.px(im, x, y, WOOD.get(lit));
            TextureGen.px(im, x + 1, y, WOOD.get(body));
            // A shadow on whatever lies to the right of the rail.
            if (TextureGen.opaque(im, x + 2, y)) TextureGen.px(im, x + 2, y, SLEEPER.get(1));
        }
        for (int row = 0; row < 4; row++) TextureGen.px(im, x + 1, 1 + row * 4, WOOD.get(1));
    }

    /** The curve from the south edge to the east edge, centred on the lower right corner of the tile. */
    static BufferedImage corner() {
        BufferedImage im = TextureGen.img();
        Random r = new Random(502);
        double cx = 16, cy = 16;
        double[] sleeperAngles = {15, 45, 75};
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double dx = cx - (x + 0.5), dy = cy - (y + 0.5);
                double d = Math.sqrt(dx * dx + dy * dy);
                double angle = Math.toDegrees(Math.atan2(dy, dx));
                // Sleepers first, as radial strips.
                for (double a : sleeperAngles) {
                    double perp = Math.abs(d * Math.sin(Math.toRadians(angle - a)));
                    double along = d * Math.cos(Math.toRadians(angle - a));
                    if (perp < 1.0 && along > 1.5 && along < 14.8 && d < 15) {
                        boolean end = d < 2.6 || d > 13.6;
                        TextureGen.px(im, x, y, SLEEPER.get(end ? 1 : perp < 0.45 ? 4 : 2));
                    }
                }
            }
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double dx = cx - (x + 0.5), dy = cy - (y + 0.5);
                double d = Math.sqrt(dx * dx + dy * dy);
                double angle = Math.toDegrees(Math.atan2(dy, dx));
                boolean outer = d >= 11.0 && d < 13.0, inner = d >= 3.0 && d < 5.0;
                if (!outer && !inner) continue;
                boolean litSide = outer ? d < 12.0 : d < 4.0;
                boolean joint = Math.floorMod((int) Math.floor(angle / 11.25), 8) == 3;
                TextureGen.px(im, x, y, WOOD.get(joint ? 1 : litSide ? 4 : 3));
            }
        return im;
    }

    /** A stop: the straight rail with a plank brake block lying between the rails, and its lever. */
    static BufferedImage tubStop() {
        BufferedImage im = straight();
        Random r = new Random(503);
        for (int y = 4; y <= 11; y++)
            for (int x = 6; x <= 9; x++) {
                int step = 3;
                if (y == 4 || x == 6) step = 5;
                if (y == 11 || x == 9) step = 2;
                if (step == 3 && r.nextInt(5) == 0) step = 4;
                TextureGen.px(im, x, y, WOOD.get(step));
            }
        // Dark rim round the block, an iron strap across it and the lever end sticking out to the left.
        for (int y = 3; y <= 12; y++) {
            TextureGen.px(im, 5, y, WOOD.get(1));
            TextureGen.px(im, 10, y, WOOD.get(1));
        }
        for (int x = 5; x <= 10; x++) {
            TextureGen.px(im, x, 3, WOOD.get(1));
            TextureGen.px(im, x, 12, WOOD.get(1));
        }
        for (int x = 6; x <= 9; x++) {
            TextureGen.px(im, x, 7, IRON.get(4));
            TextureGen.px(im, x, 8, IRON.get(2));
        }
        TextureGen.px(im, 6, 7, IRON.get(5));
        TextureGen.px(im, 9, 8, IRON.get(1));
        return im;
    }

    /** The tipple: a pivoting plank frame with iron straps, hinged to the sleepers at both ends. */
    static BufferedImage tipple() {
        BufferedImage im = straight();
        Random r = new Random(504);
        for (int[] beam : new int[][] {{2, 3}, {11, 12}}) {
            for (int x = 0; x <= 15; x++) {
                boolean end = x == 0 || x == 15;
                TextureGen.px(im, x, beam[0], WOOD.get(end ? 2 : 5 - (r.nextInt(6) == 0 ? 1 : 0)));
                TextureGen.px(im, x, beam[1], WOOD.get(end ? 1 : 2 + (r.nextInt(6) == 0 ? 1 : 0)));
            }
            // Iron hinge pins at both ends.
            for (int x : new int[] {1, 14}) {
                TextureGen.px(im, x, beam[0], IRON.get(5));
                TextureGen.px(im, x, beam[1], IRON.get(2));
            }
        }
        // A tripping bar down the middle and a latch on it.
        for (int y = 4; y <= 10; y++) {
            TextureGen.px(im, 7, y, IRON.get(4));
            TextureGen.px(im, 8, y, IRON.get(2));
        }
        TextureGen.px(im, 7, 7, IRON.get(5));
        TextureGen.px(im, 8, 8, IRON.get(1));
        return im;
    }

    /** The buffer: the straight rail seen from above with a heavy timber across its end, bolted to the sleeper. */
    static BufferedImage buffer() {
        BufferedImage im = straight();
        return im;
    }

    /** The timber of the buffer, drawn on a plain 16 x 16 face (sides and top of the model). */
    static BufferedImage bufferTimber() {
        BufferedImage im = TextureGen.img();
        Random r = new Random(505);
        TextureGen.grain(im, WOOD, r, 0, 0, 16, 16, 3, false);
        for (int x = 0; x < 16; x++) {
            TextureGen.px(im, x, 0, WOOD.get(5));
            TextureGen.px(im, x, 15, WOOD.get(1));
        }
        for (int y = 0; y < 16; y++) {
            TextureGen.px(im, 0, y, WOOD.get(4));
            TextureGen.px(im, 15, y, WOOD.get(2));
        }
        for (int[] p : new int[][] {{3, 4}, {12, 4}, {3, 11}, {12, 11}}) {
            TextureGen.px(im, p[0], p[1], IRON.get(5));
            TextureGen.px(im, p[0] + 1, p[1] + 1, IRON.get(1));
        }
        return im;
    }

    // ---------------------------------------------------------------- item

    /** The mine tub seen from the side and a little above: a plank tub, two iron bands and two wheels. */
    static BufferedImage tubItem() {
        BufferedImage im = TextureGen.img();
        // Rim.
        for (int x = 2; x <= 13; x++) TextureGen.px(im, x, 4, WOOD.get(5));
        for (int x = 2; x <= 13; x++) TextureGen.px(im, x, 5, WOOD.get(4));
        // Body, narrowing a little toward the floor.
        for (int y = 6; y <= 11; y++) {
            int inset = (y - 6) / 3;
            for (int x = 2 + inset; x <= 13 - inset; x++) {
                int step = (x + y) % 5 == 0 ? 2 : 3;
                if (x == 2 + inset) step = 4;
                if (x == 13 - inset) step = 2;
                TextureGen.px(im, x, y, WOOD.get(step));
            }
        }
        // Plank seams.
        for (int x = 3; x <= 12; x++) TextureGen.px(im, x, 8, WOOD.get(1));
        // Two iron bands.
        for (int y = 4; y <= 11; y++) {
            int inset = y < 6 ? 0 : (y - 6) / 3;
            for (int x : new int[] {5, 10}) {
                if (x < 2 + inset || x > 13 - inset) continue;
                TextureGen.px(im, x, y, IRON.get(y == 4 ? 5 : 4));
                TextureGen.px(im, x + 1, y, IRON.get(2));
            }
        }
        // Wheels.
        for (int x : new int[] {4, 10}) {
            TextureGen.px(im, x, 12, IRON.get(4));
            TextureGen.px(im, x + 1, 12, IRON.get(3));
            TextureGen.px(im, x, 13, IRON.get(3));
            TextureGen.px(im, x + 1, 13, IRON.get(1));
        }
        // A few ore flecks heaped over the rim.
        TextureGen.px(im, 5, 3, TextureGen.ORE_COPPER.get(4));
        TextureGen.px(im, 6, 3, TextureGen.ORE_COPPER.get(3));
        TextureGen.px(im, 7, 2, TextureGen.ORE_COPPER.get(5));
        TextureGen.px(im, 8, 3, TextureGen.ORE_COPPER.get(2));
        TextureGen.px(im, 9, 3, TextureGen.ORE_COPPER.get(4));
        return TextureGen.outline(im);
    }

    // ---------------------------------------------------------------- entity

    /**
     * The tub sheet, 64 x 64: planks above y 54, iron below. Floor, sides and rim boxes read the planks, the bands
     * and wheels read the iron strip.
     */
    static BufferedImage tubSheet() {
        BufferedImage im = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Random r = new Random(506);
        TextureGen.grain(im, WOOD, r, 0, 0, 64, 54, 3, false);
        // Plank joints every four rows, lit under the joint.
        for (int y = 3; y < 54; y += 4)
            for (int x = 0; x < 64; x++) {
                TextureGen.px(im, x, y, WOOD.get(1));
                if (y + 1 < 54) TextureGen.px(im, x, y + 1, WOOD.get(4));
            }
        // Treenails.
        for (int y = 1; y < 54; y += 4)
            for (int x = 4; x < 64; x += 9) TextureGen.px(im, x, y, WOOD.get(1));
        // Iron strip.
        TextureGen.grain(im, IRON, new Random(507), 0, 54, 64, 10, 3, false);
        for (int x = 0; x < 64; x++) {
            TextureGen.px(im, x, 54, IRON.get(1));
            TextureGen.px(im, x, 55, IRON.get(4));
        }
        for (int x = 3; x < 64; x += 8) {
            TextureGen.px(im, x, 57, IRON.get(5));
            TextureGen.px(im, x + 1, 58, IRON.get(1));
        }
        return im;
    }

    // ---------------------------------------------------------------- gui

    /** The stop screen: 176 x 166 in a 256 x 256 sheet. The charter board frame with a paper panel and three field slots. */
    static BufferedImage gui() {
        BufferedImage im = TransportTextures.gui();
        int w = 176;
        // The charter board has a rule at y 39 and a bottom rule; draw the field wells for the rule and the switch.
        for (int[] well : new int[][] {{14, 56, 148, 14}, {14, 98, 148, 14}}) {
            for (int y = well[1]; y < well[1] + well[3]; y++)
                for (int x = well[0]; x < well[0] + well[2]; x++) {
                    boolean top = y == well[1], left = x == well[0];
                    boolean bottom = y == well[1] + well[3] - 1, right = x == well[0] + well[2] - 1;
                    int c = (top || left) ? PAPER.get(1) : (bottom || right) ? PAPER.get(5) : PAPER.get(4);
                    TextureGen.px(im, x, y, c);
                }
        }
        // An iron pull-ring beside the heading.
        TextureGen.px(im, w - 22, 14, IRON.get(4));
        TextureGen.px(im, w - 21, 14, IRON.get(5));
        TextureGen.px(im, w - 22, 15, IRON.get(2));
        TextureGen.px(im, w - 21, 15, IRON.get(2));
        return im;
    }

    public static void main(String[] args) throws IOException {
        TextureGen.save("block/wooden_rail", straight());
        TextureGen.save("block/wooden_rail_corner", corner());
        TextureGen.save("block/tub_stop", tubStop());
        TextureGen.save("block/tipple_rail", tipple());
        TextureGen.save("block/rail_buffer", buffer());
        TextureGen.save("block/rail_buffer_timber", bufferTimber());
        TextureGen.itemsV2 = true;
        TextureGen.save("item/mine_tub", tubItem());
        TextureGen.itemsV2 = false;
        TextureGen.saveRaw("entity/mine_tub", tubSheet());
        TextureGen.saveRaw("gui/tub_stop", gui());
    }

    private RailTextures() {}
}
