package dev.strataindustria.gametest;

import dev.strataindustria.electric.BatteryBoxBlockEntity;
import dev.strataindustria.electric.PoleInsulatorBlock;
import dev.strataindustria.electric.PoleInsulatorBlockEntity;
import dev.strataindustria.grid.ElectricLampBlock;
import dev.strataindustria.grid.ElectricLampBlockEntity;
import dev.strataindustria.grid.GridBlocks;
import dev.strataindustria.grid.GridVoices;
import dev.strataindustria.grid.LeydenJarBlock;
import dev.strataindustria.grid.LeydenJarBlockEntity;
import dev.strataindustria.grid.LightningHarvest;
import dev.strataindustria.grid.StethoscopeItem;
import dev.strataindustria.power.ElectricNetwork;
import dev.strataindustria.power.ElectricNetworks;
import dev.strataindustria.power.ElectricStatus;
import dev.strataindustria.power.ElectricTier;
import dev.strataindustria.registry.Tier5Blocks;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Tier 5 extras tests (uniqueness 2.1, 7.1, 7.3), run by {@link ModGameTests}. */
final class GridGameTests {
    private GridGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("grid_load_levels", GridGameTests::loadLevels);
        tests.put("grid_lamps_dim", GridGameTests::lampsDim);
        tests.put("grid_line_follows_load", GridGameTests::lineFollowsLoad);
        tests.put("grid_hum", GridGameTests::hum);
        tests.put("grid_stethoscope", GridGameTests::stethoscope);
        tests.put("grid_lightning_harvest", GridGameTests::lightningHarvest);
    }

    private static void lamps(ServerLevel level, BlockPos first, int count) {
        for (int i = 0; i < count; i++) {
            level.setBlock(first.offset(i % 7, 0, i / 7), GridBlocks.ELECTRIC_LAMP.get().defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    // A battery box gives 32 J/t. A handful of lamps is a calm grid, most of its output is a busy one, and more lamps
    // than it can feed leave it at its limit with every lamp short.
    private static void loadLevels(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos box = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlock(box, Tier5Blocks.BATTERY_BOX.get().defaultBlockState(), Block.UPDATE_ALL);
        ((BatteryBoxBlockEntity) level.getBlockEntity(box)).setStored(80_000);
        BlockPos first = box.east();
        lamps(level, first, 6);
        ElectricNetwork network = ElectricNetworks.rebuildNow(level, box);
        for (int i = 0; i < 80; i++) network.tick();
        helper.assertValueEqual(network.loadLevel(), 0, "six lamps are a calm grid");
        helper.assertTrue(!network.strained(), "six lamps do not strain it");

        lamps(level, first, 28);
        network = ElectricNetworks.rebuildNow(level, box);
        for (int i = 0; i < 80; i++) network.tick();
        helper.assertValueEqual(network.loadLevel(), 2, "28 lamps are a heavy grid");
        helper.assertTrue(!network.strained(), "28 lamps are still fed in full");

        lamps(level, first, 42);
        network = ElectricNetworks.rebuildNow(level, box);
        for (int i = 0; i < 80; i++) network.tick();
        helper.assertTrue(network.strained(), "42 lamps are more than 32 J/t");
        helper.assertValueEqual(network.loadLevel(), 3, "a grid short of power is at its limit");
        helper.assertTrue(network.fraction() < 0.9, "every lamp gets less than it asked for");
        helper.succeed();
    }

    // A lamp burns full on a fully supplied network, dimmer as the supply falls short, dips for a moment when demand jumps
    // on a busy network, and goes out without power. A fresh network does not dip.
    private static void lampsDim(GameTestHelper helper) {
        helper.assertValueEqual(ElectricLampBlockEntity.targetLevel(ElectricStatus.RUNNING, 1.0, false), 15, "full power");
        helper.assertValueEqual(ElectricLampBlockEntity.targetLevel(ElectricStatus.LOW_POWER, 0.5, false), 8, "half power");
        helper.assertValueEqual(ElectricLampBlockEntity.targetLevel(ElectricStatus.LOW_POWER, 0.05, false), ElectricLampBlockEntity.FLOOR, "a faint glow, not dark");
        helper.assertValueEqual(ElectricLampBlockEntity.targetLevel(ElectricStatus.RUNNING, 1.0, true), 15 - ElectricLampBlockEntity.DIP, "a dip");
        helper.assertValueEqual(ElectricLampBlockEntity.targetLevel(ElectricStatus.NO_SOURCE, 0.0, false), 0, "no source");
        helper.assertValueEqual(ElectricLampBlockEntity.targetLevel(ElectricStatus.OVERVOLTAGE, 1.0, false), 0, "overvoltage");

        ServerLevel level = helper.getLevel();
        BlockPos box = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlock(box, Tier5Blocks.BATTERY_BOX.get().defaultBlockState(), Block.UPDATE_ALL);
        ((BatteryBoxBlockEntity) level.getBlockEntity(box)).setStored(80_000);
        BlockPos lampPos = box.east();
        level.setBlock(lampPos, GridBlocks.ELECTRIC_LAMP.get().defaultBlockState(), Block.UPDATE_ALL);
        ElectricNetwork network = ElectricNetworks.rebuildNow(level, box);
        network.tick();
        helper.assertTrue(!network.dipping(), "a new network starts steady");
        network.noteDemand(2.0, 32.0);
        network.noteDemand(30.0, 32.0);
        helper.assertTrue(network.dipping(), "a jump of 28 J/t on a busy network dips the lamps");
        for (int i = 0; i < ElectricNetwork.DIP_TICKS + 1; i++) network.tick();
        helper.assertTrue(!network.dipping(), "the dip passes");
        network.noteDemand(2.0, 32.0);
        network.noteDemand(6.0, 32.0);
        helper.assertTrue(!network.dipping(), "a small step is not felt");
        network.noteDemand(10.0, 500.0);
        network.noteDemand(30.0, 500.0);
        helper.assertTrue(!network.dipping(), "a grid with plenty to spare does not dip");
        helper.assertValueEqual(level.getBlockState(lampPos).getValue(ElectricLampBlock.LEVEL), 0, "lamp starts dark");
        helper.succeed();
    }

    // An insulator is told its network's load level, and starts at none on a fresh network.
    private static void lineFollowsLoad(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos box = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlock(box, Tier5Blocks.BATTERY_BOX.get().defaultBlockState().setValue(dev.strataindustria.electric.BatteryBoxBlock.TIER, ElectricTier.MV), Block.UPDATE_ALL);
        BlockPos clamp = box.above();
        level.setBlock(clamp, Tier5Blocks.POLE_INSULATOR.get().defaultBlockState().setValue(PoleInsulatorBlock.FACING, Direction.UP), Block.UPDATE_ALL);
        PoleInsulatorBlockEntity insulator = (PoleInsulatorBlockEntity) level.getBlockEntity(clamp);
        insulator.gridLoad(3);
        helper.assertValueEqual(insulator.strain(), 3, "the insulator takes the level it is told");
        ElectricNetwork network = ElectricNetworks.rebuildNow(level, box);
        helper.assertTrue(network != null && network.members().contains(clamp), "the insulator is on the box's network");
        network.tick();
        helper.assertValueEqual(insulator.strain(), 0, "a quiet network tells it calm");
        helper.succeed();
    }

    // Insulators and one cable in ten are hum points, each in some tick of the 40 tick cycle; hum gets louder and higher with load.
    private static void hum(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos box = helper.absolutePos(new BlockPos(0, 1, 1));
        level.setBlock(box, Tier5Blocks.BATTERY_BOX.get().defaultBlockState(), Block.UPDATE_ALL);
        for (int i = 1; i <= 30; i++) level.setBlock(box.east(i), Tier5Blocks.LV_CABLE.get().defaultBlockState(), Block.UPDATE_ALL);
        ElectricNetwork network = ElectricNetworks.rebuildNow(level, box);
        Set<BlockPos> points = new HashSet<>();
        for (int t = 0; t < ElectricNetwork.HUM_PERIOD; t++) points.addAll(network.humPoints(t));
        helper.assertValueEqual(points.size(), 3, "three cables of thirty hum");
        helper.assertTrue(!points.contains(box), "a battery box does not hum");

        helper.assertTrue(GridVoices.volume(0.9) > GridVoices.volume(0.1), "louder under load");
        helper.assertTrue(GridVoices.pitch(0.9) > GridVoices.pitch(0.1), "higher under load");
        network.tick();
        helper.assertTrue(!GridVoices.audible(network), "an idle grid is silent");
        helper.succeed();
    }

    // The stethoscope reads electric blocks, hears a battery box as steady or dead, and has nothing to say about stone.
    @SuppressWarnings("removal")
    private static void stethoscope(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos box = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos stone = box.east(3);
        level.setBlock(box, Tier5Blocks.BATTERY_BOX.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(stone, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(box), Direction.UP, box, false);
        ElectricNetworks.rebuildNow(level, box).tick();
        StethoscopeItem.Reading reading = StethoscopeItem.read(level, box, hit);
        helper.assertTrue(reading != null, "a battery box can be heard");
        helper.assertValueEqual(reading.beat(), StethoscopeItem.Beat.SILENT, "an empty box is a dead click");
        ((BatteryBoxBlockEntity) level.getBlockEntity(box)).setStored(50_000);
        level.setBlock(box.west(), GridBlocks.ELECTRIC_LAMP.get().defaultBlockState(), Block.UPDATE_ALL);
        ElectricNetworks.rebuildNow(level, box).tick();
        helper.assertValueEqual(StethoscopeItem.read(level, box.west(), hit).beat(), StethoscopeItem.Beat.STEADY, "a lamp that is fed beats steadily");
        helper.assertTrue(StethoscopeItem.read(level, stone, new BlockHitResult(Vec3.atCenterOf(stone), Direction.UP, stone, false)) == null, "stone has nothing to say");

        ItemStack stethoscope = new ItemStack(GridBlocks.STETHOSCOPE.get());
        helper.assertTrue(StethoscopeItem.listen(level, box, player, stethoscope, hit) == InteractionResult.SUCCESS, "listening works");
        helper.assertTrue(player.getCooldowns().isOnCooldown(stethoscope), "it needs a moment before the next listen");
        helper.assertTrue(StethoscopeItem.listen(level, stone, player, stethoscope, new BlockHitResult(Vec3.atCenterOf(stone), Direction.UP, stone, false))
                == InteractionResult.PASS, "stone is left to the block");
        helper.succeed();
    }

    // A mast of three rods on a bank of jars: a bolt gives 8 000 J a rod, shared among the jars with room; a short mast or one
    // standing on stone only draws the bolt; a real bolt on the top rod does the same as calling the strike.
    private static void lightningHarvest(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos jarA = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos jarB = jarA.east();
        level.setBlock(jarA, GridBlocks.LEYDEN_JAR.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(jarB, GridBlocks.LEYDEN_JAR.get().defaultBlockState(), Block.UPDATE_ALL);
        for (int i = 1; i <= 3; i++) level.setBlock(jarA.above(i), Blocks.LIGHTNING_ROD.weathering().unaffected().defaultBlockState(), Block.UPDATE_ALL);
        LeydenJarBlockEntity a = (LeydenJarBlockEntity) level.getBlockEntity(jarA);
        LeydenJarBlockEntity b = (LeydenJarBlockEntity) level.getBlockEntity(jarB);
        ElectricNetworks.rebuildNow(level, jarA);
        BlockPos top = jarA.above(3);

        helper.assertValueEqual(LightningHarvest.mastHeight(level, top), 3, "mast height");
        double given = LightningHarvest.strike(level, top);
        helper.assertValueEqual(given, 24_000.0, "three rods bring 24 000 J");
        helper.assertValueEqual(a.stored(), 12_000.0, "jar A takes half");
        helper.assertValueEqual(b.stored(), 12_000.0, "jar B takes half");
        helper.assertValueEqual(a.request(), 0.0, "the network never charges a jar");
        helper.assertValueEqual(a.maxOutput(), 32.0, "a jar gives an LV battery's 32 J/t");
        helper.assertValueEqual(a.segments(), 2, "half full is two segments");

        a.setStored(LeydenJarBlockEntity.CAPACITY);
        b.setStored(0);
        helper.assertValueEqual(LightningHarvest.strike(level, top), 24_000.0, "a full jar leaves the rest to the other");
        helper.assertValueEqual(b.stored(), 24_000.0, "jar B takes the lot");
        helper.assertValueEqual(LightningHarvest.strike(level, top), 1_000.0, "only 1 000 J of room is left");

        double before = b.stored();
        level.setBlock(top, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        helper.assertValueEqual(LightningHarvest.strike(level, top.below()), 0.0, "two rods are too short");
        helper.assertValueEqual(b.stored(), before, "nothing reaches the jars");

        BlockPos stoneBase = helper.absolutePos(new BlockPos(6, 1, 6));
        level.setBlock(stoneBase, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        for (int i = 1; i <= 4; i++) level.setBlock(stoneBase.above(i), Blocks.LIGHTNING_ROD.weathering().unaffected().defaultBlockState(), Block.UPDATE_ALL);
        helper.assertValueEqual(LightningHarvest.strike(level, stoneBase.above(4)), 0.0, "a mast on stone brings nothing");

        // The real thing: a bolt arriving above the top rod, as vanilla places it.
        a.setStored(0);
        b.setStored(0);
        level.setBlock(top, Blocks.LIGHTNING_ROD.weathering().unaffected().defaultBlockState(), Block.UPDATE_ALL);
        LightningBolt bolt = new LightningBolt(net.minecraft.world.entity.EntityTypes.LIGHTNING_BOLT, level);
        bolt.setPos(Vec3.atBottomCenterOf(top.above()));
        bolt.setVisualOnly(true);
        level.addFreshEntity(bolt);
        helper.assertValueEqual(a.stored() + b.stored(), 24_000.0, "a bolt on the mast fills the jars");
        helper.assertTrue(List.of(0, 1, 2, 3, 4).contains(a.getBlockState().getValue(LeydenJarBlock.CHARGE)), "the charge shows on the jar");
        helper.succeed();
    }
}
