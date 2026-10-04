package dev.strataindustria.datagen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.geology.OreGrade;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.material.Metal;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.Tier4Items;
import dev.strataindustria.registry.Tier5Fluids;
import dev.strataindustria.registry.Tier5Items;
import dev.strataindustria.tanning.BarrelRecipe;
import dev.strataindustria.tanning.FluidAmount;
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
import net.minecraft.world.level.material.Fluids;

/** Tier 5 (electric) recipes. {@link ModRecipeProvider} runs it, so tier 5 stays out of the shared provider. */
final class Tier5RecipeProvider extends RecipeProvider {
    Tier5RecipeProvider(BootstrapContext<Recipe<?>> recipeOutput, BootstrapContext<Advancement> advancementOutput) {
        super(recipeOutput, advancementOutput);
    }

    @Override
    protected void buildRecipes() {
        metals();
        rubber();
        components();
        electric();
        generators();
        shapingMachines();
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
        Item redAlloy = ModItems.ingot(Metal.RED_ALLOY);
        anvil("red_alloy_rod", redAlloy, Tier5Items.RED_ALLOY_ROD.get(), 2, 70,
                rule(Rule.Kind.DRAW, Rule.Where.LAST), rule(Rule.Kind.DRAW, Rule.Where.SECOND_LAST), rule(Rule.Kind.HIT, Rule.Where.NOT_LAST));
        anvil("red_alloy_wire", redAlloy, Tier5Items.RED_ALLOY_WIRE.get(), 3, 90,
                rule(Rule.Kind.HIT, Rule.Where.LAST), rule(Rule.Kind.DRAW, Rule.Where.SECOND_LAST), rule(Rule.Kind.DRAW, Rule.Where.THIRD_LAST));
        anvil("draw_plate", ModItems.PLATES.get(Metal.STEEL).get(), Tier5Items.DRAW_PLATE.get(), 1, 80,
                rule(Rule.Kind.PUNCH, Rule.Where.LAST), rule(Rule.Kind.PUNCH, Rule.Where.SECOND_LAST), rule(Rule.Kind.HIT, Rule.Where.NOT_LAST));

        Item magnetite = ModItems.orePiece(OreMineral.MAGNETITE, OreGrade.RICH);
        shapeless(RecipeCategory.MISC, Tier5Items.MAGNET.get())
                .requires(ModItems.RODS.get(Metal.STEEL).get())
                .requires(magnetite)
                .unlockedBy("has_rich_magnetite", has(magnetite))
                .save(output, key("magnet"));
    }

    // Spec 5.1 to 5.3: the tap, latex set in a barrel, raw rubber compounded with sulfur, then vulcanised at 140 °C.
    private void rubber() {
        Item bowl = Items.BOWL;
        shapeless(RecipeCategory.MISC, Tier5Items.TREE_TAP.get())
                .requires(ModItems.PLATES.get(Metal.COPPER).get())
                .requires(Items.STICK)
                .requires(bowl)
                .unlockedBy("has_copper_plate", has(ModItems.PLATES.get(Metal.COPPER).get()))
                .save(output, key("tree_tap"));
        output.accept(key("barrel/coagulating_latex"), new BarrelRecipe(Optional.empty(), 1,
                Optional.of(new FluidAmount(Tier5Fluids.LATEX.get(), 1000)),
                Optional.of(new ItemStackTemplate(Tier5Items.RAW_RUBBER.get(), 4)), Optional.empty(), 1200), null);
        // The slow fallback for a world without jungle: 32 dandelions and 4 buckets of water make one bucket of latex.
        output.accept(key("barrel/dandelion_latex"), new BarrelRecipe(Optional.of(Ingredient.of(Items.DANDELION)), 8,
                Optional.of(new FluidAmount(Fluids.WATER, 1000)), Optional.empty(),
                Optional.of(new FluidAmount(Tier5Fluids.LATEX.get(), 250)), 2400), null);

        Item raw = Tier5Items.RAW_RUBBER.get();
        shapeless(RecipeCategory.MISC, Tier5Items.COMPOUNDED_RUBBER.get(), 4)
                .requires(raw, 4)
                .requires(Tier4Items.SULFUR_DUST.get())
                .unlockedBy("has_raw_rubber", has(raw))
                .save(output, key("compounded_rubber"));
        output.accept(key("roasting/rubber"), new RoastingRecipe(Ingredient.of(Tier5Items.COMPOUNDED_RUBBER.get()),
                new ItemStackTemplate(Tier5Items.RUBBER.get()), 140, 200, Optional.empty()), null);
    }

