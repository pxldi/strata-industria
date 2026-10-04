package dev.strataindustria.heat;

import dev.strataindustria.registry.Tier4BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * A heat inlet's link to its multiblock. It holds no heat: what pipes or a firebox offer it goes to the
 * controller that last claimed it, and until a built structure claims it, it takes nothing.
 */
public class HeatInletBlockEntity extends BlockEntity implements HeatPort, HeatConsumer {
    private @Nullable BlockPos host;

    public HeatInletBlockEntity(BlockPos pos, BlockState state) {
        super(Tier4BlockEntities.HEAT_INLET.get(), pos, state);
    }

    /** Called by a controller each time it finds this inlet in its built structure. */
    public void claim(BlockPos controller) {
        host = controller.immutable();
    }

    public @Nullable InletHost host() {
        if (host == null || level == null || !level.isLoaded(host)) return null;
        return level.getBlockEntity(host) instanceof InletHost furnace && furnace.usesInlet(worldPosition) ? furnace : null;
    }

    @Override
    public boolean connectsHeat(Direction side) {
        return true;
    }

    @Override
    public int heatDemand(float temperature) {
        InletHost furnace = host();
        return furnace == null ? 0 : furnace.heatDemand(temperature);
    }

    @Override
    public int offerHeat(float temperature, int heat) {
        InletHost furnace = host();
        return furnace == null ? 0 : furnace.offerHeat(temperature, heat);
    }

    @Override
    public void heatRoute(int pipes, @Nullable HeatPipeBlock limitedBy) {
        InletHost furnace = host();
        if (furnace != null) furnace.heatRoute(pipes, limitedBy);
    }
}
