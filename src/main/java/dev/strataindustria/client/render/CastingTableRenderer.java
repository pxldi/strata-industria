package dev.strataindustria.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.strataindustria.metal.CastingTableBlockEntity;
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

/** Draws the molds lying on a casting table, two by two; a filled mold shows its metal. */
public class CastingTableRenderer implements BlockEntityRenderer<CastingTableBlockEntity, CastingTableRenderer.State> {
    private static final float[][] SPOTS = {{0.27f, 0.27f}, {0.73f, 0.27f}, {0.27f, 0.73f}, {0.73f, 0.73f}};

    private final ItemModelResolver itemModelResolver;

    public CastingTableRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(CastingTableBlockEntity table, State state, float partialTick, Vec3 cameraPos,
            ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(table, state, partialTick, cameraPos, crumbling);
        state.seed = (int) table.getBlockPos().asLong();
        for (int i = 0; i < CastingTableBlockEntity.SLOTS; i++) {
            ItemStack stack = table.molds().get(i);
            state.items[i].clear();
            if (stack.isEmpty()) continue;
            itemModelResolver.updateForTopItem(state.items[i], stack, ItemDisplayContext.FIXED, table.getLevel(), null, state.seed + i);
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        for (int i = 0; i < CastingTableBlockEntity.SLOTS; i++) {
            if (state.items[i].isEmpty()) continue;
            pose.pushPose();
            pose.translate(SPOTS[i][0], 0.885, SPOTS[i][1]);
            pose.rotateDegrees(Axis.YP, Math.floorMod(state.seed * 31 + i * 7, 16) - 8);
            pose.rotateDegrees(Axis.XP, 90);
            pose.scale(0.5f, 0.5f, 0.5f);
            state.items[i].submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState[] items = new ItemStackRenderState[CastingTableBlockEntity.SLOTS];
        int seed;

        State() {
            for (int i = 0; i < items.length; i++) items[i] = new ItemStackRenderState();
        }
    }
}
