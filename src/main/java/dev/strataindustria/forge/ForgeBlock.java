package dev.strataindustria.forge;

import dev.strataindustria.fire.FirestarterItem;
import dev.strataindustria.fire.Ignitable;
import dev.strataindustria.registry.ModBlockEntities;
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
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A brick forge (spec 4.5): a hearth of charcoal in a brick body. {@link #LIT} while fuel burns,
 * {@link #HOT} while the coals glow, which lasts a while after the fuel runs out.
 */
public class ForgeBlock extends BaseEntityBlock implements Ignitable {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final BooleanProperty HOT = BooleanProperty.create("hot");
    private static final VoxelShape SHAPE = Shapes.join(Shapes.block(), Block.box(2, 13, 2, 14, 16, 14), BooleanOp.ONLY_FIRST);

    public ForgeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false).setValue(HOT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT, HOT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        boolean lighter = stack.is(Items.TORCH) || stack.is(Items.FLINT_AND_STEEL) || stack.is(Items.FIRE_CHARGE);
        if (lighter && canIgnite(level, pos, state)) {
            if (!level.isClientSide() && ignite(level, pos, state)) {
                if (stack.is(Items.FLINT_AND_STEEL)) stack.hurtAndBreak(1, player, hand);
                else if (stack.is(Items.FIRE_CHARGE)) stack.consume(1, player);
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.getItem() instanceof FirestarterItem && canIgnite(level, pos, state)) return InteractionResult.PASS;
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ForgeBlockEntity forge) player.openMenu(forge);
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean canIgnite(Level level, BlockPos pos, BlockState state) {
        return !state.getValue(LIT) && level.getBlockEntity(pos) instanceof ForgeBlockEntity forge && forge.hasFuel();
    }

    @Override
    public boolean ignite(Level level, BlockPos pos, BlockState state) {
        if (!canIgnite(level, pos, state) || !(level.getBlockEntity(pos) instanceof ForgeBlockEntity forge)) return false;
        setLit(level, pos, state, true);
        forge.onIgnite();
        level.playSound(null, pos, ModSounds.FORGE_IGNITE.get(), SoundSource.BLOCKS, 1.0f, 0.9f + level.getRandom().nextFloat() * 0.2f);
        return true;
    }

    static void setLit(Level level, BlockPos pos, BlockState state, boolean lit) {
        level.setBlock(pos, state.setValue(LIT, lit), Block.UPDATE_ALL);
        if (!lit) level.playSound(null, pos, ModSounds.FIRE_PIT_EXTINGUISH.get(), SoundSource.BLOCKS, 0.4f, 1.3f);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(HOT) && !state.getValue(LIT)) return;
        double x = pos.getX() + 0.5, y = pos.getY() + 0.85, z = pos.getZ() + 0.5;
        if (state.getValue(LIT)) {
            if (random.nextInt(6) == 0) {
                level.playLocalSound(x, y, z, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.5f + random.nextFloat() * 0.5f,
                        random.nextFloat() * 0.5f + 0.7f, false);
            }
            if (random.nextInt(2) == 0) {
                level.addParticle(ParticleTypes.SMOKE, x + (random.nextDouble() - 0.5) * 0.5, y + 0.2,
                        z + (random.nextDouble() - 0.5) * 0.5, 0, 0.05, 0);
            }
            if (random.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.SMALL_FLAME, x + (random.nextDouble() - 0.5) * 0.6, y,
                        z + (random.nextDouble() - 0.5) * 0.6, 0, 0.015, 0);
            }
        }
        if (state.getValue(HOT) && random.nextInt(10) == 0) level.addParticle(ParticleTypes.LAVA, x, y, z, 0, 0, 0);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ForgeBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.FORGE.get(), ForgeBlockEntity::serverTick);
    }
}
