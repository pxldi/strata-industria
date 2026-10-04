package dev.strataindustria.power;

import dev.strataindustria.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * A step-up gearbox (spec 7.3). FACING is the output face, which turns twice as fast as the input on
 * the back; driven from the front it steps down by half. The sides do not connect.
 */
public class StepUpGearboxBlock extends BaseEntityBlock implements KineticBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final float RATIO = 2.0f;

    public StepUpGearboxBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** The output faces away from whoever places it. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection());
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
    public boolean connects(BlockState state, Direction side) {
        return side.getAxis() == state.getValue(FACING).getAxis();
    }

    /** Measured from the input shaft: the output is twice as fast. */
    @Override
    public float ratio(BlockState state, @Nullable Direction from, Direction to) {
        Direction out = state.getValue(FACING);
        if (from == null) return to == out ? RATIO : 1.0f;
        if (from == to) return 1.0f;
        return to == out ? RATIO : 1.0f / RATIO;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return Kinetics.report(level, pos, player) ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new KineticBlockEntity(ModBlockEntities.KINETIC_TRANSMISSION.get(), pos, state);
    }
}
