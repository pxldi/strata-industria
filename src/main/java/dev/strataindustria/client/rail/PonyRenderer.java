package dev.strataindustria.client.rail;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.transport.rail.PonyEntity;
import net.minecraft.client.model.animal.equine.AbstractEquineModel;
import net.minecraft.client.model.animal.equine.DonkeyModel;
import net.minecraft.client.model.animal.equine.HorseModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.AbstractMinecartRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the pony: a vanilla horse, donkey or mule model walking between the rails with the harness drawn over it. The
 * place on the track comes from the vanilla cart renderer's state; the horse faces the way the pony heads and leans
 * with the slope.
 */
public class PonyRenderer extends AbstractMinecartRenderer<PonyEntity, PonyRenderState> {
    private static final Identifier HARNESS = StrataIndustria.id("textures/entity/pony_harness.png");
    private static final String[] COATS = {"white", "creamy", "chestnut", "brown", "black", "gray", "darkbrown"};
    private static final String[] MARKINGS = {null, "white", "whitefield", "whitedots", "blackdots"};
    private static final Identifier DONKEY = Identifier.withDefaultNamespace("textures/entity/horse/donkey.png");
    private static final Identifier MULE = Identifier.withDefaultNamespace("textures/entity/horse/mule.png");

    private final AbstractEquineModel<EquineRenderState> horse;
    private final DonkeyModel donkey;
    private final DonkeyModel mule;

    public PonyRenderer(EntityRendererProvider.Context context) {
        super(context, ModelLayers.MINECART);
        shadowRadius = 0.7f;
        horse = new HorseModel(context.bakeLayer(ModelLayers.HORSE));
        donkey = new DonkeyModel(context.bakeLayer(ModelLayers.DONKEY));
        mule = new DonkeyModel(context.bakeLayer(ModelLayers.MULE));
    }

    @Override
    public PonyRenderState createRenderState() {
        return new PonyRenderState();
    }

    @Override
    public void extractRenderState(PonyEntity entity, PonyRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.kind = entity.kind();
        state.look = entity.look();
        state.heading = entity.headingYaw();
        var equine = state.equine;
        equine.ageInTicks = state.ageInTicks;
        equine.walkAnimationSpeed = entity.walkSpeed(partialTicks);
        equine.walkAnimationPos = entity.walkPos(partialTicks);
        equine.eatAnimation = entity.eating() ? 1.0f : 0.0f;
        equine.isRidden = entity.isVehicle();
        equine.hasChest = false;
        equine.yRot = 0;
        equine.xRot = 0;
        equine.lightCoords = state.lightCoords;
        equine.outlineColor = state.outlineColor;
    }

    private static Identifier coat(int look) {
        int coat = Mth.clamp(look & 255, 0, COATS.length - 1);
        return Identifier.withDefaultNamespace("textures/entity/horse/horse_" + COATS[coat] + ".png");
    }

    @Override
    public void submit(PonyRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        float slope = 0.0f;
        if (!state.isNewRender && state.posOnRail != null && state.frontPos != null && state.backPos != null) {
            Vec3 front = state.frontPos, back = state.backPos;
            poseStack.translate(state.posOnRail.x - state.x, (front.y + back.y) / 2.0 - state.y, state.posOnRail.z - state.z);
            Vec3 along = back.subtract(front);
            double run = Math.sqrt(along.x * along.x + along.z * along.z);
            if (run > 1.0E-4) {
                double yaw = Math.toRadians(state.heading);
                double forward = (-Math.sin(yaw) * along.x + Math.cos(yaw) * along.z) / run;
                slope = (float) Math.toDegrees(Math.atan2(along.y * Math.signum(forward == 0 ? 1 : forward), run));
            }
        }
        float hurt = state.hurtTime;
        if (hurt > 0.0f) poseStack.rotateDegrees(Axis.XP, Mth.sin(hurt) * hurt * state.damageTime / 10.0f * state.hurtDir);
        poseStack.rotateDegrees(Axis.YP, 180.0f - state.heading);
        poseStack.rotateDegrees(Axis.XP, -slope);
        poseStack.scale(-1.0f, -1.0f, 1.0f);
        poseStack.translate(0.0f, -1.501f, 0.0f);
        var equine = state.equine;
        int overlay = OverlayTexture.NO_OVERLAY;
        switch (state.kind) {
            case PonyEntity.KIND_DONKEY -> collector.submitModel(donkey, equine, poseStack, DONKEY, state.lightCoords, overlay, state.outlineColor);
            case PonyEntity.KIND_MULE -> collector.submitModel(mule, equine, poseStack, MULE, state.lightCoords, overlay, state.outlineColor);
            default -> {
                collector.submitModel(horse, equine, poseStack, coat(state.look), state.lightCoords, overlay, state.outlineColor);
                int marking = Mth.clamp(state.look >> 8 & 255, 0, MARKINGS.length - 1);
                if (MARKINGS[marking] != null) {
                    Identifier texture = Identifier.withDefaultNamespace("textures/entity/horse/horse_markings_" + MARKINGS[marking] + ".png");
                    collector.order(1).submitModel(horse, equine, poseStack, RenderTypes.entityTranslucent(texture), state.lightCoords, overlay, state.outlineColor);
                }
            }
        }
        var harness = RenderTypes.entityTranslucent(HARNESS);
        switch (state.kind) {
            case PonyEntity.KIND_DONKEY -> collector.order(2).submitModel(donkey, equine, poseStack, harness, state.lightCoords, overlay, state.outlineColor);
            case PonyEntity.KIND_MULE -> collector.order(2).submitModel(mule, equine, poseStack, harness, state.lightCoords, overlay, state.outlineColor);
            default -> collector.order(2).submitModel(horse, equine, poseStack, harness, state.lightCoords, overlay, state.outlineColor);
        }
        poseStack.popPose();
    }
}
