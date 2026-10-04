package dev.strataindustria.processing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.strataindustria.power.ElectricTier;
import dev.strataindustria.registry.Tier4Recipes;
import dev.strataindustria.washing.WashingRecipe;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
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
public record CrushingRecipe(Ingredient ingredient, ItemStackTemplate result, List<WashingRecipe.Chance> chances,
                             Optional<ElectricTier> minTier) implements Recipe<SingleRecipeInput> {
    private static final Codec<ElectricTier> TIER_CODEC = StringRepresentable.fromEnum(ElectricTier::values);

    public static final MapCodec<CrushingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(CrushingRecipe::ingredient),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(CrushingRecipe::result),
            WashingRecipe.Chance.CODEC.listOf(0, 3).optionalFieldOf("chances", List.of()).forGetter(CrushingRecipe::chances),
            TIER_CODEC.optionalFieldOf("min_tier").forGetter(CrushingRecipe::minTier)
    ).apply(i, CrushingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CrushingRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, CrushingRecipe::ingredient,
            ItemStackTemplate.STREAM_CODEC, CrushingRecipe::result,
            WashingRecipe.Chance.STREAM_CODEC.apply(ByteBufCodecs.list(3)), CrushingRecipe::chances,
            ByteBufCodecs.optional(ByteBufCodecs.VAR_INT.map(i -> ElectricTier.values()[i], ElectricTier::ordinal)), CrushingRecipe::minTier,
            CrushingRecipe::new);

    public static final RecipeSerializer<CrushingRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public CrushingRecipe {
        chances = List.copyOf(chances);
    }

    public CrushingRecipe(Ingredient ingredient, ItemStackTemplate result, List<WashingRecipe.Chance> chances) {
        this(ingredient, result, chances, Optional.empty());
    }

    /** The crusher's recipe for a stack; recipes with a {@code min_tier} are the macerator's alone. */
    public static Optional<RecipeHolder<CrushingRecipe>> recipeFor(Level level, ItemStack stack) {
        return recipeFor(level, stack, null);
    }

    /** The recipe for a stack; {@code machine} is the electric tier of a macerator, or null for the crusher. */
    public static Optional<RecipeHolder<CrushingRecipe>> recipeFor(Level level, ItemStack stack, @org.jspecify.annotations.Nullable ElectricTier machine) {
        if (!(level instanceof ServerLevel server) || stack.isEmpty()) return Optional.empty();
        return server.recipeAccess().recipeMap().getRecipesFor(Tier4Recipes.CRUSHING.get(), new SingleRecipeInput(stack), level)
                .filter(holder -> holder.value().minTier().map(min -> machine != null && machine.ordinal() >= min.ordinal()).orElse(true))
                .findFirst();
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
