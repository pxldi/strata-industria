package dev.strataindustria.electric.machine;

import dev.strataindustria.power.ElectricStats;
import dev.strataindustria.power.ElectricTier;
import dev.strataindustria.processing.ChemicalIo;
import dev.strataindustria.processing.ChemicalRecipe;
import dev.strataindustria.registry.ModRecipes;
import dev.strataindustria.tanning.BarrelRecipe;
import java.util.ArrayList;
import dev.strataindustria.registry.Tier5BlockEntities;
import dev.strataindustria.registry.Tier5Recipes;
import dev.strataindustria.registry.Tier5Sounds;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The mixer (spec 11.1): two items and up to three fluids in, an item and a fluid out. It also runs every barrel
 * recipe at one tenth of its ticks, each as a batch of one.
 */
public class MixerBlockEntity extends ChemicalMachineBlockEntity {
    public static final ElectricStats STATS = ElectricStats.standard(8);

    public MixerBlockEntity(BlockPos pos, BlockState state) {
        super(Tier5BlockEntities.MIXER.get(), ChemicalMachineLayout.MIXER, STATS, pos, state);
    }

    @Override
    protected List<? extends ChemicalRecipe> recipes(ServerLevel level) {
        var map = level.recipeAccess().recipeMap();
        if (map != cachedFor) {
            List<ChemicalRecipe> all = new ArrayList<>(map.byType(Tier5Recipes.MIXING.get()).stream().map(RecipeHolder::value).toList());
            for (RecipeHolder<BarrelRecipe> barrel : map.byType(ModRecipes.BARREL.get())) {
                BarrelPassthrough converted = passthrough(barrel.value());
                if (converted != null) all.add(converted);
            }
            cached = List.copyOf(all);
            cachedFor = map;
        }
        return cached;
    }

    private static Object cachedFor;
    private static List<ChemicalRecipe> cached = List.of();

    /** A barrel recipe as a mixer one; null when it has nothing to take. */
    private static BarrelPassthrough passthrough(BarrelRecipe recipe) {
        if (recipe.ingredient().isEmpty() && recipe.fluid().isEmpty()) return null;
        var items = recipe.ingredient().map(i -> List.of(new ChemicalIo.ItemInput(i, recipe.count()))).orElse(List.of());
        var fluids = recipe.fluid().map(List::of).orElse(List.of());
        var itemResults = recipe.result().map(List::of).orElse(List.of());
        var fluidResults = recipe.fluidResult().map(List::of).orElse(List.of());
        return new BarrelPassthrough(new ChemicalIo(items, fluids, itemResults, fluidResults, Math.max(1, recipe.ticks() / 10), ElectricTier.LV, 0));
    }

    /** A barrel recipe in the mixer's terms. */
    public record BarrelPassthrough(ChemicalIo io) implements ChemicalRecipe {}

    @Override
    protected SoundEvent workSound() {
        return Tier5Sounds.MIXER_STIR.get();
    }
}
