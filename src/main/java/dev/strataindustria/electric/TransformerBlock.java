package dev.strataindustria.electric;

import dev.strataindustria.power.ElectricNetworks;
import dev.strataindustria.registry.Tier5BlockEntities;
import dev.strataindustria.registry.Tier5Sounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
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
 * The transformer (spec 8.2): the front is the MV side and the rest is the LV side. A click toggles step down
 * and step up (so does the wrench of spec 13.1); sneak and click shows the diagnostics line of the side clicked.
 */
public class TransformerBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    /** False is step down, the default: MV in, LV out. */
    public static final BooleanProperty STEP_UP = BooleanProperty.create("step_up");
    /** Passing energy: drives the hum. */
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public TransformerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(STEP_UP, false).setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, STEP_UP, ACTIVE);
    }

    /** The front, the MV side, faces the player, so the cable they are standing at runs into the LV side behind. */
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
        if (!(level.getBlockEntity(pos) instanceof TransformerBlockEntity transformer)) return InteractionResult.PASS;
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) player.sendOverlayMessage(ElectricNetworks.line(level, pos, transformer.portAt(hit.getDirection())));
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide()) toggle(level, pos, state, transformer, player);
        return InteractionResult.SUCCESS;
    }

    /** Switches between step down and step up: an empty-hand click or the wrench. */
    public static void toggle(Level level, BlockPos pos, BlockState state, TransformerBlockEntity transformer, Player player) {
        BlockState next = state.cycle(STEP_UP);
        transformer.clearBuffer();
        level.setBlock(pos, next, Block.UPDATE_ALL);
        ElectricNetworks.markDirty(level, pos);
        level.playSound(null, pos, Tier5Sounds.TRANSFORMER_SWITCH.get(), SoundSource.BLOCKS, 0.8f, next.getValue(STEP_UP) ? 1.1f : 0.9f);
        if (level.getBlockEntity(pos) instanceof TransformerBlockEntity changed) player.sendOverlayMessage(changed.readout());
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ACTIVE) || random.nextInt(60) != 0) return;
        // Spec 23.6: a steady mains hum while energy passes.
        level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, Tier5Sounds.TRANSFORMER_HUM.get(), SoundSource.BLOCKS, 0.3f,
                0.98f + random.nextFloat() * 0.04f, false);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TransformerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, Tier5BlockEntities.TRANSFORMER.get(), TransformerBlockEntity::serverTick);
    }
}
