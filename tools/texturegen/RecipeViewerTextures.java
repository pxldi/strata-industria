import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Small icons for the recipe viewer pages (compat/jei): a thermometer whose glass is left clear so the
 * page can fill it with the heat band's glow colour, a clock for times and a gear for mechanical power.
 * Colours come from the style guide's ramps (tin glass and steel, bronze rim, paper face, ink).
 *
 * <p>Run from the repository root: {@code java tools/texturegen/RecipeViewerTextures.java}. Output is
 * deterministic and goes to {@code src/main/resources/assets/strataindustria/textures/gui/jei/icons.png}.
 */
public final class RecipeViewerTextures {
    static final Path OUT = Path.of("src/main/resources/assets/strataindustria/textures/gui/jei/icons.png");

    /** '.' is transparent. The thermometer's clear glass (column x=3 rows 2-8, bulb x=2-4 rows 8-10) is filled at runtime. */
    static final String[] THERMOMETER = {
            "..ooo..",
            "..oho..",
            "..o.o..",
            "..o.om.",
            "..o.o..",
            "..o.om.",
            "..o.o..",
            ".oo.oo.",
            "oh...oo",
            "oo...oo",
            ".o...o.",
            "..ooo..",
    };
    static final String[] CLOCK = {
            "..ooooo..",
            ".offfffo.",
            "offfkfffo",
            "offfkfffo",
            "offfkkkfo",
            "offffffso",
            "offfffsso",
            ".osssssr.",
            "..rrrrr..",
    };
    static final String[] GEAR = {
            "...o.o...",
            ".o.ooo.o.",
            "..olllo..",
            "oolllmmoo",
            ".olm.mso.",
            "oommmssoo",
            "..ommso..",
            ".o.ooo.o.",
            "...o.o...",
    };

    public static void main(String[] args) throws IOException {
        BufferedImage im = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        // Tin ramp for glass and the gear, ink for markings.
        draw(im, 0, 0, THERMOMETER, Map.of('o', 0x3a3d48, 'h', 0xd6dbe2, 'm', 0x838998));
        // Bronze rim, paper face, ink hands.
        draw(im, 16, 0, CLOCK, Map.of('o', 0x75501f, 'r', 0x4a3216, 'f', 0xd6caae, 's', 0xb8aa8e, 'k', 0x2e2a26));
        draw(im, 32, 0, GEAR, Map.of('o', 0x3a3d48, 'l', 0xaeb4c0, 'm', 0x838998, 's', 0x5c6170));
        File f = OUT.toFile();
        f.getParentFile().mkdirs();
        ImageIO.write(im, "png", f);
    }

    static void draw(BufferedImage im, int x0, int y0, String[] rows, Map<Character, Integer> colours) {
        for (int y = 0; y < rows.length; y++) {
            for (int x = 0; x < rows[y].length(); x++) {
                Integer c = colours.get(rows[y].charAt(x));
                if (c != null) im.setRGB(x0 + x, y0 + y, 0xFF000000 | c);
            }
        }
    }
}
