package dev.strataindustria.oil;

import com.mojang.serialization.MapCodec;
import dev.strataindustria.registry.Tier6Blocks;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntBinaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Seeps (tier 6 spec 5.1 and 19.1.5): small pools of crude oil in a shallow hollow over a reservoir, the
 * tier's first contact with oil. The pool positions come from the reservoir roll, so the same seed always
 * shows the same seeps; this feature only digs them in.
 */
public record SeepFeature() implements Feature {
    public static final SeepFeature INSTANCE = new SeepFeature();
    public static final MapCodec<SeepFeature> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public MapCodec<SeepFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        ServerLevel server = level.getLevel();
        ChunkPos chunk = new ChunkPos(origin.getX() >> 4, origin.getZ() >> 4);
        // Seeps stay off structures, and structure pieces are placed after this step.
        if (!level.getChunk(chunk.x(), chunk.z()).getAllReferences().isEmpty()) return false;
        var reservoir = OilReservoirs.at(server, chunk);
        if (reservoir.isEmpty()) return false;
        boolean placed = false;
        for (OilReservoir.Seep seep : reservoir.get().seeps()) {
            if (!seep.chunk().equals(chunk)) continue;
            if (!suitable(level, seep.x(), seep.z())) continue;
            RandomSource pool = RandomSource.create(server.getSeed() ^ (31L * seep.x() + seep.z()));
            placed |= placePool(level, seep.x(), seep.z(), seep.size(), pool,
                    (x, z) -> level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z) - 1) > 0;
        }
        return placed;
    }

    /** Dry land with open sky: no water on top, no trees overhead, not in an ocean or river biome. */
    private static boolean suitable(WorldGenLevel level, int x, int z) {
        int floor = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z);
        if (level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) != floor) return false;
        if (level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) != level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z)) {
            return false;
        }
        BlockPos top = new BlockPos(x, floor - 1, z);
        if (!level.getFluidState(top).isEmpty() || level.getBlockState(top).is(BlockTags.LEAVES)) return false;
        return OilReservoirs.isLand(level.getBiome(top));
    }

    /**
     * Digs a pool of {@code size} crude oil sources centred on a column, one block deep, with a sealed rim of
     * oil-stained soil. {@code topAt} gives the topmost solid block of a column. Returns how many sources were
     * placed: 0 when the ground is too uneven.
     */
    public static int placePool(WorldGenLevel level, int cx, int cz, int size, RandomSource random, IntBinaryOperator topAt) {
        Set<Long> cells = new LinkedHashSet<>();
        List<int[]> order = new ArrayList<>();
        cells.add(columnKey(cx, cz));
        order.add(new int[] {cx, cz});
        Direction[] sides = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
        for (int tries = 0; order.size() < size && tries < 64; tries++) {
            int[] from = order.get(random.nextInt(order.size()));
            Direction d = sides[random.nextInt(4)];
            int x = from[0] + d.getStepX(), z = from[1] + d.getStepZ();
            if (Math.abs(x - cx) > 2 || Math.abs(z - cz) > 2 || !cells.add(columnKey(x, z))) continue;
            order.add(new int[] {x, z});
        }

        int low = Integer.MAX_VALUE, high = Integer.MIN_VALUE;
        for (int[] c : order) {
            int top = topAt.applyAsInt(c[0], c[1]);
            low = Math.min(low, top);
            high = Math.max(high, top);
        }
        if (high - low > 2) return 0;
        int y = low;

        BlockState oil = Tier6Blocks.CRUDE_OIL.get().defaultBlockState();
        BlockState stain = Blocks.COARSE_DIRT.defaultBlockState();
        for (int[] c : order) {
            BlockPos below = new BlockPos(c[0], y - 1, c[1]);
            if (!solid(level, below)) level.setBlock(below, stain, Block.UPDATE_CLIENTS);
            for (int up = y + 1; up <= high + 2; up++) {
                BlockPos above = new BlockPos(c[0], up, c[1]);
                if (!level.getBlockState(above).isAir()) level.setBlock(above, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
            level.setBlock(new BlockPos(c[0], y, c[1]), oil, Block.UPDATE_CLIENTS);
        }
        // Seal the rim so nothing flows out, and stain some of the ground around the pool.
        for (int[] c : order) {
            for (Direction d : sides) {
                int x = c[0] + d.getStepX(), z = c[1] + d.getStepZ();
                if (cells.contains(columnKey(x, z))) continue;
                BlockPos rim = new BlockPos(x, y, z);
                if (!solid(level, rim)) {
                    level.setBlock(rim, stain, Block.UPDATE_CLIENTS);
                    for (BlockPos fill = rim.below(); fill.getY() > y - 3 && !solid(level, fill); fill = fill.below()) {
                        level.setBlock(fill, stain, Block.UPDATE_CLIENTS);
                    }
                    BlockPos over = rim.above();
                    BlockState above = level.getBlockState(over);
                    if (!above.isAir() && !solid(level, over)) level.setBlock(over, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                } else if (random.nextInt(3) == 0 && level.getBlockState(rim).is(BlockTags.DIRT) && level.getBlockState(rim.above()).isAir()) {
                    level.setBlock(rim, stain, Block.UPDATE_CLIENTS);
                }
            }
        }
        return order.size();
    }

    private static boolean solid(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getFluidState().isEmpty() && state.isFaceSturdy(level, pos, Direction.UP) && state.isCollisionShapeFullBlock(level, pos);
    }

    private static long columnKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }
}
