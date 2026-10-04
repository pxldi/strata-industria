import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Textures for tier 2 on foot (outposts and transport spec 4 and 14): rope, the rope ladder, the pack frame with
 * its worn layer, the handcart with its entity sheet, and the blaze mark. Cairns are drawn from the rock textures.
 *
 * <p>Run from the repository root:
 * <pre>
 * mkdir -p build/tg &amp;&amp; javac -d build/tg tools/texturegen/TextureGen.java tools/texturegen/FootTextures.java &amp;&amp; java -cp build/tg FootTextures
 * </pre>
 */
public final class FootTextures {
    static final TextureGen.Ramp WOOD = TextureGen.V2.WOOD_V2;
    static final TextureGen.Ramp BRONZE = TextureGen.V2.BRONZE_V2;
    static final TextureGen.Ramp LEATHER = TextureGen.LEATHER;
    /** Hemp rope: warm tan, a little paler than leather. */
    static final TextureGen.Ramp ROPE = TextureGen.ramp(0, 0x4e3818, 0x785526, 0xa27a38, 0xc99f4c, 0xe6c46e);

    private static int hash(int x, int y, int salt) {
        return Math.floorMod(x * 73 + y * 151 + x * y * 17 + salt * 31 + 11, 97);
    }

    // ---------------------------------------------------------------- items

