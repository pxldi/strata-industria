package dev.strataindustria.registry;

import dev.strataindustria.machine.SawingRecipe;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.crafting.KnappedToolRecipe;
import dev.strataindustria.crafting.MetalArmourRecipe;
import dev.strataindustria.crafting.MetalToolRecipe;
import dev.strataindustria.crafting.ToolShapelessRecipe;
import dev.strataindustria.knapping.KnappingRecipe;
import dev.strataindustria.quern.QuernRecipe;
import dev.strataindustria.smithing.AnvilRecipe;
import dev.strataindustria.smithing.WeldingRecipe;
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
    /** Shapeless metal tool assembly that carries the head's quality over and scales durability. */
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MetalToolRecipe>> METAL_TOOL =
            SERIALIZERS.register("metal_tool", () -> MetalToolRecipe.SERIALIZER);
    /** Shaped armour from plates that averages the plates' quality and scales durability. */
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MetalArmourRecipe>> METAL_ARMOUR =
            SERIALIZERS.register("metal_armour", () -> MetalArmourRecipe.SERIALIZER);

    /** Grinding in a quern (spec 10.1), hand or mechanical. */
    public static final DeferredHolder<RecipeType<?>, RecipeType<QuernRecipe>> QUERN =
            TYPES.register("quern", () -> RecipeType.simple(StrataIndustria.id("quern")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<QuernRecipe>> QUERN_SERIALIZER =
            SERIALIZERS.register("quern", () -> QuernRecipe.SERIALIZER);

    /** Anvil smithing (spec 9.2). */
    public static final DeferredHolder<RecipeType<?>, RecipeType<AnvilRecipe>> ANVIL =
            TYPES.register("anvil", () -> RecipeType.simple(StrataIndustria.id("anvil")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AnvilRecipe>> ANVIL_SERIALIZER =
            SERIALIZERS.register("anvil", () -> AnvilRecipe.SERIALIZER);

    /** Welding on the anvil (tier 3 spec 9.4). */
    public static final DeferredHolder<RecipeType<?>, RecipeType<WeldingRecipe>> WELDING =
            TYPES.register("welding", () -> RecipeType.simple(StrataIndustria.id("welding")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<WeldingRecipe>> WELDING_SERIALIZER =
            SERIALIZERS.register("welding", () -> WeldingRecipe.SERIALIZER);

    /** The saw mill (tier 3 spec 8.2). */
    public static final DeferredHolder<RecipeType<?>, RecipeType<SawingRecipe>> SAWING =
            TYPES.register("sawing", () -> RecipeType.simple(StrataIndustria.id("sawing")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SawingRecipe>> SAWING_SERIALIZER =
            SERIALIZERS.register("sawing", () -> SawingRecipe.SERIALIZER);

    private ModRecipes() {}
}
