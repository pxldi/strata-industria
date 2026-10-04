package dev.strataindustria.electric.machine;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.processing.Processing;
import dev.strataindustria.power.ElectricConsumer;
import dev.strataindustria.power.ElectricNetwork;
import dev.strataindustria.power.ElectricNetworks;
import dev.strataindustria.power.ElectricStats;
import dev.strataindustria.power.ElectricStatus;
import dev.strataindustria.power.ElectricTier;
import dev.strataindustria.power.StatusLight;
import dev.strataindustria.registry.Tier5Sounds;
import dev.strataindustria.tanning.FluidAmount;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A tier 5 item machine (spec 10.1): inputs on every face, outputs from every face, an internal buffer of
 * 20 ticks of its draw filled from the network first (6.4), and progress in proportion to the power it
 * gets, so 60% power runs at 60% speed (6.1). An MV machine draws 4x, takes half the ticks and works two
 * lanes at once. With auto-eject on it pushes one output stack every 20 ticks into the block behind it.
 */
public abstract class ElectricMachineBlockEntity extends BaseContainerBlockEntity implements ElectricConsumer, WorldlyContainer {
    public enum Status {
        EMPTY(StatusLight.OFF), WORKING(StatusLight.RUN), LOW_POWER(StatusLight.WAIT), NO_POWER(StatusLight.WAIT),
        NO_RECIPE(StatusLight.OFF), OUTPUT_FULL(StatusLight.ERROR), OVERVOLTAGE(StatusLight.ERROR), TOO_FAR(StatusLight.ERROR),
        /** A fluid machine whose product has nowhere to go (spec 11.2: "Hydrogen tank full"). */
        TANK_FULL(StatusLight.ERROR);

        private final StatusLight light;

        Status(StatusLight light) {
            this.light = light;
        }

        public StatusLight light() {
            return light;
        }

        public String key() {
            return StrataIndustria.MOD_ID + ".electric_machine." + name().toLowerCase(Locale.ROOT);
        }

        /** Running, at full or part power. */
        public boolean running() {
            return this == WORKING || this == LOW_POWER;
        }
    }

    /**
     * What one input item makes and how long it takes.
     *
     * @param ticks LV base ticks; the tier's multiplier applies on top
     * @param gas   given off per item, piped out of the back or vented
     */
    public record Operation(Processing processing, float ticks, Optional<FluidAmount> gas) {
        public Operation(Processing processing, float ticks) {
            this(processing, ticks, Optional.empty());
        }
    }

    public static final int EJECT_TICKS = 20;

