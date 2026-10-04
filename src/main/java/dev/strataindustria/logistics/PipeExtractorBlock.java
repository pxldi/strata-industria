package dev.strataindustria.logistics;

import dev.strataindustria.power.ElectricNetworks;
import dev.strataindustria.registry.Tier4Items;
import dev.strataindustria.registry.Tier4Sounds;
import java.util.EnumMap;
import java.util.Map;
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
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The pipe extractor (tier 5 spec 12.2): a pipe segment with a funnel mouth on its front. {@code FACING} is the
 * way the mouth points; it pulls from the inventory there and feeds the pipe on its back. The fast one is the
 * same block with a quicker pull.
 */
public class PipeExtractorBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    /** Lit while it is moving items. */
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        for (Direction facing : Direction.values()) {
            VoxelShape shape = Shapes.empty();
            // The mouth funnel at the front and the pipe stub at the back.
            shape = Shapes.or(shape, boxAlong(facing, 12, 16, 4, 12));
            shape = Shapes.or(shape, boxAlong(facing, 4, 12, 5.5, 10.5));
            shape = Shapes.or(shape, boxAlong(facing, 0, 4, 5.5, 10.5));
            SHAPES.put(facing, shape);
        }
    }

    /** A box from {@code from} to {@code to} sixteenths along the way the mouth points (0 at the back), {@code lo} to {@code hi} across. */
    private static VoxelShape boxAlong(Direction facing, double from, double to, double lo, double hi) {
        double a = facing.getAxisDirection() == Direction.AxisDirection.POSITIVE ? from : 16 - to;
        double b = facing.getAxisDirection() == Direction.AxisDirection.POSITIVE ? to : 16 - from;
        return switch (facing.getAxis()) {
            case X -> Block.box(a, lo, lo, b, hi, hi);
            case Y -> Block.box(lo, a, lo, hi, b, hi);
            case Z -> Block.box(lo, lo, a, hi, hi, b);
        };
    }

    private final boolean fast;

    public PipeExtractorBlock(boolean fast, Properties properties) {
        super(properties);
        this.fast = fast;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ACTIVE, false));
    }

    public boolean fast() {
        return fast;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE);
    }

    /** The mouth points at the block that was clicked: that is the inventory it will empty. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
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
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        if (!stack.is(Tier4Items.FILTER.get()) || !(level.getBlockEntity(pos) instanceof PipeExtractorBlockEntity extractor)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide()) {
            ItemStack old = extractor.setFilter(stack.copyWithCount(1));
            stack.consume(1, player);
            if (!old.isEmpty()) player.getInventory().placeItemBackInInventory(old, Prediction.SERVER_ONLY);
            level.playSound(null, pos, Tier4Sounds.FILTER_CONFIGURE.get(), SoundSource.BLOCKS, 0.6f, 1.0f);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof PipeExtractorBlockEntity extractor)) return InteractionResult.PASS;
        if (player.isShiftKeyDown()) {
            if (extractor.hasFilter()) {
                if (!level.isClientSide()) {
                    ItemStack old = extractor.setFilter(ItemStack.EMPTY);
                    player.getInventory().placeItemBackInInventory(old, Prediction.SERVER_ONLY);
                    level.playSound(null, pos, Tier4Sounds.FILTER_CONFIGURE.get(), SoundSource.BLOCKS, 0.6f, 0.85f);
                }
                return InteractionResult.SUCCESS;
            }
            if (ElectricNetworks.report(level, pos, player)) return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide()) player.sendOverlayMessage(extractor.report());
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PipeExtractorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, Tier5Logistics.PIPE_EXTRACTOR_BE.get(), PipeExtractorBlockEntity::serverTick);
    }
}
