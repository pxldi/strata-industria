package dev.strataindustria.transport.rail;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The coal stage (outposts spec 7.4): a timber bin with a chute. Nine slots of fuel, open to hoppers and chutes; a
 * locomotive standing at a stop within two blocks is topped up from it to a full fuel slot. The chute hatch is
 * open while it loads.
 */
public class CoalStageBlock extends BaseEntityBlock {
    public static final BooleanProperty LOADING = BooleanProperty.create("loading");
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 14, 16);

    public CoalStageBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LOADING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LOADING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof CoalStageBlockEntity stage) player.openMenu(stage);
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        if (level.getBlockEntity(pos) instanceof CoalStageBlockEntity stage) net.minecraft.world.Containers.dropContents(level, pos, stage);
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, net.minecraft.core.Direction direction) {
        return level.getBlockEntity(pos) instanceof CoalStageBlockEntity stage ? net.minecraft.world.inventory.AbstractContainerMenu.getRedstoneSignalFromContainer(stage) : 0;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CoalStageBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!(level instanceof ServerLevel) || type != RailwayRegistry.COAL_STAGE_ENTITY.get()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<CoalStageBlockEntity>) (l, p, s, stage) -> stage.serverTick((ServerLevel) l, p, s);
    }
}
