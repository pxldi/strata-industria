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
import dev.strataindustria.forge.ForgeBlock;
import dev.strataindustria.forge.ForgeBlockEntity;
import dev.strataindustria.machine.BellowsBlock;
import dev.strataindustria.machine.BellowsBlockEntity;
import dev.strataindustria.material.Metal;
import dev.strataindustria.metal.Alloy;
import dev.strataindustria.metal.CrucibleBlockEntity;
import dev.strataindustria.metal.CrucibleStatus;
import dev.strataindustria.metal.Melt;
import dev.strataindustria.metal.MetalContent;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.Tier5Blocks;
import dev.strataindustria.registry.Tier5Fluids;
import dev.strataindustria.registry.Tier5Items;
import dev.strataindustria.rubber.TreeTapBlock;
import dev.strataindustria.rubber.TreeTapBlockEntity;
import dev.strataindustria.tanning.SoakingBarrelBlock;
import dev.strataindustria.tanning.SoakingBarrelBlockEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayer;

/** Tier 5 (electric) tests, run by {@link ModGameTests}. */
final class Tier5GameTests {
    private Tier5GameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("tier5_network_maths", Tier5GameTests::networkMaths);
        tests.put("tier5_dynamo_charges_battery", Tier5GameTests::dynamoChargesBattery);
        tests.put("tier5_overvoltage", Tier5GameTests::overvoltage);
        tests.put("tier5_tree_taps", Tier5GameTests::treeTaps);
        tests.put("tier5_no_living_tree", Tier5GameTests::noLivingTree);
        tests.put("tier5_latex_to_rubber", Tier5GameTests::latexToRubber);
        tests.put("tier5_red_alloy", Tier5GameTests::redAlloy);
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

