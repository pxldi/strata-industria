package dev.strataindustria.gametest;

import dev.strataindustria.geology.OreGrade;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.material.Metal;
import dev.strataindustria.metal.Alloy;
import dev.strataindustria.metal.Melt;
import dev.strataindustria.metal.MetalContent;
import dev.strataindustria.registry.ModItems;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Tier 4 (steel and steam) tests, run by {@link ModGameTests}. */
final class Tier4GameTests {
    private Tier4GameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("tier4_alloy_rules", Tier4GameTests::alloyRules);
    }

    // Spec 4.3: the example batches for steel, pig iron, brass and solder, and the gap between steel and pig iron.
    private static void alloyRules(GameTestHelper helper) {
        Melt carbon = new Melt(Map.of(Metal.CARBON, 5), 0);
        Melt steel = melt(Items.IRON_INGOT, 5).plus(carbon);
        helper.assertValueEqual(Alloy.resultOf(steel).orElse(null), Metal.STEEL, "5 iron ingots + 5 carbon");

        Melt coFusion = melt(ModItems.ingot(Metal.PIG_IRON), 1).plus(melt(Items.IRON_INGOT, 3));
        helper.assertValueEqual(Alloy.resultOf(coFusion).orElse(null), Metal.STEEL, "1 pig iron + 3 iron ingots");

        helper.assertValueEqual(Alloy.resultOf(melt(ModItems.ingot(Metal.PIG_IRON), 2)).orElse(null), Metal.PIG_IRON, "pig iron remelt");
        helper.assertValueEqual(Alloy.resultOf(melt(ModItems.ingot(Metal.STEEL), 3)).orElse(null), Metal.STEEL, "steel remelt");
        helper.assertValueEqual(Alloy.resultOf(melt(Items.IRON_INGOT, 4)).orElse(null), Metal.WROUGHT_IRON, "iron remelt");

        Melt gap = new Melt(Map.of(Metal.WROUGHT_IRON, 974, Metal.CARBON, 26), 0);
        helper.assertTrue(Alloy.resultOf(gap).isEmpty(), "iron with 2.6% carbon should be no known alloy");

        Melt brass = melt(Items.COPPER_INGOT, 2).plus(melt(ModItems.ingot(Metal.ZINC), 1));
        helper.assertValueEqual(Alloy.resultOf(brass).orElse(null), Metal.BRASS, "2 copper + 1 zinc");
        Melt solder = melt(ModItems.NUGGETS.get(Metal.TIN).get(), 6).plus(melt(ModItems.NUGGETS.get(Metal.LEAD).get(), 4));
        helper.assertValueEqual(Alloy.resultOf(solder).orElse(null), Metal.SOLDER, "6 tin + 4 lead nuggets");

        // Spec 4.4: galena melts like the tier 2 ores; sphalerite has to be roasted first.
        helper.assertValueEqual(Alloy.resultOf(melt(ModItems.crushedOre(OreMineral.GALENA, OreGrade.NORMAL), 2)).orElse(null),
                Metal.LEAD, "crushed galena");
        helper.assertTrue(MetalContent.of(new ItemStack(ModItems.crushedOre(OreMineral.SPHALERITE, OreGrade.NORMAL))).isEmpty(),
                "sphalerite should not melt");
        helper.succeed();
    }

    private static Melt melt(net.minecraft.world.item.Item item, int count) {
        Melt one = MetalContent.of(new ItemStack(item)).orElseThrow();
        Melt total = Melt.EMPTY;
        for (int i = 0; i < count; i++) total = total.plus(one);
        return total;
    }
}
