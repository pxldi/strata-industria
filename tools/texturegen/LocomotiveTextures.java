import java.awt.image.BufferedImage;
import java.io.IOException;
import dev.strataindustria.client.rail.LocomotiveAtlas;
import java.util.Random;

/**
 * Textures for the steam locomotive, the water tower and the coal stage (outposts and transport spec 7.3, 7.4 and
 * 14): the engine's sheet and item icon, the tower's trestle and spout, the coal stage's timber and its coal.
 *
 * <p>The engine sheet is laid out by {@code dev.strataindustria.client.rail.LocomotiveAtlas}, which the model reads
 * too. Run from the repository root:
 * <pre>
 * mkdir -p build/tg &amp;&amp; javac -d build/tg tools/texturegen/TextureGen.java src/main/java/dev/strataindustria/client/rail/LocomotiveAtlas.java tools/texturegen/LocomotiveTextures.java &amp;&amp; java -cp build/tg LocomotiveTextures
 * </pre>
 */
public final class LocomotiveTextures {
    static final TextureGen.Ramp GREEN = TextureGen.ramp(0, 0x10281c, 0x173a28, 0x1f5236, 0x2a6b46, 0x3c8a5c);
    static final TextureGen.Ramp SOOT = TextureGen.ramp(0, 0x121214, 0x1e1e22, 0x2b2b30, 0x3a3a41, 0x4c4c55);
    static final TextureGen.Ramp RED = TextureGen.ramp(0, 0x4a1612, 0x7a241c, 0xa8342a, 0xc8483a, 0xe27060);
    static final TextureGen.Ramp STEEL = TextureGen.V2.STEEL_V2;
    static final TextureGen.Ramp IRON = TextureGen.WROUGHT_IRON;
    static final TextureGen.Ramp BRASS = TextureGen.BRASS;
    static final TextureGen.Ramp GLASS = TextureGen.ramp(0, 0x0e1a28, 0x16283c, 0x203c58, 0x2e5478, 0x4a7aa0);
    static final TextureGen.Ramp AMBER = TextureGen.ramp(0, 0x7a4a10, 0xb87a1c, 0xe8a830, 0xf8cc58, 0xfff0a0);
    static final TextureGen.Ramp WHITE = TextureGen.ramp(0, 0x7a7e86, 0x9ea2aa, 0xc2c6cc, 0xdee0e4, 0xf2f3f5);
    static final TextureGen.Ramp TIMBER = TextureGen.TREATED_WOOD;
    static final TextureGen.Ramp WOOD = TextureGen.V2.WOOD_V2;
    static final TextureGen.Ramp COAL = TextureGen.ORE_COAL;

    // ---------------------------------------------------------------- the engine's sheet

    private static void fill(BufferedImage im, TextureGen.Ramp ramp, Random r, int x, int y, int w, int h, boolean plates, boolean lit) {
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
        if (plates) {
            // A rivet row along the top of larger faces, and a seam through the middle of tall ones.
            if (w >= 6 && h >= 5) for (int i = 2; i < w - 1; i += 3) TextureGen.px(im, x + i, y + 2, ramp.get(5));
            if (h >= 10) for (int i = 1; i < w - 1; i++) TextureGen.px(im, x + i, y + h / 2, ramp.get(1));
        }
    }

    /** Paints the net of one part: top, bottom, the four sides, as the model lays them out. */
    private static void net(BufferedImage im, String part, TextureGen.Ramp ramp, boolean plates, long seed) {
        var c = LocomotiveAtlas.cell(part);
        Random r = new Random(seed);
        fill(im, ramp, r, c.u() + c.dz(), c.v(), c.dx(), c.dz(), plates, true);
        fill(im, ramp, r, c.u() + c.dz() + c.dx(), c.v(), c.dx(), c.dz(), plates, false);
        fill(im, ramp, r, c.u(), c.v() + c.dz(), c.dz(), c.dy(), plates, false);
        fill(im, ramp, r, c.u() + c.dz(), c.v() + c.dz(), c.dx(), c.dy(), plates, true);
        fill(im, ramp, r, c.u() + c.dz() + c.dx(), c.v() + c.dz(), c.dz(), c.dy(), plates, false);
        fill(im, ramp, r, c.u() + 2 * c.dz() + c.dx(), c.v() + c.dz(), c.dx(), c.dy(), plates, true);
    }

