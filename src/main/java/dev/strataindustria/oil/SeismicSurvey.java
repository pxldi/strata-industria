package dev.strataindustria.oil;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.strataindustria.Config;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

/**
 * What a seismic charge told an ore scanner (tier 6 spec 5.2): the reservoirs under the (2 r + 1) x (2 r + 1)
 * chunks centred on the charge, each as the tiles of the area it covers, its size class, the depth of its top and,
 * once a well has struck it, how much is left. Tiles are numbered row by row from the north-west.
 *
 * @param side    chunks along one edge of the surveyed area
 * @param originY height of the charge, to give depths below it
 */
public record SeismicSurvey(int chunkX, int chunkZ, int side, int originY, List<Hit> hits) {
    /** Bits of {@link Hit#edges}: the footprint goes on past that edge of the area. */
    public static final int NORTH = 1, EAST = 2, SOUTH = 4, WEST = 8;

    /**
     * @param size      {@link OilReservoir.SizeClass} ordinal
     * @param topY      height of the top of the reservoir
     * @param remaining percent left, or -1 while no well has struck it
     * @param tiles     bit {@code row * side + column} for each surveyed chunk the reservoir lies under
     */
    public record Hit(int size, int topY, int remaining, long tiles, int edges) {
        public static final Codec<Hit> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("size").forGetter(Hit::size),
                Codec.INT.fieldOf("top_y").forGetter(Hit::topY),
                Codec.INT.fieldOf("remaining").forGetter(Hit::remaining),
                Codec.LONG.fieldOf("tiles").forGetter(Hit::tiles),
                Codec.INT.fieldOf("edges").forGetter(Hit::edges)
        ).apply(i, Hit::new));
        public static final StreamCodec<ByteBuf, Hit> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Hit::size,
                ByteBufCodecs.VAR_INT, Hit::topY,
                ByteBufCodecs.VAR_INT, Hit::remaining,
                ByteBufCodecs.VAR_LONG, Hit::tiles,
                ByteBufCodecs.VAR_INT, Hit::edges,
                Hit::new);

        public OilReservoir.SizeClass sizeClass() {
            return OilReservoir.SizeClass.values()[Math.clamp(size, 0, OilReservoir.SizeClass.values().length - 1)];
        }

        public boolean covers(int side, int column, int row) {
            return (tiles >> (row * side + column) & 1L) != 0;
        }
    }

    public static final Codec<SeismicSurvey> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("chunk_x").forGetter(SeismicSurvey::chunkX),
            Codec.INT.fieldOf("chunk_z").forGetter(SeismicSurvey::chunkZ),
            Codec.INT.fieldOf("side").forGetter(SeismicSurvey::side),
            Codec.INT.fieldOf("origin_y").forGetter(SeismicSurvey::originY),
            Hit.CODEC.listOf().fieldOf("hits").forGetter(SeismicSurvey::hits)
    ).apply(i, SeismicSurvey::new));
    public static final StreamCodec<ByteBuf, SeismicSurvey> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SeismicSurvey::chunkX,
            ByteBufCodecs.VAR_INT, SeismicSurvey::chunkZ,
            ByteBufCodecs.VAR_INT, SeismicSurvey::side,
            ByteBufCodecs.VAR_INT, SeismicSurvey::originY,
            Hit.STREAM_CODEC.apply(ByteBufCodecs.list()), SeismicSurvey::hits,
            SeismicSurvey::new);

    /** Surveys the chunks around a charge. */
    public static SeismicSurvey take(ServerLevel level, BlockPos charge) {
        return build(charge, chunk -> OilReservoirs.at(level, chunk), OilReservoirData.get(level));
    }

    /** The survey of a world given as a way to find the reservoir under a chunk. */
    public static SeismicSurvey build(BlockPos charge, Function<ChunkPos, Optional<OilReservoir>> reservoirs, OilReservoirData data) {
        int radius = Config.SEISMIC_CHUNK_RADIUS.getAsInt();
        int side = 2 * radius + 1;
        int originX = (charge.getX() >> 4) - radius, originZ = (charge.getZ() >> 4) - radius;
        Map<Long, OilReservoir> found = new LinkedHashMap<>();
        Map<Long, Long> tiles = new LinkedHashMap<>();
        for (int row = 0; row < side; row++) {
            for (int column = 0; column < side; column++) {
                OilReservoir reservoir = reservoirs.apply(new ChunkPos(originX + column, originZ + row)).orElse(null);
                if (reservoir == null) continue;
                found.putIfAbsent(reservoir.key(), reservoir);
                tiles.merge(reservoir.key(), 1L << (row * side + column), (a, b) -> a | b);
            }
        }
        List<Hit> hits = new ArrayList<>();
        for (OilReservoir reservoir : found.values()) {
            int edges = 0;
            for (ChunkPos chunk : reservoir.chunks()) {
                if (chunk.z() < originZ) edges |= NORTH;
                if (chunk.z() >= originZ + side) edges |= SOUTH;
                if (chunk.x() < originX) edges |= WEST;
                if (chunk.x() >= originX + side) edges |= EAST;
            }
            int remaining = data.tapped(reservoir) ? (int) Math.round(data.fraction(reservoir) * 100) : -1;
            hits.add(new Hit(reservoir.sizeClass().ordinal(), reservoir.topY(), remaining, tiles.get(reservoir.key()), edges));
        }
        return new SeismicSurvey(originX, originZ, side, charge.getY(), List.copyOf(hits));
    }
}
