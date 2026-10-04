import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Textures for the indicator plants: copper flower, horsetail, stunted birch, locoweed and pink thrift.
 * Drawn the way vanilla draws small plants: cross-model sprites with no outline, three or four greens
 * with a lit top-left, the flower or leaf colour as the only saturated part. Reuses the helpers of
 * {@link TextureGen}; it does not call its {@code main}.
 *
 * <p>Run from the repository root:
 * <pre>
 * mkdir -p build/tg &amp;&amp; javac -d build/tg tools/texturegen/TextureGen.java tools/texturegen/PlantTextures.java &amp;&amp; java -cp build/tg PlantTextures
 * </pre>
 */
public final class PlantTextures {
    // Stem and leaf greens, dark to light (vanilla flower stems sit in this band).
    static final int[] GREEN = {0x1f4d16, 0x2d6a1d, 0x3f8a27, 0x57a638};
    static final int[] GREY_GREEN = {0x3b5a3a, 0x58795a, 0x79987a, 0x9bb59a};
    static final int[] BLUE = {0x4a4cb0, 0x6468d0, 0x858af0, 0xb0b4ff};
    static final int[] YELLOW = {0x8a7a1a, 0xb09f27, 0xd2c340, 0xece06a};
    static final int[] BARK = {0x2c2a28, 0x6e6a62, 0xb8b4aa, 0xe4e1d8};
    static final int[] VIOLET = {0x5a2c80, 0x7b45a8, 0x9d66cc, 0xc597ec};
    static final int[] PINK = {0xb54d78, 0xd96b9a, 0xf09bbd, 0xfdd0e0};

    /** Draws rows of digits: 0-3 index the first palette, a-d the second, '.' is clear. */
    static BufferedImage draw(String[] rows, int[] a, int[] b) {
        BufferedImage im = TextureGen.img();
        for (int y = 0; y < rows.length; y++) {
            for (int x = 0; x < rows[y].length(); x++) {
                char c = rows[y].charAt(x);
                if (c == '.') continue;
                int rgb = c >= 'a' ? b[c - 'a'] : a[c - '0'];
                TextureGen.px(im, x, y, rgb);
            }
        }
        return im;
    }

    // Small blue-purple mint: square stem, paired leaves, a spike of tiny flowers.
    static final String[] COPPER_FLOWER = {
            "................",
            "................",
            "......ab........",
            ".....bcba.......",
            "......bcb.......",
            ".....abcba......",
            "......ab1.......",
            ".....11b........",
            "..12.21.1.......",
            ".2321.2.12......",
            "..22.12.232.....",
            ".....12..22.....",
            "....2312........",
            ".....12.........",
            ".....12.........",
            "....1212........"};

    // Segmented grey-green stalks with thin whorls of branchlets at each joint.
    static final String[] HORSETAIL = {
            ".......1........",
            "..1....2....1...",
            "..2...121...2...",
            ".121..1.1..121..",
            "..2..1.2.1..2...",
            "..1....2....1...",
            ".121..121..121..",
            "..2....2....2...",
            "..1...121...1...",
            ".121....2..121..",
            "..2.....2...2...",
            "..1....121..1...",
            "..2.....1...2...",
            "..1.....2...1...",
            "..2.....2...2...",
            "..1.....1...1..."};

    // A stunted birch sapling: two thin pale stems with black flecks, a few withered yellow leaves.
    static final String[] STUNTED_BIRCH = {
            "................",
            "................",
            "....bcb.........",
            "...bcdcb..bc....",
            "...cbcbcbbcdb...",
            "....bcdbcbcb....",
            "..b...1.2..b...",
            ".......12..cb...",
            "....c.212.......",
            "...bc.212.......",
            "......1.2.......",
            "......212.......",
            "......122.......",
            ".....2.12.......",
            "......012.......",
            "......012......."};

    // Low rosette of ragged pinnate leaves with a spike of violet pea flowers.
    static final String[] LOCOWEED = {
            "................",
            ".........b......",
            "........bcb.....",
            "........bcd.....",
            ".......abcb.....",
            "........bc1.....",
            ".......a.1b.....",
            "........11......",
            "..1..2..12.2....",
            ".121.12.12.121..",
            "..2121.212.21...",
            "...2221212221...",
            "....22322322....",
            ".....2132312....",
            "......0121......",
            ".......11......."};

    // Cushion of grass tufts with round pink-white heads on thin stems.
    static final String[] PINK_THRIFT = {
            "................",
            "................",
            "................",
            "....bc.....bc...",
            "...bcdb...bcdb..",
            "...abcb...abcb..",
            "....1.......1...",
            "....2....bc.2...",
            "....1...bcdb1...",
            ".2..2...abcb2...",
            "121.1....1..12..",
            ".2121.2..2.2121.",
            "..2212122212221.",
            "..122212122122..",
            "...1232123212...",
            "....011211211..."};

    public static void main(String[] args) throws IOException {
        TextureGen.save("block/copper_flower", draw(COPPER_FLOWER, GREEN, BLUE));
        TextureGen.save("block/horsetail", draw(HORSETAIL, GREY_GREEN, GREEN));
        TextureGen.save("block/stunted_birch", draw(STUNTED_BIRCH, BARK, YELLOW));
        TextureGen.save("block/locoweed", draw(LOCOWEED, GREEN, VIOLET));
        TextureGen.save("block/pink_thrift", draw(PINK_THRIFT, GREEN, PINK));
    }

    private PlantTextures() {}
}
