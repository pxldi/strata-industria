package dev.strataindustria.registry;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.processing.AssemblingRecipe;
import dev.strataindustria.processing.ElectrolysisRecipe;
import dev.strataindustria.processing.MachiningRecipe;
import dev.strataindustria.processing.MixingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Tier 5 recipe types, registered into the shared recipe registers. */
public final class Tier5Recipes {
    /** Wiremill, bender and lathe (spec 10.4 to 10.6); the extruder joins them later. */
    public static final DeferredHolder<RecipeType<?>, RecipeType<MachiningRecipe>> MACHINING =
            ModRecipes.TYPES.register("machining", () -> RecipeType.simple(StrataIndustria.id("machining")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MachiningRecipe>> MACHINING_SERIALIZER =
            ModRecipes.SERIALIZERS.register("machining", () -> MachiningRecipe.SERIALIZER);

    /** The mixer and the electrolyser (spec 11.1 and 11.2). */
    public static final DeferredHolder<RecipeType<?>, RecipeType<MixingRecipe>> MIXING =
            ModRecipes.TYPES.register("mixing", () -> RecipeType.simple(StrataIndustria.id("mixing")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MixingRecipe>> MIXING_SERIALIZER =
            ModRecipes.SERIALIZERS.register("mixing", () -> MixingRecipe.SERIALIZER);
    public static final DeferredHolder<RecipeType<?>, RecipeType<ElectrolysisRecipe>> ELECTROLYSIS =
            ModRecipes.TYPES.register("electrolysis", () -> RecipeType.simple(StrataIndustria.id("electrolysis")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ElectrolysisRecipe>> ELECTROLYSIS_SERIALIZER =
            ModRecipes.SERIALIZERS.register("electrolysis", () -> ElectrolysisRecipe.SERIALIZER);

    /** The assembler (spec 10.7). */
    public static final DeferredHolder<RecipeType<?>, RecipeType<AssemblingRecipe>> ASSEMBLING =
            ModRecipes.TYPES.register("assembling", () -> RecipeType.simple(StrataIndustria.id("assembling")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AssemblingRecipe>> ASSEMBLING_SERIALIZER =
            ModRecipes.SERIALIZERS.register("assembling", () -> AssemblingRecipe.SERIALIZER);

    /** Loads the class so its entries join the registers before they fire. */
    public static void init() {}

    private Tier5Recipes() {}
}
