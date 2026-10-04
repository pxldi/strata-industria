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
 * An assembler recipe (tier 5 spec 10.7): up to 6 item inputs with counts, an optional fluid input, one item result.
 * Matching is shapeless: the machine looks for each ingredient in any input slot.
 * The machine reads {@link #io()} directly and walks every recipe of the type, so the recipe input is empty.
 */
public record AssemblingRecipe(ChemicalIo io) implements Recipe<ChemicalRecipe.Input>, ChemicalRecipe {
    public static final MapCodec<AssemblingRecipe> MAP_CODEC = ChemicalIo.MAP_CODEC.xmap(AssemblingRecipe::new, AssemblingRecipe::io);
    public static final StreamCodec<RegistryFriendlyByteBuf, AssemblingRecipe> STREAM_CODEC = ChemicalIo.STREAM_CODEC.map(AssemblingRecipe::new, AssemblingRecipe::io);
    public static final RecipeSerializer<AssemblingRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

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
    public RecipeSerializer<AssemblingRecipe> getSerializer() {
        return Tier5Recipes.ASSEMBLING_SERIALIZER.get();
    }

    @Override
    public RecipeType<AssemblingRecipe> getType() {
        return Tier5Recipes.ASSEMBLING.get();
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
