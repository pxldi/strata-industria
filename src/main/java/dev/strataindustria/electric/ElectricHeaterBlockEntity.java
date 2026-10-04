package dev.strataindustria.electric;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.heat.Heat;
import dev.strataindustria.heat.HeatEmitter;
import dev.strataindustria.heat.HeatNetwork;
import dev.strataindustria.heat.HeatPipeBlock;
import dev.strataindustria.heat.HeatPort;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.power.ElectricConsumer;
import dev.strataindustria.power.ElectricNetwork;
import dev.strataindustria.power.ElectricNetworks;
import dev.strataindustria.power.ElectricStats;
import dev.strataindustria.power.ElectricTier;
import dev.strataindustria.power.StatusLight;
import dev.strataindustria.registry.Tier5BlockEntities;
import dev.strataindustria.registry.Tier5Sounds;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Spec 7.6: a firebox that runs on electricity. It offers heat to the block above, to heat inlets and over
 * heat pipes, one HU for each J, up to 32 HU/t (LV, 1000 °C) or 128 HU/t (MV, 1700 °C). It draws only what its
 * consumers take, and climbs 20 °C a second toward its limit while something wants heat; that climb costs a
 * quarter of its full draw. With nothing wanting heat the coil cools at the same rate and draws nothing.
 */
public class ElectricHeaterBlockEntity extends BlockEntity implements ElectricConsumer, HeatPort, GeneratorBlock.Generator {
    public static final int LV_MAX_TEMPERATURE = 1000, MV_MAX_TEMPERATURE = 1700;
    /** 20 °C a second, per tick. */
    public static final float RATE = 1.0f;
    /** The coil glows, and the front shows it, from here. */
    public static final float GLOWS_FROM = 300.0f;
    /** Part of its full draw the climb costs. */
    private static final double CLIMB_COST = 0.25;
    private static final int SOUND_INTERVAL = 50;
    private static final ElectricStats FALLBACK = ElectricStats.standard(32);

    private final HeatEmitter emitter = new HeatEmitter();
    private float temperature = Heat.AMBIENT;
    private double buffer;
    private boolean wanting;
    private int taken;
    private int age;

    public ElectricHeaterBlockEntity(BlockPos pos, BlockState state) {
        super(Tier5BlockEntities.ELECTRIC_HEATER.get(), pos, state);
    }

    @Override
    public ElectricTier tier() {
        return getBlockState().getValue(GeneratorBlock.TIER);
    }

    private ElectricStats stats() {
        return ElectricStats.of(getBlockState().getBlock(), FALLBACK);
    }

    /** J/t at full output. */
    public int limit() {
        return stats().draw().get(tier());
    }

    public static int maxTemperature(ElectricTier tier) {
        return tier == ElectricTier.MV ? MV_MAX_TEMPERATURE : LV_MAX_TEMPERATURE;
    }

    public float temperature() {
        return temperature;
    }

    /** HU/t its consumers took last tick. */
    public int taken() {
        return taken;
    }

    // ------------------------------------------------------------------ power

    @Override
    public boolean connectsElectric(Direction side) {
        return true;
    }

    @Override
    public double request() {
        if (!wanting) return 0;
        return Math.max(0, limit() * (double) stats().bufferTicks() - buffer);
    }

    @Override
    public void receive(double amount) {
        buffer += amount;
    }

    // ------------------------------------------------------------------ heat

    @Override
    public boolean connectsHeat(Direction side) {
        return true;
    }

    @Override
    public void tick(ServerLevel level, BlockPos pos, BlockState state) {
        age++;
        ElectricNetwork.Report report = ElectricNetworks.report(level, pos);
        boolean fault = report.status().fault();
        float max = maxTemperature(tier());
        int limit = limit();
        emitter.refresh(level, pos);
        List<HeatNetwork.Route> targets = emitter.targets(level, pos);
        wanting = !fault && emitter.wants(level, targets, max);

        boolean climbing = false;
        if (wanting && buffer > 0) {
            double cost = limit * CLIMB_COST;
            if (temperature < max && buffer >= cost) {
                buffer -= cost;
                temperature = Math.min(max, temperature + RATE);
                climbing = true;
            }
        } else {
            temperature = Math.max(Heat.AMBIENT, temperature - RATE);
        }

        taken = 0;
        if (temperature > Heat.AMBIENT + 1) {
            int output = wanting ? (int) Math.min(limit, buffer) : 0;
            taken = Math.max(0, Math.min(output, HeatNetwork.deliver(level, targets, temperature, output)));
            buffer = Math.max(0, buffer - taken);
            if (taken > 0 && age % 100 == 0) Journal.awardNear(level, pos, Journal.ELECTRIC_HEAT);
        }
        emitter.glow(level, temperature >= HeatPipeBlock.GLOWS_FROM && (wanting || taken > 0));

        StatusLight light;
        if (fault) light = StatusLight.ERROR;
        else if (taken > 0 || climbing) light = StatusLight.RUN;
        else if (wanting || temperature > Heat.AMBIENT + 1) light = StatusLight.WAIT;
        else light = StatusLight.OFF;
        boolean glowing = temperature >= GLOWS_FROM;
        if (glowing && age % SOUND_INTERVAL == 0) {
            level.playSound(null, pos, Tier5Sounds.ELECTRIC_HEATER_RUN.get(), SoundSource.BLOCKS, 0.35f, tier() == ElectricTier.MV ? 1.15f : 1.0f);
        }
        BlockState next = state.setValue(GeneratorBlock.STATUS, light).setValue(GeneratorBlock.ACTIVE, glowing);
        if (next != state) level.setBlock(pos, next, Block.UPDATE_CLIENTS);
    }

    @Override
    public void animate(Level level, BlockPos pos, BlockState state, RandomSource random) {
        if (random.nextInt(12) != 0) return;
        level.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 1.02,
                pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0.0, 0.0, 0.0);
    }

    @Override
    public Component readout() {
        String key = StrataIndustria.MOD_ID + ".electric_heater.";
        if (temperature <= Heat.AMBIENT + 1) return Component.translatable(key + "idle");
        return Component.translatable(key + "heating", Math.round(temperature), taken, limit());
    }

    // ------------------------------------------------------------------ lifecycle

    @Override
    public void onLoad() {
        super.onLoad();
        ElectricNetworks.markDirty(level, worldPosition);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        ElectricNetworks.markDirty(level, worldPosition);
    }

    /** The pipes it was heating cool. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) emitter.release(level);
    }

    /** For game tests: start at this temperature without waiting for the climb. */
    public void preheat(float temperature) {
        this.temperature = temperature;
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        temperature = in.getFloatOr("temperature", Heat.AMBIENT);
        buffer = in.getDoubleOr("buffer", 0.0);
        emitter.setPipesHot(in.getBooleanOr("pipes_hot", false));
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putFloat("temperature", temperature);
        out.putDouble("buffer", buffer);
        out.putBoolean("pipes_hot", emitter.pipesHot());
    }
}