    // Spec 4.5, 9.2 and 9.3: circuit boards soaked in creosote, basic circuits and motors.
    private void components() {
        Item creosote = Tier4Items.CREOSOTE_BUCKET.get();
        shapeless(RecipeCategory.MISC, Tier5Items.CIRCUIT_BOARD.get(), 3)
                .requires(Items.PAPER, 3)
                .requires(creosote)
                .unlockedBy("has_creosote_bucket", has(creosote))
                .save(output, key("circuit_board"));
        Item board = Tier5Items.CIRCUIT_BOARD.get();
        shaped(RecipeCategory.REDSTONE, Tier5Items.BASIC_CIRCUIT.get())
                .pattern(" D ")
                .pattern("XBX")
                .pattern(" D ")
                .define('D', Tier5Items.RED_ALLOY_WIRE.get())
                .define('X', Items.REDSTONE)
                .define('B', board)
                .unlockedBy("has_circuit_board", has(board))
                .save(output, key("basic_circuit"));
        Item magnet = Tier5Items.MAGNET.get();
        shaped(RecipeCategory.REDSTONE, Tier5Items.ELECTRIC_MOTOR.get())
                .pattern("PWP")
                .pattern("WRW")
                .pattern("PXP")
                .define('P', ModItems.PLATES.get(Metal.STEEL).get())
                .define('W', Tier5Items.COPPER_WIRE.get())
                .define('R', ModItems.RODS.get(Metal.STEEL).get())
                .define('X', magnet)
                .unlockedBy("has_magnet", has(magnet))
                .save(output, key("electric_motor"));
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

        // Spec 10.2 and 10.3: H hull, C basic circuit, W copper wire, M electric motor.
        Item hull = Tier5Items.LV_MACHINE_HULL.get();
        Item circuit = Tier5Items.BASIC_CIRCUIT.get();
        shaped(RecipeCategory.REDSTONE, Tier5Items.ELECTRIC_FURNACE.get())
                .pattern(" B ")
                .pattern("CHW")
                .pattern(" B ")
                .define('B', ModItems.FIRE_BRICKS.get())
                .define('C', circuit)
                .define('H', hull)
                .define('W', wire)
                .unlockedBy("has_lv_machine_hull", has(hull))
                .save(output, key("electric_furnace"));
        shaped(RecipeCategory.REDSTONE, Tier5Items.MACERATOR.get())
                .pattern(" G ")
                .pattern("CHM")
                .pattern(" G ")
                .define('G', ModItems.GEARS.get(Metal.STEEL).get())
                .define('C', circuit)
                .define('H', hull)
                .define('M', Tier5Items.ELECTRIC_MOTOR.get())
                .unlockedBy("has_lv_machine_hull", has(hull))
                .save(output, key("macerator"));
    }

