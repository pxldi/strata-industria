package dev.strataindustria.client;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.knapping.Knapping;
import dev.strataindustria.knapping.Shaping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Sneak + scroll with a rock, flint, clay or a blank in hand picks the shape instead of changing the hotbar slot. */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID, value = Dist.CLIENT)
public final class ShapingScrollClient {
    private ShapingScrollClient() {}

    @SubscribeEvent
    static void onScroll(InputEvent.MouseScrollingEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || Minecraft.getInstance().gui.screen() != null || !player.isShiftKeyDown()) return;
        if (!Knapping.isKnappable(player.getMainHandItem()) && !Knapping.isKnappable(player.getOffhandItem())) return;
        double scroll = event.getScrollDeltaY();
        if (scroll == 0.0) return;
        event.setCanceled(true);
        ClientPacketDistributor.sendToServer(new Shaping.Cycle(scroll > 0 ? -1 : 1));
    }
}
