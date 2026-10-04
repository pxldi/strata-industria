package dev.strataindustria.datagen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.geology.OreGrade;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.material.Metal;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.Tier4Items;
import dev.strataindustria.power.ElectricTier;
import dev.strataindustria.processing.ChemicalIo;
import dev.strataindustria.processing.ElectrolysisRecipe;
import dev.strataindustria.processing.MixingRecipe;
import dev.strataindustria.registry.Tier4Fluids;
import dev.strataindustria.registry.Tier5Fluids;
import dev.strataindustria.registry.Tier5Recipes;
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
        chemistry();
        storage();
        assembling();
        overheadLines();
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


    // Spec 9.4, 9.5 and 7.4: the cell, the battery box and the MV upgrade kit.
    private void storage() {
        Item plate = Tier5Items.LEAD_PLATE.get();
        Item cell = Tier5Items.LEAD_ACID_CELL.get();
        shaped(RecipeCategory.REDSTONE, cell)
                .pattern(" W ")
                .pattern("QAQ")
                .pattern(" Q ")
                .define('Q', plate)
                .define('A', Tier5Items.SULFURIC_ACID_BUCKET.get())
                .define('W', Tier5Items.COPPER_WIRE.get())
                .unlockedBy("has_sulfuric_acid_bucket", has(Tier5Items.SULFURIC_ACID_BUCKET.get()))
                .save(output, key("lead_acid_cell"));
        shaped(RecipeCategory.REDSTONE, Tier5Items.BATTERY_BOX.get())
                .pattern("QCQ")
                .pattern("EHE")
                .pattern("QEQ")
                .define('Q', plate)
                .define('C', Tier5Items.BASIC_CIRCUIT.get())
                .define('E', cell)
                .define('H', Tier5Items.LV_MACHINE_HULL.get())
                .unlockedBy("has_lead_acid_cell", has(cell))
                .save(output, key("battery_box"));
        shapeless(RecipeCategory.REDSTONE, Tier5Items.MV_UPGRADE_KIT.get())
                .requires(ModItems.PLATES.get(Metal.ALUMINIUM).get(), 2)
                .requires(Tier5Items.BASIC_CIRCUIT.get(), 2)
                .requires(Tier5Items.MV_CABLE.get())
                .requires(Tier5Items.ELECTRIC_MOTOR.get())
                .unlockedBy("has_aluminium_plate", has(ModItems.PLATES.get(Metal.ALUMINIUM).get()))
                .save(output, key("mv_upgrade_kit"));
        // Spec 8.2 and 8.5: the transformer and the energy adapter.
        shaped(RecipeCategory.REDSTONE, Tier5Items.TRANSFORMER.get())
                .pattern(" Q ")
                .pattern("WHW")
                .pattern(" L ")
                .define('Q', Tier5Items.MV_CABLE.get())
                .define('W', Tier5Items.COPPER_WIRE.get())
                .define('H', Tier5Items.LV_MACHINE_HULL.get())
                .define('L', Tier5Items.LV_CABLE.get())
                .unlockedBy("has_mv_cable", has(Tier5Items.MV_CABLE.get()))
                .save(output, key("transformer"));
        shapeless(RecipeCategory.REDSTONE, Tier5Items.ENERGY_ADAPTER.get())
                .requires(Tier5Items.LV_MACHINE_HULL.get())
                .requires(Tier5Items.BASIC_CIRCUIT.get())
                .requires(Tier5Items.LV_CABLE.get(), 2)
                .unlockedBy("has_lv_machine_hull", has(Tier5Items.LV_MACHINE_HULL.get()))
                .save(output, key("energy_adapter"));
    }

    // Spec 11.1 to 11.4: the mixer and electrolyser, the acid and the aluminium chain. H hull, C basic circuit.
    private void chemistry() {
        Item hull = Tier5Items.LV_MACHINE_HULL.get();
        shaped(RecipeCategory.REDSTONE, Tier5Items.MIXER.get())
                .pattern(" F ")
                .pattern("CHM")
                .pattern(" F ")
                .define('F', Tier4Items.BRONZE_FLUID_PIPE.get())
                .define('C', Tier5Items.BASIC_CIRCUIT.get())
                .define('H', hull)
                .define('M', Tier5Items.ELECTRIC_MOTOR.get())
                .unlockedBy("has_lv_machine_hull", has(hull))
                .save(output, key("mixer"));
        shaped(RecipeCategory.REDSTONE, Tier5Items.ELECTROLYSER.get())
                .pattern(" Q ")
                .pattern("CHW")
                .pattern(" Q ")
                .define('Q', Tier5Items.LEAD_PLATE.get())
                .define('C', Tier5Items.BASIC_CIRCUIT.get())
                .define('H', hull)
                .define('W', Tier5Items.COPPER_WIRE.get())
                .unlockedBy("has_lv_machine_hull", has(hull))
                .save(output, key("electrolyser"));

        FluidAmount so2 = new FluidAmount(Tier4Fluids.SULFUR_DIOXIDE.get(), 100), oxygen = new FluidAmount(Tier5Fluids.OXYGEN.source().get(), 50),
                water = new FluidAmount(Fluids.WATER, 100), acid = new FluidAmount(Tier5Fluids.SULFURIC_ACID.source().get(), 100);
        // Contact process: SO2 + oxygen + water make acid, 1 : 0.5 : 1 by mB.
        chemical("mixing/sulfuric_acid", Tier5Recipes.MIXING, new ChemicalIo(List.of(), List.of(so2, oxygen, water), List.of(), List.of(acid), 40,
                ElectricTier.LV, 0));
        chemical("mixing/alum", Tier5Recipes.MIXING, new ChemicalIo(List.of(new ChemicalIo.ItemInput(Ingredient.of(Items.CLAY_BALL), 4)),
                List.of(acid), List.of(new ItemStackTemplate(Tier5Items.ALUM.get())), List.of(), 200, ElectricTier.LV, 0));
        // Alum needs a roaster or electric furnace: a forge would lose the gas.
        output.accept(key("roasting/alumina"), new RoastingRecipe(Ingredient.of(Tier5Items.ALUM.get()), new ItemStackTemplate(Tier5Items.ALUMINA.get()),
                800, 200, Optional.of(new FluidAmount(Tier4Fluids.SULFUR_DIOXIDE.get(), 50)), false), null);
        output.accept(key("roasting/sulfur"), new RoastingRecipe(Ingredient.of(Tier4Items.SULFUR_DUST.get()), Optional.empty(),
                400, 100, Optional.of(new FluidAmount(Tier4Fluids.SULFUR_DIOXIDE.get(), 100)), false), null);

        chemical("electrolysis/water", Tier5Recipes.ELECTROLYSIS, new ChemicalIo(List.of(), List.of(new FluidAmount(Fluids.WATER, 1000)), List.of(),
                List.of(new FluidAmount(Tier5Fluids.HYDROGEN.source().get(), 1000), new FluidAmount(Tier5Fluids.OXYGEN.source().get(), 500)), 200,
                ElectricTier.LV, 0));
        chemical("electrolysis/brine", Tier5Recipes.ELECTROLYSIS, new ChemicalIo(List.of(), List.of(new FluidAmount(Tier5Fluids.BRINE.source().get(), 1000)),
                List.of(), List.of(new FluidAmount(Tier5Fluids.CHLORINE.source().get(), 500), new FluidAmount(Tier5Fluids.HYDROGEN.source().get(), 500),
                new FluidAmount(dev.strataindustria.registry.ModFluids.LYE.get(), 1000)), 200, ElectricTier.LV, 0));
        chemical("electrolysis/aluminium", Tier5Recipes.ELECTROLYSIS, new ChemicalIo(List.of(
                new ChemicalIo.ItemInput(Ingredient.of(Tier5Items.ALUMINA.get()), 2), new ChemicalIo.ItemInput(Ingredient.of(Tier4Items.COKE_DUST.get()), 1)),
                List.of(), List.of(new ItemStackTemplate(ModItems.ingot(Metal.ALUMINIUM))), List.of(), 600, ElectricTier.LV, 700));

        // Spec 4.1 and 10.4: wire from rods, drawn by hand too for aluminium.
        machining("wiremill", null, ModItems.RODS.get(Metal.ALUMINIUM).get(), Tier5Items.ALUMINIUM_WIRE.get(), 2);
        machining("wiremill", null, ModItems.RODS.get(Metal.STEEL).get(), Tier5Items.STEEL_WIRE.get(), 2);
        anvil("aluminium_wire", ModItems.ingot(Metal.ALUMINIUM), Tier5Items.ALUMINIUM_WIRE.get(), 3, 80,
                rule(Rule.Kind.HIT, Rule.Where.LAST), rule(Rule.Kind.DRAW, Rule.Where.SECOND_LAST), rule(Rule.Kind.DRAW, Rule.Where.THIRD_LAST));
        anvil("steel_wire", ModItems.ingot(Metal.STEEL), Tier5Items.STEEL_WIRE.get(), 3, 90,
                rule(Rule.Kind.HIT, Rule.Where.LAST), rule(Rule.Kind.DRAW, Rule.Where.SECOND_LAST), rule(Rule.Kind.DRAW, Rule.Where.THIRD_LAST));
    }

    // Spec 10.7: the assembler and the recipes it runs. H hull, C basic circuit, M electric motor, T inserter.
    private void assembling() {
        shaped(RecipeCategory.REDSTONE, Tier5Items.ASSEMBLER.get())
                .pattern(" T ")
                .pattern("CHM")
                .pattern(" T ")
                .define('T', Tier4Items.INSERTER.get())
                .define('C', Tier5Items.BASIC_CIRCUIT.get())
                .define('H', Tier5Items.LV_MACHINE_HULL.get())
                .define('M', Tier5Items.ELECTRIC_MOTOR.get())
                .unlockedBy("has_lv_machine_hull", has(Tier5Items.LV_MACHINE_HULL.get()))
                .save(output, key("assembler"));

        Item steelPlate = ModItems.PLATES.get(Metal.STEEL).get(), steelRod = ModItems.RODS.get(Metal.STEEL).get();
        Item wire = Tier5Items.COPPER_WIRE.get();
        assemble("basic_circuit", 200, Tier5Items.BASIC_CIRCUIT.get(), 2, null, Tier5Items.CIRCUIT_BOARD.get(), 1, Tier5Items.RED_ALLOY_WIRE.get(), 2,
                Items.REDSTONE, 2);
        assemble("electric_motor", 200, Tier5Items.ELECTRIC_MOTOR.get(), 1, null, steelPlate, 2, wire, 4, steelRod, 1, Tier5Items.MAGNET.get(), 1);
        assemble("magnet", 400, Tier5Items.MAGNET.get(), 1, null, steelRod, 1, wire, 8);
        assemble("lead_acid_cell", 100, Tier5Items.LEAD_ACID_CELL.get(), 1, new FluidAmount(Tier5Fluids.SULFURIC_ACID.source().get(), 250),
                Tier5Items.LEAD_PLATE.get(), 2, wire, 1);
        assemble("lv_machine_hull", 200, Tier5Items.LV_MACHINE_HULL.get(), 1, null, steelPlate, 4, ModItems.PLATES.get(Metal.WROUGHT_IRON).get(), 2,
                Tier5Items.LV_CABLE.get(), 2);
        assemble("lv_cable", 100, Tier5Items.LV_CABLE.get(), 6, null, wire, 6, Tier5Items.RUBBER.get(), 3);
        assemble("mv_upgrade_kit", 200, Tier5Items.MV_UPGRADE_KIT.get(), 1, null, ModItems.PLATES.get(Metal.ALUMINIUM).get(), 2,
                Tier5Items.BASIC_CIRCUIT.get(), 2, Tier5Items.MV_CABLE.get(), 1, Tier5Items.ELECTRIC_MOTOR.get(), 1);
    }

    // Spec 5.4, 8.3 and 8.4: treated logs, poles, hand-made insulators and the span conductor.
    private void overheadLines() {
        output.accept(key("barrel/treated_log"), new BarrelRecipe(Optional.of(tag(net.minecraft.tags.ItemTags.LOGS)), 1,
                Optional.of(new FluidAmount(Tier4Fluids.CREOSOTE.get(), 250)), Optional.of(new ItemStackTemplate(Tier5Items.TREATED_LOG.get())),
                Optional.empty(), 1200), null);
        Ingredient saw = tag(dev.strataindustria.registry.ModTags.Items.SAWS);
        output.accept(key("utility_pole"), new dev.strataindustria.crafting.ToolShapelessRecipe(new Recipe.CommonInfo(true),
                new net.minecraft.world.item.crafting.CraftingRecipe.CraftingBookInfo(net.minecraft.world.item.crafting.CraftingBookCategory.BUILDING, "poles"),
                new ItemStackTemplate(Tier5Items.UTILITY_POLE.get(), 2), List.of(Ingredient.of(Tier5Items.TREATED_LOG.get()), saw), saw), null);
        output.accept(key("sawing/utility_pole"), new dev.strataindustria.machine.SawingRecipe(Ingredient.of(Tier5Items.TREATED_LOG.get()),
                new ItemStackTemplate(Tier5Items.UTILITY_POLE.get(), 3), Optional.empty(), dev.strataindustria.machine.SawingRecipe.DEFAULT_TICKS), null);
        shapeless(RecipeCategory.REDSTONE, Tier5Items.POLE_INSULATOR.get())
                .requires(Tier5Items.CERAMIC_INSULATOR.get())
                .requires(ModItems.RODS.get(Metal.STEEL).get())
                .unlockedBy("has_ceramic_insulator", has(Tier5Items.CERAMIC_INSULATOR.get()))
                .save(output, key("pole_insulator"));
        int pattern = dev.strataindustria.knapping.GridPattern.parse(List.of(".###.", "..#..", ".###.", "..#..", ".###.")).getOrThrow();
        output.accept(key("clay_forming/unfired_insulator"), new dev.strataindustria.knapping.KnappingRecipe(Ingredient.of(Items.CLAY_BALL),
                dev.strataindustria.knapping.Knapping.CLAY_OPENING_COST, pattern, true, new ItemStackTemplate(Tier5Items.UNFIRED_INSULATOR.get(), 2)), null);
        assemble("acsr_conductor", 200, Tier5Items.ACSR_CONDUCTOR.get(), 4, null, Tier5Items.ALUMINIUM_WIRE.get(), 6, Tier5Items.STEEL_WIRE.get(), 1);
    }

    /** An assembler recipe: {@code inputs} alternate item and count. */
    private void assemble(String name, int ticks, Item result, int count, FluidAmount fluid, Object... inputs) {
        List<ChemicalIo.ItemInput> items = new java.util.ArrayList<>();
        for (int i = 0; i < inputs.length; i += 2) items.add(new ChemicalIo.ItemInput(Ingredient.of((Item) inputs[i]), (Integer) inputs[i + 1]));
        output.accept(key("assembling/" + name), new dev.strataindustria.processing.AssemblingRecipe(new ChemicalIo(items,
                fluid == null ? List.of() : List.of(fluid), List.of(new ItemStackTemplate(result, count)), List.of(), ticks, ElectricTier.LV, 0)), null);
    }

    private <R extends Recipe<?>> void chemical(String path, net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.item.crafting.RecipeType<?>,
            ? extends net.minecraft.world.item.crafting.RecipeType<?>> type, ChemicalIo io) {
        output.accept(key(path), type == Tier5Recipes.MIXING ? new MixingRecipe(io) : new ElectrolysisRecipe(io), null);
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