    // Spec 5.1 and 24: a jungle tree with five taps: four fill 1000 mB each in 10 000 ticks and the fifth says the
    // tree is tapped out and stays dry; a full cup gives a latex bucket.
    private static void treeTaps(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos trunk = helper.absolutePos(new BlockPos(4, 1, 4));
        jungleTree(level, trunk, 3, true);
        List<BlockPos> taps = List.of(trunk.north(), trunk.east(), trunk.south(), trunk.west(), trunk.above().north());
        List<Direction> facings = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH);
        List<TreeTapBlockEntity> entities = new ArrayList<>();
        for (int i = 0; i < taps.size(); i++) {
            level.setBlock(taps.get(i), Tier5Blocks.TREE_TAP.get().defaultBlockState().setValue(TreeTapBlock.FACING, facings.get(i)), Block.UPDATE_ALL);
            entities.add((TreeTapBlockEntity) level.getBlockEntity(taps.get(i)));
        }
        for (int tick = 0; tick < 10_000; tick++) {
            for (int i = 0; i < taps.size(); i++) tickTap(level, taps.get(i), entities.get(i));
        }
        int working = 0, tappedOut = 0, total = 0;
        for (TreeTapBlockEntity tap : entities) {
            if (tap.status() == TreeTapBlockEntity.Status.WORKING) {
                working++;
                helper.assertValueEqual(tap.amount(), TreeTapBlockEntity.CUP, "a drawing tap fills its cup in 10 000 ticks");
            } else if (tap.status() == TreeTapBlockEntity.Status.TAPPED_OUT) {
                tappedOut++;
                helper.assertValueEqual(tap.amount(), 0, "a tap on a tapped-out tree stays dry");
            }
            total += tap.amount();
        }
        helper.assertValueEqual(working, 4, "taps drawing from one tree");
        helper.assertValueEqual(tappedOut, 1, "taps told the tree is tapped out");
        helper.assertValueEqual(total, 4000, "latex from one tree in 10 000 ticks");
        TreeTapBlockEntity full = entities.stream().filter(TreeTapBlockEntity::full).findFirst().orElseThrow();
        helper.assertValueEqual(level.getBlockState(full.getBlockPos()).getValue(TreeTapBlock.FILL), 3, "a full cup shows full");
        ItemStack bucket = full.takeBucket();
        helper.assertTrue(bucket.is(Tier5Items.LATEX_BUCKET.get()), "a full cup gives a latex bucket, got " + bucket);
        helper.assertValueEqual(full.amount(), 0, "the cup is empty after the bucket");
        helper.succeed();
    }

    // Spec 5.1: a tap on a trunk with no leaves, or on a stump two logs high, says "No living tree" and drips nothing.
    private static void noLivingTree(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos bare = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos stump = helper.absolutePos(new BlockPos(6, 1, 6));
        jungleTree(level, bare, 3, false);
        jungleTree(level, stump, 2, true);
        for (BlockPos trunk : List.of(bare, stump)) {
            BlockPos tapPos = trunk.north();
            level.setBlock(tapPos, Tier5Blocks.TREE_TAP.get().defaultBlockState().setValue(TreeTapBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);
            TreeTapBlockEntity tap = (TreeTapBlockEntity) level.getBlockEntity(tapPos);
            for (int tick = 0; tick < 200; tick++) tickTap(level, tapPos, tap);
            helper.assertValueEqual(tap.status(), TreeTapBlockEntity.Status.NO_TREE, "status of a tap at " + trunk);
            helper.assertValueEqual(tap.amount(), 0, "latex from a tap that is not on a living tree");
            helper.assertValueEqual(tap.statusLine().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t ? t.getKey() : "",
                    TreeTapBlockEntity.Status.NO_TREE.key(), "the tap says why");
        }
        helper.succeed();
    }

    // Spec 5.1 to 5.3: a full tap drains into the open barrel below it; sealed, 1000 mB of latex sets into 4 raw rubber.
    private static void latexToRubber(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos trunk = helper.absolutePos(new BlockPos(4, 1, 4));
        jungleTree(level, trunk, 4, true);
        BlockPos tapPos = trunk.above().east();
        BlockPos barrelPos = tapPos.below();
        level.setBlock(tapPos, Tier5Blocks.TREE_TAP.get().defaultBlockState().setValue(TreeTapBlock.FACING, Direction.EAST), Block.UPDATE_ALL);
        level.setBlock(barrelPos, ModBlocks.SOAKING_BARREL.get().defaultBlockState(), Block.UPDATE_ALL);
        TreeTapBlockEntity tap = (TreeTapBlockEntity) level.getBlockEntity(tapPos);
        SoakingBarrelBlockEntity barrel = (SoakingBarrelBlockEntity) level.getBlockEntity(barrelPos);
        tap.setAmount(TreeTapBlockEntity.CUP);
        for (int tick = 0; tick < 100; tick++) tickTap(level, tapPos, tap);
        helper.assertTrue(barrel.fluid().isSame(Tier5Fluids.LATEX.get()), "the barrel below catches latex");
        helper.assertTrue(barrel.amount() >= 1000, "the cup drains into the barrel, got " + barrel.amount());
        helper.assertTrue(tap.amount() < 20, "the cup is nearly empty, got " + tap.amount());

        int latex = barrel.amount();
        level.setBlock(barrelPos, level.getBlockState(barrelPos).setValue(SoakingBarrelBlock.SEALED, true), Block.UPDATE_ALL);
        barrel.lidChanged();
        for (int tick = 0; tick < 1300; tick++) {
            SoakingBarrelBlockEntity.serverTick(level, barrelPos, level.getBlockState(barrelPos), barrel);
        }
        ItemStack out = barrel.getItem(SoakingBarrelBlockEntity.OUTPUT);
        int batches = latex / 1000;
        helper.assertTrue(out.is(Tier5Items.RAW_RUBBER.get()) && out.getCount() == 4 * batches, "4 raw rubber per bucket of latex, got " + out);
        helper.assertValueEqual(barrel.amount(), latex - 1000 * batches, "latex left after setting");
        helper.succeed();
    }

    // Spec 4.2 and 4.3: redstone waits for molten copper ("Redstone needs molten copper"); 2 copper ingots and
    // 8 redstone make 400 units of red alloy; a red alloy ingot remelts as red alloy.
    private static void redAlloy(GameTestHelper helper) {
        Melt batch = meltOf(Items.COPPER_INGOT, 2).plus(meltOf(Items.REDSTONE, 8));
        helper.assertValueEqual(batch.total(), 400, "units in 2 copper ingots and 8 redstone");
        helper.assertValueEqual(Alloy.resultOf(batch).orElse(null), Metal.RED_ALLOY, "2 copper + 8 redstone");
        helper.assertValueEqual(Alloy.resultOf(meltOf(ModItems.ingot(Metal.RED_ALLOY), 2)).orElse(null), Metal.RED_ALLOY, "red alloy remelt");
        helper.assertTrue(Alloy.resultOf(meltOf(Items.COPPER_INGOT, 4).plus(meltOf(Items.REDSTONE, 4))).isEmpty(),
                "80% copper is outside the red alloy range");
        helper.assertTrue(Alloy.resultOf(meltOf(Items.REDSTONE, 4)).isEmpty(), "redstone alone is no metal");

        ServerLevel level = helper.getLevel();
        BlockPos forgePos = helper.absolutePos(new BlockPos(4, 1, 4));
        BlockPos cruciblePos = forgePos.above();
        BlockPos bellowsPos = forgePos.west();
        level.setBlock(forgePos, ModBlocks.FORGE.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(cruciblePos, ModBlocks.CRUCIBLE.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(bellowsPos, ModBlocks.BELLOWS.get().defaultBlockState().setValue(BellowsBlock.FACING, Direction.EAST), Block.UPDATE_ALL);
        ForgeBlockEntity forge = (ForgeBlockEntity) level.getBlockEntity(forgePos);
        CrucibleBlockEntity crucible = (CrucibleBlockEntity) level.getBlockEntity(cruciblePos);
        BellowsBlockEntity bellows = (BellowsBlockEntity) level.getBlockEntity(bellowsPos);
        forge.setItem(ForgeBlockEntity.FUEL_SLOT, new ItemStack(Items.CHARCOAL, 32));
        BlockState forgeState = level.getBlockState(forgePos);
        helper.assertTrue(((ForgeBlock) forgeState.getBlock()).ignite(level, forgePos, forgeState), "the forge should light");
        FakePlayer smith = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "smith"));

        crucible.setItem(0, new ItemStack(Items.REDSTONE, 8));
        for (int tick = 0; tick < 400; tick++) heatCrucible(level, smith, bellowsPos, bellows, forgePos, forge, cruciblePos, crucible);
        helper.assertValueEqual(crucible.getItem(0).getCount(), 8, "redstone stays in its slot without copper");
        helper.assertValueEqual(crucible.status(), CrucibleStatus.REDSTONE_WAITING, "crucible status with only redstone");

        crucible.setItem(1, new ItemStack(Items.COPPER_INGOT, 2));
        for (int tick = 0; tick < 20000; tick++) {
            heatCrucible(level, smith, bellowsPos, bellows, forgePos, forge, cruciblePos, crucible);
            if (crucible.getItem(0).isEmpty() && crucible.getItem(1).isEmpty() && crucible.isMolten()) break;
        }
        helper.assertTrue(crucible.getItem(0).isEmpty(), "the redstone dissolves into the molten copper");
        helper.assertValueEqual(crucible.melt().total(), 400, "units of copper and redstone");
        helper.assertValueEqual(crucible.result().orElse(null), Metal.RED_ALLOY, "melt result");
        helper.succeed();
    }

    private static void heatCrucible(ServerLevel level, FakePlayer smith, BlockPos bellowsPos, BellowsBlockEntity bellows, BlockPos forgePos,
            ForgeBlockEntity forge, BlockPos cruciblePos, CrucibleBlockEntity crucible) {
        bellows.pump(smith);
        BellowsBlockEntity.serverTick(level, bellowsPos, level.getBlockState(bellowsPos), bellows);
        ForgeBlockEntity.serverTick(level, forgePos, level.getBlockState(forgePos), forge);
        CrucibleBlockEntity.serverTick(level, cruciblePos, level.getBlockState(cruciblePos), crucible);
    }

    private static Melt meltOf(Item item, int count) {
        Melt one = MetalContent.of(new ItemStack(item)).orElseThrow();
        Melt total = Melt.EMPTY;
        for (int i = 0; i < count; i++) total = total.plus(one);
        return total;
    }

    /** A jungle trunk {@code height} logs high, with a ring of leaves round the top when {@code leaves}. */
    private static void jungleTree(ServerLevel level, BlockPos base, int height, boolean leaves) {
        for (int y = 0; y < height; y++) level.setBlock(base.above(y), Blocks.JUNGLE_LOG.defaultBlockState(), Block.UPDATE_ALL);
        if (!leaves) return;
        BlockPos top = base.above(height);
        BlockState leaf = Blocks.JUNGLE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        for (Direction d : Direction.Plane.HORIZONTAL) level.setBlock(top.relative(d), leaf, Block.UPDATE_ALL);
        level.setBlock(top, leaf, Block.UPDATE_ALL);
    }

    private static void tickTap(ServerLevel level, BlockPos pos, TreeTapBlockEntity tap) {
        TreeTapBlockEntity.serverTick(level, pos, level.getBlockState(pos), tap);
    }
}
