package dev.strataindustria.datagen;

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
import dev.strataindustria.material.Metal;
import dev.strataindustria.quern.QuernRecipe;
import dev.strataindustria.smithing.AnvilRecipe;
import dev.strataindustria.smithing.Rule;
import dev.strataindustria.smithing.WeldingRecipe;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.ModTags;
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
    ModRecipeProvider(BootstrapContext<Recipe<?>> recipeOutput, BootstrapContext<Advancement> advancementOutput) {
        super(recipeOutput, advancementOutput);
    }

    @Override
    protected void buildRecipes() {
        for (Rock rock : Rock.values()) {
            var loose = ModItems.LOOSE_ROCK.get(rock).get();
            var cobbled = ModItems.COBBLED_ROCK.get(rock).get();
            shaped(RecipeCategory.BUILDING_BLOCKS, cobbled)
                    .pattern("RR")
                    .pattern("RR")
                    .define('R', loose)
                    .unlockedBy("has_loose_rock", has(loose))
                    .save(output, key("cobbled_" + rock.id()));
            shapeless(RecipeCategory.MISC, loose, 4)
                    .requires(cobbled)
                    .unlockedBy("has_cobbled_rock", has(cobbled))
                    .save(output, key("loose_" + rock.id() + "_from_cobbled"));
        }

        knapping();
        clayForming();
        forge();
        fibre();
        stoneTools();
        fire();
        planks();
        metals();
        quern();
        smithing();
        ironAge();
        vanillaOverrides();
    }

    // Tier 0-2 spec 3.2. Patterns are written top row first; '#' is kept stone.
    private void knapping() {
        knap(ModItems.STONE_AXE_HEAD.get(), ".#...", "####.", "#####", "####.", ".#...");
        knap(ModItems.STONE_KNIFE_BLADE.get(), "#....", "##...", ".##..", "..##.", "...##");
        knap(ModItems.STONE_SHOVEL_HEAD.get(), ".###.", ".###.", ".###.", ".###.", "..#..");
        knap(ModItems.STONE_HOE_HEAD.get(), "#####", "##...", ".....", ".....", ".....");
        knap(ModItems.STONE_HAMMER_HEAD.get(), "#####", "#####", "..#..", ".....", ".....");
        knap(ModItems.STONE_SPEAR_HEAD.get(), "..#..", ".###.", ".###.", "..#..", "..#..");
        knap(ModItems.STONE_PICKAXE_HEAD.get(), ".###.", "#...#", ".....", ".....", ".....");
        // Spec 10.1: a round stone with a hole, worth four loose rocks.
        int quernstone = GridPattern.parse(List.of(".###.", "#####", "##.##", "#####", ".###.")).getOrThrow();
        output.accept(key("knapping/quernstone"), new KnappingRecipe(tag(ModTags.Items.LOOSE_ROCKS), 4, quernstone, false,
                new ItemStackTemplate(ModItems.QUERNSTONE.get())), null);
    }

    // Spec 4.1: the same grid, worked in five clay balls.
    private void clayForming() {
        form(ModItems.UNFIRED_SMALL_VESSEL.get(), 1, ".....", ".###.", "#####", "#####", ".###.");
        form(ModItems.UNFIRED_LARGE_VESSEL.get(), 1, ".###.", "#####", "#####", "#####", ".###.");
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

    private static Item mold(MoldType type) {
        return ModItems.UNFIRED_MOLDS.get(type).get();
    }

    private void form(Item result, int count, String... rows) {
        int pattern = GridPattern.parse(List.of(rows)).getOrThrow();
        var recipe = new KnappingRecipe(Ingredient.of(Items.CLAY_BALL), Knapping.CLAY_OPENING_COST, pattern, true,
                new ItemStackTemplate(result, count));
        output.accept(key("clay_forming/" + name(result)), recipe, null);
    }

    private void knap(Item result, String... rows) {
        int pattern = GridPattern.parse(List.of(rows)).getOrThrow();
        var recipe = new KnappingRecipe(tag(ModTags.Items.KNAPPABLE), 1, pattern, true, new ItemStackTemplate(result));
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
    }

    // Spec 3.5.
    private void fire() {
        shapeless(RecipeCategory.TOOLS, ModItems.FIRESTARTER.get())
                .requires(Items.STICK, 2)
                .requires(ModItems.TWINE.get())
                .unlockedBy("has_twine", has(ModItems.TWINE.get()))
                .save(output, key("firestarter"));
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
            anvil(m + "_plate", ingot, 1, ModItems.PLATES.get(metal).get(), 60,
                    rule(Rule.Kind.HIT, Rule.Where.LAST), rule(Rule.Kind.HIT, Rule.Where.SECOND_LAST), rule(Rule.Kind.HIT, Rule.Where.THIRD_LAST));
            anvil(m + "_pickaxe_head", ingot, 1, ModItems.head(metal, MoldType.PICKAXE_HEAD), 85,
                    rule(Rule.Kind.PUNCH, Rule.Where.LAST), rule(Rule.Kind.BEND, Rule.Where.NOT_LAST), rule(Rule.Kind.DRAW, Rule.Where.NOT_LAST));
            anvil(m + "_axe_head", ingot, 1, ModItems.head(metal, MoldType.AXE_HEAD), 75,
                    rule(Rule.Kind.PUNCH, Rule.Where.LAST), rule(Rule.Kind.HIT, Rule.Where.SECOND_LAST), rule(Rule.Kind.UPSET, Rule.Where.THIRD_LAST));
            anvil(m + "_shovel_head", ingot, 1, ModItems.head(metal, MoldType.SHOVEL_HEAD), 50,
                    rule(Rule.Kind.PUNCH, Rule.Where.LAST), rule(Rule.Kind.HIT, Rule.Where.NOT_LAST));
            anvil(m + "_hoe_head", ingot, 1, ModItems.head(metal, MoldType.HOE_HEAD), 65,
                    rule(Rule.Kind.PUNCH, Rule.Where.LAST), rule(Rule.Kind.HIT, Rule.Where.NOT_LAST), rule(Rule.Kind.BEND, Rule.Where.NOT_LAST));
            if (types.contains(MoldType.KNIFE_BLADE)) {
                anvil(m + "_knife_blade", ingot, 1, ModItems.head(metal, MoldType.KNIFE_BLADE), 95,
                        rule(Rule.Kind.HIT, Rule.Where.LAST), rule(Rule.Kind.DRAW, Rule.Where.SECOND_LAST), rule(Rule.Kind.DRAW, Rule.Where.THIRD_LAST));
                anvil(m + "_hammer_head", ingot, 1, ModItems.head(metal, MoldType.HAMMER_HEAD), 70,
                        rule(Rule.Kind.PUNCH, Rule.Where.LAST), rule(Rule.Kind.SHRINK, Rule.Where.NOT_LAST));
                anvil(m + "_saw_blade", ingot, 1, ModItems.head(metal, MoldType.SAW_BLADE), 55,
                        rule(Rule.Kind.HIT, Rule.Where.LAST), rule(Rule.Kind.HIT, Rule.Where.SECOND_LAST));
                anvil("tongs_jaw_from_" + m, ingot, 1, ModItems.TONGS_JAW.get(), 80,
                        rule(Rule.Kind.HIT, Rule.Where.LAST), rule(Rule.Kind.DRAW, Rule.Where.NOT_LAST));
            }
            // Tier 3 spec 9.3: a wrought iron sword blade is drawn from one welded double ingot.
            Item bladeStock = metal == Metal.WROUGHT_IRON ? ModItems.WROUGHT_IRON_DOUBLE_INGOT.get() : ingot;
            anvil(m + "_sword_blade", bladeStock, metal == Metal.WROUGHT_IRON ? 1 : 2, ModItems.head(metal, MoldType.SWORD_BLADE), 100,
                    rule(Rule.Kind.HIT, Rule.Where.LAST), rule(Rule.Kind.BEND, Rule.Where.SECOND_LAST), rule(Rule.Kind.BEND, Rule.Where.THIRD_LAST));
            if (metal == Metal.WROUGHT_IRON) {
                output.accept(key("anvil/wrought_iron_rod"), new AnvilRecipe(Ingredient.of(ingot), 1,
                        new ItemStackTemplate(ModItems.WROUGHT_IRON_ROD.get(), 2),
                        List.of(rule(Rule.Kind.DRAW, Rule.Where.LAST), rule(Rule.Kind.DRAW, Rule.Where.SECOND_LAST),
                                rule(Rule.Kind.HIT, Rule.Where.NOT_LAST)), 45), null);
            }
            if (ModItems.PROSPECTOR_HEADS.containsKey(metal)) {
                Item head = ModItems.PROSPECTOR_HEADS.get(metal).get();
                anvil(m + "_prospectors_pick_head", ingot, 1, head, 90,
                        rule(Rule.Kind.PUNCH, Rule.Where.LAST), rule(Rule.Kind.DRAW, Rule.Where.NOT_LAST), rule(Rule.Kind.HIT, Rule.Where.NOT_LAST));
                Item pick = ModItems.PROSPECTORS_PICKS.get(metal).get();
                save(key(name(pick)), new MetalToolRecipe(new Recipe.CommonInfo(true),
                        new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.EQUIPMENT, ""), new ItemStackTemplate(pick),
                        List.of(Ingredient.of(head), Ingredient.of(Items.STICK))), RecipeCategory.TOOLS, "has_" + name(head), has(head));
            }
            armour(metal);
        }
        shaped(RecipeCategory.DECORATIONS, ModItems.BRONZE_ANVIL.get())
                .pattern("PPP")
                .pattern(" I ")
                .pattern("III")
                .define('P', ModTags.Items.ANY_BRONZE_PLATES)
                .define('I', ModTags.Items.ANY_BRONZE_INGOTS)
                .unlockedBy("has_bronze_plate", has(ModTags.Items.ANY_BRONZE_PLATES))
                .save(output, key("bronze_anvil"));
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
        Item cloth = metal == Metal.WROUGHT_IRON || metal == Metal.GOLD ? Items.LEATHER : ModItems.FIBRE_CLOTH.get();
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
    private void ironAge() {
        int brick = GridPattern.parse(List.of("##.##", "##.##", ".....", "##.##", "##.##")).getOrThrow();
        output.accept(key("clay_forming/unfired_fire_brick"), new KnappingRecipe(Ingredient.of(ModItems.FIRE_CLAY_BALL.get()),
                Knapping.CLAY_OPENING_COST, brick, true, new ItemStackTemplate(ModItems.UNFIRED_FIRE_BRICK.get(), 4)), null);
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
        anvil("bloom_refining", ModItems.RAW_BLOOM.get(), 1, Items.IRON_INGOT, 60,
                rule(Rule.Kind.HIT, Rule.Where.LAST), rule(Rule.Kind.HIT, Rule.Where.SECOND_LAST), rule(Rule.Kind.HIT, Rule.Where.THIRD_LAST));

        // Spec 9.4: flux from the quern, welding, and the wrought iron anvil.
        grind("flux_from_sand", Ingredient.of(Items.SAND), ModItems.FLUX.get(), 2);
        grind("flux_from_limestone", Ingredient.of(ModItems.LOOSE_ROCK.get(Rock.LIMESTONE).get()), ModItems.FLUX.get(), 4);
        grind("flux_from_marble", Ingredient.of(ModItems.LOOSE_ROCK.get(Rock.MARBLE).get()), ModItems.FLUX.get(), 4);
        output.accept(key("welding/wrought_iron_double_ingot"), new WeldingRecipe(Ingredient.of(Items.IRON_INGOT),
                Ingredient.of(Items.IRON_INGOT), new ItemStackTemplate(ModItems.WROUGHT_IRON_DOUBLE_INGOT.get())), null);
        Item doubleIngot = ModItems.WROUGHT_IRON_DOUBLE_INGOT.get();
        shaped(RecipeCategory.DECORATIONS, ModItems.WROUGHT_IRON_ANVIL.get())
                .pattern("DDD")
                .pattern(" I ")
                .pattern("III")
                .define('D', doubleIngot)
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_double_ingot", has(doubleIngot))
                .save(output, key("wrought_iron_anvil"));

        // Spec 9.5: a blank pattern, and wiping a recorded one.
        Item pattern = ModItems.SMITHING_PATTERN.get();
        shapeless(RecipeCategory.MISC, pattern, 2)
                .requires(Items.PAPER, 2)
                .requires(Items.CHARCOAL)
                .unlockedBy("has_double_ingot", has(doubleIngot))
                .save(output, key("smithing_pattern"));
        shapeless(RecipeCategory.MISC, pattern)
                .requires(pattern)
                .unlockedBy("has_smithing_pattern", has(pattern))
                .save(output, key("smithing_pattern_wipe"));

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
    }

    private static Rule rule(Rule.Kind kind, Rule.Where where) {
        return Rule.of(kind, where);
    }

    private void anvil(String path, Item input, int count, Item result, int defaultTarget, Rule... rules) {
        output.accept(key("anvil/" + path), new AnvilRecipe(Ingredient.of(input), count, new ItemStackTemplate(result),
                List.of(rules), defaultTarget), null);
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
        for (OreMineral mineral : OreMineral.values()) {
            for (OreGrade grade : OreGrade.values()) {
                Item crushed = ModItems.crushedOre(mineral, grade);
                grind(name(crushed), Ingredient.of(ModItems.orePiece(mineral, grade)), crushed, 1);
            }
        }
        grind("bone_meal", Ingredient.of(Items.BONE), Items.BONE_MEAL, 4);
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
