package dev.strataindustria.oil;

import dev.strataindustria.util.Noise;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.level.ChunkPos;

/**
 * An oil reservoir (tier 6 spec 5.1 and 19.1). It is not made of blocks: it is a footprint of chunks, a top
 * Y and a capacity in mB of crude oil, rolled from the world seed per field cell of 8 x 8 chunks. How much
 * is left lives in {@link OilReservoirData} once a well draws from it.
 *
 * @param cellX     field cell, in units of {@link #CELL_CHUNKS} chunks
 * @param centreX   footprint centre in chunk units (continuous)
 * @param radiusX   half axis of the footprint ellipse in chunks
 * @param chunks    every chunk inside the footprint
 * @param seeps     surface seep pools, if the reservoir shows at the surface
 */
public record OilReservoir(int cellX, int cellZ, double centreX, double centreZ, double radiusX, double radiusZ, List<ChunkPos> chunks,
        int topY, long capacity, List<Seep> seeps) {
    public static final int CELL_CHUNKS = 8;
    /** Spec 19.1.4: crude per footprint chunk, before the 0.7 to 1.3 factor. */
    public static final long MB_PER_CHUNK = 300_000;
    public static final int MIN_TOP_Y = -48, MAX_TOP_Y = 24;
    /** Spec 19.1.3: the top is at least this far below the surface at the footprint centre. */
    public static final int MIN_COVER = 32;
    /** Spec 19.1.5: the share of reservoirs under sedimentary land that show seeps. */
    public static final double SEEP_CHANCE = 0.35;
    private static final long SALT = 0x0111_5EE9L;

    /** A seep pool: its centre block column and how many crude oil source blocks it holds (2 to 6). */
    public record Seep(int x, int z, int size) {
        public ChunkPos chunk() {
            return new ChunkPos(x >> 4, z >> 4);
        }
    }

    /** Spec 5.1: size classes for display. */
    public enum SizeClass {
        SMALL, MEDIUM, LARGE;

        public static SizeClass of(long capacity) {
            return capacity < 3_000_000 ? SMALL : capacity <= 6_000_000 ? MEDIUM : LARGE;
        }

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** What the roll needs to know about the world, so the roll itself stays a pure function of the seed. */
    public interface Site {
        /** Spec 19.1.2: the chance of a reservoir in a cell whose centre is at this block column. */
        double chance(int x, int z);

        /** Spec 19.1.5: whether this column is land under a sedimentary top layer. */
        boolean seepsPossible(int x, int z);

        /** The worldgen surface height at a block column. */
        int surface(int x, int z);
    }

    public SizeClass sizeClass() {
        return SizeClass.of(capacity);
    }

    public boolean contains(ChunkPos pos) {
        return Math.floorDiv(pos.x(), CELL_CHUNKS) == cellX && Math.floorDiv(pos.z(), CELL_CHUNKS) == cellZ && inEllipse(pos.x(), pos.z());
    }

    /** The block column at the footprint centre. */
    public int centreBlockX() {
        return (int) Math.floor(centreX * 16);
    }

    public int centreBlockZ() {
        return (int) Math.floor(centreZ * 16);
    }

    /** A key unique per field cell, for saved data. */
    public long key() {
        return cellKey(cellX, cellZ);
    }

    public static long cellKey(int cellX, int cellZ) {
        return ((long) cellX << 32) ^ (cellZ & 0xFFFFFFFFL);
    }

    private boolean inEllipse(int chunkX, int chunkZ) {
        double dx = (chunkX + 0.5 - centreX) / radiusX, dz = (chunkZ + 0.5 - centreZ) / radiusZ;
        return dx * dx + dz * dz <= 1;
    }

    /** Rolls the reservoir of one field cell, or none. Same seed and site, same answer. */
    public static Optional<OilReservoir> roll(long seed, int cellX, int cellZ, Site site) {
        long s = seed ^ SALT;
        int originX = cellX * CELL_CHUNKS, originZ = cellZ * CELL_CHUNKS;
        int cellCentreX = originX * 16 + CELL_CHUNKS * 8, cellCentreZ = originZ * 16 + CELL_CHUNKS * 8;
        if (Noise.unit(Noise.hash(s, cellX, cellZ, 0)) >= site.chance(cellCentreX, cellCentreZ)) return Optional.empty();

        // Spec 19.1.3: an ellipse 3 to 6 chunks on each axis, wholly inside the cell.
        int width = 3 + (int) (Noise.unit(Noise.hash(s, cellX, cellZ, 1)) * 4);
        int depth = 3 + (int) (Noise.unit(Noise.hash(s, cellX, cellZ, 2)) * 4);
        double centreX = originX + width / 2.0 + Noise.unit(Noise.hash(s, cellX, cellZ, 3)) * (CELL_CHUNKS - width);
        double centreZ = originZ + depth / 2.0 + Noise.unit(Noise.hash(s, cellX, cellZ, 4)) * (CELL_CHUNKS - depth);
        OilReservoir shape = new OilReservoir(cellX, cellZ, centreX, centreZ, width / 2.0, depth / 2.0, List.of(), 0, 0, List.of());
        List<ChunkPos> chunks = new ArrayList<>();
        for (int x = originX; x < originX + CELL_CHUNKS; x++) {
            for (int z = originZ; z < originZ + CELL_CHUNKS; z++) {
                if (shape.inEllipse(x, z)) chunks.add(new ChunkPos(x, z));
            }
        }
        if (chunks.isEmpty()) chunks.add(new ChunkPos((int) Math.floor(centreX), (int) Math.floor(centreZ)));

        int bx = shape.centreBlockX(), bz = shape.centreBlockZ();
        int top = MIN_TOP_Y + (int) (Noise.unit(Noise.hash(s, cellX, cellZ, 5)) * (MAX_TOP_Y - MIN_TOP_Y + 1));
        top = Math.min(top, site.surface(bx, bz) - MIN_COVER);
        double factor = 0.7 + 0.6 * Noise.unit(Noise.hash(s, cellX, cellZ, 6));
        long capacity = Math.round(chunks.size() * MB_PER_CHUNK * factor);

        List<Seep> seeps = new ArrayList<>();
        if (Noise.unit(Noise.hash(s, cellX, cellZ, 7)) < SEEP_CHANCE && site.seepsPossible(bx, bz)) {
            int count = 1 + (int) (Noise.unit(Noise.hash(s, cellX, cellZ, 8)) * 3);
            for (int i = 0; i < count; i++) {
                ChunkPos chunk = chunks.get((int) (Noise.unit(Noise.hash(s, cellX, cellZ, 10 + i * 4)) * chunks.size()));
                // Keep the pool clear of chunk edges so it is placed whole by one chunk's feature.
                int x = chunk.x() * 16 + 4 + (int) (Noise.unit(Noise.hash(s, cellX, cellZ, 11 + i * 4)) * 8);
                int z = chunk.z() * 16 + 4 + (int) (Noise.unit(Noise.hash(s, cellX, cellZ, 12 + i * 4)) * 8);
                int size = 2 + (int) (Noise.unit(Noise.hash(s, cellX, cellZ, 13 + i * 4)) * 5);
                seeps.add(new Seep(x, z, size));
            }
        }
        return Optional.of(new OilReservoir(cellX, cellZ, centreX, centreZ, width / 2.0, depth / 2.0, List.copyOf(chunks), top, capacity,
                List.copyOf(seeps)));
    }
}
