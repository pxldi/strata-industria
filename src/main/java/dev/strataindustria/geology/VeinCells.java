package dev.strataindustria.geology;

import dev.strataindustria.material.Metal;
import dev.strataindustria.util.Noise;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntBinaryOperator;

/**
 * Deterministic vein instances per 64x64 cell, computed from the seed and cell coordinates only
 * (worldgen spec 5.5). Any chunk that asks for a cell gets the same list.
 */
public final class VeinCells {
    public static final int CELL_SIZE = 64;
    public static final int MAX_RADIUS = 24;
    private static final int CACHE_LIMIT = 2048;

    public record Settings(int attempts, double shallowBias, int emptyWeight, double sizeMultiplier, boolean spawnGuarantee) {
        public static final Settings DEFAULT = new Settings(3, 0.5, 40, 1.0, true);
    }

    /** One vein. Radii are in blocks; {@code seed} drives every per-block roll. */
    public record Vein(VeinType type, int x, int y, int z, int radiusH, int radiusV, long seed, int surfaceY) {
        public boolean intersects(int minX, int minZ, int maxX, int maxZ) {
            return x + radiusH >= minX && x - radiusH <= maxX && z + radiusH >= minZ && z - radiusH <= maxZ;
        }

        /** Rough number of ore blocks, used for the indicator count. */
        public double estimatedOreBlocks() {
            return 4.0 / 3.0 * Math.PI * radiusH * radiusH * radiusV * type.shape().density() * 0.7;
        }
    }

    private final long seed;
    private final StrataSampler sampler;
    private final List<VeinType> types;
    private final Settings settings;
    /** Surface height (ocean floor) at a column, from the chunk generator. */
    private final IntBinaryOperator surface;
    private final Map<Long, List<Vein>> cache = new ConcurrentHashMap<>();

    public VeinCells(long seed, StrataSampler sampler, List<VeinType> types, Settings settings, IntBinaryOperator surface) {
        this.seed = seed;
        this.sampler = sampler;
        this.types = List.copyOf(types);
        this.settings = settings;
        this.surface = surface;
    }

    public StrataSampler sampler() {
        return sampler;
    }

    public List<Vein> cell(int cellX, int cellZ) {
        long key = ((long) cellX << 32) ^ (cellZ & 0xFFFFFFFFL);
        List<Vein> cached = cache.get(key);
        if (cached != null) return cached;
        List<Vein> veins = compute(cellX, cellZ);
        if (cache.size() > CACHE_LIMIT) cache.clear();
        cache.put(key, veins);
        return veins;
    }

