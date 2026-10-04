package dev.strataindustria.gametest;

import com.mojang.authlib.GameProfile;
import dev.strataindustria.power.HandCrankBlock;
import dev.strataindustria.power.HandCrankBlockEntity;
import dev.strataindustria.power.KineticNetworks;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.transport.outpost.Charter;
import dev.strataindustria.transport.outpost.LinkKind;
import dev.strataindustria.transport.outpost.OutpostPlan;
import dev.strataindustria.transport.outpost.RouteIndex;
import dev.strataindustria.transport.ropeway.RopewayBuilder;
import dev.strataindustria.transport.ropeway.RopewayPath;
import dev.strataindustria.transport.ropeway.RopewayRegistry;
import dev.strataindustria.transport.ropeway.RopewayReturnBlock;
import dev.strataindustria.transport.ropeway.RopewayTerminalBlock;
import dev.strataindustria.transport.ropeway.RopewayTerminalBlockEntity;
import dev.strataindustria.transport.ropeway.RopewayTowerBlockEntity;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;

/** The aerial ropeway (outposts and transport spec 8 and 15, chunk O8), run by {@link ModGameTests}. */
final class RopewayGameTests {
    private static final int ROW = RailGameTests.ROW;
    private static final int Y = RailGameTests.RAIL_Y;

