package dev.strataindustria.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.strataindustria.structure.WindlassBlock;
import dev.strataindustria.structure.WindlassBlockEntity;
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
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Turns the windlass drum. The drum is a rotor-style model ({@code items/rotor/windlass_drum}) built around
 * its own +Y axis, so it is laid along the drum axis like any rotor and spun about it.
 */
public class WindlassRenderer implements BlockEntityRenderer<WindlassBlockEntity, WindlassRenderer.State> {
    private final ItemModelResolver itemModelResolver;
    private final Supplier<ItemStack> drum = RotorRenderer.rotorStack("windlass_drum");

    public WindlassRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(WindlassBlockEntity windlass, State state, float partialTick, Vec3 cameraPos,
            ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(windlass, state, partialTick, cameraPos, crumbling);
        state.angle = windlass.angle(partialTick);
        // The drum model's +Y points along the drum: east for a drum on the x axis, south for one on the z axis.
        state.along = WindlassBlock.drumAxis(windlass.getBlockState()) == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        state.item.clear();
        itemModelResolver.updateForTopItem(state.item, drum.get(), ItemDisplayContext.NONE, windlass.getLevel(), null, 0);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.item.isEmpty()) return;
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        RotorRenderer.orient(pose, state.along);
        pose.rotateDegrees(Axis.YP, state.angle);
        state.item.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState item = new ItemStackRenderState();
        float angle;
        Direction along = Direction.EAST;
    }
}
