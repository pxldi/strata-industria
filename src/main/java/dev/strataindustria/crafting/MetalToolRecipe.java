package dev.strataindustria.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.strataindustria.metal.Quality;
import dev.strataindustria.registry.ModDataComponents;
import dev.strataindustria.registry.ModRecipes;
import java.util.List;
import net.minecraft.core.component.DataComponents;
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
 * Shapeless assembly of a metal tool (head + stick, spec 8.3). The tool takes the head's quality,
 * which scales its durability by up to 20% either way.
 */
public class MetalToolRecipe extends ShapelessRecipe {
    public static final MapCodec<MetalToolRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter(r -> r.commonInfo),
            CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(r -> r.bookInfo),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(ShapelessRecipe::result),
            Ingredient.CODEC.listOf(1, 9).fieldOf("ingredients").forGetter(r -> r.ingredients)
    ).apply(i, MetalToolRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MetalToolRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, r -> r.commonInfo,
            CraftingRecipe.CraftingBookInfo.STREAM_CODEC, r -> r.bookInfo,
            ItemStackTemplate.STREAM_CODEC, ShapelessRecipe::result,
            Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list(9)), r -> r.ingredients,
            MetalToolRecipe::new);

    public static final RecipeSerializer<MetalToolRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final List<Ingredient> ingredients;

    public MetalToolRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo, ItemStackTemplate result,
            List<Ingredient> ingredients) {
        super(commonInfo, bookInfo, result, ingredients);
        this.ingredients = List.copyOf(ingredients);
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack tool = super.assemble(input);
        for (ItemStack stack : input.items()) {
            Quality quality = stack.get(ModDataComponents.QUALITY.get());
            if (quality == null) continue;
            tool.set(ModDataComponents.QUALITY.get(), quality);
            if (tool.isDamageableItem()) {
                tool.set(DataComponents.MAX_DAMAGE, Math.max(1, Math.round(tool.getMaxDamage() * quality.durabilityMultiplier())));
            }
            break;
        }
        return tool;
    }

    @Override
    public RecipeSerializer<ShapelessRecipe> getSerializer() {
        return ToolShapelessRecipe.cast(ModRecipes.METAL_TOOL.get());
    }
}
