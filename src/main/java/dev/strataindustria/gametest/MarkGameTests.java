package dev.strataindustria.gametest;

import com.mojang.authlib.GameProfile;
import dev.strataindustria.ceramics.MoldType;
import dev.strataindustria.heat.Heat;
import dev.strataindustria.mark.MakerMarks;
import dev.strataindustria.mark.MakerStamp;
import dev.strataindustria.mark.MakersMark;
import dev.strataindustria.mark.MarkEvents;
import dev.strataindustria.mark.MarkMenu;
import dev.strataindustria.mark.MarkRegistry;
import dev.strataindustria.geology.Rock;
import dev.strataindustria.material.Metal;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.ModRecipes;
import dev.strataindustria.smithing.AnvilBlockEntity;
import dev.strataindustria.smithing.AnvilRecipe;
import dev.strataindustria.smithing.Smithing;
import dev.strataindustria.smithing.HitType;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.util.FakePlayer;

/** Maker's mark tests (uniqueness 2.2), run by {@link ModGameTests}. */
final class MarkGameTests {
    private MarkGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("mark_editor", MarkGameTests::editor);
        tests.put("mark_smithed_piece", MarkGameTests::smithedPiece);
        tests.put("mark_castings_and_tally", MarkGameTests::castingsAndTally);
    }

    // The editor flips cells, clears, refuses a blank mark and keeps a real one on the player.
    private static void editor(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FakePlayer maker = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "maker"));
        MarkMenu menu = new MarkMenu(1, MakersMark.BLANK, null);
        helper.assertTrue(menu.clickMenuButton(maker, 0), "cell 0 flips");
        helper.assertTrue(menu.clickMenuButton(maker, 9), "cell 9 flips");
        helper.assertTrue(menu.clickMenuButton(maker, 63), "cell 63 flips");
        helper.assertTrue(menu.clickMenuButton(maker, 9), "cell 9 flips back");
        helper.assertValueEqual(menu.mark().bits(), 1L | 1L << 63, "cells set");
        helper.assertTrue(menu.isOn(63) && !menu.isOn(9), "cells read back");
        menu.clickMenuButton(maker, MarkMenu.CLEAR);
        helper.assertTrue(menu.mark().isBlank(), "clear empties the grid");
        helper.assertTrue(!menu.clickMenuButton(maker, MarkMenu.CONFIRM), "a blank mark is refused");
        helper.assertTrue(MakerMarks.of(maker).isBlank(), "nothing kept");
        menu.clickMenuButton(maker, 18);
        menu.clickMenuButton(maker, 27);
        helper.assertTrue(menu.clickMenuButton(maker, MarkMenu.CONFIRM), "a cut mark is kept");
        helper.assertValueEqual(MakerMarks.of(maker).bits(), 1L << 18 | 1L << 27, "the player's mark");
        helper.succeed();
    }

    // A piece finished on the anvil by a player with a mark carries that mark and their name.
    private static void smithedPiece(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(4, 1, 4));
        level.setBlock(pos, ModBlocks.STONE_ANVILS.get(Rock.BASALT).get().defaultBlockState(), Block.UPDATE_ALL);
        AnvilBlockEntity anvil = (AnvilBlockEntity) level.getBlockEntity(pos);
        FakePlayer smith = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "smith"));
        smith.getInventory().setItem(0, new ItemStack(ModItems.STONE_HAMMER.get()));
        MakerMarks.set(smith, new MakersMark(0x8142241818244281L));

        ItemStack ingot = new ItemStack(Items.COPPER_INGOT);
        Heat.set(ingot, 1000.0f, level.getGameTime());
        anvil.setItem(AnvilBlockEntity.INPUT, ingot);
        RecipeHolder<AnvilRecipe> plate = level.recipeAccess().recipeMap()
                .getRecipesFor(ModRecipes.ANVIL.get(), new SingleRecipeInput(ingot), level)
                .filter(r -> r.value().result().create().is(ModItems.PLATES.get(Metal.COPPER).get()))
                .findFirst().orElseThrow(() -> helper.assertionException("no copper plate recipe"));
        helper.assertTrue(anvil.select(plate.id()), "the anvil should offer a copper plate");
        for (HitType hit : ModGameTests.solve(Smithing.target(level, plate.id(), plate.value()), plate.value().rules())) anvil.hit(smith, hit);

        ItemStack out = anvil.getItem(AnvilBlockEntity.OUTPUT);
        helper.assertTrue(out.is(ModItems.PLATES.get(Metal.COPPER).get()), "the anvil should hold a copper plate, got " + out);
        MakerStamp stamp = out.get(MarkRegistry.STAMP.get());
        helper.assertTrue(stamp != null, "the plate should be stamped");
        helper.assertValueEqual(stamp.maker(), "smith", "maker's name");
        helper.assertValueEqual(stamp.mark().bits(), 0x8142241818244281L, "the smith's mark");

        // A stamp for a player who cut their mark only after finishing lands on the waiting piece.
        anvil.setItem(AnvilBlockEntity.OUTPUT, out.copy());
        anvil.getItem(AnvilBlockEntity.OUTPUT).remove(MarkRegistry.STAMP.get());
        anvil.stampOutput(smith);
        helper.assertTrue(anvil.getItem(AnvilBlockEntity.OUTPUT).has(MarkRegistry.STAMP.get()), "the waiting piece is stamped late");
        helper.succeed();
    }

    // Cast heads and gears carry the mark, plain ingots do not, and a stamped tool keeps a tally of blocks mined.
    private static void castingsAndTally(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FakePlayer maker = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "maker"));
        ItemStack head = new ItemStack(ModItems.head(Metal.COPPER, MoldType.PICKAXE_HEAD));
        ItemStack headMold = new ItemStack(ModItems.MOLDS.get(MoldType.PICKAXE_HEAD).get());
        MakerMarks.stampCasting(headMold, head, maker);
        helper.assertTrue(!head.has(MarkRegistry.STAMP.get()), "no mark cut yet, so no stamp");

        MakerMarks.set(maker, new MakersMark(0x3C42A5818DA5423CL));
        MakerMarks.stampCasting(headMold, head, maker);
        helper.assertTrue(head.has(MarkRegistry.STAMP.get()), "a cast head is stamped");
        ItemStack ingot = new ItemStack(ModItems.ingot(Metal.COPPER));
        MakerMarks.stampCasting(new ItemStack(ModItems.INGOT_MOLD.get()), ingot, maker);
        helper.assertTrue(!ingot.has(MarkRegistry.STAMP.get()), "a plain ingot is not stamped");

        ItemStack plain = new ItemStack(Items.IRON_PICKAXE);
        MakerMarks.countBlock(plain);
        helper.assertTrue(!plain.has(MarkRegistry.BLOCKS_MINED.get()), "an unstamped tool keeps no tally");
        ItemStack tool = new ItemStack(Items.IRON_PICKAXE);
        MakerMarks.stamp(tool, maker);
        for (int i = 0; i < 3; i++) MakerMarks.countBlock(tool);
        helper.assertValueEqual(tool.get(MarkRegistry.BLOCKS_MINED.get()), 3, "blocks mined");
        helper.assertValueEqual(MarkEvents.lines(tool, tool.get(MarkRegistry.STAMP.get())).size(), 2, "tooltip lines with a tally");
        helper.assertValueEqual(MarkEvents.lines(head, head.get(MarkRegistry.STAMP.get())).size(), 1, "tooltip lines without one");
        helper.succeed();
    }
}
