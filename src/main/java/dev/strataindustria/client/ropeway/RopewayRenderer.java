package dev.strataindustria.client.ropeway;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.strataindustria.transport.ropeway.RopewayPath;
import dev.strataindustria.transport.ropeway.RopewayPayloads;
import dev.strataindustria.transport.ropeway.RopewayRegistry;
import dev.strataindustria.transport.ropeway.RopewayReturnBlockEntity;
import dev.strataindustria.transport.ropeway.RopewayTerminalBlockEntity;
import dev.strataindustria.transport.ropeway.RopewayTowerBlockEntity;
import dev.strataindustria.client.render.RotorRenderer;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * Draws a ropeway from the blocks that have it in range (outposts spec 8.3). The terminal and each tower draw the
 * span that leaves them, both ropes of it and every bucket hanging there; the terminal and the return also turn their
 * bull wheels with the rope, so a stopped line stands still and a running one shows its speed in the wheel.
 */
public class RopewayRenderer<T extends BlockEntity> implements BlockEntityRenderer<T, RopewayRenderer.State> {
    public enum Role { TERMINAL, RETURN, TOWER }

    /** Pieces the rope is drawn in, one every this many blocks. */
    private static final double PIECE = 1.5;
    /** Where a bucket's hanger meets the rope, below its own origin. */
    private static final float SWAY_DEGREES = 5.0f;

    private final ItemModelResolver itemModelResolver;
    private final Role role;
    private final Supplier<ItemStack> cable = RotorRenderer.rotorStack("ropeway_cable");
    private final Supplier<ItemStack> bucket = RotorRenderer.rotorStack("ropeway_bucket");
    private final Supplier<ItemStack> wheel = RotorRenderer.rotorStack("ropeway_wheel");
    private final Supplier<ItemStack> seat = RotorRenderer.rotorStack("ropeway_seat");

