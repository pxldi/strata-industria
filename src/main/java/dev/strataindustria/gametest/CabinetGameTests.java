package dev.strataindustria.gametest;

import com.mojang.authlib.GameProfile;
import dev.strataindustria.cabinet.CabinetRegistry;
import dev.strataindustria.cabinet.Shelves;
import dev.strataindustria.cabinet.SpecimenCabinetBlock;
import dev.strataindustria.cabinet.SpecimenCabinetBlockEntity;
import dev.strataindustria.cabinet.Specimens;
import dev.strataindustria.geology.OreGrade;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.geology.Rock;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.structure.MineralSpecimenItem;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

/** Specimen cabinet tests (uniqueness 9.5), run by {@link ModGameTests}. */
final class CabinetGameTests {
    private CabinetGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("cabinet_specimens", CabinetGameTests::specimens);
        tests.put("cabinet_shelves_and_pick", CabinetGameTests::shelvesAndPick);
    }

    // Loose rocks, cut specimens and raw ore pieces count; nothing else does.
    private static void specimens(GameTestHelper helper) {
        helper.assertValueEqual(Specimens.idOf(new ItemStack(ModItems.LOOSE_ROCK.get(Rock.BASALT).get())), "rock:basalt", "a loose rock");
        helper.assertValueEqual(Specimens.idOf(MineralSpecimenItem.of(OreMineral.CASSITERITE)), "mineral:cassiterite", "a cut specimen");
        helper.assertValueEqual(Specimens.idOf(new ItemStack(ModItems.ORE_PIECES.get(OreMineral.CASSITERITE).get(OreGrade.POOR).get())),
                "mineral:cassiterite", "a raw ore piece");
        helper.assertTrue(Specimens.idOf(new ItemStack(Items.STICK)) == null, "a stick is no specimen");
        helper.assertValueEqual(Specimens.shelves().size(), 5, "four rock shelves and the minerals");
        helper.assertValueEqual(Specimens.required("sedimentary").size(), 2, "limestone and shale");
        helper.succeed();
    }

    // Setting both sedimentary rocks out fills that shelf for the player, once; the front fills in; a duplicate is kept; the pick knows the rock.
    private static void shelvesAndPick(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FakePlayer collector = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "collector"));
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        level.setBlock(pos, CabinetRegistry.SPECIMEN_CABINET.get().defaultBlockState(), Block.UPDATE_ALL);
        SpecimenCabinetBlockEntity cabinet = (SpecimenCabinetBlockEntity) level.getBlockEntity(pos);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false);

        ItemStack limestone = new ItemStack(ModItems.LOOSE_ROCK.get(Rock.LIMESTONE).get(), 2);
        collector.setItemInHand(InteractionHand.MAIN_HAND, limestone);
        BlockState state = level.getBlockState(pos);
        state.useItemOn(limestone, level, collector, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(cabinet.holds("rock:limestone"), "the limestone is on the shelf");
        helper.assertValueEqual(limestone.getCount(), 1, "one rock was used");
        state.useItemOn(limestone, level, collector, InteractionHand.MAIN_HAND, hit);
        helper.assertValueEqual(limestone.getCount(), 1, "a second limestone stays in the hand");
        helper.assertTrue(!Shelves.has(collector, "sedimentary"), "one rock does not fill the shelf");
        helper.assertTrue(!Shelves.knowsRock(collector, ModBlocks.RAW_ROCK.get(Rock.LIMESTONE).get().defaultBlockState()), "the pick does not know limestone yet");

        ItemStack shale = new ItemStack(ModItems.LOOSE_ROCK.get(Rock.SHALE).get());
        level.getBlockState(pos).useItemOn(shale, level, collector, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(Shelves.has(collector, "sedimentary"), "both sedimentary rocks fill the shelf");
        helper.assertTrue(Shelves.knowsRock(collector, ModBlocks.RAW_ROCK.get(Rock.SHALE).get().defaultBlockState()), "the pick knows shale");
        helper.assertTrue(!Shelves.knowsRock(collector, ModBlocks.RAW_ROCK.get(Rock.BASALT).get().defaultBlockState()), "but not basalt");
        helper.assertTrue(level.getBlockState(pos).getValue(SpecimenCabinetBlock.FILL) > 0, "the front shows a start");
        helper.assertTrue(cabinet.collectComponents().get(CabinetRegistry.HELD.get()).size() == 2, "the cabinet keeps its collection on the item");
        helper.succeed();
    }
}
