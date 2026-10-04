package dev.strataindustria.transport.telegraph;

import dev.strataindustria.transport.outpost.RouteIndex;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The telegraph sounder (outposts spec 9.1): a magnet and an armature on a sounding board. A signal on the line pulls the
 * armature down with a clack, and the sounder gives redstone for four ticks; the armature lifts with a lighter tick.
 */
public class TelegraphSounderBlock extends Block {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    /** Ticks of redstone a clack gives. */
    public static final int PULSE = 4;
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 7, 14);

    public TelegraphSounderBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!(level instanceof ServerLevel server) || oldState.is(state.getBlock())) return;
        BlockPos pole = TelegraphIndex.get(server).attach(server, pos, TelegraphIndex.Kind.SOUNDER);
        if (pole != null) TelegraphLine.dropConnected(server, pos, pole);
    }

    /** The armature comes down: clack, redstone, a few sparks off the magnet. A second signal while it is down holds it. */
    public static void clack(ServerLevel level, BlockPos pos, BlockState state) {
        boolean wasDown = state.getValue(POWERED);
        if (!wasDown) level.setBlock(pos, state.setValue(POWERED, true), Block.UPDATE_ALL);
        level.scheduleTick(pos, state.getBlock(), PULSE);
        float pitch = 0.95f + level.getRandom().nextFloat() * 0.1f;
        level.playSound(null, pos, TelegraphRegistry.SOUNDER_CLACK.get(), SoundSource.BLOCKS, 0.9f, pitch);
        Vec3 magnet = Vec3.atCenterOf(pos).add(0, 0.15, 0);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, magnet.x, magnet.y, magnet.z, 2, 0.12, 0.05, 0.12, 0.01);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(POWERED)) return;
        level.setBlock(pos, state.setValue(POWERED, false), Block.UPDATE_ALL);
        level.playSound(null, pos, TelegraphRegistry.SOUNDER_LIFT.get(), SoundSource.BLOCKS, 0.5f, 1.0f);
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(POWERED) ? 15 : 0;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        TelegraphIndex.get(level).unregister(pos);
        RouteIndex.get(level).cut(level, pos);
    }
}
