package dev.strataindustria.mark;

import dev.strataindustria.StrataIndustria;
import java.text.NumberFormat;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/** The tooltip lines of a stamped part, and the tally of blocks a stamped tool breaks. */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class MarkEvents {
    private MarkEvents() {}

    @SubscribeEvent
    static void onBreak(BlockDropsEvent event) {
        if (event.getBreaker() instanceof ServerPlayer player) MakerMarks.countBlock(player.getMainHandItem());
    }

    @SubscribeEvent
    static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        MakerStamp stamp = stack.get(MarkRegistry.STAMP.get());
        if (stamp == null) return;
        int at = Math.min(event.getToolTip().size(), event.getToolTip().isEmpty() ? 0 : 2);
        for (Component line : lines(stack, stamp)) event.getToolTip().add(at++, line);
    }

    /** Who made it, and what the tool has done since. */
    public static java.util.List<Component> lines(ItemStack stack, MakerStamp stamp) {
        java.util.List<Component> lines = new java.util.ArrayList<>();
        lines.add(Component.translatable(StrataIndustria.MOD_ID + ".mark.made_by", stamp.maker()).withStyle(ChatFormatting.GRAY));
        int mined = stack.getOrDefault(MarkRegistry.BLOCKS_MINED.get(), 0);
        if (mined > 0) {
            lines.add(Component.translatable(StrataIndustria.MOD_ID + ".mark.mined", NumberFormat.getIntegerInstance(Locale.ROOT).format(mined))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        return lines;
    }
}
