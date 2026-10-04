package dev.strataindustria.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.strataindustria.smithing.AnvilBlock;
import dev.strataindustria.smithing.AnvilBlockEntity;
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

/**
 * Draws what lies on the anvil: the workpiece, which squashes and stretches towards the finished shape a step
 * per blow, a ghost of that shape floating above, the second piece of a weld, and the finished part, which hops
 * up when the last blow lands.
 */
public class AnvilRenderer implements BlockEntityRenderer<AnvilBlockEntity, AnvilRenderer.State> {
    /** Ticks a blow's squash takes to settle, and the finished part's hop to land. */
    private static final float SQUASH_TICKS = 6.0f, HOP_TICKS = 10.0f;

    private final ItemModelResolver itemModelResolver;

    public AnvilRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(AnvilBlockEntity anvil, State state, float partialTick, Vec3 cameraPos,
            ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(anvil, state, partialTick, cameraPos, crumbling);
        var blockState = anvil.getBlockState();
        state.top = blockState.getBlock() instanceof AnvilBlock block && block.isStone() ? 15.0f / 16.0f : 1.0f;
        state.facing = blockState.hasProperty(AnvilBlock.FACING) ? blockState.getValue(AnvilBlock.FACING).toYRot() : 0.0f;
        int seed = (int) anvil.getBlockPos().asLong();
        update(state.input, anvil.getItem(AnvilBlockEntity.INPUT), anvil, seed);
        update(state.second, anvil.getItem(AnvilBlockEntity.SECOND), anvil, seed + 2);
        update(state.output, anvil.getItem(AnvilBlockEntity.OUTPUT), anvil, seed + 1);
        boolean showGhost = !anvil.getItem(AnvilBlockEntity.INPUT).isEmpty() && anvil.getItem(AnvilBlockEntity.OUTPUT).isEmpty();
        update(state.result, showGhost ? anvil.viewResult() : ItemStack.EMPTY, anvil, seed + 3);
        float now = (anvil.getLevel() == null ? 0 : anvil.getLevel().getGameTime()) + partialTick;
        state.time = now;
        state.progress = anvil.viewTotal() <= 0 ? 0.0f : Math.min(1.0f, anvil.viewDone() / (float) anvil.viewTotal());
        float sinceBlow = now - anvil.struckAt();
        state.pulse = anvil.struckAt() == Long.MIN_VALUE || sinceBlow >= SQUASH_TICKS ? 0.0f : 1.0f - sinceBlow / SQUASH_TICKS;
        float sinceFinish = now - anvil.finishedAt();
        state.hop = anvil.finishedAt() == Long.MIN_VALUE || sinceFinish >= HOP_TICKS ? -1.0f : sinceFinish / HOP_TICKS;
    }

    private void update(ItemStackRenderState render, ItemStack stack, AnvilBlockEntity anvil, int seed) {
        render.clear();
        if (!stack.isEmpty()) itemModelResolver.updateForTopItem(render, stack, ItemDisplayContext.FIXED, anvil.getLevel(), null, seed);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        float t = state.progress;
        float squash = state.pulse * state.pulse * 0.22f;
        // The workpiece shrinks towards the result's size while the part grows out of it; each blow squashes it flat and wide.
        float inputScale = 1.0f - 0.5f * t;
        draw(state, state.input, 0.0f, 0.0f, 0.0f, inputScale * (1.0f + squash), inputScale * (1.0f - squash), 0.0f, pose, collector);
        if (t > 0.0f) {
            float resultScale = 0.45f + 0.55f * t;
            draw(state, state.result, 0.0f, 0.006f, 0.0f, resultScale * (1.0f + squash * 0.5f), resultScale * (1.0f - squash * 0.5f), 0.0f, pose, collector);
        } else {
            // The ghost of the result floats over the piece before the first blow.
            float bob = (float) Math.sin(state.time * 0.12f) * 0.03f;
            draw(state, state.result, 0.0f, 0.42f + bob, 0.0f, 0.55f, 0.55f, state.time * 2.0f, pose, collector);
        }
        draw(state, state.second, -0.22f, 0.0f, -0.05f, 1.0f, 1.0f, 0.0f, pose, collector);
        float hop = state.hop < 0.0f ? 0.0f : (float) Math.sin(Math.PI * state.hop) * 0.32f;
        draw(state, state.output, 0.0f, hop, 0.0f, 1.0f, 1.0f, state.hop < 0.0f ? 0.0f : state.hop * 360.0f, pose, collector);
    }

    private static void draw(State state, ItemStackRenderState item, float side, float lift, float depth, float scaleX, float scaleY, float spin,
            PoseStack pose, SubmitNodeCollector collector) {
        if (item.isEmpty()) return;
        pose.pushPose();
        pose.translate(0.5, state.top + 0.01 + lift, 0.5);
        pose.rotateDegrees(Axis.YP, -state.facing + spin);
        pose.translate(side, 0.0, depth);
        pose.rotateDegrees(Axis.XP, 90);
        pose.scale(0.45f * scaleX, 0.45f * scaleY, 0.45f);
        item.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState input = new ItemStackRenderState();
        final ItemStackRenderState second = new ItemStackRenderState();
        final ItemStackRenderState output = new ItemStackRenderState();
        final ItemStackRenderState result = new ItemStackRenderState();
        float top;
        float facing;
        float time;
        float progress;
        float pulse;
        float hop = -1.0f;
    }
}
