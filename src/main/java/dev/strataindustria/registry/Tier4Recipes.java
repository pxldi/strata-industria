package dev.strataindustria.registry;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.processing.CrushingRecipe;
import dev.strataindustria.roasting.RoastingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Tier 4 recipe types, registered into the shared recipe registers. */
public final class Tier4Recipes {
    /** Roasting in a forge heating slot or a roaster (tier 4 spec 5.3). */
    public static final DeferredHolder<RecipeType<?>, RecipeType<RoastingRecipe>> ROASTING =
            ModRecipes.TYPES.register("roasting", () -> RecipeType.simple(StrataIndustria.id("roasting")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<RoastingRecipe>> ROASTING_SERIALIZER =
            ModRecipes.SERIALIZERS.register("roasting", () -> RoastingRecipe.SERIALIZER);

    /** Crushing in a crusher, ahead of the quern recipes it also runs (tier 4 spec 11.2). */
    public static final DeferredHolder<RecipeType<?>, RecipeType<CrushingRecipe>> CRUSHING =
            ModRecipes.TYPES.register("crushing", () -> RecipeType.simple(StrataIndustria.id("crushing")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CrushingRecipe>> CRUSHING_SERIALIZER =
            ModRecipes.SERIALIZERS.register("crushing", () -> CrushingRecipe.SERIALIZER);

    /** Loads the class so its entries join the registers before they fire. */
    public static void init() {}

    private Tier4Recipes() {}
}
