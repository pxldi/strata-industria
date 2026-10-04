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
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
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
