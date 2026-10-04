package dev.strataindustria.gametest;

import dev.strataindustria.geology.OreGrade;
import dev.strataindustria.listening.KineticVoices;
import dev.strataindustria.listening.ListeningBlocks;
import dev.strataindustria.listening.ListeningSounds;
import dev.strataindustria.listening.SteamWhistleBlock;
import dev.strataindustria.listening.TapTest;
import dev.strataindustria.metal.Quality;
import dev.strataindustria.power.Kinetic;
import dev.strataindustria.registry.ModBlocks;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Listening kit tests (uniqueness 2.1), run by {@link ModGameTests}. */
final class ListeningGameTests {
    private ListeningGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("listening_tap_test", ListeningGameTests::tapTest);
        tests.put("listening_kinetic_voices", ListeningGameTests::kineticVoices);
        tests.put("listening_steam_whistle", ListeningGameTests::steamWhistle);
    }

    // Rich ore and fine castings ring, plain ones knock, poor ore and flawed castings thud.
    private static void tapTest(GameTestHelper helper) {
        helper.assertTrue(TapTest.forGrade(OreGrade.RICH) == ListeningSounds.TAP_RING.get(), "rich ore rings");
        helper.assertTrue(TapTest.forGrade(OreGrade.NORMAL) == ListeningSounds.TAP_KNOCK.get(), "normal ore knocks");
        helper.assertTrue(TapTest.forGrade(OreGrade.POOR) == ListeningSounds.TAP_THUD.get(), "poor ore thuds");
        helper.assertTrue(TapTest.forQuality(new Quality(Quality.CAST)) == ListeningSounds.TAP_THUD.get(), "a casting thuds");
        helper.assertTrue(TapTest.forQuality(new Quality(0)) == ListeningSounds.TAP_KNOCK.get(), "a plain strike knocks");
        helper.assertTrue(TapTest.forQuality(new Quality(4)) == ListeningSounds.TAP_RING.get(), "a bright strike rings");
        helper.succeed();
    }

    // Gearboxes tick from half load, belts and wheels only complain near the limit, and a lighter load goes quiet again.
    private static void kineticVoices(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlock(pos, ModBlocks.WOODEN_GEARBOX.get().defaultBlockState(), Block.UPDATE_ALL);
        Kinetic gearbox = (Kinetic) level.getBlockEntity(pos);
        helper.assertTrue(KineticVoices.voiceOf(gearbox) == KineticVoices.Voice.GEAR, "a gearbox has the gear voice");
        helper.assertTrue(KineticVoices.speaks(KineticVoices.Voice.GEAR, 0.6f), "gears tick at 60% load");
        helper.assertTrue(!KineticVoices.speaks(KineticVoices.Voice.GEAR, 0.3f), "gears are quiet at 30% load");
        helper.assertTrue(!KineticVoices.speaks(KineticVoices.Voice.BELT, 0.6f), "belts are quiet at 60% load");
        helper.assertTrue(KineticVoices.speaks(KineticVoices.Voice.BELT, 0.9f), "belts squeal at 90% load");
        helper.assertTrue(KineticVoices.speaks(KineticVoices.Voice.WHEEL, 0.9f), "a wheel groans at 90% load");

        int before = KineticVoices.loudCount(level);
        KineticVoices.update(level, Map.of(pos, gearbox), true, 60, 100);
        helper.assertValueEqual(KineticVoices.loudCount(level), before + 1, "loud parts under load");
        KineticVoices.update(level, Map.of(pos, gearbox), true, 10, 100);
        helper.assertValueEqual(KineticVoices.loudCount(level), before, "loud parts under a light load");
        KineticVoices.update(level, Map.of(pos, gearbox), true, 60, 100);
        KineticVoices.update(level, Map.of(pos, gearbox), false, 60, 100);
        helper.assertValueEqual(KineticVoices.loudCount(level), before, "a stopped network is quiet");
        helper.succeed();
    }

    // The whistle takes its blast from redstone and stops with it.
    private static void steamWhistle(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlock(pos, ListeningBlocks.STEAM_WHISTLE.get().defaultBlockState(), Block.UPDATE_ALL);
        helper.assertValueEqual(level.getBlockState(pos).getValue(SteamWhistleBlock.POWERED), false, "whistle without a signal");
        level.setBlock(pos.east(), Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        helper.assertValueEqual(level.getBlockState(pos).getValue(SteamWhistleBlock.POWERED), true, "whistle with a signal");
        level.setBlock(pos.east(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        helper.assertValueEqual(level.getBlockState(pos).getValue(SteamWhistleBlock.POWERED), false, "whistle after the signal ends");
        helper.succeed();
    }
}
