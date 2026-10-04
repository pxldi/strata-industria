package dev.strataindustria.gametest;

import dev.strataindustria.bloomery.BloomeryStructure;
import dev.strataindustria.coking.CokeOvenStructure;
import dev.strataindustria.ironworks.BlastFurnaceStructure;
import dev.strataindustria.ironworks.ConverterStructure;
import dev.strataindustria.multiblock.Multiblock;
import dev.strataindustria.multiblock.MultiblockEvents;
import dev.strataindustria.multiblock.Multiblocks;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.Tier4Blocks;
import dev.strataindustria.steam.SteelBoilerStructure;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Multiblock patterns as data: the five shipped files load, the checks form from them, hand-built variants still
 * count, and the ghost and the "Structure incomplete" message name the right block. Run by {@link ModGameTests}.
 */
final class MultiblockGameTests {
    private MultiblockGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("multiblock_patterns_load", MultiblockGameTests::patternsLoad);
        tests.put("multiblock_bloomery_chimney", MultiblockGameTests::bloomeryChimney);
        tests.put("multiblock_blast_furnace_variants", MultiblockGameTests::blastFurnaceVariants);
        tests.put("multiblock_converter_turned", MultiblockGameTests::converterTurned);
        tests.put("multiblock_boiler_layers", MultiblockGameTests::boilerLayers);
        tests.put("multiblock_ghost_and_message", MultiblockGameTests::ghostAndMessage);
    }

    private static void build(ServerLevel level, ResourceKey<Multiblock> key, BlockPos controller, Direction facing) {
        Multiblocks.get(level, key).parts(controller, facing).forEach(part -> level.setBlock(part.pos(), part.state(), Block.UPDATE_ALL));
    }

    // Each file loads, names the right controller and lays out the standard build (block counts from the specs).
    private static void patternsLoad(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos at = helper.absolutePos(new BlockPos(4, 1, 3));
        Object[][] expected = {
                {Multiblocks.BLOOMERY, ModBlocks.BLOOMERY.get(), 8},
                {Multiblocks.COKE_OVEN, Tier4Blocks.COKE_OVEN_DOOR.get(), 25},
                {Multiblocks.BLAST_FURNACE, Tier4Blocks.BLAST_FURNACE_CONTROLLER.get(), 41},
                {Multiblocks.CONVERTER, Tier4Blocks.CONVERTER_CONTROLLER.get(), 25},
                {Multiblocks.STEEL_BOILER, Tier4Blocks.BOILER_CONTROLLER.get(), 26},
        };
        for (Object[] row : expected) {
            @SuppressWarnings("unchecked") ResourceKey<Multiblock> key = (ResourceKey<Multiblock>) row[0];
            Multiblock multiblock = Multiblocks.get(level, key);
            helper.assertValueEqual(multiblock.controller(), (Block) row[1], key.identifier() + " controller");
            helper.assertValueEqual(multiblock.parts(at, Direction.NORTH).size(), (Integer) row[2], key.identifier() + " standard build size");
            helper.assertTrue(Multiblocks.forController(level, multiblock.controller()).isPresent(), key.identifier() + " found by its controller");
        }
        helper.succeed();
    }

    // The chimney is a repeating layer: one to three levels count, a fourth does not, and the top must stay clear of full blocks.
    private static void bloomeryChimney(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos controller = helper.absolutePos(new BlockPos(4, 0, 3));
        BlockPos chamber = controller.south();
        BlockState bricks = ModBlocks.FIRE_BRICKS.get().defaultBlockState();
        build(level, Multiblocks.BLOOMERY, controller, Direction.NORTH);
        // The chamber sits in the test floor, so dig it out.
        level.removeBlock(chamber, false);
        BloomeryStructure.Result one = BloomeryStructure.check(level, controller, Direction.NORTH);
        helper.assertTrue(one.complete(), "a standard bloomery forms, problem " + one.problem() + " at " + one.at());
        helper.assertValueEqual(one.chimney(), 1, "chimney levels of the standard build");

        for (int k = 2; k <= 4; k++) {
            for (Direction side : Direction.Plane.HORIZONTAL) level.setBlock(chamber.above(k).relative(side), bricks, Block.UPDATE_ALL);
            BloomeryStructure.Result grown = BloomeryStructure.check(level, controller, Direction.NORTH);
            helper.assertValueEqual(grown.chimney(), Math.min(k, 3), "chimney with " + k + " levels built, problem " + grown.problem() + " at " + grown.at().subtract(controller));
        }
        // A level that stops short does not break the ones under it.
        level.removeBlock(chamber.above(2).west(), false);
        helper.assertValueEqual(BloomeryStructure.check(level, controller, Direction.NORTH).chimney(), 1, "the broken second level ends the chimney");
        level.setBlock(chamber.above(2).west(), bricks, Block.UPDATE_ALL);

        // The top of the chimney may not be capped. With three levels it is the fourth one up.
        BlockPos top = chamber.above(4);
        level.setBlock(top, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        BloomeryStructure.Result capped = BloomeryStructure.check(level, controller, Direction.NORTH);
        helper.assertValueEqual(capped.problem(), BloomeryStructure.Problem.NEEDS_AIR, "a capped chimney");
        helper.assertValueEqual(capped.at(), top, "where the cap is");
        level.removeBlock(top, false);

        level.removeBlock(chamber.below(), false);
        BloomeryStructure.Result floor = BloomeryStructure.check(level, controller, Direction.NORTH);
        helper.assertValueEqual(floor.problem(), BloomeryStructure.Problem.NEEDS_BRICK, "no floor");
        helper.assertValueEqual(floor.at(), chamber.below(), "where the floor is missing");
        helper.succeed();
    }

    // Edge blocks of the hearth may be tuyeres or one tap hatch; a second tap hatch does not fit, and a missing one is named.
    private static void blastFurnaceVariants(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos controller = helper.absolutePos(new BlockPos(4, 0, 2));
        BlockPos centre = controller.south();
        build(level, Multiblocks.BLAST_FURNACE, controller, Direction.NORTH);
        BlastFurnaceStructure.Result built = BlastFurnaceStructure.check(level, controller, Direction.NORTH);
        helper.assertTrue(built.complete(), "a standard furnace forms, problem " + built.problem() + " at " + built.at());
        helper.assertValueEqual(built.tuyeres().size(), 1, "one tuyere");
        helper.assertValueEqual(built.tuyeres().getFirst().pos(), centre.east(), "the tuyere is on the left edge");
        helper.assertValueEqual(built.tuyeres().getFirst().out(), Direction.EAST, "and faces out of the furnace");
        helper.assertValueEqual(built.tap(), centre.south(), "the tap hatch is behind");
        helper.assertValueEqual(built.hatch(), centre.above(4), "the charging hatch is over the shaft");

        level.setBlock(centre.west(), Tier4Blocks.TUYERE.get().defaultBlockState(), Block.UPDATE_ALL);
        BlastFurnaceStructure.Result two = BlastFurnaceStructure.check(level, controller, Direction.NORTH);
        helper.assertTrue(two.complete(), "a second tuyere is a hand-built variant");
        helper.assertValueEqual(two.tuyeres().size(), 2, "two tuyeres");

        level.setBlock(centre.west(), Tier4Blocks.TAP_HATCH.get().defaultBlockState(), Block.UPDATE_ALL);
        BlastFurnaceStructure.Result twoTaps = BlastFurnaceStructure.check(level, controller, Direction.NORTH);
        helper.assertValueEqual(twoTaps.problem(), BlastFurnaceStructure.Problem.NEEDS_CASING, "a second tap hatch");
        helper.assertTrue(twoTaps.at().equals(centre.west()) || twoTaps.at().equals(centre.south()), "is one of the two tap hatches, not " + twoTaps.at());

        BlockState casing = Tier4Blocks.REFRACTORY_CASING.get().defaultBlockState();
        level.setBlock(centre.west(), casing, Block.UPDATE_ALL);
        level.setBlock(centre.south(), casing, Block.UPDATE_ALL);
        BlastFurnaceStructure.Result noTap = BlastFurnaceStructure.check(level, controller, Direction.NORTH);
        helper.assertValueEqual(noTap.problem(), BlastFurnaceStructure.Problem.NEEDS_TAP_HATCH, "no tap hatch");
        helper.assertValueEqual(noTap.at().distManhattan(centre), 1, "named at an edge of the hearth");
        Multiblock.Mismatch ghost = Multiblocks.get(level, Multiblocks.BLAST_FURNACE).mismatches(level, controller, Direction.NORTH).getFirst();
        helper.assertValueEqual(ghost.pos(), centre.south(), "the ghost asks for the tap hatch where the standard build has it");
        helper.assertValueEqual(ghost.expected().getBlock(), Tier4Blocks.TAP_HATCH.get(), "and shows a tap hatch");
        helper.assertValueEqual(ghost.fault(), Multiblock.Fault.WRONG, "over the casing that sits there");

        level.setBlock(centre.south(), Tier4Blocks.TAP_HATCH.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(centre.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        BlastFurnaceStructure.Result bosh = BlastFurnaceStructure.check(level, controller, Direction.NORTH);
        helper.assertTrue(bosh.complete(), "the shaft is open");
        level.setBlock(centre.above(2), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        helper.assertValueEqual(BlastFurnaceStructure.check(level, controller, Direction.NORTH).problem(), BlastFurnaceStructure.Problem.NEEDS_AIR, "a filled shaft");
        helper.succeed();
    }

    // A pattern follows the controller's facing: the same build turned east puts the tuyere on the other axis.
    private static void converterTurned(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos controller = helper.absolutePos(new BlockPos(4, 0, 3));
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            build(level, Multiblocks.CONVERTER, controller, facing);
            BlockPos vessel = ConverterStructure.vessel(controller, facing);
            var result = ConverterStructure.check(level, controller, facing);
            helper.assertTrue(result.complete(), "a converter facing " + facing + " forms, problem " + result.problem() + " at " + result.at());
            helper.assertValueEqual(result.tuyeres().getFirst().pos(), vessel.relative(facing.getClockWise()), "tuyere on the left of a converter facing " + facing);
            helper.assertValueEqual(result.tap(), vessel.above().relative(facing.getOpposite()), "tap behind a converter facing " + facing);
            helper.assertValueEqual(result.hatch(), vessel.above(2), "hatch on top of a converter facing " + facing);
            Multiblocks.get(level, Multiblocks.CONVERTER).parts(controller, facing).forEach(part -> level.removeBlock(part.pos(), false));
        }
        helper.succeed();
    }

    // Shell layers beyond the second are optional, but one that is started must be whole.
    private static void boilerLayers(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos controller = helper.absolutePos(new BlockPos(4, 1, 3));
        BlockPos centre = controller.south();
        build(level, Multiblocks.STEEL_BOILER, controller, Direction.NORTH);
        SteelBoilerStructure.Result base = SteelBoilerStructure.check(level, controller, Direction.NORTH);
        helper.assertTrue(base.complete(), "a standard boiler forms, problem " + base.problem() + " at " + base.at());
        helper.assertValueEqual(base.layers(), 2, "shell layers");
        helper.assertValueEqual(base.steamPorts(), List.of(centre.above().east()), "the steam port on the left of the second layer");
        helper.assertValueEqual(base.parts().size(), 8 + 9, "shell blocks and ports that forward to the controller");
        helper.assertValueEqual(base.inlets().size(), 0, "no heat inlets");

        BlockState shell = Tier4Blocks.STEEL_BOILER_SHELL.get().defaultBlockState();
        for (int layer = 2; layer <= 3; layer++) {
            level.setBlock(centre.above(layer).north(), shell, Block.UPDATE_ALL);
            SteelBoilerStructure.Result started = SteelBoilerStructure.check(level, controller, Direction.NORTH);
            helper.assertValueEqual(started.problem(), SteelBoilerStructure.Problem.NEEDS_SHELL, "layer " + (layer + 1) + " started with one block");
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++) level.setBlock(centre.above(layer).offset(dx, 0, dz), shell, Block.UPDATE_ALL);
            helper.assertValueEqual(SteelBoilerStructure.check(level, controller, Direction.NORTH).layers(), layer + 1, "layers with " + (layer + 1) + " built");
        }
        level.setBlock(centre.above(4), shell, Block.UPDATE_ALL);
        helper.assertValueEqual(SteelBoilerStructure.check(level, controller, Direction.NORTH).layers(), 4, "a fifth layer is not counted");

        level.setBlock(centre.above().east(), shell, Block.UPDATE_ALL);
        SteelBoilerStructure.Result noSteam = SteelBoilerStructure.check(level, controller, Direction.NORTH);
        helper.assertValueEqual(noSteam.problem(), SteelBoilerStructure.Problem.NEEDS_STEAM_PORT, "no steam port");
        helper.succeed();
    }

    // The ghost marks what is missing and what is in the way; the message names the first block and its place.
    private static void ghostAndMessage(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos door = helper.absolutePos(new BlockPos(4, 2, 5));
        Multiblock oven = Multiblocks.get(level, Multiblocks.COKE_OVEN);
        build(level, Multiblocks.COKE_OVEN, door, Direction.NORTH);
        helper.assertTrue(CokeOvenStructure.check(level, door, Direction.NORTH).complete(), "a whole oven");
        helper.assertTrue(oven.mismatches(level, door, Direction.NORTH).isEmpty(), "nothing to show for a whole oven");
        helper.assertTrue(MultiblockEvents.incomplete(level, oven, door, Direction.NORTH) == null, "nothing to say about a whole oven");

        BlockPos corner = door.south().offset(1, -1, 1);
        level.removeBlock(corner, false);
        List<Multiblock.Mismatch> missing = oven.mismatches(level, door, Direction.NORTH);
        helper.assertValueEqual(missing.size(), 1, "one block short");
        helper.assertValueEqual(missing.getFirst().pos(), corner, "the ghost sits where it is missing");
        helper.assertValueEqual(missing.getFirst().fault(), Multiblock.Fault.MISSING, "and calls it missing");
        helper.assertValueEqual(missing.getFirst().expected().getBlock(), Tier4Blocks.COKE_OVEN_BRICKS.get(), "of coke oven bricks");
        Component message = MultiblockEvents.incomplete(level, oven, door, Direction.NORTH);
        helper.assertTrue(message.getContents() instanceof TranslatableContents, "the message is a translation");
        TranslatableContents contents = (TranslatableContents) message.getContents();
        helper.assertValueEqual(contents.getKey(), "strataindustria.multiblock.incomplete", "message key");
        helper.assertValueEqual(contents.getArgs()[1], corner.toShortString(), "message position");
        helper.assertValueEqual(((Component) contents.getArgs()[0]).getString(), Tier4Blocks.COKE_OVEN_BRICKS.get().getName().getString(), "message block");

        level.setBlock(corner, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
        helper.assertValueEqual(oven.mismatches(level, door, Direction.NORTH).getFirst().fault(), Multiblock.Fault.WRONG, "a wrong block is marked wrong");
        level.setBlock(corner, Tier4Blocks.COKE_OVEN_BRICKS.get().defaultBlockState(), Block.UPDATE_ALL);

        BlockPos chamber = door.south();
        level.setBlock(chamber, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        Multiblock.Mismatch clog = oven.mismatches(level, door, Direction.NORTH).getFirst();
        helper.assertValueEqual(clog.pos(), chamber, "the filled chamber");
        helper.assertTrue(clog.expected() == null, "must be cleared, not filled");
        TranslatableContents clear = (TranslatableContents) MultiblockEvents.incomplete(level, oven, door, Direction.NORTH).getContents();
        helper.assertValueEqual(((Component) clear.getArgs()[0]).getString(), Component.translatable("strataindustria.multiblock.empty_space").getString(), "clearing is named");
        helper.succeed();
    }
}
