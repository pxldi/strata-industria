package dev.strataindustria.gametest;

import dev.strataindustria.geology.Rock;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.transport.foot.Burden;
import dev.strataindustria.transport.foot.CairnBlock;
import dev.strataindustria.transport.foot.FootEvents;
import dev.strataindustria.transport.foot.FootRegistry;
import dev.strataindustria.transport.foot.HandcartEntity;
import dev.strataindustria.transport.foot.PackContents;
import dev.strataindustria.transport.foot.PackEvents;
import dev.strataindustria.transport.foot.RopeItem;
import dev.strataindustria.transport.foot.TrailMarks;
import dev.strataindustria.transport.foot.TrailMessages;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Tier 2 on foot (outposts and transport spec 4 and 15), run by {@link ModGameTests}. */
final class FootGameTests {
    private FootGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("foot_rope_ladder", FootGameTests::ropeLadder);
        tests.put("foot_pack_frame", FootGameTests::packFrame);
        tests.put("foot_cairn", FootGameTests::cairn);
        tests.put("foot_blaze", FootGameTests::blaze);
        tests.put("foot_trail_chain", FootGameTests::trailChain);
        tests.put("foot_handcart_pull", FootGameTests::handcartPull);
        tests.put("foot_handcart_full_block", helper -> handcartStep(helper, false));
        tests.put("foot_handcart_slab", helper -> handcartStep(helper, true));
        tests.put("foot_handcart_breaks", FootGameTests::handcartBreaks);
    }

    @SuppressWarnings("removal")
    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    private static void done(GameTestHelper helper, ServerPlayer player) {
        helper.getLevel().getServer().getPlayerList().remove(player);
        helper.succeed();
    }

    private static int items(GameTestHelper helper, net.minecraft.world.item.Item item) {
        int n = 0;
        for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(16))) {
            if (entity.getItem().is(item)) n += entity.getItem().getCount();
        }
        return n;
    }

    // Rope on the underside of a block pays out one ladder block per rope; the column climbs; cutting the top drops every rope.
    private static void ropeLadder(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = player(helper);
        BlockPos roof = helper.absolutePos(new BlockPos(4, 4, 4));
        level.setBlock(roof, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        ItemStack ropes = new ItemStack(FootRegistry.ROPE.get(), 2);
        int placed = RopeItem.hang(level, roof.below(), Direction.NORTH, ropes, player);
        helper.assertValueEqual(placed, 2, "two ropes, two blocks");
        helper.assertTrue(ropes.isEmpty(), "the ropes are used up");
        for (int i = 1; i <= 2; i++) {
            BlockState state = level.getBlockState(roof.below(i));
            helper.assertTrue(state.is(FootRegistry.ROPE_LADDER.get()), "ladder at " + i + " below the roof");
            helper.assertTrue(state.is(BlockTags.CLIMBABLE), "and it can be climbed");
        }
        // More rope goes on the bottom of what hangs already.
        ItemStack more = new ItemStack(FootRegistry.ROPE.get(), 1);
        helper.assertValueEqual(RopeItem.hang(level, roof.below(3), Direction.NORTH, more, player), 1, "one more extends the column");
        // A ladder stops where the ground is, however much rope is left.
        ItemStack lots = new ItemStack(FootRegistry.ROPE.get(), 64);
        BlockPos high = helper.absolutePos(new BlockPos(1, 4, 1));
        level.setBlock(high, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        helper.assertValueEqual(RopeItem.hang(level, high.below(), Direction.NORTH, lots, player), 3, "it stops at the floor");
        helper.assertValueEqual(lots.getCount(), 61, "and only the rope it used is gone");

        level.setBlock(roof, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        helper.runAfterDelay(30, () -> {
            for (int i = 1; i <= 3; i++) helper.assertTrue(level.getBlockState(roof.below(i)).isAir(), "ladder " + i + " came down");
            helper.assertTrue(level.getBlockState(high.below()).is(FootRegistry.ROPE_LADDER.get()), "the other ladder hangs on");
            helper.assertValueEqual(items(helper, FootRegistry.ROPE.get()), 3, "every rope of the cut ladder dropped");
            done(helper, player);
        });
    }

    // Nine slots stored on the item; no frames, carts or shulker boxes inside; more than half full means no sprinting.
    private static void packFrame(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        ItemStack frame = new ItemStack(FootRegistry.PACK_FRAME.get());
        PackContents contents = new PackContents(frame, player, true);
        helper.assertValueEqual(contents.getContainerSize(), 9, "nine slots");
        helper.assertTrue(!contents.canPlaceItem(0, new ItemStack(FootRegistry.PACK_FRAME.get())), "no frame in a frame");
        helper.assertTrue(!contents.canPlaceItem(0, new ItemStack(FootRegistry.HANDCART.get())), "no cart in a frame");
        helper.assertTrue(!contents.canPlaceItem(0, new ItemStack(Items.SHULKER_BOX)), "no shulker box in a frame");
        helper.assertTrue(contents.canPlaceItem(0, new ItemStack(Items.STICK)), "sticks are fine");

        player.setItemSlot(EquipmentSlot.CHEST, frame);
        for (int i = 0; i < 4; i++) contents.setItem(i, new ItemStack(Items.STICK, 8));
        helper.assertValueEqual(PackContents.filledSlots(frame), 4, "the item keeps what was put in");
        PackEvents.refresh(player);
        helper.assertTrue(!Burden.noSprint(player), "four of nine is not heavy");
        contents.setItem(4, new ItemStack(Items.COAL, 3));
        PackEvents.refresh(player);
        helper.assertTrue(Burden.noSprint(player), "five of nine is more than half");
        // Opened again, it holds what it held.
        PackContents again = new PackContents(frame, player, false);
        helper.assertValueEqual(again.getItem(4).getCount(), 3, "the coal is still there");
        helper.assertTrue(again.stillValid(player), "a worn frame stays open while worn");
        player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        PackEvents.refresh(player);
        helper.assertTrue(!Burden.noSprint(player), "taking it off frees the legs");
        done(helper, player);
    }

    // Four shards on the ground make a cairn, four more raise it to three; it gives back what went in.
    private static void cairn(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = player(helper);
        BlockPos ground = helper.absolutePos(new BlockPos(3, 0, 3));
        BlockPos pos = ground.above();
        level.setBlock(ground, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        ItemStack granite = new ItemStack(ModItems.ROCK_SHARD.get(Rock.GRANITE).get(), 8);
        ItemStack few = new ItemStack(ModItems.ROCK_SHARD.get(Rock.GRANITE).get(), 3);
        helper.assertTrue(!FootEvents.stack(player, few, Rock.GRANITE, level, ground), "three rocks are not enough");
        helper.assertTrue(FootEvents.stack(player, granite, Rock.GRANITE, level, ground), "four make a cairn");
        BlockState state = level.getBlockState(pos);
        helper.assertTrue(state.is(FootRegistry.CAIRN.get()), "there is a cairn");
        helper.assertValueEqual(state.getValue(CairnBlock.HEIGHT), 1, "one course");
        helper.assertValueEqual(state.getValue(CairnBlock.ROCK), Rock.GRANITE, "of granite");
        helper.assertValueEqual(granite.getCount(), 4, "four rocks used");
        ItemStack basalt = new ItemStack(ModItems.ROCK_SHARD.get(Rock.BASALT).get(), 8);
        helper.assertTrue(FootEvents.stack(player, basalt, Rock.BASALT, level, pos), "four more raise it");
        helper.assertValueEqual(level.getBlockState(pos).getValue(CairnBlock.HEIGHT), 2, "two courses");
        helper.assertValueEqual(level.getBlockState(pos).getValue(CairnBlock.ROCK), Rock.BASALT, "it takes the newest rock");
        helper.assertTrue(FootEvents.stack(player, basalt, Rock.BASALT, level, pos), "and a third");
        helper.assertTrue(!FootEvents.stack(player, basalt, Rock.BASALT, level, pos), "three is the most");
        helper.assertValueEqual(level.getBlockState(pos).getValue(CairnBlock.HEIGHT), 3, "three courses");
        // Open air takes no cairn: there is nothing to stand it on.
        BlockPos air = helper.absolutePos(new BlockPos(5, 1, 3));
        helper.assertTrue(!FootEvents.stack(player, basalt, Rock.BASALT, level, air), "air takes no cairn");
        // Breaking it gives the rocks back, four to a course.
        level.destroyBlock(pos, true);
        helper.runAfterDelay(2, () -> {
            helper.assertValueEqual(items(helper, ModItems.ROCK_SHARD.get(Rock.BASALT).get()), 12, "twelve basalt rocks back");
            done(helper, player);
        });
    }

    // A knife on a log's side cuts a blaze; only logs, only sides; the blaze goes when the log does.
    private static void blaze(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = player(helper);
        BlockPos log = helper.absolutePos(new BlockPos(3, 1, 3));
        level.setBlock(log, Blocks.OAK_LOG.defaultBlockState(), Block.UPDATE_ALL);
        ItemStack knife = new ItemStack(ModItems.STONE_KNIFE.get());
        helper.assertTrue(!FootEvents.cutBlaze(player, knife, level, log, Direction.UP), "not on the top");
        helper.assertTrue(!FootEvents.cutBlaze(player, knife, level, helper.absolutePos(new BlockPos(5, 1, 3)), Direction.NORTH), "not on bare ground");
        helper.assertTrue(FootEvents.cutBlaze(player, knife, level, log, Direction.NORTH), "the north side takes a blaze");
        BlockState blaze = level.getBlockState(log.north());
        helper.assertTrue(blaze.is(FootRegistry.BLAZE_MARK.get()), "a blaze is there");
        helper.assertValueEqual(blaze.getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING), Direction.NORTH, "facing out");
        helper.assertValueEqual(knife.getDamageValue(), 1, "the knife wore a little");
        level.setBlock(log, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        helper.assertTrue(level.getBlockState(log.north()).isAir(), "no log, no blaze");
        done(helper, player);
    }

    // Marks within the trail step chain; a far one starts a new trail; next and back follow the chain; the line names a compass side.
    private static void trailChain(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        TrailMarks marks = TrailMarks.get(level.getServer());
        UUID who = UUID.randomUUID();
        BlockPos a = new BlockPos(0, 64, 0), b = new BlockPos(40, 64, 0), c = new BlockPos(80, 64, -30), far = new BlockPos(500, 64, 0);
        helper.assertValueEqual(marks.add(who, level, a), 1, "first mark");
        helper.assertValueEqual(marks.add(who, level, b), 2, "second joins");
        helper.assertValueEqual(marks.add(who, level, c), 3, "third joins");
        helper.assertValueEqual(marks.add(who, level, far), 1, "a far mark starts a new trail");
        TrailMarks.Neighbours middle = marks.neighbours(who, level, b);
        helper.assertTrue(middle != null && middle.next() != null && middle.next().pos().equals(c), "next of the second is the third");
        helper.assertTrue(middle.back() != null && middle.back().pos().equals(a), "back of the second is the first");
        TrailMarks.Neighbours last = marks.neighbours(who, level, c);
        helper.assertTrue(last != null && last.next() == null, "the chain ends before the far mark");
        TrailMarks.Neighbours alone = marks.neighbours(who, level, far);
        helper.assertTrue(alone != null && alone.next() == null && alone.back() == null, "the far mark stands alone");
        helper.assertValueEqual(TrailMessages.direction(a, b), "east", "east");
        helper.assertValueEqual(TrailMessages.direction(b, a), "west", "west");
        helper.assertValueEqual(TrailMessages.direction(a, new BlockPos(0, 64, -30)), "north", "north is down the z axis");
        helper.assertValueEqual(TrailMessages.direction(a, new BlockPos(30, 64, 30)), "south_east", "south-east");
        helper.assertValueEqual(TrailMessages.distance(a, b), 40, "forty blocks");
        marks.remove(level, b);
        TrailMarks.Neighbours after = marks.neighbours(who, level, c);
        helper.assertTrue(after != null && after.back() != null && after.back().pos().equals(a), "with the middle mark gone the others chain");
        helper.assertValueEqual(marks.count(who), 3, "three marks are left");
        for (BlockPos pos : List.of(a, c, far)) marks.remove(level, pos);
        helper.succeed();
    }

    // Sneak-use takes the shafts; the cart trails its puller and slows them; letting go frees them.
    private static void handcartPull(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = player(helper);
        BlockPos start = helper.absolutePos(new BlockPos(1, 1, 4));
        HandcartEntity cart = helper.spawn(FootRegistry.HANDCART_ENTITY.get(), new BlockPos(1, 1, 4));
        player.teleportTo(start.getX() + 2.5, start.getY(), start.getZ() + 0.5);
        cart.grab(player);
        helper.assertTrue(cart.isPulled() && HandcartEntity.isPulling(player), "the cart is held");
        helper.assertTrue(Burden.isHauling(player), "the puller is slowed");
        double startX = cart.getX();
        for (int t = 1; t <= 40; t++) {
            int step = t;
            helper.runAfterDelay(step, () -> player.teleportTo(start.getX() + 2.5 + step * 0.12, start.getY(), start.getZ() + 0.5));
        }
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(cart.getX() > startX + 3.0, "the cart followed, moved " + (cart.getX() - startX));
            double gap = Math.abs(player.getX() - cart.getX());
            helper.assertTrue(gap < HandcartEntity.TETHER + 0.8, "and stays close, gap " + gap);
            helper.assertTrue(cart.hauled() > 3.0, "the haul is counted, " + cart.hauled());
            player.hurtServer(level, level.damageSources().generic(), 1.0f);
            helper.assertTrue(!HandcartEntity.isPulling(player) && !cart.isPulled(), "a hit lets go");
            helper.assertTrue(!Burden.isHauling(player), "the slowdown goes with it");
            cart.discard();
            done(helper, player);
        });
    }

    // The cart climbs a slab the puller walks up, and stops at a whole block.
    private static void handcartStep(GameTestHelper helper, boolean slab) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = player(helper);
        BlockPos start = helper.absolutePos(new BlockPos(0, 1, 4));
        BlockPos wall = helper.absolutePos(new BlockPos(4, 1, 4));
        BlockState step = slab ? Blocks.STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, net.minecraft.world.level.block.state.properties.SlabType.BOTTOM)
                : Blocks.STONE.defaultBlockState();
        for (int dz = -1; dz <= 1; dz++) level.setBlock(wall.offset(0, 0, dz), step, Block.UPDATE_ALL);
        HandcartEntity cart = helper.spawn(FootRegistry.HANDCART_ENTITY.get(), new BlockPos(0, 1, 4));
        cart.setYRot(-90);
        player.teleportTo(start.getX() + 2.0, start.getY(), start.getZ() + 0.5);
        cart.grab(player);
        for (int t = 1; t <= 50; t++) {
            int stepNo = t;
            helper.runAfterDelay(stepNo, () -> player.teleportTo(start.getX() + 2.0 + Math.min(stepNo * 0.14, 7.0), start.getY() + (slab ? 0.5 : 0), start.getZ() + 0.5));
        }
        helper.runAfterDelay(80, () -> {
            if (slab) {
                helper.assertTrue(cart.getX() > wall.getX() + 1.0, "the cart is over the slab, x " + cart.getX());
            } else {
                helper.assertTrue(cart.getX() < wall.getX(), "a whole block stops it, x " + cart.getX());
            }
            cart.release();
            cart.discard();
            done(helper, player);
        });
    }

    // Eighteen slots; a player's blow breaks it into its item and drops what it held; nothing else does.
    private static void handcartBreaks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = player(helper);
        HandcartEntity cart = helper.spawn(FootRegistry.HANDCART_ENTITY.get(), new BlockPos(3, 1, 3));
        helper.assertValueEqual(cart.getContainerSize(), 18, "eighteen slots");
        cart.setItem(0, new ItemStack(Items.COAL, 5));
        cart.setItem(17, new ItemStack(Items.STICK, 9));
        helper.assertTrue(!cart.hurtServer(level, level.damageSources().generic(), 50f), "the world cannot hurt it");
        helper.assertTrue(!cart.isRemoved(), "so it stays");
        DamageSource blow = level.damageSources().playerAttack(player);
        helper.assertTrue(cart.hurtServer(level, blow, 1f), "a player's blow breaks it");
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(cart.isRemoved(), "it is gone");
            helper.assertValueEqual(items(helper, FootRegistry.HANDCART.get()), 1, "the cart drops as an item");
            helper.assertValueEqual(items(helper, Items.COAL), 5, "with its coal");
            helper.assertValueEqual(items(helper, Items.STICK), 9, "and its sticks");
            done(helper, player);
        });
    }
}
