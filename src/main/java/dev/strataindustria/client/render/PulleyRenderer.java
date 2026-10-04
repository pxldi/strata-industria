package dev.strataindustria.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.strataindustria.power.Kinetics;
import dev.strataindustria.power.PulleyBlock;
import dev.strataindustria.power.PulleyBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * A pulley's wheel, turning with its network, and the belt to its partner (spec 7.3). The belt is
 * drawn once, by the pulley with the lower position, as two straight runs tangent to both wheels.
 */
public class PulleyRenderer implements BlockEntityRenderer<PulleyBlockEntity, PulleyRenderer.State> {
    /** Distance from the shaft to the belt, in blocks. */
    private static final float BELT_RADIUS = 6.5f / 16.0f;

    private final ItemModelResolver itemModelResolver;
    private final ItemStack wheel = RotorRenderer.rotorStack("pulley");
    private final ItemStack belt = RotorRenderer.rotorStack("belt");

    public PulleyRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(PulleyBlockEntity pulley, State state, float partialTick, Vec3 cameraPos,
            ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(pulley, state, partialTick, cameraPos, crumbling);
        long time = pulley.getLevel() == null ? 0 : pulley.getLevel().getGameTime();
        state.angle = Kinetics.angle(pulley.kinetic().rpm(), time, partialTick);
        state.axis = pulley.getBlockState().getValue(PulleyBlock.AXIS);
        BlockPos link = pulley.link();
        BlockPos pos = pulley.getBlockPos();
        state.belt = link != null && pos.asLong() < link.asLong() ? link.subtract(pos) : null;
        itemModelResolver.updateForTopItem(state.wheel, wheel, ItemDisplayContext.NONE, pulley.getLevel(), null, 0);
        state.beltItem.clear();
        if (state.belt != null) itemModelResolver.updateForTopItem(state.beltItem, belt, ItemDisplayContext.NONE, pulley.getLevel(), null, 0);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        RotorRenderer.orient(pose, Direction.fromAxisAndDirection(state.axis, Direction.AxisDirection.POSITIVE));
        pose.rotateDegrees(Axis.YP, state.angle);
        state.wheel.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
        if (state.belt == null || state.beltItem.isEmpty()) return;

        Vector3f along = new Vector3f(state.belt.getX(), state.belt.getY(), state.belt.getZ());
        float length = along.length();
        if (length < 1.0e-3f) return;
        along.div(length);
        Direction axisDir = Direction.fromAxisAndDirection(state.axis, Direction.AxisDirection.POSITIVE);
        Vector3f axis = new Vector3f(axisDir.getStepX(), axisDir.getStepY(), axisDir.getStepZ());
        // The belt's thickness points away from the line between the shafts, its width along the shafts.
        Vector3f out = new Vector3f(axis).cross(along).normalize();
        Vector3f width = new Vector3f(out).cross(along);
        Quaternionf facing = new Quaternionf().setFromNormalized(new Matrix3f(width, out, along));
        for (int side = -1; side <= 1; side += 2) {
            pose.pushPose();
            pose.translate(0.5 + along.x * length / 2 + out.x * BELT_RADIUS * side,
                    0.5 + along.y * length / 2 + out.y * BELT_RADIUS * side,
                    0.5 + along.z * length / 2 + out.z * BELT_RADIUS * side);
            pose.mulPose(new Matrix4f().rotation(facing));
            pose.scale(1.0f, 1.0f, length);
            state.beltItem.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
    }

    @Override
    public AABB getRenderBoundingBox(PulleyBlockEntity pulley) {
        return new AABB(pulley.getBlockPos()).inflate(PulleyBlockEntity.MAX_BELT + 1);
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState wheel = new ItemStackRenderState();
        final ItemStackRenderState beltItem = new ItemStackRenderState();
        float angle;
        Direction.Axis axis = Direction.Axis.Y;
        @Nullable BlockPos belt;
    }
}
