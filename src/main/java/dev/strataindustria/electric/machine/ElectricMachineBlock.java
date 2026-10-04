package dev.strataindustria.electric.machine;

import dev.strataindustria.power.ElectricNetworks;
import dev.strataindustria.power.ElectricTier;
import dev.strataindustria.power.StatusLight;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
 * A tier 5 item machine (spec 10.1 and 23.2): it faces the player who placed it, shows its identity and
 * status lamp on the front and its tier on the casing, and takes cables on every face. Sneak-use shows
 * the power line (6.5); plain use opens the screen.
 */
public class ElectricMachineBlock<E extends ElectricMachineBlockEntity> extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<ElectricTier> TIER = ElectricTier.PROPERTY;
    public static final EnumProperty<StatusLight> STATUS = StatusLight.PROPERTY;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    private final Supplier<BlockEntityType<E>> type;
    private final BiFunction<BlockPos, BlockState, E> factory;

    public ElectricMachineBlock(Supplier<BlockEntityType<E>> type, BiFunction<BlockPos, BlockState, E> factory, Properties properties) {
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
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (ElectricNetworks.report(level, pos, player)) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof ElectricMachineBlockEntity machine) {
            serverPlayer.openMenu(machine, buf -> {
                buf.writeBlockPos(pos);
                buf.writeEnum(machine.tier());
            });
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return factory.apply(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide() ? null : createTickerHelper(blockEntityType, type.get(), ElectricMachineBlockEntity::serverTick);
    }
}
