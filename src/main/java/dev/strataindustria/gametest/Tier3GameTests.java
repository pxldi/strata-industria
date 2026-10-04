package dev.strataindustria.gametest;

import com.mojang.authlib.GameProfile;
import dev.strataindustria.bloomery.BloomeryBlock;
import dev.strataindustria.bloomery.BloomeryBlockEntity;
import dev.strataindustria.geology.OreGrade;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.geology.Rock;
import dev.strataindustria.heat.Heat;
import dev.strataindustria.machine.TripHammerBlock;
import dev.strataindustria.machine.TripHammerBlockEntity;
import dev.strataindustria.material.Metal;
import dev.strataindustria.metal.Melt;
import dev.strataindustria.metal.MetalContent;
import dev.strataindustria.power.HandCrankBlock;
import dev.strataindustria.power.HandCrankBlockEntity;
import dev.strataindustria.power.KineticNetworks;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModDataComponents;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.smithing.AnvilRecipe;
import dev.strataindustria.smithing.Smithing;
import dev.strataindustria.smithing.SmithingPattern;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayer;

/** Tier 3 (iron) tests, run by {@link ModGameTests}. */
final class Tier3GameTests {
    private Tier3GameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("bloomery_run", Tier3GameTests::bloomeryRun);
        tests.put("trip_hammer", Tier3GameTests::tripHammer);
        tests.put("trip_hammer_bloom", Tier3GameTests::tripHammerBloom);
        tests.put("step_up_gearbox_facings", Tier3GameTests::stepUpGearboxFacings);
        tests.put("overspeed_segment", Tier3GameTests::overspeedSegment);
    }

    // Bloomery (tier 3 spec 5): a two-level chimney with no bellows burns at 1200 °C, which makes
    // blooms at the low yield; the slag comes out with the last bloom.

    private static void bloomeryRun(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos controller = helper.absolutePos(new BlockPos(4, 1, 3));
        BlockPos chamber = controller.south();
        BlockState bricks = ModBlocks.FIRE_BRICKS.get().defaultBlockState();
        level.setBlock(chamber.below(), bricks, Block.UPDATE_ALL);
        for (int y = 0; y <= 2; y++) {
            for (Direction side : Direction.Plane.HORIZONTAL) {
                if (y == 0 && side == Direction.NORTH) continue;
                level.setBlock(chamber.above(y).relative(side), bricks, Block.UPDATE_ALL);
            }
        }
        level.setBlock(controller, ModBlocks.BLOOMERY.get().defaultBlockState().setValue(BloomeryBlock.FACING, Direction.NORTH),
                Block.UPDATE_ALL);
        BloomeryBlockEntity bloomery = (BloomeryBlockEntity) level.getBlockEntity(controller);
        var structure = bloomery.checkStructure();
        helper.assertTrue(structure.complete(), "the bloomery should be complete, problem " + structure.problem() + " at "
                + structure.at());
        helper.assertValueEqual(structure.chimney(), 2, "chimney levels");

        ItemStack ore = new ItemStack(ModItems.crushedOre(OreMineral.HEMATITE, OreGrade.NORMAL), 16);
        int unitsEach = MetalContent.of(ore.copyWithCount(1)).orElseThrow().units().getOrDefault(Metal.WROUGHT_IRON, 0);
        helper.assertTrue(unitsEach > 0, "crushed hematite should hold iron");
        helper.assertValueEqual(bloomery.insert(ore.copy(), null), 16, "ore taken");
        helper.assertTrue(!bloomery.canLight(), "it should not light without charcoal");
        helper.assertValueEqual(bloomery.charcoalNeeded(), 8, "charcoal needed for 16 ore");
        helper.assertValueEqual(bloomery.insert(new ItemStack(Items.CHARCOAL, 8), null), 8, "charcoal taken");
        int expected = bloomery.expectedBlooms();

        BlockState state = level.getBlockState(controller);
        helper.assertTrue(((BloomeryBlock) state.getBlock()).ignite(level, controller, state), "the bloomery should light");
        for (int tick = 0; tick < 40000 && !bloomery.hasBlooms(); tick++) {
            BloomeryBlockEntity.serverTick(level, controller, level.getBlockState(controller), bloomery);
        }
        helper.assertTrue(bloomery.hasBlooms(), "the run should finish, status " + bloomery.status() + " at "
                + bloomery.temperature() + " °C");
        helper.assertTrue(!level.getBlockState(controller).getValue(BloomeryBlock.LIT), "the fire goes out when the run ends");

        int total = (int) Math.floor(unitsEach * 16 * BloomeryBlockEntity.lowYield());
        int units = 0;
        int blooms = 0;
        while (bloomery.hasBlooms()) {
            ItemStack bloom = bloomery.extract();
            helper.assertTrue(bloom.is(ModItems.RAW_BLOOM.get()), "a raw bloom should come out, got " + bloom);
            Melt contents = bloom.get(ModDataComponents.BLOOM_CONTENTS.get());
            helper.assertTrue(contents != null, "the bloom should carry its iron");
            units += contents.total();
            blooms++;
        }
        helper.assertValueEqual(blooms, expected, "blooms against the screen's estimate");
        int dropped = total % BloomeryBlockEntity.BLOOM_UNITS < BloomeryBlockEntity.MIN_PARTIAL_UNITS
                ? total % BloomeryBlockEntity.BLOOM_UNITS : 0;
        helper.assertValueEqual(units, total - dropped, "iron in the blooms at the low yield");
        helper.succeed();
    }

    // Trip hammer (tier 3 spec 8.4): turned by two hand cranks (one alone is 64 SU, the hammer needs
    // 128 at 16 RPM), it replays a recorded copper plate pattern
    // on a hot ingot from its own slot and drops the plate into the chest under the anvil.

    private static void tripHammer(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos anvilPos = helper.absolutePos(new BlockPos(4, 1, 3));
        BlockPos hammerPos = anvilPos.south();
        BlockPos crankPos = hammerPos.south();
        level.setBlock(anvilPos.below(), Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(anvilPos, ModBlocks.STONE_ANVILS.get(Rock.BASALT).get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(hammerPos, ModBlocks.TRIP_HAMMER.get().defaultBlockState().setValue(TripHammerBlock.FACING, Direction.NORTH),
                Block.UPDATE_ALL);
        level.setBlock(crankPos, ModBlocks.HAND_CRANK.get().defaultBlockState().setValue(HandCrankBlock.FACING, Direction.NORTH),
                Block.UPDATE_ALL);
        BlockPos topCrankPos = hammerPos.above();
        level.setBlock(topCrankPos, ModBlocks.HAND_CRANK.get().defaultBlockState().setValue(HandCrankBlock.FACING, Direction.DOWN),
                Block.UPDATE_ALL);
        TripHammerBlockEntity hammer = (TripHammerBlockEntity) level.getBlockEntity(hammerPos);

        ItemStack ingot = new ItemStack(Items.COPPER_INGOT);
        Heat.set(ingot, 1000.0f, level.getGameTime());
        RecipeHolder<AnvilRecipe> plate = level.recipeAccess().recipeMap()
                .getRecipesFor(dev.strataindustria.registry.ModRecipes.ANVIL.get(), new SingleRecipeInput(ingot), level)
                .filter(r -> r.value().result().create().is(ModItems.PLATES.get(Metal.COPPER).get()))
                .findFirst().orElseThrow(() -> helper.assertionException("no copper plate recipe"));
        int total = dev.strataindustria.smithing.AnvilBlockEntity.blowsFor(plate.value(), ingot);
        ItemStack pattern = new ItemStack(ModItems.SMITHING_PATTERN.get());
        pattern.set(ModDataComponents.SMITHING_PATTERN.get(), new dev.strataindustria.smithing.SmithingPattern(plate.id(), BuiltInRegistries.ITEM.getKey(ModItems.PLATES.get(Metal.COPPER).get())));
        hammer.setItem(TripHammerBlockEntity.PATTERN, pattern);
        hammer.setItem(TripHammerBlockEntity.INPUT, ingot.copyWithCount(plate.value().count()));

        TripHammerBlockEntity.serverTick(level, hammerPos, level.getBlockState(hammerPos), hammer);
        helper.assertValueEqual(hammer.status(), TripHammerBlockEntity.Status.NOT_TURNING, "status before cranking");

        FakePlayer smith = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "smith"));
        ((HandCrankBlockEntity) level.getBlockEntity(crankPos)).crank(smith);
        ((HandCrankBlockEntity) level.getBlockEntity(topCrankPos)).crank(smith);
        KineticNetworks.rebuildNow(level, hammerPos);
        for (int tick = 0; tick < (total + 2) * 12; tick++) {
            TripHammerBlockEntity.serverTick(level, hammerPos, level.getBlockState(hammerPos), hammer);
        }
        Container chest = (Container) level.getBlockEntity(anvilPos.below());
        ItemStack out = ItemStack.EMPTY;
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            if (!chest.getItem(slot).isEmpty()) out = chest.getItem(slot);
        }
        helper.assertTrue(out.is(ModItems.PLATES.get(Metal.COPPER).get()), "the chest should hold a copper plate, got " + out
                + ", hammer status " + hammer.status());
        helper.assertTrue(hammer.getItem(TripHammerBlockEntity.INPUT).isEmpty(), "the ingot should be used up");
        helper.succeed();
    }

    /** A trip hammer with no pattern refines a hot raw bloom in one heat. */
    private static void tripHammerBloom(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos anvilPos = helper.absolutePos(new BlockPos(4, 1, 3));
        BlockPos hammerPos = anvilPos.south();
        BlockPos crankPos = hammerPos.south();
        level.setBlock(anvilPos.below(), Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(anvilPos, ModBlocks.WROUGHT_IRON_ANVIL.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(hammerPos, ModBlocks.TRIP_HAMMER.get().defaultBlockState().setValue(TripHammerBlock.FACING, Direction.NORTH),
                Block.UPDATE_ALL);
        level.setBlock(crankPos, ModBlocks.HAND_CRANK.get().defaultBlockState().setValue(HandCrankBlock.FACING, Direction.NORTH),
                Block.UPDATE_ALL);
        BlockPos topCrankPos = hammerPos.above();
        level.setBlock(topCrankPos, ModBlocks.HAND_CRANK.get().defaultBlockState().setValue(HandCrankBlock.FACING, Direction.DOWN),
                Block.UPDATE_ALL);
        TripHammerBlockEntity hammer = (TripHammerBlockEntity) level.getBlockEntity(hammerPos);

        ItemStack bloom = new ItemStack(ModItems.RAW_BLOOM.get());
        bloom.set(ModDataComponents.BLOOM_CONTENTS.get(), Melt.of(Metal.WROUGHT_IRON, BloomeryBlockEntity.BLOOM_UNITS, 0));
        Heat.set(bloom, 1200.0f, level.getGameTime());
        hammer.setItem(TripHammerBlockEntity.INPUT, bloom);

        FakePlayer smith = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "smith"));
        ((HandCrankBlockEntity) level.getBlockEntity(crankPos)).crank(smith);
        ((HandCrankBlockEntity) level.getBlockEntity(topCrankPos)).crank(smith);
        KineticNetworks.rebuildNow(level, hammerPos);
        for (int tick = 0; tick < 1500; tick++) {
            TripHammerBlockEntity.serverTick(level, hammerPos, level.getBlockState(hammerPos), hammer);
        }
        Container chest = (Container) level.getBlockEntity(anvilPos.below());
        ItemStack out = ItemStack.EMPTY;
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            if (!chest.getItem(slot).isEmpty()) out = chest.getItem(slot);
        }
        helper.assertTrue(out.is(Items.IRON_INGOT), "the chest should hold an iron ingot, got " + out + ", hammer status " + hammer.status());
        helper.assertTrue(hammer.getItem(TripHammerBlockEntity.INPUT).isEmpty(), "the bloom should be used up");
        helper.succeed();
    }

    // Step-up gearbox (tier 3 spec 7.3): the axle behind it turns at twice the crank's speed whichever
    // way it faces and whichever block the network is measured from.

    private static void stepUpGearboxFacings(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos centre = helper.absolutePos(new BlockPos(4, 1, 4));
        FakePlayer miller = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "miller"));
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            BlockPos crankPos = centre.relative(facing.getOpposite());
            BlockPos axlePos = centre.relative(facing);
            for (BlockPos start : List.of(crankPos, centre, axlePos)) {
                level.setBlock(crankPos, ModBlocks.HAND_CRANK.get().defaultBlockState().setValue(HandCrankBlock.FACING, facing),
                        Block.UPDATE_ALL);
                level.setBlock(centre, ModBlocks.STEP_UP_GEARBOX.get().defaultBlockState()
                        .setValue(dev.strataindustria.power.StepUpGearboxBlock.FACING, facing), Block.UPDATE_ALL);
                level.setBlock(axlePos, ModBlocks.WOODEN_AXLE.get().defaultBlockState()
                        .setValue(dev.strataindustria.power.AxleBlock.AXIS, facing.getAxis()), Block.UPDATE_ALL);
                ((HandCrankBlockEntity) level.getBlockEntity(crankPos)).crank(miller);
                KineticNetworks.rebuildNow(level, start);
                var axle = (dev.strataindustria.power.Kinetic) level.getBlockEntity(axlePos);
                var crank = (dev.strataindustria.power.Kinetic) level.getBlockEntity(crankPos);
                String where = "facing " + facing + ", measured from " + start.subtract(centre);
                helper.assertValueEqual(Math.round(crank.kinetic().rpm()), 16, "crank RPM, " + where);
                helper.assertValueEqual(Math.round(axle.kinetic().rpm()), 32, "axle RPM, " + where);
                for (BlockPos pos : List.of(crankPos, centre, axlePos)) level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        helper.succeed();
    }

    // Overspeed (tier 3 spec 7.1): three step-up gearboxes take a crank's 16 RPM to 128. Only the axle
    // past the 64 RPM limit stops; the slower part of the network keeps turning.

    private static void overspeedSegment(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos crankPos = helper.absolutePos(new BlockPos(1, 1, 4));
        level.setBlock(crankPos, ModBlocks.HAND_CRANK.get().defaultBlockState().setValue(HandCrankBlock.FACING, Direction.EAST),
                Block.UPDATE_ALL);
        List<BlockPos> axles = new java.util.ArrayList<>();
        for (int i = 0; i < 3; i++) {
            BlockPos gearbox = crankPos.east(1 + 2 * i);
            level.setBlock(gearbox, ModBlocks.STEP_UP_GEARBOX.get().defaultBlockState()
                    .setValue(dev.strataindustria.power.StepUpGearboxBlock.FACING, Direction.EAST), Block.UPDATE_ALL);
            level.setBlock(gearbox.east(), ModBlocks.WOODEN_AXLE.get().defaultBlockState()
                    .setValue(dev.strataindustria.power.AxleBlock.AXIS, Direction.Axis.X), Block.UPDATE_ALL);
            axles.add(gearbox.east());
        }
        ((HandCrankBlockEntity) level.getBlockEntity(crankPos)).crank(new FakePlayer(level, new GameProfile(UUID.randomUUID(), "miller")));
        KineticNetworks.rebuildNow(level, crankPos);
        int[] expected = {32, 64, 0};
        for (int i = 0; i < 3; i++) {
            var axle = ((dev.strataindustria.power.Kinetic) level.getBlockEntity(axles.get(i))).kinetic();
            helper.assertValueEqual(Math.round(axle.rpm()), expected[i], "RPM of axle " + (i + 1));
            helper.assertValueEqual(axle.status(), i < 2 ? dev.strataindustria.power.KineticState.Status.RUNNING
                    : dev.strataindustria.power.KineticState.Status.OVERSPEED, "status of axle " + (i + 1));
        }
        helper.succeed();
    }
}
