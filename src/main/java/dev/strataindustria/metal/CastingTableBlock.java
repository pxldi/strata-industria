package dev.strataindustria.metal;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.registry.PrologueRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The casting table: a low stone table for a row of molds. Right-click with a mold to lay it down, with an
 * empty hand to knock out what has set, sneak with an empty hand to take a mold back.
 */
public class CastingTableBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 10, 0, 16, 14, 16),
            Block.box(1, 0, 1, 4, 10, 4), Block.box(12, 0, 1, 15, 10, 4),
            Block.box(1, 0, 12, 4, 10, 15), Block.box(12, 0, 12, 15, 10, 15));

    public CastingTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (!CastingTableBlockEntity.accepts(stack)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.getBlockEntity(pos) instanceof CastingTableBlockEntity table && !level.isClientSide()) {
            if (table.place(stack)) {
                stack.consume(1, player);
                level.playSound(null, pos, PrologueRegistry.TABLE_SET.get(), SoundSource.BLOCKS, 0.7f, 0.9f + level.getRandom().nextFloat() * 0.2f);
            } else {
                player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".casting_table.full"));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof CastingTableBlockEntity table)) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) {
            ItemStack back = table.takeBack();
            if (!back.isEmpty() && !player.addItem(back)) Block.popResource(level, pos.above(), back);
            return InteractionResult.SUCCESS;
        }
        if (table.knockOut((ServerLevel) level, player) == 0) {
            String why = table.moltenMolds() > 0 ? "still_molten" : "nothing_set";
            player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".casting_table." + why));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CastingTableBlockEntity(pos, state);
    }
}
