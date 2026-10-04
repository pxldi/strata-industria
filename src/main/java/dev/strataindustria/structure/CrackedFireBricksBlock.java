package dev.strataindustria.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Fire bricks cracked by heat. In a ruin they still tick now and then as if cooling, and let a little grit
 * fall; about one tick in half a minute for a whole stack, heard only within eight blocks.
 */
public class CrackedFireBricksBlock extends Block {
    private static final double HEARING = 8.0;

    public CrackedFireBricksBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(900) != 0) return;
        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        if (level.getNearestPlayer(x, y, z, HEARING, false) == null) return;
        level.playLocalSound(x, y, z, StructureContent.CRACKED_FIRE_BRICKS_SETTLE.get(), SoundSource.BLOCKS, 0.5f,
                0.9f + random.nextFloat() * 0.2f, false);
        for (int i = 0; i < 4; i++) {
            level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state), x + (random.nextDouble() - 0.5) * 0.8,
                    y - 0.45, z + (random.nextDouble() - 0.5) * 0.8, 0.0, -0.05, 0.0);
        }
    }
}
