package dev.strataindustria.client.ropeway;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.transport.ropeway.RopewayRegistry;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client wiring for the ropeway (outposts spec 8): one renderer class draws the terminal, the return and the towers. */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID, value = Dist.CLIENT)
public final class RopewayClient {
    private RopewayClient() {}

    @SubscribeEvent
    static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(RopewayRegistry.TERMINAL_ENTITY.get(), context -> new RopewayRenderer<>(context, RopewayRenderer.Role.TERMINAL));
        event.registerBlockEntityRenderer(RopewayRegistry.RETURN_ENTITY.get(), context -> new RopewayRenderer<>(context, RopewayRenderer.Role.RETURN));
        event.registerEntityRenderer(RopewayRegistry.SEAT.get(), NoopRenderer::new);
        event.registerBlockEntityRenderer(RopewayRegistry.TOWER_ENTITY.get(), context -> new RopewayRenderer<>(context, RopewayRenderer.Role.TOWER));
    }
}
