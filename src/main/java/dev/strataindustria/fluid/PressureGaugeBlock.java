package dev.strataindustria.fluid;

import dev.strataindustria.registry.Tier4BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The pressure gauge (tier 4 spec 9.3): an inline pipe segment, rated as a bronze pipe, with a dial
 * that reads steam pressure (0 to 10 bar) or, for other fluids, how full the flow is. Right-click reads
 * it out.
 */
public class PressureGaugeBlock extends FluidPipeBlock implements EntityBlock {
    /** Needle position, 0 to 4 across the dial. */
    public static final IntegerProperty READING = IntegerProperty.create("reading", 0, 4);
    private static final VoxelShape HOUSING = Block.box(4, 4, 4, 12, 12, 12);

    public PressureGaugeBlock(int maxTemperature, int throughput, Properties properties) {
        super(maxTemperature, throughput, properties);
        registerDefaultState(defaultBlockState().setValue(READING, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(READING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.or(super.getShape(state, level, pos, context), HOUSING);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PressureGaugeBlockEntity gauge) {
            player.sendOverlayMessage(gauge.readout());
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PressureGaugeBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != Tier4BlockEntities.PRESSURE_GAUGE.get()) return null;
        return (l, p, s, be) -> PressureGaugeBlockEntity.serverTick(l, p, s, (PressureGaugeBlockEntity) be);
    }
}
