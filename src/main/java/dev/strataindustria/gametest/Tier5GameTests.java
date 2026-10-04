package dev.strataindustria.gametest;

import com.mojang.authlib.GameProfile;
import dev.strataindustria.electric.BatteryBoxBlock;
import dev.strataindustria.electric.BatteryBoxBlockEntity;
import dev.strataindustria.electric.KineticDynamoBlock;
import dev.strataindustria.electric.KineticDynamoBlockEntity;
import dev.strataindustria.power.ElectricNetwork;
import dev.strataindustria.power.ElectricNetworks;
import dev.strataindustria.power.ElectricShare;
import dev.strataindustria.power.ElectricStatus;
import dev.strataindustria.power.ElectricTier;
import dev.strataindustria.power.HandCrankBlock;
import dev.strataindustria.power.HandCrankBlockEntity;
import dev.strataindustria.power.KineticNetworks;
import dev.strataindustria.power.KineticState;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.Tier5Blocks;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.util.FakePlayer;

/** Tier 5 (electric) tests, run by {@link ModGameTests}. */
final class Tier5GameTests {
    private Tier5GameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("tier5_network_maths", Tier5GameTests::networkMaths);
        tests.put("tier5_dynamo_charges_battery", Tier5GameTests::dynamoChargesBattery);
        tests.put("tier5_overvoltage", Tier5GameTests::overvoltage);
    }

    // Spec 6.3, 6.7 and 24: the worked example gives 88% to every machine; a charged battery box covers the
    // 4.15 J/t gap; a 16 J/t consumer on a 5% path draws 16.84 J/t; an LV cable caps the flow at 128 J/t;
    // 200 LV cable blocks are too far and 199 are not.
    private static void networkMaths(GameTestHelper helper) {
        double demand = ElectricShare.gross(16, 0.05) + ElectricShare.gross(8, 0.05) + ElectricShare.gross(8, 0.10);
        helper.assertTrue(Math.abs(demand - 34.15) < 0.01, "grossed-up demand of the worked example, got " + demand);
        helper.assertTrue(Math.abs(ElectricShare.gross(16, 0.05) - 16.84) < 0.01, "16 J/t on a 5% path");

        ElectricShare.Result short_ = ElectricShare.share(30, 0, 0, demand, ElectricTier.LV.cableCapacity());
        helper.assertValueEqual(Math.round(short_.fraction() * 100), 88L, "share of each machine on 30 J/t of supply");
        helper.assertTrue(Math.abs(short_.generated() - 30) < 1e-6, "the turbine gives all it has");

        ElectricShare.Result battery = ElectricShare.share(30, 32, 0, demand, ElectricTier.LV.cableCapacity());
        helper.assertTrue(battery.fraction() > 0.999, "a charged battery box makes up the gap, got " + battery.fraction());
        helper.assertTrue(Math.abs(battery.discharged() - 4.15) < 0.01, "the battery discharges 4.15 J/t, got " + battery.discharged());

        ElectricShare.Result surplus = ElectricShare.share(30, 0, 100, 16, ElectricTier.LV.cableCapacity());
        helper.assertTrue(Math.abs(surplus.charged() - 14) < 1e-6, "surplus charges storage, got " + surplus.charged());

        ElectricShare.Result capped = ElectricShare.share(1000, 0, 0, 200, ElectricTier.LV.cableCapacity());
        helper.assertTrue(capped.capped() && Math.abs(capped.generated() - 128) < 1e-6, "an LV cable caps the flow at 128 J/t");
        ElectricShare.Result mv = ElectricShare.share(1000, 0, 0, 200, ElectricTier.MV.cableCapacity());
        helper.assertTrue(!mv.capped() && mv.fraction() > 0.999, "an MV cable carries 200 J/t");

        double maxLoss = dev.strataindustria.Config.ELECTRIC_MAX_PATH_LOSS.get();
        helper.assertTrue(200 * ElectricTier.LV.cableLoss() >= maxLoss - 1e-9, "200 LV cable blocks are too far");
        helper.assertTrue(199 * ElectricTier.LV.cableLoss() < maxLoss, "199 LV cable blocks are served");

        helper.assertValueEqual(KineticDynamoBlockEntity.outputAt(4), 0.0, "dynamo below 8 RPM");
        helper.assertValueEqual(KineticDynamoBlockEntity.outputAt(16), 4.0, "dynamo at 16 RPM");
        helper.assertValueEqual(KineticDynamoBlockEntity.outputAt(32), 8.0, "dynamo at 32 RPM");
        helper.assertValueEqual(KineticDynamoBlockEntity.outputAt(64), 16.0, "dynamo at 64 RPM");
        helper.assertValueEqual(KineticDynamoBlockEntity.outputAt(128), 32.0, "dynamo at 128 RPM");
        helper.assertValueEqual(KineticDynamoBlockEntity.outputAt(256), 32.0, "dynamo output is capped at the LV limit");
        helper.succeed();
    }

    // Spec 7.1 and 7.4: four hand cranks (4 x 64 SU) turn a dynamo at 16 RPM, a 256 SU load; it makes 4 J/t,
    // which reaches a battery box four LV cable blocks away less 1% loss.
    private static void dynamoChargesBattery(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos left = helper.absolutePos(new BlockPos(3, 1, 2)), right = left.east();
        level.setBlock(left, ModBlocks.WOODEN_GEARBOX.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(right, ModBlocks.WOODEN_GEARBOX.get().defaultBlockState(), Block.UPDATE_ALL);
        List<BlockPos> cranks = List.of(left.north(), left.west(), left.south(), right.north());
        List<Direction> facings = List.of(Direction.SOUTH, Direction.EAST, Direction.NORTH, Direction.SOUTH);
        for (int i = 0; i < cranks.size(); i++) {
            level.setBlock(cranks.get(i), ModBlocks.HAND_CRANK.get().defaultBlockState().setValue(HandCrankBlock.FACING, facings.get(i)), Block.UPDATE_ALL);
        }
        BlockPos dynamoPos = right.south();
        level.setBlock(dynamoPos, Tier5Blocks.KINETIC_DYNAMO.get().defaultBlockState().setValue(KineticDynamoBlock.FACING, Direction.SOUTH), Block.UPDATE_ALL);
        BlockPos pos = dynamoPos;
        for (int i = 0; i < 4; i++) {
            pos = pos.south();
            level.setBlock(pos, Tier5Blocks.LV_CABLE.get().defaultBlockState(), Block.UPDATE_ALL);
        }
        BlockPos boxPos = pos.south();
        level.setBlock(boxPos, Tier5Blocks.BATTERY_BOX.get().defaultBlockState().setValue(BatteryBoxBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);

        FakePlayer engineer = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "engineer"));
        List<HandCrankBlockEntity> crankEntities = new ArrayList<>();
        for (BlockPos crankPos : cranks) crankEntities.add((HandCrankBlockEntity) level.getBlockEntity(crankPos));
        crankEntities.forEach(crank -> crank.crank(engineer));
        KineticNetworks.rebuildNow(level, dynamoPos);
        KineticDynamoBlockEntity dynamo = (KineticDynamoBlockEntity) level.getBlockEntity(dynamoPos);
        helper.assertValueEqual(dynamo.kinetic().status(), KineticState.Status.RUNNING, "four cranks carry the dynamo");
        helper.assertValueEqual(dynamo.kinetic().load(), 256, "dynamo load at 16 RPM");
        helper.assertValueEqual(dynamo.maxOutput(), 4.0, "dynamo output at 16 RPM");

        ElectricNetwork network = ElectricNetworks.rebuildNow(level, boxPos);
        helper.assertTrue(network != null && network.members().contains(dynamoPos), "the cables join the dynamo and the battery box");
        helper.assertValueEqual(network.tier(), ElectricTier.LV, "network tier");
        helper.assertTrue(Math.abs(network.loss(boxPos) - 0.01) < 1e-9, "four LV cable blocks lose 1%, got " + network.loss(boxPos));
        BatteryBoxBlockEntity box = (BatteryBoxBlockEntity) level.getBlockEntity(boxPos);
        for (int tick = 0; tick < 100; tick++) {
            crankEntities.forEach(crank -> crank.crank(engineer));
            network.tick();
        }
        helper.assertTrue(Math.abs(box.stored() - 396) < 0.01, "100 ticks of 4 J/t less 1% loss, got " + box.stored());
        helper.assertValueEqual(network.report(dynamoPos).status(), ElectricStatus.RUNNING, "the dynamo gives power");
        helper.assertValueEqual(network.report(boxPos).status(), ElectricStatus.RUNNING, "the battery box charges");
        BatteryBoxBlockEntity.serverTick(level, boxPos, level.getBlockState(boxPos), box);
        helper.assertValueEqual(level.getBlockState(boxPos).getValue(BatteryBoxBlock.CHARGE), 1, "one meter segment lit");
        helper.succeed();
    }

    // Spec 6.2: an MV source lifts the network to MV; an LV battery box on it stops with "Overvoltage"; an LV
    // cable on it stops the whole network and names its position; with the MV source gone it is LV again.
    private static void overvoltage(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos mvPos = helper.absolutePos(new BlockPos(2, 1, 4));
        BlockPos lvPos = mvPos.east(4);
        level.setBlock(mvPos, Tier5Blocks.BATTERY_BOX.get().defaultBlockState().setValue(BatteryBoxBlock.TIER, ElectricTier.MV), Block.UPDATE_ALL);
        level.setBlock(lvPos, Tier5Blocks.BATTERY_BOX.get().defaultBlockState(), Block.UPDATE_ALL);
        for (int i = 1; i < 4; i++) level.setBlock(mvPos.east(i), Tier5Blocks.MV_CABLE.get().defaultBlockState(), Block.UPDATE_ALL);
        ((BatteryBoxBlockEntity) level.getBlockEntity(mvPos)).setStored(1000);

        ElectricNetwork network = ElectricNetworks.rebuildNow(level, mvPos);
        helper.assertValueEqual(network.tier(), ElectricTier.MV, "an MV battery box makes an MV network");
        network.tick();
        helper.assertValueEqual(network.report(lvPos).status(), ElectricStatus.OVERVOLTAGE, "an LV battery box on an MV network");
        helper.assertValueEqual(network.report(mvPos).status(), ElectricStatus.IDLE, "the MV battery box has nothing to feed");

        BlockPos lvCable = mvPos.east(2);
        level.setBlock(lvCable, Tier5Blocks.LV_CABLE.get().defaultBlockState(), Block.UPDATE_ALL);
        network = ElectricNetworks.rebuildNow(level, mvPos);
        helper.assertValueEqual(network.overvoltageCable(), lvCable, "the LV cable is named");
        network.tick();
        helper.assertValueEqual(network.report(mvPos).status(), ElectricStatus.CABLE_OVERVOLTAGE, "the whole network stops");
        helper.assertValueEqual(network.report(lvPos).status(), ElectricStatus.CABLE_OVERVOLTAGE, "the whole network stops");
        helper.assertValueEqual(((BatteryBoxBlockEntity) level.getBlockEntity(mvPos)).stored(), 1000.0, "nothing flows and nothing breaks");

        level.removeBlock(mvPos, false);
        network = ElectricNetworks.rebuildNow(level, lvPos);
        helper.assertValueEqual(network.tier(), ElectricTier.LV, "without the MV box the network is LV");
        helper.assertTrue(network.overvoltageCable() == null, "the LV cable is fine on an LV network");
        network.tick();
        helper.assertValueEqual(network.report(lvPos).status(), ElectricStatus.IDLE, "the LV battery box is back");
        helper.succeed();
    }
}
