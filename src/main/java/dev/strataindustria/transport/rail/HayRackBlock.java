package dev.strataindustria.transport.rail;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The hay rack (outposts spec 5.3): a manger of three slots that holds hay bales only. A pony standing at a stop within
 * three blocks eats from it. Hoppers and chutes can fill it; a hand puts a bale in, and a bare hand tells what is left.
 */
public class HayRackBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty FILLED = BooleanProperty.create("filled");

    public HayRackBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FILLED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FILLED);
    }

    /** The slatted back stands behind the manger, away from the player. */
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
        return switch (state.getValue(FACING)) {
            case NORTH -> Shapes.or(Block.box(1, 0, 2, 15, 6, 14), Block.box(1, 6, 2, 15, 12, 3));
            case SOUTH -> Shapes.or(Block.box(1, 0, 2, 15, 6, 14), Block.box(1, 6, 13, 15, 12, 14));
            case WEST -> Shapes.or(Block.box(2, 0, 1, 14, 6, 15), Block.box(2, 6, 1, 3, 12, 15));
            default -> Shapes.or(Block.box(2, 0, 1, 14, 6, 15), Block.box(13, 6, 1, 14, 12, 15));
        };
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Items.HAY_BLOCK)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.getBlockEntity(pos) instanceof HayRackBlockEntity rack) {
            if (!level.isClientSide() && rack.addBale()) {
                if (!player.isCreative()) stack.shrink(1);
                level.playSound(null, pos, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 0.8f, 0.9f);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof HayRackBlockEntity rack) {
            if (player.isSecondaryUseActive() && rack.takeBale()) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, new ItemStack(Items.HAY_BLOCK));
            } else {
                player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".hay_rack.stock", rack.bales()));
            }
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, boolean movedByPiston) {
        if (level.getBlockEntity(pos) instanceof HayRackBlockEntity rack) Containers.dropContents(level, pos, rack);
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof HayRackBlockEntity rack ? rack.bales() * 5 : 0;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HayRackBlockEntity(pos, state);
    }
}
