package dev.strataindustria.gametest;

import com.mojang.authlib.GameProfile;
import dev.strataindustria.branch.Branches;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;

/** Snapping branches off trees (redesign R2), run by {@link ModGameTests}. */
final class BranchGameTests {
    private BranchGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("branch_snap", BranchGameTests::snap);
        tests.put("branch_bare_after_snap", BranchGameTests::bareAfterSnap);
    }

    private static FakePlayer player(ServerLevel level) {
        return new FakePlayer(level, new GameProfile(UUID.randomUUID(), "climber"));
    }

    /** Three shakes snap the branch: sticks and bark fall, the leaf stays. */
    private static void snap(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlock(pos, Blocks.OAK_LEAVES.defaultBlockState(), 3);
        FakePlayer player = player(level);
        player.setPos(pos.getX() + 0.5, pos.getY() - 1, pos.getZ() + 2.5);
        helper.assertTrue(!Branches.shake(level, player, pos), "the first shake only bends it");
        level.getServer().getTickCount();
        helper.runAfterDelay(4, () -> {
            helper.assertTrue(!Branches.shake(level, player, pos), "the second shake only bends it");
            helper.runAfterDelay(4, () -> {
                helper.assertTrue(Branches.shake(level, player, pos), "the third shake snaps it");
                helper.assertTrue(level.getBlockState(pos).is(Blocks.OAK_LEAVES), "the leaf block stays");
                AABB box = new AABB(pos).inflate(3);
                int sticks = 0;
                int bark = 0;
                for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, box)) {
                    if (e.getItem().is(Items.STICK)) sticks += e.getItem().getCount();
                    else if (e.getItem().is(dev.strataindustria.registry.ModItems.BARK.get())) bark += e.getItem().getCount();
                }
                helper.assertTrue(sticks >= 1 && sticks <= 2, "one or two sticks fall, got " + sticks);
                helper.assertValueEqual(bark, 1, "one bark strip falls");
                helper.succeed();
            });
        });
    }

    /** A snapped branch and the leaves round it are bare: another shake gives nothing. */
    private static void bareAfterSnap(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlock(pos, Blocks.OAK_LEAVES.defaultBlockState(), 3);
        level.setBlock(pos.east(), Blocks.OAK_LEAVES.defaultBlockState(), 3);
        FakePlayer player = player(level);
        helper.runAfterDelay(1, () -> {
            Branches.shake(level, player, pos);
            helper.runAfterDelay(4, () -> {
                Branches.shake(level, player, pos);
                helper.runAfterDelay(4, () -> {
                    helper.assertTrue(Branches.shake(level, player, pos), "snaps on the third shake");
                    helper.assertTrue(Branches.isBare(level, pos, level.getGameTime()), "the branch is bare");
                    helper.assertTrue(Branches.isBare(level, pos.east(), level.getGameTime()), "the leaves beside it are bare");
                    helper.assertTrue(!Branches.shake(level, player, pos.east()), "a bare branch only rustles");
                    helper.succeed();
                });
            });
        });
    }
}
