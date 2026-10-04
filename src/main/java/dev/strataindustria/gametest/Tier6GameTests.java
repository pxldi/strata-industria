package dev.strataindustria.gametest;

import dev.strataindustria.oil.OilReservoir;
import dev.strataindustria.oil.OilStillBlockEntity;
import dev.strataindustria.oil.PumpJackBlock;
import dev.strataindustria.oil.PumpJackBlockEntity;
import dev.strataindustria.oil.OilReservoirData;
import dev.strataindustria.oil.SeepFeature;
import dev.strataindustria.registry.Tier6Blocks;
import dev.strataindustria.registry.Tier6Fluids;
import dev.strataindustria.registry.Tier6Items;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.geology.VeinType;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/** Tier 6 (industrial) tests, run by {@link ModGameTests}. */
final class Tier6GameTests {
    private Tier6GameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("tier6_reservoir_roll", Tier6GameTests::reservoirRoll);
        tests.put("tier6_reservoir_drain", Tier6GameTests::reservoirDrain);
        tests.put("tier6_crude_oil", Tier6GameTests::crudeOil);
        tests.put("tier6_seep_pool", Tier6GameTests::seepPool);
        tests.put("tier6_bauxite_bed", Tier6GameTests::bauxiteBed);
        tests.put("tier6_oil_still", Tier6GameTests::oilStill);
        tests.put("tier6_oil_still_full_tank", Tier6GameTests::oilStillFullTank);
        tests.put("tier6_liquid_fuels", Tier6GameTests::liquidFuels);
        tests.put("tier6_seismic_charge", Tier6GameTests::seismicCharge);
        tests.put("tier6_seismic_survey", Tier6GameTests::seismicSurvey);
        tests.put("tier6_wellhead_drilling", Tier6GameTests::wellheadDrilling);
        tests.put("tier6_wellhead_remembers_bore", Tier6GameTests::wellheadRemembersBore);
        tests.put("tier6_pump_jack", Tier6GameTests::pumpJack);
    }

    /** A site with a fixed chance and flat ground at Y 40; seeps allowed only west of x = 0. */
    private static OilReservoir.Site site(double chance) {
        return new OilReservoir.Site() {
            @Override
            public double chance(int x, int z) {
                return chance;
            }

            @Override
            public boolean seepsPossible(int x, int z) {
                return x < 0;
            }

            @Override
            public int surface(int x, int z) {
                return 40;
            }
        };
    }

    // Spec 19.1: the roll is deterministic, the footprint sits inside its cell, and the numbers stay in range.
    private static void reservoirRoll(GameTestHelper helper) {
        OilReservoir.Site site = site(0.5);
        int found = 0, withSeeps = 0, westOfZero = 0;
        for (int cx = -10; cx < 10; cx++) {
            for (int cz = -10; cz < 10; cz++) {
                Optional<OilReservoir> roll = OilReservoir.roll(1234L, cx, cz, site);
                helper.assertValueEqual(roll, OilReservoir.roll(1234L, cx, cz, site), "the same seed rolls the same reservoir");
                if (roll.isEmpty()) continue;
                OilReservoir r = roll.get();
                found++;
                helper.assertTrue(!r.chunks().isEmpty() && r.chunks().size() <= 36, "footprint of " + r.chunks().size() + " chunks");
                for (ChunkPos chunk : r.chunks()) {
                    helper.assertTrue(r.contains(chunk), "every footprint chunk is inside the reservoir");
                }
                helper.assertTrue(r.topY() >= OilReservoir.MIN_TOP_Y && r.topY() <= 40 - OilReservoir.MIN_COVER,
                        "top at Y " + r.topY() + " is in range and 32 below the surface");
                long low = Math.round(r.chunks().size() * OilReservoir.MB_PER_CHUNK * 0.7) - 1;
                long high = Math.round(r.chunks().size() * OilReservoir.MB_PER_CHUNK * 1.3) + 1;
                helper.assertTrue(r.capacity() >= low && r.capacity() <= high, "capacity " + r.capacity());
                if (r.centreBlockX() < 0) westOfZero++;
                if (!r.seeps().isEmpty()) {
                    withSeeps++;
                    helper.assertTrue(r.centreBlockX() < 0, "seeps only where the site allows them");
                    for (OilReservoir.Seep seep : r.seeps()) {
                        helper.assertTrue(seep.size() >= 2 && seep.size() <= 6, "seep of " + seep.size() + " blocks");
                        helper.assertTrue(r.contains(seep.chunk()), "seeps lie inside the footprint");
                        int lx = seep.x() & 15, lz = seep.z() & 15;
                        helper.assertTrue(lx >= 4 && lx <= 11 && lz >= 4 && lz <= 11, "seeps keep clear of chunk edges");
                    }
                }
            }
        }
        helper.assertTrue(found > 150 && found < 250, found + " reservoirs in 400 cells at a chance of 0.5");
        double seepShare = (double) withSeeps / westOfZero;
        helper.assertTrue(seepShare > 0.2 && seepShare < 0.5, "seep share " + seepShare + " should be near 0.35");
        helper.assertTrue(OilReservoir.roll(1234L, 3, 3, site(0)).isEmpty(), "a chance of 0 rolls nothing");
        helper.succeed();
    }

    // Spec 5.4 and 19.1.1: a reservoir starts full, drains by what is taken and never goes below zero.
    private static void reservoirDrain(GameTestHelper helper) {
        OilReservoir r = OilReservoir.roll(99L, 0, 0, site(1)).orElseThrow();
        OilReservoirData data = new OilReservoirData();
        helper.assertValueEqual(data.remaining(r), r.capacity(), "an untapped reservoir is full");
        helper.assertValueEqual(data.drain(r, 1000), 1000L, "drained");
        helper.assertValueEqual(data.remaining(r), r.capacity() - 1000, "remaining after one bucket");
        helper.assertTrue(data.isDirty(), "draining marks the data for saving");
        helper.assertValueEqual(data.drain(r, Long.MAX_VALUE), r.capacity() - 1000, "the rest comes out");
        helper.assertValueEqual(data.remaining(r), 0L, "empty");
        helper.assertValueEqual(data.drain(r, 500), 0L, "nothing more");
        helper.assertTrue(data.fraction(r) == 0, "fraction at zero");
        helper.succeed();
    }

    // Spec 5.1: crude oil flows three blocks, slowly, makes no new sources, slows walkers and fills a bucket.
    private static void crudeOil(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos source = new BlockPos(4, 1, 4);
        helper.setBlock(source, Tier6Blocks.CRUDE_OIL.get());
        helper.assertTrue(Tier6Blocks.CRUDE_OIL.get().getSpeedFactor() == 0.4f, "crude oil slows entities like honey");
        helper.runAfterDelay(30 * 5, () -> {
            FluidState third = helper.getBlockState(new BlockPos(7, 1, 4)).getFluidState();
            helper.assertTrue(third.is(Tier6Fluids.CRUDE_OIL.flowing().get()), "crude oil reaches three blocks out");
            helper.assertTrue(helper.getBlockState(new BlockPos(8, 1, 4)).getFluidState().isEmpty(), "but not four");
            BlockPos abs = helper.absolutePos(source);
            BlockState state = level.getBlockState(abs);
            ItemStack bucket = Tier6Blocks.CRUDE_OIL.get().pickupBlock(null, level, abs, state);
            helper.assertTrue(bucket.is(Tier6Items.CRUDE_OIL_BUCKET.get()), "a bucket picks up crude oil");
            helper.assertTrue(level.getFluidState(abs).isEmpty() || !level.getFluidState(abs).isSource(), "the source is gone");
            helper.succeed();
        });
    }

    // Spec 19.1.5: a seep is a sealed pool of 2 to 6 sources one block deep that never spills.
    // Bauxite (tier 6 spec 19.4): the bed drops bauxite, digs with a shovel, and its vein is limited to hot biomes.
    private static void bauxiteBed(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlock(pos, ModBlocks.BAUXITE_BED.get().defaultBlockState(), Block.UPDATE_ALL);
        List<ItemStack> drops = Block.getDrops(level.getBlockState(pos), level, pos, null);
        helper.assertValueEqual(drops.size(), 1, "one drop");
        helper.assertTrue(drops.get(0).is(ModItems.BAUXITE.get()) && drops.get(0).getCount() == 3, "three bauxite");
        helper.assertTrue(level.getBlockState(pos).is(BlockTags.MINEABLE_WITH_SHOVEL), "the bed digs with a shovel");

        var vein = level.registryAccess().lookupOrThrow(VeinType.REGISTRY)
                .getOrThrow(ResourceKey.create(VeinType.REGISTRY, StrataIndustria.id("bauxite"))).value();
        helper.assertTrue(vein.pass() == VeinType.Pass.SEDIMENT && vein.maxDepth() == 10, "a shallow sediment bed");
        var hot = vein.biomes().orElseThrow(() -> helper.assertionException("the bauxite vein has no biome limit"));
        var biomes = level.registryAccess().lookupOrThrow(Registries.BIOME);
        for (var hotBiome : List.of(Biomes.SAVANNA, Biomes.JUNGLE, Biomes.BADLANDS))
            helper.assertTrue(hot.contains(biomes.getOrThrow(hotBiome)), hotBiome.identifier() + " hosts bauxite");
        for (var coldBiome : List.of(Biomes.PLAINS, Biomes.TAIGA, Biomes.DESERT))
            helper.assertTrue(!hot.contains(biomes.getOrThrow(coldBiome)), coldBiome.identifier() + " does not");
        helper.succeed();
    }

    private static void seepPool(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK);
                if ((x + z) % 3 == 0) helper.setBlock(new BlockPos(x, 2, z), Blocks.SHORT_GRASS);
            }
        }
        BlockPos centre = helper.absolutePos(new BlockPos(4, 1, 4));
        int placed = SeepFeature.placePool(level, centre.getX(), centre.getZ(), 6, RandomSource.create(42), (x, z) -> centre.getY());
        helper.assertValueEqual(placed, 6, "sources placed");
        helper.runAfterDelay(80, () -> {
            int sources = 0;
            for (int x = 0; x < 9; x++) {
                for (int y = 0; y < 4; y++) {
                    for (int z = 0; z < 9; z++) {
                        FluidState fluid = helper.getBlockState(new BlockPos(x, y, z)).getFluidState();
                        if (fluid.isEmpty()) continue;
                        helper.assertTrue(fluid.isSource(), "the pool spills nothing at " + x + " " + y + " " + z);
                        helper.assertValueEqual(y, 1, "the pool is one block deep");
                        sources++;
                    }
                }
            }
            helper.assertValueEqual(sources, 6, "sources in the pool");
            helper.succeed();
        });
    }

    private static void still(ServerLevel level, BlockPos fireboxPos, dev.strataindustria.steam.FireboxBlockEntity firebox, BlockPos stillPos,
            OilStillBlockEntity still, int ticks) {
        for (int i = 0; i < ticks; i++) {
            dev.strataindustria.steam.FireboxBlockEntity.serverTick(level, fireboxPos, level.getBlockState(fireboxPos), firebox);
            OilStillBlockEntity.serverTick(level, stillPos, level.getBlockState(stillPos), still);
        }
    }

    // Spec 5.5: a still on a coke firebox waits for heat, then turns 1000 mB of crude into 200 naphtha, 300 diesel and
    // 400 heavy oil in 1200 ticks at 30 HU/t; it takes crude only, and a bucket of diesel can be filled from the screen.
    private static void oilStill(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos fireboxPos = helper.absolutePos(new BlockPos(2, 1, 2)), stillPos = fireboxPos.above();
        level.setBlock(fireboxPos, dev.strataindustria.registry.Tier4Blocks.FIREBOX.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(stillPos, Tier6Blocks.OIL_STILL.get().defaultBlockState(), Block.UPDATE_ALL);
        var firebox = (dev.strataindustria.steam.FireboxBlockEntity) level.getBlockEntity(fireboxPos);
        var still = (OilStillBlockEntity) level.getBlockEntity(stillPos);
        firebox.setItem(0, new ItemStack(dev.strataindustria.registry.Tier4Items.COKE.get(), 8));
        firebox.preheat(1600.0f);

        helper.assertValueEqual(still.fill(net.minecraft.core.Direction.UP, net.minecraft.world.level.material.Fluids.WATER, 1000, 0, false), 0,
                "water does not distil");
        helper.assertValueEqual(still.fill(net.minecraft.core.Direction.UP, Tier6Fluids.CRUDE_OIL.source().get(), 6000, 0, false), 4000,
                "the crude tank holds 4000 mB");
        helper.assertValueEqual(still.fill(net.minecraft.core.Direction.UP, Tier6Fluids.DIESEL.source().get(), 1000, 0, false), 0,
                "products do not go back in");
        still.setTank(0, Tier6Fluids.CRUDE_OIL.source().get(), 2000);

        still(level, fireboxPos, firebox, stillPos, still, 1);
        helper.assertValueEqual(still.status(), OilStillBlockEntity.Status.NEEDS_HEAT, "status before the heat arrives");
        still(level, fireboxPos, firebox, stillPos, still, 100);
        helper.assertValueEqual(still.status(), OilStillBlockEntity.Status.DISTILLING, "status while distilling");
        helper.assertValueEqual(firebox.taken(), OilStillBlockEntity.HEAT, "HU/t the still draws");
        helper.assertTrue(level.getBlockState(stillPos).getValue(dev.strataindustria.oil.OilStillBlock.LIT), "lit while it works");
        helper.assertValueEqual(still.amount(0), 2000, "the batch is taken when it is done");
        still(level, fireboxPos, firebox, stillPos, still, 1100);
        helper.assertValueEqual(still.amount(0), 1000, "1000 mB of crude used");
        helper.assertValueEqual(still.amount(1), 200, "naphtha");
        helper.assertValueEqual(still.amount(2), 300, "diesel");
        helper.assertValueEqual(still.amount(3), 400, "heavy oil");
        helper.assertTrue(still.fluid(2).isSame(Tier6Fluids.DIESEL.source().get()), "the middle tank holds diesel");
        helper.assertValueEqual(still.batches(), 1, "one batch");
        still(level, fireboxPos, firebox, stillPos, still, 1250);
        helper.assertValueEqual(still.amount(0), 0, "second batch");
        helper.assertValueEqual(still.amount(2), 600, "twice the diesel");
        helper.assertValueEqual(still.status(), OilStillBlockEntity.Status.EMPTY, "empty when the crude is gone");

        var player = helper.makeMockServerPlayerInLevel();
        player.getInventory().add(new ItemStack(net.minecraft.world.item.Items.BUCKET, 2));
        helper.assertTrue(!still.takeBucket(player, null, 0), "naphtha 400 is less than a bucket");
        still.setTank(2, Tier6Fluids.DIESEL.source().get(), 1500);
        helper.assertTrue(still.takeBucket(player, null, 1), "a bucket of diesel from the screen");
        helper.assertValueEqual(still.amount(2), 500, "1000 mB left the tank");
        helper.assertTrue(player.getInventory().contains(new ItemStack(Tier6Items.DIESEL_BUCKET.get())), "the diesel bucket is in the pack");
        helper.succeed();
    }

    // Spec 5.5: a full product tank pauses the still and says which one.
    private static void oilStillFullTank(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos fireboxPos = helper.absolutePos(new BlockPos(2, 1, 2)), stillPos = fireboxPos.above();
        level.setBlock(fireboxPos, dev.strataindustria.registry.Tier4Blocks.FIREBOX.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(stillPos, Tier6Blocks.OIL_STILL.get().defaultBlockState(), Block.UPDATE_ALL);
        var firebox = (dev.strataindustria.steam.FireboxBlockEntity) level.getBlockEntity(fireboxPos);
        var still = (OilStillBlockEntity) level.getBlockEntity(stillPos);
        firebox.setItem(0, new ItemStack(dev.strataindustria.registry.Tier4Items.COKE.get(), 8));
        firebox.preheat(1600.0f);
        still.setTank(0, Tier6Fluids.CRUDE_OIL.source().get(), 1000);
        still.setTank(2, Tier6Fluids.DIESEL.source().get(), 3900);
        still(level, fireboxPos, firebox, stillPos, still, 1300);
        helper.assertValueEqual(still.status(), OilStillBlockEntity.Status.TANK_FULL, "paused by the full diesel tank");
        helper.assertTrue(still.fullFluid().isSame(Tier6Fluids.DIESEL.source().get()), "it names diesel");
        helper.assertValueEqual(still.amount(0), 1000, "no crude lost");
        still.setTank(2, Tier6Fluids.DIESEL.source().get(), 0);
        still(level, fireboxPos, firebox, stillPos, still, 1250);
        helper.assertValueEqual(still.amount(2), 300, "runs again once there is room");
        helper.succeed();
    }

    // Spec 7.3: diesel burns in a generator (32 J a mB) and a burner, heavy oil and crude only in a burner, and the
    // burner takes refinery gas too.
    private static void liquidFuels(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos generatorPos = helper.absolutePos(new BlockPos(1, 1, 1)), burnerPos = helper.absolutePos(new BlockPos(3, 1, 1));
        level.setBlock(generatorPos, dev.strataindustria.registry.Tier5Blocks.COMBUSTION_GENERATOR.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(burnerPos, dev.strataindustria.registry.Tier5Blocks.LIQUID_FUEL_BURNER.get().defaultBlockState(), Block.UPDATE_ALL);
        var generator = (dev.strataindustria.electric.CombustionGeneratorBlockEntity) level.getBlockEntity(generatorPos);
        var burner = (dev.strataindustria.electric.LiquidFuelBurnerBlockEntity) level.getBlockEntity(burnerPos);
        var up = net.minecraft.core.Direction.UP;
        helper.assertValueEqual(dev.strataindustria.power.LiquidFuel.of(Tier6Fluids.DIESEL.source().get()).joulesPerMb(), 32.0, "diesel J a mB");
        helper.assertValueEqual(generator.fill(up, Tier6Fluids.HEAVY_OIL.source().get(), 1000, 0, false), 0, "heavy oil is too heavy for an engine");
        helper.assertValueEqual(generator.fill(up, Tier6Fluids.CRUDE_OIL.source().get(), 1000, 0, false), 0, "so is crude");
        helper.assertValueEqual(generator.fill(up, Tier6Fluids.DIESEL.source().get(), 1000, 0, false), 1000, "diesel runs a generator");
        helper.assertValueEqual(generator.fill(up, Tier6Fluids.REFINERY_GAS.source().get(), 1000, 0, false), 0, "and not mixed with gas");
        helper.assertValueEqual(burner.fill(up, Tier6Fluids.HEAVY_OIL.source().get(), 1000, 0, false), 1000, "heavy oil heats a burner");
        var heavy = dev.strataindustria.power.LiquidFuel.burnOf(Tier6Fluids.HEAVY_OIL.source().get());
        helper.assertTrue(heavy.huPerMb() == 24 && heavy.huPerTick() == 40 && heavy.maxTemperature() == 1500, "heavy oil burn values");
        var gas = dev.strataindustria.power.LiquidFuel.burnOf(Tier6Fluids.REFINERY_GAS.source().get());
        helper.assertTrue(gas.huPerMb() == 24 && gas.huPerTick() == 60 && gas.maxTemperature() == 1700, "refinery gas burn values");
        helper.succeed();
    }

    /** A reservoir of one chunk in an unused cell, its top {@code below} blocks under the wellhead of the test. */
    private static OilReservoir testReservoir(GameTestHelper helper, int cell, int below, long capacity) {
        BlockPos floor = helper.absolutePos(BlockPos.ZERO);
        return new OilReservoir(cell, cell, cell * 8 + 0.5, cell * 8 + 0.5, 1.0, 1.0, List.of(new ChunkPos(cell * 8, cell * 8)), floor.getY() + 1 - below,
                capacity, List.of());
    }

    // Spec 5.2: a charge lights with flint and steel, burns a fuse, thumps, breaks nothing, and an ore scanner within
    // 32 blocks records the survey while one at 40 blocks does not. On a part block it fizzles.
    private static void seismicCharge(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos ground = helper.absolutePos(new BlockPos(2, 1, 2)), charge = ground.above(), neighbour = ground.east();
        level.setBlock(ground, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(neighbour, Blocks.OAK_PLANKS.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(charge, Tier6Blocks.SEISMIC_CHARGE.get().defaultBlockState(), Block.UPDATE_ALL);
        var block = Tier6Blocks.SEISMIC_CHARGE.get();
        helper.assertTrue(block.canIgnite(level, charge, level.getBlockState(charge)), "an unlit charge can be lit");
        helper.assertTrue(block.ignite(level, charge, level.getBlockState(charge)), "it lights on solid ground");
        helper.assertTrue(level.getBlockState(charge).getValue(dev.strataindustria.oil.SeismicChargeBlock.LIT), "lit");
        helper.assertTrue(!block.ignite(level, charge, level.getBlockState(charge)), "not lit twice");

        var near = helper.makeMockServerPlayerInLevel();
        var far = helper.makeMockServerPlayerInLevel();
        near.setPos(charge.getX() + 0.5 + 20, charge.getY(), charge.getZ() + 0.5);
        far.setPos(charge.getX() + 0.5 + 40, charge.getY(), charge.getZ() + 0.5);
        near.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(dev.strataindustria.registry.Tier5Items.ORE_SCANNER.get()));
        far.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(dev.strataindustria.registry.Tier5Items.ORE_SCANNER.get()));
        helper.runAfterDelay(dev.strataindustria.oil.SeismicChargeBlock.FUSE_TICKS + 5, () -> {
            helper.assertTrue(level.getBlockState(charge).isAir(), "the charge is spent");
            helper.assertTrue(level.getBlockState(ground).is(Blocks.STONE) && level.getBlockState(neighbour).is(Blocks.OAK_PLANKS), "it breaks no block");
            helper.assertTrue(near.getMainHandItem().has(dev.strataindustria.registry.Tier6DataComponents.SEISMIC_RESULT.get()), "the scanner at 20 blocks recorded");
            helper.assertTrue(!far.getMainHandItem().has(dev.strataindustria.registry.Tier6DataComponents.SEISMIC_RESULT.get()), "the scanner at 40 blocks did not");

            BlockPos slab = helper.absolutePos(new BlockPos(5, 1, 2));
            level.setBlock(slab, Blocks.STONE_SLAB.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(slab.above(), Tier6Blocks.SEISMIC_CHARGE.get().defaultBlockState(), Block.UPDATE_ALL);
            helper.assertTrue(!block.ignite(level, slab.above(), level.getBlockState(slab.above())), "a charge on a slab fizzles");
            helper.assertTrue(!level.getBlockState(slab.above()).getValue(dev.strataindustria.oil.SeismicChargeBlock.LIT), "and stays unlit");
            helper.succeed();
        });
    }

    // Spec 5.2: the survey lists each reservoir in the 5 x 5 chunks once, with the tiles it covers, the edges it goes on
    // past, its size class and top, and what is left only once a well has struck it.
    private static void seismicSurvey(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        int cell = 9101;
        // A footprint two chunks wide that starts west of the area and runs on to the east of it.
        OilReservoir wide = new OilReservoir(cell, cell, 0, 0, 5, 1, List.of(new ChunkPos(-4, 0), new ChunkPos(-3, 0), new ChunkPos(-2, 0),
                new ChunkPos(-1, 0), new ChunkPos(0, 0), new ChunkPos(1, 0), new ChunkPos(2, 0), new ChunkPos(3, 0), new ChunkPos(4, 0)), -30,
                7_000_000L, List.of());
        OilReservoirData data = new OilReservoirData();
        BlockPos charge = new BlockPos(8, 70, 8);
        var survey = dev.strataindustria.oil.SeismicSurvey.build(charge, chunk -> chunk.z() == 0 && chunk.x() >= -4 && chunk.x() <= 4
                ? Optional.of(wide) : Optional.empty(), data);
        helper.assertValueEqual(survey.side(), 5, "5 x 5 chunks");
        helper.assertValueEqual(survey.chunkX(), -2, "origin chunk x");
        helper.assertValueEqual(survey.hits().size(), 1, "one reservoir, listed once");
        var hit = survey.hits().get(0);
        helper.assertValueEqual(hit.sizeClass(), OilReservoir.SizeClass.LARGE, "7 000 000 mB is large");
        helper.assertValueEqual(hit.topY(), -30, "top");
        helper.assertValueEqual(hit.remaining(), -1, "untapped: nothing known about what is left");
        for (int column = 0; column < 5; column++) {
            helper.assertTrue(hit.covers(5, column, 2) && !hit.covers(5, column, 1), "the middle row is covered at column " + column);
        }
        helper.assertValueEqual(hit.edges(), dev.strataindustria.oil.SeismicSurvey.EAST | dev.strataindustria.oil.SeismicSurvey.WEST,
                "it goes on past the east and west edges");
        data.setBore(wide, 0, 0, 10, true);
        data.drain(wide, 3_500_000L);
        var tapped = dev.strataindustria.oil.SeismicSurvey.build(charge, chunk -> chunk.z() == 0 ? Optional.of(wide) : Optional.empty(), data);
        helper.assertValueEqual(tapped.hits().get(0).remaining(), 50, "half left once a well has struck it");
        var codec = dev.strataindustria.oil.SeismicSurvey.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, tapped).getOrThrow();
        helper.assertValueEqual(dev.strataindustria.oil.SeismicSurvey.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, codec).getOrThrow(), tapped,
                "the survey survives saving");
        helper.assertValueEqual(dev.strataindustria.oil.SeismicSurvey.build(charge, chunk -> Optional.empty(), data).hits().size(), 0, "no reservoirs found");
        helper.succeed();
    }

    private static dev.strataindustria.oil.WellheadBlockEntity placeWellhead(GameTestHelper helper, BlockPos relative, OilReservoir reservoir) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(relative);
        level.setBlock(pos, Tier6Blocks.WELLHEAD.get().defaultBlockState(), Block.UPDATE_ALL);
        var well = (dev.strataindustria.oil.WellheadBlockEntity) level.getBlockEntity(pos);
        well.setReservoir(reservoir);
        return well;
    }

    private static void drillTicks(ServerLevel level, dev.strataindustria.oil.WellheadBlockEntity well, int ticks) {
        for (int i = 0; i < ticks; i++) {
            dev.strataindustria.oil.WellheadBlockEntity.serverTick(level, well.getBlockPos(), level.getBlockState(well.getBlockPos()), well);
        }
    }

    // Spec 5.3: a wellhead over no reservoir says so; one over a reservoir 8 blocks down needs rotation and casing, eats one
    // pipe per 4 blocks, takes 40 ticks a block at 16 RPM, and a second wellhead within 8 blocks refuses to drill.
    private static void wellheadDrilling(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos barePos = helper.absolutePos(new BlockPos(6, 1, 6));
        level.setBlock(barePos, Tier6Blocks.WELLHEAD.get().defaultBlockState(), Block.UPDATE_ALL);
        var bare = (dev.strataindustria.oil.WellheadBlockEntity) level.getBlockEntity(barePos);
        bare.setReservoir(null);
        drillTicks(level, bare, 1);
        helper.assertValueEqual(bare.status(), dev.strataindustria.oil.WellheadBlockEntity.Status.NO_RESERVOIR, "no reservoir below");

        OilReservoir reservoir = testReservoir(helper, 9102, 8, 4_000_000L);
        var well = placeWellhead(helper, new BlockPos(2, 1, 2), reservoir);
        drillTicks(level, well, 1);
        helper.assertValueEqual(well.status(), dev.strataindustria.oil.WellheadBlockEntity.Status.NOT_TURNING, "waits for rotation");
        well.kinetic().force(8);
        drillTicks(level, well, 1);
        helper.assertValueEqual(well.status(), dev.strataindustria.oil.WellheadBlockEntity.Status.TOO_SLOW, "8 RPM is too slow");
        well.kinetic().force(16);
        drillTicks(level, well, 1);
        helper.assertValueEqual(well.status(), dev.strataindustria.oil.WellheadBlockEntity.Status.NEEDS_CASING, "casing first");

        var pipes = new ItemStack(dev.strataindustria.registry.Tier4Items.STEEL_FLUID_PIPE.get(), 2);
        well.setCasing(pipes);
        drillTicks(level, well, 319);
        helper.assertValueEqual(well.bored(), 7, "7 of 8 blocks after 319 ticks at 16 RPM");
        helper.assertValueEqual(pipes.getCount(), 0, "both lengths of casing are down");
        helper.assertTrue(!well.drilled(), "not through yet");
        drillTicks(level, well, 2);
        helper.assertTrue(well.drilled(), "struck oil after 320 ticks");
        helper.assertValueEqual(well.gushing(), dev.strataindustria.oil.WellheadBlockEntity.GUSH_TICKS - 1, "it gushes for 100 ticks");
        helper.assertTrue(level.getBlockState(well.getBlockPos()).getValue(dev.strataindustria.oil.WellheadBlock.DRILLED), "the block shows it");

        var second = placeWellhead(helper, new BlockPos(6, 1, 2), reservoir);
        second.kinetic().force(16);
        drillTicks(level, second, 1);
        helper.assertValueEqual(second.status(), dev.strataindustria.oil.WellheadBlockEntity.Status.TOO_CLOSE, "4 blocks from the first well");
        helper.succeed();
    }

    // Spec 5.3: putting a drilled wellhead back on the same column keeps the bore and costs no casing.
    private static void wellheadRemembersBore(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        OilReservoir reservoir = testReservoir(helper, 9103, 4, 4_000_000L);
        var well = placeWellhead(helper, new BlockPos(2, 1, 2), reservoir);
        well.kinetic().force(16);
        well.setCasing(new ItemStack(dev.strataindustria.registry.Tier4Items.STEEL_FLUID_PIPE.get(), 1));
        drillTicks(level, well, 200);
        helper.assertTrue(well.drilled(), "drilled 4 blocks with one length of casing");
        BlockPos pos = well.getBlockPos();
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        var again = placeWellhead(helper, new BlockPos(2, 1, 2), reservoir);
        drillTicks(level, again, 2);
        helper.assertTrue(again.drilled() && level.getBlockState(pos).getValue(dev.strataindustria.oil.WellheadBlock.DRILLED),
                "the new wellhead is already drilled");
        helper.succeed();
    }

    // Spec 5.3 and 5.4: a full reservoir flows 4 mB/t by itself and needs a pump jack below 80%; a jack at 32 RPM lifts
    // 16 mB/t down to half full, 10 mB/t at a quarter and 4 mB/t when empty, and only what the outlet takes leaves the reservoir.
    private static void pumpJack(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.assertTrue(PumpJackBlockEntity.rate(32, 1.0) == 16.0f, "16 mB/t at 32 RPM");
        helper.assertTrue(PumpJackBlockEntity.rate(32, 0.5) == 16.0f, "still 16 at half full");
        helper.assertTrue(Math.abs(PumpJackBlockEntity.rate(32, 0.25) - 10.0f) < 0.01f, "10 mB/t at a quarter");
        helper.assertTrue(Math.abs(PumpJackBlockEntity.rate(32, 0.0) - 4.0f) < 0.01f, "4 mB/t when empty");
        helper.assertTrue(PumpJackBlockEntity.rate(200, 1.0) == PumpJackBlockEntity.rate(64, 1.0), "it holds at 64 RPM");

        OilReservoir reservoir = testReservoir(helper, 9104, 0, 1_000_000L);
        // Up in the open, above the roof of the test box, where the beam has room.
        helper.setBlock(new BlockPos(2, 39, 2), Blocks.STONE);
        helper.setBlock(new BlockPos(3, 39, 2), Blocks.STONE);
        var well = placeWellhead(helper, new BlockPos(2, 40, 2), reservoir);
        BlockPos wellPos = well.getBlockPos();
        level.setBlock(wellPos, level.getBlockState(wellPos).setValue(dev.strataindustria.oil.WellheadBlock.DRILLED, true), Block.UPDATE_ALL);
        OilReservoirData data = OilReservoirData.get(level);
        data.setBore(reservoir, wellPos.getX(), wellPos.getZ(), 0, true);
        BlockPos stillPos = wellPos.east();
        level.setBlock(stillPos, Tier6Blocks.OIL_STILL.get().defaultBlockState(), Block.UPDATE_ALL);
        var still = (OilStillBlockEntity) level.getBlockEntity(stillPos);
        drillTicks(level, well, 10);
        helper.assertValueEqual(still.amount(0), 40, "a full reservoir flows 4 mB/t with no pump " + well.status());
        helper.assertValueEqual(data.remaining(reservoir), 1_000_000L - 40, "and is drawn down by what flowed");
        data.drain(reservoir, 250_000L);
        drillTicks(level, well, 1);
        helper.assertValueEqual(well.status(), dev.strataindustria.oil.WellheadBlockEntity.Status.LOW_PRESSURE, "below 80% it needs a pump jack");
        helper.assertValueEqual(still.amount(0), 40, "and gives nothing more");

        BlockPos jackPos = wellPos.above();
        helper.assertTrue(PumpJackBlock.onDrilledWellhead(level, jackPos), "a drilled wellhead takes a pump jack");
        helper.assertTrue(PumpJackBlock.roomFor(level, jackPos, net.minecraft.core.Direction.NORTH), "with room for the beam");
        level.setBlock(jackPos.above(2), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        helper.assertTrue(!PumpJackBlock.roomFor(level, jackPos, net.minecraft.core.Direction.NORTH), "not with a block in the way");
        level.setBlock(jackPos.above(2), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(jackPos, Tier6Blocks.PUMP_JACK.get().defaultBlockState(), Block.UPDATE_ALL);
        var jack = (PumpJackBlockEntity) level.getBlockEntity(jackPos);
        jack.kinetic().force(32);
        still.setTank(0, Tier6Fluids.CRUDE_OIL.source().get(), 0);
        for (int i = 0; i < 10; i++) {
            PumpJackBlockEntity.serverTick(level, jackPos, level.getBlockState(jackPos), jack);
            drillTicks(level, well, 1);
        }
        helper.assertValueEqual(still.amount(0), 160, "a jack at 32 RPM lifts 16 mB/t");
        helper.assertValueEqual(well.status(), dev.strataindustria.oil.WellheadBlockEntity.Status.PUMPING, "the wellhead says so");
        helper.assertValueEqual(jack.status(), PumpJackBlockEntity.Status.PUMPING, "so does the jack");
        helper.assertTrue(level.getBlockState(jackPos).getValue(PumpJackBlock.RUNNING), "the beam moves");
        // A full outlet takes nothing and the reservoir is not drawn down.
        still.setTank(0, Tier6Fluids.CRUDE_OIL.source().get(), 4000);
        long before = data.remaining(reservoir);
        for (int i = 0; i < 5; i++) PumpJackBlockEntity.serverTick(level, jackPos, level.getBlockState(jackPos), jack);
        helper.assertValueEqual(data.remaining(reservoir), before, "nothing lifted into a full outlet");
        helper.assertValueEqual(jack.status(), PumpJackBlockEntity.Status.NO_OUTLET, "the jack reports it");
        // An empty reservoir is a stripper well.
        data.drain(reservoir, Long.MAX_VALUE);
        still.setTank(0, Tier6Fluids.CRUDE_OIL.source().get(), 0);
        for (int i = 0; i < 100; i++) PumpJackBlockEntity.serverTick(level, jackPos, level.getBlockState(jackPos), jack);
        helper.assertTrue(Math.abs(still.amount(0) - 400) <= 4, "an empty reservoir still gives 4 mB/t, got " + still.amount(0));
        helper.assertValueEqual(jack.status(), PumpJackBlockEntity.Status.STRIPPER, "stripper well");
        helper.succeed();
    }
}
