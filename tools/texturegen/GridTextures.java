import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Textures of the tier 5 extras: the electric lamp, the Leyden jar, the stethoscope and the glowing overhead span.
 *
 * <p>Run from the repository root:
 * <pre>
 * mkdir -p build/tg &amp;&amp; javac -d build/tg tools/texturegen/TextureGen.java tools/texturegen/GridTextures.java &amp;&amp; java -cp build/tg GridTextures
 * </pre>
 */
public final class GridTextures {
    static final TextureGen.Ramp BRASS = TextureGen.BRASS;
    static final TextureGen.Ramp COPPER = TextureGen.COPPER;
    static final TextureGen.Ramp STEEL = TextureGen.STEEL;
    static final TextureGen.Ramp RUBBER = TextureGen.ramp(0, 0x1a1816, 0x2c2826, 0x433b37, 0x5e534c, 0x7c6e64);
    static final TextureGen.Ramp LEAD = TextureGen.LEAD;
    /** Pale greenish glass of a Leyden jar, five steps. */
    static final TextureGen.Ramp GLASS = TextureGen.ramp(0xf0fcfa, 0x3c5a5c, 0x5c8284, 0x7ea8a8, 0xa4cccc, 0xc8e6e4);
    /** Cork of the stopper. */
    static final TextureGen.Ramp CORK = TextureGen.ramp(0, 0x4a3420, 0x6a4a2c, 0x8a6638, 0xa8844a, 0xc4a064);
    /** Charge in a jar: electric blue, dark to white. */
    static final int[] SPARK = {0x2a4a8a, 0x4a7ad0, 0x86b4f4, 0xcce4ff};
    /** A line at its limit: dull red to a warm orange. */
    static final int[] HOT = {0x6a2a20, 0xa04428, 0xd06a34, 0xee9a50};

