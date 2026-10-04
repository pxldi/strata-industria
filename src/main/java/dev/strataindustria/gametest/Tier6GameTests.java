package dev.strataindustria.gametest;

import dev.strataindustria.oil.OilReservoir;
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
}
