package dev.strataindustria.gametest;

import com.mojang.authlib.GameProfile;
import dev.strataindustria.felling.TreeFelling;
import dev.strataindustria.registry.ModItems;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

/** Felling trees (redesign R5), run by {@link ModGameTests}. */
final class FellingGameTests {
    private static final BlockPos FOOT = new BlockPos(2, 1, 2);
    private static final int LOGS = 5;

    private FellingGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("felling_notches_then_falls", FellingGameTests::notchesThenFalls);
        tests.put("felling_only_natural_trees", FellingGameTests::onlyNaturalTrees);
    }

    private static FakePlayer player(GameTestHelper helper, ItemStack held) {
        FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "feller"));
        player.setPos(helper.absoluteVec(new Vec3(2.5, 1.0, 0.5)));
        player.setYRot(0.0f);
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        return player;
    }

    private static void tree(GameTestHelper helper, boolean natural) {
        for (int y = 0; y < LOGS; y++) helper.setBlock(FOOT.above(y), Blocks.OAK_LOG);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int y = LOGS - 2; y <= LOGS; y++) {
                    BlockPos at = FOOT.offset(dx, y, dz);
                    if (dx == 0 && dz == 0 && y < LOGS) continue;
                    helper.setBlock(at, Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, !natural));
                }
            }
        }
    }

    private static int count(GameTestHelper helper, Item item) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(FOOT)).inflate(30), e -> e.getItem().is(item))
                .stream().mapToInt(e -> e.getItem().getCount()).sum();
    }

    private static int displays(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(Display.BlockDisplay.class, new AABB(helper.absolutePos(FOOT)).inflate(30)).size();
    }

    // Three blows cut a growing notch and change nothing else; the fourth fells the whole tree and everything drops.
    private static void notchesThenFalls(GameTestHelper helper) {
        TreeFelling.reset();
        tree(helper, true);
        FakePlayer player = player(helper, new ItemStack(Items.IRON_AXE));
        BlockPos foot = helper.absolutePos(FOOT);
        for (int blow = 1; blow < TreeFelling.BLOWS; blow++) {
            helper.assertTrue(TreeFelling.chop(player, foot), "blow " + blow + " should be taken by the tree");
            helper.assertTrue(helper.getLevel().getBlockState(foot).is(Blocks.OAK_LOG), "the log stays through the notches");
            helper.assertValueEqual(displays(helper), 1, "one pale notch shows on the trunk after blow " + blow);
        }
        helper.assertTrue(helper.getLevel().getBlockState(foot.above(LOGS - 1)).is(Blocks.OAK_LOG), "the crown is still up");
        helper.assertTrue(TreeFelling.chop(player, foot), "the last blow fells the tree");
        for (int y = 0; y < LOGS; y++) helper.assertTrue(helper.getLevel().getBlockState(foot.above(y)).isAir(), "log " + y + " should be gone");
        helper.assertTrue(helper.getLevel().getBlockState(foot.offset(1, LOGS, 1)).isAir(), "its leaves should be gone");
        helper.assertTrue(displays(helper) > LOGS, "the tree tips over as block displays");
        helper.assertValueEqual(count(helper, Items.OAK_LOG), 0, "nothing lands before the crash");
        helper.runAfterDelay(60, () -> {
            helper.assertValueEqual(count(helper, Items.OAK_LOG), LOGS, "every log drops");
            helper.assertTrue(count(helper, Items.STICK) >= 3, "sticks drop");
            helper.assertTrue(count(helper, ModItems.BARK.get()) >= 2, "bark drops");
            helper.assertValueEqual(displays(helper), 0, "the falling images are gone after the crash");
            helper.succeed();
        });
    }

    // Sneaking, a bare hand, logs without natural leaves and logs with player-placed leaves all break as before.
    private static void onlyNaturalTrees(GameTestHelper helper) {
        TreeFelling.reset();
        BlockPos foot = helper.absolutePos(FOOT);
        tree(helper, true);
        FakePlayer sneaker = player(helper, new ItemStack(Items.IRON_AXE));
        sneaker.setShiftKeyDown(true);
        helper.assertTrue(!TreeFelling.chop(sneaker, foot), "sneaking chops a single log");
        helper.assertTrue(!TreeFelling.chop(player(helper, ItemStack.EMPTY), foot), "a bare hand does not fell");
        tree(helper, false);
        helper.assertTrue(!TreeFelling.chop(player(helper, new ItemStack(Items.IRON_AXE)), foot), "player-placed leaves make no tree");
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int y = LOGS - 2; y <= LOGS; y++) helper.setBlock(FOOT.offset(dx, y, dz), Blocks.AIR);
            }
        }
        helper.assertTrue(!TreeFelling.chop(player(helper, new ItemStack(Items.IRON_AXE)), foot), "bare logs are a pole, not a tree");
        helper.assertTrue(!TreeFelling.chop(player(helper, new ItemStack(Items.IRON_AXE)), foot.above()), "only the bottom log is notched");
        helper.succeed();
    }
}
