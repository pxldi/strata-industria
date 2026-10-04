package dev.strataindustria.gametest;

import com.mojang.authlib.GameProfile;
import dev.strataindustria.electric.BatteryBoxBlock;
import dev.strataindustria.electric.BatteryBoxBlockEntity;
import dev.strataindustria.electric.machine.ElectricMachineBlock;
import dev.strataindustria.electric.machine.ElectricMachineBlockEntity;
import dev.strataindustria.electric.machine.MaceratorBlockEntity;
import dev.strataindustria.power.ElectricNetwork;
import dev.strataindustria.power.ElectricNetworks;
import dev.strataindustria.power.ElectricTier;
import dev.strataindustria.registry.Tier5Blocks;
import dev.strataindustria.transport.outpost.Charter;
import dev.strataindustria.transport.outpost.LinkKind;
import dev.strataindustria.transport.outpost.OutpostPlan;
import dev.strataindustria.transport.outpost.PowerLinks;
import dev.strataindustria.transport.outpost.RouteIndex;
import dev.strataindustria.transport.rail.ElectricTramEntity;
import dev.strataindustria.transport.rail.MineTubEntity;
import dev.strataindustria.transport.rail.OreWagonEntity;
import dev.strataindustria.transport.rail.RailwayRegistry;
import dev.strataindustria.transport.rail.StopData;
import dev.strataindustria.transport.rail.StopRule;
import dev.strataindustria.transport.rail.TramRegistry;
import dev.strataindustria.transport.rail.TrolleyBracketBlock;
import dev.strataindustria.transport.rail.TrolleyBracketBlockEntity;
import dev.strataindustria.transport.rail.TrolleyWires;
import dev.strataindustria.transport.rail.TubStopBlockEntity;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

/** The trolley wire, the electric tram and the power-line link (outposts and transport spec 9.2, 9.3 and 9.5, chunk O11), run by {@link ModGameTests}. */
final class TramGameTests {
    private static final int ROW = RailGameTests.ROW;
    /** The track lies on the floor of the cell so the poles and wire fit under its ceiling. */
    private static final int Y = 0;

