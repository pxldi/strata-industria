import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Random;

/**
 * Ore signs (redesign R7): the ground stains (rust gossan, green malachite bloom, yellow sulfur crust, four
 * drawings each), the same colours as coats over boulders, and black sand. Stains are clear sheets with
 * per-pixel grain, a dark edge on the lower right and a light one on the upper left, no outline. Reuses the
 * helpers of {@link TextureGen}; it does not call its {@code main}.
 *
 * <p>Run from the repository root:
 * <pre>
 * mkdir -p build/tg &amp;&amp; javac -d build/tg tools/texturegen/TextureGen.java tools/texturegen/SignTextures.java &amp;&amp; java -cp build/tg SignTextures
 * </pre>
 */
public final class SignTextures {
    static final TextureGen.Ramp GOSSAN = TextureGen.ramp(0, 0x4a2214, 0x6e3418, 0x93491f, 0xb5642c, 0xd08a48);
    static final TextureGen.Ramp BLOOM = TextureGen.ramp(0, 0x123a2a, 0x1d5a3e, 0x2e8058, 0x4caa78, 0x86d4a0);
    static final TextureGen.Ramp CRUST = TextureGen.ramp(0, 0x6a5e18, 0x938520, 0xbcab2c, 0xdccc44, 0xf2e676);
    static final TextureGen.Ramp BLACK = TextureGen.ramp(0, 0x16161c, 0x22222a, 0x2e2e38, 0x3c3c48, 0x4e4e5c);

    /** Replaces each value by its rank in 0-1, so a threshold is a share of the tile. */
    static double[][] rank(double[][] f) {
        Integer[] order = new Integer[256];
        for (int i = 0; i < 256; i++) order[i] = i;
        java.util.Arrays.sort(order, java.util.Comparator.comparingDouble(i -> f[i / 16][i % 16]));
        double[][] out = new double[16][16];
        for (int k = 0; k < 256; k++) out[order[k] / 16][order[k] % 16] = (k + 0.5) / 256;
        return out;
    }

    /**
     * A clear sheet with a patch drawn in {@code ramp}: {@code cover} is the share of the tile it fills, the
     * smear direction stretches it into streaks, and the edge pass lights the upper left and shades the lower
     * right.
     */
    static BufferedImage stain(long seed, TextureGen.Ramp ramp, int smearX, int smearY, double cover) {
        double[][] shape = TextureGen.V2.terrainField(seed, 0.28);
        if (smearX != 0 || smearY != 0) shape = TextureGen.V2.smear(shape, smearX, smearY, 0.7);
        shape = rank(shape);
        double[][] tone = rank(TextureGen.V2.terrainField(seed + 77, 0.55));
        int[][] step = new int[16][16];
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                if (shape[y][x] >= cover) continue;
                double t = tone[y][x];
                step[y][x] = t < 0.10 ? 1 : t < 0.38 ? 2 : t < 0.78 ? 3 : t < 0.94 ? 4 : 5;
            }
        }
        BufferedImage im = TextureGen.img();
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int s = step[y][x];
                if (s == 0) continue;
                boolean shade = x + 1 < 16 && y + 1 < 16 && step[y + 1][x + 1] == 0;
                boolean light = x > 0 && y > 0 && step[y - 1][x - 1] == 0;
                if (shade) s = Math.max(1, s - 1);
                else if (light) s = Math.min(5, s + 1);
                TextureGen.px(im, x, y, ramp.get(s));
            }
        }
        return im;
    }

    /** Dark sand with a few pale glints and warm specks of cassiterite. */
    static BufferedImage blackSand() {
        double[][] n = rank(TextureGen.V2.terrainField(9393, 0.7));
        BufferedImage im = TextureGen.img();
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                double t = n[y][x];
                TextureGen.px(im, x, y, BLACK.get(t < 0.10 ? 1 : t < 0.35 ? 2 : t < 0.75 ? 3 : t < 0.95 ? 4 : 5));
            }
        }
        Random r = new Random(9394);
        for (int k = 0; k < 5; k++) TextureGen.px(im, r.nextInt(16), r.nextInt(16), 0xc4c8d4);
        for (int k = 0; k < 5; k++) TextureGen.px(im, r.nextInt(16), r.nextInt(16), 0x9a7a52);
        return im;
    }

    public static void main(String[] args) throws IOException {
        for (int v = 0; v < 4; v++) {
            // Gossan streaks down, bloom streaks along a slant, the crust is crumbs.
            TextureGen.save("block/gossan_" + v, stain(7100 + v, GOSSAN, 0, 2, 0.46));
            TextureGen.save("block/malachite_bloom_" + v, stain(7200 + v, BLOOM, 1, 1, 0.42));
            TextureGen.save("block/sulfur_crust_" + v, stain(7300 + v, CRUST, 0, 0, 0.40));
        }
        TextureGen.save("block/boulder_gossan", stain(7400, GOSSAN, 0, 3, 0.50));
        TextureGen.save("block/boulder_bloom", stain(7500, BLOOM, 0, 2, 0.46));
        TextureGen.save("block/black_sand", blackSand());
    }

    private SignTextures() {}
}
