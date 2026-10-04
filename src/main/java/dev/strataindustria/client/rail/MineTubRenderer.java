package dev.strataindustria.client.rail;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.transport.rail.MineTubEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.AbstractMinecartRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the mine tub. The track following, heading and slope come from the vanilla cart renderer's state; only the
 * model and the tipping are ours.
 */
public class MineTubRenderer extends AbstractMinecartRenderer<MineTubEntity, MineTubRenderState> {
    private static final Identifier TEXTURE = StrataIndustria.id("textures/entity/mine_tub.png");
    /** The tub pivots on its bottom edge on the side it empties to. */
    private static final float TIP_DEGREES = 105.0f;

    private final MineTubModel tub;

    public MineTubRenderer(EntityRendererProvider.Context context) {
        super(context, ModelLayers.MINECART);
        shadowRadius = 0.5f;
        tub = new MineTubModel(context.bakeLayer(MineTubModel.LAYER));
    }

    @Override
    public MineTubRenderState createRenderState() {
        return new MineTubRenderState();
    }

    @Override
    public void extractRenderState(MineTubEntity entity, MineTubRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.tip = entity.tip(partialTicks);
        state.coupled = entity.isCoupled();
    }

    @Override
    public void submit(MineTubRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        if (state.isNewRender) {
            poseStack.rotateDegrees(Axis.YP, state.yRot);
            poseStack.rotateDegrees(Axis.ZP, -state.xRot);
            poseStack.translate(0.0f, 0.375f, 0.0f);
        } else {
            double entityX = state.x, entityY = state.y, entityZ = state.z;
            float xRot = state.xRot;
            float rotation = state.yRot;
            if (state.posOnRail != null && state.frontPos != null && state.backPos != null) {
                Vec3 front = state.frontPos, back = state.backPos;
                poseStack.translate(state.posOnRail.x - entityX, (front.y + back.y) / 2.0 - entityY, state.posOnRail.z - entityZ);
                Vec3 direction = back.add(-front.x, -front.y, -front.z);
                if (direction.length() != 0.0) {
                    direction = direction.normalize();
                    rotation = (float) (Math.atan2(direction.z, direction.x) * 180.0 / Math.PI);
                    xRot = (float) (Math.atan(direction.y) * 73.0);
                }
            }
            poseStack.translate(0.0f, 0.375f, 0.0f);
            poseStack.rotateDegrees(Axis.YP, 180.0f - rotation);
            poseStack.rotateDegrees(Axis.ZP, -xRot);
        }
        float hurt = state.hurtTime;
        if (hurt > 0.0f) poseStack.rotateDegrees(Axis.XP, Mth.sin(hurt) * hurt * state.damageTime / 10.0f * state.hurtDir);
        if (state.tip > 0.001f) {
            // Tip over the +z edge, hinged on the bottom of the wall.
            poseStack.translate(0.0f, -0.375f, 0.41f);
            poseStack.rotateDegrees(Axis.XP, TIP_DEGREES * state.tip);
            poseStack.translate(0.0f, 0.375f, -0.41f);
        }
        poseStack.scale(-1.0f, -1.0f, 1.0f);
        collector.submitModel(tub, state, poseStack, TEXTURE, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
    }
}
