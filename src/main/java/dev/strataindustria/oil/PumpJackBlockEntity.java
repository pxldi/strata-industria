package dev.strataindustria.oil;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.power.KineticBlockEntity;
import dev.strataindustria.power.KineticConsumer;
import dev.strataindustria.registry.Tier6BlockEntities;
import dev.strataindustria.registry.Tier6Sounds;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The pump jack (tier 6 spec 5.4). It lifts 0.5 mB per tick per RPM (64 RPM at most) times the reservoir's
 * pressure factor, which is 1 down to half full and falls to a quarter at empty; it never stops. What it lifts
 * goes into the wellhead beneath it and out into whatever pipes and tanks the wellhead has.
 */
public class PumpJackBlockEntity extends KineticBlockEntity implements KineticConsumer {
    public static final int IMPACT = 16, MIN_SPEED = 16, MAX_SPEED = 64;
    /** mB per tick per RPM at full pressure. */
    public static final float RATE_PER_RPM = 0.5f;
    /** Stroke period in ticks is this over the RPM: one second at 32 RPM. */
    public static final float STROKE_TICKS_RPM = 640.0f;

    public enum Status {
        NO_WELLHEAD, NO_ROOM, NOT_TURNING, TOO_SLOW, NO_OUTLET, PUMPING, STRIPPER;

        public String key() {
            return StrataIndustria.MOD_ID + ".pump_jack.status." + name().toLowerCase(Locale.ROOT);
        }
    }

    private Status status = Status.NOT_TURNING;
    private float carry;
    private float strokes;
    private int lastMoved;
    private int age;
    /** Where the beam is in its stroke, in cycles; only the client uses it. */
    private float phase;

    public PumpJackBlockEntity(BlockPos pos, BlockState state) {
        super(Tier6BlockEntities.PUMP_JACK.get(), pos, state);
    }

    /** Spec 5.4: 1.0 down to half full, then falling in a line to 0.25 at empty. */
    public static double pressure(double fraction) {
        return fraction >= 0.5 ? 1.0 : 0.25 + 0.75 * (Math.max(0, fraction) / 0.5);
    }

    /** mB per tick at a speed and pressure. */
    public static float rate(float rpm, double fraction) {
        return RATE_PER_RPM * Math.min(rpm, MAX_SPEED) * (float) pressure(fraction);
    }

    @Override
    public int impact() {
        return IMPACT;
    }

    @Override
    public int minSpeed() {
        return MIN_SPEED;
    }

    public Status status() {
        return status;
    }

    /** mB it gave the wellhead last tick. */
    public int lastMoved() {
        return lastMoved;
    }

    public float phase(float partialTick) {
        return phase + partialTick * kinetic().rpm() / STROKE_TICKS_RPM;
    }

    public Component report() {
        if (status == Status.PUMPING) return Component.translatable(status.key(), lastMoved);
        return Component.translatable(status.key());
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PumpJackBlockEntity jack) {
        Status before = jack.status;
        jack.status = jack.work((ServerLevel) level, pos);
        boolean running = jack.status == Status.PUMPING || jack.status == Status.STRIPPER;
        if (state.getValue(PumpJackBlock.RUNNING) != running) level.setBlock(pos, state.setValue(PumpJackBlock.RUNNING, running), Block.UPDATE_ALL);
        if (jack.status != before) jack.setChanged();
    }

    private Status work(ServerLevel level, BlockPos pos) {
        lastMoved = 0;
        if (!(level.getBlockEntity(pos.below()) instanceof WellheadBlockEntity well) || !well.drilled() || well.reservoir() == null) {
            carry = 0;
            return Status.NO_WELLHEAD;
        }
        if (age++ % 20 == 0 && !PumpJackBlock.roomFor(level, pos, getBlockState().getValue(PumpJackBlock.FACING))) return Status.NO_ROOM;
        float rpm = kinetic().rpm();
        if (rpm <= 0) return Status.NOT_TURNING;
        if (rpm < MIN_SPEED) return Status.TOO_SLOW;
        double fraction = OilReservoirData.get(level).fraction(well.reservoir());
        // A blocked outlet does not store up a burst for later.
        float perTick = rate(rpm, fraction);
        carry = Math.min(carry + perTick, perTick + 1.0f);
        int amount = (int) carry;
        int moved = amount > 0 ? well.pump(level, amount) : 0;
        carry -= moved;
        lastMoved = moved;
        if (amount > 0 && moved == 0) return Status.NO_OUTLET;
        strokes += rpm / STROKE_TICKS_RPM;
        if (strokes >= 1.0f) {
            strokes -= 1.0f;
            level.playSound(null, pos, Tier6Sounds.PUMP_JACK_STROKE.get(), SoundSource.BLOCKS, 1.0f, 0.9f + rpm / 320.0f);
        }
        return fraction <= 0.0 ? Status.STRIPPER : Status.PUMPING;
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, PumpJackBlockEntity jack) {
        if (state.getValue(PumpJackBlock.RUNNING)) jack.phase = (jack.phase + jack.kinetic().rpm() / STROKE_TICKS_RPM) % 1.0f;
    }
}
