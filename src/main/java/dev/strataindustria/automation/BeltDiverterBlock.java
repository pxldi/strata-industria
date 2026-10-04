package dev.strataindustria.automation;

import dev.strataindustria.power.Kinetics;
import dev.strataindustria.registry.Tier4BlockEntities;
import dev.strataindustria.registry.Tier4Items;
import dev.strataindustria.registry.Tier4Sounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The belt diverter (tier 4 spec 13.2): a flat belt segment with a filter slot. Items the filter matches are
 * pushed off the side of the belt, to the right of travel or, when it was placed while sneaking, to the left;
 * the rest carry on. It joins a line of belts like any other segment, and is fitted by right-clicking with a
 * filter, taken out again by sneaking with an empty hand.
 */
public class BeltDiverterBlock extends ConveyorBlock {
    /** Pushes to the left of travel instead of the right. */
    public static final BooleanProperty LEFT = BooleanProperty.create("left");
    /** Whether a filter sits in it, shown as a paper tag on the rail. */
    public static final BooleanProperty FILTERED = BooleanProperty.create("filtered");

    public BeltDiverterBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(LEFT, false).setValue(FILTERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LEFT, FILTERED);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(LEFT, context.isSecondaryUseActive());
    }

    /** The side items are pushed to. */
    public static Direction pushSide(BlockState state) {
        Direction facing = state.getValue(FACING);
        return state.getValue(LEFT) ? facing.getCounterClockWise() : facing.getClockWise();
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Tier4Items.FILTER.get()) || !(level.getBlockEntity(pos) instanceof BeltDiverterBlockEntity diverter)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide()) {
            ItemStack old = diverter.setFilter(stack.copyWithCount(1));
            stack.consume(1, player);
            if (!old.isEmpty()) player.getInventory().placeItemBackInInventory(old, Prediction.SERVER_ONLY);
            level.playSound(null, pos, Tier4Sounds.FILTER_CONFIGURE.get(), SoundSource.BLOCKS, 0.6f, 1.0f);
        }
        return InteractionResult.SUCCESS;
    }

    /** A diverter stays flat, so an empty hand does not tilt it: sneaking takes the filter out, otherwise it reports its drive. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof BeltDiverterBlockEntity diverter)) return InteractionResult.PASS;
        if (player.isShiftKeyDown() && diverter.hasFilter()) {
            if (!level.isClientSide()) {
                ItemStack old = diverter.setFilter(ItemStack.EMPTY);
                player.getInventory().placeItemBackInInventory(old, Prediction.SERVER_ONLY);
                level.playSound(null, pos, Tier4Sounds.FILTER_CONFIGURE.get(), SoundSource.BLOCKS, 0.6f, 0.85f);
            }
            return InteractionResult.SUCCESS;
        }
        Kinetics.report(level, pos, player);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected BlockEntityType<? extends ConveyorBlockEntity> beltType() {
        return Tier4BlockEntities.BELT_DIVERTER.get();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BeltDiverterBlockEntity(pos, state);
    }
}
