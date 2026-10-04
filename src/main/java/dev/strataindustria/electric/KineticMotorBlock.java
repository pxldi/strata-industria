package dev.strataindustria.electric;

import dev.strataindustria.power.ElectricNetworks;
import dev.strataindustria.power.ElectricTier;
import dev.strataindustria.power.KineticBlock;
import dev.strataindustria.power.StatusLight;
import dev.strataindustria.registry.Tier5BlockEntities;
import dev.strataindustria.registry.Tier5Sounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The kinetic motor (spec 10.11): the shaft comes out of the front face, which looks at the axle the player
 * placed it against; the fan grille is on the back. Sneak-use shows the shaft and the power line.
 */
public class KineticMotorBlock extends BaseEntityBlock implements KineticBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final EnumProperty<ElectricTier> TIER = ElectricTier.PROPERTY;
    public static final EnumProperty<StatusLight> STATUS = StatusLight.PROPERTY;

    public KineticMotorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TIER, ElectricTier.LV).setValue(STATUS, StatusLight.OFF));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, TIER, STATUS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    /** Rotation leaves through the front face only. */
    @Override
    public boolean connects(BlockState state, Direction side) {
        return side == state.getValue(FACING);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof KineticMotorBlockEntity motor)) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            Component line = motor.kinetic().report();
            if (motor.idleReason() != null) line = motor.idleReason().copy().append(Component.literal(" · ")).append(line);
            player.sendOverlayMessage(line.copy().append(Component.literal(" · ")).append(ElectricNetworks.line(level, pos)));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        StatusLight light = state.getValue(STATUS);
        if (light != StatusLight.RUN && light != StatusLight.WAIT) return;
        // Spec 23.6: a smooth whine whose pitch follows the speed.
        if (random.nextInt(50) == 0) {
            float pitch = light == StatusLight.RUN ? 1.0f : 0.8f;
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, Tier5Sounds.KINETIC_MOTOR_RUN.get(), SoundSource.BLOCKS,
                    0.35f, pitch * (state.getValue(TIER) == ElectricTier.MV ? 1.15f : 1.0f), false);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new KineticMotorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, Tier5BlockEntities.KINETIC_MOTOR.get(), KineticMotorBlockEntity::serverTick);
    }
}
