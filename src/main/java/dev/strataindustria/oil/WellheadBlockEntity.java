package dev.strataindustria.oil;

import dev.strataindustria.Config;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.fluid.FluidPipes;
import dev.strataindustria.fluid.FluidPort;
import dev.strataindustria.power.KineticConsumer;
import dev.strataindustria.power.KineticNetworks;
import dev.strataindustria.power.KineticState;
import dev.strataindustria.registry.Tier4Blocks;
import dev.strataindustria.registry.Tier6BlockEntities;
import dev.strataindustria.registry.Tier6Blocks;
import dev.strataindustria.registry.Tier6Fluids;
import dev.strataindustria.registry.Tier6Particles;
import dev.strataindustria.registry.Tier6Sounds;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The wellhead (tier 6 spec 5.3). Turning it drills a bore down to the top of the reservoir under it, one block
 * per 40 ticks at 16 RPM, and it eats one steel fluid pipe of casing for every four blocks. When the bore
 * reaches the reservoir the well gushes and from then on it either flows by itself, while the reservoir is
 * 80% full or more, or gives what a pump jack on top of it lifts. Whatever comes up goes into pipes and tanks
 * on its faces, and only what they take is drawn from the reservoir.
 */
public class WellheadBlockEntity extends BaseContainerBlockEntity implements KineticConsumer, FluidPort {
    public static final int IMPACT = 16, MIN_SPEED = 16, CASING = 0;
    public static final int TICKS_PER_BLOCK = 40, BLOCKS_PER_PIPE = 4, GUSH_TICKS = 100, NATURAL_FLOW = 4;
    /** Spec 5.3: a reservoir this full still pushes crude up by itself. */
    public static final double NATURAL_LEVEL = 0.8;
    public static final int DATA_STATUS = 0, DATA_BORED = 1, DATA_DEPTH = 2, DATA_REMAINING = 3, DATA_NEAR_X = 4, DATA_NEAR_Z = 5, DATA_RATE = 6,
            DATA_COUNT = 7;

    public enum Status {
        NO_RESERVOIR, TOO_CLOSE, NOT_TURNING, TOO_SLOW, NEEDS_CASING, DRILLING, GUSHING, FLOWING, LOW_PRESSURE, PUMPING, NO_OUTLET;

        public String key() {
            return StrataIndustria.MOD_ID + ".wellhead.status." + name().toLowerCase(Locale.ROOT);
        }
    }

    private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private final KineticState kinetic = new KineticState();
    private Status status = Status.NO_RESERVOIR;
    /** Blocks drilled, and blocks of casing that are down the bore. */
    private int bored, cased;
    private float progress;
    private int gush;
    private long lastPumped = -1000;
    private int rate;
    private int nearX, nearZ;
    private boolean loaded;
    private @Nullable OilReservoir reservoir;
    private int reservoirAge = Integer.MAX_VALUE;
    private int percent = -1;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_STATUS -> status.ordinal();
                case DATA_BORED -> bored;
                case DATA_DEPTH -> reservoir == null ? 0 : depth(reservoir);
                case DATA_REMAINING -> percent;
                case DATA_NEAR_X -> nearX;
                case DATA_NEAR_Z -> nearZ;
                case DATA_RATE -> rate;
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

    public WellheadBlockEntity(BlockPos pos, BlockState state) {
        super(Tier6BlockEntities.WELLHEAD.get(), pos, state);
    }

    // ------------------------------------------------------------------ tick

    public static void serverTick(Level level, BlockPos pos, BlockState state, WellheadBlockEntity well) {
        ServerLevel server = (ServerLevel) level;
        Status before = well.status;
        well.status = well.work(server, pos, state);
        if (well.status != before) well.setChanged();
    }

    private Status work(ServerLevel level, BlockPos pos, BlockState state) {
        rate = 0;
        if (reservoirAge++ >= 100) {
            reservoir = OilReservoirs.at(level, ChunkPos.containing(pos)).orElse(null);
            reservoirAge = 0;
        }
        OilReservoir res = reservoir;
        if (res == null) {
            percent = -1;
            return Status.NO_RESERVOIR;
        }
        OilReservoirData oil = OilReservoirData.get(level);
        percent = (int) Math.round(oil.fraction(res) * 100);
        if (!loaded) restore(level, pos, state, res, oil);
        return state.getValue(WellheadBlock.DRILLED) ? operate(level, pos, res, oil) : drill(level, pos, state, res, oil);
    }

