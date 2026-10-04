package dev.strataindustria.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.strataindustria.oil.PumpJackBlock;
import dev.strataindustria.oil.PumpJackBlockEntity;
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
 * The pump jack's parts that stand and nod (tier 6 spec 5.4 and 24.3): the Samson post behind the block, the
 * walking beam with the horse head, and the polished rod that slides into the wellhead. The models are built
 * facing north; the beam pivots on the top of the post and the rod follows the head.
 */
public class PumpJackRenderer implements BlockEntityRenderer<PumpJackBlockEntity, PumpJackRenderer.State> {
    /** Greatest tilt of the beam either way. */
    private static final float SWING_DEGREES = 14.0f;
    /** How far in front of the pivot the head hangs, in blocks, which is what the rod rises and falls by per unit of sine. */
    private static final float HEAD_REACH = 1.0f;
    /** The head's bridle sits a little ahead of the rod's line in the models. */
    private static final float ROD_SHIFT = 5.0f / 16.0f;
    /** The pivot pin in the top frame cell: 10 sixteenths up from its floor. */
    private static final float PIVOT_HEIGHT = 2.0f + 2.0f / 16.0f;

    private final ItemModelResolver itemModelResolver;
    private final Supplier<ItemStack>[] frame;
    private final Supplier<ItemStack> beam = RotorRenderer.rotorStack("pump_jack_beam");
    private final Supplier<ItemStack> rod = RotorRenderer.rotorStack("pump_jack_rod");

    @SuppressWarnings("unchecked")
    public PumpJackRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
        this.frame = new Supplier[] {RotorRenderer.rotorStack("pump_jack_frame_0"), RotorRenderer.rotorStack("pump_jack_frame_1"),
                RotorRenderer.rotorStack("pump_jack_frame_2")};
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(PumpJackBlockEntity jack, State state, float partialTick, Vec3 cameraPos,
            ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(jack, state, partialTick, cameraPos, crumbling);
        state.yaw = 180.0f - jack.getBlockState().getValue(PumpJackBlock.FACING).toYRot();
        float phase = jack.phase(jack.getBlockState().getValue(PumpJackBlock.RUNNING) ? partialTick : 0.0f);
        state.swing = SWING_DEGREES * (float) Math.sin(phase * 2.0 * Math.PI);
        for (int i = 0; i < 3; i++) {
            state.frame[i].clear();
            itemModelResolver.updateForTopItem(state.frame[i], frame[i].get(), ItemDisplayContext.NONE, jack.getLevel(), null, 0);
        }
        state.beam.clear();
        itemModelResolver.updateForTopItem(state.beam, beam.get(), ItemDisplayContext.NONE, jack.getLevel(), null, 0);
        state.rod.clear();
        itemModelResolver.updateForTopItem(state.rod, rod.get(), ItemDisplayContext.NONE, jack.getLevel(), null, 0);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        // Local frame: origin at the block centre, facing north (-Z), the post one block behind (+Z).
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.rotateDegrees(Axis.YP, state.yaw);
        for (int i = 0; i < 3; i++) {
            pose.pushPose();
            pose.translate(0, i, 1);
            state.frame[i].submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
        pose.pushPose();
        pose.translate(0, PIVOT_HEIGHT, 1);
        pose.rotateDegrees(Axis.XP, state.swing);
        state.beam.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
        pose.pushPose();
        pose.translate(0, HEAD_REACH * Math.sin(Math.toRadians(state.swing)), -ROD_SHIFT);
        state.rod.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(PumpJackBlockEntity jack) {
        return new AABB(jack.getBlockPos()).inflate(3).expandTowards(0, 2, 0);
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState[] frame = {new ItemStackRenderState(), new ItemStackRenderState(), new ItemStackRenderState()};
        final ItemStackRenderState beam = new ItemStackRenderState();
        final ItemStackRenderState rod = new ItemStackRenderState();
        float yaw;
        float swing;
    }
}
