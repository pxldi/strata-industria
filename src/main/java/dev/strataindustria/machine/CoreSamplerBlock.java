package dev.strataindustria.machine;

import dev.strataindustria.power.KineticBlock;
import dev.strataindustria.power.Kinetics;
import dev.strataindustria.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The core sampler (tier 3 spec 8.5): a derrick that drills a core from the ground below. Shafts join on any side but the bottom. */
public class CoreSamplerBlock extends BaseEntityBlock implements KineticBlock {
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 4, 16),
            Block.box(1, 4, 1, 3, 16, 3), Block.box(1, 4, 13, 3, 16, 15), Block.box(13, 4, 1, 15, 16, 3), Block.box(13, 4, 13, 15, 16, 15),
            Block.box(1, 14, 1, 15, 16, 15));

    public CoreSamplerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public boolean connects(BlockState state, Direction side) {
        return side != Direction.DOWN;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (Kinetics.report(level, pos, player)) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof CoreSamplerBlockEntity sampler) {
            serverPlayer.openMenu(sampler, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CoreSamplerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.CORE_SAMPLER.get(), CoreSamplerBlockEntity::serverTick);
    }
}
