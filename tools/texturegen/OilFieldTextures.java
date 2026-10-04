import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * Tier 6 oil field textures (spec 24.3 and 24.5): the seismic charge (block texture, lit variant, item icon),
 * the wellhead (side, bore top, drilled top, GUI), the pump jack (skid, beam, horse head) and the oil spray
 * particle. Built from TextureGen's helpers and ramps like OilStillTextures.
 *
 * <p>Compile with TextureGen and Tier6Textures and run from the repository root:
 * {@code javac -d build/tg tools/texturegen/TextureGen.java tools/texturegen/Tier6Textures.java tools/texturegen/OilFieldTextures.java}
 * then {@code java -cp build/tg OilFieldTextures}. Output is deterministic. Also writes a 6x preview to
 * {@code build/texturegen/oilfield.png}.
 *
 * <p>Texture layouts (the models in gen_oilfield_models.py rely on them):
 * <ul>
 * <li>seismic_charge: paper wrap x0..15 y0..7 (cylinder sides, 6 wide faces), clay base side x0..7 y8..9,
 *     clay top/bottom x8..15 y8..15, paper cap x0..5 y10..15, fuse column x6 y10..13 (side), x7 y10 (tip/top).
 *     The lit variant only changes the fuse tip pixels.</li>
 * <li>wellhead_side: rows 0..5 top flange (block y16..10), brass for the valve wheel and stem in columns 0..3 and 12..15 of rows 6..12, rows 6..12 neck
 *     (columns 4..11), rows 13..15 base flange; matches the default face mapping of the model.</li>
 * <li>wellhead_top / wellhead_drilled_top: 16x16 plan view, plate rim 0..1, flange x2..13, bore centred at 7.5/7.5.</li>
 * <li>pump_jack_base: rows 11..15 skid (block y0..5), rows 5..10 gearbox (y5..11), rows 0..4 collar.</li>
 * <li>pump_jack_beam: columns 0..11 plain steel (all faces except the stripe), columns 12..14 carry the one copper
 *     stripe (ramp step 4) at column 13; only the beam bar's top face maps there.</li>
 * <li>pump_jack_head: bright steel, whole tile.</li>
 * <li>gui/wellhead.png 256x256, panel 176x166 in the top-left: casing slot item origin (80,20) (frame 79,19 18x18),
 *     depth bar frame (130,17) 12x46, depth bar FILL sprite at u=176 v=0 12x46 (crude ramp, top sheen),
 *     free space for two text lines below y=68, player inventory slots at (8+18c, 84+18r), hotbar at (8+18c, 142).</li>
 * <li>particle/oil_spray.png 4x4 (crude ramp, sheen pixel at 1,1).</li>
 * </ul>
 */
public final class OilFieldTextures {
    static final TextureGen.Ramp STEEL = TextureGen.STEEL, BRASS = TextureGen.BRASS, COPPER = TextureGen.COPPER;
    static final TextureGen.Ramp CLAY = TextureGen.CLAY;
    static final Tier6Textures.Ramp CRUDE = Tier6Textures.CRUDE;
    /** The latex ramp (as in ElectricTextures); paper wrap uses its light steps. */
    static final TextureGen.Ramp LATEX = TextureGen.ramp(0, 0x6e6656, 0x8e8670, 0xb0a88c, 0xcec6a8, 0xe6e0c4);
    static final int FUSE = 0x2a2220, FUSE_HI = 0x4a3c34, DEEP = 0x0e0c0c;

    static int hash(int x, int y) { return Math.floorMod(x * 73 + y * 151 + x * y * 17 + 11, 97); }
    static void px(BufferedImage im, int x, int y, int rgb) { TextureGen.px(im, x, y, rgb); }
    static int st(int s) { return STEEL.get(s); }

    /** Brushed steel grain: horizontal streaks, steps 2..4 with the odd 1 or 5. */
    static int steelStep(int x, int y) {
        int h = hash(x / 2 * 3 + 1, y);
        int s = 3;
        if (h % 6 == 0) s = 4;
        else if (h % 4 == 0) s = 2;
        if (hash(x, y * 5 + 3) % 29 == 0) s = 5;
        return s;
    }

