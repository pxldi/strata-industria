package dev.strataindustria.ceramics;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.fire.Ignitable;
import dev.strataindustria.registry.ModBlockEntities;
import dev.strataindustria.registry.ModBlocks;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.ModSounds;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The pit kiln (spec 4.2). Sneak-placing an unfired piece on the ground makes one; eight straw and
 * eight logs go on top, one per click, then it is lit. It needs solid blocks on all four sides and
 * open air above, and burns until every piece is fired.
 */
public class PitKilnBlock extends BaseEntityBlock implements Ignitable {
    public static final int MAX_LAYERS = 8;
    public static final IntegerProperty STRAW = IntegerProperty.create("straw", 0, MAX_LAYERS);
    public static final IntegerProperty LOGS = IntegerProperty.create("logs", 0, MAX_LAYERS);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    private static final VoxelShape PLACED = Block.box(1, 0, 1, 15, 2, 15);
    private static final VoxelShape[] SHAPES = new VoxelShape[MAX_LAYERS * 2 + 1];

    static {
        for (int straw = 0; straw <= MAX_LAYERS; straw++) {
            for (int logs = 0; logs <= MAX_LAYERS; logs++) {
                int height = logs > 0 ? 8 + (logs > 4 ? 8 : 4) : straw;
                SHAPES[Math.min(height, SHAPES.length - 1)] = height == 0 ? PLACED : Block.box(0, 0, 0, 16, height, 16);
            }
        }
    }

    public PitKilnBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(STRAW, 0).setValue(LOGS, 0).setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STRAW, LOGS, LIT);
    }

    private static int height(BlockState state) {
        int logs = state.getValue(LOGS);
        return logs > 0 ? 8 + (logs > 4 ? 8 : 4) : state.getValue(STRAW);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = SHAPES[height(state)];
        return shape == null ? Shapes.block() : shape;
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

    /** Places a new kiln holding one item from {@code held}, if the spot above {@code ground} is free. */
    public static boolean placeNew(Level level, BlockPos pos, ItemStack held) {
        BlockState state = ModBlocks.PIT_KILN.get().defaultBlockState();
        if (!level.getBlockState(pos).canBeReplaced() || !state.canSurvive(level, pos)) return false;
        if (level.isClientSide()) return true;
        level.setBlock(pos, state, Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof PitKilnBlockEntity kiln) kiln.place(held);
        level.playSound(null, pos, SoundEvents.DECORATED_POT_PLACE, SoundSource.BLOCKS, 0.8f, 1.0f);
        return true;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof PitKilnBlockEntity kiln) || state.getValue(LIT)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        int straw = state.getValue(STRAW), logs = state.getValue(LOGS);

        if (stack.is(ModItems.STRAW.get()) && straw < MAX_LAYERS && !kiln.isEmpty()) {
            if (!level.isClientSide()) {
                stack.consume(1, player);
                level.setBlock(pos, state.setValue(STRAW, straw + 1), Block.UPDATE_ALL);
                level.playSound(null, pos, ModSounds.KILN_STRAW.get(), SoundSource.BLOCKS, 0.8f, 0.9f + level.getRandom().nextFloat() * 0.2f);
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.is(ItemTags.LOGS_THAT_BURN) && straw == MAX_LAYERS && logs < MAX_LAYERS) {
            if (!level.isClientSide()) {
                kiln.addLog(stack);
                stack.consume(1, player);
                level.setBlock(pos, state.setValue(LOGS, logs + 1), Block.UPDATE_ALL);
                level.playSound(null, pos, ModSounds.KILN_LOG.get(), SoundSource.BLOCKS, 0.8f, 0.9f + level.getRandom().nextFloat() * 0.2f);
            }
            return InteractionResult.SUCCESS;
        }

        boolean lighter = stack.is(Items.TORCH)
                || stack.is(Items.FLINT_AND_STEEL) || stack.is(Items.FIRE_CHARGE);
        if (lighter) {
            Optional<String> problem = problem(level, pos, state);
            if (problem.isPresent()) {
                if (!level.isClientSide()) {
                    player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".pit_kiln." + problem.get()));
                }
                return InteractionResult.FAIL;
            }
            if (!level.isClientSide() && ignite(level, pos, state)) {
                if (stack.is(Items.FLINT_AND_STEEL)) stack.hurtAndBreak(1, player, hand);
                else if (stack.is(Items.FIRE_CHARGE)) stack.consume(1, player);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    /** With an empty hand, an unthatched kiln gives its pieces back one spot at a time. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(STRAW) > 0 || !(level.getBlockEntity(pos) instanceof PitKilnBlockEntity kiln)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            ItemStack taken = kiln.takeLast();
            if (!taken.isEmpty() && !player.addItem(taken)) Block.popResource(level, pos, taken);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.4f, 1.0f + level.getRandom().nextFloat() * 0.4f);
            if (kiln.isEmpty()) level.removeBlock(pos, false);
        }
        return InteractionResult.SUCCESS;
    }

    /** What keeps the kiln from being lit, as a message key suffix; empty when it can be lit. */
    static Optional<String> problem(Level level, BlockPos pos, BlockState state) {
        if (state.getValue(LIT)) return Optional.of("burning");
        if (state.getValue(STRAW) < MAX_LAYERS) return Optional.of("needs_straw");
        if (state.getValue(LOGS) < MAX_LAYERS) return Optional.of("needs_logs");
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos next = pos.relative(side);
            if (!level.getBlockState(next).isCollisionShapeFullBlock(level, next)) return Optional.of("needs_walls");
        }
        if (!level.getBlockState(pos.above()).isAir()) return Optional.of("needs_air");
        if (Ignitable.rainedOn(level, pos)) return Optional.of("rain");
        return Optional.empty();
    }

    @Override
    public boolean canIgnite(Level level, BlockPos pos, BlockState state) {
        return problem(level, pos, state).isEmpty();
    }

    @Override
    public boolean ignite(Level level, BlockPos pos, BlockState state) {
        if (!canIgnite(level, pos, state)) return false;
        level.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL);
        level.playSound(null, pos, ModSounds.FIRE_PIT_IGNITE.get(), SoundSource.BLOCKS, 1.0f, 0.8f);
        return true;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PitKilnBlockEntity kiln && !player.isCreative()) {
            kiln.dropContents(level, pos, state.getValue(STRAW));
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) return;
        double x = pos.getX() + 0.5, y = pos.getY() + 1.0, z = pos.getZ() + 0.5;
        if (random.nextInt(6) == 0) {
            level.playLocalSound(x, y, z, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.6f + random.nextFloat(),
                    random.nextFloat() * 0.7f + 0.6f, false);
        }
        level.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true, x + (random.nextDouble() - 0.5) * 0.6, y,
                z + (random.nextDouble() - 0.5) * 0.6, 0, 0.07, 0);
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.FLAME, x + (random.nextDouble() - 0.5) * 0.8, y - 0.1,
                    z + (random.nextDouble() - 0.5) * 0.8, 0, 0.02, 0);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PitKilnBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.PIT_KILN.get(), PitKilnBlockEntity::serverTick);
    }
}
