package dev.strataindustria.gametest;

import com.mojang.authlib.GameProfile;
import dev.strataindustria.registry.Tier4Blocks;
import dev.strataindustria.transport.outpost.Charter;
import dev.strataindustria.transport.outpost.LinkKind;
import dev.strataindustria.transport.outpost.OutpostPlan;
import dev.strataindustria.transport.outpost.RouteIndex;
import dev.strataindustria.transport.rail.CoalStageBlock;
import dev.strataindustria.transport.rail.CoalStageBlockEntity;
import dev.strataindustria.transport.rail.MineTubEntity;
import dev.strataindustria.transport.rail.OreWagonEntity;
import dev.strataindustria.transport.rail.RailRegistry;
import dev.strataindustria.transport.rail.RailwayRegistry;
import dev.strataindustria.transport.rail.SteamLocomotiveEntity;
import dev.strataindustria.transport.rail.StopData;
import dev.strataindustria.transport.rail.StopRule;
import dev.strataindustria.transport.rail.TubStopBlockEntity;
import dev.strataindustria.transport.rail.WaterTowerBaseBlockEntity;
import dev.strataindustria.transport.rail.WaterTowerSpoutBlock;
import dev.strataindustria.transport.rail.WaterTowerSpoutBlockEntity;
import dev.strataindustria.fluid.FluidTankBlockEntity;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

/** The steam locomotive, the water tower and the coal stage (outposts and transport spec 7.3, 7.4 and 15, chunk O7), run by {@link ModGameTests}. */
final class LocomotiveGameTests {
    private static final int ROW = RailGameTests.ROW;
    private static final int Y = RailGameTests.RAIL_Y;

