package dev.strataindustria.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.strataindustria.automation.ChuteBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** What a chute holds, lying in the bottom of its tube so a stuck line shows where it stops. */
public class ChuteRenderer implements BlockEntityRenderer<ChuteBlockEntity, ChuteRenderer.State> {
    private final ItemModelResolver itemModelResolver;

    public ChuteRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ChuteBlockEntity chute, State state, float partialTick, Vec3 cameraPos,
            ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(chute, state, partialTick, cameraPos, crumbling);
        state.item.clear();
        ItemStack held = chute.held();
        if (held.isEmpty()) return;
        itemModelResolver.updateForTopItem(state.item, held, ItemDisplayContext.FIXED, chute.getLevel(), null, (int) chute.getBlockPos().asLong());
        state.yaw = (chute.getBlockPos().asLong() & 3) * 90.0f;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.item.isEmpty()) return;
        pose.pushPose();
        pose.translate(0.5, 0.08, 0.5);
        pose.rotateDegrees(Axis.YP, state.yaw);
        pose.rotateDegrees(Axis.XP, 90);
        pose.scale(0.4f, 0.4f, 0.4f);
        state.item.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState item = new ItemStackRenderState();
        float yaw;
    }
}
