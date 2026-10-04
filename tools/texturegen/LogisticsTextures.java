import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;
import java.util.Random;
import java.util.function.IntFunction;

/** Tier 5 logistics: item pipes, pipe extractors, storage controller, fluid filter. */
public final class LogisticsTextures {
    static final TextureGen.Ramp BRASS = TextureGen.BRASS;
    static final TextureGen.Ramp STEEL = TextureGen.STEEL;
    static final TextureGen.Ramp ALU = ElectricTextures.ALUMINIUM;
    static final TextureGen.Ramp BRONZE = TextureGen.BRONZE;

    static int c(TextureGen.Ramp r, int s) { return ElectricTextures.c(r, s); }

    static void px(BufferedImage im, int x, int y, int rgb) { TextureGen.px(im, x, y, rgb); }

    static void fill(BufferedImage im, int x0, int y0, int x1, int y1, int col) { ElectricTextures.fill(im, x0, y0, x1, y1, col); }

    static void glass(BufferedImage im, int x, int y, int alpha, int rgb) { im.setRGB(x, y, (alpha << 24) | rgb); }

    public static void main(String[] args) throws IOException {
        pipe();
        extractor();
        controller();
        filter();
        controllerGui();
        String sp = "gui/sprites/container/electric_machine/";
        TextureGen.saveRaw(sp + "extruder_item_pipe", ElectricTextures.modeButton(ITEM_PIPE,
                Map.of('h', 0xe3c8a0, 'm', 0xc0a070, 'd', 0x8a6a3a, 'f', 0x7a7a7a, 'i', 0xe8e8e8, 'g', 0x9ec6d2)));
    }

    // ---------------------------------------------------------------- pipe

    static void pipe() throws IOException {
        // Core face: brass frame around a pane of glass.
        BufferedImage core = TextureGen.img();
        for (int y = 8; y <= 15; y++) for (int x = 4; x <= 11; x++) {
            boolean edge = x == 4 || x == 11 || y == 8 || y == 15;
            if (edge) {
                int step = (x == 4 || y == 8) ? 5 : 2;
                px(core, x, y, c(BRASS, step));
            } else {
                glass(core, x, y, 0x60, ((x + y) % 3 == 0) ? 0xcfe8ee : 0x9ec6d2);
            }
        }
        px(core, 5, 9, 0xffffff);
        core.setRGB(5, 9, (0xb0 << 24) | 0xffffff);
        // Arm side: glass tube with two brass bands.
        BufferedImage arm = TextureGen.img();
        for (int y = 0; y <= 3; y++) for (int x = 0; x <= 5; x++) {
            if (y == 0 || y == 3) px(arm, x, y, c(BRASS, y == 0 ? 4 : 2));
            else glass(arm, x, y, 0x60, ((x + y) % 2 == 0) ? 0xcfe8ee : 0x9ec6d2);
        }
        // Arm end ring: 6x6, brass rim.
        for (int y = 0; y <= 5; y++) for (int x = 8; x <= 13; x++) {
            boolean edge = x == 8 || x == 13 || y == 0 || y == 5;
            if (edge) px(arm, x, y, c(BRASS, (x == 8 || y == 0) ? 5 : 2));
            else glass(arm, x, y, 0x50, 0x9ec6d2);
        }
        BufferedImage both = TextureGen.img();
        both.getGraphics().drawImage(core, 0, 0, null);
        both.getGraphics().drawImage(arm, 0, 0, null);
        // arm region (0..5,0..3) and (8..13,0..5) do not overlap the core region (4..11,8..15)
        TextureGen.save("block/item_pipe", both);
        TextureGen.save("block/item_pipe_core", core);
        TextureGen.save("block/item_pipe_arm", arm);

        BufferedImage storage = ElectricTextures.casing(BRASS, 3, 311);
        TextureGen.save("block/item_pipe_collar_storage", storage);
        BufferedImage off = ElectricTextures.casing(STEEL, 2, 312);
        for (int i = 3; i <= 12; i++) { px(off, i, i, c(STEEL, 1)); px(off, 15 - i, i, c(STEEL, 1)); }
        TextureGen.save("block/item_pipe_collar_off", off);
    }

    // ---------------------------------------------------------------- extractor