    private LocomotiveGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("locomotive_registered", LocomotiveGameTests::registered);
        tests.put("locomotive_raises_steam", LocomotiveGameTests::raisesSteam);
        tests.put("locomotive_fuel_water", LocomotiveGameTests::fuelAndWater);
        tests.put("locomotive_speed_by_rail", LocomotiveGameTests::speedByRail);
        tests.put("locomotive_runs_dry", LocomotiveGameTests::runsDry);
        tests.put("locomotive_low_water_waits", LocomotiveGameTests::lowWaterWaits);
        tests.put("locomotive_hand_use", LocomotiveGameTests::handUse);
        tests.put("locomotive_consist_limit", LocomotiveGameTests::consistLimit);
        tests.put("locomotive_driverless_legs", LocomotiveGameTests::driverlessLegs);
        tests.put("locomotive_proves_railway", LocomotiveGameTests::provesRailway);
        tests.put("locomotive_wooden_sections", LocomotiveGameTests::woodenSections);
        tests.put("water_tower_pipe_fill", LocomotiveGameTests::towerPipeFill);
        tests.put("water_tower_refill", LocomotiveGameTests::towerRefill);
        tests.put("coal_stage_tops_up", LocomotiveGameTests::coalStage);
    }

    // ---------------------------------------------------------------- setup

    private static void steel(GameTestHelper helper, int from, int to) {
        for (int x = from; x <= to; x++) RailGameTests.lay(helper, x, RailwayRegistry.STEEL_TRACK.get());
    }

    private static TubStopBlockEntity station(GameTestHelper helper, int x, StopRule rule, int seconds) {
        RailGameTests.lay(helper, x, RailwayRegistry.STATION_TRACK.get());
        BlockPos pos = RailGameTests.at(helper, x);
        TubStopBlockEntity stop = (TubStopBlockEntity) helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(stop != null, "a station track has its block entity");
        stop.apply(new StopData(pos, "", rule, seconds, true));
        return stop;
    }

    /** A locomotive with steam up, water, coal and the brake off, standing at x. */
    private static SteamLocomotiveEntity loco(GameTestHelper helper, double x) {
        SteamLocomotiveEntity loco = helper.spawn(RailwayRegistry.STEAM_LOCOMOTIVE_ENTITY.get(), new BlockPos((int) Math.floor(x), Y, ROW));
        loco.setPos(helper.absolutePos(new BlockPos(0, Y, ROW)).getX() + x, loco.getY(), loco.getZ());
        loco.setParked(false);
        loco.setWater(SteamLocomotiveEntity.WATER_CAPACITY);
        loco.setPressure(1.0f);
        loco.takeFuel(new ItemStack(Items.COAL, 8));
        return loco;
    }

    private static double flat(MineTubEntity vehicle) {
        return vehicle.getDeltaMovement().horizontalDistance();
    }

    // ---------------------------------------------------------------- the engine

    // The engine, its three blocks and its recipe items exist.
    private static void registered(GameTestHelper helper) {
        helper.assertTrue(RailwayRegistry.STEAM_LOCOMOTIVE.get() != null, "the item");
        SteamLocomotiveEntity loco = loco(helper, 3.5);
        helper.assertTrue(loco.isLead() && loco.getContainerSize() == 1, "a lead with one fuel slot");
        helper.assertValueEqual(loco.fuel().getCount(), 8, "eight coal in the slot");
        helper.assertTrue(RailwayRegistry.WATER_TOWER_BASE.get().defaultBlockState().hasBlockEntity(), "the trestle has a block entity");
        helper.assertTrue(RailwayRegistry.COAL_STAGE.get().defaultBlockState().hasBlockEntity(), "the coal stage has a block entity");
        helper.succeed();
    }

    // A cold engine with fuel and water lights its firebox and builds pressure, and tops out at full steam.
    private static void raisesSteam(GameTestHelper helper) {
        steel(helper, 0, 8);
        SteamLocomotiveEntity loco = loco(helper, 3.5);
        loco.setParked(true);
        loco.setPressure(0.0f);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(loco.burning(), "the firebox is lit");
            helper.assertValueEqual(loco.fuel().getCount(), 7, "one coal went in");
            helper.assertTrue(loco.pressure() > 0.05f && loco.pressure() < 0.2f, "steam is coming up: " + loco.pressure());
            loco.setPressure(0.95f);
        });
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(loco.pressure() >= 1.0f, "and reaches full steam: " + loco.pressure());
            helper.assertValueEqual(loco.pressurePercent(), 100, "which the gauge shows");
            helper.succeed();
        });
    }

    // Moving costs water and burns the fire down; it never beats the track's limit.
    private static void fuelAndWater(GameTestHelper helper) {
        steel(helper, 0, 8);
        SteamLocomotiveEntity loco = loco(helper, 0.5);
        loco.setDeltaMovement(0.2, 0, 0);
        float[] start = {0, 0};
        helper.runAfterDelay(2, () -> {
            start[0] = loco.water();
            start[1] = loco.burnLeft();
        });
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(start[0] - loco.water() > 20, "it used water: " + (start[0] - loco.water()));
            helper.assertTrue(loco.burnLeft() < start[1] || loco.burnLeft() > 1000, "and the fire burned on");
            helper.assertTrue(flat(loco) <= 0.5 + 1.0E-6, "never over the steel limit: " + flat(loco));
            helper.succeed();
        });
    }

    // On steel it picks up speed; on wooden rail it is held to 0.2.
    private static void speedByRail(GameTestHelper helper) {
        steel(helper, 0, 8);
        SteamLocomotiveEntity fast = loco(helper, 0.5);
        fast.setDeltaMovement(0.3, 0, 0);
        helper.runAfterDelay(8, () -> {
            helper.assertTrue(flat(fast) > 0.3 && flat(fast) <= 0.5, "quickening on steel: " + flat(fast));
            helper.succeed();
        });
    }

    private static void runsDry(GameTestHelper helper) {
        steel(helper, 0, 8);
        SteamLocomotiveEntity loco = loco(helper, 0.5);
        loco.setWater(40);
        loco.setDeltaMovement(0.2, 0, 0);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(loco.dry() && loco.water() <= 0, "boiler dry");
            helper.assertTrue(loco.pressure() <= 0.2f, "the steam is gone: " + loco.pressure());
        });
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(flat(loco) < 0.02, "so it stands: " + flat(loco));
            helper.succeed();
        });
    }

    // A stop does not let a low-water engine go; with water it sets off.
    private static void lowWaterWaits(GameTestHelper helper) {
        steel(helper, 2, 6);
        station(helper, 1, StopRule.WAIT, 1);
        station(helper, 7, StopRule.REDSTONE, 1);
        SteamLocomotiveEntity loco = loco(helper, 1.5);
        loco.setWater(500);
        BlockPos stop = RailGameTests.at(helper, 1);
        helper.runAfterDelay(60, () -> helper.assertTrue(loco.isHeldAt(stop), "held at the stop, low on water"));
        helper.runAfterDelay(80, () -> loco.setWater(5000));
        helper.runAfterDelay(110, () -> {
            helper.assertTrue(!loco.isHeldAt(stop), "watered, it went");
            helper.succeed();
        });
    }

    // A hand: coal in the door, a bucket of water in the tank, sneak with an empty hand lifts the brake.
    private static void handUse(GameTestHelper helper) {
        steel(helper, 0, 8);
        FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "driver"));
        SteamLocomotiveEntity loco = loco(helper, 3.5);
        loco.setParked(true);
        loco.setWater(2000);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COAL, 5));
        loco.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
        helper.assertValueEqual(loco.fuel().getCount(), 9, "one coal in by hand");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        loco.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
        helper.assertTrue(Math.abs(loco.water() - 3000) < 1, "a bucket of water in: " + loco.water());
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.BUCKET), "the bucket is empty");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setShiftKeyDown(true);
        loco.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
        helper.assertTrue(!loco.parked(), "the brake is off");
        loco.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
        helper.assertTrue(loco.parked(), "and on again");
        helper.succeed();
    }

    // Eight wagons follow an engine; the ninth is refused.
    private static void consistLimit(GameTestHelper helper) {
        SteamLocomotiveEntity loco = loco(helper, 0.5);
        double base = helper.absolutePos(new BlockPos(0, Y, 0)).getX() + 0.5;
        double z = helper.absolutePos(new BlockPos(0, Y, 0)).getZ() + 0.5;
        loco.setPos(base, loco.getY(), z);
        MineTubEntity last = null;
        for (int i = 1; i <= 9; i++) {
            OreWagonEntity wagon = helper.spawn(RailwayRegistry.ORE_WAGON_ENTITY.get(), new BlockPos(0, Y, 0));
            wagon.setPos(base, wagon.getY(), z + 0.9 * i);
            MineTubEntity.Coupling result = wagon.couple();
            if (i <= 8) helper.assertTrue(result == MineTubEntity.Coupling.OK, "wagon " + i + " couples: " + result);
            else helper.assertTrue(result == MineTubEntity.Coupling.TOO_LONG, "the ninth is refused: " + result);
            last = wagon;
        }
        helper.assertValueEqual(loco.consist().size(), 9, "the engine and eight wagons");
        helper.assertTrue(loco.couple() == MineTubEntity.Coupling.LEADS, "an engine is never coupled behind");
        helper.succeed();
    }

    // Between two stations it runs by itself, stop to stop, and counts the legs it ran without a rider.
    private static void driverlessLegs(GameTestHelper helper) {
        steel(helper, 2, 6);
        station(helper, 1, StopRule.WAIT, 1);
        station(helper, 7, StopRule.WAIT, 1);
        SteamLocomotiveEntity loco = loco(helper, 1.5);
        boolean[] seen = {false, false};
        for (int t = 1; t <= 190; t++) {
            helper.runAfterDelay(t, () -> {
                if (loco.isHeldAt(RailGameTests.at(helper, 7))) seen[0] = true;
                if (seen[0] && loco.isHeldAt(RailGameTests.at(helper, 1))) seen[1] = true;
            });
        }
        helper.runAfterDelay(191, () -> {
            helper.assertTrue(seen[0], "it reached the far station");
            helper.assertTrue(seen[1], "and came back to the near one");
            helper.assertTrue(loco.legs() >= 2, "two driverless legs counted: " + loco.legs());
            helper.succeed();
        });
    }

    // ---------------------------------------------------------------- the railway

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

    private static SteamLocomotiveEntity runLine(GameTestHelper helper) {
        station(helper, 1, StopRule.WAIT, 1);
        station(helper, 7, StopRule.REDSTONE, 1);
        return loco(helper, 1.5);
    }

    // A locomotive that runs stop to stop over steel track proves a railway: tier four.
    private static void provesRailway(GameTestHelper helper) {
        Plan plan = new Plan(helper);
        steel(helper, 2, 6);
        SteamLocomotiveEntity loco = runLine(helper);
        helper.runAfterDelay(150, () -> {
            try {
                var links = plan.index.linksOf(plan.home.id());
                helper.assertValueEqual(links.size(), 1, "one link from the home charter");
                helper.assertTrue(links.get(0).kind() == LinkKind.RAILWAY, "a railway: " + links.get(0).kind());
                helper.assertValueEqual(plan.index.bestTier(plan.home.id()), 4, "tier four");
                helper.assertTrue(loco.isHeldAt(RailGameTests.at(helper, 7)), "it stands at the far station");
            } finally {
                plan.close();
            }
            helper.succeed();
        });
    }

    // With wooden rail still in the run, the engine proves a railway with wooden sections that keeps the tramway's area.
    private static void woodenSections(GameTestHelper helper) {
        Plan plan = new Plan(helper);
        steel(helper, 2, 6);
        RailGameTests.lay(helper, 4, RailRegistry.WOODEN_RAIL.get());
        runLine(helper);
        helper.runAfterDelay(190, () -> {
            try {
                var links = plan.index.linksOf(plan.home.id());
                helper.assertValueEqual(links.size(), 1, "one link from the home charter");
                helper.assertTrue(links.get(0).kind() == LinkKind.RAILWAY_MIXED, "a railway with wooden sections: " + links.get(0).kind());
                helper.assertValueEqual(plan.index.bestTier(plan.home.id()), 3, "which is tier three");
            } finally {
                plan.close();
            }
            helper.succeed();
        });
    }

    // ---------------------------------------------------------------- water tower and coal stage

    /** A trestle two blocks beside the track at x, a tank on it and the spout on the tank. */
    private static BlockPos tower(GameTestHelper helper, int x) {
        ServerLevel level = helper.getLevel();
        BlockPos base = RailGameTests.at(helper, x).south(2);
        level.setBlock(base.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(base, RailwayRegistry.WATER_TOWER_BASE.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(base.above(), Tier4Blocks.FLUID_TANK.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(base.above(2), RailwayRegistry.WATER_TOWER_SPOUT.get().defaultBlockState(), Block.UPDATE_ALL);
        return base;
    }

    // Water pushed into the trestle goes up into the tank; nothing else does.
    private static void towerPipeFill(GameTestHelper helper) {
        BlockPos base = tower(helper, 4);
        WaterTowerBaseBlockEntity foot = (WaterTowerBaseBlockEntity) helper.getLevel().getBlockEntity(base);
        FluidTankBlockEntity tank = (FluidTankBlockEntity) helper.getLevel().getBlockEntity(base.above());
        helper.assertValueEqual(foot.fill(Direction.NORTH, Fluids.WATER, 4000, 0, false), 4000, "the trestle takes water");
        helper.assertValueEqual(tank.amount(), 4000, "and it is in the tank");
        helper.assertValueEqual(foot.fill(Direction.NORTH, Fluids.LAVA, 1000, 0, false), 0, "but not lava");
        helper.assertValueEqual(foot.fill(Direction.UP, Fluids.WATER, 1000, 0, false), 0, "and not from the top");
        helper.succeed();
    }

    // An engine standing at the station beside the tower is filled at 400 mB a tick; the spout swings out while it pours and back in after.
    private static void towerRefill(GameTestHelper helper) {
        steel(helper, 2, 6);
        station(helper, 4, StopRule.REDSTONE, 1);
        BlockPos base = tower(helper, 4);
        FluidTankBlockEntity tank = (FluidTankBlockEntity) helper.getLevel().getBlockEntity(base.above());
        tank.fill(Fluids.WATER, 6000, 0, false);
        SteamLocomotiveEntity loco = loco(helper, 4.5);
        loco.setParked(true);
        loco.setWater(1000);
        BlockPos spout = base.above(2);
        helper.runAfterDelay(8, () -> {
            helper.assertTrue(loco.isHeldAt(RailGameTests.at(helper, 4)), "the engine stands at the station");
            helper.assertTrue(loco.water() > 1000, "water is going in: " + loco.water());
            helper.assertTrue(helper.getLevel().getBlockState(spout).getValue(WaterTowerSpoutBlock.POURING), "the spout swung out");
        });
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(Math.abs(loco.water() - 7000) < 1, "it took what the tank gave: " + loco.water());
            helper.assertValueEqual(tank.amount(), 0, "the tank is empty");
            helper.assertTrue(!helper.getLevel().getBlockState(spout).getValue(WaterTowerSpoutBlock.POURING), "and the spout is back in");
            helper.succeed();
        });
    }

    // A coal stage beside the station tops a standing engine up from its slots, a lump at a time.
    private static void coalStage(GameTestHelper helper) {
        steel(helper, 2, 6);
        station(helper, 4, StopRule.REDSTONE, 1);
        BlockPos pos = RailGameTests.at(helper, 4).south();
        helper.getLevel().setBlock(pos, RailwayRegistry.COAL_STAGE.get().defaultBlockState(), Block.UPDATE_ALL);
        CoalStageBlockEntity stage = (CoalStageBlockEntity) helper.getLevel().getBlockEntity(pos);
        stage.setItem(0, new ItemStack(Items.COAL, 20));
        stage.setItem(1, new ItemStack(Items.STICK, 4));
        SteamLocomotiveEntity loco = loco(helper, 4.5);
        loco.setParked(true);
        loco.setPressure(1.0f);
        loco.fuel().setCount(2);
        helper.runAfterDelay(15, () -> {
            helper.assertTrue(helper.getLevel().getBlockState(pos).getValue(CoalStageBlock.LOADING), "the hatch is open");
            helper.assertTrue(loco.fuel().getCount() > 4, "coal is going in: " + loco.fuel().getCount());
        });
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(stage.getItem(0).isEmpty(), "the stage gave all its coal");
            helper.assertTrue(loco.fuel().getCount() >= 21, "and the engine holds it, less what the fire ate: " + loco.fuel().getCount());
            helper.assertValueEqual(stage.getItem(1).getCount(), 4, "and left the sticks");
            helper.assertTrue(!helper.getLevel().getBlockState(pos).getValue(CoalStageBlock.LOADING), "the hatch is shut again");
            helper.succeed();
        });
    }
}
