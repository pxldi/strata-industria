package dev.strataindustria.knapping;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.strataindustria.registry.ModDataComponents;
import dev.strataindustria.registry.ModRecipes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

/**
 * A hand-shaping recipe (knapping, clay forming, carving): hold the material, pick the shape, strike it
 * {@code blows} times and the result drops. The material is spent on the last blow, so a half-shaped piece
 * costs nothing.
 *
 * @param consume total number of material items the result costs (at least the material's own base cost)
 * @param blows   blows the shape takes
 * @param pattern silhouette of the result on a 5x5 grid, kept only for the JEI page and the journal art
 */
public record KnappingRecipe(Ingredient ingredient, int consume, int blows, int pattern, ItemStackTemplate result)
        implements Recipe<SingleRecipeInput> {
    public static final MapCodec<KnappingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(KnappingRecipe::ingredient),
            Codec.intRange(1, 64).optionalFieldOf("consume", 1).forGetter(KnappingRecipe::consume),
            Codec.intRange(1, 12).fieldOf("blows").forGetter(KnappingRecipe::blows),
            GridPattern.CODEC.fieldOf("pattern").forGetter(KnappingRecipe::pattern),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(KnappingRecipe::result)
    ).apply(i, KnappingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, KnappingRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, KnappingRecipe::ingredient,
            ByteBufCodecs.VAR_INT, KnappingRecipe::consume,
            ByteBufCodecs.VAR_INT, KnappingRecipe::blows,
            ByteBufCodecs.INT, KnappingRecipe::pattern,
            ItemStackTemplate.STREAM_CODEC, KnappingRecipe::result,
            KnappingRecipe::new);

    public static final RecipeSerializer<KnappingRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item());
    }

    /** The head remembers what it was knapped from. */
    @Override
    public ItemStack assemble(SingleRecipeInput input) {
        ItemStack out = result.create();
        Knapping.sourceOf(input.item()).ifPresent(source -> out.set(ModDataComponents.KNAPPED_FROM.get(), source));
        return out;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public RecipeSerializer<KnappingRecipe> getSerializer() {
        return ModRecipes.KNAPPING_SERIALIZER.get();
    }

    @Override
    public RecipeType<KnappingRecipe> getType() {
        return ModRecipes.KNAPPING.get();
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }
}
