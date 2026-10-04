package dev.strataindustria.steam;

import dev.strataindustria.material.Metal;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.Tier4BlockEntities;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * The steel boiler's controller (tier 4 spec 10.3): the front centre of the lowest shell layer, with the
 * big gauge, the sight glass and a status light, all drawn from its block state. Right-click for its gauges, with a water bucket to fill
 * it, or with a steel plate to patch it.
 */
public class SteelBoilerControllerBlock extends BoilerBlock {
    /** Spec 21: green running, amber warming or low water, red dry firing. */
    public enum Light implements StringRepresentable {
        OFF, GREEN, AMBER, RED;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        static Light of(BoilerBlockEntity.Status status) {
            return switch (status) {
                case RUNNING, VENTING -> GREEN;
                case HEATING, LOW_WATER, PIPE_TOO_HOT -> AMBER;
                case DRY_FIRING -> RED;
                default -> OFF;
            };
        }
    }

    public static final EnumProperty<Light> LIGHT = EnumProperty.create("light", Light.class);
    public static final BooleanProperty VENTING = BooleanProperty.create("venting");
    /** The sight glass: how high the water stands, in fifths. */
    public static final IntegerProperty GLASS = IntegerProperty.create("glass", 0, 5);

    public SteelBoilerControllerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIGHT, Light.OFF).setValue(VENTING, false).setValue(GLASS, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIGHT, VENTING, GLASS);
    }

    @Override
    protected boolean repairsWith(ItemStack stack) {
        return stack.is(ModItems.PLATES.get(Metal.STEEL).get());
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof SteelBoilerControllerBlockEntity boiler)) return;
        BoilerBlockEntity.Status status = boiler.status();
        // A wisp at the top seams while it makes steam; the safety valve and dry firing are drawn by the server.
        if ((status == BoilerBlockEntity.Status.RUNNING || status == BoilerBlockEntity.Status.LOW_WATER) && random.nextInt(3) == 0) {
            BlockPos top = boiler.valve();
            double x = top.getX() - 1 + random.nextDouble() * 3, z = top.getZ() - 1 + random.nextDouble() * 3;
            level.addParticle(ParticleTypes.WHITE_SMOKE, x, top.getY() + 1.02, z, 0, 0.03, 0);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SteelBoilerControllerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, Tier4BlockEntities.BOILER_CONTROLLER.get(), BoilerBlockEntity::serverTick);
    }
}
