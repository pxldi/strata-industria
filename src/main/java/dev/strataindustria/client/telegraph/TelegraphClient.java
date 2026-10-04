package dev.strataindustria.client.telegraph;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.transport.telegraph.TelegraphRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client wiring for the telegraph (outposts spec 9.1): the chalk on the dispatch board. */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID, value = Dist.CLIENT)
public final class TelegraphClient {
    private TelegraphClient() {}

    @SubscribeEvent
    static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(TelegraphRegistry.BOARD_ENTITY.get(), DispatchBoardRenderer::new);
    }
}
