package dev.strataindustria.gametest;

import com.mojang.authlib.GameProfile;
import dev.strataindustria.cord.BarkStripItem;
import dev.strataindustria.cord.CordItem;
import dev.strataindustria.registry.ModItems;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;

/** Twisting bark into cord and beating cord into cloth (redesign R3), run by {@link ModGameTests}. */
final class CordGameTests {
    private CordGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("cord_twist", CordGameTests::twist);
        tests.put("cord_twist_needs_two", CordGameTests::twistNeedsTwo);
        tests.put("cord_beat_into_cloth", CordGameTests::beat);
        tests.put("cord_beat_needs_four", CordGameTests::beatNeedsFour);
    }

    private static FakePlayer player(ServerLevel level) {
        return new FakePlayer(level, new GameProfile(UUID.randomUUID(), "twister"));
    }

    private static int count(ServerLevel level, BlockPos pos, net.minecraft.world.item.Item item) {
        int n = 0;
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3))) {
            if (e.getItem().is(item)) n += e.getItem().getCount();
        }
        return n;
    }

    /** Two strips become one cord, in the inventory; the third strip stays. */
    private static void twist(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FakePlayer player = player(level);
        ItemStack strips = new ItemStack(ModItems.BARK.get(), 3);
        BarkStripItem.twist(level, player, strips);
        helper.assertValueEqual(strips.getCount(), 1, "two strips used");
        int cord = 0;
        for (ItemStack s : player.getInventory().getNonEquipmentItems()) if (s.is(ModItems.CORD.get())) cord += s.getCount();
        helper.assertValueEqual(cord, 1, "one cord made");
        helper.succeed();
    }

    /** A single strip does not start a twist. */
    private static void twistNeedsTwo(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FakePlayer player = player(level);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(ModItems.BARK.get(), 1));
        var result = ModItems.BARK.get().use(level, player, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(result == net.minecraft.world.InteractionResult.FAIL, "one strip is not enough");
        helper.assertTrue(!player.isUsingItem(), "no twisting starts");
        helper.succeed();
    }

    /** Three blows on stone beat four cord into a cloth that pops off the stone. */
    private static void beat(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlock(pos, Blocks.COBBLESTONE.defaultBlockState(), 3);
        FakePlayer player = player(level);
        ItemStack cord = new ItemStack(ModItems.CORD.get(), 5);
        helper.assertTrue(!CordItem.beat(level, player, cord, pos, Direction.UP), "the first blow only thumps");
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(!CordItem.beat(level, player, cord, pos, Direction.UP), "the second blow only thumps");
            helper.assertValueEqual(cord.getCount(), 5, "nothing spent yet");
            helper.runAfterDelay(5, () -> {
                helper.assertTrue(CordItem.beat(level, player, cord, pos, Direction.UP), "the third blow frees the cloth");
                helper.assertValueEqual(cord.getCount(), 1, "four cord used");
                helper.assertValueEqual(count(level, pos, ModItems.BARK_CLOTH.get()), 1, "the cloth pops off the stone");
                helper.succeed();
            });
        });
    }

    /** Only stone can be beaten on, and only four cord make a cloth. */
    private static void beatNeedsFour(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.assertTrue(CordItem.isBeatingStone(Blocks.STONE.defaultBlockState()), "stone works");
        helper.assertTrue(CordItem.isBeatingStone(Blocks.COBBLESTONE.defaultBlockState()), "cobble works");
        helper.assertTrue(!CordItem.isBeatingStone(Blocks.OAK_PLANKS.defaultBlockState()), "planks do not");
        helper.assertTrue(!CordItem.isBeatingStone(Blocks.DIRT.defaultBlockState()), "dirt does not");
        helper.succeed();
    }
}
