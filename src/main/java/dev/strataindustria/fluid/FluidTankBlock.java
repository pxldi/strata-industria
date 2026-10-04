package dev.strataindustria.fluid;

import dev.strataindustria.registry.Tier4BlockEntities;
import dev.strataindustria.registry.Tier4Fluids;
import dev.strataindustria.registry.Tier4Items;
import dev.strataindustria.registry.Tier4Sounds;
import dev.strataindustria.registry.Tier5Fluids;
import dev.strataindustria.registry.Tier5Items;
import dev.strataindustria.registry.Tier6Fluids;
import dev.strataindustria.registry.Tier6Items;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The fluid tank (tier 4 spec 9.3). {@code UP} and {@code DOWN} say whether a tank sits above or below,
 * so a column draws as one tall tank. Buckets of carried fluids go in and out; sneak right-click reads it.
 */
public class FluidTankBlock extends BaseEntityBlock {
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    /** Fluids that travel in buckets, and their buckets. */
    private record Carried(Supplier<Item> bucket, Supplier<Fluid> fluid) {}

    private static final List<Carried> CARRIED = List.of(
            new Carried(() -> Items.WATER_BUCKET, () -> Fluids.WATER),
            new Carried(Tier4Items.CREOSOTE_BUCKET::get, Tier4Fluids.CREOSOTE::get),
            new Carried(Tier5Items.LATEX_BUCKET::get, Tier5Fluids.LATEX::get),
            new Carried(Tier6Items.CRUDE_OIL_BUCKET::get, () -> Tier6Fluids.CRUDE_OIL.source().get()),
            new Carried(Tier6Items.NAPHTHA_BUCKET::get, () -> Tier6Fluids.NAPHTHA.source().get()),
            new Carried(Tier6Items.DIESEL_BUCKET::get, () -> Tier6Fluids.DIESEL.source().get()),
            new Carried(Tier6Items.HEAVY_OIL_BUCKET::get, () -> Tier6Fluids.HEAVY_OIL.source().get()));

    public FluidTankBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(UP, false).setValue(DOWN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(UP, DOWN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        return defaultBlockState().setValue(UP, level.getBlockState(pos.above()).is(this)).setValue(DOWN, level.getBlockState(pos.below()).is(this));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
            BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (direction == Direction.UP) return state.setValue(UP, neighbourState.is(this));
        if (direction == Direction.DOWN) return state.setValue(DOWN, neighbourState.is(this));
        return state;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof FluidTankBlockEntity tank)) return InteractionResult.PASS;
        for (Carried carried : CARRIED) {
            if (!stack.is(carried.bucket().get())) continue;
            Fluid fluid = carried.fluid().get();
            if (tank.fill(fluid, FluidTankBlockEntity.BUCKET, 0, true) < FluidTankBlockEntity.BUCKET) return InteractionResult.TRY_WITH_EMPTY_HAND;
            if (!level.isClientSide()) {
                tank.fill(fluid, FluidTankBlockEntity.BUCKET, 0, false);
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
                level.playSound(null, pos, Tier4Sounds.FLUID_TANK_FILL.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
                level.gameEvent(player, GameEvent.FLUID_PLACE, pos);
            }
            return InteractionResult.SUCCESS;
        }
        if (!stack.is(Items.BUCKET)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        Fluid held = FluidTankBlockEntity.fluidOf(tank.group());
        for (Carried carried : CARRIED) {
            if (!held.isSame(carried.fluid().get())) continue;
            if (tank.drain(FluidTankBlockEntity.BUCKET, true) < FluidTankBlockEntity.BUCKET) return InteractionResult.TRY_WITH_EMPTY_HAND;
            if (!level.isClientSide()) {
                tank.drain(FluidTankBlockEntity.BUCKET, false);
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(carried.bucket().get())));
                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0f, 1.0f);
                level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof FluidTankBlockEntity tank) player.sendOverlayMessage(tank.readout());
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof FluidTankBlockEntity tank ? tank.signal() : 0;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidTankBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, Tier4BlockEntities.FLUID_TANK.get(), FluidTankBlockEntity::serverTick);
    }
}
