package dev.strataindustria.block;

import dev.strataindustria.geology.Rock;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.ModTags;
import dev.strataindustria.knapping.Boulders;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A weathered boulder of one rock (redesign round 3, R1). It is the first thing the world gives you: hit it
 * with a bare hand or a rock shard and every blow opens a crack ({@link #CRACKS}), the third one splits it.
 * A big boulder splits into a smaller one first, the smallest bursts into shards. Limestone boulders can show
 * flint nodules ({@link #FLINTY}) and give the flint when they split. The blows are in {@link Boulders}.
 */
public class BoulderBlock extends Block {
    /** 1 is a knee-high rock, 3 a boulder you walk round. */
    public static final IntegerProperty SIZE = IntegerProperty.create("size", 1, 3);
    /** Cracks opened so far; the blow after the last one splits the boulder. */
    public static final IntegerProperty CRACKS = IntegerProperty.create("cracks", 0, Boulders.MAX_CRACKS);
    /** Flint nodules in the rock. */
    public static final BooleanProperty FLINTY = BooleanProperty.create("flinty");
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape[] SHAPES = {Block.box(3, 0, 3, 13, 5, 13), Block.box(1, 0, 1, 15, 9, 15), Block.box(0, 0, 0, 16, 13, 16)};

    private final Rock rock;

    public BoulderBlock(Rock rock, Properties properties) {
        super(properties);
        this.rock = rock;
        registerDefaultState(stateDefinition.any().setValue(SIZE, 2).setValue(CRACKS, 0).setValue(FLINTY, false).setValue(FACING, Direction.NORTH));
    }

    public Rock rock() {
        return rock;
    }

    /** A boulder as worldgen and structures place it. */
    public BlockState with(int size, boolean flinty, Direction facing) {
        return defaultBlockState().setValue(SIZE, size).setValue(FLINTY, flinty).setValue(FACING, facing);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SIZE, CRACKS, FLINTY, FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(SIZE) - 1];
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (direction == Direction.DOWN && !canSurvive(state, level, pos)) return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    /** A bare hand or a rock shard strikes; anything else goes on to its own use (placing a block against it). */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (!stack.isEmpty() && !stack.is(ModTags.Items.ROCK_SHARDS)) return InteractionResult.PASS;
        if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer) {
            Boulders.strike(server, serverPlayer, pos, state, hit.getLocation(), server.getGameTime());
        }
        return InteractionResult.SUCCESS;
    }

    /** The shard this boulder breaks into. */
    public net.minecraft.world.item.Item shard() {
        return ModItems.ROCK_SHARD.get(rock).get();
    }
}
