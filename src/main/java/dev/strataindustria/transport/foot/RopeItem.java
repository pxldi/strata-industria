package dev.strataindustria.transport.foot;

import dev.strataindustria.StrataIndustria;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Rope (spec 4.1). Used on the underside of a block it pays out a rope ladder, one block per rope, down to 32
 * blocks. Used on a hanging ladder it adds to the bottom.
 */
public class RopeItem extends Item {
    public static final int MAX_LENGTH = 32;

    public RopeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos clicked = context.getClickedPos();
        BlockState state = level.getBlockState(clicked);
        BlockPos start;
        if (state.is(FootRegistry.ROPE_LADDER.get())) {
            start = RopeLadderBlock.bottom(level, clicked).below();
        } else if (context.getClickedFace() == Direction.DOWN) {
            start = clicked.below();
        } else {
            return InteractionResult.PASS;
        }
        Direction facing = player == null ? Direction.NORTH : player.getDirection().getOpposite();
        if (state.is(FootRegistry.ROPE_LADDER.get())) facing = state.getValue(RopeLadderBlock.FACING);
        int placed = hang(level, start, facing, context.getItemInHand(), player);
        if (placed == 0) return InteractionResult.PASS;
        level.playSound(null, start, FootRegistry.ROPE_HANG.get(), SoundSource.BLOCKS, 0.8f, 0.9f + level.getRandom().nextFloat() * 0.2f);
        return InteractionResult.SUCCESS;
    }

    /** Pays out as much ladder as the stack and the column allow; returns the blocks placed. */
    public static int hang(Level level, BlockPos start, Direction facing, ItemStack stack, Player player) {
        int existing = 0;
        for (BlockPos above = start.above(); level.getBlockState(above).is(FootRegistry.ROPE_LADDER.get()); above = above.above()) existing++;
        boolean creative = player != null && player.hasInfiniteMaterials();
        int room = MAX_LENGTH - existing;
        int budget = creative ? room : Math.min(room, stack.getCount());
        int placed = 0;
        BlockState ladder = FootRegistry.ROPE_LADDER.get().defaultBlockState().setValue(RopeLadderBlock.FACING, facing);
        BlockPos pos = start;
        while (placed < budget && level.getBlockState(pos).canBeReplaced() && !level.isOutsideBuildHeight(pos)) {
            if (!level.isClientSide()) level.setBlock(pos, ladder, Block.UPDATE_ALL);
            placed++;
            pos = pos.below();
        }
        if (placed == 0) {
            if (player != null && existing >= MAX_LENGTH && !level.isClientSide()) {
                player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".rope.too_long"));
            }
            return 0;
        }
        if (!creative) stack.shrink(placed);
        return placed;
    }
}
