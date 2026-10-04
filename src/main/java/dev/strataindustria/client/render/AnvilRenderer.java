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

/** Draws the workpiece lying on the anvil face, and the finished piece beside it. */
public class AnvilRenderer implements BlockEntityRenderer<AnvilBlockEntity, AnvilRenderer.State> {
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
        update(state.output, anvil.getItem(AnvilBlockEntity.OUTPUT), anvil, seed + 1);
    }

    private void update(ItemStackRenderState render, ItemStack stack, AnvilBlockEntity anvil, int seed) {
        render.clear();
        if (!stack.isEmpty()) itemModelResolver.updateForTopItem(render, stack, ItemDisplayContext.FIXED, anvil.getLevel(), null, seed);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        draw(state, state.input, 0.0f, pose, collector);
        draw(state, state.output, 0.22f, pose, collector);
    }

    private static void draw(State state, ItemStackRenderState item, float offset, PoseStack pose, SubmitNodeCollector collector) {
        if (item.isEmpty()) return;
        pose.pushPose();
        pose.translate(0.5, state.top + 0.01, 0.5);
        pose.rotateDegrees(Axis.YP, -state.facing);
        pose.translate(offset, 0.0, offset == 0.0f ? 0.0 : 0.1);
        pose.rotateDegrees(Axis.XP, 90);
        pose.scale(0.45f, 0.45f, 0.45f);
        item.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState input = new ItemStackRenderState();
        final ItemStackRenderState output = new ItemStackRenderState();
        float top;
        float facing;
    }
}
