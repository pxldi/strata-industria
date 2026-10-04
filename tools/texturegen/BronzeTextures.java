import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Textures of the bronze age touches: the fume hood, the bell and its mold, and the specimen cabinet.
 *
 * <p>Run from the repository root:
 * <pre>
 * mkdir -p build/tg &amp;&amp; javac -d build/tg tools/texturegen/TextureGen.java tools/texturegen/SharedBlockTextures.java tools/texturegen/BronzeTextures.java &amp;&amp; java -cp build/tg BronzeTextures
 * </pre>
 */
public final class BronzeTextures {
    static final TextureGen.Ramp COPPER = TextureGen.COPPER;
    /** Dark soot streak colour at the vent. */
    static final int SOOT = 0x2a1a12;

    /** Sheet copper: two riveted panels with a seam between, lit from the top left, a little grain in the metal. */
    static BufferedImage hood() {
        BufferedImage im = TextureGen.img();
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int step = 3 + ((x * 7 + y * 3) % 11 == 0 ? 1 : 0) - ((x * 5 + y * 11) % 13 == 0 ? 1 : 0);
                if (y == 0 || x == 0) step = 4;
                if (y == 15 || x == 15) step = 2;
                if (y == 7) step = 1;
                if (y == 8) step = 4;
                TextureGen.px(im, x, y, COPPER.get(step));
            }
        }
        // Rivets along both panel edges.
        for (int x : new int[] {2, 5, 8, 11, 13}) {
            for (int y : new int[] {2, 12}) {
                TextureGen.px(im, x, y, COPPER.get(5));
                TextureGen.px(im, x + 1 > 15 ? 15 : x + 1, y + 1 > 15 ? 15 : y + 1, COPPER.get(1));
            }
        }
        // Soot creeping up from the seam where the fumes pass.
        for (int x = 5; x <= 10; x++) TextureGen.px(im, x, 6, x % 2 == 0 ? COPPER.get(1) : SOOT);
        TextureGen.px(im, 7, 5, SOOT);
        TextureGen.px(im, 8, 5, COPPER.get(1));
        return im;
    }

    /** Bell cavity, eight wide and six tall: a crown, a waisted body and a flared lip. */
    static final String[] BELL_CAVITY = {
            "..####..",
            "..####..",
            ".######.",
            ".######.",
            "########",
            "########",
    };

    /** Cast bronze, lit from the left: a bright band, a dark right flank, rim lines near the top and bottom. */
    static BufferedImage bell() {
        BufferedImage im = TextureGen.img();
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int step = x < 3 ? 4 : x < 7 ? 5 : x < 11 ? 4 : x < 14 ? 3 : 2;
                if ((x * 5 + y * 9) % 17 == 0) step = Math.max(1, step - 1);
                if ((x * 3 + y * 7) % 19 == 0) step = Math.min(5, step + 1);
                if (y == 2 || y == 13) step = Math.max(1, step - 2);
                if (y == 3 || y == 14) step = Math.min(5, step + 1);
                TextureGen.px(im, x, y, TextureGen.BRONZE.get(step));
            }
        }
        // A few green flecks where the bronze has begun to weather.
        for (int[] f : new int[][] {{3, 9}, {4, 10}, {11, 6}, {12, 11}, {8, 12}}) TextureGen.px(im, f[0], f[1], TextureGen.MALACHITE.get(3));
        return im;
    }

    /** Dark iron for the bell's hanger and clapper. */
    static BufferedImage bellIron() {
        BufferedImage im = TextureGen.img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) TextureGen.px(im, x, y, TextureGen.V2.STEEL_V2.get(x < 5 ? 3 : x < 11 ? 2 : 1));
        return im;
    }

    /** Specimen colours on the shelves, cycled: grey granite, pale limestone, dark basalt, green malachite, and so on. */
    static final int[] SPECIMENS = {0x978480, 0xc1baa5, 0x53565d, 0x4a9466, 0xe08848, 0x9a8070, 0x92a2b0, 0xc06a4a, 0x8088a0, 0xe8bc34,
            0x7a6e66, 0xd8c83c, 0x6f7a73, 0xdad8d2, 0xb59d92, 0x5f656e};
    static final int[] SPRUCE = SharedBlockTextures.SPRUCE;

    /** Cabinet side: vertical spruce boards with a dark seam every five pixels. */
    static BufferedImage cabinetSide() {
        BufferedImage im = TextureGen.img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int step = 3 + (x % 5 == 0 ? -2 : 0) + ((x * 7 + y * 5) % 9 == 0 ? 1 : 0) - ((x * 3 + y * 11) % 8 == 0 ? 1 : 0);
                TextureGen.px(im, x, y, SPRUCE[Math.max(0, Math.min(5, step))]);
            }
        return im;
    }

    /** Cabinet top: lighter boards laid across. */
    static BufferedImage cabinetTop() {
        BufferedImage im = TextureGen.img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int step = 4 + (y % 4 == 0 ? -2 : 0) + ((x * 5 + y * 7) % 10 == 0 ? 1 : 0) - ((x * 11 + y * 3) % 9 == 0 ? 1 : 0);
                TextureGen.px(im, x, y, SPRUCE[Math.max(0, Math.min(5, step))]);
            }
        return im;
    }

    /**
     * The glass front: a spruce frame round four shelves of six cells, the cells filling from the bottom shelf up as
     * the collection grows ({@code filled} of 24), and two pale glints across the glass.
     */
    static BufferedImage cabinetFront(int filled) {
        BufferedImage im = TextureGen.img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                boolean edge = x == 0 || y == 0 || x == 15 || y == 15;
                int step = edge ? (x == 15 || y == 15 ? 1 : 3) : 2;
                TextureGen.px(im, x, y, SPRUCE[step]);
            }
        // Dark inside the glass, with a shelf board under each row.
        for (int y = 2; y <= 13; y++)
            for (int x = 2; x <= 13; x++) TextureGen.px(im, x, y, 0x1e140b + ((x + y) % 2 == 0 ? 0x040302 : 0));
        for (int row = 0; row < 4; row++) {
            int y0 = 2 + 3 * row;
            for (int x = 2; x <= 13; x++) TextureGen.px(im, x, y0 + 2, SPRUCE[x % 2 == 0 ? 4 : 3]);
        }
        int cell = 0;
        for (int row = 3; row >= 0; row--) {
            for (int c = 0; c < 6; c++, cell++) {
                if (cell >= filled) continue;
                int colour = SPECIMENS[(cell * 5 + row) % SPECIMENS.length];
                int x0 = 2 + 2 * c, y0 = 2 + 3 * row;
                TextureGen.px(im, x0, y0, lighten(colour, 1.2f));
                TextureGen.px(im, x0 + 1, y0, colour);
                TextureGen.px(im, x0, y0 + 1, colour);
                TextureGen.px(im, x0 + 1, y0 + 1, lighten(colour, 0.7f));
            }
        }
        // Glints on the glass.
        for (int i = 0; i < 3; i++) {
            for (int[] g : new int[][] {{3 + i, 4 - i}, {9 + i, 11 - i}}) {
                int rgb = im.getRGB(g[0], g[1]) & 0xFFFFFF;
                // Only on the dark glass; the boards and the specimens stay as they are.
                if (((rgb >> 16) & 255) < 0x30) TextureGen.px(im, g[0], g[1], 0x3c2e22);
            }
        }
        return im;
    }

    static int lighten(int rgb, float f) {
        int r = Math.min(255, Math.round(((rgb >> 16) & 255) * f)), g = Math.min(255, Math.round(((rgb >> 8) & 255) * f)),
                b = Math.min(255, Math.round((rgb & 255) * f));
        return r << 16 | g << 8 | b;
    }

    public static void main(String[] args) throws IOException {
        TextureGen.save("block/fume_hood", hood());
        TextureGen.save("block/cabinet_side", cabinetSide());
        TextureGen.save("block/cabinet_top", cabinetTop());
        for (int fill = 0; fill <= 4; fill++) TextureGen.save("block/cabinet_front_" + fill, cabinetFront(fill * 6));
        TextureGen.save("block/bell", bell());
        TextureGen.save("block/bell_iron", bellIron());
        TextureGen.save("item/unfired_bell_mold", TextureGen.mold(TextureGen.CLAY, BELL_CAVITY));
        TextureGen.save("item/bell_mold", TextureGen.mold(TextureGen.CERAMIC, BELL_CAVITY));
        TextureGen.save("item/bell_mold_filled", TextureGen.filledMold(TextureGen.CERAMIC, BELL_CAVITY));
    }

    private BronzeTextures() {}
}
