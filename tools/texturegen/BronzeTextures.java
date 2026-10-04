import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Textures of the bronze age touches: the fume hood.
 *
 * <p>Run from the repository root:
 * <pre>
 * mkdir -p build/tg &amp;&amp; javac -d build/tg tools/texturegen/TextureGen.java tools/texturegen/BronzeTextures.java &amp;&amp; java -cp build/tg BronzeTextures
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
                int step = 3 + ((x * 7 + y * 3) % 5 == 0 ? 1 : 0) - ((x * 5 + y * 11) % 7 == 0 ? 1 : 0);
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

    public static void main(String[] args) throws IOException {
        TextureGen.save("block/fume_hood", hood());
    }

    private BronzeTextures() {}
}
