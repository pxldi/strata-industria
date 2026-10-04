package dev.strataindustria.transport.foot;

import dev.strataindustria.geology.Rock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A cairn (spec 4.4): rocks stacked up to three high, taking the colour of the rock they were built from. It marks
 * a spot and a trail; bare-handed, it says where the next and the previous mark of its trail are.
 */
public class CairnBlock extends Block {
    public static final EnumProperty<Rock> ROCK = EnumProperty.create("rock", Rock.class);
    public static final IntegerProperty HEIGHT = IntegerProperty.create("height", 1, 3);
    /** Loose rocks of one type needed for each course. */
    public static final int ROCKS_PER_COURSE = 4;

    private static final VoxelShape[] SHAPES = {
            Block.box(3, 0, 3, 13, 5, 13),
            Block.box(3, 0, 3, 13, 9, 13),
            Block.box(3, 0, 3, 13, 13, 13)};

    public CairnBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ROCK, Rock.GRANITE).setValue(HEIGHT, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ROCK, HEIGHT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(HEIGHT) - 1];
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
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendOverlayMessage(TrailMessages.describe(server, serverPlayer, pos));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        TrailMarks.get(level.getServer()).remove(level, pos);
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }
}
