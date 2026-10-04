package dev.strataindustria.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.strataindustria.ceramics.PitKilnBlock;
import dev.strataindustria.ceramics.PitKilnBlockEntity;
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
 * Draws the pieces set out in a pit kiln: small pieces lie flat on a 2x2, piled up to four high for a
 * stack, and a vessel or crucible stands upright in the middle. Once thatch covers them they are hidden.
 */
public class PitKilnRenderer implements BlockEntityRenderer<PitKilnBlockEntity, PitKilnRenderer.State> {
    private static final float[][] SPOTS = {{0.27f, 0.27f}, {0.73f, 0.27f}, {0.27f, 0.73f}, {0.73f, 0.73f}};
    private static final int MAX_PILE = 4;

    private final ItemModelResolver itemModelResolver;

    public PitKilnRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(PitKilnBlockEntity kiln, State state, float partialTick, Vec3 cameraPos,
            ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(kiln, state, partialTick, cameraPos, crumbling);
        state.hidden = kiln.getBlockState().getValue(PitKilnBlock.STRAW) > 0;
        state.large = false;
        state.seed = (int) kiln.getBlockPos().asLong();
        for (int i = 0; i < PitKilnBlockEntity.SPOTS; i++) {
            ItemStack stack = kiln.items().get(i);
            state.counts[i] = Math.min(MAX_PILE, stack.getCount());
            state.items[i].clear();
            if (stack.isEmpty() || state.hidden) continue;
            if (i == 0 && PitKilnBlockEntity.isLarge(stack)) state.large = true;
            itemModelResolver.updateForTopItem(state.items[i], stack, ItemDisplayContext.FIXED, kiln.getLevel(), null, state.seed + i);
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.hidden) return;
        if (state.large) {
            if (state.items[0].isEmpty()) return;
            pose.pushPose();
            pose.translate(0.5, 0.44, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(Math.floorMod(state.seed, 4) * 90 + 45));
            pose.scale(0.85f, 0.85f, 0.85f);
            state.items[0].submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
            return;
        }
        for (int i = 0; i < PitKilnBlockEntity.SPOTS; i++) {
            if (state.items[i].isEmpty()) continue;
            for (int layer = 0; layer < state.counts[i]; layer++) {
                pose.pushPose();
                pose.translate(SPOTS[i][0], 0.02 + layer * 0.035, SPOTS[i][1]);
                // Each piece in a pile turns a little, so a stack reads as a stack.
                pose.mulPose(Axis.YP.rotationDegrees(Math.floorMod(state.seed * 31 + i * 7 + layer * 13, 40) - 20 + i * 90));
                pose.mulPose(Axis.XP.rotationDegrees(90));
                pose.scale(0.42f, 0.42f, 0.42f);
                state.items[i].submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
                pose.popPose();
            }
        }
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState[] items = new ItemStackRenderState[PitKilnBlockEntity.SPOTS];
        final int[] counts = new int[PitKilnBlockEntity.SPOTS];
        boolean large;
        boolean hidden;
        int seed;

        State() {
            for (int i = 0; i < items.length; i++) items[i] = new ItemStackRenderState();
        }
    }
}
