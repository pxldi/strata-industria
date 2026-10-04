package dev.strataindustria.gametest;

import com.mojang.authlib.GameProfile;
import dev.strataindustria.ceramics.BrickKilnBlock;
import dev.strataindustria.ceramics.BrickKilnBlockEntity;
import dev.strataindustria.forge.ForgeBlock;
import dev.strataindustria.forge.ForgeBlockEntity;
import dev.strataindustria.geology.OreGrade;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.heat.Heat;
import dev.strataindustria.material.Metal;
import dev.strataindustria.metal.CastingTableBlockEntity;
import dev.strataindustria.knapping.KnappingInput;
import dev.strataindustria.knapping.KnappingRecipe;
import dev.strataindustria.metal.CastMoldItem;
import dev.strataindustria.metal.CrucibleBlockEntity;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModDataComponents;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.ModRecipes;
import dev.strataindustria.registry.PatternRegistry;
import dev.strataindustria.registry.PrologueRegistry;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayer;

/** Brick kiln and casting table tests, run by {@link ModGameTests}. */
final class PrologueGameTests {
    private PrologueGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("brick_kiln", PrologueGameTests::brickKiln);
        tests.put("casting_table", PrologueGameTests::castingTable);
        tests.put("pattern_casting", PrologueGameTests::patternCasting);
    }

    private static ForgeBlockEntity lightForge(GameTestHelper helper, BlockPos forgePos) {
        ServerLevel level = helper.getLevel();
        level.setBlock(forgePos, ModBlocks.FORGE.get().defaultBlockState(), Block.UPDATE_ALL);
        ForgeBlockEntity forge = (ForgeBlockEntity) level.getBlockEntity(forgePos);
        forge.setItem(ForgeBlockEntity.FUEL_SLOT, new ItemStack(Items.CHARCOAL, 32));
        BlockState state = level.getBlockState(forgePos);
        helper.assertTrue(((ForgeBlock) state.getBlock()).ignite(level, forgePos, state), "the forge should light");
        return forge;
    }

    // The brick kiln fires a load on a hot forge, waits on a cold one, and takes only what a pit kiln takes.

    private static void brickKiln(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos forgePos = helper.absolutePos(new BlockPos(4, 1, 4));
        BlockPos kilnPos = forgePos.above();
        level.setBlock(kilnPos, PrologueRegistry.BRICK_KILN.get().defaultBlockState(), Block.UPDATE_ALL);
        BrickKilnBlockEntity kiln = (BrickKilnBlockEntity) level.getBlockEntity(kilnPos);

        helper.assertTrue(!kiln.canPlaceItem(0, new ItemStack(Items.IRON_INGOT)), "an ingot is not fireable");
        helper.assertTrue(kiln.canPlaceItem(0, new ItemStack(ModItems.UNFIRED_BRICK.get())), "an unfired brick is fireable");

        kiln.setItem(0, new ItemStack(ModItems.UNFIRED_BRICK.get(), 6));
        kiln.setItem(1, new ItemStack(ModItems.UNFIRED_INGOT_MOLD.get()));
        BrickKilnBlockEntity.serverTick(level, kilnPos, level.getBlockState(kilnPos), kiln);
        helper.assertValueEqual(kiln.status(), BrickKilnBlockEntity.Status.NO_FORGE, "status without a forge");

        ForgeBlockEntity forge = lightForge(helper, forgePos);
        BrickKilnBlockEntity.serverTick(level, kilnPos, level.getBlockState(kilnPos), kiln);
        helper.assertValueEqual(kiln.status(), BrickKilnBlockEntity.Status.TOO_COLD, "status on a cold forge");

        for (int tick = 0; tick < 12000; tick++) {
            ForgeBlockEntity.serverTick(level, forgePos, level.getBlockState(forgePos), forge);
            BrickKilnBlockEntity.serverTick(level, kilnPos, level.getBlockState(kilnPos), kiln);
            if (kiln.getItem(BrickKilnBlockEntity.INPUTS).getCount() + kiln.getItem(BrickKilnBlockEntity.INPUTS + 1).getCount() >= 7) break;
        }
        int bricks = 0, molds = 0;
        for (int slot = BrickKilnBlockEntity.INPUTS; slot < BrickKilnBlockEntity.SLOTS; slot++) {
            ItemStack out = kiln.getItem(slot);
            if (out.is(Items.BRICK)) bricks += out.getCount();
            if (out.is(ModItems.INGOT_MOLD.get())) molds += out.getCount();
        }
        helper.assertValueEqual(bricks, 6, "fired bricks");
        helper.assertValueEqual(molds, 1, "fired ingot molds");
        helper.assertTrue(kiln.getItem(0).isEmpty() && kiln.getItem(1).isEmpty(), "the inputs should be used up");
        helper.assertValueEqual(forge.temperature() >= BrickKilnBlockEntity.MIN_TEMPERATURE, true, "forge temperature " + forge.temperature());
        helper.succeed();
    }

    // A crucible beside a casting table pours into every empty mold on it; the table then knocks the castings out.

    private static void castingTable(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos forgePos = helper.absolutePos(new BlockPos(4, 1, 4));
        BlockPos cruciblePos = forgePos.above();
        BlockPos tablePos = cruciblePos.east();
        level.setBlock(cruciblePos, ModBlocks.CRUCIBLE.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(tablePos, PrologueRegistry.CASTING_TABLE.get().defaultBlockState(), Block.UPDATE_ALL);
        CrucibleBlockEntity crucible = (CrucibleBlockEntity) level.getBlockEntity(cruciblePos);
        CastingTableBlockEntity table = (CastingTableBlockEntity) level.getBlockEntity(tablePos);
        ForgeBlockEntity forge = lightForge(helper, forgePos);

        helper.assertValueEqual(crucible.pourProblem().orElse(""), "no_mold", "no mold and an empty table");
        helper.assertTrue(table.place(new ItemStack(ModItems.INGOT_MOLD.get())), "first mold");
        helper.assertTrue(table.place(new ItemStack(ModItems.INGOT_MOLD.get())), "second mold");
        helper.assertTrue(table.place(new ItemStack(ModItems.INGOT_MOLD.get())), "third mold");

        crucible.setItem(0, new ItemStack(ModItems.crushedOre(OreMineral.NATIVE_COPPER, OreGrade.NORMAL), 6));
        for (int tick = 0; tick < 12000; tick++) {
            ForgeBlockEntity.serverTick(level, forgePos, level.getBlockState(forgePos), forge);
            CrucibleBlockEntity.serverTick(level, cruciblePos, level.getBlockState(cruciblePos), crucible);
            if (crucible.getItem(0).isEmpty() && crucible.isMolten()) break;
        }
        helper.assertTrue(crucible.isMolten(), "the copper should be molten");
        helper.assertValueEqual(crucible.melt().total(), 210, "copper units in the melt");

        // One pour starts; the pot carries on into the next empty mold while the metal lasts.
        helper.assertTrue(crucible.startPour(), "the pour should start, problem: " + crucible.pourProblem().orElse(""));
        for (int tick = 0; tick < 120; tick++) {
            ForgeBlockEntity.serverTick(level, forgePos, level.getBlockState(forgePos), forge);
            CrucibleBlockEntity.serverTick(level, cruciblePos, level.getBlockState(cruciblePos), crucible);
        }
        int filled = 0;
        for (ItemStack mold : table.molds()) if (mold.has(ModDataComponents.CAST_CONTENTS.get())) filled++;
        helper.assertValueEqual(filled, 2, "molds filled from 210 units");
        helper.assertValueEqual(crucible.melt().total(), 10, "copper left in the pot");
        helper.assertTrue(table.hasEmptyMold(), "the third mold is still empty");
        helper.assertValueEqual(crucible.pourProblem().orElse(""), "not_enough", "the pot has too little for a third ingot");

        // Still molten: nothing comes out. Once the metal has set, one click takes both ingots.
        FakePlayer smith = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "smith"));
        helper.assertValueEqual(table.knockOut(level, smith), 0, "castings knocked out while molten");
        for (ItemStack mold : table.molds()) if (mold.has(ModDataComponents.CAST_CONTENTS.get())) Heat.set(mold, 20.0f, level.getGameTime());
        helper.assertValueEqual(table.knockOut(level, smith), 2, "castings knocked out once set");
        int ingots = 0;
        for (ItemStack held : smith.getInventory().getNonEquipmentItems()) if (held.is(ModItems.ingot(Metal.COPPER))) ingots += held.getCount();
        helper.assertValueEqual(ingots, 2, "copper ingots in the player's hands");
        helper.assertTrue(!table.molds().get(0).has(ModDataComponents.CAST_CONTENTS.get()), "the molds are free again");
        helper.succeed();
    }

    // Pattern casting: every shape can be carved from a blank, a pattern presses a sand flask into a mold and
    // is kept, and a sand mold sits on the table, takes steel and cracks after one casting.

    private static void patternCasting(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ItemStack blank = new ItemStack(PatternRegistry.PATTERN_BLANK.get());
        int carved = 0;
        for (RecipeHolder<KnappingRecipe> holder : level.recipeAccess().recipeMap().byType(ModRecipes.KNAPPING.get())) {
            KnappingRecipe recipe = holder.value();
            if (!recipe.ingredient().test(blank)) continue;
            List<RecipeHolder<KnappingRecipe>> found = level.recipeAccess().recipeMap()
                    .getRecipesFor(ModRecipes.KNAPPING.get(), new KnappingInput(blank, recipe.pattern()), level).toList();
            helper.assertValueEqual(found.size(), 1, "recipes for " + holder.id());
            helper.assertTrue(recipe.assemble(new KnappingInput(blank, recipe.pattern())).getItem() instanceof dev.strataindustria.casting.PatternItem,
                    "a carving gives a pattern");
            carved++;
        }
        helper.assertValueEqual(carved, PatternRegistry.SHAPES.size(), "carvable shapes");

        FakePlayer smith = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "smith"));
        ItemStack pattern = new ItemStack(PatternRegistry.PATTERNS.get("pickaxe_head").get());
        smith.setItemInHand(InteractionHand.MAIN_HAND, pattern);
        helper.assertValueEqual(pattern.getItem().use(level, smith, InteractionHand.MAIN_HAND), InteractionResult.FAIL, "pressing with no flask");
        smith.getInventory().add(new ItemStack(PatternRegistry.SAND_FLASK.get(), 2));
        helper.assertValueEqual(pattern.getItem().use(level, smith, InteractionHand.MAIN_HAND), InteractionResult.SUCCESS, "pressing with a flask");
        int flasks = 0, molds = 0;
        for (ItemStack held : smith.getInventory().getNonEquipmentItems()) {
            if (held.is(PatternRegistry.SAND_FLASK.get())) flasks += held.getCount();
            if (held.is(PatternRegistry.SAND_MOLDS.get("pickaxe_head").get())) molds += held.getCount();
        }
        helper.assertValueEqual(flasks, 1, "flasks left");
        helper.assertValueEqual(molds, 1, "sand molds made");
        helper.assertTrue(smith.getItemInHand(InteractionHand.MAIN_HAND).is(pattern.getItem()), "the pattern is kept");

        CastMoldItem sand = PatternRegistry.SAND_MOLDS.get("pickaxe_head").get();
        helper.assertTrue(sand.takes(Metal.STEEL.meltingPoint()), "sand takes steel");
        helper.assertValueEqual(sand.type(), dev.strataindustria.ceramics.MoldType.PICKAXE_HEAD, "the mold casts a pickaxe head");
        helper.assertTrue(CastMoldItem.breaks(new ItemStack(sand), level.getRandom()), "a sand mold cracks after one casting");
        helper.assertTrue(!CastMoldItem.breaks(new ItemStack(ModItems.INGOT_MOLD.get()), level.getRandom()), "a fired mold survives");
        BlockPos tablePos = helper.absolutePos(new BlockPos(4, 1, 4));
        level.setBlock(tablePos, PrologueRegistry.CASTING_TABLE.get().defaultBlockState(), Block.UPDATE_ALL);
        CastingTableBlockEntity table = (CastingTableBlockEntity) level.getBlockEntity(tablePos);
        helper.assertTrue(CastingTableBlockEntity.accepts(new ItemStack(sand)), "the table takes sand molds");
        helper.assertTrue(table.place(new ItemStack(sand)), "laid on the table");
        helper.assertTrue(table.hasEmptyMold(), "an unfilled sand mold is ready to pour into");
        helper.succeed();
    }
}
