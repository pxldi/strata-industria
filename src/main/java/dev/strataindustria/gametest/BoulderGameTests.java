package dev.strataindustria.gametest;

import com.mojang.authlib.GameProfile;
import dev.strataindustria.block.BoulderBlock;
import dev.strataindustria.geology.Rock;
import dev.strataindustria.knapping.Boulders;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModItems;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

/** Boulders you split by hand (redesign R1), run by {@link ModGameTests}. */
final class BoulderGameTests {
    /** Far enough apart that every blow lands. */
    private static final int GAP = 20;

    private BoulderGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("boulder_splits", BoulderGameTests::splits);
        tests.put("boulder_gap", BoulderGameTests::gap);
        tests.put("boulder_flint", BoulderGameTests::flint);
        tests.put("boulder_hand", BoulderGameTests::hand);
        tests.put("boulder_cobble", BoulderGameTests::cobble);
        tests.put("boulder_loot", BoulderGameTests::loot);
        tests.put("boulder_worldgen", BoulderGameTests::worldgen);
    }

    private static FakePlayer player(GameTestHelper helper) {
        FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "splitter"));
        player.setPos(helper.absoluteVec(new Vec3(1.5, 1.0, 1.5)));
        return player;
    }

    private static BlockPos place(GameTestHelper helper, Rock rock, int size, boolean flinty) {
        BlockPos pos = helper.absolutePos(new BlockPos(3, 1, 3));
        helper.getLevel().setBlock(pos, ModBlocks.BOULDER.get(rock).get().with(size, flinty, Direction.NORTH), Block.UPDATE_ALL);
        return pos;
    }

    private static void strike(GameTestHelper helper, FakePlayer player, BlockPos pos, long now) {
        ServerLevel level = helper.getLevel();
        Boulders.strike(level, player, pos, level.getBlockState(pos), Vec3.atCenterOf(pos), now);
    }

    private static int count(GameTestHelper helper, Item item) {
        int n = 0;
        for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(16))) {
            if (entity.getItem().is(item)) n += entity.getItem().getCount();
        }
        return n;
    }

    // Two blows open two cracks, the third splits a medium boulder into a small one and throws shards; the small one bursts.
    private static void splits(GameTestHelper helper) {
        FakePlayer player = player(helper);
        BlockPos pos = place(helper, Rock.GRANITE, 2, false);
        Item shard = ModItems.ROCK_SHARD.get(Rock.GRANITE).get();
        strike(helper, player, pos, GAP);
        helper.assertValueEqual(helper.getLevel().getBlockState(pos).getValue(BoulderBlock.CRACKS), 1, "the first blow opens a crack");
        strike(helper, player, pos, GAP * 2);
        helper.assertValueEqual(helper.getLevel().getBlockState(pos).getValue(BoulderBlock.CRACKS), 2, "the second blow opens another");
        helper.assertValueEqual(count(helper, shard), 0, "nothing falls out before the split");
        strike(helper, player, pos, GAP * 3);
        BlockState small = helper.getLevel().getBlockState(pos);
        helper.assertTrue(small.is(ModBlocks.BOULDER.get(Rock.GRANITE).get()), "a smaller boulder is left");
        helper.assertValueEqual(small.getValue(BoulderBlock.SIZE), 1, "one size down");
        helper.assertValueEqual(small.getValue(BoulderBlock.CRACKS), 0, "with no cracks");
        int first = count(helper, shard);
        helper.assertTrue(first >= Boulders.SPLIT_MIN && first <= Boulders.SPLIT_MIN + 1, "two or three shards hop out, not " + first);
        for (int i = 4; i <= 6; i++) strike(helper, player, pos, GAP * i);
        helper.assertTrue(helper.getLevel().getBlockState(pos).isAir(), "the last one bursts");
        int total = count(helper, shard) - first;
        helper.assertTrue(total >= Boulders.BURST_MIN && total <= Boulders.BURST_MAX, "three to five shards from the last, not " + total);
        helper.assertValueEqual(count(helper, Items.FLINT), 0, "granite has no flint");
        helper.succeed();
    }

    // A held button repeats faster than the rock answers: blows too close together count once.
    private static void gap(GameTestHelper helper) {
        FakePlayer player = player(helper);
        BlockPos pos = place(helper, Rock.BASALT, 3, false);
        strike(helper, player, pos, GAP);
        strike(helper, player, pos, GAP + 2);
        strike(helper, player, pos, GAP + 4);
        helper.assertValueEqual(helper.getLevel().getBlockState(pos).getValue(BoulderBlock.CRACKS), 1, "three fast clicks are one blow");
        strike(helper, player, pos, GAP + 4 + Boulders.BLOW_GAP_TICKS);
        helper.assertValueEqual(helper.getLevel().getBlockState(pos).getValue(BoulderBlock.CRACKS), 2, "the next steady one lands");
        helper.succeed();
    }

    // A limestone boulder with nodules gives flint when it breaks open.
    private static void flint(GameTestHelper helper) {
        FakePlayer player = player(helper);
        BlockPos pos = place(helper, Rock.LIMESTONE, 1, true);
        for (int i = 1; i <= 3; i++) strike(helper, player, pos, GAP * i);
        helper.assertTrue(helper.getLevel().getBlockState(pos).isAir(), "it split");
        helper.assertTrue(count(helper, Items.FLINT) >= 1, "flint came out of the nodules");
        helper.assertTrue(count(helper, ModItems.ROCK_SHARD.get(Rock.LIMESTONE).get()) >= Boulders.BURST_MIN, "and limestone shards");
        helper.succeed();
    }

    // An empty hand or a shard strikes; any other item goes on to its own use.
    private static void hand(GameTestHelper helper) {
        FakePlayer player = player(helper);
        ServerLevel level = helper.getLevel();
        BlockPos pos = place(helper, Rock.SLATE, 3, false);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        BlockState state = level.getBlockState(pos);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        helper.assertValueEqual(state.useItemOn(new ItemStack(Items.STICK), level, player, InteractionHand.MAIN_HAND, hit), InteractionResult.PASS, "a stick is left alone");
        helper.assertValueEqual(level.getBlockState(pos).getValue(BoulderBlock.CRACKS), 0, "so no crack");
        InteractionResult bare = state.useItemOn(ItemStack.EMPTY, level, player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(bare.consumesAction(), "a bare hand strikes");
        helper.assertValueEqual(level.getBlockState(pos).getValue(BoulderBlock.CRACKS), 1, "and opens a crack");
        helper.succeed();
    }

    // A cobbled rock struck on bare stone is spent for two shards of its rock.
    private static void cobble(GameTestHelper helper) {
        FakePlayer player = player(helper);
        ItemStack cobbled = new ItemStack(ModItems.COBBLED_ROCK.get(Rock.BASALT).get(), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, cobbled);
        helper.assertTrue(Boulders.strikeCobble(helper.getLevel(), player, cobbled, helper.absoluteVec(new Vec3(3.5, 1.0, 3.5)), GAP), "it strikes");
        helper.assertValueEqual(cobbled.getCount(), 1, "one cobbled rock spent");
        helper.assertValueEqual(count(helper, ModItems.ROCK_SHARD.get(Rock.BASALT).get()), 2, "two basalt shards");
        helper.assertTrue(!Boulders.strikeCobble(helper.getLevel(), player, new ItemStack(Items.COBBLESTONE), Vec3.ZERO, GAP * 2), "plain cobblestone is not a rock of ours");
        helper.succeed();
    }

    // Breaking a boulder with a tool gives shards, two to a size, and a mined raw rock gives its cobbled block.
    private static void loot(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = place(helper, Rock.GRANITE, 3, false);
        level.destroyBlock(pos, true);
        BlockPos raw = helper.absolutePos(new BlockPos(5, 1, 3));
        level.setBlock(raw, ModBlocks.RAW_ROCK.get(Rock.GRANITE).get().defaultBlockState(), Block.UPDATE_ALL);
        level.destroyBlock(raw, true);
        helper.runAfterDelay(2, () -> {
            helper.assertValueEqual(count(helper, ModItems.ROCK_SHARD.get(Rock.GRANITE).get()), 6, "six shards from a big boulder");
            helper.assertValueEqual(count(helper, ModItems.COBBLED_ROCK.get(Rock.GRANITE).get()), 1, "raw granite gives cobbled granite");
            helper.succeed();
        });
    }

    // The surface pass puts boulders on bare ground, none loose rocks, sticks or flint; limestone ones carry nodules.
    private static void worldgen(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        int groundY = helper.absolutePos(new BlockPos(0, 1, 0)).getY();
        int cx0 = (origin.getX() + 300) >> 4, cz0 = (origin.getZ() + 300) >> 4;
        int size = 6;
        for (int cx = cx0; cx < cx0 + size; cx++) {
            for (int cz = cz0; cz < cz0 + size; cz++) level.getChunk(cx, cz);
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = cx0 * 16; x < (cx0 + size) * 16; x++) {
            for (int z = cz0 * 16; z < (cz0 + size) * 16; z++) {
                for (int y = groundY - 2; y <= groundY + 6; y++) {
                    level.setBlock(pos.set(x, y, z), y > groundY ? Blocks.AIR.defaultBlockState()
                            : y == groundY ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
        for (int cx = cx0; cx < cx0 + size; cx++) {
            for (int cz = cz0; cz < cz0 + size; cz++) {
                dev.strataindustria.geology.worldgen.GroundCoverFeature.INSTANCE.place(level, level.getChunkSource().getGenerator(),
                        net.minecraft.util.RandomSource.create(cx * 31L + cz), new BlockPos(cx * 16, groundY, cz * 16));
            }
        }
        int boulders = 0, flinty = 0;
        for (int x = cx0 * 16; x < (cx0 + size) * 16; x++) {
            for (int z = cz0 * 16; z < (cz0 + size) * 16; z++) {
                BlockState state = level.getBlockState(pos.set(x, groundY + 1, z));
                if (!state.is(dev.strataindustria.registry.ModTags.Blocks.BOULDERS)) continue;
                boulders++;
                if (state.getValue(BoulderBlock.FLINTY)) {
                    flinty++;
                    helper.assertTrue(state.is(ModBlocks.BOULDER.get(Rock.LIMESTONE).get()), "only limestone shows flint");
                }
                helper.assertValueEqual(state.getValue(BoulderBlock.CRACKS), 0, "no cracks yet");
            }
        }
        helper.assertTrue(boulders >= 6, "boulders are placed, found " + boulders + " in " + size * size + " chunks");
        helper.succeed();
    }
}
