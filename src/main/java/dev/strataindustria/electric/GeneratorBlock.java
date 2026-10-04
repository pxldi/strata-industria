package dev.strataindustria.electric;

import dev.strataindustria.power.ElectricNetworks;
import dev.strataindustria.power.ElectricTier;
import dev.strataindustria.power.StatusLight;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A fuel-driven generator (spec 7.2 and 7.3): faces the player who placed it, shows its status lamp on the
 * front and its tier on the casing. Sneak-use shows the power line (6.5); plain use shows the generator's
 * own readout, and a bucket goes to {@link Generator#useBucket}.
 */
public class GeneratorBlock<E extends BlockEntity & GeneratorBlock.Generator> extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<ElectricTier> TIER = ElectricTier.PROPERTY;
    public static final EnumProperty<StatusLight> STATUS = StatusLight.PROPERTY;
    /** Animated front and exhaust: the rotor turns, the flame burns. */
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    /** What the block entity of a generator gives its block. */
    public interface Generator {
        void tick(ServerLevel level, BlockPos pos, BlockState state);

        /** The right-click line. */
        Component readout();

        /** A bucket in hand: returns PASS when it is not this generator's business. */
        default InteractionResult useBucket(Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack) {
            return InteractionResult.PASS;
        }

        /** Client-side particles while the block is {@link #ACTIVE}. */
        default void animate(Level level, BlockPos pos, BlockState state, RandomSource random) {}
    }

    private final Supplier<BlockEntityType<E>> type;
    private final BiFunction<BlockPos, BlockState, E> factory;

    public GeneratorBlock(Supplier<BlockEntityType<E>> type, BiFunction<BlockPos, BlockState, E> factory, Properties properties) {
        super(properties);
        this.type = type;
        this.factory = factory;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TIER, ElectricTier.LV)
                .setValue(STATUS, StatusLight.OFF).setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, TIER, STATUS, ACTIVE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        if (player.isShiftKeyDown() || !(level.getBlockEntity(pos) instanceof Generator generator)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        InteractionResult result = generator.useBucket(level, pos, player, hand, stack);
        return result == InteractionResult.PASS ? InteractionResult.TRY_WITH_EMPTY_HAND : result;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (ElectricNetworks.report(level, pos, player)) return InteractionResult.SUCCESS;
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof Generator generator) player.sendOverlayMessage(generator.readout());
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(ACTIVE) && level.getBlockEntity(pos) instanceof Generator generator) generator.animate(level, pos, state, random);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return factory.apply(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide() ? null : createTickerHelper(blockEntityType, type.get(),
                (l, p, s, be) -> be.tick((ServerLevel) l, p, s));
    }
}
