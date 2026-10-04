package dev.strataindustria.fluid;

import dev.strataindustria.registry.Tier4Sounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The fluid filter (tier 5 spec 12.4): an inline pipe segment that only lets one fluid through. A bucket
 * of that fluid right-clicked on it sets the fluid; unset, it takes the first fluid that arrives. It ends
 * the pipe network on each side, so every network still carries one fluid.
 */
public class FluidFilterBlock extends BaseEntityBlock {
    /** The pipe runs along this direction's axis. */
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    private static final VoxelShape[] SHAPES = new VoxelShape[3];

    static {
        VoxelShape housing = Block.box(3, 3, 3, 13, 13, 13);
        SHAPES[Direction.Axis.X.ordinal()] = Shapes.or(housing, Block.box(0, 5.5, 5.5, 16, 10.5, 10.5));
        SHAPES[Direction.Axis.Y.ordinal()] = Shapes.or(housing, Block.box(5.5, 0, 5.5, 10.5, 16, 10.5));
        SHAPES[Direction.Axis.Z.ordinal()] = Shapes.or(housing, Block.box(5.5, 5.5, 0, 10.5, 10.5, 16));
    }

    public FluidFilterBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
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
        return SHAPES[state.getValue(FACING).getAxis().ordinal()];
    }

    /** A bucket sets the fluid; the bucket stays full. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        Fluid carried = FluidBuckets.fluidOf(stack);
        if (carried == null || !(level.getBlockEntity(pos) instanceof FluidFilterBlockEntity filter)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide()) {
            filter.setFluid(carried);
            level.playSound(null, pos, Tier4Sounds.FILTER_CONFIGURE.get(), SoundSource.BLOCKS, 0.6f, 1.2f);
            player.sendOverlayMessage(filter.report());
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof FluidFilterBlockEntity filter)) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown() && filter.isSet()) {
            filter.setFluid(null);
            level.playSound(null, pos, Tier4Sounds.FILTER_CONFIGURE.get(), SoundSource.BLOCKS, 0.6f, 0.85f);
        }
        player.sendOverlayMessage(filter.report());
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidFilterBlockEntity(pos, state);
    }
}
