package dev.strataindustria.steam;

import dev.strataindustria.Config;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.fluid.FluidPort;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.power.KineticBlockEntity;
import dev.strataindustria.power.KineticNetworks;
import dev.strataindustria.power.KineticSource;
import dev.strataindustria.registry.Tier4BlockEntities;
import dev.strataindustria.registry.Tier4Fluids;
import dev.strataindustria.registry.Tier4Sounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The steam engine (tier 4 spec 10.5): a kinetic source that turns at 32 RPM on steam at 2 bar or more
 * and 16 RPM from 1 to 2 bar, using 15 or 7.5 mB a tick. It takes steam only at 1 bar or more, so a cold
 * boiler builds pressure before the engine draws it down. Once at full speed it keeps it down to 1.75
 * bar, so it does not hunt between speeds at the edge.
 */
public class SteamEngineBlockEntity extends KineticBlockEntity implements KineticSource, FluidPort {
    public static final float FULL_SPEED = 32.0f, HALF_SPEED = 16.0f;
    public static final float FULL_PRESSURE = 2.0f, HOLD_PRESSURE = 1.75f, MIN_PRESSURE = 1.0f;
    /** mB per tick at full speed; half speed uses half, as 7 and 8 in turn. */
    public static final int FULL_USE = 15;
    /** Steam it holds between strokes. */
    public static final int BUFFER = 40;
    /** Ticks without steam before the pressure it last saw counts as gone. */
    private static final int STEAM_TIMEOUT = 5;

    private int steam;
    private float pressure;
    private int sinceSteam = STEAM_TIMEOUT;
    private float speed;
    private int age;
    private int strokeTicks;
    private boolean announced;

    public SteamEngineBlockEntity(BlockPos pos, BlockState state) {
        super(Tier4BlockEntities.STEAM_ENGINE.get(), pos, state);
    }

    private Direction facing() {
        return getBlockState().getValue(SteamEngineBlock.FACING);
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

    public static void serverTick(Level level, BlockPos pos, BlockState state, SteamEngineBlockEntity engine) {
        engine.tick((ServerLevel) level, pos);
    }

    private void tick(ServerLevel level, BlockPos pos) {
        age++;
        if (++sinceSteam > STEAM_TIMEOUT) pressure = 0;
        float target;
        if (pressure >= FULL_PRESSURE || speed >= FULL_SPEED && pressure >= HOLD_PRESSURE) target = FULL_SPEED;
        else if (pressure >= MIN_PRESSURE) target = HALF_SPEED;
        else target = 0;
        int use = target >= FULL_SPEED ? FULL_USE : target > 0 ? 7 + (age & 1) : 0;
        // Without steam for this stroke the engine stops; the buffer covers the gap between pushes.
        if (target > 0 && steam < use) target = steam >= 7 ? HALF_SPEED : 0;
        use = target >= FULL_SPEED ? FULL_USE : target > 0 ? Math.min(steam, 7 + (age & 1)) : 0;
        steam -= use;

        if (target != speed) {
            boolean started = speed == 0 && target > 0, stopped = speed > 0 && target == 0;
            speed = target;
            KineticNetworks.markDirty(level, pos);
            setChanged();
            if (started) {
                level.playSound(null, pos, Tier4Sounds.STEAM_ENGINE_START.get(), SoundSource.BLOCKS, 0.9f, 1.0f);
                if (!announced) {
                    announced = true;
                    Journal.awardNear(level, pos, Journal.STEAM_ENGINE);
                }
            }
            if (stopped) level.playSound(null, pos, Tier4Sounds.STEAM_ENGINE_STOP.get(), SoundSource.BLOCKS, 0.9f, 0.9f);
        }
        if (speed > 0 && kinetic().turning()) chuff(level, pos);
    }

    /** Two strokes a turn: a chuff and a puff from the exhaust at the back each time. */
    private void chuff(ServerLevel level, BlockPos pos) {
        float rpm = kinetic().rpm();
        int interval = Math.max(4, Math.round(600.0f / Math.max(1.0f, rpm)));
        if (++strokeTicks < interval) return;
        strokeTicks = 0;
        level.playSound(null, pos, Tier4Sounds.STEAM_ENGINE_CHUFF.get(), SoundSource.BLOCKS, 0.7f,
                (rpm >= 32 ? 1.2f : 1.0f) * (0.95f + level.getRandom().nextFloat() * 0.1f));
        Direction back = facing().getOpposite();
        level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5 + back.getStepX() * 0.3, pos.getY() + 0.95,
                pos.getZ() + 0.5 + back.getStepZ() * 0.3, 2, 0.05, 0.02, 0.05, 0.02);
    }

    @Override
    public float sourceSpeed() {
        return speed;
    }

    @Override
    public int capacity() {
        return Config.STEAM_ENGINE_CAPACITY.getAsInt();
    }

    /** Iron and steel throughout: it takes the iron transmission's speeds. */
    @Override
    public int speedLimit() {
        return dev.strataindustria.power.IronTransmission.SPEED_LIMIT;
    }

    @Override
    public @Nullable Component idleReason() {
        if (speed > 0) return null;
        String key = StrataIndustria.MOD_ID + ".steam_engine.";
        if (sinceSteam > STEAM_TIMEOUT * 4) return Component.translatable(key + "no_steam");
        return Component.translatable(key + "low_pressure");
    }

    /** Right-click: what drives it and how hard. */
    public Component report() {
        String key = StrataIndustria.MOD_ID + ".steam_engine.";
        if (speed <= 0) return idleReason() == null ? Component.translatable(key + "no_steam") : idleReason();
        return Component.translatable(key + "running", Math.round(speed), String.format(java.util.Locale.ROOT, "%.1f", pressure));
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        steam = in.getIntOr("steam", 0);
        speed = in.getFloatOr("speed", 0.0f);
        announced = in.getBooleanOr("announced", false);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("steam", steam);
        out.putFloat("speed", speed);
        out.putBoolean("announced", announced);
    }
}
