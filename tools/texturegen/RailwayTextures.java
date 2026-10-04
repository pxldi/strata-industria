import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Random;

/**
 * Textures for the steel track and wagons (outposts and transport spec 7.1, 7.2 and 14): steel track with its
 * corner, the station track, the steel buffer, the wagon fluid port in both modes, the three wagon items and the
 * three wagon sheets.
 *
 * <p>Run from the repository root:
 * <pre>
 * mkdir -p build/tg &amp;&amp; javac -d build/tg tools/texturegen/TextureGen.java tools/texturegen/TransportTextures.java tools/texturegen/RailwayTextures.java &amp;&amp; java -cp build/tg RailwayTextures
 * </pre>
 */
public final class RailwayTextures {
    static final TextureGen.Ramp STEEL = TextureGen.V2.STEEL_V2;
    static final TextureGen.Ramp TAR = TextureGen.ramp(0, 0x16151a, 0x22201f, 0x302c28, 0x433d36, 0x594f44);
    static final TextureGen.Ramp IRON = TextureGen.WROUGHT_IRON;
    static final TextureGen.Ramp BRASS = TextureGen.BRASS;
    static final TextureGen.Ramp WOOD = TextureGen.V2.WOOD_V2;
    static final TextureGen.Ramp OXIDE = TextureGen.ramp(0, 0x2e1612, 0x4a241c, 0x6e3426, 0x8e4632, 0xac5c42);
    static final TextureGen.Ramp BLACK = TextureGen.ramp(0, 0x14151a, 0x1f2128, 0x2c2f38, 0x3c404b, 0x525764);
    static final TextureGen.Ramp BLUE = TextureGen.ramp(0, 0x1a2c4a, 0x264570, 0x34609a, 0x4c82c4, 0x7cb0e8);
    static final TextureGen.Ramp WHITE = TextureGen.ramp(0, 0x7a7e86, 0x9ea2aa, 0xc2c6cc, 0xdee0e4, 0xf2f3f5);
    static final TextureGen.Ramp RED = TextureGen.ramp(0, 0x4a1612, 0x7a241c, 0xa8342a, 0xc8483a, 0xe27060);
    static final int RAIL_X0 = 3, RAIL_X1 = 11;

    // ---------------------------------------------------------------- track

    /** Four tarred sleepers across, two steel rails along with a fishplate joint halfway: rails run north to south. */
    static BufferedImage straight() {
        BufferedImage im = TextureGen.img();
        Random r = new Random(701);
        for (int row = 0; row < 4; row++) {
            int y = 1 + row * 4;
            for (int x = 1; x <= 14; x++) {
                boolean end = x == 1 || x == 14;
                TextureGen.px(im, x, y, TAR.get(end ? 2 : 4 - (r.nextInt(5) == 0 ? 1 : 0)));
                TextureGen.px(im, x, y + 1, TAR.get(end ? 1 : 2 + (r.nextInt(6) == 0 ? 1 : 0)));
            }
        }
        for (int rail : new int[] {RAIL_X0, RAIL_X1}) rail(im, rail, r);
        return im;
    }

    /** One steel rail two pixels wide: the head lit on its left, a fishplate with two bolts at the joint, a spike on each sleeper. */
    private static void rail(BufferedImage im, int x, Random r) {
        for (int y = 0; y < 16; y++) {
            boolean plate = y >= 6 && y <= 9;
            TextureGen.px(im, x, y, STEEL.get(plate ? 4 : 5 - (r.nextInt(8) == 0 ? 1 : 0)));
            TextureGen.px(im, x + 1, y, STEEL.get(plate ? 2 : 3));
            if (TextureGen.opaque(im, x + 2, y)) TextureGen.px(im, x + 2, y, TAR.get(1));
            if (TextureGen.opaque(im, x - 1, y) && !plate) TextureGen.px(im, x - 1, y, TAR.get(2));
        }
        TextureGen.px(im, x, 6, STEEL.get(1));
        TextureGen.px(im, x, 9, STEEL.get(1));
        TextureGen.px(im, x + 1, 7, STEEL.get(5));
        TextureGen.px(im, x + 1, 8, STEEL.get(5));
        for (int row = 0; row < 4; row++) TextureGen.px(im, x + 1, 1 + row * 4, IRON.get(1));
    }

