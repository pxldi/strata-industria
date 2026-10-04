package dev.strataindustria.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Client-side only state of the windlass: the drum eases up to a slow turn while a player is within reach
 * and eases back to rest when they leave, creaking now and then while it moves.
 */
public class WindlassBlockEntity extends BlockEntity {
    /** Blocks within which the drum turns. */
    public static final double REACH = 8.0;
    private static final float TOP_SPEED = 1.1f;

    private float angle;
    private float speed;

    public WindlassBlockEntity(BlockPos pos, BlockState state) {
        super(SharedBlocks.WINDLASS_ENTITY.get(), pos, state);
    }

    public float angle(float partialTick) {
        return angle + speed * partialTick;
    }

    static void clientTick(Level level, BlockPos pos, BlockState state, WindlassBlockEntity windlass) {
        Player player = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, REACH, false);
        float target = player == null ? 0f : TOP_SPEED;
        windlass.speed += (target - windlass.speed) * 0.04f;
        if (windlass.speed < 0.01f) windlass.speed = 0f;
        windlass.angle = (windlass.angle + windlass.speed) % 360f;
        if (windlass.speed > 0.4f && level.getRandom().nextInt(90) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SharedBlocks.WINDLASS_CREAK.get(),
                    SoundSource.BLOCKS, 0.5f, 0.85f + level.getRandom().nextFloat() * 0.3f, false);
        }
    }
}
