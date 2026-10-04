package dev.strataindustria.gametest;

import com.mojang.authlib.GameProfile;
import dev.strataindustria.fire.FirePitBlock;
import dev.strataindustria.fire.FirePitBlockEntity;
import dev.strataindustria.fire.FirePitFuel;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModItems;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

/** Feeding the open fire and using its hearth stones by hand (redesign R6), run by {@link ModGameTests}. */
final class FirePitFeedGameTests {
    private static final BlockPos PIT = new BlockPos(2, 1, 2);

    private FirePitFeedGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("fire_pit_feed_grows_fire", FirePitFeedGameTests::feedGrowsFire);
        tests.put("fire_pit_burns_down", FirePitFeedGameTests::burnsDown);
        tests.put("fire_pit_hearth_aim", FirePitFeedGameTests::hearthAim);
        tests.put("fire_pit_food_cooks_and_hops_off", FirePitFeedGameTests::foodCooks);
    }

    private static FakePlayer player(GameTestHelper helper) {
        FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "cook"));
        player.setPos(helper.absoluteVec(new Vec3(2.5, 1.0, 3.5)));
        return player;
    }

    /** Uses the held stack on the pit at the given spot (0 to 3, the corner stones). */
    private static void use(GameTestHelper helper, FakePlayer player, ItemStack stack, int spot) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos abs = helper.absolutePos(PIT);
        Vec3 at = new Vec3(abs.getX() + FirePitBlockEntity.HEARTH_X[spot], abs.getY() + FirePitBlockEntity.HEARTH_Y, abs.getZ() + FirePitBlockEntity.HEARTH_Z[spot]);
        BlockState state = helper.getLevel().getBlockState(abs);
        state.useItemOn(stack, helper.getLevel(), player, InteractionHand.MAIN_HAND, new BlockHitResult(at, Direction.UP, abs, false));
    }

    private static void takeBare(GameTestHelper helper, FakePlayer player, int spot) {
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        BlockPos abs = helper.absolutePos(PIT);
        Vec3 at = new Vec3(abs.getX() + FirePitBlockEntity.HEARTH_X[spot], abs.getY() + FirePitBlockEntity.HEARTH_Y, abs.getZ() + FirePitBlockEntity.HEARTH_Z[spot]);
        helper.getLevel().getBlockState(abs).useWithoutItem(helper.getLevel(), player, new BlockHitResult(at, Direction.UP, abs, false));
    }

    private static void run(GameTestHelper helper, FirePitBlockEntity pit, int ticks) {
        BlockPos abs = helper.absolutePos(PIT);
        for (int i = 0; i < ticks; i++) FirePitBlockEntity.serverTick(helper.getLevel(), abs, helper.getLevel().getBlockState(abs), pit);
    }

    private static int size(GameTestHelper helper) {
        return helper.getBlockState(PIT).getValue(FirePitBlock.FUEL);
    }

    // Each log makes the fire bigger, up to three sizes, and a fourth is not taken.
    private static void feedGrowsFire(GameTestHelper helper) {
        helper.setBlock(PIT, ModBlocks.FIRE_PIT.get().defaultBlockState());
        FirePitBlockEntity pit = helper.getBlockEntity(PIT, FirePitBlockEntity.class);
        FakePlayer player = player(helper);
        ItemStack logs = new ItemStack(Items.OAK_LOG, 4);
        helper.assertValueEqual(size(helper), 0, "a new pit has a bare bed");
        use(helper, player, logs, 0);
        run(helper, pit, 1);
        helper.assertValueEqual(logs.getCount(), 3, "a log is used up");
        helper.assertValueEqual(size(helper), 2, "one log gives a middling fire");
        use(helper, player, logs, 0);
        use(helper, player, logs, 0);
        run(helper, pit, 1);
        helper.assertValueEqual(logs.getCount(), 1, "three logs in all");
        helper.assertValueEqual(size(helper), 3, "three logs make a big fire");
        use(helper, player, logs, 0);
        helper.assertValueEqual(logs.getCount(), 1, "a full fire takes no more");
        helper.succeed();
    }

    // A fire left alone shrinks as the wood burns, then goes out once it has cooled.
    private static void burnsDown(GameTestHelper helper) {
        helper.setBlock(PIT, ModBlocks.FIRE_PIT.get().defaultBlockState());
        FirePitBlockEntity pit = helper.getBlockEntity(PIT, FirePitBlockEntity.class);
        pit.feed(FirePitFuel.LOG);
        pit.feed(FirePitFuel.LOG);
        BlockPos abs = helper.absolutePos(PIT);
        helper.assertTrue(((FirePitBlock) helper.getBlockState(PIT).getBlock()).ignite(helper.getLevel(), abs, helper.getBlockState(PIT)), "the pit lights");
        run(helper, pit, 1);
        helper.assertValueEqual(size(helper), 3, "starts big");
        run(helper, pit, 700);
        helper.assertValueEqual(size(helper), 2, "settles as the wood burns");
        run(helper, pit, 1000);
        helper.assertValueEqual(size(helper), 1, "down to a small fire");
        run(helper, pit, 1000);
        helper.assertValueEqual(size(helper), 0, "burnt out");
        helper.assertTrue(helper.getBlockState(PIT).getValue(FirePitBlock.LIT), "still glowing while it cools");
        run(helper, pit, 4000);
        helper.assertTrue(!helper.getBlockState(PIT).getValue(FirePitBlock.LIT), "out once cold");
        helper.succeed();
    }

    // A piece goes on the stone aimed at, a taken-back piece comes from the stone aimed at, and a bare hand with
    // nothing aimed at takes the last one.
    private static void hearthAim(GameTestHelper helper) {
        helper.setBlock(PIT, ModBlocks.FIRE_PIT.get().defaultBlockState());
        FirePitBlockEntity pit = helper.getBlockEntity(PIT, FirePitBlockEntity.class);
        FakePlayer player = player(helper);
        use(helper, player, new ItemStack(ModItems.UNFIRED_BRICK.get(), 2), 3);
        use(helper, player, new ItemStack(ModItems.UNFIRED_CRUCIBLE.get(), 1), 0);
        helper.assertTrue(pit.hearth().get(3).is(ModItems.UNFIRED_BRICK.get()), "the brick lies on the stone aimed at");
        helper.assertTrue(pit.hearth().get(0).is(ModItems.UNFIRED_CRUCIBLE.get()), "the crucible on the other");
        use(helper, player, new ItemStack(ModItems.UNFIRED_BRICK.get(), 1), 3);
        helper.assertTrue(pit.hearth().get(1).is(ModItems.UNFIRED_BRICK.get()), "an occupied stone sends the piece to the next free one");
        takeBare(helper, player, 3);
        helper.assertTrue(pit.hearth().get(3).isEmpty(), "the aimed stone is bare again");
        helper.assertTrue(player.getInventory().contains(s -> s.is(ModItems.UNFIRED_BRICK.get())), "the brick is back in hand");
        takeBare(helper, player, 2);
        helper.assertTrue(pit.hearth().get(2).isEmpty() && pit.hearth().get(1).isEmpty(), "nothing on the stone aimed at takes the last one set down");
        helper.succeed();
    }

    // Raw meat on a stone in a hot fire is done after its cooking time and hops off as cooked meat.
    private static void foodCooks(GameTestHelper helper) {
        helper.setBlock(PIT, ModBlocks.FIRE_PIT.get().defaultBlockState());
        FirePitBlockEntity pit = helper.getBlockEntity(PIT, FirePitBlockEntity.class);
        pit.feed(FirePitFuel.LOG);
        pit.feed(FirePitFuel.LOG);
        BlockPos abs = helper.absolutePos(PIT);
        ((FirePitBlock) helper.getBlockState(PIT).getBlock()).ignite(helper.getLevel(), abs, helper.getBlockState(PIT));
        FakePlayer player = player(helper);
        ItemStack beef = new ItemStack(Items.BEEF, 2);
        use(helper, player, beef, 1);
        helper.assertValueEqual(beef.getCount(), 1, "one piece goes on the stone");
        helper.assertTrue(pit.hearth().get(1).is(Items.BEEF), "the beef lies on the stone aimed at");
        run(helper, pit, 100);
        helper.assertTrue(pit.hearth().get(1).is(Items.BEEF), "still raw while the fire warms");
        run(helper, pit, 800);
        helper.assertTrue(pit.hearth().get(1).isEmpty(), "done meat leaves the stone");
        boolean found = !helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(abs).inflate(3),
                item -> item.getItem().is(Items.COOKED_BEEF)).isEmpty();
        helper.assertTrue(found, "cooked beef hopped out beside the fire");
        helper.succeed();
    }
}
