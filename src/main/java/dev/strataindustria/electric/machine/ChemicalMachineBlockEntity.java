package dev.strataindustria.electric.machine;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.fluid.FluidPipes;
import dev.strataindustria.fluid.FluidPort;
import dev.strataindustria.power.ElectricConsumer;
import dev.strataindustria.power.ElectricNetwork;
import dev.strataindustria.power.ElectricNetworks;
import dev.strataindustria.power.ElectricStats;
import dev.strataindustria.power.ElectricStatus;
import dev.strataindustria.power.ElectricTier;
import dev.strataindustria.power.StatusLight;
import dev.strataindustria.processing.ChemicalIo;
import dev.strataindustria.processing.ChemicalRecipe;
import dev.strataindustria.registry.Tier5Fluids;
import dev.strataindustria.registry.Tier5Sounds;
import dev.strataindustria.tanning.FluidAmount;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A tier 5 fluid machine (spec 10.1, 11.1 and 11.2): two item inputs, up to three fluid tanks in and out of
 * 4000 mB each, a buffer of 20 ticks of its draw filled from the network first (6.4) and progress in
 * proportion to the power it gets. An MV machine draws 4x, takes half the ticks and runs every recipe at
 * twice the batch size, the fluid machines' form of the MV rule. Fluids come in on any face from a pipe, or
 * by bucket; products leave into any pipe network or tank touching it, and when a product cannot leave the
 * machine stops rather than losing it.
 */
public abstract class ChemicalMachineBlockEntity extends BaseContainerBlockEntity implements ElectricConsumer, WorldlyContainer, FluidPort {
    public static final int EJECT_TICKS = 20, PUSH_TICKS = 4, PUSH_AMOUNT = 250;
    private static final int BUCKET = 1000;

