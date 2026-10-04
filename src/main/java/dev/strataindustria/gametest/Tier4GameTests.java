package dev.strataindustria.gametest;

import com.mojang.authlib.GameProfile;
import dev.strataindustria.coking.CokeOvenBlock;
import dev.strataindustria.coking.CokeOvenBlockEntity;
import dev.strataindustria.coking.CokeOvenStructure;
import dev.strataindustria.fluid.FluidPipes;
import dev.strataindustria.fluid.PressureGaugeBlock;
import dev.strataindustria.forge.ForgeBlock;
import dev.strataindustria.forge.ForgeBlockEntity;
import dev.strataindustria.geology.OreGrade;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.machine.BellowsBlock;
import dev.strataindustria.power.AxleBlock;
import dev.strataindustria.power.HandCrankBlock;
import dev.strataindustria.power.HandCrankBlockEntity;
import dev.strataindustria.power.Kinetic;
import dev.strataindustria.power.KineticNetworks;
import dev.strataindustria.power.KineticState;
import dev.strataindustria.power.StepUpGearboxBlock;
import dev.strataindustria.machine.BellowsBlockEntity;
import dev.strataindustria.material.Metal;
import dev.strataindustria.metal.Alloy;
import dev.strataindustria.metal.CastMoldItem;
import dev.strataindustria.metal.CrucibleBlockEntity;
import dev.strataindustria.metal.Melt;
import dev.strataindustria.metal.MetalContent;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModDataComponents;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.Tier4Blocks;
import dev.strataindustria.registry.Tier4Fluids;
import dev.strataindustria.registry.Tier4Items;
import dev.strataindustria.steam.BoilerBlockEntity;
import dev.strataindustria.steam.FireboxBlockEntity;
import dev.strataindustria.processing.CrusherBlockEntity;
import dev.strataindustria.processing.ProcessingBlock;
import dev.strataindustria.processing.ProcessingBlockEntity;
import dev.strataindustria.processing.WasherBlockEntity;
import dev.strataindustria.steam.MechanicalPumpBlock;
import dev.strataindustria.steam.MechanicalPumpBlockEntity;
import dev.strataindustria.steam.SteamEngineBlock;
import dev.strataindustria.steam.SteamEngineBlockEntity;
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
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayer;

