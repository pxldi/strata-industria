package dev.strataindustria.machine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.strataindustria.registry.ModRecipes;
import java.util.Optional;
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
 * Sawing one item in the saw mill (spec 8.2): a log gives six planks and a strip of bark.
 *
 * @param extra a by-product, such as bark
 * @param ticks sawing time at 16 RPM
 */
public record SawingRecipe(Ingredient ingredient, ItemStackTemplate result, Optional<ItemStackTemplate> extra, int ticks)
        implements Recipe<SingleRecipeInput> {
    public static final int DEFAULT_TICKS = 40;

    public static final MapCodec<SawingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(SawingRecipe::ingredient),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(SawingRecipe::result),
            ItemStackTemplate.CODEC.optionalFieldOf("extra").forGetter(SawingRecipe::extra),
            Codec.intRange(1, 6000).optionalFieldOf("ticks", DEFAULT_TICKS).forGetter(SawingRecipe::ticks)
    ).apply(i, SawingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SawingRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, SawingRecipe::ingredient,
            ItemStackTemplate.STREAM_CODEC, SawingRecipe::result,
            ByteBufCodecs.optional(ItemStackTemplate.STREAM_CODEC), SawingRecipe::extra,
            ByteBufCodecs.VAR_INT, SawingRecipe::ticks,
            SawingRecipe::new);

    public static final RecipeSerializer<SawingRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input) {
        return result.create();
    }

    public ItemStack extraResult() {
        return extra.map(ItemStackTemplate::create).orElse(ItemStack.EMPTY);
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
    public RecipeSerializer<SawingRecipe> getSerializer() {
        return ModRecipes.SAWING_SERIALIZER.get();
    }

    @Override
    public RecipeType<SawingRecipe> getType() {
        return ModRecipes.SAWING.get();
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
