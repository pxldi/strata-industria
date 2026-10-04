package dev.strataindustria.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.strataindustria.structure.SpecimenShelfBlock;
import dev.strataindustria.structure.SpecimenShelfBlockEntity;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** The specimens standing in the four cubbies of a specimen shelf, 2x2 as the front texture draws them. */
public class SpecimenShelfRenderer implements BlockEntityRenderer<SpecimenShelfBlockEntity, SpecimenShelfRenderer.State> {
    /** Cubby centres in block units across the front, left to right and top to bottom as seen from the front. */
    private static final float[][] CUBBIES = {{0.25f, 0.75f}, {0.75f, 0.75f}, {0.25f, 0.25f}, {0.75f, 0.25f}};
    /** How far in front of the back of the shelf the specimens stand, in block units (the back board is one pixel). */
    private static final float BACK = 0.12f;

    private final ItemModelResolver itemModelResolver;

    public SpecimenShelfRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SpecimenShelfBlockEntity shelf, State state, float partialTick, Vec3 cameraPos,
            ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(shelf, state, partialTick, cameraPos, crumbling);
        state.facing = shelf.getBlockState().getValue(SpecimenShelfBlock.FACING);
        int seed = (int) shelf.getBlockPos().asLong();
        for (int i = 0; i < SpecimenShelfBlockEntity.SLOTS; i++) {
            ItemStack stack = shelf.items().get(i);
            state.items[i].clear();
            if (stack.isEmpty()) continue;
            itemModelResolver.updateForTopItem(state.items[i], stack, ItemDisplayContext.FIXED, shelf.getLevel(), null, seed + i);
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        // Work in the shelf's own frame (front toward north, x to the viewer's right), then turn it into place.
        float yaw = switch (state.facing) {
            case SOUTH -> 180f;
            case EAST -> 270f;
            case WEST -> 90f;
            default -> 0f;
        };
        for (int i = 0; i < SpecimenShelfBlockEntity.SLOTS; i++) {
            if (state.items[i].isEmpty()) continue;
            pose.pushPose();
            pose.translate(0.5, 0.0, 0.5);
            pose.rotateDegrees(com.mojang.math.Axis.YP, yaw);
            // Front faces north (-z); the cubby floor is at the back of the shelf (+z side).
            // The viewer's right is -x when looking south at a north-facing front.
            pose.translate(0.5 - CUBBIES[i][0], CUBBIES[i][1], 0.5 - BACK);
            pose.scale(0.34f, 0.34f, 0.34f);
            state.items[i].submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState[] items = new ItemStackRenderState[SpecimenShelfBlockEntity.SLOTS];
        Direction facing = Direction.NORTH;

        public State() {
            for (int i = 0; i < items.length; i++) items[i] = new ItemStackRenderState();
        }
    }
}
