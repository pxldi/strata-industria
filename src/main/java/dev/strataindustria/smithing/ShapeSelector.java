package dev.strataindustria.smithing;

import dev.strataindustria.material.Metal;
import dev.strataindustria.registry.ModDataComponents;
import dev.strataindustria.registry.ModRecipes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The shape a hammer machine works, picked with the button on its screen (redesign L4). It remembers a
 * shape by the kind of piece it makes ("pickaxe head", "plate"), so feeding it another metal carries on with
 * the same kind of piece instead of stopping. With nothing picked it takes the first shape the piece allows.
 */
public final class ShapeSelector {
    private static final Metal[] METALS_BY_NAME_LENGTH = Arrays.stream(Metal.values())
            .sorted(Comparator.comparingInt((Metal m) -> m.getSerializedName().length()).reversed()).toArray(Metal[]::new);
    private static final String PREFIX = "anvil/";

    private @Nullable ResourceKey<Recipe<?>> chosen;
    private @Nullable Optional<RecipeHolder<AnvilRecipe>> cached;
    private @Nullable Object cacheMap;
    private @Nullable Item cacheItem;
    private boolean cacheBloom;
    private @Nullable ResourceKey<Recipe<?>> cacheChosen;

    public @Nullable ResourceKey<Recipe<?>> chosen() {
        return chosen;
    }

    public void choose(ResourceKey<Recipe<?>> key) {
        chosen = key;
    }

    /** "pickaxe_head" for "anvil/bronze_pickaxe_head": the recipe's name without the metal. */
    public static String kind(ResourceKey<Recipe<?>> key) {
        String path = key.identifier().getPath();
        if (path.startsWith(PREFIX)) path = path.substring(PREFIX.length());
        int from = path.indexOf("_from_");
        if (from > 0) path = path.substring(0, from);
        for (Metal metal : METALS_BY_NAME_LENGTH) {
            String name = metal.getSerializedName() + "_";
            if (path.startsWith(name) && path.length() > name.length()) return path.substring(name.length());
        }
        return path;
    }

    /** Every anvil shape that takes {@code piece}, or every shape at all while there is no piece, in a fixed order. */
    public static List<RecipeHolder<AnvilRecipe>> options(ServerLevel level, ItemStack piece) {
        List<RecipeHolder<AnvilRecipe>> found = new ArrayList<>();
        level.recipeAccess().recipeMap().byType(ModRecipes.ANVIL.get()).forEach(holder -> {
            if (piece.isEmpty() || holder.value().matches(new SingleRecipeInput(piece), level)) found.add(holder);
        });
        found.sort(Comparator.comparing(h -> h.id().identifier().toString()));
        return found;
    }

    /**
     * The shape to work {@code piece} in: the picked one when it fits, else the same kind in the piece's own
     * metal, else (nothing picked) the first that fits. Empty when the piece takes none of them.
     */
    public Optional<RecipeHolder<AnvilRecipe>> resolve(ServerLevel level, ItemStack piece) {
        // Machines ask every tick; the answer only changes with the piece's kind, the pick or a recipe reload.
        Object map = level.recipeAccess().recipeMap();
        boolean bloom = piece.has(ModDataComponents.BLOOM_CONTENTS.get());
        if (cached != null && cacheMap == map && cacheItem == piece.getItem() && cacheBloom == bloom && Objects.equals(cacheChosen, chosen)) return cached;
        Optional<RecipeHolder<AnvilRecipe>> found = pick(level, piece);
        cached = found;
        cacheMap = map;
        cacheItem = piece.getItem();
        cacheBloom = bloom;
        cacheChosen = chosen;
        return found;
    }

    private Optional<RecipeHolder<AnvilRecipe>> pick(ServerLevel level, ItemStack piece) {
        List<RecipeHolder<AnvilRecipe>> options = options(level, piece);
        if (options.isEmpty()) return Optional.empty();
        if (chosen != null) {
            for (RecipeHolder<AnvilRecipe> holder : options) if (holder.id().equals(chosen)) return Optional.of(holder);
            String kind = kind(chosen);
            for (RecipeHolder<AnvilRecipe> holder : options) if (kind(holder.id()).equals(kind)) return Optional.of(holder);
            if (!piece.isEmpty()) return Optional.empty();
        }
        return Optional.of(options.get(0));
    }

    /** The next (or previous) shape for {@code piece}, wrapping round. Returns its position in the list, for the note. */
    public int cycle(ServerLevel level, ItemStack piece, int step) {
        List<RecipeHolder<AnvilRecipe>> options = options(level, piece);
        if (options.isEmpty()) return 0;
        Optional<RecipeHolder<AnvilRecipe>> now = resolve(level, piece);
        int at = 0;
        if (now.isPresent()) for (int i = 0; i < options.size(); i++) if (options.get(i).id().equals(now.get().id())) at = i;
        int next = Math.floorMod(at + step, options.size());
        chosen = options.get(next).id();
        return next;
    }

    /** The raw item id (plus one) of what the shape makes, for the screen; 0 for nothing. */
    public static int displayId(@Nullable RecipeHolder<AnvilRecipe> holder) {
        if (holder == null) return 0;
        return BuiltInRegistries.ITEM.getId(holder.value().result().item().value()) + 1;
    }

    public static ItemStack displayStack(int id) {
        if (id <= 0) return ItemStack.EMPTY;
        return BuiltInRegistries.ITEM.byId(id - 1).getDefaultInstance();
    }

    public void save(ValueOutput out) {
        if (chosen != null) out.putString("shape", chosen.identifier().toString());
    }

    public void load(ValueInput in) {
        chosen = in.getString("shape").map(Identifier::tryParse).map(id -> ResourceKey.<Recipe<?>>create(Registries.RECIPE, id)).orElse(null);
    }
}
