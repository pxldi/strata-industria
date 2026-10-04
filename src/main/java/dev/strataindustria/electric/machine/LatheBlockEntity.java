package dev.strataindustria.electric.machine;

import dev.strataindustria.power.ElectricStats;
import dev.strataindustria.registry.Tier5BlockEntities;
import dev.strataindustria.registry.Tier5Sounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The lathe (spec 10.6): turns an ingot into rods or into a gear, whichever its mode button says. The mode
 * is the block's {@link LatheBlock#GEAR} state, so it shows on the front and survives without saving.
 */
public class LatheBlockEntity extends MachiningBlockEntity {
    public static final String ROD = "rod", GEAR = "gear";
    public static final ElectricStats STATS = ElectricStats.standard(16);

    public LatheBlockEntity(BlockPos pos, BlockState state) {
        super(Tier5BlockEntities.LATHE.get(), ElectricMachineLayout.LATHE, STATS, pos, state);
    }

    public boolean gearMode() {
        return getBlockState().getValue(LatheBlock.GEAR);
    }

    @Override
    protected int modeIndex() {
        return gearMode() ? 1 : 0;
    }

    @Override
    protected String mode() {
        return gearMode() ? GEAR : ROD;
    }

    @Override
    public void toggleMode() {
        if (level == null) return;
        level.setBlock(worldPosition, getBlockState().cycle(LatheBlock.GEAR), net.minecraft.world.level.block.Block.UPDATE_ALL);
        setChanged();
    }

    @Override
    protected SoundEvent workSound() {
        return Tier5Sounds.LATHE_CUT.get();
    }
}
