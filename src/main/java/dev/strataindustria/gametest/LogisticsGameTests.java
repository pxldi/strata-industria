package dev.strataindustria.gametest;

import dev.strataindustria.electric.BatteryBoxBlockEntity;
import dev.strataindustria.fluid.FluidFilterBlock;
import dev.strataindustria.fluid.FluidFilterBlockEntity;
import dev.strataindustria.logistics.ItemPipeBlock;
import dev.strataindustria.logistics.ItemPipeBlockEntity;
import dev.strataindustria.logistics.PipeExtractorBlock;
import dev.strataindustria.logistics.PipeExtractorBlockEntity;
import dev.strataindustria.logistics.StorageControllerBlock;
import dev.strataindustria.logistics.StorageControllerBlockEntity;
import dev.strataindustria.logistics.Tier5Logistics;
import dev.strataindustria.power.ElectricNetwork;
import dev.strataindustria.power.ElectricNetworks;
import dev.strataindustria.registry.Tier4Blocks;
import dev.strataindustria.registry.Tier4DataComponents;
import dev.strataindustria.registry.Tier4Items;
import dev.strataindustria.registry.Tier5Blocks;
import java.util.List;
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
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

/** Tier 5 logistics tests (spec 12 and 24): item pipes, the extractor, the storage controller and the fluid filter. */
final class LogisticsGameTests {
    private LogisticsGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("tier5_item_pipe_nearest_and_round_robin", LogisticsGameTests::nearestAndRoundRobin);
        tests.put("tier5_item_pipe_filter", LogisticsGameTests::faceFilter);
        tests.put("tier5_pipe_extractor", LogisticsGameTests::extractor);
        tests.put("tier5_storage_controller", LogisticsGameTests::controller);
        tests.put("tier5_fluid_filter", LogisticsGameTests::fluidFilter);
    }

    // ------------------------------------------------------------------ helpers

    /** Places a pipe and lets it read its neighbours, as placing it by hand would. */
    private static void pipe(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, Tier5Logistics.ITEM_PIPE.get().defaultBlockState(), Block.UPDATE_ALL);
        BlockState state = Block.updateFromNeighbourShapes(level.getBlockState(pos), level, pos);
        level.setBlock(pos, state, Block.UPDATE_ALL);
    }

    private static void chest(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
    }

    private static void tickPipes(ServerLevel level, List<BlockPos> pipes, int ticks) {
        for (int i = 0; i < ticks; i++) {
            for (BlockPos pos : pipes) {
                if (level.getBlockEntity(pos) instanceof ItemPipeBlockEntity pipe) {
                    ItemPipeBlockEntity.serverTick(level, pos, level.getBlockState(pos), pipe);
                }
            }
        }
    }

    private static int count(ChestBlockEntity chest, net.minecraft.world.item.Item item) {
        int n = 0;
        for (int i = 0; i < chest.getContainerSize(); i++) if (chest.getItem(i).is(item)) n += chest.getItem(i).getCount();
        return n;
    }

    private static void fill(ChestBlockEntity chest, net.minecraft.world.item.Item item) {
        for (int i = 0; i < chest.getContainerSize(); i++) chest.setItem(i, new ItemStack(item, item.getDefaultMaxStackSize()));
    }

    // ------------------------------------------------------------------ tests

    private static void nearestAndRoundRobin(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos a = helper.absolutePos(new BlockPos(1, 1, 2));
        List<BlockPos> pipes = List.of(a.east(), a.east(2), a.east(3));
        chest(level, a);
        chest(level, a.east(4));
        for (BlockPos pos : pipes) pipe(level, pos);
        var west = (ChestBlockEntity) level.getBlockEntity(a);
        var east = (ChestBlockEntity) level.getBlockEntity(a.east(4));
        var near = (ItemPipeBlockEntity) level.getBlockEntity(pipes.get(0));
        var middle = (ItemPipeBlockEntity) level.getBlockEntity(pipes.get(1));
        helper.assertTrue(ItemPipeBlock.face(level.getBlockState(pipes.get(0)), Direction.WEST) == ItemPipeBlock.Face.PORT, "a chest beside a pipe is a port");
        helper.assertTrue(ItemPipeBlock.face(level.getBlockState(pipes.get(0)), Direction.EAST) == ItemPipeBlock.Face.PIPE, "a pipe beside a pipe joins");

        // The nearest container takes the item.
        helper.assertTrue(near.insert(new ItemStack(Items.IRON_NUGGET), Direction.UP), "the pipe takes the item");
        helper.runAfterDelay(30, () -> {
            tickPipes(level, pipes, 1);
            helper.assertValueEqual(count(west, Items.IRON_NUGGET), 1, "the nearer chest gets it");
            helper.assertValueEqual(count(east, Items.IRON_NUGGET), 0, "the far chest does not");
            // With the same distance on both sides the items alternate.
            for (int i = 0; i < 4; i++) helper.assertTrue(middle.insert(new ItemStack(Items.COAL), Direction.UP), "the middle pipe takes coal " + i);
            helper.runAfterDelay(80, () -> {
                tickPipes(level, pipes, 1);
                helper.assertValueEqual(count(west, Items.COAL), 2, "two coal go west");
                helper.assertValueEqual(count(east, Items.COAL), 2, "two coal go east");
                // A full chest is passed over.
                fill(west, Items.COBBLESTONE);
                helper.assertTrue(near.insert(new ItemStack(Items.IRON_NUGGET), Direction.UP), "it still takes an item for the far chest");
                helper.runAfterDelay(60, () -> {
                    tickPipes(level, pipes, 1);
                    helper.assertValueEqual(count(east, Items.IRON_NUGGET), 1, "the far chest takes what the near one cannot");
                    helper.succeed();
                });
            });
        });
    }

    private static void faceFilter(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos a = helper.absolutePos(new BlockPos(1, 1, 2));
        List<BlockPos> pipes = List.of(a.east(), a.east(2), a.east(3));
        chest(level, a);
        chest(level, a.east(4));
        for (BlockPos pos : pipes) pipe(level, pos);
        var west = (ChestBlockEntity) level.getBlockEntity(a);
        var east = (ChestBlockEntity) level.getBlockEntity(a.east(4));
        var first = (ItemPipeBlockEntity) level.getBlockEntity(pipes.get(0));
        ItemStack filter = new ItemStack(Tier4Items.FILTER.get());
        filter.set(Tier4DataComponents.FILTER_CONTENTS.get(), dev.strataindustria.automation.FilterContents.EMPTY.withEntry(0, Items.COAL));
        first.setFilter(Direction.WEST, filter);
        helper.assertTrue(first.hasFilter(Direction.WEST), "the face keeps its filter");
        helper.assertTrue(first.insert(new ItemStack(Items.IRON_NUGGET), Direction.UP), "the pipe takes iron");
        helper.assertTrue(first.insert(new ItemStack(Items.COAL), Direction.UP), "the pipe takes coal");
        helper.runAfterDelay(80, () -> {
            tickPipes(level, pipes, 1);
            helper.assertValueEqual(count(west, Items.COAL), 1, "the filtered face lets coal in");
            helper.assertValueEqual(count(west, Items.IRON_NUGGET), 0, "and keeps iron out");
            helper.assertValueEqual(count(east, Items.IRON_NUGGET), 1, "iron goes past to the far chest");
            helper.succeed();
        });
    }

    private static void extractor(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos base = helper.absolutePos(new BlockPos(1, 1, 2));
        BlockPos extractorPos = base.east(), pipePos = base.east(2), boxPos = extractorPos.above();
        chest(level, base);
        chest(level, base.east(3));
        level.setBlock(extractorPos, Tier5Logistics.PIPE_EXTRACTOR.get().defaultBlockState()
                .setValue(PipeExtractorBlock.FACING, Direction.WEST), Block.UPDATE_ALL);
        pipe(level, pipePos);
        level.setBlock(boxPos, Tier5Blocks.BATTERY_BOX.get().defaultBlockState(), Block.UPDATE_ALL);
        var source = (ChestBlockEntity) level.getBlockEntity(base);
        var target = (ChestBlockEntity) level.getBlockEntity(base.east(3));
        var extractor = (PipeExtractorBlockEntity) level.getBlockEntity(extractorPos);
        var box = (BatteryBoxBlockEntity) level.getBlockEntity(boxPos);
        source.setItem(0, new ItemStack(Items.STICK, 8));
        ElectricNetwork network = ElectricNetworks.rebuildNow(level, extractorPos);
        List<BlockPos> pipes = List.of(pipePos);

        helper.assertTrue(ItemPipeBlock.face(level.getBlockState(pipePos), Direction.WEST) == ItemPipeBlock.Face.PIPE, "the pipe joins the extractor's back");
        // No charge: nothing is pulled.
        for (int i = 0; i < 40; i++) {
            network.tick();
            PipeExtractorBlockEntity.serverTick(level, extractorPos, level.getBlockState(extractorPos), extractor);
        }
        helper.assertValueEqual(source.getItem(0).getCount(), 8, "an unpowered extractor pulls nothing");

        box.setStored(100_000);
        for (int i = 0; i < 60; i++) {
            network.tick();
            PipeExtractorBlockEntity.serverTick(level, extractorPos, level.getBlockState(extractorPos), extractor);
        }
        helper.assertTrue(source.getItem(0).getCount() < 8, "a powered extractor pulls items out");
        helper.runAfterDelay(60, () -> {
            tickPipes(level, pipes, 1);
            helper.assertTrue(count(target, Items.STICK) > 0, "the sticks reach the chest at the far end");

            // Nothing accepts sticks any more: the extractor leaves them alone.
            fill(target, Items.COBBLESTONE);
            int left = count(source, Items.STICK);
            for (int i = 0; i < 80; i++) {
                network.tick();
                PipeExtractorBlockEntity.serverTick(level, extractorPos, level.getBlockState(extractorPos), extractor);
            }
            helper.assertValueEqual(count(source, Items.STICK), left, "an item nothing accepts is not pulled");
            helper.assertValueEqual(extractor.status(), PipeExtractorBlockEntity.Status.NOTHING, "the status says there is nothing to move");
            helper.succeed();
        });
    }

    private static void controller(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos controllerPos = helper.absolutePos(new BlockPos(1, 1, 3));
        BlockPos pipeA = controllerPos.east(), pipeB = controllerPos.east(2), boxPos = controllerPos.above();
        level.setBlock(controllerPos, Tier5Logistics.STORAGE_CONTROLLER.get().defaultBlockState()
                .setValue(StorageControllerBlock.FACING, Direction.WEST), Block.UPDATE_ALL);
        level.setBlock(boxPos, Tier5Blocks.BATTERY_BOX.get().defaultBlockState(), Block.UPDATE_ALL);
        BlockPos[] chests = {pipeA.north(), pipeA.south(), pipeB.north(), pipeB.south()};
        for (BlockPos pos : chests) chest(level, pos);
        pipe(level, pipeA);
        pipe(level, pipeB);
        // Mark each chest face on the pipes as storage.
        for (BlockPos pos : new BlockPos[]{pipeA, pipeB}) {
            BlockState state = level.getBlockState(pos);
            for (Direction side : new Direction[]{Direction.NORTH, Direction.SOUTH}) {
                state = state.setValue(ItemPipeBlock.FACES.get(side), ItemPipeBlock.Face.STORAGE);
            }
            level.setBlock(pos, state, Block.UPDATE_ALL);
        }
        var ctrl = (StorageControllerBlockEntity) level.getBlockEntity(controllerPos);
        var box = (BatteryBoxBlockEntity) level.getBlockEntity(boxPos);
        var a = (ChestBlockEntity) level.getBlockEntity(chests[0]);
        var b = (ChestBlockEntity) level.getBlockEntity(chests[1]);
        var c = (ChestBlockEntity) level.getBlockEntity(chests[2]);
        var d = (ChestBlockEntity) level.getBlockEntity(chests[3]);
        a.setItem(0, new ItemStack(Items.STICK, 10));
        b.setItem(0, new ItemStack(Items.COAL, 5));
        c.setItem(0, new ItemStack(Items.COAL, 7));
        d.setItem(0, new ItemStack(Items.IRON_NUGGET, 3));

        ElectricNetwork network = ElectricNetworks.rebuildNow(level, controllerPos);
        box.setStored(100_000);
        for (int i = 0; i < 10; i++) {
            network.tick();
            StorageControllerBlockEntity.serverTick(level, controllerPos, level.getBlockState(controllerPos), ctrl);
        }
        helper.assertTrue(ctrl.powered(), "a charged network powers the controller");
        helper.assertValueEqual(ctrl.inventories().size(), 4, "four chests are in the storage");
        long coal = ctrl.index().stream().filter(e -> e.stack().is(Items.COAL)).mapToLong(e -> e.count()).sum();
        helper.assertValueEqual(coal, 12L, "coal from two chests adds up in the index");
        helper.assertValueEqual(ctrl.index().size(), 3, "three kinds are listed");

        ItemStack taken = ctrl.take(new ItemStack(Items.COAL), 9);
        helper.assertValueEqual(taken.getCount(), 9, "nine coal come out");
        helper.assertValueEqual(count(b, Items.COAL) + count(c, Items.COAL), 3, "three coal are left");

        // New coal goes to a chest that already holds coal, not to the first chest with room.
        ItemStack left = ctrl.insert(new ItemStack(Items.COAL, 4));
        helper.assertTrue(left.isEmpty(), "everything fits");
        helper.assertValueEqual(count(a, Items.COAL), 0, "the stick chest got none");
        helper.assertValueEqual(count(b, Items.COAL) + count(c, Items.COAL), 7, "the coal stays with the coal");

        // Without power the index is read-only.
        box.setStored(0);
        for (int i = 0; i < 10; i++) {
            network.tick();
            StorageControllerBlockEntity.serverTick(level, controllerPos, level.getBlockState(controllerPos), ctrl);
        }
        helper.assertTrue(!ctrl.powered(), "an empty network leaves it dark");
        helper.assertTrue(ctrl.take(new ItemStack(Items.STICK), 1).isEmpty(), "nothing can be taken");
        helper.assertValueEqual(ctrl.room(new ItemStack(Items.STICK)), 0, "and nothing stored");
        helper.assertValueEqual(count(a, Items.STICK), 10, "the chest is untouched");
        helper.succeed();
    }

    private static void fluidFilter(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos base = helper.absolutePos(new BlockPos(1, 1, 2));
        BlockPos filterPos = base.east();
        level.setBlock(base, Tier4Blocks.BRONZE_FLUID_PIPE.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(filterPos, Tier5Logistics.FLUID_FILTER.get().defaultBlockState().setValue(FluidFilterBlock.FACING, Direction.EAST), Block.UPDATE_ALL);
        level.setBlock(filterPos.east(), Tier4Blocks.BRONZE_FLUID_PIPE.get().defaultBlockState(), Block.UPDATE_ALL);
        var filter = (FluidFilterBlockEntity) level.getBlockEntity(filterPos);
        filter.setFluid(Fluids.WATER);
        helper.assertTrue(filter.isSet(), "the filter holds a fluid");
        helper.assertTrue(filter.connectsFluid(Direction.EAST) && filter.connectsFluid(Direction.WEST), "it joins the pipe on both ends");
        helper.assertTrue(!filter.connectsFluid(Direction.UP), "and not on the sides");
        helper.assertValueEqual(filter.fill(Direction.WEST, dev.strataindustria.registry.Tier4Fluids.SULFUR_DIOXIDE.get(), 100, 1.0f, true), 0, "other fluids are refused");
        helper.succeed();
    }
}
