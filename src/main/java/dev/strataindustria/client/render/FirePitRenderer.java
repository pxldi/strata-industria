package dev.strataindustria.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.strataindustria.fire.FirePitBlock;
import dev.strataindustria.fire.FirePitBlockEntity;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.LightCoordsUtil;
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
 * Draws the clay and food lying on the fire pit hearth stones. While the pit is hot enough a piece lights up from
 * within, and the longer it has been in the heat the harder it glows; a fired piece goes back to the
 * ordinary light of the day.
 */
public class FirePitRenderer implements BlockEntityRenderer<FirePitBlockEntity, FirePitRenderer.State> {
    private final ItemModelResolver itemModelResolver;

    public FirePitRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(FirePitBlockEntity pit, State state, float partialTick, Vec3 cameraPos,
            ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(pit, state, partialTick, cameraPos, crumbling);
        state.seed = (int) pit.getBlockPos().asLong();
        boolean lit = pit.getBlockState().getValue(FirePitBlock.LIT);
        for (int i = 0; i < FirePitBlockEntity.HEARTH_SPOTS; i++) {
            ItemStack stack = pit.hearth().get(i);
            state.items[i].clear();
            state.glow[i] = 0;
            if (stack.isEmpty()) continue;
            itemModelResolver.updateForTopItem(state.items[i], stack, ItemDisplayContext.FIXED, pit.getLevel(), null, state.seed + i);
            if (lit && pit.isClay(i) && pit.isFiring(i)) {
                // Starts at a warm half light and climbs to full bright as the piece nears done.
                float flicker = 0.04f * (float) Math.sin((pit.getLevel().getGameTime() + partialTick) * 0.35 + i * 1.7);
                state.glow[i] = Math.min(1, 0.5f + 0.5f * pit.firingProgress(i) + flicker);
            }
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        for (int i = 0; i < FirePitBlockEntity.HEARTH_SPOTS; i++) {
            if (state.items[i].isEmpty()) continue;
            pose.pushPose();
            pose.translate(FirePitBlockEntity.HEARTH_X[i], FirePitBlockEntity.HEARTH_Y + 0.02, FirePitBlockEntity.HEARTH_Z[i]);
            pose.rotateDegrees(Axis.YP, Math.floorMod(state.seed * 31 + i * 53, 90) - 45);
            pose.rotateDegrees(Axis.XP, 90);
            pose.scale(0.42f, 0.42f, 0.42f);
            int light = state.lightCoords;
            if (state.glow[i] > 0) {
                int block = Math.max(LightCoordsUtil.block(light), Math.round(state.glow[i] * 15));
                light = LightCoordsUtil.pack(block, LightCoordsUtil.sky(light));
            }
            state.items[i].submit(pose, collector, light, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState[] items = new ItemStackRenderState[FirePitBlockEntity.HEARTH_SPOTS];
        final float[] glow = new float[FirePitBlockEntity.HEARTH_SPOTS];
        int seed;

        State() {
            for (int i = 0; i < items.length; i++) items[i] = new ItemStackRenderState();
        }
    }
}
