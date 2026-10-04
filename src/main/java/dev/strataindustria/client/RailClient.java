package dev.strataindustria.client;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.client.rail.MineTubModel;
import dev.strataindustria.client.rail.InclineWinchRenderer;
import dev.strataindustria.client.rail.MineTubRenderer;
import dev.strataindustria.client.rail.PonyRenderer;
import dev.strataindustria.client.screen.TubStopScreen;
import dev.strataindustria.transport.rail.RailRegistry;
import dev.strataindustria.transport.rail.StopData;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** The client side of the wooden tramway: the tub's model and the stop screen. */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID, value = Dist.CLIENT)
public final class RailClient {
    private RailClient() {}

    public static void openStop(StopData data, String station) {
        Minecraft.getInstance().gui.setScreen(new TubStopScreen(data, station));
    }

    @SubscribeEvent
    static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(MineTubModel.LAYER, MineTubModel::createBodyLayer);
    }

    @SubscribeEvent
    static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(RailRegistry.MINE_TUB_ENTITY.get(), MineTubRenderer::new);
        event.registerEntityRenderer(RailRegistry.PONY_ENTITY.get(), PonyRenderer::new);
        event.registerBlockEntityRenderer(RailRegistry.INCLINE_WINCH_ENTITY.get(), InclineWinchRenderer::new);
    }
}
