package dev.strataindustria.structure;

import dev.strataindustria.block.GroundCoverBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Broken rock lying about, one to three layers deep, in the colour of the local rock (structures v2 section
 * 5). Breaks instantly and mostly gives nothing. Placing more on a pile makes it deeper.
 */
public class RubbleBlock extends GroundCoverBlock {
    public static final IntegerProperty LAYERS = IntegerProperty.create("layers", 1, 3);
    private static final VoxelShape[] SHAPES = {Block.box(0, 0, 0, 16, 2, 16), Block.box(0, 0, 0, 16, 5, 16), Block.box(0, 0, 0, 16, 9, 16)};

    public RubbleBlock(Properties properties) {
        super(SHAPES[2], properties);
        registerDefaultState(stateDefinition.any().setValue(LAYERS, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LAYERS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(LAYERS) - 1];
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return state.getValue(LAYERS) < 3 && context.getItemInHand().is(asItem()) || super.canBeReplaced(state, context);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState there = context.getLevel().getBlockState(context.getClickedPos());
        if (there.is(this)) return there.setValue(LAYERS, Math.min(3, there.getValue(LAYERS) + 1));
        return super.getStateForPlacement(context);
    }

    /** Rubble is not picked up by hand; it is cleared by breaking it. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return InteractionResult.PASS;
    }
}
