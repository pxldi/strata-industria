package dev.strataindustria.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.strataindustria.automation.ConveyorBlock;
import dev.strataindustria.automation.ConveyorBlockEntity;
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
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A conveyor belt's moving top (tier 4 spec 13.1, 21): the leather surface is one of four render-only
 * models, each with the cross ribs shifted a pixel along, picked from the network's speed so the ribs run
 * at the speed of the items and stand still when the belt stops. The items it carries ride on top.
 */
public class ConveyorRenderer implements BlockEntityRenderer<ConveyorBlockEntity, ConveyorRenderer.State> {
    private static final int FRAMES = 4;
    private final ItemModelResolver itemModelResolver;
    /** Top models by slope then frame. */
    private final Supplier<ItemStack>[][] tops;

    @SuppressWarnings("unchecked")
    public ConveyorRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
        this.tops = new Supplier[ConveyorBlock.Slope.values().length][FRAMES];
        for (ConveyorBlock.Slope slope : ConveyorBlock.Slope.values()) {
            for (int f = 0; f < FRAMES; f++) tops[slope.ordinal()][f] = RotorRenderer.rotorStack("conveyor_top_" + slope.getSerializedName() + "_" + f);
        }
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ConveyorBlockEntity belt, State state, float partialTick, Vec3 cameraPos,
            ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(belt, state, partialTick, cameraPos, crumbling);
        var blockState = belt.getBlockState();
        state.slope = blockState.getValue(ConveyorBlock.SLOPE);
        state.yaw = 180.0f - blockState.getValue(ConveyorBlock.FACING).toYRot();
        long time = belt.getLevel() == null ? 0 : belt.getLevel().getGameTime();
        // The ribs are 4 px apart and the belt runs rpm / 16 px a tick, the same clock for every belt of a network.
        double travelled = (time + (double) partialTick) * belt.kinetic().rpm() / 16.0;
        state.frame = belt.speed() > 0.0f ? (int) Math.floorMod((long) Math.floor(travelled), (long) FRAMES) : 0;
        state.top.clear();
        itemModelResolver.updateForTopItem(state.top, tops[state.slope.ordinal()][state.frame].get(), ItemDisplayContext.NONE, belt.getLevel(), null, 0);
        float[] at = belt.positions(partialTick);
        for (int i = 0; i < ConveyorBlockEntity.SLOTS; i++) {
            state.items[i].clear();
            state.at[i] = at[i];
            ItemStack stack = belt.item(i);
            state.has[i] = !stack.isEmpty();
            if (state.has[i]) {
                itemModelResolver.updateForTopItem(state.items[i], stack, ItemDisplayContext.FIXED, belt.getLevel(), null, (int) belt.getBlockPos().asLong() + i);
            }
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0.5, 0.0, 0.5);
        pose.rotateDegrees(Axis.YP, state.yaw);
        pose.translate(-0.5, 0.0, -0.5);
        if (!state.top.isEmpty()) state.top.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        for (int i = 0; i < ConveyorBlockEntity.SLOTS; i++) {
            if (!state.has[i] || state.items[i].isEmpty()) continue;
            float t = Math.min(state.at[i], 1.0f);
            // Facing north, the front of the belt is z = 0; the surface is 4/16 up, rising or falling a block over the belt.
            double surface = switch (state.slope) {
                case FLAT -> 0.25;
                case UP -> 0.25 + t;
                case DOWN -> 1.25 - t;
            };
            pose.pushPose();
            pose.translate(0.5, surface + 0.09, 1.0 - t);
            if (state.slope == ConveyorBlock.Slope.UP) pose.rotateDegrees(Axis.XP, 45);
            if (state.slope == ConveyorBlock.Slope.DOWN) pose.rotateDegrees(Axis.XP, -45);
            pose.rotateDegrees(Axis.XP, 90);
            pose.scale(0.34f, 0.34f, 0.34f);
            state.items[i].submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(ConveyorBlockEntity belt) {
        return new AABB(belt.getBlockPos()).inflate(1.0);
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState top = new ItemStackRenderState();
        final ItemStackRenderState[] items = {new ItemStackRenderState(), new ItemStackRenderState(), new ItemStackRenderState(), new ItemStackRenderState()};
        final float[] at = new float[ConveyorBlockEntity.SLOTS];
        final boolean[] has = new boolean[ConveyorBlockEntity.SLOTS];
        ConveyorBlock.Slope slope = ConveyorBlock.Slope.FLAT;
        float yaw;
        int frame;
    }
}
