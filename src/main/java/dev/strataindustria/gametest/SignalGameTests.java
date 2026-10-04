package dev.strataindustria.gametest;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.power.ElectricTier;
import dev.strataindustria.transport.rail.ElectricTramEntity;
import dev.strataindustria.transport.rail.MineTubEntity;
import dev.strataindustria.transport.rail.RailRegistry;
import dev.strataindustria.transport.rail.RailwayRegistry;
import dev.strataindustria.transport.rail.StopData;
import dev.strataindustria.transport.rail.StopRule;
import dev.strataindustria.transport.rail.TubStopBlockEntity;
import dev.strataindustria.transport.signal.RouteSwitchBlock;
import dev.strataindustria.transport.signal.RouteSwitchBlockEntity;
import dev.strataindustria.transport.signal.SignalBlock;
import dev.strataindustria.transport.signal.SignalRegistry;
import dev.strataindustria.transport.signal.Signals;
import dev.strataindustria.transport.signal.Timetable;
import dev.strataindustria.transport.signal.TimetableStops;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

/** Block signals, timetables and the route switch (outposts and transport spec 9.4, chunk O12), run by {@link ModGameTests}. */
final class SignalGameTests {
    private static final int ROW = RailGameTests.ROW;
    private static final int Y = 0;

    private SignalGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("signal_registered", SignalGameTests::registered);
        tests.put("signal_holds", SignalGameTests::holds);
        tests.put("signal_clear_passes", SignalGameTests::clearPasses);
        tests.put("timetable_loads", SignalGameTests::loads);
        tests.put("timetable_saves", SignalGameTests::saves);
        tests.put("timetable_passes_other_stops", SignalGameTests::passesOthers);
        tests.put("timetable_loop", SignalGameTests::loop);
        tests.put("route_switch_timetable", SignalGameTests::switchBranch);
        tests.put("route_switch_straight", SignalGameTests::switchStraight);
        tests.put("route_switch_converge", SignalGameTests::switchConverge);
    }

    // ---------------------------------------------------------------- setup

    private static BlockPos cell(GameTestHelper helper, int x, int z) {
        return helper.absolutePos(new BlockPos(x, Y, z));
    }

    private static void wood(GameTestHelper helper, int from, int to) {
        for (int x = from; x <= to; x++) TramGameTests.lay(helper, x, RailRegistry.WOODEN_RAIL.get());
    }

    private static MineTubEntity tub(GameTestHelper helper, double x) {
        MineTubEntity tub = helper.spawn(RailRegistry.MINE_TUB_ENTITY.get(), new BlockPos((int) Math.floor(x), Y, ROW));
        tub.setPos(helper.absolutePos(new BlockPos(0, Y, ROW)).getX() + x, tub.getY(), tub.getZ());
        return tub;
    }

    /** A signal on the south side of the east-west track at {@code x}, looking east. */
    private static BlockPos signal(GameTestHelper helper, int x) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = cell(helper, x, ROW + 1);
        level.setBlock(pos.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(pos, SignalRegistry.BLOCK_SIGNAL.get().defaultBlockState().setValue(SignalBlock.FACING, Direction.EAST), Block.UPDATE_ALL);
        return pos;
    }

    private static boolean clear(GameTestHelper helper, BlockPos signal) {
        return helper.getLevel().getBlockState(signal).getValue(SignalBlock.CLEAR);
    }

    private static TubStopBlockEntity named(GameTestHelper helper, BlockPos pos, String name, StopRule rule, int seconds) {
        ServerLevel level = helper.getLevel();
        level.setBlock(pos.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(pos, RailwayRegistry.STATION_TRACK.get().defaultBlockState().setValue(RailwayRegistry.STATION_TRACK.get().getShapeProperty(), RailShape.EAST_WEST), Block.UPDATE_ALL);
        TubStopBlockEntity stop = (TubStopBlockEntity) level.getBlockEntity(pos);
        helper.assertTrue(stop != null, "a station track has its block entity");
        stop.apply(new StopData(pos, name, rule, seconds, true));
        return stop;
    }

    private static Timetable table(StopRule rule, String... names) {
        return Timetable.of(new TimetableStops(java.util.Arrays.stream(names)
                .map(name -> new TimetableStops.Entry(name, Optional.ofNullable(rule), 1)).toList()));
    }

    // ---------------------------------------------------------------- the signal

    // The signal finds the rail on its left, and the block after it ends at the next signal that guards the same way.
    private static void registered(GameTestHelper helper) {
        helper.assertTrue(SignalRegistry.BLOCK_SIGNAL_ITEM.get() != null && SignalRegistry.TIMETABLE.get() != null && SignalRegistry.ROUTE_SWITCH_ITEM.get() != null, "the items");
        wood(helper, 0, 8);
        BlockPos first = signal(helper, 2), second = signal(helper, 6);
        helper.assertValueEqual(Signals.railOf(helper.getLevel(), first, Direction.EAST), cell(helper, 2, ROW), "it watches the rail beside it");
        helper.assertValueEqual(Signals.signalFor(helper.getLevel(), cell(helper, 2, ROW), Direction.EAST), first, "and the rail knows its signal");
        helper.assertTrue(Signals.signalFor(helper.getLevel(), cell(helper, 2, ROW), Direction.WEST) == null, "going the other way it guards nothing");
        List<BlockPos> block = Signals.blockAhead(helper.getLevel(), cell(helper, 2, ROW), Direction.EAST);
        helper.assertValueEqual(block.size(), 4, "three rails and the next signal's rail");
        helper.assertValueEqual(block.get(block.size() - 1), cell(helper, 6, ROW), "the block ends at the next signal");
        helper.succeed();
    }

    // A tub in the block ahead keeps the arm up; the tub behind stops at the line and goes on when the block empties.
    private static void holds(GameTestHelper helper) {
        wood(helper, 0, 8);
        BlockPos signal = signal(helper, 3);
        MineTubEntity ahead = tub(helper, 6.5), behind = tub(helper, 1.5);
        helper.runAfterDelay(8, () -> {
            helper.assertTrue(!clear(helper, signal), "the block has a tub in it: the arm is up");
            behind.setDeltaMovement(0.2, 0, 0);
        });
        helper.runAfterDelay(60, () -> {
            double centre = cell(helper, 3, ROW).getX() + 0.5;
            helper.assertTrue(behind.getX() < centre && behind.getX() > centre - 1.2, "the tub waits before the signal: " + behind.getX());
            helper.assertTrue(behind.getDeltaMovement().horizontalDistance() < 0.02, "and is standing");
            helper.assertTrue(behind.atSignal(), "it knows it is waiting at a signal");
            ahead.discard();
        });
        helper.runAfterDelay(75, () -> {
            helper.assertTrue(clear(helper, signal), "the block is empty: the arm drops");
            behind.setDeltaMovement(0.2, 0, 0);
        });
        helper.runAfterDelay(130, () -> {
            helper.assertTrue(behind.getX() > cell(helper, 3, ROW).getX() + 1.5, "and the tub goes through: " + behind.getX());
            helper.succeed();
        });
    }

    // Nothing in the block: the arm is down and a tub rolls straight by.
    private static void clearPasses(GameTestHelper helper) {
        wood(helper, 0, 8);
        BlockPos signal = signal(helper, 3);
        MineTubEntity tub = tub(helper, 1.5);
        helper.runAfterDelay(8, () -> {
            helper.assertTrue(clear(helper, signal), "an empty block shows clear");
            tub.setDeltaMovement(0.2, 0, 0);
        });
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(tub.getX() > cell(helper, 3, ROW).getX() + 1.5, "the tub went by: " + tub.getX());
            helper.assertTrue(!tub.atSignal(), "and was never held");
            helper.succeed();
        });
    }

    // ---------------------------------------------------------------- the timetable

    // A timetable used on a tram loads it, a blank one clears it, a wagon behind refuses it and a plain tub has no mind for it.
    private static void loads(GameTestHelper helper) {
        TramGameTests.steel(helper, 0, 8);
        FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "dispatcher"));
        ItemStack timetable = new ItemStack(SignalRegistry.TIMETABLE.get());
        timetable.set(SignalRegistry.TIMETABLE_STOPS.get(), new TimetableStops(List.of(
                new TimetableStops.Entry("Ridge", Optional.empty(), 10), new TimetableStops.Entry("Saltwell", Optional.of(StopRule.FULL), 10))));
        player.setItemInHand(InteractionHand.MAIN_HAND, timetable);
        ElectricTramEntity tram = TramGameTests.tram(helper, 3.5);
        tram.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
        helper.assertTrue(tram.timetable() != null && tram.timetable().size() == 2, "the tram has the timetable");
        helper.assertValueEqual(tram.timetable().target().name(), "Ridge", "bound for the first stop");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(SignalRegistry.TIMETABLE.get()));
        tram.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
        helper.assertTrue(tram.timetable() == null, "a blank one clears it");

        player.setItemInHand(InteractionHand.MAIN_HAND, timetable);
        MineTubEntity plain = tub(helper, 1.5);
        plain.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
        helper.assertTrue(plain.timetable() == null, "a plain tub has no mind to follow one");
        helper.succeed();
    }

    // A timetable is cleaned, saved and read back as it was, and the vehicle keeps its place in it.
    private static void saves(GameTestHelper helper) {
        TimetableStops stops = new TimetableStops(List.of(
                new TimetableStops.Entry("  Ridge ", Optional.empty(), 10), new TimetableStops.Entry("", Optional.empty(), 10),
                new TimetableStops.Entry("Saltwell", Optional.of(StopRule.WAIT), 30))).cleaned();
        helper.assertValueEqual(stops.entries().size(), 2, "the blank line is dropped");
        helper.assertValueEqual(stops.entries().get(0).name(), "Ridge", "names are trimmed");
        var json = TimetableStops.CODEC.encodeStart(JsonOps.INSTANCE, stops).getOrThrow();
        helper.assertTrue(TimetableStops.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow().equals(stops), "the item data round-trips");

        Timetable run = Timetable.of(stops);
        run.arrive();
        var saved = Timetable.CODEC.encodeStart(JsonOps.INSTANCE, run).getOrThrow();
        Timetable back = Timetable.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow();
        helper.assertValueEqual(back.target().name(), "Saltwell", "the vehicle is still bound for the second stop");
        helper.assertTrue(back.wants("saltwell"), "names match without regard to case");
        helper.succeed();
    }

    /** Track with three named stations at x = 2, 4 and 6 and a live line over it. */
    private static TubStopBlockEntity[] threeStations(GameTestHelper helper, StopRule rule) {
        TramGameTests.steel(helper, 0, 8);
        TubStopBlockEntity a = named(helper, cell(helper, 2, ROW), "A", rule, 1);
        TubStopBlockEntity b = named(helper, cell(helper, 4, ROW), "B", rule, 1);
        TubStopBlockEntity c = named(helper, cell(helper, 6, ROW), "C", rule, 1);
        TramGameTests.line(helper, ElectricTier.LV, 200_000);
        return new TubStopBlockEntity[] {a, b, c};
    }

    // A tram bound for C goes by A and B without stopping, and stands at C.
    private static void passesOthers(GameTestHelper helper) {
        TubStopBlockEntity[] stops = threeStations(helper, StopRule.REDSTONE);
        ElectricTramEntity tram = TramGameTests.tram(helper, 1.5);
        tram.setTimetable(table(null, "C"));
        boolean[] held = {false, false};
        for (int t = 1; t < 190; t++) {
            helper.runAfterDelay(t, () -> {
                held[0] |= stops[0].isHolding();
                held[1] |= stops[1].isHolding();
            });
        }
        helper.runAfterDelay(190, () -> {
            helper.assertTrue(!held[0] && !held[1], "it never stopped at A or B");
            helper.assertTrue(stops[2].isHolding(), "and stands at C");
            helper.succeed();
        });
    }

    // Three stops in order: the tram stands at each in turn and the third arrival completes a round.
    private static void loop(GameTestHelper helper) {
        TubStopBlockEntity[] stops = threeStations(helper, StopRule.WAIT);
        ElectricTramEntity tram = TramGameTests.tram(helper, 1.5);
        tram.setTimetable(table(StopRule.WAIT, "A", "B", "C"));
        int[] order = {0, 0, 0};
        int[] seen = {0};
        for (int t = 1; t < 195; t++) {
            helper.runAfterDelay(t, () -> {
                for (int i = 0; i < 3; i++) {
                    if (stops[i].isHolding() && order[i] == 0) order[i] = ++seen[0];
                }
            });
        }
        helper.runAfterDelay(195, () -> {
            helper.assertTrue(order[0] == 1 && order[1] == 2 && order[2] == 3, "A, then B, then C: " + order[0] + order[1] + order[2]);
            helper.assertTrue(tram.timetable().rounds() >= 1, "a round is done");
            helper.assertValueEqual(tram.timetable().target().name(), "A", "and it is bound for A again");
            helper.succeed();
        });
    }

    // ---------------------------------------------------------------- the route switch

    /**
     * The trunk runs west to east along the row to x = 8; a switch at x = 3 looks east, so its trunk is the west side and
     * its branch leaves south over two rails to a station called Branch at z + 3. The far station at x = 7 is called Far.
     */
    private static BlockPos switchLayout(GameTestHelper helper, String... branchFor) {
        ServerLevel level = helper.getLevel();
        TramGameTests.steel(helper, 0, 2);
        TramGameTests.steel(helper, 4, 6);
        named(helper, cell(helper, 7, ROW), "Far", StopRule.REDSTONE, 1);
        BlockPos switchPos = cell(helper, 3, ROW);
        level.setBlock(switchPos.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(switchPos, SignalRegistry.ROUTE_SWITCH.get().defaultBlockState()
                .setValue(RouteSwitchBlock.FACING, Direction.EAST).setValue(RouteSwitchBlock.SHAPE, RailShape.EAST_WEST), Block.UPDATE_ALL);
        // The branch: two steel rails running north to south, then a station at the end of it.
        for (int step = 1; step <= 2; step++) {
            BlockPos rail = switchPos.south(step);
            level.setBlock(rail.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(rail, RailwayRegistry.STEEL_TRACK.get().defaultBlockState().setValue(RailwayRegistry.STEEL_TRACK.get().getShapeProperty(), RailShape.NORTH_SOUTH), Block.UPDATE_ALL);
        }
        BlockPos end = switchPos.south(3);
        level.setBlock(end.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(end, RailwayRegistry.STATION_TRACK.get().defaultBlockState().setValue(RailwayRegistry.STATION_TRACK.get().getShapeProperty(), RailShape.NORTH_SOUTH), Block.UPDATE_ALL);
        TubStopBlockEntity stop = (TubStopBlockEntity) level.getBlockEntity(end);
        stop.apply(new StopData(end, "Branch", StopRule.REDSTONE, 1, true));
        ((RouteSwitchBlockEntity) level.getBlockEntity(switchPos)).setStops(List.of(branchFor));
        TramGameTests.line(helper, ElectricTier.LV, 200_000);
        return switchPos;
    }

    // A tram whose next stop is on the switch's list is sent down the branch and stands at the station there.
    private static void switchBranch(GameTestHelper helper) {
        BlockPos switchPos = switchLayout(helper, "Branch");
        ElectricTramEntity tram = TramGameTests.tram(helper, 0.5);
        tram.setTimetable(table(StopRule.REDSTONE, "Branch"));
        for (int t = 1; t < 150; t++) helper.runAfterDelay(t, () -> tram.setCharge(ElectricTramEntity.RESERVE));
        helper.runAfterDelay(14, () -> helper.assertTrue(
                helper.getLevel().getBlockState(switchPos).getValue(RouteSwitchBlock.SHAPE) == RailShape.SOUTH_WEST,
                "the switch has thrown for the branch: " + helper.getLevel().getBlockState(switchPos).getValue(RouteSwitchBlock.SHAPE)));
        helper.runAfterDelay(150, () -> {
            BlockPos end = switchPos.south(3);
            helper.assertTrue(tram.isHeldAt(end), "the tram stands at the branch station: " + tram.blockPosition());
            helper.succeed();
        });
    }

    // A tram bound for a stop that is not on the list goes straight on to the far station.
    private static void switchStraight(GameTestHelper helper) {
        BlockPos switchPos = switchLayout(helper, "Branch");
        ElectricTramEntity tram = TramGameTests.tram(helper, 0.5);
        tram.setTimetable(table(StopRule.REDSTONE, "Far"));
        helper.runAfterDelay(14, () -> helper.assertTrue(
                helper.getLevel().getBlockState(switchPos).getValue(RouteSwitchBlock.SHAPE) == RailShape.EAST_WEST,
                "the switch stays straight: " + helper.getLevel().getBlockState(switchPos).getValue(RouteSwitchBlock.SHAPE)));
        helper.runAfterDelay(150, () -> {
            helper.assertTrue(tram.isHeldAt(cell(helper, 7, ROW)), "the tram stands at Far: " + tram.blockPosition());
            helper.succeed();
        });
    }

    // Coming in off the branch the switch sets itself for the trunk, whatever the list says.
    private static void switchConverge(GameTestHelper helper) {
        BlockPos switchPos = switchLayout(helper, "Branch");
        ServerLevel level = helper.getLevel();
        MineTubEntity tub = helper.spawn(RailRegistry.MINE_TUB_ENTITY.get(), new BlockPos(3, Y, ROW + 2));
        tub.setPos(switchPos.getX() + 0.5, tub.getY(), switchPos.getZ() + 2.5);
        tub.setDeltaMovement(0, 0, -0.2);
        helper.runAfterDelay(6, () -> helper.assertTrue(level.getBlockState(switchPos).getValue(RouteSwitchBlock.SHAPE) == RailShape.SOUTH_WEST,
                "set for the trunk: " + level.getBlockState(switchPos).getValue(RouteSwitchBlock.SHAPE)));
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(tub.getX() < switchPos.getX() - 0.5, "the tub came out west of the switch: " + tub.getX());
            helper.succeed();
        });
    }
}
