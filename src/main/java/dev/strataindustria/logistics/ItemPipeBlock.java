package dev.strataindustria.logistics;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.registry.Tier4Items;
import dev.strataindustria.registry.Tier4Sounds;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The item pipe (tier 5 spec 12.1): a thin glass-and-brass tube that joins other pipes and any block that
 * holds items. Each of its six faces is in one of five states, kept in the block state so the model can
 * show them: nothing there, a pipe, an inventory the pipe feeds (a port), an inventory the storage controller
 * owns, or an inventory switched off. Right-click a face with a filter to fit it; right-click it with an
 * empty hand to step through the modes, sneaking to take the filter out.
 */
public class ItemPipeBlock extends BaseEntityBlock {
    /** What one face of a pipe is joined to. */
    public enum Face implements StringRepresentable {
        NONE, PIPE, PORT, STORAGE, OFF;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        /** Whether an inventory is behind this face. */
        public boolean inventory() {
            return this == PORT || this == STORAGE || this == OFF;
        }
    }

    public static final Map<Direction, EnumProperty<Face>> FACES = new EnumMap<>(Direction.class);
    private static final VoxelShape CORE = Block.box(5, 5, 5, 11, 11, 11);
    private static final Map<Direction, VoxelShape> ARMS = new EnumMap<>(Direction.class);

    static {
        for (Direction side : Direction.values()) FACES.put(side, EnumProperty.create(side.getName(), Face.class));
        ARMS.put(Direction.DOWN, Block.box(5.5, 0, 5.5, 10.5, 5, 10.5));
        ARMS.put(Direction.UP, Block.box(5.5, 11, 5.5, 10.5, 16, 10.5));
        ARMS.put(Direction.NORTH, Block.box(5.5, 5.5, 0, 10.5, 10.5, 5));
        ARMS.put(Direction.SOUTH, Block.box(5.5, 5.5, 11, 10.5, 10.5, 16));
        ARMS.put(Direction.WEST, Block.box(0, 5.5, 5.5, 5, 10.5, 10.5));
        ARMS.put(Direction.EAST, Block.box(11, 5.5, 5.5, 16, 10.5, 10.5));
    }

    public ItemPipeBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any();
        for (EnumProperty<Face> property : FACES.values()) state = state.setValue(property, Face.NONE);
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        FACES.values().forEach(builder::add);
    }

    public static Face face(BlockState state, Direction side) {
        return state.getValue(FACES.get(side));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Direction side : Direction.values()) {
            state = state.setValue(FACES.get(side), faceFor(context.getLevel(), context.getClickedPos(), side, Face.NONE));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
            BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        return state.setValue(FACES.get(direction), faceFor(level, pos, direction, face(state, direction)));
    }

    /** What the face of {@code pos} on {@code side} is now: a mode the player chose is kept while its inventory stays. */
    static Face faceFor(BlockGetter level, BlockPos pos, Direction side, Face current) {
        BlockPos next = pos.relative(side);
        BlockState neighbour = level.getBlockState(next);
        if (neighbour.getBlock() instanceof ItemPipeBlock) return Face.PIPE;
        if (neighbour.getBlock() instanceof PipeExtractorBlock && neighbour.getValue(PipeExtractorBlock.FACING) == side) return Face.PIPE;
        if (isInventory(level, next)) return current.inventory() ? current : Face.PORT;
        return Face.NONE;
    }

    /** Whether the block at {@code pos} is something that holds items a pipe may feed or empty. */
    public static boolean isInventory(BlockGetter level, BlockPos pos) {
        BlockEntity entity = level.getBlockEntity(pos);
        return entity instanceof Container && !(entity instanceof ItemPipeBlockEntity) && !(entity instanceof PipeExtractorBlockEntity);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE;
        for (Direction side : Direction.values()) {
            if (face(state, side) != Face.NONE) shape = Shapes.or(shape, ARMS.get(side));
        }
        return shape;
    }

    /** The face of the pipe a click landed on: the arm under the cursor, or else the side of the block that was hit. */
    static @Nullable Direction faceAt(BlockState state, BlockPos pos, BlockHitResult hit) {
        Vec3 local = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
        for (Direction side : Direction.values()) {
            if (face(state, side) == Face.NONE) continue;
            AABB arm = ARMS.get(side).bounds().inflate(0.02);
            if (arm.contains(local)) return side;
        }
        Direction hitSide = hit.getDirection();
        return face(state, hitSide) != Face.NONE ? hitSide : null;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        if (!stack.is(Tier4Items.FILTER.get())) return InteractionResult.TRY_WITH_EMPTY_HAND;
        Direction side = faceAt(state, pos, hit);
        if (side == null || !face(state, side).inventory() || !(level.getBlockEntity(pos) instanceof ItemPipeBlockEntity pipe)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide()) {
            ItemStack old = pipe.setFilter(side, stack.copyWithCount(1));
            stack.consume(1, player);
            if (!old.isEmpty()) player.getInventory().placeItemBackInInventory(old, Prediction.SERVER_ONLY);
            level.playSound(null, pos, Tier4Sounds.FILTER_CONFIGURE.get(), SoundSource.BLOCKS, 0.6f, 1.0f);
            player.sendOverlayMessage(describe(side, face(state, side), true));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        Direction side = faceAt(state, pos, hit);
        if (side == null || !face(state, side).inventory() || !(level.getBlockEntity(pos) instanceof ItemPipeBlockEntity pipe)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) {
            if (pipe.hasFilter(side)) {
                ItemStack old = pipe.setFilter(side, ItemStack.EMPTY);
                player.getInventory().placeItemBackInInventory(old, Prediction.SERVER_ONLY);
                level.playSound(null, pos, Tier4Sounds.FILTER_CONFIGURE.get(), SoundSource.BLOCKS, 0.6f, 0.85f);
            }
            player.sendOverlayMessage(describe(side, face(state, side), pipe.hasFilter(side)));
            return InteractionResult.SUCCESS;
        }
        Face next = switch (face(state, side)) {
            case PORT -> StorageRules.plain(level.getBlockEntity(pos.relative(side))) ? Face.STORAGE : Face.OFF;
            case STORAGE -> Face.OFF;
            default -> Face.PORT;
        };
        level.setBlock(pos, state.setValue(FACES.get(side), next), Block.UPDATE_ALL);
        level.playSound(null, pos, Tier4Sounds.FILTER_CONFIGURE.get(), SoundSource.BLOCKS, 0.5f, 1.3f);
        player.sendOverlayMessage(describe(side, next, pipe.hasFilter(side)));
        return InteractionResult.SUCCESS;
    }

    private static Component describe(Direction side, Face face, boolean filtered) {
        Component line = Component.translatable(StrataIndustria.MOD_ID + ".item_pipe.face." + face.getSerializedName(),
                Component.translatable("direction." + StrataIndustria.MOD_ID + "." + side.getName()));
        if (filtered) line = Component.empty().append(line).append(Component.translatable(StrataIndustria.MOD_ID + ".item_pipe.filtered"));
        return line;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ItemPipeBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, Tier5Logistics.ITEM_PIPE_BE.get(), ItemPipeBlockEntity::serverTick);
    }
}
