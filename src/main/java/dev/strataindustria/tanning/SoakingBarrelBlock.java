package dev.strataindustria.tanning;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.registry.ModBlockEntities;
import dev.strataindustria.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The soaking barrel (tier 3 spec 12.1). Sneak and use it with an empty hand to put the lid on or take
 * it off; pour water in with a bucket while it is open. Recipes run only while it is SEALED.
 */
public class SoakingBarrelBlock extends BaseEntityBlock {
    public static final BooleanProperty SEALED = BooleanProperty.create("sealed");
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 16, 15);

    public SoakingBarrelBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(SEALED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SEALED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        boolean water = stack.is(Items.WATER_BUCKET);
        if (!water && !stack.is(Items.BUCKET)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!(level.getBlockEntity(pos) instanceof SoakingBarrelBlockEntity barrel)) return InteractionResult.PASS;
        if (state.getValue(SEALED)) {
            if (!level.isClientSide()) player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".soaking_barrel.lid_on"));
            return InteractionResult.SUCCESS;
        }
        if (water) {
            if (!barrel.addWater(SoakingBarrelBlockEntity.BUCKET, true)) return InteractionResult.TRY_WITH_EMPTY_HAND;
            if (!level.isClientSide()) {
                barrel.addWater(SoakingBarrelBlockEntity.BUCKET, false);
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
                level.playSound(null, pos, ModSounds.SOAKING_BARREL_FILL.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
                level.gameEvent(player, GameEvent.FLUID_PLACE, pos);
            }
            return InteractionResult.SUCCESS;
        }
        // An empty bucket takes water back out; lye and tannin stay in the barrel.
        if (!barrel.takeWater()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide()) {
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.WATER_BUCKET)));
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0f, 1.0f);
            level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof SoakingBarrelBlockEntity barrel)) return InteractionResult.PASS;
        if (player.isSecondaryUseActive()) {
            if (!level.isClientSide()) {
                boolean seal = !state.getValue(SEALED);
                level.setBlock(pos, state.setValue(SEALED, seal), Block.UPDATE_ALL);
                barrel.lidChanged();
                level.playSound(null, pos, seal ? ModSounds.SOAKING_BARREL_SEAL.get() : ModSounds.SOAKING_BARREL_OPEN.get(),
                        SoundSource.BLOCKS, 1.0f, 0.9f + level.getRandom().nextFloat() * 0.2f);
                level.gameEvent(player, seal ? GameEvent.BLOCK_CLOSE : GameEvent.BLOCK_OPEN, pos);
            }
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) serverPlayer.openMenu(barrel, buf -> buf.writeBlockPos(pos));
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SoakingBarrelBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.SOAKING_BARREL.get(), SoakingBarrelBlockEntity::serverTick);
    }
}
