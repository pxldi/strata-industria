package dev.strataindustria.smithing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * An anvil (spec 9.1). Stone anvils are raw igneous rock with a worked top; the bronze anvil is a
 * proper anvil. Both open the smithing screen; {@code tier} is the highest metal tier they can work.
 */
public class AnvilBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape STONE = Block.box(0, 0, 0, 16, 15, 16);
    private static final VoxelShape BRONZE_X = Shapes.or(Block.box(2, 0, 2, 14, 4, 14), Block.box(4, 4, 3, 12, 5, 13),
            Block.box(6, 5, 4, 10, 10, 12), Block.box(3, 10, 0, 13, 16, 16));
    private static final VoxelShape BRONZE_Z = Shapes.or(Block.box(2, 0, 2, 14, 4, 14), Block.box(3, 4, 4, 13, 5, 12),
            Block.box(4, 5, 6, 12, 10, 10), Block.box(0, 10, 3, 16, 16, 13));

    private final int tier;
    private final boolean stone;

    public AnvilBlock(int tier, boolean stone, Properties properties) {
        super(properties);
        this.tier = tier;
        this.stone = stone;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public int tier() {
        return tier;
    }

    public boolean isStone() {
        return stone;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getClockWise());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (stone) return STONE;
        return state.getValue(FACING).getAxis() == Direction.Axis.X ? BRONZE_X : BRONZE_Z;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof AnvilBlockEntity anvil) player.openMenu(anvil);
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AnvilBlockEntity(pos, state);
    }
}