    static void extractor() throws IOException {
        TextureGen.save("block/pipe_extractor_body", ElectricTextures.casing(BRASS, 3, 321));
        TextureGen.save("block/fast_pipe_extractor_body", ElectricTextures.casing(ALU, 3, 322));
        TextureGen.save("block/pipe_extractor_funnel", funnel(BRASS));
        TextureGen.save("block/fast_pipe_extractor_funnel", funnel(ALU));
        for (boolean fast : new boolean[]{false, true}) {
            String n = fast ? "fast_pipe_extractor" : "pipe_extractor";
            TextureGen.save("block/" + n + "_mouth", mouth(fast ? ALU : BRASS, false));
            TextureGen.save("block/" + n + "_mouth_active", mouth(fast ? ALU : BRASS, true));
        }
    }

    static BufferedImage funnel(TextureGen.Ramp r) {
        BufferedImage im = ElectricTextures.casing(r, 3, 330);
        // dark throat in the centre
        fill(im, 5, 5, 10, 10, c(STEEL, 1));
        for (int i = 5; i <= 10; i++) { px(im, i, 5, c(STEEL, 1)); px(im, 5, i, c(STEEL, 1)); }
        fill(im, 6, 6, 9, 9, 0x14161a);
        return im;
    }

    static BufferedImage mouth(TextureGen.Ramp r, boolean active) {
        BufferedImage im = ElectricTextures.casing(r, 3, 340);
        fill(im, 4, 4, 11, 11, c(STEEL, 2));
        for (int i = 4; i <= 11; i++) { px(im, i, 4, c(STEEL, 1)); px(im, 4, i, c(STEEL, 1)); px(im, i, 11, c(STEEL, 4)); px(im, 11, i, c(STEEL, 4)); }
        int core = active ? 0x9ee0ff : 0x20262e;
        fill(im, 6, 6, 9, 9, core);
        if (active) { px(im, 6, 6, 0xe8faff); px(im, 9, 9, 0x5aa8d0); }
        return im;
    }

    // ---------------------------------------------------------------- controller

    static void controller() throws IOException {
        BufferedImage side = ElectricTextures.casing(STEEL, 3, 351);
        TextureGen.save("block/storage_controller_side", side);
        BufferedImage top = ElectricTextures.casing(BRASS, 3, 352);
        // vent slits
        for (int y = 5; y <= 10; y += 2) for (int x = 4; x <= 11; x++) px(top, x, y, c(BRASS, 1));
        TextureGen.save("block/storage_controller_top", top);
        TextureGen.save("block/storage_controller_bottom", ElectricTextures.casing(STEEL, 2, 353));
        TextureGen.save("block/storage_controller_front", front(-1));
        TextureGen.saveAnimated("block/storage_controller_front_active", strip(f -> front(f)), 4);
    }

    static BufferedImage strip(IntFunction<BufferedImage> f) {
        BufferedImage s = new BufferedImage(16, 64, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < 4; i++) s.getGraphics().drawImage(f.apply(i), 0, i * 16, null);
        return s;
    }

    /** Front: steel plate, a dark screen window, a brass drawer handle. frame &lt; 0 is the unlit state. */
    static BufferedImage front(int frame) {
        BufferedImage im = ElectricTextures.casing(STEEL, 3, 354);
        fill(im, 3, 3, 12, 9, c(STEEL, 1));
        for (int i = 3; i <= 12; i++) { px(im, i, 3, 0x0c0e12); px(im, i, 9, c(STEEL, 5)); }
        for (int j = 3; j <= 9; j++) { px(im, 3, j, 0x0c0e12); px(im, 12, j, c(STEEL, 5)); }
        fill(im, 4, 4, 11, 8, 0x10161a);
        if (frame >= 0) {
            for (int row = 0; row < 3; row++) {
                int y = 4 + row + (row == 2 ? 1 : 0);
                int off = (frame + row * 2) % 4;
                for (int x = 4; x <= 11; x++) {
                    int phase = (x + off) % 4;
                    px(im, x, y, phase < 2 ? (phase == 0 ? 0x8ff0b0 : 0x58c080) : 0x1c3a2a);
                }
            }
        } else {
            for (int row = 0; row < 3; row++) {
                int y = 4 + row + (row == 2 ? 1 : 0);
                for (int x = 4; x <= 11; x++) px(im, x, y, (x % 3 == 0) ? 0x1c2a22 : 0x141c18);
            }
        }
        // handle
        fill(im, 5, 11, 10, 11, c(BRASS, 5));
        fill(im, 5, 12, 10, 12, c(BRASS, 2));
        px(im, 4, 11, c(BRASS, 3)); px(im, 11, 11, c(BRASS, 3));
        return im;
    }

    // ---------------------------------------------------------------- fluid filter

