package dev.strataindustria.fire;

import dev.strataindustria.journal.Journal;
import dev.strataindustria.registry.ModBlockEntities;
import dev.strataindustria.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The first fire (tier 0-2 spec 3.5): a ring of stones around a bed of sticks. Placed unlit; lit with
 * flint struck on a rock, a torch or flint and steel once it has fuel. A lit pit turns a stick into a torch.
 */
public class FirePitBlock extends BaseEntityBlock implements Ignitable {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 4, 16);

    public FirePitBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (direction == Direction.DOWN && !canSurvive(state, level, pos)) return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        boolean lit = state.getValue(LIT);
        // A stick held into the flames comes out as a torch.
        if (lit && stack.is(Items.STICK) && !player.isSecondaryUseActive()) {
            if (!level.isClientSide()) {
                stack.consume(1, player);
                ItemStack torch = new ItemStack(Items.TORCH);
                if (!player.addItem(torch)) Block.popResource(level, pos.above(), torch);
                level.playSound(null, pos, ModSounds.FIRE_PIT_TORCH.get(), SoundSource.BLOCKS, 0.8f, 0.9f + level.getRandom().nextFloat() * 0.2f);
            }
            return InteractionResult.SUCCESS;
        }
        if (!lit && (stack.is(Items.TORCH) || stack.is(Items.FLINT_AND_STEEL) || stack.is(Items.FIRE_CHARGE))) {
            if (!canIgnite(level, pos, state)) return InteractionResult.FAIL;
            if (!level.isClientSide() && ignite(level, pos, state)) {
                if (stack.is(Items.FLINT_AND_STEEL)) stack.hurtAndBreak(1, player, hand);
                else if (stack.is(Items.FIRE_CHARGE)) stack.consume(1, player);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof FirePitBlockEntity pit) {
            player.openMenu(pit);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean canIgnite(Level level, BlockPos pos, BlockState state) {
        return !state.getValue(LIT)
                && !Ignitable.rainedOn(level, pos)
                && level.getBlockEntity(pos) instanceof FirePitBlockEntity pit
                && pit.hasFuel();
    }

    @Override
    public boolean ignite(Level level, BlockPos pos, BlockState state) {
        if (!canIgnite(level, pos, state) || !(level.getBlockEntity(pos) instanceof FirePitBlockEntity pit)) return false;
        level.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL);
        pit.onIgnite();
        Journal.awardNear(level, pos, Journal.FIRE_PIT_LIT);
        level.playSound(null, pos, ModSounds.FIRE_PIT_IGNITE.get(), SoundSource.BLOCKS, 1.0f, 0.9f + level.getRandom().nextFloat() * 0.2f);
        return true;
    }

    /** Puts the pit out. {@code doused} plays the hiss of rain on embers rather than a quiet fade. */
    static void extinguish(Level level, BlockPos pos, BlockState state, boolean doused) {
        level.setBlock(pos, state.setValue(LIT, false), Block.UPDATE_ALL);
        level.playSound(null, pos, ModSounds.FIRE_PIT_EXTINGUISH.get(), SoundSource.BLOCKS, doused ? 0.8f : 0.4f,
                doused ? 1.0f : 1.4f);
        if (doused && level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5,
                    8, 0.25, 0.1, 0.25, 0.02);
        }
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects,
            boolean intersects) {
        if (state.getValue(LIT) && entity instanceof LivingEntity && !entity.fireImmune() && level instanceof ServerLevel serverLevel) {
            entity.hurtServer(serverLevel, level.damageSources().campfire(), 1.0f);
        }
        super.entityInside(state, level, pos, entity, effects, intersects);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) return;
        double x = pos.getX() + 0.5, y = pos.getY() + 0.25, z = pos.getZ() + 0.5;
        if (random.nextInt(8) == 0) {
            level.playLocalSound(x, y, z, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS,
                    0.5f + random.nextFloat(), random.nextFloat() * 0.7f + 0.6f, false);
        }
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.SMOKE, x + (random.nextDouble() - 0.5) * 0.4, y + 0.3,
                    z + (random.nextDouble() - 0.5) * 0.4, 0, 0.04, 0);
        }
        if (random.nextInt(5) == 0) {
            level.addParticle(ParticleTypes.LAVA, x, y, z, 0, 0, 0);
        }
        if (random.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.FLAME, x + (random.nextDouble() - 0.5) * 0.3, y + 0.1,
                    z + (random.nextDouble() - 0.5) * 0.3, 0, 0.01, 0);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FirePitBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.FIRE_PIT.get(), FirePitBlockEntity::serverTick);
    }
}
