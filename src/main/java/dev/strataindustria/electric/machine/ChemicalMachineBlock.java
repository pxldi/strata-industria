package dev.strataindustria.electric.machine;

import dev.strataindustria.power.ElectricNetworks;
import dev.strataindustria.power.StatusLight;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The mixer and electrolyser (spec 10.1): the same casing, facing, tier and status states as the item
 * machines, so they share their models. A bucket pours into a tank; plain use opens the screen, and
 * sneak-use shows the power line (6.5).
 */
public class ChemicalMachineBlock<E extends ChemicalMachineBlockEntity> extends BaseEntityBlock {
    private final Supplier<BlockEntityType<E>> type;
    private final BiFunction<BlockPos, BlockState, E> factory;

    public ChemicalMachineBlock(Supplier<BlockEntityType<E>> type, BiFunction<BlockPos, BlockState, E> factory, Properties properties) {
        super(properties);
        this.type = type;
        this.factory = factory;
        registerDefaultState(stateDefinition.any().setValue(ElectricMachineBlock.FACING, net.minecraft.core.Direction.NORTH)
                .setValue(ElectricMachineBlock.TIER, dev.strataindustria.power.ElectricTier.LV)
                .setValue(ElectricMachineBlock.STATUS, StatusLight.OFF).setValue(ElectricMachineBlock.ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ElectricMachineBlock.FACING, ElectricMachineBlock.TIER, ElectricMachineBlock.STATUS, ElectricMachineBlock.ACTIVE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(ElectricMachineBlock.FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(ElectricMachineBlock.FACING, rotation.rotate(state.getValue(ElectricMachineBlock.FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(ElectricMachineBlock.FACING)));
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        if (player.isShiftKeyDown() || !(level.getBlockEntity(pos) instanceof ChemicalMachineBlockEntity machine)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        InteractionResult result = machine.useBucket(level, pos, player, hand, stack);
        return result == InteractionResult.PASS ? InteractionResult.TRY_WITH_EMPTY_HAND : result;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (ElectricNetworks.report(level, pos, player)) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof ChemicalMachineBlockEntity machine) {
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
        return level.isClientSide() ? null : createTickerHelper(blockEntityType, type.get(), ChemicalMachineBlockEntity::serverTick);
    }
}
