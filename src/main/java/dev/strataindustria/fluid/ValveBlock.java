package dev.strataindustria.fluid;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.registry.Tier4Sounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The valve (tier 4 spec 9.3): an inline pipe segment, rated as a bronze pipe. Right-click opens or
 * shuts it; a redstone signal holds it shut. A shut valve is a wall to the pipe network, and a tank
 * with an open valve below it drains into the pipes beyond.
 */
public class ValveBlock extends FluidPipeBlock {
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final VoxelShape BODY = Block.box(4, 4, 4, 12, 13, 12);

    public ValveBlock(int maxTemperature, int throughput, Properties properties) {
        super(maxTemperature, throughput, properties);
        registerDefaultState(defaultBlockState().setValue(OPEN, true).setValue(POWERED, false));
    }

    /** Whether fluid gets through: the wheel is open and no redstone holds it shut. */
    public static boolean passes(BlockState state) {
        return !(state.getBlock() instanceof ValveBlock) || state.getValue(OPEN) && !state.getValue(POWERED);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(OPEN, POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return super.getStateForPlacement(context).setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.or(super.getShape(state, level, pos, context), BODY);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        boolean open = !state.getValue(OPEN);
        level.setBlock(pos, state.setValue(OPEN, open), Block.UPDATE_ALL);
        level.playSound(null, pos, open ? Tier4Sounds.VALVE_OPEN.get() : Tier4Sounds.VALVE_CLOSE.get(), SoundSource.BLOCKS, 0.8f,
                0.95f + level.getRandom().nextFloat() * 0.1f);
        if (open && state.getValue(POWERED)) {
            player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".valve.held_shut"));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbour, @Nullable Orientation orientation,
            boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighbour, orientation, movedByPiston);
        if (level.isClientSide()) return;
        boolean powered = level.hasNeighborSignal(pos);
        if (powered == state.getValue(POWERED)) return;
        level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_ALL);
        if (state.getValue(OPEN)) {
            level.playSound(null, pos, powered ? Tier4Sounds.VALVE_CLOSE.get() : Tier4Sounds.VALVE_OPEN.get(), SoundSource.BLOCKS, 0.8f, 1.0f);
        }
    }
}
