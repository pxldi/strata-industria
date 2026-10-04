package dev.strataindustria.processing;

import dev.strataindustria.Config;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.power.KineticConsumer;
import dev.strataindustria.power.KineticNetworks;
import dev.strataindustria.power.KineticState;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A tier 4 ore processing machine on a shaft (spec 11.1): several inputs worked in parallel, each at
 * {@code baseTicks} per item at 16 RPM and proportionally faster or slower, into a shared set of
 * outputs. Hoppers feed the top and sides and empty the bottom, like the tier 3 machines.
 */
public abstract class ProcessingBlockEntity extends BaseContainerBlockEntity implements KineticConsumer, WorldlyContainer {
    public enum Status {
        EMPTY, WORKING, NOT_TURNING, TOO_SLOW, NO_RECIPE, OUTPUT_FULL, NO_WATER;

        public String key() {
            return StrataIndustria.MOD_ID + ".machine." + name().toLowerCase(Locale.ROOT);
        }
    }

    protected final MachineLayout layout;
    private final int[] inputSlots, outputSlots;
    private NonNullList<ItemStack> items;
    private final KineticState kinetic = new KineticState();
    private final float[] progress;
    private Status status = Status.EMPTY;
    private int finished;
    private int chain;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            if (index < layout.inputs()) return Math.round(progress[index] / baseTicks() * 1000);
            if (index == layout.statusIndex()) return status.ordinal();
            return extraData(index - layout.statusIndex() - 1);
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return layout.dataCount();
        }
    };

    protected ProcessingBlockEntity(BlockEntityType<?> type, MachineLayout layout, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.layout = layout;
        this.items = NonNullList.withSize(layout.slots(), ItemStack.EMPTY);
        this.progress = new float[layout.inputs()];
        this.inputSlots = new int[layout.inputs()];
        this.outputSlots = new int[layout.outputs()];
        for (int i = 0; i < inputSlots.length; i++) inputSlots[i] = i;
        for (int i = 0; i < outputSlots.length; i++) outputSlots[i] = layout.inputs() + i;
    }

    /** What one input item makes, or empty if this machine does not take it. */
    protected abstract Optional<Processing> processing(ServerLevel level, ItemStack input);

    /** Ticks per item at 16 RPM. */
    protected abstract float baseTicks();

    /** Played every second while the machine works. */
    protected abstract SoundEvent workSound();

    /** The field journal goal for a finished item, or null for none. */
    protected abstract @Nullable String journalGoal();

    /** Why this machine cannot start on {@code input} right now even though it has a recipe, or null if it can. */
    protected @Nullable Status blocked(ItemStack input) {
        return null;
    }

    /** Called once for each finished item, after its outputs are in. */
    protected void used() {}

    /** Machine-specific values for the screen, after the progress and status values. */
    protected int extraData(int index) {
        return 0;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ProcessingBlockEntity machine) {
        Status before = machine.status;
        machine.status = machine.work((ServerLevel) level, pos);
        if (machine.status != before) {
            machine.setChanged();
            boolean active = machine.status == Status.WORKING;
            if (state.hasProperty(ProcessingBlock.ACTIVE) && state.getValue(ProcessingBlock.ACTIVE) != active) {
                level.setBlock(pos, state.setValue(ProcessingBlock.ACTIVE, active), 3);
            }
        }
    }

    private Status work(ServerLevel level, BlockPos pos) {
        Status problem = Status.EMPTY;
        boolean working = false;
        ItemStack shown = ItemStack.EMPTY;
        float rpm = kinetic.rpm();
        for (int i = 0; i < layout.inputs(); i++) {
            ItemStack input = items.get(i);
            if (input.isEmpty()) {
                progress[i] = 0;
                continue;
            }
            Optional<Processing> processing = processing(level, input);
            if (processing.isEmpty()) {
                problem = worse(problem, Status.NO_RECIPE);
                continue;
            }
            if (rpm <= 0) return Status.NOT_TURNING;
            if (rpm < minSpeed()) return Status.TOO_SLOW;
            if (!fits(processing.get().mostPossible())) {
                problem = worse(problem, Status.OUTPUT_FULL);
                continue;
            }
            Status blocked = blocked(input);
            if (blocked != null) {
                problem = worse(problem, blocked);
                continue;
            }
            working = true;
            if (shown.isEmpty()) shown = input.copyWithCount(1);
            progress[i] += rpm / 16.0f;
            if (progress[i] >= baseTicks()) {
                progress[i] = 0;
                for (ItemStack out : processing.get().roll(level.getRandom())) insert(out);
                input.shrink(1);
                used();
                finished(level, pos);
            }
        }
        if (!working) return problem;
        if (level.getGameTime() % 20 == 0) {
            float pitch = rpm >= 32 ? 1.15f : rpm >= 16 ? 1.0f : 0.85f;
            level.playSound(null, pos, workSound(), SoundSource.BLOCKS, 0.6f, pitch * (0.95f + level.getRandom().nextFloat() * 0.1f));
        }
        if (level.getGameTime() % 4 == 0) {
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, shown.getItem()), pos.getX() + 0.5, pos.getY() + 1.0,
                    pos.getZ() + 0.5, 2, 0.25, 0.02, 0.25, 0.04);
        }
        setChanged();
        return Status.WORKING;
    }

    /** The later problem in the enum is the more useful one to show. */
    private static Status worse(Status a, Status b) {
        return b.ordinal() > a.ordinal() ? b : a;
    }

    protected void finished(ServerLevel level, BlockPos pos) {
        finished++;
        String goal = journalGoal();
        if (goal != null) Journal.awardNear(level, pos, goal);
        // Spec 13.6: only an unattended run counts; opening the machine starts the count over.
        if (++chain >= Config.AUTOMATION_CHAIN_OPERATIONS.getAsInt()) Journal.awardNear(level, pos, Journal.AUTOMATED_CHAIN);
    }

    /** Finished items in a row since a player last opened this machine (spec 13.6). */
    public int chainCount() {
        return chain;
    }

    /** Items this machine has finished since it was placed. */
    public int finishedCount() {
        return finished;
    }

    /** Whether every stack in {@code stacks} would fit in the output slots at once. */
    private boolean fits(List<ItemStack> stacks) {
        NonNullList<ItemStack> copy = NonNullList.withSize(outputSlots.length, ItemStack.EMPTY);
        for (int i = 0; i < outputSlots.length; i++) copy.set(i, items.get(outputSlots[i]).copy());
        for (ItemStack stack : stacks) {
            ItemStack left = stack.copy();
            for (int i = 0; i < copy.size() && !left.isEmpty(); i++) left = merge(copy, i, left);
            if (!left.isEmpty()) return false;
        }
        return true;
    }

    private void insert(ItemStack stack) {
        NonNullList<ItemStack> outputs = NonNullList.withSize(outputSlots.length, ItemStack.EMPTY);
        for (int i = 0; i < outputSlots.length; i++) outputs.set(i, items.get(outputSlots[i]));
        ItemStack left = stack;
        for (int i = 0; i < outputs.size() && !left.isEmpty(); i++) {
            left = merge(outputs, i, left);
            items.set(outputSlots[i], outputs.get(i));
        }
    }

    /** Merges as much of {@code stack} as fits into {@code slots[i]} and returns what is left. */
    private static ItemStack merge(NonNullList<ItemStack> slots, int i, ItemStack stack) {
        ItemStack there = slots.get(i);
        if (there.isEmpty()) {
            slots.set(i, stack);
            return ItemStack.EMPTY;
        }
        if (!ItemStack.isSameItemSameComponents(there, stack)) return stack;
        int move = Math.min(stack.getCount(), there.getMaxStackSize() - there.getCount());
        if (move <= 0) return stack;
        there.grow(move);
        stack.shrink(move);
        return stack.isEmpty() ? ItemStack.EMPTY : stack;
    }

    public Status status() {
        return status;
    }

    public MachineLayout layout() {
        return layout;
    }

    // ------------------------------------------------------------------ kinetics

    @Override
    public boolean connects(Direction side) {
        return getBlockState().getBlock() instanceof ProcessingBlock<?> block && block.connects(getBlockState(), side);
    }

    @Override
    public KineticState kinetic() {
        return kinetic;
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
        return side == Direction.DOWN ? outputSlots : inputSlots;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot < layout.inputs() && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= layout.inputs();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot >= layout.inputs()) return false;
        return level == null || level.isClientSide() || (level instanceof ServerLevel server && processing(server, stack).isPresent());
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
        return Component.translatable("container." + StrataIndustria.MOD_ID + "." + layout.id());
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        chain = 0;
        return new ProcessingMenu(layout, id, inventory, worldPosition, this, data);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(in, items);
        for (int i = 0; i < progress.length; i++) progress[i] = in.getFloatOr("progress" + i, 0.0f);
        finished = in.getIntOr("finished", 0);
        chain = in.getIntOr("chain", 0);
        kinetic.load(in);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        ContainerHelper.saveAllItems(out, items);
        for (int i = 0; i < progress.length; i++) out.putFloat("progress" + i, progress[i]);
        out.putInt("finished", finished);
        out.putInt("chain", chain);
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
