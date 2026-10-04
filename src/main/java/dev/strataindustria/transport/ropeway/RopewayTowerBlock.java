package dev.strataindustria.transport.ropeway;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ScheduledTickAccess;

/**
 * A ropeway tower head (outposts spec 8.1): sheave wheels on an arm, standing on any column of fence posts, logs or
 * steel posts, or on solid ground. Wooden heads carry spans of up to 24 blocks, steel heads up to 40.
 */
public class RopewayTowerBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 15, 13);
    private final boolean steel;

    public RopewayTowerBlock(Properties properties, boolean steel) {
        super(properties);
        this.steel = steel;
    }

    public boolean steel() {
        return steel;
    }

    /** Whether either hand of the player holds wire rope or a bucket: those act through the item. */
    public static boolean holdsLineGear(Player player) {
        return player.getMainHandItem().is(RopewayRegistry.WIRE_ROPE.get()) || player.getOffhandItem().is(RopewayRegistry.WIRE_ROPE.get())
                || player.getMainHandItem().is(RopewayRegistry.BUCKET.get()) || player.getOffhandItem().is(RopewayRegistry.BUCKET.get());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState base = level.getBlockState(below);
        return base.is(RopewayRegistry.TOWER_BASE) || base.isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
            BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        return direction == Direction.DOWN && !canSurvive(state, level, pos) ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
                : super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (holdsLineGear(player)) return InteractionResult.PASS;
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof RopewayTowerBlockEntity tower) {
            player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(dev.strataindustria.StrataIndustria.MOD_ID
                    + (tower.attached() ? ".ropeway.tower.carrying" : ".ropeway.tower.bare")));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RopewayTowerBlockEntity(pos, state);
    }
}
