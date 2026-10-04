package dev.strataindustria.steam;

import dev.strataindustria.Config;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.fluid.FluidPipes;
import dev.strataindustria.fluid.FluidPort;
import dev.strataindustria.heat.HeatConsumer;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.registry.Tier4BlockEntities;
import dev.strataindustria.registry.Tier4Blocks;
import dev.strataindustria.registry.Tier4Fluids;
import dev.strataindustria.registry.Tier4Sounds;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The bronze boiler (tier 4 spec 10.2 and 10.4). Sits on a firebox, takes up to 30 HU a tick at 300 °C
 * or more and turns it into up to 15 mB of steam, after a warm-up of 600 HU per bucket of water.
 * Pressure is the rated 4 bar times how full the steam buffer is. Steam leaves through the top; a full
 * buffer vents. Water comes in by pipe on the other faces or by bucket. Firing it nearly dry costs
 * integrity, and at zero it cracks.
 */
public class BoilerBlockEntity extends BlockEntity implements HeatConsumer, FluidPort, MenuProvider {
    public static final int WATER_CAPACITY = 8000;
    public static final int STEAM_CAPACITY = 4000;
    public static final float RATED_PRESSURE = 4.0f;
    public static final int MAX_HEAT = 30;
    public static final int MIN_TEMPERATURE = 300;
    /** HU that warm 1000 mB of cold water to the boil. */
    public static final int WARM_UP_PER_BUCKET = 600;
    /** Ticks without heat after which the water is cold again. */
    public static final int COLD_AFTER = 1200;
    public static final float LOW_WATER = 0.25f, DRY = 0.10f;
    /** Integrity lost when water hits a dry-fired boiler. */
    public static final float BURST_DAMAGE = 20.0f;
    public static final float PLATE_REPAIR = 10.0f;
    /** Steam leaving per tick at most, before the pipes' own limit. */
    public static final int MAX_PUSH = 400;

    public static final int DATA_WATER = 0, DATA_STEAM = 1, DATA_WARMTH = 2, DATA_INTEGRITY = 3, DATA_STATUS = 4,
            DATA_TEMPERATURE = 5, DATA_HEAT = 6, DATA_COUNT = 7;

    public enum Status {
        /** No heat coming in. */
        NO_HEAT,
        /** Heat comes in, but below 300 °C. */
        TOO_COOL,
        NO_WATER,
        /** The first 600 HU per bucket. */
        HEATING,
        RUNNING,
        /** Buffer full: the safety valve lets the excess go. */
        VENTING,
        /** The pipe on top cannot take steam this hot. */
        PIPE_TOO_HOT,
        LOW_WATER,
        DRY_FIRING;

        public String key() {
            return StrataIndustria.MOD_ID + ".boiler.status." + name().toLowerCase(Locale.ROOT);
        }

        /** Warnings show red on the screen. */
        public boolean warning() {
            return this == PIPE_TOO_HOT || this == LOW_WATER || this == DRY_FIRING || this == NO_WATER;
        }
    }

    private int water;
    private int steam;
    private float warmth;
    private float integrity = 100.0f;
    private Status status = Status.NO_HEAT;
    private boolean announced;

    // Heat offered by the firebox since the last tick, and how hot it was.
    private int pendingHeat;
    private float offeredTemperature;
    private int lastHeat;
    private float lastTemperature;
    private int ticksWithoutHeat = COLD_AFTER;
    private int dryTicks;
    private int burstCooldown;
    private int age;

    private FluidPipes.Network network = FluidPipes.Network.NONE;
    private int networkAge = Integer.MAX_VALUE;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_WATER -> water;
                case DATA_STEAM -> steam;
                case DATA_WARMTH -> Math.round(warmthShare() * 100);
                case DATA_INTEGRITY -> Math.round(integrity * 10);
                case DATA_STATUS -> status.ordinal();
                case DATA_TEMPERATURE -> Math.round(lastTemperature);
                case DATA_HEAT -> lastHeat;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public BoilerBlockEntity(BlockPos pos, BlockState state) {
        super(Tier4BlockEntities.BRONZE_BOILER.get(), pos, state);
    }

    public int water() {
        return water;
    }

    public int steam() {
        return steam;
    }

    public float integrity() {
        return integrity;
    }

    public Status status() {
        return status;
    }

    /** Bar: the rated pressure times how full the steam buffer is (spec 10.1). */
    public float pressure() {
        return RATED_PRESSURE * steam / STEAM_CAPACITY;
    }

