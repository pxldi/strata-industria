package dev.strataindustria.transport.ropeway;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** Wire rope (outposts spec 8.1): used on the terminal, each tower and the return, in turn, it strings a ropeway. */
public class WireRopeItem extends Item {
    public WireRopeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() == null) return InteractionResult.PASS;
        return RopewayBuilder.rope(context.getPlayer(), context.getLevel(), context.getClickedPos());
    }
}