    // Spec 10.4 to 10.6: the wiremill, bender and lathe and what they make. H hull, C basic circuit, M electric motor.
    private void shapingMachines() {
        shapingMachine(Tier5Items.WIREMILL.get(), "wiremill", Tier5Items.DRAW_PLATE.get());
        shapingMachine(Tier5Items.BENDER.get(), "bender", ModItems.RODS.get(Metal.STEEL).get());
        shapingMachine(Tier5Items.LATHE.get(), "lathe", ModItems.head(Metal.STEEL, dev.strataindustria.ceramics.MoldType.KNIFE_BLADE));

        // Wiremill: one more wire from an ingot than the anvil gets (2 rods, 4 wires).
        machining("wiremill", null, Tier5Items.COPPER_ROD.get(), Tier5Items.COPPER_WIRE.get(), 2);
        machining("wiremill", null, Tier5Items.RED_ALLOY_ROD.get(), Tier5Items.RED_ALLOY_WIRE.get(), 2);

        // Bender: an ingot makes a plate, a double ingot two.
        for (Metal metal : Metal.values()) {
            if (metal.hasPlate()) machining("bender", null, ModItems.ingot(metal), ModItems.PLATES.get(metal).get(), 1);
        }
        machining("bender", null, ModItems.ingot(Metal.LEAD), Tier5Items.LEAD_PLATE.get(), 1);
        machining("bender", null, ModItems.WROUGHT_IRON_DOUBLE_INGOT.get(), ModItems.PLATES.get(Metal.WROUGHT_IRON).get(), 2);
        machining("bender", null, ModItems.STEEL_DOUBLE_INGOT.get(), ModItems.PLATES.get(Metal.STEEL).get(), 2);

        // Lathe, rod mode: an ingot makes two rods. Gear mode: an ingot makes a gear.
        machining("lathe", "rod", ModItems.ingot(Metal.COPPER), Tier5Items.COPPER_ROD.get(), 2);
        machining("lathe", "rod", ModItems.ingot(Metal.RED_ALLOY), Tier5Items.RED_ALLOY_ROD.get(), 2);
        machining("lathe", "rod", ModItems.ingot(Metal.WROUGHT_IRON), ModItems.WROUGHT_IRON_ROD.get(), 2);
        for (Metal metal : Metal.values()) {
            if (metal.hasRod()) machining("lathe", "rod", ModItems.ingot(metal), ModItems.RODS.get(metal).get(), 2);
            if (metal.hasGear()) machining("lathe", "gear", ModItems.ingot(metal), ModItems.GEARS.get(metal).get(), 1);
        }
    }

    /** `.X.` / `CHM` / `.X.`: X is the machine's working part. */
    private void shapingMachine(Item result, String name, Item part) {
        Item hull = Tier5Items.LV_MACHINE_HULL.get();
        shaped(RecipeCategory.REDSTONE, result)
                .pattern(" X ")
                .pattern("CHM")
                .pattern(" X ")
                .define('X', part)
                .define('C', Tier5Items.BASIC_CIRCUIT.get())
                .define('H', hull)
                .define('M', Tier5Items.ELECTRIC_MOTOR.get())
                .unlockedBy("has_lv_machine_hull", has(hull))
                .save(output, key(name));
    }

    private void machining(String machine, String mode, Item input, Item result, int count) {
        String path = "machining/" + machine + (mode == null ? "" : "_" + mode) + "/" + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(result).getPath()
                + "_from_" + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(input).getPath();
        output.accept(key(path), new dev.strataindustria.processing.MachiningRecipe(machine, Optional.ofNullable(mode), Ingredient.of(input),
                new ItemStackTemplate(result, count)), null);
    }

    // Spec 7.2 and 7.3: P steel plate, H hull, B brass gear, R steel rod, F bronze fluid pipe, G steel gear, C basic circuit.
    private void generators() {
        Item plate = ModItems.PLATES.get(Metal.STEEL).get();
        Item hull = Tier5Items.LV_MACHINE_HULL.get();
        Item pipe = Tier4Items.BRONZE_FLUID_PIPE.get();
        shaped(RecipeCategory.REDSTONE, Tier5Items.STEAM_TURBINE.get())
                .pattern("PBP")
                .pattern("RHR")
                .pattern("PFP")
                .define('P', plate)
                .define('B', ModItems.GEARS.get(Metal.BRASS).get())
                .define('R', ModItems.RODS.get(Metal.STEEL).get())
                .define('H', hull)
                .define('F', pipe)
                .unlockedBy("has_lv_machine_hull", has(hull))
                .save(output, key("steam_turbine"));
        shaped(RecipeCategory.REDSTONE, Tier5Items.COMBUSTION_GENERATOR.get())
                .pattern("PFP")
                .pattern("GHG")
                .pattern("PCP")
                .define('P', plate)
                .define('F', pipe)
                .define('G', ModItems.GEARS.get(Metal.STEEL).get())
                .define('H', hull)
                .define('C', Tier5Items.BASIC_CIRCUIT.get())
                .unlockedBy("has_lv_machine_hull", has(hull))
                .save(output, key("combustion_generator"));
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
