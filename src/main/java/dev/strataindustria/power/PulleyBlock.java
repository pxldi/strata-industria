package dev.strataindustria.power;

import dev.strataindustria.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A pulley (spec 7.3): sits on an axle line like an axle, and a leather belt joins it to another
 * pulley on a parallel axis up to 8 blocks away, at 1:1. The renderer draws the wheel and the belt.
 */
public class PulleyBlock extends BaseEntityBlock implements KineticBlock {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    private static final VoxelShape X = Shapes.or(Block.box(0, 5, 5, 16, 11, 11), Block.box(5, 1.5, 1.5, 11, 14.5, 14.5));
    private static final VoxelShape Y = Shapes.or(Block.box(5, 0, 5, 11, 16, 11), Block.box(1.5, 5, 1.5, 14.5, 11, 14.5));
    private static final VoxelShape Z = Shapes.or(Block.box(5, 5, 0, 11, 11, 16), Block.box(1.5, 1.5, 5, 14.5, 14.5, 11));

    public PulleyBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.Y));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(AXIS, context.getClickedFace().getAxis());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        Direction.Axis axis = state.getValue(AXIS);
        if (axis == Direction.Axis.Y || rotation == Rotation.NONE || rotation == Rotation.CLOCKWISE_180) return state;
        return state.setValue(AXIS, axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(AXIS)) {
            case X -> X;
            case Y -> Y;
            case Z -> Z;
        };
    }

    @Override
    public boolean connects(BlockState state, Direction side) {
        return side.getAxis() == state.getValue(AXIS);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return Kinetics.report(level, pos, player) ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PulleyBlockEntity(pos, state);
    }
}
