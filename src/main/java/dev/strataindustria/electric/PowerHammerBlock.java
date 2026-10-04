package dev.strataindustria.electric;

import dev.strataindustria.electric.machine.ElectricMachineBlock;
import dev.strataindustria.power.ElectricNetworks;
import dev.strataindustria.power.ElectricTier;
import dev.strataindustria.power.StatusLight;
import dev.strataindustria.registry.Tier5BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
 * The power hammer (tier 5 spec 10.8): a riveted steel frame over a built-in anvil with a motor housing
 * facing {@code FACING}. It shares the electric machines' facing, tier, status and active states; plain
 * use opens the screen and sneak-use shows the power line (6.5). {@code ACTIVE} while it heats or works.
 */
public class PowerHammerBlock extends BaseEntityBlock {
    public PowerHammerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ElectricMachineBlock.FACING, Direction.NORTH)
                .setValue(ElectricMachineBlock.TIER, ElectricTier.LV).setValue(ElectricMachineBlock.STATUS, StatusLight.OFF)
                .setValue(ElectricMachineBlock.ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ElectricMachineBlock.FACING, ElectricMachineBlock.TIER, ElectricMachineBlock.STATUS, ElectricMachineBlock.ACTIVE);
    }

    /** The motor housing faces the player who places it. */
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
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (ElectricNetworks.report(level, pos, player)) return InteractionResult.SUCCESS;
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PowerHammerBlockEntity hammer && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(hammer, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ElectricMachineBlock.ACTIVE)) return;
        // Sparks from the coil as it heats.
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.5, pos.getY() + 0.45,
                    pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.5, 0.0, 0.01, 0.0);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PowerHammerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, Tier5BlockEntities.POWER_HAMMER.get(), PowerHammerBlockEntity::serverTick);
    }
}
