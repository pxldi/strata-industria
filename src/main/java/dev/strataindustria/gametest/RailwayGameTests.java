package dev.strataindustria.gametest;

import com.mojang.authlib.GameProfile;
import dev.strataindustria.registry.Tier4Blocks;
import dev.strataindustria.transport.rail.FlatWagonEntity;
import dev.strataindustria.transport.rail.FlatWagonEvents;
import dev.strataindustria.transport.rail.MineTubEntity;
import dev.strataindustria.transport.rail.OreWagonEntity;
import dev.strataindustria.transport.rail.RailBufferBlock;
import dev.strataindustria.transport.rail.RailGrade;
import dev.strataindustria.transport.rail.RailRegistry;
import dev.strataindustria.transport.rail.RailwayRegistry;
import dev.strataindustria.transport.rail.StopData;
import dev.strataindustria.transport.rail.StopRule;
import dev.strataindustria.transport.rail.TankWagonEntity;
import dev.strataindustria.transport.rail.TubStopBlockEntity;
import dev.strataindustria.transport.rail.WagonFluidPortBlock;
import dev.strataindustria.transport.rail.WagonFluidPortBlockEntity;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

/** The steel track and wagons (outposts and transport spec 7.1, 7.2 and 15, chunk O6), run by {@link ModGameTests}. */
final class RailwayGameTests {
    private RailwayGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("railway_blocks", RailwayGameTests::blocks);
        tests.put("railway_grades", RailwayGameTests::grades);
        tests.put("railway_steel_is_fast", RailwayGameTests::steelIsFast);
        tests.put("railway_wood_stays_slow", RailwayGameTests::woodStaysSlow);
        tests.put("railway_steel_buffer", RailwayGameTests::steelBuffer);
        tests.put("railway_station_holds", RailwayGameTests::stationHolds);
        tests.put("railway_wagons_couple", RailwayGameTests::wagonsCouple);
        tests.put("railway_ore_wagon_tips", RailwayGameTests::oreWagonTips);
        tests.put("railway_tank_wagon_tank", RailwayGameTests::tankWagonTank);
        tests.put("railway_tank_wagon_rules", RailwayGameTests::tankWagonRules);
        tests.put("railway_port_fills", RailwayGameTests::portFills);
        tests.put("railway_port_empties", RailwayGameTests::portEmpties);
        tests.put("railway_flat_wagon_carries", RailwayGameTests::flatWagonCarries);
        tests.put("railway_flat_wagon_refuses", RailwayGameTests::flatWagonRefuses);
    }

    // ---------------------------------------------------------------- setup

    private static void run(GameTestHelper helper, int from, int to) {
        for (int x = from; x <= to; x++) RailGameTests.lay(helper, x, RailwayRegistry.STEEL_TRACK.get());
    }

    private static <T extends MineTubEntity> T wagonAt(GameTestHelper helper, EntityType<T> type, double x) {
        T wagon = helper.spawn(type, new BlockPos((int) Math.floor(x), RailGameTests.RAIL_Y, RailGameTests.ROW));
        wagon.setPos(helper.absolutePos(new BlockPos(0, RailGameTests.RAIL_Y, RailGameTests.ROW)).getX() + x, wagon.getY(), wagon.getZ());
        return wagon;
    }

    private static double flat(MineTubEntity vehicle) {
        return vehicle.getDeltaMovement().horizontalDistance();
    }

    // ---------------------------------------------------------------- blocks

    // Every piece of steel track is a rail and part of the track a route may be proven over.
    private static void blocks(GameTestHelper helper) {
        for (var block : List.of(RailwayRegistry.STEEL_TRACK, RailwayRegistry.STATION_TRACK, RailwayRegistry.STEEL_BUFFER)) {
            var state = block.get().defaultBlockState();
            helper.assertTrue(state.is(BlockTags.RAILS), block.getId() + " is a rail");
            helper.assertTrue(state.is(RailRegistry.TRACK), block.getId() + " is track");
        }
        helper.assertTrue(!RailwayRegistry.WAGON_FLUID_PORT.get().defaultBlockState().is(RailRegistry.TRACK), "the fluid port is not track");
        helper.succeed();
    }

    // Steel is steel, wood is wood, the tipple is both; a route is a railway only when nothing wooden is in it.
    private static void grades(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.assertTrue(RailGrade.of(RailwayRegistry.STEEL_TRACK.get().defaultBlockState()) == RailGrade.STEEL, "steel track");
        helper.assertTrue(RailGrade.of(RailwayRegistry.STATION_TRACK.get().defaultBlockState()) == RailGrade.STEEL, "station track");
        helper.assertTrue(RailGrade.of(RailwayRegistry.STEEL_BUFFER.get().defaultBlockState()) == RailGrade.STEEL, "steel buffer");
        helper.assertTrue(RailGrade.of(RailRegistry.WOODEN_RAIL.get().defaultBlockState()) == RailGrade.WOOD, "wooden rail");
        helper.assertTrue(RailGrade.of(RailRegistry.TUB_STOP.get().defaultBlockState()) == RailGrade.WOOD, "tub stop");
        helper.assertTrue(RailGrade.of(RailRegistry.TIPPLE_RAIL.get().defaultBlockState()) == RailGrade.NEUTRAL, "tipple");
        helper.assertTrue(RailGrade.of(Blocks.RAIL.defaultBlockState()) == null, "a vanilla rail has no grade");
        run(helper, 0, 3);
        RailGameTests.lay(helper, 4, RailRegistry.TIPPLE_RAIL.get());
        RailGameTests.lay(helper, 5, RailwayRegistry.STATION_TRACK.get());
        List<BlockPos> route = List.of(RailGameTests.at(helper, 0), RailGameTests.at(helper, 1), RailGameTests.at(helper, 4), RailGameTests.at(helper, 5));
        helper.assertTrue(RailGrade.steelRoute(level, route), "steel and a tipple make a steel route");
        RailGameTests.lay(helper, 2, RailRegistry.WOODEN_RAIL.get());
        helper.assertTrue(!RailGrade.steelRoute(level, List.of(RailGameTests.at(helper, 1), RailGameTests.at(helper, 2))), "one wooden rail makes it a tramway");
        helper.succeed();
    }

    // ---------------------------------------------------------------- speed

    // A wagon shoved hard on steel is held to 0.5 b/t and keeps nearly all of it: the track is a good deal faster than wood.
    private static void steelIsFast(GameTestHelper helper) {
        run(helper, 0, 8);
        OreWagonEntity wagon = wagonAt(helper, RailwayRegistry.ORE_WAGON_ENTITY.get(), 0.5);
        wagon.setDeltaMovement(0.8, 0, 0);
        double[] at = {0, 0};
        helper.runAfterDelay(2, () -> at[0] = wagon.getX());
        helper.runAfterDelay(12, () -> {
            at[1] = wagon.getX();
            helper.assertTrue(at[1] - at[0] <= 10 * 0.5 + 0.01, "held to the steel limit, 0.5 b/t: moved " + (at[1] - at[0]) + " in ten ticks");
            helper.assertTrue(at[1] - at[0] >= 10 * 0.45, "and it keeps nearly all of it, moved " + (at[1] - at[0]));
            helper.succeed();
        });
    }

    // The same shove on wooden rail is held to 0.2 b/t.
    private static void woodStaysSlow(GameTestHelper helper) {
        RailGameTests.layRun(helper, 0, 8);
        OreWagonEntity wagon = wagonAt(helper, RailwayRegistry.ORE_WAGON_ENTITY.get(), 0.5);
        wagon.setDeltaMovement(0.8, 0, 0);
        double[] at = {0, 0};
        helper.runAfterDelay(2, () -> at[0] = wagon.getX());
        helper.runAfterDelay(12, () -> {
            at[1] = wagon.getX();
            helper.assertTrue(at[1] - at[0] <= 10 * 0.2 + 0.01, "held to the wooden limit, 0.2 b/t: moved " + (at[1] - at[0]) + " in ten ticks");
            helper.succeed();
        });
    }

    // ---------------------------------------------------------------- buffer and stop

    // A fast wagon stops dead at the steel buffer, short of the end of its block.
    private static void steelBuffer(GameTestHelper helper) {
        run(helper, 0, 6);
        RailGameTests.lay(helper, 7, RailwayRegistry.STEEL_BUFFER.get());
        BlockPos pos = RailGameTests.at(helper, 7);
        helper.getLevel().setBlock(pos, helper.getLevel().getBlockState(pos).setValue(RailBufferBlock.FACING, Direction.EAST), Block.UPDATE_ALL);
        TankWagonEntity wagon = wagonAt(helper, RailwayRegistry.TANK_WAGON_ENTITY.get(), 3.5);
        wagon.setDeltaMovement(0.4, 0, 0);
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(flat(wagon) < 0.01, "the wagon is at rest, speed " + flat(wagon));
            helper.assertTrue(wagon.getX() > pos.getX() + 0.2 && wagon.getX() < pos.getX() + 0.6, "against the beam, x " + wagon.getX());
            helper.succeed();
        });
    }

    // The station track is a stop with the tub stop's rules: it holds a wagon, then lets it go.
    private static void stationHolds(GameTestHelper helper) {
        run(helper, 0, 12);
        RailGameTests.lay(helper, 6, RailwayRegistry.STATION_TRACK.get());
        BlockPos pos = RailGameTests.at(helper, 6);
        TubStopBlockEntity stop = (TubStopBlockEntity) helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(stop != null, "the station track has a stop's block entity");
        stop.apply(new StopData(pos, "", StopRule.WAIT, 1, false));
        OreWagonEntity wagon = wagonAt(helper, RailwayRegistry.ORE_WAGON_ENTITY.get(), 2.5);
        wagon.setDeltaMovement(0.2, 0, 0);
        int[] held = {-1, -1};
        for (int t = 1; t <= 110; t++) {
            int tick = t;
            helper.runAfterDelay(t, () -> {
                if (stop.isHolding() && held[0] < 0) held[0] = tick;
                if (held[0] >= 0 && held[1] < 0 && !stop.isHolding()) held[1] = tick;
            });
        }
        helper.runAfterDelay(111, () -> {
            helper.assertTrue(held[0] > 0, "the station caught the wagon");
            int waited = held[1] - held[0];
            helper.assertTrue(waited >= 18 && waited <= 24, "one second is about 20 ticks, waited " + waited);
            helper.assertTrue(wagon.getX() > pos.getX() + 0.6, "and it went on east, x " + wagon.getX());
            helper.succeed();
        });
    }

    // ---------------------------------------------------------------- wagons

    // Wagons of every kind couple behind a tub and are drawn along with it; a coupled wagon can be uncoupled again.
    private static void wagonsCouple(GameTestHelper helper) {
        run(helper, 0, 8);
        MineTubEntity lead = RailGameTests.tubAt(helper, 4.5);
        OreWagonEntity ore = wagonAt(helper, RailwayRegistry.ORE_WAGON_ENTITY.get(), 3.25);
        TankWagonEntity tank = wagonAt(helper, RailwayRegistry.TANK_WAGON_ENTITY.get(), 2.0);
        FlatWagonEntity flat = wagonAt(helper, RailwayRegistry.FLAT_WAGON_ENTITY.get(), 0.75);
        var r1 = ore.couple();
        helper.assertTrue(r1 == MineTubEntity.Coupling.OK, "ore wagon couples, " + r1 + " lead x " + lead.getX() + " ore x " + ore.getX());
        var r2 = tank.couple();
        helper.assertTrue(r2 == MineTubEntity.Coupling.OK, "tank wagon couples, " + r2 + " tank x " + tank.getX());
        var r3 = flat.couple();
        helper.assertTrue(r3 == MineTubEntity.Coupling.OK, "flat wagon couples, " + r3 + " flat x " + flat.getX() + " tank x " + tank.getX() + " y " + flat.getY() + "/" + tank.getY());
        helper.assertValueEqual(lead.consist().size(), 4, "one consist of four");
        double start = flat.getX();
        for (int t = 1; t <= 15; t++) helper.runAfterDelay(t, () -> lead.setDeltaMovement(0.15, 0, 0));
        helper.runAfterDelay(26, () -> {
            helper.assertTrue(flat.getX() > start + 1.0, "the last wagon was pulled, moved " + (flat.getX() - start));
            helper.assertTrue(lead.getX() > ore.getX() && ore.getX() > tank.getX() && tank.getX() > flat.getX(), "in order");
            helper.assertValueEqual(lead.consist().size(), 4, "none let go");
            helper.succeed();
        });
    }

    // The ore wagon has 27 slots and tips what is in them into a chest on a tipple, a stack at a time.
    private static void oreWagonTips(GameTestHelper helper) {
        run(helper, 0, 2);
        RailGameTests.lay(helper, 3, RailRegistry.TIPPLE_RAIL.get());
        BlockPos chest = RailGameTests.at(helper, 3).below();
        helper.getLevel().setBlock(chest, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
        OreWagonEntity wagon = wagonAt(helper, RailwayRegistry.ORE_WAGON_ENTITY.get(), 3.5);
        helper.assertValueEqual(wagon.getContainerSize(), 27, "twenty-seven slots");
        // Slots from the far end of the wagon, so a bin that only reads the first nine would miss them.
        for (int slot = 20; slot < 27; slot++) wagon.setItem(slot, new ItemStack(Items.COBBLESTONE, 9));
        helper.succeedWhen(() -> {
            ChestBlockEntity bin = (ChestBlockEntity) helper.getLevel().getBlockEntity(chest);
            int moved = 0;
            for (int slot = 0; slot < bin.getContainerSize(); slot++) moved += bin.getItem(slot).getCount();
            helper.assertValueEqual(moved, 63, "everything arrived in the chest");
            helper.assertTrue(wagon.isEmpty(), "and the wagon is empty");
        });
    }

    // The tank wagon holds one fluid up to 16 000 mB and gives it back.
    private static void tankWagonTank(GameTestHelper helper) {
        TankWagonEntity wagon = wagonAt(helper, RailwayRegistry.TANK_WAGON_ENTITY.get(), 3.5);
        helper.assertValueEqual(wagon.fill(Fluids.WATER, 5000, false), 5000, "takes 5000 mB");
        helper.assertValueEqual(wagon.fill(Fluids.LAVA, 1000, false), 0, "one fluid at a time");
        helper.assertValueEqual(wagon.fill(Fluids.WATER, 20000, true), 11000, "room for 11000 more, simulated");
        helper.assertValueEqual(wagon.amount(), 5000, "a simulated fill changes nothing");
        helper.assertValueEqual(wagon.fill(Fluids.WATER, 20000, false), 11000, "fills to the brim");
        helper.assertValueEqual(wagon.gauge(), 16, "the gauge reads full");
        helper.assertValueEqual(wagon.drain(6000, false), 6000, "gives back 6000 mB");
        helper.assertValueEqual(wagon.amount(), 10000, "and keeps the rest");
        wagon.drain(99999, false);
        helper.assertTrue(wagon.fluid().isSame(Fluids.EMPTY) && wagon.gauge() == 0, "empty again, free for another fluid");
        helper.assertValueEqual(wagon.fill(Fluids.LAVA, 1000, false), 1000, "now it takes lava");
        helper.succeed();
    }

    // A tank wagon at a stop is full when its tank is, empty when it holds nothing, and its comparator reads the level.
    private static void tankWagonRules(GameTestHelper helper) {
        run(helper, 0, 12);
        RailGameTests.lay(helper, 6, RailwayRegistry.STATION_TRACK.get());
        BlockPos pos = RailGameTests.at(helper, 6);
        TubStopBlockEntity stop = (TubStopBlockEntity) helper.getLevel().getBlockEntity(pos);
        stop.apply(new StopData(pos, "", StopRule.FULL, 1, false));
        TankWagonEntity wagon = wagonAt(helper, RailwayRegistry.TANK_WAGON_ENTITY.get(), 2.5);
        wagon.setDeltaMovement(0.2, 0, 0);
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(stop.isHolding(), "the station holds an empty tank wagon");
            helper.assertValueEqual(stop.fill(helper.getLevel()), 0, "comparator reads nothing");
            wagon.fill(Fluids.WATER, 8000, false);
            helper.assertTrue(stop.fill(helper.getLevel()) >= 7 && stop.fill(helper.getLevel()) <= 9, "half a tank reads about half, " + stop.fill(helper.getLevel()));
            helper.assertTrue(stop.isHolding(), "half full is not full");
        });
        helper.runAfterDelay(70, () -> wagon.fill(Fluids.WATER, 8000, false));
        helper.runAfterDelay(110, () -> {
            helper.assertValueEqual(wagon.amount(), TankWagonEntity.CAPACITY, "brim full");
            helper.assertTrue(!stop.isHolding(), "a full tank is let go");
            helper.succeed();
        });
    }

    // ---------------------------------------------------------------- fluid port

    /** A station track at x 4, a tank wagon standing on it, and a fluid port south of it set to {@code mode}. */
    private static WagonFluidPortBlockEntity portBeside(GameTestHelper helper, WagonFluidPortBlock.Mode mode, TankWagonEntity[] out) {
        run(helper, 0, 8);
        RailGameTests.lay(helper, 4, RailwayRegistry.STATION_TRACK.get());
        BlockPos track = RailGameTests.at(helper, 4);
        BlockPos port = track.south();
        helper.getLevel().setBlock(port.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        helper.getLevel().setBlock(port, RailwayRegistry.WAGON_FLUID_PORT.get().defaultBlockState().setValue(WagonFluidPortBlock.MODE, mode), Block.UPDATE_ALL);
        TankWagonEntity wagon = wagonAt(helper, RailwayRegistry.TANK_WAGON_ENTITY.get(), 4.5);
        wagon.setDeltaMovement(Vec3.ZERO);
        out[0] = wagon;
        return (WagonFluidPortBlockEntity) helper.getLevel().getBlockEntity(port);
    }

    // A filling port hands what the pipes give it to the standing wagon, 200 mB a tick at most.
    private static void portFills(GameTestHelper helper) {
        TankWagonEntity[] wagon = new TankWagonEntity[1];
        WagonFluidPortBlockEntity port = portBeside(helper, WagonFluidPortBlock.Mode.LOAD, wagon);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(port.wagon() == wagon[0], "the port finds the wagon on the station track");
            int took = port.fill(Direction.EAST, Fluids.WATER, 1000, 0f, false);
            helper.assertValueEqual(took, WagonFluidPortBlockEntity.RATE, "200 mB in one tick");
            helper.assertValueEqual(port.fill(Direction.EAST, Fluids.WATER, 1000, 0f, false), 0, "and no more that tick");
        });
        helper.runAfterDelay(7, () -> {
            helper.assertValueEqual(port.fill(Direction.EAST, Fluids.WATER, 1000, 0f, false), WagonFluidPortBlockEntity.RATE, "the next tick takes 200 more");
            helper.assertValueEqual(wagon[0].amount(), 400, "400 mB in the wagon");
            wagon[0].setPos(wagon[0].getX() + 3, wagon[0].getY(), wagon[0].getZ());
            helper.assertValueEqual(port.fill(Direction.EAST, Fluids.WATER, 1000, 0f, true), 0, "a wagon that has left takes nothing");
            helper.succeed();
        });
    }

    // An emptying port pushes the wagon's fluid through a pipe into a tank.
    private static void portEmpties(GameTestHelper helper) {
        TankWagonEntity[] wagon = new TankWagonEntity[1];
        portBeside(helper, WagonFluidPortBlock.Mode.UNLOAD, wagon);
        BlockPos port = RailGameTests.at(helper, 4).south();
        helper.getLevel().setBlock(port.east(), Tier4Blocks.STEEL_FLUID_PIPE.get().defaultBlockState(), Block.UPDATE_ALL);
        helper.getLevel().setBlock(port.east(2), Tier4Blocks.FLUID_TANK.get().defaultBlockState(), Block.UPDATE_ALL);
        helper.getLevel().setBlock(port.east(2).below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        wagon[0].fill(Fluids.WATER, 3000, false);
        helper.succeedWhen(() -> {
            var tank = (dev.strataindustria.fluid.FluidTankBlockEntity) helper.getLevel().getBlockEntity(port.east(2));
            helper.assertValueEqual(tank.amount(), 3000, "all of it reached the tank");
            helper.assertTrue(wagon[0].amount() == 0, "and the wagon is empty");
        });
    }

    // ---------------------------------------------------------------- flat wagon

    // A flat wagon lifts a chest with what is in it, drops nothing, and sets it down again whole.
    private static void flatWagonCarries(GameTestHelper helper) {
        run(helper, 0, 6);
        FlatWagonEntity wagon = wagonAt(helper, RailwayRegistry.FLAT_WAGON_ENTITY.get(), 3.5);
        BlockPos chest = RailGameTests.at(helper, 3).north(2);
        helper.getLevel().setBlock(chest.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        helper.getLevel().setBlock(chest, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
        ((ChestBlockEntity) helper.getLevel().getBlockEntity(chest)).setItem(5, new ItemStack(Items.DIAMOND, 7));
        FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "stoker"));
        helper.assertTrue(FlatWagonEvents.emptyWagonNear(helper.getLevel(), chest) == wagon, "the wagon is in reach of the chest");
        helper.assertTrue(FlatWagonEvents.lift(helper.getLevel(), player, chest, wagon), "the chest comes up");
        helper.assertTrue(wagon.loaded() && wagon.load().is(Blocks.CHEST), "the wagon carries a chest");
        helper.assertTrue(helper.getLevel().getBlockState(chest).isAir(), "and the place is bare");
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(Entity.class, new net.minecraft.world.phys.AABB(chest).inflate(3),
                e -> e instanceof net.minecraft.world.entity.item.ItemEntity).isEmpty(), "nothing spilled");
        helper.assertTrue(FlatWagonEvents.emptyWagonNear(helper.getLevel(), chest) == null, "a loaded wagon takes no second block");
        helper.assertTrue(wagon.isEmpty() == false && wagon.cannotTakeMore(helper.getLevel()), "the stop rules see a full wagon");
        BlockPos down = RailGameTests.at(helper, 5).south();
        helper.getLevel().setBlock(down.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        wagon.placeLoad(helper.getLevel(), down);
        helper.assertTrue(!wagon.loaded(), "the deck is clear");
        ChestBlockEntity again = (ChestBlockEntity) helper.getLevel().getBlockEntity(down);
        helper.assertTrue(again != null && again.getItem(5).is(Items.DIAMOND) && again.getItem(5).getCount() == 7, "the chest has its diamonds");
        helper.succeed();
    }

    // Bedrock, rails and a charter will not ride; nor does a wagon already carrying take a second block.
    private static void flatWagonRefuses(GameTestHelper helper) {
        run(helper, 0, 6);
        FlatWagonEntity wagon = wagonAt(helper, RailwayRegistry.FLAT_WAGON_ENTITY.get(), 3.5);
        FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "stoker"));
        BlockPos bedrock = RailGameTests.at(helper, 3).north();
        helper.getLevel().setBlock(bedrock, Blocks.BEDROCK.defaultBlockState(), Block.UPDATE_ALL);
        helper.assertTrue(!FlatWagonEvents.lift(helper.getLevel(), player, bedrock, wagon), "bedrock stays");
        helper.assertTrue(!FlatWagonEvents.lift(helper.getLevel(), player, RailGameTests.at(helper, 2), wagon), "a rail stays");
        helper.assertTrue(!wagon.loaded(), "nothing went on the deck");
        helper.succeed();
    }
}
