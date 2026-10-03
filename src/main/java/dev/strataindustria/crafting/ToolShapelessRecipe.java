package dev.strataindustria.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.strataindustria.registry.ModRecipes;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;

/**
 * A shapeless recipe whose {@code tool} ingredient is not used up: it stays in the grid and takes one
 * point of damage, breaking when worn out. {@code ingredients} must include the tool as well.
 */
public class ToolShapelessRecipe extends ShapelessRecipe {
    public static final MapCodec<ToolShapelessRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter(r -> r.commonInfo),
            CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(r -> r.bookInfo),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(ShapelessRecipe::result),
            Ingredient.CODEC.listOf(1, 9).fieldOf("ingredients").forGetter(r -> r.ingredients),
            Ingredient.CODEC.fieldOf("tool").forGetter(r -> r.tool)
    ).apply(i, ToolShapelessRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToolShapelessRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, r -> r.commonInfo,
            CraftingRecipe.CraftingBookInfo.STREAM_CODEC, r -> r.bookInfo,
            ItemStackTemplate.STREAM_CODEC, ShapelessRecipe::result,
            Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list(9)), r -> r.ingredients,
            Ingredient.CONTENTS_STREAM_CODEC, r -> r.tool,
            ToolShapelessRecipe::new);

    public static final RecipeSerializer<ToolShapelessRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final List<Ingredient> ingredients;
    private final Ingredient tool;

    public ToolShapelessRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo, ItemStackTemplate result,
            List<Ingredient> ingredients, Ingredient tool) {
        super(commonInfo, bookInfo, result, ingredients);
        this.ingredients = List.copyOf(ingredients);
        this.tool = tool;
    }

    public Ingredient tool() {
        return tool;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = super.getRemainingItems(input);
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty() || !tool.test(stack)) continue;
            ItemStack worn = stack.copyWithCount(1);
            if (worn.isDamageableItem()) {
                worn.setDamageValue(worn.getDamageValue() + 1);
                if (worn.getDamageValue() >= worn.getMaxDamage()) worn = ItemStack.EMPTY;
            }
            remaining.set(slot, worn);
            break;
        }
        return remaining;
    }

    @Override
    public RecipeSerializer<ShapelessRecipe> getSerializer() {
        return cast(ModRecipes.TOOL_SHAPELESS.get());
    }

    @SuppressWarnings("unchecked")
    static RecipeSerializer<ShapelessRecipe> cast(RecipeSerializer<? extends ShapelessRecipe> serializer) {
        return (RecipeSerializer<ShapelessRecipe>) (RecipeSerializer<?>) serializer;
    }
}
