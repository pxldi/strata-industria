package dev.strataindustria.gametest;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.geology.Rock;
import dev.strataindustria.heat.Heat;
import dev.strataindustria.material.Metal;
import dev.strataindustria.metal.MetalContent;
import dev.strataindustria.metal.Quality;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.ModRecipes;
import dev.strataindustria.smithing.AnvilBlockEntity;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Block;

/** Two anvils (stone and iron) and the removal of ore-grade quality (redesign L10 and L11), run by {@link ModGameTests}. */
final class TwoAnvilGameTests {
    private TwoAnvilGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("two_anvils_gate_metals", TwoAnvilGameTests::anvilsGateMetals);
        tests.put("quality_is_craft_only", TwoAnvilGameTests::qualityIsCraftOnly);
    }

    private static AnvilBlockEntity anvil(ServerLevel level, BlockPos pos, Block block) {
        level.setBlock(pos, block.defaultBlockState(), Block.UPDATE_ALL);
        return (AnvilBlockEntity) level.getBlockEntity(pos);
    }

    /** Lays a hot ingot on the anvil, picks the plate shape and says what the anvil makes of it. */
    private static AnvilBlockEntity.Status statusFor(GameTestHelper helper, AnvilBlockEntity anvil, ServerLevel level, Item ingot, Metal metal) {
        ItemStack piece = new ItemStack(ingot);
        Heat.set(piece, 1500.0f, level.getGameTime());
        anvil.setItem(AnvilBlockEntity.INPUT, piece);
        var plate = level.recipeAccess().recipeMap().getRecipesFor(ModRecipes.ANVIL.get(), new SingleRecipeInput(piece), level)
                .filter(r -> r.value().result().create().is(ModItems.PLATES.get(metal).get()))
                .findFirst().orElseThrow(() -> helper.assertionException("no plate recipe for " + metal));
        helper.assertTrue(anvil.select(plate.id()), "the anvil should offer a plate of " + metal);
        return anvil.status(null);
    }

    // The stone anvil works copper and bronze and turns iron away; the iron anvil takes iron and steel.
    private static void anvilsGateMetals(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        AnvilBlockEntity stone = anvil(level, helper.absolutePos(new BlockPos(2, 1, 2)), ModBlocks.STONE_ANVILS.get(Rock.GRANITE).get());
        AnvilBlockEntity iron = anvil(level, helper.absolutePos(new BlockPos(5, 1, 2)), ModBlocks.IRON_ANVIL.get());
        helper.assertValueEqual(statusFor(helper, stone, level, ModItems.ingot(Metal.COPPER), Metal.COPPER), AnvilBlockEntity.Status.READY, "copper on stone");
        helper.assertValueEqual(statusFor(helper, stone, level, ModItems.ingot(Metal.BRONZE), Metal.BRONZE), AnvilBlockEntity.Status.READY, "bronze on stone");
        helper.assertValueEqual(statusFor(helper, stone, level, ModItems.ingot(Metal.WROUGHT_IRON), Metal.WROUGHT_IRON), AnvilBlockEntity.Status.TOO_WEAK, "iron on stone");
        helper.assertValueEqual(statusFor(helper, stone, level, ModItems.ingot(Metal.STEEL), Metal.STEEL), AnvilBlockEntity.Status.TOO_WEAK, "steel on stone");
        helper.assertValueEqual(statusFor(helper, iron, level, ModItems.ingot(Metal.WROUGHT_IRON), Metal.WROUGHT_IRON), AnvilBlockEntity.Status.READY, "iron on iron");
        helper.assertValueEqual(statusFor(helper, iron, level, ModItems.ingot(Metal.STEEL), Metal.STEEL), AnvilBlockEntity.Status.READY, "steel on iron");
        helper.assertValueEqual(statusFor(helper, iron, level, ModItems.ingot(Metal.COPPER), Metal.COPPER), AnvilBlockEntity.Status.READY, "copper on iron");
        helper.assertTrue(BuiltInRegistries.BLOCK.getOptional(StrataIndustria.id("bronze_anvil")).isPresent()
                || BuiltInRegistries.BLOCK.containsKey(StrataIndustria.id("iron_anvil")), "the iron anvil is registered");
        helper.succeed();
    }

    // The ore grade no longer leaves a mark on the metal: remelting gives every unit back whatever an item's quality, and
    // quality from an old save keeps only its craft part.
    private static void qualityIsCraftOnly(GameTestHelper helper) {
        ItemStack cast = new ItemStack(ModItems.ingot(Metal.BRONZE));
        cast.set(dev.strataindustria.registry.ModDataComponents.QUALITY.get(), new Quality(Quality.CAST));
        helper.assertValueEqual(MetalContent.of(cast).orElseThrow().total(), MetalContent.INGOT_UNITS, "a cast ingot remelts to a full ingot");
        helper.assertValueEqual(new Quality(4).total(), 4, "a bright strike is +4");
        var old = Quality.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"material\": 10, \"craft\": 2}"));
        helper.assertTrue(old.result().map(q -> q.craft() == 2 && q.total() == 2).orElse(false), "an old quality loses its material part, got " + old);
        helper.succeed();
    }
}
