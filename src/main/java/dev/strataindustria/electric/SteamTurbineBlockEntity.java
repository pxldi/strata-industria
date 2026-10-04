package dev.strataindustria.electric;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.fluid.FluidPort;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.power.ElectricNetwork;
import dev.strataindustria.power.ElectricNetworks;
import dev.strataindustria.power.ElectricSource;
import dev.strataindustria.power.ElectricTier;
import dev.strataindustria.power.StatusLight;
import dev.strataindustria.registry.Tier4Fluids;
import dev.strataindustria.registry.Tier5BlockEntities;
import dev.strataindustria.registry.Tier5Sounds;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Spec 7.2: steam in at the back, 2 J per mB out. LV takes up to 16 mB/t for 32 J/t at 2 bar or more and
 * half of that from 1 to 2 bar; MV takes 64 mB/t for 128 J/t at 4 bar or more and half from 2 to 4 bar.
 * It draws only the steam the network asks for, and its output limit climbs over 200 ticks from rest and
 * falls over 100 ticks once the steam stops.
 */
public class SteamTurbineBlockEntity extends BlockEntity implements ElectricSource, FluidPort, GeneratorBlock.Generator {
    public static final double JOULES_PER_MB = 2.0;
    public static final int RISE_TICKS = 200, FALL_TICKS = 100;
    /** Ticks without an offer of steam before the pressure it last saw counts as gone. */
    private static final int STEAM_TIMEOUT = 5;
    /** Spin below which the rotor counts as at rest. */
    private static final float REST = 0.01f;
    /** Ticks between rotor sounds. */
    private static final int SOUND_INTERVAL = 60;

    private double steam;
    private float pressure;
    private int sinceSteam = STEAM_TIMEOUT;
    private float spin;
    private boolean supplied;
    private double produced;
    private int age;

    public SteamTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(Tier5BlockEntities.STEAM_TURBINE.get(), pos, state);
    }

    /** mB/t at full output: 16 for LV, 64 for MV. */
    public static int maxUse(ElectricTier tier) {
        return tier == ElectricTier.MV ? 64 : 16;
    }

    /** Bar for full output: 2 for LV, 4 for MV. */
    public static float fullPressure(ElectricTier tier) {
        return tier == ElectricTier.MV ? 4.0f : 2.0f;
    }

    /** Bar below which it takes no steam: 1 for LV, 2 for MV. */
    public static float minPressure(ElectricTier tier) {
        return fullPressure(tier) / 2;
    }

    @Override
    public ElectricTier tier() {
        return getBlockState().getValue(GeneratorBlock.TIER);
    }

    private Direction back() {
        return getBlockState().getValue(GeneratorBlock.FACING).getOpposite();
    }

    /** Spin, 0 to 1: the share of full output it can make. */
    public float spin() {
        return spin;
    }

    /** Sets the spin directly, for game tests. */
    public void setSpin(float spin) {
        this.spin = Math.max(0, Math.min(1, spin));
    }

    public float pressure() {
        return pressure;
    }

    // ------------------------------------------------------------------ steam

    @Override
    public boolean connectsFluid(Direction side) {
        return side == back();
    }

    @Override
    public int fill(Direction side, Fluid fluid, int amount, float pressure, boolean simulate) {
        if (side != back() || !fluid.isSame(Tier4Fluids.STEAM.get())) return 0;
        // Whatever it is offered tells it the pressure, even when its buffer is full.
        this.pressure = pressure;
        sinceSteam = 0;
        if (pressure < minPressure(tier())) return 0;
        int take = (int) Math.max(0, Math.min(amount, Math.floor(buffer() - steam)));
        if (!simulate) steam += take;
        return take;
    }

    /** Two ticks of full use. */
    private double buffer() {
        return maxUse(tier()) * 2;
    }

    // ------------------------------------------------------------------ power

    @Override
    public boolean connectsElectric(Direction side) {
        return side != back();
    }

    @Override
    public double maxOutput() {
        ElectricTier tier = tier();
        double cap = pressure >= fullPressure(tier) ? tier.maxPower() : pressure >= minPressure(tier) ? tier.maxPower() / 2.0 : 0.0;
        return Math.max(0, Math.min(Math.min(tier.maxPower() * spin, cap), steam * JOULES_PER_MB));
    }

    @Override
    public void extract(double amount) {
        produced = amount;
        steam = Math.max(0, steam - amount / JOULES_PER_MB);
    }

    @Override
    public void tick(ServerLevel level, BlockPos pos, BlockState state) {
        age++;
        ElectricTier tier = tier();
        if (++sinceSteam > STEAM_TIMEOUT) pressure = 0;
        boolean wasSupplied = supplied;
        supplied = pressure >= minPressure(tier);
        float before = spin;
        spin = supplied ? Math.min(1.0f, spin + 1.0f / RISE_TICKS) : Math.max(0.0f, spin - 1.0f / FALL_TICKS);
        if (spin != before) setChanged();
        if (spin >= 1.0f && pressure >= fullPressure(tier) && age % 100 == 0) Journal.awardNear(level, pos, Journal.TURBINE);

        float pitch = (0.6f + 0.8f * spin) + (tier == ElectricTier.MV ? 0.1f : 0.0f);
        if (spin > REST && age % SOUND_INTERVAL == 0) {
            level.playSound(null, pos, Tier5Sounds.STEAM_TURBINE_RUN.get(), SoundSource.BLOCKS, 0.25f + 0.55f * spin, pitch);
        }
        if (wasSupplied && !supplied && spin > 0.2f) {
            level.playSound(null, pos, Tier5Sounds.STEAM_TURBINE_SPIN_DOWN.get(), SoundSource.BLOCKS, 0.4f + 0.5f * spin, pitch);
            Direction back = back();
            level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5 + back.getStepX() * 0.6, pos.getY() + 0.6, pos.getZ() + 0.5 + back.getStepZ() * 0.6,
                    6, 0.1, 0.1, 0.1, 0.02);
        }

        ElectricNetwork.Report report = ElectricNetworks.report(level, pos);
        StatusLight light;
        if (report.status().fault()) light = StatusLight.ERROR;
        else if (spin <= REST && !supplied) light = StatusLight.OFF;
        else light = produced > 0 ? StatusLight.RUN : StatusLight.WAIT;
        produced = 0;
        BlockState next = state.setValue(GeneratorBlock.STATUS, light).setValue(GeneratorBlock.ACTIVE, spin > REST);
        if (next != state) level.setBlock(pos, next, Block.UPDATE_CLIENTS);
    }

    @Override
    public Component readout() {
        ElectricTier tier = tier();
        String key = StrataIndustria.MOD_ID + ".steam_turbine.";
        if (pressure < minPressure(tier)) {
            return Component.translatable(key + (spin > REST ? "spinning_down" : "no_steam"), String.format(Locale.ROOT, "%.0f", minPressure(tier)));
        }
        return Component.translatable(key + "running", Math.round(spin * 100), String.format(Locale.ROOT, "%.1f", pressure),
                String.format(Locale.ROOT, "%.0f", maxOutput()));
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

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        spin = in.getFloatOr("spin", 0.0f);
        steam = in.getDoubleOr("steam", 0.0);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putFloat("spin", spin);
        out.putDouble("steam", steam);
    }
}
