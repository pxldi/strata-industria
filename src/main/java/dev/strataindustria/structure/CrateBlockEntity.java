package dev.strataindustria.structure;

import dev.strataindustria.StrataIndustria;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/** The crate's nine slots and, when a structure locked it, the puzzle that opens it. */
public class CrateBlockEntity extends NineSlotBlockEntity {
    private @Nullable PuzzleLock puzzle;

    public CrateBlockEntity(BlockPos pos, BlockState state) {
        super(SharedBlocks.CRATE_ENTITY.get(), pos, state);
    }

    @Override
    protected Component name() {
        return Component.translatable("block." + StrataIndustria.MOD_ID + ".crate");
    }

    @Override
    protected SoundEvent openSound() {
        return SharedBlocks.CRATE_OPEN.get();
    }

    @Override
    protected SoundEvent closeSound() {
        return SharedBlocks.CRATE_CLOSE.get();
    }

    public boolean isLocked() {
        return getBlockState().getValue(CrateBlock.LOCKED);
    }

    public @Nullable PuzzleLock puzzle() {
        return puzzle;
    }

    /** Nails the crate shut until {@code lock} is solved. */
    public void lock(PuzzleLock lock) {
        puzzle = lock;
        if (level != null) level.setBlock(worldPosition, getBlockState().setValue(CrateBlock.LOCKED, true), 3);
        setChanged();
    }

    /** A puzzle lamp near this crate was lit or snuffed by a player. */
    void lampChanged(ServerLevel level, BlockPos lamp, boolean lit) {
        if (puzzle == null || !puzzle.ordered()) return;
        int index = puzzle.indexOf(lamp);
        if (index < 0) return;
        if (lit && index == puzzle.progress()) {
            puzzle = puzzle.withProgress(index + 1);
            setChanged();
            if (puzzle.progress() == puzzle.steps().size()) unlock(level);
        } else {
            // Out of turn, or one of the lit lamps was put out: they all gutter out and the order starts over.
            for (PuzzleLock.Step step : puzzle.steps()) {
                BlockState state = level.getBlockState(step.pos());
                if (state.getBlock() instanceof MinersLampBlock && state.getValue(MinersLampBlock.MODE) != MinersLampBlock.Mode.OFF) {
                    level.setBlock(step.pos(), state.setValue(MinersLampBlock.MODE, MinersLampBlock.Mode.OFF), 3);
                    level.playSound(null, step.pos(), SharedBlocks.MINERS_LAMP_SNUFF.get(), SoundSource.BLOCKS, 0.6f, 0.8f);
                }
            }
            puzzle = puzzle.withProgress(0);
            setChanged();
        }
    }

    /** Something was built near this crate; opens it if a block puzzle now holds. */
    void recheck(ServerLevel level) {
        if (puzzle != null && !puzzle.ordered() && puzzle.solved(level)) unlock(level);
    }

    private void unlock(ServerLevel level) {
        puzzle = null;
        level.setBlock(worldPosition, getBlockState().setValue(CrateBlock.LOCKED, false), 3);
        level.playSound(null, worldPosition, SharedBlocks.CRATE_UNLOCK.get(), SoundSource.BLOCKS, 0.9f, 1.0f);
        level.sendParticles(ParticleTypes.POOF, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5,
                8, 0.25, 0.1, 0.25, 0.01);
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.storeNullable("puzzle", PuzzleLock.CODEC, puzzle);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        puzzle = input.read("puzzle", PuzzleLock.CODEC).orElse(null);
    }
}
