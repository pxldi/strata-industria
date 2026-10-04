package dev.strataindustria.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * An old charcoal pit still glowing inside (structures v2 section 5): campfire smoke, a spit of embers and
 * a low crackle. Found only in old clearings. Breaking it gives ash, not charcoal.
 */
public class SmoulderingLogPileBlock extends Block {
    public SmoulderingLogPileBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
        double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
        if (random.nextInt(8) == 0) {
            level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, pos.getY() + 1.0, z, 0.0, 0.07, 0.0);
        }
        if (random.nextInt(12) == 0) level.addParticle(ParticleTypes.LAVA, x, pos.getY() + 1.0, z, 0.0, 0.0, 0.0);
        if (random.nextInt(60) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SharedBlocks.SMOULDERING_CRACKLE.get(),
                    SoundSource.BLOCKS, 0.6f, 0.8f + random.nextFloat() * 0.3f, false);
        }
    }
}
