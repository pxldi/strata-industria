package dev.strataindustria.gametest;

import com.mojang.authlib.GameProfile;
import dev.strataindustria.electric.BatteryBoxBlock;
import dev.strataindustria.electric.BatteryBoxBlockEntity;
import dev.strataindustria.electric.CombustionGeneratorBlockEntity;
import dev.strataindustria.electric.GeneratorBlock;
import dev.strataindustria.electric.SteamTurbineBlockEntity;
import dev.strataindustria.registry.Tier4Fluids;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import dev.strataindustria.electric.KineticDynamoBlock;
import dev.strataindustria.electric.MachineBlockItem;
import dev.strataindustria.electric.MvUpgradeKitItem;
import dev.strataindustria.registry.Tier5DataComponents;
import dev.strataindustria.electric.KineticDynamoBlockEntity;
import dev.strataindustria.electric.machine.ChemicalMachineBlockEntity;
import dev.strataindustria.electric.machine.AssemblerBlockEntity;
import dev.strataindustria.electric.machine.ElectrolyserBlockEntity;
import dev.strataindustria.electric.machine.MixerBlockEntity;
import dev.strataindustria.heat.Heat;
import dev.strataindustria.registry.Tier4Items;
import dev.strataindustria.electric.machine.ElectricFurnaceBlockEntity;
import dev.strataindustria.electric.machine.ElectricMachineBlock;
import dev.strataindustria.electric.machine.ElectricMachineBlockEntity;
import dev.strataindustria.electric.machine.ElectricMachineLayout;
import dev.strataindustria.electric.machine.LatheBlockEntity;
import dev.strataindustria.electric.machine.LatheBlock;
import dev.strataindustria.electric.machine.MaceratorBlockEntity;
import dev.strataindustria.power.StatusLight;
import dev.strataindustria.processing.CrushingRecipe;
import dev.strataindustria.processing.Processing;
import dev.strataindustria.StrataIndustria;
import net.minecraft.core.registries.BuiltInRegistries;
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
        tests.put("tier5_macerator_mv_rule", Tier5GameTests::maceratorMvRule);
        tests.put("tier5_macerator_second_piece", Tier5GameTests::maceratorSecondPiece);
        tests.put("tier5_machine_low_power", Tier5GameTests::machineLowPower);
        tests.put("tier5_electric_furnace", Tier5GameTests::electricFurnace);
        tests.put("tier5_steam_turbine", Tier5GameTests::steamTurbine);
        tests.put("tier5_combustion_generator", Tier5GameTests::combustionGenerator);
        tests.put("tier5_shaping_machines", Tier5GameTests::shapingMachines);
        tests.put("tier5_aluminium_chain", Tier5GameTests::aluminiumChain);
        tests.put("tier5_assembler", Tier5GameTests::assembler);
        tests.put("tier5_mv_upgrade", Tier5GameTests::mvUpgrade);
        tests.put("tier5_transformer", Tier5GameTests::transformer);
        tests.put("tier5_energy_adapter", Tier5GameTests::energyAdapter);
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

    // Spec 10.1 and 24: an LV macerator on a charged battery box crushes one item per 100 ticks at 16 J/t; an MV
    // one crushes four times as many for four times the power, the same 1600 J per item; an LV macerator on an
    // MV network stops with overvoltage.
    private static void maceratorMvRule(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        int lv = runMacerator(helper, level, helper.absolutePos(new BlockPos(1, 1, 1)), ElectricTier.LV, 1000);
        int mv = runMacerator(helper, level, helper.absolutePos(new BlockPos(1, 1, 5)), ElectricTier.MV, 1000);
        helper.assertValueEqual(lv, 10, "LV macerator items in 1000 ticks");
        helper.assertValueEqual(mv, 40, "MV macerator items in 1000 ticks");

        BlockPos boxPos = helper.absolutePos(new BlockPos(5, 1, 1)), machinePos = boxPos.east();
        level.setBlock(boxPos, Tier5Blocks.BATTERY_BOX.get().defaultBlockState().setValue(BatteryBoxBlock.TIER, ElectricTier.MV), Block.UPDATE_ALL);
        level.setBlock(machinePos, Tier5Blocks.MACERATOR.get().defaultBlockState(), Block.UPDATE_ALL);
        ((BatteryBoxBlockEntity) level.getBlockEntity(boxPos)).setStored(10_000);
        MaceratorBlockEntity machine = (MaceratorBlockEntity) level.getBlockEntity(machinePos);
        machine.setItem(0, new ItemStack(Items.GRAVEL, 4));
        ElectricNetworks.rebuildNow(level, boxPos).tick();
        ElectricMachineBlockEntity.serverTick(level, machinePos, level.getBlockState(machinePos), machine);
        helper.assertValueEqual(machine.status(), ElectricMachineBlockEntity.Status.OVERVOLTAGE, "an LV macerator on an MV network");
        helper.assertValueEqual(level.getBlockState(machinePos).getValue(ElectricMachineBlock.STATUS), StatusLight.ERROR, "red status lamp");
        helper.succeed();
    }

    /** A macerator of {@code tier} beside a charged battery box of the same tier, crushing gravel; returns the items done. */
    private static int runMacerator(GameTestHelper helper, ServerLevel level, BlockPos boxPos, ElectricTier tier, int ticks) {
        BlockPos machinePos = boxPos.east();
        level.setBlock(boxPos, Tier5Blocks.BATTERY_BOX.get().defaultBlockState().setValue(BatteryBoxBlock.TIER, tier), Block.UPDATE_ALL);
        level.setBlock(machinePos, Tier5Blocks.MACERATOR.get().defaultBlockState().setValue(ElectricMachineBlock.TIER, tier), Block.UPDATE_ALL);
        BatteryBoxBlockEntity box = (BatteryBoxBlockEntity) level.getBlockEntity(boxPos);
        box.setStored(100_000);
        MaceratorBlockEntity machine = (MaceratorBlockEntity) level.getBlockEntity(machinePos);
        machine.setItem(0, new ItemStack(Items.GRAVEL, 64));
        machine.setBuffer(machine.bufferCapacity());
        ElectricNetwork network = ElectricNetworks.rebuildNow(level, boxPos);
        helper.assertTrue(network != null && network.members().contains(machinePos), "the macerator joins the battery box");
        helper.assertValueEqual(machine.lanes(), tier == ElectricTier.MV ? 2 : 1, tier.label() + " lanes");
        for (int tick = 0; tick < ticks; tick++) {
            ElectricMachineBlockEntity.serverTick(level, machinePos, level.getBlockState(machinePos), machine);
            network.tick();
        }
        int done = machine.finishedCount();
        helper.assertValueEqual(machine.status(), ElectricMachineBlockEntity.Status.WORKING, tier.label() + " macerator status");
        helper.assertValueEqual(level.getBlockState(machinePos).getValue(ElectricMachineBlock.STATUS), StatusLight.RUN, "green status lamp");
        double used = 100_000 - box.stored();
        helper.assertTrue(Math.abs(used - done * 1600.0) < 1e-6, tier.label() + " energy per item is 1600 J, used " + used + " for " + done);
        int sand = 0;
        for (int slot = 0; slot < machine.getContainerSize(); slot++) {
            if (machine.getItem(slot).is(Items.SAND)) sand += machine.getItem(slot).getCount();
        }
        helper.assertValueEqual(sand, done, tier.label() + " sand out");
        return done;
    }

    // Spec 10.3 and 24: an ore piece gives a second crushed piece 25% of the time over 1000 trials (within 3%).
    private static void maceratorSecondPiece(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Item galena = BuiltInRegistries.ITEM.getValue(StrataIndustria.id("galena"));
        Item crushed = BuiltInRegistries.ITEM.getValue(StrataIndustria.id("crushed_galena"));
        var recipe = CrushingRecipe.recipeFor(level, new ItemStack(galena));
        helper.assertTrue(recipe.isPresent(), "galena has a crushing recipe");
        Processing processing = MaceratorBlockEntity.maceration(recipe.get().value());
        int seconds = 0;
        for (int trial = 0; trial < 1000; trial++) {
            int pieces = 0;
            for (ItemStack out : processing.roll(level.getRandom())) if (out.is(crushed)) pieces += out.getCount();
            helper.assertTrue(pieces == 1 || pieces == 2, "one or two crushed pieces, got " + pieces);
            if (pieces == 2) seconds++;
        }
        helper.assertTrue(Math.abs(seconds - 250) <= 30, "second pieces in 1000 trials, got " + seconds);
        helper.succeed();
    }

    // Spec 6.1 and 6.4: half the draw runs at half speed with "Low power (50%)" and an amber lamp; no power stops
    // it; a full output stops it with a red lamp; auto-eject empties it into a chest behind.
    private static void machineLowPower(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(3, 1, 3));
        level.setBlock(pos, Tier5Blocks.MACERATOR.get().defaultBlockState().setValue(ElectricMachineBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);
        MaceratorBlockEntity machine = (MaceratorBlockEntity) level.getBlockEntity(pos);
        machine.setItem(0, new ItemStack(Items.GRAVEL, 3));
        for (int tick = 0; tick < 199; tick++) {
            machine.setBuffer(8);
            ElectricMachineBlockEntity.serverTick(level, pos, level.getBlockState(pos), machine);
        }
        helper.assertValueEqual(machine.status(), ElectricMachineBlockEntity.Status.LOW_POWER, "status on half power");
        helper.assertValueEqual(Math.round(machine.power() * 100), 50, "power percent");
        helper.assertValueEqual(level.getBlockState(pos).getValue(ElectricMachineBlock.STATUS), StatusLight.WAIT, "amber status lamp");
        helper.assertValueEqual(machine.finishedCount(), 0, "199 ticks at half speed is not yet one item");
        machine.setBuffer(8);
        ElectricMachineBlockEntity.serverTick(level, pos, level.getBlockState(pos), machine);
        helper.assertValueEqual(machine.finishedCount(), 1, "200 ticks at half speed is one item");

        machine.setBuffer(0);
        ElectricMachineBlockEntity.serverTick(level, pos, level.getBlockState(pos), machine);
        helper.assertValueEqual(machine.status(), ElectricMachineBlockEntity.Status.NO_POWER, "status with an empty buffer");

        for (int i = 0; i < 3; i++) machine.setItem(ElectricMachineLayout.MACERATOR.outputSlot(0, i), new ItemStack(Items.SAND, 64));
        machine.setBuffer(machine.bufferCapacity());
        ElectricMachineBlockEntity.serverTick(level, pos, level.getBlockState(pos), machine);
        helper.assertValueEqual(machine.status(), ElectricMachineBlockEntity.Status.OUTPUT_FULL, "status with full outputs");
        helper.assertValueEqual(level.getBlockState(pos).getValue(ElectricMachineBlock.STATUS), StatusLight.ERROR, "red status lamp");

        BlockPos chestPos = pos.south();
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
        machine.toggleAutoEject();
        ElectricMachineBlockEntity.serverTick(level, pos, level.getBlockState(pos), machine);
        var chest = (net.minecraft.world.level.block.entity.ChestBlockEntity) level.getBlockEntity(chestPos);
        helper.assertValueEqual(chest.getItem(0).getCount(), 64, "auto-eject moves one stack into the chest behind");
        helper.succeed();
    }

    // Spec 10.2: the electric furnace roasts (half the recipe ticks, the gas vented with no pipe), fires clay in
    // 200 ticks and smelts in 100; it refuses what none of those take.
    private static void electricFurnace(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(3, 1, 3));
        level.setBlock(pos, Tier5Blocks.ELECTRIC_FURNACE.get().defaultBlockState(), Block.UPDATE_ALL);
        ElectricFurnaceBlockEntity furnace = (ElectricFurnaceBlockEntity) level.getBlockEntity(pos);
        Item sphalerite = BuiltInRegistries.ITEM.getValue(StrataIndustria.id("crushed_sphalerite"));
        Item calcine = BuiltInRegistries.ITEM.getValue(StrataIndustria.id("zinc_calcine"));
        helper.assertValueEqual(furnaceTicks(level, pos, furnace, new ItemStack(sphalerite), calcine), 200, "roasting sphalerite (400 ticks at 800 C)");
        helper.assertValueEqual(furnaceTicks(level, pos, furnace, new ItemStack(ModItems.UNFIRED_BRICK.get()), Items.BRICK), 200, "firing a brick");
        helper.assertValueEqual(furnaceTicks(level, pos, furnace, new ItemStack(Items.SAND), Items.GLASS), 100, "smelting sand");
        helper.assertTrue(!furnace.canPlaceItem(0, new ItemStack(Items.STICK)), "a stick is refused");
        helper.succeed();
    }

    /** Ticks the furnace takes to turn {@code input} into {@code result} on a full buffer. */
    private static int furnaceTicks(ServerLevel level, BlockPos pos, ElectricFurnaceBlockEntity furnace, ItemStack input, Item result) {
        furnace.clearContent();
        furnace.setItem(0, input);
        for (int tick = 1; tick <= 1000; tick++) {
            furnace.setBuffer(furnace.bufferCapacity());
            ElectricMachineBlockEntity.serverTick(level, pos, level.getBlockState(pos), furnace);
            if (furnace.getItem(ElectricMachineLayout.ELECTRIC_FURNACE.outputSlot(0, 0)).is(result)) return tick;
        }
        return -1;
    }

    // Spec 10.4 to 10.6: the wiremill draws a copper rod into two wires, the bender rolls an ingot into a plate and a
    // double ingot into two, the lathe makes two rods or one gear by mode; each takes 100 ticks and the machine's
    // draw per tick, and refuses what its recipes do not take.
    private static void shapingMachines(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos base = helper.absolutePos(new BlockPos(1, 1, 1));
        Item copperWire = Tier5Items.COPPER_WIRE.get();

        BlockPos wiremillPos = base;
        level.setBlock(wiremillPos, Tier5Blocks.WIREMILL.get().defaultBlockState(), Block.UPDATE_ALL);
        ElectricMachineBlockEntity wiremill = (ElectricMachineBlockEntity) level.getBlockEntity(wiremillPos);
        helper.assertTrue(!wiremill.canPlaceItem(0, new ItemStack(Items.COPPER_INGOT)), "the wiremill refuses an ingot");
        wiremill.setItem(0, new ItemStack(Tier5Items.COPPER_ROD.get()));
        helper.assertValueEqual(shapingTicks(level, wiremillPos, wiremill, ElectricMachineLayout.WIREMILL, copperWire), 100, "copper rod to wire");
        helper.assertValueEqual(wiremill.getItem(ElectricMachineLayout.WIREMILL.outputSlot(0, 0)).getCount(), 2, "two wires from a rod");

        BlockPos benderPos = base.south(2);
        level.setBlock(benderPos, Tier5Blocks.BENDER.get().defaultBlockState(), Block.UPDATE_ALL);
        ElectricMachineBlockEntity bender = (ElectricMachineBlockEntity) level.getBlockEntity(benderPos);
        Item steelPlate = ModItems.PLATES.get(Metal.STEEL).get();
        bender.setItem(0, new ItemStack(ModItems.ingot(Metal.STEEL)));
        helper.assertValueEqual(shapingTicks(level, benderPos, bender, ElectricMachineLayout.BENDER, steelPlate), 100, "steel ingot to plate");
        helper.assertValueEqual(bender.getItem(ElectricMachineLayout.BENDER.outputSlot(0, 0)).getCount(), 1, "one plate from an ingot");
        bender.clearContent();
        bender.setItem(0, new ItemStack(ModItems.STEEL_DOUBLE_INGOT.get()));
        shapingTicks(level, benderPos, bender, ElectricMachineLayout.BENDER, steelPlate);
        helper.assertValueEqual(bender.getItem(ElectricMachineLayout.BENDER.outputSlot(0, 0)).getCount(), 2, "two plates from a double ingot");

        BlockPos lathePos = base.south(4);
        level.setBlock(lathePos, Tier5Blocks.LATHE.get().defaultBlockState(), Block.UPDATE_ALL);
        LatheBlockEntity lathe = (LatheBlockEntity) level.getBlockEntity(lathePos);
        Item steelRod = ModItems.RODS.get(Metal.STEEL).get(), steelGear = ModItems.GEARS.get(Metal.STEEL).get();
        lathe.setItem(0, new ItemStack(ModItems.ingot(Metal.STEEL)));
        helper.assertValueEqual(shapingTicks(level, lathePos, lathe, ElectricMachineLayout.LATHE, steelRod), 100, "steel ingot to rods");
        helper.assertValueEqual(lathe.getItem(ElectricMachineLayout.LATHE.outputSlot(0, 0)).getCount(), 2, "two rods from an ingot");
        lathe.clearContent();
        lathe.toggleMode();
        helper.assertTrue(level.getBlockState(lathePos).getValue(LatheBlock.GEAR), "the mode button switches to gears");
        helper.assertTrue(!lathe.canPlaceItem(0, new ItemStack(Items.COPPER_INGOT)), "copper has no gear");
        lathe.setItem(0, new ItemStack(ModItems.ingot(Metal.STEEL)));
        helper.assertValueEqual(shapingTicks(level, lathePos, lathe, ElectricMachineLayout.LATHE, steelGear), 100, "steel ingot to a gear");
        helper.assertValueEqual(lathe.getItem(ElectricMachineLayout.LATHE.outputSlot(0, 0)).getCount(), 1, "one gear from an ingot");

        // Energy per item: 100 ticks at the machine's draw.
        for (var entry : java.util.List.of(java.util.Map.entry(wiremill, 800.0), java.util.Map.entry(bender, 1600.0), java.util.Map.entry(lathe, 1600.0))) {
            double joules = entry.getKey().stats().draw().get(ElectricTier.LV) * 100;
            helper.assertTrue(Math.abs(joules - entry.getValue()) < 1e-6, "energy per item " + joules);
        }
        helper.succeed();
    }


    // Spec 24: 100 mB SO2 + 50 mB oxygen + 100 mB water make 100 mB acid in 40 ticks; 4 clay + 100 mB acid make alum in
    // 200; alum roasts to alumina; 2 alumina + coke dust make a 700 degree aluminium ingot for 19 200 J; water splits
    // into hydrogen and oxygen; a full hydrogen tank stops the electrolyser.
    // Spec 9.5: sneak-using a kit makes an LV machine MV in place, keeping its contents and facing and costing one
    // kit; the machine drops with machine_tier = mv and places back as MV; a battery box keeps its charge too.
    @SuppressWarnings("removal")
    private static void mvUpgrade(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlock(pos, Tier5Blocks.MACERATOR.get().defaultBlockState().setValue(ElectricMachineBlock.FACING, Direction.EAST), Block.UPDATE_ALL);
        ElectricMachineBlockEntity macerator = (ElectricMachineBlockEntity) level.getBlockEntity(pos);
        macerator.setItem(0, new ItemStack(Items.COBBLESTONE, 5));
        helper.assertValueEqual(macerator.lanes(), 1, "an LV machine runs one lane");

        ItemStack kit = new ItemStack(Tier5Items.MV_UPGRADE_KIT.get(), 2);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, kit);
        net.minecraft.world.phys.BlockHitResult hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos), Direction.UP, pos, false);
        player.setShiftKeyDown(false);
        kit.useOn(new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND, hit));
        helper.assertValueEqual(level.getBlockState(pos).getValue(ElectricMachineBlock.TIER), ElectricTier.LV, "a kit does nothing without sneaking");

        player.setShiftKeyDown(true);
        kit.useOn(new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND, hit));
        BlockState upgraded = level.getBlockState(pos);
        helper.assertValueEqual(upgraded.getValue(ElectricMachineBlock.TIER), ElectricTier.MV, "the machine is MV");
        helper.assertValueEqual(upgraded.getValue(ElectricMachineBlock.FACING), Direction.EAST, "orientation is kept");
        helper.assertTrue(level.getBlockEntity(pos) == macerator, "the same block entity stays");
        helper.assertValueEqual(macerator.getItem(0).getCount(), 5, "contents are kept");
        helper.assertValueEqual(macerator.lanes(), 2, "an MV machine runs two lanes");
        helper.assertValueEqual(kit.getCount(), 1, "one kit is spent");
        kit.useOn(new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND, hit));
        helper.assertValueEqual(kit.getCount(), 1, "an MV machine takes no second kit");

        BlockPos boxPos = pos.south(2);
        level.setBlock(boxPos, Tier5Blocks.BATTERY_BOX.get().defaultBlockState(), Block.UPDATE_ALL);
        ((BatteryBoxBlockEntity) level.getBlockEntity(boxPos)).setStored(50_000);
        net.minecraft.world.phys.BlockHitResult boxHit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(boxPos), Direction.UP, boxPos, false);
        kit.useOn(new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND, boxHit));
        BatteryBoxBlockEntity box = (BatteryBoxBlockEntity) level.getBlockEntity(boxPos);
        helper.assertValueEqual(box.tier(), ElectricTier.MV, "the battery box is MV");
        helper.assertValueEqual(box.capacity(), 400_000.0, "an MV box holds 400 000 J");
        helper.assertValueEqual(box.stored(), 50_000.0, "its charge is kept");

        BlockPos dynamoPos = pos.south(4);
        level.setBlock(dynamoPos, Tier5Blocks.KINETIC_DYNAMO.get().defaultBlockState(), Block.UPDATE_ALL);
        helper.assertTrue(!MvUpgradeKitItem.canUpgrade(level.getBlockState(dynamoPos)), "the dynamo cannot be upgraded");

        // Breaking an MV machine drops it as MV; an LV one drops plain.
        List<ItemStack> drops = Block.getDrops(upgraded, level, pos, macerator);
        helper.assertValueEqual(drops.size(), 1, "one drop");
        helper.assertValueEqual(MachineBlockItem.tierOf(drops.get(0)), ElectricTier.MV, "the drop is an MV machine");
        List<ItemStack> boxDrops = Block.getDrops(level.getBlockState(boxPos), level, boxPos, box);
        helper.assertValueEqual(MachineBlockItem.tierOf(boxDrops.get(0)), ElectricTier.MV, "the battery box drops as MV");
        helper.assertValueEqual(boxDrops.get(0).getOrDefault(Tier5DataComponents.ENERGY.get(), 0), 50_000, "and keeps its charge");
        BlockState lv = Tier5Blocks.MACERATOR.get().defaultBlockState();
        helper.assertTrue(!Block.getDrops(lv, level, pos, macerator).get(0).has(Tier5DataComponents.MACHINE_TIER.get()), "an LV machine drops without a tier");

        // Placing the dropped stack gives MV again.
        BlockPos floor = pos.south(6).below();
        level.setBlock(floor, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        ItemStack placed = drops.get(0);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, placed);
        placed.useOn(new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(floor), Direction.UP, floor, false)));
        helper.assertValueEqual(level.getBlockState(floor.above()).getValue(ElectricMachineBlock.TIER), ElectricTier.MV, "it places back as MV");

        level.getServer().getPlayerList().remove(player);
        helper.succeed();
    }

    // Spec 8.2 and 24: an MV box feeds the transformer's front, the LV box behind it is charged from the other
    // network, the transformer loses 2%, and step-up mode runs the other way.
    private static void transformer(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 1));
        BlockPos mvPos = pos.west(), lvPos = pos.east();
        level.setBlock(pos, Tier5Blocks.TRANSFORMER.get().defaultBlockState().setValue(dev.strataindustria.electric.TransformerBlock.FACING, Direction.WEST), Block.UPDATE_ALL);
        level.setBlock(mvPos, Tier5Blocks.BATTERY_BOX.get().defaultBlockState().setValue(BatteryBoxBlock.TIER, ElectricTier.MV), Block.UPDATE_ALL);
        level.setBlock(lvPos, Tier5Blocks.BATTERY_BOX.get().defaultBlockState(), Block.UPDATE_ALL);
        BatteryBoxBlockEntity mvBox = (BatteryBoxBlockEntity) level.getBlockEntity(mvPos), lvBox = (BatteryBoxBlockEntity) level.getBlockEntity(lvPos);
        dev.strataindustria.electric.TransformerBlockEntity transformer = (dev.strataindustria.electric.TransformerBlockEntity) level.getBlockEntity(pos);
        mvBox.setStored(50_000);

        ElectricNetworks.rebuildNow(level, mvPos);
        ElectricNetworks.rebuildNow(level, lvPos);
        ElectricNetwork mvNet = ElectricNetworks.networkAt(level, mvPos), lvNet = ElectricNetworks.networkAt(level, lvPos);
        helper.assertTrue(mvNet != lvNet, "the two sides are two networks");
        helper.assertTrue(mvNet.members().contains(pos) && lvNet.members().contains(pos), "the transformer is in both");
        helper.assertTrue(ElectricNetworks.networkAt(level, pos, 1) == mvNet && ElectricNetworks.networkAt(level, pos, 0) == lvNet, "front is the MV side");
        mvNet.tick();
        helper.assertTrue(Math.abs(transformer.buffer() - 128) < 1e-6, "the MV side fills the buffer with 128 J, got " + transformer.buffer());
        lvNet.tick();
        helper.assertTrue(Math.abs(lvBox.stored() - 32) < 1e-6, "the LV box takes its 32 J/t, got " + lvBox.stored());
        helper.assertTrue(Math.abs(transformer.buffer() - (128 - 32 / 0.98)) < 1e-6, "2% is lost on the way, buffer " + transformer.buffer());
        helper.assertValueEqual(lvNet.tier(), ElectricTier.LV, "the LV side is an LV network");
        helper.assertValueEqual(mvNet.tier(), ElectricTier.MV, "the MV side is an MV network");

        // Step up by clicking the block: the LV box now feeds the MV box.
        lvBox.setStored(50_000);
        mvBox.setStored(0);
        net.minecraft.world.phys.BlockHitResult hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos), Direction.UP, pos, false);
        player.setShiftKeyDown(false);
        level.getBlockState(pos).useWithoutItem(level, player, hit);
        helper.assertTrue(level.getBlockState(pos).getValue(dev.strataindustria.electric.TransformerBlock.STEP_UP), "a click steps up");
        ElectricNetworks.rebuildNow(level, mvPos);
        ElectricNetworks.rebuildNow(level, lvPos);
        mvNet = ElectricNetworks.networkAt(level, mvPos);
        lvNet = ElectricNetworks.networkAt(level, lvPos);
        lvNet.tick();
        mvNet.tick();
        // The level's own network tick may add a second round before this test's callback ends.
        double rounds = mvBox.stored() / (32 * 0.98);
        helper.assertTrue(Math.abs(rounds - Math.round(rounds)) < 1e-6 && Math.round(rounds) >= 1, "step up gives the MV box 98% of 32 J/t per tick, got " + mvBox.stored());
        helper.assertTrue(lvBox.stored() < 50_000, "and the LV box paid for it");

        level.getServer().getPlayerList().remove(player);
        helper.succeed();
    }

    // Spec 8.5 and 24: 32 J/t out is 128 FE/t; FE comes in only through the front, up to 32 J/t.
    private static void energyAdapter(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 1));
        BlockPos otherPos = pos.east(), boxPos = pos.north();
        level.setBlock(pos, Tier5Blocks.ENERGY_ADAPTER.get().defaultBlockState()
                .setValue(dev.strataindustria.electric.EnergyAdapterBlock.FACING, Direction.EAST), Block.UPDATE_ALL);
        level.setBlock(otherPos, Tier5Blocks.ENERGY_ADAPTER.get().defaultBlockState()
                .setValue(dev.strataindustria.electric.EnergyAdapterBlock.FACING, Direction.WEST), Block.UPDATE_ALL);
        level.setBlock(boxPos, Tier5Blocks.BATTERY_BOX.get().defaultBlockState(), Block.UPDATE_ALL);
        dev.strataindustria.electric.EnergyAdapterBlockEntity adapter = (dev.strataindustria.electric.EnergyAdapterBlockEntity) level.getBlockEntity(pos);
        dev.strataindustria.electric.EnergyAdapterBlockEntity other = (dev.strataindustria.electric.EnergyAdapterBlockEntity) level.getBlockEntity(otherPos);
        ((BatteryBoxBlockEntity) level.getBlockEntity(boxPos)).setStored(10_000);

        helper.assertTrue(level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Energy.BLOCK, pos, Direction.EAST) != null, "FE on the front");
        helper.assertTrue(level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Energy.BLOCK, pos, Direction.WEST) == null, "no FE on the back");
        helper.assertTrue(level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Energy.BLOCK, pos, Direction.UP) == null, "no FE on the side");

        // Out: the box feeds the adapter, which fills the other adapter's FE buffer.
        dev.strataindustria.electric.EnergyAdapterBlockEntity.serverTick(level, pos, level.getBlockState(pos), adapter);
        ElectricNetwork network = ElectricNetworks.rebuildNow(level, boxPos);
        helper.assertTrue(network.members().contains(pos) && !network.members().contains(otherPos), "the front face does not join the network");
        network.tick();
        helper.assertValueEqual(other.feHandler().getAmountAsLong(), 128L, "32 J/t gives 128 FE/t");

        // In: FE pushed into the front is taken up to 32 J/t (128 FE).
        try (net.neoforged.neoforge.transfer.transaction.Transaction transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            int taken = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Energy.BLOCK, otherPos, Direction.WEST).insert(1000, transaction);
            helper.assertValueEqual(taken, 0, "a full buffer takes nothing more");
        }
        dev.strataindustria.electric.EnergyAdapterBlockEntity.serverTick(level, otherPos, level.getBlockState(otherPos), other);
        helper.assertTrue(Math.abs(other.maxOutput() - 32) < 1e-6, "128 FE is a 32 J/t source, got " + other.maxOutput());
        try (net.neoforged.neoforge.transfer.transaction.Transaction transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            int taken = adapter.feHandler().insert(1000, transaction);
            transaction.commit();
            helper.assertValueEqual(taken, 128, "FE input is capped at 32 J/t");
        }
        helper.succeed();
    }

    private static void aluminiumChain(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos base = helper.absolutePos(new BlockPos(1, 1, 1));
        Fluid so2 = Tier4Fluids.SULFUR_DIOXIDE.get(), oxygen = Tier5Fluids.OXYGEN.source().get(), acid = Tier5Fluids.SULFURIC_ACID.source().get(),
                hydrogen = Tier5Fluids.HYDROGEN.source().get();

        BlockPos mixerPos = base;
        level.setBlock(mixerPos, Tier5Blocks.MIXER.get().defaultBlockState(), Block.UPDATE_ALL);
        MixerBlockEntity mixer = (MixerBlockEntity) level.getBlockEntity(mixerPos);
        mixer.setTank(0, so2, 100);
        mixer.setTank(1, oxygen, 50);
        mixer.setTank(2, Fluids.WATER, 100);
        helper.assertValueEqual(chemicalTicks(level, mixerPos, mixer, () -> mixer.amount(3) >= 100), 40, "acid takes 40 ticks");
        helper.assertTrue(mixer.fluid(3).isSame(acid) && mixer.amount(3) == 100, "100 mB acid");
        helper.assertTrue(mixer.amount(0) == 0 && mixer.amount(1) == 0 && mixer.amount(2) == 0, "the inputs are used up");

        mixer.setItem(0, new ItemStack(Items.CLAY_BALL, 4));
        mixer.setTank(0, acid, 100);
        helper.assertValueEqual(chemicalTicks(level, mixerPos, mixer, () -> mixer.getItem(2).is(Tier5Items.ALUM.get())), 200, "alum takes 200 ticks");
        helper.assertValueEqual(mixer.getItem(2).getCount(), 1, "one alum");
        helper.assertValueEqual(mixer.amount(0), 0, "the acid is used up");

        BlockPos furnacePos = base.south(2);
        level.setBlock(furnacePos, Tier5Blocks.ELECTRIC_FURNACE.get().defaultBlockState(), Block.UPDATE_ALL);
        ElectricFurnaceBlockEntity furnace = (ElectricFurnaceBlockEntity) level.getBlockEntity(furnacePos);
        helper.assertTrue(furnaceTicks(level, furnacePos, furnace, new ItemStack(Tier5Items.ALUM.get()), Tier5Items.ALUMINA.get()) > 0, "alum roasts to alumina");

        BlockPos cellPos = base.south(4);
        level.setBlock(cellPos, Tier5Blocks.ELECTROLYSER.get().defaultBlockState(), Block.UPDATE_ALL);
        ElectrolyserBlockEntity cell = (ElectrolyserBlockEntity) level.getBlockEntity(cellPos);
        cell.setItem(0, new ItemStack(Tier5Items.ALUMINA.get(), 2));
        cell.setItem(1, new ItemStack(Tier4Items.COKE_DUST.get()));
        helper.assertValueEqual(chemicalTicks(level, cellPos, cell, () -> cell.getItem(2).is(ModItems.ingot(Metal.ALUMINIUM))), 600, "reduction takes 600 ticks");
        helper.assertTrue(Heat.get(cell.getItem(2), level) > 650.0f, "the ingot comes out hot");
        helper.assertTrue(Math.abs(cell.stats().draw().get(ElectricTier.LV) * 600 - 19200.0) < 1e-6, "19 200 J");
        cell.getItem(2).setCount(0);

        cell.setTank(0, Fluids.WATER, 1000);
        helper.assertValueEqual(chemicalTicks(level, cellPos, cell, () -> cell.amount(1) >= 1000), 200, "water splits in 200 ticks");
        helper.assertTrue(cell.fluid(1).isSame(hydrogen) && cell.amount(2) == 500 && cell.fluid(2).isSame(oxygen), "1000 mB hydrogen and 500 mB oxygen");

        cell.setTank(0, Fluids.WATER, 1000);
        cell.setTank(1, hydrogen, 4000);
        cell.setBuffer(cell.bufferCapacity());
        ElectricMachineBlockEntity.Status status = null;
        for (int tick = 0; tick < 5; tick++) {
            cell.setBuffer(cell.bufferCapacity());
            ChemicalMachineBlockEntity.serverTick(level, cellPos, level.getBlockState(cellPos), cell);
        }
        status = cell.status();
        helper.assertValueEqual(status, ElectricMachineBlockEntity.Status.TANK_FULL, "full hydrogen tank stops it");
        helper.assertValueEqual(cell.amount(0), 1000, "the water is kept");

        // Bucket by hand, and the acid only goes where a recipe wants it.
        helper.assertTrue(mixer.fill(Direction.UP, acid, 1000, 0, true) == 1000, "acid goes in the mixer");
        helper.assertTrue(mixer.fill(Direction.UP, hydrogen, 1000, 0, true) == 0, "hydrogen does not");
        helper.succeed();
    }

    // Spec 10.7: shapeless matching in any slot; a circuit board, 2 red alloy wire and 2 redstone make 2 basic circuits in
    // 200 ticks; a lead-acid cell takes 250 mB acid in 100; MV runs two at once in half the ticks; a missing
    // ingredient or too little acid does nothing; a full output stops it.
    private static void assembler(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlock(pos, Tier5Blocks.ASSEMBLER.get().defaultBlockState(), Block.UPDATE_ALL);
        AssemblerBlockEntity machine = (AssemblerBlockEntity) level.getBlockEntity(pos);
        int out = machine.layout().itemInputs();

        machine.setItem(4, new ItemStack(Items.REDSTONE, 2));
        machine.setItem(1, new ItemStack(Tier5Items.RED_ALLOY_WIRE.get(), 2));
        helper.assertValueEqual(chemicalTicks(level, pos, machine, () -> machine.getItem(out).getCount() > 0), -1, "no circuit board, nothing happens");
        machine.setItem(5, new ItemStack(Tier5Items.CIRCUIT_BOARD.get()));
        helper.assertValueEqual(chemicalTicks(level, pos, machine, () -> machine.getItem(out).is(Tier5Items.BASIC_CIRCUIT.get())), 200, "circuits take 200 ticks");
        helper.assertValueEqual(machine.getItem(out).getCount(), 2, "two circuits");
        helper.assertTrue(machine.getItem(1).isEmpty() && machine.getItem(4).isEmpty() && machine.getItem(5).isEmpty(), "the inputs are used up");
        machine.setItem(out, ItemStack.EMPTY);

        machine.setItem(0, new ItemStack(Tier5Items.LEAD_PLATE.get(), 2));
        machine.setItem(3, new ItemStack(Tier5Items.COPPER_WIRE.get()));
        machine.setTank(0, Tier5Fluids.SULFURIC_ACID.source().get(), 200);
        helper.assertValueEqual(chemicalTicks(level, pos, machine, () -> machine.getItem(out).getCount() > 0), -1, "200 mB of acid is not enough");
        machine.setTank(0, Tier5Fluids.SULFURIC_ACID.source().get(), 1000);
        helper.assertValueEqual(chemicalTicks(level, pos, machine, () -> machine.getItem(out).is(Tier5Items.LEAD_ACID_CELL.get())), 100, "a cell takes 100 ticks");
        helper.assertValueEqual(machine.amount(0), 750, "a cell takes 250 mB acid");
        helper.assertTrue(Math.abs(machine.stats().draw().get(ElectricTier.LV) * 100 - 1600.0) < 1e-6, "1600 J per cell");

        // A full output stops it.
        machine.setItem(out, new ItemStack(Tier5Items.LEAD_ACID_CELL.get(), 64));
        machine.setItem(0, new ItemStack(Tier5Items.LEAD_PLATE.get(), 2));
        machine.setItem(3, new ItemStack(Tier5Items.COPPER_WIRE.get()));
        machine.setBuffer(machine.bufferCapacity());
        ChemicalMachineBlockEntity.serverTick(level, pos, level.getBlockState(pos), machine);
        helper.assertValueEqual(machine.status(), ElectricMachineBlockEntity.Status.OUTPUT_FULL, "a full output stops it");
        for (int i = 0; i < machine.getContainerSize(); i++) machine.setItem(i, ItemStack.EMPTY);
        machine.setTank(0, Fluids.EMPTY, 0);

        // MV: two at once in half the ticks.
        level.setBlock(pos, level.getBlockState(pos).setValue(ElectricMachineBlock.TIER, ElectricTier.MV), Block.UPDATE_ALL);
        machine.setItem(0, new ItemStack(ModItems.RODS.get(Metal.STEEL).get(), 2));
        machine.setItem(1, new ItemStack(Tier5Items.COPPER_WIRE.get(), 16));
        helper.assertValueEqual(chemicalTicks(level, pos, machine, () -> machine.getItem(out).is(Tier5Items.MAGNET.get())), 200, "MV halves the 400 ticks");
        helper.assertValueEqual(machine.getItem(out).getCount(), 2, "and makes two magnets");
        helper.succeed();
    }

    private static int chemicalTicks(ServerLevel level, BlockPos pos, ChemicalMachineBlockEntity machine, java.util.function.BooleanSupplier done) {
        for (int tick = 1; tick <= 2000; tick++) {
            machine.setBuffer(machine.bufferCapacity());
            ChemicalMachineBlockEntity.serverTick(level, pos, level.getBlockState(pos), machine);
            if (done.getAsBoolean()) return tick;
        }
        return -1;
    }

    /** Ticks {@code machine} takes to put {@code result} in its output on a full buffer; -1 if it never does. */
    private static int shapingTicks(ServerLevel level, BlockPos pos, ElectricMachineBlockEntity machine, ElectricMachineLayout layout, Item result) {
        for (int tick = 1; tick <= 1000; tick++) {
            machine.setBuffer(machine.bufferCapacity());
            ElectricMachineBlockEntity.serverTick(level, pos, level.getBlockState(pos), machine);
            if (machine.getItem(layout.outputSlot(0, 0)).is(result)) return tick;
        }
        return -1;
    }

    // Spec 7.2 and 24: a bronze boiler's 15 mB/t at 3 bar settles at 30 J/t after a 200 tick spin-up; with no
    // demand it takes no steam; 1.5 bar halves the output; below 1 bar it takes nothing and spins down.
    private static void steamTurbine(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2)), boxPos = pos.east();
        level.setBlock(pos, Tier5Blocks.STEAM_TURBINE.get().defaultBlockState().setValue(GeneratorBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);
        level.setBlock(boxPos, Tier5Blocks.BATTERY_BOX.get().defaultBlockState(), Block.UPDATE_ALL);
        SteamTurbineBlockEntity turbine = (SteamTurbineBlockEntity) level.getBlockEntity(pos);
        BatteryBoxBlockEntity box = (BatteryBoxBlockEntity) level.getBlockEntity(boxPos);
        ElectricNetwork network = ElectricNetworks.rebuildNow(level, pos);
        Fluid steam = Tier4Fluids.STEAM.get();
        helper.assertValueEqual(turbine.fill(Direction.WEST, steam, 15, 3.0f, true), 0, "steam only goes in at the back");
        int[] drawn = {0};
        for (int tick = 0; tick < SteamTurbineBlockEntity.RISE_TICKS + 20; tick++) {
            drawn[0] = turbine.fill(Direction.SOUTH, steam, 15, 3.0f, false);
            turbine.tick(level, pos, level.getBlockState(pos));
            network.tick();
        }
        helper.assertTrue(turbine.spin() >= 1.0f, "full spin after 200 ticks, got " + turbine.spin());
        helper.assertTrue(network.report(pos).drawn() > 29.0 && network.report(pos).drawn() <= 30.0, "30 J/t on 15 mB/t, got " + network.report(pos).drawn());
        helper.assertValueEqual(level.getBlockState(pos).getValue(GeneratorBlock.STATUS), StatusLight.RUN, "green lamp");
        // Full box: no demand, so no steam taken beyond the buffer.
        box.setStored(box.capacity());
        helper.assertValueEqual(turbine.fill(Direction.SOUTH, steam, 15, 3.0f, true) >= 0, true, "buffer query works");
        for (int tick = 0; tick < 5; tick++) network.tick();
        helper.assertTrue(turbine.fill(Direction.SOUTH, steam, 100, 3.0f, false) <= 32, "buffer is two ticks of full use");
        // 1.5 bar: half output.
        turbine.fill(Direction.SOUTH, steam, 1, 1.5f, false);
        helper.assertTrue(turbine.maxOutput() <= 16.0, "1 to 2 bar caps an LV turbine at 16 J/t, got " + turbine.maxOutput());
        // Below 1 bar: nothing in, spins down.
        helper.assertValueEqual(turbine.fill(Direction.SOUTH, steam, 15, 0.5f, false), 0, "no steam below 1 bar");
        for (int tick = 0; tick < SteamTurbineBlockEntity.FALL_TICKS + 10; tick++) {
            turbine.fill(Direction.SOUTH, steam, 15, 0.5f, false);
            turbine.tick(level, pos, level.getBlockState(pos));
        }
        helper.assertTrue(turbine.spin() <= 0.0f, "spun down after 100 ticks, got " + turbine.spin());
        helper.assertValueEqual(level.getBlockState(pos).getValue(GeneratorBlock.ACTIVE), false, "rotor stops with the steam");
        helper.succeed();
    }

    // Spec 7.3 and 24: 16 000 mB of creosote gives 128 000 J at 32 J/t; an empty tank gives nothing.
    private static void combustionGenerator(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2)), boxPos = pos.east();
        level.setBlock(pos, Tier5Blocks.COMBUSTION_GENERATOR.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(boxPos, Tier5Blocks.BATTERY_BOX.get().defaultBlockState(), Block.UPDATE_ALL);
        CombustionGeneratorBlockEntity generator = (CombustionGeneratorBlockEntity) level.getBlockEntity(pos);
        BatteryBoxBlockEntity box = (BatteryBoxBlockEntity) level.getBlockEntity(boxPos);
        ElectricNetwork network = ElectricNetworks.rebuildNow(level, pos);
        Fluid creosote = Tier4Fluids.CREOSOTE.get();
        helper.assertValueEqual(generator.fill(Direction.UP, Fluids.WATER, 1000, 0, false), 0, "water does not burn");
        helper.assertValueEqual(generator.fill(Direction.UP, creosote, 20_000, 0, false), 8000, "the tank holds 8000 mB");
        helper.assertValueEqual(generator.fill(Direction.DOWN, creosote, 1000, 0, false), 0, "and no more");
        for (int tick = 0; tick < 100; tick++) network.tick();
        helper.assertTrue(Math.abs(box.stored() - 3200) < 1.0, "100 ticks at 32 J/t, got " + box.stored());
        helper.assertTrue(Math.abs(generator.amount() - 7600) <= 1, "4 mB per tick, got " + generator.amount());
        generator.tick(level, pos, level.getBlockState(pos));
        helper.assertValueEqual(level.getBlockState(pos).getValue(GeneratorBlock.STATUS), StatusLight.RUN, "green lamp");
        // Run it dry: the whole tank is worth 64 000 J.
        for (int tick = 0; tick < 3000; tick++) network.tick();
        box.setStored(0);
        helper.assertTrue(generator.amount() == 0, "tank empty, got " + generator.amount());
        for (int tick = 0; tick < 10; tick++) network.tick();
        helper.assertTrue(box.stored() == 0, "an empty generator gives nothing");
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
