package dev.strataindustria.charcoal;

import dev.strataindustria.Config;
import dev.strataindustria.registry.ModBlockEntities;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModSounds;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The logs in one pile (spec 4.4) and, once lit, how long it has burnt. Up to {@link #MAX_LOGS} logs of
 * up to {@link #KINDS} kinds, so breaking it gives back what went in.
 */
public class LogPileBlockEntity extends BlockEntity {
    public static final int MAX_LOGS = 16;
    public static final int KINDS = 4;
    /** Ticks after lighting to cover every face. */
    public static final int GRACE_TICKS = 200;
    /** Most piles that burn together as one pit. */
    public static final int MAX_CONNECTED = 64;

    private final NonNullList<ItemStack> logs = NonNullList.withSize(KINDS, ItemStack.EMPTY);
    private int burnTicks;

    public LogPileBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LOG_PILE.get(), pos, state);
    }

    public int count() {
        int total = 0;
        for (ItemStack stack : logs) total += stack.getCount();
        return total;
    }

    /** Adds one log from {@code held}. Returns whether it fitted. */
    public boolean add(ItemStack held) {
        if (count() >= MAX_LOGS) return false;
        for (ItemStack stack : logs) {
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, held)) {
                stack.grow(1);
                held.shrink(1);
                setChanged();
                return true;
            }
        }
        for (int i = 0; i < KINDS; i++) {
            if (logs.get(i).isEmpty()) {
                logs.set(i, held.split(1));
                setChanged();
                return true;
            }
        }
        return false;
    }

    void dropLogs(Level level, BlockPos pos) {
        Containers.dropContents(level, pos, logs);
    }

    /** Lights this pile and every pile connected to it. */
    static void lightConnected(Level level, BlockPos start) {
        for (BlockPos pos : connected(level, start)) {
            BlockState state = level.getBlockState(pos);
            if (state.getValue(LogPileBlock.LIT)) continue;
            level.setBlock(pos, state.setValue(LogPileBlock.LIT, true), Block.UPDATE_ALL);
            if (level.getBlockEntity(pos) instanceof LogPileBlockEntity pile) {
                pile.burnTicks = 0;
                pile.setChanged();
            }
        }
    }

    static Set<BlockPos> connected(Level level, BlockPos start) {
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty() && seen.size() < MAX_CONNECTED) {
            BlockPos pos = queue.poll();
            for (Direction side : Direction.values()) {
                BlockPos next = pos.relative(side);
                if (!seen.contains(next) && level.getBlockState(next).is(ModBlocks.LOG_PILE.get())) {
                    seen.add(next);
                    queue.add(next);
                }
            }
        }
        return seen;
    }

    /** A face is covered by another pile or by a full block that does not burn. */
    static boolean covered(Level level, BlockPos pos) {
        for (Direction side : Direction.values()) {
            BlockPos next = pos.relative(side);
            BlockState state = level.getBlockState(next);
            if (state.is(ModBlocks.LOG_PILE.get())) continue;
            if (!state.isCollisionShapeFullBlock(level, next) || state.ignitedByLava()) return false;
        }
        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, LogPileBlockEntity pile) {
        if (!state.getValue(LogPileBlock.LIT)) return;
        pile.burnTicks++;
        if (pile.burnTicks > GRACE_TICKS && pile.burnTicks % 20 == 0 && !covered(level, pos)) {
            // Open to the air, it burns like any wood: no charcoal.
            level.setBlock(pos, BaseFireBlock.getState(level, pos), Block.UPDATE_ALL);
            level.playSound(null, pos, ModSounds.FIRE_PIT_IGNITE.get(), SoundSource.BLOCKS, 1.0f, 0.7f);
            return;
        }
        if (pile.burnTicks >= Config.CHARCOAL_BURN_TICKS.getAsInt()) {
            int logs = pile.count();
            int charcoal = Math.max(1, (int) Math.floor(logs * Config.CHARCOAL_PER_LOG.getAsDouble()));
            int ash = logs / 8;
            level.setBlock(pos, ModBlocks.CHARCOAL_PILE.get().defaultBlockState()
                    .setValue(CharcoalPileBlock.CHARCOAL, Math.min(CharcoalPileBlock.MAX_CHARCOAL, charcoal))
                    .setValue(CharcoalPileBlock.ASH, Math.min(CharcoalPileBlock.MAX_ASH, ash)), Block.UPDATE_ALL);
            level.playSound(null, pos, ModSounds.KILN_FIRED.get(), SoundSource.BLOCKS, 0.6f, 0.8f);
        } else if (pile.burnTicks % 20 == 0) {
            pile.setChanged();
        }
    }

    /** How far through the burn the pile is, 0 to 1. */
    public float progress() {
        return burnTicks / (float) Config.CHARCOAL_BURN_TICKS.getAsInt();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (int i = 0; i < KINDS; i++) logs.set(i, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, logs);
        burnTicks = input.getIntOr("burn_ticks", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, logs, true);
        output.putInt("burn_ticks", burnTicks);
    }
}
