package dev.strataindustria.client.rubber;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.registry.Tier5Fluids;
import dev.strataindustria.registry.Tier5Particles;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/** Client registrations for tier 5: the latex sprites and the tap's drip particle. */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID, value = Dist.CLIENT)
public final class Tier5Client {
    private Tier5Client() {}

    @SubscribeEvent
    static void registerFluidModels(RegisterFluidModelsEvent event) {
        event.register(new FluidModel.Unbaked(new Material(StrataIndustria.id("block/latex_still")),
                new Material(StrataIndustria.id("block/latex_flow")), null, null), Tier5Fluids.LATEX, Tier5Fluids.FLOWING_LATEX);
    }

    @SubscribeEvent
    static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(Tier5Particles.LATEX_DRIP.get(), LatexDripParticle::provider);
    }
}
