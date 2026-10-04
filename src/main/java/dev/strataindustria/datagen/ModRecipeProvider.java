package dev.strataindustria.datagen;

import dev.strataindustria.structure.StructureContent;
import java.util.Optional;
import net.minecraft.world.level.material.Fluids;
import dev.strataindustria.registry.ModFluids;
import dev.strataindustria.tanning.FluidAmount;
import dev.strataindustria.tanning.BarrelRecipe;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.ceramics.MoldType;
import dev.strataindustria.crafting.ConfigCondition;
import dev.strataindustria.crafting.KnappedToolRecipe;
import dev.strataindustria.crafting.MetalArmourRecipe;
import dev.strataindustria.crafting.MetalToolRecipe;
import dev.strataindustria.crafting.ToolShapelessRecipe;
import dev.strataindustria.geology.OreGrade;
import dev.strataindustria.geology.OreMineral;
import dev.strataindustria.geology.Rock;
import dev.strataindustria.knapping.GridPattern;
import dev.strataindustria.knapping.Knapping;
import dev.strataindustria.knapping.KnappingRecipe;
import dev.strataindustria.machine.SawingRecipe;
import dev.strataindustria.material.Metal;
import dev.strataindustria.quern.QuernRecipe;
import dev.strataindustria.smithing.AnvilRecipe;
import dev.strataindustria.smithing.WeldingRecipe;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.ModTags;
import dev.strataindustria.registry.Tier4Items;
import dev.strataindustria.washing.WashingRecipe;
import dev.strataindustria.roasting.RoastingRecipe;
import dev.strataindustria.registry.Tier4Fluids;
import java.util.List;
import java.util.Map;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.RecipeUnlockAdvancementBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.ItemLike;

final class ModRecipeProvider extends RecipeProvider {
    private final BootstrapContext<Recipe<?>> recipeContext;
    private final BootstrapContext<Advancement> advancementContext;

    ModRecipeProvider(BootstrapContext<Recipe<?>> recipeOutput, BootstrapContext<Advancement> advancementOutput) {
        super(recipeOutput, advancementOutput);
        this.recipeContext = recipeOutput;
        this.advancementContext = advancementOutput;
    }

    @Override
    protected void buildRecipes() {
        for (Rock rock : Rock.values()) {
            var shard = ModItems.ROCK_SHARD.get(rock).get();
            var cobbled = ModItems.COBBLED_ROCK.get(rock).get();
            shaped(RecipeCategory.BUILDING_BLOCKS, cobbled)
                    .pattern("RR")
                    .pattern("RR")
                    .define('R', shard)
                    .unlockedBy("has_rock_shard", has(shard))
                    .save(output, key("cobbled_" + rock.id()));
        }

        knapping();
        clayForming();
        patterns();
        forge();
        prologueMachines();
        fibre();
        stoneTools();
        fire();
        planks();
        metals();
        quern();
        smithing();
        ironAge();
        tier4();
        new Tier5RecipeProvider(recipeContext, advancementContext).buildRecipes();
        new Tier6RecipeProvider(recipeContext, advancementContext).buildRecipes();
        new GridData.Recipes(recipeContext, advancementContext).buildRecipes();
        new StructureData.CollectibleRecipes(recipeContext, advancementContext).buildRecipes();
        new SharedBlockData.Recipes(recipeContext, advancementContext).buildRecipes();
        new FootData.Recipes(recipeContext, advancementContext).buildRecipes();
        new RailData.Recipes(recipeContext, advancementContext).buildRecipes();
        new RailwayData.Recipes(recipeContext, advancementContext).buildRecipes();
        new RopewayData.Recipes(recipeContext, advancementContext).buildRecipes();
        vanillaOverrides();
    }

    /** Blows per shape (redesign L5): a tool head is four, a clay press or a carving is three. */
    private static final int KNAP_BLOWS = 4, CLAY_BLOWS = 3, CARVE_BLOWS = 3;

    // Tier 0-2 spec 3.2. Patterns are written top row first; '#' is the shape that comes out of the stone.
    private void knapping() {
        knap(ModItems.STONE_AXE_HEAD.get(), ".#...", "####.", "#####", "####.", ".#...");
        knap(ModItems.STONE_KNIFE_BLADE.get(), "#....", "##...", ".##..", "..##.", "...##");
        knap(ModItems.STONE_SHOVEL_HEAD.get(), ".###.", ".###.", ".###.", ".###.", "..#..");
        knap(ModItems.STONE_HOE_HEAD.get(), "#####", "##...", ".....", ".....", ".....");
        knap(ModItems.STONE_HAMMER_HEAD.get(), "#####", "#####", "..#..", ".....", ".....");
        knap(ModItems.STONE_SPEAR_HEAD.get(), "..#..", ".###.", ".###.", "..#..", "..#..");
        knap(ModItems.STONE_PICKAXE_HEAD.get(), ".###.", "#...#", ".....", ".....", ".....");
        // Spec 10.1: a round stone with a hole, worth four rock shards.
        int quernstone = GridPattern.parse(List.of(".###.", "#####", "##.##", "#####", ".###.")).getOrThrow();
        output.accept(key("knapping/quernstone"), new KnappingRecipe(tag(ModTags.Items.ROCK_SHARDS), 4, 6, quernstone,
                new ItemStackTemplate(ModItems.QUERNSTONE.get())), null);
    }

    // Spec 4.1: the same strikes, worked in five clay balls.
    private void clayForming() {
        form(ModItems.UNFIRED_CRUCIBLE.get(), 1, "##.##", "#...#", "#...#", "#...#", "#####");
        form(ModItems.UNFIRED_INGOT_MOLD.get(), 1, ".....", "#####", "#...#", "#####", ".....");
        form(ModItems.UNFIRED_BRICK.get(), 4, "##.##", "##.##", ".....", "##.##", "##.##");
        form(mold(MoldType.PICKAXE_HEAD), 1, "#...#", ".###.", "#####", "#####", "#####");
        form(mold(MoldType.AXE_HEAD), 1, "#.###", "....#", ".....", "....#", "#.###");
        form(mold(MoldType.SHOVEL_HEAD), 1, "#...#", "#...#", "#...#", "#...#", "##.##");
        form(mold(MoldType.HOE_HEAD), 1, ".....", "..###", "#####", "#####", "#####");
        form(mold(MoldType.KNIFE_BLADE), 1, ".####", "..###", "#..##", "##..#", "###..");
        form(mold(MoldType.HAMMER_HEAD), 1, ".....", ".....", "##.##", "#####", "#####");
        form(mold(MoldType.SAW_BLADE), 1, "#####", "#####", ".....", ".....", "#####");
        form(mold(MoldType.SWORD_BLADE), 1, "###..", "##..#", "#..##", "..###", ".####");
    }

    // Pattern casting: a plank blank is carved with the same strikes, a flask is a plank frame round damp sand.
    private void patterns() {
        shapeless(RecipeCategory.MISC, dev.strataindustria.registry.PatternRegistry.PATTERN_BLANK.get(), 2)
                .requires(ItemTags.PLANKS)
                .unlockedBy("has_crucible", has(ModItems.CRUCIBLE.get()))
                .save(output, key("pattern_blank"));
        shaped(RecipeCategory.MISC, dev.strataindustria.registry.PatternRegistry.SAND_FLASK.get(), 2)
                .pattern("P P")
                .pattern("PSP")
                .define('P', ItemTags.PLANKS)
                .define('S', Items.SAND)
                .unlockedBy("has_crucible", has(ModItems.CRUCIBLE.get()))
                .save(output, key("sand_flask"));
        carve("ingot", ".....", "#####", "#...#", "#####", ".....");
        carve("gear", "#.#.#", ".....", "#...#", ".....", "#.#.#");
        carve(MoldType.PICKAXE_HEAD.id(), "#...#", ".###.", "#####", "#####", "#####");
        carve(MoldType.AXE_HEAD.id(), "#.###", "....#", ".....", "....#", "#.###");
        carve(MoldType.SHOVEL_HEAD.id(), "#...#", "#...#", "#...#", "#...#", "##.##");
        carve(MoldType.HOE_HEAD.id(), ".....", "..###", "#####", "#####", "#####");
        carve(MoldType.KNIFE_BLADE.id(), ".####", "..###", "#..##", "##..#", "###..");
        carve(MoldType.HAMMER_HEAD.id(), ".....", ".....", "##.##", "#####", "#####");
        carve(MoldType.SAW_BLADE.id(), "#####", "#####", ".....", ".....", "#####");
        carve(MoldType.SWORD_BLADE.id(), "###..", "##..#", "#..##", "..###", ".####");
    }

    private void carve(String shape, String... rows) {
        int pattern = GridPattern.parse(List.of(rows)).getOrThrow();
        Item result = dev.strataindustria.registry.PatternRegistry.PATTERNS.get(shape).get();
        output.accept(key("carving/" + shape + "_pattern"), new KnappingRecipe(
                Ingredient.of(dev.strataindustria.registry.PatternRegistry.PATTERN_BLANK.get()), 1, CARVE_BLOWS, pattern,
                new ItemStackTemplate(result)), null);
    }

    // Spec 4.5: seven bricks around a charcoal hearth.
    private void forge() {
        shaped(RecipeCategory.DECORATIONS, ModItems.FORGE.get())
                .pattern("B B")
                .pattern("BCB")
                .pattern("BBB")
                .define('B', Items.BRICK)
                .define('C', Items.CHARCOAL)
                .unlockedBy("has_brick", has(Items.BRICK))
                .save(output, key("forge"));
    }

    // Prologue machines: a ring of bricks makes the kiln, a stone top on brick legs makes the table.
    private void prologueMachines() {
        shaped(RecipeCategory.DECORATIONS, dev.strataindustria.registry.PrologueRegistry.BRICK_KILN_ITEM.get())
                .pattern("BBB")
                .pattern("B B")
                .pattern("BBB")
                .define('B', Items.BRICK)
                .unlockedBy("has_forge", has(ModItems.FORGE.get()))
                .save(output, key("brick_kiln"));
        shaped(RecipeCategory.DECORATIONS, dev.strataindustria.registry.PrologueRegistry.CASTING_TABLE_ITEM.get())
                .pattern("SSS")
                .pattern("B B")
                .define('S', ItemTags.STONE_CRAFTING_MATERIALS)
                .define('B', Items.BRICK)
                .unlockedBy("has_crucible", has(ModItems.CRUCIBLE.get()))
                .save(output, key("casting_table"));
    }

    private static Item mold(MoldType type) {
        return ModItems.UNFIRED_MOLDS.get(type).get();
    }

    private void form(Item result, int count, String... rows) {
        int pattern = GridPattern.parse(List.of(rows)).getOrThrow();
        var recipe = new KnappingRecipe(Ingredient.of(Items.CLAY_BALL), Knapping.CLAY_OPENING_COST, CLAY_BLOWS, pattern,
                new ItemStackTemplate(result, count));
        output.accept(key("clay_forming/" + name(result)), recipe, null);
    }

    private void formFireClay(Item result, String... rows) {
        int pattern = GridPattern.parse(List.of(rows)).getOrThrow();
        output.accept(key("clay_forming/" + name(result)), new KnappingRecipe(Ingredient.of(ModItems.FIRE_CLAY_BALL.get()),
                Knapping.CLAY_OPENING_COST, CLAY_BLOWS, pattern, new ItemStackTemplate(result)), null);
    }

    private void knap(Item result, String... rows) {
        int pattern = GridPattern.parse(List.of(rows)).getOrThrow();
        var recipe = new KnappingRecipe(tag(ModTags.Items.KNAPPABLE), 1, KNAP_BLOWS, pattern, new ItemStackTemplate(result));
        output.accept(key("knapping/" + name(result)), recipe, null);
    }

