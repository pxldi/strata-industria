import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Tier 6 oil still textures (spec 24.3 and 24.5): a hammered copper pot on a fire brick base, a gauge glass
 * with three coloured cuts on the front (bubbling when it works), a gooseneck stub on the lid, and the GUI.
 * Built from TextureGen's helpers and ramps, like the tier 4 roaster.
 *
 * <p>Compile with TextureGen and Tier6Textures and run from the repository root:
 * {@code javac -d build/tg tools/texturegen/TextureGen.java tools/texturegen/Tier6Textures.java tools/texturegen/OilStillTextures.java}
 * then {@code java -cp build/tg OilStillTextures}. Output is deterministic.
 */
public final class OilStillTextures {
    static int cu(int s) { return TextureGen.COPPER.get(s); }
    static int fb(int s) { return TextureGen.FIRE_BRICK.get(s); }
    static int br(int s) { return TextureGen.BRASS.get(s); }
    static final int DEEP = 0x0e0c0c, GLASS_DARK = 0x1c1814;

    static int hash(int x, int y) { return Math.floorMod(x * 73 + y * 151 + x * y * 17 + 11, 97); }

    static void px(BufferedImage im, int x, int y, int rgb) { TextureGen.px(im, x, y, rgb); }

    /** Fire brick base on the lower 4 rows, as on the roaster and kiln. */
    static void base(BufferedImage im) {
        BufferedImage bricks = TextureGen.fireBricks();
        for (int y = 12; y < 16; y++) for (int x = 0; x < 16; x++) im.setRGB(x, y, bricks.getRGB(x, y));
    }

    /** Hammered copper plate over rows 0..11: lit top edge, dimples, a belt seam above the base. */
    static void pot(BufferedImage im) {
        for (int y = 0; y < 12; y++)
            for (int x = 0; x < 16; x++) {
                int h = hash(x, y);
                int s = 3;
                if (h % 7 == 0) s = 4;
                else if (h % 5 == 0) s = 2;
                if (y == 0) s = 5;
                else if (y == 1) s = 4;
                if (y == 10) s = 2;
                if (y == 11) s = 1;
                if (x == 0 && y < 10) s = Math.min(5, s + 1);
                if (x == 15 && y < 10) s = Math.max(2, s - 1);
                px(im, x, y, cu(s));
            }
        // Dimples read as little crescents: dark under-left, light upper-right.
        int[][] dimples = {{3, 4}, {11, 3}, {6, 8}, {13, 7}};
        for (int[] d : dimples) { px(im, d[0], d[1], cu(2)); px(im, d[0] + 1, d[1] - 1, cu(4)); }
        px(im, 2, 1, TextureGen.COPPER.spec());
    }

    static void rivets(BufferedImage im, int... xs) {
        for (int x : xs) { px(im, x, 9, cu(5)); px(im, x + 1, 10, cu(1)); }
    }

    static BufferedImage side() {
        BufferedImage im = TextureGen.img();
        pot(im);
        base(im);
        // Soldered upright seam with a lit bead beside it.
        for (int y = 2; y < 10; y++) { px(im, 7, y, cu(2)); px(im, 8, y, cu(4)); }
        rivets(im, 3, 11);
        for (int y = 2; y < 10; y += 3) { px(im, 5, y, cu(4)); px(im, 5, y + 1, cu(2)); }
        return im;
    }

