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

/** The electrolyser (spec 11.2): splits water and brine and reduces alumina; 32 J/t. */
public class ElectrolyserBlockEntity extends ChemicalMachineBlockEntity {
    public static final ElectricStats STATS = ElectricStats.standard(32);

    public ElectrolyserBlockEntity(BlockPos pos, BlockState state) {
        super(Tier5BlockEntities.ELECTROLYSER.get(), ChemicalMachineLayout.ELECTROLYSER, STATS, pos, state);
    }

    @Override
    protected List<? extends ChemicalRecipe> recipes(ServerLevel level) {
        return level.recipeAccess().recipeMap().byType(Tier5Recipes.ELECTROLYSIS.get()).stream().map(RecipeHolder::value).toList();
    }

    @Override
    protected SoundEvent workSound() {
        return Tier5Sounds.ELECTROLYSER_BUBBLE.get();
    }
}
