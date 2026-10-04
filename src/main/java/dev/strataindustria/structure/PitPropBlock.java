package dev.strataindustria.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A squared timber post that holds up a mine roof (structures spec 9). In the dark it creaks now and
 * then and lets a little grit fall from its cap: the only sound the old mines make.
 */
public class PitPropBlock extends RotatedPillarBlock {
    private static final VoxelShape Y = Block.box(2, 0, 2, 14, 16, 14);
    private static final VoxelShape X = Block.box(0, 2, 2, 16, 14, 14);
    private static final VoxelShape Z = Block.box(2, 2, 0, 14, 14, 16);
    /** Players further away than this never hear a prop. */
    private static final double HEARING = 12;
    /**
     * Chance per animation tick. A block near the player gets an animation tick about every 50 game ticks,
     * so a tunnel of twenty props creaks roughly every six seconds.
     */
    private static final int CREAK_ODDS = 50;

    public PitPropBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(AXIS)) {
            case X -> X;
            case Z -> Z;
            default -> Y;
        };
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(CREAK_ODDS) != 0) return;
        if (level.getBrightness(LightLayer.SKY, pos) > 0) return;
        Player player = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, HEARING, false);
        if (player == null) return;
        level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, StructureContent.PIT_PROP_CREAK.get(),
                SoundSource.BLOCKS, 0.5f, 0.9f + random.nextFloat() * 0.2f, false);
        BlockParticleOption dust = new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.GRAVEL.defaultBlockState());
        int count = 1 + random.nextInt(3);
        for (int i = 0; i < count; i++) {
            double x = pos.getX() + 0.2 + random.nextDouble() * 0.6;
            double z = pos.getZ() + 0.2 + random.nextDouble() * 0.6;
            level.addParticle(dust, x, pos.getY() + (state.getValue(AXIS) == Direction.Axis.Y ? 0.95 : 0.1), z, 0, 0, 0);
        }
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 5;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 5;
    }
}
