package dev.strataindustria.processing;

import com.mojang.serialization.MapCodec;
import dev.strataindustria.registry.Tier5Recipes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * A mixer recipe (tier 5 spec 11.1): up to 2 item and 3 fluid inputs, an optional item result and fluid result.
 * The machine reads {@link #io()} directly and walks every recipe of the type, so the recipe input is empty.
 */
public record MixingRecipe(ChemicalIo io) implements Recipe<ChemicalRecipe.Input>, ChemicalRecipe {
    public static final MapCodec<MixingRecipe> MAP_CODEC = ChemicalIo.MAP_CODEC.xmap(MixingRecipe::new, MixingRecipe::io);
    public static final StreamCodec<RegistryFriendlyByteBuf, MixingRecipe> STREAM_CODEC = ChemicalIo.STREAM_CODEC.map(MixingRecipe::new, MixingRecipe::io);
    public static final RecipeSerializer<MixingRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    @Override
    public boolean matches(ChemicalRecipe.Input input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(ChemicalRecipe.Input input) {
        return ItemStack.EMPTY;
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
    public RecipeSerializer<MixingRecipe> getSerializer() {
        return Tier5Recipes.MIXING_SERIALIZER.get();
    }

    @Override
    public RecipeType<MixingRecipe> getType() {
        return Tier5Recipes.MIXING.get();
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
