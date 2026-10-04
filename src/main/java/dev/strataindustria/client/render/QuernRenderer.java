package dev.strataindustria.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.strataindustria.quern.QuernBlock;
import dev.strataindustria.quern.QuernBlockEntity;
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

/** Draws the stack waiting on the quern's top stone, turning with the stone. */
public class QuernRenderer implements BlockEntityRenderer<QuernBlockEntity, QuernRenderer.State> {
    private static final int MAX_PILE = 3;

    private final ItemModelResolver itemModelResolver;

    public QuernRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(QuernBlockEntity quern, State state, float partialTick, Vec3 cameraPos,
            ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(quern, state, partialTick, cameraPos, crumbling);
        ItemStack stack = quern.input();
        state.count = stack.isEmpty() ? 0 : Math.min(MAX_PILE, 1 + stack.getCount() / 16);
        state.turn = quern.getBlockState().getValue(QuernBlock.TURN);
        state.item.clear();
        if (!stack.isEmpty()) {
            itemModelResolver.updateForTopItem(state.item, stack, ItemDisplayContext.FIXED, quern.getLevel(), null, (int) quern.getBlockPos().asLong());
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.item.isEmpty()) return;
        for (int layer = 0; layer < state.count; layer++) {
            pose.pushPose();
            pose.translate(0.5, 9.1 / 16.0 + layer * 0.03, 0.5);
            pose.rotateDegrees(Axis.YP, state.turn * 90 + layer * 37);
            pose.rotateDegrees(Axis.XP, 90);
            pose.scale(0.4f, 0.4f, 0.4f);
            state.item.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState item = new ItemStackRenderState();
        int count;
        int turn;
    }
}
