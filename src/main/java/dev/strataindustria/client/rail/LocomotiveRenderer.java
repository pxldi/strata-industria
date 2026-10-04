package dev.strataindustria.client.rail;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.transport.rail.MineTubEntity;
import dev.strataindustria.transport.rail.SteamLocomotiveEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** Draws the steam locomotive: the mine tub's track following, turned to face the way it heads. */
public final class LocomotiveRenderer extends MineTubRenderer {
    public LocomotiveRenderer(EntityRendererProvider.Context context) {
        super(context, new LocomotiveModel(context.bakeLayer(LocomotiveModel.LAYER)), StrataIndustria.id("textures/entity/steam_locomotive.png"), 0.41f);
    }

    @Override
    public void extractRenderState(MineTubEntity entity, MineTubRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        if (entity instanceof SteamLocomotiveEntity loco) {
            state.directional = true;
            state.headingYaw = loco.headingYaw();
            state.gauge = loco.waterGauge();
            state.wheelPhase = loco.wheelPhase(partialTicks);
        }
    }
}
