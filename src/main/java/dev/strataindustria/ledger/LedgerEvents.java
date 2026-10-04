package dev.strataindustria.ledger;

import dev.strataindustria.StrataIndustria;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * The ledger on a controller. A finished multiblock is entered in the book; an unfinished one whose entry the
 * player already has is stamped. This runs before the controller opens its screen.
 */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class LedgerEvents {
    private LedgerEvents() {}

    @SubscribeEvent
    static void onUse(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getItemStack().is(LedgerRegistry.BUILDERS_LEDGER.get())) return;
        BlockPos pos = event.getPos();
        BlockState state = event.getLevel().getBlockState(pos);
        Plan plan = Plan.of(state);
        if (plan == null) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        use(player, plan, pos, state);
    }

    /** The ledger meets a controller. */
    public static void use(ServerPlayer player, Plan plan, BlockPos pos, BlockState state) {
        ServerLevel level = player.level();
        net.minecraft.core.Direction facing = Plan.facing(state);
        if (plan.complete(level, pos, facing)) {
            if (Ledgers.learn(player, plan)) {
                level.playSound(null, pos, LedgerRegistry.ENTRY.get(), SoundSource.PLAYERS, 0.8f, 1.0f);
                player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".ledger.entered",
                        Component.translatable(StrataIndustria.MOD_ID + ".ledger.plan." + plan.id())));
            } else {
                Stamping.say(player, "known");
            }
        } else if (!Ledgers.knows(player, plan)) {
            player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".ledger.unknown",
                    Component.translatable(StrataIndustria.MOD_ID + ".ledger.plan." + plan.id())));
        } else {
            Stamping.start(player, plan, pos, facing);
        }
    }

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) Stamping.tick(level);
    }

    @SubscribeEvent
    static void onStopped(ServerStoppedEvent event) {
        Stamping.clear();
    }

    @SubscribeEvent
    static void onTooltip(ItemTooltipEvent event) {
        if (!event.getItemStack().is(LedgerRegistry.BUILDERS_LEDGER.get())) return;
        event.getToolTip().add(Component.translatable(StrataIndustria.MOD_ID + ".ledger.tooltip.enter").withStyle(ChatFormatting.GRAY));
        event.getToolTip().add(Component.translatable(StrataIndustria.MOD_ID + ".ledger.tooltip.stamp").withStyle(ChatFormatting.GRAY));
    }
}
