package dev.strataindustria.prospecting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.block.OreBlock;
import dev.strataindustria.geology.OreGrade;
import dev.strataindustria.geology.Rock;
import dev.strataindustria.item.ProspectorsPickItem;
import dev.strataindustria.registry.ModBlocks;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A drilled core (tier 3 spec 8.5, worldgen spec 10): the 3 x 3 column under a core sampler, read from
 * the actual blocks. One row per block of depth, named after the rock that fills most of it, plus the
 * ore and deposit runs found in it.
 */
public record CoreSample(BlockPos origin, int top, List<Row> rows, List<Find> finds) {
    public static final int DEPTH = 64;
    public static final int MAX_FINDS = 32;
    static final int CAVE_COLOUR = 0x1c1a1e;
    static final String CAVE = StrataIndustria.MOD_ID + ".core_sample.cave";

    /** One block of depth: what it is called (a translation key) and the colour the strip shows. */
    public record Row(String name, int colour) {
        public static final Codec<Row> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("name").forGetter(Row::name),
                Codec.INT.fieldOf("colour").forGetter(Row::colour)
        ).apply(i, Row::new));
        public static final StreamCodec<ByteBuf, Row> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Row::name,
                ByteBufCodecs.INT, Row::colour,
                Row::new);
    }

    /** A run of one ore or deposit from {@code top} down to {@code bottom}; grade is -1 for ungraded deposits. */
    public record Find(String deposit, int top, int bottom, int grade, int count) {
        public static final Codec<Find> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("deposit").forGetter(Find::deposit),
                Codec.INT.fieldOf("top").forGetter(Find::top),
                Codec.INT.fieldOf("bottom").forGetter(Find::bottom),
                Codec.INT.fieldOf("grade").forGetter(Find::grade),
                Codec.INT.fieldOf("count").forGetter(Find::count)
        ).apply(i, Find::new));
        public static final StreamCodec<ByteBuf, Find> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Find::deposit,
                ByteBufCodecs.VAR_INT, Find::top,
                ByteBufCodecs.VAR_INT, Find::bottom,
                ByteBufCodecs.VAR_INT, Find::grade,
                ByteBufCodecs.VAR_INT, Find::count,
                Find::new);

        public Component name() {
            return Component.translatable(StrataIndustria.MOD_ID + ".ore." + deposit);
        }

        /** "y 33 to 30: hematite (rich), 7 blocks". */
        public Component line() {
            Component depth = top == bottom
                    ? Component.translatable(StrataIndustria.MOD_ID + ".core_sample.at", top)
                    : Component.translatable(StrataIndustria.MOD_ID + ".core_sample.range", top, bottom);
            Component what = grade < 0 || grade == OreGrade.NORMAL.ordinal() ? name() : Component.translatable(StrataIndustria.MOD_ID + ".core_sample.graded", name(),
                    Component.translatable(StrataIndustria.MOD_ID + ".grade." + OreGrade.values()[grade].getSerializedName()));
            return Component.translatable(StrataIndustria.MOD_ID + ".core_sample.find", depth, what, count);
        }
    }

    public static final Codec<CoreSample> CODEC = RecordCodecBuilder.create(i -> i.group(
            BlockPos.CODEC.fieldOf("origin").forGetter(CoreSample::origin),
            Codec.INT.fieldOf("top").forGetter(CoreSample::top),
            Row.CODEC.listOf(0, DEPTH).fieldOf("rows").forGetter(CoreSample::rows),
            Find.CODEC.listOf(0, MAX_FINDS).fieldOf("finds").forGetter(CoreSample::finds)
    ).apply(i, CoreSample::new));

    public static final StreamCodec<ByteBuf, CoreSample> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, CoreSample::origin,
            ByteBufCodecs.VAR_INT, CoreSample::top,
            Row.STREAM_CODEC.apply(ByteBufCodecs.list(DEPTH)), CoreSample::rows,
            Find.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_FINDS)), CoreSample::finds,
            CoreSample::new);

    public CoreSample {
        rows = List.copyOf(rows);
        finds = List.copyOf(finds);
    }

    /** A layer of rock: consecutive rows with the same name, from {@code top} down to {@code bottom}. */
    public record Segment(String name, int top, int bottom) {
        /** "y 62 to 41: shale". */
        public Component line() {
            Component depth = top == bottom
                    ? Component.translatable(StrataIndustria.MOD_ID + ".core_sample.at", top)
                    : Component.translatable(StrataIndustria.MOD_ID + ".core_sample.range", top, bottom);
            return Component.translatable(StrataIndustria.MOD_ID + ".core_sample.layer", depth, Component.translatable(name));
        }
    }

    public List<Segment> segments() {
        List<Segment> segments = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            String name = rows.get(i).name();
            int y = top - i;
            if (!segments.isEmpty() && segments.getLast().name().equals(name)) {
                Segment last = segments.removeLast();
                segments.add(new Segment(name, last.top(), y));
            } else {
                segments.add(new Segment(name, y, y));
            }
        }
        return segments;
    }

    /** Deposits by total blocks found, most first. */
    public List<String> mainDeposits() {
        Map<String, Integer> totals = new LinkedHashMap<>();
        for (Find find : finds) totals.merge(find.deposit(), find.count(), Integer::sum);
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(totals.entrySet());
        sorted.sort(Map.Entry.<String, Integer>comparingByValue().reversed());
        return sorted.stream().map(Map.Entry::getKey).toList();
    }

    /** Tooltip: where it was drilled and the three main finds. */
    public void tooltip(List<Component> lines) {
        lines.add(Component.translatable(StrataIndustria.MOD_ID + ".core_sample.origin", origin.getX(), origin.getY(), origin.getZ())
                .withStyle(ChatFormatting.GRAY));
        List<String> main = mainDeposits();
        if (main.isEmpty()) {
            lines.add(Component.translatable(StrataIndustria.MOD_ID + ".core_sample.barren").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        MutableComponent found = Component.empty();
        for (int i = 0; i < Math.min(3, main.size()); i++) {
            if (i > 0) found.append(", ");
            found.append(Component.translatable(StrataIndustria.MOD_ID + ".ore." + main.get(i)));
        }
        lines.add(Component.translatable(StrataIndustria.MOD_ID + ".core_sample.contains", found).withStyle(ChatFormatting.GRAY));
    }

    // ------------------------------------------------------------------ drilling

    private static Map<Block, Rock> rockBlocks;

    private static Rock rockOf(Block block) {
        if (rockBlocks == null) {
            Map<Block, Rock> map = new HashMap<>();
            for (Rock rock : Rock.values()) {
                map.put(ModBlocks.RAW_ROCK.get(rock).get(), rock);
                map.put(ModBlocks.COBBLED_ROCK.get(rock).get(), rock);
            }
            rockBlocks = map;
        }
        if (block instanceof OreBlock ore && ore.rock() != null) return ore.rock();
        return rockBlocks.get(block);
    }

    private static String layerName(BlockState state) {
        if (state.isAir()) return CAVE;
        Rock rock = rockOf(state.getBlock());
        if (rock != null) return ModBlocks.RAW_ROCK.get(rock).get().getDescriptionId();
        return state.getBlock().getDescriptionId();
    }

    private static int layerColour(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.isAir()) return CAVE_COLOUR;
        Rock rock = rockOf(state.getBlock());
        int colour = rock != null ? rock.mapColor().col : state.getMapColor(level, pos).col;
        return colour == 0 ? CAVE_COLOUR : colour;
    }

    /** Reads the 3 x 3 column under {@code sampler}, from the block below it down {@link #DEPTH} blocks or to the bottom of the world. */
    public static CoreSample take(ServerLevel level, BlockPos sampler) {
        int top = sampler.getY() - 1;
        int bottom = Math.max(level.getMinY(), top - DEPTH + 1);
        List<Row> rows = new ArrayList<>();
        List<Find> finds = new ArrayList<>();
        Map<String, int[]> open = new LinkedHashMap<>(); // deposit -> {top, bottom, grade, count}
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = top; y >= bottom; y--) {
            Map<String, Integer> names = new LinkedHashMap<>();
            Map<String, Integer> colours = new HashMap<>();
            Map<String, int[]> here = new LinkedHashMap<>();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    pos.set(sampler.getX() + dx, y, sampler.getZ() + dz);
                    BlockState state = level.getBlockState(pos);
                    String name = layerName(state);
                    names.merge(name, 1, Integer::sum);
                    colours.putIfAbsent(name, layerColour(level, pos, state));
                    String deposit = ProspectorsPickItem.deposit(state);
                    if (deposit != null) {
                        int grade = state.hasProperty(OreGrade.PROPERTY) ? state.getValue(OreGrade.PROPERTY).ordinal() : -1;
                        int[] seen = here.computeIfAbsent(deposit, d -> new int[] {-1, 0});
                        seen[0] = Math.max(seen[0], grade);
                        seen[1]++;
                    }
                }
            }
            String layer = names.entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow().getKey();
            rows.add(new Row(layer, colours.get(layer)));
            // Runs of one deposit stay together across a single barren row.
            for (Map.Entry<String, int[]> entry : here.entrySet()) {
                int[] run = open.get(entry.getKey());
                if (run != null && run[1] - y <= 2) {
                    run[1] = y;
                    run[2] = Math.max(run[2], entry.getValue()[0]);
                    run[3] += entry.getValue()[1];
                } else {
                    if (run != null) finds.add(new Find(entry.getKey(), run[0], run[1], run[2], run[3]));
                    open.put(entry.getKey(), new int[] {y, y, entry.getValue()[0], entry.getValue()[1]});
                }
            }
        }
        open.forEach((deposit, run) -> finds.add(new Find(deposit, run[0], run[1], run[2], run[3])));
        finds.sort((a, b) -> Integer.compare(b.top(), a.top()));
        return new CoreSample(sampler.immutable(), top, rows, finds.size() > MAX_FINDS ? finds.subList(0, MAX_FINDS) : finds);
    }
}
