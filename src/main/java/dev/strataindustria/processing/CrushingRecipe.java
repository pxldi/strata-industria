package dev.strataindustria.processing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.strataindustria.registry.Tier4Recipes;
import dev.strataindustria.washing.WashingRecipe;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

/**
 * Crushing one item in a crusher (tier 4 spec 11.2): one main output and up to three chance outputs.
 * Where an item has both, this takes precedence over the quern recipe the crusher also runs.
 */
public record CrushingRecipe(Ingredient ingredient, ItemStackTemplate result, List<WashingRecipe.Chance> chances)
        implements Recipe<SingleRecipeInput> {
    public static final MapCodec<CrushingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(CrushingRecipe::ingredient),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(CrushingRecipe::result),
            WashingRecipe.Chance.CODEC.listOf(0, 3).optionalFieldOf("chances", List.of()).forGetter(CrushingRecipe::chances)
    ).apply(i, CrushingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CrushingRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, CrushingRecipe::ingredient,
            ItemStackTemplate.STREAM_CODEC, CrushingRecipe::result,
            WashingRecipe.Chance.STREAM_CODEC.apply(ByteBufCodecs.list(3)), CrushingRecipe::chances,
            CrushingRecipe::new);

    public static final RecipeSerializer<CrushingRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public CrushingRecipe {
        chances = List.copyOf(chances);
    }

    public static Optional<RecipeHolder<CrushingRecipe>> recipeFor(Level level, ItemStack stack) {
        if (!(level instanceof ServerLevel server) || stack.isEmpty()) return Optional.empty();
        return server.recipeAccess().getRecipeFor(Tier4Recipes.CRUSHING.get(), new SingleRecipeInput(stack), level);
    }

    public Processing processing() {
        return new Processing(List.of(result.create()), chances);
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input) {
        return result.create();
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
    public RecipeSerializer<CrushingRecipe> getSerializer() {
        return Tier4Recipes.CRUSHING_SERIALIZER.get();
    }

    @Override
    public RecipeType<CrushingRecipe> getType() {
        return Tier4Recipes.CRUSHING.get();
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