    /** The gauge glass: x5..10, y2..9, brass frame around it. */
    static BufferedImage front(int frame) {
        boolean active = frame >= 0;
        BufferedImage im = TextureGen.img();
        pot(im);
        base(im);
        // Frame.
        for (int x = 4; x <= 11; x++) { px(im, x, 1, br(5)); px(im, x, 10, br(1)); }
        for (int y = 1; y <= 10; y++) { px(im, 4, y, br(y == 1 ? 5 : 4)); px(im, 11, y, br(y == 10 ? 1 : 2)); }
        // Glass interior: three cuts, light to heavy from the top.
        for (int y = 2; y <= 9; y++)
            for (int x = 5; x <= 10; x++) {
                int c;
                if (y <= 2) c = GLASS_DARK;
                else if (y <= 4) c = Tier6Textures.NAPHTHA.get(x == 5 ? 4 : 3);
                else if (y <= 7) c = Tier6Textures.DIESEL.get(x == 5 ? 4 : 3);
                else c = Tier6Textures.HEAVY.get(x == 5 ? 4 : 3);
                // Meniscus line where two cuts meet.
                if (y == 3 || y == 5 || y == 8) c = y == 3 ? Tier6Textures.NAPHTHA.get(5) : y == 5 ? Tier6Textures.DIESEL.get(5) : Tier6Textures.HEAVY.get(4);
                px(im, x, y, c);
            }
        // Glass sheen down the left edge.
        px(im, 5, 2, 0x8a8478); px(im, 5, 3, 0xcfc9b8);
        if (active) {
            // Bubbles climb the glass, one column each, and pop at the top of their cut.
            int[][] cols = {{6, 0}, {8, 2}, {9, 1}};
            for (int[] c : cols) {
                int y = 9 - Math.floorMod(frame * 2 + c[1], 7);
                if (y >= 3) px(im, c[0], y, y >= 8 ? Tier6Textures.HEAVY.get(5) : y >= 5 ? Tier6Textures.DIESEL.get(5) : Tier6Textures.NAPHTHA.get(5));
            }
            // The glass warms: a faint glow on the frame's lower edge.
            for (int x = 5; x <= 10; x++) px(im, x, 10, TextureGen.mix(br(1), TextureGen.HEAT_BAND[2], 0.35));
        }
        rivets(im, 2, 13);
        return im;
    }

