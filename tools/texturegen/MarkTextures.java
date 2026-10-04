import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Textures for maker's marks: the maker's punch and the mark editor screen. Reuses the helpers and ramps of
 * {@link TextureGen}; it does not call its {@code main}.
 *
 * <p>Run from the repository root:
 * <pre>
 * mkdir -p build/tg &amp;&amp; javac -d build/tg tools/texturegen/TextureGen.java tools/texturegen/MarkTextures.java &amp;&amp; java -cp build/tg MarkTextures
 * </pre>
 */
public final class MarkTextures {
    static final TextureGen.Ramp BRONZE = TextureGen.BRONZE;

    /** A short bronze punch standing on its tip: struck head, shaft, and a cut face with a small mark on it. */
    static BufferedImage punch() {
        BufferedImage im = TextureGen.img();
        // Struck head, mushroomed by use.
        for (int x = 5; x <= 10; x++) TextureGen.px(im, x, 2, BRONZE.get(x == 5 ? 5 : x >= 9 ? 3 : 4));
        for (int x = 4; x <= 11; x++) TextureGen.px(im, x, 3, BRONZE.get(x <= 5 ? 4 : x >= 10 ? 2 : 3));
        // Shaft: lit left, shaded right, slightly narrower towards the tip.
        for (int y = 4; y <= 10; y++) {
            TextureGen.px(im, 6, y, BRONZE.get(5));
            TextureGen.px(im, 7, y, BRONZE.get(4));
            TextureGen.px(im, 8, y, BRONZE.get(3));
            TextureGen.px(im, 9, y, BRONZE.get(2));
        }
        // A few file marks across the grip.
        TextureGen.px(im, 7, 6, BRONZE.get(3));
        TextureGen.px(im, 8, 7, BRONZE.get(2));
        TextureGen.px(im, 7, 8, BRONZE.get(3));
        // Cut face with a small mark in it.
        for (int y = 11; y <= 13; y++)
            for (int x = 6; x <= 9; x++) TextureGen.px(im, x, y, BRONZE.get(y == 11 ? 4 : 3));
        TextureGen.px(im, 7, 12, BRONZE.get(1));
        TextureGen.px(im, 8, 13, BRONZE.get(1));
        TextureGen.px(im, 6, 13, BRONZE.get(2));
        return TextureGen.outline(im);
    }

    /**
     * The editor, 176x112 on a 256x256 sheet: an 8x8 grid of 10 pixel cells at (17,22) and a 32x32 preview at
     * (112,28). Positions match MarkScreen.
     */
    static BufferedImage gui() {
        BufferedImage im = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        TextureGen.panel(im, 176, 112);
        TextureGen.well(im, 16, 21, 82, 82, 0x8b8b8b);
        // Faint guide lines between the cells, so the grid reads as a stamp face.
        for (int i = 1; i < 8; i++) {
            for (int k = 0; k < 80; k++) {
                im.setRGB(17 + i * 10 - 1 + 0, 22 + k, 0xff000000 | 0x7d7d7d);
                im.setRGB(17 + k, 22 + i * 10 - 1, 0xff000000 | 0x7d7d7d);
            }
        }
        TextureGen.well(im, 111, 27, 34, 34, 0xc6c6c6);
        return im;
    }

    public static void main(String[] args) throws IOException {
        TextureGen.itemsV2 = true;
        TextureGen.save("item/maker_punch", punch());
        TextureGen.itemsV2 = false;
        TextureGen.saveRaw("gui/makers_mark", gui());
    }

    private MarkTextures() {}
}
