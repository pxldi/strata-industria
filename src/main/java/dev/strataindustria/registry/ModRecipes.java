package dev.strataindustria.registry;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.crafting.KnappedToolRecipe;
import dev.strataindustria.crafting.ToolShapelessRecipe;
import dev.strataindustria.knapping.KnappingRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, StrataIndustria.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, StrataIndustria.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<KnappingRecipe>> KNAPPING =
            TYPES.register("knapping", () -> RecipeType.simple(StrataIndustria.id("knapping")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<KnappingRecipe>> KNAPPING_SERIALIZER =
            SERIALIZERS.register("knapping", () -> KnappingRecipe.SERIALIZER);

    /** Shapeless crafting that damages a tool ingredient instead of using it up (planks). */
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ToolShapelessRecipe>> TOOL_SHAPELESS =
            SERIALIZERS.register("tool_shapeless", () -> ToolShapelessRecipe.SERIALIZER);
    /** Shapeless tool assembly that carries the head's knapped_from over and scales durability. */
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<KnappedToolRecipe>> KNAPPED_TOOL =
            SERIALIZERS.register("knapped_tool", () -> KnappedToolRecipe.SERIALIZER);

    private ModRecipes() {}
}
