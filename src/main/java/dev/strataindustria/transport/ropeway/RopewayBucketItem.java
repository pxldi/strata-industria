package dev.strataindustria.transport.ropeway;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** A bucket for the line (outposts spec 8.1): used on the drive terminal it goes on the hook, sneaking takes one back. */
public class RopewayBucketItem extends Item {
    public RopewayBucketItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() == null || !context.getLevel().getBlockState(context.getClickedPos()).is(RopewayRegistry.TERMINAL.get())) return InteractionResult.PASS;
        if (!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof RopewayTerminalBlockEntity terminal)) return InteractionResult.PASS;
        return terminal.bucketBy(context.getPlayer(), context.getItemInHand());
    }
}
