package dev.strataindustria.ceramics;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.heat.HeatConsumer;
import dev.strataindustria.heat.HeatIntake;
import dev.strataindustria.heat.HeatPipeBlock;
import dev.strataindustria.heat.HeatPort;
import dev.strataindustria.registry.Tier4BlockEntities;
import dev.strataindustria.registry.Tier4Items;
import dev.strataindustria.registry.Tier4Sounds;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The kiln (tier 4 spec 8.6): the pit kiln's big brother. Everything loaded in its eight input slots
 * fires together in 600 ticks at 1000 °C or more, drawing 20 HU/t from a firebox under it or over heat
 * pipes; a short supply fires it more slowly. It fires what a pit kiln fires, and turns slag dust into
 * slag wool, four to one. No straw, no logs, no rain.
 */
public class KilnBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, HeatConsumer, HeatPort {
    public static final int INPUTS = 8, OUTPUTS = 8, SLOTS = INPUTS + OUTPUTS;
    public static final int BATCH_TICKS = 600, MIN_TEMPERATURE = 1000, HEAT = 20, SLAG_PER_WOOL = 4;
    public static final int DATA_PROGRESS = 0, DATA_STATUS = 1, DATA_TEMPERATURE = 2, DATA_HEAT = 3, DATA_LIMIT = 4, DATA_COUNT = 5;

    public enum Status {
        EMPTY, NO_RECIPE, OUTPUT_FULL, NEEDS_HEAT, FIRING;

        public String key() {
            return StrataIndustria.MOD_ID + ".kiln.status." + name().toLowerCase(Locale.ROOT);
        }
    }