    protected final ElectricMachineLayout layout;
    private final ElectricStats fallbackStats;
    private NonNullList<ItemStack> items;
    private final float[] progress = new float[ElectricMachineLayout.MAX_LANES];
    private final float[] needed = new float[ElectricMachineLayout.MAX_LANES];
    private double buffer;
    private boolean autoEject;
    private Status status = Status.EMPTY;
    private float power;
    private int finished;
    private long nextEject;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            if (index < ElectricMachineLayout.MAX_LANES) {
                return needed[index] <= 0 ? 0 : Math.round(Math.min(1.0f, progress[index] / needed[index]) * 1000);
            }
            return switch (index) {
                case ElectricMachineLayout.STATUS -> status.ordinal();
                case ElectricMachineLayout.POWER -> Math.round(power * 100);
                case ElectricMachineLayout.BUFFER -> (int) Math.round(buffer / bufferCapacity() * 100);
                case ElectricMachineLayout.EJECT -> autoEject ? 1 : 0;
                case ElectricMachineLayout.MODE -> modeIndex();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return ElectricMachineLayout.DATA_COUNT;
        }
    };

    protected ElectricMachineBlockEntity(BlockEntityType<?> type, ElectricMachineLayout layout, ElectricStats fallbackStats, BlockPos pos,
            BlockState state) {
        super(type, pos, state);
        this.layout = layout;
        this.fallbackStats = fallbackStats;
        this.items = NonNullList.withSize(layout.slots(), ItemStack.EMPTY);
    }

    /** What one input item makes, or empty if this machine does not take it. */
    protected abstract Optional<Operation> operation(ServerLevel level, ItemStack input);

    /** Played every 40 ticks while the machine runs. */
    protected abstract SoundEvent workSound();

    /** The mode button's current setting, 0 or 1; machines without one ignore it. */
    protected int modeIndex() {
        return 0;
    }

    /** Menu button 1: switches a machine with modes to its next mode. */
    public void toggleMode() {}

    /** Called once for each finished item, after its outputs are in. */
    protected void finished(ServerLevel level, Operation operation) {}

    /** Called every tick the machine runs, for particles. */
    protected void running(ServerLevel level, ItemStack shown) {}

    public static void serverTick(Level level, BlockPos pos, BlockState state, ElectricMachineBlockEntity machine) {
        ServerLevel server = (ServerLevel) level;
        Status before = machine.status;
        machine.status = machine.work(server);
        if (machine.status != before) {
            machine.setChanged();
            machine.transition(server, before);
        }
        if (machine.autoEject && server.getGameTime() >= machine.nextEject) {
            machine.nextEject = server.getGameTime() + EJECT_TICKS;
            machine.eject(server);
        }
        BlockState next = state.setValue(StatusLight.PROPERTY, machine.status.light())
                .setValue(ElectricMachineBlock.ACTIVE, machine.status.running());
        if (next != state) level.setBlock(pos, next, Block.UPDATE_CLIENTS);
    }

    /** Spec 23.6: a relay clack when power comes and goes, two beeps when it runs short. */
    private void transition(ServerLevel level, Status before) {
        SoundEvent sound = null;
        if (status.running() && !before.running()) sound = Tier5Sounds.MACHINE_POWER_ON.get();
        else if (before.running() && (status == Status.NO_POWER || status.light() == StatusLight.ERROR)) sound = Tier5Sounds.MACHINE_POWER_OFF.get();
        if (sound != null) level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, 0.4f, 1.0f);
        if (status == Status.LOW_POWER && before != Status.LOW_POWER) {
            level.playSound(null, worldPosition, Tier5Sounds.MACHINE_LOW_POWER.get(), SoundSource.PLAYERS, 0.5f, 1.0f);
        }
    }

    private Status work(ServerLevel level) {
        ElectricStats stats = stats();
        ElectricTier tier = tier();
        int lanes = lanes();
        balance(lanes);
        ElectricNetwork.Report report = ElectricNetworks.report(level, worldPosition);
        if (report.status() == ElectricStatus.OVERVOLTAGE || report.status() == ElectricStatus.CABLE_OVERVOLTAGE) return Status.OVERVOLTAGE;
        if (report.status() == ElectricStatus.TOO_FAR) return Status.TOO_FAR;

        Status problem = Status.EMPTY;
        Operation[] ops = new Operation[lanes];
        int busy = 0;
        for (int lane = 0; lane < lanes; lane++) {
            ItemStack input = items.get(layout.inputSlot(lane));
            if (input.isEmpty()) {
                progress[lane] = 0;
                needed[lane] = 0;
                continue;
            }
            Optional<Operation> operation = operation(level, input);
            if (operation.isEmpty()) {
                problem = worse(problem, Status.NO_RECIPE);
                continue;
            }
            if (!fits(lane, operation.get().processing().mostPossible())) {
                problem = worse(problem, Status.OUTPUT_FULL);
                continue;
            }
            ops[lane] = operation.get();
            needed[lane] = Math.max(1.0f, operation.get().ticks() * stats.ticks().get(tier));
            busy++;
        }
        if (busy == 0) {
            power = 0;
            return problem;
        }
        // A lane's share of the draw, so one lane of an MV machine uses the same joules per item as two.
        double need = stats.draw().get(tier) * busy / (double) lanes;
        double take = Math.min(buffer, need);
        buffer -= take;
        power = (float) (take / need);
        if (power < 0.001f) {
            power = 0;
            return Status.NO_POWER;
        }
        ItemStack shown = ItemStack.EMPTY;
        for (int lane = 0; lane < lanes; lane++) {
            Operation op = ops[lane];
            if (op == null) continue;
            ItemStack input = items.get(layout.inputSlot(lane));
            if (shown.isEmpty()) shown = input.copyWithCount(1);
            progress[lane] += power;
            if (progress[lane] >= needed[lane]) {
                progress[lane] = 0;
                for (ItemStack out : op.processing().roll(level.getRandom())) insert(lane, out);
                input.shrink(1);
                finished++;
                finished(level, op);
            }
        }
        if (level.getGameTime() % 40 == 0) {
            // Spec 23.6: 0.85 at half power, 1.0 at full; MV a little higher.
            float pitch = 0.7f + 0.3f * power + (tier == ElectricTier.MV ? 0.1f : 0.0f);
            level.playSound(null, worldPosition, workSound(), SoundSource.BLOCKS, 0.5f, pitch * (0.95f + level.getRandom().nextFloat() * 0.1f));
        }
        running(level, shown);
        setChanged();
        return power >= 0.999f ? Status.WORKING : Status.LOW_POWER;
    }

    /** The later problem in the enum is the more useful one to show. */
    private static Status worse(Status a, Status b) {
        return b.ordinal() > a.ordinal() ? b : a;
    }

    /** Hoppers fill the first input; an idle second lane takes half of it so both lanes work. */
    private void balance(int lanes) {
        if (lanes < 2) return;
        ItemStack first = items.get(layout.inputSlot(0)), second = items.get(layout.inputSlot(1));
        if (second.isEmpty() && first.getCount() > 1) {
            items.set(layout.inputSlot(1), first.split(first.getCount() / 2));
        } else if (first.isEmpty() && second.getCount() > 1) {
            items.set(layout.inputSlot(0), second.split(second.getCount() / 2));
        }
    }

    /** Pushes the first output stack it can into the container behind the machine. */
    private void eject(ServerLevel level) {
        Direction back = getBlockState().getValue(ElectricMachineBlock.FACING).getOpposite();
        if (!(level.getBlockEntity(worldPosition.relative(back)) instanceof Container target)) return;
        for (int lane = 0; lane < lanes(); lane++) {
            for (int i = 0; i < layout.outputs(); i++) {
                int slot = layout.outputSlot(lane, i);
                ItemStack stack = items.get(slot);
                if (stack.isEmpty()) continue;
                ItemStack left = HopperBlockEntity.addItem(null, target, stack.copy(), back.getOpposite());
                if (left.getCount() != stack.getCount()) {
                    items.set(slot, left);
                    target.setChanged();
                    setChanged();
                    return;
                }
            }
        }
    }

    /** Whether every stack would fit in {@code lane}'s outputs at once. */
    private boolean fits(int lane, List<ItemStack> stacks) {
        NonNullList<ItemStack> copy = NonNullList.withSize(layout.outputs(), ItemStack.EMPTY);
        for (int i = 0; i < copy.size(); i++) copy.set(i, items.get(layout.outputSlot(lane, i)).copy());
        for (ItemStack stack : stacks) {
            ItemStack left = stack.copy();
            for (int i = 0; i < copy.size() && !left.isEmpty(); i++) left = merge(copy, i, left);
            if (!left.isEmpty()) return false;
        }
        return true;
    }

    private void insert(int lane, ItemStack stack) {
        NonNullList<ItemStack> outputs = NonNullList.withSize(layout.outputs(), ItemStack.EMPTY);
        for (int i = 0; i < outputs.size(); i++) outputs.set(i, items.get(layout.outputSlot(lane, i)));
        ItemStack left = stack;
        for (int i = 0; i < outputs.size() && !left.isEmpty(); i++) {
            left = merge(outputs, i, left);
            items.set(layout.outputSlot(lane, i), outputs.get(i));
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

    public ElectricStats stats() {
        return ElectricStats.of(getBlockState().getBlock(), fallbackStats);
    }

    /** Operations at once at the current tier. */
    public int lanes() {
        return Math.min(ElectricMachineLayout.MAX_LANES, stats().parallel().get(tier()));
    }

    public double bufferCapacity() {
        ElectricStats stats = stats();
        return stats.draw().get(tier()) * (double) stats.bufferTicks();
    }

    public double buffer() {
        return buffer;
    }

    /** Fills the buffer directly, for game tests. */
    public void setBuffer(double joules) {
        buffer = Math.max(0, Math.min(bufferCapacity(), joules));
    }

    public Status status() {
        return status;
    }

    /** Fraction of full speed in the last tick it ran. */
    public float power() {
        return power;
    }

    public int finishedCount() {
        return finished;
    }

    public ElectricMachineLayout layout() {
        return layout;
    }

    public boolean autoEject() {
        return autoEject;
    }

    public void toggleAutoEject() {
        autoEject = !autoEject;
        setChanged();
    }

    /** The screen's status line: "Low power (60%)" and the like. */
    public static Component statusLine(Status status, int percent) {
        return Component.translatable(status.key(), percent);
    }

    // ------------------------------------------------------------------ electricity

    @Override
    public ElectricTier tier() {
        return getBlockState().getValue(ElectricMachineBlock.TIER);
    }

    @Override
    public boolean connectsElectric(Direction side) {
        return true;
    }

    /** Spec 6.4: tops the buffer up by at most one tick of draw, so a running machine asks for exactly its draw. */
    @Override
    public double request() {
        return Math.max(0, Math.min(bufferCapacity() - buffer, stats().draw().get(tier())));
    }

    @Override
    public void receive(double amount) {
        buffer = Math.min(bufferCapacity(), buffer + amount);
    }

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

    // ------------------------------------------------------------------ container

    private int[] slotsFor(boolean inputs, boolean outputs) {
        int lanes = lanes();
        int[] slots = new int[(inputs ? lanes : 0) + (outputs ? lanes * layout.outputs() : 0)];
        int n = 0;
        for (int lane = 0; lane < lanes; lane++) {
            if (inputs) slots[n++] = layout.inputSlot(lane);
            if (outputs) for (int i = 0; i < layout.outputs(); i++) slots[n++] = layout.outputSlot(lane, i);
        }
        return slots;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return slotsFor(true, true);
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot < ElectricMachineLayout.MAX_LANES && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= ElectricMachineLayout.MAX_LANES;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot >= ElectricMachineLayout.MAX_LANES || slot >= lanes()) return false;
        return level == null || level.isClientSide() || (level instanceof ServerLevel server && operation(server, stack).isPresent());
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
        return Component.translatable("block." + StrataIndustria.MOD_ID + "." + layout.id());
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new ElectricMachineMenu(layout, tier(), id, inventory, worldPosition, this, data);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(in, items);
        for (int i = 0; i < progress.length; i++) progress[i] = in.getFloatOr("progress" + i, 0.0f);
        buffer = in.getDoubleOr("buffer", 0.0);
        autoEject = in.getBooleanOr("auto_eject", false);
        finished = in.getIntOr("finished", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        ContainerHelper.saveAllItems(out, items);
        for (int i = 0; i < progress.length; i++) out.putFloat("progress" + i, progress[i]);
        out.putDouble("buffer", buffer);
        out.putBoolean("auto_eject", autoEject);
        out.putInt("finished", finished);
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
