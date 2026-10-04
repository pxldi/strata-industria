package dev.strataindustria.processing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.strataindustria.registry.Tier6Recipes;
import dev.strataindustria.tanning.FluidAmount;
import java.util.List;
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
 * An oil still batch (tier 6 spec 5.5 and 17.4): one fluid in, up to three fluids out, a little gas vented,
 * and the temperature it needs. The still itself fixes the draw: 30 HU/t. The still reads the fields directly and walks every recipe of the type, so the
 * recipe input is empty.
 *
 * @param input         what one batch takes
 * @param results       what it makes, in the order of the still's product tanks
 * @param vented        mB of gas that leaves as a puff and is not kept
 * @param ticks         ticks per batch at full heat
 * @param minTemperature lowest °C that distils it, at least the still's own 400
 */
public record OilStillRecipe(FluidAmount input, List<FluidAmount> results, int vented, int ticks, int minTemperature)
        implements Recipe<ChemicalRecipe.Input> {
    public static final MapCodec<OilStillRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            FluidAmount.CODEC.fieldOf("input").forGetter(OilStillRecipe::input),
            FluidAmount.CODEC.listOf(1, 3).fieldOf("results").forGetter(OilStillRecipe::results),
            Codec.intRange(0, 4000).optionalFieldOf("vented", 0).forGetter(OilStillRecipe::vented),
            Codec.intRange(1, 72000).fieldOf("ticks").forGetter(OilStillRecipe::ticks),
            Codec.intRange(400, 3000).fieldOf("min_temperature").forGetter(OilStillRecipe::minTemperature)
    ).apply(i, OilStillRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, OilStillRecipe> STREAM_CODEC = StreamCodec.composite(
            FluidAmount.STREAM_CODEC, OilStillRecipe::input,
            FluidAmount.STREAM_CODEC.apply(ByteBufCodecs.list()), OilStillRecipe::results,
            ByteBufCodecs.VAR_INT, OilStillRecipe::vented,
            ByteBufCodecs.VAR_INT, OilStillRecipe::ticks,
            ByteBufCodecs.VAR_INT, OilStillRecipe::minTemperature,
            OilStillRecipe::new);
    public static final RecipeSerializer<OilStillRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

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
    public RecipeSerializer<OilStillRecipe> getSerializer() {
        return Tier6Recipes.OIL_STILL_SERIALIZER.get();
    }

    @Override
    public RecipeType<OilStillRecipe> getType() {
        return Tier6Recipes.OIL_STILL.get();
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
