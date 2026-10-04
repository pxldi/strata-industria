package dev.strataindustria.automation;

import dev.strataindustria.registry.Tier4BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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

/**
 * The chute (tier 4 spec 13.3): a wrought iron tube that lets items fall, one every 4 ticks, into the
 * block below or the next chute. It takes items from above, from hoppers, belts and inserters, from
 * players and from items dropped in, and never pulls from an inventory.
 */
public class ChuteBlock extends BaseEntityBlock {
    /** The tube and its lip, for the outline. */
    private static final VoxelShape OUTLINE = Shapes.or(Block.box(3, 0, 3, 13, 12, 13), Block.box(1, 12, 1, 15, 16, 15));
    /** Only the walls collide, so dropped items fall in. */
    private static final VoxelShape WALLS = Shapes.or(
            Block.box(1, 12, 1, 15, 16, 4), Block.box(1, 12, 12, 15, 16, 15), Block.box(1, 12, 4, 4, 16, 12), Block.box(12, 12, 4, 15, 16, 12),
            Block.box(3, 0, 3, 13, 12, 4), Block.box(3, 0, 12, 13, 12, 13), Block.box(3, 0, 4, 4, 12, 12), Block.box(12, 0, 4, 13, 12, 12));

    public ChuteBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return OUTLINE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return WALLS;
    }

    /** Right-click with anything to drop it in. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty() || !(level.getBlockEntity(pos) instanceof ChuteBlockEntity chute)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!chute.accepts(stack)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide()) chute.insert(stack);
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChuteBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, Tier4BlockEntities.CHUTE.get(), ChuteBlockEntity::serverTick);
    }
}
