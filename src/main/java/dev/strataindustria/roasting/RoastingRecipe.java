package dev.strataindustria.roasting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.strataindustria.registry.Tier4Recipes;
import dev.strataindustria.tanning.FluidAmount;
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
 * Roasting (tier 4 spec 5.3): an item held at or above a temperature for a time turns into another and
 * gives off gas. A forge heating slot roasts by hand and loses the gas; the roaster collects it.
 *
 * @param result the item it becomes; absent when only the gas is wanted (burning sulfur)
 * @param ticks time at temperature for one item; a slot roasts its whole stack together
 * @param gas   gas released per item
 * @param forge whether a forge heating slot may roast it; false where the gas is worth more than the item (alum)
 */
public record RoastingRecipe(Ingredient ingredient, Optional<ItemStackTemplate> result, int minTemperature, int ticks,
                             Optional<FluidAmount> gas, boolean forge) implements Recipe<SingleRecipeInput> {
    public RoastingRecipe(Ingredient ingredient, ItemStackTemplate result, int minTemperature, int ticks, Optional<FluidAmount> gas) {
        this(ingredient, Optional.of(result), minTemperature, ticks, gas, true);
    }

    public RoastingRecipe(Ingredient ingredient, ItemStackTemplate result, int minTemperature, int ticks, Optional<FluidAmount> gas, boolean forge) {
        this(ingredient, Optional.of(result), minTemperature, ticks, gas, forge);
    }

    public static final MapCodec<RoastingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(RoastingRecipe::ingredient),
            ItemStackTemplate.CODEC.optionalFieldOf("result").forGetter(RoastingRecipe::result),
            Codec.intRange(0, 3000).fieldOf("min_temperature").forGetter(RoastingRecipe::minTemperature),
            Codec.intRange(1, 72000).fieldOf("ticks").forGetter(RoastingRecipe::ticks),
            FluidAmount.CODEC.optionalFieldOf("gas").forGetter(RoastingRecipe::gas),
            Codec.BOOL.optionalFieldOf("forge", true).forGetter(RoastingRecipe::forge)
    ).apply(i, RoastingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, RoastingRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, RoastingRecipe::ingredient,
            ByteBufCodecs.optional(ItemStackTemplate.STREAM_CODEC), RoastingRecipe::result,
            ByteBufCodecs.VAR_INT, RoastingRecipe::minTemperature,
            ByteBufCodecs.VAR_INT, RoastingRecipe::ticks,
            ByteBufCodecs.optional(FluidAmount.STREAM_CODEC), RoastingRecipe::gas,
            ByteBufCodecs.BOOL, RoastingRecipe::forge,
            RoastingRecipe::new);

    public static final RecipeSerializer<RoastingRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public static Optional<RecipeHolder<RoastingRecipe>> recipeFor(Level level, ItemStack stack) {
        if (!(level instanceof ServerLevel server) || stack.isEmpty()) return Optional.empty();
        return server.recipeAccess().getRecipeFor(Tier4Recipes.ROASTING.get(), new SingleRecipeInput(stack), level);
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input) {
        return result.map(ItemStackTemplate::create).orElse(ItemStack.EMPTY);
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
    public RecipeSerializer<RoastingRecipe> getSerializer() {
        return Tier4Recipes.ROASTING_SERIALIZER.get();
    }

    @Override
    public RecipeType<RoastingRecipe> getType() {
        return Tier4Recipes.ROASTING.get();
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
