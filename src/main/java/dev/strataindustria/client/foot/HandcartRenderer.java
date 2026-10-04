package dev.strataindustria.client.foot;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.transport.foot.HandcartEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/** Draws the handcart, turned the way the entity faces. */
public class HandcartRenderer extends EntityRenderer<HandcartEntity, HandcartRenderState> {
    private static final Identifier TEXTURE = StrataIndustria.id("textures/entity/handcart.png");

    private final HandcartModel model;

    public HandcartRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.7f;
        model = new HandcartModel(context.bakeLayer(HandcartModel.LAYER));
    }

    @Override
    public HandcartRenderState createRenderState() {
        return new HandcartRenderState();
    }

    @Override
    public void extractRenderState(HandcartEntity entity, HandcartRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot(partialTicks);
        state.pulled = entity.isPulled();
        state.wheelAngle = entity.wheelAngle(partialTicks);
    }

    @Override
    public void submit(HandcartRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.0f, 0.375f, 0.0f);
        poseStack.rotateDegrees(Axis.YP, 180.0f - state.yRot);
        poseStack.scale(-1.0f, -1.0f, 1.0f);
        collector.submitModel(model, state, poseStack, TEXTURE, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }
}
