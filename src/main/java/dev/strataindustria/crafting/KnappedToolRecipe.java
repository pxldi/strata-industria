package dev.strataindustria.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.strataindustria.knapping.KnappedFrom;
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
 * Shapeless assembly of a knapped tool (head + stick + cord). The tool inherits {@code knapped_from}
 * from the head and its durability is {@code base_durability} times the rock's multiplier.
 */
public class KnappedToolRecipe extends ShapelessRecipe {
    public static final MapCodec<KnappedToolRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter(r -> r.commonInfo),
            CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(r -> r.bookInfo),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(ShapelessRecipe::result),
            Ingredient.CODEC.listOf(1, 9).fieldOf("ingredients").forGetter(r -> r.ingredients),
            Codec.intRange(1, 100000).fieldOf("base_durability").forGetter(r -> r.baseDurability)
    ).apply(i, KnappedToolRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, KnappedToolRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, r -> r.commonInfo,
            CraftingRecipe.CraftingBookInfo.STREAM_CODEC, r -> r.bookInfo,
            ItemStackTemplate.STREAM_CODEC, ShapelessRecipe::result,
            Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list(9)), r -> r.ingredients,
            ByteBufCodecs.VAR_INT, r -> r.baseDurability,
            KnappedToolRecipe::new);

    public static final RecipeSerializer<KnappedToolRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final List<Ingredient> ingredients;
    private final int baseDurability;

    public KnappedToolRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo, ItemStackTemplate result,
            List<Ingredient> ingredients, int baseDurability) {
        super(commonInfo, bookInfo, result, ingredients);
        this.ingredients = List.copyOf(ingredients);
        this.baseDurability = baseDurability;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack tool = super.assemble(input);
        for (ItemStack stack : input.items()) {
            KnappedFrom source = stack.get(ModDataComponents.KNAPPED_FROM.get());
            if (source == null) continue;
            tool.set(ModDataComponents.KNAPPED_FROM.get(), source);
            tool.set(DataComponents.MAX_DAMAGE, Math.max(1, Math.round(baseDurability * source.durabilityMultiplier())));
            break;
        }
        return tool;
    }

    @Override
    public RecipeSerializer<ShapelessRecipe> getSerializer() {
        return ToolShapelessRecipe.cast(ModRecipes.KNAPPED_TOOL.get());
    }
}
