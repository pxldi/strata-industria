package dev.strataindustria.processing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.strataindustria.registry.Tier5Recipes;
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
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * A shaping machine's recipe (tier 5 spec 10.4 to 10.6): one item in, one stack out, for the wiremill,
 * bender, lathe and later the extruder. A machine with a mode button (the lathe's rod and gear) names the
 * mode a recipe belongs to; the others leave it out.
 */
public record MachiningRecipe(String machine, Optional<String> mode, Ingredient ingredient, ItemStackTemplate result)
        implements Recipe<MachiningRecipe.Input> {
    /** The item in the machine's slot, and which machine and mode is asking. */
    public record Input(String machine, String mode, ItemStack item) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return item;
        }

        @Override
        public int size() {
            return 1;
        }
    }

    public static final MapCodec<MachiningRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("machine").forGetter(MachiningRecipe::machine),
            Codec.STRING.optionalFieldOf("mode").forGetter(MachiningRecipe::mode),
            Ingredient.CODEC.fieldOf("ingredient").forGetter(MachiningRecipe::ingredient),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(MachiningRecipe::result)
    ).apply(i, MachiningRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MachiningRecipe> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, MachiningRecipe::machine,
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), MachiningRecipe::mode,
            Ingredient.CONTENTS_STREAM_CODEC, MachiningRecipe::ingredient,
            ItemStackTemplate.STREAM_CODEC, MachiningRecipe::result,
            MachiningRecipe::new);

    public static final RecipeSerializer<MachiningRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    /** The recipe {@code machine} runs on {@code stack} in {@code mode} ("" for a machine without modes). */
    public static Optional<RecipeHolder<MachiningRecipe>> recipeFor(Level level, String machine, String mode, ItemStack stack) {
        if (!(level instanceof ServerLevel server) || stack.isEmpty()) return Optional.empty();
        return server.recipeAccess().getRecipeFor(Tier5Recipes.MACHINING.get(), new Input(machine, mode, stack), level);
    }

    @Override
    public boolean matches(Input input, Level level) {
        return machine.equals(input.machine()) && mode.orElse("").equals(input.mode()) && ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(Input input) {
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
    public RecipeSerializer<MachiningRecipe> getSerializer() {
        return Tier5Recipes.MACHINING_SERIALIZER.get();
    }

    @Override
    public RecipeType<MachiningRecipe> getType() {
        return Tier5Recipes.MACHINING.get();
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
