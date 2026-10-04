package dev.strataindustria.transport.signal;

import dev.strataindustria.StrataIndustria;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * The timetable (outposts spec 9.4): up to eight stop names in order, each with a departure rule or "the stop's own".
 * Used in the air it opens its screen; used on a lead vehicle it loads the vehicle. A blank one clears a vehicle.
 */
public class TimetableItem extends Item {
    public TimetableItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer serverPlayer) {
            ItemStack stack = player.getItemInHand(hand);
            TimetablePayloads.openItem(serverPlayer, stack.getOrDefault(SignalRegistry.TIMETABLE_STOPS.get(), TimetableStops.EMPTY));
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        TimetableStops stops = stack.getOrDefault(SignalRegistry.TIMETABLE_STOPS.get(), TimetableStops.EMPTY);
        String key = StrataIndustria.MOD_ID + ".timetable.";
        if (stops.isEmpty()) {
            tooltip.accept(Component.translatable(key + "blank").withStyle(ChatFormatting.GRAY));
            return;
        }
        int number = 1;
        for (TimetableStops.Entry entry : stops.entries()) {
            Component rule = entry.rule().isPresent()
                    ? Component.translatable(entry.rule().get().langKey()).append(entry.rule().get().usesSeconds() ? Component.literal(" " + entry.seconds() + " s") : Component.empty())
                    : Component.translatable(key + "stop_rule");
            tooltip.accept(Component.literal(number++ + ". " + entry.name() + "  ").withStyle(ChatFormatting.GRAY).append(rule.copy().withStyle(ChatFormatting.DARK_GRAY)));
        }
    }
}
