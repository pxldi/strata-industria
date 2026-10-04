package dev.strataindustria.fluid;

import dev.strataindustria.registry.Tier4Sounds;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A fluid pipe (tier 4 spec 9.2): joins other pipes and fluid ports on any face. It never holds fluid
 * itself; it only carries what a source pushes, up to its temperature rating and throughput.
 */
public class FluidPipeBlock extends Block {
    public static final Map<Direction, BooleanProperty> PROPERTIES = PipeBlock.PROPERTY_BY_DIRECTION;
    private static final VoxelShape CORE = Block.box(5, 5, 5, 11, 11, 11);
    private static final Map<Direction, VoxelShape> ARMS = new EnumMap<>(Direction.class);

    static {
        ARMS.put(Direction.DOWN, Block.box(5.5, 0, 5.5, 10.5, 5, 10.5));
        ARMS.put(Direction.UP, Block.box(5.5, 11, 5.5, 10.5, 16, 10.5));
        ARMS.put(Direction.NORTH, Block.box(5.5, 5.5, 0, 10.5, 10.5, 5));
        ARMS.put(Direction.SOUTH, Block.box(5.5, 5.5, 11, 10.5, 10.5, 16));
        ARMS.put(Direction.WEST, Block.box(0, 5.5, 5.5, 5, 10.5, 10.5));
        ARMS.put(Direction.EAST, Block.box(11, 5.5, 5.5, 16, 10.5, 10.5));
    }

    private final int maxTemperature;
    private final int throughput;

    /**
     * @param maxTemperature hottest fluid it carries, in °C
     * @param throughput     mB per tick it passes
     */
    public FluidPipeBlock(int maxTemperature, int throughput, Properties properties) {
        super(properties);
        this.maxTemperature = maxTemperature;
        this.throughput = throughput;
        BlockState state = stateDefinition.any();
        for (BooleanProperty property : PROPERTIES.values()) state = state.setValue(property, false);
        registerDefaultState(state);
    }

    public int maxTemperature() {
        return maxTemperature;
    }

    public int throughput() {
        return throughput;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        PROPERTIES.values().forEach(builder::add);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Direction side : Direction.values()) {
            BlockPos next = context.getClickedPos().relative(side);
            state = state.setValue(PROPERTIES.get(side), joins(context.getLevel(), next, side.getOpposite()));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        return state.setValue(PROPERTIES.get(direction), joins(level, neighbourPos, direction.getOpposite()));
    }

    /** Whether the block at {@code pos} takes a pipe on its {@code side}: another pipe or a fluid port. */
    public static boolean joins(BlockGetter level, BlockPos pos, Direction side) {
        if (level.getBlockState(pos).getBlock() instanceof FluidPipeBlock) return true;
        return level.getBlockEntity(pos) instanceof FluidPort port && port.connectsFluid(side);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE;
        for (Direction side : Direction.values()) {
            if (state.getValue(PROPERTIES.get(side))) shape = Shapes.or(shape, ARMS.get(side));
        }
        return shape;
    }

    /** Pipes that refuse hot fluid clank, at most once a second per source (see {@link FluidPipes}). */
    public static void refuseSound(net.minecraft.world.level.Level level, BlockPos pos) {
        level.playSound(null, pos, Tier4Sounds.FLUID_PIPE_REFUSE.get(), net.minecraft.sounds.SoundSource.BLOCKS, 0.4f, 1.0f);
    }
}
