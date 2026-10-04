package dev.strataindustria.client;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.client.render.FluidFilterRenderer;
import dev.strataindustria.client.render.ItemPipeRenderer;
import dev.strataindustria.client.screen.StorageControllerScreen;
import dev.strataindustria.logistics.Tier5Logistics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** The client side of tier 5 logistics: the storage screen, the items inside pipes and the fluid in the filter's window. */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID, value = Dist.CLIENT)
public final class LogisticsClient {
    private LogisticsClient() {}

    @SubscribeEvent
    static void screens(RegisterMenuScreensEvent event) {
        event.register(Tier5Logistics.STORAGE_CONTROLLER_MENU.get(), StorageControllerScreen::new);
    }

    @SubscribeEvent
    static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Tier5Logistics.ITEM_PIPE_BE.get(), ItemPipeRenderer::new);
        event.registerBlockEntityRenderer(Tier5Logistics.FLUID_FILTER_BE.get(), FluidFilterRenderer::new);
    }
}
