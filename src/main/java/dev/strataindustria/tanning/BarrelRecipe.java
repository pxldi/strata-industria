package dev.strataindustria.tanning;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.strataindustria.registry.ModRecipes;
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
import net.minecraft.world.level.Level;

/**
 * Soaking in a sealed barrel (tier 3 spec 12.1). One batch takes {@code count} items and the fluid
 * amount and gives the result and the fluid result; a barrel runs as many batches at once as it holds.
 * A recipe that makes a fluid turns the whole tank over, so it needs enough items for all of it.
 */
public record BarrelRecipe(Optional<Ingredient> ingredient, int count, Optional<FluidAmount> fluid,
                           Optional<ItemStackTemplate> result, Optional<FluidAmount> fluidResult, int ticks)
        implements Recipe<BarrelInput> {
    public static final MapCodec<BarrelRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC.optionalFieldOf("ingredient").forGetter(BarrelRecipe::ingredient),
            Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(BarrelRecipe::count),
            FluidAmount.CODEC.optionalFieldOf("fluid").forGetter(BarrelRecipe::fluid),
            ItemStackTemplate.CODEC.optionalFieldOf("result").forGetter(BarrelRecipe::result),
            FluidAmount.CODEC.optionalFieldOf("fluid_result").forGetter(BarrelRecipe::fluidResult),
            Codec.intRange(1, 72000).fieldOf("ticks").forGetter(BarrelRecipe::ticks)
    ).apply(i, BarrelRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BarrelRecipe> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(Ingredient.CONTENTS_STREAM_CODEC), BarrelRecipe::ingredient,
            ByteBufCodecs.VAR_INT, BarrelRecipe::count,
            ByteBufCodecs.optional(FluidAmount.STREAM_CODEC), BarrelRecipe::fluid,
            ByteBufCodecs.optional(ItemStackTemplate.STREAM_CODEC), BarrelRecipe::result,
            ByteBufCodecs.optional(FluidAmount.STREAM_CODEC), BarrelRecipe::fluidResult,
            ByteBufCodecs.VAR_INT, BarrelRecipe::ticks,
            BarrelRecipe::new);

    public static final RecipeSerializer<BarrelRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public static Optional<RecipeHolder<BarrelRecipe>> recipeFor(ServerLevel level, BarrelInput input) {
        if (input.isEmpty()) return Optional.empty();
        return level.recipeAccess().getRecipeFor(ModRecipes.BARREL.get(), input, level);
    }

    /**
     * How many batches the barrel can run now. For a recipe that makes a fluid this is the number the
     * whole tank needs, which may be more than the items allow (see {@link #itemsShort}).
     */
    public int batches(BarrelInput input) {
        int byFluid = fluid.map(f -> fluidResult.isPresent()
                ? (input.amount() + f.amount() - 1) / f.amount()
                : input.amount() / f.amount()).orElse(Integer.MAX_VALUE);
        if (fluidResult.isPresent()) return byFluid;
        int byItems = ingredient.isPresent() ? input.item().getCount() / count : Integer.MAX_VALUE;
        int batches = Math.min(byFluid, byItems);
        return batches == Integer.MAX_VALUE ? 1 : batches;
    }

    /** Items still missing to turn the whole tank over, or 0. */
    public int itemsShort(BarrelInput input) {
        if (ingredient.isEmpty()) return 0;
        return Math.max(0, batches(input) * count - input.item().getCount());
    }

    @Override
    public boolean matches(BarrelInput input, Level level) {
        if (ingredient.isPresent() ? !ingredient.get().test(input.item()) : !input.item().isEmpty()) return false;
        if (fluid.isPresent()) return input.fluid().isSame(fluid.get().fluid()) && input.amount() > 0;
        return true;
    }

    @Override
    public ItemStack assemble(BarrelInput input) {
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
    public RecipeSerializer<BarrelRecipe> getSerializer() {
        return ModRecipes.BARREL_SERIALIZER.get();
    }

    @Override
    public RecipeType<BarrelRecipe> getType() {
        return ModRecipes.BARREL.get();
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
