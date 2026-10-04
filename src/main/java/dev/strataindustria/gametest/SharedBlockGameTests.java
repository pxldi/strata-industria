package dev.strataindustria.gametest;

import com.mojang.serialization.JsonOps;
import dev.strataindustria.geology.Rock;
import dev.strataindustria.structure.CrateBlock;
import dev.strataindustria.structure.CrateBlockEntity;
import dev.strataindustria.structure.MinersLampBlock;
import dev.strataindustria.structure.OreCartBlock;
import dev.strataindustria.structure.OreCartBlockEntity;
import dev.strataindustria.structure.PuzzleLock;
import dev.strataindustria.structure.RubbleBlock;
import dev.strataindustria.structure.SharedBlocks;
import dev.strataindustria.structure.StructureContent;
import dev.strataindustria.structure.ToolRackBlockEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;

/** The shared structure blocks and the puzzle lock on the crate (structures v2 sections 2a and 5), run by {@link ModGameTests}. */
final class SharedBlockGameTests {
    private SharedBlockGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("shared_blocks_complete", SharedBlockGameTests::complete);
        tests.put("shared_lamp_light", SharedBlockGameTests::lampLight);
        tests.put("shared_crate_lamp_puzzle", SharedBlockGameTests::lampPuzzle);
        tests.put("shared_crate_block_puzzle", SharedBlockGameTests::blockPuzzle);
        tests.put("shared_puzzle_codec", SharedBlockGameTests::puzzleCodec);
        tests.put("shared_ore_cart_fill", SharedBlockGameTests::oreCartFill);
        tests.put("shared_tool_rack", SharedBlockGameTests::toolRack);
        tests.put("shared_rubble_layers", SharedBlockGameTests::rubbleLayers);
    }

    /** Every shared block has an item, a loot table and a registered block entity where it needs one. */
    private static void complete(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        List<String> problems = new ArrayList<>();
        List<Block> blocks = new ArrayList<>(List.of(SharedBlocks.CRATE.get(), SharedBlocks.MINERS_LAMP.get(), SharedBlocks.ORE_CART.get(),
                SharedBlocks.TOOL_RACK.get(), SharedBlocks.SMOULDERING_LOG_PILE.get(), SharedBlocks.WINDLASS.get(), SharedBlocks.SLUICE_BOX.get()));
        for (Rock rock : Rock.values()) {
            blocks.add(SharedBlocks.RUBBLE.get(rock).get());
            blocks.add(SharedBlocks.CRACKED.get(rock).get());
            blocks.add(SharedBlocks.MOSSY_COBBLED.get(rock).get());
        }
        for (Block block : blocks) {
            String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
            if (block.asItem() == Items.AIR) problems.add(name + " has no item");
            if (server.reloadableRegistries().getLootTable(block.getLootTable().orElseThrow()) == LootTable.EMPTY) problems.add(name + " has no loot table");
        }
        helper.assertTrue(problems.isEmpty(), "shared block problems: " + problems);
        helper.assertTrue(SharedBlocks.CRATE.get().newBlockEntity(BlockPos.ZERO, SharedBlocks.CRATE.get().defaultBlockState()) != null, "crate has no block entity");
        helper.succeed();
    }

    /** Burning, guttering (steady and dipped) and out give the light the spec asks for. */
    private static void lampLight(GameTestHelper helper) {
        BlockState lamp = SharedBlocks.MINERS_LAMP.get().defaultBlockState();
        helper.assertTrue(lamp.getLightEmission() == 12, "a lit lamp gives " + lamp.getLightEmission());
        BlockState guttering = lamp.setValue(MinersLampBlock.MODE, MinersLampBlock.Mode.GUTTERING);
        helper.assertTrue(guttering.getLightEmission() == 6, "a guttering lamp gives " + guttering.getLightEmission());
        helper.assertTrue(guttering.setValue(MinersLampBlock.DIM, true).getLightEmission() == 3, "a dipped flame is not dimmer");
        helper.assertTrue(lamp.setValue(MinersLampBlock.MODE, MinersLampBlock.Mode.OFF).getLightEmission() == 0, "an unlit lamp gives light");
        helper.succeed();
    }

    private static BlockPos lamp(GameTestHelper helper, int x) {
        BlockPos pos = new BlockPos(x, 1, 3);
        helper.setBlock(pos, SharedBlocks.MINERS_LAMP.get().defaultBlockState().setValue(MinersLampBlock.MODE, MinersLampBlock.Mode.OFF));
        return helper.absolutePos(pos);
    }

    private static void light(GameTestHelper helper, BlockPos abs) {
        ServerLevel level = helper.getLevel();
        level.setBlock(abs, level.getBlockState(abs).setValue(MinersLampBlock.MODE, MinersLampBlock.Mode.LIT), 3);
        PuzzleLock.lampChanged(level, abs, true);
    }

    private static MinersLampBlock.Mode mode(GameTestHelper helper, BlockPos abs) {
        return helper.getLevel().getBlockState(abs).getValue(MinersLampBlock.MODE);
    }

    /** Three lamps lit in the listed order open the crate; one out of turn snuffs them all and starts over. */
    private static void lampPuzzle(GameTestHelper helper) {
        BlockPos cratePos = new BlockPos(1, 1, 1);
        helper.setBlock(cratePos, SharedBlocks.CRATE.get().defaultBlockState());
        CrateBlockEntity crate = helper.getBlockEntity(cratePos, CrateBlockEntity.class);
        BlockPos a = lamp(helper, 1);
        BlockPos b = lamp(helper, 2);
        BlockPos c = lamp(helper, 3);
        crate.lock(PuzzleLock.lamps(List.of(a, b, c)));
        helper.assertTrue(helper.getBlockState(cratePos).getValue(CrateBlock.LOCKED), "the crate did not lock");

        light(helper, a);
        light(helper, c);
        helper.assertTrue(mode(helper, a) == MinersLampBlock.Mode.OFF && mode(helper, c) == MinersLampBlock.Mode.OFF,
                "a lamp out of turn did not snuff the others");
        helper.assertTrue(helper.getBlockState(cratePos).getValue(CrateBlock.LOCKED), "the crate opened on a wrong order");

        light(helper, a);
        light(helper, b);
        helper.assertTrue(helper.getBlockState(cratePos).getValue(CrateBlock.LOCKED), "the crate opened early");
        light(helper, c);
        helper.assertTrue(!helper.getBlockState(cratePos).getValue(CrateBlock.LOCKED), "the right order did not open the crate");
        helper.assertTrue(crate.puzzle() == null, "the puzzle stayed after it was solved");
        helper.succeed();
    }

    /** A crate that waits for blocks to be set opens when the last one is placed, in any order. */
    private static void blockPuzzle(GameTestHelper helper) {
        BlockPos cratePos = new BlockPos(1, 1, 1);
        helper.setBlock(cratePos, SharedBlocks.CRATE.get().defaultBlockState());
        CrateBlockEntity crate = helper.getBlockEntity(cratePos, CrateBlockEntity.class);
        BlockPos one = helper.absolutePos(new BlockPos(2, 1, 3));
        BlockPos two = helper.absolutePos(new BlockPos(3, 1, 3));
        String bricks = BuiltInRegistries.BLOCK.getKey(StructureContent.CRACKED_FIRE_BRICKS.get()).toString();
        crate.lock(PuzzleLock.blocks(List.of(new PuzzleLock.Step(one, bricks), new PuzzleLock.Step(two, bricks))));
        ServerLevel level = helper.getLevel();

        level.setBlock(two, StructureContent.CRACKED_FIRE_BRICKS.get().defaultBlockState(), 3);
        PuzzleLock.blockPlaced(level, two);
        helper.assertTrue(helper.getBlockState(cratePos).getValue(CrateBlock.LOCKED), "one brick of two opened the crate");
        level.setBlock(one, StructureContent.CRACKED_FIRE_BRICKS.get().defaultBlockState(), 3);
        PuzzleLock.blockPlaced(level, one);
        helper.assertTrue(!helper.getBlockState(cratePos).getValue(CrateBlock.LOCKED), "both bricks did not open the crate");
        helper.succeed();
    }

    /** A puzzle survives being saved with its crate. */
    private static void puzzleCodec(GameTestHelper helper) {
        PuzzleLock lock = new PuzzleLock(List.of(new PuzzleLock.Step(new BlockPos(1, 2, 3), "strataindustria:miners_lamp"),
                new PuzzleLock.Step(new BlockPos(4, 5, 6), "strataindustria:miners_lamp")), true, 1);
        var json = PuzzleLock.CODEC.encodeStart(JsonOps.INSTANCE, lock).getOrThrow();
        PuzzleLock back = PuzzleLock.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        helper.assertTrue(back.equals(lock), "puzzle changed in a round trip: " + back);
        helper.succeed();
    }

    /** The cart looks as full as its slots are. */
    private static void oreCartFill(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, SharedBlocks.ORE_CART.get().defaultBlockState());
        OreCartBlockEntity cart = helper.getBlockEntity(pos, OreCartBlockEntity.class);
        helper.assertTrue(helper.getBlockState(pos).getValue(OreCartBlock.FILL) == 0, "a new cart is not empty");
        cart.setItem(0, new ItemStack(Items.COAL));
        helper.assertTrue(helper.getBlockState(pos).getValue(OreCartBlock.FILL) == 1, "one slot does not show a layer");
        for (int i = 1; i < 9; i++) cart.setItem(i, new ItemStack(Items.COAL));
        helper.assertTrue(helper.getBlockState(pos).getValue(OreCartBlock.FILL) == 3, "nine slots do not show a heap");
        for (int i = 0; i < 9; i++) cart.setItem(i, ItemStack.EMPTY);
        helper.assertTrue(helper.getBlockState(pos).getValue(OreCartBlock.FILL) == 0, "an emptied cart still looks full");
        helper.succeed();
    }

    /** Three pegs; tools hang, other things do not. */
    private static void toolRack(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, SharedBlocks.TOOL_RACK.get().defaultBlockState());
        ToolRackBlockEntity rack = helper.getBlockEntity(pos, ToolRackBlockEntity.class);
        helper.assertTrue(!rack.hang(new ItemStack(Items.STONE)), "a stone hung on a peg");
        helper.assertTrue(rack.hang(new ItemStack(Items.IRON_PICKAXE)), "a pickaxe did not hang");
        helper.assertTrue(rack.hang(new ItemStack(Items.IRON_AXE)), "an axe did not hang");
        helper.assertTrue(rack.hang(new ItemStack(Items.IRON_SHOVEL)), "a shovel did not hang");
        helper.assertTrue(!rack.hang(new ItemStack(Items.IRON_HOE)), "a fourth tool fit");
        helper.assertTrue(rack.takeLast().is(Items.IRON_SHOVEL), "took the wrong tool");
        helper.succeed();
    }

    /** Rubble gets deeper with its layers and is not solid. */
    private static void rubbleLayers(GameTestHelper helper) {
        BlockState one = SharedBlocks.RUBBLE.get(Rock.GRANITE).get().defaultBlockState();
        BlockState three = one.setValue(RubbleBlock.LAYERS, 3);
        double low = one.getShape(helper.getLevel(), BlockPos.ZERO).max(net.minecraft.core.Direction.Axis.Y);
        double high = three.getShape(helper.getLevel(), BlockPos.ZERO).max(net.minecraft.core.Direction.Axis.Y);
        helper.assertTrue(low < high, "three layers are not deeper than one");
        helper.assertTrue(one.getCollisionShape(helper.getLevel(), BlockPos.ZERO).isEmpty(), "rubble has collision");
        helper.succeed();
    }
}
