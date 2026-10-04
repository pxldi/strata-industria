package dev.strataindustria.structure;

import net.minecraft.core.BlockPos;

/**
 * Age on a structure (structures spec 3.5): torn canvas, fallen fence posts, worn rugs. Decided from the
 * block position and the structure's seed, so a building that spans chunks weathers the same everywhere.
 */
final class Weathering {
    private Weathering() {}

    static long hash(BlockPos pos, long seed) {
        long h = seed ^ pos.getX() * 3129871L ^ pos.getZ() * 116129781L ^ pos.getY() * 0x9E3779B97F4A7C15L;
        h = h * h * 42317861L + h * 11L;
        return h ^ (h >>> 29);
    }

    static boolean chance(BlockPos pos, long seed, double p) {
        return ((hash(pos, seed) >>> 11) & 0xFFFF) / 65536.0 < p;
    }

    /** Whether a block of the plan has rotted or blown away. */
    static boolean removes(Plan plan, char c, BlockPos pos, long seed) {
        double p = switch (c) {
            case 'C' -> 0.12;
            case 'k' -> 0.10;
            case '_' -> 0.08;
            default -> 0;
        };
        return p > 0 && chance(pos, seed + c, p);
    }
}
