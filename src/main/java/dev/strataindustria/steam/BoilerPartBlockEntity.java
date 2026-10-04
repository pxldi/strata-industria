package dev.strataindustria.steam;

import dev.strataindustria.fluid.FluidPort;
import dev.strataindustria.heat.HeatConsumer;
import dev.strataindustria.heat.HeatPipeBlock;
import dev.strataindustria.registry.Tier4BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

/**
 * A steel boiler shell or fluid port's link to its controller. It holds nothing: the heat of a firebox
 * under it, and the water a water port is given, go to the controller that last claimed it.
 */
public class BoilerPartBlockEntity extends BlockEntity implements HeatConsumer, FluidPort {
    private @Nullable BlockPos host;

    public BoilerPartBlockEntity(BlockPos pos, BlockState state) {
        super(Tier4BlockEntities.BOILER_PART.get(), pos, state);
    }

    /** Called by a controller each time it finds this block in its built structure. */
    public void claim(BlockPos controller) {
        host = controller.immutable();
    }

    public @Nullable SteelBoilerControllerBlockEntity host() {
        if (host == null || level == null || !level.isLoaded(host)) return null;
        return level.getBlockEntity(host) instanceof SteelBoilerControllerBlockEntity boiler && boiler.usesPart(worldPosition) ? boiler : null;
    }

    private boolean port() {
        return getBlockState().hasProperty(BoilerFluidPortBlock.MODE);
    }

    @Override
    public int heatDemand(float temperature) {
        SteelBoilerControllerBlockEntity boiler = host();
        return boiler == null ? 0 : boiler.heatDemand(temperature);
    }

    @Override
    public int offerHeat(float temperature, int heat) {
        SteelBoilerControllerBlockEntity boiler = host();
        return boiler == null ? 0 : boiler.offerHeat(temperature, heat);
    }

    @Override
    public void heatRoute(int pipes, @Nullable HeatPipeBlock limitedBy) {
        SteelBoilerControllerBlockEntity boiler = host();
        if (boiler != null) boiler.heatRoute(pipes, limitedBy);
    }

    /** Pipes join the ports, not the plain shell. */
    @Override
    public boolean connectsFluid(Direction side) {
        return port();
    }

    @Override
    public int fill(Direction side, Fluid fluid, int amount, float pressure, boolean simulate) {
        if (!port() || getBlockState().getValue(BoilerFluidPortBlock.MODE) != BoilerFluidPortBlock.Mode.WATER) return 0;
        SteelBoilerControllerBlockEntity boiler = host();
        return boiler == null ? 0 : boiler.fillWater(fluid, amount, simulate);
    }
}
