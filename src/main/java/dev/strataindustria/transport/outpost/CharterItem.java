package dev.strataindustria.transport.outpost;

import dev.strataindustria.registry.TransportDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;

/** The charter post. Refuses to be set down too near another charter or past the owner's limit. */
public class CharterItem extends BlockItem {
    public CharterItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        if (context.getLevel() instanceof ServerLevel level && context.getPlayer() != null) {
            Component refusal = RouteIndex.get(level).refusal(context.getClickedPos(), context.getPlayer().getUUID());
            if (refusal != null) {
                context.getPlayer().sendOverlayMessage(refusal);
                return InteractionResult.FAIL;
            }
        }
        return super.place(context);
    }

    /** The name kept on a charter that was taken down, or "". */
    public static String name(net.minecraft.world.item.ItemStack stack) {
        return stack.getOrDefault(TransportDataComponents.CHARTER_NAME.get(), "");
    }
}