    /** The curve from the south edge to the east edge, centred on the lower right corner of the tile. */
    static BufferedImage corner() {
        BufferedImage im = TextureGen.img();
        Random r = new Random(702);
        double cx = 16, cy = 16;
        double[] sleeperAngles = {15, 45, 75};
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double dx = cx - (x + 0.5), dy = cy - (y + 0.5);
                double d = Math.sqrt(dx * dx + dy * dy);
                double angle = Math.toDegrees(Math.atan2(dy, dx));
                for (double a : sleeperAngles) {
                    double perp = Math.abs(d * Math.sin(Math.toRadians(angle - a)));
                    double along = d * Math.cos(Math.toRadians(angle - a));
                    if (perp < 1.0 && along > 1.5 && along < 14.8 && d < 15) {
                        boolean end = d < 2.6 || d > 13.6;
                        TextureGen.px(im, x, y, TAR.get(end ? 1 : perp < 0.45 ? 4 : 2));
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
                TextureGen.px(im, x, y, STEEL.get(joint ? 1 : litSide ? 5 : 3));
            }
        return im;
    }

    /** A station: the straight track with a brass-edged platen between the rails and the lever stub of the stop. */
    static BufferedImage station() {
        BufferedImage im = straight();
        Random r = new Random(703);
        for (int y = 3; y <= 12; y++)
            for (int x = 5; x <= 10; x++) {
                int step = 3;
                if (y == 3 || x == 5) step = 5;
                if (y == 12 || x == 10) step = 1;
                else if (r.nextInt(6) == 0) step = 4;
                TextureGen.px(im, x, y, STEEL.get(step));
            }
        for (int y = 4; y <= 11; y++) {
            TextureGen.px(im, 6, y, BRASS.get(5));
            TextureGen.px(im, 9, y, BRASS.get(2));
        }
        for (int x = 6; x <= 9; x++) {
            TextureGen.px(im, x, 4, BRASS.get(5));
            TextureGen.px(im, x, 11, BRASS.get(2));
        }
        // A brass gear stamped in the middle of it, as the recipe has one.
        for (int[] p : new int[][] {{7, 6}, {8, 6}, {6, 7}, {9, 7}, {6, 8}, {9, 8}, {7, 9}, {8, 9}}) TextureGen.px(im, p[0], p[1], BRASS.get(4));
        TextureGen.px(im, 7, 7, IRON.get(1));
        TextureGen.px(im, 8, 7, IRON.get(2));
        TextureGen.px(im, 7, 8, IRON.get(2));
        TextureGen.px(im, 8, 8, IRON.get(1));
        return im;
    }

    /** The beam of the buffer: dark steel with rivets and a red-lead band down the face. */
    static BufferedImage bufferBeam() {
        BufferedImage im = TextureGen.img();
        Random r = new Random(704);
        TextureGen.grain(im, STEEL, r, 0, 0, 16, 16, 3, false);
        for (int x = 0; x < 16; x++) {
            TextureGen.px(im, x, 0, STEEL.get(5));
            TextureGen.px(im, x, 15, STEEL.get(1));
        }
        for (int y = 0; y < 16; y++) {
            TextureGen.px(im, 0, y, STEEL.get(4));
            TextureGen.px(im, 15, y, STEEL.get(2));
        }
        // Red band, hazard white stripe above it.
        for (int x = 1; x < 15; x++) {
            TextureGen.px(im, x, 6, RED.get(4));
            TextureGen.px(im, x, 7, RED.get(3));
            TextureGen.px(im, x, 8, RED.get(2));
            TextureGen.px(im, x, 5, WHITE.get(3));
        }
        for (int x : new int[] {3, 8, 12}) {
            TextureGen.px(im, x, 2, STEEL.get(5));
            TextureGen.px(im, x + 1, 3, STEEL.get(1));
            TextureGen.px(im, x, 12, STEEL.get(5));
            TextureGen.px(im, x + 1, 13, STEEL.get(1));
        }
        return im;
    }

    // ---------------------------------------------------------------- the fluid port

    /** A steel box, riveted at the corners, with a round brass flange and a tab of {@code tab} colour. */
    static BufferedImage port(TextureGen.Ramp tab, boolean arrowOut) {
        BufferedImage im = TextureGen.img();
        Random r = new Random(705);
        TextureGen.grain(im, STEEL, r, 0, 0, 16, 16, 3, false);
        for (int i = 0; i < 16; i++) {
            TextureGen.px(im, i, 0, STEEL.get(5));
            TextureGen.px(im, 0, i, STEEL.get(4));
            TextureGen.px(im, i, 15, STEEL.get(1));
            TextureGen.px(im, 15, i, STEEL.get(1));
        }
        for (int[] p : new int[][] {{2, 2}, {13, 2}, {2, 13}, {13, 13}}) {
            TextureGen.px(im, p[0], p[1], STEEL.get(5));
            TextureGen.px(im, p[0] + (p[0] > 7 ? -1 : 1), p[1] + (p[1] > 7 ? -1 : 1), STEEL.get(1));
        }
        // The flange: a brass ring round a dark bore.
        double cx = 7.5, cy = 7.5;
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double d = Math.hypot(x - cx, y - cy);
                if (d <= 2.4) TextureGen.px(im, x, y, TAR.get(1));
                else if (d <= 3.4) TextureGen.px(im, x, y, TAR.get(2));
                else if (d <= 5.4) TextureGen.px(im, x, y, BRASS.get(x + y < 15 ? 5 : x + y < 17 ? 4 : 2));
                else if (d <= 6.0) TextureGen.px(im, x, y, BRASS.get(1));
            }
        // The tab at the top: where the fluid goes, an arrow in the tab's colour.
        for (int y = 1; y <= 3; y++)
            for (int x = 6; x <= 9; x++) TextureGen.px(im, x, y, tab.get(y == 1 ? 5 : y == 2 ? 4 : 2));
        TextureGen.px(im, 6, 1, tab.get(1));
        TextureGen.px(im, 9, 3, tab.get(1));
        // Arrow pointing into the bore (fill) or away from it (empty).
        int tip = arrowOut ? 11 : 4;
        TextureGen.px(im, 7, arrowOut ? 11 : 4, tab.get(5));
        TextureGen.px(im, 8, arrowOut ? 11 : 4, tab.get(5));
        return im;
    }

