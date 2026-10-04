package dev.strataindustria.electric.machine;

import dev.strataindustria.power.ElectricStats;
import dev.strataindustria.processing.ChemicalRecipe;
import dev.strataindustria.registry.Tier5BlockEntities;
import dev.strataindustria.registry.Tier5Recipes;
import dev.strataindustria.registry.Tier5Sounds;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;

/** The mixer (spec 11.1): two items and up to three fluids in, an item and a fluid out. */
public class MixerBlockEntity extends ChemicalMachineBlockEntity {
    public static final ElectricStats STATS = ElectricStats.standard(8);

    public MixerBlockEntity(BlockPos pos, BlockState state) {
        super(Tier5BlockEntities.MIXER.get(), ChemicalMachineLayout.MIXER, STATS, pos, state);
    }

    @Override
    protected List<? extends ChemicalRecipe> recipes(ServerLevel level) {
        return level.recipeAccess().recipeMap().byType(Tier5Recipes.MIXING.get()).stream().map(RecipeHolder::value).toList();
    }

    @Override
    protected SoundEvent workSound() {
        return Tier5Sounds.MIXER_STIR.get();
    }
}
