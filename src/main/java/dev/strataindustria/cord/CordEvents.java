package dev.strataindustria.cord;

import dev.strataindustria.StrataIndustria;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
final class CordEvents {
    private CordEvents() {}

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        CordItem.forget(event.getEntity().getUUID());
    }
}
