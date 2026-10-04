package dev.strataindustria.transport.outpost;

import dev.strataindustria.registry.TransportBlocks;
import dev.strataindustria.registry.TransportDataComponents;
import dev.strataindustria.registry.TransportSounds;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The outpost charter (outposts spec 3.1): a squared post with a framed board and a deed pinned on it. It
 * keeps no block entity; everything about it lives in the {@link RouteIndex}. Use opens the charter board;
 * sneak and use with an empty hand outlines the loaded area on the chunk borders.
 */
public class OutpostCharterBlock extends Block {
    public static final net.minecraft.world.level.block.state.properties.EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape SHAPE = Block.column(8.0, 0.0, 16.0);

    public OutpostCharterBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return Block.canSupportCenter(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
            BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (direction == Direction.DOWN && !canSurvive(state, level, pos)) return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!(level instanceof ServerLevel server) || !(placer instanceof ServerPlayer player)) return;
        Charter charter = RouteIndex.get(server).post(server, pos, player, CharterItem.name(stack));
        level.playSound(null, pos, TransportSounds.CHARTER_PLACE.get(), SoundSource.BLOCKS, 1f, 1f);
        OutpostPayloads.openBoard(player, charter);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;
        RouteIndex index = RouteIndex.get(server);
        Charter charter = index.charterAt(pos).orElse(null);
        if (charter == null) return InteractionResult.PASS;
        if (player.isShiftKeyDown()) {
            OutpostOutline.show(server, serverPlayer, charter);
        } else {
            OutpostPayloads.openBoard(serverPlayer, charter);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        RouteIndex.get(level).take(level, pos);
    }

    /** A taken-down charter drops with its name on it. */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        net.minecraft.world.phys.Vec3 origin = params.getOptionalParameter(LootContextParams.ORIGIN);
        ServerLevel level = params.getLevel();
        if (origin != null) {
            RouteIndex.get(level).charterEver(BlockPos.containing(origin)).ifPresent(charter -> {
                for (ItemStack stack : drops) {
                    if (stack.is(TransportBlocks.OUTPOST_CHARTER_ITEM.get())) stack.set(TransportDataComponents.CHARTER_NAME.get(), charter.name());
                }
            });
        }
        return drops;
    }
}