    /** All veins of the 3x3 cells around the given block column. */
    public List<Vein> around(int blockX, int blockZ) {
        int cx = Math.floorDiv(blockX, CELL_SIZE), cz = Math.floorDiv(blockZ, CELL_SIZE);
        List<Vein> out = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                out.addAll(cell(cx + dx, cz + dz));
            }
        }
        return out;
    }

    private List<Vein> compute(int cellX, int cellZ) {
        List<Vein> veins = new ArrayList<>();
        for (int attempt = 0; attempt < settings.attempts(); attempt++) {
            long h = Noise.hash(seed, cellX, cellZ, 1000 + attempt);
            int x = cellX * CELL_SIZE + (int) (Noise.unit(Noise.hash(h, 1)) * CELL_SIZE);
            int z = cellZ * CELL_SIZE + (int) (Noise.unit(Noise.hash(h, 2)) * CELL_SIZE);
            int top = surface.applyAsInt(x, z);
            int y;
            if (Noise.unit(Noise.hash(h, 3)) < settings.shallowBias()) {
                y = top - 40 + (int) (Noise.unit(Noise.hash(h, 4)) * 35);
            } else {
                int max = top - 6;
                y = -56 + (int) (Noise.unit(Noise.hash(h, 4)) * Math.max(1, max + 56 + 1));
            }
            Rock rock = sampler.rockAt(x, y, z, top);
            Vein vein = pick(rock, x, y, z, top, h, false);
            if (vein != null) veins.add(vein);
        }
        if (settings.spawnGuarantee() && cellX == 0 && cellZ == 0) {
            Vein forced = spawnVein(cellX, cellZ);
            if (forced != null) veins.add(forced);
        }
        return List.copyOf(veins);
    }

    private Vein pick(Rock rock, int x, int y, int z, int top, long h, boolean copperOnly) {
        List<VeinType> candidates = new ArrayList<>();
        int total = copperOnly ? 0 : settings.emptyWeight();
        for (VeinType type : types) {
            if (!type.hosts().contains(rock) || y < type.minY() || y > type.maxY()) continue;
            if (copperOnly && !(type.isStoneTier() && type.minerals().getFirst().mineral().primaryMetal() == Metal.COPPER)) continue;
            candidates.add(type);
            total += type.weight();
        }
        if (candidates.isEmpty()) return null;
        double roll = Noise.unit(Noise.hash(h, 5)) * total;
        for (VeinType type : candidates) {
            roll -= type.weight();
            if (roll < 0) {
                int rh = scale(type.shape().radiusHorizontal().sample(Noise.unit(Noise.hash(h, 6))));
                int rv = scale(type.shape().radiusVertical().sample(Noise.unit(Noise.hash(h, 7))));
                return new Vein(type, x, y, z, rh, rv, Noise.hash(h, 8), top);
            }
        }
        return null; // the empty entry
    }

    private int scale(int radius) {
        return Math.max(2, Math.min(MAX_RADIUS, (int) Math.round(radius * settings.sizeMultiplier())));
    }

    /** A shallow stone-tier copper vein near the world origin (worldgen spec 5.6). */
    private Vein spawnVein(int cellX, int cellZ) {
        long h = Noise.hash(seed, cellX, cellZ, 7777);
        int x = cellX * CELL_SIZE + 24 + (int) (Noise.unit(Noise.hash(h, 1)) * 16);
        int z = cellZ * CELL_SIZE + 24 + (int) (Noise.unit(Noise.hash(h, 2)) * 16);
        int top = surface.applyAsInt(x, z);
        for (int depth = 8; depth <= 40; depth += 8) {
            int y = top - depth;
            Vein vein = pick(sampler.rockAt(x, y, z, top), x, y, z, top, Noise.hash(h, depth), true);
            if (vein != null) {
                int lift = Math.max(0, depth - vein.radiusV() - 4);
                return new Vein(vein.type(), x, y + lift, z, vein.radiusH(), vein.radiusV(), vein.seed(), top);
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ per block rules

    /**
     * Normalised distance from the vein centre, slightly warped so veins are not perfect ellipsoids.
     * Values above 1 are outside the vein.
     */
    public static double distance(Vein vein, int x, int y, int z) {
        double dx = (x - vein.x()) / (double) vein.radiusH();
        double dy = (y - vein.y()) / (double) vein.radiusV();
        double dz = (z - vein.z()) / (double) vein.radiusH();
        double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
        return d * (0.8 + 0.4 * Noise.value3(vein.seed(), x, y, z, 1 / 5.0));
    }

    /** Whether the block at a position inside the vein becomes ore. */
    public static boolean isOre(Vein vein, int x, int y, int z, double d) {
        double chance = vein.type().shape().density() * (1 - 0.4 * d);
        return Noise.unit(Noise.hash(vein.seed(), x, y, z)) < chance;
    }

    public static OreMineral mineral(Vein vein, int x, int y, int z) {
        return vein.type().pickMineral(Noise.unit(Noise.hash(vein.seed() + 1, x, y, z)));
    }

    /** Grade from distance and grade shift, with 15% fuzz (worldgen spec 5.4). */
    public static OreGrade grade(Vein vein, Rock host, int x, int y, int z, double d) {
        int shift = 0;
        if (vein.type().preferredHosts().contains(host)) shift++;
        if (y < 0) shift++;
        double rich = shift == 0 ? 0.30 : shift == 1 ? 0.50 : 0.70;
        double normal = shift == 0 ? 0.70 : shift == 1 ? 0.85 : 1.01;
        OreGrade grade = d < rich ? OreGrade.RICH : d < normal ? OreGrade.NORMAL : OreGrade.POOR;
        double fuzz = Noise.unit(Noise.hash(vein.seed() + 2, x, y, z));
        if (fuzz < 0.075) grade = grade.up();
        else if (fuzz < 0.15) grade = grade.down();
        return grade;
    }
}
