package dev.strataindustria.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.strataindustria.logistics.ItemPipeBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The items inside an item pipe (tier 5 spec 12.1): each stack slides in from the face it entered by, through
 * the middle, and out of the face it leaves by, 5 ticks to a block. One that is waiting for somewhere to go rests in the middle.
 */
public class ItemPipeRenderer implements BlockEntityRenderer<ItemPipeBlockEntity, ItemPipeRenderer.State> {
    private final ItemModelResolver itemModelResolver;

    public ItemPipeRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ItemPipeBlockEntity pipe, State state, float partialTick, Vec3 cameraPos,
            ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(pipe, state, partialTick, cameraPos, crumbling);
        state.count = 0;
        if (pipe.getLevel() == null) return;
        double now = pipe.getLevel().getGameTime() + (double) partialTick;
        for (ItemPipeBlockEntity.Travelling item : pipe.items()) {
            if (state.count >= ItemPipeBlockEntity.CAPACITY) break;
            int i = state.count++;
            double p = Math.max(0.0, Math.min(1.0, (now - item.stamp()) / ItemPipeBlockEntity.SPEED));
            Direction to = item.to();
            Vec3 from = vec(item.from());
            Vec3 at;
            if (to == null) at = from.scale(Math.max(0.0, 0.5 - p));
            else if (p < 0.5) at = from.scale(0.5 - p);
            else at = vec(to).scale(p - 0.5);
            state.x[i] = (float) (0.5 + at.x);
            state.y[i] = (float) (0.5 + at.y);
            state.z[i] = (float) (0.5 + at.z);
            state.items[i].clear();
            itemModelResolver.updateForTopItem(state.items[i], item.stack(), ItemDisplayContext.FIXED, pipe.getLevel(), null, (int) pipe.getBlockPos().asLong() + i);
        }
    }

    private static Vec3 vec(Direction side) {
        return new Vec3(side.getStepX(), side.getStepY(), side.getStepZ());
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        for (int i = 0; i < state.count; i++) {
            if (state.items[i].isEmpty()) continue;
            pose.pushPose();
            pose.translate(state.x[i], state.y[i], state.z[i]);
            pose.scale(0.3f, 0.3f, 0.3f);
            state.items[i].submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
    }

    @Override
    public AABB getRenderBoundingBox(ItemPipeBlockEntity pipe) {
        return new AABB(pipe.getBlockPos()).inflate(0.5);
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState[] items = new ItemStackRenderState[ItemPipeBlockEntity.CAPACITY];
        final float[] x = new float[ItemPipeBlockEntity.CAPACITY], y = new float[ItemPipeBlockEntity.CAPACITY], z = new float[ItemPipeBlockEntity.CAPACITY];
        int count;

        State() {
            for (int i = 0; i < items.length; i++) items[i] = new ItemStackRenderState();
        }
    }
}
