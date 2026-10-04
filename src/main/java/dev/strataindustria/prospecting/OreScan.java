package dev.strataindustria.prospecting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.strataindustria.geology.OreGrade;
import dev.strataindustria.item.ProspectorsPickItem;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

/**
 * What an ore scanner found (spec 13.2): the 3 x 3 chunks around the player, row by row from north-west. Each tile
 * holds the vanilla map colours of its surface (one character per block, the colour id offset by 48) and the ores
 * and deposits in it with counts, the Y range and the commonest grade.
 */
public record OreScan(int chunkX, int chunkZ, List<Tile> tiles) {
    public static final int SIDE = 3;
    private static final int COLOUR_OFFSET = 48;

    public record Find(String ore, int count, int minY, int maxY, int grade) {
        public static final Codec<Find> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("ore").forGetter(Find::ore),
                Codec.INT.fieldOf("count").forGetter(Find::count),
                Codec.INT.fieldOf("min_y").forGetter(Find::minY),
                Codec.INT.fieldOf("max_y").forGetter(Find::maxY),
                Codec.INT.fieldOf("grade").forGetter(Find::grade)
        ).apply(i, Find::new));
        public static final StreamCodec<ByteBuf, Find> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Find::ore,
                ByteBufCodecs.VAR_INT, Find::count,
                ByteBufCodecs.VAR_INT, Find::minY,
                ByteBufCodecs.VAR_INT, Find::maxY,
                ByteBufCodecs.VAR_INT, Find::grade,
                Find::new);
    }

    public record Tile(String colours, List<Find> finds) {
        public static final Codec<Tile> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("colours").forGetter(Tile::colours),
                Find.CODEC.listOf().fieldOf("finds").forGetter(Tile::finds)
        ).apply(i, Tile::new));
        public static final StreamCodec<ByteBuf, Tile> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Tile::colours,
                Find.STREAM_CODEC.apply(ByteBufCodecs.list()), Tile::finds,
                Tile::new);

        /** The map colour of one block of the surface, as an ARGB value. */
        public int argb(int x, int z) {
            int index = colours.charAt(z * 16 + x) - COLOUR_OFFSET;
            MapColor colour = MapColor.byId(Math.clamp(index, 0, 63));
            return colour.calculateARGBColor(MapColor.Brightness.NORMAL);
        }
    }

    public static final Codec<OreScan> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("chunk_x").forGetter(OreScan::chunkX),
            Codec.INT.fieldOf("chunk_z").forGetter(OreScan::chunkZ),
            Tile.CODEC.listOf().fieldOf("tiles").forGetter(OreScan::tiles)
    ).apply(i, OreScan::new));
    public static final StreamCodec<ByteBuf, OreScan> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OreScan::chunkX,
            ByteBufCodecs.VAR_INT, OreScan::chunkZ,
            Tile.STREAM_CODEC.apply(ByteBufCodecs.list()), OreScan::tiles,
            OreScan::new);

    /** Totals over all tiles, most first: ore id to count. */
    public Map<String, Integer> totals() {
        Map<String, Integer> totals = new LinkedHashMap<>();
        for (Tile tile : tiles) for (Find find : tile.finds()) totals.merge(find.ore(), find.count(), Integer::sum);
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(totals.entrySet());
        sorted.sort(Map.Entry.<String, Integer>comparingByValue().reversed());
        Map<String, Integer> ordered = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : sorted) ordered.put(entry.getKey(), entry.getValue());
        return ordered;
    }

    /** Scans the loaded chunks of the 3 x 3 around a position, one chunk at a time; a chunk not loaded comes back empty. */
    public static OreScan scan(ServerLevel level, BlockPos centre) {
        int originX = (centre.getX() >> 4) - 1, originZ = (centre.getZ() >> 4) - 1;
        List<Tile> tiles = new ArrayList<>();
        for (int dz = 0; dz < SIDE; dz++) {
            for (int dx = 0; dx < SIDE; dx++) tiles.add(scanChunk(level, originX + dx, originZ + dz));
        }
        return new OreScan(originX, originZ, tiles);
    }

    private static Tile scanChunk(ServerLevel level, int cx, int cz) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
        if (chunk == null) return new Tile(String.valueOf((char) COLOUR_OFFSET).repeat(256), List.of());
        StringBuilder colours = new StringBuilder(256);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                pos.set((cx << 4) + x, y - 1, (cz << 4) + z);
                colours.append((char) (COLOUR_OFFSET + chunk.getBlockState(pos).getMapColor(level, pos).id));
            }
        }
        // ore id to {count, min y, max y, poor, normal, rich}
        Map<String, int[]> found = new LinkedHashMap<>();
        for (int index = 0; index < chunk.getSectionsCount(); index++) {
            LevelChunkSection section = chunk.getSection(index);
            if (section.hasOnlyAir() || !section.maybeHas(state -> ProspectorsPickItem.deposit(state) != null)) continue;
            int baseY = (level.getMinSectionY() + index) << 4;
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BlockState state = section.getBlockState(x, y, z);
                        String ore = ProspectorsPickItem.deposit(state);
                        if (ore == null) continue;
                        int[] entry = found.computeIfAbsent(ore, k -> new int[]{0, Integer.MAX_VALUE, Integer.MIN_VALUE, 0, 0, 0});
                        entry[0]++;
                        entry[1] = Math.min(entry[1], baseY + y);
                        entry[2] = Math.max(entry[2], baseY + y);
                        if (state.hasProperty(OreGrade.PROPERTY)) entry[3 + state.getValue(OreGrade.PROPERTY).ordinal()]++;
                    }
                }
            }
        }
        List<Find> finds = new ArrayList<>();
        for (Map.Entry<String, int[]> entry : found.entrySet()) {
            int[] v = entry.getValue();
            int grade = -1, best = 0;
            for (int g = 0; g < 3; g++) {
                if (v[3 + g] > best) {
                    best = v[3 + g];
                    grade = g;
                }
            }
            finds.add(new Find(entry.getKey(), v[0], v[1], v[2], grade));
        }
        finds.sort(Comparator.comparingInt(Find::count).reversed());
        return new Tile(colours.toString(), finds);
    }
}