    static BufferedImage top() {
        BufferedImage im = TextureGen.img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int h = hash(x, y), s = 3;
                if (h % 7 == 0) s = 4;
                else if (h % 5 == 0) s = 2;
                if (y == 0 || x == 0) s = 5;
                else if (y == 15 || x == 15) s = 1;
                else if (y == 1 || x == 1) s = 4;
                else if (y == 14 || x == 14) s = 2;
                px(im, x, y, cu(s));
            }
        // Lid bolts.
        for (int[] r : new int[][] {{2, 2}, {13, 2}, {2, 13}, {13, 13}}) { px(im, r[0], r[1], cu(5)); px(im, r[0] + 1, r[1] + 1, cu(1)); }
        // Gooseneck stub: a round collar, a dark bore, the neck leaving to the back.
        double c = 7.5;
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double dx = x - c, dy = y - c, d = Math.hypot(dx, dy);
                if (d > 5.2) continue;
                boolean lit = dx + dy < -1.2;
                int s;
                if (d <= 2.0) { px(im, x, y, dx + dy < -1 ? DEEP : 0x1b1615); continue; }
                else if (d <= 3.4) s = lit ? 5 : dx + dy > 2 ? 2 : 4;
                else s = lit ? 4 : dx + dy > 2.5 ? 1 : 2;
                if (Math.abs(d - 3.45) < 0.5) s = 1;
                px(im, x, y, cu(s));
            }
        return im;
    }

    // ---------------------------------------------------------------- GUI

    static final int TANK_Y = 17, TANK_W = 16, TANK_H = 46;

    static void tankWell(BufferedImage im, int x) {
        TextureGen.well(im, x - 1, TANK_Y - 1, TANK_W + 2, TANK_H + 2, 0x2b2b2b);
        for (int t = 0; t < 4; t++) {
            int y = TANK_Y + 9 + t * 9, len = t == 1 ? 5 : 3;
            TextureGen.fill(im, x + TANK_W - len, y, len, 1, TextureGen.SLOT_FILL);
        }
    }

    /** One 16x46 fill sprite: lit surface line, a lit left edge, a darker bottom, and a little gloss texture. */
    static BufferedImage fill(int light, int mid, int dark, int gloss) {
        BufferedImage im = new BufferedImage(TANK_W, TANK_H, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < TANK_H; y++)
            for (int x = 0; x < TANK_W; x++) {
                double t = y / (double) (TANK_H - 1);
                int c = TextureGen.mix(mid, dark, t * 0.8);
                if (x == 0) c = TextureGen.mix(c, light, 0.55);
                if (x == TANK_W - 1) c = TextureGen.mix(c, dark, 0.5);
                if (y == 0) c = light;
                if (((x * 5 + y * 3) % 11) == 0 && x > 1 && x < TANK_W - 2 && y > 1) c = TextureGen.mix(c, gloss, 0.4);
                im.setRGB(x, y, 0xff000000 | c);
            }
        return im;
    }

    static void blit(BufferedImage dst, BufferedImage src, int ox, int oy) {
        for (int y = 0; y < src.getHeight(); y++) for (int x = 0; x < src.getWidth(); x++) dst.setRGB(ox + x, oy + y, src.getRGB(x, y));
    }

    /** A right arrow 24 wide and 16 high, filled by {@code colour}. */
    static void arrow(BufferedImage im, int ox, int oy, java.util.function.IntBinaryOperator colour) {
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 24; x++) {
                int d = Math.abs(y - 7);
                boolean shaft = d <= 2 && x < 14;
                boolean head = x >= 12 && x < 24 - d * 12 / 8 - 0 && x - 12 <= 11 - d * 11 / 8 + 0;
                if (shaft || head && d <= 7) im.setRGB(ox + x, oy + y, 0xff000000 | colour.applyAsInt(x, y));
            }
    }

    static BufferedImage gui() {
        BufferedImage im = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        TextureGen.panel(im, 176, 182);
        tankWell(im, 26);
        for (int x : new int[] {88, 112, 136}) tankWell(im, x);
        arrow(im, 52, 32, (x, y) -> TextureGen.SLOT_FILL);
        // Filled arrow sprite: the heat band.
        arrow(im, 176, 0, (x, y) -> {
            int c = TextureGen.mix(TextureGen.HEAT_BAND[3], TextureGen.HEAT_BAND[4], y / 15.0);
            if (y < 6 && x > 10) c = TextureGen.mix(c, TextureGen.HEAT_BAND[2], 0.3);
            return c;
        });
        // Fill sprites: crude, naphtha, diesel, heavy oil, anything else.
        blit(im, fill(0x6e6478, 0x403638, 0x221c1e, 0x6e6478), 176, 16);
        blit(im, fill(Tier6Textures.NAPHTHA.get(5), Tier6Textures.NAPHTHA.get(4), Tier6Textures.NAPHTHA.get(2), 0xf4efd0), 192, 16);
        blit(im, fill(Tier6Textures.DIESEL.get(5), Tier6Textures.DIESEL.get(4), Tier6Textures.DIESEL.get(2), 0xe8c070), 208, 16);
        blit(im, fill(0x5e5a46, Tier6Textures.HEAVY.get(5), Tier6Textures.HEAVY.get(2), 0x6a6650), 224, 16);
        blit(im, fill(0xc8c8cc, 0x9a9aa0, 0x606068, 0xeeeeee), 240, 16);
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) TextureGen.slot(im, 8 + col * 18, 100 + row * 18);
        for (int col = 0; col < 9; col++) TextureGen.slot(im, 8 + col * 18, 158);
        return im;
    }

    public static void main(String[] args) throws IOException {
        TextureGen.save("block/oil_still_front", front(-1));
        BufferedImage strip = new BufferedImage(16, 64, BufferedImage.TYPE_INT_ARGB);
        for (int f = 0; f < 4; f++) blit(strip, front(f), 0, f * 16);
        TextureGen.saveAnimated("block/oil_still_front_active", strip, 4);
        TextureGen.save("block/oil_still_side", side());
        TextureGen.save("block/oil_still_top", top());
        TextureGen.saveRaw("gui/oil_still", gui());
    }
}
