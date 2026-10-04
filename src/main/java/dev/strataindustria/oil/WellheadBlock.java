package dev.strataindustria.oil;

import dev.strataindustria.power.KineticBlock;
import dev.strataindustria.power.Kinetics;
import dev.strataindustria.registry.Tier6BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The wellhead (tier 6 spec 5.3): a flanged steel head with a valve wheel, set on solid ground over a reservoir.
 * Rotation on any side drills; the top takes a pump jack once the well has struck oil.
 */
public class WellheadBlock extends BaseEntityBlock implements KineticBlock {
    /** Whether the bore has reached the reservoir. */
    public static final BooleanProperty DRILLED = BooleanProperty.create("drilled");
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 3, 16), Block.box(4, 3, 4, 12, 10, 12), Block.box(2, 10, 2, 14, 16, 14),
            Block.box(12, 3, 6, 16, 8, 10));

    public WellheadBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(DRILLED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DRILLED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** Rotation comes in on the four sides; the pump jack and the bore have the top and the bottom. */
    @Override
    public boolean connects(BlockState state, Direction side) {
        return side.getAxis().isHorizontal();
    }

    /** Spec 5.3: set on any solid block. */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
            BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (direction == Direction.DOWN && !canSurvive(state, level, pos)) return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (Kinetics.report(level, pos, player)) return InteractionResult.SUCCESS;
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof WellheadBlockEntity well && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(well, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WellheadBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, Tier6BlockEntities.WELLHEAD.get(), WellheadBlockEntity::serverTick);
    }
}