    /** Spec 9.1: steam is 100 °C plus 15 per bar. */
    public float steamTemperature() {
        return 100.0f + 15.0f * pressure();
    }

    private float warmUpNeeded() {
        return water * (float) WARM_UP_PER_BUCKET / 1000.0f;
    }

    private float warmthShare() {
        float need = warmUpNeeded();
        return need <= 0 ? 0 : Math.min(1.0f, warmth / need);
    }

    @Override
    public int offerHeat(float temperature, int heat) {
        offeredTemperature = Math.max(offeredTemperature, temperature);
        if (temperature < MIN_TEMPERATURE) return 0;
        int take = Math.max(0, Math.min(heat, MAX_HEAT - pendingHeat));
        pendingHeat += take;
        return take;
    }

    @Override
    public boolean connectsFluid(Direction side) {
        return true;
    }

    /** Water on any face but the top; the top is the steam outlet. */
    @Override
    public int fill(Direction side, Fluid fluid, int amount, float pressure, boolean simulate) {
        if (side == Direction.UP || !fluid.isSame(Fluids.WATER) || amount <= 0) return 0;
        int take = Math.min(amount, WATER_CAPACITY - water);
        if (take <= 0 || simulate) return Math.max(0, take);
        addWater(take);
        return take;
    }

    /** Water from a pipe or a bucket. On a dry-fired boiler it flashes to steam with a bang (spec 10.4). */
    public void addWater(int amount) {
        if (level == null) return;
        if (dryTicks >= 20 && burstCooldown <= 0) burst();
        water = Math.min(WATER_CAPACITY, water + amount);
        setChanged();
    }

    /** Spec 10.4: a plate of bronze patches 10% back. Returns whether it did. */
    public boolean repair() {
        if (integrity >= 100.0f || level == null) return false;
        integrity = Math.min(100.0f, integrity + PLATE_REPAIR);
        level.playSound(null, worldPosition, Tier4Sounds.BOILER_REPAIR.get(), SoundSource.BLOCKS, 0.9f, 0.95f + level.getRandom().nextFloat() * 0.1f);
        sync();
        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BoilerBlockEntity boiler) {
        boiler.tick((ServerLevel) level, pos);
    }

    private void tick(ServerLevel level, BlockPos pos) {
        age++;
        int heat = pendingHeat;
        pendingHeat = 0;
        lastHeat = heat;
        lastTemperature = offeredTemperature;
        offeredTemperature = 0;
        if (burstCooldown > 0) burstCooldown--;
        if (heat > 0) ticksWithoutHeat = 0;
        else if (++ticksWithoutHeat >= COLD_AFTER) warmth = 0;

        Status before = status;
        float share = water / (float) WATER_CAPACITY;
        boolean dry = heat > 0 && share < DRY;
        if (dry) {
            dryTicks++;
            integrity -= Config.STEAM_DRY_FIRING_DAMAGE.get().floatValue() / 20.0f;
            if (age % 40 == 0) level.playSound(null, pos, Tier4Sounds.BOILER_DRY_FIRE.get(), SoundSource.PLAYERS, 0.8f, 0.9f + level.getRandom().nextFloat() * 0.2f);
            if (age % 4 == 0) {
                level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 2, 0.3, 0.05, 0.3, 0.01);
                level.sendParticles(ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 1, 0.4, 0.3, 0.4, 0.0);
            }
            if (integrity <= 0) {
                crack(level, pos);
                return;
            }
        } else {
            dryTicks = 0;
        }

        int made = 0;
        if (heat > 0 && water > 0) {
            float need = warmUpNeeded();
            if (warmth < need) {
                warmth = Math.min(need, warmth + heat);
            } else {
                made = Math.min(heat / 2, water);
                water -= made;
                steam += made;
            }
        }

