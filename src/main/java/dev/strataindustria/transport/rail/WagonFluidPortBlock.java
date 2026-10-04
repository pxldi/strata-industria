package dev.strataindustria.transport.rail;

import dev.strataindustria.registry.Tier4Sounds;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The wagon fluid port (outposts spec 7.2): a flanged steel box beside a station track. While a tank wagon stands on
 * the stop it moves fluid between the wagon and the pipes, 200 mB a tick. Use turns the flange between filling the
 * wagon (blue tab) and emptying it (white tab).
 */
public class WagonFluidPortBlock extends BaseEntityBlock {
    public enum Mode implements StringRepresentable {
        LOAD, UNLOAD;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final EnumProperty<Mode> MODE = EnumProperty.create("mode", Mode.class);

    public WagonFluidPortBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(MODE, Mode.LOAD));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MODE);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            Mode next = state.getValue(MODE) == Mode.LOAD ? Mode.UNLOAD : Mode.LOAD;
            level.setBlock(pos, state.setValue(MODE, next), Block.UPDATE_ALL);
            level.playSound(null, pos, next == Mode.UNLOAD ? Tier4Sounds.VALVE_OPEN.get() : Tier4Sounds.VALVE_CLOSE.get(),
                    SoundSource.BLOCKS, 0.8f, 0.9f + level.getRandom().nextFloat() * 0.2f);
            player.sendOverlayMessage(Component.translatable("strataindustria.wagon_port." + next.getSerializedName()));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WagonFluidPortBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!(level instanceof ServerLevel) || type != RailwayRegistry.WAGON_FLUID_PORT_ENTITY.get()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<WagonFluidPortBlockEntity>) (l, p, s, port) -> port.serverTick((ServerLevel) l, s);
    }
}
