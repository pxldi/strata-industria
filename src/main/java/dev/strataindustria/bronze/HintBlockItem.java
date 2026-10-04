package dev.strataindustria.bronze;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

/**
 * A block item that says how it is used: a grey tooltip line, and the same line on the action bar when a
 * placement is refused, so a player who cannot find the right spot is told what the block needs.
 */
public class HintBlockItem extends BlockItem {
    private final String hintKey;
    private final boolean hintOnRefusal;

    public HintBlockItem(Block block, Properties properties, String hintKey, boolean hintOnRefusal) {
        super(block, properties);
        this.hintKey = hintKey;
        this.hintOnRefusal = hintOnRefusal;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        InteractionResult result = super.useOn(context);
        if (hintOnRefusal && !(result instanceof InteractionResult.Success) && !context.getLevel().isClientSide() && context.getPlayer() != null) {
            context.getPlayer().sendOverlayMessage(Component.translatable(hintKey));
        }
        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable(hintKey).withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
