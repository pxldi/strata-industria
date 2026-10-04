package dev.strataindustria.gametest;

import com.mojang.authlib.GameProfile;
import dev.strataindustria.transport.outpost.Charter;
import dev.strataindustria.transport.outpost.OutpostPlan;
import dev.strataindustria.transport.outpost.RouteIndex;
import dev.strataindustria.transport.rail.MineTubEntity;
import dev.strataindustria.transport.rail.RailBufferBlock;
import dev.strataindustria.transport.rail.RailRegistry;
import dev.strataindustria.transport.rail.StopData;
import dev.strataindustria.transport.rail.StopRule;
import dev.strataindustria.transport.rail.TubStopBlockEntity;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

/** The wooden tramway (outposts and transport spec 5 and 15, chunk O3), run by {@link ModGameTests}. */
final class RailGameTests {
    static final int ROW = 4;
    static final int RAIL_Y = 2;

    private RailGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("rail_blocks", RailGameTests::blocks);
        tests.put("rail_coupling", RailGameTests::coupling);
        tests.put("rail_consist_follows", RailGameTests::consistFollows);
        tests.put("rail_buffer", RailGameTests::buffer);
        tests.put("rail_tipple", RailGameTests::tipple);
        tests.put("rail_tipple_needs_bin", RailGameTests::tippleNeedsBin);
        tests.put("rail_stop_wait", helper -> waitStop(helper, false));
        tests.put("rail_stop_reverse", helper -> waitStop(helper, true));
        tests.put("rail_stop_redstone", RailGameTests::redstoneStop);
        tests.put("rail_stop_full", RailGameTests::fullStop);
        tests.put("rail_stop_empty", RailGameTests::emptyStop);
        tests.put("rail_stop_idle", RailGameTests::idleStop);
        tests.put("rail_tub_proves_route", RailGameTests::provesRoute);
        tests.put("rail_vanilla_not_proven", RailGameTests::vanillaNotProven);
        tests.put("rail_route_cut", RailGameTests::routeCut);
    }

    // ---------------------------------------------------------------- setup

    static BlockPos at(GameTestHelper helper, int x) {
        return helper.absolutePos(new BlockPos(x, RAIL_Y, ROW));
    }

    @SuppressWarnings("unchecked")
    static void lay(GameTestHelper helper, int x, Block block) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = at(helper, x);
        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        BlockState state = block.defaultBlockState();
        if (block instanceof BaseRailBlock rail) state = state.setValue(rail.getShapeProperty(), RailShape.EAST_WEST);
        level.setBlock(pos, state, Block.UPDATE_ALL);
    }

    static void layRun(GameTestHelper helper, int from, int to) {
        for (int x = from; x <= to; x++) lay(helper, x, RailRegistry.WOODEN_RAIL.get());
    }

    static MineTubEntity tubAt(GameTestHelper helper, double x) {
        MineTubEntity tub = helper.spawn(RailRegistry.MINE_TUB_ENTITY.get(), new BlockPos((int) Math.floor(x), RAIL_Y, ROW));
        tub.setPos(helper.absolutePos(new BlockPos(0, RAIL_Y, ROW)).getX() + x, tub.getY(), tub.getZ());
        return tub;
    }

    private static double flat(MineTubEntity tub) {
        return tub.getDeltaMovement().horizontalDistance();
    }

    static TubStopBlockEntity stopAt(GameTestHelper helper, int x, StopRule rule, int seconds, boolean reverse) {
        lay(helper, x, RailRegistry.TUB_STOP.get());
        BlockPos pos = at(helper, x);
        TubStopBlockEntity stop = (TubStopBlockEntity) helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(stop != null, "a stop has its block entity");
        stop.apply(new StopData(pos, "", rule, seconds, reverse));
        return stop;
    }

    /** What a pusher does: keeps a free tub moving east until it stands on {@code stopX}. */
    private static void push(GameTestHelper helper, MineTubEntity tub, int stopX, int ticks) {
        double edge = at(helper, stopX).getX();
        for (int t = 1; t <= ticks; t++) {
            helper.runAfterDelay(t, () -> {
                if (!tub.isRemoved() && tub.getX() < edge && !tub.isHeldAt(at(helper, stopX))) tub.setDeltaMovement(0.12, 0, 0);
            });
        }
    }

    private static void items(MineTubEntity tub, int stacks, boolean full) {
        for (int slot = 0; slot < stacks; slot++) tub.setItem(slot, new ItemStack(Items.COBBLESTONE, full ? 64 : 4));
    }

    // ---------------------------------------------------------------- blocks

    // Every tramway block is a rail and part of the track a route may be proven over.
    private static void blocks(GameTestHelper helper) {
        for (var block : java.util.List.of(RailRegistry.WOODEN_RAIL, RailRegistry.TUB_STOP, RailRegistry.TIPPLE_RAIL, RailRegistry.RAIL_BUFFER)) {
            BlockState state = block.get().defaultBlockState();
            helper.assertTrue(state.is(BlockTags.RAILS), block.getId() + " is a rail");
            helper.assertTrue(state.is(RailRegistry.TRACK), block.getId() + " is track");
        }
        helper.assertTrue(!Blocks.RAIL.defaultBlockState().is(RailRegistry.TRACK), "a vanilla rail is not track");
        helper.succeed();
    }

    // ---------------------------------------------------------------- consists

    // A tub couples behind the nearest free tub; a consist is as long as the config allows and no longer.
    private static void coupling(GameTestHelper helper) {
        layRun(helper, 0, 14);
        int allowed = dev.strataindustria.Config.TRANSPORT_MAX_CONSIST_T3.getAsInt();
        MineTubEntity head = tubAt(helper, 2.5);
        MineTubEntity last = head;
        for (int i = 1; i <= allowed; i++) {
            MineTubEntity next = tubAt(helper, 2.5 + 1.25 * i);
            helper.assertTrue(next.couple() == MineTubEntity.Coupling.OK, "tub " + i + " couples");
            helper.assertTrue(next.leader() == last && last.follower() == next, "and is linked behind the one before");
            last = next;
        }
        helper.assertValueEqual(head.consist().size(), allowed + 1, "the whole chain");
        helper.assertTrue(last.head() == head, "the last one finds the head");
        MineTubEntity extra = tubAt(helper, 2.5 + 1.25 * (allowed + 1));
        helper.assertTrue(extra.couple() == MineTubEntity.Coupling.TOO_LONG, "one more is too many");
        helper.assertTrue(last.couple() == MineTubEntity.Coupling.FOLLOWING, "a tub that follows cannot take another lead");
        // Letting go in the middle splits the chain; removing a tub closes it up.
        MineTubEntity second = head.follower();
        second.decouple();
        helper.assertTrue(head.follower() == null && second.leader() == null, "uncoupled");
        helper.assertValueEqual(head.consist().size(), 1, "the head is alone");
        helper.assertTrue(second.couple() == MineTubEntity.Coupling.OK, "and can couple again");
        MineTubEntity third = second.follower();
        second.discard();
        helper.assertTrue(third == null || third.leader() != second, "a removed tub leaves no dangling link");
        helper.succeed();
    }

    // Shoving the lead drags the followers along, in order, and keeps them about a tub's length apart.
    private static void consistFollows(GameTestHelper helper) {
        layRun(helper, 0, 14);
        MineTubEntity a = tubAt(helper, 6.5), b = tubAt(helper, 5.25), c = tubAt(helper, 4.0);
        helper.assertTrue(b.couple() == MineTubEntity.Coupling.OK && c.couple() == MineTubEntity.Coupling.OK, "three tubs coupled");
        helper.assertTrue(a.consist().size() == 3 && c.head() == a, "one consist");
        double startC = c.getX();
        for (int t = 1; t <= 25; t++) helper.runAfterDelay(t, () -> a.setDeltaMovement(0.15, 0, 0));
        helper.runAfterDelay(45, () -> {
            helper.assertTrue(c.getX() > startC + 1.5, "the last tub was pulled, moved " + (c.getX() - startC));
            helper.assertTrue(a.getX() > b.getX() && b.getX() > c.getX(), "and they are still in order");
            double gap = a.getX() - b.getX();
            helper.assertTrue(gap > 0.8 && gap < 1.8, "about a tub apart, " + gap);
            helper.assertTrue(a.consist().size() == 3, "none of them let go");
            helper.succeed();
        });
    }

    // ---------------------------------------------------------------- buffer, tipple

    // A tub runs into a buffer and stops against the timber, short of the end of its block.
    private static void buffer(GameTestHelper helper) {
        layRun(helper, 0, 6);
        lay(helper, 7, RailRegistry.RAIL_BUFFER.get());
        BlockPos pos = at(helper, 7);
        helper.getLevel().setBlock(pos, helper.getLevel().getBlockState(pos).setValue(RailBufferBlock.FACING, Direction.EAST), Block.UPDATE_ALL);
        MineTubEntity tub = tubAt(helper, 4.5);
        tub.setDeltaMovement(0.2, 0, 0);
        helper.runAfterDelay(70, () -> {
            helper.assertTrue(flat(tub) < 0.01, "the tub is at rest, speed " + flat(tub));
            helper.assertTrue(tub.getX() > pos.getX() + 0.2, "it reached the buffer block, x " + tub.getX());
            helper.assertTrue(tub.getX() < pos.getX() + 0.6, "and the timber held it, x " + tub.getX());
            helper.succeed();
        });
    }

    // A loaded tub standing on a tipple tips its load into the chest below.
    private static void tipple(GameTestHelper helper) {
        layRun(helper, 0, 2);
        lay(helper, 3, RailRegistry.TIPPLE_RAIL.get());
        BlockPos chest = at(helper, 3).below();
        helper.getLevel().setBlock(chest, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
        MineTubEntity tub = tubAt(helper, 3.5);
        tub.setItem(0, new ItemStack(Items.COBBLESTONE, 32));
        tub.setItem(1, new ItemStack(Items.IRON_INGOT, 5));
        helper.succeedWhen(() -> {
            Container bin = (ChestBlockEntity) helper.getLevel().getBlockEntity(chest);
            int moved = 0;
            for (int slot = 0; slot < bin.getContainerSize(); slot++) moved += bin.getItem(slot).getCount();
            helper.assertValueEqual(moved, 37, "everything arrived in the chest");
            helper.assertTrue(tub.isEmpty(), "and the tub is empty");
            helper.assertTrue(tub.tipAmount() > 0.5f, "it is tipped over");
        });
    }

    // With nothing to catch the load, a tipple keeps it.
    private static void tippleNeedsBin(GameTestHelper helper) {
        layRun(helper, 0, 2);
        lay(helper, 3, RailRegistry.TIPPLE_RAIL.get());
        MineTubEntity tub = tubAt(helper, 3.5);
        tub.setItem(0, new ItemStack(Items.COBBLESTONE, 32));
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(!tub.isEmpty(), "the load stays in the tub");
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    tub.getBoundingBox().inflate(6)).isEmpty(), "and none of it spilled");
            helper.succeed();
        });
    }

    // ---------------------------------------------------------------- stops

    private static final int STOP_X = 6;

    private static double centre(GameTestHelper helper) {
        return at(helper, STOP_X).getX() + 0.5;
    }

    private static MineTubEntity stopLine(GameTestHelper helper, StopRule rule, int seconds, boolean reverse, TubStopBlockEntity[] out) {
        layRun(helper, 0, 12);
        out[0] = stopAt(helper, STOP_X, rule, seconds, reverse);
        MineTubEntity tub = tubAt(helper, 2.5);
        push(helper, tub, STOP_X, 60);
        return tub;
    }

    // The wait rule holds a tub for its seconds and lets it go on the way it came, or back with "reverse here".
    private static void waitStop(GameTestHelper helper, boolean reverse) {
        TubStopBlockEntity[] stop = new TubStopBlockEntity[1];
        MineTubEntity tub = stopLine(helper, StopRule.WAIT, 1, reverse, stop);
        int[] held = {-1, -1};
        for (int t = 1; t <= 110; t++) {
            int tick = t;
            helper.runAfterDelay(t, () -> {
                if (stop[0].isHolding() && held[0] < 0) held[0] = tick;
                if (held[0] >= 0 && held[1] < 0 && !stop[0].isHolding()) held[1] = tick;
            });
        }
        helper.runAfterDelay(111, () -> {
            helper.assertTrue(held[0] > 0, "the stop caught the tub");
            int waited = held[1] - held[0];
            helper.assertTrue(waited >= 18 && waited <= 24, "one second is about 20 ticks, waited " + waited);
            if (reverse) helper.assertTrue(tub.getX() < centre(helper) - 0.05, "sent back the way it came, x " + tub.getX());
            else helper.assertTrue(tub.getX() > centre(helper) + 0.1, "sent on, x " + tub.getX());
            helper.succeed();
        });
    }

    // The redstone rule holds until a signal arrives at the stop.
    private static void redstoneStop(GameTestHelper helper) {
        TubStopBlockEntity[] stop = new TubStopBlockEntity[1];
        MineTubEntity tub = stopLine(helper, StopRule.REDSTONE, 1, false, stop);
        helper.runAfterDelay(70, () -> helper.assertTrue(stop[0].isHolding() && tub.isHeldAt(at(helper, STOP_X)), "still held with no signal"));
        helper.runAfterDelay(71, () -> helper.getLevel().setBlock(at(helper, STOP_X).north(), Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL));
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(!stop[0].isHolding(), "let go by the signal");
            helper.assertTrue(tub.getX() > centre(helper) + 0.1, "and sent on, x " + tub.getX());
            helper.succeed();
        });
    }

    // The full rule holds until every tub in the consist has no room left.
    private static void fullStop(GameTestHelper helper) {
        TubStopBlockEntity[] stop = new TubStopBlockEntity[1];
        MineTubEntity tub = stopLine(helper, StopRule.FULL, 1, false, stop);
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(stop[0].isHolding(), "an empty tub is not full");
            items(tub, MineTubEntity.SLOTS, true);
        });
        helper.runAfterDelay(90, () -> {
            helper.assertTrue(!stop[0].isHolding(), "a full tub is let go");
            helper.succeed();
        });
    }

    // The empty rule holds until the load has been taken out.
    private static void emptyStop(GameTestHelper helper) {
        TubStopBlockEntity[] stop = new TubStopBlockEntity[1];
        MineTubEntity tub = stopLine(helper, StopRule.EMPTY, 1, false, stop);
        items(tub, 3, false);
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(stop[0].isHolding(), "a loaded tub is held");
            tub.clearContent();
        });
        helper.runAfterDelay(90, () -> {
            helper.assertTrue(!stop[0].isHolding(), "an empty tub is let go");
            helper.succeed();
        });
    }

    // The idle rule waits for the loading to stop: any change starts the seconds again.
    private static void idleStop(GameTestHelper helper) {
        TubStopBlockEntity[] stop = new TubStopBlockEntity[1];
        MineTubEntity tub = stopLine(helper, StopRule.IDLE, 1, false, stop);
        int[] released = {-1};
        helper.runAfterDelay(40, () -> tub.setItem(0, new ItemStack(Items.COBBLESTONE, 8)));
        for (int t = 41; t <= 110; t++) {
            int tick = t;
            helper.runAfterDelay(t, () -> {
                if (released[0] < 0 && !stop[0].isHolding()) released[0] = tick;
            });
        }
        helper.runAfterDelay(111, () -> {
            helper.assertTrue(released[0] >= 58 && released[0] <= 72, "released a second after the last change, at tick " + released[0]);
            helper.succeed();
        });
    }

    // ---------------------------------------------------------------- routes

    static final class Plan {
        final ServerLevel level;
        final RouteIndex index;
        final FakePlayer owner;
        final Charter home, mine;

        Plan(GameTestHelper helper) {
            level = helper.getLevel();
            index = RouteIndex.get(level);
            owner = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "tubman"));
            OutpostPlan.ALSO_ONLINE.add(owner.getUUID());
            BlockPos base = helper.absolutePos(new BlockPos(0, RAIL_Y, ROW));
            home = index.post(level, base, owner, "Home");
            mine = index.post(level, base.east(8), owner, "Mine");
        }

        void close() {
            index.clearOwner(level, owner.getUUID());
            OutpostPlan.ALSO_ONLINE.remove(owner.getUUID());
        }
    }

    /** Stops one block from each charter, a run of rail between them, and a tub that gets across on its own steam. */
    private static MineTubEntity tramway(GameTestHelper helper, TubStopBlockEntity[] stops) {
        layRun(helper, 2, 6);
        stops[0] = stopAt(helper, 1, StopRule.WAIT, 1, false);
        stops[1] = stopAt(helper, 7, StopRule.REDSTONE, 1, false);
        MineTubEntity tub = tubAt(helper, 1.5);
        tub.setDeltaMovement(0.05, 0, 0);
        double edge = at(helper, 7).getX();
        for (int t = 25; t <= 150; t++) {
            helper.runAfterDelay(t, () -> {
                if (!tub.isRemoved() && tub.getX() > at(helper, 1).getX() + 0.55 && tub.getX() < edge && !tub.isHeldAt(at(helper, 1))
                        && !tub.isHeldAt(at(helper, 7))) {
                    tub.setDeltaMovement(0.12, 0, 0);
                }
            });
        }
        return tub;
    }

    // A tub that runs from a stop in one charter's area to a stop in another's proves a tramway between them.
    private static void provesRoute(GameTestHelper helper) {
        Plan plan = new Plan(helper);
        TubStopBlockEntity[] stops = new TubStopBlockEntity[2];
        MineTubEntity tub = tramway(helper, stops);
        helper.runAfterDelay(170, () -> {
            try {
                helper.assertTrue(stops[1].isHolding() && tub.isHeldAt(at(helper, 7)), "the tub arrived and was held at the far stop");
                var links = plan.index.linksOf(plan.home.id());
                helper.assertValueEqual(links.size(), 1, "one link from the home charter");
                var link = links.get(0);
                helper.assertTrue(link.open() && link.joins(plan.mine.id()), "open, and it joins the mine");
                helper.assertTrue(link.kind() == dev.strataindustria.transport.outpost.LinkKind.TRAMWAY, "a tramway");
                helper.assertValueEqual(plan.index.bestTier(plan.home.id()), 3, "tier three");
                helper.assertTrue(link.route().contains(at(helper, 4)), "the route runs over the rail in between");
            } finally {
                plan.close();
            }
            helper.succeed();
        });
    }

    // Plain vanilla rail in the middle of the run is not wooden tramway: no line is proven.
    private static void vanillaNotProven(GameTestHelper helper) {
        Plan plan = new Plan(helper);
        TubStopBlockEntity[] stops = new TubStopBlockEntity[2];
        MineTubEntity tub = tramway(helper, stops);
        BlockPos middle = at(helper, 4);
        helper.getLevel().setBlock(middle, Blocks.RAIL.defaultBlockState().setValue(net.minecraft.world.level.block.RailBlock.SHAPE, RailShape.EAST_WEST), Block.UPDATE_ALL);
        helper.runAfterDelay(170, () -> {
            try {
                helper.assertTrue(stops[1].isHolding(), "the tub still got there");
                helper.assertTrue(plan.index.linksOf(plan.home.id()).isEmpty(), "but a vanilla rail proves nothing");
            } finally {
                plan.close();
            }
            helper.succeed();
        });
    }

    // Breaking a rail of the proven run cuts the line where it broke.
    private static void routeCut(GameTestHelper helper) {
        Plan plan = new Plan(helper);
        TubStopBlockEntity[] stops = new TubStopBlockEntity[2];
        tramway(helper, stops);
        BlockPos broken = at(helper, 4);
        helper.runAfterDelay(170, () -> {
            try {
                helper.assertValueEqual(plan.index.linksOf(plan.home.id()).size(), 1, "proven first");
                helper.getLevel().setBlock(broken, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                var link = plan.index.linksOf(plan.home.id()).get(0);
                helper.assertTrue(!link.open(), "the line is cut");
                helper.assertTrue(link.cutAt().isPresent() && link.cutAt().get().equals(broken), "where the rail was taken out");
            } finally {
                plan.close();
            }
            helper.succeed();
        });
    }
}
