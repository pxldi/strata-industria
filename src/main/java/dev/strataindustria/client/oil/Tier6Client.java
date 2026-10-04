package dev.strataindustria.client.oil;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.registry.Tier6Fluids;
import dev.strataindustria.registry.Tier6Particles;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/** Client registrations for tier 6: fluid sprites (spec 24.3) and the oil drip particles (spec 24.7). */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID, value = Dist.CLIENT)
public final class Tier6Client {
    private Tier6Client() {}

    @SubscribeEvent
    static void registerFluidModels(RegisterFluidModelsEvent event) {
        for (Tier6Fluids.Entry fluid : Tier6Fluids.ALL.values()) {
            // Gases have one still frame; it doubles as the flowing sprite, since they never sit in the world.
            String still = fluid.gas() ? fluid.name() : fluid.name() + "_still";
            String flow = fluid.gas() ? fluid.name() : fluid.name() + "_flow";
            event.register(new FluidModel.Unbaked(new Material(StrataIndustria.id("block/fluid/" + still)),
                    new Material(StrataIndustria.id("block/fluid/" + flow)), null, null), fluid.source(), fluid.flowing());
        }
    }

    @SubscribeEvent
    static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(Tier6Particles.DRIPPING_OIL.get(), OilDripParticle::hang);
        event.registerSpriteSet(Tier6Particles.FALLING_OIL.get(), OilDripParticle::fall);
        event.registerSpriteSet(Tier6Particles.LANDING_OIL.get(), OilDripParticle::land);
    }
}
