package dev.strataindustria.gametest;

import dev.strataindustria.coking.CokeOvenBlock;
import dev.strataindustria.coking.CokeOvenBlockEntity;
import dev.strataindustria.coking.CokeOvenStructure;
import dev.strataindustria.geology.OreGrade;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.material.Metal;
import dev.strataindustria.metal.Alloy;
import dev.strataindustria.metal.Melt;
import dev.strataindustria.metal.MetalContent;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.Tier4Blocks;
import dev.strataindustria.registry.Tier4Items;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Tier 4 (steel and steam) tests, run by {@link ModGameTests}. */
final class Tier4GameTests {
    private Tier4GameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("tier4_alloy_rules", Tier4GameTests::alloyRules);
        tests.put("tier4_coke_oven", Tier4GameTests::cokeOven);
    }

    // Spec 4.3: the example batches for steel, pig iron, brass and solder, and the gap between steel and pig iron.
    private static void alloyRules(GameTestHelper helper) {
        Melt carbon = new Melt(Map.of(Metal.CARBON, 5), 0);
        Melt steel = melt(Items.IRON_INGOT, 5).plus(carbon);
        helper.assertValueEqual(Alloy.resultOf(steel).orElse(null), Metal.STEEL, "5 iron ingots + 5 carbon");

        Melt coFusion = melt(ModItems.ingot(Metal.PIG_IRON), 1).plus(melt(Items.IRON_INGOT, 3));
        helper.assertValueEqual(Alloy.resultOf(coFusion).orElse(null), Metal.STEEL, "1 pig iron + 3 iron ingots");

        helper.assertValueEqual(Alloy.resultOf(melt(ModItems.ingot(Metal.PIG_IRON), 2)).orElse(null), Metal.PIG_IRON, "pig iron remelt");
        helper.assertValueEqual(Alloy.resultOf(melt(ModItems.ingot(Metal.STEEL), 3)).orElse(null), Metal.STEEL, "steel remelt");
        helper.assertValueEqual(Alloy.resultOf(melt(Items.IRON_INGOT, 4)).orElse(null), Metal.WROUGHT_IRON, "iron remelt");

        Melt gap = new Melt(Map.of(Metal.WROUGHT_IRON, 974, Metal.CARBON, 26), 0);
        helper.assertTrue(Alloy.resultOf(gap).isEmpty(), "iron with 2.6% carbon should be no known alloy");

        Melt brass = melt(Items.COPPER_INGOT, 2).plus(melt(ModItems.ingot(Metal.ZINC), 1));
        helper.assertValueEqual(Alloy.resultOf(brass).orElse(null), Metal.BRASS, "2 copper + 1 zinc");
        Melt solder = melt(ModItems.NUGGETS.get(Metal.TIN).get(), 6).plus(melt(ModItems.NUGGETS.get(Metal.LEAD).get(), 4));
        helper.assertValueEqual(Alloy.resultOf(solder).orElse(null), Metal.SOLDER, "6 tin + 4 lead nuggets");

        // Spec 4.4: galena melts like the tier 2 ores; sphalerite has to be roasted first.
        helper.assertValueEqual(Alloy.resultOf(melt(ModItems.crushedOre(OreMineral.GALENA, OreGrade.NORMAL), 2)).orElse(null),
                Metal.LEAD, "crushed galena");
        helper.assertTrue(MetalContent.of(new ItemStack(ModItems.crushedOre(OreMineral.SPHALERITE, OreGrade.NORMAL))).isEmpty(),
                "sphalerite should not melt");
        helper.succeed();
    }

    // Spec 5.1 and 22: the oven forms only when whole and hollow; 16 coal give 16 coke and 4000 mB of
    // creosote in 16 x 1800 ticks; a bucket draws 1000 mB; a full tank pauses it.
    private static void cokeOven(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos chamber = helper.absolutePos(new BlockPos(4, 2, 4));
        BlockPos door = chamber.south();
        for (int dx = -1; dx <= 1; dx++)
            for (int dy = -1; dy <= 1; dy++)
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos pos = chamber.offset(dx, dy, dz);
                    if (!pos.equals(chamber) && !pos.equals(door)) level.setBlock(pos, Tier4Blocks.COKE_OVEN_BRICKS.get().defaultBlockState(), Block.UPDATE_ALL);
                }
        level.setBlock(door, Tier4Blocks.COKE_OVEN_DOOR.get().defaultBlockState().setValue(CokeOvenBlock.FACING, Direction.SOUTH), Block.UPDATE_ALL);
        CokeOvenBlockEntity oven = (CokeOvenBlockEntity) level.getBlockEntity(door);
        helper.assertTrue(oven.checkStructure().complete(), "a whole oven should form, got " + oven.checkStructure());

        BlockPos corner = chamber.offset(1, -1, -1);
        level.removeBlock(corner, false);
        helper.assertValueEqual(oven.checkStructure(), new CokeOvenStructure.Result(CokeOvenStructure.Problem.NEEDS_BRICK, corner), "missing brick");
        level.setBlock(corner, Tier4Blocks.COKE_OVEN_BRICKS.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(chamber, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        helper.assertValueEqual(oven.checkStructure(), new CokeOvenStructure.Result(CokeOvenStructure.Problem.NEEDS_AIR, chamber), "filled chamber");
        level.removeBlock(chamber, false);
        helper.assertTrue(oven.checkStructure().complete(), "the oven should form again");

        oven.setItem(CokeOvenBlockEntity.INPUT, new ItemStack(Items.COAL, 16));
        run(level, door, oven, 16 * 1800);
        ItemStack coke = oven.getItem(CokeOvenBlockEntity.OUTPUT);
        helper.assertTrue(coke.is(Tier4Items.COKE.get()) && coke.getCount() == 16, "16 coal should give 16 coke, got " + coke);
        helper.assertValueEqual(oven.creosote(), 4000, "creosote from 16 coal");
        helper.assertValueEqual(oven.status(), CokeOvenBlockEntity.Status.EMPTY, "status once the coal is gone");
        helper.assertTrue(!level.getBlockState(door).getValue(CokeOvenBlock.LIT), "the door goes dark when the oven is empty");

        oven.setItem(CokeOvenBlockEntity.BUCKET_IN, new ItemStack(Items.BUCKET));
        run(level, door, oven, 1);
        helper.assertTrue(oven.getItem(CokeOvenBlockEntity.BUCKET_OUT).is(Tier4Items.CREOSOTE_BUCKET.get()), "a bucket should be filled");
        helper.assertValueEqual(oven.creosote(), 3000, "creosote after one bucket");

        // 3000 + 52 x 250 = 16000 mB fills the tank exactly; the 53rd coal has to wait.
        oven.setItem(CokeOvenBlockEntity.OUTPUT, ItemStack.EMPTY);
        oven.setItem(CokeOvenBlockEntity.INPUT, new ItemStack(Items.COAL, 53));
        run(level, door, oven, 53 * 1800);
        helper.assertValueEqual(oven.creosote(), CokeOvenBlockEntity.CAPACITY, "a full tank");
        helper.assertValueEqual(oven.status(), CokeOvenBlockEntity.Status.TANK_FULL, "status with a full tank");
        helper.assertValueEqual(oven.getItem(CokeOvenBlockEntity.INPUT).getCount(), 1, "the last coal waits");
        helper.succeed();
    }

    private static void run(ServerLevel level, BlockPos pos, CokeOvenBlockEntity oven, int ticks) {
        for (int i = 0; i < ticks; i++) CokeOvenBlockEntity.serverTick(level, pos, level.getBlockState(pos), oven);
    }

    private static Melt melt(net.minecraft.world.item.Item item, int count) {
        Melt one = MetalContent.of(new ItemStack(item)).orElseThrow();
        Melt total = Melt.EMPTY;
        for (int i = 0; i < count; i++) total = total.plus(one);
        return total;
    }
}
