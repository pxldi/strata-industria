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
import net.minecraft.world.level.Level;

/**
 * A knapping recipe: strike cells out of a 5x5 grid of stone until exactly {@code pattern} is left.
 *
 * @param consume total number of material items the result costs; the first strike takes the
 *                opening cost, the rest is taken when the result is picked up
 */
public record KnappingRecipe(Ingredient ingredient, int consume, int pattern, boolean mirror, ItemStackTemplate result)
        implements Recipe<KnappingInput> {
    public static final MapCodec<KnappingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(KnappingRecipe::ingredient),
            Codec.intRange(1, 64).optionalFieldOf("consume", 1).forGetter(KnappingRecipe::consume),
            GridPattern.CODEC.fieldOf("pattern").forGetter(KnappingRecipe::pattern),
            Codec.BOOL.optionalFieldOf("mirror", true).forGetter(KnappingRecipe::mirror),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(KnappingRecipe::result)
    ).apply(i, KnappingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, KnappingRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, KnappingRecipe::ingredient,
            ByteBufCodecs.VAR_INT, KnappingRecipe::consume,
            ByteBufCodecs.INT, KnappingRecipe::pattern,
            ByteBufCodecs.BOOL, KnappingRecipe::mirror,
            ItemStackTemplate.STREAM_CODEC, KnappingRecipe::result,
            KnappingRecipe::new);

    public static final RecipeSerializer<KnappingRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    @Override
    public boolean matches(KnappingInput input, Level level) {
        if (!ingredient.test(input.material())) return false;
        return input.kept() == pattern || (mirror && input.kept() == GridPattern.mirror(pattern));
    }

    /** The head remembers what it was knapped from. */
    @Override
    public ItemStack assemble(KnappingInput input) {
        ItemStack out = result.create();
        Knapping.sourceOf(input.material()).ifPresent(source -> out.set(ModDataComponents.KNAPPED_FROM.get(), source));
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