    static void filter() throws IOException {
        BufferedImage h = ElectricTextures.casing(BRONZE, 3, 361);
        TextureGen.save("block/fluid_filter_housing", h);
        BufferedImage top = ElectricTextures.casing(BRONZE, 3, 362);
        fill(top, 4, 4, 11, 11, c(BRONZE, 1));
        for (int i = 4; i <= 11; i++) { px(top, i, 4, c(BRONZE, 1)); px(top, 4, i, c(BRONZE, 1)); px(top, i, 11, c(BRONZE, 5)); px(top, 11, i, c(BRONZE, 5)); }
        fill(top, 5, 5, 10, 10, 0x14181c);
        TextureGen.save("block/fluid_filter_top", top);
    }

    // ---------------------------------------------------------------- gui

    static void controllerGui() throws IOException {
        BufferedImage im = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        TextureGen.panel(im, 194, 234);
        for (int row = 0; row < 6; row++) for (int col = 0; col < 9; col++) TextureGen.slot(im, 17 + col * 18, 33 + row * 18);
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) TextureGen.slot(im, 16 + col * 18, 152 + row * 18);
        for (int col = 0; col < 9; col++) TextureGen.slot(im, 16 + col * 18, 210);
        TextureGen.well(im, 16, 18, 112, 12, TextureGen.SLOT_FILL);
        TextureGen.well(im, 180, 32, 8, 108, TextureGen.SLOT_FILL);
        for (int k = 0; k < 2; k++) {
            int x0 = 194 + k * 8;
            boolean on = k == 0;
            fill(im, x0, 0, x0 + 7, 14, on ? TextureGen.GUI_FACE : 0x9a9a9a);
            for (int i = 0; i < 8; i++) { px(im, x0 + i, 0, on ? TextureGen.GUI_LIGHT : 0xb0b0b0); px(im, x0 + i, 14, TextureGen.GUI_SHADOW); }
            for (int j = 0; j < 15; j++) { px(im, x0, j, on ? TextureGen.GUI_LIGHT : 0xb0b0b0); px(im, x0 + 7, j, TextureGen.GUI_SHADOW); }
            for (int j = 5; j <= 9; j += 2) for (int i = 2; i <= 5; i++) px(im, x0 + i, j, TextureGen.GUI_SHADOW);
        }
        // sort buttons: name, name pressed, count, count pressed
        for (int k = 0; k < 4; k++) {
            int x0 = k * 22, y0 = 236;
            boolean pressed = (k & 1) == 1;
            int face = pressed ? 0xa8a8a8 : TextureGen.GUI_FACE;
            fill(im, x0, y0, x0 + 21, y0 + 11, face);
            for (int i = 0; i < 22; i++) { px(im, x0 + i, y0, pressed ? TextureGen.GUI_SHADOW : TextureGen.GUI_LIGHT); px(im, x0 + i, y0 + 11, pressed ? TextureGen.GUI_LIGHT : TextureGen.GUI_SHADOW); }
            for (int j = 0; j < 12; j++) { px(im, x0, y0 + j, pressed ? TextureGen.GUI_SHADOW : TextureGen.GUI_LIGHT); px(im, x0 + 21, y0 + j, pressed ? TextureGen.GUI_LIGHT : TextureGen.GUI_SHADOW); }
            String[] g = k < 2 ? GLYPH_AZ : GLYPH_NUM;
            int gx = x0 + 5, gy = y0 + 3 + (pressed ? 1 : 0);
            for (int j = 0; j < g.length; j++) for (int i = 0; i < g[j].length(); i++) if (g[j].charAt(i) == '#') px(im, gx + i, gy + j, TextureGen.SLOT_DARK);
        }
        TextureGen.saveRaw("gui/storage_controller", im);
    }

    /** A and Z with a down arrow. */
    static final String[] GLYPH_AZ = {
            ".#.#####..#",
            "#.#...#...#",
            "###..#....#",
            "#.#.#.....#",
            "#.#.#####.#",
    };
    /** 1 2 3 with a down arrow. */
    static final String[] GLYPH_NUM = {
            ".#..##.##.#",
            "##....#..#.",
            ".#..##..###",
            ".#.#.....#.",
            "###.####..#",
    };

    static final String[] ITEM_PIPE = {
            "........####",
            "........#ff#",
            "#########ff#",
            "#hhhhhhh#ff#",
            "#mgiigmm#ff#",
            "#mgiigmm#ff#",
            "#ddddddd#ff#",
            "#########ff#",
            "........#ff#",
            "........####",
    };
}