    public RopewayRenderer(BlockEntityRendererProvider.Context context, Role role) {
        this.itemModelResolver = context.itemModelResolver();
        this.role = role;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    private record Piece(Vec3 mid, Matrix4f turn, float length) {}

    private record Hung(Vec3 at, float yaw, float sway, int slot, boolean seat) {}

    @Override
    public void extractRenderState(T block, State state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(block, state, partialTick, cameraPos, crumbling);
        state.pieces.clear();
        state.hung.clear();
        state.hasWheel = false;
        Level level = block.getLevel();
        BlockPos pos = block.getBlockPos();
        if (level == null) return;
        BlockPos terminal = null, next = null;
        int span = 0;
        switch (role) {
            case TERMINAL -> {
                terminal = pos;
                next = ((RopewayTerminalBlockEntity) block).first();
            }
            case TOWER -> {
                RopewayTowerBlockEntity tower = (RopewayTowerBlockEntity) block;
                terminal = tower.terminal();
                next = tower.next();
                span = tower.index();
            }
            case RETURN -> terminal = ((RopewayReturnBlockEntity) block).terminal();
        }
        ClientRopeways.Line line = terminal == null ? null : ClientRopeways.get(terminal);
        double now = ClientRopeways.now(partialTick);
        double advance = line == null ? 0 : line.advance(now);
        // Stations have a bull wheel; so does an angle station, which is a tower block.
        boolean wheeled = role != Role.TOWER || block.getBlockState().is(RopewayRegistry.ANGLE_STATION.get());
        if (wheeled && line != null) {
            state.hasWheel = true;
            state.wheelAngle = (float) Math.toDegrees(advance / RopewayPath.LATERAL);
            itemModelResolver.updateForTopItem(state.wheel, wheel.get(), ItemDisplayContext.NONE, level, null, 0);
        } else if (wheeled && terminal != null) {
            state.hasWheel = true;
            state.wheelAngle = 0;
            itemModelResolver.updateForTopItem(state.wheel, wheel.get(), ItemDisplayContext.NONE, level, null, 0);
        }
        if (next == null) return;
        Vec3 origin = new Vec3(pos.getX(), pos.getY(), pos.getZ());
        // With the whole line known the ropes follow its bends; before it arrives the span is drawn straight.
        RopewayPath shape = line != null && span < line.path.spans() ? line.path
                : new RopewayPath(List.of(RopewayPath.anchor(pos), RopewayPath.anchor(next)));
        int ropeSpan = line != null && shape == line.path ? span : 0;
        double length = shape.spanLength(ropeSpan);
        if (length < 1.0E-3) return;
        itemModelResolver.updateForTopItem(state.cable, cable.get(), ItemDisplayContext.NONE, level, null, 0);
        int pieces = Math.max(2, (int) Math.ceil(length / PIECE));
        for (int side = -1; side <= 1; side += 2) {
            Vec3 previous = shape.rope(ropeSpan, 0.0, side).subtract(origin);
            for (int i = 1; i <= pieces; i++) {
                Vec3 point = shape.rope(ropeSpan, i / (double) pieces, side).subtract(origin);
                Vec3 along = point.subtract(previous);
                double piece = along.length();
                if (piece > 1.0E-4) {
                    Vector3f dir = new Vector3f((float) (along.x / piece), (float) (along.y / piece), (float) (along.z / piece));
                    state.pieces.add(new Piece(previous.add(point).scale(0.5), new Matrix4f().rotation(new Quaternionf().rotationTo(new Vector3f(0, 0, 1), dir)), (float) piece));
                }
                previous = point;
            }
        }
        if (line == null) return;
        double loop = 2.0 * line.path.length();
        int slot = 0;
        for (RopewayPayloads.BucketView view : line.buckets) {
            double s = (((view.offset() + advance) % loop) + loop) % loop;
            RopewayPath.Point point = line.path.point(s);
            if (point.span() != span) continue;
            if (slot >= state.contents.size()) state.contents.add(new ItemStackRenderState());
            ItemStackRenderState content = state.contents.get(slot);
            content.clear();
            if (!view.stack().isEmpty()) itemModelResolver.updateForTopItem(content, view.stack(), ItemDisplayContext.FIXED, level, null, (int) pos.asLong() + slot);
            float yaw = (float) Math.atan2(point.heading().x, point.heading().z);
            // The hanger swings a little as it goes, more the faster the rope runs.
            float sway = (float) (Math.sin(now * 0.12 + s * 0.9) * SWAY_DEGREES * Math.min(1.0, line.speed() * 8.0));
            state.hung.add(new Hung(point.position().subtract(pos.getX(), pos.getY(), pos.getZ()), yaw, view.seat() ? sway * 0.4f : sway, slot, view.seat()));
            slot++;
        }
        itemModelResolver.updateForTopItem(state.hanger, bucket.get(), ItemDisplayContext.NONE, level, null, 0);
        itemModelResolver.updateForTopItem(state.chair, seat.get(), ItemDisplayContext.NONE, level, null, 0);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.hasWheel && !state.wheel.isEmpty()) {
            pose.pushPose();
            pose.translate(0.5, 0.5, 0.5);
            pose.rotateDegrees(Axis.YP, state.wheelAngle);
            state.wheel.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
        if (!state.cable.isEmpty()) {
            for (Piece piece : state.pieces) {
                pose.pushPose();
                pose.translate(piece.mid.x, piece.mid.y, piece.mid.z);
                pose.mulPose(piece.turn);
                pose.scale(1.0f, 1.0f, piece.length);
                state.cable.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
                pose.popPose();
            }
        }
        if (state.hanger.isEmpty()) return;
        for (Hung hung : state.hung) {
            pose.pushPose();
            pose.translate(hung.at.x, hung.at.y, hung.at.z);
            pose.rotateDegrees(Axis.YP, (float) Math.toDegrees(hung.yaw));
            pose.rotateDegrees(Axis.ZP, hung.sway);
            if (hung.seat) {
                // The seat is modelled at half size so it fits a block; it is drawn twice that, hanging from the rope.
                pose.translate(0.0, -0.84, 0.0);
                pose.scale(2.0f, 2.0f, 2.0f);
                state.chair.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
                pose.popPose();
                continue;
            }
            state.hanger.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            ItemStackRenderState content = state.contents.get(hung.slot);
            if (!content.isEmpty()) {
                pose.pushPose();
                pose.translate(0.0, -0.56, 0.0);
                pose.scale(0.32f, 0.32f, 0.32f);
                content.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
                pose.popPose();
            }
            pose.popPose();
        }
    }

    @Override
    public AABB getRenderBoundingBox(T block) {
        BlockPos pos = block.getBlockPos();
        BlockPos next = null;
        if (block instanceof RopewayTerminalBlockEntity terminal) next = terminal.first();
        else if (block instanceof RopewayTowerBlockEntity tower) next = tower.next();
        AABB box = new AABB(pos);
        if (next != null) box = box.minmax(new AABB(next));
        return box.inflate(2.0);
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState wheel = new ItemStackRenderState();
        final ItemStackRenderState cable = new ItemStackRenderState();
        final ItemStackRenderState hanger = new ItemStackRenderState();
        final ItemStackRenderState chair = new ItemStackRenderState();
        final List<ItemStackRenderState> contents = new ArrayList<>();
        final List<Piece> pieces = new ArrayList<>();
        final List<Hung> hung = new ArrayList<>();
        boolean hasWheel;
        float wheelAngle;
    }
}
