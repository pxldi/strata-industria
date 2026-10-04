package dev.strataindustria.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.power.Kinetic;
import dev.strataindustria.power.Kinetics;
import java.util.function.Function;
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
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Spins a rotor model at the network's speed (spec 7.4). Rotor models are built around the vertical
 * axis through the block centre and drawn through a render-only item model ({@code items/rotor/*}).
 * Every block of a network uses the same clock, so meshing parts stay in step.
 */
public class RotorRenderer<T extends BlockEntity & Kinetic> implements BlockEntityRenderer<T, RotorRenderer.State> {
    private final ItemModelResolver itemModelResolver;
    private final ItemStack rotor;
    private final Function<BlockState, @Nullable Direction> up;
    private final int reach;

    /**
     * @param model the rotor's name under {@code items/rotor/}
     * @param up    which way the rotor model's +Y points for a block state, or null to draw nothing
     * @param reach how many blocks the model reaches past its own block, for culling
     */
    public RotorRenderer(BlockEntityRendererProvider.Context context, String model, Function<BlockState, @Nullable Direction> up, int reach) {
        this.itemModelResolver = context.itemModelResolver();
        this.rotor = new ItemStack(Items.STICK);
        this.rotor.set(DataComponents.ITEM_MODEL, StrataIndustria.id("rotor/" + model));
        this.up = up;
        this.reach = reach;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(T block, State state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(block, state, partialTick, cameraPos, crumbling);
        long time = block.getLevel() == null ? 0 : block.getLevel().getGameTime();
        state.angle = Kinetics.angle(block.kinetic().rpm(), time, partialTick);
        state.up = up.apply(block.getBlockState());
        state.item.clear();
        if (state.up == null) return;
        itemModelResolver.updateForTopItem(state.item, rotor, ItemDisplayContext.NONE, block.getLevel(), null, 0);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.up == null || state.item.isEmpty()) return;
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        orient(pose, state.up);
        pose.rotateDegrees(Axis.YP, state.angle);
        state.item.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }

    /** Turns a rotor model's +Y to point {@code up}. */
    public static void orient(PoseStack pose, Direction up) {
        switch (up) {
            case DOWN -> pose.rotateDegrees(Axis.XP, 180);
            case NORTH -> pose.rotateDegrees(Axis.XP, -90);
            case SOUTH -> pose.rotateDegrees(Axis.XP, 90);
            case EAST -> pose.rotateDegrees(Axis.ZP, -90);
            case WEST -> pose.rotateDegrees(Axis.ZP, 90);
            default -> {}
        }
    }

    /** A stick that draws as the named rotor model. */
    public static ItemStack rotorStack(String model) {
        ItemStack stack = new ItemStack(Items.STICK);
        stack.set(DataComponents.ITEM_MODEL, StrataIndustria.id("rotor/" + model));
        return stack;
    }

    @Override
    public AABB getRenderBoundingBox(T block) {
        return new AABB(block.getBlockPos()).inflate(reach);
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState item = new ItemStackRenderState();
        float angle;
        @Nullable Direction up;
    }
}