    /** Lamp: a brass frame with rivets round a pane; lit, the pane is a warm white with the filament showing. */
    static BufferedImage lamp(boolean on) {
        BufferedImage im = TextureGen.img();
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                boolean frame = x < 2 || y < 2 || x > 13 || y > 13;
                int col;
                if (frame) {
                    int step = 3 + ((x * 7 + y * 3) % 5 == 0 ? 1 : 0) - ((x * 5 + y * 11) % 7 == 0 ? 1 : 0);
                    if (x == 0 || y == 0) step = 4;
                    if (x == 15 || y == 15) step = 2;
                    col = BRASS.get(Math.max(1, Math.min(5, step)));
                } else {
                    int h = Math.floorMod(x * 73 + y * 151 + x * y * 17 + 11, 97);
                    if (on) {
                        double d = Math.hypot(x - 7.5, y - 7.5) / 7.0;
                        int[] warm = {0xf0b050, 0xf8cc70, 0xfce09a, 0xfff2c4};
                        int step = d < 0.45 ? 3 : d < 0.8 ? 2 : 1;
                        if (h % 7 == 0) step = Math.max(0, step - 1);
                        col = warm[step];
                    } else {
                        int[] dull = {0x5c5a50, 0x6a685c, 0x787468};
                        col = dull[h % 5 == 0 ? 0 : h % 3 == 0 ? 2 : 1];
                    }
                }
                TextureGen.px(im, x, y, col);
            }
        }
        // Rivets at the four corners of the frame.
        for (int[] r : new int[][] {{0, 0}, {14, 0}, {0, 14}, {14, 14}}) TextureGen.px(im, r[0] + 1 - (r[0] == 14 ? 0 : 0), r[1] + 1, BRASS.get(5));
        // The filament: a zigzag of copper wire, dark when cold and white hot when lit.
        int[][] zig = {{5, 10}, {6, 8}, {7, 6}, {8, 8}, {9, 6}, {10, 8}, {10, 10}};
        for (int[] p : zig) TextureGen.px(im, p[0], p[1], on ? 0xfffbe0 : COPPER.get(2));
        TextureGen.px(im, 5, 11, on ? 0xfff2c4 : COPPER.get(3));
        TextureGen.px(im, 10, 11, on ? 0xfff2c4 : COPPER.get(3));
        return im;
    }

    /**
     * Leyden jar atlas: side of the body at 0..7 x 0..9, top and bottom of the body at 8..15 x 0..7, neck at 0..3 x 10..11,
     * cork at 4..7 x 10..13, rod at 8 x 8..9 and knob at 8..9 x 10..11. Charge 0-4 lights a thread of sparks up the middle.
     */
    static BufferedImage jar(int charge) {
        BufferedImage im = TextureGen.img();
        // Body side: glass with a metal lining over the lower two thirds, a bright streak down the lit edge.
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 8; x++) {
                int step = x == 0 ? 5 : x == 1 ? 4 : x < 6 ? 3 : x == 6 ? 2 : 1;
                if ((x * 5 + y * 3) % 7 == 0) step = Math.max(1, step - 1);
                int col = GLASS.get(step);
                if (y >= 3 && y <= 8 && x >= 1 && x <= 6) col = LEAD.get(x < 3 ? 4 : x < 5 ? 3 : 2);
                if (y == 9) col = GLASS.get(1);
                TextureGen.px(im, x, y, col);
            }
        }
        // The thread of charge up the middle of the lining, one pair of pixels per charge step.
        for (int i = 0; i < charge; i++) {
            int y = 8 - i * 2;
            int col = SPARK[Math.min(3, i)];
            TextureGen.px(im, 3, y, col);
            TextureGen.px(im, 4, y, SPARK[Math.min(3, i + 1)]);
            TextureGen.px(im, 3, y - 1, SPARK[Math.min(3, i)]);
            TextureGen.px(im, 4, y - 1, col);
        }
        // Top and bottom of the body.
        for (int y = 0; y < 8; y++)
            for (int x = 8; x < 16; x++) TextureGen.px(im, x, y, GLASS.get(((x + y) % 3 == 0) ? 3 : 2));
        // Neck.
        for (int y = 10; y < 12; y++) for (int x = 0; x < 4; x++) TextureGen.px(im, x, y, GLASS.get(x == 0 ? 4 : x < 3 ? 3 : 2));
        // Cork.
        for (int y = 10; y < 14; y++)
            for (int x = 4; x < 8; x++) TextureGen.px(im, x, y, CORK.get(((x * 3 + y * 5) % 5 == 0) ? 2 : ((x + y) % 2 == 0) ? 3 : 4));
        // Rod and knob, copper.
        TextureGen.px(im, 8, 8, COPPER.get(4));
        TextureGen.px(im, 8, 9, COPPER.get(3));
        for (int y = 10; y < 12; y++) for (int x = 8; x < 10; x++) TextureGen.px(im, x, y, COPPER.get(x == 8 ? 5 : 4));
        return im;
    }

    /** A span at its limit: aluminium strands with a red-orange heat running through them. */
    static BufferedImage hotLine() {
        BufferedImage im = TextureGen.img();
        int[] step = {3, 2, 1, 2};
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) TextureGen.px(im, x, y, HOT[Math.max(0, Math.min(3, step[(x + y) & 3] + ((x * 3 + y) % 9 == 0 ? 1 : 0)))]);
        return im;
    }

    /**
     * The stethoscope: two steel earpieces, arms that meet in a Y, a rubber tube and a round steel chestpiece with a brass
     * diaphragm, on the usual item diagonal.
     */
    static BufferedImage stethoscope() {
        BufferedImage im = TextureGen.img();
        // Earpieces and arms of the binaural, steel, joining at the top of the tube.
        int[][] left = {{3, 1}, {3, 2}, {4, 3}, {4, 4}, {5, 5}, {5, 6}};
        int[][] right = {{9, 1}, {9, 2}, {8, 3}, {8, 4}, {7, 5}, {7, 6}};
        for (int[] p : left) TextureGen.px(im, p[0], p[1], STEEL.get(p[1] < 2 ? 5 : 4));
        for (int[] p : right) TextureGen.px(im, p[0], p[1], STEEL.get(p[1] < 2 ? 4 : 3));
        // Rubber tube leaving the junction, curving to the lower right.
        int[][] tube = {{6, 6}, {6, 7}, {6, 8}, {7, 9}, {8, 10}};
        for (int i = 0; i < tube.length; i++) TextureGen.px(im, tube[i][0], tube[i][1], RUBBER.get(i % 2 == 0 ? 4 : 3));
        // Chestpiece: steel rim round a brass diaphragm.
        double cx = 11.0, cy = 11.5;
        for (int y = 8; y < 15; y++) {
            for (int x = 8; x < 15; x++) {
                double d = Math.hypot(x - cx, y - cy);
                if (d <= 1.6) TextureGen.px(im, x, y, BRASS.get(d < 0.8 ? 5 : 4));
                else if (d <= 3.0) TextureGen.px(im, x, y, STEEL.get(x + y < 22 ? 5 : 3));
            }
        }
        return TextureGen.outline(im);
    }

    public static void main(String[] args) throws IOException {
        TextureGen.itemsV2 = true;
        TextureGen.save("block/electric_lamp_off", lamp(false));
        TextureGen.save("block/electric_lamp_on", lamp(true));
        for (int charge = 0; charge <= 4; charge++) TextureGen.save("block/leyden_jar_" + charge, jar(charge));
        TextureGen.save("block/acsr_line_hot", hotLine());
        TextureGen.save("item/stethoscope", stethoscope());
        TextureGen.preview();
    }

    private GridTextures() {}
}
