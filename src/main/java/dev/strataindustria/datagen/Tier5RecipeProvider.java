package dev.strataindustria.datagen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.geology.OreGrade;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.material.Metal;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.Tier4Items;
import dev.strataindustria.registry.Tier5Items;
import dev.strataindustria.roasting.RoastingRecipe;
import dev.strataindustria.smithing.AnvilRecipe;
import dev.strataindustria.smithing.Rule;
import java.util.List;
import java.util.Optional;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;

/** Tier 5 (electric) recipes. {@link ModRecipeProvider} runs it, so tier 5 stays out of the shared provider. */
final class Tier5RecipeProvider extends RecipeProvider {
    Tier5RecipeProvider(BootstrapContext<Recipe<?>> recipeOutput, BootstrapContext<Advancement> advancementOutput) {
        super(recipeOutput, advancementOutput);
    }

    @Override
    protected void buildRecipes() {
        metals();
        rubber();
        electric();
    }

    // Spec 4.1, 9.1 and 9.4: rods and wire drawn on the anvil, plates hit flat, and the magnet.
    private void metals() {
        Item copper = Items.COPPER_INGOT;
        anvil("copper_rod", copper, Tier5Items.COPPER_ROD.get(), 2, 70,
                rule(Rule.Kind.DRAW, Rule.Where.LAST), rule(Rule.Kind.DRAW, Rule.Where.SECOND_LAST), rule(Rule.Kind.HIT, Rule.Where.NOT_LAST));
        // Hand-drawn wire wastes a quarter of the ingot: 3 wires of 25 units.
        anvil("copper_wire", copper, Tier5Items.COPPER_WIRE.get(), 3, 90,
                rule(Rule.Kind.HIT, Rule.Where.LAST), rule(Rule.Kind.DRAW, Rule.Where.SECOND_LAST), rule(Rule.Kind.DRAW, Rule.Where.THIRD_LAST));
        anvil("lead_plate", ModItems.ingot(Metal.LEAD), Tier5Items.LEAD_PLATE.get(), 1, 60,
                rule(Rule.Kind.HIT, Rule.Where.LAST), rule(Rule.Kind.HIT, Rule.Where.SECOND_LAST), rule(Rule.Kind.HIT, Rule.Where.THIRD_LAST));

        Item magnetite = ModItems.orePiece(OreMineral.MAGNETITE, OreGrade.RICH);
        shapeless(RecipeCategory.MISC, Tier5Items.MAGNET.get())
                .requires(ModItems.RODS.get(Metal.STEEL).get())
                .requires(magnetite)
                .unlockedBy("has_rich_magnetite", has(magnetite))
                .save(output, key("magnet"));
    }

    // Spec 5.3: raw rubber compounded with sulfur, then vulcanised at 140 °C.
    private void rubber() {
        Item raw = Tier5Items.RAW_RUBBER.get();
        shapeless(RecipeCategory.MISC, Tier5Items.COMPOUNDED_RUBBER.get(), 4)
                .requires(raw, 4)
                .requires(Tier4Items.SULFUR_DUST.get())
                .unlockedBy("has_raw_rubber", has(raw))
                .save(output, key("compounded_rubber"));
        output.accept(key("roasting/rubber"), new RoastingRecipe(Ingredient.of(Tier5Items.COMPOUNDED_RUBBER.get()),
                new ItemStackTemplate(Tier5Items.RUBBER.get()), 140, 200, Optional.empty()), null);
    }

    // Spec 7.1, 8.1 and 9.5: cables, the dynamo and the machine hull.
    private void electric() {
        Item rubber = Tier5Items.RUBBER.get();
        Item wire = Tier5Items.COPPER_WIRE.get();
        shaped(RecipeCategory.REDSTONE, Tier5Items.LV_CABLE.get(), 3)
                .pattern(" R ")
                .pattern("WWW")
                .pattern(" R ")
                .define('R', rubber)
                .define('W', wire)
                .unlockedBy("has_rubber", has(rubber))
                .save(output, key("lv_cable"));
        shaped(RecipeCategory.REDSTONE, Tier5Items.MV_CABLE.get(), 3)
                .pattern("RRR")
                .pattern("WWW")
                .pattern("RRR")
                .define('R', rubber)
                .define('W', wire)
                .unlockedBy("has_rubber", has(rubber))
                .save(output, key("mv_cable"));

        Item ironPlate = ModItems.PLATES.get(Metal.WROUGHT_IRON).get();
        Item cable = Tier5Items.LV_CABLE.get();
        Item magnet = Tier5Items.MAGNET.get();
        shaped(RecipeCategory.REDSTONE, Tier5Items.KINETIC_DYNAMO.get())
                .pattern("IXI")
                .pattern("WAW")
                .pattern("ILI")
                .define('I', ironPlate)
                .define('X', magnet)
                .define('W', wire)
                .define('A', Tier4Items.IRON_AXLE.get())
                .define('L', cable)
                .unlockedBy("has_magnet", has(magnet))
                .save(output, key("kinetic_dynamo"));
        shaped(RecipeCategory.REDSTONE, Tier5Items.LV_MACHINE_HULL.get())
                .pattern("PIP")
                .pattern("L L")
                .pattern("PIP")
                .define('P', ModItems.PLATES.get(Metal.STEEL).get())
                .define('I', ironPlate)
                .define('L', cable)
                .unlockedBy("has_lv_cable", has(cable))
                .save(output, key("lv_machine_hull"));
    }

    private static Rule rule(Rule.Kind kind, Rule.Where where) {
        return Rule.of(kind, where);
    }

    private void anvil(String path, Item input, Item result, int count, int defaultTarget, Rule... rules) {
        output.accept(key("anvil/" + path), new AnvilRecipe(Ingredient.of(input), 1, new ItemStackTemplate(result, count),
                List.of(rules), defaultTarget), null);
    }

    private static ResourceKey<Recipe<?>> key(String path) {
        return ResourceKey.create(Registries.RECIPE, StrataIndustria.id(path));
    }
}
