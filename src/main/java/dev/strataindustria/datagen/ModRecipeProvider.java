package dev.strataindustria.datagen;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.ceramics.MoldType;
import dev.strataindustria.crafting.ConfigCondition;
import dev.strataindustria.crafting.KnappedToolRecipe;
import dev.strataindustria.crafting.MetalToolRecipe;
import dev.strataindustria.crafting.ToolShapelessRecipe;
import dev.strataindustria.geology.Rock;
import dev.strataindustria.knapping.GridPattern;
import dev.strataindustria.knapping.Knapping;
import dev.strataindustria.knapping.KnappingRecipe;
import dev.strataindustria.material.Metal;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.ModTags;
import java.util.List;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.RecipeUnlockAdvancementBuilder;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
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
            for (MoldType type : MoldType.values()) {
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