/** Tier 4 (steel and steam) tests, run by {@link ModGameTests}. */
final class Tier4GameTests {
    private Tier4GameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("tier4_alloy_rules", Tier4GameTests::alloyRules);
        tests.put("tier4_coke_oven", Tier4GameTests::cokeOven);
        tests.put("tier4_crucible_steel", Tier4GameTests::crucibleSteel);
        tests.put("tier4_zinc_and_brass", Tier4GameTests::zincAndBrass);
        tests.put("tier4_iron_transmission", Tier4GameTests::ironTransmission);
        tests.put("tier4_boiler_raises_steam", Tier4GameTests::boilerRaisesSteam);
        tests.put("tier4_boiler_dry_firing", Tier4GameTests::boilerDryFiring);
        tests.put("tier4_fluid_pipes", Tier4GameTests::fluidPipes);
        tests.put("tier4_steam_engine", Tier4GameTests::steamEngine);
        tests.put("tier4_mechanical_pump", Tier4GameTests::mechanicalPump);
        tests.put("tier4_crusher", Tier4GameTests::crusher);
        tests.put("tier4_washer", Tier4GameTests::washer);
        tests.put("tier4_blast_furnace", Tier4GameTests::blastFurnace);
        tests.put("tier4_converter", Tier4GameTests::converter);
        tests.put("tier4_heat_pipes", Tier4GameTests::heatPipes);
        tests.put("tier4_hot_blast", Tier4GameTests::hotBlast);
        tests.put("tier4_converter_preheat", Tier4GameTests::converterPreheat);
        tests.put("tier4_kiln", Tier4GameTests::kiln);
        tests.put("tier4_roaster", Tier4GameTests::roaster);
        tests.put("tier4_smelter", Tier4GameTests::smelter);
        tests.put("tier4_steam_hammer", Tier4GameTests::steamHammer);
    }

    // Spec 4.3: the example batches for steel, pig iron, brass and solder, and the gap between steel and pig iron.
    private static void alloyRules(GameTestHelper helper) {
        Melt carbon = new Melt(Map.of(Metal.CARBON, 5), 0);
        Melt steel = melt(Items.IRON_INGOT, 5).plus(carbon);
        helper.assertValueEqual(Alloy.resultOf(steel).orElse(null), Metal.STEEL, "5 iron ingots + 5 carbon");

        Melt coFusion = melt(ModItems.ingot(Metal.PIG_IRON), 1).plus(melt(Items.IRON_INGOT, 3));
        helper.assertValueEqual(Alloy.resultOf(coFusion).orElse(null), Metal.STEEL, "1 pig iron + 3 iron ingots");

        helper.assertValueEqual(Alloy.resultOf(melt(ModItems.ingot(Metal.PIG_IRON), 2)).orElse(null), Metal.PIG_IRON, "pig iron remelt");
        helper.assertValueEqual(Alloy.resultOf(melt(ModItems.ingot(Metal.STEEL), 3)).orElse(null), Metal.STEEL, "steel remelt");
        helper.assertValueEqual(Alloy.resultOf(melt(Items.IRON_INGOT, 4)).orElse(null), Metal.WROUGHT_IRON, "iron remelt");

        Melt gap = new Melt(Map.of(Metal.WROUGHT_IRON, 974, Metal.CARBON, 26), 0);
        helper.assertTrue(Alloy.resultOf(gap).isEmpty(), "iron with 2.6% carbon should be no known alloy");

        Melt brass = melt(Items.COPPER_INGOT, 2).plus(melt(ModItems.ingot(Metal.ZINC), 1));
        helper.assertValueEqual(Alloy.resultOf(brass).orElse(null), Metal.BRASS, "2 copper + 1 zinc");
        Melt solder = melt(ModItems.NUGGETS.get(Metal.TIN).get(), 6).plus(melt(ModItems.NUGGETS.get(Metal.LEAD).get(), 4));
        helper.assertValueEqual(Alloy.resultOf(solder).orElse(null), Metal.SOLDER, "6 tin + 4 lead nuggets");

        // Spec 4.4: galena melts like the tier 2 ores; sphalerite has to be roasted first.
        helper.assertValueEqual(Alloy.resultOf(melt(ModItems.crushedOre(OreMineral.GALENA, OreGrade.NORMAL), 2)).orElse(null),
                Metal.LEAD, "crushed galena");
        helper.assertTrue(MetalContent.of(new ItemStack(ModItems.crushedOre(OreMineral.SPHALERITE, OreGrade.NORMAL))).isEmpty(),
                "sphalerite should not melt");
        helper.succeed();
    }

    // Spec 5.1 and 22: the oven forms only when whole and hollow; 16 coal give 16 coke and 4000 mB of
    // creosote in 16 x 1800 ticks; a bucket draws 1000 mB; a full tank pauses it.
    private static void cokeOven(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos chamber = helper.absolutePos(new BlockPos(4, 2, 4));
        BlockPos door = chamber.south();
        for (int dx = -1; dx <= 1; dx++)
            for (int dy = -1; dy <= 1; dy++)
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos pos = chamber.offset(dx, dy, dz);
                    if (!pos.equals(chamber) && !pos.equals(door)) level.setBlock(pos, Tier4Blocks.COKE_OVEN_BRICKS.get().defaultBlockState(), Block.UPDATE_ALL);
                }
        level.setBlock(door, Tier4Blocks.COKE_OVEN_DOOR.get().defaultBlockState().setValue(CokeOvenBlock.FACING, Direction.SOUTH), Block.UPDATE_ALL);
        CokeOvenBlockEntity oven = (CokeOvenBlockEntity) level.getBlockEntity(door);
        helper.assertTrue(oven.checkStructure().complete(), "a whole oven should form, got " + oven.checkStructure());

        BlockPos corner = chamber.offset(1, -1, -1);
        level.removeBlock(corner, false);
        helper.assertValueEqual(oven.checkStructure(), new CokeOvenStructure.Result(CokeOvenStructure.Problem.NEEDS_BRICK, corner), "missing brick");
        level.setBlock(corner, Tier4Blocks.COKE_OVEN_BRICKS.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(chamber, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        helper.assertValueEqual(oven.checkStructure(), new CokeOvenStructure.Result(CokeOvenStructure.Problem.NEEDS_AIR, chamber), "filled chamber");
        level.removeBlock(chamber, false);
        helper.assertTrue(oven.checkStructure().complete(), "the oven should form again");

        oven.setItem(CokeOvenBlockEntity.INPUT, new ItemStack(Items.COAL, 16));
        run(level, door, oven, 16 * 1800);
        ItemStack coke = oven.getItem(CokeOvenBlockEntity.OUTPUT);
        helper.assertTrue(coke.is(Tier4Items.COKE.get()) && coke.getCount() == 16, "16 coal should give 16 coke, got " + coke);
        helper.assertValueEqual(oven.creosote(), 4000, "creosote from 16 coal");
        helper.assertValueEqual(oven.status(), CokeOvenBlockEntity.Status.EMPTY, "status once the coal is gone");
        helper.assertTrue(!level.getBlockState(door).getValue(CokeOvenBlock.LIT), "the door goes dark when the oven is empty");

        oven.setItem(CokeOvenBlockEntity.BUCKET_IN, new ItemStack(Items.BUCKET));
        run(level, door, oven, 1);
        helper.assertTrue(oven.getItem(CokeOvenBlockEntity.BUCKET_OUT).is(Tier4Items.CREOSOTE_BUCKET.get()), "a bucket should be filled");
        helper.assertValueEqual(oven.creosote(), 3000, "creosote after one bucket");

        // 3000 + 52 x 250 = 16000 mB fills the tank exactly; the 53rd coal has to wait.
        oven.setItem(CokeOvenBlockEntity.OUTPUT, ItemStack.EMPTY);
        oven.setItem(CokeOvenBlockEntity.INPUT, new ItemStack(Items.COAL, 53));
        run(level, door, oven, 53 * 1800);
        helper.assertValueEqual(oven.creosote(), CokeOvenBlockEntity.CAPACITY, "a full tank");
        helper.assertValueEqual(oven.status(), CokeOvenBlockEntity.Status.TANK_FULL, "status with a full tank");
        helper.assertValueEqual(oven.getItem(CokeOvenBlockEntity.INPUT).getCount(), 1, "the last coal waits");
        helper.succeed();
    }

    // Spec 3, 4.2 and 6: only the refractory crucible takes iron, no crucible takes iron ore, clay molds
    // refuse steel, and five iron ingots with one coke dust melt on a blown coke forge into steel.
    private static void crucibleSteel(GameTestHelper helper) {
        ItemStack iron = new ItemStack(Items.IRON_INGOT);
        helper.assertTrue(!CrucibleBlockEntity.accepts(iron, false), "a clay crucible should refuse iron");
        helper.assertTrue(CrucibleBlockEntity.accepts(iron, true), "a refractory crucible should take iron");
        ItemStack ore = new ItemStack(ModItems.crushedOre(OreMineral.HEMATITE, OreGrade.NORMAL));
        helper.assertTrue(!CrucibleBlockEntity.accepts(ore, true), "no crucible should take iron ore");
        helper.assertTrue(CrucibleBlockEntity.accepts(new ItemStack(Tier4Items.COKE_DUST.get()), false), "coke dust goes in any crucible");
        helper.assertTrue(!Tier4Items.GEAR_MOLD.get().takes(Metal.STEEL.meltingPoint()), "a clay mold should refuse steel");
        helper.assertTrue(Tier4Items.GEAR_MOLD.get().takes(Metal.BRASS.meltingPoint()), "a clay mold takes brass");

        ServerLevel level = helper.getLevel();
        BlockPos forgePos = helper.absolutePos(new BlockPos(4, 1, 4));
        BlockPos cruciblePos = forgePos.above();
        BlockPos bellowsPos = forgePos.west();
        level.setBlock(forgePos, ModBlocks.FORGE.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(cruciblePos, Tier4Blocks.REFRACTORY_CRUCIBLE.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(bellowsPos, ModBlocks.BELLOWS.get().defaultBlockState().setValue(BellowsBlock.FACING, Direction.EAST), Block.UPDATE_ALL);
        ForgeBlockEntity forge = (ForgeBlockEntity) level.getBlockEntity(forgePos);
        CrucibleBlockEntity crucible = (CrucibleBlockEntity) level.getBlockEntity(cruciblePos);
        BellowsBlockEntity bellows = (BellowsBlockEntity) level.getBlockEntity(bellowsPos);
        helper.assertTrue(crucible.refractory(), "the block entity should be a refractory crucible");
        helper.assertValueEqual(crucible.capacityOf(), CrucibleBlockEntity.REFRACTORY_CAPACITY, "refractory capacity");

        forge.setItem(ForgeBlockEntity.FUEL_SLOT, new ItemStack(Tier4Items.COKE.get(), 16));
        BlockState forgeState = level.getBlockState(forgePos);
        helper.assertTrue(((ForgeBlock) forgeState.getBlock()).ignite(level, forgePos, forgeState), "the forge should light");
        crucible.setItem(0, new ItemStack(Items.IRON_INGOT, 5));
        crucible.setItem(1, new ItemStack(Tier4Items.COKE_DUST.get()));
        FakePlayer smith = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "smith"));
        for (int tick = 0; tick < 20000; tick++) {
            bellows.pump(smith);
            BellowsBlockEntity.serverTick(level, bellowsPos, level.getBlockState(bellowsPos), bellows);
            ForgeBlockEntity.serverTick(level, forgePos, level.getBlockState(forgePos), forge);
            CrucibleBlockEntity.serverTick(level, cruciblePos, level.getBlockState(cruciblePos), crucible);
            if (crucible.getItem(0).isEmpty() && crucible.getItem(1).isEmpty() && crucible.isMolten()) break;
        }
        helper.assertTrue(forge.temperature() > 1538, "a blown coke forge should pass 1538 °C, got " + forge.temperature());
        helper.assertTrue(crucible.getItem(1).isEmpty(), "the coke dust should dissolve into the molten iron");
        helper.assertValueEqual(crucible.melt().total(), 505, "units of iron and carbon");
        helper.assertValueEqual(crucible.result().orElse(null), Metal.STEEL, "melt result");

        crucible.setItem(CrucibleBlockEntity.MOLD_SLOT, new ItemStack(Tier4Items.GEAR_MOLD.get()));
        helper.assertValueEqual(crucible.pourProblem().orElse(""), "mold_too_weak", "steel into a clay mold");
        crucible.setItem(CrucibleBlockEntity.MOLD_SLOT, new ItemStack(Tier4Items.REFRACTORY_INGOT_MOLD.get()));
        helper.assertTrue(crucible.startPour(), "steel should pour into a refractory mold, problem: " + crucible.pourProblem().orElse(""));
        for (int tick = 0; tick < 40; tick++) {
            ForgeBlockEntity.serverTick(level, forgePos, level.getBlockState(forgePos), forge);
            CrucibleBlockEntity.serverTick(level, cruciblePos, level.getBlockState(cruciblePos), crucible);
        }
        Melt cast = crucible.getItem(CrucibleBlockEntity.MOLD_SLOT).get(ModDataComponents.CAST_CONTENTS.get());
        helper.assertTrue(cast != null && cast.total() == 100, "the mold should hold 100 units, got " + cast);
        helper.assertValueEqual(CastMoldItem.castMetal(cast), Metal.STEEL, "cast metal");
        helper.succeed();
    }

    // Spec 5.3, 4.2, 7 and 23: crushed sphalerite roasts to calcine in a forge slot; 3 calcine with 2 coke
    // dust does not fully reduce and says so; a third dust finishes it and the spare carbon burns off;
    // two copper ingots in the zinc make brass.
    private static void zincAndBrass(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos forgePos = helper.absolutePos(new BlockPos(4, 1, 4));
        level.setBlock(forgePos, ModBlocks.FORGE.get().defaultBlockState(), Block.UPDATE_ALL);
        ForgeBlockEntity forge = (ForgeBlockEntity) level.getBlockEntity(forgePos);
        forge.setItem(ForgeBlockEntity.FUEL_SLOT, new ItemStack(Items.CHARCOAL, 16));
        BlockState forgeState = level.getBlockState(forgePos);
        helper.assertTrue(((ForgeBlock) forgeState.getBlock()).ignite(level, forgePos, forgeState), "the forge should light");
        for (int tick = 0; tick < 4000 && forge.temperature() < 1300; tick++) {
            ForgeBlockEntity.serverTick(level, forgePos, level.getBlockState(forgePos), forge);
        }
        helper.assertTrue(forge.temperature() >= 1300, "a charcoal forge should pass 1300 °C, got " + forge.temperature());

        ItemStack crushed = new ItemStack(ModItems.crushedOre(OreMineral.SPHALERITE, OreGrade.NORMAL), 2);
        forge.setItem(ForgeBlockEntity.FIRST_HEAT_SLOT, crushed);
        long now = level.getGameTime();
        for (int step = 0; step < 400 && !forge.getItem(ForgeBlockEntity.FIRST_HEAT_SLOT).is(Tier4Items.zincCalcine(OreGrade.NORMAL)); step++) {
            now += 10;
            forge.heatItems(level, now);
        }
        ItemStack calcine = forge.getItem(ForgeBlockEntity.FIRST_HEAT_SLOT);
        helper.assertTrue(calcine.is(Tier4Items.zincCalcine(OreGrade.NORMAL)) && calcine.getCount() == 2,
                "two crushed sphalerite should roast to two zinc calcine, got " + calcine);

        BlockPos cruciblePos = forgePos.above();
        level.setBlock(cruciblePos, ModBlocks.CRUCIBLE.get().defaultBlockState(), Block.UPDATE_ALL);
        CrucibleBlockEntity crucible = (CrucibleBlockEntity) level.getBlockEntity(cruciblePos);
        helper.assertTrue(CrucibleBlockEntity.accepts(calcine, false), "a clay crucible takes calcine");
        crucible.setItem(0, new ItemStack(Tier4Items.zincCalcine(OreGrade.NORMAL), 3));
        crucible.setItem(1, new ItemStack(Tier4Items.COKE_DUST.get(), 2));
        heat(level, forgePos, forge, cruciblePos, crucible, () -> crucible.data().get(CrucibleBlockEntity.DATA_STATUS)
                == dev.strataindustria.metal.CrucibleStatus.CALCINE_SHORT.ordinal());
        helper.assertValueEqual(crucible.getItem(0).getCount(), 1, "calcine left waiting for carbon (needs 12, has 10)");
        helper.assertValueEqual(crucible.melt().units().getOrDefault(Metal.ZINC, 0), 70, "zinc from two reduced calcine");
        helper.assertValueEqual(crucible.melt().units().getOrDefault(Metal.CARBON, 0), 2, "carbon kept for the last calcine");

        crucible.setItem(1, new ItemStack(Tier4Items.COKE_DUST.get()));
        heat(level, forgePos, forge, cruciblePos, crucible, () -> crucible.getItem(0).isEmpty() && crucible.getItem(1).isEmpty()
                && !crucible.melt().units().containsKey(Metal.CARBON));
        helper.assertValueEqual(crucible.melt().total(), 105, "zinc from three normal calcine, spare carbon burned off");
        helper.assertValueEqual(crucible.result().orElse(null), Metal.ZINC, "calcine and dust pour zinc");

        crucible.setItem(0, new ItemStack(Items.COPPER_INGOT, 2));
        heat(level, forgePos, forge, cruciblePos, crucible, () -> crucible.getItem(0).isEmpty() && crucible.isMolten());
        helper.assertValueEqual(crucible.result().orElse(null), Metal.BRASS, "105 zinc + 200 copper");
        helper.succeed();
    }

    // Spec 11.7 and 23: three iron step-up gearboxes take a 16 RPM crank to 128 RPM on an iron axle;
    // a wooden axle in the same place stops with "Overspeed".
    private static void ironTransmission(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos crankPos = helper.absolutePos(new BlockPos(4, 1, 1));
        level.setBlock(crankPos, ModBlocks.HAND_CRANK.get().defaultBlockState().setValue(HandCrankBlock.FACING, Direction.SOUTH),
                Block.UPDATE_ALL);
        BlockPos pos = crankPos;
        for (int i = 0; i < 3; i++) {
            pos = pos.south();
            level.setBlock(pos, Tier4Blocks.IRON_STEP_UP_GEARBOX.get().defaultBlockState().setValue(StepUpGearboxBlock.FACING, Direction.SOUTH),
                    Block.UPDATE_ALL);
        }
        BlockPos axlePos = pos.south();
        level.setBlock(axlePos, Tier4Blocks.IRON_AXLE.get().defaultBlockState().setValue(AxleBlock.AXIS, Direction.Axis.Z), Block.UPDATE_ALL);
        HandCrankBlockEntity crank = (HandCrankBlockEntity) level.getBlockEntity(crankPos);
        crank.crank(new FakePlayer(level, new GameProfile(UUID.randomUUID(), "engineer")));
        KineticNetworks.rebuildNow(level, axlePos);
        Kinetic axle = (Kinetic) level.getBlockEntity(axlePos);
        helper.assertValueEqual(Math.round(axle.kinetic().rpm()), 128, "iron axle RPM behind three iron step-up gearboxes");
        helper.assertValueEqual(axle.kinetic().status(), KineticState.Status.RUNNING, "iron axle status");

        level.setBlock(axlePos, ModBlocks.WOODEN_AXLE.get().defaultBlockState().setValue(AxleBlock.AXIS, Direction.Axis.Z), Block.UPDATE_ALL);
        crank.crank(new FakePlayer(level, new GameProfile(UUID.randomUUID(), "engineer")));
        KineticNetworks.rebuildNow(level, axlePos);
        Kinetic wooden = (Kinetic) level.getBlockEntity(axlePos);
        helper.assertValueEqual(wooden.kinetic().status(), KineticState.Status.OVERSPEED, "a wooden axle at 128 RPM");
        helper.succeed();
    }

    // Spec 8.1, 10.1, 10.2 and 10.4: a coal firebox under a full bronze boiler. The water takes
    // 600 HU per bucket (4800 HU, 160 ticks at 30 HU/t) to warm, then makes 15 mB of steam a tick
    // until the 4000 mB buffer is full at 4 bar, and with nothing taking steam the safety valve vents.
    private static void boilerRaisesSteam(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos fireboxPos = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos boilerPos = fireboxPos.above();
        level.setBlock(fireboxPos, Tier4Blocks.FIREBOX.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(boilerPos, Tier4Blocks.BRONZE_BOILER.get().defaultBlockState(), Block.UPDATE_ALL);
        FireboxBlockEntity firebox = (FireboxBlockEntity) level.getBlockEntity(fireboxPos);
        BoilerBlockEntity boiler = (BoilerBlockEntity) level.getBlockEntity(boilerPos);
        firebox.setItem(0, new ItemStack(Items.COAL, 4));
        boiler.prime(BoilerBlockEntity.WATER_CAPACITY, false);

        // From cold the fire climbs 5 °C a second; the boiler takes nothing below 300 °C.
        steam(level, fireboxPos, firebox, boilerPos, boiler, 200);
        helper.assertTrue(firebox.burning(), "the firebox should light its coal");
        helper.assertValueEqual(boiler.status(), BoilerBlockEntity.Status.TOO_COOL, "boiler status on a firebox under 300 °C");
        helper.assertValueEqual(firebox.status(), FireboxBlockEntity.Status.IDLE, "firebox status while the boiler refuses its heat");

        firebox.preheat(1400.0f);
        steam(level, fireboxPos, firebox, boilerPos, boiler, 150);
        helper.assertValueEqual(boiler.status(), BoilerBlockEntity.Status.HEATING, "boiler status during warm-up");
        helper.assertValueEqual(boiler.steam(), 0, "steam during warm-up");
        helper.assertValueEqual(firebox.taken(), 30, "HU/t the boiler takes from a coal firebox");

        steam(level, fireboxPos, firebox, boilerPos, boiler, 30);
        helper.assertValueEqual(boiler.status(), BoilerBlockEntity.Status.RUNNING, "boiler status once warm");
        helper.assertTrue(boiler.steam() > 0 && boiler.steam() % 15 == 0, "steam comes 15 mB a tick, got " + boiler.steam());

        steam(level, fireboxPos, firebox, boilerPos, boiler, 300);
        helper.assertValueEqual(boiler.steam(), BoilerBlockEntity.STEAM_CAPACITY, "steam buffer when nothing takes steam");
        helper.assertValueEqual(boiler.pressure(), BoilerBlockEntity.RATED_PRESSURE, "pressure of a full buffer");
        helper.assertValueEqual(boiler.status(), BoilerBlockEntity.Status.VENTING, "boiler status with a full buffer");
        helper.assertTrue(boiler.water() < BoilerBlockEntity.WATER_CAPACITY - BoilerBlockEntity.STEAM_CAPACITY,
                "venting still boils water away, " + boiler.water() + " mB left");
        helper.succeed();
    }

    // Spec 10.4: below 10% water with the fire lit the shell loses 1% a second; water on it then bursts
    // to steam for 20%; a bronze plate patches 10%; at zero it cracks.
    private static void boilerDryFiring(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos fireboxPos = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos boilerPos = fireboxPos.above();
        level.setBlock(fireboxPos, Tier4Blocks.FIREBOX.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(boilerPos, Tier4Blocks.BRONZE_BOILER.get().defaultBlockState(), Block.UPDATE_ALL);
        FireboxBlockEntity firebox = (FireboxBlockEntity) level.getBlockEntity(fireboxPos);
        BoilerBlockEntity boiler = (BoilerBlockEntity) level.getBlockEntity(boilerPos);
        firebox.setItem(0, new ItemStack(Tier4Items.COKE.get(), 8));
        firebox.preheat(1500.0f);
        boiler.prime(400, true);

        steam(level, fireboxPos, firebox, boilerPos, boiler, 100);
        helper.assertValueEqual(boiler.status(), BoilerBlockEntity.Status.DRY_FIRING, "boiler status at 5% water");
        float afterFiring = boiler.integrity();
        helper.assertTrue(afterFiring > 94.0f && afterFiring < 96.0f, "integrity after 5 s of dry firing, got " + afterFiring);

        int taken = boiler.fill(Direction.NORTH, net.minecraft.world.level.material.Fluids.WATER, 1000, 0, false);
        helper.assertValueEqual(taken, 1000, "water a pipe can add");
        helper.assertTrue(Math.abs(boiler.integrity() - (afterFiring - BoilerBlockEntity.BURST_DAMAGE)) < 0.01f, "integrity after the steam burst");
        helper.assertValueEqual(boiler.fill(Direction.UP, net.minecraft.world.level.material.Fluids.WATER, 1000, 0, true), 0,
                "the top face is the steam outlet and takes no water");

        float before = boiler.integrity();
        helper.assertTrue(boiler.repair(), "a damaged boiler takes a plate");
        helper.assertTrue(Math.abs(boiler.integrity() - (before + BoilerBlockEntity.PLATE_REPAIR)) < 0.01f, "integrity after a plate");

        boiler.prime(0, true);
        steam(level, fireboxPos, firebox, boilerPos, boiler, 2100);
        helper.assertTrue(level.getBlockState(boilerPos).is(Tier4Blocks.CRACKED_BRONZE_BOILER.get()), "a boiler fired dry to zero integrity cracks");
        helper.succeed();
    }

    // Spec 9.2: pipes carry what is pushed into them up to the slowest pipe's throughput and refuse fluid
    // hotter than their rating; a gauge in the line shows the flow.
    private static void fluidPipes(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos source = helper.absolutePos(new BlockPos(0, 1, 1));
        BlockPos boilerPos = source.east(4);
        for (int i = 1; i <= 3; i++) level.setBlock(source.east(i), Tier4Blocks.COPPER_FLUID_PIPE.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(boilerPos, Tier4Blocks.BRONZE_BOILER.get().defaultBlockState(), Block.UPDATE_ALL);
        BoilerBlockEntity boiler = (BoilerBlockEntity) level.getBlockEntity(boilerPos);

        FluidPipes.Network network = FluidPipes.find(level, source, Direction.EAST);
        helper.assertValueEqual(network.pipes().size(), 3, "pipes in the line");
        helper.assertValueEqual(network.ports().size(), 1, "ports the line reaches");
        FluidPipes.Push water = FluidPipes.push(level, network, net.minecraft.world.level.material.Fluids.WATER, 500, 20.0f, 0.0f);
        helper.assertValueEqual(water.moved(), 100, "water a copper line carries in a tick");
        helper.assertValueEqual(boiler.water(), 100, "water in the boiler");

        FluidPipes.Push hot = FluidPipes.push(level, network, Tier4Fluids.STEAM.get(), 100, 250.0f, 10.0f);
        helper.assertTrue(hot.refused() && hot.moved() == 0, "copper pipe refuses 250 °C steam");

        for (int i = 1; i <= 3; i++) level.setBlock(source.east(i), Tier4Blocks.STEEL_FLUID_PIPE.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(source.east(2), Tier4Blocks.PRESSURE_GAUGE.get().defaultBlockState(), Block.UPDATE_ALL);
        network = FluidPipes.find(level, source, Direction.EAST);
        helper.assertValueEqual(network.throughput(), 200, "a bronze-rated gauge limits a steel line");
        water = FluidPipes.push(level, network, net.minecraft.world.level.material.Fluids.WATER, 500, 20.0f, 0.0f);
        helper.assertValueEqual(water.moved(), 200, "water through steel pipe and the gauge");
        helper.assertValueEqual(level.getBlockState(source.east(2)).getValue(PressureGaugeBlock.READING), 4, "gauge needle at a full flow");
        helper.succeed();
    }

    // Spec 10.5: a warm boiler on a hot firebox feeds an engine through a copper pipe. The engine waits
    // for 1 bar, turns at 16 RPM until the boiler reaches 2 bar, then holds 32 RPM on the boiler's 15 mB
    // a tick; with the pipe gone it stops.
    private static void steamEngine(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos fireboxPos = helper.absolutePos(new BlockPos(2, 1, 1));
        BlockPos boilerPos = fireboxPos.above();
        BlockPos pipePos = boilerPos.above();
        BlockPos enginePos = pipePos.south();
        BlockPos axlePos = enginePos.south();
        level.setBlock(fireboxPos, Tier4Blocks.FIREBOX.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(boilerPos, Tier4Blocks.BRONZE_BOILER.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(pipePos, Tier4Blocks.COPPER_FLUID_PIPE.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(enginePos, Tier4Blocks.STEAM_ENGINE.get().defaultBlockState().setValue(SteamEngineBlock.FACING, Direction.SOUTH),
                Block.UPDATE_ALL);
        level.setBlock(axlePos, Tier4Blocks.IRON_AXLE.get().defaultBlockState().setValue(AxleBlock.AXIS, Direction.Axis.Z), Block.UPDATE_ALL);
        FireboxBlockEntity firebox = (FireboxBlockEntity) level.getBlockEntity(fireboxPos);
        BoilerBlockEntity boiler = (BoilerBlockEntity) level.getBlockEntity(boilerPos);
        SteamEngineBlockEntity engine = (SteamEngineBlockEntity) level.getBlockEntity(enginePos);
        firebox.setItem(0, new ItemStack(Tier4Items.COKE.get(), 8));
        firebox.preheat(1500.0f);
        boiler.prime(BoilerBlockEntity.WATER_CAPACITY, true);

        engine(level, fireboxPos, firebox, boilerPos, boiler, enginePos, engine, 40);
        helper.assertValueEqual(engine.sourceSpeed(), 0.0f, "engine speed below 1 bar");
        helper.assertTrue(boiler.steam() > 0, "the boiler builds steam while the engine waits");

        engine(level, fireboxPos, firebox, boilerPos, boiler, enginePos, engine, 60);
        helper.assertValueEqual(engine.sourceSpeed(), SteamEngineBlockEntity.HALF_SPEED, "engine speed between 1 and 2 bar");

        // About 200 ticks to climb from 1 to 2 bar at half speed; a full boiler lasts some 530 ticks.
        engine(level, fireboxPos, firebox, boilerPos, boiler, enginePos, engine, 250);
        helper.assertValueEqual(engine.sourceSpeed(), SteamEngineBlockEntity.FULL_SPEED, "engine speed on a running boiler");
        helper.assertValueEqual(boiler.status(), BoilerBlockEntity.Status.RUNNING, "boiler status while driving the engine");
        helper.assertTrue(boiler.pressure() >= SteamEngineBlockEntity.HOLD_PRESSURE, "boiler holds pressure under load, got " + boiler.pressure());
        KineticNetworks.rebuildNow(level, axlePos);
        Kinetic axle = (Kinetic) level.getBlockEntity(axlePos);
        helper.assertValueEqual(Math.round(axle.kinetic().rpm()), 32, "axle RPM on the engine");

        level.setBlock(pipePos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        for (int i = 0; i < 20; i++) SteamEngineBlockEntity.serverTick(level, enginePos, level.getBlockState(enginePos), engine);
        helper.assertValueEqual(engine.sourceSpeed(), 0.0f, "engine speed with its steam cut off");
        helper.succeed();
    }

    // Spec 9.3: a cranked pump at 16 RPM lifts 25 mB a tick from the water in front of it into the pipe
    // behind; a lone source runs dry after a bucket and is gone.
    private static void mechanicalPump(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pumpPos = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos intake = pumpPos.north();
        BlockPos crankPos = pumpPos.west();
        BlockPos boilerPos = pumpPos.south(2);
        level.setBlock(pumpPos, Tier4Blocks.MECHANICAL_PUMP.get().defaultBlockState().setValue(MechanicalPumpBlock.FACING, Direction.NORTH),
                Block.UPDATE_ALL);
        level.setBlock(pumpPos.south(), Tier4Blocks.COPPER_FLUID_PIPE.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(boilerPos, Tier4Blocks.BRONZE_BOILER.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(crankPos, ModBlocks.HAND_CRANK.get().defaultBlockState().setValue(HandCrankBlock.FACING, Direction.EAST), Block.UPDATE_ALL);
        MechanicalPumpBlockEntity pump = (MechanicalPumpBlockEntity) level.getBlockEntity(pumpPos);
        BoilerBlockEntity boiler = (BoilerBlockEntity) level.getBlockEntity(boilerPos);

        pump(level, pumpPos, pump, 1);
        helper.assertValueEqual(pump.status(), MechanicalPumpBlockEntity.Status.NOT_TURNING, "pump status without a shaft turning");

        HandCrankBlockEntity crank = (HandCrankBlockEntity) level.getBlockEntity(crankPos);
        crank.crank(new FakePlayer(level, new GameProfile(UUID.randomUUID(), "engineer")));
        KineticNetworks.rebuildNow(level, pumpPos);
        pump(level, pumpPos, pump, 1);
        helper.assertValueEqual(pump.status(), MechanicalPumpBlockEntity.Status.NO_WATER, "pump status with nothing at the intake");

        level.setBlock(intake, Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
        pump(level, pumpPos, pump, 1);
        helper.assertValueEqual(pump.status(), MechanicalPumpBlockEntity.Status.PUMPING, "pump status on a water source");
        helper.assertValueEqual(pump.lastMoved(), MechanicalPumpBlockEntity.RATE, "mB a tick at 16 RPM");

        pump(level, pumpPos, pump, 39);
        helper.assertValueEqual(boiler.water(), MechanicalPumpBlockEntity.SOURCE_AMOUNT, "water pumped from one source");
        helper.assertTrue(level.getFluidState(intake).isEmpty(), "a lone source is used up after a bucket");
        helper.succeed();
    }

    // Spec 11.2: a crusher on a steam engine's 32 RPM works its three inputs side by side, 80 ticks each at
    // 16 RPM and so 40 here. Ore comes out crushed, rock as gravel, and coal is refused. (A hand crank's
    // 64 SU cannot carry a crusher's 128 SU at 16 RPM.)
    private static void crusher(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos crusherPos = helper.absolutePos(new BlockPos(2, 1, 2));
        level.setBlock(crusherPos, Tier4Blocks.CRUSHER.get().defaultBlockState().setValue(ProcessingBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);
        CrusherBlockEntity crusher = (CrusherBlockEntity) level.getBlockEntity(crusherPos);
        crusher.setItem(0, new ItemStack(ModItems.orePiece(OreMineral.HEMATITE, OreGrade.NORMAL), 2));
        crusher.setItem(1, new ItemStack(ModItems.COBBLED_ROCK.get(dev.strataindustria.geology.Rock.GRANITE).get()));
        crusher.setItem(2, new ItemStack(Items.COAL));

        crush(level, crusherPos, crusher, 1);
        helper.assertValueEqual(crusher.status(), ProcessingBlockEntity.Status.NOT_TURNING, "crusher status without a shaft turning");

        drive(level, crusherPos.west(), Direction.EAST, crusherPos);
        crush(level, crusherPos, crusher, 39);
        helper.assertValueEqual(crusher.status(), ProcessingBlockEntity.Status.WORKING, "crusher status at 32 RPM");
        helper.assertValueEqual(crusher.finishedCount(), 0, "items done after 39 ticks");
        helper.assertTrue(level.getBlockState(crusherPos).getValue(ProcessingBlock.ACTIVE), "the front shows the jaws moving");
        crush(level, crusherPos, crusher, 1);
        helper.assertValueEqual(crusher.finishedCount(), 2, "ore and rock both done after 40 ticks");
        helper.assertTrue(count(crusher, ModItems.crushedOre(OreMineral.HEMATITE, OreGrade.NORMAL)) >= 1, "crushed hematite out");
        helper.assertValueEqual(count(crusher, Items.GRAVEL), 1, "gravel from cobbled granite");
        helper.assertValueEqual(crusher.getItem(2).getCount(), 1, "coal stays in its slot");
        helper.assertTrue(!crusher.canPlaceItem(0, new ItemStack(Items.COAL)), "a hopper cannot put coal in");

        crush(level, crusherPos, crusher, 40);
        helper.assertTrue(crusher.getItem(0).isEmpty(), "second ore piece crushed");
        crush(level, crusherPos, crusher, 1);
        helper.assertValueEqual(crusher.status(), ProcessingBlockEntity.Status.NO_RECIPE, "status with only coal left");
        helper.assertTrue(!level.getBlockState(crusherPos).getValue(ProcessingBlock.ACTIVE), "the jaws stop");
        helper.succeed();
    }

    // Spec 11.3: the washer works crushed ore on piped water, 100 mB an item and 40 ticks at 16 RPM (20 at
    // the engine's 32), and has nothing to do with sulfide ore.
    private static void washer(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos washerPos = helper.absolutePos(new BlockPos(2, 1, 2));
        level.setBlock(washerPos, Tier4Blocks.WASHER.get().defaultBlockState().setValue(ProcessingBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);
        WasherBlockEntity washer = (WasherBlockEntity) level.getBlockEntity(washerPos);
        Item crushed = ModItems.crushedOre(OreMineral.HEMATITE, OreGrade.NORMAL);
        washer.setItem(0, new ItemStack(crushed, 3));
        helper.assertTrue(!washer.canPlaceItem(1, new ItemStack(ModItems.crushedOre(OreMineral.SPHALERITE, OreGrade.NORMAL))),
                "the washer refuses sulfide ore");
        drive(level, washerPos.west(), Direction.EAST, washerPos);

        wash(level, washerPos, washer, 1);
        helper.assertValueEqual(washer.status(), ProcessingBlockEntity.Status.NO_WATER, "washer status with an empty tank");
        helper.assertValueEqual(washer.fill(Direction.UP, net.minecraft.world.level.material.Fluids.WATER, 1000, 0, false), 0,
                "the top takes hoppers, not pipes");
        helper.assertValueEqual(washer.fill(Direction.EAST, net.minecraft.world.level.material.Fluids.WATER, 150, 0, false), 150,
                "water a side pipe adds");

        wash(level, washerPos, washer, 20);
        helper.assertValueEqual(count(washer, ModItems.washedOre(OreMineral.HEMATITE, OreGrade.NORMAL)), 1, "washed hematite after 20 ticks");
        helper.assertValueEqual(washer.water(), 50, "water left after one item");
        wash(level, washerPos, washer, 1);
        helper.assertValueEqual(washer.status(), ProcessingBlockEntity.Status.NO_WATER, "washer status with 50 mB left");
        helper.assertValueEqual(washer.getItem(0).getCount(), 2, "the rest waits for water");
        helper.succeed();
    }

    // Spec 12.1 and 23: with one blower, 16 crushed normal hematite (560 units), 3 coke and 3 flux give 5 pig
    // iron and 2 slag at 200 ticks an ingot once the hearth is hot, with 60 units and half a slag carried over.
    private static void blastFurnace(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // The hearth is the test floor's layer, so all five layers fit inside the test area.
        BlockPos controllerPos = helper.absolutePos(new BlockPos(4, 0, 2));
        BlockPos centre = controllerPos.south();
        blastFurnaceShell(level, controllerPos);
        BlockPos tuyerePos = centre.west(), tapPos = centre.east(), hatchPos = centre.above(4), blowerPos = tuyerePos.west();
        var furnace = (dev.strataindustria.ironworks.BlastFurnaceBlockEntity) level.getBlockEntity(controllerPos);

        smelt(level, controllerPos, furnace, 1);
        helper.assertValueEqual(furnace.status(), dev.strataindustria.ironworks.BlastFurnaceBlockEntity.Status.INCOMPLETE, "status without a charging hatch");
        helper.assertValueEqual(furnace.checkStructure().problem(), dev.strataindustria.ironworks.BlastFurnaceStructure.Problem.NEEDS_CHARGING_HATCH,
                "the missing block named");
        level.setBlock(hatchPos, Tier4Blocks.CHARGING_HATCH.get().defaultBlockState(), Block.UPDATE_ALL);
        furnace.checkStructure();
        smelt(level, controllerPos, furnace, 1);
        helper.assertValueEqual(furnace.status(), dev.strataindustria.ironworks.BlastFurnaceBlockEntity.Status.NO_AIR, "status with a still blower");

        drive(level, blowerPos.west(), Direction.EAST, blowerPos);
        smelt(level, controllerPos, furnace, 1);
        helper.assertValueEqual(furnace.air(), 1, "one blower's worth of air");
        helper.assertValueEqual(furnace.status(), dev.strataindustria.ironworks.BlastFurnaceBlockEntity.Status.NEEDS_FUEL, "status with no coke");

        // Burden goes in through the charging hatch, as from a hopper.
        var hatch = (dev.strataindustria.ironworks.FurnaceHatchBlockEntity) level.getBlockEntity(hatchPos);
        ItemStack ore = new ItemStack(ModItems.crushedOre(OreMineral.HEMATITE, OreGrade.NORMAL), 16);
        helper.assertTrue(hatch.canPlaceItemThroughFace(dev.strataindustria.ironworks.BlastFurnaceBlockEntity.ORE, ore, Direction.UP),
                "the charging hatch takes ore");
        helper.assertTrue(!hatch.canPlaceItemThroughFace(dev.strataindustria.ironworks.BlastFurnaceBlockEntity.FUEL, ore, Direction.UP),
                "ore does not go in the fuel slot");
        hatch.setItem(dev.strataindustria.ironworks.BlastFurnaceBlockEntity.ORE, ore);
        hatch.setItem(dev.strataindustria.ironworks.BlastFurnaceBlockEntity.FUEL, new ItemStack(Tier4Items.COKE.get(), 3));
        hatch.setItem(dev.strataindustria.ironworks.BlastFurnaceBlockEntity.FLUX, new ItemStack(ModItems.FLUX.get(), 3));
        smelt(level, controllerPos, furnace, 1);
        helper.assertValueEqual(furnace.iron(), 560, "iron units from 16 crushed normal hematite");
        helper.assertValueEqual(furnace.status(), dev.strataindustria.ironworks.BlastFurnaceBlockEntity.Status.HEATING, "a cold hearth heats first");
        helper.assertTrue(level.getBlockState(controllerPos).getValue(dev.strataindustria.ironworks.BlastFurnaceBlock.LIT), "the peephole glows");

        furnace.heatUp();
        smelt(level, controllerPos, furnace, 199);
        helper.assertValueEqual(furnace.getItem(dev.strataindustria.ironworks.BlastFurnaceBlockEntity.PIG_IRON).getCount(), 0, "nothing after 199 ticks");
        helper.assertTrue(level.getBlockState(tapPos).getValue(dev.strataindustria.ironworks.TapHatchBlock.HOT), "the tap glows while running");
        smelt(level, controllerPos, furnace, 801);
        var tap = (dev.strataindustria.ironworks.FurnaceHatchBlockEntity) level.getBlockEntity(tapPos);
        helper.assertValueEqual(tap.getItem(dev.strataindustria.ironworks.BlastFurnaceBlockEntity.PIG_IRON).getCount(), 5, "pig iron at the tap");
        helper.assertTrue(tap.getItem(dev.strataindustria.ironworks.BlastFurnaceBlockEntity.PIG_IRON).is(ModItems.ingot(Metal.PIG_IRON)), "it is pig iron");
        helper.assertValueEqual(tap.getItem(dev.strataindustria.ironworks.BlastFurnaceBlockEntity.SLAG).getCount(), 2, "slag at the tap");
        helper.assertTrue(tap.canTakeItemThroughFace(dev.strataindustria.ironworks.BlastFurnaceBlockEntity.PIG_IRON, ItemStack.EMPTY, Direction.DOWN),
                "a hopper can take pig iron from the tap");
        helper.assertValueEqual(furnace.iron(), 60, "iron units carried over");
        smelt(level, controllerPos, furnace, 1);
        helper.assertValueEqual(furnace.status(), dev.strataindustria.ironworks.BlastFurnaceBlockEntity.Status.NEEDS_IRON, "status with 60 units left");
        helper.succeed();
    }

    // Spec 12.2 and 23: 8 pig iron and a coke with air give 8 steel and 2 slag in 600 ticks. A smaller charge
    // waits for more, then blows with a scrap item for every 4 pig iron.
    private static void converter(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos controllerPos = helper.absolutePos(new BlockPos(4, 0, 2));
        BlockPos centre = controllerPos.south();
        converterShell(level, controllerPos);
        BlockPos tuyerePos = centre.west(), tapPos = centre.above().east(), hatchPos = centre.above(2), blowerPos = tuyerePos.west();
        var converter = (dev.strataindustria.ironworks.ConverterBlockEntity) level.getBlockEntity(controllerPos);
        var hatch = (dev.strataindustria.ironworks.FurnaceHatchBlockEntity) level.getBlockEntity(hatchPos);
        Item pig = ModItems.ingot(Metal.PIG_IRON), steel = ModItems.ingot(Metal.STEEL);
        int pigSlot = dev.strataindustria.ironworks.ConverterBlockEntity.PIG_IRON;

        blow(level, controllerPos, converter, 1);
        helper.assertValueEqual(converter.status(), dev.strataindustria.ironworks.ConverterBlockEntity.Status.EMPTY, "status when built and empty");
        hatch.setItem(pigSlot, new ItemStack(pig, 8));
        helper.assertTrue(!hatch.canPlaceItemThroughFace(pigSlot, new ItemStack(pig), Direction.UP), "8 pig iron is a full charge");
        blow(level, controllerPos, converter, 1);
        helper.assertValueEqual(converter.status(), dev.strataindustria.ironworks.ConverterBlockEntity.Status.NO_AIR, "status without air");
        drive(level, blowerPos.west(), Direction.EAST, blowerPos);
        blow(level, controllerPos, converter, 1);
        helper.assertValueEqual(converter.status(), dev.strataindustria.ironworks.ConverterBlockEntity.Status.NEEDS_PREHEAT, "status without coke");
        hatch.setItem(dev.strataindustria.ironworks.ConverterBlockEntity.COKE, new ItemStack(Tier4Items.COKE.get()));
        blow(level, controllerPos, converter, 1);
        helper.assertValueEqual(converter.status(), dev.strataindustria.ironworks.ConverterBlockEntity.Status.BLOWING, "a full charge blows at once");
        helper.assertValueEqual(converter.blowing(), 8, "pig iron in the blow");
        helper.assertTrue(converter.getItem(dev.strataindustria.ironworks.ConverterBlockEntity.COKE).isEmpty(), "the coke preheated it");
        blow(level, controllerPos, converter, 599);
        helper.assertValueEqual(converter.status(), dev.strataindustria.ironworks.ConverterBlockEntity.Status.BLOWING, "still blowing at 599 ticks");
        helper.assertTrue(level.getBlockState(tapPos).getValue(dev.strataindustria.ironworks.TapHatchBlock.HOT), "the tap glows during a blow");
        blow(level, controllerPos, converter, 1);
        var tap = (dev.strataindustria.ironworks.FurnaceHatchBlockEntity) level.getBlockEntity(tapPos);
        int steelSlot = dev.strataindustria.ironworks.ConverterBlockEntity.STEEL, slagSlot = dev.strataindustria.ironworks.ConverterBlockEntity.SLAG;
        helper.assertTrue(tap.getItem(steelSlot).is(steel), "steel at the tap");
        helper.assertValueEqual(tap.getItem(steelSlot).getCount(), 8, "steel from 8 pig iron");
        helper.assertValueEqual(tap.getItem(slagSlot).getCount(), 2, "slag from 8 pig iron");

        // Half a charge with a piece of scrap: it waits two seconds for more, then blows.
        hatch.setItem(pigSlot, new ItemStack(pig, 4));
        hatch.setItem(dev.strataindustria.ironworks.ConverterBlockEntity.SCRAP, new ItemStack(ModItems.ingot(Metal.WROUGHT_IRON), 2));
        hatch.setItem(dev.strataindustria.ironworks.ConverterBlockEntity.COKE, new ItemStack(Tier4Items.COKE.get()));
        blow(level, controllerPos, converter, 40);
        helper.assertValueEqual(converter.status(), dev.strataindustria.ironworks.ConverterBlockEntity.Status.CHARGING, "a short charge waits");
        blow(level, controllerPos, converter, 1);
        helper.assertValueEqual(converter.status(), dev.strataindustria.ironworks.ConverterBlockEntity.Status.BLOWING, "then blows as it is");
        helper.assertValueEqual(converter.getItem(dev.strataindustria.ironworks.ConverterBlockEntity.SCRAP).getCount(), 1,
                "one scrap for 4 pig iron, the other stays");
        blow(level, controllerPos, converter, 600);
        helper.assertValueEqual(tap.getItem(steelSlot).getCount(), 13, "5 more steel");
        helper.assertValueEqual(tap.getItem(slagSlot).getCount(), 3, "one more slag");
        helper.succeed();
    }

    /**
     * A blast furnace with its hearth on the floor, controller facing north, a tuyere on the west with a
     * still blower behind it and the tap on the east; everything but the charging hatch.
     */
    private static void blastFurnaceShell(ServerLevel level, BlockPos controllerPos) {
        BlockPos centre = controllerPos.south();
        BlockState casing = Tier4Blocks.REFRACTORY_CASING.get().defaultBlockState();
        for (int y = 0; y <= 4; y++)
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos pos = centre.offset(dx, y, dz);
                    boolean shaft = dx == 0 && dz == 0 && y >= 1 && y <= 3;
                    BlockState state = shaft ? Blocks.AIR.defaultBlockState() : y >= 2 ? ModBlocks.FIRE_BRICKS.get().defaultBlockState() : casing;
                    level.setBlock(pos, state, Block.UPDATE_ALL);
                }
        BlockPos tuyerePos = centre.west();
        level.setBlock(controllerPos, Tier4Blocks.BLAST_FURNACE_CONTROLLER.get().defaultBlockState()
                .setValue(dev.strataindustria.ironworks.BlastFurnaceBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);
        level.setBlock(tuyerePos, Tier4Blocks.TUYERE.get().defaultBlockState()
                .setValue(dev.strataindustria.ironworks.FurnacePartBlock.FACING, Direction.WEST), Block.UPDATE_ALL);
        level.setBlock(centre.east(), Tier4Blocks.TAP_HATCH.get().defaultBlockState()
                .setValue(dev.strataindustria.ironworks.TapHatchBlock.FACING, Direction.EAST), Block.UPDATE_ALL);
        level.setBlock(tuyerePos.west(), Tier4Blocks.BLOWER.get().defaultBlockState()
                .setValue(dev.strataindustria.ironworks.BlowerBlock.FACING, Direction.EAST), Block.UPDATE_ALL);
    }

    /** A complete converter on the floor, controller facing north, tuyere and still blower on the west, tap on the east. */
    private static void converterShell(ServerLevel level, BlockPos controllerPos) {
        BlockPos centre = controllerPos.south();
        BlockState casing = Tier4Blocks.REFRACTORY_CASING.get().defaultBlockState();
        for (int y = 0; y <= 2; y++)
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos pos = centre.offset(dx, y, dz);
                    BlockState state = dx == 0 && dz == 0 && y == 1 ? Blocks.AIR.defaultBlockState()
                            : y == 2 ? ModBlocks.FIRE_BRICKS.get().defaultBlockState() : casing;
                    level.setBlock(pos, state, Block.UPDATE_ALL);
                }
        BlockPos tuyerePos = centre.west();
        level.setBlock(controllerPos, Tier4Blocks.CONVERTER_CONTROLLER.get().defaultBlockState()
                .setValue(dev.strataindustria.ironworks.ConverterBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);
        level.setBlock(tuyerePos, Tier4Blocks.TUYERE.get().defaultBlockState()
                .setValue(dev.strataindustria.ironworks.FurnacePartBlock.FACING, Direction.WEST), Block.UPDATE_ALL);
        level.setBlock(centre.above().east(), Tier4Blocks.TAP_HATCH.get().defaultBlockState()
                .setValue(dev.strataindustria.ironworks.TapHatchBlock.FACING, Direction.EAST), Block.UPDATE_ALL);
        level.setBlock(centre.above(2), Tier4Blocks.CHARGING_HATCH.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(tuyerePos.west(), Tier4Blocks.BLOWER.get().defaultBlockState()
                .setValue(dev.strataindustria.ironworks.BlowerBlock.FACING, Direction.EAST), Block.UPDATE_ALL);
    }

    // Spec 8.2 and 23: a coke firebox (1600 °C) on 10 copper heat pipes delivers 900 °C, capped to 1000
    // and less 10 °C a block, and loses 1% of the heat a block; on refractory ducts it delivers 1550 °C.
    // Two boilers on one coal firebox (30 HU/t) asking 30 HU/t each get half each, less the pipe losses;
    // a blower makes the fire give half again as much.
    private static void heatPipes(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos fireboxPos = helper.absolutePos(new BlockPos(0, 1, 0));
        java.util.List<BlockPos> line = new java.util.ArrayList<>();
        for (int x = 1; x <= 8; x++) line.add(helper.absolutePos(new BlockPos(x, 1, 0)));
        line.add(helper.absolutePos(new BlockPos(8, 1, 1)));
        line.add(helper.absolutePos(new BlockPos(8, 1, 2)));
        BlockPos boilerPos = helper.absolutePos(new BlockPos(8, 1, 3));
        level.setBlock(fireboxPos, Tier4Blocks.FIREBOX.get().defaultBlockState(), Block.UPDATE_ALL);
        for (BlockPos pos : line) level.setBlock(pos, Tier4Blocks.COPPER_HEAT_PIPE.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(boilerPos, Tier4Blocks.BRONZE_BOILER.get().defaultBlockState(), Block.UPDATE_ALL);
        FireboxBlockEntity firebox = (FireboxBlockEntity) level.getBlockEntity(fireboxPos);
        BoilerBlockEntity boiler = (BoilerBlockEntity) level.getBlockEntity(boilerPos);
        firebox.setItem(0, new ItemStack(Tier4Items.COKE.get(), 4));
        firebox.preheat(1600.0f);

        steam(level, fireboxPos, firebox, boilerPos, boiler, 1);
        helper.assertValueEqual(Math.round(boiler.heatTemperature()), 900, "°C at the boiler over 10 copper pipes from a 1600 °C fire");
        helper.assertValueEqual(firebox.taken(), 30, "the boiler asks for its 30 HU/t");
        helper.assertValueEqual(boiler.heatTaken(), 27, "HU/t left after 10% lost on the way");
        helper.assertTrue(level.getBlockState(line.get(0)).getValue(dev.strataindustria.heat.HeatPipeBlock.HOT), "pipes glow over 580 °C");

        for (BlockPos pos : line) level.setBlock(pos, Tier4Blocks.REFRACTORY_HEAT_DUCT.get().defaultBlockState(), Block.UPDATE_ALL);
        dev.strataindustria.heat.HeatNetwork.changed();
        steam(level, fireboxPos, firebox, boilerPos, boiler, 1);
        helper.assertValueEqual(Math.round(boiler.heatTemperature()), 1550, "°C at the boiler over 10 refractory ducts");

        level.setBlock(line.get(4), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        steam(level, fireboxPos, firebox, boilerPos, boiler, 1);
        helper.assertValueEqual(boiler.heatTaken(), 0, "a gap in the line cuts the boiler off");
        helper.assertTrue(!level.getBlockState(line.get(6)).getValue(dev.strataindustria.heat.HeatPipeBlock.HOT), "pipes cut off from the fire cool");

        // Two boilers five copper pipes away on either side of one coal firebox.
        BlockPos coalPos = helper.absolutePos(new BlockPos(4, 1, 7));
        level.setBlock(coalPos, Tier4Blocks.FIREBOX.get().defaultBlockState(), Block.UPDATE_ALL);
        for (int x = 0; x <= 3; x++) level.setBlock(helper.absolutePos(new BlockPos(x, 1, 7)), Tier4Blocks.COPPER_HEAT_PIPE.get().defaultBlockState(), Block.UPDATE_ALL);
        for (int x = 5; x <= 8; x++) level.setBlock(helper.absolutePos(new BlockPos(x, 1, 7)), Tier4Blocks.COPPER_HEAT_PIPE.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(helper.absolutePos(new BlockPos(0, 1, 6)), Tier4Blocks.COPPER_HEAT_PIPE.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(helper.absolutePos(new BlockPos(8, 1, 6)), Tier4Blocks.COPPER_HEAT_PIPE.get().defaultBlockState(), Block.UPDATE_ALL);
        BlockPos westPos = helper.absolutePos(new BlockPos(0, 1, 5)), eastPos = helper.absolutePos(new BlockPos(8, 1, 5));
        level.setBlock(westPos, Tier4Blocks.BRONZE_BOILER.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(eastPos, Tier4Blocks.BRONZE_BOILER.get().defaultBlockState(), Block.UPDATE_ALL);
        FireboxBlockEntity coal = (FireboxBlockEntity) level.getBlockEntity(coalPos);
        BoilerBlockEntity west = (BoilerBlockEntity) level.getBlockEntity(westPos), east = (BoilerBlockEntity) level.getBlockEntity(eastPos);
        coal.setItem(0, new ItemStack(Items.COAL, 4));
        coal.preheat(1400.0f);
        share(level, coalPos, coal, westPos, west, eastPos, east);
        helper.assertValueEqual(coal.taken(), 30, "the coal fire's 30 HU/t all goes");
        helper.assertValueEqual(west.heatTaken(), 14, "half of it, less 5% on 5 pipes, to the west boiler");
        helper.assertValueEqual(east.heatTaken(), 14, "and as much to the east one");
        helper.assertValueEqual(Math.round(west.heatTemperature()), 950, "1000 °C copper cap less 5 pipes");

        BlockPos blowerPos = coalPos.north();
        level.setBlock(blowerPos, Tier4Blocks.BLOWER.get().defaultBlockState()
                .setValue(dev.strataindustria.ironworks.BlowerBlock.FACING, Direction.SOUTH), Block.UPDATE_ALL);
        drive(level, blowerPos.north(), Direction.SOUTH, blowerPos);
        share(level, coalPos, coal, westPos, west, eastPos, east);
        helper.assertTrue(coal.blown(), "the blower blows into the firebox");
        helper.assertValueEqual(coal.output(), 45, "a blower makes 30 HU/t into 45");
        helper.assertValueEqual(coal.taken(), 44, "22 HU/t to each boiler");
        helper.succeed();
    }

    private static void share(ServerLevel level, BlockPos fireboxPos, FireboxBlockEntity firebox, BlockPos westPos, BoilerBlockEntity west,
            BlockPos eastPos, BoilerBlockEntity east) {
        FireboxBlockEntity.serverTick(level, fireboxPos, level.getBlockState(fireboxPos), firebox);
        BoilerBlockEntity.serverTick(level, westPos, level.getBlockState(westPos), west);
        BoilerBlockEntity.serverTick(level, eastPos, level.getBlockState(eastPos), east);
    }

    // Spec 8.3 and 12.1: hot blast at 800 °C or more through a heat inlet in the hearth halves the coke,
    // to a quarter an ingot, when it covers at least half of the furnace's 40 HU/t.
    private static void hotBlast(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos controllerPos = helper.absolutePos(new BlockPos(4, 0, 2));
        BlockPos centre = controllerPos.south();
        blastFurnaceShell(level, controllerPos);
        level.setBlock(centre.above(4), Tier4Blocks.CHARGING_HATCH.get().defaultBlockState(), Block.UPDATE_ALL);
        BlockPos inletPos = centre.offset(-1, 0, -1), fireboxPos = inletPos.west();
        level.setBlock(inletPos, Tier4Blocks.HEAT_INLET.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(fireboxPos, Tier4Blocks.FIREBOX.get().defaultBlockState(), Block.UPDATE_ALL);
        BlockPos blowerPos = centre.west(2);
        drive(level, blowerPos.west(), Direction.EAST, blowerPos);
        var furnace = (dev.strataindustria.ironworks.BlastFurnaceBlockEntity) level.getBlockEntity(controllerPos);
        FireboxBlockEntity firebox = (FireboxBlockEntity) level.getBlockEntity(fireboxPos);
        firebox.setItem(0, new ItemStack(Tier4Items.COKE.get(), 8));
        firebox.preheat(1600.0f);
        furnace.setItem(dev.strataindustria.ironworks.BlastFurnaceBlockEntity.ORE,
                new ItemStack(ModItems.crushedOre(OreMineral.HEMATITE, OreGrade.NORMAL), 16));
        furnace.setItem(dev.strataindustria.ironworks.BlastFurnaceBlockEntity.FUEL, new ItemStack(Tier4Items.COKE.get(), 3));
        furnace.setItem(dev.strataindustria.ironworks.BlastFurnaceBlockEntity.FLUX, new ItemStack(ModItems.FLUX.get(), 3));
        furnace.heatUp();

        for (int i = 0; i < 1000; i++) {
            FireboxBlockEntity.serverTick(level, fireboxPos, level.getBlockState(fireboxPos), firebox);
            smelt(level, controllerPos, furnace, 1);
        }
        // A lone coke firebox makes 30 of the 40 HU/t the furnace asks for, which is most of a full supply.
        helper.assertValueEqual(furnace.hotBlast(), 30, "HU/t of hot blast from one coke firebox");
        helper.assertValueEqual(furnace.getItem(dev.strataindustria.ironworks.BlastFurnaceBlockEntity.PIG_IRON).getCount(), 5,
                "five ingots in 1000 ticks");
        helper.assertValueEqual(furnace.fuel(), 12 - 5, "a quarter of a coke an ingot on hot blast");
        helper.succeed();
    }

    // Spec 8.3 and 12.2: a heat inlet at 1250 °C or more preheats a blow instead of a coke. Copper pipe
    // holds the heat down to 1000 °C, which is not enough; a refractory duct carries it.
    private static void converterPreheat(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos controllerPos = helper.absolutePos(new BlockPos(4, 0, 2));
        BlockPos centre = controllerPos.south();
        converterShell(level, controllerPos);
        BlockPos inletPos = centre.offset(-1, 0, -1), pipePos = inletPos.west(), fireboxPos = pipePos.west();
        level.setBlock(inletPos, Tier4Blocks.HEAT_INLET.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(pipePos, Tier4Blocks.COPPER_HEAT_PIPE.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(fireboxPos, Tier4Blocks.FIREBOX.get().defaultBlockState(), Block.UPDATE_ALL);
        BlockPos blowerPos = centre.west(2);
        drive(level, blowerPos.west(), Direction.EAST, blowerPos);
        var converter = (dev.strataindustria.ironworks.ConverterBlockEntity) level.getBlockEntity(controllerPos);
        FireboxBlockEntity firebox = (FireboxBlockEntity) level.getBlockEntity(fireboxPos);
        firebox.setItem(0, new ItemStack(Tier4Items.COKE.get(), 8));
        firebox.preheat(1600.0f);
        converter.setItem(dev.strataindustria.ironworks.ConverterBlockEntity.PIG_IRON, new ItemStack(ModItems.ingot(Metal.PIG_IRON), 8));

        preheat(level, fireboxPos, firebox, controllerPos, converter, 3);
        helper.assertValueEqual(converter.status(), dev.strataindustria.ironworks.ConverterBlockEntity.Status.NEEDS_PREHEAT,
                "copper pipe holds the heat under 1250 °C");
        helper.assertValueEqual(converter.preheat().limit(), 1000, "the copper pipe's rating is what holds it");
        helper.assertValueEqual(Math.round(converter.preheat().temperature()), 990, "1000 °C less one pipe");

        level.setBlock(pipePos, Tier4Blocks.REFRACTORY_HEAT_DUCT.get().defaultBlockState(), Block.UPDATE_ALL);
        dev.strataindustria.heat.HeatNetwork.changed();
        preheat(level, fireboxPos, firebox, controllerPos, converter, 2);
        helper.assertValueEqual(converter.status(), dev.strataindustria.ironworks.ConverterBlockEntity.Status.BLOWING,
                "a duct carries 1595 °C, and the blow starts without coke");
        helper.assertValueEqual(converter.blowing(), 8, "pig iron in the blow");
        helper.succeed();
    }

    // Spec 8.6 and 8.2: a kiln on a coke firebox fires everything loaded in 600 ticks at 20 HU/t, slag dust
    // into slag wool four to one; slag wool round a pipe halves its temperature drop.
    private static void kiln(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos fireboxPos = helper.absolutePos(new BlockPos(2, 1, 2)), kilnPos = fireboxPos.above();
        level.setBlock(fireboxPos, Tier4Blocks.FIREBOX.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(kilnPos, Tier4Blocks.KILN.get().defaultBlockState(), Block.UPDATE_ALL);
        FireboxBlockEntity firebox = (FireboxBlockEntity) level.getBlockEntity(fireboxPos);
        var kiln = (dev.strataindustria.ceramics.KilnBlockEntity) level.getBlockEntity(kilnPos);
        firebox.setItem(0, new ItemStack(Tier4Items.COKE.get(), 4));
        firebox.preheat(1600.0f);
        helper.assertTrue(!kiln.canPlaceItem(0, new ItemStack(Items.COBBLESTONE)), "the kiln takes only what it fires");
        kiln.setItem(0, new ItemStack(ModItems.UNFIRED_FIRE_BRICK.get(), 8));
        kiln.setItem(1, new ItemStack(Tier4Items.SLAG_DUST.get(), 9));

        fire(level, fireboxPos, firebox, kilnPos, kiln, 1);
        helper.assertValueEqual(kiln.status(), dev.strataindustria.ceramics.KilnBlockEntity.Status.NEEDS_HEAT, "status before the heat arrives");
        fire(level, fireboxPos, firebox, kilnPos, kiln, 599);
        helper.assertValueEqual(kiln.status(), dev.strataindustria.ceramics.KilnBlockEntity.Status.FIRING, "status while firing");
        helper.assertValueEqual(firebox.taken(), dev.strataindustria.ceramics.KilnBlockEntity.HEAT, "HU/t the kiln draws");
        helper.assertTrue(kiln.getItem(dev.strataindustria.ceramics.KilnBlockEntity.INPUTS).isEmpty(), "nothing fired before 600 ticks");
        fire(level, fireboxPos, firebox, kilnPos, kiln, 1);
        int out = dev.strataindustria.ceramics.KilnBlockEntity.INPUTS;
        helper.assertTrue(kiln.getItem(out).is(ModItems.FIRE_BRICK.get()) && kiln.getItem(out).getCount() == 8, "8 fire bricks");
        helper.assertTrue(kiln.getItem(out + 1).is(Tier4Items.SLAG_WOOL.get()) && kiln.getItem(out + 1).getCount() == 2, "2 slag wool");
        helper.assertValueEqual(kiln.getItem(1).getCount(), 1, "slag dust short of four waits");

        // An insulated pipe loses 5 °C a block instead of 10.
        BlockPos hotPos = helper.absolutePos(new BlockPos(0, 1, 6));
        level.setBlock(hotPos, Tier4Blocks.FIREBOX.get().defaultBlockState(), Block.UPDATE_ALL);
        for (int x = 1; x <= 4; x++) {
            level.setBlock(helper.absolutePos(new BlockPos(x, 1, 6)), Tier4Blocks.INSULATED_COPPER_HEAT_PIPE.get().defaultBlockState(), Block.UPDATE_ALL);
        }
        BlockPos boilerPos = helper.absolutePos(new BlockPos(5, 1, 6));
        level.setBlock(boilerPos, Tier4Blocks.BRONZE_BOILER.get().defaultBlockState(), Block.UPDATE_ALL);
        FireboxBlockEntity hot = (FireboxBlockEntity) level.getBlockEntity(hotPos);
        BoilerBlockEntity boiler = (BoilerBlockEntity) level.getBlockEntity(boilerPos);
        hot.setItem(0, new ItemStack(Tier4Items.COKE.get(), 4));
        hot.preheat(1600.0f);
        steam(level, hotPos, hot, boilerPos, boiler, 1);
        helper.assertValueEqual(Math.round(boiler.heatTemperature()), 980, "1000 °C less 4 insulated pipes at 5 °C");
        helper.succeed();
    }

    // Spec 8.5 and 5.3: a roaster on a coke firebox roasts each slot's stack at half the forge's time and
    // keeps the gas; a slot whose calcine would not fit waits, and gas past the 4000 mB tank is vented.
    private static void roaster(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos fireboxPos = helper.absolutePos(new BlockPos(2, 1, 2)), roasterPos = fireboxPos.above();
        level.setBlock(fireboxPos, Tier4Blocks.FIREBOX.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(roasterPos, Tier4Blocks.ROASTER.get().defaultBlockState(), Block.UPDATE_ALL);
        FireboxBlockEntity firebox = (FireboxBlockEntity) level.getBlockEntity(fireboxPos);
        var roaster = (dev.strataindustria.roasting.RoasterBlockEntity) level.getBlockEntity(roasterPos);
        firebox.setItem(0, new ItemStack(Tier4Items.COKE.get(), 4));
        firebox.preheat(1600.0f);
        Item crushed = ModItems.crushedOre(OreMineral.SPHALERITE, OreGrade.NORMAL), calcine = Tier4Items.zincCalcine(OreGrade.NORMAL);
        helper.assertTrue(roaster.canPlaceItem(0, new ItemStack(crushed)), "the roaster takes crushed sphalerite");
        helper.assertTrue(!roaster.canPlaceItem(0, new ItemStack(Items.COBBLESTONE)), "the roaster takes only what roasts");
        roaster.setItem(0, new ItemStack(crushed, 4));
        roaster.setItem(1, new ItemStack(ModItems.SMALL_ORES.get(OreMineral.SPHALERITE).get(), 2));
        int out = dev.strataindustria.roasting.RoasterBlockEntity.INPUTS;

        roast(level, fireboxPos, firebox, roasterPos, roaster, 1);
        helper.assertValueEqual(roaster.status(), dev.strataindustria.roasting.RoasterBlockEntity.Status.NEEDS_HEAT, "status before the heat arrives");
        roast(level, fireboxPos, firebox, roasterPos, roaster, 100);
        helper.assertValueEqual(roaster.status(), dev.strataindustria.roasting.RoasterBlockEntity.Status.ROASTING, "status while roasting");
        helper.assertValueEqual(firebox.taken(), dev.strataindustria.roasting.RoasterBlockEntity.HEAT, "HU/t the roaster draws");
        helper.assertTrue(roaster.getItem(out + 1).is(Tier4Items.SMALL_ZINC_CALCINE.get()) && roaster.getItem(out + 1).getCount() == 2,
                "small sphalerite roasts in 100 ticks");
        helper.assertTrue(roaster.getItem(out).isEmpty(), "crushed sphalerite is not done at 100 ticks");
        helper.assertValueEqual(roaster.gas(), 30, "15 mB of gas from each small piece");
        roast(level, fireboxPos, firebox, roasterPos, roaster, 100);
        helper.assertTrue(roaster.getItem(out).is(calcine) && roaster.getItem(out).getCount() == 4, "4 calcine in 200 ticks");
        helper.assertValueEqual(roaster.gas(), 230, "50 mB more from each crushed piece");

        // 64 more would not fit with the 4 calcine already out, so that slot waits; two full stacks overflow the tank.
        roaster.setItem(0, new ItemStack(crushed, 64));
        roaster.setItem(2, new ItemStack(crushed, 64));
        roaster.setItem(3, new ItemStack(crushed, 64));
        roast(level, fireboxPos, firebox, roasterPos, roaster, 200);
        helper.assertTrue(roaster.getItem(out + 2).getCount() == 64 && roaster.getItem(out + 3).getCount() == 64, "two stacks roasted");
        helper.assertValueEqual(roaster.getItem(0).getCount(), 64, "a slot with no room for its calcine waits");
        helper.assertValueEqual(roaster.gas(), dev.strataindustria.roasting.RoasterBlockEntity.CAPACITY, "the tank is full");
        helper.assertValueEqual(roaster.status(), dev.strataindustria.roasting.RoasterBlockEntity.Status.VENTING, "the rest is vented");
        helper.succeed();
    }

    // Spec 8.7: a smelter on a coke firebox heats with no forge, melts two copper ingots, and with auto-pour
    // casts both by itself: pour, cool, knock out, and take the next mold from the stock.
    private static void smelter(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos fireboxPos = helper.absolutePos(new BlockPos(2, 1, 2)), smelterPos = fireboxPos.above();
        level.setBlock(fireboxPos, Tier4Blocks.FIREBOX.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(smelterPos, Tier4Blocks.SMELTER.get().defaultBlockState(), Block.UPDATE_ALL);
        FireboxBlockEntity firebox = (FireboxBlockEntity) level.getBlockEntity(fireboxPos);
        var smelter = (dev.strataindustria.metal.SmelterBlockEntity) level.getBlockEntity(smelterPos);
        Item copper = ModItems.ingot(dev.strataindustria.material.Metal.COPPER);
        helper.assertTrue(smelter.canPlaceItem(0, new ItemStack(ModItems.ingot(dev.strataindustria.material.Metal.WROUGHT_IRON))),
                "the smelter takes iron, like a refractory crucible");
        helper.assertTrue(!smelter.canPlaceItem(dev.strataindustria.metal.SmelterBlockEntity.OUTPUT_SLOT, new ItemStack(copper)),
                "nothing goes into the output");
        smelter.setItem(0, new ItemStack(copper, 2));
        smelter.setItem(dev.strataindustria.metal.SmelterBlockEntity.STOCK_SLOT, new ItemStack(ModItems.INGOT_MOLD.get(), 2));
        helper.assertTrue(smelter.autoPour(), "auto-pour starts on");

        smelt(level, fireboxPos, firebox, smelterPos, smelter, 1);
        helper.assertValueEqual(smelter.status(), dev.strataindustria.metal.CrucibleStatus.NO_HEAT, "status over a cold firebox");
        firebox.setItem(0, new ItemStack(Tier4Items.COKE.get(), 8));
        firebox.preheat(1600.0f);
        smelt(level, fireboxPos, firebox, smelterPos, smelter, 20);
        helper.assertValueEqual(firebox.taken(), 30, "a coke firebox gives the smelter its full 30 HU/t");
        helper.assertTrue(smelter.getItem(dev.strataindustria.metal.CrucibleBlockEntity.MOLD_SLOT).is(ModItems.INGOT_MOLD.get()),
                "a mold comes out of the stock");
        int out = dev.strataindustria.metal.SmelterBlockEntity.OUTPUT_SLOT;
        int tick = 0;
        for (; tick < 8000 && smelter.getItem(out).getCount() < 2; tick++) smelt(level, fireboxPos, firebox, smelterPos, smelter, 1);
        helper.assertTrue(smelter.getItem(out).is(copper) && smelter.getItem(out).getCount() == 2,
                "two copper ingots cast by themselves, got " + smelter.getItem(out) + " after " + tick + " ticks");
        helper.assertTrue(smelter.melt().isEmpty(), "the pot is empty");
        helper.assertTrue(smelter.getItem(out).getCount() == 2, "the castings stack though they came out at different heats");
        helper.succeed();
    }

    // Steam hammer (spec 10.5 and acceptance 23): over a coke firebox it heats two cold steel ingots itself,
    // waits for steam, then replays a plate pattern at one blow per 5 ticks at 2 bar and per 10 at 1.5 bar.
    // The plates come out with the pattern's full craft part, and the pieces never leave the hammer.

    private static void steamHammer(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos fireboxPos = helper.absolutePos(new BlockPos(4, 1, 4)), hammerPos = fireboxPos.above();
        level.setBlock(fireboxPos, Tier4Blocks.FIREBOX.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(hammerPos, Tier4Blocks.STEAM_HAMMER.get().defaultBlockState()
                .setValue(dev.strataindustria.steam.SteamHammerBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);
        FireboxBlockEntity firebox = (FireboxBlockEntity) level.getBlockEntity(fireboxPos);
        var hammer = (dev.strataindustria.steam.SteamHammerBlockEntity) level.getBlockEntity(hammerPos);

        ItemStack ingot = new ItemStack(ModItems.ingot(Metal.STEEL));
        Item steelPlate = ModItems.PLATES.get(Metal.STEEL).get();
        var plate = level.recipeAccess().recipeMap()
                .getRecipesFor(dev.strataindustria.registry.ModRecipes.ANVIL.get(), new net.minecraft.world.item.crafting.SingleRecipeInput(ingot), level)
                .filter(r -> r.value().result().create().is(steelPlate))
                .findFirst().orElseThrow(() -> helper.assertionException("no steel plate recipe"));
        int target = dev.strataindustria.smithing.Smithing.target(level, plate.id(), plate.value());
        java.util.List<dev.strataindustria.smithing.HitType> hits = ModGameTests.solve(target, plate.value().rules());
        helper.assertTrue(!hits.isEmpty(), "the plate should be solvable");
        int craft = dev.strataindustria.smithing.Smithing.craftQuality(hits.size(),
                dev.strataindustria.smithing.Smithing.minHits(target, plate.value().rules()));
        ItemStack pattern = new ItemStack(ModItems.SMITHING_PATTERN.get());
        helper.assertTrue(!hammer.canPlaceItem(dev.strataindustria.smithing.AnvilBlockEntity.PATTERN, pattern),
                "a blank pattern does not go in");
        pattern.set(ModDataComponents.SMITHING_PATTERN.get(), new dev.strataindustria.smithing.SmithingPattern(plate.id(),
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(steelPlate), target, hits.stream().map(Enum::ordinal).toList(), craft));
        hammer.setItem(dev.strataindustria.smithing.AnvilBlockEntity.PATTERN, pattern);
        int queue = dev.strataindustria.steam.SteamHammerBlockEntity.QUEUE, result = dev.strataindustria.steam.SteamHammerBlockEntity.RESULT;
        hammer.setItem(queue, ingot.copyWithCount(plate.value().count() * 2));

        hammerTick(level, fireboxPos, firebox, hammerPos, hammer, 2);
        helper.assertValueEqual(hammer.status(), dev.strataindustria.steam.SteamHammerBlockEntity.Status.TOO_COLD, "status over a cold firebox");
        helper.assertTrue(!hammer.input().isEmpty(), "the first ingot is on the anvil");
        firebox.setItem(0, new ItemStack(Tier4Items.COKE.get(), 8));
        firebox.preheat(1600.0f);
        hammerTick(level, fireboxPos, firebox, hammerPos, hammer, 5);
        helper.assertValueEqual(hammer.status(), dev.strataindustria.steam.SteamHammerBlockEntity.Status.HEATING, "status while the ingot heats");
        int tick = 0;
        for (; tick < 1000 && hammer.status() == dev.strataindustria.steam.SteamHammerBlockEntity.Status.HEATING; tick++) {
            hammerTick(level, fireboxPos, firebox, hammerPos, hammer, 1);
        }
        helper.assertValueEqual(hammer.status(), dev.strataindustria.steam.SteamHammerBlockEntity.Status.NO_STEAM,
                "status once hot, after " + tick + " ticks");

        // Full pressure: one blow per 5 ticks.
        int ticks = 0;
        for (; ticks < 1000 && hammer.getItem(result).isEmpty(); ticks++) steamTick(level, fireboxPos, firebox, hammerPos, hammer, 2.0f);
        helper.assertTrue(hammer.getItem(result).is(steelPlate), "a steel plate comes out, got " + hammer.getItem(result)
                + ", status " + hammer.status());
        helper.assertTrue(ticks <= hits.size() * 5 + 2, hits.size() + " blows at 2 bar took " + ticks + " ticks");
        var quality = hammer.getItem(result).get(ModDataComponents.QUALITY.get());
        helper.assertTrue(quality != null && quality.craft() == craft, "the plate carries the pattern's craft part " + craft + ", got " + quality);

        // Half pressure: the second, cold ingot heats again, then one blow per 10 ticks.
        int working = 0;
        for (ticks = 0; ticks < 3000 && hammer.getItem(result).getCount() < 2; ticks++) {
            steamTick(level, fireboxPos, firebox, hammerPos, hammer, 1.5f);
            if (hammer.status() == dev.strataindustria.steam.SteamHammerBlockEntity.Status.WORKING) working++;
        }
        helper.assertValueEqual(hammer.getItem(result).getCount(), 2, "plates after the second run, status " + hammer.status());
        helper.assertTrue(working >= hits.size() * 10 - 1 && working <= hits.size() * 10 + 2,
                hits.size() + " blows at 1.5 bar worked for " + working + " ticks");
        helper.assertTrue(hammer.getItem(queue).isEmpty() && hammer.input().isEmpty(), "both ingots used up");
        helper.succeed();
    }

    private static void hammerTick(ServerLevel level, BlockPos fireboxPos, FireboxBlockEntity firebox, BlockPos hammerPos,
            dev.strataindustria.steam.SteamHammerBlockEntity hammer, int ticks) {
        for (int i = 0; i < ticks; i++) {
            FireboxBlockEntity.serverTick(level, fireboxPos, level.getBlockState(fireboxPos), firebox);
            dev.strataindustria.steam.SteamHammerBlockEntity.serverTick(level, hammerPos, level.getBlockState(hammerPos), hammer);
        }
    }

    /** One tick with steam fed into the back of a north-facing hammer, as a boiler's pipe would. */
    private static void steamTick(ServerLevel level, BlockPos fireboxPos, FireboxBlockEntity firebox, BlockPos hammerPos,
            dev.strataindustria.steam.SteamHammerBlockEntity hammer, float pressure) {
        hammer.fill(Direction.SOUTH, Tier4Fluids.STEAM.get(), dev.strataindustria.steam.SteamHammerBlockEntity.STEAM_USE, pressure, false);
        hammerTick(level, fireboxPos, firebox, hammerPos, hammer, 1);
    }

    private static void smelt(ServerLevel level, BlockPos fireboxPos, FireboxBlockEntity firebox, BlockPos smelterPos,
            dev.strataindustria.metal.SmelterBlockEntity smelter, int ticks) {
        for (int i = 0; i < ticks; i++) {
            FireboxBlockEntity.serverTick(level, fireboxPos, level.getBlockState(fireboxPos), firebox);
            dev.strataindustria.metal.SmelterBlockEntity.serverTick(level, smelterPos, level.getBlockState(smelterPos), smelter);
        }
    }

    private static void roast(ServerLevel level, BlockPos fireboxPos, FireboxBlockEntity firebox, BlockPos roasterPos,
            dev.strataindustria.roasting.RoasterBlockEntity roaster, int ticks) {
        for (int i = 0; i < ticks; i++) {
            FireboxBlockEntity.serverTick(level, fireboxPos, level.getBlockState(fireboxPos), firebox);
            dev.strataindustria.roasting.RoasterBlockEntity.serverTick(level, roasterPos, level.getBlockState(roasterPos), roaster);
        }
    }

    private static void fire(ServerLevel level, BlockPos fireboxPos, FireboxBlockEntity firebox, BlockPos kilnPos,
            dev.strataindustria.ceramics.KilnBlockEntity kiln, int ticks) {
        for (int i = 0; i < ticks; i++) {
            FireboxBlockEntity.serverTick(level, fireboxPos, level.getBlockState(fireboxPos), firebox);
            dev.strataindustria.ceramics.KilnBlockEntity.serverTick(level, kilnPos, level.getBlockState(kilnPos), kiln);
        }
    }

    private static void preheat(ServerLevel level, BlockPos fireboxPos, FireboxBlockEntity firebox, BlockPos controllerPos,
            dev.strataindustria.ironworks.ConverterBlockEntity converter, int ticks) {
        for (int i = 0; i < ticks; i++) {
            FireboxBlockEntity.serverTick(level, fireboxPos, level.getBlockState(fireboxPos), firebox);
            blow(level, controllerPos, converter, 1);
        }
    }

    private static void blow(ServerLevel level, BlockPos pos, dev.strataindustria.ironworks.ConverterBlockEntity converter, int ticks) {
        for (int i = 0; i < ticks; i++) {
            dev.strataindustria.ironworks.ConverterBlockEntity.serverTick(level, pos, level.getBlockState(pos), converter);
        }
    }

    private static void smelt(ServerLevel level, BlockPos pos, dev.strataindustria.ironworks.BlastFurnaceBlockEntity furnace, int ticks) {
        for (int i = 0; i < ticks; i++) {
            dev.strataindustria.ironworks.BlastFurnaceBlockEntity.serverTick(level, pos, level.getBlockState(pos), furnace);
        }
    }

    /** A steam engine at {@code pos} with its shaft toward {@code facing}, given 2.5 bar steam once so it runs at 32 RPM. */
    private static void drive(ServerLevel level, BlockPos pos, Direction facing, BlockPos machine) {
        level.setBlock(pos, Tier4Blocks.STEAM_ENGINE.get().defaultBlockState().setValue(SteamEngineBlock.FACING, facing), Block.UPDATE_ALL);
        SteamEngineBlockEntity engine = (SteamEngineBlockEntity) level.getBlockEntity(pos);
        engine.fill(facing.getOpposite(), Tier4Fluids.STEAM.get(), SteamEngineBlockEntity.BUFFER, 2.5f, false);
        SteamEngineBlockEntity.serverTick(level, pos, level.getBlockState(pos), engine);
        KineticNetworks.rebuildNow(level, machine);
    }

    private static void wash(ServerLevel level, BlockPos pos, WasherBlockEntity washer, int ticks) {
        for (int i = 0; i < ticks; i++) ProcessingBlockEntity.serverTick(level, pos, level.getBlockState(pos), washer);
    }

    private static void crush(ServerLevel level, BlockPos pos, CrusherBlockEntity crusher, int ticks) {
        for (int i = 0; i < ticks; i++) ProcessingBlockEntity.serverTick(level, pos, level.getBlockState(pos), crusher);
    }

    private static int count(ProcessingBlockEntity machine, net.minecraft.world.item.Item item) {
        int n = 0;
        for (int i = 0; i < machine.getContainerSize(); i++) if (machine.getItem(i).is(item)) n += machine.getItem(i).getCount();
        return n;
    }

    private static void engine(ServerLevel level, BlockPos fireboxPos, FireboxBlockEntity firebox, BlockPos boilerPos, BoilerBlockEntity boiler,
            BlockPos enginePos, SteamEngineBlockEntity engine, int ticks) {
        for (int i = 0; i < ticks; i++) {
            FireboxBlockEntity.serverTick(level, fireboxPos, level.getBlockState(fireboxPos), firebox);
            BoilerBlockEntity.serverTick(level, boilerPos, level.getBlockState(boilerPos), boiler);
            SteamEngineBlockEntity.serverTick(level, enginePos, level.getBlockState(enginePos), engine);
        }
    }

    private static void pump(ServerLevel level, BlockPos pos, MechanicalPumpBlockEntity pump, int ticks) {
        for (int i = 0; i < ticks; i++) MechanicalPumpBlockEntity.serverTick(level, pos, level.getBlockState(pos), pump);
    }

    private static void steam(ServerLevel level, BlockPos fireboxPos, FireboxBlockEntity firebox, BlockPos boilerPos, BoilerBlockEntity boiler,
            int ticks) {
        for (int i = 0; i < ticks && level.getBlockEntity(boilerPos) == boiler; i++) {
            FireboxBlockEntity.serverTick(level, fireboxPos, level.getBlockState(fireboxPos), firebox);
            BoilerBlockEntity.serverTick(level, boilerPos, level.getBlockState(boilerPos), boiler);
        }
    }

    private static void heat(ServerLevel level, BlockPos forgePos, ForgeBlockEntity forge, BlockPos cruciblePos, CrucibleBlockEntity crucible,
            java.util.function.BooleanSupplier done) {
        for (int tick = 0; tick < 20000 && !done.getAsBoolean(); tick++) {
            ForgeBlockEntity.serverTick(level, forgePos, level.getBlockState(forgePos), forge);
            CrucibleBlockEntity.serverTick(level, cruciblePos, level.getBlockState(cruciblePos), crucible);
        }
    }

    private static void run(ServerLevel level, BlockPos pos, CokeOvenBlockEntity oven, int ticks) {
        for (int i = 0; i < ticks; i++) CokeOvenBlockEntity.serverTick(level, pos, level.getBlockState(pos), oven);
    }

    private static Melt melt(net.minecraft.world.item.Item item, int count) {
        Melt one = MetalContent.of(new ItemStack(item)).orElseThrow();
        Melt total = Melt.EMPTY;
        for (int i = 0; i < count; i++) total = total.plus(one);
        return total;
    }
}
