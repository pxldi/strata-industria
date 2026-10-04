package dev.strataindustria.smithing;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.strataindustria.registry.ModRecipes;
import net.minecraft.network.RegistryFriendlyByteBuf;
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
import net.minecraft.world.level.Level;

/**
 * A weld (tier 3 spec 9.4): two hot pieces and a pinch of flux, joined with one press of the Weld
 * button. Either piece can go in either slot.
 */
public record WeldingRecipe(Ingredient first, Ingredient second, ItemStackTemplate result) implements Recipe<WeldingInput> {
    public static final MapCodec<WeldingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC.fieldOf("first").forGetter(WeldingRecipe::first),
            Ingredient.CODEC.fieldOf("second").forGetter(WeldingRecipe::second),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(WeldingRecipe::result)
    ).apply(i, WeldingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, WeldingRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, WeldingRecipe::first,
            Ingredient.CONTENTS_STREAM_CODEC, WeldingRecipe::second,
            ItemStackTemplate.STREAM_CODEC, WeldingRecipe::result,
            WeldingRecipe::new);

    public static final RecipeSerializer<WeldingRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    @Override
    public boolean matches(WeldingInput input, Level level) {
        return first.test(input.first()) && second.test(input.second()) || first.test(input.second()) && second.test(input.first());
    }

    @Override
    public ItemStack assemble(WeldingInput input) {
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
    public RecipeSerializer<WeldingRecipe> getSerializer() {
        return ModRecipes.WELDING_SERIALIZER.get();
    }

    @Override
    public RecipeType<WeldingRecipe> getType() {
        return ModRecipes.WELDING.get();
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
