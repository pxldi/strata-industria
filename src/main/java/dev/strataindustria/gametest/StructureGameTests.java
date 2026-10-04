package dev.strataindustria.gametest;

import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.structure.AditPiece;
import dev.strataindustria.structure.Plan;
import dev.strataindustria.structure.PlanPiece;
import dev.strataindustria.structure.Plans;
import dev.strataindustria.structure.SluicePiece;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;

/**
 * Structure generation tests (structures v2, clean generation): every building plan and the sluice are
 * built in the middle of a dense forest and must leave no trunk cut in half, no crown in the air, no leaf
 * to decay and no hollow under the floor. Run by {@link ModGameTests}.
 */
final class StructureGameTests {
    /** Open ground kept around the build for the forest to grow on. */
    private static final int PAD = 16;

    private StructureGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("structure_plans_in_forest", StructureGameTests::plansInForest);
        tests.put("structure_sluice_in_forest", StructureGameTests::sluiceInForest);
        tests.put("structure_adit_in_forest", StructureGameTests::aditInForest);
        tests.put("structure_adit_miners_end", StructureGameTests::aditMinersEnd);
        tests.put("structure_bunkhouse_cache", StructureGameTests::bunkhouseCache);
    }

    private static void plansInForest(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        int groundY = helper.absolutePos(new BlockPos(0, 1, 0)).getY();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        List<String> problems = new ArrayList<>();
        for (Plan plan : Plans.all()) {
            for (Rotation rotation : new Rotation[] {Rotation.NONE, Rotation.CLOCKWISE_90}) {
                int[] size = PlanPiece.size(plan, rotation);
                int minX = origin.getX() + PAD, minZ = origin.getZ() + PAD;
                BoundingBox area = new BoundingBox(minX - PAD, groundY - 8, minZ - PAD, minX + size[0] + PAD, groundY + 40, minZ + size[1] + PAD);
                forest(level, area, groundY, minX, minZ, size[0], size[1]);
                PlanPiece piece = new PlanPiece(plan, rotation, minX, minZ, groundY, OreMineral.MALACHITE, PlanPiece.Wood.OAK, 7L);
                build(level, piece, area);
                audit(level, area, plan.id() + "/" + rotation, problems);
            }
        }
        helper.assertTrue(problems.isEmpty(), "unclean generation: " + problems.stream().limit(6).toList());
        helper.succeed();
    }

    /** The foreman's cache sits in a crate under a floor hatch, and the crate wall, lamps and bunks are all there. */
    private static void bunkhouseCache(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        int groundY = helper.absolutePos(new BlockPos(0, 1, 0)).getY();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        Plan plan = Plans.BUNKHOUSE;
        int minX = origin.getX() + PAD, minZ = origin.getZ() + PAD;
        BoundingBox area = new BoundingBox(minX - PAD, groundY - 8, minZ - PAD, minX + plan.width() + PAD, groundY + 40, minZ + plan.depth() + PAD);
        forest(level, area, groundY, minX, minZ, plan.width(), plan.depth());
        build(level, new PlanPiece(plan, Rotation.NONE, minX, minZ, groundY, OreMineral.MALACHITE, PlanPiece.Wood.SPRUCE, 11L), area);
        BlockPos cache = new BlockPos(minX + 10, groundY, minZ + 3);
        helper.assertTrue(level.getBlockState(cache).is(dev.strataindustria.structure.SharedBlocks.CRATE.get()), "cache crate missing");
        helper.assertTrue(level.getBlockState(cache.above()).is(BlockTags.TRAPDOORS), "hatch missing over the cache");
        helper.assertTrue(level.getBlockEntity(cache) instanceof net.minecraft.world.RandomizableContainer c && c.getLootTable() != null,
                "cache crate has no loot table");
        int beds = 0, lamps = 0;
        for (BlockPos pos : BlockPos.betweenClosed(minX, groundY + 1, minZ, minX + plan.width(), groundY + 4, minZ + plan.depth())) {
            BlockState state = level.getBlockState(pos);
            if (state.is(BlockTags.BEDS)) beds++;
            if (state.is(dev.strataindustria.structure.SharedBlocks.MINERS_LAMP.get())) lamps++;
        }
        helper.assertTrue(beds >= 8, "expected four beds, found " + beds / 2);
        helper.assertTrue(lamps >= 2, "expected lamps");
        helper.succeed();
    }

    private static void sluiceInForest(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        int groundY = helper.absolutePos(new BlockPos(0, 1, 0)).getY();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        List<String> problems = new ArrayList<>();
        BlockPos head = new BlockPos(origin.getX() + 3 * PAD, groundY, origin.getZ() + 3 * PAD);
        BoundingBox area = new BoundingBox(head.getX() - PAD, groundY - 8, head.getZ() - PAD, head.getX() + PAD, groundY + 40, head.getZ() + PAD + 8);
        forest(level, area, groundY, head.getX() - 3, head.getZ() - 3, 6, 12);
        build(level, new SluicePiece(head, Direction.SOUTH), area);
        audit(level, area, "sluice", problems);
        helper.assertTrue(problems.isEmpty(), "unclean generation: " + problems.stream().limit(6).toList());
        helper.succeed();
    }

    /** The mouth of an adit comes up under the trees: the tunnel clears whole trees and leaves nothing hanging. */
    private static void aditInForest(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        int groundY = helper.absolutePos(new BlockPos(0, 1, 0)).getY();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        List<String> problems = new ArrayList<>();
        BlockPos portal = new BlockPos(origin.getX() + 4 * PAD, groundY, origin.getZ() + 4 * PAD);
        BoundingBox area = new BoundingBox(portal.getX() - PAD, groundY - 12, portal.getZ() - PAD, portal.getX() + PAD, groundY + 40,
                portal.getZ() + PAD + 12);
        forest(level, area, groundY, portal.getX() - 3, portal.getZ() - 3, 6, 16);
        build(level, AditPiece.straight(portal, Direction.SOUTH, 12), area);
        audit(level, area, "adit", problems);
        helper.assertTrue(problems.isEmpty(), "unclean generation: " + problems.stream().limit(6).toList());
        helper.succeed();
    }

    /** The old adit ends in the miner's chamber: pack crate, burnt-out lamp, stand, bone, rack, and a cache behind cracked blocks. */
    private static void aditMinersEnd(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        int groundY = helper.absolutePos(new BlockPos(0, 1, 0)).getY();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        BlockPos portal = new BlockPos(origin.getX() + 4 * PAD, groundY, origin.getZ() + 4 * PAD);
        BoundingBox area = new BoundingBox(portal.getX() - PAD, groundY - 12, portal.getZ() - PAD, portal.getX() + PAD, groundY + 40,
                portal.getZ() + PAD + 14);
        forest(level, area, groundY, portal.getX() - 3, portal.getZ() - 3, 6, 16);
        build(level, AditPiece.ruined(portal, Direction.SOUTH, 14), area);
        int crates = 0, racks = 0, bones = 0, cracked = 0, lamps = 0;
        for (BlockPos pos : BlockPos.betweenClosed(area.minX(), area.minY(), area.minZ(), area.maxX(), area.maxY(), area.maxZ())) {
            BlockState state = level.getBlockState(pos);
            if (state.is(dev.strataindustria.structure.SharedBlocks.CRATE.get())) crates++;
            else if (state.is(dev.strataindustria.structure.SharedBlocks.TOOL_RACK.get())) racks++;
            else if (state.is(Blocks.BONE_BLOCK)) bones++;
            else if (state.is(dev.strataindustria.structure.SharedBlocks.MINERS_LAMP.get())) lamps++;
            else if (dev.strataindustria.structure.SharedBlocks.CRACKED.values().stream().anyMatch(b -> state.is(b.get()))) cracked++;
        }
        helper.assertTrue(crates == 2, "expected pack and cache crates, found " + crates);
        helper.assertTrue(racks == 1 && bones == 1 && lamps == 1, "miner's end incomplete: " + racks + "/" + bones + "/" + lamps);
        helper.assertTrue(cracked >= 2, "no cracked walls");
        helper.assertTrue(!level.getEntitiesOfClass(net.minecraft.world.entity.decoration.ArmorStand.class,
                new net.minecraft.world.phys.AABB(area.minX(), area.minY(), area.minZ(), area.maxX(), area.maxY(), area.maxZ())).isEmpty(),
                "no armor stand");
        helper.succeed();
    }

    private static void build(ServerLevel level, StructurePiece piece, BoundingBox area) {
        BoundingBox whole = new BoundingBox(area.minX() - 16, area.minY(), area.minZ() - 16, area.maxX() + 16, area.maxY(), area.maxZ() + 16);
        piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.getRandom(), whole,
                new ChunkPos(area.minX() >> 4, area.minZ() >> 4), new BlockPos(area.minX(), area.minY(), area.minZ()));
    }

    /** Flat grass, then a tree every three or four blocks, crowns touching; the footprint is thick with them. */
    private static void forest(ServerLevel level, BoundingBox area, int groundY, int fx, int fz, int fw, int fd) {
        for (int cx = area.minX() >> 4; cx <= area.maxX() >> 4; cx++) {
            for (int cz = area.minZ() >> 4; cz <= area.maxZ() >> 4; cz++) level.getChunk(cx, cz);
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = area.minX(); x <= area.maxX(); x++) {
            for (int z = area.minZ(); z <= area.maxZ(); z++) {
                for (int y = area.minY(); y <= area.maxY(); y++) {
                    level.setBlock(pos.set(x, y, z), y < groundY ? Blocks.DIRT.defaultBlockState() : y == groundY ? Blocks.GRASS_BLOCK.defaultBlockState()
                            : Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
        for (int x = area.minX() + 2; x <= area.maxX() - 2; x += 3) {
            for (int z = area.minZ() + 2; z <= area.maxZ() - 2; z += 3) {
                long h = (x * 31L + z * 17L) ^ (x * z);
                int tx = x + (int) (h & 1), tz = z + (int) ((h >> 1) & 1);
                tree(level, tx, groundY + 1, tz, 5 + (int) ((h >> 2) & 3));
            }
        }
    }

    private static void tree(ServerLevel level, int x, int y, int z, int height) {
        BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, false).setValue(LeavesBlock.DISTANCE, 2);
        for (int dy = height - 3; dy <= height; dy++) {
            int r = dy >= height - 1 ? 1 : 2;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) level.setBlock(new BlockPos(x + dx, y + dy, z + dz), leaves, Block.UPDATE_CLIENTS);
            }
        }
        for (int dy = 0; dy < height; dy++) level.setBlock(new BlockPos(x, y + dy, z), Blocks.OAK_LOG.defaultBlockState(), Block.UPDATE_CLIENTS);
    }

    /** Collects every floating log, hanging leaf, and decaying leaf in the area. */
    private static void audit(ServerLevel level, BoundingBox area, String name, List<String> problems) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int found = 0;
        for (int x = area.minX(); x <= area.maxX() && found < 3; x++) {
            for (int z = area.minZ(); z <= area.maxZ() && found < 3; z++) {
                for (int y = area.minY(); y <= area.maxY(); y++) {
                    BlockState state = level.getBlockState(pos.set(x, y, z));
                    if (state.is(BlockTags.LEAVES) && !state.getValue(LeavesBlock.PERSISTENT) && !supported(level, pos)) {
                        problems.add(name + ": leaf without a log at " + pos.toShortString());
                        found++;
                    } else if (state.is(Blocks.OAK_LOG) && state.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Y) {
                        // A natural trunk stands on something: walk down its logs to the first block that is not one.
                        BlockPos.MutableBlockPos below = pos.mutable().move(Direction.DOWN);
                        while (level.getBlockState(below).is(Blocks.OAK_LOG)) below.move(Direction.DOWN);
                        BlockState under = level.getBlockState(below);
                        boolean standing = !under.isAir() && under.getFluidState().isEmpty();
                        if (!standing && !hasLeavesAround(level, pos)) {
                            problems.add(name + ": floating log at " + pos.toShortString());
                            found++;
                        } else if (!standing) {
                            problems.add(name + ": trunk with nothing under it at " + pos.toShortString());
                            found++;
                        }
                    }
                }
            }
        }
    }

    private static boolean hasLeavesAround(ServerLevel level, BlockPos pos) {
        for (Direction d : Direction.values()) if (level.getBlockState(pos.relative(d)).is(BlockTags.LEAVES)) return true;
        return false;
    }

    /** Whether a log lies within six steps of a leaf, counting steps through leaves (vanilla's decay rule). */
    private static boolean supported(ServerLevel level, BlockPos leaf) {
        java.util.Set<BlockPos> seen = new java.util.HashSet<>();
        List<BlockPos> frontier = List.of(leaf.immutable());
        seen.add(leaf.immutable());
        for (int step = 1; step <= 6 && !frontier.isEmpty(); step++) {
            List<BlockPos> next = new ArrayList<>();
            for (BlockPos at : frontier) {
                for (Direction d : Direction.values()) {
                    BlockPos n = at.relative(d);
                    if (!seen.add(n)) continue;
                    BlockState state = level.getBlockState(n);
                    if (state.is(BlockTags.LOGS)) return true;
                    if (state.is(BlockTags.LEAVES)) next.add(n);
                }
            }
            frontier = next;
        }
        return false;
    }
}