    /** The bore lives with the reservoir: a wellhead put back on a column picks up where the last one stopped. */
    private void restore(ServerLevel level, BlockPos pos, BlockState state, OilReservoir res, OilReservoirData oil) {
        loaded = true;
        Optional<OilReservoirData.Bore> bore = oil.bore(res, pos.getX(), pos.getZ());
        if (bore.isEmpty()) return;
        bored = Math.max(bored, bore.get().depth());
        cased = Math.max(cased, roundUp(bored));
        if (bore.get().struck() && !state.getValue(WellheadBlock.DRILLED)) {
            level.setBlock(pos, state.setValue(WellheadBlock.DRILLED, true), Block.UPDATE_ALL);
        }
    }

    private static int roundUp(int blocks) {
        return (blocks + BLOCKS_PER_PIPE - 1) / BLOCKS_PER_PIPE * BLOCKS_PER_PIPE;
    }

    /** Blocks between the wellhead and the top of the reservoir. */
    private int depth(OilReservoir res) {
        return Math.max(0, worldPosition.getY() - res.topY());
    }

    private Status drill(ServerLevel level, BlockPos pos, BlockState state, OilReservoir res, OilReservoirData oil) {
        Optional<OilReservoirData.Bore> near = oil.tooClose(res, pos.getX(), pos.getZ(), Config.OIL_MIN_WELL_SPACING.getAsInt());
        if (near.isPresent()) {
            nearX = near.get().x();
            nearZ = near.get().z();
            return Status.TOO_CLOSE;
        }
        float rpm = kinetic.rpm();
        if (rpm <= 0) return Status.NOT_TURNING;
        if (rpm < MIN_SPEED) return Status.TOO_SLOW;
        int depth = depth(res);
        if (bored >= depth) {
            strike(level, pos, state, res, oil);
            return Status.GUSHING;
        }
        // A new four block step needs its casing first.
        if (bored >= cased) {
            ItemStack pipe = items.get(CASING);
            if (pipe.isEmpty()) return Status.NEEDS_CASING;
            pipe.shrink(1);
            cased += BLOCKS_PER_PIPE;
            level.playSound(null, pos, Tier6Sounds.WELLHEAD_CASING.get(), SoundSource.BLOCKS, 1.0f, 0.9f + level.getRandom().nextFloat() * 0.2f);
            setChanged();
        }
        progress += rpm / MIN_SPEED;
        if (level.getGameTime() % 20 == 0) {
            level.playSound(null, pos, Tier6Sounds.WELLHEAD_DRILL.get(), SoundSource.BLOCKS, 1.0f, 0.9f + rpm / 160.0f);
            BlockState under = level.getBlockState(pos.below());
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, under.isAir() ? state : under), pos.getX() + 0.5, pos.getY() + 0.1,
                    pos.getZ() + 0.5, 3, 0.25, 0.02, 0.25, 0.02);
        }
        while (progress >= TICKS_PER_BLOCK && bored < depth && bored < cased) {
            progress -= TICKS_PER_BLOCK;
            bored++;
            oil.setBore(res, pos.getX(), pos.getZ(), bored, false);
        }
        if (bored >= depth) {
            strike(level, pos, state, res, oil);
            return Status.GUSHING;
        }
        return Status.DRILLING;
    }

    private void strike(ServerLevel level, BlockPos pos, BlockState state, OilReservoir res, OilReservoirData oil) {
        oil.setBore(res, pos.getX(), pos.getZ(), bored, true);
        level.setBlock(pos, state.setValue(WellheadBlock.DRILLED, true), Block.UPDATE_ALL);
        gush = GUSH_TICKS;
        progress = 0;
        level.playSound(null, pos, Tier6Sounds.WELLHEAD_GUSHER.get(), SoundSource.BLOCKS, 3.0f, 1.0f);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 32.0 * 32.0) {
                player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".wellhead.struck"));
            }
        }
        if (Config.OIL_GUSHER_PLACES_OIL.getAsBoolean()) spill(level, pos);
        setChanged();
    }

    /** Spec 5.3 (config): a few crude oil source blocks around the head. */
    private static void spill(ServerLevel level, BlockPos pos) {
        int placed = 0;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos spot = pos.relative(side, 2);
            if (placed < 4 && level.getBlockState(spot).canBeReplaced() && level.getBlockState(spot.below()).isFaceSturdy(level, spot.below(), Direction.UP)) {
                level.setBlock(spot, Tier6Blocks.CRUDE_OIL.get().defaultBlockState(), Block.UPDATE_ALL);
                placed++;
            }
        }
    }

    private Status operate(ServerLevel level, BlockPos pos, OilReservoir res, OilReservoirData oil) {
        if (gush > 0) {
            gush--;
            for (int i = 0; i < 6; i++) {
                level.sendParticles(Tier6Particles.OIL_SPRAY.get(), pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 1, 0.1, 0.0, 0.1, 0.0);
            }
            if (gush % 20 == 0) level.playSound(null, pos, Tier6Sounds.WELLHEAD_GUSHER.get(), SoundSource.BLOCKS, 2.0f, 0.9f);
            return Status.GUSHING;
        }
        boolean pumped = level.getGameTime() - lastPumped <= 2;
        if (pumped) return Status.PUMPING;
        if (oil.fraction(res) < NATURAL_LEVEL) return Status.LOW_PRESSURE;
        rate = produce(level, pos, res, oil, NATURAL_FLOW);
        if (rate <= 0) return Status.NO_OUTLET;
        if (level.getGameTime() % 40 == 0) {
            level.playSound(null, pos, Tier6Sounds.WELLHEAD_FLOW.get(), SoundSource.BLOCKS, 0.5f, 1.0f);
        }
        return Status.FLOWING;
    }

    /**
     * What a pump jack on top lifts this tick: pushes up to {@code mb} into the pipes and tanks around and
     * returns how much they took. The reservoir is drawn down by that much; at empty it still gives, at the
     * stripper rate the jack works out.
     */
    public int pump(ServerLevel level, int mb) {
        if (reservoir == null || !getBlockState().getValue(WellheadBlock.DRILLED)) return 0;
        lastPumped = level.getGameTime();
        int moved = produce(level, worldPosition, reservoir, OilReservoirData.get(level), mb);
        rate = moved;
        return moved;
    }

    private int produce(ServerLevel level, BlockPos pos, OilReservoir res, OilReservoirData oil, int mb) {
        Fluid crude = Tier6Fluids.CRUDE_OIL.source().get();
        int left = mb;
        for (Direction face : Direction.values()) {
            if (left <= 0) break;
            if (face == Direction.DOWN || face == Direction.UP && level.getBlockEntity(pos.above()) instanceof PumpJackBlockEntity) continue;
            FluidPipes.Network network = FluidPipes.find(level, pos, face);
            if (network.isEmpty()) continue;
            left -= FluidPipes.push(level, network, crude, left, 20.0f, 0.0f).moved();
        }
        int moved = mb - left;
        if (moved > 0) {
            oil.drain(res, moved);
            percent = (int) Math.round(oil.fraction(res) * 100);
        }
        return moved;
    }

    // ------------------------------------------------------------------ state

    public Status status() {
        return status;
    }

    /** Blocks drilled so far. */
    public int bored() {
        return bored;
    }

    /** The reservoir under this wellhead, once found. */
    public @Nullable OilReservoir reservoir() {
        return reservoir;
    }

    /** Whether the bore has reached the reservoir. */
    public boolean drilled() {
        return getBlockState().getValue(WellheadBlock.DRILLED);
    }

    /** Ticks of gusher left. */
    public int gushing() {
        return gush;
    }

    /** Fixes the reservoir under the wellhead, for game tests in a world that has none. */
    public void setReservoir(@Nullable OilReservoir reservoir) {
        this.reservoir = reservoir;
        this.reservoirAge = -1_000_000;
    }

    /** Puts casing in, for game tests. */
    public void setCasing(ItemStack stack) {
        items.set(CASING, stack);
    }

    // ------------------------------------------------------------------ fluids

    /** Pipes and tanks join on every face but the bottom. */
    @Override
    public boolean connectsFluid(Direction side) {
        return side != Direction.DOWN;
    }

    /** It only gives. */
    @Override
    public int fill(Direction side, Fluid fluid, int amount, float pressure, boolean simulate) {
        return 0;
    }

    // ------------------------------------------------------------------ kinetics

    @Override
    public boolean connects(Direction side) {
        return side.getAxis().isHorizontal();
    }

    @Override
    public KineticState kinetic() {
        return kinetic;
    }

    @Override
    public int impact() {
        return IMPACT;
    }

    @Override
    public int minSpeed() {
        return MIN_SPEED;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        KineticNetworks.markDirty(level, worldPosition);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        KineticNetworks.markDirty(level, worldPosition);
    }

    // ------------------------------------------------------------------ container

    @Override
    public int getContainerSize() {
        return 1;
    }

    /** Only steel fluid pipe is casing. */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.is(Tier4Blocks.STEEL_FLUID_PIPE.asItem());
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container." + StrataIndustria.MOD_ID + ".wellhead");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new WellheadMenu(id, inventory, worldPosition, this, data);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        items = NonNullList.withSize(1, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(in, items);
        bored = in.getIntOr("bored", 0);
        cased = in.getIntOr("cased", 0);
        progress = in.getFloatOr("progress", 0.0f);
        gush = in.getIntOr("gush", 0);
        kinetic.load(in);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        ContainerHelper.saveAllItems(out, items);
        out.putInt("bored", bored);
        out.putInt("cased", cased);
        out.putFloat("progress", progress);
        out.putInt("gush", gush);
        kinetic.save(out);
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