    /** A coil of rope: a twisted ring with its free end hanging at the lower right. */
    static BufferedImage ropeItem() {
        BufferedImage im = TextureGen.img();
        double cx = 7.0, cy = 7.0;
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double dx = x - cx, dy = y - cy, d = Math.sqrt(dx * dx + dy * dy);
                if (d < 2.6 || d > 5.9) continue;
                double angle = Math.atan2(dy, dx);
                int step = 3;
                // Lit from the top left: the ring is brighter where it faces up and left.
                step += (int) Math.round(-0.9 * (dx + dy) / (Math.abs(dx) + Math.abs(dy) + 0.01) * 1.0);
                // Twist: bands that run across the strand.
                int twist = Math.floorMod((int) Math.floor(angle * 6.0 / Math.PI * 2.0 + d * 1.3), 2);
                step += twist == 0 ? 1 : -1;
                if (d > 5.0) step -= 1;
                TextureGen.px(im, x, y, ROPE.get(Math.max(1, Math.min(5, step))));
            }
        // The loose end: three twisted pixels down to the right, with a frayed tip.
        int[][] tail = {{11, 11, 3}, {12, 12, 2}, {12, 13, 3}, {13, 14, 2}, {13, 15, 4}, {12, 15, 5}};
        for (int[] t : tail) TextureGen.px(im, t[0], t[1], ROPE.get(t[2]));
        return TextureGen.outline(im);
    }

    /** A light wooden frame with a leather back panel, lashed at the crossings. */
    static BufferedImage packFrameItem() {
        BufferedImage im = TextureGen.img();
        // Uprights.
        for (int y = 1; y <= 14; y++) {
            TextureGen.px(im, 3, y, WOOD.get(4));
            TextureGen.px(im, 4, y, WOOD.get(3));
            TextureGen.px(im, 11, y, WOOD.get(4));
            TextureGen.px(im, 12, y, WOOD.get(3));
        }
        TextureGen.px(im, 3, 1, WOOD.get(5));
        TextureGen.px(im, 11, 1, WOOD.get(5));
        // Cross bars.
        for (int x = 3; x <= 12; x++) {
            TextureGen.px(im, x, 2, WOOD.get(4));
            TextureGen.px(im, x, 3, WOOD.get(2));
            TextureGen.px(im, x, 12, WOOD.get(4));
            TextureGen.px(im, x, 13, WOOD.get(2));
        }
        // Leather panel between the uprights.
        for (int y = 4; y <= 11; y++)
            for (int x = 5; x <= 10; x++) {
                int step = 3;
                if (y == 4 || x == 5) step = 4;
                if (y == 11 || x == 10) step = 2;
                if ((x + y) % 4 == 0 && step == 3) step = 4;
                TextureGen.px(im, x, y, LEATHER.get(step));
            }
        // Stitching down the panel and a buckle strap hanging from the top bar.
        for (int y = 5; y <= 10; y += 2) TextureGen.px(im, 7, y, LEATHER.get(5));
        TextureGen.px(im, 8, 6, LEATHER.get(1));
        TextureGen.px(im, 8, 8, LEATHER.get(1));
        TextureGen.px(im, 8, 7, BRONZE.get(5));
        TextureGen.px(im, 9, 7, BRONZE.get(3));
        // Lashings at the four crossings.
        for (int[] c : new int[][] {{3, 2}, {12, 2}, {3, 12}, {12, 12}}) {
            TextureGen.px(im, c[0], c[1], ROPE.get(4));
            TextureGen.px(im, c[0], c[1] + 1, ROPE.get(2));
        }
        return TextureGen.outline(im);
    }

    /** The handcart from the side: a plank bed, one wheel with a bronze hub, and the shafts running off to the left. */
    static BufferedImage handcartItem() {
        BufferedImage im = TextureGen.img();
        // Bed: planks, lit on top, a bronze band at each end.
        for (int x = 5; x <= 14; x++) {
            TextureGen.px(im, x, 3, WOOD.get(5));
            TextureGen.px(im, x, 4, WOOD.get(4));
            TextureGen.px(im, x, 5, WOOD.get(3));
            TextureGen.px(im, x, 6, WOOD.get(1));
            TextureGen.px(im, x, 7, WOOD.get(3));
            TextureGen.px(im, x, 8, WOOD.get(2));
        }
        for (int y = 3; y <= 8; y++) {
            TextureGen.px(im, 5, y, BRONZE.get(y == 3 ? 5 : 3));
            TextureGen.px(im, 14, y, BRONZE.get(y == 3 ? 5 : 2));
        }
        // Wheel in front of the bed: dark rim, lighter boards, bronze hub.
        double cx = 10, cy = 11;
        for (int y = 6; y <= 15; y++)
            for (int x = 6; x <= 15; x++) {
                double d = Math.sqrt((x - cx) * (x - cx) + (y - cy) * (y - cy));
                if (d > 3.9) continue;
                int step = d > 3.0 ? 2 : 3;
                if (d <= 3.0 && (x == 10 || y == 11)) step = 4;
                if (d <= 3.0 && x < 10 && y < 11) step += 0;
                TextureGen.px(im, x, y, WOOD.get(step));
            }
        TextureGen.px(im, 10, 11, BRONZE.get(5));
        // Shafts: two poles from the front of the bed, down to the ground on the left.
        for (int i = 0; i <= 4; i++) {
            TextureGen.px(im, 4 - i, 6 + i, WOOD.get(4));
            TextureGen.px(im, 4 - i, 7 + i, WOOD.get(2));
        }
        return TextureGen.outline(im);
    }

    // ---------------------------------------------------------------- blocks

    /** Rope on the left four columns, one rung on the right; the model cuts both out. */
    static BufferedImage ropeLadder() {
        BufferedImage im = TextureGen.img();
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 4; x++) {
                int step = 3 + (Math.floorMod(x / 2 + y / 2, 2) == 0 ? 1 : -1);
                if (x % 2 == 0) step += 0; else step -= 1;
                TextureGen.px(im, x, y, ROPE.get(Math.max(1, Math.min(5, step))));
            }
        }
        // The rung: a smooth stick, grain along its length. Rows 0 to 5 are front, side and top faces.
        for (int y = 0; y < 6; y++)
            for (int x = 8; x < 14; x++) {
                int step = y < 2 ? (y == 0 ? 4 : 3) : y < 4 ? 3 : 4;
                if (hash(x, y, 5) < 22) step -= 1;
                TextureGen.px(im, x, y, WOOD.get(Math.max(1, step)));
            }
        return im;
    }

    /** A fresh blaze: bark cut away to pale wood, the bark edge curling dark at the top. */
    static BufferedImage blazeMark() {
        BufferedImage im = TextureGen.img();
        for (int y = 3; y <= 12; y++) {
            for (int x = 5; x <= 10; x++) {
                int step = 5;
                if (y <= 4) step = 3;
                if (x == 5 || x == 6 && y > 8) step = 4;
                if (x == 10 || y == 12) step = 4;
                if (hash(x, y, 9) < 18 && step > 3) step -= 1;
                TextureGen.px(im, x, y, WOOD.get(step) + 0x101008 & 0xFFFFFF);
            }
        }
        // Bark edge around the cut: dark, ragged.
        for (int y = 2; y <= 13; y++) {
            for (int x = 4; x <= 11; x++) {
                boolean inside = x >= 5 && x <= 10 && y >= 3 && y <= 12;
                if (inside) continue;
                boolean next = x >= 4 && x <= 11 && y >= 2 && y <= 13;
                if (next && hash(x, y, 2) < 70) TextureGen.px(im, x, y, WOOD.get(hash(x, y, 4) < 40 ? 1 : 2));
            }
        }
        // Chips of the bark left on the cut.
        TextureGen.px(im, 7, 6, WOOD.get(3));
        TextureGen.px(im, 8, 9, WOOD.get(3));
        return im;
    }

    // ---------------------------------------------------------------- entity

    interface FacePainter {
        int color(int face, int x, int y, int w, int h);
    }

    /** Paints the unfolded faces of a model box at texOffs (u, v): top, bottom, west, front, east, back. */
    static void box(BufferedImage im, int u, int v, int w, int h, int d, FacePainter painter) {
        int[][] rects = {
                {u + d, v, w, d}, {u + d + w, v, w, d},
                {u, v + d, d, h}, {u + d, v + d, w, h},
                {u + d + w, v + d, d, h}, {u + 2 * d + w, v + d, w, h}};
        for (int face = 0; face < 6; face++) {
            int[] r = rects[face];
            for (int y = 0; y < r[3]; y++)
                for (int x = 0; x < r[2]; x++) TextureGen.px(im, r[0] + x, r[1] + y, painter.color(face, x, y, r[2], r[3]));
        }
    }

    /** Planks: seams every four rows, a little grain, lit from above. */
    static FacePainter planks(int lift) {
        return (face, x, y, w, h) -> {
            int step = 3 + lift;
            if (face == 0) step += 1;
            if (face == 1) step -= 1;
            if (face == 4) step -= 1;
            boolean sideways = face >= 2;
            int seam = sideways ? y : x;
            if (seam % 4 == 3) step -= 2;
            else if (seam % 4 == 0) step += 0;
            if (hash(x, y, face) < 16) step -= 1;
            if (hash(x, y, face + 7) > 88) step += 1;
            return WOOD.get(Math.max(1, Math.min(5, step)));
        };
    }

    /** The bed boards, with a bronze band on the outer side of each wall. */
    static FacePainter bedWall() {
        FacePainter wood = planks(0);
        return (face, x, y, w, h) -> {
            if ((face == 2 || face == 4) && (x >= 3 && x <= 4 || x >= 11 && x <= 12)) {
                return BRONZE.get(x == 3 || x == 11 ? 4 : 3);
            }
            return wood.color(face, x, y, w, h);
        };
    }

    /** Wheel boards: vertical grain with a dark rim ring on the round faces. */
    static FacePainter wheelBoard() {
        return (face, x, y, w, h) -> {
            int step = 3;
            if (face == 2 || face == 4) {
                boolean rim = x == 0 || y == 0 || x == w - 1 || y == h - 1;
                step = rim ? 2 : 3;
                if (x % 3 == 1 && !rim) step = 4;
            } else {
                step = 2;
            }
            if (hash(x, y, face + 3) < 14) step -= 1;
            return WOOD.get(Math.max(1, step));
        };
    }

    static BufferedImage handcartSheet() {
        BufferedImage im = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        box(im, 0, 0, 16, 2, 16, planks(-1));
        box(im, 0, 18, 2, 5, 16, bedWall());
        box(im, 36, 18, 12, 5, 2, planks(0));
        box(im, 32, 26, 2, 2, 14, planks(1));
        box(im, 32, 42, 12, 2, 2, planks(1));
        box(im, 0, 42, 4, 2, 2, (face, x, y, w, h) -> BRONZE.get(face == 0 ? 5 : face == 1 ? 2 : 4));
        box(im, 0, 46, 2, 10, 6, wheelBoard());
        box(im, 16, 46, 2, 6, 10, wheelBoard());
        box(im, 40, 46, 2, 8, 8, wheelBoard());
        return im;
    }

    /**
     * The worn layer (64 x 32 humanoid armour sheet, chest slot): the frame and its panel on the back, two shoulder
     * straps and a chest strap on the front.
     */
    static BufferedImage packFrameWorn() {
        BufferedImage im = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        // Back (32,20) 8 x 12: uprights, cross bars, leather panel.
        for (int y = 0; y < 12; y++)
            for (int x = 0; x < 8; x++) {
                int px = 32 + x, py = 20 + y;
                boolean upright = x <= 1 || x >= 6;
                boolean bar = y <= 1 || y >= 10;
                if (upright || bar) {
                    TextureGen.px(im, px, py, WOOD.get(upright && x == 0 || bar && y == 0 ? 4 : 3));
                } else {
                    int step = (x + y) % 4 == 0 ? 4 : 3;
                    if (y == 2 || x == 2) step = 4;
                    if (y == 9 || x == 5) step = 2;
                    TextureGen.px(im, px, py, LEATHER.get(step));
                }
            }
        for (int[] c : new int[][] {{32, 20}, {39, 20}, {32, 30}, {39, 30}}) TextureGen.px(im, c[0], c[1], ROPE.get(4));
        // Sides (16,20) and (28,20), 4 x 12: the edge of the frame and a strap going round.
        for (int y = 0; y < 12; y++)
            for (int x = 0; x < 4; x++) {
                boolean strap = y == 5 || y == 6;
                TextureGen.px(im, 16 + x, 20 + y, strap ? LEATHER.get(3) : WOOD.get(x == 0 ? 3 : 2));
                TextureGen.px(im, 28 + x, 20 + y, strap ? LEATHER.get(3) : WOOD.get(x == 3 ? 3 : 2));
            }
        // Front (20,20) 8 x 12: two shoulder straps and the chest strap with a bronze buckle.
        for (int y = 0; y < 12; y++)
            for (int x = 0; x < 8; x++) {
                boolean strapX = x == 1 || x == 2 || x == 5 || x == 6;
                boolean chest = y == 4 || y == 5;
                if (strapX || chest) TextureGen.px(im, 20 + x, 20 + y, LEATHER.get(chest && !strapX ? 3 : x % 2 == 1 ? 4 : 3));
            }
        TextureGen.px(im, 23, 24, BRONZE.get(5));
        TextureGen.px(im, 24, 24, BRONZE.get(3));
        // Top (20,16) 8 x 4: the straps going over the shoulders.
        for (int y = 0; y < 4; y++)
            for (int x : new int[] {1, 2, 5, 6}) TextureGen.px(im, 20 + x, 16 + y, LEATHER.get(3));
        // Arm tops (44,16) and (36,48) are not used by the chest layer; the straps end at the shoulder.
        return im;
    }

    public static void main(String[] args) throws IOException {
        TextureGen.itemsV2 = true;
        TextureGen.save("item/rope", ropeItem());
        TextureGen.save("item/pack_frame", packFrameItem());
        TextureGen.save("item/handcart", handcartItem());
        TextureGen.itemsV2 = false;
        TextureGen.save("block/rope_ladder", ropeLadder());
        TextureGen.save("block/blaze_mark", blazeMark());
        TextureGen.saveRaw("entity/handcart", handcartSheet());
        TextureGen.saveRaw("entity/equipment/humanoid/pack_frame", packFrameWorn());
    }

    private FootTextures() {}
}
