import java.awt.image.BufferedImage;
import java.io.IOException;
import dev.strataindustria.client.rail.TramAtlas;
import java.util.Random;

/**
 * Textures for the electric tram and the trolley wire (outposts and transport spec 9.2, 9.3 and 14): the tram's
 * cream and maroon sheet and item icon, the wire's spool, the bracket's arm and the copper of the wire itself.
 *
 * <p>The tram sheet is laid out by {@code dev.strataindustria.client.rail.TramAtlas}, which the model reads too.
 * Run from the repository root:
 * <pre>
 * mkdir -p build/tg &amp;&amp; javac -d build/tg tools/texturegen/TextureGen.java src/main/java/dev/strataindustria/client/rail/LocomotiveAtlas.java src/main/java/dev/strataindustria/client/rail/TramAtlas.java tools/texturegen/LocomotiveTextures.java tools/texturegen/TramTextures.java &amp;&amp; java -cp build/tg TramTextures
 * </pre>
 */
public final class TramTextures {
    static final TextureGen.Ramp CREAM = TextureGen.ramp(0, 0x7a7058, 0xa89d7c, 0xcdc19a, 0xe6dcb8, 0xf4ecd0);
    static final TextureGen.Ramp MAROON = TextureGen.ramp(0, 0x2e0e12, 0x4a161c, 0x6a2028, 0x8a2e36, 0xa8424a);
    static final TextureGen.Ramp GLASS = TextureGen.ramp(0, 0x0a1624, 0x122238, 0x1c3552, 0x2c5074, 0x5a8cb4);
    static final TextureGen.Ramp SOOT = LocomotiveTextures.SOOT;
    static final TextureGen.Ramp IRON = LocomotiveTextures.IRON;
    static final TextureGen.Ramp STEEL = LocomotiveTextures.STEEL;
    static final TextureGen.Ramp BRASS = LocomotiveTextures.BRASS;
    static final TextureGen.Ramp AMBER = LocomotiveTextures.AMBER;
    static final TextureGen.Ramp WHITE = LocomotiveTextures.WHITE;
    static final TextureGen.Ramp COPPER = TextureGen.COPPER;
    static final TextureGen.Ramp CERAMIC = TextureGen.CERAMIC;

    // ---------------------------------------------------------------- the tram's sheet

    private static void fill(BufferedImage im, TextureGen.Ramp ramp, Random r, int x, int y, int w, int h, boolean lit) {
        if (w <= 0 || h <= 0) return;
        TextureGen.grain(im, ramp, r, x, y, w, h, 3, w < h);
        if (w >= 3 && h >= 3) {
            for (int i = 0; i < w; i++) {
                TextureGen.px(im, x + i, y, ramp.get(lit ? 5 : 4));
                TextureGen.px(im, x + i, y + h - 1, ramp.get(1));
            }
            for (int j = 0; j < h; j++) {
                TextureGen.px(im, x, y + j, ramp.get(4));
                TextureGen.px(im, x + w - 1, y + j, ramp.get(2));
            }
        }
    }

    /** Paints the net of one part: top, bottom, the four sides, as the model lays them out. */
    private static void net(BufferedImage im, String part, TextureGen.Ramp ramp, long seed) {
        var c = TramAtlas.cell(part);
        Random r = new Random(seed);
        fill(im, ramp, r, c.u() + c.dz(), c.v(), c.dx(), c.dz(), true);
        fill(im, ramp, r, c.u() + c.dz() + c.dx(), c.v(), c.dx(), c.dz(), false);
        fill(im, ramp, r, c.u(), c.v() + c.dz(), c.dz(), c.dy(), false);
        fill(im, ramp, r, c.u() + c.dz(), c.v() + c.dz(), c.dx(), c.dy(), true);
        fill(im, ramp, r, c.u() + c.dz() + c.dx(), c.v() + c.dz(), c.dz(), c.dy(), false);
        fill(im, ramp, r, c.u() + 2 * c.dz() + c.dx(), c.v() + c.dz(), c.dx(), c.dy(), true);
    }

    /** A window: dark glass with a bright pixel diagonal where the light catches it. */
    private static void window(BufferedImage im, int x, int y, int w, int h) {
        for (int j = 0; j < h; j++)
            for (int i = 0; i < w; i++) TextureGen.px(im, x + i, y + j, GLASS.get(j == 0 ? 2 : 1 + (i + j) % 2));
        for (int k = 0; k < Math.min(w, h) - 1; k++) TextureGen.px(im, x + 1 + k, y + h - 2 - k, GLASS.get(5));
    }

