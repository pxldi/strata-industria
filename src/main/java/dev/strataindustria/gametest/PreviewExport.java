package dev.strataindustria.gametest;

import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.structure.AditPiece;
import dev.strataindustria.structure.Plan;
import dev.strataindustria.structure.PlanPiece;
import dev.strataindustria.structure.Plans;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;

/**
 * Dev tooling for tools/structure-preview: builds structures on bare ground and writes their blocks as JSON.
 * Only runs when the system property {@code strata.previewDir} is set, so normal test runs skip it.
 */
final class PreviewExport {
    private static final int PAD = 3;

    private PreviewExport() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("structure_preview_export", PreviewExport::export);
    }

    private static void export(GameTestHelper helper) {
        String dir = System.getProperty("strata.previewDir");
        if (dir == null) {
            helper.succeed();
            return;
        }
        String only = System.getProperty("strata.previewOnly");
        ServerLevel level = helper.getLevel();
        int groundY = helper.absolutePos(new BlockPos(0, 1, 0)).getY();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        try {
            Files.createDirectories(Path.of(dir));
            for (Plan plan : Plans.all()) {
                String name = plan.id().replace('/', '_');
                if (only != null && !name.matches(only)) continue;
                int minX = origin.getX() + 20, minZ = origin.getZ() + 20;
                BoundingBox area = new BoundingBox(minX - 14, groundY - 6, minZ - 14, minX + plan.width() + 14, groundY + 24, minZ + plan.depth() + 14);
                ground(level, area, groundY, false);
                build(level, new PlanPiece(plan, Rotation.NONE, minX, minZ, groundY, OreMineral.MALACHITE, PlanPiece.Wood.OAK, 7L), area);
                write(level, Path.of(dir, name + ".json"), name, new BoundingBox(minX - PAD, groundY - 2, minZ - PAD, minX + plan.width() + PAD,
                        groundY + 20, minZ + plan.depth() + PAD));
            }
            if (only == null || "collapsed_adit".matches(only)) {
                BlockPos portal = new BlockPos(origin.getX() + 40, groundY, origin.getZ() + 40);
                BoundingBox area = new BoundingBox(portal.getX() - 14, groundY - 8, portal.getZ() - 6, portal.getX() + 14, groundY + 24, portal.getZ() + 26);
                ground(level, area, groundY, true);
                build(level, AditPiece.ruined(portal, Direction.SOUTH, 14), area);
                write(level, Path.of(dir, "collapsed_adit.json"), "collapsed_adit", new BoundingBox(portal.getX() - 10, groundY - 6,
                        portal.getZ() - 4, portal.getX() + 10, groundY + 14, portal.getZ() + 20));
            }
        } catch (IOException e) {
            helper.fail(Component(e));
            return;
        }
        helper.succeed();
    }

    private static net.minecraft.network.chat.Component Component(IOException e) {
        return net.minecraft.network.chat.Component.literal(String.valueOf(e));
    }

    /** Grass over dirt; with {@code hill}, a stone hillside rising to the south for embedded pieces. */
    private static void ground(ServerLevel level, BoundingBox area, int groundY, boolean hill) {
        for (int cx = area.minX() >> 4; cx <= area.maxX() >> 4; cx++) {
            for (int cz = area.minZ() >> 4; cz <= area.maxZ() >> 4; cz++) level.getChunk(cx, cz);
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = area.minX(); x <= area.maxX(); x++) {
            for (int z = area.minZ(); z <= area.maxZ(); z++) {
                int top = groundY;
                if (hill && z > (area.minZ() + 6)) top = groundY + 8;
                for (int y = area.minY(); y <= area.maxY(); y++) {
                    BlockState s = y > top ? Blocks.AIR.defaultBlockState() : y == top ? Blocks.GRASS_BLOCK.defaultBlockState()
                            : hill && y > groundY - 1 ? Blocks.STONE.defaultBlockState() : Blocks.DIRT.defaultBlockState();
                    level.setBlock(pos.set(x, y, z), s, Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    private static void build(ServerLevel level, StructurePiece piece, BoundingBox area) {
        BoundingBox whole = new BoundingBox(area.minX() - 16, area.minY(), area.minZ() - 16, area.maxX() + 16, area.maxY(), area.maxZ() + 16);
        piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.getRandom(), whole,
                new ChunkPos(area.minX() >> 4, area.minZ() >> 4), new BlockPos(area.minX(), area.minY(), area.minZ()));
    }

    private static void write(ServerLevel level, Path file, String name, BoundingBox box) throws IOException {
        Map<String, Integer> palette = new LinkedHashMap<>();
        List<String> blocks = new ArrayList<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int y = box.minY(); y <= box.maxY(); y++) {
                for (int z = box.minZ(); z <= box.maxZ(); z++) {
                    BlockState s = level.getBlockState(pos.set(x, y, z));
                    if (s.isAir()) continue;
                    int id = palette.computeIfAbsent(stateString(s), k -> palette.size());
                    blocks.add("[" + (x - box.minX()) + "," + (y - box.minY()) + "," + (z - box.minZ()) + "," + id + "]");
                }
            }
        }
        StringBuilder sb = new StringBuilder("{\"name\":\"").append(name).append("\",\"size\":[").append(box.getXSpan()).append(',')
                .append(box.getYSpan()).append(',').append(box.getZSpan()).append("],\"palette\":[");
        boolean first = true;
        for (String p : palette.keySet()) {
            sb.append(first ? "" : ",").append('"').append(p).append('"');
            first = false;
        }
        sb.append("],\"blocks\":[").append(String.join(",", blocks)).append("]}");
        Files.writeString(file, sb.toString());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static String stateString(BlockState state) {
        StringBuilder sb = new StringBuilder(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
        if (state.getProperties().isEmpty()) return sb.toString();
        sb.append('[');
        boolean first = true;
        for (Property p : state.getProperties()) {
            sb.append(first ? "" : ",").append(p.getName()).append('=').append(p.getName(state.getValue(p)));
            first = false;
        }
        return sb.append(']').toString();
    }
}
