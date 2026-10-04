package dev.strataindustria.oil;

import dev.strataindustria.registry.Tier6Sounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

/**
 * Crude oil in the world (spec 5.1): slow, thick, not flammable as a block. Its block speed factor slows
 * whatever wades through it like honey. A still pool now and then lets a slow bubble break on its surface.
 */
public class CrudeOilBlock extends LiquidBlock {
    public CrudeOilBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        if (!state.getFluidState().isSource() || !level.getBlockState(pos.above()).isAir() || random.nextInt(120) != 0) return;
        double x = pos.getX() + 0.2 + random.nextDouble() * 0.6, z = pos.getZ() + 0.2 + random.nextDouble() * 0.6;
        level.addParticle(ParticleTypes.SMOKE, x, pos.getY() + 0.9, z, 0, 0.01, 0);
        level.playLocalSound(x, pos.getY() + 0.9, z, Tier6Sounds.CRUDE_OIL_BUBBLE.get(), SoundSource.BLOCKS,
                0.3f + random.nextFloat() * 0.2f, 0.8f + random.nextFloat() * 0.2f, false);
    }
}
