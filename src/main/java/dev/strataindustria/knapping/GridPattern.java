package dev.strataindustria.knapping;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.ArrayList;
import java.util.List;

/** A 5x5 shaping pattern. {@code #} is material kept, {@code .} is removed; rows are top first. */
public final class GridPattern {
    public static final int SIZE = 5;
    public static final int CELLS = SIZE * SIZE;
    public static final int FULL = (1 << CELLS) - 1;

    public static final Codec<Integer> CODEC = Codec.STRING.listOf().comapFlatMap(GridPattern::parse, GridPattern::rows);

    private GridPattern() {}

    public static DataResult<Integer> parse(List<String> rows) {
        if (rows.size() != SIZE) return DataResult.error(() -> "A shaping pattern needs " + SIZE + " rows, got " + rows.size());
        int mask = 0;
        for (int y = 0; y < SIZE; y++) {
            String row = rows.get(y);
            if (row.length() != SIZE) {
                String bad = row;
                return DataResult.error(() -> "Pattern row '" + bad + "' must be " + SIZE + " characters long");
            }
            for (int x = 0; x < SIZE; x++) {
                char c = row.charAt(x);
                if (c == '#') mask |= bit(x, y);
                else if (c != '.') return DataResult.error(() -> "Unknown pattern character '" + c + "'");
            }
        }
        return DataResult.success(mask);
    }

    public static List<String> rows(int mask) {
        List<String> rows = new ArrayList<>(SIZE);
        for (int y = 0; y < SIZE; y++) {
            StringBuilder row = new StringBuilder(SIZE);
            for (int x = 0; x < SIZE; x++) row.append((mask & bit(x, y)) != 0 ? '#' : '.');
            rows.add(row.toString());
        }
        return rows;
    }

    public static int bit(int x, int y) {
        return 1 << (y * SIZE + x);
    }

    public static int mirror(int mask) {
        int out = 0;
        for (int y = 0; y < SIZE; y++)
            for (int x = 0; x < SIZE; x++)
                if ((mask & bit(x, y)) != 0) out |= bit(SIZE - 1 - x, y);
        return out;
    }
}
