package dev.strataindustria.item;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

/**
 * Block item for ground cover. It is only placed while sneaking, so a plain right-click is free for
 * the item's own use.
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
}
