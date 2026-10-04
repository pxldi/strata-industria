package dev.strataindustria.machine;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.power.KineticConsumer;
import dev.strataindustria.power.KineticNetworks;
import dev.strataindustria.power.KineticState;
import dev.strataindustria.prospecting.CoreSample;
import dev.strataindustria.registry.ModBlockEntities;
import dev.strataindustria.registry.ModDataComponents;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.ModSounds;
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
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The core sampler (tier 3 spec 8.5). Press Drill and, after 400 ticks at 16 RPM (proportionally
 * faster or slower, at least 8 RPM), a core sample of the column below appears in the output slot.
 * Drilling breaks nothing; the only costs are time and rotation.
 */
public class CoreSamplerBlockEntity extends BaseContainerBlockEntity implements KineticConsumer, WorldlyContainer {
    public static final int OUTPUT = 0;
    public static final int IMPACT = 4, MIN_SPEED = 8;
    public static final float BASE_TICKS = 400.0f;
    public static final int DATA_PROGRESS = 0, DATA_STATUS = 1, DATA_COUNT = 2;
    public static final int DRILL_BUTTON = 0;

    public enum Status {
        IDLE, DRILLING, NOT_TURNING, TOO_SLOW, OUTPUT_FULL, DONE;

        public String key() {
            return StrataIndustria.MOD_ID + ".core_sampler." + name().toLowerCase(java.util.Locale.ROOT);
        }

        /** Whether the Drill button can start a new core. */
        public boolean canStart() {
            return this == IDLE || this == DONE;
        }
    }

    private static final int[] SLOTS = {OUTPUT};

    private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private final KineticState kinetic = new KineticState();
    private boolean drilling;
    private float progress;
    private Status status = Status.IDLE;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> Math.round(progress / BASE_TICKS * 1000);
                case DATA_STATUS -> status.ordinal();
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

    public CoreSamplerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CORE_SAMPLER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CoreSamplerBlockEntity sampler) {
        Status before = sampler.status;
        sampler.status = sampler.work((ServerLevel) level);
        if (sampler.status != before) sampler.setChanged();
    }

    /** The Drill button. Starts a core unless one is running or the last one is still in the slot. */
    public void startDrilling() {
        if (drilling || level == null) return;
        if (!items.get(OUTPUT).isEmpty()) {
            status = Status.OUTPUT_FULL;
            return;
        }
        drilling = true;
        progress = 0;
        setChanged();
    }

    private Status work(ServerLevel level) {
        if (!drilling) {
            if (status == Status.OUTPUT_FULL && items.get(OUTPUT).isEmpty()) return Status.IDLE;
            return status == Status.DRILLING || status == Status.NOT_TURNING || status == Status.TOO_SLOW ? Status.IDLE : status;
        }
        float rpm = kinetic.rpm();
        if (rpm <= 0) return Status.NOT_TURNING;
        if (rpm < MIN_SPEED) return Status.TOO_SLOW;
        progress += rpm / 16.0f;
        if (level.getGameTime() % 8 == 0) {
            level.playSound(null, worldPosition, ModSounds.CORE_SAMPLER_DRILL.get(), SoundSource.BLOCKS, 0.45f,
                    0.9f + level.getRandom().nextFloat() * 0.15f);
        }
        if (level.getGameTime() % 4 == 0) {
            BlockState below = level.getBlockState(worldPosition.below());
            if (!below.isAir()) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, below), worldPosition.getX() + 0.5,
                        worldPosition.getY() + 0.05, worldPosition.getZ() + 0.5, 2, 0.08, 0.0, 0.08, 0.05);
            }
        }
        if (progress < BASE_TICKS) {
            setChanged();
            return Status.DRILLING;
        }
        drilling = false;
        progress = 0;
        ItemStack core = new ItemStack(ModItems.CORE_SAMPLE.get());
        core.set(ModDataComponents.CORE_SAMPLE.get(), CoreSample.take(level, worldPosition));
        items.set(OUTPUT, core);
        level.playSound(null, worldPosition, ModSounds.CORE_SAMPLER_DONE.get(), SoundSource.PLAYERS, 0.7f, 1.0f);
        setChanged();
        return Status.DONE;
    }

    public Status status() {
        return status;
    }

    // ------------------------------------------------------------------ kinetics

    @Override
    public boolean connects(Direction side) {
        return side != Direction.DOWN;
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
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return true;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public int getContainerSize() {
        return items.size();
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
        return Component.translatable("container." + StrataIndustria.MOD_ID + ".core_sampler");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new CoreSamplerMenu(id, inventory, worldPosition, this, data);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(in, items);
        drilling = in.getBooleanOr("drilling", false);
        progress = in.getFloatOr("progress", 0.0f);
        kinetic.load(in);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        ContainerHelper.saveAllItems(out, items);
        out.putBoolean("drilling", drilling);
        out.putFloat("progress", progress);
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
