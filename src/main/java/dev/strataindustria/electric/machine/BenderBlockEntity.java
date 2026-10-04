package dev.strataindustria.electric.machine;

import dev.strataindustria.power.ElectricStats;
import dev.strataindustria.registry.Tier5BlockEntities;
import dev.strataindustria.registry.Tier5Sounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.state.BlockState;

/** The bender (spec 10.5): an ingot rolled out into a plate, a double ingot into two. */
public class BenderBlockEntity extends MachiningBlockEntity {
    public static final ElectricStats STATS = ElectricStats.standard(16);

    public BenderBlockEntity(BlockPos pos, BlockState state) {
        super(Tier5BlockEntities.BENDER.get(), ElectricMachineLayout.BENDER, STATS, pos, state);
    }

    @Override
    protected SoundEvent workSound() {
        return Tier5Sounds.BENDER_PRESS.get();
    }
}
