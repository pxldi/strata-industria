package dev.strataindustria.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A wall rack with three pegs (structures v2 section 5). Click with a tool to hang it, with an empty hand to
 * take the last one down. Worn tools hang on the wall instead of lying in a chest.
 */
public class ToolRackBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    /** Depth in sixteenths. */
    public static final int DEPTH = 3;
    private static final VoxelShape NORTH = Block.box(0, 0, 16 - DEPTH, 16, 16, 16);
    private static final VoxelShape SOUTH = Block.box(0, 0, 0, 16, 16, DEPTH);
    private static final VoxelShape EAST = Block.box(0, 0, 0, DEPTH, 16, 16);
    private static final VoxelShape WEST = Block.box(16 - DEPTH, 0, 0, 16, 16, 16);

    public ToolRackBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        return defaultBlockState().setValue(FACING, face.getAxis().isHorizontal() ? face : context.getHorizontalDirection().getOpposite());
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
        return switch (state.getValue(FACING)) {
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            default -> NORTH;
        };
    }

    /** Anything with a durability bar or a tool component can hang on a peg. */
    static boolean hangs(ItemStack stack) {
        return !stack.isEmpty() && (stack.isDamageableItem() || stack.has(DataComponents.TOOL));
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (!hangs(stack)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!(level.getBlockEntity(pos) instanceof ToolRackBlockEntity rack)) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            if (!rack.hang(stack.copyWithCount(1))) return InteractionResult.CONSUME;
            if (!player.hasInfiniteMaterials()) stack.shrink(1);
            level.playSound(null, pos, SharedBlocks.TOOL_RACK_CLINK.get(), SoundSource.BLOCKS, 0.7f, 1.0f + level.getRandom().nextFloat() * 0.2f);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ToolRackBlockEntity rack) || rack.isEmpty()) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            ItemStack taken = rack.takeLast();
            if (!player.addItem(taken)) Block.popResource(level, pos, taken);
            level.playSound(null, pos, SharedBlocks.TOOL_RACK_CLINK.get(), SoundSource.BLOCKS, 0.7f, 0.8f);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ToolRackBlockEntity(pos, state);
    }
}
