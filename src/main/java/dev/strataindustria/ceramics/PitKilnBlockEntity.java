package dev.strataindustria.ceramics;

import dev.strataindustria.Config;
import dev.strataindustria.fire.Ignitable;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.registry.ModBlockEntities;
import dev.strataindustria.registry.ModSounds;
import dev.strataindustria.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Pieces laid out for a pit kiln (spec 4.2): four spots in a 2x2, the logs stacked on the thatch, and
 * the burn progress. A vessel or crucible fills all four spots.
 */
public class PitKilnBlockEntity extends BlockEntity {
    public static final int SPOTS = 4;
    public static final int SMALL_STACK = 16;
    public static final int RAIN_TICKS_TO_DOUSE = 200;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SPOTS, ItemStack.EMPTY);
    private final NonNullList<ItemStack> logs = NonNullList.withSize(PitKilnBlock.MAX_LAYERS, ItemStack.EMPTY);
    private int burnTicks;
    private int rainTicks;

    public PitKilnBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PIT_KILN.get(), pos, state);
    }

    public static boolean isLarge(ItemStack stack) {
        return stack.is(ModTags.Items.PIT_KILN_LARGE);
    }

    public NonNullList<ItemStack> items() {
        return items;
    }

    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    /** Places one item from {@code held}. Returns whether anything was placed. */
    public boolean place(ItemStack held) {
        if (!items.get(0).isEmpty() && isLarge(items.get(0))) return false;
        if (isLarge(held)) {
            if (!isEmpty()) return false;
            items.set(0, held.split(1));
            changed();
            return true;
        }
        for (ItemStack spot : items) {
            if (!spot.isEmpty() && ItemStack.isSameItemSameComponents(spot, held) && spot.getCount() < SMALL_STACK) {
                spot.grow(1);
                held.shrink(1);
                changed();
                return true;
            }
        }
        for (int i = 0; i < SPOTS; i++) {
            if (items.get(i).isEmpty()) {
                items.set(i, held.split(1));
                changed();
                return true;
            }
        }
        return false;
    }

    /** Takes back the whole stack in the last filled spot. */
    public ItemStack takeLast() {
        for (int i = SPOTS - 1; i >= 0; i--) {
            if (!items.get(i).isEmpty()) {
                ItemStack taken = items.get(i);
                items.set(i, ItemStack.EMPTY);
                changed();
                return taken;
            }
        }
        return ItemStack.EMPTY;
    }

    void addLog(ItemStack log) {
        for (int i = 0; i < logs.size(); i++) {
            if (logs.get(i).isEmpty()) {
                logs.set(i, log.copyWithCount(1));
                changed();
                return;
            }
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PitKilnBlockEntity kiln) {
        if (!state.getValue(PitKilnBlock.LIT)) return;
        if (Ignitable.rainedOn(level, pos)) {
            // Rain puts the kiln out after a while; the progress so far is kept for relighting.
            if (++kiln.rainTicks >= RAIN_TICKS_TO_DOUSE) {
                kiln.rainTicks = 0;
                level.setBlock(pos, state.setValue(PitKilnBlock.LIT, false), Block.UPDATE_ALL);
                level.playSound(null, pos, ModSounds.FIRE_PIT_EXTINGUISH.get(), SoundSource.BLOCKS, 1.0f, 0.9f);
                kiln.setChanged();
                return;
            }
        } else if (kiln.rainTicks > 0) {
            kiln.rainTicks--;
        }
        if (++kiln.burnTicks >= Config.KILN_BURN_TICKS.getAsInt()) {
            kiln.finishFiring(level, pos, state);
        } else if (kiln.burnTicks % 20 == 0) {
            kiln.setChanged();
        }
    }

    private void finishFiring(Level level, BlockPos pos, BlockState state) {
        for (int i = 0; i < SPOTS; i++) items.set(i, KilnFiring.fire(items.get(i)));
        for (int i = 0; i < logs.size(); i++) logs.set(i, ItemStack.EMPTY);
        burnTicks = 0;
        rainTicks = 0;
        level.setBlock(pos, state.setValue(PitKilnBlock.STRAW, 0).setValue(PitKilnBlock.LOGS, 0).setValue(PitKilnBlock.LIT, false),
                Block.UPDATE_ALL);
        level.playSound(null, pos, ModSounds.KILN_FIRED.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
        Journal.awardNear(level, pos, Journal.PIT_KILN_FIRED);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5,
                    12, 0.3, 0.2, 0.3, 0.02);
        }
        changed();
    }

    /** Fraction of the burn done, for the debug overlay and the journal. */
    public float progress() {
        return burnTicks / (float) Config.KILN_BURN_TICKS.getAsInt();
    }

    /** Spills everything still in the kiln: pieces, logs that have not burnt, and the straw. */
    void dropContents(Level level, BlockPos pos, int straw) {
        Containers.dropContents(level, pos, items);
        Containers.dropContents(level, pos, logs);
        if (straw > 0) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(),
                new ItemStack(dev.strataindustria.registry.ModItems.STRAW.get(), straw));
    }

    private void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (int i = 0; i < SPOTS; i++) items.set(i, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        for (int i = 0; i < logs.size(); i++) logs.set(i, ItemStack.EMPTY);
        input.child("logs").ifPresent(child -> ContainerHelper.loadAllItems(child, logs));
        burnTicks = input.getIntOr("burn_ticks", 0);
        rainTicks = input.getIntOr("rain_ticks", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items, true);
        ContainerHelper.saveAllItems(output.child("logs"), logs, true);
        output.putInt("burn_ticks", burnTicks);
        output.putInt("rain_ticks", rainTicks);
    }

    // The client renders the placed pieces, so it needs the items.
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
