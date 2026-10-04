package dev.strataindustria.processing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.strataindustria.registry.Tier5Recipes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
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
 * An extruder recipe (tier 5 spec 10.9): the shaping machines' {@code machining} idea with the assembler's item
 * inputs, because extrusion takes two inputs at once. {@code mode} names the extruder mode button setting it
 * belongs to ({@code cable_lv}, {@code cable_mv}, {@code pipe}). The machine reads {@link #io()} directly.
 */
public record ExtrudingRecipe(String mode, ChemicalIo io) implements Recipe<ChemicalRecipe.Input>, ChemicalRecipe {
    public static final MapCodec<ExtrudingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("mode").forGetter(ExtrudingRecipe::mode),
            ChemicalIo.MAP_CODEC.forGetter(ExtrudingRecipe::io)
    ).apply(i, ExtrudingRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, ExtrudingRecipe> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ExtrudingRecipe::mode,
            ChemicalIo.STREAM_CODEC, ExtrudingRecipe::io,
            ExtrudingRecipe::new);
    public static final RecipeSerializer<ExtrudingRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

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
    public RecipeSerializer<ExtrudingRecipe> getSerializer() {
        return Tier5Recipes.EXTRUDING_SERIALIZER.get();
    }

    @Override
    public RecipeType<ExtrudingRecipe> getType() {
        return Tier5Recipes.EXTRUDING.get();
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
