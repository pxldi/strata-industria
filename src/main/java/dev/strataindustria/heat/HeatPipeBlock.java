package dev.strataindustria.heat;

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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A heat pipe or duct (tier 4 spec 8.2): joins other heat pipes, fireboxes, heat inlets and consumers on
 * any face. It holds no heat itself; a firebox finds the consumers along it, and each block of it costs
 * some temperature and a share of the heat. It glows while it carries heat above 580 °C.
 */
public class HeatPipeBlock extends Block {
    public static final Map<Direction, BooleanProperty> PROPERTIES = PipeBlock.PROPERTY_BY_DIRECTION;
    public static final BooleanProperty HOT = BlockStateProperties.LIT;
    /** Pipes glow from here (spec 21.2). */
    public static final float GLOWS_FROM = 580.0f;
    private static final VoxelShape CORE = Block.box(4, 4, 4, 12, 12, 12);
    private static final Map<Direction, VoxelShape> ARMS = new EnumMap<>(Direction.class);

    static {
        ARMS.put(Direction.DOWN, Block.box(4.5, 0, 4.5, 11.5, 4, 11.5));
        ARMS.put(Direction.UP, Block.box(4.5, 12, 4.5, 11.5, 16, 11.5));
        ARMS.put(Direction.NORTH, Block.box(4.5, 4.5, 0, 11.5, 11.5, 4));
        ARMS.put(Direction.SOUTH, Block.box(4.5, 4.5, 12, 11.5, 11.5, 16));
        ARMS.put(Direction.WEST, Block.box(0, 4.5, 4.5, 4, 11.5, 11.5));
        ARMS.put(Direction.EAST, Block.box(12, 4.5, 4.5, 16, 11.5, 11.5));
    }

    private final int maxTemperature;
    private final float drop;
    private final float loss;

    /**
     * @param maxTemperature the hottest it carries, in °C; a hotter source is held down to this
     * @param drop           °C lost per block
     * @param loss           share of the heat lost per block
     */
    public HeatPipeBlock(int maxTemperature, float drop, float loss, Properties properties) {
        super(properties);
        this.maxTemperature = maxTemperature;
        this.drop = drop;
        this.loss = loss;
        BlockState state = stateDefinition.any().setValue(HOT, false);
        for (BooleanProperty property : PROPERTIES.values()) state = state.setValue(property, false);
        registerDefaultState(state);
    }

    public int maxTemperature() {
        return maxTemperature;
    }

    public float drop() {
        return drop;
    }

    public float loss() {
        return loss;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        PROPERTIES.values().forEach(builder::add);
        builder.add(HOT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Direction side : Direction.values()) {
            BlockPos next = context.getClickedPos().relative(side);
            state = state.setValue(PROPERTIES.get(side), joins(context.getLevel(), next, side.getOpposite()));
        }
        HeatNetwork.changed();
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        boolean joined = joins(level, neighbourPos, direction.getOpposite());
        if (joined == state.getValue(PROPERTIES.get(direction))) return state;
        HeatNetwork.changed();
        return state.setValue(PROPERTIES.get(direction), joined);
    }

    /** Whether the block at {@code pos} takes a heat pipe on its {@code side}: another pipe or a heat port. */
    public static boolean joins(BlockGetter level, BlockPos pos, Direction side) {
        if (level.getBlockState(pos).getBlock() instanceof HeatPipeBlock) return true;
        return level.getBlockEntity(pos) instanceof HeatPort port && port.connectsHeat(side);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE;
        for (Direction side : Direction.values()) {
            if (state.getValue(PROPERTIES.get(side))) shape = Shapes.or(shape, ARMS.get(side));
        }
        return shape;
    }
}
