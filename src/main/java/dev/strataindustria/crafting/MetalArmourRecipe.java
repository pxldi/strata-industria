package dev.strataindustria.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.strataindustria.metal.Quality;
import dev.strataindustria.registry.ModDataComponents;
import dev.strataindustria.registry.ModRecipes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

/**
 * Shaped armour from plates (spec 8.4). The piece takes the average quality of its plates, which scales
 * its durability by up to 20% either way, as for tools.
 */
public class MetalArmourRecipe extends ShapedRecipe {
    public static final MapCodec<MetalArmourRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter(r -> r.commonInfo),
            CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(r -> r.bookInfo),
            ShapedRecipePattern.MAP_CODEC.forGetter(r -> r.pattern),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(r -> r.result)
    ).apply(i, MetalArmourRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MetalArmourRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, r -> r.commonInfo,
            CraftingRecipe.CraftingBookInfo.STREAM_CODEC, r -> r.bookInfo,
            ShapedRecipePattern.STREAM_CODEC, r -> r.pattern,
            ItemStackTemplate.STREAM_CODEC, r -> r.result,
            MetalArmourRecipe::new);

    public static final RecipeSerializer<MetalArmourRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final ItemStackTemplate result;

    public MetalArmourRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo, ShapedRecipePattern pattern,
            ItemStackTemplate result) {
        super(commonInfo, bookInfo, pattern, result);
        this.result = result;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack piece = super.assemble(input);
        int material = 0, craft = 0, plates = 0;
        for (ItemStack stack : input.items()) {
            Quality quality = stack.get(ModDataComponents.QUALITY.get());
            if (quality == null) continue;
            material += quality.material();
            craft += quality.craft();
            plates++;
        }
        if (plates == 0) return piece;
        Quality quality = new Quality(Math.round(material / (float) plates), Math.round(craft / (float) plates));
        piece.set(ModDataComponents.QUALITY.get(), quality);
        if (piece.isDamageableItem()) {
            piece.set(DataComponents.MAX_DAMAGE, Math.max(1, Math.round(piece.getMaxDamage() * quality.durabilityMultiplier())));
        }
        return piece;
    }

    @Override
    @SuppressWarnings("unchecked")
    public RecipeSerializer<ShapedRecipe> getSerializer() {
        return (RecipeSerializer<ShapedRecipe>) (RecipeSerializer<?>) ModRecipes.METAL_ARMOUR.get();
    }
}
