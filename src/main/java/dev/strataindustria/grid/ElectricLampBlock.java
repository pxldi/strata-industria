package dev.strataindustria.grid;

import dev.strataindustria.power.ElectricNetworks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The electric lamp (uniqueness 7.1): a bulb on the grid that burns as bright as the power it gets. Its light level is
 * the blockstate, so a network that falls short or a big machine starting up dims every lamp on it for a moment.
 */
public class ElectricLampBlock extends BaseEntityBlock {
    public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL;

    public ElectricLampBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LEVEL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return ElectricNetworks.report(level, pos, player) ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ElectricLampBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, GridBlocks.ELECTRIC_LAMP_ENTITY.get(), ElectricLampBlockEntity::serverTick);
    }
}