    static BufferedImage sheet() {
        BufferedImage im = new BufferedImage(LocomotiveAtlas.WIDTH, LocomotiveAtlas.HEIGHT, BufferedImage.TYPE_INT_ARGB);
        long seed = 1000;
        for (String part : new String[] {"back_wall", "side_panel", "boiler_a", "boiler_b", "tank"}) net(im, part, GREEN, true, seed++);
        for (String part : new String[] {"frame", "smokebox", "chimney", "roof", "post"}) net(im, part, SOOT, part.equals("frame"), seed++);
        for (String part : new String[] {"band", "dome", "dome_cap", "chimney_cap"}) net(im, part, BRASS, false, seed++);
        net(im, "beam", RED, false, seed++);
        for (String part : new String[] {"buffer", "wheel", "hitch"}) net(im, part, IRON, false, seed++);
        net(im, "rod", STEEL, false, seed++);
        net(im, "glass", GLASS, false, seed++);
        net(im, "needle", WHITE, false, seed++);
        // The lamp: brass case, a bright lens on the face that looks forward (+x is the east face of the net).
        net(im, "lamp", BRASS, false, seed++);
        var lamp = LocomotiveAtlas.cell("lamp");
        int lx = lamp.u() + lamp.dz() + lamp.dx(), ly = lamp.v() + lamp.dz();
        for (int j = 0; j < lamp.dy(); j++)
            for (int i = 0; i < lamp.dz(); i++) TextureGen.px(im, lx + i, ly + j, AMBER.get(j == 0 ? 5 : 4));
        // A rivet row on the cab roof and the brass bands' ridges are already in the plates; the buffer beam gets its hazard stripe.
        var beam = LocomotiveAtlas.cell("beam");
        for (int i = 0; i < beam.dz(); i += 2) {
            TextureGen.px(im, beam.u() + beam.dz() + beam.dx() + i, beam.v() + beam.dz() + 1, WHITE.get(4));
            TextureGen.px(im, beam.u() + beam.dz() + beam.dx() + i + 1, beam.v() + beam.dz() + 1, WHITE.get(3));
        }
        return im;
    }

    // ---------------------------------------------------------------- the item

    private static void rect(BufferedImage im, int x0, int y0, int x1, int y1, TextureGen.Ramp ramp, int top, int mid, int bottom) {
        for (int y = y0; y <= y1; y++)
            for (int x = x0; x <= x1; x++) TextureGen.px(im, x, y, ramp.get(y == y0 ? top : y == y1 ? bottom : mid));
    }

    /** The engine from the side: green cab and boiler, a black smokebox and chimney, a brass dome, a red beam, two big wheels. */
    static BufferedImage item() {
        BufferedImage im = TextureGen.img();
        rect(im, 0, 3, 6, 3, SOOT, 5, 4, 3);
        rect(im, 1, 4, 5, 10, GREEN, 5, 3, 2);
        for (int y = 5; y <= 7; y++) for (int x = 2; x <= 4; x++) TextureGen.px(im, x, y, GLASS.get(y == 5 ? 2 : 3));
        rect(im, 6, 5, 12, 10, GREEN, 5, 3, 2);
        for (int y = 5; y <= 10; y++) {
            TextureGen.px(im, 8, y, BRASS.get(y == 5 ? 5 : 4));
            TextureGen.px(im, 11, y, BRASS.get(y == 5 ? 5 : 4));
        }
        rect(im, 12, 5, 14, 10, SOOT, 4, 3, 2);
        rect(im, 12, 2, 13, 4, SOOT, 4, 3, 2);
        rect(im, 11, 1, 14, 1, BRASS, 5, 4, 3);
        rect(im, 7, 3, 9, 4, BRASS, 5, 4, 3);
        rect(im, 14, 9, 15, 11, RED, 5, 4, 2);
        TextureGen.px(im, 15, 7, AMBER.get(5));
        TextureGen.px(im, 14, 7, AMBER.get(4));
        rect(im, 1, 11, 14, 11, SOOT, 4, 3, 2);
        for (int cx : new int[] {4, 11}) {
            for (int y = 11; y <= 14; y++)
                for (int x = cx - 2; x <= cx + 1; x++) {
                    boolean corner = (x == cx - 2 || x == cx + 1) && (y == 11 || y == 14);
                    if (!corner) TextureGen.px(im, x, y, IRON.get(x < cx ? (y < 13 ? 5 : 4) : (y < 13 ? 3 : 2)));
                }
            TextureGen.px(im, cx, 12, RED.get(4));
            TextureGen.px(im, cx - 1, 13, RED.get(2));
        }
        for (int x = 4; x <= 11; x++) TextureGen.px(im, x, 12, STEEL.get(x % 2 == 0 ? 5 : 4));
        return TextureGen.outline(im);
    }

