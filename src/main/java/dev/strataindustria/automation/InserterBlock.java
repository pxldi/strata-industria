package dev.strataindustria.automation;

import dev.strataindustria.power.Kinetics;
import dev.strataindustria.power.KineticBlock;
import dev.strataindustria.registry.Tier4BlockEntities;
import dev.strataindustria.registry.Tier4Items;
import dev.strataindustria.registry.Tier4Sounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The inserter (tier 4 spec 13.4): an axle in its base turns an arm that takes one item from the block
 * behind it and puts it in the block in front. {@code FACING} is the way items travel. A filter can be
 * put in with a right-click and taken out again by sneaking with an empty hand.
 */
public class InserterBlock extends BaseEntityBlock implements KineticBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    /** Whether a filter sits in it, shown as a paper tag on the base. */
    public static final BooleanProperty FILTERED = BooleanProperty.create("filtered");
    private static final VoxelShape SHAPE = Shapes.or(Block.box(1, 0, 1, 15, 4, 15), Block.box(4, 4, 4, 12, 14, 12));

    public InserterBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FILTERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FILTERED);
    }

    /** Items travel away from the player: the arm picks up behind and sets down in front. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    public boolean connects(BlockState state, Direction side) {
        return side == Direction.DOWN;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    /** Right-click with a filter to fit it, swapping out the one already there. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Tier4Items.FILTER.get()) || !(level.getBlockEntity(pos) instanceof InserterBlockEntity inserter)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide()) {
            ItemStack old = inserter.setFilter(stack.copyWithCount(1));
            stack.consume(1, player);
            if (!old.isEmpty()) player.getInventory().placeItemBackInInventory(old, Prediction.SERVER_ONLY);
            level.playSound(null, pos, Tier4Sounds.FILTER_CONFIGURE.get(), SoundSource.BLOCKS, 0.6f, 1.0f);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof InserterBlockEntity inserter)) return InteractionResult.PASS;
        if (player.isShiftKeyDown() && inserter.hasFilter()) {
            if (!level.isClientSide()) {
                ItemStack old = inserter.setFilter(ItemStack.EMPTY);
                player.getInventory().placeItemBackInInventory(old, Prediction.SERVER_ONLY);
                level.playSound(null, pos, Tier4Sounds.FILTER_CONFIGURE.get(), SoundSource.BLOCKS, 0.6f, 0.85f);
            }
            return InteractionResult.SUCCESS;
        }
        Kinetics.report(level, pos, player);
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InserterBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, Tier4BlockEntities.INSERTER.get(), InserterBlockEntity::serverTick);
    }
}
