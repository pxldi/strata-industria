package dev.strataindustria.client.rail;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.transport.rail.MineTubEntity;
import dev.strataindustria.transport.rail.TankWagonEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.block.BlockModelRenderState;
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

    private final EntityModel<MineTubRenderState> tub;
    private final Identifier texture;
    private final float hinge;

    public MineTubRenderer(EntityRendererProvider.Context context) {
        this(context, new MineTubModel(context.bakeLayer(MineTubModel.LAYER)), TEXTURE, 0.41f);
    }

    /** A wagon drawn the same way with its own body, sheet and the distance from its middle to the edge it tips over. */
    protected MineTubRenderer(EntityRendererProvider.Context context, EntityModel<MineTubRenderState> body, Identifier texture, float hinge) {
        super(context, ModelLayers.MINECART);
        shadowRadius = 0.5f;
        this.tub = body;
        this.texture = texture;
        this.hinge = hinge;
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
        state.gauge = entity instanceof TankWagonEntity wagon ? wagon.gauge() : 0;
    }

    /**
     * Whether a model with a front must be turned about to face the way the vehicle heads, given the yaw the pose
     * is about to be rotated by. The model's +x end is its front: after the pose's flip it points along (-cos yaw, sin yaw).
     */
    protected boolean reversed(MineTubRenderState state, float poseYaw) {
        if (!state.directional) return false;
        double yaw = Math.toRadians(poseYaw), head = Math.toRadians(state.headingYaw);
        double frontX = -Math.cos(yaw), frontZ = Math.sin(yaw);
        double headX = -Math.sin(head), headZ = Math.cos(head);
        return frontX * headX + frontZ * headZ < 0;
    }

    @Override
    public void submit(MineTubRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        if (state.isNewRender) {
            float yaw = state.yRot, pitch = state.xRot;
            if (reversed(state, yaw)) {
                yaw += 180.0f;
                pitch = -pitch;
            }
            poseStack.rotateDegrees(Axis.YP, yaw);
            poseStack.rotateDegrees(Axis.ZP, -pitch);
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
            float yaw = 180.0f - rotation;
            if (reversed(state, yaw)) {
                yaw += 180.0f;
                xRot = -xRot;
            }
            poseStack.translate(0.0f, 0.375f, 0.0f);
            poseStack.rotateDegrees(Axis.YP, yaw);
            poseStack.rotateDegrees(Axis.ZP, -xRot);
        }
        float hurt = state.hurtTime;
        if (hurt > 0.0f) poseStack.rotateDegrees(Axis.XP, Mth.sin(hurt) * hurt * state.damageTime / 10.0f * state.hurtDir);
        if (state.tip > 0.001f) {
            // Tip over the +z edge, hinged on the bottom of the wall.
            poseStack.translate(0.0f, -0.375f, hinge);
            poseStack.rotateDegrees(Axis.XP, TIP_DEGREES * state.tip);
            poseStack.translate(0.0f, 0.375f, -hinge);
        }
        // A flat wagon's load is drawn as the vanilla cart draws a block it carries.
        BlockModelRenderState block = state.displayBlockModel;
        if (!block.isEmpty()) {
            poseStack.pushPose();
            poseStack.scale(0.75f, 0.75f, 0.75f);
            poseStack.translate(-0.5f, (state.displayOffset - 8) / 16.0f, 0.5f);
            poseStack.rotateDegrees(Axis.YP, 90.0f);
            block.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
            poseStack.popPose();
        }
        poseStack.scale(-1.0f, -1.0f, 1.0f);
        collector.submitModel(tub, state, poseStack, texture, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
    }
}