    static void steelFill(BufferedImage im, int x0, int y0, int x1, int y1) {
        for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) px(im, x, y, st(steelStep(x, y)));
    }

    static void bolt(BufferedImage im, int x, int y) { px(im, x, y, st(5)); px(im, x + 1, y + 1, st(1)); }

    // ---------------------------------------------------------------- seismic charge

    static BufferedImage charge(boolean lit) {
        BufferedImage im = TextureGen.img();
        // Paper wrap x0..15 y0..7, with two printed bands and a worn crease.
        for (int y = 0; y < 8; y++)
            for (int x = 0; x < 16; x++) {
                int h = hash(x, y), s = 4;
                if (h % 5 == 0) s = 3;
                else if (h % 9 == 0) s = 5;
                if (y == 0) s = 5;
                if (y == 7) s = 3;
                if (y == 2 || y == 5) s = 2;
                if (x % 6 == 0 && y > 0) s = Math.max(2, s - 1);
                px(im, x, y, LATEX.get(s));
            }
        for (int x = 1; x < 16; x += 6) { px(im, x, 3, LATEX.get(2)); px(im, x + 1, 3, LATEX.get(2)); }
        // Clay base side x0..7 y8..9, top and bottom x8..15 y8..15.
        for (int y = 8; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                if (y >= 10 && x < 8) continue;
                int h = hash(x, y), s = 3;
                if (h % 5 == 0) s = 2;
                else if (h % 7 == 0) s = 4;
                if (y == 8 && x < 8) s = 4;
                if (y == 9 && x < 8) s = 2;
                px(im, x, y, CLAY.get(s));
            }
        // Paper cap x0..5 y10..15 with a ring edge.
        for (int y = 10; y < 16; y++)
            for (int x = 0; x < 6; x++) {
                int s = 5;
                if (x == 0 || y == 10) s = 5;
                else if (x == 5 || y == 15) s = 3;
                else if (hash(x, y) % 4 == 0) s = 4;
                px(im, x, y, LATEX.get(s));
            }
        // Fuse: column x6 y10..13 and the tip at x7 y10 (the lit variant glows only here).
        for (int y = 10; y <= 13; y++) px(im, 6, y, y % 2 == 0 ? FUSE : FUSE_HI);
        px(im, 7, 10, FUSE);
        if (lit) {
            px(im, 6, 10, TextureGen.HEAT_BAND[4]);
            px(im, 7, 10, TextureGen.HEAT_BAND[5]);
            px(im, 6, 11, TextureGen.HEAT_BAND[3]);
            px(im, 7, 11, TextureGen.HEAT_BAND[2]);
        }
        return im;
    }

    /** Flat inventory icon: wrapped paper cylinder on a clay stump, fuse at the top right, 1 px outline. */
    static BufferedImage chargeItem() {
        BufferedImage im = TextureGen.img();
        int cx0 = 3, cx1 = 11, top = 5, bot = 13;
        for (int y = top; y <= bot; y++)
            for (int x = cx0; x <= cx1; x++) {
                int s = 4;
                if (x == cx0) s = 5;
                else if (x == cx0 + 1) s = 4;
                else if (x >= cx1 - 1) s = 2;
                else if (hash(x, y) % 5 == 0) s = 3;
                if (y == top) s = x >= cx1 - 1 ? 3 : 5;
                if (y == top + 3 || y == top + 5) s = Math.max(2, s - 2);
                px(im, x, y, LATEX.get(s));
            }
        // Printed band label.
        for (int x = cx0 + 2; x <= cx1 - 2; x += 2) px(im, x, top + 4, LATEX.get(2));
        // Clay stump along the bottom.
        for (int y = bot - 1; y <= bot; y++)
            for (int x = cx0 - 1; x <= cx1 + 1; x++) px(im, x, y, CLAY.get(y == bot ? 2 : x < 5 ? 4 : 3));
        // Rounded top corners of the cylinder.
        im.setRGB(cx0, top, 0);
        im.setRGB(cx1, top, 0);
        // Fuse leaving the top right.
        px(im, 9, top - 1, FUSE);
        px(im, 10, top - 2, FUSE_HI);
        px(im, 11, top - 3, FUSE);
        px(im, 12, top - 3, FUSE_HI);
        return TextureGen.V2.outline(im);
    }

    // ---------------------------------------------------------------- wellhead

    static BufferedImage wellheadSide() {
        BufferedImage im = TextureGen.img();
        steelFill(im, 0, 0, 15, 15);
        // Rows 0..5: the top flange (block y16..10), lit upper edge, two bolts.
        for (int x = 0; x < 16; x++) {
            px(im, x, 0, st(5)); px(im, x, 1, st(4)); px(im, x, 4, st(3)); px(im, x, 5, st(2));
        }
        bolt(im, 4, 2); bolt(im, 10, 2);
        // Brass for the valve wheel and stem: columns 0..3 and 12..15, rows 6..12 (unused by the neck).
        for (int y = 6; y <= 12; y++)
            for (int x = 0; x < 16; x++) {
                if (x >= 4 && x < 12) continue;
                int s = y == 6 ? 5 : y == 12 ? 1 : (hash(x, y) % 3 == 0 ? 3 : 4);
                px(im, x, y, BRASS.get(s));
            }
        // Rows 6..12: neck, round pipe shading left to right with a weld bead near the top.
        for (int y = 6; y <= 12; y++)
            for (int x = 4; x < 12; x++) {
                int s = x < 6 ? 5 : x < 8 ? 4 : x < 10 ? 3 : x == 10 ? 2 : 1;
                if (hash(x, y) % 7 == 0 && s > 1) s--;
                if (y == 7) s = Math.max(1, s - 1);
                px(im, x, y, st(s));
            }
        for (int x = 4; x < 12; x++) px(im, x, 6, st(Math.max(1, (x < 8 ? 4 : 2))));
        // The valve flange boss on the neck.
        // Rows 13..15: base plate, bolts at the corners.
        for (int x = 0; x < 16; x++) { px(im, x, 13, st(5)); px(im, x, 14, st(4)); px(im, x, 15, st(1)); }
        bolt(im, 2, 14 - 1); bolt(im, 12, 14 - 1);
        return im;
    }

    static void flangeTop(BufferedImage im) {
        steelFill(im, 0, 0, 15, 15);
        // Plate rim bevel, light top-left.
        for (int i = 0; i < 16; i++) { px(im, i, 0, st(5)); px(im, 0, i, st(5)); px(im, i, 15, st(1)); px(im, 15, i, st(1)); }
        for (int i = 1; i < 15; i++) { px(im, i, 1, st(4)); px(im, 1, i, st(4)); px(im, i, 14, st(2)); px(im, 14, i, st(2)); }
        // Upper flange face x2..13: edge line and bolts.
        for (int i = 2; i < 14; i++) { px(im, i, 2, st(5)); px(im, 2, i, st(5)); px(im, i, 13, st(2)); px(im, 13, i, st(2)); }
        for (int[] b : new int[][] {{3, 3}, {11, 3}, {3, 11}, {11, 11}}) bolt(im, b[0], b[1]);
    }

    static BufferedImage wellheadTop() {
        BufferedImage im = TextureGen.img();
        flangeTop(im);
        double c = 7.5;
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double dx = x - c, dy = y - c, d = Math.hypot(dx, dy);
                if (d <= 2.6) px(im, x, y, dx + dy < -1 ? DEEP : 0x1b1615);
                else if (d <= 3.7) px(im, x, y, st(dx + dy < -0.5 ? 5 : dx + dy > 2 ? 1 : 3));
                else if (d <= 4.3) px(im, x, y, st(1));
            }
        return im;
    }

    static BufferedImage wellheadDrilledTop() {
        BufferedImage im = TextureGen.img();
        flangeTop(im);
        double c = 7.5;
        // Oil stain creeping out of the bore over the steel.
        for (int y = 2; y < 14; y++)
            for (int x = 2; x < 14; x++) {
                double d = Math.hypot(x - c, y - c);
                int h = hash(x, y) % 10;
                if (d > 4.4 && d < 5.2 + h * 0.2 && h < 6) px(im, x, y, CRUDE.get(h < 3 ? 2 : 3));
            }
        px(im, 11, 5, CRUDE.get(2)); px(im, 4, 10, CRUDE.get(3)); px(im, 12, 9, CRUDE.get(2));
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double dx = x - c, dy = y - c, d = Math.hypot(dx, dy);
                if (d <= 1.9) px(im, x, y, dx + dy < -1 ? CRUDE.get(2) : CRUDE.get(1));
                else if (d <= 3.0) px(im, x, y, BRASS.get(dx + dy < -0.8 ? 5 : dx + dy > 1.8 ? 2 : 4));
                else if (d <= 3.7) px(im, x, y, BRASS.get(1));
                else if (d <= 4.4) px(im, x, y, CRUDE.get(2));
            }
        // Cap bolts on the brass ring and a sheen on the plug.
        for (int[] b : new int[][] {{7, 4}, {7, 11}, {4, 7}, {11, 7}}) px(im, b[0], b[1], BRASS.get(5));
        px(im, 7, 7, CRUDE.spec());
        return im;
    }

    // ---------------------------------------------------------------- pump jack

    static BufferedImage pumpBase() {
        BufferedImage im = TextureGen.img();
        steelFill(im, 0, 0, 15, 15);
        // Rows 11..15: the skid, I-beam look: bright upper flange, shadowed web, dark lower flange.
        for (int x = 0; x < 16; x++) { px(im, x, 11, st(5)); px(im, x, 12, st(4)); px(im, x, 14, st(2)); px(im, x, 15, st(1)); }
        for (int x : new int[] {2, 7, 12}) { bolt(im, x, 12); px(im, x, 13, st(2)); }
        // Rows 5..10: gearbox casing with a seam and a breather plug.
        for (int x = 0; x < 16; x++) { px(im, x, 5, st(5)); px(im, x, 10, st(1)); px(im, x, 9, st(2)); }
        for (int y = 6; y <= 8; y++) { px(im, 0, y, st(5)); px(im, 15, y, st(2)); px(im, 7, y, st(2)); px(im, 8, y, st(4)); }
        bolt(im, 2, 6); bolt(im, 11, 6); bolt(im, 2, 8 - 1 + 0); bolt(im, 11, 7);
        px(im, 4, 7, BRASS.get(5)); px(im, 5, 7, BRASS.get(4)); px(im, 4, 8, BRASS.get(2)); px(im, 5, 8, BRASS.get(1));
        // Rows 0..4: stuffing box collar, steel with a brass gland ring.
        for (int x = 0; x < 16; x++) { px(im, x, 0, st(5)); px(im, x, 1, st(4)); px(im, x, 3, BRASS.get(3)); px(im, x, 4, BRASS.get(1)); }
        px(im, 3, 3, BRASS.get(5)); px(im, 9, 3, BRASS.get(5));
        return im;
    }

    static BufferedImage pumpBeam() {
        BufferedImage im = TextureGen.img();
        steelFill(im, 0, 0, 15, 15);
        for (int x = 0; x < 12; x++) { px(im, x, 0, st(5)); px(im, x, 15, st(2)); }
        for (int y = 0; y < 16; y += 5) px(im, 11, y, st(2));
        // Stripe patch: columns 12..14, ONE copper stripe (ramp step 4) at column 13.
        for (int y = 0; y < 16; y++) {
            px(im, 12, y, st(4));
            px(im, 13, y, COPPER.get(4));
            px(im, 14, y, st(2));
            px(im, 15, y, st(3));
        }
        return im;
    }

    static BufferedImage pumpHead() {
        BufferedImage im = TextureGen.img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int s = 5, h = hash(x, y);
                if (h % 4 == 0) s = 4;
                if (h % 17 == 0) px(im, x, y, STEEL.spec());
                else px(im, x, y, st(s));
            }
        for (int x = 0; x < 16; x++) { px(im, x, 0, STEEL.spec()); px(im, x, 15, st(3)); px(im, x, 14, st(4)); }
        for (int y = 0; y < 16; y++) { px(im, 0, y, STEEL.spec()); px(im, 15, y, st(3)); }
        bolt(im, 3, 6); bolt(im, 11, 6);
        return im;
    }

    // ---------------------------------------------------------------- particle

    static BufferedImage spray() {
        BufferedImage im = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 4; y++)
            for (int x = 0; x < 4; x++) {
                if ((x == 0 || x == 3) && (y == 0 || y == 3)) continue;
                int s = x + y >= 4 ? 1 : x + y <= 1 ? 3 : 2;
                im.setRGB(x, y, 0xff000000 | CRUDE.get(s));
            }
        im.setRGB(1, 1, 0xff000000 | CRUDE.spec());
        return im;
    }

    // ---------------------------------------------------------------- GUI

    static final int BAR_X = 130, BAR_Y = 17, BAR_W = 12, BAR_H = 46;

    /** 12x46 fill: crude oil, a lit left edge, darker towards the bottom, a sheen line on top. */
    static BufferedImage depthFill() {
        BufferedImage im = new BufferedImage(BAR_W, BAR_H, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < BAR_H; y++)
            for (int x = 0; x < BAR_W; x++) {
                double t = y / (double) (BAR_H - 1);
                int c = TextureGen.mix(CRUDE.get(4), CRUDE.get(2), t * 0.9);
                if (x == 0) c = TextureGen.mix(c, CRUDE.get(5), 0.5);
                if (x == BAR_W - 1) c = TextureGen.mix(c, CRUDE.get(1), 0.6);
                if (y == 0) c = CRUDE.spec();
                if (y == 1) c = CRUDE.get(5);
                if (((x * 5 + y * 3) % 11) == 0 && x > 1 && x < BAR_W - 2 && y > 2) c = TextureGen.mix(c, CRUDE.spec(), 0.35);
                im.setRGB(x, y, 0xff000000 | c);
            }
        return im;
    }

    static BufferedImage gui() {
        BufferedImage im = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        TextureGen.panel(im, 176, 166);
        TextureGen.slot(im, 80, 20);
        TextureGen.well(im, BAR_X, BAR_Y, BAR_W, BAR_H, 0x2b2b2b);
        // Depth ticks on the right of the empty frame.
        for (int t = 1; t < 5; t++) TextureGen.fill(im, BAR_X + BAR_W - (t == 2 ? 4 : 3), BAR_Y + t * 9, t == 2 ? 3 : 2, 1, TextureGen.SLOT_FILL);
        BufferedImage fill = depthFill();
        for (int y = 0; y < BAR_H; y++) for (int x = 0; x < BAR_W; x++) im.setRGB(176 + x, y, fill.getRGB(x, y));
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) TextureGen.slot(im, 8 + col * 18, 84 + row * 18);
        for (int col = 0; col < 9; col++) TextureGen.slot(im, 8 + col * 18, 142);
        return im;
    }

    // ---------------------------------------------------------------- main

    static final java.util.Map<String, BufferedImage> OUT = new java.util.LinkedHashMap<>();

    static void write(String path, BufferedImage im) throws IOException {
        OUT.put(path, im);
        File f = TextureGen.OUT.resolve(path + ".png").toFile();
        f.getParentFile().mkdirs();
        ImageIO.write(im, "png", f);
    }

    public static void main(String[] args) throws IOException {
        write("block/seismic_charge", charge(false));
        write("block/seismic_charge_lit", charge(true));
        write("item/seismic_charge", chargeItem());
        write("block/wellhead_side", wellheadSide());
        write("block/wellhead_top", wellheadTop());
        write("block/wellhead_drilled_top", wellheadDrilledTop());
        write("block/pump_jack_base", pumpBase());
        write("block/pump_jack_beam", pumpBeam());
        write("block/pump_jack_head", pumpHead());
        write("particle/oil_spray", spray());
        write("gui/wellhead", gui());
        preview();
    }

    static void preview() throws IOException {
        int s = 6, cell = 16 * s + 8, cols = 5, n = OUT.size() - 1;
        BufferedImage sheet = new BufferedImage(cols * cell + 8, 3 * cell + 8 + 256 * 2 + 8, BufferedImage.TYPE_INT_ARGB);
        var g = sheet.createGraphics();
        g.setColor(new java.awt.Color(0x24, 0x24, 0x28));
        g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        int i = 0;
        for (var e : OUT.entrySet()) {
            BufferedImage im = e.getValue();
            if (e.getKey().startsWith("gui/")) {
                g.drawImage(im, 8, 3 * cell + 16, 512, 512, null);
                continue;
            }
            int sc = im.getWidth() == 4 ? 24 : s;
            g.drawImage(im, 8 + (i % cols) * cell, 8 + (i / cols) * cell, im.getWidth() * sc, im.getHeight() * sc, null);
            i++;
        }
        g.dispose();
        File f = new File("build/texturegen/oilfield.png");
        f.getParentFile().mkdirs();
        ImageIO.write(sheet, "png", f);
    }
}
