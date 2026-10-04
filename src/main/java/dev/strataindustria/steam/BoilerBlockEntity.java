package dev.strataindustria.steam;

import dev.strataindustria.Config;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.fluid.FluidPipes;
import dev.strataindustria.fluid.FluidPort;
import dev.strataindustria.heat.HeatConsumer;
import dev.strataindustria.heat.HeatPort;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.registry.Tier4BlockEntities;
import dev.strataindustria.registry.Tier4Blocks;
import dev.strataindustria.registry.Tier4Fluids;
import dev.strataindustria.registry.Tier4Menus;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
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
public class BoilerBlockEntity extends BlockEntity implements HeatConsumer, HeatPort, FluidPort, MenuProvider {
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
            DATA_TEMPERATURE = 5, DATA_HEAT = 6, DATA_WATER_CAPACITY = 7, DATA_STEAM_CAPACITY = 8, DATA_RATED = 9,
            DATA_PROBLEM = 10, DATA_WHERE = 11, DATA_COUNT = 12;

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
        DRY_FIRING,
        /** A steel boiler whose structure is not built. */
        INCOMPLETE;

        public String key() {
            return StrataIndustria.MOD_ID + ".boiler.status." + name().toLowerCase(Locale.ROOT);
        }

        /** Warnings show red on the screen. */
        public boolean warning() {
            return this == PIPE_TOO_HOT || this == LOW_WATER || this == DRY_FIRING || this == NO_WATER || this == INCOMPLETE;
        }
    }

    protected int water;
    protected int steam;
    private float warmth;
    private float integrity = 100.0f;
    protected Status status = Status.NO_HEAT;
    private boolean announced;

    // Heat offered by the firebox since the last tick, and how hot it was.
    private int pendingHeat;
    private float offeredTemperature;
    private int lastHeat;
    private float lastTemperature;
    private int ticksWithoutHeat = COLD_AFTER;
    private int dryTicks;
    private int burstCooldown;
    protected int age;

    private FluidPipes.Network network = FluidPipes.Network.NONE;
    private int networkAge = 20;

    protected final ContainerData data = new ContainerData() {
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
                case DATA_WATER_CAPACITY -> waterCapacity();
                case DATA_STEAM_CAPACITY -> steamCapacity();
                case DATA_RATED -> Math.round(ratedPressure() * 10);
                case DATA_PROBLEM -> problem();
                case DATA_WHERE -> problemWhere();
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
        this(Tier4BlockEntities.BRONZE_BOILER.get(), pos, state);
    }

    protected BoilerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // Hooks for the steel boiler (spec 10.3), which grows with its shell layers and takes heat and water
    // through its parts.

    public int waterCapacity() {
        return WATER_CAPACITY;
    }

    public int steamCapacity() {
        return STEAM_CAPACITY;
    }

    public float ratedPressure() {
        return RATED_PRESSURE;
    }

    public int maxHeat() {
        return MAX_HEAT;
    }

    /** Whether it can work at all; a steel boiler needs its structure built. */
    protected boolean ready() {
        return true;
    }

    /** Which faces take water by pipe. */
    protected boolean takesWater(Direction side) {
        return side != Direction.UP;
    }

    /** Sends steam on to the pipes; the bronze boiler's leave through its top. */
    protected FluidPipes.Push pushSteam(ServerLevel level, BlockPos pos, int amount) {
        if (++networkAge >= 20) {
            network = FluidPipes.find(level, pos, Direction.UP);
            networkAge = 0;
        }
        FluidPipes.Push push = FluidPipes.push(level, network, Tier4Fluids.STEAM.get(), amount, steamTemperature(), pressure());
        if (push.refused() && age % 20 == 0 && network.firstPipe() != null) dev.strataindustria.fluid.FluidPipeBlock.refuseSound(level, network.firstPipe());
        return push;
    }

    /** Where the safety valve lets steam out. */
    protected BlockPos valve() {
        return worldPosition;
    }

    /** What is left when it cracks. */
    protected BlockState cracked(BlockState state) {
        return Tier4Blocks.CRACKED_BRONZE_BOILER.get().defaultBlockState().setValue(BoilerBlock.FACING, state.getValue(BoilerBlock.FACING));
    }

    /** Called after each tick's status is set. */
    protected void afterTick(ServerLevel level, BlockPos pos) {}

    protected int problem() {
        return 0;
    }

    protected int problemWhere() {
        return 0;
    }

    protected String containerName() {
        return "bronze_boiler";
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
        return ratedPressure() * steam / steamCapacity();
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
    public int heatDemand(float temperature) {
        return temperature < MIN_TEMPERATURE || !ready() ? 0 : Math.max(0, maxHeat() - pendingHeat);
    }

    /** Heat pipes join it on any face. */
    @Override
    public boolean connectsHeat(Direction side) {
        return true;
    }

    /** The temperature and HU it was offered last tick, for tests. */
    public float heatTemperature() {
        return lastTemperature;
    }

    public int heatTaken() {
        return lastHeat;
    }

    @Override
    public int offerHeat(float temperature, int heat) {
        offeredTemperature = Math.max(offeredTemperature, temperature);
        if (temperature < MIN_TEMPERATURE || !ready()) return 0;
        int take = Math.max(0, Math.min(heat, maxHeat() - pendingHeat));
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
        if (!takesWater(side)) return 0;
        return fillWater(fluid, amount, simulate);
    }

    /** Takes water, from a pipe or a steel boiler's water port. */
    public int fillWater(Fluid fluid, int amount, boolean simulate) {
        if (!fluid.isSame(Fluids.WATER) || amount <= 0 || !ready()) return 0;
        int take = Math.min(amount, waterCapacity() - water);
        if (take <= 0 || simulate) return Math.max(0, take);
        addWater(take);
        return take;
    }

    /** Water from a pipe or a bucket. On a dry-fired boiler it flashes to steam with a bang (spec 10.4). */
    public void addWater(int amount) {
        if (level == null) return;
        if (dryTicks >= 20 && burstCooldown <= 0) burst();
        water = Math.min(waterCapacity(), water + amount);
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
        if (!ready()) {
            // A steel boiler with its structure broken holds what it has and does nothing.
            status = Status.INCOMPLETE;
            afterTick(level, pos);
            if (status != before) sync();
            return;
        }
        float share = water / (float) waterCapacity();
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
            FluidPipes.Push push = pushSteam(level, pos, Math.min(steam, MAX_PUSH * Math.max(1, maxHeat() / MAX_HEAT)));
            steam -= push.moved();
            refused = push.refused();
        }
        boolean venting = steam > steamCapacity();
        if (venting) {
            steam = steamCapacity();
            BlockPos valve = valve();
            if (age % 30 == 0) level.playSound(null, valve, Tier4Sounds.BOILER_VENT.get(), SoundSource.BLOCKS, 1.0f, 0.95f + level.getRandom().nextFloat() * 0.1f);
            if (age % 2 == 0) level.sendParticles(ParticleTypes.CLOUD, valve.getX() + 0.5, valve.getY() + 1.1, valve.getZ() + 0.5, 3, 0.08, 0.1, 0.08, 0.06);
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

        afterTick(level, pos);
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
        level.setBlock(pos, cracked(state), Block.UPDATE_ALL);
        if (Config.STEAM_BOILER_EXPLOSIONS.get()) {
            level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3.0f, Level.ExplosionInteraction.BLOCK);
        }
    }

    protected void sync() {
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
        return Component.translatable("container." + StrataIndustria.MOD_ID + "." + containerName());
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return menu(id, inventory);
    }

    protected AbstractContainerMenu menu(int id, Inventory inventory) {
        return new BoilerMenu(Tier4Menus.BRONZE_BOILER.get(), id, inventory, worldPosition, data);
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
