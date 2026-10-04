package dev.strataindustria.bloomery;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.fire.FlintStrike;
import dev.strataindustria.fire.Ignitable;
import dev.strataindustria.registry.ModBlockEntities;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The bloomery controller (spec 5.1): a fire brick block with a copper door, set in front of the
 * chamber. Right-click with ore or charcoal to charge it, with a torch or flint and a rock to light it,
 * with a pickaxe to pull a bloom, or empty-handed for the status screen.
 */
public class BloomeryBlock extends BaseEntityBlock implements Ignitable {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public BloomeryBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof BloomeryBlockEntity bloomery)) return InteractionResult.PASS;
        boolean lighter = stack.is(Items.TORCH) || stack.is(Items.FLINT_AND_STEEL) || stack.is(Items.FIRE_CHARGE);
        if (lighter && !state.getValue(LIT)) {
            if (!level.isClientSide()) {
                if (ignite(level, pos, state)) {
                    if (stack.is(Items.FLINT_AND_STEEL)) stack.hurtAndBreak(1, player, hand);
                    else if (stack.is(Items.FIRE_CHARGE)) stack.consume(1, player);
                } else if (bloomery.refusal() != null) {
                    player.sendOverlayMessage(bloomery.refusal());
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (FlintStrike.isStriker(stack)) {
            if (!level.isClientSide() && !canIgnite(level, pos, state) && bloomery.refusal() != null) {
                player.sendOverlayMessage(bloomery.refusal());
            }
            return InteractionResult.PASS;
        }
        if (stack.is(ItemTags.PICKAXES) && bloomery.hasBlooms()) {
            if (level instanceof ServerLevel server) {
                ItemStack bloom = bloomery.extract();
                if (!player.addItem(bloom)) Block.popResource(level, pos.relative(state.getValue(FACING)), bloom);
                stack.hurtAndBreak(1, player, hand);
                server.playSound(null, pos, ModSounds.BLOOMERY_EXTRACT.get(), SoundSource.BLOCKS, 1.0f,
                        0.9f + server.getRandom().nextFloat() * 0.2f);
                Direction front = state.getValue(FACING);
                server.sendParticles(ParticleTypes.LAVA, pos.getX() + 0.5 + front.getStepX() * 0.55, pos.getY() + 0.4,
                        pos.getZ() + 0.5 + front.getStepZ() * 0.55, 5, 0.1, 0.1, 0.1, 0.0);
            }
            return InteractionResult.SUCCESS;
        }
        if (BloomeryBlockEntity.isOre(stack) || BloomeryBlockEntity.isFuel(stack) || stack.is(ModItems.LIGNITE.get())) {
            if (!level.isClientSide()) {
                int moved = bloomery.insert(stack, player);
                if (moved > 0) stack.consume(moved, player);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof BloomeryBlockEntity bloomery)) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) {
            ItemStack back = bloomery.takeLast();
            if (!back.isEmpty()) {
                if (!player.addItem(back)) Block.popResource(level, pos.relative(state.getValue(FACING)), back);
                level.playSound(null, pos, ModSounds.BLOOMERY_CHARGE.get(), SoundSource.BLOCKS, 0.6f, 1.3f);
            }
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(bloomery, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean canIgnite(Level level, BlockPos pos, BlockState state) {
        return !state.getValue(LIT) && level.getBlockEntity(pos) instanceof BloomeryBlockEntity bloomery && bloomery.canLight();
    }

    @Override
    public boolean ignite(Level level, BlockPos pos, BlockState state) {
        if (!canIgnite(level, pos, state) || !(level.getBlockEntity(pos) instanceof BloomeryBlockEntity bloomery)) return false;
        setLit(level, pos, state, true);
        bloomery.onLit();
        level.playSound(null, pos, ModSounds.BLOOMERY_LIGHT.get(), SoundSource.BLOCKS, 1.0f, 0.9f + level.getRandom().nextFloat() * 0.2f);
        return true;
    }

    static void setLit(Level level, BlockPos pos, BlockState state, boolean lit) {
        level.setBlock(pos, level.getBlockState(pos).setValue(LIT, lit), Block.UPDATE_ALL);
        if (!lit) level.playSound(null, pos, ModSounds.FIRE_PIT_EXTINGUISH.get(), SoundSource.BLOCKS, 0.5f, 0.8f);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) return;
        Direction front = state.getValue(FACING);
        double x = pos.getX() + 0.5 + front.getStepX() * 0.52, y = pos.getY() + 0.3, z = pos.getZ() + 0.5 + front.getStepZ() * 0.52;
        if (random.nextInt(5) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, ModSounds.BLOOMERY_ROAR.get(), SoundSource.BLOCKS,
                    0.7f + random.nextFloat() * 0.3f, 0.9f + random.nextFloat() * 0.2f, false);
        }
        double side = (random.nextDouble() - 0.5) * 0.5;
        double sx = front.getAxis() == Direction.Axis.X ? 0 : side, sz = front.getAxis() == Direction.Axis.Z ? 0 : side;
        if (random.nextInt(3) == 0) level.addParticle(ParticleTypes.SMALL_FLAME, x + sx, y, z + sz, 0, 0.01, 0);
        if (random.nextInt(8) == 0) level.addParticle(ParticleTypes.SMOKE, x + sx, y + 0.3, z + sz, 0, 0.03, 0);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BloomeryBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.BLOOMERY.get(), BloomeryBlockEntity::serverTick);
    }

    static String key(String suffix) {
        return StrataIndustria.MOD_ID + ".bloomery." + suffix;
    }
}
