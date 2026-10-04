package dev.strataindustria.gametest;

import dev.strataindustria.bronze.BellBlock;
import dev.strataindustria.bronze.BellBlockEntity;
import dev.strataindustria.bronze.BellTone;
import dev.strataindustria.bronze.BronzeRegistry;
import dev.strataindustria.material.Metal;
import dev.strataindustria.metal.CastMoldItem;
import dev.strataindustria.metal.Melt;
import dev.strataindustria.metal.Quality;
import dev.strataindustria.registry.ModDataComponents;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Bell tests (uniqueness 4.2), run by {@link ModGameTests}. */
final class BellGameTests {
    private BellGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("bell_tone_follows_alloy", BellGameTests::toneFollowsAlloy);
        tests.put("bell_cast_and_hang", BellGameTests::castAndHang);
    }

    private static Melt alloy(int copper, int tin) {
        return new Melt(Map.of(Metal.COPPER, copper, Metal.TIN, tin), 0);
    }

    // More tin rings higher, plain copper stays low, a crude casting is cracked.
    private static void toneFollowsAlloy(GameTestHelper helper) {
        Quality standard = new Quality(0, -4);
        float copper = BellTone.of(Melt.of(Metal.COPPER, 200, 0), standard).pitch();
        float lean = BellTone.of(alloy(184, 16), standard).pitch();
        float rich = BellTone.of(alloy(176, 24), standard).pitch();
        helper.assertValueEqual(copper, BellTone.LOWEST, "a copper bell is the lowest");
        helper.assertTrue(lean > copper && rich > lean, "tin raises the pitch: " + copper + ", " + lean + ", " + rich);
        helper.assertTrue(rich <= BellTone.HIGHEST, "the pitch stays in range");
        helper.assertTrue(!BellTone.of(alloy(180, 20), standard).cracked(), "an ordinary casting is whole");
        helper.assertTrue(BellTone.of(alloy(180, 20), new Quality(-10, -4)).cracked(), "a crude casting is cracked");
        helper.assertTrue(CastMoldItem.ringsAsBell(Metal.BRONZE) && CastMoldItem.ringsAsBell(Metal.BRASS), "bronze and brass make bells");
        helper.assertTrue(!CastMoldItem.ringsAsBell(Metal.WROUGHT_IRON), "iron does not");
        helper.succeed();
    }

    // The bell mold gives a bell carrying its tone, the placed bell remembers it, and it comes back on the item.
    private static void castAndHang(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ItemStack mold = new ItemStack(BronzeRegistry.BELL_MOLD.get());
        mold.set(ModDataComponents.CAST_CONTENTS.get(), alloy(180, 20));
        ItemStack cast = CastMoldItem.castOf(mold, 20, level.getGameTime());
        helper.assertTrue(cast.is(BronzeRegistry.BELL_ITEM.get()), "the bell mold casts a bell, got " + cast);
        BellTone tone = cast.get(BronzeRegistry.BELL_TONE.get());
        helper.assertTrue(tone != null && tone.pitch() > BellTone.LOWEST, "the bell carries its tone, got " + tone);

        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlock(pos.above(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(pos, BronzeRegistry.BELL.get().defaultBlockState(), Block.UPDATE_ALL);
        BellBlockEntity bell = (BellBlockEntity) level.getBlockEntity(pos);
        bell.setTone(tone);
        helper.assertValueEqual(bell.collectComponents().get(BronzeRegistry.BELL_TONE.get()), tone, "the bell keeps its tone on the item");
        BellBlock.ring(level, pos);
        level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        helper.assertTrue(!level.getBlockState(pos).is(BronzeRegistry.BELL.get()), "a bell with nothing above it falls");
        helper.succeed();
    }
}
