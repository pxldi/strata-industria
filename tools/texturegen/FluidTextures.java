import java.io.IOException;

/**
 * Textures for the fluids of tiers 3 and 4 that have no tier 6 sheet of their own: lye, tannin, creosote,
 * sulfur dioxide and steam. Reuses the fluid helpers of {@link Tier6Textures}, so compile both together
 * from the repository root:
 * {@code javac -d build/texturegen tools/texturegen/Tier6Textures.java tools/texturegen/FluidTextures.java}
 * then {@code java -cp build/texturegen FluidTextures}. Output is deterministic.
 */
public final class FluidTextures {
    // Ramps, dark to light, plus specular (0 = none).
    static final Tier6Textures.Ramp LYE = new Tier6Textures.Ramp(0, 0x7e8a78, 0x95a18d, 0xabb7a2, 0xc0cbb7, 0xd4ddcb);
    static final Tier6Textures.Ramp TANNIN = new Tier6Textures.Ramp(0, 0x44280f, 0x5c3b1b, 0x75502a, 0x8d6738, 0xa5804a);
    static final Tier6Textures.Ramp CREOSOTE = new Tier6Textures.Ramp(0, 0x120c08, 0x1e140c, 0x2c1e12, 0x3c2a1a, 0x503824);

    private FluidTextures() {}

    public static void main(String[] args) throws IOException {
        // Lye and tannin: thin and tinted, like naphtha and diesel but opaque enough to read as a brew.
        Tier6Textures.thin("block/fluid/lye_still", LYE, 16, 16, 2, 0xB4, true);
        Tier6Textures.thin("block/fluid/lye_flow", LYE, 32, 16, 2, 0xB4, true);
        Tier6Textures.thin("block/fluid/tannin_still", TANNIN, 16, 16, 2, 0xD0, false);
        Tier6Textures.thin("block/fluid/tannin_flow", TANNIN, 32, 16, 2, 0xD0, false);
        // Creosote: thick, dark and slow, like heavy oil without its sheen.
        Tier6Textures.oil("block/fluid/creosote_still", CREOSOTE, 16, 32, 4, 11, false);
        Tier6Textures.oil("block/fluid/creosote_flow", CREOSOTE, 32, 32, 4, 12, false);
        // Gases have one still frame that doubles as the flowing sprite.
        Tier6Textures.save("block/fluid/sulfur_dioxide", Tier6Textures.gas(0xd6d070, 211));
        Tier6Textures.save("block/fluid/steam", Tier6Textures.gas(0xe6eaee, 223));
    }
}
