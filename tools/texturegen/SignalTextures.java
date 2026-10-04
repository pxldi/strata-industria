import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Random;

/**
 * Textures for the line control (outposts and transport spec 9.4 and 14): the block signal's post, arm and lamp, the
 * route switch in both settings, the timetable item and the timetable screen.
 *
 * <p>The signal is one 16 x 16 sheet that {@code tools/gen_signal_models.py} reads in four bands: iron (rows 0-5),
 * the red and white arm (6-9), the arm's dark underside (10-12) and the lamp (13-15, dark glass left, lit right).
 * Run from the repository root:
 * <pre>
 * mkdir -p build/tg &amp;&amp; javac -d build/tg tools/texturegen/TextureGen.java tools/texturegen/TransportTextures.java tools/texturegen/RailwayTextures.java tools/texturegen/LocomotiveTextures.java tools/texturegen/SignalTextures.java &amp;&amp; java -cp build/tg SignalTextures
 * </pre>
 */
public final class SignalTextures {
    static final TextureGen.Ramp IRON = RailwayTextures.IRON;
    static final TextureGen.Ramp STEEL = RailwayTextures.STEEL;
    static final TextureGen.Ramp RED = RailwayTextures.RED;
    static final TextureGen.Ramp WHITE = RailwayTextures.WHITE;
    static final TextureGen.Ramp AMBER = LocomotiveTextures.AMBER;
    static final TextureGen.Ramp BRASS = RailwayTextures.BRASS;
    static final TextureGen.Ramp PAPER = TextureGen.PAPER;
    static final TextureGen.Ramp WOOD = TransportTextures.WOOD;
    static final TextureGen.Ramp BRONZE = TransportTextures.BRONZE;
    static final TextureGen.Ramp COPPER = TextureGen.COPPER;

    // ---------------------------------------------------------------- the signal

    static BufferedImage signal() {
        BufferedImage im = TextureGen.img();
        Random r = new Random(901);
        // Iron: a lit left edge, a shaded right edge, a few rivets.
        for (int y = 0; y <= 5; y++)
            for (int x = 0; x < 16; x++) {
                int step = 3 + (r.nextInt(7) == 0 ? 1 : 0);
                if (x % 8 == 0) step = 5;
                if (x % 8 == 7) step = 1;
                if (y == 0) step = Math.min(5, step + 1);
                if (y == 5) step = Math.max(1, step - 1);
                TextureGen.px(im, x, y, IRON.get(step));
            }
        for (int x : new int[] {2, 5, 10, 13}) TextureGen.px(im, x, 3, IRON.get(5));
        // The arm: red and white in four-pixel stripes slanting one way, a dark rim above and below.
        for (int y = 6; y <= 9; y++)
            for (int x = 0; x < 16; x++) {
                boolean red = ((x + y) / 4) % 2 == 0;
                int step = red ? 3 : 4;
                if (y == 6) step++;
                if (y == 9) step--;
                TextureGen.px(im, x, y, (red ? RED : WHITE).get(Math.max(1, Math.min(5, step))));
            }
        // Underside: the red's darkest shades with a faint grain.
        for (int y = 10; y <= 12; y++)
            for (int x = 0; x < 16; x++) TextureGen.px(im, x, y, RED.get(1 + (r.nextInt(5) == 0 ? 1 : 0) + (y == 10 ? 0 : 0)));
        // The lamp: dark smoked glass with a glint, and the same glass lit.
        for (int y = 13; y <= 15; y++)
            for (int x = 0; x < 8; x++) {
                int step = 2;
                if (x == 0 || y == 15) step = 1;
                if (x == 1 && y == 13) step = 4;
                TextureGen.px(im, x, y, RED.get(step));
            }
        for (int y = 13; y <= 15; y++)
            for (int x = 8; x < 16; x++) {
                int step = 4;
                if (x == 8 || x == 15 || y == 15) step = 3;
                if (x > 9 && x < 14 && y < 15) step = 5;
                TextureGen.px(im, x, y, y == 14 && x > 9 && x < 14 ? AMBER.get(5) : RED.get(step));
            }
        return im;
    }

    // ---------------------------------------------------------------- the switch

    /** A switch machine laid between the rails: a steel box, a brass lever stub and a lamp of {@code lamp}'s colour. */
    private static void machine(BufferedImage im, int x0, int y0, TextureGen.Ramp lamp) {
        for (int y = 0; y < 5; y++)
            for (int x = 0; x < 5; x++) {
                int step = 3;
                if (y == 0 || x == 0) step = 5;
                if (y == 4 || x == 4) step = 1;
                TextureGen.px(im, x0 + x, y0 + y, STEEL.get(step));
            }
        // The lens, two pixels, lit.
        TextureGen.px(im, x0 + 2, y0 + 1, lamp.get(5));
        TextureGen.px(im, x0 + 3, y0 + 1, lamp.get(4));
        TextureGen.px(im, x0 + 2, y0 + 2, lamp.get(4));
        TextureGen.px(im, x0 + 3, y0 + 2, lamp.get(3));
        // The lever stub.
        TextureGen.px(im, x0 + 1, y0 + 3, BRASS.get(5));
        TextureGen.px(im, x0 + 2, y0 + 3, BRASS.get(3));
        TextureGen.px(im, x0 + 1, y0 + 2, IRON.get(2));
    }

    static BufferedImage routeSwitch() {
        BufferedImage im = RailwayTextures.straight();
        // Clear the sleepers under the box so it reads, then set it in.
        machine(im, 6, 5, WHITE);
        return im;
    }

