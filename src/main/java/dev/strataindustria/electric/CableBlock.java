package dev.strataindustria.electric;

import dev.strataindustria.power.ElectricNetworks;
import dev.strataindustria.power.ElectricNode;
import dev.strataindustria.power.ElectricTier;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * An insulated cable (spec 8.1): a thin pipe-like block that joins other cables and every electric
 * face it touches. Waterloggable. Cables of different tiers join each other; the network rules
 * decide what works (spec 6.2).
 */
public class CableBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final Map<Direction, BooleanProperty> PROPERTIES = PipeBlock.PROPERTY_BY_DIRECTION;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private final ElectricTier tier;
    private final VoxelShape core;
    private final Map<Direction, VoxelShape> arms = new EnumMap<>(Direction.class);

    public CableBlock(ElectricTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
        // Spec 8.1: 6 px LV, 8 px MV.
        double r = tier == ElectricTier.LV ? 3 : 4, lo = 8 - r, hi = 8 + r;
        core = Block.box(lo, lo, lo, hi, hi, hi);
        arms.put(Direction.DOWN, Block.box(lo, 0, lo, hi, lo, hi));
        arms.put(Direction.UP, Block.box(lo, hi, lo, hi, 16, hi));
        arms.put(Direction.NORTH, Block.box(lo, lo, 0, hi, hi, lo));
        arms.put(Direction.SOUTH, Block.box(lo, lo, hi, hi, hi, 16));
        arms.put(Direction.WEST, Block.box(0, lo, lo, lo, hi, hi));
        arms.put(Direction.EAST, Block.box(hi, lo, lo, 16, hi, hi));
        BlockState state = stateDefinition.any().setValue(WATERLOGGED, false);
        for (BooleanProperty property : PROPERTIES.values()) state = state.setValue(property, false);
        registerDefaultState(state);
    }

    public ElectricTier tier() {
        return tier;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        PROPERTIES.values().forEach(builder::add);
        builder.add(WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState()
                .setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
        for (Direction side : Direction.values()) {
            state = state.setValue(PROPERTIES.get(side), joins(context.getLevel(), context.getClickedPos().relative(side), side.getOpposite()));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (state.getValue(WATERLOGGED)) ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return state.setValue(PROPERTIES.get(direction), joins(level, neighbourPos, direction.getOpposite()));
    }

    /** Whether the block at {@code pos} takes a cable on its {@code side}: another cable or an electric face. */
    public static boolean joins(BlockGetter level, BlockPos pos, Direction side) {
        if (level.getBlockState(pos).getBlock() instanceof CableBlock) return true;
        return level.getBlockEntity(pos) instanceof ElectricNode node && node.connectsElectric(side);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = core;
        for (Direction side : Direction.values()) {
            if (state.getValue(PROPERTIES.get(side))) shape = Shapes.or(shape, arms.get(side));
        }
        return shape;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return ElectricNetworks.report(level, pos, player) ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CableBlockEntity(pos, state);
    }
}