    protected final ChemicalMachineLayout layout;
    private final ElectricStats fallbackStats;
    private NonNullList<ItemStack> items;
    private final Fluid[] fluid;
    private final int[] amount;
    private double buffer;
    private float progress;
    private float needed;
    private float power;
    private boolean autoEject;
    private long nextEject;
    private int finished;
    private ElectricMachineBlockEntity.Status status = ElectricMachineBlockEntity.Status.EMPTY;
    private Fluid fullFluid = Fluids.EMPTY;
    /** The recipe running now, so progress restarts when the inputs change to another one. */
    private @Nullable Object running;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            if (index >= ChemicalMachineLayout.TANKS && index < ChemicalMachineLayout.MODE) {
                int tank = (index - ChemicalMachineLayout.TANKS) / 2;
                if (tank >= layout.tanks()) return 0;
                return (index - ChemicalMachineLayout.TANKS) % 2 == 0 ? (amount[tank] > 0 ? BuiltInRegistries.FLUID.getId(fluid[tank]) : 0) : amount[tank];
            }
            return switch (index) {
                case ChemicalMachineLayout.STATUS -> status.ordinal();
                case ChemicalMachineLayout.POWER -> Math.round(power * 100);
                case ChemicalMachineLayout.BUFFER -> (int) Math.round(buffer / bufferCapacity() * 100);
                case ChemicalMachineLayout.EJECT -> autoEject ? 1 : 0;
                case ChemicalMachineLayout.PROGRESS -> needed <= 0 ? 0 : Math.round(Math.min(1.0f, progress / needed) * 1000);
                case ChemicalMachineLayout.FULL_FLUID -> BuiltInRegistries.FLUID.getId(fullFluid);
                case ChemicalMachineLayout.MODE -> modeIndex();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return ChemicalMachineLayout.DATA_COUNT;
        }
    };

    protected ChemicalMachineBlockEntity(BlockEntityType<?> type, ChemicalMachineLayout layout, ElectricStats fallbackStats, BlockPos pos,
            BlockState state) {
        super(type, pos, state);
        this.layout = layout;
        this.fallbackStats = fallbackStats;
        this.items = NonNullList.withSize(layout.slots(), ItemStack.EMPTY);
        this.fluid = new Fluid[layout.tanks()];
        this.amount = new int[layout.tanks()];
        java.util.Arrays.fill(fluid, Fluids.EMPTY);
    }

    /** Every recipe this machine runs. */
    protected abstract List<? extends ChemicalRecipe> recipes(ServerLevel level);

    /** Played every 40 ticks while the machine runs. */
    protected abstract SoundEvent workSound();

    /** Played when an operation finishes; the chemistry machines pop. */
    protected SoundEvent finishSound() {
        return SoundEvents.BUBBLE_COLUMN_BUBBLE_POP;
    }

    /** Whether this machine runs {@code recipe} right now; a machine with modes runs only its current mode's. */
    protected boolean runs(ChemicalRecipe recipe) {
        return true;
    }

    /** The mode button's current setting; machines without one ignore it. */
    protected int modeIndex() {
        return 0;
    }

    /** Menu button 1: switches a machine with modes to its next mode. */
    public void toggleMode() {}

    // ------------------------------------------------------------------ tick

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChemicalMachineBlockEntity machine) {
        ServerLevel server = (ServerLevel) level;
        ElectricMachineBlockEntity.Status before = machine.status;
        machine.status = machine.work(server);
        if (machine.status != before) {
            machine.setChanged();
            machine.transition(server, before);
        }
        long time = server.getGameTime();
        if (time % PUSH_TICKS == 0) machine.pushFluids(server);
        if (machine.autoEject && time >= machine.nextEject) {
            machine.nextEject = time + EJECT_TICKS;
            machine.eject(server);
        }
        BlockState next = state.setValue(StatusLight.PROPERTY, machine.status.light())
                .setValue(ElectricMachineBlock.ACTIVE, machine.status.running());
        if (next != state) level.setBlock(pos, next, Block.UPDATE_CLIENTS);
    }

    private void transition(ServerLevel level, ElectricMachineBlockEntity.Status before) {
        SoundEvent sound = null;
        if (status.running() && !before.running()) sound = Tier5Sounds.MACHINE_POWER_ON.get();
        else if (before.running() && (status == ElectricMachineBlockEntity.Status.NO_POWER || status.light() == StatusLight.ERROR)) {
            sound = Tier5Sounds.MACHINE_POWER_OFF.get();
        }
        if (sound != null) level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, 0.4f, 1.0f);
        if (status == ElectricMachineBlockEntity.Status.LOW_POWER && before != ElectricMachineBlockEntity.Status.LOW_POWER) {
            level.playSound(null, worldPosition, Tier5Sounds.MACHINE_LOW_POWER.get(), SoundSource.PLAYERS, 0.5f, 1.0f);
        }
    }

    private ElectricMachineBlockEntity.Status work(ServerLevel level) {
        ElectricStats stats = stats();
        ElectricTier tier = tier();
        ElectricNetwork.Report report = ElectricNetworks.report(level, worldPosition);
        if (report.status() == ElectricStatus.OVERVOLTAGE || report.status() == ElectricStatus.CABLE_OVERVOLTAGE) {
            return ElectricMachineBlockEntity.Status.OVERVOLTAGE;
        }
        if (report.status() == ElectricStatus.TOO_FAR) return ElectricMachineBlockEntity.Status.TOO_FAR;

        int batch = stats.parallel().get(tier);
        ChemicalRecipe recipe = find(level, tier, batch);
        fullFluid = Fluids.EMPTY;
        if (recipe == null) {
            running = null;
            progress = 0;
            needed = 0;
            power = 0;
            return holdsNothing() ? ElectricMachineBlockEntity.Status.EMPTY : ElectricMachineBlockEntity.Status.NO_RECIPE;
        }
        ChemicalIo io = recipe.io();
        ElectricMachineBlockEntity.Status blocked = room(io, batch);
        if (blocked != null) {
            power = 0;
            return blocked;
        }
        if (running != recipe) {
            running = recipe;
            progress = 0;
        }
        needed = Math.max(1.0f, io.ticks() * stats.ticks().get(tier));
        double need = stats.draw().get(tier);
        double take = Math.min(buffer, need);
        buffer -= take;
        power = (float) (take / need);
        if (power < 0.001f) {
            power = 0;
            return ElectricMachineBlockEntity.Status.NO_POWER;
        }
        progress += power;
        if (progress >= needed) {
            progress = 0;
            consume(io, batch);
            produce(level, io, batch);
            finished++;
            level.playSound(null, worldPosition, finishSound(), SoundSource.BLOCKS, 0.3f, 1.2f);
        }
        if (level.getGameTime() % 40 == 0) {
            float pitch = 0.7f + 0.3f * power + (tier == ElectricTier.MV ? 0.1f : 0.0f);
            level.playSound(null, worldPosition, workSound(), SoundSource.BLOCKS, 0.5f, pitch * (0.95f + level.getRandom().nextFloat() * 0.1f));
        }
        setChanged();
        return power >= 0.999f ? ElectricMachineBlockEntity.Status.WORKING : ElectricMachineBlockEntity.Status.LOW_POWER;
    }

    private boolean holdsNothing() {
        for (ItemStack stack : items) if (!stack.isEmpty()) return false;
        for (int a : amount) if (a > 0) return false;
        return true;
    }

    // ------------------------------------------------------------------ recipes

    /** The first recipe whose inputs are all there at this batch size and that this tier can run. */
    private @Nullable ChemicalRecipe find(ServerLevel level, ElectricTier tier, int batch) {
        for (ChemicalRecipe recipe : recipes(level)) {
            ChemicalIo io = recipe.io();
            if (io.minTier().ordinal() > tier.ordinal() || !runs(recipe)) continue;
            if (hasInputs(io, batch)) return recipe;
        }
        return null;
    }

    private boolean hasInputs(ChemicalIo io, int batch) {
        boolean[] used = new boolean[layout.itemInputs()];
        for (ChemicalIo.ItemInput input : io.items()) {
            if (itemSlotFor(input, batch, used) < 0) return false;
        }
        boolean[] usedTank = new boolean[layout.fluidInputs()];
        for (FluidAmount need : io.fluids()) {
            if (tankFor(need.fluid(), need.amount() * batch, usedTank) < 0) return false;
        }
        return true;
    }

    /** The input slot that supplies {@code input}, marked used; or -1. */
    private int itemSlotFor(ChemicalIo.ItemInput input, int batch, boolean[] used) {
        for (int slot = 0; slot < used.length; slot++) {
            ItemStack stack = items.get(slot);
            if (!used[slot] && input.ingredient().test(stack) && stack.getCount() >= input.count() * batch) {
                used[slot] = true;
                return slot;
            }
        }
        return -1;
    }

    /** The input tank holding at least {@code mb} of {@code want}, marked used; or -1. */
    private int tankFor(Fluid want, int mb, boolean[] used) {
        for (int tank = 0; tank < used.length; tank++) {
            if (!used[tank] && amount[tank] >= mb && fluid[tank].isSame(want)) {
                used[tank] = true;
                return tank;
            }
        }
        return -1;
    }

    /** Null if the results fit; otherwise why not. */
    private ElectricMachineBlockEntity.@Nullable Status room(ChemicalIo io, int batch) {
        // Result i of fluid goes to output tank i; it must be empty or already hold that fluid.
        for (int i = 0; i < io.fluidResults().size() && i < layout.fluidOutputs(); i++) {
            FluidAmount result = io.fluidResults().get(i);
            int tank = layout.fluidInputs() + i;
            if (amount[tank] > 0 && !fluid[tank].isSame(result.fluid()) || amount[tank] + result.amount() * batch > ChemicalMachineLayout.TANK_CAPACITY) {
                fullFluid = result.fluid();
                return ElectricMachineBlockEntity.Status.TANK_FULL;
            }
        }
        // Items: the outputs after this operation's inputs are gone must take every result.
        NonNullList<ItemStack> outputs = NonNullList.withSize(layout.itemOutputs(), ItemStack.EMPTY);
        for (int i = 0; i < outputs.size(); i++) outputs.set(i, items.get(layout.itemInputs() + i).copy());
        for (var result : io.itemResults()) {
            ItemStack left = result.create();
            left.setCount(left.getCount() * batch);
            for (int i = 0; i < outputs.size() && !left.isEmpty(); i++) left = merge(outputs, i, left);
            if (!left.isEmpty()) return ElectricMachineBlockEntity.Status.OUTPUT_FULL;
        }
        return null;
    }

    private void consume(ChemicalIo io, int batch) {
        boolean[] used = new boolean[layout.itemInputs()];
        for (ChemicalIo.ItemInput input : io.items()) {
            int slot = itemSlotFor(input, batch, used);
            if (slot >= 0) items.get(slot).shrink(input.count() * batch);
        }
        boolean[] usedTank = new boolean[layout.fluidInputs()];
        for (FluidAmount need : io.fluids()) {
            int tank = tankFor(need.fluid(), need.amount() * batch, usedTank);
            if (tank < 0) continue;
            amount[tank] -= need.amount() * batch;
            if (amount[tank] <= 0) {
                amount[tank] = 0;
                fluid[tank] = Fluids.EMPTY;
            }
        }
    }

    private void produce(ServerLevel level, ChemicalIo io, int batch) {
        for (int i = 0; i < io.fluidResults().size() && i < layout.fluidOutputs(); i++) {
            FluidAmount result = io.fluidResults().get(i);
            int tank = layout.fluidInputs() + i;
            fluid[tank] = result.fluid();
            amount[tank] += result.amount() * batch;
        }
        NonNullList<ItemStack> outputs = NonNullList.withSize(layout.itemOutputs(), ItemStack.EMPTY);
        for (int i = 0; i < outputs.size(); i++) outputs.set(i, items.get(layout.itemInputs() + i));
        for (var result : io.itemResults()) {
            ItemStack left = result.create();
            left.setCount(left.getCount() * batch);
            if (io.resultTemperature() > 0) dev.strataindustria.heat.Heat.set(left, io.resultTemperature(), level.getGameTime());
            for (int i = 0; i < outputs.size() && !left.isEmpty(); i++) {
                left = merge(outputs, i, left);
                items.set(layout.itemInputs() + i, outputs.get(i));
            }
        }
    }

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

    /** Whether any recipe of this machine takes {@code fluid} or an item matching {@code stack}. */
    private boolean uses(Fluid want, ItemStack stack) {
        if (!(level instanceof ServerLevel server)) return !stack.isEmpty();
        for (ChemicalRecipe recipe : recipes(server)) {
            if (!stack.isEmpty()) {
                for (ChemicalIo.ItemInput input : recipe.io().items()) if (input.ingredient().test(stack)) return true;
            } else {
                for (FluidAmount need : recipe.io().fluids()) if (need.fluid().isSame(want)) return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ fluids

    @Override
    public boolean connectsFluid(Direction side) {
        return true;
    }

    /** Input tanks only; each locks to the first fluid it gets and unlocks when empty (spec 11.1). */
    @Override
    public int fill(Direction side, Fluid want, int mb, float pressure, boolean simulate) {
        if (mb <= 0 || want.isSame(Fluids.EMPTY) || !uses(want, ItemStack.EMPTY)) return 0;
        int tank = -1;
        for (int i = 0; i < layout.fluidInputs(); i++) {
            if (amount[i] > 0 && fluid[i].isSame(want)) {
                tank = i;
                break;
            }
        }
        if (tank < 0) {
            for (int i = 0; i < layout.fluidInputs(); i++) {
                if (amount[i] == 0) {
                    tank = i;
                    break;
                }
            }
        }
        if (tank < 0) return 0;
        int take = Math.max(0, Math.min(mb, ChemicalMachineLayout.TANK_CAPACITY - amount[tank]));
        if (!simulate && take > 0) {
            fluid[tank] = want;
            amount[tank] += take;
            setChanged();
        }
        return take;
    }

    /** A bucket of water or acid poured in by hand. */
    public InteractionResult useBucket(Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack) {
        @Nullable Fluid carried = Tier5Fluids.fluidOfBucket(stack);
        if (carried == null || fill(Direction.UP, carried, BUCKET, 0, true) < BUCKET) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            fill(Direction.UP, carried, BUCKET, 0, false);
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0f, 1.0f);
            level.gameEvent(player, GameEvent.FLUID_PLACE, pos);
        }
        return InteractionResult.SUCCESS;
    }

    /** Spec 11.2: pushes each product into any pipe network or tank on a face that takes it, first product first. */
    private void pushFluids(ServerLevel level) {
        for (int tank = layout.fluidInputs(); tank < layout.tanks(); tank++) {
            for (Direction face : Direction.values()) {
                if (amount[tank] <= 0) break;
                FluidPipes.Network network = FluidPipes.find(level, worldPosition, face);
                if (network.isEmpty()) continue;
                int moved = FluidPipes.push(level, network, fluid[tank], Math.min(amount[tank], PUSH_AMOUNT), 20, 0).moved();
                if (moved <= 0) continue;
                amount[tank] -= moved;
                if (amount[tank] <= 0) {
                    amount[tank] = 0;
                    fluid[tank] = Fluids.EMPTY;
                }
                setChanged();
            }
        }
    }

    public Fluid fluid(int tank) {
        return amount[tank] > 0 ? fluid[tank] : Fluids.EMPTY;
    }

    public int amount(int tank) {
        return amount[tank];
    }

    /** Fills a tank directly, for game tests. */
    public void setTank(int tank, Fluid fluid, int mb) {
        this.fluid[tank] = mb > 0 ? fluid : Fluids.EMPTY;
        this.amount[tank] = Math.max(0, Math.min(ChemicalMachineLayout.TANK_CAPACITY, mb));
    }

    // ------------------------------------------------------------------ items out

    private void eject(ServerLevel level) {
        Direction back = getBlockState().getValue(ElectricMachineBlock.FACING).getOpposite();
        if (!(level.getBlockEntity(worldPosition.relative(back)) instanceof Container target)) return;
        for (int i = 0; i < layout.itemOutputs(); i++) {
            int slot = layout.itemInputs() + i;
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

    // ------------------------------------------------------------------ electricity

    public ElectricStats stats() {
        return ElectricStats.of(getBlockState().getBlock(), fallbackStats);
    }

    public double bufferCapacity() {
        ElectricStats stats = stats();
        return stats.draw().get(tier()) * (double) stats.bufferTicks();
    }

    public double buffer() {
        return buffer;
    }

    public void setBuffer(double joules) {
        buffer = Math.max(0, Math.min(bufferCapacity(), joules));
    }

    public ElectricMachineBlockEntity.Status status() {
        return status;
    }

    public int finishedCount() {
        return finished;
    }

    public ChemicalMachineLayout layout() {
        return layout;
    }

    public void toggleAutoEject() {
        autoEject = !autoEject;
        setChanged();
    }

    @Override
    public ElectricTier tier() {
        return getBlockState().getValue(ElectricMachineBlock.TIER);
    }

    @Override
    public boolean connectsElectric(Direction side) {
        return true;
    }

    @Override
    public double request() {
        return Math.max(0, Math.min(bufferCapacity() - buffer, stats().draw().get(tier())));
    }

    @Override
    public void receive(double joules) {
        buffer = Math.min(bufferCapacity(), buffer + joules);
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

    @Override
    public int[] getSlotsForFace(Direction side) {
        int[] slots = new int[layout.slots()];
        for (int i = 0; i < slots.length; i++) slots[i] = i;
        return slots;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= layout.itemInputs();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot < layout.itemInputs() && uses(Fluids.EMPTY, stack);
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
        return new ChemicalMachineMenu(layout, tier(), id, inventory, worldPosition, this, data);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(in, items);
        buffer = in.getDoubleOr("buffer", 0.0);
        progress = in.getFloatOr("progress", 0.0f);
        autoEject = in.getBooleanOr("auto_eject", false);
        finished = in.getIntOr("finished", 0);
        for (int i = 0; i < layout.tanks(); i++) {
            amount[i] = in.getIntOr("tank" + i, 0);
            fluid[i] = in.read("fluid" + i, BuiltInRegistries.FLUID.byNameCodec()).orElse(Fluids.EMPTY);
            if (fluid[i].isSame(Fluids.EMPTY)) amount[i] = 0;
        }
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        ContainerHelper.saveAllItems(out, items);
        out.putDouble("buffer", buffer);
        out.putFloat("progress", progress);
        out.putBoolean("auto_eject", autoEject);
        out.putInt("finished", finished);
        for (int i = 0; i < layout.tanks(); i++) {
            out.putInt("tank" + i, amount[i]);
            if (amount[i] > 0) out.store("fluid" + i, BuiltInRegistries.FLUID.byNameCodec(), fluid[i]);
        }
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
