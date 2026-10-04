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

/** The assembler (spec 10.7): six items and one fluid in, one item out, shapeless, 200 ticks unless the recipe says otherwise. */
public class AssemblerBlockEntity extends ChemicalMachineBlockEntity {
    public static final ElectricStats STATS = ElectricStats.standard(16);

    public AssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(Tier5BlockEntities.ASSEMBLER.get(), ChemicalMachineLayout.ASSEMBLER, STATS, pos, state);
    }

    @Override
    protected List<? extends ChemicalRecipe> recipes(ServerLevel level) {
        return level.recipeAccess().recipeMap().byType(Tier5Recipes.ASSEMBLING.get()).stream().map(RecipeHolder::value).toList();
    }

    @Override
    protected SoundEvent workSound() {
        return Tier5Sounds.ASSEMBLER_WORK.get();
    }
}
