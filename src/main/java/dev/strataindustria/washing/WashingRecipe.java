package dev.strataindustria.washing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.strataindustria.registry.ModRecipes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
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
 * Washing one item in a pan or a sluice (tier 3 spec 11): one main output and up to three chance outputs.
 *
 * @param ticks washing time in a sluice; the pan always takes its own 40 ticks
 */
public record WashingRecipe(Ingredient ingredient, ItemStackTemplate result, List<Chance> chances, int ticks)
        implements Recipe<SingleRecipeInput> {
    public static final int DEFAULT_TICKS = 60;

    /** A by-product that comes out with the given chance. */
    public record Chance(ItemStackTemplate item, float chance) {
        public static final Codec<Chance> CODEC = RecordCodecBuilder.create(i -> i.group(
                ItemStackTemplate.CODEC.fieldOf("item").forGetter(Chance::item),
                Codec.floatRange(0.0f, 1.0f).fieldOf("chance").forGetter(Chance::chance)
        ).apply(i, Chance::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Chance> STREAM_CODEC = StreamCodec.composite(
                ItemStackTemplate.STREAM_CODEC, Chance::item,
                ByteBufCodecs.FLOAT, Chance::chance,
                Chance::new);
    }

    public static final MapCodec<WashingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(WashingRecipe::ingredient),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(WashingRecipe::result),
            Chance.CODEC.listOf(0, 3).optionalFieldOf("chances", List.of()).forGetter(WashingRecipe::chances),
            Codec.intRange(1, 6000).optionalFieldOf("ticks", DEFAULT_TICKS).forGetter(WashingRecipe::ticks)
    ).apply(i, WashingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, WashingRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, WashingRecipe::ingredient,
            ItemStackTemplate.STREAM_CODEC, WashingRecipe::result,
            Chance.STREAM_CODEC.apply(ByteBufCodecs.list(3)), WashingRecipe::chances,
            ByteBufCodecs.VAR_INT, WashingRecipe::ticks,
            WashingRecipe::new);

    public static final RecipeSerializer<WashingRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public WashingRecipe {
        chances = List.copyOf(chances);
    }

    public static Optional<RecipeHolder<WashingRecipe>> recipeFor(Level level, ItemStack stack) {
        if (!(level instanceof ServerLevel server) || stack.isEmpty()) return Optional.empty();
        return server.recipeAccess().getRecipeFor(ModRecipes.WASHING.get(), new SingleRecipeInput(stack), level);
    }

    /** The main output followed by whichever by-products came up this time. */
    public List<ItemStack> roll(RandomSource random) {
        List<ItemStack> out = new ArrayList<>();
        out.add(result.create());
        for (Chance chance : chances) {
            if (random.nextFloat() < chance.chance()) out.add(chance.item().create());
        }
        return out;
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
    public RecipeSerializer<WashingRecipe> getSerializer() {
        return ModRecipes.WASHING_SERIALIZER.get();
    }

    @Override
    public RecipeType<WashingRecipe> getType() {
        return ModRecipes.WASHING.get();
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
