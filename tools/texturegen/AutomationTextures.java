import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Random;

/**
 * Tier 4 automation textures (spec 13, 21): the inserter's base, gear, arm and claw, the paper tag
 * that marks a filter, and the conveyor belt's frame and its four leather top frames. Reuses the helpers and ramps of {@link TextureGen}; it does not call its {@code main}.
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


    static int leather(int step) { return TextureGen.LEATHER.get(step); }

    /**
     * One frame of the belt's leather top. The ribs run across the belt, 4 px apart, and frame {@code f} has
     * them shifted {@code f} px towards the front, so the renderer's four frames make the belt run. Only
     * columns 2 to 13 show on the model; the stitched seams sit at its edges.
     */
    static BufferedImage beltTop(int f) {
        BufferedImage im = TextureGen.img();
        Random r = new Random(13200);
        for (int y = 0; y < 16; y++) {
            int k = Math.floorMod(y + f, 4);
            for (int x = 0; x < 16; x++) {
                int step = switch (k) {
                    case 0 -> 4;   // the lit ridge of the rib
                    case 1 -> 3;
                    case 2 -> 2;
                    default -> 1;  // the dark crease between ribs
                };
                // Wear and sheen that stay with the leather as it moves, so they come from the pattern row, not the screen row.
                long seed = 977L * x + 131L * Math.floorDiv(y + f, 4) + 13L * k;
                int wear = (int) Math.floorMod(seed * 2654435761L >>> 7, 11L);
                if (k == 0 && wear == 0) step = 5;
                if (k == 2 && wear == 1) step = 3;
                if (k == 1 && wear == 2) step = 2;
                px(im, x, y, leather(step));
            }
            // Stitched seams along both edges: a thread every other row.
            boolean stitch = k == 0 || k == 2;
            px(im, 2, y, stitch ? paper(4) : leather(1));
            px(im, 13, y, stitch ? paper(3) : leather(1));
            px(im, 3, y, leather(2));
            px(im, 12, y, leather(1));
        }
        return im;
    }

    /** The leather's thin side, where it wraps over the bed. */
    static BufferedImage beltEdge() {
        BufferedImage im = TextureGen.img();
        Random r = new Random(13210);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) px(im, x, y, leather(r.nextInt(5) == 0 ? 3 : 2));
        for (int x = 0; x < 16; x += 4) px(im, x, 12, leather(4));
        return im;
    }

    /** Treated planks seen from above on the rails: grain runs along the belt. */
    static BufferedImage beltRail() {
        BufferedImage im = TextureGen.img();
        Random r = new Random(13220);
        for (int x = 0; x < 16; x++) {
            int base = 3 + (x % 5 == 1 ? -1 : 0) + (x % 7 == 3 ? 1 : 0);
            for (int y = 0; y < 16; y++) {
                int step = base;
                if (r.nextInt(6) == 0) step += r.nextBoolean() ? 1 : -1;
                px(im, x, y, wood(Math.max(1, Math.min(5, step))));
            }
        }
        for (int y = 0; y < 16; y++) { px(im, 0, y, wood(4)); px(im, 1, y, wood(4)); }
        return im;
    }

    /** The rail's outer face, rows 11 to 15 of the sheet: a plank edge with two brass nails. */
    static BufferedImage beltSide() {
        BufferedImage im = TextureGen.img();
        Random r = new Random(13230);
        for (int y = 0; y < 16; y++) {
            int base = y == 11 ? 4 : y == 15 ? 1 : y == 14 ? 2 : 3;
            for (int x = 0; x < 16; x++) {
                int step = base;
                if (y > 11 && y < 14 && r.nextInt(5) == 0) step += r.nextBoolean() ? 1 : -1;
                if (y > 11 && y < 14 && x % 8 == 3 && r.nextBoolean()) step -= 1;
                px(im, x, y, wood(Math.max(1, Math.min(5, step))));
            }
        }
        for (int x : new int[] {2, 13}) { px(im, x, 13, brass(5)); px(im, x + 1, 13, brass(2)); }
        // The plank seam running down the grain.
        for (int x = 0; x < 16; x++) px(im, x, 12, wood(2));
        return im;
    }

    /** The bed and underside: dark treated boards with cross braces. */
    static BufferedImage beltBed() {
        BufferedImage im = TextureGen.img();
        Random r = new Random(13240);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int step = 2 + (r.nextInt(5) == 0 ? 1 : 0) - (r.nextInt(7) == 0 ? 1 : 0);
                if (y % 8 == 0) step = 1;
                if (y % 8 == 7) step = 3;
                px(im, x, y, wood(Math.max(1, Math.min(5, step))));
            }
        return im;
    }

    /** The brass hub plate on each rail where an axle drives the belt, drawn in the middle 8 px. */
    static BufferedImage beltHub() {
        BufferedImage im = TextureGen.img();
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double d = Math.hypot(x - 7.5, y - 7.5);
                int step = 3;
                if (x < 4 || x > 11 || y < 4 || y > 11) step = 2;
                else if (d < 1.8) step = 1;
                else if (d < 3.0) step = 5;
                else if (x == 4 || y == 4) step = 4;
                else if (x == 11 || y == 11) step = 2;
                px(im, x, y, brass(step));
            }
        return im;
    }

    /** The diverter's brass lip along the open side and its end posts: a plate with a bevelled top edge and rivets. */
    static BufferedImage diverterSide() {
        BufferedImage im = TextureGen.img();
        Random r = new Random(13250);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int step = r.nextInt(7) == 0 ? 4 : 3;
                if (y == 0) step = 5;
                else if (y == 1) step = 4;
                else if (y == 15) step = 1;
                else if (y == 14) step = 2;
                px(im, x, y, brass(step));
            }
        for (int x : new int[] {2, 7, 13}) { px(im, x, 4, brass(5)); px(im, x + 1, 4, brass(2)); px(im, x, 5, brass(1)); }
        return im;
    }

    /** The paddle's bar: brass with a lit top edge and a dark underside; the hinge pin is the dark pixel at the top. */
    static BufferedImage diverterPaddle() {
        BufferedImage im = TextureGen.img();
        Random r = new Random(13260);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int step = r.nextInt(6) == 0 ? 2 : 3;
                if (x == 0 || y == 0) step = 5;
                if (x == 1 && y > 0) step = 4;
                if (x == 15 || y == 15) step = 1;
                px(im, x, y, brass(step));
            }
        px(im, 0, 1, iron(1));
        px(im, 1, 1, iron(3));
        for (int y = 3; y < 11; y += 2) px(im, 1, y, brass(2));
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
        for (int f = 0; f < 4; f++) TextureGen.save("block/conveyor_belt_top_" + f, beltTop(f));
        TextureGen.save("block/conveyor_belt_edge", beltEdge());
        TextureGen.save("block/conveyor_belt_rail", beltRail());
        TextureGen.save("block/conveyor_belt_side", beltSide());
        TextureGen.save("block/conveyor_belt_bed", beltBed());
        TextureGen.save("block/conveyor_belt_hub", beltHub());
        TextureGen.save("block/belt_diverter_side", diverterSide());
        TextureGen.save("block/belt_diverter_paddle", diverterPaddle());
        System.out.println("automation textures written");
    }
}