    // ---------------------------------------------------------------- blocks

    /** Treated planks in vertical boards with two iron straps and rivets: the tower's trestle. */
    static BufferedImage trestle() {
        BufferedImage im = TextureGen.img();
        Random r = new Random(1301);
        for (int b = 0; b < 4; b++) {
            TextureGen.grain(im, TIMBER, r, b * 4, 0, 4, 16, 3, true);
            for (int y = 0; y < 16; y++) {
                TextureGen.px(im, b * 4, y, TIMBER.get(4));
                TextureGen.px(im, b * 4 + 3, y, TIMBER.get(1));
            }
        }
        for (int y : new int[] {3, 12}) {
            for (int x = 0; x < 16; x++) {
                TextureGen.px(im, x, y, IRON.get(4));
                TextureGen.px(im, x, y + 1, IRON.get(2));
            }
            for (int x : new int[] {1, 5, 9, 13}) TextureGen.px(im, x, y, IRON.get(5));
        }
        return im;
    }

    /** Riveted brass sheet with two seams: the spout's pipe and arm. */
    static BufferedImage spout() {
        BufferedImage im = TextureGen.img();
        Random r = new Random(1302);
        TextureGen.grain(im, BRASS, r, 0, 0, 16, 16, 3, false);
        for (int x = 0; x < 16; x++) {
            TextureGen.px(im, x, 0, BRASS.get(5));
            TextureGen.px(im, x, 15, BRASS.get(1));
            TextureGen.px(im, x, 7, BRASS.get(1));
            TextureGen.px(im, x, 8, BRASS.get(5));
        }
        for (int y = 0; y < 16; y++) {
            TextureGen.px(im, 0, y, BRASS.get(4));
            TextureGen.px(im, 15, y, BRASS.get(2));
        }
        for (int x = 2; x < 16; x += 4) {
            TextureGen.px(im, x, 3, IRON.get(2));
            TextureGen.px(im, x, 11, IRON.get(2));
        }
        return im;
    }

    /** Dark timber boards across, iron corner plates: the coal stage's bin. */
    static BufferedImage stage() {
        BufferedImage im = TextureGen.img();
        Random r = new Random(1303);
        for (int b = 0; b < 4; b++) {
            TextureGen.grain(im, WOOD, r, 0, b * 4, 16, 4, 3, false);
            for (int x = 0; x < 16; x++) {
                TextureGen.px(im, x, b * 4, WOOD.get(4));
                TextureGen.px(im, x, b * 4 + 3, WOOD.get(1));
            }
        }
        for (int y = 0; y < 16; y++) {
            TextureGen.px(im, 0, y, IRON.get(4));
            TextureGen.px(im, 1, y, IRON.get(2));
            TextureGen.px(im, 15, y, IRON.get(2));
            TextureGen.px(im, 14, y, IRON.get(4));
        }
        for (int y : new int[] {2, 8, 13}) {
            TextureGen.px(im, 0, y, IRON.get(5));
            TextureGen.px(im, 15, y, IRON.get(5));
        }
        return im;
    }

    /** A heap of coal seen from above: lumps with a lit corner. */
    static BufferedImage heap() {
        BufferedImage im = TextureGen.img();
        Random r = new Random(1304);
        TextureGen.grain(im, COAL, r, 0, 0, 16, 16, 2, false);
        for (int i = 0; i < 26; i++) {
            int x = r.nextInt(14), y = r.nextInt(14);
            int w = 2 + r.nextInt(2), h = 2 + r.nextInt(2);
            for (int j = 0; j < h; j++)
                for (int k = 0; k < w; k++) TextureGen.px(im, x + k, y + j, COAL.get(j == 0 || k == 0 ? 4 : j == h - 1 || k == w - 1 ? 1 : 3));
            TextureGen.px(im, x, y, COAL.get(5));
        }
        return im;
    }

    public static void main(String[] args) throws IOException {
        TextureGen.saveRaw("entity/steam_locomotive", sheet());
        TextureGen.itemsV2 = true;
        TextureGen.save("item/steam_locomotive", item());
        TextureGen.itemsV2 = false;
        TextureGen.save("block/water_tower_base", trestle());
        TextureGen.save("block/water_tower_spout", spout());
        TextureGen.save("block/coal_stage", stage());
        TextureGen.save("block/coal_stage_top", heap());
    }

    private LocomotiveTextures() {}
}
