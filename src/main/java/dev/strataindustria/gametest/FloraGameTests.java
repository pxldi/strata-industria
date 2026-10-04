package dev.strataindustria.gametest;

import dev.strataindustria.flora.FloraBlocks;
import dev.strataindustria.flora.IndicatorPlant;
import dev.strataindustria.flora.IndicatorPlants;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.journal.JournalContent;
import dev.strataindustria.journal.JournalState;
import dev.strataindustria.journal.Leads;
import dev.strataindustria.journal.PlantNotes;
import dev.strataindustria.journal.Study;
import dev.strataindustria.registry.ModItems;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Indicator plant tests, run by {@link ModGameTests}. */
final class FloraGameTests {
    private FloraGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("flora_survival", FloraGameTests::survival);
        tests.put("flora_placement", FloraGameTests::placement);
        tests.put("flora_colonies", FloraGameTests::colonies);
        tests.put("flora_journal", FloraGameTests::journal);
    }

    @SuppressWarnings("removal")
    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Leads.refresh(player);
        return player;
    }

    private static void done(GameTestHelper helper, ServerPlayer player) {
        helper.getLevel().getServer().getPlayerList().remove(player);
        helper.succeed();
    }

    private static BlockState plant(IndicatorPlant plant) {
        return FloraBlocks.PLANTS.get(plant).get().defaultBlockState();
    }

    // Every plant stands on soil and falls off bare stone; horsetail alone takes gravel.
    private static void survival(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        for (IndicatorPlant plant : IndicatorPlant.values()) {
            helper.setBlock(pos.below(), Blocks.GRASS_BLOCK);
            helper.assertTrue(plant(plant).canSurvive(helper.getLevel(), helper.absolutePos(pos)), plant + " stands on grass");
            helper.setBlock(pos.below(), Blocks.STONE);
            helper.assertTrue(!plant(plant).canSurvive(helper.getLevel(), helper.absolutePos(pos)), plant + " does not stand on stone");
            helper.setBlock(pos.below(), Blocks.GRAVEL);
            boolean onGravel = plant(plant).canSurvive(helper.getLevel(), helper.absolutePos(pos));
            helper.assertTrue(onGravel == (plant == IndicatorPlant.HORSETAIL), plant + " on gravel: " + onGravel);
        }
        helper.succeed();
    }

    // A plant goes onto free soil, never onto bare stone or into an occupied space.
    private static void placement(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos spot = new BlockPos(2, 2, 2);
        helper.setBlock(spot.below(), Blocks.GRASS_BLOCK);
        helper.setBlock(spot, Blocks.AIR);
        helper.assertTrue(IndicatorPlants.placeAt(level, helper.absolutePos(spot), IndicatorPlant.COPPER_FLOWER), "a plant went onto the grass");
        helper.assertTrue(level.getBlockState(helper.absolutePos(spot)).is(FloraBlocks.PLANTS.get(IndicatorPlant.COPPER_FLOWER).get()), "the plant stands there");
        helper.assertTrue(!IndicatorPlants.placeAt(level, helper.absolutePos(spot), IndicatorPlant.HORSETAIL), "an occupied space is left alone");
        helper.setBlock(spot, Blocks.AIR);
        helper.setBlock(spot.below(), Blocks.STONE);
        helper.assertTrue(!IndicatorPlants.placeAt(level, helper.absolutePos(spot), IndicatorPlant.HORSETAIL), "nothing grows on bare stone");
        helper.succeed();
    }

    // Colony centres depend only on the seed and the cell, and most cells have none.
    private static void colonies(GameTestHelper helper) {
        int found = 0;
        for (int x = -10; x < 10; x++) {
            for (int z = -10; z < 10; z++) {
                BlockPos a = IndicatorPlants.colony(1234L, x, z);
                BlockPos b = IndicatorPlants.colony(1234L, x, z);
                helper.assertTrue((a == null) == (b == null) && (a == null || a.equals(b)), "colony is a pure function of its cell");
                if (a == null) continue;
                found++;
                helper.assertTrue(Math.floorDiv(a.getX(), IndicatorPlants.COLONY_CELL) == x
                        && Math.floorDiv(a.getZ(), IndicatorPlants.COLONY_CELL) == z, "the colony lies inside its own cell");
            }
        }
        helper.assertTrue(found > 8 && found < 100, "colonies are rare but there are some, found " + found + " in 400 cells");
        helper.succeed();
    }

    // A plant is noted once when seen. The ore it marks, found afterwards, writes the link once. Studying the plant
    // hints its lead, and a plant that was never seen has no link.
    private static void journal(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        JournalState state = JournalContent.state(player);
        player.getInventory().add(new ItemStack(ModItems.SMALL_ORES.get(OreMineral.NATIVE_COPPER).get()));
        PlantNotes.scan(player);
        helper.assertTrue(state.notes().isEmpty(), "ore alone, with no plant seen, writes no link");

        ServerLevel level = helper.getLevel();
        BlockPos at = player.blockPosition();
        level.setBlock(at.east().below(), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
        level.setBlock(at.east(), plant(IndicatorPlant.COPPER_FLOWER), 3);
        PlantNotes.scan(player);
        helper.assertTrue(state.seen("plant/copper_flower"), "the plant was noticed");
        helper.assertTrue(state.notes().size() == 2, "one note for the plant, one for the link, found " + state.notes().size());
        helper.assertTrue(state.notes().stream().anyMatch(n -> n.key().endsWith("observe.plant.copper_flower")), "the plant note");
        helper.assertTrue(state.notes().stream().anyMatch(n -> n.key().endsWith("observe.link.copper_flower")), "the link note");
        PlantNotes.scan(player);
        helper.assertTrue(state.notes().size() == 2, "each fires once");

        helper.assertTrue(!state.lead("t1/nugget").hinted(), "no hint before studying");
        helper.assertTrue(Study.study(player, at.east()), "studying the plant did something");
        helper.assertTrue(state.lead("t1/nugget").hinted(), "studying the copper flower hints the nugget lead");
        done(helper, player);
    }
}
