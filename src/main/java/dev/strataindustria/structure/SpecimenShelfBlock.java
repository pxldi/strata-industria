package dev.strataindustria.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
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
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.Mirror;
import dev.strataindustria.journal.Journal;
import net.minecraft.server.level.ServerPlayer;

/**
 * A wall shelf with four cubbies for mineral specimens (structures v2 section 5). Click with a specimen to
 * set it in, click with an empty hand to take the last one back. Four different minerals on one shelf is a
 * collection page in the journal.
 */
public class SpecimenShelfBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    /** Shelf depth in sixteenths. */
    public static final int DEPTH = 6;
    private static final VoxelShape NORTH = Block.box(0, 0, 16 - DEPTH, 16, 16, 16);
    private static final VoxelShape SOUTH = Block.box(0, 0, 0, 16, 16, DEPTH);
    private static final VoxelShape EAST = Block.box(0, 0, 0, DEPTH, 16, 16);
    private static final VoxelShape WEST = Block.box(16 - DEPTH, 0, 0, 16, 16, 16);

    public SpecimenShelfBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // The front faces the player, so the back sits against the wall they clicked.
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

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(StructureContent.MINERAL_SPECIMEN.get())) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!(level.getBlockEntity(pos) instanceof SpecimenShelfBlockEntity shelf)) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            ItemStack single = stack.copyWithCount(1);
            if (!shelf.place(single)) return InteractionResult.CONSUME;
            if (!player.hasInfiniteMaterials()) stack.shrink(1);
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.8f, 1.2f);
            if (shelf.isFullCollection() && player instanceof ServerPlayer serverPlayer) {
                Journal.award(serverPlayer, SpecimenShelfBlockEntity.COLLECTION);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof SpecimenShelfBlockEntity shelf) || shelf.isEmpty()) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            ItemStack taken = shelf.takeLast();
            if (!player.addItem(taken)) Block.popResource(level, pos, taken);
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.8f, 1.0f);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SpecimenShelfBlockEntity shelf && !player.isCreative()) {
            shelf.dropContents(level, pos);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SpecimenShelfBlockEntity(pos, state);
    }
}
