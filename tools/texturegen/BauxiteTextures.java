import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Random;

/**
 * Bauxite: red-brown laterite with pale pisolites (pea-sized iron-stained nodules) for the bed block,
 * and a rust-coloured lump with pale gibbsite flecks for the item. Reuses the helpers of
 * {@link TextureGen}; it does not call its {@code main}.
 *
 * <p>Run from the repository root:
 * <pre>
 * mkdir -p build/tg &amp;&amp; javac -d build/tg tools/texturegen/TextureGen.java tools/texturegen/BauxiteTextures.java &amp;&amp; java -cp build/tg BauxiteTextures
 * </pre>
 */
public final class BauxiteTextures {
    static final TextureGen.Ramp LATERITE = TextureGen.ramp(0, 0x3a1812, 0x5c2a1e, 0x80402a, 0xa05a38, 0xbe7a52);
    static final TextureGen.Ramp NODULE = TextureGen.ramp(0, 0x7a5040, 0x9c6e56, 0xbc9074, 0xd8b498, 0xecd2b8);

    /** Baked red earth with banding and a scatter of pale two-pixel nodules. */
    static BufferedImage bed() {
        BufferedImage im = TextureGen.V2.paint(
                TextureGen.V2.quantize(TextureGen.V2.terrainField(7171, 0.5), new int[] {0, 22, 44, 26, 8}), LATERITE);
        Random r = new Random(7171);
        for (int i = 0; i < 11; i++) {
            int x = r.nextInt(16), y = r.nextInt(16);
            TextureGen.px(im, x, y, NODULE.get(3));
            TextureGen.px(im, Math.floorMod(x + 1, 16), y, NODULE.get(2));
            if (r.nextInt(3) == 0) TextureGen.px(im, x, Math.floorMod(y + 1, 16), NODULE.get(1));
        }
        return im;
    }

    // Digits 1-5 index the laterite ramp, a-c the nodule ramp.
    static final String[] LUMP = {
            "....3444....",
            "..334544b3..",
            ".23445443332",
            ".2344a4433a2",
            "223444544332",
            "2233444433a1",
            ".22334433321",
            ".122233322b1",
            "..1122222.1.",
    };

    static BufferedImage lump() {
        BufferedImage im = TextureGen.img();
        for (int y = 0; y < LUMP.length; y++) {
            for (int x = 0; x < LUMP[y].length(); x++) {
                char c = LUMP[y].charAt(x);
                if (c == '.') continue;
                TextureGen.px(im, x + 2, y + 3, c >= 'a' ? NODULE.get(c - 'a' + 2) : LATERITE.get(c - '0'));
            }
        }
        return im;
    }

    public static void main(String[] args) throws IOException {
        TextureGen.save("block/bauxite_bed", bed());
        TextureGen.save("item/bauxite", lump());
    }

    private BauxiteTextures() {}
}