    // ---------------------------------------------------------------- items

    private static void wheel(BufferedImage im, int x, int y) {
        TextureGen.px(im, x, y, IRON.get(4));
        TextureGen.px(im, x + 1, y, IRON.get(3));
        TextureGen.px(im, x, y + 1, IRON.get(3));
        TextureGen.px(im, x + 1, y + 1, IRON.get(1));
    }

    private static void frame(BufferedImage im, int y) {
        for (int x = 1; x <= 14; x++) {
            TextureGen.px(im, x, y, IRON.get(4));
            TextureGen.px(im, x, y + 1, IRON.get(2));
        }
        TextureGen.px(im, 0, y, IRON.get(3));
        TextureGen.px(im, 15, y, IRON.get(3));
        wheel(im, 3, y + 2);
        wheel(im, 11, y + 2);
    }

    /** The ore wagon from the side: a riveted oxide-red steel body, a heap of ore over the rim. */
    static BufferedImage oreItem() {
        BufferedImage im = TextureGen.img();
        for (int x = 2; x <= 13; x++) {
            TextureGen.px(im, x, 5, OXIDE.get(5));
            TextureGen.px(im, x, 6, OXIDE.get(4));
        }
        for (int y = 7; y <= 10; y++) {
            for (int x = 2; x <= 13; x++) {
                int step = (x + y) % 6 == 0 ? 2 : 3;
                if (x == 2) step = 4;
                if (x == 13) step = 2;
                TextureGen.px(im, x, y, OXIDE.get(step));
            }
        }
        for (int x = 3; x <= 12; x += 3) TextureGen.px(im, x, 8, OXIDE.get(5));
        for (int x = 2; x <= 13; x++) TextureGen.px(im, x, 10, OXIDE.get(1));
        // Ore heaped above the rim.
        int[][] heap = {{4, 4, 4}, {5, 3, 5}, {6, 3, 3}, {7, 2, 5}, {8, 2, 4}, {9, 3, 5}, {10, 3, 3}, {11, 4, 4}, {5, 4, 3}, {6, 4, 5}, {8, 3, 3}, {9, 4, 4}, {10, 4, 5}, {7, 3, 4}};
        for (int[] h : heap) TextureGen.px(im, h[0], h[1], TextureGen.ORE_HEMATITE.get(h[2]));
        frame(im, 11);
        return TextureGen.outline(im);
    }

    /** The tank wagon from the side: a black drum with a brass dome and two straps. */
    static BufferedImage tankItem() {
        BufferedImage im = TextureGen.img();
        for (int y = 4; y <= 10; y++) {
            int inset = (y == 4 || y == 10) ? 1 : 0;
            for (int x = 2 + inset; x <= 13 - inset; x++) {
                int step = y <= 5 ? 5 : y <= 7 ? 4 : y <= 9 ? 3 : 2;
                if (x == 2 + inset) step = Math.min(5, step + 1);
                if (x == 13 - inset) step = Math.max(1, step - 1);
                TextureGen.px(im, x, y, BLACK.get(step));
            }
        }
        for (int y = 4; y <= 10; y++) {
            TextureGen.px(im, 5, y, IRON.get(4));
            TextureGen.px(im, 10, y, IRON.get(4));
        }
        // Brass dome and its cap.
        for (int x = 6; x <= 9; x++) TextureGen.px(im, x, 3, BRASS.get(x == 6 ? 5 : x == 9 ? 2 : 4));
        TextureGen.px(im, 7, 2, BRASS.get(5));
        TextureGen.px(im, 8, 2, BRASS.get(3));
        // A drop-shaped gauge mark on the drum.
        TextureGen.px(im, 7, 7, BLUE.get(5));
        TextureGen.px(im, 8, 8, BLUE.get(3));
        frame(im, 11);
        return TextureGen.outline(im);
    }