    // Spec 3.3.
    private void fibre() {
        shapeless(RecipeCategory.MISC, ModItems.TWINE.get())
                .requires(ModItems.PLANT_FIBRE.get(), 2)
                .unlockedBy("has_plant_fibre", has(ModItems.PLANT_FIBRE.get()))
                .save(output, key("twine"));
        shaped(RecipeCategory.MISC, ModItems.FIBRE_CLOTH.get())
                .pattern("TT")
                .pattern("TT")
                .define('T', ModItems.TWINE.get())
                .unlockedBy("has_twine", has(ModItems.TWINE.get()))
                .save(output, key("fibre_cloth"));
        // Spec 3.6.
        shapeless(RecipeCategory.MISC, ModItems.FIELD_JOURNAL.get())
                .requires(ModItems.STRAW.get(), 2)
                .requires(ModItems.TWINE.get())
                .unlockedBy("has_twine", has(ModItems.TWINE.get()))
                .save(output, key("field_journal"));
        // Structures spec 9.
        shaped(RecipeCategory.BUILDING_BLOCKS, StructureContent.FIBRE_CANVAS_ITEM.get())
                .pattern("CC")
                .pattern("CC")
                .define('C', ModItems.FIBRE_CLOTH.get())
                .unlockedBy("has_fibre_cloth", has(ModItems.FIBRE_CLOTH.get()))
                .save(output, key("fibre_canvas"));
        shaped(RecipeCategory.DECORATIONS, StructureContent.FIBRE_CANVAS_CARPET_ITEM.get(), 3)
                .pattern("CC")
                .define('C', StructureContent.FIBRE_CANVAS_ITEM.get())
                .unlockedBy("has_fibre_canvas", has(StructureContent.FIBRE_CANVAS_ITEM.get()))
                .save(output, key("fibre_canvas_carpet"));
        shaped(RecipeCategory.BUILDING_BLOCKS, StructureContent.FIBRE_CANVAS_STAIRS_ITEM.get(), 4)
                .pattern("C  ")
                .pattern("CC ")
                .pattern("CCC")
                .define('C', StructureContent.FIBRE_CANVAS_ITEM.get())
                .unlockedBy("has_fibre_canvas", has(StructureContent.FIBRE_CANVAS_ITEM.get()))
                .save(output, key("fibre_canvas_stairs"));
        shaped(RecipeCategory.BUILDING_BLOCKS, StructureContent.PIT_PROP_ITEM.get(), 4)
                .pattern("L")
                .pattern("L")
                .pattern("L")
                .define('L', net.neoforged.neoforge.common.Tags.Items.STRIPPED_LOGS)
                .unlockedBy("has_stripped_log", has(net.neoforged.neoforge.common.Tags.Items.STRIPPED_LOGS))
                .save(output, key("pit_prop"));
    }

    // Spec 3.5.
    private void fire() {
        shapeless(RecipeCategory.DECORATIONS, ModItems.FIRE_PIT.get())
                .requires(Items.STICK, 4)
                .requires(ModItems.STRAW.get())
                .unlockedBy("has_straw", has(ModItems.STRAW.get()))
                .save(output, key("fire_pit"));
    }

    // Spec 3.4: head + stick + twine.
    private void stoneTools() {
        knappedTool(ModItems.STONE_AXE.get(), ModItems.STONE_AXE_HEAD.get(), ModItems.KNAPPED_DURABILITY);
        knappedTool(ModItems.STONE_KNIFE.get(), ModItems.STONE_KNIFE_BLADE.get(), ModItems.KNAPPED_DURABILITY);
        knappedTool(ModItems.STONE_SHOVEL.get(), ModItems.STONE_SHOVEL_HEAD.get(), ModItems.KNAPPED_DURABILITY);
        knappedTool(ModItems.STONE_HOE.get(), ModItems.STONE_HOE_HEAD.get(), ModItems.KNAPPED_DURABILITY);
        knappedTool(ModItems.STONE_HAMMER.get(), ModItems.STONE_HAMMER_HEAD.get(), ModItems.KNAPPED_DURABILITY);
        knappedTool(ModItems.STONE_PICKAXE.get(), ModItems.STONE_PICKAXE_HEAD.get(), ModItems.KNAPPED_DURABILITY);
        knappedTool(Items.STONE_SPEAR, ModItems.STONE_SPEAR_HEAD.get(), ModItems.KNAPPED_SPEAR_DURABILITY);
    }

