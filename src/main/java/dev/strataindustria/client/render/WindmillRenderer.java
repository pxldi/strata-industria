package dev.strataindustria.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.strataindustria.power.Kinetics;
import dev.strataindustria.power.WindmillBearingBlockEntity;
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
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * Turns a windmill (spec 7.2): the hub on the bearing and every sail joined to it, spun together
 * about the bearing's axis. The sail blocks themselves draw nothing while they are attached.
 */
public class WindmillRenderer implements BlockEntityRenderer<WindmillBearingBlockEntity, WindmillRenderer.State> {
    private final ItemModelResolver itemModelResolver;
    private final Supplier<ItemStack> hub = RotorRenderer.rotorStack("windmill_hub");
    private final Supplier<ItemStack> sail = RotorRenderer.rotorStack("windmill_sail");

    public WindmillRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(WindmillBearingBlockEntity bearing, State state, float partialTick, Vec3 cameraPos,
            ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(bearing, state, partialTick, cameraPos, crumbling);
        long time = bearing.getLevel() == null ? 0 : bearing.getLevel().getGameTime();
        state.angle = Kinetics.angle(bearing.kinetic().rpm(), time, partialTick);
        state.facing = bearing.facing();
        state.sails.clear();
        state.sails.addAll(bearing.sails());
        itemModelResolver.updateForTopItem(state.hub, hub.get(), ItemDisplayContext.NONE, bearing.getLevel(), null, 0);
        itemModelResolver.updateForTopItem(state.sail, sail.get(), ItemDisplayContext.NONE, bearing.getLevel(), null, 0);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        Direction facing = state.facing;
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        RotorRenderer.orient(pose, facing);
        pose.rotateDegrees(Axis.YP, state.angle);
        state.hub.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
        if (state.sails.isEmpty()) return;

        // Sails turn about the centre of their plane, one block in front of the bearing.
        pose.pushPose();
        pose.translate(0.5 + facing.getStepX(), 0.5 + facing.getStepY(), 0.5 + facing.getStepZ());
        pose.mulPose(new Matrix4f().rotation(Axis.of(new Vector3f(facing.getStepX(), facing.getStepY(), facing.getStepZ())).rotationDegrees(state.angle)));
        for (BlockPos rel : state.sails) {
            pose.pushPose();
            pose.translate(rel.getX() - facing.getStepX(), rel.getY() - facing.getStepY(), rel.getZ() - facing.getStepZ());
            RotorRenderer.orient(pose, facing);
            state.sail.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(WindmillBearingBlockEntity bearing) {
        return new AABB(bearing.getBlockPos()).inflate(WindmillBearingBlockEntity.REACH + 2);
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState hub = new ItemStackRenderState();
        final ItemStackRenderState sail = new ItemStackRenderState();
        final List<BlockPos> sails = new ArrayList<>();
        float angle;
        Direction facing = Direction.NORTH;
    }
}