    private RopewayGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("ropeway_registered", RopewayGameTests::registered);
        tests.put("ropeway_strings_a_line", RopewayGameTests::stringsALine);
        tests.put("ropeway_refuses_bad_spans", RopewayGameTests::refusesBadSpans);
        tests.put("ropeway_needs_rope", RopewayGameTests::needsRope);
        tests.put("ropeway_tower_needs_a_base", RopewayGameTests::towerNeedsBase);
        tests.put("ropeway_carries", RopewayGameTests::carries);
        tests.put("ropeway_stops_unpowered", RopewayGameTests::stopsUnpowered);
        tests.put("ropeway_backs_up", RopewayGameTests::backsUp);
        tests.put("ropeway_cut_drops_loads", RopewayGameTests::cutDropsLoads);
        tests.put("ropeway_proves_link", RopewayGameTests::provesLink);
        tests.put("ropeway_counts_delivery", RopewayGameTests::countsDelivery);
        tests.put("ropeway_path_geometry", RopewayGameTests::pathGeometry);
    }

    // ---------------------------------------------------------------- setup

    private static BlockPos abs(GameTestHelper helper, int x, int dy, int z) {
        return helper.absolutePos(new BlockPos(x, Y + dy, z));
    }

    private static FakePlayer player(GameTestHelper helper, int rope) {
        FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "linesman"));
        if (rope > 0) player.getInventory().add(new ItemStack(RopewayRegistry.WIRE_ROPE.get(), rope));
        return player;
    }

    private static int rope(FakePlayer player) {
        int total = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(RopewayRegistry.WIRE_ROPE.get())) total += stack.getCount();
        }
        return total;
    }

    private static void solid(ServerLevel level, BlockPos pos) {
        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
    }

    private static BlockPos terminal(GameTestHelper helper, int x) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = abs(helper, x, 0, ROW);
        solid(level, pos);
        level.setBlock(pos, RopewayRegistry.TERMINAL.get().defaultBlockState().setValue(RopewayTerminalBlock.FACING, Direction.EAST), Block.UPDATE_ALL);
        return pos;
    }

    private static BlockPos station(GameTestHelper helper, int x) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = abs(helper, x, 0, ROW);
        solid(level, pos);
        level.setBlock(pos, RopewayRegistry.RETURN.get().defaultBlockState().setValue(RopewayReturnBlock.FACING, Direction.WEST), Block.UPDATE_ALL);
        return pos;
    }

    private static BlockPos tower(GameTestHelper helper, int x, int dy, int z, boolean steel) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = abs(helper, x, dy, z);
        solid(level, pos);
        level.setBlock(pos, (steel ? RopewayRegistry.STEEL_TOWER : RopewayRegistry.WOODEN_TOWER).get().defaultBlockState(), Block.UPDATE_ALL);
        return pos;
    }

    private static BarrelBlockEntity barrel(GameTestHelper helper, BlockPos pos) {
        helper.getLevel().setBlock(pos, Blocks.BARREL.defaultBlockState(), Block.UPDATE_ALL);
        return (BarrelBlockEntity) helper.getLevel().getBlockEntity(pos);
    }

    /** A line from a terminal at x 1 to a return at x 7, six blocks, room for one bucket, with a barrel behind each. */
    private static final class Line {
        final ServerLevel level;
        final BlockPos terminal, far, crank;
        final RopewayTerminalBlockEntity drive;
        final BarrelBlockEntity source, sink;

        Line(GameTestHelper helper) {
            level = helper.getLevel();
            terminal = terminal(helper, 1);
            far = station(helper, 7);
            source = barrel(helper, terminal.west());
            sink = barrel(helper, far.east());
            crank = terminal.north();
            level.setBlock(crank, ModBlocks.HAND_CRANK.get().defaultBlockState().setValue(HandCrankBlock.FACING, Direction.SOUTH), Block.UPDATE_ALL);
            drive = (RopewayTerminalBlockEntity) level.getBlockEntity(terminal);
            FakePlayer linesman = player(helper, 4);
            RopewayBuilder.rope(linesman, level, terminal);
            RopewayBuilder.rope(linesman, level, far);
            helper.assertTrue(drive.hasLine(), "the line is strung");
        }

        /** Cranks the terminal's shaft for {@code ticks} ticks. */
        void power(GameTestHelper helper, int ticks) {
            FakePlayer hand = player(helper, 0);
            HandCrankBlockEntity handle = (HandCrankBlockEntity) level.getBlockEntity(crank);
            for (int t = 1; t <= ticks; t += 4) {
                helper.runAfterDelay(t, () -> {
                    handle.crank(hand);
                    KineticNetworks.rebuildNow(level, terminal);
                });
            }
        }

        int count(Container container) {
            int total = 0;
            for (int i = 0; i < container.getContainerSize(); i++) total += container.getItem(i).getCount();
            return total;
        }
    }

    // ---------------------------------------------------------------- the blocks

    // Every block and item of the ropeway exists, the terminal takes a shaft at its sides but not at its front or back.
    private static void registered(GameTestHelper helper) {
        helper.assertTrue(RopewayRegistry.WIRE_ROPE.get() != null && RopewayRegistry.BUCKET.get() != null, "wire rope and the bucket");
        BlockPos pos = terminal(helper, 1);
        var state = helper.getLevel().getBlockState(pos);
        var block = (RopewayTerminalBlock) state.getBlock();
        helper.assertTrue(block.connects(state, Direction.NORTH) && block.connects(state, Direction.SOUTH) && block.connects(state, Direction.DOWN),
                "the shaft comes in at the sides and underneath");
        helper.assertTrue(!block.connects(state, Direction.EAST) && !block.connects(state, Direction.WEST) && !block.connects(state, Direction.UP),
                "not at the line, the loading face or the wheel");
        helper.assertTrue(helper.getLevel().getBlockEntity(pos) instanceof RopewayTerminalBlockEntity, "the terminal has its block entity");
        helper.assertTrue(RopewayRegistry.WOODEN_TOWER.get().defaultBlockState().hasBlockEntity(), "towers carry the line's shape");
        helper.succeed();
    }

    // Wire rope on the terminal, a tower and the return strings a line, charging 1 rope per 8 blocks of each span.
    private static void stringsALine(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos from = terminal(helper, 1), mid = tower(helper, 4, 1, ROW + 3, false), to = station(helper, 7);
        FakePlayer linesman = player(helper, 10);
        helper.assertTrue(RopewayBuilder.rope(linesman, level, from) == InteractionResult.SUCCESS_SERVER, "the terminal takes the rope");
        helper.assertTrue(RopewayBuilder.stringing(linesman), "a line is being strung");
        RopewayBuilder.rope(linesman, level, mid);
        RopewayBuilder.rope(linesman, level, to);
        RopewayTerminalBlockEntity drive = (RopewayTerminalBlockEntity) level.getBlockEntity(from);
        helper.assertTrue(drive.hasLine() && drive.nodes().size() == 3, "terminal, tower, return");
        helper.assertTrue(!RopewayBuilder.stringing(linesman), "stringing is finished");
        helper.assertValueEqual(rope(linesman), 8, "two spans of under 8 blocks cost one rope each... plus the first");
        RopewayTowerBlockEntity head = (RopewayTowerBlockEntity) level.getBlockEntity(mid);
        helper.assertTrue(head.attached() && head.terminal().equals(from) && head.index() == 1 && to.equals(head.next()),
                "the tower knows its terminal, its place and where its span ends");
        helper.assertTrue(drive.capacity() >= 2, "room for buckets: " + drive.capacity());
        helper.succeed();
    }

    // A span steeper than one block in two, or with stone across it, is refused and the rest of the line carries on without it.
    private static void refusesBadSpans(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos from = terminal(helper, 1), to = station(helper, 7);
        BlockPos steep = tower(helper, 3, 4, ROW + 2, false);
        FakePlayer linesman = player(helper, 20);
        RopewayBuilder.rope(linesman, level, from);
        int before = rope(linesman);
        RopewayBuilder.rope(linesman, level, steep);
        helper.assertValueEqual(rope(linesman), before, "a refused span costs nothing");
        // Stone across the straight line to the return.
        level.setBlock(abs(helper, 4, 0, ROW), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(abs(helper, 4, 1, ROW), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        RopewayBuilder.rope(linesman, level, to);
        RopewayTerminalBlockEntity drive = (RopewayTerminalBlockEntity) level.getBlockEntity(from);
        helper.assertTrue(!drive.hasLine(), "stone in the way: no line");
        helper.assertTrue(RopewayBuilder.stringing(linesman), "still stringing, so the player can clear it and try again");
        level.setBlock(abs(helper, 4, 0, ROW), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(abs(helper, 4, 1, ROW), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        RopewayBuilder.rope(linesman, level, to);
        helper.assertTrue(drive.hasLine() && drive.nodes().size() == 2, "cleared, the line is made without the steep tower");
        helper.succeed();
    }

    // Without wire rope nothing is strung; the terminal that already has a line asks for a sneak before it is taken down.
    private static void needsRope(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos from = terminal(helper, 1), to = station(helper, 7);
        FakePlayer bare = player(helper, 0);
        RopewayBuilder.rope(bare, level, from);
        RopewayBuilder.rope(bare, level, to);
        RopewayTerminalBlockEntity drive = (RopewayTerminalBlockEntity) level.getBlockEntity(from);
        helper.assertTrue(!drive.hasLine(), "no rope, no line");
        FakePlayer linesman = player(helper, 4);
        RopewayBuilder.rope(linesman, level, from);
        RopewayBuilder.rope(linesman, level, to);
        helper.assertTrue(drive.hasLine(), "with rope the same two clicks make a line");
        RopewayBuilder.rope(linesman, level, from);
        helper.assertTrue(drive.hasLine(), "a plain click does not take the line down");
        linesman.setShiftKeyDown(true);
        RopewayBuilder.rope(linesman, level, from);
        helper.assertTrue(!drive.hasLine(), "sneaking with the rope takes it down");
        helper.succeed();
    }

    // A tower head stands on solid ground or a fence or log column, never on air.
    private static void towerNeedsBase(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos air = abs(helper, 3, 2, ROW);
        var wood = RopewayRegistry.WOODEN_TOWER.get().defaultBlockState();
        helper.assertTrue(!wood.canSurvive(level, air), "not on air");
        level.setBlock(air.below(), Blocks.OAK_FENCE.defaultBlockState(), Block.UPDATE_ALL);
        helper.assertTrue(wood.canSurvive(level, air), "on a fence post");
        level.setBlock(air.below(), Blocks.OAK_LOG.defaultBlockState(), Block.UPDATE_ALL);
        helper.assertTrue(wood.canSurvive(level, air), "on a log");
        level.setBlock(air.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        helper.assertTrue(wood.canSurvive(level, air), "on solid ground");
        helper.succeed();
    }

    // ---------------------------------------------------------------- the loop

    // A turning drive takes the load from the barrel behind the terminal across the line and tips it into the barrel at the return.
    private static void carries(GameTestHelper helper) {
        Line line = new Line(helper);
        line.source.setItem(0, new ItemStack(Items.COBBLESTONE, 32));
        line.source.setItem(1, new ItemStack(Items.COAL, 16));
        FakePlayer hand = player(helper, 0);
        helper.assertTrue(line.drive.bucketBy(hand, new ItemStack(RopewayRegistry.BUCKET.get(), 2)) == InteractionResult.SUCCESS_SERVER, "a bucket goes on the hook");
        helper.assertValueEqual(line.drive.spare(), 1, "one spare bucket");
        line.power(helper, 185);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(line.drive.speed() > 0.0, "the line is moving: " + line.drive.speed());
            helper.assertValueEqual(line.drive.onLine(), 1, "the bucket is out on the line");
            helper.assertValueEqual(line.count(line.sink), 0, "nothing has arrived yet");
        });
        helper.runAfterDelay(150, () -> {
            helper.assertValueEqual(line.count(line.sink), 32, "the first stack was tipped out at the return");
            helper.assertValueEqual(line.drive.deliveredItems(), 32, "and counted");
            helper.assertTrue(line.drive.buckets().isEmpty() || line.drive.buckets().get(0).stack.is(Items.COAL) || line.drive.buckets().get(0).stack.isEmpty(),
                    "the bucket is on its way home or loaded again");
            helper.succeed();
        });
    }

    // With nothing turning the drive, a loaded bucket does not leave the terminal.
    private static void stopsUnpowered(GameTestHelper helper) {
        Line line = new Line(helper);
        line.source.setItem(0, new ItemStack(Items.COBBLESTONE, 32));
        line.drive.addSpare(1);
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(line.drive.onLine() == 0 && line.drive.advance() == 0.0, "nothing moved");
            helper.assertValueEqual(line.count(line.sink), 0, "nothing delivered");
            helper.assertValueEqual(line.count(line.source), 32, "the load is still behind the terminal");
            helper.succeed();
        });
    }

    // A full barrel at the return stops the line with the bucket at the lip; emptying it lets the load tip out and the line go on.
    private static void backsUp(GameTestHelper helper) {
        Line line = new Line(helper);
        line.source.setItem(0, new ItemStack(Items.COBBLESTONE, 32));
        line.drive.addSpare(1);
        for (int i = 0; i < line.sink.getContainerSize(); i++) line.sink.setItem(i, new ItemStack(Items.STONE, 64));
        line.power(helper, 180);
        helper.runAfterDelay(120, () -> {
            helper.assertTrue(line.drive.stalled(), "the line is backed up");
            helper.assertTrue(line.drive.speed() == 0.0, "and standing still");
            helper.assertTrue(line.drive.buckets().size() == 1 && !line.drive.buckets().get(0).stack.isEmpty(), "the bucket still holds its load");
            for (int i = 0; i < line.sink.getContainerSize(); i++) line.sink.setItem(i, ItemStack.EMPTY);
        });
        helper.runAfterDelay(160, () -> {
            helper.assertTrue(!line.drive.stalled(), "emptied, the line runs again");
            helper.assertValueEqual(line.count(line.sink), 32, "the load tipped out");
            helper.succeed();
        });
    }

    // Breaking a tower under a loaded bucket drops the load at the terminal, takes the bucket back and cuts the line.
    private static void cutDropsLoads(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos from = terminal(helper, 1), mid = tower(helper, 4, 0, ROW, false), to = station(helper, 7);
        barrel(helper, from.west()).setItem(0, new ItemStack(Items.COBBLESTONE, 16));
        barrel(helper, to.east());
        FakePlayer linesman = player(helper, 6);
        RopewayBuilder.rope(linesman, level, from);
        RopewayBuilder.rope(linesman, level, mid);
        RopewayBuilder.rope(linesman, level, to);
        RopewayTerminalBlockEntity drive = (RopewayTerminalBlockEntity) level.getBlockEntity(from);
        helper.assertTrue(drive.hasLine(), "the line is strung");
        drive.addSpare(1);
        BlockPos crank = from.north();
        level.setBlock(crank, ModBlocks.HAND_CRANK.get().defaultBlockState().setValue(HandCrankBlock.FACING, Direction.SOUTH), Block.UPDATE_ALL);
        HandCrankBlockEntity handle = (HandCrankBlockEntity) level.getBlockEntity(crank);
        FakePlayer hand = player(helper, 0);
        for (int t = 1; t <= 50; t += 4) {
            helper.runAfterDelay(t, () -> {
                handle.crank(hand);
                KineticNetworks.rebuildNow(level, from);
            });
        }
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(drive.onLine() == 1, "a loaded bucket is on the line");
            level.destroyBlock(mid, false);
        });
        helper.runAfterDelay(44, () -> {
            helper.assertTrue(!drive.hasLine(), "the line is cut");
            helper.assertValueEqual(drive.spare(), 1, "the bucket is back with the terminal");
            helper.assertValueEqual(drive.onLine(), 0, "nothing hangs on the rope");
            int dropped = 0;
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(from).inflate(3))) {
                if (item.getItem().is(Items.COBBLESTONE)) dropped += item.getItem().getCount();
            }
            helper.assertValueEqual(dropped, 16, "its load lies at the terminal");
            helper.succeed();
        });
    }

    // A bucket that carries a load out and comes home between two charters proves a ropeway; breaking a tower cuts it.
    private static void provesLink(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RouteIndex index = RouteIndex.get(level);
        FakePlayer owner = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "stationmaster"));
        OutpostPlan.ALSO_ONLINE.add(owner.getUUID());
        Charter home = index.post(level, abs(helper, 0, 0, ROW), owner, "Home");
        Charter far = index.post(level, abs(helper, 8, 0, ROW), owner, "Far");
        Line line = new Line(helper);
        line.source.setItem(0, new ItemStack(Items.COBBLESTONE, 8));
        line.drive.addSpare(1);
        line.power(helper, 190);
        helper.runAfterDelay(185, () -> {
            try {
                var links = index.linksOf(home.id());
                helper.assertValueEqual(links.size(), 1, "one link from the home charter");
                helper.assertTrue(links.get(0).kind() == LinkKind.ROPEWAY && links.get(0).open(), "an open ropeway: " + links.get(0).kind());
                helper.assertTrue(links.get(0).joins(far.id()), "it joins the far charter");
                helper.assertValueEqual(index.bestTier(home.id()), 4, "tier four");
                level.destroyBlock(line.far, false);
                helper.assertTrue(!index.linksOf(home.id()).get(0).open(), "taking the return out cuts the link");
            } finally {
                index.clearOwner(level, owner.getUUID());
                OutpostPlan.ALSO_ONLINE.remove(owner.getUUID());
            }
            helper.succeed();
        });
    }

    // A full stack of 64 across the line is the journal's number.
    private static void countsDelivery(GameTestHelper helper) {
        Line line = new Line(helper);
        line.source.setItem(0, new ItemStack(Items.COBBLESTONE, 64));
        line.drive.addSpare(1);
        line.power(helper, 130);
        helper.runAfterDelay(120, () -> {
            helper.assertValueEqual(line.drive.deliveredItems(), 64, "sixty-four items delivered");
            helper.succeed();
        });
    }

    // The path through two spans is as long as the spans, buckets cross each rope, and the loop wraps.
    private static void pathGeometry(GameTestHelper helper) {
        RopewayPath path = RopewayPath.of(List.of(new BlockPos(0, 0, 0), new BlockPos(3, 0, 4), new BlockPos(3, 0, 12)));
        helper.assertTrue(Math.abs(path.length() - 13.0) < 1.0E-6, "5 + 8 blocks: " + path.length());
        helper.assertTrue(path.spans() == 2 && path.spanOf(2.0) == 0 && path.spanOf(9.0) == 1, "the spans");
        var out = path.point(2.0);
        var back = path.point(2.0 * path.length() - 2.0);
        helper.assertTrue(out.position().distanceTo(back.position()) > 0.5, "the two ropes hang apart");
        helper.assertTrue(out.heading().dot(back.heading()) < -0.99, "buckets on the return rope run the other way");
        helper.assertTrue(path.point(0.0).position().distanceTo(path.point(2.0 * path.length()).position()) < 1.0E-6, "the loop closes");
        helper.assertTrue(RopewayPath.crosses(25.9, 0.1, 0.0, 26.0), "a step across the wrap passes the terminal");
        helper.assertTrue(!RopewayPath.crosses(1.0, 1.1, 0.0, 26.0), "and a step in the middle does not");
        helper.succeed();
    }
}
