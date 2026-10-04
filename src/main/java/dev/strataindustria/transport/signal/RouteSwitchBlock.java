package dev.strataindustria.transport.signal;

import dev.strataindustria.transport.outpost.RouteIndex;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The route switch (outposts spec 9.4): a T-junction of steel track that sets itself for each lead that comes up to
 * it. It stands with its trunk behind it and its straight end ahead, looking along {@link #FACING}; the branch leaves
 * to whichever side has track. A lead coming in from the trunk is sent to the branch when the stop it is bound for is
 * on the switch's list, and straight on otherwise; one coming in from either end leaves by the trunk.
 */
public class RouteSwitchBlock extends BaseRailBlock implements EntityBlock {
    public static final EnumProperty<RailShape> SHAPE = BlockStateProperties.RAIL_SHAPE;
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public RouteSwitchBlock(Properties properties) {
        super(false, properties);
        registerDefaultState(stateDefinition.any().setValue(SHAPE, RailShape.NORTH_SOUTH).setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, false));
    }

    @Override
    public Property<RailShape> getShapeProperty() {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SHAPE, FACING, WATERLOGGED);
    }

    /** The straight run along {@code facing}. */
    public static RailShape straight(Direction facing) {
        return facing.getAxis() == Direction.Axis.X ? RailShape.EAST_WEST : RailShape.NORTH_SOUTH;
    }

    /** The flat rail shape that joins two sides of a block. */
    public static RailShape joining(Direction a, Direction b) {
        if (a.getAxis() == b.getAxis()) return straight(a);
        boolean north = a == Direction.NORTH || b == Direction.NORTH, south = a == Direction.SOUTH || b == Direction.SOUTH;
        boolean east = a == Direction.EAST || b == Direction.EAST;
        if (north) return east ? RailShape.NORTH_EAST : RailShape.NORTH_WEST;
        if (south) return east ? RailShape.SOUTH_EAST : RailShape.SOUTH_WEST;
        return RailShape.NORTH_SOUTH;
    }

    /** The side the branch leaves by: the right of the switch's facing if it has track, else the left, else none. */
    public static @Nullable Direction branchOf(net.minecraft.world.level.BlockGetter level, BlockPos pos, Direction facing) {
        for (Direction side : new Direction[] {facing.getClockWise(), facing.getCounterClockWise()}) {
            BlockPos at = pos.relative(side);
            if (BaseRailBlock.isRail(level.getBlockState(at)) || BaseRailBlock.isRail(level.getBlockState(at.below())) || BaseRailBlock.isRail(level.getBlockState(at.above()))) {
                return side;
            }
        }
        return null;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection();
        boolean water = context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER;
        return defaultBlockState().setValue(FACING, facing).setValue(SHAPE, straight(facing)).setValue(WATERLOGGED, water);
    }

    /** The shape is the switch's own business: neighbouring rails do not get to bend it. */
    @Override
    protected BlockState updateState(BlockState state, Level level, BlockPos pos, boolean movedByPiston) {
        return state;
    }

    @Override
    protected void updateState(BlockState state, Level level, BlockPos pos, Block block) {}

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof RouteSwitchBlockEntity entity) {
            TimetablePayloads.openSwitch(serverPlayer, entity);
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING))).setValue(SHAPE, rotate(state.getValue(SHAPE), rotation));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING))).setValue(SHAPE, mirror(state.getValue(SHAPE), mirror));
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        RouteIndex.get(level).cut(level, pos);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RouteSwitchBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!(level instanceof ServerLevel) || type != SignalRegistry.ROUTE_SWITCH_ENTITY.get()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<RouteSwitchBlockEntity>) (l, p, s, entity) -> entity.serverTick((ServerLevel) l);
    }
}