    static BufferedImage sheet() {
        BufferedImage im = new BufferedImage(TramAtlas.WIDTH, TramAtlas.HEIGHT, BufferedImage.TYPE_INT_ARGB);
        long seed = 2000;
        net(im, "body_low", MAROON, seed++);
        net(im, "body_up", CREAM, seed++);
        net(im, "roof", CREAM, seed++);
        for (String part : new String[] {"frame", "vent", "pole_base"}) net(im, part, SOOT, seed++);
        for (String part : new String[] {"bumper", "wheel", "hitch", "pole"}) net(im, part, IRON, seed++);
        for (String part : new String[] {"shoe", "bell"}) net(im, part, BRASS, seed++);
        net(im, "lamp", BRASS, seed++);
        // The roof's edge is maroon, a band along its sides.
        var roof = TramAtlas.cell("roof");
        for (int i = 0; i < roof.dx(); i++) {
            for (int j = 0; j < roof.dy(); j++) {
                TextureGen.px(im, roof.u() + roof.dz() + i, roof.v() + roof.dz() + j, MAROON.get(j == 0 ? 4 : 3));
                TextureGen.px(im, roof.u() + 2 * roof.dz() + roof.dx() + i, roof.v() + roof.dz() + j, MAROON.get(j == 0 ? 4 : 3));
            }
        }
        // A cream belt line along the top of the lower body.
        var low = TramAtlas.cell("body_low");
        for (int i = 0; i < low.dx(); i++) {
            TextureGen.px(im, low.u() + low.dz() + i, low.v() + low.dz(), CREAM.get(4));
            TextureGen.px(im, low.u() + 2 * low.dz() + low.dx() + i, low.v() + low.dz(), CREAM.get(4));
        }
        for (int j = 0; j < low.dy(); j++) {
            // Panel seams down the sides.
            for (int x : new int[] {10, 21}) {
                TextureGen.px(im, low.u() + low.dz() + x, low.v() + low.dz() + j, MAROON.get(1));
                TextureGen.px(im, low.u() + 2 * low.dz() + low.dx() + x, low.v() + low.dz() + j, MAROON.get(1));
            }
        }
        // The upper body: four windows a side between cream pillars, a windscreen at the cab end, a small one at the back.
        var up = TramAtlas.cell("body_up");
        int north = up.u() + up.dz(), south = up.u() + 2 * up.dz() + up.dx(), front = up.u() + up.dz() + up.dx(), back = up.u(), v = up.v() + up.dz();
        for (int x : new int[] {2, 10, 18, 26}) {
            window(im, north + x, v + 1, 6, 4);
            window(im, south + x, v + 1, 6, 4);
        }
        window(im, front + 2, v + 1, 6, 4);
        window(im, front + 10, v + 1, 6, 4);
        window(im, back + 5, v + 1, 8, 4);
        // The lamp: a bright lens on the face that looks forward (+x is the east face of the net).
        var lamp = TramAtlas.cell("lamp");
        int lx = lamp.u() + lamp.dz() + lamp.dx(), ly = lamp.v() + lamp.dz();
        for (int j = 0; j < lamp.dy(); j++)
            for (int i = 0; i < lamp.dz(); i++) TextureGen.px(im, lx + i, ly + j, AMBER.get(j == 0 ? 5 : 4));
        // A hub on each wheel face.
        var wheel = TramAtlas.cell("wheel");
        TextureGen.px(im, wheel.u() + wheel.dz() + 1, wheel.v() + wheel.dz() + 1, WHITE.get(4));
        TextureGen.px(im, wheel.u() + 2 * wheel.dz() + wheel.dx() + 1, wheel.v() + wheel.dz() + 1, WHITE.get(4));
        return im;
    }

    // ---------------------------------------------------------------- the items

    private static void rect(BufferedImage im, int x0, int y0, int x1, int y1, TextureGen.Ramp ramp, int top, int mid, int bottom) {
        for (int y = y0; y <= y1; y++)
            for (int x = x0; x <= x1; x++) TextureGen.px(im, x, y, ramp.get(y == y0 ? top : y == y1 ? bottom : mid));
    }