        boolean refused = false;
        if (steam > 0) {
            if (++networkAge >= 20) {
                network = FluidPipes.find(level, pos, Direction.UP);
                networkAge = 0;
            }
            FluidPipes.Push push = FluidPipes.push(level, network, Tier4Fluids.STEAM.get(), Math.min(steam, MAX_PUSH), steamTemperature(), pressure());
            steam -= push.moved();
            refused = push.refused();
            if (refused && age % 20 == 0 && network.firstPipe() != null) dev.strataindustria.fluid.FluidPipeBlock.refuseSound(level, network.firstPipe());
        }
        boolean venting = steam > STEAM_CAPACITY;
        if (venting) {
            steam = STEAM_CAPACITY;
            if (age % 30 == 0) level.playSound(null, pos, Tier4Sounds.BOILER_VENT.get(), SoundSource.BLOCKS, 1.0f, 0.95f + level.getRandom().nextFloat() * 0.1f);
            if (age % 2 == 0) level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 3, 0.08, 0.1, 0.08, 0.06);
        }

        if (heat <= 0) status = water <= 0 ? Status.NO_WATER : lastTemperature > 0 ? Status.TOO_COOL : Status.NO_HEAT;
        else if (dry) status = Status.DRY_FIRING;
        else if (refused) status = Status.PIPE_TOO_HOT;
        else if (venting) status = Status.VENTING;
        else if (share < LOW_WATER) status = Status.LOW_WATER;
        else if (made == 0) status = Status.HEATING;
        else status = Status.RUNNING;

        if (status == Status.HEATING && age % 60 == 0) play(level, pos, Tier4Sounds.BOILER_HEAT.get(), SoundSource.BLOCKS, 0.6f);
        if (made > 0 && age % 50 == 0) play(level, pos, Tier4Sounds.BOILER_RUN.get(), SoundSource.BLOCKS, 0.7f);
        if (status == Status.LOW_WATER && age % 100 == 0) play(level, pos, Tier4Sounds.BOILER_LOW_WATER.get(), SoundSource.PLAYERS, 0.6f);

        if (!announced && pressure() >= 1.0f) {
            announced = true;
            Journal.awardNear(level, pos, Journal.BOILER);
        }
        if (status != before) sync();
        else setChanged();
    }

    private void play(Level level, BlockPos pos, SoundEvent sound, SoundSource source, float volume) {
        level.playSound(null, pos, sound, source, volume, 0.9f + level.getRandom().nextFloat() * 0.2f);
    }

    private void burst() {
        if (!(level instanceof ServerLevel server)) return;
        integrity -= BURST_DAMAGE;
        burstCooldown = 600;
        dryTicks = 0;
        BlockPos pos = worldPosition;
        server.playSound(null, pos, Tier4Sounds.BOILER_STEAM_BURST.get(), SoundSource.BLOCKS, 1.5f, 0.9f + server.getRandom().nextFloat() * 0.2f);
        server.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 40, 0.6, 0.6, 0.6, 0.12);
        server.sendParticles(ParticleTypes.POOF, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 15, 0.5, 0.4, 0.5, 0.05);
        if (integrity <= 0) crack(server, pos);
    }

    /** Spec 10.4: the shell splits and the boiler is done; with explosions on, it takes the room with it. */
    private void crack(ServerLevel level, BlockPos pos) {
        BlockState state = getBlockState();
        level.playSound(null, pos, Tier4Sounds.BOILER_CRACK.get(), SoundSource.BLOCKS, 1.2f, 1.0f);
        level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 30, 0.5, 0.5, 0.5, 0.1);
        level.setBlock(pos, Tier4Blocks.CRACKED_BRONZE_BOILER.get().defaultBlockState()
                .setValue(BoilerBlock.FACING, state.getValue(BoilerBlock.FACING)), Block.UPDATE_ALL);
        if (Config.STEAM_BOILER_EXPLOSIONS.get()) {
            level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3.0f, Level.ExplosionInteraction.BLOCK);
        }
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    /** For game tests: a boiler already full of hot water. */
    public void prime(int water, boolean warm) {
        this.water = water;
        this.warmth = warm ? warmUpNeeded() : 0;
        this.ticksWithoutHeat = 0;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container." + StrataIndustria.MOD_ID + ".bronze_boiler");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new BoilerMenu(id, inventory, worldPosition, data);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        water = in.getIntOr("water", 0);
        steam = in.getIntOr("steam", 0);
        warmth = in.getFloatOr("warmth", 0.0f);
        integrity = in.getFloatOr("integrity", 100.0f);
        announced = in.getBooleanOr("announced", false);
        int s = in.getIntOr("status", 0);
        status = s >= 0 && s < Status.values().length ? Status.values()[s] : Status.NO_HEAT;
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("water", water);
        out.putInt("steam", steam);
        out.putFloat("warmth", warmth);
        out.putFloat("integrity", integrity);
        out.putBoolean("announced", announced);
        out.putInt("status", status.ordinal());
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
