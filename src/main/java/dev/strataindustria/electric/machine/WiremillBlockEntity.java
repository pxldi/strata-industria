package dev.strataindustria.electric.machine;

import dev.strataindustria.power.ElectricStats;
import dev.strataindustria.registry.Tier5BlockEntities;
import dev.strataindustria.registry.Tier5Sounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.state.BlockState;

/** The wiremill (spec 10.4): a rod pulled through the draw plate, two wires from every rod. */
public class WiremillBlockEntity extends MachiningBlockEntity {
    public static final ElectricStats STATS = ElectricStats.standard(8);

    public WiremillBlockEntity(BlockPos pos, BlockState state) {
        super(Tier5BlockEntities.WIREMILL.get(), ElectricMachineLayout.WIREMILL, STATS, pos, state);
    }

    @Override
    protected SoundEvent workSound() {
        return Tier5Sounds.WIREMILL_DRAW.get();
    }
}