    /** The flat wagon from the side: a timber deck on the frame with a stake at each end and a small crate on it. */
    static BufferedImage flatItem() {
        BufferedImage im = TextureGen.img();
        for (int x = 1; x <= 14; x++) {
            TextureGen.px(im, x, 9, WOOD.get(5));
            TextureGen.px(im, x, 10, WOOD.get(3));
        }
        for (int x = 1; x <= 14; x += 4) TextureGen.px(im, x, 10, WOOD.get(1));
        for (int y = 7; y <= 8; y++) {
            TextureGen.px(im, 1, y, IRON.get(4));
            TextureGen.px(im, 14, y, IRON.get(3));
        }
        // The load: one grey block, lit on top.
        for (int y = 4; y <= 8; y++)
            for (int x = 5; x <= 10; x++) {
                int step = y == 4 ? 5 : x == 5 ? 4 : x == 10 ? 2 : 3;
                TextureGen.px(im, x, y, TextureGen.GRANITE.get(step));
            }
        TextureGen.px(im, 7, 6, TextureGen.GRANITE.get(2));
        TextureGen.px(im, 8, 7, TextureGen.GRANITE.get(2));
        frame(im, 11);
        return TextureGen.outline(im);
    }

    // ---------------------------------------------------------------- entity sheets

    /**
     * A wagon sheet, 64 x 64: paint above y 54, an iron strip at y 54 to 57, dark steel below it at x 31 and down, brass
     * at x 32 and up. {@code planks} lays timber joints in the paint instead of riveted plates.
     */
    static BufferedImage sheet(TextureGen.Ramp paint, long seed, boolean planks) {
        BufferedImage im = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Random r = new Random(seed);
        TextureGen.grain(im, paint, r, 0, 0, 64, 54, 3, false);
        if (planks) {
            for (int y = 3; y < 54; y += 4)
                for (int x = 0; x < 64; x++) {
                    TextureGen.px(im, x, y, paint.get(1));
                    if (y + 1 < 54) TextureGen.px(im, x, y + 1, paint.get(4));
                }
            for (int y = 1; y < 54; y += 4)
                for (int x = 4; x < 64; x += 9) TextureGen.px(im, x, y, paint.get(1));
        } else {
            // Plate seams every eight rows and rivet heads along them every four columns.
            for (int y = 7; y < 54; y += 8)
                for (int x = 0; x < 64; x++) {
                    TextureGen.px(im, x, y, paint.get(1));
                    if (y + 1 < 54) TextureGen.px(im, x, y + 1, paint.get(5));
                }
            for (int y = 5; y < 54; y += 8)
                for (int x = 2; x < 64; x += 4) {
                    TextureGen.px(im, x, y, paint.get(5));
                    TextureGen.px(im, x + 1, y + 1, paint.get(1));
                }
        }
        TextureGen.grain(im, IRON, new Random(seed + 1), 0, 54, 64, 4, 3, false);
        for (int x = 0; x < 64; x++) {
            TextureGen.px(im, x, 54, IRON.get(5));
            TextureGen.px(im, x, 57, IRON.get(1));
        }
        TextureGen.grain(im, TAR, new Random(seed + 2), 0, 58, 32, 6, 3, false);
        TextureGen.grain(im, BRASS, new Random(seed + 3), 32, 58, 32, 6, 3, false);
        for (int x = 32; x < 64; x++) {
            TextureGen.px(im, x, 58, BRASS.get(5));
            TextureGen.px(im, x, 63, BRASS.get(2));
        }
        return im;
    }

    public static void main(String[] args) throws IOException {
        TextureGen.save("block/steel_track", straight());
        TextureGen.save("block/steel_track_corner", corner());
        TextureGen.save("block/station_track", station());
        TextureGen.save("block/steel_buffer", straight());
        TextureGen.save("block/steel_buffer_beam", bufferBeam());
        TextureGen.save("block/wagon_fluid_port_load", port(BLUE, false));
        TextureGen.save("block/wagon_fluid_port_unload", port(WHITE, true));
        TextureGen.itemsV2 = true;
        TextureGen.save("item/ore_wagon", oreItem());
        TextureGen.save("item/tank_wagon", tankItem());
        TextureGen.save("item/flat_wagon", flatItem());
        TextureGen.itemsV2 = false;
        TextureGen.saveRaw("entity/ore_wagon", sheet(OXIDE, 711, false));
        TextureGen.saveRaw("entity/tank_wagon", sheet(BLACK, 712, false));
        TextureGen.saveRaw("entity/flat_wagon", sheet(WOOD, 713, true));
    }

    private RailwayTextures() {}
}
