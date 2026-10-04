package dev.strataindustria.client.rail;

import dev.strataindustria.StrataIndustria;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** Draws the ore, tank and flat wagons: the mine tub's track following with a wagon body and sheet. */
public final class WagonRenderer extends MineTubRenderer {
    private WagonRenderer(EntityRendererProvider.Context context, ModelLayerLocation layer, String texture, float hinge) {
        super(context, new WagonModel(context.bakeLayer(layer)), StrataIndustria.id("textures/entity/" + texture + ".png"), hinge);
    }

    public static WagonRenderer of(EntityRendererProvider.Context context, ModelLayerLocation layer, String texture, float hinge) {
        return new WagonRenderer(context, layer, texture, hinge);
    }
}
