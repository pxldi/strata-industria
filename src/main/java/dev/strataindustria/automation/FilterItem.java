package dev.strataindustria.automation;

import dev.strataindustria.StrataIndustria;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * The filter (tier 4 spec 13.5): right-click to set up to nine items or tags, whitelist or blacklist, and
 * whether ore grade counts. Inserters and belt diverters take one.
 */
public class FilterItem extends Item {
    public FilterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (id, inventory, p) -> new FilterMenu(id, inventory, hand),
                    Component.translatable("container." + StrataIndustria.MOD_ID + ".filter")),
                    buf -> buf.writeBoolean(hand == InteractionHand.MAIN_HAND));
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.6f, 1.1f);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        FilterContents contents = FilterContents.of(stack);
        String key = StrataIndustria.MOD_ID + ".filter.";
        if (contents.isEmpty()) {
            tooltip.accept(Component.translatable(key + "empty").withStyle(ChatFormatting.GRAY));
            return;
        }
        tooltip.accept(Component.translatable(key + (contents.whitelist() ? "whitelist" : "blacklist")).withStyle(ChatFormatting.GRAY));
        for (int i = 0; i < FilterContents.SIZE; i++) {
            if (contents.item(i) == Items.AIR) continue;
            Component line = contents.tag(i).isEmpty() ? new ItemStack(contents.item(i)).getHoverName()
                    : Component.translatable(key + "tag", contents.tag(i));
            tooltip.accept(Component.literal(" ").append(line).withStyle(ChatFormatting.DARK_GRAY));
        }
        if (contents.matchGrade()) tooltip.accept(Component.translatable(key + "grade_on").withStyle(ChatFormatting.GRAY));
    }
}
