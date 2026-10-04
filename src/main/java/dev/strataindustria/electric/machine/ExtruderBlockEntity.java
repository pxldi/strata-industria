package dev.strataindustria.electric.machine;

import dev.strataindustria.power.ElectricStats;
import dev.strataindustria.processing.ChemicalRecipe;
import dev.strataindustria.processing.ExtrudingRecipe;
import dev.strataindustria.registry.Tier5BlockEntities;
import dev.strataindustria.registry.Tier5Recipes;
import dev.strataindustria.registry.Tier5Sounds;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The extruder (spec 10.9): MV only, two items in and one stack out, 64 J/t for 100 ticks. It does not
 * follow the MV rule (no LV form, so one operation at the base speed). The mode button steps through
 * LV cable, MV cable and pipe; only recipes of the current mode run, but any mode's items are accepted.
 */
public class ExtruderBlockEntity extends ChemicalMachineBlockEntity {
    public static final String[] MODES = {"cable_lv", "cable_mv", "pipe", "item_pipe"};
    public static final ElectricStats STATS = new ElectricStats(new ElectricStats.Tiered<>(64, 64), new ElectricStats.Tiered<>(1.0f, 1.0f),
            new ElectricStats.Tiered<>(1, 1), 20);

    private int mode;

    public ExtruderBlockEntity(BlockPos pos, BlockState state) {
        super(Tier5BlockEntities.EXTRUDER.get(), ChemicalMachineLayout.EXTRUDER, STATS, pos, state);
    }

    @Override
    protected List<? extends ChemicalRecipe> recipes(ServerLevel level) {
        return level.recipeAccess().recipeMap().byType(Tier5Recipes.EXTRUDING.get()).stream().map(RecipeHolder::value).toList();
    }

    @Override
    protected boolean runs(ChemicalRecipe recipe) {
        return recipe instanceof ExtrudingRecipe extruding && extruding.mode().equals(MODES[mode]);
    }

    @Override
    protected int modeIndex() {
        return mode;
    }

    public String mode() {
        return MODES[mode];
    }

    @Override
    public void toggleMode() {
        mode = (mode + 1) % MODES.length;
        setChanged();
    }

    @Override
    protected SoundEvent workSound() {
        return Tier5Sounds.EXTRUDER_PRESS.get();
    }

    @Override
    protected SoundEvent finishSound() {
        return Tier5Sounds.EXTRUDER_PRESS.get();
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        mode = Math.floorMod(in.getIntOr("mode", 0), MODES.length);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("mode", mode);
    }
}
