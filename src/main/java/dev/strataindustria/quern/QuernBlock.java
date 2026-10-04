package dev.strataindustria.quern;

import dev.strataindustria.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Hand quern (spec 10.1). Right-click with something grindable to put it on the stone, then hold
 * right-click to turn the handle. Sneak with an empty hand to take the stack back.
 */
public class QuernBlock extends BaseEntityBlock {
    /** Where the handle stands: four quarter turns. */
    public static final IntegerProperty TURN = IntegerProperty.create("turn", 0, 3);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(1, 0, 1, 15, 5, 15), Block.box(2, 5, 2, 14, 9, 14));

    public QuernBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(TURN, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TURN);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof QuernBlockEntity quern)) return InteractionResult.PASS;
        if (QuernBlockEntity.recipeFor(level, stack).isPresent() && quern.insert(stack)) {
            level.playSound(null, pos, ModSounds.QUERN_LOAD.get(), SoundSource.BLOCKS, 0.6f, 1.0f);
            return InteractionResult.SUCCESS;
        }
        // Anything else turns the handle, so grinding works whatever is in hand.
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof QuernBlockEntity quern)) return InteractionResult.PASS;
        if (player.isSecondaryUseActive()) {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            ItemStack taken = quern.takeAll();
            if (taken.isEmpty()) return InteractionResult.PASS;
            if (!player.addItem(taken)) Block.popResource(level, pos.above(), taken);
            return InteractionResult.SUCCESS;
        }
        if (level.isClientSide()) return quern.input().isEmpty() ? InteractionResult.PASS : InteractionResult.SUCCESS;
        if (!quern.turn((ServerLevel) level, pos)) return InteractionResult.PASS;
        level.setBlock(pos, state.setValue(TURN, (state.getValue(TURN) + 1) % 4), Block.UPDATE_CLIENTS);
        if (state.getValue(TURN) % 2 == 0) {
            level.playSound(null, pos, ModSounds.QUERN_GRIND.get(), SoundSource.BLOCKS, 0.5f, 0.85f + level.getRandom().nextFloat() * 0.3f);
        }
        player.causeFoodExhaustion(0.01f);
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new QuernBlockEntity(pos, state);
    }
}