    private static final int[] INPUT_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7};
    private static final int[] OUTPUT_SLOTS = {8, 9, 10, 11, 12, 13, 14, 15};

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private final HeatIntake intake = new HeatIntake(MIN_TEMPERATURE, HEAT);
    private float progress;
    private Status status = Status.EMPTY;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> Math.round(progress * 1000 / BATCH_TICKS);
                case DATA_STATUS -> status.ordinal();
                case DATA_TEMPERATURE -> Math.round(intake.temperature());
                case DATA_HEAT -> intake.heat();
                case DATA_LIMIT -> intake.limit();
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

    public KilnBlockEntity(BlockPos pos, BlockState state) {
        super(Tier4BlockEntities.KILN.get(), pos, state);
    }

    /** Whether the kiln fires this: anything a pit kiln fires, and slag dust. */
    public static boolean fires(ItemStack stack) {
        return KilnFiring.isFireable(stack) || stack.is(Tier4Items.SLAG_DUST.get());
    }

    /** What a loaded stack fires into; slag dust short of four makes nothing and waits. */
    public static ItemStack fired(ItemStack stack) {
        if (stack.is(Tier4Items.SLAG_DUST.get())) {
            int wool = stack.getCount() / SLAG_PER_WOOL;
            return wool <= 0 ? ItemStack.EMPTY : new ItemStack(Tier4Items.SLAG_WOOL.get(), wool);
        }
        return KilnFiring.isFireable(stack) ? KilnFiring.fire(stack) : ItemStack.EMPTY;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, KilnBlockEntity kiln) {
        kiln.intake.roll();
        Status before = kiln.status;
        kiln.status = kiln.work((ServerLevel) level, pos);
        if (kiln.status != before || kiln.status == Status.FIRING) kiln.setChanged();
        boolean lit = kiln.status == Status.FIRING;
        if (state.getValue(KilnBlock.LIT) != lit) level.setBlock(pos, state.setValue(KilnBlock.LIT, lit), Block.UPDATE_ALL);
    }

    private Status work(ServerLevel level, BlockPos pos) {
        List<ItemStack> batch = batch();
        if (batch.isEmpty()) {
            progress = 0;
            for (int slot : INPUT_SLOTS) if (!items.get(slot).isEmpty()) return Status.NO_RECIPE;
            return Status.EMPTY;
        }
        if (!fits(batch)) return Status.OUTPUT_FULL;
        if (intake.heat() <= 0) return Status.NEEDS_HEAT;
        // A short supply of heat fires the batch more slowly.
        progress += intake.share();
        if (progress < BATCH_TICKS) return Status.FIRING;
        progress = 0;
        fire();
        level.playSound(null, pos, Tier4Sounds.KILN_DONE.get(), SoundSource.BLOCKS, 0.8f, 0.9f + level.getRandom().nextFloat() * 0.2f);
        level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 8, 0.2, 0.05, 0.2, 0.02);
        return Status.FIRING;
    }

    /** What the loaded inputs fire into, one stack per input that makes something. */
    private List<ItemStack> batch() {
        List<ItemStack> batch = new ArrayList<>();
        for (int slot : INPUT_SLOTS) {
            ItemStack made = fired(items.get(slot));
            if (!made.isEmpty()) batch.add(made);
        }
        return batch;
    }

    private void fire() {
        for (int slot : INPUT_SLOTS) {
            ItemStack input = items.get(slot);
            ItemStack made = fired(input);
            if (made.isEmpty()) continue;
            insert(made);
            if (input.is(Tier4Items.SLAG_DUST.get())) input.shrink(made.getCount() * SLAG_PER_WOOL);
            else items.set(slot, ItemStack.EMPTY);
        }
    }

    /** Whether every stack would fit in the output slots at once. */
    private boolean fits(List<ItemStack> made) {
        NonNullList<ItemStack> copy = NonNullList.withSize(OUTPUTS, ItemStack.EMPTY);
        for (int i = 0; i < OUTPUTS; i++) copy.set(i, items.get(OUTPUT_SLOTS[i]).copy());
        for (ItemStack stack : made) {
            if (!merge(copy, stack.copy()).isEmpty()) return false;
        }
        return true;
    }

    private void insert(ItemStack stack) {
        NonNullList<ItemStack> outputs = NonNullList.withSize(OUTPUTS, ItemStack.EMPTY);
        for (int i = 0; i < OUTPUTS; i++) outputs.set(i, items.get(OUTPUT_SLOTS[i]));
        merge(outputs, stack);
        for (int i = 0; i < OUTPUTS; i++) items.set(OUTPUT_SLOTS[i], outputs.get(i));
    }

    /** Merges {@code stack} into {@code slots}, filling matching stacks first, and returns what is left. */
    private static ItemStack merge(NonNullList<ItemStack> slots, ItemStack stack) {
        for (int pass = 0; pass < 2 && !stack.isEmpty(); pass++) {
            for (int i = 0; i < slots.size() && !stack.isEmpty(); i++) {
                ItemStack there = slots.get(i);
                if (there.isEmpty()) {
                    if (pass == 0) continue;
                    int move = Math.min(stack.getCount(), stack.getMaxStackSize());
                    slots.set(i, stack.copyWithCount(move));
                    stack.shrink(move);
                } else if (ItemStack.isSameItemSameComponents(there, stack)) {
                    int move = Math.min(stack.getCount(), there.getMaxStackSize() - there.getCount());
                    there.grow(move);
                    stack.shrink(move);
                }
            }
        }
        return stack;
    }

    public Status status() {
        return status;
    }

    /** The heat that came in last tick, for tests. */
    public HeatIntake intake() {
        return intake;
    }

    // ------------------------------------------------------------------ heat

    @Override
    public boolean connectsHeat(Direction side) {
        return true;
    }

    @Override
    public int heatDemand(float temperature) {
        return intake.demand(temperature, status == Status.FIRING || status == Status.NEEDS_HEAT);
    }

    @Override
    public int offerHeat(float temperature, int heat) {
        return intake.offer(temperature, heat);
    }

    @Override
    public void heatRoute(int pipes, @Nullable HeatPipeBlock limitedBy) {
        intake.route(limitedBy);
    }

    // ------------------------------------------------------------------ container

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? OUTPUT_SLOTS : INPUT_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= INPUTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot < INPUTS && fires(stack);
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
        return Component.translatable("container." + StrataIndustria.MOD_ID + ".kiln");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new KilnMenu(id, inventory, worldPosition, this, data);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(in, items);
        progress = in.getFloatOr("progress", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        ContainerHelper.saveAllItems(out, items);
        out.putFloat("progress", progress);
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
