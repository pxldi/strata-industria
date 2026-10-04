package dev.strataindustria.transport.foot;

import dev.strataindustria.StrataIndustria;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Keeps the puller's side of the handcart honest: a hit lets go, and a stale slowdown is cleared. */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class HandcartEvents {
    private HandcartEvents() {}

    /** Taking damage lets go of the shafts (spec 4.3). */
    @SubscribeEvent
    static void onDamaged(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getInflictedDamage() <= 0) return;
        HandcartEntity cart = HandcartEntity.pulledBy(player);
        if (cart != null) cart.release();
    }

    /** A slowdown with no cart behind it, left by a dimension change or a crash, is taken off. */
    @SubscribeEvent
    static void onTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 20 == 0
                && Burden.isHauling(player) && !HandcartEntity.isPulling(player)) {
            Burden.setHauling(player, false);
        }
    }

    @SubscribeEvent
    static void onStopped(ServerStoppedEvent event) {
        HandcartEntity.forgetAll();
    }
}
