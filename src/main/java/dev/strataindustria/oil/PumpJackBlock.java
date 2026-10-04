package dev.strataindustria.oil;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.power.KineticBlock;
import dev.strataindustria.power.Kinetics;
import dev.strataindustria.registry.Tier6BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The pump jack (tier 6 spec 5.4): sits on a drilled wellhead and nods. Its horse head faces {@code FACING}; the
 * walking beam and its post stand behind and above it, so the three blocks behind and above must be clear. The
 * shaft comes in at the back, low, between the legs of the frame.
 */
public class PumpJackBlock extends BaseEntityBlock implements KineticBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    /** Whether it is lifting oil, which is when the beam moves. */
    public static final BooleanProperty RUNNING = BooleanProperty.create("running");
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 11, 16);

    public PumpJackBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(RUNNING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, RUNNING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** Spec 5.4: only on a drilled wellhead, and only with room for the beam. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (!onDrilledWellhead(level, pos)) {
            if (player != null && !level.isClientSide()) player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".pump_jack.status.no_wellhead"));
            return null;
        }
        Direction facing = context.getHorizontalDirection();
        if (!roomFor(level, pos, facing)) {
            if (player != null && !level.isClientSide()) player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".pump_jack.status.no_room"));
            return null;
        }
        return defaultBlockState().setValue(FACING, facing);
    }

    public static boolean onDrilledWellhead(LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.getBlock() instanceof WellheadBlock && below.getValue(WellheadBlock.DRILLED);
    }

    /** The beam, its post and its tail: three blocks back and three up from the horse head, all clear. */
    public static boolean roomFor(LevelReader level, BlockPos pos, Direction facing) {
        for (int back = 0; back <= 2; back++) {
            for (int up = 1; up <= 3; up++) {
                BlockPos spot = pos.relative(facing.getOpposite(), back).above(up);
                BlockState state = level.getBlockState(spot);
                if (!state.isAir() && !state.canBeReplaced()) return false;
            }
        }
        return true;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return onDrilledWellhead(level, pos);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
            BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (direction == Direction.DOWN && !canSurvive(state, level, pos)) return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    /** The shaft comes in at the back. */
    @Override
    public boolean connects(BlockState state, Direction side) {
        return side == state.getValue(FACING).getOpposite();
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
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (Kinetics.report(level, pos, player)) return InteractionResult.SUCCESS;
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PumpJackBlockEntity jack) player.sendOverlayMessage(jack.report());
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PumpJackBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? createTickerHelper(type, Tier6BlockEntities.PUMP_JACK.get(), PumpJackBlockEntity::clientTick)
                : createTickerHelper(type, Tier6BlockEntities.PUMP_JACK.get(), PumpJackBlockEntity::serverTick);
    }
}
