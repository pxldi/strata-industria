package dev.strataindustria.registry;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.processing.OilStillRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Tier 6 recipe types, registered into the shared recipe registers. */
public final class Tier6Recipes {
    /** The oil still (spec 5.5 and 17.4). */
    public static final DeferredHolder<RecipeType<?>, RecipeType<OilStillRecipe>> OIL_STILL =
            ModRecipes.TYPES.register("oil_still", () -> RecipeType.simple(StrataIndustria.id("oil_still")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<OilStillRecipe>> OIL_STILL_SERIALIZER =
            ModRecipes.SERIALIZERS.register("oil_still", () -> OilStillRecipe.SERIALIZER);

    /** Loads the class so its entries join the registers before they fire. */
    public static void init() {}

    private Tier6Recipes() {}
}
