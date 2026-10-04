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
import dev.strataindustria.smithing.HitType;
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
        int target = Smithing.target(level, plate.id(), plate.value());
        List<HitType> hits = ModGameTests.solve(target, plate.value().rules());
        helper.assertTrue(!hits.isEmpty(), "the plate should be solvable");
        ItemStack pattern = new ItemStack(ModItems.SMITHING_PATTERN.get());
        pattern.set(ModDataComponents.SMITHING_PATTERN.get(), new SmithingPattern(plate.id(),
                BuiltInRegistries.ITEM.getKey(ModItems.PLATES.get(Metal.COPPER).get()), target,
                hits.stream().map(Enum::ordinal).toList(), 10));
        hammer.setItem(TripHammerBlockEntity.PATTERN, pattern);
        hammer.setItem(TripHammerBlockEntity.INPUT, ingot.copyWithCount(plate.value().count()));

        TripHammerBlockEntity.serverTick(level, hammerPos, level.getBlockState(hammerPos), hammer);
        helper.assertValueEqual(hammer.status(), TripHammerBlockEntity.Status.NOT_TURNING, "status before cranking");

        FakePlayer smith = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "smith"));
        ((HandCrankBlockEntity) level.getBlockEntity(crankPos)).crank(smith);
        ((HandCrankBlockEntity) level.getBlockEntity(topCrankPos)).crank(smith);
        KineticNetworks.rebuildNow(level, hammerPos);
        for (int tick = 0; tick < (hits.size() + 2) * 12; tick++) {
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
}
