package dev.strataindustria.client.rail;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.transport.rail.ElectricTramEntity;
import dev.strataindustria.transport.rail.MineTubEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** Draws the electric tram: the mine tub's track following, turned to face the way it heads, with its pole on the wire. */
public final class TramRenderer extends MineTubRenderer {
    public TramRenderer(EntityRendererProvider.Context context) {
        super(context, new TramModel(context.bakeLayer(TramModel.LAYER)), StrataIndustria.id("textures/entity/electric_tram.png"), 0.41f);
    }

    @Override
    public void extractRenderState(MineTubEntity entity, MineTubRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        if (entity instanceof ElectricTramEntity tram) {
            state.directional = true;
            state.headingYaw = tram.headingYaw();
            state.poleAngle = tram.poleAngle(partialTicks);
            state.bellSwing = tram.bellSwing(partialTicks);
        }
    }
}