    /** The tram from the side: a cream car with a maroon skirt and roof, windows, a trolley pole to a copper wire. */
    static BufferedImage tram() {
        BufferedImage im = TextureGen.img();
        for (int x = 0; x <= 15; x++) TextureGen.px(im, x, 0, COPPER.get(x % 2 == 0 ? 4 : 3));
        // The pole leans back from the roof to the wire.
        for (int k = 0; k <= 5; k++) TextureGen.px(im, 5 - k * 4 / 5, 6 - k, IRON.get(k == 5 ? 5 : 4));
        TextureGen.px(im, 1, 1, BRASS.get(5));
        TextureGen.px(im, 2, 1, BRASS.get(4));
        rect(im, 2, 6, 14, 6, MAROON, 5, 4, 3);
        rect(im, 3, 7, 14, 9, CREAM, 5, 4, 3);
        for (int x : new int[] {4, 8, 12}) {
            for (int y = 7; y <= 8; y++) for (int dx = 0; dx <= 1; dx++) TextureGen.px(im, x + dx, y, GLASS.get(y == 7 ? 4 : 3));
        }
        TextureGen.px(im, 14, 8, AMBER.get(5));
        rect(im, 2, 10, 14, 11, MAROON, 4, 3, 2);
        for (int cx : new int[] {5, 11}) {
            for (int y = 11; y <= 13; y++)
                for (int x = cx - 1; x <= cx + 1; x++) {
                    boolean corner = (x == cx - 1 || x == cx + 1) && (y == 11 || y == 13);
                    if (!corner) TextureGen.px(im, x, y, IRON.get(x < cx ? 5 : y < 13 ? 3 : 2));
                }
        }
        rect(im, 3, 14, 13, 14, SOOT, 4, 3, 2);
        return TextureGen.outline(im);
    }

    /** A spool of copper wire between two steel flanges, an end hanging free. */
    static BufferedImage wire() {
        BufferedImage im = TextureGen.img();
        for (int y = 4; y <= 12; y++) {
            TextureGen.px(im, 3, y, STEEL.get(y < 8 ? 5 : 3));
            TextureGen.px(im, 12, y, STEEL.get(y < 8 ? 4 : 2));
        }
        for (int y = 5; y <= 11; y++) {
            for (int x = 4; x <= 11; x++) {
                int band = (x + (y % 2)) % 2;
                TextureGen.px(im, x, y, COPPER.get(y <= 6 ? 5 - band : y >= 10 ? 1 + band : 3 + band));
            }
        }
        // The loose end, curling off the top and down.
        for (int[] p : new int[][] {{11, 4}, {12, 3}, {13, 3}, {14, 4}, {14, 5}, {14, 6}}) TextureGen.px(im, p[0], p[1], COPPER.get(4));
        // A brass cap in the hub.
        TextureGen.px(im, 7, 8, BRASS.get(5));
        TextureGen.px(im, 8, 8, BRASS.get(4));
        return TextureGen.outline(im);
    }

    // ---------------------------------------------------------------- blocks

    /** Three bands: steel plate on top (rows 0-7), brown ceramic (8-11), the copper clamp (12-15). */
    static BufferedImage bracket() {
        BufferedImage im = TextureGen.img();
        Random r = new Random(2101);
        TextureGen.grain(im, STEEL, r, 0, 0, 16, 8, 3, false);
        for (int x = 0; x < 16; x++) {
            TextureGen.px(im, x, 0, STEEL.get(5));
            TextureGen.px(im, x, 7, STEEL.get(1));
        }
        for (int x : new int[] {2, 7, 12}) TextureGen.px(im, x, 3, IRON.get(2));
        TextureGen.grain(im, CERAMIC, r, 0, 8, 16, 4, 3, false);
        for (int x = 0; x < 16; x++) {
            TextureGen.px(im, x, 8, CERAMIC.get(5));
            TextureGen.px(im, x, 11, CERAMIC.get(1));
        }
        TextureGen.grain(im, COPPER, r, 0, 12, 16, 4, 3, false);
        for (int x = 0; x < 16; x++) {
            TextureGen.px(im, x, 12, COPPER.get(5));
            TextureGen.px(im, x, 15, COPPER.get(1));
        }
        return im;
    }

    /** The wire itself: the model reads one column, strands of copper along it. */
    static BufferedImage line() {
        BufferedImage im = TextureGen.img();
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 2; x++) TextureGen.px(im, x, y, COPPER.get(((y / 2) + x) % 2 == 0 ? 4 : 3));
        }
        return im;
    }

    public static void main(String[] args) throws IOException {
        TextureGen.saveRaw("entity/electric_tram", sheet());
        TextureGen.itemsV2 = true;
        TextureGen.save("item/electric_tram", tram());
        TextureGen.save("item/trolley_wire", wire());
        TextureGen.itemsV2 = false;
        TextureGen.save("block/trolley_bracket", bracket());
        TextureGen.save("block/trolley_line", line());
    }

    private TramTextures() {}
}
