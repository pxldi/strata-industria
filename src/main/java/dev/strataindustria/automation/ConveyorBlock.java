package dev.strataindustria.automation;

import dev.strataindustria.power.Kinetics;
import dev.strataindustria.power.KineticBlock;
import dev.strataindustria.registry.Tier4BlockEntities;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The conveyor belt (tier 4 spec 13.1). {@code FACING} is the way items travel. A belt is flat, or a
 * ramp that rises or falls one block over its length; belts in a line pass items on and share one
 * drive, so an axle into the side of any belt turns the whole line of up to {@link #MAX_LINE}.
 * Right-click a belt with an empty hand to tilt it: flat, rising, falling.
 */
public class ConveyorBlock extends BaseEntityBlock implements KineticBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<Slope> SLOPE = EnumProperty.create("slope", Slope.class);
    /** Longest line of belts one drive turns (spec 13.1). */
    public static final int MAX_LINE = 16;

    /**
     * A flat belt's top is 4/16 up. A rising ramp goes from there to a block higher over its length; a
     * falling ramp is the mirror, from a block above the flat level down to it.
     */
    public enum Slope implements StringRepresentable {
        FLAT("flat"), UP("up"), DOWN("down");

        private final String name;

        Slope(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        /** Whether the belt's end is the high one, so the next belt sits a block higher. */
        public boolean highEnd() {
            return this == UP;
        }

        public Slope next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private static final VoxelShape FLAT_SHAPE = Block.box(0, 0, 0, 16, 5, 16);

    public ConveyorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SLOPE, Slope.FLAT));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SLOPE);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection());
        return lineLength(context.getLevel(), context.getClickedPos(), state) > MAX_LINE ? null : state;
    }

    /** Only the sides take an axle; the line carries the drive from belt to belt. */
    @Override
    public boolean connects(BlockState state, Direction side) {
        return side.getAxis().isHorizontal() && side.getAxis() != state.getValue(FACING).getAxis();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(SLOPE) == Slope.FLAT ? FLAT_SHAPE : collision(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(SLOPE) == Slope.FLAT ? Block.box(0, 0, 0, 16, 4, 16) : collision(state);
    }

    /** The steps of a ramp, turned to face the way it runs; a falling ramp is a rising one seen from the other end. */
    private static VoxelShape collision(BlockState state) {
        Direction high = state.getValue(SLOPE) == Slope.UP ? state.getValue(FACING) : state.getValue(FACING).getOpposite();
        VoxelShape shape = Shapes.empty();
        for (int i = 0; i < 4; i++) {
            // Step i counts from the low end; its top is 8, 12, 16, 16 sixteenths.
            int height = Math.min(16, 8 + 4 * i);
            int a = 4 * i, b = 4 * i + 4;
            shape = Shapes.or(shape, switch (high) {
                case NORTH -> Block.box(0, 0, 16 - b, 16, height, 16 - a);
                case SOUTH -> Block.box(0, 0, a, 16, height, b);
                case EAST -> Block.box(a, 0, 0, b, height, 16);
                default -> Block.box(16 - b, 0, 0, 16 - a, height, 16);
            });
        }
        return shape;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    /** Where a belt at {@code pos} hands its items on: the next belt of its line, or null when the line ends here. */
    public static @Nullable BlockPos next(BlockGetter level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        int rise = state.getValue(SLOPE).highEnd() ? 1 : 0;
        for (int dy : new int[] {rise, rise - 1, rise + 1}) {
            if (dy < -1 || dy > 1) continue;
            BlockPos at = pos.relative(facing).above(dy);
            BlockState there = level.getBlockState(at);
            if (!(there.getBlock() instanceof ConveyorBlock) || there.getValue(FACING) != facing) continue;
            // The belts meet when this one's end height equals the next one's start height.
            int next = there.getValue(SLOPE) == Slope.DOWN ? 1 : 0;
            if (dy == rise - next) return at;
        }
        return null;
    }

    /** The belts that hand their items to the one at {@code pos}. */
    public static List<BlockPos> previous(BlockGetter level, BlockPos pos, BlockState state) {
        List<BlockPos> found = new ArrayList<>(1);
        Direction back = state.getValue(FACING).getOpposite();
        for (int dy = -1; dy <= 1; dy++) {
            BlockPos at = pos.relative(back).above(dy);
            BlockState there = level.getBlockState(at);
            if (there.getBlock() instanceof ConveyorBlock && there.getValue(FACING) == state.getValue(FACING)
                    && pos.equals(next(level, at, there))) {
                found.add(at);
            }
        }
        return found;
    }

    /** How many belts are in the line through {@code pos}, counting a belt of {@code state} there. */
    public static int lineLength(BlockGetter level, BlockPos pos, BlockState state) {
        int length = 1;
        BlockPos at = pos;
        BlockState here = state;
        for (int i = 0; i < 2 * MAX_LINE; i++) {
            List<BlockPos> before = previous(level, at, here);
            if (before.isEmpty()) break;
            at = before.getFirst();
            here = level.getBlockState(at);
            length++;
        }
        at = pos;
        here = state;
        for (int i = 0; i < 2 * MAX_LINE; i++) {
            BlockPos after = next(level, at, here);
            if (after == null) break;
            at = after;
            here = level.getBlockState(at);
            length++;
        }
        return length;
    }

    /** Empty-handed, a right-click tilts the belt to the next slope; sneaking shows the network line. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (Kinetics.report(level, pos, player)) return InteractionResult.SUCCESS;
        BlockState tilted = state.setValue(SLOPE, state.getValue(SLOPE).next());
        if (lineLength(level, pos, tilted) > MAX_LINE) {
            if (!level.isClientSide()) player.sendOverlayMessage(Component.translatable("strataindustria.conveyor.line_too_long", MAX_LINE));
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof ConveyorBlockEntity belt) belt.relink(() -> level.setBlock(pos, tilted, Block.UPDATE_ALL));
            player.sendOverlayMessage(Component.translatable("strataindustria.conveyor." + tilted.getValue(SLOPE).getSerializedName()));
        }
        return InteractionResult.SUCCESS;
    }

    /** Things standing in the belt's cell ride it, and dropped items are picked up. */
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier applier, boolean intersects) {
        if (level.getBlockEntity(pos) instanceof ConveyorBlockEntity belt) belt.carry(entity);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ConveyorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, Tier4BlockEntities.CONVEYOR_BELT.get(), ConveyorBlockEntity::tick);
    }
}
