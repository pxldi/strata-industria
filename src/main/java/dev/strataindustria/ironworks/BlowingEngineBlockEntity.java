package dev.strataindustria.ironworks;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.fluid.FluidPort;
import dev.strataindustria.registry.Tier4BlockEntities;
import dev.strataindustria.registry.Tier4Fluids;
import dev.strataindustria.registry.Tier4Sounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The blowing engine (tier 4 spec 10.5): 15 mB of steam a tick at 2 bar or more blows as much air as two
 * blowers; at 1 to 2 bar it runs at half stroke, for half the steam and one blower's worth. It is not a
 * kinetic source.
 */
public class BlowingEngineBlockEntity extends BlockEntity implements FluidPort, AirBlast {
    public static final float FULL_PRESSURE = 2.0f, MIN_PRESSURE = 1.0f;
    public static final int FULL_USE = 15, HALF_USE = 8, BUFFER = 40;
    /** Ticks without steam before the pressure it last saw counts as gone. */
    private static final int STEAM_TIMEOUT = 5;
    private static final int STROKE_TICKS = 12;

    private int steam;
    private float pressure;
    private int sinceSteam = STEAM_TIMEOUT;
    /** Blowers' worth of air this tick: 0, 1 or 2. */
    private int air;
    private int strokeTicks;

    public BlowingEngineBlockEntity(BlockPos pos, BlockState state) {
        super(Tier4BlockEntities.BLOWING_ENGINE.get(), pos, state);
    }

    private Direction facing() {
        return getBlockState().getValue(BlowingEngineBlock.FACING);
    }

    public int air() {
        return air;
    }

    @Override
    public int airOut(Direction out) {
        return out == facing() ? air : 0;
    }

    @Override
    public boolean connectsFluid(Direction side) {
        return side == facing().getOpposite();
    }

    @Override
    public int fill(Direction side, Fluid fluid, int amount, float pressure, boolean simulate) {
        if (side != facing().getOpposite() || !fluid.isSame(Tier4Fluids.STEAM.get())) return 0;
        // Whatever it is offered tells it the pressure, even when its buffer is full.
        this.pressure = pressure;
        sinceSteam = 0;
        if (pressure < MIN_PRESSURE) return 0;
        int take = Math.max(0, Math.min(amount, BUFFER - steam));
        if (!simulate) steam += take;
        return take;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BlowingEngineBlockEntity engine) {
        engine.tick(level, pos, state);
    }

    private void tick(Level level, BlockPos pos, BlockState state) {
        if (++sinceSteam > STEAM_TIMEOUT) pressure = 0;
        int want = pressure >= FULL_PRESSURE ? 2 : pressure >= MIN_PRESSURE ? 1 : 0;
        int use = want == 2 ? FULL_USE : want == 1 ? HALF_USE : 0;
        // The buffer covers the gaps between pushes; short of it, the stroke drops to half or stops.
        if (want == 2 && steam < FULL_USE) want = steam >= HALF_USE ? 1 : 0;
        if (want == 1 && steam < HALF_USE) want = 0;
        use = want == 2 ? FULL_USE : want == 1 ? HALF_USE : 0;
        steam -= use;
        if (want != air) {
            if (air == 0 && want > 0) level.playSound(null, pos, Tier4Sounds.STEAM_ENGINE_START.get(), SoundSource.BLOCKS, 0.8f, 1.1f);
            if (air > 0 && want == 0) level.playSound(null, pos, Tier4Sounds.STEAM_ENGINE_STOP.get(), SoundSource.BLOCKS, 0.8f, 1.0f);
            air = want;
            setChanged();
        }
        boolean active = air > 0;
        if (state.getValue(BlowingEngineBlock.ACTIVE) != active) level.setBlock(pos, state.setValue(BlowingEngineBlock.ACTIVE, active), Block.UPDATE_ALL);
        if (active && ++strokeTicks >= (air == 2 ? STROKE_TICKS : 2 * STROKE_TICKS)) {
            strokeTicks = 0;
            level.playSound(null, pos, Tier4Sounds.BLOWING_ENGINE_STROKE.get(), SoundSource.BLOCKS, 0.7f,
                    (air == 2 ? 1.0f : 0.85f) * (0.95f + level.getRandom().nextFloat() * 0.1f));
        }
    }

    public Component readout() {
        String key = StrataIndustria.MOD_ID + ".blowing_engine.";
        String bar = String.format(java.util.Locale.ROOT, "%.1f", pressure);
        if (air == 2) return Component.translatable(key + "full", bar);
        if (air == 1) return Component.translatable(key + "half", bar);
        return Component.translatable(key + "no_steam");
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        steam = in.getIntOr("steam", 0);
        air = in.getIntOr("air", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("steam", steam);
        out.putInt("air", air);
    }
}
