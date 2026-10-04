import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Random;

/**
 * Tier 4 automation textures (spec 13, 21): the inserter's base, gear, arm and claw, and the paper tag
 * that marks a filter. Reuses the helpers and ramps of {@link TextureGen}; it does not call its {@code main}.
 *
 * <p>Run from the repository root:
 * <pre>
 * mkdir -p build/tg &amp;&amp; javac -d build/tg tools/texturegen/TextureGen.java tools/texturegen/AutomationTextures.java &amp;&amp; java -cp build/tg AutomationTextures
 * </pre>
 */
public final class AutomationTextures {
    static int iron(int step) { return TextureGen.WROUGHT_IRON.get(step); }
    static int brass(int step) { return TextureGen.BRASS.get(step); }
    static int wood(int step) { return TextureGen.DARK_WOOD.get(step); }
    static int paper(int step) { return TextureGen.PAPER.get(step); }

    static void px(BufferedImage im, int x, int y, int c) { TextureGen.px(im, x, y, c); }

    static void plate(BufferedImage im, long seed) {
        Random r = new Random(seed);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int step = r.nextInt(6) == 0 ? 4 : r.nextInt(8) == 0 ? 2 : 3;
                if (x == 0 || y == 0) step = 4;
                if (x == 15 || y == 15) step = 2;
                px(im, x, y, iron(step));
            }
    }

    static void rivet(BufferedImage im, int x, int y) {
        px(im, x, y, iron(5));
        px(im, x + 1, y, iron(2));
        px(im, x, y + 1, iron(1));
    }

    /** Plate with a brass band across the middle and rivets, for the base slab's sides. */
    static BufferedImage baseSide() {
        BufferedImage im = TextureGen.img();
        plate(im, 13100);
        for (int x = 0; x < 16; x++) {
            px(im, x, 6, brass(4));
            px(im, x, 7, brass(3));
            px(im, x, 8, brass(2));
        }
        for (int x : new int[] {2, 12}) { rivet(im, x, 2); rivet(im, x, 11); }
        return im;
    }

    static BufferedImage baseTop() {
        BufferedImage im = TextureGen.img();
        plate(im, 13110);
        for (int[] rv : new int[][] {{1, 1}, {13, 1}, {1, 13}, {13, 13}}) rivet(im, rv[0], rv[1]);
        // A bearing seat for the column.
        for (int y = 3; y <= 12; y++)
            for (int x = 3; x <= 12; x++) {
                double d = Math.hypot(x - 7.5, y - 7.5);
                if (d > 5.0 || d < 4.0) continue;
                px(im, x, y, iron(x + y < 15 ? 4 : 1));
            }
        return im;
    }

    /** The underside: a square axle socket in a bronze ring. */
    static BufferedImage baseBottom() {
        BufferedImage im = TextureGen.img();
        plate(im, 13120);
        for (int[] rv : new int[][] {{1, 1}, {13, 1}, {1, 13}, {13, 13}}) rivet(im, rv[0], rv[1]);
        for (int y = 4; y <= 11; y++)
            for (int x = 4; x <= 11; x++) {
                boolean edge = x == 4 || y == 4 || x == 11 || y == 11;
                if (edge) px(im, x, y, TextureGen.BRONZE.get(x == 4 || y == 4 ? 4 : 2));
                else px(im, x, y, TextureGen.BRONZE.get(x == 5 || y == 5 ? 3 : 1));
            }
        for (int y = 6; y <= 9; y++)
            for (int x = 6; x <= 9; x++) px(im, x, y, (x == 6 || y == 6) ? TextureGen.PIG_IRON.get(1) : TextureGen.PIG_IRON.get(2));
        return im;
    }

    /** A round iron column, lit from the upper left. */
    static BufferedImage column() {
        BufferedImage im = TextureGen.img();
        Random r = new Random(13130);
        int[] colStep = {3, 3, 4, 5, 5, 5, 4, 4, 3, 3, 3, 3, 2, 2, 2, 1};
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int step = colStep[x];
                if (r.nextInt(9) == 0) step = Math.max(1, step - 1);
                px(im, x, y, iron(step));
            }
        for (int x = 0; x < 16; x++) { px(im, x, 0, iron(5)); px(im, x, 1, iron(4)); px(im, x, 14, iron(1)); px(im, x, 15, iron(1)); }
        return im;
    }

    /** The brass gear seen from above: a toothed ring around a hub with a square axle hole. */
    static BufferedImage gearTop() {
        BufferedImage im = TextureGen.img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double dx = x - 7.5, dy = y - 7.5, d = Math.hypot(dx, dy);
                double angle = Math.atan2(dy, dx);
                boolean tooth = Math.cos(angle * 8) > 0.15;
                double outer = tooth ? 7.4 : 6.0;
                if (d > outer) continue;
                int step = (dx + dy < 0) ? 4 : 2;
                if (d < 2.8) step = 1;
                else if (d < 4.0) step = (dx + dy < 0) ? 5 : 3;
                else if (d > outer - 1.0) step = (dx + dy < 0) ? 5 : 1;
                px(im, x, y, brass(step));
            }
        for (int y = 6; y <= 9; y++)
            for (int x = 6; x <= 9; x++) px(im, x, y, (x == 6 || y == 6) ? iron(1) : iron(2));
        return im;
    }

    /** Treated wood with long grain running along the arm (the V axis). */
    static BufferedImage arm() {
        BufferedImage im = TextureGen.img();
        Random r = new Random(13140);
        for (int x = 0; x < 16; x++) {
            int base = 3 + (x % 5 == 0 ? -1 : 0) + (x % 7 == 3 ? 1 : 0);
            for (int y = 0; y < 16; y++) {
                int step = base;
                if (r.nextInt(7) == 0) step += r.nextBoolean() ? 1 : -1;
                px(im, x, y, wood(step));
            }
        }
        for (int y = 0; y < 16; y++) { px(im, 0, y, wood(4)); px(im, 15, y, wood(2)); }
        // A knot and two small iron pins where the arm meets the hub.
        for (int[] k : new int[][] {{9, 5, 2}, {10, 5, 1}, {9, 6, 1}, {10, 6, 2}, {8, 5, 3}, {11, 6, 3}}) px(im, k[0], k[1], wood(k[2]));
        for (int[] p : new int[][] {{3, 13}, {12, 13}}) { px(im, p[0], p[1], iron(5)); px(im, p[0] + 1, p[1], iron(2)); }
        return im;
    }

    /** The iron claw: brushed plate with a darker gripping edge. */
    static BufferedImage claw() {
        BufferedImage im = TextureGen.img();
        plate(im, 13150);
        for (int y = 0; y < 16; y++) { px(im, 0, y, iron(5)); px(im, 1, y, iron(4)); px(im, 14, y, iron(1)); px(im, 15, y, iron(1)); }
        for (int x = 2; x < 14; x++) { px(im, x, 12, iron(1)); px(im, x, 13, iron(2)); }
        rivet(im, 6, 4);
        return im;
    }

    /** The small paper tag that shows a filter in the base. */
    static BufferedImage tag() {
        BufferedImage im = TextureGen.img();
        for (int y = 3; y <= 12; y++)
            for (int x = 4; x <= 11; x++) {
                int step = (x + y) % 5 == 0 ? 4 : 3;
                if (x == 4 || y == 3) step = 5;
                if (x == 11 || y == 12) step = 2;
                px(im, x, y, paper(step));
            }
        for (int x = 6; x <= 9; x++) { px(im, x, 6, paper(1)); px(im, x, 8, paper(1)); px(im, x, 10, paper(1)); }
        px(im, 7, 4, iron(2)); px(im, 8, 4, iron(2));
        return im;
    }

    public static void main(String[] args) throws IOException {
        TextureGen.save("block/inserter_base_side", baseSide());
        TextureGen.save("block/inserter_base_top", baseTop());
        TextureGen.save("block/inserter_base_bottom", baseBottom());
        TextureGen.save("block/inserter_column", column());
        TextureGen.save("block/inserter_gear", gearTop());
        TextureGen.save("block/inserter_arm", arm());
        TextureGen.save("block/inserter_claw", claw());
        TextureGen.save("block/inserter_tag", tag());
        System.out.println("automation textures written");
    }
}
