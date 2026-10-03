package dev.strataindustria.geology;

import dev.strataindustria.util.Noise;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pure, seeded function from a position to its province, layer and rock (worldgen spec 7.2). It reads
 * no blocks and has no side effects, so any worldgen thread may call it, and tests can use it without
 * a running game.
 */
public final class StrataSampler {
    /** Extra weight factor for a province at a world position, used for the biome bias. */
    @FunctionalInterface
    public interface Weigher {
        double weight(Province province, int x, int z);

        Weigher UNBIASED = (p, x, z) -> p.weight();
    }

    private static final int CACHE_LIMIT = 4096;

    private final long seed;
    private final List<Province> provinces;
    private final int scale;
    private final Weigher weigher;
    private final Map<Long, Province> cellCache = new ConcurrentHashMap<>();

    public StrataSampler(long seed, List<Province> provinces, int scale, Weigher weigher) {
        if (provinces.isEmpty()) throw new IllegalArgumentException("No provinces");
        this.seed = seed;
        this.provinces = List.copyOf(provinces);
        this.scale = scale;
        this.weigher = weigher;
    }

    public long seed() {
        return seed;
    }

    // ------------------------------------------------------------------ provinces

    public Province provinceAt(int x, int z) {
        double wx = x + 96 * (Noise.value2(seed + 11, x, z, 1 / 256.0) * 2 - 1);
        double wz = z + 96 * (Noise.value2(seed + 12, x, z, 1 / 256.0) * 2 - 1);
        int cx = Math.floorDiv((int) Math.floor(wx), scale);
        int cz = Math.floorDiv((int) Math.floor(wz), scale);
        int bestX = cx, bestZ = cz;
        double best = Double.MAX_VALUE;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                double ox = centreX(cx + dx, cz + dz) - wx;
                double oz = centreZ(cx + dx, cz + dz) - wz;
                double d = ox * ox + oz * oz;
                if (d < best) {
                    best = d;
                    bestX = cx + dx;
                    bestZ = cz + dz;
                }
            }
        }
        return cellProvince(bestX, bestZ);
    }

    double centreX(int cx, int cz) {
        return (cx + 0.5 + (Noise.unit(Noise.hash(seed, cx, cz, 1)) - 0.5) * 0.8) * scale;
    }

    double centreZ(int cx, int cz) {
        return (cz + 0.5 + (Noise.unit(Noise.hash(seed, cx, cz, 2)) - 0.5) * 0.8) * scale;
    }

    /** The province of a cell, rerolled once if it equals the base roll of its west or north neighbour. */
    public Province cellProvince(int cx, int cz) {
        long key = ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
        Province cached = cellCache.get(key);
        if (cached != null) return cached;
        Province p = roll(cx, cz, 0);
        if (p == roll(cx - 1, cz, 0) || p == roll(cx, cz - 1, 0)) {
            p = roll(cx, cz, 1);
        }
        if (cellCache.size() > CACHE_LIMIT) cellCache.clear();
        cellCache.put(key, p);
        return p;
    }

    private Province roll(int cx, int cz, int salt) {
        int x = (int) centreX(cx, cz), z = (int) centreZ(cx, cz);
        double[] weights = new double[provinces.size()];
        double total = 0;
        for (int i = 0; i < weights.length; i++) {
            weights[i] = Math.max(0, weigher.weight(provinces.get(i), x, z));
            total += weights[i];
        }
        if (total <= 0) return provinces.getFirst();
        double r = Noise.unit(Noise.hash(seed, cx, cz, 100 + salt)) * total;
        for (int i = 0; i < weights.length; i++) {
            r -= weights[i];
            if (r < 0) return provinces.get(i);
        }
        return provinces.getLast();
    }

    // ------------------------------------------------------------------ layers

    /** Layer boundaries of one column: blocks above {@code topBottom} are top layer, and so on. */
    public record Column(Province province, int topBottom, int middleBottom) {
        public Province.Layer layer(int y) {
            if (y > topBottom) return Province.Layer.TOP;
            if (y > middleBottom) return Province.Layer.MIDDLE;
            return Province.Layer.BOTTOM;
        }

        public Rock rock(int y) {
            return province.rock(layer(y));
        }
    }

    public Column column(int x, int z, int surfaceY) {
        double t1 = 12 + 28 * Noise.value2(seed + 21, x, z, 1 / 128.0);
        double wobbleTop = 3 * (Noise.value2(seed + 23, x, z, 1 / 24.0) * 2 - 1);
        double wobbleMid = 3 * (Noise.value2(seed + 24, x, z, 1 / 24.0) * 2 - 1);
        int topBottom = (int) Math.floor(surfaceY - t1 + wobbleTop);
        int middleBottom = (int) Math.floor(24 * (Noise.value2(seed + 22, x, z, 1 / 192.0) * 2 - 1) + wobbleMid);
        if (topBottom - middleBottom < 8) middleBottom = topBottom - 8;
        return new Column(provinceAt(x, z), topBottom, middleBottom);
    }

    public Rock rockAt(int x, int y, int z, int surfaceY) {
        return column(x, z, surfaceY).rock(y);
    }
}
