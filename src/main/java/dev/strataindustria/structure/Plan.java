package dev.strataindustria.structure;

import java.util.List;

/**
 * A hand-drawn building plan (structures spec 6): layers of rows, bottom layer first, one character per
 * block. Layer 0 is the ground the building stands on; layer 1 is the first layer above it. Rows run north
 * to south ({@code z}), characters west to east ({@code x}). Short rows are padded with {@code ' '}.
 *
 * <p>Characters (resolved by {@link PlanPiece}):
 * <pre>
 *   ' ' leave the world as it is (plants and terrain above the ground layer are cleared)
 *   '.' air                         ',' trampled ground (coarse dirt)   'd' dirt
 *   'g' gravel                      '$' suspicious gravel (the plan's archaeology loot)
 *   '#' cobbled local rock          'm' cobbled local rock, mossy      'R' raw local rock
 *   'r' / '1' top rock sample       '2' middle rock sample             '3' bottom rock sample
 *   'C' fibre canvas                '_' fibre canvas carpet
 *   'P' pit prop, upright           '=' stripped log along x           '|' stripped log along z
 *   'L' log pile                    'f' fire pit                       'F' forge   'c' crucible
 *   'A' stone anvil of the bottom rock                                 'B' barrel (the plan's loot)
 *   'E' bed head (pointing north)   'e' bed foot                       'k' fence   's' slab, top half
 *   'H' ladder on a north wall
 *   'n' a pebble of the camp's ore  'o' upright log (a chopping block)
 *   'K' cracked fire bricks         'Y' cracked fire bricks, half the time
 *   'S' slag heap                   'Z' slag heap, half the time
 *   'b' barrel sunk into the ground, half the time                     'l' log pile of two logs
 *   'G' placer gravel
 * </pre>
 */
public record Plan(String id, int width, int depth, List<String[]> layers, Kind kind) {
    public enum Kind {
        /** The ground under the whole footprint is levelled to one height. */
        LEVELLED,
        /** Each column follows its own ground; for paths, heaps and cairns. */
        TERRAIN,
        /** Cut into a hillside: only the drawn blocks change, nothing is levelled or cleared. */
        EMBEDDED
    }

    public static Plan of(String id, Kind kind, String[]... layers) {
        int width = 0, depth = 0;
        for (String[] layer : layers) {
            depth = Math.max(depth, layer.length);
            for (String row : layer) width = Math.max(width, row.length());
        }
        return new Plan(id, width, depth, List.of(layers), kind);
    }

    public int height() {
        return layers.size();
    }

    /** The character at a cell, {@code ' '} outside the drawn rows. */
    public char at(int x, int y, int z) {
        if (y < 0 || y >= layers.size()) return ' ';
        String[] layer = layers.get(y);
        if (z < 0 || z >= layer.length) return ' ';
        String row = layer[z];
        return x < 0 || x >= row.length() ? ' ' : row.charAt(x);
    }
}
