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
import dev.strataindustria.transport.ropeway.RopewayAngleBlock;
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
import net.minecraft.world.phys.Vec3;
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
        tests.put("ropeway_angle_turns", RopewayGameTests::angleTurns);
        tests.put("ropeway_turn_limits", RopewayGameTests::turnLimits);
        tests.put("ropeway_angle_tops_up", RopewayGameTests::anglePassesAndTopsUp);
        tests.put("ropeway_rides_out", RopewayGameTests::ridesOut);
        tests.put("ropeway_rides_home", RopewayGameTests::ridesHome);
        tests.put("ropeway_rider_stays_on", RopewayGameTests::riderStaysOn);
        tests.put("ropeway_cut_drops_rider", RopewayGameTests::cutDropsRider);
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

    private static BlockPos angle(GameTestHelper helper, int x, int z, Direction facing) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = abs(helper, x, 0, z);
        solid(level, pos);
        level.setBlock(pos, RopewayRegistry.ANGLE_STATION.get().defaultBlockState().setValue(RopewayAngleBlock.FACING, facing), Block.UPDATE_ALL);
        return pos;
    }

    /**
     * A line from a terminal at x 1 to a return at x 7, six blocks, room for one bucket, with a barrel behind each. The
     * bent kind runs east four blocks to an angle station and south three to the return, with a chest beside the angle.
     */
    private static final class Line {
        final ServerLevel level;
        final BlockPos terminal, far, crank;
        final @org.jspecify.annotations.Nullable BlockPos angle;
        final RopewayTerminalBlockEntity drive;
        final BarrelBlockEntity source, sink, chest;

        Line(GameTestHelper helper) {
            this(helper, false);
        }

        Line(GameTestHelper helper, boolean bent) {
            level = helper.getLevel();
            terminal = terminal(helper, 1);
            FakePlayer linesman = player(helper, 6);
            if (bent) {
                angle = angle(helper, 5, ROW, Direction.WEST);
                far = abs(helper, 5, 0, ROW + 3);
                solid(level, far);
                level.setBlock(far, RopewayRegistry.RETURN.get().defaultBlockState().setValue(RopewayReturnBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);
                sink = barrel(helper, far.south());
                chest = barrel(helper, angle.east());
            } else {
                angle = null;
                far = station(helper, 7);
                sink = barrel(helper, far.east());
                chest = null;
            }
            source = barrel(helper, terminal.west());
            crank = terminal.north();
            level.setBlock(crank, ModBlocks.HAND_CRANK.get().defaultBlockState().setValue(HandCrankBlock.FACING, Direction.SOUTH), Block.UPDATE_ALL);
            drive = (RopewayTerminalBlockEntity) level.getBlockEntity(terminal);
            RopewayBuilder.rope(linesman, level, terminal);
            if (angle != null) RopewayBuilder.rope(linesman, level, angle);
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
        BlockPos from = terminal(helper, 1), mid = tower(helper, 4, 1, ROW, false), to = station(helper, 7);
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
        helper.assertTrue(drive.capacity() >= 1, "room for buckets: " + drive.capacity());
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

    // ---------------------------------------------------------------- the angle station

    // An angle station turns the line a right angle, its tower head knows its place, and the two ropes stay joined round the bend.
    private static void angleTurns(GameTestHelper helper) {
        Line line = new Line(helper, true);
        helper.assertValueEqual(line.drive.nodes().size(), 3, "terminal, angle station, return");
        RopewayTowerBlockEntity head = (RopewayTowerBlockEntity) line.level.getBlockEntity(line.angle);
        helper.assertTrue(head.attached() && head.index() == 1 && line.far.equals(head.next()), "the angle station carries the line on to the return");
        RopewayPath path = line.drive.path();
        for (int side : new int[] {1, -1}) {
            helper.assertTrue(path.rope(0, 1.0, side).distanceTo(path.rope(1, 0.0, side)) < 1.0E-6, "the rope on side " + side + " is whole round the bend");
        }
        helper.assertTrue(path.rope(0, 1.0, 1).distanceTo(path.rope(0, 1.0, -1)) > 0.5, "and the two ropes hang apart at it");
        var before = path.point(path.at(1) - 0.1);
        var after = path.point(path.at(1) + 0.1);
        helper.assertTrue(before.heading().dot(after.heading()) > 0.5, "a bucket turns gently, not all at once");
        helper.succeed();
    }

    // A tower head turns the line only a little; an angle station up to a right angle and no further.
    private static void turnLimits(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos from = terminal(helper, 1);
        BlockPos mid = tower(helper, 4, 0, ROW, false);
        BlockPos bent = abs(helper, 6, 0, ROW + 3);
        solid(level, bent);
        level.setBlock(bent, RopewayRegistry.RETURN.get().defaultBlockState().setValue(RopewayReturnBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);
        FakePlayer linesman = player(helper, 20);
        RopewayTerminalBlockEntity drive = (RopewayTerminalBlockEntity) level.getBlockEntity(from);
        RopewayBuilder.rope(linesman, level, from);
        RopewayBuilder.rope(linesman, level, mid);
        int before = rope(linesman);
        RopewayBuilder.rope(linesman, level, bent);
        helper.assertTrue(!drive.hasLine() && RopewayBuilder.stringing(linesman), "a tower will not take a bend of 56 degrees");
        helper.assertValueEqual(rope(linesman), before, "and it costs nothing");
        helper.assertTrue(RopewayBuilder.turn(from, mid, bent) > RopewayBuilder.TOWER_TURN, "the turn is over the limit");
        // The same bend at an angle station is allowed; a turn back on itself is not.
        level.setBlock(mid, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        BlockPos hub = angle(helper, 4, ROW, Direction.WEST);
        FakePlayer second = player(helper, 20);
        RopewayBuilder.rope(second, level, from);
        RopewayBuilder.rope(second, level, hub);
        BlockPos back = abs(helper, 2, 0, ROW + 2);
        solid(level, back);
        level.setBlock(back, RopewayRegistry.RETURN.get().defaultBlockState().setValue(RopewayReturnBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);
        RopewayBuilder.rope(second, level, back);
        helper.assertTrue(!drive.hasLine(), "an angle station will not turn the line back on itself");
        RopewayBuilder.rope(second, level, bent);
        helper.assertTrue(drive.hasLine() && drive.nodes().size() == 3, "but the same bend is fine");
        helper.succeed();
    }

    // A bucket carries its load round the angle station, takes more of the same from the chest beside it, and tips the lot at the return.
    private static void anglePassesAndTopsUp(GameTestHelper helper) {
        Line line = new Line(helper, true);
        line.source.setItem(0, new ItemStack(Items.COBBLESTONE, 20));
        line.chest.setItem(0, new ItemStack(Items.COBBLESTONE, 64));
        line.chest.setItem(1, new ItemStack(Items.COAL, 10));
        line.drive.addSpare(1);
        line.power(helper, 170);
        helper.runAfterDelay(150, () -> {
            helper.assertValueEqual(line.count(line.sink), 64, "twenty from the terminal and forty-four more from the angle station's chest");
            helper.assertValueEqual(line.chest.getItem(0).getCount(), 20, "the chest gave what the bucket had room for");
            helper.assertValueEqual(line.chest.getItem(1).getCount(), 10, "and kept what was not the same");
            helper.succeed();
        });
    }

    // ---------------------------------------------------------------- riding

    /** A real player on a mock connection: a fake one cannot ride. */
    @SuppressWarnings("removal")
    private static net.minecraft.server.level.ServerPlayer rider(GameTestHelper helper, BlockPos at) {
        net.minecraft.server.level.ServerPlayer rider = helper.makeMockServerPlayerInLevel();
        rider.teleportTo(at.getX() + 0.5, at.getY() + 1.0, at.getZ() + 0.5);
        return rider;
    }

    // An empty hand on the terminal hangs a spare bucket for the rider; the seat goes out along the rope and the rider steps off at the next station.
    private static void ridesOut(GameTestHelper helper) {
        Line line = new Line(helper, true);
        line.drive.addSpare(1);
        var rider = rider(helper, line.terminal);
        line.power(helper, 150);
        helper.runAfterDelay(10, () -> helper.assertTrue(line.drive.ride(rider, line.terminal) == InteractionResult.SUCCESS_SERVER, "the terminal answers"));
        helper.runAfterDelay(14, () -> helper.assertTrue(line.drive.powered() && line.drive.speed() > 0 && line.drive.waiting() + line.drive.onLine() > 0,
                "asked for a seat: powered " + line.drive.powered() + " speed " + line.drive.speed() + " waiting " + line.drive.waiting() + " out " + line.drive.onLine() + " spare " + line.drive.spare()));
        double[] seen = new double[1];
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(rider.getVehicle() instanceof dev.strataindustria.transport.ropeway.RopewaySeatEntity, "the rider sits on a seat: waiting " + line.drive.waiting()
                    + " out " + line.drive.onLine() + " spare " + line.drive.spare() + " speed " + line.drive.speed() + " vehicle " + rider.getVehicle()
                    + " bucket " + (line.drive.buckets().isEmpty() ? null : line.drive.buckets().get(0).rider));
            seen[0] = rider.getVehicle().getX();
        });
        helper.runAfterDelay(35, () -> {
            helper.assertTrue(rider.getVehicle() != null && rider.getVehicle().getX() > seen[0] + 0.3, "and it moves along the line");
            helper.assertTrue(rider.getVehicle().getY() < line.terminal.getY() + 1.0 && rider.getVehicle().getY() > line.terminal.getY() - 2.5, "hanging below the rope");
        });
        helper.runAfterDelay(110, () -> {
            helper.assertTrue(rider.getVehicle() == null, "the rider is off");
            helper.assertTrue(rider.position().distanceTo(Vec3.atBottomCenterOf(line.angle.above())) < 1.5, "at the angle station, the first stop: " + rider.position());
            helper.assertTrue(line.level.getEntitiesOfClass(dev.strataindustria.transport.ropeway.RopewaySeatEntity.class, new AABB(line.terminal).inflate(12)).isEmpty(), "and the seat is gone");
            helper.succeed();
        });
    }

    // A rider at the return takes the bucket that has just tipped its load there, and is carried home.
    private static void ridesHome(GameTestHelper helper) {
        Line line = new Line(helper);
        line.source.setItem(0, new ItemStack(Items.COBBLESTONE, 8));
        line.drive.addSpare(2);
        var rider = rider(helper, line.far);
        line.power(helper, 200);
        helper.runAfterDelay(10, () -> helper.assertTrue(line.drive.ride(rider, line.far) == InteractionResult.SUCCESS_SERVER, "the return answers"));
        helper.runAfterDelay(110, () -> helper.assertTrue(rider.getVehicle() instanceof dev.strataindustria.transport.ropeway.RopewaySeatEntity, "taken up as the empty bucket swings round"));
        helper.runAfterDelay(190, () -> {
            helper.assertTrue(rider.getVehicle() == null, "home and off");
            helper.assertTrue(rider.position().distanceTo(Vec3.atBottomCenterOf(line.terminal.above())) < 1.5, "on the terminal: " + rider.position());
            helper.assertValueEqual(line.count(line.sink), 8, "the load went on without them");
            helper.succeed();
        });
    }

    // Nobody is shaken off a moving line by a sneak; once the line stands still they can climb out, and they come down slowly.
    private static void riderStaysOn(GameTestHelper helper) {
        Line line = new Line(helper);
        line.drive.addSpare(1);
        var rider = rider(helper, line.terminal);
        line.power(helper, 40);
        helper.runAfterDelay(10, () -> line.drive.ride(rider, line.terminal));
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(rider.getVehicle() != null, "aboard");
            rider.stopRiding();
            helper.assertTrue(rider.getVehicle() != null, "a moving line keeps them on");
        });
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(line.drive.speed() == 0.0 && rider.getVehicle() != null, "the line has stopped between stations");
            rider.stopRiding();
            helper.assertTrue(rider.getVehicle() == null, "so they can climb out");
            helper.assertTrue(rider.hasEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING), "and come down slowly");
            helper.succeed();
        });
    }

    // Breaking the line under a rider takes the seat away; they come down slowly and the bucket goes home to the terminal.
    private static void cutDropsRider(GameTestHelper helper) {
        Line line = new Line(helper);
        line.drive.addSpare(1);
        var rider = rider(helper, line.terminal);
        line.power(helper, 60);
        helper.runAfterDelay(10, () -> line.drive.ride(rider, line.terminal));
        helper.runAfterDelay(35, () -> {
            helper.assertTrue(rider.getVehicle() != null, "aboard");
            line.level.destroyBlock(line.far, false);
        });
        helper.runAfterDelay(42, () -> {
            helper.assertTrue(rider.getVehicle() == null, "the seat is gone");
            helper.assertTrue(rider.hasEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING), "the rider floats down");
            helper.assertValueEqual(line.drive.spare(), 1, "and the bucket is back with the terminal");
            helper.succeed();
        });
    }
}