    private void knappedTool(Item tool, Item head, int durability) {
        var recipe = new KnappedToolRecipe(new Recipe.CommonInfo(true),
                new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.EQUIPMENT, ""),
                new ItemStackTemplate(tool),
                List.of(Ingredient.of(head), Ingredient.of(Items.STICK), Ingredient.of(ModItems.TWINE.get())),
                durability);
        save(key(name(tool)), recipe, RecipeCategory.TOOLS, "has_" + name(head), has(head));
    }

    // Spec 2: log + axe gives 2 planks, log + saw gives 4; the tool takes 1 damage.
    private void planks() {
        logToPlanks(ItemTags.OAK_LOGS, Items.OAK_PLANKS);
        logToPlanks(ItemTags.SPRUCE_LOGS, Items.SPRUCE_PLANKS);
        logToPlanks(ItemTags.BIRCH_LOGS, Items.BIRCH_PLANKS);
        logToPlanks(ItemTags.JUNGLE_LOGS, Items.JUNGLE_PLANKS);
        logToPlanks(ItemTags.ACACIA_LOGS, Items.ACACIA_PLANKS);
        logToPlanks(ItemTags.DARK_OAK_LOGS, Items.DARK_OAK_PLANKS);
        logToPlanks(ItemTags.MANGROVE_LOGS, Items.MANGROVE_PLANKS);
        logToPlanks(ItemTags.CHERRY_LOGS, Items.CHERRY_PLANKS);
        logToPlanks(ItemTags.PALE_OAK_LOGS, Items.PALE_OAK_PLANKS);
        logToPlanks(ItemTags.POPLAR_LOGS, Items.POPLAR_PLANKS);
        logToPlanks(ItemTags.CRIMSON_STEMS, Items.CRIMSON_PLANKS);
        logToPlanks(ItemTags.WARPED_STEMS, Items.WARPED_PLANKS);
    }

    private void logToPlanks(TagKey<Item> logs, Item planks) {
        logToPlanks(logs, planks, ModTags.Items.AXES, 2, "_with_axe");
        logToPlanks(logs, planks, ModTags.Items.SAWS, 4, "_with_saw");
    }

    private void logToPlanks(TagKey<Item> logs, Item planks, TagKey<Item> tools, int count, String suffix) {
        Ingredient tool = tag(tools);
        var recipe = new ToolShapelessRecipe(new Recipe.CommonInfo(true),
                new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.BUILDING, "planks"),
                new ItemStackTemplate(planks, count),
                List.of(tag(logs), tool),
                tool);
        save(key(name(planks) + suffix), recipe, RecipeCategory.BUILDING_BLOCKS, "has_logs", has(logs));
    }

    // Spec 9.3: what an anvil makes from one ingot (two for a sword blade), and the rules that finish it.
    private void smithing() {
        for (Metal metal : Metal.values()) {
            if (!metal.isToolMetal()) continue;
            Item ingot = ModItems.ingot(metal);
            String m = metal.id();
            var types = metal.toolTypes();
            anvil(m + "_plate", ingot, 1, ModItems.PLATES.get(metal).get(), 3);
            anvil(m + "_pickaxe_head", ingot, 1, ModItems.head(metal, MoldType.PICKAXE_HEAD), 5);
            anvil(m + "_axe_head", ingot, 1, ModItems.head(metal, MoldType.AXE_HEAD), 5);
            anvil(m + "_shovel_head", ingot, 1, ModItems.head(metal, MoldType.SHOVEL_HEAD), 5);
            anvil(m + "_hoe_head", ingot, 1, ModItems.head(metal, MoldType.HOE_HEAD), 5);
            if (types.contains(MoldType.KNIFE_BLADE)) {
                anvil(m + "_knife_blade", ingot, 1, ModItems.head(metal, MoldType.KNIFE_BLADE), 5);
                anvil(m + "_hammer_head", ingot, 1, ModItems.head(metal, MoldType.HAMMER_HEAD), 5);
                anvil(m + "_saw_blade", ingot, 1, ModItems.head(metal, MoldType.SAW_BLADE), 5);
                anvil("tongs_jaw_from_" + m, ingot, 1, ModItems.TONGS_JAW.get(), 4);
            }
            // Tier 3 spec 9.3 and tier 4 spec 14.1: iron and steel sword blades are drawn from one welded double ingot.
            Item doubleIngot = metal == Metal.WROUGHT_IRON ? ModItems.WROUGHT_IRON_DOUBLE_INGOT.get()
                    : metal == Metal.STEEL ? ModItems.STEEL_DOUBLE_INGOT.get() : null;
            Item bladeStock = doubleIngot != null ? doubleIngot : ingot;
            anvil(m + "_sword_blade", bladeStock, doubleIngot != null ? 1 : 2, ModItems.head(metal, MoldType.SWORD_BLADE), 5);
            if (metal == Metal.WROUGHT_IRON) {
                output.accept(key("anvil/wrought_iron_rod"), new AnvilRecipe(Ingredient.of(ingot), 1,
                        new ItemStackTemplate(ModItems.WROUGHT_IRON_ROD.get(), 2), 3), null);
            }
            if (ModItems.PROSPECTOR_HEADS.containsKey(metal)) {
                Item head = ModItems.PROSPECTOR_HEADS.get(metal).get();
                anvil(m + "_prospectors_pick_head", ingot, 1, head, 5);
                Item pick = ModItems.PROSPECTORS_PICKS.get(metal).get();
                save(key(name(pick)), new MetalToolRecipe(new Recipe.CommonInfo(true),
                        new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.EQUIPMENT, ""), new ItemStackTemplate(pick),
                        List.of(Ingredient.of(head), Ingredient.of(Items.STICK))), RecipeCategory.TOOLS, "has_" + name(head), has(head));
            }
            armour(metal);
        }
        // Tier 4 spec 14.1: brass and steel plates, rods and gears.
        for (Metal metal : Metal.values()) {
            if (!ModItems.RODS.containsKey(metal)) continue;
            Item ingot = ModItems.ingot(metal);
            String m = metal.id();
            if (!metal.isToolMetal()) {
                anvil(m + "_plate", ingot, 1, ModItems.PLATES.get(metal).get(), 3);
            }
            output.accept(key("anvil/" + m + "_rod"), new AnvilRecipe(Ingredient.of(ingot), 1,
                    new ItemStackTemplate(ModItems.RODS.get(metal).get(), 2), 3), null);
            if (ModItems.GEARS.containsKey(metal)) {
                anvil(m + "_gear", ingot, 1, ModItems.GEARS.get(metal).get(), 4);
            }
        }
        shapeless(RecipeCategory.TOOLS, ModItems.TONGS.get())
                .requires(ModItems.TONGS_JAW.get())
                .requires(Items.STICK, 2)
                .unlockedBy("has_tongs_jaw", has(ModItems.TONGS_JAW.get()))
                .save(output, key("tongs"));
    }

    // Spec 8.4: armour from plates over fibre cloth, 14 plates a set.
    private void armour(Metal metal) {
        Item plate = ModItems.PLATES.get(metal).get();
        // Tier 3 spec 10.4: iron and gold plates are laced onto leather instead of fibre cloth.
        Item cloth = metal == Metal.WROUGHT_IRON || metal == Metal.GOLD || metal == Metal.STEEL ? Items.LEATHER : ModItems.FIBRE_CLOTH.get();
        var pieces = ModItems.ARMOUR.get(metal);
        // Shapes from the spec table: helmet, chestplate, leggings, boots.
        String[][] shapes = {{"PPP", " C "}, {"P P", "PCP", " P "}, {"PCP", "P P"}, {"PCP"}};
        var types = ModItems.armourTypes();
        for (int i = 0; i < types.length; i++) {
            Item piece = pieces.get(types[i]).get();
            var pattern = ShapedRecipePattern.of(Map.of('P', Ingredient.of(plate), 'C', Ingredient.of(cloth)), shapes[i]);
            var recipe = new MetalArmourRecipe(new Recipe.CommonInfo(true),
                    new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.EQUIPMENT, ""), pattern, new ItemStackTemplate(piece));
            String path = metal.isVanilla() ? metal.id() + "_" + types[i].getName() + "_from_plates" : name(piece);
            save(key(path), recipe, RecipeCategory.COMBAT, "has_plate", has(plate));
        }
    }


    // Tier 3 spec 3: fire clay, fire bricks and the fire brick furnace.
    /** Tier 4 spec 4.6 and 5: coke, the coke oven and treated wood. */
    private void tier4() {
        Item fireClay = ModItems.FIRE_CLAY_BALL.get();
        shapeless(RecipeCategory.MISC, Tier4Items.UNFIRED_COKE_OVEN_BRICK.get(), 4)
                .requires(fireClay, 2)
                .requires(Items.SAND)
                .unlockedBy("has_fire_clay_ball", has(fireClay))
                .save(output, key("unfired_coke_oven_brick"));
        Item ovenBrick = Tier4Items.COKE_OVEN_BRICK.get();
        shaped(RecipeCategory.BUILDING_BLOCKS, Tier4Items.COKE_OVEN_BRICKS.get())
                .pattern("BB")
                .pattern("BB")
                .define('B', ovenBrick)
                .unlockedBy("has_coke_oven_brick", has(ovenBrick))
                .save(output, key("coke_oven_bricks"));
        Item ironPlate = ModItems.PLATES.get(Metal.WROUGHT_IRON).get();
        shaped(RecipeCategory.DECORATIONS, Tier4Items.COKE_OVEN_DOOR.get())
                .pattern("BIB")
                .pattern("I I")
                .pattern("BIB")
                .define('B', ovenBrick)
                .define('I', ironPlate)
                .unlockedBy("has_coke_oven_brick", has(ovenBrick))
                .save(output, key("coke_oven_door"));

        // Spec 6.1: refractory pieces formed in fire clay, and a gear mold in plain clay.
        formFireClay(Tier4Items.UNFIRED_REFRACTORY_CRUCIBLE.get(), "##.##", "#...#", "#...#", "#...#", "#####");
        formFireClay(Tier4Items.UNFIRED_REFRACTORY_INGOT_MOLD.get(), ".....", "#####", "#...#", "#####", ".....");
        formFireClay(Tier4Items.UNFIRED_REFRACTORY_GEAR_MOLD.get(), "#.#.#", ".....", "#...#", ".....", "#.#.#");
        form(Tier4Items.UNFIRED_GEAR_MOLD.get(), 1, "#.#.#", ".....", "#...#", ".....", "#.#.#");
        // Uniqueness 4.2: a bell mold, formed like a bell.
        form(dev.strataindustria.bronze.BronzeRegistry.UNFIRED_BELL_MOLD.get(), 1, "..#..", ".###.", ".###.", "#####", "#####");

        // Spec 5.3: crushed sphalerite roasts to zinc calcine at 800 °C, giving off sulfur dioxide.
        for (OreGrade grade : OreGrade.values()) {
            roast(grade.prefix() + "zinc_calcine", ModItems.crushedOre(OreMineral.SPHALERITE, grade), Tier4Items.zincCalcine(grade), 400, 50);
        }
        roast("small_zinc_calcine", ModItems.SMALL_ORES.get(OreMineral.SPHALERITE).get(), Tier4Items.SMALL_ZINC_CALCINE.get(), 200, 15);

        // Spec 14.1: welded steel.
        Item steel = ModItems.ingot(Metal.STEEL);
        Item steelDouble = ModItems.STEEL_DOUBLE_INGOT.get();
        output.accept(key("welding/steel_double_ingot"), new WeldingRecipe(Ingredient.of(steel), Ingredient.of(steel),
                new ItemStackTemplate(steelDouble)), null);

        // Spec 11.7: iron transmission for speeds past the wooden 64 RPM.
        Item ironRod = ModItems.WROUGHT_IRON_ROD.get(), ironAxle = Tier4Items.IRON_AXLE.get(), brassGear = ModItems.GEARS.get(Metal.BRASS).get();
        shaped(RecipeCategory.REDSTONE, ironAxle, 4)
                .pattern("R")
                .pattern("R")
                .pattern("R")
                .define('R', ironRod)
                .unlockedBy("has_wrought_iron_rod", has(ironRod))
                .save(output, key("iron_axle"));
        shaped(RecipeCategory.REDSTONE, Tier4Items.IRON_GEARBOX.get())
                .pattern("PGP")
                .pattern("GAG")
                .pattern("PGP")
                .define('P', ModItems.PLATES.get(Metal.WROUGHT_IRON).get())
                .define('G', brassGear)
                .define('A', ironAxle)
                .unlockedBy("has_iron_axle", has(ironAxle))
                .save(output, key("iron_gearbox"));
        shapeless(RecipeCategory.REDSTONE, Tier4Items.IRON_STEP_UP_GEARBOX.get())
                .requires(Tier4Items.IRON_GEARBOX.get())
                .requires(brassGear, 2)
                .requires(ModItems.RODS.get(Metal.STEEL).get())
                .unlockedBy("has_iron_gearbox", has(Tier4Items.IRON_GEARBOX.get()))
                .save(output, key("iron_step_up_gearbox"));

        // Spec 8.1, 9.2, 9.3 and 10.2: steam. Solder joins copper and bronze pipe; steel pipe is welded.
        Item fireBrick = ModItems.FIRE_BRICK.get(), solder = ModItems.ingot(Metal.SOLDER);
        shaped(RecipeCategory.DECORATIONS, Tier4Items.FIREBOX.get())
                .pattern("BBB")
                .pattern("B B")
                .pattern("BPB")
                .define('B', fireBrick)
                .define('P', ModItems.PLATES.get(Metal.WROUGHT_IRON).get())
                .unlockedBy("has_fire_brick", has(fireBrick))
                .save(output, key("firebox"));
        Item copperPlate = ModItems.PLATES.get(Metal.COPPER).get();
        shaped(RecipeCategory.REDSTONE, Tier4Items.COPPER_FLUID_PIPE.get(), 4)
                .pattern("PSP")
                .define('P', copperPlate)
                .define('S', solder)
                .unlockedBy("has_solder_ingot", has(solder))
                .save(output, key("copper_fluid_pipe"));
        // Spec 8.2 and 8.4: heat pipes and the inlet.
        shaped(RecipeCategory.REDSTONE, Tier4Items.COPPER_HEAT_PIPE.get(), 4)
                .pattern("PSP")
                .define('P', copperPlate)
                .define('S', solder)
                .unlockedBy("has_firebox", has(Tier4Items.FIREBOX.get()))
                .save(output, key("copper_heat_pipe"));
        Item steelPlate = ModItems.PLATES.get(Metal.STEEL).get();
        shaped(RecipeCategory.REDSTONE, Tier4Items.REFRACTORY_HEAT_DUCT.get(), 2)
                .pattern("BPB")
                .define('B', fireBrick)
                .define('P', steelPlate)
                .unlockedBy("has_steel_plate", has(steelPlate))
                .save(output, key("refractory_heat_duct"));
        shapeless(RecipeCategory.REDSTONE, Tier4Items.HEAT_INLET.get())
                .requires(Tier4Items.REFRACTORY_HEAT_DUCT.get())
                .requires(steelPlate)
                .requires(ModItems.FIRE_BRICKS.get())
                .unlockedBy("has_refractory_heat_duct", has(Tier4Items.REFRACTORY_HEAT_DUCT.get()))
                .save(output, key("heat_inlet"));
        // Spec 8.2 and 8.6: wrapping pipes in slag wool, and the kiln that makes it.
        for (var pair : java.util.List.of(java.util.Map.entry(Tier4Items.COPPER_HEAT_PIPE, Tier4Items.INSULATED_COPPER_HEAT_PIPE),
                java.util.Map.entry(Tier4Items.REFRACTORY_HEAT_DUCT, Tier4Items.INSULATED_REFRACTORY_HEAT_DUCT))) {
            shapeless(RecipeCategory.REDSTONE, pair.getValue().get())
                    .requires(pair.getKey().get())
                    .requires(Tier4Items.SLAG_WOOL.get())
                    .unlockedBy("has_slag_wool", has(Tier4Items.SLAG_WOOL.get()))
                    .save(output, key(pair.getValue().getId().getPath()));
        }
        shaped(RecipeCategory.DECORATIONS, Tier4Items.KILN.get())
                .pattern("BBB")
                .pattern("B B")
                .pattern("FFF")
                .define('B', fireBrick)
                .define('F', ModItems.FIRE_BRICKS.get())
                .unlockedBy("has_firebox", has(Tier4Items.FIREBOX.get()))
                .save(output, key("kiln"));
        shaped(RecipeCategory.DECORATIONS, Tier4Items.ROASTER.get())
                .pattern("BPB")
                .pattern("B B")
                .pattern("BPB")
                .define('B', fireBrick)
                .define('P', ModItems.PLATES.get(Metal.COPPER).get())
                .unlockedBy("has_firebox", has(Tier4Items.FIREBOX.get()))
                .save(output, key("roaster"));
        shaped(RecipeCategory.DECORATIONS, Tier4Items.SMELTER.get())
                .pattern("FCF")
                .pattern("F F")
                .pattern("FPF")
                .define('F', ModItems.FIRE_BRICKS.get())
                .define('C', Tier4Items.REFRACTORY_CRUCIBLE.get())
                .define('P', ModItems.PLATES.get(Metal.STEEL).get())
                .unlockedBy("has_refractory_crucible", has(Tier4Items.REFRACTORY_CRUCIBLE.get()))
                .save(output, key("smelter"));
        shaped(RecipeCategory.REDSTONE, Tier4Items.BRONZE_FLUID_PIPE.get(), 4)
                .pattern("PSP")
                .define('P', ModTags.Items.ANY_BRONZE_PLATES)
                .define('S', solder)
                .unlockedBy("has_solder_ingot", has(solder))
                .save(output, key("bronze_fluid_pipe"));
        shaped(RecipeCategory.REDSTONE, Tier4Items.STEEL_FLUID_PIPE.get(), 4)
                .pattern("PPP")
                .define('P', steelPlate)
                .unlockedBy("has_steel_plate", has(steelPlate))
                .save(output, key("steel_fluid_pipe"));
        Item brassPlate = ModItems.PLATES.get(Metal.BRASS).get();
        shapeless(RecipeCategory.REDSTONE, Tier4Items.PRESSURE_GAUGE.get())
                .requires(brassPlate, 2)
                .requires(Items.GLASS)
                .requires(ModItems.RODS.get(Metal.BRASS).get())
                .unlockedBy("has_brass_plate", has(brassPlate))
                .save(output, key("pressure_gauge"));
        // Spec 9.3: the valve and the fluid tank.
        shapeless(RecipeCategory.REDSTONE, Tier4Items.VALVE.get())
                .requires(Tier4Items.BRONZE_FLUID_PIPE.get())
                .requires(brassPlate)
                .requires(ModItems.RODS.get(Metal.BRASS).get())
                .unlockedBy("has_brass_plate", has(brassPlate))
                .save(output, key("valve"));
        shaped(RecipeCategory.DECORATIONS, Tier4Items.FLUID_TANK.get())
                .pattern("PGP")
                .pattern("G G")
                .pattern("PGP")
                .define('P', ModItems.PLATES.get(Metal.COPPER).get())
                .define('G', Items.GLASS)
                .unlockedBy("has_copper_fluid_pipe", has(Tier4Items.COPPER_FLUID_PIPE.get()))
                .save(output, key("fluid_tank"));
        shaped(RecipeCategory.DECORATIONS, Tier4Items.BRONZE_BOILER.get())
                .pattern("PPP")
                .pattern("PGP")
                .pattern("PPP")
                .define('P', ModTags.Items.ANY_BRONZE_PLATES)
                .define('G', Tier4Items.PRESSURE_GAUGE.get())
                .unlockedBy("has_pressure_gauge", has(Tier4Items.PRESSURE_GAUGE.get()))
                .save(output, key("bronze_boiler"));
        // Spec 13.3 and 13.5: the chute and the filter.
        Item chuteIron = ModItems.PLATES.get(Metal.WROUGHT_IRON).get();
        shaped(RecipeCategory.REDSTONE, Tier4Items.CHUTE.get(), 2)
                .pattern("P P")
                .pattern("P P")
                .pattern(" P ")
                .define('P', chuteIron)
                .unlockedBy("has_wrought_iron_plate", has(chuteIron))
                .save(output, key("chute"));
        // Spec 13.4: the inserter.
        shaped(RecipeCategory.REDSTONE, Tier4Items.INSERTER.get())
                .pattern(" R ")
                .pattern(" G ")
                .pattern("PAP")
                .define('R', Tier4Items.TREATED_STICK.get())
                .define('G', ModItems.GEARS.get(Metal.BRASS).get())
                .define('P', chuteIron)
                .define('A', Tier4Items.IRON_AXLE.get())
                .unlockedBy("has_iron_axle", has(Tier4Items.IRON_AXLE.get()))
                .save(output, key("inserter"));
        // Spec 13.1: the conveyor belt.
        shaped(RecipeCategory.REDSTONE, Tier4Items.CONVEYOR_BELT.get(), 6)
                .pattern("LLL")
                .pattern("TGT")
                .define('L', Items.LEATHER)
                .define('T', Tier4Items.TREATED_PLANKS.get())
                .define('G', ModItems.GEARS.get(Metal.BRASS).get())
                .unlockedBy("has_brass_gear", has(ModItems.GEARS.get(Metal.BRASS).get()))
                .save(output, key("conveyor_belt"));
        // Spec 13.2: the belt diverter.
        shapeless(RecipeCategory.REDSTONE, Tier4Items.BELT_DIVERTER.get())
                .requires(Tier4Items.CONVEYOR_BELT.get())
                .requires(ModItems.GEARS.get(Metal.BRASS).get())
                .requires(ModItems.PLATES.get(Metal.BRASS).get())
                .unlockedBy("has_conveyor_belt", has(Tier4Items.CONVEYOR_BELT.get()))
                .save(output, key("belt_diverter"));
        shapeless(RecipeCategory.REDSTONE, Tier4Items.FILTER.get())
                .requires(Items.PAPER)
                .requires(ModItems.PLATES.get(Metal.BRASS).get())
                .requires(ModItems.TWINE.get())
                .unlockedBy("has_brass_plate", has(ModItems.PLATES.get(Metal.BRASS).get()))
                .save(output, key("filter"));
        // Spec 10.3: the steel boiler.
        Item boilerSteel = ModItems.PLATES.get(Metal.STEEL).get();
        shaped(RecipeCategory.DECORATIONS, Tier4Items.STEEL_BOILER_SHELL.get())
                .pattern("SW")
                .pattern("WS")
                .define('S', boilerSteel)
                .define('W', ModItems.PLATES.get(Metal.WROUGHT_IRON).get())
                .unlockedBy("has_steel_plate", has(boilerSteel))
                .save(output, key("steel_boiler_shell"));
        shaped(RecipeCategory.DECORATIONS, Tier4Items.BOILER_CONTROLLER.get())
                .pattern("PGP")
                .pattern("PVP")
                .pattern("PPP")
                .define('P', boilerSteel)
                .define('G', Tier4Items.PRESSURE_GAUGE.get())
                .define('V', Tier4Items.VALVE.get())
                .unlockedBy("has_steel_boiler_shell", has(Tier4Items.STEEL_BOILER_SHELL.get()))
                .save(output, key("boiler_controller"));
        shapeless(RecipeCategory.DECORATIONS, Tier4Items.BOILER_FLUID_PORT.get())
                .requires(Tier4Items.STEEL_BOILER_SHELL.get())
                .requires(Tier4Items.BRONZE_FLUID_PIPE.get())
                .unlockedBy("has_steel_boiler_shell", has(Tier4Items.STEEL_BOILER_SHELL.get()))
                .save(output, key("boiler_fluid_port"));
        shapeless(RecipeCategory.MISC, boilerSteel, 4)
                .requires(Tier4Items.CRACKED_BOILER_CONTROLLER.get())
                .unlockedBy("has_cracked_boiler_controller", has(Tier4Items.CRACKED_BOILER_CONTROLLER.get()))
                .save(output, key("steel_plate_from_cracked_boiler_controller"));
        shaped(RecipeCategory.REDSTONE, Tier4Items.MECHANICAL_PUMP.get())
                .pattern("PGP")
                .pattern(" A ")
                .pattern("PRP")
                .define('P', ModTags.Items.ANY_BRONZE_PLATES)
                .define('G', brassGear)
                .define('A', ironAxle)
                .define('R', Tier4Items.BRONZE_FLUID_PIPE.get())
                .unlockedBy("has_bronze_fluid_pipe", has(Tier4Items.BRONZE_FLUID_PIPE.get()))
                .save(output, key("mechanical_pump"));
        shaped(RecipeCategory.TOOLS, dev.strataindustria.mark.MarkRegistry.MAKER_PUNCH.get())
                .pattern("I")
                .pattern("I")
                .pattern("S")
                .define('I', ModItems.ingot(Metal.BRONZE))
                .define('S', Items.STICK)
                .unlockedBy("has_bronze_ingot", has(ModItems.ingot(Metal.BRONZE)))
                .save(output, key("maker_punch"));
        shapeless(RecipeCategory.TOOLS, dev.strataindustria.ledger.LedgerRegistry.BUILDERS_LEDGER.get())
                .requires(Items.BOOK)
                .requires(ModItems.ingot(Metal.BRONZE))
                .requires(ModItems.TWINE.get())
                .unlockedBy("has_bronze_ingot", has(ModItems.ingot(Metal.BRONZE)))
                .save(output, key("builders_ledger"));
        shaped(RecipeCategory.DECORATIONS, dev.strataindustria.ledger.LedgerRegistry.BUILDERS_CRATE_ITEM.get())
                .pattern("PPP")
                .pattern("PIP")
                .pattern("PPP")
                .define('P', net.minecraft.tags.ItemTags.PLANKS)
                .define('I', ModItems.ingot(Metal.BRONZE))
                .unlockedBy("has_bronze_ingot", has(ModItems.ingot(Metal.BRONZE)))
                .save(output, key("builders_crate"));
        shaped(RecipeCategory.DECORATIONS, dev.strataindustria.registry.TransportBlocks.OUTPOST_CHARTER_ITEM.get())
                .pattern(" A ")
                .pattern(" W ")
                .pattern(" S ")
                .define('A', Items.PAPER)
                .define('W', ModItems.PLATES.get(Metal.WROUGHT_IRON))
                .define('S', Items.STICK)
                .unlockedBy("has_wrought_iron_plate", has(ModItems.PLATES.get(Metal.WROUGHT_IRON)))
                .save(output, key("outpost_charter"));
        shaped(RecipeCategory.DECORATIONS, dev.strataindustria.cabinet.CabinetRegistry.SPECIMEN_CABINET_ITEM.get())
                .pattern("PGP")
                .pattern("PGP")
                .pattern("PPP")
                .define('P', net.minecraft.tags.ItemTags.PLANKS)
                .define('G', Items.GLASS_PANE)
                .unlockedBy("has_glass_pane", has(Items.GLASS_PANE))
                .save(output, key("specimen_cabinet"));
        shaped(RecipeCategory.DECORATIONS, dev.strataindustria.bronze.BronzeRegistry.FUME_HOOD_ITEM.get())
                .pattern("III")
                .pattern("I I")
                .define('I', ModItems.ingot(Metal.COPPER))
                .unlockedBy("has_copper_ingot", has(ModItems.ingot(Metal.COPPER)))
                .save(output, key("fume_hood"));
        shaped(RecipeCategory.REDSTONE, dev.strataindustria.listening.ListeningBlocks.STEAM_WHISTLE_ITEM.get())
                .pattern(" R ")
                .pattern("PRP")
                .pattern("PPP")
                .define('P', ModTags.Items.ANY_BRONZE_PLATES)
                .define('R', Tier4Items.BRONZE_FLUID_PIPE.get())
                .unlockedBy("has_bronze_fluid_pipe", has(Tier4Items.BRONZE_FLUID_PIPE.get()))
                .save(output, key("steam_whistle"));
        shaped(RecipeCategory.REDSTONE, Tier4Items.STEAM_ENGINE.get())
                .pattern("BGB")
                .pattern("PRP")
                .pattern("PPP")
                .define('B', brassPlate)
                .define('G', brassGear)
                .define('R', ModItems.RODS.get(Metal.STEEL).get())
                .define('P', ModItems.PLATES.get(Metal.WROUGHT_IRON).get())
                .unlockedBy("has_bronze_boiler", has(Tier4Items.BRONZE_BOILER.get()))
                .save(output, key("steam_engine"));
        // Spec 10.5: the steam hammer, around an iron anvil.
        shaped(RecipeCategory.REDSTONE, Tier4Items.STEAM_HAMMER.get())
                .pattern("PVP")
                .pattern(" R ")
                .pattern("IAI")
                .define('P', ModItems.PLATES.get(Metal.STEEL).get())
                .define('V', Tier4Items.VALVE.get())
                .define('R', ModItems.RODS.get(Metal.STEEL).get())
                .define('I', ModItems.PLATES.get(Metal.WROUGHT_IRON).get())
                .define('A', ModItems.IRON_ANVIL.get())
                .unlockedBy("has_steam_engine", has(Tier4Items.STEAM_ENGINE.get()))
                .save(output, key("steam_hammer"));
        // Spec 11.2: the crusher, and what it does better than a quern.
        shaped(RecipeCategory.REDSTONE, Tier4Items.CRUSHER.get())
                .pattern("PGP")
                .pattern("TAT")
                .pattern("PPP")
                .define('P', ModItems.PLATES.get(Metal.WROUGHT_IRON).get())
                .define('G', ModItems.GEARS.get(Metal.STEEL).get())
                .define('T', ModItems.PLATES.get(Metal.STEEL).get())
                .define('A', Tier4Items.IRON_AXLE.get())
                .unlockedBy("has_steam_engine", has(Tier4Items.STEAM_ENGINE.get()))
                .save(output, key("crusher"));
        crushing();
        // Spec 11.3: the washer, a treated wood tub with bronze bands.
        List<Item> bronzePlates = new java.util.ArrayList<>();
        for (Metal metal : Metal.values()) if (metal.isBronze() && ModItems.PLATES.containsKey(metal)) bronzePlates.add(ModItems.PLATES.get(metal).get());
        shaped(RecipeCategory.REDSTONE, Tier4Items.WASHER.get())
                .pattern("TPT")
                .pattern("TGT")
                .pattern("TRT")
                .define('T', Tier4Items.TREATED_PLANKS.get())
                .define('P', Ingredient.of(bronzePlates.toArray(Item[]::new)))
                .define('G', ModItems.GEARS.get(Metal.BRASS).get())
                .define('R', Tier4Items.BRONZE_FLUID_PIPE.get())
                .unlockedBy("has_crusher", has(Tier4Items.CRUSHER.get()))
                .save(output, key("washer"));
        blastFurnace();
        // Spec 10.4: a cracked boiler is good for four of its plates.
        shapeless(RecipeCategory.MISC, ModItems.PLATES.get(Metal.BRONZE).get(), 4)
                .requires(Tier4Items.CRACKED_BRONZE_BOILER.get())
                .unlockedBy("has_cracked_bronze_boiler", has(Tier4Items.CRACKED_BRONZE_BOILER.get()))
                .save(output, key("bronze_plate_from_cracked_boiler"));

        Item coke = Tier4Items.COKE.get();
        shaped(RecipeCategory.MISC, Tier4Items.COKE_BLOCK.get())
                .pattern("CCC")
                .pattern("CCC")
                .pattern("CCC")
                .define('C', coke)
                .unlockedBy("has_coke", has(coke))
                .save(output, key("coke_block"));
        shapeless(RecipeCategory.MISC, coke, 9)
                .requires(Tier4Items.COKE_BLOCK.get())
                .unlockedBy("has_coke_block", has(Tier4Items.COKE_BLOCK.get()))
                .save(output, key("coke_from_block"));
        grind("coke_dust", Ingredient.of(coke), Tier4Items.COKE_DUST.get(), 2);

        // Spec 5.2: creosote soaks eight planks; the bucket comes back.
        Item creosote = Tier4Items.CREOSOTE_BUCKET.get();
        shaped(RecipeCategory.BUILDING_BLOCKS, Tier4Items.TREATED_PLANKS.get(), 8)
                .pattern("PPP")
                .pattern("PCP")
                .pattern("PPP")
                .define('P', ItemTags.PLANKS)
                .define('C', creosote)
                .unlockedBy("has_creosote_bucket", has(creosote))
                .save(output, key("treated_planks"));
        Item treated = Tier4Items.TREATED_PLANKS.get();
        shaped(RecipeCategory.MISC, Tier4Items.TREATED_STICK.get(), 4)
                .pattern("P")
                .pattern("P")
                .define('P', treated)
                .unlockedBy("has_treated_planks", has(treated))
                .save(output, key("treated_stick"));
        shaped(RecipeCategory.BUILDING_BLOCKS, Tier4Items.TREATED_SLAB.get(), 6)
                .pattern("PPP")
                .define('P', treated)
                .unlockedBy("has_treated_planks", has(treated))
                .save(output, key("treated_slab"));
        shaped(RecipeCategory.BUILDING_BLOCKS, Tier4Items.TREATED_STAIRS.get(), 4)
                .pattern("P  ")
                .pattern("PP ")
                .pattern("PPP")
                .define('P', treated)
                .unlockedBy("has_treated_planks", has(treated))
                .save(output, key("treated_stairs"));
        shaped(RecipeCategory.DECORATIONS, Tier4Items.TREATED_FENCE.get(), 3)
                .pattern("PSP")
                .pattern("PSP")
                .define('P', treated)
                .define('S', Tier4Items.TREATED_STICK.get())
                .unlockedBy("has_treated_planks", has(treated))
                .save(output, key("treated_fence"));
    }

    private void ironAge() {
        int brick = GridPattern.parse(List.of("##.##", "##.##", ".....", "##.##", "##.##")).getOrThrow();
        output.accept(key("clay_forming/unfired_fire_brick"), new KnappingRecipe(Ingredient.of(ModItems.FIRE_CLAY_BALL.get()),
                Knapping.CLAY_OPENING_COST, CLAY_BLOWS, brick, new ItemStackTemplate(ModItems.UNFIRED_FIRE_BRICK.get(), 4)), null);
        shapeless(RecipeCategory.MISC, ModItems.FIRE_CLAY_BALL.get(), 2)
                .requires(Items.CLAY_BALL, 3)
                .requires(ModItems.GROG.get())
                .unlockedBy("has_grog", has(ModItems.GROG.get()))
                .save(output, key("fire_clay_ball_from_grog"));
        grind("grog", Ingredient.of(Items.BRICK), ModItems.GROG.get(), 2);

        Item fireBrick = ModItems.FIRE_BRICK.get();
        shaped(RecipeCategory.BUILDING_BLOCKS, ModItems.FIRE_BRICKS.get())
                .pattern("BB")
                .pattern("BB")
                .define('B', fireBrick)
                .unlockedBy("has_fire_brick", has(fireBrick))
                .save(output, key("fire_bricks"));
        Item bricks = ModItems.FIRE_BRICKS.get();
        shaped(RecipeCategory.BUILDING_BLOCKS, ModItems.FIRE_BRICK_SLAB.get(), 6)
                .pattern("BBB")
                .define('B', bricks)
                .unlockedBy("has_fire_bricks", has(bricks))
                .save(output, key("fire_brick_slab"));
        shaped(RecipeCategory.BUILDING_BLOCKS, ModItems.FIRE_BRICK_STAIRS.get(), 4)
                .pattern("B  ")
                .pattern("BB ")
                .pattern("BBB")
                .define('B', bricks)
                .unlockedBy("has_fire_bricks", has(bricks))
                .save(output, key("fire_brick_stairs"));
        shaped(RecipeCategory.DECORATIONS, ModItems.FIRE_BRICK_WALL.get(), 6)
                .pattern("BBB")
                .pattern("BBB")
                .define('B', bricks)
                .unlockedBy("has_fire_bricks", has(bricks))
                .save(output, key("fire_brick_wall"));

        // Spec 5.1: the bloomery controller, with a copper or bronze plate for the door.
        shaped(RecipeCategory.DECORATIONS, ModItems.BLOOMERY.get())
                .pattern("FFF")
                .pattern("F F")
                .pattern("FPF")
                .define('F', fireBrick)
                .define('P', ModTags.Items.SOFT_METAL_PLATES)
                .unlockedBy("has_fire_brick", has(fireBrick))
                .save(output, key("bloomery"));
        // Spec 9.3: hammering the slag out of a bloom. Partial blooms give nuggets instead (AnvilRecipe#assemble).
        anvil("bloom_refining", ModItems.RAW_BLOOM.get(), 1, Items.IRON_INGOT, 6);

        // Spec 9.4: flux from the quern, welding, and the iron anvil.
        grind("flux_from_sand", Ingredient.of(Items.SAND), ModItems.FLUX.get(), 2);
        grind("flux_from_limestone", Ingredient.of(ModItems.ROCK_SHARD.get(Rock.LIMESTONE).get()), ModItems.FLUX.get(), 4);
        grind("flux_from_marble", Ingredient.of(ModItems.ROCK_SHARD.get(Rock.MARBLE).get()), ModItems.FLUX.get(), 4);
        output.accept(key("welding/wrought_iron_double_ingot"), new WeldingRecipe(Ingredient.of(Items.IRON_INGOT),
                Ingredient.of(Items.IRON_INGOT), new ItemStackTemplate(ModItems.WROUGHT_IRON_DOUBLE_INGOT.get())), null);
        // Two anvils: the iron anvil is built straight from ingots, so it needs no welding to get started. It
        // is the anvil that works wrought iron and steel.
        shaped(RecipeCategory.DECORATIONS, ModItems.IRON_ANVIL.get())
                .pattern("III")
                .pattern(" I ")
                .pattern("III")
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                .save(output, key("iron_anvil"));

        kinetics();

        // Spec 2: the furnace returns in tier 3, built from fire bricks.
        shaped(RecipeCategory.DECORATIONS, Items.FURNACE)
                .pattern("FFF")
                .pattern("F F")
                .pattern("FFF")
                .define('F', fireBrick)
                .unlockedBy("has_fire_brick", has(fireBrick))
                .save(output.withConditions(new ConfigCondition("vanilla.gateFurnace", true)), key("furnace_from_fire_bricks"));
    }

    // Spec 14.3: mechanical power and the first machines.
    private void kinetics() {
        Item axle = ModItems.WOODEN_AXLE.get(), gear = ModItems.WOODEN_GEAR.get(), rod = ModItems.WROUGHT_IRON_ROD.get();
        shaped(RecipeCategory.REDSTONE, axle, 4)
                .pattern("P")
                .pattern("P")
                .pattern("P")
                .define('P', ItemTags.PLANKS)
                .unlockedBy("has_planks", has(ItemTags.PLANKS))
                .save(output, key("wooden_axle"));
        shaped(RecipeCategory.MISC, gear, 2)
                .pattern(" S ")
                .pattern("SPS")
                .pattern(" S ")
                .define('S', Items.STICK)
                .define('P', ItemTags.PLANKS)
                .unlockedBy("has_wooden_axle", has(axle))
                .save(output, key("wooden_gear"));
        shaped(RecipeCategory.REDSTONE, ModItems.WOODEN_GEARBOX.get())
                .pattern("PGP")
                .pattern("GAG")
                .pattern("PGP")
                .define('P', ItemTags.PLANKS)
                .define('G', gear)
                .define('A', axle)
                .unlockedBy("has_wooden_gear", has(gear))
                .save(output, key("wooden_gearbox"));
        shapeless(RecipeCategory.REDSTONE, ModItems.HAND_CRANK.get())
                .requires(axle)
                .requires(Items.STICK, 2)
                .requires(ItemTags.PLANKS)
                .unlockedBy("has_wooden_axle", has(axle))
                .save(output, key("hand_crank"));
        shaped(RecipeCategory.REDSTONE, ModItems.WATER_WHEEL.get())
                .pattern("PSP")
                .pattern("SRS")
                .pattern("PSP")
                .define('P', ItemTags.PLANKS)
                .define('S', Items.STICK)
                .define('R', rod)
                .unlockedBy("has_wrought_iron_rod", has(rod))
                .save(output, key("water_wheel"));
        shaped(RecipeCategory.REDSTONE, ModItems.MILLSTONE.get())
                .pattern(" A ")
                .pattern("QGQ")
                .pattern("PPP")
                .define('A', axle)
                .define('Q', ModItems.QUERNSTONE.get())
                .define('G', gear)
                .define('P', ItemTags.PLANKS)
                .unlockedBy("has_wooden_gear", has(gear))
                .save(output, key("millstone"));
        // Leather comes from tanning later in tier 3; until then vanilla leather does.
        shaped(RecipeCategory.REDSTONE, ModItems.BELLOWS.get())
                .pattern("PPP")
                .pattern("LLL")
                .pattern("PRP")
                .define('P', ItemTags.PLANKS)
                .define('L', Items.LEATHER)
                .define('R', rod)
                .unlockedBy("has_wrought_iron_rod", has(rod))
                .save(output, key("bellows"));

        // Spec 8.2 and 8.4: the saw mill and the trip hammer.
        shaped(RecipeCategory.REDSTONE, ModItems.SAW_MILL.get())
                .pattern("PRP")
                .pattern("GAG")
                .pattern("PPP")
                .define('P', ItemTags.PLANKS)
                .define('R', rod)
                .define('G', gear)
                .define('A', axle)
                .unlockedBy("has_wooden_gear", has(gear))
                .save(output, key("saw_mill"));
        // Any hammer head will do, stone included, and sticks carry it, so the first trip hammer needs no iron.
        List<Item> hammerHeads = new java.util.ArrayList<>();
        hammerHeads.add(ModItems.STONE_HAMMER_HEAD.get());
        for (Metal metal : Metal.values()) {
            if ((metal.isBronze() || metal == Metal.WROUGHT_IRON) && metal.toolTypes().contains(MoldType.HAMMER_HEAD)) {
                hammerHeads.add(ModItems.head(metal, MoldType.HAMMER_HEAD));
            }
        }
        shaped(RecipeCategory.REDSTONE, ModItems.TRIP_HAMMER.get())
                .pattern(" H ")
                .pattern("SAS")
                .pattern("PGP")
                .define('H', Ingredient.of(hammerHeads.toArray(Item[]::new)))
                .define('S', Items.STICK)
                .define('A', axle)
                .define('P', ItemTags.PLANKS)
                .define('G', gear)
                .unlockedBy("has_wooden_gear", has(gear))
                .save(output, key("trip_hammer"));
        // Spec 8.5: the core sampler.
        shaped(RecipeCategory.TOOLS, ModItems.CORE_SAMPLER.get())
                .pattern("PAP")
                .pattern("RGR")
                .pattern(" R ")
                .define('P', ItemTags.PLANKS)
                .define('A', axle)
                .define('R', rod)
                .define('G', gear)
                .unlockedBy("has_wooden_gear", has(gear))
                .save(output, key("core_sampler"));
        washing();
        // Spec 7.2 and 7.3: wind, gearing and belts.
        shapeless(RecipeCategory.REDSTONE, ModItems.STEP_UP_GEARBOX.get())
                .requires(ModItems.WOODEN_GEARBOX.get())
                .requires(gear, 2)
                .requires(rod)
                .unlockedBy("has_wooden_gearbox", has(ModItems.WOODEN_GEARBOX.get()))
                .save(output, key("step_up_gearbox"));
        shaped(RecipeCategory.REDSTONE, ModItems.PULLEY.get())
                .pattern(" P ")
                .pattern("PAP")
                .pattern(" P ")
                .define('P', ItemTags.PLANKS)
                .define('A', axle)
                .unlockedBy("has_wooden_axle", has(axle))
                .save(output, key("pulley"));
        shapeless(RecipeCategory.REDSTONE, ModItems.LEATHER_BELT.get())
                .requires(Items.LEATHER, 3)
                .requires(ModItems.TWINE.get())
                .unlockedBy("has_pulley", has(ModItems.PULLEY.get()))
                .save(output, key("leather_belt"));
        Item ironPlate = ModItems.PLATES.get(Metal.WROUGHT_IRON).get();
        shaped(RecipeCategory.REDSTONE, ModItems.WINDMILL_BEARING.get())
                .pattern("PIP")
                .pattern(" A ")
                .pattern("PIP")
                .define('P', ItemTags.PLANKS)
                .define('I', ironPlate)
                .define('A', axle)
                .unlockedBy("has_wrought_iron_plate", has(ironPlate))
                .save(output, key("windmill_bearing"));
        shapeless(RecipeCategory.REDSTONE, ModItems.WINDMILL_SAIL.get(), 2)
                .requires(ItemTags.PLANKS)
                .requires(ItemTags.PLANKS)
                .requires(ModItems.FIBRE_CLOTH.get())
                .unlockedBy("has_windmill_bearing", has(ModItems.WINDMILL_BEARING.get()))
                .save(output, key("windmill_sail"));

        tanning();

        Item bark = ModItems.BARK.get();
        saw("oak_planks", ItemTags.OAK_LOGS, Items.OAK_PLANKS, bark);
        saw("spruce_planks", ItemTags.SPRUCE_LOGS, Items.SPRUCE_PLANKS, bark);
        saw("birch_planks", ItemTags.BIRCH_LOGS, Items.BIRCH_PLANKS, bark);
        saw("jungle_planks", ItemTags.JUNGLE_LOGS, Items.JUNGLE_PLANKS, bark);
        saw("acacia_planks", ItemTags.ACACIA_LOGS, Items.ACACIA_PLANKS, bark);
        saw("dark_oak_planks", ItemTags.DARK_OAK_LOGS, Items.DARK_OAK_PLANKS, bark);
        saw("mangrove_planks", ItemTags.MANGROVE_LOGS, Items.MANGROVE_PLANKS, bark);
        saw("cherry_planks", ItemTags.CHERRY_LOGS, Items.CHERRY_PLANKS, bark);
        saw("pale_oak_planks", ItemTags.PALE_OAK_LOGS, Items.PALE_OAK_PLANKS, bark);
        saw("poplar_planks", ItemTags.POPLAR_LOGS, Items.POPLAR_PLANKS, bark);
        // Nether stems have no bark worth tanning with.
        saw("crimson_planks", ItemTags.CRIMSON_STEMS, Items.CRIMSON_PLANKS, null);
        saw("warped_planks", ItemTags.WARPED_STEMS, Items.WARPED_PLANKS, null);
        output.accept(key("sawing/bamboo_planks"), new SawingRecipe(Ingredient.of(Items.BAMBOO_BLOCK, Items.STRIPPED_BAMBOO_BLOCK),
                new ItemStackTemplate(Items.BAMBOO_PLANKS, 3), java.util.Optional.empty(), SawingRecipe.DEFAULT_TICKS), null);
        output.accept(key("sawing/sticks"), new SawingRecipe(tag(ItemTags.PLANKS), new ItemStackTemplate(Items.STICK, 3),
                java.util.Optional.empty(), SawingRecipe.DEFAULT_TICKS / 2), null);
    }

    /** A log gives six planks and, for overworld wood, a strip of bark (spec 8.2). */
    private void saw(String path, TagKey<Item> logs, Item planks, @org.jspecify.annotations.Nullable Item bark) {
        output.accept(key("sawing/" + path), new SawingRecipe(tag(logs), new ItemStackTemplate(planks, 6),
                java.util.Optional.ofNullable(bark).map(ItemStackTemplate::new), SawingRecipe.DEFAULT_TICKS), null);
    }

    private void anvil(String path, Item input, int count, Item result, int blows) {
        output.accept(key("anvil/" + path), new AnvilRecipe(Ingredient.of(input), count, new ItemStackTemplate(result), blows), null);
    }

    // Spec 10.1: the quern, and what it grinds.
    private void quern() {
        shaped(RecipeCategory.DECORATIONS, ModItems.QUERN.get())
                .pattern("S")
                .pattern("Q")
                .pattern("Q")
                .define('S', Items.STICK)
                .define('Q', ModItems.QUERNSTONE.get())
                .unlockedBy("has_quernstone", has(ModItems.QUERNSTONE.get()))
                .save(output, key("quern"));
        for (OreMineral mineral : OreMineral.withPieces()) {
            for (OreGrade grade : OreGrade.values()) {
                Item crushed = ModItems.crushedOre(mineral, grade);
                grind(name(crushed), Ingredient.of(ModItems.orePiece(mineral, grade)), crushed, 1);
            }
        }
        grind("bone_meal", Ingredient.of(Items.BONE), Items.BONE_MEAL, 4);
        // Tier 4 spec 3: carbon and sulfur dusts.
        grind("charcoal_dust", Ingredient.of(Items.CHARCOAL), dev.strataindustria.registry.Tier4Items.CHARCOAL_DUST.get(), 2);
        grind("sulfur_dust", Ingredient.of(dev.strataindustria.registry.Tier4Items.SULFUR.get()), dev.strataindustria.registry.Tier4Items.SULFUR_DUST.get(), 1);
        shapeless(RecipeCategory.MISC, Items.GUNPOWDER, 2)
                .requires(dev.strataindustria.registry.Tier4Items.SULFUR.get(), 2)
                .requires(dev.strataindustria.registry.Tier4Items.CHARCOAL_DUST.get())
                .unlockedBy("has_sulfur", has(dev.strataindustria.registry.Tier4Items.SULFUR.get()))
                .save(output, key("gunpowder_from_sulfur"));
        // Every vanilla flower that crafts into a dye gives two of it.
        flower(Items.DANDELION, Items.DYE.pick(DyeColor.YELLOW));
        flower(Items.POPPY, Items.DYE.pick(DyeColor.RED));
        flower(Items.BLUE_ORCHID, Items.DYE.pick(DyeColor.LIGHT_BLUE));
        flower(Items.ALLIUM, Items.DYE.pick(DyeColor.MAGENTA));
        flower(Items.AZURE_BLUET, Items.DYE.pick(DyeColor.LIGHT_GRAY));
        flower(Items.RED_TULIP, Items.DYE.pick(DyeColor.RED));
        flower(Items.ORANGE_TULIP, Items.DYE.pick(DyeColor.ORANGE));
        flower(Items.WHITE_TULIP, Items.DYE.pick(DyeColor.LIGHT_GRAY));
        flower(Items.PINK_TULIP, Items.DYE.pick(DyeColor.PINK));
        flower(Items.OXEYE_DAISY, Items.DYE.pick(DyeColor.LIGHT_GRAY));
        flower(Items.CORNFLOWER, Items.DYE.pick(DyeColor.BLUE));
        flower(Items.LILY_OF_THE_VALLEY, Items.DYE.pick(DyeColor.WHITE));
        flower(Items.WITHER_ROSE, Items.DYE.pick(DyeColor.BLACK));
        flower(Items.SUNFLOWER, Items.DYE.pick(DyeColor.YELLOW));
        flower(Items.LILAC, Items.DYE.pick(DyeColor.MAGENTA));
        flower(Items.ROSE_BUSH, Items.DYE.pick(DyeColor.RED));
        flower(Items.PEONY, Items.DYE.pick(DyeColor.PINK));
        flower(Items.TORCHFLOWER, Items.DYE.pick(DyeColor.ORANGE));
        flower(Items.PITCHER_PLANT, Items.DYE.pick(DyeColor.CYAN));
        flower(Items.PINK_PETALS, Items.DYE.pick(DyeColor.PINK));
        flower(Items.CLOSED_EYEBLOSSOM, Items.DYE.pick(DyeColor.GRAY));
        flower(Items.OPEN_EYEBLOSSOM, Items.DYE.pick(DyeColor.ORANGE));
        flower(Items.WILDFLOWERS, Items.DYE.pick(DyeColor.YELLOW));
        flower(Items.CACTUS_FLOWER, Items.DYE.pick(DyeColor.PINK));
    }

    private void flower(Item flower, Item dye) {
        grind(name(dye) + "_from_" + name(flower), Ingredient.of(flower), dye, 2);
    }

    /** Tier 3 spec 11: the pan, the sluice, and the washing recipes they share. */
    private void washing() {
        List<Item> panPlates = new java.util.ArrayList<>();
        for (Metal metal : Metal.values()) {
            if ((metal == Metal.COPPER || metal.isBronze()) && ModItems.PLATES.containsKey(metal)) panPlates.add(ModItems.PLATES.get(metal).get());
        }
        shapeless(RecipeCategory.TOOLS, ModItems.WASHING_PAN.get())
                .requires(Ingredient.of(panPlates.toArray(Item[]::new)))
                .requires(Items.STICK)
                .unlockedBy("has_placer_gravel", has(ModItems.PLACER_GRAVEL.get()))
                .unlockedBy("has_stick", has(Items.STICK))
                .save(output, key("washing_pan"));
        shaped(RecipeCategory.MISC, ModItems.SLUICE.get())
                .pattern("S  ")
                .pattern("PS ")
                .pattern("PPP")
                .define('S', Items.STICK)
                .define('P', ItemTags.PLANKS)
                .unlockedBy("has_washing_pan", has(ModItems.WASHING_PAN.get()))
                .save(output, key("sluice"));

        Map<OreMineral, Item> byproduct = new java.util.EnumMap<>(OreMineral.class);
        Map<OreMineral, Float> odds = new java.util.EnumMap<>(OreMineral.class);
        byproduct.put(OreMineral.NATIVE_COPPER, ModItems.SMALL_ORES.get(OreMineral.NATIVE_GOLD).get());
        odds.put(OreMineral.NATIVE_COPPER, 0.08f);
        byproduct.put(OreMineral.MALACHITE, Items.DYE.pick(DyeColor.GREEN));
        odds.put(OreMineral.MALACHITE, 0.15f);
        byproduct.put(OreMineral.TENNANTITE, ModItems.SMALL_ORES.get(OreMineral.BISMUTHINITE).get());
        odds.put(OreMineral.TENNANTITE, 0.15f);
        byproduct.put(OreMineral.CASSITERITE, ModItems.SMALL_ORES.get(OreMineral.MAGNETITE).get());
        odds.put(OreMineral.CASSITERITE, 0.15f);
        byproduct.put(OreMineral.BISMUTHINITE, ModItems.SMALL_ORES.get(OreMineral.NATIVE_GOLD).get());
        odds.put(OreMineral.BISMUTHINITE, 0.10f);
        byproduct.put(OreMineral.LIMONITE, Items.CLAY_BALL);
        odds.put(OreMineral.LIMONITE, 0.30f);
        byproduct.put(OreMineral.HEMATITE, Items.DYE.pick(DyeColor.RED));
        odds.put(OreMineral.HEMATITE, 0.15f);
        byproduct.put(OreMineral.MAGNETITE, ModItems.SMALL_ORES.get(OreMineral.NATIVE_COPPER).get());
        odds.put(OreMineral.MAGNETITE, 0.10f);
        // Tier 4 spec 11.3: galena gives up a little bismuthinite.
        byproduct.put(OreMineral.GALENA, ModItems.SMALL_ORES.get(OreMineral.BISMUTHINITE).get());
        odds.put(OreMineral.GALENA, 0.15f);
        for (OreMineral mineral : OreMineral.washableValues()) {
            for (OreGrade grade : OreGrade.values()) {
                List<WashingRecipe.Chance> chances = byproduct.containsKey(mineral)
                        ? List.of(new WashingRecipe.Chance(new ItemStackTemplate(byproduct.get(mineral)), odds.get(mineral)))
                        : List.of();
                Item washed = ModItems.washedOre(mineral, grade);
                output.accept(key("washing/" + name(washed)), new WashingRecipe(Ingredient.of(ModItems.crushedOre(mineral, grade)),
                        new ItemStackTemplate(washed), chances, WashingRecipe.DEFAULT_TICKS), null);
            }
        }
        Item gold = ModItems.SMALL_ORES.get(OreMineral.NATIVE_GOLD).get(), magnetite = ModItems.SMALL_ORES.get(OreMineral.MAGNETITE).get(),
                cassiterite = ModItems.SMALL_ORES.get(OreMineral.CASSITERITE).get();
        output.accept(key("washing/placer_gravel"), new WashingRecipe(Ingredient.of(ModItems.PLACER_GRAVEL.get()),
                new ItemStackTemplate(Items.GRAVEL), List.of(new WashingRecipe.Chance(new ItemStackTemplate(gold), 0.30f),
                new WashingRecipe.Chance(new ItemStackTemplate(magnetite), 0.20f), new WashingRecipe.Chance(new ItemStackTemplate(cassiterite), 0.10f)),
                WashingRecipe.DEFAULT_TICKS), null);
        output.accept(key("washing/placer_sand"), new WashingRecipe(Ingredient.of(ModItems.PLACER_SAND.get()),
                new ItemStackTemplate(Items.SAND), List.of(new WashingRecipe.Chance(new ItemStackTemplate(gold), 0.25f),
                new WashingRecipe.Chance(new ItemStackTemplate(magnetite), 0.25f), new WashingRecipe.Chance(new ItemStackTemplate(cassiterite), 0.05f)),
                WashingRecipe.DEFAULT_TICKS), null);
        // Redesign R7: black sand under a tin vein is mostly cassiterite and magnetite, the best tin there is by hand.
        output.accept(key("washing/black_sand"), new WashingRecipe(Ingredient.of(ModItems.BLACK_SAND.get()),
                new ItemStackTemplate(Items.SAND), List.of(new WashingRecipe.Chance(new ItemStackTemplate(cassiterite), 0.55f),
                new WashingRecipe.Chance(new ItemStackTemplate(magnetite), 0.25f)),
                WashingRecipe.DEFAULT_TICKS), null);
    }

    /** Tier 3 spec 12.1: the soaking barrel, tannin from bark and the one soak that turns a raw hide into leather. */
    private void tanning() {
        shaped(RecipeCategory.MISC, ModItems.SOAKING_BARREL.get())
                .pattern("P P")
                .pattern("P P")
                .pattern("PPP")
                .define('P', ItemTags.PLANKS)
                .unlockedBy("has_raw_hide", has(ModItems.RAW_HIDE.get()))
                .save(output, key("soaking_barrel"));
        FluidAmount water = new FluidAmount(Fluids.WATER, 1000);
        soak("tannin", Optional.of(Ingredient.of(ModItems.BARK.get())), 4, water,
                Optional.empty(), Optional.of(new FluidAmount(ModFluids.TANNIN.get(), 1000)), 2400);
        soak("leather", Optional.of(Ingredient.of(ModItems.RAW_HIDE.get())), 1, new FluidAmount(ModFluids.TANNIN.get(), 250),
                Optional.of(new ItemStackTemplate(Items.LEATHER, 2)), Optional.empty(), 6000);
    }

    private void soak(String path, Optional<Ingredient> input, int count, FluidAmount fluid, Optional<ItemStackTemplate> result,
                      Optional<FluidAmount> fluidResult, int ticks) {
        output.accept(key("barrel/" + path), new BarrelRecipe(input, count, Optional.of(fluid), result, fluidResult, ticks), null);
    }

    private void roast(String path, Item input, Item result, int ticks, int gas) {
        output.accept(key("roasting/" + path), new RoastingRecipe(Ingredient.of(input), new ItemStackTemplate(result), 800, ticks,
                Optional.of(new FluidAmount(Tier4Fluids.SULFUR_DIOXIDE.get(), gas))), null);
    }

    /**
     * Tier 4 spec 11.2 and 11.5: an ore piece gives its crushed piece, a 10% chance of a second, and the
     * crusher byproduct (doubled for rich ore, halved for poor). Rock goes to gravel, gravel to sand, and
     * carbonate rock to flux.
     */
    private void crushing() {
        Map<OreMineral, Item> byproduct = new java.util.EnumMap<>(OreMineral.class);
        byproduct.put(OreMineral.NATIVE_COPPER, ModItems.SMALL_ORES.get(OreMineral.MALACHITE).get());
        byproduct.put(OreMineral.MALACHITE, ModItems.SMALL_ORES.get(OreMineral.NATIVE_COPPER).get());
        byproduct.put(OreMineral.TENNANTITE, ModItems.SMALL_ORES.get(OreMineral.GALENA).get());
        byproduct.put(OreMineral.CASSITERITE, ModItems.SMALL_ORES.get(OreMineral.BISMUTHINITE).get());
        byproduct.put(OreMineral.BISMUTHINITE, ModItems.SMALL_ORES.get(OreMineral.GALENA).get());
        byproduct.put(OreMineral.GALENA, ModItems.crushedOre(OreMineral.SPHALERITE, OreGrade.POOR));
        byproduct.put(OreMineral.SPHALERITE, ModItems.crushedOre(OreMineral.GALENA, OreGrade.POOR));
        byproduct.put(OreMineral.LIMONITE, Items.CLAY_BALL);
        byproduct.put(OreMineral.HEMATITE, ModItems.SMALL_ORES.get(OreMineral.MAGNETITE).get());
        byproduct.put(OreMineral.MAGNETITE, ModItems.SMALL_ORES.get(OreMineral.HEMATITE).get());
        byproduct.put(OreMineral.NATIVE_GOLD, ModItems.SMALL_ORES.get(OreMineral.NATIVE_COPPER).get());
        for (OreMineral mineral : OreMineral.withPieces()) {
            for (OreGrade grade : OreGrade.values()) {
                Item crushed = ModItems.crushedOre(mineral, grade);
                List<WashingRecipe.Chance> chances = new java.util.ArrayList<>();
                chances.add(new WashingRecipe.Chance(new ItemStackTemplate(crushed), 0.10f));
                Item extra = byproduct.get(mineral);
                if (extra != null) {
                    float odds = grade == OreGrade.RICH ? 0.20f : grade == OreGrade.POOR ? 0.05f : 0.10f;
                    chances.add(new WashingRecipe.Chance(new ItemStackTemplate(extra), odds));
                }
                crush(name(crushed), Ingredient.of(ModItems.orePiece(mineral, grade)), new ItemStackTemplate(crushed), chances);
            }
        }
        for (Rock rock : Rock.values()) {
            Item cobbled = ModItems.COBBLED_ROCK.get(rock).get();
            boolean carbonate = rock == Rock.LIMESTONE || rock == Rock.MARBLE;
            ItemStackTemplate result = carbonate ? new ItemStackTemplate(ModItems.FLUX.get(), 8) : new ItemStackTemplate(Items.GRAVEL);
            crush((carbonate ? "flux_from_" : "gravel_from_") + name(cobbled), Ingredient.of(cobbled), result, List.of());
        }
        crush("sand_from_gravel", Ingredient.of(Items.GRAVEL), new ItemStackTemplate(Items.SAND), List.of());
    }

    /**
     * Tier 4 spec 12.1 and 11.6: the blast furnace parts and the blower. Until the heat network and the
     * chute exist, the tuyere's copper collar is a copper plate and the charging hatch takes a hopper.
     */
    private void blastFurnace() {
        Item plate = ModItems.PLATES.get(Metal.WROUGHT_IRON).get();
        Item casing = Tier4Items.REFRACTORY_CASING.get();
        shapeless(RecipeCategory.BUILDING_BLOCKS, casing)
                .requires(ModItems.FIRE_BRICKS.get())
                .requires(plate, 2)
                .unlockedBy("has_fire_bricks", has(ModItems.FIRE_BRICKS.get()))
                .save(output, key("refractory_casing"));
        shaped(RecipeCategory.DECORATIONS, Tier4Items.BLAST_FURNACE_CONTROLLER.get())
                .pattern("CGC")
                .pattern("CFC")
                .pattern("CCC")
                .define('C', casing)
                .define('G', Tier4Items.PRESSURE_GAUGE.get())
                .define('F', Tier4Items.FIREBOX.get())
                .unlockedBy("has_refractory_casing", has(casing))
                .save(output, key("blast_furnace_controller"));
        shapeless(RecipeCategory.DECORATIONS, Tier4Items.TUYERE.get())
                .requires(casing)
                .requires(Tier4Items.BRONZE_FLUID_PIPE.get())
                .requires(Tier4Items.COPPER_HEAT_PIPE.get())
                .unlockedBy("has_refractory_casing", has(casing))
                .save(output, key("tuyere"));
        shapeless(RecipeCategory.DECORATIONS, Tier4Items.CHARGING_HATCH.get())
                .requires(casing)
                .requires(Items.HOPPER)
                .unlockedBy("has_refractory_casing", has(casing))
                .save(output, key("charging_hatch"));
        shapeless(RecipeCategory.DECORATIONS, Tier4Items.TAP_HATCH.get())
                .requires(casing)
                .requires(plate)
                .requires(Items.IRON_BARS)
                .unlockedBy("has_refractory_casing", has(casing))
                .save(output, key("tap_hatch"));
        shaped(RecipeCategory.REDSTONE, Tier4Items.BLOWER.get())
                .pattern("PGP")
                .pattern("L L")
                .pattern("PAP")
                .define('P', plate)
                .define('G', ModItems.GEARS.get(Metal.BRASS).get())
                .define('L', Items.LEATHER)
                .define('A', Tier4Items.IRON_AXLE.get())
                .unlockedBy("has_iron_axle", has(Tier4Items.IRON_AXLE.get()))
                .save(output, key("blower"));
        // Spec 10.5: the blowing engine.
        shapeless(RecipeCategory.REDSTONE, Tier4Items.BLOWING_ENGINE.get())
                .requires(Tier4Items.STEAM_ENGINE.get())
                .requires(Tier4Items.BLOWER.get())
                .unlockedBy("has_blower", has(Tier4Items.BLOWER.get()))
                .save(output, key("blowing_engine"));
        shaped(RecipeCategory.DECORATIONS, Tier4Items.CONVERTER_CONTROLLER.get())
                .pattern("PGP")
                .pattern("PCP")
                .pattern("PPP")
                .define('P', ModItems.PLATES.get(Metal.STEEL).get())
                .define('G', Tier4Items.PRESSURE_GAUGE.get())
                .define('C', casing)
                .unlockedBy("has_blast_furnace_controller", has(Tier4Items.BLAST_FURNACE_CONTROLLER.get()))
                .save(output, key("converter_controller"));
        // Spec 11.2: the crusher gets three dust from slag, the quern two.
        crush("slag_dust_from_slag", Ingredient.of(Tier4Items.SLAG.get()), new ItemStackTemplate(Tier4Items.SLAG_DUST.get(), 3), List.of());
        grind("slag_dust_from_slag", Ingredient.of(Tier4Items.SLAG.get()), Tier4Items.SLAG_DUST.get(), 2);
    }

    private void crush(String path, Ingredient input, ItemStackTemplate result, List<WashingRecipe.Chance> chances) {
        output.accept(key("crushing/" + path), new dev.strataindustria.processing.CrushingRecipe(input, result, chances), null);
    }

    private void grind(String path, Ingredient input, Item result, int count) {
        output.accept(key("quern/" + path), new QuernRecipe(input, new ItemStackTemplate(result, count), QuernRecipe.DEFAULT_TICKS), null);
    }

    // Spec 6.1 and 8.1: nuggets, and metal tools from a cast head and a stick.
    private void metals() {
        for (Metal metal : Metal.values()) {
            if (!metal.hasIngot()) continue;
            Item ingot = ModItems.ingot(metal);
            if (!metal.isVanilla() && metal.hasNugget()) {
                Item nugget = ModItems.NUGGETS.get(metal).get();
                // Nine to the ingot, as with vanilla nuggets; a 3x3 grid has no room for ten.
                shapeless(RecipeCategory.MISC, nugget, 9)
                        .requires(ingot)
                        .unlockedBy("has_ingot", has(ingot))
                        .save(output, key(name(nugget) + "_from_ingot"));
                shapeless(RecipeCategory.MISC, ingot)
                        .requires(nugget, 9)
                        .unlockedBy("has_nugget", has(nugget))
                        .save(output, key(name(ingot) + "_from_nuggets"));
            }
            if (!metal.isToolMetal()) continue;
            for (MoldType type : metal.toolTypes()) {
                Item head = ModItems.head(metal, type);
                Item tool = ModItems.tool(metal, type);
                var recipe = new MetalToolRecipe(new Recipe.CommonInfo(true),
                        new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.EQUIPMENT, ""),
                        new ItemStackTemplate(tool),
                        List.of(Ingredient.of(head), Ingredient.of(Items.STICK)));
                save(key(name(tool)), recipe, RecipeCategory.TOOLS, "has_" + name(head), has(head));
            }
        }
    }

    private void save(ResourceKey<Recipe<?>> key, Recipe<?> recipe, RecipeCategory category, String criterion, Criterion<?> trigger) {
        var advancement = new RecipeUnlockAdvancementBuilder();
        advancement.unlockedBy(criterion, trigger);
        output.accept(key, recipe, advancement.build(output, key, category));
    }

    /**
     * Vanilla recipes that spec section 2 removes. Each is rewritten under its vanilla id with a
     * condition, so it only loads while its {@code vanilla.*} switch is off.
     */
    private void vanillaOverrides() {
        RecipeOutput woodTools = whenOff("vanilla.removeWoodTools");
        vanillaToolSet(woodTools, ItemTags.WOODEN_TOOL_MATERIALS, "has_planks",
                Items.WOODEN_PICKAXE, Items.WOODEN_AXE, Items.WOODEN_SHOVEL, Items.WOODEN_HOE, Items.WOODEN_SWORD, Items.WOODEN_SPEAR);
        RecipeOutput stoneTools = whenOff("vanilla.removeStoneTools");
        vanillaToolSet(stoneTools, ItemTags.STONE_TOOL_MATERIALS, "has_cobblestone",
                Items.STONE_PICKAXE, Items.STONE_AXE, Items.STONE_SHOVEL, Items.STONE_HOE, Items.STONE_SWORD, Items.STONE_SPEAR);

        // Copper armour comes from plates while the switch is on.
        RecipeOutput copperArmour = whenOff("vanilla.replaceCopperGear");
        Criterion<?> hasCopper = has(ItemTags.COPPER_TOOL_MATERIALS);
        shaped(RecipeCategory.COMBAT, Items.COPPER_HELMET).define('X', Items.COPPER_INGOT).pattern("XXX").pattern("X X")
                .unlockedBy("has_copper_ingot", hasCopper).save(copperArmour, vanillaKey(Items.COPPER_HELMET));
        shaped(RecipeCategory.COMBAT, Items.COPPER_CHESTPLATE).define('X', Items.COPPER_INGOT).pattern("X X").pattern("XXX").pattern("XXX")
                .unlockedBy("has_copper_ingot", hasCopper).save(copperArmour, vanillaKey(Items.COPPER_CHESTPLATE));
        shaped(RecipeCategory.COMBAT, Items.COPPER_LEGGINGS).define('X', Items.COPPER_INGOT).pattern("XXX").pattern("X X").pattern("X X")
                .unlockedBy("has_copper_ingot", hasCopper).save(copperArmour, vanillaKey(Items.COPPER_LEGGINGS));
        shaped(RecipeCategory.COMBAT, Items.COPPER_BOOTS).define('X', Items.COPPER_INGOT).pattern("X X").pattern("X X")
                .unlockedBy("has_copper_ingot", hasCopper).save(copperArmour, vanillaKey(Items.COPPER_BOOTS));

        // Copper tools come from cast heads; the copper spear has no mold and keeps its recipe.
        // Armour is handled with the plates.
        RecipeOutput copperTools = whenOff("vanilla.replaceCopperGear");
        vanillaToolSet(copperTools, ItemTags.COPPER_TOOL_MATERIALS, "has_copper_ingot",
                Items.COPPER_PICKAXE, Items.COPPER_AXE, Items.COPPER_SHOVEL, Items.COPPER_HOE, Items.COPPER_SWORD, null);

        RecipeOutput planks = whenOff("vanilla.planksNeedTools");
        vanillaPlanks(planks, ItemTags.OAK_LOGS, Items.OAK_PLANKS);
        vanillaPlanks(planks, ItemTags.SPRUCE_LOGS, Items.SPRUCE_PLANKS);
        vanillaPlanks(planks, ItemTags.BIRCH_LOGS, Items.BIRCH_PLANKS);
        vanillaPlanks(planks, ItemTags.JUNGLE_LOGS, Items.JUNGLE_PLANKS);
        vanillaPlanks(planks, ItemTags.ACACIA_LOGS, Items.ACACIA_PLANKS);
        vanillaPlanks(planks, ItemTags.DARK_OAK_LOGS, Items.DARK_OAK_PLANKS);
        vanillaPlanks(planks, ItemTags.MANGROVE_LOGS, Items.MANGROVE_PLANKS);
        vanillaPlanks(planks, ItemTags.CHERRY_LOGS, Items.CHERRY_PLANKS);
        vanillaPlanks(planks, ItemTags.PALE_OAK_LOGS, Items.PALE_OAK_PLANKS);
        vanillaPlanks(planks, ItemTags.POPLAR_LOGS, Items.POPLAR_PLANKS);
        vanillaPlanks(planks, ItemTags.CRIMSON_STEMS, Items.CRIMSON_PLANKS);
        vanillaPlanks(planks, ItemTags.WARPED_STEMS, Items.WARPED_PLANKS);

        shaped(RecipeCategory.DECORATIONS, Items.CAMPFIRE)
                .pattern(" S ")
                .pattern("SCS")
                .pattern("LLL")
                .define('L', ItemTags.LOGS_THAT_BURN)
                .define('S', Items.STICK)
                .define('C', ItemTags.COALS)
                .unlockedBy("has_stick", has(Items.STICK))
                .unlockedBy("has_coal", has(ItemTags.COALS))
                .save(whenOff("vanilla.removeCampfire"), vanillaKey(Items.CAMPFIRE));

        shaped(RecipeCategory.DECORATIONS, Items.FURNACE)
                .pattern("###")
                .pattern("# #")
                .pattern("###")
                .define('#', ItemTags.STONE_CRAFTING_MATERIALS)
                .unlockedBy("has_cobblestone", has(ItemTags.STONE_CRAFTING_MATERIALS))
                .save(whenOff("vanilla.gateFurnace"), vanillaKey(Items.FURNACE));

        // Tier 3 spec 2: iron and gold gear comes from smithing, casting and plates.
        RecipeOutput ironGear = whenOff("vanilla.replaceIronGear");
        vanillaToolSet(ironGear, ItemTags.IRON_TOOL_MATERIALS, "has_iron_ingot",
                Items.IRON_PICKAXE, Items.IRON_AXE, Items.IRON_SHOVEL, Items.IRON_HOE, Items.IRON_SWORD, null);
        vanillaArmour(ironGear, Items.IRON_INGOT, "has_iron_ingot",
                Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS);
        RecipeOutput goldGear = whenOff("vanilla.replaceGoldGear");
        vanillaToolSet(goldGear, ItemTags.GOLD_TOOL_MATERIALS, "has_gold_ingot",
                Items.GOLDEN_PICKAXE, Items.GOLDEN_AXE, Items.GOLDEN_SHOVEL, Items.GOLDEN_HOE, Items.GOLDEN_SWORD, null);
        vanillaArmour(goldGear, Items.GOLD_INGOT, "has_gold_ingot",
                Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS);

        SimpleCookingRecipeBuilder.smelting(tag(ItemTags.LOGS_THAT_BURN), RecipeCategory.MISC, CookingBookCategory.MISC, Items.CHARCOAL, 0.15f, 200)
                .unlockedBy("has_log", has(ItemTags.LOGS_THAT_BURN))
                .save(whenOff("vanilla.removeFurnaceCharcoal"), vanillaKey(Items.CHARCOAL));

        RecipeOutput oreSmelting = whenOff("vanilla.removeOreSmelting");
        vanillaOreSmelting(oreSmelting, Items.IRON_INGOT, 0.7f, Items.IRON_ORE, Items.DEEPSLATE_IRON_ORE, Items.RAW_IRON);
        vanillaOreSmelting(oreSmelting, Items.COPPER_INGOT, 0.7f, Items.COPPER_ORE, Items.DEEPSLATE_COPPER_ORE, Items.RAW_COPPER);
        vanillaOreSmelting(oreSmelting, Items.GOLD_INGOT, 1.0f, Items.GOLD_ORE, Items.DEEPSLATE_GOLD_ORE, Items.NETHER_GOLD_ORE,
                Items.RAW_GOLD);

        shaped(RecipeCategory.DECORATIONS, Items.BLAST_FURNACE)
                .pattern("III")
                .pattern("IXI")
                .pattern("###")
                .define('I', Items.IRON_INGOT)
                .define('X', Items.FURNACE)
                .define('#', Items.SMOOTH_STONE)
                .unlockedBy("has_smooth_stone", has(Items.SMOOTH_STONE))
                .save(whenOff("vanilla.removeBlastFurnace"), vanillaKey(Items.BLAST_FURNACE));
    }

    private RecipeOutput whenOff(String configKey) {
        return output.withConditions(new ConfigCondition(configKey, false));
    }

    private void vanillaToolSet(RecipeOutput out, TagKey<Item> material, String criterion,
            Item pickaxe, Item axe, Item shovel, Item hoe, Item sword, Item spear) {
        Criterion<?> has = has(material);
        shaped(RecipeCategory.TOOLS, pickaxe).define('#', Items.STICK).define('X', material)
                .pattern("XXX").pattern(" # ").pattern(" # ")
                .unlockedBy(criterion, has).save(out, vanillaKey(pickaxe));
        shaped(RecipeCategory.TOOLS, axe).define('#', Items.STICK).define('X', material)
                .pattern("XX").pattern("X#").pattern(" #")
                .unlockedBy(criterion, has).save(out, vanillaKey(axe));
        shaped(RecipeCategory.TOOLS, shovel).define('#', Items.STICK).define('X', material)
                .pattern("X").pattern("#").pattern("#")
                .unlockedBy(criterion, has).save(out, vanillaKey(shovel));
        shaped(RecipeCategory.TOOLS, hoe).define('#', Items.STICK).define('X', material)
                .pattern("XX").pattern(" #").pattern(" #")
                .unlockedBy(criterion, has).save(out, vanillaKey(hoe));
        shaped(RecipeCategory.COMBAT, sword).define('#', Items.STICK).define('X', material)
                .pattern("X").pattern("X").pattern("#")
                .unlockedBy(criterion, has).save(out, vanillaKey(sword));
        if (spear == null) return;
        shaped(RecipeCategory.COMBAT, spear).define('#', Items.STICK).define('X', material)
                .pattern("  X").pattern(" # ").pattern("#  ")
                .unlockedBy(criterion, has).save(out, vanillaKey(spear));
    }

    private void vanillaArmour(RecipeOutput out, Item ingot, String criterion, Item helmet, Item chestplate, Item leggings, Item boots) {
        Criterion<?> has = has(ingot);
        shaped(RecipeCategory.COMBAT, helmet).define('X', ingot).pattern("XXX").pattern("X X")
                .unlockedBy(criterion, has).save(out, vanillaKey(helmet));
        shaped(RecipeCategory.COMBAT, chestplate).define('X', ingot).pattern("X X").pattern("XXX").pattern("XXX")
                .unlockedBy(criterion, has).save(out, vanillaKey(chestplate));
        shaped(RecipeCategory.COMBAT, leggings).define('X', ingot).pattern("XXX").pattern("X X").pattern("X X")
                .unlockedBy(criterion, has).save(out, vanillaKey(leggings));
        shaped(RecipeCategory.COMBAT, boots).define('X', ingot).pattern("X X").pattern("X X")
                .unlockedBy(criterion, has).save(out, vanillaKey(boots));
    }

    /** The vanilla smelting and blasting recipes of one metal's ores and raw item, under their vanilla ids. */
    private void vanillaOreSmelting(RecipeOutput out, Item result, float experience, Item... inputs) {
        for (Item input : inputs) {
            SimpleCookingRecipeBuilder.smelting(Ingredient.of(input), RecipeCategory.MISC, CookingBookCategory.MISC, result, experience, 200)
                    .group(name(result))
                    .unlockedBy("has_" + name(input), has(input))
                    .save(out, ResourceKey.create(Registries.RECIPE, Identifier.withDefaultNamespace(
                            name(result) + "_from_smelting_" + name(input))));
            SimpleCookingRecipeBuilder.blasting(Ingredient.of(input), RecipeCategory.MISC, CookingBookCategory.MISC, result, experience, 100)
                    .group(name(result))
                    .unlockedBy("has_" + name(input), has(input))
                    .save(out, ResourceKey.create(Registries.RECIPE, Identifier.withDefaultNamespace(
                            name(result) + "_from_blasting_" + name(input))));
        }
    }

    private void vanillaPlanks(RecipeOutput out, TagKey<Item> logs, Item planks) {
        shapeless(RecipeCategory.BUILDING_BLOCKS, planks, 4)
                .requires(logs)
                .group("planks")
                .unlockedBy("has_logs", has(logs))
                .save(out, vanillaKey(planks));
    }

    private static String name(ItemLike item) {
        return item.asItem().builtInRegistryHolder().key().identifier().getPath();
    }

    private static ResourceKey<Recipe<?>> vanillaKey(ItemLike item) {
        return ResourceKey.create(Registries.RECIPE, Identifier.withDefaultNamespace(name(item)));
    }

    private static ResourceKey<Recipe<?>> key(String path) {
        return ResourceKey.create(Registries.RECIPE, StrataIndustria.id(path));
    }
}
