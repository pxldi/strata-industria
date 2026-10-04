package dev.strataindustria.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.strataindustria.electric.PoleInsulatorBlock;
import dev.strataindustria.electric.PoleInsulatorBlockEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
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
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * The overhead spans of an insulator (spec 8.4), drawn as a sagging line of short straight pieces. Each span is
 * drawn once, by the insulator at the lower position. There is no collision and no shock; the line is only seen.
 */
public class PoleInsulatorRenderer implements BlockEntityRenderer<PoleInsulatorBlockEntity, PoleInsulatorRenderer.State> {
    /** Sag at the middle of a span, as a fraction of its length. */
    private static final double SAG = 0.04;
    private static final int MAX_RENDER = 40;

    private final ItemModelResolver itemModelResolver;
    private final Supplier<ItemStack> segment = RotorRenderer.rotorStack("acsr_segment");

    public PoleInsulatorRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(PoleInsulatorBlockEntity insulator, State state, float partialTick, Vec3 cameraPos,
            ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(insulator, state, partialTick, cameraPos, crumbling);
        state.ends.clear();
        Level level = insulator.getLevel();
        BlockPos pos = insulator.getBlockPos();
        state.origin = pos;
        if (level == null) return;
        Vec3 from = PoleInsulatorBlockEntity.tip(pos, insulator.getBlockState());
        for (BlockPos other : insulator.spans()) {
            if (other.asLong() < pos.asLong()) continue;
            BlockState otherState = level.getBlockState(other);
            if (!(otherState.getBlock() instanceof PoleInsulatorBlock)) continue;
            state.ends.add(PoleInsulatorBlockEntity.tip(other, otherState).subtract(from));
        }
        state.start = from.subtract(pos.getX(), pos.getY(), pos.getZ());
        state.piece.clear();
        if (!state.ends.isEmpty()) itemModelResolver.updateForTopItem(state.piece, segment.get(), ItemDisplayContext.NONE, level, null, 0);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.ends.isEmpty() || state.piece.isEmpty()) return;
        for (Vec3 span : state.ends) {
            double length = span.length();
            if (length < 1.0e-3) continue;
            int pieces = Math.max(2, (int) Math.ceil(length * 1.5));
            Vec3 previous = state.start;
            for (int i = 1; i <= pieces; i++) {
                double t = i / (double) pieces;
                Vec3 next = state.start.add(span.x * t, span.y * t - 4 * SAG * length * t * (1 - t), span.z * t);
                Vec3 along = next.subtract(previous);
                double piece = along.length();
                if (piece > 1.0e-4) {
                    Vector3f dir = new Vector3f((float) (along.x / piece), (float) (along.y / piece), (float) (along.z / piece));
                    Quaternionf facing = new Quaternionf().rotationTo(new Vector3f(0, 0, 1), dir);
                    Vec3 mid = previous.add(next).scale(0.5);
                    pose.pushPose();
                    pose.translate(mid.x, mid.y, mid.z);
                    pose.mulPose(new Matrix4f().rotation(facing));
                    pose.scale(1.0f, 1.0f, (float) piece);
                    state.piece.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
                    pose.popPose();
                }
                previous = next;
            }
        }
    }

    @Override
    public AABB getRenderBoundingBox(PoleInsulatorBlockEntity insulator) {
        return new AABB(insulator.getBlockPos()).inflate(MAX_RENDER);
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState piece = new ItemStackRenderState();
        final List<Vec3> ends = new ArrayList<>();
        BlockPos origin = BlockPos.ZERO;
        Vec3 start = Vec3.ZERO;
    }
}
