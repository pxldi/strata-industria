package dev.strataindustria.ceramics;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.forge.ForgeBlockEntity;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.registry.ModTags;
import dev.strataindustria.registry.PrologueRegistry;
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
 * The brick kiln: four pieces in, four out. It takes its heat from a forge directly below, like a crucible
 * does, and fires the whole load in 600 ticks once that forge is at 700 degrees or more. It fires what a
 * pit kiln fires, and nothing that needs more than a bronze age fire.
 */
public class BrickKilnBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int INPUTS = 4, OUTPUTS = 4, SLOTS = INPUTS + OUTPUTS;
    public static final int BATCH_TICKS = 600, MIN_TEMPERATURE = 700;
    public static final int DATA_PROGRESS = 0, DATA_STATUS = 1, DATA_COUNT = 2;

    public enum Status {
        EMPTY, NO_RECIPE, OUTPUT_FULL, NO_FORGE, TOO_COLD, FIRING;

        public String key() {
            return StrataIndustria.MOD_ID + ".brick_kiln.status." + name().toLowerCase(Locale.ROOT);
        }
    }

    private static final int[] INPUT_SLOTS = {0, 1, 2, 3};
    private static final int[] OUTPUT_SLOTS = {4, 5, 6, 7};

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private float progress;
    private Status status = Status.EMPTY;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> Math.round(progress * 1000 / BATCH_TICKS);
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

    public BrickKilnBlockEntity(BlockPos pos, BlockState state) {
        super(PrologueRegistry.BRICK_KILN_BE.get(), pos, state);
    }

    /** What the brick kiln fires: the same unfired clay a pit kiln fires. */
    public static boolean fires(ItemStack stack) {
        return stack.is(ModTags.Items.FIREABLE) && KilnFiring.isFireable(stack);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BrickKilnBlockEntity kiln) {
        Status before = kiln.status;
        kiln.status = kiln.work((ServerLevel) level, pos);
        if (kiln.status != before || kiln.status == Status.FIRING) kiln.setChanged();
        boolean lit = kiln.status == Status.FIRING;
        if (state.getValue(BrickKilnBlock.LIT) != lit) level.setBlock(pos, state.setValue(BrickKilnBlock.LIT, lit), Block.UPDATE_ALL);
    }

    private Status work(ServerLevel level, BlockPos pos) {
        boolean any = false, loaded = false;
        for (int slot : INPUT_SLOTS) {
            ItemStack stack = items.get(slot);
            if (stack.isEmpty()) continue;
            loaded = true;
            any |= fires(stack);
        }
        if (!any) {
            progress = 0;
            return loaded ? Status.NO_RECIPE : Status.EMPTY;
        }
        if (!fits()) return Status.OUTPUT_FULL;
        if (!(level.getBlockEntity(pos.below()) instanceof ForgeBlockEntity forge)) return Status.NO_FORGE;
        if (forge.temperature() < MIN_TEMPERATURE) return Status.TOO_COLD;
        progress++;
        if (progress < BATCH_TICKS) return Status.FIRING;
        progress = 0;
        fire();
        level.playSound(null, pos, PrologueRegistry.BRICK_KILN_DONE.get(), SoundSource.BLOCKS, 0.8f, 0.9f + level.getRandom().nextFloat() * 0.2f);
        level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 6, 0.2, 0.05, 0.2, 0.02);
        Journal.awardNear(level, pos, Journal.BRICK_KILN_FIRED);
        return Status.FIRING;
    }

    private void fire() {
        NonNullList<ItemStack> outputs = outputs();
        for (int slot : INPUT_SLOTS) {
            ItemStack input = items.get(slot);
            if (!fires(input)) continue;
            KilnBlockEntity.merge(outputs, KilnFiring.fire(input));
            items.set(slot, ItemStack.EMPTY);
        }
        for (int i = 0; i < OUTPUTS; i++) items.set(OUTPUT_SLOTS[i], outputs.get(i));
    }

    private NonNullList<ItemStack> outputs() {
        NonNullList<ItemStack> outputs = NonNullList.withSize(OUTPUTS, ItemStack.EMPTY);
        for (int i = 0; i < OUTPUTS; i++) outputs.set(i, items.get(OUTPUT_SLOTS[i]).copy());
        return outputs;
    }

    /** Whether every fired piece would fit in the output slots at once. */
    private boolean fits() {
        NonNullList<ItemStack> outputs = outputs();
        for (int slot : INPUT_SLOTS) {
            ItemStack input = items.get(slot);
            if (fires(input) && !KilnBlockEntity.merge(outputs, KilnFiring.fire(input)).isEmpty()) return false;
        }
        return true;
    }

    public Status status() {
        return status;
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
        return Component.translatable("container." + StrataIndustria.MOD_ID + ".brick_kiln");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new BrickKilnMenu(id, inventory, worldPosition, this, data);
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
