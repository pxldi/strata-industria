package dev.strataindustria.smithing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.strataindustria.bloomery.BloomeryBlockEntity;
import dev.strataindustria.metal.Melt;
import dev.strataindustria.metal.MetalContent;
import dev.strataindustria.registry.ModDataComponents;
import dev.strataindustria.registry.ModRecipes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
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
 * A shape the anvil can strike: {@code blows} blows on {@code count} of the input make the result. Iron and
 * steel take a few more (see {@link Smithing#totalBlows}).
 */
public record AnvilRecipe(Ingredient ingredient, int count, ItemStackTemplate result, int blows)
        implements Recipe<SingleRecipeInput> {
    public static final MapCodec<AnvilRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(AnvilRecipe::ingredient),
            Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(AnvilRecipe::count),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(AnvilRecipe::result),
            Codec.intRange(1, 12).fieldOf("blows").forGetter(AnvilRecipe::blows)
    ).apply(i, AnvilRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AnvilRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, AnvilRecipe::ingredient,
            ByteBufCodecs.VAR_INT, AnvilRecipe::count,
            ItemStackTemplate.STREAM_CODEC, AnvilRecipe::result,
            ByteBufCodecs.VAR_INT, AnvilRecipe::blows,
            AnvilRecipe::new);

    public static final RecipeSerializer<AnvilRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input) {
        // Bloom refining (tier 3 spec 9.3): a full bloom gives the ingot, a partial one a nugget per 10 units.
        Melt bloom = input.item().get(ModDataComponents.BLOOM_CONTENTS.get());
        if (bloom != null && bloom.total() < BloomeryBlockEntity.BLOOM_UNITS) {
            return new ItemStack(Items.IRON_NUGGET, Math.max(1, bloom.total() / MetalContent.NUGGET_UNITS));
        }
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
    public RecipeSerializer<AnvilRecipe> getSerializer() {
        return ModRecipes.ANVIL_SERIALIZER.get();
    }

    @Override
    public RecipeType<AnvilRecipe> getType() {
        return ModRecipes.ANVIL.get();
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