    private TramGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("tram_registered", TramGameTests::registered);
        tests.put("trolley_wire_rules", TramGameTests::wireRules);
        tests.put("tram_runs_on_wire", TramGameTests::runsOnWire);
        tests.put("tram_mv_wire_overvoltage", TramGameTests::tooStrong);
        tests.put("tram_hand_use", TramGameTests::handUse);
        tests.put("tram_consist_limit", TramGameTests::consistLimit);
        tests.put("tram_proves_link", TramGameTests::provesLink);
        tests.put("power_line_proves_link", TramGameTests::powerLink);
    }

    // ---------------------------------------------------------------- setup

    static BlockPos at(GameTestHelper helper, int x) {
        return helper.absolutePos(new BlockPos(x, Y, ROW));
    }

    @SuppressWarnings("unchecked")
    static void lay(GameTestHelper helper, int x, Block block) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = at(helper, x);
        level.setBlock(pos.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        net.minecraft.world.level.block.state.BlockState state = block.defaultBlockState();
        if (block instanceof net.minecraft.world.level.block.BaseRailBlock rail) {
            state = state.setValue(rail.getShapeProperty(), net.minecraft.world.level.block.state.properties.RailShape.EAST_WEST);
        }
        level.setBlock(pos, state, Block.UPDATE_ALL);
    }

    static void steel(GameTestHelper helper, int from, int to) {
        for (int x = from; x <= to; x++) lay(helper, x, RailwayRegistry.STEEL_TRACK.get());
    }

    static TubStopBlockEntity station(GameTestHelper helper, int x, StopRule rule, int seconds) {
        lay(helper, x, RailwayRegistry.STATION_TRACK.get());
        BlockPos pos = at(helper, x);
        TubStopBlockEntity stop = (TubStopBlockEntity) helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(stop != null, "a station track has its block entity");
        stop.apply(new StopData(pos, "", rule, seconds, true));
        return stop;
    }

    /** A pole column beside the track at x, four blocks of it, and a bracket on top reaching over the rail. */
    private static BlockPos bracket(GameTestHelper helper, int x) {
        ServerLevel level = helper.getLevel();
        BlockPos foot = helper.absolutePos(new BlockPos(x, Y, ROW + 1));
        for (int i = 0; i <= 4; i++) level.setBlock(foot.above(i), Tier5Blocks.UTILITY_POLE.get().defaultBlockState(), Block.UPDATE_ALL);
        BlockPos pos = foot.above(4).north();
        level.setBlock(pos, TramRegistry.TROLLEY_BRACKET.get().defaultBlockState().setValue(TrolleyBracketBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);
        helper.assertTrue(level.getBlockEntity(pos) instanceof TrolleyBracketBlockEntity, "the bracket stands on the pole");
        return pos;
    }

    /** Wire from x = 1 to x = 7 over the track and a battery box of {@code tier} at the foot of the first pole. */
    static BatteryBoxBlockEntity line(GameTestHelper helper, ElectricTier tier, double stored) {
        ServerLevel level = helper.getLevel();
        BlockPos a = bracket(helper, 1), b = bracket(helper, 7);
        helper.assertTrue(TrolleyWires.problem(level, a, b) == null, "a clear span: " + TrolleyWires.problem(level, a, b));
        TrolleyWires.connect(level, a, b);
        BlockPos boxPos = helper.absolutePos(new BlockPos(2, Y, ROW + 1));
        level.setBlock(boxPos, Tier5Blocks.BATTERY_BOX.get().defaultBlockState().setValue(BatteryBoxBlock.TIER, tier), Block.UPDATE_ALL);
        BatteryBoxBlockEntity box = (BatteryBoxBlockEntity) level.getBlockEntity(boxPos);
        box.setStored(stored);
        ElectricNetworks.rebuildNow(level, boxPos);
        return box;
    }

    /** A tram, brake off, standing at x. */
    static ElectricTramEntity tram(GameTestHelper helper, double x) {
        ElectricTramEntity tram = helper.spawn(TramRegistry.ELECTRIC_TRAM_ENTITY.get(), new BlockPos((int) Math.floor(x), Y, ROW));
        tram.setPos(helper.absolutePos(new BlockPos(0, Y, ROW)).getX() + x, tram.getY(), tram.getZ());
        tram.setParked(false);
        return tram;
    }

    static double flat(MineTubEntity vehicle) {
        return vehicle.getDeltaMovement().horizontalDistance();
    }

    // ---------------------------------------------------------------- the pieces

    // The items and the entity exist; the tram is a lead with nine freight slots.
    private static void registered(GameTestHelper helper) {
        helper.assertTrue(TramRegistry.TROLLEY_BRACKET_ITEM.get() != null && TramRegistry.TROLLEY_WIRE.get() != null, "bracket and wire items");
        helper.assertTrue(TramRegistry.ELECTRIC_TRAM.get() != null, "the tram item");
        steel(helper, 0, 8);
        ElectricTramEntity tram = tram(helper, 3.5);
        helper.assertTrue(tram.isLead() && tram.getContainerSize() == 9, "a lead with nine slots");
        helper.assertTrue(tram.couple() == MineTubEntity.Coupling.LEADS, "a tram is never coupled behind");
        helper.assertTrue(tram.wire() == ElectricTramEntity.Wire.NONE && !tram.onWire(), "no wire overhead yet");
        helper.succeed();
    }

    // A span is checked for distance, a clear line and room on the bracket; it costs a wire per two blocks, and a broken bracket gives it back.
    private static void wireRules(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos a = bracket(helper, 1), b = bracket(helper, 7);
        helper.assertValueEqual(TrolleyWires.problem(level, a, a), "same", "one bracket is no span");
        helper.assertValueEqual(TrolleyWires.wireFor(a, b), 3, "six blocks cost three wire");
        helper.assertValueEqual(TrolleyWires.wireFor(a, a.east(7)), 4, "seven blocks round up to four");

        BlockPos middle = new BlockPos(a.getX() + 3, a.getY(), a.getZ());
        level.setBlock(middle, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        helper.assertValueEqual(TrolleyWires.problem(level, a, b), "blocked", "a solid block in the way refuses the span");
        level.setBlock(middle, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        helper.assertTrue(TrolleyWires.problem(level, a, b) == null, "a clear line is accepted");

        BlockPos far = a.east(20);
        level.setBlock(far.south(), Tier5Blocks.UTILITY_POLE.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(far, TramRegistry.TROLLEY_BRACKET.get().defaultBlockState().setValue(TrolleyBracketBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);
        helper.assertValueEqual(TrolleyWires.problem(level, a, far), "too_far", "twenty blocks is too far");

        TrolleyWires.connect(level, a, b);
        helper.assertValueEqual(TrolleyWires.problem(level, a, b), "already", "a second span between the same two is refused");
        ElectricNetwork net = ElectricNetworks.rebuildNow(level, a);
        helper.assertTrue(net.members().contains(b), "the span joins the two poles' network");

        level.destroyBlock(a, true);
        long dropped = level.getEntitiesOfClass(ItemEntity.class, new AABB(a).inflate(2)).stream()
                .filter(e -> e.getItem().is(TramRegistry.TROLLEY_WIRE.get())).mapToInt(e -> e.getItem().getCount()).sum();
        helper.assertValueEqual(dropped, 3L, "breaking a bracket drops its wire");
        helper.assertTrue(((TrolleyBracketBlockEntity) level.getBlockEntity(b)).spans().isEmpty(), "the other end is freed");
        helper.succeed();
    }

    // ---------------------------------------------------------------- the tram

    // Under live wire a tram sets off by itself, takes power from the battery behind the line, and coasts to a stop when the wire is cut.
    private static void runsOnWire(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        steel(helper, 0, 8);
        BatteryBoxBlockEntity box = line(helper, ElectricTier.LV, 50_000);
        ElectricTramEntity tram = tram(helper, 3.5);
        double[] start = {0, 0};
        for (int t = 5; t <= 30; t++) helper.runAfterDelay(t, () -> start[1] = Math.max(start[1], flat(tram)));
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(tram.onWire(), "the pole has found the wire");
            helper.assertTrue(tram.wire() == ElectricTramEntity.Wire.LIVE, "and it is live: " + tram.wire());
            helper.assertTrue(box.stored() < 50_000 - 100, "the battery gave power: " + box.stored());
            helper.assertTrue(start[1] > 0.1, "the tram ran: " + start[1]);
            start[0] = tram.distance();
            BlockPos a = helper.absolutePos(new BlockPos(1, Y + 4, ROW));
            level.destroyBlock(a, true);
        });
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(tram.distance() >= start[0], "it ran on live wire");
            helper.assertTrue(tram.wire() == ElectricTramEntity.Wire.NONE, "the wire is gone: " + tram.wire());
        });
        helper.runAfterDelay(200, () -> {
            helper.assertTrue(flat(tram) < 0.05, "and it has stopped: " + flat(tram));
            helper.succeed();
        });
    }

    // An MV line reads Overvoltage to the LV tram: it does not move.
    private static void tooStrong(GameTestHelper helper) {
        steel(helper, 0, 8);
        BatteryBoxBlockEntity box = line(helper, ElectricTier.MV, 50_000);
        ElectricTramEntity tram = tram(helper, 3.5);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(tram.wire() == ElectricTramEntity.Wire.TOO_STRONG, "too strong: " + tram.wire());
            helper.assertTrue(flat(tram) < 0.02, "it stands: " + flat(tram));
            helper.assertTrue(box.stored() > 49_999, "and takes nothing: " + box.stored());
            helper.succeed();
        });
    }

    // Sneak with an empty hand sets the brake; the bell rings and then waits.
    private static void handUse(GameTestHelper helper) {
        steel(helper, 0, 8);
        FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "driver"));
        ElectricTramEntity tram = tram(helper, 3.5);
        player.setShiftKeyDown(true);
        tram.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
        helper.assertTrue(tram.parked(), "the brake is on");
        tram.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
        helper.assertTrue(!tram.parked(), "and off again");
        helper.assertTrue(tram.ring(helper.getLevel(), 1.0f), "the bell rings");
        helper.assertTrue(!tram.ring(helper.getLevel(), 1.0f), "and not twice in a breath");
        helper.assertTrue(new ItemStack(TramRegistry.ELECTRIC_TRAM.get()).getMaxStackSize() == 1, "a tram stacks alone");
        helper.succeed();
    }

    // Four wagons follow a tram; the fifth is refused.
    private static void consistLimit(GameTestHelper helper) {
        ElectricTramEntity tram = tram(helper, 0.5);
        double base = helper.absolutePos(new BlockPos(0, Y, 0)).getX() + 0.5;
        double z = helper.absolutePos(new BlockPos(0, Y, 0)).getZ() + 0.5;
        tram.setPos(base, tram.getY(), z);
        for (int i = 1; i <= 5; i++) {
            OreWagonEntity wagon = helper.spawn(RailwayRegistry.ORE_WAGON_ENTITY.get(), new BlockPos(0, Y, 0));
            wagon.setPos(base, wagon.getY(), z + 0.9 * i);
            MineTubEntity.Coupling result = wagon.couple();
            if (i <= 4) helper.assertTrue(result == MineTubEntity.Coupling.OK, "wagon " + i + " couples: " + result);
            else helper.assertTrue(result == MineTubEntity.Coupling.TOO_LONG, "the fifth is refused: " + result);
        }
        helper.assertValueEqual(tram.consist().size(), 5, "the tram and four wagons");
        helper.succeed();
    }

    // ---------------------------------------------------------------- the links

    private static final class Plan {
        final ServerLevel level;
        final RouteIndex index;
        final FakePlayer owner;
        final Charter home, mine;

        Plan(GameTestHelper helper) {
            level = helper.getLevel();
            index = RouteIndex.get(level);
            owner = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "stationmaster"));
            OutpostPlan.ALSO_ONLINE.add(owner.getUUID());
            BlockPos base = helper.absolutePos(new BlockPos(0, Y, ROW));
            home = index.post(level, base, owner, "Home");
            mine = index.post(level, base.east(8), owner, "Mine");
        }

        void close() {
            index.clearOwner(level, owner.getUUID());
            OutpostPlan.ALSO_ONLINE.remove(owner.getUUID());
        }
    }

    // A tram that runs stop to stop over steel track under wire proves a tram line: tier five.
    private static void provesLink(GameTestHelper helper) {
        Plan plan = new Plan(helper);
        steel(helper, 2, 6);
        station(helper, 1, StopRule.WAIT, 1);
        station(helper, 7, StopRule.REDSTONE, 1);
        line(helper, ElectricTier.LV, 100_000);
        ElectricTramEntity tram = tram(helper, 1.5);
        helper.runAfterDelay(160, () -> {
            try {
                // Trams running in the cells beside this one may prove the same two charters over a different stretch of track.
                var links = plan.index.linksOf(plan.home.id());
                helper.assertTrue(!links.isEmpty(), "a link from the home charter");
                helper.assertTrue(links.stream().allMatch(link -> link.kind() == LinkKind.TRAM), "a tram line: " + links.get(0).kind());
                helper.assertValueEqual(plan.index.bestTier(plan.home.id()), 5, "tier five");
                helper.assertTrue(tram.isHeldAt(at(helper, 7)), "it stands at the far station");
            } finally {
                plan.close();
            }
            helper.succeed();
        });
    }

    // Overhead line that carries power between two charters' areas proves a power link; cutting the line cuts the link.
    private static void powerLink(GameTestHelper helper) {
        Plan plan = new Plan(helper);
        try {
            ServerLevel level = helper.getLevel();
            BlockPos a = helper.absolutePos(new BlockPos(1, 3, 1)), b = helper.absolutePos(new BlockPos(7, 3, 1));
            for (BlockPos top : java.util.List.of(a, b)) {
                level.setBlock(top.below(2), Tier5Blocks.UTILITY_POLE.get().defaultBlockState(), Block.UPDATE_ALL);
                level.setBlock(top.below(), Tier5Blocks.UTILITY_POLE.get().defaultBlockState(), Block.UPDATE_ALL);
                level.setBlock(top, Tier5Blocks.POLE_INSULATOR.get().defaultBlockState(), Block.UPDATE_ALL);
            }
            BlockPos boxPos = a.below(2).east(), machinePos = b.below(2).west();
            level.setBlock(boxPos, Tier5Blocks.BATTERY_BOX.get().defaultBlockState().setValue(BatteryBoxBlock.TIER, ElectricTier.MV), Block.UPDATE_ALL);
            level.setBlock(machinePos, Tier5Blocks.MACERATOR.get().defaultBlockState().setValue(ElectricMachineBlock.TIER, ElectricTier.MV), Block.UPDATE_ALL);
            ((BatteryBoxBlockEntity) level.getBlockEntity(boxPos)).setStored(100_000);
            MaceratorBlockEntity machine = (MaceratorBlockEntity) level.getBlockEntity(machinePos);
            machine.setItem(0, new ItemStack(Items.GRAVEL, 16));
            dev.strataindustria.electric.OverheadLine.connect(level, a, b);
            ElectricNetwork network = ElectricNetworks.rebuildNow(level, machinePos);
            helper.assertTrue(network.members().contains(boxPos), "the span joins the machine to the battery");
            for (int tick = 0; tick < 5; tick++) {
                ElectricMachineBlockEntity.serverTick(level, machinePos, level.getBlockState(machinePos), machine);
                network.tick();
            }
            helper.assertTrue(network.delivered() > 0, "power is moving over the line: " + network.delivered());
            PowerLinks.check(level, network);
            var links = plan.index.linksOf(plan.home.id());
            helper.assertValueEqual(links.size(), 1, "one link from the home charter");
            helper.assertTrue(links.get(0).kind() == LinkKind.POWER, "a power link: " + links.get(0).kind());
            helper.assertTrue(links.get(0).joins(plan.mine.id()), "joining the other charter");

            level.destroyBlock(a, true);
            helper.assertTrue(plan.index.linksOf(plan.home.id()).stream().noneMatch(link -> link.kind() == LinkKind.POWER && link.open()),
                    "breaking the insulator cuts the link");
        } finally {
            plan.close();
        }
        helper.succeed();
    }
}
