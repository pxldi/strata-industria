package dev.strataindustria.gametest;

import com.mojang.authlib.GameProfile;
import dev.strataindustria.bronze.BronzeRegistry;
import dev.strataindustria.bronze.Fumes;
import dev.strataindustria.ceramics.MoldType;
import dev.strataindustria.forge.ForgeBlock;
import dev.strataindustria.forge.ForgeBlockEntity;
import dev.strataindustria.geology.OreGrade;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.geology.Rock;
import dev.strataindustria.knapping.Grain;
import dev.strataindustria.knapping.KnappedFrom;
import dev.strataindustria.knapping.KnappingMenu;
import dev.strataindustria.metal.CrucibleBlockEntity;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModItems;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayer;

/** Bronze age touches (uniqueness 3.2, 4.3), run by {@link ModGameTests}. */
final class BronzeGameTests {
    private BronzeGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("bronze_rock_grain", BronzeGameTests::rockGrain);
        tests.put("bronze_fume_venting", BronzeGameTests::fumeVenting);
        tests.put("bronze_arsenical_pour_fumes", BronzeGameTests::arsenicalPour);
    }

    /** How many of 120 fresh grids lose a second cell to one strike on the centre cell. */
    private static int crumbles(ServerLevel level, Rock rock) {
        int crumbled = 0;
        for (int i = 0; i < 120; i++) {
            FakePlayer knapper = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "knapper"));
            ItemStack rocks = new ItemStack(ModItems.LOOSE_ROCK.get(rock).get(), 16);
            knapper.setItemInHand(InteractionHand.MAIN_HAND, rocks);
            KnappingMenu menu = new KnappingMenu(7, knapper.getInventory(), rocks.copyWithCount(1), InteractionHand.MAIN_HAND);
            knapper.containerMenu = menu;
            menu.clickMenuButton(knapper, 12);
            if (Integer.bitCount(menu.keptMask()) < 24) crumbled++;
        }
        return crumbled;
    }

    // Crumbly stone sometimes loses a neighbouring cell, coarse and clean stone never does, and clean stone holds a better edge.
    private static void rockGrain(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.assertValueEqual(Rock.LIMESTONE.grain(), Grain.CRUMBLY, "limestone grain");
        helper.assertValueEqual(Rock.BASALT.grain(), Grain.COARSE, "basalt grain");
        helper.assertValueEqual(Rock.RHYOLITE.grain(), Grain.CLEAN, "rhyolite grain");
        helper.assertTrue(crumbles(level, Rock.LIMESTONE) > 0, "limestone should crumble now and then");
        helper.assertValueEqual(crumbles(level, Rock.BASALT), 0, "basalt never crumbles");
        helper.assertValueEqual(crumbles(level, Rock.RHYOLITE), 0, "rhyolite never crumbles");
        helper.assertTrue(KnappedFrom.of(Rock.RHYOLITE).durabilityMultiplier() > Rock.RHYOLITE.category().durabilityMultiplier(),
                "clean-grained stone should hold a better edge than its category alone");
        helper.assertValueEqual(KnappedFrom.of(Rock.BASALT).durabilityMultiplier(), Rock.BASALT.category().durabilityMultiplier(),
                "coarse stone gets no bonus");
        helper.succeed();
    }

    // Fumes go straight out under open sky or under a hood, and fill a roofed room without one.
    private static void fumeVenting(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pot = helper.absolutePos(new BlockPos(2, 1, 2));
        level.setBlock(pot, ModBlocks.CRUCIBLE.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(pot.above(3), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        helper.assertTrue(!Fumes.vented(level, pot), "a roofed pot with no hood is not vented");
        level.setBlock(pot.above(), BronzeRegistry.FUME_HOOD.get().defaultBlockState(), Block.UPDATE_ALL);
        helper.assertTrue(Fumes.vented(level, pot), "a hood directly over the pot vents it");
        helper.succeed();
    }

    // Pouring arsenical bronze fumes for the length of the pour and a little after; plain bronze does not.
    private static void arsenicalPour(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos forgePos = helper.absolutePos(new BlockPos(4, 1, 4));
        BlockPos cruciblePos = forgePos.above();
        level.setBlock(forgePos, ModBlocks.FORGE.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(cruciblePos, ModBlocks.CRUCIBLE.get().defaultBlockState(), Block.UPDATE_ALL);
        ForgeBlockEntity forge = (ForgeBlockEntity) level.getBlockEntity(forgePos);
        CrucibleBlockEntity crucible = (CrucibleBlockEntity) level.getBlockEntity(cruciblePos);
        forge.setItem(ForgeBlockEntity.FUEL_SLOT, new ItemStack(Items.CHARCOAL, 32));
        BlockState forgeState = level.getBlockState(forgePos);
        helper.assertTrue(((ForgeBlock) forgeState.getBlock()).ignite(level, forgePos, forgeState), "the forge should light");
        crucible.setItem(0, new ItemStack(ModItems.crushedOre(OreMineral.TENNANTITE, OreGrade.NORMAL), 5));
        crucible.setItem(1, new ItemStack(ModItems.crushedOre(OreMineral.NATIVE_COPPER, OreGrade.NORMAL), 4));
        for (int tick = 0; tick < 20000; tick++) {
            ForgeBlockEntity.serverTick(level, forgePos, level.getBlockState(forgePos), forge);
            CrucibleBlockEntity.serverTick(level, cruciblePos, level.getBlockState(cruciblePos), crucible);
            if (crucible.isMolten() && crucible.getItem(0).isEmpty() && crucible.getItem(1).isEmpty()) break;
        }
        helper.assertTrue(crucible.isMolten(), "the arsenical melt should be molten");
        helper.assertValueEqual(crucible.fumeTicks(), 0, "no fumes before the pour");
        crucible.setItem(CrucibleBlockEntity.MOLD_SLOT, new ItemStack(ModItems.MOLDS.get(MoldType.PICKAXE_HEAD).get()));
        helper.assertTrue(crucible.startPour(), "the pour should start, problem: " + crucible.pourProblem().orElse(""));
        helper.assertTrue(crucible.fumeTicks() > 0, "pouring arsenical bronze should fume");
        for (int tick = 0; tick < 200; tick++) {
            ForgeBlockEntity.serverTick(level, forgePos, level.getBlockState(forgePos), forge);
            CrucibleBlockEntity.serverTick(level, cruciblePos, level.getBlockState(cruciblePos), crucible);
        }
        helper.assertValueEqual(crucible.fumeTicks(), 0, "the fumes die away after the pour");
        helper.succeed();
    }
}
