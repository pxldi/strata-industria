package dev.strataindustria.power;

import dev.strataindustria.registry.ModBlockEntities;
import dev.strataindustria.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Turns at 16 RPM for a few ticks after each use. Holding right-click repeats the use every 4
 * ticks, so it keeps turning for as long as the player holds on, and tires them like sprinting.
 */
public class HandCrankBlockEntity extends KineticBlockEntity implements KineticSource {
    public static final float SPEED = 16.0f;
    public static final int CAPACITY = 64;
    /** A little longer than the 4-tick use repeat, so the crank does not stutter between uses. */
    private static final int HOLD_TICKS = 6;
    /** 0.05 exhaustion per tick of cranking, paid per use. */
    private static final float EXHAUSTION_PER_USE = 0.05f * 4;

    private int turning;

    public HandCrankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HAND_CRANK.get(), pos, state);
    }

    public void crank(Player player) {
        if (level == null) return;
        boolean started = turning == 0;
        turning = HOLD_TICKS;
        player.causeFoodExhaustion(EXHAUSTION_PER_USE);
        if (started) KineticNetworks.markDirty(level, worldPosition);
        if (level.getGameTime() % 12 < 4) {
            level.playSound(null, worldPosition, ModSounds.HAND_CRANK_TURN.get(), SoundSource.BLOCKS, 0.4f,
                    0.9f + level.getRandom().nextFloat() * 0.2f);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, HandCrankBlockEntity crank) {
        if (crank.turning > 0 && --crank.turning == 0) KineticNetworks.markDirty(level, pos);
    }

    @Override
    public float sourceSpeed() {
        return turning > 0 ? SPEED : 0.0f;
    }

    @Override
    public int capacity() {
        return CAPACITY;
    }
}
