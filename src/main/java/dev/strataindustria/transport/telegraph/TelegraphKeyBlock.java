package dev.strataindustria.transport.telegraph;

import dev.strataindustria.transport.outpost.RouteIndex;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The telegraph key (outposts spec 9.1). Click it to send; a rising edge of redstone sends too. {@code FACING} is the
 * way the operator looks, so the container it reports the fill of is the one in front of it.
 */
public class TelegraphKeyBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty PRESSED = BooleanProperty.create("pressed");
    /** Ticks the lever stays down after a press. */
    static final int HOLD = 3;
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 5, 14);

    public TelegraphKeyBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PRESSED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PRESSED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
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

    /** Standing in the world: it looks for a pole to hang its drop wire on. */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!(level instanceof ServerLevel server) || oldState.is(state.getBlock())) return;
        BlockPos pole = TelegraphIndex.get(server).attach(server, pos, TelegraphIndex.Kind.KEY);
        if (pole != null) TelegraphLine.dropConnected(server, pos, pole);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof TelegraphKeyBlockEntity key) key.press(server, state, player instanceof ServerPlayer p ? p : null);
        return InteractionResult.SUCCESS_SERVER;
    }

    /** A rising edge of redstone presses the key. */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        if (!(level instanceof ServerLevel server) || !(level.getBlockEntity(pos) instanceof TelegraphKeyBlockEntity key)) return;
        boolean powered = level.hasNeighborSignal(pos);
        if (powered != key.wasPowered()) {
            key.setWasPowered(powered);
            if (powered) key.press(server, state, null);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(PRESSED)) return;
        level.setBlock(pos, state.setValue(PRESSED, false), Block.UPDATE_ALL);
        level.playSound(null, pos, TelegraphRegistry.KEY_UP.get(), net.minecraft.sounds.SoundSource.BLOCKS, 0.5f, 1.0f);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        TelegraphIndex.get(level).unregister(pos);
        RouteIndex.get(level).cut(level, pos);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TelegraphKeyBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!(level instanceof ServerLevel) || type != TelegraphRegistry.KEY_ENTITY.get()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<TelegraphKeyBlockEntity>) (l, p, s, key) -> key.serverTick((ServerLevel) l, s);
    }
}
