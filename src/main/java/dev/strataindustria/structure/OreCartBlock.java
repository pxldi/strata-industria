package dev.strataindustria.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A wooden mine cart with a nine-slot load that shows how full it is (structures v2 section 5). */
public class OreCartBlock extends NineSlotBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    /** 0 empty, 1 a layer of ore, 2 heaped, 3 heaped high. */
    public static final IntegerProperty FILL = IntegerProperty.create("fill", 0, 3);

    private static final VoxelShape ALONG_Z = Block.box(0, 0, 2, 16, 11, 14);
    private static final VoxelShape ALONG_X = Block.box(2, 0, 0, 14, 11, 16);

    public OreCartBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FILL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FILL);
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
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? ALONG_Z : ALONG_X;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
            BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (direction == Direction.DOWN && !canSurvive(state, level, pos)) return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OreCartBlockEntity(pos, state);
    }
}
