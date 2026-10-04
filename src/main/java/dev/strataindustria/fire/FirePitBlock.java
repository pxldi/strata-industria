package dev.strataindustria.fire;

import dev.strataindustria.ceramics.KilnFiring;
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
import java.util.Optional;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The first fire (redesign R6): a ring of stones round a bed, no screen. Sticks, straw, logs and charcoal are
 * fed to it by use and the fire grows in three sizes as it fills; unlit it is lit with flint struck on a rock,
 * a torch or flint and steel. Unfired clay and raw food stand on the four flat stones in the corners, set
 * down on the one you aim at and taken back with an empty hand. A stick held to a full, lit fire comes out
 * as a torch.
 */
public class FirePitBlock extends BaseEntityBlock implements Ignitable {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    /** The size of the fire, 0 (cold bed) to 3 (roaring); follows the burning time in the block entity. */
    public static final IntegerProperty FUEL = IntegerProperty.create("fuel", 0, 3);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 4, 16);

    public FirePitBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false).setValue(FUEL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT, FUEL);
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

    /** Light given off: embers glow dimly, and each size of flame is brighter. */
    public static int lightFor(BlockState state) {
        if (!state.getValue(LIT)) return 0;
        return switch (state.getValue(FUEL)) {
            case 0 -> 6;
            case 1 -> 10;
            case 2 -> 13;
            default -> 15;
        };
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        boolean lit = state.getValue(LIT);
        if (!(level.getBlockEntity(pos) instanceof FirePitBlockEntity pit)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        Optional<FirePitFuel> fuel = FirePitFuel.of(stack);
        if (fuel.isPresent()) {
            // The client does not know how much the fire holds, only its size.
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            if (pit.feed(fuel.get())) {
                stack.consume(1, player);
                feedEffects(level, pos, state, pit);
                return InteractionResult.SUCCESS;
            }
            // Too full to take more: a stick held to a lit fire comes out as a torch, anything else just thuds off.
            if (lit && stack.is(Items.STICK)) {
                stack.consume(1, player);
                ItemStack torch = new ItemStack(Items.TORCH);
                if (!player.addItem(torch)) Block.popResource(level, pos.above(), torch);
                level.playSound(null, pos, ModSounds.FIRE_PIT_TORCH.get(), SoundSource.BLOCKS, 0.8f, 0.9f + level.getRandom().nextFloat() * 0.2f);
            } else {
                level.playSound(null, pos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 0.5f, 0.7f);
            }
            return InteractionResult.SUCCESS;
        }
        // Unfired clay and raw food go on the stone you aim at; the heat does the rest.
        if (pit.canHold(stack, level) || (level.isClientSide() && KilnFiring.isFireable(stack))) {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            int want = FirePitBlockEntity.spotAt(hit.getLocation().x - pos.getX(), hit.getLocation().z - pos.getZ());
            ItemStack held = player.hasInfiniteMaterials() ? stack.copy() : stack;
            int spot = pit.placeOnHearth(held, want);
            if (spot < 0) return InteractionResult.TRY_WITH_EMPTY_HAND;
            // Each piece set down sounds a step higher.
            long count = pit.hearth().stream().filter(piece -> !piece.isEmpty()).count();
            level.playSound(null, pos, ModSounds.POTTERY_SET.get(), SoundSource.BLOCKS, 0.8f, 0.85f + 0.1f * count);
            ((ServerLevel) level).sendParticles(ParticleTypes.POOF, pos.getX() + FirePitBlockEntity.HEARTH_X[spot],
                    pos.getY() + FirePitBlockEntity.HEARTH_Y + 0.05, pos.getZ() + FirePitBlockEntity.HEARTH_Z[spot], 3, 0.06, 0.02, 0.06, 0.01);
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

    /** Fuel goes on: the flames flare up, embers fly and the pitch climbs with the size of the fire. */
    private static void feedEffects(Level level, BlockPos pos, BlockState state, FirePitBlockEntity pit) {
        int size = pit.size();
        float pitch = 0.8f + 0.18f * size + level.getRandom().nextFloat() * 0.06f;
        level.playSound(null, pos, ModSounds.FIRE_PIT_FEED.get(), SoundSource.BLOCKS, state.getValue(LIT) ? 0.9f : 0.6f, pitch);
        if (!(level instanceof ServerLevel serverLevel)) return;
        double x = pos.getX() + 0.5, y = pos.getY() + 0.2, z = pos.getZ() + 0.5;
        if (state.getValue(LIT)) {
            serverLevel.sendParticles(ParticleTypes.FLAME, x, y + 0.1, z, 6 + 5 * size, 0.12, 0.05, 0.12, 0.05);
            serverLevel.sendParticles(ParticleTypes.LAVA, x, y + 0.2, z, 3 + size * 2, 0.15, 0.05, 0.15, 0);
            serverLevel.sendParticles(ParticleTypes.SMALL_FLAME, x, y + 0.3, z, 4 + 3 * size, 0.2, 0.15, 0.2, 0.06);
        } else {
            serverLevel.sendParticles(ParticleTypes.POOF, x, y, z, 3, 0.15, 0.02, 0.15, 0.01);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof FirePitBlockEntity pit) || pit.hearthEmpty()) return InteractionResult.PASS;
        // An empty hand takes back the piece on the stone you aim at, or else the last one set down.
        if (!level.isClientSide()) {
            int want = FirePitBlockEntity.spotAt(hit.getLocation().x - pos.getX(), hit.getLocation().z - pos.getZ());
            ItemStack taken = pit.takeFrom(want);
            if (taken.isEmpty()) taken = pit.takeFromHearth();
            if (!player.addItem(taken)) Block.popResource(level, pos.above(), taken);
            level.playSound(null, pos, ModSounds.POTTERY_SET.get(), SoundSource.BLOCKS, 0.6f, 1.3f);
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
        Journal.awardNear(level, pos, Journal.FIRE_PIT_LIT);
        level.playSound(null, pos, ModSounds.FIRE_PIT_IGNITE.get(), SoundSource.BLOCKS, 1.0f, 0.9f + level.getRandom().nextFloat() * 0.2f);
        return true;
    }

    /** Puts the pit out. {@code doused} plays the hiss of rain on embers rather than a quiet fade. */
    static BlockState extinguish(Level level, BlockPos pos, BlockState state, boolean doused) {
        BlockState out = state.setValue(LIT, false);
        level.setBlock(pos, out, Block.UPDATE_ALL);
        level.playSound(null, pos, ModSounds.FIRE_PIT_EXTINGUISH.get(), SoundSource.BLOCKS, doused ? 0.8f : 0.4f,
                doused ? 1.0f : 1.4f);
        if (doused && level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5,
                    8, 0.25, 0.1, 0.25, 0.02);
        }
        return out;
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
        int size = state.getValue(FUEL);
        if (level.getBlockEntity(pos) instanceof FirePitBlockEntity pit) {
            // Pieces heating on the stones throw off shimmer, and food that is nearly done spits and steams.
            for (int i = 0; i < FirePitBlockEntity.HEARTH_SPOTS; i++) {
                if (!pit.isFiring(i) || random.nextInt(4) != 0) continue;
                boolean clay = pit.isClay(i);
                double sx = pos.getX() + FirePitBlockEntity.HEARTH_X[i] + (random.nextDouble() - 0.5) * 0.15;
                double sz = pos.getZ() + FirePitBlockEntity.HEARTH_Z[i] + (random.nextDouble() - 0.5) * 0.15;
                double sy = pos.getY() + FirePitBlockEntity.HEARTH_Y + 0.15;
                if (clay) {
                    level.addParticle(random.nextInt(3) == 0 ? ParticleTypes.SMALL_FLAME : ParticleTypes.SMOKE, sx, sy, sz, 0, 0.02, 0);
                } else {
                    level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, sx, sy, sz, 0, 0.012 + 0.02 * pit.firingProgress(i), 0);
                    if (pit.firingProgress(i) > 0.8f && random.nextInt(2) == 0) {
                        level.playLocalSound(sx, sy, sz, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.4f, 1.5f + random.nextFloat() * 0.4f, false);
                    }
                }
            }
        }
        double x = pos.getX() + 0.5, y = pos.getY() + 0.2, z = pos.getZ() + 0.5;
        if (size == 0) {
            // Down to embers: a faint glow and a thread of smoke.
            if (random.nextInt(10) == 0) level.addParticle(ParticleTypes.LAVA, x + (random.nextDouble() - 0.5) * 0.3, y, z + (random.nextDouble() - 0.5) * 0.3, 0, 0, 0);
            if (random.nextInt(6) == 0) level.addParticle(ParticleTypes.SMOKE, x, y + 0.1, z, 0, 0.03, 0);
            return;
        }
        if (random.nextInt(Math.max(2, 10 - 2 * size)) == 0) {
            level.playLocalSound(x, y, z, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS,
                    0.3f + 0.25f * size + random.nextFloat() * 0.5f, random.nextFloat() * 0.7f + 0.6f, false);
        }
        if (random.nextInt(3) == 0) {
            level.addParticle(size == 3 ? ParticleTypes.CAMPFIRE_COSY_SMOKE : ParticleTypes.SMOKE, x + (random.nextDouble() - 0.5) * 0.4,
                    y + 0.2 + 0.15 * size, z + (random.nextDouble() - 0.5) * 0.4, 0, 0.03 + 0.01 * size, 0);
        }
        // Embers drift up out of the flames, more of them and higher the bigger the fire.
        if (random.nextInt(Math.max(2, 7 - 2 * size)) == 0) {
            level.addParticle(ParticleTypes.LAVA, x + (random.nextDouble() - 0.5) * 0.3, y + 0.1, z + (random.nextDouble() - 0.5) * 0.3, 0, 0, 0);
        }
        if (random.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.FLAME, x + (random.nextDouble() - 0.5) * 0.15 * size, y + 0.1 * size,
                    z + (random.nextDouble() - 0.5) * 0.15 * size, 0, 0.005 + 0.01 * size, 0);
        }
        if (size == 3 && random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.SMALL_FLAME, x + (random.nextDouble() - 0.5) * 0.3, y + 0.6,
                    z + (random.nextDouble() - 0.5) * 0.3, (random.nextDouble() - 0.5) * 0.01, 0.03, (random.nextDouble() - 0.5) * 0.01);
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
