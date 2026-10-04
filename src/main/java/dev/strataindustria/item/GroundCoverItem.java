package dev.strataindustria.item;

import dev.strataindustria.knapping.Knapping;
import dev.strataindustria.knapping.Shaping;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Block item for ground cover. It is only placed while sneaking, so a plain right-click is free for
 * the item's own use: loose rocks are struck into tools. Sneaking in the air picks the shape.
 */
public class GroundCoverItem extends BlockItem {
    public GroundCoverItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() != null && !context.getPlayer().isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        return super.useOn(context);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!Knapping.isKnappable(held)) return super.use(level, player, hand);
        if (player instanceof ServerPlayer serverPlayer) Shaping.use(serverPlayer, hand);
        return InteractionResult.SUCCESS;
    }
}
