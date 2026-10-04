package dev.strataindustria.transport.ropeway;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The angle station (outposts spec 8.1): a steel tower with a wheel of its own that turns the line by up to 90 degrees.
 * Buckets pass round it, and a chest behind or beneath it tops up the loads that pass. An empty hand rides the line
 * home from here. {@code FACING} points back along the loading chest's side, like the return's.
 */
public class RopewayAngleBlock extends RopewayTowerBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape SHAPE = Block.box(0.5, 0, 0.5, 15.5, 15, 15.5);

    public RopewayAngleBlock(Properties properties) {
        super(properties, true);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
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

    /** An empty hand rides the line; sneaking asks the station how it stands. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (holdsLineGear(player)) return InteractionResult.PASS;
        if (player.isShiftKeyDown() || !(player instanceof ServerPlayer rider)) return super.useWithoutItem(state, level, pos, player, hit);
        if (level.getBlockEntity(pos) instanceof RopewayTowerBlockEntity tower && tower.terminal() != null
                && level.getBlockEntity(tower.terminal()) instanceof RopewayTerminalBlockEntity terminal) {
            return terminal.ride(rider, pos);
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }
}
