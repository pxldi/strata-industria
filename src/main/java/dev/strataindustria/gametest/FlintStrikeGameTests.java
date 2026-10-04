package dev.strataindustria.gametest;

import com.mojang.authlib.GameProfile;
import dev.strataindustria.fire.FirePitBlock;
import dev.strataindustria.fire.FlintStrike;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.geology.Rock;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

/** Lighting a fire by striking flint on a rock (redesign L9), run by {@link ModGameTests}. */
final class FlintStrikeGameTests {
    private static final BlockPos PIT = new BlockPos(2, 1, 2);

    private FlintStrikeGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("flint_strike_lights_pit", FlintStrikeGameTests::lightsPit);
        tests.put("flint_strike_needs_rock_and_fuel", FlintStrikeGameTests::needsRockAndFuel);
    }

    private static FakePlayer player(GameTestHelper helper, boolean rock) {
        FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "striker"));
        player.setPos(helper.absoluteVec(new Vec3(2.5, 1.0, 3.5)));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FLINT, 3));
        if (rock) player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.ROCK_SHARD.get(Rock.values()[0]).get()));
        return player;
    }

    private static void pit(GameTestHelper helper, boolean fuel) {
        helper.setBlock(PIT, ModBlocks.FIRE_PIT.get().defaultBlockState());
        if (fuel) {
            helper.getBlockEntity(PIT, dev.strataindustria.fire.FirePitBlockEntity.class)
                    .setItem(dev.strataindustria.fire.FirePitBlockEntity.FUEL_SLOT, new ItemStack(Items.STICK, 4));
        }
    }

    // Each strike gets the pit closer and costs nothing; the third one catches.
    private static void lightsPit(GameTestHelper helper) {
        pit(helper, true);
        FakePlayer player = player(helper, true);
        BlockPos abs = helper.absolutePos(PIT);
        Vec3 at = Vec3.atCenterOf(abs);
        long t = 1000;
        for (int n = 1; n < FlintStrike.STRIKES; n++, t += 10) {
            helper.assertTrue(FlintStrike.strike(player, InteractionHand.MAIN_HAND, abs, at, t) == FlintStrike.Result.STRUCK, "strike " + n + " builds up");
            helper.assertValueEqual(FlintStrike.strikes(player, abs, t), n, "strikes so far");
            helper.assertTrue(!helper.getBlockState(PIT).getValue(FirePitBlock.LIT), "not lit before the last strike");
        }
        helper.assertTrue(FlintStrike.strike(player, InteractionHand.MAIN_HAND, abs, at, t) == FlintStrike.Result.LIT, "the last strike catches");
        helper.assertTrue(helper.getBlockState(PIT).getValue(FirePitBlock.LIT), "the pit is lit");
        helper.assertValueEqual(player.getMainHandItem().getCount(), 3, "the flint is not used up");
        helper.assertValueEqual(player.getOffhandItem().getCount(), 1, "nor the rock");
        helper.succeed();
    }

    // A click held down counts as one strike, and without a rock or fuel nothing happens.
    private static void needsRockAndFuel(GameTestHelper helper) {
        pit(helper, true);
        BlockPos abs = helper.absolutePos(PIT);
        Vec3 at = Vec3.atCenterOf(abs);
        FakePlayer bare = player(helper, false);
        helper.assertTrue(FlintStrike.strike(bare, InteractionHand.MAIN_HAND, abs, at, 1000) == FlintStrike.Result.NO_ROCK, "flint alone does nothing");
        FakePlayer player = player(helper, true);
        helper.assertTrue(FlintStrike.strike(player, InteractionHand.MAIN_HAND, abs, at, 1000) == FlintStrike.Result.STRUCK, "first strike");
        helper.assertTrue(FlintStrike.strike(player, InteractionHand.MAIN_HAND, abs, at, 1001) == FlintStrike.Result.WAIT, "a repeat inside a few ticks is the same strike");
        helper.assertValueEqual(FlintStrike.strikes(player, abs, 1001), 1, "still one strike");
        helper.assertValueEqual(FlintStrike.strikes(player, abs, 1000 + FlintStrike.STRIKES * 100), 0, "cold sparks are forgotten");
        helper.setBlock(PIT, net.minecraft.world.level.block.Blocks.AIR);
        pit(helper, false);
        helper.assertTrue(FlintStrike.strike(player, InteractionHand.MAIN_HAND, abs, at, 2000) == FlintStrike.Result.NOT_A_TARGET, "no fuel, nothing to light");
        helper.succeed();
    }
}
