package dev.strataindustria.quern;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
 * Grinding one item in a quern (spec 10.1). The hand quern and the tier 3 mechanical quern share this
 * recipe type.
 *
 * @param ticks grinding time for one input item
 */
public record QuernRecipe(Ingredient ingredient, ItemStackTemplate result, int ticks) implements Recipe<SingleRecipeInput> {
    public static final int DEFAULT_TICKS = 40;

    public static final MapCodec<QuernRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(QuernRecipe::ingredient),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(QuernRecipe::result),
            Codec.intRange(1, 6000).optionalFieldOf("ticks", DEFAULT_TICKS).forGetter(QuernRecipe::ticks)
    ).apply(i, QuernRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, QuernRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, QuernRecipe::ingredient,
            ItemStackTemplate.STREAM_CODEC, QuernRecipe::result,
            ByteBufCodecs.VAR_INT, QuernRecipe::ticks,
            QuernRecipe::new);

    public static final RecipeSerializer<QuernRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

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
    public RecipeSerializer<QuernRecipe> getSerializer() {
        return ModRecipes.QUERN_SERIALIZER.get();
    }

    @Override
    public RecipeType<QuernRecipe> getType() {
        return ModRecipes.QUERN.get();
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
