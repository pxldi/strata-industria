package dev.strataindustria.fluid;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.registry.Tier4BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

/**
 * What a pressure gauge last saw go through it. Pipes hold no fluid, so the needle falls back to zero
 * a second and a half after the flow stops.
 */
public class PressureGaugeBlockEntity extends BlockEntity {
    /** Full scale for steam (spec 9.3: a steel boiler's 10 bar). */
    public static final float FULL_SCALE = 10.0f;
    private static final int HOLD = 30;

    private @Nullable Fluid fluid;
    private float pressure;
    private int flow;
    private float fraction;
    private int sinceRead = HOLD;

    public PressureGaugeBlockEntity(BlockPos pos, BlockState state) {
        super(Tier4BlockEntities.PRESSURE_GAUGE.get(), pos, state);
    }

    /** Called by a push through the gauge's network: steam reads as pressure, anything else as flow. */
    public void read(Fluid fluid, float pressure, int flow, int throughput) {
        this.fluid = fluid;
        this.pressure = pressure;
        this.flow = flow;
        this.fraction = pressure > 0 ? Math.min(1.0f, pressure / FULL_SCALE) : throughput <= 0 ? 0 : Math.min(1.0f, flow / (float) throughput);
        sinceRead = 0;
        showNeedle();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PressureGaugeBlockEntity gauge) {
        if (gauge.sinceRead < HOLD && ++gauge.sinceRead >= HOLD) {
            gauge.fluid = null;
            gauge.pressure = 0;
            gauge.flow = 0;
            gauge.fraction = 0;
            gauge.showNeedle();
        }
    }

    private void showNeedle() {
        if (level == null) return;
        BlockState state = getBlockState();
        int reading = Math.round(fraction * 4);
        if (state.hasProperty(PressureGaugeBlock.READING) && state.getValue(PressureGaugeBlock.READING) != reading) {
            level.setBlock(worldPosition, state.setValue(PressureGaugeBlock.READING, reading), Block.UPDATE_CLIENTS);
        }
    }

    public float pressure() {
        return pressure;
    }

    /** The line shown on right-click. */
    public Component readout() {
        String key = StrataIndustria.MOD_ID + ".pressure_gauge.";
        if (fluid == null) return Component.translatable(key + "empty");
        if (pressure > 0) return Component.translatable(key + "pressure", String.format(java.util.Locale.ROOT, "%.1f", pressure));
        return Component.translatable(key + "flow", fluid.getFluidType().getDescription(), flow);
    }
}
