package dev.strataindustria.registry;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.processing.MachiningRecipe;
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

    /** Loads the class so its entries join the registers before they fire. */
    public static void init() {}

    private Tier5Recipes() {}
}
