package dev.strataindustria.grid;

import dev.strataindustria.power.ElectricNetworks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A Leyden jar (uniqueness 7.3): a glass jar lined with metal that holds what a lightning mast brings down. */
public class LeydenJarBlock extends BaseEntityBlock {
    public static final int SEGMENTS = 4;
    public static final IntegerProperty CHARGE = IntegerProperty.create("charge", 0, SEGMENTS);
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 14, 13);

    public LeydenJarBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CHARGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CHARGE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof LeydenJarBlockEntity jar)) return InteractionResult.PASS;
        if (!level.isClientSide()) player.sendOverlayMessage(player.isShiftKeyDown() ? ElectricNetworks.line(level, pos) : jar.chargeLine());
        return InteractionResult.SUCCESS;
    }

    /** A well charged jar throws the odd spark off its terminal. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(CHARGE) < 3 || random.nextInt(30) != 0) return;
        level.addParticle(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 0.0, 0.0, 0.0);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LeydenJarBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, GridBlocks.LEYDEN_JAR_ENTITY.get(), LeydenJarBlockEntity::serverTick);
    }
}
