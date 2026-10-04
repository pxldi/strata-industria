package dev.strataindustria.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.strataindustria.structure.ToolRackBlock;
import dev.strataindustria.structure.ToolRackBlockEntity;
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

/** The tools hanging on a tool rack's three pegs, drawn the way {@link SpecimenShelfRenderer} draws its cubbies. */
public class ToolRackRenderer implements BlockEntityRenderer<ToolRackBlockEntity, ToolRackRenderer.State> {
    /** Peg centres across the front, left to right as seen from the front, in block units. */
    private static final float[] PEGS = {0.2f, 0.5f, 0.8f};
    private static final float HEIGHT = 0.55f;
    /** How far in front of the rack's back the tools hang: the board is 3 pixels deep plus a little. */
    private static final float BACK = ToolRackBlock.DEPTH / 16f + 0.02f;

    private final ItemModelResolver itemModelResolver;

    public ToolRackRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ToolRackBlockEntity rack, State state, float partialTick, Vec3 cameraPos,
            ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(rack, state, partialTick, cameraPos, crumbling);
        state.facing = rack.getBlockState().getValue(ToolRackBlock.FACING);
        int seed = (int) rack.getBlockPos().asLong();
        for (int i = 0; i < ToolRackBlockEntity.SLOTS; i++) {
            ItemStack stack = rack.items().get(i);
            state.items[i].clear();
            if (stack.isEmpty()) continue;
            itemModelResolver.updateForTopItem(state.items[i], stack, ItemDisplayContext.FIXED, rack.getLevel(), null, seed + i);
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        float yaw = switch (state.facing) {
            case SOUTH -> 180f;
            case EAST -> 270f;
            case WEST -> 90f;
            default -> 0f;
        };
        for (int i = 0; i < ToolRackBlockEntity.SLOTS; i++) {
            if (state.items[i].isEmpty()) continue;
            pose.pushPose();
            pose.translate(0.5, 0.0, 0.5);
            pose.rotateDegrees(com.mojang.math.Axis.YP, yaw);
            // Same frame as the specimen shelf: front toward north, the viewer's right is -x, the back plane at +z.
            pose.translate(0.5 - PEGS[i], HEIGHT, 0.5 - BACK);
            pose.scale(0.42f, 0.42f, 0.42f);
            state.items[i].submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState[] items = new ItemStackRenderState[ToolRackBlockEntity.SLOTS];
        Direction facing = Direction.NORTH;

        public State() {
            for (int i = 0; i < items.length; i++) items[i] = new ItemStackRenderState();
        }
    }
}