    static BufferedImage routeSwitchCorner() {
        BufferedImage im = RailwayTextures.corner();
        machine(im, 7, 7, AMBER);
        return im;
    }

    // ---------------------------------------------------------------- the item

    /** A sheet of paper with ruled lines of stops, a brass clip at the top and a copper circuit chip in the corner. */
    static BufferedImage timetable() {
        BufferedImage im = TextureGen.img();
        for (int y = 2; y <= 14; y++)
            for (int x = 3; x <= 12; x++) {
                int step = 4;
                if (x == 3 || y == 2) step = 5;
                if (x == 12 || y == 14) step = 3;
                TextureGen.px(im, x, y, PAPER.get(step));
            }
        // Lines of writing: a short stop name and a time each.
        for (int row = 0; row < 4; row++) {
            int y = 5 + row * 2;
            for (int x = 5; x <= 8 - (row % 2); x++) TextureGen.px(im, x, y, 0xFF4a4034);
            TextureGen.px(im, 10, y, 0xFF6e6250);
        }
        // The clip.
        for (int x = 6; x <= 9; x++) TextureGen.px(im, x, 1, BRASS.get(x == 6 ? 5 : x == 9 ? 2 : 4));
        TextureGen.px(im, 7, 2, BRASS.get(2));
        TextureGen.px(im, 8, 2, BRASS.get(2));
        // The chip stuck on the lower corner.
        for (int y = 11; y <= 13; y++)
            for (int x = 9; x <= 12; x++) TextureGen.px(im, x, y, y == 11 ? COPPER.get(5) : y == 13 ? COPPER.get(2) : COPPER.get(3));
        TextureGen.px(im, 10, 12, BRASS.get(5));
        return TextureGen.outline(im);
    }

    // ---------------------------------------------------------------- the screen

    /** The timetable and route switch screen: 176 x 200 in a 256 x 256 sheet, a plank frame round a sheet of paper with a well for each line. */
    static BufferedImage gui() {
        BufferedImage im = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        int w = 176, h = 200;
        Random r = new Random(905);
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) im.setRGB(x, y, 0xFF000000 | WOOD.get(3));
        TextureGen.grain(im, WOOD, r, 0, 0, w, h, 3, false);
        for (int x = 0; x < w; x++) {
            TextureGen.px(im, x, 0, WOOD.get(1));
            TextureGen.px(im, x, h - 1, WOOD.get(1));
            TextureGen.px(im, x, 1, WOOD.get(4));
            TextureGen.px(im, x, h - 2, WOOD.get(2));
        }
        for (int y = 0; y < h; y++) {
            TextureGen.px(im, 0, y, WOOD.get(1));
            TextureGen.px(im, w - 1, y, WOOD.get(1));
            if (y > 0 && y < h - 1) {
                TextureGen.px(im, 1, y, WOOD.get(4));
                TextureGen.px(im, w - 2, y, WOOD.get(2));
            }
        }
        int x0 = 7, y0 = 7, x1 = w - 8, y1 = 167;
        for (int y = y0; y <= y1; y++)
            for (int x = x0; x <= x1; x++) {
                boolean edge = x == x0 || y == y0, shade = x == x1 || y == y1;
                TextureGen.px(im, x, y, edge ? WOOD.get(1) : shade ? PAPER.get(5) : PAPER.get(3));
            }
        for (int i = 0; i < 200; i++) {
            int x = x0 + 2 + r.nextInt(x1 - x0 - 4), y = y0 + 2 + r.nextInt(y1 - y0 - 4);
            boolean light = r.nextInt(3) > 0;
            for (int k = 0; k < 2 + r.nextInt(2); k++) TextureGen.px(im, x + k, y, light ? PAPER.get(4) : PAPER.get(2));
        }
        // A rule under the heading and one above the Done button.
        for (int x = 14; x < w - 14; x++) {
            TextureGen.px(im, x, 37, PAPER.get(2));
            TextureGen.px(im, x, y1 - 6, PAPER.get(2));
        }
        // A well for each of the eight lines, the same rows as the screen's text boxes.
        for (int row = 0; row < 8; row++) {
            int wy = 40 + row * 15 + 1, wx = 22, ww = 140, wh = 12;
            for (int y = wy; y < wy + wh; y++)
                for (int x = wx; x < wx + ww; x++) {
                    boolean top = y == wy, left = x == wx, bottom = y == wy + wh - 1, right = x == wx + ww - 1;
                    TextureGen.px(im, x, y, (top || left) ? PAPER.get(1) : (bottom || right) ? PAPER.get(5) : PAPER.get(4));
                }
        }
        for (int[] c : new int[][] {{x0 + 3, y0 + 3}, {x1 - 4, y0 + 3}, {x0 + 3, y1 - 4}, {x1 - 4, y1 - 4}}) {
            TextureGen.px(im, c[0], c[1], BRONZE.get(5));
            TextureGen.px(im, c[0] + 1, c[1], BRONZE.get(3));
            TextureGen.px(im, c[0], c[1] + 1, BRONZE.get(3));
            TextureGen.px(im, c[0] + 1, c[1] + 1, BRONZE.get(1));
        }
        return im;
    }

    public static void main(String[] args) throws IOException {
        TextureGen.save("block/block_signal", signal());
        TextureGen.save("block/route_switch", routeSwitch());
        TextureGen.save("block/route_switch_corner", routeSwitchCorner());
        TextureGen.itemsV2 = true;
        TextureGen.save("item/timetable", timetable());
        TextureGen.itemsV2 = false;
        TextureGen.saveRaw("gui/timetable", gui());
    }

    private SignalTextures() {}
}
