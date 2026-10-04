package dev.strataindustria.gametest;

import dev.strataindustria.fire.FirePitBlock;
import dev.strataindustria.fire.FirePitBlockEntity;
import dev.strataindustria.fire.FirePitFuel;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModItems;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Firing clay on the fire pit hearth (redesign L6), run by {@link ModGameTests}. */
final class FirePitFiringGameTests {
    private static final BlockPos PIT = new BlockPos(2, 1, 2);

    private FirePitFiringGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("fire_pit_firing", FirePitFiringGameTests::firesFourPieces);
        tests.put("fire_pit_too_cold_to_fire", FirePitFiringGameTests::sticksAreTooCold);
        tests.put("fire_pit_hearth_take_back", FirePitFiringGameTests::takeBack);
    }

    private static FirePitBlockEntity lit(GameTestHelper helper, Item fuel) {
        helper.setBlock(PIT, ModBlocks.FIRE_PIT.get().defaultBlockState());
        FirePitBlockEntity pit = helper.getBlockEntity(PIT, FirePitBlockEntity.class);
        FirePitFuel burning = FirePitFuel.of(new ItemStack(fuel)).orElseThrow();
        while (pit.feed(burning)) {
            // lay on as much as the fire holds
        }
        BlockPos abs = helper.absolutePos(PIT);
        helper.assertTrue(((FirePitBlock) helper.getBlockState(PIT).getBlock()).ignite(helper.getLevel(), abs, helper.getBlockState(PIT)), "the pit lights");
        return pit;
    }

    private static void run(GameTestHelper helper, FirePitBlockEntity pit, int ticks) {
        BlockPos abs = helper.absolutePos(PIT);
        for (int i = 0; i < ticks; i++) FirePitBlockEntity.serverTick(helper.getLevel(), abs, helper.getLevel().getBlockState(abs), pit);
    }

    // Four pieces fit, a fifth does not, and hot charcoal fires them all.
    private static void firesFourPieces(GameTestHelper helper) {
        FirePitBlockEntity pit = lit(helper, Items.CHARCOAL);
        Item[] raw = {ModItems.UNFIRED_CRUCIBLE.get(), ModItems.UNFIRED_INGOT_MOLD.get(), ModItems.UNFIRED_BRICK.get(), ModItems.UNFIRED_CRUCIBLE.get()};
        for (Item item : raw) {
            helper.assertTrue(pit.placeOnHearth(new ItemStack(item, 3)) >= 0, "a piece should fit");
        }
        ItemStack fifth = new ItemStack(ModItems.UNFIRED_BRICK.get(), 2);
        helper.assertValueEqual(pit.placeOnHearth(fifth), -1, "a fifth piece has no spot");
        helper.assertValueEqual(fifth.getCount(), 2, "nothing is taken when there is no spot");
        helper.assertTrue(pit.hearth().get(0).getCount() == 1, "one piece is set down at a time");

        run(helper, pit, 400);
        helper.assertTrue(pit.hearth().get(0).is(ModItems.UNFIRED_CRUCIBLE.get()), "still raw while the pit warms");
        run(helper, pit, 4000);
        helper.assertTrue(pit.hearth().get(0).is(ModItems.CRUCIBLE.get()), "the crucible is fired, got " + pit.hearth().get(0));
        helper.assertTrue(pit.hearth().get(1).is(ModItems.INGOT_MOLD.get()), "the mold is fired, got " + pit.hearth().get(1));
        helper.assertTrue(pit.hearth().get(2).is(Items.BRICK), "the brick is fired, got " + pit.hearth().get(2));
        helper.assertTrue(pit.hearth().get(3).is(ModItems.CRUCIBLE.get()), "the fourth is fired, got " + pit.hearth().get(3));
        helper.succeed();
    }

    // A fire of sticks never gets past 400 degrees, so clay beside it stays raw.
    private static void sticksAreTooCold(GameTestHelper helper) {
        FirePitBlockEntity pit = lit(helper, Items.STICK);
        pit.placeOnHearth(new ItemStack(ModItems.UNFIRED_CRUCIBLE.get()));
        run(helper, pit, 4000);
        helper.assertTrue(pit.hearth().get(0).is(ModItems.UNFIRED_CRUCIBLE.get()), "sticks are too cool to fire clay");
        helper.assertTrue(pit.temperature() < FirePitBlockEntity.FIRING_TEMPERATURE, "sticks stay under the firing heat");
        helper.succeed();
    }

    // The last piece set down comes back first, and an emptied hearth reports empty.
    private static void takeBack(GameTestHelper helper) {
        helper.setBlock(PIT, ModBlocks.FIRE_PIT.get().defaultBlockState());
        FirePitBlockEntity pit = helper.getBlockEntity(PIT, FirePitBlockEntity.class);
        helper.assertTrue(pit.hearthEmpty(), "a new pit has an empty hearth");
        pit.placeOnHearth(new ItemStack(ModItems.UNFIRED_BRICK.get()));
        pit.placeOnHearth(new ItemStack(ModItems.UNFIRED_CRUCIBLE.get()));
        helper.assertTrue(pit.takeFromHearth().is(ModItems.UNFIRED_CRUCIBLE.get()), "the last piece comes back first");
        helper.assertTrue(pit.takeFromHearth().is(ModItems.UNFIRED_BRICK.get()), "then the one before");
        helper.assertTrue(pit.takeFromHearth().isEmpty() && pit.hearthEmpty(), "nothing left");
        helper.succeed();
    }
}
