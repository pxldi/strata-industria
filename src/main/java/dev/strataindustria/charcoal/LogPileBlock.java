package dev.strataindustria.charcoal;

import dev.strataindustria.fire.FirestarterItem;
import dev.strataindustria.fire.Ignitable;
import dev.strataindustria.registry.ModBlockEntities;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Logs stacked for a charcoal pit (spec 4.4). Sneak-click with logs to place or fill one; light it and
 * bury every face under soil, sand, gravel or stone before the fire takes hold, and the logs char.
 */
public class LogPileBlock extends BaseEntityBlock implements Ignitable {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public LogPileBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    /** Places a new pile holding one log from {@code held}. */
    public static boolean placeNew(Level level, BlockPos pos, ItemStack held) {
        if (!level.getBlockState(pos).canBeReplaced()) return false;
        if (level.isClientSide()) return true;
        level.setBlock(pos, ModBlocks.LOG_PILE.get().defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof LogPileBlockEntity pile) pile.add(held);
        level.playSound(null, pos, ModSounds.KILN_LOG.get(), SoundSource.BLOCKS, 0.9f, 0.9f);
        return true;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(LIT)) return InteractionResult.PASS;
        if (stack.getItem() instanceof FirestarterItem) return InteractionResult.PASS;
        if (stack.is(Items.TORCH) || stack.is(Items.FLINT_AND_STEEL) || stack.is(Items.FIRE_CHARGE)) {
            if (!level.isClientSide() && ignite(level, pos, state)) {
                if (stack.is(Items.FLINT_AND_STEEL)) stack.hurtAndBreak(1, player, hand);
                else if (stack.is(Items.FIRE_CHARGE)) stack.consume(1, player);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    public boolean canIgnite(Level level, BlockPos pos, BlockState state) {
        return !state.getValue(LIT);
    }

    @Override
    public boolean ignite(Level level, BlockPos pos, BlockState state) {
        if (!canIgnite(level, pos, state)) return false;
        LogPileBlockEntity.lightConnected(level, pos);
        level.playSound(null, pos, ModSounds.FIRE_PIT_IGNITE.get(), SoundSource.BLOCKS, 1.0f, 0.9f);
        return true;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && !player.isCreative() && !state.getValue(LIT)
                && level.getBlockEntity(pos) instanceof LogPileBlockEntity pile) {
            pile.dropLogs(level, pos);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** Smoke seeps up through the cover while the pit burns. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT) || random.nextInt(3) != 0) return;
        BlockPos top = pos.above();
        for (int i = 0; i < 4 && !level.getBlockState(top).isAir(); i++) top = top.above();
        if (!level.getBlockState(top).isAir()) return;
        double x = top.getX() + 0.2 + random.nextDouble() * 0.6, z = top.getZ() + 0.2 + random.nextDouble() * 0.6;
        level.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true, x, top.getY() + 0.05, z, 0, 0.04, 0);
        if (top.equals(pos.above()) && random.nextInt(4) == 0) {
            // Nothing on top at all: the fire shows.
            level.addParticle(ParticleTypes.FLAME, x, top.getY(), z, 0, 0.02, 0);
            level.playLocalSound(x, top.getY(), z, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.6f, 1.0f, false);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LogPileBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.LOG_PILE.get(), LogPileBlockEntity::serverTick);
    }
}
