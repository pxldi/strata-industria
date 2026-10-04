import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Textures for the builder's ledger: the book and the builder's crate. The crate is the structure crate
 * ({@link SharedBlockTextures#crate}) with bronze corner caps and a paper label in place of the rope grip.
 *
 * <p>Run from the repository root:
 * <pre>
 * mkdir -p build/tg &amp;&amp; javac -d build/tg tools/texturegen/TextureGen.java tools/texturegen/SharedBlockTextures.java tools/texturegen/LedgerTextures.java &amp;&amp; java -cp build/tg LedgerTextures
 * </pre>
 */
public final class LedgerTextures {
    static final TextureGen.Ramp BRONZE = TextureGen.BRONZE;
    static final TextureGen.Ramp PAPER = TextureGen.PAPER;
    /** Dark green cloth binding. */
    static final int[] CLOTH = {0x1b3326, 0x27483a, 0x335b49, 0x426f59, 0x538468};
    static final int RIBBON = 0xa8362a, RIBBON_DARK = 0x6e231b, PENCIL = 0x4a4540;

    /** A cloth-bound book lying flat: bronze corners, a pale page edge on the right and bottom, a label and a ribbon. */
    static BufferedImage ledger() {
        BufferedImage im = TextureGen.img();
        // Cover, lit from the top left.
        for (int y = 2; y <= 12; y++)
            for (int x = 3; x <= 11; x++) {
                int step = 3;
                if (x == 3 || y == 2) step = 4;
                if (x == 11 || y == 12) step = 2;
                if (x == 4) step = 1;
                TextureGen.px(im, x, y, CLOTH[step]);
            }
        // Page block showing below and to the right of the cover.
        for (int y = 3; y <= 13; y++) for (int x = 12; x <= 13; x++) TextureGen.px(im, x, y, PAPER.get(x == 12 ? 5 : 3));
        for (int x = 4; x <= 13; x++) TextureGen.px(im, x, 13, PAPER.get(x % 3 == 0 ? 4 : 5));
        for (int x = 5; x <= 13; x++) TextureGen.px(im, x, 14, PAPER.get(2));
        TextureGen.px(im, 14, 4, PAPER.get(2));
        for (int y = 5; y <= 13; y++) TextureGen.px(im, 14, y, PAPER.get(2));
        // Bronze corners.
        for (int[] c : new int[][] {{3, 2}, {11, 2}, {3, 12}, {11, 12}}) {
            TextureGen.px(im, c[0], c[1], BRONZE.get(5));
            TextureGen.px(im, c[0] == 3 ? 4 : 10, c[1], BRONZE.get(4));
            TextureGen.px(im, c[0], c[1] == 2 ? 3 : 11, BRONZE.get(3));
        }
        // Paper label with two pencil lines.
        for (int y = 5; y <= 8; y++) for (int x = 6; x <= 9; x++) TextureGen.px(im, x, y, PAPER.get(y == 5 ? 5 : 4));
        for (int x = 6; x <= 9; x++) TextureGen.px(im, x, 5, PAPER.get(5));
        TextureGen.px(im, 6, 6, PENCIL);
        TextureGen.px(im, 7, 6, PENCIL);
        TextureGen.px(im, 8, 6, PENCIL);
        TextureGen.px(im, 6, 7, PENCIL);
        TextureGen.px(im, 7, 7, PENCIL);
        // Ribbon marker hanging out below the pages.
        TextureGen.px(im, 8, 14, RIBBON);
        TextureGen.px(im, 8, 15, RIBBON);
        TextureGen.px(im, 9, 15, RIBBON_DARK);
        return TextureGen.outline(im);
    }

    /** The structure crate with a paper label on the side and bronze corner caps on the top. */
    static BufferedImage crate(boolean side) {
        BufferedImage im = SharedBlockTextures.crate(side, false);
        if (side) {
            // Cover the rope grip with a tacked paper label and two pencilled lines.
            for (int y = 4; y <= 10; y++) for (int x = 4; x <= 11; x++) TextureGen.px(im, x, y, PAPER.get(y == 4 || x == 4 ? 5 : y == 10 || x == 11 ? 3 : 4));
            for (int x = 5; x <= 9; x++) TextureGen.px(im, x, 6, PENCIL);
            for (int x = 5; x <= 7; x++) TextureGen.px(im, x, 8, PENCIL);
            TextureGen.px(im, 9, 8, PENCIL);
            TextureGen.px(im, 4, 4, BRONZE.get(4));
            TextureGen.px(im, 11, 4, BRONZE.get(3));
            // Bronze corner straps along the lower battens.
            for (int y = 12; y <= 14; y++) {
                TextureGen.px(im, 1, y, BRONZE.get(y == 12 ? 5 : 3));
                TextureGen.px(im, 14, y, BRONZE.get(y == 12 ? 4 : 2));
            }
        } else {
            for (int[] c : new int[][] {{0, 0}, {13, 0}, {0, 13}, {13, 13}}) {
                for (int dy = 0; dy < 3; dy++)
                    for (int dx = 0; dx < 3; dx++) {
                        if (dx + dy > 3) continue;
                        int x = c[0] + dx, y = c[1] + dy;
                        TextureGen.px(im, x, y, BRONZE.get(dx == 0 || dy == 0 ? 5 : dx + dy == 3 ? 2 : 4));
                    }
            }
        }
        return im;
    }

    public static void main(String[] args) throws IOException {
        TextureGen.itemsV2 = true;
        TextureGen.save("item/builders_ledger", ledger());
        TextureGen.itemsV2 = false;
        TextureGen.save("block/builders_crate_side", crate(true));
        TextureGen.save("block/builders_crate_top", crate(false));
    }

    private LedgerTextures() {}
}
