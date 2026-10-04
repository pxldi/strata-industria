package dev.strataindustria.client.rail;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.strataindustria.transport.rail.InclineWinchBlock;
import dev.strataindustria.transport.rail.InclineWinchBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Draws the winch's rope from the front of the drum to the first vehicle it is tied to. */
public class InclineWinchRenderer implements BlockEntityRenderer<InclineWinchBlockEntity, InclineWinchRenderer.State> {
    /** Rope colour (ARGB), the tan of the rope item. */
    private static final int ROPE = 0xFFB08838;

    public InclineWinchRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(InclineWinchBlockEntity winch, State state, float partialTick, Vec3 cameraPos,
            ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(winch, state, partialTick, cameraPos, crumbling);
        state.rope = null;
        if (winch.ropeTarget() < 0 || winch.getLevel() == null) return;
        Entity target = winch.getLevel().getEntity(winch.ropeTarget());
        if (target == null) return;
        Direction facing = winch.getBlockState().getValue(InclineWinchBlock.FACING);
        Vec3 from = new Vec3(0.5 + facing.getStepX() * 0.32, 0.38, 0.5 + facing.getStepZ() * 0.32);
        Vec3 to = target.getPosition(partialTick).add(0, 0.45, 0).subtract(winch.getBlockPos().getX(), winch.getBlockPos().getY(), winch.getBlockPos().getZ());
        state.from = from;
        state.rope = to.subtract(from);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        Vec3 rope = state.rope;
        if (rope == null || rope.lengthSqr() < 1.0E-4) return;
        Vec3 from = state.from;
        Vec3 unit = rope.normalize();
        float width = Minecraft.getInstance().gameRenderer.gameRenderState().windowRenderState.appropriateLineWidth * 1.6f;
        collector.submitCustomGeometry(pose, RenderTypes.lines(), (entry, buffer) ->
                buffer.addVertex(entry, (float) from.x, (float) from.y, (float) from.z).setColor(ROPE)
                        .setNormal(entry, (float) unit.x, (float) unit.y, (float) unit.z).setLineWidth(width));
        collector.submitCustomGeometry(pose, RenderTypes.lines(), (entry, buffer) ->
                buffer.addVertex(entry, (float) (from.x + rope.x), (float) (from.y + rope.y), (float) (from.z + rope.z)).setColor(ROPE)
                        .setNormal(entry, (float) unit.x, (float) unit.y, (float) unit.z).setLineWidth(width));
    }

    @Override
    public AABB getRenderBoundingBox(InclineWinchBlockEntity winch) {
        return new AABB(winch.getBlockPos()).inflate(34);
    }

    public static final class State extends BlockEntityRenderState {
        @Nullable Vec3 rope;
        Vec3 from = Vec3.ZERO;
    }
}
