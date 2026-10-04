package dev.strataindustria.smithing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.strataindustria.registry.ModRecipes;
import java.util.List;
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
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

/**
 * A smithing recipe (spec 9.2 and 9.3): hammer {@code count} of the input until the workpiece sits on
 * the target with the last three hits matching every rule.
 *
 * @param defaultTarget the target used when per-world random targets are switched off
 */
public record AnvilRecipe(Ingredient ingredient, int count, ItemStackTemplate result, List<Rule> rules, int defaultTarget)
        implements Recipe<SingleRecipeInput> {
    public static final MapCodec<AnvilRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(AnvilRecipe::ingredient),
            Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(AnvilRecipe::count),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(AnvilRecipe::result),
            Rule.CODEC.listOf(0, 3).fieldOf("rules").forGetter(AnvilRecipe::rules),
            Codec.intRange(Smithing.MIN_TARGET, Smithing.MAX_TARGET).optionalFieldOf("default_target", 75).forGetter(AnvilRecipe::defaultTarget)
    ).apply(i, AnvilRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AnvilRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, AnvilRecipe::ingredient,
            ByteBufCodecs.VAR_INT, AnvilRecipe::count,
            ItemStackTemplate.STREAM_CODEC, AnvilRecipe::result,
            Rule.STREAM_CODEC.apply(ByteBufCodecs.list(3)), AnvilRecipe::rules,
            ByteBufCodecs.VAR_INT, AnvilRecipe::defaultTarget,
            AnvilRecipe::new);

    public static final RecipeSerializer<AnvilRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public AnvilRecipe {
        rules = List.copyOf(rules);
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
